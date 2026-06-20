package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.access.DateValidationState
import com.guyghost.wakeve.access.ParticipantAccessState
import com.guyghost.wakeve.access.ParticipantRsvp
import com.guyghost.wakeve.models.Album
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.payment.SettlementRecord
import com.guyghost.wakeve.postevent.PostEventControlSummary
import com.guyghost.wakeve.postevent.toPostEventControlSummary
import com.guyghost.wakeve.presentation.state.EventManagementContract
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
    val scheduleSummary: EventScheduleSummary?,
    val attendanceSummary: EventAttendanceSummary?,
    val budgetSummary: EventBudgetPlanningSummary?,
    val dayOfSummary: EventDayOfSummary?,
    val postEventSummary: PostEventControlSummary?,
    val rsvp: EventRsvpUiState?
)

data class EventDayOfSummary(
    val title: String,
    val status: EventDayOfStatus,
    val controlLabel: String,
    val meetingPointLabel: String,
    val meetingTimeLabel: String,
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

data class EventBudgetPlanningSummary(
    val title: String,
    val statusLabel: String,
    val participantBasisLabel: String,
    val scopeLabel: String,
    val nextActionLabel: String,
    val canOpenBudget: Boolean
)

data class EventAttendanceSummary(
    val title: String,
    val statusLabel: String,
    val confirmedLabel: String,
    val pendingLabel: String,
    val declinedLabel: String,
    val nextActionLabel: String,
    val hasSyncedRsvp: Boolean
)

data class EventScheduleSummary(
    val title: String,
    val statusLabel: String,
    val primaryLabel: String,
    val deadlineLabel: String,
    val nextActionLabel: String,
    val hasConfirmedDate: Boolean
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
    val eventPotentialLocations = event
        ?.let { selected -> potentialLocations.filter { it.eventId == selected.id } }
        .orEmpty()

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
        scheduleSummary = event?.toScheduleSummary(),
        attendanceSummary = event?.toAttendanceSummary(participantAccessStates),
        budgetSummary = event?.toBudgetPlanningSummary(
            accessStates = participantAccessStates,
            canOpenBudget = canAccessOrganizationDetails &&
                status in listOf(EventStatus.CONFIRMED, EventStatus.ORGANIZING, EventStatus.FINALIZED)
        ),
        dayOfSummary = event?.toDayOfSummary(
            accessStates = participantAccessStates,
            potentialLocations = eventPotentialLocations
        ),
        postEventSummary = event
            ?.takeIf { it.status == EventStatus.FINALIZED }
            ?.toPostEventControlSummary(settlements = settlements, albums = albums),
        rsvp = event?.let {
            participantAccessStates.firstOrNull { accessState -> accessState.userId == currentUserId }
                ?.toRsvpUiState(isOrganizer = isOrganizer)
        }
    )
}

internal fun Event.toScheduleSummary(): EventScheduleSummary =
    EventScheduleSummary(
        title = "Date et depart",
        statusLabel = eventScheduleStatusLabel(status, finalDate),
        primaryLabel = eventSchedulePrimaryLabel(finalDate, proposedSlots.mapNotNull { it.start }),
        deadlineLabel = eventScheduleDeadlineLabel(status, deadline),
        nextActionLabel = eventScheduleNextActionLabel(status, finalDate, proposedSlots.size),
        hasConfirmedDate = !finalDate.isNullOrBlank()
    )

internal fun eventScheduleStatusLabel(status: EventStatus, finalDate: String?): String =
    if (!finalDate.isNullOrBlank()) {
        "Date retenue"
    } else {
        when (status) {
            EventStatus.DRAFT -> "A planifier"
            EventStatus.POLLING -> "Vote en cours"
            EventStatus.COMPARING -> "Options a comparer"
            EventStatus.CONFIRMED,
            EventStatus.ORGANIZING,
            EventStatus.FINALIZED -> "Date a verifier"
        }
    }

internal fun eventSchedulePrimaryLabel(finalDate: String?, proposedSlotStarts: List<String>): String {
    if (!finalDate.isNullOrBlank()) {
        return "Depart confirme : ${eventDayOfFormatFinalDate(finalDate)}"
    }

    val slotCount = proposedSlotStarts.size
    if (slotCount == 0) return "Aucun creneau propose"

    val firstSlot = proposedSlotStarts
        .mapNotNull { raw -> runCatching { Instant.parse(raw) }.getOrNull()?.let { it to raw } }
        .minByOrNull { it.first.toEpochMilliseconds() }
        ?.second
    val countLabel = "$slotCount creneau${if (slotCount == 1) "" else "x"} propose${if (slotCount == 1) "" else "s"}"

    return firstSlot
        ?.let { "$countLabel - premier depart possible : ${eventDayOfFormatFinalDate(it)}" }
        ?: countLabel
}

