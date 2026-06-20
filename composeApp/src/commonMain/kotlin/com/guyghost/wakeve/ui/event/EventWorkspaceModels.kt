package com.guyghost.wakeve.ui.event

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.presentation.state.EventManagementContract
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

enum class EventListFilter(
    val label: String,
    val icon: ImageVector
) {
    Upcoming("À venir", Icons.Outlined.CalendarToday),
    Past("Passés", Icons.Outlined.History),
    Drafts("Brouillons", Icons.Outlined.Edit),
    OrganizedByMe("Organisés", Icons.Outlined.Star),
    Confirmed("Confirmés", Icons.Outlined.CheckCircle)
}

data class EventListItemUiState(
    val id: String,
    val title: String,
    val description: String,
    val statusLabel: String,
    val nextActionLabel: String,
    val deadlineLabel: String,
    val participantsLabel: String,
    val isOrganizer: Boolean
)

data class EventWorkspaceActionSummary(
    val eventId: String,
    val title: String,
    val body: String,
    val actionLabel: String,
    val action: EventWorkspaceSummaryAction,
    val template: EventWorkspaceCreationTemplate? = null
)

data class EventViralLoopSummary(
    val eventId: String?,
    val title: String,
    val headline: String,
    val inviteReasonLabel: String,
    val installReasonLabel: String,
    val returnReasonLabel: String,
    val actionLabel: String,
    val action: EventWorkspaceSummaryAction?,
    val template: EventWorkspaceCreationTemplate? = null
)

enum class EventWorkspaceSummaryAction {
    OpenEvent,
    OpenPoll,
    RecreateFromTemplate
}

data class EventWorkspaceCreationTemplate(
    val title: String,
    val description: String,
    val eventType: EventType
)

data class EventReorganizationSummary(
    val title: String,
    val body: String,
    val actionLabel: String,
    val template: EventWorkspaceCreationTemplate
)

enum class EventWidgetKind {
    EventToday,
    Travel,
    Countdown,
    NextTask,
    Empty
}

data class EventWidgetSummary(
    val kind: EventWidgetKind,
    val eventId: String?,
    val title: String,
    val headline: String,
    val body: String,
    val userInterestLabel: String = "",
    val rationaleLabel: String = "",
    val actionLabel: String
)

data class EventWorkspaceUiState(
    val isLoading: Boolean,
    val error: String?,
    val selectedFilter: EventListFilter,
    val searchQuery: String,
    val actionSummary: EventWorkspaceActionSummary?,
    val viralLoopSummary: EventViralLoopSummary,
    val widgetSummary: EventWidgetSummary,
    val events: List<EventListItemUiState>,
    val selectedEvent: Event?,
    val participantCount: Int,
    val pollVoteCount: Int
)

fun EventManagementContract.State.toEventWorkspaceUiState(
    currentUserId: String,
    selectedFilter: EventListFilter,
    searchQuery: String,
    selectedEventId: String?,
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): EventWorkspaceUiState {
    val filtered = events
        .filter { event -> event.matchesFilter(selectedFilter, currentUserId) }
        .filter { event ->
            searchQuery.isBlank() ||
                event.title.contains(searchQuery, ignoreCase = true) ||
                event.description.contains(searchQuery, ignoreCase = true)
        }

    val selected = selectedEvent
        ?: selectedEventId?.let { id -> filtered.firstOrNull { it.id == id } }

    return EventWorkspaceUiState(
        isLoading = isLoading,
        error = error,
        selectedFilter = selectedFilter,
        searchQuery = searchQuery,
        actionSummary = filtered.toWorkspaceActionSummary(currentUserId, pollVotes),
        viralLoopSummary = events.toViralLoopSummary(currentUserId, pollVotes),
        widgetSummary = events.toEventWidgetSummary(
            now = now,
            timeZone = timeZone,
            currentUserId = currentUserId
        ),
        events = filtered.map { it.toListItem(currentUserId) },
        selectedEvent = selected,
        participantCount = if (selected?.id == selectedEvent?.id) participantIds.size else selected?.participants?.size ?: 0,
        pollVoteCount = selected?.let { pollVotes[it.id]?.size } ?: 0
    )
}

