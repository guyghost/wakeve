package com.guyghost.wakeve.auth.shell.statemachine

import com.guyghost.wakeve.auth.core.logic.validateEmail
import com.guyghost.wakeve.auth.core.logic.currentTimeMillis
import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.shell.services.AuthService
import com.guyghost.wakeve.auth.shell.services.EmailAuthService
import com.guyghost.wakeve.auth.shell.services.GuestModeService
import com.guyghost.wakeve.auth.shell.services.AccountDeletionGateway
import com.guyghost.wakeve.auth.shell.services.UnconfiguredAccountDeletionGateway
import com.guyghost.wakeve.auth.shell.services.TokenStorage
import com.guyghost.wakeve.auth.shell.services.TokenKeys
import com.guyghost.wakeve.auth.shell.statemachine.AuthContract.Intent
import com.guyghost.wakeve.auth.shell.statemachine.AuthContract.SideEffect
import com.guyghost.wakeve.auth.shell.statemachine.AuthContract.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * State Machine for the Authentication Flow.
 * 
 * This state machine manages the complete auth flow:
 * - OAuth sign-in (Google, Apple)
 * - Email + OTP authentication
 * - Guest mode (skip authentication)
 * - Session restoration
 * - Error handling
 * 
 * Architecture:
 * - Single source of truth for auth state
 * - Unidirectional data flow (UI → Intent → StateMachine → State → UI)
 * - Side effects for one-shot events (navigation, toasts)
 * 
 * @param authService Service for OAuth authentication
 * @param emailAuthService Service for email OTP flow
 * @param guestModeService Service for guest mode
 * @param tokenStorage Secure token storage
 * @param scope Coroutine scope for state machine operations
 */
