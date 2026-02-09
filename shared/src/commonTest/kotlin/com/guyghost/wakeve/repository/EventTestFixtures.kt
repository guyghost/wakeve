package com.guyghost.wakeve.repository

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.TimeOfDay
import kotlinx.datetime.Clock

object EventTestFixtures {
    
    private val sampleTimeSlot = TimeSlot(
        id = "slot-default",
        start = "2025-12-01T10:00:00Z",
        end = "2025-12-01T12:00:00Z",
        timezone = "UTC",
        timeOfDay = TimeOfDay.SPECIFIC
    )
    
    private val flexibleTimeSlot = TimeSlot(
        id = "slot-flexible",
        start = null,
        end = null,
        timezone = "UTC",
        timeOfDay = TimeOfDay.AFTERNOON
    )
    
    private fun createBaseEvent(
        id: String = "event-test-${Clock.System.now().toEpochMilliseconds()}",
        organizerId: String = "user-1",
        title: String = "Test Event",
        description: String = "Test Description",
        status: EventStatus = EventStatus.DRAFT,
        eventType: EventType = EventType.OTHER,
        eventTypeCustom: String? = null,
        minParticipants: Int? = null,
        maxParticipants: Int? = null,
        expectedParticipants: Int? = null,
        heroImageUrl: String? = null,
        proposedSlots: List<TimeSlot> = listOf(sampleTimeSlot),
        deadline: String = "2025-12-31T23:59:59Z"
    ): Event {
        val now = "2025-11-12T10:00:00Z" // Consistent test timestamp
        return Event(
            id = id,
            title = title,
            description = description,
            organizerId = organizerId,
            participants = emptyList(),
            proposedSlots = proposedSlots,
            deadline = deadline,
            status = status,
            finalDate = null,
            createdAt = now,
            updatedAt = now,
            eventType = eventType,
            eventTypeCustom = eventTypeCustom,
            minParticipants = minParticipants,
            maxParticipants = maxParticipants,
            expectedParticipants = expectedParticipants,
            heroImageUrl = heroImageUrl
        )
    }
    
    fun createSampleEvent(
        id: String = "event-test-${Clock.System.now().toEpochMilliseconds()}",
        organizerId: String = "user-1",
        title: String = "Test Event",
        description: String = "Test Description",
        status: EventStatus = EventStatus.DRAFT,
        eventType: EventType = EventType.OTHER,
        eventTypeCustom: String? = null,
        minParticipants: Int? = null,
        maxParticipants: Int? = null,
        expectedParticipants: Int? = null,
        heroImageUrl: String? = null,
        proposedSlots: List<TimeSlot> = listOf(sampleTimeSlot),
        deadline: String = "2025-12-31T23:59:59Z"
    ): Event {
        return createBaseEvent(
            id = id,
            organizerId = organizerId,
            title = title,
            description = description,
            status = status,
            eventType = eventType,
            eventTypeCustom = eventTypeCustom,
            minParticipants = minParticipants,
            maxParticipants = maxParticipants,
            expectedParticipants = expectedParticipants,
            heroImageUrl = heroImageUrl,
            proposedSlots = proposedSlots,
            deadline = deadline
        )
    }
    
    fun createDraftEvent(organizerId: String = "user-1", id: String? = null): Event {
        return createBaseEvent(
            id = id ?: "draft-event-${Clock.System.now().toEpochMilliseconds()}",
            status = EventStatus.DRAFT,
            organizerId = organizerId
        )
    }
    
    fun createPollingEvent(organizerId: String = "user-1", id: String? = null): Event {
        return createBaseEvent(
            id = id ?: "polling-event-${Clock.System.now().toEpochMilliseconds()}",
            status = EventStatus.POLLING,
            organizerId = organizerId
        ).copy(participants = listOf("user-2", "user-3"))
    }
    
    fun createConfirmedEvent(organizerId: String = "user-1", id: String? = null): Event {
        return createBaseEvent(
            id = id ?: "confirmed-event-${Clock.System.now().toEpochMilliseconds()}",
            status = EventStatus.CONFIRMED,
            organizerId = organizerId
        ).copy(finalDate = "2025-12-15T10:00:00Z")
    }
    
