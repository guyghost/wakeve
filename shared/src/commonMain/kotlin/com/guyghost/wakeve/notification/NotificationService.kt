package com.guyghost.wakeve.notification

import com.guyghost.wakeve.Notification
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.NotificationMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Notification service for cross-platform push notifications.
 * Handles token registration, notification sending, and history management.
 */
class NotificationService(
    private val database: WakeveDb,
    private val preferencesRepository: NotificationPreferencesRepositoryInterface,
    private val fcmSender: FCMSender,
    private val apnsSender: APNsSender
) {
    suspend fun registerPushToken(
        userId: String,
        platform: Platform,
        token: String
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            database.notificationQueries.upsertToken(
                user_id = userId,
                platform = platform.name,
                token = token,
                updated_at = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    suspend fun unregisterPushToken(
        userId: String,
        platform: Platform
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            database.notificationQueries.deleteToken(
                user_id = userId,
                platform = platform.name
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun sendNotification(
        request: NotificationRequest,
        currentTime: Instant = Clock.System.now()
    ): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            val preferences = preferencesRepository.getPreferences(request.userId)
                ?: defaultNotificationPreferences(request.userId)

            if (!preferences.shouldSend(request.type, currentTime)) {
                error("Notification type ${request.type.name} is disabled or in quiet hours")
            }

            val tokens = database.notificationQueries.getTokensByUser(request.userId).executeAsList()
            if (tokens.isEmpty()) {
                error("No push tokens registered for user ${request.userId}")
            }

            val notificationId = Uuid.random().toString()
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val payload = request.data + ("notificationId" to notificationId)

            for (token in tokens) {
                when (token.platform.uppercase()) {
                    Platform.ANDROID.name -> {
                        fcmSender.sendNotification(token.token, request.title, request.body, payload).getOrThrow()
                    }
                    Platform.IOS.name -> {
                        apnsSender.sendNotification(token.token, request.title, request.body, payload).getOrThrow()
                    }
                }
            }

            database.notificationQueries.insertNotification(
                id = notificationId,
                user_id = request.userId,
                type = request.type.name,
                title = request.title,
                body = request.body,
                data_ = Json.encodeToString(payload),
                created_at = nowMs,
                sent_at = nowMs
            )

            notificationId
        }
    }

    suspend fun getUnreadNotifications(userId: String): List<NotificationMessage> = withContext(Dispatchers.Default) {
        database.notificationQueries.getUnreadNotifications(user_id = userId).executeAsList()
            .map(Notification::toNotificationMessage)
    }

    suspend fun getNotifications(userId: String, limit: Int = 50): List<NotificationMessage> = withContext(Dispatchers.Default) {
        database.notificationQueries.getNotifications(user_id = userId, value_ = limit.toLong()).executeAsList()
            .map(Notification::toNotificationMessage)
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            database.notificationQueries.markAsRead(
                read_at = Clock.System.now().toEpochMilliseconds(),
                id = notificationId
            )
        }
    }

    suspend fun markAllAsRead(userId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            database.notificationQueries.markAllAsRead(
                read_at = Clock.System.now().toEpochMilliseconds(),
                user_id = userId
            )
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            database.notificationQueries.deleteNotification(notificationId)
        }
    }

    suspend fun getPreferences(userId: String): NotificationPreferences? =
        preferencesRepository.getPreferences(userId)

    suspend fun updatePreferences(preferences: NotificationPreferences): Result<Unit> =
        preferencesRepository.savePreferences(preferences)
}

private fun Notification.toNotificationMessage(): NotificationMessage {
    val dataMap = runCatching {
        data_?.let { Json.decodeFromString<Map<String, String>>(it) }.orEmpty()
    }.getOrDefault(emptyMap())

    return NotificationMessage(
        id = id,
        userId = user_id,
        type = type.toModelNotificationType(),
        title = title,
        body = body,
        data = dataMap,
        sentAt = sent_at?.let { Instant.fromEpochMilliseconds(it).toString() },
        readAt = read_at?.let { Instant.fromEpochMilliseconds(it).toString() }
    )
}

private fun String.toModelNotificationType(): com.guyghost.wakeve.models.NotificationType {
    return when (runCatching { NotificationType.valueOf(this) }.getOrNull()) {
        NotificationType.DEADLINE_REMINDER -> com.guyghost.wakeve.models.NotificationType.DEADLINE_REMINDER
        NotificationType.EVENT_UPDATE -> com.guyghost.wakeve.models.NotificationType.EVENT_UPDATE
        NotificationType.VOTE_CLOSE_REMINDER,
        NotificationType.VOTE_REMINDER -> com.guyghost.wakeve.models.NotificationType.VOTE_CLOSE_REMINDER
        NotificationType.DATE_CONFIRMED -> com.guyghost.wakeve.models.NotificationType.EVENT_CONFIRMED
        NotificationType.MENTION -> com.guyghost.wakeve.models.NotificationType.MENTION
        NotificationType.COMMENT_REPLY -> com.guyghost.wakeve.models.NotificationType.COMMENT_REPLY
        NotificationType.NEW_COMMENT -> com.guyghost.wakeve.models.NotificationType.COMMENT_POSTED
        NotificationType.EVENT_INVITE,
        NotificationType.NEW_SCENARIO,
        NotificationType.SCENARIO_SELECTED,
        NotificationType.MEETING_REMINDER,
        NotificationType.PAYMENT_DUE,
        null -> com.guyghost.wakeve.models.NotificationType.EVENT_UPDATE
    }
}

interface FCMSender {
    suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit>
}

interface APNsSender {
    suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit>
}

class MockFCMSender : FCMSender {
    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> = Result.success(Unit)
}

class MockAPNsSender : APNsSender {
    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> = Result.success(Unit)
}
