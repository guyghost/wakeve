package com.guyghost.wakeve.models

import com.guyghost.wakeve.currentTimeMillis
import kotlinx.serialization.Serializable

/**
 * Badge types for gamification and notifications.
 * Represents different notification scenarios for event updates, reminders, and achievements.
 */
@Serializable
enum class BadgeType {
    /** New event created by the user */
    EVENT_CREATED,
    
    /** Poll started for an event */
    POLL_OPENED,
    
    /** Poll closing soon (within 24 hours) */
    POLL_CLOSING_SOON,
    
    /** Date confirmed for an event */
    DATE_CONFIRMED,
    
    /** Scenarios are now available for comparison */
    SCENARIO_UNLOCKED,
    
    /** Meeting scheduled for the event */
    MEETING_SCHEDULED,
    
    /** User was mentioned in a comment */
    COMMENT_MENTION,
    
    /** Event has been finalized and completed */
    EVENT_FINALIZED
}

/**
 * Badge count data for tracking notification counts.
 * Pure data class representing badge notification counts.
 *
 * @property count The current badge count
 * @property lastUpdated Timestamp of last update in milliseconds
 */
@Serializable
data class BadgeCount(
    val count: Int,
    val lastUpdated: Long = currentTimeMillis()
) {
    /**
     * Creates a new BadgeCount with incremented count.
     */
    fun increment(): BadgeCount = BadgeCount(
        count = count + 1,
        lastUpdated = currentTimeMillis()
    )
    
    /**
     * Creates a new BadgeCount with decremented count (minimum 0).
     */
    fun decrement(): BadgeCount = BadgeCount(
        count = if (count > 0) count - 1 else 0,
        lastUpdated = currentTimeMillis()
    )
    
    /**
     * Creates a new BadgeCount with updated count.
     */
    fun update(newCount: Int): BadgeCount = BadgeCount(
        count = if (newCount >= 0) newCount else 0,
        lastUpdated = currentTimeMillis()
    )
    
    /**
     * Checks if the badge count has been updated since a given timestamp.
     */
    fun isUpdatedSince(timestamp: Long): Boolean = lastUpdated > timestamp
    
    /**
     * Validates that the count is non-negative.
     */
    fun isValid(): Boolean = count >= 0
}

/**
 * Notification payload for badge notifications.
 * Pure data class representing a notification to be displayed.
 *
 * @property id Unique identifier for the notification
 * @property type The type of badge notification
 * @property title Notification title
 * @property message Notification message body
 * @property badgeCount Optional badge count to display on app icon
 * @property deepLink Optional deep link to navigate when notification is tapped
 */
@Serializable
data class BadgeNotification(
    val id: String,
    val type: BadgeType,
    val title: String,
    val message: String,
    val badgeCount: Int? = null,
    val deepLink: String? = null
) {
    /**
     * Creates a copy with an updated badge count.
     */
    fun withBadgeCount(count: Int): BadgeNotification = copy(badgeCount = count)
    
    /**
     * Creates a copy with a deep link.
     */
    fun withDeepLink(link: String): BadgeNotification = copy(deepLink = link)
    
    /**
     * Generates a notification channel ID based on the badge type.
     */
    fun getChannelId(): String = when (type) {
        BadgeType.EVENT_CREATED -> "event_notifications"
        BadgeType.POLL_OPENED, BadgeType.POLL_CLOSING_SOON -> "poll_notifications"
        BadgeType.DATE_CONFIRMED -> "date_notifications"
        BadgeType.SCENARIO_UNLOCKED -> "scenario_notifications"
        BadgeType.MEETING_SCHEDULED -> "meeting_notifications"
        BadgeType.COMMENT_MENTION -> "comment_notifications"
        BadgeType.EVENT_FINALIZED -> "event_finalized_notifications"
    }
    
    /**
     * Validates that the notification has required fields.
     */
    fun isValid(): Boolean = id.isNotBlank() && title.isNotBlank() && message.isNotBlank()
}

/**
 * Maps a BadgeType to a user-friendly notification title.
 */
fun BadgeType.getNotificationTitle(): String = when (this) {
    BadgeType.EVENT_CREATED -> "Nouvel événement créé"
    BadgeType.POLL_OPENED -> "Nouveau sondage ouvert"
    BadgeType.POLL_CLOSING_SOON -> "Sondage bientôt terminé"
    BadgeType.DATE_CONFIRMED -> "Date confirmée"
    BadgeType.SCENARIO_UNLOCKED -> "Scénarios disponibles"
    BadgeType.MEETING_SCHEDULED -> "Réunion planifiée"
    BadgeType.COMMENT_MENTION -> "Mention dans un commentaire"
    BadgeType.EVENT_FINALIZED -> "Événement finalisé"
}

/**
 * Maps a BadgeType to a user-friendly default message.
 */
fun BadgeType.getDefaultMessage(eventTitle: String = "votre événement"): String = when (this) {
    BadgeType.EVENT_CREATED -> "Vous avez créé $eventTitle"
    BadgeType.POLL_OPENED -> "Un nouveau sondage est disponible pour $eventTitle"
    BadgeType.POLL_CLOSING_SOON -> "Le sondage pour $eventTitle se termine bientôt"
    BadgeType.DATE_CONFIRMED -> "La date pour $eventTitle a été confirmée"
    BadgeType.SCENARIO_UNLOCKED -> "Les scénarios pour $eventTitle sont maintenant disponibles"
    BadgeType.MEETING_SCHEDULED -> "Une réunion a été planifiée pour $eventTitle"
    BadgeType.COMMENT_MENTION -> "Quelqu'un vous a mentionné dans un commentaire"
    BadgeType.EVENT_FINALIZED -> "$eventTitle est maintenant finalisé"
}

/**
 * Creates a deep link for a specific notification type.
 */
fun BadgeType.createDeepLink(eventId: String, additionalPath: String? = null): String {
    val basePath = "wakeve://events/$eventId"
    return when {
        additionalPath != null -> "$basePath/$additionalPath"
        this == BadgeType.SCENARIO_UNLOCKED -> "$basePath/scenarios"
        this == BadgeType.MEETING_SCHEDULED -> "$basePath/meetings"
        this == BadgeType.COMMENT_MENTION -> "$basePath/comments"
        else -> basePath
    }
}
