package com.guyghost.wakeve.security

import java.util.prefs.Preferences

/**
 * Secure token storage for JVM using Java Preferences API
 *
 * Note: This is a basic implementation using Preferences API.
 * For production, consider using a more secure solution like:
 * - OS-specific keychains (macOS Keychain, Windows DPAPI)
 * - Encrypted file storage
 * - Hardware security modules (HSM)
 */
class JvmSecureTokenStorage : SecureTokenStorage {
    private val prefs = Preferences.userNodeForPackage(JvmSecureTokenStorage::class.java)

    private val accessTokenKey = "access_token"
    private val refreshTokenKey = "refresh_token"
    private val userIdKey = "user_id"
    private val tokenExpiryKey = "token_expiry"

    override suspend fun storeAccessToken(token: String): Result<Unit> = runCatching {
        prefs.put(accessTokenKey, token)
        prefs.flush()
    }

    override suspend fun storeRefreshToken(token: String): Result<Unit> = runCatching {
        prefs.put(refreshTokenKey, token)
        prefs.flush()
    }

    override suspend fun storeUserId(userId: String): Result<Unit> = runCatching {
        prefs.put(userIdKey, userId)
        prefs.flush()
    }

    override suspend fun storeTokenExpiry(expiryTimestamp: Long): Result<Unit> = runCatching {
        prefs.putLong(tokenExpiryKey, expiryTimestamp)
        prefs.flush()
    }

    override suspend fun getAccessToken(): String? {
        return prefs.get(accessTokenKey, null)
    }

    override suspend fun getRefreshToken(): String? {
        return prefs.get(refreshTokenKey, null)
    }

    override suspend fun getUserId(): String? {
        return prefs.get(userIdKey, null)
    }

    override suspend fun getTokenExpiry(): Long? {
        val expiry = prefs.getLong(tokenExpiryKey, 0L)
        return if (expiry == 0L) null else expiry
    }

    override suspend fun clearAllTokens(): Result<Unit> = runCatching {
        prefs.remove(accessTokenKey)
        prefs.remove(refreshTokenKey)
        prefs.remove(userIdKey)
        prefs.remove(tokenExpiryKey)
        prefs.flush()
    }

    override suspend fun isTokenExpired(): Boolean {
        val expiry = getTokenExpiry()
        return expiry != null && System.currentTimeMillis() >= expiry
    }

    override suspend fun hasValidToken(): Boolean {
        val token = getAccessToken()
        return token != null && !isTokenExpired()
    }
}
