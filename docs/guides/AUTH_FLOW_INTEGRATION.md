# Auth Flow Integration Guide

This guide describes how to integrate the authentication flow into the Wakeve application.

## Overview

The authentication flow is managed by `AuthStateMachine`, a state machine that handles:
- OAuth sign-in (Google, Apple)
- Email + OTP authentication
- Guest mode (skip authentication)
- Session restoration
- Error handling

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     UI Layer (Android/iOS)                   │
│  ┌─────────────────┐         ┌─────────────────┐            │
│  │ AuthScreen      │         │ AuthViewModel   │            │
│  │ (Compose/Swift) │  ←→     │ (State Machine) │            │
│  └─────────────────┘         └────────┬────────┘            │
│                                       │                     │
│                              ┌────────▼────────┐            │
│                              │ AuthStateMachine │            │
│                              │ (Shared Core)    │            │
│                              └────────┬────────┘            │
│                                       │                     │
│  ┌────────────────────────────────────▼─────────────────┐   │
│  │              Services Layer (Imperative Shell)         │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │   │
│  │  │AuthService│ │EmailAuth │ │GuestMode │ │TokenStorage│  │   │
│  │  │          │ │Service   │ │Service   │ │          │  │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │   │
│  └────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## State Machine

### States

```kotlin
enum class AuthState {
    Initial,      // No action taken
    Loading,      // Processing authentication
    Authenticated, // Successfully authenticated
    Guest,        // Guest mode active
    Error         // Authentication failed
}
```

### Intents (User Actions)

| Intent | Description |
|--------|-------------|
| `SignInWithGoogle` | Initiate Google OAuth flow |
| `SignInWithApple` | Initiate Apple Sign-In flow |
| `SignInWithEmail` | Show email input screen |
| `SubmitEmail(email)` | Submit email for OTP |
| `SubmitOTP(otp)` | Submit OTP code |
| `SkipToGuest` | Enter guest mode |
| `ResendOTP` | Request new OTP |
| `GoBack` | Navigate back |
| `CheckExistingSession` | Restore previous session |
| `ClearError` | Dismiss error message |
| `SignOut` | Sign out current user |

### Side Effects (One-Shot Events)

| Side Effect | Description |
|-------------|-------------|
| `NavigateToMain` | Navigate to home screen |
| `NavigateToOnboarding` | Navigate to onboarding |
| `ShowError(message)` | Display error toast |
| `ShowSuccess(message)` | Display success toast |
| `ShowOTPInput(email, seconds)` | Show OTP input with timer |
| `NavigateToEmailInput` | Show email input screen |
| `NavigateBack` | Navigate back |
| `HapticFeedback` | Trigger haptic feedback |
| `AnimateSuccess` | Show success animation |

## Sequence Diagrams

### Google Sign-In Flow

```
User → AuthScreen: Tap "Sign in with Google"
AuthScreen → AuthViewModel: signInWithGoogle()
AuthViewModel → AuthStateMachine: Intent.SignInWithGoogle
AuthStateMachine → AuthService: signInWithGoogle()
AuthService → Google SDK: Initiate OAuth
Google SDK → AuthService: Return idToken
AuthStateMachine → processAuthResult()
AuthStateMachine → Update State (Authenticated)
AuthStateMachine → Emit SideEffect.NavigateToMain
AuthViewModel → AuthScreen: navigationEvent = NavigateToMain
AuthScreen → Navigation: Navigate to Home
```

### Email OTP Flow

```
User → AuthScreen: Tap "Sign in with Email"
AuthScreen → AuthViewModel: signInWithEmail()
AuthViewModel → AuthStateMachine: Intent.SignInWithEmail
AuthStateMachine → Update State (showEmailInput = true)
AuthScreen → EmailAuthScreen: Show email input

User → EmailAuthScreen: Enter email, tap "Send"
EmailAuthScreen → AuthViewModel: submitEmail(email)
AuthViewModel → AuthStateMachine: Intent.SubmitEmail(email)
AuthStateMachine → EmailAuthService: requestOTP(email)
EmailAuthService → Email Service: Send OTP
EmailAuthService → AuthStateMachine: Success
AuthStateMachine → Update State (showOTPInput = true)
EmailAuthScreen → OTP Input: Show OTP screen with timer

User → OTP Input: Enter 6-digit OTP, tap "Verify"
OTP Input → AuthViewModel: submitOTP(otp)
AuthViewModel → AuthStateMachine: Intent.SubmitOTP(otp)
AuthStateMachine → EmailAuthService: verifyOTP(email, otp)
EmailAuthService → AuthStateMachine: Success
AuthStateMachine → Update State (Authenticated)
AuthStateMachine → Emit SideEffect.NavigateToMain
```

### Guest Mode Flow

```
User → AuthScreen: Tap "Passer" (Skip)
AuthScreen → AuthViewModel: skipToGuest()
AuthViewModel → AuthStateMachine: Intent.SkipToGuest
AuthStateMachine → GuestModeService: createGuestSession()
GuestModeService → AuthStateMachine: GuestUser created
AuthStateMachine → Update State (isGuest = true)
AuthStateMachine → Emit SideEffect.NavigateToOnboarding
AuthViewModel → AuthScreen: navigationEvent = NavigateToOnboarding
AuthScreen → Navigation: Navigate to Onboarding
```

## Android Integration

### 1. Add AuthScreen to Navigation

```kotlin
// In your NavHost
composable(Screen.Auth.route) {
    AuthScreen(
        onNavigateToMain = {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
        },
        onNavigateToOnboarding = {
            navController.navigate(Screen.Onboarding.route) {
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
        }
    )
}
```

