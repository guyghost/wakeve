package com.guyghost.wakeve.repository

import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.poll.PollBallotContract
import com.guyghost.wakeve.presentation.state.EventManagementContract
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.io.File
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.sql.DriverManager
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TimeSlotStorageIdentityRedTest {

    @Test
    fun `same logical slot id in two events is independently ballotable and confirmable`() = runBlocking {
        val database = createFreshTestDatabase()
        val repository = DatabaseEventRepository(
            database,
            ConfirmationClock { Instant.parse(NOW) }
        )
        val first = persistPollingEvent(repository, "event-a", "organizer-a", SHARED_SLOT_ID)
        val second = persistPollingEvent(repository, "event-b", "organizer-b", SHARED_SLOT_ID)

        commitOneSlotBallot(repository, first, "ballot-a")
        commitOneSlotBallot(repository, second, "ballot-b")

        val firstConfirmation = repository.confirmPollDate(
            confirmationCommand(first, "confirm-a")
        )
        val secondConfirmation = repository.confirmPollDate(
            confirmationCommand(second, "confirm-b")
        )

        assertIs<EventManagementContract.ConfirmationResult.Committed>(firstConfirmation)
        assertIs<EventManagementContract.ConfirmationResult.Committed>(
            secondConfirmation,
            "Confirmation lookup must resolve (eventId, logicalSlotId), not the first global slot id."
        )
        assertEquals("2099-10-01T10:00:00Z", repository.getEvent("event-a")?.finalDate)
        assertEquals("2099-10-01T10:00:00Z", repository.getEvent("event-b")?.finalDate)
    }

    @Test
    fun `save event slot synchronization never drops a colliding logical slot`() = runBlocking {
        val database = createFreshTestDatabase()
        val repository = DatabaseEventRepository(
            database,
            ConfirmationClock { Instant.parse(NOW) }
        )
        persistPollingEvent(repository, "event-a", "organizer-a", SHARED_SLOT_ID)
        val second = persistPollingEvent(repository, "event-b", "organizer-b", SHARED_SLOT_ID)
        val updatedSlots = listOf(
            slot(SHARED_SLOT_ID, "2099-10-02T10:00:00Z"),
            slot("event-b-only", "2099-10-03T10:00:00Z")
        )

        repository.saveEvent(second.copy(proposedSlots = updatedSlots)).getOrThrow()

        assertEquals(
            updatedSlots.map { it.id }.toSet(),
            repository.getEvent("event-b")?.proposedSlots?.map { it.id }?.toSet(),
            "Delete-then-insert must be one transaction and use event-scoped physical identities."
        )
        assertEquals(
            setOf(SHARED_SLOT_ID),
            repository.getEvent("event-a")?.proposedSlots?.map { it.id }?.toSet(),
            "Synchronizing event B must not overwrite or delete event A's logical peer."
        )
    }

    @Test
    fun `legacy and mixed slot migration rewrites all references in one rollback safe migration`() {
        val migration = listOf(
            File("src/commonMain/sqldelight/com/guyghost/wakeve/migrations/13.sqm"),
            File("shared/src/commonMain/sqldelight/com/guyghost/wakeve/migrations/13.sqm")
        ).firstOrNull(File::isFile)

        assertTrue(migration != null, "A durable event-scoped slot migration must follow schema 12.")
        val source = migration!!.readText()
        for (requiredReference in listOf("timeSlot", "vote", "confirmedDate", "pollBallotReceipt", "syncMetadata")) {
            assertTrue(
                source.contains(requiredReference, ignoreCase = true),
                "Migration 13 must rewrite $requiredReference references atomically."
            )
        }
        assertTrue(source.contains("slot:v1"), "Physical ids must use the reviewed injective v1 encoding.")
        assertTrue(
            source.contains("ROLLBACK", ignoreCase = true) ||
                source.contains("RAISE(ROLLBACK", ignoreCase = true),
            "A collision or missing reference must abort the entire mixed migration."
        )
    }

    @Test
    fun `migration 13 treats a legacy logical slot beginning with the v1 prefix as raw data`() {
        val url = "jdbc:sqlite:${Files.createTempFile("wakeve-slot-v13-prefix-", ".db")}" 
        val driver = JdbcSqliteDriver(url)
        createVersion13SlotFixture(driver, logicalSlotId = "slot:v1|raw", danglingVote = false)

        WakeveDb.Schema.migrate(driver, oldVersion = 13, newVersion = 14)

        DriverManager.getConnection(url).use { connection ->
            val physicalId = text(connection, "SELECT id FROM timeSlot")
            assertEquals(
                TimeSlotStorageIdentity.physicalId("event-prefix", "slot:v1|raw"),
                physicalId,
                "A legacy logical ID is not already encoded merely because its text starts with slot:v1|."
            )
            assertEquals("slot:v1|raw", TimeSlotStorageIdentity.decode(physicalId)?.logicalSlotId)
            assertEquals(physicalId, text(connection, "SELECT timeslotId FROM vote"))
            assertEquals(physicalId, text(connection, "SELECT timeslotId FROM confirmedDate"))
            assertEquals("MIGRATION_COMPLETE", text(connection, "SELECT status FROM slotIdentityMigration"))
        }
        driver.close()
    }

    @Test
    fun `migration 13 rolls back references and completion marker when a slot reference dangles`() {
        val url = "jdbc:sqlite:${Files.createTempFile("wakeve-slot-v13-rollback-", ".db")}" 
        val driver = JdbcSqliteDriver(url)
        createVersion13SlotFixture(driver, logicalSlotId = "legacy-slot", danglingVote = true)

        val outcome = runCatching {
            WakeveDb.Schema.migrate(driver, oldVersion = 13, newVersion = 14)
        }

        assertTrue(outcome.isFailure, "A dangling slot reference must abort migration 13.")
        DriverManager.getConnection(url).use { connection ->
            assertEquals("legacy-slot", text(connection, "SELECT id FROM timeSlot"))
            assertEquals("missing-slot", text(connection, "SELECT timeslotId FROM vote"))
            assertEquals(
                0,
                connection.prepareStatement(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'slotIdentityMigration'"
                ).use { statement -> statement.executeQuery().use { rows -> rows.next(); rows.getInt(1) } },
                "The migration-complete marker must roll back with every rewritten reference."
            )
        }
        driver.close()
    }

    private fun createVersion13SlotFixture(
        driver: JdbcSqliteDriver,
        logicalSlotId: String,
        danglingVote: Boolean
    ) {
        listOf(
            "CREATE TABLE timeSlot(id TEXT PRIMARY KEY NOT NULL, eventId TEXT NOT NULL)",
            "CREATE TABLE vote(id TEXT PRIMARY KEY NOT NULL, timeslotId TEXT NOT NULL)",
            "CREATE TABLE confirmedDate(id TEXT PRIMARY KEY NOT NULL, timeslotId TEXT NOT NULL)",
            "CREATE TABLE confirmation_effect_outbox(id TEXT PRIMARY KEY NOT NULL, slotId TEXT NOT NULL)",
            "CREATE TABLE scenario(id TEXT PRIMARY KEY NOT NULL, sourceTimeSlotId TEXT)",
            "CREATE TABLE syncMetadata(id TEXT PRIMARY KEY NOT NULL, entityType TEXT NOT NULL, entityId TEXT NOT NULL, operation TEXT NOT NULL, payload TEXT NOT NULL DEFAULT '{}', timestamp TEXT NOT NULL, retryState TEXT NOT NULL DEFAULT 'READY', retryCount INTEGER NOT NULL DEFAULT 0, synced INTEGER NOT NULL DEFAULT 0)"
        ).forEach { driver.execute(null, it, 0).value }
        driver.execute(null, "INSERT INTO timeSlot(id, eventId) VALUES (?, 'event-prefix')", 1) {
            bindString(0, logicalSlotId)
        }.value
        driver.execute(null, "INSERT INTO vote(id, timeslotId) VALUES ('vote-1', ?)", 1) {
            bindString(0, if (danglingVote) "missing-slot" else logicalSlotId)
        }.value
        driver.execute(null, "INSERT INTO confirmedDate(id, timeslotId) VALUES ('confirmed-1', ?)", 1) {
            bindString(0, logicalSlotId)
        }.value
        driver.execute(null, "INSERT INTO confirmation_effect_outbox(id, slotId) VALUES ('outbox-1', ?)", 1) {
            bindString(0, logicalSlotId)
        }.value
    }

    private fun text(connection: java.sql.Connection, sql: String): String =
        connection.prepareStatement(sql).use { statement ->
            statement.executeQuery().use { rows -> rows.next(); rows.getString(1) }
        }

    private suspend fun persistPollingEvent(
        repository: DatabaseEventRepository,
        eventId: String,
        organizerId: String,
        slotId: String
    ): Event {
        repository.createEvent(
            Event(
                id = eventId,
                title = eventId,
                description = "Event-scoped slot identity",
                organizerId = organizerId,
                proposedSlots = listOf(slot(slotId, "2099-10-01T10:00:00Z")),
                deadline = DEADLINE,
                status = EventStatus.POLLING,
                createdAt = NOW,
                updatedAt = NOW
            )
        ).getOrThrow()
        return requireNotNull(repository.getEvent(eventId))
    }

    private suspend fun commitOneSlotBallot(
        repository: DatabaseEventRepository,
        event: Event,
        operationId: String
    ) {
        assertIs<PollBallotContract.CommitResult.Committed>(
            repository.commitCompleteBallot(
                PollBallotContract.CommitCompleteBallotCommand(
                    eventId = event.id,
                    actorId = event.organizerId,
                    pollRevision = event.aggregateRevision,
                    entries = listOf(PollBallotContract.BallotEntry(SHARED_SLOT_ID, Vote.YES)),
                    operationId = operationId
                )
            )
        )
    }

    private fun confirmationCommand(event: Event, operationId: String) =
        EventManagementContract.ConfirmPollDateCommand(
            operationId = operationId,
            eventId = event.id,
            slotId = SHARED_SLOT_ID,
            actorId = event.organizerId,
            requestedAt = Instant.parse(NOW)
        )

    private fun slot(id: String, start: String) = TimeSlot(
        id = id,
        start = start,
        end = null,
        timezone = "UTC",
        timeOfDay = TimeOfDay.SPECIFIC
    )

    private companion object {
        const val SHARED_SLOT_ID = "shared-slot"
        const val NOW = "2026-08-28T12:00:00Z"
        const val DEADLINE = "2099-09-30T12:00:00Z"
    }
}
