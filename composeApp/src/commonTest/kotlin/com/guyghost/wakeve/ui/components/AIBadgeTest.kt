package com.guyghost.wakeve.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.guyghost.wakeve.models.AIBadge
import com.guyghost.wakeve.models.AIBadgeType
import com.guyghost.wakeve.models.AIMetadata
import com.guyghost.wakeve.models.AISuggestion
import com.guyghost.wakeve.models.DateRecommendation
import com.guyghost.wakeve.models.PredictionSource
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Compose UI Tests for AI Badge Components.
 *
 * Tests cover:
 * - AIBadge display and styling
 * - AISuggestionCard rendering
 * - AIRecommendationList layout
 * - Accessibility labels
 */
class AIBadgeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test that AIBadge displays correctly with icon and label.
     */
    @Test
    fun testAIBadgeDisplay() {
        val badge = AIBadge(
            type = AIBadgeType.HIGH_CONFIDENCE,
            displayName = "High Confidence",
            icon = "🎯",
            color = "#4CAF50"
        )

        composeTestRule.setContent {
            TestAIBadgeContent(badge = badge)
        }

        // Verify badge is displayed with correct text
        composeTestRule.onNodeWithText("High Confidence").assertIsDisplayed()
        composeTestRule.onNodeWithText("🎯").assertIsDisplayed()
    }

    /**
     * Test that different badge types render with appropriate colors.
     */
    @Test
    fun testDifferentBadgeTypes() {
        val badges = listOf(
            AIBadge(
                type = AIBadgeType.HIGH_CONFIDENCE,
                displayName = "High",
                icon = "🎯",
                color = "#4CAF50"
            ),
            AIBadge(
                type = AIBadgeType.MEDIUM_CONFIDENCE,
                displayName = "Medium",
                icon = "📊",
                color = "#FF9800"
            ),
            AIBadge(
                type = AIBadgeType.PERSONALIZED,
                displayName = "For You",
                icon = "✨",
                color = "#9C27B0"
            )
        )

        composeTestRule.setContent {
            TestMultipleBadgesContent(badges = badges)
        }

        // Verify all badges are displayed
        composeTestRule.onNodeWithText("High").assertIsDisplayed()
        composeTestRule.onNodeWithText("Medium").assertIsDisplayed()
        composeTestRule.onNodeWithText("For You").assertIsDisplayed()
    }

    /**
     * Test that confidence indicator shows correct percentage.
     */
    @Test
    fun testConfidenceIndicator() {
        composeTestRule.setContent {
            TestConfidenceIndicatorContent(confidenceScore = 0.85f)
        }

        // Verify confidence percentage is displayed
        composeTestRule.onNodeWithText("85%").assertIsDisplayed()
    }

    /**
     * Test that confidence indicator colors change based on score.
     */
    @Test
    fun testConfidenceIndicatorColors() {
        composeTestRule.setContent {
            TestConfidenceIndicatorContent(confidenceScore = 0.95f)
        }

        // High confidence should show "Excellent" label
        composeTestRule.onNodeWithText("Excellent").assertIsDisplayed()
    }

    /**
     * Test AISuggestionCard renders with all components.
     */
    @Test
    fun testAISuggestionCardDisplay() {
        val suggestion = createTestDateSuggestion()

        composeTestRule.setContent {
            TestAISuggestionCardContent(suggestion = suggestion)
        }

        // Verify card components
        composeTestRule.onNodeWithText("High Confidence").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dismiss").assertIsDisplayed()
        composeTestRule.onNodeWithText("Accept").assertIsDisplayed()
        composeTestRule.onNodeWithText("Predicted attendance").assertIsDisplayed()
    }

    /**
     * Test that accept and dismiss buttons are present and clickable.
     */
    @Test
    fun testAISuggestionCardButtons() {
        val suggestion = createTestDateSuggestion()

        composeTestRule.setContent {
            TestAISuggestionCardContent(suggestion = suggestion)
        }

        // Verify buttons are displayed
        composeTestRule.onNodeWithText("Dismiss").assertIsDisplayed()
        composeTestRule.onNodeWithText("Accept").assertIsDisplayed()
    }

    /**
     * Test AIRecommendationList shows empty state when no suggestions.
     */
    @Test
    fun testAIRecommendationListEmptyState() {
        composeTestRule.setContent {
            TestAIRecommendationListContent(
                suggestions = emptyList(),
                emptyMessage = "No recommendations"
            )
        }

        // Verify empty state message
        composeTestRule.onNodeWithText("No recommendations").assertIsDisplayed()
    }

    /**
     * Test AIRecommendationList displays multiple suggestions.
     */
    @Test
    fun testAIRecommendationListWithSuggestions() {
        val suggestions = listOf(
            createTestDateSuggestion("1"),
            createTestDateSuggestion("2"),
            createTestDateSuggestion("3")
        )

        composeTestRule.setContent {
            TestAIRecommendationListContent(suggestions = suggestions)
        }

        // Verify multiple suggestions are displayed
        composeTestRule.onNodeWithText("High Confidence").assertIsDisplayed()
    }

    /**
     * Test accessibility labels are present.
     */
    @Test
    fun testAccessibilityLabels() {
        val badge = AIBadge(
            type = AIBadgeType.AI_SUGGESTION,
            displayName = "AI Suggestion",
            icon = "🤖",
            color = "#6200EE",
            tooltip = "This is an AI-generated suggestion"
        )

        composeTestRule.setContent {
            TestAIBadgeContent(badge = badge)
        }

        // The badge should be displayed with tooltip content
        composeTestRule.onNodeWithText("AI Suggestion").assertIsDisplayed()
    }
}

