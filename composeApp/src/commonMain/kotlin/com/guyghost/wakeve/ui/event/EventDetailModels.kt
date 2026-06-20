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
    val dayOfSummary: EventDayOfSummary?,
    val rsvp: EventRsvpUiState?
)

data class EventDayOfSummary(
    val title: String,
    val status: EventDayOfStatus,
    val controlLabel: String,
    val attendanceLabel: String,
    val missingLabel: String,
    val arrivalTrackingLabel: String,
    val missingPeopleLabel: String,
    val nextActionLabel: String
)

enum class EventDayOfStatus {
    Ready,
    NeedsAttention,
    Blocked
}

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
        dayOfSummary = event?.toDayOfSummary(participantAccessStates),
        rsvp = event?.let {
            participantAccessStates.firstOrNull { accessState -> accessState.userId == currentUserId }
                ?.toRsvpUiState(isOrganizer = isOrganizer)
        }
    )
}

internal fun Event.toDayOfSummary(
    accessStates: List<ParticipantAccessState>
): EventDayOfSummary? {
    if (status !in listOf(EventStatus.CONFIRMED, EventStatus.ORGANIZING, EventStatus.FINALIZED)) {
        return null
    }

    val relevantAccessStates = accessStates
        .filter { accessState -> accessState.userId == organizerId || accessState.userId in participants }
        .distinctBy { it.userId }
    if (relevantAccessStates.isEmpty()) {
        return EventDayOfSummary(
            title = eventDayOfTitle(),
            status = EventDayOfStatus.NeedsAttention,
            controlLabel = eventDayOfControlLabel(
                status = status,
                acceptedCount = 0,
                pendingCount = participants.size,
                declinedCount = 0,
                hasSyncedRsvp = false
            ),
            attendanceLabel = "Participants invites : ${participants.size}",
            missingLabel = "RSVP detailles non synchronises",
            arrivalTrackingLabel = eventDayOfArrivalTrackingUnavailableLabel(),
            missingPeopleLabel = eventDayOfMissingPeopleUnavailableLabel(),
            nextActionLabel = eventDayOfNextActionLabel(status)
        )
    }

    val acceptedParticipants = relevantAccessStates.filter { it.rsvp == ParticipantRsvp.ACCEPTED }
    val pendingParticipants = relevantAccessStates.filter { it.rsvp == ParticipantRsvp.PENDING }
    val declinedParticipants = relevantAccessStates.filter { it.rsvp == ParticipantRsvp.DECLINED }
    val acceptedCount = acceptedParticipants.size
    val pendingCount = pendingParticipants.size
    val declinedCount = declinedParticipants.size

    return EventDayOfSummary(
        title = eventDayOfTitle(),
        status = eventDayOfStatus(
            eventStatus = status,
            acceptedCount = acceptedCount,
            pendingCount = pendingCount,
            declinedCount = declinedCount
        ),
        controlLabel = eventDayOfControlLabel(
            status = status,
            acceptedCount = acceptedCount,
            pendingCount = pendingCount,
            declinedCount = declinedCount,
            hasSyncedRsvp = true
        ),
        attendanceLabel = "$acceptedCount participant${if (acceptedCount == 1) "" else "s"} attendus",
        missingLabel = eventDayOfMissingLabel(pendingCount, declinedCount),
        arrivalTrackingLabel = eventDayOfArrivalTrackingLabel(acceptedParticipants.map { it.userId }),
        missingPeopleLabel = eventDayOfMissingPeopleLabel(
            pendingParticipantIds = pendingParticipants.map { it.userId },
            declinedParticipantIds = declinedParticipants.map { it.userId }
        ),
        nextActionLabel = eventDayOfNextActionLabel(status)
    )
}

internal fun eventDayOfTitle(): String = "Jour J"

