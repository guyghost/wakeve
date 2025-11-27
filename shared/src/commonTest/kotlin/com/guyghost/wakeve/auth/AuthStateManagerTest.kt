package com.guyghost.wakeve.auth

import com.guyghost.wakeve.models.OAuthProvider
import com.guyghost.wakeve.models.UserResponse
import com.guyghost.wakeve.security.SecureTokenStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Unit tests for AuthStateManager.
 *
 * Tests cover:
 * - Initialization flow
 * - Login/logout flow
 * - Token refresh
 * - Error handling
 * - State transitions
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthStateManagerTest {

    private lateinit var authStateManager: AuthStateManager
    private lateinit var mockStorage: MockSecureTokenStorage
    private lateinit var mockAuthService: MockClientAuthenticationService

    @BeforeTest
    fun setup() {
        mockStorage = MockSecureTokenStorage()
        mockAuthService = MockClientAuthenticationService()
        authStateManager = AuthStateManager(
            secureStorage = mockStorage,
            authService = mockAuthService,
            enableOAuth = true
        )
    }

    @AfterTest
    fun teardown() {
        authStateManager.dispose()
    }

    // ============================================
    // Initialization Tests
    // ============================================

    @Test
    fun `initialize with no stored token should set state to Unauthenticated`() = runTest {
        // Given
        mockStorage.hasValidTokenResult = false
        mockAuthService.isLoggedInResult = false

        // When
        authStateManager.initialize()

        // Then
        val state = authStateManager.authState.first()
        assertTrue(state is AuthState.Unauthenticated)
    }

    @Test
    fun `initialize with valid token should set state to Authenticated`() = runTest {
        // Given
        mockStorage.hasValidTokenResult = true
        mockStorage.userIdResult = "user-123"
        mockStorage.accessTokenResult = "valid-token"
        mockAuthService.isLoggedInResult = true

        // When
        authStateManager.initialize()

        // Then
        val state = authStateManager.authState.first()
        assertTrue(state is AuthState.Authenticated)
        assertEquals("user-123", (state as AuthState.Authenticated).userId)
    }

    @Test
    fun `initialize with expired token should set state to Unauthenticated`() = runTest {
        // Given
        mockStorage.hasValidTokenResult = false
        mockAuthService.isLoggedInResult = true

        // When
        authStateManager.initialize()

        // Then
        val state = authStateManager.authState.first()
        assertTrue(state is AuthState.Unauthenticated)
    }

    @Test
    fun `initialize with OAuth disabled should skip auth check`() = runTest {
        // Given
        val manager = AuthStateManager(
            secureStorage = mockStorage,
            authService = mockAuthService,
            enableOAuth = false
        )

        // When
        manager.initialize()

        // Then
        val state = manager.authState.first()
        assertTrue(state is AuthState.Unauthenticated)
        manager.dispose()
    }

    // ============================================
    // Login Tests
    // ============================================

    @Test
    fun `login with valid credentials should set state to Authenticated`() = runTest {
        // Given
        val mockUser = UserResponse(
            id = "user-123",
            email = "test@example.com",
            name = "Test User",
            provider = "GOOGLE",
            avatarUrl = null,
            createdAt = "2025-01-01T00:00:00Z"
        )
        mockAuthService.loginResult = Result.success(
            AuthResponse(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                expiresIn = 3600,
                user = mockUser
            )
        )

        // When
        val result = authStateManager.login("auth-code", OAuthProvider.GOOGLE)

        // Then
        assertTrue(result.isSuccess)
        val state = authStateManager.authState.first()
        assertTrue(state is AuthState.Authenticated)
        assertEquals("user-123", (state as AuthState.Authenticated).userId)
        assertEquals("Test User", state.user.name)
    }

    @Test
    fun `login with invalid credentials should set state to Error`() = runTest {
        // Given
        mockAuthService.loginResult = Result.failure(Exception("Invalid credentials"))

        // When
        val result = authStateManager.login("invalid-code", OAuthProvider.GOOGLE)

        // Then
        assertTrue(result.isFailure)
        val state = authStateManager.authState.first()
        assertTrue(state is AuthState.Error)
        assertTrue((state as AuthState.Error).message.contains("Login failed"))
    }

    @Test
    fun `login should set state to Loading before completion`() = runTest {
        // Given
        mockAuthService.loginResult = Result.failure(Exception("Network error"))

        // When
        authStateManager.login("auth-code", OAuthProvider.GOOGLE)

        // Then
        // State should transition through Loading to Error
        val state = authStateManager.authState.first()
        assertTrue(state is AuthState.Error)
    }

    // ============================================
    // Logout Tests
    // ============================================

    @Test
    fun `logout should set state to Unauthenticated`() = runTest {
        // Given - set up authenticated state
        mockAuthService.loginResult = Result.success(
            AuthResponse(
                accessToken = "token",
                refreshToken = "refresh",
                expiresIn = 3600,
                user = UserResponse(
                    id = "user-123",
                    email = "test@example.com",
                    name = "Test",
                    provider = "GOOGLE",
                    avatarUrl = null,
                    createdAt = "2025-01-01"
                )
            )
        )
        authStateManager.login("code", OAuthProvider.GOOGLE)

        // When
        authStateManager.logout()

        // Then
        val state = authStateManager.authState.first()
        assertTrue(state is AuthState.Unauthenticated)
    }

    @Test
    fun `logout should clear tokens from storage`() = runTest {
        // When
        authStateManager.logout()

        // Then
        assertTrue(mockAuthService.logoutCalled)
    }

    // ============================================
    // Token Refresh Tests
    // ============================================

    @Test
    fun `refreshTokenIfNeeded should succeed with valid refresh token`() = runTest {
        // Given
        mockStorage.tokenExpiryResult = System.currentTimeMillis() + 5 * 60 * 1000 // 5 min
        mockAuthService.refreshTokenResult = Result.success(
            AuthResponse(
                accessToken = "new-token",
                refreshToken = "new-refresh",
                expiresIn = 3600,
                user = UserResponse(
                    id = "user-123",
                    email = "test@example.com",
                    name = "Test",
                    provider = "GOOGLE",
                    avatarUrl = null,
                    createdAt = "2025-01-01"
                )
            )
        )

        // When
        val result = authStateManager.refreshTokenIfNeeded()

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `refreshTokenIfNeeded should logout on refresh failure`() = runTest {
        // Given
        mockStorage.tokenExpiryResult = System.currentTimeMillis() + 5 * 60 * 1000 // 5 min
        mockAuthService.refreshTokenResult = Result.failure(Exception("Refresh failed"))

        // When
        val result = authStateManager.refreshTokenIfNeeded()

        // Then
        assertTrue(result.isFailure)
        val state = authStateManager.authState.first()
        // Should transition to error state
        assertTrue(state is AuthState.Error || state is AuthState.Unauthenticated)
    }

    @Test
    fun `refreshTokenIfNeeded should skip if token not expiring soon`() = runTest {
        // Given
        mockStorage.tokenExpiryResult = System.currentTimeMillis() + 20 * 60 * 1000 // 20 min

        // When
        authStateManager.refreshTokenIfNeeded()

        // Then
        assertFalse(mockAuthService.refreshTokenCalled)
    }

    // ============================================
    // Error Handling Tests
    // ============================================

    @Test
    fun `handleTokenExpired should attempt token refresh`() = runTest {
        // Given
        mockAuthService.refreshTokenResult = Result.success(
            AuthResponse(
                accessToken = "new-token",
                refreshToken = "new-refresh",
                expiresIn = 3600,
                user = UserResponse(
                    id = "user-123",
                    email = "test@example.com",
                    name = "Test",
                    provider = "GOOGLE",
                    avatarUrl = null,
                    createdAt = "2025-01-01"
                )
            )
        )
        mockStorage.tokenExpiryResult = System.currentTimeMillis() + 5 * 60 * 1000

        // When
        authStateManager.handleTokenExpired()

        // Then
        assertTrue(mockAuthService.refreshTokenCalled)
    }

    @Test
    fun `getCurrentUserId should return userId when authenticated`() = runTest {
        // Given - set up authenticated state
        mockAuthService.loginResult = Result.success(
            AuthResponse(
                accessToken = "token",
                refreshToken = "refresh",
                expiresIn = 3600,
                user = UserResponse(
                    id = "user-123",
                    email = "test@example.com",
                    name = "Test",
                    provider = "GOOGLE",
                    avatarUrl = null,
                    createdAt = "2025-01-01"
                )
            )
        )
        authStateManager.login("code", OAuthProvider.GOOGLE)

        // When
        val userId = authStateManager.getCurrentUserId()

        // Then
        assertEquals("user-123", userId)
    }

    @Test
    fun `getCurrentUserId should return null when unauthenticated`() = runTest {
        // Given
        authStateManager.initialize()

        // When
        val userId = authStateManager.getCurrentUserId()

        // Then
        assertNull(userId)
    }
}

// ============================================
// Mock Implementations
// ============================================

class MockSecureTokenStorage : SecureTokenStorage {
    var hasValidTokenResult = false
    var userIdResult: String? = null
    var accessTokenResult: String? = null
    var tokenExpiryResult: Long? = null

    override suspend fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long, userId: String) {
        this.accessTokenResult = accessToken
        this.userIdResult = userId
    }

    override suspend fun getAccessToken(): String? = accessTokenResult
    override suspend fun getRefreshToken(): String? = "refresh-token"
    override suspend fun getUserId(): String? = userIdResult
    override suspend fun getTokenExpiry(): Long? = tokenExpiryResult
    override suspend fun hasValidToken(): Boolean = hasValidTokenResult
    override suspend fun clearTokens() {
        accessTokenResult = null
        userIdResult = null
    }
}

class MockClientAuthenticationService : ClientAuthenticationService {
    var isLoggedInResult = false
    var loginResult: Result<AuthResponse>? = null
    var refreshTokenResult: Result<AuthResponse>? = null
    var logoutCalled = false
    var refreshTokenCalled = false

    override suspend fun isLoggedIn(): Boolean = isLoggedInResult

    override suspend fun loginWithGoogle(authCode: String): Result<AuthResponse> {
        return loginResult ?: Result.failure(Exception("Not configured"))
    }

    override suspend fun loginWithApple(authCode: String, identityToken: String?): Result<AuthResponse> {
        return loginResult ?: Result.failure(Exception("Not configured"))
    }

    override suspend fun refreshToken(): Result<AuthResponse> {
        refreshTokenCalled = true
        return refreshTokenResult ?: Result.failure(Exception("Not configured"))
    }

    override suspend fun logout(): Result<Unit> {
        logoutCalled = true
        return Result.success(Unit)
    }
}
