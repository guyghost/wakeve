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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
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
            val normalizedUserId = normalizeNotificationUserId(userId).getOrThrow()
            val normalizedToken = normalizePushToken(token).getOrThrow()

            database.notificationQueries.upsertToken(
                user_id = normalizedUserId,
                platform = platform.name,
                token = normalizedToken,
                updated_at = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    suspend fun unregisterPushToken(
        userId: String,
        platform: Platform
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val normalizedUserId = normalizeNotificationUserId(userId).getOrThrow()

            database.notificationQueries.deleteToken(
                user_id = normalizedUserId,
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
            val normalizedRequest = normalizeNotificationRequest(request).getOrThrow()
            val preferences = preferencesRepository.getPreferences(normalizedRequest.userId)
                ?: defaultNotificationPreferences(normalizedRequest.userId)

            if (!preferences.shouldSend(normalizedRequest.type, currentTime)) {
                error("Notification type ${normalizedRequest.type.name} is disabled or in quiet hours")
            }

            val notificationId = Uuid.random().toString()
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val payload = normalizedRequest.data + ("notificationId" to notificationId)

            database.notificationQueries.insertNotification(
                id = notificationId,
                user_id = normalizedRequest.userId,
                type = normalizedRequest.type.name,
                title = normalizedRequest.title,
                body = normalizedRequest.body,
                data_ = Json.encodeToString(payload),
                created_at = nowMs,
                sent_at = nowMs
            )

            val tokens = database.notificationQueries.getTokensByUser(normalizedRequest.userId).executeAsList()
            for (token in tokens) {
                when (token.platform.uppercase()) {
                    Platform.ANDROID.name -> {
                        runCatching {
                            fcmSender
                                .sendNotification(token.token, normalizedRequest.title, normalizedRequest.body, payload)
                                .getOrThrow()
                        }
                    }
                    Platform.IOS.name -> {
                        runCatching {
                            apnsSender
                                .sendNotification(token.token, normalizedRequest.title, normalizedRequest.body, payload)
                                .getOrThrow()
                        }
                    }
                }
            }

            notificationId
        }
    }

    suspend fun getUnreadNotifications(
        userId: String,
        limit: Int = 50
    ): List<NotificationMessage> = withContext(Dispatchers.Default) {
        database.notificationQueries
            .getUnreadNotificationsLimited(
                user_id = normalizeNotificationUserId(userId).getOrThrow(),
                value_ = normalizeNotificationHistoryLimit(limit).toLong()
            )
            .executeAsList()
            .map(Notification::toNotificationMessage)
    }

    suspend fun getNotifications(userId: String, limit: Int = 50): List<NotificationMessage> = withContext(Dispatchers.Default) {
        database.notificationQueries
            .getNotifications(
                user_id = normalizeNotificationUserId(userId).getOrThrow(),
                value_ = normalizeNotificationHistoryLimit(limit).toLong()
            )
            .executeAsList()
            .map(Notification::toNotificationMessage)
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val normalizedNotificationId = normalizeNotificationId(notificationId).getOrThrow()
            database.notificationQueries.markAsRead(
                read_at = Clock.System.now().toEpochMilliseconds(),
                id = normalizedNotificationId
            )
        }
    }

    suspend fun markAsReadForUser(
        notificationId: String,
        userId: String
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val normalizedNotificationId = normalizeNotificationId(notificationId).getOrThrow()
            val normalizedUserId = normalizeNotificationUserId(userId).getOrThrow()
            val notification = database.notificationQueries
                .getNotificationById(normalizedNotificationId)
                .executeAsOneOrNull()

            if (notification?.user_id != normalizedUserId) {
                error("Notification not found for authenticated user")
            }

            database.notificationQueries.markAsReadForUser(
                read_at = Clock.System.now().toEpochMilliseconds(),
                id = normalizedNotificationId,
                user_id = normalizedUserId
            )
        }
    }

    suspend fun markAllAsRead(userId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val normalizedUserId = normalizeNotificationUserId(userId).getOrThrow()
            database.notificationQueries.markAllAsRead(
                read_at = Clock.System.now().toEpochMilliseconds(),
                user_id = normalizedUserId
            )
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            database.notificationQueries.deleteNotification(normalizeNotificationId(notificationId).getOrThrow())
        }
    }

    suspend fun deleteNotificationForUser(
        notificationId: String,
        userId: String
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val normalizedNotificationId = normalizeNotificationId(notificationId).getOrThrow()
            val normalizedUserId = normalizeNotificationUserId(userId).getOrThrow()
            val notification = database.notificationQueries
                .getNotificationById(normalizedNotificationId)
                .executeAsOneOrNull()

            if (notification?.user_id != normalizedUserId) {
                error("Notification not found for authenticated user")
            }

            database.notificationQueries.deleteNotificationForUser(
                id = normalizedNotificationId,
                user_id = normalizedUserId
            )
        }
    }

    suspend fun getPreferences(userId: String): NotificationPreferences? =
        preferencesRepository.getPreferences(userId)

    suspend fun updatePreferences(preferences: NotificationPreferences): Result<Unit> =
        preferencesRepository.savePreferences(preferences)
}

