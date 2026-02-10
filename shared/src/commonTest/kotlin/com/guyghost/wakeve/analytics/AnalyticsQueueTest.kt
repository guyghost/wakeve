package com.guyghost.wakeve.analytics

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for AnalyticsQueue.
 *
 * Tests cover:
 * - Enqueueing events
 * - Getting pending events
 * - Marking events as synced
 * - Marking events as failed
 * - Retry logic
 * - Clearing queue
 */
class AnalyticsQueueTest {

    private lateinit var queue: AnalyticsQueue

    @BeforeTest
    fun setup() {
        queue = AnalyticsQueue()
    }

    @AfterTest
    fun tearDown() = runTest {
        queue.clear()
    }

    @Test
    fun `enqueue adds event to queue`() = runTest {
        // Given
        val event = AnalyticsEvent.AppStart
        val properties = mapOf("test" to "value")

        // When
        queue.enqueue(event, properties)

        // Then
        assertEquals(1, queue.size)
    }

    @Test
    fun `enqueue stores event correctly`() = runTest {
        // Given
        val event = AnalyticsEvent.EventCreated("BIRTHDAY")
        val properties = mapOf("test" to "value")

        // When
        queue.enqueue(event, properties)

        // Then
        val pending = queue.getPendingEvents()
        assertEquals(1, pending.size)
        assertEquals("event_created", pending[0].eventName)
        assertEquals("value", pending[0].properties["test"])
    }

    @Test
    fun `enqueue multiple events`() = runTest {
        // Given
        val events = listOf(
            AnalyticsEvent.AppStart to emptyMap<String, Any?>(),
            AnalyticsEvent.ScreenView("home") to mapOf("screen" to "home"),
            AnalyticsEvent.EventCreated("WEDDING") to emptyMap()
        )

        // When
        events.forEach { (event, props) ->
            queue.enqueue(event, props)
        }

        // Then
        assertEquals(3, queue.size)
    }

    @Test
    fun `getPendingEvents returns all events`() = runTest {
        // Given
        queue.enqueue(AnalyticsEvent.AppStart, emptyMap())
        queue.enqueue(AnalyticsEvent.ScreenView("home"), emptyMap())

        // When
        val pending = queue.getPendingEvents()

        // Then
        assertEquals(2, pending.size)
    }

    @Test
    fun `getPendingEvents filters events exceeding max retries`() = runTest {
        // Given
        queue.enqueue(AnalyticsEvent.AppStart, emptyMap())
        val eventId = queue.getPendingEvents()[0].id

        // When - fail 3 times (max retries)
        repeat(3) {
            queue.markAsFailed(listOf(eventId))
        }

        // Then - event should be removed
        val pending = queue.getPendingEvents()
        assertEquals(0, pending.size)
    }

    @Test
    fun `markAsSynced removes events from queue`() = runTest {
        // Given
        queue.enqueue(AnalyticsEvent.AppStart, emptyMap())
        queue.enqueue(AnalyticsEvent.ScreenView("home"), emptyMap())
        val eventIds = queue.getPendingEvents().map { it.id }

        // When
        queue.markAsSynced(eventIds)

        // Then
        assertEquals(0, queue.size)
    }

    @Test
    fun `markAsSynced removes only specified events`() = runTest {
        // Given
        queue.enqueue(AnalyticsEvent.AppStart, emptyMap())
        queue.enqueue(AnalyticsEvent.ScreenView("home"), emptyMap())
        val allEvents = queue.getPendingEvents()
        val firstEventId = allEvents[0].id

        // When - mark only first event as synced
        queue.markAsSynced(listOf(firstEventId))

        // Then - second event should remain
        assertEquals(1, queue.size)
        val remaining = queue.getPendingEvents()
        assertEquals("screen_view", remaining[0].eventName)
    }

