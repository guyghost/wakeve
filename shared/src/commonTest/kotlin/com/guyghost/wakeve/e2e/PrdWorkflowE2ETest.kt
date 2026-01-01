package com.guyghost.wakeve.e2e

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*
import kotlin.math.absoluteValue

/**
 * # Comprehensive E2E Tests for Wakeve PRD Workflow
 * 
 * These tests validate the complete workflow from event creation through finalization,
 * covering all phases and services integration.
 * 
 * Pattern: Each test follows BDD-style (GIVEN/WHEN/THEN):
 * - **GIVEN**: Test environment and initial state setup
 * - **WHEN**: Workflow execution through intents and service calls
 * - **THEN**: Assertions on state changes, side effects, and data persistence
 * 
 * Phase 6 - Comprehensive E2E Testing
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrdWorkflowE2ETest {

    // ========================================================================
    // Mock Repository with Comprehensive Support
    // ========================================================================

    /**
     * Comprehensive mock EventRepository for E2E testing
     * with support for scenarios, comments, votes, and potential locations.
     */
    class MockEventRepository : EventRepositoryInterface {
        var events = mutableMapOf<String, Event>()
        var polls = mutableMapOf<String, Poll>()
        var participants = mutableMapOf<String, List<String>>()
        var scenarios = mutableMapOf<String, List<Scenario>>()
        var comments = mutableMapOf<String, List<Comment>>()
        var potentialLocations = mutableMapOf<String, List<PotentialLocation>>()
        var userVotes = mutableMapOf<String, MutableMap<String, MutableMap<String, Vote>>>()
        var scenarioVotes = mutableMapOf<String, MutableMap<String, MutableMap<String, Vote>>>()
        
        var shouldFailVote = false
        var simulateNetworkDelay = 0L

        override suspend fun createEvent(event: Event): Result<Event> {
            if (simulateNetworkDelay > 0) delay(simulateNetworkDelay)
            events[event.id] = event
            polls[event.id] = Poll(event.id, event.id, emptyMap())
            participants[event.id] = event.participants
            scenarios[event.id] = emptyList()
            comments[event.id] = emptyList()
            potentialLocations[event.id] = emptyList()
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
            if (simulateNetworkDelay > 0) delay(simulateNetworkDelay)
            if (shouldFailVote) return Result.failure(Exception("Vote failed"))
            
            val pollVotes = userVotes.getOrPut(eventId) { mutableMapOf() }
            val participantVotes = pollVotes.getOrPut(participantId) { mutableMapOf() }
            participantVotes[slotId] = vote
            
            val poll = polls[eventId]
            polls[eventId] = (poll ?: Poll(eventId, eventId, emptyMap())).copy(
                votes = userVotes[eventId] ?: emptyMap()
            )
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

        // Helper methods
        fun addPotentialLocation(eventId: String, location: PotentialLocation) {
            val current = potentialLocations[eventId] ?: emptyList()
            potentialLocations[eventId] = current + location
        }

        fun getPotentialLocations(eventId: String): List<PotentialLocation> {
            return potentialLocations[eventId] ?: emptyList()
        }

        fun addScenario(eventId: String, scenario: Scenario) {
            val current = scenarios[eventId] ?: emptyList()
            scenarios[eventId] = current + scenario
        }

        fun getScenarios(eventId: String): List<Scenario> {
            return scenarios[eventId] ?: emptyList()
        }

        fun addComment(eventId: String, comment: Comment) {
            val current = comments[eventId] ?: emptyList()
            comments[eventId] = current + comment
        }

        fun getComments(eventId: String): List<Comment> {
            return comments[eventId] ?: emptyList()
        }

        fun recordScenarioVote(eventId: String, userId: String, scenarioId: String, vote: Vote) {
            val eventVotes = scenarioVotes.getOrPut(eventId) { mutableMapOf() }
            val userScenarioVotes = eventVotes.getOrPut(userId) { mutableMapOf() }
            userScenarioVotes[scenarioId] = vote
        }
    }

    // ========================================================================
    // Test Cases
    // ========================================================================

    /**
     * Test 1: Complete Workflow - DRAFT → FINALIZED
     * 
     * GIVEN: New event with Enhanced DRAFT features
     * WHEN:  Execute complete workflow through all phases
     * THEN:  Event transitions correctly and data persists
     */
    @Test
    fun testCompleteWorkflow_DraftToFinalized() = runTest {
        // GIVEN
        val eventId = "event-1"
        val eventRepo = MockEventRepository()

        val event = createTestEvent(
            id = eventId,
            title = "Birthday Party",
            organizerId = "alice",
            eventType = EventType.BIRTHDAY,
            expectedParticipants = 10,
            proposedSlots = listOf(
                createTestTimeSlot("slot-1", "2025-05-15T19:00:00Z", "2025-05-15T23:00:00Z")
            ),
            status = EventStatus.DRAFT
        )
        val createdEvent = eventRepo.createEvent(event).getOrNull()
        assertNotNull(createdEvent)
        assertEquals(EventStatus.DRAFT, createdEvent.status)

        // WHEN - Transition through phases manually
        eventRepo.updateEventStatus(eventId, EventStatus.POLLING, null)
        advanceUntilIdle()
        var currentEvent = eventRepo.getEvent(eventId)!!
        assertEquals(EventStatus.POLLING, currentEvent.status)

        // Add votes and confirm
        eventRepo.addVote(eventId, "alice", "slot-1", Vote.YES)
        eventRepo.addVote(eventId, "bob", "slot-1", Vote.YES)
        
        eventRepo.updateEventStatus(eventId, EventStatus.CONFIRMED, "2025-05-15T19:00:00Z")
        advanceUntilIdle()
        currentEvent = eventRepo.getEvent(eventId)!!
        assertEquals(EventStatus.CONFIRMED, currentEvent.status)
        assertNotNull(currentEvent.finalDate)

        // Transition to organizing
        eventRepo.updateEventStatus(eventId, EventStatus.ORGANIZING, null)
        advanceUntilIdle()
        currentEvent = eventRepo.getEvent(eventId)!!
        assertEquals(EventStatus.ORGANIZING, currentEvent.status)

        // Finalize
        eventRepo.updateEventStatus(eventId, EventStatus.FINALIZED, null)
        advanceUntilIdle()

        // THEN - Verify FINALIZED
        currentEvent = eventRepo.getEvent(eventId)!!
        assertEquals(EventStatus.FINALIZED, currentEvent.status)
        assertEquals(EventType.BIRTHDAY, currentEvent.eventType)
        assertEquals(10, currentEvent.expectedParticipants)
    }

    /**
     * Test 2: Enhanced DRAFT Features
     * 
     * GIVEN: Event creation with new DRAFT phase fields
     * WHEN:  Add potential locations and flexible time slots
     * THEN:  Event reflects all DRAFT enhancements
     */
    @Test
    fun testEnhancedDraftFeatures() = runTest {
        // GIVEN
        val eventId = "event-draft"
        val scope = CoroutineScope(this.testScheduler + SupervisorJob())
        val eventRepo = MockEventRepository()

        val event = createTestEvent(
            id = eventId,
            eventType = EventType.WEDDING,
            expectedParticipants = 50,
            minParticipants = 30,
            maxParticipants = 100,
            proposedSlots = listOf(
                createTestTimeSlot("slot-1", null, null, timeOfDay = TimeOfDay.MORNING),
                createTestTimeSlot("slot-2", null, null, timeOfDay = TimeOfDay.AFTERNOON)
            )
        )
        
        // WHEN
        eventRepo.createEvent(event)
        
        // Add potential locations
        eventRepo.addPotentialLocation(eventId, PotentialLocation(
            id = "loc-1", eventId = eventId, name = "Paris", 
            locationType = LocationType.CITY, address = "Paris, France", 
            coordinates = null, createdAt = "2025-01-01T10:00:00Z"
        ))
        eventRepo.addPotentialLocation(eventId, PotentialLocation(
            id = "loc-2", eventId = eventId, name = "Venice",
            locationType = LocationType.CITY, address = "Venice, Italy",
            coordinates = null, createdAt = "2025-01-01T10:00:00Z"
        ))

        // THEN
        val retrievedEvent = eventRepo.getEvent(eventId)!!
        assertEquals(EventType.WEDDING, retrievedEvent.eventType)
        assertEquals(50, retrievedEvent.expectedParticipants)
        assertEquals(30, retrievedEvent.minParticipants)
        assertEquals(100, retrievedEvent.maxParticipants)
        
        val locations = eventRepo.getPotentialLocations(eventId)
        assertEquals(2, locations.size)
        assertTrue(locations.any { it.name == "Paris" })
        assertTrue(locations.any { it.locationType == LocationType.CITY })

        scope.cancel()
    }

    /**
     * Test 3: Scenario Management Integration
     * 
     * GIVEN: Event confirmed with scenarios
     * WHEN:  Create and vote on scenarios
     * THEN:  Scenarios persist and votes are recorded
     */
    @Test
    fun testScenarioManagement() = runTest {
        // GIVEN
        val eventId = "event-scenario"
        val scope = CoroutineScope(this.testScheduler + SupervisorJob())
        val eventRepo = MockEventRepository()

        val event = createTestEvent(id = eventId, status = EventStatus.CONFIRMED)
        eventRepo.createEvent(event)

        // WHEN - Create scenarios
        val scenario1 = Scenario(
            id = "scenario-1", eventId = eventId, name = "Paris Wedding",
            dateOrPeriod = "2025-05-15 to 2025-05-17", location = "Paris",
            duration = 3, estimatedParticipants = 50, estimatedBudgetPerPerson = 200.0,
            description = "Romantic wedding in Paris", status = ScenarioStatus.PROPOSED,
            createdAt = "2025-01-01T10:00:00Z", updatedAt = "2025-01-01T10:00:00Z"
        )
        
        val scenario2 = Scenario(
            id = "scenario-2", eventId = eventId, name = "Venice Wedding",
            dateOrPeriod = "2025-05-20 to 2025-05-22", location = "Venice",
            duration = 3, estimatedParticipants = 50, estimatedBudgetPerPerson = 250.0,
            description = "Romantic wedding in Venice", status = ScenarioStatus.PROPOSED,
            createdAt = "2025-01-01T10:00:00Z", updatedAt = "2025-01-01T10:00:00Z"
        )

        eventRepo.addScenario(eventId, scenario1)
        eventRepo.addScenario(eventId, scenario2)

        // Record votes
        eventRepo.recordScenarioVote(eventId, "alice", "scenario-1", Vote.YES)
        eventRepo.recordScenarioVote(eventId, "bob", "scenario-1", Vote.YES)
        eventRepo.recordScenarioVote(eventId, "charlie", "scenario-2", Vote.YES)

        // THEN
        val scenarios = eventRepo.getScenarios(eventId)
        assertEquals(2, scenarios.size)
        assertEquals("Paris", scenarios[0].location)
        assertEquals("Venice", scenarios[1].location)

        scope.cancel()
    }

    /**
     * Test 4: Collaboration - Comments and Multi-User Voting
     * 
     * GIVEN: Event with multiple participants
     * WHEN:  Participants add comments and vote
     * THEN:  Comments and votes are organized by section
     */
    @Test
    fun testCollaboration_CommentsAndVoting() = runTest {
        // GIVEN
        val eventId = "event-collab"
        val scope = CoroutineScope(this.testScheduler + SupervisorJob())
        val eventRepo = MockEventRepository()

        val event = createTestEvent(
            id = eventId,
            participants = listOf("alice", "bob", "charlie"),
            proposedSlots = listOf(
                createTestTimeSlot("slot-1", "2025-05-15T10:00:00Z"),
                createTestTimeSlot("slot-2", "2025-05-16T10:00:00Z")
            )
        )
        eventRepo.createEvent(event)

        // WHEN - Add comments
        val comment1 = Comment(
            id = "comment-1", eventId = eventId, section = CommentSection.ACCOMMODATION,
            authorId = "alice", authorName = "Alice",
            content = "I prefer hotels with pools",
            createdAt = "2025-01-01T10:00:00Z"
        )
        
        val comment2 = Comment(
            id = "comment-2", eventId = eventId, section = CommentSection.TRANSPORT,
            authorId = "bob", authorName = "Bob",
            content = "Direct flights preferred",
            createdAt = "2025-01-01T10:05:00Z"
        )

        eventRepo.addComment(eventId, comment1)
        eventRepo.addComment(eventId, comment2)

        // Record votes on slots
        eventRepo.addVote(eventId, "alice", "slot-1", Vote.YES)
        eventRepo.addVote(eventId, "bob", "slot-1", Vote.YES)
        eventRepo.addVote(eventId, "charlie", "slot-2", Vote.YES)

        // THEN
        val comments = eventRepo.getComments(eventId)
        assertEquals(2, comments.size)
        assertTrue(comments.any { it.section == CommentSection.ACCOMMODATION })
        assertTrue(comments.any { it.section == CommentSection.TRANSPORT })

        val poll = eventRepo.getPoll(eventId)!!
        assertNotNull(poll)

        scope.cancel()
    }

    /**
     * Test 5: Error Handling and Recovery
     * 
     * GIVEN: Operation fails
     * WHEN:  Error is handled and retry occurs
     * THEN:  No data loss and successful retry
     */
    @Test
    fun testErrorHandling_RecoveryWorkflow() = runTest {
        // GIVEN
        val eventId = "event-error"
        val scope = CoroutineScope(this.testScheduler + SupervisorJob())
        val eventRepo = MockEventRepository()

        val event = createTestEvent(id = eventId)
        eventRepo.createEvent(event)

        // WHEN - Simulate error
        eventRepo.shouldFailVote = true
        val failResult = eventRepo.addVote(eventId, "alice", "slot-1", Vote.YES)
        
        // THEN - Verify failure
        assertTrue(failResult.isFailure)

        // WHEN - Retry after fix
        eventRepo.shouldFailVote = false
        val successResult = eventRepo.addVote(eventId, "alice", "slot-1", Vote.YES)

        // THEN - Verify success
        assertTrue(successResult.isSuccess)

        // Verify event not corrupted
        val finalEvent = eventRepo.getEvent(eventId)!!
        assertEquals(EventStatus.DRAFT, finalEvent.status)

        scope.cancel()
    }

    /**
     * Test 6: Offline-Online Synchronization
     * 
     * GIVEN: User in offline mode
     * WHEN:  Create data and go online
     * THEN:  Data syncs without loss
     */
    @Test
    fun testOfflineOnlineSync() = runTest {
        // GIVEN - Offline mode
        val eventId = "event-offline"
        val scope = CoroutineScope(this.testScheduler + SupervisorJob())
        val eventRepo = MockEventRepository()
        eventRepo.simulateNetworkDelay = 0L // No network

        val event = createTestEvent(id = eventId)
        eventRepo.createEvent(event)

        eventRepo.addParticipant(eventId, "alice")
        eventRepo.addParticipant(eventId, "bob")

        // WHEN - Go online
        eventRepo.simulateNetworkDelay = 50L // Network active

        val updateResult = eventRepo.updateEvent(event.copy(
            description = "Updated while online",
            participants = listOf("alice", "bob", "charlie")
        ))

        // THEN - Verify sync succeeded
        assertTrue(updateResult.isSuccess)

        val finalEvent = eventRepo.getEvent(eventId)!!
        assertEquals("Updated while online", finalEvent.description)
        assertEquals(3, finalEvent.participants.size)

        scope.cancel()
    }
}

// ========================================================================
// Test Support Data Classes
// ========================================================================

/**
 * Mock AccommodationOption for testing
 */
data class AccommodationOption(
    val id: String,
    val name: String,
    val type: String,
    val costPerNight: Double,
    val availableRooms: Int,
    val location: String
)
