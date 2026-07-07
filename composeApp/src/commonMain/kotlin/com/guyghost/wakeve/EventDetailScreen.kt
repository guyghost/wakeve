package com.guyghost.wakeve

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.guyghost.wakeve.access.ParticipantRsvp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.payment.SettlementRecord
import com.guyghost.wakeve.postevent.PostEventControlSummary
import com.guyghost.wakeve.postevent.PostEventPrimaryAction
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.ui.components.HeroImageSection
import com.guyghost.wakeve.ui.designsystem.WakeveCard
import com.guyghost.wakeve.ui.designsystem.WakeveProgressIndicator
import com.guyghost.wakeve.ui.designsystem.WakeveSize
import com.guyghost.wakeve.ui.designsystem.WakeveSpacing
import com.guyghost.wakeve.ui.designsystem.WakeveStateMessage
import com.guyghost.wakeve.ui.event.EventAttendanceSummary
import com.guyghost.wakeve.ui.event.EventBudgetPlanningSummary
import com.guyghost.wakeve.ui.event.EventDayOfSummary
import com.guyghost.wakeve.ui.event.EventDetailUiState
import com.guyghost.wakeve.ui.event.EventDestinationSummary
import com.guyghost.wakeve.ui.event.EventNotificationSummary
import com.guyghost.wakeve.ui.event.EventProgramPlanningSummary
import com.guyghost.wakeve.ui.event.EventReorganizationSummary
import com.guyghost.wakeve.ui.event.EventSettlementSummary
import com.guyghost.wakeve.ui.event.EventWorkspaceCreationTemplate
import com.guyghost.wakeve.ui.event.EventRsvpResponseCard
import com.guyghost.wakeve.ui.event.EventScheduleSummary
import com.guyghost.wakeve.ui.event.toEventDetailUiState
import com.guyghost.wakeve.ui.event.toReorganizationSummary
import com.guyghost.wakeve.viewmodel.EventManagementViewModel
import com.guyghost.wakeve.weather.EventWeatherContext
import com.guyghost.wakeve.weather.WeatherAvailability
import kotlinx.datetime.toLocalDateTime

private object OrganizationUxLabels {
    val sectionKeys = listOf(
        "organization.section.participants",
        "organization.section.scenario",
        "organization.section.destination",
        "organization.section.lodging",
        "organization.section.transport",
        "organization.section.meetings",
        "organization.section.calendar",
        "organization.section.notifications",
        "organization.section.budget",
        "organization.section.payment",
        "organization.section.tricount",
        "organization.section.sync",
        "organization.section.unsafe_links",
        "organization.section.access_control"
    )
    val stateKeys = listOf(
        "organization.state.empty",
        "organization.state.optional_not_needed",
        "organization.state.incomplete",
        "organization.state.complete",
        "organization.state.pending_sync",
        "organization.state.failed_sync",
        "organization.state.access_denied"
    )
    const val pendingSyncCopy = "queued for sync; not server confirmed; pending server confirmation"
    const val failedSyncCopy = "failedSync ConflictDetected retry resolveConflict"
    const val optionalNotNeededCopy = "NotNeeded optional not needed"
    const val completeCopy = "Complete ready"
    const val incompleteCopy = "Incomplete missing required"
}

