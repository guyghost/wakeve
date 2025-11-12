package com.guyghost.wakeve

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote

class DatabaseEventRepositoryTest {

    private lateinit var db: com.guyghost.wakeve.database.WakevDb
    private lateinit var repository: DatabaseEventRepository

    fun setup() {
        db = createTestDatabase()
        repository = DatabaseEventRepository(db)
    }

    @Test
    fun testCreateAndRetrieveEvent() {
        setup()
        
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.DRAFT
        )

        val result = repository.createEvent(event)
        assertTrue(result.isSuccess, "Event creation should succeed")
        assertEquals(event, result.getOrNull(), "Created event should match input")

        val retrieved = repository.getEvent("event-1")
        assertNotNull(retrieved, "Event should be retrievable")
        assertEquals("Team Meeting", retrieved?.title, "Title should match")
        assertEquals(EventStatus.DRAFT, retrieved?.status, "Status should match")
    }

    @Test
    fun testAddParticipant() {
        setup()
        
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.DRAFT
        )
        
        repository.createEvent(event)

        val result = repository.addParticipant("event-1", "participant-1")
        assertTrue(result.isSuccess, "Adding participant should succeed")

        val participants = repository.getParticipants("event-1")
        assertNotNull(participants, "Participants list should exist")
        assertTrue(participants?.contains("participant-1") == true, "Participant should be in list")
    }

    @Test
    fun testAddParticipantToNonDraftEventFails() {
        setup()
        
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.POLLING
        )
        
        repository.createEvent(event)

        val result = repository.addParticipant("event-1", "participant-1")
        assertTrue(result.isFailure, "Adding participant to non-DRAFT event should fail")
    }

    @Test
    fun testAddDuplicateParticipantFails() {
        setup()
        
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.DRAFT
        )
        
        repository.createEvent(event)
        repository.addParticipant("event-1", "participant-1")

        val result = repository.addParticipant("event-1", "participant-1")
        assertTrue(result.isFailure, "Adding duplicate participant should fail")
    }

    @Test
    fun testUpdateEventStatus() {
        setup()
        
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.DRAFT
        )
        
        repository.createEvent(event)

        val result = repository.updateEventStatus("event-1", EventStatus.POLLING)
        assertTrue(result.isSuccess, "Status update should succeed")

        val updated = repository.getEvent("event-1")
        assertEquals(EventStatus.POLLING, updated?.status, "Status should be updated")
    }

    @Test
    fun testAddVoteToEvent() {
        setup()
        
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = listOf("participant-1"),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.POLLING
        )
        
        repository.createEvent(event)
        // Add participant to event
        repository.addParticipant("event-1", "participant-1")

        val result = repository.addVote("event-1", "participant-1", "slot-1", Vote.YES)
        assertTrue(result.isSuccess, "Adding vote should succeed")
    }

    @Test
    fun testVoteBeforeDeadlineFails() {
        setup()
        
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = listOf("participant-1"),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-10T10:00:00Z", // Past deadline
            status = EventStatus.POLLING
        )
        
        repository.createEvent(event)
        repository.addParticipant("event-1", "participant-1")

        val result = repository.addVote("event-1", "participant-1", "slot-1", Vote.YES)
        assertTrue(result.isFailure, "Voting after deadline should fail")
    }

    @Test
    fun testGetPoll() {
        setup()
        
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val slot2 = TimeSlot("slot-2", "2025-12-02T14:00:00Z", "2025-12-02T16:00:00Z", "UTC")
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = listOf("participant-1", "participant-2"),
            proposedSlots = listOf(slot1, slot2),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.POLLING
        )
        
        repository.createEvent(event)
        repository.addParticipant("event-1", "participant-1")
        repository.addParticipant("event-1", "participant-2")
        repository.addVote("event-1", "participant-1", "slot-1", Vote.YES)
        repository.addVote("event-1", "participant-2", "slot-1", Vote.MAYBE)

        val poll = repository.getPoll("event-1")
        assertNotNull(poll, "Poll should exist")
        assertEquals("event-1", poll?.id, "Poll ID should match")
    }

    @Test
    fun testGetAllEvents() {
        setup()
        
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        
        repeat(3) { i ->
            val event = Event(
                id = "event-$i",
                title = "Meeting $i",
                description = "Description $i",
                organizerId = "org-1",
                participants = emptyList(),
                proposedSlots = listOf(slot1),
                deadline = "2025-11-20T18:00:00Z",
                status = EventStatus.DRAFT
            )
            repository.createEvent(event)
        }

        val allEvents = repository.getAllEvents()
        assertEquals(3, allEvents.size, "Should have 3 events")
    }

    @Test
    fun testIsOrganizer() {
        setup()
        
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.DRAFT
        )
        
        repository.createEvent(event)

        assertTrue(repository.isOrganizer("event-1", "org-1"), "Organizer should be recognized")
        assertTrue(!repository.isOrganizer("event-1", "participant-1"), "Non-organizer should not be recognized")
    }

    @Test
    fun testCanModifyEvent() {
        setup()
        
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.DRAFT
        )
        
        repository.createEvent(event)

        assertTrue(repository.canModifyEvent("event-1", "org-1"), "Organizer can modify")
        assertTrue(!repository.canModifyEvent("event-1", "participant-1"), "Participant cannot modify")
    }

    @Test
    fun testEventNotFound() {
        setup()
        
        val retrieved = repository.getEvent("nonexistent")
        assertNull(retrieved, "Nonexistent event should return null")
    }

    @Test
    fun testConfirmEventDate() {
        setup()
        
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.POLLING
        )
        
        repository.createEvent(event)

        val result = repository.updateEventStatus(
            "event-1",
            EventStatus.CONFIRMED,
            "2025-12-01T10:00:00Z"
        )
        assertTrue(result.isSuccess, "Confirming date should succeed")

        val updated = repository.getEvent("event-1")
        assertEquals(EventStatus.CONFIRMED, updated?.status, "Status should be CONFIRMED")
    }
}
