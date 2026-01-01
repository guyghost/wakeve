package com.guyghost.wakeve.ml

import kotlinx.serialization.Serializable

/**
 * Represents a voice command extracted from user speech.
 *
 * @property intent The recognized intent (e.g., CREATE_EVENT, ADD_SLOT)
 * @property parameters Map of extracted parameters from the command
 * @property confidenceScore Confidence score of the recognition (0.0 - 1.0)
 * @property rawTranscript Original speech transcript
 * @property timestamp ISO timestamp when the command was issued
 */
@Serializable
data class VoiceCommand(
    val intent: VoiceIntent,
    val parameters: Map<String, String>,
    val confidenceScore: Double,
    val rawTranscript: String,
    val timestamp: String
)

/**
 * Supported voice intents for the intelligent voice assistant.
 */
@Serializable
enum class VoiceIntent {
    /** Create a new event */
    CREATE_EVENT,

    /** Set the event title */
    SET_TITLE,

    /** Set the event description */
    SET_DESCRIPTION,

    /** Set the event date */
    SET_DATE,

    /** Set the event participants */
    SET_PARTICIPANTS,

    /** Add a time slot to the poll */
    ADD_SLOT,

    /** Confirm the poll date */
    CONFIRM_POLL,

    /** Send invitations to participants */
    SEND_INVITATIONS,

    /** Open the calendar view */
    OPEN_CALENDAR,

    /** Cancel an event */
    CANCEL_EVENT,

    /** Get statistics about events */
    GET_STATS
}

/**
 * Represents a voice assistant session for multi-step interactions.
 *
 * @property sessionId Unique identifier for this session
 * @property userId ID of the user who owns this session
 * @property commands List of commands issued in this session
 * @property context Current context of the session (event, step, language)
 * @property status Current status of the session
 * @property startTime ISO timestamp when the session started
 * @property endTime ISO timestamp when the session ended (null if active)
 */
@Serializable
data class VoiceSession(
    val sessionId: String,
    val userId: String,
    val commands: List<VoiceCommand>,
    val context: VoiceContext,
    val status: SessionStatus,
    val startTime: String,
    val endTime: String?
)

/**
 * Status of a voice session.
 */
@Serializable
enum class SessionStatus {
    /** Session is active and waiting for commands */
    ACTIVE,

    /** Session has been completed successfully */
    COMPLETED,

    /** Session was cancelled by the user */
    CANCELLED
}

/**
 * Context information for an ongoing voice session.
 *
 * @property eventId ID of the event being created/modified (null if none)
 * @property currentStep Current step in the multi-step creation flow
 * @property language Language being used for this session
 * @property suggestionsProvided Whether suggestions have been provided
 */
@Serializable
data class VoiceContext(
    val eventId: String?,
    val step: VoiceStep,
    val language: Language,
    val suggestionsProvided: Boolean
)

/**
 * Step in the multi-step voice-assisted event creation flow.
 */
@Serializable
enum class VoiceStep {
    /** Collecting event title */
    TITLE,

    /** Collecting event description */
    DESCRIPTION,

    /** Collecting event date */
    DATE,

    /** Collecting participants */
    PARTICIPANTS,

    /** Confirming creation */
    CONFIRM,

    /** Event creation complete */
    COMPLETE
}

/**
 * Supported languages for voice recognition and synthesis.
 */
@Serializable
enum class Language {
    /** English */
    EN,

    /** French */
    FR,

    /** Spanish */
    ES,

    /** German */
    DE,

    /** Italian */
    IT
}

/**
 * Configuration for language-specific behavior.
 *
 * @property code Language enum value
 * @property name Human-readable language name
 * @property locale Locale string (e.g., "en-US", "fr-FR")
 * @property dateFormats List of preferred date formats
 */
data class LanguageConfig(
    val code: Language,
    val name: String,
    val locale: String,
    val dateFormats: List<String>
)
