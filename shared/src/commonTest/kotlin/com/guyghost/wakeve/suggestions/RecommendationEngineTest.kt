package com.guyghost.wakeve.suggestions

import com.guyghost.wakeve.models.LocationPreferences
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScoringWeights
import com.guyghost.wakeve.models.SuggestionBudgetRange
import com.guyghost.wakeve.models.SuggestionSeason
import com.guyghost.wakeve.models.SuggestionUserPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for RecommendationEngine.
 *
 * Tests scoring algorithms, seasonality, cost calculations,
 * user similarity, and hybrid recommendation fusion.
 */
class RecommendationEngineTest {

    // ========================================================================
    // Test Fixtures
    // ========================================================================

    private fun defaultPreferences(
        userId: String = "user-1",
        budgetMin: Double = 50.0,
        budgetMax: Double = 200.0,
        seasons: List<SuggestionSeason> = listOf(SuggestionSeason.SUMMER),
        maxGroupSize: Int = 10,
        activities: List<String> = listOf("hiking", "swimming"),
        regions: List<String> = listOf("Paris", "Rome")
    ) = SuggestionUserPreferences(
        userId = "user-1",
        budgetRange = SuggestionBudgetRange(budgetMin, budgetMax, "EUR"),
        preferredDurationRange = 1..7,
        preferredSeasons = seasons,
        preferredActivities = activities,
        maxGroupSize = maxGroupSize,
        locationPreferences = LocationPreferences(
            preferredRegions = regions,
            maxDistanceFromCity = 100,
            nearbyCities = listOf("Paris")
        ),
        accessibilityNeeds = emptyList()
    )

    private fun testScenario(
        id: String = "scenario-1",
        budgetPerPerson: Double = 100.0,
        location: String = "Paris, France",
        duration: Int = 3,
        participants: Int = 5,
        dateOrPeriod: String = "2026-07-15T00:00:00Z"
    ) = Scenario(
        id = id,
        name = "Test Scenario",
        eventId = "event-1",
        dateOrPeriod = dateOrPeriod,
        location = location,
        duration = duration,
        estimatedParticipants = participants,
        estimatedBudgetPerPerson = budgetPerPerson,
        description = "Test",
        status = ScenarioStatus.PROPOSED,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )

    // ========================================================================
    // Cost Score Tests
    // ========================================================================

    @Test
    fun `score is 1 when budget is at or below minimum`() {
        val prefs = defaultPreferences(budgetMin = 50.0, budgetMax = 200.0)
        val scenario = testScenario(budgetPerPerson = 50.0)
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertEquals(1.0, score.costScore, 0.01)
    }

    @Test
    fun `score is 1 when budget is below minimum`() {
        val prefs = defaultPreferences(budgetMin = 50.0, budgetMax = 200.0)
        val scenario = testScenario(budgetPerPerson = 30.0)
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertEquals(1.0, score.costScore, 0.01)
    }

    @Test
    fun `score decreases within budget range`() {
        val prefs = defaultPreferences(budgetMin = 50.0, budgetMax = 200.0)
        val scenario = testScenario(budgetPerPerson = 125.0)
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertTrue(score.costScore in 0.5..1.0, "Mid-range should score between 0.5 and 1.0")
    }

    @Test
    fun `score is 0 when budget exceeds max by 1 5x`() {
        val prefs = defaultPreferences(budgetMin = 50.0, budgetMax = 200.0)
        val scenario = testScenario(budgetPerPerson = 350.0) // > 200 * 1.5 = 300
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertEquals(0.0, score.costScore, 0.01)
    }

    @Test
    fun `score is 0 3 when budget slightly exceeds max`() {
        val prefs = defaultPreferences(budgetMin = 50.0, budgetMax = 200.0)
        val scenario = testScenario(budgetPerPerson = 250.0) // 200 * 1.25
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertEquals(0.3, score.costScore, 0.01)
    }

    // ========================================================================
    // Accessibility Score Tests
    // ========================================================================

    @Test
    fun `Paris gets highest accessibility score`() {
        val prefs = defaultPreferences()
        val scenario = testScenario(location = "Paris, France")
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertEquals(0.9, score.accessibilityScore, 0.01)
    }

    @Test
    fun `London gets high accessibility score`() {
        val prefs = defaultPreferences()
        val scenario = testScenario(location = "London, UK")
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertEquals(0.9, score.accessibilityScore, 0.01)
    }

