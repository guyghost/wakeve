package com.guyghost.wakeve.gamification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.guyghost.wakeve.R
import com.guyghost.wakeve.models.BadgeNotification
import com.guyghost.wakeve.models.BadgeType
import com.guyghost.wakeve.models.getNotificationTitle
import com.guyghost.wakeve.models.getDefaultMessage
import com.guyghost.wakeve.models.createDeepLink

/**
 * Android-specific implementation of badge notifications.
 * Uses NotificationCompat for compatibility with Android 13+.
 *
 * This service handles:
 * - Notification channels configuration (Android 8+)
 * - Badge number display on app icon
 * - Action buttons (Open, Dismiss)
 * - Sound and vibration feedback
 * - Notification ID tracking for updates
 *
 * Architecture: Imperative Shell - Handles platform-specific I/O operations.
 */
class AndroidBadgeNotificationService(
    private val context: Context
) : BadgeNotificationService {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val badgeNotificationId = BADGE_NOTIFICATION_ID
    
    companion object {
        private const val BADGE_NOTIFICATION_ID = 1000
        private const val CHANNEL_ID_BASE = "wakeve_notifications"
        private const val ACTION_DISMISS = "com.guyghost.wakeve.DISMISS_BADGE"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    init {
        createNotificationChannels()
    }

    /**
     * Creates all notification channels required for badge notifications.
     * Channels are required for Android 8+ (API 26+).
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_BASE,
                    "Notifications Wakeve",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications générales pour les événements"
                    enableVibration(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    "${CHANNEL_ID_BASE}_polls",
                    "Sondages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications pour les sondages d'événements"
                    enableVibration(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    "${CHANNEL_ID_BASE}_dates",
                    "Dates confirmées",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications pour les dates d'événements confirmées"
                    enableVibration(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    "${CHANNEL_ID_BASE}_scenarios",
                    "Scénarios",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications pour les scénarios d'événements"
                    enableVibration(false)
                    setShowBadge(true)
                },
                NotificationChannel(
                    "${CHANNEL_ID_BASE}_meetings",
                    "Réunions",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications pour les réunions planifiées"
                    enableVibration(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    "${CHANNEL_ID_BASE}_comments",
                    "Commentaires",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications pour les mentions dans les commentaires"
                    enableVibration(false)
                    setShowBadge(true)
                }
            )

            val manager = context.getSystemService(NotificationManager::class.java)
            channels.forEach { channel ->
                manager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Sends a badge notification to the system notification shade.
     *
     * @param notification The notification payload to display
     */
    override suspend fun sendBadgeNotification(notification: BadgeNotification) {
        val channelId = getChannelIdForType(notification.type)
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setSmallIcon(R.drawable.ic_notification_badge)
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        
        // Set badge count on app icon (Android 8+)
        notification.badgeCount?.let { count ->
            builder.setNumber(count)
            updateLauncherIconBadge(count)
        }
        
        // Set sound and vibration
        builder.setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
        
        // Add open action button
        notification.deepLink?.let { deepLink ->
            val openIntent = createOpenIntent(deepLink)
            val openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            builder.addAction(
                R.drawable.ic_open,
                context.getString(R.string.notification_action_view_badge),
                openPendingIntent
            )
        }
        
        // Add dismiss action button
        val dismissIntent = createDismissIntent(notification.id)
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notification.id.hashCode(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        builder.addAction(
            R.drawable.ic_dismiss,
            context.getString(R.string.notification_action_dismiss),
            dismissPendingIntent
        )
        
        // Build and show notification
        val androidNotification = builder.build()
        notificationManager.notify(badgeNotificationId, androidNotification)
    }

    /**
     * Clears a specific badge notification from the notification shade.
     *
     * @param notificationId The ID of the notification to clear
     */
    override suspend fun clearBadgeNotification(notificationId: String) {
        notificationManager.cancel(notificationId.hashCode())
    }

    /**
     * Updates the badge count displayed on the app launcher icon.
     *
     * @param count The badge count to display
     */
    override suspend fun updateBadgeCount(count: Int) {
        updateLauncherIconBadge(count)
    }

    /**
     * Maps a BadgeType to the corresponding notification channel ID.
     */
    private fun getChannelIdForType(type: BadgeType): String {
        return when (type) {
            BadgeType.EVENT_CREATED -> CHANNEL_ID_BASE
            BadgeType.POLL_OPENED, BadgeType.POLL_CLOSING_SOON -> "${CHANNEL_ID_BASE}_polls"
            BadgeType.DATE_CONFIRMED -> "${CHANNEL_ID_BASE}_dates"
            BadgeType.SCENARIO_UNLOCKED -> "${CHANNEL_ID_BASE}_scenarios"
            BadgeType.MEETING_SCHEDULED -> "${CHANNEL_ID_BASE}_meetings"
            BadgeType.COMMENT_MENTION -> "${CHANNEL_ID_BASE}_comments"
            BadgeType.EVENT_FINALIZED -> CHANNEL_ID_BASE
        }
    }

    /**
     * Creates an intent to open the app when notification is tapped.
     */
    private fun createOpenIntent(deepLink: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse(deepLink)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            setPackage(context.packageName)
        }
    }

    /**
     * Creates a broadcast intent for the dismiss action.
     */
    private fun createDismissIntent(notificationId: String): Intent {
        return Intent(ACTION_DISMISS).apply {
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            setPackage(context.packageName)
        }
    }

    /**
     * Updates the launcher icon badge with the given count.
     * This uses the system broadcast API for supported launchers.
     */
    private fun updateLauncherIconBadge(count: Int) {
        try {
            // Method 1: ShortcutBadger (supports most launchers)
            val shortcutBadgerClass = Class.forName("androidx.core.app.ShortcutBadger")
            val showBadgeMethod = shortcutBadgerClass.getMethod(
                "showBadge",
                Context::class.java,
                Int::class.java
            )
            showBadgeMethod.invoke(null, context, count)
        } catch (e: Exception) {
            // ShortcutBadger not available, try alternative methods
            try {
                // Method 2: Samsung launcher
                val intent = Intent("android.intent.action.BADGE_COUNT_UPDATE").apply {
                    putExtra("badge_count", count)
                    putExtra("badge_count_package", context.packageName)
                    putExtra("badge_count_class", getMainActivityClassName())
                }
                context.sendBroadcast(intent)
            } catch (e2: Exception) {
                // Badge not supported on this launcher, silently ignore
            }
        }
    }

    /**
     * Gets the main activity class name for badge updates.
     */
    private fun getMainActivityClassName(): String {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launchIntent?.component?.className ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Clears all badge notifications and resets the badge count.
     */
    suspend fun clearAllNotifications() {
        notificationManager.cancelAll()
        updateLauncherIconBadge(0)
    }
}

/**
 * Broadcast receiver for handling badge notification dismiss actions.
 * Receives the dismiss action broadcast and clears the notification.
 */
class BadgeDismissReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "com.guyghost.wakeve.DISMISS_BADGE") {
            val notificationId = intent.getStringExtra("notification_id") ?: return
            
            try {
                val manager = NotificationManagerCompat.from(context)
                manager.cancel(notificationId.hashCode())
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}
