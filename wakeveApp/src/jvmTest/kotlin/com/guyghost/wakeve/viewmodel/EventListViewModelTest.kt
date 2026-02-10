package com.guyghost.wakeve.viewmodel

import com.guyghost.wakeve.EventRepository
import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.TimeOfDay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for EventListViewModel.
 */
class EventListViewModelTest : ViewModelTestBase() {

    @Mock
    private lateinit var eventRepository: EventRepository

    private lateinit var viewModel: EventListViewModel

    @Before
    override fun setUp() {
        super.setUp()
        MockitoAnnotations.openMocks(this)
        viewModel = EventListViewModel(eventRepository, mockAnalyticsProvider)
    }

    @Test
    fun `init should track screen_view`() = runTest {
        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("screen_view"),
            "screen_view event should be tracked on init"
        )
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.ScreenView
        assertEquals("event_list", lastEvent?.screenName, "Screen name should be event_list")
    }

    @Test
    fun `init should load events`() = runTest {
        // Arrange
        val testEvents = listOf(
            Event(
                id = "event1",
                title = "Test Event 1",
                description = "Description 1",
                organizerId = "user1",
                participants = emptyList(),
                proposedSlots = emptyList(),
                deadline = "2024-12-31T23:59:59Z",
                status = EventStatus.DRAFT,
                createdAt = "2024-01-01T00:00:00Z",
                updatedAt = "2024-01-01T00:00:00Z",
                eventType = EventType.OTHER
            )
        )
        doReturn(testEvents).`when`(eventRepository).getAllEvents()

        // Act
        advanceUntilIdle()

        // Assert
        val events = viewModel.events.value
        assertEquals(testEvents.size, events.size, "Events should be loaded")
        assertEquals(testEvents[0].id, events[0].id, "Event ID should match")
    }

    @Test
    fun `onEventClicked should track event_viewed`() = runTest {
        // Arrange
        val eventId = "test_event_id"

        // Act
        viewModel.onEventClicked(eventId)
        advanceUntilIdle()

        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("event_viewed"),
            "event_viewed event should be tracked"
        )
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.EventViewed
        assertEquals(eventId, lastEvent?.eventId, "Event ID should match")
        assertEquals("list", lastEvent?.source, "Source should be 'list'")
    }

    @Test
    fun `onEventShared should track event_shared`() = runTest {
        // Arrange
        val eventId = "test_event_id"
        val shareMethod = "link"

        // Act
        viewModel.onEventShared(eventId, shareMethod)
        advanceUntilIdle()

        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("event_shared"),
            "event_shared event should be tracked"
        )
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.EventShared
        assertEquals(eventId, lastEvent?.eventId, "Event ID should match")
        assertEquals(shareMethod, lastEvent?.shareMethod, "Share method should match")
    }

    @Test
    fun `onCreateEventClick should track event_created`() = runTest {
        // Act
        viewModel.onCreateEventClick()
        advanceUntilIdle()

        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("event_created"),
            "event_created event should be tracked"
        )
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.EventCreated
        assertEquals("initiated", lastEvent?.eventType, "Event type should be 'initiated'")
        assertFalse(lastEvent?.hasLocation ?: true, "Has location should be false")
        assertEquals(0, lastEvent?.timeSlotsCount, "Time slots count should be 0")
    }

    @Test
    fun `loadEvents should set loading state correctly`() = runTest {
        // Arrange
        val testEvents = listOf(
            Event(
                id = "event1",
                title = "Test Event 1",
                description = "Description 1",
                organizerId = "user1",
                participants = emptyList(),
                proposedSlots = emptyList(),
                deadline = "2024-12-31T23:59:59Z",
                status = EventStatus.DRAFT,
                createdAt = "2024-01-01T00:00:00Z",
                updatedAt = "2024-01-01T00:00:00Z",
                eventType = EventType.OTHER
            )
        )
        doReturn(testEvents).`when`(eventRepository).getAllEvents()

        // Act
        viewModel.loadEvents()
        assertTrue(viewModel.isLoading.value, "Should be loading")

        // Wait for async operation
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.isLoading.value, "Should not be loading after completion")
    }
}
