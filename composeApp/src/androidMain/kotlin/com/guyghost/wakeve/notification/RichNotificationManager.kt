package com.guyghost.wakeve.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager for rich notifications with images, actions, and progress indicators.
 *
 * This class provides methods to display:
 * - Rich notifications with big images (BigPictureStyle)
 * - Notifications with action buttons
 * - Progress notifications (indeterminate and determinate)
 *
 * Uses Coil for efficient image loading with caching and SVG support.
 *
 * @property context Application context
 * @property notificationManager System notification manager
 * @property channelManager Notification channel manager
 */
class RichNotificationManager(
    private val context: Context,
    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager,
    private val channelManager: NotificationChannelManager = NotificationChannelManager(context)
) {
    companion object {
        private const val TAG = "RichNotificationManager"

        /**
         * Default notification icon (small icon).
         */
        private const val DEFAULT_SMALL_ICON = R.drawable.ic_launcher_foreground
    }

    /**
     * Data class representing a notification action button.
     *
     * @property title Text to display on the action button
     * @property intent Intent to execute when action is clicked
     */
    data class NotificationAction(
        val title: String,
        val intent: Intent
    )

    /**
     * Show a rich notification with optional image and actions.
     *
     * This method creates a notification with the following features:
     * - BigPictureStyle with large image (optional)
     * - Action buttons (optional)
     * - Large icon (optional)
     * - Content intent for navigation (optional)
     *
     * @param notificationId Unique ID for the notification
     * @param title Notification title
     * @param message Notification body text
     * @param channelId Notification channel ID
     * @param largeIcon Large icon bitmap (optional)
     * @param bigPicture Big picture bitmap for BigPictureStyle (optional)
     * @param contentIntent Intent to execute when notification is tapped (optional)
     * @param actions List of action buttons (optional)
     * @param autoCancel Whether to cancel notification when tapped (default: true)
     */
    suspend fun showRichNotification(
        notificationId: Int,
        title: String,
        message: String,
        channelId: String,
        largeIcon: Bitmap? = null,
        bigPicture: Bitmap? = null,
        contentIntent: Intent? = null,
        actions: List<NotificationAction> = emptyList(),
        autoCancel: Boolean = true
    ) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(DEFAULT_SMALL_ICON)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(autoCancel)

        // Set large icon
        largeIcon?.let { builder.setLargeIcon(it) }

        // Set content intent (action on tap)
        contentIntent?.let {
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                it,
                pendingIntentFlags
            )
            builder.setContentIntent(pendingIntent)
        }

        // Set big picture style (expands notification with large image)
        bigPicture?.let {
            val style = NotificationCompat.BigPictureStyle()
                .bigPicture(it)
                .bigLargeIcon(null) // Don't show large icon in expanded view
            builder.setStyle(style)
        }

        // Add action buttons
        actions.forEach { action ->
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                action.title.hashCode(),
                action.intent,
                pendingIntentFlags
            )
            builder.addAction(
                NotificationCompat.Action.Builder(null, action.title, pendingIntent).build()
            )
        }

        // Build and show notification
        val notification = builder.build()
        try {
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "Rich notification shown: id=$notificationId, title=$title")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to show notification: ${e.message}", e)
        }
    }

    /**
     * Show a progress notification.
     *
     * Progress notifications can be indeterminate (ongoing operation with unknown duration)
     * or determinate (with specific progress value).
     *
     * @param notificationId Unique ID for the notification
     * @param title Notification title
     * @param message Notification body text
     * @param channelId Notification channel ID (should be PROGRESS channel)
     * @param indeterminate Whether progress is indeterminate (true) or determinate (false)
     * @param progress Progress value (only for determinate progress)
     * @param max Maximum progress value (only for determinate progress)
     * @param ongoing Whether notification is ongoing (cannot be swiped away)
     */
    suspend fun showProgressNotification(
        notificationId: Int,
        title: String,
        message: String,
        channelId: String,
        indeterminate: Boolean = true,
        progress: Int = 0,
        max: Int = 100,
        ongoing: Boolean = true
    ) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(DEFAULT_SMALL_ICON)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(ongoing)
            .setAutoCancel(false)

        if (indeterminate) {
            builder.setProgress(max, progress, true)
        } else {
            builder.setProgress(max, progress, false)
        }

        val notification = builder.build()
        try {
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "Progress notification shown: id=$notificationId, title=$title, indeterminate=$indeterminate")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to show progress notification: ${e.message}", e)
        }
    }

    /**
     * Update an existing progress notification.
     *
     * This method should be called to update the progress of an ongoing operation.
     *
     * @param notificationId Unique ID of the notification to update
     * @param title Updated title (optional, keeps existing if null)
     * @param message Updated message (optional, keeps existing if null)
     * @param channelId Notification channel ID
     * @param progress Current progress value
     * @param max Maximum progress value
     * @param indeterminate Whether progress is indeterminate
     */
    suspend fun updateProgressNotification(
        notificationId: Int,
        title: String? = null,
        message: String? = null,
        channelId: String,
        progress: Int,
        max: Int = 100,
        indeterminate: Boolean = false
    ) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(DEFAULT_SMALL_ICON)

        title?.let { builder.setContentTitle(it) }
        message?.let { builder.setContentText(it) }

        if (indeterminate) {
            builder.setProgress(max, progress, true)
        } else {
            builder.setProgress(max, progress, false)
        }

        val notification = builder.build()
        try {
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "Progress notification updated: id=$notificationId, progress=$progress/$max")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to update progress notification: ${e.message}", e)
        }
    }

    /**
     * Cancel a notification by its ID.
     *
     * @param notificationId Unique ID of the notification to cancel
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
        Log.d(TAG, "Notification cancelled: id=$notificationId")
    }

    /**
     * Cancel all notifications shown by the app.
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
        Log.d(TAG, "All notifications cancelled")
    }

    /**
     * Load a bitmap image from a URI using Coil.
     *
     * This method supports:
     * - HTTP/HTTPS URLs (with caching)
     * - Local file URIs
     * - Resource URIs
     * - SVG images
     *
     * @param uri URI of the image to load
     * @return Bitmap image, or null if loading failed
     */
    suspend fun loadImageBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val imageLoader = ImageLoader.Builder(context)
                .components {
                    add(SvgDecoder.Factory())
                }
                .build()

            val request = ImageRequest.Builder(context)
                .data(uri)
                .target { drawable ->
                    // Convert drawable to bitmap
                    if (drawable is android.graphics.drawable.BitmapDrawable) {
                        return@target drawable.bitmap
                    } else {
                        // For other drawable types, convert to bitmap
                        val bitmap = Bitmap.createBitmap(
                            drawable.intrinsicWidth.takeIf { it > 0 } ?: 100,
                            drawable.intrinsicHeight.takeIf { it > 0 } ?: 100,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        return@target bitmap
                    }
                }
                .build()

            val result = imageLoader.execute(request)
            (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load image from URI: $uri", e)
            null
        }
    }

    /**
     * Load a bitmap from a file path.
     *
     * Convenience method that converts file path to URI and calls loadImageBitmap.
     *
     * @param filePath Path to the image file
     * @return Bitmap image, or null if loading failed
     */
    suspend fun loadImageBitmap(filePath: String): Bitmap? {
        return loadImageBitmap(Uri.parse("file://$filePath"))
    }

    /**
     * Load a bitmap from a URL string.
     *
     * Convenience method that converts URL string to URI and calls loadImageBitmap.
     *
     * @param url URL of the image
     * @return Bitmap image, or null if loading failed
     */
    suspend fun loadImageBitmapFromUrl(url: String): Bitmap? {
        return loadImageBitmap(Uri.parse(url))
    }

    /**
     * Create a notification with inline reply action.
     *
     * This is useful for quick responses to notifications without opening the app.
     *
     * @param notificationId Unique ID for the notification
     * @param title Notification title
     * @param message Notification body text
     * @param channelId Notification channel ID
     * @param replyLabel Label for the reply button (e.g., "Reply")
     * @param replyIntent Intent to handle the reply
     */
    suspend fun showInlineReplyNotification(
        notificationId: Int,
        title: String,
        message: String,
        channelId: String,
        replyLabel: String = "Reply",
        replyIntent: Intent
    ) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(DEFAULT_SMALL_ICON)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val remoteInput = android.app.RemoteInput.Builder("key_text_reply").build()

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            replyIntent,
            pendingIntentFlags
        )

        val action = NotificationCompat.Action.Builder(
            null,
            replyLabel,
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()

        builder.addAction(action)

        val notification = builder.build()
        try {
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "Inline reply notification shown: id=$notificationId, title=$title")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to show inline reply notification: ${e.message}", e)
        }
    }
}
