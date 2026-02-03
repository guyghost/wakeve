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
 * Get priority for a notification type.
 */
fun NotificationType.getPriority(): NotificationPriority = when (this) {
    NotificationType.EVENT_INVITE,
    NotificationType.DATE_CONFIRMED,
    NotificationType.SCENARIO_SELECTED -> NotificationPriority.HIGH

    NotificationType.MEETING_REMINDER -> NotificationPriority.URGENT

    NotificationType.VOTE_REMINDER,
    NotificationType.NEW_SCENARIO,
    NotificationType.MENTION,
    NotificationType.PAYMENT_DUE,
    NotificationType.VOTE_CLOSE_REMINDER,
    NotificationType.DEADLINE_REMINDER -> NotificationPriority.MEDIUM

    NotificationType.NEW_COMMENT,
    NotificationType.EVENT_UPDATE,
    NotificationType.COMMENT_REPLY -> NotificationPriority.LOW
}

/**
 * Check if notification should bypass quiet hours.
 */
fun NotificationType.isUrgent(): Boolean = when (this) {
    NotificationType.MEETING_REMINDER -> true
    else -> false
}

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
