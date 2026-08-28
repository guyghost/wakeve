package com.guyghost.wakeve.poll

import com.guyghost.wakeve.models.Vote
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Versioned, cross-platform boundary for one complete poll ballot. */
object PollBallotContract {
    const val SCHEMA_VERSION: Int = 1
    const val MAX_SAFE_POLL_REVISION: Long = 9_007_199_254_740_991L

    @Serializable
    data class BallotEntry(
        val slotId: String,
        @SerialName("choice") val vote: Vote
    )

    /** Flat compatibility API used by Kotlin callers and exported cleanly to Swift. */
    data class CommitCompleteBallotCommand(
        val eventId: String,
        val actorId: String,
        val pollRevision: Long,
        val entries: List<BallotEntry>,
        val operationId: String,
        val authoritativeDeadlineIso: String = ""
    )

    @Serializable
    data class BallotOperationIdentity(
        val eventId: String = "",
        val actorId: String = "",
        val pollRevision: Long = -1,
        val operationId: String = ""
    )

    @Serializable
    data class BallotCommandEnvelope(
        val schemaVersion: Int = SCHEMA_VERSION,
        val identity: BallotOperationIdentity,
        val authoritativeDeadlineIso: String = "",
        val entries: List<BallotEntry>,
        val ballotFingerprint: String
    )

    @Serializable
    data class BallotSyncPayload(
        val schemaVersion: Int = SCHEMA_VERSION,
        val localReceiptId: String,
        val command: BallotCommandEnvelope
    )

    @Serializable
    enum class BallotServerOutcome { APPLIED, ALREADY_APPLIED }

    @Serializable
    data class BallotServerAck(
        val localReceiptId: String,
        val serverReceiptId: String,
        val identity: BallotOperationIdentity,
        val ballotFingerprint: String,
        val outcome: BallotServerOutcome
    )

    enum class BallotJournalStatus {
        STAGED_NOT_DISPATCHED,
        DISPATCHED,
        CANCELLED,
        DISPATCH_CANCELLATION_TOMBSTONED
    }

    @Serializable
    enum class BallotTerminalDestinationKind { CANCELLED, TERMINAL_FAILURE, REVISED }

    /** Closed, discriminated terminal destination; invalid field combinations are unrepresentable. */
    @Serializable
    sealed interface BallotTerminalDestination {
        val kind: BallotTerminalDestinationKind

        @Serializable
        @SerialName("CANCELLED")
        data object Cancelled : BallotTerminalDestination {
            override val kind = BallotTerminalDestinationKind.CANCELLED
        }

        @Serializable
        @SerialName("TERMINAL_FAILURE")
        data class TerminalFailure(
            val failureCode: FailureCode,
            val commitOutcome: CommitOutcome
        ) : BallotTerminalDestination {
            override val kind = BallotTerminalDestinationKind.TERMINAL_FAILURE
        }

        @Serializable
        @SerialName("REVISED")
        data object Revised : BallotTerminalDestination {
            override val kind = BallotTerminalDestinationKind.REVISED
        }
    }

    /** Compatibility factory retained for Kotlin callers during the v1 rollout. */
    fun BallotTerminalDestination(
        kind: BallotTerminalDestinationKind,
        failureCode: FailureCode? = null,
        commitOutcome: CommitOutcome? = null
    ): BallotTerminalDestination = when (kind) {
        BallotTerminalDestinationKind.CANCELLED -> {
            require(failureCode == null) { "CANCELLED cannot carry a failure code" }
            require(commitOutcome == null || commitOutcome == CommitOutcome.UNKNOWN) {
                "CANCELLED cannot claim a commit outcome"
            }
            BallotTerminalDestination.Cancelled
        }
        BallotTerminalDestinationKind.REVISED -> {
            require(failureCode == null && commitOutcome == null) {
                "REVISED cannot carry failure data"
            }
            BallotTerminalDestination.Revised
        }
        BallotTerminalDestinationKind.TERMINAL_FAILURE ->
            BallotTerminalDestination.TerminalFailure(
                failureCode = requireNotNull(failureCode),
                commitOutcome = requireNotNull(commitOutcome)
            )
    }

    enum class SyncStatus {
        LOCAL_PENDING,
        SYNCED,
        /** Persisted spelling retained while older clients are still on disk. */
        SERVER_ACKNOWLEDGED
    }

    data class Receipt(
        val receiptId: String,
        val operationId: String,
        val eventId: String,
        val actorId: String,
        val pollRevision: Long,
        val ballotFingerprint: String,
        val authoritativeDeadlineIso: String,
        val acceptedAtIso: String,
        val syncStatus: SyncStatus,
        val syncPayload: BallotSyncPayload,
        val serverReceiptId: String? = null
    )

    @Serializable
    enum class CommitOutcome { NOT_COMMITTED, UNKNOWN }

    @Serializable
    enum class FailureCode {
        REPOSITORY_UNAVAILABLE,
        EVENT_NOT_FOUND,
        FORBIDDEN,
        INVALID_EVENT_STATUS,
        INVALID_POLL_REVISION,
        POLL_REVISION_CONFLICT,
        INVALID_DEADLINE_ISO,
        INVALID_NOW_ISO,
        DEADLINE_REACHED,
        INCOMPLETE_BALLOT,
        DUPLICATE_SLOT,
        UNKNOWN_SLOT,
        INVALID_SLOT_ID,
        INVALID_CHOICE,
        LOCAL_TRANSACTION_FAILED,
        IDEMPOTENCY_CONFLICT,
        CLOCK_UNAVAILABLE,
        COMMAND_JOURNAL_FAILED,
        REPOSITORY_INCONSISTENT
    }

