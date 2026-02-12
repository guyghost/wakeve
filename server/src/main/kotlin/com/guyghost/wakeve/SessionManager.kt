package com.guyghost.wakeve

import com.guyghost.wakeve.database.WakeveDb

/**
 * Server-side SessionManager wrapping SessionRepository
 *
 * This manager provides high-level session operations for the server endpoints.
 */
class SessionManager(private val database: WakeveDb) {
    private val sessionRepository = SessionRepository(database)

    /**
     * Get all active sessions for a user
     */
    suspend fun getUserSessions(userId: String): List<SessionData> {
        return sessionRepository.getActiveSessionsForUser(userId)
            .getOrElse { emptyList() }
    }

    /**
     * Get a specific session by ID
     */
    suspend fun getSession(sessionId: String): SessionData? {
        return sessionRepository.getSessionById(sessionId)
            .getOrNull()
    }

    /**
     * Revoke a session (add to blacklist and mark as revoked)
     */
    suspend fun revokeSession(sessionId: String): Result<Unit> {
        return sessionRepository.revokeSession(sessionId)
    }

    /**
     * Update session last accessed timestamp
     */
    suspend fun updateLastAccessed(sessionId: String): Result<Unit> {
        return sessionRepository.updateSessionLastAccessed(sessionId)
    }

    /**
     * Check if a token is blacklisted
     */
    suspend fun isTokenBlacklisted(token: String): Boolean {
        return sessionRepository.isTokenBlacklisted(token)
            .getOrElse { false }
    }
}
