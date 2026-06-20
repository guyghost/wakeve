package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.SyncChange
import com.guyghost.wakeve.models.SyncEventData
import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.SyncParticipantData
import com.guyghost.wakeve.models.SyncRequest
import com.guyghost.wakeve.models.SyncVoteData
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.module
import com.guyghost.wakeve.repository.DatabaseEventRepository
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncRoutesAccessValidationTest {
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
    fun `sync rejects event update from non organizer`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        seedPollingEvent(database, eventRepository)
        val client = createJsonClient()

        application { module(database = database, eventRepository = eventRepository) }

        val response = client.post("/api/sync") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("participant-user")}")
            contentType(ContentType.Application.Json)
            setBody(
                SyncRequest(
                    changes = listOf(
                        SyncChange(
                            id = "sync-event-update",
                            table = "events",
                            operation = SyncOperation.UPDATE.name,
                            recordId = "sync-event",
                            data = json.encodeToString(
                                SyncEventData(
                                    id = "sync-event",
                                    title = "Participant rewrite",
                                    description = "Unauthorized offline edit",
                                    organizerId = "organizer-user",
                                    deadline = "2026-07-01T00:00:00Z",
                                    timezone = "UTC"
                                )
                            ),
                            timestamp = "2026-06-20T10:00:00Z",
                            userId = "participant-user"
                        )
                    )
                )
            )
        }

        assertEquals(HttpStatusCode.Conflict, response.status, response.bodyAsText())
        assertEquals("Original Sync Event", eventRepository.getEvent("sync-event")?.title)
    }

    @Test
    fun `sync rejects participant mutation from non organizer`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        seedPollingEvent(database, eventRepository)
        insertUser(database, "added-user")
        val client = createJsonClient()

        application { module(database = database, eventRepository = eventRepository) }

        val response = client.post("/api/sync") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("participant-user")}")
            contentType(ContentType.Application.Json)
            setBody(
                SyncRequest(
                    changes = listOf(
                        SyncChange(
                            id = "sync-participant-create",
                            table = "participants",
                            operation = SyncOperation.CREATE.name,
                            recordId = "participant-sync-event-added-user",
                            data = json.encodeToString(
                                SyncParticipantData(
                                    eventId = "sync-event",
                                    userId = "added-user"
                                )
                            ),
                            timestamp = "2026-06-20T10:00:00Z",
                            userId = "participant-user"
                        )
                    )
                )
            )
        }

        assertEquals(HttpStatusCode.Conflict, response.status, response.bodyAsText())
        assertNull(database.participantQueries.selectByEventIdAndUserId("sync-event", "added-user").executeAsOneOrNull())
    }

    @Test
    fun `sync rejects vote submitted for another participant`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        seedPollingEvent(database, eventRepository)
        val client = createJsonClient()

        application { module(database = database, eventRepository = eventRepository) }

        val response = client.post("/api/sync") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("participant-user")}")
            contentType(ContentType.Application.Json)
            setBody(
                SyncRequest(
                    changes = listOf(
                        SyncChange(
                            id = "sync-spoofed-vote",
                            table = "votes",
                            operation = SyncOperation.CREATE.name,
                            recordId = "vote_sync-slot_other-participant",
                            data = json.encodeToString(
                                SyncVoteData(
                                    eventId = "sync-event",
                                    participantId = "other-participant",
                                    slotId = "sync-slot",
                                    preference = "YES"
                                )
                            ),
                            timestamp = "2026-06-20T10:00:00Z",
                            userId = "participant-user"
                        )
                    )
                )
            )
        }

        assertEquals(HttpStatusCode.Conflict, response.status, response.bodyAsText())
        assertNull(database.voteQueries.selectById("vote_sync-slot_other-participant").executeAsOneOrNull())
    }

    private fun ApplicationTestBuilder.createJsonClient() = createClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun seedPollingEvent(database: WakeveDb, eventRepository: DatabaseEventRepository) {
        insertUser(database, "organizer-user")
        insertUser(database, "participant-user")
        insertUser(database, "other-participant")

        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = "sync-event",
                    title = "Original Sync Event",
                    description = "Sync access validation",
                    organizerId = "organizer-user",
                    participants = emptyList(),
                    proposedSlots = listOf(
                        TimeSlot(
                            id = "sync-slot",
                            start = "2026-07-01T10:00:00Z",
                            end = "2026-07-01T12:00:00Z",
                            timezone = "UTC",
                            timeOfDay = TimeOfDay.SPECIFIC
                        )
                    ),
                    deadline = "2026-07-01T00:00:00Z",
                    status = EventStatus.POLLING,
                    createdAt = "2026-06-20T10:00:00Z",
                    updatedAt = "2026-06-20T10:00:00Z",
                    eventType = EventType.OTHER
                )
            ).getOrThrow()
        }
        database.participantQueries.insertParticipant(
            id = "participant-sync-event-participant-user",
            eventId = "sync-event",
            userId = "participant-user",
            role = "PARTICIPANT",
            hasValidatedDate = 0,
            joinedAt = "2026-06-20T10:00:00Z",
            updatedAt = "2026-06-20T10:00:00Z"
        )
        database.participantQueries.insertParticipant(
            id = "participant-sync-event-other-participant",
            eventId = "sync-event",
            userId = "other-participant",
            role = "PARTICIPANT",
            hasValidatedDate = 0,
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
