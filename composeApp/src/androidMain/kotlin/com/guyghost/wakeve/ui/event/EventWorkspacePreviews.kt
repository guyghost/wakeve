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
            title = "Invitations et retours",
            headline = "1 vote à obtenir",
            inviteReasonLabel = "Invitation : chaque invité débloque la décision collective.",
            installReasonLabel = "Pourquoi ouvrir Wakeve : voter, suivre la date limite et éviter les relances privées.",
            returnReasonLabel = "À suivre : voir la date retenue et la suite du plan.",
            actionLabel = "Partager le vote",
            action = EventWorkspaceSummaryAction.OpenPoll
        ),
        emotionalSummary = EventEmotionalSummary(
            eventId = events.first().id,
            title = "Ambiance du groupe",
            headline = "Engagement à débloquer",
            scoreLabel = "Confiance du groupe : 58/100",
            excitementLabel = "Excitation : moyenne, le groupe commence à se projeter.",
            anticipationLabel = "Anticipation : liée à la date qui va sortir du vote.",
            engagementLabel = "Engagement : 1 participant à relancer.",
            groupFeelingLabel = "Sentiment de groupe : visible grâce aux réponses partagées.",
            serenityLabel = "Sérénité : encore fragile tant que la date n'est pas retenue.",
            controlLabel = "Contrôle : meilleur que WhatsApp, mais la décision reste ouverte.",
            nextActionLabel = "Relancez les votes manquants avant de promettre une date.",
            actionLabel = "Ouvrir le vote",
            action = EventWorkspaceSummaryAction.OpenPoll
        ),
        strategicSummary = EventStrategicSummary(
            eventId = events.first().id,
            title = "Prochaine décision",
            headline = "Date à décider",
            verdictLabel = "État : le vote avance, mais la préparation n'est pas encore ouverte.",
            scorecardLabel = "Priorité : concentrez le groupe sur la prochaine action utile.",
            honestAnswerLabel = "Confiance : adapté à un événement simple, encore fragile pour un grand groupe.",
            competitorLabel = "À éviter : laisser les votes et les relances se perdre dans le chat.",
            operatingSystemLabel = "Coordination : la première décision commune existe.",
            criticalProblemLabel = "Blocage : la valeur s'arrête si le vote ne débouche pas sur un plan.",
            valueFeatureLabel = "Action utile : conversion automatique du vote en plan de préparation.",
            missingCapabilityLabel = "Manque : transformer le vote en plan budget, transport et programme.",
            nextActionLabel = "Obtenez les votes manquants pour sortir du débat.",
            actionLabel = "Ouvrir le vote",
            action = EventWorkspaceSummaryAction.OpenPoll
        ),
        roadmapSummary = EventRoadmapSummary(
            eventId = events.first().id,
            title = "Plan d'action",
            headline = "Sortir du débat de date",
            firstMonthLabel = "Maintenant : relances utiles, vote lisible et décision sans ambiguïté.",
            secondQuarterLabel = "Ensuite : convertir la date retenue en budget, transport et programme.",
            sixthMonthLabel = "Plus tard : recommandations qui anticipent les blocages du groupe.",
            teamFocusLabel = "Focus : obtenez les votes manquants, confirmez la date, puis ouvrez la préparation.",
            actionLabel = "Ouvrir le vote",
            action = EventWorkspaceSummaryAction.OpenPoll
        ),
        widgetSummary = EventWidgetSummary(
            kind = EventWidgetKind.Countdown,
            eventId = events.first().id,
            title = "Lisbon team retreat",
            headline = "J-12",
            body = "Vote attendu",
            userInterestLabel = "Utilité : 8/10",
            rationaleLabel = "Compte à rebours : il cree de l'anticipation sans spammer le groupe.",
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
