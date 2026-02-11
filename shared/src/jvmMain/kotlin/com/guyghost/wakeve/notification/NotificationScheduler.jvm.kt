package com.guyghost.wakeve.notification

import kotlinx.datetime.Instant

/**
 * JVM implementation of NotificationScheduler for testing purposes.
 * 
 * This is a no-op implementation suitable for unit tests and benchmarks
 * on the JVM platform, where native notifications are not available.
 */
actual class NotificationScheduler {

    /**
     * No-op implementation for JVM platform.
     * 
     * In a real application, this would schedule a system notification.
     * For testing purposes, we simply return success.
     */
    actual suspend fun scheduleEventReminder(
        eventId: String,
        title: String,
        body: String,
        scheduledTime: Instant
    ): Result<Unit> {
        // No-op for JVM testing
        return Result.success(Unit)
    }

    /**
     * No-op implementation for JVM platform.
     */
    actual suspend fun schedulePollDeadlineReminder(
        pollId: String,
        eventId: String,
        title: String,
        body: String,
        deadlineTime: Instant
    ): Result<Unit> {
        // No-op for JVM testing
        return Result.success(Unit)
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
}