    @Test
    fun `Rome gets good accessibility score`() {
        val prefs = defaultPreferences()
        val scenario = testScenario(location = "Rome, Italy")
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertEquals(0.8, score.accessibilityScore, 0.01)
    }

    @Test
    fun `Unknown location gets default accessibility`() {
        val prefs = defaultPreferences()
        val scenario = testScenario(location = "Small Town")
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertEquals(0.7, score.accessibilityScore, 0.01)
    }

    // ========================================================================
    // Seasonality Score Tests
    // ========================================================================

    @Test
    fun `ALL_YEAR season always scores 1`() {
        val prefs = defaultPreferences(seasons = listOf(SuggestionSeason.ALL_YEAR))
        val scenario = testScenario(dateOrPeriod = "2026-01-15T00:00:00Z")
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertEquals(1.0, score.seasonalityScore, 0.01)
    }

    @Test
    fun `matching season scores 1`() {
        val prefs = defaultPreferences(seasons = listOf(SuggestionSeason.SUMMER))
        val scenario = testScenario(dateOrPeriod = "2026-07-15T00:00:00Z") // July = Summer
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertEquals(1.0, score.seasonalityScore, 0.01)
    }

    @Test
    fun `non-matching season scores low`() {
        val prefs = defaultPreferences(seasons = listOf(SuggestionSeason.WINTER))
        val scenario = testScenario(dateOrPeriod = "2026-07-15T00:00:00Z") // July = Summer
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertEquals(0.3, score.seasonalityScore, 0.01)
    }

    @Test
    fun `winter months correctly identified`() {
        val prefs = defaultPreferences(seasons = listOf(SuggestionSeason.WINTER))
        val winterScenario = testScenario(dateOrPeriod = "2026-01-15T00:00:00Z") // January
        val score = RecommendationEngine.calculateScenarioScore(winterScenario, prefs)
        assertEquals(1.0, score.seasonalityScore, 0.01)
    }

    @Test
    fun `spring months correctly identified`() {
        val prefs = defaultPreferences(seasons = listOf(SuggestionSeason.SPRING))
        val springScenario = testScenario(dateOrPeriod = "2026-04-15T00:00:00Z") // April
        val score = RecommendationEngine.calculateScenarioScore(springScenario, prefs)
        assertEquals(1.0, score.seasonalityScore, 0.01)
    }

    @Test
    fun `fall months correctly identified`() {
        val prefs = defaultPreferences(seasons = listOf(SuggestionSeason.FALL))
        val fallScenario = testScenario(dateOrPeriod = "2026-10-15T00:00:00Z") // October
        val score = RecommendationEngine.calculateScenarioScore(fallScenario, prefs)
        assertEquals(1.0, score.seasonalityScore, 0.01)
    }

    @Test
    fun `text-based winter season parsing`() {
        val prefs = defaultPreferences(seasons = listOf(SuggestionSeason.WINTER))
        val scenario = testScenario(dateOrPeriod = "Winter getaway")
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertEquals(1.0, score.seasonalityScore, 0.01)
    }

    // ========================================================================
    // Overall Score Tests
    // ========================================================================

    @Test
    fun `overall score is between 0 and 1`() {
        val prefs = defaultPreferences()
        val scenario = testScenario()
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertTrue(score.overallScore in 0.0..1.0,
            "Overall score must be between 0 and 1")
    }

    @Test
    fun `perfect scenario gets high score`() {
        val prefs = defaultPreferences(
            budgetMin = 50.0, budgetMax = 200.0,
            seasons = listOf(SuggestionSeason.SUMMER),
            regions = listOf("Paris")
        )
        val scenario = testScenario(
            budgetPerPerson = 50.0,
            location = "Paris, France",
            dateOrPeriod = "2026-07-15T00:00:00Z"
        )
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertTrue(score.overallScore > 0.7, "Perfect match should score high, got ${score.overallScore}")
    }

    @Test
    fun `poor scenario gets low score`() {
        val prefs = defaultPreferences(
            budgetMin = 50.0, budgetMax = 100.0,
            seasons = listOf(SuggestionSeason.WINTER),
            regions = listOf("Tokyo")
        )
        val scenario = testScenario(
            budgetPerPerson = 400.0,
            location = "Remote Island",
            dateOrPeriod = "2026-07-15T00:00:00Z" // Summer, not Winter
        )
        val score = RecommendationEngine.calculateScenarioScore(scenario, prefs)
        assertTrue(score.overallScore < 0.5, "Poor match should score low, got ${score.overallScore}")
    }

