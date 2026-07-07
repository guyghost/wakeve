package com.guyghost.wakeve.notification

import kotlinx.serialization.Serializable

/**
 * Notification types supported by the system.
 * Each type has different routing and display behavior.
 */
@Serializable
enum class NotificationType {
    /**
     * User invited to event
     * Priority: HIGH, Action Required
     */
    EVENT_INVITE,

    /**
     * Poll deadline approaching
     * Priority: MEDIUM, Reminder
     */
    VOTE_REMINDER,

    /**
     * Event date confirmed
     * Priority: HIGH, Informational
     */
    DATE_CONFIRMED,

    /**
     * New scenario proposed
     * Priority: MEDIUM, Informational
     */
    NEW_SCENARIO,

    /**
     * Final scenario selected
     * Priority: HIGH, Action Required
     */
    SCENARIO_SELECTED,

    /**
     * New comment on event
     * Priority: LOW, Informational
     */
    NEW_COMMENT,

    /**
     * User mentioned in comment
     * Priority: MEDIUM, Action Required
     */
    MENTION,

    /**
     * Meeting starting soon
     * Priority: HIGH, Urgent
     */
    MEETING_REMINDER,

    /**
     * Payment settlement pending
     * Priority: MEDIUM, Action Required
     */
    PAYMENT_DUE,

    /**
     * Generic event update
     * Priority: LOW, Informational
     */
    EVENT_UPDATE,

    /**
     * Vote reminder before deadline
     * Priority: MEDIUM, Reminder
     */
    VOTE_CLOSE_REMINDER,

    /**
     * Deadline reminder
     * Priority: MEDIUM, Reminder
     */
    DEADLINE_REMINDER,

    /**
     * Reply to comment
     * Priority: LOW, Informational
     */
    COMMENT_REPLY
}

/**
 * Platform enum for device identification.
 */
@Serializable
enum class Platform {
    ANDROID,
    IOS
}

/**
 * Priority levels for notification routing.
 */
@Serializable
enum class NotificationPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

/**
 * Get priority for a notification type from the centralized delivery policy.
 */
fun NotificationType.getPriority(): NotificationPriority =
    deliveryProfile().priority

/**
 * Check if notification should bypass quiet hours.
 */
fun NotificationType.isUrgent(): Boolean =
    deliveryProfile().bypassQuietHours

/**
 * Check if notification requires user action.
 */
fun NotificationType.requiresAction(): Boolean = when (this) {
    NotificationType.EVENT_INVITE,
    NotificationType.SCENARIO_SELECTED,
    NotificationType.MENTION,
    NotificationType.PAYMENT_DUE -> true
    else -> false
}
