package com.guyghost.wakeve.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * iOS analytics provider.
 *
 * The first App Store build keeps iOS analytics local-only until the native
 * Firebase iOS SDK bridge is linked and reviewed against App Store privacy
 * labels. Events are queued for the existing consent/data-deletion flows but
 * are not emitted to a third-party analytics SDK on iOS.
 *
 * @param analyticsQueue Queue for offline analytics event backup
 */
actual class FirebaseAnalyticsProvider actual constructor(
    private val analyticsQueue: AnalyticsQueue
) : AnalyticsProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isEnabled = true

    init {
        startSyncJob()
    }

    /**
     * Track an analytics event.
     *
     * Queues the event locally for consent-aware retention/clear flows.
 *
 * @param event The event to track
 * @param properties Optional custom properties for the event
 */
    actual override fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?>) {
        if (!isEnabled) return

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
        // No-op until the native iOS analytics SDK bridge is intentionally enabled.
    }

    /**
     * Set user ID.
     *
     * @param userId User identifier, or null to clear
     */
    actual override fun setUserId(userId: String?) {
        if (!isEnabled) return
        // No-op until the native iOS analytics SDK bridge is intentionally enabled.
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
     * Offline analytics sync is intentionally disabled on iOS for the first
     * App Store build because no third-party analytics SDK is linked.
     */
    private fun startSyncJob() {
        // No-op.
    }
}
