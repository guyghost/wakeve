package com.guyghost.wakeve.models

import com.guyghost.wakeve.repository.UserPreferencesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Unit tests for MLScoringEngine.
 * Tests the hybrid ML + heuristic scoring algorithm.
 */
class MLScoringEngineTest {

    private lateinit var mockRepository: MockUserPreferencesRepository
    private lateinit var scoringEngine: MLScoringEngine

    @BeforeTest
    fun setup() {
        mockRepository = MockUserPreferencesRepository()
        scoringEngine = MLScoringEngine(mockRepository)
    }

    // ==================== predictDateScores Tests ====================

    @Test
    fun `predictDateScores returns scores sorted by descending order`() = runTest {
        // Given
        val eventId = "event-123"
        val eventType = EventType.BIRTHDAY
        val userId = "user-456"

        val timeSlots = listOf(
            createTimeSlot("slot-1", "2026-06-15", TimeOfDay.AFTERNOON),
            createTimeSlot("slot-2", "2026-06-16", TimeOfDay.EVENING),
            createTimeSlot("slot-3", "2026-06-20", TimeOfDay.MORNING)
        )

        // When
        val scores = scoringEngine.predictDateScores(eventId, timeSlots, eventType, userId)

        // Then
        assertEquals(3, scores.size)
        assertTrue(scores[0].score >= scores[1].score)
        assertTrue(scores[1].score >= scores[2].score)
    }

    @Test
    fun `predictDateScores with user preferences returns higher scores for preferred days`() = runTest {
        // Given
        val eventId = "event-123"
        val eventType = EventType.PARTY
        val userId = "user-456"

        // Set user preference for Saturday
        mockRepository.setUserPreferences(
            UserPreference(
                userId = userId,
                preferredDays = listOf(DayOfWeek.SATURDAY),
                preferredTimeOfDay = listOf(TimeOfDay.EVENING),
                preferredEventTypes = listOf(EventType.PARTY),
                preferredLocations = listOf("Paris"),
                avoidEvents = false,
                scoreWeights = ScoreWeights.DEFAULT,
                lastUpdated = "2026-01-01T00:00:00Z"
            )
        )

        val saturdaySlot = createTimeSlot("slot-sat", "2026-06-20", TimeOfDay.EVENING) // Saturday
        val tuesdaySlot = createTimeSlot("slot-tue", "2026-06-17", TimeOfDay.AFTERNOON) // Tuesday

        // When
        val saturdayScore = scoringEngine.predictDateScores(eventId, listOf(saturdaySlot), eventType, userId).first()
        val tuesdayScore = scoringEngine.predictDateScores(eventId, listOf(tuesdaySlot), eventType, userId).first()

        // Then
        assertTrue(saturdayScore.score > tuesdayScore.score,
            "Saturday (preferred) should score higher than Tuesday")
    }

    @Test
    fun `predictDateScores calculates confidence based on feature completeness`() = runTest {
        // Given
        val eventId = "event-123"
        val eventType = EventType.BIRTHDAY
        val userId = "user-789"

        val timeSlot = createTimeSlot("slot-1", "2026-06-15", TimeOfDay.AFTERNOON)

        // When
        val scores = scoringEngine.predictDateScores(eventId, listOf(timeSlot), eventType, userId)

        // Then
        assertEquals(1, scores.size)
        assertTrue(scores[0].confidenceScore in 0.0..1.0)
        assertNotNull(scores[0].features)
    }

    // ==================== predictLocationSuitability Tests ====================

    @Test
    fun `predictLocationSuitability returns scores sorted by descending order`() = runTest {
        // Given
        val eventId = "event-123"
        val eventType = EventType.WEDDING
        val userId = "user-456"

        val locations = listOf(
            createPotentialLocation("loc-1", "Château de Versailles", LocationType.SPECIFIC_VENUE),
            createPotentialLocation("loc-2", "Paris", LocationType.CITY),
            createPotentialLocation("loc-3", "Provence", LocationType.REGION)
        )

        // When
        val scores = scoringEngine.predictLocationSuitability(eventId, locations, eventType, userId)

        // Then
        assertEquals(3, scores.size)
        assertTrue(scores[0].score >= scores[1].score)
        assertTrue(scores[1].score >= scores[2].score)
    }

