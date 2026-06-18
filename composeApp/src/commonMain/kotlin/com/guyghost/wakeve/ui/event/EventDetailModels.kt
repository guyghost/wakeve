package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.access.DateValidationState
import com.guyghost.wakeve.access.ParticipantAccessState
import com.guyghost.wakeve.access.ParticipantRsvp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract

data class EventDetailUiState(
    val eventId: String,
    val event: Event?,
    val isLoading: Boolean,
    val participants: List<String>,
    val pollVotes: Map<String, Vote>,
    val isOrganizer: Boolean,
    val canDelete: Boolean,
    val canAccessOrganizationDetails: Boolean,
    val showTransportPlanning: Boolean,
    val showOrganizationTools: Boolean,
    val rsvp: EventRsvpUiState?
)

data class EventRsvpUiState(
    val participantId: String,
    val selectedResponse: ParticipantRsvp,
    val isOrganizer: Boolean,
    val isEnabled: Boolean,
    val statusLabel: String
)

fun EventManagementContract.State.toEventDetailUiState(
    eventId: String,
    currentUserId: String
): EventDetailUiState {
    val event = selectedEvent?.takeIf { it.id == eventId }
        ?: events.firstOrNull { it.id == eventId }
    val isOrganizer = event?.organizerId == currentUserId
    val isParticipantConfirmed = participantAccessStates
        .firstOrNull { it.userId == currentUserId }
        ?.dateValidation == DateValidationState.VALIDATED_RETAINED_DATE
    val canAccessOrganizationDetails = isOrganizer || isParticipantConfirmed
    val status = event?.status

    return EventDetailUiState(
        eventId = eventId,
        event = event,
        isLoading = isLoading,
        participants = if (selectedEvent?.id == eventId) participantIds else event?.participants.orEmpty(),
        pollVotes = pollVotes[eventId].orEmpty(),
        isOrganizer = isOrganizer,
        canDelete = isOrganizer && status != EventStatus.FINALIZED,
        canAccessOrganizationDetails = canAccessOrganizationDetails,
        showTransportPlanning = canAccessOrganizationDetails &&
            status in listOf(EventStatus.CONFIRMED, EventStatus.ORGANIZING, EventStatus.FINALIZED),
        showOrganizationTools = canAccessOrganizationDetails &&
            status in listOf(EventStatus.ORGANIZING, EventStatus.FINALIZED),
        rsvp = event?.let {
            participantAccessStates.firstOrNull { accessState -> accessState.userId == currentUserId }
                ?.toRsvpUiState(isOrganizer = isOrganizer)
        }
    )
}

private fun ParticipantAccessState.toRsvpUiState(isOrganizer: Boolean): EventRsvpUiState =
    EventRsvpUiState(
        participantId = userId,
        selectedResponse = rsvp,
        isOrganizer = isOrganizer,
        isEnabled = !isOrganizer,
        statusLabel = when (rsvp) {
            ParticipantRsvp.PENDING -> "Réponse en attente"
            ParticipantRsvp.ACCEPTED -> "Participation confirmée"
            ParticipantRsvp.DECLINED -> "Participation refusée"
        }
    )
