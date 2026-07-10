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
    val pending = PendingFact.LOCAL_MUTATION in input.pendingFacts

    return ProductLanguageProjection(
        domainStatus = input.status,
        title = title,
        status = if (pending) SemanticKey("sync.waiting") else null,
        primaryAction = when (input.allowedAction) {
            AllowedAction.CONTINUE -> SemanticKey("event.action.continue")
            AllowedAction.RETRY_SYNC -> SemanticKey("sync.retry")
            null -> null
        },
        sharedConfirmation = !pending,
    )
}
