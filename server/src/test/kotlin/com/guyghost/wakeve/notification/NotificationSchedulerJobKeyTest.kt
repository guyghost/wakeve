package com.guyghost.wakeve.notification

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationSchedulerJobKeyTest {
    @Test
    fun scheduledJobBelongsToEventMatchesOnlyExactEventIdKeys() {
        val eventId = "event-1"

        assertTrue(scheduledJobBelongsToEvent("deadline-24h-event-1", eventId))
        assertTrue(scheduledJobBelongsToEvent("deadline-1h-event-1", eventId))
        assertTrue(scheduledJobBelongsToEvent("deadline-check-24h-event-1", eventId))
        assertTrue(scheduledJobBelongsToEvent("deadline-check-1h-event-1", eventId))
        assertTrue(scheduledJobBelongsToEvent("event-day-event-1-20623", eventId))

        assertFalse(scheduledJobBelongsToEvent("deadline-24h-event-10", eventId))
        assertFalse(scheduledJobBelongsToEvent("deadline-check-1h-event-10", eventId))
        assertFalse(scheduledJobBelongsToEvent("event-day-event-10-20623", eventId))
        assertFalse(scheduledJobBelongsToEvent("weekly-digest-2938", eventId))
        assertFalse(scheduledJobBelongsToEvent("deadline-24h-event-1-extra", eventId))
        assertFalse(scheduledJobBelongsToEvent("deadline-24h-event-1", ""))
    }
}
