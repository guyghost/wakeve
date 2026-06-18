package com.guyghost.wakeve.navigation

/**
 * Navigation 3-ready destination keys.
 *
 * Production routing still uses Navigation 2 route strings in [Screen]. These keys mirror the
 * main destinations so a future NavDisplay resolver can reuse the current Compose screen content
 * without coupling that content to string routes.
 */
sealed interface WakeveNavKey {
    data object Home : WakeveNavKey
    data object Inbox : WakeveNavKey
    data object Explore : WakeveNavKey
    data object Profile : WakeveNavKey
    data object EventCreation : WakeveNavKey
    data class EventDetail(val eventId: String) : WakeveNavKey
    data class PollVoting(val eventId: String) : WakeveNavKey
    data class PollResults(val eventId: String) : WakeveNavKey
}

fun WakeveNavKey.toScreenRoute(): String =
    when (this) {
        WakeveNavKey.Home -> Screen.Home.route
        WakeveNavKey.Inbox -> Screen.Inbox.route
        WakeveNavKey.Explore -> Screen.Explore.route
        WakeveNavKey.Profile -> Screen.Profile.route
        WakeveNavKey.EventCreation -> Screen.EventCreation.route
        is WakeveNavKey.EventDetail -> Screen.EventDetail.createRoute(eventId)
        is WakeveNavKey.PollVoting -> Screen.PollVoting.createRoute(eventId)
        is WakeveNavKey.PollResults -> Screen.PollResults.createRoute(eventId)
    }
