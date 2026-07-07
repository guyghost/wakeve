package com.guyghost.wakeve.notification

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationScheduleValidationTest {
    private val now = Instant.parse("2026-06-20T10:00:00Z")

    @Test
    fun `future scheduled time returns positive delay`() {
        val result = futureScheduleDelayMillis(
            targetTime = Instant.parse("2026-06-20T10:00:30Z"),
            now = now
        )

        assertTrue(result.isSuccess)
        assertEquals(30_000L, result.getOrThrow())
    }

    @Test
    fun `current scheduled time fails instead of reporting scheduled`() {
        val result = futureScheduleDelayMillis(
            targetTime = now,
            now = now
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Notification scheduled time must be in the future",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `past scheduled time fails instead of reporting scheduled`() {
        val result = futureScheduleDelayMillis(
            targetTime = Instant.parse("2026-06-20T09:59:59Z"),
            now = now
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Notification scheduled time must be in the future",
            result.exceptionOrNull()?.message
        )
    }
}
