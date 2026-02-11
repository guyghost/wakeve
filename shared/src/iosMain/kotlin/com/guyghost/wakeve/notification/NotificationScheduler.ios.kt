package com.guyghost.wakeve.notification

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import platform.Foundation.NSDate
import platform.Foundation.NSTimeInterval
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNAuthorizationOption
import platform.UserNotifications.UNAuthorizationOptions
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS implementation of NotificationScheduler.
 *
 * Uses UNUserNotificationCenter with UNNotificationRequest for scheduling.
 * Notifications are shown using iOS's native notification system.
 *
 * ## Platform Specifics
 * - **Scheduling**: UNTimeIntervalNotificationTrigger or UNCalendarNotificationTrigger
 * - **Delivery**: UNMutableNotificationContent with userInfo for navigation
 * - **Persistence**: Notifications persist across app restarts
 * - **Permissions**: User must grant notification permission first
 *
 * ## Permissions
 * - UNAuthorizationOption.alert: Show alert banners
 * - UNAuthorizationOption.sound: Play sounds
 * - UNAuthorizationOption.badge: Update app badge count
 *
 * @note This is a Kotlin/Native implementation that bridges to iOS UNUserNotificationCenter.
 * In production, you may want to handle this from SwiftUI with proper error handling.
 */
actual class NotificationScheduler {

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    companion object {
        private const val CATEGORY_EVENT = "EVENT_CATEGORY"
        private const val CATEGORY_POLL = "POLL_CATEGORY"
        private const val ACTION_VIEW = "VIEW_ACTION"

        // UserInfo keys
        private const val KEY_EVENT_ID = "event_id"
        private const val KEY_POLL_ID = "poll_id"
        private const val KEY_NOTIFICATION_TYPE = "notification_type"

        private var instance: NotificationScheduler? = null

        /**
         * Initialize singleton. No-op on iOS.
         */
        actual fun initialize(context: Any?) {
            // No initialization needed on iOS
        }

        /**
         * Returns singleton NotificationScheduler instance.
         */
        actual fun getInstance(): NotificationScheduler {
            return instance ?: NotificationScheduler().also { instance = it }
        }
    }

    /**
     * Schedule event reminder using UNNotificationRequest.
     *
     * Calculates time interval from now to scheduled time and creates
     * a notification with UNTimeIntervalNotificationTrigger.
     */
    actual suspend fun scheduleEventReminder(
        eventId: String,
        title: String,
        body: String,
        scheduledTime: Instant
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val notificationId = generateNotificationId("event", eventId)
            val interval = calculateTimeInterval(scheduledTime)

            if (interval <= 0) {
                // Time already passed
                return@runCatching
            }

            val content = createNotificationContent(
                id = notificationId,
                title = title,
                body = body,
                userInfo = mapOf(
                    KEY_EVENT_ID to eventId,
                    KEY_NOTIFICATION_TYPE to "event"
                )
            )

            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                interval,
                repeats = false
            )

            val request = UNNotificationRequest.requestWithIdentifier(
                notificationId,
                content,
                trigger
            )

            notificationCenter.addNotificationRequest(request) { error ->
                if (error != null) {
                    throw IllegalStateException("Failed to schedule notification: ${error.localizedDescription}")
                }
            }

            Result.success(Unit)
        }
    }

    /**
     * Schedule poll deadline reminder using UNNotificationRequest.
     *
     * Calculates time interval from now to deadline and creates
     * a notification with UNTimeIntervalNotificationTrigger.
     */
    actual suspend fun schedulePollDeadlineReminder(
        pollId: String,
        eventId: String,
        title: String,
        body: String,
        deadlineTime: Instant
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val notificationId = generateNotificationId("poll", pollId)
            val interval = calculateTimeInterval(deadlineTime)

            if (interval <= 0) {
                // Deadline already passed
                return@runCatching
            }

            val content = createNotificationContent(
                id = notificationId,
                title = title,
                body = body,
                userInfo = mapOf(
                    KEY_POLL_ID to pollId,
                    KEY_EVENT_ID to eventId,
                    KEY_NOTIFICATION_TYPE to "poll"
                )
            )

            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                interval,
                repeats = false
            )

            val request = UNNotificationRequest.requestWithIdentifier(
                notificationId,
                content,
                trigger
            )

            notificationCenter.addNotificationRequest(request) { error ->
                if (error != null) {
                    throw IllegalStateException("Failed to schedule notification: ${error.localizedDescription}")
                }
            }

            Result.success(Unit)
        }
    }

    /**
     * Cancel a scheduled notification by identifier.
     */
    actual suspend fun cancelScheduledNotification(notificationId: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                notificationCenter.removePendingNotificationRequestsWithIdentifiers(
                    listOf(notificationId)
                )
                // Also remove any delivered notification
                notificationCenter.removeDeliveredNotificationsWithIdentifiers(
                    listOf(notificationId)
                )
                Result.success(Unit)
            }
        }

    /**
     * Cancel all scheduled notifications.
     */
    actual suspend fun cancelAllScheduledNotifications(): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                notificationCenter.removeAllPendingNotificationRequests()
                notificationCenter.removeAllDeliveredNotifications()
                Result.success(Unit)
            }
        }

    /**
     * Calculate time interval in seconds from now to target time.
     */
    private fun calculateTimeInterval(targetTime: Instant): NSTimeInterval {
        val now = platform.Foundation.NSDate()
        val targetDate = NSDate(timeIntervalSince1970 = targetTime.epochSeconds)
        return targetDate.timeIntervalSince1970 - now.timeIntervalSince1970
    }

    /**
     * Generate unique notification ID.
     */
    private fun generateNotificationId(type: String, id: String): String {
        return "$type-$id"
    }

    /**
     * Create notification content with user info.
     */
    private fun createNotificationContent(
        id: String,
        title: String,
        body: String,
        userInfo: Map<String, Any>
    ): UNMutableNotificationContent {
        return UNMutableNotificationContent().apply {
            this.title = title
            this.body = body
            this.userInfo = userInfo
            this.sound = UNNotificationSound.default
            // Category for grouping notifications
            this.categoryIdentifier = when {
                id.startsWith("event") -> CATEGORY_EVENT
                id.startsWith("poll") -> CATEGORY_POLL
                else -> "DEFAULT"
            }
        }
    }
}
