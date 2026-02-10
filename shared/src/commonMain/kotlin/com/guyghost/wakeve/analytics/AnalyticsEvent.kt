package com.guyghost.wakeve.analytics

/**
 * Sealed class representing all trackable analytics events.
 *
 * Each event type contains relevant parameters for that event.
 */
sealed class AnalyticsEvent(val eventName: String) {

    // ==================== APP LIFECYCLE ====================

    /** App started */
    data object AppStart : AnalyticsEvent("app_start")

    /** App brought to foreground */
    data object AppForeground : AnalyticsEvent("app_foreground")

    /** App sent to background */
    data object AppBackground : AnalyticsEvent("app_background")

    /** Screen viewed */
    data class ScreenView(
        val screenName: String,
        val screenClass: String? = null
    ) : AnalyticsEvent("screen_view")

    // ==================== EVENT ACTIONS ====================

    /** Event created */
    data class EventCreated(
        val eventType: String,
        val hasLocation: Boolean = false,
        val timeSlotsCount: Int = 0
    ) : AnalyticsEvent("event_created")

    /** User joined an event */
    data class EventJoined(
        val eventId: String,
        val isGuest: Boolean = false
    ) : AnalyticsEvent("event_joined")

    /** Event viewed */
    data class EventViewed(
        val eventId: String,
        val source: String = "list" // list, notification, deep_link
    ) : AnalyticsEvent("event_viewed")

    /** Event shared */
    data class EventShared(
        val eventId: String,
        val shareMethod: String // link, qr_code, native_share
    ) : AnalyticsEvent("event_shared")

    // ==================== POLL ACTIONS ====================

    /** User voted on poll */
    data class PollVoted(
        val eventId: String,
        val response: String, // yes, no, maybe
        val isChangingVote: Boolean = false
    ) : AnalyticsEvent("poll_voted")

    /** Poll viewed */
    data class PollViewed(
        val eventId: String
    ) : AnalyticsEvent("poll_viewed")

    /** Poll closed and date confirmed */
    data class PollClosed(
        val eventId: String,
        val participantsCount: Int,
        val votesCount: Int
    ) : AnalyticsEvent("poll_closed")

    // ==================== SCENARIO ACTIONS ====================

    /** Scenario created */
    data class ScenarioCreated(
        val eventId: String,
        val hasAccommodation: Boolean = false
    ) : AnalyticsEvent("scenario_created")

    /** Scenario viewed */
    data class ScenarioViewed(
        val eventId: String,
        val scenarioId: String
    ) : AnalyticsEvent("scenario_viewed")

    /** Scenario selected as final */
    data class ScenarioSelected(
        val eventId: String,
        val scenarioId: String
    ) : AnalyticsEvent("scenario_selected")

    /** User voted on scenario */
    data class ScenarioVoted(
        val eventId: String,
        val scenarioId: String,
        val vote: String // yes, no
    ) : AnalyticsEvent("scenario_voted")

    // ==================== MEETING ACTIONS ====================

    /** Meeting created */
    data class MeetingCreated(
        val eventId: String,
        val platform: String // zoom, google_meet, teams, jitsi
    ) : AnalyticsEvent("meeting_created")

    /** Meeting joined */
    data class MeetingJoined(
        val eventId: String,
        val meetingId: String,
        val platform: String
    ) : AnalyticsEvent("meeting_joined")

    /** Meeting link generated */
    data class MeetingLinkGenerated(
        val eventId: String,
        val platform: String
    ) : AnalyticsEvent("meeting_link_generated")

    // ==================== USER ACTIONS ====================

    /** User registered */
    data class UserRegistered(
        val authMethod: String // email, google, apple, guest
    ) : AnalyticsEvent("user_registered")

    /** User logged in */
    data class UserLoggedIn(
        val authMethod: String
    ) : AnalyticsEvent("user_logged_in")

    /** User logged out */
    data object UserLoggedOut : AnalyticsEvent("user_logged_out")

    /** User profile updated */
    data class UserProfileUpdated(
        val fieldsUpdated: List<String>
    ) : AnalyticsEvent("user_profile_updated")

    // ==================== OFFLINE ACTIONS ====================

    /** Action queued for offline sync */
    data class OfflineActionQueued(
        val actionType: String, // create_event, vote, etc.
        val queueSize: Int
    ) : AnalyticsEvent("offline_action_queued")

    /** Sync completed */
    data class SyncCompleted(
        val itemsSynced: Int,
        val durationMs: Long
    ) : AnalyticsEvent("sync_completed")

    /** Sync failed */
    data class SyncFailed(
        val errorType: String,
        val itemsPending: Int
    ) : AnalyticsEvent("sync_failed")

    // ==================== ERROR EVENTS ====================

    /** Error occurred */
    data class ErrorOccurred(
        val errorType: String,
        val errorContext: String?,
        val isFatal: Boolean = false
    ) : AnalyticsEvent("error_occurred")

    /** API error */
    data class ApiError(
        val endpoint: String,
        val statusCode: Int,
        val errorMessage: String?
    ) : AnalyticsEvent("api_error")

    // ==================== RGPD ====================

    /** Analytics consent granted */
    data object AnalyticsConsentGranted : AnalyticsEvent("analytics_consent_granted")

    /** Analytics consent revoked */
    data object AnalyticsConsentRevoked : AnalyticsEvent("analytics_consent_revoked")

    /** User data deleted */
    data object UserDataDeleted : AnalyticsEvent("user_data_deleted")
}
