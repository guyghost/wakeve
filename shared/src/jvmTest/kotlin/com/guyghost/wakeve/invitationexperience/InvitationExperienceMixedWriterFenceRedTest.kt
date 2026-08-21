package com.guyghost.wakeve.invitationexperience

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.ScenarioRepository
import com.guyghost.wakeve.repository.UserRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvitationExperienceMixedWriterFenceRedTest {

    @Test
    fun `aggregate permit is correlated to the row being mutated and cannot authorize another event`() {
        val fixture = fixture()
        fixture.seedProtectedAggregate()
        fixture.seedSecondProtectedAggregate()
        fixture.execute(
            """INSERT INTO aggregate_write_authorization(
                event_id, expected_revision, writer_schema_version, operation_id, created_at
            ) VALUES ('event-2', 9, 1, 'authorized-event-2', '2030-01-01T00:00:00Z')"""
        )

        runCatching {
            fixture.execute(
                "UPDATE event SET title = 'Cross-event overwrite' WHERE id = 'event-1'"
            )
        }

        assertEquals(
            "Protected event",
            fixture.text("SELECT title FROM event WHERE id = 'event-1'"),
            "A valid permit for event-2 must never authorize a legacy write to event-1."
        )
        assertEquals(7L, fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
    }

    @Test
    fun `a committed receipt never exempts a protected event from the expected revision fence`() {
        val fixture = fixture()
        fixture.seedProtectedAggregate()
        fixture.execute(
            """INSERT INTO event_operation_receipt(
                operation_id, event_id, actor_id, action, aggregate_revision,
                request_fingerprint, status, created_at, updated_at
            ) VALUES (
                'already-committed', 'event-1', 'organizer-1', 'UPDATE_DRAFT_AGGREGATE', 7,
                'fingerprint', 'COMMITTED', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z'
            )"""
        )

        runCatching {
            fixture.execute(
                "UPDATE event SET status = 'POLLING' WHERE id = 'event-1'"
            )
        }

        assertEquals(
            "DRAFT",
            fixture.text("SELECT status FROM event WHERE id = 'event-1'"),
            "Historical COMMITTED receipts prove an earlier operation; they cannot authorize a new unfenced write."
        )
        assertEquals(7L, fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
    }

    @Test
    fun `old binary SQL cannot mutate protected event or scenario rows without a revision fence`() {
        val cases = listOf(
            LegacyMutation(
                sql = "UPDATE event SET title = 'Legacy overwrite', version = version + 1 WHERE id = 'event-1'",
                observedSql = "SELECT title FROM event WHERE id = 'event-1'",
                expectedValue = "Protected event"
            ),
            LegacyMutation(
                sql = "UPDATE event SET status = 'POLLING' WHERE id = 'event-1'",
                observedSql = "SELECT status FROM event WHERE id = 'event-1'",
                expectedValue = "DRAFT"
            ),
            LegacyMutation(
                sql = "UPDATE event SET planningMode = 'SCENARIO_MATRIX' WHERE id = 'event-1'",
                observedSql = "SELECT planningMode FROM event WHERE id = 'event-1'",
                expectedValue = "TIME_SLOT_POLL"
            ),
            LegacyMutation(
                sql = "UPDATE event SET organizerId = 'deleted-user' WHERE id = 'event-1'",
                observedSql = "SELECT organizerId FROM event WHERE id = 'event-1'",
                expectedValue = "organizer-1"
            ),
            LegacyMutation(
                sql = "UPDATE scenario SET status = 'SELECTED' WHERE id = 'scenario-1'",
                observedSql = "SELECT status FROM scenario WHERE id = 'scenario-1'",
                expectedValue = "PROPOSED"
            ),
            LegacyMutation(
                sql = "DELETE FROM scenario WHERE id = 'scenario-1'",
                observedSql = "SELECT CAST(COUNT(*) AS TEXT) FROM scenario WHERE id = 'scenario-1'",
                expectedValue = "1"
            ),
            LegacyMutation(
                sql = "DELETE FROM event WHERE id = 'event-1'",
                observedSql = "SELECT CAST(COUNT(*) AS TEXT) FROM event WHERE id = 'event-1'",
                expectedValue = "1"
            )
        )

        cases.forEach { mutation ->
            val fixture = fixture()
            fixture.seedProtectedAggregate()

            runCatching { fixture.execute(mutation.sql) }

            assertEquals(
                mutation.expectedValue,
                fixture.text(mutation.observedSql),
                "A database migrated for the invitation experience must fence the legacy mutation: ${mutation.sql}"
            )
            assertEquals(
                7L,
                fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"),
                "A rejected legacy writer must preserve the authoritative aggregate revision."
            )
        }
    }

    @Test
    fun `legacy first insertion cannot hide a later protected event or scenario from the raw writer fence`() {
        val fixture = fixture().also { it.seedLegacyFirstThenProtectedTarget() }

        val eventAttempt = runCatching {
            fixture.execute(
                "UPDATE event SET title = 'Inverse-order event overwrite' WHERE id = 'event-2'"
            )
        }
        val scenarioAttempt = runCatching {
            fixture.execute(
                "UPDATE scenario SET status = 'SELECTED' WHERE id = 'scenario-2'"
            )
        }

        assertTrue(
            eventAttempt.isFailure,
            "The event trigger must evaluate the later event-2 row, not the first rev1 row in the table."
        )
        assertTrue(
            scenarioAttempt.isFailure,
            "The scenario trigger must evaluate scenario-2's protected parent, not the first scenario row."
        )
        assertEquals("Protected target", fixture.text("SELECT title FROM event WHERE id = 'event-2'"))
        assertEquals("PROPOSED", fixture.text("SELECT status FROM scenario WHERE id = 'scenario-2'"))
        assertEquals(7L, fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-2'"))
    }

    @Test
    fun `only a matching permit authorizes a later protected event and its scenario`() {
        val fixture = fixture().also { it.seedLegacyFirstThenProtectedTarget() }
        fixture.execute(
            """INSERT INTO aggregate_write_authorization(
                event_id, expected_revision, writer_schema_version, operation_id, created_at
            ) VALUES ('event-1', 1, 1, 'permit-first-event', '2030-01-01T00:00:00Z')"""
        )

        val crossEventAttempt = runCatching {
            fixture.execute(
                "UPDATE event SET title = 'Cross-permit overwrite' WHERE id = 'event-2'"
            )
        }
        val crossScenarioAttempt = runCatching {
            fixture.execute(
                "UPDATE scenario SET status = 'SELECTED' WHERE id = 'scenario-2'"
            )
        }

        assertTrue(crossEventAttempt.isFailure, "A permit for the first aggregate must not authorize event-2.")
        assertTrue(crossScenarioAttempt.isFailure, "A permit for the first aggregate must not authorize scenario-2.")
        assertEquals("Protected target", fixture.text("SELECT title FROM event WHERE id = 'event-2'"))
        assertEquals("PROPOSED", fixture.text("SELECT status FROM scenario WHERE id = 'scenario-2'"))
        assertEquals(7L, fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-2'"))

        fixture.execute(
            """INSERT INTO aggregate_write_authorization(
                event_id, expected_revision, writer_schema_version, operation_id, created_at
            ) VALUES ('event-2', 7, 1, 'permit-protected-target', '2030-01-01T00:00:00Z')"""
        )

        val authorizedEventAttempt = runCatching {
            fixture.execute(
                "UPDATE event SET title = 'Authorized target', aggregateRevision = 8 WHERE id = 'event-2'"
            )
        }
        val authorizedScenarioAttempt = runCatching {
            fixture.execute(
                "UPDATE scenario SET status = 'SELECTED' WHERE id = 'scenario-2'"
            )
        }

        assertTrue(
            authorizedEventAttempt.isSuccess,
            "The matching event-2 permit must authorize its exact-revision CAS: ${authorizedEventAttempt.exceptionOrNull()}"
        )
        assertTrue(
            authorizedScenarioAttempt.isSuccess,
            "The same event-2 permit must authorize its owned scenario after the one-step revision advance: ${authorizedScenarioAttempt.exceptionOrNull()}"
        )
        assertEquals("Authorized target", fixture.text("SELECT title FROM event WHERE id = 'event-2'"))
        assertEquals("SELECTED", fixture.text("SELECT status FROM scenario WHERE id = 'scenario-2'"))
        assertEquals(8L, fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-2'"))
        assertEquals(1L, fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
    }

    @Test
    fun `legacy repository lifecycle and scenario writers fail closed without caller expected revision`() = runTest {
        val eventFixture = fixture().also { it.seedProtectedAggregate() }
        val lifecycle = DatabaseEventRepository(eventFixture.database)
            .updateEventStatus("event-1", EventStatus.POLLING, null)

        assertTrue(lifecycle.isFailure, "Lifecycle mutation without expectedRevision must be rejected.")
        assertEquals("DRAFT", eventFixture.text("SELECT status FROM event WHERE id = 'event-1'"))
        assertEquals(7L, eventFixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"))

        val statusFixture = fixture().also { it.seedProtectedAggregate() }
        val scenarioStatus = ScenarioRepository(statusFixture.database)
            .updateScenarioStatus("scenario-1", ScenarioStatus.SELECTED)

        assertTrue(scenarioStatus.isFailure, "Scenario status mutation needs its parent expectedRevision.")
        assertEquals("PROPOSED", statusFixture.text("SELECT status FROM scenario WHERE id = 'scenario-1'"))
        assertEquals(7L, statusFixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"))

        val deleteFixture = fixture().also { it.seedProtectedAggregate() }
        val scenarioDelete = ScenarioRepository(deleteFixture.database).deleteScenario("scenario-1")

        assertTrue(scenarioDelete.isFailure, "Scenario deletion needs its parent expectedRevision.")
        assertEquals(1L, deleteFixture.number("SELECT COUNT(*) FROM scenario WHERE id = 'scenario-1'"))
        assertEquals(7L, deleteFixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
    }

    @Test
    fun `legacy account erasure API cannot anonymize protected events without actor and expected revisions`() = runTest {
        val fixture = fixture().also { it.seedProtectedAggregate() }

        val result = UserRepository(fixture.database).deleteUser("organizer-1")

        assertTrue(
            result.isFailure,
            "Account erasure must be owned by an actor-bound command carrying every event expectedRevision."
        )
        assertEquals("organizer-1", fixture.text("SELECT organizerId FROM event WHERE id = 'event-1'"))
        assertEquals(7L, fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
        assertEquals(1L, fixture.number("SELECT COUNT(*) FROM user WHERE id = 'organizer-1'"))
    }

    @Test
    fun crossEventPermitDoesNotAuthorizeScenarioRows() {
        val fixture = fixture()
        fixture.seedProtectedAggregate()
        fixture.seedSecondProtectedAggregate()
        fixture.execute(
            """INSERT INTO aggregate_write_authorization(
                event_id, expected_revision, writer_schema_version, operation_id, created_at
            ) VALUES ('event-2', 9, 1, 'authorized-event-2', '2030-01-01T00:00:00Z')"""
        )

        runCatching {
            fixture.execute("UPDATE scenario SET status = 'SELECTED' WHERE id = 'scenario-1'")
        }

        assertEquals(
            "PROPOSED",
            fixture.text("SELECT status FROM scenario WHERE id = 'scenario-1'"),
            "A permit for event-2 must not authorize a child-row mutation owned by event-1."
        )
        assertEquals(7L, fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
    }

    private fun fixture(): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
        WakeveDb.Schema.create(driver)
        return Fixture(driver, WakeveDb(driver))
    }

    private data class LegacyMutation(
        val sql: String,
        val observedSql: String,
        val expectedValue: String
    )

    private class Fixture(
        private val driver: SqlDriver,
        val database: WakeveDb
    ) {
        fun seedProtectedAggregate() {
            execute(
                "INSERT INTO user(id, provider_id, email, name, provider, role, created_at, updated_at) " +
                    "VALUES ('organizer-1', 'provider-1', 'owner@example.invalid', 'Owner', 'apple', " +
                    "'ORGANIZER', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')"
            )
            execute(
                """INSERT INTO event(
                    id, organizerId, title, description, status, deadline, createdAt, updatedAt,
                    version, aggregateRevision, aggregateSchemaVersion, planningMode
                ) VALUES (
                    'event-1', 'organizer-1', 'Protected event', 'Description', 'DRAFT',
                    '2030-01-10T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z',
                    4, 7, 1, 'TIME_SLOT_POLL'
                )"""
            )
            execute(
                "INSERT INTO event_artwork(event_id, kind, updated_at) " +
                    "VALUES ('event-1', 'NONE', '2030-01-01T00:00:00Z')"
            )
            execute(
                """INSERT INTO scenario(
                    id, eventId, name, dateOrPeriod, location, duration, estimatedParticipants,
                    estimatedBudgetPerPerson, description, status, createdAt, updatedAt
                ) VALUES (
                    'scenario-1', 'event-1', 'Lake', '2030-02-01', 'Annecy', 2, 8,
                    100.0, 'Description', 'PROPOSED',
                    '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z'
                )"""
            )
        }

        fun seedSecondProtectedAggregate() {
            execute(
                """INSERT INTO event(
                    id, organizerId, title, description, status, deadline, createdAt, updatedAt,
                    version, aggregateRevision, aggregateSchemaVersion, planningMode
                ) VALUES (
                    'event-2', 'organizer-1', 'Second protected event', 'Description', 'DRAFT',
                    '2030-01-10T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z',
                    5, 9, 1, 'TIME_SLOT_POLL'
                )"""
            )
            execute(
                "INSERT INTO event_artwork(event_id, kind, updated_at) " +
                    "VALUES ('event-2', 'NONE', '2030-01-01T00:00:00Z')"
            )
        }

        fun seedLegacyFirstThenProtectedTarget() {
            execute(
                "INSERT INTO user(id, provider_id, email, name, provider, role, created_at, updated_at) " +
                    "VALUES ('organizer-1', 'provider-1', 'owner@example.invalid', 'Owner', 'apple', " +
                    "'ORGANIZER', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')"
            )
            execute(
                """INSERT INTO event(
                    id, organizerId, title, description, status, deadline, createdAt, updatedAt,
                    version, aggregateRevision, aggregateSchemaVersion, planningMode
                ) VALUES (
                    'event-1', 'organizer-1', 'Legacy first event', 'Description', 'DRAFT',
                    '2030-01-10T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z',
                    1, 1, 1, 'TIME_SLOT_POLL'
                )"""
            )
            execute(
                "INSERT INTO event_artwork(event_id, kind, updated_at) " +
                    "VALUES ('event-1', 'NONE', '2030-01-01T00:00:00Z')"
            )
            execute(
                """INSERT INTO scenario(
                    id, eventId, name, dateOrPeriod, location, duration, estimatedParticipants,
                    estimatedBudgetPerPerson, description, status, createdAt, updatedAt
                ) VALUES (
                    'scenario-1', 'event-1', 'Legacy first scenario', '2030-01-20', 'Paris', 1, 4,
                    40.0, 'Description', 'PROPOSED',
                    '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z'
                )"""
            )
            execute(
                """INSERT INTO event(
                    id, organizerId, title, description, status, deadline, createdAt, updatedAt,
                    version, aggregateRevision, aggregateSchemaVersion, planningMode
                ) VALUES (
                    'event-2', 'organizer-1', 'Protected target', 'Description', 'DRAFT',
                    '2030-01-10T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z',
                    4, 7, 1, 'TIME_SLOT_POLL'
                )"""
            )
            execute(
                "INSERT INTO event_artwork(event_id, kind, updated_at) " +
                    "VALUES ('event-2', 'NONE', '2030-01-01T00:00:00Z')"
            )
            execute(
                """INSERT INTO scenario(
                    id, eventId, name, dateOrPeriod, location, duration, estimatedParticipants,
                    estimatedBudgetPerPerson, description, status, createdAt, updatedAt
                ) VALUES (
                    'scenario-2', 'event-2', 'Protected target scenario', '2030-02-01', 'Annecy', 2, 8,
                    100.0, 'Description', 'PROPOSED',
                    '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z'
                )"""
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
                QueryResult.Value(cursor.getString(0))
            },
            parameters = 0
        ).value

        fun number(sql: String): Long? = driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0))
            },
            parameters = 0
        ).value
    }
}
