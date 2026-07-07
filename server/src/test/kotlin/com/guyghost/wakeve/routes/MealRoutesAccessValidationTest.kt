package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.meal.MealRepository
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.MealRequest
import com.guyghost.wakeve.models.MealStatus
import com.guyghost.wakeve.models.MealType
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MealRoutesAccessValidationTest {
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
    fun `meal plan is visible to participants but hidden from non members`() = testApplication {
        val fixture = createMealFixture("visibility")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        application {
            module(
                database = fixture.database,
                eventRepository = fixture.eventRepository,
                mealRepository = fixture.mealRepository
            )
        }

        val participantRead = client.get("/api/events/${fixture.eventId}/meals") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.participantId)}")
        }
        val participantText = participantRead.bodyAsText()
        val participantMeals = json.parseToJsonElement(participantText).jsonArray

        val nonMemberRead = client.get("/api/events/${fixture.eventId}/meals") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.nonMemberId)}")
        }
        val participantCreate = client.post("/api/events/${fixture.eventId}/meals") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.participantId)}")
            contentType(ContentType.Application.Json)
            setBody(mealBody(fixture.eventId, "Participant Meal"))
        }
        val nonMemberCreate = client.post("/api/events/${fixture.eventId}/meals") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.nonMemberId)}")
            contentType(ContentType.Application.Json)
            setBody(mealBody(fixture.eventId, "Non-member Meal"))
        }

        assertEquals(HttpStatusCode.OK, participantRead.status, participantText)
        assertEquals(1, participantMeals.size)
        assertEquals("Organizer Dinner", participantMeals[0].jsonObject.getValue("name").jsonPrimitive.content)
        assertEquals(HttpStatusCode.Forbidden, nonMemberRead.status, nonMemberRead.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, participantCreate.status, participantCreate.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, nonMemberCreate.status, nonMemberCreate.bodyAsText())
    }

    @Test
    fun `dietary restrictions are self managed or organizer managed`() = testApplication {
        val fixture = createMealFixture("diet")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        application {
            module(
                database = fixture.database,
                eventRepository = fixture.eventRepository,
                mealRepository = fixture.mealRepository
            )
        }

        val ownRestriction = client.post("/api/events/${fixture.eventId}/dietary-restrictions") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.participantId)}")
            contentType(ContentType.Application.Json)
            setBody(dietaryRestrictionBody(fixture.eventId, fixture.participantId, notes = "No shellfish"))
        }
        val spoofedRestriction = client.post("/api/events/${fixture.eventId}/dietary-restrictions") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.participantId)}")
            contentType(ContentType.Application.Json)
            setBody(dietaryRestrictionBody(fixture.eventId, fixture.secondParticipantId, notes = "Spoofed"))
        }
        val participantReadsOwn = client.get(
            "/api/events/${fixture.eventId}/dietary-restrictions/participant/${fixture.participantId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.participantId)}")
        }
        val participantReadsOther = client.get(
            "/api/events/${fixture.eventId}/dietary-restrictions/participant/${fixture.secondParticipantId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.participantId)}")
        }
        val organizerReadsParticipant = client.get(
            "/api/events/${fixture.eventId}/dietary-restrictions/participant/${fixture.participantId}"
        ) {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
        }

        assertEquals(HttpStatusCode.Created, ownRestriction.status, ownRestriction.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, spoofedRestriction.status, spoofedRestriction.bodyAsText())
        assertEquals(HttpStatusCode.OK, participantReadsOwn.status, participantReadsOwn.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, participantReadsOther.status, participantReadsOther.bodyAsText())
        assertEquals(HttpStatusCode.OK, organizerReadsParticipant.status, organizerReadsParticipant.bodyAsText())
        assertTrue(organizerReadsParticipant.bodyAsText().contains("No shellfish"))
    }

    private fun createMealFixture(suffix: String): MealRouteFixture {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val mealRepository = MealRepository(database)
        val eventId = "meal-event-$suffix"
        val organizerId = "meal-organizer-$suffix"
        val participantId = "meal-participant-$suffix"
        val secondParticipantId = "meal-second-participant-$suffix"
        val nonMemberId = "meal-non-member-$suffix"

        insertUser(database, organizerId)
        insertUser(database, participantId)
        insertUser(database, secondParticipantId)
        insertUser(database, nonMemberId)
        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = eventId,
                    title = "Meal Event $suffix",
                    description = "Meal access test event",
                    organizerId = organizerId,
                    participants = emptyList(),
                    proposedSlots = listOf(
                        TimeSlot(
                            id = "slot-$suffix",
                            start = "2026-08-01T10:00:00Z",
                            end = "2026-08-01T12:00:00Z",
                            timezone = "UTC",
                            timeOfDay = TimeOfDay.SPECIFIC
                        )
                    ),
                    deadline = "2026-07-20T00:00:00Z",
                    status = EventStatus.DRAFT,
                    createdAt = "2026-06-20T10:00:00Z",
                    updatedAt = "2026-06-20T10:00:00Z",
                    eventType = EventType.OTHER
                )
            ).getOrThrow()
            eventRepository.addParticipant(eventId, participantId).getOrThrow()
            eventRepository.addParticipant(eventId, secondParticipantId).getOrThrow()
        }
        mealRepository.createMeal(
            MealRequest(
                eventId = eventId,
                type = MealType.DINNER,
                name = "Organizer Dinner",
                date = "2026-08-01",
                time = "19:00",
                responsibleParticipantIds = listOf(organizerId),
                estimatedCost = 12_000L,
                servings = 4,
                status = MealStatus.PLANNED
            )
        )

        return MealRouteFixture(
            database = database,
            eventRepository = eventRepository,
            mealRepository = mealRepository,
            eventId = eventId,
            organizerId = organizerId,
            participantId = participantId,
            secondParticipantId = secondParticipantId,
            nonMemberId = nonMemberId
        )
    }

    private fun mealBody(eventId: String, name: String): String =
        """
        {
          "eventId": "$eventId",
          "type": "DINNER",
          "name": "$name",
          "date": "2026-08-01",
          "time": "19:00",
          "responsibleParticipantIds": [],
          "estimatedCost": 12000,
          "servings": 4,
          "status": "PLANNED"
        }
        """.trimIndent()

    private fun dietaryRestrictionBody(eventId: String, participantId: String, notes: String): String =
        """
        {
          "participantId": "$participantId",
          "eventId": "$eventId",
          "restriction": "SHELLFISH_ALLERGY",
          "notes": "$notes"
        }
        """.trimIndent()

    private fun insertUser(database: WakeveDb, userId: String) {
        database.userQueries.insertUser(
            id = userId,
            provider_id = "provider-$userId",
            email = "$userId@example.test",
            name = userId,
            avatar_url = null,
            provider = "google",
            role = "USER",
            created_at = "2026-06-20T10:00:00Z",
            updated_at = "2026-06-20T10:00:00Z"
        )
    }

    private fun createTestJwt(userId: String): String =
        JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withClaim("role", "USER")
            .withClaim("sessionId", "test-session-$userId")
            .withClaim("permissions", listOf("READ", "WRITE"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))
}

private data class MealRouteFixture(
    val database: WakeveDb,
    val eventRepository: DatabaseEventRepository,
    val mealRepository: MealRepository,
    val eventId: String,
    val organizerId: String,
    val participantId: String,
    val secondParticipantId: String,
    val nonMemberId: String
)
