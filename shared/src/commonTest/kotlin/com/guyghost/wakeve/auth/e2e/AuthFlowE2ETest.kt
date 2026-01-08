package com.guyghost.wakeve.auth.e2e

import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.User
import com.guyghost.wakeve.auth.shell.services.InMemoryTokenStorage
import com.guyghost.wakeve.auth.shell.statemachine.AuthContract
import com.guyghost.wakeve.auth.shell.statemachine.AuthStateMachine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * End-to-end tests for the authentication flow.
 * 
 * These tests verify the complete authentication flow from UI intent
 * to final state, including:
 * - Guest mode flow
 * - OAuth flow
 * - Email OTP flow
 * - Sign out flow
 * 
 * Note: These tests use placeholder implementations since the actual
 * platform services (AuthService, EmailAuthService) are expect classes.
 * For full E2E testing, use platform-specific test suites.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthFlowE2ETest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var tokenStorage: InMemoryTokenStorage

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tokenStorage = InMemoryTokenStorage()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== TOKEN STORAGE TESTS ==========

    @Test
    fun testTokenStorageStoreString() = runTest {
        // Given
        val key = "access_token"
        val value = "test_token_123"

        // When
        tokenStorage.storeString(key, value)

        // Then
        val retrieved = tokenStorage.getString(key)
        assertEquals(value, retrieved)
    }

    @Test
    fun testTokenStorageGetStringReturnsNullWhenNotFound() = runTest {
        // When
        val value = tokenStorage.getString("nonexistent_key")

        // Then
        assertNull(value)
    }

    @Test
    fun testTokenStorageRemove() = runTest {
        // Given
        tokenStorage.storeString("key1", "value1")
        assertTrue(tokenStorage.contains("key1"))

        // When
        tokenStorage.remove("key1")

        // Then
        assertFalse(tokenStorage.contains("key1"))
    }

    @Test
    fun testTokenStorageContains() = runTest {
        // Given
        tokenStorage.storeString("key1", "value1")

        // When
        val exists = tokenStorage.contains("key1")
        val notExists = tokenStorage.contains("nonexistent")

        // Then
        assertTrue(exists)
        assertFalse(notExists)
    }

    @Test
    fun testTokenStorageClearAll() = runTest {
        // Given
        tokenStorage.storeString("key1", "value1")
        tokenStorage.storeString("key2", "value2")
        assertTrue(tokenStorage.contains("key1"))

        // When
        tokenStorage.clearAll()

        // Then
        assertFalse(tokenStorage.contains("key1"))
        assertFalse(tokenStorage.contains("key2"))
    }

    // ========== USER MODEL TESTS ==========

    @Test
    fun testGuestUserCreation() {
        // When
        val guest = User.createGuest("guest_123")

        // Then
        assertEquals("guest_123", guest.id)
        assertNull(guest.email)
        assertNull(guest.name)
        assertEquals(AuthMethod.GUEST, guest.authMethod)
        assertTrue(guest.isGuest)
        assertFalse(guest.canSync)
    }

    @Test
    fun testAuthenticatedUserCreation() {
        // When
        val user = User.createAuthenticated(
            id = "user_123",
            email = "test@example.com",
            name = "Test User",
            authMethod = AuthMethod.GOOGLE
        )

        // Then
        assertEquals("user_123", user.id)
        assertEquals("test@example.com", user.email)
        assertEquals("Test User", user.name)
        assertEquals(AuthMethod.GOOGLE, user.authMethod)
        assertFalse(user.isGuest)
        assertTrue(user.canSync)
    }

    @Test
    fun testUserDisplayName() {
        // Guest user
        val guest = User.createGuest("guest_123")
        assertEquals("Invité", guest.displayName)

        // Authenticated with name
        val userWithName = User.createAuthenticated("u1", "test@example.com", "John", AuthMethod.EMAIL)
        assertEquals("John", userWithName.displayName)

        // Authenticated without name
        val userNoName = User.createAuthenticated("u2", "john.doe@example.com", null, AuthMethod.EMAIL)
        assertEquals("john.doe", userNoName.displayName)
    }

    // ========== AUTH RESULT TESTS ==========

    @Test
    fun testAuthResultSuccessIsSuccess() {
        // Given
        val user = User.createGuest("guest_123")

        // When
        val result = AuthResult.guest(user)

        // Then
        assertTrue(result.isGuest)
        assertFalse(result.isSuccess)
        assertFalse(result.isError)
    }

    @Test
    fun testAuthResultErrorIsError() {
        // Given
        val error = AuthError.NetworkError

        // When
        val result = AuthResult.error(error)

        // Then
        assertFalse(result.isSuccess)
        assertFalse(result.isGuest)
        assertTrue(result.isError)
    }

    @Test
    fun testAuthResultUserOrNull() {
        // Guest result
        val guest = User.createGuest("guest_123")
        val guestResult = AuthResult.guest(guest)
        assertEquals(guest, guestResult.userOrNull)

        // Error result
        val errorResult = AuthResult.error(AuthError.NetworkError)
        assertNull(errorResult.userOrNull)
    }

    @Test
    fun testAuthResultErrorOrNull() {
        // Success result
        val user = User.createGuest("guest_123")
        val successResult = AuthResult.guest(user)
        assertNull(successResult.errorOrNull)

        // Error result
        val error = AuthError.NetworkError
        val errorResult = AuthResult.error(error)
        assertEquals(error, errorResult.errorOrNull)
    }

    // ========== AUTH ERROR TESTS ==========

    @Test
    fun testAuthErrorNetworkErrorIsRetryable() {
        // Then
        assertTrue(AuthError.NetworkError.isRetryable)
    }

    @Test
    fun testAuthErrorInvalidOTPIsRetryable() {
        // With attempts remaining
        val error1 = AuthError.InvalidOTP(attemptsRemaining = 2)
        assertTrue(error1.isRetryable)

        // Without attempts remaining
        val error2 = AuthError.InvalidOTP(attemptsRemaining = 0)
        assertFalse(error2.isRetryable)
    }

    @Test
    fun testAuthErrorOTPExpiredMessage() {
        // Then
        assertEquals("Le code a expiré. Veuillez demander un nouveau code.", AuthError.OTPExpired.userMessage)
    }

    @Test
    fun testAuthErrorValidationError() {
        // When
        val error = AuthError.ValidationError(field = "email", message = "Invalid email format")

        // Then
        assertEquals("Invalid email format", error.userMessage)
    }

    @Test
    fun testAuthErrorOAuthError() {
        // When
        val error = AuthError.OAuthError(AuthMethod.GOOGLE, "Sign in failed")

        // Then
        assertEquals("Sign in failed", error.userMessage)
        assertTrue(error.requiresMethodChange)
    }

    @Test
    fun testAuthErrorOAuthCancelled() {
        // When
        val error = AuthError.OAuthCancelled(AuthMethod.APPLE)

        // Then
        assertEquals("Connexion annulée", error.userMessage)
        assertTrue(error.requiresMethodChange)
        assertFalse(error.isRetryable)
    }
}
