package com.guyghost.wakeve.deeplink

import android.net.Uri
import android.util.Log
import androidx.navigation.NavController
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
sealed class DeepLink(val route: String) {
    data class EventDetail(val eventId: String) : DeepLink("event")
    data class PollVoting(val eventId: String) : DeepLink("poll")
    data class MeetingDetail(val meetingId: String) : DeepLink("meeting")
    data class Invite(val token: String) : DeepLink("invite")
}

/**
 * Parse a deep link URI into a DeepLink object.
 *
 * This is a pure function (no side effects) that extracts parameters from the URI.
 *
 * @param uri The deep link URI to parse
 * @return Parsed DeepLink object or null if invalid
 */
fun parseDeepLink(uri: Uri): DeepLink? {
    return try {
        val scheme = uri.scheme
        val host = uri.host
        val pathSegments = uri.pathSegments

        // Validate scheme
        if (scheme != "wakeve") {
            Log.d("DeepLinkParser", "Invalid scheme: $scheme")
            return null
        }

        when (host) {
            "event" -> {
                // wakeve://event/{id}
                val eventId = pathSegments.firstOrNull()
                if (eventId != null) {
                    DeepLink.EventDetail(eventId)
                } else {
                    Log.d("DeepLinkParser", "Missing event ID in deep link")
                    null
                }
            }
            "poll" -> {
                // wakeve://poll/{eventId}
                val eventId = pathSegments.firstOrNull()
                if (eventId != null) {
                    DeepLink.PollVoting(eventId)
                } else {
                    Log.d("DeepLinkParser", "Missing event ID in poll deep link")
                    null
                }
            }
            "meeting" -> {
                // wakeve://meeting/{meetingId}
                val meetingId = pathSegments.firstOrNull()
                if (meetingId != null) {
                    DeepLink.MeetingDetail(meetingId)
                } else {
                    Log.d("DeepLinkParser", "Missing meeting ID in deep link")
                    null
                }
            }
            "invite" -> {
                // wakeve://invite/{token}
                val token = pathSegments.firstOrNull()
                if (token != null) {
                    DeepLink.Invite(token)
                } else {
                    Log.d("DeepLinkParser", "Missing token in invite deep link")
                    null
                }
            }
            else -> {
                Log.d("DeepLinkParser", "Unknown deep link host: $host")
                null
            }
        }
    } catch (e: Exception) {
        Log.e("DeepLinkParser", "Error parsing deep link: ${e.message}", e)
        null
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
class DeepLinkHandler {

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
        Log.d(TAG, "Handling deep link: $uri")

        // Parse the deep link
        val deepLink = parseDeepLink(uri) ?: run {
            Log.w(TAG, "Failed to parse deep link: $uri")
            return false
        }

        // Handle the deep link based on type
        return when (deepLink) {
            is DeepLink.EventDetail -> {
                handleEventDetail(deepLink.eventId, navController, isAuthenticated)
            }
            is DeepLink.PollVoting -> {
                handlePollVoting(deepLink.eventId, navController, isAuthenticated)
            }
            is DeepLink.MeetingDetail -> {
                handleMeetingDetail(deepLink.meetingId, navController, isAuthenticated)
            }
            is DeepLink.Invite -> {
                handleInvite(deepLink.token, navController, isAuthenticated)
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
     * This is a special case that may require additional processing:
     * - Validate the invite token
     * - Show user a preview of the event
     * - Optionally prompt for authentication
     *
     * TODO: Implement invite flow (not yet specified)
     *
     * @param token The invite token from the deep link
     * @param navController The navigation controller
     * @param isAuthenticated Whether the user is authenticated
     * @return true if navigation was successful
     */
    private fun handleInvite(
        token: String,
        navController: NavController,
        isAuthenticated: Boolean
    ): Boolean {
        Log.d(TAG, "Handling invite with token: $token")

        // TODO: Implement invite flow
        // For now, just log the token
        Log.w(TAG, "Invite flow not yet implemented. Token: $token")

        // Navigate to a placeholder screen or show a toast
        return false
    }
}
