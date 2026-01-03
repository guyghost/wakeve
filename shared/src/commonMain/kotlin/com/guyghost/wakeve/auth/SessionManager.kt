package com.guyghost.wakeve.auth

import com.guyghost.wakeve.SessionRepository
import com.guyghost.wakeve.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Session management with automatic token rotation.
 *
 * This manager handles:
 * - Token lifecycle management
 * - Automatic token refresh
 * - Session validation and cleanup
 * - Multi-device session coordination
 *
 * Usage:
 * ```kotlin
 * val sessionManager = SessionManager(sessionRepository, authService)
 * sessionManager.startSession(userId, deviceId, deviceName, jwtToken, refreshToken)
 * sessionManager.startTokenRotation() // Automatic refresh
 * ```
 */
class SessionManager(
    private val sessionRepository: SessionRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private var tokenRotationJob: Job? = null

    /**
     * Start a new session.
     *
     * Creates a session record and starts token rotation monitoring.
     *
     * @param userId The user ID
     * @param deviceId Unique device identifier
     * @param deviceName Human-readable device name
     * @param jwtToken The JWT access token
     * @param refreshToken The refresh token
     * @param ipAddress Optional IP address
     * @param userAgent Optional user agent
     * @param tokenExpiryMs Token expiry time in milliseconds from epoch
     * @return Result with session ID on success
     */
    suspend fun startSession(
        userId: String,
        deviceId: String,
        deviceName: String,
        jwtToken: String,
        refreshToken: String,
        ipAddress: String? = null,
        userAgent: String? = null,
        tokenExpiryMs: Long
    ): Result<String> {
        // Stop any existing rotation
        stopTokenRotation()

        // Calculate expiry timestamp
        val expiresAt = tokenExpiryMs.toString()

        // Create session
        return sessionRepository.createSession(
            userId = userId,
            deviceId = deviceId,
            deviceName = deviceName,
            jwtToken = jwtToken,
            refreshToken = refreshToken,
            ipAddress = ipAddress,
            userAgent = userAgent,
            expiresAt = expiresAt
        ).onSuccess { sessionId ->
            _currentSessionId.value = sessionId

            // Register device fingerprint
            val fingerprintHash = generateDeviceFingerprint(deviceId, deviceName, userAgent)
            sessionRepository.registerDevice(
                userId = userId,
                deviceId = deviceId,
                deviceName = deviceName,
                deviceType = getDeviceType(),
                fingerprintHash = fingerprintHash,
                trusted = false
            )

            // Start automatic token rotation
            startTokenRotation(sessionId, tokenExpiryMs)
        }
    }

    /**
     * Update session with new tokens after refresh.
     *
     * @param sessionId The session ID
     * @param newJwtToken New JWT access token
     * @param newRefreshToken New refresh token
     * @param newTokenExpiryMs New token expiry time in milliseconds
     * @return Result indicating success or failure
     */
    suspend fun updateSessionTokens(
        sessionId: String,
        newJwtToken: String,
        newRefreshToken: String,
        newTokenExpiryMs: Long
    ): Result<Unit> {
        val newExpiresAt = newTokenExpiryMs.toString()

        return sessionRepository.updateSessionTokens(
            sessionId = sessionId,
            newJwtToken = newJwtToken,
            newRefreshToken = newRefreshToken,
            newExpiresAt = newExpiresAt
        ).onSuccess {
            // Update last accessed time
            sessionRepository.updateSessionLastAccessed(sessionId)

            // Restart rotation with new expiry
            stopTokenRotation()
            startTokenRotation(sessionId, newTokenExpiryMs)
        }
    }

    /**
     * Validate if a JWT token is valid (not blacklisted).
     *
     * @param jwtToken The JWT token to validate
     * @return Result with true if valid, false if blacklisted
     */
    suspend fun validateToken(jwtToken: String): Result<Boolean> {
        return sessionRepository.isTokenBlacklisted(jwtToken)
            .map { isBlacklisted -> !isBlacklisted }
    }

    /**
     * End the current session (logout).
     *
     * @param reason Reason for ending session (default: "logout")
     * @return Result indicating success or failure
     */
    suspend fun endCurrentSession(reason: String = "logout"): Result<Unit> {
        val sessionId = _currentSessionId.value ?: return Result.success(Unit)

        stopTokenRotation()

        return sessionRepository.revokeSession(sessionId, reason)
            .onSuccess {
                _currentSessionId.value = null
            }
    }

    /**
     * End all sessions for a user (logout from all devices).
     *
     * @param userId The user ID
     * @param reason Reason for ending sessions (default: "logout_all")
     * @return Result indicating success or failure
     */
    suspend fun endAllUserSessions(userId: String, reason: String = "logout_all"): Result<Unit> {
        stopTokenRotation()

        return sessionRepository.revokeAllUserSessions(userId, reason)
            .onSuccess {
                _currentSessionId.value = null
            }
    }

    /**
     * End all other sessions except the current one.
     *
     * Useful for "logout from other devices" functionality.
     *
     * @param userId The user ID
     * @param reason Reason for ending sessions (default: "logout_others")
     * @return Result indicating success or failure
     */
    suspend fun endAllOtherSessions(userId: String, reason: String = "logout_others"): Result<Unit> {
        val currentSessionId = _currentSessionId.value ?: return Result.success(Unit)

        return sessionRepository.revokeAllOtherSessions(userId, currentSessionId, reason)
    }

    /**
     * Get all active sessions for a user.
     *
     * @param userId The user ID
     * @return Result with list of sessions
     */
    suspend fun getActiveUserSessions(userId: String) =
        sessionRepository.getActiveSessionsForUser(userId)

    /**
     * Count active sessions for a user.
     *
     * @param userId The user ID
     * @return Result with session count
     */
    suspend fun countActiveUserSessions(userId: String) =
        sessionRepository.countActiveSessions(userId)

    /**
     * Start automatic token rotation.
     *
     * Monitors token expiry and triggers refresh callback when token is about to expire.
     *
     * @param sessionId The session ID to monitor
     * @param tokenExpiryMs Token expiry time in milliseconds from epoch
     * @param refreshThresholdMs Refresh token when this many milliseconds remain (default: 5 minutes)
     * @param onTokenRefreshNeeded Callback when token needs refresh
     */
    fun startTokenRotation(
        sessionId: String,
        tokenExpiryMs: Long,
        refreshThresholdMs: Long = 5 * 60 * 1000, // 5 minutes
        onTokenRefreshNeeded: suspend (sessionId: String) -> Unit = {}
    ) {
        tokenRotationJob?.cancel()

        tokenRotationJob = scope.launch {
            while (isActive) {
                val currentTime = currentTimeMillis()
                val timeUntilExpiry = tokenExpiryMs - currentTime

                when {
                    timeUntilExpiry <= 0 -> {
                        // Token expired - trigger refresh immediately
                        try {
                            onTokenRefreshNeeded(sessionId)
                        } catch (e: Exception) {
                            println("Token refresh failed: ${e.message}")
                        }
                        // Wait before retrying
                        delay(30_000) // 30 seconds
                    }
                    timeUntilExpiry <= refreshThresholdMs -> {
                        // Token expiring soon - trigger refresh
                        try {
                            onTokenRefreshNeeded(sessionId)
                        } catch (e: Exception) {
                            println("Token refresh failed: ${e.message}")
                        }
                        // Wait before checking again
                        delay(60_000) // 1 minute
                    }
                    else -> {
                        // Token still valid - check again in 1 minute
                        delay(60_000)
                    }
                }
            }
        }
    }

    /**
     * Stop token rotation monitoring.
     */
    fun stopTokenRotation() {
        tokenRotationJob?.cancel()
        tokenRotationJob = null
    }

    /**
     * Cleanup old sessions (remove expired/revoked sessions older than 30 days).
     *
     * Should be called periodically in background.
     */
    suspend fun cleanupOldSessions(): Result<Unit> {
        return sessionRepository.cleanupOldSessions()
    }

    /**
     * Cleanup expired JWT blacklist entries.
     *
     * Should be called periodically in background.
     */
    suspend fun cleanupExpiredBlacklist(): Result<Unit> {
        return sessionRepository.cleanupExpiredBlacklist()
    }

    /**
     * Generate device fingerprint hash.
     *
     * Combines device ID, name, and user agent to create a unique fingerprint.
     */
    private fun generateDeviceFingerprint(
        deviceId: String,
        deviceName: String,
        userAgent: String?
    ): String {
        val fingerprintString = "$deviceId|$deviceName|${userAgent ?: "unknown"}"
        // Use a simple hash for fingerprint
        // In production, this should include more device characteristics
        return fingerprintString.hashCode().toString(16).padStart(8, '0')
    }

    /**
     * Get device type based on platform.
     */
    private fun getDeviceType(): String {
        // This should be implemented as expect/actual for platform-specific detection
        // For now, return "unknown"
        return "unknown"
    }

    /**
     * Clean up resources.
     */
    fun dispose() {
        stopTokenRotation()
        scope.cancel()
    }
}
