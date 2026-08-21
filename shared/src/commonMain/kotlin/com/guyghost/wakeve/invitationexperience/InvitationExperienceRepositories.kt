package com.guyghost.wakeve.invitationexperience

import com.guyghost.wakeve.access.ParticipantAccessMapper
import com.guyghost.wakeve.access.ParticipantAccessState
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.repository.DatabaseEventRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.days

data class ArchiveSnapshot(
    val event: Event,
    val artwork: Artwork,
    val temporalClass: TemporalClass,
    val interactionPolicy: InteractionPolicy,
    val syncState: LibrarySyncState,
    val warning: LibrarySyncWarning?,
    val settledSummary: List<String>
)

sealed interface ArchiveLoadState {
    data object Idle : ArchiveLoadState
    data class Loading(
        val previousStableState: PreviousStableState<ArchiveSnapshot>
    ) : ArchiveLoadState
    data class Ready(
        val snapshot: ArchiveSnapshot,
        val freshness: Freshness
    ) : ArchiveLoadState
    data object Empty : ArchiveLoadState
    data class Failed(
        val error: InvitationExperienceError,
        val previousStableState: PreviousStableState<ArchiveSnapshot>
    ) : ArchiveLoadState
}

interface InvitationExperienceProjectionRepository {
    fun artwork(eventId: String): Artwork?

    suspend fun library(
        viewerId: String,
        projection: LibraryProjection,
        now: Instant
    ): LibraryLoadState<List<LibraryCardProjection>>

    suspend fun archive(
        eventId: String,
        viewerId: String,
        now: Instant
    ): ArchiveLoadState
}

class DatabaseInvitationExperienceProjectionRepository(
    private val database: WakeveDb
) : InvitationExperienceProjectionRepository {
    private val eventRepository = DatabaseEventRepository(database)
    private val projector = EventLibraryProjector()

    override fun artwork(eventId: String): Artwork? {
        if (database.eventQueries.selectById(eventId).executeAsOneOrNull() == null) return null
        return database.invitationExperienceQueries.selectArtworkByEventId(eventId)
            .executeAsOneOrNull()
            ?.toArtwork()
            ?: Artwork.None
    }

    override suspend fun library(
        viewerId: String,
        projection: LibraryProjection,
        now: Instant
    ): LibraryLoadState<List<LibraryCardProjection>> = try {
        val cards = eventRepository.getAllEvents().mapNotNull { event ->
            val participant = eventRepository.getParticipantRecords(event.id)
                ?.firstOrNull { it.userId == viewerId }
            val isOrganizer = event.organizerId == viewerId
            val access = if (isOrganizer) {
                ParticipantAccessState.organizer(viewerId)
            } else {
                participant?.let(ParticipantAccessMapper::fromRepositoryRecord)
                    ?: ParticipantAccessState.nonMember(viewerId)
            }
            val viewerRole = when {
                access.role == ParticipantAccessState.Role.ORGANIZER -> ViewerRole.ORGANIZER
                access.role == ParticipantAccessState.Role.MEMBER -> ViewerRole.MEMBER
                else -> ViewerRole.NON_MEMBER
            }
            val membership = if (access.role == ParticipantAccessState.Role.MEMBER && participant != null) {
                MembershipState.ActiveMember(participant.id)
            } else {
                MembershipState.NonMember
            }
            val rsvp = when {
                isOrganizer -> RsvpState.ACCEPTED
                access.role != ParticipantAccessState.Role.MEMBER -> RsvpState.NOT_APPLICABLE
                access.rsvp == com.guyghost.wakeve.access.ParticipantRsvp.ACCEPTED -> RsvpState.ACCEPTED
                access.rsvp == com.guyghost.wakeve.access.ParticipantRsvp.DECLINED -> RsvpState.DECLINED
                access.rsvp == com.guyghost.wakeve.access.ParticipantRsvp.PENDING -> RsvpState.PENDING
                access.rsvp == com.guyghost.wakeve.access.ParticipantRsvp.NOT_APPLICABLE ->
                    RsvpState.NOT_APPLICABLE
                else -> RsvpState.UNAVAILABLE
            }
            val artwork = database.invitationExperienceQueries.selectArtworkByEventId(event.id)
                .executeAsOneOrNull()
                ?.toArtwork()
                ?: Artwork.None
            projector.project(
                input = LibraryEventInput(
                    event = event,
                    viewerId = viewerId,
                    viewerRole = viewerRole,
                    membershipState = membership,
                    rsvpState = rsvp,
                    interactiveNextAction = interactiveNextAction(
                        status = event.status,
                        viewerRole = viewerRole,
                        rsvpState = rsvp
                    ),
                    syncState = syncState(event.id),
                    archiveActive = event.status == EventStatus.FINALIZED,
                    artwork = artwork
                ),
                now = now
            )
        }
        val filtered = projector.filter(cards, projection)
        if (filtered.isEmpty()) {
            LibraryLoadState.Empty(projection)
        } else {
            LibraryLoadState.Ready(filtered, Freshness.Current)
        }
    } catch (_: Exception) {
        LibraryLoadState.Failed(
            error = InvitationExperienceError.REPOSITORY_UNAVAILABLE,
            previousStableState = PreviousStableState.Idle
        )
    }

    override suspend fun archive(
        eventId: String,
        viewerId: String,
        now: Instant
    ): ArchiveLoadState {
        return try {
            val event = eventRepository.getEvent(eventId) ?: return ArchiveLoadState.Empty
            val canRead = event.organizerId == viewerId ||
                eventRepository.getParticipantRecords(eventId).orEmpty()
                    .firstOrNull { it.userId == viewerId }
                    ?.let(ParticipantAccessMapper::fromRepositoryRecord)
                    ?.role == ParticipantAccessState.Role.MEMBER
            if (!canRead) return ArchiveLoadState.Empty

            val temporalClass = EventTemporalClassifier.classify(event, now)
            if (event.status != EventStatus.FINALIZED && temporalClass != TemporalClass.PAST) {
                return ArchiveLoadState.Empty
            }
            val artwork = database.invitationExperienceQueries.selectArtworkByEventId(eventId)
                .executeAsOneOrNull()
                ?.toArtwork()
                ?: Artwork.None
            val sync = syncState(eventId)
            val settledSummary = database.confirmedDateQueries.selectWithTimeslotDetails(eventId)
                .executeAsOneOrNull()
                ?.let { confirmed ->
                    listOfNotNull(
                        confirmed.startTime,
                        confirmed.endTime,
                        confirmed.timezone
                    )
                }
                .orEmpty()
            ArchiveLoadState.Ready(
                snapshot = ArchiveSnapshot(
                    event = event,
                    artwork = artwork,
                    temporalClass = temporalClass,
                    interactionPolicy = InteractionPolicy.READ_ONLY,
                    syncState = sync,
                    warning = sync.warning(),
                    settledSummary = settledSummary
                ),
                freshness = Freshness.Current
            )
        } catch (_: Exception) {
            ArchiveLoadState.Failed(
                error = InvitationExperienceError.REPOSITORY_UNAVAILABLE,
                previousStableState = PreviousStableState.Idle
            )
        }
    }

    private fun interactiveNextAction(
        status: EventStatus,
        viewerRole: ViewerRole,
        rsvpState: RsvpState
    ): LibraryNextAction? = when (status) {
        EventStatus.DRAFT -> null
        EventStatus.POLLING -> when {
            viewerRole == ViewerRole.ORGANIZER -> LibraryNextAction.VIEW_POLL_RESULTS
            rsvpState == RsvpState.ACCEPTED -> LibraryNextAction.SUBMIT_VOTE
            else -> LibraryNextAction.VIEW_EVENT
        }
        EventStatus.COMPARING -> LibraryNextAction.COMPARE_OPTIONS
        EventStatus.CONFIRMED -> if (viewerRole == ViewerRole.ORGANIZER) {
            LibraryNextAction.CONTINUE_ORGANIZATION
        } else {
            LibraryNextAction.VIEW_EVENT
        }
        EventStatus.ORGANIZING -> LibraryNextAction.VIEW_EVENT
        EventStatus.FINALIZED -> LibraryNextAction.VIEW_ARCHIVE
    }

    private fun syncState(eventId: String): LibrarySyncState {
        val records = database.syncMetadataQueries.selectByEntity("event", eventId).executeAsList()
        val pending = records.firstOrNull { it.synced == 0L }
            ?: return if (records.isEmpty()) LibrarySyncState.Unavailable else LibrarySyncState.Synced
        return when (pending.retryState) {
            "CONFLICT" -> LibrarySyncState.Conflict(pending.id)
            "PERMANENT_FAILURE" -> LibrarySyncState.PermanentFailure(pending.id)
            else -> LibrarySyncState.Pending(pending.id)
        }
    }
}

