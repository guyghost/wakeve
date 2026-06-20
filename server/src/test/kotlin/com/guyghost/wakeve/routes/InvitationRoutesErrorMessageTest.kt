package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class InvitationRoutesErrorMessageTest {
    @Test
    fun invitationFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            createInvitationValidationFailureMessage(),
            createInvitationFailureMessage(),
            invitationResolveFailureMessage(),
            invitationAcceptFailureMessage(),
            invitationAcceptUnexpectedFailureMessage()
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
            "invite_",
            "event_",
            "part_"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
