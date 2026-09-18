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
 * Regression tests for BUG-6, BUG-7 and BUG-8 (QA session 2026-09-18):
 * - the first budget baseline PUT must persist the received values (previously
 *   a zeroed budget was silently created),
 * - malformed budget/comment payloads must return 400 instead of 500,
 * - the provided comment authorName must be persisted so threads display human
 *   names instead of guest identifiers.
 */
class BudgetAndCommentsApiFixTest {

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
        val slot = TimeSlot(
            id = "slot-1",
            start = "2026-12-05T12:00:00Z",
            end = "2026-12-12T22:00:00Z",
            timezone = "America/Sao_Paulo",
            timeOfDay = TimeOfDay.ALL_DAY
        )
        val event = Event(
            id = "event-api-fix",
            title = "Vacances à Rio",
            description = "QA",
            organizerId = "organizer-alice",
            participants = listOf("organizer-alice"),
            proposedSlots = listOf(slot),
            deadline = "2099-01-01T00:00:00Z",
            status = EventStatus.ORGANIZING,
            createdAt = "2026-09-18T19:00:00Z",
            updatedAt = "2026-09-18T19:00:00Z"
        )
        runBlocking { repository.createEvent(event).getOrThrow() }
    }

    private fun baselinePayload(): String = """
        {"id":"budget-x","eventId":"event-api-fix","totalEstimated":4200,"totalActual":0,
         "transportEstimated":2400,"transportActual":0,"accommodationEstimated":1200,"accommodationActual":0,
         "mealsEstimated":400,"mealsActual":0,"activitiesEstimated":150,"activitiesActual":0,
         "equipmentEstimated":0,"equipmentActual":0,"otherEstimated":50,"otherActual":0,
         "createdAt":"2026-09-18T19:00:00Z","updatedAt":"2026-09-18T19:00:00Z"}
    """.trimIndent()

    @Test
    fun `first budget baseline PUT persists the received values`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        val response = client.put("/api/events/event-api-fix/budget") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("organizer-alice")}")
            contentType(ContentType.Application.Json)
            setBody(baselinePayload())
        }

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        assertTrue(
            response.bodyAsText().contains("4200"),
            "first PUT must persist the received baseline: ${response.bodyAsText()}"
        )
    }

    @Test
    fun `malformed budget payload returns 400 instead of 500`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        val response = client.put("/api/events/event-api-fix/budget") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("organizer-alice")}")
            contentType(ContentType.Application.Json)
            setBody("""{"eventId":"event-api-fix"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
    }

    @Test
    fun `malformed comment payload returns 400 instead of 500`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        val response = client.post("/api/events/event-api-fix/comments") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("organizer-alice")}")
            contentType(ContentType.Application.Json)
            setBody("""{"participantId":"organizer-alice","content":"ancien format"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
    }

    @Test
    fun `provided authorName is persisted and listed in the thread`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        val created = client.post("/api/events/event-api-fix/comments") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("organizer-alice")}")
            contentType(ContentType.Application.Json)
            setBody("""{"authorId":"organizer-alice","authorName":"Alice","content":"Pensez à l'assurance !","section":"GENERAL"}""")
        }
        assertEquals(HttpStatusCode.Created, created.status, created.bodyAsText())

        val thread = client.get("/api/events/event-api-fix/comments") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("organizer-alice")}")
        }
        assertTrue(thread.bodyAsText().contains("Alice"), "thread must show the human name: ${thread.bodyAsText()}")
    }
}
