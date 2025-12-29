package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.models.Event

/**
 * Use case for creating a new event in the repository.
 *
 * This use case validates the event and persists it to the repository.
 * It's used by EventManagementStateMachine when handling CreateEvent intent.
 *
 * ## Validation Rules
 *
 * - Event ID must not be empty
 * - Event title must not be empty
 * - Organizer ID must not be empty
 * - At least one proposed slot is required
 * - Deadline must not be empty
 *
 * ## Usage
 *
 * ```kotlin
 * val createEventUseCase = CreateEventUseCase(eventRepository)
 * val event = Event(
 *     id = "evt-123",
 *     title = "Team Meeting",
 *     description = "Quarterly sync",
 *     organizerId = "user-1",
 *     proposedSlots = listOf(...),
 *     deadline = "2025-12-31T18:00:00Z",
 *     status = EventStatus.DRAFT,
 *     createdAt = "2025-12-01T10:00:00Z",
 *     updatedAt = "2025-12-01T10:00:00Z"
 * )
 * val result: Result<Event> = createEventUseCase(event)
 * ```
 *
 * @property eventRepository The repository to save the event to (nullable)
 */
class CreateEventUseCase(
    private val eventRepository: EventRepositoryInterface?
) {
    /**
     * Create and save a new event.
     *
     * @param event The event to create
     * @return Result containing the created event, or failure if validation or save failed
     */
    suspend operator fun invoke(event: Event): Result<Event> {
        // Validation
        val validationError = validateEvent(event)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        // Create in repository
        return eventRepository?.createEvent(event) ?: Result.failure(
            IllegalStateException("EventRepository is not available")
        )
    }

    /**
     * Validate event fields.
     *
     * @param event The event to validate
     * @return Error message if invalid, null if valid
     */
    private fun validateEvent(event: Event): String? {
        return when {
            event.id.isBlank() -> "Event ID cannot be empty"
            event.title.isBlank() -> "Event title cannot be empty"
            event.organizerId.isBlank() -> "Organizer ID cannot be empty"
            event.proposedSlots.isEmpty() -> "At least one time slot is required"
            event.deadline.isBlank() -> "Deadline cannot be empty"
            else -> null
        }
    }
}
