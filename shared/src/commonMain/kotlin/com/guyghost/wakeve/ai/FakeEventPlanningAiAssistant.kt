package com.guyghost.wakeve.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeEventPlanningAiAssistant(
    private val draft: EventPlanDraft = EventPlanDraft(
        destination = "Biarritz",
        startDate = "2026-07-12",
        endDate = "2026-07-15",
        participantCount = 8,
        budgetPerPerson = MoneyAmount(300.0, "EUR"),
        source = EventPlanDraftSource.FAKE
    ).withMissingInformation(),
    private val availability: EventPlanningAiAvailability = EventPlanningAiAvailability.AVAILABLE
) : EventPlanningAiAssistant {
    override suspend fun availability(): EventPlanningAiAvailability = availability

    override fun extractEventPlan(prompt: EventPlanningPrompt): Flow<EventPlanningAiUpdate> = flow {
        emit(EventPlanningAiUpdate.Availability(availability))
        emit(EventPlanningAiUpdate.Draft(draft.copy(source = EventPlanDraftSource.FAKE).withMissingInformation()))
    }
}
