package com.guyghost.wakeve.services

import com.guyghost.wakeve.ml.Language

/**
 * iOS text-to-speech placeholder.
 *
 * Fails explicitly until AVSpeechSynthesizer is bridged into this service.
 */
class IosTextToSpeechService : TextToSpeechService {
    override suspend fun speak(
        text: String,
        language: Language,
        queueMode: QueueMode
    ): TTSResult = NoConfiguredTextToSpeechService.speak(text, language, queueMode)

    override suspend fun stop() {
        NoConfiguredTextToSpeechService.stop()
    }

    override suspend fun pause() {
        NoConfiguredTextToSpeechService.pause()
    }

    override suspend fun resume() {
        NoConfiguredTextToSpeechService.resume()
    }

    override fun isSpeaking(): Boolean = NoConfiguredTextToSpeechService.isSpeaking()

    override fun isAvailable(): Boolean = NoConfiguredTextToSpeechService.isAvailable()

    override fun setLanguage(language: Language) {
        NoConfiguredTextToSpeechService.setLanguage(language)
    }

    override fun getAvailableLanguages(): List<Language> =
        NoConfiguredTextToSpeechService.getAvailableLanguages()

    override fun setSpeechRate(rate: Float) {
        NoConfiguredTextToSpeechService.setSpeechRate(rate)
    }

    override fun getSpeechRate(): Float = NoConfiguredTextToSpeechService.getSpeechRate()
}
