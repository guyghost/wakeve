package com.guyghost.wakeve.chat

import kotlinx.serialization.Serializable

/**
 * Represents an image attachment for a chat message.
 *
 * @property uri Content URI of the image
 * @property mimeType MIME type of the image
 * @property width Width in pixels (null if unknown)
 * @property height Height in pixels (null if unknown)
 * @property sizeBytes File size in bytes
 * @property thumbnail Optional thumbnail URL
 */
@Serializable
data class ChatImageAttachment(
    val uri: String,
    val mimeType: String,
    val width: Int? = null,
    val height: Int? = null,
    val sizeBytes: Long,
    val thumbnail: String? = null
) {
    /**
     * Get human-readable file size.
     */
    val formattedSize: String
        get() = formatFileSize(sizeBytes)
    
    /**
     * Get image dimensions as formatted string.
     */
    val dimensions: String?
        get() = if (width != null && height != null) "${width}×${height}" else null
    
    companion object {
        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> {
                    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
                    val rounded = (gb * 100).toLong() / 100.0
                    "$rounded GB"
                }
            }
        }
    }
}

/**
 * Represents a chat message in the real-time messaging system.
 *
 * @property id Unique message identifier
 * @property eventId Associated event for context
 * @property senderId User who sent the message
 * @property senderName Display name of the sender
 * @property content Text content of the message
 * @property section Optional category for organizing messages (TRANSPORT, FOOD, etc.)
 * @property parentId Reference to parent message for threaded replies
 * @property timestamp ISO 8601 timestamp
 * @property reactions List of emoji reactions on this message
 * @property status Delivery status (SENT, DELIVERED, FAILED, READ)
 * @property readBy List of user IDs who have read this message
 * @property isOffline Flag for messages queued while offline
 * @property imageAttachment Optional image attachment
 */
@Serializable
data class ChatMessage(
    val id: String,
    val eventId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val section: CommentSection? = null,
    val parentId: String? = null,
    val timestamp: String,
    val reactions: List<Reaction> = emptyList(),
    val status: MessageStatus = MessageStatus.SENT,
    val readBy: List<String> = emptyList(),
    val isOffline: Boolean = false,
    val imageAttachment: ChatImageAttachment? = null
) {
    /**
     * Check if this message is from the current user
     */
    fun isFromUser(userId: String): Boolean = senderId == userId
    
    /**
     * Get reactions grouped by emoji for display
     */
    fun groupedReactions(): Map<String, List<Reaction>> = reactions.groupBy { it.emoji }
    
    /**
     * Check if user has reacted with specific emoji
     */
    fun hasUserReacted(userId: String, emoji: String): Boolean =
        reactions.any { it.userId == userId && it.emoji == emoji }
    
    /**
     * Check if this message contains an image attachment
     */
    val isImageMessage: Boolean
        get() = imageAttachment != null
    
    /**
     * Check if this message has text content only
     */
    val isTextMessage: Boolean
        get() = content.isNotBlank() && imageAttachment == null
    
    /**
     * Check if this is an empty message (no content and no image)
     */
    val isEmpty: Boolean
        get() = content.isBlank() && imageAttachment == null
}

/**
 * Represents an emoji reaction on a message.
 *
 * @property userId User who added the reaction
 * @property emoji The emoji character (❤️, 👍, etc.)
 * @property timestamp ISO 8601 when reaction was added
 */
@Serializable
data class Reaction(
    val userId: String,
    val emoji: String,
    val timestamp: String
)

/**
 * Message delivery and read status.
 */
@Serializable
enum class MessageStatus {
    /** Message created, waiting to be sent */
    SENT,
    
    /** Successfully delivered to all participants */
    DELIVERED,
    
    /** Delivery failed, needs retry */
    FAILED,
    
    /** At least one participant has read the message */
    READ
}

/**
 * Categories for organizing chat messages by topic.
 */
@Serializable
enum class CommentSection {
    /** Transport-related discussions */
    TRANSPORT,
    
    /** Accommodation and lodging discussions */
    ACCOMMODATION,
    
    /** Food and meal planning */
    FOOD,
    
    /** Equipment and supplies */
    EQUIPMENT,
    
    /** Activity planning and suggestions */
    ACTIVITIES,
    
    /** General event discussions */
    GENERAL
}

/**
 * Typing indicator data for real-time presence.
 *
 * @property userId User who is typing
 * @property userName Display name of typing user
 * @property chatId Event/chat room identifier
 * @property lastSeenTyping ISO 8601 of last typing activity
 */
@Serializable
data class TypingIndicator(
    val userId: String,
    val userName: String,
    val chatId: String,
    val lastSeenTyping: String
)

/**
 * User presence information for the chat.
 *
 * @property userId User identifier
 * @property userName Display name
 * @property isOnline Whether user is currently connected
 * @property lastSeen Last online timestamp
 */
@Serializable
data class ChatParticipant(
    val userId: String,
    val userName: String,
    val isOnline: Boolean = false,
    val lastSeen: String? = null
)
