package com.guyghost.wakeve.presentation.statemachine

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.models.Coordinates
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
 * Tests for userId permission validation in EventManagementStateMachine and ScenarioManagementStateMachine.
 *
 * This test suite covers the organizer permission validation for privileged operations:
 * - ConfirmDate: Only organizer can confirm dates
 * - TransitionToOrganizing: Only organizer can transition phases
 * - MarkAsFinalized: Only organizer can finalize events
 * - SelectScenarioAsFinal: Only organizer can select final scenarios
 *
 * All tests follow the AAA pattern (Arrange, Act, Assert) and use mocks to isolate
 * the state machine behavior from the repository.
 */
class OrganizerPermissionValidationTest {

    // ========================================================================
    // Mock Repository Implementation
    // ========================================================================

    /**
     * Mock implementation of EventRepositoryInterface for testing permission validation.
     */
    class MockEventRepository : EventRepositoryInterface {
        var events = mutableMapOf<String, Event>()
        var polls = mutableMapOf<String, Poll>()
        var participants = mutableMapOf<String, List<String>>()
        var potentialLocations = mutableMapOf<String, MutableList<PotentialLocation>>()
        var shouldFailLoadAllEvents = false

        override suspend fun createEvent(event: Event): Result<Event> {
            events[event.id] = event
            polls[event.id] = Poll(event.id, event.id, emptyMap())
            potentialLocations[event.id] = mutableListOf()
            return Result.success(event)
        }

        override fun getEvent(id: String): Event? = events[id]

        override fun getPoll(eventId: String): Poll? = polls[eventId]

        override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> {
            val event = events[eventId] ?: return Result.failure(Exception("Event not found"))
            if (!event.participants.contains(participantId)) {
                events[eventId] = event.copy(participants = event.participants + participantId)
                val current = participants[eventId] ?: emptyList()
                participants[eventId] = current + participantId
            }
            return Result.success(true)
        }

        override fun getParticipants(eventId: String): List<String>? = participants[eventId]

        override suspend fun addVote(
            eventId: String,
            participantId: String,
            slotId: String,
            vote: Vote
        ): Result<Boolean> {
            val poll = polls[eventId] ?: return Result.failure(Exception("Poll not found"))
            val updatedVotes = poll.votes.toMutableMap()
            val participantVotes = (updatedVotes[slotId] ?: emptyMap()).toMutableMap()
            participantVotes[participantId] = vote
            updatedVotes[slotId] = participantVotes
            polls[eventId] = poll.copy(votes = updatedVotes)
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

        override fun isOrganizer(eventId: String, userId: String): Boolean {
            val event = events[eventId] ?: return false
            return event.organizerId == userId
        }

        override fun canModifyEvent(eventId: String, userId: String): Boolean {
            val event = events[eventId] ?: return false
            return event.organizerId == userId
        }

        override fun getAllEvents(): List<Event> {
            if (shouldFailLoadAllEvents) {
                throw Exception("Failed to load events")
            }
            return events.values.toList()
        }
    }

    // ========================================================================
    // Mock Scenario Repository Implementation
    // ========================================================================

    /**
     * Mock implementation of ScenarioRepository for testing permission validation.
     */
    class MockScenarioRepository {
        var scenarios = mutableMapOf<String, Scenario>()

        fun getScenarioById(id: String): Scenario? = scenarios[id]

        suspend fun updateScenarioStatus(scenarioId: String, status: ScenarioStatus): Result<Scenario> {
            val scenario = scenarios[scenarioId] ?: return Result.failure(Exception("Scenario not found"))
            val updated = scenario.copy(status = status)
            scenarios[scenarioId] = updated
            return Result.success(updated)
        }
    }

    // ========================================================================
    // Test Helpers
    // ========================================================================

