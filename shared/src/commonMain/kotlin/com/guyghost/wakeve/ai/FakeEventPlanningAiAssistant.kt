package com.guyghost.wakeve.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Deprecated(
    message = "Use RuleBasedEventPlanningAiAssistant in production or DeterministicEventPlanningAiAssistant in tests.",
    replaceWith = ReplaceWith("RuleBasedEventPlanningAiAssistant()")
)
class FakeEventPlanningAiAssistant(
    private val draft: EventPlanDraft = EventPlanDraft(
        destination = "Biarritz",
        startDate = "2026-07-12",
        endDate = "2026-07-15",
        participantCount = 8,
        budgetPerPerson = MoneyAmount(300.0, "EUR"),
        source = EventPlanDraftSource.FAKE
    ).withMissingInformation(),
    private val availability: EventPlanningAiAvailability = EventPlanningAiAvailability.UNAVAILABLE
) : EventPlanningAiAssistant {
    override suspend fun availability(): EventPlanningAiAvailability = EventPlanningAiAvailability.UNAVAILABLE

    override fun extractEventPlan(prompt: EventPlanningPrompt): Flow<EventPlanningAiUpdate> = flow {
        emit(EventPlanningAiUpdate.Availability(EventPlanningAiAvailability.UNAVAILABLE))
        emit(EventPlanningAiUpdate.Error("Event planning test fake is not available in production."))
    }
}
