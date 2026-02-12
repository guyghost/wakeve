package com.guyghost.wakeve.analytics

import kotlin.reflect.KClass

/**
 * Mock implementation of AnalyticsProvider for testing.
 */
class MockAnalyticsProvider : AnalyticsProvider {

    data class TrackedEvent(
        val event: AnalyticsEvent,
        val properties: Map<String, Any?>
    )

    private val _trackedEvents = mutableListOf<TrackedEvent>()
    val trackedEvents: List<TrackedEvent> get() = _trackedEvents.toList()

    private val _userProperties = mutableMapOf<String, String>()
    val userProperties: Map<String, String> get() = _userProperties.toMap()

    private var _userId: String? = null
    val userId: String? get() = _userId

    private var _enabled = true
    val isEnabled: Boolean get() = _enabled

    var clearUserDataCalled = false

    override fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?>) {
        if (_enabled) {
            _trackedEvents.add(TrackedEvent(event, properties))
        }
    }

    override fun setUserProperty(name: String, value: String) {
        if (_enabled) {
            _userProperties[name] = value
        }
    }

    override fun setUserId(userId: String?) {
        if (_enabled) {
            _userId = userId
        }
    }

    override fun setEnabled(enabled: Boolean) {
        _enabled = enabled
    }

    override fun clearUserData() {
        _trackedEvents.clear()
        _userProperties.clear()
        _userId = null
        clearUserDataCalled = true
    }

    fun clear() {
        _trackedEvents.clear()
        _userProperties.clear()
        _userId = null
        clearUserDataCalled = false
        _enabled = true
    }

    /**
     * Check if an event of the given type was tracked.
     * Uses reflection to avoid inline/reified type issues.
     */
    fun hasEvent(eventClass: KClass<out AnalyticsEvent>): Boolean {
        return trackedEvents.any { eventClass.isInstance(it.event) }
    }

    /**
     * Find all events of the given type.
     * Uses reflection to avoid inline/reified type issues.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : AnalyticsEvent> findEvents(eventClass: KClass<T>): List<T> {
        return trackedEvents
            .mapNotNull { it.event as? T }
            .filter { eventClass.isInstance(it) }
    }
}
