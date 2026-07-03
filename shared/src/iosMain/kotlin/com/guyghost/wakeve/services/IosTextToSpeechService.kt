package com.guyghost.wakeve.services

import com.guyghost.wakeve.ml.Language
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

/**
 * iOS text-to-speech service backed by AVSpeechSynthesizer.
 */
class IosTextToSpeechService : TextToSpeechService {
    private val synthesizer = AVSpeechSynthesizer()
    private var currentLanguage: Language = Language.FR
    private var speechRate: Float = 1.0f

    override suspend fun speak(
        text: String,
        language: Language,
        queueMode: QueueMode
    ): TTSResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return TTSResult.Error("Text cannot be empty")
        }

        if (!isAvailable()) {
            return TTSResult.Error("Text-to-speech is unavailable on this iOS device")
        }

        if (queueMode == QueueMode.FLUSH || queueMode == QueueMode.INTERRUPT) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }

        currentLanguage = language
        val utterance = AVSpeechUtterance.speechUtteranceWithString(trimmed)
        utterance.setVoice(AVSpeechSynthesisVoice.voiceWithLanguage(language.localeIdentifier))
        utterance.setRate(normalizedSpeechRate())
        synthesizer.speakUtterance(utterance)

        return TTSResult.Success(estimatedDurationMillis(trimmed))
    }

    override suspend fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    override suspend fun pause() {
        synthesizer.pauseSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    override suspend fun resume() {
        synthesizer.continueSpeaking()
    }

    override fun isSpeaking(): Boolean = synthesizer.speaking

    override fun isAvailable(): Boolean = getAvailableLanguages().isNotEmpty()

    override fun setLanguage(language: Language) {
        currentLanguage = language
    }

    override fun getAvailableLanguages(): List<Language> =
        Language.entries.filter { AVSpeechSynthesisVoice.voiceWithLanguage(it.localeIdentifier) != null }

    override fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
    }

    override fun getSpeechRate(): Float = speechRate

    private fun normalizedSpeechRate(): Float = (0.5f * speechRate).coerceIn(0.1f, 1.0f)

    private fun estimatedDurationMillis(text: String): Long {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }.size.coerceAtLeast(1)
        val wordsPerMinute = (160f * speechRate).coerceAtLeast(80f)
        return ((words / wordsPerMinute) * 60_000).toLong().coerceAtLeast(500L)
    }

    private val Language.localeIdentifier: String
        get() = when (this) {
            Language.EN -> "en-US"
            Language.FR -> "fr-FR"
            Language.ES -> "es-ES"
            Language.DE -> "de-DE"
            Language.IT -> "it-IT"
            Language.PT -> "pt-PT"
        }
}
