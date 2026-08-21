package com.guyghost.wakeve.invitationexperience

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.repository.UserRepository
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InvitationExperienceSecurityArchitectureRedTest {

    @Test
    fun `only a persisted active membership proof can create ActiveMember access`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("event-1", organizerId = "organizer-1", status = "POLLING")
        fixture.insertNoneArtwork("event-1")
        fixture.execute(
            "INSERT INTO participant(id, eventId, userId, role, hasValidatedDate, joinedAt, updatedAt) " +
                "VALUES ('removed-membership', 'event-1', 'viewer-1', 'REMOVED', 1, " +
                "'2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')"
        )

        val state = DatabaseInvitationExperienceProjectionRepository(fixture.database).library(
            viewerId = "viewer-1",
            projection = LibraryProjection.ATTENDING,
            now = Instant.parse("2030-01-01T00:00:00Z")
        )

        assertIs<LibraryLoadState.Empty>(
            state,
            "A participant-shaped row with REMOVED/unknown role is not proof of active membership."
        )
    }

    @Test
    fun `direct invite queue owns an encrypted expiring delivery envelope`() {
        val fixture = fixture()

        assertEquals(
            "direct_invite_delivery_envelope",
            fixture.text(
                "SELECT name FROM sqlite_master " +
                    "WHERE type = 'table' AND name = 'direct_invite_delivery_envelope'"
            ),
            "Queued/retryable delivery cannot work from a one-way recipient digest; the owner needs a protected operation-scoped envelope."
        )
        val columns = fixture.texts("PRAGMA table_info('direct_invite_delivery_envelope')", columnIndex = 1)
        assertTrue("batch_id" in columns)
        assertTrue("recipient_key" in columns)
        assertTrue("ciphertext" in columns)
        assertTrue("key_version" in columns)
        assertTrue("expires_at" in columns)
        assertTrue("transport_state" in columns)
    }

    @Test
    fun `notification operation id is globally unique and cannot alias two preference writes`() {
        val fixture = fixture()
        fixture.seedEvent("event-1", organizerId = "organizer-1", status = "DRAFT")
        fixture.seedEvent("event-2", organizerId = "organizer-2", status = "DRAFT")
        fixture.seedUser("viewer-1")
        fixture.seedUser("viewer-2")
        fixture.execute(
            "INSERT INTO event_notification_preference(" +
                "event_id, user_id, preference, operation_id, sync_status, updated_at" +
                ") VALUES (" +
                "'event-1', 'viewer-1', 'ESSENTIAL_ONLY', 'same-operation', 'PENDING', " +
                "'2030-01-01T00:00:00Z'" +
                ")"
        )

        assertFails("One stable operation id must identify exactly one notification preference write.") {
            fixture.execute(
                "INSERT INTO event_notification_preference(" +
                    "event_id, user_id, preference, operation_id, sync_status, updated_at" +
                    ") VALUES (" +
                    "'event-2', 'viewer-2', 'MUTED', 'same-operation', 'PENDING', " +
                    "'2030-01-01T00:00:00Z'" +
                    ")"
            )
        }
    }

    @Test
    fun `production JVM database factory enables foreign key enforcement on every connection`() {
        val databaseFile = File.createTempFile("wakeve-foreign-key-", ".sqlite")
        assertTrue(databaseFile.delete(), "The production factory must receive a fresh path for this test.")
        val driver = JvmDatabaseFactory(databaseFile.absolutePath).createDriver()

        try {
            assertEquals(
                1L,
                number(driver, "PRAGMA foreign_keys"),
                "Production database factories must enable FK enforcement instead of relying on test-only setup."
            )
        } finally {
            driver.close()
            databaseFile.delete()
        }
    }

    @Test
    fun `account erasure reports missing subject instead of false-success Unit`() = runTest {
        val fixture = fixture()

        val result = UserRepository(fixture.database).deleteUser("missing-user")

        assertTrue(
            result.isFailure,
            "Erasure needs an explicit not-found or rejected outcome; deleting zero rows cannot be reported as success."
        )
    }

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
        fun seedEvent(id: String, organizerId: String, status: String) {
            execute(
                """INSERT INTO event(
                    id, organizerId, title, description, status, deadline, createdAt, updatedAt,
                    aggregateRevision, aggregateSchemaVersion
                ) VALUES (
                    '$id', '$organizerId', 'Event $id', 'Description', '$status',
                    '2030-01-10T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z',
                    1, 1
                )"""
            )
        }

        fun seedUser(id: String) {
            execute(
                """INSERT INTO user(
                    id, provider_id, email, name, provider, created_at, updated_at
                ) VALUES (
                    '$id', 'provider-$id', '$id@example.com', 'Viewer $id', 'apple',
                    '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z'
                )"""
            )
        }

        fun insertNoneArtwork(eventId: String) {
            execute(
                "INSERT INTO event_artwork(event_id, kind, updated_at) " +
                    "VALUES ('$eventId', 'NONE', '2030-01-01T00:00:00Z')"
            )
        }

        fun execute(sql: String) {
            driver.execute(null, sql, 0).value
        }

        fun text(sql: String): String? = driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                app.cash.sqldelight.db.QueryResult.Value(cursor.getString(0))
            },
            parameters = 0
        ).value

        fun texts(sql: String, columnIndex: Int): Set<String> = driver.executeQuery(
            identifier = null,
        sql = sql,
        mapper = { cursor ->
                val values = mutableSetOf<String>()
                while (cursor.next().value) {
                    cursor.getString(columnIndex)?.let(values::add)
                }
                app.cash.sqldelight.db.QueryResult.Value(values.toSet())
            },
            parameters = 0
        ).value
    }

    private fun number(driver: SqlDriver, sql: String): Long? = driver.executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            cursor.next()
            app.cash.sqldelight.db.QueryResult.Value(cursor.getLong(0))
        },
        parameters = 0
    ).value
}
