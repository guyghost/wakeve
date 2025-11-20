package com.guyghost.wakeve.auth

import android.content.Context
import com.guyghost.wakeve.security.AndroidSecureTokenStorage
import com.guyghost.wakeve.models.OAuthLoginRequest
import com.guyghost.wakeve.models.OAuthLoginResponse
import com.guyghost.wakeve.models.OAuthProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

/**
 * Android-specific authentication service
 */
class AndroidAuthenticationService(
    context: Context,
    baseUrl: String = "http://10.0.2.2:8080" // Android emulator localhost
) : ClientAuthenticationService(
    secureStorage = AndroidSecureTokenStorage(context),
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
        val request = OAuthLoginRequest(
            provider = OAuthProvider.APPLE.name,
            authorizationCode = authorizationCode,
            accessToken = userInfo // For Apple, userInfo contains the identity token
        )
        return performLoginRequest(request).onSuccess { response ->
            storeTokens(response)
        }
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
     * Handle Google OAuth2 callback from Custom Tabs or WebView
     */
    suspend fun handleGoogleCallback(authorizationCode: String): Result<OAuthLoginResponse> {
        return loginWithGoogle(authorizationCode)
    }

    /**
     * Handle Apple Sign In callback
     */
    suspend fun handleAppleCallback(authorizationCode: String, userInfo: String? = null): Result<OAuthLoginResponse> {
        return loginWithApple(authorizationCode, userInfo)
    }
}