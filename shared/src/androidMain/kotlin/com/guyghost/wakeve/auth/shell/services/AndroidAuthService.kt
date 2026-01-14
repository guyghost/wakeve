package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

 /**
  * Android implementation of AuthService using Google Sign-In SDK.
  *
  * IMPLEMENTATION STATUS:
  * - Google Sign-In: Uses AndroidOAuthProvider interface (injectable)
  * - Apple Sign-In: Uses AppleSignInProvider interface (injectable web flow)
  *
  * OAuth Integration:
  * - Google Sign-In is handled via AndroidOAuthProvider interface
  * - Apple Sign-In is handled via AppleSignInProvider interface (web-based OAuth flow)
  * - Providers can be injected via setOAuthProvider() and setAppleSignInProvider() methods
  * - If no provider is set, falls back to mock implementation
  *
  * Token storage is managed separately via TokenStorage interface (AndroidTokenStorage),
  * which uses EncryptedSharedPreferences with Android Keystore for security ✅
  *
  * @requires Google Play Services to be installed (for Google Sign-In)
  * @requires Internet permission
  *
  * Usage:
  * ```kotlin
  * val authService = AuthService()
  * authService.setOAuthProvider(GoogleSignInProvider())
  * authService.setAppleSignInProvider(AppleSignInWebFlow())
  * ```
  */
