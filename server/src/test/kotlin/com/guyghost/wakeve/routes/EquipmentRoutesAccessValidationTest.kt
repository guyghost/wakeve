package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.equipment.EquipmentRepository
import com.guyghost.wakeve.models.EquipmentCategory
import com.guyghost.wakeve.models.EquipmentItem
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.ItemStatus
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
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Regression tests for QA BUG-B (API multi-user QA 2026-09-27): the equipment
 * ("who brings what") routes had no authorization at all.
 */
class EquipmentRoutesAccessValidationTest {
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
    fun `outsider cannot read create reassign change status or delete equipment`() = testApplication {
        val f = createFixture("outsider")
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(database = f.database, eventRepository = f.eventRepository, equipmentRepository = f.equipmentRepository) }
        val outsider = "Bearer ${jwt(f.outsiderId)}"
        val base = "/api/events/${f.eventId}/equipment"

        val reads = listOf(
            base,
            "$base/category/CAMPING",
            "$base/status/ASSIGNED",
            "$base/participant/${f.participantId}",
            "$base/statistics"
        ).associateWith { path -> client.get(path) { header(HttpHeaders.Authorization, outsider) }.status }
        reads.forEach { (path, status) -> assertEquals(HttpStatusCode.Forbidden, status, "GET $path") }

