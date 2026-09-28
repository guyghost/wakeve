package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.BudgetCategory
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.module
import com.guyghost.wakeve.repository.DatabaseEventRepository
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for QA BUG-D (API multi-user QA 2026-09-27):
 * `GET /budget/items`, `/budget/summary` and `/budget/statistics` always
 * answered 500 because their heterogeneous `mapOf(...)` bodies could not be
 * serialized by kotlinx.serialization.
 */
class BudgetReadRoutesSerializationTest {
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
    fun `budget items summary and statistics are readable by the organizer`() = testApplication {
        val f = createFixture()
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(f.database, f.eventRepository) }
        val auth = "Bearer ${jwt(f.organizerId)}"
        val base = "/api/events/${f.eventId}/budget"

        val items = client.get("$base/items") { header(HttpHeaders.Authorization, auth) }
        assertEquals(HttpStatusCode.OK, items.status, items.bodyAsText())
        val itemsBody = json.parseToJsonElement(items.bodyAsText()).jsonObject
        assertEquals(2, itemsBody.getValue("count").jsonPrimitive.int)
        assertEquals(2, itemsBody.getValue("items").jsonArray.size)

        val filtered = client.get("$base/items?category=meals") { header(HttpHeaders.Authorization, auth) }
        assertEquals(HttpStatusCode.OK, filtered.status, filtered.bodyAsText())
        assertEquals(1, json.parseToJsonElement(filtered.bodyAsText()).jsonObject.getValue("count").jsonPrimitive.int)

        val summary = client.get("$base/summary") { header(HttpHeaders.Authorization, auth) }
        assertEquals(HttpStatusCode.OK, summary.status, summary.bodyAsText())
        val summaryBody = json.parseToJsonElement(summary.bodyAsText()).jsonObject
        assertEquals(2, summaryBody.getValue("itemCount").jsonPrimitive.int)
        assertEquals(f.eventId, summaryBody.getValue("budget").jsonObject.getValue("eventId").jsonPrimitive.content)
        assertTrue(
            summaryBody.getValue("summary").jsonPrimitive.content.contains("Budget Summary"),
            summary.bodyAsText()
        )

        val statistics = client.get("$base/statistics") { header(HttpHeaders.Authorization, auth) }
        assertEquals(HttpStatusCode.OK, statistics.status, statistics.bodyAsText())
        val statsBody = json.parseToJsonElement(statistics.bodyAsText()).jsonObject
        assertEquals(2L, statsBody.getValue("totalItems").jsonPrimitive.long)
        assertEquals(0L, statsBody.getValue("paidItems").jsonPrimitive.long)
        assertEquals(2L, statsBody.getValue("unpaidItems").jsonPrimitive.long)
        val categories = statsBody.getValue("categoryStatistics").jsonArray.map { it.jsonObject }
        assertEquals(setOf("MEALS", "ACTIVITIES"), categories.map { it.getValue("category").jsonPrimitive.content }.toSet())
        val meals = categories.single { it.getValue("category").jsonPrimitive.content == "MEALS" }
        assertEquals(1, meals.getValue("count").jsonPrimitive.int)
        assertEquals(50.0, meals.getValue("estimatedTotal").jsonPrimitive.double)
    }

    @Test
    fun `budget read routes reject invalid filters and outsiders`() = testApplication {
        val f = createFixture()
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(f.database, f.eventRepository) }
        val base = "/api/events/${f.eventId}/budget"

        val badCategory = client.get("$base/items?category=PALEO") {
            header(HttpHeaders.Authorization, "Bearer ${jwt(f.organizerId)}")
        }
        assertEquals(HttpStatusCode.BadRequest, badCategory.status, badCategory.bodyAsText())

        listOf("$base/items", "$base/summary", "$base/statistics").forEach { path ->
            val response = client.get(path) { header(HttpHeaders.Authorization, "Bearer ${jwt("budget-outsider")}") }
            assertEquals(HttpStatusCode.Forbidden, response.status, "GET $path")
        }
    }

    @Test
    fun `malformed budget payloads return bad request instead of server error`() = testApplication {
        val f = createFixture()
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(f.database, f.eventRepository) }
        val auth = "Bearer ${jwt(f.organizerId)}"
        val base = "/api/events/${f.eventId}/budget"
        val itemId = BudgetRepository(f.database).getBudgetItems(
            BudgetRepository(f.database).getBudgetByEventId(f.eventId)!!.id
        ).first().id

        val missingItemFields = client.post("$base/items") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"No cost"}""")
        }
        val missingExpenseFields = client.post("$base/expenses") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"beaucoup"}""")
        }
        val malformedItemUpdate = client.put("$base/items/$itemId") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Only a name"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, missingItemFields.status, missingItemFields.bodyAsText())
        assertEquals(HttpStatusCode.BadRequest, missingExpenseFields.status, missingExpenseFields.bodyAsText())
        assertEquals(HttpStatusCode.BadRequest, malformedItemUpdate.status, malformedItemUpdate.bodyAsText())
    }

    private fun createFixture(): Fixture {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val budgetRepository = BudgetRepository(database)
        val eventId = "budget-read-event"
        val organizerId = "budget-read-organizer"
        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = eventId,
                    title = "Budget read",
                    description = "Budget read serialization test",
                    organizerId = organizerId,
                    participants = emptyList(),
                    proposedSlots = emptyList(),
                    deadline = "2026-12-20T00:00:00Z",
                    status = EventStatus.ORGANIZING,
                    finalDate = "2027-01-10T08:00:00Z",
                    createdAt = "2026-09-27T10:00:00Z",
                    updatedAt = "2026-09-27T10:00:00Z",
                    eventType = EventType.OTHER
                )
            ).getOrThrow()
        }
        val budget = budgetRepository.createBudget(eventId)
        budgetRepository.createBudgetItem(
            budgetId = budget.id,
            category = BudgetCategory.MEALS,
            name = "Dinner",
            description = "Shared dinner",
            estimatedCost = 50.0,
            sharedBy = listOf(organizerId)
        )
        budgetRepository.createBudgetItem(
            budgetId = budget.id,
            category = BudgetCategory.ACTIVITIES,
            name = "Climbing",
            description = "Gym entry",
            estimatedCost = 100.0,
            sharedBy = listOf(organizerId)
        )
        return Fixture(database, eventRepository, eventId, organizerId)
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
        val eventId: String,
        val organizerId: String
    )
}
