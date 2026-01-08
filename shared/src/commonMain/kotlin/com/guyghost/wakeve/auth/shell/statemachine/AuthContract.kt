package com.guyghost.wakeve.auth.shell.statemachine

import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.User

/**
 * Contract for the Authentication State Machine.
 * 
 * This contract defines:
 * - State: All possible states of the auth flow
 * - Intent: All possible actions/triggers from the UI
 * - SideEffect: One-shot events (navigation, toasts, etc.)
 */
object AuthContract {

    /**
     * Represents the current state of the authentication flow.
     */
    data class State(
        val isLoading: Boolean = false,
        val currentUser: User? = null,
        val isAuthenticated: Boolean = false,
        val isGuest: Boolean = false,
        val lastError: AuthError? = null,
        val authMethod: AuthMethod? = null,
        val pendingEmail: String? = null,
        val remainingOTPTime: Int = 0,
        val otpAttemptsRemaining: Int = 0,
        val showEmailInput: Boolean = false,
        val showOTPInput: Boolean = false,
        val isProviderAvailable: Map<AuthMethod, Boolean> = emptyMap()
    ) {
        companion object {
            /**
             * Initial state when no auth attempt has been made.
             */
            fun initial() = State()
        }

        /**
         * Returns true if the state is in a loading/processing state.
         */
        val isProcessing: Boolean
            get() = isLoading || showEmailInput || showOTPInput

        /**
         * Returns the display name for the current user.
         */
        val displayName: String
            get() = currentUser?.displayName ?: "Invité"

        /**
         * Returns true if we can show the main error message.
         */
        val canShowError: Boolean
            get() = lastError != null && !isLoading
    }

    /**
     * Intents (user actions or UI events) that can trigger state transitions.
     */
    sealed interface Intent {
        /**
         * User initiates Google Sign-In.
         */
        data object SignInWithGoogle : Intent

        /**
         * User initiates Apple Sign-In.
         */
        data object SignInWithApple : Intent

        /**
         * User taps "Sign in with Email" button.
         */
        data object SignInWithEmail : Intent

        /**
         * User submits their email address.
         */
        data class SubmitEmail(val email: String) : Intent

        /**
         * User submits the OTP code.
         */
        data class SubmitOTP(val otp: String) : Intent

        /**
         * User taps "Passer" (Skip) button to enter guest mode.
         */
        data object SkipToGuest : Intent

        /**
         * User requests a new OTP code.
         */
        data object ResendOTP : Intent

        /**
         * User goes back from email/OTP input to main auth screen.
         */
        data object GoBack : Intent

        /**
         * Check existing session on app start.
         */
        data object CheckExistingSession : Intent

        /**
         * Clear any error state.
         */
        data object ClearError : Intent

        /**
         * Sign out the current user.
         */
        data object SignOut : Intent
    }

    /**
     * Side effects - one-shot events that the UI should handle.
     */
    sealed interface SideEffect {
        /**
         * Navigate to the main app screen.
         */
        data object NavigateToMain : SideEffect

        /**
         * Navigate to the onboarding flow (after skip).
         */
        data object NavigateToOnboarding : SideEffect

        /**
         * Show an error toast/snackbar.
         */
        data class ShowError(val message: String) : SideEffect

        /**
         * Show a success toast/snackbar.
         */
        data class ShowSuccess(val message: String) : SideEffect

        /**
         * Show OTP input screen with timer.
         */
        data class ShowOTPInput(val email: String, val remainingSeconds: Int) : SideEffect

        /**
         * Navigate to email input screen.
         */
        data object NavigateToEmailInput : SideEffect

        /**
         * Navigate back to main auth screen.
         */
        data object NavigateBack : SideEffect

        /**
         * Trigger haptic feedback.
         */
        data object HapticFeedback : SideEffect

        /**
         * Animate the success state.
         */
        data object AnimateSuccess : SideEffect
    }
}
