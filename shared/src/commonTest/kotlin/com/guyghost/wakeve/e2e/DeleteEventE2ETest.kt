package com.guyghost.wakeve.e2e

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * E2E Tests for Delete Event Feature
 * 
 * Tests the complete flow from UI intent to repository cascade delete.
 * Covers:
 * - Organizer can delete events in allowed statuses
 * - Non-organizer cannot delete
 * - FINALIZED events cannot be deleted
 * - Cascade delete removes all related data
 * - State machine emits correct side effects
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeleteEventE2ETest {

    // ========================================================================
    // Mock Repository for E2E Testing
    // ========================================================================

    class MockEventRepository : EventRepositoryInterface {
        var events = mutableMapOf<String, Event>()
        var polls = mutableMapOf<String, Poll>()
        var participants = mutableMapOf<String, List<String>>()
        var potentialLocations = mutableMapOf<String, MutableList<PotentialLocation>>()
        var userVotes = mutableMapOf<String, MutableMap<String, MutableMap<String, Vote>>>()
        var deletedEventIds = mutableListOf<String>()

        override suspend fun createEvent(event: Event): Result<Event> {
            events[event.id] = event
            polls[event.id] = Poll(event.id, event.id, emptyMap())
            participants[event.id] = event.participants
            potentialLocations[event.id] = mutableListOf()
            return Result.success(event)
        }

        override fun getEvent(id: String): Event? = events[id]
        override fun getPoll(eventId: String): Poll? = polls[eventId]

        override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> {
            val event = events[eventId] ?: return Result.failure(Exception("Event not found"))
            events[eventId] = event.copy(participants = event.participants + participantId)
            val current = participants[eventId] ?: emptyList()
            participants[eventId] = current + participantId
            return Result.success(true)
        }

        override fun getParticipants(eventId: String): List<String>? = participants[eventId]

        override suspend fun addVote(
            eventId: String,
            participantId: String,
            slotId: String,
            vote: Vote
        ): Result<Boolean> {
            val eventVotes = userVotes.getOrPut(eventId) { mutableMapOf() }
            val participantVotes = eventVotes.getOrPut(participantId) { mutableMapOf() }
            participantVotes[slotId] = vote
            return Result.success(true)
        }

        override suspend fun updateEvent(event: Event): Result<Event> {
            events[event.id] = event
            return Result.success(event)
        }

        override suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean> {
            val event = events[id] ?: return Result.failure(Exception("Event not found"))
            events[id] = event.copy(status = status, finalDate = finalDate)
            return Result.success(true)
        }

        override suspend fun saveEvent(event: Event): Result<Event> {
            events[event.id] = event
            return Result.success(event)
        }

        override suspend fun deleteEvent(eventId: String): Result<Unit> {
            if (events[eventId] == null) {
                return Result.failure(IllegalArgumentException("Event not found"))
            }
            // Cascade delete
            events.remove(eventId)
            polls.remove(eventId)
            participants.remove(eventId)
            potentialLocations.remove(eventId)
            userVotes.remove(eventId)
            deletedEventIds.add(eventId)
            return Result.success(Unit)
        }

        override fun isDeadlinePassed(deadline: String): Boolean = false
        override fun isOrganizer(eventId: String, userId: String): Boolean {
            return events[eventId]?.organizerId == userId
        }
        override fun canModifyEvent(eventId: String, userId: String): Boolean = isOrganizer(eventId, userId)
        override fun getAllEvents(): List<Event> = events.values.toList()
    }

    // ========================================================================
    // Test Helpers
    // ========================================================================

    private fun createTestEvent(
        id: String,
        title: String = "Test Event",
        organizerId: String = "organizer-1",
        eventType: EventType = EventType.TEAM_BUILDING,
        status: EventStatus = EventStatus.DRAFT
    ): Event = Event(
        id = id,
        title = title,
        description = "Test description",
        organizerId = organizerId,
        participants = listOf("participant-1"),
        proposedSlots = listOf(
            TimeSlot(
                id = "slot-1",
                start = "2026-02-20T09:00:00Z",
                end = "2026-02-20T17:00:00Z",
                timezone = "UTC"
            )
        ),
        deadline = "2026-02-15T23:59:59Z",
        status = status,
        createdAt = "2026-01-16T10:00:00Z",
        updatedAt = "2026-01-16T10:00:00Z",
        eventType = eventType
    )

    private fun createStateMachine(
        repository: MockEventRepository,
        dispatcher: kotlinx.coroutines.test.TestDispatcher
    ): Pair<EventManagementStateMachine, CoroutineScope> {
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val loadEventsUseCase = LoadEventsUseCase(repository)
        val createEventUseCase = CreateEventUseCase(repository)
        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = repository,
            scope = scope
        )
        return Pair(stateMachine, scope)
    }

    // ========================================================================
    // E2E Test: Delete DRAFT Event by Organizer
    // ========================================================================

    @Test
    fun testDeleteDraftEvent_OrganizerSuccess() = runTest {
        // GIVEN: A DRAFT event created by Alice
        val eventId = "delete-e2e-1"
        val organizerId = "alice"
        val repository = MockEventRepository()
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val event = createTestEvent(
            id = eventId,
            title = "Event to Delete",
            organizerId = organizerId,
            status = EventStatus.DRAFT
        )
        repository.createEvent(event)
        assertNotNull(repository.getEvent(eventId), "Event should exist before deletion")

        val (stateMachine, scope) = createStateMachine(repository, testDispatcher)

        // Load events first
        stateMachine.dispatch(EventManagementContract.Intent.LoadEvents)
        advanceUntilIdle()

        // WHEN: Organizer deletes the event
        stateMachine.dispatch(EventManagementContract.Intent.DeleteEvent(eventId, organizerId))
        advanceUntilIdle()

        // THEN: Event is deleted
        assertNull(repository.getEvent(eventId), "Event should be deleted from repository")
        assertNull(repository.getPoll(eventId), "Poll should be cascade deleted")
        assertTrue(repository.deletedEventIds.contains(eventId), "Event ID should be recorded as deleted")

        // THEN: State machine state is updated
        val state = stateMachine.state.value
        assertFalse(state.events.any { it.id == eventId }, "Event should be removed from state")

        scope.cancel()
    }

    // ========================================================================
    // E2E Test: Delete POLLING Event by Organizer
    // ========================================================================

    @Test
    fun testDeletePollingEvent_OrganizerSuccess() = runTest {
        // GIVEN: A POLLING event with votes
        val eventId = "delete-e2e-2"
        val organizerId = "alice"
        val repository = MockEventRepository()
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val event = createTestEvent(
            id = eventId,
            title = "Polling Event to Delete",
            organizerId = organizerId,
            status = EventStatus.POLLING
        )
        repository.createEvent(event)
        repository.addVote(eventId, "bob", "slot-1", Vote.YES)
        repository.addVote(eventId, "charlie", "slot-1", Vote.MAYBE)

        val (stateMachine, scope) = createStateMachine(repository, testDispatcher)

        stateMachine.dispatch(EventManagementContract.Intent.LoadEvents)
        advanceUntilIdle()

        // WHEN: Organizer deletes the event
        stateMachine.dispatch(EventManagementContract.Intent.DeleteEvent(eventId, organizerId))
        advanceUntilIdle()

        // THEN: Event and votes are deleted
        assertNull(repository.getEvent(eventId))
        assertNull(repository.userVotes[eventId], "Votes should be cascade deleted")
        assertTrue(repository.deletedEventIds.contains(eventId))

        scope.cancel()
    }

    // ========================================================================
    // E2E Test: Non-Organizer Cannot Delete
    // ========================================================================

    @Test
    fun testDeleteEvent_NonOrganizerFails() = runTest {
        // GIVEN: An event created by Alice
        val eventId = "delete-e2e-3"
        val organizerId = "alice"
        val attackerId = "eve"
        val repository = MockEventRepository()
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val event = createTestEvent(
            id = eventId,
            title = "Protected Event",
            organizerId = organizerId,
            status = EventStatus.DRAFT
        )
        repository.createEvent(event)

        val (stateMachine, scope) = createStateMachine(repository, testDispatcher)

        stateMachine.dispatch(EventManagementContract.Intent.LoadEvents)
        advanceUntilIdle()

        // WHEN: Non-organizer tries to delete
        stateMachine.dispatch(EventManagementContract.Intent.DeleteEvent(eventId, attackerId))
        advanceUntilIdle()

        // THEN: Event is NOT deleted
        assertNotNull(repository.getEvent(eventId), "Event should still exist")
        assertFalse(repository.deletedEventIds.contains(eventId))

        // THEN: Error is in state
        val state = stateMachine.state.value
        assertNotNull(state.error, "Error should be set in state")

        scope.cancel()
    }

    // ========================================================================
    // E2E Test: Cannot Delete FINALIZED Event
    // ========================================================================

    @Test
    fun testDeleteFinalizedEvent_Fails() = runTest {
        // GIVEN: A FINALIZED event
        val eventId = "delete-e2e-4"
        val organizerId = "alice"
        val repository = MockEventRepository()
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val event = createTestEvent(
            id = eventId,
            title = "Finalized Event",
            organizerId = organizerId,
            status = EventStatus.FINALIZED
        )
        repository.createEvent(event)

        val (stateMachine, scope) = createStateMachine(repository, testDispatcher)

        stateMachine.dispatch(EventManagementContract.Intent.LoadEvents)
        advanceUntilIdle()

        // WHEN: Organizer tries to delete FINALIZED event
        stateMachine.dispatch(EventManagementContract.Intent.DeleteEvent(eventId, organizerId))
        advanceUntilIdle()

        // THEN: Event is NOT deleted
        assertNotNull(repository.getEvent(eventId), "FINALIZED event should still exist")
        assertFalse(repository.deletedEventIds.contains(eventId))

        // THEN: Error is in state
        val state = stateMachine.state.value
        assertNotNull(state.error, "Error should be set for FINALIZED deletion attempt")

        scope.cancel()
    }

    // ========================================================================
    // E2E Test: Delete Multiple Events Sequentially
    // ========================================================================

    @Test
    fun testDeleteMultipleEvents_Sequential() = runTest {
        // GIVEN: Multiple events
        val repository = MockEventRepository()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val organizerId = "alice"

        listOf("event-a", "event-b", "event-c").forEach { id ->
            repository.createEvent(
                createTestEvent(
                    id = id,
                    title = "Event $id",
                    organizerId = organizerId,
                    status = EventStatus.DRAFT
                )
            )
        }
        assertEquals(3, repository.getAllEvents().size)

        val (stateMachine, scope) = createStateMachine(repository, testDispatcher)

        stateMachine.dispatch(EventManagementContract.Intent.LoadEvents)
        advanceUntilIdle()

        // WHEN: Delete events one by one
        stateMachine.dispatch(EventManagementContract.Intent.DeleteEvent("event-a", organizerId))
        advanceUntilIdle()
        assertEquals(2, repository.getAllEvents().size)

        stateMachine.dispatch(EventManagementContract.Intent.DeleteEvent("event-b", organizerId))
        advanceUntilIdle()
        assertEquals(1, repository.getAllEvents().size)

        // THEN: Only event-c remains
        assertNull(repository.getEvent("event-a"))
        assertNull(repository.getEvent("event-b"))
        assertNotNull(repository.getEvent("event-c"))

        scope.cancel()
    }

    // ========================================================================
    // E2E Test: Delete Non-Existent Event
    // ========================================================================

    @Test
    fun testDeleteNonExistentEvent_Fails() = runTest {
        // GIVEN: Empty repository
        val repository = MockEventRepository()
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val (stateMachine, scope) = createStateMachine(repository, testDispatcher)

        // WHEN: Try to delete non-existent event
        stateMachine.dispatch(EventManagementContract.Intent.DeleteEvent("non-existent", "alice"))
        advanceUntilIdle()

        // THEN: Error is in state
        val state = stateMachine.state.value
        assertNotNull(state.error, "Error should be set for non-existent event")

        scope.cancel()
    }
}
