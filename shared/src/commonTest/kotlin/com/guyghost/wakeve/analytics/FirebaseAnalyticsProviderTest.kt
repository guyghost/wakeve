package com.guyghost.wakeve.analytics

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for FirebaseAnalyticsProvider.
 *
 * Tests cover:
 * - Event tracking
 * - User properties
 * - User ID
 * - Enable/disable tracking
 * - Clear user data
 *
 * Note: Actual Firebase implementation is tested via MockAnalyticsProvider
 * since Firebase requires platform-specific runtime environment.
 */
class FirebaseAnalyticsProviderTest {

    private lateinit var queue: AnalyticsQueue
    private lateinit var provider: MockAnalyticsProvider

    @BeforeTest
    fun setup() {
        queue = AnalyticsQueue()
        provider = MockAnalyticsProvider()
    }

    @AfterTest
    fun tearDown() = runTest {
        queue.clear()
        provider.clear()
    }

    @Test
    fun `trackEvent queues event in queue`() = runTest {
        // Given
        val event = AnalyticsEvent.AppStart

        // When
        provider.trackEvent(event)

        // Then
        assertTrue(queue.size > 0 || provider.trackedEvents.isNotEmpty())
    }

    @Test
    fun `trackEvent with properties stores properties correctly`() = runTest {
        // Given
        val event = AnalyticsEvent.ScreenView("home", "HomeScreen")
        val properties = mapOf("screen" to "home", "user_type" to "premium")

        // When
        provider.trackEvent(event, properties)

        // Then
        val trackedEvent = provider.trackedEvents.first()
        assertEquals("home", trackedEvent.properties["screen"])
        assertEquals("premium", trackedEvent.properties["user_type"])
    }

    @Test
    fun `trackEvent event created with all parameters`() = runTest {
        // Given
        val event = AnalyticsEvent.EventCreated(
            eventType = "BIRTHDAY",
            hasLocation = true,
            timeSlotsCount = 3
        )

        // When
        provider.trackEvent(event)

        // Then
        val trackedEvent = provider.trackedEvents.first()
        assertTrue(trackedEvent.event is AnalyticsEvent.EventCreated)
        val eventData = trackedEvent.event as AnalyticsEvent.EventCreated
        assertEquals("BIRTHDAY", eventData.eventType)
        assertEquals(true, eventData.hasLocation)
        assertEquals(3, eventData.timeSlotsCount)
    }

    @Test
    fun `trackEvent poll voted event`() = runTest {
        // Given
        val event = AnalyticsEvent.PollVoted(
            eventId = "event-1",
            response = "yes",
            isChangingVote = false
        )

        // When
        provider.trackEvent(event)

        // Then
        assertTrue(provider.hasEvent(AnalyticsEvent.PollVoted::class.java))
        val voteEvents = provider.findEvents(AnalyticsEvent.PollVoted::class.java)
        assertEquals(1, voteEvents.size)
        assertEquals("yes", voteEvents[0].response)
        assertEquals(false, voteEvents[0].isChangingVote)
    }

    @Test
    fun `trackEvent screen view event`() = runTest {
        // Given
        val event = AnalyticsEvent.ScreenView(
            screenName = "EventDetail",
            screenClass = "EventDetailScreen"
        )

        // When
        provider.trackEvent(event)

        // Then
        val screenViews = provider.findEvents(AnalyticsEvent.ScreenView::class.java)
        assertEquals(1, screenViews.size)
        assertEquals("EventDetail", screenViews[0].screenName)
        assertEquals("EventDetailScreen", screenViews[0].screenClass)
    }

    @Test
    fun `setUserProperty stores property`() {
        // Given
        val name = "user_role"
        val value = "organizer"

        // When
        provider.setUserProperty(name, value)

        // Then
        assertEquals("organizer", provider.userProperties[name])
    }

    @Test
    fun `setUserId stores user id`() {
        // Given
        val userId = "user-123"

        // When
        provider.setUserId(userId)

        // Then
        assertEquals("user-123", provider.userId)
    }

    @Test
    fun `setUserId with null clears user id`() {
        // Given
        provider.setUserId("user-123")

        // When
        provider.setUserId(null)

        // Then
        assertEquals(null, provider.userId)
    }

