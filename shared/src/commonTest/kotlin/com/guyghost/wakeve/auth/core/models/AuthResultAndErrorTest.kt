package com.guyghost.wakeve.auth.core.models

import com.guyghost.wakeve.auth.core.models.AuthError.InvalidOTP
import com.guyghost.wakeve.auth.core.models.AuthError.NetworkError
import com.guyghost.wakeve.auth.core.models.AuthError.OAuthCancelled
import com.guyghost.wakeve.auth.core.models.AuthError.OAuthError
import com.guyghost.wakeve.auth.core.models.AuthError.OTPExpired
import com.guyghost.wakeve.auth.core.models.AuthError.ValidationError
import com.guyghost.wakeve.auth.core.models.AuthMethod.APPLE
import com.guyghost.wakeve.auth.core.models.AuthMethod.EMAIL
import com.guyghost.wakeve.auth.core.models.AuthMethod.GOOGLE
import com.guyghost.wakeve.auth.core.models.AuthResult.Error
import com.guyghost.wakeve.auth.core.models.AuthResult.Guest
import com.guyghost.wakeve.auth.core.models.AuthResult.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for AuthResult sealed class.
 */
class AuthResultTest {

    @Test
    fun `Success isSuccess returns true`() {
        // Given
        val user = User.createGuest("guest_123")
        val token = AuthToken.createLongLived("test_token")
        val result = Success(user, token)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.isGuest)
        assertFalse(result.isError)
    }

    @Test
    fun `Guest isGuest returns true`() {
        // Given
        val guest = User.createGuest("guest_123")
        val result = Guest(guest)
        
        // Then
        assertFalse(result.isSuccess)
        assertTrue(result.isGuest)
        assertFalse(result.isError)
    }

    @Test
    fun `Error isError returns true`() {
        // Given
        val result = Error(NetworkError)
        
        // Then
        assertFalse(result.isSuccess)
        assertFalse(result.isGuest)
        assertTrue(result.isError)
    }

    @Test
    fun `userOrNull returns user for Success`() {
        // Given
        val user = User.createAuthenticated("user_123", "test@example.com", "Test", EMAIL)
        val token = AuthToken.createLongLived("test_token")
        val result = Success(user, token)
        
        // When
        val retrievedUser = result.userOrNull
        
        // Then
        assertEquals(user, retrievedUser)
    }

    @Test
    fun `userOrNull returns guestUser for Guest`() {
        // Given
        val guest = User.createGuest("guest_123")
        val result = Guest(guest)
        
        // When
        val retrievedUser = result.userOrNull
        
        // Then
        assertEquals(guest, retrievedUser)
    }

    @Test
    fun `userOrNull returns null for Error`() {
        // Given
        val result = Error(NetworkError)
        
        // When
        val retrievedUser = result.userOrNull
        
        // Then
        assertNull(retrievedUser)
    }

    @Test
    fun `errorOrNull returns null for Success`() {
        // Given
        val user = User.createGuest("guest_123")
        val token = AuthToken.createLongLived("test_token")
        val result = Success(user, token)
        
        // When
        val error = result.errorOrNull
        
        // Then
        assertNull(error)
    }

    @Test
    fun `errorOrNull returns error for Error`() {
        // Given
        val result = Error(NetworkError)
        
        // When
        val error = result.errorOrNull
        
        // Then
        assertEquals(NetworkError, error)
    }

    @Test
    fun `companion factory methods create correct types`() {
        // Given
        val user = User.createGuest("guest_123")
        val token = AuthToken.createLongLived("test_token")
        
        // When
        val successResult = AuthResult.success(user, token)
        val guestResult = AuthResult.guest(user)
        val errorResult = AuthResult.error(NetworkError)
        val networkErrorResult = AuthResult.networkError()
        val invalidCredsResult = AuthResult.invalidCredentials()
        val invalidOTPResult = AuthResult.invalidOTP(2)
        
        // Then
        assertTrue(successResult is Success)
        assertTrue(guestResult is Guest)
        assertTrue(errorResult is Error)
        assertTrue(networkErrorResult is Error)
        assertEquals(NetworkError, (networkErrorResult as Error).error)
        assertTrue(invalidCredsResult is Error)
        assertTrue((invalidCredsResult as Error).error is AuthError.InvalidCredentials)
        assertTrue(invalidOTPResult is Error)
        assertEquals(2, ((invalidOTPResult as Error).error as InvalidOTP).attemptsRemaining)
    }
}

/**
 * Tests for AuthError sealed class.
 */
class AuthErrorTest {

    @Test
    fun `NetworkError has correct userMessage`() {
        // Given
        val error = NetworkError
        
        // Then
        assertEquals("Problème de connexion. Vérifiez votre connexion internet.", error.userMessage)
    }

    @Test
    fun `InvalidOTP with attemptsRemaining shows retry message`() {
        // Given
        val error = InvalidOTP(attemptsRemaining = 2)
        
        // Then
        assertEquals("Code invalide. 2 tentatives restantes.", error.userMessage)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `InvalidOTP with zero attempts shows lockout message`() {
        // Given
        val error = InvalidOTP(attemptsRemaining = 0)
        
        // Then
        assertEquals("Code invalide. Veuillez demander un nouveau code.", error.userMessage)
        assertFalse(error.isRetryable)
    }

    @Test
    fun `OTPExpired has correct userMessage`() {
        // Given
        val error = OTPExpired
        
        // Then
        assertEquals("Le code a expiré. Veuillez demander un nouveau code.", error.userMessage)
    }

    @Test
    fun `OAuthError has provider name in message`() {
        // Given
        val error = OAuthError(APPLE, "Sign in failed")
        
        // Then
        assertEquals("Sign in failed", error.userMessage)
    }

    @Test
    fun `OAuthCancelled requires method change`() {
        // Given
        val error = OAuthCancelled(EMAIL)
        
        // Then
        assertTrue(error.requiresMethodChange)
        assertEquals("Connexion annulée", error.userMessage)
    }

    @Test
    fun `ValidationError returns field message`() {
        // Given
        val error = ValidationError("email", "Email invalide")
        
        // Then
        assertEquals("Email invalide", error.userMessage)
    }

     @Test
    fun `OAuthError requires method change`() {
        // Given
        val error = OAuthError(AuthMethod.GOOGLE, "Error")
        
        // Then
        assertTrue(error.requiresMethodChange)
    }

    @Test
    fun `isRetryable is correct for different error types`() {
        // Then
        assertTrue(NetworkError.isRetryable)
        assertTrue(AuthError.InvalidCredentials.isRetryable)
        assertTrue(InvalidOTP(attemptsRemaining = 1).isRetryable)
        assertFalse(InvalidOTP(attemptsRemaining = 0).isRetryable)
        assertFalse(OAuthCancelled(EMAIL).isRetryable)
        assertTrue(OAuthError(AuthMethod.GOOGLE, "Error").isRetryable)
    }
}
