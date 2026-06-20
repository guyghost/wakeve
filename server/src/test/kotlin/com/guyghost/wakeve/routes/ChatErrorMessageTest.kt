package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ChatErrorMessageTest {
    @Test
    fun chatFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            chatWebSocketInvalidMessageFailureMessage(),
            chatMessageSendFailureMessage(),
            chatReactionAddFailureMessage(),
            chatReactionRemoveFailureMessage(),
            chatReadReceiptFailureMessage(),
            chatMarkAllReadFailureMessage(),
            chatTypingStatusFailureMessage(),
            chatMessagesFetchFailureMessage(),
            chatMessagesCountFailureMessage(),
            chatThreadMessagesFetchFailureMessage(),
            chatSectionMessagesFetchFailureMessage(),
            chatMessageFetchFailureMessage(),
            chatUnreadCountFailureMessage(),
            chatTypingUsersFetchFailureMessage(),
            chatMessageUpdateFailureMessage(),
            chatMessageDeleteFailureMessage()
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
            "message_",
            "event_",
            "sender-"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
