package com.guyghost.wakeve.sync

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.ConfirmationEnvelopeAcknowledgement
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.SyncConflict
import com.guyghost.wakeve.models.SyncResponse
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * RED contracts for publishing a locally committed confirmation to the sync boundary.
 *
 * The local decision remains final offline. The acknowledgement can only clear its pending
 * status when it names the exact durable envelope; generic batch success is insufficient.
 */
class ConfirmationEnvelopeSyncManagerRedTest {
    @Test
    fun committedConfirmationPublishesConfirmedDecisionBeforeItsExactEnvelope() = runBlocking {
        val fixture = confirmedLocalFixture()
        val changes = fixture.syncManager.getPendingChangesForSync()

        assertEquals(
            listOf("events", "confirmation_effect_outbox"),
            changes.map { it.table },
            "The confirmed event decision must reach the server before its downstream envelope"
        )

        val eventChange = changes[0]
        val eventPayload = Json.parseToJsonElement(eventChange.data).jsonObject
        assertEquals("CONFIRMED", eventPayload["status"]?.jsonPrimitive?.content)
        assertEquals(fixture.slot.id, eventPayload["confirmedSlotId"]?.jsonPrimitive?.content)
        assertEquals(fixture.slot.start, eventPayload["finalDate"]?.jsonPrimitive?.content)

        val envelope = changes[1]
        val envelopePayload = Json.parseToJsonElement(envelope.data).jsonObject
        assertEquals("CREATE", envelope.operation)
        assertEquals(fixture.domainEventId, envelope.recordId)
        assertEquals(fixture.domainEventId, envelopePayload["domainEventId"]?.jsonPrimitive?.content)
        assertEquals(fixture.effectKey, envelopePayload["effectKey"]?.jsonPrimitive?.content)
        assertEquals(fixture.slot.id, envelopePayload["slotId"]?.jsonPrimitive?.content)
        assertEquals(fixture.operationId, envelopePayload["operationId"]?.jsonPrimitive?.content)
    }

    @Test
    fun onlyTheCorrelatedEnvelopeAcknowledgementMarksTheLocalConfirmationSynced() = runBlocking {
        val acknowledgement = ConfirmationEnvelopeAcknowledgement(
            domainEventId = DOMAIN_EVENT_ID,
            effectKey = EFFECT_KEY,
            operationId = OPERATION_ID,
            receiptId = "server-confirmation-ack"
        )
        val fixture = confirmedLocalFixture(
            response = SyncResponse(
                success = true,
                appliedChanges = 2,
                conflicts = emptyList(),
                serverTimestamp = TIMESTAMP,
                confirmationAcknowledgements = listOf(acknowledgement)
            )
        )

        fixture.network.setAvailable(true)
        fixture.syncManager.triggerSync().getOrThrow()

        val sent = assertNotNull(fixture.httpClient.requests.lastOrNull())
        assertTrue(sent.changes.any { it.table == "confirmation_effect_outbox" })
        assertEquals(
            EventManagementContract.DecisionSyncStatus.SERVER_ACKNOWLEDGED,
            confirmedProjection(fixture).decisionSyncStatus
        )
        assertEquals(
            1L,
            fixture.database.syncMetadataQueries
                .selectById("sync_confirm_${fixture.event.id}")
                .executeAsOne()
                .synced
        )
    }

    @Test
    fun conflictOrUncorrelatedAcknowledgementLeavesTheConfirmationEnvelopePending() = runBlocking {
        val fixture = confirmedLocalFixture(
            response = SyncResponse(
                success = true,
                appliedChanges = 1,
                conflicts = listOf(
                    SyncConflict(
                        changeId = "confirmation-envelope-change",
                        table = "confirmation_effect_outbox",
                        recordId = DOMAIN_EVENT_ID,
                        clientData = "{}",
                        serverData = "{}",
                        resolution = "SERVER_WINS"
                    )
                ),
                serverTimestamp = TIMESTAMP,
                confirmationAcknowledgements = listOf(
                    ConfirmationEnvelopeAcknowledgement(
                        domainEventId = "poll-date-confirmed:other:slot:v1",
                        effectKey = "poll-date-confirmed:other:slot:v1:confirmation",
                        operationId = "other-operation",
                        receiptId = "other-receipt"
                    )
                )
            )
        )

        fixture.network.setAvailable(true)
        fixture.syncManager.triggerSync().getOrThrow()

        val sent = assertNotNull(fixture.httpClient.requests.lastOrNull())
        assertTrue(sent.changes.any { it.table == "confirmation_effect_outbox" })
        assertEquals(
            EventManagementContract.DecisionSyncStatus.LOCAL_PENDING,
            confirmedProjection(fixture).decisionSyncStatus,
            "A conflict or another decision's acknowledgement must not clear this decision's pending sync"
        )
        assertEquals(
            0L,
            fixture.database.syncMetadataQueries
                .selectById("sync_confirm_${fixture.event.id}")
                .executeAsOne()
                .synced
        )
        assertTrue(
            fixture.syncManager.getPendingChangesForSync().any { it.recordId == fixture.domainEventId },
            "The rejected envelope must remain retryable"
        )
    }