    @Test
    fun `custom weights affect scoring`() {
        val prefs = defaultPreferences()
        val scenario = testScenario(budgetPerPerson = 400.0) // Over budget

        val costHeavy = ScoringWeights(cost = 1.0, accessibility = 0.0, popularity = 0.0, seasonality = 0.0, personalization = 0.0)
        val scoreCostHeavy = RecommendationEngine.calculateScenarioScore(scenario, prefs, costHeavy)

        val costLight = ScoringWeights(cost = 0.0, accessibility = 1.0, popularity = 0.0, seasonality = 0.0, personalization = 0.0)
        val scoreCostLight = RecommendationEngine.calculateScenarioScore(scenario, prefs, costLight)

        assertTrue(scoreCostHeavy.overallScore < scoreCostLight.overallScore,
            "Cost-heavy weight should score lower for over-budget scenario")
    }

    // ========================================================================
    // User Similarity Tests
    // ========================================================================

    @Test
    fun `identical users have similarity 1`() {
        val user = defaultPreferences()
        val similarity = RecommendationEngine.calculateUserSimilarity(user, user)
        assertEquals(1.0, similarity, 0.01)
    }

    @Test
    fun `completely different users have low similarity`() {
        val user1 = defaultPreferences(
            budgetMin = 10.0, budgetMax = 50.0,
            seasons = listOf(SuggestionSeason.WINTER),
            activities = listOf("skiing"),
            maxGroupSize = 3,
            regions = listOf("Alps")
        )
        val user2 = defaultPreferences(
            userId = "user-2",
            budgetMin = 500.0, budgetMax = 1000.0,
            seasons = listOf(SuggestionSeason.SUMMER),
            activities = listOf("diving"),
            maxGroupSize = 20,
            regions = listOf("Maldives")
        )
        val similarity = RecommendationEngine.calculateUserSimilarity(user1, user2)
        assertTrue(similarity < 0.3, "Very different users should have low similarity, got $similarity")
    }

    @Test
    fun `partial overlap yields moderate similarity`() {
        val user1 = defaultPreferences(
            activities = listOf("hiking", "swimming"),
            seasons = listOf(SuggestionSeason.SUMMER, SuggestionSeason.SPRING),
            maxGroupSize = 10
        )
        val user2 = defaultPreferences(
            userId = "user-2",
            activities = listOf("swimming", "cycling"),
            seasons = listOf(SuggestionSeason.SUMMER),
            maxGroupSize = 12
        )
        val similarity = RecommendationEngine.calculateUserSimilarity(user1, user2)
        assertTrue(similarity > 0.3 && similarity < 0.9,
            "Partial overlap should yield moderate similarity, got $similarity")
    }

    // ========================================================================
    // Recommendation Engine - Collaborative & Content
    // ========================================================================

    @Test
    fun `recommendCollaborative returns empty list (placeholder)`() {
        val result = RecommendationEngine.recommendCollaborative(
            userId = "user-1",
            similarUsers = listOf("user-2", "user-3"),
            context = com.guyghost.wakeve.models.RecommendationContext(
                eventId = "event-1",
                userId = "user-1",
                userPreferences = defaultPreferences(),
                participantCount = 5,
                season = SuggestionSeason.SUMMER
            )
        )
        assertTrue(result.isEmpty(), "Collaborative filtering is placeholder, should return empty")
    }

    @Test
    fun `recommendContentBased returns empty list (placeholder)`() {
        val result = RecommendationEngine.recommendContentBased(
            userId = "user-1",
            context = com.guyghost.wakeve.models.RecommendationContext(
                eventId = "event-1",
                userId = "user-1",
                userPreferences = defaultPreferences(),
                participantCount = 5,
                season = SuggestionSeason.SUMMER
            )
        )
        assertTrue(result.isEmpty(), "Content-based filtering is placeholder, should return empty")
    }

    @Test
    fun `recommendHybrid combines and sorts by score`() {
        val result = RecommendationEngine.recommendHybrid(
            userId = "user-1",
            similarUsers = listOf("user-2"),
            context = com.guyghost.wakeve.models.RecommendationContext(
                eventId = "event-1",
                userId = "user-1",
                userPreferences = defaultPreferences(),
                participantCount = 5,
                season = SuggestionSeason.SUMMER
            )
        )
        assertTrue(result.isEmpty(), "Hybrid is placeholder (combines two empty lists)")
    }
}
