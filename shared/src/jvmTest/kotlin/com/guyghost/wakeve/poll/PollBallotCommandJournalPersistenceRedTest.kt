package com.guyghost.wakeve.poll

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.repository.DatabaseEventRepository
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Persistence specification for the durable pre-dispatch ballot journal.
 *
 * The runtime API does not exist yet, so these tests pin the minimum durable schema and
 * transition invariants at the database boundary. They intentionally remain RED until the
 * journal is part of WakeveDb and owned by the ballot orchestration layer.
 */
class PollBallotCommandJournalPersistenceRedTest {

    @Test
    fun `journal persists the three modeled statuses across restart`() {
        withJournalDatabase("statuses") { connection ->
            insertJournal(connection, "key-staged", "event-1", "actor-1", "STAGED_NOT_DISPATCHED")
            insertJournal(connection, "key-dispatched", "event-1", "actor-1", "DISPATCHED")
            insertJournal(connection, "key-cancelled", "event-1", "actor-1", "CANCELLED")
        }.also { url ->
            DriverManager.getConnection(url).use { connection ->
                assertEquals(
                    listOf("CANCELLED", "DISPATCHED", "STAGED_NOT_DISPATCHED"),
                    connection.prepareStatement(
                        "SELECT status FROM $TABLE ORDER BY status"
                    ).use { statement ->
                        statement.executeQuery().use { rows ->
                            buildList { while (rows.next()) add(rows.getString("status")) }
                        }
                    }
                )
                assertFails("Free-text journal states must fail closed") {
                    insertJournal(connection, "key-invalid", "event-1", "actor-1", "ACKED_MAYBE")
                }
            }
        }
    }

    @Test
    fun `only dispatched commands are rehydratable for unknown outcome resolution`() {
        withJournalDatabase("rehydrate") { connection ->
            insertJournal(connection, "key-staged", "event-1", "actor-1", "STAGED_NOT_DISPATCHED")
            insertJournal(connection, "key-dispatched", "event-1", "actor-1", "DISPATCHED")
            insertJournal(connection, "key-cancelled", "event-1", "actor-1", "CANCELLED")

            val dispatchable = selectDispatchable(connection, "event-1", "actor-1")
            assertEquals(listOf("key-dispatched"), dispatchable)
        }
    }

    @Test
    fun `rehydration is correlated to the exact event and actor subject`() {
        withJournalDatabase("subject") { connection ->
            insertJournal(connection, "key-target", "event-target", "actor-target", "DISPATCHED")
            insertJournal(connection, "key-other-event", "event-other", "actor-target", "DISPATCHED")
            insertJournal(connection, "key-other-actor", "event-target", "actor-other", "DISPATCHED")

            assertEquals(
                listOf("key-target"),
                selectDispatchable(connection, "event-target", "actor-target")
            )
        }
    }

    @Test
    fun `cancelled command cannot be dispatched by a late acknowledgement`() {
        withJournalDatabase("cancel-late-ack") { connection ->
            insertJournal(connection, "key-cancel", "event-1", "actor-1", "STAGED_NOT_DISPATCHED")
            connection.prepareStatement(
                "UPDATE $TABLE SET status = 'CANCELLED' WHERE operationKey = ? AND status = 'STAGED_NOT_DISPATCHED'"
            ).use { statement ->
                statement.setString(1, "key-cancel")
                assertEquals(1, statement.executeUpdate())
            }

            // This is the durable equivalent of a COMMAND_DISPATCH_MARKED callback arriving
            // after cancellation. It must never resurrect the command.
            connection.prepareStatement(
                "UPDATE $TABLE SET status = 'DISPATCHED' WHERE operationKey = ? AND status = 'STAGED_NOT_DISPATCHED'"
            ).use { statement ->
                statement.setString(1, "key-cancel")
                assertEquals(0, statement.executeUpdate())
            }
            assertTrue(selectDispatchable(connection, "event-1", "actor-1").isEmpty())
        }
    }

