package com.guyghost.wakeve.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.guyghost.wakeve.models.EventType
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Compose tests for EventTypeSelector component.
 */
class EventTypeSelectorTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun eventTypeSelector_displaysSelectedType() {
        // Given
        val selectedType = EventType.BIRTHDAY
        
        // When
        composeTestRule.setContent {
            EventTypeSelector(
                selectedType = selectedType,
                customTypeValue = "",
                onTypeSelected = {},
                onCustomTypeChanged = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Birthday")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun eventTypeSelector_showsCustomFieldWhenCustomSelected() {
        // Given
        val selectedType = EventType.CUSTOM
        val customValue = "Charity Gala"
        
        // When
        composeTestRule.setContent {
            EventTypeSelector(
                selectedType = selectedType,
                customTypeValue = customValue,
                onTypeSelected = {},
                onCustomTypeChanged = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Custom Event Type")
            .assertExists()
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText(customValue)
            .assertExists()
    }
    
    @Test
    fun eventTypeSelector_hidesCustomFieldForOtherTypes() {
        // Given
        val selectedType = EventType.PARTY
        
        // When
        composeTestRule.setContent {
            EventTypeSelector(
                selectedType = selectedType,
                customTypeValue = "",
                onTypeSelected = {},
                onCustomTypeChanged = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Custom Event Type")
            .assertDoesNotExist()
    }
    
    @Test
    fun eventTypeSelector_callsCallbackWhenTypeChanged() {
        // Given
        var capturedType: EventType? = null
        
        // When
        composeTestRule.setContent {
            EventTypeSelector(
                selectedType = EventType.OTHER,
                customTypeValue = "",
                onTypeSelected = { capturedType = it },
                onCustomTypeChanged = {}
            )
        }
        
        // Open dropdown
        composeTestRule
            .onNodeWithContentDescription("Select event type")
            .performClick()
        
        // Select WEDDING
        composeTestRule
            .onNodeWithText("Wedding")
            .performClick()
        
        // Then
        composeTestRule.waitForIdle()
        assertEquals(EventType.WEDDING, capturedType)
    }
    
    @Test
    fun eventTypeSelector_showsErrorWhenCustomTypeEmpty() {
        // When
        composeTestRule.setContent {
            EventTypeSelector(
                selectedType = EventType.CUSTOM,
                customTypeValue = "",
                onTypeSelected = {},
                onCustomTypeChanged = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Custom event type is required")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun eventTypeSelector_disabledWhenEnabledFalse() {
        // When
        composeTestRule.setContent {
            EventTypeSelector(
                selectedType = EventType.PARTY,
                customTypeValue = "",
                onTypeSelected = {},
                onCustomTypeChanged = {},
                enabled = false
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Select event type")
            .assertIsNotEnabled()
    }
}
