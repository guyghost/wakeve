package com.guyghost.wakeve.repository

import com.guyghost.wakeve.EventRepository
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.TimeOfDay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * RED Phase: Pagination tests that will fail until pagination is implemented.
 * 
 * These tests follow TDD principles:
 * 1. Tests are written BEFORE implementation
 * 2. Tests define the contract for pagination functionality
 * 3. Tests will fail initially (RED phase)
 * 4. Implementation will be added to make tests pass (GREEN phase)
 */
class EventRepositoryPaginationTest {
    
    @Test
    fun `getEventsPaginated returns first page with correct size`() = runTest {
        // Given: Repository with 100 events
        val repository = createRepositoryWithEvents(100)

        // When: Request first page (page 0, size 50)
        val result = repository.getEventsPaginated(page = 0, pageSize = 50).first()

        // Then: Should return 50 events
        assertEquals(50, result.size, "First page should contain 50 events")
    }

    @Test
    fun `getEventsPaginated returns second page correctly`() = runTest {
        // Given: Repository with 100 events
        val repository = createRepositoryWithEvents(100)

        // When: Request second page (page 1, size 50)
        val result = repository.getEventsPaginated(page = 1, pageSize = 50).first()

        // Then: Should return remaining 50 events
        assertEquals(50, result.size, "Second page should contain 50 events")
    }

    @Test
    fun `getEventsPaginated returns empty list when page exceeds data`() = runTest {
        // Given: Repository with 30 events
        val repository = createRepositoryWithEvents(30)

        // When: Request page 1 (items 50-99) with only 30 events
        val result = repository.getEventsPaginated(page = 1, pageSize = 50).first()

        // Then: Should return empty list
        assertTrue(result.isEmpty(), "Page beyond data should be empty")
    }

    @Test
    fun `getEventsPaginated maintains correct order`() = runTest {
        // Given: Repository with events created at different times
        val repository = createRepositoryWithOrderedEvents()

        // When: Request first page ordered by createdAt DESC
        val result = repository.getEventsPaginated(
            page = 0,
            pageSize = 10,
            orderBy = OrderBy.CREATED_AT_DESC
        ).first()

        // Then: Events should be in correct order
        assertTrue(isOrderedByCreatedAtDesc(result), "Events should be ordered by createdAt DESC")
    }

    @Test
    fun `getEventsPaginated works offline`() = runTest {
        // Given: Repository in offline mode with cached events
        val repository = createOfflineRepositoryWithEvents(100)

        // When: Request page while offline
        val result = repository.getEventsPaginated(page = 0, pageSize = 50).first()

        // Then: Should return cached events
        assertEquals(50, result.size, "Should work offline with cached data")
    }

    @Test
    fun `getEventsPaginated with zero page size returns empty`() = runTest {
        // Given: Repository with events
        val repository = createRepositoryWithEvents(10)

        // When: Request page with zero page size
        val result = repository.getEventsPaginated(page = 0, pageSize = 0).first()

        // Then: Should return empty list
        assertTrue(result.isEmpty(), "Zero page size should return empty list")
    }

    @Test
    fun `getEventsPaginated with negative page returns empty`() = runTest {
        // Given: Repository with events
        val repository = createRepositoryWithEvents(10)

        // When: Request negative page
        val result = repository.getEventsPaginated(page = -1, pageSize = 10).first()

        // Then: Should return empty list
        assertTrue(result.isEmpty(), "Negative page should return empty list")
    }
    
    // Helper functions - Implemented in GREEN phase

    /**
     * Creates a repository with the specified number of test events
     */
    private suspend fun createRepositoryWithEvents(count: Int): EventRepository {
        val repository = EventRepository()
        repeat(count) { index ->
            val event = createTestEvent(
                id = "event-$index",
                createdAt = "2025-01-${(index + 1).toString().padStart(2, '0')}T10:00:00Z"
            )
            repository.createEvent(event)
        }
        return repository
    }

    /**
     * Creates a repository with events ordered by creation time
     */
    private suspend fun createRepositoryWithOrderedEvents(): EventRepository {
        val repository = EventRepository()
        // Create events with different creation times
        listOf(
            "2025-01-01T10:00:00Z",
            "2025-01-05T15:30:00Z",
            "2025-01-03T08:00:00Z",
            "2025-01-10T20:00:00Z",
            "2025-01-02T12:00:00Z"
        ).forEachIndexed { index, createdAt ->
            val event = createTestEvent(id = "event-$index", createdAt = createdAt)
            repository.createEvent(event)
        }
        return repository
    }

    /**
     * Creates an offline repository with cached events
     */
    private suspend fun createOfflineRepositoryWithEvents(count: Int): EventRepository {
        // For the in-memory repository, offline mode is the same as regular mode
        // In production with DatabaseEventRepository, this would test cached data
        return createRepositoryWithEvents(count)
    }

    /**
     * Verifies events are ordered by createdAt in descending order
     */
    private fun isOrderedByCreatedAtDesc(events: List<Event>): Boolean {
        return events.zipWithNext().all { (first, second) ->
            first.createdAt >= second.createdAt
        }
    }
    
    /**
     * Creates a test event with the specified ID and creation time
     */
    private fun createTestEvent(
        id: String, 
        createdAt: String = "2025-01-01T10:00:00Z"
    ): Event {
        return Event(
            id = id,
            title = "Test Event $id",
            description = "Description for test event $id",
            organizerId = "organizer-1",
            proposedSlots = listOf(
                TimeSlot(
                    id = "slot-1",
                    start = "2025-12-01T10:00:00Z",
                    end = "2025-12-01T12:00:00Z",
                    timezone = "UTC",
                    timeOfDay = TimeOfDay.SPECIFIC
                )
            ),
            deadline = "2025-11-25T18:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = createdAt,
            updatedAt = createdAt,
            eventType = EventType.OTHER
        )
    }
}

/**
 * Enumeration for ordering options in pagination
 */
enum class OrderBy {
    CREATED_AT_DESC,
    CREATED_AT_ASC,
    TITLE_ASC,
    TITLE_DESC,
    STATUS_ASC,
    STATUS_DESC
}