internal fun eventScheduleDeadlineLabel(status: EventStatus, deadline: String): String =
    when (status) {
        EventStatus.DRAFT -> "Date limite de vote : ${eventDayOfFormatFinalDate(deadline)}"
        EventStatus.POLLING,
        EventStatus.COMPARING -> "Votes ouverts jusqu'au ${eventDayOfFormatFinalDate(deadline)}"
        EventStatus.CONFIRMED,
        EventStatus.ORGANIZING -> "Decision prise; gardez l'horaire visible pour le groupe."
        EventStatus.FINALIZED -> "Decision verrouillee pour le recap."
    }

internal fun eventScheduleNextActionLabel(
    status: EventStatus,
    finalDate: String?,
    proposedSlotCount: Int
): String = when {
    !finalDate.isNullOrBlank() -> "Partagez la date retenue et preparez les rappels de depart."
    proposedSlotCount == 0 -> "Ajoutez au moins un creneau pour obtenir une decision."
    status == EventStatus.POLLING -> "Relancez les votes avant de confirmer la date."
    status == EventStatus.COMPARING -> "Choisissez l'option qui minimise les absents et les conflits."
    status == EventStatus.DRAFT -> "Verifiez les creneaux avant de lancer le sondage."
    else -> "Confirmez une date avant d'ouvrir l'organisation detaillee."
}

internal fun Event.toAttendanceSummary(
    accessStates: List<ParticipantAccessState>
): EventAttendanceSummary {
    val relevantAccessStates = accessStates
        .filter { accessState -> accessState.userId == organizerId || accessState.userId in participants }
        .distinctBy { it.userId }

    if (relevantAccessStates.isEmpty()) {
        val invitedCount = participants.map { it.trim() }.filter { it.isNotEmpty() }.distinct().size
        return EventAttendanceSummary(
            title = "Presence",
            statusLabel = "RSVP non synchronises",
            confirmedLabel = "Confirmes : non disponible",
            pendingLabel = "Invites : $invitedCount",
            declinedLabel = "Refus : non disponible",
            nextActionLabel = eventAttendanceUnsyncedActionLabel(status),
            hasSyncedRsvp = false
        )
    }

    val acceptedCount = relevantAccessStates.count { it.rsvp == ParticipantRsvp.ACCEPTED }
    val pendingCount = relevantAccessStates.count { it.rsvp == ParticipantRsvp.PENDING }
    val declinedCount = relevantAccessStates.count { it.rsvp == ParticipantRsvp.DECLINED }

    return EventAttendanceSummary(
        title = "Presence",
        statusLabel = eventAttendanceStatusLabel(
            status = status,
            acceptedCount = acceptedCount,
            pendingCount = pendingCount,
            declinedCount = declinedCount
        ),
        confirmedLabel = "Confirmes : $acceptedCount",
        pendingLabel = "En attente : $pendingCount",
        declinedLabel = "Ne viennent pas : $declinedCount",
        nextActionLabel = eventAttendanceNextActionLabel(
            status = status,
            acceptedCount = acceptedCount,
            pendingCount = pendingCount,
            declinedCount = declinedCount
        ),
        hasSyncedRsvp = true
    )
}

internal fun eventAttendanceStatusLabel(
    status: EventStatus,
    acceptedCount: Int,
    pendingCount: Int,
    declinedCount: Int
): String = when {
    status == EventStatus.FINALIZED -> "Groupe final"
    acceptedCount == 0 -> "Aucun confirme"
    pendingCount > 0 -> "Reponses a relancer"
    declinedCount > 0 -> "Presences a verifier"
    else -> "Groupe confirme"
}

