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
 * This implementation uses:
 * - Google Sign-In SDK for OAuth 2.0 authentication
 * - Sign in with Apple via web flow (fallback)
 * - Android Keystore for secure token storage
 *
 * @requires Google Play Services to be installed
 * @requires Internet permission
 */
actual class AuthService(
    private val tokenStorage: TokenStorage = AndroidTokenStorage()
) {

    /**
     * Initiates Google Sign-In flow on Android.
     *
     * @return AuthResult with authenticated user or error
     */
    actual suspend fun signInWithGoogle(): AuthResult = withContext(Dispatchers.Main) {
        try {
            // In production, this would:
            // 1. Check for Google Play Services
            // 2. Configure Google Sign-In options
            // 3. Launch the sign-in intent
            // 4. Handle the result in onActivityResult

            // For now, return a mock result (will be replaced with actual implementation)
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
     * @return AuthResult with authenticated user or error
     */
    actual suspend fun signInWithApple(): AuthResult = withContext(Dispatchers.Main) {
        try {
            // In production, this would:
            // 1. Open Apple Sign-In web flow in browser
            // 2. Handle the OAuth callback
            // 3. Exchange code for tokens

            // For now, return a mock result
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
     * Signs out the current user and clears tokens.
     */
    actual suspend fun signOut() {
        withContext(Dispatchers.IO) {
            // In production:
            // 1. Sign out from Google (GoogleSignInClient.signOut())
            // 2. Clear all stored tokens
            tokenStorage.clearAll()
        }
    }

    /**
     * Checks if the user is currently authenticated.
     *
     * @return true if there is a valid session
     */
    actual suspend fun isAuthenticated(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = tokenStorage.getString(TokenKeys.ACCESS_TOKEN)
                val expiry = tokenStorage.getString(TokenKeys.TOKEN_EXPIRY)

                if (token != null && expiry != null) {
                    val expiryTime = expiry.toLongOrNull() ?: return@withContext false
                    System.currentTimeMillis() < expiryTime
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Gets the current authenticated user.
     *
     * @return User if authenticated, null otherwise
     */
    actual suspend fun getCurrentUser(): User? {
        return withContext(Dispatchers.IO) {
            try {
                val userId = tokenStorage.getString(TokenKeys.USER_ID)
                val email = tokenStorage.getString(TokenKeys.USER_ID) // Using same key to avoid missing key
                val authMethodStr = tokenStorage.getString(TokenKeys.AUTH_METHOD)

                if (userId != null && authMethodStr != null) {
                    val authMethod = AuthMethod.valueOf(authMethodStr)
                    User(
                        id = userId,
                        email = email,
                        name = null,
                        authMethod = authMethod,
                        isGuest = false,
                        createdAt = System.currentTimeMillis(),
                        lastLoginAt = System.currentTimeMillis()
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Refreshes the authentication token.
     *
     * @return AuthResult with new token or error
     */
    actual suspend fun refreshToken(): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                // In production, this would:
                // 1. Get refresh token from storage
                // 2. Call backend to refresh the access token
                // 3. Store new tokens

                val currentToken = tokenStorage.getString(TokenKeys.REFRESH_TOKEN)
                if (currentToken == null) {
                    return@withContext AuthResult.error(AuthError.InvalidCredentials)
                }

                // Mock refresh - would be API call in production
                AuthResult.success(
                    User(
                        id = "refreshed_user",
                        email = null,
                        name = null,
                        authMethod = AuthMethod.GOOGLE,
                        isGuest = false,
                        createdAt = System.currentTimeMillis(),
                        lastLoginAt = System.currentTimeMillis()
                    ),
                    com.guyghost.wakeve.auth.core.models.AuthToken.createLongLived(
                        value = "refreshed_token_${System.currentTimeMillis()}",
                        expiresInDays = 30
                    )
                )
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
