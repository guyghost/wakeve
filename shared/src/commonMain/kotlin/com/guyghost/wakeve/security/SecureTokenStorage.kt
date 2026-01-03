package com.guyghost.wakeve.security

/**
 * Common interface for secure token storage across platforms
 */
interface SecureTokenStorage {
    suspend fun storeAccessToken(token: String): Result<Unit>
    suspend fun storeRefreshToken(token: String): Result<Unit>
    suspend fun storeUserId(userId: String): Result<Unit>
    suspend fun storeTokenExpiry(expiryTimestamp: Long): Result<Unit>

    // User profile storage
    suspend fun storeUserEmail(email: String): Result<Unit>
    suspend fun storeUserName(name: String): Result<Unit>
    suspend fun storeUserProvider(provider: String): Result<Unit>
    suspend fun storeUserAvatarUrl(avatarUrl: String?): Result<Unit>

    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun getUserId(): String?
    suspend fun getTokenExpiry(): Long?

    // User profile retrieval
    suspend fun getUserEmail(): String?
    suspend fun getUserName(): String?
    suspend fun getUserProvider(): String?
    suspend fun getUserAvatarUrl(): String?

    /**
     * Get all user profile data at once
     */
    suspend fun getUserProfile(): UserProfileData?

    /**
     * Store all user profile data at once
     */
    suspend fun storeUserProfile(profile: UserProfileData): Result<Unit>

    suspend fun clearAllTokens(): Result<Unit>
    suspend fun isTokenExpired(): Boolean
    suspend fun hasValidToken(): Boolean
}

/**
 * Data class holding user profile information from storage
 */
data class UserProfileData(
    val userId: String,
    val email: String?,
    val name: String?,
    val provider: String?,
    val avatarUrl: String?
)