package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.invitation.InvitationRepository
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.Invitation
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventOrganizationBackendAccessRoutesTest {

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
    fun `accepting invitation creates pending participant and keeps organization budget protected`() = testApplication {
        val fixture = createFixture()
        val participantToken = createTestJwt(fixture.pendingUserId)

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val acceptResponse = client.post("/api/invite/${fixture.invitationCode}/accept") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }

        assertEquals(HttpStatusCode.OK, acceptResponse.status)
        assertTrue(acceptResponse.bodyAsText().contains(fixture.eventId))

        val participant = fixture.database.participantQueries
            .selectByEventIdAndUserId(fixture.eventId, fixture.pendingUserId)
            .executeAsOneOrNull()

        assertNotNull(participant)
        assertEquals(0L, participant.hasValidatedDate)

        val budgetResponse = client.get("/api/events/${fixture.eventId}/budget") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }

        assertEquals(
            HttpStatusCode.Forbidden,
            budgetResponse.status,
            "Accepted-but-pending participants must not access protected organization budget details"
        )
    }

    @Test
    fun `participant rsvp date validation endpoint marks participant confirmed for retained date`() = testApplication {
        val fixture = createFixture()
        val participantToken = createTestJwt(fixture.pendingUserId)

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        application {
            module(fixture.database, fixture.eventRepository)
        }

        client.post("/api/invite/${fixture.invitationCode}/accept") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }

        val rsvpResponse = client.post(
            "/api/events/${fixture.eventId}/participants/${fixture.pendingUserId}/rsvp"
        ) {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "slotId": "${fixture.finalSlotId}",
                  "attendance": "CONFIRMED"
                }
                """.trimIndent()
            )
        }

        assertEquals(
            HttpStatusCode.OK,
            rsvpResponse.status,
            "Expected POST /api/events/{id}/participants/{userId}/rsvp to confirm attendance for the retained date"
        )

        val participant = fixture.database.participantQueries
            .selectByEventIdAndUserId(fixture.eventId, fixture.pendingUserId)
            .executeAsOneOrNull()

        assertNotNull(participant)
        assertEquals(1L, participant.hasValidatedDate)
    }

    @Test
    fun `organization budget route refuses pending participant and allows confirmed participant`() = testApplication {
        val fixture = createFixture()
        val participantToken = createTestJwt(fixture.pendingUserId)

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        application {
            module(fixture.database, fixture.eventRepository)
        }

        client.post("/api/invite/${fixture.invitationCode}/accept") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }

        val pendingResponse = client.get("/api/events/${fixture.eventId}/budget") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }

        assertEquals(
            HttpStatusCode.Forbidden,
            pendingResponse.status,
            "Pending participants must be denied protected organization routes"
        )

        val participant = fixture.database.participantQueries
            .selectByEventIdAndUserId(fixture.eventId, fixture.pendingUserId)
            .executeAsOne()

        fixture.database.participantQueries.updateValidation(
            hasValidatedDate = 1,
            updatedAt = "2026-05-21T12:00:00Z",
            id = participant.id
        )

        val confirmedResponse = client.get("/api/events/${fixture.eventId}/budget") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }

        assertEquals(HttpStatusCode.OK, confirmedResponse.status)

        val body = json.parseToJsonElement(confirmedResponse.bodyAsText()).jsonObject
        assertEquals(fixture.eventId, body["eventId"]?.jsonPrimitive?.content)
    }

    @Test
    fun `pending participant is forbidden from creating budget item`() = testApplication {
        val fixture = createFixture()
        val participantToken = createTestJwt(fixture.pendingUserId)

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        application {
            module(fixture.database, fixture.eventRepository)
        }

        client.post("/api/invite/${fixture.invitationCode}/accept") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }

        val response = client.post("/api/events/${fixture.eventId}/budget/items") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name": "Leaked expense",
                  "description": "Pending participant must not create this",
                  "category": "OTHER",
                  "estimatedCost": 42.0
                }
                """.trimIndent()
            )
        }

        assertEquals(
            HttpStatusCode.Forbidden,
            response.status,
            "Pending participants must not mutate protected budget subroutes"
        )
    }

    @Test
    fun `unrelated authenticated user cannot list event participants`() = testApplication {
        val fixture = createFixture()
        val unrelatedToken = createTestJwt("unrelated-user")

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.get("/api/events/${fixture.eventId}/participants") {
            header(HttpHeaders.Authorization, "Bearer $unrelatedToken")
        }

        assertEquals(
            HttpStatusCode.Forbidden,
            response.status,
            "Authenticated users without event access must not enumerate participants"
        )
    }

    @Test
    fun `non organizer participant cannot add participants to event`() = testApplication {
        val fixture = createFixture(status = EventStatus.DRAFT)
        val participantToken = createTestJwt(fixture.pendingUserId)

        fixture.database.participantQueries.insertParticipant(
            id = "part_${fixture.eventId}_${fixture.pendingUserId}",
            eventId = fixture.eventId,
            userId = fixture.pendingUserId,
            role = "PARTICIPANT",
            hasValidatedDate = 0,
            joinedAt = "2026-05-21T10:05:00Z",
            updatedAt = "2026-05-21T10:05:00Z"
        )

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.post("/api/events/${fixture.eventId}/participants") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "eventId": "${fixture.eventId}",
                  "participantId": "attacker-added-user"
                }
                """.trimIndent()
            )
        }

        assertEquals(
            HttpStatusCode.Forbidden,
            response.status,
            "Only the organizer should be able to add participants"
        )
    }

    @Test
    fun `participant rsvp with non retained slot is rejected and does not validate date`() = testApplication {
        val fixture = createFixture()
        val participantToken = createTestJwt(fixture.pendingUserId)

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        application {
            module(fixture.database, fixture.eventRepository)
        }

        client.post("/api/invite/${fixture.invitationCode}/accept") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }

        val response = client.post(
            "/api/events/${fixture.eventId}/participants/${fixture.pendingUserId}/rsvp"
        ) {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "slotId": "slot-not-retained",
                  "attendance": "CONFIRMED"
                }
                """.trimIndent()
            )
        }

        assertTrue(
            response.status == HttpStatusCode.BadRequest || response.status == HttpStatusCode.Forbidden,
            "RSVP for a non-retained slot must be rejected"
        )

        val participant = fixture.database.participantQueries
            .selectByEventIdAndUserId(fixture.eventId, fixture.pendingUserId)
            .executeAsOne()

        assertEquals(0L, participant.hasValidatedDate)
    }

    @Test
    fun `participant rsvp without confirmed final slot is rejected and does not validate date`() = testApplication {
        val fixture = createFixture(confirmFinalSlot = false)
        val participantToken = createTestJwt(fixture.pendingUserId)

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        application {
            module(fixture.database, fixture.eventRepository)
        }

        client.post("/api/invite/${fixture.invitationCode}/accept") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }

        val response = client.post(
            "/api/events/${fixture.eventId}/participants/${fixture.pendingUserId}/rsvp"
        ) {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "slotId": "${fixture.finalSlotId}",
                  "attendance": "CONFIRMED"
                }
                """.trimIndent()
            )
        }

        assertTrue(
            response.status == HttpStatusCode.BadRequest || response.status == HttpStatusCode.Forbidden,
            "RSVP must be rejected when the event has no confirmed retained slot"
        )

        val participant = fixture.database.participantQueries
            .selectByEventIdAndUserId(fixture.eventId, fixture.pendingUserId)
            .executeAsOne()

        assertEquals(0L, participant.hasValidatedDate)
    }

    private fun createFixture(
        status: EventStatus = EventStatus.ORGANIZING,
        confirmFinalSlot: Boolean = true
    ): BackendAccessFixture {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val invitationRepository = InvitationRepository(database)
        val budgetRepository = BudgetRepository(database)

        val eventId = "event-org-access"
        val organizerId = "organizer-user"
        val pendingUserId = "pending-user"
        val invitationCode = "ORGRED12"
        val finalSlotId = "slot-retained"
        val now = "2026-05-21T10:00:00Z"

        val event = Event(
            id = eventId,
            title = "Weekend Organization",
            description = "Event with protected organization details",
            organizerId = organizerId,
            participants = emptyList(),
            proposedSlots = listOf(
                TimeSlot(
                    id = finalSlotId,
                    start = "2026-06-20T09:00:00Z",
                    end = "2026-06-20T11:00:00Z",
                    timezone = "UTC",
                    timeOfDay = TimeOfDay.SPECIFIC
                )
            ),
            deadline = "2026-06-01T00:00:00Z",
            status = status,
            finalDate = "2026-06-20T09:00:00Z",
            createdAt = now,
            updatedAt = now,
            eventType = EventType.OTHER
        )

        runBlocking {
            eventRepository.createEvent(event).getOrThrow()
        }

        invitationRepository.createInvitation(
            Invitation(
                id = "inv-org-access",
                code = invitationCode,
                eventId = eventId,
                createdBy = organizerId,
                createdAt = now
            )
        ).getOrThrow()

        budgetRepository.createBudget(eventId)

        if (confirmFinalSlot) {
            database.confirmedDateQueries.insertConfirmedDate(
                id = "confirmed-$eventId",
                eventId = eventId,
                timeslotId = finalSlotId,
                confirmedByOrganizerId = organizerId,
                confirmedAt = "2026-05-21T11:00:00Z",
                updatedAt = "2026-05-21T11:00:00Z"
            )
        }

        return BackendAccessFixture(
            database = database,
            eventRepository = eventRepository,
            eventId = eventId,
            pendingUserId = pendingUserId,
            invitationCode = invitationCode,
            finalSlotId = finalSlotId
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

    private data class BackendAccessFixture(
        val database: com.guyghost.wakeve.database.WakeveDb,
        val eventRepository: DatabaseEventRepository,
        val eventId: String,
        val pendingUserId: String,
        val invitationCode: String,
        val finalSlotId: String
    )
}
