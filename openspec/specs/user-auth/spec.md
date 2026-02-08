# User Authentication Specification

## Version
**Version**: 1.1.0
**Status**: ✅ Implémenté
**Date**: 8 février 2026
**Last Updated**: 2026-02-08 - Added comprehensive OAuth implementation details

## Purpose

Provides a flexible authentication system supporting multiple sign-in methods (Google OAuth, Apple Sign-In, Email/OTP) and a guest mode with limited functionality. The system ensures secure token storage, RGPD compliance, and offline support for both authenticated and guest users.
## Requirements
### Requirement: Optional Authentication Screen
The application SHALL present an optional authentication screen as the first step after app installation, before accessing the main application features.

#### Scenario: User sees auth options on first launch
- **WHEN** a user opens the application for the first time
- **THEN** the application SHALL display the authentication screen with:
  - Title and welcoming message
  - Sign in with Google button
  - Sign in with Apple button
  - Sign in with Email link/button
  - "Passer" (Skip) button positioned at top-right of the screen

#### Scenario: User skips authentication
- **WHEN** the user taps the "Passer" button
- **THEN** the application SHALL:
  - Create a guest session with limited permissions
  - Navigate to the main application interface
  - Display the user as "Invité" (Guest)
  - Allow basic event creation and participation without account

#### Scenario: User chooses Google Sign-In
- **WHEN** the user taps the Google Sign-In button
- **THEN** the application SHALL:
  - Initiate Google OAuth flow
  - Request user consent for profile and email access
  - Create user account upon successful authentication
  - Store authentication token securely
  - Navigate to main application with authenticated user state

#### Scenario: User chooses Apple Sign-In
- **WHEN** the user taps the Apple Sign-In button
- **THEN** the application SHALL:
  - Initiate Apple Sign-In flow (Sign in with Apple)
  - Request user consent for name and email (if not hidden)
  - Create user account upon successful authentication
  - Store authentication token securely
  - Navigate to main application with authenticated user state

### Requirement: Email Authentication with OTP
The system SHALL provide email-based authentication using One-Time Password (OTP) for users who prefer not to use social login providers.

#### Scenario: User initiates email authentication
- **WHEN** the user taps the "Sign in with Email" button
- **THEN** the application SHALL display:
  - Email input field
  - "Send OTP" button
  - Instructions explaining the OTP process

#### Scenario: System sends OTP email
- **WHEN** a valid email address is provided and user taps "Send OTP"
- **THEN** the system SHALL:
  - Validate the email format
  - Generate a 6-digit OTP code
  - Send the OTP code to the provided email address
  - Display a message confirming OTP was sent
  - Show OTP input field for verification

#### Scenario: User verifies OTP
- **WHEN** the user enters the correct OTP code and submits
- **THEN** the system SHALL:
  - Verify the OTP code
  - Check if OTP is still valid (5-minute expiry)
  - Create user account if email not already registered
  - Create session for registered user if email exists
  - Store authentication token securely
  - Navigate to main application with authenticated user state

#### Scenario: Invalid OTP entered
- **WHEN** the user enters an incorrect OTP code
- **THEN** the system SHALL:
  - Display error message indicating invalid OTP
  - Allow user to retry (max 3 attempts)
  - After 3 failed attempts, require sending new OTP

### Requirement: Guest Mode Limitations
The application SHALL provide a guest mode with limited functionality for users who skip authentication.

#### Scenario: Guest mode feature restrictions
- **WHEN** a user is in guest mode (has not authenticated)
- **THEN** the application SHALL:
  - Allow creating events locally
  - Allow participating in events as a guest
  - Allow viewing events locally
  - Prevent cloud synchronization of events
  - Prevent receiving push notifications
  - Prevent accessing premium features (collaborative albums, chat)
  - Display "Mode Invité" indicator in the UI

#### Scenario: Guest mode data persistence
- **WHEN** a user is in guest mode
- **THEN** the application SHALL:
  - Store all data locally in SQLite
  - Allow data export/import for backup
  - Provide option to authenticate later to migrate guest data

