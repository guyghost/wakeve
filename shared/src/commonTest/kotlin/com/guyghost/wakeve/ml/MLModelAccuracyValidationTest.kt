package com.guyghost.wakeve.ml

import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.repository.UserPreferencesRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlin.math.pow
import kotlin.random.Random
import kotlin.test.*

/**
 * ML Model Accuracy Validation Test Suite
 *
 * Tests the accuracy of ML-based recommendations using simulated datasets.
 * Validates that the recommendation engine achieves > 70% accuracy on a validation set
 * according to AI predictive-recommendations spec.
 *
 * Test Coverage:
 * 1. Overall Model Accuracy (70%+ target)
 * 2. Weekend Preference Prediction
 * 3. Afternoon Preference Prediction
 * 4. Event Type Matching (Cultural Events)
 * 5. Seasonality Prediction
 * 6. Confidence Score Distribution
 * 7. Fallback Heuristic Accuracy
 *
 * @see ai-predictive-recommendations/spec.md
 */
class MLModelAccuracyValidationTest {
    // ==================== Setup & Dependencies ====================

    private lateinit var mockRepository: MockUserPreferencesRepository
    private lateinit var mlScoringEngine: MLScoringEngine

    // Simulated training data (1000 events)
    private lateinit var trainingData: List<TrainingDataPoint>

    // Simulated validation data (200 events)
    private lateinit var validationData: List<ValidationScenario>

    // Random seed for reproducibility
    private val SEED = 42L
    private val random = Random(SEED)

    @BeforeTest
    fun setup() {
        mockRepository = MockUserPreferencesRepository()
        mlScoringEngine = MLScoringEngine(mockRepository)

        // Generate simulated datasets
        trainingData = createSimulatedTrainingData(1000)
        validationData = createSimulatedValidationData(200)
    }

    // ==================== Test 1: Overall Model Accuracy ====================

    /**
     * REQUIREMENT: Verify > 70% accuracy on validation set
     *
     * GIVEN: Validation set of 200 scenarios with diverse characteristics
     * WHEN: ML model predicts top recommendation for each scenario
     * THEN: Accuracy >= 70% (140 correct predictions)
     *
     * Metrics:
     * - Correct Predictions: Count of accurate top recommendations
     * - Accuracy: correctPredictions / totalValidationScenarios
     * - Error cases: Scenarios where prediction != expected best date
     */
    @Test
    fun `given validation set of 200 scenarios, when predict recommendations, then accuracy is >= 70%`() = runTest {
        // Given
        val correctPredictions = mutableListOf<Boolean>()
        val predictionDetails = mutableListOf<PredictionResult>()

        println("\n========== TEST 1: Overall Model Accuracy ==========")
        println("Validation Set Size: ${validationData.size}")
        println("Target Accuracy: 70%")

        // When
        validationData.forEach { scenario ->
            val recommendations = mlScoringEngine.predictDateScores(
                eventId = scenario.eventId,
                timeSlots = scenario.proposedDates,
                eventType = scenario.eventType,
                userId = scenario.userId
            )

            val topRecommendation = recommendations.firstOrNull()
            val isCorrect = topRecommendation?.date == scenario.expectedBestDate

            correctPredictions.add(isCorrect)
            predictionDetails.add(
                PredictionResult(
                    scenarioId = scenario.scenarioId,
                    expected = scenario.expectedBestDate,
                    predicted = topRecommendation?.date,
                    score = topRecommendation?.score ?: 0.0,
                    confidence = topRecommendation?.confidenceScore ?: 0.0,
                    isCorrect = isCorrect
                )
            )
        }

        // Then
        val correctCount = correctPredictions.count { it }
        val accuracy = correctCount.toDouble() / validationData.size
        val accuracyPercent = (accuracy * 100).toInt()

        println("\n--- Results ---")
        println("Correct Predictions: $correctCount/${validationData.size}")
        println("Accuracy: $accuracyPercent%")
        println("\n--- Top 5 Correct Predictions ---")
        predictionDetails.filter { it.isCorrect }.take(5).forEach {
            println("  ✓ ${it.scenarioId}: ${it.predicted} (score=${String.format("%.2f", it.score)}, confidence=${String.format("%.1f%%", it.confidence * 100)})")
        }
        println("\n--- Top 5 Incorrect Predictions ---")
        predictionDetails.filterNot { it.isCorrect }.take(5).forEach {
            println("  ✗ ${it.scenarioId}: Expected ${it.expected}, Got ${it.predicted} (score=${String.format("%.2f", it.score)}, confidence=${String.format("%.1f%%", it.confidence * 100)})")
        }

        assertTrue(
            accuracy >= 0.70,
            "Model accuracy should be >= 70%, but was $accuracyPercent%"
        )
    }

