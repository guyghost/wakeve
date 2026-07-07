package com.guyghost.wakeve.notification

import kotlinx.datetime.Instant

/**
 * JVM implementation of NotificationScheduler.
 *
 * Native scheduled notifications are not available on plain JVM, so scheduling
 * fails explicitly instead of reporting a reminder that will never fire.
 */
actual class NotificationScheduler {

    /**
     * JVM has no native notification backend.
     */
    actual suspend fun scheduleEventReminder(
        eventId: String,
        title: String,
        body: String,
        scheduledTime: Instant
    ): Result<Unit> {
        return unavailable()
    }

    /**
     * JVM has no native notification backend.
     */
    actual suspend fun scheduleEventReminderWithId(
        notificationId: String,
        eventId: String,
        title: String,
        body: String,
        scheduledTime: Instant
    ): Result<String> {
        return unavailableWithId()
    }

    /**
     * JVM has no native notification backend.
     */
    actual suspend fun schedulePollDeadlineReminder(
        pollId: String,
        eventId: String,
        title: String,
        body: String,
        deadlineTime: Instant
    ): Result<Unit> {
        return unavailable()
    }

    /**
     * No-op implementation for JVM platform.
     */
    actual suspend fun cancelScheduledNotification(notificationId: String): Result<Unit> {
        // No-op for JVM testing
        return Result.success(Unit)
    }

    /**
     * No-op implementation for JVM platform.
     */
    actual suspend fun cancelAllScheduledNotifications(): Result<Unit> {
        // No-op for JVM testing
        return Result.success(Unit)
    }

    actual companion object {
        
        /**
         * Returns a singleton instance for JVM platform.
         */
        actual fun getInstance(): NotificationScheduler {
            return InstanceHolder.instance
        }

        /**
         * No-op initialization for JVM platform.
         */
        actual fun initialize(context: Any?) {
            // No initialization needed for JVM testing
        }
    }

    /**
     * Singleton holder for lazy initialization.
     */
    private object InstanceHolder {
        val instance: NotificationScheduler = NotificationScheduler()
    }

    private fun unavailable(): Result<Unit> {
        return Result.failure(IllegalStateException("Notification scheduling is not available on JVM"))
    }

    private fun unavailableWithId(): Result<String> {
        return Result.failure(IllegalStateException("Notification scheduling is not available on JVM"))
    }
}
