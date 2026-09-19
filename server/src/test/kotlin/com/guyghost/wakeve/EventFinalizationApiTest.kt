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
import com.guyghost.wakeve.repository.TimeSlotStorageIdentity
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
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for BUG-3 + BUG-5 (QA session 2026-09-18):
 * - the FINALIZED failure must expose the exact readiness blockers instead of a
 *   generic "try again" message,
 * - MEETING_REQUIRED must be satisfiable via the API (explicit organizer
 *   not-needed decision),
 * - lodging creation must be allowed in ORGANIZING (removes the catch-22),
 * - a full ORGANIZING -> FINALIZED journey must be achievable through the API.
 */
class EventFinalizationApiTest {

    private lateinit var database: WakeveDb
    private lateinit var eventRepository: DatabaseEventRepository
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

    private fun seedOrganizingEvent(): DatabaseEventRepository {
        val repository = DatabaseEventRepository(database)
        val slot = TimeSlot(
            id = "slot-1",
            start = "2026-12-05T12:00:00Z",
            end = "2026-12-12T22:00:00Z",
            timezone = "America/Sao_Paulo",
            timeOfDay = TimeOfDay.ALL_DAY
        )
        val event = Event(
            id = "event-finalize",
            title = "Vacances à Rio",
            description = "QA finalization journey",
            organizerId = "organizer-alice",
            participants = listOf("organizer-alice", "user-bob"),
            proposedSlots = listOf(slot),
            deadline = "2099-01-01T00:00:00Z",
            status = EventStatus.CONFIRMED,
            finalDate = slot.start,
            createdAt = "2026-09-18T19:00:00Z",
            updatedAt = "2026-09-18T19:00:00Z"
        )
        runBlocking { repository.createEvent(event).getOrThrow() }
        database.confirmedDateQueries.insertConfirmedDate(
            id = "confirmed-event-finalize",
            eventId = "event-finalize",
            timeslotId = TimeSlotStorageIdentity.physicalId("event-finalize", "slot-1"),
            confirmedByOrganizerId = "organizer-alice",
            confirmedAt = "2026-09-18T19:05:00Z",
            updatedAt = "2026-09-18T19:05:00Z"
        )
        eventRepository = repository
        database.participantQueries.insertParticipantWithAxes(
            id = "part_user-bob",
            eventId = "event-finalize",
            userId = "user-bob",
            role = "PARTICIPANT",
            hasValidatedDate = 0,
            rsvpState = "NOT_APPLICABLE",
            dateValidationState = "NOT_APPLICABLE",
            joinedAt = "2026-09-18T19:00:00Z",
            updatedAt = "2026-09-18T19:00:00Z"
        )
        return repository
    }

    @Test
    fun `failed finalization exposes the readiness blockers`() = testApplication {
        application { module(database) }
        val repository = seedOrganizingEvent()
        val auth = "Bearer ${createTestJwt("organizer-alice")}"

        // Validate attendees against the retained date (seeded in CONFIRMED).
        listOf("organizer-alice", "user-bob").forEach { userId ->
            val rsvp = client.post("/api/events/event-finalize/participants/$userId/rsvp") {
                header(HttpHeaders.Authorization, auth)
                contentType(ContentType.Application.Json)
                setBody("""{"attendance":"CONFIRMED","slotId":"slot-1"}""")
            }
            assertEquals(HttpStatusCode.OK, rsvp.status, rsvp.bodyAsText())
        }
        listOf("COMPARING", "ORGANIZING").forEach { target ->
            val response = client.put("/api/events/event-finalize/status") {
                header(HttpHeaders.Authorization, auth)
                contentType(ContentType.Application.Json)
                setBody("""{"eventId":"event-finalize","status":"$target"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        }

        val response = client.put("/api/events/event-finalize/status") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"eventId":"event-finalize","status":"FINALIZED"}""")
        }
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.Conflict, response.status, body)
        assertTrue(body.contains("blockers"), "error body must list blockers: $body")
        assertTrue(body.contains("MEETING_REQUIRED"), "meetings blocker must be listed: $body")
    }

