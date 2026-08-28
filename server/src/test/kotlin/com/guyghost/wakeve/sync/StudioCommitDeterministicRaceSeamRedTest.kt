package com.guyghost.wakeve.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.invitationexperience.Artwork
import com.guyghost.wakeve.invitationexperience.StudioCommitEnvelopeFactory
import com.guyghost.wakeve.invitationexperience.StudioEventFields
import com.guyghost.wakeve.invitationexperience.StudioPendingSyncSubject
import com.guyghost.wakeve.invitationexperience.UpdateDraftAggregateCommand
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.SyncChange
import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.SyncRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StudioCommitDeterministicRaceSeamRedTest {

    @Test
    fun `loser that pre-read no receipt converges on winner ACK through deterministic race seam`() = runBlocking {
        val databaseFile = Files.createTempFile("wakeve-studio-deterministic-race-", ".sqlite")
        val firstDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.toAbsolutePath()}")
        val secondDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.toAbsolutePath()}")
        try {
            WakeveDb.Schema.create(firstDriver)
            val loserPreReadNone = CountDownLatch(1)
            val winnerCommitted = CountDownLatch(1)
            val loserService = SyncService(
                db = WakeveDb(firstDriver),
                studioPreReadNoneBarrier = { operationId: String ->
                    assertEquals(OPERATION_ID, operationId)
                    loserPreReadNone.countDown()
                    assertTrue(
                        winnerCommitted.await(5, TimeUnit.SECONDS),
                        "The deterministic loser barrier timed out waiting for the winner receipt."
                    )
                }
            )
            val winnerService = SyncService(WakeveDb(secondDriver))
            val request = request()

            val loser = async(Dispatchers.IO) {
                loserService.processSyncChanges(request, ACTOR_ID)
            }
            assertTrue(
                loserPreReadNone.await(5, TimeUnit.SECONDS),
                "The loser must prove its pre-read observed no durable receipt before the winner commits."
            )
            val winner = winnerService.processSyncChanges(request, ACTOR_ID)
            winnerCommitted.countDown()
            val loserResponse = loser.await()

            assertTrue(winner.conflicts.isEmpty())
            assertTrue(loserResponse.conflicts.isEmpty())
            val winnerAck = assertNotNull(winner.studioAcknowledgements.singleOrNull())
            val loserAck = assertNotNull(loserResponse.studioAcknowledgements.singleOrNull())
            assertEquals(Json.encodeToString(winnerAck), Json.encodeToString(loserAck))
            assertEquals(Artwork.None, loserAck.artwork)
        } finally {
            firstDriver.close()
            secondDriver.close()
            Files.deleteIfExists(databaseFile)
        }
    }

    @Test
    fun `loser returns transaction captured winner ACK after aggregate mutates before outer processing`() = runBlocking {
        val databaseFile = Files.createTempFile("wakeve-studio-transaction-result-race-", ".sqlite")
        val firstDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.toAbsolutePath()}")
        val secondDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.toAbsolutePath()}")
        try {
            WakeveDb.Schema.create(firstDriver)
            val loserPreReadNone = CountDownLatch(1)
            val winnerCommitted = CountDownLatch(1)
            val loserTransactionReturned = CountDownLatch(1)
            val aggregateMutated = CountDownLatch(1)
            val loserService = SyncService(
                db = WakeveDb(firstDriver),
                studioPreReadNoneBarrier = { operationId: String ->
                    assertEquals(OPERATION_ID, operationId)
                    loserPreReadNone.countDown()
                    assertTrue(
                        winnerCommitted.await(5, TimeUnit.SECONDS),
                        "The deterministic loser timed out before entering the receipt-owning transaction."
                    )
                },
                studioAfterCommitTransactionBarrier = { operationId: String ->
                    assertEquals(OPERATION_ID, operationId)
                    loserTransactionReturned.countDown()
                    assertTrue(
                        aggregateMutated.await(5, TimeUnit.SECONDS),
                        "The loser must be paused after its transaction result and before outer processing."
                    )
                }
            )
            val winnerDatabase = WakeveDb(secondDriver)
            val winnerService = SyncService(winnerDatabase)
            val syncRequest = request()

            val loser = async(Dispatchers.IO) {
                loserService.processSyncChanges(syncRequest, ACTOR_ID)
            }
            assertTrue(loserPreReadNone.await(5, TimeUnit.SECONDS))
            val winnerResponse = winnerService.processSyncChanges(syncRequest, ACTOR_ID)
            val winnerAck = assertNotNull(winnerResponse.studioAcknowledgements.singleOrNull())
            val winnerAckBytes = Json.encodeToString(winnerAck)
            winnerCommitted.countDown()

            assertTrue(
                loserTransactionReturned.await(5, TimeUnit.SECONDS),
                "The loser must expose a deterministic post-transaction seam."
            )
            secondDriver.execute(
                identifier = null,
                sql = "UPDATE event SET status = 'POLLING', aggregateRevision = 77 WHERE id = ?",
                parameters = 1
            ) {
                bindString(0, EVENT_ID)
            }.value
            aggregateMutated.countDown()

            val loserResponse = loser.await()
            assertTrue(loserResponse.conflicts.isEmpty(), loserResponse.conflicts.toString())
            val loserAck = assertNotNull(
                loserResponse.studioAcknowledgements.singleOrNull(),
                "The ACK discovered inside the loser transaction is its immutable result, not a later aggregate projection."
            )
            assertEquals(winnerAckBytes, Json.encodeToString(loserAck))
            val externallyMutated = assertNotNull(winnerDatabase.eventQueries.selectById(EVENT_ID).executeAsOneOrNull())
            assertEquals(77L, externallyMutated.aggregateRevision)
            assertEquals("POLLING", externallyMutated.status)
        } finally {
            firstDriver.close()
            secondDriver.close()
            Files.deleteIfExists(databaseFile)
        }
    }

    @Test
    fun `loser rejects captured ACK when the intra transaction receipt tuple is inconsistent`() = runBlocking {
        data class CorruptionCase(
            val name: String,
            val mutateWinnerReceipt: (JdbcSqliteDriver) -> Unit
        )
        val cases = listOf(
            CorruptionCase("substituted server receipt id") { driver ->
                driver.execute(
                    identifier = null,
                    sql = "UPDATE event_operation_receipt SET server_receipt_id = ? WHERE operation_id = ?",
                    parameters = 2
                ) {
                    bindString(0, "substituted-server-receipt")
                    bindString(1, OPERATION_ID)
                }.value
            },
            CorruptionCase("non committed receipt status") { driver ->
                driver.execute(
                    identifier = null,
                    sql = "UPDATE event_operation_receipt SET status = 'PENDING_SYNC' WHERE operation_id = ?",
                    parameters = 1
                ) {
                    bindString(0, OPERATION_ID)
                }.value
            }
        )

        cases.forEach { case ->
            val databaseFile = Files.createTempFile("wakeve-studio-inconsistent-receipt-", ".sqlite")
            val loserDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.toAbsolutePath()}")
            val winnerDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.toAbsolutePath()}")
            try {
                WakeveDb.Schema.create(loserDriver)
                val loserPreReadNone = CountDownLatch(1)
                val corruptedWinnerReceiptReady = CountDownLatch(1)
                val loserService = SyncService(
                    db = WakeveDb(loserDriver),
                    studioPreReadNoneBarrier = { operationId: String ->
                        assertEquals(OPERATION_ID, operationId)
                        loserPreReadNone.countDown()
                        assertTrue(
                            corruptedWinnerReceiptReady.await(5, TimeUnit.SECONDS),
                            "${case.name}: the loser must enter its transaction only after receipt corruption."
                        )
                    }
                )
                val winnerService = SyncService(WakeveDb(winnerDriver))
                val syncRequest = request()

                val loser = async(Dispatchers.IO) {
                    loserService.processSyncChanges(syncRequest, ACTOR_ID)
                }
                assertTrue(loserPreReadNone.await(5, TimeUnit.SECONDS), case.name)
                val winnerResponse = winnerService.processSyncChanges(syncRequest, ACTOR_ID)
                assertNotNull(winnerResponse.studioAcknowledgements.singleOrNull(), case.name)
                case.mutateWinnerReceipt(winnerDriver)
                corruptedWinnerReceiptReady.countDown()

                val loserResponse = loser.await()
                assertTrue(
                    loserResponse.studioAcknowledgements.isEmpty(),
                    "${case.name}: a valid JSON payload is insufficient proof when its receipt tuple is corrupt."
                )
                assertEquals(0, loserResponse.appliedChanges, case.name)
                val conflict = assertNotNull(loserResponse.conflicts.singleOrNull(), case.name)
                assertEquals("REPOSITORY_INCONSISTENT", conflict.code, case.name)
                assertEquals(false, conflict.retryable, case.name)
            } finally {
                loserDriver.close()
                winnerDriver.close()
                Files.deleteIfExists(databaseFile)
            }
        }
    }

    private fun request(): SyncRequest {
        val command = UpdateDraftAggregateCommand(
            eventId = EVENT_ID,
            actorId = ACTOR_ID,
            expectedBaseRevision = 0,
            eventDraft = StudioEventFields(
                title = "Deterministic race",
                description = "The loser must return the exact durable winner acknowledgement.",
                deadline = "2099-01-01T00:00:00Z",
                eventType = EventType.OTHER
            ),
            artwork = Artwork.None,
            operationId = OPERATION_ID,
            draftRevision = 1
        )
        val envelope = StudioCommitEnvelopeFactory.build(command)
        val subject = StudioPendingSyncSubject(
            eventId = EVENT_ID,
            committedRevision = 1,
            localReceiptId = OPERATION_ID,
            envelope = envelope,
            expectedResultingArtwork = Artwork.None
        )
        return SyncRequest(
            changes = listOf(
                SyncChange(
                    id = "sync:$OPERATION_ID",
                    table = "studio_commit",
                    operation = SyncOperation.CREATE.name,
                    recordId = OPERATION_ID,
                    data = Json.encodeToString(subject),
                    timestamp = "2030-01-01T00:00:00Z",
                    userId = ACTOR_ID
                )
            )
        )
    }

    private companion object {
        const val EVENT_ID = "deterministic-studio-race-event"
        const val ACTOR_ID = "deterministic-studio-race-actor"
        const val OPERATION_ID = "deterministic-studio-race-operation"
    }
}
