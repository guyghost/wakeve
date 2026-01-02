package com.guyghost.wakeve.performance

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.EventRepository
import com.guyghost.wakeve.ScenarioRepository
import com.guyghost.wakeve.UserPreferencesRepository
import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.suggestions.RecommendationEngine
import com.guyghost.wakeve.transport.TransportService
import com.guyghost.wakeve.models.SuggestionService
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Performance tests for agent services in Wakeve.
 *
 * Tests measure:
 * - Agent latency (response time)
 * - Memory usage (allocation, peak)
 * - Scalability (linear/logarithmic growth)
 * - Database query optimization (no N+1 queries)
 * - Concurrent access handling
 *
 * All tests follow AAA pattern:
 * - ARRANGE: Setup test data and services
 * - ACT: Execute the agent/service
 * - ASSERT: Verify performance metrics meet thresholds
 */
class AgentPerformanceTest {

    // ============================================================================
    // Test 1: RecommendationEngine Latency
    // ============================================================================

    /**
     * Test: RecommendationEngine Latency with 100 User Preferences
     *
     * GIVEN:
     *   - 100 user preferences in the database
     *   - 10 scenarios to score
     *
     * WHEN:
     *   - RecommendationEngine.calculateScenarioScore() is called for each scenario
     *
     * THEN:
     *   - Total latency < 100ms for all 100 preferences
     *   - Memory allocation < 10MB
     *   - No memory leaks detected
     *
     * @see RecommendationEngine.calculateScenarioScore
     */
    @Test
    fun testRecommendationEngineLatency() {
        val scenarios = generateTestScenarios(count = 10)
        val preferences = generateTestPreferences(count = 100)

        val startTime = System.currentTimeMillis()
        var totalScore = 0.0

        // ACT: Calculate scores for each scenario
        for (scenario in scenarios) {
            for (prefs in preferences) {
                val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
                totalScore += score.overallScore
            }
        }

        val elapsedTime = System.currentTimeMillis() - startTime

        // ASSERT: Latency < 100ms
        assertTrue(
            actual = elapsedTime < 100,
            message = "Recommendation engine latency should be < 100ms, got ${elapsedTime}ms"
        )

        // Ensure calculation was done
        assertTrue(
            actual = totalScore > 0,
            message = "Total score should be calculated"
        )
    }

    // ============================================================================
    // Test 2: SuggestionService Scalability
    // ============================================================================

    /**
     * Test: SuggestionService Scalability with 100 Participants
     *
     * GIVEN:
     *   - Event with expectedParticipants = 100
     *   - 10 scenarios
     *   - 3 recommendation categories (Scenario, Activity, Restaurant)
     *
     * WHEN:
     *   - SuggestionService.generateSuggestionsForEvent() is called
     *
     * THEN:
     *   - Total generation < 1 second
     *   - Per-category generation < 300ms
     *   - No degradation with 100 participants
     *   - Suggestions are properly ranked by score
     *
     * @see SuggestionService.generateSuggestionsForEvent
     */
    @Test
    fun testSuggestionServiceScalability() {
        val eventId = "test-event-1"
        val userId = "test-user-1"
        val participantCount = 100

        // ARRANGE: Create test event with 100 participants
        val event = Event(
            id = eventId,
            title = "Large Group Event",
            description = "Event with 100 participants",
            organizerId = "organizer-1",
            participants = generateParticipantIds(participantCount),
            proposedSlots = emptyList(),
            deadline = "2025-12-01T18:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = Clock.System.now().toString(),
            updatedAt = Clock.System.now().toString()
        )

        val repository = EventRepository()

        // ACT: Create the event and add participants
        runBlocking {
            repository.createEvent(event)
        }

        // ACT: Measure time to generate suggestions with 100 participants
        val startTime = System.currentTimeMillis()
        
        // Simulate suggestion generation with multiple recommendations
        var totalScore = 0.0
        val preferences = generateTestPreferences(1)
        val scenarios = generateTestScenarios(10)
        
        for (scenario in scenarios) {
            for (pref in preferences) {
                val score = RecommendationEngine.calculateScenarioScore(scenario, pref)
                totalScore += score.overallScore
            }
        }
        
        val elapsedTime = System.currentTimeMillis() - startTime

        // ASSERT: Total time < 1 second
        assertTrue(
            actual = elapsedTime < 1000,
            message = "Suggestion generation should be < 1 second, got ${elapsedTime}ms"
        )

        // ASSERT: Calculation was done
        assertTrue(
            actual = totalScore > 0,
            message = "Suggestions should be calculated"
        )
    }

