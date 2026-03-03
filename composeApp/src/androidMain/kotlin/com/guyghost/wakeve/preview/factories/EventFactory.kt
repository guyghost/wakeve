package com.guyghost.wakeve.preview.factories

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot

/**
 * Factory for creating preview/test Event instances.
 *
 * All properties use `get()` to return fresh instances on each access.
 * Dates use ISO 8601 string literals for simplicity.
 */
object EventFactory {

    // ---- Reusable time slots ------------------------------------------------

    private val futureSlotA
        get() = TimeSlot(
            id = "slot-001",
            start = "2026-04-12T10:00:00Z",
            end = "2026-04-12T18:00:00Z",
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.SPECIFIC
        )

    private val futureSlotB
        get() = TimeSlot(
            id = "slot-002",
            start = "2026-04-19T14:00:00Z",
            end = "2026-04-19T22:00:00Z",
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.AFTERNOON
        )

    private val futureSlotC
        get() = TimeSlot(
            id = "slot-003",
            start = "2026-04-26T09:00:00Z",
            end = "2026-04-26T17:00:00Z",
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.MORNING
        )

    private val pastSlotA
        get() = TimeSlot(
            id = "slot-past-001",
            start = "2025-12-20T18:00:00Z",
            end = "2025-12-20T23:00:00Z",
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.EVENING
        )

    private val pastSlotB
        get() = TimeSlot(
            id = "slot-past-002",
            start = "2025-12-21T10:00:00Z",
            end = "2025-12-21T16:00:00Z",
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.SPECIFIC
        )

    // ---- Factory properties -------------------------------------------------

    /** An empty draft event with no participants or slots. */
    val empty
        get() = Event(
            id = "event-draft-001",
            title = "",
            description = "",
            organizerId = "user-organizer-001",
            participants = emptyList(),
            proposedSlots = emptyList(),
            deadline = "2026-04-01T23:59:59Z",
            status = EventStatus.DRAFT,
            createdAt = "2026-03-01T08:00:00Z",
            updatedAt = "2026-03-01T08:00:00Z",
            eventType = EventType.OTHER
        )

    /** A fully confirmed event with 5 participants and 2 time slots. */
    val complete
        get() = Event(
            id = "event-confirmed-002",
            title = "Weekend Hiking Trip",
            description = "A two-day hike in the Vosges mountains with overnight camping.",
            organizerId = "user-organizer-001",
            participants = listOf(
                "user-participant-002",
                "user-email-003",
                "user-group-004",
                "user-group-005",
                "user-group-006"
            ),
            proposedSlots = listOf(futureSlotA, futureSlotB),
            deadline = "2026-04-05T23:59:59Z",
            status = EventStatus.CONFIRMED,
            finalDate = "2026-04-12T10:00:00Z",
            createdAt = "2026-03-01T10:00:00Z",
            updatedAt = "2026-03-10T14:30:00Z",
            eventType = EventType.OUTDOOR_ACTIVITY,
            minParticipants = 3,
            maxParticipants = 10,
            expectedParticipants = 6,
            heroImageUrl = null
        )

    /** An event in polling phase with 8 participants and 3 proposed slots. */
    val polling
        get() = Event(
            id = "event-polling-003",
            title = "Team Offsite Q2",
            description = "Quarterly team-building day: activities, lunch, and retrospective.",
            organizerId = "user-organizer-001",
            participants = (1..8).map { "user-polling-${it.toString().padStart(3, '0')}" },
            proposedSlots = listOf(futureSlotA, futureSlotB, futureSlotC),
            deadline = "2026-04-08T23:59:59Z",
            status = EventStatus.POLLING,
            createdAt = "2026-03-02T09:00:00Z",
            updatedAt = "2026-03-03T11:00:00Z",
            eventType = EventType.TEAM_BUILDING,
            expectedParticipants = 8
        )

    /** A finalized event with dates in the past. */
    val past
        get() = Event(
            id = "event-past-004",
            title = "Holiday Dinner 2025",
            description = "End-of-year team dinner at Le Petit Bistrot.",
            organizerId = "user-organizer-001",
            participants = listOf(
                "user-participant-002",
                "user-email-003",
                "user-group-004"
            ),
            proposedSlots = listOf(pastSlotA, pastSlotB),
            deadline = "2025-12-15T23:59:59Z",
            status = EventStatus.FINALIZED,
            finalDate = "2025-12-20T18:00:00Z",
            createdAt = "2025-11-20T12:00:00Z",
            updatedAt = "2025-12-20T23:00:00Z",
            eventType = EventType.PARTY,
            expectedParticipants = 4
        )

    /**
     * Returns a mixed list of events covering all key statuses.
     */
    fun mixedList(): List<Event> = listOf(empty, polling, complete, past)
}
