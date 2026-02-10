package com.guyghost.wakeve.presentation.statemachine

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.Coordinates
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.EventRepositoryInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

/**
 * Mock EventRepository for testing edge cases
 */
class MockEventRepository : EventRepositoryInterface {
    val events = mutableMapOf<String, Event>()
    val participants = mutableMapOf<String, List<String>>()
    val polls = mutableMapOf<String, com.guyghost.wakeve.models.Poll>()
    var shouldFail = false
    var failureMessage = "Repository failure"
    var isOffline = false
    var pendingSyncs = mutableMapOf<String, EventStatus>()
    
    override suspend fun createEvent(event: Event): Result<Event> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        events[event.id] = event
        return Result.success(event)
    }
    
    override fun getEvent(id: String): Event? {
        if (shouldFail) return null
        return events[id]
    }
    
    override fun getPoll(eventId: String): com.guyghost.wakeve.models.Poll? {
        if (shouldFail) return null
        return polls[eventId]
    }
    
    override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        val current = participants[eventId]?.toMutableList() ?: mutableListOf()
        current.add(participantId)
        participants[eventId] = current
        return Result.success(true)
    }
    
    override fun getParticipants(eventId: String): List<String>? {
        if (shouldFail) return null
        return participants[eventId]
    }
    
    override suspend fun addVote(eventId: String, participantId: String, slotId: String, vote: Vote): Result<Boolean> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        return Result.success(true)
    }
    
    override suspend fun updateEvent(event: Event): Result<Event> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        events[event.id] = event
        return Result.success(event)
    }
    
    override suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        val event = events[id] ?: return Result.failure(Exception("Event not found"))
        val updatedEvent = event.copy(status = status, finalDate = finalDate)
        events[id] = updatedEvent
        return Result.success(true)
    }
    
    override suspend fun saveEvent(event: Event): Result<Event> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        events[event.id] = event
        return Result.success(event)
    }
    
    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        events.remove(eventId)
        return Result.success(Unit)
    }
    
    override fun isDeadlinePassed(deadline: String): Boolean = false
    
    override fun isOrganizer(eventId: String, userId: String): Boolean {
        val event = events[eventId] ?: return false
        return event.organizerId == userId
    }
    
    override fun canModifyEvent(eventId: String, userId: String): Boolean {
        return isOrganizer(eventId, userId)
    }
    
    override fun getAllEvents(): List<Event> = events.values.toList()
    
    override fun getEventsPaginated(page: Int, pageSize: Int, orderBy: com.guyghost.wakeve.repository.OrderBy): kotlinx.coroutines.flow.Flow<List<Event>> {
        return kotlinx.coroutines.flow.flowOf(events.values.toList())
    }
    
    fun setPendingSync(eventId: String, status: EventStatus) {
        pendingSyncs[eventId] = status
    }
    
    fun clearPendingSync(eventId: String) {
        pendingSyncs.remove(eventId)
    }
    
    fun configureOffline(offline: Boolean) {
        this.isOffline = offline
    }
}

/**
 * Mock use cases for testing
 */
class MockLoadEventsUseCase {
    var shouldFail = false
    var failureMessage = "Load failed"
    var events: List<Event> = emptyList()
    
    operator fun invoke(): Result<List<Event>> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        return Result.success(events)
    }
}

class MockCreateEventUseCase {
    var shouldFail = false
    var failureMessage = "Create failed"
    
    suspend operator fun invoke(event: Event): Result<Event> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        return Result.success(event)
    }
}

/**
 * Edge case tests for EventManagementStateMachine
 * 
 * Tests:
 * - Concurrent operations
 * - Invalid state transitions
 * - Race conditions
 * - Boundary conditions
 * - Recovery scenarios
 * - Offline scenarios
 */
@ExperimentalCoroutinesApi
class EventManagementStateMachineEdgeCasesTest {

    private lateinit var mockRepository: MockEventRepository
    private lateinit var mockLoadUseCase: MockLoadEventsUseCase
    private lateinit var mockCreateUseCase: MockCreateEventUseCase
    private lateinit var testScope: TestScope
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var stateMachine: EventManagementStateMachine
    
