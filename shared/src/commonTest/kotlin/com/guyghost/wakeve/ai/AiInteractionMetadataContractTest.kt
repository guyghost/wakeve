package com.guyghost.wakeve.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiInteractionMetadataContractTest {
    @Test
    fun `accepted metadata allows apply actions`() {
        val metadata = sampleMetadata(validation = AiValidationResult.accepted())

        val validation = AiInteractionMetadataPolicy.validate(metadata)

        assertEquals(AiValidationStatus.ACCEPTED, validation.status)
        assertTrue(AiInteractionMetadataPolicy.canExposeApplyAction(metadata))
        assertEquals(AiCostEstimate.zeroOnDevice(), metadata.cost)
    }

    @Test
    fun `missing metadata rejects apply actions`() {
        val validation = AiInteractionMetadataPolicy.validate(null)

        assertEquals(AiValidationStatus.REJECTED, validation.status)
        assertTrue("missing_metadata" in validation.issues)
        assertFalse(AiInteractionMetadataPolicy.canExposeApplyAction(null))
    }

    @Test
    fun `invalid metadata fails deterministic validation`() {
        val metadata = sampleMetadata(
            confidence = 1.4,
            sanitizedInputSummary = "",
            sanitizedOutputSummary = "",
            reasoningSummary = ""
        )

        val validation = AiInteractionMetadataPolicy.validate(metadata)

        assertEquals(AiValidationStatus.REJECTED, validation.status)
        assertTrue("invalid_confidence" in validation.issues)
        assertTrue("missing_input_summary" in validation.issues)
        assertTrue("missing_output_summary" in validation.issues)
        assertTrue("missing_reasoning_summary" in validation.issues)
    }

    @Test
    fun `rejected output metadata cannot expose apply actions`() {
        val metadata = sampleMetadata(
            validation = AiValidationResult.rejected("invented_participant")
        )

        assertFalse(AiInteractionMetadataPolicy.canExposeApplyAction(metadata))
    }

    private fun sampleMetadata(
        confidence: Double = 0.82,
        sanitizedInputSummary: String = "Weekend event context",
        sanitizedOutputSummary: String = "Draft invitation message",
        reasoningSummary: String = "Generated copy from supplied event facts.",
        validation: AiValidationResult = AiValidationResult.needsReview()
    ): AiInteractionMetadata =
        AiInteractionMetadata(
            useCase = AiUseCase.ORGANIZER_MESSAGE,
            routing = AiRoutingMetadata(
                route = AiInferenceRoute.ON_DEVICE,
                providerName = "Deterministic test model",
                modelName = "deterministic-test-model",
                cloudUsed = false
            ),
            sanitizedInputSummary = sanitizedInputSummary,
            sanitizedOutputSummary = sanitizedOutputSummary,
            confidence = confidence,
            reasoningSummary = reasoningSummary,
            latencyMillis = 42,
            validation = validation,
            cost = AiCostEstimate.zeroOnDevice()
        )
}
