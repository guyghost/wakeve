package com.guyghost.wakeve.analytics

/**
 * Interface for analytics providers.
 *
 * This interface abstracts analytics operations to allow different
 * implementations (Firebase, Mixpanel, Amplitude) and enable testing.
 */
interface AnalyticsProvider {

    /**
     * Track an analytics event.
     *
     * @param event The event to track
     * @param properties Optional custom properties for the event
     */
    fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?> = emptyMap())

    /**
     * Set a user property.
     *
     * @param name Property name
     * @param value Property value
     */
    fun setUserProperty(name: String, value: String)

    /**
     * Set the user ID.
     *
     * @param userId User identifier, or null to clear
     */
    fun setUserId(userId: String?)

    /**
     * Enable or disable analytics collection.
     * Used for RGPD consent management.
     *
     * @param enabled true to enable, false to disable
     */
    fun setEnabled(enabled: Boolean)

    /**
     * Clear all user data from analytics.
     * Used when user revokes consent.
     */
    fun clearUserData()
}
