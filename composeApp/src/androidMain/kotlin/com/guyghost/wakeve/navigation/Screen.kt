package com.guyghost.wakeve.navigation

import android.net.Uri
import com.guyghost.wakeve.models.EventType

internal fun routePathSegment(value: String): String = Uri.encode(value)

internal const val NOTIFICATIONS_FILTER_ARG = "filter"
internal const val NOTIFICATIONS_FILTER_UNREAD = "unread"

/**
 * Sealed class defining all navigation routes in the Wakeve Android app.
 * 
 * Each route corresponds to a screen or destination in the navigation graph.
 * Routes with parameters provide helper functions to create parameterized routes.
 */
sealed class Screen(val route: String) {
    // Bottom Navigation Tabs
    data object Home : Screen("home")
    data object Events : Screen("events")
    data object Explore : Screen("explore")
    data object Profile : Screen("profile")
    
    // Auth & Onboarding
    data object Splash : Screen("splash")
    data object GetStarted : Screen("get_started")
    data object Auth : Screen("auth")  // NEW - Main auth screen
    data object EmailAuth : Screen("auth/email")  // NEW - Email OTP screen
    @Deprecated("Use Auth instead", ReplaceWith("Auth"))
    data object Login : Screen("login")  // Keep for backward compatibility
    data object Onboarding : Screen("onboarding")
    
    // Event Management
    data object EventCreation : Screen("event_creation?templateTitle={templateTitle}&templateDescription={templateDescription}&templateType={templateType}") {
        const val baseRoute = "event_creation"

        fun createRoute(
            templateTitle: String? = null,
            templateDescription: String? = null,
            templateType: EventType? = null
        ): String {
            val params = buildList {
                if (!templateTitle.isNullOrBlank()) {
                    add("templateTitle=${Uri.encode(templateTitle)}")
                }
                if (!templateDescription.isNullOrBlank()) {
                    add("templateDescription=${Uri.encode(templateDescription)}")
                }
                if (templateType != null) {
                    add("templateType=${Uri.encode(templateType.name)}")
                }
            }

            return if (params.isEmpty()) baseRoute else "$baseRoute?${params.joinToString("&")}"
        }
    }
    data object EventPlanningAssistant : Screen("event_planning_assistant")
    data object EventDetail : Screen("event/{eventId}") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}"
    }
    
    // Participant & Poll Management
    data object ParticipantManagement : Screen("event/{eventId}/participants") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/participants"
    }
    data object PollVoting : Screen("event/{eventId}/poll/vote") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/poll/vote"
    }
    data object PollResults : Screen("event/{eventId}/poll/results") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/poll/results"
    }
    
    // Scenario Management
    data object ScenarioList : Screen("event/{eventId}/scenarios") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/scenarios"
    }
    data object ScenarioDetail : Screen("event/{eventId}/scenario/{scenarioId}") {
        fun createRoute(eventId: String, scenarioId: String) = 
            "event/${routePathSegment(eventId)}/scenario/${routePathSegment(scenarioId)}"
    }
    data object ScenarioComparison : Screen("event/{eventId}/scenarios/compare") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/scenarios/compare"
    }
    data object ScenarioManagement : Screen("event/{eventId}/scenarios/manage") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/scenarios/manage"
    }
    
    // Budget, Payment & Tricount Management
    data object BudgetOverview : Screen("event/{eventId}/budget") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/budget"
    }
    data object BudgetDetail : Screen("event/{eventId}/budget/{budgetItemId}") {
        fun createRoute(eventId: String, budgetItemId: String) = 
            "event/${routePathSegment(eventId)}/budget/${routePathSegment(budgetItemId)}"
    }
    data object PaymentPot : Screen("event/{eventId}/payment") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/payment"
    }
    data object Tricount : Screen("event/{eventId}/tricount") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/tricount"
    }
    
    // Logistics & Planning
    data object Accommodation : Screen("event/{eventId}/accommodation") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/accommodation"
    }
    data object MealPlanning : Screen("event/{eventId}/meals") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/meals"
    }
    data object EquipmentChecklist : Screen("event/{eventId}/equipment") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/equipment"
    }
    data object ActivityPlanning : Screen("event/{eventId}/activities") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/activities"
    }
    data object TransportPlanning : Screen("event/{eventId}/transport") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/transport"
    }
    
    // Communication
    data object Comments : Screen("event/{eventId}/comments") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/comments"
    }
    data object EventPhotos : Screen("event/{eventId}/photos") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/photos"
    }
    data object Inbox : Screen("inbox")
    
    // Meetings (Phase 4)
    data object MeetingList : Screen("event/{eventId}/meetings") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/meetings"
    }
    data object MeetingDetail : Screen("meeting/{meetingId}") {
        fun createRoute(meetingId: String) = "meeting/${routePathSegment(meetingId)}"
    }
    
    // Invitation Share
    data object InvitationShare : Screen("event/{eventId}/invite") {
        fun createRoute(eventId: String) = "event/${routePathSegment(eventId)}/invite"
    }

    // Settings
    data object Settings : Screen("settings")
    data object DataManagement : Screen("settings/data_management")

    // Notifications
    data object Notifications : Screen("notifications?$NOTIFICATIONS_FILTER_ARG={$NOTIFICATIONS_FILTER_ARG}") {
        private const val BASE_ROUTE = "notifications"

        fun createRoute(filter: String? = null): String {
            val normalizedFilter = filter?.trim()?.lowercase()
            return if (normalizedFilter == NOTIFICATIONS_FILTER_UNREAD) {
                "$BASE_ROUTE?$NOTIFICATIONS_FILTER_ARG=$NOTIFICATIONS_FILTER_UNREAD"
            } else {
                BASE_ROUTE
            }
        }
    }
    data object NotificationPreferences : Screen("notifications/preferences")

    // Gamification
    data object Leaderboard : Screen("leaderboard")

    // Organizer Dashboard
    data object OrganizerDashboard : Screen("organizer_dashboard")
}
