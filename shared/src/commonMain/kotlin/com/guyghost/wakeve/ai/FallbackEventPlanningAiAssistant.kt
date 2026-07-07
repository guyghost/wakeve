package com.guyghost.wakeve.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class FallbackEventPlanningAiAssistant(
    private val primary: EventPlanningAiAssistant,
    private val fallback: EventPlanningAiAssistant = RuleBasedEventPlanningAiAssistant()
) : EventPlanningAiAssistant {
    override suspend fun availability(): EventPlanningAiAvailability {
        return when (val availability = primary.availability()) {
            EventPlanningAiAvailability.AVAILABLE -> EventPlanningAiAvailability.AVAILABLE
            else -> availability
        }
    }

    override fun extractEventPlan(prompt: EventPlanningPrompt): Flow<EventPlanningAiUpdate> = flow {
        when (val availability = primary.availability()) {
            EventPlanningAiAvailability.AVAILABLE -> emitAll(primary.extractEventPlan(prompt))
            else -> {
                emit(EventPlanningAiUpdate.Availability(availability))
                emitAll(fallback.extractEventPlan(prompt))
            }
        }
    }.catch {
        emit(EventPlanningAiUpdate.Error("On-device AI failed. Using local fallback."))
        emitAll(fallback.extractEventPlan(prompt))
    }
}
