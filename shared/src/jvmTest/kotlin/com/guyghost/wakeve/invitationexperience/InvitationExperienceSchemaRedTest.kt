package com.guyghost.wakeve.invitationexperience

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvitationExperienceSchemaRedTest {

    @Test
    fun `release one schema contains every event-owned invitation experience table`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WakeveDb.Schema.create(driver)

        val expectedTables = setOf(
            "event_artwork",
            "event_artwork_migration_issue",
            "event_operation_receipt",
            "direct_invite_batch",
            "direct_invite_recipient_outcome",
            "event_notification_preference"
        )
        val actualTables = driver.executeQuery(
            identifier = null,
            sql = "SELECT name FROM sqlite_master WHERE type = 'table'",
            mapper = { cursor ->
                val names = mutableSetOf<String>()
                while (cursor.next().value) {
                    cursor.getString(0)?.let(names::add)
                }
                app.cash.sqldelight.db.QueryResult.Value(names)
            },
            parameters = 0
        ).value

        assertTrue(
            actualTables.containsAll(expectedTables),
            "Missing invitation-experience tables: ${expectedTables - actualTables}"
        )
    }

    @Test
    fun `event aggregate exposes protected revision and schema version columns`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WakeveDb.Schema.create(driver)

        val columns = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA table_info(event)",
            mapper = { cursor ->
                val names = mutableSetOf<String>()
                while (cursor.next().value) {
                    cursor.getString(1)?.let(names::add)
                }
                app.cash.sqldelight.db.QueryResult.Value(names)
            },
            parameters = 0
        ).value

        assertEquals(
            emptySet(),
            setOf("aggregateRevision", "aggregateSchemaVersion") - columns,
            "The event aggregate must persist both protected version dimensions."
        )
    }

    @Test
    fun `protected recipient and event preference rows carry retention and revision metadata`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WakeveDb.Schema.create(driver)

        assertEquals(
            emptySet(),
            setOf("recipient_key", "key_version", "expires_at") -
                tableColumns(driver, "direct_invite_recipient_outcome"),
            "Recipient outcomes need an explicit protected-key version and bounded retention."
        )
        assertEquals(
            emptySet(),
            setOf("preference", "revision", "operation_id", "sync_status") -
                tableColumns(driver, "event_notification_preference"),
            "Event preference writes need revision and exact operation sync metadata."
        )
    }

    private fun tableColumns(
        driver: JdbcSqliteDriver,
        table: String
    ): Set<String> = driver.executeQuery(
        identifier = null,
        sql = "PRAGMA table_info($table)",
        mapper = { cursor ->
            val names = mutableSetOf<String>()
            while (cursor.next().value) {
                cursor.getString(1)?.let(names::add)
            }
            app.cash.sqldelight.db.QueryResult.Value(names)
        },
        parameters = 0
    ).value
}
