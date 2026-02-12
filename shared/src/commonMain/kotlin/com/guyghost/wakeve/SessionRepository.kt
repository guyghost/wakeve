package com.guyghost.wakeve

import com.guyghost.wakeve.database.WakeveDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compute SHA-256 hash of a string.
 * Platform-specific implementation.
 */
expect fun sha256Hash(input: String): String

/**
 * Get current time in milliseconds since epoch.
 * Platform-specific implementation.
 */
expect fun currentTimeMillis(): Long

/**
 * Session data model for application use.
 */
data class SessionData(
    val id: String,
    val userId: String,
    val deviceId: String,
    val deviceName: String,
    val jwtTokenHash: String,
    val refreshTokenHash: String,
    val ipAddress: String?,
    val userAgent: String?,
    val createdAt: String,
    val lastAccessed: String,
    val expiresAt: String,
    val status: String
)

/**
 * Device fingerprint data model.
 */
data class DeviceData(
    val id: String,
    val userId: String,
    val deviceId: String,
    val deviceName: String,
    val deviceType: String?,
    val fingerprintHash: String,
    val firstSeen: String,
    val lastSeen: String,
    val trusted: Boolean,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Repository for managing user sessions, JWT blacklist, and device fingerprints.
 *
 * This repository handles:
 * - Multi-device session tracking
 * - JWT token blacklisting for revoked tokens
 * - Device fingerprinting for security
 * - Session lifecycle management (creation, validation, revocation)
 */
class SessionRepository(private val db: WakeveDb) {

    private val sessionQueries = db.sessionQueries

    // =================================================
    // Session Management
    // =================================================

    /**
     * Create a new session.
     *
     * @param userId The user ID
     * @param deviceId Unique device identifier
     * @param deviceName Human-readable device name
     * @param jwtToken The JWT access token
     * @param refreshToken The refresh token
     * @param ipAddress Optional IP address
     * @param userAgent Optional user agent string
     * @param expiresAt Session expiration timestamp
     * @return The created session ID
     */
    suspend fun createSession(
        userId: String,
        deviceId: String,
        deviceName: String,
        jwtToken: String,
        refreshToken: String,
        ipAddress: String? = null,
        userAgent: String? = null,
        expiresAt: String
    ): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            val sessionId = generateSessionId()
            val jwtTokenHash = hashToken(jwtToken)
            val refreshTokenHash = hashToken(refreshToken)
            val now = getCurrentTimestamp()

            sessionQueries.insertSession(
                id = sessionId,
                user_id = userId,
                device_id = deviceId,
                device_name = deviceName,
                jwt_token_hash = jwtTokenHash,
                refresh_token_hash = refreshTokenHash,
                ip_address = ipAddress,
                user_agent = userAgent,
                created_at = now,
                last_accessed = now,
                expires_at = expiresAt,
                status = "active"
            )

            sessionId
        }
    }

    /**
     * Get all active sessions for a user.
     */
    suspend fun getActiveSessionsForUser(userId: String): Result<List<SessionData>> = withContext(Dispatchers.Default) {
        runCatching {
            val now = getCurrentTimestamp()
            sessionQueries.selectActiveSessionsByUserId(userId, now)
                .executeAsList()
                .map { it.toSessionData() }
        }
    }

    /**
     * Get session by ID.
     */
    suspend fun getSessionById(sessionId: String): Result<SessionData?> = withContext(Dispatchers.Default) {
        runCatching {
            sessionQueries.selectSessionById(sessionId)
                .executeAsOneOrNull()
                ?.toSessionData()
        }
    }

    /**
     * Get session by JWT token hash.
     */
    suspend fun getSessionByToken(jwtToken: String): Result<SessionData?> = withContext(Dispatchers.Default) {
        runCatching {
            val tokenHash = hashToken(jwtToken)
            sessionQueries.selectSessionByTokenHash(tokenHash)
                .executeAsOneOrNull()
                ?.toSessionData()
        }
    }

    /**
     * Update session last accessed time.
     */
    suspend fun updateSessionLastAccessed(sessionId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val now = getCurrentTimestamp()
            sessionQueries.updateSessionLastAccessed(now, sessionId)
        }
    }

    /**
     * Update session tokens (after token refresh).
     */
    suspend fun updateSessionTokens(
        sessionId: String,
        newJwtToken: String,
        newRefreshToken: String,
        newExpiresAt: String
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val jwtTokenHash = hashToken(newJwtToken)
            val refreshTokenHash = hashToken(newRefreshToken)
            val now = getCurrentTimestamp()

            sessionQueries.updateSessionTokens(
                jwt_token_hash = jwtTokenHash,
                refresh_token_hash = refreshTokenHash,
                expires_at = newExpiresAt,
                last_accessed = now,
                id = sessionId
            )
        }
    }

    /**
     * Revoke a specific session (logout from one device).
     */
    suspend fun revokeSession(sessionId: String, reason: String = "logout"): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            // Get session to blacklist its token
            val session = sessionQueries.selectSessionById(sessionId).executeAsOneOrNull()
            if (session != null) {
                // Add token to blacklist
                addToBlacklist(session.jwt_token_hash, session.user_id, reason, session.expires_at)
            }

            // Revoke the session
            sessionQueries.revokeSession(sessionId)
        }
    }

    /**
     * Revoke all sessions for a user (logout from all devices).
     */
    suspend fun revokeAllUserSessions(userId: String, reason: String = "logout_all"): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            // Get all active sessions to blacklist their tokens
            val now = getCurrentTimestamp()
            val sessions = sessionQueries.selectActiveSessionsByUserId(userId, now).executeAsList()

            sessions.forEach { session ->
                addToBlacklist(session.jwt_token_hash, userId, reason, session.expires_at)
            }

            // Revoke all sessions
            sessionQueries.revokeAllUserSessions(userId)
        }
    }

    /**
     * Revoke all other sessions except the current one.
     */
    suspend fun revokeAllOtherSessions(userId: String, currentSessionId: String, reason: String = "logout_others"): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            // Get all active sessions except current
            val now = getCurrentTimestamp()
            val sessions = sessionQueries.selectActiveSessionsByUserId(userId, now).executeAsList()
                .filter { it.id != currentSessionId }

            sessions.forEach { session ->
                addToBlacklist(session.jwt_token_hash, userId, reason, session.expires_at)
            }

            // Revoke all other sessions
            sessionQueries.revokeAllOtherSessions(userId, currentSessionId)
        }
    }

    /**
     * Count active sessions for a user.
     */
    suspend fun countActiveSessions(userId: String): Result<Long> = withContext(Dispatchers.Default) {
        runCatching {
            val now = getCurrentTimestamp()
            sessionQueries.countActiveSessionsByUserId(userId, now).executeAsOne()
        }
    }

    /**
     * Cleanup old sessions (delete expired/revoked sessions older than 30 days).
     */
    suspend fun cleanupOldSessions(): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val thirtyDaysAgo = (currentTimeMillis() - 30L * 24 * 60 * 60 * 1000).toString()

            // Mark expired sessions
            val now = getCurrentTimestamp()
            sessionQueries.markExpiredSessions(now)

            // Delete old sessions
            sessionQueries.deleteOldSessions(thirtyDaysAgo)
        }
    }

    // =================================================
    // JWT Blacklist Management
    // =================================================

    /**
     * Check if a JWT token is blacklisted.
     */
    suspend fun isTokenBlacklisted(jwtToken: String): Result<Boolean> = withContext(Dispatchers.Default) {
        runCatching {
            val tokenHash = hashToken(jwtToken)
            sessionQueries.isTokenBlacklisted(tokenHash).executeAsOne()
        }
    }

    /**
     * Add a token to the blacklist.
     */
    private suspend fun addToBlacklist(
        tokenHash: String,
        userId: String,
        reason: String,
        expiresAt: String
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val now = getCurrentTimestamp()
            sessionQueries.insertBlacklistedToken(
                token_hash = tokenHash,
                user_id = userId,
                revoked_at = now,
                reason = reason,
                expires_at = expiresAt
            )
        }
    }

    /**
     * Cleanup expired blacklist entries.
     */
    suspend fun cleanupExpiredBlacklist(): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val now = getCurrentTimestamp()
            sessionQueries.cleanupExpiredBlacklist(now)
        }
    }

    // =================================================
    // Device Fingerprint Management
    // =================================================

    /**
     * Register or update a device fingerprint.
     */
    suspend fun registerDevice(
        userId: String,
        deviceId: String,
        deviceName: String,
        deviceType: String?,
        fingerprintHash: String,
        trusted: Boolean = false
    ): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            // Check if device already exists
            val existing = sessionQueries.selectDeviceByFingerprint(fingerprintHash).executeAsOneOrNull()

            if (existing != null) {
                // Update last seen
                val now = getCurrentTimestamp()
                sessionQueries.updateDeviceLastSeen(now, now, existing.id)
                existing.id
            } else {
                // Create new device
                val deviceDbId = generateDeviceId()
                val now = getCurrentTimestamp()

                sessionQueries.insertDevice(
                    id = deviceDbId,
                    user_id = userId,
                    device_id = deviceId,
                    device_name = deviceName,
                    device_type = deviceType,
                    fingerprint_hash = fingerprintHash,
                    first_seen = now,
                    last_seen = now,
                    trusted = if (trusted) 1L else 0L,
                    created_at = now,
                    updated_at = now
                )

                deviceDbId
            }
        }
    }

    /**
     * Get all devices for a user.
     */
    suspend fun getDevicesForUser(userId: String): Result<List<DeviceData>> = withContext(Dispatchers.Default) {
        runCatching {
            sessionQueries.selectDevicesByUserId(userId)
                .executeAsList()
                .map { it.toDeviceData() }
        }
    }

    /**
     * Update device trust status.
     */
    suspend fun updateDeviceTrust(deviceId: String, trusted: Boolean): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val now = getCurrentTimestamp()
            sessionQueries.updateDeviceTrust(if (trusted) 1L else 0L, now, deviceId)
        }
    }

    // =================================================
    // Helper Methods
    // =================================================

    /**
     * Hash a token using SHA-256.
     */
    private fun hashToken(token: String): String {
        return sha256Hash(token)
    }

    /**
     * Generate a unique session ID.
     */
    private fun generateSessionId(): String {
        return "session-${currentTimeMillis()}-${(0..999999).random()}"
    }

    /**
     * Generate a unique device ID.
     */
    private fun generateDeviceId(): String {
        return "device-${currentTimeMillis()}-${(0..999999).random()}"
    }

    /**
     * Get current timestamp as string.
     */
    private fun getCurrentTimestamp(): String {
        return currentTimeMillis().toString()
    }

    /**
     * Convert database Session to SessionData.
     */
    private fun Session.toSessionData() = SessionData(
        id = id,
        userId = user_id,
        deviceId = device_id,
        deviceName = device_name,
        jwtTokenHash = jwt_token_hash,
        refreshTokenHash = refresh_token_hash,
        ipAddress = ip_address,
        userAgent = user_agent,
        createdAt = created_at,
        lastAccessed = last_accessed,
        expiresAt = expires_at,
        status = status
    )

    /**
     * Convert database Device_fingerprint to DeviceData.
     */
    private fun Device_fingerprint.toDeviceData() = DeviceData(
        id = id,
        userId = user_id,
        deviceId = device_id,
        deviceName = device_name,
        deviceType = device_type,
        fingerprintHash = fingerprint_hash,
        firstSeen = first_seen,
        lastSeen = last_seen,
        trusted = trusted == 1L,
        createdAt = created_at,
        updatedAt = updated_at
    )
}
