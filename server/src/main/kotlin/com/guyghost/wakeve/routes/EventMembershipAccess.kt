package com.guyghost.wakeve.routes

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.repository.EventRepositoryInterface

/**
 * Event membership policy shared by event-scoped collaborative routes
 * (activities, comments, equipment): only the event organizer or a participant
 * of the event may read or contribute. Unknown events are treated as "no access"
 * so callers answer 403 without revealing whether the event exists.
 *
 * Originally introduced for activities (QA BUG-4 hardening) and reused for
 * comments and equipment (API multi-user QA 2026-09-27, BUG-A / BUG-B).
 */
internal fun hasEventMemberAccess(
    eventRepository: EventRepositoryInterface,
    database: WakeveDb,
    eventId: String,
    userId: String
): Boolean {
    val event = eventRepository.getEvent(eventId) ?: return false
    if (event.organizerId == userId) {
        return true
    }
    return database.participantQueries
        .selectByEventIdAndUserId(eventId, userId)
        .executeAsOneOrNull() != null
}

/** True when [userId] is the organizer of [eventId]. */
internal fun isEventOrganizerOf(
    eventRepository: EventRepositoryInterface,
    eventId: String,
    userId: String
): Boolean = eventRepository.getEvent(eventId)?.organizerId == userId
