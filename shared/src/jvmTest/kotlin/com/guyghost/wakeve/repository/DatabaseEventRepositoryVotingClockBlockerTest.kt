package com.guyghost.wakeve.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.database.WakeveDb
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseEventRepositoryVotingClockBlockerTest {

    @Test
    fun `voting remains open only while injected now is strictly before deadline`() {
        val deadline = "2026-08-28T12:00:00Z"

        assertFalse(repositoryAt("2026-08-28T11:59:59.999Z").isDeadlinePassed(deadline))
        assertTrue(
            repositoryAt(deadline).isDeadlinePassed(deadline),
            "The deadline instant itself is closed; only now < deadline may mutate a vote."
        )
        assertTrue(repositoryAt("2026-08-28T12:00:00.001Z").isDeadlinePassed(deadline))
    }

    @Test
    fun `malformed deadline fails closed`() {
        assertTrue(
            repositoryAt("2026-08-28T11:00:00Z").isDeadlinePassed("not-an-iso-instant"),
            "An invalid deadline must never silently reopen voting."
        )
    }

    private fun repositoryAt(nowIso: String): DatabaseEventRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WakeveDb.Schema.create(driver)
        return DatabaseEventRepository(
            WakeveDb(driver),
            ConfirmationClock { Instant.parse(nowIso) }
        )
    }
}
