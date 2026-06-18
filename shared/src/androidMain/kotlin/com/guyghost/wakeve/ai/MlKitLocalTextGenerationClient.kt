package com.guyghost.wakeve.ai

import android.os.Build
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest

class MlKitLocalTextGenerationClient : AiTextGenerationClient {
    private val generativeModel by lazy { Generation.getClient() }

    override suspend fun availability(): AiModelAvailability =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            AiModelAvailability.UNAVAILABLE
        } else {
            generativeModel.checkStatus().toAiAvailability()
        }

    override suspend fun generateText(
        prompt: String,
        config: AiTextGenerationConfig
    ): AiTextGenerationResult {
        if (availability() != AiModelAvailability.AVAILABLE) {
            error("Gemini Nano is not available for on-device generation.")
        }

        val response = generativeModel.generateContent(
            generateContentRequest(TextPart(prompt)) {
                temperature = config.temperature
                topK = 10
                candidateCount = 1
                maxOutputTokens = config.maxOutputTokens
            }
        )

        return AiTextGenerationResult(
            text = response.candidates.firstOrNull()?.text.orEmpty(),
            routing = AiRoutingMetadata(
                route = AiInferenceRoute.ON_DEVICE,
                providerName = "ML Kit GenAI Prompt API",
                modelName = "Gemini Nano",
                cloudUsed = false
            )
        )
    }

    private fun Int.toAiAvailability(): AiModelAvailability = when (this) {
        FeatureStatus.AVAILABLE -> AiModelAvailability.AVAILABLE
        FeatureStatus.DOWNLOADABLE -> AiModelAvailability.DOWNLOADABLE
        FeatureStatus.DOWNLOADING -> AiModelAvailability.DOWNLOADING
        else -> AiModelAvailability.UNAVAILABLE
    }
}
