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
    val deadlineLabel: String,
    val participantsLabel: String,
    val isOrganizer: Boolean
)

data class EventWorkspaceUiState(
    val isLoading: Boolean,
    val error: String?,
    val selectedFilter: EventListFilter,
    val searchQuery: String,
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
        statusLabel = status.name.lowercase().replaceFirstChar { it.titlecase() },
        deadlineLabel = "Deadline $deadline",
        participantsLabel = "${participants.size} participant${if (participants.size == 1) "" else "s"}",
        isOrganizer = organizerId == currentUserId
    )
