package com.guyghost.wakeve.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RichNotificationManagerLogContractTest {

    @Test
    fun richNotificationShownLogMessage_doesNotIncludeUserVisibleNotificationText() {
        val title = "Secret birthday for Alice"
        val message = "Meet at 42 Private Street"

        val result = richNotificationShownLogMessage(
            notificationId = 42,
            actionCount = 2,
            hasLargeIcon = true,
            hasBigPicture = true,
            hasContentIntent = true
        )

        assertEquals(
            "Rich notification shown: id=42, actions=2, largeIcon=true, bigPicture=true, contentIntent=true",
            result
        )
        assertFalse(result.contains(title))
        assertFalse(result.contains(message))
    }

    @Test
    fun imageLoadFailureLogMessage_doesNotIncludeImageUri() {
        val imageUri = "https://cdn.example.com/private/event-photo.jpg?token=SECRET"

        val result = imageLoadFailureLogMessage()

        assertEquals("Failed to load notification image", result)
        assertFalse(result.contains(imageUri))
        assertFalse(result.contains("SECRET"))
    }

    @Test
    fun notificationFailureLogMessage_doesNotIncludeExceptionMessage() {
        val exceptionMessage = "Permission denied for private channel private-event-123"

        val result = notificationFailureLogMessage("show rich notification")

        assertEquals("Failed to show rich notification", result)
        assertFalse(result.contains(exceptionMessage))
        assertFalse(result.contains("private-event-123"))
    }

    @Test
    fun progressAndInlineLogMessages_keepOnlyOperationalMetadata() {
        assertEquals(
            "Progress notification shown: id=7, indeterminate=true",
            progressNotificationShownLogMessage(notificationId = 7, indeterminate = true)
        )
        assertEquals(
            "Progress notification updated: id=7, progress=50/100, indeterminate=false",
            progressNotificationUpdatedLogMessage(
                notificationId = 7,
                progress = 50,
                max = 100,
                indeterminate = false
            )
        )
        assertEquals(
            "Inline reply notification shown: id=7",
            inlineReplyNotificationShownLogMessage(notificationId = 7)
        )
    }
}
