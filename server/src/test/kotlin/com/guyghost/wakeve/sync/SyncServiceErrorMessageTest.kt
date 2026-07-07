package com.guyghost.wakeve.sync

import kotlin.test.Test
import kotlin.test.assertFalse

class SyncServiceErrorMessageTest {
    @Test
    fun serverSyncFailureMessageDoesNotExposeSensitiveDetails() {
        val message = serverSyncFailureMessage()

        assertFalse(message.isBlank())
        listOf(
            "SECRET",
            "SQL constraint",
            "token=",
            "jdbc:",
            "internal.local",
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
