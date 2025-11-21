package com.guyghost.wakeve.auth

import com.guyghost.wakeve.models.OAuthProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * OAuth2 service for Apple Sign In authentication
 */
class AppleOAuth2Service(
    private val clientId: String,
    private val teamId: String,
    private val keyId: String,
    private val privateKey: String, // PKCS#8 format
    private val redirectUri: String
) : OAuth2Service {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    override suspend fun exchangeCodeForToken(code: String): OAuthTokenResponse {
        val clientSecret = generateClientSecret()

        val response = httpClient.post("https://appleid.apple.com/auth/token") {
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
        // Apple doesn't provide a user info endpoint like Google
        // User info is provided in the authorization code flow
        // This method should not be called for Apple Sign In
        throw UnsupportedOperationException("Apple Sign In user info should be obtained from the authorization response")
    }

    override suspend fun refreshToken(refreshToken: String): OAuthTokenResponse {
        val clientSecret = generateClientSecret()

        val response = httpClient.post("https://appleid.apple.com/auth/token") {
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
            "scope" to "name email",
            "response_mode" to "form_post",
            "state" to state
        ).formUrlEncode()

        return "https://appleid.apple.com/auth/authorize?$params"
    }

    /**
     * Parse user info from Apple's authorization response
     * Apple provides user info in the POST body during the authorization flow
     */
    fun parseUserInfoFromAuthResponse(userJson: String?): OAuthUserInfo? {
        if (userJson.isNullOrBlank()) return null

        return try {
            val appleUserInfo = Json.decodeFromString<AppleUserInfo>(userJson)
            OAuthUserInfo(
                id = appleUserInfo.sub,
                email = appleUserInfo.email,
                name = appleUserInfo.name?.let { "${it.firstName} ${it.lastName}".trim() },
                avatarUrl = null, // Apple doesn't provide avatar
                provider = OAuthProvider.APPLE
            )
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateClientSecret(): String {
        val now = System.currentTimeMillis() / 1000
        val exp = now + 3600 // 1 hour

        val header = mapOf(
            "alg" to "ES256",
            "kid" to keyId
        )

        val payload = mapOf(
            "iss" to teamId,
            "iat" to now,
            "exp" to exp,
            "aud" to "https://appleid.apple.com",
            "sub" to clientId
        )

        val headerJson = Json.encodeToString(header)
        val payloadJson = Json.encodeToString(payload)

        val headerEncoded = Base64.UrlSafe.encode(headerJson.toByteArray()).trimEnd('=')
        val payloadEncoded = Base64.UrlSafe.encode(payloadJson.toByteArray()).trimEnd('=')

        val message = "$headerEncoded.$payloadEncoded"
        val signature = signMessage(message, privateKey)

        return "$message.$signature"
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun signMessage(message: String, privateKeyPem: String): String {
        val privateKey = loadPrivateKey(privateKeyPem)
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(message.toByteArray())

        val signedBytes = signature.sign()
        return Base64.UrlSafe.encode(signedBytes).trimEnd('=')
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun loadPrivateKey(privateKeyPem: String): PrivateKey {
        val privateKeyContent = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")

        val keyBytes = Base64.decode(privateKeyContent)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePrivate(keySpec)
    }
}

@Serializable
private data class AppleUserInfo(
    val sub: String,
    val email: String,
    val name: AppleName? = null
)

@Serializable
private data class AppleName(
    @SerialName("firstName")
    val firstName: String,
    @SerialName("lastName")
    val lastName: String
)