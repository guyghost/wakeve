package com.guyghost.wakeve.repository

import com.guyghost.wakeve.access.ParticipantRepositoryRecord
import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.confirmation.SystemConfirmationClock
import com.guyghost.wakeve.confirmation.confirmationEffectKeys
import com.guyghost.wakeve.database.WakeveDb
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
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.repository.OrderBy
import com.guyghost.wakeve.sync.SyncManager
import com.guyghost.wakeve.workflow.WorkflowOutboxRecord
import com.guyghost.wakeve.workflow.WorkflowOutboxType
import com.guyghost.wakeve.workflow.PendingWorkflowStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
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
    private val confirmationMutex = Mutex()

    override suspend fun createEvent(event: Event): Result<Event> {
        return try {
            val now = getCurrentUtcIsoString()
            // Determine isSample flag from ID prefix
            val isSample = com.guyghost.wakeve.sample.SampleEventFactory.isSampleEventId(event.id)

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
            eventQueries.updateEventPlanningMode(
                planningMode = event.planningMode.name,
                updatedAt = now,
                id = event.id
            )

            // Insert organizer as participant
            val organizerId = "org_${event.id}"
            participantQueries.insertParticipant(
                id = organizerId,
                eventId = event.id,
                userId = event.organizerId,
                role = "ORGANIZER",
                hasValidatedDate = 0,
                joinedAt = now,
                updatedAt = now
            )

            // Insert proposed time slots
            event.proposedSlots.forEach { slot ->
                timeSlotQueries.insertTimeSlot(
                    id = slot.id,
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

            // Record creation in sync metadata
            // Record sync change for offline tracking
            syncManager?.recordLocalChange(
                table = "events",
                operation = SyncOperation.CREATE,
                recordId = event.id,
                data = """{"id":"${event.id}","title":"${event.title}","description":"${event.description}","organizerId":"${event.organizerId}","deadline":"${event.deadline}"}""",
                userId = event.organizerId
            )

            syncMetadataQueries.insertSyncMetadata(
                id = "sync_${event.id}",
                entityType = "event",
                entityId = event.id,
                operation = "CREATE",
                timestamp = now,
                synced = 0
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

            Event(
                id = eventRow.id,
                title = eventRow.title,
                description = eventRow.description,
                organizerId = eventRow.organizerId,
                participants = participants.map { it.userId },
                proposedSlots = timeSlots.map {
                    TimeSlot(
                        id = it.id,
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
                planningMode = parseEventPlanningMode(eventRow.planningMode)
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
            val slotId = voteRow.timeslotId
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
            participantQueries.insertParticipant(
                id = newParticipantId,
                eventId = eventId,
                userId = participantId,
                role = "PARTICIPANT",
                hasValidatedDate = 0,
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
                    role = if (participant.role == "ORGANIZER") "ORGANIZER" else "MEMBER",
                    rsvp = if (participant.hasValidatedDate == 1L) "ACCEPTED" else "PENDING",
                    hasValidatedDate = participant.hasValidatedDate
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
            val voteId = "vote_${slotId}_${participantId}"
            voteQueries.insertVote(
                id = voteId,
                eventId = eventId,
                timeslotId = slotId,
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

    override suspend fun updateEvent(event: Event): Result<Event> {
        return try {
            val isSample = com.guyghost.wakeve.sample.SampleEventFactory.isSampleEventId(event.id)
            val now = getCurrentUtcIsoString()
            eventQueries.updateEvent(
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
                id = event.id
            )
            eventQueries.updateEventPlanningMode(
                planningMode = event.planningMode.name,
                updatedAt = now,
                id = event.id
            )

            // Record sync change
            syncManager?.recordLocalChange(
                table = "events",
                operation = SyncOperation.UPDATE,
                recordId = event.id,
                data = """{"title":"${event.title}","description":"${event.description}","status":"${event.status}","deadline":"${event.deadline}"}""",
                userId = event.organizerId
            )

            Result.success(event)
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
                // Event exists, update it
                updateEvent(event)

                // Also update time slots
                syncTimeSlots(event.id, event.proposedSlots)
            } else {
                // Event doesn't exist, create it (createEvent already handles time slots)
                createEvent(event)
            }
            Result.success(event)
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
    private suspend fun syncTimeSlots(eventId: String, timeSlots: List<TimeSlot>) {
        try {
            // Delete existing time slots for this event
            timeSlotQueries.deleteByEventId(eventId)

            // Insert new time slots
            val now = getCurrentUtcIsoString()
            timeSlots.forEach { slot ->
                timeSlotQueries.insertTimeSlot(
                    id = slot.id,
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

            // Record sync change
            syncManager?.recordLocalChange(
                table = "timeSlots",
                operation = SyncOperation.UPDATE,
                recordId = eventId,
                data = """{"count":"${timeSlots.size}"}""",
                userId = getEvent(eventId)?.organizerId ?: "unknown"
            )
        } catch (e: Exception) {
            // Log error but don't fail the entire save operation
            println(databaseEventRepositoryTimeSlotSyncFailureLogMessage())
        }
    }

    override suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean> {
        val event = getEvent(id) ?: return Result.failure(IllegalArgumentException("Event not found"))

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
                    timeslotId = firstTimeSlot,
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

        val slot = timeSlotQueries.selectById(command.slotId).executeAsOneOrNull()
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
            timeslotId = command.slotId,
            confirmedByOrganizerId = command.actorId,
            confirmedAt = now,
            updatedAt = now
        )
        confirmationEffectOutboxQueries.insertEnvelope(
            domainEventId = effectKeys.domainEventId,
            effectKey = effectKeys.effectKey,
            eventId = command.eventId,
            slotId = command.slotId,
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
            deadline < getCurrentUtcIsoString()
        } catch (e: Exception) {
            false
        }
    }

    private fun getCurrentUtcIsoString(): String {
        // For Phase 1, we use a fixed test date
        // In Phase 2, integrate with kotlinx.datetime for full timezone support
        return "2025-11-12T10:00:00Z"
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

            val now = getCurrentUtcIsoString()

            // Use a transaction to ensure atomicity
            db.transaction {
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

                // 2. Insert participants
                val participantIds = factory.createParticipantIds()
                participantIds.forEachIndexed { index, userId ->
                    val role = if (index == 0) "ORGANIZER" else "PARTICIPANT"
                    val participantId = "sample-part-${index}"
                    participantQueries.insertParticipant(
                        id = participantId,
                        eventId = event.id,
                        userId = userId,
                        role = role,
                        hasValidatedDate = 0,
                        joinedAt = now,
                        updatedAt = now
                    )
                }

                // 3. Insert time slots
                event.proposedSlots.forEach { slot ->
                    timeSlotQueries.insertTimeSlot(
                        id = slot.id,
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
                        voteQueries.insertVote(
                            id = "sample-vote-${slotId}-${userId}",
                            eventId = event.id,
                            timeslotId = slotId,
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
