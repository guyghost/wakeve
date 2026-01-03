package com.guyghost.wakeve.gamification

import com.guyghost.wakeve.ml.Language
import com.guyghost.wakeve.models.BadgeNotification

/**
 * Service interface for gamification-related push notifications.
 * Handles badge unlocked notifications, points earned, and voice assistant errors.
 *
 * This interface is implemented on each platform using:
 * - Android: NotificationManagerCompat for local notifications
 * - iOS: UNUserNotificationCenter for remote push notifications
 *
 * @property badgeNotificationChannel Channel configuration for badge notifications
 * @property pointsNotificationChannel Channel configuration for points notifications
 * @property voiceAssistantNotificationChannel Channel configuration for voice assistant errors
 */
interface BadgeNotificationService {
    
    /**
     * Shows a notification when a badge is unlocked.
     * Displays the badge name, earned points, and a description.
     *
     * @param userId The user who unlocked the badge
     * @param badge The badge that was unlocked
     * @param pointsEarned Points awarded for unlocking the badge
     */
    suspend fun showBadgeUnlockedNotification(
        userId: String,
        badge: Badge,
        pointsEarned: Int
    )
    
    /**
     * Shows a notification when points are earned from an action.
     *
     * @param userId The user who earned the points
     * @param points The number of points earned
     * @param action The action that triggered the points (e.g., "created an event")
     */
    suspend fun showPointsEarnedNotification(
        userId: String,
        points: Int,
        action: String
    )
    
    /**
     * Shows an error notification for voice assistant issues.
     *
     * @param userId The user experiencing the error
     * @param error The error message to display
     */
    suspend fun showVoiceAssistantError(
        userId: String,
        error: String
    )
    
    /**
     * Sends a badge notification to the system notification shade.
     *
     * @param notification The notification payload to display
     */
    suspend fun sendBadgeNotification(notification: BadgeNotification)
    
    /**
     * Clears a specific badge notification from the notification shade.
     *
     * @param notificationId The ID of the notification to clear
     */
    suspend fun clearBadgeNotification(notificationId: String)
    
    /**
     * Updates the badge count displayed on the app launcher icon.
     *
     * @param count The badge count to display
     */
    suspend fun updateBadgeCount(count: Int)
    
    /**
     * Requests notification permission from the user.
     *
     * @return true if permission granted, false otherwise
     */
    suspend fun requestNotificationPermission(): Boolean
    
    /**
     * Checks if notification permission is granted.
     *
     * @return true if notifications are enabled, false otherwise
     */
    fun isNotificationEnabled(): Boolean
}

/**
 * Notification channels for gamification features.
 */
enum class NotificationChannel {
    /** Channel for badge unlock notifications */
    BADGES,
    
    /** Channel for points earned notifications */
    POINTS,
    
    /** Channel for voice assistant errors and status */
    VOICE_ASSISTANT
}

/**
 * Configuration for a notification channel.
 *
 * @property id Unique identifier for the channel
 * @property name User-visible name of the channel
 * @property description User-visible description of the channel
 * @property importance Importance level for notifications in this channel
 */
data class NotificationChannelConfig(
    val id: String,
    val name: String,
    val description: String,
    val importance: Int
)

/**
 * Result of a notification permission request.
 */
sealed class NotificationPermissionResult {
    /** Permission granted */
    data object Granted : NotificationPermissionResult()
    
    /** Permission denied by user */
    data object Denied : NotificationPermissionResult()
    
    /** Permission request was not needed (already granted/denied) */
    data object NotNeeded : NotificationPermissionResult()
}

/**
 * Helper object for generating notification content for badges.
 */
object BadgeNotificationContent {
    
    /**
     * Generates the title for a badge notification.
     *
     * @param badgeName The name of the badge
     * @param language The user's language preference
     * @return Localized notification title
     */
    fun getTitle(badgeName: String, language: Language = Language.FR): String {
        return when (language) {
            Language.FR -> "Badge débloqué !"
            Language.EN -> "Badge unlocked!"
            Language.ES -> "¡Insignia desbloqueada!"
            Language.DE -> "Abzeichen freigeschaltet!"
            Language.IT -> "Badge sbloccato!"
        }
    }
    
    /**
     * Generates the body text for a badge notification.
     *
     * @param badge The badge that was unlocked
     * @param pointsEarned Points earned for the badge
     * @param language The user's language preference
     * @return Localized notification body
     */
    fun getBody(badge: Badge, pointsEarned: Int, language: Language = Language.FR): String {
        return when (language) {
            Language.FR -> "Félicitations ! Vous avez gagné $pointsEarned points et le badge '${badge.name}'"
            Language.EN -> "Congratulations! You earned $pointsEarned points and the '${badge.name}' badge"
            Language.ES -> "¡Enhorabuena! Has ganado $pointsEarned puntos y la insignia '${badge.name}'"
            Language.DE -> "Glückwunsch! Du hast $pointsEarned Punkte und das '${badge.name}' Abzeichen verdient"
            Language.IT -> "Congratulazioni! Hai guadagnato $pointsEarned punti e il badge '${badge.name}'"
        }
    }
    
    /**
     * Generates the expanded (big text) notification content.
     *
     * @param badge The badge that was unlocked
     * @param pointsEarned Points earned for the badge
     * @param language The user's language preference
     * @return Localized expanded notification text
     */
    fun getExpandedText(badge: Badge, pointsEarned: Int, language: Language = Language.FR): String {
        val baseText = getBody(badge, pointsEarned, language)
        return when (language) {
            Language.FR -> "$baseText\n\n${badge.description}"
            Language.EN -> "$baseText\n\n${badge.description}"
            Language.ES -> "$baseText\n\n${badge.description}"
            Language.DE -> "$baseText\n\n${badge.description}"
            Language.IT -> "$baseText\n\n${badge.description}"
        }
    }
}

/**
 * Helper object for generating notification content for points.
 */
object PointsNotificationContent {
    
    /**
     * Generates the title for a points notification.
     *
     * @param points The number of points earned
     * @param language The user's language preference
     * @return Localized notification title
     */
    fun getTitle(points: Int, language: Language = Language.FR): String {
        return when (language) {
            Language.FR -> when {
                points == 1 -> "1 point gagné !"
                else -> "$points points gagnés !"
            }
            Language.EN -> when {
                points == 1 -> "1 point earned!"
                else -> "$points points earned!"
            }
            Language.ES -> when {
                points == 1 -> "¡1 punto ganado!"
                else -> "¡$points puntos ganados!"
            }
            Language.DE -> when {
                points == 1 -> "1 Punkt verdient!"
                else -> "$points Punkte verdient!"
            }
            Language.IT -> when {
                points == 1 -> "1 punto guadagnato!"
                else -> "$points punti guadagnati!"
            }
        }
    }
    
    /**
     * Generates the body text for a points notification.
     *
     * @param points The number of points earned
     * @param action The action that triggered the points
     * @param language The user's language preference
     * @return Localized notification body
     */
    fun getBody(points: Int, action: String, language: Language = Language.FR): String {
        return when (language) {
            Language.FR -> "Vous avez gagné $points points pour $action"
            Language.EN -> "You earned $points points for $action"
            Language.ES -> "Has ganado $points puntos por $action"
            Language.DE -> "Du hast $points Punkte für $action verdient"
            Language.IT -> "Hai guadagnato $points punti per $action"
        }
    }
}