    private suspend fun confirmedLocalFixture(
        response: SyncResponse = SyncResponse(
            success = true,
            appliedChanges = 2,
            conflicts = emptyList(),
            serverTimestamp = TIMESTAMP
        )
    ): ConfirmationFixture {
        val database = createFreshTestDatabase()
        val repository = DatabaseEventRepository(database)
        val slot = TimeSlot(
            id = SLOT_ID,
            start = "2026-08-16T10:00:00Z",
            end = "2026-08-16T12:00:00Z",
            timezone = "UTC",
            timeOfDay = TimeOfDay.SPECIFIC
        )
        val event = Event(
            id = EVENT_ID,
            title = "Local confirmation sync",
            description = "The local decision must publish a durable envelope",
            organizerId = ORGANIZER_ID,
            proposedSlots = listOf(slot),
            deadline = "2026-08-01T00:00:00Z",
            status = EventStatus.POLLING,
            createdAt = TIMESTAMP,
            updatedAt = TIMESTAMP,
            eventType = EventType.OTHER
        )
        repository.createEvent(event).getOrThrow()
        database.voteQueries.insertVote(
            id = "vote-$EVENT_ID-$SLOT_ID",
            eventId = EVENT_ID,
            timeslotId = SLOT_ID,
            participantId = "org_$EVENT_ID",
            vote = "YES",
            createdAt = TIMESTAMP,
            updatedAt = TIMESTAMP
        )
        assertIs<EventManagementContract.ConfirmationResult.Committed>(
            repository.confirmPollDate(
                EventManagementContract.ConfirmPollDateCommand(
                    operationId = OPERATION_ID,
                    eventId = EVENT_ID,
                    slotId = SLOT_ID,
                    actorId = ORGANIZER_ID,
                    requestedAt = Instant.parse(TIMESTAMP)
                )
            )
        )

        val network = ControlledNetworkDetector()
        val httpClient = CapturingSyncHttpClient(response)
        val syncManager = SyncManager(
            database = database,
            eventRepository = repository,
            userRepository = UserRepository(database),
            networkDetector = network,
            httpClient = httpClient,
            authTokenProvider = { "test-token" },
            maxRetries = 0
        )
        return ConfirmationFixture(database, repository, event, slot, network, httpClient, syncManager)
    }

    private fun confirmedProjection(fixture: ConfirmationFixture) = assertIs<EventManagementContract.ConfirmationProjection.Confirmed>(
        fixture.repository.loadConfirmationProjection(fixture.event.id)
    )

    private data class ConfirmationFixture(
        val database: WakeveDb,
        val repository: DatabaseEventRepository,
        val event: Event,
        val slot: TimeSlot,
        val network: ControlledNetworkDetector,
        val httpClient: CapturingSyncHttpClient,
        val syncManager: SyncManager
    ) {
        val domainEventId: String get() = DOMAIN_EVENT_ID
        val effectKey: String get() = EFFECT_KEY
        val operationId: String get() = OPERATION_ID
    }

    private class ControlledNetworkDetector : NetworkStatusDetector {
        private val available = MutableStateFlow(false)
        override val isNetworkAvailable: StateFlow<Boolean> = available

        fun setAvailable(value: Boolean) {
            available.value = value
        }
    }

    private class CapturingSyncHttpClient(
        private val response: SyncResponse
    ) : SyncHttpClient {
        val requests = mutableListOf<com.guyghost.wakeve.models.SyncRequest>()

        override suspend fun sync(requestJson: String, authToken: String): Result<String> = runCatching {
            requests += Json.decodeFromString(com.guyghost.wakeve.models.SyncRequest.serializer(), requestJson)
            Json.encodeToString(SyncResponse.serializer(), response)
        }
    }

    private companion object {
        const val EVENT_ID = "local-confirmation-event"
        const val SLOT_ID = "local-confirmation-slot"
        const val ORGANIZER_ID = "local-organizer"
        const val OPERATION_ID = "local-confirmation-operation"
        const val TIMESTAMP = "2026-07-09T10:00:00Z"
        const val DOMAIN_EVENT_ID = "poll-date-confirmed:$EVENT_ID:$SLOT_ID:v1"
        const val EFFECT_KEY = "$DOMAIN_EVENT_ID:confirmation"
    }
}
