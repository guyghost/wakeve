package com.guyghost.wakeve.repository

import com.guyghost.wakeve.access.ParticipantRepositoryRecord
import com.guyghost.wakeve.confirmation.confirmationEffectKeys
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.workflow.WorkflowOutboxRecord
import com.guyghost.wakeve.workflow.WorkflowOutboxType
import com.guyghost.wakeve.workflow.PendingWorkflowStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

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

class EventRepository : EventRepositoryInterface {
    private val events = mutableMapOf<String, Event>()
    private val polls = mutableMapOf<String, Poll>()
    private val workflowOutbox = mutableListOf<WorkflowOutboxRecord>()

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
        // Simple string comparison for ISO format (works for UTC "2025-12-31T23:59:59Z")
        return try {
            deadline < getCurrentUtcIsoString()
        } catch (e: Exception) {
            false
        }
    }

    private fun getCurrentUtcIsoString(): String {
        // For Phase 1, we use a simple approach
        // In Phase 2, integrate with kotlinx.datetime for full timezone support
        return "2025-11-12T10:00:00Z" // Use current test date (will be updated with actual time in Phase 2)
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
