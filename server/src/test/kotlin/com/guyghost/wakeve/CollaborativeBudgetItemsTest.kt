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
import io.ktor.client.request.header
import io.ktor.client.request.post
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

/**
 * Regression tests for proposal #37 (collaborative budget): confirmed
 * participants may record their own budget items during ORGANIZING, closing
 * the Tricount-style gap found in the QA Rio session where only the organizer
 * could create items.
 */
class CollaborativeBudgetItemsTest {

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
            id = "event-cobudget",
            title = "Vacances à Rio",
            description = "QA",
            organizerId = "organizer-alice",
            participants = listOf("organizer-alice", "user-carla"),
            proposedSlots = listOf(slot),
            deadline = "2099-01-01T00:00:00Z",
            status = EventStatus.ORGANIZING,
            createdAt = "2026-09-18T19:00:00Z",
            updatedAt = "2026-09-18T19:00:00Z"
        )
        runBlocking { repository.createEvent(event).getOrThrow() }
        database.participantQueries.insertParticipantWithAxes(
            id = "part_user-carla",
            eventId = "event-cobudget",
            userId = "user-carla",
            role = "PARTICIPANT",
            hasValidatedDate = 1,
            rsvpState = "ACCEPTED",
            dateValidationState = "VALIDATED_RETAINED_DATE",
            joinedAt = "2026-09-18T19:00:00Z",
            updatedAt = "2026-09-18T19:00:00Z"
        )
        // Budget baseline must exist before items can be added.
        database.budgetQueries.insertBudget(
            id = "budget-cobudget", eventId = "event-cobudget",
            totalEstimated = 4200.0, totalActual = 0.0,
            transportEstimated = 2400.0, transportActual = 0.0,
            accommodationEstimated = 1200.0, accommodationActual = 0.0,
            mealsEstimated = 400.0, mealsActual = 0.0,
            activitiesEstimated = 150.0, activitiesActual = 0.0,
            equipmentEstimated = 0.0, equipmentActual = 0.0,
            otherEstimated = 50.0, otherActual = 0.0,
            createdAt = "2026-09-18T19:00:00Z", updatedAt = "2026-09-18T19:00:00Z"
        )
    }

    @Test
    fun `confirmed participant can create a budget item`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        val response = client.post("/api/events/event-cobudget/budget/items") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("user-carla")}")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Pain de Sucre","description":"Billets","category":"ACTIVITIES","estimatedCost":37.5,"sharedBy":["user-carla"]}""")
        }

        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
    }

    @Test
    fun `unconfirmed participant is still forbidden`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()
        // Steve has no participant record at all.

        val response = client.post("/api/events/event-cobudget/budget/items") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("user-steve")}")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"X","description":"x","category":"OTHER","estimatedCost":10}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status, response.bodyAsText())
    }

    @Test
    fun `organizer creation keeps working`() = testApplication {
        application { module(database) }
        seedOrganizingEvent()

        val response = client.post("/api/events/event-cobudget/budget/items") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("organizer-alice")}")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Churrasco","description":"Rodizio","category":"MEALS","estimatedCost":66}""")
        }

        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
    }
}
