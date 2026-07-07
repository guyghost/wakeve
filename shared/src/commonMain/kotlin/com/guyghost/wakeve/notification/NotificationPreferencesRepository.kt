package com.guyghost.wakeve.notification

import com.guyghost.wakeve.Notification_preferences
import com.guyghost.wakeve.database.WakeveDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface NotificationPreferencesRepositoryInterface {
    suspend fun getPreferences(userId: String): NotificationPreferences?
    suspend fun savePreferences(preferences: NotificationPreferences): Result<Unit>
    suspend fun deletePreferences(userId: String): Result<Unit>
}

/**
 * Repository for managing notification preferences in database.
 */
class NotificationPreferencesRepository(
    private val database: WakeveDb
) : NotificationPreferencesRepositoryInterface {

    override suspend fun getPreferences(userId: String): NotificationPreferences? = withContext(Dispatchers.Default) {
        runCatching {
            val record = database.userQueries.selectPreferencesByUserId(userId).executeAsOneOrNull()
            record?.toNotificationPreferences()
        }.getOrNull()
    }

    override suspend fun savePreferences(preferences: NotificationPreferences): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val enabledTypesJson = Json.encodeToString(preferences.enabledTypes.map { it.name })
            val updatedAtMs = preferences.updatedAt.toEpochMilliseconds()
            val existing = database.userQueries.selectPreferencesByUserId(preferences.userId).executeAsOneOrNull()

            if (existing == null) {
                database.userQueries.insertPreferences(
                    user_id = preferences.userId,
                    enabled_types = enabledTypesJson,
                    quiet_hours_start = preferences.quietHoursStart?.toDisplayString(),
                    quiet_hours_end = preferences.quietHoursEnd?.toDisplayString(),
                    sound_enabled = if (preferences.soundEnabled) 1L else 0L,
                    vibration_enabled = if (preferences.vibrationEnabled) 1L else 0L,
                    updated_at = updatedAtMs
                )
            } else {
                database.userQueries.updatePreferences(
                    enabled_types = enabledTypesJson,
                    quiet_hours_start = preferences.quietHoursStart?.toDisplayString(),
                    quiet_hours_end = preferences.quietHoursEnd?.toDisplayString(),
                    sound_enabled = if (preferences.soundEnabled) 1L else 0L,
                    vibration_enabled = if (preferences.vibrationEnabled) 1L else 0L,
                    updated_at = updatedAtMs,
                    user_id = preferences.userId
                )
            }
        }
    }

    override suspend fun deletePreferences(userId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            database.userQueries.deletePreferences(userId)
        }
    }
}

private fun Notification_preferences.toNotificationPreferences(): NotificationPreferences {
    val enabledTypesSet = parseEnabledNotificationTypes(
        enabledTypesJson = enabled_types,
        userId = user_id
    )

    return NotificationPreferences(
        userId = user_id,
        enabledTypes = enabledTypesSet,
        quietHoursStart = quiet_hours_start?.let(QuietTime::fromString),
        quietHoursEnd = quiet_hours_end?.let(QuietTime::fromString),
        soundEnabled = sound_enabled != 0L,
        vibrationEnabled = vibration_enabled != 0L,
        updatedAt = Instant.fromEpochMilliseconds(updated_at)
    )
}

private fun parseEnabledNotificationTypes(enabledTypesJson: String?, userId: String): Set<NotificationType> {
    val defaultTypes = defaultNotificationPreferences(userId).enabledTypes
    val rawTypes = enabledTypesJson?.let { json ->
        runCatching { Json.decodeFromString<List<String>>(json) }.getOrNull()
    } ?: return defaultTypes

    val parsedTypes = rawTypes
        .mapNotNull { raw -> runCatching { NotificationType.valueOf(raw) }.getOrNull() }
        .toSet()

    return if (rawTypes.isNotEmpty() && parsedTypes.isEmpty()) {
        defaultTypes
    } else {
        parsedTypes
    }
}
