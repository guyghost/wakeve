package com.guyghost.wakeve.invitationexperience

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.ScenarioRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InvitationExperienceArchitectureReviewRedTest {

    @Test
    fun `legacy event update rejects a stale snapshot even when no protected receipt exists`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("event-1", status = "DRAFT", revision = 1)
        fixture.insertNoneArtwork("event-1")
        val repository = DatabaseEventRepository(fixture.database)
        val staleSnapshot = checkNotNull(repository.getEvent("event-1")).copy(title = "Stale overwrite")

        fixture.execute(
            "UPDATE event SET title = 'Concurrent authoritative title', aggregateRevision = 2 " +
                "WHERE id = 'event-1'"
        )

        val result = repository.updateEvent(staleSnapshot)

        assertTrue(
            result.isFailure,
            "A preserving legacy writer must compare the caller-held revision, not reload and accept the latest revision."
        )
        assertEquals(
            "Concurrent authoritative title",
            fixture.text("SELECT title FROM event WHERE id = 'event-1'")
        )
        assertEquals(2L, fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
    }

    @Test
    fun `unbound lifecycle writer cannot advance a protected aggregate without expected revision`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("event-1", status = "DRAFT", revision = 5)
        fixture.insertNoneArtwork("event-1")
        fixture.execute(
            "INSERT INTO event_operation_receipt(" +
                "operation_id, event_id, actor_id, action, aggregate_revision, status, created_at, updated_at" +
                ") VALUES (" +
                "'current-commit', 'event-1', 'organizer-1', 'UPDATE_DRAFT_AGGREGATE', 5, " +
                "'COMMITTED', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z'" +
                ")"
        )

        val result = DatabaseEventRepository(fixture.database)
            .updateEventStatus("event-1", EventStatus.POLLING, null)

        assertTrue(
            result.isFailure,
            "The legacy lifecycle API has no actor/base revision and must fail closed for a protected current commit."
        )
        assertEquals("DRAFT", fixture.text("SELECT status FROM event WHERE id = 'event-1'"))
        assertEquals(5L, fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
        assertEquals(
            0L,
            fixture.number("SELECT COUNT(*) FROM syncMetadata WHERE entityId = 'event-1'"),
            "A rejected CAS must not enqueue a false-success sync record."
        )
    }

    @Test
    fun `scenario creation advances its protected parent aggregate exactly once`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("event-1", status = "DRAFT", revision = 7)
        fixture.insertNoneArtwork("event-1")
        val scenario = Scenario(
            id = "scenario-1",
            eventId = "event-1",
            name = "Annecy",
            dateOrPeriod = "2030-02-01/2030-02-02",
            location = "Annecy",
            duration = 2,
            estimatedParticipants = 8,
            estimatedBudgetPerPerson = 120.0,
            description = "Weekend au lac",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2030-01-01T00:00:00Z",
            updatedAt = "2030-01-01T00:00:00Z"
        )

        val result = ScenarioRepository(fixture.database).createScenario(scenario)

        assertTrue(result.isSuccess)
        assertEquals(
            8L,
            fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"),
            "A Scenario child insert cannot bypass the event aggregate CAS owner."
        )
    }

    @Test
    fun `Library repairs a legacy event without artwork to total NONE instead of dropping it`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("legacy-event", status = "DRAFT", revision = 1)

        val state = DatabaseInvitationExperienceProjectionRepository(fixture.database).library(
            viewerId = "organizer-1",
            projection = LibraryProjection.DRAFTS,
            now = Instant.parse("2030-01-01T00:00:00Z")
        )

        val ready = assertIs<LibraryLoadState.Ready<List<LibraryCardProjection>>>(state)
        assertEquals(1, ready.snapshot.size)
        assertEquals("legacy-event", ready.snapshot.single().event.id)
        assertEquals(
            Artwork.None,
            ready.snapshot.single().artwork,
            "A mixed-version row without artwork must project the total NONE fallback."
        )
    }

    @Test
    fun `Library derives the typed polling action instead of falling back to generic view event`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("polling-event", status = "POLLING", revision = 3)
        fixture.insertNoneArtwork("polling-event")
        fixture.execute(
            "INSERT INTO timeSlot(id, eventId, startTime, endTime, timezone, createdAt, updatedAt) " +
                "VALUES ('future-slot', 'polling-event', '2030-01-08T10:00:00Z', " +
                "'2030-01-08T12:00:00Z', 'UTC', '2030-01-01T00:00:00Z', " +
                "'2030-01-01T00:00:00Z')"
        )

        val state = DatabaseInvitationExperienceProjectionRepository(fixture.database).library(
            viewerId = "organizer-1",
            projection = LibraryProjection.HOSTING,
            now = Instant.parse("2030-01-01T00:00:00Z")
        )

        val card = assertIs<LibraryLoadState.Ready<List<LibraryCardProjection>>>(state).snapshot.single()
        assertEquals(
            LibraryNextAction.VIEW_POLL_RESULTS,
            card.nextAction,
            "An interactive POLLING card must carry the domain-derived typed action supplied by its repository owner."
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
        fun seedEvent(id: String, status: String, revision: Long) {
            execute(
                """INSERT INTO event(
                    id, organizerId, title, description, status, deadline, createdAt, updatedAt,
                    aggregateRevision, aggregateSchemaVersion
                ) VALUES (
                    '$id', 'organizer-1', 'Event $id', 'Description', '$status',
                    '2030-01-10T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z',
                    $revision, 1
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
