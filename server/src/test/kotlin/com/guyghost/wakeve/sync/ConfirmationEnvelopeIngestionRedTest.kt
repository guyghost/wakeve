package com.guyghost.wakeve.sync

import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.SyncChange
import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.SyncRequest
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.repository.DatabaseEventRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * RED contract for the backend boundary after a locally committed confirmation envelope.
 *
 * The stable acknowledgement deliberately uses the existing sync response until the
 * production API exposes a dedicated acknowledgement object. It prevents a client from
 * interpreting envelope acceptance as either participant delivery or calendar dispatch.
 */
class ConfirmationEnvelopeIngestionRedTest {
    @BeforeTest
    fun setup() {
        DatabaseProvider.resetDatabase()
    }

    @AfterTest
    fun teardown() {
        DatabaseProvider.resetDatabase()
    }

    @Test
    fun confirmationEnvelopeIsAcknowledgedIdempotentlyWhileEffectDispatchRemainsPending() = runBlocking {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        seedConfirmedEvent(database)
        val service = SyncService(database)

        val first = service.processSyncChanges(envelopeRequest(), ORGANIZER_ID)
        val replay = service.processSyncChanges(envelopeRequest(), ORGANIZER_ID)

        listOf(first, replay).forEach { response ->
            assertTrue(response.success, response.message.orEmpty())
            assertEquals(1, response.appliedChanges, response.message.orEmpty())
            assertTrue(response.conflicts.isEmpty(), response.conflicts.toString())
            assertEquals(ACKNOWLEDGED_PENDING_DISPATCH, response.message)
        }
    }

    @Test
    fun confirmationEnvelopeAcknowledgementKeepsRecipientAndCalendarFanOutDisabledUntilReady() = runBlocking {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        seedConfirmedEvent(database)
        val service = SyncService(database)

        val response = service.processSyncChanges(envelopeRequest(), ORGANIZER_ID)

        assertTrue(response.success, response.message.orEmpty())
        assertEquals(ACKNOWLEDGED_PENDING_DISPATCH, response.message)
        assertTrue(
            response.message.orEmpty().contains("fan-out-disabled"),
            "Envelope acknowledgement must declare recipient and calendar fan-out disabled before readiness"
        )
    }

    @Test
    fun confirmationEnvelopeIsRejectedWhenItsSlotDiffersFromThePersistedConfirmedDate() = runBlocking {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        seedConfirmedEvent(database, confirmedSlotId = ALTERNATE_SLOT_ID)

        val response = SyncService(database).processSyncChanges(
            envelopeRequest(slotId = SLOT_ID),
            ORGANIZER_ID
        )

        assertTrue(response.success, response.message.orEmpty())
        assertEquals(0, response.appliedChanges, response.message.orEmpty())
        assertEquals(1, response.conflicts.size, response.conflicts.toString())
        assertEquals(
            null,
            database.confirmationEffectOutboxQueries
                .selectByDomainEventId(domainEventId(SLOT_ID))
                .executeAsOneOrNull(),
            "A slot that merely belongs to the confirmed event must not be acknowledged"
        )
    }

    @Test
    fun acknowledgementCorrelatesEveryAcceptedEnvelopeWithItsDecisionIdentityAndReceipt() = runBlocking {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        seedConfirmedEvent(database)

        val response = SyncService(database).processSyncChanges(envelopeRequest(), ORGANIZER_ID)

        val acknowledgement = assertNotNull(
            confirmationAcknowledgements(response).singleOrNull(),
            "The sync response must return a receipt-correlated acknowledgement for the accepted envelope"
        )
        assertEquals(DOMAIN_EVENT_ID, acknowledgementValue(acknowledgement, "DomainEventId"))
        assertEquals(EFFECT_KEY, acknowledgementValue(acknowledgement, "EffectKey"))
        assertEquals(OPERATION_ID, acknowledgementValue(acknowledgement, "OperationId"))
        assertTrue(
            acknowledgementValue(acknowledgement, "ReceiptId")?.toString()?.isNotBlank() == true,
            "A non-empty acknowledgement receipt is required so a client can mark only this decision synced"
        )
    }

