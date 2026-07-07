package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MeetingProxyRoutesErrorMessageTest {
    @Test
    fun meetingProxyFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            zoomMeetingCreateFailureMessage(),
            zoomMeetingCancelFailureMessage(),
            zoomMeetingStatusFailureMessage(),
            googleMeetCreateFailureMessage()
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
            "ZOOM_API_KEY",
            "GOOGLE_MEET_CREDENTIALS",
            "meeting_"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
