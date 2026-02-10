package com.guyghost.wakeve.analytics

import cocoapods.FirebaseAnalytics.FIRAnalytics
import cocoapods.FirebaseAnalytics.FIRApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.Foundation.NSDictionary
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSString

/**
 * Firebase Analytics implementation for iOS.
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isEnabled = true

    init {
        // Firebase is initialized automatically on iOS via CocoaPods
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
    override fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?>) {
        if (!isEnabled) return

        // Convert properties to NSDictionary
        val parameters = NSMutableDictionary()

        // Add custom properties
        properties.forEach { (key, value) ->
            val nsValue: Any = when (value) {
                is String -> NSString.create(string = value)
                is Int -> NSNumber.numberWithInt(value)
                is Long -> NSNumber.numberWithLong(value)
                is Double -> NSNumber.numberWithDouble(value)
                is Boolean -> NSNumber.numberWithBool(value)
                else -> NSString.create(string = value?.toString() ?: "")
            }
            parameters.setObject(nsValue, NSString.create(string = key))
        }

        // Add event-specific parameters
        when (event) {
            is AnalyticsEvent.EventCreated -> {
                parameters.setObject(
                    NSString.create(string = event.eventType),
                    NSString.create(string = "event_type")
                )
                parameters.setObject(
                    NSNumber.numberWithBool(event.hasLocation),
                    NSString.create(string = "has_location")
                )
                parameters.setObject(
                    NSNumber.numberWithInt(event.timeSlotsCount),
                    NSString.create(string = "time_slots_count")
                )
            }
            is AnalyticsEvent.EventJoined -> {
                parameters.setObject(
                    NSString.create(string = event.eventId),
                    NSString.create(string = "event_id")
                )
                parameters.setObject(
                    NSNumber.numberWithBool(event.isGuest),
                    NSString.create(string = "is_guest")
                )
            }
            is AnalyticsEvent.EventViewed -> {
                parameters.setObject(
                    NSString.create(string = event.eventId),
                    NSString.create(string = "event_id")
                )
                parameters.setObject(
                    NSString.create(string = event.source),
                    NSString.create(string = "source")
                )
            }
            is AnalyticsEvent.EventShared -> {
                parameters.setObject(
                    NSString.create(string = event.eventId),
                    NSString.create(string = "event_id")
                )
                parameters.setObject(
                    NSString.create(string = event.shareMethod),
                    NSString.create(string = "share_method")
                )
            }
            is AnalyticsEvent.PollVoted -> {
                parameters.setObject(
                    NSString.create(string = event.eventId),
                    NSString.create(string = "event_id")
                )
                parameters.setObject(
                    NSString.create(string = event.response),
                    NSString.create(string = "response")
                )
                parameters.setObject(
                    NSNumber.numberWithBool(event.isChangingVote),
                    NSString.create(string = "is_changing_vote")
                )
            }
            is AnalyticsEvent.PollViewed -> {
                parameters.setObject(
                    NSString.create(string = event.eventId),
                    NSString.create(string = "event_id")
                )
            }
            is AnalyticsEvent.PollClosed -> {
                parameters.setObject(
                    NSString.create(string = event.eventId),
                    NSString.create(string = "event_id")
                )
                parameters.setObject(
                    NSNumber.numberWithInt(event.participantsCount),
                    NSString.create(string = "participants_count")
                )
                parameters.setObject(
                    NSNumber.numberWithInt(event.votesCount),
                    NSString.create(string = "votes_count")
                )
            }
            is AnalyticsEvent.ScreenView -> {
                parameters.setObject(
                    NSString.create(string = event.screenName),
                    NSString.create(string = "screen_name")
                )
                event.screenClass?.let {
                    parameters.setObject(
                        NSString.create(string = it),
                        NSString.create(string = "screen_class")
                    )
                }
            }
            is AnalyticsEvent.ScenarioCreated -> {
                parameters.setObject(
                    NSString.create(string = event.eventId),
                    NSString.create(string = "event_id")
                )
                parameters.setObject(
                    NSNumber.numberWithBool(event.hasAccommodation),
                    NSString.create(string = "has_accommodation")
                )
            }
            is AnalyticsEvent.ScenarioViewed -> {
                parameters.setObject(
                    NSString.create(string = event.eventId),
                    NSString.create(string = "event_id")
                )
                parameters.setObject(
                    NSString.create(string = event.scenarioId),
                    NSString.create(string = "scenario_id")
                )
            }
            is AnalyticsEvent.ScenarioSelected -> {
                parameters.setObject(
                    NSString.create(string = event.eventId),
                    NSString.create(string = "event_id")
                )
                parameters.setObject(
                    NSString.create(string = event.scenarioId),
                    NSString.create(string = "scenario_id")
                )
            }
            is AnalyticsEvent.ScenarioVoted -> {
                parameters.setObject(
                    NSString.create(string = event.eventId),
                    NSString.create(string = "event_id")
                )
                parameters.setObject(
                    NSString.create(string = event.scenarioId),
                    NSString.create(string = "scenario_id")
                )
                parameters.setObject(
                    NSString.create(string = event.vote),
                    NSString.create(string = "vote")
                )
            }
            is AnalyticsEvent.MeetingCreated -> {
                parameters.setObject(
                    NSString.create(string = event.eventId),
                    NSString.create(string = "event_id")
                )
                parameters.setObject(
                    NSString.create(string = event.platform),
                    NSString.create(string = "platform")
                )
            }
            is AnalyticsEvent.MeetingJoined -> {
                parameters.setObject(
                    NSString.create(string = event.eventId),
                    NSString.create(string = "event_id")
                )
                parameters.setObject(
                    NSString.create(string = event.meetingId),
                    NSString.create(string = "meeting_id")
                )
                parameters.setObject(
                    NSString.create(string = event.platform),
                    NSString.create(string = "platform")
                )
            }
            is AnalyticsEvent.MeetingLinkGenerated -> {
                parameters.setObject(
                    NSString.create(string = event.eventId),
                    NSString.create(string = "event_id")
                )
                parameters.setObject(
                    NSString.create(string = event.platform),
                    NSString.create(string = "platform")
                )
            }
            is AnalyticsEvent.UserRegistered -> {
                parameters.setObject(
                    NSString.create(string = event.authMethod),
                    NSString.create(string = "auth_method")
                )
            }
            is AnalyticsEvent.UserLoggedIn -> {
                parameters.setObject(
                    NSString.create(string = event.authMethod),
                    NSString.create(string = "auth_method")
                )
            }
            is AnalyticsEvent.UserProfileUpdated -> {
                parameters.setObject(
                    NSString.create(string = event.fieldsUpdated.joinToString(",")),
                    NSString.create(string = "fields_updated")
                )
            }
            is AnalyticsEvent.OfflineActionQueued -> {
                parameters.setObject(
                    NSString.create(string = event.actionType),
                    NSString.create(string = "action_type")
                )
                parameters.setObject(
                    NSNumber.numberWithInt(event.queueSize),
                    NSString.create(string = "queue_size")
                )
            }
            is AnalyticsEvent.SyncCompleted -> {
                parameters.setObject(
                    NSNumber.numberWithInt(event.itemsSynced),
                    NSString.create(string = "items_synced")
                )
                parameters.setObject(
                    NSNumber.numberWithLong(event.durationMs),
                    NSString.create(string = "duration_ms")
                )
            }
            is AnalyticsEvent.SyncFailed -> {
                parameters.setObject(
                    NSString.create(string = event.errorType),
                    NSString.create(string = "error_type")
                )
                parameters.setObject(
                    NSNumber.numberWithInt(event.itemsPending),
                    NSString.create(string = "items_pending")
                )
            }
            is AnalyticsEvent.ErrorOccurred -> {
                parameters.setObject(
                    NSString.create(string = event.errorType),
                    NSString.create(string = "error_type")
                )
                event.errorContext?.let {
                    parameters.setObject(
                        NSString.create(string = it),
                        NSString.create(string = "error_context")
                    )
                }
                parameters.setObject(
                    NSNumber.numberWithBool(event.isFatal),
                    NSString.create(string = "is_fatal")
                )
            }
            is AnalyticsEvent.ApiError -> {
                parameters.setObject(
                    NSString.create(string = event.endpoint),
                    NSString.create(string = "endpoint")
                )
                parameters.setObject(
                    NSNumber.numberWithInt(event.statusCode),
                    NSString.create(string = "status_code")
                )
                event.errorMessage?.let {
                    parameters.setObject(
                        NSString.create(string = it),
                        NSString.create(string = "error_message")
                    )
                }
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

        // Log to Firebase Analytics
        FIRAnalytics.logEventWithName(
            NSString.create(string = event.eventName),
            parameters as NSDictionary
        )

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
    override fun setUserProperty(name: String, value: String) {
        if (!isEnabled) return
        FIRAnalytics.setUserPropertyString(
            NSString.create(string = value),
            NSString.create(string = name)
        )
    }

    /**
     * Set user ID.
     *
     * @param userId User identifier, or null to clear
     */
    override fun setUserId(userId: String?) {
        if (!isEnabled) return
        val nsUserId = userId?.let { NSString.create(string = it) }
        FIRAnalytics.setUserID(nsUserId)
    }

    /**
     * Enable or disable analytics collection.
     * Used for RGPD consent management.
     *
     * @param enabled true to enable, false to disable
     */
    override fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        FIRAnalytics.setAnalyticsCollectionEnabled(enabled)
    }

    /**
     * Clear all user data from analytics.
     * Used when user revokes consent.
     */
    override fun clearUserData() {
        FIRAnalytics.resetAnalyticsData()
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
