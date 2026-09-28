package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.module
import com.guyghost.wakeve.repository.DatabaseEventRepository
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for QA BUG-A (API multi-user QA 2026-09-27): event comments
 * must only be readable/writable by the event organizer or its participants.
 */
class CommentRoutesMembershipTest {
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "wakev-api"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "wakev-client"
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setup() {
        DatabaseProvider.resetDatabase()
    }

    @AfterTest
    fun teardown() {
        DatabaseProvider.resetDatabase()
    }

    @Test
    fun `non member cannot post or read event comments`() = testApplication {
        val fixture = createFixture("outsider")
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(database = fixture.database, eventRepository = fixture.eventRepository) }

        val participantPost = client.post("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer ${jwt(fixture.participantId)}")
            contentType(ContentType.Application.Json)
            setBody(commentBody("Participant comment", fixture.participantId))
        }
        assertEquals(HttpStatusCode.Created, participantPost.status, participantPost.bodyAsText())
        val commentId = json.parseToJsonElement(participantPost.bodyAsText()).jsonObject
            .getValue("id").jsonPrimitive.content

        val outsiderPost = client.post("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer ${jwt(fixture.outsiderId)}")
            contentType(ContentType.Application.Json)
            setBody(commentBody("je m incruste", fixture.outsiderId))
        }
        assertEquals(HttpStatusCode.Forbidden, outsiderPost.status, outsiderPost.bodyAsText())
        assertEquals(1, fixture.database.commentQueries.countCommentsByEvent(fixture.eventId).executeAsOne())

        val outsiderReads = listOf(
            "/api/events/${fixture.eventId}/comments",
            "/api/events/${fixture.eventId}/comments?section=GENERAL",
            "/api/events/${fixture.eventId}/comments/$commentId",
            "/api/events/${fixture.eventId}/comments/statistics",
            "/api/events/${fixture.eventId}/comments/top-contributors",
            "/api/events/${fixture.eventId}/comments/recent?since=2000-01-01T00:00:00Z",
            "/api/events/${fixture.eventId}/comments/sections"
        ).associateWith { path ->
            client.get(path) { header(HttpHeaders.Authorization, "Bearer ${jwt(fixture.outsiderId)}") }.status
        }
        outsiderReads.forEach { (path, status) ->
            assertEquals(HttpStatusCode.Forbidden, status, "Outsider read of $path must be forbidden")
        }

        val organizerRead = client.get("/api/events/${fixture.eventId}/comments?threaded=false") {
            header(HttpHeaders.Authorization, "Bearer ${jwt(fixture.organizerId)}")
        }
        assertEquals(HttpStatusCode.OK, organizerRead.status, organizerRead.bodyAsText())
        assertEquals(1, json.parseToJsonElement(organizerRead.bodyAsText()).jsonArray.size)
    }

    @Test
    fun `comment on unknown event is forbidden rather than a server error`() = testApplication {
        val fixture = createFixture("unknown-event")
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(database = fixture.database, eventRepository = fixture.eventRepository) }

        val response = client.post("/api/events/does-not-exist/comments") {
            header(HttpHeaders.Authorization, "Bearer ${jwt(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(commentBody("Hello", fixture.organizerId))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status, response.bodyAsText())
    }

    @Test
    fun `blank comment content is rejected with bad request`() = testApplication {
        val fixture = createFixture("blank")
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(database = fixture.database, eventRepository = fixture.eventRepository) }

        val response = client.post("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer ${jwt(fixture.participantId)}")
            contentType(ContentType.Application.Json)
            setBody(commentBody("", fixture.participantId))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        assertEquals(0, fixture.database.commentQueries.countCommentsByEvent(fixture.eventId).executeAsOne())
    }

    private fun createFixture(suffix: String): Fixture {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val eventId = "comment-member-event-$suffix"
        val organizerId = "comment-organizer-$suffix"
        val participantId = "comment-participant-$suffix"
        val outsiderId = "comment-outsider-$suffix"
        listOf(organizerId, participantId, outsiderId).forEach { insertUser(database, it) }
        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = eventId,
                    title = "Comment membership $suffix",
                    description = "Comment membership test",
                    organizerId = organizerId,
                    participants = emptyList(),
                    proposedSlots = emptyList(),
                    deadline = "2026-12-20T00:00:00Z",
                    status = EventStatus.DRAFT,
                    createdAt = "2026-09-27T10:00:00Z",
                    updatedAt = "2026-09-27T10:00:00Z",
                    eventType = EventType.OTHER
                )
            ).getOrThrow()
            eventRepository.addParticipant(eventId, participantId).getOrThrow()
        }
        return Fixture(database, eventRepository, eventId, organizerId, participantId, outsiderId)
    }

    private fun insertUser(database: WakeveDb, userId: String) {
        database.userQueries.insertUser(
            id = userId,
            provider_id = "provider-$userId",
            email = "$userId@example.test",
            name = userId,
            avatar_url = null,
            provider = "google",
            role = "USER",
            created_at = "2026-09-27T10:00:00Z",
            updated_at = "2026-09-27T10:00:00Z"
        )
    }

    private fun commentBody(content: String, authorId: String): String =
        """{"section":"GENERAL","content":"$content","authorId":"$authorId","authorName":"x"}"""

    private fun jwt(userId: String): String =
        JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withClaim("sessionId", "test-session-$userId")
            .withClaim("permissions", listOf("READ", "WRITE"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))

    private data class Fixture(
        val database: WakeveDb,
        val eventRepository: DatabaseEventRepository,
        val eventId: String,
        val organizerId: String,
        val participantId: String,
        val outsiderId: String
    )
}
