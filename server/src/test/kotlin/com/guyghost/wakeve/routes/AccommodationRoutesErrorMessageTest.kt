package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AccommodationRoutesErrorMessageTest {
    @Test
    fun accommodationFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            accommodationListFailureMessage(),
            accommodationValidationFailureMessage(),
            accommodationCreateFailureMessage(),
            accommodationDetailFailureMessage(),
            accommodationUpdateFailureMessage(),
            accommodationDeleteFailureMessage(),
            roomListFailureMessage(),
            roomAssignedParticipantsFailureMessage(),
            roomValidationFailureMessage(),
            roomCreateFailureMessage(),
            roomUpdateFailureMessage(),
            roomDeleteFailureMessage(),
            accommodationStatisticsFailureMessage()
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
            "accommodation_",
            "room_",
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
