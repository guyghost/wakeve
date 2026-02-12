package com.guyghost.wakeve.analytics

/**
 * Firebase Analytics Provider - Platform-specific implementation.
 *
 * This expect class is implemented differently for Android and iOS platforms:
 * - Android: Uses Firebase SDK via Google Play Services
 * - iOS: Uses Firebase SDK via CocoaPods
 *
 * @param analyticsQueue Queue for offline analytics event backup
 */
expect class FirebaseAnalyticsProvider(
    analyticsQueue: AnalyticsQueue
) : AnalyticsProvider {

    override fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?>)
    override fun setUserProperty(name: String, value: String)
    override fun setUserId(userId: String?)
    override fun setEnabled(enabled: Boolean)
    override fun clearUserData()
}
