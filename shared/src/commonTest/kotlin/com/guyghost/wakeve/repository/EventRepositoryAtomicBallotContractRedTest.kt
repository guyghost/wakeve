package com.guyghost.wakeve.repository

import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.poll.PollBallotContract
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EventRepositoryAtomicBallotContractRedTest {

    @Test
    fun `in memory ballot uses the injected real clock and strict deadline boundary`() = runTest {
        val before = fixture("2026-08-28T11:59:59.999Z")
        assertIs<PollBallotContract.CommitResult.Committed>(
            before.repository.commitCompleteBallot(command(before.event))
        )

        for (now in listOf("2026-08-28T12:00:00Z", "2026-08-28T12:00:00.001Z")) {
            val closed = fixture(now)
            val rejected = assertIs<PollBallotContract.CommitResult.Rejected>(
                closed.repository.commitCompleteBallot(command(closed.event))
            )
            assertEquals(PollBallotContract.FailureCode.DEADLINE_REACHED, rejected.failure.code)
            assertFalse(closed.repository.hasCompleteBallot(closed.event.id, closed.event.organizerId))
        }
    }

    @Test
    fun `in memory ballot invalid deadline fails closed with typed failure`() = runTest {
        val fixture = fixture(
            nowIso = "2026-08-28T11:00:00Z",
            deadline = "not-an-iso-instant"
        )

        val rejected = assertIs<PollBallotContract.CommitResult.Rejected>(
            fixture.repository.commitCompleteBallot(command(fixture.event))
        )

        assertEquals("INVALID_DEADLINE_ISO", rejected.failure.code.name)
        assertFalse(fixture.repository.hasCompleteBallot(fixture.event.id, fixture.event.organizerId))
    }

    @Test
    fun `in memory concurrent retries share one receipt under a mutex`() = runTest {
        val fixture = fixture("2026-08-28T11:00:00Z")
        val command = command(fixture.event, operationId = "operation-concurrent-memory")

        val results = coroutineScope {
            List(32) {
                async(Dispatchers.Default) {
                    fixture.repository.commitCompleteBallot(command)
                }
            }.awaitAll()
        }

        assertEquals(1, results.count { it is PollBallotContract.CommitResult.Committed })
        assertEquals(31, results.count { it is PollBallotContract.CommitResult.AlreadyCommitted })
        val receiptIds = results.map {
            when (it) {
                is PollBallotContract.CommitResult.Committed -> it.receipt.receiptId
                is PollBallotContract.CommitResult.AlreadyCommitted -> it.receipt.receiptId
                else -> error("Concurrent retry escaped idempotent receipt resolution: $it")
            }
        }
        assertEquals(1, receiptIds.toSet().size)
        assertTrue(fixture.repository.hasCompleteBallot(fixture.event.id, fixture.event.organizerId))
    }

    private suspend fun fixture(
        nowIso: String,
        deadline: String = "2026-08-28T12:00:00Z"
    ): Fixture {
        val repository = EventRepository(
            confirmationClock = ConfirmationClock { Instant.parse(nowIso) }
        )
        val event = createTestEvent(
            id = "memory-ballot-${nowIso.hashCode()}-${deadline.hashCode()}",
            organizerId = "memory-organizer",
            proposedSlots = listOf(
                createTestTimeSlot("slot-1", "2026-09-01T10:00:00Z"),
                createTestTimeSlot("slot-2", "2026-09-02T10:00:00Z")
            ),
            deadline = deadline,
            status = EventStatus.POLLING
        )
        repository.createEvent(event).getOrThrow()
        return Fixture(repository, event)
    }

    private fun command(
        event: com.guyghost.wakeve.models.Event,
        operationId: String = "memory-operation"
    ) = PollBallotContract.CommitCompleteBallotCommand(
        eventId = event.id,
        actorId = event.organizerId,
        pollRevision = event.aggregateRevision,
        entries = listOf(
            PollBallotContract.BallotEntry("slot-1", Vote.YES),
            PollBallotContract.BallotEntry("slot-2", Vote.MAYBE)
        ),
        operationId = operationId
    )

    private data class Fixture(
        val repository: EventRepository,
        val event: com.guyghost.wakeve.models.Event
    )
}