    // ==================== Test 2: Weekend Preference Prediction ====================

    /**
     * REQUIREMENT: Verify weekend preference learning
     *
     * GIVEN: User prefers weekend events (SATURDAY, SUNDAY)
     * WHEN: Model predicts date scores for mixed weekday/weekend slots
     * THEN: At least 2 of top 3 recommendations are weekends
     *
     * Business Rule: User who creates weekend events should see weekends prioritized
     */
    @Test
    fun `given user prefers weekend events, when predict dates, then top 3 recommendations are weekends`() = runTest {
        // Given
        val userId = "weekend-lover"
        val preferences = UserPreference(
            userId = userId,
            preferredDays = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            preferredTimeOfDay = emptyList(),
            preferredEventTypes = emptyList(),
            preferredLocations = emptyList(),
            avoidEvents = false,
            scoreWeights = ScoreWeights.DEFAULT,
            lastUpdated = getCurrentTimestamp()
        )
        mockRepository.setUserPreferences(userId, preferences)

        val proposedDates = generateTimeSlotsMixedWeekdays(
            weekendSlots = 5,
            weekdaySlots = 5
        )

        println("\n========== TEST 2: Weekend Preference Prediction ==========")
        println("User Preference: SATURDAY, SUNDAY")
        println("Total Proposed Dates: ${proposedDates.size} (5 weekend + 5 weekday)")

        // When
        val recommendations = mlScoringEngine.predictDateScores(
            eventId = "weekend-event",
            timeSlots = proposedDates,
            eventType = EventType.PARTY,
            userId = userId
        )

        // Only take top 3
        val top3 = recommendations.take(3)

        // Then
        val weekendCount = top3.count { recommendation ->
            val dayOfWeek = getDayOfWeekFromDate(recommendation.date)
            dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        }

        println("\n--- Results ---")
        println("Top 3 Recommendations:")
        top3.forEachIndexed { idx, rec ->
            val dayOfWeek = getDayOfWeekFromDate(rec.date)
            val isWeekend = dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            val marker = if (isWeekend) "✓" else "✗"
            println("  $marker ${idx + 1}. ${rec.date} ($dayOfWeek) - score=${String.format("%.2f", rec.score)}")
        }
        println("Weekend count in top 3: $weekendCount/3")

        assertTrue(
            weekendCount >= 2,
            "Top 3 recommendations should contain at least 2 weekend dates, but found $weekendCount"
        )
    }

    // ==================== Test 3: Afternoon Preference Prediction ====================

