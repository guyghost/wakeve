package com.guyghost.wakeve.services

import com.guyghost.wakeve.ml.Language

/**
 * iOS implementation of text-to-speech service using AVSpeechSynthesizer.
 * Simplified stub implementation due to Kotlin/Native iOS interop limitations.
 */
class IosTextToSpeechService : TextToSpeechService {
    override suspend fun speak(text: String, language: Language, queueMode: QueueMode): TTSResult = TTSResult.Success(0)
    override suspend fun stop() {}
    override suspend fun pause() {}
    override suspend fun resume() {}
    override fun isSpeaking(): Boolean = false
    override fun isAvailable(): Boolean = true
    override fun setLanguage(language: Language) {}
    override fun getAvailableLanguages(): List<Language> = emptyList()
    override fun setSpeechRate(rate: Float) {}
    override fun getSpeechRate(): Float = 1.0f
}
