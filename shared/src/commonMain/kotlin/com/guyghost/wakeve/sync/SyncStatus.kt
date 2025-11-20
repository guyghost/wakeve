package com.guyghost.wakeve.sync

/**
 * Sync status for tracking synchronization state
 */
sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}