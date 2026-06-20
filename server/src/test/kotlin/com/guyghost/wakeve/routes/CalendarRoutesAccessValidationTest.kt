package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.calendar.PlatformCalendarService
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EnhancedCalendarEvent
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.module
import com.guyghost.wakeve.repository.DatabaseEventRepository
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
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

class CalendarRoutesAccessValidationTest {
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
    fun `organizer can add native calendar entry for event participant`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val platformCalendar = RecordingPlatformCalendarService()
        val token = createTestJwt("calendar-organizer")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        seedConfirmedEvent(database, eventRepository)
        insertParticipant(database, "calendar-event", "validated-participant", hasValidatedDate = true)

        application {
            module(
                database = database,
                eventRepository = eventRepository,
                calendarService = CalendarService(database, platformCalendar)
            )
        }

        val response = client.post("/api/events/calendar-event/calendar/native") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"participantId":"validated-participant"}""")
        }
        val responseText = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status, responseText)
        assertEquals(listOf("add:calendar-event_validated-participant"), platformCalendar.calls)
    }

    @Test
    fun `validated participant can update and remove only own native calendar entry`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val platformCalendar = RecordingPlatformCalendarService()
        val token = createTestJwt("validated-participant")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        seedConfirmedEvent(database, eventRepository)
        insertParticipant(database, "calendar-event", "validated-participant", hasValidatedDate = true)
        insertParticipant(database, "calendar-event", "other-participant", hasValidatedDate = true)

        application {
            module(
                database = database,
                eventRepository = eventRepository,
                calendarService = CalendarService(database, platformCalendar)
            )
        }

        val updateOwn = client.put("/api/events/calendar-event/calendar/native/validated-participant") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{}""")
        }
        val updateOther = client.put("/api/events/calendar-event/calendar/native/other-participant") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{}""")
        }
        val deleteOwn = client.delete("/api/events/calendar-event/calendar/native/validated-participant") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, updateOwn.status, updateOwn.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, updateOther.status, updateOther.bodyAsText())
        assertEquals(HttpStatusCode.OK, deleteOwn.status, deleteOwn.bodyAsText())
        assertEquals(
            listOf("update:calendar-event_validated-participant", "delete:calendar-event_validated-participant"),
            platformCalendar.calls
        )
    }

    @Test
    fun `non participant cannot add native calendar entry`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val platformCalendar = RecordingPlatformCalendarService()
        val token = createTestJwt("calendar-stranger")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        seedConfirmedEvent(database, eventRepository)
        insertParticipant(database, "calendar-event", "validated-participant", hasValidatedDate = true)

        application {
            module(
                database = database,
                eventRepository = eventRepository,
                calendarService = CalendarService(database, platformCalendar)
            )
        }

        val response = client.post("/api/events/calendar-event/calendar/native") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"participantId":"validated-participant"}""")
        }
        val responseText = response.bodyAsText()

        assertEquals(HttpStatusCode.Forbidden, response.status, responseText)
        assertEquals(emptyList(), platformCalendar.calls)
    }

    @Test
    fun `unvalidated participant cannot add native calendar entry`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val platformCalendar = RecordingPlatformCalendarService()
        val token = createTestJwt("unvalidated-participant")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        seedConfirmedEvent(database, eventRepository)
        insertParticipant(database, "calendar-event", "unvalidated-participant", hasValidatedDate = false)

        application {
            module(
                database = database,
                eventRepository = eventRepository,
                calendarService = CalendarService(database, platformCalendar)
            )
        }

        val response = client.post("/api/events/calendar-event/calendar/native") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"participantId":"unvalidated-participant"}""")
        }
        val responseText = response.bodyAsText()

        assertEquals(HttpStatusCode.Forbidden, response.status, responseText)
        assertEquals(emptyList(), platformCalendar.calls)
    }

    private fun seedConfirmedEvent(database: WakeveDb, eventRepository: DatabaseEventRepository) {
        insertUser(database, "calendar-organizer")
        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = "calendar-event",
                    title = "Calendar Event",
                    description = "Calendar access validation event",
                    organizerId = "calendar-organizer",
                    participants = emptyList(),
                    proposedSlots = listOf(
                        TimeSlot(
                            id = "slot-calendar-event",
                            start = "2026-08-01T10:00:00Z",
                            end = "2026-08-01T12:00:00Z",
                            timezone = "UTC",
                            timeOfDay = TimeOfDay.SPECIFIC
                        )
                    ),
                    deadline = "2026-07-20T00:00:00Z",
                    status = EventStatus.CONFIRMED,
                    createdAt = "2026-06-20T10:00:00Z",
                    updatedAt = "2026-06-20T10:00:00Z",
                    eventType = EventType.OTHER
                )
            ).getOrThrow()
        }
    }

    private fun insertParticipant(database: WakeveDb, eventId: String, userId: String, hasValidatedDate: Boolean) {
        insertUser(database, userId)
        database.participantQueries.insertParticipant(
            id = "participant-$eventId-$userId",
            eventId = eventId,
            userId = userId,
            role = "PARTICIPANT",
            hasValidatedDate = if (hasValidatedDate) 1 else 0,
            joinedAt = "2026-06-20T10:00:00Z",
            updatedAt = "2026-06-20T10:00:00Z"
        )
    }

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

private class RecordingPlatformCalendarService : PlatformCalendarService {
    val calls = mutableListOf<String>()

    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> {
        calls += "add:${event.id}"
        return Result.success(Unit)
    }

    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> {
        calls += "update:${event.id}"
        return Result.success(Unit)
    }

    override fun deleteEvent(eventId: String): Result<Unit> {
        calls += "delete:$eventId"
        return Result.success(Unit)
    }
}
