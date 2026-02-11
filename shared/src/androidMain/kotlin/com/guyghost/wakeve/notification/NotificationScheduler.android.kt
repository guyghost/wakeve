package com.guyghost.wakeve.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.guyghost.wakeve.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock

/**
 * Android implementation of NotificationScheduler.
 *
 * Uses WorkManager with OneTimeWorkRequest for scheduling notifications.
 * Notifications are shown using Android's NotificationManager.
 *
 * ## Platform Specifics
 * - **Scheduling**: WorkManager OneTimeWorkRequest
 * - **Delivery**: NotificationCompat with PendingIntent for navigation
 * - **Persistence**: Notifications persist across app restarts
 * - **Channels**: Uses NotificationChannel (Android 8.0+)
 *
 * ## Permissions
 * - POST_NOTIFICATIONS (Android 13+): Required to show notifications
 */
actual class NotificationScheduler {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val CHANNEL_ID_EVENTS = "wakeve_events"
        private const val CHANNEL_ID_POLLS = "wakeve_polls"
        private const val NOTIFICATION_TAG = "wakeve_notification"

        // Work input data keys
        private const val KEY_NOTIFICATION_ID = "notification_id"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_EVENT_ID = "event_id"

        @Volatile
        private var instance: NotificationScheduler? = null

        actual fun getInstance(): NotificationScheduler {
            return instance ?: synchronized(this) {
                instance ?: createNotificationScheduler()
            }
        }

        actual fun initialize(context: Any?) {
            if (context is Context) {
                synchronized(this) {
                    if (instance == null) {
                        instance = NotificationScheduler().apply {
                            this.context = context
                            this.workManager = WorkManager.getInstance(context)
                            this.notificationManager =
                                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            createNotificationChannels()
                        }
                    }
                }
            }
        }

        private fun createNotificationScheduler(): NotificationScheduler {
            return NotificationScheduler()
        }
    }

    /**
     * Schedule event reminder using WorkManager.
     *
     * Creates a OneTimeWorkRequest that triggers NotificationWorker at specified time.
     */
    actual suspend fun scheduleEventReminder(
        eventId: String,
        title: String,
        body: String,
        scheduledTime: Instant
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val notificationId = generateNotificationId("event", eventId)
            val delayMillis = calculateDelayMillis(scheduledTime)

            if (delayMillis <= 0) {
                // Time already passed
                return@runCatching Result.success(Unit)
            }

            val workData = Data.Builder()
                .putString(KEY_NOTIFICATION_ID, notificationId)
                .putString(KEY_TITLE, title)
                .putString(KEY_BODY, body)
                .putString(KEY_EVENT_ID, eventId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setInputData(workData)
                .addTag(NOTIFICATION_TAG)
                .build()

            workManager.enqueueUniqueWork(
                notificationId,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            Result.success(Unit)
        }
    }

    /**
     * Schedule poll deadline reminder using WorkManager.
     *
     * Creates a OneTimeWorkRequest that triggers NotificationWorker at deadline time.
     */
    actual suspend fun schedulePollDeadlineReminder(
        pollId: String,
        eventId: String,
        title: String,
        body: String,
        deadlineTime: Instant
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val notificationId = generateNotificationId("poll", pollId)
            val delayMillis = calculateDelayMillis(deadlineTime)

            if (delayMillis <= 0) {
                // Deadline already passed
                return@runCatching Result.success(Unit)
            }

            val workData = Data.Builder()
                .putString(KEY_NOTIFICATION_ID, notificationId)
                .putString(KEY_TITLE, title)
                .putString(KEY_BODY, body)
                .putString(KEY_EVENT_ID, eventId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setInputData(workData)
                .addTag(NOTIFICATION_TAG)
                .build()

            workManager.enqueueUniqueWork(
                notificationId,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            Result.success(Unit)
        }
    }

    /**
     * Cancel a scheduled notification by work ID.
     */
    actual suspend fun cancelScheduledNotification(notificationId: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                workManager.cancelUniqueWork(notificationId)
                // Also cancel any pending notification that might have been shown
                notificationManager.cancel(notificationId.hashCode())
                Result.success(Unit)
            }
        }

    /**
     * Cancel all scheduled notifications.
     */
    actual suspend fun cancelAllScheduledNotifications(): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                workManager.cancelAllWorkByTag(NOTIFICATION_TAG)
                notificationManager.cancelAll()
                Result.success(Unit)
            }
        }

    /**
     * Create notification channels for Android 8.0+.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val eventsChannel = NotificationChannel(
                CHANNEL_ID_EVENTS,
                "Wakeve Events",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming events"
            }

            val pollsChannel = NotificationChannel(
                CHANNEL_ID_POLLS,
                "Wakeve Polls",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for poll deadlines"
            }

            notificationManager.createNotificationChannel(eventsChannel)
            notificationManager.createNotificationChannel(pollsChannel)
        }
    }

    /**
     * Calculate delay in milliseconds from now to target time.
     */
    private fun calculateDelayMillis(targetTime: Instant): Long {
        val now = Clock.System.now()
        val duration = targetTime - now
        return duration.inWholeMilliseconds
    }

    /**
     * Generate unique notification ID.
     */
    private fun generateNotificationId(type: String, id: String): String {
        return "$type-$id"
    }

    /**
     * Show notification immediately.
     *
     * Called by NotificationWorker when scheduled time is reached.
     */
    internal fun showNotification(
        notificationId: String,
        title: String,
        body: String,
        eventId: String
    ) {
        val channelId = if (notificationId.startsWith("poll")) {
            CHANNEL_ID_POLLS
        } else {
            CHANNEL_ID_EVENTS
        }

        val iconResId = if (notificationId.startsWith("poll")) {
            R.drawable.ic_points_black_24dp
        } else {
            R.drawable.ic_event
        }

        // Create intent for tapping notification (navigate to event)
        val intent = Intent(context, com.guyghost.wakeve.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("eventId", eventId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconResId)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId.hashCode(), notification)
    }
}

/**
 * Worker class for showing scheduled notifications.
 *
 * This worker is executed by WorkManager at the scheduled time
 * and displays the notification using NotificationManager.
 */
internal class NotificationWorker(
    context: Context,
    workerParams: androidx.work.WorkerParameters
) : androidx.work.Worker(context, workerParams) {

    companion object {
        private const val TAG = "NotificationWorker"
    }

    override fun doWork(): androidx.work.ListenableWorker.Result {
        try {
            val notificationId = inputData.getString(NotificationScheduler.KEY_NOTIFICATION_ID)
            val title = inputData.getString(NotificationScheduler.KEY_TITLE)
            val body = inputData.getString(NotificationScheduler.KEY_BODY)
            val eventId = inputData.getString(NotificationScheduler.KEY_EVENT_ID)

            if (notificationId != null && title != null && body != null && eventId != null) {
                // Access NotificationScheduler to show notification
                val scheduler = NotificationScheduler.getInstance()
                scheduler.showNotification(notificationId, title, body, eventId)
                return androidx.work.ListenableWorker.Result.success()
            } else {
                android.util.Log.e(TAG, "Invalid work data")
                return androidx.work.ListenableWorker.Result.failure()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error showing notification", e)
            return androidx.work.ListenableWorker.Result.failure()
        }
    }
}
