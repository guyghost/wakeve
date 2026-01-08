package com.guyghost.wakeve.auth.shell.services

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of TokenStorage using EncryptedSharedPreferences
 * with Android Keystore for secure storage.
 */
class AndroidTokenStorage(
    private val context: Context
) : TokenStorage {

    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to non-encrypted prefs if encrypted not available
            context.getSharedPreferences(PREFS_FILE_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }

    override suspend fun storeString(key: String, value: String) {
        withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().putString(key, value).apply()
            } catch (e: Exception) {
                // Log error in production, but don't throw to maintain interface contract
            }
        }
    }

    override suspend fun getString(key: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.getString(key, null)
            } catch (e: Exception) {
                // Log error in production, but don't throw to maintain interface contract
                null
            }
        }
    }

    override suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().remove(key).apply()
            } catch (e: Exception) {
                // Log error in production, but don't throw to maintain interface contract
            }
        }
    }

    override suspend fun contains(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.contains(key)
            } catch (e: Exception) {
                // Log error in production, but don't throw to maintain interface contract
                false
            }
        }
    }

    override suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().clear().apply()
            } catch (e: Exception) {
                // Log error in production, but don't throw to maintain interface contract
            }
        }
    }

    companion object {
        private const val PREFS_FILE_NAME = "wakeve_auth_prefs"
    }
}
