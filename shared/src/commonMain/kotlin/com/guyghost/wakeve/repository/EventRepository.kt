package com.guyghost.wakeve.repository

import com.guyghost.wakeve.access.EventAccessPolicy
import com.guyghost.wakeve.access.ParticipantAccessMapper
import com.guyghost.wakeve.access.ParticipantAccessState
import com.guyghost.wakeve.access.ParticipantRepositoryRecord
import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.confirmation.SystemConfirmationClock
import com.guyghost.wakeve.confirmation.confirmationEffectKeys
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.poll.PollBallotContract
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.workflow.WorkflowOutboxRecord
import com.guyghost.wakeve.workflow.WorkflowOutboxType
import com.guyghost.wakeve.workflow.PendingWorkflowStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant

interface EventRepositoryInterface {
    suspend fun createEvent(event: Event): Result<Event>
    fun getEvent(id: String): Event?
    fun getPoll(eventId: String): Poll?
    suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean>
    fun getParticipants(eventId: String): List<String>?
    fun getParticipantRecords(eventId: String): List<ParticipantRepositoryRecord>? {
        val event = getEvent(eventId)
        return getParticipants(eventId)?.map { participantId ->
            val isOrganizer = event?.organizerId == participantId
            ParticipantRepositoryRecord(
                id = "participant-record-$eventId-$participantId",
                eventId = eventId,
                userId = participantId,
                role = if (isOrganizer) "ORGANIZER" else "MEMBER",
                rsvp = if (isOrganizer) "ACCEPTED" else "PENDING",
                hasValidatedDate = if (isOrganizer) 1L else 0L
            )
        }
    }
    suspend fun addVote(eventId: String, participantId: String, slotId: String, vote: Vote): Result<Boolean>
    suspend fun commitCompleteBallot(
        command: PollBallotContract.CommitCompleteBallotCommand
    ): PollBallotContract.CommitResult = PollBallotContract.CommitResult.Rejected(
        operationId = command.operationId,
        failure = PollBallotContract.Failure(
            PollBallotContract.FailureCode.REPOSITORY_UNAVAILABLE,
            retryable = true
        )
    )
    suspend fun resolveCompleteBallotOutcome(
        command: PollBallotContract.CommitCompleteBallotCommand
    ): PollBallotContract.ResolutionResult = PollBallotContract.ResolutionResult.Unknown(
        PollBallotContract.Failure(
            PollBallotContract.FailureCode.REPOSITORY_UNAVAILABLE,
            retryable = true,
            commitOutcome = PollBallotContract.CommitOutcome.UNKNOWN
        )
    )
    fun hasCompleteBallot(eventId: String, participantId: String): Boolean = false
    suspend fun updateEvent(event: Event): Result<Event>
    suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean>
    suspend fun confirmEventDate(
        eventId: String,
        slotId: String,
        confirmedByOrganizerId: String
    ): Result<Boolean> = Result.failure(UnsupportedOperationException("Confirm event date is not supported"))
    suspend fun confirmEventDateCommand(eventId: String, slotId: String, confirmedByOrganizerId: String, operationId: String, requestedAt: String): Result<Boolean> =
        confirmEventDate(eventId, slotId, confirmedByOrganizerId)
    suspend fun confirmPollDate(
        command: EventManagementContract.ConfirmPollDateCommand
    ): EventManagementContract.ConfirmationResult =
        EventManagementContract.ConfirmationResult.Failed(
            operationId = command.operationId,
            failure = EventManagementContract.ConfirmationFailure(
                code = EventManagementContract.ConfirmationFailureCode.REPOSITORY_UNAVAILABLE,
                retryable = true
            )
        )
    /**
     * Persists the server acknowledgement for a locally committed decision.
     * A non-durable repository cannot truthfully report a synced confirmation.
     */
    suspend fun markConfirmationSynced(
        receiptId: String
    ): EventManagementContract.ConfirmationProjection? = null
    /**
     * Reads the durable confirmation state for composition-time rehydration.
     * Implementations without durable confirmation storage are reviewing-only.
     */
    fun loadConfirmationProjection(
        eventId: String
    ): EventManagementContract.ConfirmationProjection =
        EventManagementContract.ConfirmationProjection.Reviewing(eventId)
    suspend fun queueWorkflowOutbox(record: WorkflowOutboxRecord): Result<Boolean> = Result.success(true)
    fun getWorkflowOutbox(eventId: String): List<WorkflowOutboxRecord> = emptyList()
    suspend fun saveEvent(event: Event): Result<Event>
    
