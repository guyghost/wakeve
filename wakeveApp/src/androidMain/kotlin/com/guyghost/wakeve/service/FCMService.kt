package com.guyghost.wakeve.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.guyghost.wakeve.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Firebase Cloud Messaging Service for Android.
 * Handles push notifications in foreground and background.
 */
class FCMService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val CHANNEL_ID = "wakeve_notifications"
    private val CHANNEL_NAME = "Wakeve Notifications"
    private val CHANNEL_DESCRIPTION = "Notifications for Wakeve events and updates"

    companion object {
        private const val TAG = "FCMService"

        /**
         * Request notification permission for Android 13+.
         * Call this from an Activity.
         */
        fun requestNotificationPermission(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    context as android.app.Activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        /**
         * Check if notification permission is granted.
         */
        fun isNotificationPermissionGranted(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * Called when a new message is received.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Napier.d(tag = TAG, message = "Received FCM message")

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Napier.d(tag = TAG, message = "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Napier.d(tag = TAG, message = "Message notification payload: ${it.body}")
            handleNotificationMessage(it, remoteMessage.data)
        }
    }

    /**
     * Called when a new token is generated or refreshed.
     */
    override fun onNewToken(token: String) {
        Napier.d(tag = TAG, message = "New FCM token: ${token.take(10)}...")

        // Register token with backend
        serviceScope.launch {
            try {
                registerTokenWithBackend(token)
            } catch (e: Exception) {
                Napier.e(tag = TAG, message = "Failed to register token: ${e.message}", throwable = e)
            }
        }
    }

    /**
     * Handle data-only messages.
     * These are used when app is in foreground.
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "Wakeve"
        val body = data["body"] ?: "New notification"
        val notificationId = data["notificationId"] ?: "unknown"
        val eventId = data["eventId"]

        Napier.i(tag = TAG, message = "Data message: title=$title, body=$body")

        // Show notification
        showNotification(title, body, notificationId, eventId)
    }

    /**
     * Handle notification messages (with title and body).
     * These are handled by system when app is in background.
     */
    private fun handleNotificationMessage(
        notification: RemoteMessage.Notification,
        data: Map<String, String>
    ) {
        val title = notification.title ?: "Wakeve"
        val body = notification.body ?: "New notification"
        val notificationId = data["notificationId"] ?: "unknown"
        val eventId = data["eventId"]

        Napier.i(tag = TAG, message = "Notification message: title=$title, body=$body")

        showNotification(title, body, notificationId, eventId)
    }

    /**
     * Show a system notification.
     */
    private fun showNotification(
        title: String,
        body: String,
        notificationId: String,
        eventId: String?
    ) {
        // Create intent for deep link
        val intent = if (eventId != null) {
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = android.net.Uri.parse("wakeve://event/$eventId")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId.hashCode(),
            intent,
            pendingIntentFlags
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.guyghost.wakeve.R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Show notification
        val notificationManager = NotificationManagerCompat.from(this)
        if (isNotificationPermissionGranted(this)) {
            notificationManager.notify(notificationId.hashCode(), notification)
        } else {
            Napier.w(tag = TAG, message = "Notification permission not granted, skipping notification")
        }
    }

    /**
     * Create notification channel (required for Android 8.0+).
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

            Napier.d(tag = TAG, message = "Notification channel created")
        }
    }

    /**
     * Register FCM token with backend.
     * In production, use Ktor client to call /api/notifications/register.
     */
    private suspend fun registerTokenWithBackend(token: String) {
        // TODO: Integrate with actual backend API
        // For now, this is a placeholder

        Napier.i(tag = TAG, message = "Registering token with backend...")
        Napier.d(tag = TAG, message = "Token (first 20 chars): ${token.take(20)}...")

        // Example implementation with Ktor client:
        /*
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        try {
            val response = client.post("http://your-server/api/notifications/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterTokenRequest(
                    userId = getUserId(), // Get from SessionManager
                    token = token,
                    platform = "android"
                ))
            }

            Napier.i(tag = TAG, message = "Token registered successfully")
        } catch (e: Exception) {
            Napier.e(tag = TAG, message = "Failed to register token: ${e.message}")
        } finally {
            client.close()
        }
        */
    }
}