    /**
     * Create a test event in POLLING status with a vote.
     */
    private fun createPollingEvent(
        id: String = "evt-1",
        organizerId: String = "org-1",
        hasVote: Boolean = false
    ): Event {
        val votes = if (hasVote) {
            mapOf("slot-1" to mapOf("participant-1" to Vote.POSSIBLE))
        } else {
            emptyMap()
        }
        return Event(
            id = id,
            title = "Polling Event",
            description = "Test event in polling phase",
            organizerId = organizerId,
            participants = listOf("participant-1"),
            proposedSlots = listOf(
                TimeSlot(
                    id = "slot-1",
                    start = "2025-12-20T10:00:00Z",
                    end = "2025-12-20T12:00:00Z",
                    timezone = "UTC"
                )
            ),
            deadline = "2025-12-15T18:00:00Z",
            status = EventStatus.POLLING,
            createdAt = "2025-12-01T10:00:00Z",
            updatedAt = "2025-12-01T10:00:00Z",
            eventType = EventType.OTHER
        )
    }

    /**
     * Create a test event in CONFIRMED status.
     */
    private fun createConfirmedEvent(
        id: String = "evt-1",
        organizerId: String = "org-1"
    ): Event = Event(
        id = id,
        title = "Confirmed Event",
        description = "Test event in confirmed phase",
        organizerId = organizerId,
        participants = listOf("participant-1"),
        proposedSlots = listOf(
            TimeSlot(
                id = "slot-1",
                start = "2025-12-20T10:00:00Z",
                end = "2025-12-20T12:00:00Z",
                timezone = "UTC"
            )
        ),
        deadline = "2025-12-15T18:00:00Z",
        status = EventStatus.CONFIRMED,
        finalDate = "2025-12-20T10:00:00Z",
        createdAt = "2025-12-01T10:00:00Z",
        updatedAt = "2025-12-01T10:00:00Z",
        eventType = EventType.OTHER
    )

    /**
     * Create a test event in ORGANIZING status.
     */
    private fun createOrganizingEvent(
        id: String = "evt-1",
        organizerId: String = "org-1"
    ): Event = Event(
        id = id,
        title = "Organizing Event",
        description = "Test event in organizing phase",
        organizerId = organizerId,
        participants = listOf("participant-1"),
        proposedSlots = listOf(
            TimeSlot(
                id = "slot-1",
                start = "2025-12-20T10:00:00Z",
                end = "2025-12-20T12:00:00Z",
                timezone = "UTC"
            )
        ),
        deadline = "2025-12-15T18:00:00Z",
        status = EventStatus.ORGANIZING,
        finalDate = "2025-12-20T10:00:00Z",
        createdAt = "2025-12-01T10:00:00Z",
        updatedAt = "2025-12-01T10:00:00Z",
        eventType = EventType.OTHER
    )

    /**
     * Create a test event in COMPARING status.
     */
    private fun createComparingEvent(
        id: String = "evt-1",
        organizerId: String = "org-1"
    ): Event = Event(
        id = id,
        title = "Comparing Event",
        description = "Test event in comparing phase",
        organizerId = organizerId,
        participants = listOf("participant-1"),
        proposedSlots = listOf(
            TimeSlot(
                id = "slot-1",
                start = "2025-12-20T10:00:00Z",
                end = "2025-12-20T12:00:00Z",
                timezone = "UTC"
            )
        ),
        deadline = "2025-12-15T18:00:00Z",
        status = EventStatus.COMPARING,
        finalDate = "2025-12-20T10:00:00Z",
        createdAt = "2025-12-01T10:00:00Z",
        updatedAt = "2025-12-01T10:00:00Z",
        eventType = EventType.OTHER
    )

    /**
     * Create a test scenario.
     */
    private fun createTestScenario(
        id: String = "scenario-1",
        eventId: String = "evt-1",
        status: ScenarioStatus = ScenarioStatus.PROPOSED
    ): Scenario = Scenario(
        id = id,
        eventId = eventId,
        name = "Test Scenario",
        dateOrPeriod = "Dec 20-23",
        location = "Paris",
        duration = 3,
        estimatedParticipants = 8,
        estimatedBudgetPerPerson = 500.0,
        description = "Test scenario description",
        status = status,
        createdAt = "2025-12-01T10:00:00Z",
        updatedAt = "2025-12-01T10:00:00Z"
    )

