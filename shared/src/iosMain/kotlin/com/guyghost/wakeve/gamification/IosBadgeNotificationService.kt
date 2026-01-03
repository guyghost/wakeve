package com.guyghost.wakeve.gamification

import com.guyghost.wakeve.models.BadgeNotification

/**
 * iOS stub implementation of BadgeNotificationService.
 * 
 * This is a placeholder implementation. Full iOS notification support
 * using UNUserNotificationCenter should be implemented in the iosApp module.
 */
class IosBadgeNotificationService : BadgeNotificationService {
    
    override suspend fun showBadgeUnlockedNotification(
        userId: String,
        badge: Badge,
        pointsEarned: Int
    ) {
        // Stub: iOS notifications should be handled in SwiftUI using UNUserNotificationCenter
        println("iOS Badge notification: ${badge.name} unlocked with $pointsEarned points")
    }
    
    override suspend fun showPointsEarnedNotification(
        userId: String,
        points: Int,
        action: String
    ) {
        // Stub: iOS notifications should be handled in SwiftUI using UNUserNotificationCenter
        println("iOS Points notification: $points points earned for $action")
    }
    
    override suspend fun showVoiceAssistantError(
        userId: String,
        error: String
    ) {
        // Stub: iOS notifications should be handled in SwiftUI using UNUserNotificationCenter
        println("iOS Voice assistant error: $error")
    }
    
    override suspend fun sendBadgeNotification(notification: BadgeNotification) {
        // Stub: iOS notifications should be handled in SwiftUI
        println("iOS Badge notification: ${notification.title}")
    }
    
    override suspend fun clearBadgeNotification(notificationId: String) {
        // Stub: iOS notifications should be handled in SwiftUI
        println("iOS Clear notification: $notificationId")
    }
    
    override suspend fun updateBadgeCount(count: Int) {
        // Stub: iOS badge count should be set via UIApplication.shared.applicationIconBadgeNumber in SwiftUI
        println("iOS Badge count: $count")
    }
    
    override suspend fun requestNotificationPermission(): Boolean {
        // Stub: iOS notification permission should be requested via UNUserNotificationCenter in SwiftUI
        return false
    }
    
    override fun isNotificationEnabled(): Boolean {
        // Stub: iOS notification status should be checked via UNUserNotificationCenter in SwiftUI
        return false
    }
}