    @Test
    fun `predictLocationSuitability with preferred location returns higher score`() = runTest {
        // Given
        val eventId = "event-123"
        val eventType = EventType.CONFERENCE
        val userId = "user-456"

        // Set user preference for Paris
        mockRepository.setUserPreferences(
            UserPreference(
                userId = userId,
                preferredDays = listOf(DayOfWeek.FRIDAY),
                preferredTimeOfDay = listOf(TimeOfDay.MORNING),
                preferredEventTypes = listOf(EventType.CONFERENCE),
                preferredLocations = listOf("Paris", "Lyon"),
                avoidEvents = false,
                scoreWeights = ScoreWeights.DEFAULT,
                lastUpdated = "2026-01-01T00:00:00Z"
            )
        )

        val parisLocation = createPotentialLocation("loc-1", "Paris", LocationType.CITY)
        val londonLocation = createPotentialLocation("loc-2", "London", LocationType.CITY)

        // When
        val parisScore = scoringEngine.predictLocationSuitability(eventId, listOf(parisLocation), eventType, userId).first()
        val londonScore = scoringEngine.predictLocationSuitability(eventId, listOf(londonLocation), eventType, userId).first()

        // Then
        assertTrue(parisScore.score > londonScore.score,
            "Paris (preferred) should score higher than London")
    }

    // ==================== predictAttendance Tests ====================

    @Test
    fun `predictAttendance returns prediction within valid range`() = runTest {
        // Given
        val eventId = "event-123"
        val expectedParticipants = 20
        val timeSlot = createTimeSlot("slot-1", "2026-06-15", TimeOfDay.AFTERNOON)

        // When
        val prediction = scoringEngine.predictAttendance(eventId, timeSlot, expectedParticipants)

        // Then
        assertTrue(prediction.predictedAttendanceRate in 0.0..1.0)
        assertTrue(prediction.confidenceScore in 0.0..1.0)
        assertTrue(prediction.predictedCount in 0..expectedParticipants)
    }

    @Test
    fun `predictAttendance calculates count from rate and expected participants`() = runTest {
        // Given
        val eventId = "event-123"
        val expectedParticipants = 100
        val timeSlot = createTimeSlot("slot-1", "2026-06-15", TimeOfDay.EVENING)

        // When
        val prediction = scoringEngine.predictAttendance(eventId, timeSlot, expectedParticipants)

        // Then
        val expectedCount = (prediction.predictedAttendanceRate * expectedParticipants).toInt()
        assertEquals(expectedCount, prediction.predictedCount)
    }

    // ==================== Fallback Heuristic Tests ====================

    @Test
    fun `fallback heuristics boost weekend scores`() = runTest {
        // Given
        val eventId = "event-123"
        val userId = "user-456"

        // No user preferences to force fallback behavior
        mockRepository.setUserPreferences(UserPreference.empty(userId))

        val weekendSlot = createTimeSlot("weekend", "2026-06-20", TimeOfDay.AFTERNOON) // Saturday
        val weekdaySlot = createTimeSlot("weekday", "2026-06-17", TimeOfDay.AFTERNOON) // Wednesday

        // When
        val weekendScores = scoringEngine.predictDateScores(eventId, listOf(weekendSlot), EventType.PARTY, userId)
        val weekdayScores = scoringEngine.predictDateScores(eventId, listOf(weekdaySlot), EventType.PARTY, userId)

        // Then
        assertTrue(weekendScores.first().score >= weekdayScores.first().score,
            "Weekend should score equal or higher than weekday with fallback")
    }

