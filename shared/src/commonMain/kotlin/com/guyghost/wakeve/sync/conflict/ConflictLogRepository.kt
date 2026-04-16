package com.guyghost.wakeve.sync.conflict

import com.guyghost.wakeve.database.WakeveDb
import kotlinx.datetime.Clock

/**
 * Repository for persisting conflict audit log entries.
 *
 * Wraps the SQLDelight `conflict_log` table.
 * Imperative Shell: handles all I/O for the conflict domain.
 */
class ConflictLogRepository(private val db: WakeveDb) {

    /**
     * Persist all conflicts from a [ConflictSummary].
     *
     * PENDING status is assigned to CRITICAL conflicts (awaiting user resolution).
     * AUTO_LWW is assigned to NON_CRITICAL conflicts (already resolved by system).
     */
    fun logSummary(summary: ConflictSummary) {
        val now = Clock.System.now().toString()

        db.transaction {
            summary.criticalConflicts.forEach { conflict ->
                insertConflict(conflict, resolution = "PENDING", resolvedBy = null, resolvedAt = null, now = now)
            }
            summary.autoResolved.forEach { conflict ->
                insertConflict(conflict, resolution = "AUTO_LWW", resolvedBy = "SYSTEM", resolvedAt = now, now = now)
            }
        }
    }

    /**
     * Mark a conflict as resolved with the given strategy.
     *
     * @param conflictId  The conflict_log.id to update
     * @param strategy    "KEEP_LOCAL" | "KEEP_REMOTE"
     * @param resolvedBy  "USER" | "SYSTEM"
     */
    fun markResolved(conflictId: String, strategy: String, resolvedBy: String) {
        val now = Clock.System.now().toString()
        db.conflictLogQueries.updateResolution(
            resolution_strategy = strategy,
            resolved_by = resolvedBy,
            resolved_at = now,
            id = conflictId
        )
    }

    /**
     * Count pending CRITICAL conflicts across all events.
     * Used by the sync layer to decide whether to surface the resolution UI.
     */
    fun pendingCriticalCount(): Long =
        db.conflictLogQueries.countPendingCritical().executeAsOne()

    /**
     * Prune conflict log entries older than 30 days to manage storage.
     */
    fun pruneOlderThan(isoTimestamp: String) {
        db.conflictLogQueries.deleteOlderThan(isoTimestamp)
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun insertConflict(
        conflict: ConflictRecord,
        resolution: String,
        resolvedBy: String?,
        resolvedAt: String?,
        now: String
    ) {
        val id = "conflict_${conflict.eventId}_${conflict.fieldName}_$now"
            .replace(":", "-").take(120)

        db.conflictLogQueries.insertConflict(
            id                = id,
            event_id          = conflict.eventId,
            field_name        = conflict.fieldName,
            local_value       = conflict.localValue,
            remote_value      = conflict.remoteValue,
            local_updated_at  = conflict.localUpdatedAt,
            remote_updated_at = conflict.remoteUpdatedAt,
            severity          = conflict.severity.name,
            resolution_strategy = resolution,
            resolved_by       = resolvedBy,
            resolved_at       = resolvedAt,
            created_at        = now
        )
    }
}
