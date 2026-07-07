package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NotificationRoutesErrorMessageTest {
    @Test
    fun notificationFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            pushTokenValidationFailureMessage(),
            notificationPlatformValidationFailureMessage(),
            notificationTokenRegisterFailureMessage(),
            notificationTokenUnregisterFailureMessage(),
            notificationSendForbiddenMessage(),
            notificationSendFailureMessage(),
            notificationHistoryFailureMessage(),
            unreadNotificationsFailureMessage(),
            notificationMarkReadFailureMessage(),
            notificationMarkAllReadFailureMessage(),
            notificationDeleteFailureMessage(),
            notificationPreferencesReadFailureMessage(),
            notificationPreferencesForbiddenMessage(),
            notificationPreferencesUpdateFailureMessage()
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
            "fcm-",
            "apns-",
            "notification-",
            "user-"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
