package com.guyghost.wakeve.workflow

/**
 * Local-first workflow work produced by state transitions.
 *
 * These records are queued locally before sync so notification and calendar
 * artifacts can be reconciled later when connectivity or platform services
 * are available.
 */
data class WorkflowOutboxRecord(
    val eventId: String,
    val type: WorkflowOutboxType,
    val finalDate: String,
    val status: PendingWorkflowStatus = PendingWorkflowStatus.PENDING_SYNC,
    val operationId: String? = null,
    val effectKey: String? = null
)

enum class WorkflowOutboxType {
    CONFIRMATION_EFFECT,
    DATE_CONFIRMATION_NOTIFICATION,
    CALENDAR_INVITATION_ARTIFACT
}

enum class PendingWorkflowStatus {
    PENDING_SYNC,
    SYNCED
}
