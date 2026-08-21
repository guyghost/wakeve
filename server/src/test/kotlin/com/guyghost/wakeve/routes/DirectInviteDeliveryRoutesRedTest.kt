package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.invitationexperience.DirectInviteDeliveryRequest
import com.guyghost.wakeve.invitationexperience.DirectInviteDeliveryResult
import com.guyghost.wakeve.invitationexperience.DirectInviteRecipientOutcome
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.module
import com.guyghost.wakeve.repository.DatabaseEventRepository
import io.ktor.client.request.get
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DirectInviteDeliveryRoutesRedTest {
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "wakev-api"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "wakev-client"
    private val json = Json { ignoreUnknownKeys = false }

    @BeforeTest
    fun setup() {
        DatabaseProvider.resetDatabase()
    }

    @AfterTest
    fun teardown() {
        DatabaseProvider.resetDatabase()
    }

    @Test
    fun `direct invite capability is authenticated and bound to the current draft organizer revision`() = testApplication {
        val fixture = fixture(EventStatus.DRAFT, future = true)
        val deliveryOwner = RecordingDeliveryOwner(fixture.database)
        application {
            module(
                fixture.database,
                fixture.repository,
                directInviteDeliveryOwner = deliveryOwner
            )
        }

        val unauthenticated = client.get("/api/events/${fixture.eventId}/direct-invites/capability")
        assertEquals(HttpStatusCode.Unauthorized, unauthenticated.status)

        val response = client.get("/api/events/${fixture.eventId}/direct-invites/capability") {
            header(HttpHeaders.Authorization, "Bearer ${token(fixture.organizerId)}")
        }
        val bodyText = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, bodyText)

        val body = json.parseToJsonElement(bodyText).jsonObject
        assertEquals("READY", body.getValue("state").jsonPrimitive.content)
        assertEquals(fixture.eventId, body.getValue("eventId").jsonPrimitive.content)
        assertEquals(fixture.organizerId, body.getValue("actorId").jsonPrimitive.content)
        assertEquals(fixture.accessRevision.toString(), body.getValue("accessRevision").jsonPrimitive.content)

        val otherActor = client.get("/api/events/${fixture.eventId}/direct-invites/capability") {
            header(HttpHeaders.Authorization, "Bearer ${token("other-user")}")
        }
        assertEquals(HttpStatusCode.Forbidden, otherActor.status, otherActor.bodyAsText())
    }

    @Test
    fun `sealed batch is durably dispatched and replay returns the exact acknowledgement once`() = testApplication {
        val fixture = fixture(EventStatus.DRAFT, future = true)
        val deliveryOwner = RecordingDeliveryOwner(fixture.database)
        application {
            module(
                fixture.database,
                fixture.repository,
                directInviteDeliveryOwner = deliveryOwner
            )
        }

        val first = client.post("/api/events/${fixture.eventId}/direct-invites/batches") {
            header(HttpHeaders.Authorization, "Bearer ${token(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(sealedRequest(fixture))
        }
        val firstBody = first.bodyAsText()
        assertEquals(HttpStatusCode.OK, first.status, firstBody)

        val replay = client.post("/api/events/${fixture.eventId}/direct-invites/batches") {
            header(HttpHeaders.Authorization, "Bearer ${token(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(sealedRequest(fixture))
        }
        val replayBody = replay.bodyAsText()
        assertEquals(HttpStatusCode.OK, replay.status, replayBody)
        assertEquals(
            json.parseToJsonElement(firstBody),
            json.parseToJsonElement(replayBody),
            "An operation replay must return the stored acknowledgement, not dispatch a second invitation."
        )
        assertEquals(1, deliveryOwner.dispatchCount)
        assertTrue(
            deliveryOwner.batchWasDurableBeforeDispatch,
            "The authoritative server queue must commit the bound batch before a provider can observe it."
        )
        assertTrue(
            deliveryOwner.envelopeWasDurableBeforeDispatch,
            "The provider must never be invoked from transient recipient input before its encrypted envelope is durable."
        )

        val forgedReplay = client.post("/api/events/${fixture.eventId}/direct-invites/batches") {
            header(HttpHeaders.Authorization, "Bearer ${token(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(sealedRequest(fixture).replace(fixture.ciphertext, "enc:v1:Zm9yZ2VkLXJlcGxheQ"))
        }
        assertEquals(
            HttpStatusCode.Conflict,
            forgedReplay.status,
            "The operation id must bind the original encrypted request fingerprint."
        )

        val response = json.parseToJsonElement(firstBody).jsonObject
        assertEquals(fixture.batchId, response.getValue("batchId").jsonPrimitive.content)
        assertEquals(fixture.operationId, response.getValue("operationId").jsonPrimitive.content)
        assertEquals("ACKNOWLEDGED", response.getValue("status").jsonPrimitive.content)
        assertTrue(firstBody.contains(fixture.recipientKey), firstBody)
        assertTrue(firstBody.contains("SERVER_ACCEPTED"), firstBody)

        val batch = fixture.database.invitationExperienceQueries
            .selectDirectInviteBatch(fixture.batchId)
            .executeAsOneOrNull()
        assertNotNull(batch)
        assertEquals(fixture.organizerId, batch.actor_id)
        assertEquals(fixture.accessRevision, batch.access_revision)
        assertEquals("COMPLETED", batch.status)

        val outcomes = fixture.database.invitationExperienceQueries
            .selectDirectInviteRecipientOutcomes(fixture.batchId)
            .executeAsList()
        assertEquals(1, outcomes.size)
        assertEquals(fixture.recipientKey, outcomes.single().recipient_key)
        assertEquals("SERVER_ACCEPTED", outcomes.single().status)
        assertNotNull(outcomes.single().invitation_id)

        val envelopes = fixture.database.invitationExperienceQueries
            .selectDirectInviteDeliveryEnvelopes(fixture.batchId)
            .executeAsList()
        assertEquals(1, envelopes.size)
        assertEquals(fixture.ciphertext, envelopes.single().ciphertext)
        assertEquals("SERVER_ACCEPTED", envelopes.single().transport_state)
        assertFalse(firstBody.contains(fixture.rawRecipient), firstBody)
    }

    @Test
    fun `raw recipient input and unprotected recipient keys are rejected before persistence`() = testApplication {
        val fixture = fixture(EventStatus.DRAFT, future = true)
        val deliveryOwner = RecordingDeliveryOwner(fixture.database)
        application {
            module(
                fixture.database,
                fixture.repository,
                directInviteDeliveryOwner = deliveryOwner
            )
        }

        val rawRecipient = client.post("/api/events/${fixture.eventId}/direct-invites/batches") {
            header(HttpHeaders.Authorization, "Bearer ${token(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(sealedRequest(fixture).replace(
                "\"ciphertext\": \"${fixture.ciphertext}\"",
                "\"rawRecipient\": \"${fixture.rawRecipient}\", \"ciphertext\": \"${fixture.ciphertext}\""
            ))
        }
        assertEquals(HttpStatusCode.BadRequest, rawRecipient.status, rawRecipient.bodyAsText())

        val unprotectedKey = client.post("/api/events/${fixture.eventId}/direct-invites/batches") {
            header(HttpHeaders.Authorization, "Bearer ${token(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(sealedRequest(fixture).replace(fixture.recipientKey, fixture.rawRecipient))
        }
        assertEquals(HttpStatusCode.BadRequest, unprotectedKey.status, unprotectedKey.bodyAsText())

        assertEquals(
            null,
            fixture.database.invitationExperienceQueries
                .selectDirectInviteBatch(fixture.batchId)
                .executeAsOneOrNull()
        )
        assertFalse(rawRecipient.bodyAsText().contains(fixture.rawRecipient))
        assertFalse(unprotectedKey.bodyAsText().contains(fixture.rawRecipient))
        assertEquals(0, deliveryOwner.dispatchCount)
    }

    @Test
    fun `stale revision non draft and past bindings fail closed without dispatch`() = testApplication {
        val draft = fixture(EventStatus.DRAFT, future = true, eventId = "event-stale")
        val polling = fixture(EventStatus.POLLING, future = true, eventId = "event-polling")
        val past = fixture(EventStatus.DRAFT, future = false, eventId = "event-past")
        val deliveryOwner = RecordingDeliveryOwner(draft.database)
        application {
            module(
                draft.database,
                draft.repository,
                directInviteDeliveryOwner = deliveryOwner
            )
        }

        val stale = client.post("/api/events/${draft.eventId}/direct-invites/batches") {
            header(HttpHeaders.Authorization, "Bearer ${token(draft.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(sealedRequest(draft, accessRevision = draft.accessRevision + 1L))
        }
        assertEquals(HttpStatusCode.Conflict, stale.status, stale.bodyAsText())

        val afterDraft = client.post("/api/events/${polling.eventId}/direct-invites/batches") {
            header(HttpHeaders.Authorization, "Bearer ${token(polling.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(sealedRequest(polling))
        }
        assertEquals(HttpStatusCode.Forbidden, afterDraft.status, afterDraft.bodyAsText())

        val pastResponse = client.post("/api/events/${past.eventId}/direct-invites/batches") {
            header(HttpHeaders.Authorization, "Bearer ${token(past.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(sealedRequest(past))
        }
        assertEquals(HttpStatusCode.Forbidden, pastResponse.status, pastResponse.bodyAsText())

        for (batchId in listOf(draft.batchId, polling.batchId, past.batchId)) {
            assertEquals(
                null,
                draft.database.invitationExperienceQueries
                    .selectDirectInviteBatch(batchId)
                    .executeAsOneOrNull(),
                "A rejected binding must not leave a local/server dispatch record."
            )
        }
        assertEquals(0, deliveryOwner.dispatchCount)
    }

    private fun fixture(
        status: EventStatus,
        future: Boolean,
        eventId: String = "event-direct-delivery"
    ): Fixture {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val repository = DatabaseEventRepository(database)
        val organizerId = "organizer-direct-delivery"
        val slotStart = if (future) "2030-09-10T18:00:00Z" else "2020-09-10T18:00:00Z"
        val slotEnd = if (future) "2030-09-10T20:00:00Z" else "2020-09-10T20:00:00Z"

        runBlocking {
            repository.createEvent(
                Event(
                    id = eventId,
                    title = "Escapade à Annecy",
                    description = "Préparer un week-end entre proches",
                    organizerId = organizerId,
                    proposedSlots = listOf(
                        TimeSlot(
                            id = "slot-$eventId",
                            start = slotStart,
                            end = slotEnd,
                            timezone = "Europe/Paris",
                            timeOfDay = TimeOfDay.SPECIFIC
                        )
                    ),
                    deadline = slotStart,
                    status = status,
                    createdAt = "2026-08-21T10:00:00Z",
                    updatedAt = "2026-08-21T10:00:00Z"
                )
            ).getOrThrow()
        }

        val row = database.eventQueries.selectById(eventId).executeAsOne()
        return Fixture(
            database = database,
            repository = repository,
            eventId = eventId,
            organizerId = organizerId,
            accessRevision = row.aggregateRevision,
            batchId = "batch-$eventId",
            operationId = "operation-$eventId",
            recipientKey = "hmac-v1-0123456789abcdef0123456789abcdef",
            ciphertext = "enc:v1:VGhpcyBpcyBhIHByb3RlY3RlZCByZWNpcGllbnQ",
            rawRecipient = "lea@example.com"
        )
    }

    private fun sealedRequest(fixture: Fixture, accessRevision: Long = fixture.accessRevision): String =
        """
        {
          "accessRevision": $accessRevision,
          "batchId": "${fixture.batchId}",
          "operationId": "${fixture.operationId}",
          "envelopes": [
            {
              "recipientKey": "${fixture.recipientKey}",
              "ciphertext": "${fixture.ciphertext}",
              "keyVersion": 1,
              "expiresAt": "2030-09-11T20:00:00Z"
            }
          ]
        }
        """.trimIndent()

    private fun token(userId: String): String = JWT.create()
        .withIssuer(jwtIssuer)
        .withAudience(jwtAudience)
        .withClaim("userId", userId)
        .withClaim("role", "USER")
        .withClaim("sessionId", "session-$userId")
        .withClaim("permissions", listOf("READ", "WRITE"))
        .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
        .sign(Algorithm.HMAC256(jwtSecret))

    private data class Fixture(
        val database: WakeveDb,
        val repository: DatabaseEventRepository,
        val eventId: String,
        val organizerId: String,
        val accessRevision: Long,
        val batchId: String,
        val operationId: String,
        val recipientKey: String,
        val ciphertext: String,
        val rawRecipient: String
    )

    private class RecordingDeliveryOwner(
        private val database: WakeveDb
    ) : DirectInviteBackendDeliveryOwner {
        var dispatchCount: Int = 0
            private set
        var batchWasDurableBeforeDispatch: Boolean = false
            private set
        var envelopeWasDurableBeforeDispatch: Boolean = false
            private set

        override suspend fun dispatch(
            request: DirectInviteDeliveryRequest
        ): DirectInviteDeliveryResult {
            dispatchCount += 1
            batchWasDurableBeforeDispatch = database.invitationExperienceQueries
                .selectDirectInviteBatch(request.binding.batchId)
                .executeAsOneOrNull()
                ?.let { batch ->
                    batch.event_id == request.binding.eventId &&
                        batch.actor_id == request.binding.actorId &&
                        batch.access_revision == request.binding.accessRevision &&
                        batch.operation_id == request.binding.operationId &&
                        batch.status == "PENDING_SYNC"
                } == true
            envelopeWasDurableBeforeDispatch = database.invitationExperienceQueries
                .selectDirectInviteDeliveryEnvelopes(request.binding.batchId)
                .executeAsList()
                .map { it.recipient_key }
                .toSet() == request.envelopes.map { it.recipientKey.value }.toSet()

            return DirectInviteDeliveryResult.Acknowledged(
                batchId = request.binding.batchId,
                operationId = request.binding.operationId,
                outcomesByRecipientKey = request.envelopes.associate { envelope ->
                    envelope.recipientKey to DirectInviteRecipientOutcome.ServerAccepted(
                        invitationId = "provider-${envelope.recipientKey.value.takeLast(12)}"
                    )
                }
            )
        }
    }
}
