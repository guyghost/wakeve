package com.guyghost.wakeve.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseEventRepositoryConfirmDateTest {

    private lateinit var db: WakeveDb
    private lateinit var repository: DatabaseEventRepository
    private lateinit var jdbcUrl: String

    @BeforeTest
    fun setup() {
        jdbcUrl = "jdbc:sqlite:${Files.createTempFile("wakeve-confirmation-", ".db")}"
        val driver = JdbcSqliteDriver(jdbcUrl)
        WakeveDb.Schema.create(driver)
        db = WakeveDb(driver)
        repository = DatabaseEventRepository(db)
    }

    @Test
    fun `confirmEventDate stores the selected slot instead of the first proposed slot`() = runBlocking {
        val firstSlot = createTestTimeSlot(
            id = "slot-first",
            start = "2026-07-10T09:00:00Z",
            end = "2026-07-10T11:00:00Z"
        )
        val selectedSecondSlot = createTestTimeSlot(
            id = "slot-selected-second",
            start = "2026-07-11T14:00:00Z",
            end = "2026-07-11T16:00:00Z"
        )
        val event = createTestEvent(
            id = "event-confirm-selected-slot",
            organizerId = "organizer-1",
            proposedSlots = listOf(firstSlot, selectedSecondSlot),
            status = EventStatus.POLLING,
            deadline = "2026-07-01T23:59:59Z"
        )
        assertTrue(repository.createEvent(event).isSuccess)
        persistVote(db, event, selectedSecondSlot.id)

        val result = repository.confirmEventDate(
            eventId = event.id,
            slotId = selectedSecondSlot.id,
            confirmedByOrganizerId = event.organizerId
        )

        assertTrue(result.isSuccess)
        val confirmedDate = db.confirmedDateQueries
            .selectByEventId(event.id)
            .executeAsOne()
        assertEquals(selectedSecondSlot.id, confirmedDate.timeslotId)
        assertEquals(event.organizerId, confirmedDate.confirmedByOrganizerId)

        val storedEvent = db.eventQueries.selectById(event.id).executeAsOne()
        assertEquals(EventStatus.CONFIRMED.name, storedEvent.status)
    }

    @Test
    fun `confirmation atomically commits decision date one envelope sync and receipt`() = runBlocking {
        val event = pollingEvent("event-atomic")
        val operationId = "operation-atomic"
        val capturedAt = "2026-07-09T19:23:45Z"
        assertTrue(repository.createEvent(event).isSuccess)
        persistVote(db, event)
        val clockedRepository = DatabaseEventRepository(
            db,
            com.guyghost.wakeve.confirmation.ConfirmationClock {
                kotlinx.datetime.Instant.parse(capturedAt)
            }
        )

        val result = clockedRepository.confirmPollDate(confirmationCommand(event, operationId, capturedAt))

        assertIs<EventManagementContract.ConfirmationResult.Committed>(result)
        assertEquals(EventStatus.CONFIRMED, repository.getEvent(event.id)?.status)
        assertEquals("slot-selected", db.confirmedDateQueries.selectByEventId(event.id).executeAsOne().timeslotId)
        assertEquals(1, durableRowCount("confirmation_effect_outbox", "eventId", event.id))
        assertEquals(
            "poll-date-confirmed:${event.id}:slot-selected:v1",
            selectColumnValue("confirmation_effect_outbox", "domainEventId", "eventId", event.id)
        )
        assertEquals(
            "poll-date-confirmed:${event.id}:slot-selected:v1:confirmation",
            selectColumnValue("confirmation_effect_outbox", "effectKey", "eventId", event.id)
        )
        assertEquals(
            "QUEUED",
            selectColumnValue("confirmation_effect_outbox", "status", "eventId", event.id)
        )
        assertEquals(1, confirmationSyncRows(db, event.id).size)
        assertEquals(1, durableRowCount("confirmationReceipt", "eventId", event.id))
        assertEquals(operationId, db.confirmationReceiptQueries.selectByEventId(event.id).executeAsOne().operationId)
    }

    @Test
    fun `confirmation without a vote returns typed no-votes failure without writes`() = runBlocking {
        val event = pollingEvent("event-no-votes")
        assertTrue(repository.createEvent(event).isSuccess)

        val result = repository.confirmPollDate(
            confirmationCommand(event, operationId = "operation-no-votes")
        )

        val failed = assertIs<EventManagementContract.ConfirmationResult.Failed>(result)
        assertEquals(EventManagementContract.ConfirmationFailureCode.NO_VOTES, failed.failure.code)
        assertEquals(EventStatus.POLLING, repository.getEvent(event.id)?.status)
        assertNull(db.confirmedDateQueries.selectByEventId(event.id).executeAsOneOrNull())
        assertEquals(0, durableRowCount("confirmation_effect_outbox", "eventId", event.id))
        assertEquals(0, confirmationSyncRows(db, event.id).size)
        assertNull(db.confirmationReceiptQueries.selectByEventId(event.id).executeAsOneOrNull())
    }

    @Test
    fun `every local confirmation write boundary rolls back every persisted confirmation record`() = runBlocking {
        confirmationWriteBoundaryTriggers().forEach { boundary ->
            val failureUrl = "jdbc:sqlite:${Files.createTempFile("wakeve-confirmation-rollback-", ".db")}"
            val failureDriver = JdbcSqliteDriver(failureUrl)
            WakeveDb.Schema.create(failureDriver)
            val failureDb = WakeveDb(failureDriver)
            val failureRepository = DatabaseEventRepository(failureDb)
            val event = pollingEvent("event-rollback-${boundary.name}")
            assertTrue(failureRepository.createEvent(event).isSuccess)
            persistVote(failureDb, event)
            failureDriver.execute(null, boundary.triggerSql, 0)

            val result = failureRepository.confirmPollDate(
                confirmationCommand(event, "operation-rollback-${boundary.name}")
            )

            assertIs<EventManagementContract.ConfirmationResult.Failed>(result)
            assertEquals(
                EventStatus.POLLING.name,
                failureDb.eventQueries.selectById(event.id).executeAsOne().status,
                "${boundary.name} must roll back the event transition"
            )
            assertNull(
                failureDb.confirmedDateQueries.selectByEventId(event.id).executeAsOneOrNull(),
                "${boundary.name} must roll back the selected date"
            )
            assertEquals(
                0,
                durableRowCount(failureUrl, "confirmation_effect_outbox", "eventId", event.id),
                "${boundary.name} must roll back the durable envelope"
            )
            assertTrue(
                confirmationSyncRows(failureDb, event.id).isEmpty(),
                "${boundary.name} must roll back confirmation sync metadata"
            )
            assertNull(
                failureDb.confirmationReceiptQueries.selectByEventId(event.id).executeAsOneOrNull(),
                "${boundary.name} must roll back the receipt"
            )
        }
    }

    @Test
    fun `same operation and same slot replay returns one durable outcome`() = runBlocking {
        val operationId = "operation-idempotent-1"
        val event = pollingEvent("event-idempotent")
        assertTrue(repository.createEvent(event).isSuccess)
        persistVote(db, event)

        val committed = repository.confirmPollDate(confirmationCommand(event, operationId))
        val replay = repository.confirmPollDate(confirmationCommand(event, operationId))

        val committedResult = assertIs<EventManagementContract.ConfirmationResult.Committed>(committed)
        val replayResult = assertIs<EventManagementContract.ConfirmationResult.AlreadyCommitted>(replay)
        assertEquals(committedResult.receipt, replayResult.receipt)
        assertEquals(1, db.confirmedDateQueries.selectAll().executeAsList().count { it.eventId == event.id })
        assertEquals(1, confirmationSyncRows(db, event.id).size)
        assertEquals(1, durableRowCount("confirmation_effect_outbox", "eventId", event.id))
        assertEquals(1, durableRowCount("confirmationReceipt", "operationId", operationId))
    }

    @Test
    fun `different slot after confirmation is a typed conflict without mutation`() = runBlocking {
        val event = pollingEvent("event-conflict")
        assertTrue(repository.createEvent(event).isSuccess)
        persistVote(db, event)
        assertIs<EventManagementContract.ConfirmationResult.Committed>(
            repository.confirmPollDate(confirmationCommand(event, "operation-conflict-first"))
        )
        val originalStatus = repository.getEvent(event.id)?.status
        val originalSyncCount = confirmationSyncRows(db, event.id).size
        val originalOutboxCount = durableRowCount("confirmation_effect_outbox", "eventId", event.id)

        val conflict = repository.confirmPollDate(
            confirmationCommand(event, "operation-conflict-second", slotId = "slot-other")
        )

        val typedConflict = assertIs<EventManagementContract.ConfirmationResult.Conflict>(conflict)
        assertEquals(
            EventManagementContract.ConfirmationFailureCode.ALREADY_CONFIRMED_DIFFERENT_SLOT,
            typedConflict.failure.code
        )
        assertEquals("slot-selected", db.confirmedDateQueries.selectByEventId(event.id).executeAsOne().timeslotId)
        assertEquals(originalStatus, repository.getEvent(event.id)?.status)
        assertEquals(originalSyncCount, confirmationSyncRows(db, event.id).size)
        assertEquals(originalOutboxCount, durableRowCount("confirmation_effect_outbox", "eventId", event.id))
    }

    @Test
    fun `concurrent duplicate confirmation converges on one database receipt`() = runBlocking {
        val event = pollingEvent("event-concurrent")
        assertTrue(repository.createEvent(event).isSuccess)
        persistVote(db, event)

        val results = coroutineScope {
            val ready = Channel<Unit>(capacity = 2)
            val release = CompletableDeferred<Unit>()
            listOf(
                async(Dispatchers.Default) {
                    ready.send(Unit)
                    release.await()
                    repository.confirmPollDate(confirmationCommand(event, "operation-concurrent-a"))
                },
                async(Dispatchers.Default) {
                    ready.send(Unit)
                    release.await()
                    repository.confirmPollDate(confirmationCommand(event, "operation-concurrent-b"))
                }
            ).also {
                ready.receive()
                ready.receive()
                release.complete(Unit)
            }.awaitAll()
        }

        assertTrue(
            results.all {
                it is EventManagementContract.ConfirmationResult.Committed ||
                    it is EventManagementContract.ConfirmationResult.AlreadyCommitted
            },
            "both concurrent requests must converge on the receipt; outcomes=$results"
        )
        assertEquals(
            1,
            results.map {
                when (it) {
                    is EventManagementContract.ConfirmationResult.Committed -> {
                        assertEquals(
                            it.receipt.receiptId,
                            assertIs<EventManagementContract.ConfirmationProjection.Confirmed>(it.projection).receiptId
                        )
                        it.receipt.receiptId
                    }
                    is EventManagementContract.ConfirmationResult.AlreadyCommitted -> {
                        assertEquals(
                            it.receipt.receiptId,
                            assertIs<EventManagementContract.ConfirmationProjection.Confirmed>(it.projection).receiptId
                        )
                        it.receipt.receiptId
                    }
                    else -> error("Concurrent confirmations must have already been asserted as committed")
                }
            }.toSet().size,
            "concurrent confirmations must return the same durable receipt"
        )
        assertEquals(1, confirmationSyncRows(db, event.id).size)
        assertEquals(1, durableRowCount("confirmation_effect_outbox", "eventId", event.id))
        assertEquals(1, durableRowCount("confirmationReceipt", "eventId", event.id))
    }

    @Test
    fun `restart rehydrates receipt and explicit replay returns it without another envelope`() = runBlocking {
        val event = pollingEvent("event-restart")
        assertTrue(repository.createEvent(event).isSuccess)
        persistVote(db, event)
        assertIs<EventManagementContract.ConfirmationResult.Committed>(
            repository.confirmPollDate(confirmationCommand(event, "operation-restart"))
        )

        val restarted = DatabaseEventRepository(WakeveDb(JdbcSqliteDriver(jdbcUrl)))
        val replay = restarted.confirmPollDate(confirmationCommand(event, "operation-restart"))

        assertEquals(EventStatus.CONFIRMED, restarted.getEvent(event.id)?.status)
        assertEquals(1, durableRowCount("confirmationReceipt", "eventId", event.id))
        assertIs<EventManagementContract.ConfirmationResult.AlreadyCommitted>(replay)
        assertEquals(1, durableRowCount("confirmation_effect_outbox", "eventId", event.id))
        assertFalse(confirmationSyncRows(db, event.id).single().synced == 1L)
    }

    @Test
    fun `confirmation transaction persists every timestamp from its single captured instant`() = runBlocking {
        val event = pollingEvent("event-captured-clock")
        val operationId = "operation-captured-clock"
        val capturedAt = "2026-07-09T19:23:45Z"
        assertTrue(repository.createEvent(event).isSuccess)
        persistVote(db, event)
        val clockedRepository = DatabaseEventRepository(
            db,
            com.guyghost.wakeve.confirmation.ConfirmationClock {
                kotlinx.datetime.Instant.parse(capturedAt)
            }
        )

        val result = clockedRepository.confirmPollDate(confirmationCommand(event, operationId, capturedAt))

        assertIs<EventManagementContract.ConfirmationResult.Committed>(result)
        val confirmedDate = db.confirmedDateQueries.selectByEventId(event.id).executeAsOne()
        val receipt = db.confirmationReceiptQueries.selectByEventId(event.id).executeAsOne()
        val confirmationSync = confirmationSyncRows(db, event.id).single()
        assertEquals(
            listOf(capturedAt, capturedAt, capturedAt, capturedAt, capturedAt, capturedAt, capturedAt),
            listOf(
                receipt.requestedAt,
                db.eventQueries.selectById(event.id).executeAsOne().updatedAt,
                confirmedDate.confirmedAt,
                confirmedDate.updatedAt,
                receipt.committedAt,
                confirmationSync.timestamp,
                selectColumnValue("confirmation_effect_outbox", "createdAt", "eventId", event.id)
            ),
            "all confirmation timestamps must reuse one injected/captured clock instant"
        )
    }

    @Test
    fun `confirmation envelope schema has unique durable domain and effect identities`() = runBlocking {
        val event = pollingEvent("event-envelope-identities")
        assertTrue(repository.createEvent(event).isSuccess)
        persistVote(db, event)
        assertIs<EventManagementContract.ConfirmationResult.Committed>(
            repository.confirmPollDate(confirmationCommand(event, "operation-envelope-identities"))
        )

        val table = "confirmation_effect_outbox"
        assertTrue(
            tableExists(table),
            "the local confirmation envelope must have its own durable outbox table"
        )
        assertTrue(tableColumns(table).containsAll(setOf("domainEventId", "effectKey", "createdAt")))
        assertTrue(isUniquelyConstrained(table, "domainEventId"))
        assertTrue(isUniquelyConstrained(table, "effectKey"))
    }

    private fun pollingEvent(id: String) = createTestEvent(
        id = id,
        organizerId = "organizer-1",
        proposedSlots = listOf(
            createTestTimeSlot("slot-first", "2026-07-10T09:00:00Z", "2026-07-10T11:00:00Z"),
            createTestTimeSlot("slot-selected", "2026-07-11T14:00:00Z", "2026-07-11T16:00:00Z"),
            createTestTimeSlot("slot-other", "2026-07-12T14:00:00Z", "2026-07-12T16:00:00Z")
        ),
        status = EventStatus.POLLING,
        deadline = "2026-07-01T23:59:59Z"
    )

    private fun confirmationCommand(
        event: com.guyghost.wakeve.models.Event,
        operationId: String,
        requestedAt: String = "2026-07-09T19:23:45Z",
        slotId: String = "slot-selected"
    ) = EventManagementContract.ConfirmPollDateCommand(
        operationId = operationId,
        eventId = event.id,
        slotId = slotId,
        actorId = event.organizerId,
        requestedAt = kotlinx.datetime.Instant.parse(requestedAt)
    )

    private fun persistVote(database: WakeveDb, event: com.guyghost.wakeve.models.Event, slotId: String = "slot-selected") {
        database.voteQueries.insertVote(
            id = "vote-${event.id}-$slotId",
            eventId = event.id,
            timeslotId = slotId,
            participantId = "org_${event.id}",
            vote = "YES",
            createdAt = "2026-07-09T19:23:45Z",
            updatedAt = "2026-07-09T19:23:45Z"
        )
    }

    private fun durableRowCount(table: String, keyColumn: String, key: String): Int =
        durableRowCount(jdbcUrl, table, keyColumn, key)

    private fun durableRowCount(url: String, table: String, keyColumn: String, key: String): Int =
        DriverManager.getConnection(url).use { connection ->
            val exists = connection.prepareStatement(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?"
            ).use { statement ->
                statement.setString(1, table)
                statement.executeQuery().use { rows -> rows.next(); rows.getInt(1) == 1 }
            }
            if (!exists) return@use 0
            connection.prepareStatement("SELECT COUNT(*) FROM $table WHERE $keyColumn = ?").use { statement ->
                statement.setString(1, key)
                statement.executeQuery().use { rows -> rows.next(); rows.getInt(1) }
            }
        }

    private fun selectColumnValue(table: String, column: String, keyColumn: String, key: String): String =
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.prepareStatement("SELECT $column FROM $table WHERE $keyColumn = ?").use { statement ->
                statement.setString(1, key)
                statement.executeQuery().use { rows ->
                    check(rows.next())
                    rows.getString(1)
                }
            }
        }

    private fun confirmationSyncRows(database: WakeveDb, eventId: String) =
        database.syncMetadataQueries.selectByEntity("event", eventId).executeAsList()
            .filter { it.id.startsWith("sync_confirm_") }

    private fun tableExists(table: String): Boolean =
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.prepareStatement(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?"
            ).use { statement ->
                statement.setString(1, table)
                statement.executeQuery().use { rows -> rows.next(); rows.getInt(1) == 1 }
            }
        }

    private fun tableColumns(table: String): Set<String> =
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA table_info($table)").use { rows ->
                    buildSet {
                        while (rows.next()) add(rows.getString("name"))
                    }
                }
            }
        }

    private fun isUniquelyConstrained(table: String, column: String): Boolean =
        DriverManager.getConnection(jdbcUrl).use { connection ->
            val primaryKey = connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA table_info($table)").use { rows ->
                    generateSequence { if (rows.next()) rows else null }
                        .any { it.getString("name") == column && it.getInt("pk") > 0 }
                }
            }
            val hasSingleColumnUniqueIndex = connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA index_list($table)").use { indexes ->
                    var unique = false
                    while (indexes.next()) {
                        if (indexes.getInt("unique") == 1) {
                            val indexName = indexes.getString("name")
                            val indexColumns = connection.createStatement().use { indexStatement ->
                                indexStatement.executeQuery("PRAGMA index_info($indexName)").use { indexRows ->
                                    buildSet {
                                        while (indexRows.next()) add(indexRows.getString("name"))
                                    }
                                }
                            }
                            if (indexColumns == setOf(column)) unique = true
                        }
                    }
                    unique
                }
            }
            primaryKey || hasSingleColumnUniqueIndex
        }

    private fun confirmationWriteBoundaryTriggers() = listOf(
        ConfirmationWriteBoundary(
            "receipt",
            "CREATE TRIGGER fail_confirmation_receipt BEFORE INSERT ON confirmationReceipt " +
                "BEGIN SELECT RAISE(FAIL, 'injected receipt failure'); END"
        ),
        ConfirmationWriteBoundary(
            "event",
            "CREATE TRIGGER fail_confirmation_event BEFORE UPDATE OF status ON event " +
                "WHEN NEW.status = 'CONFIRMED' BEGIN SELECT RAISE(FAIL, 'injected event failure'); END"
        ),
        ConfirmationWriteBoundary(
            "confirmed-date",
            "CREATE TRIGGER fail_confirmation_date BEFORE INSERT ON confirmedDate " +
                "BEGIN SELECT RAISE(FAIL, 'injected confirmed-date failure'); END"
        ),
        ConfirmationWriteBoundary(
            "outbox",
            "CREATE TRIGGER fail_confirmation_outbox BEFORE INSERT ON confirmation_effect_outbox " +
                "BEGIN SELECT RAISE(FAIL, 'injected outbox failure'); END"
        ),
        ConfirmationWriteBoundary(
            "sync",
            "CREATE TRIGGER fail_confirmation_sync BEFORE INSERT ON syncMetadata " +
                "BEGIN SELECT RAISE(FAIL, 'injected sync failure'); END"
        )
    )

    private data class ConfirmationWriteBoundary(val name: String, val triggerSql: String)
}