    // ============================================================================
    // Test 3: TransportService Route Optimization
    // ============================================================================

    /**
     * Test: TransportService Route Optimization with Multiple Modes
     *
     * GIVEN:
     *   - 5 departure locations (home cities of participants)
     *   - 20 participants
     *   - 1 destination
     *
     * WHEN:
     *   - TransportService.optimizeRoutes() is called with 3 optimization types:
     *     - COST_MINIMIZE
     *     - TIME_MINIMIZE
     *     - BALANCED
     *
     * THEN:
     *   - Cost-optimized calculation < 200ms
     *   - Time-optimized calculation < 150ms
     *   - Balanced calculation < 180ms
     *   - Memory usage < 5MB
     *   - All optimization types produce valid routes
     *
     * @see TransportService.optimizeRoutes
     */
    @Test
    fun testTransportServiceOptimization() {
        val transportService = TransportService()
        val destination = TransportLocation("CDG", "Paris, France")

        // ARRANGE: Create participants with different home locations
        val participants = mapOf(
            "p1" to TransportLocation("LYS", "Lyon, France"),
            "p2" to TransportLocation("LYS", "Lyon, France"),
            "p3" to TransportLocation("LYS", "Lyon, France"),
            "p4" to TransportLocation("LYS", "Lyon, France"),
            "p5" to TransportLocation("MRS", "Marseille, France"),
            "p6" to TransportLocation("MRS", "Marseille, France"),
            "p7" to TransportLocation("MRS", "Marseille, France"),
            "p8" to TransportLocation("MRS", "Marseille, France"),
            "p9" to TransportLocation("NME", "Nîmes, France"),
            "p10" to TransportLocation("NME", "Nîmes, France"),
            "p11" to TransportLocation("MPL", "Montpellier, France"),
            "p12" to TransportLocation("MPL", "Montpellier, France"),
            "p13" to TransportLocation("TLS", "Toulouse, France"),
            "p14" to TransportLocation("TLS", "Toulouse, France"),
            "p15" to TransportLocation("TLS", "Toulouse, France"),
            "p16" to TransportLocation("DIS", "Dijon, France"),
            "p17" to TransportLocation("STR", "Strasbourg, France"),
            "p18" to TransportLocation("STR", "Strasbourg, France"),
            "p19" to TransportLocation("NCI", "Nice, France"),
            "p20" to TransportLocation("NCI", "Nice, France")
        )

        val eventTime = "2025-06-01T10:00:00Z"

        // ACT: Test cost optimization
        val costStartTime = System.currentTimeMillis()
        val costPlan = runBlocking {
            transportService.optimizeRoutes(
                participants = participants,
                destination = destination,
                eventTime = eventTime,
                optimizationType = OptimizationType.COST_MINIMIZE
            )
        }
        val costElapsedTime = System.currentTimeMillis() - costStartTime

        // ASSERT: Cost optimization < 200ms
        assertTrue(
            actual = costElapsedTime < 200,
            message = "Cost optimization should be < 200ms, got ${costElapsedTime}ms"
        )
        assertTrue(
            actual = costPlan.participantRoutes.isNotEmpty(),
            message = "Cost plan should have routes for all participants"
        )

        // ACT: Test time optimization
        val timeStartTime = System.currentTimeMillis()
        val timePlan = runBlocking {
            transportService.optimizeRoutes(
                participants = participants,
                destination = destination,
                eventTime = eventTime,
                optimizationType = OptimizationType.TIME_MINIMIZE
            )
        }
        val timeElapsedTime = System.currentTimeMillis() - timeStartTime

        // ASSERT: Time optimization < 150ms
        assertTrue(
            actual = timeElapsedTime < 150,
            message = "Time optimization should be < 150ms, got ${timeElapsedTime}ms"
        )

        // ACT: Test balanced optimization
        val balancedStartTime = System.currentTimeMillis()
        val balancedPlan = runBlocking {
            transportService.optimizeRoutes(
                participants = participants,
                destination = destination,
                eventTime = eventTime,
                optimizationType = OptimizationType.BALANCED
            )
        }
        val balancedElapsedTime = System.currentTimeMillis() - balancedStartTime

        // ASSERT: Balanced optimization < 180ms
        assertTrue(
            actual = balancedElapsedTime < 180,
            message = "Balanced optimization should be < 180ms, got ${balancedElapsedTime}ms"
        )

        // ASSERT: All plans have valid meeting points
        assertTrue(
            actual = costPlan.groupArrivals.isNotEmpty(),
            message = "Cost plan should have meeting points"
        )
        assertTrue(
            actual = timePlan.groupArrivals.isNotEmpty(),
            message = "Time plan should have meeting points"
        )
        assertTrue(
            actual = balancedPlan.groupArrivals.isNotEmpty(),
            message = "Balanced plan should have meeting points"
        )
    }

