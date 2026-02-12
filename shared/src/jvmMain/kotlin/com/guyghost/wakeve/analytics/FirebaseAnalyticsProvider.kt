package com.guyghost.wakeve.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Firebase Analytics implementation for JVM (for testing purposes).
 *
 * Note: This is a minimal implementation for testing common code.
 * In production, actual Firebase SDK is only available on Android and iOS.
 *
 * @param analyticsQueue Queue for offline analytics event backup
 */
actual class FirebaseAnalyticsProvider actual constructor(
    private val analyticsQueue: AnalyticsQueue
) : AnalyticsProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isEnabled = true

    init {
        // No Firebase SDK for JVM, just logging would be used
        startSyncJob()
    }

    /**
     * Track an analytics event.
     *
     * For JVM, this just queues event for offline backup.
     *
     * @param event The event to track
     * @param properties Optional custom properties for the event
     */
    actual override fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?>) {
        if (!isEnabled) return

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
        // No-op for JVM testing
    }

    /**
     * Set user ID.
     *
     * @param userId User identifier, or null to clear
     */
    actual override fun setUserId(userId: String?) {
        if (!isEnabled) return
        // No-op for JVM testing
    }

    /**
     * Enable or disable analytics collection.
     * Used for RGPD consent management.
     *
     * @param enabled true to enable, false to disable
     */
    actual override fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * Clear all user data from analytics.
     * Used when user revokes consent.
     */
    actual override fun clearUserData() {
        scope.launch {
            analyticsQueue.clear()
        }
    }

    /**
     * Start periodic sync job for offline events.
     * No-op for JVM testing.
     */
    private fun startSyncJob() {
        scope.launch {
            // No-op for JVM testing
            // In production, this would sync with Firebase
        }
    }
}
