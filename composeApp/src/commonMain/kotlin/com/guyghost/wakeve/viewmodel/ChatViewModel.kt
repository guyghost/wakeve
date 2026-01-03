package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.chat.ChatMessage
import com.guyghost.wakeve.chat.ChatParticipant
import com.guyghost.wakeve.chat.ChatService
import com.guyghost.wakeve.chat.CommentSection
import com.guyghost.wakeve.chat.ConnectionEvent
import com.guyghost.wakeve.chat.TypingIndicator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Chat screen.
 * 
 * This ViewModel wraps the ChatService and exposes state flows for Compose UI.
 * It handles:
 * - Message list and threading
 * - Typing indicators
 * - Connection state
 * - Offline queue management
 * 
 * @property chatService The underlying chat service
 */
class ChatViewModel(
    private val chatService: ChatService
) : ViewModel() {
    
    // Messages state
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    // Typing indicators
    private val _typingUsers = MutableStateFlow<List<TypingIndicator>>(emptyList())
    val typingUsers: StateFlow<List<TypingIndicator>> = _typingUsers.asStateFlow()
    
    // Online participants
    private val _participants = MutableStateFlow<List<ChatParticipant>>(emptyList())
    val participants: StateFlow<List<ChatParticipant>> = _participants.asStateFlow()
    
    // Connection state
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    // Current user info (would come from auth service in production)
    private val _currentUser = MutableStateFlow<ChatParticipant?>(null)
    val currentUser: StateFlow<ChatParticipant?> = _currentUser.asStateFlow()
    
    // Selected section filter
    private val _selectedSection = MutableStateFlow<CommentSection?>(null)
    val selectedSection: StateFlow<CommentSection?> = _selectedSection.asStateFlow()
    
    // Connection events for UI feedback
    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>()
    val connectionEvents: Flow<ConnectionEvent> = _connectionEvents.asSharedFlow()
    
    // UI state for connection banner
    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Connected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    // Current event ID
    private var eventId: String? = null
    
    init {
        // Collect state from ChatService
        viewModelScope.launch {
            chatService.messages.collect { messages ->
                _messages.value = messages
            }
        }
        
        viewModelScope.launch {
            chatService.typingUsers.collect { typing ->
                _typingUsers.value = typing
            }
        }
        
        viewModelScope.launch {
            chatService.participants.collect { participants ->
                _participants.value = participants
            }
        }
        
        viewModelScope.launch {
            chatService.isConnected.collect { connected ->
                _isConnected.value = connected
                _connectionStatus.value = if (connected) {
                    ConnectionStatus.Connected
                } else {
                    ConnectionStatus.Disconnected
                }
            }
        }
        
        viewModelScope.launch {
            chatService.connectionEvents.collect { event ->
                handleConnectionEvent(event)
            }
        }
    }
    
    /**
     * Connect to chat for a specific event.
     * 
     * @param eventId The event to connect to
     * @param webSocketUrl Base WebSocket URL
     */
    fun connectToChat(eventId: String, webSocketUrl: String) {
        this.eventId = eventId
        chatService.connectToChat(eventId, webSocketUrl)
    }
    
    /**
     * Disconnect from the current chat.
     */
    fun disconnect() {
        chatService.disconnect()
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
        if (content.isBlank()) return
        
        chatService.sendMessage(content = content, section = section, parentId = parentId)
    }
    
    /**
     * Add an emoji reaction to a message.
     * 
     * @param messageId The message to react to
     * @param emoji The emoji to add
     */
    fun addReaction(messageId: String, emoji: String) {
        chatService.addReaction(messageId, emoji)
    }
    
    /**
     * Remove an emoji reaction from a message.
     * 
     * @param messageId The message to unreact from
     * @param emoji The emoji to remove
     */
    fun removeReaction(messageId: String, emoji: String) {
        chatService.removeReaction(messageId, emoji)
    }
    
    /**
     * Toggle reaction on a message (add if not present, remove if present).
     * 
     * @param messageId The message to toggle reaction on
     * @param emoji The emoji to toggle
     */
    fun toggleReaction(messageId: String, emoji: String) {
        val message = _messages.value.find { it.id == messageId } ?: return
        if (message.hasUserReacted(_currentUser.value?.userId ?: "", emoji)) {
            removeReaction(messageId, emoji)
        } else {
            addReaction(messageId, emoji)
        }
    }
    
    /**
     * Notify that user started typing.
     */
    fun startTyping() {
        chatService.startTyping()
    }
    
    /**
     * Notify that user stopped typing.
     */
    fun stopTyping() {
        chatService.stopTyping()
    }
    
    /**
     * Mark a message as read.
     * 
     * @param messageId The message that was read
     */
    fun markAsRead(messageId: String) {
        chatService.markAsRead(messageId)
    }
    
    /**
     * Mark all messages as read.
     */
    fun markAllAsRead() {
        chatService.markAllAsRead()
    }
    
    /**
     * Filter messages by section.
     * 
     * @param section The section to filter by, or null for all messages
     */
    fun setSectionFilter(section: CommentSection?) {
        _selectedSection.value = section
    }
    
    /**
     * Get messages filtered by the selected section.
     */
    fun getFilteredMessages(): List<ChatMessage> {
        val section = _selectedSection.value
        return if (section == null) {
            _messages.value
        } else {
            _messages.value.filter { it.section == section }
        }
    }
    
    /**
     * Get replies to a specific message (for threading).
     * 
     * @param parentId The parent message ID
     */
    fun getThreadReplies(parentId: String): List<ChatMessage> {
        return _messages.value.filter { it.parentId == parentId }
    }
    
    /**
     * Set the current user (would come from auth service).
     * 
     * @param userId The current user's ID
     * @param userName The current user's display name
     */
    fun setCurrentUser(userId: String, userName: String) {
        _currentUser.value = ChatParticipant(
            userId = userId,
            userName = userName,
            isOnline = true,
            lastSeen = null
        )
    }
    
    /**
     * Clear all data (when leaving chat).
     */
    fun clearData() {
        _messages.value = emptyList()
        _typingUsers.value = emptyList()
        _selectedSection.value = null
        chatService.clearMessages()
    }
    
    private fun handleConnectionEvent(event: ConnectionEvent) {
        viewModelScope.launch {
            when (event) {
                is ConnectionEvent.Connected -> {
                    _connectionStatus.value = ConnectionStatus.Connected
                }
                is ConnectionEvent.Disconnected -> {
                    _connectionStatus.value = ConnectionStatus.Disconnected
                }
                is ConnectionEvent.MessageQueued -> {
                    _connectionStatus.value = ConnectionStatus.Queued(_messages.value.count { it.isOffline })
                }
                is ConnectionEvent.QueueFlushed -> {
                    _connectionStatus.value = ConnectionStatus.Syncing(event.count)
                    delay(500)
                    _connectionStatus.value = ConnectionStatus.Connected
                }
                is ConnectionEvent.Error -> {
                    _connectionStatus.value = ConnectionStatus.Error(event.message)
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        chatService.disconnect()
    }
}

/**
 * Connection status for UI display.
 */
sealed class ConnectionStatus {
    data object Connected : ConnectionStatus()
    data object Disconnected : ConnectionStatus()
    data class Queued(val count: Int) : ConnectionStatus()
    data class Syncing(val count: Int) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}