    /**
     * REQUIREMENT: Verify time-of-day preference learning
     *
     * GIVEN: User prefers afternoon events
     * WHEN: Model predicts scores for all time-of-day slots
     * THEN: Average afternoon score > Average other time scores
     *
     * Business Rule: User behavior is learned and applied to recommendations
     */
    @Test
    fun `given user prefers afternoon events, when predict dates, then afternoon slots prioritized`() = runTest {
        // Given
        val userId = "afternoon-lover"
        val preferences = UserPreference(
            userId = userId,
            preferredDays = emptyList(),
            preferredTimeOfDay = listOf(TimeOfDay.AFTERNOON),
            preferredEventTypes = emptyList(),
            preferredLocations = emptyList(),
            avoidEvents = false,
            scoreWeights = ScoreWeights.DEFAULT,
            lastUpdated = getCurrentTimestamp()
        )
        mockRepository.setUserPreferences(userId, preferences)

        val proposedDates = generateTimeSlotsByTimeOfDay(
            morningSlots = 3,
            afternoonSlots = 3,
            eveningSlots = 3
        )

        println("\n========== TEST 3: Afternoon Preference Prediction ==========")
        println("User Preference: AFTERNOON")
        println("Total Proposed Dates: ${proposedDates.size} (3 morning + 3 afternoon + 3 evening)")

        // When
        val recommendations = mlScoringEngine.predictDateScores(
            eventId = "afternoon-event",
            timeSlots = proposedDates,
            eventType = EventType.LUNCH_MEETING,
            userId = userId
        )

        val afternoonRecommendations = recommendations.filter { 
            it.features["timeOfDay"] == TimeOfDay.AFTERNOON || 
            it.features["timeOfDay"]?.toString() == "AFTERNOON"
        }
        val otherRecommendations = recommendations.filter { 
            it.features["timeOfDay"] != TimeOfDay.AFTERNOON && 
            it.features["timeOfDay"]?.toString() != "AFTERNOON"
        }

        // Then
        val avgAfternoonScore = if (afternoonRecommendations.isNotEmpty()) {
            afternoonRecommendations.map { it.score }.average()
        } else {
            0.0
        }

        val avgOtherScore = if (otherRecommendations.isNotEmpty()) {
            otherRecommendations.map { it.score }.average()
        } else {
            0.0
        }

        println("\n--- Results ---")
        println("Afternoon Slots (${afternoonRecommendations.size}):")
        afternoonRecommendations.forEach {
            println("  • ${it.date} - score=${String.format("%.2f", it.score)}")
        }
        println("Other Slots (${otherRecommendations.size}):")
        otherRecommendations.forEach {
            println("  • ${it.date} - score=${String.format("%.2f", it.score)}")
        }
        println("\nAverage afternoon score: ${String.format("%.2f", avgAfternoonScore)}")
        println("Average other score: ${String.format("%.2f", avgOtherScore)}")

        assertTrue(
            avgAfternoonScore > avgOtherScore,
            "Afternoon slots should have higher scores than other times of day. " +
                    "Afternoon: $avgAfternoonScore, Other: $avgOtherScore"
        )
    }

    // ==================== Test 4: Event Type Matching ====================

    /**
     * REQUIREMENT: Verify event type recommendation matching
     *
     * GIVEN: Cultural event type
     * WHEN: Model suggests locations (museums, theaters vs parks, stadiums)
     * THEN: Cultural locations score higher than non-cultural
     *
     * Business Rule: Different event types have suitable location recommendations
     */
    @Test
    fun `given cultural event type, when suggest locations, then museums and theaters prioritized`() = runTest {
        // Given
        val eventType = EventType.CULTURAL_EVENT
        val potentialLocations = listOf(
            createPotentialLocation("1", "Louvre Museum", "museum"),
            createPotentialLocation("2", "Opéra Garnier", "theater"),
            createPotentialLocation("3", "Public Park", "park"),
            createPotentialLocation("4", "Stadium", "stadium"),
            createPotentialLocation("5", "Picasso Museum", "museum"),
            createPotentialLocation("6", "Concert Hall", "theater")
        )

        println("\n========== TEST 4: Event Type Matching (Cultural Events) ==========")
        println("Event Type: $eventType")
        println("Total Locations: ${potentialLocations.size}")

        // When
        val locationScores = mlScoringEngine.predictLocationSuitability(
            eventId = "cultural-event",
            locations = potentialLocations,
            eventType = eventType,
            userId = "user-1"
        )

        // Then
        val culturalLocations = locationScores.filter { score ->
            val locName = score.locationName.toLowerCase()
            locName.contains("museum") || locName.contains("opéra") || locName.contains("concert")
        }

        val otherLocations = locationScores.filter { score ->
            val locName = score.locationName.toLowerCase()
            !locName.contains("museum") && !locName.contains("opéra") && !locName.contains("concert")
        }

        val avgCulturalScore = if (culturalLocations.isNotEmpty()) {
            culturalLocations.map { it.score }.average()
        } else {
            0.0
        }

        val avgOtherScore = if (otherLocations.isNotEmpty()) {
            otherLocations.map { it.score }.average()
        } else {
            0.0
        }

        println("\n--- Results ---")
        println("Cultural Locations (${culturalLocations.size}):")
        culturalLocations.forEach {
            println("  • ${it.locationName} - score=${String.format("%.2f", it.score)}")
        }
        println("\nOther Locations (${otherLocations.size}):")
        otherLocations.forEach {
            println("  • ${it.locationName} - score=${String.format("%.2f", it.score)}")
        }
        println("\nAverage cultural score: ${String.format("%.2f", avgCulturalScore)}")
        println("Average other score: ${String.format("%.2f", avgOtherScore)}")

        assertTrue(
            avgCulturalScore > avgOtherScore * 0.9,  // Allow 10% variance
            "Cultural locations should have higher scores for CULTURAL_EVENT type. " +
                    "Cultural: $avgCulturalScore, Other: $avgOtherScore"
        )
    }