### 2. Use AuthViewModel

```kotlin
@Composable
fun AuthScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val viewModel: AuthViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle navigation events
    LaunchedEffect(uiState.navigationEvent) {
        when (uiState.navigationEvent) {
            NavigationEvent.NavigateToMain -> onNavigateToMain()
            NavigationEvent.NavigateToOnboarding -> onNavigateToOnboarding()
            else -> {}
        }
        viewModel.onNavigationHandled()
    }
    
    // Show error snackbar
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar
            viewModel.onErrorShown()
        }
    }
    
    AuthContent(
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onGoogleSignIn = viewModel::onGoogleSignIn,
        onAppleSignIn = viewModel::onAppleSignIn,
        onEmailSignIn = viewModel::onEmailSignIn,
        onSkipToGuest = viewModel::onSkipToGuest
    )
}
```

### 3. Check Session on App Start

```kotlin
// In your SplashScreen or MainActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val viewModel: AuthViewModel = viewModel()
        
        // Check for existing session
        viewModel.checkExistingSession()
        
        // Collect state to determine initial route
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                val route = when {
                    state.isAuthenticated -> Screen.Home.route
                    state.isGuest -> Screen.Onboarding.route
                    else -> Screen.Auth.route
                }
                navigateToRoute(route)
            }
        }
    }
}
```

## iOS Integration

### 1. Add AuthView to Navigation

```swift
struct AuthView_Previews: PreviewProvider {
    static var previews: some View {
        AuthView()
    }
}
```

### 2. Handle Navigation

```swift
struct AuthView: View {
    @StateObject private var viewModel = AuthViewModel()
    @State private var showEmailAuth = false
    
    var body: some View {
        ZStack {
            // ... UI code ...
        }
        .sheet(isPresented: $showEmailAuth) {
            EmailAuthView(
                viewModel: viewModel,
                isPresented: $showEmailAuth
            )
        }
        .onChange(of: viewModel.navigationEvent) { _, newValue in
            switch newValue {
            case .navigateToMain:
                // Navigate to Home
                viewModel.navigationEventHandled()
            case .navigateToOnboarding:
                // Navigate to Onboarding
                viewModel.navigationEventHandled()
            case .none:
                break
            }
        }
    }
}
```

## Backend Integration

### 1. Configure Auth Routes

```kotlin
fun Application.configureAuth() {
    routing {
        authRoutes(authService = AuthenticationService(...))
    }
}
```

### 2. Protect Endpoints

```kotlin
authenticate {
    get("/api/events") {
        // This endpoint requires authentication
        val user = call.principal<UserPrincipal>()
        // ...
    }
}
```

## Error Handling

### Common Errors

| Error | User Message | Handling |
|-------|--------------|----------|
| `NetworkError` | "Vérifiez votre connexion internet" | Show retry button |
| `InvalidCredentials` | "Identifiants incorrects" | Clear form, allow retry |
| `InvalidOTP` | "Code incorrect" | Allow retry, max 3 attempts |
| `OTPExpired` | "Le code a expiré" | Auto-resend OTP |
| `OAuthError` | "Erreur de connexion avec le provider" | Allow alternative method |

### Error Display

```kotlin
// Android (Compose)
if (state.errorMessage != null) {
    ErrorSnackbar(
        message = state.errorMessage,
        onDismiss = viewModel::onClearError
    )
}
```

```swift
// iOS (SwiftUI)
if let error = viewModel.errorMessage {
    Text(error)
        .foregroundColor(.red)
        .padding()
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                viewModel.clearError()
            }
        }
}
```

## Testing

### Unit Tests

```kotlin
@Test
fun `Google sign in updates state to authenticated`() = runTest {
    // Given
    val stateMachine = createAuthStateMachine()
    
    // When
    stateMachine.handleIntent(Intent.SignInWithGoogle)
    
    // Then
    assertEquals(true, stateMachine.state.value.isAuthenticated)
}
```

### Integration Tests

```kotlin
@Test
fun `Guest mode skips auth and navigates to onboarding`() = runTest {
    // Given
    val stateMachine = createAuthStateMachine()
    
    // When
    stateMachine.handleIntent(Intent.SkipToGuest)
    
    // Then
    assertEquals(true, stateMachine.state.value.isGuest)
    // Verify side effect was emitted
}
```

## Security Best Practices

1. **Token Storage:**
   - Use Keychain (iOS) / Keystore (Android)
   - Never store tokens in plaintext
   - Use encryption at rest

2. **OAuth Security:**
   - Validate ID tokens server-side
   - Use PKCE for web flows
   - Implement state parameter to prevent CSRF

3. **OTP Security:**
   - 6 digits, 5-minute expiry
   - Rate limiting (5 requests/hour)
   - Max 3 verification attempts

4. **GDPR Compliance:**
   - Only collect necessary data
   - Provide data export/deletion
   - Clear consent for data collection

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| Google Sign-In not working | Verify OAuth client ID, check SHA-1 fingerprint |
| Apple Sign-In fails on iOS 15 | Ensure Sign in with Apple capability is enabled |
| OTP not received | Check spam folder, verify email format |
| Session not restored | Check token storage, verify token hasn't expired |
| Navigation loops | Ensure `onNavigationHandled()` is called |

### Debug Mode

Enable debug logging:

```kotlin
// Android
AuthStateMachine.logger = { message ->
    Log.d("AuthStateMachine", message)
}
```

```swift
// iOS
AuthStateMachine.onLog = { message in
    print("AuthStateMachine: \(message)")
}
```
