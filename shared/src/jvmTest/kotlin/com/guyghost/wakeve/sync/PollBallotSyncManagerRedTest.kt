package com.guyghost.wakeve.sync

import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.poll.PollBallotContract
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.UserRepository
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PollBallotSyncManagerRedTest {

    @Test
    fun `missing receipt and malformed pending ballot metadata stay visible as inconsistencies`() {
        val source = listOf(
            File("src/commonMain/kotlin/com/guyghost/wakeve/sync/SyncManager.kt"),
            File("shared/src/commonMain/kotlin/com/guyghost/wakeve/sync/SyncManager.kt")
        ).first(File::isFile).readText()
        val projection = source.substringAfter("private fun pendingBallotSyncs")
            .substringBefore("private suspend fun handleConflict")

        assertTrue(
            source.contains("PendingBallotSyncJoinProjection"),
            "SyncManager needs a one-row/one-projection API so missing receipts remain observable."
        )
        assertTrue(
            projection.contains("RECEIPT_MISSING") && projection.contains("PAYLOAD_MALFORMED"),
            "Absent receipts and malformed payloads must be named, not silently skipped."
        )
        assertTrue(
            projection.contains("REPOSITORY_INCONSISTENT") &&
                projection.contains("CommitOutcome.UNKNOWN") &&
                projection.contains("retryable = false"),
            "Every invalid pending row is terminal non-retryable UNKNOWN."
        )
        assertTrue(
            !projection.contains(".mapNotNull"),
            "Lossless pending metadata projection must never filter inconsistent rows."
        )
    }

    @Test
    fun `pending ballot join inconsistencies are exposed by a public diagnostic projection boundary`() {
        val source = listOf(
            File("src/commonMain/kotlin/com/guyghost/wakeve/sync/SyncManager.kt"),
            File("shared/src/commonMain/kotlin/com/guyghost/wakeve/sync/SyncManager.kt")
        ).first(File::isFile).readText()

        assertTrue(
            source.contains("fun getPendingBallotSyncJoinProjections") &&
                !source.contains("private fun getPendingBallotSyncJoinProjections"),
            "UI and diagnostics need the lossless PendingBallotSyncJoinProjection union, including Inconsistent rows."
        )
        assertTrue(
            source.contains("getPendingBallotSyncJoinProjections(): List<PendingBallotSyncJoinProjection>"),
            "The public projection must preserve typed inconsistencies instead of exposing only transportable SyncChange rows."
        )
    }

    @Test
    fun `pending poll ballot publishes its exact non empty command and local receipt`() = runBlocking {
        val fixture = committedBallotFixture()

        val change = assertNotNull(
            fixture.syncManager.getPendingChangesForSync().singleOrNull { it.table == "poll_ballot" },
            "A LOCAL_PENDING ballot must be represented in the durable sync batch."
        )
        val payload = Json.parseToJsonElement(change.data).jsonObject
        assertEquals(1, payload["schemaVersion"]?.jsonPrimitive?.content?.toInt())
        assertEquals(fixture.receiptId, payload["localReceiptId"]?.jsonPrimitive?.content)

        val command = assertNotNull(payload["command"]?.jsonObject)
        assertEquals(1, command["schemaVersion"]?.jsonPrimitive?.content?.toInt())
        val identity = assertNotNull(command["identity"]?.jsonObject)
        assertEquals(EVENT_ID, identity["eventId"]?.jsonPrimitive?.content)
        assertEquals(ACTOR_ID, identity["actorId"]?.jsonPrimitive?.content)
        assertEquals(fixture.pollRevision.toString(), identity["pollRevision"]?.jsonPrimitive?.content)
        assertEquals(OPERATION_ID, identity["operationId"]?.jsonPrimitive?.content)
        assertEquals(
            fixture.fingerprint,
            command["ballotFingerprint"]?.jsonPrimitive?.content
        )
        assertEquals(
            listOf("slot-1:YES", "slot-2:MAYBE"),
            command["entries"]?.jsonArray?.map { entry ->
                val value = entry.jsonObject
                "${value["slotId"]?.jsonPrimitive?.content}:${value["choice"]?.jsonPrimitive?.content}"
            }
        )
        assertTrue(change.data != "{}" && change.data.isNotBlank())
    }

    @Test
    fun `generic successful batch without ballot acknowledgement keeps receipt pending`() = runBlocking {
        val fixture = committedBallotFixture(responseJson = successfulResponse())
        fixture.enqueueLegacyTransportRow()

        fixture.network.setAvailable(true)
        fixture.syncManager.triggerSync().getOrThrow()

        assertEquals("LOCAL_PENDING", fixture.receiptStatus())
    }

    @Test
    fun `only a correlated non empty ballot acknowledgement marks the receipt synced`() = runBlocking {
        val fixture = committedBallotFixture(
            responseJson = successfulResponse(
                ballotAcknowledgements = """
                    [{
                      "localReceiptId":"LOCAL_RECEIPT",
                      "serverReceiptId":"server-ballot-receipt-1",
                      "identity":{
                        "eventId":"$EVENT_ID",
                        "actorId":"$ACTOR_ID",
                        "pollRevision":POLL_REVISION,
                        "operationId":"$OPERATION_ID"
                      },
                      "ballotFingerprint":"BALLOT_FINGERPRINT",
                      "outcome":"APPLIED"
                    }]
                """.trimIndent()
            )
        )
        fixture.responseJson = fixture.responseJson
            .replace("LOCAL_RECEIPT", fixture.receiptId)
            .replace("POLL_REVISION", fixture.pollRevision.toString())
            .replace("BALLOT_FINGERPRINT", fixture.fingerprint)
        fixture.enqueueLegacyTransportRow()

        fixture.network.setAvailable(true)
        fixture.syncManager.triggerSync().getOrThrow()

        assertEquals(
            "SERVER_ACKNOWLEDGED",
            fixture.receiptStatus(),
            "Batch success is insufficient; the exact receipt, tuple and fingerprint acknowledgement owns this transition."
        )
    }

    @Test
    fun `blank wrong or conflicted ballot acknowledgement remains retryable`() = runBlocking {
        val cases = listOf(
            """[{"localReceiptId":"","serverReceiptId":"server-1","identity":{},"ballotFingerprint":"x","outcome":"APPLIED"}]""",
            """[{"localReceiptId":"other-receipt","serverReceiptId":"server-2","identity":{"eventId":"other"},"ballotFingerprint":"x","outcome":"APPLIED"}]""",
            """[{"localReceiptId":"LOCAL_RECEIPT","serverReceiptId":"server-3","identity":{"eventId":"$EVENT_ID","actorId":"$ACTOR_ID","pollRevision":POLL_REVISION,"operationId":"$OPERATION_ID"},"ballotFingerprint":"BALLOT_FINGERPRINT","outcome":"APPLIED"}]"""
        )

        cases.forEachIndexed { index, acknowledgement ->
            val conflictJson = if (index == 2) {
                """[{"changeId":"ballot-conflict","table":"poll_ballot","recordId":"LOCAL_RECEIPT","clientData":"{}","serverData":"{}","resolution":"SERVER_WINS"}]"""
            } else {
                "[]"
            }
            val fixture = committedBallotFixture(
                responseJson = successfulResponse(
                    ballotAcknowledgements = acknowledgement,
                    conflicts = conflictJson
                )
            )
            fixture.responseJson = fixture.responseJson
                .replace("LOCAL_RECEIPT", fixture.receiptId)
                .replace("POLL_REVISION", fixture.pollRevision.toString())
                .replace("BALLOT_FINGERPRINT", fixture.fingerprint)
            fixture.enqueueLegacyTransportRow()

            fixture.network.setAvailable(true)
            fixture.syncManager.triggerSync().getOrThrow()

            assertEquals("LOCAL_PENDING", fixture.receiptStatus(), "case=$index")
        }
    }

    private suspend fun committedBallotFixture(
        responseJson: String = successfulResponse()
    ): Fixture {
        val database = createFreshTestDatabase()
        val repository = DatabaseEventRepository(
            database,
            ConfirmationClock { Instant.parse(NOW) }
        )
        repository.createEvent(
            createTestEvent(
                id = EVENT_ID,
                organizerId = ACTOR_ID,
                proposedSlots = listOf(
                    createTestTimeSlot("slot-1", "2026-09-01T10:00:00Z"),
                    createTestTimeSlot("slot-2", "2026-09-02T10:00:00Z")
                ),
                deadline = DEADLINE,
                status = EventStatus.POLLING
            )
        ).getOrThrow()
        val event = requireNotNull(repository.getEvent(EVENT_ID))
        val result = repository.commitCompleteBallot(
            PollBallotContract.CommitCompleteBallotCommand(
                eventId = EVENT_ID,
                actorId = ACTOR_ID,
                pollRevision = event.aggregateRevision,
                entries = listOf(
                    PollBallotContract.BallotEntry("slot-1", Vote.YES),
                    PollBallotContract.BallotEntry("slot-2", Vote.MAYBE)
                ),
                operationId = OPERATION_ID
            )
        )
        val receipt = when (result) {
            is PollBallotContract.CommitResult.Committed -> result.receipt
            else -> error("fixture ballot was not committed: $result")
        }

        val network = ControlledNetworkDetector()
        val http = RawResponseHttpClient(responseJson)
        val userRepository = UserRepository(database)
        val manager = SyncManager(
            database = database,
            eventRepository = repository,
            userRepository = userRepository,
            networkDetector = network,
            httpClient = http,
            authTokenProvider = { "test-token" },
            maxRetries = 0
        )
        return Fixture(
            database,
            userRepository,
            network,
            http,
            manager,
            receipt.receiptId,
            receipt.pollRevision,
            receipt.ballotFingerprint
        )
    }

    private class Fixture(
        val database: WakeveDb,
        private val userRepository: UserRepository,
        val network: ControlledNetworkDetector,
        private val http: RawResponseHttpClient,
        val syncManager: SyncManager,
        val receiptId: String,
        val pollRevision: Long,
        val fingerprint: String
    ) {
        var responseJson: String
            get() = http.responseJson
            set(value) { http.responseJson = value }

        suspend fun enqueueLegacyTransportRow() {
            userRepository.addSyncMetadata(
                id = "transport-$receiptId",
                tableName = "poll_ballot",
                recordId = receiptId,
                operation = SyncOperation.UPDATE,
                timestamp = NOW,
                userId = ACTOR_ID
            ).getOrThrow()
        }

        fun receiptStatus(): String = database.pollBallotReceiptQueries
            .selectByOperationId(OPERATION_ID)
            .executeAsOne()
            .syncStatus
    }

    private class ControlledNetworkDetector : NetworkStatusDetector {
        private val available = MutableStateFlow(false)
        override val isNetworkAvailable: StateFlow<Boolean> = available

        fun setAvailable(value: Boolean) {
            available.value = value
        }
    }

    private class RawResponseHttpClient(
        var responseJson: String
    ) : SyncHttpClient {
        val requests = mutableListOf<String>()

        override suspend fun sync(requestJson: String, authToken: String): Result<String> {
            requests += requestJson
            return Result.success(responseJson)
        }
    }

    private fun successfulResponse(
        ballotAcknowledgements: String = "[]",
        conflicts: String = "[]"
    ): String = """
        {
          "success":true,
          "appliedChanges":1,
          "conflicts":$conflicts,
          "serverTimestamp":"2026-08-28T11:05:00Z",
          "ballotAcknowledgements":$ballotAcknowledgements
        }
    """.trimIndent()

    private companion object {
        const val EVENT_ID = "sync-ballot-event"
        const val ACTOR_ID = "sync-ballot-actor"
        const val OPERATION_ID = "sync-ballot-operation"
        const val NOW = "2026-08-28T11:00:00Z"
        const val DEADLINE = "2026-08-28T12:00:00Z"
    }
}