    @Test
    fun `markAsFailed increments retry count`() = runTest {
        // Given
        queue.enqueue(AnalyticsEvent.AppStart, emptyMap())
        val eventId = queue.getPendingEvents()[0].id

        // When
        queue.markAsFailed(listOf(eventId))

        // Then
        val pending = queue.getPendingEvents()
        assertEquals(1, pending.size)
        assertEquals(1, pending[0].retryCount)
    }

    @Test
    fun `markAsFailed increments retry count multiple times`() = runTest {
        // Given
        queue.enqueue(AnalyticsEvent.AppStart, emptyMap())
        val eventId = queue.getPendingEvents()[0].id

        // When - fail twice
        queue.markAsFailed(listOf(eventId))
        queue.markAsFailed(listOf(eventId))

        // Then
        val pending = queue.getPendingEvents()
        assertEquals(2, pending[0].retryCount)
    }

    @Test
    fun `markAsFailed removes events after max retries`() = runTest {
        // Given
        queue.enqueue(AnalyticsEvent.AppStart, emptyMap())
        val eventId = queue.getPendingEvents()[0].id

        // When - fail 3 times
        repeat(3) {
            queue.markAsFailed(listOf(eventId))
        }

        // Then - event should be removed
        val pending = queue.getPendingEvents()
        assertEquals(0, pending.size)
        assertEquals(0, queue.size)
    }

    @Test
    fun `clear removes all events from queue`() = runTest {
        // Given
        repeat(5) {
            queue.enqueue(AnalyticsEvent.AppStart, emptyMap())
        }

        // When
        queue.clear()

        // Then
        assertEquals(0, queue.size)
        val pending = queue.getPendingEvents()
        assertEquals(0, pending.size)
    }

    @Test
    fun `event properties are converted to strings`() = runTest {
        // Given
        val properties = mapOf(
            "string" to "value",
            "int" to 42,
            "long" to 123456789L,
            "double" to 3.14,
            "boolean" to true,
            "null" to null
        )
        val event = AnalyticsEvent.ScreenView("test")

        // When
        queue.enqueue(event, properties)

        // Then
        val pending = queue.getPendingEvents()
        val queuedProps = pending[0].properties
        assertEquals("value", queuedProps["string"])
        assertEquals("42", queuedProps["int"])
        assertEquals("123456789", queuedProps["long"])
        assertEquals("3.14", queuedProps["double"])
        assertEquals("true", queuedProps["boolean"])
        assertEquals("", queuedProps["null"])
    }

    @Test
    fun `event IDs are unique`() = runTest {
        // Given
        val events = List(10) { AnalyticsEvent.AppStart }

        // When
        events.forEach { queue.enqueue(it, emptyMap()) }

        // Then
        val pending = queue.getPendingEvents()
        val ids = pending.map { it.id }
        assertEquals(10, ids.size)
        assertEquals(10, ids.toSet().size) // All IDs should be unique
    }

    @Test
    fun `retry count starts at 0`() = runTest {
        // Given
        queue.enqueue(AnalyticsEvent.AppStart, emptyMap())

        // When
        val pending = queue.getPendingEvents()

        // Then
        assertEquals(0, pending[0].retryCount)
    }

    @Test
    fun `markAsFailed only affects specified events`() = runTest {
        // Given
        queue.enqueue(AnalyticsEvent.AppStart, emptyMap())
        queue.enqueue(AnalyticsEvent.ScreenView("home"), emptyMap())
        val allEvents = queue.getPendingEvents()
        val firstEventId = allEvents[0].id

        // When - fail only first event
        queue.markAsFailed(listOf(firstEventId))

        // Then - first event has retry count 1, second has 0
        val pending = queue.getPendingEvents()
        val firstEvent = pending.find { it.id == firstEventId }
        val secondEvent = pending.find { it.id != firstEventId }

        assertEquals(1, firstEvent?.retryCount)
        assertEquals(0, secondEvent?.retryCount)
    }
}
