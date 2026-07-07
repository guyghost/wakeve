package com.guyghost.wakeve.notification

import com.guyghost.wakeve.Notification
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.deeplink.DeepLinkFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val richNotificationJson = Json {
    ignoreUnknownKeys = true
}

internal object RichNotificationDeepLinks {
    fun event(eventId: String): String =
        DeepLinkFactory.createEventDetailsLink(eventId).fullUri

    fun poll(eventId: String): String =
        DeepLinkFactory.createPollVoteLink(eventId).fullUri

    fun meeting(meetingId: String): String =
        DeepLinkFactory.createNotificationsListLink(filter = "unread").fullUri

    fun meeting(eventId: String, meetingId: String): String =
        DeepLinkFactory.createMeetingJoinLink(eventId = eventId, meetingId = meetingId).fullUri
}

internal object RichNotificationActionPolicy {
    fun trustedActionsFor(category: NotificationCategory): List<NotificationAction> = when (category) {
        NotificationCategory.MEETING_STARTING -> NotificationAction.meetingStartingActions()
        NotificationCategory.EVENT_INVITE,
        NotificationCategory.POLL_REMINDER,
        NotificationCategory.SCENARIO_VOTE,
        NotificationCategory.GENERAL -> emptyList()
    }
}

/**
 * Service for sending rich notifications with enhanced visual and interactive capabilities.
 * Supports images, custom sounds, vibration patterns, LED colors, actions, and deep links.
 *
 * ## Features
 * - Rich media support (images, icons)
 * - Custom sounds and vibration patterns
 * - LED color customization
 * - Actionable notifications with buttons
 * - Deep linking for in-app navigation
 * - Priority-based delivery
 *
 * ## Platform Support
 * - **Android**: Uses NotificationCompat.BigPictureStyle, custom sounds, vibration patterns, LED colors
 * - **iOS**: Uses UNNotificationAttachment for images, custom sounds, critical alerts for HIGH priority
 *
 * @property database SQLDelight database for persistence
 * @property preferencesRepository User notification preferences
 * @property fcmSender FCM sender for Android devices
 * @property apnsSender APNs sender for iOS devices
 */
