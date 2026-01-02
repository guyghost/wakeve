package com.guyghost.wakeve.voice.handlers

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.ml.Language
import com.guyghost.wakeve.ml.VoiceCommand
import com.guyghost.wakeve.ml.VoiceContext
import com.guyghost.wakeve.ml.VoiceSession
import com.guyghost.wakeve.ml.VoiceStep
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.TimeOfDay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Handler for event creation and management voice commands.
 * Handles the multi-step process of creating events through voice.
 */
class VoiceEventHandlers(
    private val eventRepository: EventRepositoryInterface
) {
    private fun generateId(): String = "id_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"

    /**
     * Handles the CREATE_EVENT intent.
     * Initializes a new event creation session and extracts any event type from the command.
     *
     * @param session The current voice session
     * @param command The parsed voice command
     * @return Result containing the created event ID or error
     */
    suspend fun handleCreateEvent(
        session: VoiceSession,
        command: VoiceCommand
    ): Result<String> {
        return try {
            val parameters = command.parameters
            val eventType = parameters["eventType"]?.let {
                runCatching { EventType.valueOf(it) }.getOrNull()
            } ?: EventType.OTHER

            val title = parameters["title"] ?: extractTitleFromCommand(command.rawTranscript)

            // Create a new event in DRAFT status
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val eventId = generateId()

            val event = Event(
                id = eventId,
                title = title ?: "Nouvel événement",
                description = "",
                organizerId = session.userId,
                participants = emptyList(),
                proposedSlots = emptyList(),
                deadline = "2026-01-09 10:00:00", // 7 days from now (simplified)
                status = EventStatus.DRAFT,
                finalDate = null,
                createdAt = getCurrentTimestamp(),
                updatedAt = getCurrentTimestamp(),
                eventType = eventType,
                eventTypeCustom = null,
                minParticipants = null,
                maxParticipants = null,
                expectedParticipants = null
            )

            eventRepository.createEvent(event)
            Result.success(eventId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Handles the SET_TITLE intent.
     * Updates the event with the provided title.
     *
     * @param session The current voice session
     * @param command The parsed voice command
     * @return Result indicating success or failure
     */
    suspend fun handleSetTitle(
        session: VoiceSession,
        command: VoiceCommand
    ): Result<Unit> {
        val eventId = session.context.eventId ?: return Result.failure(
            IllegalStateException("No event in progress")
        )

        val event = eventRepository.getEvent(eventId) ?: return Result.failure(
            IllegalStateException("Event not found")
        )

        val title = command.parameters["title"]
            ?: extractTitleFromCommand(command.rawTranscript)
            ?: return Result.failure(IllegalArgumentException("No title provided"))

        val updatedEvent = event.copy(
            title = title,
            updatedAt = getCurrentTimestamp()
        )

        return eventRepository.updateEvent(updatedEvent).map { }
    }

    /**
     * Handles the SET_DESCRIPTION intent.
     * Updates the event with the provided description.
     *
     * @param session The current voice session
     * @param command The parsed voice command
     * @return Result indicating success or failure
     */
    suspend fun handleSetDescription(
        session: VoiceSession,
        command: VoiceCommand
    ): Result<Unit> {
        val eventId = session.context.eventId ?: return Result.failure(
            IllegalStateException("No event in progress")
        )

        val event = eventRepository.getEvent(eventId) ?: return Result.failure(
            IllegalStateException("Event not found")
        )

        val description = command.parameters["description"]
            ?: command.rawTranscript
            ?: return Result.failure(IllegalArgumentException("No description provided"))

        val updatedEvent = event.copy(
            description = description,
            updatedAt = getCurrentTimestamp()
        )

        return eventRepository.updateEvent(updatedEvent).map { }
    }

    /**
     * Handles the SET_DATE intent.
     * Updates the event with the provided date and creates an initial time slot.
     *
     * @param session The current voice session
     * @param command The parsed voice command
     * @return Result containing the created time slot ID or error
     */
    suspend fun handleSetDate(
        session: VoiceSession,
        command: VoiceCommand
    ): Result<String> {
        val eventId = session.context.eventId ?: return Result.failure(
            IllegalStateException("No event in progress")
        )

        val event = eventRepository.getEvent(eventId) ?: return Result.failure(
            IllegalStateException("Event not found")
        )

        val dateString = command.parameters["date"]
            ?: return Result.failure(IllegalArgumentException("No date provided"))

        val timeOfDay = command.parameters["timeOfDay"]?.let {
            runCatching { TimeOfDay.valueOf(it) }.getOrNull()
        } ?: TimeOfDay.SPECIFIC

        // Create a time slot for the proposed date
        val slotId = generateId()
        val timeZone = TimeZone.currentSystemDefault().id

        val timeSlot = TimeSlot(
            id = slotId,
            start = dateString,
            end = null,
            timezone = timeZone,
            timeOfDay = timeOfDay
        )

        val updatedEvent = event.copy(
            proposedSlots = event.proposedSlots + timeSlot,
            updatedAt = getCurrentTimestamp()
        )

        eventRepository.updateEvent(updatedEvent)
        return Result.success(slotId)
    }

    /**
     * Handles the SET_PARTICIPANTS intent.
     * Updates the event with the expected participant count.
     *
     * @param session The current voice session
     * @param command The parsed voice command
     * @return Result indicating success or failure
     */
    suspend fun handleSetParticipants(
        session: VoiceSession,
        command: VoiceCommand
    ): Result<Unit> {
        val eventId = session.context.eventId ?: return Result.failure(
            IllegalStateException("No event in progress")
        )

        val event = eventRepository.getEvent(eventId) ?: return Result.failure(
            IllegalStateException("Event not found")
        )

        val participantCount = command.parameters["participantCount"]?.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("No participant count provided"))

        val updatedEvent = event.copy(
            expectedParticipants = participantCount,
            updatedAt = getCurrentTimestamp()
        )

        return eventRepository.updateEvent(updatedEvent).map { }
    }

    /**
     * Finalizes event creation after user confirmation.
     * Transitions the event from DRAFT to POLLING status.
     *
     * @param session The current voice session
     * @param command The parsed voice command (should contain confirmation)
     * @return Result containing the final event or error
     */
    suspend fun finalizeEventCreation(
        session: VoiceSession,
        command: VoiceCommand
    ): Result<Event> {
        val eventId = session.context.eventId ?: return Result.failure(
            IllegalStateException("No event in progress")
        )

        val event = eventRepository.getEvent(eventId) ?: return Result.failure(
            IllegalStateException("Event not found")
        )

        // Validate that all required fields are set
        if (event.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Event title is required"))
        }

        if (event.proposedSlots.isEmpty()) {
            return Result.failure(IllegalArgumentException("At least one time slot is required"))
        }

        // Transition to POLLING status to start the voting process
        val updatedEvent = event.copy(
            status = EventStatus.POLLING,
            updatedAt = getCurrentTimestamp()
        )

        eventRepository.updateEventStatus(
            id = eventId,
            status = EventStatus.POLLING,
            finalDate = null
        )

        return Result.success(updatedEvent)
    }

    /**
     * Extracts a title from the raw command transcript.
     * Removes common prefixes and cleans up the text.
     *
     * @param rawTranscript The raw voice transcript
     * @return Extracted title or null
     */
    private fun extractTitleFromCommand(rawTranscript: String): String? {
        // Remove common prefixes
        val cleaned = rawTranscript
            .replace(Regex("""^crée(?: un)?\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^create\s*(?:an?)?\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^organize\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^plan\s*""", RegexOption.IGNORE_CASE), "")
            .trim()

        return cleaned.takeIf { it.isNotBlank() && it.length >= 3 }
    }

    /**
     * Gets the current timestamp in ISO format.
     */
    private fun getCurrentTimestamp(): String {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            .toString()
            .replace("T", " ")
            .substringBefore(".")
    }
}
