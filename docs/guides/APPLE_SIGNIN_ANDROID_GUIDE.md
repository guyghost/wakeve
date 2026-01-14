# Apple Sign-In Android Implementation Guide

## Overview

This guide explains how to implement Apple Sign-In on Android using the web-based OAuth 2.0 flow. Since Apple doesn't provide a native Sign in with Apple SDK for Android, we use a web-based approach with Custom Tabs or WebView.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Android App                           │
│                                                              │
│  ┌──────────────┐     ┌────────────────────────────────┐   │
│  │ AuthService  │────▶│ AppleSignInProvider (WebFlow)│   │
│  └──────────────┘     └────────────────────────────────┘   │
│        │                                                       │
│        │ 1. getAuthorizationUrl()                              │
│        ▼                                                       │
│  ┌──────────────┐     ┌────────────────────────────────┐   │
│  │ Custom Tab   │────▶│ Apple Sign-In Page              │   │
│  └──────────────┘     └────────────────────────────────┘   │
│        │                                                       │
│        │ 2. Redirect back with code                             │
│        ▼                                                       │
│  ┌──────────────┐     ┌────────────────────────────────┐   │
│  │ Callback     │────▶│ handleAppleAuthCallback()     │   │
│  └──────────────┘     └────────────────────────────────┘   │
│        │                                                       │
│        │ 3. Exchange code for tokens                           │
│        ▼                                                       │
│  ┌──────────────┐     ┌────────────────────────────────┐   │
│  │ Auth Result  │◀────│ parseIdToken() + create User  │   │
│  └──────────────┘     └────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## Setup

### 1. Apple Developer Configuration

Before implementing, you need to configure Apple Sign-In in the Apple Developer portal:

1. **Create a Services ID**
   - Go to [Apple Developer > Certificates, Identifiers & Profiles](https://developer.apple.com/account/resources/identifiers/list)
   - Click "+" > "Services ID"
   - Enter your Bundle ID (e.g., `com.example.app`)
   - Check "Sign in with Apple"
   - Configure your redirect URI (e.g., `yourapp://callback`)

2. **Generate a Private Key** (for client_secret generation)
   - Go to [Apple Developer > Keys](https://developer.apple.com/account/resources/authkeys/list)
   - Create a new key
   - Check "Sign in with Apple"
   - Download the `.p8` file (you can only download once!)
   - Save: Key ID, Team ID, and Private Key content

### 2. Android Manifest Configuration

Add your custom scheme to `AndroidManifest.xml`:

```xml
<activity
    android:name=".MainActivity"
    android:launchMode="singleTask">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <!-- Your custom scheme -->
        <data
            android:scheme="yourapp"
            android:host="callback" />
    </intent-filter>
</activity>
```

## Implementation

### Step 1: Initialize the Service

In your `Application` class or `MainActivity`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize auth service with Apple Sign-In provider
        authService.setAppleSignInProvider(AppleSignInWebFlow())
    }
}
```

### Step 2: Start the Sign-In Flow

```kotlin
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import java.util.UUID

// In your Activity or ViewModel
fun startAppleSignIn() {
    lifecycleScope.launch {
        // Generate a secure random state for CSRF protection
        val state = UUID.randomUUID().toString()

        // Store the state for validation on callback
        // (e.g., in SharedPreferences or ViewModel)
        storedState = state

        // Get the Apple Sign-In authorization URL
        val authUrl = authService.getAppleAuthUrl(
            clientId = "com.example.app", // Your Services ID
            redirectUri = "yourapp://callback", // Your registered redirect URI
            state = state
        )

        // Open the URL in a Custom Tab
        authUrl?.let { url ->
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        }
    }
}
```

### Step 3: Handle the Callback

Add this to your `MainActivity`:

```kotlin
class MainActivity : AppCompatActivity() {
    private var storedState: String? = null

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleCallback(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handleCallback(intent)
    }

    private fun handleCallback(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "yourapp" && uri.host == "callback") {
            val code = uri.getQueryParameter("code")
            val state = uri.getQueryParameter("state")

            // Validate state to prevent CSRF attacks
            if (state == storedState && code != null) {
                lifecycleScope.launch {
                    val result = authService.handleAppleAuthCallback(
                        code = code,
                        clientId = "com.example.app",
                        clientSecret = null, // Or generate JWT client secret
                        redirectUri = "yourapp://callback"
                    )

                    when {
                        result.isSuccess -> {
                            val user = result.userOrNull
                            val token = result.getOrNull() as? AuthResult.Success
                            // Handle successful sign-in
                            // User ID: user.id (Apple's "sub" claim)
                            // Email: user.email
                            // Name: user.name
                            // Auth Token: token.token
                        }
                        result.isError -> {
                            val error = result.errorOrNull
                            // Handle error
                        }
                    }
                }
            } else {
                // Invalid state or missing code
                Log.e("AppleSignIn", "Invalid callback: state mismatch or missing code")
            }
        }
    }
}
```

## Client Secret Generation

For token exchange, Apple requires a JWT client_secret. You have two options:

### Option 1: Backend Proxy (Recommended)

Move token exchange to your backend server:

1. Send the authorization code to your backend
2. Backend generates client_secret using Apple private key
3. Backend exchanges code for tokens
4. Backend returns tokens to the client

**Pros:** More secure (no private key on client)
**Cons:** Requires backend changes

### Option 2: Client-Side Generation (Not Recommended)

Generate client_secret on the client:

```kotlin
// NOT RECOMMENDED - Private key exposed in client
// Only for development/testing