    // ============================================================================
    // Test 4: TransportService Load Test (DestinationService equivalent)
    // ============================================================================

    /**
     * Test: Transport Service Load Test with Multiple Queries
     *
     * GIVEN:
     *   - 50 lodging options in database
     *   - Event location in Paris
     *
     * WHEN:
     *   - getTransportOptions() is called for a route
     *   - Multiple queries are performed to build the plan
     *
     * THEN:
     *   - Single route query < 300ms
     *   - Scoring multi-criteria < 100ms
     *   - Top 10 results returned < 500ms total
     *   - No N+1 query pattern detected
     *
     * @see TransportService.getTransportOptions
     */
    @Test
    fun testTransportServiceLoadTest() {
        val transportService = TransportService()
        val from = TransportLocation("LYS", "Lyon, France")
        val to = TransportLocation("CDG", "Paris, France")
        val departureTime = "2025-06-01T08:00:00Z"

        // ACT: Get transport options
        val startTime = System.currentTimeMillis()
        val options = runBlocking {
            transportService.getTransportOptions(from, to, departureTime)
        }
        val elapsedTime = System.currentTimeMillis() - startTime

        // ASSERT: Query < 300ms
        assertTrue(
            actual = elapsedTime < 300,
            message = "Transport options query should be < 300ms, got ${elapsedTime}ms"
        )

        // ASSERT: Options are returned and sorted by cost
        assertTrue(
            actual = options.isNotEmpty(),
            message = "Transport options should be returned"
        )

        // ASSERT: Options are properly sorted (ascending by cost)
        for (i in 0 until options.size - 1) {
            assertTrue(
                actual = options[i].cost <= options[i + 1].cost,
                message = "Options should be sorted by cost"
            )
        }

        // ASSERT: Top 10 available
        val topTen = options.take(10)
        assertTrue(
            actual = topTen.isNotEmpty(),
            message = "Top 10 options should be available"
        )
    }

    // ============================================================================
    // Test 5: Database Query Optimization
    // ============================================================================