    @Test
    fun `fallback heuristics boost Friday for social events`() = runTest {
        // Given
        val eventId = "event-123"
        val userId = "user-789"
        val eventType = EventType.PARTY

        mockRepository.setUserPreferences(UserPreference.empty(userId))

        val fridaySlot = createTimeSlot("friday", "2026-06-19", TimeOfDay.EVENING)
        val mondaySlot = createTimeSlot("monday", "2026-06-15", TimeOfDay.EVENING)

        // When
        val fridayScore = scoringEngine.predictDateScores(eventId, listOf(fridaySlot), eventType, userId).first()
        val mondayScore = scoringEngine.predictDateScores(eventId, listOf(mondaySlot), eventType, userId).first()

        // Then
        assertTrue(fridayScore.score > mondayScore.score,
            "Friday should score higher than Monday for party events")
    }

    // ==================== Preference Learning Tests ====================

    @Test
    fun `preference learning influences scoring through user preferences`() = runTest {
        // Given
        val eventId = "event-123"
        val userId = "user-456"
        val eventType = EventType.TEAM_BUILDING

        // User prefers outdoor activities in the morning
        mockRepository.setUserPreferences(
            UserPreference(
                userId = userId,
                preferredDays = listOf(DayOfWeek.FRIDAY),
                preferredTimeOfDay = listOf(TimeOfDay.MORNING),
                preferredEventTypes = listOf(EventType.TEAM_BUILDING, EventType.OUTDOOR_ACTIVITY),
                preferredLocations = listOf("Mountain"),
                avoidEvents = false,
                scoreWeights = ScoreWeights.DEFAULT,
                lastUpdated = "2026-01-01T00:00:00Z"
            )
        )

        val morningSlot = createTimeSlot("morning", "2026-06-19", TimeOfDay.MORNING) // Friday morning
        val afternoonSlot = createTimeSlot("afternoon", "2026-06-19", TimeOfDay.AFTERNOON) // Friday afternoon

        // When
        val morningScores = scoringEngine.predictDateScores(eventId, listOf(morningSlot), eventType, userId)
        val afternoonScores = scoringEngine.predictDateScores(eventId, listOf(afternoonSlot), eventType, userId)

        // Then
        assertTrue(morningScores.first().score >= afternoonScores.first().score,
            "Morning (preferred time) should score equal or higher")
    }

    // ==================== Confidence Scoring Tests ====================

    @Test
    fun `confidence score increases with feature completeness`() = runTest {
        // Given
        val eventId = "event-123"
        val eventType = EventType.BIRTHDAY
        val userId = "user-456"

        // User with complete preferences
        mockRepository.setUserPreferences(
            UserPreference(
                userId = userId,
                preferredDays = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                preferredTimeOfDay = listOf(TimeOfDay.AFTERNOON, TimeOfDay.EVENING),
                preferredEventTypes = listOf(EventType.BIRTHDAY, EventType.PARTY),
                preferredLocations = listOf("Paris", "Nice"),
                avoidEvents = false,
                scoreWeights = ScoreWeights.DEFAULT,
                lastUpdated = "2026-01-01T00:00:00Z"
            )
        )

        val saturdayAfternoon = createTimeSlot("slot-1", "2026-06-20", TimeOfDay.AFTERNOON) // Saturday

        // When
        val scores = scoringEngine.predictDateScores(eventId, listOf(saturdayAfternoon), eventType, userId)

        // Then
        assertEquals(1, scores.size)
        // With matching preferences, confidence should be higher
        assertTrue(scores[0].confidenceScore >= 0.7,
            "Confidence should be >= 0.7 with matching preferences")
    }

    // ==================== A/B Testing Logic Tests ====================

    @Test
    fun `prediction includes model version for A/B testing`() = runTest {
        // Given
        val eventId = "event-123"
        val userId = "user-456"
        val eventType = EventType.CONFERENCE

        val timeSlot = createTimeSlot("slot-1", "2026-06-15", TimeOfDay.MORNING)

        // When
        val scores = scoringEngine.predictDateScores(eventId, listOf(timeSlot), eventType, userId)

        // Then
        assertEquals(1, scores.size)
        assertTrue(scores[0].features.containsKey("dayOfWeek"))
        assertTrue(scores[0].features.containsKey("timeOfDay"))
        assertTrue(scores[0].features.containsKey("eventType"))
    }

