package com.guyghost.wakeve.gamification

import com.guyghost.wakeve.models.BadgeNotification
import com.guyghost.wakeve.models.BadgeType
import com.guyghost.wakeve.models.getNotificationTitle
import com.guyghost.wakeve.models.getDefaultMessage
import com.guyghost.wakeve.models.createDeepLink
import kotlinx.coroutines.MainDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSUUID
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNAuthorizationOptions
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptions
import platform.UserNotifications.UNNotificationInterruptionLevel
import platform.UserNotifications.UNNotificationInterruptionLevelActive
import platform.UserNotifications.UNNotificationInterruptionLevelTimeSensitive
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSettings
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS-specific implementation of badge notifications.
 * Uses UserNotifications framework (iOS 10+) for local notifications.
 *
 * This service handles:
 * - UNNotificationContent with interruption levels
 * - Badge number display on app icon
 * - Action buttons (Open, Dismiss)
 * - Permission requests and status checks
 *
 * Architecture: Imperative Shell - Handles platform-specific I/O operations.
 */
class IosBadgeNotificationService : BadgeNotificationService {

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()
    private val mainDispatcher = MainDispatcher()

    companion object {
        private const val CATEGORY_IDENTIFIER_BADGE = "WAKEVE_BADGE_NOTIFICATION"
        private const val ACTION_OPEN = "OPEN_ACTION"
        private const val ACTION_DISMISS = "DISMISS_ACTION"
    }

    init {
        setupNotificationCategories()
    }

    /**
     * Sets up notification categories and actions for badge notifications.
     */
    private fun setupNotificationCategories() {
        // Create open action
        val openAction = UNNotificationAction(
            identifier = ACTION_OPEN,
            title = "Ouvrir",
            options = UNNotificationActionOptions.Foreground
        )
        
        // Create dismiss action
        val dismissAction = UNNotificationAction(
            identifier = ACTION_DISMISS,
            title = "Ignorer",
            options = UNNotificationActionOptions.Destructive
        )
        
        // Create notification category
        val category = platform.UserNotifications.UNNotificationCategory(
            identifier = CATEGORY_IDENTIFIER_BADGE,
            actions = listOf(openAction, dismissAction),
            intentIdentifiers = emptyList(),
            options = platform.UserNotifications.UNNotificationCategoryOptions.None
        )
        
        // Set categories
        notificationCenter.setNotificationCategories(setOf(category))
    }

    /**
     * Sends a badge notification to the system notification center.
     *
     * @param notification The notification payload to display
     */
    override suspend fun sendBadgeNotification(notification: BadgeNotification) {
        val content = UNMutableNotificationContent().apply {
            setTitle(notification.title)
            setBody(notification.message)
            setCategoryIdentifier(CATEGORY_IDENTIFIER_BADGE)
            
            // Set badge number on app icon
            notification.badgeCount?.let { count ->
                setBadge(count)
            }
            
            // Set interruption level based on notification type
            setInterruptionLevel(getInterruptionLevelForType(notification.type))
            
            // Set sound (default sound)
            setSound(UNNotificationSound.defaultSound)
            
            // Add thread identifier for grouping
            setThreadIdentifier(notification.type.name)
            
            // Set custom user info for deep link
            notification.deepLink?.let { deepLink ->
                val userInfo = mapOf("deepLink" to deepLink, "eventId" to extractEventId(deepLink))
                setUserInfo(userInfo)
            }
        }
        
        val request = UNNotificationRequest(
            identifier = notification.id,
            content = content,
            trigger = null // Immediate delivery
        )
        
        suspendCancellableCoroutine { continuation ->
            notificationCenter.addNotificationRequest(request) { error ->
                if (error != null) {
                    continuation.resumeWithException(error)
                } else {
                    continuation.resume(Unit)
                }
            }
            
            continuation.invokeOnCancellation {
                notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(notification.id))
            }
        }
    }

    /**
     * Clears a specific badge notification from the notification center.
     *
     * @param notificationId The ID of the notification to clear
     */
    override suspend fun clearBadgeNotification(notificationId: String) {
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(listOf(notificationId))
    }

    /**
     * Updates the badge count displayed on the app icon.
     *
     * @param count The badge count to display
     */
    override suspend fun updateBadgeCount(count: Int) {
        notificationCenter.setBadgeCount(count)
    }

    /**
     * Determines the interruption level for a notification based on its type.
     * Higher interruption levels will bypass Do Not Disturb and appear on lock screen.
     */
    private fun getInterruptionLevelForType(type: BadgeType): UNNotificationInterruptionLevel {
        return when (type) {
            BadgeType.POLL_CLOSING_SOON -> UNNotificationInterruptionLevelTimeSensitive
            BadgeType.DATE_CONFIRMED, BadgeType.EVENT_FINALIZED -> UNNotificationInterruptionLevelActive
            else -> UNNotificationInterruptionLevelActive
        }
    }

    /**
     * Extracts event ID from a deep link string.
     */
    private fun extractEventId(deepLink: String): String {
        // Expected format: wakeve://events/{eventId}
        return deepLink.removePrefix("wakeve://events/")
    }

    /**
     * Requests notification permissions from the user.
     *
     * @return true if permission granted, false otherwise
     */
    override suspend fun requestNotificationPermission(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val options = UNAuthorizationOptions(
                UNAuthorizationOptionAlert or
                UNAuthorizationOptionBadge or
                UNAuthorizationOptionSound
            )
            
            notificationCenter.requestAuthorization(options) { granted, error ->
                if (error != null) {
                    continuation.resume(false)
                } else {
                    continuation.resume(granted)
                }
            }
        }
    }

    /**
     * Checks if notification permission is granted.
     *
     * @return true if notifications are enabled, false otherwise
     */
    override fun isNotificationEnabled(): Boolean {
        // This is a simplified check - in production, use async checkSettingsForAuthorizationOptions
        return true // Will be updated to actual check
    }

    /**
     * Gets the current notification settings.
     */
    suspend fun getNotificationSettings(): UNNotificationSettings {
        return suspendCancellableCoroutine { continuation ->
            notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                continuation.resume(settings)
            }
        }
    }

    /**
     * Clears all badge notifications and resets the badge count.
     */
    suspend fun clearAllNotifications() {
        notificationCenter.removeAllDeliveredNotifications()
        notificationCenter.setBadgeCount(0)
    }

    /**
     * Generates a unique notification ID.
     */
    fun generateNotificationId(): String {
        return NSUUID.UUID().UUIDString
    }
}

/**
 * Extension function to create UNAuthorizationOptions with multiple flags.
 */
private operator fun UNAuthorizationOptions.invokevarargs(vararg options: ULong): UNAuthorizationOptions {
    var result: ULong = 0uL
    options.forEach { result = result or it }
    return result
}
