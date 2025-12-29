package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.models.Event

/**
 * Use case for loading all events from the repository.
 *
 * This is a simple use case that queries the repository for all events.
 * It's used by EventManagementStateMachine when handling LoadEvents intent.
 *
 * ## Usage
 *
 * ```kotlin
 * val loadEventsUseCase = LoadEventsUseCase(eventRepository)
 * val result: Result<List<Event>> = loadEventsUseCase()
 * ```
 *
 * @property eventRepository The repository to load events from
 */
class LoadEventsUseCase(
    private val eventRepository: EventRepositoryInterface
) {
    /**
     * Load all events.
     *
     * @return Result containing list of events, or failure if operation failed
     */
    operator fun invoke(): Result<List<Event>> {
        return try {
            val events = eventRepository.getAllEvents()
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
