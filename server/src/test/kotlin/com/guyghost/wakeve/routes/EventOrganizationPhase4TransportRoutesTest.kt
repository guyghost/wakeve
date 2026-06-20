package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.OptimizationType
import com.guyghost.wakeve.models.TransportLocation
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.module
import com.guyghost.wakeve.repository.DatabaseEventRepository
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventOrganizationPhase4TransportRoutesTest {

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
    fun `readiness lists confirmed participants missing departure data`() = testApplication {
        val fixture = createFixture("readiness-missing", EventStatus.ORGANIZING)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.get("/api/events/${fixture.eventId}/transport/readiness") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(fixture.confirmedParticipantId))
        assertTrue(body.contains(fixture.secondConfirmedParticipantId))
        assertTrue(body.contains("departure", ignoreCase = true))
        assertTrue(body.contains("false") || body.contains("INCOMPLETE", ignoreCase = true))
    }

    @Test
    fun `plan generation returns conflict while confirmed participant departures are missing`() = testApplication {
        val fixture = createFixture("generate-missing", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.post("/api/events/${fixture.eventId}/transport/plans/generate") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(generatePlanBody())
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `participant departure updates are limited to self unless organizer acts`() = testApplication {
        val fixture = createFixture("departure-auth", EventStatus.ORGANIZING)
        val participantToken = createTestJwt(fixture.confirmedParticipantId)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val ownDeparture = client.put(
            "/api/events/${fixture.eventId}/transport/departures/${fixture.confirmedParticipantId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
            contentType(ContentType.Application.Json)
            setBody(departureBody(fixture.confirmedParticipantId, "Paris Gare de Lyon"))
        }
        val anotherParticipantDeparture = client.put(
            "/api/events/${fixture.eventId}/transport/departures/${fixture.secondConfirmedParticipantId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
            contentType(ContentType.Application.Json)
            setBody(departureBody(fixture.secondConfirmedParticipantId, "Lille Flandres"))
        }
        val organizerDeparture = client.put(
            "/api/events/${fixture.eventId}/transport/departures/${fixture.secondConfirmedParticipantId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(departureBody(fixture.secondConfirmedParticipantId, "Lille Flandres"))
        }

        assertEquals(HttpStatusCode.OK, ownDeparture.status)
        assertEquals(HttpStatusCode.Forbidden, anotherParticipantDeparture.status)
        assertEquals(HttpStatusCode.OK, organizerDeparture.status)
    }

    @Test
    fun `organizer gets explicit conflict when transport provider is not configured`() = testApplication {
        val fixture = createFixture("generate-select", EventStatus.ORGANIZING)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        putAllDepartures(client, fixture, organizerToken)

        val generated = client.post("/api/events/${fixture.eventId}/transport/plans/generate") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(generatePlanBody())
        }

        assertEquals(HttpStatusCode.Conflict, generated.status)
        val body = generated.bodyAsText()
        assertTrue(body.contains("Transport option provider is not configured"), body)
    }

    @Test
    fun `non members and unconfirmed participants cannot read transport details`() = testApplication {
        val fixture = createFixture("read-auth", EventStatus.ORGANIZING)
        val nonMemberToken = createTestJwt(fixture.nonMemberId)
        val pendingToken = createTestJwt(fixture.pendingParticipantId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val nonMemberReadiness = client.get("/api/events/${fixture.eventId}/transport/readiness") {
            header(HttpHeaders.Authorization, "Bearer $nonMemberToken")
        }
        val pendingPlans = client.get("/api/events/${fixture.eventId}/transport/plans") {
            header(HttpHeaders.Authorization, "Bearer $pendingToken")
        }

        assertEquals(HttpStatusCode.Forbidden, nonMemberReadiness.status)
        assertEquals(HttpStatusCode.Forbidden, pendingPlans.status)
    }

    @Test
    fun `finalized event rejects transport generate select and delete mutations`() = testApplication {
        val fixture = createFixture("finalized-readonly", EventStatus.FINALIZED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val generate = client.post("/api/events/${fixture.eventId}/transport/plans/generate") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(generatePlanBody())
        }
        val select = client.post("/api/events/${fixture.eventId}/transport/plans/${fixture.transportPlanId}/select") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }
        val delete = client.delete("/api/events/${fixture.eventId}/transport/plans/${fixture.transportPlanId}") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }

        assertEquals(HttpStatusCode.Conflict, generate.status)
        assertEquals(HttpStatusCode.Conflict, select.status)
        assertEquals(HttpStatusCode.Conflict, delete.status)
    }

    @Test
    fun `finalized event rejects departure mutations for organizer and confirmed participant`() = testApplication {
        val fixture = createFixture("finalized-departure-readonly", EventStatus.FINALIZED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val participantToken = createTestJwt(fixture.confirmedParticipantId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val organizerUpdate = client.put(
            "/api/events/${fixture.eventId}/transport/departures/${fixture.confirmedParticipantId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(departureBody(fixture.confirmedParticipantId, "Paris Gare de Lyon"))
        }
        val participantUpdate = client.put(
            "/api/events/${fixture.eventId}/transport/departures/${fixture.confirmedParticipantId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
            contentType(ContentType.Application.Json)
            setBody(departureBody(fixture.confirmedParticipantId, "Paris Gare de Lyon"))
        }

        assertEquals(HttpStatusCode.Conflict, organizerUpdate.status)
        assertEquals(HttpStatusCode.Conflict, participantUpdate.status)
    }

    @Test
    fun `finalized event rejects non member departure mutation with forbidden before workflow conflict`() = testApplication {
        val fixture = createFixture("finalized-departure-guard-order", EventStatus.FINALIZED)
        val nonMemberToken = createTestJwt(fixture.nonMemberId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.put(
            "/api/events/${fixture.eventId}/transport/departures/${fixture.confirmedParticipantId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $nonMemberToken")
            contentType(ContentType.Application.Json)
            setBody(departureBody(fixture.confirmedParticipantId, "Paris Gare de Lyon"))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `transport generation is limited to confirmed comparing and organizing workflow states`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val draft = createFixture("status-draft", EventStatus.DRAFT, database)
        val polling = createFixture("status-polling", EventStatus.POLLING, database)
        val organizerTokenDraft = createTestJwt(draft.organizerId)
        val organizerTokenPolling = createTestJwt(polling.organizerId)
        val client = createJsonClient()

        application {
            module(database)
        }

        val draftResponse = client.post("/api/events/${draft.eventId}/transport/plans/generate") {
            header(HttpHeaders.Authorization, "Bearer $organizerTokenDraft")
            contentType(ContentType.Application.Json)
            setBody(generatePlanBody())
        }
        val pollingResponse = client.post("/api/events/${polling.eventId}/transport/plans/generate") {
            header(HttpHeaders.Authorization, "Bearer $organizerTokenPolling")
            contentType(ContentType.Application.Json)
            setBody(generatePlanBody())
        }

        assertEquals(HttpStatusCode.Conflict, draftResponse.status)
        assertEquals(HttpStatusCode.Conflict, pollingResponse.status)
    }

    @Test
    fun `transport routes block draft workflow before exposing readiness plans departures select delete or generate`() = testApplication {
        val fixture = createFixture("draft-global-phase-block", EventStatus.DRAFT)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        insertTransportPlan(fixture.database, fixture.eventId, fixture.transportPlanId)

        application {
            module(fixture.database)
        }

        val readiness = client.get("/api/events/${fixture.eventId}/transport/readiness") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }
        val plans = client.get("/api/events/${fixture.eventId}/transport/plans") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }
        val departure = client.put(
            "/api/events/${fixture.eventId}/transport/departures/${fixture.confirmedParticipantId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(departureBody(fixture.confirmedParticipantId, "Paris Gare de Lyon"))
        }
        val select = client.post("/api/events/${fixture.eventId}/transport/plans/${fixture.transportPlanId}/select") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }
        val delete = client.delete("/api/events/${fixture.eventId}/transport/plans/${fixture.transportPlanId}") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }
        val generate = client.post("/api/events/${fixture.eventId}/transport/plans/generate") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(generatePlanBody())
        }

        listOf(readiness, plans, departure, select, delete, generate).forEach { response ->
            assertEquals(
                HttpStatusCode.Conflict,
                response.status,
                "DRAFT events must block every transport route after authorization and before exposing logistics state"
            )
        }
    }

    @Test
    fun `transport routes block polling workflow before exposing readiness plans departures select delete or generate`() = testApplication {
        val fixture = createFixture("polling-global-phase-block", EventStatus.POLLING)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        insertTransportPlan(fixture.database, fixture.eventId, fixture.transportPlanId)

        application {
            module(fixture.database)
        }

        val readiness = client.get("/api/events/${fixture.eventId}/transport/readiness") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }
        val plans = client.get("/api/events/${fixture.eventId}/transport/plans") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }
        val departure = client.put(
            "/api/events/${fixture.eventId}/transport/departures/${fixture.confirmedParticipantId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(departureBody(fixture.confirmedParticipantId, "Paris Gare de Lyon"))
        }
        val select = client.post("/api/events/${fixture.eventId}/transport/plans/${fixture.transportPlanId}/select") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }
        val delete = client.delete("/api/events/${fixture.eventId}/transport/plans/${fixture.transportPlanId}") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }
        val generate = client.post("/api/events/${fixture.eventId}/transport/plans/generate") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(generatePlanBody())
        }

        listOf(readiness, plans, departure, select, delete, generate).forEach { response ->
            assertEquals(
                HttpStatusCode.Conflict,
                response.status,
                "POLLING events must block every transport route after authorization and before exposing logistics state"
            )
        }
    }

    @Test
    fun `non member receives forbidden on draft transport routes before phase conflict`() = testApplication {
        val fixture = createFixture("draft-non-member-order", EventStatus.DRAFT)
        val nonMemberToken = createTestJwt(fixture.nonMemberId)
        val client = createJsonClient()

        insertTransportPlan(fixture.database, fixture.eventId, fixture.transportPlanId)

        application {
            module(fixture.database)
        }

        val readiness = client.get("/api/events/${fixture.eventId}/transport/readiness") {
            header(HttpHeaders.Authorization, "Bearer $nonMemberToken")
        }
        val plans = client.get("/api/events/${fixture.eventId}/transport/plans") {
            header(HttpHeaders.Authorization, "Bearer $nonMemberToken")
        }
        val departure = client.put(
            "/api/events/${fixture.eventId}/transport/departures/${fixture.confirmedParticipantId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $nonMemberToken")
            contentType(ContentType.Application.Json)
            setBody(departureBody(fixture.confirmedParticipantId, "Paris Gare de Lyon"))
        }
        val select = client.post("/api/events/${fixture.eventId}/transport/plans/${fixture.transportPlanId}/select") {
            header(HttpHeaders.Authorization, "Bearer $nonMemberToken")
        }
        val delete = client.delete("/api/events/${fixture.eventId}/transport/plans/${fixture.transportPlanId}") {
            header(HttpHeaders.Authorization, "Bearer $nonMemberToken")
        }
        val generate = client.post("/api/events/${fixture.eventId}/transport/plans/generate") {
            header(HttpHeaders.Authorization, "Bearer $nonMemberToken")
            contentType(ContentType.Application.Json)
            setBody(generatePlanBody())
        }

        listOf(readiness, plans, departure, select, delete, generate).forEach { response ->
            assertEquals(
                HttpStatusCode.Forbidden,
                response.status,
                "Authorization must run before DRAFT/POLLING workflow phase conflicts for non-members"
            )
        }
    }

    @Test
    fun `generate refuses client supplied arbitrary destination and uses selected scenario destination only`() = testApplication {
        val fixture = createFixture("reject-client-destination", EventStatus.ORGANIZING)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        putAllDepartures(client, fixture, organizerToken)

        val response = client.post("/api/events/${fixture.eventId}/transport/plans/generate") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(
                """
                    {
                      "optimizationType": "BALANCED",
                      "destination": {
                        "name": "Mallory Island",
                        "address": "Untrusted client supplied destination",
                        "latitude": 1.0,
                        "longitude": 2.0
                      }
                    }
                """.trimIndent()
            )
        }

        assertFalse(
            response.status == HttpStatusCode.Created,
            "Transport generation must not accept an arbitrary destination supplied by the client"
        )
        assertFalse(
            response.bodyAsText().contains("Mallory Island"),
            "Rejected transport generation must not persist or echo an untrusted client destination as the planning anchor"
        )
    }

    @Test
    fun `generate refuses event without confirmed selected destination instead of falling back to event title`() = testApplication {
        val fixture = createFixture(
            suffix = "missing-selected-destination",
            status = EventStatus.ORGANIZING,
            includeSelectedScenario = false
        )
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        putAllDepartures(client, fixture, organizerToken)

        val response = client.post("/api/events/${fixture.eventId}/transport/plans/generate") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(generatePlanBody())
        }

        assertFalse(
            response.status == HttpStatusCode.Created,
            "Transport generation must require a confirmed scenario/destination and refuse event-title fallback"
        )
        assertFalse(
            response.bodyAsText().contains("Phase 4 Transport missing-selected-destination"),
            "The event title is not a valid transport destination fallback"
        )
    }

    @Test
    fun `organizer can mark transport as not needed through API and readiness exposes the decision`() = testApplication {
        val fixture = createFixture("not-needed-api", EventStatus.ORGANIZING)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val markNotNeeded = client.post("/api/events/${fixture.eventId}/transport/not-needed") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"Participants are already on site"}""")
        }
        val readiness = client.get("/api/events/${fixture.eventId}/transport/readiness") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }

        assertEquals(HttpStatusCode.OK, markNotNeeded.status)
        assertEquals(HttpStatusCode.OK, readiness.status)
        val body = readiness.bodyAsText()
        assertTrue(body.contains("transportNotNeeded"))
        assertTrue(body.contains("canFinalizeWithoutPlan"))
        assertTrue(body.contains("true"))
    }

    @Test
    fun `plan ids from another event cannot be read selected or deleted through current event path`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventA = createFixture("cross-event-a", EventStatus.ORGANIZING, database)
        val eventB = createFixture("cross-event-b", EventStatus.ORGANIZING, database)
        val organizerAToken = createTestJwt(eventA.organizerId)
        val client = createJsonClient()

        application {
            module(database)
        }

        val eventBPlanId = "transport-plan-cross-event-b-existing"
        insertTransportPlan(database, eventB.eventId, eventBPlanId)

        val readThroughEventA = client.get("/api/events/${eventA.eventId}/transport/plans/$eventBPlanId") {
            header(HttpHeaders.Authorization, "Bearer $organizerAToken")
        }
        val selectThroughEventA = client.post("/api/events/${eventA.eventId}/transport/plans/$eventBPlanId/select") {
            header(HttpHeaders.Authorization, "Bearer $organizerAToken")
        }
        val deleteThroughEventA = client.delete("/api/events/${eventA.eventId}/transport/plans/$eventBPlanId") {
            header(HttpHeaders.Authorization, "Bearer $organizerAToken")
        }

        assertTrue(
            readThroughEventA.status == HttpStatusCode.Forbidden ||
                readThroughEventA.status == HttpStatusCode.NotFound
        )
        assertTrue(
            selectThroughEventA.status == HttpStatusCode.Forbidden ||
                selectThroughEventA.status == HttpStatusCode.NotFound
        )
        assertTrue(
            deleteThroughEventA.status == HttpStatusCode.Forbidden ||
                deleteThroughEventA.status == HttpStatusCode.NotFound
        )
    }

    private suspend fun putAllDepartures(
        client: io.ktor.client.HttpClient,
        fixture: Phase4TransportFixture,
        organizerToken: String
    ) {
        val first = client.put("/api/events/${fixture.eventId}/transport/departures/${fixture.confirmedParticipantId}") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(departureBody(fixture.confirmedParticipantId, "Paris Gare de Lyon"))
        }
        val second = client.put(
            "/api/events/${fixture.eventId}/transport/departures/${fixture.secondConfirmedParticipantId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(departureBody(fixture.secondConfirmedParticipantId, "Lille Flandres"))
        }

        assertEquals(HttpStatusCode.OK, first.status)
        assertEquals(HttpStatusCode.OK, second.status)
    }

    private fun ApplicationTestBuilder.createJsonClient() = createClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun createFixture(
        suffix: String,
        status: EventStatus,
        database: WakeveDb = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:")),
        includeSelectedScenario: Boolean = true
    ): Phase4TransportFixture {
        val eventRepository = DatabaseEventRepository(database)

        val eventId = "event-phase4-$suffix"
        val organizerId = "organizer-$suffix"
        val confirmedParticipantId = "participant-confirmed-$suffix"
        val secondConfirmedParticipantId = "participant-second-confirmed-$suffix"
        val pendingParticipantId = "participant-pending-$suffix"
        val nonMemberId = "non-member-$suffix"
        val slotId = "slot-$suffix"
        val scenarioId = "scenario-selected-$suffix"
        val transportPlanId = "transport-plan-$suffix"
        val now = "2026-05-22T10:00:00Z"

        insertUser(database, organizerId, "Organizer $suffix")
        insertUser(database, confirmedParticipantId, "Confirmed Participant $suffix")
        insertUser(database, secondConfirmedParticipantId, "Second Confirmed Participant $suffix")
        insertUser(database, pendingParticipantId, "Pending Participant $suffix")
        insertUser(database, nonMemberId, "Non Member $suffix")

        val event = Event(
            id = eventId,
            title = "Phase 4 Transport $suffix",
            description = "Backend route contract coverage for Phase 4 transport planning",
            organizerId = organizerId,
            participants = emptyList(),
            proposedSlots = listOf(
                TimeSlot(
                    id = slotId,
                    start = "2026-06-21T14:00:00Z",
                    end = "2026-06-21T16:00:00Z",
                    timezone = "UTC",
                    timeOfDay = TimeOfDay.SPECIFIC
                )
            ),
            deadline = "2026-06-01T00:00:00Z",
            status = status,
            finalDate = "2026-06-21T14:00:00Z",
            createdAt = now,
            updatedAt = now,
            eventType = EventType.OTHER
        )

        runBlocking {
            eventRepository.createEvent(event).getOrThrow()
        }

        database.confirmedDateQueries.insertConfirmedDate(
            id = "confirmed-$suffix",
            eventId = eventId,
            timeslotId = slotId,
            confirmedByOrganizerId = organizerId,
            confirmedAt = now,
            updatedAt = now
        )
        database.participantQueries.insertParticipant(
            id = "part-confirmed-$suffix",
            eventId = eventId,
            userId = confirmedParticipantId,
            role = "PARTICIPANT",
            hasValidatedDate = 1,
            joinedAt = now,
            updatedAt = now
        )
        database.participantQueries.insertParticipant(
            id = "part-second-confirmed-$suffix",
            eventId = eventId,
            userId = secondConfirmedParticipantId,
            role = "PARTICIPANT",
            hasValidatedDate = 1,
            joinedAt = now,
            updatedAt = now
        )
        database.participantQueries.insertParticipant(
            id = "part-pending-$suffix",
            eventId = eventId,
            userId = pendingParticipantId,
            role = "PARTICIPANT",
            hasValidatedDate = 0,
            joinedAt = now,
            updatedAt = now
        )
        if (includeSelectedScenario) {
            database.scenarioQueries.insertScenario(
                id = scenarioId,
                eventId = eventId,
                name = "Selected destination $suffix",
                dateOrPeriod = "2026-06-21",
                location = "Lyon",
                duration = 2,
                estimatedParticipants = 4,
                estimatedBudgetPerPerson = 180.0,
                description = "Selected destination used as transport planning input",
                status = "SELECTED",
                createdAt = now,
                updatedAt = now
            )
        }

        return Phase4TransportFixture(
            database = database,
            eventId = eventId,
            organizerId = organizerId,
            confirmedParticipantId = confirmedParticipantId,
            secondConfirmedParticipantId = secondConfirmedParticipantId,
            pendingParticipantId = pendingParticipantId,
            nonMemberId = nonMemberId,
            transportPlanId = transportPlanId
        )
    }

    private fun insertUser(database: WakeveDb, userId: String, name: String) {
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

    private fun departureBody(participantId: String, name: String): String {
        return """
            {
              "participantId": "$participantId",
              "location": {
                "name": "$name",
                "address": "$name, France",
                "latitude": 48.8566,
                "longitude": 2.3522
              }
            }
        """.trimIndent()
    }

    private fun generatePlanBody(): String {
        return """
            {
              "optimizationType": "BALANCED"
            }
        """.trimIndent()
    }

    private fun insertTransportPlan(database: WakeveDb, eventId: String, planId: String) {
        database.transportQueries.insertPlan(
            id = planId,
            event_id = eventId,
            destination_json = json.encodeToString(TransportLocation(name = "Lyon", address = "Lyon, France")),
            optimization_type = OptimizationType.BALANCED.name,
            total_group_cost = 120.0,
            group_arrivals_json = json.encodeToString(listOf("2026-06-21T12:00:00Z")),
            created_at = "2026-05-22T10:00:00Z"
        )
    }

    private suspend fun io.ktor.client.statement.HttpResponse.extractId(): String {
        val parsed = json.parseToJsonElement(bodyAsText()).jsonObject
        return parsed["id"]?.jsonPrimitive?.content
            ?: parsed["planId"]?.jsonPrimitive?.content
            ?: parsed["transportPlanId"]?.jsonPrimitive?.content
            ?: error("Transport plan response must include id, planId, or transportPlanId")
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

    private data class Phase4TransportFixture(
        val database: WakeveDb,
        val eventId: String,
        val organizerId: String,
        val confirmedParticipantId: String,
        val secondConfirmedParticipantId: String,
        val pendingParticipantId: String,
        val nonMemberId: String,
        val transportPlanId: String
    )
}
