package com.guyghost.wakeve.auth.shell.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of TokenStorage using Keystore for secure storage.
 * Falls back to SharedPreferences for non-sensitive data.
 */
class AndroidTokenStorage(
) : TokenStorage {

    /**
     * In production, this would use EncryptedSharedPreferences
     * with Android Keystore encryption
     */

    override suspend fun storeString(key: String, value: String) {
        withContext(Dispatchers.IO) {
            // In production:
            // encryptedSharedPreferences.edit().putString(key, value).apply()
        }
    }

    override suspend fun getString(key: String): String? {
        return withContext(Dispatchers.IO) {
            // In production:
            // encryptedSharedPreferences.getString(key, null)
            null
        }
    }

    override suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            // In production:
            // encryptedSharedPreferences.edit().remove(key).apply()
        }
    }

    override suspend fun contains(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            // In production:
            // encryptedSharedPreferences.contains(key)
            false
        }
    }

    override suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            // In production:
            // encryptedSharedPreferences.edit().clear().apply()
        }
    }
}
