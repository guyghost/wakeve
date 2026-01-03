package com.guyghost.wakeve.gamification

/**
 * iOS implementation of BadgeNotificationService.
 * Simplified stub implementation.
 */
class IosBadgeNotificationService : BadgeNotificationService {
    override suspend fun showBadgeUnlockedNotification(userId: String, badge: Badge, pointsEarned: Int) {}
    override suspend fun showPointsEarnedNotification(userId: String, points: Int, action: String) {}
    override suspend fun showVoiceAssistantError(userId: String, error: String) {}
    override suspend fun requestNotificationPermission(): Boolean = true
    override fun isNotificationEnabled(): Boolean = true
}
