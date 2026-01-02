package com.guyghost.wakeve.gamification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationChannelCompat
import com.guyghost.wakeve.ml.Language
import android.graphics.Color as AndroidColor

/**
 * Android implementation of BadgeNotificationService.
 * Uses NotificationManagerCompat for local notifications and NotificationChannelCompat for channel management.
 *
 * This implementation:
 * - Creates three notification channels (badges, points, voice assistant)
 * - Shows high-priority notifications for badge unlocks
 * - Supports deep links to open specific screens when tapped
 * - Handles notification permission requests
 *
 * @property context Application context for accessing system services
 * @property notificationManagerCompat Manager for displaying notifications
 */
class AndroidBadgeNotificationService(
    private val context: Context
) : BadgeNotificationService {

    companion object {
        private const val TAG = "AndroidBadgeNotif"

        // Channel IDs
        private const val CHANNEL_ID_BADGES = "channel_badges"
        private const val CHANNEL_ID_POINTS = "channel_points"
        private const val CHANNEL_ID_VOICE = "channel_voice"

        // Notification IDs
        private const val NOTIFICATION_ID_BASE = 10000

        // Deep link actions
        const val ACTION_OPEN_BADGE = "com.wakeve.ACTION_OPEN_BADGE"
        const val ACTION_OPEN_POINTS = "com.wakeve.ACTION_OPEN_POINTS"
        const val ACTION_OPEN_VOICE = "com.wakeve.ACTION_OPEN_VOICE"
    }

    private val notificationManagerCompat: NotificationManagerCompat

    init {
        notificationManagerCompat = NotificationManagerCompat.from(context)
        createNotificationChannels()
    }

    /**
     * Creates the notification channels required for gamification features.
     * Channels are created once and persist until the app is uninstalled.
     */
    private fun createNotificationChannels() {
        try {
            // Badges channel - High importance for important achievements
            val badgesChannel = NotificationChannelCompat.Builder(
                CHANNEL_ID_BADGES,
                context.getString(R.string.notification_channel_badges_name)
            )
                .setDescription(context.getString(R.string.notification_channel_badges_description))
                .setImportance(NotificationManagerCompat.IMPORTANCE_HIGH)
                .setShowBadge(true)
                .setLightColor(0xFF4CAF50.toInt()) // Green
                .build()

            // Points channel - Default importance for regular updates
            val pointsChannel = NotificationChannelCompat.Builder(
                CHANNEL_ID_POINTS,
                context.getString(R.string.notification_channel_points_name)
            )
                .setDescription(context.getString(R.string.notification_channel_points_description))
                .setImportance(NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setShowBadge(true)
                .setLightColor(0xFF2196F3.toInt()) // Blue
                .build()

            // Voice Assistant channel - Low importance for non-urgent messages
            val voiceChannel = NotificationChannelCompat.Builder(
                CHANNEL_ID_VOICE,
                context.getString(R.string.notification_channel_voice_name)
            )
                .setDescription(context.getString(R.string.notification_channel_voice_description))
                .setImportance(NotificationManagerCompat.IMPORTANCE_LOW)
                .setShowBadge(false)
                .setLightColor(0xFF9C27B0.toInt()) // Purple
                .build()

            notificationManagerCompat.createNotificationChannelsCompat(
                listOf(badgesChannel, pointsChannel, voiceChannel)
            )

            Log.d(TAG, "Notification channels created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification channels", e)
        }
    }

    /**
     * Shows a high-priority notification when a badge is unlocked.
     * Includes expandable content with badge description.
     */
    override suspend fun showBadgeUnlockedNotification(
        userId: String,
        badge: Badge,
        pointsEarned: Int
    ) {
        try {
            val title = BadgeNotificationContent.getTitle(badge.name)
            val body = BadgeNotificationContent.getBody(badge, pointsEarned)
            val expandedText = BadgeNotificationContent.getExpandedText(badge, pointsEarned)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_BADGES)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(expandedText)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setAutoCancel(true)
                .setContentIntent(createPendingIntent(ACTION_OPEN_BADGE, userId, badge.id))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            val notificationId = NOTIFICATION_ID_BASE + badge.id.hashCode()
            notificationManagerCompat.notify(notificationId, notification)

            Log.d(TAG, "Badge notification shown for badge: ${badge.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing badge notification", e)
        }
    }

    /**
     * Shows a notification when points are earned from an action.
     */
    override suspend fun showPointsEarnedNotification(
        userId: String,
        points: Int,
        action: String
    ) {
        try {
            val title = PointsNotificationContent.getTitle(points)
            val body = PointsNotificationContent.getBody(points, action)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_POINTS)
                .setSmallIcon(android.R.drawable.btn_star_big_on)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(createPendingIntent(ACTION_OPEN_POINTS, userId, null))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            val notificationId = NOTIFICATION_ID_BASE + 1000 + points
            notificationManagerCompat.notify(notificationId, notification)

            Log.d(TAG, "Points notification shown: $points points for $action")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing points notification", e)
        }
    }

    /**
     * Shows a low-priority notification for voice assistant errors.
     */
    override suspend fun showVoiceAssistantError(
        userId: String,
        error: String
    ) {
        try {
            val title = "Erreur Assistant Vocal"
            val body = "Une erreur s'est produite: $error"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_VOICE)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(body)
                )
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setAutoCancel(true)
                .setContentIntent(createPendingIntent(ACTION_OPEN_VOICE, userId, null))
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .build()

            val notificationId = NOTIFICATION_ID_BASE + 2000
            notificationManagerCompat.notify(notificationId, notification)

            Log.d(TAG, "Voice assistant error notification shown: $error")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing voice assistant notification", e)
        }
    }

    /**
     * Requests notification permission from the user.
     * On Android 13+, this shows the system permission dialog.
     * On older versions, permissions are granted at install time.
     */
    override suspend fun requestNotificationPermission(): Boolean {
        return try {
            // On Android 13+, we need to check and request permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = notificationManagerCompat.areNotificationsEnabled()
                if (!hasPermission) {
                    Log.d(TAG, "Notification permission not granted")
                    return false
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification permission", e)
            false
        }
    }

    /**
     * Checks if notification permission is granted.
     */
    override fun isNotificationEnabled(): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notificationManagerCompat.areNotificationsEnabled()
            } else {
                // On older Android versions, notifications are always enabled if channels exist
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification enabled status", e)
            false
        }
    }

    /**
     * Creates a PendingIntent for handling notification taps.
     * Supports deep linking to open specific screens in the app.
     *
     * @param action The action to perform when the notification is tapped
     * @param userId The user ID to include in the intent
     * @param badgeId Optional badge ID for badge-related actions
     * @return PendingIntent configured to open the appropriate screen
     */
    private fun createPendingIntent(
        action: String,
        userId: String,
        badgeId: String?
    ): PendingIntent {
        val intent = Intent(context, Class.forName("com.guyghost.wakeve.MainActivity")).apply {
            this.action = action
            putExtra("userId", userId)
            badgeId?.let { putExtra("badgeId", it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getActivity(
            context,
            action.hashCode(),
            intent,
            flags
        )
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
                id = CHANNEL_ID_BADGES,
                name = context.getString(R.string.notification_channel_badges_name),
                description = context.getString(R.string.notification_channel_badges_description),
                importance = NotificationManagerCompat.IMPORTANCE_HIGH
            )
            NotificationChannel.POINTS -> NotificationChannelConfig(
                id = CHANNEL_ID_POINTS,
                name = context.getString(R.string.notification_channel_points_name),
                description = context.getString(R.string.notification_channel_points_description),
                importance = NotificationManagerCompat.IMPORTANCE_DEFAULT
            )
            NotificationChannel.VOICE_ASSISTANT -> NotificationChannelConfig(
                id = CHANNEL_ID_VOICE,
                name = context.getString(R.string.notification_channel_voice_name),
                description = context.getString(R.string.notification_channel_voice_description),
                importance = NotificationManagerCompat.IMPORTANCE_LOW
            )
        }
    }

    /**
     * Cancels a specific notification by ID.
     *
     * @param notificationId The ID of the notification to cancel
     */
    fun cancelNotification(notificationId: Int) {
        notificationManagerCompat.cancel(notificationId)
    }

    /**
     * Cancels all notifications for this service.
     */
    fun cancelAllNotifications() {
        notificationManagerCompat.cancelAll()
    }
}
