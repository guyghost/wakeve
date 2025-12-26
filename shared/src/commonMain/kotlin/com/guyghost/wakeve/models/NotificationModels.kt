package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationType {
    DEADLINE_REMINDER,
    EVENT_UPDATE,
    VOTE_CLOSE_REMINDER,
    EVENT_CONFIRMED,
    PARTICIPANT_JOINED,
    VOTE_SUBMITTED,
    COMMENT_POSTED,        // Nouveau commentaire sur une section
    COMMENT_REPLY           // Réponse à un commentaire
}

@Serializable
data class NotificationMessage(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(), // Additional data like eventId
    val sentAt: String? = null, // ISO 8601, null if not sent yet
    val readAt: String? = null // ISO 8601
)

@Serializable
data class PushToken(
    val userId: String,
    val token: String,
    val platform: String, // "ios" or "android"
    val deviceId: String,
    val registeredAt: String // ISO 8601
)

interface NotificationService {
    suspend fun sendNotification(message: NotificationMessage): Result<Unit>
    suspend fun registerPushToken(token: PushToken): Result<Unit>
    suspend fun unregisterPushToken(userId: String, deviceId: String): Result<Unit>
    suspend fun getUnreadNotifications(userId: String): List<NotificationMessage>
    suspend fun markAsRead(notificationId: String): Result<Unit>
}

@Serializable
data class NotificationRequest(
    val userId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val eventId: String? = null,
    val data: Map<String, String> = emptyMap(),
    // Champs spécifiques pour les commentaires
    val commentId: String? = null,              // ID du commentaire concerné
    val parentCommentId: String? = null,        // Pour les réponses
    val section: String? = null,                // Section concernée (GENERAL, SCENARIO, etc.)
    val sectionItemId: String? = null           // Item ID dans la section
)