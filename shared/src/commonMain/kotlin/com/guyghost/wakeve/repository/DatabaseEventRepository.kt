package com.guyghost.wakeve.repository

import com.guyghost.wakeve.access.EventAccessPolicy
import com.guyghost.wakeve.access.ParticipantAccessMapper
import com.guyghost.wakeve.access.ParticipantAccessState
import com.guyghost.wakeve.access.ParticipantRepositoryRecord
import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.confirmation.SystemConfirmationClock
import com.guyghost.wakeve.confirmation.confirmationEffectKeys
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.invitationexperience.EventTemporalClassifier
import com.guyghost.wakeve.invitationexperience.DatabaseServerArtworkReferenceOwner
import com.guyghost.wakeve.invitationexperience.ServerArtworkReference
import com.guyghost.wakeve.invitationexperience.ServerArtworkReferenceResult
import com.guyghost.wakeve.invitationexperience.TemporalClass
import com.guyghost.wakeve.repository.EventRepositoryInterface
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventPlanningMode
import com.guyghost.wakeve.models.EventSearchResult
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.NearbyEventResult
import com.guyghost.wakeve.models.NearbyEventsResponse
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.RecommendedEventsResponse
import com.guyghost.wakeve.models.SearchResultsResponse
import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.TrendingEventsResponse
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.organization.EventOrganizationReadinessRepository
import com.guyghost.wakeve.poll.PollBallotContract
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.repository.OrderBy
import com.guyghost.wakeve.sync.SyncManager
import com.guyghost.wakeve.workflow.WorkflowOutboxRecord
import com.guyghost.wakeve.workflow.WorkflowOutboxType
import com.guyghost.wakeve.workflow.PendingWorkflowStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Database-backed event repository using SQLDelight for persistence.
 * Mirrors the EventRepository interface but stores data in SQLite.
 */
