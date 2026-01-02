package com.guyghost.wakeve.chat

/**
 * WebSocket message types for real-time communication.
 */
@kotlinx.serialization.Serializable
enum class WebSocketMessageType {
    /** New chat message */
    MESSAGE,
    
    /** User is typing indicator */
    TYPING,
    
    /** User stopped typing */
    STOPPED_TYPING,
    
    /** Emoji reaction added/removed */
    REACTION,
    
    /** Message read receipt */
    READ_RECEIPT,
    
    /** User joined/left chat */
    PRESENCE,
    
    /** Connection established */
    CONNECT,
    
    /** Connection closed */
    DISCONNECT,
    
    /** Error occurred */
    ERROR
}

/**
 * WebSocket message envelope for real-time events.
 *
 * @property type The type of message
 * @property data Serialized payload based on message type
 */
@kotlinx.serialization.Serializable
data class WebSocketMessage(
    val type: WebSocketMessageType,
    val data: String // JSON serialized payload
)

/**
 * Payload for MESSAGE type WebSocket messages.
 */
@kotlinx.serialization.Serializable
data class MessagePayload(
    val messageId: String,
    val eventId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val section: String?, // null or section name
    val parentId: String?,
    val timestamp: String
)

/**
 * Payload for TYPING/STOPPED_TYPING type WebSocket messages.
 */
@kotlinx.serialization.Serializable
data class TypingPayload(
    val userId: String,
    val userName: String,
    val chatId: String,
    val timestamp: String
)

/**
 * Payload for REACTION type WebSocket messages.
 */
@kotlinx.serialization.Serializable
data class ReactionPayload(
    val messageId: String,
    val userId: String,
    val emoji: String,
    val action: String // "add" or "remove"
)

/**
 * Payload for READ_RECEIPT type WebSocket messages.
 */
@kotlinx.serialization.Serializable
data class ReadReceiptPayload(
    val messageId: String,
    val userId: String,
    val timestamp: String
)

/**
 * Payload for PRESENCE type WebSocket messages.
 */
@kotlinx.serialization.Serializable
data class PresencePayload(
    val userId: String,
    val userName: String,
    val chatId: String,
    val isOnline: Boolean,
    val lastSeen: String?
)
