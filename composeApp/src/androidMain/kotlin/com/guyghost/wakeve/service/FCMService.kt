package com.guyghost.wakeve.service

import android.Manifest
import android.app.Activity
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
import com.guyghost.wakeve.BuildConfig
import com.guyghost.wakeve.MainActivity
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.deeplink.normalizeDeepLinkPathSegment
import com.guyghost.wakeve.deeplink.parseDeepLinkParts
import com.guyghost.wakeve.notification.NotificationChannelManager
import com.guyghost.wakeve.notification.NotificationPriority
import com.guyghost.wakeve.notification.QuietTime
import com.guyghost.wakeve.notification.NotificationType
import com.guyghost.wakeve.notification.getPriority
import com.guyghost.wakeve.notification.isUrgent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
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
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.net.URI
import java.net.URLDecoder
import java.util.Calendar

internal fun validateBackendHttpSuccess(
    operation: String,
    status: HttpStatusCode
): Result<Unit> {
    return if (status.value in 200..299) {
        Result.success(Unit)
    } else {
        Result.failure(IllegalStateException("$operation failed with status $status"))
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

internal enum class NotificationPermissionRequestDecision {
    REQUEST,
    ALREADY_GRANTED,
    NOT_REQUIRED,
    MISSING_ACTIVITY
}

internal data class FcmNotificationDisplayOptions(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

internal data class FcmNotificationText(
    val title: String,
    val body: String
)

internal data class FcmNotificationLogSummary(
    val type: NotificationType,
    val hasEventContext: Boolean,
    val hasDeepLink: Boolean,
    val hasPayloadTitle: Boolean,
    val hasPayloadBody: Boolean
)

internal fun fcmStoredTokenRegistrationFailureLogMessage(): String =
    "Failed to register stored FCM token"

internal fun fcmTokenUnregistrationFailureLogMessage(): String =
    "Failed to unregister FCM token"

internal fun fcmNotificationPreferencesReadFailureLogMessage(): String =
    "Failed to read notification preferences"

internal fun fcmTokenRegistrationFailureLogMessage(): String =
    "Failed to register FCM token"

internal fun fcmNotificationPersistenceFailureLogMessage(): String =
    "Failed to persist FCM notification"

private data class LocalNotificationPreferenceDecision(
    val shouldPersist: Boolean,
    val shouldDisplay: Boolean,
    val displayOptions: FcmNotificationDisplayOptions = FcmNotificationDisplayOptions()
)

private const val MAX_FCM_TITLE_LENGTH = 80
private const val MAX_FCM_BODY_LENGTH = 240
private const val MAX_FCM_NOTIFICATION_ID_LENGTH = 120
private const val EPOCH_SECONDS_UPPER_BOUND = 9_999_999_999L
private const val MAX_FCM_TIMESTAMP_FUTURE_SKEW_MS = 5 * 60 * 1000L
private const val DEFAULT_ANDROID_SERVER_URL = "https://api.wakeve.app"

internal fun resolveFcmBackendApiBaseUrl(serverUrl: String?): String {
    val normalized = serverUrl
        ?.trim()
        ?.trimEnd('/')
        ?.takeIf { it.isNotBlank() }
        ?: DEFAULT_ANDROID_SERVER_URL

    return if (normalized.endsWith("/api")) {
        normalized
    } else {
        "$normalized/api"
    }
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

internal fun resolveFcmNotificationId(
    payloadNotificationId: String?,
    messageId: String?,
    fallbackId: String = UUID.randomUUID().toString()
): String {
    return sequenceOf(payloadNotificationId, messageId, fallbackId)
        .map { it?.trim().orEmpty() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
        .let { normalized ->
            if (normalized.length <= MAX_FCM_NOTIFICATION_ID_LENGTH) {
                normalized
            } else {
                normalized.take(MAX_FCM_NOTIFICATION_ID_LENGTH)
            }
        }
}

internal fun resolveFcmDisplayNotificationId(
    notificationId: String,
    notificationType: NotificationType,
    eventId: String?,
    resolvedDeepLink: String? = null
): Int {
    val normalizedEventId = normalizeDeepLinkPathSegment(eventId)
        ?: resolveFcmDisplayEventIdFromDeepLink(resolvedDeepLink)
    val replacementKey = if (normalizedEventId != null && shouldReplaceDisplayedNotification(notificationType)) {
        "fcm-display:${notificationType.name}:$normalizedEventId"
    } else {
        notificationId
    }

    return replacementKey.hashCode()
}

private fun resolveFcmDisplayEventIdFromDeepLink(resolvedDeepLink: String?): String? {
    val normalizedDeepLink = resolvedDeepLink?.trim().orEmpty()
    if (normalizedDeepLink.isBlank()) {
        return null
    }

    return runCatching {
        val uri = URI(normalizedDeepLink)
        val pathSegments = uri.path
            ?.split("/")
            ?.filter { it.isNotBlank() }
            .orEmpty()

        when (uri.host) {
            "event" -> normalizeDeepLinkPathSegment(pathSegments.firstOrNull())
            "poll" -> normalizeDeepLinkPathSegment(pathSegments.singleOrNull())
            else -> null
        }
    }.getOrNull()
}

private fun shouldReplaceDisplayedNotification(notificationType: NotificationType): Boolean {
    return when (notificationType) {
        NotificationType.NEW_COMMENT,
        NotificationType.COMMENT_REPLY,
        NotificationType.EVENT_UPDATE,
        NotificationType.VOTE_REMINDER,
        NotificationType.VOTE_CLOSE_REMINDER,
        NotificationType.DEADLINE_REMINDER -> true

        NotificationType.EVENT_INVITE,
        NotificationType.DATE_CONFIRMED,
        NotificationType.NEW_SCENARIO,
        NotificationType.SCENARIO_SELECTED,
        NotificationType.MENTION,
        NotificationType.MEETING_REMINDER,
        NotificationType.PAYMENT_DUE -> false
    }
}

internal fun resolveFcmNotificationText(
    notificationType: NotificationType,
    dataTitle: String?,
    notificationTitle: String?,
    dataBody: String?,
    notificationBody: String?
): FcmNotificationText {
    val fallback = fallbackFcmNotificationText(notificationType)
    return FcmNotificationText(
        title = normalizeFcmNotificationText(
            value = dataTitle ?: notificationTitle,
            fallback = fallback.title,
            maxLength = MAX_FCM_TITLE_LENGTH
        ),
        body = normalizeFcmNotificationText(
            value = dataBody ?: notificationBody,
            fallback = fallback.body,
            maxLength = MAX_FCM_BODY_LENGTH
        )
    )
}

private fun fallbackFcmNotificationText(notificationType: NotificationType): FcmNotificationText {
    return when (notificationType) {
        NotificationType.EVENT_INVITE -> FcmNotificationText(
            title = "Invitation Wakeve",
            body = "Tu as une nouvelle invitation"
        )
        NotificationType.VOTE_REMINDER -> FcmNotificationText(
            title = "Vote en attente",
            body = "Vote pour aider le groupe a choisir"
        )
        NotificationType.DATE_CONFIRMED -> FcmNotificationText(
            title = "Date confirmee",
            body = "La date de l'evenement est validee"
        )
        NotificationType.NEW_SCENARIO -> FcmNotificationText(
            title = "Nouveau scenario",
            body = "Une nouvelle option est proposee"
        )
        NotificationType.SCENARIO_SELECTED -> FcmNotificationText(
            title = "Scenario selectionne",
            body = "Le groupe a choisi une option finale"
        )
        NotificationType.NEW_COMMENT -> FcmNotificationText(
            title = "Nouveau commentaire",
            body = "Une nouvelle discussion t'attend"
        )
        NotificationType.MENTION -> FcmNotificationText(
            title = "Tu as ete mentionne",
            body = "Quelqu'un attend ton attention"
        )
        NotificationType.MEETING_REMINDER -> FcmNotificationText(
            title = "Reunion bientot",
            body = "Une reunion commence bientot"
        )
        NotificationType.PAYMENT_DUE -> FcmNotificationText(
            title = "Paiement a regler",
            body = "Un paiement est en attente"
        )
        NotificationType.EVENT_UPDATE -> FcmNotificationText(
            title = "Evenement mis a jour",
            body = "Des informations ont change"
        )
        NotificationType.VOTE_CLOSE_REMINDER -> FcmNotificationText(
            title = "Vote bientot cloture",
            body = "Il reste peu de temps pour voter"
        )
        NotificationType.DEADLINE_REMINDER -> FcmNotificationText(
            title = "Echeance proche",
            body = "Une echeance approche"
        )
        NotificationType.COMMENT_REPLY -> FcmNotificationText(
            title = "Nouvelle reponse",
            body = "Quelqu'un a repondu a la discussion"
        )
    }
}

internal fun summarizeFcmNotificationForLog(
    data: Map<String, String>,
    notificationType: NotificationType,
    resolvedDeepLink: String?
): FcmNotificationLogSummary {
    return FcmNotificationLogSummary(
        type = notificationType,
        hasEventContext = normalizeDeepLinkPathSegment(data["eventId"]) != null,
        hasDeepLink = !resolvedDeepLink.isNullOrBlank(),
        hasPayloadTitle = !data["title"].isNullOrBlank(),
        hasPayloadBody = !data["body"].isNullOrBlank()
    )
}

private fun normalizeFcmNotificationText(
    value: String?,
    fallback: String,
    maxLength: Int
): String {
    val normalized = value
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        .orEmpty()
        .ifBlank { fallback }

    return if (normalized.length <= maxLength) {
        normalized
    } else {
        normalized.take(maxLength - 3).trimEnd() + "..."
    }
}

internal fun resolveFcmNotificationTimestampMillis(
    rawTimestamp: String?,
    nowMs: Long,
    maxFutureSkewMs: Long = MAX_FCM_TIMESTAMP_FUTURE_SKEW_MS
): Long {
    val parsedTimestamp = parseFcmNotificationTimestampMillis(rawTimestamp) ?: return nowMs
    if (parsedTimestamp <= 0L) {
        return nowMs
    }

    return if (parsedTimestamp <= nowMs + maxFutureSkewMs) {
        parsedTimestamp
    } else {
        nowMs
    }
}

private fun parseFcmNotificationTimestampMillis(rawTimestamp: String?): Long? {
    val normalized = rawTimestamp?.trim().orEmpty()
    if (normalized.isBlank()) {
        return null
    }

    normalized.toLongOrNull()?.let { numericTimestamp ->
        return if (numericTimestamp in 1..EPOCH_SECONDS_UPPER_BOUND) {
            numericTimestamp * 1000L
        } else {
            numericTimestamp
        }
    }

    return runCatching { Instant.parse(normalized).toEpochMilliseconds() }.getOrNull()
}

internal fun resolveNotificationPermissionRequestDecision(
    sdkInt: Int,
    permissionGranted: Boolean,
    hasActivityContext: Boolean
): NotificationPermissionRequestDecision {
    return when {
        sdkInt < Build.VERSION_CODES.TIRAMISU -> NotificationPermissionRequestDecision.NOT_REQUIRED
        permissionGranted -> NotificationPermissionRequestDecision.ALREADY_GRANTED
        !hasActivityContext -> NotificationPermissionRequestDecision.MISSING_ACTIVITY
        else -> NotificationPermissionRequestDecision.REQUEST
    }
}

internal fun resolveNotificationType(data: Map<String, String>): NotificationType {
    return resolveNotificationTypeToken(data)
        ?.let(::normalizeNotificationTypeToken)
        ?.let(::resolveNotificationTypeAlias)
        ?.let { runCatching { NotificationType.valueOf(it) }.getOrNull() }
        ?: NotificationType.EVENT_UPDATE
}

private fun resolveNotificationTypeToken(data: Map<String, String>): String? {
    return sequenceOf("type", "notificationType", "notification_type", "category")
        .map { key -> data[key]?.trim().orEmpty() }
        .firstOrNull { it.isNotBlank() }
}

internal fun normalizeNotificationTypeToken(value: String): String {
    return value
        .trim()
        .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
        .replace(Regex("[\\s-]+"), "_")
        .uppercase()
}

private fun resolveNotificationTypeAlias(value: String): String {
    return when (value) {
        "INVITE",
        "EVENT_INVITATION" -> NotificationType.EVENT_INVITE.name
        "EVENT_CONFIRMED" -> NotificationType.DATE_CONFIRMED.name
        "COMMENT_POSTED" -> NotificationType.NEW_COMMENT.name
        "MEETING_STARTING" -> NotificationType.MEETING_REMINDER.name
        "PAYMENT_REMINDER" -> NotificationType.PAYMENT_DUE.name
        "POLL_REMINDER" -> NotificationType.VOTE_REMINDER.name
        else -> value
    }
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

    val invitationCode = normalizeDeepLinkPathSegment(data["invitationCode"] ?: data["inviteCode"])
    if (invitationCode != null) {
        return "wakeve://invite/$invitationCode"
    }

    val type = resolveNotificationType(data)
    val meetingId = normalizeDeepLinkPathSegment(data["meetingId"])
    if (meetingId != null && type == NotificationType.MEETING_REMINDER) {
        return "wakeve://meeting/$meetingId"
    }

    val eventId = normalizeDeepLinkPathSegment(data["eventId"]) ?: return null

    return when (type) {
        NotificationType.VOTE_REMINDER,
        NotificationType.VOTE_CLOSE_REMINDER,
        NotificationType.DEADLINE_REMINDER -> "wakeve://poll/$eventId"
        else -> "wakeve://event/$eventId"
    }
}

internal fun resolveNotificationPersistenceUserId(
    data: Map<String, String>,
    storedUserId: String?
): String? {
    val normalizedStoredUserId = storedUserId?.trim().orEmpty()
    val payloadUserIds = sequenceOf(data["userId"], data["recipientUserId"])
        .map { it?.trim().orEmpty() }
        .filter { it.isNotBlank() }
        .toList()

    if (normalizedStoredUserId.isBlank()) {
        return payloadUserIds.firstOrNull()
    }

    return when {
        payloadUserIds.isEmpty() -> normalizedStoredUserId
        payloadUserIds.any { it == normalizedStoredUserId } -> normalizedStoredUserId
        else -> null
    }
}

internal fun shouldProcessNotificationForCurrentUser(
    data: Map<String, String>,
    storedUserId: String?
): Boolean {
    val normalizedStoredUserId = storedUserId?.trim().orEmpty()
    if (normalizedStoredUserId.isBlank()) {
        return true
    }

    val payloadUserIds = sequenceOf(data["userId"], data["recipientUserId"])
        .map { it?.trim().orEmpty() }
        .filter { it.isNotBlank() }
        .toList()

    return payloadUserIds.isEmpty() || payloadUserIds.any { it == normalizedStoredUserId }
}

internal fun shouldAcceptFcmNotificationPayload(
    notificationType: NotificationType,
    resolvedDeepLink: String?
): Boolean {
    return !resolvedDeepLink.isNullOrBlank()
}

internal fun shouldProcessNotificationForPreferences(
    notificationType: NotificationType,
    enabledTypesJson: String?,
    quietHoursStart: String? = null,
    quietHoursEnd: String? = null,
    currentTime: Instant = Clock.System.now(),
    currentMinuteOfDay: Int? = null
): Boolean {
    return resolveFcmNotificationPreferenceDecision(
        notificationType = notificationType,
        enabledTypesJson = enabledTypesJson,
        quietHoursStart = quietHoursStart,
        quietHoursEnd = quietHoursEnd,
        currentTime = currentTime,
        currentMinuteOfDay = currentMinuteOfDay
    ).shouldDisplay
}

internal data class FcmNotificationPreferenceDecision(
    val shouldPersist: Boolean,
    val shouldDisplay: Boolean
)

internal fun resolveFcmNotificationPreferenceDecision(
    notificationType: NotificationType,
    enabledTypesJson: String?,
    quietHoursStart: String? = null,
    quietHoursEnd: String? = null,
    currentTime: Instant = Clock.System.now(),
    currentMinuteOfDay: Int? = null
): FcmNotificationPreferenceDecision {
    val rawTypes = enabledTypesJson?.let { raw ->
        runCatching { Json.decodeFromString<List<String>>(raw) }.getOrNull()
    } ?: return FcmNotificationPreferenceDecision(shouldPersist = true, shouldDisplay = true)

    val enabledTypes = rawTypes
        .mapNotNull { raw -> runCatching { NotificationType.valueOf(raw) }.getOrNull() }
        .toSet()

    val typeEnabled = when {
        rawTypes.isNotEmpty() && enabledTypes.isEmpty() -> true
        else -> notificationType in enabledTypes
    }

    if (!typeEnabled) {
        return FcmNotificationPreferenceDecision(shouldPersist = false, shouldDisplay = false)
    }

    if (notificationType.isUrgent()) {
        return FcmNotificationPreferenceDecision(shouldPersist = true, shouldDisplay = true)
    }

    val start = quietHoursStart?.let(QuietTime::fromString)
    val end = quietHoursEnd?.let(QuietTime::fromString)
    val inQuietHours = if (start != null && end != null) {
        isInQuietHours(
            currentMinuteOfDay = currentMinuteOfDay ?: currentUtcMinuteOfDay(currentTime),
            start = start,
            end = end
        )
    } else {
        false
    }

    return FcmNotificationPreferenceDecision(
        shouldPersist = true,
        shouldDisplay = !inQuietHours
    )
}

internal fun resolveFcmNotificationDisplayOptions(
    soundEnabled: Long?,
    vibrationEnabled: Long?
): FcmNotificationDisplayOptions {
    return FcmNotificationDisplayOptions(
        soundEnabled = soundEnabled?.let { it != 0L } ?: true,
        vibrationEnabled = vibrationEnabled?.let { it != 0L } ?: true
    )
}

internal fun resolveFcmNotificationDefaults(options: FcmNotificationDisplayOptions): Int {
    var defaults = 0
    if (options.soundEnabled) {
        defaults = defaults or NotificationCompat.DEFAULT_SOUND
    }
    if (options.vibrationEnabled) {
        defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
    }
    return defaults
}

private fun isInQuietHours(
    currentMinuteOfDay: Int,
    start: QuietTime,
    end: QuietTime
): Boolean {
    val currentMinutes = currentMinuteOfDay.coerceIn(0, 1439)
    val startMinutes = start.hour * 60 + start.minute
    val endMinutes = end.hour * 60 + end.minute

    return if (startMinutes > endMinutes) {
        currentMinutes >= startMinutes || currentMinutes < endMinutes
    } else {
        currentMinutes in startMinutes..<endMinutes
    }
}

private fun currentUtcMinuteOfDay(currentTime: Instant): Int =
    (currentTime.toEpochMilliseconds() / 60000 % 1440).toInt()

private fun currentDeviceLocalMinuteOfDay(): Int {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}

internal fun enrichPersistedNotificationData(
    data: Map<String, String>,
    resolvedDeepLink: String?
): Map<String, String> {
    val payloadDeepLink = data["deepLink"]?.trim().orEmpty()
    val normalizedDeepLink = resolvedDeepLink?.trim().orEmpty()

    if (payloadDeepLink.isNotBlank() && isSupportedNotificationDeepLink(payloadDeepLink)) {
        return data + ("deepLink" to payloadDeepLink)
    }

    if (normalizedDeepLink.isNotBlank() && isSupportedNotificationDeepLink(normalizedDeepLink)) {
        return data + ("deepLink" to normalizedDeepLink)
    }

    return data - "deepLink"
}

internal fun resolveNotificationTapUri(
    resolvedDeepLink: String?,
    eventId: String?
): String? {
    val normalizedDeepLink = resolvedDeepLink?.trim().orEmpty()
    if (normalizedDeepLink.isNotBlank() && isSupportedNotificationDeepLink(normalizedDeepLink)) {
        return normalizedDeepLink
    }

    return normalizeDeepLinkPathSegment(eventId)
        ?.let { "wakeve://event/$it" }
}

private fun isSupportedNotificationDeepLink(rawDeepLink: String): Boolean {
    return try {
        val uri = URI(rawDeepLink)
        if (hasUnsupportedNotificationDeepLinkComponents(uri)) {
            return false
        }

        parseDeepLinkParts(
            scheme = uri.scheme,
            host = uri.host,
            pathSegments = uri.path
                ?.split("/")
                ?.filter { it.isNotBlank() }
                .orEmpty(),
            queryParameters = parseQueryParameters(uri.rawQuery)
        ) != null
    } catch (e: Exception) {
        false
    }
}

private fun hasUnsupportedNotificationDeepLinkComponents(uri: URI): Boolean {
    return uri.fragment != null || uri.userInfo != null || uri.port != -1
}

private fun parseQueryParameters(rawQuery: String?): Map<String, String> {
    return rawQuery
        ?.split("&")
        ?.mapNotNull { pair ->
            val index = pair.indexOf("=")
            if (index <= 0) {
                null
            } else {
                decodeQueryComponent(pair.substring(0, index)) to
                    decodeQueryComponent(pair.substring(index + 1))
            }
        }
        ?.toMap()
        .orEmpty()
}

private fun decodeQueryComponent(value: String): String =
    URLDecoder.decode(value, "UTF-8")

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
        private fun getBaseUrl(): String = resolveFcmBackendApiBaseUrl(BuildConfig.SERVER_URL)

        /**
         * Request notification permission for Android 13+.
         * Call this from an Activity.
         */
        fun requestNotificationPermission(context: Context): Boolean {
            val activity = context as? Activity
            val decision = resolveNotificationPermissionRequestDecision(
                sdkInt = Build.VERSION.SDK_INT,
                permissionGranted = isNotificationPermissionGranted(context),
                hasActivityContext = activity != null
            )

            return when (decision) {
                NotificationPermissionRequestDecision.REQUEST -> {
                    ActivityCompat.requestPermissions(
                        activity ?: return false,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        1001
                    )
                    true
                }
                NotificationPermissionRequestDecision.MISSING_ACTIVITY -> {
                    Log.w(TAG, "Cannot request POST_NOTIFICATIONS permission without an Activity context")
                    false
                }
                NotificationPermissionRequestDecision.ALREADY_GRANTED,
                NotificationPermissionRequestDecision.NOT_REQUIRED -> false
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
                    Log.e(TAG, fcmStoredTokenRegistrationFailureLogMessage(), e)
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
                            status = response.status
                        ).getOrThrow()
                        Log.i(TAG, "Token unregistered from backend")
                    } finally {
                        client.close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, fcmTokenUnregistrationFailureLogMessage(), e)
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
                    status = response.status
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
        val storedUserId = getSharedPreferences("wakeve_prefs", Context.MODE_PRIVATE).getString("user_id", null)
        if (!shouldProcessNotificationForCurrentUser(data, storedUserId)) {
            Log.w(TAG, "Skipping FCM notification for a different user")
            return
        }

        val notification = remoteMessage.notification
        val notificationType = resolveNotificationType(data)
        val text = resolveFcmNotificationText(
            notificationType = notificationType,
            dataTitle = data["title"],
            notificationTitle = notification?.title,
            dataBody = data["body"],
            notificationBody = notification?.body
        )
        val notificationId = resolveFcmNotificationId(
            payloadNotificationId = data["notificationId"],
            messageId = remoteMessage.messageId
        )
        val eventId = data["eventId"]
        val deepLink = resolveNotificationDeepLink(data)
        if (!shouldAcceptFcmNotificationPayload(notificationType, deepLink)) {
            Log.i(TAG, "Skipping non-actionable FCM notification: $notificationType")
            return
        }

        val persistedData = enrichPersistedNotificationData(data, deepLink)
        val persistenceUserId = resolveNotificationPersistenceUserId(persistedData, storedUserId)
        val preferenceDecision = resolveLocalNotificationPreferenceDecision(persistenceUserId, notificationType)

        if (!preferenceDecision.shouldPersist) {
            Log.i(TAG, "Skipping disabled FCM notification type: $notificationType")
            return
        }

        Log.i(
            TAG,
            "FCM payload accepted: ${summarizeFcmNotificationForLog(data, notificationType, deepLink)}"
        )

        persistNotification(notificationId, text.title, text.body, notificationType, persistedData)
        if (preferenceDecision.shouldDisplay) {
            showNotification(
                title = text.title,
                body = text.body,
                notificationId = notificationId,
                notificationType = notificationType,
                eventId = eventId,
                deepLink = deepLink,
                displayOptions = preferenceDecision.displayOptions
            )
        } else {
            Log.i(TAG, "Persisted FCM notification without display due to quiet hours: $notificationType")
        }
    }

    private fun resolveLocalNotificationPreferenceDecision(
        userId: String?,
        notificationType: NotificationType
    ): LocalNotificationPreferenceDecision {
        val normalizedUserId = userId?.trim().orEmpty()
        if (normalizedUserId.isBlank()) {
            return LocalNotificationPreferenceDecision(
                shouldPersist = true,
                shouldDisplay = true
            )
        }

        return runCatching {
            val database = DatabaseProvider.getDatabase(AndroidDatabaseFactory(applicationContext))
            val preferences = database.userQueries.selectPreferencesByUserId(normalizedUserId).executeAsOneOrNull()
            val preferenceDecision = resolveFcmNotificationPreferenceDecision(
                notificationType = notificationType,
                enabledTypesJson = preferences?.enabled_types,
                quietHoursStart = preferences?.quiet_hours_start,
                quietHoursEnd = preferences?.quiet_hours_end,
                currentMinuteOfDay = currentDeviceLocalMinuteOfDay()
            )
            LocalNotificationPreferenceDecision(
                shouldPersist = preferenceDecision.shouldPersist,
                shouldDisplay = preferenceDecision.shouldDisplay,
                displayOptions = resolveFcmNotificationDisplayOptions(
                    soundEnabled = preferences?.sound_enabled,
                    vibrationEnabled = preferences?.vibration_enabled
                )
            )
        }.getOrElse { error ->
            Log.w(TAG, fcmNotificationPreferencesReadFailureLogMessage())
            LocalNotificationPreferenceDecision(
                shouldPersist = true,
                shouldDisplay = true
            )
        }
    }

    private fun persistNotification(
        notificationId: String,
        title: String,
        body: String,
        notificationType: NotificationType,
        data: Map<String, String>
    ) {
        val userId = resolveNotificationPersistenceUserId(
            data = data,
            storedUserId = getSharedPreferences("wakeve_prefs", Context.MODE_PRIVATE).getString("user_id", null)
        )

        if (userId == null) {
            Log.w(TAG, "Cannot persist FCM notification without userId")
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
                    created_at = resolveFcmNotificationTimestampMillis(data["createdAt"], nowMs),
                    sent_at = resolveFcmNotificationTimestampMillis(data["sentAt"], nowMs)
                )
                Log.i(TAG, "Persisted FCM notification for current user")
            } catch (e: Exception) {
                Log.e(TAG, fcmNotificationPersistenceFailureLogMessage())
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
        Log.d(TAG, "New FCM token received")

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
                Log.e(TAG, fcmTokenRegistrationFailureLogMessage(), e)
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
        deepLink: String? = null,
        displayOptions: FcmNotificationDisplayOptions = FcmNotificationDisplayOptions()
    ) {
        val tapUri = resolveNotificationTapUri(
            resolvedDeepLink = deepLink,
            eventId = eventId
        )
        val displayNotificationId = resolveFcmDisplayNotificationId(
            notificationId = notificationId,
            notificationType = notificationType,
            eventId = eventId,
            resolvedDeepLink = deepLink
        )

        // Create intent for deep link navigation
        val intent = when {
            tapUri != null -> {
                Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    this.data = android.net.Uri.parse(tapUri)
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
            displayNotificationId,
            intent,
            pendingIntentFlags
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, NotificationChannelManager.getChannelId(notificationType).id)
            .setSmallIcon(com.guyghost.wakeve.R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(resolveNotificationPriority(mapOf("type" to notificationType.name)))
            .setDefaults(resolveFcmNotificationDefaults(displayOptions))
            .setSilent(!displayOptions.soundEnabled && !displayOptions.vibrationEnabled)
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
            notificationManager.notify(displayNotificationId, notification)
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