class AuthStateMachine(
    private val authService: AuthService,
    private val emailAuthService: EmailAuthService,
    private val guestModeService: GuestModeService,
    private val tokenStorage: TokenStorage,
    private val accountDeletionGateway: AccountDeletionGateway = UnconfiguredAccountDeletionGateway(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _state = MutableStateFlow(State.initial())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _sideEffect = Channel<SideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        // Check for existing session on init
        scope.launch {
            handleIntent(Intent.CheckExistingSession)
        }
    }

    /**
     * Processes an intent and updates the state accordingly.
     * 
     * @param intent The user action to process
     */
    fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.SignInWithGoogle -> handleGoogleSignIn()
            is Intent.SignInWithApple -> handleAppleSignIn()
            is Intent.SignInWithEmail -> handleShowEmailInput()
            is Intent.SubmitEmail -> handleSubmitEmail(intent.email)
            is Intent.SubmitOTP -> handleSubmitOTP(intent.otp)
            is Intent.SkipToGuest -> handleSkipToGuest()
            is Intent.ResendOTP -> handleResendOTP()
            is Intent.GoBack -> handleGoBack()
            is Intent.CheckExistingSession -> handleCheckSession()
            is Intent.ClearError -> handleClearError()
            is Intent.SignOut -> handleSignOut()
            is Intent.DeleteAccount -> handleDeleteAccount()
            is Intent.DeleteGuestData -> handleDeleteGuestData()
        }
    }

    /**
     * Updates the current state.
     */
    private fun updateState(reducer: State.() -> State) {
        _state.value = _state.value.reducer()
    }

    /**
     * Emits a side effect.
     */
    private fun emitSideEffect(effect: SideEffect) {
        scope.launch {
            _sideEffect.send(effect)
        }
    }

    private fun handleGoogleSignIn() {
        updateState { copy(isLoading = true, lastError = null) }

        scope.launch {
            val result = authService.signInWithGoogle()
            processAuthResult(result)
        }
    }

    private fun handleAppleSignIn() {
        updateState { copy(isLoading = true, lastError = null) }

        scope.launch {
            val result = authService.signInWithApple()
            processAuthResult(result)
        }
    }

    private fun handleShowEmailInput() {
        updateState { copy(showEmailInput = true, showOTPInput = false) }
        emitSideEffect(SideEffect.NavigateToEmailInput)
    }

    private fun handleSubmitEmail(email: String) {
        // Validate email first
        val validationResult = validateEmail(email)
        if (validationResult.isFailure) {
            updateState { copy(lastError = validationResult.errorOrNull) }
            emitSideEffect(SideEffect.ShowError(validationResult.errorOrNull?.userMessage ?: "Email invalide"))
            return
        }

        updateState { copy(isLoading = true, pendingEmail = email) }

        scope.launch {
            emailAuthService.requestOTP(email).fold(
                onSuccess = {
                    updateState {
                        copy(
                            isLoading = false,
                            showEmailInput = false,
                            showOTPInput = true,
                            remainingOTPTime = 300 // 5 minutes
                        )
                    }
                    emitSideEffect(SideEffect.ShowOTPInput(email, 300))
                },
                onFailure = { error ->
                    updateState { copy(isLoading = false, lastError = AuthError.NetworkError) }
                    emitSideEffect(SideEffect.ShowError("Erreur lors de l'envoi du code"))
                }
            )
        }
    }

    private fun handleSubmitOTP(otp: String) {
        val pendingEmail = _state.value.pendingEmail ?: return

        updateState { copy(isLoading = true, lastError = null) }

        scope.launch {
            val result = emailAuthService.verifyOTP(
                email = pendingEmail,
                otp = otp,
                userCreator = { email ->
                    com.guyghost.wakeve.auth.core.models.User(
                        id = "email_user_${currentTimeMillis()}",
                        email = email,
                        name = null,
                        authMethod = AuthMethod.EMAIL,
                        isGuest = false,
                        createdAt = currentTimeMillis(),
                        lastLoginAt = currentTimeMillis()
                    )
                }
            )

            when (result) {
                is AuthResult.Success -> {
                    // Store tokens
                    tokenStorage.storeString(TokenKeys.ACCESS_TOKEN, result.token.value)
                    tokenStorage.storeString(TokenKeys.TOKEN_EXPIRY, result.token.expiresAt.toString())

                    updateState {
                        copy(
                            isLoading = false,
                            currentUser = result.user,
                            isAuthenticated = true,
                            isGuest = false,
                            authMethod = AuthMethod.EMAIL,
                            showOTPInput = false
                        )
                    }
                    emitSideEffect(SideEffect.AnimateSuccess)
                    emitSideEffect(SideEffect.NavigateToMain)
                }
                is AuthResult.Error -> {
                    val error = result.error
                    if (error is AuthError.InvalidOTP) {
                        updateState {
                            copy(
                                isLoading = false,
                                lastError = error,
                                otpAttemptsRemaining = error.attemptsRemaining
                            )
                        }
                    } else {
                        updateState { copy(isLoading = false, lastError = error) }
                    }
                    emitSideEffect(SideEffect.ShowError(error.userMessage))
                    emitSideEffect(SideEffect.HapticFeedback)
                }
                is AuthResult.Guest -> {
                    // Not expected for email auth
                    updateState { copy(isLoading = false) }
                }
            }
        }
    }

    private fun handleSkipToGuest() {
        updateState { copy(isLoading = true) }

        scope.launch {
            val result = guestModeService.createGuestSession()

            when (result) {
                is AuthResult.Guest -> {
                    updateState {
                        copy(
                            isLoading = false,
                            currentUser = result.guestUser,
                            isAuthenticated = false,
                            isGuest = true
                        )
                    }
                    emitSideEffect(SideEffect.NavigateToOnboarding)
                }
                is AuthResult.Error -> {
                    updateState { copy(isLoading = false, lastError = result.error) }
                    emitSideEffect(SideEffect.ShowError("Erreur lors de la création du mode invité"))
                }
                else -> {
                    updateState { copy(isLoading = false) }
                }
            }
        }
    }

    private fun handleResendOTP() {
        val pendingEmail = _state.value.pendingEmail ?: return

        scope.launch {
            emailAuthService.resendOTP(pendingEmail).fold(
                onSuccess = {
                    updateState { copy(remainingOTPTime = 300) }
                    emitSideEffect(SideEffect.ShowSuccess("Nouveau code envoyé"))
                },
                onFailure = {
                    emitSideEffect(SideEffect.ShowError("Erreur lors de l'envoi du nouveau code"))
                }
            )
        }
    }

    private fun handleGoBack() {
        val hadOTPInput = _state.value.showOTPInput
        val hadEmailInput = _state.value.showEmailInput

        updateState {
            copy(
                showEmailInput = false,
                showOTPInput = false,
                pendingEmail = null,
                lastError = null
            )
        }

        if (hadOTPInput || hadEmailInput) {
            emitSideEffect(SideEffect.NavigateBack)
        }
    }

    private fun handleCheckSession() {
        scope.launch {
            // Check for existing authenticated session
            val isAuthenticated = authService.isAuthenticated()
            if (isAuthenticated) {
                val user = authService.getCurrentUser()
                if (user != null) {
                    updateState {
                        copy(
                            currentUser = user,
                            isAuthenticated = true,
                            isGuest = false
                        )
                    }
                    emitSideEffect(SideEffect.NavigateToMain)
                    return@launch
                }
            }

            // Check for existing guest session
            val hasGuestSession = guestModeService.hasGuestSession()
            if (hasGuestSession) {
                val guestResult = guestModeService.restoreGuestSession()
                if (guestResult is AuthResult.Guest) {
                    updateState {
                        copy(
                            currentUser = guestResult.guestUser,
                            isAuthenticated = false,
                            isGuest = true
                        )
                    }
                    emitSideEffect(SideEffect.NavigateToOnboarding)
                    return@launch
                }
            }

            // Check provider availability
            val googleAvailable = authService.isProviderAvailable(AuthMethod.GOOGLE)
            val appleAvailable = authService.isProviderAvailable(AuthMethod.APPLE)

            updateState {
                copy(
                    isProviderAvailable = mapOf(
                        AuthMethod.GOOGLE to googleAvailable,
                        AuthMethod.APPLE to appleAvailable
                    )
                )
            }
        }
    }

    private fun handleClearError() {
        updateState { copy(lastError = null) }
    }

    private fun handleSignOut() {
        scope.launch {
            authService.signOut()
            guestModeService.endGuestSession()

            updateState {
                State.initial().copy(
                    isProviderAvailable = isProviderAvailable
                )
            }

            emitSideEffect(SideEffect.NavigateBack)
        }
    }

    private fun handleDeleteAccount() {
        if (_state.value.isGuest) {
            handleDeleteGuestData()
            return
        }

        updateState { copy(isLoading = true, lastError = null) }

        scope.launch {
            val accessToken = tokenStorage.getString(TokenKeys.ACCESS_TOKEN)
            if (accessToken.isNullOrBlank()) {
                updateState { copy(isLoading = false) }
                emitSideEffect(SideEffect.ShowError("Authentification requise"))
                return@launch
            }

            accountDeletionGateway.deleteAccount(accessToken).fold(
                onSuccess = {
                    completeLocalAccountDeletion()
                    emitSideEffect(SideEffect.ShowSuccess("Compte supprimé"))
                    emitSideEffect(SideEffect.NavigateToAuthAfterDeletion)
                },
                onFailure = {
                    updateState { copy(isLoading = false) }
                    emitSideEffect(SideEffect.ShowError("Impossible de supprimer le compte. Réessayez."))
                }
            )
        }
    }

    private fun handleDeleteGuestData() {
        updateState { copy(isLoading = true, lastError = null) }

        scope.launch {
            completeLocalAccountDeletion()
            emitSideEffect(SideEffect.ShowSuccess("Données invité supprimées"))
            emitSideEffect(SideEffect.NavigateToAuthAfterDeletion)
        }
    }

    private suspend fun completeLocalAccountDeletion() {
        tokenStorage.clearAll()
        guestModeService.endGuestSession()
        authService.signOut()

        updateState {
            State.initial().copy(
                isProviderAvailable = isProviderAvailable
            )
        }
    }

    private fun processAuthResult(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                // Store tokens securely
                scope.launch {
                    tokenStorage.storeString(TokenKeys.ACCESS_TOKEN, result.token.value)
                    tokenStorage.storeString(TokenKeys.TOKEN_EXPIRY, result.token.expiresAt.toString())
                    tokenStorage.storeString(TokenKeys.USER_ID, result.user.id)
                    tokenStorage.storeString(TokenKeys.AUTH_METHOD, result.user.authMethod.name)
                }

                updateState {
                    copy(
                        isLoading = false,
                        currentUser = result.user,
                        isAuthenticated = true,
                        isGuest = false,
                        authMethod = result.user.authMethod
                    )
                }
                emitSideEffect(SideEffect.AnimateSuccess)
                emitSideEffect(SideEffect.NavigateToMain)
            }
            is AuthResult.Error -> {
                updateState {
                    copy(isLoading = false, lastError = result.error)
                }
                emitSideEffect(SideEffect.ShowError(result.error.userMessage))
            }
            is AuthResult.Guest -> {
                // Should not happen for OAuth flows
                updateState { copy(isLoading = false) }
            }
        }
    }
}
