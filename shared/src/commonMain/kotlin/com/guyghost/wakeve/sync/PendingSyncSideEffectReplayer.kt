package com.guyghost.wakeve.sync

/**
 * Replays local side effects that are queued in sync metadata but must not be
 * sent to the backend as generic domain changes.
 */
interface PendingSyncSideEffectReplayer {
    suspend fun hasPending(): Boolean
    suspend fun replayPending(): Result<Int>
}
