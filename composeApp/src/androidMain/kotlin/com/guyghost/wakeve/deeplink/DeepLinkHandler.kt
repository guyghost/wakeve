@file:JvmName("AndroidNavigationDeepLinkParser")

package com.guyghost.wakeve.deeplink

import android.net.Uri
import android.util.Log
import androidx.navigation.NavController
import com.guyghost.wakeve.navigation.NOTIFICATIONS_FILTER_UNREAD
import com.guyghost.wakeve.navigation.Screen

/**
 * Deep link handler for Wakeve Android app.
 *
 * Handles deep link navigation for various Wakeve features:
 * - wakeve://event/{id} - Navigate to event details
 * - wakeve://poll/{eventId} - Navigate to poll voting
 * - wakeve://meeting/{meetingId} - Navigate to meeting details
 * - wakeve://invite/{token} - Handle invite tokens
 *
 * Pattern: Functional Core (DeepLinkParser) + Imperative Shell (DeepLinkHandler)
 */
sealed class AndroidNavigationDeepLink(val route: String) {
    data object EventCreate : AndroidNavigationDeepLink("event/create")
    data class EventDetail(val eventId: String) : AndroidNavigationDeepLink("event")
    data class PollVoting(val eventId: String) : AndroidNavigationDeepLink("poll")
    data class ParticipantManagement(val eventId: String) : AndroidNavigationDeepLink("participants")
    data class ScenarioList(val eventId: String) : AndroidNavigationDeepLink("scenarios")
    data class BudgetOverview(val eventId: String) : AndroidNavigationDeepLink("budget")
    data class MeetingList(val eventId: String) : AndroidNavigationDeepLink("meetings")
    data class MeetingDetail(val meetingId: String) : AndroidNavigationDeepLink("meeting")
    data class Comments(val eventId: String) : AndroidNavigationDeepLink("comments")
    data class Invite(val token: String) : AndroidNavigationDeepLink("invite")
    data object Calendar : AndroidNavigationDeepLink("calendar")
    data object VoteReminder : AndroidNavigationDeepLink("reminder/check-votes")
    data object Home : AndroidNavigationDeepLink("home")
    data object Profile : AndroidNavigationDeepLink("profile")
    data object Settings : AndroidNavigationDeepLink("settings")
    data object NotificationPreferences : AndroidNavigationDeepLink("notifications/preferences")
    data class Notifications(val filter: String? = null) : AndroidNavigationDeepLink("notifications")
}

/**
 * Parse a deep link URI into a DeepLink object.
 *
 * This is a pure function (no side effects) that extracts parameters from the URI.
 *
 * @param uri The deep link URI to parse
 * @return Parsed DeepLink object or null if invalid
 */
fun parseDeepLink(uri: Uri): AndroidNavigationDeepLink? {
    return try {
        if (hasUnsupportedDeepLinkUriComponents(uri)) {
            return null
        }

        parseDeepLinkParts(
            scheme = uri.scheme,
            host = uri.host,
            pathSegments = uri.pathSegments,
            queryParameters = uri.queryParameterNames.associateWith { name ->
                uri.getQueryParameter(name).orEmpty()
            }
        )
    } catch (e: Exception) {
        Log.e("DeepLinkParser", "Error parsing deep link", e)
        null
    }
}

internal fun hasUnsupportedDeepLinkUriComponents(uri: Uri): Boolean =
    hasUnsupportedDeepLinkUriComponents(
        encodedFragment = uri.encodedFragment,
        encodedUserInfo = uri.encodedUserInfo,
        port = uri.port
    )

internal fun hasUnsupportedDeepLinkUriComponents(
    encodedFragment: String?,
    encodedUserInfo: String?,
    port: Int
): Boolean {
    return encodedFragment != null || encodedUserInfo != null || port != -1
}

