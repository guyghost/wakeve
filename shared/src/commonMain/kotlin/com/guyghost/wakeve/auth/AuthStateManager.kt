package com.guyghost.wakeve.auth

import com.guyghost.wakeve.currentTimeMillis
import com.guyghost.wakeve.models.OAuthProvider
import com.guyghost.wakeve.models.UserResponse
import com.guyghost.wakeve.security.SecureTokenStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Authentication state sealed class.
 *
 * Represents all possible authentication states in the application.
 */
sealed class AuthState {
    /**
     * Initial state while checking for existing authentication.
     */
    object Loading : AuthState()

    /**
     * User is not authenticated.
     */
    object Unauthenticated : AuthState()

    /**
     * User is successfully authenticated.
     *
     * @property userId The unique user identifier
     * @property user The user profile information
     * @property sessionId The current session identifier
     */
    data class Authenticated(
        val userId: String,
        val user: UserResponse,
        val sessionId: String
    ) : AuthState()

    /**
     * Authentication error occurred.
     *
     * @property message User-friendly error message
     * @property code Error code for debugging
     */
    data class Error(
        val message: String,
        val code: ErrorCode = ErrorCode.UNKNOWN
    ) : AuthState()
}

/**
 * Error codes for authentication failures.
 */
enum class ErrorCode {
    UNKNOWN,
    NETWORK_ERROR,
    INVALID_CREDENTIALS,
    TOKEN_EXPIRED,
    SERVER_ERROR,
    USER_CANCELLED
}

/**
 * Central authentication state manager.
 *
 * This class manages the authentication state across the entire application using
 * StateFlow for reactive updates. It handles:
 * - Initial authentication check on app startup
 * - OAuth login flow coordination
 * - Token refresh and expiry handling
 * - Logout and session cleanup
 * - Feature flag integration for progressive rollout
 *
 * Usage:
 * ```kotlin
 * val authStateManager = AuthStateManager(secureStorage, authService)
 * val authState by authStateManager.authState.collectAsState()
 *
 * when (authState) {
 *     is AuthState.Loading -> LoadingScreen()
 *     is AuthState.Unauthenticated -> LoginScreen()
 *     is AuthState.Authenticated -> MainApp()
 *     is AuthState.Error -> ErrorScreen()
 * }
 * ```
 */
