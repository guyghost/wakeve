package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Status of a chat message delivery.
 */
@Serializable
enum class MessageStatus {
    SENT,       // Message sent to server
    DELIVERED,  // Message delivered to recipient
    FAILED,     // Message failed to send
    READ        // Message read by recipient
}

/**
 * Type of chat (group event chat, direct message, thread).
 */
@Serializable
enum class ChatType {
    EVENT,      // Group chat for an event
    DIRECT,     // Direct message between two users
    THREAD      // Thread within a message
}

/**
 * Typing indicator status.
 */
@Serializable
enum class TypingStatus {
    TYPING,     // User is currently typing
    STOPPED,    // User stopped typing
    IDLE        // User is idle
}

/**
 * Represents a chat message.
 *
 * @property id Unique message identifier
 * @property eventId Event this message belongs to
 * @property senderId User who sent the message
 * @property senderName Name of the sender (denormalized for display)
 * @property senderAvatarUrl Avatar URL of the sender (denormalized)
 * @property content Message text content
 * @property section Section of the event (nullable for general chat)
 * @property sectionItemId Optional ID of specific item within section
 * @property parentMessageId ID of parent message (null for top-level messages)
 * @property timestamp Message timestamp (ISO 8601 UTC)
 * @property status Delivery status
 * @property isOffline Whether message was created while offline
 * @property reactions List of reactions to this message
 * @property readBy List of user IDs who have read this message
 * @property isEdited Whether the message has been edited
 */
@Serializable
data class ChatMessage(
    val id: String,
    val eventId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val content: String,
    val section: CommentSection? = null,
    val sectionItemId: String? = null,
    val parentMessageId: String? = null,
    val timestamp: String,
    val status: MessageStatus = MessageStatus.SENT,
    val isOffline: Boolean = false,
    val reactions: List<Reaction> = emptyList(),
    val readBy: List<String> = emptyList(),
    val isEdited: Boolean = false
)

/**
 * Represents a reaction to a message.
 *
 * @property userId User who added the reaction
 * @property emoji Emoji character
 * @property timestamp Reaction timestamp (ISO 8601 UTC)
 */
@Serializable
data class Reaction(
    val userId: String,
    val emoji: String,
    val timestamp: String
)

/**
 * Typing indicator for real-time display.
 *
 * @property userId User who is typing
 * @property chatId Chat identifier (usually eventId)
 * @property chatType Type of chat
 * @property typingStatus Current typing status
 * @property lastSeenTyping Last time typing was seen
 * @property lastActivity Last activity timestamp
 */
@Serializable
data class TypingIndicator(
    val userId: String,
    val chatId: String,
    val chatType: ChatType = ChatType.EVENT,
    val typingStatus: TypingStatus = TypingStatus.TYPING,
    val lastSeenTyping: String,
    val lastActivity: String
)

/**
 * Request to create a chat message.
 *
 * @property content Message text content
 * @property section Section of the event
 * @property parentMessageId ID of parent message (for replies)
 */
@Serializable
data class CreateMessageRequest(
    val content: String,
    val section: CommentSection? = null,
    val parentMessageId: String? = null
)

/**
 * Request to add a reaction to a message.
 *
 * @property emoji Emoji character to add
 */
@Serializable
data class AddReactionRequest(
    val emoji: String
)

/**
 * Response for paginated messages.
 *
 * @property messages List of messages
 * @property totalCount Total number of messages
 * @property hasMore Whether there are more messages
 */
@Serializable
data class MessagesResponse(
    val messages: List<ChatMessage>,
    val totalCount: Int,
    val hasMore: Boolean
)

/**
 * Response for typing indicators.
 *
 * @property typingUsers List of users currently typing
 */
@Serializable
data class TypingUsersResponse(
    val typingUsers: List<TypingIndicator>
)