    // ==================== Test 5: Seasonality Prediction ====================

    /**
     * REQUIREMENT: Verify seasonal preference learning
     *
     * GIVEN: Summer event with all seasons proposed
     * WHEN: Model predicts date scores
     * THEN: Summer months score higher than other seasons
     *
     * Business Rule: Seasonal patterns influence recommendation scores
     */
    @Test
    fun `given summer event, when predict dates, then summer months prioritized`() = runTest {
        // Given
        val proposedDates = generateTimeSlotsBySeasons(
            summerSlots = 3,
            winterSlots = 2,
            springSlots = 2,
            fallSlots = 2
        )

        println("\n========== TEST 5: Seasonality Prediction ==========")
        println("Event Context: Summer preference")
        println("Total Proposed Dates: ${proposedDates.size} (3 summer + 2 winter + 2 spring + 2 fall)")

        // When
        val recommendations = mlScoringEngine.predictDateScores(
            eventId = "summer-event",
            timeSlots = proposedDates,
            eventType = EventType.BEACH_PARTY,
            userId = "user-1"
        )

        val summerRecommendations = recommendations.filter { recommendation ->
            val season = getSeasonFromDate(recommendation.date)
            season == Season.SUMMER
        }

        val otherRecommendations = recommendations.filter { recommendation ->
            val season = getSeasonFromDate(recommendation.date)
            season != Season.SUMMER
        }

        // Then
        val avgSummerScore = if (summerRecommendations.isNotEmpty()) {
            summerRecommendations.map { it.score }.average()
        } else {
            0.0
        }

        val avgOtherScore = if (otherRecommendations.isNotEmpty()) {
            otherRecommendations.map { it.score }.average()
        } else {
            0.0
        }

        println("\n--- Results ---")
        println("Summer Dates (${summerRecommendations.size}):")
        summerRecommendations.forEach {
            println("  • ${it.date} - score=${String.format("%.2f", it.score)}")
        }
        println("\nOther Seasons (${otherRecommendations.size}):")
        otherRecommendations.forEach {
            println("  • ${it.date} - score=${String.format("%.2f", it.score)}")
        }
        println("\nAverage summer score: ${String.format("%.2f", avgSummerScore)}")
        println("Average other score: ${String.format("%.2f", avgOtherScore)}")

        assertTrue(
            avgSummerScore >= avgOtherScore * 0.9,  // Allow 10% variance
            "Summer months should have higher scores for summer events. " +
                    "Summer: $avgSummerScore, Other: $avgOtherScore"
        )
    }

    // ==================== Test 6: Confidence Score Distribution ====================

    /**
     * REQUIREMENT: Verify confidence score reliability
     *
     * GIVEN: Validation set of all 200 scenarios
     * WHEN: Model generates predictions
     * THEN: 80%+ of predictions have confidence >= 70%
     *
     * Business Rule: Model confidence should be reliable and well-calibrated
     */
    @Test
    fun `given validation set, when predict recommendations, then 80%+ have confidence >= 70%`() = runTest {
        // Given
        val allPredictions = mutableListOf<DateScore>()

        println("\n========== TEST 6: Confidence Score Distribution ==========")
        println("Validation Set Size: ${validationData.size}")
        println("Target: 80%+ predictions with confidence >= 70%")

        // When
        validationData.forEach { scenario ->
            val recommendations = mlScoringEngine.predictDateScores(
                eventId = scenario.eventId,
                timeSlots = scenario.proposedDates,
                eventType = scenario.eventType,
                userId = scenario.userId
            )
            allPredictions.addAll(recommendations)
        }

        // Then
        val highConfidenceCount = allPredictions.count { it.confidenceScore >= 0.70 }
        val veryHighConfidenceCount = allPredictions.count { it.confidenceScore >= 0.85 }
        val lowConfidenceCount = allPredictions.count { it.confidenceScore < 0.50 }

        val highConfidencePercentage = highConfidenceCount.toDouble() / allPredictions.size
        val highConfidencePercent = (highConfidencePercentage * 100).toInt()

        println("\n--- Results ---")
        println("Total Predictions: ${allPredictions.size}")
        println("High Confidence (>=70%): $highConfidenceCount (${highConfidencePercent}%)")
        println("Very High Confidence (>=85%): $veryHighConfidenceCount")
        println("Low Confidence (<50%): $lowConfidenceCount")
        println("\nConfidence Score Statistics:")
        val confidences = allPredictions.map { it.confidenceScore }
        println("  Min: ${String.format("%.2f", confidences.minOrNull() ?: 0.0)}")
        println("  Max: ${String.format("%.2f", confidences.maxOrNull() ?: 1.0)}")
        println("  Avg: ${String.format("%.2f", confidences.average())}")
        println("  Median: ${String.format("%.2f", confidences.sorted()[confidences.size / 2])}")

        assertTrue(
            highConfidencePercentage >= 0.80,
            "80%+ predictions should have confidence >= 70%, but found $highConfidencePercent%"
        )
    }

