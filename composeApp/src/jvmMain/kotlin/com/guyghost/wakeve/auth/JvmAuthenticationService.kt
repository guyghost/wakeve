package com.guyghost.wakeve.auth

import com.guyghost.wakeve.models.OAuthLoginRequest
import com.guyghost.wakeve.models.OAuthLoginResponse
import com.guyghost.wakeve.models.OAuthProvider
import com.guyghost.wakeve.security.JvmSecureTokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

/**
 * JVM-specific authentication service
 */
class JvmAuthenticationService(
    baseUrl: String = "http://localhost:8080"
) : ClientAuthenticationService(
    secureStorage = JvmSecureTokenStorage(),
    baseUrl = baseUrl
) {
    private val httpClient = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loginWithGoogle(authorizationCode: String): Result<OAuthLoginResponse> {
        val request = OAuthLoginRequest(
            provider = OAuthProvider.GOOGLE.name,
            authorizationCode = authorizationCode
        )
        return performLoginRequest(request).onSuccess { response ->
            storeTokens(response)
        }
    }

    override suspend fun loginWithApple(authorizationCode: String, userInfo: String?): Result<OAuthLoginResponse> {
        // Apple Sign-In not typically available on JVM/Desktop
        return Result.failure(Exception("Apple Sign-In not supported on JVM"))
    }

    override suspend fun refreshToken(): Result<OAuthLoginResponse> {
        val refreshToken = secureStorage.getRefreshToken()
            ?: return Result.failure(Exception("No refresh token available"))

        val request = OAuthLoginRequest(
            provider = "REFRESH",
            refreshToken = refreshToken
        )
        return performLoginRequest(request).onSuccess { response ->
            storeTokens(response)
        }
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        secureStorage.clearAllTokens()
    }

    override suspend fun getStoredAccessToken(): String? {
        return secureStorage.getAccessToken()
    }

    override suspend fun isLoggedIn(): Boolean {
        return secureStorage.hasValidToken()
    }

    override suspend fun performLoginRequest(request: OAuthLoginRequest): Result<OAuthLoginResponse> = runCatching {
        val response: HttpResponse = httpClient.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status.value == 200) {
            response.body<OAuthLoginResponse>()
        } else {
            throw Exception("Login failed: ${response.status}")
        }
    }

    private suspend fun storeTokens(response: OAuthLoginResponse) {
        secureStorage.storeAccessToken(response.accessToken)
        response.refreshToken?.let { secureStorage.storeRefreshToken(it) }
        response.user.id.let { secureStorage.storeUserId(it) }
        val expiry = System.currentTimeMillis() + (response.expiresIn * 1000)
        secureStorage.storeTokenExpiry(expiry)
    }

    /**
     * Handle Google OAuth2 callback from browser
     */
    suspend fun handleGoogleCallback(authorizationCode: String): Result<OAuthLoginResponse> {
        return loginWithGoogle(authorizationCode)
    }
}