    // ========================================================================
    // Tests: ConfirmDate Intent
    // ========================================================================

    @Test
    fun `confirmDate should succeed when user is organizer`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockEventRepository()

        val loadEventsUseCase = LoadEventsUseCase(repository)
        val createEventUseCase = CreateEventUseCase(repository)

        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = repository,
            scope = scope
        )

        // ARRANGE: Create event in POLLING status with a vote
        val event = createPollingEvent(organizerId = "org-1", hasVote = true)
        repository.events[event.id] = event
        repository.polls[event.id] = Poll(event.id, event.id, mapOf("slot-1" to mapOf("participant-1" to Vote.POSSIBLE)))

        // ACT: Dispatch ConfirmDate with organizer userId
        stateMachine.dispatch(
            EventManagementContract.Intent.ConfirmDate(
                eventId = "evt-1",
                slotId = "slot-1",
                userId = "org-1"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify event was confirmed
        val updatedEvent = repository.getEvent("evt-1")
        val state = stateMachine.state.value

        assertNotNull(updatedEvent)
        assertEquals(EventStatus.CONFIRMED, updatedEvent.status)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `confirmDate should fail when user is not organizer`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockEventRepository()

        val loadEventsUseCase = LoadEventsUseCase(repository)
        val createEventUseCase = CreateEventUseCase(repository)

        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = repository,
            scope = scope
        )

        // ARRANGE: Create event in POLLING status with a vote
        val event = createPollingEvent(organizerId = "org-1", hasVote = true)
        repository.events[event.id] = event
        repository.polls[event.id] = Poll(event.id, event.id, mapOf("slot-1" to mapOf("participant-1" to Vote.POSSIBLE)))

        // ACT: Dispatch ConfirmDate with non-organizer userId
        stateMachine.dispatch(
            EventManagementContract.Intent.ConfirmDate(
                eventId = "evt-1",
                slotId = "slot-1",
                userId = "not-organizer"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify operation was rejected
        val state = stateMachine.state.value
        val updatedEvent = repository.getEvent("evt-1")

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Only event organizer can confirm dates"))
        assertEquals(EventStatus.POLLING, updatedEvent?.status) // Status unchanged
        assertFalse(state.isLoading)
    }

    @Test
    fun `confirmDate should fail when event not found`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockEventRepository()

        val loadEventsUseCase = LoadEventsUseCase(repository)
        val createEventUseCase = CreateEventUseCase(repository)

        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = repository,
            scope = scope
        )

        // ACT: Dispatch ConfirmDate with non-existent event
        stateMachine.dispatch(
            EventManagementContract.Intent.ConfirmDate(
                eventId = "non-existent",
                slotId = "slot-1",
                userId = "org-1"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify error
        val state = stateMachine.state.value

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Event not found"))
        assertFalse(state.isLoading)
    }

    // ========================================================================
    // Tests: TransitionToOrganizing Intent
    // ========================================================================

    @Test
    fun `transitionToOrganizing should succeed when user is organizer`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockEventRepository()

        val loadEventsUseCase = LoadEventsUseCase(repository)
        val createEventUseCase = CreateEventUseCase(repository)

        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = repository,
            scope = scope
        )

        // ARRANGE: Create event in CONFIRMED status
        val event = createConfirmedEvent(organizerId = "org-1")
        repository.events[event.id] = event

        // ACT: Dispatch TransitionToOrganizing with organizer userId
        stateMachine.dispatch(
            EventManagementContract.Intent.TransitionToOrganizing(
                eventId = "evt-1",
                userId = "org-1"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify event was transitioned
        val updatedEvent = repository.getEvent("evt-1")
        val state = stateMachine.state.value

        assertNotNull(updatedEvent)
        assertEquals(EventStatus.ORGANIZING, updatedEvent.status)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `transitionToOrganizing should fail when user is not organizer`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockEventRepository()

        val loadEventsUseCase = LoadEventsUseCase(repository)
        val createEventUseCase = CreateEventUseCase(repository)

        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = repository,
            scope = scope
        )

        // ARRANGE: Create event in CONFIRMED status
        val event = createConfirmedEvent(organizerId = "org-1")
        repository.events[event.id] = event

        // ACT: Dispatch TransitionToOrganizing with non-organizer userId
        stateMachine.dispatch(
            EventManagementContract.Intent.TransitionToOrganizing(
                eventId = "evt-1",
                userId = "not-organizer"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify operation was rejected
        val state = stateMachine.state.value
        val updatedEvent = repository.getEvent("evt-1")

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Only event organizer can transition to organizing"))
        assertEquals(EventStatus.CONFIRMED, updatedEvent?.status) // Status unchanged
        assertFalse(state.isLoading)
    }

    // ========================================================================
    // Tests: MarkAsFinalized Intent
    // ========================================================================

    @Test
    fun `markAsFinalized should succeed when user is organizer`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockEventRepository()

        val loadEventsUseCase = LoadEventsUseCase(repository)
        val createEventUseCase = CreateEventUseCase(repository)

        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = repository,
            scope = scope
        )

        // ARRANGE: Create event in ORGANIZING status
        val event = createOrganizingEvent(organizerId = "org-1")
        repository.events[event.id] = event

        // ACT: Dispatch MarkAsFinalized with organizer userId
        stateMachine.dispatch(
            EventManagementContract.Intent.MarkAsFinalized(
                eventId = "evt-1",
                userId = "org-1"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify event was finalized
        val updatedEvent = repository.getEvent("evt-1")
        val state = stateMachine.state.value

        assertNotNull(updatedEvent)
        assertEquals(EventStatus.FINALIZED, updatedEvent.status)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `markAsFinalized should fail when user is not organizer`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockEventRepository()

        val loadEventsUseCase = LoadEventsUseCase(repository)
        val createEventUseCase = CreateEventUseCase(repository)

        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = repository,
            scope = scope
        )

        // ARRANGE: Create event in ORGANIZING status
        val event = createOrganizingEvent(organizerId = "org-1")
        repository.events[event.id] = event

        // ACT: Dispatch MarkAsFinalized with non-organizer userId
        stateMachine.dispatch(
            EventManagementContract.Intent.MarkAsFinalized(
                eventId = "evt-1",
                userId = "not-organizer"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify operation was rejected
        val state = stateMachine.state.value
        val updatedEvent = repository.getEvent("evt-1")

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Only event organizer can finalize the event"))
        assertEquals(EventStatus.ORGANIZING, updatedEvent?.status) // Status unchanged
        assertFalse(state.isLoading)
    }

    // ========================================================================
    // Tests: SelectScenarioAsFinal Intent (ScenarioManagementStateMachine)
    // ========================================================================

    @Test
    fun `selectScenarioAsFinal should succeed when user is organizer`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockEventRepository()
        val scenarioRepository = MockScenarioRepository()

        val stateMachine = ScenarioManagementStateMachine(
            loadScenariosUseCase = { _ -> Result.success(emptyList()) },
            createScenarioUseCase = { _, _, _, _, _, _, _ -> Result.success(Unit) },
            voteScenarioUseCase = { _, _, _ -> Result.success(Unit) },
            updateScenarioUseCase = { _ -> Result.success(createTestScenario()) },
            deleteScenarioUseCase = { _ -> Result.success(Unit) },
            eventRepository = repository,
            scenarioRepository = scenarioRepository,
            scope = scope
        )

        // ARRANGE: Create event in COMPARING status and scenario
        val event = createComparingEvent(organizerId = "org-1")
        repository.events[event.id] = event
        scenarioRepository.scenarios["scenario-1"] = createTestScenario(eventId = "evt-1")

        // ACT: Dispatch SelectScenarioAsFinal with organizer userId
        stateMachine.dispatch(
            ScenarioManagementContract.Intent.SelectScenarioAsFinal(
                eventId = "evt-1",
                scenarioId = "scenario-1",
                userId = "org-1"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify scenario was selected and event updated
        val state = stateMachine.state.value

        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `selectScenarioAsFinal should fail when user is not organizer`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockEventRepository()
        val scenarioRepository = MockScenarioRepository()

        val stateMachine = ScenarioManagementStateMachine(
            loadScenariosUseCase = { _ -> Result.success(emptyList()) },
            createScenarioUseCase = { _, _, _, _, _, _, _ -> Result.success(Unit) },
            voteScenarioUseCase = { _, _, _ -> Result.success(Unit) },
            updateScenarioUseCase = { _ -> Result.success(createTestScenario()) },
            deleteScenarioUseCase = { _ -> Result.success(Unit) },
            eventRepository = repository,
            scenarioRepository = scenarioRepository,
            scope = scope
        )

        // ARRANGE: Create event in COMPARING status and scenario
        val event = createComparingEvent(organizerId = "org-1")
        repository.events[event.id] = event
        scenarioRepository.scenarios["scenario-1"] = createTestScenario(eventId = "evt-1")

        // ACT: Dispatch SelectScenarioAsFinal with non-organizer userId
        stateMachine.dispatch(
            ScenarioManagementContract.Intent.SelectScenarioAsFinal(
                eventId = "evt-1",
                scenarioId = "scenario-1",
                userId = "not-organizer"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify operation was rejected
        val state = stateMachine.state.value

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Only event organizer can select final scenario"))
        assertFalse(state.isLoading)
    }

    // ========================================================================
    // Tests: Edge Cases
    // ========================================================================

    @Test
    fun `confirmDate should fail when no votes exist`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockEventRepository()

        val loadEventsUseCase = LoadEventsUseCase(repository)
        val createEventUseCase = CreateEventUseCase(repository)

        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = repository,
            scope = scope
        )

        // ARRANGE: Create event in POLLING status WITHOUT any votes
        val event = createPollingEvent(organizerId = "org-1", hasVote = false)
        repository.events[event.id] = event
        repository.polls[event.id] = Poll(event.id, event.id, emptyMap())

        // ACT: Dispatch ConfirmDate with organizer userId
        stateMachine.dispatch(
            EventManagementContract.Intent.ConfirmDate(
                eventId = "evt-1",
                slotId = "slot-1",
                userId = "org-1"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify error about no votes
        val state = stateMachine.state.value
        val updatedEvent = repository.getEvent("evt-1")

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("No votes"))
        assertEquals(EventStatus.POLLING, updatedEvent?.status) // Status unchanged
        assertFalse(state.isLoading)
    }

    @Test
    fun `transitionToOrganizing should fail when event not in CONFIRMED status`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockEventRepository()

        val loadEventsUseCase = LoadEventsUseCase(repository)
        val createEventUseCase = CreateEventUseCase(repository)

        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = repository,
            scope = scope
        )

        // ARRANGE: Create event in DRAFT status (wrong status for transition)
        val event = createPollingEvent(organizerId = "org-1", hasVote = true)
        repository.events[event.id] = event

        // ACT: Dispatch TransitionToOrganizing with organizer userId
        stateMachine.dispatch(
            EventManagementContract.Intent.TransitionToOrganizing(
                eventId = "evt-1",
                userId = "org-1"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify error about status
        val state = stateMachine.state.value

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("not in CONFIRMED status"))
        assertFalse(state.isLoading)
    }

    @Test
    fun `markAsFinalized should fail when event not in ORGANIZING status`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockEventRepository()

        val loadEventsUseCase = LoadEventsUseCase(repository)
        val createEventUseCase = CreateEventUseCase(repository)

        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = repository,
            scope = scope
        )

        // ARRANGE: Create event in CONFIRMED status (wrong status for finalization)
        val event = createConfirmedEvent(organizerId = "org-1")
        repository.events[event.id] = event

        // ACT: Dispatch MarkAsFinalized with organizer userId
        stateMachine.dispatch(
            EventManagementContract.Intent.MarkAsFinalized(
                eventId = "evt-1",
                userId = "org-1"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify error about status
        val state = stateMachine.state.value

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("not in ORGANIZING status"))
        assertFalse(state.isLoading)
    }
}