class RichNotificationService(
    private val database: WakeveDb,
    private val preferencesRepository: NotificationPreferencesRepositoryInterface,
    private val fcmSender: RichFCMSender,
    private val apnsSender: RichAPNsSender
) {

    /**
     * Send a rich notification with all enhanced features.
     *
     * @param notification The rich notification to send
     * @param currentTime Current time for quiet hours checking (default: system clock)
     * @return Result with notification ID on success, or exception on failure
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun sendRichNotification(
        notification: RichNotification,
        currentTime: Instant = Clock.System.now()
    ): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            val normalizedNotification = normalizeRichNotificationForSend(notification).getOrThrow()

            // Check user preferences
            val preferences = preferencesRepository.getPreferences(normalizedNotification.userId)
                ?: defaultNotificationPreferences(normalizedNotification.userId)

            // Check if notification should be sent (respecting quiet hours)
            if (!shouldSendNotification(normalizedNotification, preferences, currentTime)) {
                throw IllegalStateException(
                    "Notification blocked by user preferences or quiet hours"
                )
            }

            val nowMs = Clock.System.now().toEpochMilliseconds()

            // Persist notification to database before transport delivery.
            persistNotification(normalizedNotification.id, normalizedNotification, nowMs)

            // Get user push tokens
            val tokens = database.notificationQueries.getTokensByUser(normalizedNotification.userId)
                .executeAsList()

            // Send to all user devices based on platform
            for (token in tokens) {
                when (token.platform.uppercase()) {
                    Platform.ANDROID.name -> {
                        runCatching {
                            fcmSender.sendRichNotification(
                                token = token.token,
                                notification = normalizedNotification
                            ).getOrThrow()
                        }
                    }
                    Platform.IOS.name -> {
                        runCatching {
                            apnsSender.sendRichNotification(
                                token = token.token,
                                notification = normalizedNotification
                            ).getOrThrow()
                        }
                    }
                }
            }

            normalizedNotification.id
        }
    }

    /**
     * Send an event invite notification with an image.
     * Tapping the notification opens the event flow; invite decisions stay in-app.
     *
     * @param eventId The event identifier
     * @param userId Target user identifier
     * @param eventTitle Event title for the notification
     * @param eventImageUrl Optional URL to event image
     * @param organizerName Name of the event organizer
     * @return Result with notification ID on success
     */
    suspend fun sendEventInviteWithImage(
        eventId: String,
        userId: String,
        eventTitle: String,
        eventImageUrl: String? = null,
        organizerName: String = ""
    ): Result<String> {
        val notification = richNotification {
            id(generateNotificationId())
            userId(userId)
            title("You're invited to $eventTitle!")
            body(if (organizerName.isNotBlank()) {
                "$organizerName invited you to join this event."
            } else {
                "You've been invited to join this event."
            })
            imageUrl(eventImageUrl)
            category(NotificationCategory.EVENT_INVITE)
            priority(RichNotificationPriority.HIGH)
            deepLink(RichNotificationDeepLinks.event(eventId))
            actions(RichNotificationActionPolicy.trustedActionsFor(NotificationCategory.EVENT_INVITE))
        }

        return sendRichNotification(notification)
    }

    /**
     * Send a poll reminder notification.
     * Tapping the notification opens the poll flow; vote mutations stay in-app.
     *
     * @param eventId The event identifier
     * @param userId Target user identifier
     * @param eventTitle Event title
     * @param deadlineHours Hours remaining until poll closes
     * @return Result with notification ID on success
     */
    suspend fun sendPollReminderWithActions(
        eventId: String,
        userId: String,
        eventTitle: String,
        deadlineHours: Int = 24
    ): Result<String> {
        val notification = richNotification {
            id(generateNotificationId())
            userId(userId)
            title("Poll closing soon: $eventTitle")
            body("You have $deadlineHours hours left to vote on the date.")
            category(NotificationCategory.POLL_REMINDER)
            priority(RichNotificationPriority.DEFAULT)
            deepLink(RichNotificationDeepLinks.poll(eventId))
            vibrationPattern(RichNotification.DEFAULT_VIBRATION_PATTERN)
            actions(RichNotificationActionPolicy.trustedActionsFor(NotificationCategory.POLL_REMINDER))
        }

        return sendRichNotification(notification)
    }

    /**
     * Send a meeting starting notification with a join button.
     * High priority with custom sound and vibration.
     *
     * @param meetingId The meeting identifier
     * @param userId Target user identifier
     * @param joinUrl URL for joining the meeting (Zoom, Meet, etc.)
     * @param meetingTitle Meeting title
     * @param startsInMinutes Minutes until meeting starts
     * @param eventId Optional event identifier for event-scoped meeting navigation
     * @return Result with notification ID on success
     */
    suspend fun sendMeetingStartingWithJoinButton(
        meetingId: String,
        userId: String,
        joinUrl: String,
        meetingTitle: String,
        startsInMinutes: Int = 15,
        eventId: String? = null
    ): Result<String> {
        val (title, body) = when {
            startsInMinutes <= 0 -> "Meeting started: $meetingTitle" to "Join now!"
            startsInMinutes == 1 -> "Meeting starts in 1 minute: $meetingTitle" to "Get ready to join!"
            else -> "Meeting starts in $startsInMinutes minutes: $meetingTitle" to "Don't be late!"
        }

        val notification = richNotification {
            id(generateNotificationId())
            userId(userId)
            title(title)
            body(body)
            category(NotificationCategory.MEETING_STARTING)
            priority(RichNotificationPriority.HIGH)
            deepLink(
                if (eventId.isNullOrBlank()) {
                    RichNotificationDeepLinks.meeting(meetingId)
                } else {
                    RichNotificationDeepLinks.meeting(eventId = eventId, meetingId = meetingId)
                }
            )
            customSound("meeting_start_alert")
            vibrationPattern(RichNotification.URGENT_VIBRATION_PATTERN)
            ledColor(RichNotification.URGENT_LED_COLOR)
            actions(RichNotificationActionPolicy.trustedActionsFor(NotificationCategory.MEETING_STARTING))
        }

        return sendRichNotification(notification)
    }

    /**
     * Send a scenario vote notification.
     * Tapping the notification opens the event flow; scenario votes stay in-app.
     *
     * @param eventId The event identifier
     * @param userId Target user identifier
     * @param scenarioDescription Description of the scenario to vote on
     * @param proposedBy Name of user who proposed the scenario
     * @return Result with notification ID on success
     */
    suspend fun sendScenarioVoteNotification(
        eventId: String,
        userId: String,
        scenarioDescription: String,
        proposedBy: String
    ): Result<String> {
        val notification = richNotification {
            id(generateNotificationId())
            userId(userId)
            title("New scenario proposed")
            body("$proposedBy proposed: $scenarioDescription")
            category(NotificationCategory.SCENARIO_VOTE)
            priority(RichNotificationPriority.DEFAULT)
            deepLink(RichNotificationDeepLinks.event(eventId))
            actions(RichNotificationActionPolicy.trustedActionsFor(NotificationCategory.SCENARIO_VOTE))
        }

        return sendRichNotification(notification)
    }

    /**
     * Get all rich notifications for a user.
     *
     * @param userId Target user identifier
     * @param limit Maximum number of notifications to return
     * @return List of rich notifications
     */
    suspend fun getRichNotifications(
        userId: String,
        limit: Int = 50
    ): List<RichNotification> = withContext(Dispatchers.Default) {
        val normalizedUserId = normalizeNotificationUserId(userId).getOrThrow()
        database.notificationQueries.getNotifications(
            user_id = normalizedUserId,
            value_ = normalizeNotificationHistoryLimit(limit).toLong()
        ).executeAsList()
            .map { it.toRichNotification() }
    }

    /**
     * Mark a notification as read.
     *
     * @param notificationId The notification identifier
     * @return Result success or failure
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val normalizedNotificationId = normalizeNotificationId(notificationId).getOrThrow()
            database.notificationQueries.markAsRead(
                read_at = Clock.System.now().toEpochMilliseconds(),
                id = normalizedNotificationId
            )
        }
    }

    /**
     * Mark a notification as read only if it belongs to the authenticated user.
     *
     * @param notificationId The notification identifier
     * @param userId Expected notification owner
     * @return Result success or failure
     */
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

    /**
     * Delete a notification.
     *
     * @param notificationId The notification identifier
     * @return Result success or failure
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            database.notificationQueries.deleteNotification(normalizeNotificationId(notificationId).getOrThrow())
        }
    }

    /**
     * Delete a notification only if it belongs to the authenticated user.
     *
     * @param notificationId The notification identifier
     * @param userId Expected notification owner
     * @return Result success or failure
     */
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

    // Private helper methods

    private fun shouldSendNotification(
        notification: RichNotification,
        preferences: NotificationPreferences,
        currentTime: Instant
    ): Boolean {
        return preferences.shouldSend(
            type = notification.category.toNotificationType(),
            currentTime = currentTime
        )
    }

    private fun persistNotification(
        notificationId: String,
        notification: RichNotification,
        timestamp: Long
    ) {
        // Serialize rich notification data
        val richData = richNotificationJson.encodeToString(
            RichNotificationData(
                imageUrl = notification.imageUrl,
                largeIcon = notification.largeIcon,
                actions = notification.actions,
                priority = notification.priority,
                category = notification.category,
                deepLink = notification.deepLink,
                customSound = notification.customSound,
                vibrationPattern = notification.vibrationPattern,
                ledColor = notification.ledColor
            )
        )

        database.notificationQueries.insertNotification(
            id = notificationId,
            user_id = notification.userId,
            type = notification.category.toNotificationType().name,
            title = notification.title,
            body = notification.body,
            data_ = richData,
            created_at = timestamp,
            sent_at = timestamp
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateNotificationId(): String = Uuid.random().toString()

    private fun Notification.toRichNotification(): RichNotification {
        val richData = data_?.let { rawData ->
            runCatching {
                richNotificationJson.decodeFromString<RichNotificationData>(rawData)
            }.getOrNull()
        }

        return RichNotification(
            id = id,
            userId = user_id,
            title = title,
            body = body,
            imageUrl = richData?.imageUrl,
            largeIcon = richData?.largeIcon,
            actions = richData?.actions ?: emptyList(),
            priority = richData?.priority ?: RichNotificationPriority.DEFAULT,
            category = richData?.category ?: type.toNotificationCategory(),
            deepLink = richData?.deepLink,
            customSound = richData?.customSound,
            vibrationPattern = richData?.vibrationPattern,
            ledColor = richData?.ledColor
        )
    }
}

private fun String.toNotificationCategory(): NotificationCategory {
    return runCatching { NotificationCategory.valueOf(this) }.getOrNull()
        ?: NotificationCategory.fromString(this)
        ?: NotificationCategory.GENERAL
}

internal fun NotificationCategory.toNotificationType(): NotificationType {
    return when (this) {
        NotificationCategory.EVENT_INVITE -> NotificationType.EVENT_INVITE
        NotificationCategory.POLL_REMINDER -> NotificationType.VOTE_REMINDER
        NotificationCategory.MEETING_STARTING -> NotificationType.MEETING_REMINDER
        NotificationCategory.SCENARIO_VOTE -> NotificationType.NEW_SCENARIO
        NotificationCategory.GENERAL -> NotificationType.EVENT_UPDATE
    }
}

@OptIn(ExperimentalUuidApi::class)
internal fun normalizeRichNotificationForSend(
    notification: RichNotification,
    fallbackId: String = Uuid.random().toString()
): Result<RichNotification> {
    val normalizedId = notification.id.trim().ifBlank { fallbackId.trim() }
    val normalizedNotification = notification.copy(
        id = normalizedId,
        userId = notification.userId.trim(),
        title = notification.title.trim(),
        body = notification.body.trim()
    )

    return normalizedNotification.validate()?.let { error ->
        Result.failure(IllegalArgumentException("Invalid notification: $error"))
    } ?: Result.success(normalizedNotification)
}

/**
 * Serializable data class for persisting rich notification extras.
 */
@Serializable
private data class RichNotificationData(
    val imageUrl: String? = null,
    val largeIcon: String? = null,
    val actions: List<NotificationAction> = emptyList(),
    val priority: RichNotificationPriority = RichNotificationPriority.DEFAULT,
    val category: NotificationCategory? = null,
    val deepLink: String? = null,
    val customSound: String? = null,
    val vibrationPattern: List<Int>? = null,
    val ledColor: Int? = null
)

/**
 * Extended FCM sender interface for rich notifications.
 */
interface RichFCMSender {
    suspend fun sendRichNotification(
        token: String,
        notification: RichNotification
    ): Result<Unit>
}

/**
 * Extended APNs sender interface for rich notifications.
 */
interface RichAPNsSender {
    suspend fun sendRichNotification(
        token: String,
        notification: RichNotification
    ): Result<Unit>
}

object NoConfiguredRichFCMSender : RichFCMSender {
    override suspend fun sendRichNotification(
        token: String,
        notification: RichNotification
    ): Result<Unit> = Result.failure(IllegalStateException("Rich FCM sender is not configured"))
}

object NoConfiguredRichAPNsSender : RichAPNsSender {
    override suspend fun sendRichNotification(
        token: String,
        notification: RichNotification
    ): Result<Unit> = Result.failure(IllegalStateException("Rich APNs sender is not configured"))
}

@Deprecated(
    message = "Use NoConfiguredRichFCMSender in production or a deterministic test sender in tests.",
    replaceWith = ReplaceWith("NoConfiguredRichFCMSender")
)
class MockRichFCMSender : RichFCMSender {
    override suspend fun sendRichNotification(
        token: String,
        notification: RichNotification
    ): Result<Unit> = NoConfiguredRichFCMSender.sendRichNotification(token, notification)
}

@Deprecated(
    message = "Use NoConfiguredRichAPNsSender in production or a deterministic test sender in tests.",
    replaceWith = ReplaceWith("NoConfiguredRichAPNsSender")
)
class MockRichAPNsSender : RichAPNsSender {
    override suspend fun sendRichNotification(
        token: String,
        notification: RichNotification
    ): Result<Unit> = NoConfiguredRichAPNsSender.sendRichNotification(token, notification)
}
