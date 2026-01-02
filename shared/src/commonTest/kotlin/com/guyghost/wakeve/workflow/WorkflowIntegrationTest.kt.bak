package com.guyghost.wakeve.workflow

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * Integration tests for State Machine Workflow Coordination.
 * 
 * Tests the complete workflow from DRAFT → FINALIZED, validating:
 * - Event.status propagation through repository
 * - Cross-state-machine coordination via shared repository
 * - Navigation side effects at each transition
 * - Business rule enforcement across the workflow
 * 
 * Phase 5 - Integration Testing
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkflowIntegrationTest {

    // ========================================================================
    // Mock Implementations
    // ========================================================================

    /**
     * Mock EventRepository for integration testing.
     * Simulates repository-mediated communication between state machines.
     */
    class MockEventRepository : EventRepositoryInterface {
        var events = mutableMapOf<String, Event>()
        var polls = mutableMapOf<String, Poll>()
        var participants = mutableMapOf<String, List<String>>()

        override suspend fun createEvent(event: Event): Result<Event> {
            events[event.id] = event
            return Result.success(event)
        }

        override fun getEvent(id: String): Event? = events[id]

        override fun getPoll(eventId: String): Poll? = polls[eventId]

        override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> {
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
            val poll = polls[eventId]
            if (poll == null) {
                polls[eventId] = Poll(
                    id = "poll-$eventId", 
                    eventId = eventId,
                    votes = mapOf(participantId to mapOf(slotId to vote))
                )
            } else {
                val updatedVotes = poll.votes.toMutableMap()
                val participantVotes = updatedVotes[participantId]?.toMutableMap() ?: mutableMapOf()
                participantVotes[slotId] = vote
                updatedVotes[participantId] = participantVotes
                polls[eventId] = poll.copy(votes = updatedVotes)
            }
            return Result.success(true)
        }

        override suspend fun updateEvent(event: Event): Result<Event> {
            events[event.id] = event
            return Result.success(event)
        }

        override suspend fun updateEventStatus(
            id: String,
            status: EventStatus,
            finalDate: String?
        ): Result<Boolean> {
            val event = events[id] ?: return Result.failure(Exception("Event not found"))
            events[id] = event.copy(status = status, finalDate = finalDate)
            return Result.success(true)
        }

        override fun isDeadlinePassed(deadline: String): Boolean = false

        override fun isOrganizer(eventId: String, userId: String): Boolean = true

        override fun canModifyEvent(eventId: String, userId: String): Boolean = true

        override fun getAllEvents(): List<Event> = events.values.toList()
    }

    // ========================================================================
    // Helper Functions
    // ========================================================================

    private fun createTestEvent(
        id: String = "event-1",
        status: EventStatus = EventStatus.DRAFT
    ): Event = Event(
        id = id,
        title = "Integration Test Event",
        description = "Event for integration testing",
        organizerId = "organizer-1",
        participants = listOf("organizer-1", "participant-1", "participant-2"),
        proposedSlots = listOf(
            TimeSlot(
                id = "slot-1",
                start = "2025-12-20T10:00:00Z",
                end = "2025-12-20T12:00:00Z",
                timezone = "UTC"
            ),
            TimeSlot(
                id = "slot-2",
                start = "2025-12-21T14:00:00Z",
                end = "2025-12-21T16:00:00Z",
                timezone = "UTC"
            )
        ),
        deadline = "2025-12-15T18:00:00Z",
        status = status,
        finalDate = null,
        createdAt = "2025-12-01T10:00:00Z",
        updatedAt = "2025-12-01T10:00:00Z"
    )

    // ========================================================================
    // Integration Tests
    // ========================================================================

    @Test
    fun testCompleteWorkflow_DraftToFinalized() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())

        // Setup repository
        val eventRepo = MockEventRepository()

        // Create initial event in DRAFT
        val event = createTestEvent("event-1", EventStatus.DRAFT)
        eventRepo.createEvent(event)

        // Create EventManagementStateMachine
        val loadEventsUseCase = LoadEventsUseCase(eventRepo)
        val createEventUseCase = CreateEventUseCase(eventRepo)

        val eventStateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = eventRepo,
            scope = scope
        )

        val eventStates = mutableListOf<EventManagementContract.State>()
        val eventSideEffects = mutableListOf<EventManagementContract.SideEffect>()

        // Collect state and side effects
        val eventStateJob = launch(testDispatcher) {
            eventStateMachine.state.collect { eventStates.add(it) }
        }
        val eventSideEffectJob = launch(testDispatcher) {
            eventStateMachine.sideEffect.collect { eventSideEffects.add(it) }
        }

        advanceUntilIdle()

        // Step 1: Start Poll (DRAFT → POLLING)
        eventStateMachine.dispatch(EventManagementContract.Intent.StartPoll("event-1"))
        advanceUntilIdle()

        val eventAfterPoll = eventRepo.getEvent("event-1")
        assertNotNull(eventAfterPoll)
        assertEquals(EventStatus.POLLING, eventAfterPoll.status)

        // Step 2: Add votes
        eventRepo.addVote("event-1", "participant-1", "slot-1", Vote.YES)
        eventRepo.addVote("event-1", "participant-2", "slot-1", Vote.YES)
        advanceUntilIdle()

        // Step 3: Confirm Date (POLLING → CONFIRMED)
        eventStateMachine.dispatch(
            EventManagementContract.Intent.ConfirmDate("event-1", "slot-1")
        )
        advanceUntilIdle()

        val eventAfterConfirm = eventRepo.getEvent("event-1")
        assertNotNull(eventAfterConfirm)
        assertEquals(EventStatus.CONFIRMED, eventAfterConfirm.status)
        // finalDate is set to the slot's start time (not the slotId)
        assertEquals("2025-12-20T10:00:00Z", eventAfterConfirm.finalDate)

        // Verify scenariosUnlocked
        val stateAfterConfirm = eventStates.lastOrNull()
        assertNotNull(stateAfterConfirm)
        assertTrue(stateAfterConfirm.scenariosUnlocked)

        // Verify navigation to scenarios
        val navigateToScenarios = eventSideEffects.filterIsInstance<EventManagementContract.SideEffect.NavigateTo>()
            .firstOrNull { it.route.startsWith("scenarios/") }
        assertNotNull(navigateToScenarios)

        // Step 4: Transition to ORGANIZING (CONFIRMED → ORGANIZING)
        eventStateMachine.dispatch(
            EventManagementContract.Intent.TransitionToOrganizing("event-1")
        )
        advanceUntilIdle()

        val eventAfterOrganizing = eventRepo.getEvent("event-1")
        assertNotNull(eventAfterOrganizing)
        assertEquals(EventStatus.ORGANIZING, eventAfterOrganizing.status)

        // Verify meetingsUnlocked
        val stateAfterOrganizing = eventStates.lastOrNull()
        assertNotNull(stateAfterOrganizing)
        assertTrue(stateAfterOrganizing.meetingsUnlocked)

        // Verify navigation to meetings
        val navigateToMeetings = eventSideEffects.filterIsInstance<EventManagementContract.SideEffect.NavigateTo>()
            .firstOrNull { it.route.startsWith("meetings/") }
        assertNotNull(navigateToMeetings)

        // Step 5: Mark as Finalized (ORGANIZING → FINALIZED)
        eventStateMachine.dispatch(
            EventManagementContract.Intent.MarkAsFinalized("event-1")
        )
        advanceUntilIdle()

        val eventAfterFinalized = eventRepo.getEvent("event-1")
        assertNotNull(eventAfterFinalized)
        assertEquals(EventStatus.FINALIZED, eventAfterFinalized.status)

        // Cleanup
        eventStateJob.cancel()
        eventSideEffectJob.cancel()
        scope.cancel()
    }

    @Test
    fun testEventStatusPropagation_ThroughRepository() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())

        val eventRepo = MockEventRepository()

        // Create event in CONFIRMED status
        val event = createTestEvent("event-1", EventStatus.CONFIRMED)
        eventRepo.createEvent(event)

        // EventManagement state machine
        val eventStateMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(eventRepo),
            createEventUseCase = CreateEventUseCase(eventRepo),
            eventRepository = eventRepo,
            scope = scope
        )

        advanceUntilIdle()

        // Verify initial state in repository
        val initialEvent = eventRepo.getEvent("event-1")
        assertNotNull(initialEvent)
        assertEquals(EventStatus.CONFIRMED, initialEvent.status)

        // EventManagement transitions to ORGANIZING
        eventStateMachine.dispatch(
            EventManagementContract.Intent.TransitionToOrganizing("event-1")
        )
        advanceUntilIdle()

        // Verify repository was updated (Event.status propagates through repository)
        val updatedEvent = eventRepo.getEvent("event-1")
        assertNotNull(updatedEvent)
        assertEquals(EventStatus.ORGANIZING, updatedEvent.status)

        // This demonstrates repository-mediated communication:
        // Other state machines can now read the updated status from the repository

        scope.cancel()
    }

    @Test
    fun testCanCreateScenarios_BasedOnEventStatus() = runTest {
        // This test validates business rules for scenario creation across different statuses
        
        // DRAFT: Cannot create scenarios
        val stateDraft = ScenarioManagementContract.State(eventStatus = EventStatus.DRAFT)
        assertFalse(stateDraft.canCreateScenarios(), "Cannot create scenarios in DRAFT")

        // POLLING: Cannot create scenarios
        val statePolling = ScenarioManagementContract.State(eventStatus = EventStatus.POLLING)
        assertFalse(statePolling.canCreateScenarios(), "Cannot create scenarios in POLLING")

        // COMPARING: Can create scenarios
        val stateComparing = ScenarioManagementContract.State(eventStatus = EventStatus.COMPARING)
        assertTrue(stateComparing.canCreateScenarios(), "Can create scenarios in COMPARING")

        // CONFIRMED: Can create scenarios
        val stateConfirmed = ScenarioManagementContract.State(eventStatus = EventStatus.CONFIRMED)
        assertTrue(stateConfirmed.canCreateScenarios(), "Can create scenarios in CONFIRMED")

        // ORGANIZING: Cannot create scenarios
        val stateOrganizing = ScenarioManagementContract.State(eventStatus = EventStatus.ORGANIZING)
        assertFalse(stateOrganizing.canCreateScenarios(), "Cannot create scenarios in ORGANIZING")

        // FINALIZED: Cannot create scenarios
        val stateFinalized = ScenarioManagementContract.State(eventStatus = EventStatus.FINALIZED)
        assertFalse(stateFinalized.canCreateScenarios(), "Cannot create scenarios in FINALIZED")
    }

    @Test
    fun testNavigationSideEffects_AcrossWorkflow() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())

        val eventRepo = MockEventRepository()

        // Create event in DRAFT
        val event = createTestEvent("event-1", EventStatus.DRAFT)
        eventRepo.createEvent(event)

        val eventStateMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(eventRepo),
            createEventUseCase = CreateEventUseCase(eventRepo),
            eventRepository = eventRepo,
            scope = scope
        )

        val sideEffects = mutableListOf<EventManagementContract.SideEffect>()
        val sideEffectJob = launch(testDispatcher) {
            eventStateMachine.sideEffect.collect { sideEffects.add(it) }
        }

        advanceUntilIdle()

        // Start Poll
        eventStateMachine.dispatch(EventManagementContract.Intent.StartPoll("event-1"))
        advanceUntilIdle()

        // Add votes
        eventRepo.addVote("event-1", "participant-1", "slot-1", Vote.YES)
        advanceUntilIdle()

        // Confirm Date
        eventStateMachine.dispatch(
            EventManagementContract.Intent.ConfirmDate("event-1", "slot-1")
        )
        advanceUntilIdle()

        // Verify NavigateTo scenarios side effect
        val navigateToScenarios = sideEffects.filterIsInstance<EventManagementContract.SideEffect.NavigateTo>()
            .firstOrNull { it.route.startsWith("scenarios/") }
        assertNotNull(navigateToScenarios, "Should emit NavigateTo scenarios side effect")
        assertEquals("scenarios/event-1", navigateToScenarios.route)

        // Transition to Organizing
        eventStateMachine.dispatch(
            EventManagementContract.Intent.TransitionToOrganizing("event-1")
        )
        advanceUntilIdle()

        // Verify NavigateTo meetings side effect
        val navigateToMeetings = sideEffects.filterIsInstance<EventManagementContract.SideEffect.NavigateTo>()
            .firstOrNull { it.route.startsWith("meetings/") }
        assertNotNull(navigateToMeetings, "Should emit NavigateTo meetings side effect")
        assertEquals("meetings/event-1", navigateToMeetings.route)

        sideEffectJob.cancel()
        scope.cancel()
    }

    @Test
    fun testRepositoryMediatedCommunication() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())

        val eventRepo = MockEventRepository()

        // Create event
        val event = createTestEvent("event-1", EventStatus.DRAFT)
        eventRepo.createEvent(event)

        // State Machine 1: Updates Event.status
        val eventStateMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(eventRepo),
            createEventUseCase = CreateEventUseCase(eventRepo),
            eventRepository = eventRepo,
            scope = scope
        )

        // Start Poll - updates repository
        eventStateMachine.dispatch(EventManagementContract.Intent.StartPoll("event-1"))
        advanceUntilIdle()

        // Verify repository was updated
        val updatedEvent = eventRepo.getEvent("event-1")
        assertNotNull(updatedEvent)
        assertEquals(EventStatus.POLLING, updatedEvent.status)

        // State Machine 2 (simulated): Reads Event.status from repository
        val currentEventStatus = eventRepo.getEvent("event-1")?.status
        assertNotNull(currentEventStatus)
        assertEquals(EventStatus.POLLING, currentEventStatus)

        // Verify business rules based on repository state
        val scenarioState = ScenarioManagementContract.State(eventStatus = currentEventStatus)
        assertFalse(scenarioState.canCreateScenarios(), "Cannot create scenarios in POLLING status")

        scope.cancel()
    }

    @Test
    fun testWorkflowTransitionValidation() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())

        val eventRepo = MockEventRepository()

        // Create event in CONFIRMED status (skipping DRAFT/POLLING for this test)
        val event = createTestEvent("event-1", EventStatus.CONFIRMED)
        eventRepo.createEvent(event)

        val eventStateMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(eventRepo),
            createEventUseCase = CreateEventUseCase(eventRepo),
            eventRepository = eventRepo,
            scope = scope
        )

        advanceUntilIdle()

        // Try to mark as finalized without being in ORGANIZING (should fail)
        eventStateMachine.dispatch(
            EventManagementContract.Intent.MarkAsFinalized("event-1")
        )
        advanceUntilIdle()

        // Verify event status has NOT changed (guard prevented transition)
        val eventAfterFailedFinalize = eventRepo.getEvent("event-1")
        assertNotNull(eventAfterFailedFinalize)
        assertEquals(EventStatus.CONFIRMED, eventAfterFailedFinalize.status, 
            "Status should remain CONFIRMED (guard prevented invalid transition)")

        // Now transition properly: CONFIRMED → ORGANIZING
        eventStateMachine.dispatch(
            EventManagementContract.Intent.TransitionToOrganizing("event-1")
        )
        advanceUntilIdle()

        val eventAfterOrganizing = eventRepo.getEvent("event-1")
        assertNotNull(eventAfterOrganizing)
        assertEquals(EventStatus.ORGANIZING, eventAfterOrganizing.status)

        // Now finalize should work
        eventStateMachine.dispatch(
            EventManagementContract.Intent.MarkAsFinalized("event-1")
        )
        advanceUntilIdle()

        val eventAfterFinalized = eventRepo.getEvent("event-1")
        assertNotNull(eventAfterFinalized)
        assertEquals(EventStatus.FINALIZED, eventAfterFinalized.status)

        scope.cancel()
    }
}
