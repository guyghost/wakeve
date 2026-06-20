package com.guyghost.wakeve.notification

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertIs

class NotificationSchedulerJvmContractTest {

    private val scheduler = NotificationScheduler.getInstance()
    private val futureTime = Instant.parse("2026-06-21T10:00:00Z")

    @Test
    fun `scheduleEventReminder fails instead of reporting a no-op reminder`() = runTest {
        val result = scheduler.scheduleEventReminder(
            eventId = "event-1",
            title = "Event reminder",
            body = "Starts soon",
            scheduledTime = futureTime
        )

        assertUnavailable(result)
    }

    @Test
    fun `scheduleEventReminderWithId fails instead of reporting a no-op reminder`() = runTest {
        val result = scheduler.scheduleEventReminderWithId(
            notificationId = "event-1-recurring-0",
            eventId = "event-1",
            title = "Event reminder",
            body = "Starts soon",
            scheduledTime = futureTime
        )

        assertUnavailableWithId(result)
    }

    @Test
    fun `schedulePollDeadlineReminder fails instead of reporting a no-op reminder`() = runTest {
        val result = scheduler.schedulePollDeadlineReminder(
            pollId = "poll-1",
            eventId = "event-1",
            title = "Vote reminder",
            body = "Vote soon",
            deadlineTime = futureTime
        )

        assertUnavailable(result)
    }

    private fun assertUnavailable(result: Result<Unit>) {
        assertFalse(result.isSuccess)
        val error = assertIs<IllegalStateException>(result.exceptionOrNull())
        assertContains(error.message.orEmpty(), "not available on JVM")
    }

    private fun assertUnavailableWithId(result: Result<String>) {
        assertFalse(result.isSuccess)
        val error = assertIs<IllegalStateException>(result.exceptionOrNull())
        assertContains(error.message.orEmpty(), "not available on JVM")
    }
}
