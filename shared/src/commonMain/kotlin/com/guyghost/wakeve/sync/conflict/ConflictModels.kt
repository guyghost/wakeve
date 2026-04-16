package com.guyghost.wakeve.sync.conflict

import kotlinx.serialization.Serializable

/**
 * Severity classification for Event fields during conflict resolution.
 *
 * CRITICAL fields require user intervention when conflicting.
 * NON_CRITICAL fields are auto-resolved via last-write-wins.
 *
 * FC&IS: Pure enumeration — no side effects.
 */
enum class ConflictFieldSeverity {
    /**
     * Field whose conflict must be shown to the user.
     * Examples: title, description, status, finalDate, proposedSlots, participants
     */
    CRITICAL,

    /**
     * Field whose conflict is resolved silently via last-write-wins.
     * Examples: expectedParticipants, minParticipants, maxParticipants, eventTypeCustom
     */
    NON_CRITICAL
}

/**
 * Registry of Event field severities for conflict classification.
 *
 * Centralised here so severity policy is a single-point-of-change.
 * FC&IS: Pure data — no I/O.
 */
object EventFieldRegistry {

    /**
     * Map of Event field name → severity.
     * All fields NOT in this map default to NON_CRITICAL.
     */
    val fieldSeverity: Map<String, ConflictFieldSeverity> = mapOf(
        "title"          to ConflictFieldSeverity.CRITICAL,
        "description"    to ConflictFieldSeverity.CRITICAL,
        "status"         to ConflictFieldSeverity.CRITICAL,
        "finalDate"      to ConflictFieldSeverity.CRITICAL,
        "deadline"       to ConflictFieldSeverity.CRITICAL,
        "proposedSlots"  to ConflictFieldSeverity.CRITICAL,
        "participants"   to ConflictFieldSeverity.CRITICAL,

        // Non-critical: informational / soft fields
        "eventTypeCustom"    to ConflictFieldSeverity.NON_CRITICAL,
        "expectedParticipants" to ConflictFieldSeverity.NON_CRITICAL,
        "minParticipants"    to ConflictFieldSeverity.NON_CRITICAL,
        "maxParticipants"    to ConflictFieldSeverity.NON_CRITICAL,
        "heroImageUrl"       to ConflictFieldSeverity.NON_CRITICAL,
        "eventType"          to ConflictFieldSeverity.NON_CRITICAL,
        "updatedAt"          to ConflictFieldSeverity.NON_CRITICAL,
        "createdAt"          to ConflictFieldSeverity.NON_CRITICAL
    )

    /** Look up severity for a field name. Defaults to NON_CRITICAL if unknown. */
    fun severityOf(fieldName: String): ConflictFieldSeverity =
        fieldSeverity[fieldName] ?: ConflictFieldSeverity.NON_CRITICAL
}

/**
 * A detected conflict on a single field between the local and remote versions
 * of an event.
 *
 * FC&IS: Immutable value object — no side effects.
 *
 * @property eventId     The ID of the conflicting event
 * @property fieldName   The Event field that has diverged
 * @property localValue  The device-local string representation of the field value
 * @property remoteValue The server-side string representation of the field value
 * @property localUpdatedAt  ISO 8601 timestamp of the local mutation
 * @property remoteUpdatedAt ISO 8601 timestamp of the server version
 * @property severity    Whether this conflict requires user intervention
 */
@Serializable
data class ConflictRecord(
    val eventId: String,
    val fieldName: String,
    val localValue: String,
    val remoteValue: String,
    val localUpdatedAt: String,
    val remoteUpdatedAt: String,
    val severity: ConflictFieldSeverity
) {
    val isCritical: Boolean get() = severity == ConflictFieldSeverity.CRITICAL
}

/**
 * The outcome of a conflict resolution decision for a single [ConflictRecord].
 *
 * FC&IS: Sealed hierarchy — pure data, no I/O.
 */
sealed class ResolutionDecision {
    abstract val fieldName: String

    /** User (or auto-resolver) chose to keep the local device value. */
    data class KeepLocal(
        override val fieldName: String,
        val localValue: String
    ) : ResolutionDecision()

    /** User (or auto-resolver) chose to keep the remote server value. */
    data class KeepRemote(
        override val fieldName: String,
        val remoteValue: String
    ) : ResolutionDecision()

    /**
     * System automatically resolved without user interaction.
     * Used for NON_CRITICAL fields via last-write-wins.
     */
    data class AutoResolved(
        override val fieldName: String,
        val chosenValue: String,
        val strategy: AutoStrategy
    ) : ResolutionDecision()

    enum class AutoStrategy {
        /** Most-recent timestamp wins. Used for NON_CRITICAL fields. */
        LAST_WRITE_WINS,
        /** Both values are merged (e.g., participant union). */
        MERGE_UNION
    }
}

/**
 * Summary of all conflicts detected during a sync cycle.
 *
 * @property eventId           The event that conflicted
 * @property criticalConflicts Fields that require user intervention
 * @property autoResolved      Fields silently resolved by the system
 * @property hasCritical       True if any user intervention is required
 */
data class ConflictSummary(
    val eventId: String,
    val criticalConflicts: List<ConflictRecord>,
    val autoResolved: List<ConflictRecord>
) {
    val hasCritical: Boolean get() = criticalConflicts.isNotEmpty()
    val totalConflicts: Int get() = criticalConflicts.size + autoResolved.size
}