internal fun eventAttendanceNextActionLabel(
    status: EventStatus,
    acceptedCount: Int,
    pendingCount: Int,
    declinedCount: Int
): String = when {
    acceptedCount == 0 && status != EventStatus.DRAFT ->
        "Obtenez au moins une confirmation avant de poursuivre l'organisation."
    pendingCount > 0 ->
        "Relancez les invites en attente avant de figer le budget et le programme."
    declinedCount > 0 ->
        "Retirez les absents des decisions de budget, transport et programme."
    status == EventStatus.DRAFT ->
        "Ajoutez les invites pour savoir qui vient avant de lancer le sondage."
    status == EventStatus.FINALIZED ->
        "Utilisez cette base pour le recap, les photos et les remboursements."
    else ->
        "Le groupe attendu est clair; continuez l'organisation."
}

internal fun eventAttendanceUnsyncedActionLabel(status: EventStatus): String = when (status) {
    EventStatus.DRAFT -> "Ajoutez les invites pour savoir qui vient avant de lancer le sondage."
    EventStatus.POLLING,
    EventStatus.COMPARING -> "Synchronisez les RSVP pour savoir qui vient vraiment."
    EventStatus.CONFIRMED,
    EventStatus.ORGANIZING -> "Synchronisez les RSVP avant de figer budget, transport et programme."
    EventStatus.FINALIZED -> "Synchronisez les RSVP pour fiabiliser le recap."
}

internal fun Event.toBudgetPlanningSummary(
    accessStates: List<ParticipantAccessState>,
    canOpenBudget: Boolean
): EventBudgetPlanningSummary? {
    if (status !in listOf(EventStatus.CONFIRMED, EventStatus.ORGANIZING, EventStatus.FINALIZED)) {
        return null
    }

    val acceptedCount = accessStates
        .filter { accessState -> accessState.userId == organizerId || accessState.userId in participants }
        .distinctBy { it.userId }
        .count { it.rsvp == ParticipantRsvp.ACCEPTED }

    return EventBudgetPlanningSummary(
        title = "Budget",
        statusLabel = eventBudgetStatusLabel(status),
        participantBasisLabel = eventBudgetParticipantBasisLabel(
            acceptedCount = acceptedCount,
            expectedParticipants = expectedParticipants,
            invitedCount = participants.size
        ),
        scopeLabel = eventBudgetScopeLabel(eventType),
        nextActionLabel = eventBudgetNextActionLabel(status),
        canOpenBudget = canOpenBudget
    )
}

internal fun eventBudgetStatusLabel(status: EventStatus): String = when (status) {
    EventStatus.CONFIRMED -> "A cadrer"
    EventStatus.ORGANIZING -> "A suivre"
    EventStatus.FINALIZED -> "A solder"
    EventStatus.DRAFT,
    EventStatus.POLLING,
    EventStatus.COMPARING -> "Non pret"
}

internal fun eventBudgetParticipantBasisLabel(
    acceptedCount: Int,
    expectedParticipants: Int?,
    invitedCount: Int
): String = when {
    acceptedCount > 0 -> "Base budget : $acceptedCount participant${if (acceptedCount == 1) "" else "s"} confirme${if (acceptedCount == 1) "" else "s"}"
    expectedParticipants != null -> "Base budget : $expectedParticipants participant${if (expectedParticipants == 1) "" else "s"} prevu${if (expectedParticipants == 1) "" else "s"}"
    invitedCount > 0 -> "Base budget : $invitedCount invite${if (invitedCount == 1) "" else "s"} a confirmer"
    else -> "Base budget : participants a definir"
}

internal fun eventBudgetScopeLabel(eventType: EventType): String = when (eventType) {
    EventType.WEDDING,
    EventType.TEAM_BUILDING,
    EventType.CONFERENCE,
    EventType.OUTDOOR_ACTIVITY ->
        "A chiffrer : transport, logement, repas, activites et extras."
    EventType.FOOD_TASTING,
    EventType.BIRTHDAY,
    EventType.PARTY,
    EventType.CULTURAL_EVENT,
    EventType.FAMILY_GATHERING ->
        "A chiffrer : lieu, repas, activites et extras."
    EventType.WORKSHOP,
    EventType.TECH_MEETUP,
    EventType.CREATIVE_WORKSHOP ->
        "A chiffrer : lieu, materiel, repas et intervenants."
    EventType.SPORTS_EVENT,
    EventType.SPORT_EVENT,
    EventType.WELLNESS_EVENT ->
        "A chiffrer : inscription, equipement, transport et repas."
    EventType.OTHER,
    EventType.CUSTOM ->
        "A chiffrer : transport, repas, activites et extras."
}

