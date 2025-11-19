package com.guyghost.wakeve.auth

import com.guyghost.wakeve.models.OAuthProvider
import kotlinx.serialization.Serializable

/**
 * Common interface for OAuth2 services
 */
interface OAuth2Service {
    suspend fun exchangeCodeForToken(code: String): OAuthTokenResponse
    suspend fun getUserInfo(accessToken: String): OAuthUserInfo
    suspend fun refreshToken(refreshToken: String): OAuthTokenResponse
    fun getAuthorizationUrl(state: String): String
}

/**
 * OAuth2 token response from providers
 */
@Serializable
data class OAuthTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Long? = null,
    val refresh_token: String? = null,
    val scope: String? = null,
    val id_token: String? = null // For OpenID Connect
)

/**
 * User information from OAuth2 providers
 */
data class OAuthUserInfo(
    val id: String,
    val email: String,
    val name: String?,
    val avatarUrl: String?,
    val provider: OAuthProvider
)

/**
 * Exception for OAuth2 operations
 */
class OAuth2Exception(message: String, cause: Throwable? = null) : Exception(message, cause)