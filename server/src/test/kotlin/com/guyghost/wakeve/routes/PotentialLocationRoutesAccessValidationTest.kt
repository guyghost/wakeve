package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.module
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.PotentialLocationRepository
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
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

class PotentialLocationRoutesAccessValidationTest {
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
    fun `potential locations are readable by members but mutable only by organizer`() = testApplication {
        val fixture = createPotentialLocationFixture("access")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        application {
            module(
                database = fixture.database,
                eventRepository = fixture.eventRepository,
                locationRepository = fixture.locationRepository
            )
        }

        val participantRead = client.get("/api/events/${fixture.eventId}/potential-locations") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.participantId)}")
        }
        val participantText = participantRead.bodyAsText()
        val participantLocations = json.parseToJsonElement(participantText)
            .jsonObject
            .getValue("locations")
            .jsonArray
        val nonMemberRead = client.get("/api/events/${fixture.eventId}/potential-locations") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.nonMemberId)}")
        }
        val participantCreate = client.post("/api/events/${fixture.eventId}/potential-locations") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.participantId)}")
            contentType(ContentType.Application.Json)
            setBody(locationBody("Participant City"))
        }
        val organizerCreate = client.post("/api/events/${fixture.eventId}/potential-locations") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(locationBody("Organizer City"))
        }
        val participantDelete = client.delete("/api/events/${fixture.eventId}/potential-locations/${fixture.locationId}") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.participantId)}")
        }

        assertEquals(HttpStatusCode.OK, participantRead.status, participantText)
        assertEquals(1, participantLocations.size)
        assertEquals("Existing City", participantLocations[0].jsonObject.getValue("name").jsonPrimitive.content)
        assertEquals(HttpStatusCode.Forbidden, nonMemberRead.status, nonMemberRead.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, participantCreate.status, participantCreate.bodyAsText())
        assertEquals(HttpStatusCode.Created, organizerCreate.status, organizerCreate.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, participantDelete.status, participantDelete.bodyAsText())
    }

    private fun createPotentialLocationFixture(suffix: String): PotentialLocationRouteFixture {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val locationRepository = PotentialLocationRepository(eventRepository)
        val eventId = "location-event-$suffix"
        val organizerId = "location-organizer-$suffix"
        val participantId = "location-participant-$suffix"
        val nonMemberId = "location-non-member-$suffix"
        val locationId = "location-existing-$suffix"

        insertUser(database, organizerId)
        insertUser(database, participantId)
        insertUser(database, nonMemberId)
        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = eventId,
                    title = "Location Event $suffix",
                    description = "Potential location access test event",
                    organizerId = organizerId,
                    participants = emptyList(),
                    proposedSlots = listOf(
                        TimeSlot(
                            id = "slot-$suffix",
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
            eventRepository.addParticipant(eventId, participantId).getOrThrow()
            locationRepository.addLocation(
                eventId,
                PotentialLocation(
                    id = locationId,
                    eventId = eventId,
                    name = "Existing City",
                    locationType = LocationType.CITY,
                    address = "Existing Address",
                    createdAt = "2026-06-20T10:00:00Z"
                )
            ).getOrThrow()
        }

        return PotentialLocationRouteFixture(
            database = database,
            eventRepository = eventRepository,
            locationRepository = locationRepository,
            eventId = eventId,
            organizerId = organizerId,
            participantId = participantId,
            nonMemberId = nonMemberId,
            locationId = locationId
        )
    }

    private fun locationBody(name: String): String =
        """
        {
          "name": "$name",
          "locationType": "CITY",
          "address": "$name Address"
        }
        """.trimIndent()

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

private data class PotentialLocationRouteFixture(
    val database: WakeveDb,
    val eventRepository: DatabaseEventRepository,
    val locationRepository: PotentialLocationRepository,
    val eventId: String,
    val organizerId: String,
    val participantId: String,
    val nonMemberId: String,
    val locationId: String
)