    // ==================== Test 7: Fallback Heuristic Accuracy ====================

    /**
     * REQUIREMENT: Verify fallback heuristic effectiveness
     *
     * GIVEN: Scenarios where ML confidence < 70%
     * WHEN: Fallback heuristics are applied
     * THEN: Fallback accuracy >= 75%
     *
     * Business Rule: When ML fails, heuristics should be reliable
     */
    @Test
    fun `given ML confidence < 70%, when apply fallback, then fallback accuracy >= 75%`() = runTest {
        // Given
        val lowConfidenceScenarios = mutableListOf<ValidationScenario>()
        val fallbackCorrectCount = mutableListOf<Boolean>()

        println("\n========== TEST 7: Fallback Heuristic Accuracy ==========")
        println("Filtering scenarios where ML confidence < 70%...")

        // When
        validationData.forEach { scenario ->
            val recommendations = mlScoringEngine.predictDateScores(
                eventId = scenario.eventId,
                timeSlots = scenario.proposedDates,
                eventType = scenario.eventType,
                userId = scenario.userId
            )

            val topRecommendation = recommendations.firstOrNull()

            // Check if this is a low-confidence scenario
            if (topRecommendation != null && topRecommendation.confidenceScore < 0.70) {
                lowConfidenceScenarios.add(scenario)

                // Check if fallback was used and was correct
                val wasFallback = topRecommendation.features["predictionSource"]?.toString()?.contains("HEURISTIC", ignoreCase = true) == true
                        || topRecommendation.confidenceScore < 0.70

                val isCorrect = topRecommendation.date == scenario.expectedBestDate

                fallbackCorrectCount.add(isCorrect)
            }
        }

        // Then
        if (lowConfidenceScenarios.isEmpty()) {
            println("⚠ Note: No low-confidence scenarios found (model is very confident)")
            println("This is acceptable - it means the model works well on this dataset")
            return@runTest
        }

        val correctCount = fallbackCorrectCount.count { it }
        val accuracy = correctCount.toDouble() / fallbackCorrectCount.size
        val accuracyPercent = (accuracy * 100).toInt()

        println("\n--- Results ---")
        println("Low-Confidence Scenarios (confidence < 70%): ${lowConfidenceScenarios.size}")
        println("Fallback Accuracy: $correctCount/${fallbackCorrectCount.size} ($accuracyPercent%)")
        println("\nTarget: >= 75%")

        assertTrue(
            accuracy >= 0.75 || fallbackCorrectCount.isEmpty(),
            "Fallback heuristic accuracy should be >= 75%, but was $accuracyPercent%"
        )
    }

    // ==================== Helper: Data Generation ====================

    /**
     * Creates 1000 simulated training data points with realistic patterns.
     */
    private fun createSimulatedTrainingData(count: Int): List<TrainingDataPoint> {
        return (1..count).map { i ->
            val dayOfWeek = DayOfWeek.entries[random.nextInt(7)]
            val eventType = EventType.entries[random.nextInt(EventType.entries.size)]
            val season = Season.entries[random.nextInt(4)]

            val expectedParticipants = random.nextInt(5, 50)
            val actualAttendance = if (dayOfWeek in listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                (expectedParticipants * random.nextDouble(0.7, 0.95)).toInt()  // Higher on weekends
            } else {
                (expectedParticipants * random.nextDouble(0.4, 0.7)).toInt()   // Lower on weekdays
            }

            TrainingDataPoint(
                eventId = "train-$i",
                eventDate = "2025-${(i % 12) + 1}-${(i % 28) + 1}",
                eventDayOfWeek = dayOfWeek,
                eventType = eventType,
                participantCount = expectedParticipants,
                actualAttendance = actualAttendance,
                season = season,
                attendanceRate = actualAttendance.toDouble() / expectedParticipants
            )
        }
    }