internal fun parseDeepLinkParts(
    scheme: String?,
    host: String?,
    pathSegments: List<String>,
    queryParameters: Map<String, String> = emptyMap()
): AndroidNavigationDeepLink? {
    // Handle universal links (https://wakeve.app/invite/{code})
    if (scheme == "https" && host == "wakeve.app") {
        if (pathSegments.size == 2 && pathSegments[0] == "invite") {
            return normalizeDeepLinkPathSegment(pathSegments[1])?.let(AndroidNavigationDeepLink::Invite)
        }
        return null
    }

    // Validate custom scheme
    if (scheme != "wakeve") {
        return null
    }

    return when (host) {
        "event" -> {
            parseEventDeepLink(pathSegments, queryParameters)
        }
        "calendar" -> {
            if (pathSegments.isEmpty()) AndroidNavigationDeepLink.Calendar else null
        }
        "reminder" -> {
            if (pathSegments.singleOrNull() == "check-votes") AndroidNavigationDeepLink.VoteReminder else null
        }
        "poll" -> {
            // wakeve://poll/{eventId}
            normalizeDeepLinkPathSegment(pathSegments.singleOrNull())
                ?.let(AndroidNavigationDeepLink::PollVoting)
        }
        "meeting" -> {
            // wakeve://meeting/{meetingId}
            normalizeDeepLinkPathSegment(pathSegments.singleOrNull())
                ?.let(AndroidNavigationDeepLink::MeetingDetail)
        }
        "invite" -> {
            // wakeve://invite/{token}
            normalizeDeepLinkPathSegment(pathSegments.singleOrNull())
                ?.let(AndroidNavigationDeepLink::Invite)
        }
        "home" -> {
            if (pathSegments.isEmpty()) AndroidNavigationDeepLink.Home else null
        }
        "profile" -> {
            if (pathSegments.isEmpty()) AndroidNavigationDeepLink.Profile else null
        }
        "settings" -> {
            if (pathSegments.isNotEmpty()) {
                null
            } else if (queryParameters["category"]?.trim() == "notifications") {
                AndroidNavigationDeepLink.NotificationPreferences
            } else {
                AndroidNavigationDeepLink.Settings
            }
        }
        "notifications" -> {
            if (pathSegments.isEmpty()) {
                AndroidNavigationDeepLink.Notifications(
                    filter = normalizeNotificationsFilter(queryParameters["filter"])
                )
            } else {
                null
            }
        }
        else -> null
    }
}

private fun parseEventDeepLink(
    pathSegments: List<String>,
    queryParameters: Map<String, String>
): AndroidNavigationDeepLink? {
    when (pathSegments.singleOrNull()) {
        "create" -> return AndroidNavigationDeepLink.EventCreate
        "share",
        "cancel" -> return null
    }

    val eventId = normalizeDeepLinkPathSegment(pathSegments.firstOrNull()) ?: return null
    return when {
        pathSegments.size == 1 -> AndroidNavigationDeepLink.EventDetail(eventId)
        pathSegments.size == 2 -> when (pathSegments[1]) {
            "details" -> parseEventDetailsTab(eventId, queryParameters["tab"])
            "poll" -> AndroidNavigationDeepLink.PollVoting(eventId)
            "scenarios" -> AndroidNavigationDeepLink.ScenarioList(eventId)
            "meetings" -> {
                if ("meetingId" in queryParameters) {
                    normalizeDeepLinkPathSegment(queryParameters["meetingId"])
                        ?.let(AndroidNavigationDeepLink::MeetingDetail)
                } else {
                    AndroidNavigationDeepLink.MeetingList(eventId)
                }
            }
            else -> null
        }
        else -> null
    }
}

private fun parseEventDetailsTab(
    eventId: String,
    tab: String?
): AndroidNavigationDeepLink {
    return when (tab?.trim()?.lowercase()) {
        "comments" -> AndroidNavigationDeepLink.Comments(eventId)
        "budget" -> AndroidNavigationDeepLink.BudgetOverview(eventId)
        "participants" -> AndroidNavigationDeepLink.ParticipantManagement(eventId)
        else -> AndroidNavigationDeepLink.EventDetail(eventId)
    }
}

internal fun normalizeDeepLinkPathSegment(value: String?): String? {
    val normalized = value?.trim().orEmpty()
    val lowercase = normalized.lowercase()
    return normalized.takeIf {
        it.isNotBlank() &&
            !it.contains("/") &&
            !it.contains("?") &&
            !it.contains("#") &&
            !lowercase.contains("%2f") &&
            !lowercase.contains("%3f") &&
            !lowercase.contains("%23")
    }
}

internal fun requiresAuthenticatedDeepLinkSession(deepLink: AndroidNavigationDeepLink): Boolean {
    return when (deepLink) {
        AndroidNavigationDeepLink.EventCreate,
        is AndroidNavigationDeepLink.EventDetail,
        is AndroidNavigationDeepLink.PollVoting,
        is AndroidNavigationDeepLink.ParticipantManagement,
        is AndroidNavigationDeepLink.ScenarioList,
        is AndroidNavigationDeepLink.BudgetOverview,
        is AndroidNavigationDeepLink.MeetingList,
        is AndroidNavigationDeepLink.MeetingDetail,
        is AndroidNavigationDeepLink.Comments,
        AndroidNavigationDeepLink.Calendar,
        AndroidNavigationDeepLink.VoteReminder,
        AndroidNavigationDeepLink.Profile,
        AndroidNavigationDeepLink.Settings,
        AndroidNavigationDeepLink.NotificationPreferences,
        is AndroidNavigationDeepLink.Notifications -> true

        is AndroidNavigationDeepLink.Invite,
        AndroidNavigationDeepLink.Home -> false
    }
}