    @Test
    fun acknowledgementSurvivesNewServiceInstanceAndRejectsConflictingEnvelopeWithoutReplacingReceipt() = runBlocking {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        seedConfirmedEvent(database)

        val first = SyncService(database).processSyncChanges(envelopeRequest(), ORGANIZER_ID)
        val replayAfterRestart = SyncService(database).processSyncChanges(envelopeRequest(), ORGANIZER_ID)
        val conflicting = SyncService(database).processSyncChanges(
            envelopeRequest(operationId = "different-confirmation-operation"),
            ORGANIZER_ID
        )

        assertEquals(1, first.appliedChanges, first.message.orEmpty())
        assertEquals(1, replayAfterRestart.appliedChanges, replayAfterRestart.message.orEmpty())
        assertEquals(0, conflicting.appliedChanges, conflicting.message.orEmpty())
        assertEquals(1, conflicting.conflicts.size, conflicting.conflicts.toString())
        assertEquals(
            OPERATION_ID,
            database.confirmationEffectOutboxQueries
                .selectByDomainEventId(DOMAIN_EVENT_ID)
                .executeAsOne()
                .operationId,
            "A conflicting envelope must not overwrite the original acknowledged decision"
        )
    }

    @Test
    fun confirmedDecisionSyncAppliesTheExactSlotBeforeItsEnvelopeIsAcknowledged() = runBlocking {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        seedPollingEvent(database)

        val response = SyncService(database).processSyncChanges(
            SyncRequest(changes = listOf(confirmedDecisionChange(), envelopeRequest().changes.single())),
            ORGANIZER_ID
        )

        assertTrue(response.success, response.message.orEmpty())
        assertEquals(2, response.appliedChanges, response.message.orEmpty())
        assertTrue(response.conflicts.isEmpty(), response.conflicts.toString())
        assertEquals(EventStatus.CONFIRMED, DatabaseEventRepository(database).getEvent(EVENT_ID)?.status)
        assertEquals(
            SLOT_ID,
            database.confirmedDateQueries.selectByEventId(EVENT_ID).executeAsOne().timeslotId,
            "The server must persist the exact selected slot before accepting its envelope"
        )
        assertEquals(
            "ACKNOWLEDGED_PENDING_DISPATCH",
            database.confirmationEffectOutboxQueries
                .selectByDomainEventId(DOMAIN_EVENT_ID)
                .executeAsOne()
                .status
        )
    }

    private suspend fun seedConfirmedEvent(
        database: WakeveDb,
        confirmedSlotId: String = SLOT_ID
    ) {
        database.userQueries.insertUser(
            id = ORGANIZER_ID,
            provider_id = "provider-$ORGANIZER_ID",
            email = "$ORGANIZER_ID@example.test",
            name = ORGANIZER_ID,
            avatar_url = null,
            provider = "google",
            role = "USER",
            created_at = CREATED_AT,
            updated_at = CREATED_AT
        )

        DatabaseEventRepository(database).createEvent(
            Event(
                id = EVENT_ID,
                title = "Confirmed event",
                description = "Envelope ingestion contract",
                organizerId = ORGANIZER_ID,
                proposedSlots = listOf(
                    TimeSlot(
                        id = SLOT_ID,
                        start = SLOT_START,
                        end = "2026-08-16T12:00:00Z",
                        timezone = "UTC",
                        timeOfDay = TimeOfDay.SPECIFIC
                    ),
                    TimeSlot(
                        id = ALTERNATE_SLOT_ID,
                        start = ALTERNATE_SLOT_START,
                        end = "2026-08-17T12:00:00Z",
                        timezone = "UTC",
                        timeOfDay = TimeOfDay.SPECIFIC
                    )
                ),
                deadline = "2026-08-01T00:00:00Z",
                status = EventStatus.CONFIRMED,
                finalDate = if (confirmedSlotId == SLOT_ID) SLOT_START else ALTERNATE_SLOT_START,
                createdAt = CREATED_AT,
                updatedAt = CREATED_AT,
                eventType = EventType.OTHER
            )
        ).getOrThrow()

        database.confirmedDateQueries.insertConfirmedDate(
            id = "confirmed-$EVENT_ID",
            eventId = EVENT_ID,
            timeslotId = confirmedSlotId,
            confirmedByOrganizerId = ORGANIZER_ID,
            confirmedAt = CREATED_AT,
            updatedAt = CREATED_AT
        )
    }