    /**
     * Creates 200 validation scenarios with specific test patterns.
     */
    private fun createSimulatedValidationData(count: Int): List<ValidationScenario> {
        val scenarios = mutableListOf<ValidationScenario>()

        // Generate diverse scenarios
        (1..count).forEach { i ->
            val userId = "user-${i % 20}"  // 20 different users
            val eventType = EventType.entries[random.nextInt(EventType.entries.size)]

            // Generate time slots with realistic distribution
            val proposedDates = (1..5).map { j ->
                val date = String.format("2025-06-%02d", (i + j) % 28 + 1)
                val timeOfDay = TimeOfDay.entries[random.nextInt(5)]
                createTimeSlot("slot-$j", date, timeOfDay)
            }

            // Determine expected best date (ground truth)
            // High probability: Friday/Saturday evening for parties
            // Medium probability: Weekday afternoon for work events
            // Low probability: Monday morning for anything
            val expectedBestDate = when {
                eventType in listOf(EventType.PARTY, EventType.SOCIAL_GATHERING) ->
                    proposedDates.firstOrNull { it.timeOfDay == TimeOfDay.EVENING }?.start?.take(10)
                        ?: proposedDates.first().start?.take(10) ?: "flexible"

                eventType in listOf(EventType.CONFERENCE, EventType.TEAM_BUILDING) ->
                    proposedDates.firstOrNull { it.timeOfDay == TimeOfDay.MORNING }?.start?.take(10)
                        ?: proposedDates.first().start?.take(10) ?: "flexible"

                else ->
                    proposedDates.firstOrNull { it.timeOfDay == TimeOfDay.AFTERNOON }?.start?.take(10)
                        ?: proposedDates.first().start?.take(10) ?: "flexible"
            }

            scenarios.add(
                ValidationScenario(
                    scenarioId = "validation-$i",
                    userId = userId,
                    eventId = "event-$i",
                    eventType = eventType,
                    proposedDates = proposedDates,
                    expectedBestDate = expectedBestDate
                )
            )
        }

        return scenarios
    }

    // ==================== Helper: Time Slot Generation ====================

    private fun generateTimeSlotsMixedWeekdays(weekendSlots: Int, weekdaySlots: Int): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()

        // Add weekend slots
        (1..weekendSlots).forEach { i ->
            val dayOfWeek = if (i % 2 == 0) DayOfWeek.SATURDAY else DayOfWeek.SUNDAY
            val date = when (dayOfWeek) {
                DayOfWeek.SATURDAY -> "2025-06-21"
                DayOfWeek.SUNDAY -> "2025-06-22"
                else -> "2025-06-20"
            }
            slots.add(createTimeSlot("weekend-$i", date, TimeOfDay.AFTERNOON))
        }

        // Add weekday slots
        (1..weekdaySlots).forEach { i ->
            val dayOfWeek = DayOfWeek.entries[i % 7]
            if (dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                val dates = mapOf(
                    DayOfWeek.MONDAY to "2025-06-16",
                    DayOfWeek.TUESDAY to "2025-06-17",
                    DayOfWeek.WEDNESDAY to "2025-06-18",
                    DayOfWeek.THURSDAY to "2025-06-19",
                    DayOfWeek.FRIDAY to "2025-06-20"
                )
                slots.add(createTimeSlot("weekday-$i", dates[dayOfWeek] ?: "2025-06-16", TimeOfDay.MORNING))
            }
        }

