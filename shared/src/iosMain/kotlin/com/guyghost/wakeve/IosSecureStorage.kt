package com.guyghost.wakeve

import com.guyghost.wakeve.models.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implémentation iOS du stockage sécurisé
 * Délègue au gestionnaire de stockage SecureStorageManager en Swift
 */
class IosSecureStorage : SecureStorage {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun saveSession(session: UserSession) {
        // Déléguer à Swift
        iosSecureStorageSaveSession(
            userId = session.user.id,
            userEmail = session.user.email,
            userName = session.user.name,
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            expiresAt = session.expiresAt,
            createdAt = session.createdAt
        )
    }
    
    override suspend fun getSession(): UserSession? {
        // Déléguer à Swift
        val sessionData = iosSecureStorageGetSession()
        return sessionData
    }
    
    override fun clearSession() {
        // Déléguer à Swift
        iosSecureStorageClear()
    }
}

/**
 * Déclarations des fonctions interop Swift
 * Ces fonctions sont implémentées en Swift dans SecureStorageManager
 */
@kotlin.native.internal.ExportTypeInfo("errors")
external fun iosSecureStorageSaveSession(
    userId: String,
    userEmail: String,
    userName: String,
    accessToken: String,
    refreshToken: String,
    expiresAt: Long,
    createdAt: Long
)

@kotlin.native.internal.ExportTypeInfo("errors")
external fun iosSecureStorageGetSession(): UserSession?

@kotlin.native.internal.ExportTypeInfo("errors")
external fun iosSecureStorageClear()
