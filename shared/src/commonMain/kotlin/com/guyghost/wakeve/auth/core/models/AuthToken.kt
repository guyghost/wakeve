package com.guyghost.wakeve.auth.core.models

/**
 * Represents an authentication token (JWT or session token).
 * This is a value object that encapsulates token data with validation.
 * 
 * @property value The raw token string
 * @property type The type of token (Bearer, JWT, Session)
 * @property expiresAt Timestamp when the token expires (milliseconds since epoch)
 */
data class AuthToken(
    val value: String,
    val type: TokenType = TokenType.BEARER,
    val expiresAt: Long
) {
    /**
     * Returns true if the token is expired.
     * 
     * @param currentTime The current time in milliseconds (defaults to System.currentTimeMillis())
     * @return True if the token is expired
     */
    fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean =
        currentTime >= expiresAt

    /**
     * Returns true if the token expires within the given time window.
     * 
     * @param windowMs The time window in milliseconds
     * @param currentTime The current time
     * @return True if the token expires within the window
     */
    fun expiresWithin(windowMs: Long, currentTime: Long = System.currentTimeMillis()): Boolean =
        expiresAt - currentTime <= windowMs

    /**
     * Returns the time remaining until expiration in milliseconds.
     * 
     * @param currentTime The current time
     * @return Time remaining, or 0 if already expired
     */
    fun timeUntilExpiry(currentTime: Long = System.currentTimeMillis()): Long =
        (expiresAt - currentTime).coerceAtLeast(0)

    /**
     * Returns true if the token is valid (not expired).
     * 
     * @param currentTime The current time
     * @return True if the token is valid
     */
    fun isValid(currentTime: Long = System.currentTimeMillis()): Boolean =
        value.isNotBlank() && !isExpired(currentTime)

    companion object {
        /**
         * Creates a new AuthToken with the given value and expiry time.
         * 
         * @param value The token value
         * @param type The token type
         * @param expiresInSeconds Time until expiry in seconds
         * @return A new AuthToken
         */
        fun create(
            value: String,
            type: TokenType = TokenType.BEARER,
            expiresInSeconds: Long
        ): AuthToken = AuthToken(
            value = value,
            type = type,
            expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000)
        )

        /**
         * Creates a long-lived token (e.g., for refresh tokens).
         * 
         * @param value The token value
         * @param type The token type
         * @param expiresInDays Days until expiry
         * @return A new AuthToken
         */
        fun createLongLived(
            value: String,
            type: TokenType = TokenType.BEARER,
            expiresInDays: Long = 30
        ): AuthToken = AuthToken(
            value = value,
            type = type,
            expiresAt = System.currentTimeMillis() + (expiresInDays * 24 * 60 * 60 * 1000)
        )
    }
}

/**
 * Types of authentication tokens supported by Wakeve.
 */
enum class TokenType {
    /** Standard OAuth 2.0 Bearer token */
    BEARER,
    
    /** JSON Web Token (JWT) */
    JWT,
    
    /** Session-based token */
    SESSION,

    /** Refresh token for obtaining new access tokens */
    REFRESH
}
