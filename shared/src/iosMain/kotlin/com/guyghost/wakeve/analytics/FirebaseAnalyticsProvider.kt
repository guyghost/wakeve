package com.guyghost.wakeve.analytics

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
    actual override fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?>) {
        if (!isEnabled) return

        // Convert properties to NSDictionary
        val parameters = NSMutableDictionary()

        // Add custom properties
        properties.forEach { (key, value) ->
            val nsValue: Any = when (value) {
                is String -> value as NSString
                is Int -> NSNumber(int = value)
                is Long -> NSNumber(longLong = value)
                is Double -> NSNumber(double = value)
                is Boolean -> NSNumber(bool = value)
                else -> (value?.toString() ?: "") as NSString
            }
            parameters.setObject(nsValue, key as NSString)
        }

        // Add event-specific parameters
        when (event) {
            is AnalyticsEvent.EventCreated -> {
                parameters.setObject(
                    event.eventType as NSString,
                    "event_type" as NSString
                )
                parameters.setObject(
                    NSNumber(bool = event.hasLocation),
                    "has_location" as NSString
                )
                parameters.setObject(
                    NSNumber(int = event.timeSlotsCount),
                    "time_slots_count" as NSString
                )
            }
            is AnalyticsEvent.EventJoined -> {
                parameters.setObject(
                    event.eventId as NSString,
                    "event_id" as NSString
                )
                parameters.setObject(
                    NSNumber(bool = event.isGuest),
                    "is_guest" as NSString
                )
            }
            is AnalyticsEvent.EventViewed -> {
                parameters.setObject(
                    event.eventId as NSString,
                    "event_id" as NSString
                )
                parameters.setObject(
                    event.source as NSString,
                    "source" as NSString
                )
            }
            is AnalyticsEvent.EventShared -> {
                parameters.setObject(
                    event.eventId as NSString,
                    "event_id" as NSString
                )
                parameters.setObject(
                    event.shareMethod as NSString,
                    "share_method" as NSString
                )
            }
            is AnalyticsEvent.PollVoted -> {
                parameters.setObject(
                    event.eventId as NSString,
                    "event_id" as NSString
                )
                parameters.setObject(
                    event.response as NSString,
                    "response" as NSString
                )
                parameters.setObject(
                    NSNumber(bool = event.isChangingVote),
                    "is_changing_vote" as NSString
                )
            }
            is AnalyticsEvent.PollViewed -> {
                parameters.setObject(
                    event.eventId as NSString,
                    "event_id" as NSString
                )
            }
            is AnalyticsEvent.PollClosed -> {
                parameters.setObject(
                    event.eventId as NSString,
                    "event_id" as NSString
                )
                parameters.setObject(
                    NSNumber(int = event.participantsCount),
                    "participants_count" as NSString
                )
                parameters.setObject(
                    NSNumber(int = event.votesCount),
                    "votes_count" as NSString
                )
            }
            is AnalyticsEvent.ScreenView -> {
                parameters.setObject(
                    event.screenName as NSString,
                    "screen_name" as NSString
                )
                event.screenClass?.let {
                    parameters.setObject(
                        it as NSString,
                        "screen_class" as NSString
                    )
                }
            }
            is AnalyticsEvent.ScenarioCreated -> {
                parameters.setObject(
                    event.eventId as NSString,
                    "event_id" as NSString
                )
                parameters.setObject(
                    NSNumber(bool = event.hasAccommodation),
                    "has_accommodation" as NSString
                )
            }
            is AnalyticsEvent.ScenarioViewed -> {
                parameters.setObject(
                    event.eventId as NSString,
                    "event_id" as NSString
                )
                parameters.setObject(
                    event.scenarioId as NSString,
                    "scenario_id" as NSString
                )
            }
            is AnalyticsEvent.ScenarioSelected -> {
                parameters.setObject(
                    event.eventId as NSString,
                    "event_id" as NSString
                )
                parameters.setObject(
                    event.scenarioId as NSString,
                    "scenario_id" as NSString
                )
            }
            is AnalyticsEvent.ScenarioVoted -> {
                parameters.setObject(
                    event.eventId as NSString,
                    "event_id" as NSString
                )
                parameters.setObject(
                    event.scenarioId as NSString,
                    "scenario_id" as NSString
                )
                parameters.setObject(
                    event.vote as NSString,
                    "vote" as NSString
                )
            }
            is AnalyticsEvent.MeetingCreated -> {
                parameters.setObject(
                    event.eventId as NSString,
                    "event_id" as NSString
                )
                parameters.setObject(
                    event.platform as NSString,
                    "platform" as NSString
                )
            }
            is AnalyticsEvent.MeetingJoined -> {
                parameters.setObject(
                    event.eventId as NSString,
                    "event_id" as NSString
                )
                parameters.setObject(
                    event.meetingId as NSString,
                    "meeting_id" as NSString
                )
                parameters.setObject(
                    event.platform as NSString,
                    "platform" as NSString
                )
            }
            is AnalyticsEvent.MeetingLinkGenerated -> {
                parameters.setObject(
                    event.eventId as NSString,
                    "event_id" as NSString
                )
                parameters.setObject(
                    event.platform as NSString,
                    "platform" as NSString
                )
            }
            is AnalyticsEvent.UserRegistered -> {
                parameters.setObject(
                    event.authMethod as NSString,
                    "auth_method" as NSString
                )
            }
            is AnalyticsEvent.UserLoggedIn -> {
                parameters.setObject(
                    event.authMethod as NSString,
                    "auth_method" as NSString
                )
            }
            is AnalyticsEvent.UserProfileUpdated -> {
                parameters.setObject(
                    event.fieldsUpdated.joinToString(",") as NSString,
                    "fields_updated" as NSString
                )
            }
            is AnalyticsEvent.OfflineActionQueued -> {
                parameters.setObject(
                    event.actionType as NSString,
                    "action_type" as NSString
                )
                parameters.setObject(
                    NSNumber(int = event.queueSize),
                    "queue_size" as NSString
                )
            }
            is AnalyticsEvent.SyncCompleted -> {
                parameters.setObject(
                    NSNumber(int = event.itemsSynced),
                    "items_synced" as NSString
                )
                parameters.setObject(
                    NSNumber(longLong = event.durationMs),
                    "duration_ms" as NSString
                )
            }
            is AnalyticsEvent.SyncFailed -> {
                parameters.setObject(
                    event.errorType as NSString,
                    "error_type" as NSString
                )
                parameters.setObject(
                    NSNumber(int = event.itemsPending),
                    "items_pending" as NSString
                )
            }
            is AnalyticsEvent.ErrorOccurred -> {
                parameters.setObject(
                    event.errorType as NSString,
                    "error_type" as NSString
                )
                event.errorContext?.let {
                    parameters.setObject(
                        it as NSString,
                        "error_context" as NSString
                    )
                }
                parameters.setObject(
                    NSNumber(bool = event.isFatal),
                    "is_fatal" as NSString
                )
            }
            is AnalyticsEvent.ApiError -> {
                parameters.setObject(
                    event.endpoint as NSString,
                    "endpoint" as NSString
                )
                parameters.setObject(
                    NSNumber(int = event.statusCode),
                    "status_code" as NSString
                )
                event.errorMessage?.let {
                    parameters.setObject(
                        it as NSString,
                        "error_message" as NSString
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

        // Log to Firebase Analytics using the native iOS FIRAnalytics
        // This is called via the Kotlin/Native interop with the Firebase iOS SDK
        // The FIRAnalytics class is available through the CocoaPods integration
        logFirebaseEvent(event.eventName, parameters)

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
        // FIRAnalytics.setUserPropertyString is called via native interop
        setFirebaseUserProperty(value, name)
    }

    /**
     * Set user ID.
     *
     * @param userId User identifier, or null to clear
     */
    actual override fun setUserId(userId: String?) {
        if (!isEnabled) return
        setFirebaseUserId(userId)
    }

    /**
     * Enable or disable analytics collection.
     * Used for RGPD consent management.
     *
     * @param enabled true to enable, false to disable
     */
    actual override fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        setFirebaseAnalyticsCollectionEnabled(enabled)
    }

    /**
     * Clear all user data from analytics.
     * Used when user revokes consent.
     */
    actual override fun clearUserData() {
        resetFirebaseAnalyticsData()
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

    // Native interop functions - these are implemented as expect/actual
    // and will call the actual Firebase iOS SDK methods
    
    private fun logFirebaseEvent(name: String, parameters: NSMutableDictionary) {
        // This will be implemented via cinterop with Firebase Analytics
        // For now, we just log to console in debug builds
        // TODO: Implement actual Firebase Analytics logging via cinterop
        println("[Analytics] Event: $name, Parameters: $parameters")
    }
    
    private fun setFirebaseUserProperty(value: String, name: String) {
        // TODO: Implement via cinterop with Firebase Analytics
        println("[Analytics] Set User Property: $name = $value")
    }
    
    private fun setFirebaseUserId(userId: String?) {
        // TODO: Implement via cinterop with Firebase Analytics
        println("[Analytics] Set User ID: $userId")
    }
    
    private fun setFirebaseAnalyticsCollectionEnabled(enabled: Boolean) {
        // TODO: Implement via cinterop with Firebase Analytics
        println("[Analytics] Set Collection Enabled: $enabled")
    }
    
    private fun resetFirebaseAnalyticsData() {
        // TODO: Implement via cinterop with Firebase Analytics
        println("[Analytics] Reset Data")
    }
}
