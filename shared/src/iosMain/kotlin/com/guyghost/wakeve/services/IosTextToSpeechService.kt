package com.guyghost.wakeve.services

import com.guyghost.wakeve.ml.Language
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVSpeechBoundary
import platform.AVFoundation.AVSpeechSynthesizer
import platform.AVFoundation.AVSpeechSynthesizerDelegateProtocol
import platform.AVFoundation.AVSpeechSynthesisVoice
import platform.AVFoundation.AVSpeechUtterance
import platform.Foundation.NSUUID

/**
 * iOS implementation of TextToSpeechService.
 * Uses AVFoundation's AVSpeechSynthesizer for voice feedback.
 *
 * Provides native iOS TTS with support for multiple languages,
 * pause/resume functionality, and precise speech control.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class IosTextToSpeechService : TextToSpeechService {
    
    private val synthesizer = AVSpeechSynthesizer()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentLanguage: Language = Language.FR
    private var speechRate: Float = 1.0f
    private var isSpeaking: Boolean = false
    private var isPaused: Boolean = false
    private var currentUtteranceId: String? = null
    
    init {
        // Set up delegate for tracking speech progress
        synthesizer.delegate = object : AVSpeechSynthesizerDelegateProtocol {
            override fun speechSynthesizer(
                synthesizer: AVSpeechSynthesizer,
                didFinishSpeechUtterance: AVSpeechUtterance
            ) {
                isSpeaking = false
                isPaused = false
                currentUtteranceId = null
            }
            
            override fun speechSynthesizer(
                synthesizer: AVSpeechSynthesizer,
                didStartSpeechUtterance: AVSpeechUtterance
            ) {
                isSpeaking = true
                isPaused = false
            }
            
            override fun speechSynthesizer(
                synthesizer: AVSpeechSynthesizer,
                didPauseSpeechUtterance: AVSpeechUtterance
            ) {
                isPaused = true
                isSpeaking = false
            }
            
            override fun speechSynthesizer(
                synthesizer: AVSpeechSynthesizer,
                didContinueSpeechUtterance: AVSpeechUtterance
            ) {
                isSpeaking = true
                isPaused = false
            }
            
            override fun speechSynthesizer(
                synthesizer: AVSpeechSynthesizer,
                didCancelSpeechUtterance: AVSpeechUtterance
            ) {
                isSpeaking = false
                isPaused = false
                currentUtteranceId = null
            }
        }
    }
    
    /**
     * Converts a Language enum to an iOS language code.
     *
     * @param language The language to convert
     * @return The corresponding BCP 47 language code
     */
    private fun getLanguageCode(language: Language): String {
        return when (language) {
            Language.FR -> "fr-FR"
            Language.EN -> "en-US"
            Language.ES -> "es-ES"
            Language.DE -> "de-DE"
            Language.IT -> "it-IT"
        }
    }
    
    override suspend fun speak(
        text: String,
        language: Language,
        queueMode: QueueMode
    ): TTSResult = withContext(Dispatchers.Main) {
        try {
            // Handle queue modes
            when (queueMode) {
                QueueMode.INTERRUPT -> {
                    synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.immediate)
                    isSpeaking = false
                    isPaused = false
                }
                QueueMode.FLUSH -> {
                    synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.immediate)
                    isSpeaking = false
                    isPaused = false
                }
                QueueMode.ADD -> {
                    // Do nothing, just add to queue
                }
            }
            
            // Create speech utterance
            val utterance = AVSpeechUtterance.speechUtteranceWithString(text).apply {
                this.voice = AVSpeechSynthesisVoice.voiceWithLanguage(getLanguageCode(language))
                this.rate = speechRate * AVSpeechUtteranceDefaultSpeechRate
                this.pitchMultiplier = 1.0f
                this.volume = 1.0f
                this.utteranceIdentifier = NSUUID().UUIDString()
            }
            
            currentUtteranceId = utterance.utteranceIdentifier
            
            val startTime = System.currentTimeMillis()
            
            synthesizer.speakUtterance(utterance)
            
            // Wait for speech to complete
            // Using a simple polling approach since we have the delegate callbacks
            while (isSpeaking && !isPaused) {
                delay(100)
            }
            
            // Add a small buffer to ensure complete speech
            delay(100)
            
            val durationMs = System.currentTimeMillis() - startTime
            TTSResult.Success(durationMs)
        } catch (e: Exception) {
            TTSResult.Error(e.message ?: "Unknown TTS error")
        }
    }
    
    override suspend fun stop() {
        withContext(Dispatchers.Main) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.immediate)
            isSpeaking = false
            isPaused = false
            currentUtteranceId = null
        }
    }
    
    override suspend fun pause() {
        withContext(Dispatchers.Main) {
            synthesizer.pauseSpeakingAtBoundary(AVSpeechBoundary.immediate)
        }
    }
    
    override suspend fun resume() {
        withContext(Dispatchers.Main) {
            synthesizer.continueSpeaking()
        }
    }
    
    override fun isSpeaking(): Boolean = isSpeaking
    
    override fun isAvailable(): Boolean = true  // AVSpeechSynthesizer is always available on iOS
    
    override fun setLanguage(language: Language) {
        currentLanguage = language
    }
    
    override fun getAvailableLanguages(): List<Language> {
        // iOS supports all our languages by default
        return Language.entries
    }
    
    override fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
    }
    
    override fun getSpeechRate(): Float = speechRate
    
    companion object {
        private const val MIN_SPEECH_RATE = 0.5f
        private const val MAX_SPEECH_RATE = 2.0f
    }
}
