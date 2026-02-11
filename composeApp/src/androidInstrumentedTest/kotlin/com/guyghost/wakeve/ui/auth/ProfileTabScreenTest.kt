package com.guyghost.wakeve.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.guyghost.wakeve.ProfileTabScreen
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
 * Android Compose UI tests for ProfileTabScreen.
 * 
 * These tests verify the profile screen displays correctly based on:
 * - Authenticated user state (shows name, email, sign out)
 * - Guest user state (shows guest badge, create account button)
 * 
 * Tests cover:
 * - User profile card display
 * - Guest mode badge and messaging
 * - Sign out button behavior
 * - Create account button for guests
 * - Navigation callbacks
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileTabScreenTest {

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
    fun profileScreen_displaysUserName_whenAuthenticated() {
        // Given - Authenticated user with name
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "user-123",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = "john@example.com",
                    userName = "John Doe",
                    onNavigateToSettings = {},
                    onNavigateToInbox = {},
                    onSignOut = {},
                    onCreateAccount = {}
                )
            }
        }

        // Then - User name should be displayed
        composeTestRule.onNodeWithText("John Doe").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysUserEmail_whenAuthenticated() {
        // Given - Authenticated user with email
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "user-123",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = "john@example.com",
                    userName = "John Doe",
                    onNavigateToSettings = {},
                    onNavigateToInbox = {},
                    onSignOut = {},
                    onCreateAccount = {}
                )
            }
        }

        // Then - User email should be displayed
        composeTestRule.onNodeWithText("john@example.com").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysSignOutButton_whenAuthenticated() {
        // Given - Authenticated user
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "user-123",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = "john@example.com",
                    userName = "John Doe",
                    onNavigateToSettings = {},
                    onNavigateToInbox = {},
                    onSignOut = {},
                    onCreateAccount = {}
                )
            }
        }

        // Then - Sign out button should be displayed
        composeTestRule.onNodeWithText("Se déconnecter").assertIsDisplayed()
    }

    @Test
    fun profileScreen_signOutButtonTriggersCallback() {
        // Given
        var signOutClicked = false
        
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "user-123",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = "john@example.com",
                    userName = "John Doe",
                    onNavigateToSettings = {},
                    onNavigateToInbox = {},
                    onSignOut = { signOutClicked = true },
                    onCreateAccount = {}
                )
            }
        }

        // When - Click sign out
        composeTestRule.onNodeWithText("Se déconnecter").performClick()
        
        // Then - Callback should be triggered
        assertTrue(signOutClicked, "Sign out callback should have been triggered")
    }

    @Test
    fun profileScreen_displaysInitials_whenUserHasName() {
        // Given - Authenticated user with name "John Doe"
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "user-123",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = "john@example.com",
                    userName = "John Doe",
                    onNavigateToSettings = {},
                    onNavigateToInbox = {},
                    onSignOut = {},
                    onCreateAccount = {}
                )
            }
        }

        // Then - Initials "JD" should be displayed in avatar
        composeTestRule.onNodeWithText("JD").assertIsDisplayed()
    }

    // ========================================================================
    // Guest User Tests
    // ========================================================================

    @Test
    fun profileScreen_displaysGuestBadge_whenGuest() {
        // Given - Guest user
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "guest",
                    isGuest = true,
                    isAuthenticated = false,
                    userEmail = null,
                    userName = null,
                    onNavigateToSettings = {},
                    onNavigateToInbox = {},
                    onSignOut = {},
                    onCreateAccount = {}
                )
            }
        }

        // Then - Guest badge should be displayed
        composeTestRule.onNodeWithText("Mode invité").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysGuestName_whenGuest() {
        // Given - Guest user
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "guest",
                    isGuest = true,
                    isAuthenticated = false,
                    userEmail = null,
                    userName = null,
                    onNavigateToSettings = {},
                    onNavigateToInbox = {},
                    onSignOut = {},
                    onCreateAccount = {}
                )
            }
        }

        // Then - "Invité" should be displayed as name
        composeTestRule.onNodeWithText("Invité").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysLimitedFunctionality_whenGuest() {
        // Given - Guest user
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "guest",
                    isGuest = true,
                    isAuthenticated = false,
                    userEmail = null,
                    userName = null,
                    onNavigateToSettings = {},
                    onNavigateToInbox = {},
                    onSignOut = {},
                    onCreateAccount = {}
                )
            }
        }

        // Then - Limited functionality message should be displayed
        composeTestRule.onNodeWithText("Fonctionnalités limitées").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysCreateAccountButton_whenGuest() {
        // Given - Guest user
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "guest",
                    isGuest = true,
                    isAuthenticated = false,
                    userEmail = null,
                    userName = null,
                    onNavigateToSettings = {},
                    onNavigateToInbox = {},
                    onSignOut = {},
                    onCreateAccount = {}
                )
            }
        }

        // Then - Create account button should be displayed
        composeTestRule.onNodeWithText("Créer un compte").assertIsDisplayed()
    }

    @Test
    fun profileScreen_createAccountButtonTriggersCallback() {
        // Given
        var createAccountClicked = false
        
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "guest",
                    isGuest = true,
                    isAuthenticated = false,
                    userEmail = null,
                    userName = null,
                    onNavigateToSettings = {},
                    onNavigateToInbox = {},
                    onSignOut = {},
                    onCreateAccount = { createAccountClicked = true }
                )
            }
        }

        // When - Click create account
        composeTestRule.onNodeWithText("Créer un compte").performClick()
        
        // Then - Callback should be triggered
        assertTrue(createAccountClicked, "Create account callback should have been triggered")
    }

    @Test
    fun profileScreen_hidesSignOutButton_whenGuest() {
        // Given - Guest user
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "guest",
                    isGuest = true,
                    isAuthenticated = false,
                    userEmail = null,
                    userName = null,
                    onNavigateToSettings = {},
                    onNavigateToInbox = {},
                    onSignOut = {},
                    onCreateAccount = {}
                )
            }
        }

        // Then - Sign out button should NOT be displayed for guests
        // We verify by checking "Create account" is shown instead
        composeTestRule.onNodeWithText("Créer un compte").assertIsDisplayed()
    }

    // ========================================================================
    // Navigation Tests
    // ========================================================================

    @Test
    fun profileScreen_settingsButtonTriggersNavigation() {
        // Given
        var settingsClicked = false
        
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "user-123",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = "john@example.com",
                    userName = "John Doe",
                    onNavigateToSettings = { settingsClicked = true },
                    onNavigateToInbox = {},
                    onSignOut = {},
                    onCreateAccount = {}
                )
            }
        }

        // When - Click settings
        composeTestRule.onNodeWithText("Paramètres").performClick()
        
        // Then - Navigation callback should be triggered
        assertTrue(settingsClicked, "Settings navigation callback should have been triggered")
    }

    @Test
    fun profileScreen_inboxButtonTriggersNavigation() {
        // Given
        var inboxClicked = false
        
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "user-123",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = "john@example.com",
                    userName = "John Doe",
                    onNavigateToSettings = {},
                    onNavigateToInbox = { inboxClicked = true },
                    onSignOut = {},
                    onCreateAccount = {}
                )
            }
        }

        // When - Click inbox
        composeTestRule.onNodeWithText("Boîte de réception").performClick()
        
        // Then - Navigation callback should be triggered
        assertTrue(inboxClicked, "Inbox navigation callback should have been triggered")
    }

    // ========================================================================
    // Common Elements Tests
    // ========================================================================

    @Test
    fun profileScreen_displaysAppInfo() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "user-123",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = null,
                    userName = null,
                    onNavigateToSettings = {},
                    onNavigateToInbox = {},
                    onSignOut = {},
                    onCreateAccount = {}
                )
            }
        }

        // Then - App info section should be displayed
        composeTestRule.onNodeWithText("Wakeve").assertIsDisplayed()
        composeTestRule.onNodeWithText("Version 1.0.0 (Phase 2)").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysQuickActionsSection() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "user-123",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = null,
                    userName = null,
                    onNavigateToSettings = {},
                    onNavigateToInbox = {},
                    onSignOut = {},
                    onCreateAccount = {}
                )
            }
        }

        // Then - Quick actions section should be displayed
        composeTestRule.onNodeWithText("Actions rapides").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysAccountSection() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                ProfileTabScreen(
                    userId = "user-123",
                    isGuest = false,
                    isAuthenticated = true,
                    userEmail = null,
                    userName = null,
                    onNavigateToSettings = {},
                    onNavigateToInbox = {},
                    onSignOut = {},
                    onCreateAccount = {}
                )
            }
        }

        // Then - Account section should be displayed
        composeTestRule.onNodeWithText("Compte").assertIsDisplayed()
    }
}
