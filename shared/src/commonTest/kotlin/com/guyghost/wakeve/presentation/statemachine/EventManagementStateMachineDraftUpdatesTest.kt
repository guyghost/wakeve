package com.guyghost.wakeve.presentation.statemachine

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.models.Coordinates
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract
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
 * Tests for EventManagementStateMachine draft phase update intents.
 *
 * This test suite covers the enhanced DRAFT phase functionality:
 * - UpdateDraftEvent: Incremental updates to event fields (eventType, expectedParticipants, etc.)
 * - AddPotentialLocation: Add brainstormed locations to event
 * - RemovePotentialLocation: Remove locations from event
 *
 * All tests follow the AAA pattern (Arrange, Act, Assert) and use mocks to isolate
 * the state machine behavior from the repository.
 */
class EventManagementStateMachineDraftUpdatesTest {

    // ========================================================================
    // Mock Repository Implementation
    // ========================================================================

    /**
     * Mock implementation of EventRepositoryInterface for testing.
     *
     * Extends the existing MockEventRepository from EventManagementStateMachineTest
     * with support for PotentialLocation operations.
     */
    class MockEventRepository : EventRepositoryInterface {
        var events = mutableMapOf<String, Event>()
        var polls = mutableMapOf<String, Poll>()
        var participants = mutableMapOf<String, List<String>>()
        var potentialLocations = mutableMapOf<String, MutableList<PotentialLocation>>()
        var shouldFailLoadAllEvents = false
        var shouldFailCreateEvent = false
        var shouldFailUpdateEvent = false

        override suspend fun createEvent(event: Event): Result<Event> {
            return if (shouldFailCreateEvent) {
                Result.failure(Exception("Failed to create event"))
            } else {
                events[event.id] = event
                polls[event.id] = Poll(event.id, event.id, emptyMap())
                potentialLocations[event.id] = mutableListOf()
                Result.success(event)
            }
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
            return Result.success(true)
        }

        override suspend fun updateEvent(event: Event): Result<Event> {
            return if (shouldFailUpdateEvent) {
                Result.failure(Exception("Failed to update event"))
            } else {
                events[event.id] = event
                Result.success(event)
            }
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

        override suspend fun saveEvent(event: Event): Result<Event> {
            return if (shouldFailUpdateEvent) {
                Result.failure(Exception("Failed to save event"))
            } else {
                val existingEvent = events[event.id]
                if (existingEvent != null) {
                    events[event.id] = event
                } else {
                    events[event.id] = event
                }
                Result.success(event)
            }
        }

        override fun isDeadlinePassed(deadline: String): Boolean = false

        override fun isOrganizer(eventId: String, userId: String): Boolean = true

        override fun canModifyEvent(eventId: String, userId: String): Boolean = true

        override fun getAllEvents(): List<Event> {
            if (shouldFailLoadAllEvents) {
                throw Exception("Failed to load events")
            }
            return events.values.toList()
        }

        override suspend fun deleteEvent(eventId: String): Result<Unit> {
            val event = events[eventId]
                ?: return Result.failure(IllegalArgumentException("Event not found"))
            
            events.remove(eventId)
            polls.remove(eventId)
            potentialLocations.remove(eventId)
            participants.remove(eventId)
            
            return Result.success(Unit)
        }

        /**
         * Get all potential locations for an event.
         */
        fun getPotentialLocations(eventId: String): List<PotentialLocation> {
            return potentialLocations[eventId] ?: emptyList()
        }

        /**
         * Add a potential location to an event.
         */
        fun addPotentialLocation(location: PotentialLocation) {
            val locations = potentialLocations.getOrPut(location.eventId) { mutableListOf() }
            locations.add(location)
        }

        /**
         * Remove a potential location from an event.
         */
        fun removePotentialLocation(eventId: String, locationId: String) {
            val locations = potentialLocations[eventId] ?: return
            locations.removeAll { it.id == locationId }
        }
    }

    // ========================================================================
    // Test Helpers
    // ========================================================================

