package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventType

/**
 * Result type for event draft validation.
 *
 * Represents the outcome of validating an event's draft fields.
 */
sealed class ValidationResult {
    /**
     * Event validation succeeded - all fields are valid.
     */
    data object Success : ValidationResult()

    /**
     * Event validation failed.
     *
     * @property message Error message describing what validation failed
     */
    data class Error(val message: String) : ValidationResult()
}

/**
 * Use case for validating draft events.
 *
 * Validates that an event has all required fields and respects business rules:
 * - Title and description are required (non-empty)
 * - If minParticipants and maxParticipants provided: max >= min
 * - All participant counts must be non-negative
 * - If eventType = CUSTOM, eventTypeCustom must be provided
 *
 * ## Usage
 *
 * ```kotlin
 * val validateEventDraft = ValidateEventDraftUseCase()
 * val result = validateEventDraft(event)
 * when (result) {
 *     is ValidationResult.Success -> println("Event is valid")
 *     is ValidationResult.Error -> println("Error: ${result.message}")
 * }
 * ```
 */
class ValidateEventDraftUseCase {
    /**
     * Validate event draft fields.
     *
     * @param event The event to validate
     * @return ValidationResult.Success if valid, ValidationResult.Error with message if invalid
     */
    operator fun invoke(event: Event): ValidationResult {
        // Title is required
        if (event.title.isBlank()) {
            return ValidationResult.Error("Title is required")
        }

        // Description is required
        if (event.description.isBlank()) {
            return ValidationResult.Error("Description is required")
        }

        // CUSTOM event type requires eventTypeCustom
        if (event.eventType == EventType.CUSTOM && event.eventTypeCustom.isNullOrBlank()) {
            return ValidationResult.Error("Custom event type requires a description")
        }

        // Participant counts validation
        if (event.minParticipants != null && event.minParticipants < 0) {
            return ValidationResult.Error("Participants counts must be positive")
        }

        if (event.maxParticipants != null && event.maxParticipants < 0) {
            return ValidationResult.Error("Participants counts must be positive")
        }

        if (event.expectedParticipants != null && event.expectedParticipants < 0) {
            return ValidationResult.Error("Participants counts must be positive")
        }

        // maxParticipants must be >= minParticipants
        if (event.minParticipants != null && event.maxParticipants != null) {
            if (event.maxParticipants < event.minParticipants) {
                return ValidationResult.Error("Max participants must be >= min participants")
            }
        }

        return ValidationResult.Success
    }
}