internal fun normalizeNotificationRequest(request: NotificationRequest): Result<NotificationRequest> {
    val normalizedUserId = normalizeNotificationUserId(request.userId).getOrElse { return Result.failure(it) }

    val normalizedTitle = request.title.trim()
    if (normalizedTitle.isBlank()) {
        return Result.failure(IllegalArgumentException("Notification title must not be blank"))
    }

    val normalizedBody = request.body.trim()
    if (normalizedBody.isBlank()) {
        return Result.failure(IllegalArgumentException("Notification body must not be blank"))
    }

    return Result.success(
        request.copy(
            userId = normalizedUserId,
            title = normalizedTitle,
            body = normalizedBody
        )
    )
}

internal fun normalizeNotificationUserId(userId: String): Result<String> {
    val normalizedUserId = userId.trim()
    return if (normalizedUserId.isBlank()) {
        Result.failure(IllegalArgumentException("Notification userId must not be blank"))
    } else {
        Result.success(normalizedUserId)
    }
}

internal fun normalizeNotificationId(notificationId: String): Result<String> {
    val normalizedNotificationId = notificationId.trim()
    return if (normalizedNotificationId.isBlank()) {
        Result.failure(IllegalArgumentException("Notification id must not be blank"))
    } else {
        Result.success(normalizedNotificationId)
    }
}

internal fun normalizePushToken(token: String): Result<String> {
    val normalizedToken = token.trim()
    return if (normalizedToken.isBlank()) {
        Result.failure(IllegalArgumentException("Push token must not be blank"))
    } else {
        Result.success(normalizedToken)
    }
}

internal fun normalizeNotificationHistoryLimit(limit: Int): Int =
    limit.coerceIn(MIN_NOTIFICATION_HISTORY_LIMIT, MAX_NOTIFICATION_HISTORY_LIMIT)

private const val MIN_NOTIFICATION_HISTORY_LIMIT = 1
private const val MAX_NOTIFICATION_HISTORY_LIMIT = 100

private fun Notification.toNotificationMessage(): NotificationMessage {
    return NotificationMessage(
        id = id,
        userId = user_id,
        type = type.toModelNotificationType(),
        title = title,
        body = body,
        data = decodeNotificationDataMap(data_),
        sentAt = sent_at?.let { Instant.fromEpochMilliseconds(it).toString() },
        readAt = read_at?.let { Instant.fromEpochMilliseconds(it).toString() }
    )
}

internal fun decodeNotificationDataMap(rawData: String?): Map<String, String> {
    val normalizedData = rawData?.trim().orEmpty()
    if (normalizedData.isBlank()) {
        return emptyMap()
    }

    val jsonObject = runCatching {
        Json.parseToJsonElement(normalizedData)
    }.getOrNull() as? JsonObject ?: return emptyMap()

    return jsonObject
        .mapNotNull { (key, value) ->
            val primitive = value as? JsonPrimitive ?: return@mapNotNull null
            primitive.contentOrNull?.let { key to it }
        }
        .toMap()
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

object NoConfiguredFCMSender : FCMSender {
    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> = Result.failure(IllegalStateException("FCM sender is not configured"))
}

object NoConfiguredAPNsSender : APNsSender {
    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> = Result.failure(IllegalStateException("APNs sender is not configured"))
}

@Deprecated(
    message = "Use NoConfiguredFCMSender in production or a deterministic test sender in tests.",
    replaceWith = ReplaceWith("NoConfiguredFCMSender")
)
class MockFCMSender : FCMSender {
    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> = NoConfiguredFCMSender.sendNotification(token, title, body, data)
}

@Deprecated(
    message = "Use NoConfiguredAPNsSender in production or a deterministic test sender in tests.",
    replaceWith = ReplaceWith("NoConfiguredAPNsSender")
)
class MockAPNsSender : APNsSender {
    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> = NoConfiguredAPNsSender.sendNotification(token, title, body, data)
}
