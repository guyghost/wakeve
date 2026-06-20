package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AnalyticsRoutesErrorMessageTest {
    @Test
    fun analyticsFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            analyticsMetricsFailureMessage(),
            analyticsMauFailureMessage(),
            analyticsDauFailureMessage(),
            analyticsRetentionFailureMessage(),
            analyticsFunnelFailureMessage(),
            analyticsExportFailureMessage()
        )

        assertEquals(messages.size, messages.distinct().size)
        messages.forEach { message ->
            assertFalse(message.isBlank())
            assertDoesNotExposeSensitiveDetails(message)
        }
    }

    private fun assertDoesNotExposeSensitiveDetails(message: String) {
        listOf(
            "secret@example.com",
            "SECRET",
            "SQL constraint",
            "http://internal.local",
            "token=",
            "analytics_",
            "admin-"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
