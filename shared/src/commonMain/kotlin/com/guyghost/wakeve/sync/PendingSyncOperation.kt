package com.guyghost.wakeve.sync

enum class SyncOperationType {
    CREATE,
    UPDATE,
    DELETE
}

data class PendingSyncOperation(
    val id: String,
    val entityType: String,
    val entityId: String,
    val operation: SyncOperationType,
    val createdAt: String,
    val isSynced: Boolean = false
)
