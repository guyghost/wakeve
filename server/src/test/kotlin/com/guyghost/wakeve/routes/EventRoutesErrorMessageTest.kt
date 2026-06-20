package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EventRoutesErrorMessageTest {
    @Test
    fun eventFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            eventListFailureMessage(),
            eventDetailFailureMessage(),
            eventCreateFailureMessage(),
            eventSearchFailureMessage(),
            trendingEventsFailureMessage(),
            nearbyEventsFailureMessage(),
            recommendedEventsFailureMessage(),
            eventStatusUpdateFailureMessage()
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
            "event_",
            "organizer-"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
