package com.guyghost.wakeve.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.poll.PollBallotContract
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DatabaseEventRepositoryAtomicBallotBlockerTest {

    @Test
    fun `complete ballot and receipt commit atomically or no vote is written`() = runBlocking {
        val fixture = fixture()
        val event = fixture.seedPollingEvent("event-atomic-ballot")
        val failingPhysicalSlotId = TimeSlotStorageIdentity.physicalId(event.id, "slot-2")
        fixture.driver.execute(
            identifier = null,
            sql = """
                CREATE TRIGGER fail_second_ballot_vote
                BEFORE INSERT ON vote
                WHEN NEW.timeslotId = '$failingPhysicalSlotId'
                BEGIN
                    SELECT RAISE(ABORT, 'forced second vote failure');
                END
            """.trimIndent(),
            parameters = 0
        ).value

        val result = fixture.repository.commitCompleteBallot(command(event, operationId = "operation-atomic"))

        val rejected = assertIs<PollBallotContract.CommitResult.Rejected>(result)
        assertEquals(PollBallotContract.FailureCode.LOCAL_TRANSACTION_FAILED, rejected.failure.code)
        assertTrue(fixture.repository.getPoll(event.id)?.votes?.get(event.organizerId).orEmpty().isEmpty())
        assertFalse(fixture.repository.hasCompleteBallot(event.id, event.organizerId))
    }

    @Test
    fun `complete ballot requires exact current slot coverage`() = runBlocking {
        val fixture = fixture()
        val event = fixture.seedPollingEvent("event-ballot-coverage")

        val missing = fixture.repository.commitCompleteBallot(
            command(event, entries = listOf(entry("slot-1", Vote.YES)), operationId = "operation-missing")
        )
        val duplicate = fixture.repository.commitCompleteBallot(
            command(
                event,
                entries = listOf(
                    entry("slot-1", Vote.YES),
                    entry("slot-1", Vote.NO),
                    entry("slot-2", Vote.MAYBE)
                ),
                operationId = "operation-duplicate"
            )
        )
        val unknown = fixture.repository.commitCompleteBallot(
            command(
                event,
                entries = listOf(entry("slot-1", Vote.YES), entry("slot-unknown", Vote.NO)),
                operationId = "operation-unknown"
            )
        )

        assertFailureCode(missing, PollBallotContract.FailureCode.INCOMPLETE_BALLOT)
        assertFailureCode(duplicate, PollBallotContract.FailureCode.DUPLICATE_SLOT)
        assertFailureCode(unknown, PollBallotContract.FailureCode.UNKNOWN_SLOT)
        assertTrue(fixture.repository.getPoll(event.id)?.votes?.get(event.organizerId).orEmpty().isEmpty())
        assertFalse(fixture.repository.hasCompleteBallot(event.id, event.organizerId))
    }

    @Test
    fun `legacy partial rows are never projected as a complete ballot`() = runBlocking {
        val fixture = fixture()
        val event = fixture.seedPollingEvent("event-legacy-partial")
        assertTrue(fixture.repository.addVote(event.id, event.organizerId, "slot-1", Vote.YES).isSuccess)

        assertEquals(mapOf("slot-1" to Vote.YES), fixture.repository.getPoll(event.id)?.votes?.get(event.organizerId))
        assertFalse(
            fixture.repository.hasCompleteBallot(event.id, event.organizerId),
            "One legacy row cannot make a two-slot ballot complete."
        )
    }

    @Test
    fun `repository rechecks permission status revision and deadline inside the command`() = runBlocking {
        suspend fun rejected(
            id: String,
            nowIso: String = "2026-08-28T11:00:00Z",
            eventTransform: (Event) -> Event = { it },
            commandTransform: (PollBallotContract.CommitCompleteBallotCommand) -> PollBallotContract.CommitCompleteBallotCommand = { it }
        ): PollBallotContract.FailureCode {
            val fixture = fixture(nowIso)
            val event = fixture.seedPollingEvent(id, eventTransform)
            val result = fixture.repository.commitCompleteBallot(commandTransform(command(event)))
            assertTrue(fixture.repository.getPoll(event.id)?.votes?.get(event.organizerId).orEmpty().isEmpty())
            return assertIs<PollBallotContract.CommitResult.Rejected>(result).failure.code
        }

        assertEquals(
            PollBallotContract.FailureCode.FORBIDDEN,
            rejected(
                "event-forbidden",
                commandTransform = { it.copy(actorId = "not-a-participant") }
            )
        )
        assertEquals(
            PollBallotContract.FailureCode.INVALID_EVENT_STATUS,
            rejected("event-status", eventTransform = { it.copy(status = EventStatus.DRAFT) })
        )
        assertEquals(
            PollBallotContract.FailureCode.POLL_REVISION_CONFLICT,
            rejected("event-revision", commandTransform = { it.copy(pollRevision = it.pollRevision + 1) })
        )
        assertEquals(
            PollBallotContract.FailureCode.DEADLINE_REACHED,
            rejected("event-at-deadline", nowIso = "2026-08-28T12:00:00Z")
        )
        assertEquals(
            PollBallotContract.FailureCode.DEADLINE_REACHED,
            rejected("event-after-deadline", nowIso = "2026-08-28T12:00:00.001Z")
        )
        assertEquals(
            PollBallotContract.FailureCode.INVALID_DEADLINE_ISO,
            rejected(
                "event-invalid-deadline",
                eventTransform = { it.copy(deadline = "not-an-iso-instant") }
            )
        )
    }

    @Test
    fun `same operation and payload returns one receipt while changed payload conflicts`() = runBlocking {
        val fixture = fixture()
        val event = fixture.seedPollingEvent("event-idempotent-ballot")
        val firstCommand = command(event, operationId = "operation-stable")

        val first = assertIs<PollBallotContract.CommitResult.Committed>(
            fixture.repository.commitCompleteBallot(firstCommand)
        )
        val replay = assertIs<PollBallotContract.CommitResult.AlreadyCommitted>(
            fixture.repository.commitCompleteBallot(firstCommand)
        )
        val conflict = fixture.repository.commitCompleteBallot(
            firstCommand.copy(
                entries = listOf(entry("slot-1", Vote.NO), entry("slot-2", Vote.MAYBE))
            )
        )

        assertEquals(first.receipt, replay.receipt)
        assertEquals("operation-stable", first.receipt.operationId)
        assertEquals(PollBallotContract.SyncStatus.LOCAL_PENDING, first.receipt.syncStatus)
        assertFailureCode(conflict, PollBallotContract.FailureCode.IDEMPOTENCY_CONFLICT)
        assertEquals(
            mapOf("slot-1" to Vote.YES, "slot-2" to Vote.MAYBE),
            fixture.repository.getPoll(event.id)?.votes?.get(event.organizerId)
        )
        assertTrue(fixture.repository.hasCompleteBallot(event.id, event.organizerId))
    }

    @Test
    fun `corrupt durable receipt is terminal repository inconsistency with unknown commit outcome`() = runBlocking {
        val fixture = fixture()
        val event = fixture.seedPollingEvent("event-corrupt-durable-receipt")
        val submitted = command(event, operationId = "operation-corrupt-receipt")
        assertIs<PollBallotContract.CommitResult.Committed>(
            fixture.repository.commitCompleteBallot(submitted)
        )
        fixture.driver.execute(
            identifier = null,
            sql = "UPDATE pollBallotReceipt SET syncPayload = '{malformed' " +
                "WHERE operationId = 'operation-corrupt-receipt'",
            parameters = 0
        ).value

        val rejected = assertIs<PollBallotContract.CommitResult.Rejected>(
            fixture.repository.commitCompleteBallot(submitted)
        )
        assertEquals(PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT, rejected.failure.code)
        assertFalse(rejected.failure.retryable)
        assertEquals(
            PollBallotContract.CommitOutcome.UNKNOWN,
            rejected.failure.commitOutcome,
            "A corrupt durable receipt proves repository inconsistency, but cannot prove the transaction did not commit."
        )
    }

    @Test
    fun `vote access admits the exact organizer and accepted member only`() = runBlocking {
        val organizerFixture = fixture()
        val organizerEvent = organizerFixture.seedPollingEvent("event-organizer-access")
        assertIs<PollBallotContract.CommitResult.Committed>(
            organizerFixture.repository.commitCompleteBallot(command(organizerEvent))
        )

        val acceptedFixture = fixture()
        val acceptedEvent = acceptedFixture.seedPollingEvent("event-accepted-access")
        acceptedFixture.insertMember(acceptedEvent.id, "accepted-member", "ACCEPTED")
        assertIs<PollBallotContract.CommitResult.Committed>(
            acceptedFixture.repository.commitCompleteBallot(
                command(acceptedEvent).copy(actorId = "accepted-member")
            )
        )

        val forgedOrganizerFixture = fixture()
        val forgedOrganizerEvent = forgedOrganizerFixture.seedPollingEvent("event-forged-organizer")
        forgedOrganizerFixture.insertMember(
            forgedOrganizerEvent.id,
            userId = "forged-organizer",
            rsvpState = "NOT_APPLICABLE",
            role = "ORGANIZER"
        )
        assertFailureCode(
            forgedOrganizerFixture.repository.commitCompleteBallot(
                command(forgedOrganizerEvent).copy(actorId = "forged-organizer")
            ),
            PollBallotContract.FailureCode.FORBIDDEN
        )
    }

    @Test
    fun `pending declined unavailable and non member cannot commit a ballot`() = runBlocking {
        for (rsvp in listOf("PENDING", "DECLINED", "UNAVAILABLE")) {
            val fixture = fixture()
            val actorId = "${rsvp.lowercase()}-member"
            val event = fixture.seedPollingEvent("event-${rsvp.lowercase()}-access")
            fixture.insertMember(event.id, actorId, rsvp)

            assertFailureCode(
                fixture.repository.commitCompleteBallot(command(event).copy(actorId = actorId)),
                PollBallotContract.FailureCode.FORBIDDEN
            )
            assertFalse(fixture.repository.hasCompleteBallot(event.id, actorId))
        }

        val fixture = fixture()
        val event = fixture.seedPollingEvent("event-non-member-access")
        assertFailureCode(
            fixture.repository.commitCompleteBallot(command(event).copy(actorId = "non-member")),
            PollBallotContract.FailureCode.FORBIDDEN
        )
    }

    @Test
    fun `operation id is scoped by event actor revision tuple`() = runBlocking {
        val actorFixture = fixture()
        val actorEvent = actorFixture.seedPollingEvent("event-operation-actor-scope")
        actorFixture.insertMember(actorEvent.id, "accepted-member", "ACCEPTED")

        assertIs<PollBallotContract.CommitResult.Committed>(
            actorFixture.repository.commitCompleteBallot(
                command(actorEvent, operationId = "shared-operation")
            )
        )
        assertIs<PollBallotContract.CommitResult.Committed>(
            actorFixture.repository.commitCompleteBallot(
                command(actorEvent, operationId = "shared-operation").copy(actorId = "accepted-member")
            )
        )

        val eventFixture = fixture()
        val firstEvent = eventFixture.seedPollingEvent("event-operation-scope-a")
        val secondEvent = eventFixture.seedPollingEvent("event-operation-scope-b")
        assertIs<PollBallotContract.CommitResult.Committed>(
            eventFixture.repository.commitCompleteBallot(
                command(firstEvent, operationId = "shared-event-operation")
            )
        )
        assertIs<PollBallotContract.CommitResult.Committed>(
            eventFixture.repository.commitCompleteBallot(
                command(secondEvent, operationId = "shared-event-operation")
            )
        )
        Unit
    }

    @Test
    fun `same tuple with changed payload is an idempotency conflict`() = runBlocking {
        val fixture = fixture()
        val event = fixture.seedPollingEvent("event-tuple-payload-conflict")
        val original = command(event, operationId = "tuple-operation")
        assertIs<PollBallotContract.CommitResult.Committed>(
            fixture.repository.commitCompleteBallot(original)
        )

        assertFailureCode(
            fixture.repository.commitCompleteBallot(
                original.copy(entries = listOf(entry("slot-1", Vote.NO), entry("slot-2", Vote.YES)))
            ),
            PollBallotContract.FailureCode.IDEMPOTENCY_CONFLICT
        )
    }

    @Test
    fun `fingerprint matches model golden vectors byte for byte`() {
        assertEquals(
            "v1|612f623f633d642678=YES|737061636520736c6f74=NO",
            PollBallotContract.fingerprint(
                listOf(entry("space slot", Vote.NO), entry("a/b?c=d&x", Vote.YES))
            )
        )
        assertEquals(
            "v1|c3a9=MAYBE|e69db1e4baac=YES|f09f8c8d=NO",
            PollBallotContract.fingerprint(
                listOf(entry("🌍", Vote.NO), entry("東京", Vote.YES), entry("é", Vote.MAYBE))
            )
        )
    }

    @Test
    fun `negative and non interoperable poll revisions are rejected explicitly`() = runBlocking {
        for (revision in listOf(-1L, 9_007_199_254_740_992L, Long.MAX_VALUE)) {
            val fixture = fixture()
            val event = fixture.seedPollingEvent("event-invalid-revision-$revision")
            val rejected = assertIs<PollBallotContract.CommitResult.Rejected>(
                fixture.repository.commitCompleteBallot(command(event).copy(pollRevision = revision))
            )
            assertEquals("INVALID_POLL_REVISION", rejected.failure.code.name)
            assertFalse(fixture.repository.hasCompleteBallot(event.id, event.organizerId))
        }
    }

    @Test
    fun `independent repositories racing on one tuple converge without generic transaction failure`() = runBlocking {
        val url = "jdbc:sqlite:${Files.createTempFile("wakeve-ballot-race-", ".db")}" 
        val schemaDriver = JdbcSqliteDriver(url)
        WakeveDb.Schema.create(schemaDriver)
        schemaDriver.close()

        val firstDriver = JdbcSqliteDriver(url)
        val secondDriver = JdbcSqliteDriver(url)
        firstDriver.execute(null, "PRAGMA busy_timeout = 5000", 0).value
        secondDriver.execute(null, "PRAGMA busy_timeout = 5000", 0).value
        val firstDatabase = WakeveDb(firstDriver)
        val first = Fixture(
            firstDriver,
            firstDatabase,
            DatabaseEventRepository(
                firstDatabase,
                ConfirmationClock { Instant.parse("2026-08-28T11:00:00Z") }
            )
        )
        val secondRepository = DatabaseEventRepository(
            WakeveDb(secondDriver),
            ConfirmationClock { Instant.parse("2026-08-28T11:00:00Z") }
        )
        val event = first.seedPollingEvent("event-concurrent-ballot")
        val racedCommand = command(event, operationId = "operation-concurrent-ballot")

        val results = coroutineScope {
            val ready = CompletableDeferred<Unit>()
            val secondReady = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            listOf(
                async(Dispatchers.Default) {
                    ready.complete(Unit)
                    release.await()
                    first.repository.commitCompleteBallot(racedCommand)
                },
                async(Dispatchers.Default) {
                    secondReady.complete(Unit)
                    release.await()
                    secondRepository.commitCompleteBallot(racedCommand)
                }
            ).also {
                ready.await()
                secondReady.await()
                release.complete(Unit)
            }.awaitAll()
        }

        assertTrue(
            results.all {
                it is PollBallotContract.CommitResult.Committed ||
                    it is PollBallotContract.CommitResult.AlreadyCommitted
            },
            "A unique-key race must re-read the tuple receipt, never collapse to LOCAL_TRANSACTION_FAILED: $results"
        )
        val receipts = results.map {
            when (it) {
                is PollBallotContract.CommitResult.Committed -> it.receipt
                is PollBallotContract.CommitResult.AlreadyCommitted -> it.receipt
                else -> error("result was asserted as committed")
            }
        }
        assertEquals(1, receipts.map { it.receiptId }.toSet().size)
        firstDriver.close()
        secondDriver.close()
    }

    private fun assertFailureCode(
        result: PollBallotContract.CommitResult,
        expected: PollBallotContract.FailureCode
    ) {
        assertEquals(expected, assertIs<PollBallotContract.CommitResult.Rejected>(result).failure.code)
    }

    private fun command(
        event: Event,
        entries: List<PollBallotContract.BallotEntry> = listOf(
            entry("slot-1", Vote.YES),
            entry("slot-2", Vote.MAYBE)
        ),
        operationId: String = "operation-1"
    ) = PollBallotContract.CommitCompleteBallotCommand(
        eventId = event.id,
        actorId = event.organizerId,
        pollRevision = event.aggregateRevision,
        entries = entries,
        operationId = operationId
    )

    private fun entry(slotId: String, vote: Vote) = PollBallotContract.BallotEntry(slotId, vote)

    private fun fixture(nowIso: String = "2026-08-28T11:00:00Z"): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
        WakeveDb.Schema.create(driver)
        val database = WakeveDb(driver)
        return Fixture(
            driver = driver,
            database = database,
            repository = DatabaseEventRepository(
                database,
                ConfirmationClock { Instant.parse(nowIso) }
            )
        )
    }

    private class Fixture(
        val driver: SqlDriver,
        val database: WakeveDb,
        val repository: DatabaseEventRepository
    ) {
        fun insertMember(
            eventId: String,
            userId: String,
            rsvpState: String,
            role: String = "PARTICIPANT"
        ) {
            database.participantQueries.insertParticipantWithAxes(
                id = "member-$eventId-$userId",
                eventId = eventId,
                userId = userId,
                role = role,
                hasValidatedDate = 0,
                rsvpState = rsvpState,
                dateValidationState = "NOT_VALIDATED",
                joinedAt = "2026-08-28T10:00:00Z",
                updatedAt = "2026-08-28T10:00:00Z"
            )
        }

        suspend fun seedPollingEvent(
            id: String,
            transform: (Event) -> Event = { it }
        ): Event {
            val event = transform(
                createTestEvent(
                    id = id,
                    organizerId = "participant-1",
                    proposedSlots = listOf(
                        createTestTimeSlot(id = "slot-1", start = "2026-09-01T10:00:00Z"),
                        createTestTimeSlot(id = "slot-2", start = "2026-09-02T10:00:00Z")
                    ),
                    deadline = "2026-08-28T12:00:00Z",
                    status = EventStatus.POLLING
                )
            )
            assertTrue(repository.createEvent(event).isSuccess)
            return requireNotNull(repository.getEvent(event.id))
        }
    }
}
