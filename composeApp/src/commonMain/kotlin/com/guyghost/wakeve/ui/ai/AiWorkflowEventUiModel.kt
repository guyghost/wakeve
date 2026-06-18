package com.guyghost.wakeve.ui.ai

import com.guyghost.wakeve.ai.PlanningAgentEvent

data class PlanningAgentEventUiItem(
    val title: String,
    val body: String,
    val requiresUserAction: Boolean = false,
    val requestId: String? = null,
    val primaryActionLabel: String? = null,
    val secondaryActionLabel: String? = null
)

fun PlanningAgentEvent.toPlanningAgentEventUiItem(): PlanningAgentEventUiItem = when (this) {
    is PlanningAgentEvent.SessionStarted -> PlanningAgentEventUiItem(
        title = "Session started",
        body = session.title
    )

    is PlanningAgentEvent.Progress -> PlanningAgentEventUiItem(
        title = "Step $step of $totalSteps",
        body = message
    )

    is PlanningAgentEvent.SuggestedPlan -> PlanningAgentEventUiItem(
        title = title,
        body = items.joinToString(separator = "\n") { "- $it" }
    )

    is PlanningAgentEvent.ParticipantTasksSuggested -> PlanningAgentEventUiItem(
        title = "Participant tasks",
        body = tasks.joinToString(separator = "\n") { "- ${it.participantName}: ${it.task}" }
    )

    is PlanningAgentEvent.BudgetCategoriesSuggested -> PlanningAgentEventUiItem(
        title = "Budget categories",
        body = categories.joinToString(separator = "\n") { "- ${it.name}: ${it.description}" }
    )

    is PlanningAgentEvent.MissingLogisticsIdentified -> PlanningAgentEventUiItem(
        title = "Missing logistics",
        body = items.joinToString(separator = "\n") { "- $it" }
    )

    is PlanningAgentEvent.ConfirmationRequested -> PlanningAgentEventUiItem(
        title = "Action needed",
        body = "${request.title}\n${request.description}",
        requiresUserAction = true,
        requestId = request.id,
        primaryActionLabel = request.confirmLabel,
        secondaryActionLabel = request.dismissLabel
    )

    is PlanningAgentEvent.ConfirmationResolved -> PlanningAgentEventUiItem(
        title = "Action resolved",
        body = if (accepted) "Accepted $requestId" else "Skipped $requestId"
    )

    is PlanningAgentEvent.Completed -> PlanningAgentEventUiItem(
        title = "Completed",
        body = summary
    )

    is PlanningAgentEvent.Failed -> PlanningAgentEventUiItem(
        title = "Error",
        body = message
    )
}
