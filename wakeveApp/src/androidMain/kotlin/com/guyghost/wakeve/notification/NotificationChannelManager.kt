package com.guyghost.wakeve.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Manager for creating and configuring notification channels (Android O+).
 *
 * This class handles the creation of notification channels with appropriate
 * importance levels, vibration patterns, and other settings for different
 * types of notifications.
 *
 * @property context Application context
 * @property notificationManager System notification manager
 */
class NotificationChannelManager(
    private val context: Context,
    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
) {
    companion object {
        private const val TAG = "NotificationChannelManager"

        /**
         * Notification channel IDs used throughout the app.
         */
        enum class ChannelId(val id: String) {
            /** Default notifications with standard importance */
            DEFAULT("wakeve_default"),

            /** High priority notifications (event invites, confirmations) */
            HIGH_PRIORITY("wakeve_high_priority"),

            /** Event-related notifications (updates, comments) */
            EVENTS("wakeve_events"),

            /** Reminder notifications (vote deadlines, meeting reminders) */
            REMINDERS("wakeve_reminders"),

            /** Progress notifications (uploads, downloads, sync) */
            PROGRESS("wakeve_progress")
        }

        /**
         * Get the appropriate channel ID for a notification type.
         *
         * @param type The notification type
         * @return The corresponding channel ID
         */
        fun getChannelId(type: com.guyghost.wakeve.notification.NotificationType): ChannelId {
            return when (type) {
                com.guyghost.wakeve.notification.NotificationType.EVENT_INVITE,
                com.guyghost.wakeve.notification.NotificationType.DATE_CONFIRMED,
                com.guyghost.wakeve.notification.NotificationType.SCENARIO_SELECTED -> ChannelId.HIGH_PRIORITY

                com.guyghost.wakeve.notification.NotificationType.VOTE_REMINDER,
                com.guyghost.wakeve.notification.NotificationType.VOTE_CLOSE_REMINDER,
                com.guyghost.wakeve.notification.NotificationType.DEADLINE_REMINDER,
                com.guyghost.wakeve.notification.NotificationType.MEETING_REMINDER -> ChannelId.REMINDERS

                com.guyghost.wakeve.notification.NotificationType.NEW_COMMENT,
                com.guyghost.wakeve.notification.NotificationType.MENTION,
                com.guyghost.wakeve.notification.NotificationType.COMMENT_REPLY,
                com.guyghost.wakeve.notification.NotificationType.NEW_SCENARIO -> ChannelId.EVENTS

                com.guyghost.wakeve.notification.NotificationType.PAYMENT_DUE,
                com.guyghost.wakeve.notification.NotificationType.EVENT_UPDATE -> ChannelId.DEFAULT
            }
        }
    }

    /**
     * Create all notification channels for the app.
     *
     * This method should be called during app initialization (e.g., in Application.onCreate()
     * or MainActivity.onCreate()) to ensure channels are created before any notifications are sent.
     *
     * Channels are only created on Android O (API 26+) and above.
     */
    fun createAllChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createDefaultChannel()
            createHighPriorityChannel()
            createEventsChannel()
            createRemindersChannel()
            createProgressChannel()
            Log.d(TAG, "All notification channels created")
        } else {
            Log.d(TAG, "Notification channels not required on Android ${Build.VERSION.SDK_INT}")
        }
    }

    /**
     * Create a custom notification channel with specific settings.
     *
     * @param id Unique channel ID
     * @param name User-visible channel name
     * @description User-visible channel description
     * @importance Importance level (see NotificationManager.IMPORTANCE_*)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel(
        id: String,
        name: String,
        description: String,
        importance: Int,
        enableVibration: Boolean = true,
        enableLights: Boolean = true,
        showBadge: Boolean = false
    ) {
        val channel = NotificationChannel(id, name, importance).apply {
            this.description = description
            enableVibration(enableVibration)
            enableLights(enableLights)
            setShowBadge(showBadge)
        }

        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Created custom notification channel: $id")
    }

    /**
     * Create the default notification channel.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createDefaultChannel() {
        val channel = NotificationChannel(
            ChannelId.DEFAULT.id,
            "Wakeve Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Default notifications from Wakeve"
            enableVibration(true)
            enableLights(true)
            setShowBadge(false)
        }

        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Created default notification channel")
    }

    /**
     * Create the high priority notification channel.
     *
     * Used for urgent notifications that require immediate attention:
     * - Event invitations
     * - Date confirmations
     * - Scenario selections
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createHighPriorityChannel() {
        val channel = NotificationChannel(
            ChannelId.HIGH_PRIORITY.id,
            "Important Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important notifications that require your attention"
            enableVibration(true)
            enableLights(true)
            setShowBadge(true)
        }

        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Created high priority notification channel")
    }

    /**
     * Create the events notification channel.
     *
     * Used for event-related updates:
     * - Event updates
     * - New comments
     * - Mentions
     * - New scenarios
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createEventsChannel() {
        val channel = NotificationChannel(
            ChannelId.EVENTS.id,
            "Event Updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Updates and activity in your events"
            enableVibration(true)
            enableLights(true)
            setShowBadge(true)
        }

        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Created events notification channel")
    }

    /**
     * Create the reminders notification channel.
     *
     * Used for reminder notifications:
     * - Vote reminders
     * - Deadline reminders
     * - Meeting reminders
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemindersChannel() {
        val channel = NotificationChannel(
            ChannelId.REMINDERS.id,
            "Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for upcoming events and deadlines"
            enableVibration(true)
            enableLights(true)
            setShowBadge(true)
        }

        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Created reminders notification channel")
    }

    /**
     * Create the progress notification channel.
     *
     * Used for ongoing operations:
     * - Uploads
     * - Downloads
     * - Sync operations
     *
     * These notifications are low importance to avoid disturbing the user.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createProgressChannel() {
        val channel = NotificationChannel(
            ChannelId.PROGRESS.id,
            "Progress Updates",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing operations and progress updates"
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }

        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Created progress notification channel")
    }

    /**
     * Delete a notification channel.
     *
     * @param channelId The channel ID to delete
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun deleteChannel(channelId: String) {
        notificationManager.deleteNotificationChannel(channelId)
        Log.d(TAG, "Deleted notification channel: $channelId")
    }

    /**
     * Check if a notification channel exists.
     *
     * @param channelId The channel ID to check
     * @return true if the channel exists, false otherwise
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun channelExists(channelId: String): Boolean {
        val channel = notificationManager.getNotificationChannel(channelId)
        return channel != null
    }
}
