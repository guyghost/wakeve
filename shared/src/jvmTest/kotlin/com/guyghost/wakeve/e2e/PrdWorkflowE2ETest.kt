package com.guyghost.wakeve.e2e

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.notification.NotificationService
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import com.guyghost.wakeve.presentation.statemachine.ScenarioManagementStateMachine
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.*
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * # Complete PRD Workflow E2E Test (E2E-001)
 * 
 * Tests the complete workflow from DRAFT to FINALIZED status:
 * DRAFT → POLLING → CONFIRMED → COMPARING → ORGANIZING → FINALIZED
 * 
 * Validates:
 * - Status transitions are correct
 * - Navigation side effects are emitted
 * - Features unlock at correct phases
 * - Data persists correctly
 * - Repository-mediated communication between state machines
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrdWorkflowE2ETest {

    // ========================================================================
    // Test Infrastructure
    // ========================================================================

    private lateinit var database: WakevDb
    private lateinit var eventRepository: EventRepositoryInterface
    private lateinit var eventStateMachine: EventManagementStateMachine
    private lateinit var scenarioStateMachine: ScenarioManagementStateMachine
    private lateinit var testScope: CoroutineScope

    // Mock notification service for side effects tracking
    private val emittedSideEffects = mutableListOf<EventManagementContract.SideEffect>()
    private val scenarioSideEffects = mutableListOf<ScenarioManagementContract.SideEffect>()

    @BeforeTest
    fun setup() {
        // Initialize in-memory database
        database = createFreshTestDatabase()
        eventRepository = DatabaseEventRepository(database)
        
        // Setup test scope
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        
        // Initialize use cases
        val loadEventsUseCase = LoadEventsUseCase(eventRepository)
        val createEventUseCase = CreateEventUseCase(eventRepository)
        
        // Initialize state machines
        eventStateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = eventRepository,
            scope = testScope
        ).apply {
            // Track side effects
            onSideEffect = { sideEffect ->
                emittedSideEffects.add(sideEffect)
            }
        }
        
        scenarioStateMachine = ScenarioManagementStateMachine(
            eventRepository = eventRepository,
            scope = testScope
        ).apply {
            // Track side effects
            onSideEffect = { sideEffect ->
                scenarioSideEffects.add(sideEffect)
            }
        }
    }

    @AfterTest
    fun cleanup() {
        testScope.cancel()
        database.close()
    }

    // ========================================================================
    // Test Cases
    // ========================================================================

    /**
     * Test: Complete workflow from DRAFT to FINALIZED
     * 
     * GIVEN: Event in DRAFT status with time slots and participants
     * WHEN: Execute complete workflow through all phases
     * THEN: Event transitions correctly and features unlock appropriately
     */
    @Test
    fun `complete workflow from draft to finalized`() = runTest {
        // GIVEN
        emittedSideEffects.clear()
        scenarioSideEffects.clear()
        
        val eventId = "event-workflow-complete"
        val organizerId = "organizer-1"
        val participantIds = listOf("user-1", "user-2", "user-3")
        
        val event = createTestEvent(
            id = eventId,
            title = "Team Building Event",
            description = "Annual team building retreat",
            organizerId = organizerId,
            eventType = EventType.TEAM_BUILDING,
            expectedParticipants = 10,
            minParticipants = 5,
            maxParticipants = 20,
            participants = participantIds,
            proposedSlots = listOf(
                createTestTimeSlot("slot-1", "2025-06-15T09:00:00Z", "2025-06-15T17:00:00Z"),
                createTestTimeSlot("slot-2", "2025-06-16T09:00:00Z", "2025-06-16T17:00:00Z"),
                createTestTimeSlot("slot-3", "2025-06-17T09:00:00Z", "2025-06-17T17:00:00Z")
            ),
            status = EventStatus.DRAFT
        )
        
        // Create event
        val createResult = eventRepository.createEvent(event)
        assertTrue(createResult.isSuccess, "Event creation should succeed")
        
        var currentEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.DRAFT, currentEvent.status)
        
        // WHEN - Step 1: Start Poll (DRAFT → POLLING)
        eventStateMachine.dispatch(EventManagementContract.Intent.StartPoll(eventId))
        advanceUntilIdle()
        
        currentEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.POLLING, currentEvent.status)
        
        // Verify poll is active
        val poll = eventRepository.getPoll(eventId)
        assertNotNull(poll, "Poll should be created")
        
        // WHEN - Step 2: Participants vote
        eventRepository.addVote(eventId, participantIds[0], "slot-1", Vote.YES)
        eventRepository.addVote(eventId, participantIds[1], "slot-1", Vote.YES)
        eventRepository.addVote(eventId, participantIds[2], "slot-2", Vote.YES)
        
        // WHEN - Step 3: Confirm Date (POLLING → CONFIRMED)
        eventStateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(
            eventId = eventId,
            slotId = "slot-1",
            userId = organizerId
        ))
        advanceUntilIdle()
        
        currentEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.CONFIRMED, currentEvent.status)
        assertNotNull(currentEvent.finalDate, "Final date should be set")
        
        // Verify navigation side effect
        assertTrue(
            emittedSideEffects.any { it is EventManagementContract.SideEffect.NavigateTo && it.route.contains("scenarios") },
            "Should emit NavigateTo scenarios"
        )
        
        // WHEN - Step 4: Create Scenarios
        val scenario1 = Scenario(
            id = "scenario-1",
            eventId = eventId,
            name = "Mountain Retreat",
            dateOrPeriod = "2025-06-15 to 2025-06-17",
            location = "Swiss Alps",
            duration = 3,
            estimatedParticipants = 10,
            estimatedBudgetPerPerson = 500.0,
            description = "Mountain hiking and team building",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-01-01T10:00:00Z",
            updatedAt = "2025-01-01T10:00:00Z"
        )
        
        val scenario2 = Scenario(
            id = "scenario-2",
            eventId = eventId,
            name = "Beach Resort",
            dateOrPeriod = "2025-06-15 to 2025-06-17",
            location = "Miami Beach",
            duration = 3,
            estimatedParticipants = 10,
            estimatedBudgetPerPerson = 700.0,
            description = "Beach activities and relaxation",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-01-01T10:00:00Z",
            updatedAt = "2025-01-01T10:00:00Z"
        )
        
        // Save scenarios via repository (simulating scenario creation)
        // In real implementation, this would go through ScenarioStateMachine
        eventRepository.saveScenario(scenario1)
        eventRepository.saveScenario(scenario2)
        
        // WHEN - Step 5: Vote on Scenarios and Select Final (implicitly COMPARING → ORGANIZING)
        scenarioStateMachine.dispatch(ScenarioManagementContract.Intent.SelectScenarioAsFinal(
            eventId = eventId,
            scenarioId = "scenario-1",
            userId = organizerId
        ))
        advanceUntilIdle()
        
        // Verify scenario selection navigation
        assertTrue(
            scenarioSideEffects.any { it is ScenarioManagementContract.SideEffect.NavigateTo && it.route.contains("meetings") },
            "Should emit NavigateTo meetings"
        )
        
        // WHEN - Step 6: Transition to Organizing (CONFIRMED → ORGANIZING)
        eventStateMachine.dispatch(EventManagementContract.Intent.TransitionToOrganizing(
            eventId = eventId,
            userId = organizerId
        ))
        advanceUntilIdle()
        
        currentEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.ORGANIZING, currentEvent.status)
        
        // Verify meetings are unlocked
        assertTrue(currentEvent.meetingsUnlocked, "Meetings should be unlocked")
        
        // Verify navigation side effect
        assertTrue(
            emittedSideEffects.any { it is EventManagementContract.SideEffect.NavigateTo && it.route.contains("meetings") },
            "Should emit NavigateTo meetings"
        )
        
        // WHEN - Step 7: Finalize Event (ORGANIZING → FINALIZED)
        eventStateMachine.dispatch(EventManagementContract.Intent.MarkAsFinalized(
            eventId = eventId,
            userId = organizerId
        ))
        advanceUntilIdle()
        
        // THEN - Verify FINALIZED state
        currentEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.FINALIZED, currentEvent.status)
        
        // Verify event is read-only
        assertFalse(currentEvent.canEdit(), "Event should be read-only when finalized")
        
        // Verify all side effects were emitted
        assertTrue(emittedSideEffects.isNotEmpty(), "Should have emitted side effects")
        
        // Verify data persistence
        val persistedEvent = eventRepository.getEvent(eventId)
        assertNotNull(persistedEvent)
        assertEquals("Team Building Event", persistedEvent.title)
        assertEquals(EventType.TEAM_BUILDING, persistedEvent.eventType)
        assertEquals(10, persistedEvent.expectedParticipants)
        assertEquals(EventStatus.FINALIZED, persistedEvent.status)
    }

    /**
     * Test: Invalid transitions are blocked
     * 
     * GIVEN: Event in various statuses
     * WHEN: Attempt invalid state transitions
     * THEN: Transitions are blocked with appropriate errors
     */
    @Test
    fun `invalid transitions are blocked`() = runTest {
        // GIVEN
        val eventId = "event-invalid-transitions"
        val event = createTestEvent(
            id = eventId,
            status = EventStatus.DRAFT
        )
        eventRepository.createEvent(event)
        
        // WHEN - Attempt to confirm date in DRAFT
        eventStateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(
            eventId = eventId,
            slotId = "slot-1",
            userId = "organizer"
        ))
        advanceUntilIdle()
        
        // THEN - Should remain in DRAFT
        val currentEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.DRAFT, currentEvent.status)
        
        // Check for error side effect
        assertTrue(
            emittedSideEffects.any { it is EventManagementContract.SideEffect.ShowError },
            "Should emit error for invalid transition"
        )
    }

    /**
     * Test: Features unlock at correct phases
     * 
     * GIVEN: Event transitioning through phases
     * WHEN: Check feature availability at each status
     * THEN: Features are correctly locked/unlocked
     */
    @Test
    fun `features unlock at correct phases`() = runTest {
        // GIVEN
        val eventId = "event-feature-unlock"
        val event = createTestEvent(
            id = eventId,
            status = EventStatus.DRAFT
        )
        eventRepository.createEvent(event)
        
        // DRAFT phase - Scenarios and meetings should be locked
        var currentEvent = eventRepository.getEvent(eventId)!!
        assertFalse(currentEvent.scenariosUnlocked, "Scenarios should be locked in DRAFT")
        assertFalse(currentEvent.meetingsUnlocked, "Meetings should be locked in DRAFT")
        
        // Start poll - DRAFT → POLLING
        eventStateMachine.dispatch(EventManagementContract.Intent.StartPoll(eventId))
        advanceUntilIdle()
        
        currentEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.POLLING, currentEvent.status)
        assertFalse(currentEvent.scenariosUnlocked, "Scenarios should still be locked in POLLING")
        assertFalse(currentEvent.meetingsUnlocked, "Meetings should still be locked in POLLING")
        
        // Confirm date - POLLING → CONFIRMED
        eventStateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(
            eventId = eventId,
            slotId = "slot-1",
            userId = "organizer"
        ))
        advanceUntilIdle()
        
        currentEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.CONFIRMED, currentEvent.status)
        assertTrue(currentEvent.scenariosUnlocked, "Scenarios should be unlocked in CONFIRMED")
        assertFalse(currentEvent.meetingsUnlocked, "Meetings should still be locked in CONFIRMED")
        
        // Transition to organizing - CONFIRMED → ORGANIZING
        eventStateMachine.dispatch(EventManagementContract.Intent.TransitionToOrganizing(
            eventId = eventId,
            userId = "organizer"
        ))
        advanceUntilIdle()
        
        currentEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.ORGANIZING, currentEvent.status)
        // Scenarios remain unlocked
        assertTrue(currentEvent.scenariosUnlocked, "Scenarios should remain unlocked in ORGANIZING")
        assertTrue(currentEvent.meetingsUnlocked, "Meetings should be unlocked in ORGANIZING")
    }

    /**
     * Test: Repository-mediated communication between state machines
     * 
     * GIVEN: Two state machines sharing a repository
     * WHEN: EventManagement updates status
     * THEN: ScenarioManagement reads updated status
     */
    @Test
    fun `repository mediated communication works`() = runTest {
        // GIVEN
        val eventId = "event-communication"
        val event = createTestEvent(
            id = eventId,
            status = EventStatus.DRAFT
        )
        eventRepository.createEvent(event)
        
        // WHEN - EventManagement transitions to CONFIRMED
        eventStateMachine.dispatch(EventManagementContract.Intent.StartPoll(eventId))
        advanceUntilIdle()
        
        eventStateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(
            eventId = eventId,
            slotId = "slot-1",
            userId = "organizer"
        ))
        advanceUntilIdle()
        
        // THEN - ScenarioManagement should see the updated status
        val canCreateScenarios = scenarioStateMachine.currentState.canCreateScenarios(
            eventRepository.getEvent(eventId)?.status
        )
        assertTrue(canCreateScenarios, "Should be able to create scenarios when CONFIRMED")
    }
}