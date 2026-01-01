package com.guyghost.wakeve

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseEventRepositoryTest {

    private lateinit var db: WakevDb
    private lateinit var repository: DatabaseEventRepository

    @BeforeTest
    fun setup() {
        // Create a fresh database for each test to ensure isolation
        db = createFreshTestDatabase()
        repository = DatabaseEventRepository(db)
    }

    @Test
    fun testCreateAndRetrieveEvent() = runBlocking {
        
        val event = createTestEvent(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            proposedSlots = listOf(
                createTestTimeSlot(id = "slot-1", start = "2025-12-01T10:00:00Z", end = "2025-12-01T12:00:00Z")
            ),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
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
    fun testAddParticipant() = runBlocking {
        
        val event = createTestEvent(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            deadline = "2025-11-20T18:00:00Z",
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )
        
        repository.createEvent(event)

        val result = repository.addParticipant("event-1", "participant-1")
        assertTrue(result.isSuccess, "Adding participant should succeed")

        val participants = repository.getParticipants("event-1")
        assertNotNull(participants, "Participants list should exist")
        assertTrue(participants?.contains("participant-1") == true, "Participant should be in list")
    }

    @Test
    fun testAddParticipantToNonDraftEventFails() = runBlocking {
        
        val event = createTestEvent(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.POLLING,
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )
        
        repository.createEvent(event)

        val result = repository.addParticipant("event-1", "participant-1")
        assertTrue(result.isFailure, "Adding participant to non-DRAFT event should fail")
    }

    @Test
    fun testAddDuplicateParticipantFails() = runBlocking {
        
        val event = createTestEvent(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            deadline = "2025-11-20T18:00:00Z",
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )
        
        repository.createEvent(event)
        repository.addParticipant("event-1", "participant-1")

        val result = repository.addParticipant("event-1", "participant-1")
        assertTrue(result.isFailure, "Adding duplicate participant should fail")
    }

    @Test
    fun testUpdateEventStatus() = runBlocking {
        
        val event = createTestEvent(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            deadline = "2025-11-20T18:00:00Z",
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )
        
        repository.createEvent(event)

        val result = repository.updateEventStatus("event-1", EventStatus.POLLING, null)
        assertTrue(result.isSuccess, "Status update should succeed")

        val updated = repository.getEvent("event-1")
        assertEquals(EventStatus.POLLING, updated?.status, "Status should be updated")
    }

    @Test
    fun testAddVoteToEvent() = runBlocking {
        
        // Create event in DRAFT status first
        val event = createTestEvent(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            deadline = "2025-11-20T18:00:00Z",
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )
        
        repository.createEvent(event)
        // Add participant while in DRAFT status
        repository.addParticipant("event-1", "participant-1")
        // Then transition to POLLING status
        repository.updateEventStatus("event-1", EventStatus.POLLING, null)

        val result = repository.addVote("event-1", "participant-1", "slot-1", Vote.YES)
        assertTrue(result.isSuccess, "Adding vote should succeed")
    }

    @Test
    fun testVoteBeforeDeadlineFails() = runBlocking {
        
        // Create event in DRAFT status first
        val event = createTestEvent(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            deadline = "2025-11-10T10:00:00Z", // Past deadline
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )
        
        repository.createEvent(event)
        // Add participant while in DRAFT status
        repository.addParticipant("event-1", "participant-1")
        // Then transition to POLLING status
        repository.updateEventStatus("event-1", EventStatus.POLLING, null)

        val result = repository.addVote("event-1", "participant-1", "slot-1", Vote.YES)
        assertTrue(result.isFailure, "Voting after deadline should fail")
    }

    @Test
    fun testGetPoll() = runBlocking {
        
        // Create event in DRAFT status first
        val event = createTestEvent(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            proposedSlots = listOf(
                createTestTimeSlot(id = "slot-1", start = "2025-12-01T10:00:00Z", end = "2025-12-01T12:00:00Z"),
                createTestTimeSlot(id = "slot-2", start = "2025-12-02T14:00:00Z", end = "2025-12-02T16:00:00Z")
            ),
            deadline = "2025-11-20T18:00:00Z",
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )
        
        repository.createEvent(event)
        // Add participants while in DRAFT status
        repository.addParticipant("event-1", "participant-1")
        repository.addParticipant("event-1", "participant-2")
        // Then transition to POLLING status
        repository.updateEventStatus("event-1", EventStatus.POLLING, null)
        
        repository.addVote("event-1", "participant-1", "slot-1", Vote.YES)
        repository.addVote("event-1", "participant-2", "slot-1", Vote.MAYBE)

        val poll = repository.getPoll("event-1")
        assertNotNull(poll, "Poll should exist")
        assertEquals("event-1", poll?.id, "Poll ID should match")
    }

    @Test
    fun testGetAllEvents() = runBlocking {
        
        repeat(3) { i ->
            val event = createTestEvent(
                id = "event-$i",
                title = "Meeting $i",
                description = "Description $i",
                organizerId = "org-1",
                deadline = "2025-11-20T18:00:00Z",
                createdAt = "2025-11-20T10:00:00Z",
                updatedAt = "2025-11-20T10:00:00Z"
            )
            repository.createEvent(event)
        }

        val allEvents = repository.getAllEvents()
        assertEquals(3, allEvents.size, "Should have 3 events")
    }

    @Test
    fun testIsOrganizer() = runBlocking {
        
        val event = createTestEvent(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            deadline = "2025-11-20T18:00:00Z",
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )
        
        repository.createEvent(event)

        assertTrue(repository.isOrganizer("event-1", "org-1"), "Organizer should be recognized")
        assertTrue(!repository.isOrganizer("event-1", "participant-1"), "Non-organizer should not be recognized")
    }

    @Test
    fun testCanModifyEvent() = runBlocking {
        
        val event = createTestEvent(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            deadline = "2025-11-20T18:00:00Z",
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )
        
        repository.createEvent(event)

        assertTrue(repository.canModifyEvent("event-1", "org-1"), "Organizer can modify")
        assertTrue(!repository.canModifyEvent("event-1", "participant-1"), "Participant cannot modify")
    }

    @Test
    fun testEventNotFound() = runBlocking {
        
        val retrieved = repository.getEvent("nonexistent")
        assertNull(retrieved, "Nonexistent event should return null")
    }

    @Test
    fun testConfirmEventDate() = runBlocking {
        
        val event = createTestEvent(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            status = EventStatus.POLLING,
            deadline = "2025-11-20T18:00:00Z",
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
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
