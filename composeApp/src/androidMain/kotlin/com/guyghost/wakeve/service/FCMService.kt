package com.guyghost.wakeve.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.guyghost.wakeve.AndroidDatabaseFactory
import com.guyghost.wakeve.MainActivity
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.deeplink.parseDeepLinkParts
import com.guyghost.wakeve.notification.NotificationChannelManager
import com.guyghost.wakeve.notification.NotificationPriority
import com.guyghost.wakeve.notification.NotificationType
import com.guyghost.wakeve.notification.getPriority
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.net.URI

internal fun validateBackendHttpSuccess(
    operation: String,
    status: HttpStatusCode,
    responseBody: String = ""
): Result<Unit> {
    return if (status.value in 200..299) {
        Result.success(Unit)
    } else {
        val bodySuffix = responseBody
            .takeIf { it.isNotBlank() }
            ?.let { ": $it" }
            .orEmpty()
        Result.failure(IllegalStateException("$operation failed with status $status$bodySuffix"))
    }
}

internal sealed interface FcmTokenRegistrationDecision {
    data class Register(
        val token: String,
        val accessToken: String
    ) : FcmTokenRegistrationDecision

    data object MissingToken : FcmTokenRegistrationDecision
    data object MissingAccessToken : FcmTokenRegistrationDecision
}

internal fun resolveFcmTokenRegistrationDecision(
    storedToken: String?,
    accessToken: String?
): FcmTokenRegistrationDecision {
    val normalizedToken = storedToken?.trim().orEmpty()
    if (normalizedToken.isBlank()) {
        return FcmTokenRegistrationDecision.MissingToken
    }

    val normalizedAccessToken = accessToken?.trim().orEmpty()
    if (normalizedAccessToken.isBlank()) {
        return FcmTokenRegistrationDecision.MissingAccessToken
    }

    return FcmTokenRegistrationDecision.Register(
        token = normalizedToken,
        accessToken = normalizedAccessToken
    )
}

internal fun resolveNotificationType(data: Map<String, String>): NotificationType {
    return data["type"]
        ?.trim()
        ?.uppercase()
        ?.let { runCatching { NotificationType.valueOf(it) }.getOrNull() }
        ?: NotificationType.EVENT_UPDATE
}

internal fun resolveNotificationChannelId(data: Map<String, String>): String {
    return NotificationChannelManager.getChannelId(resolveNotificationType(data)).id
}

internal fun resolveNotificationPriority(data: Map<String, String>): Int {
    return when (resolveNotificationType(data).getPriority()) {
        NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
        NotificationPriority.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
        NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
        NotificationPriority.URGENT -> NotificationCompat.PRIORITY_MAX
    }
}

internal fun resolveNotificationDeepLink(data: Map<String, String>): String? {
    data["deepLink"]?.trim()
        ?.takeIf { it.isNotBlank() && isSupportedNotificationDeepLink(it) }
        ?.let { return it }

    val invitationCode = data["invitationCode"] ?: data["inviteCode"]
    if (!invitationCode.isNullOrBlank()) {
        return "wakeve://invite/$invitationCode"
    }

    val type = data["type"]?.uppercase().orEmpty()
    val meetingId = data["meetingId"]
    if (!meetingId.isNullOrBlank() && (type == "MEETING_REMINDER" || type == "MEETING_STARTING")) {
        return "wakeve://meeting/$meetingId"
    }

    val eventId = data["eventId"] ?: return null
    if (eventId.isBlank()) return null

    return when (type) {
        "VOTE_REMINDER",
        "VOTE_CLOSE_REMINDER",
        "POLL_REMINDER",
        "DEADLINE_REMINDER" -> "wakeve://poll/$eventId"
        else -> "wakeve://event/$eventId"
    }
}

