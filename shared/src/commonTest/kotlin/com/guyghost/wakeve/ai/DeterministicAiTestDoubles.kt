package com.guyghost.wakeve.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DeterministicAiTextGenerationClient(
    private val availability: AiModelAvailability = AiModelAvailability.AVAILABLE,
    private val response: String,
    private val providerName: String = "Deterministic test model",
    private val modelName: String? = "deterministic-test-model",
    private val route: AiInferenceRoute = AiInferenceRoute.ON_DEVICE
) : AiTextGenerationClient {
    var generateCallCount: Int = 0
        private set

    var lastPrompt: String? = null
        private set

    override suspend fun availability(): AiModelAvailability = availability

    override suspend fun generateText(
        prompt: String,
        config: AiTextGenerationConfig
    ): AiTextGenerationResult {
        generateCallCount += 1
        lastPrompt = prompt
        return AiTextGenerationResult(
            text = response,
            routing = AiRoutingMetadata(
                route = route,
                providerName = providerName,
                modelName = modelName,
                cloudUsed = route == AiInferenceRoute.CLOUD_FIREBASE_AI_LOGIC
            )
        )
    }
}

class DeterministicEventPlanningAiAssistant(
    private val draft: EventPlanDraft = EventPlanDraft(
        destination = "Biarritz",
        startDate = "2026-07-12",
        endDate = "2026-07-15",
        participantCount = 8,
        budgetPerPerson = MoneyAmount(300.0, "EUR"),
        source = EventPlanDraftSource.ML_KIT_GENAI
    ).withMissingInformation(),
    private val availability: EventPlanningAiAvailability = EventPlanningAiAvailability.AVAILABLE
) : EventPlanningAiAssistant {
    override suspend fun availability(): EventPlanningAiAvailability = availability

    override fun extractEventPlan(prompt: EventPlanningPrompt): Flow<EventPlanningAiUpdate> = flow {
        emit(EventPlanningAiUpdate.Availability(availability))
        if (availability == EventPlanningAiAvailability.AVAILABLE) {
            emit(EventPlanningAiUpdate.Draft(draft.withMissingInformation()))
        }
    }
}

class DeterministicPlanningAgentClient : PlanningAgentClient {
    override fun startSession(context: EventPlanningPromptContext): Flow<PlanningAgentEvent> = flow {
        val session = PlanningAgentSession(
            id = "test-agent-${context.eventId ?: context.title.testSlug()}",
            eventId = context.eventId,
            title = context.title,
            status = PlanningAgentSessionStatus.RUNNING
        )
        emit(PlanningAgentEvent.SessionStarted(session))
        emit(PlanningAgentEvent.Progress("Reading event context", step = 1, totalSteps = 5))
        emit(
            PlanningAgentEvent.SuggestedPlan(
                title = "Suggested event plan",
                items = listOf(
                    "Confirm dates and destination",
                    "Lock participant count",
                    "Prepare logistics checklist"
                )
            )
        )
        emit(PlanningAgentEvent.Progress("Splitting participant tasks", step = 2, totalSteps = 5))
        emit(
            PlanningAgentEvent.ParticipantTasksSuggested(
                tasks = listOf(
                    ParticipantTaskSuggestion("Alice", "Confirm transport"),
                    ParticipantTaskSuggestion("Mehdi", "Check lodging"),
                    ParticipantTaskSuggestion("Nora", "Coordinate food")
                )
            )
        )
        emit(PlanningAgentEvent.Progress("Proposing budget categories", step = 3, totalSteps = 5))
        emit(
            PlanningAgentEvent.BudgetCategoriesSuggested(
                categories = listOf(
                    BudgetCategorySuggestion("Transport", "Train, fuel, parking, or rideshare costs"),
                    BudgetCategorySuggestion("Lodging", "Shared accommodation and deposits")
                )
            )
        )
        emit(PlanningAgentEvent.Progress("Checking missing logistics", step = 4, totalSteps = 5))
        emit(
            PlanningAgentEvent.MissingLogisticsIdentified(
                items = context.missingInformationLabels().ifEmpty { listOf("Arrival times") }
            )
        )
        emit(
            PlanningAgentEvent.ConfirmationRequested(
                PlanningAgentConfirmationRequest(
                    id = "confirm-budget-categories",
                    title = "Apply budget categories",
                    description = "Use draft budget categories.",
                    confirmLabel = "Apply",
                    dismissLabel = "Skip"
                )
            )
        )
        emit(PlanningAgentEvent.Progress("Preparing final review", step = 5, totalSteps = 5))
        emit(PlanningAgentEvent.Completed("Planning draft is ready for organizer review."))
    }

    override suspend fun submitConfirmation(
        sessionId: String,
        requestId: String,
        accepted: Boolean
    ): PlanningAgentEvent = PlanningAgentEvent.ConfirmationResolved(
        requestId = requestId,
        accepted = accepted
    )
}

private fun String.testSlug(): String =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "event" }
