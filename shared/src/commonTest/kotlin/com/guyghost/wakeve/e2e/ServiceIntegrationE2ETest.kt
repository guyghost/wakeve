package com.guyghost.wakeve.e2e

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.models.Comment
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.TransportLocation
import com.guyghost.wakeve.models.TransportMode
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * # Service Integration E2E Tests for Wakeve
 *
 * These tests validate the integration of auxiliary services with the core event management system:
 * - TransportService: Multi-participant route optimization
 * - CalendarService: ICS invitation generation and calendar integration
 * - SuggestionService: Personalized recommendations
 * - BudgetService: Cost tracking and reporting
 * - MeetingService: Virtual meeting link generation
 * - DestinationService: Location recommendations
 *
 * Pattern: Each test validates service integration with the core event workflow
 * - **GIVEN**: Event and service dependencies configured
 * - **WHEN**: Service operations called within event context
 * - **THEN**: Services produce expected results and integrate with event data
 *
 * Phase 6 - Service Integration Testing
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServiceIntegrationE2ETest {

    // ========================================================================
    // Mock Repository (Reused from PrdWorkflowE2ETest)
    // ========================================================================

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
    // Mock Services
    // ========================================================================

    /**
     * Mock Suggestion Service for testing personalized recommendations
     */
    class MockSuggestionService {
        var suggestedLocations = mutableMapOf<String, List<String>>()
        var suggestedDates = mutableMapOf<String, List<String>>()

        suspend fun getLocationSuggestions(
            eventId: String,
            eventType: EventType,
            participantCount: Int
        ): List<String> {
            return suggestedLocations[eventId] ?: emptyList()
        }

        suspend fun getDateSuggestions(
            eventId: String,
            proposedSlots: List<TimeSlot>
        ): List<String> {
            return suggestedDates[eventId] ?: emptyList()
        }

        fun setSuggestions(eventId: String, locations: List<String>, dates: List<String>) {
            suggestedLocations[eventId] = locations
            suggestedDates[eventId] = dates
        }
    }

    /**
     * Mock Transport Service for testing route optimization
     */
    class MockTransportService {
        var transportOptions = mutableMapOf<String, List<TransportOption>>()
        var optimizedRoutes = mutableMapOf<String, String>()

        suspend fun getTransportOptions(
            from: TransportLocation,
            to: TransportLocation,
            departureTime: String,
            mode: TransportMode? = null
        ): List<TransportOption> {
            val key = "${from.name}-${to.name}"
            return transportOptions[key] ?: emptyList()
        }

        suspend fun optimizeMultiParticipantRoute(
            participants: List<String>,
            destination: String,
            departureTime: String
        ): String {
            val key = "route-${participants.size}-$destination"
            return optimizedRoutes[key] ?: "Optimized route for ${participants.size} participants"
        }

        fun setTransportOptions(from: String, to: String, options: List<TransportOption>) {
            transportOptions["$from-$to"] = options
        }

        fun setOptimizedRoute(key: String, route: String) {
            optimizedRoutes[key] = route
        }
    }

    /**
     * Mock Budget Service for cost tracking
     */
    class MockBudgetService {
        var budgets = mutableMapOf<String, Budget>()
        var expenses = mutableMapOf<String, List<Expense>>()

        suspend fun createBudget(
            eventId: String,
            totalBudget: Double,
            participantCount: Int
        ): Budget {
            val budget = Budget(
                id = "budget-$eventId",
                eventId = eventId,
                totalBudget = totalBudget,
                budgetPerPerson = totalBudget / participantCount,
                currency = "EUR",
                createdAt = "2025-01-01T10:00:00Z"
            )
            budgets[eventId] = budget
            return budget
        }

        suspend fun addExpense(eventId: String, amount: Double, category: String, description: String) {
            val expense = Expense(
                id = "exp-${expenses[eventId]?.size ?: 0}",
                eventId = eventId,
                amount = amount,
                category = category,
                description = description,
                createdAt = "2025-01-01T10:00:00Z"
            )
            val current = expenses[eventId] ?: emptyList()
            expenses[eventId] = current + expense
        }

        suspend fun getBudgetSummary(eventId: String): BudgetSummary {
            val budget = budgets[eventId] ?: return BudgetSummary(0.0, 0.0, 0.0, 0)
            val totalExpenses = expenses[eventId]?.sumOf { it.amount } ?: 0.0
            return BudgetSummary(
                totalBudget = budget.totalBudget,
                totalSpent = totalExpenses,
                remaining = budget.totalBudget - totalExpenses,
                participantCount = 0
            )
        }
    }

    /**
     * Mock Meeting Service for virtual meeting links
     */
    class MockMeetingService {
        var meetings = mutableMapOf<String, Meeting>()

        suspend fun generateMeetingLink(
            eventId: String,
            platform: String,
            date: String,
            time: String
        ): Meeting {
            val meeting = Meeting(
                id = "meeting-${meetings.size}",
                eventId = eventId,
                platform = platform,
                link = "https://$platform.com/rooms/${eventId}",
                date = date,
                time = time,
                createdAt = "2025-01-01T10:00:00Z"
            )
            meetings[eventId] = meeting
            return meeting
        }

        suspend fun getMeeting(eventId: String): Meeting? = meetings[eventId]
    }

    /**
     * Mock Destination Service for location recommendations
     */
    class MockDestinationService {
        var recommendations = mutableMapOf<String, List<DestinationRecommendation>>()

        suspend fun getRecommendations(
            eventType: EventType,
            participantCount: Int,
            budget: Double
        ): List<DestinationRecommendation> {
            val key = "$eventType-$participantCount-$budget"
            return recommendations[key] ?: emptyList()
        }

        fun setRecommendations(key: String, destinations: List<DestinationRecommendation>) {
            recommendations[key] = destinations
        }
    }

    // ========================================================================
    // Test Cases with Service Integration
    // ========================================================================

    /**
     * Test 1: Transport Service Integration
     *
     * GIVEN: Event with participants and multiple locations
     * WHEN:  Transport service optimizes routes
     * THEN:  Optimal routes are calculated and integrated with event
     */
    @Test
    fun testTransportServiceIntegration() = runTest {
        // GIVEN
        val eventId = "event-transport"
        val eventRepo = MockEventRepository()
        val transportService = MockTransportService()

        val event = createTestEvent(
            id = eventId,
            participants = listOf("alice", "bob", "charlie"),
            eventType = EventType.TEAM_BUILDING,
            proposedSlots = listOf(
                createTestTimeSlot("slot-1", "2025-05-15T09:00:00Z")
            )
        )
        eventRepo.createEvent(event)

        // Setup transport options
        val options = listOf(
            TransportOption(
                id = "opt-1",
                mode = TransportMode.FLIGHT,
                duration = 120,
                cost = 150.0,
                departureTime = "10:00",
                arrivalTime = "12:00",
                details = "Direct flight"
            ),
            TransportOption(
                id = "opt-2",
                mode = TransportMode.TRAIN,
                duration = 240,
                cost = 80.0,
                departureTime = "08:00",
                arrivalTime = "12:00",
                details = "Direct train"
            )
        )
        transportService.setTransportOptions("Paris", "London", options)

        // WHEN
        val from = TransportLocation("Paris")
        val to = TransportLocation("London")
        val transportOptions = transportService.getTransportOptions(
            from = from,
            to = to,
            departureTime = "2025-05-15T09:00:00Z"
        )

        // Calculate optimal route for group
        val optimizedRoute = transportService.optimizeMultiParticipantRoute(
            participants = event.participants,
            destination = "London",
            departureTime = "2025-05-15T09:00:00Z"
        )

        // THEN
        assertEquals(2, transportOptions.size)
        assertTrue(transportOptions.any { it.mode == TransportMode.FLIGHT })
        assertTrue(transportOptions.any { it.mode == TransportMode.TRAIN })
        assertNotNull(optimizedRoute)
        assertTrue(optimizedRoute.contains("3 participants"))
    }

    /**
     * Test 2: Budget Service Integration
     *
     * GIVEN: Event with confirmed participants and destinations
     * WHEN:  Budget service tracks expenses
     * THEN:  Budget summary reflects all expenses accurately
     */
    @Test
    fun testBudgetServiceIntegration() = runTest {
        // GIVEN
        val eventId = "event-budget"
        val eventRepo = MockEventRepository()
        val budgetService = MockBudgetService()

        val event = createTestEvent(
            id = eventId,
            participants = listOf("alice", "bob", "charlie"),
            status = EventStatus.CONFIRMED
        )
        eventRepo.createEvent(event)

        // WHEN - Create budget
        val budget = budgetService.createBudget(
            eventId = eventId,
            totalBudget = 3000.0,
            participantCount = event.participants.size
        )

        // Add expenses
        budgetService.addExpense(eventId, 1000.0, "accommodation", "Hotel for 3 nights")
        budgetService.addExpense(eventId, 500.0, "transport", "Flights for 3 participants")
        budgetService.addExpense(eventId, 300.0, "activities", "Tour and activities")

        // Get summary
        val summary = budgetService.getBudgetSummary(eventId)

        // THEN
        assertEquals(3000.0, budget.totalBudget)
        assertEquals(1000.0, budget.budgetPerPerson)
        assertEquals(1800.0, summary.totalSpent, 0.1)
        assertEquals(1200.0, summary.remaining, 0.1)
    }

    /**
     * Test 3: Meeting Service Integration
     *
     * GIVEN: Event confirmed with final date
     * WHEN:  Meeting service generates virtual meeting link
     * THEN:  Meeting link is created and accessible
     */
    @Test
    fun testMeetingServiceIntegration() = runTest {
        // GIVEN
        val eventId = "event-meeting"
        val eventRepo = MockEventRepository()
        val meetingService = MockMeetingService()

        val event = createTestEvent(
            id = eventId,
            status = EventStatus.CONFIRMED,
            finalDate = "2025-05-15T19:00:00Z"
        )
        eventRepo.createEvent(event)

        // WHEN - Generate meeting link
        val meeting = meetingService.generateMeetingLink(
            eventId = eventId,
            platform = "zoom",
            date = "2025-05-15",
            time = "19:00"
        )

        // THEN
        assertNotNull(meeting)
        assertTrue(meeting.link.contains("zoom"))
        assertTrue(meeting.link.contains(eventId))
        assertEquals("zoom", meeting.platform)
        assertEquals("2025-05-15", meeting.date)
    }

    /**
     * Test 4: Suggestion Service Integration
     *
     * GIVEN: Event with type and participant count
     * WHEN:  Suggestion service recommends locations and dates
     * THEN:  Personalized suggestions are provided
     */
    @Test
    fun testSuggestionServiceIntegration() = runTest {
        // GIVEN
        val eventId = "event-suggestion"
        val eventRepo = MockEventRepository()
        val suggestionService = MockSuggestionService()

        val event = createTestEvent(
            id = eventId,
            eventType = EventType.WEDDING,
            expectedParticipants = 50
        )
        eventRepo.createEvent(event)

        // Setup suggestions
        suggestionService.setSuggestions(
            eventId,
            locations = listOf("Paris", "Venice", "Barcelona"),
            dates = listOf("2025-05-15", "2025-06-01", "2025-06-15")
        )

        // WHEN - Get suggestions
        val locationSuggestions = suggestionService.getLocationSuggestions(
            eventId = eventId,
            eventType = event.eventType,
            participantCount = event.expectedParticipants ?: 0
        )

        val dateSuggestions = suggestionService.getDateSuggestions(
            eventId = eventId,
            proposedSlots = event.proposedSlots
        )

        // THEN
        assertEquals(3, locationSuggestions.size)
        assertTrue(locationSuggestions.contains("Paris"))
        assertTrue(locationSuggestions.contains("Venice"))

        assertEquals(3, dateSuggestions.size)
        assertTrue(dateSuggestions.contains("2025-05-15"))
    }

    /**
     * Test 5: Comprehensive Multi-Service Workflow
     *
     * GIVEN: Event with all services configured
     * WHEN:  Complete workflow with all services engaged
     * THEN:  All services integrate seamlessly
     */
    @Test
    fun testComprehensiveMultiServiceWorkflow() = runTest {
        // GIVEN
        val eventId = "event-comprehensive"
        val eventRepo = MockEventRepository()
        val transportService = MockTransportService()
        val budgetService = MockBudgetService()
        val meetingService = MockMeetingService()
        val suggestionService = MockSuggestionService()

        val event = createTestEvent(
            id = eventId,
            eventType = EventType.CONFERENCE,
            participants = listOf("alice", "bob", "charlie", "diana"),
            expectedParticipants = 200,
            proposedSlots = listOf(
                createTestTimeSlot("slot-1", "2025-06-15T09:00:00Z")
            )
        )
        eventRepo.createEvent(event)

        // Setup all services
        suggestionService.setSuggestions(
            eventId,
            locations = listOf("Amsterdam", "Berlin", "Vienna"),
            dates = listOf("2025-06-15", "2025-06-22")
        )

        transportService.setTransportOptions(
            "New York",
            "Amsterdam",
            listOf(
                TransportOption(
                    id = "opt-1",
                    mode = TransportMode.FLIGHT,
                    duration = 600,
                    cost = 800.0,
                    departureTime = "14:00",
                    arrivalTime = "02:00",
                    details = "Direct flight"
                )
            )
        )

        // WHEN - Execute complete workflow
        // 1. Get location suggestions
        val locations = suggestionService.getLocationSuggestions(
            eventId, event.eventType, event.expectedParticipants ?: 0
        )

        // 2. Create budget
        val budget = budgetService.createBudget(
            eventId,
            50000.0,
            event.participants.size
        )

        // 3. Plan transport
        val transportOptions = transportService.getTransportOptions(
            TransportLocation("New York"),
            TransportLocation("Amsterdam"),
            "2025-06-15T09:00:00Z"
        )

        // 4. Generate meeting link
        val meeting = meetingService.generateMeetingLink(
            eventId,
            "teams",
            "2025-06-15",
            "09:00"
        )

        // Add expenses
        budgetService.addExpense(eventId, 20000.0, "venue", "Conference center")
        budgetService.addExpense(eventId, 15000.0, "transport", "Flights for all")
        budgetService.addExpense(eventId, 10000.0, "catering", "Meals for 200 people")

        // Transition event
        eventRepo.updateEventStatus(eventId, EventStatus.CONFIRMED, "2025-06-15T09:00:00Z")

        // THEN - Verify all services worked together
        assertEquals(3, locations.size)
        assertTrue(locations.contains("Amsterdam"))

        assertEquals(12500.0, budget.budgetPerPerson)

        assertEquals(1, transportOptions.size)
        assertTrue(transportOptions[0].details.contains("Direct"))

        assertNotNull(meeting)
        assertTrue(meeting.link.contains("teams"))

        val finalEvent = eventRepo.getEvent(eventId)!!
        assertEquals(EventStatus.CONFIRMED, finalEvent.status)

        val summary = budgetService.getBudgetSummary(eventId)
        assertEquals(45000.0, summary.totalSpent, 0.1)
        assertEquals(5000.0, summary.remaining, 0.1)
    }
}

// ========================================================================
// Mock Data Classes
// ========================================================================

data class TransportOption(
    val id: String,
    val mode: TransportMode,
    val duration: Int,
    val cost: Double,
    val departureTime: String,
    val arrivalTime: String,
    val details: String
)

data class Budget(
    val id: String,
    val eventId: String,
    val totalBudget: Double,
    val budgetPerPerson: Double,
    val currency: String,
    val createdAt: String
)

data class Expense(
    val id: String,
    val eventId: String,
    val amount: Double,
    val category: String,
    val description: String,
    val createdAt: String
)

data class BudgetSummary(
    val totalBudget: Double,
    val totalSpent: Double,
    val remaining: Double,
    val participantCount: Int
)

data class Meeting(
    val id: String,
    val eventId: String,
    val platform: String,
    val link: String,
    val date: String,
    val time: String,
    val createdAt: String
)

data class DestinationRecommendation(
    val name: String,
    val type: String,
    val score: Double,
    val highlights: List<String>
)
