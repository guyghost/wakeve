package com.guyghost.wakeve.viewmodel

import com.guyghost.wakeve.analytics.AnalyticsEvent
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for AnalyticsViewModel.
 */
class AnalyticsViewModelTest : ViewModelTestBase() {

    @Test
    fun `trackEvent should track event through provider`() = runTest {
        // Arrange
        val viewModel = TestAnalyticsViewModel(mockAnalyticsProvider)
        val testEvent = AnalyticsEvent.AppStart
        val testProperties = mapOf("key" to "value")

        // Act
        viewModel.trackEvent(testEvent, testProperties)
        advanceUntilIdle()

        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked(testEvent.eventName),
            "Event should be tracked"
        )
        val tracked = mockAnalyticsProvider.getEventsByName(testEvent.eventName)
        assertEquals(1, tracked.size, "One event should be tracked")
        assertEquals(testProperties, tracked[0].second, "Properties should match")
    }

    @Test
    fun `trackScreenView should track screen_view event`() = runTest {
        // Arrange
        val viewModel = TestAnalyticsViewModel(mockAnalyticsProvider)
        val screenName = "test_screen"
        val screenClass = "TestClass"

        // Act
        viewModel.trackScreenView(screenName, screenClass)
        advanceUntilIdle()

        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("screen_view"),
            "screen_view event should be tracked"
        )
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.ScreenView
        assertNotNull(lastEvent, "Last event should be ScreenView")
        assertEquals(screenName, lastEvent.screenName, "Screen name should match")
        assertEquals(screenClass, lastEvent.screenClass, "Screen class should match")
    }

    @Test
    fun `trackError should track error_occurred event`() = runTest {
        // Arrange
        val viewModel = TestAnalyticsViewModel(mockAnalyticsProvider)
        val errorType = "test_error"
        val context = "test_context"
        val isFatal = true

        // Act
        viewModel.trackError(errorType, context, isFatal)
        advanceUntilIdle()

        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("error_occurred"),
            "error_occurred event should be tracked"
        )
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.ErrorOccurred
        assertNotNull(lastEvent, "Last event should be ErrorOccurred")
        assertEquals(errorType, lastEvent.errorType, "Error type should match")
        assertEquals(context, lastEvent.errorContext, "Error context should match")
        assertEquals(isFatal, lastEvent.isFatal, "Is fatal should match")
    }

    @Test
    fun `trackScreenView with null screenClass should work`() = runTest {
        // Arrange
        val viewModel = TestAnalyticsViewModel(mockAnalyticsProvider)
        val screenName = "test_screen"

        // Act
        viewModel.trackScreenView(screenName)
        advanceUntilIdle()

        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("screen_view"),
            "screen_view event should be tracked"
        )
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.ScreenView
        assertNotNull(lastEvent, "Last event should be ScreenView")
        assertEquals(screenName, lastEvent.screenName, "Screen name should match")
    }

    @Test
    fun `trackError with null context should work`() = runTest {
        // Arrange
        val viewModel = TestAnalyticsViewModel(mockAnalyticsProvider)
        val errorType = "test_error"

        // Act
        viewModel.trackError(errorType, null)
        advanceUntilIdle()

        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("error_occurred"),
            "error_occurred event should be tracked"
        )
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.ErrorOccurred
        assertNotNull(lastEvent, "Last event should be ErrorOccurred")
        assertEquals(errorType, lastEvent.errorType, "Error type should match")
    }
}

/**
 * Test implementation of AnalyticsViewModel for testing.
 */
class TestAnalyticsViewModel(analyticsProvider: com.guyghost.wakeve.analytics.AnalyticsProvider) :
    AnalyticsViewModel(analyticsProvider) {
    // Public access to protected methods for testing
    fun publicTrackEvent(event: AnalyticsEvent, properties: Map<String, Any?> = emptyMap()) {
        trackEvent(event, properties)
    }

    fun publicTrackScreenView(screenName: String, screenClass: String? = null) {
        trackScreenView(screenName, screenClass)
    }

    fun publicTrackError(errorType: String, context: String?, isFatal: Boolean = false) {
        trackError(errorType, context, isFatal)
    }
}
