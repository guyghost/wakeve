package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.AuthResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for the pure Google auth mapping logic used by both the legacy
 * GoogleSignIn flow and the Credential Manager flow (DAO #21).
 *
 * TDD: written before GoogleAuthResultFactory / GoogleCredentialErrorMapper.
 */
class GoogleCredentialMappingTest {

    private val now = 1_700_000_000_000L

    // ========================================================================
    // GoogleAuthResultFactory.createFromProfile
    // ========================================================================

    @Test
    fun createFromProfile_withCompleteProfile_returnsSuccess() {
        val result = GoogleAuthResultFactory.createFromProfile(
            id = "google-id-123",
            email = "user@example.com",
            displayName = "Alice Martin",
            idToken = "id-token-abc",
            currentTimeMillis = now
        )

        val success = assertIs<AuthResult.Success>(result)
        assertEquals("google-id-123", success.user.id)
        assertEquals("user@example.com", success.user.email)
        assertEquals("Alice Martin", success.user.name)
        assertEquals(AuthMethod.GOOGLE, success.user.authMethod)
        assertFalse(success.user.isGuest)
        assertEquals(now, success.user.createdAt)
    }

    @Test
    fun createFromProfile_tokenCarriesGoogleIdToken() {
        val result = GoogleAuthResultFactory.createFromProfile(
            id = "google-id-123",
            email = "user@example.com",
            displayName = null,
            idToken = "id-token-abc",
            currentTimeMillis = now
        )

        val success = assertIs<AuthResult.Success>(result)
        assertEquals("id-token-abc", success.token.value)
        assertTrue(success.token.isValid(now))
    }

    @Test
    fun createFromProfile_withNullId_returnsOAuthError() {
        val result = GoogleAuthResultFactory.createFromProfile(
            id = null,
            email = "user@example.com",
            displayName = "Alice",
            idToken = "id-token-abc",
            currentTimeMillis = now
        )

        val error = assertIs<AuthResult.Error>(result)
        assertIs<AuthError.OAuthError>(error.error)
    }

    @Test
    fun createFromProfile_withNullIdToken_returnsOAuthError() {
        val result = GoogleAuthResultFactory.createFromProfile(
            id = "google-id-123",
            email = "user@example.com",
            displayName = "Alice",
            idToken = null,
            currentTimeMillis = now
        )

        val error = assertIs<AuthResult.Error>(result)
        assertIs<AuthError.OAuthError>(error.error)
    }

    @Test
    fun createFromProfile_withNullEmail_returnsOAuthError() {
        val result = GoogleAuthResultFactory.createFromProfile(
            id = "google-id-123",
            email = null,
            displayName = "Alice",
            idToken = "id-token-abc",
            currentTimeMillis = now
        )

        val error = assertIs<AuthResult.Error>(result)
        assertIs<AuthError.OAuthError>(error.error)
    }

    // ========================================================================
    // GoogleCredentialErrorMapper.map
    // ========================================================================

    @Test
    fun map_userCancelled_returnsOAuthCancelled() {
        val result = GoogleCredentialErrorMapper.map(
            type = GoogleCredentialErrorMapper.TYPE_USER_CANCELED,
            message = null
        )

        val error = assertIs<AuthResult.Error>(result)
        val cancelled = assertIs<AuthError.OAuthCancelled>(error.error)
        assertEquals(AuthMethod.GOOGLE, cancelled.provider)
    }

    @Test
    fun map_noCredential_returnsOAuthError() {
        val result = GoogleCredentialErrorMapper.map(
            type = GoogleCredentialErrorMapper.TYPE_NO_CREDENTIAL,
            message = "No eligible accounts"
        )

        val error = assertIs<AuthResult.Error>(result)
        val oauthError = assertIs<AuthError.OAuthError>(error.error)
        assertEquals(AuthMethod.GOOGLE, oauthError.provider)
        assertTrue(oauthError.message.orEmpty().contains("aucun compte", ignoreCase = true))
    }

    @Test
    fun map_unknownType_returnsOAuthErrorWithMessage() {
        val result = GoogleCredentialErrorMapper.map(
            type = "TYPE_UNEXPECTED",
            message = "something broke"
        )

        val error = assertIs<AuthResult.Error>(result)
        val oauthError = assertIs<AuthError.OAuthError>(error.error)
        assertTrue(oauthError.message.orEmpty().contains("TYPE_UNEXPECTED"))
    }

    @Test
    fun map_nullType_returnsGenericOAuthError() {
        val result = GoogleCredentialErrorMapper.map(type = null, message = null)

        val error = assertIs<AuthResult.Error>(result)
        assertIs<AuthError.OAuthError>(error.error)
    }
}
