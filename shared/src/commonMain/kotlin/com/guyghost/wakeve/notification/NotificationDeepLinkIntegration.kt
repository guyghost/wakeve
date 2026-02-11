package com.guyghost.wakeve.notification

import com.guyghost.wakeve.deeplink.DeepLink
import com.guyghost.wakeve.deeplink.DeepLinkFactory
import com.guyghost.wakeve.deeplink.DeepLinkRoute
import kotlinx.datetime.Instant

/**
 * Integration layer between notifications and deep links.
 *
 * This file provides extension functions on NotificationService to automatically
 * add deep link data to notifications, enabling users to navigate directly to
 * relevant content when tapping a notification.
 */

/**
 * Data class representing a notification with deep link information.
 *
 * @property request The original notification request
 * @property deepLink The deep link to navigate to when tapped
 * @property deepLinkRoute The route type for the deep link
 */
data class NotificationWithDeepLink(
    val request: NotificationRequest,
    val deepLink: DeepLink,
    val deepLinkRoute: DeepLinkRoute
)

/**
 * Sends a notification with an associated deep link.
 *
 * This extension function automatically adds deep link data to the notification payload,
 * allowing the app to navigate to the correct screen when the user taps the notification.
 *
 * @param request The notification request
 * @param deepLinkRoute The route to navigate to when tapped
 * @param currentTime Optional current time for quiet hours checking
 * @return Result containing the notification ID or an error
 */
suspend fun NotificationService.sendNotificationWithDeepLink(
    request: NotificationRequest,
    deepLinkRoute: DeepLinkRoute,
    currentTime: Instant = kotlinx.datetime.Clock.System.now()
): Result<String> {
    val deepLink = createDeepLinkForRoute(deepLinkRoute, request)
    val enhancedRequest = request.copy(
        data = request.data + createDeepLinkData(deepLink, deepLinkRoute)
    )
    return sendNotification(enhancedRequest, currentTime)
}

/**
 * Sends a notification with a custom deep link.
 *
 * @param request The notification request
 * @param deepLink The custom deep link to use
 * @param currentTime Optional current time for quiet hours checking
 * @return Result containing the notification ID or an error
 */
suspend fun NotificationService.sendNotificationWithDeepLink(
    request: NotificationRequest,
    deepLink: DeepLink,
    currentTime: Instant = kotlinx.datetime.Clock.System.now()
): Result<String> {
    val route = deepLink.toDeepLinkRoute()
    val enhancedRequest = request.copy(
        data = request.data + createDeepLinkData(deepLink, route)
    )
    return sendNotification(enhancedRequest, currentTime)
}

/**
 * Creates notification data map with deep link information.
 *
 * @param eventId The event ID
 * @param deepLinkRoute The route for the deep link
 * @param additionalParams Additional parameters to include
 * @return Map of data to add to the notification
 */
fun createNotificationDataWithDeepLink(
    eventId: String?,
    deepLinkRoute: DeepLinkRoute,
    additionalParams: Map<String, String> = emptyMap()
): Map<String, String> {
    val deepLink = when (deepLinkRoute) {
        DeepLinkRoute.EVENT_DETAILS -> {
            DeepLinkFactory.createEventDetailsLink(
                eventId = eventId ?: "",
                tab = additionalParams["tab"]
            )
        }
        DeepLinkRoute.EVENT_POLL -> {
            DeepLinkFactory.createPollVoteLink(
                eventId = eventId ?: "",
                slotId = additionalParams["slotId"]
            )
        }
        DeepLinkRoute.EVENT_SCENARIOS -> {
            DeepLinkFactory.createScenarioComparisonLink(
                eventId = eventId ?: "",
                scenarioId = additionalParams["scenarioId"]
            )
        }
        DeepLinkRoute.EVENT_MEETINGS -> {
            DeepLinkFactory.createMeetingJoinLink(
                eventId = eventId ?: "",
                meetingId = additionalParams["meetingId"] ?: "",
                autoJoin = additionalParams["autoJoin"] == "true"
            )
        }
        DeepLinkRoute.PROFILE -> DeepLinkFactory.createProfileLink(additionalParams["userId"])
        DeepLinkRoute.SETTINGS -> DeepLinkFactory.createNotificationPreferencesLink(
            section = additionalParams["section"]
        )
        DeepLinkRoute.NOTIFICATIONS -> DeepLinkFactory.createNotificationsListLink(
            filter = additionalParams["filter"]
        )
        DeepLinkRoute.HOME -> DeepLinkFactory.createHomeLink(tab = additionalParams["tab"])
    }

    return createDeepLinkData(deepLink, deepLinkRoute) + additionalParams
}

/**
 * Creates a deep link for a notification request based on the notification type.
 *
 * @param request The notification request
 * @return A DeepLink object appropriate for the notification type
 */
fun createDeepLinkForNotification(request: NotificationRequest): DeepLink {
    return DeepLinkFactory.createFromNotification(
        notificationType = request.type.name,
        eventId = request.eventId,
        additionalParams = buildMap {
            request.commentId?.let { put("commentId", it) }
            request.parentCommentId?.let { put("parentCommentId", it) }
            request.section?.let { put("section", it) }
            request.sectionItemId?.let { put("sectionItemId", it) }
        } + request.data
    )
}

/**
 * Extension function to add deep link to a notification request.
 *
 * @return A new NotificationRequest with deep link data added
 */
fun NotificationRequest.withDeepLink(): NotificationRequest {
    val deepLink = createDeepLinkForNotification(this)
    val route = deepLink.toDeepLinkRoute()
    return this.copy(
        data = this.data + createDeepLinkData(deepLink, route)
    )
}

