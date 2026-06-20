package com.guyghost.wakeve.ui.event

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.preview.PreviewTheme

@Preview(name = "Event workspace - phone", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun EventWorkspacePhonePreview() {
    PreviewTheme {
        EventWorkspaceScreen(
            state = previewWorkspaceState(),
            onFilterChange = {},
            onSearchChange = {},
            onCreateEvent = {},
            onCreateFromTemplate = {},
            onOpenProfile = {},
            onSelectEvent = { _, _ -> },
            onOpenEvent = {},
            onOpenPoll = {},
            onRetry = {}
        )
    }
}

@Preview(name = "Event workspace - phone landscape", widthDp = 852, heightDp = 393, showBackground = true)
@Composable
private fun EventWorkspacePhoneLandscapePreview() {
    PreviewTheme {
        EventWorkspaceScreen(
            state = previewWorkspaceState(),
            onFilterChange = {},
            onSearchChange = {},
            onCreateEvent = {},
            onCreateFromTemplate = {},
            onOpenProfile = {},
            onSelectEvent = { _, _ -> },
            onOpenEvent = {},
            onOpenPoll = {},
            onRetry = {}
        )
    }
}

@Preview(name = "Event workspace - foldable", widthDp = 673, heightDp = 841, showBackground = true)
@Composable
private fun EventWorkspaceFoldablePreview() {
    PreviewTheme {
        EventWorkspaceScreen(
            state = previewWorkspaceState(),
            onFilterChange = {},
            onSearchChange = {},
            onCreateEvent = {},
            onCreateFromTemplate = {},
            onOpenProfile = {},
            onSelectEvent = { _, _ -> },
            onOpenEvent = {},
            onOpenPoll = {},
            onRetry = {}
        )
    }
}

@Preview(name = "Event workspace - tablet", widthDp = 1024, heightDp = 768, showBackground = true)
@Composable
private fun EventWorkspaceTabletPreview() {
    PreviewTheme {
        EventWorkspaceScreen(
            state = previewWorkspaceState(),
            onFilterChange = {},
            onSearchChange = {},
            onCreateEvent = {},
            onCreateFromTemplate = {},
            onOpenProfile = {},
            onSelectEvent = { _, _ -> },
            onOpenEvent = {},
            onOpenPoll = {},
            onRetry = {}
        )
    }
}

@Preview(name = "Event workspace - desktop no selection", widthDp = 1440, heightDp = 960, showBackground = true)
@Composable
private fun EventWorkspaceDesktopPreview() {
    PreviewTheme {
        EventWorkspaceScreen(
            state = previewWorkspaceState(selected = false),
            onFilterChange = {},
            onSearchChange = {},
            onCreateEvent = {},
            onCreateFromTemplate = {},
            onOpenProfile = {},
            onSelectEvent = { _, _ -> },
            onOpenEvent = {},
            onOpenPoll = {},
            onRetry = {}
        )
    }
}

private fun previewWorkspaceState(selected: Boolean = true): EventWorkspaceUiState {
    val events = previewEvents()
    return EventWorkspaceUiState(
        isLoading = false,
        error = null,
        selectedFilter = EventListFilter.Upcoming,
        searchQuery = "",
        actionSummary = EventWorkspaceActionSummary(
            eventId = events.first().id,
            title = "Faites avancer le sondage",
            body = "1 participant à relancer avant de bloquer une date.",
            actionLabel = "Ouvrir le vote",
            action = EventWorkspaceSummaryAction.OpenPoll
        ),
        viralLoopSummary = EventViralLoopSummary(
            eventId = events.first().id,
            title = "Boucle de croissance",
            headline = "1 vote à obtenir",
            inviteReasonLabel = "Pourquoi inviter : chaque invité débloque la décision collective.",
            installReasonLabel = "Pourquoi installer : voter, suivre la date limite et éviter les relances privées.",
            returnReasonLabel = "Pourquoi revenir : voir la date retenue et la suite du plan.",
            actionLabel = "Partager le vote",
            action = EventWorkspaceSummaryAction.OpenPoll
        ),
        widgetSummary = EventWidgetSummary(
            kind = EventWidgetKind.Countdown,
            eventId = events.first().id,
            title = "Lisbon team retreat",
            headline = "J-12",
            body = "Vote attendu",
            userInterestLabel = "Interet utilisateur : 8/10",
            rationaleLabel = "Widget compte a rebours utile : il cree de l'anticipation sans spammer le groupe.",
            actionLabel = "Préparer"
        ),
        events = events.map {
            EventListItemUiState(
                id = it.id,
                title = it.title,
                description = it.description,
                statusLabel = it.status.name.lowercase().replaceFirstChar { char -> char.titlecase() },
                nextActionLabel = "Vote attendu",
                deadlineLabel = "Deadline ${it.deadline}",
                participantsLabel = "${it.participants.size} participants",
                isOrganizer = it.organizerId == "preview-user"
            )
        },
        selectedEvent = events.first().takeIf { selected },
        participantCount = events.first().participants.size,
        pollVoteCount = 2
    )
}

private fun previewEvents(): List<Event> =
    listOf(
        Event(
            id = "preview-retreat",
            title = "Lisbon team retreat",
            description = "Three days to compare dates, pick a neighborhood, and organize travel.",
            organizerId = "preview-user",
            participants = listOf("alice", "bea", "sam"),
            proposedSlots = previewSlots(),
            deadline = "2026-07-01T12:00:00Z",
            status = EventStatus.POLLING,
            createdAt = "2026-06-01T08:00:00Z",
            updatedAt = "2026-06-02T08:00:00Z"
        ),
        Event(
            id = "preview-dinner",
            title = "Birthday dinner",
            description = "Confirm date and guest availability before booking.",
            organizerId = "friend",
            participants = listOf("preview-user", "camille"),
            proposedSlots = previewSlots(),
            deadline = "2026-06-24T18:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = "2026-06-10T08:00:00Z",
            updatedAt = "2026-06-11T08:00:00Z"
        )
    )

private fun previewSlots(): List<TimeSlot> =
    listOf(
        TimeSlot(
            id = "slot-1",
            start = "2026-07-14T09:00:00Z",
            end = "2026-07-14T18:00:00Z",
            timezone = "Europe/Paris"
        )
    )
