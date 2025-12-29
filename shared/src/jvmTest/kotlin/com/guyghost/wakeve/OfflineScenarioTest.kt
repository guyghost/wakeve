package com.guyghost.wakeve

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for offline scenarios and data recovery.
 * These tests verify that:
 * 1. Data is properly persisted in the local database
 * 2. Sync metadata is properly tracked for offline changes
 * 3. Data can be retrieved after being created
 * 
 * Note: These tests use in-memory databases, so they test persistence within a session,
 * not across app restarts. For true persistence tests, use file-based SQLite databases.
 */
class OfflineScenarioTest {

    private lateinit var db: WakevDb
    private lateinit var repository: DatabaseEventRepository

    @BeforeTest
    fun setup() {
        // Create a fresh database for each test
        db = createFreshTestDatabase()
        repository = DatabaseEventRepository(db)
    }

    @Test
    fun testDataPersistsAcrossSessions() = runBlocking {
        // Create event and add participant
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val now = "2025-11-20T10:00:00Z"
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = now,
            updatedAt = now
        )

        repository.createEvent(event)
        repository.addParticipant("event-1", "participant-1")

        // Create a new repository instance using the SAME database
        // This simulates the app restarting but connecting to the same persisted database
        val repository2 = DatabaseEventRepository(db)

        val retrieved = repository2.getEvent("event-1")
        assertNotNull(retrieved, "Event should be persisted and retrievable")
        assertEquals("Team Meeting", retrieved?.title, "Event title should be unchanged")
        
        val participants = repository2.getParticipants("event-1")
        assertTrue(participants?.contains("participant-1") == true, "Participants should be persisted")
    }

    @Test
    fun testOfflineChangesAreTracked() = runBlocking {
        val syncQueries = db.syncMetadataQueries

        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val now = "2025-11-20T10:00:00Z"
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = now,
            updatedAt = now
        )

        // Create event (should create sync metadata)
        repository.createEvent(event)

        // Check that sync metadata was created
        val pending = syncQueries.selectPending().executeAsList()
        assertTrue(pending.isNotEmpty(), "Pending sync records should exist")
        assertTrue(pending.any { it.entityType == "event" }, "Event sync record should exist")
    }

    @Test
    fun testVotesArePersisted() = runBlocking {
        // Create event and add votes
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val slot2 = TimeSlot("slot-2", "2025-12-02T14:00:00Z", "2025-12-02T16:00:00Z", "UTC")
        val now = "2025-11-20T10:00:00Z"
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = listOf(slot1, slot2),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = now,
            updatedAt = now
        )

        repository.createEvent(event)
        repository.addParticipant("event-1", "participant-1")
        repository.addParticipant("event-1", "participant-2")
        repository.updateEventStatus("event-1", EventStatus.POLLING, null)

        // Add votes
        repository.addVote("event-1", "participant-1", "slot-1", Vote.YES)
        repository.addVote("event-1", "participant-2", "slot-1", Vote.MAYBE)

        // Verify votes are persisted using a new repository instance
        val repository2 = DatabaseEventRepository(db)
        val poll = repository2.getPoll("event-1")
        assertNotNull(poll, "Poll should exist")
        assertNotNull(poll?.votes?.get("participant-1"), "Participant 1 votes should exist")
        assertEquals(Vote.YES, poll?.votes?.get("participant-1")?.get("slot-1"), "Vote should be YES")
    }

    @Test
    fun testEventStatusChangesArePersisted() = runBlocking {
        // Create event and change status
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val now = "2025-11-20T10:00:00Z"
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = now,
            updatedAt = now
        )

        repository.createEvent(event)
        repository.updateEventStatus("event-1", EventStatus.POLLING, null)

        // Verify status change persisted using new repository
        val repository2 = DatabaseEventRepository(db)
        val retrieved = repository2.getEvent("event-1")
        assertEquals(EventStatus.POLLING, retrieved?.status, "Status change should be persisted")
    }

    @Test
    fun testMultipleEventsArePersisted() = runBlocking {
        // Create multiple events
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val now = "2025-11-20T10:00:00Z"

        repeat(5) { i ->
            val event = Event(
                id = "event-$i",
                title = "Meeting $i",
                description = "Description $i",
                organizerId = "org-1",
                participants = emptyList(),
                proposedSlots = listOf(slot1.copy(id = "slot-$i")),
                deadline = "2025-11-20T18:00:00Z",
                status = EventStatus.DRAFT,
                createdAt = now,
                updatedAt = now
            )
            repository.createEvent(event)
        }

        // Verify all events persisted using new repository
        val repository2 = DatabaseEventRepository(db)
        val allEvents = repository2.getAllEvents()
        assertEquals(5, allEvents.size, "All 5 events should be persisted")
        
        // Verify specific events
        for (i in 0..4) {
            val event = repository2.getEvent("event-$i")
            assertNotNull(event, "Event $i should be retrievable")
            assertEquals("Meeting $i", event?.title, "Event $i title should match")
        }
    }

    @Test
    fun testDataRecoveryAfterCrash() = runBlocking {
        // Create event with participants and votes
        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val now = "2025-11-20T10:00:00Z"
        val event = Event(
            id = "event-crash",
            title = "Critical Meeting",
            description = "Important discussion",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = now,
            updatedAt = now
        )

        repository.createEvent(event)
        repository.addParticipant("event-crash", "participant-1")
        repository.updateEventStatus("event-crash", EventStatus.POLLING, null)
        repository.addVote("event-crash", "participant-1", "slot-1", Vote.YES)

        // Simulate recovery by creating new repository instance
        val repository2 = DatabaseEventRepository(db)

        // Verify no data was lost
        val recovered = repository2.getEvent("event-crash")
        assertNotNull(recovered, "Event should be recovered")
        assertEquals("Critical Meeting", recovered?.title, "Event data should be intact")
        assertEquals(EventStatus.POLLING, recovered?.status, "Event status should be intact")

        val poll = repository2.getPoll("event-crash")
        assertNotNull(poll, "Poll data should be recovered")
        assertNotNull(poll?.votes?.get("participant-1"), "Vote should be recovered")
        assertEquals(Vote.YES, poll?.votes?.get("participant-1")?.get("slot-1"), "Vote value should be intact")
    }

    @Test
    fun testSyncMetadataTracksPendingChanges() = runBlocking {
        val syncQueries = db.syncMetadataQueries

        val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
        val now = "2025-11-20T10:00:00Z"
        val event = Event(
            id = "event-1",
            title = "Team Meeting",
            description = "Q4 Planning",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = listOf(slot1),
            deadline = "2025-11-20T18:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = now,
            updatedAt = now
        )

        repository.createEvent(event)
        repository.addParticipant("event-1", "participant-1")

        // Get pending sync records
        val pending = syncQueries.selectPending().executeAsList()
        
        // Should have sync records for event creation and participant addition
        assertTrue(pending.isNotEmpty(), "Pending sync records should exist")
        assertTrue(pending.any { it.operation == "CREATE" }, "CREATE operations should be tracked")
        
        // Verify sync flags
        assertTrue(pending.all { it.synced == 0L }, "All pending records should have synced=0")
    }
}
