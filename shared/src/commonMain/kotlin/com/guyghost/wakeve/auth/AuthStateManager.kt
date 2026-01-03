package com.guyghost.wakeve.auth

import com.guyghost.wakeve.currentTimeMillis
import com.guyghost.wakeve.models.OAuthProvider
import com.guyghost.wakeve.models.UserResponse
import com.guyghost.wakeve.security.SecureTokenStorage
import com.guyghost.wakeve.security.UserProfileData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tokenRefreshJob: Job? = null

    init {
        scope.launch {
            checkExistingAuth()
        }
    }

    /**
     * Check for existing authentication on startup.
     */
    private suspend fun checkExistingAuth() {
        val accessToken = secureStorage.getAccessToken()
        val userId = secureStorage.getUserId()

        if (accessToken != null && userId != null && !secureStorage.isTokenExpired()) {
            // User is logged in, fetch full profile
            fetchAndSetUserProfile(userId)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    /**
     * Fetch full user profile from server.
     */
    private suspend fun fetchAndSetUserProfile(userId: String) {
        _authState.value = AuthState.Loading

        try {
            val response = authService.getUserProfile(userId)
            if (response.isSuccess && response.valueOrNull() != null) {
                val userProfile = response.valueOrNull()!!

                // Store user profile data
                secureStorage.storeUserName(userProfile.name)
                secureStorage.storeUserEmail(userProfile.email)
                secureStorage.storeUserProvider(userProfile.provider.toString())

                // Set authenticated state
                val session = generateSessionId()
                secureStorage.storeSessionId(session)

                _authState.value = AuthState.Authenticated(
                    userId = userId,
                    user = userProfile,
                    sessionId = session
                )
            } else {
                _authState.value = AuthState.Error(
                    message = "Failed to fetch user profile",
                    code = ErrorCode.SERVER_ERROR
                )
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(
                message = e.message ?: "Unknown error",
                code = mapErrorCode(e)
            )
        }
    }

    /**
     * Login with Google OAuth.
     */
    suspend fun loginWithGoogle(authCode: String) {
        if (!enableOAuth) {
            _authState.value = AuthState.Error(
                message = "OAuth is not enabled",
                code = ErrorCode.UNKNOWN
            )
            return
        }

        _authState.value = AuthState.Loading

        try {
            val response = authService.loginWithGoogle(authCode)

            if (response.isSuccess && response.valueOrNull() != null) {
                val loginResponse = response.valueOrNull()!!

                // Store tokens
                secureStorage.storeAccessToken(loginResponse.accessToken)
                secureStorage.storeRefreshToken(loginResponse.refreshToken)
                secureStorage.storeTokenExpiry(loginResponse.expiresIn * 1000L + currentTimeMillis())

                // Store user profile
                secureStorage.storeUserId(loginResponse.user.id)
                secureStorage.storeUserName(loginResponse.user.name)
                secureStorage.storeUserEmail(loginResponse.user.email)
                secureStorage.storeUserProvider(loginResponse.user.provider.toString())

                // Set authenticated state
                val session = generateSessionId()
                secureStorage.storeSessionId(session)

                _authState.value = AuthState.Authenticated(
                    userId = loginResponse.user.id,
                    user = loginResponse.user,
                    sessionId = session
                )
            } else {
                _authState.value = AuthState.Error(
                    message = "OAuth login failed",
                    code = ErrorCode.INVALID_CREDENTIALS
                )
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(
                message = e.message ?: "Unknown error",
                code = mapErrorCode(e)
            )
        }
    }

    /**
     * Login with Apple Sign In.
     */
    suspend fun loginWithApple(authCode: String, userInfo: String? = null) {
        if (!enableOAuth) {
            _authState.value = AuthState.Error(
                message = "OAuth is not enabled",
                code = ErrorCode.UNKNOWN
            )
            return
        }

        _authState.value = AuthState.Loading

        try {
            val response = authService.loginWithApple(authCode, userInfo)

            if (response.isSuccess && response.valueOrNull() != null) {
                val loginResponse = response.valueOrNull()!!

                // Store tokens
                secureStorage.storeAccessToken(loginResponse.accessToken)
                secureStorage.storeRefreshToken(loginResponse.refreshToken)
                secureStorage.storeTokenExpiry(loginResponse.expiresIn * 1000L + currentTimeMillis())

                // Store user profile
                secureStorage.storeUserId(loginResponse.user.id)
                secureStorage.storeUserName(loginResponse.user.name)
                secureStorage.storeUserEmail(loginResponse.user.email)
                secureStorage.storeUserProvider(loginResponse.user.provider.toString())

                // Set authenticated state
                val session = generateSessionId()
                secureStorage.storeSessionId(session)

                _authState.value = AuthState.Authenticated(
                    userId = loginResponse.user.id,
                    user = loginResponse.user,
                    sessionId = session
                )
            } else {
                _authState.value = AuthState.Error(
                    message = "Apple login failed",
                    code = ErrorCode.INVALID_CREDENTIALS
                )
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(
                message = e.message ?: "Unknown error",
                code = mapErrorCode(e)
            )
        }
    }

    /**
     * Refresh access token.
     */
    suspend fun refreshToken() {
        _authState.value = AuthState.Loading

        try {
            val response = authService.refreshToken()

            if (response.isSuccess && response.valueOrNull() != null) {
                val refreshResponse = response.valueOrNull()!!

                // Update tokens
                secureStorage.storeAccessToken(refreshResponse.accessToken)
                secureStorage.storeRefreshToken(refreshResponse.refreshToken)
                secureStorage.storeTokenExpiry(refreshResponse.expiresIn * 1000L + currentTimeMillis())

                _authState.value = AuthState.Authenticated(
                    userId = getCurrentUserId(),
                    user = getCurrentUser(),
                    sessionId = getCurrentSessionId()
                )
            } else {
                _authState.value = AuthState.Error(
                    message = "Token refresh failed",
                    code = ErrorCode.TOKEN_EXPIRED
                )
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(
                message = e.message ?: "Unknown error",
                code = mapErrorCode(e)
            )
        }
    }

    /**
     * Logout user.
     */
    suspend fun logout() {
        _authState.value = AuthState.Loading

        try {
            authService.logout()
            secureStorage.clearAllTokens()
            _authState.value = AuthState.Unauthenticated
        } catch (e: Exception) {
            _authState.value = AuthState.Error(
                message = e.message ?: "Logout failed",
                code = mapErrorCode(e)
            )
        }
    }

    /**
     * Check if user is logged in.
     */
    suspend fun isLoggedIn(): Boolean {
        return authState.value is AuthState.Authenticated
    }

    /**
     * Get current user ID.
     */
    suspend fun getCurrentUserId(): String? {
        return secureStorage.getUserId()
    }

    /**
     * Get current user.
     */
    suspend fun getCurrentUser(): UserResponse? {
        val userId = secureStorage.getUserId()
        val email = secureStorage.getUserEmail()
        val name = secureStorage.getUserName()
        val providerStr = secureStorage.getUserProvider()

        if (userId != null && email != null && name != null && providerStr != null) {
            val provider = try {
                OAuthProvider.valueOf(providerStr)
            } catch (e: Exception) {
                OAuthProvider.GOOGLE
            }

            return UserResponse(
                id = userId,
                email = email,
                name = name,
                provider = provider,
                profilePictureUrl = secureStorage.getUserAvatarUrl()
            )
        }

        return null
    }

    /**
     * Get current session ID.
     */
    suspend fun getCurrentSessionId(): String? {
        return secureStorage.getSessionId()
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

    companion object {
        /**
         * Singleton instance for simple access.
         *
         * Note: For production use, consider dependency injection (Koin).
         *
         * @param secureStorage Optional secure storage (lazy initialized if not provided)
         * @param authService Optional auth service (lazy initialized if not provided)
         * @param enableOAuth Feature flag for OAuth
         */
        private var instance: AuthStateManager? = null

        /**
         * Get singleton instance of AuthStateManager.
         *
         * Note: For production use, consider dependency injection (Koin).
         *
         * @param secureStorage Optional secure storage (lazy initialized if not provided)
         * @param authService Optional auth service (lazy initialized if not provided)
         * @param enableOAuth Feature flag for OAuth
         */
        fun getInstance(
            secureStorage: SecureTokenStorage? = null,
            authService: ClientAuthenticationService? = null,
            enableOAuth: Boolean = false
        ): AuthStateManager {
            // Use memoization for thread-safe lazy initialization
            val currentValue = instance

            if (currentValue != null) {
                return currentValue
            }

            val newInstance = AuthStateManager(
                secureStorage = secureStorage ?: DummySecureTokenStorage(),
                authService = authService ?: DummyAuthenticationService(),
                enableOAuth = enableOAuth
            )

            instance = newInstance
            return newInstance
        }

        /**
         * Check if instance exists
         */
        fun hasInstance(): Boolean = instance != null

        /**
         * Clear the singleton instance (for testing)
         */
        fun clearInstance() {
            instance?.dispose()
            instance = null
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
 * Simple authentication error class.
 */
private class NotImplementedError(message: String) : Exception(message)

/**
 * Dummy implementation of SecureTokenStorage for shared module compilation.
 */
private class DummySecureTokenStorage : SecureTokenStorage {
    override suspend fun storeAccessToken(token: String) = Result.success(Unit)
    override suspend fun storeRefreshToken(token: String) = Result.success(Unit)
    override suspend fun storeUserId(userId: String) = Result.success(Unit)
    override suspend fun storeTokenExpiry(expiryTimestamp: Long) = Result.success(Unit)
    override suspend fun storeUserEmail(email: String) = Result.success(Unit)
    override suspend fun storeUserName(name: String) = Result.success(Unit)
    override suspend fun storeUserProvider(provider: String) = Result.success(Unit)
    override suspend fun storeUserAvatarUrl(avatarUrl: String?) = Result.success(Unit)
    override suspend fun storeUserProfile(profile: UserProfileData) = Result.success(Unit)
    override suspend fun getAccessToken(): String? = null
    override suspend fun getRefreshToken(): String? = null
    override suspend fun getUserId(): String? = null
    override suspend fun getTokenExpiry(): Long? = null
    override suspend fun getUserEmail(): String? = null
    override suspend fun getUserName(): String? = null
    override suspend fun getUserProvider(): String? = null
    override suspend fun getUserAvatarUrl(): String? = null
    override suspend fun getUserProfile(): UserProfileData? = null
    override suspend fun getSessionId(): String? = null
    override suspend fun storeSessionId(sessionId: String) = Result.success(Unit)
    override suspend fun clearAllTokens() = Result.success(Unit)
    override suspend fun isTokenExpired(): Boolean = true
    override suspend fun hasValidToken(): Boolean = false
}

/**
 * Dummy implementation of ClientAuthenticationService for shared module compilation.
 */
private class DummyAuthenticationService : ClientAuthenticationService {
    override suspend fun loginWithGoogle(authCode: String): Result<AuthResult<UserResponse>> =
        Result.failure(NotImplementedError("Use platform-specific implementation"))

    override suspend fun loginWithApple(authCode: String, userInfo: String?): Result<AuthResult<UserResponse>> =
        Result.failure(NotImplementedError("Use platform-specific implementation"))

    override suspend fun refreshToken(): Result<AuthResult<UserResponse>> =
        Result.failure(NotImplementedError("Use platform-specific implementation"))

    override suspend fun logout(): Result<Unit> = Result.success(Unit)

    override fun isLoggedIn(): Boolean = false
}

/**
 * Result wrapper for auth operations.
 */
sealed class AuthResult<out T> {
    data class Success<out T>(val value: T) : AuthResult<T>()
    data class Failure(val error: Exception) : AuthResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun valueOrNull(): T? = when (this) {
        is Success<T> -> value
        is Failure -> null
    }
}
