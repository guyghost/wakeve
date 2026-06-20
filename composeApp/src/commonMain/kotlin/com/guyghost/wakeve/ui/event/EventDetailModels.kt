package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.access.DateValidationState
import com.guyghost.wakeve.access.ParticipantAccessState
import com.guyghost.wakeve.access.ParticipantRsvp
import com.guyghost.wakeve.models.Album
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.payment.SettlementRecord
import com.guyghost.wakeve.postevent.PostEventControlSummary
import com.guyghost.wakeve.postevent.toPostEventControlSummary
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
    val postEventSummary: PostEventControlSummary?,
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
    val nextActionLabel: String,
    val checklist: List<EventDayOfChecklistItem>
)

data class EventDayOfChecklistItem(
    val title: String,
    val body: String,
    val statusLabel: String,
    val isComplete: Boolean,
    val isBlocking: Boolean
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
    currentUserId: String,
    settlements: List<SettlementRecord> = emptyList(),
    albums: List<Album> = emptyList()
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
        postEventSummary = event
            ?.takeIf { it.status == EventStatus.FINALIZED }
            ?.toPostEventControlSummary(settlements = settlements, albums = albums),
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
            nextActionLabel = eventDayOfNextActionLabel(status),
            checklist = eventDayOfChecklist(
                status = status,
                acceptedParticipantIds = emptyList(),
                pendingParticipantIds = participants,
                declinedParticipantIds = emptyList(),
                hasSyncedRsvp = false
            )
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
        nextActionLabel = eventDayOfNextActionLabel(status),
        checklist = eventDayOfChecklist(
            status = status,
            acceptedParticipantIds = acceptedParticipants.map { it.userId },
            pendingParticipantIds = pendingParticipants.map { it.userId },
            declinedParticipantIds = declinedParticipants.map { it.userId },
            hasSyncedRsvp = true
        )
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

internal fun eventDayOfChecklist(
    status: EventStatus,
    acceptedParticipantIds: List<String>,
    pendingParticipantIds: List<String>,
    declinedParticipantIds: List<String>,
    hasSyncedRsvp: Boolean
): List<EventDayOfChecklistItem> {
    val acceptedCount = acceptedParticipantIds.distinct().size
    val pendingCount = pendingParticipantIds.distinct().size
    val declinedCount = declinedParticipantIds.distinct().size

    return listOf(
        EventDayOfChecklistItem(
            title = "Point de rendez-vous",
            body = when (status) {
                EventStatus.CONFIRMED -> "Confirmez l'adresse et envoyez le rappel de depart."
                EventStatus.ORGANIZING -> "Gardez le lieu de rendez-vous visible pour le groupe."
                EventStatus.FINALIZED -> "Le rendez-vous est termine; basculez sur le recap."
                EventStatus.DRAFT,
                EventStatus.POLLING,
                EventStatus.COMPARING -> "Attendez la date confirmee avant de fixer le rendez-vous final."
            },
            statusLabel = if (status in listOf(EventStatus.CONFIRMED, EventStatus.ORGANIZING, EventStatus.FINALIZED)) {
                "A verifier"
            } else {
                "Non pret"
            },
            isComplete = status == EventStatus.FINALIZED,
            isBlocking = status !in listOf(EventStatus.CONFIRMED, EventStatus.ORGANIZING, EventStatus.FINALIZED)
        ),
        EventDayOfChecklistItem(
            title = "Presents a pointer",
            body = if (!hasSyncedRsvp) {
                eventDayOfArrivalTrackingUnavailableLabel()
            } else {
                eventDayOfArrivalTrackingLabel(acceptedParticipantIds)
            },
            statusLabel = when {
                !hasSyncedRsvp -> "A synchroniser"
                acceptedCount == 0 -> "Bloque"
                else -> "Pret"
            },
            isComplete = hasSyncedRsvp && acceptedCount > 0,
            isBlocking = hasSyncedRsvp && acceptedCount == 0
        ),
        EventDayOfChecklistItem(
            title = "Reponses a relancer",
            body = when {
                !hasSyncedRsvp -> "Synchronisez les RSVP avant de relancer."
                pendingCount == 0 -> "Aucune reponse manquante."
                else -> "A verifier : ${eventDayOfCompactParticipantList(pendingParticipantIds)}."
            },
            statusLabel = when {
                !hasSyncedRsvp -> "A synchroniser"
                pendingCount == 0 -> "OK"
                else -> "A relancer"
            },
            isComplete = hasSyncedRsvp && pendingCount == 0,
            isBlocking = pendingCount > 0
        ),
        EventDayOfChecklistItem(
            title = "Absents a traiter",
            body = when {
                !hasSyncedRsvp -> "La liste des absents depend des RSVP synchronises."
                declinedCount == 0 -> "Aucun absent signale."
                else -> "Ne viennent pas : ${eventDayOfCompactParticipantList(declinedParticipantIds)}."
            },
            statusLabel = when {
                !hasSyncedRsvp -> "A synchroniser"
                declinedCount == 0 -> "OK"
                else -> "A traiter"
            },
            isComplete = hasSyncedRsvp && declinedCount == 0,
            isBlocking = false
        )
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