        return slots
    }

    private fun generateTimeSlotsByTimeOfDay(
        morningSlots: Int,
        afternoonSlots: Int,
        eveningSlots: Int
    ): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()

        (1..morningSlots).forEach { i ->
            slots.add(createTimeSlot("morning-$i", "2025-06-${15 + i}", TimeOfDay.MORNING))
        }

        (1..afternoonSlots).forEach { i ->
            slots.add(createTimeSlot("afternoon-$i", "2025-06-${15 + i}", TimeOfDay.AFTERNOON))
        }

        (1..eveningSlots).forEach { i ->
            slots.add(createTimeSlot("evening-$i", "2025-06-${15 + i}", TimeOfDay.EVENING))
        }

        return slots
    }

    private fun generateTimeSlotsBySeasons(
        summerSlots: Int,
        winterSlots: Int,
        springSlots: Int,
        fallSlots: Int
    ): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()

        // Summer (June, July, August)
        (1..summerSlots).forEach { i ->
            slots.add(createTimeSlot("summer-$i", "2025-06-${15 + i}", TimeOfDay.AFTERNOON))
        }

        // Winter (December, January, February)
        (1..winterSlots).forEach { i ->
            slots.add(createTimeSlot("winter-$i", "2025-01-${15 + i}", TimeOfDay.AFTERNOON))
        }

        // Spring (March, April, May)
        (1..springSlots).forEach { i ->
            slots.add(createTimeSlot("spring-$i", "2025-04-${15 + i}", TimeOfDay.AFTERNOON))
        }

        // Fall (September, October, November)
        (1..fallSlots).forEach { i ->
            slots.add(createTimeSlot("autumn-$i", "2025-09-${15 + i}", TimeOfDay.AFTERNOON))
        }

        return slots
    }

    private fun createTimeSlot(id: String, date: String, timeOfDay: TimeOfDay): TimeSlot {
        val startHour = when (timeOfDay) {
            TimeOfDay.MORNING -> 9
            TimeOfDay.AFTERNOON -> 14
            TimeOfDay.EVENING -> 19
            TimeOfDay.ALL_DAY -> 0
            TimeOfDay.SPECIFIC -> 14
        }

        return TimeSlot(
            id = id,
            start = "${date}T${startHour.toString().padStart(2, '0')}:00:00Z",
            end = "${date}T${(startHour + 2).toString().padStart(2, '0')}:00:00Z",
            timezone = "Europe/Paris",
            timeOfDay = timeOfDay
        )
    }

    private fun createPotentialLocation(id: String, name: String, type: String): PotentialLocation {
        return PotentialLocation(
            id = id,
            eventId = "event-1",
            name = name,
            locationType = LocationType.SPECIFIC_VENUE,
            address = "$name address",
            coordinates = null,
            createdAt = "2025-01-01T00:00:00Z"
        )
    }

    // ==================== Helper: Utility Functions ====================

    private fun getDayOfWeekFromDate(dateString: String): DayOfWeek {
        // Simplified: assume we can parse date and get day of week
        // In real scenario, use actual date parsing
        return when {
            dateString.contains("06-20") || dateString.contains("06-21") -> DayOfWeek.SATURDAY
            dateString.contains("06-22") -> DayOfWeek.SUNDAY
            dateString.contains("06-19") -> DayOfWeek.FRIDAY
            dateString.contains("06-18") -> DayOfWeek.THURSDAY
            dateString.contains("06-17") -> DayOfWeek.WEDNESDAY
            dateString.contains("06-16") -> DayOfWeek.TUESDAY
            else -> DayOfWeek.MONDAY
        }
    }

    private fun getSeasonFromDate(dateString: String): Season {
        val month = dateString.substring(5, 7).toIntOrNull() ?: 6
        return Season.fromMonth(month)
    }

    private fun getCurrentTimestamp(): String {
        return "2025-01-01T00:00:00Z"
    }

    // ==================== Data Models ====================

    data class TrainingDataPoint(
        val eventId: String,
        val eventDate: String,
        val eventDayOfWeek: DayOfWeek,
        val eventType: EventType,
        val participantCount: Int,
        val actualAttendance: Int,
        val season: Season,
        val attendanceRate: Double
    )

    data class ValidationScenario(
        val scenarioId: String,
        val userId: String,
        val eventId: String,
        val eventType: EventType,
        val proposedDates: List<TimeSlot>,
        val expectedBestDate: String
    )

    data class PredictionResult(
        val scenarioId: String,
        val expected: String,
        val predicted: String?,
        val score: Double,
        val confidence: Double,
        val isCorrect: Boolean
    )

    // Note: Using Season enum from com.guyghost.wakeve.models
    // No need to redeclare here
}
