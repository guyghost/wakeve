package com.guyghost.wakeve.auth.oauth

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guyghost.wakeve.auth.shell.services.AppleSignInException
import com.guyghost.wakeve.auth.shell.services.AppleSignInProvider
import com.guyghost.wakeve.auth.shell.services.AppleSignInWebFlow
import com.guyghost.wakeve.auth.shell.services.AppleTokenResponse
import com.guyghost.wakeve.auth.shell.services.AppleUserInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Instrumented tests for Apple Sign-In provider.
 *
 * Tests the complete Apple Sign-In OAuth web flow:
 * - Authorization URL construction with proper parameters
 * - URL encoding of special characters
 * - Token exchange with success and error scenarios
 * - JWT ID token parsing and user information extraction
 * - Edge cases (malformed JWT, missing claims, etc.)
 *
 * Uses Ktor MockEngine for HTTP mocking and base64 encoded JWT for testing.
 */
@RunWith(AndroidJUnit4::class)
class AppleSignInProviderTest {

    private lateinit var provider: AppleSignInProvider
    private lateinit var mockEngine: MockEngine
    private lateinit var httpClient: HttpClient

    // Test data
    private val testClientId = "com.example.wakeve"
    private val testRedirectUri = "https://wakeve.example.com/callback"
    private val testState = "test_state_12345"
    private val testCode = "test_authorization_code"
    private val testClientSecret = "test_jwt_client_secret"

    // Valid JWT for testing (decoded: {"iss":"https://appleid.apple.com","sub":"test.user.123","email":"user@icloud.com","email_verified":true,"name":{"firstName":"John","lastName":"Doe"}})
    private val validIdToken = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJpc3MiOiJodHRwczovL2FwcGxlaWQuYXBwbGUuY29tIiwic3ViIjoidGVzdC51c2VyLjEyMyIsImVtYWlsIjoidXNlckBpY2xvdWQuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOnsiZmlyc3ROYW1lIjoiSm9obiIsImxhc3ROYW1lIjoiRG9lIn19."

    // JWT without email
    private val idTokenNoEmail = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJpc3MiOiJodHRwczovL2FwcGxlaWQuYXBwbGUuY29tIiwic3ViIjoidGVzdC51c2VyLjEyMyIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlfQ."

    // JWT without name
    private val idTokenNoName = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJpc3MiOiJodHRwczovL2FwcGxlaWQuYXBwbGUuY29tIiwic3ViIjoidGVzdC51c2VyLjEyMyIsImVtYWlsIjoidXNlckBpY2xvdWQuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWV9."

    @Before
    fun setup() {
        mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/auth/token" -> {
                    respond(
                        content = """{
                            "access_token": "test_access_token",
                            "id_token": "${request.body.toByteArray().toString().let { body ->
                                if (body.contains("invalid_code")) {
                                    """{"error":"invalid_grant","error_description":"Invalid authorization code"}"""
                                } else {
                                    """{"access_token":"test_access_token","id_token":"$validIdToken","expires_in":3600}"""
                                }
                            }}""",
                        status = if (request.body.toByteArray().toString().contains("invalid_code")) {
                            HttpStatusCode.BadRequest
                        } else {
                            HttpStatusCode.OK
                        },
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> error("Unhandled ${request.url.encodedPath}")
            }
        }

        httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }

        provider = AppleSignInWebFlow(httpClient)
    }

    // ==================== getAuthorizationUrl Tests ====================

    /**
     * Test getAuthorizationUrl constructs URL with correct parameters.
     *
     * GIVEN client ID, redirect URI, state, and scopes
     * WHEN calling getAuthorizationUrl
     * THEN URL contains all required parameters with correct values
     */
    @Test
    fun `getAuthorizationUrl contains correct base URL and parameters`() = runTest {
        // ACT
        val url = provider.getAuthorizationUrl(testClientId, testRedirectUri, testState)

        // ASSERT
        assertTrue(url.startsWith("https://appleid.apple.com/auth/authorize"))
        assertTrue(url.contains("client_id=$testClientId"))
        assertTrue(url.contains("redirect_uri=${testRedirectUri.encodeURLParameter()}"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("response_mode=query"))
        assertTrue(url.contains("state=$testState"))
        assertTrue(url.contains("scope=name%20email"))
    }

    /**
     * Test getAuthorizationUrl uses default scopes.
     *
     * GIVEN client ID, redirect URI, and state
     * WHEN calling getAuthorizationUrl without specifying scopes
     * THEN URL contains default scopes (name email)
     */
    @Test
    fun `getAuthorizationUrl uses default scopes when not specified`() = runTest {
        // ACT
        val url = provider.getAuthorizationUrl(testClientId, testRedirectUri, testState)

        // ASSERT
        assertTrue(url.contains("scope=name%20email"))
    }

    /**
     * Test getAuthorizationUrl uses custom scopes.
     *
     * GIVEN client ID, redirect URI, state, and custom scopes
     * WHEN calling getAuthorizationUrl with custom scopes
     * THEN URL contains the custom scopes
     */
    @Test
    fun `getAuthorizationUrl uses custom scopes when specified`() = runTest {
        // ARRANGE
        val customScopes = listOf("email")

        // ACT
        val url = provider.getAuthorizationUrl(testClientId, testRedirectUri, testState, customScopes)

        // ASSERT
        assertTrue(url.contains("scope=email"))
        assertFalse(url.contains("name"))
    }

    /**
     * Test getAuthorizationUrl parameters are URL encoded.
     *
     * GIVEN client ID and redirect URI with special characters
     * WHEN calling getAuthorizationUrl
     * THEN special characters are properly URL-encoded
     */
    @Test
    fun `getAuthorizationUrl parameters are URL encoded`() = runTest {
        // ARRANGE
        val clientIdWithSpecialChars = "com.example+wakeve"
        val redirectUriWithSpecialChars = "https://wakeve.example.com/callback?param=value&other=test"

        // ACT
        val url = provider.getAuthorizationUrl(clientIdWithSpecialChars, redirectUriWithSpecialChars, testState)

        // ASSERT
        // Check that special characters are encoded
        assertTrue(url.contains("client_id=com.example%2Bwakeve"))
        assertTrue(url.contains("redirect_uri=https%3A%2F%2Fwakeve.example.com%2Fcallback%3Fparam%3Dvalue%26other%3Dtest"))
        // Check that ampersands in URL parameters are not encoded (they're separators)
        assertTrue(url.contains("&"))
    }

    /**
     * Test getAuthorizationUrl with empty scopes.
     *
     * GIVEN empty scopes list
     * WHEN calling getAuthorizationUrl
     * THEN URL contains empty scope parameter
     */
    @Test
    fun `getAuthorizationUrl handles empty scopes`() = runTest {
        // ARRANGE
        val emptyScopes = emptyList<String>()

        // ACT
        val url = provider.getAuthorizationUrl(testClientId, testRedirectUri, testState, emptyScopes)

        // ASSERT
        assertTrue(url.contains("scope="))
    }

    // ==================== exchangeCodeForTokens Success Tests ====================

    /**
     * Test exchangeCodeForTokens success returns correct token response.
     *
     * GIVEN valid authorization code and parameters
     * WHEN calling exchangeCodeForTokens
     * THEN Result.success with AppleTokenResponse is returned
     */
    @Test
    fun `exchangeCodeForTokens success returns AppleTokenResponse`() = runTest {
        // ACT
        val result = provider.exchangeCodeForTokens(testCode, testClientId, testClientSecret, testRedirectUri)

        // ASSERT
        assertTrue(result.isSuccess)
        val tokenResponse = result.getOrThrow()
        
        assertEquals("test_access_token", tokenResponse.accessToken)
        assertEquals(validIdToken, tokenResponse.idToken)
        assertEquals(3600, tokenResponse.expiresIn)
        assertEquals(null, tokenResponse.refreshToken)
    }

    /**
     * Test exchangeCodeForTokens success without client secret.
     *
     * GIVEN valid authorization code without client secret
     * WHEN calling exchangeCodeForTokens
     * THEN Result.success is returned (depends on Apple's response)
     */
    @Test
    fun `exchangeCodeForTokens works without client secret`() = runTest {
        // ACT
        val result = provider.exchangeCodeForTokens(testCode, testClientId, null, testRedirectUri)

        // ASSERT
        assertTrue(result.isSuccess)
        val tokenResponse = result.getOrThrow()
        
        assertEquals("test_access_token", tokenResponse.accessToken)
        assertEquals(validIdToken, tokenResponse.idToken)
    }

    // ==================== exchangeCodeForTokens Error Tests ====================

    /**
     * Test exchangeCodeForTokens invalid code returns error.
     *
     * GIVEN invalid authorization code
     * WHEN calling exchangeCodeForTokens
     * THEN Result.failure with AppleSignInException is returned
     */
    @Test
    fun `exchangeCodeForTokens invalid code returns failure`() = runTest {
        // ARRANGE
        val invalidCode = "invalid_code"

        // ACT
        val result = provider.exchangeCodeForTokens(invalidCode, testClientId, testClientSecret, testRedirectUri)

        // ASSERT
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AppleSignInException)
        assertTrue(exception!!.message!!.contains("Token exchange failed"))
        assertTrue(exception.message!!.contains("invalid_grant"))
    }

    /**
     * Test exchangeCodeForTokens network error returns failure.
     *
     * GIVEN a network error during token exchange
     * WHEN calling exchangeCodeForTokens
     * THEN Result.failure with AppleSignInException is returned
     */
    @Test
    fun `exchangeCodeForTokens network error returns failure`() = runTest {
        // ARRANGE
        val networkErrorEngine = MockEngine { _ ->
            throw Exception("Network error")
        }
        val networkErrorProvider = AppleSignInWebFlow(HttpClient(networkErrorEngine))

        // ACT
        val result = networkErrorProvider.exchangeCodeForTokens(testCode, testClientId, testClientSecret, testRedirectUri)

        // ASSERT
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AppleSignInException)
        assertTrue(exception!!.message!!.contains("Token exchange failed"))
    }

    // ==================== parseIdToken Success Tests ====================

    /**
     * Test parseIdToken success extracts user information.
     *
     * GIVEN a valid JWT ID token
     * WHEN calling parseIdToken
     * THEN Result.success with AppleUserInfo containing all user data is returned
     */
    @Test
    fun `parseIdToken success extracts complete user information`() = runTest {
        // ACT
        val result = provider.parseIdToken(validIdToken)

        // ASSERT
        assertTrue(result.isSuccess)
        val userInfo = result.getOrThrow()
        
        assertEquals("test.user.123", userInfo.sub)
        assertEquals("user@icloud.com", userInfo.email)
        assertEquals("John Doe", userInfo.name)
        assertTrue(userInfo.emailVerified)
    }

    /**
     * Test parseIdToken validates issuer.
     *
     * GIVEN a JWT with correct Apple issuer
     * WHEN calling parseIdToken
     * THEN user info is extracted successfully
     */
    @Test
    fun `parseIdToken validates correct Apple issuer`() = runTest {
        // ACT
        val result = provider.parseIdToken(validIdToken)

        // ASSERT
        assertTrue(result.isSuccess)
    }

    // ==================== parseIdToken Error Tests ====================

    /**
     * Test parseIdToken malformed JWT returns error.
     *
     * GIVEN a malformed JWT string
     * WHEN calling parseIdToken
     * THEN Result.failure with AppleSignInException is returned
     */
    @Test
    fun `parseIdToken malformed JWT returns failure`() = runTest {
        // ARRANGE
        val malformedJWT = "not.a.valid.jwt"

        // ACT
        val result = provider.parseIdToken(malformedJWT)

        // ASSERT
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AppleSignInException)
        assertTrue(exception!!.message!!.contains("Failed to parse ID token"))
    }

    /**
     * Test parseIdToken empty JWT returns error.
     *
     * GIVEN an empty JWT string
     * WHEN calling parseIdToken
     * THEN Result.failure is returned
     */
    @Test
    fun `parseIdToken empty JWT returns failure`() = runTest {
        // ACT
        val result = provider.parseIdToken("")

        // ASSERT
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AppleSignInException)
    }

    /**
     * Test parseIdToken null JWT returns error.
     *
     * GIVEN null JWT
     * WHEN calling parseIdToken
     * THEN Result.failure is returned
     */
    @Test
    fun `parseIdToken null JWT returns failure`() = runTest {
        // ACT
        val result = provider.parseIdToken("null")

        // ASSERT
        assertTrue(result.isFailure)
    }

    /**
     * Test parseIdToken invalid issuer returns error.
     *
     * GIVEN a JWT with invalid issuer
     * WHEN calling parseIdToken
     * THEN Result.failure with issuer validation error is returned
     */
    @Test
    fun `parseIdToken invalid issuer returns failure`() = runTest {
        // ARRANGE
        // JWT with invalid issuer: {"iss":"https://evil.com","sub":"test.user.123"}
        val invalidIssuerJWT = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJpc3MiOiJodHRwczovL2V2aWwuY29tIiwic3ViIjoidGVzdC51c2VyLjEyMyJ9."

        // ACT
        val result = provider.parseIdToken(invalidIssuerJWT)

        // ASSERT
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AppleSignInException)
        assertTrue(exception!!.message!!.contains("Invalid ID token issuer"))
    }

    /**
     * Test parseIdToken missing sub claim returns error.
     *
     * GIVEN a JWT without sub claim
     * WHEN calling parseIdToken
     * THEN Result.failure with missing sub error is returned
     */
    @Test
    fun `parseIdToken missing sub claim returns failure`() = runTest {
        // ARRANGE
        // JWT without sub: {"iss":"https://appleid.apple.com","email":"user@icloud.com"}
        val noSubJWT = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJpc3MiOiJodHRwczovL2FwcGxlaWQuYXBwbGUuY29tIiwiZW1haWwiOiJ1c2VyQGljbG91ZC5jb20ifQ."

        // ACT
        val result = provider.parseIdToken(noSubJWT)

        // ASSERT
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AppleSignInException)
        assertTrue(exception!!.message!!.contains("missing 'sub' claim"))
    }

    // ==================== parseIdToken Edge Cases ====================

    /**
     * Test parseIdToken no email handles null email.
     *
     * GIVEN a JWT without email claim
     * WHEN calling parseIdToken
     * THEN AppleUserInfo with null email is returned
     */
    @Test
    fun `parseIdToken no email returns user info with null email`() = runTest {
        // ACT
        val result = provider.parseIdToken(idTokenNoEmail)

        // ASSERT
        assertTrue(result.isSuccess)
        val userInfo = result.getOrThrow()
        
        assertEquals("test.user.123", userInfo.sub)
        assertEquals(null, userInfo.email)
        assertEquals(null, userInfo.name)
        assertFalse(userInfo.emailVerified)
    }

    /**
     * Test parseIdToken no name handles null name.
     *
     * GIVEN a JWT without name claim
     * WHEN calling parseIdToken
     * THEN AppleUserInfo with null name is returned
     */
    @Test
    fun `parseIdToken no name returns user info with null name`() = runTest {
        // ACT
        val result = provider.parseIdToken(idTokenNoName)

        // ASSERT
        assertTrue(result.isSuccess)
        val userInfo = result.getOrThrow()
        
        assertEquals("test.user.123", userInfo.sub)
        assertEquals("user@icloud.com", userInfo.email)
        assertEquals(null, userInfo.name)
        assertTrue(userInfo.emailVerified)
    }

    /**
     * Test parseIdToken with email_verified false.
     *
     * GIVEN a JWT with email_verified: false
     * WHEN calling parseIdToken
     * THEN AppleUserInfo with emailVerified=false is returned
     */
    @Test
    fun `parseIdToken unverified email returns emailVerified false`() = runTest {
        // ARRANGE
        // JWT with unverified email: {"iss":"https://appleid.apple.com","sub":"test.user.123","email":"user@icloud.com","email_verified":false}
        val unverifiedEmailJWT = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJpc3MiOiJodHRwczovL2FwcGxlaWQuYXBwbGUuY29tIiwic3ViIjoidGVzdC51c2VyLjEyMyIsImVtYWlsIjoidXNlckBpY2xvdWQuY29tIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlfQ."

        // ACT
        val result = provider.parseIdToken(unverifiedEmailJWT)

        // ASSERT
        assertTrue(result.isSuccess)
        val userInfo = result.getOrThrow()
        
        assertEquals("test.user.123", userInfo.sub)
        assertEquals("user@icloud.com", userInfo.email)
        assertFalse(userInfo.emailVerified)
    }

    /**
     * Test parseIdToken with name only first name.
     *
     * GIVEN a JWT with name containing only first name
     * WHEN calling parseIdToken
     * THEN AppleUserInfo with first name only is returned
     */
    @Test
    fun `parseIdToken name with only first name returns first name`() = runTest {
        // ARRANGE
        // JWT with only first name: {"iss":"https://appleid.apple.com","sub":"test.user.123","email":"user@icloud.com","email_verified":true,"name":{"firstName":"John"}}
        val firstNameOnlyJWT = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJpc3MiOiJodHRwczovL2FwcGxlaWQuYXBwbGUuY29tIiwic3ViIjoidGVzdC51c2VyLjEyMyIsImVtYWlsIjoidXNlckBpY2xvdWQuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOnsiZmlyc3ROYW1lIjoiSm9obiJ9fQ."

        // ACT
        val result = provider.parseIdToken(firstNameOnlyJWT)

        // ASSERT
        assertTrue(result.isSuccess)
        val userInfo = result.getOrThrow()
        
        assertEquals("John", userInfo.name)
    }

    /**
     * Test parseIdToken with name only last name.
     *
     * GIVEN a JWT with name containing only last name
     * WHEN calling parseIdToken
     * THEN AppleUserInfo with last name only is returned
     */
    @Test
    fun `parseIdToken name with only last name returns last name`() = runTest {
        // ARRANGE
        // JWT with only last name: {"iss":"https://appleid.apple.com","sub":"test.user.123","email":"user@icloud.com","email_verified":true,"name":{"lastName":"Doe"}}
        val lastNameOnlyJWT = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJpc3MiOiJodHRwczovL2FwcGxlaWQuYXBwbGUuY29tIiwic3ViIjoidGVzdC51c2VyLjEyMyIsImVtYWlsIjoidXNlckBpY2xvdWQuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOnsibGFzdE5hbWUiOiJEb2UifX0."

        // ACT
        val result = provider.parseIdToken(lastNameOnlyJWT)

        // ASSERT
        assertTrue(result.isSuccess)
        val userInfo = result.getOrThrow()
        
        assertEquals("Doe", userInfo.name)
    }

    // ==================== URL Encoding Helper Tests ====================

    /**
     * Test URL encoding handles various special characters.
     *
     * GIVEN various special characters in parameters
     * WHEN calling getAuthorizationUrl
     * THEN all characters are properly encoded
     */
    @Test
    fun `URL encoding handles various special characters`() = runTest {
        // ARRANGE
        val specialChars = "!@#$%^&*(){}[]|\\:;\"'<>?,./"
        val clientId = "com.example$app"
        val redirectUri = "https://example.com/path?param=$specialChars"
        val state = "state&with=special"

        // ACT
        val url = provider.getAuthorizationUrl(clientId, redirectUri, state)

        // ASSERT
        // Check that ampersands in parameter values are encoded
        assertTrue(url.contains("client_id=com.example%24app"))
        assertTrue(url.contains("state=state%26with%3Dspecial"))
        
        // Check that query parameter separators remain unencoded
        assertTrue(url.contains("?"))
        assertTrue(url.contains("&"))
    }

    /**
     * Helper function to URL-encode a parameter value for testing.
     */
    private fun String.encodeURLParameter(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }
}