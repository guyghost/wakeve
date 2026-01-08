package com.guyghost.wakeve.auth.shell.services

/**
 * Interface for secure token storage.
 * Implementations should use platform-specific secure storage:
 * - Android: Android Keystore
 * - iOS: iOS Keychain
 */
interface TokenStorage {
    /**
     * Stores a string value securely.
     * 
     * @param key The key to store under
     * @param value The value to store
     */
    suspend fun storeString(key: String, value: String)

    /**
     * Retrieves a stored string value.
     * 
     * @param key The key to retrieve
     * @return The stored value, or null if not found
     */
    suspend fun getString(key: String): String?

    /**
     * Removes a stored value.
     * 
     * @param key The key to remove
     */
    suspend fun remove(key: String)

    /**
     * Checks if a key exists in storage.
     * 
     * @param key The key to check
     * @return true if the key exists
     */
    suspend fun contains(key: String): Boolean

    /**
     * Clears all stored values.
     */
    suspend fun clearAll()
}

/**
 * Keys for token storage.
 */
object TokenKeys {
    const val ACCESS_TOKEN = "access_token"
    const val REFRESH_TOKEN = "refresh_token"
    const val ID_TOKEN = "id_token"
    const val USER_ID = "user_id"
    const val AUTH_METHOD = "auth_method"
    const val GUEST_USER_ID = "guest_user_id"
    const val TOKEN_EXPIRY = "token_expiry"
}
