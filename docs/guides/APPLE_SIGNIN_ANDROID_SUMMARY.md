# Apple Sign-In Android Implementation Summary

## Overview

This implementation adds Apple Sign-In OAuth 2.0 web flow support for Android in the Wakeve project. Since Apple doesn't provide a native Sign in with Apple SDK for Android, we use a web-based OAuth 2.0 authorization flow.

## Deliverables

### 1. AppleSignInProvider.kt
**Location**: `shared/src/androidMain/kotlin/com/guyghost/wakeve/auth/shell/services/AppleSignInProvider.kt`

**Purpose**: Interface defining the contract for Apple Sign-In on Android.

**Key Components**:
- `AppleSignInProvider` interface with three methods:
  - `getAuthorizationUrl()` - Constructs the Apple auth URL for browser/Custom Tab
  - `exchangeCodeForTokens()` - Exchanges authorization code for access/ID tokens
  - `parseIdToken()` - Parses JWT ID token to extract user information
- `AppleTokenResponse` data class - Token response from Apple
- `AppleUserInfo` data class - User information from ID token

**Design Pattern**: Interface abstraction for OAuth provider (similar to AndroidOAuthProvider for Google)

### 2. AppleSignInWebFlow.kt
**Location**: `shared/src/androidMain/kotlin/com/guyghost/wakeve/auth/shell/services/AppleSignInWebFlow.kt`

**Purpose**: Implementation of AppleSignInProvider using Ktor HTTP client.

**Key Components**:
- `AppleSignInWebFlow` class implementing AppleSignInProvider
- Uses Ktor HTTP client with CIO engine (matches project configuration)
- Implements full OAuth 2.0 flow:
  1. Constructs authorization URL with proper parameters
  2. Makes POST request to Apple's token endpoint
  3. Parses JSON response and handles errors
  4. Decodes JWT ID token (without signature verification)
- `AppleSignInException` for error handling
- URL encoding helper function
- JWT payload extraction logic for nested name structure

**Security Notes**:
- JWT signature verification NOT implemented (should be done on backend)
- client_secret can be nullable (for backend proxy approach)
- State parameter must be validated by caller (CSRF protection)

### 3. AndroidAuthService.kt (Modified)
**Location**: `shared/src/androidMain/kotlin/com/guyghost/wakeve/auth/shell/services/AndroidAuthService.kt`

**Changes**:
- Added `appleSignInProvider` property
- Added `setAppleSignInProvider()` method
- Updated `signInWithApple()` to use provider if set
- Added `getAppleAuthUrl()` - Get authorization URL for browser/Custom Tab
- Added `handleAppleAuthCallback()` - Process OAuth callback and create AuthResult
- Updated `signOut()` documentation for Apple Sign-In

**New Methods**:
```kotlin
suspend fun getAppleAuthUrl(
    clientId: String,
    redirectUri: String,
    state: String
): String?

suspend fun handleAppleAuthCallback(
    code: String?,
    clientId: String,
    clientSecret: String?,
    redirectUri: String
): AuthResult
```

### 4. AppleSignInProviderTest.kt (Test)
**Location**: `shared/src/androidTest/kotlin/com/guyghost/wakeve/auth/shell/services/AppleSignInProviderTest.kt`

**Purpose**: Unit tests for Apple Sign-In provider interface and data models.

**Test Coverage**:
- AppleTokenResponse field validation
- AppleUserInfo field validation (with null handling)
- Interface structure verification
- Mock provider implementation

### 5. APPLE_SIGNIN_ANDROID_GUIDE.md (Documentation)
**Location**: `docs/guides/APPLE_SIGNIN_ANDROID_GUIDE.md`

**Purpose**: Comprehensive implementation guide for developers.

**Contents**:
- Architecture diagram
- Apple Developer setup instructions
- Android Manifest configuration
- Step-by-step implementation guide
- Client secret generation options (backend vs client)
- Security considerations
- Error handling reference
- Testing guidelines
- Complete flow example with ViewModel

## Implementation Details

### OAuth 2.0 Flow

```
1. getAuthorizationUrl()
   → https://appleid.apple.com/auth/authorize
   → Parameters: client_id, redirect_uri, response_type=code, state, scope

2. User signs in on Apple's page
   → Redirected to redirect_uri with code and state

3. exchangeCodeForTokens()
   → POST to https://appleid.apple.com/auth/token
   → Parameters: code, client_id, client_secret, redirect_uri, grant_type
   → Returns: access_token, id_token, refresh_token, expires_in

4. parseIdToken()
   → Decode JWT id_token (base64)
   → Extract: sub (user ID), email, name, email_verified
   → Returns: AppleUserInfo
```

