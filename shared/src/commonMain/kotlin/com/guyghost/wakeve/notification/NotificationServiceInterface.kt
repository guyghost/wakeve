package com.guyghost.wakeve.notification

import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.PushToken

interface NotificationServiceInterface {
    suspend fun sendNotification(message: NotificationMessage): Result<Unit>
    suspend fun registerPushToken(token: PushToken): Result<Unit>
    suspend fun unregisterPushToken(userId: String, deviceId: String): Result<Unit>
    suspend fun getUnreadNotifications(userId: String): List<NotificationMessage>
    suspend fun markAsRead(notificationId: String): Result<Unit>
}

class DefaultNotificationService : NotificationServiceInterface {
    override suspend fun sendNotification(message: NotificationMessage): Result<Unit> {
        return Result.failure(IllegalStateException("Notification delivery service is not configured"))
    }

    override suspend fun registerPushToken(token: PushToken): Result<Unit> {
        return Result.failure(IllegalStateException("Push token registration service is not configured"))
    }

    override suspend fun unregisterPushToken(userId: String, deviceId: String): Result<Unit> {
        return Result.failure(IllegalStateException("Push token registration service is not configured"))
    }

    override suspend fun getUnreadNotifications(userId: String): List<NotificationMessage> {
        return emptyList()
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return Result.failure(IllegalStateException("Notification storage service is not configured"))
    }
}
