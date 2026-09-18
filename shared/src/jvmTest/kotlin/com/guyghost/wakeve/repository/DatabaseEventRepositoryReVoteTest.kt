package com.guyghost.wakeve.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for BUG-1 (QA session 2026-09-18): re-voting on the same
 * slot must UPDATE the existing vote instead of failing on the
 * UNIQUE(timeslotId, participantId) constraint.
 */
class DatabaseEventRepositoryReVoteTest {

    private lateinit var db: WakeveDb
    private lateinit var repository: DatabaseEventRepository

    @BeforeTest
    fun setup() {
        val jdbcUrl = "jdbc:sqlite:${Files.createTempFile("wakeve-revote-", ".db")}"
        val driver = JdbcSqliteDriver(jdbcUrl)
        WakeveDb.Schema.create(driver)
        db = WakeveDb(driver)
        repository = DatabaseEventRepository(db)
    }

    @Test
    fun `re-voting on the same slot updates the existing vote`() = runBlocking {
        val event = createTestEvent(
            id = "event-revote",
            organizerId = "user-1",
            participants = listOf("user-1"),
            proposedSlots = listOf(createTestTimeSlot(id = "slot-1")),
            status = EventStatus.POLLING,
            deadline = "2099-01-01T00:00:00Z"
        )
        assertTrue(repository.createEvent(event).isSuccess)

        val firstVote = repository.addVote(event.id, "user-1", "slot-1", Vote.YES)
        assertTrue(firstVote.isSuccess, "first vote should succeed")

        val revote = repository.addVote(event.id, "user-1", "slot-1", Vote.MAYBE)
        assertTrue(revote.isSuccess, "re-vote must update the existing row, not violate the UNIQUE constraint")

        val poll = repository.getPoll(event.id)
        assertEquals(Vote.MAYBE, poll?.votes?.get("user-1")?.get("slot-1"))
    }

    @Test
    fun `re-voting keeps a single vote row per slot and participant`() = runBlocking {
        val event = createTestEvent(
            id = "event-revote-single-row",
            organizerId = "user-1",
            participants = listOf("user-1"),
            proposedSlots = listOf(createTestTimeSlot(id = "slot-1")),
            status = EventStatus.POLLING,
            deadline = "2099-01-01T00:00:00Z"
        )
        assertTrue(repository.createEvent(event).isSuccess)

        repository.addVote(event.id, "user-1", "slot-1", Vote.YES)
        repository.addVote(event.id, "user-1", "slot-1", Vote.NO)
        repository.addVote(event.id, "user-1", "slot-1", Vote.YES)

        val persistedSlotId = TimeSlotStorageIdentity.physicalId(event.id, "slot-1")
        val organizerRecord = db.participantQueries
            .selectByEventIdAndUserId(event.id, "user-1")
            .executeAsOne()
        val rows = db.voteQueries
            .selectByTimeslotAndParticipant(persistedSlotId, organizerRecord.id)
            .executeAsList()
        assertEquals(1, rows.size, "exactly one vote row must exist per (slot, participant)")
        assertEquals(Vote.YES.name, rows.single().vote)
    }
}
