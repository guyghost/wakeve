package com.guyghost.wakeve.gamification

import com.guyghost.wakeve.models.BadgeNotification
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import platform.Foundation.NSNumber
import platform.Foundation.NSUUID
import platform.UIKit.UIApplication
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS badge notification service backed by UNUserNotificationCenter.
 */
class IosBadgeNotificationService : BadgeNotificationService {
    private var cachedNotificationEnabled = false
    
    override suspend fun showBadgeUnlockedNotification(
        userId: String,
        badge: Badge,
        pointsEarned: Int
    ) {
        sendLocalNotification(
            id = "badge-${badge.id}-${NSUUID.UUID().UUIDString}",
            title = "Badge unlocked: ${badge.name}",
            message = "+$pointsEarned points - ${badge.description}",
            badgeCount = null,
            deepLink = null
        )
    }
    
    override suspend fun showPointsEarnedNotification(
        userId: String,
        points: Int,
        action: String
    ) {
        sendLocalNotification(
            id = "points-$userId-${NSUUID.UUID().UUIDString}",
            title = "Points earned",
            message = "+$points for $action",
            badgeCount = null,
            deepLink = null
        )
    }
    
    override suspend fun showVoiceAssistantError(
        userId: String,
        error: String
    ) {
        sendLocalNotification(
            id = "voice-error-$userId-${NSUUID.UUID().UUIDString}",
            title = "Voice assistant unavailable",
            message = error,
            badgeCount = null,
            deepLink = null
        )
    }
    
    override suspend fun sendBadgeNotification(notification: BadgeNotification) {
        if (!notification.isValid()) return

        sendLocalNotification(
            id = notification.id,
            title = notification.title,
            message = notification.message,
            badgeCount = notification.badgeCount,
            deepLink = notification.deepLink
        )
    }
    
    override suspend fun clearBadgeNotification(notificationId: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removePendingNotificationRequestsWithIdentifiers(listOf(notificationId))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(notificationId))
    }
    
    override suspend fun updateBadgeCount(count: Int) {
        UIApplication.sharedApplication.applicationIconBadgeNumber = count.toLong()
    }
    
    override suspend fun requestNotificationPermission(): Boolean {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        return suspendCoroutine { continuation ->
            center.requestAuthorizationWithOptions(options) { granted, _ ->
                cachedNotificationEnabled = granted
                continuation.resume(granted)
            }
        }
    }
    
    override fun isNotificationEnabled(): Boolean {
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            cachedNotificationEnabled = status == UNAuthorizationStatusAuthorized ||
                status == UNAuthorizationStatusProvisional ||
                status == UNAuthorizationStatusEphemeral
        }
        return cachedNotificationEnabled
    }

    private fun sendLocalNotification(
        id: String,
        title: String,
        message: String,
        badgeCount: Int?,
        deepLink: String?
    ) {
        val content = UNMutableNotificationContent()
        content.setTitle(title)
        content.setBody(message)
        content.setSound(UNNotificationSound.defaultSound())

        if (badgeCount != null) {
            content.setBadge(NSNumber(int = badgeCount))
        }

        if (!deepLink.isNullOrBlank()) {
            content.setUserInfo(mapOf<Any?, Any?>("deep_link" to deepLink))
        }

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = 1.0,
            repeats = false
        )
        val request = UNNotificationRequest.requestWithIdentifier(id, content, trigger)
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { error ->
            if (error != null) {
                cachedNotificationEnabled = false
            }
        }
    }
}
