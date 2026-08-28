package com.guyghost.wakeve.poll

import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.confirmation.SystemConfirmationClock
import com.guyghost.wakeve.database.WakeveDb
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

data class BallotJournalRecord(
    val operationKey: String,
    val command: PollBallotContract.BallotCommandEnvelope,
    val status: PollBallotContract.BallotJournalStatus,
    val updatedAtIso: String,
    val terminalDestination: PollBallotContract.BallotTerminalDestination? = null
)

/** Total rehydration projection: every durable DISPATCHED row remains observable. */
sealed interface BallotJournalProjection {
    data class Valid(val record: BallotJournalRecord) : BallotJournalProjection
    data class Inconsistent(
        val code: String = PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT.name,
        val retryable: Boolean = false,
        val commitOutcome: String = PollBallotContract.CommitOutcome.UNKNOWN.name
    ) : BallotJournalProjection
}

sealed interface BallotJournalResult {
    data class Stored(val record: BallotJournalRecord) : BallotJournalResult
    data class Rejected(val failure: PollBallotContract.Failure) : BallotJournalResult
}

interface BallotCommandJournal {
    suspend fun stage(command: PollBallotContract.BallotCommandEnvelope): BallotJournalResult
    suspend fun markDispatched(operationKey: String, ballotFingerprint: String): BallotJournalResult
    suspend fun cancel(operationKey: String, ballotFingerprint: String): BallotJournalResult
    fun loadDispatched(eventId: String, actorId: String): List<BallotJournalProjection>
}