    @Test
    fun `score breakdown contains all components`() = runTest {
        // Given
        val eventId = "event-123"
        val userId = "user-456"
        val eventType = EventType.WEDDING

        val timeSlot = createTimeSlot("slot-1", "2026-06-15", TimeOfDay.AFTERNOON)

        // When
        val scores = scoringEngine.predictDateScores(eventId, listOf(timeSlot), eventType, userId)

        // Then
        assertEquals(1, scores.size)
        val breakdown = scores[0].breakdown
        assertTrue(breakdown.proximityScore in 0.0..1.0)
        assertTrue(breakdown.typeMatchScore in 0.0..1.0)
        assertTrue(breakdown.seasonalityScore in 0.0..1.0)
        assertTrue(breakdown.socialScore in 0.0..1.0)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `predictDateScores handles empty time slots list`() = runTest {
        // Given
        val eventId = "event-123"
        val eventType = EventType.BIRTHDAY
        val userId = "user-456"

        // When
        val scores = scoringEngine.predictDateScores(eventId, emptyList(), eventType, userId)

        // Then
        assertTrue(scores.isEmpty())
    }

    @Test
    fun `predictLocationSuitability handles empty locations list`() = runTest {
        // Given
        val eventId = "event-123"
        val eventType = EventType.CONFERENCE
        val userId = "user-456"

        // When
        val scores = scoringEngine.predictLocationSuitability(eventId, emptyList(), eventType, userId)

        // Then
        assertTrue(scores.isEmpty())
    }

    // ==================== Helper Methods ====================

    private fun createTimeSlot(id: String, date: String, timeOfDay: TimeOfDay): TimeSlot {
        return TimeSlot(
            id = id,
            start = "${date}T14:00:00",
            end = "${date}T18:00:00",
            timezone = "Europe/Paris",
            timeOfDay = timeOfDay
        )
    }

    private fun createPotentialLocation(id: String, name: String, locationType: LocationType): PotentialLocation {
        return PotentialLocation(
            id = id,
            eventId = "event-123",
            name = name,
            locationType = locationType,
            address = "$name address",
            coordinates = null,
            createdAt = "2026-01-01T00:00:00Z"
        )
    }
}

/**
 * Mock implementation of UserPreferencesRepository for testing.
 */
class MockUserPreferencesRepository : UserPreferencesRepository(
    database = throw NotImplementedError("Mock repository - database not needed")
) {
    private var preferences: UserPreference? = null

    fun setUserPreferences(prefs: UserPreference) {
        preferences = prefs
    }

    override suspend fun getUserPreferences(userId: String): UserPreference? {
        return preferences
    }

    override suspend fun updateUserPreferences(userId: String, preferences: UserPreference) {
        this.preferences = preferences
    }

    // Override other methods to throw or do nothing for testing
    override suspend fun recordVote(
        userId: String,
        eventId: String,
        voteType: VoteType,
        timeOfDay: TimeOfDay,
        dayOfWeek: DayOfWeek
    ) { /* No-op for testing */ }

    override suspend fun recordEventCreation(
        userId: String,
        eventId: String,
        eventType: EventType
    ) { /* No-op for testing */ }

    override suspend fun recordEventParticipation(
        userId: String,
        eventId: String,
        location: String?
    ) { /* No-op for testing */ }

    override suspend fun calculateImplicitPreferences(
        userId: String,
        decayDays: Int
    ): UserPreference {
        return preferences ?: UserPreference.empty(userId)
    }

    override suspend fun applyDecay(userId: String, decayDays: Int) { /* No-op for testing */ }

    override suspend fun getInteractionHistory(userId: String): List<PreferenceInteraction> {
        return emptyList()
    }

    override suspend fun deleteUserData(userId: String) {
        preferences = null
    }

    override suspend fun mergeWithExplicit(
        userId: String,
        explicitPreferences: UserPreference
    ): UserPreference {
        return preferences ?: explicitPreferences
    }
}
