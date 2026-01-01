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
 *
 * These tests are written in TDD style (Red-Green-Blue) and will initially fail
 * because the intents are not yet implemented in EventManagementStateMachine.
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

        override fun isDeadlinePassed(deadline: String): Boolean = false

        override fun isOrganizer(eventId: String, userId: String): Boolean = true

        override fun canModifyEvent(eventId: String, userId: String): Boolean = true

        override fun getAllEvents(): List<Event> {
            if (shouldFailLoadAllEvents) {
                throw Exception("Failed to load events")
            }
            return events.values.toList()
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
     *
     * @param id The event ID
     * @param title The event title
     * @param eventType The event type (default: OTHER)
     * @param expectedParticipants Expected participant count (default: null)
     * @param minParticipants Minimum participants (default: null)
     * @param maxParticipants Maximum participants (default: null)
     * @return A test Event in DRAFT status
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
     *
     * @param id The location ID
     * @param eventId The event ID this location belongs to
     * @param name The location name
     * @param locationType The type of location
     * @param address Optional address
     * @param coordinates Optional geographic coordinates
     * @return A test PotentialLocation
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

    /**
     * Setup a state machine with test repositories.
     *
     * @return A tuple of (stateMachine, repository)
     */
    private fun setupStateMachine(): Pair<EventManagementStateMachine, MockEventRepository> {
        val testDispatcher = StandardTestDispatcher()
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

        return Pair(stateMachine, repository)
    }

    // ========================================================================
    // Tests: UpdateDraftEvent Intent
    // ========================================================================

    /**
     * Test UpdateDraftEvent with partial field updates.
     *
     * **Scenario:** UpdateDraftEvent - Success (mise à jour partielle)
     * - **GIVEN** Événement en status DRAFT avec title="Old Title"
     * - **WHEN** Dispatch `UpdateDraftEvent(eventId, eventType=TEAM_BUILDING, expectedParticipants=20)`
     * - **THEN** Event mis à jour avec les nouveaux champs, status reste DRAFT, ShowToast emitted
     */
    @Test
    fun testUpdateDraftEvent_Success() = runTest {
        val (stateMachine, repository) = setupStateMachine()

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
        assertEquals("Old Title", updatedEvent.title) // Other fields unchanged
        assertEquals(EventStatus.DRAFT, updatedEvent.status) // Status remains DRAFT
        assertFalse(state.isLoading)
    }

    /**
     * Test UpdateDraftEvent with validation error (max < min).
     *
     * **Scenario:** UpdateDraftEvent - Validation error (maxParticipants < minParticipants)
     * - **GIVEN** Événement en status DRAFT
     * - **WHEN** Dispatch `UpdateDraftEvent(eventId, minParticipants=30, maxParticipants=20)`
     * - **THEN** Validation error émise, "Le maximum doit être supérieur ou égal au minimum"
     */
    @Test
    fun testUpdateDraftEvent_ValidationError_MaxLessThanMin() = runTest {
        val (stateMachine, repository) = setupStateMachine()

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
        assertTrue(state.error!!.contains("maximum") || state.error!!.contains("maximum"))
        assertFalse(state.isLoading)
    }

    /**
     * Test UpdateDraftEvent fails if event is not in DRAFT status.
     *
     * **Scenario:** UpdateDraftEvent - Not in DRAFT status
     * - **GIVEN** Événement en status POLLING
     * - **WHEN** Dispatch `UpdateDraftEvent(eventId, ...)`
     * - **THEN** Error émise, "Cannot update draft: Event not in DRAFT status"
     */
    @Test
    fun testUpdateDraftEvent_FailsIfNotDraft() = runTest {
        val (stateMachine, repository) = setupStateMachine()

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

    /**
     * Test UpdateDraftEvent with custom event type.
     *
     * **Scenario:** UpdateDraftEvent - Success with CUSTOM event type
     * - **GIVEN** Événement en status DRAFT
     * - **WHEN** Dispatch `UpdateDraftEvent(eventId, eventType=CUSTOM, eventTypeCustom="Fantasy Convention")`
     * - **THEN** Event mis à jour avec les champs, status reste DRAFT
     */
    @Test
    fun testUpdateDraftEvent_CustomEventType() = runTest {
        val (stateMachine, repository) = setupStateMachine()

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

    /**
     * Test AddPotentialLocation successfully adds a location.
     *
     * **Scenario:** AddPotentialLocation - Success
     * - **GIVEN** Événement en status DRAFT, sans PotentialLocations
     * - **WHEN** Dispatch `AddPotentialLocation(eventId, locationId, "Paris", CITY)`
     * - **THEN** PotentialLocation créée, list mise à jour dans state, ShowToast emitted
     */
    @Test
    fun testAddPotentialLocation_Success() = runTest {
        val (stateMachine, repository) = setupStateMachine()

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
        val locations = repository.potentialLocations["evt-1"] ?: emptyList()

        assertEquals(1, locations.size)
        assertEquals("Paris", locations[0].name)
        assertEquals(LocationType.CITY, locations[0].locationType)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    /**
     * Test AddPotentialLocation with multiple locations.
     *
     * **Scenario:** AddPotentialLocation - Multiple locations
     * - **GIVEN** Événement en status DRAFT avec 1 PotentialLocation
     * - **WHEN** Dispatch `AddPotentialLocation(eventId, locationId, "London", CITY)`
     * - **THEN** Deuxième location créée, list contient 2 locations
     */
    @Test
    fun testAddPotentialLocation_Multiple() = runTest {
        val (stateMachine, repository) = setupStateMachine()

        // ARRANGE: Create event with existing location
        val event = createDraftEvent("evt-1")
        repository.events[event.id] = event
        val existingLocation = createTestLocation("loc-1", "evt-1", "Paris", LocationType.CITY)
        repository.potentialLocations[event.id] = mutableListOf(existingLocation)

        // ACT: Add a second location
        stateMachine.dispatch(
            EventManagementContract.Intent.AddPotentialLocation(
                eventId = "evt-1",
                locationId = "loc-2",
                locationName = "London",
                locationType = LocationType.CITY
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify both locations exist
        val locations = repository.potentialLocations["evt-1"] ?: emptyList()

        assertEquals(2, locations.size)
        assertEquals("Paris", locations[0].name)
        assertEquals("London", locations[1].name)
    }

    /**
     * Test AddPotentialLocation fails if event is not in DRAFT status.
     *
     * **Scenario:** AddPotentialLocation - Not in DRAFT status
     * - **GIVEN** Événement en status POLLING
     * - **WHEN** Dispatch `AddPotentialLocation(eventId, ...)`
     * - **THEN** Error émise, "Cannot add location: Event not in DRAFT status"
     */
    @Test
    fun testAddPotentialLocation_FailsIfNotDraft() = runTest {
        val (stateMachine, repository) = setupStateMachine()

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
        val locations = repository.potentialLocations["evt-1"] ?: emptyList()
        assertEquals(0, locations.size)
    }

    /**
     * Test AddPotentialLocation with specific venue and coordinates.
     *
     * **Scenario:** AddPotentialLocation - Specific venue with coordinates
     * - **GIVEN** Événement en status DRAFT
     * - **WHEN** Dispatch `AddPotentialLocation(eventId, ..., locationType=SPECIFIC_VENUE, coordinates=...)`
     * - **THEN** Location créée avec adresse et coordonnées
     */
    @Test
    fun testAddPotentialLocation_WithVenueDetails() = runTest {
        val (stateMachine, repository) = setupStateMachine()

        // ARRANGE: Create event in DRAFT status
        val event = createDraftEvent("evt-1")
        repository.events[event.id] = event

        val coordinates = Coordinates(48.8566, 2.3522) // Eiffel Tower coordinates

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
        val locations = repository.potentialLocations["evt-1"] ?: emptyList()

        assertEquals(1, locations.size)
        assertEquals("Château de Versailles", locations[0].name)
        assertEquals(LocationType.SPECIFIC_VENUE, locations[0].locationType)
        assertEquals("78000 Versailles, France", locations[0].address)
        assertNotNull(locations[0].coordinates)
        assertEquals(48.8566, locations[0].coordinates!!.latitude)
        assertEquals(2.3522, locations[0].coordinates!!.longitude)
    }

    // ========================================================================
    // Tests: RemovePotentialLocation Intent
    // ========================================================================

    /**
     * Test RemovePotentialLocation successfully removes a location.
     *
     * **Scenario:** RemovePotentialLocation - Success
     * - **GIVEN** Événement en status DRAFT avec 2 PotentialLocations
     * - **WHEN** Dispatch `RemovePotentialLocation(eventId, locationId)`
     * - **THEN** PotentialLocation supprimée, list mise à jour, ShowToast emitted
     */
    @Test
    fun testRemovePotentialLocation_Success() = runTest {
        val (stateMachine, repository) = setupStateMachine()

        // ARRANGE: Create event with two locations
        val event = createDraftEvent("evt-1")
        repository.events[event.id] = event
        val location1 = createTestLocation("loc-1", "evt-1", "Paris", LocationType.CITY)
        val location2 = createTestLocation("loc-2", "evt-1", "London", LocationType.CITY)
        repository.potentialLocations[event.id] = mutableListOf(location1, location2)

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
        val locations = repository.potentialLocations["evt-1"] ?: emptyList()

        assertEquals(1, locations.size)
        assertEquals("London", locations[0].name)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    /**
     * Test RemovePotentialLocation removes all occurrences.
     *
     * **Scenario:** RemovePotentialLocation - Remove specific location
     * - **GIVEN** Événement avec 3 locations (loc-1, loc-2, loc-3)
     * - **WHEN** Dispatch `RemovePotentialLocation(eventId, "loc-2")`
     * - **THEN** Seule loc-2 est supprimée, list contient loc-1 et loc-3
     */
    @Test
    fun testRemovePotentialLocation_RemovesCorrectOne() = runTest {
        val (stateMachine, repository) = setupStateMachine()

        // ARRANGE: Create event with three locations
        val event = createDraftEvent("evt-1")
        repository.events[event.id] = event
        val location1 = createTestLocation("loc-1", "evt-1", "Paris")
        val location2 = createTestLocation("loc-2", "evt-1", "London")
        val location3 = createTestLocation("loc-3", "evt-1", "Berlin")
        repository.potentialLocations[event.id] = mutableListOf(location1, location2, location3)

        // ACT: Remove middle location
        stateMachine.dispatch(
            EventManagementContract.Intent.RemovePotentialLocation(
                eventId = "evt-1",
                locationId = "loc-2"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify correct location was removed
        val locations = repository.potentialLocations["evt-1"] ?: emptyList()

        assertEquals(2, locations.size)
        assertEquals("Paris", locations[0].name)
        assertEquals("Berlin", locations[1].name)
    }

    /**
     * Test RemovePotentialLocation fails if event is not in DRAFT status.
     *
     * **Scenario:** RemovePotentialLocation - Not in DRAFT status
     * - **GIVEN** Événement en status POLLING
     * - **WHEN** Dispatch `RemovePotentialLocation(eventId, ...)`
     * - **THEN** Error émise, "Cannot remove location: Event not in DRAFT status"
     */
    @Test
    fun testRemovePotentialLocation_FailsIfNotDraft() = runTest {
        val (stateMachine, repository) = setupStateMachine()

        // ARRANGE: Create event in POLLING status with a location
        val event = createDraftEvent("evt-1").copy(status = EventStatus.POLLING)
        repository.events[event.id] = event
        val location = createTestLocation("loc-1", "evt-1", "Paris")
        repository.potentialLocations[event.id] = mutableListOf(location)

        // ACT: Try to remove location
        stateMachine.dispatch(
            EventManagementContract.Intent.RemovePotentialLocation(
                eventId = "evt-1",
                locationId = "loc-1"
            )
        )
        advanceUntilIdle()

        // ASSERT: Verify error and location still exists
        val state = stateMachine.state.value
        val locations = repository.potentialLocations["evt-1"] ?: emptyList()

        assertNotNull(state.error)
        assertTrue(state.error!!.contains("not in DRAFT status"))
        assertEquals(1, locations.size) // Location should not be removed
    }

    /**
     * Test RemovePotentialLocation from empty list.
     *
     * **Scenario:** RemovePotentialLocation - No locations to remove
     * - **GIVEN** Événement en status DRAFT sans locations
     * - **WHEN** Dispatch `RemovePotentialLocation(eventId, "nonexistent")`
     * - **THEN** State remains unchanged (no error, location list still empty)
     */
    @Test
    fun testRemovePotentialLocation_EmptyList() = runTest {
        val (stateMachine, repository) = setupStateMachine()

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
        val locations = repository.potentialLocations["evt-1"] ?: emptyList()

        assertNull(state.error)
        assertEquals(0, locations.size)
    }

    // ========================================================================
    // Integration Tests: Multiple Intents in Sequence
    // ========================================================================

    /**
     * Test draft event updates and location management workflow.
     *
     * **Scenario:** Workflow - Update event type, add multiple locations, remove one
     * - **GIVEN** Événement DRAFT vierge
     * - **WHEN** Dispatch UpdateDraftEvent, puis AddPotentialLocation (x2), puis RemovePotentialLocation
     * - **THEN** Event correctement mis à jour, locations correctement gérées
     */
    @Test
    fun testDraftWorkflow_UpdateAndManageLocations() = runTest {
        val (stateMachine, repository) = setupStateMachine()

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

        var locations = repository.potentialLocations["evt-1"] ?: emptyList()
        assertEquals(1, locations.size)

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

        locations = repository.potentialLocations["evt-1"] ?: emptyList()
        assertEquals(2, locations.size)

        // Step 4 - Remove first location
        stateMachine.dispatch(
            EventManagementContract.Intent.RemovePotentialLocation(
                eventId = "evt-1",
                locationId = "loc-1"
            )
        )
        advanceUntilIdle()

        locations = repository.potentialLocations["evt-1"] ?: emptyList()
        assertEquals(1, locations.size)
        assertEquals("French Riviera", locations[0].name)

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
}
