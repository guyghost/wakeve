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
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Regression tests for BUG-4 (QA session 2026-09-18): activity routes were
 * defined but never mounted in the application routing, so every request to
 * /api/events/{id}/activities returned 404 — even for event members.
 */
class ActivityRoutesMountingTest {

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
        val slot = TimeSlot(
            id = "slot-1",
            start = "2026-12-05T12:00:00Z",
            end = "2026-12-12T22:00:00Z",
            timezone = "America/Sao_Paulo",
            timeOfDay = TimeOfDay.ALL_DAY
        )
        val event = Event(
            id = "event-activities",
            title = "Vacances à Rio",
            description = "QA",
            organizerId = "organizer-alice",
            participants = listOf("organizer-alice", "user-bob", "user-carla"),
            proposedSlots = listOf(slot),
            deadline = "2099-01-01T00:00:00Z",
            status = EventStatus.ORGANIZING,
            createdAt = "2026-09-18T19:00:00Z",
            updatedAt = "2026-09-18T19:00:00Z"
        )
        runBlocking { repository.createEvent(event).getOrThrow() }
        // createEvent only persists the organizer participant record; add the
        // member records for the invited participants explicitly.
        listOf("user-bob", "user-carla").forEach { userId ->
            database.participantQueries.insertParticipantWithAxes(
                id = "part_$userId",
                eventId = "event-activities",
                userId = userId,
                role = "PARTICIPANT",
                hasValidatedDate = 0,
                rsvpState = "NOT_APPLICABLE",
                dateValidationState = "NOT_APPLICABLE",
                joinedAt = "2026-09-18T19:00:00Z",
                updatedAt = "2026-09-18T19:00:00Z"
            )
        }
    }

    @Test
    fun `organizer can create an activity (route is mounted)`() = testApplication {
        application { module(database) }
        seedEvent()

        val response = client.post("/api/events/event-activities/activities") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("organizer-alice")}")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Pain de Sucre","description":"Coucher de soleil","durationMinutes":180,"organizerId":"organizer-alice"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
    }

    @Test
    fun `participant can list event activities`() = testApplication {
        application { module(database) }
        seedEvent()

        val response = client.get("/api/events/event-activities/activities") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("user-bob")}")
        }

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
    }

    @Test
    fun `unrelated user is forbidden from event activities`() = testApplication {
        application { module(database) }
        seedEvent()

        val response = client.post("/api/events/event-activities/activities") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("user-steve")}")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Tour piégé","description":"spam","durationMinutes":60,"organizerId":"organizer-alice"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status, response.bodyAsText())
    }
}
