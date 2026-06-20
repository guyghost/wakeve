package com.guyghost.wakeve.notification

import kotlin.test.Test
import kotlin.test.assertEquals

class EventNotificationTriggerDeliverySummaryTest {
    @Test
    fun summarizeNotificationDeliveryCountsAttemptsSuccessesAndFailures() {
        val summary = summarizeNotificationDelivery(
            listOf(
                Result.success("notification-1"),
                Result.failure(IllegalStateException("No push tokens")),
                Result.success("notification-2"),
                Result.failure(IllegalStateException("FCM delivery failed"))
            )
        )

        assertEquals(4, summary.attempted)
        assertEquals(2, summary.sent)
        assertEquals(2, summary.failed)
    }

    @Test
    fun summarizeNotificationDeliveryHandlesNoRecipients() {
        val summary = summarizeNotificationDelivery(emptyList())

        assertEquals(0, summary.attempted)
        assertEquals(0, summary.sent)
        assertEquals(0, summary.failed)
    }
}
