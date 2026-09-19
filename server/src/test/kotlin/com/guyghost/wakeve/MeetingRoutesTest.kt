package com.guyghost.wakeve

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.organization.EventOrganizationReadinessRepository
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
import kotlin.test.assertTrue

/**
 * Regression tests for proposal #45: server-side persisted meetings —
 * organizer creates, members list, and the finalization readiness
 * MEETING_REQUIRED becomes satisfiable via the API (QA-13).
 */
class MeetingRoutesTest {

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
        val event = Event(
            id = "event-meetings",
            title = "Rio",
            description = "QA",
            organizerId = "organizer-alice",
            participants = listOf("organizer-alice", "user-bob"),
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
        database.participantQueries.insertParticipantWithAxes(
            id = "part_user-bob",
            eventId = "event-meetings",
            userId = "user-bob",
            role = "PARTICIPANT",
            hasValidatedDate = 0,
            rsvpState = "NOT_APPLICABLE",
            dateValidationState = "NOT_APPLICABLE",
            joinedAt = "2026-09-19T07:00:00Z",
            updatedAt = "2026-09-19T07:00:00Z"
        )
    }

    private val createBody = """
        {"platform":"FACETIME","title":"Briefing avant-départ","startTime":"2026-12-01T19:00:00Z","duration":"1h"}
    """.trimIndent()

    @Test
    fun `organizer creates a persisted meeting`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        val response = client.post("/api/events/event-meetings/meetings/persisted") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("organizer-alice")}")
            contentType(ContentType.Application.Json)
            setBody(createBody)
        }

        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        assertTrue(response.bodyAsText().contains("FACETIME"), response.bodyAsText())
    }

    @Test
    fun `members can list persisted meetings`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        client.post("/api/events/event-meetings/meetings/persisted") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("organizer-alice")}")
            contentType(ContentType.Application.Json)
            setBody(createBody)
        }

        val response = client.get("/api/events/event-meetings/meetings/persisted") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("user-bob")}")
        }
        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        assertTrue(response.bodyAsText().contains("Briefing avant-départ"), response.bodyAsText())
    }

    @Test
    fun `outsider cannot create or list meetings`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        val created = client.post("/api/events/event-meetings/meetings/persisted") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("user-outsider")}")
            contentType(ContentType.Application.Json)
            setBody(createBody)
        }
        assertEquals(HttpStatusCode.Forbidden, created.status, created.bodyAsText())

        val listed = client.get("/api/events/event-meetings/meetings/persisted") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("user-outsider")}")
        }
        assertEquals(HttpStatusCode.Forbidden, listed.status, listed.bodyAsText())
    }

    @Test
    fun `invalid platform is rejected`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        val response = client.post("/api/events/event-meetings/meetings/persisted") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("organizer-alice")}")
            contentType(ContentType.Application.Json)
            setBody("""{"platform":"TEAMS","title":"x","startTime":"2026-12-01T19:00:00Z"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
    }

    @Test
    fun `meeting creation satisfies MEETING_REQUIRED in readiness`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        val create = client.post("/api/events/event-meetings/meetings/persisted") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("organizer-alice")}")
            contentType(ContentType.Application.Json)
            setBody(createBody)
        }
        assertEquals(HttpStatusCode.Created, create.status, create.bodyAsText())

        val readiness = EventOrganizationReadinessRepository(database).getMeetingReadiness("event-meetings")
        assertTrue(readiness.complete, "MEETING_REQUIRED must be satisfied by the persisted meeting")
        assertEquals(1, readiness.meetingCount)
        // Proposal #45: a default reminder is created with the meeting
        val reminders = database.meetingReminderQueries.selectByMeetingId(
            database.meetingQueries.selectByEventId("event-meetings").executeAsOne().id
        ).executeAsList()
        assertEquals(1, reminders.size, "a default ONE_DAY_BEFORE reminder must exist")
    }
}
