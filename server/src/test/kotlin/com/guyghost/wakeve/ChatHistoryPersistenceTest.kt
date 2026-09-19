package com.guyghost.wakeve

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.repository.DatabaseEventRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for proposal #43: messages sent over the WebSocket are
 * persisted server-side (same service as the REST route) so late joiners get
 * the history from the existing GET /chat/messages endpoint.
 */
class ChatHistoryPersistenceTest {

    private lateinit var database: WakeveDb
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "wakev-api"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "wakev-client"

    @Before
    fun setup() {
        DatabaseProvider.resetDatabase()
        database = DatabaseProvider.getDatabase(JvmTestDatabaseFactory())
    }

    @After
    fun teardown() {
        DatabaseProvider.resetDatabase()
    }

    private fun createTestJwt(userId: String): String =
        JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))

    private fun seedEvent() {
        val repository = DatabaseEventRepository(database)
        val event = Event(
            id = "event-chat-history",
            title = "Rio",
            description = "QA",
            organizerId = "organizer-1",
            participants = listOf("organizer-1"),
            proposedSlots = listOf(
                TimeSlot(
                    id = "slot-1",
                    start = "2026-12-05T12:00:00Z",
                    end = "2026-12-12T22:00:00Z",
                    timezone = "UTC",
                    timeOfDay = TimeOfDay.ALL_DAY
                )
            ),
            deadline = "2099-01-01T00:00:00Z",
            status = EventStatus.ORGANIZING,
            createdAt = "2026-09-19T07:00:00Z",
            updatedAt = "2026-09-19T07:00:00Z"
        )
        runBlocking { repository.createEvent(event).getOrThrow() }
    }

    private fun ApplicationTestBuilder.buildClient(): HttpClient =
        createClient { install(WebSockets) }

    private fun messageFrame(content: String): String =
        """{"type":"MESSAGE","data":{"eventId":"event-chat-history","userId":"organizer-1","userName":"forged-name","content":"$content"}}"""

    @Test
    fun `messages sent over websocket are persisted and served by history`() = testApplication {
        application { module(database) }
        seedEvent()
        val auth = "Bearer ${createTestJwt("organizer-1")}"

        val client = buildClient()
        client.webSocket(
            urlString = "/ws/events/event-chat-history/chat",
            request = {
                headers.append(HttpHeaders.Authorization, auth)
            }
        ) {
            send(Frame.Text(messageFrame("Premier message")))
            val echo1 = Json.parseToJsonElement((incoming.receive() as Frame.Text).readText())
            send(Frame.Text(messageFrame("Deuxième message")))
            val echo2 = Json.parseToJsonElement((incoming.receive() as Frame.Text).readText())

            assertEquals("Premier message", echo1.jsonObject["data"]!!.jsonObject["content"]!!.jsonPrimitive.content)
            assertEquals("Deuxième message", echo2.jsonObject["data"]!!.jsonObject["content"]!!.jsonPrimitive.content)
            assertTrue(
                echo1.jsonObject["data"]!!.jsonObject["messageId"]!!.jsonPrimitive.content.startsWith("msg_")
            )
        }

        val rows = database.chatMessagesQueries.selectMessagesByEvent("event-chat-history").executeAsList()
        println("[QA-DEBUG] persisted rows: ${rows.size} -> ${rows.map { it.content }}")
        println("[QA-DEBUG] rows detail: ${rows.map { it.id + "|" + it.sender_id + "|" + it.moderation_status }}")
        val response = client.get("/api/events/event-chat-history/chat/messages") {
            header(HttpHeaders.Authorization, auth)
        }
        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        val messages = Json.parseToJsonElement(response.bodyAsText()).jsonObject["messages"]!!.jsonArray
        assertEquals(2, messages.size, response.bodyAsText())
        // History contract: newest first (pagination-friendly DESC order).
        assertEquals("Deuxième message", messages[0].jsonObject["content"]!!.jsonPrimitive.content)
        assertEquals("Premier message", messages[1].jsonObject["content"]!!.jsonPrimitive.content)
        // forged display name from the payload must never be persisted
        assertTrue(!response.bodyAsText().contains("forged-name"), response.bodyAsText())
    }
}
