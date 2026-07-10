package com.guyghost.wakeve.productlanguage

import com.guyghost.wakeve.models.EventStatus

@JvmInline
value class SemanticKey(val value: String)

enum class UserRole { ORGANIZER, PARTICIPANT }

enum class PendingFact { LOCAL_MUTATION, SYNC_CONFLICT }

enum class AllowedAction { CONTINUE, RETRY_SYNC }

data class ProductLanguageInput(
    val status: EventStatus,
    val role: UserRole,
    val confirmedFacts: Set<String>,
    val pendingFacts: Set<PendingFact>,
    val allowedAction: AllowedAction?,
)

data class ProductLanguageProjection(
    val domainStatus: EventStatus,
    val title: SemanticKey,
    val status: SemanticKey?,
    val primaryAction: SemanticKey?,
    val sharedConfirmation: Boolean,
)

fun projectEventState(input: ProductLanguageInput): ProductLanguageProjection {
    val title = SemanticKey("event.state.${input.status.name.lowercase()}")
    val hasConflict = PendingFact.SYNC_CONFLICT in input.pendingFacts
    val hasPendingSync = PendingFact.LOCAL_MUTATION in input.pendingFacts
    val isTerminal = input.status == EventStatus.FINALIZED

    return ProductLanguageProjection(
        domainStatus = input.status,
        title = title,
        status = when {
            isTerminal -> null
            hasConflict -> SemanticKey("sync.conflict")
            hasPendingSync -> SemanticKey("sync.waiting")
            else -> null
        },
        primaryAction = when {
            isTerminal -> null
            hasConflict && input.allowedAction == AllowedAction.RETRY_SYNC -> SemanticKey("sync.retry")
            hasPendingSync && input.allowedAction == AllowedAction.RETRY_SYNC -> SemanticKey("sync.retry")
            !hasConflict && !hasPendingSync && input.allowedAction == AllowedAction.CONTINUE ->
                SemanticKey("event.action.continue")
            else -> null
        },
        sharedConfirmation = !hasPendingSync && !hasConflict,
    )
}