### Dependencies

**Existing** (already in project):
- `ktor-client-core` - HTTP client
- `ktor-client-cio` - CIO engine for Android
- `ktor-client-content-negotiation` - JSON parsing
- `kotlinx-serialization-json` - Serialization
- `kotlinx-coroutines` - Coroutines

**No new dependencies required** - implementation uses existing project dependencies

### Architecture Pattern

Follows existing patterns in the project:
- **Interface + Implementation** (AppleSignInProvider + AppleSignInWebFlow)
- **Provider Injection** (setAppleSignInProvider())
- **Result Type** (Result<T> for error handling)
- **AuthResult** (Consistent with other auth methods)
- **Pure Functions** (parseJWT from existing auth/core/logic)

### Security Considerations

✅ **Implemented**:
- State parameter support (CSRF protection)
- HTTPS-only endpoints
- Error handling for all network operations
- Optional client_secret (backend proxy support)

⚠️ **Not Implemented** (intentional):
- JWT signature verification (should be done on backend)
- client_secret generation (security risk on client)

📋 **Caller Responsibility**:
- Generate and validate random state parameter
- Store tokens securely (AndroidTokenStorage)
- Use backend for client_secret generation in production
- Validate callback state before processing

## Integration Points

### 1. Application Initialization
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        authService.setAppleSignInProvider(AppleSignInWebFlow())
    }
}
```

### 2. Start Sign-In Flow
```kotlin
val authUrl = authService.getAppleAuthUrl(
    clientId = "com.example.app",
    redirectUri = "yourapp://callback",
    state = UUID.randomUUID().toString()
)
CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(authUrl))
```

### 3. Handle Callback
```kotlin
override fun onNewIntent(intent: Intent?) {
    val uri = intent?.data
    val code = uri?.getQueryParameter("code")
    val state = uri?.getQueryParameter("state")

    val result = authService.handleAppleAuthCallback(
        code = code,
        clientId = "com.example.app",
        clientSecret = null,
        redirectUri = "yourapp://callback"
    )
}
```

## Testing Strategy

### Unit Tests
- ✅ AppleSignInProviderTest.kt (5 tests)
- Tests for data models and interface structure

### Integration Tests (Future)
- Mock Apple endpoints
- Test full OAuth flow
- Test error scenarios
- Test token expiration

### Manual Testing
- Use Apple Sandbox accounts
- Test on physical devices
- Verify token exchange and user info

## Compatibility

### Platform
- ✅ Android (Kotlin)
- ❌ iOS (has native Sign in with Apple)
- ❌ JVM (no UI)

### Kotlin Version
- Kotlin Multiplatform 2.2.20
- kotlinx.coroutines
- kotlinx.serialization

### Android API Level
- Minimum: API 21+ (as per project configuration)
- Requires: Internet permission
- Requires: Custom Tabs (or WebView)

## Known Limitations

1. **No Native SDK**: Apple doesn't provide a native Android SDK
2. **First Sign-In Only**: User's name is only provided on first sign-in
3. **Email Privacy**: Users can hide their email (returns relay email)
4. **No Sign-Out**: Apple doesn't provide a sign-out endpoint
5. **Client Secret**: Requires backend proxy or client-side JWT generation

## Future Enhancements

1. **Backend Integration**: Move token exchange to backend server
2. **JWT Verification**: Verify ID token signature on backend
3. **Refresh Token**: Implement token refresh flow
4. **Biometric Prompt**: Add biometric confirmation for security
5. **Analytics**: Track sign-in success/failure rates

## References

- [Sign in with Apple REST API](https://developer.apple.com/documentation/sign-in-with-apple/rest-api)
- [OAuth 2.0 Authorization Code Flow](https://datatracker.ietf.org/doc/html/rfc6749#section-4.1)
- [OpenID Connect Core 1.0](https://openid.net/connect/)

## Checklist

- [x] AppleSignInProvider interface created
- [x] AppleSignInWebFlow implementation created
- [x] AndroidAuthService updated with Apple provider support
- [x] Unit tests created
- [x] Documentation created (APPLE_SIGNIN_ANDROID_GUIDE.md)
- [x] Uses existing project dependencies (ktor, kotlinx)
- [x] Follows existing code patterns and conventions
- [x] Security considerations documented
- [x] Error handling implemented
- [x] Usage examples provided

## Status

✅ **Complete** - All deliverables implemented and documented.

Ready for:
- Code review
- Integration testing with Apple Developer accounts
- UI integration (Custom Tabs implementation)
- Backend integration (token exchange proxy)
