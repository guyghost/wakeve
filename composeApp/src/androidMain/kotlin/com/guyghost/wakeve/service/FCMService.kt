package com.guyghost.wakeve.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.guyghost.wakeve.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

/**
 * Firebase Cloud Messaging Service for Android.
 *
 * Handles push notifications in foreground and background:
 * - Token generation and refresh (registered with backend)
 * - Data messages (app handles display)
 * - Notification messages (system handles display in background)
 * - Deep link navigation on notification tap
 */
class FCMService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val CHANNEL_ID = "wakeve_notifications"
    private val CHANNEL_NAME = "Wakeve Notifications"
    private val CHANNEL_DESCRIPTION = "Notifications for Wakeve events and updates"

    companion object {
        private const val TAG = "FCMService"
        private const val PREF_FCM_TOKEN = "fcm_device_token"

        /**
         * Get the server base URL.
         */
        private fun getBaseUrl(): String {
            // TODO: Move to BuildConfig or remote config
            return "http://10.0.2.2:8080/api" // Android emulator localhost
        }

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

        /**
         * Get stored FCM token.
         */
        fun getStoredToken(context: Context): String? {
            return context.getSharedPreferences("wakeve_prefs", Context.MODE_PRIVATE)
                .getString(PREF_FCM_TOKEN, null)
        }

        /**
         * Register the stored FCM token with the backend.
         * Call this after successful login.
         *
         * @param context Application context
         * @param accessToken JWT access token for authentication
         */
        fun registerStoredTokenWithBackend(context: Context, accessToken: String) {
            val token = getStoredToken(context) ?: return
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    registerTokenWithBackendApi(token, accessToken)
                    Log.i(TAG, "Stored FCM token registered with backend after login")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to register stored token: ${e.message}", e)
                }
            }
        }

        /**
         * Unregister FCM token from backend on logout.
         *
         * @param accessToken JWT access token for authentication
         */
        fun unregisterTokenFromBackend(accessToken: String) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    val client = createHttpClient()
                    val response = client.delete("${getBaseUrl()}/notifications/unregister?platform=android") {
                        header("Authorization", "Bearer $accessToken")
                    }
                    Log.i(TAG, "Token unregistered from backend: ${response.status}")
                    client.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unregister token: ${e.message}", e)
                }
            }
        }

        private fun createHttpClient(): HttpClient {
            return HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        }

        /**
         * Register token with the backend API.
         */
        private suspend fun registerTokenWithBackendApi(token: String, accessToken: String) {
            val client = createHttpClient()
            try {
                val response = client.post("${getBaseUrl()}/notifications/register") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $accessToken")
                    setBody(Json.encodeToString(RegisterTokenBody(
                        token = token,
                        platform = "android"
                    )))
                }
                Log.i(TAG, "Token registration response: ${response.status}")
            } finally {
                client.close()
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
        Log.d(TAG, "Received FCM message from: ${remoteMessage.from}")

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message notification payload: ${it.body}")
            handleNotificationMessage(it, remoteMessage.data)
        }
    }

    /**
     * Called when a new token is generated or refreshed.
     *
     * This happens when:
     * - The app is installed for the first time
     * - The app is restored to a new device
     * - The user uninstalls/reinstalls the app
     * - The user clears app data
     * - The token is periodically refreshed by FCM
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token: ${token.take(10)}...")

        // Store token locally
        getSharedPreferences("wakeve_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_FCM_TOKEN, token)
            .apply()

        // Register token with backend if user is authenticated
        serviceScope.launch {
            try {
                registerTokenWithBackend(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register token: ${e.message}", e)
            }
        }
    }

    /**
     * Handle data-only messages.
     * These are always delivered to onMessageReceived (foreground + background).
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "Wakeve"
        val body = data["body"] ?: "New notification"
        val notificationId = data["notificationId"] ?: "unknown"
        val eventId = data["eventId"]
        val deepLink = data["deepLink"]

        Log.i(TAG, "Data message: title=$title, body=$body, eventId=$eventId")

        // Show notification
        showNotification(title, body, notificationId, eventId, deepLink)
    }

    /**
     * Handle notification messages (with title and body).
     * In background, system shows notification automatically.
     * In foreground, we handle it here.
     */
    private fun handleNotificationMessage(
        notification: RemoteMessage.Notification,
        data: Map<String, String>
    ) {
        val title = notification.title ?: "Wakeve"
        val body = notification.body ?: "New notification"
        val notificationId = data["notificationId"] ?: "unknown"
        val eventId = data["eventId"]
        val deepLink = data["deepLink"]

        Log.i(TAG, "Notification message: title=$title, body=$body")

        showNotification(title, body, notificationId, eventId, deepLink)
    }

    /**
     * Show a system notification with deep link intent.
     */
    private fun showNotification(
        title: String,
        body: String,
        notificationId: String,
        eventId: String?,
        deepLink: String? = null
    ) {
        // Create intent for deep link navigation
        val intent = when {
            deepLink != null -> {
                Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    this.data = android.net.Uri.parse(deepLink)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
            eventId != null -> {
                Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    this.data = android.net.Uri.parse("wakeve://event/$eventId")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
            else -> {
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
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

        // Show notification (with permission check)
        val notificationManager = NotificationManagerCompat.from(this)
        val canPostNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (canPostNotifications) {
            notificationManager.notify(notificationId.hashCode(), notification)
        } else {
            Log.w(TAG, "Notification permission not granted, skipping notification display")
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

            Log.d(TAG, "Notification channel created")
        }
    }

    /**
     * Register FCM token with backend.
     * Gets the access token from SharedPreferences and sends to /api/notifications/register.
     */
    private suspend fun registerTokenWithBackend(token: String) {
        // Get stored access token
        val accessToken = getSharedPreferences("wakeve_prefs", Context.MODE_PRIVATE)
            .getString("access_token", null)

        if (accessToken == null) {
            Log.i(TAG, "User not authenticated, storing token for later registration")
            return
        }

        registerTokenWithBackendApi(token, accessToken)
    }
}

/**
 * Request body for token registration.
 */
@Serializable
private data class RegisterTokenBody(
    val token: String,
    val platform: String
)
