package com.guyghost.wakeve.notification

import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.NotificationType
import com.guyghost.wakeve.models.PushToken
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class DefaultNotificationServiceContractTest {
    @Test
    fun defaultNotificationServiceFailsInsteadOfMarkingMessagesAsSent() = runTest {
        val service = DefaultNotificationService()

        val result = service.sendNotification(
            NotificationMessage(
                id = "notification-1",
                userId = "user-1",
                type = NotificationType.EVENT_UPDATE,
                title = "Update",
                body = "Event changed"
            )
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "not configured")
        assertTrue(service.getUnreadNotifications("user-1").isEmpty())
    }

    @Test
    fun defaultNotificationServiceFailsPushTokenMutations() = runTest {
        val service = DefaultNotificationService()

        val register = service.registerPushToken(
            PushToken(
                userId = "user-1",
                token = "token",
                platform = "android",
                deviceId = "device-1",
                registeredAt = "2026-06-20T00:00:00Z"
            )
        )
        val unregister = service.unregisterPushToken("user-1", "device-1")
        val markAsRead = service.markAsRead("notification-1")

        assertTrue(register.isFailure)
        assertTrue(unregister.isFailure)
        assertTrue(markAsRead.isFailure)
    }
}