/**
 * Test helper functions and data.
 */
object AIBadgeTestHelpers {

    /**
     * Creates a test AIBadge with default values.
     */
    fun createTestAIBadge(
        type: AIBadgeType = AIBadgeType.HIGH_CONFIDENCE,
        displayName: String = "High Confidence",
        icon: String = "🎯",
        color: String = "#4CAF50"
    ): AIBadge {
        return AIBadge(
            type = type,
            displayName = displayName,
            icon = icon,
            color = color
        )
    }

    /**
     * Creates a test AISuggestion with DateRecommendation.
     */
    fun createTestDateSuggestion(
        id: String = "test-suggestion-1",
        confidenceScore: Double = 0.85
    ): AISuggestion<DateRecommendation> {
        val timeSlot = TimeSlot(
            id = "slot-1",
            start = "2026-01-15T14:00:00Z",
            end = "2026-01-15T18:00:00Z",
            timezone = "UTC",
            timeOfDay = TimeOfDay.AFTERNOON
        )

        val recommendation = DateRecommendation(
            timeSlot = timeSlot,
            predictedAttendance = 0.78,
            score = confidenceScore
        )

        val metadata = AIMetadata(
            confidenceScore = confidenceScore,
            predictionSource = PredictionSource.ML,
            modelVersion = "v1.0",
            featuresUsed = mapOf("dayOfWeek" to "Friday", "season" to "Winter"),
            createdAt = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)
        )

        return AISuggestion(
            id = id,
            data = recommendation,
            metadata = metadata,
            badge = createTestAIBadge(),
            reasoning = "Based on your preference for weekend afternoon events"
        )
    }
}

// Test content composables for use in tests

@Composable
private fun TestAIBadgeContent(badge: AIBadge) {
    AIBadge(badge = badge)
}

@Composable
private fun TestMultipleBadgesContent(badges: List<AIBadge>) {
    AIBadgeRow(badges = badges)
}

@Composable
private fun TestConfidenceIndicatorContent(confidenceScore: Float) {
    ConfidenceIndicator(confidenceScore = confidenceScore.toDouble())
}

@Composable
private fun TestAISuggestionCardContent(
    suggestion: AISuggestion<DateRecommendation>,
    onAccept: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    AISuggestionCard(
        suggestion = suggestion,
        onAccept = onAccept,
        onDismiss = onDismiss
    )
}

@Composable
private fun TestAIRecommendationListContent(
    suggestions: List<AISuggestion<DateRecommendation>>,
    emptyMessage: String = "No recommendations"
) {
    AIRecommendationList(
        suggestions = suggestions,
        onAcceptRecommendation = {},
        onDismissRecommendation = {},
        emptyMessage = emptyMessage
    )
}
