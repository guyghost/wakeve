package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
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

class EventRoutesAuthValidationTest {
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
    fun `create event binds organizer to authenticated user not request body`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val token = createTestJwt("real-organizer")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        application { module(database = database) }

        val response = client.post("/api/events") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(createEventBody(organizerId = "spoofed-organizer"))
        }
        val responseText = response.bodyAsText()
        val responseBody = json.parseToJsonElement(responseText).jsonObject
        val eventId = responseBody.getValue("id").jsonPrimitive.content
        val storedEvent = database.eventQueries.selectById(eventId).executeAsOne()

        assertEquals(HttpStatusCode.Created, response.status, responseText)
        assertEquals("real-organizer", responseBody.getValue("organizerId").jsonPrimitive.content)
        assertEquals("real-organizer", storedEvent.organizerId)
    }

    @Test
    fun `event list only returns events accessible to authenticated user`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val token = createTestJwt("visible-user")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        seedEvent(database, eventRepository, "owned-event", organizerId = "visible-user")
        seedEvent(database, eventRepository, "joined-event", organizerId = "other-organizer")
        database.participantQueries.insertParticipant(
            id = "participant-joined-visible-user",
            eventId = "joined-event",
            userId = "visible-user",
            role = "PARTICIPANT",
            hasValidatedDate = 0,
            joinedAt = "2026-06-20T10:00:00Z",
            updatedAt = "2026-06-20T10:00:00Z"
        )
        seedEvent(database, eventRepository, "hidden-event", organizerId = "stranger-user")

        application { module(database = database, eventRepository = eventRepository) }

        val response = client.get("/api/events") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val responseText = response.bodyAsText()
        val eventIds = json.parseToJsonElement(responseText)
            .jsonObject
            .getValue("events")
            .jsonArray
            .map { it.jsonObject.getValue("id").jsonPrimitive.content }

        assertEquals(HttpStatusCode.OK, response.status, responseText)
        assertEquals(listOf("owned-event", "joined-event").toSet(), eventIds.toSet())
    }

    @Test
    fun `event details are visible to organizer`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val token = createTestJwt("detail-organizer")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        seedEvent(database, eventRepository, "organizer-visible-event", organizerId = "detail-organizer")

        application { module(database = database, eventRepository = eventRepository) }

        val response = client.get("/api/events/organizer-visible-event") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val responseText = response.bodyAsText()
        val eventId = json.parseToJsonElement(responseText)
            .jsonObject
            .getValue("id")
            .jsonPrimitive
            .content

        assertEquals(HttpStatusCode.OK, response.status, responseText)
        assertEquals("organizer-visible-event", eventId)
    }

    @Test
    fun `event details are visible to participant`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val token = createTestJwt("detail-participant")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        seedEvent(database, eventRepository, "participant-visible-event", organizerId = "detail-organizer")
        insertParticipant(database, eventId = "participant-visible-event", userId = "detail-participant")

        application { module(database = database, eventRepository = eventRepository) }

        val response = client.get("/api/events/participant-visible-event") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val responseText = response.bodyAsText()
        val eventId = json.parseToJsonElement(responseText)
            .jsonObject
            .getValue("id")
            .jsonPrimitive
            .content

        assertEquals(HttpStatusCode.OK, response.status, responseText)
        assertEquals("participant-visible-event", eventId)
    }

    @Test
    fun `event details are hidden from non participant`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val token = createTestJwt("non-member")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        seedEvent(database, eventRepository, "hidden-detail-event", organizerId = "detail-organizer")

        application { module(database = database, eventRepository = eventRepository) }

        val response = client.get("/api/events/hidden-detail-event") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val responseText = response.bodyAsText()

        assertEquals(HttpStatusCode.NotFound, response.status, responseText)
        assertEquals(false, responseText.contains("hidden-detail-event"), responseText)
    }

    private fun createEventBody(organizerId: String): String =
        """
        {
          "title": "Trusted Organizer Event",
          "description": "Body organizer id must not be trusted",
          "organizerId": "$organizerId",
          "deadline": "2026-07-20T00:00:00Z",
          "proposedSlots": [
            {
              "id": "slot-auth-validation",
              "start": "2026-08-01T10:00:00Z",
              "end": "2026-08-01T12:00:00Z",
              "timezone": "UTC",
              "timeOfDay": "SPECIFIC"
            }
          ]
        }
        """.trimIndent()

    private fun seedEvent(
        database: WakeveDb,
        eventRepository: DatabaseEventRepository,
        eventId: String,
        organizerId: String
    ) {
        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = eventId,
                    title = "Event $eventId",
                    description = "Auth validation event",
                    organizerId = organizerId,
                    participants = emptyList(),
                    proposedSlots = listOf(
                        TimeSlot(
                            id = "slot-$eventId",
                            start = "2026-08-01T10:00:00Z",
                            end = "2026-08-01T12:00:00Z",
                            timezone = "UTC",
                            timeOfDay = TimeOfDay.SPECIFIC
                        )
                    ),
                    deadline = "2026-07-20T00:00:00Z",
                    status = EventStatus.DRAFT,
                    createdAt = "2026-06-20T10:00:00Z",
                    updatedAt = "2026-06-20T10:00:00Z",
                    eventType = EventType.OTHER
                )
            ).getOrThrow()
        }
        insertUser(database, organizerId)
    }

    private fun insertParticipant(database: WakeveDb, eventId: String, userId: String) {
        insertUser(database, userId)
        database.participantQueries.insertParticipant(
            id = "participant-$eventId-$userId",
            eventId = eventId,
            userId = userId,
            role = "PARTICIPANT",
            hasValidatedDate = 0,
            joinedAt = "2026-06-20T10:00:00Z",
            updatedAt = "2026-06-20T10:00:00Z"
        )
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
            created_at = "2026-06-20T10:00:00Z",
            updated_at = "2026-06-20T10:00:00Z"
        )
    }

    private fun createTestJwt(userId: String): String =
        JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withClaim("role", "USER")
            .withClaim("sessionId", "test-session-$userId")
            .withClaim("permissions", listOf("READ", "WRITE"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))
}
