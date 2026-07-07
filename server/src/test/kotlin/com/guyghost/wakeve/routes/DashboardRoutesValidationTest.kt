package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DashboardRoutesValidationTest {
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
    fun `dashboard events are paginated and scoped to organizer`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val token = createTestJwt("dashboard-organizer")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        repeat(4) { index ->
            seedEvent(
                eventRepository = eventRepository,
                eventId = "dashboard-event-$index",
                organizerId = "dashboard-organizer",
                createdAt = "2026-06-13T10:0$index:00Z"
            )
        }
        seedEvent(
            eventRepository = eventRepository,
            eventId = "hidden-dashboard-event",
            organizerId = "other-organizer",
            createdAt = "2026-06-13T10:05:00Z"
        )

        application { module(database, eventRepository) }

        val response = client.get("/api/dashboard/events?limit=2&offset=1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val responseText = response.bodyAsText()
        val responseBody = json.parseToJsonElement(responseText).jsonObject
        val eventIds = responseBody
            .getValue("events")
            .jsonArray
            .map { it.jsonObject.getValue("eventId").jsonPrimitive.content }

        assertEquals(HttpStatusCode.OK, response.status, responseText)
        assertEquals(4, responseBody.getValue("totalCount").jsonPrimitive.int)
        assertEquals(2, responseBody.getValue("limit").jsonPrimitive.int)
        assertEquals(1, responseBody.getValue("offset").jsonPrimitive.int)
        assertEquals(listOf("dashboard-event-2", "dashboard-event-1"), eventIds)
    }

    @Test
    fun `dashboard events reject invalid pagination parameters`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val token = createTestJwt("dashboard-organizer")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        application { module(database, eventRepository) }

        listOf(
            "/api/dashboard/events?limit=0",
            "/api/dashboard/events?limit=101",
            "/api/dashboard/events?limit=abc",
            "/api/dashboard/events?offset=-1",
            "/api/dashboard/events?offset=10001"
        ).forEach { path ->
            val response = client.get(path) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        }
    }

    private fun seedEvent(
        eventRepository: DatabaseEventRepository,
        eventId: String,
        organizerId: String,
        createdAt: String
    ) {
        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = eventId,
                    title = "Dashboard $eventId",
                    description = "Dashboard route validation event",
                    organizerId = organizerId,
                    participants = emptyList(),
                    proposedSlots = listOf(
                        TimeSlot(
                            id = "slot-$eventId",
                            start = "2026-07-01T10:00:00Z",
                            end = "2026-07-01T12:00:00Z",
                            timezone = "UTC",
                            timeOfDay = TimeOfDay.SPECIFIC
                        )
                    ),
                    deadline = "2026-06-20T00:00:00Z",
                    status = EventStatus.CONFIRMED,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                    eventType = EventType.OTHER
                )
            ).getOrThrow()
        }
    }

    private fun createTestJwt(userId: String): String =
        JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withClaim("sessionId", "test-session-$userId")
            .withClaim("permissions", listOf("READ", "WRITE"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))
}