### Requirement: Authentication State Management
The application SHALL maintain authentication state across app launches and integrate with the global application state.

#### Scenario: Authenticated user returns to app
- **WHEN** an authenticated user reopens the application
- **THEN** the application SHALL:
  - Validate stored authentication token
  - If token is valid, automatically log user in
  - Skip authentication screen
  - Navigate to main application

#### Scenario: Guest user returns to app
- **WHEN** a guest user reopens the application
- **THEN** the application SHALL:
  - Restore guest session
  - Skip authentication screen
  - Navigate to main application
  - Maintain "Mode Invité" indicator

#### Scenario: Session expires
- **WHEN** the authentication token has expired
- **THEN** the application SHALL:
  - Display authentication screen
  - Allow user to re-authenticate
  - Preserve local data during re-authentication

### Requirement: Token Security
The application SHALL store authentication tokens securely using platform-specific secure storage mechanisms.

#### Scenario: Storing authentication tokens
- **WHEN** a user successfully authenticates
- **THEN** the application SHALL:
  - On Android: Store tokens in Android Keystore
  - On iOS: Store tokens in iOS Keychain
  - Never store tokens in SharedPreferences or UserDefaults (plain text)

#### Scenario: Retrieving authentication tokens
- **WHEN** the application needs to access stored tokens
- **THEN** the application SHALL:
  - Retrieve tokens from secure storage only
  - Handle secure storage access errors gracefully
  - Fall back to requiring re-authentication if tokens are inaccessible

### Requirement: Privacy and RGPD Compliance
The authentication system SHALL comply with RGPD principles of data minimization and user consent.

#### Scenario: Minimal data collection
- **WHEN** a user authenticates (via Google, Apple, or Email)
- **THEN** the system SHALL:
  - Collect only necessary data: email, name (if provided), auth provider
  - Do NOT collect unnecessary personal information
  - Display clear consent message during authentication

#### Scenario: Guest mode privacy
- **WHEN** a user uses guest mode
- **THEN** the system SHALL:
  - Store all data locally on device
  - Not send any data to backend servers
  - Provide clear indication of local-only storage
  - Allow complete data deletion on request

#### Scenario: Data deletion on request
- **WHEN** a user requests account deletion
- **THEN** the system SHALL:
  - Delete all user data from backend
  - Delete authentication tokens
  - Delete local data
  - Provide confirmation of deletion

### Requirement: Offline Support
The authentication system SHALL support offline access for both authenticated and guest users.

#### Scenario: Authenticated user offline
- **WHEN** an authenticated user uses the app offline
- **THEN** the application SHALL:
  - Allow access to previously synced data
  - Allow local data modifications
  - Queue changes for synchronization when online
  - Display offline indicator in UI

#### Scenario: Guest user offline
- **WHEN** a guest user uses the app offline
- **THEN** the application SHALL:
  - Continue full functionality (all data is local anyway)
  - Not display offline indicator (guest mode is inherently local-first)

### Requirement: Authentication Error Handling
The application SHALL provide clear error messages and recovery options for authentication failures.

#### Scenario: Network error during authentication
- **WHEN** a network error occurs during authentication
- **THEN** the application SHALL:
  - Display user-friendly error message
  - Allow retry operation
  - Do not crash or show technical errors

#### Scenario: OAuth provider error
- **WHEN** Google or Apple authentication fails
- **THEN** the application SHALL:
  - Display specific error message from provider
  - Allow user to retry
  - Offer alternative authentication methods (Email, other provider)

#### Scenario: Invalid credentials
- **WHEN** authentication fails due to invalid credentials
- **THEN** the application SHALL:
  - Display clear error message
  - Highlight the problematic field (if applicable)
  - Allow user to correct and retry

## OAuth Implementation Details

### Google OAuth 2.0 Flow

The application implements the OAuth 2.0 Authorization Code flow with PKCE for Google authentication, providing secure access to user profile and email data.

