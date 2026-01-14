package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.auth.core.logic.JWTPayload
import com.guyghost.wakeve.auth.core.logic.parseJWT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Implementation of AppleSignInProvider for Android using OAuth 2.0 web flow.
 *
 * This class implements the Sign in with Apple REST API for Android clients.
 * Since there's no native Sign in with Apple SDK for Android, we use the
 * web-based OAuth 2.0 authorization flow.
 *
 * Architecture:
 * - getAuthorizationUrl() builds the authorization URL for the browser/Custom Tab
 * - exchangeCodeForTokens() makes a POST request to Apple's token endpoint
 * - parseIdToken() decodes the JWT id_token to extract user information
 *
 * Security Notes:
 * - JWT signature verification is NOT implemented (should be done on backend)
 * - client_secret is required for token exchange (consider using backend proxy)
 * - state parameter must be validated on callback to prevent CSRF attacks
 *
 * @param httpClient Ktor HTTP client (optional, defaults to CIO-based client)
 *
 * Reference: https://developer.apple.com/documentation/sign-in-with-apple/rest-api
 */
class AppleSignInWebFlow(
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
) : AppleSignInProvider {

    companion object {
        private const val AUTHORIZATION_ENDPOINT = "https://appleid.apple.com/auth/authorize"
        private const val TOKEN_ENDPOINT = "https://appleid.apple.com/auth/token"
    }

    /**
     * Constructs the Apple Sign-In authorization URL.
     *
     * @param clientId The Services ID from Apple Developer portal
     * @param redirectUri The redirect URI registered in Apple Developer portal
     * @param state A random string to prevent CSRF attacks
     * @param scopes OAuth scopes (name, email)
     * @return Complete authorization URL
     */
    override suspend fun getAuthorizationUrl(
        clientId: String,
        redirectUri: String,
        state: String,
        scopes: List<String>
    ): String {
        val params = mapOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "response_mode" to "query",
            "scope" to scopes.joinToString(" "),
            "state" to state
        )

        val queryString = params.entries
            .map { (key, value) -> "$key=${value.encodeURLParameter()}" }
            .joinToString("&")

        return "$AUTHORIZATION_ENDPOINT?$queryString"
    }

    /**
     * Exchanges the authorization code for access and ID tokens.
     *
     * POST to https://appleid.apple.com/auth/token with:
     * - code: The authorization code
     * - client_id: The Services ID
     * - client_secret: JWT client secret (generated from Apple private key)
     * - redirect_uri: The redirect URI
     * - grant_type: authorization_code
     *
     * @param code The authorization code received in the redirect callback
     * @param clientId The Services ID
     * @param clientSecret The JWT client secret (nullable for fallback)
     * @param redirectUri The redirect URI
     * @return Result<AppleTokenResponse> with tokens or error
     */
    override suspend fun exchangeCodeForTokens(
        code: String,
        clientId: String,
        clientSecret: String?,
        redirectUri: String
    ): Result<AppleTokenResponse> {
        return try {
            val formParams = mutableMapOf(
                "code" to code,
                "client_id" to clientId,
                "redirect_uri" to redirectUri,
                "grant_type" to "authorization_code"
            )

            // Add client_secret if provided
            clientSecret?.let { formParams["client_secret"] = it }

            val response = httpClient.post(TOKEN_ENDPOINT) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(formParams.entries.map { it.key to it.value }.formUrlEncode())
            }

            if (response.status != HttpStatusCode.OK) {
                val errorBody = response.body<JsonObject>()
                val errorMessage = errorBody["error"]?.jsonPrimitive?.content
                    ?: errorBody["error_description"]?.jsonPrimitive?.content
                    ?: "Unknown error"
                return Result.failure(
                    AppleSignInException(
                        "Token exchange failed: $errorMessage (${response.status.value})"
                    )
                )
            }

            val responseBody = response.body<JsonObject>()
            val accessToken = responseBody["access_token"]?.jsonPrimitive?.content
            val idToken = responseBody["id_token"]?.jsonPrimitive?.content
            val refreshToken = responseBody["refresh_token"]?.jsonPrimitive?.content
            val expiresIn = responseBody["expires_in"]?.jsonPrimitive?.content?.toIntOrNull()

            if (accessToken == null || idToken == null) {
                return Result.failure(
                    AppleSignInException("Invalid token response: missing access_token or id_token")
                )
            }

            Result.success(
                AppleTokenResponse(
                    accessToken = accessToken,
                    idToken = idToken,
                    refreshToken = refreshToken,
                    expiresIn = expiresIn
                )
            )
        } catch (e: Exception) {
            Result.failure(
                AppleSignInException("Token exchange failed: ${e.message}", e)
            )
        }
    }

    /**
     * Parses the JWT ID token to extract user information.
     *
     * The ID token is a JWT with the following structure:
     * - header: {"alg": "RS256", "kid": "..."}
     * - payload: {"iss": "https://appleid.apple.com", "sub": "...", "email": "...", ...}
     * - signature: (not verified in this implementation)
     *
     * @param idToken The JWT ID token
     * @return Result<AppleUserInfo> with user information or error
     */
    override suspend fun parseIdToken(idToken: String): Result<AppleUserInfo> {
        return try {
            val payload: JWTPayload = parseJWT(idToken)
                ?: return Result.failure(
                    AppleSignInException("Failed to parse ID token")
                )

            // Validate issuer (should be Apple)
            val issuer = payload.issuer
            if (issuer != "https://appleid.apple.com") {
                return Result.failure(
                    AppleSignInException("Invalid ID token issuer: $issuer")
                )
            }

            // Extract user information
            val sub = payload.subject
                ?: return Result.failure(
                    AppleSignInException("ID token missing 'sub' claim")
                )

            val email = payload.getAs<String>("email")
            val emailVerified = payload.getAs<Boolean>("email_verified") ?: false

            // Extract name (only available on first sign-in)
            val name = extractNameFromPayload(payload)

            Result.success(
                AppleUserInfo(
                    sub = sub,
                    email = email,
                    name = name,
                    emailVerified = emailVerified
                )
            )
        } catch (e: Exception) {
            Result.failure(
                AppleSignInException("Failed to parse ID token: ${e.message}", e)
            )
        }
    }

    /**
     * Extracts the user's name from the JWT payload.
     *
     * Apple provides the name only on the first sign-in in a nested structure:
     * {
     *   "name": {
     *     "firstName": "...",
     *     "lastName": "..."
     *   }
     * }
     *
     * @param payload The JWT payload
     * @return The full name (first + last) or null if not available
     */
    @Suppress("UNCHECKED_CAST")
    private fun extractNameFromPayload(payload: JWTPayload): String? {
        val name = payload["name"] as? Map<String, Any?> ?: return null

        val firstName = name["firstName"] as? String
        val lastName = name["lastName"] as? String

        return when {
            firstName != null && lastName != null -> "$firstName $lastName"
            firstName != null -> firstName
            lastName != null -> lastName
            else -> null
        }
    }
}

/**
 * Exception thrown during Apple Sign-In operations.
 */
class AppleSignInException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Helper function to URL-encode a parameter value.
 */
private fun String.encodeURLParameter(): String {
    // Use Kotlin's built-in URL encoding
    return java.net.URLEncoder.encode(this, "UTF-8")
}

/**
 * Internal serializable for Apple token response (for parsing with kotlinx.serialization).
 */
@Serializable
private data class AppleTokenResponseDto(
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("id_token")
    val idToken: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("expires_in")
    val expiresIn: Int? = null,
    @SerialName("token_type")
    val tokenType: String? = null,
    @SerialName("error")
    val error: String? = null,
    @SerialName("error_description")
    val errorDescription: String? = null
)