/**
 * Extension function to add a specific deep link route to a notification request.
 *
 * @param deepLinkRoute The route to use
 * @return A new NotificationRequest with deep link data added
 */
fun NotificationRequest.withDeepLink(deepLinkRoute: DeepLinkRoute): NotificationRequest {
    val deepLink = createDeepLinkForRoute(deepLinkRoute, this)
    return this.copy(
        data = this.data + createDeepLinkData(deepLink, deepLinkRoute)
    )
}

/**
 * Internal function to create deep link data map.
 *
 * @param deepLink The deep link
 * @param route The route (may be null if not a recognized route)
 * @return Map of data fields for the notification payload
 */
private fun createDeepLinkData(
    deepLink: DeepLink,
    route: DeepLinkRoute?
): Map<String, String> {
    return buildMap {
        put("deepLink", deepLink.fullUri)
        put("deepLinkRoute", route?.name ?: "UNKNOWN")
        put("deepLinkPath", deepLink.route)

        // Add all deep link parameters with prefix
        deepLink.parameters.forEach { (key, value) ->
            put("param_$key", value)
        }
    }
}

/**
 * Creates a deep link for a specific route and notification request.
 *
 * @param route The deep link route
 * @param request The notification request
 * @return A DeepLink object
 */
private fun createDeepLinkForRoute(
    route: DeepLinkRoute,
    request: NotificationRequest
): DeepLink {
    val additionalParams = buildMap {
        request.commentId?.let { put("commentId", it) }
        request.parentCommentId?.let { put("parentCommentId", it) }
        request.section?.let { put("section", it) }
        request.sectionItemId?.let { put("sectionItemId", it) }
    } + request.data.filterKeys { it != "deepLink" && !it.startsWith("param_") }

    return when (route) {
        DeepLinkRoute.EVENT_DETAILS -> DeepLinkFactory.createEventDetailsLink(
            eventId = request.eventId ?: "",
            tab = additionalParams["tab"] ?: "details"
        )
        DeepLinkRoute.EVENT_POLL -> DeepLinkFactory.createPollVoteLink(
            eventId = request.eventId ?: "",
            slotId = additionalParams["slotId"]
        )
        DeepLinkRoute.EVENT_SCENARIOS -> DeepLinkFactory.createScenarioComparisonLink(
            eventId = request.eventId ?: "",
            scenarioId = additionalParams["scenarioId"]
        )
        DeepLinkRoute.EVENT_MEETINGS -> DeepLinkFactory.createMeetingJoinLink(
            eventId = request.eventId ?: "",
            meetingId = additionalParams["meetingId"] ?: "",
            autoJoin = additionalParams["autoJoin"] == "true"
        )
        DeepLinkRoute.PROFILE -> DeepLinkFactory.createProfileLink(
            userId = additionalParams["userId"]
        )
        DeepLinkRoute.SETTINGS -> DeepLinkFactory.createNotificationPreferencesLink(
            section = additionalParams["section"]
        )
        DeepLinkRoute.NOTIFICATIONS -> DeepLinkFactory.createNotificationsListLink(
            filter = additionalParams["filter"]
        )
        DeepLinkRoute.HOME -> DeepLinkFactory.createHomeLink(
            tab = additionalParams["tab"]
        )
    }
}

/**
 * Parses deep link information from notification data.
 *
 * @param data The notification data map
 * @return DeepLinkInfo if deep link data is present, null otherwise
 */
fun parseDeepLinkFromNotificationData(data: Map<String, String>): DeepLinkInfo? {
    val deepLinkUri = data["deepLink"] ?: return null
    val deepLinkRoute = data["deepLinkRoute"]?.let { routeName ->
        try {
            DeepLinkRoute.valueOf(routeName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    val deepLink = DeepLink.parse(deepLinkUri).getOrNull() ?: return null

    // Extract parameters (those with param_ prefix)
    val parameters = data.filterKeys { it.startsWith("param_") }
        .mapKeys { it.key.removePrefix("param_") }

    return DeepLinkInfo(
        uri = deepLinkUri,
        route = deepLinkRoute,
        path = data["deepLinkPath"] ?: deepLink.route,
        parameters = parameters + deepLink.parameters,
        rawData = data
    )
}

/**
 * Information about a deep link extracted from notification data.
 *
 * @property uri The full deep link URI
 * @property route The recognized route (may be null)
 * @property path The path component
 * @property parameters Combined parameters from the deep link
 * @property rawData The original notification data
 */
data class DeepLinkInfo(
    val uri: String,
    val route: DeepLinkRoute?,
    val path: String,
    val parameters: Map<String, String>,
    val rawData: Map<String, String>
) {
    /**
     * Gets a parameter value.
     *
     * @param key The parameter key
     * @return The value or null if not found
     */
    fun getParameter(key: String): String? = parameters[key]

    /**
     * Checks if this deep link is for a specific route.
     *
     * @param route The route to check
     * @return True if the routes match
     */
    fun isRoute(route: DeepLinkRoute): Boolean = this.route == route
}

/**
 * Extension property to check if notification data contains deep link information.
 */
val Map<String, String>.hasDeepLink: Boolean
    get() = containsKey("deepLink")

/**
 * Extension property to get the deep link URI from notification data.
 */
val Map<String, String>.deepLinkUri: String?
    get() = this["deepLink"]

/**
 * Extension property to get the deep link route from notification data.
 */
val Map<String, String>.deepLinkRoute: DeepLinkRoute?
    get() = this["deepLinkRoute"]?.let { routeName ->
        try {
            DeepLinkRoute.valueOf(routeName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
