package com.guyghost.wakeve.invitationexperience

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.repository.DatabaseEventRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ServerArtworkReferenceOwnerRedTest {

    @Test
    fun `bind validates persisted artwork and replays one stable operation idempotently`() = runTest {
        val fixture = fixture()
        fixture.seedServerArtwork("event-1", "asset-shared", 4)
        fixture.seedServerArtwork("event-2", "asset-shared", 4)
        val owner = DatabaseServerArtworkReferenceOwner(fixture.database)
        val reference = reference("event-1")

        val first = ServerArtworkReferenceResult.Bound(reference, referenceCount = 1)
        assertEquals(first, owner.bind(reference, "bind-operation-1"))
        assertEquals(first, owner.bind(reference, "bind-operation-1"))
        assertIs<ServerArtworkReferenceResult.Rejected>(
            owner.bind(reference("event-2"), "bind-operation-1"),
            "One stable operation id cannot be rebound to a different event reference."
        )
        assertIs<ServerArtworkReferenceResult.Rejected>(
            owner.bind(reference.copy(assetRevision = 3), "bind-stale-revision")
        )
        assertEquals(1L, fixture.number("SELECT COUNT(*) FROM server_artwork_reference"))
    }

    @Test
    fun `shared asset schedules one physical release only after final reference`() = runTest {
        val fixture = fixture()
        fixture.seedServerArtwork("event-1", "asset-shared", 4)
        fixture.seedServerArtwork("event-2", "asset-shared", 4)
        val owner = DatabaseServerArtworkReferenceOwner(fixture.database)
        val first = reference("event-1")
        val second = reference("event-2")
        assertIs<ServerArtworkReferenceResult.Bound>(owner.bind(first, "bind-operation-1"))
        assertEquals(
            ServerArtworkReferenceResult.Bound(second, referenceCount = 2),
            owner.bind(second, "bind-operation-2")
        )

        fixture.execute("DELETE FROM event_artwork WHERE event_id = 'event-1'")
        assertEquals(
            ServerArtworkReferenceResult.Retained(referenceCount = 1),
            owner.release(first, "release-operation-1")
        )
        assertEquals(
            ServerArtworkReferenceResult.Retained(referenceCount = 1),
            owner.release(first, "release-operation-1"),
            "Release replay must not decrement a second event's reference."
        )
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM server_artwork_release_outbox"))

        fixture.execute("DELETE FROM event_artwork WHERE event_id = 'event-2'")
        val final = ServerArtworkReferenceResult.FinalReleaseScheduled(
            ServerArtworkReleaseSignal("asset-shared", 4, "release-operation-2")
        )
        assertEquals(final, owner.release(second, "release-operation-2"))
        assertEquals(final, owner.release(second, "release-operation-2"))
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM server_artwork_reference"))
        assertEquals(1L, fixture.number("SELECT COUNT(*) FROM server_artwork_release_outbox"))
        assertEquals(
            "release-operation-2",
            fixture.text("SELECT release_operation_id FROM server_artwork_release_outbox")
        )
    }

    @Test
    fun `event deletion transaction retains shared asset then schedules final release`() = runTest {
        val fixture = fixture()
        fixture.seedServerArtwork("event-1", "asset-shared", 4)
        fixture.seedServerArtwork("event-2", "asset-shared", 4)
        val owner = DatabaseServerArtworkReferenceOwner(fixture.database)
        assertIs<ServerArtworkReferenceResult.Bound>(owner.bind(reference("event-1"), "bind-1"))
        assertIs<ServerArtworkReferenceResult.Bound>(owner.bind(reference("event-2"), "bind-2"))
        val events = DatabaseEventRepository(fixture.database)

        assertTrue(events.deleteEvent("event-1").isSuccess)
        assertEquals(1L, fixture.number("SELECT COUNT(*) FROM server_artwork_reference"))
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM server_artwork_release_outbox"))

        assertTrue(events.deleteEvent("event-2").isSuccess)
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM server_artwork_reference"))
        assertEquals(1L, fixture.number("SELECT COUNT(*) FROM server_artwork_release_outbox"))
    }

    @Test
    fun `failed event deletion rolls back reference release and outbox together`() = runTest {
        val fixture = fixture()
        fixture.seedServerArtwork("event-1", "asset-shared", 4)
        val owner = DatabaseServerArtworkReferenceOwner(fixture.database)
        assertIs<ServerArtworkReferenceResult.Bound>(owner.bind(reference("event-1"), "bind-1"))
        fixture.execute(
            "CREATE TRIGGER abort_server_artwork_event_delete BEFORE DELETE ON event " +
                "WHEN OLD.id = 'event-1' BEGIN SELECT RAISE(ABORT, 'forced delete failure'); END"
        )

        assertTrue(DatabaseEventRepository(fixture.database).deleteEvent("event-1").isFailure)
        assertEquals(1L, fixture.number("SELECT COUNT(*) FROM event WHERE id = 'event-1'"))
        assertEquals(1L, fixture.number("SELECT COUNT(*) FROM event_artwork WHERE event_id = 'event-1'"))
        assertEquals(1L, fixture.number("SELECT COUNT(*) FROM server_artwork_reference"))
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM server_artwork_release_outbox"))
    }

    private fun reference(eventId: String) = ServerArtworkReference(
        eventId = eventId,
        assetId = "asset-shared",
        assetRevision = 4
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
        fun seedServerArtwork(eventId: String, assetId: String, assetRevision: Long) {
            execute(
                """INSERT INTO event(
                    id, organizerId, title, description, status, deadline, createdAt, updatedAt,
                    aggregateRevision, aggregateSchemaVersion
                ) VALUES (
                    '$eventId', 'organizer-1', 'Event $eventId', 'Description', 'DRAFT',
                    '2100-01-10T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z',
                    4, 1
                )"""
            )
            execute(
                "INSERT INTO event_artwork(" +
                    "event_id, kind, structured_version, source_kind, server_asset_id, " +
                    "canonical_https_url, asset_revision, alt_kind, focal_x, focal_y, crop, updated_at" +
                    ") VALUES (" +
                    "'$eventId', 'STRUCTURED', 1, 'SERVER_ASSET', '$assetId', " +
                    "'https://cdn.wakeve.app/assets/$assetId.jpg', $assetRevision, " +
                    "'DECORATIVE', 0.5, 0.5, 'FILL', '2030-01-01T00:00:00Z'" +
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
