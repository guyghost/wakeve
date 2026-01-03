package com.guyghost.wakeve.gamification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.guyghost.wakeve.models.BadgeNotification

/**
 * Android-specific implementation of badge notifications.
 * Uses NotificationCompat for compatibility with Android 13+.
 *
 * Architecture: Imperative Shell - Handles platform-specific I/O operations.
 */
class AndroidBadgeNotificationService(
    private val context: Context
) : BadgeNotificationService {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        private const val CHANNEL_ID_BADGES = "wakeve_badges"
        private const val CHANNEL_ID_POINTS = "wakeve_points"
        private const val CHANNEL_ID_VOICE = "wakeve_voice"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val badgesChannel = NotificationChannel(
                CHANNEL_ID_BADGES,
                "Badges débloqués",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications lors du déverrouillage de badges"
            }
            
            val pointsChannel = NotificationChannel(
                CHANNEL_ID_POINTS,
                "Points gagnés",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications lors du gain de points"
            }
            
            val voiceChannel = NotificationChannel(
                CHANNEL_ID_VOICE,
                "Assistant vocal",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications de l'assistant vocal"
            }
            
            notificationManager.createNotificationChannel(badgesChannel)
            notificationManager.createNotificationChannel(pointsChannel)
            notificationManager.createNotificationChannel(voiceChannel)
        }
    }

    override suspend fun showBadgeUnlockedNotification(
        userId: String,
        badge: Badge,
        pointsEarned: Int
    ) {
        // Stub implementation - notifications should be handled in composeApp
        // where R resources are available
    }

    override suspend fun showPointsEarnedNotification(
        userId: String,
        points: Int,
        action: String
    ) {
        // Stub implementation
    }

    override suspend fun showVoiceAssistantError(
        userId: String,
        error: String
    ) {
        // Stub implementation
    }

    override suspend fun sendBadgeNotification(notification: BadgeNotification) {
        // Stub implementation
    }

    override suspend fun clearBadgeNotification(notificationId: String) {
        notificationManager.cancel(notificationId.hashCode())
    }

    override suspend fun updateBadgeCount(count: Int) {
        // Badge count on launcher icon - requires ShortcutBadger or similar
    }

    override suspend fun requestNotificationPermission(): Boolean {
        // For Android 13+, this should be handled in the Activity
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }

    override fun isNotificationEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
}
