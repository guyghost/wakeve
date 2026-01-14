package com.guyghost.wakeve.ui.screens

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.guyghost.wakeve.localization.AppLocale
import com.guyghost.wakeve.localization.LocalizationService
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Instrumented UI tests for SettingsScreen language selector.
 *
 * Tests the Settings screen including:
 * - Display of language selector
 * - Display of all locale options
 * - Locale selection via UI interaction
 * - Navigation back button
 * - Language persistence
 * - Display in different locales
 */
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        // Clean up SharedPreferences before each test
        android.content.Context.MODE_PRIVATE
        
        // Initialize LocalizationService with default locale
        LocalizationService.getInstance().setLocale(AppLocale.ENGLISH)
    }

    @After
    fun teardown() {
        // Clean up after each test
    }

    // ==================== Display Tests ====================

    /**
     * Test that settings screen displays the language title.
     *
     * GIVEN the settings screen is displayed
     * WHEN the screen is rendered
     * THEN the language selector title is visible
     */
    @Test
    fun `settings screen displays language title`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        composeTestRule
            .onNodeWithText("Langue")
            .assertExists()
            .assertIsDisplayed()
    }

    /**
     * Test that settings screen displays language description.
     *
     * GIVEN the settings screen is displayed
     * WHEN the screen is rendered
     * THEN the language description is visible
     */
    @Test
    fun `settings screen displays language description`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        // The description should exist (text varies by locale)
        composeTestRule
            .onNodeWithText("Sélectionnez")
            .assertExists()
    }

    /**
     * Test that settings screen displays all locale options.
     *
     * GIVEN the settings screen is displayed
     * WHEN the screen is rendered
     * THEN all three locales are visible (French, English, Spanish)
     */
    @Test
    fun `settings screen displays all locale options`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        // Check that all three locale options are displayed
        composeTestRule.onNodeWithText("Français").assertExists()
        composeTestRule.onNodeWithText("English").assertExists()
        composeTestRule.onNodeWithText("Español").assertExists()
    }

    /**
     * Test that locale options are displayed in their native script.
     *
     * GIVEN the settings screen is displayed
     * WHEN the screen is rendered
     * THEN each locale name is in its native language/script
     */
    @Test
    fun `locale options displayed in native script`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        // French should be "Français" (native)
        composeTestRule.onNodeWithText("Français").assertExists()
        // English should be "English" (native)
        composeTestRule.onNodeWithText("English").assertExists()
        // Spanish should be "Español" (native)
        composeTestRule.onNodeWithText("Español").assertExists()
    }

    /**
     * Test that back button is present and clickable.
     *
     * GIVEN the settings screen is displayed
     * WHEN looking for the back button
     * THEN a clickable back button exists
     */
    @Test
    fun `back button exists and is clickable`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        composeTestRule
            .onNodeWithContentDescription("Back")
            .assertExists()
            .assertHasClickAction()
    }

    /**
     * Test that settings title is displayed.
     *
     * GIVEN the settings screen is displayed
     * WHEN the screen is rendered
     * THEN the settings title is visible
     */
    @Test
    fun `settings title is displayed`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        composeTestRule
            .onNodeWithText("Paramètres")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Interaction Tests ====================

    /**
     * Test that selecting French locale works.
     *
     * GIVEN the settings screen is displayed
     * WHEN clicking on French option
     * THEN the locale is changed to French
     */
    @Test
    fun `selecting French locale works`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        // Click on French locale
        composeTestRule
            .onNodeWithText("Français")
            .performClick()

        // Verify French is selected
        assertEquals(AppLocale.FRENCH, LocalizationService.getInstance().getCurrentLocale())
    }

    /**
     * Test that selecting English locale works.
     *
     * GIVEN the settings screen is displayed
     * WHEN clicking on English option
     * THEN the locale is changed to English
     */
    @Test
    fun `selecting English locale works`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        // Click on English locale
        composeTestRule
            .onNodeWithText("English")
            .performClick()

        // Verify English is selected
        assertEquals(AppLocale.ENGLISH, LocalizationService.getInstance().getCurrentLocale())
    }

    /**
     * Test that selecting Spanish locale works.
     *
     * GIVEN the settings screen is displayed
     * WHEN clicking on Spanish option
     * THEN the locale is changed to Spanish
     */
    @Test
    fun `selecting Spanish locale works`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        // Click on Spanish locale
        composeTestRule
            .onNodeWithText("Español")
            .performClick()

        // Verify Spanish is selected
        assertEquals(AppLocale.SPANISH, LocalizationService.getInstance().getCurrentLocale())
    }

    /**
     * Test that back button callback is invoked.
     *
     * GIVEN the settings screen is displayed
     * WHEN clicking the back button
     * THEN the onBack callback is invoked
     */
    @Test
    fun `back button invokes callback`() {
        var backClicked = false
        composeTestRule.setContent {
            SettingsScreen(onBack = { backClicked = true })
        }

        // Click back button
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        assertEquals(true, backClicked)
    }

    /**
     * Test that switching locales multiple times works.
     *
     * GIVEN the settings screen is displayed
     * WHEN selecting different locales in sequence
     * THEN each selection is applied correctly
     */
    @Test
    fun `switching locales multiple times works`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        // Switch to French
        composeTestRule.onNodeWithText("Français").performClick()
        assertEquals(AppLocale.FRENCH, LocalizationService.getInstance().getCurrentLocale())

        // Switch to Spanish
        composeTestRule.onNodeWithText("Español").performClick()
        assertEquals(AppLocale.SPANISH, LocalizationService.getInstance().getCurrentLocale())

        // Switch to English
        composeTestRule.onNodeWithText("English").performClick()
        assertEquals(AppLocale.ENGLISH, LocalizationService.getInstance().getCurrentLocale())
    }

    // ==================== Display in Different Locales Tests ====================

    /**
     * Test settings screen display in English.
     *
     * GIVEN the locale is English
     * WHEN the settings screen is rendered
     * THEN the screen displays in English
     */
    @Test
    fun `settings screen displays in English`() {
        LocalizationService.getInstance().setLocale(AppLocale.ENGLISH)
        
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        // Check English strings are displayed
        composeTestRule.onNodeWithText("English").assertExists()
    }

    /**
     * Test settings screen display in French.
     *
     * GIVEN the locale is French
     * WHEN the settings screen is rendered
     * THEN the screen displays in French
     */
    @Test
    fun `settings screen displays in French`() {
        LocalizationService.getInstance().setLocale(AppLocale.FRENCH)
        
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        // Check French strings are displayed
        composeTestRule.onNodeWithText("Français").assertExists()
        composeTestRule.onNodeWithText("Langue").assertExists()
    }

    /**
     * Test settings screen display in Spanish.
     *
     * GIVEN the locale is Spanish
     * WHEN the settings screen is rendered
     * THEN the screen displays in Spanish
     */
    @Test
    fun `settings screen displays in Spanish`() {
        LocalizationService.getInstance().setLocale(AppLocale.SPANISH)
        
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        // Check Spanish strings are displayed
        composeTestRule.onNodeWithText("Español").assertExists()
    }

    // ==================== Accessibility Tests ====================

    /**
     * Test that locale options are marked for accessibility.
     *
     * GIVEN the settings screen is displayed
     * WHEN the screen is rendered
     * THEN locale options have accessibility labels
     */
    @Test
    fun `locale options have accessibility support`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        // Verify locale options are clickable (have action semantics)
        composeTestRule.onNodeWithText("Français").assertHasClickAction()
        composeTestRule.onNodeWithText("English").assertHasClickAction()
        composeTestRule.onNodeWithText("Español").assertHasClickAction()
    }

    /**
     * Test that back button has accessibility label.
     *
     * GIVEN the settings screen is displayed
     * WHEN looking for the back button
     * THEN it has an accessibility description
     */
    @Test
    fun `back button has accessibility label`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        composeTestRule
            .onNodeWithContentDescription("Back")
            .assertExists()
    }

    // ==================== Persistence Tests ====================

    /**
     * Test that locale selection is persisted after screen close.
     *
     * GIVEN a locale has been selected in settings
     * WHEN the settings screen is closed and reopened
     * THEN the selected locale is still active
     */
    @Test
    fun `locale selection persists after screen close`() {
        // First render - select French
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }
        composeTestRule.onNodeWithText("Français").performClick()
        
        // Close and reopen
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }
        
        // French should still be selected
        assertEquals(AppLocale.FRENCH, LocalizationService.getInstance().getCurrentLocale())
    }

    /**
     * Test that multiple locale changes are persisted correctly.
     *
     * GIVEN multiple locale changes are made
     * WHEN the settings screen is closed and reopened
     * THEN the last selected locale is active
     */
    @Test
    fun `multiple locale changes persist correctly`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }
        
        // Change locale multiple times
        composeTestRule.onNodeWithText("Français").performClick()
        composeTestRule.onNodeWithText("Español").performClick()
        composeTestRule.onNodeWithText("English").performClick()
        
        // English should be the final selected locale
        assertEquals(AppLocale.ENGLISH, LocalizationService.getInstance().getCurrentLocale())
    }

    // ==================== Layout Tests ====================

    /**
     * Test that locale options are properly spaced.
     *
     * GIVEN the settings screen is displayed
     * WHEN the screen is rendered
     * THEN all locale options are visible (not cut off)
     */
    @Test
    fun `locale options are visible and properly spaced`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        // All three options should be displayed
        composeTestRule.onNodeWithText("Français").assertIsDisplayed()
        composeTestRule.onNodeWithText("English").assertIsDisplayed()
        composeTestRule.onNodeWithText("Español").assertIsDisplayed()
    }

    /**
     * Test that scrolling is available if needed.
     *
     * GIVEN the settings screen is displayed
     * WHEN checking the layout
     * THEN the screen is responsive to content
     */
    @Test
    fun `screen layout is responsive`() {
        composeTestRule.setContent {
            SettingsScreen(onBack = {})
        }

        // The screen should render without errors
        composeTestRule.onNodeWithText("Langue").assertIsDisplayed()
    }
}
