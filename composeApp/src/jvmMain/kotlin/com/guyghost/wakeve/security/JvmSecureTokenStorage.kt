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

    // User profile keys
    private val userEmailKey = "user_email"
    private val userNameKey = "user_name"
    private val userProviderKey = "user_provider"
    private val userAvatarUrlKey = "user_avatar_url"

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

    override suspend fun storeUserEmail(email: String): Result<Unit> = runCatching {
        prefs.put(userEmailKey, email)
        prefs.flush()
    }

    override suspend fun storeUserName(name: String): Result<Unit> = runCatching {
        prefs.put(userNameKey, name)
        prefs.flush()
    }

    override suspend fun storeUserProvider(provider: String): Result<Unit> = runCatching {
        prefs.put(userProviderKey, provider)
        prefs.flush()
    }

    override suspend fun storeUserAvatarUrl(avatarUrl: String?): Result<Unit> = runCatching {
        if (avatarUrl != null) {
            prefs.put(userAvatarUrlKey, avatarUrl)
        } else {
            prefs.remove(userAvatarUrlKey)
        }
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

    override suspend fun getUserEmail(): String? {
        return prefs.get(userEmailKey, null)
    }

    override suspend fun getUserName(): String? {
        return prefs.get(userNameKey, null)
    }

    override suspend fun getUserProvider(): String? {
        return prefs.get(userProviderKey, null)
    }

    override suspend fun getUserAvatarUrl(): String? {
        return prefs.get(userAvatarUrlKey, null)
    }

    override suspend fun getUserProfile(): UserProfileData? {
        val userId = getUserId() ?: return null
        return UserProfileData(
            userId = userId,
            email = getUserEmail(),
            name = getUserName(),
            provider = getUserProvider(),
            avatarUrl = getUserAvatarUrl()
        )
    }

    override suspend fun storeUserProfile(profile: UserProfileData): Result<Unit> = runCatching {
        storeUserId(profile.userId)
        profile.email?.let { storeUserEmail(it) }
        profile.name?.let { storeUserName(it) }
        profile.provider?.let { storeUserProvider(it) }
        storeUserAvatarUrl(profile.avatarUrl)
    }

    override suspend fun clearAllTokens(): Result<Unit> = runCatching {
        prefs.remove(accessTokenKey)
        prefs.remove(refreshTokenKey)
        prefs.remove(userIdKey)
        prefs.remove(tokenExpiryKey)
        prefs.remove(userEmailKey)
        prefs.remove(userNameKey)
        prefs.remove(userProviderKey)
        prefs.remove(userAvatarUrlKey)
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
