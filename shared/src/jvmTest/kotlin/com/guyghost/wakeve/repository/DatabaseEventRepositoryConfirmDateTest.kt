package com.guyghost.wakeve.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        assertTrue(repository.createEvent(event).isSuccess)

        val result = repository.confirmEventDate(event.id, "slot-selected", event.organizerId)

        assertTrue(result.isSuccess)
        assertEquals(EventStatus.CONFIRMED, repository.getEvent(event.id)?.status)
        assertEquals("slot-selected", db.confirmedDateQueries.selectByEventId(event.id).executeAsOne().timeslotId)
        assertEquals(1, repository.getWorkflowOutbox(event.id).size, "one durable confirmation_effect_outbox envelope")
        assertEquals(1, db.syncMetadataQueries.selectByEntity("event", event.id).executeAsList().size)
        assertEquals(1, durableRowCount("confirmationReceipt", "eventId", event.id))
    }

    @Test
    fun `failure at sync boundary rolls back event and confirmed date`() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WakeveDb.Schema.create(driver)
        val failingDb = WakeveDb(driver)
        val failingRepository = DatabaseEventRepository(failingDb)
        val event = pollingEvent("event-rollback-sync")
        assertTrue(failingRepository.createEvent(event).isSuccess)
        driver.execute(
            null,
            "CREATE TRIGGER fail_confirmation_sync BEFORE INSERT ON syncMetadata BEGIN SELECT RAISE(FAIL, 'injected sync failure'); END",
            0
        )

        val result = failingRepository.confirmEventDate(event.id, "slot-selected", event.organizerId)

        assertTrue(result.isFailure)
        assertEquals(EventStatus.POLLING.name, failingDb.eventQueries.selectById(event.id).executeAsOne().status)
        assertNull(failingDb.confirmedDateQueries.selectByEventId(event.id).executeAsOneOrNull())
        assertTrue(failingRepository.getWorkflowOutbox(event.id).isEmpty())
    }

    @Test
    fun `same operation and same slot replay returns one durable outcome`() = runBlocking {
        val operationId = "operation-idempotent-1"
        val event = pollingEvent("event-idempotent")
        assertTrue(repository.createEvent(event).isSuccess)

        assertTrue(repository.confirmEventDate(event.id, "slot-selected", event.organizerId).isSuccess)
        assertTrue(repository.confirmEventDate(event.id, "slot-selected", event.organizerId).isSuccess)

        assertEquals(1, db.confirmedDateQueries.selectAll().executeAsList().count { it.eventId == event.id })
        assertEquals(1, db.syncMetadataQueries.selectByEntity("event", event.id).executeAsList().size)
        assertEquals(1, repository.getWorkflowOutbox(event.id).size)
        assertEquals(1, durableRowCount("confirmationReceipt", "operationId", operationId))
    }

    @Test
    fun `different slot after confirmation is a typed conflict without mutation`() = runBlocking {
        val event = pollingEvent("event-conflict")
        assertTrue(repository.createEvent(event).isSuccess)
        assertTrue(repository.confirmEventDate(event.id, "slot-selected", event.organizerId).isSuccess)
        val originalStatus = repository.getEvent(event.id)?.status
        val originalSyncCount = db.syncMetadataQueries.selectByEntity("event", event.id).executeAsList().size
        val originalOutboxCount = repository.getWorkflowOutbox(event.id).size

        val conflict = repository.confirmEventDate(event.id, "slot-other", event.organizerId)

        assertTrue(conflict.isFailure)
        assertEquals("ALREADY_CONFIRMED_DIFFERENT_SLOT", conflict.exceptionOrNull()?.message)
        assertEquals("slot-selected", db.confirmedDateQueries.selectByEventId(event.id).executeAsOne().timeslotId)
        assertEquals(originalStatus, repository.getEvent(event.id)?.status)
        assertEquals(originalSyncCount, db.syncMetadataQueries.selectByEntity("event", event.id).executeAsList().size)
        assertEquals(originalOutboxCount, repository.getWorkflowOutbox(event.id).size)
    }

    @Test
    fun `concurrent duplicate confirmation converges on one database receipt`() = runBlocking {
        val event = pollingEvent("event-concurrent")
        assertTrue(repository.createEvent(event).isSuccess)

        val results = listOf(
            async { repository.confirmEventDate(event.id, "slot-selected", event.organizerId) },
            async { repository.confirmEventDate(event.id, "slot-selected", event.organizerId) }
        ).awaitAll()

        assertTrue(results.all { it.isSuccess })
        assertEquals(1, db.syncMetadataQueries.selectByEntity("event", event.id).executeAsList().size)
        assertEquals(1, repository.getWorkflowOutbox(event.id).size)
    }

    @Test
    fun `restart rehydrates receipt and replays one durable envelope`() = runBlocking {
        val event = pollingEvent("event-restart")
        assertTrue(repository.createEvent(event).isSuccess)
        assertTrue(repository.confirmEventDate(event.id, "slot-selected", event.organizerId).isSuccess)

        val restarted = DatabaseEventRepository(db)

        assertEquals(EventStatus.CONFIRMED, restarted.getEvent(event.id)?.status)
        assertEquals(1, durableRowCount("confirmationReceipt", "eventId", event.id))
        assertEquals(1, restarted.getWorkflowOutbox(event.id).size)
        assertFalse(db.syncMetadataQueries.selectByEntity("event", event.id).executeAsList().single().synced == 1L)
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

    private fun durableRowCount(table: String, keyColumn: String, key: String): Int =
        DriverManager.getConnection(jdbcUrl).use { connection ->
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
}
