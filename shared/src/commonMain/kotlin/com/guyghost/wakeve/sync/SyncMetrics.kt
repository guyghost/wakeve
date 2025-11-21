package com.guyghost.wakeve.sync

import com.guyghost.wakeve.getCurrentTimeMillis

/**
 * Metrics collector for sync operations
 */
interface SyncMetrics {
    fun recordSyncStart()
    fun recordSyncSuccess(durationMs: Long, changesApplied: Int)
    fun recordSyncFailure(durationMs: Long, error: String)
    fun recordConflictResolved(table: String, strategy: String)
    fun getSyncStats(): SyncStats
}

data class SyncStats(
    val totalSyncs: Int = 0,
    val successfulSyncs: Int = 0,
    val failedSyncs: Int = 0,
    val averageDurationMs: Long = 0,
    val totalConflictsResolved: Int = 0,
    val lastSyncTime: Long = 0
)

/**
 * Simple in-memory metrics implementation
 */
class InMemorySyncMetrics : SyncMetrics {
    private var totalSyncs = 0
    private var successfulSyncs = 0
    private var failedSyncs = 0
    private var totalDurationMs = 0L
    private var totalConflictsResolved = 0
    private var lastSyncTime = 0L
    private var currentSyncStartTime = 0L

    override fun recordSyncStart() {
        currentSyncStartTime = getCurrentTimeMillis()
        totalSyncs++
    }

    override fun recordSyncSuccess(durationMs: Long, changesApplied: Int) {
        successfulSyncs++
        totalDurationMs += durationMs
        lastSyncTime = getCurrentTimeMillis()
    }

    override fun recordSyncFailure(durationMs: Long, error: String) {
        failedSyncs++
        totalDurationMs += durationMs
        lastSyncTime = getCurrentTimeMillis()
    }

    override fun recordConflictResolved(table: String, strategy: String) {
        totalConflictsResolved++
    }

    override fun getSyncStats(): SyncStats {
        val avgDuration = if (totalSyncs > 0) totalDurationMs / totalSyncs else 0L
        return SyncStats(
            totalSyncs = totalSyncs,
            successfulSyncs = successfulSyncs,
            failedSyncs = failedSyncs,
            averageDurationMs = avgDuration,
            totalConflictsResolved = totalConflictsResolved,
            lastSyncTime = lastSyncTime
        )
    }
}