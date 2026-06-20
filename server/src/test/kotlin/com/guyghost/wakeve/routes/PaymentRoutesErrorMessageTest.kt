package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertFalse

class PaymentRoutesErrorMessageTest {
    @Test
    fun paymentFailureMessagesUseStableSafeCopy() {
        val message = paymentPotCreateValidationFailureMessage()

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
            "TRICOUNT",
            "providerId",
            "event_"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