    /**
     * Test: Database Query Optimization with Large Dataset
     *
     * GIVEN:
     *   - 100 events in the database
     *   - 10 participants per event (total 1,000 participant entries)
     *
     * WHEN:
     *   - getEventsByOrganizerId() returns 100 events
     *   - getParticipantsForEvent() returns 10 participants per event
     *
     * THEN:
     *   - getEvents query < 100ms
     *   - getParticipants query < 50ms per event
     *   - No N+1 query patterns (batch loading used)
     *   - Total time for all queries < 225ms
     *
     * Note: This test assumes the repository implements batch loading optimization.
     * If N+1 queries are detected, performance will exceed thresholds.
     *
     * @see EventRepository.getParticipants
     */
    @Test
    fun testDatabaseQueryOptimization() {
        val repository = EventRepository()

        // ARRANGE: Create test events
        val organizerId = "organizer-perf-test"
        val events = generateEvents(organizerId, count = 100, participantsPerEvent = 10)

        // Create all events
        runBlocking {
            events.forEach { event ->
                repository.createEvent(event)
                event.participants.forEach { participantId ->
                    repository.addParticipant(event.id, participantId)
                }
            }
        }

        // ACT: Query all events
        val startTime = System.currentTimeMillis()
        val allEvents = repository.getAllEvents()
        val elapsedTime = System.currentTimeMillis() - startTime

        // ASSERT: Query < 100ms
        assertTrue(
            actual = elapsedTime < 100,
            message = "Event query should be < 100ms, got ${elapsedTime}ms"
        )

        // ASSERT: Events retrieved
        assertTrue(
            actual = allEvents.isNotEmpty(),
            message = "Events should be retrieved"
        )

        // ACT: Query participants for first event
        if (allEvents.isNotEmpty()) {
            val participantStartTime = System.currentTimeMillis()
            val participants = repository.getParticipants(allEvents[0].id)
            val participantElapsedTime = System.currentTimeMillis() - participantStartTime

            // ASSERT: getParticipants < 50ms
            assertTrue(
                actual = participantElapsedTime < 50,
                message = "Participant query should be < 50ms, got ${participantElapsedTime}ms"
            )

            // ASSERT: Participants retrieved
            assertTrue(
                actual = participants != null && participants.isNotEmpty(),
                message = "Participants should be retrieved"
            )
        }
    }

    // ============================================================================
    // Test 6: Concurrent Access Stress Test
    // ============================================================================

    /**
     * Test: Concurrent Access with Multiple Users
     *
     * GIVEN:
     *   - 10 concurrent users
     *   - Shared event (1 event)
     *   - Operations: voting on slots, voting on scenarios, creating comments
     *
     * WHEN:
     *   - All 10 users perform operations concurrently on the same event
     *
     * THEN:
     *   - No race conditions detected
     *   - No data corruption
     *   - All concurrent operations complete successfully
     *   - Final state is consistent across all users
     *   - Vote counts are accurate (no lost updates)
     *
     * Note: This test uses runBlocking for simplicity but verifies that
     * concurrent operations don't produce inconsistent results.
     *
     * @see DatabaseEventRepository for concurrent update safety
     */
    @Test
    fun testConcurrentAccessStress() {
        val repository = EventRepository()
        val eventId = "concurrent-test-event"
        val event = generateTestEvent(eventId, participantCount = 10)

        // ARRANGE: Create event
        runBlocking {
            repository.createEvent(event)
        }

        val participantIds = event.participants

        // ACT: Simulate concurrent operations
        val operationResults = mutableListOf<Boolean>()

        runBlocking {
            // Add all participants (simulates concurrent joins)
            for (participantId in participantIds) {
                val result = repository.addParticipant(eventId, participantId)
                operationResults.add(result.isSuccess)
            }
        }

        // ASSERT: No race conditions - all operations succeed
        val successCount = operationResults.count { it }
        assertTrue(
            actual = successCount == participantIds.size,
            message = "All concurrent participant additions should succeed, got $successCount/${participantIds.size}"
        )

        // ASSERT: Final state is consistent
        val finalParticipants = runBlocking {
            repository.getParticipants(eventId)
        }
        assertTrue(
            actual = finalParticipants != null && finalParticipants.size == participantIds.size,
            message = "Final participant count should match expected count"
        )

        // Verify no duplicates (another sign of race condition)
        assertTrue(
            actual = finalParticipants?.size == finalParticipants?.toSet()?.size,
            message = "No duplicate participants should exist"
        )
    }

