package com.guyghost.wakeve.models

import com.guyghost.wakeve.models.AIBadge
import com.guyghost.wakeve.models.AIBadgeType
import com.guyghost.wakeve.models.AIMetadata
import com.guyghost.wakeve.models.AISuggestion
import com.guyghost.wakeve.models.AISuggestionHelper
import com.guyghost.wakeve.models.PredictionSource
import com.guyghost.wakeve.models.ScoreBreakdown
import com.guyghost.wakeve.models.SuggestionCategory
import com.guyghost.wakeve.models.TimeOfDay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AI Suggestion Models.
 * Tests serialization/deserialization, badge calculation, and helper functions.
 */
class AISuggestionModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // ========== Serialization Tests ==========

    @Test
    fun `AIMetadata serialization and deserialization`() {
        val metadata = AIMetadata(
            confidenceScore = 0.85,
            predictionSource = PredictionSource.ML_MODEL,
            modelVersion = "1.2.0",
            featuresUsed = mapOf("eventType" to "BIRTHDAY", "season" to "SUMMER"),
            createdAt = "2026-01-15T10:30:00Z"
        )

        val jsonString = json.encodeToString(metadata)
        val decoded = json.decodeFromString<AIMetadata>(jsonString)

        assertEquals(0.85, decoded.confidenceScore, 0.001)
        assertEquals(PredictionSource.ML_MODEL, decoded.predictionSource)
        assertEquals("1.2.0", decoded.modelVersion)
        assertEquals(2, decoded.featuresUsed.size)
        assertEquals("2026-01-15T10:30:00Z", decoded.createdAt)
    }

    @Test
    fun `AIBadge serialization and deserialization`() {
        val badge = AIBadge(
            type = AIBadgeType.HIGH_CONFIDENCE,
            displayName = "High Confidence",
            icon = "🎯",
            color = "#4CAF50",
            tooltip = "High confidence prediction"
        )

        val jsonString = json.encodeToString(badge)
        val decoded = json.decodeFromString<AIBadge>(jsonString)

        assertEquals(AIBadgeType.HIGH_CONFIDENCE, decoded.type)
        assertEquals("High Confidence", decoded.displayName)
        assertEquals("🎯", decoded.icon)
        assertEquals("#4CAF50", decoded.color)
        assertEquals("High confidence prediction", decoded.tooltip)
    }

    @Test
    fun `AISuggestion serialization and deserialization with generic data`() {
        // Using TimeSlot from Event.kt with start, end, timezone fields
        val timeSlot = TimeSlot(
            id = "slot-1",
            start = "2026-02-14T18:00:00Z",
            end = "2026-02-14T22:00:00Z",
            timezone = "UTC",
            timeOfDay = TimeOfDay.EVENING
        )

        val metadata = AIMetadata(
            confidenceScore = 0.92,
            predictionSource = PredictionSource.ML_MODEL,
            modelVersion = "1.0.0",
            featuresUsed = mapOf("dayOfWeek" to "SATURDAY"),
            createdAt = "2026-01-15T10:30:00Z"
        )

        val badge = AIBadge(
            type = AIBadgeType.HIGH_CONFIDENCE,
            displayName = "High Confidence",
            icon = "🎯",
            color = "#4CAF50"
        )

        val suggestion = AISuggestion(
            id = "suggestion-1",
            data = timeSlot,
            metadata = metadata,
            badge = badge,
            reasoning = "Perfect for a Saturday evening event"
        )

        val jsonString = json.encodeToString(suggestion)
        val decoded = json.decodeFromString<AISuggestion<TimeSlot>>(jsonString)

        assertEquals("suggestion-1", decoded.id)
        assertEquals("slot-1", decoded.data.id)
        assertEquals("2026-02-14T18:00:00Z", decoded.data.start)
        assertEquals(0.92, decoded.metadata.confidenceScore, 0.001)
        assertEquals(AIBadgeType.HIGH_CONFIDENCE, decoded.badge.type)
    }

    // ========== Badge Calculation Tests ==========

    @Test
    fun `calculateAIBadge returns HIGH_CONFIDENCE for score >= 90`() {
        val badge = AISuggestionHelper.calculateAIBadge(0.95)

        assertEquals(AIBadgeType.HIGH_CONFIDENCE, badge.type)
        assertEquals("High Confidence", badge.displayName)
        assertEquals("🎯", badge.icon)
        assertEquals("#4CAF50", badge.color)
    }

    @Test
    fun `calculateAIBadge returns MEDIUM_CONFIDENCE for score 70-90`() {
        val badge = AISuggestionHelper.calculateAIBadge(0.75)

        assertEquals(AIBadgeType.MEDIUM_CONFIDENCE, badge.type)
        assertEquals("Medium Confidence", badge.displayName)
        assertEquals("📊", badge.icon)
        assertEquals("#FF9800", badge.color)
    }

    @Test
    fun `calculateAIBadge returns AI_SUGGESTION for score < 70`() {
        val badge = AISuggestionHelper.calculateAIBadge(0.50)

        assertEquals(AIBadgeType.AI_SUGGESTION, badge.type)
        assertEquals("AI Suggestion", badge.displayName)
        assertEquals("🤖", badge.icon)
        assertEquals("#6200EE", badge.color)
    }

    @Test
    fun `calculateAIBadge returns PERSONALIZED when flagged`() {
        val badge = AISuggestionHelper.calculateAIBadge(
            confidenceScore = 0.60,
            isPersonalized = true
        )

        assertEquals(AIBadgeType.PERSONALIZED, badge.type)
        assertEquals("Personalized for You", badge.displayName)
        assertEquals("⭐", badge.icon)
        assertEquals("#FFD700", badge.color)
    }

    @Test
    fun `calculateAIBadge returns POPULAR_CHOICE when flagged`() {
        val badge = AISuggestionHelper.calculateAIBadge(
            confidenceScore = 0.65,
            isPopular = true
        )

        assertEquals(AIBadgeType.POPULAR_CHOICE, badge.type)
        assertEquals("Popular Choice", badge.displayName)
        assertEquals("🔥", badge.icon)
        assertEquals("#FF5722", badge.color)
    }

    @Test
    fun `calculateAIBadge returns SEASONAL when flagged`() {
        val badge = AISuggestionHelper.calculateAIBadge(
            confidenceScore = 0.55,
            isSeasonal = true
        )

        assertEquals(AIBadgeType.SEASONAL, badge.type)
        assertEquals("Seasonal Pick", badge.displayName)
        assertEquals("🌸", badge.icon)
        assertEquals("#4CAF50", badge.color)
    }

    // ========== Conversion Tests ==========

    @Test
    fun `DateScore toAISuggestion converts correctly`() {
        val dateScore = DateScore(
            date = "2026-02-14",
            score = 0.88,
            confidenceScore = 0.85,
            features = mapOf("eventType" to "BIRTHDAY"),
            breakdown = ScoreBreakdown(
                proximityScore = 0.9,
                typeMatchScore = 0.8,
                seasonalityScore = 0.85,
                socialScore = 0.92
            )
        )

        // Using TimeSlot from Event.kt
        val timeSlot = TimeSlot(
            id = "slot-1",
            start = "2026-02-14T18:00:00Z",
            end = "2026-02-14T22:00:00Z",
            timezone = "UTC",
            timeOfDay = TimeOfDay.EVENING
        )

        val suggestion = dateScore.toAISuggestion(timeSlot)

        assertNotNull(suggestion.id)
        assertEquals("slot-1", suggestion.data.timeSlot.id)
        assertEquals(0.88, suggestion.data.score, 0.001)
        assertEquals(0.85, suggestion.metadata.confidenceScore, 0.001)
        assertEquals(PredictionSource.ML_MODEL, suggestion.metadata.predictionSource)
        assertTrue(suggestion.reasoning?.isNotEmpty() == true)
    }

    @Test
    fun `DateScore toAISuggestion uses HEURISTIC_FALLBACK for low confidence`() {
        val dateScore = DateScore(
            date = "2026-02-14",
            score = 0.50,
            confidenceScore = 0.55,
            features = emptyMap(),
            breakdown = ScoreBreakdown()
        )

        val timeSlot = TimeSlot(
            id = "slot-1",
            start = null,
            end = null,
            timezone = "UTC",
            timeOfDay = TimeOfDay.ALL_DAY
        )

        val suggestion = dateScore.toAISuggestion(timeSlot)

        assertEquals(PredictionSource.HEURISTIC_FALLBACK, suggestion.metadata.predictionSource)
    }

    @Test
    fun `LocationScore toAISuggestion converts correctly`() {
        val locationScore = LocationScore(
            locationId = "loc-1",
            locationName = "Paris",
            score = 0.92,
            confidenceScore = 0.88,
            features = mapOf("region" to "ILE_DE_FRANCE"),
            breakdown = ScoreBreakdown(
                proximityScore = 0.9,
                typeMatchScore = 0.95,
                seasonalityScore = 0.85,
                socialScore = 0.88
            )
        )

        val location = PotentialLocation(
            id = "loc-1",
            eventId = "event-1",
            name = "Paris",
            locationType = LocationType.CITY,
            address = "Paris, France",
            createdAt = "2026-01-01T00:00:00Z"
        )

        val suggestion = locationScore.toAISuggestion(location)

        assertNotNull(suggestion.id)
        assertEquals("loc-1", suggestion.data.location.id)
        assertEquals(0.92, suggestion.data.matchScore, 0.001)
        assertEquals(SuggestionCategory.TRAVEL, suggestion.data.category) // Default for locations
        assertEquals(PredictionSource.ML_MODEL, suggestion.metadata.predictionSource)
    }

    // ========== Summary Generation Tests ==========

    @Test
    fun `generateSummary creates correct statistics`() {
        val suggestions = listOf(
            createMockSuggestion(0.95),
            createMockSuggestion(0.88),
            createMockSuggestion(0.92),
            createMockSuggestion(0.65)
        )

        val summary = AISuggestionHelper.generateSummary(suggestions)

        assertEquals(4, summary.totalRecommendations)
        assertEquals(2, summary.highConfidenceCount) // 0.95 and 0.92
        assertEquals(0.85, summary.averageConfidence, 0.001) // (0.95+0.88+0.92+0.65)/4
    }

    @Test
    fun `generateSummary handles empty list`() {
        val summary = AISuggestionHelper.generateSummary(emptyList())

        assertEquals(0, summary.totalRecommendations)
        assertEquals(0, summary.highConfidenceCount)
        assertEquals(0.0, summary.averageConfidence, 0.001)
        assertEquals("", summary.bestRecommendation)
    }

    // ========== Validation Tests ==========

    @Test(expected = IllegalArgumentException::class)
    fun `AIMetadata throws for confidence score below 0`() {
        AIMetadata(
            confidenceScore = -0.1,
            predictionSource = PredictionSource.ML_MODEL,
            modelVersion = "1.0.0",
            featuresUsed = emptyMap(),
            createdAt = "2026-01-15T10:30:00Z"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `AIMetadata throws for confidence score above 1`() {
        AIMetadata(
            confidenceScore = 1.1,
            predictionSource = PredictionSource.ML_MODEL,
            modelVersion = "1.0.0",
            featuresUsed = emptyMap(),
            createdAt = "2026-01-15T10:30:00Z"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `AIBadge throws for invalid hex color`() {
        AIBadge(
            type = AIBadgeType.AI_SUGGESTION,
            displayName = "Test",
            icon = "🤖",
            color = "invalid-color"
        )
    }

    // ========== Helper Functions ==========

    private fun createMockSuggestion(confidenceScore: Double): AISuggestion<TimeSlot> {
        val metadata = AIMetadata(
            confidenceScore = confidenceScore,
            predictionSource = PredictionSource.ML_MODEL,
            modelVersion = "1.0.0",
            featuresUsed = emptyMap(),
            createdAt = "2026-01-15T10:30:00Z"
        )

        val badge = AISuggestionHelper.calculateAIBadge(confidenceScore)

        return AISuggestion(
            id = "test-${confidenceScore}",
            data = TimeSlot(
                id = "slot-${confidenceScore}",
                start = null,
                end = null,
                timezone = "UTC",
                timeOfDay = TimeOfDay.SPECIFIC
            ),
            metadata = metadata,
            badge = badge
        )
    }
}