    private suspend fun seedPollingEvent(database: WakeveDb) {
        database.userQueries.insertUser(
            id = ORGANIZER_ID,
            provider_id = "provider-$ORGANIZER_ID",
            email = "$ORGANIZER_ID@example.test",
            name = ORGANIZER_ID,
            avatar_url = null,
            provider = "google",
            role = "USER",
            created_at = CREATED_AT,
            updated_at = CREATED_AT
        )
        DatabaseEventRepository(database).createEvent(
            Event(
                id = EVENT_ID,
                title = "Polling event",
                description = "Confirmed decision sync contract",
                organizerId = ORGANIZER_ID,
                proposedSlots = listOf(
                    TimeSlot(
                        id = SLOT_ID,
                        start = SLOT_START,
                        end = "2026-08-16T12:00:00Z",
                        timezone = "UTC",
                        timeOfDay = TimeOfDay.SPECIFIC
                    ),
                    TimeSlot(
                        id = ALTERNATE_SLOT_ID,
                        start = ALTERNATE_SLOT_START,
                        end = "2026-08-17T12:00:00Z",
                        timezone = "UTC",
                        timeOfDay = TimeOfDay.SPECIFIC
                    )
                ),
                deadline = "2026-08-01T00:00:00Z",
                status = EventStatus.POLLING,
                createdAt = CREATED_AT,
                updatedAt = CREATED_AT,
                eventType = EventType.OTHER
            )
        ).getOrThrow()
    }

    private fun envelopeRequest(
        slotId: String = SLOT_ID,
        operationId: String = OPERATION_ID
    ) = SyncRequest(
        changes = listOf(
            SyncChange(
                id = ENVELOPE_ID,
                table = "confirmation_effect_outbox",
                operation = SyncOperation.CREATE.name,
                recordId = domainEventId(slotId),
                data = """
                    {
                      "domainEventId":"${domainEventId(slotId)}",
                      "effectKey":"${effectKey(slotId)}",
                      "eventId":"$EVENT_ID",
                      "slotId":"$slotId",
                      "operationId":"$operationId",
                      "createdAt":"$CREATED_AT"
                    }
                """.trimIndent(),
                timestamp = CREATED_AT,
                userId = ORGANIZER_ID
            )
        )
    )

    private fun confirmedDecisionChange() = SyncChange(
        id = "sync-confirmed-decision-1",
        table = "events",
        operation = SyncOperation.UPDATE.name,
        recordId = EVENT_ID,
        data = """
            {
              "id":"$EVENT_ID",
              "title":"Confirmed event",
              "description":"Decision synchronized before its envelope",
              "organizerId":"$ORGANIZER_ID",
              "deadline":"2026-08-01T00:00:00Z",
              "timezone":"UTC",
              "status":"CONFIRMED",
              "confirmedSlotId":"$SLOT_ID",
              "finalDate":"$SLOT_START"
            }
        """.trimIndent(),
        timestamp = CREATED_AT,
        userId = ORGANIZER_ID
    )

    private fun confirmationAcknowledgements(response: Any): List<Any> {
        val getter = assertNotNull(
            response.javaClass.methods.singleOrNull {
                it.name == "getConfirmationAcknowledgements" && it.parameterCount == 0
            },
            "SyncResponse must expose one typed acknowledgement per accepted confirmation envelope"
        )
        return assertNotNull(
            getter.invoke(response) as? Iterable<*>,
            "Confirmation acknowledgements must be iterable"
        ).filterNotNull()
    }

    private fun acknowledgementValue(acknowledgement: Any, suffix: String): Any? =
        acknowledgement.javaClass.methods
            .singleOrNull { it.name == "get$suffix" && it.parameterCount == 0 }
            ?.invoke(acknowledgement)

    private companion object {
        const val ORGANIZER_ID = "confirmation-organizer"
        const val EVENT_ID = "confirmation-event"
        const val SLOT_ID = "confirmation-slot"
        const val SLOT_START = "2026-08-16T10:00:00Z"
        const val ALTERNATE_SLOT_ID = "confirmation-alternate-slot"
        const val ALTERNATE_SLOT_START = "2026-08-17T10:00:00Z"
        const val CREATED_AT = "2026-07-09T10:00:00Z"
        const val DOMAIN_EVENT_ID = "poll-date-confirmed:$EVENT_ID:$SLOT_ID:v1"
        const val EFFECT_KEY = "$DOMAIN_EVENT_ID:confirmation"
        const val ENVELOPE_ID = "sync-confirmation-envelope-1"
        const val OPERATION_ID = "confirmation-operation-1"
        const val ACKNOWLEDGED_PENDING_DISPATCH =
            "confirmation-envelope-acknowledged; effect-dispatch-pending; fan-out-disabled"
    }

    private fun domainEventId(slotId: String): String = "poll-date-confirmed:$EVENT_ID:$slotId:v1"

    private fun effectKey(slotId: String): String = "${domainEventId(slotId)}:confirmation"
}
