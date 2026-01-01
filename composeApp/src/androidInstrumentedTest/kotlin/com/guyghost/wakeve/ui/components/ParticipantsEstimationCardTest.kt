package com.guyghost.wakeve.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Compose tests for ParticipantsEstimationCard component.
 */
class ParticipantsEstimationCardTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun participantsCard_displaysAllThreeFields() {
        // When
        composeTestRule.setContent {
            ParticipantsEstimationCard(
                minParticipants = null,
                maxParticipants = null,
                expectedParticipants = null,
                onMinChanged = {},
                onMaxChanged = {},
                onExpectedChanged = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Minimum Participants").assertExists()
        composeTestRule.onNodeWithText("Maximum Participants").assertExists()
        composeTestRule.onNodeWithText("Expected Participants").assertExists()
    }
    
    @Test
    fun participantsCard_displaysTitle() {
        // When
        composeTestRule.setContent {
            ParticipantsEstimationCard(
                minParticipants = null,
                maxParticipants = null,
                expectedParticipants = null,
                onMinChanged = {},
                onMaxChanged = {},
                onExpectedChanged = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Participants Estimation")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun participantsCard_showsValuesWhenProvided() {
        // Given
        val min = 5
        val max = 50
        val expected = 20
        
        // When
        composeTestRule.setContent {
            ParticipantsEstimationCard(
                minParticipants = min,
                maxParticipants = max,
                expectedParticipants = expected,
                onMinChanged = {},
                onMaxChanged = {},
                onExpectedChanged = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("5").assertExists()
        composeTestRule.onNodeWithText("50").assertExists()
        composeTestRule.onNodeWithText("20").assertExists()
    }
    
    @Test
    fun participantsCard_callsMinCallbackWhenChanged() {
        // Given
        var capturedMin: Int? = null
        
        // When
        composeTestRule.setContent {
            ParticipantsEstimationCard(
                minParticipants = null,
                maxParticipants = null,
                expectedParticipants = null,
                onMinChanged = { capturedMin = it },
                onMaxChanged = {},
                onExpectedChanged = {}
            )
        }
        
        // Type "10" in minimum field
        composeTestRule
            .onNodeWithText("Minimum Participants")
            .performTextInput("10")
        
        // Then
        composeTestRule.waitForIdle()
        assertEquals(10, capturedMin)
    }
    
    @Test
    fun participantsCard_callsMaxCallbackWhenChanged() {
        // Given
        var capturedMax: Int? = null
        
        // When
        composeTestRule.setContent {
            ParticipantsEstimationCard(
                minParticipants = null,
                maxParticipants = null,
                expectedParticipants = null,
                onMinChanged = {},
                onMaxChanged = { capturedMax = it },
                onExpectedChanged = {}
            )
        }
        
        // Type "50" in maximum field
        composeTestRule
            .onNodeWithText("Maximum Participants")
            .performTextInput("50")
        
        // Then
        composeTestRule.waitForIdle()
        assertEquals(50, capturedMax)
    }
    
    @Test
    fun participantsCard_callsExpectedCallbackWhenChanged() {
        // Given
        var capturedExpected: Int? = null
        
        // When
        composeTestRule.setContent {
            ParticipantsEstimationCard(
                minParticipants = null,
                maxParticipants = null,
                expectedParticipants = null,
                onMinChanged = {},
                onMaxChanged = {},
                onExpectedChanged = { capturedExpected = it }
            )
        }
        
        // Type "25" in expected field
        composeTestRule
            .onNodeWithText("Expected Participants")
            .performTextInput("25")
        
        // Then
        composeTestRule.waitForIdle()
        assertEquals(25, capturedExpected)
    }
    
    @Test
    fun participantsCard_showsErrorWhenMaxLessThanMin() {
        // When
        composeTestRule.setContent {
            ParticipantsEstimationCard(
                minParticipants = 50,
                maxParticipants = 10,
                expectedParticipants = null,
                onMinChanged = {},
                onMaxChanged = {},
                onExpectedChanged = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Maximum must be greater than or equal to minimum")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun participantsCard_showsWarningWhenExpectedOutOfRange() {
        // When
        composeTestRule.setContent {
            ParticipantsEstimationCard(
                minParticipants = 10,
                maxParticipants = 50,
                expectedParticipants = 100, // Out of range
                onMinChanged = {},
                onMaxChanged = {},
                onExpectedChanged = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Expected is outside min-max range")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun participantsCard_showsHelperTextWhenExpectedProvided() {
        // When
        composeTestRule.setContent {
            ParticipantsEstimationCard(
                minParticipants = 10,
                maxParticipants = 50,
                expectedParticipants = 25,
                onMinChanged = {},
                onMaxChanged = {},
                onExpectedChanged = {}
            )
        }
        
        // Then - helper text should mention the expected count
        composeTestRule
            .onNodeWithText("💡 This helps us suggest appropriate venues and estimate costs for ~25 people")
            .assertExists()
    }
    
    @Test
    fun participantsCard_ignoresNegativeValues() {
        // Given
        var capturedMin: Int? = null
        
        // When
        composeTestRule.setContent {
            ParticipantsEstimationCard(
                minParticipants = null,
                maxParticipants = null,
                expectedParticipants = null,
                onMinChanged = { capturedMin = it },
                onMaxChanged = {},
                onExpectedChanged = {}
            )
        }
        
        // Type "-5" in minimum field
        composeTestRule
            .onNodeWithText("Minimum Participants")
            .performTextInput("-5")
        
        // Then - should be null (filtered out)
        composeTestRule.waitForIdle()
        assertNull(capturedMin)
    }
    
    @Test
    fun participantsCard_disabledWhenEnabledFalse() {
        // When
        composeTestRule.setContent {
            ParticipantsEstimationCard(
                minParticipants = null,
                maxParticipants = null,
                expectedParticipants = null,
                onMinChanged = {},
                onMaxChanged = {},
                onExpectedChanged = {},
                enabled = false
            )
        }
        
        // Then - all fields should be disabled
        composeTestRule.onNodeWithText("Minimum Participants").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Maximum Participants").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Expected Participants").assertIsNotEnabled()
    }
}