/**
 * Deep link handler for Wakeve Android app.
 *
 * This class handles incoming deep links and navigates to the appropriate screen.
 * It should be called from MainActivity's onNewIntent() or onCreate() when the app
 * receives a deep link intent.
 *
 * Architecture: Imperative Shell
 * - Parses deep link URI using pure function
 * - Navigates to appropriate screen (side effect)
 * - Handles edge cases (invalid links, unauthenticated users, etc.)
 */
class AndroidNavigationDeepLinkHandler {

    companion object {
        private const val TAG = "DeepLinkHandler"
    }

    /**
     * Handle a deep link URI.
     *
     * This method parses the URI and navigates to the appropriate screen.
     * It handles edge cases such as:
     * - Invalid URIs
     * - Unauthenticated users (may need to show auth screen first)
     * - Non-existent events/meetings
     *
     * @param uri The deep link URI to handle
     * @param navController The navigation controller to use for navigation
     * @param isAuthenticated Whether the user is authenticated
     * @return true if the deep link was handled successfully, false otherwise
     */
    fun handleDeepLink(
        uri: Uri,
        navController: NavController,
        isAuthenticated: Boolean
    ): Boolean {
        Log.d(TAG, "Handling deep link: ${redactDeepLinkForLog(uri.toString())}")

        // Parse the deep link
        val deepLink = parseDeepLink(uri) ?: run {
            Log.w(TAG, "Failed to parse deep link: ${redactDeepLinkForLog(uri.toString())}")
            return false
        }

        if (!isAuthenticated && requiresAuthenticatedDeepLinkSession(deepLink)) {
            Log.d(TAG, "Authentication required for deep link: ${redactDeepLinkForLog(uri.toString())}")
            navController.navigate(Screen.Auth.route)
            return false
        }

        // Handle the deep link based on type
        return when (deepLink) {
            is AndroidNavigationDeepLink.EventDetail -> {
                handleEventDetail(deepLink.eventId, navController, isAuthenticated)
            }
            is AndroidNavigationDeepLink.PollVoting -> {
                handlePollVoting(deepLink.eventId, navController, isAuthenticated)
            }
            is AndroidNavigationDeepLink.ParticipantManagement -> {
                handleParticipantManagement(deepLink.eventId, navController)
            }
            is AndroidNavigationDeepLink.ScenarioList -> {
                handleScenarioList(deepLink.eventId, navController)
            }
            is AndroidNavigationDeepLink.BudgetOverview -> {
                handleBudgetOverview(deepLink.eventId, navController)
            }
            is AndroidNavigationDeepLink.MeetingList -> {
                handleMeetingList(deepLink.eventId, navController)
            }
            is AndroidNavigationDeepLink.MeetingDetail -> {
                handleMeetingDetail(deepLink.meetingId, navController, isAuthenticated)
            }
            is AndroidNavigationDeepLink.Comments -> {
                handleComments(deepLink.eventId, navController)
            }
            is AndroidNavigationDeepLink.Invite -> {
                handleInvite(deepLink.token, navController, isAuthenticated)
            }
            AndroidNavigationDeepLink.EventCreate -> {
                navController.navigate(Screen.EventCreation.createRoute())
                true
            }
            AndroidNavigationDeepLink.Calendar,
            AndroidNavigationDeepLink.VoteReminder -> {
                navController.navigate(Screen.Home.route)
                true
            }
            AndroidNavigationDeepLink.Home -> {
                navController.navigate(Screen.Home.route)
                true
            }
            AndroidNavigationDeepLink.Profile -> {
                navController.navigate(Screen.Profile.route)
                true
            }
            AndroidNavigationDeepLink.Settings -> {
                navController.navigate(Screen.Settings.route)
                true
            }
            AndroidNavigationDeepLink.NotificationPreferences -> {
                navController.navigate(Screen.NotificationPreferences.route)
                true
            }
            is AndroidNavigationDeepLink.Notifications -> {
                navController.navigate(Screen.Notifications.createRoute(deepLink.filter))
                true
            }
        }
    }

    /**
     * Handle deep link to event details.
     *
     * @param eventId The event ID from the deep link
     * @param navController The navigation controller
     * @param isAuthenticated Whether the user is authenticated
     * @return true if navigation was successful
     */
    private fun handleEventDetail(
        eventId: String,
        navController: NavController,
        isAuthenticated: Boolean
    ): Boolean {
        Log.d(TAG, "Navigating to event detail: $eventId")

        // TODO: Check if event exists (optional validation)
        // For now, navigate directly

        navController.navigate(Screen.EventDetail.createRoute(eventId))
        return true
    }