class DatabaseDirectInviteBatchRepository(
    private val database: WakeveDb,
    private val deliveryTransport: DirectInviteDeliveryTransport?
) : DirectInviteBatchUseCase {
    private val policy = DefaultDirectInviteBatchUseCase()

    constructor(database: WakeveDb) : this(database, null)

    override suspend fun submit(
        command: SubmitDirectInviteBatchCommand
    ): DirectInviteOperation = forbidden(command)

    suspend fun submit(
        command: SubmitDirectInviteBatchCommand,
        deliveryEnvelopes: Set<DirectInviteDeliveryEnvelope>
    ): DirectInviteOperation {
        val proposed = policy.submit(command)
        if (proposed !is DirectInviteOperation.Submitting) return proposed
        val capability = command.capability as? DirectInviteCapability.Ready
            ?: return forbidden(command)
        val binding = DirectInviteDeliveryBinding(
            eventId = command.eventId,
            actorId = command.actorId,
            accessRevision = capability.accessRevision,
            batchId = command.batchId,
            operationId = command.operationId
        )
        val now = Clock.System.now()
        if (!deliveryEnvelopes.areValidFor(binding, command.recipientKeys, now)) {
            return forbidden(command)
        }

        val existing = database.invitationExperienceQueries
            .selectDirectInviteBatchByOperationId(command.operationId)
            .executeAsOneOrNull()
        if (existing != null) {
            return if (
                existing.batch_id == command.batchId &&
                existing.event_id == command.eventId &&
                existing.actor_id == command.actorId &&
                existing.access_revision == capability.accessRevision
            ) {
                loadPersisted(command.batchId) ?: forbidden(command)
            } else {
                forbidden(command)
            }
        }

        val pending: DirectInviteOperation = try {
            database.transactionWithResult {
                val event = database.eventQueries.selectById(command.eventId).executeAsOneOrNull()
                    ?: return@transactionWithResult forbidden(command)
                val domainEvent = DatabaseEventRepository(database).getEvent(command.eventId)
                    ?: return@transactionWithResult forbidden(command)
                if (
                    event.organizerId != command.actorId ||
                    event.status != EventStatus.DRAFT.name ||
                    command.eventStatus != EventStatus.DRAFT ||
                    EventStatus.DRAFT !in capability.allowedEventStatuses ||
                    event.aggregateSchemaVersion != 1L ||
                    event.aggregateRevision != capability.accessRevision ||
                    EventTemporalClassifier.classify(domainEvent, now) == TemporalClass.PAST
                ) {
                    return@transactionWithResult forbidden(command)
                }

                val timestamp = now.toString()
                val retentionExpiry = deliveryEnvelopes.minOf { it.expiresAt }
                database.invitationExperienceQueries.insertDirectInviteBatch(
                    batch_id = command.batchId,
                    event_id = command.eventId,
                    actor_id = command.actorId,
                    operation_id = command.operationId,
                    access_revision = capability.accessRevision,
                    status = DIRECT_INVITE_PENDING,
                    created_at = timestamp,
                    updated_at = timestamp,
                    expires_at = retentionExpiry
                )
                deliveryEnvelopes.forEach { envelope ->
                    database.invitationExperienceQueries.insertDirectInviteRecipientOutcome(
                        batch_id = command.batchId,
                        recipient_key = envelope.recipientKey.value,
                        key_version = envelope.keyVersion.toLong(),
                        status = RECIPIENT_QUEUED,
                        invitation_id = null,
                        reason_code = null,
                        expires_at = envelope.expiresAt,
                        updated_at = timestamp
                    )
                    database.invitationExperienceQueries.insertDirectInviteDeliveryEnvelope(
                        batch_id = command.batchId,
                        recipient_key = envelope.recipientKey.value,
                        ciphertext = envelope.ciphertext,
                        key_version = envelope.keyVersion.toLong(),
                        expires_at = envelope.expiresAt,
                        transport_state = ENVELOPE_QUEUED
                    )
                }
                database.syncMetadataQueries.insertSyncMetadata(
                    id = "direct-invite:${command.operationId}",
                    entityType = "direct_invite_batch",
                    entityId = command.batchId,
                    operation = "CREATE",
                    timestamp = timestamp,
                    synced = 0L
                )
                DirectInviteOperation.PendingSync(
                    batchId = command.batchId,
                    operationId = command.operationId,
                    recipientKeys = command.recipientKeys
                )
            }
        } catch (_: Exception) {
            forbidden(command, InvitationExperienceError.REPOSITORY_UNAVAILABLE)
        }
        if (pending !is DirectInviteOperation.PendingSync) return pending
        return dispatch(
            operation = pending,
            binding = binding,
            envelopes = deliveryEnvelopes,
            capability = capability
        )
    }

    override suspend fun retry(
        command: RetryDirectInviteBatchCommand
    ): DirectInviteOperation {
        val proposed = policy.retry(command)
        if (proposed !is DirectInviteOperation.Submitting) return proposed

        val plan: DirectInviteDispatchPlan? = try {
            database.transactionWithResult {
                val nowInstant = Clock.System.now()
                database.invitationExperienceQueries
                    .deleteExpiredDirectInviteRecipientOutcomes(nowInstant.toString())
                val batch = database.invitationExperienceQueries
                    .selectDirectInviteBatch(command.operation.batchId)
                    .executeAsOneOrNull()
                    ?: return@transactionWithResult null
                val event = database.eventQueries.selectById(batch.event_id).executeAsOneOrNull()
                    ?: return@transactionWithResult null
                val domainEvent = DatabaseEventRepository(database).getEvent(batch.event_id)
                    ?: return@transactionWithResult null
                val capability = command.capability as? DirectInviteCapability.Ready
                    ?: return@transactionWithResult null
                if (
                    batch.expires_at.isExpiredAt(nowInstant) ||
                    batch.operation_id != command.operation.operationId ||
                    batch.event_id != capability.eventId ||
                    batch.actor_id != capability.actorId ||
                    batch.access_revision != capability.accessRevision ||
                    event.organizerId != capability.actorId ||
                    event.status != EventStatus.DRAFT.name ||
                    EventStatus.DRAFT !in capability.allowedEventStatuses ||
                    event.aggregateSchemaVersion != 1L ||
                    event.aggregateRevision != capability.accessRevision ||
                    EventTemporalClassifier.classify(domainEvent, nowInstant) == TemporalClass.PAST
                ) {
                    return@transactionWithResult null
                }

                val rows = database.invitationExperienceQueries
                    .selectDirectInviteRecipientOutcomes(batch.batch_id)
                    .executeAsList()
                val persistedRetryableKeys = rows.mapNotNullTo(linkedSetOf()) { row ->
                    val outcome = row.toRecipientOutcome()
                    val failed = outcome as? DirectInviteRecipientOutcome.Failed
                    if (row.status == "FAILED" && failed?.error.isRetryableDeliveryError()) {
                        RecipientKey(row.recipient_key)
                    } else {
                        null
                    }
                }
                if (
                    persistedRetryableKeys.isEmpty() ||
                    proposed.recipientKeys != persistedRetryableKeys
                ) {
                    return@transactionWithResult null
                }

                val envelopes = database.invitationExperienceQueries
                    .selectDirectInviteDeliveryEnvelopes(batch.batch_id)
                    .executeAsList()
                    .filter { row -> RecipientKey(row.recipient_key) in persistedRetryableKeys }
                    .mapTo(linkedSetOf()) { row -> row.toDeliveryEnvelope(batch) }
                val binding = DirectInviteDeliveryBinding(
                    eventId = batch.event_id,
                    actorId = batch.actor_id,
                    accessRevision = batch.access_revision,
                    batchId = batch.batch_id,
                    operationId = batch.operation_id
                )
                if (!envelopes.areValidFor(binding, persistedRetryableKeys, nowInstant)) {
                    return@transactionWithResult null
                }

                val now = nowInstant.toString()
                database.invitationExperienceQueries.updateDirectInviteBatchStatus(
                    status = DIRECT_INVITE_PENDING,
                    updated_at = now,
                    batch_id = batch.batch_id,
                    operation_id = batch.operation_id
                )
                persistedRetryableKeys.forEach { key ->
                    database.invitationExperienceQueries.updateDirectInviteRecipientOutcome(
                        status = RECIPIENT_QUEUED,
                        invitation_id = null,
                        reason_code = null,
                        updated_at = now,
                        batch_id = batch.batch_id,
                        recipient_key = key.value
                    )
                    database.invitationExperienceQueries.updateDirectInviteDeliveryEnvelopeState(
                        transport_state = ENVELOPE_QUEUED,
                        batch_id = batch.batch_id,
                        recipient_key = key.value
                    )
                }
                DirectInviteDispatchPlan(
                    operation = DirectInviteOperation.PendingSync(
                        batchId = batch.batch_id,
                        operationId = batch.operation_id,
                        recipientKeys = persistedRetryableKeys
                    ),
                    binding = binding,
                    envelopes = envelopes,
                    capability = capability
                )
            }
        } catch (_: Exception) {
            null
        }
        return plan?.let {
            dispatch(it.operation, it.binding, it.envelopes, it.capability)
        } ?: command.operation
    }

    override suspend fun cancel(
        command: CancelDirectInviteBatchCommand
    ): DirectInviteOperation {
        val batchId = when (val operation = command.operation) {
            is DirectInviteOperation.Submitting -> operation.batchId
            is DirectInviteOperation.PendingSync -> operation.batchId
            is DirectInviteOperation.Failed -> operation.batchId
            else -> return operation
        }
        val batch = database.invitationExperienceQueries.selectDirectInviteBatch(batchId)
            .executeAsOneOrNull() ?: return command.operation
        val event = database.eventQueries.selectById(batch.event_id).executeAsOneOrNull()
            ?: return command.operation
        val nowInstant = Clock.System.now()
        database.invitationExperienceQueries
            .deleteExpiredDirectInviteRecipientOutcomes(nowInstant.toString())
        val domainEvent = DatabaseEventRepository(database).getEvent(batch.event_id)
            ?: return command.operation
        val capability = command.capability as? DirectInviteCapability.Ready
            ?: return command.operation
        if (
            batch.expires_at.isExpiredAt(nowInstant) ||
            event.status != EventStatus.DRAFT.name || event.aggregateSchemaVersion != 1L ||
            event.aggregateRevision != capability.accessRevision ||
            capability.eventId != batch.event_id || capability.actorId != batch.actor_id ||
            EventTemporalClassifier.classify(domainEvent, nowInstant) == TemporalClass.PAST
        ) return command.operation

        val cancelled = policy.cancel(command)
        if (cancelled !is DirectInviteOperation.Cancelled) return cancelled
        return try {
            database.transactionWithResult {
                val now = Clock.System.now().toString()
                database.invitationExperienceQueries.updateDirectInviteBatchStatus(
                    status = DIRECT_INVITE_CANCELLED,
                    updated_at = now,
                    batch_id = batch.batch_id,
                    operation_id = batch.operation_id
                )
                cancelled.outcomesByRecipientKey.forEach { (key, outcome) ->
                    val persisted = outcome.persistedOutcome()
                    database.invitationExperienceQueries.updateDirectInviteRecipientOutcome(
                        status = persisted.status,
                        invitation_id = persisted.invitationId,
                        reason_code = persisted.reasonCode,
                        updated_at = now,
                        batch_id = batch.batch_id,
                        recipient_key = key.value
                    )
                    database.invitationExperienceQueries.updateDirectInviteDeliveryEnvelopeState(
                        transport_state = outcome.envelopeTransportState(),
                        batch_id = batch.batch_id,
                        recipient_key = key.value
                    )
                }
                cancelled
            }
        } catch (_: Exception) {
            command.operation
        }
    }

    suspend fun load(batchId: String): DirectInviteOperation? = loadPersisted(batchId)

    suspend fun acknowledge(
        command: AcknowledgeDirectInviteBatchCommand
    ): DirectInviteOperation {
        val previous = loadPersisted(command.batchId) ?: DirectInviteOperation.Idle
        if (previous !is DirectInviteOperation.PendingSync) return previous

        return try {
            database.transactionWithResult {
                val nowInstant = Clock.System.now()
                database.invitationExperienceQueries
                    .deleteExpiredDirectInviteRecipientOutcomes(nowInstant.toString())
                val batch = database.invitationExperienceQueries
                    .selectDirectInviteBatch(command.batchId)
                    .executeAsOneOrNull()
                    ?: return@transactionWithResult previous
                val capability = command.capability as? DirectInviteCapability.Ready
                    ?: return@transactionWithResult previous
                val event = database.eventQueries.selectById(batch.event_id).executeAsOneOrNull()
                    ?: return@transactionWithResult previous
                val domainEvent = DatabaseEventRepository(database).getEvent(batch.event_id)
                    ?: return@transactionWithResult previous
                if (
                    batch.status != DIRECT_INVITE_PENDING ||
                    batch.expires_at.isExpiredAt(nowInstant) ||
                    batch.operation_id != command.operationId ||
                    batch.event_id != capability.eventId ||
                    batch.actor_id != capability.actorId ||
                    batch.access_revision != capability.accessRevision ||
                    event.organizerId != capability.actorId ||
                    event.status != EventStatus.DRAFT.name ||
                    EventStatus.DRAFT !in capability.allowedEventStatuses ||
                    event.aggregateSchemaVersion != 1L ||
                    event.aggregateRevision != capability.accessRevision ||
                    EventTemporalClassifier.classify(domainEvent, nowInstant) == TemporalClass.PAST
                ) {
                    return@transactionWithResult previous
                }

                val persistedRows = database.invitationExperienceQueries
                    .selectDirectInviteRecipientOutcomes(batch.batch_id)
                    .executeAsList()
                val persistedKeys = persistedRows.mapTo(linkedSetOf()) {
                    RecipientKey(it.recipient_key)
                }
                val activeKeys = persistedRows
                    .filter { it.status == RECIPIENT_QUEUED }
                    .mapTo(linkedSetOf()) { RecipientKey(it.recipient_key) }
                if (
                    persistedKeys.isEmpty() ||
                    activeKeys != previous.recipientKeys ||
                    activeKeys != command.outcomesByRecipientKey.keys
                ) {
                    return@transactionWithResult previous
                }

                val mergedOutcomes = persistedRows.associate { row ->
                    val key = RecipientKey(row.recipient_key)
                    key to (command.outcomesByRecipientKey[key] ?: row.toRecipientOutcome())
                }
                val failedOutcome = mergedOutcomes.values
                    .filterIsInstance<DirectInviteRecipientOutcome.Failed>()
                    .firstOrNull()
                val acknowledged = if (failedOutcome == null) {
                    DirectInviteOperation.Completed(batch.batch_id, mergedOutcomes)
                } else {
                    DirectInviteOperation.Failed(
                        batchId = batch.batch_id,
                        operationId = batch.operation_id,
                        requestedRecipientKeys = persistedKeys,
                        outcomesByRecipientKey = mergedOutcomes,
                        batchError = failedOutcome.error
                    )
                }
                val persistedStatus = when (acknowledged) {
                    is DirectInviteOperation.Completed -> DIRECT_INVITE_COMPLETED
                    is DirectInviteOperation.Failed -> DIRECT_INVITE_FAILED
                    else -> return@transactionWithResult previous
                }
                val now = nowInstant.toString()
                command.outcomesByRecipientKey.forEach { (key, outcome) ->
                    val persisted = outcome.persistedOutcome()
                    database.invitationExperienceQueries.updateDirectInviteRecipientOutcome(
                        status = persisted.status,
                        invitation_id = persisted.invitationId,
                        reason_code = persisted.reasonCode,
                        updated_at = now,
                        batch_id = batch.batch_id,
                        recipient_key = key.value
                    )
                    database.invitationExperienceQueries.updateDirectInviteDeliveryEnvelopeState(
                        transport_state = outcome.envelopeTransportState(),
                        batch_id = batch.batch_id,
                        recipient_key = key.value
                    )
                }
                database.invitationExperienceQueries.updateDirectInviteBatchStatus(
                    status = persistedStatus,
                    updated_at = now,
                    batch_id = batch.batch_id,
                    operation_id = batch.operation_id
                )
                if (acknowledged is DirectInviteOperation.Completed) {
                    database.syncMetadataQueries.markSynced("direct-invite:${batch.operation_id}")
                }
                acknowledged
            }
        } catch (_: Exception) {
            previous
        }
    }

    private fun loadPersisted(batchId: String): DirectInviteOperation? {
        val now = Clock.System.now()
        database.invitationExperienceQueries
            .deleteExpiredDirectInviteRecipientOutcomes(now.toString())
        val batch = database.invitationExperienceQueries.selectDirectInviteBatch(batchId)
            .executeAsOneOrNull() ?: return null
        if (batch.expires_at.isExpiredAt(now)) return null
        val outcomes = database.invitationExperienceQueries
            .selectDirectInviteRecipientOutcomes(batchId)
            .executeAsList()
        if (outcomes.isEmpty()) return null
        val keys = outcomes.mapTo(linkedSetOf()) { RecipientKey(it.recipient_key) }
        val queuedKeys = outcomes
            .filter { it.status == RECIPIENT_QUEUED }
            .mapTo(linkedSetOf()) { RecipientKey(it.recipient_key) }
        val mapped = outcomes.associate { row ->
            RecipientKey(row.recipient_key) to row.toRecipientOutcome()
        }
        return when (batch.status) {
            DIRECT_INVITE_CANCELLED -> DirectInviteOperation.Cancelled(batchId, mapped)
            "COMPLETED" -> DirectInviteOperation.Completed(batchId, mapped)
            "FAILED" -> DirectInviteOperation.Failed(
                batchId = batchId,
                operationId = batch.operation_id,
                requestedRecipientKeys = keys,
                outcomesByRecipientKey = mapped,
                batchError = mapped.values
                    .filterIsInstance<DirectInviteRecipientOutcome.Failed>()
                    .firstOrNull()
                    ?.error
                    ?: InvitationExperienceError.PERMANENT_FAILURE
            )
            else -> DirectInviteOperation.PendingSync(batchId, batch.operation_id, queuedKeys)
        }
    }

    private fun forbidden(
        command: SubmitDirectInviteBatchCommand,
        error: InvitationExperienceError = InvitationExperienceError.FORBIDDEN
    ) = DirectInviteOperation.Failed(
        batchId = command.batchId,
        operationId = command.operationId,
        requestedRecipientKeys = command.recipientKeys,
        outcomesByRecipientKey = command.recipientKeys.associateWith {
            DirectInviteRecipientOutcome.Failed(error)
        },
        batchError = error
    )

    private suspend fun dispatch(
        operation: DirectInviteOperation.PendingSync,
        binding: DirectInviteDeliveryBinding,
        envelopes: Set<DirectInviteDeliveryEnvelope>,
        capability: DirectInviteCapability.Ready
    ): DirectInviteOperation {
        val transport = deliveryTransport ?: return operation
        val result = try {
            transport.dispatch(DirectInviteDeliveryRequest(binding, envelopes))
        } catch (_: Exception) {
            DirectInviteDeliveryResult.Deferred(InvitationExperienceError.NETWORK_UNAVAILABLE)
        }
        return when (result) {
            is DirectInviteDeliveryResult.Deferred -> operation
            is DirectInviteDeliveryResult.Acknowledged -> {
                if (
                    result.batchId != binding.batchId ||
                    result.operationId != binding.operationId ||
                    result.outcomesByRecipientKey.keys != operation.recipientKeys
                ) {
                    operation
                } else {
                    acknowledge(
                        AcknowledgeDirectInviteBatchCommand(
                            batchId = result.batchId,
                            operationId = result.operationId,
                            outcomesByRecipientKey = result.outcomesByRecipientKey,
                            capability = capability
                        )
                    )
                }
            }
            is DirectInviteDeliveryResult.Rejected -> acknowledge(
                AcknowledgeDirectInviteBatchCommand(
                    batchId = binding.batchId,
                    operationId = binding.operationId,
                    outcomesByRecipientKey = operation.recipientKeys.associateWith {
                        DirectInviteRecipientOutcome.Failed(result.error)
                    },
                    capability = capability
                )
            )
        }
    }
}

