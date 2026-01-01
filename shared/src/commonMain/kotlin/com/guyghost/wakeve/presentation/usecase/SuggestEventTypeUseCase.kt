package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.models.EventType

/**
 * Use case for providing preset event type suggestions.
 *
 * Returns the list of predefined event types, excluding CUSTOM.
 * The CUSTOM type is reserved for user-entered custom types.
 *
 * ## Usage
 *
 * ```kotlin
 * val suggestEventType = SuggestEventTypeUseCase()
 * val types = suggestEventType()
 * // types = [BIRTHDAY, WEDDING, TEAM_BUILDING, ...]
 * ```
 */
class SuggestEventTypeUseCase {
    /**
     * Get list of preset event types.
     *
     * @return List of preset event types in consistent order (excluding CUSTOM)
     */
    operator fun invoke(): List<EventType> {
        return listOf(
            EventType.BIRTHDAY,
            EventType.WEDDING,
            EventType.TEAM_BUILDING,
            EventType.CONFERENCE,
            EventType.WORKSHOP,
            EventType.PARTY,
            EventType.SPORTS_EVENT,
            EventType.CULTURAL_EVENT,
            EventType.FAMILY_GATHERING,
            EventType.OTHER
        )
    }
}
