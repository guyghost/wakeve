package com.guyghost.wakeve.navigation

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
    data object Login : Screen("login")
    data object Onboarding : Screen("onboarding")
    
    // Event Management
    data object EventCreation : Screen("event_creation")
    data object EventDetail : Screen("event/{eventId}") {
        fun createRoute(eventId: String) = "event/$eventId"
    }
    
    // Participant & Poll Management
    data object ParticipantManagement : Screen("event/{eventId}/participants") {
        fun createRoute(eventId: String) = "event/$eventId/participants"
    }
    data object PollVoting : Screen("event/{eventId}/poll/vote") {
        fun createRoute(eventId: String) = "event/$eventId/poll/vote"
    }
    data object PollResults : Screen("event/{eventId}/poll/results") {
        fun createRoute(eventId: String) = "event/$eventId/poll/results"
    }
    
    // Scenario Management
    data object ScenarioList : Screen("event/{eventId}/scenarios") {
        fun createRoute(eventId: String) = "event/$eventId/scenarios"
    }
    data object ScenarioDetail : Screen("event/{eventId}/scenario/{scenarioId}") {
        fun createRoute(eventId: String, scenarioId: String) = 
            "event/$eventId/scenario/$scenarioId"
    }
    data object ScenarioComparison : Screen("event/{eventId}/scenarios/compare") {
        fun createRoute(eventId: String) = "event/$eventId/scenarios/compare"
    }
    data object ScenarioManagement : Screen("event/{eventId}/scenarios/manage") {
        fun createRoute(eventId: String) = "event/$eventId/scenarios/manage"
    }
    
    // Budget Management
    data object BudgetOverview : Screen("event/{eventId}/budget") {
        fun createRoute(eventId: String) = "event/$eventId/budget"
    }
    data object BudgetDetail : Screen("event/{eventId}/budget/{budgetItemId}") {
        fun createRoute(eventId: String, budgetItemId: String) = 
            "event/$eventId/budget/$budgetItemId"
    }
    
    // Logistics & Planning
    data object Accommodation : Screen("event/{eventId}/accommodation") {
        fun createRoute(eventId: String) = "event/$eventId/accommodation"
    }
    data object MealPlanning : Screen("event/{eventId}/meals") {
        fun createRoute(eventId: String) = "event/$eventId/meals"
    }
    data object EquipmentChecklist : Screen("event/{eventId}/equipment") {
        fun createRoute(eventId: String) = "event/$eventId/equipment"
    }
    data object ActivityPlanning : Screen("event/{eventId}/activities") {
        fun createRoute(eventId: String) = "event/$eventId/activities"
    }
    
    // Communication
    data object Comments : Screen("event/{eventId}/comments") {
        fun createRoute(eventId: String) = "event/$eventId/comments"
    }
    data object Inbox : Screen("inbox")
    
    // Meetings (Phase 4)
    data object MeetingList : Screen("meetings")
    data object MeetingDetail : Screen("meeting/{meetingId}") {
        fun createRoute(meetingId: String) = "meeting/$meetingId"
    }
    
    // Settings
    data object Settings : Screen("settings")
}