#### Authorization Code Request

**Endpoint**: `https://accounts.google.com/o/oauth2/v2/auth`

**Request Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `client_id` | string | Yes | OAuth 2.0 client ID from Google Cloud Console |
| `redirect_uri` | string | Yes | Callback URI configured in Google Console (custom scheme) |
| `response_type` | string | Yes | Must be `code` for authorization code flow |
| `scope` | string | Yes | Space-separated list of requested scopes |
| `state` | string | Yes | Random value to prevent CSRF attacks |
| `code_challenge` | string | Yes | PKCE code challenge (base64url-encoded SHA256) |
| `code_challenge_method` | string | Yes | Must be `S256` |

**Required Scopes**:
- `openid` - Required for OpenID Connect protocol
- `https://www.googleapis.com/auth/userinfo.email` - User's email address
- `https://www.googleapis.com/auth/userinfo.profile` - User's basic profile information

#### Token Exchange

**Endpoint**: `https://oauth2.googleapis.com/token`

**Request Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `code` | string | Yes | Authorization code from callback |
| `client_id` | string | Yes | OAuth 2.0 client ID |
| `client_secret` | string | Yes | Client secret (server-side only) |
| `redirect_uri` | string | Yes | Must match authorization request |
| `grant_type` | string | Yes | Must be `authorization_code` |
| `code_verifier` | string | Yes | PKCE code verifier used to generate challenge |

**Response 200 OK**:
```json
{
  "access_token": "ya29.a0AfH6...",
  "expires_in": 3599,
  "refresh_token": "1//0g...",
  "scope": "openid https://www.googleapis.com/auth/userinfo.email",
  "token_type": "Bearer",
  "id_token": "eyJhbGci..."
}
```

#### Profile Retrieval

**Endpoint**: `https://www.googleapis.com/oauth2/v2/userinfo`

**Authorization**: Bearer token from token exchange

**Response 200 OK**:
```json
{
  "id": "11223344556677889900",
  "email": "user@example.com",
  "verified_email": true,
  "name": "John Doe",
  "given_name": "John",
  "family_name": "Doe",
  "picture": "https://lh3.googleusercontent.com/...",
  "locale": "en"
}
```

#### ID Token Validation

The ID token (JWT) MUST be validated on the backend before creating a session:

1. Verify signature using Google's public keys
2. Verify issuer is `https://accounts.google.com`
3. Verify audience matches client_id
4. Verify token expiration
5. Verify nonce matches state parameter

### Apple Sign In Implementation

Apple Sign In uses JWT-based authentication with client-side `authorizationCode` exchange and private key management for server verification.

#### Sign In Request

**Endpoint**: `https://appleid.apple.com/auth/authorize`

**Request Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `client_id` | string | Yes | Bundle ID or Service ID from Apple Developer |
| `redirect_uri` | string | Yes | Callback URI configured in Apple Developer |
| `response_type` | string | Yes | Must include `code` |
| `scope` | string | No | Space-separated: `name`, `email` |
| `state` | string | Yes | Random value for CSRF protection |
| `response_mode` | string | No | Must be `form_post` or `fragment` |
| `code_challenge` | string | Yes | PKCE challenge |
| `code_challenge_method` | string | Yes | Must be `S256` |

#### Token Exchange

**Endpoint**: `https://appleid.apple.com/auth/token`

**Request Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `client_id` | string | Yes | Bundle ID or Service ID |
| `client_secret` | string | Yes | JWT generated with private key |
| `code` | string | Yes | Authorization code from callback |
| `grant_type` | string | Yes | Must be `authorization_code` |
| `redirect_uri` | string | Yes | Must match authorization request |

**Response 200 OK**:
```json
{
  "access_token": "a3f5d...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "r9e7c...",
  "id_token": "eyJhbGci...",
  "expires_in": 3600,
  "token_type": "Bearer"
}
```

#### Private Key Management

The application MUST generate a client secret JWT signed with the private key from Apple Developer:

