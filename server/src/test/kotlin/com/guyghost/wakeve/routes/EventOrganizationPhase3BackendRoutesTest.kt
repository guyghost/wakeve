package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.accommodation.AccommodationRepository
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Accommodation
import com.guyghost.wakeve.models.AccommodationType
import com.guyghost.wakeve.models.BookingStatus
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioResponse
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.RoomAssignment
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.module
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.ScenarioRepository
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventOrganizationPhase3BackendRoutesTest {

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
    fun `confirmed participant can list scenarios for confirmed event`() = testApplication {
        val fixture = createFixture("scenario-list-confirmed", EventStatus.CONFIRMED)
        val participantToken = createTestJwt(fixture.confirmedParticipantId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.get("/api/events/${fixture.eventId}/scenarios") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains(fixture.scenarioName))
    }

    @Test
    fun `confirmed participant can list scenarios for comparing event`() = testApplication {
        val fixture = createFixture("scenario-list-comparing", EventStatus.COMPARING)
        val participantToken = createTestJwt(fixture.confirmedParticipantId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.get("/api/events/${fixture.eventId}/scenarios") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains(fixture.scenarioName))
    }

    @Test
    fun `pending participant cannot list scenarios or accommodations`() = testApplication {
        val fixture = createFixture("pending-access", EventStatus.CONFIRMED)
        val pendingToken = createTestJwt(fixture.pendingParticipantId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val scenarios = client.get("/api/events/${fixture.eventId}/scenarios") {
            header(HttpHeaders.Authorization, "Bearer $pendingToken")
        }
        val accommodations = client.get("/api/events/${fixture.eventId}/accommodation") {
            header(HttpHeaders.Authorization, "Bearer $pendingToken")
        }

        assertEquals(
            HttpStatusCode.Forbidden,
            scenarios.status,
            "Participants who have not validated the confirmed date must not access scenario details"
        )
        assertEquals(
            HttpStatusCode.Forbidden,
            accommodations.status,
            "Participants who have not validated the confirmed date must not access accommodation details"
        )
    }

    @Test
    fun `non member cannot vote on scenario`() = testApplication {
        val fixture = createFixture("non-member-vote", EventStatus.COMPARING)
        val nonMemberToken = createTestJwt(fixture.nonMemberId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.post("/api/scenarios/${fixture.scenarioId}/vote") {
            header(HttpHeaders.Authorization, "Bearer $nonMemberToken")
            contentType(ContentType.Application.Json)
            setBody(scenarioVoteBody(fixture.nonMemberId))
        }

        assertEquals(
            HttpStatusCode.Forbidden,
            response.status,
            "Scenario votes must be authorized against event membership, not a caller-controlled participantId"
        )
    }

    @Test
    fun `non organizer cannot create scenario or accommodation`() = testApplication {
        val fixture = createFixture("non-organizer-create", EventStatus.CONFIRMED)
        val participantToken = createTestJwt(fixture.confirmedParticipantId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val scenario = client.post("/api/events/${fixture.eventId}/scenarios") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
            contentType(ContentType.Application.Json)
            setBody(createScenarioBody(fixture, "Participant scenario"))
        }
        val accommodation = client.post("/api/events/${fixture.eventId}/accommodation") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
            contentType(ContentType.Application.Json)
            setBody(createAccommodationBody(fixture, "Participant hotel"))
        }

        assertEquals(HttpStatusCode.Forbidden, scenario.status)
        assertEquals(HttpStatusCode.Forbidden, accommodation.status)
    }

    @Test
    fun `organizer can create scenario and accommodation for event`() = testApplication {
        val fixture = createFixture("organizer-create", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val scenario = client.post("/api/events/${fixture.eventId}/scenarios") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(createScenarioBody(fixture, "Organizer scenario"))
        }
        val accommodation = client.post("/api/events/${fixture.eventId}/accommodation") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(createAccommodationBody(fixture, "Organizer hotel"))
        }

        assertEquals(HttpStatusCode.Created, scenario.status)
        assertEquals(HttpStatusCode.Created, accommodation.status)
    }

    @Test
    fun `organizer cannot create accommodation with mismatched event body`() = testApplication {
        val fixture = createFixture("accommodation-body-mismatch", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val repository = AccommodationRepository(fixture.database)
        val beforeCount = repository.getAccommodationsByEventId(fixture.eventId).size
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.post("/api/events/${fixture.eventId}/accommodation") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(createAccommodationBody(fixture, "Wrong event hotel", eventId = "another-event"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        assertEquals(
            beforeCount,
            repository.getAccommodationsByEventId(fixture.eventId).size,
            "Mismatched accommodation body must not create accommodation rows"
        )
    }

    @Test
    fun `organizer cannot create invalid accommodation payload`() = testApplication {
        val fixture = createFixture("invalid-accommodation-create", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val repository = AccommodationRepository(fixture.database)
        val beforeCount = repository.getAccommodationsByEventId(fixture.eventId).size
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.post("/api/events/${fixture.eventId}/accommodation") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(
                createAccommodationBody(
                    fixture = fixture,
                    name = "Invalid hotel",
                    address = "   ",
                    bookingUrl = "javascript:alert(1)",
                    checkInDate = "2026-06-24",
                    checkOutDate = "2026-06-22"
                )
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        assertEquals(
            beforeCount,
            repository.getAccommodationsByEventId(fixture.eventId).size,
            "Invalid accommodation payload must not create accommodation rows"
        )
    }

    @Test
    fun `organizer cannot update accommodation with invalid payload`() = testApplication {
        val fixture = createFixture("invalid-accommodation-update", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val repository = AccommodationRepository(fixture.database)
        val before = repository.getAccommodationById(fixture.accommodationId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.put("/api/events/${fixture.eventId}/accommodation/${fixture.accommodationId}") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(
                createAccommodationBody(
                    fixture = fixture,
                    name = "Invalid update",
                    address = "Too many nights lane",
                    totalNights = 366
                )
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        val after = repository.getAccommodationById(fixture.accommodationId)
        assertEquals(before?.name, after?.name)
        assertEquals(before?.totalNights, after?.totalNights)
        assertEquals(before?.totalCost, after?.totalCost)
    }

    @Test
    fun `organizer created accommodation is normalized before persistence`() = testApplication {
        val fixture = createFixture("normalized-accommodation-create", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.post("/api/events/${fixture.eventId}/accommodation") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(
                createAccommodationBody(
                    fixture = fixture,
                    name = "  Trimmed hotel  ",
                    address = "  3 rue Normalisee, Lyon  ",
                    bookingUrl = "  https://example.test/normalized  ",
                    notes = "  Bring towels  "
                )
            )
        }

        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        val created = json.decodeFromString<Accommodation>(response.bodyAsText())
        assertEquals("Trimmed hotel", created.name)
        assertEquals("3 rue Normalisee, Lyon", created.address)
        assertEquals("https://example.test/normalized", created.bookingUrl)
        assertEquals("Bring towels", created.notes)
        assertEquals(28_000, created.totalCost)
    }

    @Test
    fun `organizer cannot create scenario with mismatched event body`() = testApplication {
        val fixture = createFixture("scenario-body-mismatch", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val beforeCount = ScenarioRepository(fixture.database).getScenariosByEventId(fixture.eventId).size
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.post("/api/events/${fixture.eventId}/scenarios") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(createScenarioBody(fixture, "Wrong event scenario", eventId = "another-event"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        assertEquals(
            beforeCount,
            ScenarioRepository(fixture.database).getScenariosByEventId(fixture.eventId).size,
            "Mismatched scenario body must not create a scenario"
        )
    }

    @Test
    fun `organizer cannot create invalid scenario payload`() = testApplication {
        val fixture = createFixture("invalid-scenario-create", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val repository = ScenarioRepository(fixture.database)
        val beforeCount = repository.getScenariosByEventId(fixture.eventId).size
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.post("/api/events/${fixture.eventId}/scenarios") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(
                createScenarioBody(
                    fixture = fixture,
                    name = "Scenario impossible",
                    duration = 366,
                    estimatedBudgetPerPerson = 1_000_001.0
                )
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        assertEquals(
            beforeCount,
            repository.getScenariosByEventId(fixture.eventId).size,
            "Invalid scenario payload must not create scenario rows"
        )
    }

    @Test
    fun `organizer created scenario is normalized before persistence`() = testApplication {
        val fixture = createFixture("normalized-scenario-create", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.post("/api/events/${fixture.eventId}/scenarios") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(
                createScenarioBody(
                    fixture = fixture,
                    name = "  Trimmed scenario  ",
                    dateOrPeriod = "  2026-06-22  ",
                    location = "  Lyon  ",
                    description = "  Scenario created through backend API  ",
                    generationType = "manual"
                )
            )
        }

        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        val created = json.decodeFromString<ScenarioResponse>(response.bodyAsText())
        assertEquals("Trimmed scenario", created.name)
        assertEquals("2026-06-22", created.dateOrPeriod)
        assertEquals("Lyon", created.location)
        assertEquals("Scenario created through backend API", created.description)
        assertEquals("MANUAL", created.generationType)
    }

    @Test
    fun `organizer cannot update scenario with invalid payload`() = testApplication {
        val fixture = createFixture("invalid-scenario-update", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val repository = ScenarioRepository(fixture.database)
        val before = repository.getScenarioById(fixture.scenarioId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.put("/api/scenarios/${fixture.scenarioId}") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(
                updateScenarioBody(
                    name = "Invalid update",
                    duration = 366,
                    estimatedBudgetPerPerson = 1_000_001.0
                )
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        val after = repository.getScenarioById(fixture.scenarioId)
        assertEquals(before?.name, after?.name)
        assertEquals(before?.duration, after?.duration)
        assertEquals(before?.estimatedBudgetPerPerson, after?.estimatedBudgetPerPerson)
    }

    @Test
    fun `confirmed participant cannot read accommodation from another event through authorized event path`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventA = createFixture("idor-read-event-a", EventStatus.CONFIRMED, database)
        val eventB = createFixture("idor-read-event-b", EventStatus.CONFIRMED, database)
        val participantAToken = createTestJwt(eventA.confirmedParticipantId)
        val client = createJsonClient()

        application {
            module(database)
        }

        val response = client.get("/api/events/${eventA.eventId}/accommodation/${eventB.accommodationId}") {
            header(HttpHeaders.Authorization, "Bearer $participantAToken")
        }

        assertTrue(
            response.status == HttpStatusCode.Forbidden || response.status == HttpStatusCode.NotFound,
            "Event A participant must not read Event B accommodation through Event A path; status was ${response.status}"
        )
    }

    @Test
    fun `organizer cannot mutate accommodation from another event through authorized event path`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventA = createFixture("idor-mut-accommodation-a", EventStatus.CONFIRMED, database)
        val eventB = createFixture("idor-mut-accommodation-b", EventStatus.CONFIRMED, database)
        val organizerAToken = createTestJwt(eventA.organizerId)
        val client = createJsonClient()

        application {
            module(database)
        }

        val update = client.put("/api/events/${eventA.eventId}/accommodation/${eventB.accommodationId}") {
            header(HttpHeaders.Authorization, "Bearer $organizerAToken")
            contentType(ContentType.Application.Json)
            setBody(createAccommodationBody(eventA, "Cross-event rewrite"))
        }
        val afterUpdate = AccommodationRepository(database).getAccommodationById(eventB.accommodationId)

        assertTrue(
            update.status == HttpStatusCode.Forbidden || update.status == HttpStatusCode.NotFound,
            "Event A organizer must not update Event B accommodation through Event A path; status was ${update.status}"
        )
        assertEquals(eventB.accommodationName, afterUpdate?.name, "Event B accommodation must remain unchanged")

        val delete = client.delete("/api/events/${eventA.eventId}/accommodation/${eventB.accommodationId}") {
            header(HttpHeaders.Authorization, "Bearer $organizerAToken")
        }
        val afterDelete = AccommodationRepository(database).getAccommodationById(eventB.accommodationId)

        assertTrue(
            delete.status == HttpStatusCode.Forbidden || delete.status == HttpStatusCode.NotFound,
            "Event A organizer must not delete Event B accommodation through Event A path; status was ${delete.status}"
        )
        assertEquals(eventB.accommodationName, afterDelete?.name, "Event B accommodation must still exist")
    }

    @Test
    fun `confirmed participant cannot read rooms from another event through authorized event path`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventA = createFixture("idor-read-room-a", EventStatus.CONFIRMED, database)
        val eventB = createFixture("idor-read-room-b", EventStatus.CONFIRMED, database)
        val participantAToken = createTestJwt(eventA.confirmedParticipantId)
        val client = createJsonClient()

        application {
            module(database)
        }

        val response = client.get("/api/events/${eventA.eventId}/accommodation/${eventB.accommodationId}/rooms") {
            header(HttpHeaders.Authorization, "Bearer $participantAToken")
        }

        assertTrue(
            response.status == HttpStatusCode.Forbidden || response.status == HttpStatusCode.NotFound,
            "Event A participant must not read Event B rooms through Event A path; status was ${response.status}"
        )
    }

    @Test
    fun `organizer cannot mutate room from another event through authorized event path`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventA = createFixture("idor-mut-room-a", EventStatus.CONFIRMED, database)
        val eventB = createFixture("idor-mut-room-b", EventStatus.CONFIRMED, database)
        val organizerAToken = createTestJwt(eventA.organizerId)
        val client = createJsonClient()

        application {
            module(database)
        }

        val update = client.put(
            "/api/events/${eventA.eventId}/accommodation/${eventA.accommodationId}/rooms/${eventB.roomId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerAToken")
            contentType(ContentType.Application.Json)
            setBody(createRoomBody(eventA, "Cross-event room"))
        }
        val afterUpdate = AccommodationRepository(database).getRoomAssignmentById(eventB.roomId)

        assertTrue(
            update.status == HttpStatusCode.Forbidden || update.status == HttpStatusCode.NotFound,
            "Event A organizer must not update Event B room through Event A path; status was ${update.status}"
        )
        assertEquals(eventB.roomNumber, afterUpdate?.roomNumber, "Event B room must remain unchanged")

        val delete = client.delete(
            "/api/events/${eventA.eventId}/accommodation/${eventA.accommodationId}/rooms/${eventB.roomId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerAToken")
        }
        val afterDelete = AccommodationRepository(database).getRoomAssignmentById(eventB.roomId)

        assertTrue(
            delete.status == HttpStatusCode.Forbidden || delete.status == HttpStatusCode.NotFound,
            "Event A organizer must not delete Event B room through Event A path; status was ${delete.status}"
        )
        assertEquals(eventB.roomNumber, afterDelete?.roomNumber, "Event B room must still exist")
    }

    @Test
    fun `organizer cannot assign room to non member or blank participant`() = testApplication {
        val fixture = createFixture("room-assignment-membership", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val repository = AccommodationRepository(fixture.database)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val beforeRooms = repository.getRoomAssignmentsByAccommodationId(fixture.accommodationId)
        val nonMemberCreate = client.post(
            "/api/events/${fixture.eventId}/accommodation/${fixture.accommodationId}/rooms"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(createRoomBodyWithParticipants(fixture, "Non-member room", listOf(fixture.nonMemberId)))
        }

        assertEquals(HttpStatusCode.BadRequest, nonMemberCreate.status, nonMemberCreate.bodyAsText())
        assertEquals(
            beforeRooms.size,
            repository.getRoomAssignmentsByAccommodationId(fixture.accommodationId).size,
            "Invalid room assignment must not create a room"
        )

        val blankParticipantCreate = client.post(
            "/api/events/${fixture.eventId}/accommodation/${fixture.accommodationId}/rooms"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(createRoomBodyWithParticipants(fixture, "Blank participant room", listOf("   ")))
        }

        assertEquals(HttpStatusCode.BadRequest, blankParticipantCreate.status, blankParticipantCreate.bodyAsText())
    }

    @Test
    fun `organizer cannot create room with mismatched accommodation body`() = testApplication {
        val fixture = createFixture("room-body-mismatch", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val repository = AccommodationRepository(fixture.database)
        val beforeRooms = repository.getRoomAssignmentsByAccommodationId(fixture.accommodationId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.post(
            "/api/events/${fixture.eventId}/accommodation/${fixture.accommodationId}/rooms"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(
                createRoomBodyWithParticipants(
                    fixture = fixture,
                    roomNumber = "Mismatched room",
                    participantIds = listOf(fixture.confirmedParticipantId),
                    accommodationId = "another-accommodation"
                )
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        assertEquals(
            beforeRooms.size,
            repository.getRoomAssignmentsByAccommodationId(fixture.accommodationId).size,
            "Mismatched room body must not create room rows"
        )
    }

    @Test
    fun `organizer created room assignment is normalized before persistence`() = testApplication {
        val fixture = createFixture("room-normalized-create", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.post(
            "/api/events/${fixture.eventId}/accommodation/${fixture.accommodationId}/rooms"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(
                createRoomBodyWithParticipants(
                    fixture = fixture,
                    roomNumber = "  Trimmed room  ",
                    participantIds = listOf("  ${fixture.confirmedParticipantId}  ")
                )
            )
        }

        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        val created = json.decodeFromString<RoomAssignment>(response.bodyAsText())
        assertEquals("Trimmed room", created.roomNumber)
        assertEquals(listOf(fixture.confirmedParticipantId), created.assignedParticipants)
    }

    @Test
    fun `organizer cannot update room assignment with non member participant`() = testApplication {
        val fixture = createFixture("room-update-membership", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val repository = AccommodationRepository(fixture.database)
        val beforeRoom = repository.getRoomAssignmentById(fixture.roomId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.put(
            "/api/events/${fixture.eventId}/accommodation/${fixture.accommodationId}/rooms/${fixture.roomId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(createRoomBodyWithParticipants(fixture, "Invalid room update", listOf(fixture.confirmedParticipantId, fixture.nonMemberId)))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        val afterRoom = repository.getRoomAssignmentById(fixture.roomId)
        assertEquals(beforeRoom?.roomNumber, afterRoom?.roomNumber)
        assertEquals(beforeRoom?.assignedParticipants, afterRoom?.assignedParticipants)
    }

    @Test
    fun `organizer cannot update room with mismatched accommodation body`() = testApplication {
        val fixture = createFixture("room-update-body-mismatch", EventStatus.CONFIRMED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val repository = AccommodationRepository(fixture.database)
        val beforeRoom = repository.getRoomAssignmentById(fixture.roomId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.put(
            "/api/events/${fixture.eventId}/accommodation/${fixture.accommodationId}/rooms/${fixture.roomId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(
                createRoomBodyWithParticipants(
                    fixture = fixture,
                    roomNumber = "Mismatched update",
                    participantIds = listOf(fixture.confirmedParticipantId),
                    accommodationId = "another-accommodation"
                )
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        val afterRoom = repository.getRoomAssignmentById(fixture.roomId)
        assertEquals(beforeRoom?.roomNumber, afterRoom?.roomNumber)
        assertEquals(beforeRoom?.assignedParticipants, afterRoom?.assignedParticipants)
    }


    @Test
    fun `organizer cannot create logistics before confirmed or after finalized workflow status`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val disallowedFixtures = listOf(
            createFixture("create-guard-draft", EventStatus.DRAFT, database),
            createFixture("create-guard-polling", EventStatus.POLLING, database),
            createFixture("create-guard-finalized", EventStatus.FINALIZED, database)
        )
        val client = createJsonClient()

        application {
            module(database)
        }

        disallowedFixtures.forEach { fixture ->
            val organizerToken = createTestJwt(fixture.organizerId)
            val scenario = client.post("/api/events/${fixture.eventId}/scenarios") {
                header(HttpHeaders.Authorization, "Bearer $organizerToken")
                contentType(ContentType.Application.Json)
                setBody(createScenarioBody(fixture, "Blocked ${fixture.eventId}"))
            }
            val accommodation = client.post("/api/events/${fixture.eventId}/accommodation") {
                header(HttpHeaders.Authorization, "Bearer $organizerToken")
                contentType(ContentType.Application.Json)
                setBody(createAccommodationBody(fixture, "Blocked ${fixture.eventId}"))
            }

            assertEquals(
                HttpStatusCode.Conflict,
                scenario.status,
                "Scenario creation must be blocked while ${fixture.eventId} is ${fixture.status}"
            )
            assertEquals(
                HttpStatusCode.Conflict,
                accommodation.status,
                "Accommodation creation must be blocked while ${fixture.eventId} is ${fixture.status}"
            )
        }
    }

    @Test
    fun `organizer cannot update scenario after event is finalized`() = testApplication {
        val fixture = createFixture("finalized-scenario-update", EventStatus.FINALIZED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.put("/api/scenarios/${fixture.scenarioId}") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(updateScenarioBody("Changed after finalization"))
        }

        assertEquals(
            HttpStatusCode.Conflict,
            response.status,
            "Finalized events must be read-only: scenario update should be blocked"
        )
        assertEquals(
            fixture.scenarioName,
            ScenarioRepository(fixture.database).getScenarioById(fixture.scenarioId)?.name,
            "Finalized scenario must remain unchanged"
        )
    }

    @Test
    fun `organizer cannot delete scenario after event is finalized`() = testApplication {
        val fixture = createFixture("finalized-scenario-delete", EventStatus.FINALIZED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.delete("/api/scenarios/${fixture.scenarioId}") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }

        assertEquals(
            HttpStatusCode.Conflict,
            response.status,
            "Finalized events must be read-only: scenario delete should be blocked"
        )
        assertEquals(
            fixture.scenarioName,
            ScenarioRepository(fixture.database).getScenarioById(fixture.scenarioId)?.name,
            "Finalized scenario must still exist"
        )
    }

    @Test
    fun `organizer cannot update accommodation after event is finalized`() = testApplication {
        val fixture = createFixture("finalized-accommodation-update", EventStatus.FINALIZED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.put("/api/events/${fixture.eventId}/accommodation/${fixture.accommodationId}") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(createAccommodationBody(fixture, "Changed after finalization"))
        }

        assertEquals(
            HttpStatusCode.Conflict,
            response.status,
            "Finalized events must be read-only: accommodation update should be blocked"
        )
        assertEquals(
            fixture.accommodationName,
            AccommodationRepository(fixture.database).getAccommodationById(fixture.accommodationId)?.name,
            "Finalized accommodation must remain unchanged"
        )
    }

    @Test
    fun `organizer cannot delete accommodation after event is finalized`() = testApplication {
        val fixture = createFixture("finalized-accommodation-delete", EventStatus.FINALIZED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.delete("/api/events/${fixture.eventId}/accommodation/${fixture.accommodationId}") {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }

        assertEquals(
            HttpStatusCode.Conflict,
            response.status,
            "Finalized events must be read-only: accommodation delete should be blocked"
        )
        assertEquals(
            fixture.accommodationName,
            AccommodationRepository(fixture.database).getAccommodationById(fixture.accommodationId)?.name,
            "Finalized accommodation must still exist"
        )
    }

    @Test
    fun `organizer cannot update room assignment after event is finalized`() = testApplication {
        val fixture = createFixture("finalized-room-update", EventStatus.FINALIZED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.put(
            "/api/events/${fixture.eventId}/accommodation/${fixture.accommodationId}/rooms/${fixture.roomId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
            contentType(ContentType.Application.Json)
            setBody(createRoomBody(fixture, "Changed after finalization"))
        }

        assertEquals(
            HttpStatusCode.Conflict,
            response.status,
            "Finalized events must be read-only: room update should be blocked"
        )
        assertEquals(
            fixture.roomNumber,
            AccommodationRepository(fixture.database).getRoomAssignmentById(fixture.roomId)?.roomNumber,
            "Finalized room assignment must remain unchanged"
        )
    }

    @Test
    fun `organizer cannot delete room assignment after event is finalized`() = testApplication {
        val fixture = createFixture("finalized-room-delete", EventStatus.FINALIZED)
        val organizerToken = createTestJwt(fixture.organizerId)
        val client = createJsonClient()

        application {
            module(fixture.database)
        }

        val response = client.delete(
            "/api/events/${fixture.eventId}/accommodation/${fixture.accommodationId}/rooms/${fixture.roomId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $organizerToken")
        }

        assertEquals(
            HttpStatusCode.Conflict,
            response.status,
            "Finalized events must be read-only: room delete should be blocked"
        )
        assertEquals(
            fixture.roomNumber,
            AccommodationRepository(fixture.database).getRoomAssignmentById(fixture.roomId)?.roomNumber,
            "Finalized room assignment must still exist"
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
        database: WakeveDb = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
    ): Phase3BackendFixture {
        val eventRepository = DatabaseEventRepository(database)
        val scenarioRepository = ScenarioRepository(database)
        val accommodationRepository = AccommodationRepository(database)

        val eventId = "event-phase3-$suffix"
        val organizerId = "organizer-$suffix"
        val confirmedParticipantId = "participant-confirmed-$suffix"
        val pendingParticipantId = "participant-pending-$suffix"
        val nonMemberId = "non-member-$suffix"
        val slotId = "slot-$suffix"
        val scenarioId = "scenario-$suffix"
        val scenarioName = "Scenario $suffix"
        val accommodationId = "accommodation-$suffix"
        val accommodationName = "Hotel $suffix"
        val roomId = "room-$suffix"
        val roomNumber = "Room $suffix"
        val now = "2026-05-22T10:00:00Z"

        insertUser(database, organizerId, "Organizer $suffix")
        insertUser(database, confirmedParticipantId, "Confirmed Participant $suffix")
        insertUser(database, pendingParticipantId, "Pending Participant $suffix")
        insertUser(database, nonMemberId, "Non Member $suffix")

        val event = Event(
            id = eventId,
            title = "Phase 3 Backend $suffix",
            description = "Backend route contract coverage for Phase 3 scenario and accommodation access",
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
            id = "part-pending-$suffix",
            eventId = eventId,
            userId = pendingParticipantId,
            role = "PARTICIPANT",
            hasValidatedDate = 0,
            joinedAt = now,
            updatedAt = now
        )

        runBlocking {
            scenarioRepository.createScenario(
                Scenario(
                    id = scenarioId,
                    eventId = eventId,
                    name = scenarioName,
                    dateOrPeriod = "2026-06-21",
                    location = "Paris",
                    duration = 2,
                    estimatedParticipants = 4,
                    estimatedBudgetPerPerson = 120.0,
                    description = "Seeded scenario for access-control tests",
                    status = ScenarioStatus.PROPOSED,
                    createdAt = now,
                    updatedAt = now
                )
            ).getOrThrow()
        }

        accommodationRepository.createAccommodation(
            Accommodation(
                id = accommodationId,
                eventId = eventId,
                name = accommodationName,
                type = AccommodationType.HOTEL,
                address = "1 rue de Test, Paris",
                capacity = 4,
                pricePerNight = 12_000,
                totalNights = 2,
                totalCost = 24_000,
                bookingStatus = BookingStatus.RESERVED,
                bookingUrl = "https://example.test/booking/$suffix",
                checkInDate = "2026-06-21",
                checkOutDate = "2026-06-23",
                notes = "Seeded accommodation for access-control tests",
                createdAt = now,
                updatedAt = now
            )
        )
        accommodationRepository.createRoomAssignment(
            RoomAssignment(
                id = roomId,
                accommodationId = accommodationId,
                roomNumber = roomNumber,
                capacity = 2,
                assignedParticipants = listOf(confirmedParticipantId),
                priceShare = 12_000,
                createdAt = now,
                updatedAt = now
            )
        )

        return Phase3BackendFixture(
            database = database,
            eventId = eventId,
            status = status,
            organizerId = organizerId,
            confirmedParticipantId = confirmedParticipantId,
            pendingParticipantId = pendingParticipantId,
            nonMemberId = nonMemberId,
            scenarioId = scenarioId,
            scenarioName = scenarioName,
            accommodationId = accommodationId,
            accommodationName = accommodationName,
            roomId = roomId,
            roomNumber = roomNumber
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

    private fun scenarioVoteBody(participantId: String): String {
        return """
            {
              "participantId": "$participantId",
              "vote": "PREFER"
            }
        """.trimIndent()
    }

    private fun createScenarioBody(
        fixture: Phase3BackendFixture,
        name: String,
        eventId: String = fixture.eventId,
        dateOrPeriod: String = "2026-06-22",
        location: String = "Lyon",
        duration: Int = 2,
        estimatedParticipants: Int = 4,
        estimatedBudgetPerPerson: Double = 150.0,
        description: String = "Scenario created through backend API",
        generationType: String? = null
    ): String {
        val generationTypeJson = generationType?.let { ""","generationType": "$it"""" } ?: ""
        return """
            {
              "eventId": "$eventId",
              "name": "$name",
              "dateOrPeriod": "$dateOrPeriod",
              "location": "$location",
              "duration": $duration,
              "estimatedParticipants": $estimatedParticipants,
              "estimatedBudgetPerPerson": $estimatedBudgetPerPerson,
              "description": "$description"$generationTypeJson
            }
        """.trimIndent()
    }

    private fun updateScenarioBody(
        name: String,
        dateOrPeriod: String = "2026-06-22",
        location: String = "Lyon",
        duration: Int = 2,
        estimatedParticipants: Int = 4,
        estimatedBudgetPerPerson: Double = 150.0,
        description: String = "Scenario updated through backend API",
        status: String = "PROPOSED",
        generationType: String? = null
    ): String {
        val generationTypeJson = generationType?.let { ""","generationType": "$it"""" } ?: ""
        return """
            {
              "name": "$name",
              "dateOrPeriod": "$dateOrPeriod",
              "location": "$location",
              "duration": $duration,
              "estimatedParticipants": $estimatedParticipants,
              "estimatedBudgetPerPerson": $estimatedBudgetPerPerson,
              "description": "$description",
              "status": "$status"$generationTypeJson
            }
        """.trimIndent()
    }

    private fun createAccommodationBody(
        fixture: Phase3BackendFixture,
        name: String,
        eventId: String = fixture.eventId,
        address: String = "2 rue API, Lyon",
        pricePerNight: Long = 14_000,
        totalNights: Int = 2,
        bookingUrl: String = "https://example.test/api-booking",
        checkInDate: String = "2026-06-22",
        checkOutDate: String = "2026-06-24",
        notes: String = "Created through backend API"
    ): String {
        return """
            {
              "eventId": "$eventId",
              "name": "$name",
              "type": "HOTEL",
              "address": "$address",
              "capacity": 4,
              "pricePerNight": $pricePerNight,
              "totalNights": $totalNights,
              "bookingStatus": "RESERVED",
              "bookingUrl": "$bookingUrl",
              "checkInDate": "$checkInDate",
              "checkOutDate": "$checkOutDate",
              "notes": "$notes"
            }
        """.trimIndent()
    }

    private fun createRoomBody(fixture: Phase3BackendFixture, roomNumber: String): String {
        return createRoomBodyWithParticipants(fixture, roomNumber, listOf(fixture.confirmedParticipantId))
    }

    private fun createRoomBodyWithParticipants(
        fixture: Phase3BackendFixture,
        roomNumber: String,
        participantIds: List<String>,
        accommodationId: String = fixture.accommodationId
    ): String {
        val participantsJson = participantIds.joinToString(", ") { "\"$it\"" }
        return """
            {
              "accommodationId": "$accommodationId",
              "roomNumber": "$roomNumber",
              "capacity": 2,
              "assignedParticipants": [$participantsJson]
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

    private data class Phase3BackendFixture(
        val database: WakeveDb,
        val eventId: String,
        val status: EventStatus,
        val organizerId: String,
        val confirmedParticipantId: String,
        val pendingParticipantId: String,
        val nonMemberId: String,
        val scenarioId: String,
        val scenarioName: String,
        val accommodationId: String,
        val accommodationName: String,
        val roomId: String,
        val roomNumber: String
    )
}
