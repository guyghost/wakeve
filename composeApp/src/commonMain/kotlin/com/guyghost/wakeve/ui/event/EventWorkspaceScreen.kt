package com.guyghost.wakeve.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.ui.designsystem.WakeveButtonGroup
import com.guyghost.wakeve.ui.designsystem.WakeveCard
import com.guyghost.wakeve.ui.designsystem.WakeveAdaptiveInfo
import com.guyghost.wakeve.ui.designsystem.WakeveProgressIndicator
import com.guyghost.wakeve.ui.designsystem.WakeveScaffold
import com.guyghost.wakeve.ui.designsystem.WakeveSearchBar
import com.guyghost.wakeve.ui.designsystem.WakeveSegmentedOptions
import com.guyghost.wakeve.ui.designsystem.WakeveSize
import com.guyghost.wakeve.ui.designsystem.WakeveSpacing
import com.guyghost.wakeve.ui.designsystem.WakeveStateMessage
import com.guyghost.wakeve.ui.designsystem.WakeveStatusChip
import com.guyghost.wakeve.ui.designsystem.calculateWakeveAdaptiveInfo

@Composable
fun EventWorkspaceScreen(
    state: EventWorkspaceUiState,
    onFilterChange: (EventListFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onCreateEvent: () -> Unit,
    onCreateFromTemplate: (EventWorkspaceCreationTemplate) -> Unit,
    onOpenProfile: () -> Unit,
    onSelectEvent: (String, Boolean) -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenPoll: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    adaptiveInfoOverride: WakeveAdaptiveInfo? = null
) {
    BoxWithConstraints(modifier = modifier) {
        val availableWidth = maxWidth
        val adaptiveInfo = adaptiveInfoOverride
            ?: calculateWakeveAdaptiveInfo(
                widthDp = maxWidth.value.toInt(),
                heightDp = maxHeight.value.toInt()
            )

        WakeveScaffold(
            title = "Wakeve",
            modifier = Modifier.fillMaxSize(),
            showTopBar = !adaptiveInfo.useCompactChrome,
            actions = {
                IconButton(onClick = onOpenProfile) {
                    Icon(Icons.Default.Person, contentDescription = "Profil")
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onCreateEvent) {
                    Icon(Icons.Default.Add, contentDescription = "Créer un événement")
                }
            }
        ) { padding ->
            if (adaptiveInfo.supportsListDetail) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    val plannedListPaneWidth = when {
                        adaptiveInfo.widthDp >= 1440 -> 960.dp
                        adaptiveInfo.widthDp >= 1200 -> 720.dp
                        adaptiveInfo.widthDp >= 840 -> 420.dp
                        else -> WakeveSize.eventListPaneWidth
                    }
                    val listPaneWidth = minOf(
                        plannedListPaneWidth,
                        (availableWidth.value * 0.48f).dp
                    )
                    EventListPane(
                        state = state,
                        selectedEventId = state.selectedEvent?.id,
                        adaptiveInfo = calculateWakeveAdaptiveInfo(
                            widthDp = listPaneWidth.value.toInt(),
                            heightDp = adaptiveInfo.heightDp
                        ),
                        onFilterChange = onFilterChange,
                        onSearchChange = onSearchChange,
                        onSelectEvent = { eventId -> onSelectEvent(eventId, false) },
                        onSummaryAction = { summary ->
                            when (summary.action) {
                                EventWorkspaceSummaryAction.OpenEvent -> onSelectEvent(summary.eventId, false)
                                EventWorkspaceSummaryAction.OpenPoll -> onOpenPoll(summary.eventId)
                                EventWorkspaceSummaryAction.RecreateFromTemplate -> summary.template?.let(onCreateFromTemplate)
                            }
                        },
                        onViralLoopAction = { summary ->
                            when (summary.action) {
                                EventWorkspaceSummaryAction.OpenEvent -> summary.eventId?.let { onSelectEvent(it, false) }
                                EventWorkspaceSummaryAction.OpenPoll -> summary.eventId?.let(onOpenPoll)
                                EventWorkspaceSummaryAction.RecreateFromTemplate -> summary.template?.let(onCreateFromTemplate)
                                null -> onCreateEvent()
                            }
                        },
                        onEmotionalAction = { summary ->
                            when (summary.action) {
                                EventWorkspaceSummaryAction.OpenEvent -> summary.eventId?.let { onSelectEvent(it, false) }
                                EventWorkspaceSummaryAction.OpenPoll -> summary.eventId?.let(onOpenPoll)
                                EventWorkspaceSummaryAction.RecreateFromTemplate -> summary.template?.let(onCreateFromTemplate)
                                null -> onCreateEvent()
                            }
                        },
                        onStrategicAction = { summary ->
                            when (summary.action) {
                                EventWorkspaceSummaryAction.OpenEvent -> summary.eventId?.let { onSelectEvent(it, false) }
                                EventWorkspaceSummaryAction.OpenPoll -> summary.eventId?.let(onOpenPoll)
                                EventWorkspaceSummaryAction.RecreateFromTemplate -> summary.template?.let(onCreateFromTemplate)
                                null -> onCreateEvent()
                            }
                        },
                        onRoadmapAction = { summary ->
                            when (summary.action) {
                                EventWorkspaceSummaryAction.OpenEvent -> summary.eventId?.let { onSelectEvent(it, false) }
                                EventWorkspaceSummaryAction.OpenPoll -> summary.eventId?.let(onOpenPoll)
                                EventWorkspaceSummaryAction.RecreateFromTemplate -> summary.template?.let(onCreateFromTemplate)
                                null -> onCreateEvent()
                            }
                        },
                        onWidgetAction = { summary ->
                            val eventId = summary.eventId
                            if (eventId == null) {
                                onCreateEvent()
                            } else {
                                onSelectEvent(eventId, false)
                            }
                        },
                        onRetry = onRetry,
                        modifier = Modifier
                            .width(listPaneWidth)
                            .fillMaxHeight()
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(WakeveSpacing.md)
                    ) {
                        EventDetailPane(
                            event = state.selectedEvent,
                            participantCount = state.participantCount,
                            pollVoteCount = state.pollVoteCount,
                            onOpenEvent = onOpenEvent,
                            onOpenPoll = onOpenPoll,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                EventListPane(
                    state = state,
                    selectedEventId = null,
                    adaptiveInfo = adaptiveInfo,
                    onFilterChange = onFilterChange,
                    onSearchChange = onSearchChange,
                    onSelectEvent = { eventId -> onSelectEvent(eventId, true) },
                    onSummaryAction = { summary ->
                        when (summary.action) {
                            EventWorkspaceSummaryAction.OpenEvent -> onSelectEvent(summary.eventId, true)
                            EventWorkspaceSummaryAction.OpenPoll -> onOpenPoll(summary.eventId)
                            EventWorkspaceSummaryAction.RecreateFromTemplate -> summary.template?.let(onCreateFromTemplate)
                        }
                    },
                    onViralLoopAction = { summary ->
                        when (summary.action) {
                            EventWorkspaceSummaryAction.OpenEvent -> summary.eventId?.let { onSelectEvent(it, true) }
                            EventWorkspaceSummaryAction.OpenPoll -> summary.eventId?.let(onOpenPoll)
                            EventWorkspaceSummaryAction.RecreateFromTemplate -> summary.template?.let(onCreateFromTemplate)
                            null -> onCreateEvent()
                        }
                    },
                    onEmotionalAction = { summary ->
                        when (summary.action) {
                            EventWorkspaceSummaryAction.OpenEvent -> summary.eventId?.let { onSelectEvent(it, true) }
                            EventWorkspaceSummaryAction.OpenPoll -> summary.eventId?.let(onOpenPoll)
                            EventWorkspaceSummaryAction.RecreateFromTemplate -> summary.template?.let(onCreateFromTemplate)
                            null -> onCreateEvent()
                        }
                    },
                    onStrategicAction = { summary ->
                        when (summary.action) {
                            EventWorkspaceSummaryAction.OpenEvent -> summary.eventId?.let { onSelectEvent(it, true) }
                            EventWorkspaceSummaryAction.OpenPoll -> summary.eventId?.let(onOpenPoll)
                            EventWorkspaceSummaryAction.RecreateFromTemplate -> summary.template?.let(onCreateFromTemplate)
                            null -> onCreateEvent()
                        }
                    },
                    onRoadmapAction = { summary ->
                        when (summary.action) {
                            EventWorkspaceSummaryAction.OpenEvent -> summary.eventId?.let { onSelectEvent(it, true) }
                            EventWorkspaceSummaryAction.OpenPoll -> summary.eventId?.let(onOpenPoll)
                            EventWorkspaceSummaryAction.RecreateFromTemplate -> summary.template?.let(onCreateFromTemplate)
                            null -> onCreateEvent()
                        }
                    },
                    onWidgetAction = { summary ->
                        val eventId = summary.eventId
                        if (eventId == null) {
                            onCreateEvent()
                        } else {
                            onSelectEvent(eventId, true)
                        }
                    },
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

@Composable
private fun EventListPane(
    state: EventWorkspaceUiState,
    selectedEventId: String?,
    adaptiveInfo: WakeveAdaptiveInfo,
    onFilterChange: (EventListFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectEvent: (String) -> Unit,
    onSummaryAction: (EventWorkspaceActionSummary) -> Unit,
    onViralLoopAction: (EventViralLoopSummary) -> Unit,
    onEmotionalAction: (EventEmotionalSummary) -> Unit,
    onStrategicAction: (EventStrategicSummary) -> Unit,
    onRoadmapAction: (EventRoadmapSummary) -> Unit,
    onWidgetAction: (EventWidgetSummary) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .testTag("event_list_pane")
            .padding(WakeveSpacing.md),
        verticalArrangement = Arrangement.spacedBy(WakeveSpacing.md)
    ) {
        WakeveSearchBar(
            query = state.searchQuery,
            onQueryChange = onSearchChange,
            placeholder = "Rechercher un événement"
        )
        WakeveSegmentedOptions(
            options = EventListFilter.entries,
            selected = state.selectedFilter,
            label = { it.label },
            onSelected = onFilterChange,
            layout = adaptiveInfo.filterLayout,
            modifier = Modifier.testTag("event_filter_${adaptiveInfo.filterLayout.name.lowercase()}")
        )
        EventWidgetSummaryCard(
            summary = state.widgetSummary,
            onAction = { onWidgetAction(state.widgetSummary) }
        )
        EventViralLoopSummaryCard(
            summary = state.viralLoopSummary,
            onAction = { onViralLoopAction(state.viralLoopSummary) }
        )
        EventEmotionalSummaryCard(
            summary = state.emotionalSummary,
            onAction = { onEmotionalAction(state.emotionalSummary) }
        )
        EventStrategicSummaryCard(
            summary = state.strategicSummary,
            onAction = { onStrategicAction(state.strategicSummary) }
        )
        EventRoadmapSummaryCard(
            summary = state.roadmapSummary,
            onAction = { onRoadmapAction(state.roadmapSummary) }
        )
        state.actionSummary?.let { summary ->
            WorkspaceActionSummaryCard(
                summary = summary,
                onAction = { onSummaryAction(summary) }
            )
        }
        when {
            state.isLoading && state.events.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    WakeveProgressIndicator()
                }
            }
            state.error != null && state.events.isEmpty() -> {
                WakeveStateMessage(
                    title = "Impossible de charger les événements",
                    body = state.error,
                    actionLabel = "Réessayer",
                    onAction = onRetry,
                    modifier = Modifier.fillMaxSize()
                )
            }
            state.events.isEmpty() -> {
                WakeveStateMessage(
                    title = "Aucun événement",
                    body = "Créez un événement ou changez de filtre pour continuer.",
                    actionLabel = "Actualiser",
                    onAction = onRetry,
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(adaptiveInfo.eventGridColumns),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("event_grid_columns_${adaptiveInfo.eventGridColumns}"),
                    contentPadding = PaddingValues(bottom = WakeveSpacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)
                ) {
                    items(state.events, key = { it.id }) { event ->
                        EventListRow(
                            event = event,
                            selected = event.id == selectedEventId,
                            onClick = { onSelectEvent(event.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventListRow(
    event: EventListItemUiState,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(
        selected = selected,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                WakeveStatusChip(label = event.statusLabel)
            }
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Text(
                text = event.nextActionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)) {
                Text(
                    text = event.participantsLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.isOrganizer) {
                    Text(
                        text = "Organisateur",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EventWidgetSummaryCard(
    summary: EventWidgetSummary,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("event_widget_summary")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)
                ) {
                    Text(
                        text = summary.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = summary.headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = summary.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    if (summary.userInterestLabel.isNotBlank()) {
                        Text(
                            text = summary.userInterestLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (summary.rationaleLabel.isNotBlank()) {
                        Text(
                            text = summary.rationaleLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
                TextButton(onClick = onAction) {
                    Text(summary.actionLabel)
                }
            }
        }
    }
}

@Composable
private fun EventViralLoopSummaryCard(
    summary: EventViralLoopSummary,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("event_viral_loop_summary")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)
                ) {
                    Text(
                        text = summary.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = summary.headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(onClick = onAction) {
                    Text(summary.actionLabel)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
                Text(
                    text = summary.inviteReasonLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = summary.installReasonLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = summary.returnReasonLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EventEmotionalSummaryCard(
    summary: EventEmotionalSummary,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("event_emotional_summary")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)
                ) {
                    Text(
                        text = summary.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = summary.headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = summary.scoreLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(onClick = onAction) {
                    Text(summary.actionLabel)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
                listOf(
                    summary.excitementLabel,
                    summary.anticipationLabel,
                    summary.engagementLabel,
                    summary.groupFeelingLabel,
                    summary.serenityLabel,
                    summary.controlLabel
                ).forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                Text(
                    text = summary.nextActionLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun EventStrategicSummaryCard(
    summary: EventStrategicSummary,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("event_strategic_summary")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)
                ) {
                    Text(
                        text = summary.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = summary.headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(onClick = onAction) {
                    Text(summary.actionLabel)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
                listOf(
                    summary.verdictLabel,
                    summary.scorecardLabel,
                    summary.honestAnswerLabel,
                    summary.competitorLabel,
                    summary.operatingSystemLabel,
                    summary.criticalProblemLabel,
                    summary.valueFeatureLabel,
                    summary.missingCapabilityLabel
                ).forEachIndexed { index, label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (index == 0) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 2
                    )
                }
                Text(
                    text = summary.nextActionLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun EventRoadmapSummaryCard(
    summary: EventRoadmapSummary,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("event_roadmap_summary")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)
                ) {
                    Text(
                        text = summary.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = summary.headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(onClick = onAction) {
                    Text(summary.actionLabel)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
                listOf(
                    summary.firstMonthLabel,
                    summary.secondQuarterLabel,
                    summary.sixthMonthLabel,
                    summary.teamFocusLabel
                ).forEachIndexed { index, label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (index == 0) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceActionSummaryCard(
    summary: EventWorkspaceActionSummary,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WakeveSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)
            ) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = summary.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
            TextButton(onClick = onAction) {
                Text(summary.actionLabel)
            }
        }
    }
}

@Composable
private fun EventDetailPane(
    event: Event?,
    participantCount: Int,
    pollVoteCount: Int,
    onOpenEvent: (String) -> Unit,
    onOpenPoll: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (event == null) {
        WakeveStateMessage(
            title = "Sélectionnez un événement",
            body = "Les détails apparaîtront ici sur les grands écrans.",
            icon = Icons.Default.CalendarMonth,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.testTag("event_detail_pane"),
        contentPadding = PaddingValues(WakeveSpacing.md),
        verticalArrangement = Arrangement.spacedBy(WakeveSpacing.md)
    ) {
        item {
            WakeveCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.md)) {
                    WakeveStatusChip(label = event.status.displayLabel())
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(WakeveSpacing.md)) {
                        DetailMetric(icon = Icons.Default.Group, label = "Participants", value = participantCount.toString())
                        DetailMetric(icon = Icons.Default.CalendarMonth, label = "Votes", value = pollVoteCount.toString())
                    }
                    WakeveButtonGroup {
                        Button(
                            onClick = { onOpenEvent(event.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ouvrir")
                        }
                        if (event.status == EventStatus.POLLING) {
                            Button(
                                onClick = { onOpenPoll(event.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Voter")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun EventStatus.displayLabel(): String {
    return when (this) {
        EventStatus.DRAFT -> "Brouillon"
        EventStatus.POLLING -> "Vote en cours"
        EventStatus.COMPARING -> "Comparaison"
        EventStatus.CONFIRMED -> "Confirmé"
        EventStatus.ORGANIZING -> "Organisation"
        EventStatus.FINALIZED -> "Finalisé"
    }
}

@Composable
private fun DetailMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(WakeveSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(text = value, style = MaterialTheme.typography.titleMedium)
                Text(text = label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
