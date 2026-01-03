package com.guyghost.wakeve.test

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import kotlinx.datetime.Clock

/**
 * Helper function to create a test Event with sensible defaults.
 * Simplifies test creation by providing default values for all new fields.
 */
fun createTestEvent(
    id: String = "test-event-1",
    title: String = "Test Event",
    description: String = "Test Description",
    organizerId: String = "org-1",
    participants: List<String> = emptyList(),
    proposedSlots: List<TimeSlot> = emptyList(),
    deadline: String = "2025-12-31T23:59:59Z",
    status: EventStatus = EventStatus.DRAFT,
    createdAt: String = Clock.System.now().toString(),
    updatedAt: String = Clock.System.now().toString(),
    finalDate: String? = null,
    eventType: EventType = EventType.OTHER,
    eventTypeCustom: String? = null,
    minParticipants: Int? = null,
    maxParticipants: Int? = null,
    expectedParticipants: Int? = null
): Event {
    return Event(
        id = id,
        title = title,
        description = description,
        organizerId = organizerId,
        participants = participants,
        proposedSlots = proposedSlots,
        deadline = deadline,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        finalDate = finalDate,
        eventType = eventType,
        eventTypeCustom = eventTypeCustom,
        minParticipants = minParticipants,
        maxParticipants = maxParticipants,
        expectedParticipants = expectedParticipants
    )
}

/**
 * Helper function to create a test TimeSlot with sensible defaults.
 */
fun createTestTimeSlot(
    id: String = "slot-1",
    start: String? = "2025-01-15T10:00:00Z",
    end: String? = "2025-01-15T12:00:00Z",
    timezone: String = "UTC",
    timeOfDay: TimeOfDay = TimeOfDay.SPECIFIC
): TimeSlot {
    return TimeSlot(
        id = id,
        start = start,
        end = end,
        timezone = timezone,
        timeOfDay = timeOfDay
    )
}

