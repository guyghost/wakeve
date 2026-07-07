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
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MeetingProxyRoutesAccessValidationTest {

    private val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "wakev-api"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "wakev-client"
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @BeforeTest
    fun setup() {
        DatabaseProvider.resetDatabase()
    }

    @AfterTest
    fun teardown() {
        DatabaseProvider.resetDatabase()
    }

    @Test
    fun `zoom proxy status and cancel are scoped to the local meeting event`() = testApplication {
        val fixture = createFixture()
        val organizerToken = createTestJwt(fixture.organizerId)
        val participantToken = createTestJwt(fixture.participantUserId)
        val outsiderToken = createTestJwt("outsider-user")

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val outsiderStatus = client.get("/api/meetings/proxy/zoom/${fixture.hostMeetingId}/status") {
            header(HttpHeaders.Authorization, "Bearer $outsiderToken")
        }
        assertEquals(HttpStatusCode.Forbidden, outsiderStatus.status, outsiderStatus.bodyAsText())

        val participantStatus = client.get("/api/meetings/proxy/zoom/${fixture.hostMeetingId}/status") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, participantStatus.status, participantStatus.bodyAsText())

        val participantCancel = client.post("/api/meetings/proxy/zoom/${fixture.hostMeetingId}/cancel") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }
        assertEquals(HttpStatusCode.Forbidden, participantCancel.status, participantCancel.bodyAsText())

        val organizerCancel = client.post("/api/meetings/proxy/zoom/${fixture.hostMeetingId}/cancel") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, organizerCancel.status, organizerCancel.bodyAsText())

        val unknownStatus = client.get("/api/meetings/proxy/zoom/unknown-host-meeting/status") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }
        assertEquals(HttpStatusCode.NotFound, unknownStatus.status, unknownStatus.bodyAsText())
    }

    private fun createFixture(): MeetingProxyFixture {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val eventId = "event-meeting-proxy"
        val organizerId = "organizer-user"
        val participantUserId = "participant-user"
        val hostMeetingId = "zoom-host-123"
        val now = "2026-06-20T10:00:00Z"

        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = eventId,
                    title = "Meeting Proxy Event",
                    description = "Event with a secured meeting proxy",
                    organizerId = organizerId,
                    participants = emptyList(),
                    proposedSlots = listOf(
                        TimeSlot(
                            id = "slot-meeting-proxy",
                            start = "2026-07-01T09:00:00Z",
                            end = "2026-07-01T10:00:00Z",
                            timezone = "UTC",
                            timeOfDay = TimeOfDay.SPECIFIC
                        )
                    ),
                    deadline = "2026-06-25T00:00:00Z",
                    status = EventStatus.ORGANIZING,
                    finalDate = "2026-07-01T09:00:00Z",
                    createdAt = now,
                    updatedAt = now,
                    eventType = EventType.OTHER
                )
            ).getOrThrow()
        }

        database.participantQueries.insertParticipant(
            id = "participant-$eventId",
            eventId = eventId,
            userId = participantUserId,
            role = "PARTICIPANT",
            hasValidatedDate = 1,
            joinedAt = now,
            updatedAt = now
        )

        database.meetingQueries.insertMeeting(
            id = "meeting-$eventId",
            eventId = eventId,
            organizerId = organizerId,
            title = "Kickoff",
            description = "Kickoff call",
            startTime = "2026-07-01T09:00:00Z",
            duration = "1h",
            platform = "ZOOM",
            meetingLink = "https://zoom.example/j/$hostMeetingId",
            provider = "ZOOM",
            displayLabel = "Zoom",
            targetUrl = "https://zoom.example/j/$hostMeetingId",
            creatorId = organizerId,
            verificationState = "VERIFIED",
            hostMeetingId = hostMeetingId,
            password = "secret",
            invitedParticipants = """["$participantUserId"]""",
            status = "SCHEDULED",
            createdAt = now
        )

        return MeetingProxyFixture(
            database = database,
            eventRepository = eventRepository,
            organizerId = organizerId,
            participantUserId = participantUserId,
            hostMeetingId = hostMeetingId
        )
    }

    private fun createTestJwt(userId: String): String {
        return JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withClaim("sessionId", "test-session-$userId")
            .withClaim("permissions", listOf("READ", "WRITE"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    private data class MeetingProxyFixture(
        val database: com.guyghost.wakeve.database.WakeveDb,
        val eventRepository: DatabaseEventRepository,
        val organizerId: String,
        val participantUserId: String,
        val hostMeetingId: String
    )
}
