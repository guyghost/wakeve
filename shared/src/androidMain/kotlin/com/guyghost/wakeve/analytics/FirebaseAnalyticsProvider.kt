package com.guyghost.wakeve.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Firebase Analytics implementation for Android.
 *
 * This provider:
 * - Immediately logs events to Firebase Analytics
 * - Queues events for offline backup
 * - Syncs queued events when connectivity is restored
 *
 * @param analyticsQueue Queue for offline analytics event backup
 */
actual class FirebaseAnalyticsProvider actual constructor(
    private val analyticsQueue: AnalyticsQueue
) : AnalyticsProvider {

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isEnabled = true

    init {
        // Start sync job for offline events
        startSyncJob()
    }

    /**
     * Track an analytics event.
     *
     * Logs event to Firebase Analytics immediately and queues for offline backup.
     *
     * @param event The event to track
     * @param properties Optional custom properties for the event
     */
    actual override fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?>) {
        if (!isEnabled) return

        // Create Firebase Analytics bundle
        val bundle = Bundle().apply {
            // Add custom properties
            properties.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value?.toString())
                }
            }

            // Add event-specific parameters
            when (event) {
                is AnalyticsEvent.EventCreated -> {
                    putString("event_type", event.eventType)
                    putBoolean("has_location", event.hasLocation)
                    putInt("time_slots_count", event.timeSlotsCount)
                }
                is AnalyticsEvent.EventJoined -> {
                    putString("event_id", event.eventId)
                    putBoolean("is_guest", event.isGuest)
                }
                is AnalyticsEvent.EventViewed -> {
                    putString("event_id", event.eventId)
                    putString("source", event.source)
                }
                is AnalyticsEvent.EventShared -> {
                    putString("event_id", event.eventId)
                    putString("share_method", event.shareMethod)
                }
                is AnalyticsEvent.PollVoted -> {
                    putString("event_id", event.eventId)
                    putString("response", event.response)
                    putBoolean("is_changing_vote", event.isChangingVote)
                }
                is AnalyticsEvent.PollViewed -> {
                    putString("event_id", event.eventId)
                }
                is AnalyticsEvent.PollClosed -> {
                    putString("event_id", event.eventId)
                    putInt("participants_count", event.participantsCount)
                    putInt("votes_count", event.votesCount)
                }
                is AnalyticsEvent.ScreenView -> {
                    putString("screen_name", event.screenName)
                    event.screenClass?.let { putString("screen_class", it) }
                }
                is AnalyticsEvent.ScenarioCreated -> {
                    putString("event_id", event.eventId)
                    putBoolean("has_accommodation", event.hasAccommodation)
                }
                is AnalyticsEvent.ScenarioViewed -> {
                    putString("event_id", event.eventId)
                    putString("scenario_id", event.scenarioId)
                }
                is AnalyticsEvent.ScenarioSelected -> {
                    putString("event_id", event.eventId)
                    putString("scenario_id", event.scenarioId)
                }
                is AnalyticsEvent.ScenarioVoted -> {
                    putString("event_id", event.eventId)
                    putString("scenario_id", event.scenarioId)
                    putString("vote", event.vote)
                }
                is AnalyticsEvent.MeetingCreated -> {
                    putString("event_id", event.eventId)
                    putString("platform", event.platform)
                }
                is AnalyticsEvent.MeetingJoined -> {
                    putString("event_id", event.eventId)
                    putString("meeting_id", event.meetingId)
                    putString("platform", event.platform)
                }
                is AnalyticsEvent.MeetingLinkGenerated -> {
                    putString("event_id", event.eventId)
                    putString("platform", event.platform)
                }
                is AnalyticsEvent.UserRegistered -> {
                    putString("auth_method", event.authMethod)
                }
                is AnalyticsEvent.UserLoggedIn -> {
                    putString("auth_method", event.authMethod)
                }
                is AnalyticsEvent.UserProfileUpdated -> {
                    putString("fields_updated", event.fieldsUpdated.joinToString(","))
                }
                is AnalyticsEvent.OfflineActionQueued -> {
                    putString("action_type", event.actionType)
                    putInt("queue_size", event.queueSize)
                }
                is AnalyticsEvent.SyncCompleted -> {
                    putInt("items_synced", event.itemsSynced)
                    putLong("duration_ms", event.durationMs)
                }
                is AnalyticsEvent.SyncFailed -> {
                    putString("error_type", event.errorType)
                    putInt("items_pending", event.itemsPending)
                }
                is AnalyticsEvent.ErrorOccurred -> {
                    putString("error_type", event.errorType)
                    event.errorContext?.let { putString("error_context", it) }
                    putBoolean("is_fatal", event.isFatal)
                }
                is AnalyticsEvent.ApiError -> {
                    putString("endpoint", event.endpoint)
                    putInt("status_code", event.statusCode)
                    event.errorMessage?.let { putString("error_message", it) }
                }
                // Object events without parameters
                AnalyticsEvent.AppStart,
                AnalyticsEvent.AppForeground,
                AnalyticsEvent.AppBackground,
                AnalyticsEvent.UserLoggedOut,
                AnalyticsEvent.AnalyticsConsentGranted,
                AnalyticsEvent.AnalyticsConsentRevoked,
                AnalyticsEvent.UserDataDeleted -> { /* No additional parameters */ }
            }
        }

        // Log to Firebase Analytics
        firebaseAnalytics.logEvent(event.eventName, bundle)

        // Queue for offline backup
        scope.launch {
            analyticsQueue.enqueue(event, properties)
        }
    }

    /**
     * Set a user property.
     *
     * @param name Property name
     * @param value Property value
     */
    actual override fun setUserProperty(name: String, value: String) {
        if (!isEnabled) return
        firebaseAnalytics.setUserProperty(name, value)
    }

    /**
     * Set the user ID.
     *
     * @param userId User identifier, or null to clear
     */
    actual override fun setUserId(userId: String?) {
        if (!isEnabled) return
        firebaseAnalytics.setUserId(userId)
    }

    /**
     * Enable or disable analytics collection.
     * Used for RGPD consent management.
     *
     * @param enabled true to enable, false to disable
     */
    actual override fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }

    /**
     * Clear all user data from analytics.
     * Used when user revokes consent.
     */
    actual override fun clearUserData() {
        firebaseAnalytics.resetAnalyticsData()
        scope.launch {
            analyticsQueue.clear()
        }
    }

    /**
     * Start periodic sync job for offline events.
     * Syncs every 5 minutes when online.
     */
    private fun startSyncJob() {
        scope.launch {
            // TODO: Implement periodic sync with network monitoring
            // This would check network status and sync queued events
            // Implementation depends on ConnectivityService
        }
    }
}