/**
 * Event detail screen showing full event information.
 *
 * This screen displays:
 * - Event title, description, and status
 * - List of participants
 * - Poll results and voting status
 * - Organizer-only actions (edit, delete)
 *
 * ## Architecture
 *
 * - State: Observed via `state.collectAsState()` from the ViewModel
 * - Intents: Dispatched via `viewModel.dispatch(intent)` for user actions
 * - Side Effects: Collected in LaunchedEffect for navigation and toasts
 *
 * ## Delete Functionality
 *
 * - Delete button is only visible if user is the organizer
 * - Delete is disabled for FINALIZED events (business rule)
 * - Shows confirmation dialog before deletion
 * - On deletion: removes event from repository, navigates back, shows toast
 *
 * @param eventId The ID of the event to display
 * @param userId The current user's ID (used for organizer check and delete permission)
 * @param viewModel The EventManagementViewModel providing state and intent handling
 * @param onNavigateTo Callback for navigation to other screens
 * @param onShowToast Callback for showing toast messages
 * @param onNavigateBack Callback for navigating back
 * @param modifier Modifier for customizing the layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    userId: String,
    viewModel: EventManagementViewModel,
    onNavigateTo: (String) -> Unit = {},
    onShowToast: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onShareInvite: ((eventId: String, eventTitle: String) -> Unit)? = null,
    onCreateFromTemplate: ((EventWorkspaceCreationTemplate) -> Unit)? = null,
    settlements: List<SettlementRecord> = emptyList(),
    modifier: Modifier = Modifier
) {
    // State from ViewModel
    val state by viewModel.state.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        viewModel.dispatch(EventManagementContract.Intent.SelectEvent(eventId))
    }

    // Handle side effects (navigation, toasts)
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is EventManagementContract.SideEffect.NavigateTo -> {
                    onNavigateTo(effect.route)
                }
                is EventManagementContract.SideEffect.ShowToast -> {
                    onShowToast(effect.message)
                }
                is EventManagementContract.SideEffect.NavigateBack -> {
                    onNavigateBack()
                }
                is EventManagementContract.SideEffect.ConflictDetected -> {
                    // Conflict banner — full ConflictResolutionDialog wired in a follow-up
                    onShowToast("⚠️ ${effect.criticalFieldCount} sync conflict(s) need your attention")
                }
            }
        }
    }

    val uiState = state.toEventDetailUiState(
        eventId = eventId,
        currentUserId = userId,
        settlements = settlements
    )

    EventDetailContent(
        state = uiState,
        onNavigateTo = onNavigateTo,
        onNavigateBack = onNavigateBack,
        onShareInvite = onShareInvite,
        onRsvpSelected = { response ->
            onShowToast("Réponse RSVP sélectionnée : ${response.toFrenchLabel()}")
        },
        onCreateFromTemplate = onCreateFromTemplate,
        onRequestDelete = { showDeleteConfirmation = true },
        modifier = modifier
    )

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Supprimer l'événement") },
            text = { Text("Êtes-vous sûr de vouloir supprimer cet événement ? Cette action est irréversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dispatch(EventManagementContract.Intent.DeleteEvent(eventId, userId))
                        showDeleteConfirmation = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "Confirmer la suppression définitive de l'événement"
                        role = Role.Button
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    modifier = Modifier.semantics {
                        contentDescription = "Annuler et garder l'événement"
                        role = Role.Button
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailContent(
    state: EventDetailUiState,
    onNavigateTo: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onShareInvite: ((eventId: String, eventTitle: String) -> Unit)?,
    onRsvpSelected: (ParticipantRsvp) -> Unit,
    onCreateFromTemplate: ((EventWorkspaceCreationTemplate) -> Unit)?,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val event = state.event
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails de l'événement") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    // Share / Invite button (always visible)
                    IconButton(
                        onClick = {
                            val title = event?.title ?: ""
                            onShareInvite?.invoke(state.eventId, title)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Partager l'invitation")
                    }
                    if (state.isOrganizer) {
                        IconButton(onClick = { onNavigateTo("edit_event/${state.eventId}") }) {
                            Icon(Icons.Default.Edit, contentDescription = "Éditer")
                        }
                    }
                    if (state.canDelete) {
                        IconButton(
                            onClick = onRequestDelete,
                            modifier = Modifier.semantics {
                                contentDescription = "Supprimer l'événement. Action irréversible."
                                role = Role.Button
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (event == null) {
            // Loading or not found state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoading) {
                    WakeveProgressIndicator()
                } else {
                    WakeveStateMessage(
                        title = "Événement non trouvé",
                        body = "Revenez à la liste pour sélectionner un événement disponible."
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(WakeveSpacing.md),
                verticalArrangement = Arrangement.spacedBy(WakeveSpacing.md)
            ) {
                // Hero Image Section (NEW - feature parity with iOS)
                item {
                    HeroImageSection(event = event)
                }

                // Event info card
                item {
                    EventInfoCard(event = event)
                }

                // Status card
                item {
                    StatusCard(event = event)
                }

                state.scheduleSummary?.let { summary ->
                    item {
                        EventScheduleSummaryCard(summary = summary)
                    }
                }

                state.attendanceSummary?.let { summary ->
                    item {
                        EventAttendanceSummaryCard(summary = summary)
                    }
                }

                state.destinationSummary?.let { summary ->
                    item {
                        EventDestinationSummaryCard(
                            summary = summary,
                            onOpenScenarios = { onNavigateTo("event/${event.id}/scenarios") }
                        )
                    }
                }

                state.notificationSummary?.let { summary ->
                    item {
                        EventNotificationSummaryCard(summary = summary)
                    }
                }

                state.postEventSummary?.let { summary ->
                    item {
                        EventPostEventSummaryCard(
                            summary = summary,
                            event = event,
                            onNavigateTo = onNavigateTo,
                            onCreateFromTemplate = onCreateFromTemplate
                        )
                    }
                }

                state.settlementSummary?.let { summary ->
                    item {
                        EventSettlementSummaryCard(
                            summary = summary,
                            onOpenBudget = { onNavigateTo("event/${event.id}/budget") }
                        )
                    }
                }

                if (state.postEventSummary == null) {
                    event.toReorganizationSummary()?.let { summary ->
                        item {
                            EventReorganizationCard(
                                summary = summary,
                                onCreateFromTemplate = onCreateFromTemplate
                            )
                        }
                    }
                }

                state.dayOfSummary?.let { summary ->
                    item {
                        EventDayOfSummaryCard(summary = summary)
                    }
                }

                state.programSummary?.let { summary ->
                    item {
                        EventProgramPlanningCard(
                            summary = summary,
                            onOpenProgram = { onNavigateTo("event/${event.id}/activities") }
                        )
                    }
                }

                state.budgetSummary?.let { summary ->
                    item {
                        EventBudgetPlanningCard(
                            summary = summary,
                            onOpenBudget = { onNavigateTo("event/${event.id}/budget") }
                        )
                    }
                }

                if (event.status in listOf(EventStatus.CONFIRMED, EventStatus.ORGANIZING, EventStatus.FINALIZED)) {
                    item {
                        AndroidWeatherStatusCard(
                            context = null,
                            event = event
                        )
                    }
                }

                state.rsvp?.let { rsvp ->
                    item {
                        EventRsvpResponseCard(
                            state = rsvp,
                            onResponseSelected = onRsvpSelected
                        )
                    }
                }

                if (state.showTransportPlanning) {
                    item {
                        TransportPlanningEntryCard(
                            event = event,
                            readOnly = event.status == EventStatus.FINALIZED,
                            onOpenTransport = {
                                onNavigateTo("event/${event.id}/transport")
                            }
                        )
                    }
                }

                if (state.showOrganizationTools) {
                    item {
                        Phase5OrganizationEntryCard(
                            event = event,
                            readOnly = event.status == EventStatus.FINALIZED,
                            onOpenMeetings = { onNavigateTo("event/${event.id}/meetings") },
                            onOpenActivities = { onNavigateTo("event/${event.id}/activities") },
                            onOpenBudget = { onNavigateTo("event/${event.id}/budget") },
                            onOpenPayment = { onNavigateTo("event/${event.id}/payment") },
                            onOpenTricount = { onNavigateTo("event/${event.id}/tricount") }
                        )
                    }
                }

                // Participants section
                item {
                    ParticipantsHeader()
                }

                items(state.participants) { participantId ->
                    ParticipantItem(participantId = participantId)
                }

                // Poll results section
                if (state.pollVotes.isNotEmpty()) {
                    item {
                        PollResultsHeader()
                    }

                    items(state.pollVotes.toList()) { (participantId, vote) ->
                        PollVoteItem(participantId = participantId, vote = vote)
                    }
                }

                // Action buttons (only for organizer)
                if (state.isOrganizer) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = WakeveSpacing.md),
                            horizontalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)
                        ) {
                            Button(
                                onClick = { onNavigateTo("edit_event/${state.eventId}") },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = WakeveSize.minTouchTarget)
                            ) {
                                Text("Éditer")
                            }
                            if (state.canDelete) {
                                FilledTonalButton(
                                    onClick = onRequestDelete,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = WakeveSize.minTouchTarget)
                                        .semantics {
                                            contentDescription = "Supprimer l'événement. Action irréversible."
                                            role = Role.Button
                                        }
                                ) {
                                    Text("Supprimer")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventDestinationSummaryCard(
    summary: EventDestinationSummary,
    onOpenScenarios: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = summary.primaryLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.detailsLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            summary.options.forEach { option ->
                Text(
                    text = "${option.typeLabel} - ${option.title}: ${option.body}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = summary.nextActionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (summary.canOpenScenarios) {
                Button(
                    onClick = onOpenScenarios,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = WakeveSize.minTouchTarget)
                ) {
                    Text("Voir les scenarios")
                }
            }
        }
    }
}

@Composable
private fun EventSettlementSummaryCard(
    summary: EventSettlementSummary,
    onOpenBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = summary.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = summary.totalLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            summary.lines.forEach { line ->
                Text(
                    text = line.sentence,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onOpenBudget,
                enabled = summary.canOpenBudget,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = WakeveSize.minTouchTarget)
            ) {
                Text(summary.actionLabel)
            }
        }
    }
}

@Composable
private fun EventPostEventSummaryCard(
    summary: PostEventControlSummary,
    event: Event,
    onNavigateTo: (String) -> Unit,
    onCreateFromTemplate: ((EventWorkspaceCreationTemplate) -> Unit)?,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.headline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = summary.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
                Text(
                    text = summary.settlementLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = summary.photoLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = summary.reorganizationLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = {
                    when (summary.primaryAction) {
                        PostEventPrimaryAction.OPEN_EVENT -> onNavigateTo("event/${event.id}")
                        PostEventPrimaryAction.OPEN_SETTLEMENTS -> onNavigateTo("event/${event.id}/budget")
                        PostEventPrimaryAction.OPEN_PHOTOS -> onNavigateTo("event/${event.id}/photos")
                        PostEventPrimaryAction.RECREATE_EVENT -> {
                            event.toReorganizationSummary()?.template?.let { template ->
                                onCreateFromTemplate?.invoke(template)
                            }
                        }
                    }
                },
                enabled = summary.primaryAction != PostEventPrimaryAction.RECREATE_EVENT ||
                    onCreateFromTemplate != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = WakeveSize.minTouchTarget)
            ) {
                Text(summary.primaryAction.postEventActionLabel())
            }
        }
    }
}

private fun PostEventPrimaryAction.postEventActionLabel(): String =
    when (this) {
        PostEventPrimaryAction.OPEN_EVENT -> "Ouvrir l'événement"
        PostEventPrimaryAction.OPEN_SETTLEMENTS -> "Voir les remboursements"
        PostEventPrimaryAction.OPEN_PHOTOS -> "Voir les photos"
        PostEventPrimaryAction.RECREATE_EVENT -> "Réorganiser"
    }

@Composable
private fun EventNotificationSummaryCard(
    summary: EventNotificationSummary,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)
            ) {
                Text(
                    text = summary.priorityLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = summary.spamRiskLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            summary.reminders.forEach { reminder ->
                Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
                    Text(
                        text = "${if (reminder.isRecommended) "Recommande" else "Attendre"} - ${reminder.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (reminder.isRecommended) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = "${reminder.priorityLabel} · ${reminder.spamRiskLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = reminder.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = summary.nextActionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventReorganizationCard(
    summary: EventReorganizationSummary,
    onCreateFromTemplate: ((EventWorkspaceCreationTemplate) -> Unit)?,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onCreateFromTemplate != null) {
                Button(
                    onClick = { onCreateFromTemplate(summary.template) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = WakeveSize.minTouchTarget)
                ) {
                    Text(summary.actionLabel)
                }
            }
        }
    }
}

@Composable
private fun EventDayOfSummaryCard(
    summary: EventDayOfSummary,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.controlLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Rendez-vous : ${summary.meetingPointLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Depart : ${summary.meetingTimeLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = summary.attendanceLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.missingLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = summary.arrivalTrackingLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.missingPeopleLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
                summary.checklist.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
                        Text(
                            text = "${item.statusLabel} - ${item.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.isBlocking) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = item.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                text = summary.nextActionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EventAttendanceSummaryCard(
    summary: EventAttendanceSummary,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (summary.hasSyncedRsvp) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)
            ) {
                Text(
                    text = summary.confirmedLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = summary.pendingLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = summary.declinedLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = summary.nextActionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventScheduleSummaryCard(
    summary: EventScheduleSummary,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (summary.hasConfirmedDate) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                }
            )
            Text(
                text = summary.primaryLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.deadlineLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = summary.nextActionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventProgramPlanningCard(
    summary: EventProgramPlanningSummary,
    onOpenProgram: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (summary.canOpenProgram) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                }
            )
            Text(
                text = summary.scopeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = summary.nextActionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (summary.canOpenProgram) {
                Button(
                    onClick = onOpenProgram,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = WakeveSize.minTouchTarget)
                ) {
                    Text("Ouvrir le programme")
                }
            } else {
                Text(
                    text = "Confirmez la date pour ouvrir le programme partage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EventBudgetPlanningCard(
    summary: EventBudgetPlanningSummary,
    onOpenBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = summary.participantBasisLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.scopeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = summary.nextActionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (summary.canOpenBudget) {
                Button(
                    onClick = onOpenBudget,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = WakeveSize.minTouchTarget)
                ) {
                    Text("Ouvrir le budget")
                }
            } else {
                Text(
                    text = "Confirmez votre participation pour ouvrir le budget.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AndroidWeatherStatusCard(
    context: EventWeatherContext?,
    event: Event,
    modifier: Modifier = Modifier
) {
    val availability = context?.availability ?: WeatherAvailability.PROVIDER_UNAVAILABLE
    val dailyForecasts = context?.dailyForecasts.orEmpty()
    val body = when (availability) {
        WeatherAvailability.AVAILABLE, WeatherAvailability.STALE -> {
            val forecast = dailyForecasts.firstOrNull()
            val temp = forecast?.let { daily ->
                val low = daily.temperatureLowCelsius?.let { "${it.toInt()}°" } ?: "--"
                val high = daily.temperatureHighCelsius?.let { "${it.toInt()}°" } ?: "--"
                "$low / $high"
            } ?: "Prévision disponible"
            val freshness = if (availability == WeatherAvailability.STALE) {
                "Données météo en cache, à actualiser."
            } else {
                "Prévision météo à jour."
            }
            "$temp · $freshness"
        }
        WeatherAvailability.PENDING_FORECAST_WINDOW ->
            "La météo de ${event.title} sera disponible quand la date entrera dans la fenêtre de prévision."
        WeatherAvailability.MISSING_LOCATION ->
            "Ajoutez ou précisez un lieu pour afficher la météo de l'événement."
        WeatherAvailability.OFFLINE_UNAVAILABLE ->
            "La météo sera disponible hors ligne après une première consultation en ligne."
        WeatherAvailability.PERMISSION_OR_ENTITLEMENT_REQUIRED ->
            "La météo nécessite une configuration fournisseur valide."
        WeatherAvailability.PROVIDER_UNAVAILABLE ->
            "Météo Android non configurée. Le fournisseur recommandé est Google Maps Platform Weather API."
    }

    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WakeveSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(WakeveSpacing.xl),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
                Text(
                    text = "Météo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Phase5OrganizationEntryCard(
    event: Event,
    readOnly: Boolean,
    onOpenMeetings: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenPayment: () -> Unit,
    onOpenTricount: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)
        ) {
            Text(
                text = "Organisation",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (readOnly) {
                    "Mode consultation pour ${event.title} : programme, réunions, budget, cagnotte et Tricount restent consultables. Aucune modification n'est possible."
                } else {
                    "Organiser le programme, les réunions, le budget, la cagnotte et Tricount pour ${event.title}."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onOpenActivities, modifier = Modifier.fillMaxWidth()) {
                Text("Programme")
            }
            Button(onClick = onOpenMeetings, modifier = Modifier.fillMaxWidth()) {
                Text("Réunions")
            }
            Button(onClick = onOpenBudget, modifier = Modifier.fillMaxWidth()) {
                Text("Budget et dépenses")
            }
            Button(onClick = onOpenPayment, modifier = Modifier.fillMaxWidth()) {
                Text("Cagnotte")
            }
            Button(onClick = onOpenTricount, modifier = Modifier.fillMaxWidth()) {
                Text("Tricount")
            }
        }
    }
}

@Composable
private fun EventInfoCard(
    event: Event,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = WakeveSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Créé le",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDateTime(event.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text(
                        text = "Organisateur",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = event.organizerId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    event: Event,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)
        ) {
            Text(
                text = "Statut",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(status = event.status)
                Text(
                    text = event.status.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TransportPlanningEntryCard(
    event: Event,
    readOnly: Boolean,
    onOpenTransport: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)
        ) {
            Text(
                text = "Transport",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (readOnly) {
                    "Mode consultation pour ${event.title} : départs, trajets et plan final restent consultables. Aucune modification n'est possible."
                } else {
                    "Planifier les départs, optimiser les trajets et retenir le plan final pour ${event.title}."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onOpenTransport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ouvrir le transport")
            }
        }
    }
}

@Composable
private fun ParticipantsHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = WakeveSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(WakeveSize.progressIndicator),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(WakeveSpacing.sm))
        Text(
            text = "Participants",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ParticipantItem(
    participantId: String,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(WakeveSpacing.lg),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(WakeveSpacing.sm))
            Text(
                text = participantId,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PollResultsHeader(modifier: Modifier = Modifier) {
    Text(
        text = "Résultats du sondage",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = WakeveSpacing.sm)
    )
}

@Composable
private fun PollVoteItem(
    participantId: String,
    vote: Vote,
    modifier: Modifier = Modifier
) {
    WakeveCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = participantId,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            VoteChip(vote = vote)
        }
    }
}

@Composable
private fun VoteChip(
    vote: Vote,
    modifier: Modifier = Modifier
) {
    val (label, containerColor, contentColor) = when (vote) {
        Vote.YES -> Triple("Oui", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        Vote.MAYBE -> Triple("Peut-être", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        Vote.NO -> Triple("Non", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }

    Surface(
        color = containerColor,
        modifier = modifier.padding(WakeveSpacing.xs),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = WakeveSpacing.sm, vertical = WakeveSpacing.xs)
        )
    }
}

private fun formatDateTime(isoDate: String): String {
    return try {
        val instant = kotlinx.datetime.Instant.parse(isoDate)
        val local = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        "${local.dayOfMonth} ${local.month.name.lowercase().take(3)} ${local.year} à ${local.hour}:${String.format("%02d", local.minute)}"
    } catch (e: Exception) {
        isoDate.take(10)
    }
}

private fun ParticipantRsvp.toFrenchLabel(): String =
    when (this) {
        ParticipantRsvp.ACCEPTED -> "oui"
        ParticipantRsvp.DECLINED -> "non"
        ParticipantRsvp.PENDING -> "peut-être"
    }