    /**
     * Create a test event in DRAFT status.
     */
    private fun createDraftEvent(
        id: String = "evt-draft-1",
        title: String = "Draft Event",
        eventType: EventType = EventType.OTHER,
        expectedParticipants: Int? = null,
        minParticipants: Int? = null,
        maxParticipants: Int? = null
    ): Event = Event(
        id = id,
        title = title,
        description = "Test draft event description",
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
        updatedAt = "2025-12-01T10:00:00Z",
        eventType = eventType,
        expectedParticipants = expectedParticipants,
        minParticipants = minParticipants,
        maxParticipants = maxParticipants
    )

    /**
     * Create a test potential location.
     */
    private fun createTestLocation(
        id: String = "loc-1",
        eventId: String = "evt-draft-1",
        name: String = "Paris",
        locationType: LocationType = LocationType.CITY,
        address: String? = null,
        coordinates: Coordinates? = null
    ): PotentialLocation = PotentialLocation(
        id = id,
        eventId = eventId,
        name = name,
        locationType = locationType,
        address = address,
        coordinates = coordinates,
        createdAt = "2025-12-01T10:00:00Z"
    )

    // ========================================================================
    // Tests: UpdateDraftEvent Intent
    // ========================================================================

    @Test
    fun testUpdateDraftEvent_Success() = runTest {
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

        // ARRANGE: Create event with initial state
        val originalEvent = createDraftEvent(
            id = "evt-1",
            title = "Old Title",
            eventType = EventType.OTHER,
            expectedParticipants = null
        )
        repository.events[originalEvent.id] = originalEvent

        // ACT: Dispatch UpdateDraftEvent intent
        stateMachine.dispatch(
            EventManagementContract.Intent.UpdateDraftEvent(
                eventId = "evt-1",
                eventType = EventType.TEAM_BUILDING,
                expectedParticipants = 20
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify event was updated
        val state = stateMachine.state.value
        val updatedEvent = repository.getEvent("evt-1")

        assertNotNull(updatedEvent)
        assertEquals(EventType.TEAM_BUILDING, updatedEvent.eventType)
        assertEquals(20, updatedEvent.expectedParticipants)
        assertEquals("Old Title", updatedEvent.title)
        assertEquals(EventStatus.DRAFT, updatedEvent.status)
        assertFalse(state.isLoading)
    }

    @Test
    fun testUpdateDraftEvent_ValidationError_MaxLessThanMin() = runTest {
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

        // ARRANGE: Create event in DRAFT status
        val event = createDraftEvent("evt-1")
        repository.events[event.id] = event

        // ACT: Dispatch UpdateDraftEvent with invalid participant counts
        stateMachine.dispatch(
            EventManagementContract.Intent.UpdateDraftEvent(
                eventId = "evt-1",
                minParticipants = 30,
                maxParticipants = 20
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify validation error
        val state = stateMachine.state.value

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Max") || state.error!!.contains("max"))
        assertFalse(state.isLoading)
    }

    @Test
    fun testUpdateDraftEvent_FailsIfNotDraft() = runTest {
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

        // ARRANGE: Create event in POLLING status
        val event = createDraftEvent("evt-1").copy(status = EventStatus.POLLING)
        repository.events[event.id] = event

        // ACT: Dispatch UpdateDraftEvent
        stateMachine.dispatch(
            EventManagementContract.Intent.UpdateDraftEvent(
                eventId = "evt-1",
                eventType = EventType.TEAM_BUILDING,
                expectedParticipants = 20
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify error
        val state = stateMachine.state.value

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("not in DRAFT status"))
        assertFalse(state.isLoading)
    }

    @Test
    fun testUpdateDraftEvent_CustomEventType() = runTest {
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

        // ARRANGE: Create event in DRAFT status
        val event = createDraftEvent("evt-1")
        repository.events[event.id] = event

        // ACT: Dispatch UpdateDraftEvent with custom type
        stateMachine.dispatch(
            EventManagementContract.Intent.UpdateDraftEvent(
                eventId = "evt-1",
                eventType = EventType.CUSTOM,
                eventTypeCustom = "Fantasy Convention"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify custom event type was set
        val updatedEvent = repository.getEvent("evt-1")

        assertNotNull(updatedEvent)
        assertEquals(EventType.CUSTOM, updatedEvent.eventType)
        assertEquals("Fantasy Convention", updatedEvent.eventTypeCustom)
        assertEquals(EventStatus.DRAFT, updatedEvent.status)
    }

    // ========================================================================
    // Tests: AddPotentialLocation Intent
    // ========================================================================

    @Test
    fun testAddPotentialLocation_Success() = runTest {
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

        // ARRANGE: Create event in DRAFT status
        val event = createDraftEvent("evt-1")
        repository.events[event.id] = event
        repository.potentialLocations[event.id] = mutableListOf()

        // ACT: Dispatch AddPotentialLocation
        stateMachine.dispatch(
            EventManagementContract.Intent.AddPotentialLocation(
                eventId = "evt-1",
                locationId = "loc-1",
                locationName = "Paris",
                locationType = LocationType.CITY
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify location was added
        val state = stateMachine.state.value

        assertEquals(1, state.potentialLocations.size)
        assertEquals("Paris", state.potentialLocations[0].name)
        assertEquals(LocationType.CITY, state.potentialLocations[0].locationType)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun testAddPotentialLocation_Multiple() = runTest {
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

        // ARRANGE: Create event in DRAFT status
        val event = createDraftEvent("evt-1")
        repository.events[event.id] = event

        // ACT: Add first location through state machine
        stateMachine.dispatch(
            EventManagementContract.Intent.AddPotentialLocation(
                eventId = "evt-1",
                locationId = "loc-1",
                locationName = "Paris",
                locationType = LocationType.CITY
            )
        )
        advanceUntilIdle()

        // ACT: Add second location through state machine
        stateMachine.dispatch(
            EventManagementContract.Intent.AddPotentialLocation(
                eventId = "evt-1",
                locationId = "loc-2",
                locationName = "London",
                locationType = LocationType.CITY
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify both locations exist in state
        val state = stateMachine.state.value

        assertEquals(2, state.potentialLocations.size)
        assertEquals("Paris", state.potentialLocations[0].name)
        assertEquals("London", state.potentialLocations[1].name)
    }

    @Test
    fun testAddPotentialLocation_FailsIfNotDraft() = runTest {
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

        // ARRANGE: Create event in POLLING status
        val event = createDraftEvent("evt-1").copy(status = EventStatus.POLLING)
        repository.events[event.id] = event

        // ACT: Dispatch AddPotentialLocation
        stateMachine.dispatch(
            EventManagementContract.Intent.AddPotentialLocation(
                eventId = "evt-1",
                locationId = "loc-1",
                locationName = "Paris",
                locationType = LocationType.CITY
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify error
        val state = stateMachine.state.value

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("not in DRAFT status"))
        assertEquals(0, state.potentialLocations.size)
    }

    @Test
    fun testAddPotentialLocation_WithVenueDetails() = runTest {
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

        // ARRANGE: Create event in DRAFT status
        val event = createDraftEvent("evt-1")
        repository.events[event.id] = event

        val coordinates = Coordinates(48.8566, 2.3522)

        // ACT: Add a venue location with details
        stateMachine.dispatch(
            EventManagementContract.Intent.AddPotentialLocation(
                eventId = "evt-1",
                locationId = "loc-1",
                locationName = "Château de Versailles",
                locationType = LocationType.SPECIFIC_VENUE,
                address = "78000 Versailles, France",
                coordinates = coordinates
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify location details
        val state = stateMachine.state.value

        assertEquals(1, state.potentialLocations.size)
        assertEquals("Château de Versailles", state.potentialLocations[0].name)
        assertEquals(LocationType.SPECIFIC_VENUE, state.potentialLocations[0].locationType)
        assertEquals("78000 Versailles, France", state.potentialLocations[0].address)
        assertNotNull(state.potentialLocations[0].coordinates)
        assertEquals(48.8566, state.potentialLocations[0].coordinates!!.latitude)
        assertEquals(2.3522, state.potentialLocations[0].coordinates!!.longitude)
    }

    // ========================================================================
    // Tests: RemovePotentialLocation Intent
    // ========================================================================

    @Test
    fun testRemovePotentialLocation_Success() = runTest {
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

        // ARRANGE: Create event and add two locations through state machine
        val event = createDraftEvent("evt-1")
        repository.events[event.id] = event

        stateMachine.dispatch(
            EventManagementContract.Intent.AddPotentialLocation(
                eventId = "evt-1",
                locationId = "loc-1",
                locationName = "Paris",
                locationType = LocationType.CITY
            )
        )
        advanceUntilIdle()

        stateMachine.dispatch(
            EventManagementContract.Intent.AddPotentialLocation(
                eventId = "evt-1",
                locationId = "loc-2",
                locationName = "London",
                locationType = LocationType.CITY
            )
        )
        advanceUntilIdle()

        // ACT: Remove first location
        stateMachine.dispatch(
            EventManagementContract.Intent.RemovePotentialLocation(
                eventId = "evt-1",
                locationId = "loc-1"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify location was removed
        val state = stateMachine.state.value

        assertEquals(1, state.potentialLocations.size)
        assertEquals("London", state.potentialLocations[0].name)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun testRemovePotentialLocation_RemovesCorrectOne() = runTest {
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

        // ARRANGE: Create event and add three locations through state machine
        val event = createDraftEvent("evt-1")
        repository.events[event.id] = event

        stateMachine.dispatch(
            EventManagementContract.Intent.AddPotentialLocation(
                eventId = "evt-1",
                locationId = "loc-1",
                locationName = "Paris",
                locationType = LocationType.CITY
            )
        )
        advanceUntilIdle()

        stateMachine.dispatch(
            EventManagementContract.Intent.AddPotentialLocation(
                eventId = "evt-1",
                locationId = "loc-2",
                locationName = "London",
                locationType = LocationType.CITY
            )
        )
        advanceUntilIdle()

        stateMachine.dispatch(
            EventManagementContract.Intent.AddPotentialLocation(
                eventId = "evt-1",
                locationId = "loc-3",
                locationName = "Berlin",
                locationType = LocationType.CITY
            )
        )
        advanceUntilIdle()

        // ACT: Remove middle location
        stateMachine.dispatch(
            EventManagementContract.Intent.RemovePotentialLocation(
                eventId = "evt-1",
                locationId = "loc-2"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify correct location was removed
        val state = stateMachine.state.value

        assertEquals(2, state.potentialLocations.size)
        assertEquals("Paris", state.potentialLocations[0].name)
        assertEquals("Berlin", state.potentialLocations[1].name)
    }

    @Test
    fun testRemovePotentialLocation_FailsIfNotDraft() = runTest {
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

        // ARRANGE: Create event in DRAFT status first
        val event = createDraftEvent("evt-1")
        repository.events[event.id] = event

        // Add a location while in DRAFT status
        stateMachine.dispatch(
            EventManagementContract.Intent.AddPotentialLocation(
                eventId = "evt-1",
                locationId = "loc-1",
                locationName = "Paris",
                locationType = LocationType.CITY
            )
        )
        advanceUntilIdle()

        // Verify location was added
        var state = stateMachine.state.value
        assertEquals(1, state.potentialLocations.size)

        // Now change event to POLLING status (simulating state transition)
        repository.events[event.id] = event.copy(status = EventStatus.POLLING)

        // ACT: Try to remove location (should fail because not DRAFT)
        stateMachine.dispatch(
            EventManagementContract.Intent.RemovePotentialLocation(
                eventId = "evt-1",
                locationId = "loc-1"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify error and location still exists
        state = stateMachine.state.value

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("not in DRAFT status"))
        assertEquals(1, state.potentialLocations.size) // Location should not be removed
    }

    @Test
    fun testRemovePotentialLocation_EmptyList() = runTest {
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

        // ARRANGE: Create event with no locations
        val event = createDraftEvent("evt-1")
        repository.events[event.id] = event
        repository.potentialLocations[event.id] = mutableListOf()

        // ACT: Try to remove non-existent location
        stateMachine.dispatch(
            EventManagementContract.Intent.RemovePotentialLocation(
                eventId = "evt-1",
                locationId = "nonexistent"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify no error and list is still empty
        val state = stateMachine.state.value

        assertNull(state.error)
        assertEquals(0, state.potentialLocations.size)
    }

    // ========================================================================
    // Integration Tests: Multiple Intents in Sequence
    // ========================================================================

    @Test
    fun testDraftWorkflow_UpdateAndManageLocations() = runTest {
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

        // ARRANGE: Create initial draft event
        val event = createDraftEvent("evt-1", "Team Retreat", EventType.OTHER)
        repository.events[event.id] = event
        repository.potentialLocations[event.id] = mutableListOf()

        // ACT & ASSERT: Step 1 - Update event type and participant count
        stateMachine.dispatch(
            EventManagementContract.Intent.UpdateDraftEvent(
                eventId = "evt-1",
                eventType = EventType.TEAM_BUILDING,
                expectedParticipants = 25
            )
        )
        advanceUntilIdle()

        var state = stateMachine.state.value
        var updatedEvent = repository.getEvent("evt-1")
        assertEquals(EventType.TEAM_BUILDING, updatedEvent?.eventType)
        assertEquals(25, updatedEvent?.expectedParticipants)

        // Step 2 - Add first location
        stateMachine.dispatch(
            EventManagementContract.Intent.AddPotentialLocation(
                eventId = "evt-1",
                locationId = "loc-1",
                locationName = "Swiss Alps",
                locationType = LocationType.REGION
            )
        )
        advanceUntilIdle()

        state = stateMachine.state.value
        assertEquals(1, state.potentialLocations.size)

        // Step 3 - Add second location
        stateMachine.dispatch(
            EventManagementContract.Intent.AddPotentialLocation(
                eventId = "evt-1",
                locationId = "loc-2",
                locationName = "French Riviera",
                locationType = LocationType.REGION
            )
        )
        advanceUntilIdle()

        state = stateMachine.state.value
        assertEquals(2, state.potentialLocations.size)

        // Step 4 - Remove first location
        stateMachine.dispatch(
            EventManagementContract.Intent.RemovePotentialLocation(
                eventId = "evt-1",
                locationId = "loc-1"
            )
        )
        advanceUntilIdle()

        state = stateMachine.state.value
        assertEquals(1, state.potentialLocations.size)
        assertEquals("French Riviera", state.potentialLocations[0].name)

        // Step 5 - Update with min/max participants
        stateMachine.dispatch(
            EventManagementContract.Intent.UpdateDraftEvent(
                eventId = "evt-1",
                minParticipants = 15,
                maxParticipants = 30
            )
        )
        advanceUntilIdle()

        state = stateMachine.state.value
        updatedEvent = repository.getEvent("evt-1")
        assertEquals(15, updatedEvent?.minParticipants)
        assertEquals(30, updatedEvent?.maxParticipants)
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    // ========================================================================
    // Tests: DeleteEvent Intent
    // ========================================================================

    @Test
    fun testDeleteEvent_Success() = runTest {
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

        // ARRANGE: Create event in DRAFT status
        val event = createDraftEvent("evt-1", "Team Retreat")
        repository.events[event.id] = event

        // Also add to state machine's events list
        stateMachine.dispatch(EventManagementContract.Intent.LoadEvents)
        advanceUntilIdle()

        // ACT: Dispatch DeleteEvent intent
        stateMachine.dispatch(
            EventManagementContract.Intent.DeleteEvent(
                eventId = "evt-1",
                userId = "org-1" // organizer
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify event was deleted
        val state = stateMachine.state.value

        assertNull(repository.getEvent("evt-1"))
        assertFalse(state.events.any { it.id == "evt-1" })
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun testDeleteEvent_FailsIfNotOrganizer() = runTest {
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

        // ARRANGE: Create event with specific organizer
        val event = createDraftEvent("evt-1", "Team Retreat")
        repository.events[event.id] = event

        // ACT: Dispatch DeleteEvent with wrong userId
        stateMachine.dispatch(
            EventManagementContract.Intent.DeleteEvent(
                eventId = "evt-1",
                userId = "wrong-user" // not the organizer
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify error and event still exists
        val state = stateMachine.state.value

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("organizer"))
        assertNotNull(repository.getEvent("evt-1"))
    }

    @Test
    fun testDeleteEvent_FailsIfFinalized() = runTest {
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

        // ARRANGE: Create event in FINALIZED status
        val event = createDraftEvent("evt-1", "Team Retreat").copy(status = EventStatus.FINALIZED)
        repository.events[event.id] = event

        // ACT: Dispatch DeleteEvent
        stateMachine.dispatch(
            EventManagementContract.Intent.DeleteEvent(
                eventId = "evt-1",
                userId = "org-1" // organizer
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify error and event still exists
        val state = stateMachine.state.value

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("finalized"))
        assertNotNull(repository.getEvent("evt-1"))
    }

    @Test
    fun testDeleteEvent_FailsIfEventNotFound() = runTest {
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

        // ACT: Dispatch DeleteEvent for non-existent event
        stateMachine.dispatch(
            EventManagementContract.Intent.DeleteEvent(
                eventId = "non-existent",
                userId = "org-1"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify error
        val state = stateMachine.state.value

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("not found"))
    }

    @Test
    fun testDeleteEvent_ClearsSelectedEvent() = runTest {
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

        // ARRANGE: Create event and select it
        val event = createDraftEvent("evt-1", "Team Retreat")
        repository.events[event.id] = event

        // Load events to populate state
        stateMachine.dispatch(EventManagementContract.Intent.LoadEvents)
        advanceUntilIdle()

        // Select the event
        stateMachine.dispatch(EventManagementContract.Intent.SelectEvent("evt-1"))
        advanceUntilIdle()

        // Verify event is selected
        var state = stateMachine.state.value
        assertNotNull(state.selectedEvent)
        assertEquals("evt-1", state.selectedEvent?.id)

        // ACT: Delete the selected event
        stateMachine.dispatch(
            EventManagementContract.Intent.DeleteEvent(
                eventId = "evt-1",
                userId = "org-1"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify selectedEvent is cleared
        state = stateMachine.state.value

        assertNull(state.selectedEvent)
        assertFalse(state.events.any { it.id == "evt-1" })
    }

    @Test
    fun testDeleteEvent_AllowedStatuses() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())

        // Test that deletion is allowed for DRAFT, POLLING, CONFIRMED, COMPARING, ORGANIZING
        val allowedStatuses = listOf(
            EventStatus.DRAFT,
            EventStatus.POLLING,
            EventStatus.CONFIRMED,
            EventStatus.COMPARING,
            EventStatus.ORGANIZING
        )

        for (status in allowedStatuses) {
            val repository = MockEventRepository()
            val loadEventsUseCase = LoadEventsUseCase(repository)
            val createEventUseCase = CreateEventUseCase(repository)

            val stateMachine = EventManagementStateMachine(
                loadEventsUseCase = loadEventsUseCase,
                createEventUseCase = createEventUseCase,
                eventRepository = repository,
                scope = scope
            )

            // ARRANGE: Create event with specific status
            val event = createDraftEvent("evt-${status.name}").copy(status = status)
            repository.events[event.id] = event

            // ACT: Dispatch DeleteEvent
            stateMachine.dispatch(
                EventManagementContract.Intent.DeleteEvent(
                    eventId = event.id,
                    userId = "org-1"
                )
            )
            advanceUntilIdle()

            // ASSERT: Verify event was deleted
            val state = stateMachine.state.value

            assertNull(repository.getEvent(event.id), "Event with status $status should be deleted")
            assertNull(state.error, "No error should occur for status $status")
        }
    }
}
