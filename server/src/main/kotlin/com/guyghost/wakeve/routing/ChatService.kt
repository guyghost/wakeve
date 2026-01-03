package com.guyghost.wakeve.routes

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.AddReactionRequest
import com.guyghost.wakeve.models.ChatMessage
import com.guyghost.wakeve.models.ChatMessageType
import com.guyghost.wakeve.models.ChatType
import com.guyghost.wakeve.models.ChatWebSocketResponse
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.models.CreateMessageRequest
import com.guyghost.wakeve.models.MessageData
import com.guyghost.wakeve.models.MessageStatus
import com.guyghost.wakeve.models.Reaction
import com.guyghost.wakeve.models.TypingIndicator
import com.guyghost.wakeve.models.TypingStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Service for managing real-time chat messages.
 *
 * Provides functionality for:
 * - Sending and receiving messages via WebSocket
 * - Managing message reactions
 * - Tracking read receipts
 * - Handling typing indicators
 * - Persisting messages for offline access
 *
 * This service coordinates with:
 * - SQLDelight database for message persistence
 * - EventChatConnections for WebSocket broadcasting
 */
class ChatService(
    private val database: WakevDb,
    private val eventConnections: EventChatConnections = com.guyghost.wakeve.routes.eventChatConnections
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Sends a new message to an event chat.
     *
     * @param eventId Event to send message to
     * @param userId User sending the message
     * @param userName Name of the sender
     * @param userAvatarUrl Avatar URL of the sender (optional)
     * @param content Message text content
     * @param section Section of the event (nullable for general chat)
     * @param parentMessageId ID of parent message (for replies/threads)
     * @return The created ChatMessage
     */
    suspend fun sendMessage(
        eventId: String,
        userId: String,
        userName: String,
        userAvatarUrl: String? = null,
        content: String,
        section: CommentSection? = null,
        parentMessageId: String? = null
    ): ChatMessage = withContext(Dispatchers.IO) {
        try {
            val messageId = generateMessageId()
            val timestamp = getCurrentTimestamp()
            
            val message = ChatMessage(
                id = messageId,
                eventId = eventId,
                senderId = userId,
                senderName = userName,
                senderAvatarUrl = userAvatarUrl,
                content = content,
                section = section,
                parentMessageId = parentMessageId,
                timestamp = timestamp,
                status = MessageStatus.SENT,
                isOffline = false,
                reactions = emptyList(),
                readBy = emptyList()
            )
            
            // Save to database
            database.chatMessagesQueries.insertMessage(
                id = messageId,
                event_id = eventId,
                sender_id = userId,
                sender_name = userName,
                sender_avatar_url = userAvatarUrl,
                content = content,
                section = section?.name,
                section_item_id = null,
                parent_message_id = parentMessageId,
                timestamp = timestamp,
                status = MessageStatus.SENT.name,
                is_offline = 0,
                reply_count = 0,
                edit_timestamp = null,
                is_edited = 0,
                reactions_json = null,
                read_by_json = null,
                created_at = timestamp,
                updated_at = timestamp
            )
            
            // If this is a reply, increment parent's reply count
            parentMessageId?.let { parentId ->
                database.chatMessagesQueries.incrementReplyCount(
                    timestamp,
                    parentId
                )
            }
            
            // Broadcast to all connected users via WebSocket
            broadcastMessage(eventId, message)
            
            message
        } catch (e: Exception) {
            throw ChatServiceException("Failed to send message: ${e.message}", e)
        }
    }
    
    /**
     * Adds a reaction to a message.
     *
     * @param eventId Event containing the message
     * @param messageId Message to react to
     * @param userId User adding the reaction
     * @param emoji Emoji character to add
     */
    suspend fun addReaction(
        eventId: String,
        messageId: String,
        userId: String,
        emoji: String
    ) = withContext(Dispatchers.IO) {
        try {
            val timestamp = getCurrentTimestamp()
            val reactionId = generateMessageId()
            
            // Add reaction to message_reaction table
            database.chatMessagesQueries.insertReaction(
                id = reactionId,
                message_id = messageId,
                user_id = userId,
                emoji = emoji,
                timestamp = timestamp
            )
            
            // Get updated reactions for the message
            val reactions = database.chatMessagesQueries
                .selectReactionsByMessage(messageId)
                .executeAsList()
                .map { Reaction(it.user_id, it.emoji, it.timestamp) }
            
            // Update reactions_json in chat_message
            val reactionsJson = json.encodeToString(reactions)
            database.chatMessagesQueries.updateMessageReactions(
                reactionsJson,
                timestamp,
                messageId
            )
            
            // Broadcast reaction via WebSocket
            broadcastReaction(eventId, messageId, userId, emoji)
        } catch (e: Exception) {
            throw ChatServiceException("Failed to add reaction: ${e.message}", e)
        }
    }
    
    /**
     * Removes a reaction from a message.
     *
     * @param eventId Event containing the message
     * @param messageId Message to remove reaction from
     * @param userId User removing the reaction
     * @param emoji Emoji character to remove
     */
    suspend fun removeReaction(
        eventId: String,
        messageId: String,
        userId: String,
        emoji: String
    ) = withContext(Dispatchers.IO) {
        try {
            val timestamp = getCurrentTimestamp()
            
            // Remove reaction from message_reaction table
            database.chatMessagesQueries.deleteReaction(
                messageId,
                userId,
                emoji
            )
            
            // Get updated reactions for the message
            val reactions = database.chatMessagesQueries
                .selectReactionsByMessage(messageId)
                .executeAsList()
                .map { Reaction(it.user_id, it.emoji, it.timestamp) }
            
            // Update reactions_json in chat_message
            val reactionsJson = json.encodeToString(reactions)
            database.chatMessagesQueries.updateMessageReactions(
                reactionsJson,
                timestamp,
                messageId
            )
            
            // Broadcast reaction removal via WebSocket
            broadcastReaction(eventId, messageId, userId, emoji)
        } catch (e: Exception) {
            throw ChatServiceException("Failed to remove reaction: ${e.message}", e)
        }
    }
    
    /**
     * Marks a message as read by a user.
     *
     * @param eventId Event containing the message
     * @param messageId Message to mark as read
     * @param userId User who read the message
     */
    suspend fun markAsRead(
        eventId: String,
        messageId: String,
        userId: String
    ) = withContext(Dispatchers.IO) {
        try {
            val timestamp = getCurrentTimestamp()
            val readStatusId = generateMessageId()
            
            // Add read status to message_read_status table
            database.chatMessagesQueries.insertReadStatus(
                id = readStatusId,
                message_id = messageId,
                user_id = userId,
                read_at = timestamp
            )
            
            // Update read_by_json in chat_message
            val message = database.chatMessagesQueries
                .selectMessageById(messageId)
                .executeAsOneOrNull()
            
            message?.let { msg ->
                val currentReadBy = msg.read_by_json?.let {
                    try {
                        json.decodeFromString<List<String>>(it)
                    } catch (e: Exception) {
                        emptyList<String>()
                    }
                }?.toMutableList() ?: mutableListOf()
                
                if (!currentReadBy.contains(userId)) {
                    currentReadBy.add(userId)
                    val readByJson = json.encodeToString(currentReadBy)
                    database.chatMessagesQueries.updateMessageReadBy(
                        readByJson,
                        timestamp,
                        messageId
                    )
                }
            }
            
            // Broadcast read receipt via WebSocket
            broadcastReadReceipt(eventId, messageId, userId)
        } catch (e: Exception) {
            throw ChatServiceException("Failed to mark message as read: ${e.message}", e)
        }
    }
    
    /**
     * Marks all messages in an event as read by a user.
     *
     * @param eventId Event to mark messages as read
     * @param userId User who read the messages
     */
    suspend fun markAllAsRead(
        eventId: String,
        userId: String
    ) = withContext(Dispatchers.IO) {
        try {
            val timestamp = getCurrentTimestamp()
            
            // Get all messages not sent by this user
            val messages = database.chatMessagesQueries
                .selectMessagesByEvent(eventId)
                .executeAsList()
                .filter { it.sender_id != userId }
            
            // Mark each message as read
            messages.forEach { message ->
                val readStatusId = generateMessageId()
                database.chatMessagesQueries.insertReadStatus(
                    id = readStatusId,
                    message_id = message.id,
                    user_id = userId,
                    read_at = timestamp
                )
            }
            
            // Update read_by_json for all messages
            val readByJson = json.encodeToString(listOf(userId))
            database.chatMessagesQueries.markAllMessagesAsReadByUser(
                readByJson,
                timestamp,
                eventId
            )
        } catch (e: Exception) {
            throw ChatServiceException("Failed to mark all messages as read: ${e.message}", e)
        }
    }
    
    /**
     * Updates the typing status of a user.
     *
     * @param eventId Event where user is typing
     * @param userId User whose typing status changed
     * @param userName Name of the user
     * @param isTyping Whether the user is currently typing
     */
    suspend fun setTypingStatus(
        eventId: String,
        userId: String,
        userName: String,
        isTyping: Boolean
    ) = withContext(Dispatchers.IO) {
        try {
            val timestamp = getCurrentTimestamp()
            val status = if (isTyping) TypingStatus.TYPING.name else TypingStatus.IDLE.name
            
            // Save typing indicator to database
            database.typingIndicatorsQueries.insertTypingIndicator(
                user_id = userId,
                chat_id = eventId,
                chat_type = ChatType.EVENT.name,
                last_seen_typing = timestamp,
                typing_status = status,
                last_activity = timestamp
            )
            
            // Broadcast typing indicator via WebSocket
            broadcastTyping(eventId, userId, userName, isTyping)
        } catch (e: Exception) {
            throw ChatServiceException("Failed to update typing status: ${e.message}", e)
        }
    }
    
    /**
     * Gets all messages for an event.
     *
     * @param eventId Event to get messages for
     * @param limit Maximum number of messages
     * @param offset Offset for pagination
     * @return List of messages
     */
    suspend fun getMessages(
        eventId: String,
        limit: Int = 100,
        offset: Int = 0
    ): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val messages = if (limit > 0) {
                database.chatMessagesQueries
                    .selectMessagesByEventPaginated(eventId, limit.toLong(), offset.toLong())
                    .executeAsList()
            } else {
                database.chatMessagesQueries
                    .selectMessagesByEvent(eventId)
                    .executeAsList()
            }
            
            messages.map { row ->
                ChatMessage(
                    id = row.id,
                    eventId = row.event_id,
                    senderId = row.sender_id,
                    senderName = row.sender_name,
                    senderAvatarUrl = row.sender_avatar_url,
                    content = row.content,
                    section = row.section?.let { CommentSection.valueOf(it) },
                    sectionItemId = row.section_item_id,
                    parentMessageId = row.parent_message_id,
                    timestamp = row.timestamp,
                    status = MessageStatus.valueOf(row.status),
                    isOffline = row.is_offline == 1L,
                    reactions = row.reactions_json?.let {
                        try {
                            json.decodeFromString<List<Reaction>>(it)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } ?: emptyList(),
                    readBy = row.read_by_json?.let {
                        try {
                            json.decodeFromString<List<String>>(it)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } ?: emptyList(),
                    isEdited = row.is_edited == 1L
                )
            }
        } catch (e: Exception) {
            throw ChatServiceException("Failed to get messages: ${e.message}", e)
        }
    }
    
    /**
     * Gets messages for a specific thread.
     *
     * @param parentMessageId Parent message ID (thread root)
     * @return List of messages in the thread
     */
    suspend fun getThreadMessages(
        parentMessageId: String
    ): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val messages = database.chatMessagesQueries
                .selectThreadMessages(parentMessageId)
                .executeAsList()
            
            messages.map { row ->
                ChatMessage(
                    id = row.id,
                    eventId = row.event_id,
                    senderId = row.sender_id,
                    senderName = row.sender_name,
                    senderAvatarUrl = row.sender_avatar_url,
                    content = row.content,
                    section = row.section?.let { CommentSection.valueOf(it) },
                    sectionItemId = row.section_item_id,
                    parentMessageId = row.parent_message_id,
                    timestamp = row.timestamp,
                    status = MessageStatus.valueOf(row.status),
                    isOffline = row.is_offline == 1L,
                    reactions = row.reactions_json?.let {
                        try {
                            json.decodeFromString<List<Reaction>>(it)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } ?: emptyList(),
                    readBy = row.read_by_json?.let {
                        try {
                            json.decodeFromString<List<String>>(it)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } ?: emptyList(),
                    isEdited = row.is_edited == 1L
                )
            }
        } catch (e: Exception) {
            throw ChatServiceException("Failed to get thread messages: ${e.message}", e)
        }
    }
    
    /**
     * Gets messages by section.
     *
     * @param eventId Event to get messages for
     * @param section Section to filter by
     * @return List of messages in the section
     */
    suspend fun getMessagesBySection(
        eventId: String,
        section: CommentSection
    ): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val messages = database.chatMessagesQueries
                .selectMessagesByEventAndSection(eventId, section.name)
                .executeAsList()
            
            messages.map { row ->
                ChatMessage(
                    id = row.id,
                    eventId = row.event_id,
                    senderId = row.sender_id,
                    senderName = row.sender_name,
                    senderAvatarUrl = row.sender_avatar_url,
                    content = row.content,
                    section = CommentSection.valueOf(row.section!!),
                    sectionItemId = row.section_item_id,
                    parentMessageId = row.parent_message_id,
                    timestamp = row.timestamp,
                    status = MessageStatus.valueOf(row.status),
                    isOffline = row.is_offline == 1L,
                    reactions = row.reactions_json?.let {
                        try {
                            json.decodeFromString<List<Reaction>>(it)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } ?: emptyList(),
                    readBy = row.read_by_json?.let {
                        try {
                            json.decodeFromString<List<String>>(it)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } ?: emptyList(),
                    isEdited = row.is_edited == 1L
                )
            }
        } catch (e: Exception) {
            throw ChatServiceException("Failed to get messages by section: ${e.message}", e)
        }
    }
    
    /**
     * Gets a single message by ID.
     *
     * @param messageId Message ID
     * @return The message or null if not found
     */
    suspend fun getMessage(messageId: String): ChatMessage? = withContext(Dispatchers.IO) {
        try {
            database.chatMessagesQueries
                .selectMessageById(messageId)
                .executeAsOneOrNull()
                ?.let { row ->
                    ChatMessage(
                        id = row.id,
                        eventId = row.event_id,
                        senderId = row.sender_id,
                        senderName = row.sender_name,
                        senderAvatarUrl = row.sender_avatar_url,
                        content = row.content,
                        section = row.section?.let { CommentSection.valueOf(it) },
                        sectionItemId = row.section_item_id,
                        parentMessageId = row.parent_message_id,
                        timestamp = row.timestamp,
                        status = MessageStatus.valueOf(row.status),
                        isOffline = row.is_offline == 1L,
                        reactions = row.reactions_json?.let {
                            try {
                                json.decodeFromString<List<Reaction>>(it)
                            } catch (e: Exception) {
                                emptyList()
                            }
                        } ?: emptyList(),
                        readBy = row.read_by_json?.let {
                            try {
                                json.decodeFromString<List<String>>(it)
                            } catch (e: Exception) {
                                emptyList()
                            }
                        } ?: emptyList(),
                        isEdited = row.is_edited == 1L
                    )
                }
        } catch (e: Exception) {
            throw ChatServiceException("Failed to get message: ${e.message}", e)
        }
    }
    
    /**
     * Gets the count of unread messages for a user.
     *
     * @param eventId Event to check
     * @param userId User to check unread messages for
     * @return Number of unread messages
     */
    suspend fun getUnreadCount(
        eventId: String,
        userId: String
    ): Int = withContext(Dispatchers.IO) {
        try {
            database.chatMessagesQueries
                .selectUnreadCountForUser(eventId, userId, userId)
                .executeAsOne()
                .toInt()
        } catch (e: Exception) {
            throw ChatServiceException("Failed to get unread count: ${e.message}", e)
        }
    }
    
    /**
     * Gets currently typing users for an event.
     *
     * @param eventId Event to get typing users for
     * @return List of typing indicators
     */
    suspend fun getTypingUsers(eventId: String): List<TypingIndicator> = withContext(Dispatchers.IO) {
        try {
            val threshold = (System.currentTimeMillis() - 3000).toString()
            
            database.typingIndicatorsQueries
                .selectActiveTypingUsers(eventId, threshold)
                .executeAsList()
                .map { row ->
                    TypingIndicator(
                        userId = row.user_id,
                        chatId = eventId,
                        chatType = ChatType.EVENT,
                        typingStatus = try {
                            TypingStatus.valueOf(row.typing_status)
                        } catch (e: Exception) {
                            TypingStatus.IDLE
                        },
                        lastSeenTyping = row.last_seen_typing,
                        lastActivity = row.last_activity
                    )
                }
        } catch (e: Exception) {
            throw ChatServiceException("Failed to get typing users: ${e.message}", e)
        }
    }
    
    /**
     * Updates message content.
     *
     * @param messageId Message to update
     * @param content New message content
     * @param userId User making the update (for authorization)
     * @return The updated message
     */
    suspend fun updateMessage(
        messageId: String,
        content: String,
        userId: String
    ): ChatMessage? = withContext(Dispatchers.IO) {
        try {
            val timestamp = getCurrentTimestamp()
            
            // Verify user owns the message
            val message = database.chatMessagesQueries
                .selectMessageById(messageId)
                .executeAsOneOrNull()
            
            if (message == null || message.sender_id != userId) {
                return@withContext null
            }
            
            // Update the message
            database.chatMessagesQueries.updateMessage(
                content,
                timestamp,
                timestamp,
                messageId
            )
            
            // Get the updated message
            getMessage(messageId)
        } catch (e: Exception) {
            throw ChatServiceException("Failed to update message: ${e.message}", e)
        }
    }
    
    /**
     * Deletes a message.
     *
     * @param messageId Message to delete
     * @param userId User making the deletion (for authorization)
     * @return True if deleted, false otherwise
     */
    suspend fun deleteMessage(
        messageId: String,
        userId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Verify user owns the message
            val message = database.chatMessagesQueries
                .selectMessageById(messageId)
                .executeAsOneOrNull()
            
            if (message == null || message.sender_id != userId) {
                return@withContext false
            }
            
            // Delete the message
            database.chatMessagesQueries.deleteMessage(messageId)
            
            // If this was a reply, decrement parent's reply count
            message.parent_message_id?.let { parentId ->
                database.chatMessagesQueries.decrementReplyCount(
                    getCurrentTimestamp(),
                    parentId
                )
            }
            
            true
        } catch (e: Exception) {
            throw ChatServiceException("Failed to delete message: ${e.message}", e)
        }
    }
    
    /**
     * Cleans up expired typing indicators.
     *
     * @param eventId Event to clean up
     */
    suspend fun cleanupExpiredTypingIndicators(eventId: String) = withContext(Dispatchers.IO) {
        try {
            val threshold = (System.currentTimeMillis() - 3000).toString()
            database.typingIndicatorsQueries.cleanupExpiredTypingIndicators(
                eventId,
                threshold
            )
        } catch (e: Exception) {
            // Log but don't throw - this is a cleanup operation
        }
    }
    
    // ========== Private broadcast methods ==========
    
    private fun broadcastMessage(eventId: String, message: ChatMessage) {
        val response = ChatWebSocketResponse(
            type = ChatMessageType.MESSAGE,
            data = MessageData(
                messageId = message.id,
                eventId = eventId,
                userId = message.senderId,
                userName = message.senderName,
                content = message.content,
                timestamp = message.timestamp
            ),
            success = true
        )
        eventConnections.broadcast(eventId, response)
    }
    
    private fun broadcastReaction(eventId: String, messageId: String, userId: String, emoji: String) {
        val response = ChatWebSocketResponse(
            type = ChatMessageType.REACTION,
            data = MessageData(
                messageId = messageId,
                eventId = eventId,
                userId = userId,
                userName = "",
                content = null,
                reaction = emoji,
                targetMessageId = messageId,
                timestamp = getCurrentTimestamp()
            ),
            success = true
        )
        eventConnections.broadcast(eventId, response)
    }
    
    private fun broadcastReadReceipt(eventId: String, messageId: String, userId: String) {
        val response = ChatWebSocketResponse(
            type = ChatMessageType.READ_RECEIPT,
            data = MessageData(
                messageId = messageId,
                eventId = eventId,
                userId = userId,
                userName = "",
                content = null,
                timestamp = getCurrentTimestamp()
            ),
            success = true
        )
        eventConnections.broadcast(eventId, response)
    }
    
    private fun broadcastTyping(eventId: String, userId: String, userName: String, isTyping: Boolean) {
        val response = ChatWebSocketResponse(
            type = ChatMessageType.TYPING,
            data = MessageData(
                eventId = eventId,
                userId = userId,
                userName = userName,
                content = if (isTyping) "typing" else null,
                timestamp = getCurrentTimestamp()
            ),
            success = true
        )
        eventConnections.broadcast(eventId, response)
    }
    
    companion object {
        /**
         * Generates a unique message ID.
         */
        private fun generateMessageId(): String {
            return "msg_${UUID.randomUUID()}"
        }
        
        /**
         * Gets the current timestamp in ISO 8601 format.
         */
        private fun getCurrentTimestamp(): String {
            return java.time.Instant.now().toString()
        }
    }
}

