package com.guyghost.wakeve.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

class FirebaseAiLogicCloudTextGenerationClient(
    private val modelName: String = "gemini-3.1-flash-lite"
) : AiTextGenerationClient {
    override suspend fun availability(): AiModelAvailability = AiModelAvailability.AVAILABLE

    override suspend fun generateText(
        prompt: String,
        config: AiTextGenerationConfig
    ): AiTextGenerationResult {
        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(modelName = modelName)
        val response = model.generateContent(prompt)

        return AiTextGenerationResult(
            text = response.text.orEmpty(),
            routing = AiRoutingMetadata(
                route = AiInferenceRoute.CLOUD_FIREBASE_AI_LOGIC,
                providerName = "Firebase AI Logic",
                modelName = modelName,
                cloudUsed = true
            )
        )
    }
}