    @Test
    fun `readiness endpoint is visible to the organizer`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        val response = client.get("/api/events/event-finalize/readiness") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("organizer-alice")}")
        }

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        assertTrue(response.bodyAsText().contains("blockers"), response.bodyAsText())
    }

    @Test
    fun `a failed critical sync row still blocks finalization`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()
        val auth = "Bearer ${createTestJwt("organizer-alice")}"

        // Walk to ORGANIZING.
        listOf("COMPARING", "ORGANIZING").forEach { target ->
            val response = client.put("/api/events/event-finalize/status") {
                header(HttpHeaders.Authorization, auth)
                contentType(ContentType.Application.Json)
                setBody("""{"eventId":"event-finalize","status":"$target"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        }

        // A FAILED critical sync row is a genuine failure: it must block.
        database.syncMetadataQueries.insertSyncMetadataWithPayload(
            id = "sync_qa_failed_row",
            entityType = "meeting",
            entityId = "event-finalize",
            operation = "UPDATE",
            payload = "{}",
            timestamp = "2026-09-19T08:00:00Z",
            retryState = "PERMANENT_FAILURE",
            retryCount = 3,
            synced = 0
        )

        val response = client.put("/api/events/event-finalize/status") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"eventId":"event-finalize","status":"FINALIZED"}""")
        }
        val body = response.bodyAsText()
        assertTrue(
            body.contains("CRITICAL_SYNC_FAILED"),
            "a failed critical sync row must block finalization: $body"
        )
    }

    @Test
    fun `full finalization journey succeeds through the API`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()
        val auth = "Bearer ${createTestJwt("organizer-alice")}"

        // Validate attendees against the retained date (seeded in CONFIRMED).
        listOf("organizer-alice", "user-bob").forEach { userId ->
            val rsvp = client.post("/api/events/event-finalize/participants/$userId/rsvp") {
                header(HttpHeaders.Authorization, auth)
                contentType(ContentType.Application.Json)
                setBody("""{"attendance":"CONFIRMED","slotId":"slot-1"}""")
            }
            assertEquals(HttpStatusCode.OK, rsvp.status, rsvp.bodyAsText())
        }
        // Final scenario is planned in the CONFIRMED phase (scenarios unlock once
        // the date is retained, cf. AGENTS.md workflow).
        val scenario = client.post("/api/events/event-finalize/scenarios") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(
                """{"eventId":"event-finalize","name":"Appart Ipanema","dateOrPeriod":"2026-12-05 au 2026-12-12",
                   "location":"Ipanema, Rio de Janeiro","duration":7,"estimatedParticipants":3,
                   "estimatedBudgetPerPerson":1400,"description":"Appart 3 chambres"}"""
            )
        }
        assertEquals(HttpStatusCode.Created, scenario.status, scenario.bodyAsText())
        val scenarioId = Regex(""""id":\s*"([^"]+)"""").find(scenario.bodyAsText())?.groupValues?.get(1)
        val selected = client.post("/api/events/event-finalize/scenarios/$scenarioId/select-final") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.OK, selected.status, selected.bodyAsText())

        // Walk to ORGANIZING following the allowed transitions (select-final may
        // already have moved the event into COMPARING).
        while (true) {
            val current = client.get("/api/events/event-finalize") {
                header(HttpHeaders.Authorization, auth)
            }.bodyAsText()
            val status = Regex(""""status":\s*"([^"]+)"""").find(current)?.groupValues?.get(1)
            if (status == EventStatus.ORGANIZING.name) break
            val target = if (status == EventStatus.CONFIRMED.name) "COMPARING" else "ORGANIZING"
            val response = client.put("/api/events/event-finalize/status") {
                header(HttpHeaders.Authorization, auth)
                contentType(ContentType.Application.Json)
                setBody("""{"eventId":"event-finalize","status":"$target"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        }
        // Lodging: confirmed accommodation is creatable in ORGANIZING (catch-22 fix).
        val accommodation = client.post("/api/events/event-finalize/accommodation") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(
                """{"eventId":"event-finalize","name":"Appart Ipanema","type":"VACATION_RENTAL",
                   "address":"Rua Francisco Sa 90","capacity":4,"pricePerNight":17100,"totalNights":7,
                   "bookingStatus":"CONFIRMED","checkInDate":"2026-12-05","checkOutDate":"2026-12-12"}"""
            )
        }
        assertEquals(HttpStatusCode.Created, accommodation.status, accommodation.bodyAsText())

        // Meetings + reminders: explicit organizer decision.
        val meetingsNotNeeded = client.post("/api/events/event-finalize/readiness/MEETINGS/not-needed") {
            header(HttpHeaders.Authorization, auth)
        }
        assertEquals(HttpStatusCode.OK, meetingsNotNeeded.status, meetingsNotNeeded.bodyAsText())

        // Budget baseline.
        // NOTE: two PUTs are required until #35 lands — the first PUT currently
        // creates an empty budget (BUG-6), the second persists the baseline.
        repeat(2) {
            val budget = client.put("/api/events/event-finalize/budget") {
                header(HttpHeaders.Authorization, auth)
                contentType(ContentType.Application.Json)
                setBody(
                    """{"id":"budget-finalize","eventId":"event-finalize","totalEstimated":4200,"totalActual":0,
                       "transportEstimated":2400,"transportActual":0,"accommodationEstimated":1200,"accommodationActual":0,
                       "mealsEstimated":400,"mealsActual":0,"activitiesEstimated":150,"activitiesActual":0,
                       "equipmentEstimated":0,"equipmentActual":0,"otherEstimated":50,"otherActual":0,
                       "createdAt":"2026-09-18T19:00:00Z","updatedAt":"2026-09-18T19:00:00Z"}"""
                )
            }
            assertEquals(HttpStatusCode.OK, budget.status, budget.bodyAsText())
        }

        // Payment: pot + trusted Tricount link.
        val pot = client.post("/api/events/event-finalize/payment/pot") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"eventId":"event-finalize","goalAmount":4200,"title":"Cagnotte Rio"}""")
        }
        assertEquals(HttpStatusCode.Created, pot.status, pot.bodyAsText())
        val tricount = client.post("/api/events/event-finalize/payment/tricount/link") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"provider":"TRICOUNT","providerId":"rio-group","providerUrl":"https://tricount.com/g/rio","syncStatus":"LINKED"}""")
        }
        assertEquals(HttpStatusCode.Created, tricount.status, tricount.bodyAsText())

        // Transport: not needed (organizer decision).

        // Transport: not needed (organizer decision).
        val transport = client.post("/api/events/event-finalize/transport/not-needed") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"Chacun réserve son vol"}""")
        }
        assertEquals(HttpStatusCode.OK, transport.status, transport.bodyAsText())

        val finalized = client.put("/api/events/event-finalize/status") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"eventId":"event-finalize","status":"FINALIZED"}""")
        }
        assertEquals(HttpStatusCode.OK, finalized.status, finalized.bodyAsText())
        assertTrue(finalized.bodyAsText().contains(""""status":"FINALIZED"""") || finalized.bodyAsText().contains("\"status\": \"FINALIZED\""), finalized.bodyAsText())
    }
}
