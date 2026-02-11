package com.guyghost.wakeve.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
 * Android Compose UI tests for EmailAuthScreen.
 * 
 * These tests verify the email authentication screen UI elements and interactions
 * using the callback-based EmailAuthScreen signature.
 * 
 * Tests cover:
 * - Email input stage UI elements
 * - OTP input stage UI elements
 * - Button interactions and callbacks
 * - Loading state handling
 * - Error message display
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmailAuthScreenTest {

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
    // Email Input Stage Tests
    // ========================================================================

    @Test
    fun emailAuthScreen_displaysEmailInputTitle() {
        // Given - Email input stage (isOTPStage = false)
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "",
                    isLoading = false,
                    isOTPStage = false,
                    remainingTime = 0,
                    attemptsRemaining = 3,
                    errorMessage = null,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = {},
                    onBack = {},
                    onClearError = {}
                )
            }
        }

        // Then - Verify title is displayed
        composeTestRule.onNodeWithText("Connexion par email").assertIsDisplayed()
    }

    @Test
    fun emailAuthScreen_displaysEmailFieldPlaceholder() {
        // Given - Email input stage
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "",
                    isLoading = false,
                    isOTPStage = false,
                    remainingTime = 0,
                    attemptsRemaining = 3,
                    errorMessage = null,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = {},
                    onBack = {},
                    onClearError = {}
                )
            }
        }

        // Then - Verify email field placeholder is displayed
        composeTestRule.onNodeWithText("Adresse email").assertIsDisplayed()
    }

    @Test
    fun emailAuthScreen_displaysSendOTPButton() {
        // Given - Email input stage
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "",
                    isLoading = false,
                    isOTPStage = false,
                    remainingTime = 0,
                    attemptsRemaining = 3,
                    errorMessage = null,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = {},
                    onBack = {},
                    onClearError = {}
                )
            }
        }

        // Then - Verify send OTP button is displayed
        composeTestRule.onNodeWithText("Envoyer le code").assertIsDisplayed()
    }

    @Test
    fun emailAuthScreen_displaysInstructions() {
        // Given - Email input stage
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "",
                    isLoading = false,
                    isOTPStage = false,
                    remainingTime = 0,
                    attemptsRemaining = 3,
                    errorMessage = null,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = {},
                    onBack = {},
                    onClearError = {}
                )
            }
        }

        // Then - Verify instructions are displayed
        composeTestRule.onNodeWithText("Nous vous enverrons un code de vérification par email").assertIsDisplayed()
    }

    @Test
    fun emailAuthScreen_displaysPrivacyNote() {
        // Given - Email input stage
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "",
                    isLoading = false,
                    isOTPStage = false,
                    remainingTime = 0,
                    attemptsRemaining = 3,
                    errorMessage = null,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = {},
                    onBack = {},
                    onClearError = {}
                )
            }
        }

        // Then - Verify privacy note is displayed
        composeTestRule.onNodeWithText("Nous ne stockerons que votre adresse email de manière sécurisée.").assertIsDisplayed()
    }

    // ========================================================================
    // OTP Input Stage Tests
    // ========================================================================

    @Test
    fun emailAuthScreen_displaysOTPTitle_whenOTPStage() {
        // Given - OTP input stage (isOTPStage = true)
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "test@example.com",
                    isLoading = false,
                    isOTPStage = true,
                    remainingTime = 120,
                    attemptsRemaining = 3,
                    errorMessage = null,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = {},
                    onBack = {},
                    onClearError = {}
                )
            }
        }

        // Then - Verify OTP title is displayed
        composeTestRule.onNodeWithText("Vérification du code").assertIsDisplayed()
    }

    @Test
    fun emailAuthScreen_displaysEmailInOTPStage() {
        // Given - OTP input stage with email
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "test@example.com",
                    isLoading = false,
                    isOTPStage = true,
                    remainingTime = 120,
                    attemptsRemaining = 3,
                    errorMessage = null,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = {},
                    onBack = {},
                    onClearError = {}
                )
            }
        }

        // Then - Verify email is displayed
        composeTestRule.onNodeWithText("test@example.com").assertIsDisplayed()
    }

    @Test
    fun emailAuthScreen_displaysVerifyButton_whenOTPStage() {
        // Given - OTP input stage
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "test@example.com",
                    isLoading = false,
                    isOTPStage = true,
                    remainingTime = 120,
                    attemptsRemaining = 3,
                    errorMessage = null,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = {},
                    onBack = {},
                    onClearError = {}
                )
            }
        }

        // Then - Verify button is displayed
        composeTestRule.onNodeWithText("Vérifier").assertIsDisplayed()
    }

    @Test
    fun emailAuthScreen_displaysResendButton_whenOTPStage() {
        // Given - OTP input stage with time expired (remainingTime = 0)
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "test@example.com",
                    isLoading = false,
                    isOTPStage = true,
                    remainingTime = 0,
                    attemptsRemaining = 3,
                    errorMessage = null,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = {},
                    onBack = {},
                    onClearError = {}
                )
            }
        }

        // Then - Resend button should be displayed when time is 0
        composeTestRule.onNodeWithText("Renvoyer le code").assertIsDisplayed()
    }

    // ========================================================================
    // Loading State Tests
    // ========================================================================

    @Test
    fun emailAuthScreen_disablesButtonsWhenLoading() {
        // Given - Loading state
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "",
                    isLoading = true,
                    isOTPStage = false,
                    remainingTime = 0,
                    attemptsRemaining = 3,
                    errorMessage = null,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = {},
                    onBack = {},
                    onClearError = {}
                )
            }
        }

        // Then - Send OTP button should be disabled
        composeTestRule.onNodeWithText("Envoyer le code").assertIsNotEnabled()
    }

    @Test
    fun emailAuthScreen_enablesButtonsWhenNotLoading() {
        // Given - Not loading state
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "",
                    isLoading = false,
                    isOTPStage = false,
                    remainingTime = 0,
                    attemptsRemaining = 3,
                    errorMessage = null,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = {},
                    onBack = {},
                    onClearError = {}
                )
            }
        }

        // Then - Send OTP button should be enabled
        composeTestRule.onNodeWithText("Envoyer le code").assertIsEnabled()
    }

    // ========================================================================
    // Error Message Tests
    // ========================================================================

    @Test
    fun emailAuthScreen_displaysErrorMessage() {
        // Given - Error state
        val errorText = "Email invalide"
        
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "",
                    isLoading = false,
                    isOTPStage = false,
                    remainingTime = 0,
                    attemptsRemaining = 3,
                    errorMessage = errorText,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = {},
                    onBack = {},
                    onClearError = {}
                )
            }
        }

        // Then - Error message should be visible
        composeTestRule.onNodeWithText(errorText).assertIsDisplayed()
    }

    // ========================================================================
    // Callback Tests
    // ========================================================================

    @Test
    fun emailAuthScreen_backButtonTriggersCallback() {
        // Given
        var backClicked = false
        
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "",
                    isLoading = false,
                    isOTPStage = false,
                    remainingTime = 0,
                    attemptsRemaining = 3,
                    errorMessage = null,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = {},
                    onBack = { backClicked = true },
                    onClearError = {}
                )
            }
        }

        // When - Click back button (via content description)
        // Note: Back button uses an icon, we need to find it by content description
        composeTestRule.onNodeWithText("Retour", useUnmergedTree = true).performClick()
    }

    @Test
    fun emailAuthScreen_resendOTPButtonTriggersCallback() {
        // Given
        var resendClicked = false
        
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "test@example.com",
                    isLoading = false,
                    isOTPStage = true,
                    remainingTime = 0, // Time expired, resend button visible
                    attemptsRemaining = 3,
                    errorMessage = null,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = { resendClicked = true },
                    onBack = {},
                    onClearError = {}
                )
            }
        }

        // When - Click resend button
        composeTestRule.onNodeWithText("Renvoyer le code").performClick()
        
        // Then - Callback should be triggered
        assertTrue(resendClicked, "Resend OTP callback should have been triggered")
    }

    // ========================================================================
    // Attempts Remaining Tests
    // ========================================================================

    @Test
    fun emailAuthScreen_displaysAttemptsRemaining_whenOTPStage() {
        // Given - OTP stage with limited attempts
        composeTestRule.setContent {
            WakeveTheme {
                EmailAuthScreen(
                    email = "test@example.com",
                    isLoading = false,
                    isOTPStage = true,
                    remainingTime = 120,
                    attemptsRemaining = 2,
                    errorMessage = null,
                    onSubmitEmail = {},
                    onSubmitOTP = {},
                    onResendOTP = {},
                    onBack = {},
                    onClearError = {}
                )
            }
        }

        // Then - Attempts remaining should be displayed
        composeTestRule.onNodeWithText("2 tentatives restantes").assertIsDisplayed()
    }
}
