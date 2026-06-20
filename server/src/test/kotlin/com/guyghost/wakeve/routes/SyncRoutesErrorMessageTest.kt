package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertFalse

class SyncRoutesErrorMessageTest {
    @Test
    fun syncFailureMessageUsesStableSafeCopy() {
        val message = syncRequestFailureMessage()

        assertFalse(message.isBlank())
        assertDoesNotExposeSensitiveDetails(message)
    }

    private fun assertDoesNotExposeSensitiveDetails(message: String) {
        listOf(
            "secret@example.com",
            "SECRET",
            "SQL constraint",
            "http://internal.local",
            "token=",
            "sync_",
            "event_"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
