package com.guyghost.wakeve.viewmodel

import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.analytics.AnalyticsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

/**
 * Mock implementation of AnalyticsProvider for testing.
 */
class MockAnalyticsProvider : AnalyticsProvider {
    private val _trackedEvents = mutableListOf<Pair<AnalyticsEvent, Map<String, Any?>>>()
    val trackedEvents: List<Pair<AnalyticsEvent, Map<String, Any?>>>
        get() = _trackedEvents

    private val _userProperties = mutableMapOf<String, String>()
    val userProperties: Map<String, String>
        get() = _userProperties

    private var _userId: String? = null
    val userId: String?
        get() = _userId

    private var _enabled = true
    val isEnabled: Boolean
        get() = _enabled

    override fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?>) {
        if (_enabled) {
            _trackedEvents.add(event to properties)
        }
    }

    override fun setUserProperty(name: String, value: String) {
        _userProperties[name] = value
    }

    override fun setUserId(userId: String?) {
        _userId = userId
    }

    override fun setEnabled(enabled: Boolean) {
        _enabled = enabled
    }

    override fun clearUserData() {
        _trackedEvents.clear()
        _userProperties.clear()
        _userId = null
    }

    /**
     * Reset the mock state.
     */
    fun reset() {
        _trackedEvents.clear()
        _userProperties.clear()
        _userId = null
        _enabled = true
    }

    /**
     * Check if a specific event was tracked.
     */
    fun wasEventTracked(eventName: String): Boolean {
        return _trackedEvents.any { it.first.eventName == eventName }
    }

    /**
     * Get all events with the specified name.
     */
    fun getEventsByName(eventName: String): List<Pair<AnalyticsEvent, Map<String, Any?>>> {
        return _trackedEvents.filter { it.first.eventName == eventName }
    }

    /**
     * Get the last tracked event (if any).
     */
    fun getLastEvent(): AnalyticsEvent? {
        return _trackedEvents.lastOrNull()?.first
    }
}

/**
 * Base test class for ViewModels with common setup.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTestBase {
    protected val testDispatcher = UnconfinedTestDispatcher()

    @Before
    open fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    open fun tearDown() {
        Dispatchers.resetMain()
    }

    protected val mockAnalyticsProvider = MockAnalyticsProvider()
}