**JWT Structure**:
```json
{
  "iss": "TEAM123456",
  "iat": 1704067200,
  "exp": 1704153600,
  "aud": "https://appleid.apple.com",
  "sub": "com.example.app"
}
```

**JWT Header**:
```json
{
  "alg": "ES256",
  "kid": "KEY_ID_123",
  "typ": "JWT"
}
```

**Private Key Storage**:
- On Server: Store `.p8` private key securely (environment variable or secret manager)
- On Client: Never store private key - only public operations
- Key rotation: Generate new JWT before expiration (recommended 180 days)

#### Email Masking

Apple may provide a masked email for privacy:

**Masked Email Format**: `abcd1234@privaterelay.appleid.com`

**Handling Strategy**:
- Treat masked emails as primary identifiers
- Use `sub` (subject) from ID token as unique user ID
- Real email is only provided on first sign-in
- Store both masked and real email (if provided) for account recovery

#### ID Token Claims

**Required Claims Validation**:
```json
{
  "iss": "https://appleid.apple.com",
  "aud": "com.example.app",
  "exp": 1704153600,
  "iat": 1704067200,
  "sub": "001234.abcd1234abcd1234abcd1234abcd1234.1234",
  "email": "user@privaterelay.appleid.com",
  "email_verified": "true",
  "is_private_email": "true"
}
```

### Token Management

#### Secure Storage

**Android - Keystore**:
```kotlin
val keyGen = KeyGenerator.getInstance(
    KeyProperties.KEY_ALGORITHM_AES,
    "AndroidKeyStore"
)
val keyGenSpec = KeyGenParameterSpec.Builder(
    "auth_token_master",
    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setUserAuthenticationRequired(true)
    .build()

keyGen.init(keyGenSpec)
keyGen.generateKey()

// Encrypt and store token in EncryptedSharedPreferences
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val prefs = EncryptedSharedPreferences.create(
    context,
    "auth_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

**iOS - Keychain**:
```swift
let keychainQuery: [String: Any] = [
    kSecClass as String: kSecClassGenericPassword,
    kSecAttrAccount as String: "auth_token",
    kSecValueData as String: tokenData,
    kSecAttrAccessControl as String: accessControl,
    kSecAttrAccessible whenUnlockedThisDeviceOnly
]

let status = SecItemAdd(keychainQuery as CFDictionary, nil)
```

#### Token Refresh Flow

**Trigger Conditions**:
- Token expires (check `exp` claim or local timestamp)
- 401 Unauthorized response from API
- 5 minutes before expiration (proactive refresh)

**Google Refresh**:
```kotlin
suspend fun refreshToken(refreshToken: String): TokenResult {
    return httpClient.post("https://oauth2.googleapis.com/token") {
        parameter("grant_type", "refresh_token")
        parameter("refresh_token", refreshToken)
        parameter("client_id", clientId)
        parameter("client_secret", clientSecret)
    }
}
```

**Apple Refresh**:
```kotlin
suspend fun refreshToken(refreshToken: String): TokenResult {
    return httpClient.post("https://appleid.apple.com/auth/token") {
        parameter("grant_type", "refresh_token")
        parameter("refresh_token", refreshToken)
        parameter("client_id", clientId)
        parameter("client_secret", generateClientSecretJWT())
    }
}
```

#### Token Revocation

**User Logout**:
```kotlin
suspend fun revokeGoogleToken(accessToken: String) {
    httpClient.post("https://oauth2.googleapis.com/revoke") {
        parameter("token", accessToken)
    }
}
```

**Security Requirements**:
- Clear all tokens from secure storage on logout
- Clear all tokens from secure storage on account deletion
- Invalidate refresh token on server-side after use (one-time use for Google)

### Multi-Device Support

#### Cross-Device Token Sync

**Token Propagation**:
- Primary device: Tokens stored locally in secure storage
- Secondary devices: Tokens synced via encrypted cloud backup (optional)
- Token sync uses end-to-end encryption (user-managed encryption key)

**Device Registration**:
```kotlin
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val platform: Platform,
    val lastActive: Instant,
    val pushToken: String?
)

