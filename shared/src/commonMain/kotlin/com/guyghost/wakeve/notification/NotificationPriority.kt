package com.guyghost.wakeve.notification

/**
 * Priority levels for rich notifications.
 * These map to the existing NotificationPriority enum values.
 */
enum class RichNotificationPriority {
    HIGH,
    DEFAULT,
    LOW
}

/**
 * Map RichNotification priority to system notification priority level.
 * Returns an integer value suitable for Android NotificationCompat.Builder.setPriority()
 * and iOS UNNotificationPresentationOptions.
 */
fun RichNotificationPriority.toSystemPriority(): Int = when (this) {
    RichNotificationPriority.HIGH -> 2  // NotificationCompat.PRIORITY_HIGH
    RichNotificationPriority.DEFAULT -> 0  // NotificationCompat.PRIORITY_DEFAULT
    RichNotificationPriority.LOW -> -1  // NotificationCompat.PRIORITY_LOW
}

/**
 * Check if this priority should bypass quiet hours/Do Not Disturb.
 */
fun RichNotificationPriority.isInterruptive(): Boolean = this == RichNotificationPriority.HIGH

/**
 * Convert RichNotificationPriority to legacy NotificationPriority.
 */
fun RichNotificationPriority.toLegacyPriority(): NotificationPriority = when (this) {
    RichNotificationPriority.HIGH -> NotificationPriority.HIGH
    RichNotificationPriority.DEFAULT -> NotificationPriority.MEDIUM
    RichNotificationPriority.LOW -> NotificationPriority.LOW
}

/**
 * Convert legacy NotificationPriority to RichNotificationPriority.
 */
fun NotificationPriority.toRichPriority(): RichNotificationPriority = when (this) {
    NotificationPriority.URGENT,
    NotificationPriority.HIGH -> RichNotificationPriority.HIGH
    NotificationPriority.MEDIUM -> RichNotificationPriority.DEFAULT
    NotificationPriority.LOW -> RichNotificationPriority.LOW
}

/**
 * Convert legacy NotificationPriority (from NotificationTypes) to RichNotification priority.
 */
fun NotificationType.getRichPriority(): RichNotificationPriority {
    return this.getPriority().toRichPriority()
}
