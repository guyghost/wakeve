package com.guyghost.wakeve

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.guyghost.wakeve.models.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implémentation Android du stockage sécurisé
 * Utilise EncryptedSharedPreferences pour chiffrer les données sensibles
 */
class AndroidSecureStorage(private val context: Context) : SecureStorage {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedSharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "wakeve_auth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    private val sessionKey = "user_session"
    
    override suspend fun saveSession(session: UserSession) {
        val json = Json.encodeToString(session)
        encryptedSharedPreferences.edit().apply {
            putString(sessionKey, json)
            apply()
        }
    }
    
    override suspend fun getSession(): UserSession? {
        val json = encryptedSharedPreferences.getString(sessionKey, null) ?: return null
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun clearSession() {
        encryptedSharedPreferences.edit().apply {
            remove(sessionKey)
            apply()
        }
    }
}