class AppleClientSecretGenerator(
    private val keyId: String,
    private val teamId: String,
    private val clientId: String,
    private val privateKeyPem: String
) {
    fun generate(): String {
        // Create JWT with ES256 signature
        // Header: {"alg": "ES256", "kid": keyId}
        // Payload: {"iss": teamId, "iat": now, "exp": now + 3600, "aud": "https://appleid.apple.com", "sub": clientId}
        // Sign with Apple's private key using ECDSA
        // ...
    }
}
```

**Pros:** Simpler (no backend changes)
**Cons:** Less secure (private key exposed)

## Security Considerations

1. **State Parameter**: Always generate and validate a random state parameter to prevent CSRF attacks
2. **HTTPS Only**: All OAuth endpoints use HTTPS
3. **Private Key**: Never commit Apple private key to version control
4. **Client Secret**: Ideally generated on backend, not client
5. **Token Storage**: Store tokens securely using EncryptedSharedPreferences
6. **User ID**: Always use Apple's `sub` claim as the unique user ID, not email

## Error Handling

Common errors and solutions:

| Error | Cause | Solution |
|-------|-------|----------|
| `invalid_client` | Incorrect client_id | Verify Services ID matches Apple Developer |
| `invalid_redirect_uri` | Redirect URI mismatch | Ensure URI matches Apple Developer exactly |
| `invalid_grant` | Expired or invalid code | Authorization codes expire in 5 minutes |
| `invalid_client_secret` | Invalid client_secret | Generate correct JWT with valid private key |

## Testing

For testing, use the Apple Sign-In sandbox:

1. Use test email accounts in Apple Developer > Sandbox
2. Test on physical devices (simulator has limitations)
3. Verify token exchange and user info parsing

## References

- [Sign in with Apple REST API](https://developer.apple.com/documentation/sign-in-with-apple/rest-api)
- [Sign in with Apple for Android](https://developer.apple.com/documentation/sign-in-with-apple/getting_started_with_sign_in_with_apple_on_android)
- [OAuth 2.0 Authorization Code Flow](https://datatracker.ietf.org/doc/html/rfc6749#section-4.1)

## Example: Complete Flow

```kotlin
// In your ViewModel
class AuthViewModel(
    private val authService: AuthService
) : ViewModel() {

    private var _authState = mutableStateOf<AuthState>(AuthState.Idle)
    val authState: State<AuthState> = _authState

    fun signInWithApple() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                // 1. Generate state and auth URL
                val state = UUID.randomUUID().toString()
                val authUrl = authService.getAppleAuthUrl(
                    clientId = BuildConfig.APPLE_CLIENT_ID,
                    redirectUri = "${BuildConfig.APP_SCHEME}://callback",
                    state = state
                )

                // 2. Store state and emit URL
                _authState.value = AuthState.OpenAuthUrl(authUrl, state)

            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun handleAppleCallback(code: String, state: String, expectedState: String) {
        viewModelScope.launch {
            if (state != expectedState) {
                _authState.value = AuthState.Error("Invalid state")
                return@launch
            }

            val result = authService.handleAppleAuthCallback(
                code = code,
                clientId = BuildConfig.APPLE_CLIENT_ID,
                clientSecret = null, // Use backend for production
                redirectUri = "${BuildConfig.APP_SCHEME}://callback"
            )

            when {
                result.isSuccess -> {
                    val user = result.userOrNull!!
                    _authState.value = AuthState.Success(user)
                }
                result.isError -> {
                    val error = result.errorOrNull!!
                    _authState.value = AuthState.Error(error.toString())
                }
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class OpenAuthUrl(val url: String, val state: String) : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}
```

## Next Steps

1. Configure Apple Developer portal with Services ID and redirect URI
2. Add custom scheme to AndroidManifest.xml
3. Implement startAppleSignIn() in your UI
4. Handle the callback in MainActivity
5. Decide on client_secret generation strategy (backend vs client)
6. Test with Apple Sandbox accounts
7. Store tokens securely using AndroidTokenStorage
