# ChatService Documentation

## Overview

The `ChatService` provides real-time messaging functionality for Wakeve events via WebSocket and HTTP endpoints. It supports:

- **Real-time messaging** via WebSocket (`/ws/events/{eventId}/chat`)
- **Message persistence** using SQLDelight for offline access
- **Message threading** with parent/reply relationships
- **Emoji reactions** with real-time updates
- **Typing indicators** that disappear after 3 seconds
- **Read receipts** with delivery status tracking

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                      ChatService                             │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────┐ │
│  │ WebSocket   │  │ HTTP Routes  │  │ SQLDelight         │ │
│  │ Broadcasting│  │ chatRoutes() │  │ Persistence        │ │
│  └─────────────┘  └──────────────┘  └────────────────────┘ │
│         │                │                   │              │
│         └────────────────┴───────────────────┘              │
│                          │                                  │
│              ┌───────────▼───────────┐                      │
│              │  EventChatConnections │                      │
│              │  (WebSocket Manager)  │                      │
│              └───────────────────────┘                      │
└─────────────────────────────────────────────────────────────┘
```

### File Structure

```
server/src/main/kotlin/com/guyghost/wakeve/
├── routing/
│   ├── ChatService.kt      # Main chat service implementation
│   └── ChatWebSocket.kt    # WebSocket route and connection manager
└── models/
    ├── ChatModels.kt       # Chat data models (ChatMessage, Reaction, etc.)
    └── ChatWebSocketDTOs.kt # WebSocket message DTOs
```

## Models

### ChatMessage
```kotlin
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
```

### MessageStatus
- `SENT` - Message sent to server
- `DELIVERED` - Message delivered to recipient
- `FAILED` - Message failed to send
- `READ` - Message read by recipient

### Reaction
```kotlin
data class Reaction(
    val userId: String,
    val emoji: String,
    val timestamp: String
)
```

### TypingIndicator
```kotlin
data class TypingIndicator(
    val userId: String,
    val chatId: String,
    val chatType: ChatType = ChatType.EVENT,
    val typingStatus: TypingStatus = TypingStatus.TYPING,
    val lastSeenTyping: String,
    val lastActivity: String
)
```

## WebSocket API

### Endpoint
```
ws://host:port/ws/events/{eventId}/chat
```

### Message Types

#### MESSAGE
```json
{
  "type": "MESSAGE",
  "data": {
    "messageId": "msg_uuid",
    "eventId": "event_uuid",
    "userId": "user_uuid",
    "userName": "John Doe",
    "content": "Hello everyone!",
    "section": "GENERAL",
    "parentMessageId": null,
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

#### TYPING
```json
{
  "type": "TYPING",
  "data": {
    "eventId": "event_uuid",
    "userId": "user_uuid",
    "userName": "John Doe",
    "content": "typing"
  }
}
```

#### REACTION
```json
{
  "type": "REACTION",
  "data": {
    "messageId": "msg_uuid",
    "eventId": "event_uuid",
    "userId": "user_uuid",
    "reaction": "👍"
  }
}
```

#### READ_RECEIPT
```json
{
  "type": "READ_RECEIPT",
  "data": {
    "messageId": "msg_uuid",
    "eventId": "event_uuid",
    "userId": "user_uuid",
    "timestamp": "2024-01-15T10:31:00Z"
  }
}
```

## HTTP API

### Base URL
```
/api/events/{eventId}/chat
```

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/messages` | Get all messages for an event |
| POST | `/messages` | Send a new message |
| GET | `/messages/{messageId}` | Get a specific message |
| PUT | `/messages/{messageId}` | Update a message |
| DELETE | `/messages/{messageId}` | Delete a message |
| GET | `/messages/{messageId}/replies` | Get thread replies |
| POST | `/messages/{messageId}/reactions` | Add a reaction |
| DELETE | `/messages/{messageId}/reactions` | Remove a reaction |
| POST | `/messages/{messageId}/read` | Mark as read |
| POST | `/messages/read-all` | Mark all as read |
| GET | `/typing` | Get typing users |
| POST | `/typing` | Set typing status |
| GET | `/unread-count` | Get unread count |
| GET | `/messages/section/{section}` | Get messages by section |

### Example: Send Message

```bash
curl -X POST /api/events/{eventId}/chat/messages \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Hello everyone!",
    "section": "GENERAL",
    "parentMessageId": null
  }'
```

### Example: Get Messages

```bash
curl /api/events/{eventId}/chat/messages?limit=50&offset=0 \
  -H "Authorization: Bearer {token}"
```

## Database Schema

### Tables

#### chat_message
- `id` - Primary key (TEXT)
- `event_id` - Foreign key to event
- `sender_id` - User who sent the message
- `sender_name` - Denormalized sender name
- `content` - Message text
- `section` - CommentSection (optional)
- `parent_message_id` - Thread parent (optional)
- `timestamp` - ISO 8601 UTC timestamp
- `status` - MessageStatus
- `reactions_json` - JSON array of reactions
- `read_by_json` - JSON array of user IDs who read

#### message_reaction
- `id` - Primary key
- `message_id` - Foreign key to chat_message
- `user_id` - User who reacted
- `emoji` - Emoji character
- `timestamp` - Reaction timestamp

#### message_read_status
- `id` - Primary key
- `message_id` - Foreign key to chat_message
- `user_id` - User who read
- `read_at` - Read timestamp

#### typing_indicator
- `user_id` - User who is typing
- `chat_id` - Event ID
- `chat_type` - ChatType (EVENT, DIRECT, THREAD)
- `typing_status` - TypingStatus (TYPING, STOPPED, IDLE)
- `last_activity` - Last activity timestamp

## Integration with Application.kt

```kotlin
fun Application.module(...) {
    val chatService = ChatService(database)
    
    routing {
        chatWebSocketRoute()
        
        authenticate("auth-jwt") {
            route("/api") {
                chatRoutes(chatService)
            }
        }
    }
}
```

## Usage Example

```kotlin
val chatService = ChatService(database)

// Send a message
val message = chatService.sendMessage(
    eventId = "event-123",
    userId = "user-456",
    userName = "John Doe",
    content = "Hello everyone!",
    section = CommentSection.GENERAL,
    parentMessageId = null
)

// Add a reaction
chatService.addReaction(
    eventId = "event-123",
    messageId = message.id,
    userId = "user-456",
    emoji = "👍"
)

// Mark as read
chatService.markAsRead(
    eventId = "event-123",
    messageId = message.id,
    userId = "user-789"
)

// Get messages
val messages = chatService.getMessages(eventId = "event-123")

// Get typing users
val typingUsers = chatService.getTypingUsers(eventId = "event-123")
```

## Offline Support

The ChatService supports offline-first functionality:

1. **Message Creation Offline**: Messages created while offline are stored with `isOffline = true`
2. **Sync on Reconnection**: Offline messages are synced when connection is restored
3. **Typing Indicators**: Stored in-memory with automatic cleanup after 3 seconds of inactivity

## Performance Considerations

- Messages are paginated with configurable limit (default: 100)
- Reactions and read receipts are denormalized for fast reads
- Typing indicators use in-memory storage with automatic expiration
- Database indexes on `event_id`, `timestamp`, and `sender_id`

## Error Handling

All service methods throw `ChatServiceException` on failure:

```kotlin
class ChatServiceException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
```

## Testing

Run server tests:
```bash
./gradlew :server:test
```

Run chat-specific tests:
```bash
./gradlew :server:test --tests "*Chat*"
```
