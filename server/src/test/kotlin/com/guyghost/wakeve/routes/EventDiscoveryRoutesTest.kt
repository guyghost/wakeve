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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventDiscoveryRoutesTest {
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
    fun `event discovery endpoints read query parameters`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val token = createTestJwt("discovery-user")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        seedEvent(
            database,
            eventRepository,
            "event-alpine",
            "Alpine Team Retreat",
            EventType.TEAM_BUILDING,
            organizerId = "discovery-user"
        )
        seedEvent(database, eventRepository, "event-beach", "Beach Birthday", EventType.BIRTHDAY)
        seedEvent(database, eventRepository, "event-museum", "Museum Night", EventType.CULTURAL_EVENT)
        database.potentialLocationQueries.insertLocation(
            id = "location-alpine",
            eventId = "event-alpine",
            name = "Chamonix",
            locationType = "CITY",
            address = "Chamonix, France",
            coordinates = """{"latitude":45.9237,"longitude":6.8694}""",
            createdAt = "2026-06-13T10:00:00Z"
        )

        application { module(database, eventRepository) }

        val search = client.get("/api/events/search?q=Alpine&limit=1&offset=0") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val searchBodyText = search.bodyAsText()
        val searchBody = json.parseToJsonElement(searchBodyText).jsonObject

        val nearby = client.get("/api/events/nearby?lat=45.9237&lon=6.8694&radius=5&limit=1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val nearbyBodyText = nearby.bodyAsText()
        val nearbyBody = json.parseToJsonElement(nearbyBodyText).jsonObject

        val trending = client.get("/api/events/trending?limit=1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val trendingBody = json.parseToJsonElement(trending.bodyAsText()).jsonObject

        val recommended = client.get("/api/events/recommended/discovery-user?limit=1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val recommendedBody = json.parseToJsonElement(recommended.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, search.status, searchBodyText)
        assertEquals(1, searchBody.getValue("totalCount").jsonPrimitive.int)
        assertEquals(1, searchBody.getValue("limit").jsonPrimitive.int)
        assertTrue(searchBodyText.contains("Alpine Team Retreat"), searchBodyText)
        assertFalse(searchBodyText.contains("Beach Birthday"), searchBodyText)

        assertEquals(HttpStatusCode.OK, nearby.status, nearbyBodyText)
        assertEquals(1, nearbyBody.getValue("events").jsonArray.size)
        assertEquals(45.9237, nearbyBody.getValue("centerLat").jsonPrimitive.double)
        assertEquals(6.8694, nearbyBody.getValue("centerLon").jsonPrimitive.double)

        assertEquals(HttpStatusCode.OK, trending.status, trending.bodyAsText())
        assertEquals(1, trendingBody.getValue("events").jsonArray.size)

        assertEquals(HttpStatusCode.OK, recommended.status, recommended.bodyAsText())
        assertEquals(1, recommendedBody.getValue("events").jsonArray.size)
    }

    @Test
    fun `event discovery endpoints hide events outside authenticated user scope`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val token = createTestJwt("discovery-user")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        seedEvent(database, eventRepository, "event-visible", "Visible Alpine Retreat", EventType.TEAM_BUILDING)
        seedEvent(
            database,
            eventRepository,
            "event-hidden",
            "Hidden Alpine Strategy Offsite",
            EventType.TEAM_BUILDING,
            includeDiscoveryUser = false
        )
        database.potentialLocationQueries.insertLocation(
            id = "location-hidden",
            eventId = "event-hidden",
            name = "Hidden Chamonix Chalet",
            locationType = "CITY",
            address = "Chamonix, France",
            coordinates = """{"latitude":45.9237,"longitude":6.8694}""",
            createdAt = "2026-06-13T10:00:00Z"
        )

        application { module(database, eventRepository) }

        val search = client.get("/api/events/search?q=Hidden&limit=10&offset=0") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val searchText = search.bodyAsText()
        val searchBody = json.parseToJsonElement(searchText).jsonObject

        val nearby = client.get("/api/events/nearby?lat=45.9237&lon=6.8694&radius=5&limit=10") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val nearbyText = nearby.bodyAsText()

        val trending = client.get("/api/events/trending?limit=10") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val trendingText = trending.bodyAsText()

        val otherUserRecommended = client.get("/api/events/recommended/other-user?limit=1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, search.status, searchText)
        assertEquals(0, searchBody.getValue("totalCount").jsonPrimitive.int)
        assertFalse(searchText.contains("Hidden Alpine Strategy Offsite"), searchText)

        assertEquals(HttpStatusCode.OK, nearby.status, nearbyText)
        assertFalse(nearbyText.contains("Hidden Alpine Strategy Offsite"), nearbyText)

        assertEquals(HttpStatusCode.OK, trending.status, trendingText)
        assertFalse(trendingText.contains("Hidden Alpine Strategy Offsite"), trendingText)

        assertEquals(HttpStatusCode.Forbidden, otherUserRecommended.status, otherUserRecommended.bodyAsText())
    }

    private fun seedEvent(
        database: WakeveDb,
        eventRepository: DatabaseEventRepository,
        eventId: String,
        title: String,
        eventType: EventType,
        includeDiscoveryUser: Boolean = true,
        organizerId: String = "organizer-discovery"
    ) {
        val now = "2026-06-13T10:00:00Z"
        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = eventId,
                    title = title,
                    description = "Discovery route test event",
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
                    createdAt = now,
                    updatedAt = now,
                    eventType = eventType
                )
            ).getOrThrow()
        }

        if (includeDiscoveryUser && organizerId != "discovery-user") {
            database.participantQueries.insertParticipant(
                id = "participant-discovery-$eventId",
                eventId = eventId,
                userId = "discovery-user",
                role = "PARTICIPANT",
                hasValidatedDate = 1,
                joinedAt = now,
                updatedAt = now
            )
        }

        database.participantQueries.insertParticipant(
            id = "participant-$eventId",
            eventId = eventId,
            userId = "participant-$eventId",
            role = "PARTICIPANT",
            hasValidatedDate = 1,
            joinedAt = now,
            updatedAt = now
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
