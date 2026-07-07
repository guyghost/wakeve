package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EquipmentRoutesErrorMessageTest {
    @Test
    fun equipmentFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            equipmentListFailureMessage(),
            equipmentCategoryListFailureMessage(),
            equipmentStatusListFailureMessage(),
            participantEquipmentListFailureMessage(),
            equipmentStatisticsFailureMessage(),
            equipmentCreateFailureMessage(),
            equipmentAutoGenerateFailureMessage(),
            equipmentUpdateFailureMessage(),
            equipmentAssignFailureMessage(),
            equipmentStatusUpdateFailureMessage(),
            equipmentDeleteFailureMessage()
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
            "equipment-",
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
