package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Types de messages supportés par le chat WebSocket.
 */
@Serializable
enum class ChatMessageType {
    MESSAGE,
    TYPING,
    REACTION,
    READ_RECEIPT
}

/**
 * Payload du message WebSocket.
 */
@Serializable
data class ChatWebSocketMessage(
    val type: ChatMessageType,
    val data: MessageData
)

/**
 * Données du message.
 */
@Serializable
data class MessageData(
    val messageId: String? = null,
    val eventId: String,
    val userId: String,
    val userName: String,
    val content: String? = null,
    val reaction: String? = null,
    val targetMessageId: String? = null,
    val timestamp: String = System.currentTimeMillis().toString()
)

/**
 * Réponse du serveur WebSocket.
 */
@Serializable
data class ChatWebSocketResponse(
    val type: ChatMessageType,
    val data: MessageData,
    val success: Boolean = true,
    val errorMessage: String? = null
)