    @BeforeTest
    fun setup() {
        mockRepository = MockEventRepository()
        mockLoadUseCase = MockLoadEventsUseCase()
        mockCreateUseCase = MockCreateEventUseCase()
        testScope = TestScope(testDispatcher)
        
        stateMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(mockRepository),
            createEventUseCase = CreateEventUseCase(mockRepository),
            eventRepository = mockRepository,
            scope = testScope
        )
    }

    // ==================== CONCURRENT OPERATIONS ====================
    
    @Test
    fun `concurrent state transitions maintain consistency`() = runTest {
        // Given
        val eventId = "event-1"
        val organizerId = "user-1"
        val event = createDraftEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        
        // When - dispatch multiple intents rapidly from different contexts
        stateMachine.dispatch(EventManagementContract.Intent.StartPoll(eventId, organizerId))
        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", organizerId))
        stateMachine.dispatch(EventManagementContract.Intent.TransitionToOrganizing(eventId, organizerId))
        
        advanceUntilIdle()
        advanceUntilIdle()
        
        // Then - state should be consistent (only valid transitions succeed)
        val finalState = stateMachine.state.value
        val finalEvent = mockRepository.events[eventId]
        
        // Should end up in a valid state
        assertTrue(
            finalEvent?.status == EventStatus.DRAFT || 
            finalEvent?.status == EventStatus.POLLING ||
            finalEvent?.status == EventStatus.CONFIRMED
        )
        
        // Error should be set for invalid transitions
        if (finalEvent?.status == EventStatus.DRAFT) {
            assertNotNull(finalState.error)
        }
    }

    @Test
    fun `state machine handles repository failure gracefully`() = runTest {
        // Given
        mockRepository.shouldFail = true
        val eventId = "event-1"
        
        // When
        stateMachine.dispatch(EventManagementContract.Intent.SelectEvent(eventId))
        advanceUntilIdle()
        
        // Then - should show error, not crash
        val state = stateMachine.state.value
        assertNotNull(state.error)
        assertTrue(state.error?.contains("Repository failure") == true)
    }

    @Test
    fun `rapid intent dispatching doesn't cause corruption`() = runTest {
        // Given
        val eventId = "event-1"
        val event = createDraftEvent(eventId)
        mockRepository.events[eventId] = event
        
        // When - dispatch many intents rapidly
        repeat(100) {
            stateMachine.dispatch(EventManagementContract.Intent.SelectEvent(eventId))
        }
        advanceUntilIdle()
        
        // Then - state should be consistent
        val state = stateMachine.state.value
        assertEquals(eventId, state.selectedEvent?.id)
        // Should not crash or have corrupted state
    }

    // ==================== INVALID STATE TRANSITIONS ====================
    
    @Test
    fun `cannot start poll from CONFIRMED status`() = runTest {
        // Given
        val eventId = "event-1"
        val organizerId = "user-1"
        val event = createConfirmedEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        
        // When - try invalid transition
        stateMachine.dispatch(EventManagementContract.Intent.StartPoll(eventId, organizerId))
        advanceUntilIdle()
        
        // Then - should remain CONFIRMED with error
        val state = stateMachine.state.value
        assertEquals(EventStatus.CONFIRMED, mockRepository.events[eventId]?.status)
        assertTrue(state.error?.contains("Cannot start poll") == true)
    }

    @Test
    fun `cannot confirm date from DRAFT status`() = runTest {
        // Given
        val eventId = "event-1"
        val organizerId = "user-1"
        val event = createDraftEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        
        // When - try invalid transition
        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", organizerId))
        advanceUntilIdle()
        
        // Then - should remain DRAFT with error
        val state = stateMachine.state.value
        assertEquals(EventStatus.DRAFT, mockRepository.events[eventId]?.status)
        assertTrue(state.error?.contains("Cannot confirm date") == true)
    }

    @Test
    fun `cannot transition to organizing from DRAFT status`() = runTest {
        // Given
        val eventId = "event-1"
        val organizerId = "user-1"
        val event = createDraftEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        
        // When - try invalid transition
        stateMachine.dispatch(EventManagementContract.Intent.TransitionToOrganizing(eventId, organizerId))
        advanceUntilIdle()
        
        // Then - should remain DRAFT with error
        val state = stateMachine.state.value
        assertEquals(EventStatus.DRAFT, mockRepository.events[eventId]?.status)
        assertTrue(state.error?.contains("Cannot transition to organizing") == true)
    }

    @Test
    fun `cannot finalize from CONFIRMED status`() = runTest {
        // Given
        val eventId = "event-1"
        val organizerId = "user-1"
        val event = createConfirmedEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        
        // When - try invalid transition
        stateMachine.dispatch(EventManagementContract.Intent.MarkAsFinalized(eventId, organizerId))
        advanceUntilIdle()
        
        // Then - should remain CONFIRMED with error
        val state = stateMachine.state.value
        assertEquals(EventStatus.CONFIRMED, mockRepository.events[eventId]?.status)
        assertTrue(state.error?.contains("Cannot finalize") == true)
    }

    @Test
    fun `cannot update draft event when not DRAFT`() = runTest {
        // Given
        val eventId = "event-1"
        val event = createPollingEvent(eventId)
        mockRepository.events[eventId] = event
        
        // When - try to update draft fields
        stateMachine.dispatch(EventManagementContract.Intent.UpdateDraftEvent(
            eventId = eventId,
            eventType = EventType.BIRTHDAY
        ))
        advanceUntilIdle()
        
        // Then - should fail
        val state = stateMachine.state.value
        assertTrue(state.error?.contains("Cannot update draft") == true)
        assertEquals(EventStatus.POLLING, mockRepository.events[eventId]?.status)
    }

    // ==================== RACE CONDITIONS ====================
    
    @Test
    fun `poll closure while user is voting`() = runTest {
        // Given - POLLING state with active voting
        val eventId = "event-1"
        val organizerId = "user-1"
        val event = createPollingEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        
        // Set up poll with votes
        val poll = com.guyghost.wakeve.models.Poll(
            id = "poll-1",
            eventId = eventId,
            votes = mapOf(
                "slot-1" to mapOf("user-1" to Vote.YES, "user-2" to Vote.YES),
                "slot-2" to mapOf("user-3" to Vote.MAYBE)
            )
        )
        mockRepository.polls[eventId] = poll
        
        // When - simulate concurrent poll closure while voting
        // Simulate another vote coming in (would normally come from VoteStateMachine)
        val updatedVotes = poll.votes.toMutableMap()
        val slotVotes = updatedVotes["slot-1"]?.toMutableMap() ?: mutableMapOf()
        slotVotes["user-4"] = Vote.NO
        updatedVotes["slot-1"] = slotVotes
        val updatedPoll = poll.copy(votes = updatedVotes)
        mockRepository.polls[eventId] = updatedPoll
        
        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", organizerId))
        advanceUntilIdle()
        
        // Then - should handle gracefully (either accept vote or reject it)
        val finalState = stateMachine.state.value
        val finalEvent = mockRepository.events[eventId]
        
        assertTrue(
            finalEvent?.status == EventStatus.CONFIRMED ||
            finalEvent?.status == EventStatus.POLLING
        )
        
        // Should not crash
        assertNotNull(finalState)
    }

    @Test
    fun `event deletion during state transition`() = runTest {
        // Given
        val eventId = "event-1"
        val organizerId = "user-1"
        val event = createDraftEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        
        // When - delete event during transition
        stateMachine.dispatch(EventManagementContract.Intent.DeleteEvent(eventId, organizerId))
        stateMachine.dispatch(EventManagementContract.Intent.StartPoll(eventId, organizerId))
        advanceUntilIdle()
        
        // Then - should handle gracefully
        val state = stateMachine.state.value
        // Event should be deleted or transition should fail
        assertTrue(
            mockRepository.events.containsKey(eventId).not() ||
            state.error != null
        )
    }

    @Test
    fun `multiple users trying to confirm same date`() = runTest {
        // Given
        val eventId = "event-1"
        val organizerId = "user-1"
        val otherUserId = "user-2"
        val event = createPollingEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        
        // Set up poll
        val poll = com.guyghost.wakeve.models.Poll(
            id = "poll-1",
            eventId = eventId,
            votes = mapOf(
                "slot-1" to mapOf("user-1" to Vote.YES, "user-2" to Vote.YES)
            )
        )
        mockRepository.polls[eventId] = poll
        
        // When - both users try to confirm
        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", organizerId))
        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", otherUserId))
        advanceUntilIdle()
        
        // Then - only organizer should succeed
        val state = stateMachine.state.value
        val finalEvent = mockRepository.events[eventId]
        
        if (finalEvent?.status == EventStatus.CONFIRMED) {
            // Should have succeeded without errors
            assertNull(state.error)
        } else {
            // Should have error for unauthorized user
            assertTrue(state.error?.contains("Only event organizer can confirm dates") == true)
        }
    }

    // ==================== BOUNDARY CONDITIONS ====================
    
    @Test
    fun `state machine with empty event ID`() = runTest {
        // When
        stateMachine.dispatch(EventManagementContract.Intent.SelectEvent(""))
        advanceUntilIdle()
        
        // Then
        val state = stateMachine.state.value
        assertTrue(state.error?.contains("Event not found") == true)
    }

    @Test
    fun `state machine with null values in update`() = runTest {
        // Given
        val eventId = "event-1"
        val event = createDraftEvent(eventId)
        mockRepository.events[eventId] = event
        
        // When - try to update with all null values (should not crash)
        stateMachine.dispatch(EventManagementContract.Intent.UpdateDraftEvent(
            eventId = eventId,
            eventType = null,
            eventTypeCustom = null,
            expectedParticipants = null,
            minParticipants = null,
            maxParticipants = null
        ))
        advanceUntilIdle()
        
        // Then - should succeed without changes
        val state = stateMachine.state.value
        assertNull(state.error)
    }

    @Test
    fun `transition with non-existent slot ID`() = runTest {
        // Given
        val eventId = "event-1"
        val organizerId = "user-1"
        val event = createPollingEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        
        // Set up poll
        val poll = com.guyghost.wakeve.models.Poll(
            id = "poll-1",
            eventId = eventId,
            votes = mapOf("slot-1" to mapOf("user-1" to Vote.YES))
        )
        mockRepository.polls[eventId] = poll
        
        // When - try to confirm with non-existent slot
        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "non-existent-slot", organizerId))
        advanceUntilIdle()
        
        // Then - should fail validation
        val state = stateMachine.state.value
        assertTrue(state.error?.contains("Selected time slot not found") == true)
        assertEquals(EventStatus.POLLING, mockRepository.events[eventId]?.status)
    }

    @Test
    fun `negative participant counts validation`() = runTest {
        // Given
        val eventId = "event-1"
        val event = createDraftEvent(eventId)
        mockRepository.events[eventId] = event
        
        // When - try to set negative participants
        stateMachine.dispatch(EventManagementContract.Intent.UpdateDraftEvent(
            eventId = eventId,
            minParticipants = -5
        ))
        advanceUntilIdle()
        
        // Then - should fail validation
        val state = stateMachine.state.value
        assertTrue(state.error?.contains("must be non-negative") == true)
    }

    @Test
    fun `max participants less than min participants`() = runTest {
        // Given
        val eventId = "event-1"
        val event = createDraftEvent(eventId)
        mockRepository.events[eventId] = event
        
        // When - try to set invalid range
        stateMachine.dispatch(EventManagementContract.Intent.UpdateDraftEvent(
            eventId = eventId,
            minParticipants = 10,
            maxParticipants = 5
        ))
        advanceUntilIdle()
        
        // Then - should fail validation
        val state = stateMachine.state.value
        assertTrue(state.error?.contains("Max participants must be >= min") == true)
    }

    @Test
    fun `custom event type without description`() = runTest {
        // Given
        val eventId = "event-1"
        val event = createDraftEvent(eventId)
        mockRepository.events[eventId] = event
        
        // When - try to set custom type without description
        stateMachine.dispatch(EventManagementContract.Intent.UpdateDraftEvent(
            eventId = eventId,
            eventType = EventType.CUSTOM,
            eventTypeCustom = null
        ))
        advanceUntilIdle()
        
        // Then - should fail validation
        val state = stateMachine.state.value
        assertTrue(state.error?.contains("Custom event type requires a description") == true)
    }

    // ==================== RECOVERY ====================
    
    @Test
    fun `state machine recovers after error`() = runTest {
        // Given - previous error
        val eventId = "event-1"
        val organizerId = "user-1"
        mockRepository.shouldFail = true
        
        stateMachine.dispatch(EventManagementContract.Intent.SelectEvent(eventId))
        advanceUntilIdle()
        assertTrue(stateMachine.state.value.error != null)
        
        // When - fix repository and retry
        mockRepository.shouldFail = false
        val event = createDraftEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        
        stateMachine.dispatch(EventManagementContract.Intent.ClearError)
        stateMachine.dispatch(EventManagementContract.Intent.SelectEvent(eventId))
        advanceUntilIdle()
        
        // Then - should succeed
        val state = stateMachine.state.value
        assertNull(state.error)
        assertEquals(eventId, state.selectedEvent?.id)
    }

    @Test
    fun `multiple rapid retries after failure`() = runTest {
        // Given
        val eventId = "event-1"
        mockRepository.shouldFail = true
        
        // When - retry multiple times
        repeat(5) {
            stateMachine.dispatch(EventManagementContract.Intent.SelectEvent(eventId))
        }
        advanceUntilIdle()
        
        // Then - should handle gracefully without crashing
        val state = stateMachine.state.value
        assertNotNull(state.error)
        // Should not have multiple error states or corruption
    }

    @Test
    fun `repository failure doesn't corrupt state`() = runTest {
        // Given
        val eventId = "event-1"
        val event = createDraftEvent(eventId)
        mockRepository.events[eventId] = event
        
        // Get initial state
        stateMachine.dispatch(EventManagementContract.Intent.SelectEvent(eventId))
        advanceUntilIdle()
        val initialState = stateMachine.state.value
        
        // When - repository fails during update
        mockRepository.shouldFail = true
        stateMachine.dispatch(EventManagementContract.Intent.UpdateDraftEvent(
            eventId = eventId,
            eventType = EventType.BIRTHDAY
        ))
        advanceUntilIdle()
        
        // Then - state should not be corrupted
        val failedState = stateMachine.state.value
        assertNotNull(failedState.error)
        assertEquals(initialState.selectedEvent?.id, failedState.selectedEvent?.id)
        
        // When - repository recovers
        mockRepository.shouldFail = false
        stateMachine.dispatch(EventManagementContract.Intent.UpdateDraftEvent(
            eventId = eventId,
            eventType = EventType.BIRTHDAY
        ))
        advanceUntilIdle()
        
        // Then - should work again
        val recoveredState = stateMachine.state.value
        assertNull(recoveredState.error)
    }

    // ==================== OFFLINE SCENARIOS ====================
    
    @Test
    fun `state transition while offline`() = runTest {
        // Given - offline mode
        val eventId = "event-1"
        val organizerId = "user-1"
        val event = createDraftEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        mockRepository.configureOffline(true)
        
        // When
        stateMachine.dispatch(EventManagementContract.Intent.StartPoll(eventId, organizerId))
        advanceUntilIdle()
        
        // Then - should work (offline operations are allowed locally)
        val state = stateMachine.state.value
        val updatedEvent = mockRepository.events[eventId]
        
        // In offline mode, local operations should still work
        assertEquals(EventStatus.POLLING, updatedEvent?.status)
        assertNull(state.error)
    }

    @Test
    fun `sync conflicts during state transition`() = runTest {
        // Given - pending sync with different state
        val eventId = "event-1"
        val organizerId = "user-1"
        val event = createPollingEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        mockRepository.setPendingSync(eventId, EventStatus.CONFIRMED)
        
        // When - try to transition locally
        stateMachine.dispatch(EventManagementContract.Intent.TransitionToOrganizing(eventId, organizerId))
        advanceUntilIdle()
        
        // Then - should handle based on conflict resolution strategy
        val state = stateMachine.state.value
        val finalEvent = mockRepository.events[eventId]
        
        // Should either succeed locally or indicate conflict
        assertTrue(
            finalEvent?.status == EventStatus.ORGANIZING ||
            state.error != null
        )
    }

    // ==================== PERMISSION VALIDATION ====================
    
    @Test
    fun `non-organizer cannot start poll`() = runTest {
        // Given
        val eventId = "event-1"
        val organizerId = "user-1"
        val nonOrganizerId = "user-2"
        val event = createDraftEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        
        // When - non-organizer tries to start poll
        stateMachine.dispatch(EventManagementContract.Intent.StartPoll(eventId, nonOrganizerId))
        advanceUntilIdle()
        
        // Then - should fail
        val state = stateMachine.state.value
        assertTrue(state.error?.contains("Only event organizer can start poll") == true)
        assertEquals(EventStatus.DRAFT, mockRepository.events[eventId]?.status)
    }

    @Test
    fun `non-organizer cannot delete event`() = runTest {
        // Given
        val eventId = "event-1"
        val organizerId = "user-1"
        val nonOrganizerId = "user-2"
        val event = createDraftEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        
        // When - non-organizer tries to delete
        stateMachine.dispatch(EventManagementContract.Intent.DeleteEvent(eventId, nonOrganizerId))
        advanceUntilIdle()
        
        // Then - should fail
        val state = stateMachine.state.value
        assertTrue(state.error?.contains("Only event organizer can delete") == true)
        assertTrue(mockRepository.events.containsKey(eventId))
    }

    @Test
    fun `cannot delete finalized event`() = runTest {
        // Given
        val eventId = "event-1"
        val organizerId = "user-1"
        val event = createFinalizedEvent(eventId, organizerId)
        mockRepository.events[eventId] = event
        
        // When - try to delete finalized event
        stateMachine.dispatch(EventManagementContract.Intent.DeleteEvent(eventId, organizerId))
        advanceUntilIdle()
        
        // Then - should fail
        val state = stateMachine.state.value
        assertTrue(state.error?.contains("Cannot delete a finalized event") == true)
        assertTrue(mockRepository.events.containsKey(eventId))
    }

    // Helper methods
    private fun createDraftEvent(id: String, organizerId: String = "user-1") = Event(
        id = id,
        title = "Test Event",
        description = "Test Description",
        organizerId = organizerId,
        status = EventStatus.DRAFT,
        eventType = EventType.OTHER,
        proposedSlots = listOf(
            TimeSlot(
                id = "slot-1",
                start = Clock.System.now().toString(),
                end = Clock.System.now().toString(),
                timezone = "UTC",
                timeOfDay = TimeOfDay.MORNING
            )
        ),
        participants = emptyList(),
        deadline = Clock.System.now().toString(),
        createdAt = Clock.System.now().toString(),
        updatedAt = Clock.System.now().toString()
    )
    
    private fun createPollingEvent(id: String, organizerId: String = "user-1") = createDraftEvent(id, organizerId).copy(
        status = EventStatus.POLLING
    )
    
    private fun createConfirmedEvent(id: String, organizerId: String = "user-1") = createDraftEvent(id, organizerId).copy(
        status = EventStatus.CONFIRMED,
        finalDate = Clock.System.now().toString()
    )
    
    private fun createOrganizingEvent(id: String, organizerId: String = "user-1") = createDraftEvent(id, organizerId).copy(
        status = EventStatus.ORGANIZING,
        finalDate = Clock.System.now().toString()
    )
    
    private fun createFinalizedEvent(id: String, organizerId: String = "user-1") = createDraftEvent(id, organizerId).copy(
        status = EventStatus.FINALIZED,
        finalDate = Clock.System.now().toString()
    )
}