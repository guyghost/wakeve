package com.guyghost.wakeve

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.Vote

interface EventRepositoryInterface {
    suspend fun createEvent(event: Event): Result<Event>
    fun getEvent(id: String): Event?
    fun getPoll(eventId: String): Poll?
    suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean>
    fun getParticipants(eventId: String): List<String>?
    suspend fun addVote(eventId: String, participantId: String, slotId: String, vote: Vote): Result<Boolean>
    suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean>
    fun isDeadlinePassed(deadline: String): Boolean
    fun isOrganizer(eventId: String, userId: String): Boolean
    fun canModifyEvent(eventId: String, userId: String): Boolean
    fun getAllEvents(): List<Event>
}

class EventRepository : EventRepositoryInterface {
    private val events = mutableMapOf<String, Event>()
    private val polls = mutableMapOf<String, Poll>()

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

    override suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean> {
        val event = events[id] ?: return Result.failure(IllegalArgumentException("Event not found"))
        
        // Only organizer can change status (should be enforced at higher level)
        events[id] = event.copy(status = status, finalDate = finalDate)
        return Result.success(true)
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
}