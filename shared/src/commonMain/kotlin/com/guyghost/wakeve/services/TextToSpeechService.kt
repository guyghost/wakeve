package com.guyghost.wakeve.services

import com.guyghost.wakeve.ml.Language

/**
 * Service interface for Text-to-Speech (TTS) functionality.
 * Provides voice feedback for the Wakeve voice assistant.
 *
 * This interface is implemented on each platform using:
 * - Android: android.speech.tts.TextToSpeech
 * - iOS: AVFoundation.AVSpeechSynthesizer
 *
 * Supports multiple languages (FR, EN, ES, DE, IT) with configurable
 * speech rate and queue management for spoken messages.
 */
interface TextToSpeechService {
    
    /**
     * Speaks the given text aloud.
     *
     * @param text The text to be spoken
     * @param language The language for speech synthesis (default: FR)
     * @param queueMode How to handle the speaking queue (default: ADD)
     * @return TTSResult indicating success with duration or error message
     */
    suspend fun speak(
        text: String,
        language: Language = Language.FR,
        queueMode: QueueMode = QueueMode.ADD
    ): TTSResult
    
    /**
     * Stops any ongoing speech immediately.
     */
    suspend fun stop()
    
    /**
     * Pauses the current speech utterance.
     * Note: Android TTS does not support native pause/resume.
     */
    suspend fun pause()
    
    /**
     * Resumes paused speech.
     * Note: Android TTS does not support native pause/resume.
     */
    suspend fun resume()
    
    /**
     * Returns whether the TTS engine is currently speaking.
     *
     * @return true if speaking, false otherwise
     */
    fun isSpeaking(): Boolean
    
    /**
     * Returns whether the TTS service is available on this device.
     *
     * @return true if TTS is available, false otherwise
     */
    fun isAvailable(): Boolean
    
    /**
     * Sets the current language for speech synthesis.
     *
     * @param language The language to use
     */
    fun setLanguage(language: Language)
    
    /**
     * Returns the list of supported languages on this device.
     *
     * @return List of available Language enum values
     */
    fun getAvailableLanguages(): List<Language>
    
    /**
     * Sets the speech rate for synthesis.
     *
     * @param rate Speech rate between 0.5f (slower) and 2.0f (faster)
     */
    fun setSpeechRate(rate: Float)
    
    /**
     * Returns the current speech rate.
     *
     * @return Current speech rate (0.5f to 2.0f)
     */
    fun getSpeechRate(): Float
}

/**
 * Defines how new speech requests are handled relative to the current queue.
 */
enum class QueueMode {
    /** Add to the end of the queue */
    ADD,
    
    /** Clear the queue and speak immediately */
    FLUSH,
    
    /** Stop current speech and speak immediately */
    INTERRUPT
}

/**
 * Represents the current status of the TTS engine.
 */
enum class TTSStatus {
    /** Not speaking and ready */
    IDLE,
    
    /** Currently speaking */
    SPEAKING,
    
    /** Speech is paused */
    PAUSED,
    
    /** An error occurred */
    ERROR
}

/**
 * Result type for TTS operations.
 * Provides type-safe success/error handling for speech synthesis.
 */
sealed class TTSResult {
    /**
     * Successful speech synthesis.
     *
     * @property durationMs The duration of the speech in milliseconds
     */
    data class Success(val durationMs: Long) : TTSResult()
    
    /**
     * Failed speech synthesis.
     *
     * @property message Error message describing what went wrong
     */
    data class Error(val message: String) : TTSResult()
}
