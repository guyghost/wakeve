package com.guyghost.wakeve

import com.guyghost.wakeve.models.*

class DefaultNotificationService : NotificationService {

    // Mock storage - in real implementation, this would be database
    private val pushTokens = mutableMapOf<String, PushToken>()
    private val notifications = mutableMapOf<String, NotificationMessage>()

    override suspend fun sendNotification(message: NotificationMessage): Result<Unit> {
        // Platform-specific implementation would send to FCM/APNs
        // For now, just mark as sent
        val sentMessage = message.copy(sentAt = "2025-11-20T10:00:00Z")
        notifications[message.id] = sentMessage
        return Result.success(Unit)
    }

    override suspend fun registerPushToken(token: PushToken): Result<Unit> {
        pushTokens["${token.userId}-${token.deviceId}"] = token
        return Result.success(Unit)
    }

    override suspend fun unregisterPushToken(userId: String, deviceId: String): Result<Unit> {
        pushTokens.remove("$userId-$deviceId")
        return Result.success(Unit)
    }

    override suspend fun getUnreadNotifications(userId: String): List<NotificationMessage> {
        return notifications.values.filter { it.userId == userId && it.readAt == null }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        val notification = notifications[notificationId]
        if (notification != null) {
            notifications[notificationId] = notification.copy(readAt = "2025-11-20T10:00:00Z")
        }
        return Result.success(Unit)
    }
}