    data class Failure(
        val code: FailureCode,
        val retryable: Boolean = false,
        val commitOutcome: CommitOutcome = CommitOutcome.NOT_COMMITTED
    )

    sealed interface CommitResult {
        data class Committed(val receipt: Receipt) : CommitResult
        data class AlreadyCommitted(val receipt: Receipt) : CommitResult
        data class Rejected(
            val operationId: String,
            val failure: Failure
        ) : CommitResult
    }

    /** Read-only cancellation resolution after a command has crossed DISPATCHED. */
    sealed interface ResolutionResult {
        data class Committed(val receipt: Receipt) : ResolutionResult
        data class ProvenNotCommitted(
            val operationKey: String,
            val ballotFingerprint: String
        ) : ResolutionResult
        data class Unknown(val failure: Failure) : ResolutionResult
    }

    fun identity(command: CommitCompleteBallotCommand): BallotOperationIdentity =
        BallotOperationIdentity(
            eventId = command.eventId,
            actorId = command.actorId,
            pollRevision = command.pollRevision,
            operationId = command.operationId
        )

    fun envelope(command: CommitCompleteBallotCommand): BallotCommandEnvelope {
        require(isValidPollRevision(command.pollRevision)) { FailureCode.INVALID_POLL_REVISION.name }
        val canonicalEntries = canonicalize(command.entries)
        return BallotCommandEnvelope(
            identity = identity(command),
            authoritativeDeadlineIso = command.authoritativeDeadlineIso,
            entries = canonicalEntries,
            ballotFingerprint = fingerprint(canonicalEntries)
        )
    }

    fun command(envelope: BallotCommandEnvelope): CommitCompleteBallotCommand =
        CommitCompleteBallotCommand(
            eventId = envelope.identity.eventId,
            actorId = envelope.identity.actorId,
            pollRevision = envelope.identity.pollRevision,
            entries = envelope.entries,
            operationId = envelope.identity.operationId,
            authoritativeDeadlineIso = envelope.authoritativeDeadlineIso
        )

    fun isValidPollRevision(value: Long): Boolean = value in 0L..MAX_SAFE_POLL_REVISION

    fun operationKey(command: CommitCompleteBallotCommand): String = operationKey(identity(command))

    fun operationKey(identity: BallotOperationIdentity): String {
        require(isValidPollRevision(identity.pollRevision)) { FailureCode.INVALID_POLL_REVISION.name }
        return "v1|${identityField(identity.eventId)}|${identityField(identity.actorId)}|" +
            "${identity.pollRevision}|${identityField(identity.operationId)}"
    }

    fun validateEntries(validSlotIds: Collection<String>, entries: List<BallotEntry>): FailureCode? {
        val required = validSlotIds.toSet()
        val seen = mutableSetOf<String>()
        for (entry in entries) {
            if (!isUnicodeScalarString(entry.slotId)) return FailureCode.INVALID_SLOT_ID
            if (!seen.add(entry.slotId)) return FailureCode.DUPLICATE_SLOT
            if (entry.slotId !in required) return FailureCode.UNKNOWN_SLOT
        }
        return if (required.isNotEmpty() && seen == required) null else FailureCode.INCOMPLETE_BALLOT
    }

    /** Locale-free v1 format: unsigned UTF-8 order and lowercase UTF-8 hex. */
    fun fingerprint(entries: List<BallotEntry>): String = buildString {
        append("v1")
        canonicalize(entries).forEach { entry ->
            append('|')
            append(utf8Hex(entry.slotId))
            append('=')
            append(entry.vote.name)
        }
    }

    fun canonicalize(entries: List<BallotEntry>): List<BallotEntry> =
        entries.sortedWith { left, right -> compareUtf8(left.slotId, right.slotId) }

    fun matches(envelope: BallotCommandEnvelope, command: CommitCompleteBallotCommand): Boolean {
        if (!isValidPollRevision(command.pollRevision)) return false
        val incoming = envelope(command)
        return envelope.schemaVersion == incoming.schemaVersion &&
            envelope.identity == incoming.identity &&
            envelope.authoritativeDeadlineIso == incoming.authoritativeDeadlineIso &&
            envelope.ballotFingerprint == incoming.ballotFingerprint &&
            envelope.entries == incoming.entries
    }

    private fun identityField(value: String): String {
        val bytes = value.encodeToByteArray()
        return "${bytes.size}:${bytes.toHex()}"
    }

    private fun utf8Hex(value: String): String = value.encodeToByteArray().toHex()

    private fun ByteArray.toHex(): String = buildString(size * 2) {
        for (byte in this@toHex) {
            val value = byte.toInt() and 0xff
            append(HEX[value ushr 4])
            append(HEX[value and 0x0f])
        }
    }

    private fun compareUtf8(left: String, right: String): Int {
        val leftBytes = left.encodeToByteArray()
        val rightBytes = right.encodeToByteArray()
        val commonLength = minOf(leftBytes.size, rightBytes.size)
        for (index in 0 until commonLength) {
            val comparison = (leftBytes[index].toInt() and 0xff) -
                (rightBytes[index].toInt() and 0xff)
            if (comparison != 0) return comparison
        }
        return leftBytes.size - rightBytes.size
    }

    private fun isUnicodeScalarString(value: String): Boolean {
        if (value.isEmpty()) return false
        var index = 0
        while (index < value.length) {
            when (value[index].code) {
                in 0xD800..0xDBFF -> {
                    if (index + 1 >= value.length || value[index + 1].code !in 0xDC00..0xDFFF) {
                        return false
                    }
                    index += 2
                }
                in 0xDC00..0xDFFF -> return false
                else -> index += 1
            }
        }
        return true
    }

    private const val HEX = "0123456789abcdef"
}
