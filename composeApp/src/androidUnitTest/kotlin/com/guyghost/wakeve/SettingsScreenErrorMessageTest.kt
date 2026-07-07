package com.guyghost.wakeve

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SettingsScreenErrorMessageTest {
    @Test
    fun sessionFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            activeSessionsLoadFailureMessage(),
            otherSessionsRevokeFailureMessage(),
            sessionRevokeFailureMessage()
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
            "session-"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