private fun Event.matchesFilter(filter: EventListFilter, currentUserId: String): Boolean =
    when (filter) {
        EventListFilter.Upcoming -> status != EventStatus.FINALIZED
        EventListFilter.Past -> status == EventStatus.FINALIZED
        EventListFilter.Drafts -> status == EventStatus.DRAFT
        EventListFilter.OrganizedByMe -> organizerId == currentUserId
        EventListFilter.Confirmed -> status == EventStatus.CONFIRMED
    }

private fun Event.toListItem(currentUserId: String): EventListItemUiState =
    EventListItemUiState(
        id = id,
        title = title,
        description = description,
        statusLabel = status.workspaceStatusLabel(),
        nextActionLabel = workspaceNextActionLabel(isOrganizer = organizerId == currentUserId),
        deadlineLabel = "Deadline $deadline",
        participantsLabel = "${participants.size} participant${if (participants.size == 1) "" else "s"}",
        isOrganizer = organizerId == currentUserId
    )

internal fun EventStatus.workspaceStatusLabel(): String = when (this) {
    EventStatus.DRAFT -> "Brouillon"
    EventStatus.POLLING -> "Sondage"
    EventStatus.COMPARING -> "Comparaison"
    EventStatus.CONFIRMED -> "Confirmé"
    EventStatus.ORGANIZING -> "Organisation"
    EventStatus.FINALIZED -> "Finalisé"
}

internal fun Event.workspaceNextActionLabel(isOrganizer: Boolean): String = when (status) {
    EventStatus.DRAFT -> if (isOrganizer) "À reprendre" else "En préparation"
    EventStatus.POLLING -> "Vote attendu"
    EventStatus.COMPARING -> if (isOrganizer) "Option à choisir" else "Options à comparer"
    EventStatus.CONFIRMED -> "À préparer"
    EventStatus.ORGANIZING -> "À piloter"
    EventStatus.FINALIZED -> "Récapitulatif"
}

internal fun List<Event>.toWorkspaceActionSummary(
    currentUserId: String,
    pollVotes: Map<String, Map<String, com.guyghost.wakeve.models.Vote>>
): EventWorkspaceActionSummary? {
    val activeEvent = filterNot { it.status == EventStatus.FINALIZED }
        .minWithOrNull(compareBy<Event> { it.workspaceActionPriority(currentUserId) }.thenBy { it.updatedAt })
    val nextEvent = activeEvent ?: filter { it.status == EventStatus.FINALIZED }
        .maxByOrNull { it.updatedAt }
        ?: return null

    return EventWorkspaceActionSummary(
        eventId = nextEvent.id,
        title = nextEvent.workspaceActionTitle(currentUserId),
        body = nextEvent.workspaceActionBody(currentUserId, pollVotes[nextEvent.id]?.size ?: 0),
        actionLabel = nextEvent.workspaceActionButtonLabel(currentUserId),
        action = nextEvent.workspaceSummaryAction(),
        template = nextEvent.workspaceCreationTemplate()
    )
}

