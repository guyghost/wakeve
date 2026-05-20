package com.guyghost.wakeve.sync.conflict

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.sample.SampleEventFactory

/**
 * Pure functional conflict detector for offline sync.
 *
 * Compares a local [Event] against a remote server [Event] and produces
 * a [ConflictSummary] classifying all diverged fields by severity.
 *
 * ## Design
 * - Stateless object — all functions are pure (no I/O, no side effects)
 * - Field comparison is string-based: each field is serialised to a canonical
 *   string and compared; the serialisation is deterministic per field type
 * - Sample events (isSample) are never flagged as conflicting
 *
 * FC&IS: This is Functional Core — all logic lives here, no DB or network.
 */
object ConflictDetector {

    /**
     * Detect conflicts between a locally-mutated event and the server version.
     *
     * Non-conflicting fields (same value on both sides) are ignored.
     * Conflicting fields are classified as CRITICAL or NON_CRITICAL via
     * [EventFieldRegistry].
     *
     * @param local   The local device version of the event
     * @param remote  The server version of the event
     * @return [ConflictSummary] — empty summary if no conflicts or if event is a sample
     */
    fun detect(local: Event, remote: Event): ConflictSummary {
        // Guard: never flag sample events as conflicting
        if (SampleEventFactory.isSampleEventId(local.id)) {
            return ConflictSummary(local.id, emptyList(), emptyList())
        }

        require(local.id == remote.id) {
            "Cannot compare events with different IDs: ${local.id} vs ${remote.id}"
        }

        val allConflicts = compareFields(local, remote)

        val critical = allConflicts.filter { it.isCritical }
        val nonCritical = allConflicts.filter { !it.isCritical }

        return ConflictSummary(
            eventId = local.id,
            criticalConflicts = critical,
            autoResolved = nonCritical
        )
    }

    /**
     * Auto-resolve all NON_CRITICAL conflicts using last-write-wins.
     *
     * Returns a list of [ResolutionDecision.AutoResolved] decisions ready
     * to be persisted by the sync layer.
     *
     * CRITICAL conflicts are NOT touched — they must go through user resolution.
     *
     * @param summary The conflict summary from [detect]
     * @return Decisions for all NON_CRITICAL fields in the summary
     */
    fun autoResolveNonCritical(summary: ConflictSummary): List<ResolutionDecision> {
        return summary.autoResolved.map { conflict ->
            val winner = pickLastWriteWins(conflict)
            ResolutionDecision.AutoResolved(
                fieldName = conflict.fieldName,
                chosenValue = winner,
                strategy = ResolutionDecision.AutoStrategy.LAST_WRITE_WINS
            )
        }
    }

    /**
     * Apply a list of [ResolutionDecision]s to a base event, producing a
     * merged event that reflects the chosen values.
     *
     * Only fields explicitly covered by decisions are changed; all other
     * fields are taken from [base] (the local version).
     *
     * @param base      The local event (starting point for merging)
     * @param decisions The resolution decisions to apply
     * @return A new Event with decisions applied
     */
    fun applyDecisions(base: Event, decisions: List<ResolutionDecision>): Event {
        var result = base
        for (decision in decisions) {
            val chosenValue = when (decision) {
                is ResolutionDecision.KeepLocal     -> decision.localValue
                is ResolutionDecision.KeepRemote    -> decision.remoteValue
                is ResolutionDecision.AutoResolved  -> decision.chosenValue
            }
            result = applyField(result, decision.fieldName, chosenValue)
        }
        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compare all Event fields and return a [ConflictRecord] for each that differs.
     */
    private fun compareFields(local: Event, remote: Event): List<ConflictRecord> {
        val localTs = local.updatedAt
        val remoteTs = remote.updatedAt

        val fields: List<Triple<String, String, String>> = listOf(
            Triple("title",               local.title,                       remote.title),
            Triple("description",         local.description,                 remote.description),
            Triple("status",              local.status.name,                 remote.status.name),
            Triple("finalDate",           local.finalDate ?: "",             remote.finalDate ?: ""),
            Triple("deadline",            local.deadline,                    remote.deadline),
            Triple("eventType",           local.eventType.name,              remote.eventType.name),
            Triple("eventTypeCustom",     local.eventTypeCustom ?: "",       remote.eventTypeCustom ?: ""),
            Triple("expectedParticipants",local.expectedParticipants?.toString() ?: "", remote.expectedParticipants?.toString() ?: ""),
            Triple("minParticipants",     local.minParticipants?.toString() ?: "",      remote.minParticipants?.toString() ?: ""),
            Triple("maxParticipants",     local.maxParticipants?.toString() ?: "",      remote.maxParticipants?.toString() ?: ""),
            // Structural fields: serialise as sorted, comma-joined canonical strings
            Triple("participants",
                local.participants.sorted().joinToString(","),
                remote.participants.sorted().joinToString(",")
            ),
            Triple("proposedSlots",
                local.proposedSlots.sortedBy { it.id }.joinToString(";") { "${it.id}:${it.start}:${it.end}:${it.timeOfDay}" },
                remote.proposedSlots.sortedBy { it.id }.joinToString(";") { "${it.id}:${it.start}:${it.end}:${it.timeOfDay}" }
            )
        )

        return fields.mapNotNull { (fieldName, localVal, remoteVal) ->
            if (localVal == remoteVal) null
            else ConflictRecord(
                eventId        = local.id,
                fieldName      = fieldName,
                localValue     = localVal,
                remoteValue    = remoteVal,
                localUpdatedAt = localTs,
                remoteUpdatedAt = remoteTs,
                severity       = EventFieldRegistry.severityOf(fieldName)
            )
        }
    }

    /**
     * Pick the winning value for a NON_CRITICAL field using last-write-wins.
     * The event with the more-recent `updatedAt` timestamp wins.
     */
    private fun pickLastWriteWins(conflict: ConflictRecord): String =
        if (conflict.localUpdatedAt >= conflict.remoteUpdatedAt) conflict.localValue
        else conflict.remoteValue

    /**
     * Apply a single field value to an event, producing a new (immutable) copy.
     *
     * Only fields known to [EventFieldRegistry] are handled; unknown fields are ignored.
     */
    private fun applyField(event: Event, fieldName: String, value: String): Event =
        when (fieldName) {
            "title"               -> event.copy(title = value)
            "description"         -> event.copy(description = value)
            "status"              -> event.copy(status = com.guyghost.wakeve.models.EventStatus.valueOf(value))
            "finalDate"           -> event.copy(finalDate = value.ifBlank { null })
            "deadline"            -> event.copy(deadline = value)
            "eventType"           -> event.copy(eventType = com.guyghost.wakeve.models.EventType.valueOf(value))
            "eventTypeCustom"     -> event.copy(eventTypeCustom = value.ifBlank { null })
            "expectedParticipants"-> event.copy(expectedParticipants = value.toIntOrNull())
            "minParticipants"     -> event.copy(minParticipants = value.toIntOrNull())
            "maxParticipants"     -> event.copy(maxParticipants = value.toIntOrNull())
            // Structural fields are intentionally NOT applied here —
            // participants and proposedSlots require domain-level merge logic
            // handled by the sync layer after user resolution.
            else -> event
        }
}