    /**
     * Delete an event and all its related data.
     *
     * Only the organizer can delete an event, and FINALIZED events cannot be deleted.
     * This method performs cascade deletion of all related entities:
     * - Participants
     * - Votes
     * - Time slots
     * - Potential locations
     * - Scenarios
     * - Confirmed dates
     * - Sync metadata
     *
     * @param eventId The ID of the event to delete
     * @return Result<Unit> success if deleted, failure with error message
     */
    suspend fun deleteEvent(eventId: String): Result<Unit>
    
    fun isDeadlinePassed(deadline: String): Boolean
    fun isOrganizer(eventId: String, userId: String): Boolean
    fun canModifyEvent(eventId: String, userId: String): Boolean
    fun getAllEvents(): List<Event>
    fun getEventsPaginated(
        page: Int,
        pageSize: Int,
        orderBy: OrderBy = OrderBy.CREATED_AT_DESC
    ): kotlinx.coroutines.flow.Flow<List<Event>>
}

class EventRepository(
    private val confirmationClock: ConfirmationClock = SystemConfirmationClock,
    @Suppress("unused") private val constructorMarker: Unit = Unit
) : EventRepositoryInterface {
    /** Explicit no-arg constructor retained for Kotlin/Native preview and legacy Swift code. */
    constructor() : this(SystemConfirmationClock, Unit)
    private val events = mutableMapOf<String, Event>()
    private val polls = mutableMapOf<String, Poll>()
    private val workflowOutbox = mutableListOf<WorkflowOutboxRecord>()
    private val ballotReceipts = mutableMapOf<String, PollBallotContract.Receipt>()
    private val ballotMutex = Mutex()

    override suspend fun createEvent(event: Event): Result<Event> {
        return try {
            events[event.id] = event
            polls[event.id] = Poll(event.id, event.id, emptyMap())
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getEvent(id: String): Event? = events[id]

    override fun getPoll(eventId: String): Poll? = polls[eventId]

    override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> {
        val event = events[eventId] ?: return Result.failure(IllegalArgumentException("Event not found"))
        
        if (event.status != EventStatus.DRAFT) {
            return Result.failure(IllegalStateException("Cannot add participants after DRAFT status"))
        }
        
        if (event.participants.contains(participantId)) {
            return Result.failure(IllegalArgumentException("Participant already added"))
        }
        
        events[eventId] = event.copy(participants = event.participants + participantId)
        return Result.success(true)
    }

    override fun getParticipants(eventId: String): List<String>? = events[eventId]?.participants

    override fun getParticipantRecords(eventId: String): List<ParticipantRepositoryRecord>? {
        val event = events[eventId] ?: return null
        return event.participants.map { participantId ->
            val isOrganizer = event.organizerId == participantId
            ParticipantRepositoryRecord(
                id = "participant-record-$eventId-$participantId",
                eventId = eventId,
                userId = participantId,
                role = if (isOrganizer) "ORGANIZER" else "MEMBER",
                rsvp = if (isOrganizer) "ACCEPTED" else "PENDING",
                hasValidatedDate = if (isOrganizer) 1L else 0L
            )
        }
    }

    override suspend fun addVote(eventId: String, participantId: String, slotId: String, vote: Vote): Result<Boolean> {
        val event = events[eventId] ?: return Result.failure(IllegalArgumentException("Event not found"))
        val poll = polls[eventId] ?: return Result.failure(IllegalStateException("Poll not found"))
        
        // Check if event is in POLLING status
        if (event.status != EventStatus.POLLING) {
            return Result.failure(IllegalStateException("Event is not in POLLING status"))
        }
        
        // Check if deadline has passed
        if (isDeadlinePassed(event.deadline)) {
            return Result.failure(IllegalStateException("Voting deadline has passed"))
        }
        
        // Check if participant is in the event
        if (!event.participants.contains(participantId)) {
            return Result.failure(IllegalArgumentException("Participant not in event"))
        }
        
        val participantVotes = poll.votes[participantId]?.toMutableMap() ?: mutableMapOf()
        participantVotes[slotId] = vote
        polls[eventId] = poll.copy(votes = poll.votes + (participantId to participantVotes))
        
        return Result.success(true)
    }

    override suspend fun commitCompleteBallot(
        command: PollBallotContract.CommitCompleteBallotCommand
    ): PollBallotContract.CommitResult = ballotMutex.withLock {
        if (!PollBallotContract.isValidPollRevision(command.pollRevision)) {
            return@withLock ballotRejected(command, PollBallotContract.FailureCode.INVALID_POLL_REVISION)
        }
        val operationKey = PollBallotContract.operationKey(command)
        ballotReceipts[operationKey]?.let { receipt ->
            val replayEnvelope = PollBallotContract.envelope(
                command.copy(authoritativeDeadlineIso = command.authoritativeDeadlineIso.ifBlank {
                    receipt.authoritativeDeadlineIso
                })
            )
            return@withLock if (receipt.syncPayload.command == replayEnvelope) {
                PollBallotContract.CommitResult.AlreadyCommitted(receipt)
            } else {
                ballotRejected(command, PollBallotContract.FailureCode.IDEMPOTENCY_CONFLICT)
            }
        }

        val event = events[command.eventId]
            ?: return@withLock ballotRejected(command, PollBallotContract.FailureCode.EVENT_NOT_FOUND)
        val viewer = if (command.actorId == event.organizerId) {
            ParticipantAccessState.organizer(command.actorId)
        } else {
            getParticipantRecords(command.eventId)
                ?.firstOrNull { it.userId == command.actorId }
                ?.let(ParticipantAccessMapper::fromRepositoryRecord)
                ?: ParticipantAccessState.nonMember(command.actorId)
        }
        if (!EventAccessPolicy.canSubmitPollBallot(event, viewer).isAllowed) {
            return@withLock ballotRejected(command, PollBallotContract.FailureCode.FORBIDDEN)
        }
        if (event.status != EventStatus.POLLING) {
            return@withLock ballotRejected(command, PollBallotContract.FailureCode.INVALID_EVENT_STATUS)
        }
        if (event.aggregateRevision != command.pollRevision) {
            return@withLock ballotRejected(command, PollBallotContract.FailureCode.POLL_REVISION_CONFLICT)
        }
        val authoritativeDeadlineIso = command.authoritativeDeadlineIso.ifBlank { event.deadline }
        if (authoritativeDeadlineIso != event.deadline) {
            return@withLock ballotRejected(command, PollBallotContract.FailureCode.POLL_REVISION_CONFLICT)
        }
        val effectiveCommand = command.copy(authoritativeDeadlineIso = authoritativeDeadlineIso)
        val envelope = PollBallotContract.envelope(effectiveCommand)
        val deadline = try {
            Instant.parse(authoritativeDeadlineIso)
        } catch (_: Exception) {
            return@withLock ballotRejected(command, PollBallotContract.FailureCode.INVALID_DEADLINE_ISO)
        }
        val acceptedAt = try {
            confirmationClock.now()
        } catch (_: Exception) {
            return@withLock ballotRejected(
                command,
                PollBallotContract.FailureCode.CLOCK_UNAVAILABLE,
                retryable = true
            )
        }
        if (acceptedAt >= deadline) {
            return@withLock ballotRejected(command, PollBallotContract.FailureCode.DEADLINE_REACHED)
        }

        PollBallotContract.validateEntries(
            event.proposedSlots.map { it.id },
            command.entries
        )?.let { failure ->
            return@withLock ballotRejected(command, failure)
        }

        val poll = polls[command.eventId]
            ?: return@withLock ballotRejected(
                command,
                PollBallotContract.FailureCode.REPOSITORY_UNAVAILABLE,
                true
            )
        val completeVotes = command.entries.associate { it.slotId to it.vote }
        polls[command.eventId] = poll.copy(votes = poll.votes + (command.actorId to completeVotes))
        val acceptedAtIso = acceptedAt.toString()
        val receiptId = "poll-ballot:$operationKey"
        val syncPayload = PollBallotContract.BallotSyncPayload(
            localReceiptId = receiptId,
            command = envelope
        )
        val receipt = PollBallotContract.Receipt(
            receiptId = receiptId,
            operationId = command.operationId,
            eventId = command.eventId,
            actorId = command.actorId,
            pollRevision = command.pollRevision,
            ballotFingerprint = envelope.ballotFingerprint,
            authoritativeDeadlineIso = authoritativeDeadlineIso,
            acceptedAtIso = acceptedAtIso,
            syncStatus = PollBallotContract.SyncStatus.LOCAL_PENDING,
            syncPayload = syncPayload
        )
        ballotReceipts[operationKey] = receipt
        PollBallotContract.CommitResult.Committed(receipt)
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
        val envelope = runCatching { PollBallotContract.envelope(command) }.getOrNull()
            ?: return@withLock PollBallotContract.ResolutionResult.Unknown(
                PollBallotContract.Failure(
                    PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT,
                    retryable = false,
                    commitOutcome = PollBallotContract.CommitOutcome.UNKNOWN
                )
            )
        val receipt = ballotReceipts[operationKey]
        when {
            receipt == null -> PollBallotContract.ResolutionResult.ProvenNotCommitted(
                operationKey,
                envelope.ballotFingerprint
            )
            receipt.syncPayload.command == envelope ->
                PollBallotContract.ResolutionResult.Committed(receipt)
            else -> PollBallotContract.ResolutionResult.Unknown(
                PollBallotContract.Failure(
                    PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT,
                    retryable = false,
                    commitOutcome = PollBallotContract.CommitOutcome.UNKNOWN
                )
            )
        }
    }

    override fun hasCompleteBallot(eventId: String, participantId: String): Boolean {
        val requiredSlots = events[eventId]?.proposedSlots?.map { it.id }?.toSet() ?: return false
        val submittedSlots = polls[eventId]?.votes?.get(participantId)?.keys ?: return false
        return requiredSlots.isNotEmpty() && submittedSlots == requiredSlots
    }

    private fun ballotRejected(
        command: PollBallotContract.CommitCompleteBallotCommand,
        code: PollBallotContract.FailureCode,
        retryable: Boolean = false
    ) = PollBallotContract.CommitResult.Rejected(
        command.operationId,
        PollBallotContract.Failure(code, retryable)
    )

    override suspend fun updateEvent(event: Event): Result<Event> {
        return try {
            events[event.id] = event
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean> {
        val event = events[id] ?: return Result.failure(IllegalArgumentException("Event not found"))

        // Only organizer can change status (should be enforced at higher level)
        events[id] = event.copy(status = status, finalDate = finalDate)
        return Result.success(true)
    }

    override suspend fun confirmEventDate(
        eventId: String,
        slotId: String,
        confirmedByOrganizerId: String
    ): Result<Boolean> {
        val event = events[eventId] ?: return Result.failure(IllegalArgumentException("Event not found"))
        if (event.organizerId != confirmedByOrganizerId) {
            return Result.failure(IllegalStateException("Only event organizer can confirm dates"))
        }

        val selectedSlot = event.proposedSlots.find { it.id == slotId }
            ?: return Result.failure(IllegalArgumentException("Selected time slot not found"))
        val finalDate = selectedSlot.start
            ?: return Result.failure(IllegalStateException("Selected time slot has no confirmed start date"))

        if (event.status == EventStatus.CONFIRMED) {
            return if (event.finalDate == finalDate) Result.success(true)
            else Result.failure(IllegalStateException("ALREADY_CONFIRMED_DIFFERENT_SLOT"))
        }

        events[eventId] = event.copy(
            status = EventStatus.CONFIRMED,
            finalDate = finalDate
        )
        return Result.success(true)
    }

    override suspend fun confirmEventDateCommand(eventId: String, slotId: String, confirmedByOrganizerId: String, operationId: String, requestedAt: String): Result<Boolean> =
        confirmEventDate(eventId, slotId, confirmedByOrganizerId)

    override suspend fun queueWorkflowOutbox(record: WorkflowOutboxRecord): Result<Boolean> {
        if (workflowOutbox.none { it.eventId == record.eventId && it.type == record.type }) workflowOutbox += record
        return Result.success(true)
    }

    override fun getWorkflowOutbox(eventId: String): List<WorkflowOutboxRecord> =
        workflowOutbox.filter { it.eventId == eventId }

    /**
     * Save an event (create if it doesn't exist, otherwise update).
     * This is useful for auto-save functionality during draft wizard steps.
     *
     * @param event The event to save
     * @return Result containing the saved event, or an error
     */
    override suspend fun saveEvent(event: Event): Result<Event> {
        return try {
            // If event already exists, update it; otherwise create it
            val existingEvent = events[event.id]
            if (existingEvent != null) {
                // Update existing event
                events[event.id] = event
            } else {
                // Create new event
                events[event.id] = event
                polls[event.id] = Poll(event.id, event.id, emptyMap())
            }
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        return events[eventId]?.organizerId == userId
    }

    override fun canModifyEvent(eventId: String, userId: String): Boolean {
        return isOrganizer(eventId, userId)
    }

    override fun getAllEvents(): List<Event> = events.values.toList()

    override fun getEventsPaginated(
        page: Int,
        pageSize: Int,
        orderBy: OrderBy
    ): Flow<List<Event>> {
        // Handle invalid parameters
        if (page < 0 || pageSize <= 0) {
            return flowOf(emptyList())
        }

        // Sort events based on orderBy
        val sortedEvents = when (orderBy) {
            OrderBy.CREATED_AT_DESC -> events.values.sortedByDescending { it.createdAt }
            OrderBy.CREATED_AT_ASC -> events.values.sortedBy { it.createdAt }
            OrderBy.TITLE_ASC -> events.values.sortedBy { it.title }
            OrderBy.TITLE_DESC -> events.values.sortedByDescending { it.title }
            OrderBy.STATUS_ASC -> events.values.sortedBy { it.status.name }
            OrderBy.STATUS_DESC -> events.values.sortedByDescending { it.status.name }
        }

        // Calculate offset and limit
        val offset = page * pageSize
        return flowOf(
            sortedEvents
                .drop(offset)
                .take(pageSize)
        )
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            val event = events[eventId]
                ?: return Result.failure(IllegalArgumentException("Event not found"))

            // Remove event and associated poll
            events.remove(eventId)
            polls.remove(eventId)
            workflowOutbox.removeAll { it.eventId == eventId }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
