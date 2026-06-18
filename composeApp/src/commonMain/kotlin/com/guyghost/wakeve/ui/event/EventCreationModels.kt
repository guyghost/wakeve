package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.TimeSlot

data class DraftEventCreationRouteUiState(
    val hasPersistedDraft: Boolean = false,
    val lastSavedEventId: String? = null
)

data class DraftEventWizardUiState(
    val currentStep: Int = 0,
    val title: String = "",
    val description: String = "",
    val eventType: EventType = EventType.OTHER,
    val eventTypeCustom: String = "",
    val participantCount: String = "",
    val locations: List<PotentialLocation> = emptyList(),
    val timeSlots: List<TimeSlot> = emptyList(),
    val editingTimeSlot: TimeSlot? = null,
    val showLocationDialog: Boolean = false,
    val showTimeSlotInput: Boolean = false
)

fun Event?.toDraftEventWizardUiState(): DraftEventWizardUiState =
    DraftEventWizardUiState(
        title = this?.title.orEmpty(),
        description = this?.description.orEmpty(),
        eventType = this?.eventType ?: EventType.OTHER,
        eventTypeCustom = this?.eventTypeCustom.orEmpty(),
        participantCount = this?.expectedParticipants?.toString().orEmpty(),
        timeSlots = this?.proposedSlots.orEmpty()
    )
