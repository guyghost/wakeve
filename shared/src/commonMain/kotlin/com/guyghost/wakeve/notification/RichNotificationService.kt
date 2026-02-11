package com.guyghost.wakeve.notification

import com.guyghost.wakeve.Notification
import com.guyghost.wakeve.database.WakevDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    private val database: WakevDb,
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
            // Validate notification
            notification.validate()?.let { error ->
                throw IllegalArgumentException("Invalid notification: $error")
            }

            // Check user preferences
            val preferences = preferencesRepository.getPreferences(notification.userId)
                ?: defaultNotificationPreferences(notification.userId)

            // Check if notification should be sent (respecting quiet hours)
            if (!shouldSendNotification(notification, preferences, currentTime)) {
                throw IllegalStateException(
                    "Notification blocked by user preferences or quiet hours"
                )
            }

            // Get user push tokens
            val tokens = database.notificationQueries.getTokensByUser(notification.userId)
                .executeAsList()

            if (tokens.isEmpty()) {
                throw IllegalStateException("No push tokens registered for user ${notification.userId}")
            }

            // Generate notification ID if not provided
            val notificationId = notification.id.takeIf { it.isNotBlank() }
                ?: Uuid.random().toString()

            val nowMs = Clock.System.now().toEpochMilliseconds()

            // Send to all user devices based on platform
            for (token in tokens) {
                when (token.platform.uppercase()) {
                    Platform.ANDROID.name -> {
                        fcmSender.sendRichNotification(
                            token = token.token,
                            notification = notification.copy(id = notificationId)
                        ).getOrThrow()
                    }
                    Platform.IOS.name -> {
                        apnsSender.sendRichNotification(
                            token = token.token,
                            notification = notification.copy(id = notificationId)
                        ).getOrThrow()
                    }
                }
            }

            // Persist notification to database
            persistNotification(notificationId, notification, nowMs)

            notificationId
        }
    }

    /**
     * Send an event invite notification with an image.
     * Includes accept/decline/maybe actions.
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
            deepLink("wakeve://events/$eventId/invite")
            withDefaultActions()
        }

        return sendRichNotification(notification)
    }

    /**
     * Send a poll reminder notification with action buttons.
     * Includes vote and view actions.
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
            deepLink("wakeve://events/$eventId/poll")
            vibrationPattern(RichNotification.DEFAULT_VIBRATION_PATTERN)
            withDefaultActions()
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
     * @return Result with notification ID on success
     */
    suspend fun sendMeetingStartingWithJoinButton(
        meetingId: String,
        userId: String,
        joinUrl: String,
        meetingTitle: String,
        startsInMinutes: Int = 15
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
            deepLink("wakeve://meetings/$meetingId/join")
            customSound("meeting_start_alert")
            vibrationPattern(RichNotification.URGENT_VIBRATION_PATTERN)
            ledColor(RichNotification.URGENT_LED_COLOR)
            actions(NotificationAction.meetingStartingActions())
        }

        // Store join URL in notification data for action handling
        return sendRichNotification(notification.copy(
            actions = listOf(
                NotificationAction("join", "Join Now", ActionType.JOIN_MEETING),
                NotificationAction("snooze", "Snooze", ActionType.VOTE_MAYBE)
            )
        ))
    }

    /**
     * Send a scenario vote notification with yes/no actions.
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
            deepLink("wakeve://events/$eventId/scenarios")
            withDefaultActions()
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
        database.notificationQueries.getNotifications(
            user_id = userId,
            value_ = limit.toLong()
        ).executeAsList()
            .mapNotNull { it.toRichNotification() }
    }

    /**
     * Mark a notification as read.
     *
     * @param notificationId The notification identifier
     * @return Result success or failure
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            database.notificationQueries.markAsRead(
                read_at = Clock.System.now().toEpochMilliseconds(),
                id = notificationId
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
            database.notificationQueries.deleteNotification(notificationId)
        }
    }

    // Private helper methods

    private fun shouldSendNotification(
        notification: RichNotification,
        preferences: NotificationPreferences,
        currentTime: Instant
    ): Boolean {
        // HIGH priority notifications can bypass quiet hours
        if (notification.priority == RichNotificationPriority.HIGH) return true

        // Check quiet hours for non-high priority
        val start = preferences.quietHoursStart
        val end = preferences.quietHoursEnd

        if (start == null || end == null) return true

        // Convert to minutes since midnight
        val currentMinutes = currentTime.toEpochMilliseconds() / 60000 % 1440
        val startMinutes = start.hour * 60 + start.minute
        val endMinutes = end.hour * 60 + end.minute

        // Handle overnight quiet hours
        val inQuietHours = if (startMinutes > endMinutes) {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            currentMinutes in startMinutes..<endMinutes
        }

        return !inQuietHours
    }

    private fun persistNotification(
        notificationId: String,
        notification: RichNotification,
        timestamp: Long
    ) {
        // Serialize rich notification data
        val richData = Json.encodeToString(
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
            type = notification.category.name,
            title = notification.title,
            body = notification.body,
            data_ = richData,
            created_at = timestamp,
            sent_at = timestamp
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateNotificationId(): String = Uuid.random().toString()

    private fun Notification.toRichNotification(): RichNotification? {
        return runCatching {
            val richData = data_?.let {
                Json.decodeFromString<RichNotificationData>(it)
            }

            RichNotification(
                id = id,
                userId = user_id,
                title = title,
                body = body,
                imageUrl = richData?.imageUrl,
                largeIcon = richData?.largeIcon,
                actions = richData?.actions ?: emptyList(),
                priority = richData?.priority ?: RichNotificationPriority.DEFAULT,
                category = richData?.category ?: NotificationCategory.GENERAL,
                deepLink = richData?.deepLink,
                customSound = richData?.customSound,
                vibrationPattern = richData?.vibrationPattern,
                ledColor = richData?.ledColor
            )
        }.getOrNull()
    }
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
    val category: NotificationCategory = NotificationCategory.GENERAL,
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

/**
 * Mock implementation of RichFCMSender for testing.
 */
class MockRichFCMSender : RichFCMSender {
    override suspend fun sendRichNotification(
        token: String,
        notification: RichNotification
    ): Result<Unit> = Result.success(Unit)
}

/**
 * Mock implementation of RichAPNsSender for testing.
 */
class MockRichAPNsSender : RichAPNsSender {
    override suspend fun sendRichNotification(
        token: String,
        notification: RichNotification
    ): Result<Unit> = Result.success(Unit)
}
