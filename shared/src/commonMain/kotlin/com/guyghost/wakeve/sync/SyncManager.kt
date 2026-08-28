package com.guyghost.wakeve.sync

import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.UserRepository
import com.guyghost.wakeve.repository.TimeSlotStorageIdentity
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.getCurrentTimeMillis
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.SyncChange
import com.guyghost.wakeve.models.SyncConflict
import com.guyghost.wakeve.models.SyncEventData
import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.SyncRequest
import com.guyghost.wakeve.models.SyncResponse
import com.guyghost.wakeve.invitationexperience.CreationStudioSyncBinding
import com.guyghost.wakeve.invitationexperience.StudioCommitEnvelope
import com.guyghost.wakeve.invitationexperience.StudioCommitEnvelopeFactory
import com.guyghost.wakeve.invitationexperience.StudioPendingSyncSubject
import com.guyghost.wakeve.poll.PollBallotContract
import com.guyghost.wakeve.sync.conflict.ConflictDetector
import com.guyghost.wakeve.sync.conflict.ConflictLogRepository
import com.guyghost.wakeve.sync.conflict.ConflictSummary
import com.guyghost.wakeve.sync.conflict.ResolutionDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class PendingBallotSyncJoinFailureCode {
    RECEIPT_MISSING,
    PAYLOAD_EMPTY,
    PAYLOAD_MALFORMED,
    RECEIPT_ID_DIVERGENT,
    TUPLE_DIVERGENT,
    FINGERPRINT_DIVERGENT,
    OPERATION_KEY_DIVERGENT,
    RECEIPT_NOT_LOCAL_PENDING,
    RECEIPT_ALREADY_ACKNOWLEDGED
}

sealed interface PendingBallotSyncJoinProjection {
    data class Valid(val change: SyncChange) : PendingBallotSyncJoinProjection
    data class Inconsistent(
        val diagnosticCode: PendingBallotSyncJoinFailureCode,
        val code: PollBallotContract.FailureCode = PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT,
        val retryable: Boolean = false,
        val commitOutcome: PollBallotContract.CommitOutcome = PollBallotContract.CommitOutcome.UNKNOWN
    ) : PendingBallotSyncJoinProjection
}

enum class PendingStudioSyncJoinFailureCode {
    RECEIPT_MISSING,
    PAYLOAD_MALFORMED,
    SUBJECT_DIVERGENT,
    ENVELOPE_DIVERGENT,
    RECEIPT_NOT_PENDING
}

sealed interface PendingStudioSyncJoinProjection {
    data class Valid(
        val change: SyncChange,
        val subject: StudioPendingSyncSubject
    ) : PendingStudioSyncJoinProjection

    data class Inconsistent(
        val metadataId: String,
        val diagnosticCode: PendingStudioSyncJoinFailureCode,
        val code: PollBallotContract.FailureCode = PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT,
        val retryable: Boolean = false,
        val commitOutcome: PollBallotContract.CommitOutcome = PollBallotContract.CommitOutcome.UNKNOWN
    ) : PendingStudioSyncJoinProjection
}

/**
 * Client-side sync manager for offline-first synchronization
 */
