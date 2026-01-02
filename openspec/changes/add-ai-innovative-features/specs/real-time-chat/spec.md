# Specification: Real-Time Chat Integration

> **Change ID**: `add-ai-innovative-features`
> **Capability**: `collaboration-management` (extension)
> **Type**: Enhancement with New Capability
> **Date**: 2026-01-01

## Summary

This specification defines the integration of a real-time chat system into the Wakeve application. It extends the existing collaboration capabilities by providing instant messaging, message threading, emoji reactions, typing indicators, and robust offline synchronization using WebSockets with SSE fallback.

## ADDED Requirements

### Requirement: Real-Time Messaging
**ID**: `chat-101`

The system SHALL enable real-time messaging between event participants.

**Business Rules:**
- Messages are delivered instantly via WebSocket (primary) with Server-Sent Events (SSE) fallback
- Messages persist in database for offline access
- Messages support text content and emoji reactions
- Users see read receipts and typing indicators
- Threaded conversations organized by section (Transport, Accommodation, Food, Equipment, Activities)
- Each message has senderId, timestamp, and optional parentId (for threads)

**Scenarios:**
- **Scenario:** Participants send real-time messages
  - **GIVEN** Event in ORGANIZING status, 2 participants online
  - **WHEN** User A sends "Who's bringing drinks?"
  - **THEN** User B receives message instantly (< 500ms)
  - **THEN** Both see typing indicators and read receipts

- **Scenario:** Offline to online sync
  - **GIVEN** User A sends message while offline
  - **WHEN** Connection restored
  - **THEN** User B receives message with sync timestamp
  - **THEN** Messages are visible on both devices

### Requirement: Message Threading
**ID**: `chat-102`

The system SHALL support threaded conversations with replies.

**Business Rules:**
- Users can reply to any message creating a thread
- Threads display visually nested (indentation)
- Each reply has parentId pointing to the original message
- Thread depth can be unlimited
- Reply notifications go to all participants in the thread

**Scenarios:**
- **Scenario:** Multi-participant discussion thread
  - **GIVEN** User A: "I propose we go to a restaurant"
  - **WHEN** User B replies "Which restaurant? Asian or Italian?"
  - **WHEN** User C replies "Italian, I love pasta!"
  - **THEN** Thread shows: A(original) → B(reply1) → C(reply2) with proper indentation
  - **THEN** All thread participants notified

### Requirement: Emoji Reactions
**ID**: `chat-103`

The system SHALL support emoji reactions on messages.

**Business Rules:**
- Users can react with any emoji (❤️ 👍 👎 😂 etc.)
- Reactions persist in database with userId and timestamp
- A user can react multiple times on the same message (updates counter)
- Reactions are collapsed (ex: "❤️ 3 people")
- Real-time delivery via WebSocket to all participants

**Scenarios:**
- **Scenario:** Quick reaction without typing
  - **GIVEN** User A posts "I'm bringing chips!"
  - **WHEN** Users B, C, D all react with 👍
  - **THEN** Real-time updates to all: "❤️ User B, 👍 User C, 👍 User D"
  - **THEN** Original message updated with reaction count

### Requirement: Typing Indicators
**ID**: `chat-104`

The system SHALL show when a user is typing a message.

**Business Rules:**
- Typing indicators are real-time via WebSocket
- Indicator disappears after 3 seconds of inactivity
- Shows typing user name: "Jean is typing..."
- Multiple users typing: "Jean, Marie and 2 others are typing..."

**Scenarios:**
- **GIVEN** User A sees "Jean is typing..."
- **WHEN** Jean sends message after 2 seconds
- **THEN** Typing indicator disappears immediately
- **THEN** Message appears with "Jean: ..."

### Requirement: Message Status & Read Receipts
**ID**: `chat-105`

The system SHALL track message delivery and read status.

**Business Rules:**
- Sent: Message created, waiting to send
- Delivered: Successfully sent to all participants
- Read: At least one participant has opened the chat
- Failed: Delivery failed, retry logic with exponential backoff
- Read receipts are visible (✓ read checkmarks)

**Scenarios:**
- **GIVEN** User A sends "When should we meet?"
- **WHEN** Network issue, message fails
- **THEN** Status: Failed (⚠️)
- **THEN** Auto-retry after 5 seconds
- **THEN** Status: Delivered (✓)
- **WHEN** User B reads message
- **THEN** Status: Read for User B only (User A sent it)

### Requirement: Offline Message Queue
**ID**: `chat-106`

The system SHALL queue messages when offline and send when connection restored.