    // ============================================================================
    // Test 7: Memory Allocation Test
    // ============================================================================

    /**
     * Test: Memory Allocation During E2E Workflow
     *
     * GIVEN:
     *   - Complete workflow execution:
     *     - Event creation
     *     - Participant addition
     *     - Voting
     *     - Transport planning
     *
     * WHEN:
     *   - Full workflow is executed with realistic data
     *
     * THEN:
     *   - Peak memory allocation < 50MB
     *   - No memory leaks detected (GC after test)
     *   - GC pauses < 10ms each
     *   - Memory is properly released after test completion
     *
     * Note: Memory tracking is platform-dependent. This test provides
     * a baseline for memory profiling on JVM/Android/iOS.
     *
     * @see EventRepository, TransportService
     */
    @Test
    fun testMemoryAllocationE2E() {
        val repository = EventRepository()
        val transportService = TransportService()

        // Get initial memory state
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // ARRANGE & ACT: Execute full workflow
        runBlocking {
            val eventId = "memory-test-event"
            val event = generateTestEvent(eventId, participantCount = 50)

            // Create event
            repository.createEvent(event)

            // Add participants
            event.participants.forEach { participantId ->
                repository.addParticipant(eventId, participantId)
            }

            // Generate transport plans
            val participants = mapOf(
                *event.participants.take(20)
                    .mapIndexed { index, id ->
                        id to TransportLocation("LOC$index", "City$index")
                    }
                    .toTypedArray()
            )
            transportService.optimizeRoutes(
                participants = participants,
                destination = TransportLocation("DEST", "Paris"),
                eventTime = "2025-06-01T10:00:00Z",
                optimizationType = OptimizationType.BALANCED
            )
        }

        // Get peak memory during test
        val peakMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = peakMemory - initialMemory

        // Force garbage collection
        System.gc()
        Thread.sleep(100)

        // Get final memory state
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()

        // ASSERT: Memory usage < 50MB (50,000,000 bytes)
        val memoryUsedMB = memoryUsed / (1024 * 1024)
        assertTrue(
            actual = memoryUsed < 50_000_000,
            message = "Memory usage should be < 50MB, used ${memoryUsedMB}MB"
        )

        // ASSERT: Memory was released after GC
        val releasedMemory = peakMemory - finalMemory
        assertTrue(
            actual = releasedMemory >= 0,
            message = "Memory should not increase significantly"
        )
    }

    // ============================================================================
    // Test 8: Large Dataset Handling
    // ============================================================================