    fun createCustomEvent(
        customType: String = "Custom Workshop",
        organizerId: String = "user-1",
        id: String? = null
    ): Event {
        return createBaseEvent(
            id = id ?: "custom-event-${Clock.System.now().toEpochMilliseconds()}",
            eventType = EventType.CUSTOM,
            eventTypeCustom = customType,
            organizerId = organizerId
        )
    }
    
    fun createTeamBuildingEvent(
        minParticipants: Int = 5,
        maxParticipants: Int = 20,
        expectedParticipants: Int = 12,
        organizerId: String = "user-1",
        id: String? = null
    ): Event {
        return createBaseEvent(
            id = id ?: "team-event-${Clock.System.now().toEpochMilliseconds()}",
            eventType = EventType.TEAM_BUILDING,
            minParticipants = minParticipants,
            maxParticipants = maxParticipants,
            expectedParticipants = expectedParticipants,
            organizerId = organizerId
        )
    }
    
    fun createEventWithHeroImage(
        imageUrl: String = "https://example.com/hero.jpg",
        organizerId: String = "user-1",
        id: String? = null
    ): Event {
        return createBaseEvent(
            id = id ?: "hero-event-${Clock.System.now().toEpochMilliseconds()}",
            heroImageUrl = imageUrl,
            organizerId = organizerId
        )
    }
    
    fun createEventWithFlexibleTimeSlots(
        organizerId: String = "user-1",
        id: String? = null
    ): Event {
        return createBaseEvent(
            id = id ?: "flexible-event-${Clock.System.now().toEpochMilliseconds()}",
            proposedSlots = listOf(
                TimeSlot(
                    id = "morning-slot",
                    start = null,
                    end = null,
                    timezone = "UTC",
                    timeOfDay = TimeOfDay.MORNING
                ),
                TimeSlot(
                    id = "afternoon-slot",
                    start = null,
                    end = null,
                    timezone = "UTC",
                    timeOfDay = TimeOfDay.AFTERNOON
                )
            ),
            organizerId = organizerId
        )
    }
    
    fun createBatchEvents(count: Int): List<Event> {
        return (1..count).map { index ->
            createBaseEvent(
                id = "event-batch-$index",
                title = "Event $index",
                description = "Description for event $index"
            )
        }
    }
    
    fun createEventsWithDifferentStatuses(organizerId: String = "user-1"): Map<EventStatus, Event> {
        return mapOf(
            EventStatus.DRAFT to createDraftEvent(organizerId, "draft-test"),
            EventStatus.POLLING to createPollingEvent(organizerId, "polling-test"),
            EventStatus.COMPARING to createBaseEvent(
                id = "comparing-test",
                status = EventStatus.COMPARING,
                organizerId = organizerId
            ),
            EventStatus.CONFIRMED to createConfirmedEvent(organizerId, "confirmed-test"),
            EventStatus.ORGANIZING to createBaseEvent(
                id = "organizing-test",
                status = EventStatus.ORGANIZING,
                organizerId = organizerId
            ),
            EventStatus.FINALIZED to createBaseEvent(
                id = "finalized-test",
                status = EventStatus.FINALIZED,
                organizerId = organizerId
            )
        )
    }
    
    fun createEventsWithAllTypes(organizerId: String = "user-1"): Map<EventType, Event> {
        return EventType.values().associate { eventType ->
            when (eventType) {
                EventType.CUSTOM -> eventType to createCustomEvent("Custom Test Event", organizerId, "custom-type-test")
                else -> eventType to createBaseEvent(
                    id = "${eventType.name.lowercase()}-test",
                    eventType = eventType,
                    organizerId = organizerId
                )
            }
        }
    }
    
    fun createEventWithPastDeadline(organizerId: String = "user-1"): Event {
        return createBaseEvent(
            id = "past-deadline-${Clock.System.now().toEpochMilliseconds()}",
            status = EventStatus.POLLING,
            deadline = "2020-01-01T00:00:00Z", // Past deadline
            organizerId = organizerId
        ).copy(participants = listOf("user-2"))
    }
    
    fun createEventForVoting(organizerId: String = "user-1"): Event {
        return createBaseEvent(
            id = "voting-event-${Clock.System.now().toEpochMilliseconds()}",
            status = EventStatus.POLLING,
            deadline = "2025-12-31T23:59:59Z", // Future deadline
            organizerId = organizerId
        ).copy(participants = listOf("user-2", "user-3"))
    }
}