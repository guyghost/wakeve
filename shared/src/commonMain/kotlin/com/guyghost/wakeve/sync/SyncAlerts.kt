package com.guyghost.wakeve.sync

/**
 * Alert manager for sync operations
 */
interface SyncAlertManager {
    fun alertSyncFailure(error: String, retryCount: Int)
    fun alertHighConflictRate(conflicts: Int)
    fun alertNetworkIssues()
}

/**
 * Simple logging-based alert manager
 */
class LoggingSyncAlertManager : SyncAlertManager {
    override fun alertSyncFailure(error: String, retryCount: Int) {
        println("ALERT: Sync failure - $error (retry $retryCount)")
    }

    override fun alertHighConflictRate(conflicts: Int) {
        println("ALERT: High conflict rate - $conflicts conflicts detected")
    }

    override fun alertNetworkIssues() {
        println("ALERT: Network connectivity issues detected")
    }
}