**Business Rules:**
- Messages created while offline are queued in local SQLite
- Queue is ordered by timestamp
- On reconnection, all queued messages sent in order
- Duplicate detection based on messageId
- Conflict resolution: Local message wins (last-write-wins)

**Scenarios:**
- **GIVEN** User A offline, creates 3 messages
- **WHEN** Connection restored
- **THEN** All 3 messages sent in order
- **THEN** All participants receive messages
- **THEN** No duplicates or conflicts

## Data Models

### ChatMessage
```kotlin
@Serializable
data class ChatMessage(
    val id: String,                    // Unique messageId
    val eventId: String,               // Associated event
    val senderId: String,              // User who sent it
    val content: String,                // Text content
    val section: CommentSection?,   // Optional: TRANSPORT, FOOD, etc.
    val parentId: String?,               // For threaded replies
    val timestamp: String,             // ISO 8601
    val reactions: List<Reaction>,     // Emoji reactions
    val status: MessageStatus,         // SENT, DELIVERED, FAILED, READ
    val readBy: List<String>,          // userIds who read it
    val isOffline: Boolean = false      // Flag for offline queue
)

data class Reaction(
    val userId: String,
    val emoji: String,                  // "❤️", "👍", etc.
    val timestamp: String
)

enum class MessageStatus {
    SENT,
    DELIVERED,
    FAILED,
    READ
}
```

### TypingIndicator
```kotlin
@Serializable
data class TypingIndicator(
    val userId: String,
    val chatId: String,              // eventId
    val lastSeenTyping: String         // ISO timestamp
)

enum class CommentSection {
    TRANSPORT,
    ACCOMMODATION,
    FOOD,
    EQUIPMENT,
    ACTIVITIES
}
```

## API Changes

### WebSocket Endpoint
```
wss://api.wakeve.com/ws/events/{eventId}/chat
```

**Message Types:**
```json
{
  "type": "MESSAGE",
  "data": {
    "messageId": "msg-123",
    "eventId": "event-123",
    "senderId": "user-456",
    "content": "When should we meet?",
    "section": null,
    "parentId": null,
    "timestamp": "2026-01-01T14:30:00Z"
  }
}
```

**Typing Indicator:**
```json
{
  "type": "TYPING",
  "data": {
    "userId": "user-456",
    "chatId": "event-123",
    "timestamp": "2026-01-01T14:29:55Z"
  }
}
```

**Reaction:**
```json
{
  "type": "REACTION",
  "data": {
    "messageId": "msg-123",
    "userId": "user-456",
    "emoji": "❤️"
  }
}
```

**Read Receipt:**
```json
{
  "type": "READ_RECEIPT",
  "data": {
    "messageId": "msg-123",
    "userId": "user-456",
    "timestamp": "2026-01-01T14:31:00Z"
  }
}
```

### HTTP Fallback Endpoints
```
GET /api/events/{eventId}/chat/messages
GET /api/events/{eventId}/chat/messages/{messageId}
POST /api/events/{eventId}/chat/messages
POST /api/events/{eventId}/chat/messages/{messageId}/reactions
```

## Testing Requirements

### Unit Tests (shared)
- ChatServiceTest: 8 tests (message sending, queuing, offline handling)
- TypingIndicatorTest: 3 tests (start/stop/expiry)
- ReactionServiceTest: 4 tests (add, remove, collapse)
- MessageStatusTest: 5 tests (status transitions)

### Integration Tests
- RealTimeChatWorkflowTest: 6 tests (full chat workflow)
- OfflineSyncTest: 3 tests (queue order, conflict resolution)
- WebSocketConnectionTest: 2 tests (connect/disconnect)
- MultipleUsersTest: 2 tests (10 concurrent participants)

### Performance Tests
- MessageLatencyTest: 2 tests (< 500ms target)
- ScalabilityTest: 1 test (100 concurrent users, < 1000ms p99)

### Accessibility
- VoiceOver/TalkBack: All chat UI elements accessible
- High contrast mode: Messages and reactions readable

## Implementation Notes

### WebSocket Server
- Technology: Ktor WebSocket with Kotlin Coroutines
- Connection pooling: Support 10k+ concurrent connections
- Message ordering: Per-event queue per chat ID
- Backpressure: Slow consumers to prevent overload

### Offline Queue
- Database: SQLite queue in ChatMessage.isOffline flag
- Sync logic: Send on WebSocket reconnection
- Conflict: last-write-wins based on messageId

### Fallback Strategy
- WebSocket → SSE (Server-Sent Events)
- SSE → Polling (10s interval)
- Polling → None (user explicitly disables)

### Scalability
- Horizontal scaling: Multiple Ktor instances
- Load balancing: Round-robin per chat
- Database connection pooling: 10 connections per instance
