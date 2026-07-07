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
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.module
import com.guyghost.wakeve.repository.DatabaseEventRepository
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventOrganizationPhase2BackendRoutesTest {

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
    fun `authenticated participant can vote on polling event slot`() = testApplication {
        val fixture = createFixture("vote-allowed", status = EventStatus.POLLING)
        val participantToken = createTestJwt(fixture.participantId)
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.post("/api/events/${fixture.eventId}/poll/votes") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
            contentType(ContentType.Application.Json)
            setBody(voteBody(fixture, fixture.participantId, fixture.secondSlotId, "YES"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val vote = fixture.database.voteQueries
            .selectByTimeslotAndParticipant(fixture.secondSlotId, fixture.participantRecordId)
            .executeAsOneOrNull()
        assertNotNull(vote)
        assertEquals(Vote.YES.name, vote.vote)
    }

    @Test
    fun `authenticated non participant cannot vote on polling event slot`() = testApplication {
        val fixture = createFixture("vote-non-member", status = EventStatus.POLLING)
        val nonMemberToken = createTestJwt(fixture.nonMemberId)
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.post("/api/events/${fixture.eventId}/poll/votes") {
            header(HttpHeaders.Authorization, "Bearer $nonMemberToken")
            contentType(ContentType.Application.Json)
            setBody(voteBody(fixture, fixture.nonMemberId, fixture.secondSlotId, "YES"))
        }

        assertEquals(
            HttpStatusCode.Forbidden,
            response.status,
            "Non-participants must be rejected as an authorization failure, not accepted as a caller-controlled participantId"
        )
    }

    @Test
    fun `poll votes are visible to participants but hidden from non members`() = testApplication {
        val fixture = createFixture("poll-read-access", status = EventStatus.POLLING, seedVote = true)
        val participantToken = createTestJwt(fixture.participantId)
        val nonMemberToken = createTestJwt(fixture.nonMemberId)
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val participantRead = client.get("/api/events/${fixture.eventId}/poll") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }
        val nonMemberRead = client.get("/api/events/${fixture.eventId}/poll") {
            header(HttpHeaders.Authorization, "Bearer $nonMemberToken")
        }

        assertEquals(HttpStatusCode.OK, participantRead.status, participantRead.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, nonMemberRead.status, nonMemberRead.bodyAsText())
    }

    @Test
    fun `organizer can confirm an existing voted slot`() = testApplication {
        val fixture = createFixture("confirm-allowed", status = EventStatus.POLLING, seedVote = true)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.put("/api/events/${fixture.eventId}/status") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(confirmSlotBody(fixture, fixture.secondSlotId))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val confirmed = fixture.database.confirmedDateQueries
            .selectByEventId(fixture.eventId)
            .executeAsOneOrNull()
        assertNotNull(confirmed)
        assertEquals(
            fixture.secondSlotId,
            confirmed.timeslotId,
            "Date confirmation must retain the organizer-selected existing slot, not an implicit first slot"
        )
    }

    @Test
    fun `non organizer cannot confirm event date`() = testApplication {
        val fixture = createFixture("confirm-non-organizer", status = EventStatus.POLLING, seedVote = true)
        val participantToken = createTestJwt(fixture.participantId)
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.put("/api/events/${fixture.eventId}/status") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
            contentType(ContentType.Application.Json)
            setBody(confirmSlotBody(fixture, fixture.secondSlotId))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `non organizer cannot drive confirmed event into comparing or organizing`() = testApplication {
        val fixture = createFixture("non-organizer-later-workflow", status = EventStatus.CONFIRMED, confirmed = true)
        val participantToken = createTestJwt(fixture.participantId)
        val client = createJsonClient()
        val requestedStatuses = listOf(
            EventStatus.COMPARING,
            EventStatus.ORGANIZING
        )
        val violations = mutableListOf<String>()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        requestedStatuses.forEach { requestedStatus ->
            val response = client.put("/api/events/${fixture.eventId}/status") {
                header(HttpHeaders.Authorization, "Bearer $participantToken")
                contentType(ContentType.Application.Json)
                setBody(statusBody(fixture, requestedStatus))
            }

            if (response.status != HttpStatusCode.Forbidden) {
                violations += "${requestedStatus.name} returned ${response.status}: ${response.bodyAsText()}"
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Only the organizer may drive event workflow transitions after polling:\n${violations.joinToString("\n")}"
        )
    }

    @Test
    fun `non organizer cannot finalize organizing event`() = testApplication {
        val fixture = createFixture("non-organizer-finalized", status = EventStatus.ORGANIZING, confirmed = true)
        val participantToken = createTestJwt(fixture.participantId)
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.put("/api/events/${fixture.eventId}/status") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
            contentType(ContentType.Application.Json)
            setBody(statusBody(fixture, EventStatus.FINALIZED))
        }

        assertEquals(
            HttpStatusCode.Forbidden,
            response.status,
            "FINALIZED must be organizer-only before finalization readiness is evaluated"
        )
    }

    @Test
    fun `event status route rejects invalid workflow jumps and keeps current status`() = testApplication {
        val fixture = createFixture("invalid-draft-to-organizing", status = EventStatus.DRAFT)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.put("/api/events/${fixture.eventId}/status") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(statusBody(fixture, EventStatus.ORGANIZING))
        }

        assertTrue(
            response.status == HttpStatusCode.BadRequest || response.status == HttpStatusCode.Conflict,
            "DRAFT -> ORGANIZING must be rejected by the backend workflow guard, got ${response.status}: ${response.bodyAsText()}"
        )
        assertEquals(
            EventStatus.DRAFT,
            fixture.eventRepository.getEvent(fixture.eventId)?.status,
            "Rejected workflow jumps must not mutate the persisted event status"
        )
    }

    @Test
    fun `organizer cannot confirm unknown slot`() = testApplication {
        val fixture = createFixture("confirm-bad-slot", status = EventStatus.POLLING, seedVote = true)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.put("/api/events/${fixture.eventId}/status") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(confirmSlotBody(fixture, "slot-does-not-exist"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `organizer can download ICS after date confirmation`() = testApplication {
        val fixture = createFixture("ics-organizer", status = EventStatus.CONFIRMED, confirmed = true)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.get("/api/events/${fixture.eventId}/calendar/ics") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("BEGIN:VCALENDAR"))
    }

    @Test
    fun `confirmed participant can download ICS after date confirmation`() = testApplication {
        val fixture = createFixture("ics-participant", status = EventStatus.CONFIRMED, confirmed = true)
        val participantToken = createTestJwt(fixture.participantId)
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.get("/api/events/${fixture.eventId}/calendar/ics") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("UID:${fixture.eventId}@wakeve.app"))
    }

    @Test
    fun `non member cannot download ICS for confirmed event`() = testApplication {
        val fixture = createFixture("ics-non-member", status = EventStatus.CONFIRMED, confirmed = true)
        val nonMemberToken = createTestJwt(fixture.nonMemberId)
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.get("/api/events/${fixture.eventId}/calendar/ics") {
            header(HttpHeaders.Authorization, "Bearer $nonMemberToken")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `non member cannot schedule event reminders`() = testApplication {
        val fixture = createFixture("reminder-non-member", status = EventStatus.CONFIRMED, confirmed = true)
        val nonMemberToken = createTestJwt(fixture.nonMemberId)
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.post("/api/events/${fixture.eventId}/calendar/reminders/one_hour_before") {
            header(HttpHeaders.Authorization, "Bearer $nonMemberToken")
        }

        assertEquals(
            HttpStatusCode.Forbidden,
            response.status,
            "If reminder scheduling is not implemented yet, the route must still authenticate event membership before returning its implementation status"
        )
    }

    private fun ApplicationTestBuilder.createJsonClient() = createClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun createFixture(
        suffix: String,
        status: EventStatus,
        seedVote: Boolean = false,
        confirmed: Boolean = false
    ): Phase2BackendFixture {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)

        val eventId = "event-phase2-$suffix"
        val organizerId = "organizer-$suffix"
        val participantId = "participant-$suffix"
        val participantRecordId = "part-$suffix"
        val nonMemberId = "non-member-$suffix"
        val firstSlotId = "slot-first-$suffix"
        val secondSlotId = "slot-second-$suffix"
        val now = "2026-05-22T10:00:00Z"

        insertUser(database, organizerId, "Organizer $suffix")
        insertUser(database, participantId, "Participant $suffix")
        insertUser(database, nonMemberId, "Non Member $suffix")

        val event = Event(
            id = eventId,
            title = "Phase 2 Backend $suffix",
            description = "Backend route contract coverage for task 2.3",
            organizerId = organizerId,
            participants = emptyList(),
            proposedSlots = listOf(
                TimeSlot(
                    id = firstSlotId,
                    start = "2026-06-20T09:00:00Z",
                    end = "2026-06-20T11:00:00Z",
                    timezone = "UTC",
                    timeOfDay = TimeOfDay.SPECIFIC
                ),
                TimeSlot(
                    id = secondSlotId,
                    start = "2026-06-21T14:00:00Z",
                    end = "2026-06-21T16:00:00Z",
                    timezone = "UTC",
                    timeOfDay = TimeOfDay.SPECIFIC
                )
            ),
            deadline = "2026-06-01T00:00:00Z",
            status = status,
            finalDate = if (confirmed) "2026-06-21T14:00:00Z" else null,
            createdAt = now,
            updatedAt = now,
            eventType = EventType.OTHER
        )

        runBlocking {
            eventRepository.createEvent(event).getOrThrow()
        }

        database.participantQueries.insertParticipant(
            id = participantRecordId,
            eventId = eventId,
            userId = participantId,
            role = "PARTICIPANT",
            hasValidatedDate = if (confirmed) 1 else 0,
            joinedAt = now,
            updatedAt = now
        )

        if (seedVote) {
            runBlocking {
                eventRepository.addVote(eventId, participantId, secondSlotId, Vote.YES).getOrThrow()
            }
        }

        if (confirmed) {
            database.confirmedDateQueries.insertConfirmedDate(
                id = "confirmed-$suffix",
                eventId = eventId,
                timeslotId = secondSlotId,
                confirmedByOrganizerId = organizerId,
                confirmedAt = "2026-05-22T11:00:00Z",
                updatedAt = "2026-05-22T11:00:00Z"
            )
        }

        return Phase2BackendFixture(
            database = database,
            eventRepository = eventRepository,
            eventId = eventId,
            organizerId = organizerId,
            participantId = participantId,
            participantRecordId = participantRecordId,
            nonMemberId = nonMemberId,
            firstSlotId = firstSlotId,
            secondSlotId = secondSlotId
        )
    }

    private fun insertUser(
        database: com.guyghost.wakeve.database.WakeveDb,
        userId: String,
        name: String
    ) {
        database.userQueries.insertUser(
            id = userId,
            provider_id = "provider-$userId",
            email = "$userId@example.test",
            name = name,
            avatar_url = null,
            provider = "google",
            role = "USER",
            created_at = "2026-05-22T10:00:00Z",
            updated_at = "2026-05-22T10:00:00Z"
        )
    }

    private fun voteBody(
        fixture: Phase2BackendFixture,
        participantId: String,
        slotId: String,
        vote: String
    ): String {
        return """
            {
              "eventId": "${fixture.eventId}",
              "participantId": "$participantId",
              "slotId": "$slotId",
              "vote": "$vote"
            }
        """.trimIndent()
    }

    private fun confirmSlotBody(fixture: Phase2BackendFixture, slotId: String): String {
        return """
            {
              "eventId": "${fixture.eventId}",
              "status": "CONFIRMED",
              "slotId": "$slotId",
              "finalDate": "2026-06-21T14:00:00Z"
            }
        """.trimIndent()
    }

    private fun statusBody(fixture: Phase2BackendFixture, status: EventStatus): String {
        return """
            {
              "eventId": "${fixture.eventId}",
              "status": "${status.name}",
              "finalDate": "2026-06-21T14:00:00Z"
            }
        """.trimIndent()
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

    private data class Phase2BackendFixture(
        val database: com.guyghost.wakeve.database.WakeveDb,
        val eventRepository: DatabaseEventRepository,
        val eventId: String,
        val organizerId: String,
        val participantId: String,
        val participantRecordId: String,
        val nonMemberId: String,
        val firstSlotId: String,
        val secondSlotId: String
    )
}