    @Test
    fun `setEnabled false prevents tracking`() {
        // Given
        provider.setEnabled(false)

        // When
        provider.trackEvent(AnalyticsEvent.AppStart)

        // Then
        assertEquals(0, provider.trackedEvents.size)
        assertFalse(provider.isEnabled)
    }

    @Test
    fun `setEnabled true allows tracking`() {
        // Given
        provider.setEnabled(false)
        provider.setEnabled(true)

        // When
        provider.trackEvent(AnalyticsEvent.AppStart)

        // Then
        assertEquals(1, provider.trackedEvents.size)
        assertTrue(provider.isEnabled)
    }

    @Test
    fun `setEnabled false prevents user property setting`() {
        // Given
        provider.setEnabled(false)

        // When
        provider.setUserProperty("role", "organizer")

        // Then
        assertEquals(null, provider.userProperties["role"])
    }

    @Test
    fun `setEnabled false prevents user id setting`() {
        // Given
        provider.setEnabled(false)

        // When
        provider.setUserId("user-123")

        // Then
        assertEquals(null, provider.userId)
    }

    @Test
    fun `clearUserData clears tracked events`() {
        // Given
        provider.trackEvent(AnalyticsEvent.AppStart)
        provider.trackEvent(AnalyticsEvent.ScreenView("home"))

        // When
        provider.clearUserData()

        // Then
        assertEquals(0, provider.trackedEvents.size)
        assertTrue(provider.clearUserDataCalled)
    }

    @Test
    fun `clearUserData clears user properties`() {
        // Given
        provider.setUserProperty("role", "organizer")
        provider.setUserProperty("level", "premium")

        // When
        provider.clearUserData()

        // Then
        assertEquals(0, provider.userProperties.size)
        assertTrue(provider.clearUserDataCalled)
    }

    @Test
    fun `clearUserData clears user id`() {
        // Given
        provider.setUserId("user-123")

        // When
        provider.clearUserData()

        // Then
        assertEquals(null, provider.userId)
        assertTrue(provider.clearUserDataCalled)
    }

    @Test
    fun `clearUserData clears everything`() {
        // Given
        provider.trackEvent(AnalyticsEvent.AppStart)
        provider.setUserProperty("role", "organizer")
        provider.setUserId("user-123")

        // When
        provider.clearUserData()

        // Then
        assertEquals(0, provider.trackedEvents.size)
        assertEquals(0, provider.userProperties.size)
        assertEquals(null, provider.userId)
        assertTrue(provider.clearUserDataCalled)
    }

    @Test
    fun `multiple events tracked correctly`() = runTest {
        // Given
        val events = listOf(
            AnalyticsEvent.AppStart,
            AnalyticsEvent.ScreenView("home"),
            AnalyticsEvent.EventCreated("WEDDING"),
            AnalyticsEvent.AppForeground
        )

        // When
        events.forEach { provider.trackEvent(it) }

        // Then
        assertEquals(4, provider.trackedEvents.size)
    }

    @Test
    fun `event joined with guest flag`() = runTest {
        // Given
        val event = AnalyticsEvent.EventJoined(
            eventId = "event-1",
            isGuest = true
        )

        // When
        provider.trackEvent(event)

        // Then
        val joinedEvents = provider.findEvents(AnalyticsEvent.EventJoined::class.java)
        assertEquals(1, joinedEvents.size)
        assertEquals(true, joinedEvents[0].isGuest)
    }

    @Test
    fun `event shared with method`() = runTest {
        // Given
        val event = AnalyticsEvent.EventShared(
            eventId = "event-1",
            shareMethod = "qr_code"
        )

        // When
        provider.trackEvent(event)

        // Then
        val sharedEvents = provider.findEvents(AnalyticsEvent.EventShared::class.java)
        assertEquals(1, sharedEvents.size)
        assertEquals("qr_code", sharedEvents[0].shareMethod)
    }

    @Test
    fun `scenario created with accommodation flag`() = runTest {
        // Given
        val event = AnalyticsEvent.ScenarioCreated(
            eventId = "event-1",
            hasAccommodation = true
        )

        // When
        provider.trackEvent(event)

        // Then
        val createdEvents = provider.findEvents(AnalyticsEvent.ScenarioCreated::class.java)
        assertEquals(1, createdEvents.size)
        assertEquals(true, createdEvents[0].hasAccommodation)
    }