internal fun List<Event>.toViralLoopSummary(
    currentUserId: String,
    pollVotes: Map<String, Map<String, com.guyghost.wakeve.models.Vote>>
): EventViralLoopSummary {
    val event = maxWithOrNull(
        compareBy<Event> { it.viralLoopPriority(currentUserId, pollVotes[it.id]?.size ?: 0) }
            .thenBy { it.updatedAt }
    ) ?: return EventViralLoopSummary(
        eventId = null,
        title = "Boucle de croissance",
        headline = "Aucun événement à partager",
        inviteReasonLabel = "Pourquoi inviter : il manque un événement concret à proposer.",
        installReasonLabel = "Pourquoi installer : Wakeve doit d'abord montrer un groupe actif.",
        returnReasonLabel = "Pourquoi revenir : créez un premier événement réutilisable.",
        actionLabel = "Créer",
        action = null
    )

    val voteCount = pollVotes[event.id]?.size ?: 0
    val missingVotes = (event.participants.size - voteCount).coerceAtLeast(0)

    return when (event.status) {
        EventStatus.DRAFT -> EventViralLoopSummary(
            eventId = event.id,
            title = "Boucle de croissance",
            headline = "Invitation pas encore prête",
            inviteReasonLabel = "Pourquoi inviter : le groupe ne doit recevoir le lien qu'une fois le sondage clair.",
            installReasonLabel = "Pourquoi installer : voir les créneaux et répondre sans fouiller WhatsApp.",
            returnReasonLabel = "Pourquoi revenir : reprendre le brouillon pour lancer le vote.",
            actionLabel = if (event.organizerId == currentUserId) "Finaliser" else "Voir",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.POLLING -> EventViralLoopSummary(
            eventId = event.id,
            title = "Boucle de croissance",
            headline = if (missingVotes > 0) {
                "$missingVotes vote${if (missingVotes == 1) "" else "s"} à obtenir"
            } else {
                "Votes prêts à convertir"
            },
            inviteReasonLabel = "Pourquoi inviter : chaque invité débloque la décision collective.",
            installReasonLabel = "Pourquoi installer : voter, suivre la date limite et éviter les relances privées.",
            returnReasonLabel = "Pourquoi revenir : voir la date retenue et la suite du plan.",
            actionLabel = "Partager le vote",
            action = EventWorkspaceSummaryAction.OpenPoll
        )
        EventStatus.COMPARING -> EventViralLoopSummary(
            eventId = event.id,
            title = "Boucle de croissance",
            headline = "Décision à transformer en plan",
            inviteReasonLabel = "Pourquoi inviter : les retardataires voient les options avant la décision finale.",
            installReasonLabel = "Pourquoi installer : comparer destination, budget et contraintes au même endroit.",
            returnReasonLabel = "Pourquoi revenir : suivre le scénario choisi après la comparaison.",
            actionLabel = "Comparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.CONFIRMED -> EventViralLoopSummary(
            eventId = event.id,
            title = "Boucle de croissance",
            headline = "Date confirmée à diffuser",
            inviteReasonLabel = "Pourquoi inviter : l'événement a maintenant une date crédible à partager.",
            installReasonLabel = "Pourquoi installer : calendrier, budget, transport et programme restent centralisés.",
            returnReasonLabel = "Pourquoi revenir : préparer le départ et suivre les changements utiles.",
            actionLabel = "Préparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.ORGANIZING -> EventViralLoopSummary(
            eventId = event.id,
            title = "Boucle de croissance",
            headline = "Centre de contrôle actif",
            inviteReasonLabel = "Pourquoi inviter : les participants manquants ont besoin du plan à jour.",
            installReasonLabel = "Pourquoi installer : savoir où aller, qui vient, quoi payer et quoi faire ensuite.",
            returnReasonLabel = "Pourquoi revenir : suivre le jour J et les prochaines étapes.",
            actionLabel = "Piloter",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.FINALIZED -> EventViralLoopSummary(
            eventId = event.id,
            title = "Boucle de croissance",
            headline = "Réutilisation après événement",
            inviteReasonLabel = "Pourquoi inviter : partager le récap, les photos et les remboursements.",
            installReasonLabel = "Pourquoi installer : récupérer ce qui reste à solder sans refaire un groupe.",
            returnReasonLabel = "Pourquoi revenir : recréer une nouvelle édition en un geste.",
            actionLabel = "Réutiliser",
            action = EventWorkspaceSummaryAction.RecreateFromTemplate,
            template = event.workspaceCreationTemplate()
        )
    }
}

private fun Event.viralLoopPriority(currentUserId: String, voteCount: Int): Int =
    when (status) {
        EventStatus.ORGANIZING -> 60
        EventStatus.CONFIRMED -> 55
        EventStatus.POLLING -> 50 + (participants.size - voteCount).coerceAtLeast(0)
        EventStatus.COMPARING -> 45
        EventStatus.FINALIZED -> 35
        EventStatus.DRAFT -> if (organizerId == currentUserId) 25 else 10
    }

private fun Event.workspaceActionPriority(currentUserId: String): Int =
    when (status) {
        EventStatus.DRAFT -> if (organizerId == currentUserId) 0 else 5
        EventStatus.POLLING -> 1
        EventStatus.COMPARING -> 2
        EventStatus.CONFIRMED -> 3
        EventStatus.ORGANIZING -> 4
        EventStatus.FINALIZED -> 6
    }

private fun Event.workspaceActionTitle(currentUserId: String): String =
    when (status) {
        EventStatus.DRAFT -> if (organizerId == currentUserId) {
            "Reprenez ${title}"
        } else {
            "${title} est en préparation"
        }
        EventStatus.POLLING -> "Faites avancer le sondage"
        EventStatus.COMPARING -> "Choisissez la meilleure option"
        EventStatus.CONFIRMED -> "Préparez ${title}"
        EventStatus.ORGANIZING -> "Pilotez ${title}"
        EventStatus.FINALIZED -> "Réorganisez ${title}"
    }

private fun Event.workspaceActionBody(currentUserId: String, voteCount: Int): String =
    when (status) {
        EventStatus.DRAFT -> if (organizerId == currentUserId) {
            "Terminez les infos manquantes pour lancer le vote sans repartir de zéro."
        } else {
            "L'organisateur finalise encore les détails avant d'inviter le groupe."
        }
        EventStatus.POLLING -> {
            val missingVotes = (participants.size - voteCount).coerceAtLeast(0)
            if (missingVotes == 0) {
                "Tous les votes connus sont enregistrés; ouvrez l'événement pour confirmer la suite."
            } else {
                "$missingVotes participant${if (missingVotes > 1) "s" else ""} à relancer avant de bloquer une date."
            }
        }
        EventStatus.COMPARING -> "Comparez destination, budget et contraintes avant de sélectionner le scénario final."
        EventStatus.CONFIRMED -> "Partagez l'invitation, ajoutez le calendrier et préparez budget, transport et activités."
        EventStatus.ORGANIZING -> "Suivez les prochaines décisions et les tâches critiques depuis le centre de contrôle."
        EventStatus.FINALIZED -> "Repartez de ce qui a marché: titre, description et type sont préremplis pour créer une nouvelle édition."
    }

private fun Event.workspaceActionButtonLabel(currentUserId: String): String =
    when (status) {
        EventStatus.DRAFT -> if (organizerId == currentUserId) "Continuer" else "Voir"
        EventStatus.POLLING -> "Ouvrir le vote"
        EventStatus.COMPARING -> "Comparer"
        EventStatus.CONFIRMED -> "Préparer"
        EventStatus.ORGANIZING -> "Piloter"
        EventStatus.FINALIZED -> "Réutiliser"
    }

private fun Event.workspaceSummaryAction(): EventWorkspaceSummaryAction =
    when (status) {
        EventStatus.POLLING -> EventWorkspaceSummaryAction.OpenPoll
        EventStatus.FINALIZED -> EventWorkspaceSummaryAction.RecreateFromTemplate
        else -> EventWorkspaceSummaryAction.OpenEvent
    }

internal fun Event.workspaceCreationTemplate(): EventWorkspaceCreationTemplate? =
    if (status == EventStatus.FINALIZED) {
        EventWorkspaceCreationTemplate(
            title = title,
            description = description,
            eventType = eventType
        )
    } else {
        null
    }

internal fun Event.toReorganizationSummary(): EventReorganizationSummary? {
    val template = workspaceCreationTemplate() ?: return null

    return EventReorganizationSummary(
        title = "Réorganiser rapidement",
        body = "Créez une nouvelle édition de $title avec le titre, la description et le type déjà repris.",
        actionLabel = "Créer une nouvelle édition",
        template = template
    )
}

internal fun List<Event>.toEventWidgetSummary(
    now: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    currentUserId: String
): EventWidgetSummary {
    val today = now.toLocalDateTime(timeZone).date

    val eventsWithDate = mapNotNull { event ->
        event.widgetStartInstant()?.let { start -> event to start }
    }

    val todayEvent = eventsWithDate
        .filter { (event, start) ->
            event.status in listOf(EventStatus.CONFIRMED, EventStatus.ORGANIZING) &&
                start.toLocalDateTime(timeZone).date == today
        }
        .minByOrNull { (_, start) -> start.toEpochMilliseconds() }
        ?.first

    if (todayEvent != null) {
        return EventWidgetSummary(
            kind = EventWidgetKind.EventToday,
            eventId = todayEvent.id,
            title = "Aujourd'hui",
            headline = todayEvent.title,
            body = "${todayEvent.participants.size} participant${if (todayEvent.participants.size > 1) "s" else ""} attendus",
            userInterestLabel = "Interet utilisateur : 10/10",
            rationaleLabel = "Widget prioritaire : il evite d'ouvrir l'app pour retrouver le rendez-vous du jour.",
            actionLabel = "Ouvrir"
        )
    }

    val nextDatedEvent = eventsWithDate
        .filter { (event, start) ->
            event.status != EventStatus.FINALIZED &&
                start.toLocalDateTime(timeZone).date >= today
        }
        .minByOrNull { (_, start) -> start.toEpochMilliseconds() }

    if (nextDatedEvent != null) {
        val (event, start) = nextDatedEvent
        val eventDate = start.toLocalDateTime(timeZone).date
        val daysUntil = today.daysUntil(eventDate)
        if (event.isTravelWidgetCandidate()) {
            return EventWidgetSummary(
                kind = EventWidgetKind.Travel,
                eventId = event.id,
                title = "Voyage à préparer",
                headline = event.title,
                body = "${event.participants.size} participant${if (event.participants.size > 1) "s" else ""} - transport, budget et programme à vérifier",
                userInterestLabel = "Interet utilisateur : 9/10",
                rationaleLabel = "Widget voyage pertinent : il regroupe les points qui cassent les groupes longs.",
                actionLabel = if (event.organizerId == currentUserId) "Piloter" else "Préparer"
            )
        }
        return EventWidgetSummary(
            kind = EventWidgetKind.Countdown,
            eventId = event.id,
            title = event.title,
            headline = if (daysUntil == 1) "Demain" else "J-$daysUntil",
            body = event.workspaceNextActionLabel(isOrganizer = event.organizerId == currentUserId),
            userInterestLabel = "Interet utilisateur : 8/10",
            rationaleLabel = "Widget compte a rebours utile : il cree de l'anticipation sans spammer le groupe.",
            actionLabel = "Préparer"
        )
    }

    val nextTask = filterNot { it.status == EventStatus.FINALIZED }
        .minWithOrNull(compareBy<Event> { it.workspaceActionPriority(currentUserId) }.thenBy { it.updatedAt })

    if (nextTask != null) {
        return EventWidgetSummary(
            kind = EventWidgetKind.NextTask,
            eventId = nextTask.id,
            title = "Prochaine tâche",
            headline = nextTask.workspaceActionTitle(currentUserId),
            body = nextTask.workspaceActionButtonLabel(currentUserId),
            userInterestLabel = "Interet utilisateur : 7/10",
            rationaleLabel = "Widget tache utile : il ramene l'organisateur vers le prochain blocage.",
            actionLabel = "Continuer"
        )
    }

    return EventWidgetSummary(
        kind = EventWidgetKind.Empty,
        eventId = null,
        title = "Wakeve",
        headline = "Aucun événement actif",
        body = "Créez un événement pour afficher le prochain rendez-vous ici.",
        userInterestLabel = "Interet utilisateur : a activer",
        rationaleLabel = "Aucun widget utile sans evenement actif.",
        actionLabel = "Créer"
    )
}

private fun Event.widgetStartInstant(): Instant? {
    val final = finalDate?.let { raw -> runCatching { Instant.parse(raw) }.getOrNull() }
    if (final != null) return final

    return proposedSlots
        .mapNotNull { slot -> slot.start?.let { raw -> runCatching { Instant.parse(raw) }.getOrNull() } }
        .minByOrNull { it.toEpochMilliseconds() }
}

private fun Event.isTravelWidgetCandidate(): Boolean {
    if (eventType == EventType.OUTDOOR_ACTIVITY) return true

    val searchableText = "$title $description ${eventType.name}".lowercase()
    return listOf(
        "trip",
        "travel",
        "voyage",
        "weekend",
        "week-end",
        "road trip",
        "retreat",
        "séjour",
        "sejour"
    ).any { keyword -> keyword in searchableText }
}
