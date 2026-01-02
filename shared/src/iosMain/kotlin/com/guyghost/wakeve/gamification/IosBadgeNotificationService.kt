package com.guyghost.wakeve.gamification

import com.guyghost.wakeve.ml.Language
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptions
import platform.UserNotifications.UNNotificationAttachment
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSettings
import platform.UserNotifications.UNAuthorizationStatus

/**
 * iOS implementation of BadgeNotificationService.
 * Uses UNUserNotificationCenter for local and remote push notifications.
 *
 * This implementation:
 * - Requests notification permissions on first launch
 * - Creates notification categories for badge and points actions
 * - Shows notifications with badge icons as attachments
 * - Supports deep links via notification userInfo
 *
 * Note: For full push notification support (FCM/APNs), this service integrates
 * with the platform's push notification infrastructure.
 */
@OptIn(ExperimentalForeignApi::class)
class IosBadgeNotificationService : BadgeNotificationService {

    companion object {
        private const val TAG = "IosBadgeNotif"
        
        // Notification category identifiers
        private const val CATEGORY_BADGE = "CATEGORY_BADGE"
        private const val CATEGORY_POINTS = "CATEGORY_POINTS"
        private const val CATEGORY_VOICE = "CATEGORY_VOICE"
        
        // Notification action identifiers
        private const val ACTION_VIEW_BADGE = "ACTION_VIEW_BADGE"
        private const val ACTION_SHARE = "ACTION_SHARE"
        private const val ACTION_RETRY = "ACTION_RETRY"
        
        // Notification identifiers
        private const val NOTIFICATION_ID_BASE = 10000
        
        // Base URL for badge icons (would be replaced with actual CDN URL)
        private const val BADGE_ICON_BASE_URL = "https://wakeve.app/badges"
    }

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    init {
        setupNotificationCategories()
    }

    /**
     * Sets up notification categories with actions for better user interaction.
     * Categories define how notifications appear and what actions users can take.
     */
    private fun setupNotificationCategories() {
        try {
            // Badge category with "View Badge" action
            val viewBadgeAction = UNNotificationAction.actionWithIdentifier(
                ACTION_VIEW_BADGE,
                "Voir le badge",
                UNNotificationActionOptions.Foreground
            )
            
            val shareBadgeAction = UNNotificationAction.actionWithIdentifier(
                ACTION_SHARE,
                "Partager",
                UNNotificationActionOptions.None
            )
            
            val badgeCategory = UNNotificationCategory.categoryWithIdentifier(
                CATEGORY_BADGES,
                listOf(viewBadgeAction, shareBadgeAction),
                listOf(),
                UNNotificationCategoryOptions.None
            )

            // Points category with simple view action
            val viewPointsAction = UNNotificationAction.actionWithIdentifier(
                ACTION_VIEW_BADGE,
                "Voir mes points",
                UNNotificationActionOptions.Foreground
            )
            
            val pointsCategory = UNNotificationCategory.categoryWithIdentifier(
                CATEGORY_POINTS,
                listOf(viewPointsAction),
                listOf(),
                UNNotificationCategoryOptions.None
            )

            // Voice assistant category with retry action
            val retryAction = UNNotificationAction.actionWithIdentifier(
                ACTION_RETRY,
                "Réessayer",
                UNNotificationActionOptions.Foreground
            )
            
            val voiceCategory = UNNotificationCategory.categoryWithIdentifier(
                CATEGORY_VOICE,
                listOf(retryAction),
                listOf(),
                UNNotificationCategoryOptions.None
            )

            notificationCenter.setNotificationCategories(
                setOf(badgeCategory, pointsCategory, voiceCategory)
            )

            println("$TAG: Notification categories set up successfully")
        } catch (e: Exception) {
            println("$TAG: Error setting up notification categories: ${e.message}")
        }
    }

