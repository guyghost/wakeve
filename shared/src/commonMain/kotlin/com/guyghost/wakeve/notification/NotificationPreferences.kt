package com.guyghost.wakeve.notification

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * User notification preferences.
 * Controls which notifications are sent and when.
 */
@Serializable
data class NotificationPreferences(
    val userId: String,
    val enabledTypes: Set<NotificationType>,
    val quietHoursStart: QuietTime? = null,
    val quietHoursEnd: QuietTime? = null,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val updatedAt: Instant
)

/**
 * Quiet time for notifications.
 */
@Serializable
data class QuietTime(
    val hour: Int,
    val minute: Int
) {
    init {
        require(hour in 0..23) { "Hour must be between 0 and 23" }
        require(minute in 0..59) { "Minute must be between 0 and 59" }
    }

    /**
     * Format as HH:MM.
     */
    fun toDisplayString(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    /**
     * Parse from HH:MM string.
     */
    companion object {
        fun fromString(time: String): QuietTime? {
            val parts = time.split(":")
            return if (parts.size == 2) {
                try {
                    QuietTime(parts[0].toInt(), parts[1].toInt())
                } catch (e: NumberFormatException) {
                    null
                }
            } else null
        }
    }
}

/**
 * Check if notification should be sent based on quiet hours.
 * Urgent notifications bypass quiet hours.
 */
fun NotificationPreferences.shouldSend(
    type: NotificationType,
    currentTime: Instant
): Boolean {
    // Check if type is enabled
    if (type !in enabledTypes) return false

    // Urgent notifications bypass quiet hours
    if (type.isUrgent()) return true

    // Check quiet hours
    val start = quietHoursStart ?: return true
    val end = quietHoursEnd ?: return true

    // Convert to minutes since midnight
    val currentMinutes = currentTime.toEpochMilliseconds() / 60000 % 1440
    val startMinutes = start.hour * 60 + start.minute
    val endMinutes = end.hour * 60 + end.minute

    // Handle overnight quiet hours (e.g., 22:00 - 08:00)
    val inQuietHours = if (startMinutes > endMinutes) {
        // Overnight: 22:00 - 08:00 means quiet between 22:00-24:00 AND 00:00-08:00
        currentMinutes >= startMinutes || currentMinutes < endMinutes
    } else {
        // Same day: 22:00 - 23:59
        currentMinutes in startMinutes..<endMinutes
    }

    return !inQuietHours
}

/**
 * Default notification preferences for new users.
 */
fun defaultNotificationPreferences(userId: String): NotificationPreferences =
    NotificationPreferences(
        userId = userId,
        enabledTypes = setOf(
            NotificationType.EVENT_INVITE,
            NotificationType.VOTE_REMINDER,
            NotificationType.DATE_CONFIRMED,
            NotificationType.NEW_SCENARIO,
            NotificationType.MENTION,
            NotificationType.MEETING_REMINDER
        ),
        quietHoursStart = QuietTime(22, 0), // 10 PM
        quietHoursEnd = QuietTime(8, 0),    // 8 AM
        soundEnabled = true,
        vibrationEnabled = true,
        updatedAt = Clock.System.now()
    )