class AuthStateManager(
    private val secureStorage: SecureTokenStorage,
    private val authService: ClientAuthenticationService,
    private val enableOAuth: Boolean = false // Feature flag
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Token refresh job
    private var tokenRefreshJob: Job? = null

    /**
     * Initialize authentication state.
     *
     * This should be called when the app starts. It checks for existing
     * authentication and validates the stored tokens.
     */
    suspend fun initialize() {
        _authState.value = AuthState.Loading

        // If OAuth is disabled, create a demo user and skip auth
        if (!enableOAuth) {
            // Create a demo user for development/testing
            val demoUser = UserResponse(
                id = "demo-user-${(1000..9999).random()}",
                email = "demo@wakeve.local",
                name = "Demo User",
                provider = "DEMO",
                avatarUrl = null,
                role = "USER",
                createdAt = getCurrentTimestamp()
            )
            
            _authState.value = AuthState.Authenticated(
                userId = demoUser.id,
                user = demoUser,
                sessionId = generateSessionId()
            )
            return
        }

        try {
            // Check if user is already logged in
            if (authService.isLoggedIn()) {
                // Check if token is still valid
                if (secureStorage.hasValidToken()) {
                    val userId = secureStorage.getUserId()
                    val accessToken = secureStorage.getAccessToken()

                    if (userId != null && accessToken != null) {
                        // Try to refresh token proactively if it's close to expiry
                        if (shouldRefreshToken()) {
                            refreshTokenIfNeeded()
                        }

                        // TODO: Fetch full user profile from server
                        // For now, create a minimal user response
                        val user = UserResponse(
                            id = userId,
                            email = "user@example.com", // TODO: Get from server
                            name = "User", // TODO: Get from server
                            provider = "GOOGLE", // TODO: Get from storage
                            avatarUrl = null,
                            createdAt = getCurrentTimestamp()
                        )

                        _authState.value = AuthState.Authenticated(
                            userId = userId,
                            user = user,
                            sessionId = generateSessionId()
                        )

                        // Start token refresh monitoring
                        startTokenRefreshMonitoring()
                        return
                    }
                }

                // Token invalid or expired - logout
                logout()
            }

            _authState.value = AuthState.Unauthenticated

        } catch (e: Exception) {
            _authState.value = AuthState.Error(
                message = "Failed to initialize authentication: ${e.message}",
                code = ErrorCode.UNKNOWN
            )
        }
    }

    /**
     * Login with OAuth authorization code.
     *
     * @param authCode The authorization code from OAuth provider
     * @param provider The OAuth provider (Google, Apple)
     * @return Result indicating success or failure
     */
    suspend fun login(authCode: String, provider: OAuthProvider): Result<Unit> = runCatching {
        _authState.value = AuthState.Loading

        // Perform login request
        val result = when (provider) {
            OAuthProvider.GOOGLE -> authService.loginWithGoogle(authCode)
            OAuthProvider.APPLE -> authService.loginWithApple(authCode, null)
        }

        result.fold(
            onSuccess = { response ->
                // Login successful
                _authState.value = AuthState.Authenticated(
                    userId = response.user.id,
                    user = response.user,
                    sessionId = generateSessionId()
                )

                // Start token refresh monitoring
                startTokenRefreshMonitoring()
            },
            onFailure = { error ->
                _authState.value = AuthState.Error(
                    message = "Login failed: ${error.message}",
                    code = mapErrorCode(error)
                )
                throw error
            }
        )
    }

    /**
     * Logout the current user.
     *
     * Clears all stored tokens and resets state to Unauthenticated.
     */
    suspend fun logout(): Result<Unit> = runCatching {
        // Stop token refresh monitoring
        tokenRefreshJob?.cancel()
        tokenRefreshJob = null

        // Clear tokens
        authService.logout()

        // Update state
        _authState.value = AuthState.Unauthenticated
    }

    /**
     * Refresh token if needed.
     *
     * This proactively refreshes the access token before it expires.
     */
    suspend fun refreshTokenIfNeeded(): Result<Unit> = runCatching {
        if (!shouldRefreshToken()) {
            return@runCatching
        }

        val result = authService.refreshToken()

        result.fold(
            onSuccess = { response ->
                // Token refreshed successfully
                // State remains Authenticated, just with new token
                val currentState = _authState.value
                if (currentState is AuthState.Authenticated) {
                    // Update user info if needed
                    _authState.value = currentState.copy(user = response.user)
                }
            },
            onFailure = { error ->
                // Refresh failed - logout user
                _authState.value = AuthState.Error(
                    message = "Session expired. Please login again.",
                    code = ErrorCode.TOKEN_EXPIRED
                )
                logout()
                throw error
            }
        )
    }

    /**
     * Handle token expiry (typically called from API error handlers).
     *
     * Attempts to refresh the token. If refresh fails, logs out the user.
     */
    suspend fun handleTokenExpired() {
        try {
            refreshTokenIfNeeded()
        } catch (e: Exception) {
            logout()
        }
    }

    /**
     * Get the current user ID (if authenticated).
     */
    fun getCurrentUserId(): String? {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> state.userId
            else -> null
        }
    }

    /**
     * Get the current access token (if authenticated).
     */
    suspend fun getCurrentAccessToken(): String? {
        return if (_authState.value is AuthState.Authenticated) {
            secureStorage.getAccessToken()
        } else {
            null
        }
    }

    /**
     * Check if token should be refreshed.
     *
     * Returns true if token expires in less than 10 minutes.
     */
    private suspend fun shouldRefreshToken(): Boolean {
        val expiryTimestamp = secureStorage.getTokenExpiry() ?: return false
        val currentTime = currentTimeMillis()
        val timeUntilExpiry = expiryTimestamp - currentTime

        // Refresh if expires in less than 10 minutes
        return timeUntilExpiry < 10 * 60 * 1000
    }

    /**
     * Start background token refresh monitoring.
     *
     * Checks token expiry every 5 minutes and refreshes proactively.
     */
    private fun startTokenRefreshMonitoring() {
        tokenRefreshJob?.cancel()
        tokenRefreshJob = scope.launch {
            while (isActive) {
                delay(5 * 60 * 1000) // Check every 5 minutes

                if (_authState.value is AuthState.Authenticated) {
                    try {
                        refreshTokenIfNeeded()
                    } catch (e: Exception) {
                        // Log error but don't crash
                        println("Token refresh failed: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Generate a unique session ID.
     */
    private fun generateSessionId(): String {
        return "session-${currentTimeMillis()}-${(0..999).random()}"
    }

    /**
     * Map exception to error code.
     */
    private fun mapErrorCode(error: Throwable): ErrorCode {
        return when {
            error.message?.contains("network", ignoreCase = true) == true -> ErrorCode.NETWORK_ERROR
            error.message?.contains("invalid", ignoreCase = true) == true -> ErrorCode.INVALID_CREDENTIALS
            error.message?.contains("expired", ignoreCase = true) == true -> ErrorCode.TOKEN_EXPIRED
            error.message?.contains("server", ignoreCase = true) == true -> ErrorCode.SERVER_ERROR
            error.message?.contains("cancel", ignoreCase = true) == true -> ErrorCode.USER_CANCELLED
            else -> ErrorCode.UNKNOWN
        }
    }

    /**
     * Get current timestamp in ISO 8601 format.
     */
    private fun getCurrentTimestamp(): String {
        // Simple ISO 8601 timestamp
        return "${currentTimeMillis()}"
    }

    /**
     * Clean up resources.
     */
    fun dispose() {
        tokenRefreshJob?.cancel()
        scope.cancel()
    }
}
