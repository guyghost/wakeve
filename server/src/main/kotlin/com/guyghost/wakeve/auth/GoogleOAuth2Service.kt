package com.guyghost.wakeve.auth

import com.guyghost.wakeve.models.OAuthProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OAuth2 service for Google authentication
 */
class GoogleOAuth2Service(
    private val clientId: String,
    private val clientSecret: String,
    private val redirectUri: String
) : OAuth2Service {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    override suspend fun exchangeCodeForToken(code: String): OAuthTokenResponse {
        val response = httpClient.post("https://oauth2.googleapis.com/token") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                listOf(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "code" to code,
                    "grant_type" to "authorization_code",
                    "redirect_uri" to redirectUri
                ).formUrlEncode()
            )
        }

        if (response.status != HttpStatusCode.OK) {
            throw OAuth2Exception("Failed to exchange code for token: ${response.status}")
        }

        return response.body<OAuthTokenResponse>()
    }

    override suspend fun getUserInfo(accessToken: String): OAuthUserInfo {
        val response = httpClient.get("https://www.googleapis.com/oauth2/v2/userinfo") {
            bearerAuth(accessToken)
        }

        if (response.status != HttpStatusCode.OK) {
            throw OAuth2Exception("Failed to get user info: ${response.status}")
        }

        val googleUserInfo = response.body<GoogleUserInfo>()
        return OAuthUserInfo(
            id = googleUserInfo.id,
            email = googleUserInfo.email,
            name = googleUserInfo.name,
            avatarUrl = googleUserInfo.picture,
            provider = OAuthProvider.GOOGLE
        )
    }

    override suspend fun refreshToken(refreshToken: String): OAuthTokenResponse {
        val response = httpClient.post("https://oauth2.googleapis.com/token") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                listOf(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "refresh_token" to refreshToken,
                    "grant_type" to "refresh_token"
                ).formUrlEncode()
            )
        }

        if (response.status != HttpStatusCode.OK) {
            throw OAuth2Exception("Failed to refresh token: ${response.status}")
        }

        return response.body<OAuthTokenResponse>()
    }

    override fun getAuthorizationUrl(state: String): String {
        val params = listOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to "openid email profile",
            "state" to state,
            "access_type" to "offline",
            "prompt" to "consent"
        ).formUrlEncode()

        return "https://accounts.google.com/o/oauth2/v2/auth?$params"
    }
}

@Serializable
private data class GoogleUserInfo(
    val id: String,
    val email: String,
    val name: String,
    val picture: String? = null
)