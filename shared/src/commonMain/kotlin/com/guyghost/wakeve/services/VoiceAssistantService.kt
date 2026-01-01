package com.guyghost.wakeve.services

import com.guyghost.wakeve.ml.Language
import com.guyghost.wakeve.ml.VoiceCommand
import com.guyghost.wakeve.ml.VoiceSession
import com.guyghost.wakeve.models.EventType

/**
 * Result type for voice assistant operations.
 * Provides type-safe success/error handling.
 */
sealed class VoiceResult<out T> {
    /** Successful operation with data */
    data class Success<T>(val data: T) : VoiceResult<T>()

    /** Failed operation with exception */
    data class Error(val exception: Exception) : VoiceResult<Nothing>()
}

/**
 * Interface for the intelligent voice assistant service.
 * Provides voice-based event management with multi-step conversation support.
 *
 * This interface is implemented on each platform using:
 * - iOS: Speech Framework for recognition, native TTS for synthesis
 * - Android: SpeechRecognizer API for recognition, TextToSpeech for synthesis
 * - Shared: NLP logic for intent classification and entity extraction
 */
interface VoiceAssistantService {
    /**
     * Start a new voice assistant session.
     * Sessions track the multi-step conversation flow for event creation.
     *
     * @param userId ID of the user starting the session
     * @param language Language for speech recognition (default: FR)
     * @return The newly created session
     */
    suspend fun startSession(
        userId: String,
        language: Language = Language.FR
    ): VoiceResult<VoiceSession>

    /**
     * Process a voice command transcript and extract intent/parameters.
     * Uses NLP to classify intent and extract entities like dates and counts.
     *
     * @param sessionId ID of the active session
     * @param transcript Text transcript of the voice command
     * @return Processed voice command with intent and parameters
     */
    suspend fun processCommand(
        sessionId: String,
        transcript: String
    ): VoiceResult<VoiceCommand>

    /**
     * Get contextual suggestions based on the current event being created.
     * Suggestions are personalized based on event type, location, and preferences.
     *
     * @param eventId ID of the event (null if new event)
     * @param eventType Type of event being created (null if unknown)
     * @return List of contextual suggestions
     */
    suspend fun getContextualSuggestions(
        eventId: String?,
        eventType: EventType?
    ): VoiceResult<List<String>>

    /**
     * End an active voice session.
     * Marks the session as completed or cancelled.
     *
     * @param sessionId ID of the session to end
     * @return The completed session
     */
    suspend fun endSession(
        sessionId: String
    ): VoiceResult<VoiceSession>
}