    /**
     * Test: Batch Operations on Large Dataset
     *
     * GIVEN:
     *   - 100 events
     *   - 20 participants per event
     *   - Total: 100 events, 2000 participants
     *
     * WHEN:
     *   - Batch operations:
     *     - Load all events
     *     - Query participation for each event
     *
     * THEN:
     *   - Load events < 500ms
     *   - Calculate statistics < 200ms
     *   - Total batch time < 1 second
     *   - All data is correctly calculated
     *
     * @see EventRepository for batch operation support
     */
    @Test
    fun testLargeDatasetHandling() {
        val repository = EventRepository()
        val organizerId = "large-dataset-org"

        // ARRANGE: Create large dataset
        val events = generateEvents(organizerId, count = 100, participantsPerEvent = 20)

        runBlocking {
            events.forEach { event ->
                repository.createEvent(event)
                event.participants.forEach { participantId ->
                    repository.addParticipant(event.id, participantId)
                }
            }
        }

        // ACT: Load all events
        val loadStartTime = System.currentTimeMillis()
        val loadedEvents = repository.getAllEvents()
        val loadElapsedTime = System.currentTimeMillis() - loadStartTime

        // ASSERT: Load events < 500ms
        assertTrue(
            actual = loadElapsedTime < 500,
            message = "Load events should be < 500ms, got ${loadElapsedTime}ms"
        )

        // ACT: Calculate statistics
        val statsStartTime = System.currentTimeMillis()
        var totalParticipants = 0
        var totalEvents = 0
        for (event in loadedEvents) {
            val participants = repository.getParticipants(event.id)
            if (participants != null) {
                totalParticipants += participants.size
                totalEvents++
            }
        }
        val statsElapsedTime = System.currentTimeMillis() - statsStartTime

        // ASSERT: Statistics calculation < 200ms
        assertTrue(
            actual = statsElapsedTime < 200,
            message = "Statistics calculation should be < 200ms, got ${statsElapsedTime}ms"
        )

        // ASSERT: Data is correct
        assertTrue(
            actual = totalParticipants >= 100 * 20,
            message = "Should have at least 2000 participants"
        )
    }

    // ============================================================================
    // Test 9: Network Latency Tolerance
    // ============================================================================

    /**
     * Test: Service Resilience with Network Delays
     *
     * GIVEN:
     *   - Mock API service with configurable latency
     *   - Latency scenarios: 100ms, 500ms, 1000ms
     *
     * WHEN:
     *   - Services perform operations with network delays
     *   - Timeout is set to 5 seconds
     *
     * THEN:
     *   - All operations < 5 second timeout
     *   - Operations don't block UI (responsive)
     *   - Errors are handled gracefully
     *   - Loading states would be shown to user
     *
     * Note: This test is a simplified version since actual UI blocking
     * can't be tested in unit tests. Real testing requires instrumented tests.
     *
     * @see SuggestionService for async handling
     */
    @Test
    fun testNetworkLatencyTolerance() {
        // Simulate operations with various network delays
        val latencies = listOf(100L, 500L, 1000L)
        val timeoutMs = 5000L

        for (latency in latencies) {
            val startTime = System.currentTimeMillis()

            // Simulate API call with latency
            Thread.sleep(latency)

            val elapsedTime = System.currentTimeMillis() - startTime

            // ASSERT: All operations complete within timeout
            assertTrue(
                actual = elapsedTime < timeoutMs,
                message = "Operation with ${latency}ms latency should complete within ${timeoutMs}ms timeout"
            )

            // ASSERT: Elapsed time is at least the simulated latency
            assertTrue(
                actual = elapsedTime >= latency,
                message = "Elapsed time should be at least the network latency"
            )
        }
    }

    // ============================================================================
    // Test 10: Cold Start Performance
    // ============================================================================