    @Test
    fun `malformed dispatched row remains visible as terminal repository inconsistency`() {
        val url = withJournalDatabase("malformed-dispatched") { connection ->
            insertJournal(connection, "key-malformed", "event-1", "actor-1", "DISPATCHED")
            connection.prepareStatement(
                "UPDATE $TABLE SET commandPayload = '{malformed' WHERE operationKey = 'key-malformed'"
            ).use { statement -> assertEquals(1, statement.executeUpdate()) }
        }
        val driver = JdbcSqliteDriver(url)
        val journal = DatabaseBallotCommandJournal(WakeveDb(driver))

        val projections = journal.loadDispatched("event-1", "actor-1")

        assertEquals(
            1,
            projections.size,
            "Malformed durable rows are repository inconsistencies, not absent commands."
        )
        val projection: Any = projections.single()
        assertEquals("REPOSITORY_INCONSISTENT", reflectedValue(projection, "Code"))
        assertFalse(
            reflectedValue(projection, "Retryable") as? Boolean ?: true,
            "Malformed DISPATCHED is terminal and must not loop."
        )
        assertEquals("UNKNOWN", reflectedValue(projection, "CommitOutcome"))
        driver.close()
    }

    @Test
    fun `journal persists the complete receipt recorrelation tuple`() {
        withJournalDatabase("receipt-recorrelation") { connection ->
            val columns = connection.prepareStatement("PRAGMA table_info($TABLE)").use { statement ->
                statement.executeQuery().use { rows ->
                    buildSet { while (rows.next()) add(rows.getString("name")) }
                }
            }
            assertTrue(
                columns.containsAll(
                    setOf(
                        "operationKey", "eventId", "actorId", "pollRevision", "operationId",
                        "ballotFingerprint", "commandPayload", "status"
                    )
                ),
                "A late receipt must be re-correlated by the full durable operation tuple."
            )
        }
    }

    @Test
    fun `restarted dispatched command re correlates the complete durable receipt`() = runBlocking {
        val database = createFreshTestDatabase()
        val repository = DatabaseEventRepository(
            database,
            ConfirmationClock { Instant.parse("2026-08-28T11:00:00Z") }
        )
        repository.createEvent(
            Event(
                id = "journal-receipt-event",
                title = "Receipt correlation",
                description = "Full tuple",
                organizerId = "journal-receipt-actor",
                proposedSlots = listOf(
                    TimeSlot("slot-1", "2026-08-29T10:00:00Z", null, "UTC")
                ),
                deadline = "2026-08-28T12:00:00Z",
                status = EventStatus.POLLING,
                createdAt = "2026-08-28T10:00:00Z",
                updatedAt = "2026-08-28T10:00:00Z"
            )
        ).getOrThrow()
        val event = requireNotNull(repository.getEvent("journal-receipt-event"))
        val command = PollBallotContract.CommitCompleteBallotCommand(
            eventId = event.id,
            actorId = event.organizerId,
            pollRevision = event.aggregateRevision,
            entries = listOf(PollBallotContract.BallotEntry("slot-1", Vote.YES)),
            operationId = "journal-receipt-operation"
        )
        val journal = DatabaseBallotCommandJournal(database)
        assertIs<BallotJournalResult.Stored>(journal.stageCommand(command))
        assertIs<BallotJournalResult.Stored>(journal.markCommandDispatched(command))
        val committed = assertIs<PollBallotContract.CommitResult.Committed>(
            repository.commitCompleteBallot(command)
        )

        val restored = DatabaseBallotCommandJournal(database)
            .loadDispatchableCommands(event.id, event.organizerId)
            .single()
        val replay = assertIs<PollBallotContract.CommitResult.AlreadyCommitted>(
            repository.commitCompleteBallot(restored)
        )

        assertEquals(committed.receipt.receiptId, replay.receipt.receiptId)
        assertEquals(committed.receipt.eventId, replay.receipt.eventId)
        assertEquals(committed.receipt.actorId, replay.receipt.actorId)
        assertEquals(committed.receipt.pollRevision, replay.receipt.pollRevision)
        assertEquals(committed.receipt.operationId, replay.receipt.operationId)
        assertEquals(committed.receipt.ballotFingerprint, replay.receipt.ballotFingerprint)
    }

