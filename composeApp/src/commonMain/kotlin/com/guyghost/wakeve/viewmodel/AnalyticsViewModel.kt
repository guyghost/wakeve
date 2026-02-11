package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.analytics.AnalyticsProvider
import kotlinx.coroutines.launch

/**
 * Base class for ViewModels with analytics tracking capabilities.
 *
 * This ViewModel extends the standard Android ViewModel and adds
 * helper methods for tracking analytics events. All ViewModels that
 * need analytics tracking should extend this class.
 *
 * ## Usage
 *
 * ```kotlin
 * class MyViewModel(
 *     analyticsProvider: AnalyticsProvider
 * ) : AnalyticsViewModel(analyticsProvider) {
 *
 *     init {
 *         trackScreenView("my_screen", "MyViewModel")
 *     }
 *
 *     fun onUserAction() {
 *         trackEvent(AnalyticsEvent.EventViewed(eventId, "button"))
 *     }
 *
 *     fun performOperation() {
 *         viewModelScope.launch {
 *             try {
 *                 // ... operation
 *             } catch (e: Exception) {
 *                 trackError("operation_failed", e.message)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @property analyticsProvider The analytics provider for tracking events
 */
abstract class AnalyticsViewModel(
    protected val analyticsProvider: AnalyticsProvider
) : ViewModel() {

    /**
     * Track an analytics event with optional properties.
     *
     * This method is coroutine-based to avoid blocking the UI thread
     * when communicating with the analytics provider.
     *
     * @param event The analytics event to track
     * @param properties Optional custom properties for the event
     */
    protected fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?> = emptyMap()) {
        viewModelScope.launch {
            analyticsProvider.trackEvent(event, properties)
        }
    }

    /**
     * Track a screen view event.
     *
     * This is a convenience method for tracking screen views,
     * which is a common use case in ViewModels.
     *
     * @param screenName The name of the screen being viewed
     * @param screenClass Optional class name of the screen (e.g., ViewModel class name)
     */
    protected fun trackScreenView(screenName: String, screenClass: String? = null) {
        trackEvent(AnalyticsEvent.ScreenView(screenName, screenClass))
    }

    /**
     * Track an error event.
     *
     * This method should be called when an error occurs during
     * ViewModel operations to help with debugging and monitoring.
     *
     * @param errorType The type of error that occurred
     * @param context Optional context about when/where the error occurred
     * @param isFatal Whether this is a fatal error (should crash the app)
     */
    protected fun trackError(errorType: String, context: String?, isFatal: Boolean = false) {
        trackEvent(AnalyticsEvent.ErrorOccurred(errorType, context, isFatal))
    }
}