private fun isSupportedNotificationDeepLink(rawDeepLink: String): Boolean {
    return try {
        val uri = URI(rawDeepLink)
        parseDeepLinkParts(
            scheme = uri.scheme,
            host = uri.host,
            pathSegments = uri.path
                ?.split("/")
                ?.filter { it.isNotBlank() }
                .orEmpty()
        ) != null
    } catch (e: Exception) {
        false
    }
}

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
            val decision = resolveFcmTokenRegistrationDecision(
                storedToken = getStoredToken(context),
                accessToken = accessToken
            )
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    when (decision) {
                        FcmTokenRegistrationDecision.MissingToken -> {
                            Log.i(TAG, "No stored FCM token to register after login")
                        }
                        FcmTokenRegistrationDecision.MissingAccessToken -> {
                            Log.w(TAG, "Cannot register stored FCM token without access token")
                        }
                        is FcmTokenRegistrationDecision.Register -> {
                            registerTokenWithBackendApi(decision.token, decision.accessToken)
                            Log.i(TAG, "Stored FCM token registered with backend after login")
                        }
                    }
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
                    try {
                        val response = client.delete("${getBaseUrl()}/notifications/unregister?platform=android") {
                            header("Authorization", "Bearer $accessToken")
                        }
                        validateBackendHttpSuccess(
                            operation = "FCM token unregistration",
                            status = response.status,
                            responseBody = response.bodyAsText()
                        ).getOrThrow()
                        Log.i(TAG, "Token unregistered from backend")
                    } finally {
                        client.close()
                    }
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
                validateBackendHttpSuccess(
                    operation = "FCM token registration",
                    status = response.status,
                    responseBody = response.bodyAsText()
                ).getOrThrow()
                Log.i(TAG, "Token registered with backend")
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

        val data = remoteMessage.data
        val notification = remoteMessage.notification
        val title = data["title"] ?: notification?.title ?: "Wakeve"
        val body = data["body"] ?: notification?.body ?: "Nouvelle notification"
        val notificationId = data["notificationId"] ?: remoteMessage.messageId ?: UUID.randomUUID().toString()
        val notificationType = resolveNotificationType(data)
        val eventId = data["eventId"]
        val deepLink = resolveNotificationDeepLink(data)

        Log.i(TAG, "FCM payload: title=$title, body=$body, eventId=$eventId")

        persistNotification(notificationId, title, body, notificationType, data)
        showNotification(title, body, notificationId, notificationType, eventId, deepLink)
    }

    private fun persistNotification(
        notificationId: String,
        title: String,
        body: String,
        notificationType: NotificationType,
        data: Map<String, String>
    ) {
        val userId = data["userId"]
            ?: data["recipientUserId"]
            ?: getSharedPreferences("wakeve_prefs", Context.MODE_PRIVATE).getString("user_id", null)

        if (userId.isNullOrBlank()) {
            Log.w(TAG, "Cannot persist notification $notificationId without userId")
            return
        }

        serviceScope.launch {
            try {
                val database = DatabaseProvider.getDatabase(AndroidDatabaseFactory(applicationContext))
                val existing = database.notificationQueries.getNotificationById(notificationId).executeAsOneOrNull()
                if (existing != null) {
                    return@launch
                }

                val nowMs = Clock.System.now().toEpochMilliseconds()
                database.notificationQueries.insertNotification(
                    id = notificationId,
                    user_id = userId,
                    type = notificationType.name,
                    title = title,
                    body = body,
                    data_ = Json.encodeToString(data),
                    created_at = data["createdAt"]?.toLongOrNull() ?: nowMs,
                    sent_at = data["sentAt"]?.toLongOrNull() ?: nowMs
                )
                Log.i(TAG, "Persisted FCM notification $notificationId for user $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist FCM notification $notificationId: ${e.message}", e)
            }
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
     * Show a system notification with deep link intent.
     */
    private fun showNotification(
        title: String,
        body: String,
        notificationId: String,
        notificationType: NotificationType,
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
        val notification = NotificationCompat.Builder(this, NotificationChannelManager.getChannelId(notificationType).id)
            .setSmallIcon(com.guyghost.wakeve.R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(resolveNotificationPriority(mapOf("type" to notificationType.name)))
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
        NotificationChannelManager(this).createAllChannels()
    }

    /**
     * Register FCM token with backend.
     * Gets the access token from SharedPreferences and sends to /api/notifications/register.
     */
    private suspend fun registerTokenWithBackend(token: String) {
        // Get stored access token
        val accessToken = getSharedPreferences("wakeve_prefs", Context.MODE_PRIVATE)
            .getString("access_token", null)

        when (val decision = resolveFcmTokenRegistrationDecision(token, accessToken)) {
            FcmTokenRegistrationDecision.MissingToken -> {
                Log.w(TAG, "Cannot register blank FCM token")
            }
            FcmTokenRegistrationDecision.MissingAccessToken -> {
                Log.i(TAG, "User not authenticated, storing token for later registration")
            }
            is FcmTokenRegistrationDecision.Register -> {
                registerTokenWithBackendApi(decision.token, decision.accessToken)
            }
        }
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
