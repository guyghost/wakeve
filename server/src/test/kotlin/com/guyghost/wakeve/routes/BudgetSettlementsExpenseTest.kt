package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
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
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression test for QA BUG-C (API multi-user QA 2026-09-27): expenses
 * advanced by a participant (`POST /budget/expenses`) were ignored by the
 * settlement computation, which only read budget items.
 */
class BudgetSettlementsExpenseTest {
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
    fun `advanced expense is reflected in settlements and participant balance`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val eventId = "wedding-event"
        val organizerId = "wedding-organizer"
        val karim = "karim"
        val sophie = "sophie"
        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = eventId,
                    title = "Mariage",
                    description = "Settlement with advanced expenses",
                    organizerId = organizerId,
                    participants = emptyList(),
                    proposedSlots = emptyList(),
                    deadline = "2026-12-20T00:00:00Z",
                    status = EventStatus.ORGANIZING,
                    finalDate = "2027-06-10T08:00:00Z",
                    createdAt = "2026-09-27T10:00:00Z",
                    updatedAt = "2026-09-27T10:00:00Z",
                    eventType = EventType.OTHER
                )
            ).getOrThrow()
        }
        listOf(karim, sophie).forEach { confirmParticipant(database, eventId, it) }
        BudgetRepository(database).createBudget(eventId)

        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(database, eventRepository) }

        val expense = client.post("/api/events/$eventId/budget/expenses") {
            header(HttpHeaders.Authorization, "Bearer ${jwt(karim)}")
            contentType(ContentType.Application.Json)
            setBody("""{"amount":1900,"category":"OTHER","payerId":"$karim","splitParticipantIds":["$sophie","$karim"]}""")
        }
        assertEquals(HttpStatusCode.Created, expense.status, expense.bodyAsText())

        val settlements = client.get("/api/events/$eventId/budget/settlements") {
            header(HttpHeaders.Authorization, "Bearer ${jwt(organizerId)}")
        }
        assertEquals(HttpStatusCode.OK, settlements.status, settlements.bodyAsText())
        val body = json.parseToJsonElement(settlements.bodyAsText()).jsonObject
        assertEquals(1, body.getValue("count").jsonPrimitive.int, settlements.bodyAsText())
        val settlement = body.getValue("settlements").jsonArray.single().jsonObject
        assertEquals(sophie, settlement.getValue("fromParticipantId").jsonPrimitive.content)
        assertEquals(karim, settlement.getValue("toParticipantId").jsonPrimitive.content)
        assertEquals(950.0, settlement.getValue("amount").jsonPrimitive.double, 0.001)

        val karimInfo = client.get("/api/events/$eventId/budget/participants/$karim") {
            header(HttpHeaders.Authorization, "Bearer ${jwt(karim)}")
        }
        assertEquals(HttpStatusCode.OK, karimInfo.status, karimInfo.bodyAsText())
        assertEquals(
            -950.0,
            json.parseToJsonElement(karimInfo.bodyAsText()).jsonObject.getValue("balance").jsonPrimitive.double,
            0.001
        )
    }

    private fun confirmParticipant(database: WakeveDb, eventId: String, userId: String) {
        database.participantQueries.insertParticipant(
            id = "participant-$userId",
            eventId = eventId,
            userId = userId,
            role = "PARTICIPANT",
            hasValidatedDate = 1,
            joinedAt = "2026-09-27T10:00:00Z",
            updatedAt = "2026-09-27T10:00:00Z"
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
}
