package com.guyghost.wakeve.routes

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.AddReactionRequest
import com.guyghost.wakeve.models.ChatMessage
import com.guyghost.wakeve.models.ChatMessageType
import com.guyghost.wakeve.models.ChatType
import com.guyghost.wakeve.models.ChatWebSocketResponse
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.models.CreateMessageRequest
import com.guyghost.wakeve.models.MessageData
import com.guyghost.wakeve.models.MessageStatus
import com.guyghost.wakeve.models.MessagesResponse
import com.guyghost.wakeve.models.Reaction
import com.guyghost.wakeve.models.TypingIndicator
import com.guyghost.wakeve.models.TypingStatus
import com.guyghost.wakeve.moderation.ModerationPolicy
import com.guyghost.wakeve.moderation.ModerationRejectedException
import com.guyghost.wakeve.moderation.ModerationRepository
import com.guyghost.wakeve.moderation.ModerationStatus
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
    private val database: WakeveDb,
    private val eventConnections: EventChatConnections = com.guyghost.wakeve.routes.eventChatConnections,
    private val moderationPolicy: ModerationPolicy = ModerationPolicy(),
    private val moderationRepository: ModerationRepository? = null
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
    ): ChatMessage? = withContext(Dispatchers.IO) {
        try {
            val moderationResult = moderationPolicy.evaluate(content)
            if (moderationResult.status == ModerationStatus.REJECTED) {
                throw ModerationRejectedException(moderationResult)
            }
            val parentMessage = parentMessageId?.let { parentId ->
                getMessage(parentId) ?: return@withContext null
            }
            if (parentMessage != null && (parentMessage.eventId != eventId || !parentMessage.isVisibleTo(userId))) {
                return@withContext null
            }

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
                readBy = emptyList(),
                moderationStatus = moderationResult.status
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
                moderation_status = moderationResult.status.name,
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
            
            // Broadcast only content that is immediately visible to other participants.
            if (moderationResult.status == ModerationStatus.APPROVED) {
                broadcastMessage(eventId, message)
            }
            
            message
        } catch (e: ModerationRejectedException) {
            throw e
        } catch (e: Exception) {
            throw ChatServiceException(chatMessageSendFailureMessage(), e)
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
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            getAccessibleMessage(eventId, messageId, userId) ?: return@withContext false
            val normalizedEmoji = emoji.trim().takeIf { it.isNotEmpty() } ?: return@withContext false
            val timestamp = getCurrentTimestamp()
            val reactionId = generateMessageId()
            
            // Add reaction to message_reaction table
            database.chatMessagesQueries.insertReaction(
                id = reactionId,
                message_id = messageId,
                user_id = userId,
                emoji = normalizedEmoji,
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
            broadcastReaction(eventId, messageId, userId, normalizedEmoji)
            true
        } catch (e: Exception) {
            throw ChatServiceException(chatReactionAddFailureMessage(), e)
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
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            getAccessibleMessage(eventId, messageId, userId) ?: return@withContext false
            val normalizedEmoji = emoji.trim().takeIf { it.isNotEmpty() } ?: return@withContext false
            val timestamp = getCurrentTimestamp()
            
            // Remove reaction from message_reaction table
            database.chatMessagesQueries.deleteReaction(
                messageId,
                userId,
                normalizedEmoji
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
            broadcastReaction(eventId, messageId, userId, normalizedEmoji)
            true
        } catch (e: Exception) {
            throw ChatServiceException(chatReactionRemoveFailureMessage(), e)
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
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val message = getAccessibleMessage(eventId, messageId, userId) ?: return@withContext false
            val timestamp = getCurrentTimestamp()
            val readStatusId = generateMessageId()
            
            // Add read status to message_read_status table
            database.chatMessagesQueries.insertReadStatus(
                id = readStatusId,
                message_id = messageId,
                user_id = userId,
                read_at = timestamp
            )
            
            val currentReadBy = message.readBy.toMutableList()
            if (!currentReadBy.contains(userId)) {
                currentReadBy.add(userId)
                val readByJson = json.encodeToString(currentReadBy)
                database.chatMessagesQueries.updateMessageReadBy(
                    readByJson,
                    timestamp,
                    messageId
                )
            }
            
            // Broadcast read receipt via WebSocket
            broadcastReadReceipt(eventId, messageId, userId)
            true
        } catch (e: Exception) {
            throw ChatServiceException(chatReadReceiptFailureMessage(), e)
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
            
            val messages = getMessages(eventId, limit = 0, viewerUserId = userId)
                .filter { it.senderId != userId }
            
            // Mark each message as read
            messages.forEach { message ->
                val readStatusId = generateMessageId()
                database.chatMessagesQueries.insertReadStatus(
                    id = readStatusId,
                    message_id = message.id,
                    user_id = userId,
                    read_at = timestamp
                )

                if (!message.readBy.contains(userId)) {
                    val readByJson = json.encodeToString(message.readBy + userId)
                    database.chatMessagesQueries.updateMessageReadBy(
                        readByJson,
                        timestamp,
                        message.id
                    )
                }
            }
        } catch (e: Exception) {
            throw ChatServiceException(chatMarkAllReadFailureMessage(), e)
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
            throw ChatServiceException(chatTypingStatusFailureMessage(), e)
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
        offset: Int = 0,
        viewerUserId: String? = null
    ): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val messages = if (limit > 0) {
                if (viewerUserId != null) {
                    database.chatMessagesQueries
                        .selectVisibleMessagesByEventPaginated(eventId, viewerUserId, limit.toLong(), offset.toLong())
                        .executeAsList()
                } else {
                    database.chatMessagesQueries
                        .selectMessagesByEventPaginated(eventId, limit.toLong(), offset.toLong())
                        .executeAsList()
                }
            } else {
                if (viewerUserId != null) {
                    database.chatMessagesQueries
                        .selectVisibleMessagesByEvent(eventId, viewerUserId)
                        .executeAsList()
                } else {
                    database.chatMessagesQueries
                        .selectMessagesByEvent(eventId)
                        .executeAsList()
                }
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
                    isEdited = row.is_edited == 1L,
                    moderationStatus = ModerationStatus.valueOf(row.moderation_status)
                )
            }.filter { message -> message.isVisibleTo(viewerUserId) }
        } catch (e: Exception) {
            throw ChatServiceException(chatMessagesFetchFailureMessage(), e)
        }
    }

    suspend fun countVisibleMessages(
        eventId: String,
        viewerUserId: String? = null
    ): Int = withContext(Dispatchers.IO) {
        try {
            val totalCount = if (viewerUserId != null) {
                database.chatMessagesQueries.countVisibleApprovedMessagesByEvent(eventId, viewerUserId).executeAsOne()
            } else {
                database.chatMessagesQueries.countApprovedMessagesByEvent(eventId).executeAsOne()
            }

            totalCount
                .coerceAtLeast(0)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        } catch (e: Exception) {
            throw ChatServiceException(chatMessagesCountFailureMessage(), e)
        }
    }
    
    /**
     * Gets messages for a specific thread.
     *
     * @param parentMessageId Parent message ID (thread root)
     * @return List of messages in the thread
     */
    suspend fun getThreadMessages(
        eventId: String,
        parentMessageId: String,
        viewerUserId: String? = null
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
                    isEdited = row.is_edited == 1L,
                    moderationStatus = ModerationStatus.valueOf(row.moderation_status)
                )
            }.filter { message ->
                message.eventId == eventId && message.isVisibleTo(viewerUserId)
            }
        } catch (e: Exception) {
            throw ChatServiceException(chatThreadMessagesFetchFailureMessage(), e)
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
        section: CommentSection,
        viewerUserId: String? = null
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
                    isEdited = row.is_edited == 1L,
                    moderationStatus = ModerationStatus.valueOf(row.moderation_status)
                )
            }.filter { message -> message.isVisibleTo(viewerUserId) }
        } catch (e: Exception) {
            throw ChatServiceException(chatSectionMessagesFetchFailureMessage(), e)
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
                        isEdited = row.is_edited == 1L,
                        moderationStatus = ModerationStatus.valueOf(row.moderation_status)
                    )
                }
        } catch (e: Exception) {
            throw ChatServiceException(chatMessageFetchFailureMessage(), e)
        }
    }

    fun isBlockedForViewer(eventId: String, viewerUserId: String, senderId: String): Boolean =
        moderationRepository?.isBlockedForEvent(viewerUserId, senderId, eventId) == true

    private suspend fun getAccessibleMessage(
        eventId: String,
        messageId: String,
        viewerUserId: String
    ): ChatMessage? {
        val message = getMessage(messageId) ?: return null
        return message.takeIf { it.eventId == eventId && it.isVisibleTo(viewerUserId) }
    }

    private fun ChatMessage.isVisibleTo(viewerUserId: String?): Boolean {
        if (moderationStatus != ModerationStatus.APPROVED && senderId != viewerUserId) {
            return false
        }

        return viewerUserId == null || moderationRepository?.isBlockedForEvent(viewerUserId, senderId, eventId) != true
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
            getMessages(eventId, limit = 0, viewerUserId = userId)
                .count { message -> message.senderId != userId && !message.readBy.contains(userId) }
        } catch (e: Exception) {
            throw ChatServiceException(chatUnreadCountFailureMessage(), e)
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
            throw ChatServiceException(chatTypingUsersFetchFailureMessage(), e)
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
        eventId: String,
        messageId: String,
        content: String,
        userId: String
    ): ChatMessage? = withContext(Dispatchers.IO) {
        try {
            val moderationResult = moderationPolicy.evaluate(content)
            if (moderationResult.status == ModerationStatus.REJECTED) {
                throw ModerationRejectedException(moderationResult)
            }
            val timestamp = getCurrentTimestamp()
            
            // Verify user owns the message
            val message = database.chatMessagesQueries
                .selectMessageById(messageId)
                .executeAsOneOrNull()
            
            if (message == null || message.event_id != eventId || message.sender_id != userId) {
                return@withContext null
            }
            
            // Update the message
            database.chatMessagesQueries.updateMessage(
                content,
                timestamp,
                moderationResult.status.name,
                timestamp,
                messageId
            )
            
            // Get the updated message
            getMessage(messageId)
        } catch (e: ModerationRejectedException) {
            throw e
        } catch (e: Exception) {
            throw ChatServiceException(chatMessageUpdateFailureMessage(), e)
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
        eventId: String,
        messageId: String,
        userId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Verify user owns the message
            val message = database.chatMessagesQueries
                .selectMessageById(messageId)
                .executeAsOneOrNull()
            
            if (message == null || message.event_id != eventId || message.sender_id != userId) {
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
            throw ChatServiceException(chatMessageDeleteFailureMessage(), e)
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
        eventConnections.broadcast(eventId, response, moderationRepository)
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
        eventConnections.broadcast(eventId, response, moderationRepository)
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
        eventConnections.broadcast(eventId, response, moderationRepository)
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
        eventConnections.broadcast(eventId, response, moderationRepository)
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

internal fun chatMessageSendFailureMessage(): String =
    "Failed to send chat message. Please try again."

internal fun chatReactionAddFailureMessage(): String =
    "Failed to add chat reaction. Please try again."

internal fun chatReactionRemoveFailureMessage(): String =
    "Failed to remove chat reaction. Please try again."

internal fun chatReadReceiptFailureMessage(): String =
    "Failed to mark chat message as read. Please try again."

internal fun chatMarkAllReadFailureMessage(): String =
    "Failed to mark chat messages as read. Please try again."

internal fun chatTypingStatusFailureMessage(): String =
    "Failed to update chat typing status. Please try again."

internal fun chatMessagesFetchFailureMessage(): String =
    "Failed to fetch chat messages. Please try again."

internal fun chatMessagesCountFailureMessage(): String =
    "Failed to count chat messages. Please try again."

internal fun chatThreadMessagesFetchFailureMessage(): String =
    "Failed to fetch chat thread messages. Please try again."

internal fun chatSectionMessagesFetchFailureMessage(): String =
    "Failed to fetch chat section messages. Please try again."

internal fun chatMessageFetchFailureMessage(): String =
    "Failed to fetch chat message. Please try again."

internal fun chatUnreadCountFailureMessage(): String =
    "Failed to fetch unread chat count. Please try again."

internal fun chatTypingUsersFetchFailureMessage(): String =
    "Failed to fetch chat typing users. Please try again."

internal fun chatMessageUpdateFailureMessage(): String =
    "Failed to update chat message. Please try again."

internal fun chatMessageDeleteFailureMessage(): String =
    "Failed to delete chat message. Please try again."

/**
 * Routes for chat HTTP endpoints.
 */
fun io.ktor.server.routing.Route.chatRoutes(
    chatService: ChatService
) {
    route("/events/{eventId}/chat") {
        // Get messages for an event
        get("messages") {
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
            val limit = parseChatMessageLimit(call.request.queryParameters["limit"])
            val offset = parseChatMessageOffset(call.request.queryParameters["offset"])
            val lookaheadMessages = chatService.getMessages(eventId, limit + 1, offset, userId)
            val messages = lookaheadMessages.take(limit)
            val totalCount = chatService.countVisibleMessages(eventId, userId)

            call.respond(
                HttpStatusCode.OK,
                MessagesResponse(
                    messages = messages,
                    totalCount = totalCount,
                    hasMore = lookaheadMessages.size > limit
                )
            )
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
            val userName = principal.chatDisplayName(userId)
            val userAvatarUrl = principal.payload.getClaim("avatarUrl")?.asString()

            val request = call.receive<CreateMessageRequest>()

            try {
                val message = chatService.sendMessage(
                    eventId = eventId,
                    userId = userId,
                    userName = userName,
                    userAvatarUrl = userAvatarUrl,
                    content = request.content,
                    section = request.section,
                    parentMessageId = request.parentMessageId
                )
                if (message == null) {
                    return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Parent message not found"))
                }

                val status = if (message.moderationStatus == ModerationStatus.PENDING_REVIEW) {
                    HttpStatusCode.Accepted
                } else {
                    HttpStatusCode.Created
                }

                call.respond(status, mapOf("message" to message))
            } catch (e: ModerationRejectedException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.result.userMessage, "reasonCode" to e.result.reasonCode)
                )
            }
        }

        // Get a specific message
        get("messages/{messageId}") {
            val eventId = call.parameters["eventId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Event ID required")
            )
            val messageId = call.parameters["messageId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Message ID required")
            )
            val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val userId = principal.payload.getClaim("userId")?.asString() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Invalid user ID in token")
            )
            val message = chatService.getMessage(messageId)

            if (message != null) {
                if (
                    message.eventId != eventId ||
                    (message.moderationStatus != ModerationStatus.APPROVED && message.senderId != userId) ||
                    chatService.isBlockedForViewer(eventId, userId, message.senderId)
                ) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Message not found"))
                } else {
                    call.respond(HttpStatusCode.OK, mapOf("message" to message))
                }
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Message not found"))
            }
        }

        // Update a message
        put("messages/{messageId}") {
            val eventId = call.parameters["eventId"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Event ID required")
            )
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

            try {
                val message = chatService.updateMessage(eventId, messageId, request.content, userId)

                if (message != null) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to message))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Message not found or unauthorized"))
                }
            } catch (e: ModerationRejectedException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.result.userMessage, "reasonCode" to e.result.reasonCode)
                )
            }
        }

        // Delete a message
        delete("messages/{messageId}") {
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

            val deleted = chatService.deleteMessage(eventId, messageId, userId)

            if (deleted) {
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Message not found or unauthorized"))
            }
        }

        // Get thread messages
        get("messages/{messageId}/replies") {
            val eventId = call.parameters["eventId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Event ID required")
            )
            val messageId = call.parameters["messageId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Message ID required")
            )
            val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val userId = principal.payload.getClaim("userId")?.asString() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Invalid user ID in token")
            )
            val messages = chatService.getThreadMessages(eventId, messageId, userId)
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

            val added = chatService.addReaction(eventId, messageId, userId, request.emoji)
            if (added) {
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Message not found"))
            }
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

            val removed = chatService.removeReaction(eventId, messageId, userId, emoji)
            if (removed) {
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Message not found"))
            }
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

            val marked = chatService.markAsRead(eventId, messageId, userId)
            if (marked) {
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Message not found"))
            }
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
            val userName = principal.chatDisplayName(userId)
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
            val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val userId = principal.payload.getClaim("userId")?.asString() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Invalid user ID in token")
            )

            val messages = chatService.getMessagesBySection(eventId, section, userId)
            call.respond(HttpStatusCode.OK, mapOf("messages" to messages))
        }
    }
}

