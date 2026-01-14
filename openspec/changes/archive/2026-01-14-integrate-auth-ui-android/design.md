# Design: Integrate Authentication UI on Android

## Architecture Overview

### Current State (Broken)

```
┌─────────────────────────────────────────────────────────────────┐
│                        MainActivity                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ setupOAuthProviders() ✅                                 │    │
│  │ onGoogleSignInClick() ✅ ──────────────────────────────┐│    │
│  │ onAppleSignInClick() ✅                                ││    │
│  │ handleAuthResult() ✅                                  ││    │
│  └─────────────────────────────────────────────────────────┘│    │
│                                                    ▼NEVER CALLED│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                           App.kt                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ AuthStateManager ✅ (initialized)                        │    │
│  │ authState.collect() ✅                                   │    │
│  │ BUT: Only checks, doesn't dispatch intents              │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        WakevNavHost                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ LoginScreen(                                             │    │
│  │     onGoogleSignIn = { /* TODO */ },  ❌ STUB            │    │
│  │     onAppleSignIn = { /* N/A */ }     ❌ STUB            │    │
│  │ )                                                        │    │
│  │                                                          │    │
│  │ AuthScreen EXISTS but NEVER USED ❌                      │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### Target State (Fixed)

```
┌─────────────────────────────────────────────────────────────────┐
│                        MainActivity                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ AuthCallbackProvider (interface)                         │    │
│  │   ├─ launchGoogleSignIn()                               │    │
│  │   ├─ launchAppleSignIn() [disabled on Android]          │    │
│  │   └─ handleAuthResult()                                 │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │ provides via CompositionLocal     │
└──────────────────────────────┼──────────────────────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                           App.kt                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ authState = authStateManager.authState.collectAsState()  │    │
│  │                                                          │    │
│  │ LaunchedEffect(authState) {                              │    │
│  │   when (authState) {                                     │    │
│  │     Unauthenticated → navigate(Auth)                     │    │
│  │     Authenticated → navigate(Home or Onboarding)         │    │
│  │   }                                                      │    │
│  │ }                                                        │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                        WakevNavHost                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ composable(Screen.Auth.route) {                          │    │
│  │     val viewModel: AuthViewModel = koinInject()          │    │
│  │     val callbacks = LocalAuthCallbacks.current           │    │
│  │                                                          │    │
│  │     AuthScreen(                                          │    │
│  │         onGoogleSignIn = { callbacks.launchGoogleSignIn()│    │
│  │         onAppleSignIn = { /* disabled */ },              │    │
│  │         onEmailSignIn = { navController.navigate(Email) }│    │
│  │         onSkip = { viewModel.skipAuth() },               │    │
│  │         isLoading = viewModel.state.isLoading,           │    │
│  │         errorMessage = viewModel.state.error             │    │
│  │     )                                                    │    │
│  │ }                                                        │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                        AuthViewModel                             │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ private val authStateMachine: AuthStateMachine           │    │
│  │                                                          │    │
│  │ val state: StateFlow<AuthUiState>                        │    │
│  │ val sideEffects: SharedFlow<AuthSideEffect>              │    │
│  │                                                          │    │
│  │ fun skipAuth() {                                         │    │
│  │     authStateMachine.dispatch(Intent.SkipAuth)           │    │
│  │ }                                                        │    │
│  │                                                          │    │
│  │ fun handleOAuthResult(result: AuthResult) {              │    │
│  │     // Map to state machine intent                       │    │
│  │ }                                                        │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                      AuthStateMachine                            │
│               (Already implemented - shared module)              │
└─────────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. LocalAuthCallbacks (CompositionLocal)

```kotlin
// navigation/AuthCallbacks.kt

/**
 * Interface for OAuth callbacks that require Activity context.
 */
interface AuthCallbacks {
    /**
     * Launch Google Sign-In flow.
     * Will trigger GoogleSignInClient and return result via handleOAuthResult.
     */
    fun launchGoogleSignIn()
    
    /**
     * Launch Apple Sign-In flow (web-based on Android).
     * Opens Custom Tab with Apple OAuth URL.
     */
    fun launchAppleSignIn()
}

/**
 * CompositionLocal providing AuthCallbacks from MainActivity.
 */
val LocalAuthCallbacks = staticCompositionLocalOf<AuthCallbacks> {
    error("No AuthCallbacks provided")
}
```

### 2. AuthViewModel

```kotlin
// ui/auth/AuthViewModel.kt

class AuthViewModel(
    private val authStateMachine: AuthStateMachine,
    private val secureTokenStorage: SecureTokenStorage
) : ViewModel() {
    
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    
    private val _sideEffects = MutableSharedFlow<AuthSideEffect>()
    val sideEffects: SharedFlow<AuthSideEffect> = _sideEffects.asSharedFlow()
    
    init {
        observeAuthState()
        observeSideEffects()
    }
    
    private fun observeAuthState() {
        viewModelScope.launch {
            authStateMachine.state.collect { authState ->
                _state.update { current ->
                    current.copy(
                        isLoading = authState.isLoading,
                        isAuthenticated = authState.isAuthenticated,
                        isGuest = authState.isGuest,
                        currentUser = authState.currentUser,
                        errorMessage = authState.error?.message
                    )
                }
            }
        }
    }
    
    private fun observeSideEffects() {
        viewModelScope.launch {
            authStateMachine.sideEffects.collect { effect ->
                when (effect) {
                    is AuthContract.SideEffect.NavigateToMain -> 
                        _sideEffects.emit(AuthSideEffect.NavigateToHome)
                    is AuthContract.SideEffect.NavigateToOnboarding -> 
                        _sideEffects.emit(AuthSideEffect.NavigateToOnboarding)
                    is AuthContract.SideEffect.ShowError -> 
                        _sideEffects.emit(AuthSideEffect.ShowError(effect.message))
                }
            }
        }
    }
    
    fun skipAuth() {
        authStateMachine.dispatch(AuthContract.Intent.SkipAuth)
    }
    
    fun handleOAuthSuccess(user: User, token: AuthToken) {
        viewModelScope.launch {
            secureTokenStorage.storeAccessToken(token.value)
            secureTokenStorage.storeTokenExpiry(token.expiresAt)
            authStateMachine.dispatch(
                AuthContract.Intent.OAuthSuccess(user, token)
            )
        }
    }
    
    fun handleOAuthError(error: AuthError) {
        authStateMachine.dispatch(AuthContract.Intent.OAuthError(error))
    }
    
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isGuest: Boolean = false,
    val currentUser: User? = null,
    val errorMessage: String? = null
)

sealed class AuthSideEffect {
    object NavigateToHome : AuthSideEffect()
    object NavigateToOnboarding : AuthSideEffect()
    data class ShowError(val message: String) : AuthSideEffect()
}
```

### 3. Modified App.kt

```kotlin
@Composable
fun App() {
    val context = LocalContext.current
    val navController = rememberNavController()
    
    // Auth state from AuthStateManager
    val authStateManager = remember { 
        AuthStateManager(
            secureStorage = AndroidSecureTokenStorage(context),
            authService = AndroidAuthenticationService(context),
            enableOAuth = true
        )
    }
    val authState by authStateManager.authState.collectAsState()
    val hasOnboarded by remember { mutableStateOf(hasCompletedOnboarding(context)) }
    
    // Reactive navigation based on auth state
    LaunchedEffect(authState, hasOnboarded) {
        when (authState) {
            is AuthState.Loading -> {
                // Stay on splash or show loading
            }
            is AuthState.Unauthenticated -> {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.Authenticated -> {
                val destination = if (hasOnboarded) Screen.Home.route else Screen.Onboarding.route
                navController.navigate(destination) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.Error -> {
                // Show error on current screen, don't navigate away
            }
        }
    }
    
    // ... rest of scaffold with WakevNavHost
}
```

### 4. Modified WakevNavHost.kt (Auth Section)

```kotlin
// Auth & Onboarding section in WakevNavHost

composable(Screen.Auth.route) {
    val viewModel: AuthViewModel = koinInject()
    val state by viewModel.state.collectAsState()
    val callbacks = LocalAuthCallbacks.current
    
    // Handle side effects for navigation
    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is AuthSideEffect.NavigateToHome -> {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
                is AuthSideEffect.NavigateToOnboarding -> {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
                is AuthSideEffect.ShowError -> {
                    // Handled by state.errorMessage
                }
            }
        }
    }
    
    AuthScreen(
        onGoogleSignIn = { callbacks.launchGoogleSignIn() },
        onAppleSignIn = { /* Not available on Android */ },
        onEmailSignIn = { 
            navController.navigate(Screen.EmailAuth.route) 
        },
        onSkip = { viewModel.skipAuth() },
        isLoading = state.isLoading,
        errorMessage = state.errorMessage
    )
}

composable(Screen.EmailAuth.route) {
    val viewModel: AuthViewModel = koinInject()
    val state by viewModel.state.collectAsState()
    
    EmailAuthScreen(
        email = state.email ?: "",
        isLoading = state.isLoading,
        isOTPStage = state.isOTPStage,
        remainingTime = state.otpRemainingTime,
        attemptsRemaining = state.otpAttemptsRemaining,
        errorMessage = state.errorMessage,
        onSubmitEmail = { email -> viewModel.requestEmailOTP(email) },
        onSubmitOTP = { otp -> viewModel.verifyEmailOTP(otp) },
        onResendOTP = { viewModel.resendEmailOTP() },
        onBack = { navController.navigateUp() },
        onClearError = { viewModel.clearError() }
    )
}
```

### 5. Modified MainActivity.kt

```kotlin
class MainActivity : ComponentActivity(), AuthCallbacks {
    
    // ... existing OAuth setup code ...
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initializeKoinWithContext()
        setupOAuthProviders()
        
        setContent {
            // Provide AuthCallbacks via CompositionLocal
            CompositionLocalProvider(
                LocalAuthCallbacks provides this@MainActivity
            ) {
                App()
            }
        }
    }
    
    // Implement AuthCallbacks interface
    override fun launchGoogleSignIn() {
        lifecycleScope.launch {
            try {
                val authService = createAuthService()
                googleSignInClient = authService.createGoogleSignInClient(
                    this@MainActivity,
                    GOOGLE_WEB_CLIENT_ID
                )
                
                googleSignInClient?.let { client ->
                    val signInIntent = authService.getGoogleSignInIntent(client)
                    signInIntent?.let { googleSignInLauncher.launch(it) }
                } ?: showToast("Google Sign-In non configuré")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error launching Google Sign-In", e)
                notifyAuthError(AuthError.OAuthError(e.message ?: "Unknown error"))
            }
        }
    }
    
    override fun launchAppleSignIn() {
        // Apple Sign-In not natively available on Android
        // Could implement web-based flow via Custom Tabs
        showToast("Apple Sign-In non disponible sur Android")
    }
    
    /**
     * Notify AuthViewModel of OAuth result.
     * Called from googleSignInLauncher result handler.
     */
    private fun notifyAuthResult(result: AuthResult) {
        // Get AuthViewModel from Koin and forward result
        val viewModel: AuthViewModel = GlobalContext.get().get()
        when (result) {
            is AuthResult.Success -> viewModel.handleOAuthSuccess(result.user, result.token)
            is AuthResult.Guest -> viewModel.handleGuestCreated(result.guestUser)
            is AuthResult.Error -> viewModel.handleOAuthError(result.error)
        }
    }
    
    private fun notifyAuthError(error: AuthError) {
        val viewModel: AuthViewModel = GlobalContext.get().get()
        viewModel.handleOAuthError(error)
    }
}
```

## Navigation Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           APP LAUNCH                                      │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                         Check Stored Token                                │
│                                                                          │
│  SecureTokenStorage.getAccessToken()                                     │
│         │                                                                │
│         ├── Token exists & valid → AuthState.Authenticated               │
│         ├── Token exists & expired → Try refresh                         │
│         └── No token → AuthState.Unauthenticated                         │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
        ┌───────────────────┐           ┌───────────────────┐
        │  Unauthenticated  │           │   Authenticated   │
        └───────────────────┘           └───────────────────┘
                    │                               │
                    ▼                               ▼
        ┌───────────────────┐           ┌───────────────────┐
        │   AuthScreen      │           │ Check Onboarding  │
        │                   │           │                   │
        │ ┌─────────────┐   │           │ hasOnboarded?     │
        │ │ Skip Button │───┼───────────┼───────┐           │
        │ └─────────────┘   │           │       │           │
        │                   │           │       ▼           │
        │ ┌─────────────┐   │           │   ┌───────┐       │
        │ │   Google    │───┼───────────┼───│ Home  │       │
        │ └─────────────┘   │           │   └───────┘       │
        │                   │           │       ▲           │
        │ ┌─────────────┐   │           │       │           │
        │ │   Email     │───┼───────────┼───────┤           │
        │ └─────────────┘   │           │       │           │
        └───────────────────┘           │   ┌───────────┐   │
                                        │   │Onboarding │   │
                                        │   └───────────┘   │
                                        │       │           │
                                        │       ▼           │
                                        │   ┌───────┐       │
                                        └───│ Home  │───────┘
                                            └───────┘
```

## Error Handling Strategy

### OAuth Errors

| Error Type | User Message | Action |
|------------|--------------|--------|
| `OAuthCancelled` | "Connexion annulée" | Stay on AuthScreen |
| `OAuthError` | "Erreur de connexion: {details}" | Show error, allow retry |
| `NetworkError` | "Vérifiez votre connexion internet" | Show error, allow retry |
| `TokenExpired` | (silent) | Auto-refresh or re-auth |
| `InvalidCredentials` | "Identifiants invalides" | Show error, clear fields |

### Email OTP Errors

| Error Type | User Message | Action |
|------------|--------------|--------|
| `InvalidEmail` | "Adresse email invalide" | Highlight field |
| `OTPExpired` | "Code expiré, demandez un nouveau code" | Enable resend button |
| `OTPInvalid` | "Code incorrect ({attempts} restants)" | Clear OTP field |
| `TooManyAttempts` | "Trop de tentatives, réessayez plus tard" | Disable for cooldown |

## Security Considerations

1. **Token Storage**: Utilisation de `AndroidSecureTokenStorage` (Keystore-backed)
2. **OAuth State**: Paramètre `state` CSRF généré avec `SecureRandom`
3. **Deep Links**: Validation du scheme et host pour callbacks OAuth
4. **Session Timeout**: Refresh token avant expiration
5. **Logout**: Clear tous les tokens et données sensibles