internal fun eventDayOfStatus(
    eventStatus: EventStatus,
    acceptedCount: Int,
    pendingCount: Int,
    declinedCount: Int
): EventDayOfStatus =
    when {
        eventStatus == EventStatus.FINALIZED -> EventDayOfStatus.Ready
        acceptedCount == 0 -> EventDayOfStatus.Blocked
        pendingCount > 0 || declinedCount > 0 -> EventDayOfStatus.NeedsAttention
        else -> EventDayOfStatus.Ready
    }

internal fun eventDayOfControlLabel(
    status: EventStatus,
    acceptedCount: Int,
    pendingCount: Int,
    declinedCount: Int,
    hasSyncedRsvp: Boolean
): String {
    if (status == EventStatus.FINALIZED) {
        return "Evenement termine : recap, photos et remboursements a traiter."
    }
    if (!hasSyncedRsvp) {
        return "Controle incomplet : synchronisez les RSVP avant de pointer les presents."
    }
    if (acceptedCount == 0) {
        return "Controle bloque : aucun participant confirme pour l'instant."
    }

    val unresolvedCount = pendingCount + declinedCount
    return if (unresolvedCount == 0) {
        "Controle pret : tous les participants attendus sont confirmes."
    } else {
        "Controle incomplet : $unresolvedCount RSVP a verifier avant depart."
    }
}

internal fun eventDayOfMissingLabel(pendingCount: Int, declinedCount: Int): String {
    val parts = buildList {
        if (pendingCount > 0) add("$pendingCount sans reponse")
        if (declinedCount > 0) add("$declinedCount ne ${if (declinedCount > 1) "viennent" else "vient"} pas")
    }
    return if (parts.isEmpty()) "Aucune reponse manquante" else parts.joinToString(" · ")
}

internal fun eventDayOfArrivalTrackingUnavailableLabel(): String =
    "Presence a pointer des que les participants se presentent."

internal fun eventDayOfMissingPeopleUnavailableLabel(): String =
    "Liste des presents non disponible tant que les RSVP ne sont pas synchronises."

internal fun eventDayOfArrivalTrackingLabel(acceptedParticipantIds: List<String>): String {
    if (acceptedParticipantIds.isEmpty()) {
        return "Aucun participant attendu a pointer pour l'instant."
    }

    return "Presence a pointer : ${eventDayOfCompactParticipantList(acceptedParticipantIds)}."
}

internal fun eventDayOfMissingPeopleLabel(
    pendingParticipantIds: List<String>,
    declinedParticipantIds: List<String>
): String {
    val parts = buildList {
        if (pendingParticipantIds.isNotEmpty()) {
            add("A verifier : ${eventDayOfCompactParticipantList(pendingParticipantIds)}")
        }
        if (declinedParticipantIds.isNotEmpty()) {
            add("Ne viennent pas : ${eventDayOfCompactParticipantList(declinedParticipantIds)}")
        }
    }

    return if (parts.isEmpty()) {
        "Personne ne manque d'apres les confirmations actuelles."
    } else {
        parts.joinToString(" · ")
    }
}

internal fun eventDayOfCompactParticipantList(participantIds: List<String>): String {
    val normalizedIds = participantIds
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
    if (normalizedIds.isEmpty()) return "aucun participant"

    val shown = normalizedIds.take(3).joinToString(", ")
    val remainingCount = normalizedIds.size - 3
    return if (remainingCount > 0) {
        "$shown + $remainingCount autre${if (remainingCount > 1) "s" else ""}"
    } else {
        shown
    }
}

internal fun eventDayOfNextActionLabel(status: EventStatus): String = when (status) {
    EventStatus.CONFIRMED -> "Verifier le lieu de rendez-vous et envoyer le rappel de depart."
    EventStatus.ORGANIZING -> "Suivre les arrivees et traiter les absents avant la prochaine etape."
    EventStatus.FINALIZED -> "Evenement termine : consulter recap, photos et remboursements."
    EventStatus.DRAFT,
    EventStatus.POLLING,
    EventStatus.COMPARING -> "Continuer la preparation avant le jour J."
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
