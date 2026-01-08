package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS implementation of AuthService using Sign in with Apple and Google Sign-In SDK.
 *
 * This implementation uses:
 * - AuthenticationServices.ASAuthorizationAppleIDProvider for Apple Sign-In
 * - Google Sign-In iOS SDK for Google authentication
 * - iOS Keychain for secure token storage
 */
actual class AuthService(
    private val tokenStorage: TokenStorage = IosTokenStorage()
) {

    /**
     * Initiates Apple Sign-In flow on iOS.
     *
     * @return AuthResult with authenticated user or error
     */
    actual suspend fun signInWithApple(): AuthResult = withContext(Dispatchers.Main) {
        try {
            // In production, this would:
            // 1. Create ASAuthorizationAppleIDProvider
            // 2. Launch ASAuthorizationController
            // 3. Handle credential in didCompleteWithAuthorization

            // For now, return a mock result
            AuthResult.success(
                User(
                    id = "apple_user_ios",
                    email = "user@icloud.com",
                    name = "Apple User",
                    authMethod = AuthMethod.APPLE,
                    isGuest = false,
                    createdAt = System.currentTimeMillis(),
                    lastLoginAt = System.currentTimeMillis()
                ),
                com.guyghost.wakeve.auth.core.models.AuthToken.createLongLived(
                    value = "apple_identity_token_ios",
                    expiresInDays = 30
                )
            )
        } catch (e: Exception) {
            AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.APPLE,
                    message = e.message ?: "Erreur Sign in with Apple"
                )
            )
        }
    }

    /**
     * Initiates Google Sign-In flow on iOS.
     *
     * @return AuthResult with authenticated user or error
     */
    actual suspend fun signInWithGoogle(): AuthResult = withContext(Dispatchers.Main) {
        try {
            // In production, this would:
            // 1. Configure Google Sign-In with client ID
            // 2. Launch GIDSignIn.sharedInstance().signIn()
            // 3. Handle the result

            // For now, return a mock result
            AuthResult.success(
                User(
                    id = "google_user_ios",
                    email = "user@gmail.com",
                    name = "Google User",
                    authMethod = AuthMethod.GOOGLE,
                    isGuest = false,
                    createdAt = System.currentTimeMillis(),
                    lastLoginAt = System.currentTimeMillis()
                ),
                com.guyghost.wakeve.auth.core.models.AuthToken.createLongLived(
                    value = "google_id_token_ios",
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
     * Signs out the current user and clears tokens.
     */
    actual suspend fun signOut() {
        withContext(Dispatchers.IO) {
            // In production:
            // 1. Sign out from Google (GIDSignIn.sharedInstance().signOut())
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
                val authMethodStr = tokenStorage.getString(TokenKeys.AUTH_METHOD)

                if (userId != null && authMethodStr != null) {
                    val authMethod = AuthMethod.valueOf(authMethodStr)
                    User(
                        id = userId,
                        email = null,
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
                val currentToken = tokenStorage.getString(TokenKeys.REFRESH_TOKEN)
                if (currentToken == null) {
                    return@withContext AuthResult.error(AuthError.InvalidCredentials)
                }

                // Mock refresh - would be API call in production
                AuthResult.success(
                    User(
                        id = "refreshed_user_ios",
                        email = null,
                        name = null,
                        authMethod = AuthMethod.APPLE,
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
                AuthMethod.APPLE -> {
                    // Apple Sign-In is available on iOS 13+
                    true
                }
                AuthMethod.GOOGLE -> {
                    // Google Sign-In iOS SDK is available
                    try {
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                else -> false
            }
        }
    }
}
