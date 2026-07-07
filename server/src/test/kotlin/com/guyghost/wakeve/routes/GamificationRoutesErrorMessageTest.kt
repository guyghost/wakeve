package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GamificationRoutesErrorMessageTest {
    @Test
    fun gamificationFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            leaderboardFetchFailureMessage(),
            userBadgesFetchFailureMessage(),
            userPointsFetchFailureMessage(),
            pointsAwardFailureMessage()
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
            "user_",
            "badge_"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
