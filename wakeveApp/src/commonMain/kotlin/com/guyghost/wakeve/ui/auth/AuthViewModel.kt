package com.guyghost.wakeve.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.auth.shell.services.AuthService
import com.guyghost.wakeve.auth.shell.services.EmailAuthService
import com.guyghost.wakeve.auth.shell.services.GuestModeService
import com.guyghost.wakeve.auth.shell.services.TokenStorage
import com.guyghost.wakeve.auth.shell.statemachine.AuthContract
import com.guyghost.wakeve.auth.shell.statemachine.AuthStateMachine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Authentication screens.
 * 
 * This ViewModel:
 * - Manages auth state using AuthStateMachine
 * - Provides UI state for Compose screens
 * - Handles navigation based on auth results
 * 
 * @param authService Service for OAuth authentication
 * @param emailAuthService Service for email OTP flow
 * @param guestModeService Service for guest mode
 * @param tokenStorage Secure token storage
 */
class AuthViewModel(
    private val authService: AuthService,
    private val emailAuthService: EmailAuthService,
    private val guestModeService: GuestModeService,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val stateMachine = AuthStateMachine(
        authService = authService,
        emailAuthService = emailAuthService,
        guestModeService = guestModeService,
        tokenStorage = tokenStorage,
        scope = viewModelScope
    )

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Observe state machine state
        viewModelScope.launch {
            stateMachine.state.collect { state ->
                _uiState.value = AuthUiState(
                    isLoading = state.isLoading,
                    isAuthenticated = state.isAuthenticated,
                    isGuest = state.isGuest,
                    currentUser = state.currentUser,
                    errorMessage = state.lastError?.userMessage,
                    showEmailInput = state.showEmailInput,
                    showOTPInput = state.showOTPInput,
                    pendingEmail = state.pendingEmail,
                    remainingOTPTime = state.remainingOTPTime,
                    otpAttemptsRemaining = state.otpAttemptsRemaining,
                    authMethod = state.authMethod
                )
            }
        }

        // Observe side effects
        viewModelScope.launch {
            stateMachine.sideEffect.collect { effect ->
                handleSideEffect(effect)
            }
        }
    }

    /**
     * Handles side effects from the state machine.
     */
    private fun handleSideEffect(effect: AuthContract.SideEffect) {
        when (effect) {
            is AuthContract.SideEffect.NavigateToMain -> {
                _uiState.value = _uiState.value.copy(
                    navigationEvent = NavigationEvent.NavigateToMain
                )
            }
            is AuthContract.SideEffect.NavigateToOnboarding -> {
                _uiState.value = _uiState.value.copy(
                    navigationEvent = NavigationEvent.NavigateToOnboarding
                )
            }
            is AuthContract.SideEffect.ShowError -> {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = effect.message
                )
            }
            is AuthContract.SideEffect.ShowSuccess -> {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = effect.message
                )
            }
            is AuthContract.SideEffect.ShowOTPInput -> {
                _uiState.value = _uiState.value.copy(
                    pendingEmail = effect.email,
                    remainingOTPTime = effect.remainingSeconds,
                    showEmailInput = false,
                    showOTPInput = true
                )
            }
            else -> {
                // Other effects can be handled if needed
            }
        }
    }

    /**
     * Processes Google Sign-In intent.
     */
    fun onGoogleSignIn() {
        stateMachine.handleIntent(AuthContract.Intent.SignInWithGoogle)
    }

    /**
     * Processes Apple Sign-In intent.
     */
    fun onAppleSignIn() {
        stateMachine.handleIntent(AuthContract.Intent.SignInWithApple)
    }

    /**
     * Processes Email Sign-In intent.
     */
    fun onEmailSignIn() {
        stateMachine.handleIntent(AuthContract.Intent.SignInWithEmail)
    }

    /**
     * Processes email submission.
     */
    fun onSubmitEmail(email: String) {
        stateMachine.handleIntent(AuthContract.Intent.SubmitEmail(email))
    }

    /**
     * Processes OTP submission.
     */
    fun onSubmitOTP(otp: String) {
        stateMachine.handleIntent(AuthContract.Intent.SubmitOTP(otp))
    }

    /**
     * Processes Skip to Guest intent.
     */
    fun onSkipToGuest() {
        stateMachine.handleIntent(AuthContract.Intent.SkipToGuest)
    }

    /**
     * Processes OTP resend request.
     */
    fun onResendOTP() {
        stateMachine.handleIntent(AuthContract.Intent.ResendOTP)
    }

    /**
     * Processes back navigation.
     */
    fun onBack() {
        stateMachine.handleIntent(AuthContract.Intent.GoBack)
    }

    /**
     * Processes error dismissal.
     */
    fun onClearError() {
        stateMachine.handleIntent(AuthContract.Intent.ClearError)
    }

    /**
     * Clears the navigation event after it's been handled.
     */
    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(navigationEvent = null)
    }

    /**
     * Clears the snackbar message after it's been shown.
     */
    fun onSnackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    /**
     * Check existing session on app start.
     */
    fun checkExistingSession() {
        stateMachine.handleIntent(AuthContract.Intent.CheckExistingSession)
    }
}

/**
 * UI State for authentication screens.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isGuest: Boolean = false,
    val currentUser: com.guyghost.wakeve.auth.core.models.User? = null,
    val errorMessage: String? = null,
    val showEmailInput: Boolean = false,
    val showOTPInput: Boolean = false,
    val pendingEmail: String? = null,
    val remainingOTPTime: Int = 0,
    val otpAttemptsRemaining: Int = 0,
    val authMethod: com.guyghost.wakeve.auth.core.models.AuthMethod? = null,
    val navigationEvent: NavigationEvent? = null,
    val snackbarMessage: String? = null
)

/**
 * Navigation events from auth flow.
 */
sealed class NavigationEvent {
    data object NavigateToMain : NavigationEvent()
    data object NavigateToOnboarding : NavigationEvent()
}
