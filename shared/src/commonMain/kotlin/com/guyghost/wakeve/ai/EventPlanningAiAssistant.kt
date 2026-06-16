package com.guyghost.wakeve.ai

import kotlinx.coroutines.flow.Flow

interface EventPlanningAiAssistant {
    suspend fun availability(): EventPlanningAiAvailability

    fun extractEventPlan(prompt: EventPlanningPrompt): Flow<EventPlanningAiUpdate>
}
