package com.guyghost.wakeve.viewmodel

import com.guyghost.wakeve.EventRepository
import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for CreateEventViewModel.
 */
class CreateEventViewModelTest : ViewModelTestBase() {

    @Mock
    private lateinit var eventRepository: EventRepository

    private lateinit var viewModel: CreateEventViewModel

    @Before
    override fun setUp() {
        super.setUp()
        MockitoAnnotations.openMocks(this)
        viewModel = CreateEventViewModel(eventRepository, mockAnalyticsProvider)
    }

    @Test
    fun `init should track screen_view`() = runTest {
        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("screen_view"),
            "screen_view event should be tracked on init"
        )
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.ScreenView
        assertEquals("create_event", lastEvent?.screenName, "Screen name should be create_event")
    }

    @Test
    fun `createEvent with valid data should track event_created`() = runTest {
        // Arrange
        val title = "Test Event"
        val description = "Test Description"
        val eventType = EventType.BIRTHDAY
        val timeSlot = TimeSlot(
            id = "slot1",
            start = "2024-12-31T10:00:00Z",
            end = "2024-12-31T12:00:00Z",
            timezone = "UTC",
            timeOfDay = TimeOfDay.MORNING
        )

        doReturn(kotlin.Result.success(testEvent())).`when`(eventRepository)
            .createEvent(any())

        // Act
        viewModel.updateTitle(title)
        viewModel.updateDescription(description)
        viewModel.updateEventType(eventType)
        viewModel.addTimeSlot(timeSlot)
        viewModel.createEvent()
        advanceUntilIdle()

        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("event_created"),
            "event_created event should be tracked"
        )
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.EventCreated
        assertEquals(eventType.name, lastEvent?.eventType, "Event type should match")
        assertFalse(lastEvent?.hasLocation ?: true, "Has location should be false")
        assertEquals(1, lastEvent?.timeSlotsCount, "Time slots count should be 1")
        assertFalse(viewModel.isCreating.value, "Should not be creating after success")
        assertNull(viewModel.creationError.value, "No error should be present")
    }

    @Test
    fun `createEvent with empty title should show validation error`() = runTest {
        // Arrange
        val description = "Test Description"
        val timeSlot = TimeSlot(
            id = "slot1",
            start = "2024-12-31T10:00:00Z",
            end = "2024-12-31T12:00:00Z",
            timezone = "UTC"
        )

        // Act
        viewModel.updateTitle("")
        viewModel.updateDescription(description)
        viewModel.addTimeSlot(timeSlot)
        viewModel.createEvent()
        advanceUntilIdle()

        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("error_occurred"),
            "error_occurred event should be tracked for validation"
        )
        val error = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.ErrorOccurred
        assertEquals("validation_failed", error?.errorType, "Error type should be validation_failed")
        assertEquals("Title is required", viewModel.creationError.value, "Error message should match")
        verify(eventRepository, never()).createEvent(any())
    }

    @Test
    fun `createEvent with empty description should show validation error`() = runTest {
        // Arrange
        val title = "Test Event"
        val timeSlot = TimeSlot(
            id = "slot1",
            start = "2024-12-31T10:00:00Z",
            end = "2024-12-31T12:00:00Z",
            timezone = "UTC"
        )

        // Act
        viewModel.updateTitle(title)
        viewModel.updateDescription("")
        viewModel.addTimeSlot(timeSlot)
        viewModel.createEvent()
        advanceUntilIdle()

        // Assert
        assertEquals("Description is required", viewModel.creationError.value, "Error message should match")
        verify(eventRepository, never()).createEvent(any())
    }

    @Test
    fun `createEvent without time slots should show validation error`() = runTest {
        // Arrange
        val title = "Test Event"
        val description = "Test Description"

        // Act
        viewModel.updateTitle(title)
        viewModel.updateDescription(description)
        viewModel.createEvent()
        advanceUntilIdle()

        // Assert
        assertEquals("At least one time slot is required", viewModel.creationError.value, "Error message should match")
        verify(eventRepository, never()).createEvent(any())
    }

    @Test
    fun `createEvent with CUSTOM type but no custom text should show validation error`() = runTest {
        // Arrange
        val title = "Test Event"
        val description = "Test Description"
        val timeSlot = TimeSlot(
            id = "slot1",
            start = "2024-12-31T10:00:00Z",
            end = "2024-12-31T12:00:00Z",
            timezone = "UTC"
        )

        // Act
        viewModel.updateTitle(title)
        viewModel.updateDescription(description)
        viewModel.updateEventType(EventType.CUSTOM)
        viewModel.addTimeSlot(timeSlot)
        viewModel.createEvent()
        advanceUntilIdle()

        // Assert
        assertEquals(
            "Custom event type requires a description",
            viewModel.creationError.value,
            "Error message should match"
        )
        verify(eventRepository, never()).createEvent(any())
    }

    @Test
    fun `createEvent with max less than min should show validation error`() = runTest {
        // Arrange
        val title = "Test Event"
        val description = "Test Description"
        val timeSlot = TimeSlot(
            id = "slot1",
            start = "2024-12-31T10:00:00Z",
            end = "2024-12-31T12:00:00Z",
            timezone = "UTC"
        )

        // Act
        viewModel.updateTitle(title)
        viewModel.updateDescription(description)
        viewModel.updateMinParticipants(10)
        viewModel.updateMaxParticipants(5)
        viewModel.addTimeSlot(timeSlot)
        viewModel.createEvent()
        advanceUntilIdle()

        // Assert
        assertEquals(
            "Maximum participants must be greater than or equal to minimum",
            viewModel.creationError.value,
            "Error message should match"
        )
        verify(eventRepository, never()).createEvent(any())
    }

    @Test
    fun `createEvent with locations should track hasLocation true`() = runTest {
        // Arrange
        val title = "Test Event"
        val description = "Test Description"
        val location = com.guyghost.wakeve.models.PotentialLocation(
            id = "loc1",
            name = "Test Location",
            latitude = 40.7128,
            longitude = -74.0060,
            locationType = com.guyghost.wakeve.models.LocationType.CITY
        )
        val timeSlot = TimeSlot(
            id = "slot1",
            start = "2024-12-31T10:00:00Z",
            end = "2024-12-31T12:00:00Z",
            timezone = "UTC"
        )

        doReturn(kotlin.Result.success(testEvent())).`when`(eventRepository)
            .createEvent(any())

        // Act
        viewModel.updateTitle(title)
        viewModel.updateDescription(description)
        viewModel.addPotentialLocation(location)
        viewModel.addTimeSlot(timeSlot)
        viewModel.createEvent()
        advanceUntilIdle()

        // Assert
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.EventCreated
        assertTrue(lastEvent?.hasLocation ?: false, "Has location should be true")
    }

    @Test
    fun `createEvent on repository failure should track error`() = runTest {
        // Arrange
        val title = "Test Event"
        val description = "Test Description"
        val timeSlot = TimeSlot(
            id = "slot1",
            start = "2024-12-31T10:00:00Z",
            end = "2024-12-31T12:00:00Z",
            timezone = "UTC"
        )
        val errorMessage = "Failed to create event"

        doReturn(kotlin.Result.failure<Unit>(IllegalArgumentException(errorMessage))).`when`(eventRepository)
            .createEvent(any())

        // Act
        viewModel.updateTitle(title)
        viewModel.updateDescription(description)
        viewModel.addTimeSlot(timeSlot)
        viewModel.createEvent()
        advanceUntilIdle()

        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("error_occurred"),
            "error_occurred event should be tracked"
        )
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.ErrorOccurred
        assertEquals("create_event_failed", lastEvent?.errorType, "Error type should match")
        assertEquals(errorMessage, viewModel.creationError.value, "Error message should match")
        assertFalse(viewModel.isCreating.value, "Should not be creating after error")
    }

    @Test
    fun `clearError should clear error state`() = runTest {
        // Arrange
        viewModel.updateTitle("")
        viewModel.updateDescription("desc")
        viewModel.addTimeSlot(
            TimeSlot(id = "slot1", start = "2024-01-01T00:00:00Z", end = "2024-01-01T01:00:00Z", timezone = "UTC")
        )
        viewModel.createEvent()
        advanceUntilIdle()

        // Assert error is set
        assertNotNull(viewModel.creationError.value, "Error should be present")

        // Act
        viewModel.clearError()

        // Assert
        assertNull(viewModel.creationError.value, "Error should be cleared")
    }

    @Test
    fun `createEvent should reset form on success`() = runTest {
        // Arrange
        val title = "Test Event"
        val description = "Test Description"
        val timeSlot = TimeSlot(
            id = "slot1",
            start = "2024-12-31T10:00:00Z",
            end = "2024-12-31T12:00:00Z",
            timezone = "UTC"
        )

        doReturn(kotlin.Result.success(testEvent())).`when`(eventRepository)
            .createEvent(any())

        // Act
        viewModel.updateTitle(title)
        viewModel.updateDescription(description)
        viewModel.addTimeSlot(timeSlot)
        viewModel.createEvent()
        advanceUntilIdle()

        // Assert
        assertEquals("", viewModel.title.value, "Title should be reset")
        assertEquals("", viewModel.description.value, "Description should be reset")
        assertEquals(0, viewModel.timeSlots.value.size, "Time slots should be reset")
    }

    @Test
    fun `createEvent should pass correct data to repository`() = runTest {
        // Arrange
        val title = "Test Event Title"
        val description = "Test Event Description"
        val eventType = EventType.TEAM_BUILDING
        val timeSlot = TimeSlot(
            id = "slot1",
            start = "2024-12-31T10:00:00Z",
            end = "2024-12-31T12:00:00Z",
            timezone = "UTC",
            timeOfDay = TimeOfDay.AFTERNOON
        )
        val minParticipants = 5
        val maxParticipants = 20
        val expectedParticipants = 12

        doReturn(kotlin.Result.success(testEvent())).`when`(eventRepository)
            .createEvent(any())

        // Act
        viewModel.updateTitle(title)
        viewModel.updateDescription(description)
        viewModel.updateEventType(eventType)
        viewModel.updateMinParticipants(minParticipants)
        viewModel.updateMaxParticipants(maxParticipants)
        viewModel.updateExpectedParticipants(expectedParticipants)
        viewModel.addTimeSlot(timeSlot)
        viewModel.createEvent()
        advanceUntilIdle()

        // Assert
        val captor = argumentCaptor<Event>()
        verify(eventRepository).createEvent(captor.capture())
        val capturedEvent = captor.firstValue
        assertEquals(title.trim(), capturedEvent.title, "Title should match (trimmed)")
        assertEquals(description.trim(), capturedEvent.description, "Description should match (trimmed)")
        assertEquals(eventType, capturedEvent.eventType, "Event type should match")
        assertEquals(minParticipants, capturedEvent.minParticipants, "Min participants should match")
        assertEquals(maxParticipants, capturedEvent.maxParticipants, "Max participants should match")
        assertEquals(expectedParticipants, capturedEvent.expectedParticipants, "Expected participants should match")
        assertEquals(1, capturedEvent.proposedSlots.size, "Should have 1 time slot")
        assertEquals(timeSlot, capturedEvent.proposedSlots[0], "Time slot should match")
        assertEquals(EventStatus.DRAFT, capturedEvent.status, "Status should be DRAFT")
    }

    private fun testEvent(): Event {
        return Event(
            id = "event1",
            title = "Test Event",
            description = "Description",
            organizerId = "user1",
            participants = emptyList(),
            proposedSlots = emptyList(),
            deadline = "2024-12-31T23:59:59Z",
            status = EventStatus.DRAFT,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            eventType = EventType.OTHER
        )
    }
}