    /**
     * Handle deep link to poll voting.
     *
     * @param eventId The event ID from the deep link
     * @param navController The navigation controller
     * @param isAuthenticated Whether the user is authenticated
     * @return true if navigation was successful
     */
    private fun handlePollVoting(
        eventId: String,
        navController: NavController,
        isAuthenticated: Boolean
    ): Boolean {
        Log.d(TAG, "Navigating to poll voting: $eventId")

        // Navigate to poll voting screen
        navController.navigate(Screen.PollVoting.createRoute(eventId))
        return true
    }

    private fun handleParticipantManagement(
        eventId: String,
        navController: NavController
    ): Boolean {
        Log.d(TAG, "Navigating to participants: $eventId")
        navController.navigate(Screen.ParticipantManagement.createRoute(eventId))
        return true
    }

    private fun handleScenarioList(
        eventId: String,
        navController: NavController
    ): Boolean {
        Log.d(TAG, "Navigating to scenarios: $eventId")
        navController.navigate(Screen.ScenarioList.createRoute(eventId))
        return true
    }

    private fun handleBudgetOverview(
        eventId: String,
        navController: NavController
    ): Boolean {
        Log.d(TAG, "Navigating to budget: $eventId")
        navController.navigate(Screen.BudgetOverview.createRoute(eventId))
        return true
    }

    private fun handleMeetingList(
        eventId: String,
        navController: NavController
    ): Boolean {
        Log.d(TAG, "Navigating to meeting list: $eventId")
        navController.navigate(Screen.MeetingList.createRoute(eventId))
        return true
    }

    private fun handleComments(
        eventId: String,
        navController: NavController
    ): Boolean {
        Log.d(TAG, "Navigating to comments: $eventId")
        navController.navigate(Screen.Comments.createRoute(eventId))
        return true
    }

    /**
     * Handle deep link to meeting details.
     *
     * @param meetingId The meeting ID from the deep link
     * @param navController The navigation controller
     * @param isAuthenticated Whether the user is authenticated
     * @return true if navigation was successful
     */
    private fun handleMeetingDetail(
        meetingId: String,
        navController: NavController,
        isAuthenticated: Boolean
    ): Boolean {
        Log.d(TAG, "Navigating to meeting detail: $meetingId")

        // Navigate to meeting detail screen
        navController.navigate(Screen.MeetingDetail.createRoute(meetingId))
        return true
    }

    /**
     * Handle deep link to invite.
     *
     * Resolves the invitation code and navigates to accept the invitation.
     * If the user is not authenticated, stores the pending invite for later.
     * If authenticated, navigates to the event after accepting.
     *
     * Deep link format: wakeve://invite/{code}
     *
     * In a full implementation, this would:
     * 1. Call GET /api/invite/{code} to resolve the invitation
     * 2. Show the user a preview of the event
     * 3. Call POST /api/invite/{code}/accept to join the event
     * 4. Navigate to the event detail screen
     *
     * @param token The invite token/code from the deep link
     * @param navController The navigation controller
     * @param isAuthenticated Whether the user is authenticated
     * @return true if navigation was successful
     */
    private fun handleInvite(
        token: String,
        navController: NavController,
        isAuthenticated: Boolean
    ): Boolean {
        Log.d(TAG, "Handling invite with token: ${redactDeepLinkForLog("wakeve://invite/$token")}")

        if (!isAuthenticated) {
            // Store pending invite code for processing after authentication
            pendingInviteCode = token
            Log.d(TAG, "User not authenticated. Storing invite code for later")
            // Navigate to auth screen
            navController.navigate(Screen.Auth.route)
            return true
        }

        // Store the invite code - the UI layer will call the API to resolve and accept
        pendingInviteCode = token
        Log.d(TAG, "Invite code stored for acceptance")

        // Navigate to Home where the pending invite will be processed
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
        }
        return true
    }

    /**
     * Pending invitation code waiting to be processed.
     * After authentication, the app should check this value and accept the invitation.
     */
    var pendingInviteCode: String?
        get() = DeepLinkStateManager.pendingInviteCode.value
        private set(value) {
            if (value == null) {
                DeepLinkStateManager.clearPendingInviteCode()
            } else {
                DeepLinkStateManager.updatePendingInviteCode(value)
            }
        }

    /**
     * Clear the pending invite code after it has been processed.
     */
    fun clearPendingInvite() {
        DeepLinkStateManager.clearPendingInviteCode()
        Log.d(TAG, "Cleared pending invite code")
    }
}

internal fun normalizeNotificationsFilter(value: String?): String? {
    return when (value?.trim()?.lowercase()) {
        NOTIFICATIONS_FILTER_UNREAD -> NOTIFICATIONS_FILTER_UNREAD
        else -> null
    }
}
