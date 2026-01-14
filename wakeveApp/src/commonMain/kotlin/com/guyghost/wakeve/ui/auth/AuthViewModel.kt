package com.guyghost.wakeve.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.User
import com.guyghost.wakeve.auth.shell.statemachine.AuthContract
import com.guyghost.wakeve.auth.shell.statemachine.AuthStateMachine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the AuthScreen.
 * 
 * Wraps the AuthStateMachine and exposes:
 * - UI state for Compose observation
 * - Side effects for one-shot events (navigation, toasts)
 * - Methods for user actions
 * 
 * Architecture: Imperative Shell (orchestrates auth flow)
 */
class AuthViewModel(
    private val authStateMachine: AuthStateMachine
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val _sideEffects = MutableSharedFlow<AuthSideEffect>()
    val sideEffects: SharedFlow<AuthSideEffect> = _sideEffects.asSharedFlow()
    
    init {
        observeAuthState()
        observeAuthSideEffects()
    }
    
    /**
     * Observe state changes from AuthStateMachine.
     */
    private fun observeAuthState() {
        viewModelScope.launch {
            authStateMachine.state.collect { authState ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = authState.isLoading,
                        isAuthenticated = authState.isAuthenticated,
                        isGuest = authState.isGuest,
                        currentUser = authState.currentUser,
                        errorMessage = authState.lastError?.userMessage,
                        showEmailInput = authState.showEmailInput,
                        showOTPInput = authState.showOTPInput,
                        pendingEmail = authState.pendingEmail,
                        remainingOTPTime = authState.remainingOTPTime,
                        otpAttemptsRemaining = authState.otpAttemptsRemaining
                    )
                }
            }
        }
    }
    
    /**
     * Observe side effects from AuthStateMachine and forward to UI.
     */
    private fun observeAuthSideEffects() {
        viewModelScope.launch {
            authStateMachine.sideEffect.collect { effect ->
                val uiEffect = when (effect) {
                    is AuthContract.SideEffect.NavigateToMain -> AuthSideEffect.NavigateToHome
                    is AuthContract.SideEffect.NavigateToOnboarding -> AuthSideEffect.NavigateToOnboarding
                    is AuthContract.SideEffect.ShowError -> AuthSideEffect.ShowError(effect.message)
                    is AuthContract.SideEffect.ShowSuccess -> AuthSideEffect.ShowSuccess(effect.message)
                    is AuthContract.SideEffect.NavigateToEmailInput -> AuthSideEffect.NavigateToEmailAuth
                    is AuthContract.SideEffect.NavigateBack -> AuthSideEffect.NavigateBack
                    is AuthContract.SideEffect.ShowOTPInput -> AuthSideEffect.ShowOTPInput(effect.email, effect.remainingSeconds)
                    is AuthContract.SideEffect.HapticFeedback -> AuthSideEffect.HapticFeedback
                    is AuthContract.SideEffect.AnimateSuccess -> AuthSideEffect.AnimateSuccess
                }
                _sideEffects.emit(uiEffect)
            }
        }
    }
    
    // ========================================================================
    // User Actions
    // ========================================================================
    
    /**
     * User taps "Sign in with Google" button.
     * Note: The actual OAuth flow is triggered via AuthCallbacks (Activity-level).
     */
    fun onGoogleSignInRequested() {
        authStateMachine.handleIntent(AuthContract.Intent.SignInWithGoogle)
    }
    
    /**
     * User taps "Sign in with Apple" button.
     */
    fun onAppleSignInRequested() {
        authStateMachine.handleIntent(AuthContract.Intent.SignInWithApple)
    }
    
    /**
     * User taps "Sign in with Email" button.
     */
    fun onEmailSignInRequested() {
        authStateMachine.handleIntent(AuthContract.Intent.SignInWithEmail)
    }
    
    /**
     * User taps "Skip" button to enter guest mode.
     */
    fun onSkipAuth() {
        authStateMachine.handleIntent(AuthContract.Intent.SkipToGuest)
    }
    
    /**
     * User submits email address.
     */
    fun onSubmitEmail(email: String) {
        authStateMachine.handleIntent(AuthContract.Intent.SubmitEmail(email))
    }
    
    /**
     * User submits OTP code.
     */
    fun onSubmitOTP(otp: String) {
        authStateMachine.handleIntent(AuthContract.Intent.SubmitOTP(otp))
    }
    
    /**
     * User requests a new OTP code.
     */
    fun onResendOTP() {
        authStateMachine.handleIntent(AuthContract.Intent.ResendOTP)
    }
    
    /**
     * User goes back from email/OTP input.
     */
    fun onGoBack() {
        authStateMachine.handleIntent(AuthContract.Intent.GoBack)
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        authStateMachine.handleIntent(AuthContract.Intent.ClearError)
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * User signs out.
     */
    fun signOut() {
        authStateMachine.handleIntent(AuthContract.Intent.SignOut)
    }
}

/**
 * UI State for the AuthScreen.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isGuest: Boolean = false,
    val currentUser: User? = null,
    val errorMessage: String? = null,
    val showEmailInput: Boolean = false,
    val showOTPInput: Boolean = false,
    val pendingEmail: String? = null,
    val remainingOTPTime: Int = 0,
    val otpAttemptsRemaining: Int = 3
) {
    val displayName: String
        get() = currentUser?.displayName ?: "Invité"
    
    val canSubmitEmail: Boolean
        get() = !isLoading && showEmailInput
    
    val canSubmitOTP: Boolean
        get() = !isLoading && showOTPInput && otpAttemptsRemaining > 0
    
    val canResendOTP: Boolean
        get() = !isLoading && showOTPInput && remainingOTPTime <= 0
}

/**
 * Side effects for the AuthScreen.
 * One-shot events that should be handled once by the UI.
 */
sealed class AuthSideEffect {
    data object NavigateToHome : AuthSideEffect()
    data object NavigateToOnboarding : AuthSideEffect()
    data object NavigateToEmailAuth : AuthSideEffect()
    data object NavigateBack : AuthSideEffect()
    data class ShowError(val message: String) : AuthSideEffect()
    data class ShowSuccess(val message: String) : AuthSideEffect()
    data class ShowOTPInput(val email: String, val remainingSeconds: Int) : AuthSideEffect()
    data object HapticFeedback : AuthSideEffect()
    data object AnimateSuccess : AuthSideEffect()
}
