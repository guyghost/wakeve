package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [LoadEventsUseCase].
 *
 * Verifies:
 * - Loading events successfully
 * - Handling empty event list
 * - Handling exceptions from repository
 * - Result wrapping (success vs failure)
 * - Use case invocation with invoke()
 */
class LoadEventsUseCaseTest {

    // ========================================================================
    // Mock Repository Implementation
    // ========================================================================

    class MockEventRepository : EventRepositoryInterface {
        var events = mutableMapOf<String, Event>()
        var shouldThrowException = false

        override suspend fun createEvent(event: Event): Result<Event> = Result.success(event)

        override fun getEvent(id: String): Event? = events[id]

        override fun getPoll(eventId: String) = null

        override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> =
            Result.success(true)

        override fun getParticipants(eventId: String): List<String>? = null

        override suspend fun addVote(eventId: String, participantId: String, slotId: String, vote: com.guyghost.wakeve.models.Vote): Result<Boolean> =
            Result.success(true)

        override suspend fun updateEvent(event: Event): Result<Event> = Result.success(event)

        override suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean> =
            Result.success(true)

        override fun isDeadlinePassed(deadline: String): Boolean = false

        override fun isOrganizer(eventId: String, userId: String): Boolean = true

        override fun canModifyEvent(eventId: String, userId: String): Boolean = true

        override fun getAllEvents(): List<Event> {
            if (shouldThrowException) {
                throw Exception("Repository error: Failed to load events")
            }
            return events.values.toList()
        }
    }

    // ========================================================================
    // Test Helpers
    // ========================================================================

    private fun createTestEvent(
        id: String = "evt-1",
        title: String = "Test Event"
    ): Event = Event(
        id = id,
        title = title,
        description = "Test event description",
        organizerId = "org-1",
        participants = emptyList(),
        proposedSlots = listOf(
            TimeSlot(
                id = "slot-1",
                start = "2025-12-20T10:00:00Z",
                end = "2025-12-20T12:00:00Z",
                timezone = "UTC"
            )
        ),
        deadline = "2025-12-15T18:00:00Z",
        status = EventStatus.DRAFT,
        createdAt = "2025-12-01T10:00:00Z",
        updatedAt = "2025-12-01T10:00:00Z"
    )

    // ========================================================================
    // Tests
    // ========================================================================

    @Test
    fun testLoadEvents_Success() {
        val repository = MockEventRepository()
        val event1 = createTestEvent("evt-1", "Event 1")
        val event2 = createTestEvent("evt-2", "Event 2")
        repository.events[event1.id] = event1
        repository.events[event2.id] = event2

        val useCase = LoadEventsUseCase(repository)
        val result = useCase()

        assertTrue(result.isSuccess)
        val events = result.getOrNull()
        assertNotNull(events)
        assertEquals(2, events.size)
        assertEquals("Event 1", events[0].title)
        assertEquals("Event 2", events[1].title)
    }

    @Test
    fun testLoadEvents_EmptyList() {
        val repository = MockEventRepository()
        val useCase = LoadEventsUseCase(repository)
        val result = useCase()

        assertTrue(result.isSuccess)
        val events = result.getOrNull()
        assertNotNull(events)
        assertTrue(events.isEmpty())
    }

    @Test
    fun testLoadEvents_Exception() {
        val repository = MockEventRepository()
        repository.shouldThrowException = true

        val useCase = LoadEventsUseCase(repository)
        val result = useCase()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue("Failed to load events" in exception.message.orEmpty())
    }

    @Test
    fun testLoadEvents_ResultWrapping() {
        val repository = MockEventRepository()
        val event = createTestEvent("evt-1", "Test Event")
        repository.events[event.id] = event

        val useCase = LoadEventsUseCase(repository)
        val result = useCase()

        // Verify Result is properly wrapping the value
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun testLoadEvents_InvokeOperator() {
        val repository = MockEventRepository()
        val event = createTestEvent("evt-1", "Test Event")
        repository.events[event.id] = event

        val useCase = LoadEventsUseCase(repository)

        // Use invoke operator (operator fun invoke())
        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }
}