internal fun eventBudgetNextActionLabel(status: EventStatus): String = when (status) {
    EventStatus.CONFIRMED -> "Creez une estimation par personne avant de lancer les depenses."
    EventStatus.ORGANIZING -> "Ajoutez les depenses au fil de l'eau pour garder le groupe aligne."
    EventStatus.FINALIZED -> "Verifiez les depenses finales avant les remboursements."
    EventStatus.DRAFT,
    EventStatus.POLLING,
    EventStatus.COMPARING -> "Attendez une date confirmee avant d'ouvrir le budget."
}

internal fun Event.toDayOfSummary(
    accessStates: List<ParticipantAccessState>,
    potentialLocations: List<PotentialLocation> = emptyList()
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
            meetingPointLabel = eventDayOfMeetingPointLabel(potentialLocations),
            meetingTimeLabel = eventDayOfMeetingTimeLabel(finalDate),
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
                hasSyncedRsvp = false,
                meetingPointLabel = eventDayOfMeetingPointLabel(potentialLocations),
                meetingTimeLabel = eventDayOfMeetingTimeLabel(finalDate)
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
        meetingPointLabel = eventDayOfMeetingPointLabel(potentialLocations),
        meetingTimeLabel = eventDayOfMeetingTimeLabel(finalDate),
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
            hasSyncedRsvp = true,
            meetingPointLabel = eventDayOfMeetingPointLabel(potentialLocations),
            meetingTimeLabel = eventDayOfMeetingTimeLabel(finalDate)
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

internal fun eventDayOfMeetingPointLabel(potentialLocations: List<PotentialLocation>): String {
    val location = potentialLocations
        .firstOrNull { it.name.isNotBlank() }
        ?: return "Lieu de rendez-vous a confirmer"
    val name = location.name.trim()
    val address = location.address
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it != name }

    return if (address == null) name else "$name ($address)"
}

internal fun eventDayOfMeetingTimeLabel(finalDate: String?): String =
    finalDate
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let(::eventDayOfFormatFinalDate)
        ?: "Horaire a confirmer"

internal fun eventDayOfMeetingPointBody(
    status: EventStatus,
    meetingPointLabel: String,
    meetingTimeLabel: String
): String = when (status) {
    EventStatus.CONFIRMED ->
        "Rendez-vous : $meetingPointLabel. Depart : $meetingTimeLabel. Envoyez le rappel de depart."
    EventStatus.ORGANIZING ->
        "Rendez-vous : $meetingPointLabel. Depart : $meetingTimeLabel. Gardez ce point visible pour le groupe."
    EventStatus.FINALIZED ->
        "Rendez-vous traite : $meetingPointLabel. Depart : $meetingTimeLabel. Basculez sur le recap."
    EventStatus.DRAFT,
    EventStatus.POLLING,
    EventStatus.COMPARING ->
        "Attendez la date confirmee avant de fixer le rendez-vous final."
}

internal fun eventDayOfFormatFinalDate(value: String): String =
    runCatching {
        val localDateTime = Instant.parse(value).toLocalDateTime(TimeZone.currentSystemDefault())
        val month = eventDayOfFrenchMonths.getOrElse(localDateTime.monthNumber - 1) { "" }
        val hour = localDateTime.hour.toString().padStart(2, '0')
        val minute = localDateTime.minute.toString().padStart(2, '0')
        "${localDateTime.dayOfMonth} $month ${localDateTime.year} a $hour:$minute"
    }.getOrElse { value }

private val eventDayOfFrenchMonths = listOf(
    "janvier",
    "fevrier",
    "mars",
    "avril",
    "mai",
    "juin",
    "juillet",
    "aout",
    "septembre",
    "octobre",
    "novembre",
    "decembre"
)

internal fun eventDayOfChecklist(
    status: EventStatus,
    acceptedParticipantIds: List<String>,
    pendingParticipantIds: List<String>,
    declinedParticipantIds: List<String>,
    hasSyncedRsvp: Boolean,
    meetingPointLabel: String = "Lieu de rendez-vous a confirmer",
    meetingTimeLabel: String = "Horaire a confirmer"
): List<EventDayOfChecklistItem> {
    val acceptedCount = acceptedParticipantIds.distinct().size
    val pendingCount = pendingParticipantIds.distinct().size
    val declinedCount = declinedParticipantIds.distinct().size

    return listOf(
        EventDayOfChecklistItem(
            title = "Point de rendez-vous",
            body = eventDayOfMeetingPointBody(status, meetingPointLabel, meetingTimeLabel),
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