    /**
     * Shows a notification when a badge is unlocked.
     * Includes the badge icon as an attachment and expandable content.
     */
    override suspend fun showBadgeUnlockedNotification(
        userId: String,
        badge: Badge,
        pointsEarned: Int
    ) = withContext(Dispatchers.Main) {
        try {
            val title = BadgeNotificationContent.getTitle(badge.name)
            val body = BadgeNotificationContent.getBody(badge, pointsEarned)
            val expandedText = BadgeNotificationContent.getExpandedText(badge, pointsEarned)

            val content = UNMutableNotificationContent().apply {
                this.title = title
                this.body = body
                this.sound = .default
                this.badge = 1
                this.categoryIdentifier = CATEGORY_BADGE
                this.threadIdentifier = "badges"
                
                // Add expanded text for 3D Touch/Long press
                this.summaryArgument = badge.name
                
                // Add user info for deep linking
                setValue(userId, forKey = "userId")
                setValue(badge.id, forKey = "badgeId")
                setValue("badge_unlocked", forKey = "notificationType")
            }

            // Create badge icon attachment
            val iconUrl = NSURL(string = "$BADGE_ICON_BASE_URL/${badge.id}.png")
            val attachment = try {
                UNNotificationAttachment.identifierUrlOptions(
                    "badge-icon-${badge.id}",
                    iconUrl,
                    null
                )
            } catch (e: Exception) {
                println("$TAG: Error creating badge attachment: ${e.message}")
                null
            }
            
            attachment?.let {
                content.attachments = listOf(it)
            }

            val request = UNNotificationRequest.identifierContentTrigger(
                "badge_${badge.id}",
                content,
                null
            )

            notificationCenter.addNotificationRequest(request) { error ->
                if (error != null) {
                    println("$TAG: Error adding badge notification: ${error.description}")
                } else {
                    println("$TAG: Badge notification scheduled for badge: ${badge.id}")
                }
            }
        } catch (e: Exception) {
            println("$TAG: Error showing badge notification: ${e.message}")
        }
    }

    /**
     * Shows a notification when points are earned from an action.
     */
    override suspend fun showPointsEarnedNotification(
        userId: String,
        points: Int,
        action: String
    ) = withContext(Dispatchers.Main) {
        try {
            val title = PointsNotificationContent.getTitle(points)
            val body = PointsNotificationContent.getBody(points, action)

            val content = UNMutableNotificationContent().apply {
                this.title = title
                this.body = body
                this.sound = .default
                this.categoryIdentifier = CATEGORY_POINTS
                this.threadIdentifier = "points"
                
                setValue(userId, forKey = "userId")
                setValue(points, forKey = "pointsEarned")
                setValue(action, forKey = "action")
                setValue("points_earned", forKey = "notificationType")
            }

            val request = UNNotificationRequest.identifierContentTrigger(
                "points_$points",
                content,
                null
            )

            notificationCenter.addNotificationRequest(request) { error ->
                if (error != null) {
                    println("$TAG: Error adding points notification: ${error.description}")
                } else {
                    println("$TAG: Points notification scheduled: $points points for $action")
                }
            }
        } catch (e: Exception) {
            println("$TAG: Error showing points notification: ${e.message}")
        }
    }

    /**
     * Shows a notification for voice assistant errors.
     */
    override suspend fun showVoiceAssistantError(
        userId: String,
        error: String
    ) = withContext(Dispatchers.Main) {
        try {
            val title = "Erreur Assistant Vocal"
            val body = "Une erreur s'est produite: $error"

            val content = UNMutableNotificationContent().apply {
                this.title = title
                this.body = body
                this.sound = .default
                this.categoryIdentifier = CATEGORY_VOICE
                this.threadIdentifier = "voice"
                this.interruptionLevel = UNNotificationSettings.InterruptionLevelPassive
                
                setValue(userId, forKey = "userId")
                setValue(error, forKey = "errorMessage")
                setValue("voice_error", forKey = "notificationType")
            }

            val request = UNNotificationRequest.identifierContentTrigger(
                "voice_error",
                content,
                null
            )

            notificationCenter.addNotificationRequest(request) { error ->
                if (error != null) {
                    println("$TAG: Error adding voice notification: ${error.description}")
                } else {
                    println("$TAG: Voice error notification scheduled: $error")
                }
            }
        } catch (e: Exception) {
            println("$TAG: Error showing voice notification: ${e.message}")
        }
    }