/**
 * Exception thrown by ChatService operations.
 *
 * @property message Error message
 * @property cause Original exception
 */
class ChatServiceException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Routes for chat HTTP endpoints.
 */
fun io.ktor.server.routing.Route.chatRoutes(
    chatService: ChatService
) {
    route("/api/events/{eventId}/chat") {
        // Get messages for an event
        get("messages") {
            val eventId = call.parameters["eventId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Event ID required")
            )
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            val messages = chatService.getMessages(eventId, limit, offset)
            val totalCount = chatService.getMessages(eventId, Int.MAX_VALUE, 0).size

            call.respond(HttpStatusCode.OK, mapOf(
                "messages" to messages,
                "totalCount" to totalCount,
                "hasMore" to (messages.size == limit)
            ))
        }

        // Send a new message
        post("messages") {
            val eventId = call.parameters["eventId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Event ID required")
            )
            val principal = call.principal<JWTPrincipal>() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val userId = principal.payload.getClaim("userId")?.asString() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Invalid user ID in token")
            )
            val userName = principal.payload.getClaim("userName")?.asString() ?: "Unknown"
            val userAvatarUrl = principal.payload.getClaim("avatarUrl")?.asString()

            val request = call.receive<CreateMessageRequest>()

            val message = chatService.sendMessage(
                eventId = eventId,
                userId = userId,
                userName = userName,
                userAvatarUrl = userAvatarUrl,
                content = request.content,
                section = request.section,
                parentMessageId = request.parentMessageId
            )

            call.respond(HttpStatusCode.Created, mapOf("message" to message))
        }

        // Get a specific message
        get("messages/{messageId}") {
            val messageId = call.parameters["messageId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Message ID required")
            )
            val message = chatService.getMessage(messageId)

            if (message != null) {
                call.respond(HttpStatusCode.OK, mapOf("message" to message))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Message not found"))
            }
        }

        // Update a message
        put("messages/{messageId}") {
            val messageId = call.parameters["messageId"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Message ID required")
            )
            val principal = call.principal<JWTPrincipal>() ?: return@put call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val userId = principal.payload.getClaim("userId")?.asString() ?: return@put call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Invalid user ID in token")
            )
            val request = call.receive<CreateMessageRequest>()

            val message = chatService.updateMessage(messageId, request.content, userId)

            if (message != null) {
                call.respond(HttpStatusCode.OK, mapOf("message" to message))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Message not found or unauthorized"))
            }
        }

        // Delete a message
        delete("messages/{messageId}") {
            val messageId = call.parameters["messageId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Message ID required")
            )
            val principal = call.principal<JWTPrincipal>() ?: return@delete call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val userId = principal.payload.getClaim("userId")?.asString() ?: return@delete call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Invalid user ID in token")
            )

            val deleted = chatService.deleteMessage(messageId, userId)

            if (deleted) {
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Message not found or unauthorized"))
            }
        }

        // Get thread messages
        get("messages/{messageId}/replies") {
            val messageId = call.parameters["messageId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Message ID required")
            )
            val messages = chatService.getThreadMessages(messageId)
            call.respond(HttpStatusCode.OK, mapOf("messages" to messages))
        }

        // Add reaction to a message
        post("messages/{messageId}/reactions") {
            val eventId = call.parameters["eventId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Event ID required")
            )
            val messageId = call.parameters["messageId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Message ID required")
            )
            val principal = call.principal<JWTPrincipal>() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val userId = principal.payload.getClaim("userId")?.asString() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Invalid user ID in token")
            )
            val request = call.receive<AddReactionRequest>()

            chatService.addReaction(eventId, messageId, userId, request.emoji)
            call.respond(HttpStatusCode.OK, mapOf("success" to true))
        }

        // Remove reaction from a message
        delete("messages/{messageId}/reactions") {
            val eventId = call.parameters["eventId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Event ID required")
            )
            val messageId = call.parameters["messageId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Message ID required")
            )
            val principal = call.principal<JWTPrincipal>() ?: return@delete call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val userId = principal.payload.getClaim("userId")?.asString() ?: return@delete call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Invalid user ID in token")
            )
            val emoji = call.request.queryParameters["emoji"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Emoji required")
            )

            chatService.removeReaction(eventId, messageId, userId, emoji)
            call.respond(HttpStatusCode.OK, mapOf("success" to true))
        }

        // Mark message as read
        post("messages/{messageId}/read") {
            val eventId = call.parameters["eventId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Event ID required")
            )
            val messageId = call.parameters["messageId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Message ID required")
            )
            val principal = call.principal<JWTPrincipal>() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val userId = principal.payload.getClaim("userId")?.asString() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Invalid user ID in token")
            )

            chatService.markAsRead(eventId, messageId, userId)
            call.respond(HttpStatusCode.OK, mapOf("success" to true))
        }

        // Mark all messages as read
        post("messages/read-all") {
            val eventId = call.parameters["eventId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Event ID required")
            )
            val principal = call.principal<JWTPrincipal>() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val userId = principal.payload.getClaim("userId")?.asString() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Invalid user ID in token")
            )

            chatService.markAllAsRead(eventId, userId)
            call.respond(HttpStatusCode.OK, mapOf("success" to true))
        }

        // Get typing users
        get("typing") {
            val eventId = call.parameters["eventId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Event ID required")
            )
            val typingUsers = chatService.getTypingUsers(eventId)
            call.respond(HttpStatusCode.OK, mapOf("typingUsers" to typingUsers))
        }

        // Set typing status
        post("typing") {
            val eventId = call.parameters["eventId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Event ID required")
            )
            val principal = call.principal<JWTPrincipal>() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val userId = principal.payload.getClaim("userId")?.asString() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Invalid user ID in token")
            )
            val userName = principal.payload.getClaim("userName")?.asString() ?: "Unknown"
            val isTyping = call.request.queryParameters["typing"]?.toBoolean() ?: true

            chatService.setTypingStatus(eventId, userId, userName, isTyping)
            call.respond(HttpStatusCode.OK, mapOf("success" to true))
        }

        // Get unread count
        get("unread-count") {
            val eventId = call.parameters["eventId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Event ID required")
            )
            val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val userId = principal.payload.getClaim("userId")?.asString() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Invalid user ID in token")
            )

            val count = chatService.getUnreadCount(eventId, userId)
            call.respond(HttpStatusCode.OK, mapOf("unreadCount" to count))
        }

        // Get messages by section
        get("messages/section/{section}") {
            val eventId = call.parameters["eventId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Event ID required")
            )
            val sectionName = call.parameters["section"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Section required")
            )
            val section = try {
                CommentSection.valueOf(sectionName.uppercase())
            } catch (e: Exception) {
                return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid section"))
            }

            val messages = chatService.getMessagesBySection(eventId, section)
            call.respond(HttpStatusCode.OK, mapOf("messages" to messages))
        }
    }
}
