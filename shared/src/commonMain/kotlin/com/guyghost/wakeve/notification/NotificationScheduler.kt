package com.guyghost.wakeve.notification

import kotlinx.datetime.Instant

/**
 * Cross-platform notification scheduler.
 *
 * This service schedules local notifications to be shown at specific times:
 * - Event reminders (before an event starts)
 * - Poll deadline reminders (before voting ends)
 *
 * ## Platform Implementation
 * - **Android**: Uses WorkManager with OneTimeWorkRequest
 * - **iOS**: Uses UNNotificationRequest with UNUserNotificationCenter
 *
 * ## Functional Core & Imperative Shell Pattern
 * - **Shell**: This service handles platform-specific I/O and scheduling
 *
 * @see NotificationScheduler.android.kt for Android implementation
 * @see NotificationScheduler.ios.kt for iOS implementation
 */
expect class NotificationScheduler {

    /**
     * Schedule a reminder for an upcoming event.
     *
     * The notification will be shown at the specified time with the title and body
     * identifying the event.
     *
     * @param eventId Unique identifier of the event
     * @param title Notification title (e.g., "Event Starting Soon")
     * @param body Notification body (e.g., "Your event 'Birthday Party' starts in 1 hour")
     * @param scheduledTime When to show the notification (UTC timestamp)
     * @return Result with Unit on success, error on failure
     */
    suspend fun scheduleEventReminder(
        eventId: String,
        title: String,
        body: String,
        scheduledTime: Instant
    ): Result<Unit>

    /**
     * Schedule a reminder for a poll deadline.
     *
     * The notification will be shown at the specified time to remind users
     * to cast their votes before the deadline.
     *
     * @param pollId Unique identifier of the poll
     * @param eventId Associated event ID (for navigation)
     * @param title Notification title (e.g., "Poll Closing Soon")
     * @param body Notification body (e.g., "Cast your vote before the deadline")
     * @param deadlineTime When the poll closes (UTC timestamp)
     * @return Result with Unit on success, error on failure
     */
    suspend fun schedulePollDeadlineReminder(
        pollId: String,
        eventId: String,
        title: String,
        body: String,
        deadlineTime: Instant
    ): Result<Unit>

    /**
     * Cancel a scheduled notification.
     *
     * Removes a previously scheduled notification before it is shown.
     *
     * @param notificationId Unique identifier of the notification to cancel
     * @return Result with Unit on success, error on failure
     */
    suspend fun cancelScheduledNotification(notificationId: String): Result<Unit>

    /**
     * Cancel all scheduled notifications.
     *
     * Removes all pending scheduled notifications.
     * Useful when user logs out or deletes an event.
     *
     * @return Result with Unit on success, error on failure
     */
    suspend fun cancelAllScheduledNotifications(): Result<Unit>

    companion object {
        /**
         * Returns NotificationScheduler singleton instance.
         *
         * The instance is platform-specific and initialized on first access.
         *
         * @return The singleton [NotificationScheduler] instance
         */
        fun getInstance(): NotificationScheduler

        /**
         * Initialize the NotificationScheduler with platform-specific context.
         *
         * Required on Android before calling [getInstance].
         * No-op on iOS.
         *
         * @param context Platform-specific context (Android Context or null)
         */
        fun initialize(context: Any?)
    }
}