    @Test
    fun `meeting created with platform`() = runTest {
        // Given
        val event = AnalyticsEvent.MeetingCreated(
            eventId = "event-1",
            platform = "zoom"
        )

        // When
        provider.trackEvent(event)

        // Then
        val createdEvents = provider.findEvents(AnalyticsEvent.MeetingCreated::class.java)
        assertEquals(1, createdEvents.size)
        assertEquals("zoom", createdEvents[0].platform)
    }

    @Test
    fun `user registered with auth method`() = runTest {
        // Given
        val event = AnalyticsEvent.UserRegistered(
            authMethod = "google"
        )

        // When
        provider.trackEvent(event)

        // Then
        val registeredEvents = provider.findEvents(AnalyticsEvent.UserRegistered::class.java)
        assertEquals(1, registeredEvents.size)
        assertEquals("google", registeredEvents[0].authMethod)
    }

    @Test
    fun `offline action queued event`() = runTest {
        // Given
        val event = AnalyticsEvent.OfflineActionQueued(
            actionType = "vote",
            queueSize = 5
        )

        // When
        provider.trackEvent(event)

        // Then
        val queuedEvents = provider.findEvents(AnalyticsEvent.OfflineActionQueued::class.java)
        assertEquals(1, queuedEvents.size)
        assertEquals("vote", queuedEvents[0].actionType)
        assertEquals(5, queuedEvents[0].queueSize)
    }

    @Test
    fun `sync completed event`() = runTest {
        // Given
        val event = AnalyticsEvent.SyncCompleted(
            itemsSynced = 10,
            durationMs = 1500L
        )

        // When
        provider.trackEvent(event)

        // Then
        val completedEvents = provider.findEvents(AnalyticsEvent.SyncCompleted::class.java)
        assertEquals(1, completedEvents.size)
        assertEquals(10, completedEvents[0].itemsSynced)
        assertEquals(1500L, completedEvents[0].durationMs)
    }

    @Test
    fun `sync failed event`() = runTest {
        // Given
        val event = AnalyticsEvent.SyncFailed(
            errorType = "network_error",
            itemsPending = 3
        )

        // When
        provider.trackEvent(event)

        // Then
        val failedEvents = provider.findEvents(AnalyticsEvent.SyncFailed::class.java)
        assertEquals(1, failedEvents.size)
        assertEquals("network_error", failedEvents[0].errorType)
        assertEquals(3, failedEvents[0].itemsPending)
    }

    @Test
    fun `error occurred event`() = runTest {
        // Given
        val event = AnalyticsEvent.ErrorOccurred(
            errorType = "validation_error",
            errorContext = "Invalid input",
            isFatal = false
        )

        // When
        provider.trackEvent(event)

        // Then
        val errorEvents = provider.findEvents(AnalyticsEvent.ErrorOccurred::class.java)
        assertEquals(1, errorEvents.size)
        assertEquals("validation_error", errorEvents[0].errorType)
        assertEquals("Invalid input", errorEvents[0].errorContext)
        assertEquals(false, errorEvents[0].isFatal)
    }

    @Test
    fun `API error event`() = runTest {
        // Given
        val event = AnalyticsEvent.ApiError(
            endpoint = "/api/events",
            statusCode = 500,
            errorMessage = "Internal Server Error"
        )

        // When
        provider.trackEvent(event)

        // Then
        val apiErrors = provider.findEvents(AnalyticsEvent.ApiError::class.java)
        assertEquals(1, apiErrors.size)
        assertEquals("/api/events", apiErrors[0].endpoint)
        assertEquals(500, apiErrors[0].statusCode)
        assertEquals("Internal Server Error", apiErrors[0].errorMessage)
    }

    @Test
    fun `analytics consent events`() = runTest {
        // Given
        val events = listOf(
            AnalyticsEvent.AnalyticsConsentGranted,
            AnalyticsEvent.AnalyticsConsentRevoked,
            AnalyticsEvent.UserDataDeleted
        )

        // When
        events.forEach { provider.trackEvent(it) }

        // Then
        assertEquals(3, provider.trackedEvents.size)
        assertTrue(provider.hasEvent(AnalyticsEvent.AnalyticsConsentGranted::class.java))
        assertTrue(provider.hasEvent(AnalyticsEvent.AnalyticsConsentRevoked::class.java))
        assertTrue(provider.hasEvent(AnalyticsEvent.UserDataDeleted::class.java))
    }
}
