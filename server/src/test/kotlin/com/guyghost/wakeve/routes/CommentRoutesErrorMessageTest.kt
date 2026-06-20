package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CommentRoutesErrorMessageTest {
    @Test
    fun commentFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            commentAuthorForbiddenMessage(),
            commentCreateFailureMessage(),
            commentListInvalidRequestMessage(),
            commentListFailureMessage(),
            commentDetailFailureMessage(),
            commentUpdateInvalidRequestMessage(),
            commentUpdateFailureMessage(),
            commentDeleteFailureMessage(),
            commentStatisticsFailureMessage(),
            commentTopContributorsFailureMessage(),
            recentCommentsFailureMessage(),
            commentPinFailureMessage(),
            commentUnpinFailureMessage(),
            commentRestoreFailureMessage(),
            commentSectionsFailureMessage()
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
            "comment-",
            "event_",
            "user-"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