    /**
     * Requests notification permission from the user.
     * Shows the system permission dialog on first launch.
     */
    override suspend fun requestNotificationPermission(): Boolean = withContext(Dispatchers.Main) {
        try {
            val options = UNAuthorizationOptionAlert or 
                         UNAuthorizationOptionSound or 
                         UNAuthorizationOptionBadge

            var granted = false
            
            notificationCenter.requestAuthorizationWithOptions(options) { granted, error ->
                if (error != null) {
                    println("$TAG: Notification permission error: ${error.description}")
                    granted = false
                } else {
                    println("$TAG: Notification permission granted: $granted")
                }
            }

            // Also register for remote notifications (push)
            // This would typically be called from AppDelegate
            // UIApplication.sharedApplication().registerForRemoteNotifications()

            granted
        } catch (e: Exception) {
            println("$TAG: Error requesting notification permission: ${e.message}")
            false
        }
    }

    /**
     * Checks if notification permission is already granted.
     */
    override fun isNotificationEnabled(): Boolean {
        var isEnabled = false
        
        notificationCenter.getNotificationSettings { settings ->
            isEnabled = when (settings.authorizationStatus) {
                UNAuthorizationStatus.Authorized -> true
                UNAuthorizationStatus.Provisional -> true
                else -> false
            }
        }
        
        return isEnabled
    }

    /**
     * Gets the current notification authorization status.
     *
     * @return The authorization status as a string for debugging
     */
    fun getAuthorizationStatus(): String {
        var status = "Unknown"
        
        notificationCenter.getNotificationSettings { settings ->
            status = when (settings.authorizationStatus) {
                UNAuthorizationStatus.NotDetermined -> "Not Determined"
                UNAuthorizationStatus.Denied -> "Denied"
                UNAuthorizationStatus.Authorized -> "Authorized"
                UNAuthorizationStatus.Provisional -> "Provisional"
                UNAuthorizationStatus.Ephemeral -> "Ephemeral"
                else -> "Unknown"
            }
        }
        
        return status
    }

    /**
     * Removes all pending and delivered notifications.
     */
    suspend fun removeAllNotifications() = withContext(Dispatchers.Main) {
        notificationCenter.removeAllPendingNotificationRequests()
        notificationCenter.removeAllDeliveredNotifications()
        println("$TAG: All notifications removed")
    }

    /**
     * Removes a specific notification by identifier.
     *
     * @param identifier The notification identifier to remove
     */
    suspend fun removeNotification(identifier: String) = withContext(Dispatchers.Main) {
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(identifier))
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(listOf(identifier))
        println("$TAG: Notification removed: $identifier")
    }

    /**
     * Gets the channel configuration for a specific channel type.
     *
     * @param channel The channel type to get config for
     * @return NotificationChannelConfig with channel details
     */
    fun getChannelConfig(channel: NotificationChannel): NotificationChannelConfig {
        return when (channel) {
            NotificationChannel.BADGES -> NotificationChannelConfig(
                id = "com.wakeve.badges",
                name = "Badges débloqués",
                description = "Notifications lors du déverrouillage de badges",
                importance = 4 // High
            )
            NotificationChannel.POINTS -> NotificationChannelConfig(
                id = "com.wakeve.points",
                name = "Points gagnés",
                description = "Notifications lors du gain de points",
                importance = 3 // Default
            )
            NotificationChannel.VOICE_ASSISTANT -> NotificationChannelConfig(
                id = "com.wakeve.voice",
                name = "Assistant vocal",
                description = "Notifications de l'assistant vocal",
                importance = 1 // Low
            )
        }
    }
}