        val create = client.post(base) {
            header(HttpHeaders.Authorization, outsider)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Intrus","category":"OTHER","quantity":1}""")
        }
        val autoGenerate = client.post("$base/auto-generate") {
            header(HttpHeaders.Authorization, outsider)
            contentType(ContentType.Application.Json)
            setBody("""{"eventType":"CAMPING","participantCount":3}""")
        }
        val update = client.put("$base/${f.itemId}") {
            header(HttpHeaders.Authorization, outsider)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Renamed"}""")
        }
        val assign = client.put("$base/${f.itemId}/assign") {
            header(HttpHeaders.Authorization, outsider)
            contentType(ContentType.Application.Json)
            setBody("""{"participantId":"${f.outsiderId}"}""")
        }
        val status = client.put("$base/${f.itemId}/status") {
            header(HttpHeaders.Authorization, outsider)
            contentType(ContentType.Application.Json)
            setBody("""{"newStatus":"PACKED"}""")
        }
        val delete = client.delete("$base/${f.itemId}") { header(HttpHeaders.Authorization, outsider) }

        assertEquals(HttpStatusCode.Forbidden, create.status, create.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, autoGenerate.status, autoGenerate.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, update.status, update.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, assign.status, assign.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, status.status, status.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, delete.status, delete.bodyAsText())

        val item = assertNotNull(f.equipmentRepository.getEquipmentItemById(f.itemId))
        assertEquals(f.participantId, item.assignedTo)
        assertEquals(ItemStatus.ASSIGNED, item.status)
        assertEquals(1, f.equipmentRepository.getEquipmentItemsByEventId(f.eventId).size)
    }

    @Test
    fun `participants contribute while organizer keeps list management`() = testApplication {
        val f = createFixture("members")
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(database = f.database, eventRepository = f.eventRepository, equipmentRepository = f.equipmentRepository) }
        val participant = "Bearer ${jwt(f.participantId)}"
        val second = "Bearer ${jwt(f.secondParticipantId)}"
        val organizer = "Bearer ${jwt(f.organizerId)}"
        val base = "/api/events/${f.eventId}/equipment"

        val read = client.get(base) { header(HttpHeaders.Authorization, second) }
        assertEquals(HttpStatusCode.OK, read.status, read.bodyAsText())

        val create = client.post(base) {
            header(HttpHeaders.Authorization, second)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Thermos","category":"COOKING","quantity":1}""")
        }
        assertEquals(HttpStatusCode.Created, create.status, create.bodyAsText())

        // A participant cannot take over an item already assigned to someone else...
        val steal = client.put("$base/${f.itemId}/assign") {
            header(HttpHeaders.Authorization, second)
            contentType(ContentType.Application.Json)
            setBody("""{"participantId":"${f.secondParticipantId}"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, steal.status, steal.bodyAsText())
        // ...nor change its status or delete it.
        val foreignStatus = client.put("$base/${f.itemId}/status") {
            header(HttpHeaders.Authorization, second)
            contentType(ContentType.Application.Json)
            setBody("""{"newStatus":"CANCELLED"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, foreignStatus.status, foreignStatus.bodyAsText())
        val participantDelete = client.delete("$base/${f.itemId}") { header(HttpHeaders.Authorization, second) }
        assertEquals(HttpStatusCode.Forbidden, participantDelete.status, participantDelete.bodyAsText())

        // The assignee manages the status of what they bring.
        val ownStatus = client.put("$base/${f.itemId}/status") {
            header(HttpHeaders.Authorization, participant)
            contentType(ContentType.Application.Json)
            setBody("""{"newStatus":"PACKED"}""")
        }
        assertEquals(HttpStatusCode.OK, ownStatus.status, ownStatus.bodyAsText())

        // Assigning to someone outside the event is rejected, even for the organizer.
        val assignOutsider = client.put("$base/${f.itemId}/assign") {
            header(HttpHeaders.Authorization, organizer)
            contentType(ContentType.Application.Json)
            setBody("""{"participantId":"${f.outsiderId}"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, assignOutsider.status, assignOutsider.bodyAsText())

        // The organizer can reassign and delete.
        val reassign = client.put("$base/${f.itemId}/assign") {
            header(HttpHeaders.Authorization, organizer)
            contentType(ContentType.Application.Json)
            setBody("""{"participantId":"${f.secondParticipantId}"}""")
        }
        assertEquals(HttpStatusCode.OK, reassign.status, reassign.bodyAsText())
        val organizerDelete = client.delete("$base/${f.itemId}") { header(HttpHeaders.Authorization, organizer) }
        assertEquals(HttpStatusCode.NoContent, organizerDelete.status, organizerDelete.bodyAsText())
    }

    @Test
    fun `item from another event is not reachable through this event`() = testApplication {
        val f = createFixture("idor")
        val otherEventId = "equipment-event-idor-other"
        createEvent(f.eventRepository, otherEventId, f.outsiderId)
        val foreignItem = f.equipmentRepository.createEquipmentItem(item(otherEventId, "foreign-item", assignedTo = null))
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(database = f.database, eventRepository = f.eventRepository, equipmentRepository = f.equipmentRepository) }
        val organizer = "Bearer ${jwt(f.organizerId)}"
        val base = "/api/events/${f.eventId}/equipment"

        val assign = client.put("$base/${foreignItem.id}/assign") {
            header(HttpHeaders.Authorization, organizer)
            contentType(ContentType.Application.Json)
            setBody("""{"participantId":"${f.organizerId}"}""")
        }
        val status = client.put("$base/${foreignItem.id}/status") {
            header(HttpHeaders.Authorization, organizer)
            contentType(ContentType.Application.Json)
            setBody("""{"newStatus":"CANCELLED"}""")
        }
        val delete = client.delete("$base/${foreignItem.id}") { header(HttpHeaders.Authorization, organizer) }

        assertEquals(HttpStatusCode.NotFound, assign.status, assign.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, status.status, status.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, delete.status, delete.bodyAsText())
        assertNotNull(f.equipmentRepository.getEquipmentItemById(foreignItem.id))
    }

    @Test
    fun `invalid equipment payloads return bad request`() = testApplication {
        val f = createFixture("invalid")
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(database = f.database, eventRepository = f.eventRepository, equipmentRepository = f.equipmentRepository) }
        val organizer = "Bearer ${jwt(f.organizerId)}"
        val base = "/api/events/${f.eventId}/equipment"

        val unknownCategory = client.post(base) {
            header(HttpHeaders.Authorization, organizer)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Truc","category":"UNKNOWN","quantity":1}""")
        }
        val missingFields = client.post(base) {
            header(HttpHeaders.Authorization, organizer)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Truc"}""")
        }
        val badStatus = client.put("$base/${f.itemId}/status") {
            header(HttpHeaders.Authorization, organizer)
            contentType(ContentType.Application.Json)
            setBody("""{"newStatus":"LOST"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, unknownCategory.status, unknownCategory.bodyAsText())
        assertEquals(HttpStatusCode.BadRequest, missingFields.status, missingFields.bodyAsText())
        assertEquals(HttpStatusCode.BadRequest, badStatus.status, badStatus.bodyAsText())
    }

    private fun createFixture(suffix: String): Fixture {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val equipmentRepository = EquipmentRepository(database)
        val eventId = "equipment-event-$suffix"
        val organizerId = "equipment-organizer-$suffix"
        val participantId = "equipment-participant-$suffix"
        val secondParticipantId = "equipment-second-$suffix"
        val outsiderId = "equipment-outsider-$suffix"
        listOf(organizerId, participantId, secondParticipantId, outsiderId).forEach { insertUser(database, it) }
        createEvent(eventRepository, eventId, organizerId)
        runBlocking {
            eventRepository.addParticipant(eventId, participantId).getOrThrow()
            eventRepository.addParticipant(eventId, secondParticipantId).getOrThrow()
        }
        val item = equipmentRepository.createEquipmentItem(item(eventId, "crash-pad-$suffix", assignedTo = participantId))
        return Fixture(
            database, eventRepository, equipmentRepository, eventId,
            organizerId, participantId, secondParticipantId, outsiderId, item.id
        )
    }

    private fun createEvent(eventRepository: DatabaseEventRepository, eventId: String, organizerId: String) {
        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = eventId,
                    title = "Equipment $eventId",
                    description = "Equipment access test",
                    organizerId = organizerId,
                    participants = emptyList(),
                    proposedSlots = emptyList(),
                    deadline = "2026-12-20T00:00:00Z",
                    status = EventStatus.DRAFT,
                    createdAt = "2026-09-27T10:00:00Z",
                    updatedAt = "2026-09-27T10:00:00Z",
                    eventType = EventType.OTHER
                )
            ).getOrThrow()
        }
    }

    private fun item(eventId: String, id: String, assignedTo: String?) = EquipmentItem(
        id = id,
        eventId = eventId,
        name = "Crash pad",
        category = EquipmentCategory.SPORTS,
        quantity = 1,
        assignedTo = assignedTo,
        status = if (assignedTo != null) ItemStatus.ASSIGNED else ItemStatus.NEEDED,
        createdAt = "2026-09-27T10:00:00Z",
        updatedAt = "2026-09-27T10:00:00Z"
    )

    private fun insertUser(database: WakeveDb, userId: String) {
        database.userQueries.insertUser(
            id = userId,
            provider_id = "provider-$userId",
            email = "$userId@example.test",
            name = userId,
            avatar_url = null,
            provider = "google",
            role = "USER",
            created_at = "2026-09-27T10:00:00Z",
            updated_at = "2026-09-27T10:00:00Z"
        )
    }

    private fun jwt(userId: String): String =
        JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withClaim("sessionId", "test-session-$userId")
            .withClaim("permissions", listOf("READ", "WRITE"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))

    private data class Fixture(
        val database: WakeveDb,
        val eventRepository: DatabaseEventRepository,
        val equipmentRepository: EquipmentRepository,
        val eventId: String,
        val organizerId: String,
        val participantId: String,
        val secondParticipantId: String,
        val outsiderId: String,
        val itemId: String
    )
}
