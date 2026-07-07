package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeSlot

internal class DraftEventWizardEventFactory(
    private val initialEvent: Event?,
    private val generatedId: String,
    private val nowIso: () -> String
) {
    fun buildEvent(
        userId: String,
        title: String,
        description: String,
        eventType: EventType,
        eventTypeCustom: String,
        participantCount: String,
        timeSlots: List<TimeSlot>,
        status: EventStatus
    ): Event {
        val now = nowIso()
        val participantCountInt = participantCount.toIntOrNull()

        return Event(
            id = initialEvent?.id ?: generatedId,
            title = title,
            description = description,
            organizerId = initialEvent?.organizerId ?: userId,
            participants = initialEvent?.participants ?: emptyList(),
            proposedSlots = timeSlots,
            deadline = initialEvent?.deadline ?: now,
            status = status,
            createdAt = initialEvent?.createdAt ?: now,
            updatedAt = now,
            finalDate = initialEvent?.finalDate,
            eventType = eventType,
            eventTypeCustom = if (eventType == EventType.CUSTOM) eventTypeCustom else null,
            minParticipants = initialEvent?.minParticipants,
            maxParticipants = initialEvent?.maxParticipants,
            expectedParticipants = participantCountInt,
            heroImageUrl = initialEvent?.heroImageUrl
        )
    }

    fun buildEvent(
        userId: String,
        state: DraftEventWizardUiState,
        status: EventStatus
    ): Event =
        buildEvent(
            userId = userId,
            title = state.title,
            description = state.description,
            eventType = state.eventType,
            eventTypeCustom = state.eventTypeCustom,
            participantCount = state.participantCount,
            timeSlots = state.timeSlots,
            status = status
        )
}