class SyncManager(
    private val database: WakeveDb,
    private val eventRepository: DatabaseEventRepository,
    private val userRepository: UserRepository,
    private val networkDetector: NetworkStatusDetector,
    private val httpClient: SyncHttpClient,
    private val authTokenProvider: () -> String?,
    private val authTokenRefreshProvider: (suspend () -> String?)? = null,
    private val maxRetries: Int = 3,
    private val baseRetryDelayMs: Long = 1000L,
    private val metrics: SyncMetrics = InMemorySyncMetrics(),
    private val alertManager: SyncAlertManager = LoggingSyncAlertManager(),
    /**
     * Feature flag: enable conflict detection & logging.
     * When false, falls back to the original last-write-wins behaviour.
     * Safe to flip at runtime — the sync loop checks this on every conflict.
     */
    val conflictResolutionEnabled: Boolean = true,
    private val pendingSideEffectReplayers: List<PendingSyncSideEffectReplayer> = emptyList()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val conflictLog = ConflictLogRepository(database)
    private val syncMutex = Mutex()

    // Callbacks for the presentation layer
    /** Called when critical conflicts require user resolution. */
    var onCriticalConflictsDetected: ((ConflictSummary) -> Unit)? = null

    // Sync status
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    // Timestamp of the last successful sync — used for incremental sync (avoids full re-sync)
    private var lastSuccessfulSyncTimestamp: String? = null

    // Network status from platform-specific detector
    val isNetworkAvailable: StateFlow<Boolean> = networkDetector.isNetworkAvailable

    init {
        scope.launch {
            networkDetector.isNetworkAvailable.collect { available ->
                if (available && hasPendingChanges()) {
                    triggerSync()
                }
            }
        }
    }

    /**
     * Record a local change for later synchronization
     */
    suspend fun recordLocalChange(
        table: String,
        operation: SyncOperation,
        recordId: String,
        data: String,
        userId: String
    ): Result<Unit> = runCatching {
        val syncId = "sync_${getCurrentTimeMillis()}_${recordId}"
        val timestamp = getCurrentUtcIsoString()

        // Store the change in sync metadata
        userRepository.addSyncMetadata(
            id = syncId,
            tableName = table,
            recordId = recordId,
            operation = operation,
            timestamp = timestamp,
            userId = userId
        ).getOrThrow()

        // If network is available, trigger immediate sync
        if (isNetworkAvailable.value) {
            triggerSync()
        }
    }

    /**
     * Check if there are pending changes to sync
     */
    suspend fun hasPendingChanges(): Boolean {
        return userRepository.getPendingSyncChanges().isNotEmpty() ||
            pendingConfirmationDecisionSyncs().isNotEmpty() ||
            getPendingBallotSyncJoinProjections().isNotEmpty() ||
            getPendingStudioSyncJoinProjections().isNotEmpty() ||
            pendingSideEffectReplayers.any { it.hasPending() }
    }

    /**
     * Get all pending changes ready for sync
     */
    suspend fun getPendingChangesForSync(): List<SyncChange> {
        val genericChanges = userRepository.getPendingSyncChanges()
            .filterNot {
                it.tableName == POLL_BALLOT_TABLE || it.tableName == "poll_ballots" ||
                    it.id.startsWith(STUDIO_SYNC_METADATA_PREFIX)
            }
            .map { metadata ->
            // Get the actual data for this change
            val data = getChangeData(metadata.tableName, metadata.recordId)
            SyncChange(
                id = metadata.id,
                table = metadata.tableName,
                operation = metadata.operation.name,
                recordId = metadata.recordId,
                data = data,
                timestamp = metadata.timestamp,
                userId = metadata.userId
            )
        }
        val validBallots = getPendingBallotSyncJoinProjections()
            .filterIsInstance<PendingBallotSyncJoinProjection.Valid>()
            .map { it.change }
        val validStudioCommits = getPendingStudioSyncJoinProjections()
            .filterIsInstance<PendingStudioSyncJoinProjection.Valid>()
            .map { it.change }
        return genericChanges + pendingConfirmationDecisionSyncs() + validBallots + validStudioCommits
    }

    /**
     * Trigger synchronization with server
     */
    suspend fun triggerSync(): Result<SyncResponse> = runCatching {
        syncMutex.withLock {
            syncWithRetry().getOrThrow()
        }
    }

    /**
     * Kotlin/Native-friendly ballot retry boundary. Success is proven from the durable receipt,
     * whose transition is owned exclusively by a correlated non-blank server acknowledgement.
     */
    suspend fun retryPendingBallotSync(localReceiptId: String): Boolean {
        if (localReceiptId.isBlank()) return false
        val existing = database.pollBallotReceiptQueries
            .selectByReceiptId(localReceiptId)
            .executeAsOneOrNull()
            ?: return false
        if (existing.syncStatus == "SERVER_ACKNOWLEDGED" && !existing.serverReceiptId.isNullOrBlank()) {
            return true
        }
        if (existing.syncStatus != "LOCAL_PENDING") return false

        if (triggerSync().isFailure) return false
        val refreshed = database.pollBallotReceiptQueries
            .selectByReceiptId(localReceiptId)
            .executeAsOneOrNull()
            ?: return false
        return refreshed.syncStatus == "SERVER_ACKNOWLEDGED" &&
            !refreshed.serverReceiptId.isNullOrBlank()
    }

    /** Retries the exact persisted Studio subject; it never invokes aggregate creation again. */
    suspend fun retryPendingStudioSync(binding: CreationStudioSyncBinding): Boolean {
        val receipt = database.invitationExperienceQueries
            .selectOperationReceipt(binding.operationId, binding.eventId)
            .executeAsOneOrNull()
            ?: return false
        if (receipt.status == "COMMITTED" && !receipt.server_receipt_id.isNullOrBlank()) return true
        if (receipt.status != "PENDING_SYNC" || receipt.aggregate_revision != binding.aggregateRevision) return false
        if (binding.durableOperationRef.isNotBlank() &&
            receipt.durable_operation_ref != binding.durableOperationRef
        ) return false
        if (binding.requestFingerprint.isNotBlank() &&
            receipt.request_fingerprint != binding.requestFingerprint
        ) return false

        val syncId = "studio:${binding.operationId}"
        val metadata = database.syncMetadataQueries.selectById(syncId).executeAsOneOrNull()
            ?: return false
        if (metadata.synced != 0L || metadata.entityType != "event" ||
            !metadata.id.startsWith(STUDIO_SYNC_METADATA_PREFIX)
        ) return false
        if (metadata.retryState == "FAILED") {
            database.syncMetadataQueries.markStudioSyncReadyForDispatch(syncId, binding.eventId)
        } else if (metadata.retryState != "READY") {
            return false
        }
        if (triggerSync().isFailure) return false
        val refreshed = database.invitationExperienceQueries
            .selectOperationReceipt(binding.operationId, binding.eventId)
            .executeAsOneOrNull()
            ?: return false
        return refreshed.status == "COMMITTED" && !refreshed.server_receipt_id.isNullOrBlank()
    }

    /**
     * Get data for a specific change
     */
    private suspend fun getChangeData(table: String, recordId: String): String {
        return when (table) {
            "events" -> {
                val event = eventRepository.getEvent(recordId)
                if (event != null) {
                    val confirmedSlotId = database.confirmedDateQueries
                        .selectByEventId(recordId)
                        .executeAsOneOrNull()
                        ?.timeslotId
                        ?.let { persisted ->
                            TimeSlotStorageIdentity.logicalId(recordId, persisted)
                                ?: persisted.takeUnless { it.startsWith("slot:v1|") }
                        }
                    json.encodeToString(SyncEventData.serializer(), SyncEventData(
                        id = event.id,
                        title = event.title,
                        description = event.description,
                        organizerId = event.organizerId,
                        deadline = event.deadline,
                        timezone = event.proposedSlots.firstOrNull()?.timezone ?: "UTC",
                        status = event.status.name,
                        confirmedSlotId = confirmedSlotId,
                        finalDate = event.finalDate
                    ))
                } else {
                    "{}" // Fallback for deleted items
                }
            }
            "participants" -> {
                // For participants, we need to reconstruct the data
                // This is simplified - in practice we'd store the full data
                """{"eventId":"unknown","userId":"$recordId"}"""
            }
            "votes" -> {
                // For votes, we need to reconstruct the data
                // This is simplified - in practice we'd store the full data
                """{"eventId":"unknown","participantId":"$recordId","slotId":"unknown","preference":"YES"}"""
            }
            else -> "{}"
        }
    }

    /**
     * Confirmation metadata is written atomically with the local decision, but is intentionally
     * separate from the legacy user sync queue. Build an ordered server batch from that durable
     * record: the confirmed event decision first, then the one domain-effect envelope.
     */
    private suspend fun pendingConfirmationDecisionSyncs(): List<SyncChange> {
        val changes = mutableListOf<SyncChange>()
        val pendingMetadata = database.syncMetadataQueries.selectPending().executeAsList()

        pendingMetadata.forEach { metadata ->
            if (!metadata.id.startsWith(CONFIRMATION_SYNC_METADATA_PREFIX) ||
                metadata.entityType != CONFIRMATION_EVENT_ENTITY_TYPE ||
                metadata.operation != SyncOperation.UPDATE.name
            ) {
                return@forEach
            }

            val event = eventRepository.getEvent(metadata.entityId) ?: return@forEach
            val receipt = database.confirmationReceiptQueries
                .selectByEventId(event.id)
                .executeAsOneOrNull()
                ?: return@forEach
            val envelope = database.confirmationEffectOutboxQueries
                .selectByEventId(event.id)
                .executeAsOneOrNull()
                ?: return@forEach
            val confirmedDate = database.confirmedDateQueries
                .selectByEventId(event.id)
                .executeAsOneOrNull()
                ?: return@forEach
            val confirmedLogicalSlotId = TimeSlotStorageIdentity
                .logicalId(event.id, confirmedDate.timeslotId)
                ?: confirmedDate.timeslotId.takeUnless { it.startsWith("slot:v1|") }
                ?: return@forEach
            val envelopeLogicalSlotId = TimeSlotStorageIdentity
                .logicalId(event.id, envelope.slotId)
                ?: envelope.slotId.takeUnless { it.startsWith("slot:v1|") }
                ?: return@forEach
            val selectedSlot = event.proposedSlots.firstOrNull { it.id == confirmedLogicalSlotId }
                ?: return@forEach
            val finalDate = selectedSlot.start ?: return@forEach

            if (event.status != EventStatus.CONFIRMED ||
                receipt.slotId != selectedSlot.id ||
                envelopeLogicalSlotId != selectedSlot.id ||
                receipt.operationId != envelope.operationId
            ) {
                return@forEach
            }

            changes += SyncChange(
                id = metadata.id,
                table = "events",
                operation = SyncOperation.UPDATE.name,
                recordId = event.id,
                data = json.encodeToString(
                    SyncEventData.serializer(),
                    SyncEventData(
                        id = event.id,
                        title = event.title,
                        description = event.description,
                        organizerId = event.organizerId,
                        deadline = event.deadline,
                        timezone = selectedSlot.timezone,
                        status = EventStatus.CONFIRMED.name,
                        confirmedSlotId = selectedSlot.id,
                        finalDate = finalDate
                    )
                ),
                timestamp = metadata.timestamp,
                userId = receipt.actorId
            )
            changes += SyncChange(
                id = "confirmation-envelope-${envelope.domainEventId}",
                table = CONFIRMATION_EFFECT_OUTBOX_TABLE,
                operation = SyncOperation.CREATE.name,
                recordId = envelope.domainEventId,
                data = json.encodeToString(
                    ConfirmationEnvelopeSyncPayload.serializer(),
                    ConfirmationEnvelopeSyncPayload(
                        domainEventId = envelope.domainEventId,
                        effectKey = envelope.effectKey,
                        eventId = envelope.eventId,
                        slotId = envelopeLogicalSlotId,
                        operationId = envelope.operationId,
                        createdAt = envelope.createdAt
                    )
                ),
                timestamp = metadata.timestamp,
                userId = receipt.actorId
            )
        }

        return changes
    }

    /** One pending poll-ballot metadata row always produces one visible join projection. */
    fun getPendingBallotSyncJoinProjections(): List<PendingBallotSyncJoinProjection> =
        pendingBallotSyncs()

    private fun pendingBallotSyncs(): List<PendingBallotSyncJoinProjection> {
        val projections = mutableListOf<PendingBallotSyncJoinProjection>()
        val metadataRows = database.syncMetadataQueries.selectPending().executeAsList()
        for (metadata in metadataRows) {
            if (metadata.entityType != POLL_BALLOT_TABLE || metadata.operation != SyncOperation.UPDATE.name) {
                continue
            }
            val receipt = database.pollBallotReceiptQueries
                .selectByReceiptId(metadata.entityId)
                .executeAsOneOrNull()
            if (receipt == null) {
                projections += ballotSyncInconsistency(PendingBallotSyncJoinFailureCode.RECEIPT_MISSING)
                continue
            }
            if (receipt.syncStatus == "SERVER_ACKNOWLEDGED" || !receipt.serverReceiptId.isNullOrBlank()) {
                projections += ballotSyncInconsistency(
                    PendingBallotSyncJoinFailureCode.RECEIPT_ALREADY_ACKNOWLEDGED
                )
                continue
            }
            if (receipt.syncStatus != "LOCAL_PENDING") {
                projections += ballotSyncInconsistency(
                    PendingBallotSyncJoinFailureCode.RECEIPT_NOT_LOCAL_PENDING
                )
                continue
            }
            if (metadata.payload.isBlank() || metadata.payload == "{}") {
                projections += ballotSyncInconsistency(PendingBallotSyncJoinFailureCode.PAYLOAD_EMPTY)
                continue
            }
            val payload = try {
                json.decodeFromString(
                    PollBallotContract.BallotSyncPayload.serializer(),
                    metadata.payload
                )
            } catch (_: Exception) {
                projections += ballotSyncInconsistency(PendingBallotSyncJoinFailureCode.PAYLOAD_MALFORMED)
                continue
            }
            if (payload.localReceiptId != receipt.receiptId) {
                projections += ballotSyncInconsistency(
                    PendingBallotSyncJoinFailureCode.RECEIPT_ID_DIVERGENT
                )
                continue
            }
            val identity = payload.command.identity
            if (identity.eventId != receipt.eventId ||
                identity.actorId != receipt.actorId ||
                identity.pollRevision != receipt.pollRevision ||
                identity.operationId != receipt.operationId
            ) {
                projections += ballotSyncInconsistency(PendingBallotSyncJoinFailureCode.TUPLE_DIVERGENT)
                continue
            }
            if (payload.command.ballotFingerprint != receipt.ballotFingerprint ||
                payload.command.ballotFingerprint != PollBallotContract.fingerprint(payload.command.entries)
            ) {
                projections += ballotSyncInconsistency(
                    PendingBallotSyncJoinFailureCode.FINGERPRINT_DIVERGENT
                )
                continue
            }
            val operationKey = runCatching { PollBallotContract.operationKey(identity) }.getOrNull()
            if (operationKey == null || operationKey != receipt.operationKey) {
                projections += ballotSyncInconsistency(
                    PendingBallotSyncJoinFailureCode.OPERATION_KEY_DIVERGENT
                )
                continue
            }

            projections += PendingBallotSyncJoinProjection.Valid(
                SyncChange(
                    id = metadata.id,
                    table = POLL_BALLOT_TABLE,
                    operation = SyncOperation.UPDATE.name,
                    recordId = receipt.receiptId,
                    data = metadata.payload,
                    timestamp = metadata.timestamp,
                    userId = receipt.actorId
                )
            )
        }
        return projections
    }

    private fun ballotSyncInconsistency(
        diagnosticCode: PendingBallotSyncJoinFailureCode
    ): PendingBallotSyncJoinProjection.Inconsistent = PendingBallotSyncJoinProjection.Inconsistent(
        diagnosticCode = diagnosticCode,
        code = PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT,
        retryable = false,
        commitOutcome = PollBallotContract.CommitOutcome.UNKNOWN
    )

    /** Lossless Studio join: every pending metadata row is either transportable or visible as inconsistent. */
    fun getPendingStudioSyncJoinProjections(): List<PendingStudioSyncJoinProjection> {
        val projections = mutableListOf<PendingStudioSyncJoinProjection>()
        database.syncMetadataQueries.selectPending().executeAsList()
            .filter {
                it.entityType == "event" && it.id.startsWith(STUDIO_SYNC_METADATA_PREFIX)
            }
            .forEach { metadata ->
                val subject = try {
                    json.decodeFromString(StudioPendingSyncSubject.serializer(), metadata.payload)
                } catch (_: Exception) {
                    projections += PendingStudioSyncJoinProjection.Inconsistent(
                        metadata.id,
                        PendingStudioSyncJoinFailureCode.PAYLOAD_MALFORMED
                    )
                    return@forEach
                }
                val receipt = database.invitationExperienceQueries
                    .selectOperationReceiptByOperationId(subject.envelope.identity.operationId)
                    .executeAsOneOrNull()
                if (receipt == null) {
                    projections += PendingStudioSyncJoinProjection.Inconsistent(
                        metadata.id,
                        PendingStudioSyncJoinFailureCode.RECEIPT_MISSING
                    )
                    return@forEach
                }
                if (receipt.status != "PENDING_SYNC" || receipt.server_receipt_id != null) {
                    projections += PendingStudioSyncJoinProjection.Inconsistent(
                        metadata.id,
                        PendingStudioSyncJoinFailureCode.RECEIPT_NOT_PENDING
                    )
                    return@forEach
                }
                if (subject.schemaVersion != 1 || subject.eventId != metadata.entityId ||
                    subject.eventId != receipt.event_id ||
                    subject.committedRevision != receipt.aggregate_revision ||
                    subject.localReceiptId != receipt.operation_id ||
                    subject.expectedResultingArtwork != subject.envelope.expectedResultingArtwork
                ) {
                    projections += PendingStudioSyncJoinProjection.Inconsistent(
                        metadata.id,
                        PendingStudioSyncJoinFailureCode.SUBJECT_DIVERGENT
                    )
                    return@forEach
                }
                val persistedEnvelope = try {
                    json.decodeFromString(StudioCommitEnvelope.serializer(), receipt.commit_envelope)
                } catch (_: Exception) {
                    null
                }
                if (persistedEnvelope == null || persistedEnvelope != subject.envelope ||
                    !StudioCommitEnvelopeFactory.isValid(subject.envelope) ||
                    receipt.durable_operation_ref != subject.envelope.durableOperationRef ||
                    receipt.request_fingerprint != subject.envelope.requestFingerprint
                ) {
                    projections += PendingStudioSyncJoinProjection.Inconsistent(
                        metadata.id,
                        PendingStudioSyncJoinFailureCode.ENVELOPE_DIVERGENT
                    )
                    return@forEach
                }
                projections += PendingStudioSyncJoinProjection.Valid(
                    change = SyncChange(
                        id = metadata.id,
                        table = STUDIO_SYNC_ENTITY_TYPE,
                        operation = metadata.operation,
                        recordId = subject.localReceiptId,
                        data = metadata.payload,
                        timestamp = metadata.timestamp,
                        userId = subject.envelope.requestPayload.actorId
                    ),
                    subject = subject
                )
            }
        return projections
    }

    /**
     * Handle conflicts using CRDT-based merging
     */
    private suspend fun handleConflict(conflict: SyncConflict) {
        when (conflict.table) {
            "events" -> handleEventConflict(conflict)
            "participants" -> handleParticipantConflict(conflict)
            "votes" -> handleVoteConflict(conflict)
            else -> {
                // Fallback: mark as synced (client wins)
                userRepository.updateSyncStatus(
                    syncId = conflict.changeId,
                    synced = true,
                    retryCount = 0,
                    error = null
                )
            }
        }
    }

    private suspend fun handleEventConflict(conflict: SyncConflict) {
        val localEvent = eventRepository.getEvent(conflict.recordId)
        val serverEventJson = conflict.serverData ?: "{}"
        val serverEvent = try {
            json.decodeFromString(Event.serializer(), serverEventJson)
        } catch (e: Exception) { null }

        if (localEvent == null || serverEvent == null) {
            // Nothing to compare — fall back to marking synced
            userRepository.updateSyncStatus(conflict.changeId, true, 0, null)
            return
        }

        if (conflictResolutionEnabled) {
            // ── New path: detect + classify + log ─────────────────────────
            val summary = ConflictDetector.detect(localEvent, serverEvent)

            // 1. Persist audit log
            conflictLog.logSummary(summary)

            // 2. Auto-resolve non-critical fields
            val autoDecisions = ConflictDetector.autoResolveNonCritical(summary)
            if (autoDecisions.isNotEmpty()) {
                val autoMerged = ConflictDetector.applyDecisions(localEvent, autoDecisions)
                eventRepository.updateEvent(autoMerged)
            }

            // 3. Surface critical conflicts to the presentation layer
            if (summary.hasCritical) {
                onCriticalConflictsDetected?.invoke(summary)
                // For now: do NOT overwrite local for critical fields.
                // The user will resolve via the dialog and a follow-up sync.
                println("CONFLICT: ${summary.criticalConflicts.size} critical field(s) on event ${localEvent.id} — awaiting user resolution")
            } else {
                // All conflicts auto-resolved — mark synced
                userRepository.updateSyncStatus(conflict.changeId, true, 0, null)
            }
        } else {
            // ── Legacy path: last-write-wins (flag off) ───────────────────
            val mergedEvent = if (localEvent.updatedAt >= serverEvent.updatedAt) localEvent else serverEvent
            eventRepository.updateEvent(mergedEvent)
            userRepository.updateSyncStatus(conflict.changeId, true, 0, null)
        }
    }

    private suspend fun handleParticipantConflict(conflict: SyncConflict) {
        // For participants, use union (additive)
        // This is simplified - in practice we'd merge participant lists
        userRepository.updateSyncStatus(
            syncId = conflict.changeId,
            synced = true,
            retryCount = 0,
            error = null
        )
    }

    private suspend fun handleVoteConflict(conflict: SyncConflict) {
        // For votes, merge vote lists (union of preferences)
        // This is simplified
        userRepository.updateSyncStatus(
            syncId = conflict.changeId,
            synced = true,
            retryCount = 0,
            error = null
        )
    }

    /**
     * Merge two events using CRDT logic
     */
    private fun mergeEventsCRDT(local: Event, server: Event, serverTimestamp: String?): Event {
        // Parse timestamps (simplified - in practice use proper ISO parsing)
        val localTime = parseTimestamp(local.updatedAt)
        val serverTime = serverTimestamp?.let { parseTimestamp(it) } ?: 0L

        return Event(
            id = local.id,
            title = if (localTime >= serverTime) local.title else server.title,
            description = if (localTime >= serverTime) local.description else server.description,
            organizerId = local.organizerId, // Organizer doesn't change
            participants = (local.participants + server.participants).distinct(), // Union (G-Set)
            proposedSlots = if (localTime >= serverTime) local.proposedSlots else server.proposedSlots,
            status = if (localTime >= serverTime) local.status else server.status,
            finalDate = if (local.finalDate != null && server.finalDate != null) {
                if (localTime >= serverTime) local.finalDate else server.finalDate
            } else {
                local.finalDate ?: server.finalDate
            },
            deadline = if (localTime >= serverTime) local.deadline else server.deadline,
            createdAt = local.createdAt, // Creation time doesn't change
            updatedAt = if (localTime >= serverTime) local.updatedAt else server.updatedAt
        )
    }

    private fun parseTimestamp(isoString: String): Long {
        return try {
            Instant.parse(isoString).toEpochMilliseconds()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Update sync status for failed operations
     */
    private suspend fun updateSyncStatusForFailure(error: Exception) {
        val pendingChanges = userRepository.getPendingSyncChanges()
        pendingChanges.forEach { change ->
            val newRetryCount = change.retryCount + 1
            userRepository.updateSyncStatus(
                syncId = change.id,
                synced = false,
                retryCount = newRetryCount,
                error = syncFailureMessage()
            )
        }
    }

    /**
     * Clean up old sync metadata (older than 30 days)
     */
    suspend fun cleanupOldSyncData(): Result<Unit> = runCatching {
        val thirtyDaysAgo = Clock.System.now().minus(30 * 24, DateTimeUnit.HOUR).toString()
        userRepository.cleanupOldSyncMetadata(thirtyDaysAgo)
    }

    /**
     * Get current UTC timestamp as ISO 8601 string
     */
    private fun getCurrentUtcIsoString(): String = Clock.System.now().toString()

    /**
     * Get sync metrics for monitoring
     */
    fun getSyncMetrics(): SyncStats = metrics.getSyncStats()

    /**
     * Log current sync status for monitoring
     */
    fun logSyncStatus() {
        val stats = metrics.getSyncStats()
        println("Sync Stats: total=${stats.totalSyncs}, success=${stats.successfulSyncs}, failed=${stats.failedSyncs}, avgDuration=${stats.averageDurationMs}ms, conflicts=${stats.totalConflictsResolved}")
    }

    /**
     * Performance monitoring: check if sync is taking too long
     */
    private fun checkPerformance(durationMs: Long) {
        if (durationMs > 30000) { // 30 seconds
            println("PERFORMANCE ALERT: Sync took ${durationMs}ms, which is longer than expected")
        }
    }

    private suspend fun updateLocalSyncStatus(response: SyncResponse) {
        // Mark all pending changes as synced if the sync was successful
        if (response.success) {
            val pendingChanges = userRepository.getPendingSyncChanges()
            pendingChanges.filterNot { change ->
                change.tableName == POLL_BALLOT_TABLE || change.tableName == "poll_ballots" ||
                change.id.startsWith(STUDIO_SYNC_METADATA_PREFIX) ||
                    change.id.startsWith(CONFIRMATION_SYNC_METADATA_PREFIX)
            }.forEach { change ->
                userRepository.updateSyncStatus(
                    syncId = change.id,
                    synced = true,
                    retryCount = 0,
                    error = null
                )
            }
            markAcknowledgedConfirmationsSynced(response)
        }
        markRejectedStudioCommitsTerminal(response)
        markAcknowledgedBallotsSynced(response)
        markAcknowledgedStudioCommitsSynced(response)
    }

    /** A typed non-retryable Studio rejection is terminal only after exact durable correlation. */
    private fun markRejectedStudioCommitsTerminal(response: SyncResponse) {
        response.conflicts.forEach { conflict ->
            if (conflict.table != STUDIO_SYNC_ENTITY_TYPE ||
                conflict.resolution != "REJECTED" ||
                conflict.retryable != false ||
                conflict.code !in STUDIO_TERMINAL_REJECTION_CODES
            ) return@forEach
            val metadata = database.syncMetadataQueries.selectById(conflict.changeId)
                .executeAsOneOrNull()
                ?: return@forEach
            if (metadata.id != "studio:${conflict.recordId}" ||
                metadata.entityType != "event" ||
                metadata.synced != 0L ||
                metadata.retryState != "READY" ||
                metadata.payload != conflict.clientData
            ) return@forEach
            val subject = try {
                json.decodeFromString(StudioPendingSyncSubject.serializer(), metadata.payload)
            } catch (_: Exception) {
                return@forEach
            }
            val receipt = database.invitationExperienceQueries
                .selectOperationReceiptByOperationId(conflict.recordId)
                .executeAsOneOrNull()
                ?: return@forEach
            val envelope = try {
                json.decodeFromString(StudioCommitEnvelope.serializer(), receipt.commit_envelope)
            } catch (_: Exception) {
                return@forEach
            }
            if (subject.localReceiptId != conflict.recordId ||
                subject.eventId != metadata.entityId ||
                subject.committedRevision != receipt.aggregate_revision ||
                subject.envelope != envelope ||
                subject.expectedResultingArtwork != envelope.expectedResultingArtwork ||
                !StudioCommitEnvelopeFactory.isValid(envelope) ||
                receipt.event_id != subject.eventId ||
                receipt.status != "PENDING_SYNC" ||
                receipt.server_receipt_id != null ||
                receipt.durable_operation_ref != envelope.durableOperationRef ||
                receipt.request_fingerprint != envelope.requestFingerprint
            ) return@forEach

            database.transaction {
                database.invitationExperienceQueries.markStudioOperationReceiptTerminalFailure(
                    updated_at = getCurrentUtcIsoString(),
                    operation_id = receipt.operation_id,
                    event_id = receipt.event_id,
                    aggregate_revision = receipt.aggregate_revision,
                    durable_operation_ref = receipt.durable_operation_ref,
                    request_fingerprint = receipt.request_fingerprint
                )
                database.syncMetadataQueries.markStudioSyncPermanentFailure(
                    rejectionCode = conflict.code,
                    id = metadata.id,
                    eventId = metadata.entityId
                )
            }
        }
    }

    /** A generic successful batch can never advance a ballot receipt. */
    private fun markAcknowledgedBallotsSynced(response: SyncResponse) {
        val conflictedReceiptIds = response.conflicts
            .asSequence()
            .filter { it.table == POLL_BALLOT_TABLE || it.table == "poll_ballots" }
            .map { it.recordId }
            .toSet()

        response.ballotAcknowledgements.forEach { acknowledgement ->
            if (acknowledgement.localReceiptId.isBlank() ||
                acknowledgement.serverReceiptId.isBlank() ||
                acknowledgement.localReceiptId in conflictedReceiptIds
            ) return@forEach
            val receipt = database.pollBallotReceiptQueries
                .selectByReceiptId(acknowledgement.localReceiptId)
                .executeAsOneOrNull()
                ?: return@forEach
            val payload = try {
                json.decodeFromString(
                    PollBallotContract.BallotSyncPayload.serializer(),
                    receipt.syncPayload
                )
            } catch (_: Exception) {
                return@forEach
            }
            if (receipt.syncStatus != "LOCAL_PENDING" ||
                payload.localReceiptId != acknowledgement.localReceiptId ||
                payload.command.identity != acknowledgement.identity ||
                payload.command.ballotFingerprint != acknowledgement.ballotFingerprint ||
                receipt.ballotFingerprint != acknowledgement.ballotFingerprint
            ) return@forEach

            database.transaction {
                database.pollBallotReceiptQueries.markSynced(
                    serverReceiptId = acknowledgement.serverReceiptId,
                    receiptId = acknowledgement.localReceiptId
                )
                database.syncMetadataQueries.selectPending()
                    .executeAsList()
                    .filter {
                        it.entityType == POLL_BALLOT_TABLE &&
                            it.entityId == acknowledgement.localReceiptId
                    }
                    .forEach { database.syncMetadataQueries.markSynced(it.id) }
            }
        }
    }

    /** Only the exact receipt/envelope tuple can turn a local Studio commit into COMMITTED. */
    private fun markAcknowledgedStudioCommitsSynced(response: SyncResponse) {
        response.studioAcknowledgements.forEach { acknowledgement ->
            if (acknowledgement.localReceiptId.isBlank() || acknowledgement.serverReceiptId.isBlank()) {
                return@forEach
            }
            val receipt = database.invitationExperienceQueries
                .selectOperationReceiptByOperationId(acknowledgement.localReceiptId)
                .executeAsOneOrNull()
                ?: return@forEach
            val envelope = try {
                json.decodeFromString(StudioCommitEnvelope.serializer(), receipt.commit_envelope)
            } catch (_: Exception) {
                return@forEach
            }
            val metadata = database.syncMetadataQueries
                .selectById("studio:${receipt.operation_id}")
                .executeAsOneOrNull()
                ?: return@forEach
            val subject = try {
                json.decodeFromString(StudioPendingSyncSubject.serializer(), metadata.payload)
            } catch (_: Exception) {
                return@forEach
            }
            val expectedDisposition = when (envelope.requestPayload.subject) {
                is com.guyghost.wakeve.invitationexperience.StudioCommitSubject.New ->
                    com.guyghost.wakeve.invitationexperience.StudioCommitDisposition.CREATED
                is com.guyghost.wakeve.invitationexperience.StudioCommitSubject.EditExisting ->
                    com.guyghost.wakeve.invitationexperience.StudioCommitDisposition.UPDATED
            }
            if (receipt.status != "PENDING_SYNC" || receipt.server_receipt_id != null ||
                metadata.synced != 0L || metadata.entityType != "event" ||
                !metadata.id.startsWith(STUDIO_SYNC_METADATA_PREFIX) ||
                !StudioCommitEnvelopeFactory.isValid(envelope) ||
                subject.localReceiptId != acknowledgement.localReceiptId ||
                subject.eventId != acknowledgement.eventId ||
                subject.committedRevision != acknowledgement.committedRevision ||
                subject.envelope != envelope ||
                subject.expectedResultingArtwork != envelope.expectedResultingArtwork ||
                acknowledgement.eventId != receipt.event_id ||
                acknowledgement.committedRevision != receipt.aggregate_revision ||
                acknowledgement.durableOperationRef != receipt.durable_operation_ref ||
                acknowledgement.requestFingerprint != receipt.request_fingerprint ||
                acknowledgement.durableOperationRef != envelope.durableOperationRef ||
                acknowledgement.requestFingerprint != envelope.requestFingerprint ||
                acknowledgement.outcome !in setOf(
                    com.guyghost.wakeve.invitationexperience.StudioSyncOutcome.APPLIED,
                    com.guyghost.wakeve.invitationexperience.StudioSyncOutcome.ALREADY_APPLIED
                ) ||
                acknowledgement.disposition != expectedDisposition ||
                acknowledgement.artwork != subject.expectedResultingArtwork
            ) return@forEach

            database.transaction {
                database.invitationExperienceQueries.acknowledgeStudioOperationReceipt(
                    server_receipt_id = acknowledgement.serverReceiptId,
                    updated_at = getCurrentUtcIsoString(),
                    operation_id = receipt.operation_id,
                    event_id = receipt.event_id,
                    aggregate_revision = receipt.aggregate_revision,
                    durable_operation_ref = receipt.durable_operation_ref,
                    request_fingerprint = receipt.request_fingerprint
                )
                database.syncMetadataQueries.markSynced(metadata.id)
            }
        }
    }

    /**
     * Generic batch success does not acknowledge a confirmation. The acknowledgement has to
     * name the durable local envelope and its operation, and a conflict for that envelope
     * leaves the local decision retryable.
     */
    private suspend fun markAcknowledgedConfirmationsSynced(response: SyncResponse) {
        val conflictingEnvelopeIds = response.conflicts
            .asSequence()
            .filter { it.table == CONFIRMATION_EFFECT_OUTBOX_TABLE }
            .map { it.recordId }
            .toSet()

        response.confirmationAcknowledgements.forEach { acknowledgement ->
            if (acknowledgement.domainEventId in conflictingEnvelopeIds || acknowledgement.receiptId.isBlank()) {
                return@forEach
            }
            val envelope = database.confirmationEffectOutboxQueries
                .selectByDomainEventId(acknowledgement.domainEventId)
                .executeAsOneOrNull()
                ?: return@forEach
            if (envelope.effectKey != acknowledgement.effectKey ||
                envelope.operationId != acknowledgement.operationId
            ) {
                return@forEach
            }
            val receipt = database.confirmationReceiptQueries
                .selectByOperationId(acknowledgement.operationId)
                .executeAsOneOrNull()
                ?: return@forEach
            val envelopeLogicalSlotId = TimeSlotStorageIdentity
                .logicalId(envelope.eventId, envelope.slotId)
                ?: envelope.slotId.takeUnless { it.startsWith("slot:v1|") }
                ?: return@forEach
            if (receipt.eventId != envelope.eventId || receipt.slotId != envelopeLogicalSlotId) return@forEach

            eventRepository.markConfirmationSynced(receipt.operationId)
        }
    }

    /**
     * Perform sync with automatic token refresh on 401 errors
     */
    private suspend fun performSyncWithTokenRefresh(): SyncResponse {
        try {
            return performSync()
        } catch (e: UnauthorizedException) {
            // Token expired or invalid, try to refresh it
            if (authTokenRefreshProvider != null) {
                println("AUTH: Token expired (401), attempting to refresh...")
                val newToken = authTokenRefreshProvider.invoke()

                if (newToken != null) {
                    println("AUTH: Token refreshed successfully, retrying sync...")
                    // Retry sync with new token
                    return performSync()
                } else {
                    println("AUTH: Token refresh failed, no new token available")
                    throw UnauthorizedException("Token refresh failed")
                }
            } else {
                // No token refresh provider, cannot recover from 401
                println("AUTH: No token refresh provider configured")
                throw e
            }
        }
    }

    /**
     * Perform actual sync operation (extracted from triggerSync)
     */
    private suspend fun performSync(): SyncResponse {
        val startTime = getCurrentTimeMillis()
        metrics.recordSyncStart()

        if (!networkDetector.isNetworkAvailable.value) {
            throw IllegalStateException("Network not available")
        }

        val authToken = authTokenProvider() ?: throw IllegalStateException("No auth token available")

        if (hasPendingChanges()) {
            _syncStatus.value = SyncStatus.Syncing
        }

        replayPendingSideEffects()

        val pendingChanges = getPendingChangesForSync()
        if (pendingChanges.isEmpty()) {
            _syncStatus.value = SyncStatus.Idle
            val duration = getCurrentTimeMillis() - startTime
            metrics.recordSyncSuccess(duration, 0)
            return SyncResponse(
                success = true,
                appliedChanges = 0,
                conflicts = emptyList(),
                serverTimestamp = getCurrentUtcIsoString(),
                message = "No changes to sync"
            )
        }

        val syncRequest = SyncRequest(
            changes = pendingChanges,
            lastSyncTimestamp = lastSuccessfulSyncTimestamp
        )

        // Make actual HTTP call to server
        val requestJson = json.encodeToString(SyncRequest.serializer(), syncRequest)
        val responseJson = httpClient.sync(requestJson, authToken).getOrThrow()
        val response = json.decodeFromString(SyncResponse.serializer(), responseJson)

        // Update local sync status based on response
        updateLocalSyncStatus(response)

        // Handle conflicts with CRDT-based merging
        for (conflict in response.conflicts) {
            handleConflict(conflict)
            metrics.recordConflictResolved(conflict.table, "CRDT")
        }

        // Alert if high conflict rate
        if (response.conflicts.size > 5) {
            alertManager.alertHighConflictRate(response.conflicts.size)
        }

        _syncStatus.value = if (response.success) SyncStatus.Idle else SyncStatus.Error(syncFailureMessage())

        val duration = getCurrentTimeMillis() - startTime
        if (response.success) {
            // Update incremental sync timestamp so next sync only fetches new changes
            lastSuccessfulSyncTimestamp = response.serverTimestamp
            metrics.recordSyncSuccess(duration, response.appliedChanges)
        } else {
            metrics.recordSyncFailure(duration, syncFailureMessage())
        }

        checkPerformance(duration)

        return response
    }

    private suspend fun replayPendingSideEffects(): Int {
        var replayed = 0
        pendingSideEffectReplayers.forEach { replayer ->
            if (replayer.hasPending()) {
                replayed += replayer.replayPending().getOrThrow()
            }
        }
        return replayed
    }

    /**
     * Perform sync with retry mechanism and exponential backoff
     */
    private suspend fun syncWithRetry(): Result<SyncResponse> {
        var lastException: Exception? = null

        for (attempt in 0..maxRetries) {
            try {
                return Result.success(performSyncWithTokenRefresh())
            } catch (e: Exception) {
                lastException = e
                _syncStatus.value = SyncStatus.Error(syncRetryFailureMessage(attempt + 1, maxRetries + 1))

                if (attempt < maxRetries) {
                    // Calculate exponential backoff delay: baseDelay * 2^attempt
                    val delayMs = baseRetryDelayMs * (1L shl attempt) // 2^attempt
                    delay(delayMs)
                } else {
                    // All retries failed, update sync status for failed changes
                    updateSyncStatusForFailure(e)
                    alertManager.alertSyncFailure(syncFailureMessage(), maxRetries)
                }
            }
        }

        // All retries failed
        return Result.failure(lastException ?: Exception("Sync failed after $maxRetries retries"))
    }

    /**
     * Schedule automatic retry for failed changes
     */
    fun scheduleRetryForFailedChanges() {
        scope.launch {
            while (isActive) {
                delay(30000L) // Check every 30 seconds

                if (networkDetector.isNetworkAvailable.value) {
                    val failedChanges = userRepository.getPendingSyncChanges()
                        .filter { it.retryCount < maxRetries && it.lastError != null }

                    if (failedChanges.isNotEmpty()) {
                        // Trigger sync to retry failed changes
                        triggerSync()
                    }
                }
            }
        }
    }

    /**
     * Clean up resources
     */
    fun dispose() {
        scope.cancel()
    }
}

@Serializable
private data class ConfirmationEnvelopeSyncPayload(
    val domainEventId: String,
    val effectKey: String,
    val eventId: String,
    val slotId: String,
    val operationId: String,
    val createdAt: String
)

private const val CONFIRMATION_EFFECT_OUTBOX_TABLE = "confirmation_effect_outbox"
private const val CONFIRMATION_EVENT_ENTITY_TYPE = "event"
private const val CONFIRMATION_SYNC_METADATA_PREFIX = "sync_confirm_"
private const val POLL_BALLOT_TABLE = "poll_ballot"
private const val STUDIO_SYNC_ENTITY_TYPE = "studio_commit"
private const val STUDIO_SYNC_METADATA_PREFIX = "studio:"
private val STUDIO_TERMINAL_REJECTION_CODES = setOf(
    "FORBIDDEN",
    "EVENT_NOT_DRAFT",
    "STALE_BASE_REVISION",
    "IDEMPOTENCY_CONFLICT",
    "REPOSITORY_INCONSISTENT"
)

internal fun syncFailureMessage(): String =
    "Sync failed. Please retry when your connection is stable."

internal fun syncRetryFailureMessage(attempt: Int, totalAttempts: Int): String =
    "Sync failed (attempt $attempt/$totalAttempts). Please retry when your connection is stable."
