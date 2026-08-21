package com.guyghost.wakeve.invitationexperience

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.repository.DatabaseEventRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ParticipantAxesPersistenceRedTest {

    @Test
    fun `participant schema persists independent total RSVP and retained-date axes`() {
        val fixture = fixture()
        val columns = fixture.texts("PRAGMA table_info(participant)", columnIndex = 1).toSet()
        val tableSql = fixture.text(
            "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'participant'"
        ).orEmpty()

        assertTrue(
            "rsvpState" in columns,
            "RSVP must be persisted independently instead of being reconstructed from hasValidatedDate."
        )
        assertTrue(
            "dateValidationState" in columns,
            "Retained-date validation needs its own total persisted axis."
        )
        listOf(
            "PENDING",
            "ACCEPTED",
            "DECLINED",
            "NOT_APPLICABLE",
            "UNAVAILABLE"
        ).forEach { state ->
            assertTrue(state in tableSql, "Participant RSVP persistence must accept $state explicitly.")
        }
        listOf(
            "NOT_VALIDATED",
            "VALIDATED_RETAINED_DATE",
            "NOT_APPLICABLE",
            "UNAVAILABLE"
        ).forEach { state ->
            assertTrue(
                state in tableSql,
                "Participant retained-date persistence must accept $state explicitly."
            )
        }
    }

    @Test
    fun `legacy validation bit never infers RSVP acceptance or pending membership`() {
        val fixture = fixture()
        fixture.seedPollingEvent()
        fixture.insertParticipant("member-validated", "viewer-validated", hasValidatedDate = 1)
        fixture.insertParticipant("member-unvalidated", "viewer-unvalidated", hasValidatedDate = 0)

        val records = checkNotNull(
            DatabaseEventRepository(fixture.database).getParticipantRecords("event-1")
        ).associateBy { it.id }

        assertEquals(
            "UNAVAILABLE",
            records.getValue("member-validated").rsvp,
            "A legacy date-validation bit is not an RSVP acceptance record."
        )
        assertEquals(
            "UNAVAILABLE",
            records.getValue("member-unvalidated").rsvp,
            "Absence of date validation is not a pending RSVP record."
        )
    }

    @Test
    fun `Library action consumes total access and never offers vote from a derived RSVP`() = runTest {
        val fixture = fixture()
        fixture.seedPollingEvent()
        fixture.insertParticipant("member-1", "viewer-1", hasValidatedDate = 1)

        val repository = DatabaseInvitationExperienceProjectionRepository(fixture.database)
        val upcoming = repository.library(
            viewerId = "viewer-1",
            projection = LibraryProjection.UPCOMING,
            now = Instant.parse("2030-01-01T00:00:00Z")
        )

        val card = assertIs<LibraryLoadState.Ready<List<LibraryCardProjection>>>(upcoming)
            .snapshot
            .single()
        assertEquals(
            LibraryNextAction.VIEW_EVENT,
            card.nextAction,
            "Missing RSVP access must fall back to the safe event view, never SUBMIT_VOTE."
        )

        assertIs<LibraryLoadState.Empty>(
            repository.library(
                viewerId = "viewer-1",
                projection = LibraryProjection.ATTENDING,
                now = Instant.parse("2030-01-01T00:00:00Z")
            ),
            "A retained-date bit cannot make an identity attend without accepted RSVP evidence."
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
        fun seedPollingEvent() {
            execute(
                """INSERT INTO event(
                    id, organizerId, title, description, status, deadline, createdAt, updatedAt,
                    aggregateRevision, aggregateSchemaVersion
                ) VALUES (
                    'event-1', 'organizer-1', 'Polling event', 'Description', 'POLLING',
                    '2030-01-10T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z',
                    3, 1
                )"""
            )
            execute(
                "INSERT INTO event_artwork(event_id, kind, updated_at) " +
                    "VALUES ('event-1', 'NONE', '2030-01-01T00:00:00Z')"
            )
            execute(
                "INSERT INTO timeSlot(id, eventId, startTime, endTime, timezone, createdAt, updatedAt) " +
                    "VALUES ('slot-1', 'event-1', '2030-01-08T10:00:00Z', '2030-01-08T12:00:00Z', " +
                    "'UTC', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')"
            )
        }

        fun insertParticipant(id: String, userId: String, hasValidatedDate: Long) {
            execute(
                "INSERT INTO participant(id, eventId, userId, role, hasValidatedDate, joinedAt, updatedAt) " +
                    "VALUES ('$id', 'event-1', '$userId', 'MEMBER', $hasValidatedDate, " +
                    "'2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')"
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

        fun texts(sql: String, columnIndex: Int): List<String> = driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                val values = buildList {
                    while (cursor.next().value) {
                        cursor.getString(columnIndex)?.let(::add)
                    }
                }
                QueryResult.Value(values)
            },
            parameters = 0
        ).value
    }
}
