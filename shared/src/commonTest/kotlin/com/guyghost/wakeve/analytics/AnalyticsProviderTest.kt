package com.guyghost.wakeve.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnalyticsProviderTest {

    @Test
    fun `trackEvent adds event to tracked list`() {
        // Given
        val mockProvider = MockAnalyticsProvider()

        // When
        mockProvider.trackEvent(AnalyticsEvent.AppStart)

        // Then
        assertEquals(1, mockProvider.trackedEvents.size)
        assertTrue(mockProvider.hasEvent(AnalyticsEvent.AppStart::class))
    }

    @Test
    fun `trackEvent with properties stores properties`() {
        // Given
        val mockProvider = MockAnalyticsProvider()
        val properties = mapOf("screen" to "home", "user_type" to "premium")

        // When
        mockProvider.trackEvent(
            AnalyticsEvent.ScreenView("home"),
            properties
        )

        // Then
        val event = mockProvider.trackedEvents.first()
        assertEquals("home", event.properties["screen"])
        assertEquals("premium", event.properties["user_type"])
    }

    @Test
    fun `setUserProperty stores property`() {
        // Given
        val mockProvider = MockAnalyticsProvider()

        // When
        mockProvider.setUserProperty("role", "organizer")

        // Then
        assertEquals("organizer", mockProvider.userProperties["role"])
    }

    @Test
    fun `setUserId stores user id`() {
        // Given
        val mockProvider = MockAnalyticsProvider()

        // When
        mockProvider.setUserId("user-123")

        // Then
        assertEquals("user-123", mockProvider.userId)
    }

    @Test
    fun `setEnabled false prevents tracking`() {
        // Given
        val mockProvider = MockAnalyticsProvider()
        mockProvider.setEnabled(false)

        // When
        mockProvider.trackEvent(AnalyticsEvent.AppStart)

        // Then
        assertEquals(0, mockProvider.trackedEvents.size)
    }

    @Test
    fun `clearUserData clears all data`() {
        // Given
        val mockProvider = MockAnalyticsProvider()
        mockProvider.trackEvent(AnalyticsEvent.AppStart)
        mockProvider.setUserProperty("role", "admin")
        mockProvider.setUserId("user-123")

        // When
        mockProvider.clearUserData()

        // Then
        assertEquals(0, mockProvider.trackedEvents.size)
        assertEquals(0, mockProvider.userProperties.size)
        assertNull(mockProvider.userId)
        assertTrue(mockProvider.clearUserDataCalled)
    }

    @Test
    fun `findEvents returns correct event types`() {
        // Given
        val mockProvider = MockAnalyticsProvider()
        mockProvider.trackEvent(AnalyticsEvent.AppStart)
        mockProvider.trackEvent(AnalyticsEvent.ScreenView("home"))
        mockProvider.trackEvent(AnalyticsEvent.ScreenView("profile"))

        // When
        val screenViews = mockProvider.findEvents(AnalyticsEvent.ScreenView::class)

        // Then
        assertEquals(2, screenViews.size)
    }

    @Test
    fun `event created with all parameters`() {
        // Given
        val mockProvider = MockAnalyticsProvider()

        // When
        mockProvider.trackEvent(
            AnalyticsEvent.EventCreated(
                eventType = "BIRTHDAY",
                hasLocation = true,
                timeSlotsCount = 3
            )
        )

        // Then
        val event = mockProvider.trackedEvents.first()
        assertTrue(event.event is AnalyticsEvent.EventCreated)
        val eventData = event.event as AnalyticsEvent.EventCreated
        assertEquals("BIRTHDAY", eventData.eventType)
        assertEquals(true, eventData.hasLocation)
        assertEquals(3, eventData.timeSlotsCount)
    }

    @Test
    fun `poll voted event`() {
        // Given
        val mockProvider = MockAnalyticsProvider()

        // When
        mockProvider.trackEvent(
            AnalyticsEvent.PollVoted(
                eventId = "event-1",
                response = "yes",
                isChangingVote = false
            )
        )

        // Then
        assertTrue(mockProvider.hasEvent(AnalyticsEvent.PollVoted::class))
        val voteEvents = mockProvider.findEvents(AnalyticsEvent.PollVoted::class)
        assertEquals(1, voteEvents.size)
        assertEquals("yes", voteEvents[0].response)
        assertEquals(false, voteEvents[0].isChangingVote)
    }

    @Test
    fun `screen view with class parameter`() {
        // Given
        val mockProvider = MockAnalyticsProvider()

        // When
        mockProvider.trackEvent(
            AnalyticsEvent.ScreenView(
                screenName = "EventDetail",
                screenClass = "EventDetailScreen"
            )
        )

        // Then
        val screenViews = mockProvider.findEvents(AnalyticsEvent.ScreenView::class)
        assertEquals(1, screenViews.size)
        assertEquals("EventDetail", screenViews[0].screenName)
        assertEquals("EventDetailScreen", screenViews[0].screenClass)
    }

    @Test
    fun `multiple events tracked`() {
        // Given
        val mockProvider = MockAnalyticsProvider()

        // When
        mockProvider.trackEvent(AnalyticsEvent.AppStart)
        mockProvider.trackEvent(AnalyticsEvent.ScreenView("home"))
        mockProvider.trackEvent(AnalyticsEvent.EventViewed("event-1", "list"))
        mockProvider.trackEvent(AnalyticsEvent.AppForeground)

        // Then
        assertEquals(4, mockProvider.trackedEvents.size)
    }

    @Test
    fun `analytics consent events`() {
        // Given
        val mockProvider = MockAnalyticsProvider()

        // When
        mockProvider.trackEvent(AnalyticsEvent.AnalyticsConsentGranted)
        mockProvider.trackEvent(AnalyticsEvent.AnalyticsConsentRevoked)
        mockProvider.trackEvent(AnalyticsEvent.UserDataDeleted)

        // Then
        assertEquals(3, mockProvider.trackedEvents.size)
        assertTrue(mockProvider.hasEvent(AnalyticsEvent.AnalyticsConsentGranted::class))
        assertTrue(mockProvider.hasEvent(AnalyticsEvent.AnalyticsConsentRevoked::class))
        assertTrue(mockProvider.hasEvent(AnalyticsEvent.UserDataDeleted::class))
    }
}