    @Test
    fun `journal rehydrates the immutable authoritative deadline after the event deadline changes`() = runBlocking {
        val database = createFreshTestDatabase()
        val repository = DatabaseEventRepository(database)
        val event = Event(
            id = "journal-deadline-event",
            title = "Authoritative deadline",
            description = "The dispatched command owns the reviewed deadline",
            organizerId = "journal-deadline-actor",
            proposedSlots = listOf(TimeSlot("slot-1", "2099-09-01T10:00:00Z", null, "UTC")),
            deadline = "2099-08-28T12:00:00Z",
            status = EventStatus.POLLING,
            createdAt = "2026-08-28T10:00:00Z",
            updatedAt = "2026-08-28T10:00:00Z"
        )
        repository.createEvent(event).getOrThrow()
        val persisted = requireNotNull(repository.getEvent(event.id))
        val command = PollBallotContract.CommitCompleteBallotCommand(
            eventId = event.id,
            actorId = event.organizerId,
            pollRevision = persisted.aggregateRevision,
            entries = listOf(PollBallotContract.BallotEntry("slot-1", Vote.YES)),
            operationId = "journal-deadline-operation"
        )
        val journal = DatabaseBallotCommandJournal(database)
        assertIs<BallotJournalResult.Stored>(journal.stageCommand(command))
        assertIs<BallotJournalResult.Stored>(journal.markCommandDispatched(command))

        repository.saveEvent(persisted.copy(deadline = "2099-08-29T12:00:00Z")).getOrThrow()
        val restored = journal.loadDispatchableCommands(event.id, event.organizerId).single()

        assertEquals(
            event.deadline,
            reflectedValue(restored, "AuthoritativeDeadlineIso"),
            "A mutable Event.deadline cannot rewrite the deadline reviewed and dispatched with the ballot."
        )
    }

    @Test
    fun `rehydration is a total union of staged dispatched cancelled and tombstoned rows`() = runBlocking {
        val database = createFreshTestDatabase()
        val repository = DatabaseEventRepository(database)
        val event = Event(
            id = "journal-total-event",
            title = "Total journal",
            description = "Every durable state stays diagnosable",
            organizerId = "journal-total-actor",
            proposedSlots = listOf(TimeSlot("slot-1", "2099-09-01T10:00:00Z", null, "UTC")),
            deadline = "2099-08-28T12:00:00Z",
            status = EventStatus.POLLING,
            createdAt = "2026-08-28T10:00:00Z",
            updatedAt = "2026-08-28T10:00:00Z"
        )
        repository.createEvent(event).getOrThrow()
        val revision = requireNotNull(repository.getEvent(event.id)).aggregateRevision
        val journal = DatabaseBallotCommandJournal(database)
        fun command(operationId: String) = PollBallotContract.CommitCompleteBallotCommand(
            eventId = event.id,
            actorId = event.organizerId,
            pollRevision = revision,
            entries = listOf(PollBallotContract.BallotEntry("slot-1", Vote.YES)),
            operationId = operationId
        )
        val staged = command("operation-staged")
        val dispatched = command("operation-dispatched")
        val cancelled = command("operation-cancelled")
        val tombstoned = command("operation-tombstoned")
        listOf(staged, dispatched, cancelled, tombstoned).forEach {
            assertIs<BallotJournalResult.Stored>(journal.stageCommand(it))
        }
        assertIs<BallotJournalResult.Stored>(journal.markCommandDispatched(dispatched))
        assertIs<BallotJournalResult.Stored>(journal.cancelCommand(cancelled))
        assertIs<BallotJournalResult.Stored>(journal.markCommandDispatched(tombstoned))
        assertIs<BallotJournalResult.Stored>(
            journal.tombstoneDispatchedCommand(
                tombstoned,
                PollBallotContract.BallotTerminalDestination(
                    kind = PollBallotContract.BallotTerminalDestinationKind.CANCELLED,
                    commitOutcome = PollBallotContract.CommitOutcome.UNKNOWN
                )
            )
        )

        val projections = journal.loadRehydrationProjections(event.id, event.organizerId)
        val statuses = projections.mapNotNull {
            (it as? BallotJournalProjection.Valid)?.record?.status
        }.toSet()

        assertEquals(4, projections.size, "No durable journal state may disappear from diagnostics.")
        assertEquals(PollBallotContract.BallotJournalStatus.entries.toSet(), statuses)
        val tombstone = projections.filterIsInstance<BallotJournalProjection.Valid>()
            .single { it.record.status == PollBallotContract.BallotJournalStatus.DISPATCH_CANCELLATION_TOMBSTONED }
        assertEquals(
            PollBallotContract.BallotTerminalDestinationKind.CANCELLED,
            tombstone.record.terminalDestination?.kind,
            "Terminal destinations must remain typed and discriminated after restart."
        )
    }

