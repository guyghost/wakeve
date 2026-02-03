package com.guyghost.wakeve

import com.guyghost.wakeve.auth.UserRole
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.NotificationPreferences
import com.guyghost.wakeve.models.OAuthProvider
import com.guyghost.wakeve.models.SyncMetadata
import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.User
import com.guyghost.wakeve.models.UserToken
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserRepository(private val db: WakevDb) {

    private val userQueries = db.userQueries

    // User operations
    suspend fun createUser(
        providerId: String,
        email: String,
        name: String,
        avatarUrl: String? = null,
        provider: OAuthProvider
    ): Result<User> = runCatching {
        val now = getCurrentUtcIsoString()
        val userId = "user_${providerId.hashCode()}_${now.hashCode()}"

        userQueries.insertUser(
            id = userId,
            provider_id = providerId,
            email = email,
            name = name,
            avatar_url = avatarUrl,
            provider = provider.name.lowercase(),
            role = UserRole.default().name,  // Default role for new users
            created_at = now,
            updated_at = now
        )

        // Create default notification preferences
        createDefaultNotificationPreferences(userId)

        // Return the created user
        userQueries.selectUserById(userId).executeAsOne().let { row ->
            User(
                id = row.id,
                providerId = row.provider_id,
                email = row.email,
                name = row.name,
                avatarUrl = row.avatar_url,
                provider = OAuthProvider.valueOf(row.provider.uppercase()),
                role = UserRole.fromString(row.role) ?: UserRole.USER,
                createdAt = row.created_at,
                updatedAt = row.updated_at
            )
        }
    }

    suspend fun getUserById(userId: String): User? = runCatching {
        userQueries.selectUserById(userId).executeAsOneOrNull()?.let { row ->
            User(
                id = row.id,
                providerId = row.provider_id,
                email = row.email,
                name = row.name,
                avatarUrl = row.avatar_url,
                provider = OAuthProvider.valueOf(row.provider.uppercase()),
                role = UserRole.fromString(row.role) ?: UserRole.USER,
                createdAt = row.created_at,
                updatedAt = row.updated_at
            )
        }
    }.getOrNull()

    suspend fun getUserByProviderId(providerId: String, provider: OAuthProvider): User? = runCatching {
        userQueries.selectUserByProviderId(providerId, provider.name.lowercase()).executeAsOneOrNull()?.let { row ->
            User(
                id = row.id,
                providerId = row.provider_id,
                email = row.email,
                name = row.name,
                avatarUrl = row.avatar_url,
                provider = OAuthProvider.valueOf(row.provider.uppercase()),
                role = UserRole.fromString(row.role) ?: UserRole.USER,
                createdAt = row.created_at,
                updatedAt = row.updated_at
            )
        }
    }.getOrNull()

    suspend fun getUserByEmail(email: String): User? = runCatching {
        userQueries.selectUserByEmail(email).executeAsOneOrNull()?.let { row ->
            User(
                id = row.id,
                providerId = row.provider_id,
                email = row.email,
                name = row.name,
                avatarUrl = row.avatar_url,
                provider = OAuthProvider.valueOf(row.provider.uppercase()),
                role = UserRole.fromString(row.role) ?: UserRole.USER,
                createdAt = row.created_at,
                updatedAt = row.updated_at
            )
        }
    }.getOrNull()

    suspend fun updateUser(userId: String, name: String, avatarUrl: String?): Result<User> = runCatching {
        val now = getCurrentUtcIsoString()
        userQueries.updateUser(name, avatarUrl, now, userId)

        getUserById(userId) ?: throw IllegalStateException("User not found after update")
    }

    // Token operations
    suspend fun createToken(
        userId: String,
        accessToken: String,
        refreshToken: String? = null,
        tokenType: String = "Bearer",
        expiresAt: String,
        scope: String? = null
    ): Result<UserToken> = runCatching {
        val now = getCurrentUtcIsoString()
        val tokenId = "token_${userId.hashCode()}_${now.hashCode()}"

        userQueries.insertToken(
            id = tokenId,
            user_id = userId,
            access_token = accessToken,
            refresh_token = refreshToken,
            token_type = tokenType,
            expires_at = expiresAt,
            scope = scope,
            created_at = now,
            updated_at = now
        )

        // Return the created token
        userQueries.selectTokenById(tokenId).executeAsOne().let { row ->
            UserToken(
                id = row.id,
                userId = row.user_id,
                accessToken = row.access_token,
                refreshToken = row.refresh_token,
                tokenType = row.token_type,
                expiresAt = row.expires_at,
                scope = row.scope,
                createdAt = row.created_at,
                updatedAt = row.updated_at
            )
        }
    }

    suspend fun getTokenByUserId(userId: String): UserToken? = runCatching {
        userQueries.selectTokenByUserId(userId).executeAsOneOrNull()?.let { row ->
            UserToken(
                id = row.id,
                userId = row.user_id,
                accessToken = row.access_token,
                refreshToken = row.refresh_token,
                tokenType = row.token_type,
                expiresAt = row.expires_at,
                scope = row.scope,
                createdAt = row.created_at,
                updatedAt = row.updated_at
            )
        }
    }.getOrNull()

    suspend fun getUserTokenByRefreshToken(refreshToken: String): UserToken? = runCatching {
        userQueries.selectTokenByRefreshToken(refreshToken).executeAsOneOrNull()?.let { row ->
            UserToken(
                id = row.id,
                userId = row.user_id,
                accessToken = row.access_token,
                refreshToken = row.refresh_token,
                tokenType = row.token_type,
                expiresAt = row.expires_at,
                scope = row.scope,
                createdAt = row.created_at,
                updatedAt = row.updated_at
            )
        }
    }.getOrNull()

    suspend fun updateToken(tokenId: String, accessToken: String, refreshToken: String?, expiresAt: String): Result<UserToken> = runCatching {
        val now = getCurrentUtcIsoString()
        userQueries.updateToken(accessToken, refreshToken, expiresAt, now, tokenId)

        userQueries.selectTokenById(tokenId).executeAsOne().let { row ->
            UserToken(
                id = row.id,
                userId = row.user_id,
                accessToken = row.access_token,
                refreshToken = row.refresh_token,
                tokenType = row.token_type,
                expiresAt = row.expires_at,
                scope = row.scope,
                createdAt = row.created_at,
                updatedAt = row.updated_at
            )
        }
    }

    suspend fun updateTokenExpiry(tokenId: String, expiresAt: String): Result<Unit> = runCatching {
        val now = getCurrentUtcIsoString()
        userQueries.updateTokenExpiry(expiresAt, now, tokenId)
    }

    suspend fun deleteTokensForUser(userId: String): Result<Unit> = runCatching {
        userQueries.deleteToken(userId)
    }

    suspend fun cleanupExpiredTokens(): Result<Unit> = runCatching {
        val now = getCurrentUtcIsoString()
        userQueries.deleteExpiredTokens(now)
    }

    // Notification preferences operations
    private suspend fun createDefaultNotificationPreferences(userId: String): Result<NotificationPreferences> = runCatching {
        val now = getCurrentUtcIsoString()
        val enabledTypes = setOf("DEADLINE_REMINDER", "EVENT_UPDATE", "VOTE_CLOSE_REMINDER")
        val enabledTypesJson = Json.encodeToString(enabledTypes.toList())
        val updatedAtMillis = currentTimeMillis()

        userQueries.insertPreferences(
            user_id = userId,
            enabled_types = enabledTypesJson,
            quiet_hours_start = null,
            quiet_hours_end = null,
            sound_enabled = 1L,
            vibration_enabled = 1L,
            updated_at = updatedAtMillis
        )

        // Return the created preferences
        userQueries.selectPreferencesByUserId(userId).executeAsOne().let { row ->
            val parsedEnabled = parseEnabledTypes(row.enabled_types)
            NotificationPreferences(
                id = "prefs_${row.user_id}",
                userId = row.user_id,
                deadlineReminder = "DEADLINE_REMINDER" in parsedEnabled,
                eventUpdate = "EVENT_UPDATE" in parsedEnabled,
                voteCloseReminder = "VOTE_CLOSE_REMINDER" in parsedEnabled,
                timezone = "UTC",
                createdAt = now,
                updatedAt = row.updated_at.toString()
            )
        }
    }

    suspend fun getNotificationPreferences(userId: String): NotificationPreferences? = runCatching {
        userQueries.selectPreferencesByUserId(userId).executeAsOneOrNull()?.let { row ->
            val parsedEnabled = parseEnabledTypes(row.enabled_types)
            NotificationPreferences(
                id = "prefs_${row.user_id}",
                userId = row.user_id,
                deadlineReminder = "DEADLINE_REMINDER" in parsedEnabled,
                eventUpdate = "EVENT_UPDATE" in parsedEnabled,
                voteCloseReminder = "VOTE_CLOSE_REMINDER" in parsedEnabled,
                timezone = "UTC",
                createdAt = row.updated_at.toString(),
                updatedAt = row.updated_at.toString()
            )
        }
    }.getOrNull()

    suspend fun updateNotificationPreferences(
        userId: String,
        deadlineReminder: Boolean,
        eventUpdate: Boolean,
        voteCloseReminder: Boolean,
        timezone: String
    ): Result<NotificationPreferences> = runCatching {
        val now = currentTimeMillis()
        val enabledTypes = buildSet {
            if (deadlineReminder) add("DEADLINE_REMINDER")
            if (eventUpdate) add("EVENT_UPDATE")
            if (voteCloseReminder) add("VOTE_CLOSE_REMINDER")
        }

        userQueries.updatePreferences(
            enabled_types = Json.encodeToString(enabledTypes.toList()),
            quiet_hours_start = null,
            quiet_hours_end = null,
            sound_enabled = 1L,
            vibration_enabled = 1L,
            updated_at = now,
            user_id = userId
        )

        getNotificationPreferences(userId) ?: throw IllegalStateException("Preferences not found after update")
    }

    // Sync metadata operations
    suspend fun addSyncMetadata(
        id: String,
        tableName: String,
        recordId: String,
        operation: SyncOperation,
        timestamp: String,
        userId: String
    ): Result<Unit> = runCatching {
        userQueries.insertSyncMetadata(
            id = id,
            table_name = tableName,
            record_id = recordId,
            operation = operation.name,
            timestamp = timestamp,
            user_id = userId,
            synced = 0L,
            retry_count = 0L,
            last_error = null
        )
    }

    suspend fun getPendingSyncChanges(): List<SyncMetadata> = runCatching {
        userQueries.selectPendingSync().executeAsList().map { row ->
            SyncMetadata(
                id = row.id,
                tableName = row.table_name,
                recordId = row.record_id,
                operation = SyncOperation.valueOf(row.operation),
                timestamp = row.timestamp,
                userId = row.user_id,
                synced = row.synced == 1L,
                retryCount = row.retry_count?.toInt() ?: 0,
                lastError = row.last_error
            )
        }
    }.getOrDefault(emptyList())

    suspend fun updateSyncStatus(
        syncId: String,
        synced: Boolean,
        retryCount: Int = 0,
        error: String? = null
    ): Result<Unit> = runCatching {
        userQueries.updateSyncMetadata(
            synced = if (synced) 1L else 0L,
            retry_count = retryCount.toLong(),
            last_error = error,
            id = syncId
        )
    }

    suspend fun removeSyncMetadata(syncId: String): Result<Unit> = runCatching {
        userQueries.deleteSyncMetadata(syncId)
    }

    suspend fun cleanupOldSyncMetadata(olderThan: String): Result<Unit> = runCatching {
        userQueries.cleanupOldSyncMetadata(olderThan)
    }

    private fun getCurrentUtcIsoString(): String {
        // For Phase 3 Sprint 1, we use a fixed test date
        // In Phase 4, integrate with kotlinx.datetime for full timezone support
        return "2025-11-12T10:00:00Z"
    }

    private fun parseEnabledTypes(enabledTypesJson: String?): Set<String> {
        if (enabledTypesJson.isNullOrBlank()) return emptySet()
        return runCatching { Json.decodeFromString<List<String>>(enabledTypesJson).toSet() }
            .getOrDefault(emptySet())
    }
}
