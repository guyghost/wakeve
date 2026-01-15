package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [CreateEventUseCase].
 *
 * Verifies:
 * - Creating events successfully
 * - Validation of event fields (ID, title, organizer, slots, deadline)
 * - Handling repository errors
 * - Result wrapping
 * - Suspend function invocation
 */
class CreateEventUseCaseTest {

    // ========================================================================
    // Mock Repository Implementation
    // ========================================================================

    class MockEventRepository : EventRepositoryInterface {
        var events = mutableMapOf<String, Event>()
        var shouldFailCreate = false

        override suspend fun createEvent(event: Event): Result<Event> {
            return if (shouldFailCreate) {
                Result.failure(Exception("Repository error: Failed to create event"))
            } else {
                events[event.id] = event
                Result.success(event)
            }
        }

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

        override suspend fun saveEvent(event: Event): Result<Event> {
            val existingEvent = events[event.id]
            if (existingEvent != null) {
                events[event.id] = event
            } else {
                events[event.id] = event
            }
            return Result.success(event)
        }

        override fun isDeadlinePassed(deadline: String): Boolean = false

        override fun isOrganizer(eventId: String, userId: String): Boolean = true

        override fun canModifyEvent(eventId: String, userId: String): Boolean = true

        override fun getAllEvents(): List<Event> = events.values.toList()

        override suspend fun deleteEvent(eventId: String): Result<Unit> {
            events.remove(eventId)
            return Result.success(Unit)
        }
    }

    // ========================================================================
    // Test Helpers
    // ========================================================================

    private fun createValidEvent(
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
    fun testCreateEvent_Success() = runTest {
        val repository = MockEventRepository()
        val useCase = CreateEventUseCase(repository)

        val event = createValidEvent("evt-1", "New Event")
        val result = useCase(event)

        assertTrue(result.isSuccess)
        val createdEvent = result.getOrNull()
        assertNotNull(createdEvent)
        assertEquals("evt-1", createdEvent.id)
        assertEquals("New Event", createdEvent.title)
    }

    @Test
    fun testCreateEvent_ValidationError_EmptyId() = runTest {
        val repository = MockEventRepository()
        val useCase = CreateEventUseCase(repository)

        val event = createValidEvent("", "Valid Title")
        val result = useCase(event)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue("Event ID cannot be empty" in exception.message.orEmpty())
    }

    @Test
    fun testCreateEvent_ValidationError_EmptyTitle() = runTest {
        val repository = MockEventRepository()
        val useCase = CreateEventUseCase(repository)

        val event = createValidEvent("evt-1", "")
        val result = useCase(event)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue("Event title cannot be empty" in exception.message.orEmpty())
    }

    @Test
    fun testCreateEvent_ValidationError_EmptyOrganizerID() = runTest {
        val repository = MockEventRepository()
        val useCase = CreateEventUseCase(repository)

        val event = createValidEvent("evt-1", "Valid Title").copy(organizerId = "")
        val result = useCase(event)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue("Organizer ID cannot be empty" in exception.message.orEmpty())
    }

    @Test
    fun testCreateEvent_ValidationError_NoProposedSlots() = runTest {
        val repository = MockEventRepository()
        val useCase = CreateEventUseCase(repository)

        val event = createValidEvent("evt-1", "Valid Title").copy(proposedSlots = emptyList())
        val result = useCase(event)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue("At least one time slot is required" in exception.message.orEmpty())
    }

    @Test
    fun testCreateEvent_ValidationError_EmptyDeadline() = runTest {
        val repository = MockEventRepository()
        val useCase = CreateEventUseCase(repository)

        val event = createValidEvent("evt-1", "Valid Title").copy(deadline = "")
        val result = useCase(event)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue("Deadline cannot be empty" in exception.message.orEmpty())
    }

    @Test
    fun testCreateEvent_RepositoryError() = runTest {
        val repository = MockEventRepository()
        repository.shouldFailCreate = true
        val useCase = CreateEventUseCase(repository)

        val event = createValidEvent("evt-1", "New Event")
        val result = useCase(event)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue("Failed to create event" in exception.message.orEmpty())
    }
}