    @Test
    fun `dispatched cancellation tombstone persists one typed terminal destination`() {
        withJournalDatabase("dispatch-tombstone") { connection ->
            val columns = connection.prepareStatement("PRAGMA table_info($TABLE)").use { statement ->
                statement.executeQuery().use { rows ->
                    buildSet { while (rows.next()) add(rows.getString("name")) }
                }
            }
            assertTrue(
                "terminalDestination" in columns,
                "A dispatched cancellation cannot become terminal until its typed no-redispatch destination is durable."
            )
            insertJournal(connection, "key-tombstone", "event-1", "actor-1", "DISPATCHED")
            connection.prepareStatement(
                """
                    UPDATE $TABLE
                    SET status = 'DISPATCH_CANCELLATION_TOMBSTONED',
                        terminalDestination = '{"kind":"CANCELLED"}'
                    WHERE operationKey = 'key-tombstone' AND status = 'DISPATCHED'
                """.trimIndent()
            ).use { statement -> assertEquals(1, statement.executeUpdate()) }
            assertTrue(selectDispatchable(connection, "event-1", "actor-1").isEmpty())
        }
    }

    private fun withJournalDatabase(
        suffix: String,
        block: (Connection) -> Unit
    ): String {
        val url = "jdbc:sqlite:${Files.createTempFile("wakeve-ballot-journal-$suffix-", ".db")}" 
        val driver = JdbcSqliteDriver(url)
        WakeveDb.Schema.create(driver)
        driver.close()
        DriverManager.getConnection(url).use { connection ->
            assertTrue(tableExists(connection), "$TABLE must be provisioned by WakeveDb.Schema")
            val columns = connection.prepareStatement("PRAGMA table_info($TABLE)").use { statement ->
                statement.executeQuery().use { rows ->
                    buildSet { while (rows.next()) add(rows.getString("name")) }
                }
            }
            assertTrue(
                columns.containsAll(
                    setOf(
                        "operationKey", "eventId", "actorId", "pollRevision", "operationId",
                        "ballotFingerprint", "commandPayload", "status", "updatedAt"
                    )
                ),
                "Journal schema cannot rehydrate or correlate the exact modeled command: $columns"
            )
            block(connection)
        }
        return url
    }

    private fun tableExists(connection: Connection): Boolean = connection.prepareStatement(
        "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?"
    ).use { statement ->
        statement.setString(1, TABLE)
        statement.executeQuery().use { rows -> rows.next(); rows.getInt(1) == 1 }
    }

    private fun insertJournal(
        connection: Connection,
        operationKey: String,
        eventId: String,
        actorId: String,
        status: String
    ) {
        connection.prepareStatement(
            """
                INSERT INTO $TABLE(
                    operationKey, eventId, actorId, pollRevision, operationId,
                    ballotFingerprint, commandPayload, status, updatedAt
                ) VALUES (?, ?, ?, 7, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, operationKey)
            statement.setString(2, eventId)
            statement.setString(3, actorId)
            statement.setString(4, "operation-$operationKey")
            statement.setString(5, "v1|736c6f742d31=YES")
            statement.setString(6, "{\"schemaVersion\":1,\"operationKey\":\"$operationKey\"}")
            statement.setString(7, status)
            statement.setString(8, "2026-08-28T11:00:00Z")
            statement.executeUpdate()
        }
    }

    private fun selectDispatchable(
        connection: Connection,
        eventId: String,
        actorId: String
    ): List<String> = connection.prepareStatement(
        """
            SELECT operationKey FROM $TABLE
            WHERE eventId = ? AND actorId = ? AND status = 'DISPATCHED'
            ORDER BY operationKey
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, eventId)
        statement.setString(2, actorId)
        statement.executeQuery().use { rows ->
            buildList { while (rows.next()) add(rows.getString("operationKey")) }
        }
    }

    private fun reflectedValue(value: Any, suffix: String): Any? = value.javaClass.methods
        .singleOrNull { it.name == "get$suffix" && it.parameterCount == 0 }
        ?.invoke(value)

    private companion object {
        const val TABLE = "pollBallotCommandJournal"
    }
}
