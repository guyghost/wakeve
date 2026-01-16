package com.guyghost.wakeve.offline

import com.guyghost.wakeve.EventRepository
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.Vote
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for offline delete event functionality.
 * 
 * Verifies that:
 * 1. Events can be deleted while offline (in-memory)
 * 2. Cascade delete removes all related data
 * 3. Edge cases are handled properly
 */
class DeleteEventOfflineTest {

    private val repository = EventRepository()

    private val testTimeSlots = listOf(
        TimeSlot(
            id = "slot-1",
            start = "2026-02-20T09:00:00Z",
            end = "2026-02-20T17:00:00Z",
            timezone = "UTC",
            timeOfDay = TimeOfDay.SPECIFIC
        )
    )

    private val testEvent = Event(
        id = "offline-event-1",
        title = "Offline Delete Test Event",
        description = "Test event for offline deletion",
        organizerId = "organizer-1",
        status = EventStatus.DRAFT,
        deadline = "2026-02-15T23:59:59Z",
        createdAt = "2026-01-16T10:00:00Z",
        updatedAt = "2026-01-16T10:00:00Z",
        participants = listOf("participant-1", "participant-2"),
        proposedSlots = testTimeSlots,
        eventType = EventType.TEAM_BUILDING
    )

    // ========================================================================
    // Offline Delete: Basic Functionality
    // ========================================================================

    @Test
    fun `delete event offline removes event from local storage`() = runBlocking {
        // GIVEN: An event exists in local storage
        repository.createEvent(testEvent)
        assertNotNull(repository.getEvent(testEvent.id))

        // WHEN: Event is deleted while offline (in-memory repository simulates offline)
        val result = repository.deleteEvent(testEvent.id)

        // THEN: Delete succeeds and event is removed
        assertTrue(result.isSuccess, "Delete should succeed")
        assertNull(repository.getEvent(testEvent.id), "Event should be removed")
    }

    @Test
    fun `delete event offline removes from all events list`() = runBlocking {
        // GIVEN: Multiple events exist
        repository.createEvent(testEvent)
        val event2 = testEvent.copy(id = "offline-event-2", title = "Second Event")
        repository.createEvent(event2)
        assertEquals(2, repository.getAllEvents().size)

        // WHEN: One event is deleted offline
        repository.deleteEvent(testEvent.id)

        // THEN: Only that event is removed, other remains
        val remaining = repository.getAllEvents()
        assertEquals(1, remaining.size)
        assertEquals("offline-event-2", remaining[0].id)
    }

    // ========================================================================
    // Offline Delete: Cascade Delete (In-Memory)
    // ========================================================================

    @Test
    fun `delete event offline cascades to poll votes`() = runBlocking {
        // GIVEN: Event with poll and votes
        repository.createEvent(testEvent)
        repository.addVote(testEvent.id, "participant-1", "slot-1", Vote.YES)
        repository.addVote(testEvent.id, "participant-2", "slot-1", Vote.MAYBE)

        // Verify poll exists before deletion
        val pollBefore = repository.getPoll(testEvent.id)
        assertNotNull(pollBefore)

        // WHEN: Event is deleted
        repository.deleteEvent(testEvent.id)

        // THEN: Poll and votes are also removed
        val pollAfter = repository.getPoll(testEvent.id)
        assertNull(pollAfter, "Poll should be removed with event")
    }

    // ========================================================================
    // Offline Delete: Edge Cases
    // ========================================================================

    @Test
    fun `delete non-existent event fails gracefully`() = runBlocking {
        // WHEN: Trying to delete an event that doesn't exist
        val result = repository.deleteEvent("non-existent-id")

        // THEN: Operation fails with appropriate error
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `delete event is idempotent - second delete fails`() = runBlocking {
        // GIVEN: An event that was already deleted
        repository.createEvent(testEvent)
        val firstDelete = repository.deleteEvent(testEvent.id)
        assertTrue(firstDelete.isSuccess)

        // WHEN: Trying to delete the same event again
        val secondDelete = repository.deleteEvent(testEvent.id)

        // THEN: Second delete fails (event no longer exists)
        assertFalse(secondDelete.isSuccess)
    }

    @Test
    fun `delete event with participants removes all participants`() = runBlocking {
        // GIVEN: Event with participants
        repository.createEvent(testEvent)
        assertEquals(2, testEvent.participants.size)

        // WHEN: Event is deleted
        repository.deleteEvent(testEvent.id)

        // THEN: Event and its participants data is removed
        val deletedEvent = repository.getEvent(testEvent.id)
        assertNull(deletedEvent)
    }

    // ========================================================================
    // Offline Delete: Status-based Rules
    // ========================================================================

    @Test
    fun `can delete DRAFT event offline`() = runBlocking {
        val draftEvent = testEvent.copy(status = EventStatus.DRAFT)
        repository.createEvent(draftEvent)

        val result = repository.deleteEvent(draftEvent.id)

        assertTrue(result.isSuccess)
        assertNull(repository.getEvent(draftEvent.id))
    }

    @Test
    fun `can delete POLLING event offline`() = runBlocking {
        val pollingEvent = testEvent.copy(id = "polling-1", status = EventStatus.POLLING)
        repository.createEvent(pollingEvent)

        val result = repository.deleteEvent(pollingEvent.id)

        assertTrue(result.isSuccess)
        assertNull(repository.getEvent(pollingEvent.id))
    }

    @Test
    fun `can delete CONFIRMED event offline`() = runBlocking {
        val confirmedEvent = testEvent.copy(id = "confirmed-1", status = EventStatus.CONFIRMED)
        repository.createEvent(confirmedEvent)

        val result = repository.deleteEvent(confirmedEvent.id)

        assertTrue(result.isSuccess)
        assertNull(repository.getEvent(confirmedEvent.id))
    }

    @Test
    fun `can delete ORGANIZING event offline`() = runBlocking {
        val organizingEvent = testEvent.copy(id = "organizing-1", status = EventStatus.ORGANIZING)
        repository.createEvent(organizingEvent)

        val result = repository.deleteEvent(organizingEvent.id)

        assertTrue(result.isSuccess)
        assertNull(repository.getEvent(organizingEvent.id))
    }
}
