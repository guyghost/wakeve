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
import kotlin.test.assertTrue

/**
 * Regression tests for proposal #40: marking transport as not needed is a
 * group-level decision that confirmed participants may record when the
 * organizer is unavailable (consistent with the collaborative budget #37).
 */
class TransportNotNeededAccessTest {

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

    private fun seedOrganizingEvent() {
        val repository = DatabaseEventRepository(database)
        val slot = TimeSlot(
            id = "slot-1",
            start = "2026-12-05T12:00:00Z",
            end = "2026-12-12T22:00:00Z",
            timezone = "America/Sao_Paulo",
            timeOfDay = TimeOfDay.ALL_DAY
        )
        val event = Event(
            id = "event-transport",
            title = "Vacances à Rio",
            description = "QA",
            organizerId = "organizer-alice",
            participants = listOf("organizer-alice", "user-carla"),
            proposedSlots = listOf(slot),
            deadline = "2099-01-01T00:00:00Z",
            status = EventStatus.ORGANIZING,
            createdAt = "2026-09-19T07:00:00Z",
            updatedAt = "2026-09-19T07:00:00Z"
        )
        runBlocking { repository.createEvent(event).getOrThrow() }
        database.participantQueries.insertParticipantWithAxes(
            id = "part_user-carla",
            eventId = "event-transport",
            userId = "user-carla",
            role = "PARTICIPANT",
            hasValidatedDate = 1,
            rsvpState = "ACCEPTED",
            dateValidationState = "VALIDATED_RETAINED_DATE",
            joinedAt = "2026-09-19T07:00:00Z",
            updatedAt = "2026-09-19T07:00:00Z"
        )
    }

    @Test
    fun `confirmed participant can mark transport as not needed`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        val response = client.post("/api/events/event-transport/transport/not-needed") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("user-carla")}")
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"Chacun réserve son vol"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        assertTrue(response.bodyAsText().contains("transportNotNeeded"), response.bodyAsText())
    }

    @Test
    fun `organizer keeps the ability to mark transport as not needed`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        val response = client.post("/api/events/event-transport/transport/not-needed") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("organizer-alice")}")
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"vols individuels"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
    }

    @Test
    fun `unrelated user is still forbidden`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        val response = client.post("/api/events/event-transport/transport/not-needed") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("user-steve")}")
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"spam"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status, response.bodyAsText())
    }
}
