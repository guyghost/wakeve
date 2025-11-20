package com.guyghost.wakeve.auth

import com.guyghost.wakeve.models.OAuthLoginRequest
import com.guyghost.wakeve.models.OAuthLoginResponse
import com.guyghost.wakeve.security.SecureTokenStorage

/**
 * Common authentication service interface for client-side OAuth2
 */
abstract class ClientAuthenticationService(
    protected val secureStorage: SecureTokenStorage,
    protected val baseUrl: String
) {

    /**
     * Login with Google OAuth2
     */
    abstract suspend fun loginWithGoogle(authorizationCode: String): Result<OAuthLoginResponse>

    /**
     * Login with Apple OAuth2
     */
    abstract suspend fun loginWithApple(authorizationCode: String, userInfo: String? = null): Result<OAuthLoginResponse>

    /**
     * Refresh the current access token
     */
    abstract suspend fun refreshToken(): Result<OAuthLoginResponse>

    /**
     * Logout and clear stored tokens
     */
    abstract suspend fun logout(): Result<Unit>

    /**
     * Get stored access token
     */
    abstract suspend fun getStoredAccessToken(): String?

    /**
     * Check if user is logged in
     */
    abstract suspend fun isLoggedIn(): Boolean

    /**
     * Helper method to make OAuth login request to server
     */
    protected abstract suspend fun performLoginRequest(request: OAuthLoginRequest): Result<OAuthLoginResponse>
}