package com.guyghost.wakeve.ai

import kotlinx.serialization.Serializable

@Serializable
enum class AiUseCase {
    EVENT_PLAN_DRAFT,
    EVENT_SUMMARY,
    ORGANIZER_MESSAGE,
    PLANNING_AGENT,
    POLL_SUGGESTION,
    CHECKLIST,
    INVITATION_MESSAGE,
    TRANSPORT_SUGGESTION,
    SUGGESTION_RATIONALE
}

@Serializable
enum class AiValidationStatus {
    ACCEPTED,
    NEEDS_REVIEW,
    REJECTED
}

@Serializable
data class AiValidationResult(
    val status: AiValidationStatus,
    val issues: List<String> = emptyList()
) {
    companion object {
        fun accepted(): AiValidationResult = AiValidationResult(AiValidationStatus.ACCEPTED)

        fun needsReview(vararg issues: String): AiValidationResult =
            AiValidationResult(AiValidationStatus.NEEDS_REVIEW, issues.toList())

        fun rejected(vararg issues: String): AiValidationResult =
            AiValidationResult(AiValidationStatus.REJECTED, issues.toList())
    }
}

@Serializable
data class AiCostEstimate(
    val known: Boolean,
    val amount: Double? = null,
    val currencyCode: String? = null,
    val inputUnits: Long? = null,
    val outputUnits: Long? = null
) {
    companion object {
        fun unknown(): AiCostEstimate = AiCostEstimate(known = false)

        fun zeroOnDevice(): AiCostEstimate =
            AiCostEstimate(
                known = true,
                amount = 0.0,
                currencyCode = "USD"
            )
    }
}

@Serializable
data class AiInteractionMetadata(
    val useCase: AiUseCase,
    val routing: AiRoutingMetadata,
    val sanitizedInputSummary: String,
    val sanitizedOutputSummary: String,
    val confidence: Double,
    val reasoningSummary: String,
    val latencyMillis: Long? = null,
    val validation: AiValidationResult,
    val cost: AiCostEstimate = routing.defaultCostEstimate()
) {
    companion object {
        fun fromRouting(
            useCase: AiUseCase,
            routing: AiRoutingMetadata,
            sanitizedInputSummary: String,
            sanitizedOutputSummary: String,
            confidence: Double = routing.defaultConfidence(),
            reasoningSummary: String,
            latencyMillis: Long? = null,
            validation: AiValidationResult = AiValidationResult.needsReview()
        ): AiInteractionMetadata =
            AiInteractionMetadata(
                useCase = useCase,
                routing = routing,
                sanitizedInputSummary = sanitizedInputSummary.sanitizedMetadataText("Input summary unavailable"),
                sanitizedOutputSummary = sanitizedOutputSummary.sanitizedMetadataText("Output summary unavailable"),
                confidence = confidence,
                reasoningSummary = reasoningSummary.sanitizedMetadataText("Reasoning summary unavailable"),
                latencyMillis = latencyMillis,
                validation = validation,
                cost = routing.defaultCostEstimate()
            )

        fun localFallback(
            useCase: AiUseCase,
            providerName: String,
            sanitizedInputSummary: String,
            sanitizedOutputSummary: String,
            reasoningSummary: String,
            validation: AiValidationResult = AiValidationResult.accepted()
        ): AiInteractionMetadata =
            fromRouting(
                useCase = useCase,
                routing = AiRoutingMetadata(
                    route = AiInferenceRoute.LOCAL_FALLBACK,
                    providerName = providerName,
                    cloudUsed = false
                ),
                sanitizedInputSummary = sanitizedInputSummary,
                sanitizedOutputSummary = sanitizedOutputSummary,
                confidence = 1.0,
                reasoningSummary = reasoningSummary,
                validation = validation
            )
    }
}

object AiInteractionMetadataPolicy {
    fun validate(metadata: AiInteractionMetadata?): AiValidationResult {
        if (metadata == null) return AiValidationResult.rejected("missing_metadata")

        val issues = buildList {
            if (metadata.confidence !in 0.0..1.0) add("invalid_confidence")
            if (metadata.sanitizedInputSummary.isBlank()) add("missing_input_summary")
            if (metadata.sanitizedOutputSummary.isBlank()) add("missing_output_summary")
            if (metadata.reasoningSummary.isBlank()) add("missing_reasoning_summary")
            if (metadata.latencyMillis != null && metadata.latencyMillis < 0) add("invalid_latency")
            if (metadata.cost.amount != null && metadata.cost.amount < 0.0) add("invalid_cost")
            if (metadata.cost.inputUnits != null && metadata.cost.inputUnits < 0) add("invalid_input_units")
            if (metadata.cost.outputUnits != null && metadata.cost.outputUnits < 0) add("invalid_output_units")
            if (metadata.validation.status == AiValidationStatus.REJECTED) add("output_rejected")
            addAll(metadata.validation.issues)
        }.distinct()

        return when {
            issues.isNotEmpty() -> AiValidationResult(AiValidationStatus.REJECTED, issues)
            metadata.validation.status == AiValidationStatus.ACCEPTED -> AiValidationResult.accepted()
            else -> AiValidationResult.needsReview()
        }
    }

    fun canExposeApplyAction(metadata: AiInteractionMetadata?): Boolean =
        validate(metadata).status != AiValidationStatus.REJECTED
}

internal fun AiRoutingMetadata.defaultMetadata(
    useCase: AiUseCase,
    inputSummary: String,
    outputSummary: String,
    reasoningSummary: String,
    validation: AiValidationResult = AiValidationResult.needsReview(),
    latencyMillis: Long? = null
): AiInteractionMetadata =
    AiInteractionMetadata.fromRouting(
        useCase = useCase,
        routing = this,
        sanitizedInputSummary = inputSummary,
        sanitizedOutputSummary = outputSummary,
        reasoningSummary = reasoningSummary,
        latencyMillis = latencyMillis,
        validation = validation
    )

private fun AiRoutingMetadata.defaultConfidence(): Double =
    when (route) {
        AiInferenceRoute.LOCAL_FALLBACK -> 1.0
        AiInferenceRoute.ON_DEVICE -> 0.8
        AiInferenceRoute.CLOUD_FIREBASE_AI_LOGIC -> 0.7
    }

private fun AiRoutingMetadata.defaultCostEstimate(): AiCostEstimate =
    when (route) {
        AiInferenceRoute.ON_DEVICE,
        AiInferenceRoute.LOCAL_FALLBACK -> AiCostEstimate.zeroOnDevice()
        AiInferenceRoute.CLOUD_FIREBASE_AI_LOGIC -> AiCostEstimate.unknown()
    }

private fun String.sanitizedMetadataText(fallback: String): String =
    trim()
        .takeIf { it.isNotBlank() }
        ?.take(240)
        ?: fallback
