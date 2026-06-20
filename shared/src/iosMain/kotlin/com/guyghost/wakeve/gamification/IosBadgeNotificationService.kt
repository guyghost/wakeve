package com.guyghost.wakeve.gamification

import com.guyghost.wakeve.models.BadgeNotification

/**
 * iOS badge notification service placeholder.
 *
 * Fails explicitly until a UNUserNotificationCenter bridge is wired in the iOS app.
 */
class IosBadgeNotificationService : BadgeNotificationService {
    
    override suspend fun showBadgeUnlockedNotification(
        userId: String,
        badge: Badge,
        pointsEarned: Int
    ) {
        NoConfiguredBadgeNotificationService.showBadgeUnlockedNotification(userId, badge, pointsEarned)
    }
    
    override suspend fun showPointsEarnedNotification(
        userId: String,
        points: Int,
        action: String
    ) {
        NoConfiguredBadgeNotificationService.showPointsEarnedNotification(userId, points, action)
    }
    
    override suspend fun showVoiceAssistantError(
        userId: String,
        error: String
    ) {
        NoConfiguredBadgeNotificationService.showVoiceAssistantError(userId, error)
    }
    
    override suspend fun sendBadgeNotification(notification: BadgeNotification) {
        NoConfiguredBadgeNotificationService.sendBadgeNotification(notification)
    }
    
    override suspend fun clearBadgeNotification(notificationId: String) {
        NoConfiguredBadgeNotificationService.clearBadgeNotification(notificationId)
    }
    
    override suspend fun updateBadgeCount(count: Int) {
        NoConfiguredBadgeNotificationService.updateBadgeCount(count)
    }
    
    override suspend fun requestNotificationPermission(): Boolean {
        return NoConfiguredBadgeNotificationService.requestNotificationPermission()
    }
    
    override fun isNotificationEnabled(): Boolean {
        return NoConfiguredBadgeNotificationService.isNotificationEnabled()
    }
}
