package com.guyghost.wakeve.invitationexperience

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CreationStudioSyncOwnerRedTest {

    @Test
    fun `observe requires exact receipt and sync subject before pending or completed`() = runTest {
        val fixture = fixture()
        fixture.seedPendingStudioOperation()
        val owner = DatabaseCreationStudioSyncOwner(fixture.database)
        val binding = binding()

        assertEquals(CreationStudioSyncResult.Pending(binding), owner.observe(binding))

        fixture.execute("UPDATE syncMetadata SET synced = 1 WHERE id = 'studio:operation-1'")
        fixture.execute(
            "UPDATE event_operation_receipt SET status = 'COMMITTED' " +
                "WHERE operation_id = 'operation-1' AND event_id = 'event-1'"
        )

        assertEquals(CreationStudioSyncResult.Completed(binding), owner.observe(binding))
    }

    @Test
    fun `late or forged binding fails closed without mutating the current operation`() = runTest {
        val fixture = fixture()
        fixture.seedPendingStudioOperation()
        val owner = DatabaseCreationStudioSyncOwner(fixture.database)
        val mismatches = listOf(
            binding().copy(eventId = "other-event"),
            binding().copy(aggregateRevision = 4),
            binding().copy(operationId = "other-operation")
        )

        mismatches.forEach { mismatch ->
            val failed = assertIs<CreationStudioSyncResult.Failed>(owner.observe(mismatch))
            assertEquals(mismatch, failed.binding)
        }
        assertEquals(0L, fixture.number("SELECT synced FROM syncMetadata WHERE id = 'studio:operation-1'"))
        assertEquals("READY", fixture.text("SELECT retryState FROM syncMetadata WHERE id = 'studio:operation-1'"))
        assertEquals(5L, fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
    }

    @Test
    fun `post commit retry requeues only exact sync subject without rewriting aggregate`() = runTest {
        val fixture = fixture()
        fixture.seedPendingStudioOperation()
        fixture.execute(
            "UPDATE syncMetadata SET retryState = 'FAILED', retryCount = 2, synced = 0 " +
                "WHERE id = 'studio:operation-1'"
        )
        val owner = DatabaseCreationStudioSyncOwner(fixture.database)
        val binding = binding()

        assertIs<CreationStudioSyncResult.Failed>(owner.observe(binding))
        assertEquals(CreationStudioSyncResult.Pending(binding), owner.retry(binding))

        assertEquals("READY", fixture.text("SELECT retryState FROM syncMetadata WHERE id = 'studio:operation-1'"))
        assertEquals(3L, fixture.number("SELECT retryCount FROM syncMetadata WHERE id = 'studio:operation-1'"))
        assertEquals(0L, fixture.number("SELECT synced FROM syncMetadata WHERE id = 'studio:operation-1'"))
        assertEquals(5L, fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
        assertEquals("Original title", fixture.text("SELECT title FROM event WHERE id = 'event-1'"))
        assertEquals(
            1L,
            fixture.number(
                "SELECT COUNT(*) FROM event_operation_receipt " +
                    "WHERE operation_id = 'operation-1' AND aggregate_revision = 5"
            ),
            "A post-commit retry must never execute UpdateDraftAggregate again or create a second receipt."
        )
    }

    @Test
    fun `permanent sync failure cannot be retried into a loop`() = runTest {
        val fixture = fixture()
        fixture.seedPendingStudioOperation()
        fixture.execute(
            "UPDATE syncMetadata SET retryState = 'PERMANENT_FAILURE', retryCount = 4, synced = 0 " +
                "WHERE id = 'studio:operation-1'"
        )
        val owner = DatabaseCreationStudioSyncOwner(fixture.database)
        val binding = binding()

        val observed = assertIs<CreationStudioSyncResult.Failed>(owner.observe(binding))
        assertEquals(InvitationExperienceError.PERMANENT_FAILURE, observed.error)
        val retried = assertIs<CreationStudioSyncResult.Failed>(owner.retry(binding))
        assertEquals(InvitationExperienceError.PERMANENT_FAILURE, retried.error)
        assertEquals("PERMANENT_FAILURE", fixture.text("SELECT retryState FROM syncMetadata WHERE id = 'studio:operation-1'"))
        assertEquals(4L, fixture.number("SELECT retryCount FROM syncMetadata WHERE id = 'studio:operation-1'"))
    }

    private fun binding() = CreationStudioSyncBinding(
        eventId = "event-1",
        aggregateRevision = 5,
        operationId = "operation-1"
    )

    private fun fixture(): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
        WakeveDb.Schema.create(driver)
        return Fixture(driver, WakeveDb(driver))
    }

    private class Fixture(
        private val driver: SqlDriver,
        val database: WakeveDb
    ) {
        fun seedPendingStudioOperation() {
            execute(
                """INSERT INTO event(
                    id, organizerId, title, description, status, deadline, createdAt, updatedAt,
                    aggregateRevision, aggregateSchemaVersion
                ) VALUES (
                    'event-1', 'organizer-1', 'Original title', 'Description', 'DRAFT',
                    '2100-01-10T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z',
                    5, 1
                )"""
            )
            execute(
                "INSERT INTO event_artwork(event_id, kind, updated_at) " +
                    "VALUES ('event-1', 'NONE', '2030-01-01T00:00:00Z')"
            )
            execute(
                "INSERT INTO event_operation_receipt(" +
                    "operation_id, event_id, actor_id, action, aggregate_revision, " +
                    "request_fingerprint, status, created_at, updated_at" +
                    ") VALUES (" +
                    "'operation-1', 'event-1', 'organizer-1', 'UPDATE_DRAFT_AGGREGATE', 5, " +
                    "'fingerprint-1', 'PENDING_SYNC', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z'" +
                    ")"
            )
            execute(
                "INSERT INTO syncMetadata(" +
                    "id, entityType, entityId, operation, timestamp, retryState, retryCount, synced" +
                    ") VALUES (" +
                    "'studio:operation-1', 'event', 'event-1', 'UPDATE', " +
                    "'2030-01-01T00:00:00Z', 'READY', 0, 0" +
                    ")"
            )
        }

        fun execute(sql: String) {
            driver.execute(null, sql, 0).value
        }

        fun number(sql: String): Long? = driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                app.cash.sqldelight.db.QueryResult.Value(cursor.getLong(0))
            },
            parameters = 0
        ).value

        fun text(sql: String): String? = driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                app.cash.sqldelight.db.QueryResult.Value(cursor.getString(0))
            },
            parameters = 0
        ).value
    }
}
