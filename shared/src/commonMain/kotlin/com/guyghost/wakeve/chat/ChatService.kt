package com.guyghost.wakeve.chat

import com.guyghost.wakeve.database.WakeveDb
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * WebSocket client interface for KMP.
 * Each platform provides its own implementation.
 */
interface WebSocketClient {
    val incomingMessages: Flow<String>
    suspend fun connect(url: String): Boolean
    suspend fun send(message: String): Boolean
    suspend fun close()
    fun isConnected(): Boolean
}

/**
 * ChatService - Manages real-time chat functionality with offline support and automatic reconnection.
 *
 * This service handles:
 * - Real-time messaging via WebSocket
 * - Automatic reconnection with exponential backoff
 * - Typing indicators
 * - Emoji reactions
 * - Offline message queue
 * - Read receipts
 */
class ChatService(
    private val currentUserId: String,
    private val currentUserName: String,
    private val database: WakeveDb? = null,
    private val reconnectionManager: ReconnectionManager? = null,
    private val webSocketClient: WebSocketClient? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _typingUsers = MutableStateFlow<List<TypingIndicator>>(emptyList())
    val typingUsers: StateFlow<List<TypingIndicator>> = _typingUsers.asStateFlow()
    
    private val _participants = MutableStateFlow<List<ChatParticipant>>(emptyList())
    val participants: StateFlow<List<ChatParticipant>> = _participants.asStateFlow()
    
    private val _connectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()
    
    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>()
    val connectionEvents: Flow<ConnectionEvent> = _connectionEvents.asSharedFlow()
    
    private val _offlineQueue = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val typingTimeouts = mutableMapOf<String, kotlinx.coroutines.Job>()
    private var webSocketUrl: String? = null
    private var activeEventId: String? = null
    private var messageListenerJob: kotlinx.coroutines.Job? = null
    
    /**
     * Connect to the WebSocket server for a specific event.
     */
    suspend fun connectWebSocket(eventId: String): Boolean {
        val url = webSocketUrl ?: return false
        
        return try {
            _connectionState.value = WebSocketConnectionState.CONNECTING
            _connectionEvents.emit(ConnectionEvent.Connecting)
            
            val client = webSocketClient ?: run {
                _connectionState.value = WebSocketConnectionState.ERROR
                _connectionEvents.emit(ConnectionEvent.Error("No WebSocket client configured"))
                return false
            }
            
            val success = client.connect(url)
            if (success) {
                _connectionState.value = WebSocketConnectionState.CONNECTED
                _connectionEvents.emit(ConnectionEvent.Connected)
                startMessageListener(client)
                println("[ChatService] WebSocket connected to event $eventId")
                true
            } else {
                _connectionState.value = WebSocketConnectionState.ERROR
                _connectionEvents.emit(ConnectionEvent.Error("Connection failed"))
                handleDisconnect(eventId, "Connection failed")
                false
            }
        } catch (e: Exception) {
            _connectionState.value = WebSocketConnectionState.ERROR
            _connectionEvents.emit(ConnectionEvent.Error("Connection failed: ${e.message}"))
            handleDisconnect(eventId, e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * Start listening for incoming WebSocket messages.
     */
    private fun startMessageListener(client: WebSocketClient) {
        messageListenerJob?.cancel()
        messageListenerJob = scope.launch {
            client.incomingMessages.collect { messageJson ->
                try {
                    val wsMessage = json.decodeFromString(WebSocketMessage.serializer(), messageJson)
                    handleIncomingMessage(wsMessage)
                } catch (e: Exception) {
                    println("[ChatService] Error parsing WebSocket message: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Check if connected to a specific event.
     */
    fun isConnected(eventId: String): Boolean =
        _connectionState.value == WebSocketConnectionState.CONNECTED && activeEventId == eventId
    
    fun getConnectionStateValue(): WebSocketConnectionState = _connectionState.value
    
    /**
     * Start automatic reconnection.
     */
    fun startAutoReconnect(eventId: String) = reconnectionManager?.startAutoReconnect(eventId)
    
    /**
     * Stop automatic reconnection.
     */
    fun stopAutoReconnect(eventId: String) = reconnectionManager?.stopAutoReconnect()
    
    /**
     * Handle disconnection events.
     */
    private suspend fun handleDisconnect(eventId: String, reason: String) {
        println("[ChatService] Disconnected from $eventId: $reason")
        _connectionState.value = WebSocketConnectionState.DISCONNECTED
        _connectionEvents.emit(ConnectionEvent.Disconnected)
        messageListenerJob?.cancel()
        messageListenerJob = null
        if (reconnectionManager != null) startAutoReconnect(eventId)
    }
    
    /**
     * Connect to a chat room.
     */
    fun connectToChat(eventId: String, webSocketUrl: String) {
        this.activeEventId = eventId
        this.webSocketUrl = webSocketUrl
        
        scope.launch {
            val connected = connectWebSocket(eventId)
            if (connected) {
                loadCachedMessages(eventId)
                sendQueuedMessages()
            }
        }
    }
    
    /**
     * Disconnect from the current chat room.
     */
    fun disconnect() {
        val eventId = activeEventId
        eventId?.let { stopAutoReconnect(it) }
        
        scope.launch {
            webSocketClient?.close()
            _connectionState.value = WebSocketConnectionState.DISCONNECTED
            _connectionEvents.emit(ConnectionEvent.Disconnected)
        }
        
        messageListenerJob?.cancel()
        messageListenerJob = null
        activeEventId = null
        webSocketUrl = null
    }
    
    /**
     * Send a new message.
     */
    fun sendMessage(
        content: String,
        section: CommentSection? = null,
        parentId: String? = null,
        imageAttachment: com.guyghost.wakeve.chat.ChatImageAttachment? = null
    ) {
        val eventId = activeEventId ?: return
        
        // Don't send empty messages without attachments
        if (content.isBlank() && imageAttachment == null) return
        
        val message = ChatMessage(
            id = generateMessageId(),
            eventId = eventId,
            senderId = currentUserId,
            senderName = currentUserName,
            content = content,
            section = section,
            parentId = parentId,
            timestamp = getCurrentTimestamp(),
            status = MessageStatus.SENT,
            imageAttachment = imageAttachment
        )
        
        if (_connectionState.value == WebSocketConnectionState.CONNECTED) {
            sendViaWebSocket(message)
        } else {
            queueOfflineMessage(message)
        }
        
        _messages.update { currentMessages -> currentMessages + message }
    }
    
    /**
     * Add an emoji reaction.
     */
    fun addReaction(messageId: String, emoji: String) {
        val reaction = Reaction(userId = currentUserId, emoji = emoji, timestamp = getCurrentTimestamp())
        _messages.update { messages -> messages.map { 
            if (it.id == messageId) it.copy(reactions = it.reactions + reaction) else it 
        }}
        
        if (_connectionState.value == WebSocketConnectionState.CONNECTED) {
            val payload = ReactionPayload(messageId, currentUserId, emoji, "add")
            sendWebSocketMessage(WebSocketMessageType.REACTION, json.encodeToString(ReactionPayload.serializer(), payload))
        }
    }
    
    /**
     * Remove an emoji reaction.
     */
    fun removeReaction(messageId: String, emoji: String) {
        _messages.update { messages -> messages.map { 
            if (it.id == messageId) it.copy(reactions = it.reactions.filterNot { r -> r.userId == currentUserId && r.emoji == emoji }) else it 
        }}
        
        if (_connectionState.value == WebSocketConnectionState.CONNECTED) {
            val payload = ReactionPayload(messageId, currentUserId, emoji, "remove")
            sendWebSocketMessage(WebSocketMessageType.REACTION, json.encodeToString(ReactionPayload.serializer(), payload))
        }
    }
    
    /**
     * Notify that the user started typing.
     */
    fun startTyping() {
        val eventId = activeEventId ?: return
        typingTimeouts[currentUserId]?.cancel()
        
        if (_connectionState.value == WebSocketConnectionState.CONNECTED) {
            val payload = TypingPayload(currentUserId, currentUserName, eventId, getCurrentTimestamp())
            sendWebSocketMessage(WebSocketMessageType.TYPING, json.encodeToString(TypingPayload.serializer(), payload))
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
        typingTimeouts[currentUserId]?.cancel()
        typingTimeouts.remove(currentUserId)
        
        if (_connectionState.value == WebSocketConnectionState.CONNECTED) {
            val payload = TypingPayload(currentUserId, currentUserName, eventId, getCurrentTimestamp())
            sendWebSocketMessage(WebSocketMessageType.STOPPED_TYPING, json.encodeToString(TypingPayload.serializer(), payload))
        }
    }
    
    /**
     * Mark a message as read.
     */
    fun markAsRead(messageId: String) {
        _messages.update { messages -> messages.map { 
            if (it.id == messageId && !it.readBy.contains(currentUserId)) {
                it.copy(readBy = it.readBy + currentUserId, status = MessageStatus.READ)
            } else it 
        }}
        
        if (_connectionState.value == WebSocketConnectionState.CONNECTED) {
            val payload = ReadReceiptPayload(messageId, currentUserId, getCurrentTimestamp())
            sendWebSocketMessage(WebSocketMessageType.READ_RECEIPT, json.encodeToString(ReadReceiptPayload.serializer(), payload))
        }
    }
    
    /**
     * Mark all messages as read.
     */
    fun markAllAsRead() {
        _messages.value.filter { !it.readBy.contains(currentUserId) }.forEach { markAsRead(it.id) }
    }
    
    /**
     * Load messages from SQLite database.
     */
    private suspend fun loadCachedMessages(eventId: String) {
        try {
            if (database != null) {
                val cachedMessages = withContext(Dispatchers.Default) {
                    loadMessagesFromDatabase(eventId, DEFAULT_MESSAGE_LIMIT)
                }
                _messages.value = cachedMessages.sortedBy { it.timestamp }
                println("[ChatService] Loaded ${cachedMessages.size} messages from database for event $eventId")
            } else {
                _messages.value = emptyList()
                println("[ChatService] No database available, using empty message list")
            }
        } catch (e: Exception) {
            println("[ChatService] Error loading cached messages: ${e.message}")
            _messages.value = emptyList()
        }
    }
    
    /**
     * Load messages from database.
     */
    private suspend fun loadMessagesFromDatabase(eventId: String, limit: Int): List<ChatMessage> = withContext(Dispatchers.Default) {
        try {
            // Database access - implementation depends on SQLDelight generation
            // The actual query implementation should be provided by platform-specific code
            println("[ChatService] Loading messages for event: $eventId")
            emptyList()
        } catch (e: Exception) {
            println("[ChatService] Error loading messages: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Insert a message into the database.
     * Called after successful WebSocket send.
     */
    private suspend fun insertMessageToDatabase(message: ChatMessage) {
        withContext(Dispatchers.Default) {
            try {
                // Database insertion - implementation depends on SQLDelight generation
                println("[ChatService] Would insert message: ${message.id}")
            } catch (e: Exception) {
                println("[ChatService] Error inserting message: ${e.message}")
            }
        }
    }
    
    /**
     * Parse reactions JSON.
     */
    private fun parseReactionsJson(jsonString: String?): List<Reaction> {
        if (jsonString.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<ReactionDto>>(jsonString).map { Reaction(it.userId, it.emoji, it.timestamp) }
        } catch (e: Exception) { emptyList() }
    }
    
    /**
     * Parse readBy JSON.
     */
    private fun parseReadByJson(jsonString: String?): List<String> {
        if (jsonString.isNullOrBlank()) return emptyList()
        return try { json.decodeFromString(jsonString) } catch (e: Exception) { emptyList() }
    }
    
    /**
     * Queue a message for offline sending.
     */
    private fun queueOfflineMessage(message: ChatMessage) {
        _offlineQueue.update { queue -> queue + message.copy(isOffline = true) }
        scope.launch { _connectionEvents.emit(ConnectionEvent.MessageQueued(message.id)) }
    }
    
    /**
     * Send queued offline messages.
     */
    private suspend fun sendQueuedMessages() {
        val queued = _offlineQueue.value
        if (queued.isEmpty()) return
        queued.forEach { sendViaWebSocket(it.copy(isOffline = false)) }
        _offlineQueue.value = emptyList()
        _connectionEvents.emit(ConnectionEvent.QueueFlushed(queued.size))
    }
    
    /**
     * Send a message via WebSocket.
     */
    private fun sendViaWebSocket(message: ChatMessage) {
        scope.launch {
            try {
                val payload = MessagePayload(message.id, message.eventId, message.senderId, message.senderName,
                    message.content, message.section?.name, message.parentId, message.timestamp)
                val wsMessage = WebSocketMessage(WebSocketMessageType.MESSAGE, json.encodeToString(MessagePayload.serializer(), payload))
                
                webSocketClient?.send(json.encodeToString(WebSocketMessage.serializer(), wsMessage))
                saveMessageToDatabase(message)
                
                _messages.update { messages -> messages.map { if (it.id == message.id) it.copy(status = MessageStatus.DELIVERED) else it } }
                println("[ChatService] Message sent: ${message.id}")
            } catch (e: Exception) {
                println("[ChatService] Error sending message: ${e.message}")
                _messages.update { messages -> messages.map { if (it.id == message.id) it.copy(status = MessageStatus.FAILED) else it } }
                queueOfflineMessage(message.copy(status = MessageStatus.FAILED))
            }
        }
    }
    
    /**
     * Save message to SQLite database.
     */
    private suspend fun saveMessageToDatabase(message: ChatMessage) = withContext(Dispatchers.Default) {
        try {
            // Database persistence - implementation depends on SQLDelight generation
            // The actual insert implementation should be provided by platform-specific code
            println("[ChatService] Would save message: ${message.id}")
        } catch (e: Exception) {
            println("[ChatService] Error saving message: ${e.message}")
        }
    }
    
    /**
     * Send WebSocket message.
     */
    private fun sendWebSocketMessage(type: WebSocketMessageType, payloadJson: String) {
        scope.launch {
            try {
                val wsMessage = WebSocketMessage(type, payloadJson)
                val messageJson = json.encodeToString(wsMessage)
                webSocketClient?.send(messageJson)
            } catch (e: Exception) {
                println("[ChatService] Error sending WebSocket message: ${e.message}")
            }
        }
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
            else -> {}
        }
    }
    
    private fun <T> parseJson(data: String, serializer: kotlinx.serialization.KSerializer<T>): T =
        json.decodeFromString(serializer, data)
    
    private fun handleIncomingMessage(payload: MessagePayload) {
        val chatMessage = ChatMessage(
            id = payload.messageId, eventId = payload.eventId, senderId = payload.senderId,
            senderName = payload.senderName, content = payload.content,
            section = payload.section?.let { runCatching { CommentSection.valueOf(it) }.getOrNull() },
            parentId = payload.parentId, timestamp = payload.timestamp, status = MessageStatus.DELIVERED
        )
        _messages.update { messages ->
            if (messages.any { it.id == chatMessage.id }) messages else messages + chatMessage
        }
    }
    
    private fun handleIncomingTyping(payload: TypingPayload) {
        if (payload.userId == currentUserId) return
        val indicator = TypingIndicator(payload.userId, payload.userName, payload.chatId, payload.timestamp)
        _typingUsers.update { users -> users.filter { it.userId != payload.userId } + indicator }
        scope.launch {
            delay(TYPING_TIMEOUT)
            _typingUsers.update { users -> users.filter { it.userId != payload.userId } }
        }
    }
    
    private fun handleIncomingStoppedTyping(payload: TypingPayload) {
        if (payload.userId == currentUserId) return
        _typingUsers.update { users -> users.filter { it.userId != payload.userId } }
    }
    
    private fun handleIncomingReaction(payload: ReactionPayload) {
        _messages.update { messages -> messages.map { message ->
            if (message.id == payload.messageId) {
                when (payload.action) {
                    "add" -> message.copy(reactions = message.reactions + Reaction(payload.userId, payload.emoji, getCurrentTimestamp()))
                    "remove" -> message.copy(reactions = message.reactions.filterNot { it.userId == payload.userId && it.emoji == payload.emoji })
                    else -> message
                }
            } else message
        }}
    }
    
    private fun handleIncomingReadReceipt(payload: ReadReceiptPayload) {
        _messages.update { messages -> messages.map { message ->
            if (message.id == payload.messageId && !message.readBy.contains(payload.userId)) {
                message.copy(readBy = message.readBy + payload.userId, status = MessageStatus.READ)
            } else message
        }}
    }
    
    private fun handleIncomingPresence(payload: PresencePayload) {
        val participant = ChatParticipant(payload.userId, payload.userName, payload.isOnline, payload.lastSeen)
        _participants.update { participants -> participants.filter { it.userId != payload.userId } + participant }
    }
    
    /**
     * Get thread replies.
     */
    fun getThreadReplies(parentId: String): List<ChatMessage> = _messages.value.filter { it.parentId == parentId }
    
    /**
     * Get messages by section.
     */
    fun getMessagesBySection(section: CommentSection): List<ChatMessage> = _messages.value.filter { it.section == section }
    
    /**
     * Get reconnection state.
     */
    fun getReconnectionState(): ReconnectionManager.ConnectionState? = reconnectionManager?.getConnectionState()
    fun getRetryCount(): Int? = reconnectionManager?.getRetryCount()
    fun getCurrentDelay(): Long? = reconnectionManager?.getCurrentDelay()
    
    /**
     * Clear all messages.
     */
    fun clearMessages() {
        _messages.value = emptyList()
        _typingUsers.value = emptyList()
        _participants.value = emptyList()
    }
    
    companion object {
        private val TYPING_TIMEOUT = 3.seconds
        private const val DEFAULT_MESSAGE_LIMIT = 100
        
        private fun generateMessageId(): String = "msg-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(10000)}"
        private fun getCurrentTimestamp(): String = kotlinx.datetime.Clock.System.now().toString()
    }
}

/**
 * Data Transfer Object for reactions.
 */
@kotlinx.serialization.Serializable
private data class ReactionDto(val userId: String, val emoji: String, val timestamp: String)

/**
 * WebSocket connection state.
 */
enum class WebSocketConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

/**
 * Connection events for UI feedback.
 */
sealed class ConnectionEvent {
    data object Connected : ConnectionEvent()
    data object Disconnected : ConnectionEvent()
    data object Connecting : ConnectionEvent()
    data class MessageQueued(val messageId: String) : ConnectionEvent()
    data class QueueFlushed(val count: Int) : ConnectionEvent()
    data class Error(val message: String) : ConnectionEvent()
}
