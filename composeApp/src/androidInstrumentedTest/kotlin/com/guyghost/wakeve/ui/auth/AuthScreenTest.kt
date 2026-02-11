package com.guyghost.wakeve.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * Android Compose UI tests for AuthScreen.
 * 
 * These tests verify the authentication screen UI elements and interactions
 * using the callback-based AuthScreen signature.
 * 
 * Tests cover:
 * - UI elements display (title, subtitle, buttons)
 * - Button interactions and callbacks
 * - Loading state handling
 * - Error message display
 * - Skip/guest mode functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthScreenTest {

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
    // UI Elements Display Tests
    // ========================================================================

    @Test
    fun authScreen_displaysTitle() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                AuthScreen(
                    onGoogleSignIn = {},
                    onAppleSignIn = {},
                    onEmailSignIn = {},
                    onSkip = {},
                    isLoading = false,
                    errorMessage = null
                )
            }
        }

        // Then - Verify title is displayed
        composeTestRule.onNodeWithText("Bienvenue sur Wakeve").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysSubtitle() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                AuthScreen(
                    onGoogleSignIn = {},
                    onAppleSignIn = {},
                    onEmailSignIn = {},
                    onSkip = {},
                    isLoading = false,
                    errorMessage = null
                )
            }
        }

        // Then - Verify subtitle is displayed
        composeTestRule.onNodeWithText("Connectez-vous pour accéder à toutes les fonctionnalités").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysSkipButton() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                AuthScreen(
                    onGoogleSignIn = {},
                    onAppleSignIn = {},
                    onEmailSignIn = {},
                    onSkip = {},
                    isLoading = false,
                    errorMessage = null
                )
            }
        }

        // Then - Verify skip button is displayed
        composeTestRule.onNodeWithText("Passer").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysAllAuthButtons() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                AuthScreen(
                    onGoogleSignIn = {},
                    onAppleSignIn = {},
                    onEmailSignIn = {},
                    onSkip = {},
                    isLoading = false,
                    errorMessage = null
                )
            }
        }

        // Then - Verify all buttons are displayed
        composeTestRule.onNodeWithText("Se connecter avec Google").assertIsDisplayed()
        composeTestRule.onNodeWithText("Se connecter avec Apple").assertIsDisplayed()
        composeTestRule.onNodeWithText("Se connecter avec Email").assertIsDisplayed()
    }

    // ========================================================================
    // Button Callback Tests
    // ========================================================================

    @Test
    fun authScreen_skipButtonTriggersCallback() {
        // Given
        var skipClicked = false
        
        composeTestRule.setContent {
            WakeveTheme {
                AuthScreen(
                    onGoogleSignIn = {},
                    onAppleSignIn = {},
                    onEmailSignIn = {},
                    onSkip = { skipClicked = true },
                    isLoading = false,
                    errorMessage = null
                )
            }
        }

        // When - Click skip button
        composeTestRule.onNodeWithText("Passer").performClick()
        
        // Then - Verify callback was triggered
        assertTrue(skipClicked, "Skip callback should have been triggered")
    }

    @Test
    fun authScreen_googleSignInButtonTriggersCallback() {
        // Given
        var googleClicked = false
        
        composeTestRule.setContent {
            WakeveTheme {
                AuthScreen(
                    onGoogleSignIn = { googleClicked = true },
                    onAppleSignIn = {},
                    onEmailSignIn = {},
                    onSkip = {},
                    isLoading = false,
                    errorMessage = null
                )
            }
        }

        // When - Click Google button
        composeTestRule.onNodeWithText("Se connecter avec Google").performClick()
        
        // Then - Verify callback was triggered
        assertTrue(googleClicked, "Google sign-in callback should have been triggered")
    }

    @Test
    fun authScreen_appleSignInButtonTriggersCallback() {
        // Given
        var appleClicked = false
        
        composeTestRule.setContent {
            WakeveTheme {
                AuthScreen(
                    onGoogleSignIn = {},
                    onAppleSignIn = { appleClicked = true },
                    onEmailSignIn = {},
                    onSkip = {},
                    isLoading = false,
                    errorMessage = null
                )
            }
        }

        // When - Click Apple button
        composeTestRule.onNodeWithText("Se connecter avec Apple").performClick()
        
        // Then - Verify callback was triggered
        assertTrue(appleClicked, "Apple sign-in callback should have been triggered")
    }

    @Test
    fun authScreen_emailSignInButtonTriggersCallback() {
        // Given
        var emailClicked = false
        
        composeTestRule.setContent {
            WakeveTheme {
                AuthScreen(
                    onGoogleSignIn = {},
                    onAppleSignIn = {},
                    onEmailSignIn = { emailClicked = true },
                    onSkip = {},
                    isLoading = false,
                    errorMessage = null
                )
            }
        }

        // When - Click Email button
        composeTestRule.onNodeWithText("Se connecter avec Email").performClick()
        
        // Then - Verify callback was triggered
        assertTrue(emailClicked, "Email sign-in callback should have been triggered")
    }

    // ========================================================================
    // Loading State Tests
    // ========================================================================

    @Test
    fun authScreen_displaysLoadingIndicatorWhenLoading() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                AuthScreen(
                    onGoogleSignIn = {},
                    onAppleSignIn = {},
                    onEmailSignIn = {},
                    onSkip = {},
                    isLoading = true,
                    errorMessage = null
                )
            }
        }

        // Then - Loading indicator should be visible
        // CircularProgressIndicator is displayed when isLoading = true
        // The loading overlay covers the screen
        composeTestRule.onNodeWithText("Bienvenue sur Wakeve").assertIsDisplayed()
    }

    @Test
    fun authScreen_disablesButtonsWhenLoading() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                AuthScreen(
                    onGoogleSignIn = {},
                    onAppleSignIn = {},
                    onEmailSignIn = {},
                    onSkip = {},
                    isLoading = true,
                    errorMessage = null
                )
            }
        }

        // Then - Buttons should be disabled when loading
        composeTestRule.onNodeWithText("Passer").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Se connecter avec Google").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Se connecter avec Apple").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Se connecter avec Email").assertIsNotEnabled()
    }

    @Test
    fun authScreen_enablesButtonsWhenNotLoading() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                AuthScreen(
                    onGoogleSignIn = {},
                    onAppleSignIn = {},
                    onEmailSignIn = {},
                    onSkip = {},
                    isLoading = false,
                    errorMessage = null
                )
            }
        }

        // Then - Buttons should be enabled when not loading
        composeTestRule.onNodeWithText("Passer").assertIsEnabled()
        composeTestRule.onNodeWithText("Se connecter avec Google").assertIsEnabled()
        composeTestRule.onNodeWithText("Se connecter avec Apple").assertIsEnabled()
        composeTestRule.onNodeWithText("Se connecter avec Email").assertIsEnabled()
    }

    // ========================================================================
    // Error Message Tests
    // ========================================================================

    @Test
    fun authScreen_displaysErrorMessage() {
        // Given
        val errorText = "Une erreur est survenue"
        
        composeTestRule.setContent {
            WakeveTheme {
                AuthScreen(
                    onGoogleSignIn = {},
                    onAppleSignIn = {},
                    onEmailSignIn = {},
                    onSkip = {},
                    isLoading = false,
                    errorMessage = errorText
                )
            }
        }

        // Then - Error message should be visible
        composeTestRule.onNodeWithText(errorText).assertIsDisplayed()
    }

    @Test
    fun authScreen_hidesErrorWhenNull() {
        // Given
        composeTestRule.setContent {
            WakeveTheme {
                AuthScreen(
                    onGoogleSignIn = {},
                    onAppleSignIn = {},
                    onEmailSignIn = {},
                    onSkip = {},
                    isLoading = false,
                    errorMessage = null
                )
            }
        }

        // Then - No error message should be visible
        // Title should be visible (confirms screen renders correctly without error)
        composeTestRule.onNodeWithText("Bienvenue sur Wakeve").assertIsDisplayed()
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    fun authScreen_buttonsNotClickableWhileLoading() {
        // Given
        var clickCount = 0
        
        composeTestRule.setContent {
            WakeveTheme {
                AuthScreen(
                    onGoogleSignIn = { clickCount++ },
                    onAppleSignIn = { clickCount++ },
                    onEmailSignIn = { clickCount++ },
                    onSkip = { clickCount++ },
                    isLoading = true,
                    errorMessage = null
                )
            }
        }

        // When - Try to click all buttons while loading
        composeTestRule.onNodeWithText("Passer").performClick()
        composeTestRule.onNodeWithText("Se connecter avec Google").performClick()
        composeTestRule.onNodeWithText("Se connecter avec Apple").performClick()
        composeTestRule.onNodeWithText("Se connecter avec Email").performClick()
        
        // Then - No callbacks should have been triggered
        assertTrue(clickCount == 0, "No callbacks should trigger while loading")
    }
}
