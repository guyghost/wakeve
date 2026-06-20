package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CalendarRoutesErrorMessageTest {
    @Test
    fun calendarFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            calendarIcsGenerateFailureMessage(),
            calendarIcsDownloadFailureMessage(),
            nativeCalendarAddFailureMessage(),
            nativeCalendarUpdateFailureMessage(),
            nativeCalendarDeleteFailureMessage()
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
            "calendar-event",
            "event_",
            "participant-"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