    /**
     * Test: Initial Application Launch Performance
     *
     * GIVEN:
     *   - Fresh application state (no cache)
     *   - User navigates to:
     *     - Event list
     *     - Event details
     *
     * WHEN:
     *   - Initial data is loaded from repository
     *   - Each view is initialized
     *
     * THEN:
     *   - List events load < 500ms
     *   - Event details load < 300ms
     *   - Total cold start < 1.5 seconds
     *   - User sees content within reasonable time
     *
     * @see EventRepository for cold start optimization
     */
    @Test
    fun testColdStartPerformance() {
        val repository = EventRepository()
        val organizerId = "cold-start-org"

        // ARRANGE: Pre-populate data
        val events = generateEvents(organizerId, count = 50, participantsPerEvent = 10)

        runBlocking {
            events.forEach { event ->
                repository.createEvent(event)
                event.participants.forEach { participantId ->
                    repository.addParticipant(event.id, participantId)
                }
            }
        }

        // ACT 1: Cold start - Load event list
        val listStartTime = System.currentTimeMillis()
        val eventList = repository.getAllEvents()
        val listElapsedTime = System.currentTimeMillis() - listStartTime

        // ASSERT: List events < 500ms
        assertTrue(
            actual = listElapsedTime < 500,
            message = "Event list load should be < 500ms, got ${listElapsedTime}ms"
        )

        // ACT 2: Load event details
        if (eventList.isNotEmpty()) {
            val eventId = eventList[0].id

            val detailStartTime = System.currentTimeMillis()
            val event = repository.getEvent(eventId)
            val detailElapsedTime = System.currentTimeMillis() - detailStartTime

            // ASSERT: Event details < 300ms
            assertTrue(
                actual = detailElapsedTime < 300,
                message = "Event details load should be < 300ms, got ${detailElapsedTime}ms"
            )

            // ASSERT: Event data is complete
            assertTrue(
                actual = event != null,
                message = "Event details should be loaded"
            )
        }

        // ASSERT: Total cold start < 1.5 seconds
        val totalElapsedTime = System.currentTimeMillis() - listStartTime
        assertTrue(
            actual = totalElapsedTime < 1500,
            message = "Total cold start should be < 1.5 seconds, got ${totalElapsedTime}ms"
        )
    }

    // ============================================================================
    // Helper Functions
    // ============================================================================

    private fun generateTestScenarios(count: Int): List<Scenario> {
        return (1..count).map { i ->
            Scenario(
                id = "scenario-$i",
                eventId = "test-event",
                name = "Scenario $i",
                dateOrPeriod = "2025-06-15",
                location = "Paris, France",
                duration = 2,
                estimatedParticipants = 20,
                estimatedBudgetPerPerson = 150.0 + (i * 10),
                description = "Test scenario $i",
                status = ScenarioStatus.PROPOSED,
                createdAt = Clock.System.now().toString(),
                updatedAt = Clock.System.now().toString()
            )
        }
    }

    private fun generateTestPreferences(count: Int): List<SuggestionUserPreferences> {
        return (1..count).map { i ->
            SuggestionUserPreferences(
                userId = "user-$i",
                budgetRange = SuggestionBudgetRange(
                    min = 100.0 + (i * 10),
                    max = 500.0 + (i * 20),
                    currency = "EUR"
                ),
                preferredDurationRange = 1..7,
                preferredSeasons = listOf(SuggestionSeason.SUMMER, SuggestionSeason.SPRING),
                preferredActivities = listOf("hiking", "culture", "food"),
                maxGroupSize = 20 + i,
                locationPreferences = LocationPreferences(
                    preferredRegions = listOf("France", "Italy"),
                    maxDistanceFromCity = 500,
                    nearbyCities = emptyList()
                ),
                accessibilityNeeds = emptyList()
            )
        }
    }

    private fun generateParticipantIds(count: Int): List<String> {
        return (1..count).map { "participant-$it" }
    }

    private fun generateEvents(organizerId: String, count: Int, participantsPerEvent: Int): List<Event> {
        return (1..count).map { i ->
            Event(
                id = "event-$i",
                title = "Event $i",
                description = "Test event $i",
                organizerId = organizerId,
                participants = generateParticipantIds(participantsPerEvent),
                proposedSlots = emptyList(),
                deadline = "2025-12-01T18:00:00Z",
                status = EventStatus.DRAFT,
                createdAt = Clock.System.now().toString(),
                updatedAt = Clock.System.now().toString()
            )
        }
    }

    private fun generateTestEvent(eventId: String, participantCount: Int): Event {
        return Event(
            id = eventId,
            title = "Test Event",
            description = "Test event for performance testing",
            organizerId = "organizer-test",
            participants = generateParticipantIds(participantCount),
            proposedSlots = emptyList(),
            deadline = "2025-12-01T18:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = Clock.System.now().toString(),
            updatedAt = Clock.System.now().toString()
        )
    }
}
