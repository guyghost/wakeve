package com.guyghost.wakeve

import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.NotificationType
import com.guyghost.wakeve.models.PushToken
import kotlin.test.Test
import kotlin.test.assertTrue

class NotificationServiceTest {

    private val service = DefaultNotificationService()

    private val sampleMessage = NotificationMessage(
        id = "notif-1",
        userId = "user-1",
        type = NotificationType.EVENT_CONFIRMED,
        title = "Event Confirmed",
        body = "Your event 'Team Meeting' has been confirmed for Dec 1, 2025",
        data = mapOf("eventId" to "event-1"),
        sentAt = null
    )

    @Test
    fun sendNotificationReturnsSuccess() {
        // Since it's mock, it should succeed
        val result = service.sendNotification(sampleMessage)
        assertTrue(result.isSuccess)
    }

    @Test
    fun registerPushTokenReturnsSuccess() {
        val token = PushToken(
            userId = "user-1",
            token = "device-token-123",
            platform = "ios",
            deviceId = "device-1",
            registeredAt = "2025-11-20T10:00:00Z"
        )
        val result = service.registerPushToken(token)
        assertTrue(result.isSuccess)
    }

    @Test
    fun getUnreadNotificationsReturnsEmptyListInitially() {
        val notifications = service.getUnreadNotifications("user-1")
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun markAsReadReturnsSuccess() {
        val result = service.markAsRead("notif-1")
        assertTrue(result.isSuccess)
    }
}