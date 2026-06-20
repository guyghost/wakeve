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

data class EventWorkspaceUiState(
    val isLoading: Boolean,
    val error: String?,
    val selectedFilter: EventListFilter,
    val searchQuery: String,
    val actionSummary: EventWorkspaceActionSummary?,
    val events: List<EventListItemUiState>,
    val selectedEvent: Event?,
    val participantCount: Int,
    val pollVoteCount: Int
)

fun EventManagementContract.State.toEventWorkspaceUiState(
    currentUserId: String,
    selectedFilter: EventListFilter,
    searchQuery: String,
    selectedEventId: String?
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

private fun Event.workspaceCreationTemplate(): EventWorkspaceCreationTemplate? =
    if (status == EventStatus.FINALIZED) {
        EventWorkspaceCreationTemplate(
            title = title,
            description = description,
            eventType = eventType
        )
    } else {
        null
    }
