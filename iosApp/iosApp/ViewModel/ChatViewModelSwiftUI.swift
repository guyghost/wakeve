//
//  ChatViewModelSwiftUI.swift
//  iosApp
//
//  ChatViewModelSwiftUI - SwiftUI wrapper for ChatService.
//  NOTE: This file is temporarily disabled as ChatService requires additional
//  configuration (database, reconnectionManager, webSocketClient) that will be
//  implemented in Phase 4. The ChatService implementation is stubbed out.
//
//  This class bridges the Kotlin ChatService with SwiftUI by:
//  - Converting Kotlin StateFlows to Swift@Published properties
//  - Providing async methods for SwiftUI actions
//  - Managing the lifecycle of the chat connection
//
//  TODO: Re-enable when Phase 4 ChatService implementation is complete
//

//import SwiftUI
//import Combine
//import Shared
//
//@MainActor
//class ChatViewModelSwiftUI: ObservableObject {
//    // MARK: - Published Properties
//
//    @Published var messages: [ChatMessage] = []
//    @Published var typingUsers: [TypingIndicator] = []
//    @Published var participants: [ChatParticipant] = []
//    @Published var isConnected: Bool = false
//    @Published var selectedSection: CommentSection? = nil
//    @Published var connectionStatus: ConnectionStatus = .disconnected
//
//    // MARK: - Properties
//
//    private var chatService: ChatService?
//    private var cancellables = Set<AnyCancellable>()
//    private let currentUserId: String
//    private let currentUserName: String
//    private var eventId: String?
//
//    // MARK: - Initialization
//
//    init(currentUserId: String = "user-1", currentUserName: String = "Jean") {
//        self.currentUserId = currentUserId
//        self.currentUserName = currentUserName
//
//        // Initialize the chat service
//        self.chatService = ChatService(
//            currentUserId: currentUserId,
//            currentUserName: currentUserName
//        )
//
//        // Set up bindings from Kotlin StateFlows
//        setupBindings()
//    }
//
//    // MARK: - Private Methods
//
//    private func setupBindings() {
//        guard let chatService = chatService else { return }
//
//        // Bind messages
//        chatService.messages
//            .receive(on: DispatchQueue.main)
//            .sink { [weak self] messages in
//                self?.messages = messages
//            }
//            .store(in: &cancellables)
//
//        // Bind typing users
//        chatService.typingUsers
//            .receive(on: DispatchQueue.main)
//            .sink { [weak self] users in
//                self?.typingUsers = users
//            }
//            .store(in: &cancellables)
//
//        // Bind participants
//        chatService.participants
//            .receive(on: DispatchQueue.main)
//            .sink { [weak self] participants in
//                self?.participants = participants
//            }
//            .store(in: &cancellables)
//
//        // Bind connection state
//        chatService.isConnected
//            .receive(on: DispatchQueue.main)
//            .sink { [weak self] connected in
//                self?.isConnected = connected
//                self?.connectionStatus = connected ? .connected : .disconnected
//            }
//            .store(in: &cancellables)
//    }
//
//    // MARK: - Public Methods
//
//    /**
//     * Connect to a chat room for a specific event.
//     */
//    func connectToChat(eventId: String, webSocketUrl: String = "wss://api.wakeve.com/ws") {
//        self.eventId = eventId
//        chatService?.connectToChat(eventId: eventId, webSocketUrl: webSocketUrl)
//    }
//
//    /**
//     * Disconnect from the current chat.
//     */
//    func disconnect() {
//        chatService?.disconnect()
//    }
//
//    /**
//     * Send a new message.
//     */
//    func sendMessage(content: String, section: CommentSection? = nil, parentId: String? = nil) {
//        guard !content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
//        chatService?.sendMessage(
//            content: content,
//            section: section,
//            parentId: parentId
//        )
//    }
//
//    /**
//     * Add or toggle a reaction on a message.
//     */
//    func toggleReaction(messageId: String, emoji: String) {
//        // Check if user has already reacted with this emoji
//        let hasReacted = messages.first { $0.id == messageId }?.reactions?
//            .contains { reaction in
//                reaction.userId == currentUserId && reaction.emoji == emoji
//            } ?? false
//
//        if hasReacted {
//            chatService?.removeReaction(messageId: messageId, emoji: emoji)
//        } else {
//            chatService?.addReaction(messageId: messageId, emoji: emoji)
//        }
//    }
//
//    /**
//     * Remove a reaction from a message.
//     */
//    func removeReaction(messageId: String, emoji: String) {
//        chatService?.removeReaction(messageId: messageId, emoji: emoji)
//    }
//
//    /**
//     * Notify that user started typing.
//     */
//    func startTyping() {
//        chatService?.startTyping()
//    }
//
//    /**
//     * Notify that user stopped typing.
//     */
//    func stopTyping() {
//        chatService?.stopTyping()
//    }
//
//    /**
//     * Mark a message as read.
//     */
//    func markAsRead(messageId: String) {
//        chatService?.markAsRead(messageId: messageId)
//    }
//
//    /**
//     * Mark all messages as read.
//     */
//    func markAllAsRead() {
//        chatService?.markAllAsRead()
//    }
//
//    /**
//     * Filter messages by section.
//     */
//    func setSectionFilter(_ section: CommentSection?) {
//        selectedSection = section
//    }
//
//    /**
//     * Get filtered messages based on selected section.
//     */
//    func getFilteredMessages() -> [ChatMessage] {
//        guard let section = selectedSection else {
//            return messages
//        }
//        return messages.filter { $0.section == section }
//    }
//
//    /**
//     * Get replies to a specific message.
//     */
//    func getThreadReplies(parentId: String) -> [ChatMessage] {
//        return messages.filter { $0.parentId == parentId }
//    }
//
//    /**
//     * Clear all data (when leaving chat).
//     */
//    func clearData() {
//        messages = []
//        typingUsers = []
//        participants = []
//        selectedSection = nil
//        chatService?.clearMessages()
//    }
//
//    // MARK: - Deinit
//
//    deinit {
//        cancellables.removeAll()
//        chatService?.disconnect()
//    }
//}
//
//// MARK: - Connection Status
//
//enum ConnectionStatus {
//    case connected
//    case disconnected
//    case queued(count: Int)
//    case syncing(count: Int)
//    case error(message: String)
//}

