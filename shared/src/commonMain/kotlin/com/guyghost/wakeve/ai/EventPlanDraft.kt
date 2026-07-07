package com.guyghost.wakeve.ai

import com.guyghost.wakeve.models.EventType
import kotlinx.serialization.Serializable

@Serializable
data class EventPlanDraft(
    val destination: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val participantCount: Int? = null,
    val budgetPerPerson: MoneyAmount? = null,
    val eventType: EventType = EventType.OTHER,
    val constraints: List<String> = emptyList(),
    val missingInformation: List<EventPlanMissingInformation> = emptyList(),
    val source: EventPlanDraftSource = EventPlanDraftSource.RULE_BASED,
    val metadata: AiInteractionMetadata = source.defaultMetadata()
) {
    val hasRequiredCreationFields: Boolean
        get() = destination != null &&
            startDate != null &&
            endDate != null &&
            participantCount != null
}

@Serializable
data class MoneyAmount(
    val amount: Double,
    val currencyCode: String = "EUR"
)

@Serializable
enum class EventPlanDraftSource {
    ML_KIT_GENAI,
    RULE_BASED,
    FAKE
}

internal fun EventPlanDraftSource.defaultMetadata(): AiInteractionMetadata =
    when (this) {
        EventPlanDraftSource.ML_KIT_GENAI -> AiRoutingMetadata(
            route = AiInferenceRoute.ON_DEVICE,
            providerName = "ML Kit GenAI",
            modelName = "gemini-nano",
            cloudUsed = false
        )
        EventPlanDraftSource.RULE_BASED -> AiRoutingMetadata(
            route = AiInferenceRoute.LOCAL_FALLBACK,
            providerName = "Wakeve rule-based event parser",
            cloudUsed = false
        )
        EventPlanDraftSource.FAKE -> AiRoutingMetadata(
            route = AiInferenceRoute.LOCAL_FALLBACK,
            providerName = "Deterministic test event parser",
            cloudUsed = false
        )
    }.defaultMetadata(
        useCase = AiUseCase.EVENT_PLAN_DRAFT,
        inputSummary = "Event planning prompt",
        outputSummary = "Structured event draft",
        reasoningSummary = "Parsed into typed draft fields for user review.",
        validation = AiValidationResult.needsReview()
    )

@Serializable
enum class EventPlanMissingInformation {
    DESTINATION,
    START_DATE,
    END_DATE,
    PARTICIPANT_COUNT,
    BUDGET_PER_PERSON,
    EVENT_TYPE,
    CONSTRAINTS
}

enum class EventPlanningInputModality {
    TEXT,
    SPEECH_TRANSCRIPT
}

data class EventPlanningPrompt(
    val text: String,
    val modality: EventPlanningInputModality = EventPlanningInputModality.TEXT,
    val localeTag: String = "fr-FR",
    val referenceYear: Int? = null
)

enum class EventPlanningAiAvailability {
    AVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    UNAVAILABLE,
    FALLBACK_ONLY
}

sealed interface EventPlanningAiUpdate {
    data class Availability(val availability: EventPlanningAiAvailability) : EventPlanningAiUpdate
    data class Draft(val draft: EventPlanDraft) : EventPlanningAiUpdate
    data class Error(val message: String, val fallbackDraft: EventPlanDraft? = null) : EventPlanningAiUpdate
}