/** SQLite-backed pre-dispatch journal. Its guarded updates make every status transition monotone. */
class DatabaseBallotCommandJournal private constructor(
    private val db: WakeveDb,
    private val clock: ConfirmationClock
) : BallotCommandJournal {
    constructor(db: WakeveDb) : this(db, SystemConfirmationClock)

    internal constructor(db: WakeveDb, clock: ConfirmationClock, testSeam: Unit = Unit) :
        this(db, clock)

    private val queries = db.pollBallotCommandJournalQueries
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    /** Flat facades keep the Kotlin/Native API stable for Swift orchestration. */
    suspend fun stageCommand(
        command: PollBallotContract.CommitCompleteBallotCommand
    ): BallotJournalResult {
        if (!PollBallotContract.isValidPollRevision(command.pollRevision)) return rejected()
        val authoritativeCommand = if (command.authoritativeDeadlineIso.isBlank()) {
            val deadline = db.eventQueries.selectById(command.eventId)
                .executeAsOneOrNull()
                ?.deadline
                ?: return rejected(PollBallotContract.FailureCode.EVENT_NOT_FOUND)
            command.copy(authoritativeDeadlineIso = deadline)
        } else {
            command
        }
        return stage(PollBallotContract.envelope(authoritativeCommand))
    }

    suspend fun markCommandDispatched(
        command: PollBallotContract.CommitCompleteBallotCommand
    ): BallotJournalResult {
        if (!PollBallotContract.isValidPollRevision(command.pollRevision)) return rejected()
        val envelope = PollBallotContract.envelope(command)
        return markDispatched(PollBallotContract.operationKey(envelope.identity), envelope.ballotFingerprint)
    }

    suspend fun cancelCommand(
        command: PollBallotContract.CommitCompleteBallotCommand
    ): BallotJournalResult {
        if (!PollBallotContract.isValidPollRevision(command.pollRevision)) return rejected()
        val envelope = PollBallotContract.envelope(command)
        return cancel(PollBallotContract.operationKey(envelope.identity), envelope.ballotFingerprint)
    }

    fun loadDispatchableCommands(
        eventId: String,
        actorId: String
    ): List<PollBallotContract.CommitCompleteBallotCommand> =
        loadDispatched(eventId, actorId).mapNotNull { projection ->
            (projection as? BallotJournalProjection.Valid)?.record?.command
                ?.let(PollBallotContract::command)
        }

    fun loadRehydrationProjections(
        eventId: String,
        actorId: String
    ): List<BallotJournalProjection> = queries.selectAllBySubject(eventId, actorId)
        .executeAsList()
        .map { row ->
            row.toRecord()
                ?.let(BallotJournalProjection::Valid)
                ?: BallotJournalProjection.Inconsistent()
        }

    suspend fun markOutcomeUnknownIfDispatched(
        operationKey: String,
        ballotFingerprint: String
    ): BallotJournalResult = mutex.withLock {
        val now = try {
            clock.now().toString()
        } catch (_: Exception) {
            return@withLock rejected(PollBallotContract.FailureCode.CLOCK_UNAVAILABLE, true)
        }
        queries.markOutcomeUnknownIfDispatched(now, operationKey, ballotFingerprint)
        queries.selectByOperationKey(operationKey).executeAsOneOrNull()
            ?.toRecord()
            ?.takeIf {
                it.status == PollBallotContract.BallotJournalStatus.DISPATCHED &&
                    it.command.ballotFingerprint == ballotFingerprint &&
                    it.terminalDestination == null
            }
            ?.let(BallotJournalResult::Stored)
            ?: rejected(PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT)
    }

    override suspend fun stage(
        command: PollBallotContract.BallotCommandEnvelope
    ): BallotJournalResult = mutex.withLock {
        if (!isValidEnvelope(command)) return@withLock rejected()
        val operationKey = PollBallotContract.operationKey(command.identity)
        queries.selectByOperationKey(operationKey).executeAsOneOrNull()?.let { existing ->
            val record = existing.toRecord() ?: return@withLock rejected()
            return@withLock if (record.command == command) {
                BallotJournalResult.Stored(record)
            } else {
                rejected(PollBallotContract.FailureCode.IDEMPOTENCY_CONFLICT)
            }
        }
        val now = try {
            clock.now().toString()
        } catch (_: Exception) {
            return@withLock rejected(PollBallotContract.FailureCode.CLOCK_UNAVAILABLE, true)
        }
        return@withLock try {
            queries.insertStaged(
                operationKey = operationKey,
                eventId = command.identity.eventId,
                actorId = command.identity.actorId,
                pollRevision = command.identity.pollRevision,
                operationId = command.identity.operationId,
                ballotFingerprint = command.ballotFingerprint,
                commandPayload = json.encodeToString(
                    PollBallotContract.BallotCommandEnvelope.serializer(),
                    command
                ),
                updatedAt = now
            )
            queries.selectByOperationKey(operationKey).executeAsOne().toRecord()
                ?.let(BallotJournalResult::Stored)
                ?: rejected()
        } catch (_: Exception) {
            rejected()
        }
    }

    override suspend fun markDispatched(
        operationKey: String,
        ballotFingerprint: String
    ): BallotJournalResult = transition(operationKey, ballotFingerprint, dispatch = true)

    override suspend fun cancel(
        operationKey: String,
        ballotFingerprint: String
    ): BallotJournalResult = transition(operationKey, ballotFingerprint, dispatch = false)

    override fun loadDispatched(eventId: String, actorId: String): List<BallotJournalProjection> =
        queries.selectDispatchableBySubject(eventId, actorId)
            .executeAsList()
            .map { row ->
                row.toRecord()
                    ?.let(BallotJournalProjection::Valid)
                    ?: BallotJournalProjection.Inconsistent()
            }

    suspend fun tombstoneDispatchedCommand(
        command: PollBallotContract.CommitCompleteBallotCommand,
        destination: PollBallotContract.BallotTerminalDestination
    ): BallotJournalResult = mutex.withLock {
        if (!PollBallotContract.isValidPollRevision(command.pollRevision)) return@withLock rejected()
        val envelope = PollBallotContract.envelope(command)
        val operationKey = PollBallotContract.operationKey(envelope.identity)
        val existing = queries.selectByOperationKey(operationKey).executeAsOneOrNull()
            ?: return@withLock rejected()
        if (existing.ballotFingerprint != envelope.ballotFingerprint) {
            return@withLock rejected(PollBallotContract.FailureCode.IDEMPOTENCY_CONFLICT)
        }
        val now = try {
            clock.now().toString()
        } catch (_: Exception) {
            return@withLock rejected(PollBallotContract.FailureCode.CLOCK_UNAVAILABLE, true)
        }
        val encodedDestination = json.encodeToString(
            PollBallotContract.BallotTerminalDestination.serializer(),
            destination
        )
        queries.tombstoneDispatched(
            terminalDestination = encodedDestination,
            updatedAt = now,
            operationKey = operationKey,
            ballotFingerprint = envelope.ballotFingerprint
        )
        queries.selectByOperationKey(operationKey).executeAsOneOrNull()?.toRecord()
            ?.takeIf {
                it.status == PollBallotContract.BallotJournalStatus.DISPATCH_CANCELLATION_TOMBSTONED &&
                    it.terminalDestination == destination
            }
            ?.let(BallotJournalResult::Stored)
            ?: rejected()
    }

    private suspend fun transition(
        operationKey: String,
        ballotFingerprint: String,
        dispatch: Boolean
    ): BallotJournalResult = mutex.withLock {
        val existing = queries.selectByOperationKey(operationKey).executeAsOneOrNull()
            ?: return@withLock rejected()
        if (existing.ballotFingerprint != ballotFingerprint) {
            return@withLock rejected(PollBallotContract.FailureCode.IDEMPOTENCY_CONFLICT)
        }
        val target = if (dispatch) {
            PollBallotContract.BallotJournalStatus.DISPATCHED
        } else {
            PollBallotContract.BallotJournalStatus.CANCELLED
        }
        if (existing.status == target.name) {
            return@withLock existing.toRecord()?.let(BallotJournalResult::Stored) ?: rejected()
        }
        if (existing.status != PollBallotContract.BallotJournalStatus.STAGED_NOT_DISPATCHED.name) {
            return@withLock rejected()
        }
        val now = try {
            clock.now().toString()
        } catch (_: Exception) {
            return@withLock rejected(PollBallotContract.FailureCode.CLOCK_UNAVAILABLE, true)
        }
        if (dispatch) {
            queries.markDispatched(now, operationKey, ballotFingerprint)
        } else {
            queries.cancelStaged(now, operationKey, ballotFingerprint)
        }
        queries.selectByOperationKey(operationKey).executeAsOneOrNull()?.toRecord()
            ?.takeIf { it.status == target }
            ?.let(BallotJournalResult::Stored)
            ?: rejected()
    }

    private fun com.guyghost.wakeve.PollBallotCommandJournal.toRecord(): BallotJournalRecord? {
        val command = try {
            json.decodeFromString(
                PollBallotContract.BallotCommandEnvelope.serializer(),
                commandPayload
            )
        } catch (_: Exception) {
            return null
        }
        val parsedStatus = PollBallotContract.BallotJournalStatus.entries
            .firstOrNull { it.name == status }
            ?: return null
        val destination = when {
            terminalDestination == null -> null
            parsedStatus != PollBallotContract.BallotJournalStatus.DISPATCH_CANCELLATION_TOMBSTONED -> return null
            else -> try {
                json.decodeFromString(
                    PollBallotContract.BallotTerminalDestination.serializer(),
                    terminalDestination
                )
            } catch (_: Exception) {
                return null
            }
        }
        if (parsedStatus == PollBallotContract.BallotJournalStatus.DISPATCH_CANCELLATION_TOMBSTONED &&
            destination == null
        ) return null
        if (!isValidEnvelope(command) ||
            eventId != command.identity.eventId ||
            actorId != command.identity.actorId ||
            pollRevision != command.identity.pollRevision ||
            operationId != command.identity.operationId ||
            operationKey != PollBallotContract.operationKey(command.identity) ||
            ballotFingerprint != command.ballotFingerprint
        ) return null
        return BallotJournalRecord(operationKey, command, parsedStatus, updatedAt, destination)
    }

    private fun isValidEnvelope(command: PollBallotContract.BallotCommandEnvelope): Boolean =
        command.schemaVersion == PollBallotContract.SCHEMA_VERSION &&
            command.identity.eventId.isNotEmpty() &&
            command.identity.actorId.isNotEmpty() &&
            command.identity.operationId.isNotEmpty() &&
            runCatching { Instant.parse(command.authoritativeDeadlineIso) }.isSuccess &&
            PollBallotContract.isValidPollRevision(command.identity.pollRevision) &&
            command.entries.isNotEmpty() &&
            PollBallotContract.validateEntries(command.entries.map { it.slotId }, command.entries) == null &&
            command.entries == PollBallotContract.canonicalize(command.entries) &&
            command.ballotFingerprint == PollBallotContract.fingerprint(command.entries)

    private fun rejected(
        code: PollBallotContract.FailureCode = PollBallotContract.FailureCode.COMMAND_JOURNAL_FAILED,
        retryable: Boolean = false
    ) = BallotJournalResult.Rejected(PollBallotContract.Failure(code, retryable))
}