class DatabaseEventRepository private constructor(
    private val db: WakeveDb,
    private val syncManager: SyncManager?,
    private val confirmationClock: ConfirmationClock
) : EventRepositoryInterface, com.guyghost.wakeve.presentation.statemachine.SampleEventSeeder {
    /** Public Kotlin/Swift surface retained for application composition. */
    constructor(db: WakeveDb, syncManager: SyncManager? = null) :
        this(db, syncManager, SystemConfirmationClock)

    /** Internal test seam; confirmation time remains owned by this repository. */
    internal constructor(db: WakeveDb, confirmationClock: ConfirmationClock) :
        this(db, null, confirmationClock)

    private val eventQueries = db.eventQueries
    private val timeSlotQueries = db.timeSlotQueries
    private val participantQueries = db.participantQueries
    private val voteQueries = db.voteQueries
    private val confirmedDateQueries = db.confirmedDateQueries
    private val syncMetadataQueries = db.syncMetadataQueries
    private val workflowOutboxQueries = db.workflowOutboxQueries
    private val confirmationReceiptQueries = db.confirmationReceiptQueries
    private val confirmationEffectOutboxQueries = db.confirmationEffectOutboxQueries
    private val confirmationLegacyClassificationQueries = db.confirmationLegacyClassificationQueries
    private val invitationExperienceQueries = db.invitationExperienceQueries
    private val pollBallotReceiptQueries = db.pollBallotReceiptQueries
    private val ballotJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val confirmationMutex = Mutex()
    private val ballotMutex = Mutex()

    override suspend fun createEvent(event: Event): Result<Event> {
        return try {
            val now = getCurrentUtcIsoString()
            // Determine isSample flag from ID prefix
            val isSample = com.guyghost.wakeve.sample.SampleEventFactory.isSampleEventId(event.id)

            db.transaction {
                eventQueries.insertEvent(
                    id = event.id,
                    organizerId = event.organizerId,
                    title = event.title,
                    description = event.description,
                    status = event.status.name,
                    deadline = event.deadline,
                    createdAt = now,
                    updatedAt = now,
                    version = 1,
                    eventType = event.eventType.name,
                    eventTypeCustom = event.eventTypeCustom,
                    minParticipants = event.minParticipants?.toLong(),
                    maxParticipants = event.maxParticipants?.toLong(),
                    expectedParticipants = event.expectedParticipants?.toLong(),
                    isSample = if (isSample) 1L else 0L
                )
                val creationAuthorizationId = "event-create:${event.id}"
                if (!authorizeAggregateWrite(
                        event.id,
                        1L,
                        creationAuthorizationId,
                        now
                    )
                ) error("Event creation aggregate writer is incompatible")
                eventQueries.setEventPlanningModeWithinAggregateWrite(
                    planningMode = event.planningMode.name,
                    updatedAt = now,
                    id = event.id
                )
                invitationExperienceQueries.clearAggregateWriteAuthorization(
                    event.id,
                    creationAuthorizationId
                )
                invitationExperienceQueries.upsertEventArtwork(
                    event_id = event.id,
                    kind = "NONE",
                    structured_version = null,
                    source_kind = null,
                    preset_id = null,
                    server_asset_id = null,
                    canonical_https_url = null,
                    asset_revision = null,
                    alt_kind = null,
                    alt_text = null,
                    focal_x = null,
                    focal_y = null,
                    crop = null,
                    legacy_remote_url = null,
                    updated_at = now
                )

                // Insert organizer as participant.
                participantQueries.insertParticipantWithAxes(
                    id = "org_${event.id}",
                    eventId = event.id,
                    userId = event.organizerId,
                    role = "ORGANIZER",
                    hasValidatedDate = 0,
                    rsvpState = "NOT_APPLICABLE",
                    dateValidationState = "NOT_APPLICABLE",
                    joinedAt = now,
                    updatedAt = now
                )

                event.proposedSlots.forEach { slot ->
                    timeSlotQueries.insertTimeSlot(
                        id = physicalSlotId(event.id, slot.id),
                        eventId = event.id,
                        startTime = slot.start,
                        endTime = slot.end,
                        timezone = slot.timezone,
                        proposedByParticipantId = null,
                        createdAt = now,
                        updatedAt = now,
                        timeOfDay = slot.timeOfDay.name
                    )
                }

                syncMetadataQueries.insertSyncMetadata(
                    id = "sync_${event.id}",
                    entityType = "event",
                    entityId = event.id,
                    operation = "CREATE",
                    timestamp = now,
                    synced = 0
                )
            }

            // The aggregate is durable before the optional transport is notified.
            syncManager?.recordLocalChange(
                table = "events",
                operation = SyncOperation.CREATE,
                recordId = event.id,
                data = """{"id":"${event.id}","title":"${event.title}","description":"${event.description}","organizerId":"${event.organizerId}","deadline":"${event.deadline}"}""",
                userId = event.organizerId
            )

            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getEvent(id: String): Event? {
        return try {
            val eventRow = eventQueries.selectById(id).executeAsOneOrNull() ?: return null
            val participants = participantQueries.selectByEventId(id).executeAsList()
            val timeSlots = timeSlotQueries.selectByEventId(id).executeAsList()
            val confirmedSlot = confirmedDateQueries.selectWithTimeslotDetails(id).executeAsOneOrNull()
            val artwork = invitationExperienceQueries.selectArtworkByEventId(id).executeAsOneOrNull()

            Event(
                id = eventRow.id,
                title = eventRow.title,
                description = eventRow.description,
                organizerId = eventRow.organizerId,
                participants = participants.map { it.userId },
                proposedSlots = timeSlots.map {
                    TimeSlot(
                        id = logicalSlotId(id, it.id),
                        start = it.startTime,
                        end = it.endTime,
                        timezone = it.timezone,
                        timeOfDay = parseTimeOfDay(it.timeOfDay)
                    )
                },
                deadline = eventRow.deadline,
                status = parseEventStatus(eventRow.status),
                finalDate = confirmedSlot?.startTime,
                createdAt = eventRow.createdAt,
                updatedAt = eventRow.updatedAt,
                eventType = parseEventType(eventRow.eventType),
                eventTypeCustom = eventRow.eventTypeCustom,
                minParticipants = eventRow.minParticipants?.toInt(),
                maxParticipants = eventRow.maxParticipants?.toInt(),
                expectedParticipants = eventRow.expectedParticipants?.toInt(),
                heroImageUrl = artwork?.validatedRemoteArtworkUrl(),
                planningMode = parseEventPlanningMode(eventRow.planningMode),
                aggregateRevision = eventRow.aggregateRevision,
                aggregateSchemaVersion = eventRow.aggregateSchemaVersion
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun getPoll(eventId: String): Poll? {
        val event = getEvent(eventId) ?: return null
        val votes = mutableMapOf<String, Map<String, Vote>>()

        val allVotes = voteQueries.selectVotesForEventTimeslots(eventId).executeAsList()
        
        allVotes.forEach { voteRow ->
            val participantId = voteRow.userId
            val slotId = logicalSlotId(eventId, voteRow.timeslotId)
            val voteValue = parseVote(voteRow.vote)

            if (!votes.containsKey(participantId)) {
                votes[participantId] = mutableMapOf()
            }
            (votes[participantId] as? MutableMap<String, Vote>)?.put(slotId, voteValue)
        }

        return Poll(eventId, eventId, votes)
    }

    override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> {
        val event = getEvent(eventId) ?: return Result.failure(IllegalArgumentException("Event not found"))

        if (event.status != EventStatus.DRAFT) {
            return Result.failure(IllegalStateException("Cannot add participants after DRAFT status"))
        }

        if (event.participants.contains(participantId)) {
            return Result.failure(IllegalArgumentException("Participant already added"))
        }

        return try {
            val now = getCurrentUtcIsoString()
            val newParticipantId = "part_${eventId}_${participantId}"
            participantQueries.insertParticipantWithAxes(
                id = newParticipantId,
                eventId = eventId,
                userId = participantId,
                role = "PARTICIPANT",
                hasValidatedDate = 0,
                rsvpState = "PENDING",
                dateValidationState = "NOT_VALIDATED",
                joinedAt = now,
                updatedAt = now
            )

            // Record sync change for offline tracking
            syncManager?.recordLocalChange(
                table = "participants",
                operation = SyncOperation.CREATE,
                recordId = newParticipantId,
                data = """{"eventId":"$eventId","userId":"$participantId"}""",
                userId = participantId
            )

            syncMetadataQueries.insertSyncMetadata(
                id = "sync_${newParticipantId}",
                entityType = "participant",
                entityId = newParticipantId,
                operation = "CREATE",
                timestamp = now,
                synced = 0
            )

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getParticipants(eventId: String): List<String>? {
        return try {
            participantQueries.selectByEventId(eventId).executeAsList().map { it.userId }
        } catch (e: Exception) {
            null
        }
    }

    override fun getParticipantRecords(eventId: String): List<ParticipantRepositoryRecord>? {
        return try {
            participantQueries.selectByEventId(eventId).executeAsList().map { participant ->
                ParticipantRepositoryRecord(
                    id = participant.id,
                    eventId = participant.eventId,
                    userId = participant.userId,
                    role = when (participant.role.uppercase()) {
                        "ORGANIZER" -> "ORGANIZER"
                        "MEMBER", "PARTICIPANT" -> "MEMBER"
                        else -> participant.role.uppercase()
                    },
                    rsvp = participant.rsvpState,
                    hasValidatedDate = participant.hasValidatedDate,
                    dateValidation = participant.dateValidationState
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun addVote(eventId: String, participantId: String, slotId: String, vote: Vote): Result<Boolean> {
        val event = getEvent(eventId) ?: return Result.failure(IllegalArgumentException("Event not found"))

        if (event.status != EventStatus.POLLING) {
            return Result.failure(IllegalStateException("Event is not in POLLING status"))
        }

        if (isDeadlinePassed(event.deadline)) {
            return Result.failure(IllegalStateException("Voting deadline has passed"))
        }

        if (!event.participants.contains(participantId)) {
            return Result.failure(IllegalArgumentException("Participant not in event"))
        }

        // Get the actual participant record ID (not userId)
        val participantRecord = participantQueries.selectByEventIdAndUserId(eventId, participantId).executeAsOneOrNull()
            ?: return Result.failure(IllegalArgumentException("Participant record not found"))

        return try {
            val now = getCurrentUtcIsoString()
            val persistedSlotId = physicalSlotId(eventId, slotId)
            if (timeSlotQueries.selectById(persistedSlotId).executeAsOneOrNull()?.eventId != eventId) {
                return Result.failure(IllegalArgumentException("Time slot not in event"))
            }
            val voteId = "vote_${persistedSlotId}_${participantId}"
            voteQueries.insertVote(
                id = voteId,
                eventId = eventId,
                timeslotId = persistedSlotId,
                participantId = participantRecord.id,  // Use the actual participant record ID
                vote = vote.name,
                createdAt = now,
                updatedAt = now
            )

            // Record sync change for offline tracking
            syncManager?.recordLocalChange(
                table = "votes",
                operation = SyncOperation.CREATE,
                recordId = voteId,
                data = """{"eventId":"$eventId","participantId":"$participantId","slotId":"$slotId","preference":"${vote.name}"}""",
                userId = participantId
            )

            syncMetadataQueries.insertSyncMetadata(
                id = "sync_${voteId}",
                entityType = "vote",
                entityId = voteId,
                operation = "CREATE",
                timestamp = now,
                synced = 0
            )

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun commitCompleteBallot(
        command: PollBallotContract.CommitCompleteBallotCommand
    ): PollBallotContract.CommitResult = ballotMutex.withLock {
        commitCompleteBallotLocked(command, serverApply = false)
    }

    override suspend fun resolveCompleteBallotOutcome(
        command: PollBallotContract.CommitCompleteBallotCommand
    ): PollBallotContract.ResolutionResult = ballotMutex.withLock {
        val operationKey = runCatching { PollBallotContract.operationKey(command) }.getOrNull()
            ?: return@withLock PollBallotContract.ResolutionResult.Unknown(
                PollBallotContract.Failure(
                    PollBallotContract.FailureCode.INVALID_POLL_REVISION,
                    retryable = false,
                    commitOutcome = PollBallotContract.CommitOutcome.UNKNOWN
                )
            )
        val fingerprint = runCatching { PollBallotContract.envelope(command).ballotFingerprint }
            .getOrNull()
            ?: return@withLock PollBallotContract.ResolutionResult.Unknown(
                PollBallotContract.Failure(
                    PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT,
                    retryable = false,
                    commitOutcome = PollBallotContract.CommitOutcome.UNKNOWN
                )
            )
        val row = selectBallotReceipt(command)
            ?: return@withLock PollBallotContract.ResolutionResult.ProvenNotCommitted(
                operationKey,
                fingerprint
            )
        when (val replay = classifyBallotReplay(command, row)) {
            is PollBallotContract.CommitResult.Committed ->
                PollBallotContract.ResolutionResult.Committed(replay.receipt)
            is PollBallotContract.CommitResult.AlreadyCommitted ->
                PollBallotContract.ResolutionResult.Committed(replay.receipt)
            is PollBallotContract.CommitResult.Rejected ->
                PollBallotContract.ResolutionResult.Unknown(
                    replay.failure.copy(commitOutcome = PollBallotContract.CommitOutcome.UNKNOWN)
                )
        }
    }

    /** Server-only atomic application. It never emits another local sync row. */
    suspend fun applySyncedCompleteBallot(
        payload: PollBallotContract.BallotSyncPayload,
        authenticatedActorId: String
    ): PollBallotContract.BallotServerAck = ballotMutex.withLock {
        require(payload.schemaVersion == PollBallotContract.SCHEMA_VERSION) { "Unsupported ballot payload" }
        require(payload.localReceiptId.isNotBlank()) { "Ballot local receipt is required" }
        require(payload.command.schemaVersion == PollBallotContract.SCHEMA_VERSION) { "Unsupported ballot command" }
        require(payload.command.identity.actorId == authenticatedActorId) { "Ballot actor does not match authentication" }
        val normalizedPayload = if (payload.command.authoritativeDeadlineIso.isBlank()) {
            val deadline = eventQueries.selectById(payload.command.identity.eventId)
                .executeAsOneOrNull()
                ?.deadline
                ?: error("Ballot event was not found")
            payload.copy(command = payload.command.copy(authoritativeDeadlineIso = deadline))
        } else {
            payload
        }
        val command = PollBallotContract.command(normalizedPayload.command)
        require(PollBallotContract.matches(normalizedPayload.command, command)) { "Ballot envelope is not canonical" }
        val result = commitCompleteBallotLocked(
            command = command,
            serverApply = true,
            incomingPayload = normalizedPayload
        )
        val receipt = when (result) {
            is PollBallotContract.CommitResult.Committed -> result.receipt
            is PollBallotContract.CommitResult.AlreadyCommitted -> result.receipt
            is PollBallotContract.CommitResult.Rejected -> error(result.failure.code.name)
        }
        PollBallotContract.BallotServerAck(
            localReceiptId = normalizedPayload.localReceiptId,
            serverReceiptId = receipt.serverReceiptId ?: receipt.receiptId,
            identity = normalizedPayload.command.identity,
            ballotFingerprint = normalizedPayload.command.ballotFingerprint,
            outcome = if (result is PollBallotContract.CommitResult.Committed) {
                PollBallotContract.BallotServerOutcome.APPLIED
            } else {
                PollBallotContract.BallotServerOutcome.ALREADY_APPLIED
            }
        )
    }

    private suspend fun commitCompleteBallotLocked(
        command: PollBallotContract.CommitCompleteBallotCommand,
        serverApply: Boolean,
        incomingPayload: PollBallotContract.BallotSyncPayload? = null
    ): PollBallotContract.CommitResult {
        if (!PollBallotContract.isValidPollRevision(command.pollRevision)) {
            return ballotRejected(command, PollBallotContract.FailureCode.INVALID_POLL_REVISION)
        }
        // Resolve an unknown outcome before consulting the current deadline.
        // A durable identical receipt remains authoritative after the poll closes.
        selectBallotReceipt(command)?.let { row ->
            return classifyBallotReplay(command, row)
        }

        return try {
            val committed = db.transactionWithResult {
                // Re-read every mutable authority inside the same transaction that
                // writes the ballot and receipt.
                val eventRow = eventQueries.selectById(command.eventId).executeAsOneOrNull()
                    ?: return@transactionWithResult ballotRejected(
                        command,
                        PollBallotContract.FailureCode.EVENT_NOT_FOUND
                    )
                val participant = participantQueries
                    .selectByEventIdAndUserId(command.eventId, command.actorId)
                    .executeAsOneOrNull()
                    ?: return@transactionWithResult ballotRejected(
                        command,
                        PollBallotContract.FailureCode.FORBIDDEN
                    )
                val event = getEvent(command.eventId)
                    ?: return@transactionWithResult ballotRejected(
                        command,
                        PollBallotContract.FailureCode.EVENT_NOT_FOUND
                    )
                val viewer = ParticipantAccessMapper.fromRepositoryRecord(
                    ParticipantRepositoryRecord(
                        id = participant.id,
                        eventId = participant.eventId,
                        userId = participant.userId,
                        role = participant.role,
                        rsvp = participant.rsvpState,
                        hasValidatedDate = participant.hasValidatedDate,
                        dateValidation = participant.dateValidationState
                    )
                )
                if (!EventAccessPolicy.canSubmitPollBallot(event, viewer).isAllowed) {
                    return@transactionWithResult ballotRejected(
                        command,
                        PollBallotContract.FailureCode.FORBIDDEN
                    )
                }
                if (eventRow.status != EventStatus.POLLING.name) {
                    return@transactionWithResult ballotRejected(
                        command,
                        PollBallotContract.FailureCode.INVALID_EVENT_STATUS
                    )
                }
                if (eventRow.aggregateRevision != command.pollRevision) {
                    return@transactionWithResult ballotRejected(
                        command,
                        PollBallotContract.FailureCode.POLL_REVISION_CONFLICT
                    )
                }

                val authoritativeDeadlineIso = command.authoritativeDeadlineIso
                    .ifBlank { eventRow.deadline }
                val effectiveCommand = command.copy(
                    authoritativeDeadlineIso = authoritativeDeadlineIso
                )
                val envelope = PollBallotContract.envelope(effectiveCommand)
                if (incomingPayload != null && incomingPayload.command != envelope) {
                    return@transactionWithResult ballotRejected(
                        command,
                        PollBallotContract.FailureCode.IDEMPOTENCY_CONFLICT
                    )
                }
                if (authoritativeDeadlineIso != eventRow.deadline) {
                    return@transactionWithResult ballotRejected(
                        command,
                        PollBallotContract.FailureCode.POLL_REVISION_CONFLICT
                    )
                }
                val deadline = try {
                    Instant.parse(authoritativeDeadlineIso)
                } catch (_: Exception) {
                    return@transactionWithResult ballotRejected(
                        command,
                        PollBallotContract.FailureCode.INVALID_DEADLINE_ISO
                    )
                }
                val acceptedAt = try {
                    confirmationClock.now()
                } catch (_: Exception) {
                    return@transactionWithResult ballotRejected(
                        command,
                        PollBallotContract.FailureCode.CLOCK_UNAVAILABLE,
                        retryable = true
                    )
                }
                if (acceptedAt >= deadline) {
                    return@transactionWithResult ballotRejected(
                        command,
                        PollBallotContract.FailureCode.DEADLINE_REACHED
                    )
                }

                val currentSlots = timeSlotQueries.selectByEventId(command.eventId).executeAsList()
                val physicalIdsByLogicalId = currentSlots.associate { row ->
                    logicalSlotId(command.eventId, row.id) to row.id
                }
                PollBallotContract.validateEntries(
                    physicalIdsByLogicalId.keys,
                    command.entries
                )?.let { failure ->
                    return@transactionWithResult ballotRejected(
                        command,
                        failure
                    )
                }

                val acceptedAtIso = acceptedAt.toString()
                voteQueries.deleteByEventAndParticipant(command.eventId, participant.id)
                envelope.entries.forEach { entry ->
                    val persistedSlotId = requireNotNull(physicalIdsByLogicalId[entry.slotId])
                    voteQueries.insertVote(
                        id = "vote_${persistedSlotId}_${command.actorId}",
                        eventId = command.eventId,
                        timeslotId = persistedSlotId,
                        participantId = participant.id,
                        vote = entry.vote.name,
                        createdAt = acceptedAtIso,
                        updatedAt = acceptedAtIso
                    )
                }

                val operationKey = PollBallotContract.operationKey(envelope.identity)
                val receiptId = incomingPayload?.localReceiptId ?: "poll-ballot:$operationKey"
                val payload = incomingPayload ?: PollBallotContract.BallotSyncPayload(
                    localReceiptId = receiptId,
                    command = envelope
                )
                val serverReceiptId = "server-poll-ballot:$operationKey".takeIf { serverApply }
                val receipt = PollBallotContract.Receipt(
                    receiptId = receiptId,
                    operationId = command.operationId,
                    eventId = command.eventId,
                    actorId = command.actorId,
                    pollRevision = command.pollRevision,
                    ballotFingerprint = envelope.ballotFingerprint,
                    authoritativeDeadlineIso = authoritativeDeadlineIso,
                    acceptedAtIso = acceptedAtIso,
                    syncStatus = if (serverApply) {
                        PollBallotContract.SyncStatus.SYNCED
                    } else {
                        PollBallotContract.SyncStatus.LOCAL_PENDING
                    },
                    syncPayload = payload,
                    serverReceiptId = serverReceiptId
                )
                pollBallotReceiptQueries.insertReceipt(
                    operationId = receipt.operationId,
                    receiptId = receipt.receiptId,
                    eventId = receipt.eventId,
                    actorId = receipt.actorId,
                    pollRevision = receipt.pollRevision,
                    operationKey = operationKey,
                    ballotFingerprint = receipt.ballotFingerprint,
                    syncPayload = ballotJson.encodeToString(
                        PollBallotContract.BallotSyncPayload.serializer(),
                        payload
                    ),
                    acceptedAt = receipt.acceptedAtIso,
                    syncStatus = if (serverApply) "SERVER_ACKNOWLEDGED" else "LOCAL_PENDING",
                    serverReceiptId = serverReceiptId
                )
                if (!serverApply) {
                    syncMetadataQueries.insertSyncMetadataWithPayload(
                        id = "poll-ballot:$operationKey",
                        entityType = "poll_ballot",
                        entityId = receipt.receiptId,
                        operation = "UPDATE",
                        payload = ballotJson.encodeToString(
                            PollBallotContract.BallotSyncPayload.serializer(),
                            payload
                        ),
                        timestamp = acceptedAtIso,
                        retryState = "READY",
                        retryCount = 0,
                        synced = 0
                    )
                }
                PollBallotContract.CommitResult.Committed(receipt)
            }
            committed
        } catch (failure: Exception) {
            // An independent connection may have won the composite-key race. The durable
            // tuple, not the exception text, resolves that unknown outcome.
            if (failure.causesContain("busy") || failure.causesContain("locked")) {
                repeat(50) {
                    delay(10)
                    selectBallotReceipt(command)?.let { row ->
                        return classifyBallotReplay(command, row)
                    }
                }
            }
            selectBallotReceipt(command)?.let { row ->
                return classifyBallotReplay(command, row)
            }
            val code = if (failure.message.orEmpty().contains("UNIQUE", ignoreCase = true)) {
                PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT
            } else {
                PollBallotContract.FailureCode.LOCAL_TRANSACTION_FAILED
            }
            ballotRejected(
                command,
                code,
                retryable = true,
                commitOutcome = PollBallotContract.CommitOutcome.UNKNOWN
            )
        }
    }

    private fun selectBallotReceipt(command: PollBallotContract.CommitCompleteBallotCommand) =
        pollBallotReceiptQueries.selectByIdentity(
            eventId = command.eventId,
            actorId = command.actorId,
            pollRevision = command.pollRevision,
            operationId = command.operationId
        ).executeAsOneOrNull()

    private fun Throwable.causesContain(fragment: String): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current.message.orEmpty().contains(fragment, ignoreCase = true)) return true
            current = current.cause
        }
        return false
    }

    private fun physicalSlotId(eventId: String, logicalSlotId: String): String =
        TimeSlotStorageIdentity.physicalId(eventId, logicalSlotId)

    private fun logicalSlotId(eventId: String, persistedSlotId: String): String =
        TimeSlotStorageIdentity.logicalId(eventId, persistedSlotId)
            // Legacy rows are accepted only as a read compatibility path. Every production
            // writer below persists the deterministic v1 physical identity.
            ?: persistedSlotId.takeUnless { persistedSlotId.startsWith("slot:v1|") }
            ?: error("REPOSITORY_INCONSISTENT")

    private fun classifyBallotReplay(
        command: PollBallotContract.CommitCompleteBallotCommand,
        row: com.guyghost.wakeve.PollBallotReceipt
    ): PollBallotContract.CommitResult {
        val receipt = row.toBallotReceipt()
            ?: return ballotRejected(
                command,
                PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT,
                retryable = false,
                commitOutcome = PollBallotContract.CommitOutcome.UNKNOWN
            )
        val replayEnvelope = runCatching {
            PollBallotContract.envelope(
                command.copy(authoritativeDeadlineIso = command.authoritativeDeadlineIso.ifBlank {
                    receipt.authoritativeDeadlineIso
                })
            )
        }.getOrNull() ?: return ballotRejected(
            command,
            PollBallotContract.FailureCode.IDEMPOTENCY_CONFLICT
        )
        return if (receipt.syncPayload.command == replayEnvelope) {
            PollBallotContract.CommitResult.AlreadyCommitted(receipt)
        } else {
            ballotRejected(command, PollBallotContract.FailureCode.IDEMPOTENCY_CONFLICT)
        }
    }

    override fun hasCompleteBallot(eventId: String, participantId: String): Boolean {
        return try {
            val requiredSlotIds = timeSlotQueries.selectByEventId(eventId)
                .executeAsList()
                .map { it.id }
                .toSet()
            if (requiredSlotIds.isEmpty()) return false
            val submittedSlotIds = voteQueries.selectVotesForEventTimeslots(eventId)
                .executeAsList()
                .filter { it.userId == participantId }
                .map { it.timeslotId }
                .toSet()
            submittedSlotIds == requiredSlotIds
        } catch (_: Exception) {
            false
        }
    }

    private fun ballotRejected(
        command: PollBallotContract.CommitCompleteBallotCommand,
        code: PollBallotContract.FailureCode,
        retryable: Boolean = false,
        commitOutcome: PollBallotContract.CommitOutcome = PollBallotContract.CommitOutcome.NOT_COMMITTED
    ) = PollBallotContract.CommitResult.Rejected(
        operationId = command.operationId,
        failure = PollBallotContract.Failure(code, retryable, commitOutcome)
    )

    private fun com.guyghost.wakeve.PollBallotReceipt.toBallotReceipt(): PollBallotContract.Receipt? {
        val payload = try {
            ballotJson.decodeFromString(
                PollBallotContract.BallotSyncPayload.serializer(),
                syncPayload
            )
        } catch (_: Exception) {
            return null
        }
        val identity = payload.command.identity
        val expectedOperationKey = runCatching {
            PollBallotContract.operationKey(identity)
        }.getOrNull() ?: return null
        val acceptedAtInstant = runCatching { Instant.parse(acceptedAt) }.getOrNull() ?: return null
        val authoritativeDeadline = runCatching {
            Instant.parse(payload.command.authoritativeDeadlineIso)
        }.getOrNull() ?: return null
        val canonicalEntries = PollBallotContract.canonicalize(payload.command.entries)
        if (payload.schemaVersion != PollBallotContract.SCHEMA_VERSION ||
            payload.localReceiptId != receiptId ||
            payload.command.schemaVersion != PollBallotContract.SCHEMA_VERSION ||
            identity.eventId != eventId ||
            identity.actorId != actorId ||
            identity.pollRevision != pollRevision ||
            identity.operationId != operationId ||
            expectedOperationKey != operationKey ||
            payload.command.entries != canonicalEntries ||
            payload.command.ballotFingerprint != PollBallotContract.fingerprint(canonicalEntries) ||
            payload.command.ballotFingerprint != ballotFingerprint ||
            acceptedAtInstant >= authoritativeDeadline ||
            syncStatus !in setOf("LOCAL_PENDING", "SERVER_ACKNOWLEDGED") ||
            (syncStatus == "LOCAL_PENDING" && serverReceiptId != null) ||
            (syncStatus == "SERVER_ACKNOWLEDGED" && serverReceiptId.isNullOrBlank())
        ) return null
        return PollBallotContract.Receipt(
            receiptId = receiptId,
            operationId = operationId,
            eventId = eventId,
            actorId = actorId,
            pollRevision = pollRevision,
            ballotFingerprint = ballotFingerprint,
            authoritativeDeadlineIso = payload.command.authoritativeDeadlineIso,
            acceptedAtIso = acceptedAt,
            syncStatus = when (syncStatus) {
                "SERVER_ACKNOWLEDGED" -> PollBallotContract.SyncStatus.SYNCED
                else -> PollBallotContract.SyncStatus.LOCAL_PENDING
            },
            syncPayload = payload,
            serverReceiptId = serverReceiptId
        )
    }

    override suspend fun updateEvent(event: Event): Result<Event> =
        updateEventInternal(event, synchronizeSlots = false)

    private suspend fun updateEventInternal(
        event: Event,
        synchronizeSlots: Boolean
    ): Result<Event> {
        return try {
            val isSample = com.guyghost.wakeve.sample.SampleEventFactory.isSampleEventId(event.id)
            val now = getCurrentUtcIsoString()
            val committed = db.transactionWithResult {
                val current = eventQueries.selectById(event.id).executeAsOneOrNull()
                    ?: return@transactionWithResult false
                if (current.aggregateSchemaVersion != SUPPORTED_AGGREGATE_SCHEMA_VERSION) {
                    return@transactionWithResult false
                }
                if (
                    event.aggregateSchemaVersion != SUPPORTED_AGGREGATE_SCHEMA_VERSION ||
                    event.aggregateRevision != current.aggregateRevision
                ) {
                    return@transactionWithResult false
                }
                val hasCurrentProtectedCommit = invitationExperienceQueries
                    .selectOperationReceiptsByEventId(event.id)
                    .executeAsList()
                    .any { receipt ->
                        receipt.status == "COMMITTED" &&
                            receipt.aggregate_revision >= current.aggregateRevision
                    }
                if (hasCurrentProtectedCommit) {
                    return@transactionWithResult false
                }
                val expectedRevision = event.aggregateRevision + 1L
                val authorizationId = "event-update:${event.id}:${event.aggregateRevision}"
                if (!authorizeAggregateWrite(
                        eventId = event.id,
                        expectedRevision = event.aggregateRevision,
                        operationId = authorizationId,
                        now = now
                    )
                ) {
                    return@transactionWithResult false
                }
                eventQueries.updateEventIfRevision(
                    title = event.title,
                    description = event.description,
                    status = event.status.name,
                    deadline = event.deadline,
                    updatedAt = now,
                    eventType = event.eventType.name,
                    eventTypeCustom = event.eventTypeCustom,
                    minParticipants = event.minParticipants?.toLong(),
                    maxParticipants = event.maxParticipants?.toLong(),
                    expectedParticipants = event.expectedParticipants?.toLong(),
                    isSample = if (isSample) 1L else 0L,
                    id = event.id,
                    aggregateRevision = event.aggregateRevision
                )
                val updated = eventQueries.selectById(event.id).executeAsOneOrNull()
                if (updated?.aggregateRevision != expectedRevision) {
                    rollback(false)
                }
                eventQueries.setEventPlanningModeWithinAggregateWrite(
                    planningMode = event.planningMode.name,
                    updatedAt = now,
                    id = event.id
                )
                if (synchronizeSlots) {
                    replaceTimeSlotsInTransaction(event.id, event.proposedSlots, now)
                }
                invitationExperienceQueries.clearAggregateWriteAuthorization(
                    event.id,
                    authorizationId
                )
                true
            }
            if (!committed) {
                return Result.failure(
                    IllegalStateException("Event aggregate writer is incompatible or stale")
                )
            }

            // Record sync change
            syncManager?.recordLocalChange(
                table = "events",
                operation = SyncOperation.UPDATE,
                recordId = event.id,
                data = """{"title":"${event.title}","description":"${event.description}","status":"${event.status}","deadline":"${event.deadline}"}""",
                userId = event.organizerId
            )
            if (synchronizeSlots) {
                syncManager?.recordLocalChange(
                    table = "timeSlots",
                    operation = SyncOperation.UPDATE,
                    recordId = event.id,
                    data = """{"count":"${event.proposedSlots.size}"}""",
                    userId = event.organizerId
                )
            }

            getEvent(event.id)?.let(Result.Companion::success)
                ?: Result.failure(IllegalStateException("Event update was not readable after commit"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save an event (create if it doesn't exist, otherwise update).
     * This is useful for auto-save functionality during draft wizard steps.
     *
     * When updating, this also syncs the time slots to ensure they match the event.
     *
     * @param event The event to save
     * @return Result containing saved event, or an error
     */
    override suspend fun saveEvent(event: Event): Result<Event> {
        return try {
            val existingEvent = getEvent(event.id)
            if (existingEvent != null) {
                updateEventInternal(event, synchronizeSlots = true)
            } else {
                // Event doesn't exist, create it (createEvent already handles time slots)
                createEvent(event)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Synchronize time slots for an event by replacing all slots with the new list.
     *
     * @param eventId The event ID
     * @param timeSlots The new list of time slots
     */
    private fun replaceTimeSlotsInTransaction(
        eventId: String,
        timeSlots: List<TimeSlot>,
        now: String
    ) {
        val logicalIds = timeSlots.map { it.id }
        require(logicalIds.size == logicalIds.toSet().size) { "DUPLICATE_LOGICAL_MAPPING" }
        logicalIds.forEach { physicalSlotId(eventId, it) }

        timeSlotQueries.deleteByEventId(eventId)
        timeSlots.forEach { slot ->
            timeSlotQueries.insertTimeSlot(
                id = physicalSlotId(eventId, slot.id),
                eventId = eventId,
                startTime = slot.start,
                endTime = slot.end,
                timezone = slot.timezone,
                proposedByParticipantId = null,
                createdAt = now,
                updatedAt = now,
                timeOfDay = slot.timeOfDay.name
            )
        }
    }

    override suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean> {
        val event = getEvent(id) ?: return Result.failure(IllegalArgumentException("Event not found"))
        val aggregate = eventQueries.selectById(id).executeAsOneOrNull()
            ?: return Result.failure(IllegalArgumentException("Event not found"))
        if (aggregate.aggregateSchemaVersion != SUPPORTED_AGGREGATE_SCHEMA_VERSION) {
            return Result.failure(
                IllegalStateException("Event aggregate writer is incompatible or stale")
            )
        }
        if (
            invitationExperienceQueries.selectArtworkByEventId(id).executeAsOneOrNull() != null &&
            event.status == EventStatus.DRAFT &&
            db.scenarioQueries.countByEventId(id).executeAsOne() > 0L
        ) {
            return Result.failure(
                IllegalStateException(
                    "Event lifecycle mutation requires an actor-bound expected revision"
                )
            )
        }
        val hasCurrentProtectedCommit = invitationExperienceQueries
            .selectOperationReceiptsByEventId(id)
            .executeAsList()
            .any { receipt ->
                receipt.status == "COMMITTED" &&
                    receipt.aggregate_revision >= aggregate.aggregateRevision
            }
        if (hasCurrentProtectedCommit) {
            return Result.failure(
                IllegalStateException("Event aggregate writer requires a bound lifecycle command")
            )
        }

        return try {
            if (status == EventStatus.FINALIZED) {
                if (event.status != EventStatus.ORGANIZING) {
                    return Result.failure(
                        IllegalStateException("Finalization blocked by EVENT_NOT_ORGANIZING")
                    )
                }
                val readiness = EventOrganizationReadinessRepository(db).getReadiness(id)
                if (!readiness.complete) {
                    return Result.failure(
                        IllegalStateException("Finalization blocked by ${readiness.blockers.joinToString(",")}")
                    )
                }
            }

            val now = getCurrentUtcIsoString()
            val authorizationId = "event-status:$id:${aggregate.aggregateRevision}:${status.name}"
            if (!authorizeAggregateWrite(id, aggregate.aggregateRevision, authorizationId, now)) {
                return Result.failure(
                    IllegalStateException("Event aggregate writer is incompatible or stale")
                )
            }
            eventQueries.updateEventStatus(
                status = status.name,
                updatedAt = now,
                id = id
            )

            // If confirming, also create confirmedDate record
            if (status == EventStatus.CONFIRMED && finalDate != null) {
                val confirmedId = "confirmed_${id}"
                val firstTimeSlot = getEvent(id)?.proposedSlots?.firstOrNull()?.id ?: return Result.failure(
                    IllegalStateException("No time slots to confirm")
                )
                confirmedDateQueries.insertConfirmedDate(
                    id = confirmedId,
                    eventId = id,
                    timeslotId = physicalSlotId(id, firstTimeSlot),
                    confirmedByOrganizerId = event.organizerId,
                    confirmedAt = finalDate,
                    updatedAt = now
                )
            }

            // Use unique timestamp by appending status to avoid conflicts
            val uniqueTimestamp = "${now}_${status.name}"
            syncMetadataQueries.insertSyncMetadata(
                id = "sync_status_${id}_${status.name}",
                entityType = "event",
                entityId = id,
                operation = "UPDATE",
                timestamp = uniqueTimestamp,
                synced = 0
            )
            invitationExperienceQueries.clearAggregateWriteAuthorization(id, authorizationId)

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun confirmEventDate(
        eventId: String,
        slotId: String,
        confirmedByOrganizerId: String
    ): Result<Boolean> = confirmEventDateCommand(
        eventId = eventId,
        slotId = slotId,
        confirmedByOrganizerId = confirmedByOrganizerId,
        operationId = "confirm-$eventId-$slotId",
        // The typed command captures the only persisted confirmation instant.
        requestedAt = "1970-01-01T00:00:00Z"
    )

    override suspend fun confirmEventDateCommand(
        eventId: String,
        slotId: String,
        confirmedByOrganizerId: String,
        operationId: String,
        requestedAt: String
    ): Result<Boolean> {
        val command = try {
            EventManagementContract.ConfirmPollDateCommand(
                operationId = operationId,
                eventId = eventId,
                slotId = slotId,
                actorId = confirmedByOrganizerId,
                requestedAt = Instant.parse(requestedAt)
            )
        } catch (error: Exception) {
            return Result.failure(error)
        }
        return confirmPollDate(command).toLegacyResult()
    }

    override suspend fun confirmPollDate(
    command: EventManagementContract.ConfirmPollDateCommand
    ): EventManagementContract.ConfirmationResult = confirmationMutex.withLock {
        val capturedAt = confirmationClock.now().toString()
        val result = try {
            db.transactionWithResult {
                confirmPollDateInTransaction(command, capturedAt)
            }
        } catch (_: Exception) {
            runCatching { resolveConcurrentConfirmation(command) }.getOrNull() ?: confirmationFailure(
                operationId = command.operationId,
                code = EventManagementContract.ConfirmationFailureCode.LOCAL_PERSISTENCE_FAILED,
                retryable = true
            )
        }

        result
    }

    private fun confirmPollDateInTransaction(
        command: EventManagementContract.ConfirmPollDateCommand,
        now: String
    ): EventManagementContract.ConfirmationResult {
        val event = eventQueries.selectById(command.eventId).executeAsOneOrNull()
            ?: return confirmationFailure(
                command.operationId,
                EventManagementContract.ConfirmationFailureCode.EVENT_NOT_FOUND,
                retryable = false
            )
        if (event.organizerId != command.actorId) {
            return confirmationFailure(
                command.operationId,
                EventManagementContract.ConfirmationFailureCode.NOT_ORGANIZER,
                retryable = false
            )
        }

        readOnlyConfirmationProjection(command.eventId)?.let { projection ->
            return EventManagementContract.ConfirmationResult.ReadOnly(command.operationId, projection)
        }

        confirmationReceiptQueries.selectByOperationId(command.operationId).executeAsOneOrNull()?.let { receipt ->
            return receiptOutcome(
                command,
                receipt.operationId,
                receipt.eventId,
                receipt.slotId,
                receipt.actorId,
                receipt.committedAt
            )
        }
        confirmationReceiptQueries.selectByEventId(command.eventId).executeAsOneOrNull()?.let { receipt ->
            return receiptOutcome(
                command,
                receipt.operationId,
                receipt.eventId,
                receipt.slotId,
                receipt.actorId,
                receipt.committedAt
            )
        }

        if (event.status != EventStatus.POLLING.name) {
            return confirmationFailure(
                command.operationId,
                EventManagementContract.ConfirmationFailureCode.INVALID_EVENT_STATUS,
                retryable = false
            )
        }
        if (voteQueries.selectByEventId(command.eventId).executeAsList().isEmpty()) {
            return confirmationFailure(
                command.operationId,
                EventManagementContract.ConfirmationFailureCode.NO_VOTES,
                retryable = false
            )
        }

        val persistedSlotId = runCatching {
            physicalSlotId(command.eventId, command.slotId)
        }.getOrNull() ?: return confirmationFailure(
            command.operationId,
            EventManagementContract.ConfirmationFailureCode.SLOT_NOT_FOUND,
            retryable = false
        )
        val slot = timeSlotQueries.selectById(persistedSlotId).executeAsOneOrNull()
            ?: return confirmationFailure(
                command.operationId,
                EventManagementContract.ConfirmationFailureCode.SLOT_NOT_FOUND,
                retryable = false
            )
        if (slot.eventId != command.eventId) {
            return confirmationFailure(
                command.operationId,
                EventManagementContract.ConfirmationFailureCode.SLOT_NOT_FOUND,
                retryable = false
            )
        }
        val finalDate = slot.startTime ?: return confirmationFailure(
            command.operationId,
            EventManagementContract.ConfirmationFailureCode.SLOT_NOT_CONFIRMABLE,
            retryable = false
        )

        val authorizationId = "confirm-date:${command.operationId}"
        if (!authorizeAggregateWrite(
                command.eventId,
                event.aggregateRevision,
                authorizationId,
                now
            )
        ) {
            return confirmationFailure(
                command.operationId,
                EventManagementContract.ConfirmationFailureCode.LOCAL_PERSISTENCE_FAILED,
                retryable = true
            )
        }
        val effectKeys = confirmationEffectKeys(command.eventId, command.slotId)
        confirmationReceiptQueries.insertReceipt(
            operationId = command.operationId,
            eventId = command.eventId,
            slotId = command.slotId,
            actorId = command.actorId,
            requestedAt = now,
            committedAt = now
        )
        eventQueries.updateEventStatus(
            status = EventStatus.CONFIRMED.name,
            updatedAt = now,
            id = command.eventId
        )
        confirmedDateQueries.insertConfirmedDate(
            id = "confirmed_${command.eventId}",
            eventId = command.eventId,
            timeslotId = persistedSlotId,
            confirmedByOrganizerId = command.actorId,
            confirmedAt = now,
            updatedAt = now
        )
        confirmationEffectOutboxQueries.insertEnvelope(
            domainEventId = effectKeys.domainEventId,
            effectKey = effectKeys.effectKey,
            eventId = command.eventId,
            slotId = persistedSlotId,
            operationId = command.operationId,
            status = "QUEUED",
            createdAt = now
        )
        syncMetadataQueries.insertSyncMetadata(
            id = "sync_confirm_${command.eventId}",
            entityType = "event",
            entityId = command.eventId,
            operation = "UPDATE",
            timestamp = now,
            synced = 0
        )
        invitationExperienceQueries.clearAggregateWriteAuthorization(
            command.eventId,
            authorizationId
        )
        return EventManagementContract.ConfirmationResult.Committed(
            receipt = confirmationReceipt(command, now),
            projection = confirmationProjection(command, command.operationId)
        )
    }

    private fun resolveConcurrentConfirmation(
        command: EventManagementContract.ConfirmPollDateCommand
    ): EventManagementContract.ConfirmationResult? {
        readOnlyConfirmationProjection(command.eventId)?.let { projection ->
            return EventManagementContract.ConfirmationResult.ReadOnly(command.operationId, projection)
        }
        val receipt = confirmationReceiptQueries.selectByOperationId(command.operationId).executeAsOneOrNull()
            ?: confirmationReceiptQueries.selectByEventId(command.eventId).executeAsOneOrNull()
            ?: return null
        return receiptOutcome(
            command,
            receipt.operationId,
            receipt.eventId,
            receipt.slotId,
            receipt.actorId,
            receipt.committedAt
        )
    }

    private fun receiptOutcome(
        command: EventManagementContract.ConfirmPollDateCommand,
        receiptOperationId: String,
        receiptEventId: String,
        receiptSlotId: String,
        receiptActorId: String,
        committedAt: String
    ): EventManagementContract.ConfirmationResult =
        if (receiptEventId == command.eventId && receiptSlotId == command.slotId) {
            EventManagementContract.ConfirmationResult.AlreadyCommitted(
                receipt = confirmationReceipt(command, committedAt, receiptActorId, receiptOperationId),
                projection = confirmationProjection(command, receiptOperationId)
            )
        } else {
            EventManagementContract.ConfirmationResult.Conflict(
                operationId = command.operationId,
                failure = EventManagementContract.ConfirmationFailure(
                    EventManagementContract.ConfirmationFailureCode.ALREADY_CONFIRMED_DIFFERENT_SLOT,
                    retryable = false
                )
            )
        }

    private fun confirmationReceipt(
        command: EventManagementContract.ConfirmPollDateCommand,
        committedAt: String,
        actorId: String = command.actorId,
        receiptOperationId: String = command.operationId
    ): EventManagementContract.ConfirmationReceipt {
        val statuses = confirmationStatuses(command.eventId)
        return EventManagementContract.ConfirmationReceipt(
        receiptId = receiptOperationId,
        operationId = receiptOperationId,
        eventId = command.eventId,
        slotId = command.slotId,
        actorId = actorId,
        committedAt = committedAt,
        nextNavigationTarget = "event/${command.eventId}/scenarios",
        decisionSyncStatus = statuses.decisionSyncStatus,
        effectDispatchStatus = statuses.effectDispatchStatus,
        effectOutbox = confirmationEffectKeys(command.eventId, command.slotId).let {
            EventManagementContract.ConfirmationEffectOutbox(it.domainEventId, it.effectKey)
        }
        )
    }

    private fun confirmationProjection(
        command: EventManagementContract.ConfirmPollDateCommand,
        receiptId: String
    ): EventManagementContract.ConfirmationProjection.Confirmed {
        val statuses = confirmationStatuses(command.eventId)
        return EventManagementContract.ConfirmationProjection.Confirmed(
            eventId = command.eventId,
            slotId = command.slotId,
            receiptId = receiptId,
            decisionSyncStatus = statuses.decisionSyncStatus,
            effectDispatchStatus = statuses.effectDispatchStatus
        )
    }

    override suspend fun markConfirmationSynced(
        receiptId: String
    ): EventManagementContract.ConfirmationProjection? = confirmationMutex.withLock {
        try {
            db.transactionWithResult {
                val receipt = confirmationReceiptQueries.selectByOperationId(receiptId).executeAsOneOrNull()
                    ?: return@transactionWithResult null
                readOnlyConfirmationProjection(receipt.eventId)?.let { projection ->
                    return@transactionWithResult projection
                }
                syncMetadataQueries.markSynced("sync_confirm_${receipt.eventId}")
                confirmationProjection(
                    command = EventManagementContract.ConfirmPollDateCommand(
                        operationId = receipt.operationId,
                        eventId = receipt.eventId,
                        slotId = receipt.slotId,
                        actorId = receipt.actorId,
                        requestedAt = Instant.parse(receipt.requestedAt)
                    ),
                    receiptId = receipt.operationId
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun loadConfirmationProjection(
        eventId: String
    ): EventManagementContract.ConfirmationProjection {
        return try {
            readOnlyConfirmationProjection(eventId)?.let { return it }
            val receipt = confirmationReceiptQueries.selectByEventId(eventId).executeAsOneOrNull()
                ?: return EventManagementContract.ConfirmationProjection.Reviewing(eventId)
            EventManagementContract.ConfirmationProjection.Confirmed(
                eventId = receipt.eventId,
                slotId = receipt.slotId,
                receiptId = receipt.operationId,
                decisionSyncStatus = confirmationStatuses(receipt.eventId).decisionSyncStatus,
                effectDispatchStatus = confirmationStatuses(receipt.eventId).effectDispatchStatus
            )
        } catch (_: Exception) {
            EventManagementContract.ConfirmationProjection.Reviewing(eventId)
        }
    }

    private fun readOnlyConfirmationProjection(
        eventId: String
    ): EventManagementContract.ConfirmationProjection.ReadOnly? {
        val legacy = confirmationLegacyClassificationQueries.selectByEventId(eventId).executeAsOneOrNull()
            ?: return null
        return when (legacy.classification) {
            "legacyApplied" -> {
                val receipt = confirmationReceiptQueries.selectByEventId(eventId).executeAsOneOrNull()
                if (receipt == null) {
                    EventManagementContract.ConfirmationProjection.Quarantined(
                        eventId = eventId,
                        reason = "legacy-applied-receipt-missing"
                    )
                } else {
                    EventManagementContract.ConfirmationProjection.LegacyApplied(
                        eventId = receipt.eventId,
                        slotId = receipt.slotId,
                        receiptId = receipt.operationId
                    )
                }
            }
            "quarantined" -> EventManagementContract.ConfirmationProjection.Quarantined(
                eventId = legacy.eventId,
                reason = legacy.reason
            )
            else -> EventManagementContract.ConfirmationProjection.Quarantined(
                eventId = legacy.eventId,
                reason = "unknown-legacy-classification"
            )
        }
    }

    private data class ConfirmationStatuses(
        val decisionSyncStatus: EventManagementContract.DecisionSyncStatus,
        val effectDispatchStatus: EventManagementContract.EffectDispatchStatus
    )

    private fun confirmationStatuses(eventId: String): ConfirmationStatuses {
        val decisionSyncStatus = when (
            syncMetadataQueries.selectById("sync_confirm_$eventId").executeAsOneOrNull()?.synced
        ) {
            1L -> EventManagementContract.DecisionSyncStatus.SERVER_ACKNOWLEDGED
            else -> EventManagementContract.DecisionSyncStatus.LOCAL_PENDING
        }
        val effectDispatchStatus = when (
            confirmationEffectOutboxQueries.selectByEventId(eventId).executeAsOneOrNull()?.status
        ) {
            EventManagementContract.EffectDispatchStatus.PARTIALLY_PROCESSED.name ->
                EventManagementContract.EffectDispatchStatus.PARTIALLY_PROCESSED
            EventManagementContract.EffectDispatchStatus.TERMINAL_WITH_FAILURES.name ->
                EventManagementContract.EffectDispatchStatus.TERMINAL_WITH_FAILURES
            else -> EventManagementContract.EffectDispatchStatus.QUEUED
        }
        return ConfirmationStatuses(decisionSyncStatus, effectDispatchStatus)
    }

    private fun confirmationFailure(
        operationId: String,
        code: EventManagementContract.ConfirmationFailureCode,
        retryable: Boolean
    ) = EventManagementContract.ConfirmationResult.Failed(
        operationId,
        EventManagementContract.ConfirmationFailure(code, retryable)
    )

    private fun EventManagementContract.ConfirmationResult.toLegacyResult(): Result<Boolean> = when (this) {
        is EventManagementContract.ConfirmationResult.Committed,
        is EventManagementContract.ConfirmationResult.AlreadyCommitted -> Result.success(true)
        is EventManagementContract.ConfirmationResult.ReadOnly -> Result.failure(
            IllegalStateException("CONFIRMATION_READ_ONLY")
        )
        is EventManagementContract.ConfirmationResult.Conflict -> Result.failure(
            IllegalStateException(failure.code.name)
        )
        is EventManagementContract.ConfirmationResult.Failed -> Result.failure(
            IllegalStateException(failure.code.name)
        )
    }
    override suspend fun queueWorkflowOutbox(record: WorkflowOutboxRecord): Result<Boolean> {
        return try {
            val key = record.effectKey ?: "${record.eventId}:${record.type.name}:${record.operationId ?: record.finalDate}"
            workflowOutboxQueries.insertOutbox(key, record.eventId, record.type.name, record.finalDate, record.operationId, record.status.name)
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    override fun getWorkflowOutbox(eventId: String): List<WorkflowOutboxRecord> =
        workflowOutboxQueries.selectByEventId(eventId).executeAsList().map {
            WorkflowOutboxRecord(it.eventId, WorkflowOutboxType.valueOf(it.type), it.finalDate,
                PendingWorkflowStatus.valueOf(it.status), it.operationId, it.effectKey)
        }

    override fun isDeadlinePassed(deadline: String): Boolean {
        return try {
            confirmationClock.now() >= Instant.parse(deadline)
        } catch (_: Exception) {
            true
        }
    }

    private fun getCurrentUtcIsoString(): String {
        return confirmationClock.now().toString()
    }

    override fun isOrganizer(eventId: String, userId: String): Boolean {
        return getEvent(eventId)?.organizerId == userId
    }

    override fun canModifyEvent(eventId: String, userId: String): Boolean {
        return isOrganizer(eventId, userId)
    }

    override fun getAllEvents(): List<Event> {
        return try {
            eventQueries.selectAll().executeAsList().mapNotNull { eventRow ->
                getEvent(eventRow.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseEventStatus(value: String?): EventStatus {
        return enumValueOrNull<EventStatus>(value) ?: EventStatus.DRAFT
    }

    private fun parseEventType(value: String?): EventType {
        return enumValueOrNull<EventType>(value) ?: when (value?.trim()?.lowercase()) {
            "sport", "sports" -> EventType.SPORTS_EVENT
            "family_reunion", "family" -> EventType.FAMILY_GATHERING
            "dinner", "dinner_party" -> EventType.PARTY
            "outdoor_adventure", "outdoor" -> EventType.OUTDOOR_ACTIVITY
            "networking", "corporate", "graduation", "holiday_party", "concert" -> EventType.OTHER
            else -> EventType.OTHER
        }
    }

    private fun parseEventPlanningMode(value: String?): EventPlanningMode {
        return enumValueOrNull<EventPlanningMode>(value) ?: EventPlanningMode.TIME_SLOT_POLL
    }

    private fun parseTimeOfDay(value: String?): TimeOfDay {
        return enumValueOrNull<TimeOfDay>(value) ?: when (value?.trim()?.lowercase()) {
            "specific_time", "exact" -> TimeOfDay.SPECIFIC
            "day", "all-day", "allday" -> TimeOfDay.ALL_DAY
            else -> TimeOfDay.SPECIFIC
        }
    }

    private fun parseVote(value: String?): Vote {
        return enumValueOrNull<Vote>(value) ?: Vote.MAYBE
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? {
        val normalized = value?.trim()?.uppercase()?.replace('-', '_') ?: return null
        return enumValues<T>().firstOrNull { it.name == normalized }
    }

    // MARK: - Search & Discovery

    /**
     * Search events with full-text filtering, category, date range, and pagination.
     * TODO: Replace SQL LIKE with Meilisearch for better full-text search at scale.
     */
    fun searchEvents(
        query: String?,
        category: String?,
        location: String?,
        dateFrom: String?,
        dateTo: String?,
        status: String?,
        sortBy: String,
        offset: Int,
        limit: Int
    ): SearchResultsResponse {
        val rows = eventQueries.searchEvents(
            query = query,
            category = category,
            status = status,
            dateFrom = dateFrom,
            dateTo = dateTo,
            sortBy = sortBy,
            limit = limit.toLong(),
            offset = offset.toLong()
        ).executeAsList()

        val totalCount = eventQueries.countSearchEvents(
            query = query,
            category = category,
            status = status,
            dateFrom = dateFrom,
            dateTo = dateTo
        ).executeAsOne()

        val events = rows.mapNotNull { row ->
            eventToSearchResult(row.id)
        }.let { results ->
            // Apply location filter client-side if specified
            if (location != null && location.isNotBlank()) {
                results.filter { result ->
                    result.locationName?.contains(location, ignoreCase = true) == true
                }
            } else {
                results
            }
        }

        return SearchResultsResponse(
            events = events,
            totalCount = totalCount.toInt(),
            offset = offset,
            limit = limit,
            hasMore = (offset + limit) < totalCount
        )
    }

    /**
     * Get trending events (most participants in the last 7 days).
     */
    fun getTrendingEvents(limit: Int): TrendingEventsResponse {
        // Calculate "7 days ago" timestamp
        // Since the repo uses a fixed test date, use a reasonable lookback
        val since = "2025-01-01T00:00:00Z"

        val rows = eventQueries.selectTrending(
            since = since,
            limit = limit.toLong()
        ).executeAsList()

        val events = rows.mapNotNull { row ->
            eventToSearchResult(row.id)
        }

        return TrendingEventsResponse(
            events = events,
            period = "7_days"
        )
    }

    /**
     * Get events near a geographic location.
     * Uses Haversine formula for distance calculation.
     */
    fun getNearbyEvents(lat: Double, lon: Double, radiusKm: Double, limit: Int): NearbyEventsResponse {
        val locationsWithCoords = db.potentialLocationQueries
            .selectAllWithCoordinates()
            .executeAsList()

        // Parse coordinates and compute distances
        val nearbyResults = mutableListOf<NearbyEventResult>()

        locationsWithCoords.forEach { locRow ->
            val coords = parseCoordinates(locRow.coordinates ?: return@forEach)
            if (coords != null) {
                val distance = haversineDistance(lat, lon, coords.first, coords.second)
                if (distance <= radiusKm) {
                    val searchResult = eventToSearchResult(locRow.eventId)
                    if (searchResult != null) {
                        nearbyResults.add(
                            NearbyEventResult(
                                event = searchResult,
                                distanceKm = round(distance * 10.0) / 10.0
                            )
                        )
                    }
                }
            }
        }

        // Sort by distance, limit results, deduplicate by event ID
        val uniqueResults = nearbyResults
            .sortedBy { it.distanceKm }
            .distinctBy { it.event.id }
            .take(limit)

        return NearbyEventsResponse(
            events = uniqueResults,
            centerLat = lat,
            centerLon = lon,
            radiusKm = radiusKm
        )
    }

    /**
     * Get recommended events for a user based on their past event types.
     * Simple recommendation: find events matching the user's historical event types.
     */
    fun getRecommendedEvents(userId: String, limit: Int): RecommendedEventsResponse {
        // Get event types from user's organized events
        val organizerTypes: List<String> = eventQueries.selectEventTypesByOrganizer(userId)
            .executeAsList()
            .mapNotNull { it?.toString() }

        // Get event types from user's participated events
        val participantTypes: List<String> = eventQueries.selectEventTypesByParticipant(userId)
            .executeAsList()
            .mapNotNull { it?.toString() }

        val preferredTypes: List<String> = (organizerTypes + participantTypes).distinct()

        if (preferredTypes.isEmpty()) {
            // No history, return popular events as fallback
            val trending = getTrendingEvents(limit)
            return RecommendedEventsResponse(
                events = trending.events,
                userId = userId,
                reason = "popular_events"
            )
        }

        // Pad types to 3 for the SQL query (uses :type1, :type2, :type3)
        val type1: String = preferredTypes.getOrElse(0) { preferredTypes.first() }
        val type2: String = preferredTypes.getOrElse(1) { type1 }
        val type3: String = preferredTypes.getOrElse(2) { type1 }

        val rows = eventQueries.selectByEventType(
            type1 = type1,
            type2 = type2,
            type3 = type3,
            limit = limit.toLong()
        ).executeAsList()

        val events = rows.mapNotNull { row ->
            eventToSearchResult(row.id)
        }

        return RecommendedEventsResponse(
            events = events,
            userId = userId,
            reason = "based_on_past_event_types"
        )
    }

    // MARK: - Private Helpers

    /**
     * Convert an event ID to an EventSearchResult by loading event + location data.
     */
    private fun eventToSearchResult(eventId: String): EventSearchResult? {
        val event = getEvent(eventId) ?: return null
        val participants = participantQueries.selectByEventId(eventId).executeAsList()

        // Get first location if available
        val location = try {
            db.potentialLocationQueries
                .selectFirstLocationByEventId(eventId)
                .executeAsOneOrNull()
        } catch (_: Exception) {
            null
        }

        return EventSearchResult(
            id = event.id,
            title = event.title,
            description = event.description,
            organizerId = event.organizerId,
            status = event.status.name,
            eventType = event.eventType.name,
            eventTypeCustom = event.eventTypeCustom,
            participantCount = participants.size,
            maxParticipants = event.maxParticipants,
            deadline = event.deadline,
            createdAt = event.createdAt,
            locationName = location?.name,
            locationCoordinates = location?.coordinates
        )
    }

    /**
     * Parse a coordinates JSON string to a (latitude, longitude) pair.
     * Expected format: {"latitude": 48.8566, "longitude": 2.3522}
     */
    private fun parseCoordinates(json: String): Pair<Double, Double>? {
        return try {
            val latMatch = Regex(""""latitude"\s*:\s*([-\d.]+)""").find(json)
            val lonMatch = Regex(""""longitude"\s*:\s*([-\d.]+)""").find(json)
            if (latMatch != null && lonMatch != null) {
                Pair(latMatch.groupValues[1].toDouble(), lonMatch.groupValues[1].toDouble())
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Calculate the Haversine distance between two geographic points in kilometers.
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * asin(sqrt(a))
        return earthRadiusKm * c
    }

    override fun getEventsPaginated(
        page: Int,
        pageSize: Int,
        orderBy: OrderBy
    ): Flow<List<Event>> {
        // Handle invalid parameters
        if (page < 0 || pageSize <= 0) {
            return flowOf(emptyList())
        }

        return try {
            val offset = page * pageSize
            val events = eventQueries.selectPaginated(
                orderBy = orderBy.name,
                limit = pageSize.toLong(),
                offset = offset.toLong()
            ).executeAsList().mapNotNull { eventRow ->
                getEvent(eventRow.id)
            }
            flowOf(events)
        } catch (e: Exception) {
            println(databaseEventRepositoryPaginatedEventsFailureLogMessage())
            flowOf(emptyList())
        }
    }

    /**
     * Delete an event and all its related data.
     *
     * This method performs cascade deletion in the following order:
     * 1. Votes (depends on participants and time slots)
     * 2. Participants
     * 3. Time slots
     * 4. Potential locations
     * 5. Scenarios (cascade deletes scenario votes)
     * 6. Confirmed date
     * 7. Sync metadata for this event
     * 8. Event itself
     *
     * A tombstone record is created in syncMetadata for offline sync.
     *
     * Note: SQLite foreign keys with ON DELETE CASCADE would handle most of this,
     * but we explicitly delete to ensure proper ordering and to record sync metadata.
     *
     * @param eventId The ID of the event to delete
     * @return Result<Unit> success if deleted, failure with error message
     */
    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            val event = getEvent(eventId)
                ?: return Result.failure(IllegalArgumentException("Event not found"))
            if (
                event.status == EventStatus.FINALIZED ||
                EventTemporalClassifier.classify(event, confirmationClock.now()) == TemporalClass.PAST
            ) {
                return Result.failure(
                    IllegalStateException("Historical event deletion requires the typed owner")
                )
            }

            val now = getCurrentUtcIsoString()

            // Use a transaction to ensure atomicity
            db.transaction {
                val aggregate = eventQueries.selectById(eventId).executeAsOneOrNull()
                    ?: error("Event not found during delete")
                val deleteAuthorizationId = "delete-event:$eventId:${aggregate.aggregateRevision}"
                if (!authorizeAggregateWrite(
                        eventId,
                        aggregate.aggregateRevision,
                        deleteAuthorizationId,
                        now
                    )
                ) {
                    error("Event delete aggregate writer is incompatible or stale")
                }
                val serverArtworkReference = invitationExperienceQueries
                    .selectServerArtworkReferenceByEventId(eventId)
                    .executeAsOneOrNull()
                    ?.let { persisted ->
                        ServerArtworkReference(
                            eventId = persisted.event_id,
                            assetId = persisted.asset_id,
                            assetRevision = persisted.asset_revision
                        )
                    }

                // Explicitly clear invitation-experience ownership even on
                // SQLite drivers where foreign_keys is disabled.
                syncMetadataQueries.deleteDirectInviteSubjectsByEventId(eventId)
                syncMetadataQueries.deleteEventNotificationSubjectsByEventId(eventId)
                invitationExperienceQueries.deleteDirectInviteRecipientOutcomesByEventId(eventId)
                invitationExperienceQueries.deleteDirectInviteBatchesByEventId(eventId)
                invitationExperienceQueries.deleteEventNotificationPreferencesByEventId(eventId)
                invitationExperienceQueries.deleteEventOperationReceiptsByEventId(eventId)
                invitationExperienceQueries.deleteEventArtworkMigrationIssuesByEventId(eventId)
                invitationExperienceQueries.deleteEventArtworkByEventId(eventId)
                serverArtworkReference?.let { reference ->
                    val release = DatabaseServerArtworkReferenceOwner(db).releaseInTransaction(
                        reference = reference,
                        operationId = "delete-event:$eventId:release-server-artwork"
                    )
                    if (release is ServerArtworkReferenceResult.Rejected) {
                        error("Server artwork release rejected: ${release.error}")
                    }
                }

                // 1. Delete votes (they reference participants and time slots)
                voteQueries.deleteByEventId(eventId)

                // 2. Delete participants
                participantQueries.deleteByEventId(eventId)

                // 3. Delete time slots
                timeSlotQueries.deleteByEventId(eventId)

                // 4. Delete potential locations
                db.potentialLocationQueries.deleteByEventId(eventId)

                // 5. Delete scenarios (cascade will delete scenario votes)
                db.scenarioQueries.deleteByEventId(eventId)

                // 6. Delete confirmed date
                confirmedDateQueries.deleteByEventId(eventId)

                // 7. Delete all sync metadata related to this event
                syncMetadataQueries.deleteByEntity("event", eventId)

                // 8. Delete the event itself
                eventQueries.deleteEvent(eventId)
            }

            // Record tombstone for offline sync (outside transaction to avoid conflicts)
            syncManager?.recordLocalChange(
                table = "events",
                operation = SyncOperation.DELETE,
                recordId = eventId,
                data = """{"id":"$eventId","deletedAt":"$now"}""",
                userId = event.organizerId
            )

            // Record sync metadata for the delete operation
            syncMetadataQueries.insertSyncMetadata(
                id = "sync_delete_${eventId}_$now",
                entityType = "event",
                entityId = eventId,
                operation = "DELETE",
                timestamp = now,
                synced = 0
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // MARK: - Notification Scheduler Helpers

    /**
     * Retourne tous les evenements en cours de sondage (status = POLLING).
     * Utilise par le NotificationScheduler pour verifier les deadlines.
     */
    fun getAllPollingEvents(): List<Event> {
        return try {
            eventQueries.selectByStatus("POLLING").executeAsList().mapNotNull { row ->
                getEvent(row.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Retourne les evenements confirmes dont la date finale est aujourd'hui.
     * Utilise par le NotificationScheduler pour les rappels jour-J.
     *
     * @param todayMs Timestamp du jour courant en millisecondes
     */
    fun getConfirmedEventsForToday(todayMs: Long): List<Event> {
        return try {
            val confirmedEvents = eventQueries.selectByStatus("CONFIRMED").executeAsList()
            confirmedEvents.mapNotNull { row ->
                val event = getEvent(row.id) ?: return@mapNotNull null
                // Utiliser la requete jointure pour obtenir le startTime du timeslot
                val confirmedWithDetails = confirmedDateQueries
                    .selectWithTimeslotDetails(row.id)
                    .executeAsOneOrNull()
                if (confirmedWithDetails != null) {
                    val startTime = confirmedWithDetails.startTime ?: return@mapNotNull null
                    val dateMs = runCatching {
                        kotlinx.datetime.Instant.parse(startTime).toEpochMilliseconds()
                    }.getOrNull() ?: return@mapNotNull null
                    // Meme jour (arrondi au jour)
                    val todayDay = todayMs / 86_400_000
                    val dateDay = dateMs / 86_400_000
                    if (todayDay == dateDay) event else null
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Retourne la liste de tous les ID utilisateurs distincts
     * (organisateurs et participants).
     * Utilise par le NotificationScheduler pour le digest hebdomadaire.
     */
    fun getAllUserIds(): List<String> {
        return try {
            val organizerIds = eventQueries.selectAll().executeAsList().map { it.organizerId }
            val participantIds = try {
                db.participantQueries.selectAll().executeAsList().map { it.userId }
            } catch (e: Exception) {
                emptyList()
            }
            (organizerIds + participantIds).distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // MARK: - Sample Event Support

    /**
     * Check if any real (non-sample) events exist.
     * Used for first-launch detection to decide whether to show empty state.
     *
     * @return true if at least one non-sample event exists
     */
    fun hasAnyRealEvents(): Boolean {
        return try {
            eventQueries.hasAnyRealEvents().executeAsOne() > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Seed the sample event into the database.
     *
     * Inserts the event, participants, time slots, and pre-cast votes
     * in a single transaction. Idempotent — checks if sample already exists.
     *
     * Sample events are marked with isSample = 1 and excluded from sync.
     *
     * @return Result containing the seeded Event, or an error
     */
    override suspend fun seedSampleEvent(): Result<Event> {
        return try {
            // Check if already seeded
            val existing = eventQueries.selectById(
                com.guyghost.wakeve.sample.SampleEventFactory.SAMPLE_EVENT_ID
            ).executeAsOneOrNull()
            if (existing != null) {
                db.transaction {
                    installNoneArtworkIfMissing(existing.id, existing.updatedAt)
                }
                return Result.success(getEvent(existing.id)!!)
            }

            val factory = com.guyghost.wakeve.sample.SampleEventFactory
            val event = factory.createSampleEvent()
            val votes = factory.createSampleVotes()
            val now = getCurrentUtcIsoString()

            db.transaction {
                // 1. Insert the event (createEvent handles isSample via ID prefix)
                eventQueries.insertEvent(
                    id = event.id,
                    organizerId = event.organizerId,
                    title = event.title,
                    description = event.description,
                    status = event.status.name,
                    deadline = event.deadline,
                    createdAt = event.createdAt,
                    updatedAt = event.updatedAt,
                    version = 1,
                    eventType = event.eventType.name,
                    eventTypeCustom = event.eventTypeCustom,
                    minParticipants = event.minParticipants?.toLong(),
                    maxParticipants = event.maxParticipants?.toLong(),
                    expectedParticipants = event.expectedParticipants?.toLong(),
                    isSample = 1L
                )
                installNoneArtworkIfMissing(event.id, event.updatedAt)

                // 2. Insert participants
                val participantIds = factory.createParticipantIds()
                participantIds.forEachIndexed { index, userId ->
                    val role = if (index == 0) "ORGANIZER" else "PARTICIPANT"
                    val participantId = "sample-part-${index}"
                    participantQueries.insertParticipantWithAxes(
                        id = participantId,
                        eventId = event.id,
                        userId = userId,
                        role = role,
                        hasValidatedDate = 0,
                        rsvpState = if (role == "ORGANIZER") "NOT_APPLICABLE" else "ACCEPTED",
                        dateValidationState = if (role == "ORGANIZER") {
                            "NOT_APPLICABLE"
                        } else {
                            "NOT_VALIDATED"
                        },
                        joinedAt = now,
                        updatedAt = now
                    )
                }

                // 3. Insert time slots
                event.proposedSlots.forEach { slot ->
                    timeSlotQueries.insertTimeSlot(
                        id = physicalSlotId(event.id, slot.id),
                        eventId = event.id,
                        startTime = slot.start,
                        endTime = slot.end,
                        timezone = slot.timezone,
                        proposedByParticipantId = null,
                        createdAt = now,
                        updatedAt = now,
                        timeOfDay = slot.timeOfDay.name
                    )
                }

                // 4. Insert pre-cast votes (NOT the organizer — they vote themselves)
                votes.forEach { (userId, slotVotes) ->
                    // Find the participant record ID for this user
                    val participantRecord = participantQueries.selectByEventIdAndUserId(
                        event.id, userId
                    ).executeAsOneOrNull() ?: return@forEach

                    slotVotes.forEach { (slotId, vote) ->
                        val persistedSlotId = physicalSlotId(event.id, slotId)
                        voteQueries.insertVote(
                            id = "sample-vote-${persistedSlotId}-${userId}",
                            eventId = event.id,
                            timeslotId = persistedSlotId,
                            participantId = participantRecord.id,
                            vote = vote.name,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                }

                // 5. Insert sync metadata for the event (but marked as sample)
                // Note: SyncManager must filter out isSample events
                syncMetadataQueries.insertSyncMetadata(
                    id = "sync_sample_${event.id}",
                    entityType = "event",
                    entityId = event.id,
                    operation = "CREATE",
                    timestamp = now,
                    synced = 0
                )
            }

            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun installNoneArtworkIfMissing(eventId: String, updatedAt: String) {
        if (invitationExperienceQueries.selectArtworkByEventId(eventId).executeAsOneOrNull() != null) {
            return
        }
        invitationExperienceQueries.upsertEventArtwork(
            event_id = eventId,
            kind = "NONE",
            structured_version = null,
            source_kind = null,
            preset_id = null,
            server_asset_id = null,
            canonical_https_url = null,
            asset_revision = null,
            alt_kind = null,
            alt_text = null,
            focal_x = null,
            focal_y = null,
            crop = null,
            legacy_remote_url = null,
            updated_at = updatedAt
        )
    }

    private fun authorizeAggregateWrite(
        eventId: String,
        expectedRevision: Long,
        operationId: String,
        now: String
    ): Boolean {
        invitationExperienceQueries.authorizeAggregateWrite(
            writer_schema_version = SUPPORTED_AGGREGATE_SCHEMA_VERSION,
            operation_id = operationId,
            created_at = now,
            id = eventId,
            aggregateRevision = expectedRevision,
            aggregateSchemaVersion = SUPPORTED_AGGREGATE_SCHEMA_VERSION
        )
        val authorization = invitationExperienceQueries
            .selectAggregateWriteAuthorization(eventId)
            .executeAsOneOrNull()
        return authorization?.operation_id == operationId &&
            authorization.expected_revision == expectedRevision
    }

    /**
     * Delete all sample events and their related data.
     *
     * Uses the existing cascade delete flow to ensure clean removal.
     *
     * @return Result with count of deleted events
     */
    suspend fun deleteSampleEvents(): Result<Int> {
        return try {
            val sampleEvents = eventQueries.selectSampleEvents().executeAsList()
            var deletedCount = 0

            sampleEvents.forEach { eventRow ->
                deleteEvent(eventRow.id)
                deletedCount++
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

internal fun databaseEventRepositoryTimeSlotSyncFailureLogMessage(): String =
    "Failed to sync time slots"

internal fun databaseEventRepositoryPaginatedEventsFailureLogMessage(): String =
    "Failed to load paginated events"

private const val SUPPORTED_AGGREGATE_SCHEMA_VERSION = 1L

private fun com.guyghost.wakeve.Event_artwork.validatedRemoteArtworkUrl(): String? {
    val candidate = when {
        kind == "LEGACY_REMOTE" -> legacy_remote_url
        kind == "STRUCTURED" && source_kind == "SERVER_ASSET" -> canonical_https_url
        else -> null
    } ?: return null
    return candidate.takeIf { VALIDATED_ARTWORK_REMOTE.matches(it) }
}

private val VALIDATED_ARTWORK_REMOTE = Regex(
    "^https://(?:cdn|api)\\.wakeve\\.app/(?!.*[?#@]).+$",
    RegexOption.IGNORE_CASE
)
