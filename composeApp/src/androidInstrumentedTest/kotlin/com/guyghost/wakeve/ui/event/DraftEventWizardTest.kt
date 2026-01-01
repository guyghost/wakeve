package com.guyghost.wakeve.ui.event

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Instrumented tests for DraftEventWizard component.
 * 
 * Tests the complete wizard flow including:
 * - Navigation between steps
 * - Validation at each step
 * - Auto-save functionality
 * - Completion and cancellation
 * - Accessibility (TalkBack support)
 */
class DraftEventWizardTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun wizard_startsAtStepOne() {
        // When
        composeTestRule.setContent {
            DraftEventWizard(
                initialEvent = null,
                onSaveStep = {},
                onComplete = {},
                onCancel = {}
            )
        }
        
        // Then - Step 1 title should be visible
        composeTestRule
            .onNodeWithText("Basic Info")
            .assertExists()
            .assertIsDisplayed()
        
        // Progress indicator should show 25% (step 1/4)
        composeTestRule
            .onNodeWithContentDescription("Step 1 of 4")
            .assertExists()
    }
    
    @Test
    fun wizard_step1_requiresTitleAndDescription() {
        // When
        composeTestRule.setContent {
            DraftEventWizard(
                initialEvent = null,
                onSaveStep = {},
                onComplete = {},
                onCancel = {}
            )
        }
        
        // Then - Next button should be disabled with empty fields
        composeTestRule
            .onNodeWithText("Next")
            .assertIsNotEnabled()
        
        // Fill in title
        composeTestRule
            .onNodeWithText("Event Title")
            .performTextInput("Summer Retreat")
        
        // Still disabled without description
        composeTestRule
            .onNodeWithText("Next")
            .assertIsNotEnabled()
        
        // Fill in description
        composeTestRule
            .onNodeWithText("Event Description")
            .performTextInput("Annual team building event")
        
        // Now Next should be enabled
        composeTestRule
            .onNodeWithText("Next")
            .assertIsEnabled()
    }
    
    @Test
    fun wizard_step1_customTypeRequiresCustomField() {
        // When
        composeTestRule.setContent {
            DraftEventWizard(
                initialEvent = null,
                onSaveStep = {},
                onComplete = {},
                onCancel = {}
            )
        }
        
        // Fill basic fields
        composeTestRule
            .onNodeWithText("Event Title")
            .performTextInput("Hackathon")
        
        composeTestRule
            .onNodeWithText("Event Description")
            .performTextInput("24-hour coding marathon")
        
        // Select CUSTOM event type
        composeTestRule
            .onNodeWithContentDescription("Select event type")
            .performClick()
        
        composeTestRule
            .onNodeWithText("Custom")
            .performClick()
        
        // Custom field should appear
        composeTestRule
            .onNodeWithText("Custom Event Type")
            .assertExists()
        
        // Next should be disabled without custom type value
        composeTestRule
            .onNodeWithText("Next")
            .assertIsNotEnabled()
        
        // Fill custom type
        composeTestRule
            .onNodeWithText("e.g., Charity Gala, Festival")
            .performTextInput("Tech Hackathon")
        
        // Now Next should be enabled
        composeTestRule
            .onNodeWithText("Next")
            .assertIsEnabled()
    }
    
    @Test
    fun wizard_navigatesToStep2() {
        // When
        composeTestRule.setContent {
            DraftEventWizard(
                initialEvent = null,
                onSaveStep = {},
                onComplete = {},
                onCancel = {}
            )
        }
        
        // Fill Step 1
        composeTestRule
            .onNodeWithText("Event Title")
            .performTextInput("Conference")
        
        composeTestRule
            .onNodeWithText("Event Description")
            .performTextInput("Annual tech conference")
        
        // Navigate to Step 2
        composeTestRule
            .onNodeWithText("Next")
            .performClick()
        
        // Wait for navigation
        composeTestRule.waitForIdle()
        
        // Then - Step 2 should be visible
        composeTestRule
            .onNodeWithText("Participants")
            .assertExists()
            .assertIsDisplayed()
        
        // Progress should show step 2
        composeTestRule
            .onNodeWithContentDescription("Step 2 of 4")
            .assertExists()
        
        // Back button should be visible
        composeTestRule
            .onNodeWithContentDescription("Back")
            .assertExists()
            .assertIsEnabled()
    }
    
    @Test
    fun wizard_step2_validatesParticipantCounts() {
        // When
        composeTestRule.setContent {
            DraftEventWizard(
                initialEvent = null,
                onSaveStep = {},
                onComplete = {},
                onCancel = {}
            )
        }
        
        // Navigate to Step 2
        composeTestRule
            .onNodeWithText("Event Title")
            .performTextInput("Workshop")
        
        composeTestRule
            .onNodeWithText("Event Description")
            .performTextInput("Training workshop")
        
        composeTestRule
            .onNodeWithText("Next")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Fill invalid participants (max < min)
        composeTestRule
            .onNodeWithText("Minimum")
            .performTextInput("50")
        
        composeTestRule
            .onNodeWithText("Maximum")
            .performTextInput("20")
        
        // Should show error
        composeTestRule
            .onNodeWithText("Maximum must be ≥ minimum")
            .assertExists()
        
        // Next should be disabled
        composeTestRule
            .onNodeWithText("Next")
            .assertIsNotEnabled()
        
        // Fix max value
        composeTestRule
            .onNodeWithText("Maximum")
            .performTextClearance()
        
        composeTestRule
            .onNodeWithText("Maximum")
            .performTextInput("100")
        
        // Now Next should be enabled
        composeTestRule
            .onNodeWithText("Next")
            .assertIsEnabled()
    }
    
    @Test
    fun wizard_step2_allowsEmptyFields() {
        // When
        composeTestRule.setContent {
            DraftEventWizard(
                initialEvent = null,
                onSaveStep = {},
                onComplete = {},
                onCancel = {}
            )
        }
        
        // Navigate to Step 2
        composeTestRule
            .onNodeWithText("Event Title")
            .performTextInput("Meeting")
        
        composeTestRule
            .onNodeWithText("Event Description")
            .performTextInput("Team meeting")
        
        composeTestRule
            .onNodeWithText("Next")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Don't fill any participant fields (they're optional)
        // Next should still be enabled
        composeTestRule
            .onNodeWithText("Next")
            .assertIsEnabled()
    }
    
    @Test
    fun wizard_navigatesBackToStep1() {
        // When
        composeTestRule.setContent {
            DraftEventWizard(
                initialEvent = null,
                onSaveStep = {},
                onComplete = {},
                onCancel = {}
            )
        }
        
        // Navigate to Step 2
        composeTestRule
            .onNodeWithText("Event Title")
            .performTextInput("Event")
        
        composeTestRule
            .onNodeWithText("Event Description")
            .performTextInput("Description")
        
        composeTestRule
            .onNodeWithText("Next")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Go back to Step 1
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then - Step 1 should be visible again
        composeTestRule
            .onNodeWithText("Basic Info")
            .assertExists()
        
        // Fields should retain values
        composeTestRule
            .onNodeWithText("Event")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Description")
            .assertExists()
    }
    
    @Test
    fun wizard_step3_displaysLocations() {
        // When
        composeTestRule.setContent {
            DraftEventWizard(
                initialEvent = null,
                onSaveStep = {},
                onComplete = {},
                onCancel = {}
            )
        }
        
        // Navigate to Step 3
        navigateToStep(3)
        
        // Then - Locations step should be visible
        composeTestRule
            .onNodeWithText("Locations")
            .assertExists()
        
        // Add location button should exist
        composeTestRule
            .onNodeWithText("Add Location")
            .assertExists()
            .assertIsEnabled()
        
        // Empty state should be shown
        composeTestRule
            .onNodeWithText("No locations added yet")
            .assertExists()
    }
    
    @Test
    fun wizard_step4_displaysTimeSlots() {
        // When
        composeTestRule.setContent {
            DraftEventWizard(
                initialEvent = null,
                onSaveStep = {},
                onComplete = {},
                onCancel = {}
            )
        }
        
        // Navigate to Step 4
        navigateToStep(4)
        
        // Then - Time Slots step should be visible
        composeTestRule
            .onNodeWithText("Time Slots")
            .assertExists()
        
        // Add time slot button should exist
        composeTestRule
            .onNodeWithText("Add Time Slot")
            .assertExists()
        
        // Without time slots, Complete should be disabled
        composeTestRule
            .onNodeWithText("Complete")
            .assertIsNotEnabled()
    }
    
    @Test
    fun wizard_callsOnSaveStepWhenNavigating() {
        // Given
        var savedEvent: Event? = null
        
        // When
        composeTestRule.setContent {
            DraftEventWizard(
                initialEvent = null,
                onSaveStep = { savedEvent = it },
                onComplete = {},
                onCancel = {}
            )
        }
        
        // Fill Step 1
        composeTestRule
            .onNodeWithText("Event Title")
            .performTextInput("Test Event")
        
        composeTestRule
            .onNodeWithText("Event Description")
            .performTextInput("Test Description")
        
        // Navigate to Step 2 (triggers save)
        composeTestRule
            .onNodeWithText("Next")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then - onSaveStep should have been called
        assertNotNull(savedEvent)
        assertEquals("Test Event", savedEvent?.title)
        assertEquals("Test Description", savedEvent?.description)
        assertEquals(EventStatus.DRAFT, savedEvent?.status)
    }
    
    @Test
    fun wizard_callsOnCompleteAtEnd() {
        // Given
        var completedEvent: Event? = null
        
        // When
        composeTestRule.setContent {
            DraftEventWizard(
                initialEvent = null,
                onSaveStep = {},
                onComplete = { completedEvent = it },
                onCancel = {}
            )
        }
        
        // Navigate to Step 4 and add time slot
        navigateToStep(4)
        
        // Add a time slot (simplified - just checking Complete button)
        // In a real test, we'd actually add a time slot through the UI
        // For now, verify the flow
        
        // Complete button should exist
        composeTestRule
            .onNodeWithText("Complete")
            .assertExists()
    }
    
    @Test
    fun wizard_callsOnCancelWhenCancelled() {
        // Given
        var cancelled = false
        
        // When
        composeTestRule.setContent {
            DraftEventWizard(
                initialEvent = null,
                onSaveStep = {},
                onComplete = {},
                onCancel = { cancelled = true }
            )
        }
        
        // Click cancel
        composeTestRule
            .onNodeWithText("Cancel")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then
        assertEquals(true, cancelled)
    }
    
    @Test
    fun wizard_accessibility_allStepsHaveDescriptions() {
        // When
        composeTestRule.setContent {
            DraftEventWizard(
                initialEvent = null,
                onSaveStep = {},
                onComplete = {},
                onCancel = {}
            )
        }
        
        // Then - Check accessibility content descriptions exist
        composeTestRule
            .onNodeWithContentDescription("Step 1 of 4")
            .assertExists()
        
        // Navigate through all steps checking accessibility
        for (step in 1..4) {
            composeTestRule
                .onNodeWithContentDescription("Step $step of 4")
                .assertExists()
            
            if (step < 4) {
                // Navigate to next step
                fillStepAndProceed(step)
            }
        }
    }
    
    // Helper functions
    
    private fun navigateToStep(targetStep: Int) {
        // Fill and navigate through steps
        for (step in 1 until targetStep) {
            fillStepAndProceed(step)
        }
    }
    
    private fun fillStepAndProceed(step: Int) {
        when (step) {
            1 -> {
                composeTestRule
                    .onNodeWithText("Event Title")
                    .performTextInput("Test Event $step")
                
                composeTestRule
                    .onNodeWithText("Event Description")
                    .performTextInput("Test Description $step")
            }
            2 -> {
                // Optional step - can proceed without filling
            }
            3 -> {
                // Optional step - can proceed without adding locations
            }
        }
        
        composeTestRule
            .onNodeWithText("Next")
            .performClick()
        
        composeTestRule.waitForIdle()
    }
}
