package com.guyghost.wakeve.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * ChatService - Manages real-time chat functionality with offline support and automatic reconnection.
 *
 * This service handles:
 * - Real-time messaging via WebSocket (with SSE fallback)
 * - Automatic reconnection with exponential backoff
 * - Typing indicators
 * - Emoji reactions
 * - Offline message queue
 * - Read receipts
 *
 * @property currentUserId The ID of the currently authenticated user
 * @property currentUserName The display name of the current user
 * @property reconnectionManager Manages WebSocket reconnection with exponential backoff
 */
class ChatService(
    private val currentUserId: String,
    private val currentUserName: String,
    private val reconnectionManager: ReconnectionManager? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // JSON serialization
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // Messages state
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    // Typing indicators
    private val _typingUsers = MutableStateFlow<List<TypingIndicator>>(emptyList())
    val typingUsers: StateFlow<List<TypingIndicator>> = _typingUsers.asStateFlow()
    
    // Online participants
    private val _participants = MutableStateFlow<List<ChatParticipant>>(emptyList())
    val participants: StateFlow<List<ChatParticipant>> = _participants.asStateFlow()
    
    // WebSocket connection state
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    // Connection events (for UI feedback)
    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>()
    val connectionEvents: Flow<ConnectionEvent> = _connectionEvents.asSharedFlow()
    
    // Offline message queue (messages created while disconnected)
    private val _offlineQueue = MutableStateFlow<List<ChatMessage>>(emptyList())
    
    // Typing timeout jobs per user
    private val typingTimeouts = mutableMapOf<String, kotlinx.coroutines.Job>()
    
    // WebSocket URL (set when connecting to an event)
    private var webSocketUrl: String? = null
    
    // Active event ID
    private var activeEventId: String? = null

    /**
     * Connects to the WebSocket server for a specific event.
     * This method initiates the WebSocket connection and sets up reconnection handling.
     *
     * @param eventId The event ID to connect to
     * @return true if connection successful, false otherwise
     */
    suspend fun connectWebSocket(eventId: String): Boolean {
        // TODO: Implement actual WebSocket connection
        // For now, simulate successful connection
        scope.launch {
            _isConnected.value = true
            _connectionEvents.emit(ConnectionEvent.Connected)
        }

        return true
    }

    /**
     * Checks if the WebSocket is currently connected for a specific event.
     *
     * @param eventId The event ID to check
     * @return true if connected, false otherwise
     */
    fun isConnected(eventId: String): Boolean {
        return _isConnected.value && activeEventId == eventId
    }

    /**
     * Starts automatic reconnection for the WebSocket.
     * Uses exponential backoff with maximum 10 attempts.
     *
     * @param eventId The event ID to reconnect to
     */
    fun startAutoReconnect(eventId: String) {
        reconnectionManager?.startAutoReconnect(eventId)
    }

    /**
     * Stops automatic reconnection attempts.
     *
     * @param eventId The event ID to stop reconnecting to
     */
    fun stopAutoReconnect(eventId: String) {
        reconnectionManager?.stopAutoReconnect()
    }

    /**
     * Handles WebSocket disconnection events.
     * Removes the connection from active connections and initiates auto-reconnect.
     *
     * @param eventId The event ID that was disconnected
     * @param reason The reason for disconnection
     */
    private suspend fun handleDisconnect(eventId: String, reason: String) {
        println("[ChatService] Disconnected from $eventId: $reason")

        // Update connection state
        _isConnected.value = false
        _connectionEvents.emit(ConnectionEvent.Disconnected)

        // Remove from active connections (if we were tracking them)
        // Note: In this implementation, we use _isConnected state

        // Start auto-reconnect if reconnection manager is available
        if (reconnectionManager != null) {
            startAutoReconnect(eventId)
        }
    }

    /**
     * Handles successful WebSocket connection.
     * Updates connection state and resets the reconnection manager.
     *
     * @param eventId The event ID that connected
     */
    private fun handleConnect(eventId: String) {
        println("[ChatService] Connected to $eventId")

        // Update connection state
        _isConnected.value = true

        // Reset reconnection manager
        reconnectionManager?.reset()
    }
    
    /**
     * Connect to a chat room for a specific event.
     *
     * @param eventId The event to connect to
     * @param webSocketUrl Base WebSocket URL (will be appended with eventId)
     */
    fun connectToChat(eventId: String, webSocketUrl: String) {
        this.activeEventId = eventId
        this.webSocketUrl = webSocketUrl

        // Connect to WebSocket
        scope.launch {
            val connected = connectWebSocket(eventId)
            if (connected) {
                handleConnect(eventId)
            }

            // Load existing messages from local cache/database
            loadCachedMessages(eventId)

            // Send any queued offline messages
            sendQueuedMessages()
        }
    }

    /**
     * Disconnect from the current chat room.
     * Stops auto-reconnection to prevent unwanted reconnects.
     */
    fun disconnect() {
        val eventId = activeEventId

        // Stop auto-reconnect
        eventId?.let { stopAutoReconnect(it) }

        scope.launch {
            _isConnected.value = false
            _connectionEvents.emit(ConnectionEvent.Disconnected)
        }

        activeEventId = null
        webSocketUrl = null
    }
    
    /**
     * Send a new message.
     * 
     * @param content The message text content
     * @param section Optional category for the message
     * @param parentId Optional parent message ID for replies
     */
    fun sendMessage(
        content: String,
        section: CommentSection? = null,
        parentId: String? = null
    ) {
        val eventId = activeEventId ?: return
        
        val message = ChatMessage(
            id = generateMessageId(),
            eventId = eventId,
            senderId = currentUserId,
            senderName = currentUserName,
            content = content,
            section = section,
            parentId = parentId,
            timestamp = getCurrentTimestamp(),
            status = if (_isConnected.value) MessageStatus.SENT else MessageStatus.SENT
        )
        
        if (_isConnected.value) {
            // Send via WebSocket
            sendViaWebSocket(message)
        } else {
            // Queue for offline sending
            queueOfflineMessage(message)
        }
        
        // Add to local state immediately (optimistic update)
        _messages.update { currentMessages ->
            currentMessages + message
        }
    }
    
    /**
     * Add an emoji reaction to a message.
     * 
     * @param messageId The message to react to
     * @param emoji The emoji to add
     */
    fun addReaction(messageId: String, emoji: String) {
        val reaction = Reaction(
            userId = currentUserId,
            emoji = emoji,
            timestamp = getCurrentTimestamp()
        )
        
        // Optimistic update
        _messages.update { messages ->
            messages.map { message ->
                if (message.id == messageId) {
                    message.copy(
                        reactions = message.reactions + reaction
                    )
                } else {
                    message
                }
            }
        }
        
        // Send via WebSocket if connected
        if (_isConnected.value) {
            val payload = ReactionPayload(
                messageId = messageId,
                userId = currentUserId,
                emoji = emoji,
                action = "add"
            )
            sendWebSocketMessage(WebSocketMessageType.REACTION, json.encodeToString(ReactionPayload.serializer(), payload))
        }
    }
    
    /**
     * Remove an emoji reaction from a message.
     * 
     * @param messageId The message to unreact from
     * @param emoji The emoji to remove
     */
    fun removeReaction(messageId: String, emoji: String) {
        // Optimistic update
        _messages.update { messages ->
            messages.map { message ->
                if (message.id == messageId) {
                    message.copy(
                        reactions = message.reactions.filterNot {
                            it.userId == currentUserId && it.emoji == emoji
                        }
                    )
                } else {
                    message
                }
            }
        }
        
        // Send via WebSocket if connected
        if (_isConnected.value) {
            val payload = ReactionPayload(
                messageId = messageId,
                userId = currentUserId,
                emoji = emoji,
                action = "remove"
            )
            sendWebSocketMessage(WebSocketMessageType.REACTION, json.encodeToString(ReactionPayload.serializer(), payload))
        }
    }
    
    /**
     * Notify that the user started typing.
     */
    fun startTyping() {
        val eventId = activeEventId ?: return
        
        // Cancel existing timeout for this user
        typingTimeouts[currentUserId]?.cancel()
        
        // Send typing indicator
        if (_isConnected.value) {
            val payload = TypingPayload(
                userId = currentUserId,
                userName = currentUserName,
                chatId = eventId,
                timestamp = getCurrentTimestamp()
            )
            sendWebSocketMessage(WebSocketMessageType.TYPING, json.encodeToString(TypingPayload.serializer(), payload))
            
            // Set timeout to auto-stop typing after 3 seconds
            typingTimeouts[currentUserId] = scope.launch {
                delay(TYPING_TIMEOUT)
                stopTyping()
            }
        }
    }
    
    /**
     * Notify that the user stopped typing.
     */
    fun stopTyping() {
        val eventId = activeEventId ?: return
        
        // Cancel timeout
        typingTimeouts[currentUserId]?.cancel()
        typingTimeouts.remove(currentUserId)
        
        // Send stopped typing indicator
        if (_isConnected.value) {
            val payload = TypingPayload(
                userId = currentUserId,
                userName = currentUserName,
                chatId = eventId,
                timestamp = getCurrentTimestamp()
            )
            sendWebSocketMessage(WebSocketMessageType.STOPPED_TYPING, json.encodeToString(TypingPayload.serializer(), payload))
        }
    }
    
    /**
     * Mark a message as read.
     * 
     * @param messageId The message that was read
     */
    fun markAsRead(messageId: String) {
        // Optimistic update
        _messages.update { messages ->
            messages.map { message ->
                if (message.id == messageId && !message.readBy.contains(currentUserId)) {
                    message.copy(
                        readBy = message.readBy + currentUserId,
                        status = if (message.readBy.size >= 1) MessageStatus.READ else message.status
                    )
                } else {
                    message
                }
            }
        }
        
        // Send read receipt via WebSocket
        if (_isConnected.value) {
            val payload = ReadReceiptPayload(
                messageId = messageId,
                userId = currentUserId,
                timestamp = getCurrentTimestamp()
            )
            sendWebSocketMessage(WebSocketMessageType.READ_RECEIPT, json.encodeToString(ReadReceiptPayload.serializer(), payload))
        }
    }
    
    /**
     * Mark all messages in the current chat as read.
     */
    fun markAllAsRead() {
        _messages.value.forEach { message ->
            if (!message.readBy.contains(currentUserId)) {
                markAsRead(message.id)
            }
        }
    }
    
    /**
     * Load messages for a specific event from cache.
     */
    private fun loadCachedMessages(eventId: String) {
        // TODO: Load from local database/SQLDelight
        // For now, use empty list
        _messages.value = emptyList()
    }
    
    /**
     * Queue a message for sending when connection is restored.
     */
    private fun queueOfflineMessage(message: ChatMessage) {
        _offlineQueue.update { queue ->
            queue + message.copy(isOffline = true)
        }
        scope.launch {
            _connectionEvents.emit(ConnectionEvent.MessageQueued(message.id))
        }
    }
    
    /**
     * Send all queued offline messages when connection is restored.
     */
    private suspend fun sendQueuedMessages() {
        val queued = _offlineQueue.value
        if (queued.isEmpty()) return
        
        queued.forEach { message ->
            sendViaWebSocket(message.copy(isOffline = false))
        }
        
        _offlineQueue.value = emptyList()
        _connectionEvents.emit(ConnectionEvent.QueueFlushed(queued.size))
    }
    
    /**
     * Send a message via WebSocket.
     */
    private fun sendViaWebSocket(message: ChatMessage) {
        // TODO: Implement actual WebSocket send
        // For now, simulate successful send
        scope.launch {
            // Simulate network delay
            delay(100)
            
            // Update message status to DELIVERED
            _messages.update { messages ->
                messages.map { msg ->
                    if (msg.id == message.id) {
                        msg.copy(status = MessageStatus.DELIVERED)
                    } else {
                        msg
                    }
                }
            }
        }
    }
    
    /**
     * Send a WebSocket message with the given type and payload.
     */
    private fun sendWebSocketMessage(type: WebSocketMessageType, payloadJson: String) {
        // TODO: Implement actual WebSocket send
        val wsMessage = WebSocketMessage(type = type, data = payloadJson)
        val messageJson = json.encodeToString(wsMessage)
        
        // Log for debugging (in production, this would go to WebSocket)
        println("WebSocket send: $messageJson")
    }
    
    /**
     * Handle incoming WebSocket message.
     */
    fun handleIncomingMessage(message: WebSocketMessage) {
        when (message.type) {
            WebSocketMessageType.MESSAGE -> handleIncomingMessage(parseJson(message.data, MessagePayload.serializer()))
            WebSocketMessageType.TYPING -> handleIncomingTyping(parseJson(message.data, TypingPayload.serializer()))
            WebSocketMessageType.STOPPED_TYPING -> handleIncomingStoppedTyping(parseJson(message.data, TypingPayload.serializer()))
            WebSocketMessageType.REACTION -> handleIncomingReaction(parseJson(message.data, ReactionPayload.serializer()))
            WebSocketMessageType.READ_RECEIPT -> handleIncomingReadReceipt(parseJson(message.data, ReadReceiptPayload.serializer()))
            WebSocketMessageType.PRESENCE -> handleIncomingPresence(parseJson(message.data, PresencePayload.serializer()))
            else -> { /* Ignore other message types */ }
        }
    }
    
    /**
     * Helper function to parse JSON with explicit serializer.
     */
    private fun <T> parseJson(data: String, serializer: kotlinx.serialization.KSerializer<T>): T {
        return json.decodeFromString(serializer, data)
    }
    
    private fun handleIncomingMessage(payload: MessagePayload) {
        val chatMessage = ChatMessage(
            id = payload.messageId,
            eventId = payload.eventId,
            senderId = payload.senderId,
            senderName = payload.senderName,
            content = payload.content,
            section = payload.section?.let { runCatching { CommentSection.valueOf(it) }.getOrNull() },
            parentId = payload.parentId,
            timestamp = payload.timestamp,
            status = MessageStatus.DELIVERED
        )
        
        _messages.update { messages ->
            if (messages.any { it.id == chatMessage.id }) {
                messages // Already exists, ignore
            } else {
                messages + chatMessage
            }
        }
    }
    
    private fun handleIncomingTyping(payload: TypingPayload) {
        if (payload.userId == currentUserId) return // Ignore own typing
        
        val indicator = TypingIndicator(
            userId = payload.userId,
            userName = payload.userName,
            chatId = payload.chatId,
            lastSeenTyping = payload.timestamp
        )
        
        _typingUsers.update { users ->
            val filtered = users.filter { it.userId != payload.userId }
            filtered + indicator
        }
        
        // Set timeout to remove typing indicator
        scope.launch {
            delay(TYPING_TIMEOUT)
            _typingUsers.update { users ->
                users.filter { it.userId != payload.userId }
            }
        }
    }
    
    private fun handleIncomingStoppedTyping(payload: TypingPayload) {
        if (payload.userId == currentUserId) return
        
        _typingUsers.update { users ->
            users.filter { it.userId != payload.userId }
        }
    }
    
    private fun handleIncomingReaction(payload: ReactionPayload) {
        _messages.update { messages ->
            messages.map { message ->
                if (message.id == payload.messageId) {
                    when (payload.action) {
                        "add" -> message.copy(
                            reactions = message.reactions + Reaction(
                                userId = payload.userId,
                                emoji = payload.emoji,
                                timestamp = getCurrentTimestamp()
                            )
                        )
                        "remove" -> message.copy(
                            reactions = message.reactions.filterNot {
                                it.userId == payload.userId && it.emoji == payload.emoji
                            }
                        )
                        else -> message
                    }
                } else {
                    message
                }
            }
        }
    }
    
    private fun handleIncomingReadReceipt(payload: ReadReceiptPayload) {
        _messages.update { messages ->
            messages.map { message ->
                if (message.id == payload.messageId && !message.readBy.contains(payload.userId)) {
                    message.copy(
                        readBy = message.readBy + payload.userId,
                        status = if (message.readBy.isNotEmpty()) MessageStatus.READ else message.status
                    )
                } else {
                    message
                }
            }
        }
    }
    
    private fun handleIncomingPresence(payload: PresencePayload) {
        val participant = ChatParticipant(
            userId = payload.userId,
            userName = payload.userName,
            isOnline = payload.isOnline,
            lastSeen = payload.lastSeen
        )
        
        _participants.update { participants ->
            val filtered = participants.filter { it.userId != payload.userId }
            filtered + participant
        }
    }
    
    /**
     * Get messages for a specific thread (replies to a parent message).
     */
    fun getThreadReplies(parentId: String): List<ChatMessage> {
        return _messages.value.filter { it.parentId == parentId }
    }
    
    /**
     * Filter messages by section/category.
     */
    fun getMessagesBySection(section: CommentSection): List<ChatMessage> {
        return _messages.value.filter { it.section == section }
    }
    
    /**
     * Get the current reconnection state.
     *
     * @return The current [ReconnectionManager.ConnectionState] or null if no reconnection manager is configured
     */
    fun getReconnectionState(): ReconnectionManager.ConnectionState? {
        return reconnectionManager?.getConnectionState()
    }

    /**
     * Get the current retry count for reconnection.
     *
     * @return The number of retries attempted, or null if no reconnection manager is configured
     */
    fun getRetryCount(): Int? {
        return reconnectionManager?.getRetryCount()
    }

    /**
     * Get the current delay before the next reconnection attempt.
     *
     * @return The current delay in milliseconds, or null if no reconnection manager is configured
     */
    fun getCurrentDelay(): Long? {
        return reconnectionManager?.getCurrentDelay()
    }

    /**
     * Clear all messages (e.g., when leaving chat).
     */
    fun clearMessages() {
        _messages.value = emptyList()
        _typingUsers.value = emptyList()
        _participants.value = emptyList()
    }
    
    companion object {
        private val TYPING_TIMEOUT = 3.seconds
        
        private fun generateMessageId(): String = "msg-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(10000)}"
        private fun getCurrentTimestamp(): String = kotlinx.datetime.Clock.System.now().toString()
    }
}

/**
 * Connection events for UI feedback.
 */
sealed class ConnectionEvent {
    data object Connected : ConnectionEvent()
    data object Disconnected : ConnectionEvent()
    data class MessageQueued(val messageId: String) : ConnectionEvent()
    data class QueueFlushed(val count: Int) : ConnectionEvent()
    data class Error(val message: String) : ConnectionEvent()
}
