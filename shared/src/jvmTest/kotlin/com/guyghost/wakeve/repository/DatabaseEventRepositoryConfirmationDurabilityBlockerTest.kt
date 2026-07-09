package com.guyghost.wakeve.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DatabaseEventRepositoryConfirmationDurabilityBlockerTest {

    @Test
    fun `schema version 6 upgrade provisions confirmation persistence before confirming`() = runBlocking {
        val url = temporaryJdbcUrl("v6-to-current")
        val driver = JdbcSqliteDriver(url)
        WakeveDb.Schema.create(driver)

        // A device already at schema version 5 has no tables introduced by this change.
        driver.execute(null, "DROP TABLE confirmation_effect_outbox", 0)
        driver.execute(null, "DROP TABLE confirmationReceipt", 0)
        driver.execute(null, "DROP TABLE workflowOutbox", 0)
        driver.execute(null, "DROP TABLE confirmationLegacyClassification", 0)

        WakeveDb.Schema.migrate(driver, oldVersion = 6, newVersion = 8)

        assertTrue(tableExists(url, "confirmationReceipt"))
        assertTrue(tableExists(url, "workflowOutbox"))
        assertTrue(tableExists(url, "confirmation_effect_outbox"))

        val migratedDatabase = WakeveDb(driver)
        val migratedRepository = DatabaseEventRepository(migratedDatabase)
        val event = pollingEvent("event-migrated-confirmation")
        persistEventAndVote(migratedRepository, migratedDatabase, event)

        assertIs<EventManagementContract.ConfirmationResult.Committed>(
            migratedRepository.confirmPollDate(command(event, "operation-migrated-confirmation"))
        )
        Unit
    }

    @Test
    fun `independent repository instances racing on one file converge on one durable receipt`() = runBlocking {
        val url = temporaryJdbcUrl("independent-repositories")
        val initialDriver = JdbcSqliteDriver(url)
        WakeveDb.Schema.create(initialDriver)
        initialDriver.execute(null, "PRAGMA busy_timeout = 5000", 0)

        val firstDriver = JdbcSqliteDriver(url)
        val secondDriver = JdbcSqliteDriver(url)
        firstDriver.execute(null, "PRAGMA busy_timeout = 5000", 0)
        secondDriver.execute(null, "PRAGMA busy_timeout = 5000", 0)

        val firstDatabase = WakeveDb(firstDriver)
        val secondDatabase = WakeveDb(secondDriver)
        val firstRepository = DatabaseEventRepository(firstDatabase)
        val secondRepository = DatabaseEventRepository(secondDatabase)
        val event = pollingEvent("event-independent-repositories")
        persistEventAndVote(firstRepository, firstDatabase, event)

        val results = coroutineScope {
            val ready = CompletableDeferred<Unit>()
            val secondReady = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            listOf(
                async(Dispatchers.Default) {
                    ready.complete(Unit)
                    release.await()
                    firstRepository.confirmPollDate(command(event, "operation-independent-first"))
                },
                async(Dispatchers.Default) {
                    secondReady.complete(Unit)
                    release.await()
                    secondRepository.confirmPollDate(command(event, "operation-independent-second"))
                }
            ).also {
                ready.await()
                secondReady.await()
                release.complete(Unit)
            }.awaitAll()
        }

        assertTrue(
            results.all {
                it is EventManagementContract.ConfirmationResult.Committed ||
                    it is EventManagementContract.ConfirmationResult.AlreadyCommitted
            },
            "independent repository instances must converge through durable constraints: $results"
        )
        val receiptIds = results.map {
            when (it) {
                is EventManagementContract.ConfirmationResult.Committed -> it.receipt.receiptId
                is EventManagementContract.ConfirmationResult.AlreadyCommitted -> it.receipt.receiptId
                else -> error("outcome was already asserted as committed")
            }
        }
        assertEquals(1, receiptIds.toSet().size)
        assertEquals(1, rowCount(url, "confirmationReceipt", "eventId", event.id))
        assertEquals(1, rowCount(url, "confirmation_effect_outbox", "eventId", event.id))
    }

    @Test
    fun `typed confirmation command captures time exclusively from the injected repository clock`() = runBlocking {
        val url = temporaryJdbcUrl("injected-clock")
        val driver = JdbcSqliteDriver(url)
        WakeveDb.Schema.create(driver)
        val database = WakeveDb(driver)
        val injectedNow = Instant.parse("2031-05-06T07:08:09Z")
        val repository = DatabaseEventRepository(
            database,
            confirmationClock = ConfirmationClock { injectedNow }
        )
        val event = pollingEvent("event-injected-clock")
        persistEventAndVote(repository, database, event)

        assertIs<EventManagementContract.ConfirmationResult.Committed>(
            repository.confirmPollDate(
                command(
                    event = event,
                    operationId = "operation-injected-clock",
                    requestedAt = "2028-01-02T03:04:05Z"
                )
            )
        )

        val expected = injectedNow.toString()
        assertEquals(expected, database.confirmationReceiptQueries.selectByEventId(event.id).executeAsOne().requestedAt)
        assertEquals(expected, database.confirmationReceiptQueries.selectByEventId(event.id).executeAsOne().committedAt)
        assertEquals(expected, database.confirmedDateQueries.selectByEventId(event.id).executeAsOne().confirmedAt)
        assertEquals(expected, database.syncMetadataQueries.selectByEntity("event", event.id).executeAsList()
            .single { it.id == "sync_confirm_${event.id}" }.timestamp)
        assertEquals(expected, columnValue(url, "confirmation_effect_outbox", "createdAt", "eventId", event.id))
    }

    @Test
    fun `restart reloads durable decision and effect statuses into the typed confirmation projection`() = runBlocking {
        val url = temporaryJdbcUrl("status-reload")
        val driver = JdbcSqliteDriver(url)
        WakeveDb.Schema.create(driver)
        val database = WakeveDb(driver)
        val repository = DatabaseEventRepository(database)
        val event = pollingEvent("event-status-reload")
        persistEventAndVote(repository, database, event)
        val command = command(event, "operation-status-reload")
        assertIs<EventManagementContract.ConfirmationResult.Committed>(repository.confirmPollDate(command))

        val syncedProjection = assertIs<EventManagementContract.ConfirmationProjection.Confirmed>(
            repository.markConfirmationSynced(command.operationId)
        )
        assertEquals(EventManagementContract.DecisionSyncStatus.SERVER_ACKNOWLEDGED, syncedProjection.decisionSyncStatus)
        driver.execute(
            null,
            "UPDATE confirmation_effect_outbox SET status = 'PARTIALLY_PROCESSED' WHERE eventId = '${event.id}'",
            0
        )

        val restarted = DatabaseEventRepository(WakeveDb(JdbcSqliteDriver(url)))
        val result = assertIs<EventManagementContract.ConfirmationResult.AlreadyCommitted>(
            restarted.confirmPollDate(command)
        )
        val projection = assertIs<EventManagementContract.ConfirmationProjection.Confirmed>(result.projection)

        assertEquals(EventManagementContract.DecisionSyncStatus.SERVER_ACKNOWLEDGED, projection.decisionSyncStatus)
        assertEquals(EventManagementContract.EffectDispatchStatus.PARTIALLY_PROCESSED, projection.effectDispatchStatus)
        assertEquals(EventManagementContract.DecisionSyncStatus.SERVER_ACKNOWLEDGED, result.receipt.decisionSyncStatus)
        assertEquals(EventManagementContract.EffectDispatchStatus.PARTIALLY_PROCESSED, result.receipt.effectDispatchStatus)
    }

    @Test
    fun `default repository confirmation refuses to fabricate a durable commit`() = runBlocking {
        val repository = NonDurableConfirmationRepository()
        val event = pollingEvent("event-no-durable-confirmation")

        val result = repository.confirmPollDate(command(event, "operation-no-durable-confirmation"))

        val failure = assertIs<EventManagementContract.ConfirmationResult.Failed>(result)
        assertEquals(EventManagementContract.ConfirmationFailureCode.REPOSITORY_UNAVAILABLE, failure.failure.code)
        assertTrue(failure.failure.retryable)
    }

    @Test
    fun `cold restart loads confirmed and reviewing projections without issuing confirmation`() = runBlocking {
        val url = temporaryJdbcUrl("projection-reload")
        val driver = JdbcSqliteDriver(url)
        WakeveDb.Schema.create(driver)
        val database = WakeveDb(driver)
        val repository = DatabaseEventRepository(database)
        val confirmedEvent = pollingEvent("event-projection-confirmed")
        val reviewingEvent = pollingEvent("event-projection-reviewing").copy(
            proposedSlots = listOf(
                createTestTimeSlot(
                    id = "slot-projection-reviewing",
                    start = "2026-07-12T14:00:00Z",
                    end = "2026-07-12T16:00:00Z"
                )
            )
        )
        persistEventAndVote(repository, database, confirmedEvent)
        assertTrue(repository.createEvent(reviewingEvent).isSuccess)
        assertIs<EventManagementContract.ConfirmationResult.Committed>(
            repository.confirmPollDate(command(confirmedEvent, "operation-projection-confirmed"))
        )
        val receiptCountBeforeRestart = rowCount(url, "confirmationReceipt", "eventId", confirmedEvent.id)
        val outboxCountBeforeRestart = rowCount(url, "confirmation_effect_outbox", "eventId", confirmedEvent.id)

        val restarted = DatabaseEventRepository(WakeveDb(JdbcSqliteDriver(url)))
        val confirmed = assertIs<EventManagementContract.ConfirmationProjection.Confirmed>(
            loadConfirmationProjection(restarted, confirmedEvent.id)
        )
        val reviewing = assertIs<EventManagementContract.ConfirmationProjection.Reviewing>(
            loadConfirmationProjection(restarted, reviewingEvent.id)
        )

        assertEquals(selectedSlotId, confirmed.slotId)
        assertEquals("operation-projection-confirmed", confirmed.receiptId)
        assertEquals(reviewingEvent.id, reviewing.eventId)
        assertEquals(receiptCountBeforeRestart, rowCount(url, "confirmationReceipt", "eventId", confirmedEvent.id))
        assertEquals(outboxCountBeforeRestart, rowCount(url, "confirmation_effect_outbox", "eventId", confirmedEvent.id))
    }

    @Test
    fun `v5 migration classifies only an unambiguous historical confirmation and never backfills its envelope`() = runBlocking {
        val url = temporaryJdbcUrl("legacy-classification")
        val driver = JdbcSqliteDriver(url)
        WakeveDb.Schema.create(driver)
        val database = WakeveDb(driver)
        val repository = DatabaseEventRepository(database)
        val unambiguousEvent = pollingEvent("event-legacy-unambiguous").copy(status = EventStatus.CONFIRMED)
        val ambiguousEvent = pollingEvent("event-legacy-ambiguous").copy(
            status = EventStatus.CONFIRMED,
            proposedSlots = listOf(
                createTestTimeSlot(
                    id = "slot-legacy-ambiguous",
                    start = "2026-07-12T14:00:00Z",
                    end = "2026-07-12T16:00:00Z"
                )
            )
        )
        assertTrue(repository.createEvent(unambiguousEvent).isSuccess)
        assertTrue(repository.createEvent(ambiguousEvent).isSuccess)
        database.confirmedDateQueries.insertConfirmedDate(
            id = "confirmed_${unambiguousEvent.id}",
            eventId = unambiguousEvent.id,
            timeslotId = selectedSlotId,
            confirmedByOrganizerId = organizerId,
            confirmedAt = "2025-12-01T10:00:00Z",
            updatedAt = "2025-12-01T10:00:00Z"
        )

        // Reproduce an installed v5 database, before confirmation persistence existed.
        driver.execute(null, "DROP TABLE confirmation_effect_outbox", 0)
        driver.execute(null, "DROP TABLE confirmationReceipt", 0)
        driver.execute(null, "DROP TABLE workflowOutbox", 0)
        driver.execute(null, "DROP TABLE confirmationLegacyClassification", 0)

        WakeveDb.Schema.migrate(driver, oldVersion = 5, newVersion = 8)

        assertTrue(tableExists(url, "confirmationLegacyClassification"))
        assertEquals(
            "legacyApplied",
            columnValue(url, "confirmationLegacyClassification", "classification", "eventId", unambiguousEvent.id)
        )
        assertEquals(
            "quarantined",
            columnValue(url, "confirmationLegacyClassification", "classification", "eventId", ambiguousEvent.id)
        )
        assertEquals(1, rowCount(url, "confirmationReceipt", "eventId", unambiguousEvent.id))
        assertEquals(0, rowCount(url, "confirmationReceipt", "eventId", ambiguousEvent.id))
        assertEquals(0, rowCount(url, "confirmation_effect_outbox", "eventId", unambiguousEvent.id))
        assertEquals(0, rowCount(url, "confirmation_effect_outbox", "eventId", ambiguousEvent.id))
        assertTrue(
            database.syncMetadataQueries.selectByEntity("event", unambiguousEvent.id)
                .executeAsList().none { it.id.startsWith("sync_confirm_") },
            "legacyApplied must not fabricate a decision-sync record"
        )

        val restarted = DatabaseEventRepository(WakeveDb(JdbcSqliteDriver(url)))
        assertEquals(
            "LegacyApplied",
            restarted.loadConfirmationProjection(unambiguousEvent.id)::class.simpleName,
            "legacyApplied must not be rendered as a pending modern confirmation"
        )
        assertEquals(
            "Quarantined",
            restarted.loadConfirmationProjection(ambiguousEvent.id)::class.simpleName,
            "ambiguous historical data must remain diagnostics/read-only"
        )

        val replayOutcomes = listOf(
            restarted.confirmPollDate(command(unambiguousEvent, "operation-legacy-applied-replay")),
            restarted.confirmPollDate(
                command(ambiguousEvent, "operation-legacy-quarantined-replay").copy(
                    slotId = "slot-legacy-ambiguous"
                )
            )
        )

        assertEquals(0, rowCount(url, "confirmation_effect_outbox", "eventId", unambiguousEvent.id))
        assertEquals(0, rowCount(url, "confirmation_effect_outbox", "eventId", ambiguousEvent.id))
        assertTrue(
            database.syncMetadataQueries.selectByEntity("event", unambiguousEvent.id)
                .executeAsList().none { it.id.startsWith("sync_confirm_") }
        )
        assertTrue(
            database.syncMetadataQueries.selectByEntity("event", ambiguousEvent.id)
                .executeAsList().none { it.id.startsWith("sync_confirm_") }
        )
        assertEquals("LegacyApplied", restarted.loadConfirmationProjection(unambiguousEvent.id)::class.simpleName)
        assertEquals("Quarantined", restarted.loadConfirmationProjection(ambiguousEvent.id)::class.simpleName)
        assertEquals(
            listOf("ReadOnly", "ReadOnly"),
            replayOutcomes.map { it::class.simpleName },
            "legacy replay must preserve a typed read-only outcome instead of a live pending confirmation"
        )
    }

    @Test
    fun `legacy confirmation entrypoint captures one repository instant and ignores caller timestamp`() = runBlocking {
        val url = temporaryJdbcUrl("legacy-temporal-authority")
        val driver = JdbcSqliteDriver(url)
        WakeveDb.Schema.create(driver)
        val database = WakeveDb(driver)
        var clockReads = 0
        val repository = DatabaseEventRepository(
            database,
            confirmationClock = ConfirmationClock {
                clockReads += 1
                Instant.parse("2035-02-03T04:05:06Z")
            }
        )
        val event = pollingEvent("event-legacy-temporal-authority")
        persistEventAndVote(repository, database, event)

        assertTrue(
            repository.confirmEventDateCommand(
                eventId = event.id,
                slotId = selectedSlotId,
                confirmedByOrganizerId = organizerId,
                operationId = "operation-legacy-temporal-authority",
                requestedAt = "1999-01-01T00:00:00Z"
            ).isSuccess
        )

        assertEquals(1, clockReads, "one command must capture exactly one repository-owned instant")
        assertEquals(
            "2035-02-03T04:05:06Z",
            database.confirmationReceiptQueries.selectByEventId(event.id).executeAsOne().requestedAt
        )
    }

    @Test
    fun `legacy confirmation convenience command captures the repository clock once`() = runBlocking {
        val url = temporaryJdbcUrl("legacy-convenience-temporal-authority")
        val driver = JdbcSqliteDriver(url)
        WakeveDb.Schema.create(driver)
        val database = WakeveDb(driver)
        var clockReads = 0
        val repository = DatabaseEventRepository(
            database,
            confirmationClock = ConfirmationClock {
                clockReads += 1
                Instant.parse("2035-03-04T05:06:07Z")
            }
        )
        val event = pollingEvent("event-legacy-convenience-temporal-authority")
        persistEventAndVote(repository, database, event)

        assertTrue(repository.confirmEventDate(event.id, selectedSlotId, organizerId).isSuccess)

        assertEquals(1, clockReads, "the convenience API must not create a competing timestamp capture")
        assertEquals(
            "2035-03-04T05:06:07Z",
            database.confirmationReceiptQueries.selectByEventId(event.id).executeAsOne().requestedAt
        )
    }

    @Test
    fun `concurrent recovery read failure returns a typed result instead of escaping`() = runBlocking {
        val url = temporaryJdbcUrl("recovery-read-failure")
        val driver = JdbcSqliteDriver(url)
        WakeveDb.Schema.create(driver)
        val database = WakeveDb(driver)
        val repository = DatabaseEventRepository(database)
        val event = pollingEvent("event-recovery-read-failure")
        persistEventAndVote(repository, database, event)

        // Both the failed transaction and its concurrent-recovery read must remain inside
        // the command's typed failure boundary.
        driver.execute(null, "DROP TABLE confirmationReceipt", 0)
        val outcome = runCatching {
            repository.confirmPollDate(command(event, "operation-recovery-read-failure"))
        }

        assertTrue(outcome.isSuccess, "recovery read failures must not escape the typed command")
        val failure = assertIs<EventManagementContract.ConfirmationResult.Failed>(outcome.getOrThrow())
        assertEquals(EventManagementContract.ConfirmationFailureCode.LOCAL_PERSISTENCE_FAILED, failure.failure.code)
        assertTrue(failure.failure.retryable)
    }

    private suspend fun persistEventAndVote(
        repository: DatabaseEventRepository,
        database: WakeveDb,
        event: Event
    ) {
        assertTrue(repository.createEvent(event).isSuccess)
        database.voteQueries.insertVote(
            id = "vote-${event.id}",
            eventId = event.id,
            timeslotId = selectedSlotId,
            participantId = "participant-${event.id}",
            vote = "YES",
            createdAt = "2026-07-09T19:23:45Z",
            updatedAt = "2026-07-09T19:23:45Z"
        )
    }

    private fun pollingEvent(id: String): Event = createTestEvent(
        id = id,
        organizerId = organizerId,
        status = EventStatus.POLLING,
        deadline = "2026-07-01T23:59:59Z",
        proposedSlots = listOf(
            createTestTimeSlot(
                id = selectedSlotId,
                start = "2026-07-11T14:00:00Z",
                end = "2026-07-11T16:00:00Z"
            )
        )
    )

    private fun command(
        event: Event,
        operationId: String,
        requestedAt: String = "2026-07-09T19:23:45Z"
    ) = EventManagementContract.ConfirmPollDateCommand(
        operationId = operationId,
        eventId = event.id,
        slotId = selectedSlotId,
        actorId = event.organizerId,
        requestedAt = Instant.parse(requestedAt)
    )

    private fun temporaryJdbcUrl(prefix: String): String =
        "jdbc:sqlite:${Files.createTempFile("wakeve-confirmation-$prefix-", ".db")}"

    private fun tableExists(url: String, table: String): Boolean =
        DriverManager.getConnection(url).use { connection ->
            connection.prepareStatement(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?"
            ).use { statement ->
                statement.setString(1, table)
                statement.executeQuery().use { rows -> rows.next(); rows.getInt(1) == 1 }
            }
        }

    private fun rowCount(url: String, table: String, keyColumn: String, key: String): Int =
        DriverManager.getConnection(url).use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM $table WHERE $keyColumn = ?").use { statement ->
                statement.setString(1, key)
                statement.executeQuery().use { rows -> rows.next(); rows.getInt(1) }
            }
        }

    private fun columnValue(url: String, table: String, column: String, keyColumn: String, key: String): String =
        DriverManager.getConnection(url).use { connection ->
            connection.prepareStatement("SELECT $column FROM $table WHERE $keyColumn = ?").use { statement ->
                statement.setString(1, key)
                statement.executeQuery().use { rows ->
                    check(rows.next())
                    rows.getString(1)
                }
            }
        }

    private fun loadConfirmationProjection(
        repository: DatabaseEventRepository,
        eventId: String
    ): EventManagementContract.ConfirmationProjection = repository.loadConfirmationProjection(eventId)

    private class NonDurableConfirmationRepository : EventRepositoryInterface by EventRepository() {
        override suspend fun confirmEventDateCommand(
            eventId: String,
            slotId: String,
            confirmedByOrganizerId: String,
            operationId: String,
            requestedAt: String
        ): Result<Boolean> = Result.success(true)

        override suspend fun confirmPollDate(
            command: EventManagementContract.ConfirmPollDateCommand
        ): EventManagementContract.ConfirmationResult = super<EventRepositoryInterface>.confirmPollDate(command)
    }

    private companion object {
        const val organizerId = "organizer-1"
        const val selectedSlotId = "slot-selected"
    }
}
