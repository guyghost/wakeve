package com.guyghost.wakeve.security

/**
 * Common interface for secure token storage across platforms
 */
interface SecureTokenStorage {
    suspend fun storeAccessToken(token: String): Result<Unit>
    suspend fun storeRefreshToken(token: String): Result<Unit>
    suspend fun storeUserId(userId: String): Result<Unit>
    suspend fun storeTokenExpiry(expiryTimestamp: Long): Result<Unit>

    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun getUserId(): String?
    suspend fun getTokenExpiry(): Long?

    suspend fun clearAllTokens(): Result<Unit>
    suspend fun isTokenExpired(): Boolean
    suspend fun hasValidToken(): Boolean
}