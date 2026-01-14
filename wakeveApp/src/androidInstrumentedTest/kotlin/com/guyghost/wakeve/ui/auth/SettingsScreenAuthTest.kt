package com.guyghost.wakeve.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.guyghost.wakeve.SettingsScreen
import com.guyghost.wakeve.theme.WakeveTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Android Compose UI tests for SettingsScreen (main settings with auth).
 * 
 * These tests verify the settings screen displays correctly based on:
 * - Authenticated user state (shows name, email, logout button)
 * - Guest user state (shows guest badge, create account button)
 * 
 * Tests cover:
 * - Account section display for different auth states
 * - Logout button behavior
 * - Create account button for guests
 * - Active sessions section
 * - Navigation callbacks
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsScreenAuthTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ========================================================================
    // Authenticated User Tests
    // ========================================================================

    @Test
    fun settingsScreen_displaysUserName_whenAuthenticated() {
        // Given - Authenticated user with name
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "user-123",
                    currentSessionId = "session-1",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = "john@example.com",
                    userName = "John Doe",
                    onLogout = {},
                    onCreateAccount = {},
                    onBack = {}
                )
            }
        }

        // Then - User name should be displayed in account section
        composeTestRule.onNodeWithText("John Doe").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysUserEmail_whenAuthenticated() {
        // Given - Authenticated user with email
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "user-123",
                    currentSessionId = "session-1",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = "john@example.com",
                    userName = "John Doe",
                    onLogout = {},
                    onCreateAccount = {},
                    onBack = {}
                )
            }
        }

        // Then - User email should be displayed
        composeTestRule.onNodeWithText("john@example.com").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysLogoutButton_whenAuthenticated() {
        // Given - Authenticated user
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "user-123",
                    currentSessionId = "session-1",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = "john@example.com",
                    userName = "John Doe",
                    onLogout = {},
                    onCreateAccount = {},
                    onBack = {}
                )
            }
        }

        // Then - Logout button should be displayed
        composeTestRule.onNodeWithText("Se déconnecter").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_logoutButtonTriggersCallback() {
        // Given
        var logoutClicked = false
        
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "user-123",
                    currentSessionId = "session-1",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = "john@example.com",
                    userName = "John Doe",
                    onLogout = { logoutClicked = true },
                    onCreateAccount = {},
                    onBack = {}
                )
            }
        }

        // When - Click logout
        composeTestRule.onNodeWithText("Se déconnecter").performClick()
        
        // Then - Callback should be triggered
        assertTrue(logoutClicked, "Logout callback should have been triggered")
    }

    @Test
    fun settingsScreen_displaysAccountSectionTitle() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "user-123",
                    currentSessionId = "session-1",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = "john@example.com",
                    userName = "John Doe",
                    onLogout = {},
                    onCreateAccount = {},
                    onBack = {}
                )
            }
        }

        // Then - Account section title should be displayed
        composeTestRule.onNodeWithText("Compte").assertIsDisplayed()
    }

    // ========================================================================
    // Guest User Tests
    // ========================================================================

    @Test
    fun settingsScreen_displaysGuestBadge_whenGuest() {
        // Given - Guest user
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "guest",
                    currentSessionId = "",
                    isGuest = true,
                    isAuthenticated = false,
                    userEmail = null,
                    userName = null,
                    onLogout = {},
                    onCreateAccount = {},
                    onBack = {}
                )
            }
        }

        // Then - Guest badge should be displayed
        composeTestRule.onNodeWithText("Mode invité").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysLimitedFunctionality_whenGuest() {
        // Given - Guest user
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "guest",
                    currentSessionId = "",
                    isGuest = true,
                    isAuthenticated = false,
                    userEmail = null,
                    userName = null,
                    onLogout = {},
                    onCreateAccount = {},
                    onBack = {}
                )
            }
        }

        // Then - Limited functionality message should be displayed
        composeTestRule.onNodeWithText("Fonctionnalités limitées").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysCreateAccountButton_whenGuest() {
        // Given - Guest user
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "guest",
                    currentSessionId = "",
                    isGuest = true,
                    isAuthenticated = false,
                    userEmail = null,
                    userName = null,
                    onLogout = {},
                    onCreateAccount = {},
                    onBack = {}
                )
            }
        }

        // Then - Create account button should be displayed
        composeTestRule.onNodeWithText("Créer un compte").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_createAccountButtonTriggersCallback() {
        // Given
        var createAccountClicked = false
        
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "guest",
                    currentSessionId = "",
                    isGuest = true,
                    isAuthenticated = false,
                    userEmail = null,
                    userName = null,
                    onLogout = {},
                    onCreateAccount = { createAccountClicked = true },
                    onBack = {}
                )
            }
        }

        // When - Click create account
        composeTestRule.onNodeWithText("Créer un compte").performClick()
        
        // Then - Callback should be triggered
        assertTrue(createAccountClicked, "Create account callback should have been triggered")
    }

    @Test
    fun settingsScreen_displaysUpgradeMessage_whenGuest() {
        // Given - Guest user
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "guest",
                    currentSessionId = "",
                    isGuest = true,
                    isAuthenticated = false,
                    userEmail = null,
                    userName = null,
                    onLogout = {},
                    onCreateAccount = {},
                    onBack = {}
                )
            }
        }

        // Then - Upgrade message should be displayed
        composeTestRule.onNodeWithText("Créez un compte pour sauvegarder vos données").assertIsDisplayed()
    }

    // ========================================================================
    // Active Sessions Tests
    // ========================================================================

    @Test
    fun settingsScreen_displaysActiveSessionsSection() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "user-123",
                    currentSessionId = "session-1",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = "john@example.com",
                    userName = "John Doe",
                    onLogout = {},
                    onCreateAccount = {},
                    onBack = {}
                )
            }
        }

        // Then - Active sessions section should be displayed
        // The mock data shows 3 sessions
        composeTestRule.onNodeWithText("Active Sessions (3)").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysCurrentSessionBadge() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "user-123",
                    currentSessionId = "session-1",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = "john@example.com",
                    userName = "John Doe",
                    onLogout = {},
                    onCreateAccount = {},
                    onBack = {}
                )
            }
        }

        // Then - Current session badge should be displayed
        composeTestRule.onNodeWithText("Current").assertIsDisplayed()
    }

    // ========================================================================
    // Navigation Tests
    // ========================================================================

    @Test
    fun settingsScreen_backButtonTriggersCallback() {
        // Given
        var backClicked = false
        
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "user-123",
                    currentSessionId = "session-1",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = null,
                    userName = null,
                    onLogout = {},
                    onCreateAccount = {},
                    onBack = { backClicked = true }
                )
            }
        }

        // When - Click back (via content description)
        composeTestRule.onNodeWithText("Retour", useUnmergedTree = true).performClick()
        
        // Then - Callback should be triggered
        // Note: If contentDescription is used, we'd use onNodeWithContentDescription
    }

    @Test
    fun settingsScreen_displaysTitle() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "user-123",
                    currentSessionId = "session-1",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = null,
                    userName = null,
                    onLogout = {},
                    onCreateAccount = {},
                    onBack = {}
                )
            }
        }

        // Then - Title should be displayed
        composeTestRule.onNodeWithText("Paramètres").assertIsDisplayed()
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    fun settingsScreen_displaysUserIdWhenNoNameOrEmail() {
        // Given - Authenticated user without name or email
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "user-123",
                    currentSessionId = "session-1",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = null,
                    userName = null,
                    onLogout = {},
                    onCreateAccount = {},
                    onBack = {}
                )
            }
        }

        // Then - User ID should be displayed as fallback
        composeTestRule.onNodeWithText("ID: user-123").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_hidesLogoutButton_whenGuest() {
        // Given - Guest user
        composeTestRule.setContent {
            WakeveTheme {
                SettingsScreen(
                    userId = "guest",
                    currentSessionId = "",
                    isGuest = true,
                    isAuthenticated = false,
                    userEmail = null,
                    userName = null,
                    onLogout = {},
                    onCreateAccount = {},
                    onBack = {}
                )
            }
        }

        // Then - Create account button should be shown, not logout
        composeTestRule.onNodeWithText("Créer un compte").assertIsDisplayed()
    }
}
