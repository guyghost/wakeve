package com.guyghost.wakeve

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventRepositoryTest {

    private val repository = EventRepository()

    private val slot1 = TimeSlot(
        id = "slot-1",
        start = "2025-12-01T10:00:00Z",
        end = "2025-12-01T12:00:00Z",
        timezone = "UTC"
    )

    private val slot2 = TimeSlot(
        id = "slot-2",
        start = "2025-12-02T10:00:00Z",
        end = "2025-12-02T12:00:00Z",
        timezone = "UTC"
    )

    private val event = Event(
        id = "event-1",
        title = "Team Retreat",
        description = "Annual team planning",
        organizerId = "organizer-1",
        participants = emptyList(),
        proposedSlots = listOf(slot1, slot2),
        deadline = "2025-11-25T18:00:00Z",
        status = EventStatus.DRAFT
    )

    @Test
    fun createEventSuccess() {
        val result = repository.createEvent(event)
        
        assertTrue(result.isSuccess)
        val retrievedEvent = repository.getEvent("event-1")
        assertNotNull(retrievedEvent)
        assertEquals("Team Retreat", retrievedEvent.title)
        assertEquals(EventStatus.DRAFT, retrievedEvent.status)
    }

    @Test
    fun addParticipantToDraftEvent() {
        repository.createEvent(event)
        
        val result = repository.addParticipant("event-1", "participant-1")
        
        assertTrue(result.isSuccess)
        val participants = repository.getParticipants("event-1")
        assertNotNull(participants)
        assertTrue(participants.contains("participant-1"))
    }

    @Test
    fun cannotAddDuplicateParticipant() {
        repository.createEvent(event)
        repository.addParticipant("event-1", "participant-1")
        
        val result = repository.addParticipant("event-1", "participant-1")
        
        assertFalse(result.isSuccess)
    }

    @Test
    fun cannotAddParticipantAfterDraft() {
        repository.createEvent(event)
        repository.updateEventStatus("event-1", EventStatus.POLLING)
        
        val result = repository.addParticipant("event-1", "participant-2")
        
        assertFalse(result.isSuccess)
    }

    @Test
    fun organizerCanModifyEvent() {
        repository.createEvent(event)
        
        val canModify = repository.canModifyEvent("event-1", "organizer-1")
        
        assertTrue(canModify)
    }

    @Test
    fun participantCannotModifyEvent() {
        repository.createEvent(event)
        
        val canModify = repository.canModifyEvent("event-1", "participant-1")
        
        assertFalse(canModify)
    }

    @Test
    fun addVoteDuringPolling() {
        val eventWithParticipant = event.copy(
            participants = listOf("participant-1"),
            status = EventStatus.POLLING
        )
        repository.createEvent(eventWithParticipant)
        
        val result = repository.addVote("event-1", "participant-1", "slot-1", Vote.YES)
        
        assertTrue(result.isSuccess)
        val poll = repository.getPoll("event-1")
        assertNotNull(poll)
        assertEquals(Vote.YES, poll.votes["participant-1"]?.get("slot-1"))
    }

    @Test
    fun cannotVoteAfterDeadline() {
        val pastDeadline = "2020-01-01T00:00:00Z"
        val eventWithPastDeadline = event.copy(
            participants = listOf("participant-1"),
            status = EventStatus.POLLING,
            deadline = pastDeadline
        )
        repository.createEvent(eventWithPastDeadline)
        
        val result = repository.addVote("event-1", "participant-1", "slot-1", Vote.YES)
        
        assertFalse(result.isSuccess)
    }

    @Test
    fun cannotVoteIfNotParticipant() {
        val eventWithParticipant = event.copy(
            participants = listOf("participant-1"),
            status = EventStatus.POLLING
        )
        repository.createEvent(eventWithParticipant)
        
        val result = repository.addVote("event-1", "unknown-participant", "slot-1", Vote.YES)
        
        assertFalse(result.isSuccess)
    }

    @Test
    fun updateEventStatus() {
        repository.createEvent(event)
        
        val result = repository.updateEventStatus("event-1", EventStatus.CONFIRMED, "2025-12-01T10:00:00Z")
        
        assertTrue(result.isSuccess)
        val updatedEvent = repository.getEvent("event-1")
        assertNotNull(updatedEvent)
        assertEquals(EventStatus.CONFIRMED, updatedEvent.status)
        assertEquals("2025-12-01T10:00:00Z", updatedEvent.finalDate)
    }
}