private fun JWTPrincipal.chatDisplayName(userId: String): String {
    val userName = payload.getClaim("userName")?.asString()?.trim()
    if (!userName.isNullOrEmpty()) return userName

    val emailName = payload.getClaim("email")?.asString()
        ?.trim()
        ?.substringBefore("@")
        ?.takeIf { it.isNotEmpty() }

    return emailName ?: userId
}

internal fun parseChatMessageLimit(rawLimit: String?): Int {
    val parsedLimit = rawLimit?.trim()?.toIntOrNull() ?: DEFAULT_CHAT_MESSAGE_LIMIT
    return parsedLimit.coerceIn(MIN_CHAT_MESSAGE_LIMIT, MAX_CHAT_MESSAGE_LIMIT)
}

internal fun parseChatMessageOffset(rawOffset: String?): Int {
    val parsedOffset = rawOffset?.trim()?.toIntOrNull() ?: DEFAULT_CHAT_MESSAGE_OFFSET
    return parsedOffset.coerceIn(MIN_CHAT_MESSAGE_OFFSET, MAX_CHAT_MESSAGE_OFFSET)
}

private const val DEFAULT_CHAT_MESSAGE_LIMIT = 100
private const val MIN_CHAT_MESSAGE_LIMIT = 1
private const val MAX_CHAT_MESSAGE_LIMIT = 100
private const val DEFAULT_CHAT_MESSAGE_OFFSET = 0
private const val MIN_CHAT_MESSAGE_OFFSET = 0
private const val MAX_CHAT_MESSAGE_OFFSET = 10_000