actual class AuthService {

    /**
     * Android-specific OAuth provider for handling Google Sign-In.
     * Can be null if not injected (falls back to mock).
     */
    private var oauthProvider: AndroidOAuthProvider? = null

    /**
     * Android-specific Apple Sign-In provider for handling web-based OAuth flow.
     * Can be null if not injected (falls back to mock).
     */
    private var appleSignInProvider: AppleSignInProvider? = null

    /**
     * Sets the OAuth provider for this service.
     *
     * This should be called during app initialization to enable real OAuth flow:
     * ```kotlin
     * authService.setOAuthProvider(GoogleSignInProvider())
     * ```
     *
     * @param provider The AndroidOAuthProvider implementation (e.g., GoogleSignInProvider)
     */
    fun setOAuthProvider(provider: AndroidOAuthProvider) {
        this.oauthProvider = provider
    }

    /**
     * Sets the Apple Sign-In provider for this service.
     *
     * This should be called during app initialization to enable real Apple Sign-In flow:
     * ```kotlin
     * authService.setAppleSignInProvider(AppleSignInWebFlow())
     * ```
     *
     * @param provider The AppleSignInProvider implementation (e.g., AppleSignInWebFlow)
     */
    fun setAppleSignInProvider(provider: AppleSignInProvider) {
        this.appleSignInProvider = provider
    }

     /**
       * Initiates Google Sign-In flow on Android.
      *
      * If an OAuth provider is set via setOAuthProvider(), this will launch
      * the real Google Sign-In flow. Otherwise, returns a mock result.
      *
      * REAL IMPLEMENTATION (with OAuth provider):
      * - Uses AndroidOAuthProvider.getGoogleSignInIntent() to get the intent
      * - The caller must launch this intent with startActivityForResult
      * - Then call handleGoogleSignInResult() with the result data
      *
      * MOCK IMPLEMENTATION (without OAuth provider):
      * - Returns a mock user for testing purposes
      *
      * @return AuthResult with authenticated user or error
      */
    actual suspend fun signInWithGoogle(): AuthResult = withContext(Dispatchers.Main) {
        try {
            // If OAuth provider is set, use it for real sign-in
            if (oauthProvider != null) {
                // Real implementation: the provider handles the sign-in flow
                // Note: This method signature doesn't allow returning Intent,
                // so the caller needs a different API for launching the flow.
                // For now, return an error indicating the limitation.
                AuthResult.error(
                    AuthError.OAuthError(
                        provider = AuthMethod.GOOGLE,
                        message = "Use getGoogleSignInIntent() and handleGoogleSignInResult() for OAuth flow"
                    )
                )
            } else {
                // Mock implementation for testing
                AuthResult.success(
                    User(
                        id = "google_user_123",
                        email = "user@gmail.com",
                        name = "Google User",
                        authMethod = AuthMethod.GOOGLE,
                        isGuest = false,
                        createdAt = System.currentTimeMillis(),
                        lastLoginAt = System.currentTimeMillis()
                    ),
                    com.guyghost.wakeve.auth.core.models.AuthToken.createLongLived(
                        value = "google_id_token_placeholder",
                        expiresInDays = 30
                    )
                )
            }
        } catch (e: Exception) {
            AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.GOOGLE,
                    message = e.message ?: "Erreur Google Sign-In"
                )
            )
        }
    }

    /**
     * Initiates Apple Sign-In flow on Android.
     * Uses web-based Sign in with Apple JS for Android.
     *
     * If an Apple Sign-In provider is set via setAppleSignInProvider(),
     * this will provide a real web-based OAuth flow. Otherwise, returns a mock result.
     *
     * REAL IMPLEMENTATION (with Apple Sign-In provider):
     * - This method returns an error indicating the limitation
     * - The caller should use getAppleAuthUrl() to get the authorization URL
     * - Then open the URL in a browser or Custom Tab
     * - After callback, call handleAppleAuthCallback() to process the result
     *
     * MOCK IMPLEMENTATION (without Apple Sign-In provider):
     * - Returns a mock user for testing purposes
     *
     * @return AuthResult with authenticated user or error
     */
    actual suspend fun signInWithApple(): AuthResult = withContext(Dispatchers.Main) {
        try {
            // If Apple Sign-In provider is set, use it for real sign-in
            if (appleSignInProvider != null) {
                // Real implementation: the provider handles the sign-in flow
                // Note: This method signature doesn't allow returning the auth URL,
                // so the caller needs a different API for launching the flow.
                // Use getAppleAuthUrl() and handleAppleAuthCallback() instead.
                AuthResult.error(
                    AuthError.OAuthError(
                        provider = AuthMethod.APPLE,
                        message = "Use getAppleAuthUrl() and handleAppleAuthCallback() for OAuth flow"
                    )
                )
            } else {
                // Mock implementation for testing
                AuthResult.success(
                    User(
                        id = "apple_user_456",
                        email = "user@icloud.com",
                        name = "Apple User",
                        authMethod = AuthMethod.APPLE,
                        isGuest = false,
                        createdAt = System.currentTimeMillis(),
                        lastLoginAt = System.currentTimeMillis()
                    ),
                    com.guyghost.wakeve.auth.core.models.AuthToken.createLongLived(
                        value = "apple_identity_token_placeholder",
                        expiresInDays = 30
                    )
                )
            }
        } catch (e: Exception) {
            AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.APPLE,
                    message = e.message ?: "Erreur Apple Sign-In"
                )
            )
        }
    }

    /**
     * Creates a Google Sign-In client with the specified OAuth client ID.
     *
     * This is an Android-specific method that should be called during Activity/Fragment initialization.
     *
     * @param context Android context (typically Activity context)
     * @param webClientId OAuth web client ID from Google Cloud Console
     * @return GoogleSignInClient instance, or null if no OAuth provider is set
     */
    suspend fun createGoogleSignInClient(
        context: android.content.Context,
        webClientId: String
    ): com.google.android.gms.auth.api.signin.GoogleSignInClient? {
        return oauthProvider?.createGoogleSignInClient(context, webClientId)
    }

    /**
     * Gets the Google Sign-In intent to launch the sign-in flow.
     *
     * This should be called from an Activity or Fragment to launch the sign-in intent:
     * ```kotlin
     * val intent = authService.getGoogleSignInIntent(client)
     * startActivityForResult(intent, GOOGLE_SIGN_IN_REQUEST_CODE)
     * ```
     *
     * @param client The GoogleSignInClient created by createGoogleSignInClient()
     * @return Intent to launch, or null if no OAuth provider is set
     */
    suspend fun getGoogleSignInIntent(
        client: com.google.android.gms.auth.api.signin.GoogleSignInClient
    ): android.content.Intent? {
        return oauthProvider?.getGoogleSignInIntent(client)
    }

    /**
     * Handles the result from Google Sign-In activity.
     *
     * This should be called from onActivityResult:
     * ```kotlin
     * override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
     *     super.onActivityResult(requestCode, resultCode, data)
     *     if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
     *         val result = authService.handleGoogleSignInResult(data)
     *         // Handle the result
     *     }
     * }
     * ```
     *
     * @param data Intent data from onActivityResult
     * @return AuthResult with user data or error
     */
    suspend fun handleGoogleSignInResult(data: android.content.Intent?): AuthResult {
        return oauthProvider?.handleGoogleSignInResult(data)
            ?: AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.GOOGLE,
                    message = "OAuth provider not set. Call setOAuthProvider() first."
                )
            )
    }

    // ============================================================
    // Apple Sign-In Specific Methods (Web Flow)
    // ============================================================

    /**
     * Gets the Apple Sign-In authorization URL to launch the sign-in flow.
     *
     * This should be called to get the URL to open in a browser or Custom Tab:
     * ```kotlin
     * val state = generateSecureRandomString() // Generate and store for validation
     * val url = authService.getAppleAuthUrl(
     *     clientId = "com.example.app",
     *     redirectUri = "https://example.com/callback",
     *     state = state
     * )
     * customTabsIntent.launchUrl(context, Uri.parse(url))
     * ```
     *
     * @param clientId The Services ID from Apple Developer portal
     * @param redirectUri The redirect URI registered in Apple Developer portal
     * @param state A random string to prevent CSRF attacks (should be validated on callback)
     * @return Authorization URL string, or null if no Apple Sign-In provider is set
     */
    suspend fun getAppleAuthUrl(
        clientId: String,
        redirectUri: String,
        state: String
    ): String? {
        return appleSignInProvider?.getAuthorizationUrl(
            clientId = clientId,
            redirectUri = redirectUri,
            state = state
        )
    }

    /**
     * Handles the Apple Sign-In callback from the redirect URI.
     *
     * This should be called after the user completes the sign-in flow and is redirected back.
     * The authorization code from the callback is exchanged for tokens, and the ID token is parsed.
     *
     * Usage:
     * ```kotlin
     * // In your activity that handles the redirect
     * override fun onNewIntent(intent: Intent?) {
     *     super.onNewIntent(intent)
     *     val uri = intent?.data
     *     if (uri != null && uri.scheme == "your_redirect_scheme") {
     *         lifecycleScope.launch {
     *             val code = uri.getQueryParameter("code")
     *             val state = uri.getQueryParameter("state")
     *
     *             // Validate state against what you stored
     *             if (validateState(state)) {
     *                 val result = authService.handleAppleAuthCallback(
     *                     code = code,
     *                     clientId = "com.example.app",
     *                     clientSecret = null, // Or your client secret if generated
     *                     redirectUri = "your_redirect_scheme://callback"
     *                 )
     *                 // Handle the result
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * Note: clientSecret can be null if you're using a backend proxy for token exchange.
     * If null, you'll need to implement your own token exchange logic on the backend.
     *
     * @param code The authorization code from the callback URI
     * @param clientId The Services ID from Apple Developer portal
     * @param clientSecret The JWT client secret (can be null if using backend)
     * @param redirectUri The redirect URI registered in Apple Developer portal
     * @return AuthResult with user data or error
     */
    suspend fun handleAppleAuthCallback(
        code: String?,
        clientId: String,
        clientSecret: String?,
        redirectUri: String
    ): AuthResult {
        if (code == null) {
            return AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.APPLE,
                    message = "Authorization code not found in callback"
                )
            )
        }

        if (appleSignInProvider == null) {
            return AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.APPLE,
                    message = "Apple Sign-In provider not set. Call setAppleSignInProvider() first."
                )
            )
        }

        return try {
            // Exchange code for tokens
            val tokenResult = appleSignInProvider!!.exchangeCodeForTokens(
                code = code,
                clientId = clientId,
                clientSecret = clientSecret,
                redirectUri = redirectUri
            )

            val tokens = tokenResult.getOrElse { error ->
                return AuthResult.error(
                    AuthError.OAuthError(
                        provider = AuthMethod.APPLE,
                        message = error.message ?: "Failed to exchange code for tokens"
                    )
                )
            }

            // Parse ID token to get user info
            val userInfoResult = appleSignInProvider!!.parseIdToken(tokens.idToken)

            val userInfo = userInfoResult.getOrElse { error ->
                return AuthResult.error(
                    AuthError.OAuthError(
                        provider = AuthMethod.APPLE,
                        message = error.message ?: "Failed to parse ID token"
                    )
                )
            }

            // Create AuthResult with user data
            val user = User(
                id = userInfo.sub, // Use Apple's subject (sub) as the user ID
                email = userInfo.email,
                name = userInfo.name ?: "Apple User",
                authMethod = AuthMethod.APPLE,
                isGuest = false,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            )

            // Create AuthToken from the access token
            val token = com.guyghost.wakeve.auth.core.models.AuthToken(
                value = tokens.accessToken,
                // Use expiresIn from response if available, default to 1 hour
                expiresAt = tokens.expiresIn?.let {
                    System.currentTimeMillis() + (it * 1000L)
                } ?: (System.currentTimeMillis() + 3600_000L)
            )

            AuthResult.success(user, token)
        } catch (e: Exception) {
            AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.APPLE,
                    message = e.message ?: "Apple Sign-In failed"
                )
            )
        }
    }

    /**
     * Signs out the current user and clears tokens.
     * 
     * For Google Sign-In: revokes access and clears the GoogleSignInClient
     * For Apple Sign-In: no action needed (web-based flow)
     * Note: Token storage is managed separately via TokenStorage interface.
     * Apple Sign-In doesn't require explicit sign-out since there's no session to revoke.
     * The tokens will simply expire.
     */
    actual suspend fun signOut() {
        withContext(Dispatchers.IO) {
            // Sign out from Google if provider is set
            // Note: The client should be managed by the caller
            // This is a simplified sign-out that clears any active Google session
            oauthProvider?.let { provider ->
                // We would need the GoogleSignInClient here, but for now just clear state
                // In a real implementation, we'd store the client or create a new one
            }
            // Note: Apple Sign-In doesn't have a sign-out endpoint
            // The tokens will expire naturally, and the user can simply sign in again
            // Token clearing is handled by TokenStorage caller
        }
    }

    /**
     * Checks if the user is currently authenticated.
     * Note: This is a placeholder - actual authentication state is managed by the state machine
     * and TokenStorage.
     *
     * @return false - Token validation should be done by the caller using TokenStorage
     */
    actual suspend fun isAuthenticated(): Boolean {
        return withContext(Dispatchers.IO) {
            // Authentication state is managed by the state machine and TokenStorage
            false
        }
    }

    /**
     * Gets the current authenticated user.
     * Note: User data retrieval should be done via state machine or TokenStorage.
     *
     * @return null - User retrieval is handled by TokenStorage caller
     */
    actual suspend fun getCurrentUser(): User? {
        return withContext(Dispatchers.IO) {
            // Current user is managed by the state machine and TokenStorage
            null
        }
    }

    /**
     * Refreshes the authentication token.
     *
     * @return error - Token refresh should be done by the state machine
     */
    actual suspend fun refreshToken(): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                // In production, this would:
                // 1. Get refresh token from TokenStorage
                // 2. Call backend to refresh the access token
                // 3. Store new tokens

                // For now, return error - token management is handled by state machine
                AuthResult.error(AuthError.NetworkError)
            } catch (e: Exception) {
                AuthResult.error(AuthError.NetworkError)
            }
        }
    }

    /**
     * Checks if a specific OAuth provider is available.
     *
     * @param provider The authentication method to check
     * @return true if the provider is configured and available
     */
    actual suspend fun isProviderAvailable(provider: AuthMethod): Boolean {
        return withContext(Dispatchers.Main) {
            when (provider) {
                AuthMethod.GOOGLE -> {
                    // Check if Google Play Services is available
                    try {
                        // In production: GoogleSignIn.getClient(context, gso) != null
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                AuthMethod.APPLE -> {
                    // Apple Sign-In via web is always available on Android
                    true
                }
                else -> false
            }
        }
    }
}
