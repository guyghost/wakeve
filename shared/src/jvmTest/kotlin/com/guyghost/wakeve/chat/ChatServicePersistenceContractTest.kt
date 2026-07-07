package com.guyghost.wakeve.chat

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ChatServicePersistenceContractTest {

    private lateinit var database: WakeveDb

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
        seedEvent("event-1")
    }

    @Test
    fun `incoming websocket message is persisted to sqlite`() = runTest {
        val service = ChatService(
            currentUserId = "user-local",
            currentUserName = "Local User",
            database = database
        )

        service.handleIncomingMessage(
            WebSocketMessage(
                type = WebSocketMessageType.MESSAGE,
                data = Json.encodeToString(
                    MessagePayload(
                        messageId = "message-1",
                        eventId = "event-1",
                        senderId = "user-remote",
                        senderName = "Remote User",
                        content = "Meet at 19:00",
                        section = CommentSection.GENERAL.name,
                        parentId = null,
                        timestamp = "2026-06-20T18:00:00Z"
                    )
                )
            )
        )

        val stored = awaitMessage("message-1")

        assertEquals("Meet at 19:00", stored.content)
        assertEquals("DELIVERED", stored.status)
        assertEquals(CommentSection.GENERAL.name, stored.section)
    }

    @Test
    fun `offline message is persisted to sqlite queue`() = runTest {
        val service = ChatService(
            currentUserId = "user-local",
            currentUserName = "Local User",
            database = database
        )
        service.connectToChat("event-1", "wss://example.invalid/chat")

        service.sendMessage("I am offline but this must survive")

        val stored = awaitMessageWithContent("I am offline but this must survive")

        assertEquals("user-local", stored.sender_id)
        assertEquals(1L, stored.is_offline)
        assertEquals("SENT", stored.status)
    }

    private suspend fun awaitMessage(messageId: String): com.guyghost.wakeve.Chat_message {
        repeat(20) {
            database.chatMessagesQueries.selectMessageById(messageId).executeAsOneOrNull()?.let {
                return it
            }
            delay(50)
        }
        return assertNotNull(database.chatMessagesQueries.selectMessageById(messageId).executeAsOneOrNull())
    }

    private suspend fun awaitMessageWithContent(content: String): com.guyghost.wakeve.Chat_message {
        repeat(20) {
            database.chatMessagesQueries.selectMessagesByEvent("event-1").executeAsList()
                .firstOrNull { it.content == content }
                ?.let { return it }
            delay(50)
        }
        return assertNotNull(
            database.chatMessagesQueries.selectMessagesByEvent("event-1").executeAsList()
                .firstOrNull { it.content == content }
        )
    }

    private fun seedEvent(eventId: String) {
        database.eventQueries.insertEvent(
            id = eventId,
            organizerId = "organizer-1",
            title = "Event",
            description = "Description",
            status = "POLLING",
            deadline = "2026-06-30T00:00:00Z",
            createdAt = "2026-06-20T00:00:00Z",
            updatedAt = "2026-06-20T00:00:00Z",
            version = 1,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null,
            isSample = 0L
        )
    }
}
