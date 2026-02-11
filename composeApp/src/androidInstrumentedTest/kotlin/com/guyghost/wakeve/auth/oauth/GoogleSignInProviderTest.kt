package com.guyghost.wakeve.auth.oauth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.TokenType
import com.guyghost.wakeve.auth.core.models.User
import com.guyghost.wakeve.auth.core.models.AuthToken
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for Google Sign-In provider logic.
 *
 * These tests validate:
 * - AuthResult creation from user data
 * - AuthError handling for various scenarios
 * - AuthToken creation with correct properties
 * - User model creation with Google auth method
 *
 * Note: Actual GoogleSignInClient tests require device/emulator with Google Play Services.
 * These tests focus on the business logic without mocking Google's static methods.
 */
@RunWith(AndroidJUnit4::class)
class GoogleSignInProviderTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    // ==================== AuthResult.Success Creation Tests ====================

    /**
     * Test AuthResult.Success with complete user data.
     *
     * GIVEN complete Google user data (id, email, name, token)
     * WHEN creating AuthResult.Success
     * THEN all user and token data is correctly populated
     */
    @Test
    fun `AuthResult success with complete user data`() = runTest {
        // ARRANGE
        val testId = "google-user-123"
        val testEmail = "test@gmail.com"
        val testName = "Test User"
        val testIdToken = "test-id-token"

        // ACT
        val user = User.createAuthenticated(
            id = testId,
            email = testEmail,
            name = testName,
            authMethod = AuthMethod.GOOGLE
        )
        val token = AuthToken.createLongLived(
            value = testIdToken,
            type = TokenType.BEARER,
            expiresInDays = 30
        )
        val result = AuthResult.success(user, token)

        // ASSERT
        assertTrue(result is AuthResult.Success)
        assertEquals(testId, result.user.id)
        assertEquals(testEmail, result.user.email)
        assertEquals(testName, result.user.name)
        assertEquals(AuthMethod.GOOGLE, result.user.authMethod)
        assertEquals(testIdToken, result.token.value)
        assertEquals(TokenType.BEARER, result.token.type)
    }

    /**
     * Test AuthResult.Success with null display name.
     *
     * GIVEN Google user data without display name
     * WHEN creating AuthResult.Success
     * THEN user is created with null name
     */
    @Test
    fun `AuthResult success handles null display name`() = runTest {
        // ARRANGE
        val testId = "google-user-123"
        val testEmail = "test@gmail.com"
        val testIdToken = "test-id-token"

        // ACT
        val user = User.createAuthenticated(
            id = testId,
            email = testEmail,
            name = null,
            authMethod = AuthMethod.GOOGLE
        )
        val token = AuthToken.createLongLived(
            value = testIdToken,
            type = TokenType.BEARER,
            expiresInDays = 30
        )
        val result = AuthResult.success(user, token)

        // ASSERT
        assertTrue(result is AuthResult.Success)
        assertEquals(testId, result.user.id)
        assertEquals(testEmail, result.user.email)
        assertEquals(null, result.user.name)
        assertEquals(AuthMethod.GOOGLE, result.user.authMethod)
    }

    // ==================== AuthResult.Error Tests ====================

    /**
     * Test OAuthCancelled error creation.
     *
     * GIVEN user cancelled Google sign-in (status code 12501)
     * WHEN creating AuthResult.Error
     * THEN OAuthCancelled error is created with GOOGLE provider
     */
    @Test
    fun `AuthError OAuthCancelled for user cancellation`() = runTest {
        // ACT
        val error = AuthError.OAuthCancelled(provider = AuthMethod.GOOGLE)
        val result = AuthResult.error(error)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        val authError = result.error
        assertTrue(authError is AuthError.OAuthCancelled)
        assertEquals(AuthMethod.GOOGLE, (authError as AuthError.OAuthCancelled).provider)
    }

    /**
     * Test OAuthError error creation for re-authentication.
     *
     * GIVEN sign-in required status (status code 4)
     * WHEN creating AuthResult.Error
     * THEN OAuthError is created with re-auth message
     */
    @Test
    fun `AuthError OAuthError for sign in required`() = runTest {
        // ACT
        val error = AuthError.OAuthError(
            provider = AuthMethod.GOOGLE,
            message = "Ré-authentification requise"
        )
        val result = AuthResult.error(error)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        val authError = result.error
        assertTrue(authError is AuthError.OAuthError)
        assertEquals(AuthMethod.GOOGLE, (authError as AuthError.OAuthError).provider)
        assertEquals("Ré-authentification requise", authError.message)
    }

    /**
     * Test OAuthError error creation for generic failure.
     *
     * GIVEN generic sign-in failure with status code
     * WHEN creating AuthResult.Error
     * THEN OAuthError is created with status code in message
     */
    @Test
    fun `AuthError OAuthError for generic sign in failure`() = runTest {
        // ARRANGE
        val testStatusCode = 10
        val testMessage = "Test error message"

        // ACT
        val error = AuthError.OAuthError(
            provider = AuthMethod.GOOGLE,
            message = "Erreur Google Sign-In: $testStatusCode - $testMessage"
        )
        val result = AuthResult.error(error)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        val authError = result.error
        assertTrue(authError is AuthError.OAuthError)
        assertEquals(AuthMethod.GOOGLE, (authError as AuthError.OAuthError).provider)
        assertTrue(authError.message?.contains("10") == true)
        assertTrue(authError.message?.contains(testMessage) == true)
    }

    /**
     * Test OAuthError for null account data.
     *
     * GIVEN null Google account in result
     * WHEN handling error
     * THEN OAuthError is created
     */
    @Test
    fun `AuthError OAuthError for null account data`() = runTest {
        // ACT
        val error = AuthError.OAuthError(
            provider = AuthMethod.GOOGLE,
            message = "Google account is null"
        )
        val result = AuthResult.error(error)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        val authError = result.error
        assertTrue(authError is AuthError.OAuthError)
    }

    /**
     * Test OAuthError for missing required fields.
     *
     * GIVEN account with null required field (email, id, token)
     * WHEN handling error
     * THEN OAuthError is created
     */
    @Test
    fun `AuthError OAuthError for missing email`() = runTest {
        // ACT
        val error = AuthError.OAuthError(
            provider = AuthMethod.GOOGLE,
            message = "Google account email is null"
        )
        val result = AuthResult.error(error)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        assertTrue(result.error is AuthError.OAuthError)
    }

    @Test
    fun `AuthError OAuthError for missing id token`() = runTest {
        // ACT
        val error = AuthError.OAuthError(
            provider = AuthMethod.GOOGLE,
            message = "Google ID token is null"
        )
        val result = AuthResult.error(error)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        assertTrue(result.error is AuthError.OAuthError)
    }

    // ==================== AuthToken Creation Tests ====================

    /**
     * Test AuthToken creation with correct properties.
     *
     * GIVEN ID token from Google
     * WHEN creating AuthToken
     * THEN token has correct type and expiration
     */
    @Test
    fun `AuthToken created with correct properties`() = runTest {
        // ARRANGE
        val testIdToken = "test-id-token"

        // ACT
        val token = AuthToken.createLongLived(
            value = testIdToken,
            type = TokenType.BEARER,
            expiresInDays = 30
        )

        // ASSERT
        assertEquals(testIdToken, token.value)
        assertEquals(TokenType.BEARER, token.type)

        // Verify expiration is set to approximately 30 days from now
        val expectedExpiration = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
        val timeDifference = kotlin.math.abs(expectedExpiration - token.expiresAt)
        assertTrue(timeDifference < 5000, "Token expiration should be ~30 days from now")
    }

    /**
     * Test AuthToken is not expired immediately.
     *
     * GIVEN newly created token
     * WHEN checking expiration
     * THEN token is not expired
     */
    @Test
    fun `AuthToken is not expired immediately after creation`() = runTest {
        // ARRANGE
        val testIdToken = "test-id-token"

        // ACT
        val token = AuthToken.createLongLived(
            value = testIdToken,
            type = TokenType.BEARER,
            expiresInDays = 30
        )

        // ASSERT
        assertTrue(token.expiresAt > System.currentTimeMillis(), "Token should not be expired")
    }

    // ==================== User Model Tests ====================

    /**
     * Test User model creation for Google auth.
     *
     * GIVEN Google user data
     * WHEN creating User
     * THEN user has GOOGLE auth method and is not a guest
     */
    @Test
    fun `User created with GOOGLE auth method`() = runTest {
        // ARRANGE
        val testId = "google-user-123"
        val testEmail = "test@gmail.com"

        // ACT
        val user = User.createAuthenticated(
            id = testId,
            email = testEmail,
            name = "Test User",
            authMethod = AuthMethod.GOOGLE
        )

        // ASSERT
        assertEquals(testId, user.id)
        assertEquals(testEmail, user.email)
        assertEquals(AuthMethod.GOOGLE, user.authMethod)
        assertFalse(user.isGuest, "Google authenticated user should not be a guest")
    }

    /**
     * Test User model is not guest when authenticated via Google.
     *
     * GIVEN Google authenticated user
     * WHEN checking auth status
     * THEN user is not guest (isGuest = false)
     */
    @Test
    fun `Google authenticated user is not guest`() = runTest {
        // ARRANGE
        val user = User.createAuthenticated(
            id = "google-user-123",
            email = "test@gmail.com",
            name = "Test User",
            authMethod = AuthMethod.GOOGLE
        )

        // ASSERT
        assertFalse(user.isGuest, "Google authenticated user should not be a guest")
        // Authenticated = !isGuest
        assertTrue(!user.isGuest, "User should be authenticated (not a guest)")
    }

    // ==================== AuthMethod Tests ====================

    /**
     * Test AuthMethod.GOOGLE is correctly used.
     *
     * GIVEN Google sign-in provider
     * WHEN creating auth objects
     * THEN GOOGLE auth method is used
     */
    @Test
    fun `AuthMethod GOOGLE is used for Google sign in`() {
        // ASSERT
        assertEquals("GOOGLE", AuthMethod.GOOGLE.name)
        assertNotNull(AuthMethod.GOOGLE)
    }
}