sealed class AuthSyncStrategy {
    data object LocalOnly : AuthSyncStrategy()
    data class CloudSync(val encryptionKeyId: String) : AuthSyncStrategy()
}
```

#### Concurrent Session Management

**Session Limits**:
- Maximum 5 active devices per user
- Oldest session revoked when limit exceeded
- User can view and revoke active sessions in settings

**Session Validation**:
```kotlin
data class Session(
    val sessionId: String,
    val deviceId: String,
    val deviceInfo: DeviceInfo,
    val createdAt: Instant,
    val lastActive: Instant,
    val ipAddress: String?,
    val location: String?
)

suspend fun validateSession(token: String): SessionValidationResult {
    // Check token hasn't been revoked
    // Check device hasn't been revoked
    // Check session limit not exceeded
}
```

### Error Handling

#### OAuth Error Codes

**Google OAuth Errors**:
| Error Code | Description | User Action |
|------------|-------------|-------------|
| `access_denied` | User denied authorization | Re-initiate flow with clear consent explanation |
| `invalid_request` | Malformed request | Check redirect_uri and client_id |
| `unauthorized_client` | Client not authorized | Verify OAuth consent screen configured |
| `invalid_scope` | Invalid scope requested | Verify scope format |
| `server_error` | Google server error | Retry with exponential backoff |
| `temporarily_unavailable` | Service temporarily unavailable | Retry after delay |

**Apple Sign In Errors**:
| Error Code | Description | User Action |
|------------|-------------|-------------|
| `invalid_request` | Malformed request | Check redirect_uri and client_id |
| `invalid_client` | Client authentication failed | Verify JWT client_secret |
| `invalid_grant` | Authorization code invalid | Code may be expired or already used |
| `unauthorized_client` | Client not authorized | Verify Sign In with Apple enabled |
| `unsupported_response_type` | Invalid response type | Use `code` response type |
| `invalid_scope` | Invalid scope | Scope must be `name` or `email` |

#### Client-Side Error Handling

**Network Errors**:
```kotlin
sealed class AuthError : Exception() {
    data class NetworkError(val message: String) : AuthError()
    data class OAuthError(val code: String, val message: String) : AuthError()
    data class TokenError(val message: String) : AuthError()
    data class UserCancelled : AuthError()
}

suspend fun handleOAuthError(error: AuthError): AuthResult {
    return when (error) {
        is AuthError.NetworkError -> {
            // Show retry option with offline mode
            AuthResult.RetryAvailable
        }
        is AuthError.OAuthError -> {
            when (error.code) {
                "access_denied" -> AuthResult.UserDenied
                "server_error" -> AuthResult.TransientError
                else -> AuthResult.FatalError(error.message)
            }
        }
        is AuthError.UserCancelled -> AuthResult.Cancelled
    }
}
```

**UI Error Messages** (French):
| Scenario | Message |
|----------|---------|
| Network unavailable | "Connexion réseau requise. Vérifiez votre connexion Internet." |
| User cancelled | "Connexion annulée." |
| Access denied | "L'autorisation est requise pour continuer." |
| Server error | "Service temporairement indisponible. Veuillez réessayer." |
| Invalid token | "Votre session a expiré. Veuillez vous reconnecter." |

#### Retry Strategy

**Exponential Backoff**:
```kotlin
data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelay: Duration = 1.seconds,
    val maxDelay: Duration = 30.seconds,
    val backoffMultiplier: Double = 2.0
)

suspend fun <T> withRetry(config: RetryConfig, block: suspend () -> T): T {
    var delay = config.initialDelay
    repeat(config.maxRetries) {
        try {
            return block()
        } catch (e: AuthError.NetworkError) {
            if (it == config.maxRetries - 1) throw e
            delay(delay)
            delay = (delay * config.backoffMultiplier).coerceAtMost(config.maxDelay)
        }
    }
    throw IllegalStateException("Should not reach here")
}
```