class DatabaseEventNotificationPreferenceRepository(
    private val database: WakeveDb
) : EventNotificationPreferenceRepository {
    override suspend fun get(
        eventId: String,
        userId: String
    ): EventNotificationPreferenceRecord? = database.invitationExperienceQueries
        .selectEventNotificationPreference(eventId, userId)
        .executeAsOneOrNull()
        ?.toNotificationPreferenceRecord()

    override suspend fun save(
        operationKey: OperationKey,
        preference: EventNotificationPreference
    ): Result<EventNotificationPreferenceRecord> {
        val subject = operationKey.subject as? OperationSubject.EventNotification
            ?: return invalidNotificationWrite()
        val target = operationKey.target as? OperationTarget.User
            ?: return invalidNotificationWrite()
        if (
            operationKey.action != InformationOperationAction.SAVE_EVENT_PREFERENCE ||
            operationKey.operationId.isBlank() || target.userId != subject.userId
        ) return invalidNotificationWrite()

        return try {
            database.transactionWithResult {
                val event = database.eventQueries.selectById(subject.eventId).executeAsOneOrNull()
                    ?: return@transactionWithResult invalidNotificationWrite()
                val temporal = DatabaseEventRepository(database).getEvent(subject.eventId)
                    ?.let { EventTemporalClassifier.classify(it, Clock.System.now()) }
                if (
                    event.aggregateSchemaVersion != 1L || event.status == EventStatus.FINALIZED.name ||
                    temporal == TemporalClass.PAST
                ) return@transactionWithResult invalidNotificationWrite()
                val isMember = event.organizerId == subject.userId ||
                    DatabaseEventRepository(database).getParticipantRecords(subject.eventId)
                        .orEmpty()
                        .firstOrNull { it.userId == subject.userId }
                        ?.let(ParticipantAccessMapper::fromRepositoryRecord)
                        ?.role == ParticipantAccessState.Role.MEMBER
                if (!isMember) return@transactionWithResult invalidNotificationWrite()

                database.invitationExperienceQueries
                    .selectEventNotificationPreferenceByOperationId(operationKey.operationId)
                    .executeAsOneOrNull()
                    ?.let { existing ->
                        return@transactionWithResult if (
                            existing.event_id == subject.eventId && existing.user_id == subject.userId &&
                            existing.preference == preference.name
                        ) {
                            Result.success(existing.toNotificationPreferenceRecord())
                        } else {
                            invalidNotificationWrite()
                        }
                    }

                val existing = database.invitationExperienceQueries
                    .selectEventNotificationPreference(subject.eventId, subject.userId)
                    .executeAsOneOrNull()
                val now = Clock.System.now().toString()
                if (existing == null) {
                    database.invitationExperienceQueries.insertEventNotificationPreference(
                        event_id = subject.eventId,
                        user_id = subject.userId,
                        preference = preference.name,
                        revision = 1L,
                        operation_id = operationKey.operationId,
                        sync_status = NOTIFICATION_PENDING,
                        updated_at = now
                    )
                } else {
                    database.invitationExperienceQueries.updateEventNotificationPreference(
                        preference = preference.name,
                        operation_id = operationKey.operationId,
                        sync_status = NOTIFICATION_PENDING,
                        updated_at = now,
                        event_id = subject.eventId,
                        user_id = subject.userId
                    )
                }
                database.syncMetadataQueries.insertSyncMetadata(
                    id = "event-notification:${operationKey.operationId}",
                    entityType = "event_notification_preference",
                    entityId = "${subject.eventId}:${subject.userId}",
                    operation = "UPDATE",
                    timestamp = now,
                    synced = 0L
                )
                Result.success(
                    checkNotNull(
                        database.invitationExperienceQueries
                            .selectEventNotificationPreference(subject.eventId, subject.userId)
                            .executeAsOneOrNull()
                    ).toNotificationPreferenceRecord()
                )
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun invalidNotificationWrite(): Result<EventNotificationPreferenceRecord> =
        Result.failure(IllegalArgumentException("Invalid event-scoped notification operation"))
}

sealed interface InformationDestination {
    data class Calendar(val eventId: String) : InformationDestination
    data class Maps(val locationId: String) : InformationDestination
    data class Weather(val locationId: String) : InformationDestination
}

sealed interface InformationDestinationState {
    data object Hidden : InformationDestinationState
    data object Loading : InformationDestinationState
    data class Ready(val destination: InformationDestination) : InformationDestinationState
    data class Unavailable(val error: InvitationExperienceError) : InformationDestinationState
    data class Failed(val error: InvitationExperienceError) : InformationDestinationState
}

data class EventInformationCapabilities(
    val canLeave: Boolean,
    val canRemoveParticipant: Boolean,
    val canDelete: Boolean,
    val canWriteEventPreference: Boolean
)

data class EventInformationSnapshot(
    val event: Event,
    val temporalClass: TemporalClass,
    val interactionPolicy: InteractionPolicy,
    val calendar: InformationDestinationState,
    val maps: InformationDestinationState,
    val weather: InformationDestinationState,
    val eventPreferenceRecord: EventNotificationPreferenceRecord?,
    val accountEnabledTypes: Set<EventNotificationType>,
    val quietHoursActive: Boolean,
    val systemAuthorization: SystemNotificationAuthorization,
    val capabilities: EventInformationCapabilities
)

sealed interface EventInformationLoadState {
    data object Idle : EventInformationLoadState
    data class Loading(
        val previousStableState: PreviousStableState<EventInformationSnapshot>
    ) : EventInformationLoadState
    data class Ready(
        val snapshot: EventInformationSnapshot,
        val freshness: Freshness
    ) : EventInformationLoadState
    data object Empty : EventInformationLoadState
    data class Failed(
        val error: InvitationExperienceError,
        val previousStableState: PreviousStableState<EventInformationSnapshot>
    ) : EventInformationLoadState
}

class DatabaseEventInformationRepository(
    private val database: WakeveDb
) {
    suspend fun load(
        eventId: String,
        viewerId: String,
        now: Instant
    ): EventInformationLoadState {
        return try {
            val repository = DatabaseEventRepository(database)
            val event = repository.getEvent(eventId) ?: return EventInformationLoadState.Empty
            val participant = repository.getParticipantRecords(eventId).orEmpty()
                .firstOrNull { it.userId == viewerId }
            val isOrganizer = event.organizerId == viewerId
            val isActiveMember = participant
                ?.let(ParticipantAccessMapper::fromRepositoryRecord)
                ?.role == ParticipantAccessState.Role.MEMBER
            if (!isOrganizer && !isActiveMember) return EventInformationLoadState.Empty

        val temporalClass = EventTemporalClassifier.classify(event, now)
        val policy = InvitationExperienceInteractionPolicy.derive(
            temporalClass,
            event.status
        )
        val interactive = policy == InteractionPolicy.INTERACTIVE
        val confirmedDate = database.confirmedDateQueries.selectByEventId(eventId)
            .executeAsOneOrNull()
        val location = database.eventWeatherQueries.selectResolvedLocationByEvent(eventId)
            .executeAsOneOrNull()
        val weather = database.eventWeatherQueries.selectLatestWeatherSnapshotForEvent(eventId)
            .executeAsOneOrNull()
            ?.takeIf { location != null && it.locationId == location.id }
        val preference = database.invitationExperienceQueries
            .selectEventNotificationPreference(eventId, viewerId)
            .executeAsOneOrNull()
            ?.toNotificationPreferenceRecord()
        val accountPreferences = database.userQueries
            .selectPreferencesByUserId(viewerId)
            .executeAsOneOrNull()
        val accountEnabledTypes = accountPreferences?.enabled_types.toEventNotificationTypes()
        val quietHoursActive = accountPreferences?.let { stored ->
            isInsideQuietHours(
                now = now,
                start = stored.quiet_hours_start,
                end = stored.quiet_hours_end
            )
        } ?: false

            EventInformationLoadState.Ready(
            snapshot = EventInformationSnapshot(
                event = event,
                temporalClass = temporalClass,
                interactionPolicy = policy,
                calendar = when {
                    !interactive -> InformationDestinationState.Hidden
                    confirmedDate != null -> InformationDestinationState.Ready(
                        InformationDestination.Calendar(eventId)
                    )
                    else -> InformationDestinationState.Unavailable(
                        InvitationExperienceError.PROVIDER_UNAVAILABLE
                    )
                },
                maps = when {
                    !interactive -> InformationDestinationState.Hidden
                    location != null -> InformationDestinationState.Ready(
                        InformationDestination.Maps(location.id)
                    )
                    else -> InformationDestinationState.Unavailable(
                        InvitationExperienceError.PROVIDER_UNAVAILABLE
                    )
                },
                weather = when {
                    !interactive -> InformationDestinationState.Hidden
                    weather != null -> InformationDestinationState.Ready(
                        InformationDestination.Weather(weather.locationId)
                    )
                    else -> InformationDestinationState.Unavailable(
                        InvitationExperienceError.PROVIDER_UNAVAILABLE
                    )
                },
                eventPreferenceRecord = preference,
                accountEnabledTypes = accountEnabledTypes,
                quietHoursActive = quietHoursActive,
                // The repository has no OS authorization port. Absence of that
                // reading is distinct from a user decision of NOT_DETERMINED.
                systemAuthorization = SystemNotificationAuthorization.UNAVAILABLE,
                capabilities = EventInformationCapabilities(
                    canLeave = interactive && !isOrganizer,
                    canRemoveParticipant = interactive && isOrganizer,
                    canDelete = interactive && isOrganizer,
                    canWriteEventPreference = interactive
                )
            ),
            freshness = Freshness.Current
            )
        } catch (_: Exception) {
            EventInformationLoadState.Failed(
                error = InvitationExperienceError.REPOSITORY_UNAVAILABLE,
                previousStableState = PreviousStableState.Idle
            )
        }
    }
}

private fun String?.toEventNotificationTypes(): Set<EventNotificationType> {
    if (isNullOrBlank()) return emptySet()
    return runCatching { Json.decodeFromString<List<String>>(this) }
        .getOrDefault(emptyList())
        .mapNotNullTo(linkedSetOf()) { storedName ->
            EventNotificationType.entries.firstOrNull { it.name == storedName }
        }
}

private fun isInsideQuietHours(now: Instant, start: String?, end: String?): Boolean {
    val startMinute = start.toMinuteOfDayOrNull() ?: return false
    val endMinute = end.toMinuteOfDayOrNull() ?: return false
    val current = now.toLocalDateTime(TimeZone.UTC)
    val currentMinute = current.hour * 60 + current.minute
    return when {
        startMinute == endMinute -> true
        startMinute < endMinute -> currentMinute in startMinute until endMinute
        else -> currentMinute >= startMinute || currentMinute < endMinute
    }
}

private fun String?.toMinuteOfDayOrNull(): Int? {
    if (this == null || length != 5 || this[2] != ':') return null
    val hour = substring(0, 2).toIntOrNull() ?: return null
    val minute = substring(3, 5).toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private const val DIRECT_INVITE_PENDING = "PENDING_SYNC"
private const val DIRECT_INVITE_CANCELLED = "CANCELLED"
private const val DIRECT_INVITE_COMPLETED = "COMPLETED"
private const val DIRECT_INVITE_FAILED = "FAILED"
private const val RECIPIENT_QUEUED = "QUEUED_LOCAL"
private const val ENVELOPE_QUEUED = "QUEUED_LOCAL"
private const val NOTIFICATION_PENDING = "PENDING_SYNC"
private const val PROTECTED_RECIPIENT_RETENTION_DAYS = 30

private data class DirectInviteDispatchPlan(
    val operation: DirectInviteOperation.PendingSync,
    val binding: DirectInviteDeliveryBinding,
    val envelopes: Set<DirectInviteDeliveryEnvelope>,
    val capability: DirectInviteCapability.Ready
)

private data class PersistedRecipientOutcome(
    val status: String,
    val invitationId: String?,
    val reasonCode: String?
)

private fun DirectInviteRecipientOutcome.persistedOutcome(): PersistedRecipientOutcome = when (this) {
    is DirectInviteRecipientOutcome.ServerAccepted -> PersistedRecipientOutcome(
        "SERVER_ACCEPTED",
        invitationId,
        null
    )
    is DirectInviteRecipientOutcome.Invalid -> PersistedRecipientOutcome("INVALID", null, reason)
    is DirectInviteRecipientOutcome.Failed -> PersistedRecipientOutcome("FAILED", null, error.name)
    DirectInviteRecipientOutcome.Cancelled -> PersistedRecipientOutcome("CANCELLED", null, null)
}

private fun DirectInviteRecipientOutcome.envelopeTransportState(): String = when (this) {
    is DirectInviteRecipientOutcome.ServerAccepted -> "SERVER_ACCEPTED"
    is DirectInviteRecipientOutcome.Invalid -> "FAILED_PERMANENT"
    is DirectInviteRecipientOutcome.Failed -> if (error.isRetryableDeliveryError()) {
        "FAILED_RETRYABLE"
    } else {
        "FAILED_PERMANENT"
    }
    DirectInviteRecipientOutcome.Cancelled -> "CANCELLED"
}

private fun Set<DirectInviteDeliveryEnvelope>.areValidFor(
    binding: DirectInviteDeliveryBinding,
    recipientKeys: Set<RecipientKey>,
    now: Instant
): Boolean {
    if (isEmpty() || mapTo(linkedSetOf()) { it.recipientKey } != recipientKeys) return false
    val latestAllowed = now.plus(PROTECTED_RECIPIENT_RETENTION_DAYS.days)
    return all { envelope ->
        val expiry = runCatching { Instant.parse(envelope.expiresAt) }.getOrNull()
            ?: return@all false
        envelope.binding == binding &&
            envelope.keyVersion > 0 &&
            envelope.ciphertext.isNotBlank() &&
            envelope.ciphertext != envelope.recipientKey.value &&
            expiry > now && expiry <= latestAllowed
    }
}

private fun com.guyghost.wakeve.Direct_invite_delivery_envelope.toDeliveryEnvelope(
    batch: com.guyghost.wakeve.Direct_invite_batch
): DirectInviteDeliveryEnvelope = DirectInviteDeliveryEnvelope(
    binding = DirectInviteDeliveryBinding(
        eventId = batch.event_id,
        actorId = batch.actor_id,
        accessRevision = batch.access_revision,
        batchId = batch.batch_id,
        operationId = batch.operation_id
    ),
    recipientKey = RecipientKey(recipient_key),
    ciphertext = ciphertext,
    keyVersion = key_version.toInt(),
    expiresAt = expires_at
)

private fun com.guyghost.wakeve.Direct_invite_recipient_outcome.toRecipientOutcome(): DirectInviteRecipientOutcome =
    when (status) {
        "SERVER_ACCEPTED", "DELIVERED" -> invitation_id?.let {
            DirectInviteRecipientOutcome.ServerAccepted(it)
        } ?: DirectInviteRecipientOutcome.Failed(InvitationExperienceError.PERMANENT_FAILURE)
        "INVALID" -> DirectInviteRecipientOutcome.Invalid(reason_code.orEmpty())
        "CANCELLED" -> DirectInviteRecipientOutcome.Cancelled
        else -> DirectInviteRecipientOutcome.Failed(
            runCatching { InvitationExperienceError.valueOf(reason_code.orEmpty()) }
                .getOrDefault(InvitationExperienceError.NETWORK_UNAVAILABLE)
        )
    }

private fun InvitationExperienceError?.isRetryableDeliveryError(): Boolean = when (this) {
    InvitationExperienceError.NETWORK_UNAVAILABLE,
    InvitationExperienceError.REPOSITORY_UNAVAILABLE,
    InvitationExperienceError.PROVIDER_UNAVAILABLE,
    InvitationExperienceError.SERVER_UNAVAILABLE -> true
    else -> false
}

private fun String?.isExpiredAt(now: Instant): Boolean {
    val expiry = this?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }
        ?: return true
    return expiry <= now
}

private fun com.guyghost.wakeve.Event_notification_preference.toNotificationPreferenceRecord() =
    EventNotificationPreferenceRecord(
        eventId = event_id,
        userId = user_id,
        preference = runCatching { EventNotificationPreference.valueOf(preference) }
            .getOrDefault(EventNotificationPreference.INHERIT_ACCOUNT),
        operationId = operation_id,
        pendingSync = sync_status != "SYNCED"
    )

private fun com.guyghost.wakeve.Event_artwork.toArtwork(): Artwork? {
    return when (kind) {
        "NONE" -> Artwork.None
        "LEGACY_REMOTE" -> legacy_remote_url
            ?.takeIf { ALLOWED_ARTWORK_REMOTE.matches(it) }
            ?.let(Artwork::LegacyRemote)
        "STRUCTURED" -> {
            val assetId = server_asset_id
            val remoteUrl = canonical_https_url
            val revision = asset_revision
            val source = when (source_kind) {
                "PRESET" -> preset_id?.takeIf(String::isNotBlank)?.let(ArtworkSource::Preset)
                "SERVER_ASSET" -> if (
                    !assetId.isNullOrBlank() && !remoteUrl.isNullOrBlank() &&
                    revision != null && ALLOWED_ARTWORK_REMOTE.matches(remoteUrl)
                ) {
                    ArtworkSource.ServerAsset(assetId, remoteUrl, revision)
                } else null
                else -> null
            } ?: return null
            val alt = when (alt_kind) {
                "DECORATIVE" -> ArtworkAlt.Decorative
                "INFORMATIVE" -> alt_text?.takeIf(String::isNotBlank)?.let(ArtworkAlt::Informative)
                else -> null
            } ?: return null
            val focalX = focal_x ?: return null
            val focalY = focal_y ?: return null
            val artworkCrop = runCatching { ArtworkCrop.valueOf(crop.orEmpty()) }.getOrNull()
                ?: return null
            Artwork.Structured(
                version = structured_version?.toInt() ?: return null,
                ref = ArtworkRef(
                    source,
                    alt,
                    ArtworkFocalPoint(focalX, focalY),
                    artworkCrop
                )
            )
        }
        else -> null
    }
}

private fun LibrarySyncState.warning(): LibrarySyncWarning? = when (this) {
    is LibrarySyncState.Conflict -> LibrarySyncWarning.CONFLICT
    is LibrarySyncState.PermanentFailure -> LibrarySyncWarning.PERMANENT_FAILURE
    else -> null
}

private val ALLOWED_ARTWORK_REMOTE = Regex(
    "^https://(?:cdn|api)\\.wakeve\\.app(?:/[^@?#]*)?$",
    RegexOption.IGNORE_CASE
)
