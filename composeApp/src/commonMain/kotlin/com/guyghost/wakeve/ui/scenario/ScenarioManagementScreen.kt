package com.guyghost.wakeve.ui.scenario

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioGenerationType
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.models.ScenarioWithVotes
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract.Intent
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract.State
import kotlinx.coroutines.launch

/**
 * Scenario Management Screen for Android (Jetpack Compose)
 *
 * Displays a list of scenarios for an event with voting capabilities.
 * Features:
 * - List of scenarios sorted by score
 * - Pull-to-refresh
 * - Vote on scenarios (PREFER/NEUTRAL/AGAINST)
 * - Compare scenarios side-by-side
 * - Create/update/delete scenarios
 * - Detailed voting breakdown
 * - Empty states and loading indicators
 *
 * @param state The current state from ScenarioManagementStateMachine
 * @param onDispatch Callback to dispatch intents to the state machine
 * @param onNavigate Callback for navigation events
 * @param eventId The ID of the event
 * @param participantId The ID of the current participant
 * @param isOrganizer Whether the current user is the event organizer
 * @param modifier Modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioManagementScreen(
    state: State,
    onDispatch: (Intent) -> Unit,
    onNavigate: (String) -> Unit,
    eventId: String,
    participantId: String,
    isOrganizer: Boolean = false,
    isParticipantConfirmed: Boolean? = null,
    modifier: Modifier = Modifier
) {
    // Snackbar state for showing messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Pull-to-refresh state
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    // Dialog state for create/edit
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingScenario by remember { mutableStateOf<Scenario?>(null) }

    // Dialog state for delete confirmation
    var scenarioToDelete by remember { mutableStateOf<Scenario?>(null) }

    // Comparison state
    var selectedForComparison by remember { mutableStateOf(setOf<String>()) }
    var showComparisonMode by remember { mutableStateOf(false) }
    val canAccessScenarioDetails = isOrganizer || isParticipantConfirmed == true
    val canVote = canAccessScenarioDetails
    val canCreateScenario = isOrganizer && state.canCreateScenarios()

    // Load scenarios on first composition
    LaunchedEffect(Unit) {
        onDispatch(Intent.LoadScenariosForEvent(eventId, participantId))
    }

    // Handle side effects
    LaunchedEffect(Unit) {
        // Side effects would be emitted by the state machine
        // This is a placeholder for where you'd collect them in the actual integration
    }

    // Handle pull-to-refresh
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            onDispatch(Intent.LoadScenariosForEvent(eventId, participantId))
            isRefreshing = false
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            ScenarioManagementTopBar(
                showComparisonMode = showComparisonMode,
                comparisonCount = selectedForComparison.size,
                onClearComparison = { showComparisonMode = false; selectedForComparison = setOf() },
                onCompare = {
                    if (selectedForComparison.size >= 2) {
                        onDispatch(Intent.CompareScenarios(selectedForComparison.toList()))
                        onNavigate("event/$eventId/scenarios/compare")
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                scenarioCompareMinimumMessage(),
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (canCreateScenario && !showComparisonMode) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = scenarioCreateContentDescription())
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && state.scenarios.isEmpty() -> {
                    // Loading state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.scenarios.isEmpty() -> {
                    // Empty state
                    ScenarioEmptyState(
                        modifier = Modifier.fillMaxSize(),
                        canCreate = canCreateScenario,
                        isAccessLocked = !canAccessScenarioDetails,
                        onCreateClick = { showCreateDialog = true }
                    )
                }

                else -> {
                    // Scenarios list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            WorkflowStatusCard(
                                eventStatus = state.eventStatus,
                                isAccessLocked = !canAccessScenarioDetails,
                                lockMessage = state.error.takeIf { it.isConfirmedParticipantAccessError() }
                            )
                        }

                        if (!canAccessScenarioDetails) {
                            item {
                                LockedAccessMessage(
                                    message = scenarioAccessLockedDetailsMessage(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            items(
                                items = state.getScenariosRanked(),
                                key = { it.scenario.id }
                            ) { scenarioWithVotes ->
                                ScenarioCard(
                                    scenarioWithVotes = scenarioWithVotes,
                                    isSelected = scenarioWithVotes.scenario.id in selectedForComparison,
                                    isComparisonMode = showComparisonMode,
                                    onSelect = {
                                        if (showComparisonMode) {
                                            selectedForComparison = if (it in selectedForComparison) {
                                                selectedForComparison - it
                                            } else {
                                                selectedForComparison + it
                                            }
                                        } else {
                                            onDispatch(Intent.SelectScenario(it))
                                            onNavigate("event/$eventId/scenario/$it")
                                        }
                                    },
                                    onVote = { scenarioId, voteType ->
                                        onDispatch(Intent.VoteScenario(scenarioId, voteType))
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                scenarioVoteSubmittedMessage(),
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                    onEdit = {
                                        editingScenario = scenarioWithVotes.scenario
                                        showCreateDialog = true
                                    },
                                    onDelete = {
                                        scenarioToDelete = scenarioWithVotes.scenario
                                    },
                                    isOrganizer = isOrganizer,
                                    isLocked = scenarioWithVotes.scenario.status == ScenarioStatus.SELECTED,
                                    canAccessDetails = canAccessScenarioDetails,
                                    canVote = canVote
                                )
                            }
                        }
                    }
                }
            }

            // Error state overlay
            if (state.hasError) {
                ErrorBanner(
                    message = state.error ?: scenarioUnknownErrorMessage(),
                    onDismiss = { onDispatch(Intent.ClearError) },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }

    // Create/Edit scenario dialog
    if (showCreateDialog) {
        CreateScenarioDialog(
            scenario = editingScenario,
            onDismiss = {
                showCreateDialog = false
                editingScenario = null
            },
            onCreate = { scenario ->
                onDispatch(Intent.CreateScenario(scenario))
                showCreateDialog = false
                editingScenario = null
                scope.launch {
                    snackbarHostState.showSnackbar(
                        scenarioCreatedMessage(),
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onUpdate = { scenario ->
                onDispatch(Intent.UpdateScenario(scenario))
                showCreateDialog = false
                editingScenario = null
                scope.launch {
                    snackbarHostState.showSnackbar(
                        scenarioUpdatedMessage(),
                        duration = SnackbarDuration.Short
                    )
                }
            },
            eventId = eventId
        )
    }

    // Delete confirmation dialog
    if (scenarioToDelete != null) {
        DeleteConfirmationDialog(
            scenarioName = scenarioToDelete?.name ?: "",
            onConfirm = {
                scenarioToDelete?.id?.let { id ->
                    onDispatch(Intent.DeleteScenario(id))
                    scenarioToDelete = null
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            scenarioDeletedMessage(),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            onDismiss = {
                scenarioToDelete = null
            }
        )
    }
}

/**
 * Top app bar for scenario management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScenarioManagementTopBar(
    showComparisonMode: Boolean,
    comparisonCount: Int,
    onClearComparison: () -> Unit,
    onCompare: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            if (showComparisonMode) {
                Text(
                    scenarioComparisonTitle(comparisonCount),
                    style = MaterialTheme.typography.headlineSmall
                )
            } else {
                Text(
                    scenarioScreenTitle(),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        actions = {
            if (showComparisonMode) {
                IconButton(onClick = onClearComparison) {
                    Icon(Icons.Default.Edit, contentDescription = scenarioCancelComparisonContentDescription())
                }
                Button(
                    onClick = onCompare,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(scenarioCompareActionLabel())
                }
            }
        }
    )
}

/**
 * Scenario card showing scenario details and voting breakdown
 *
 * Displays:
 * - Scenario name and description (truncated)
 * - Date/period and location
 * - Voting breakdown (PREFER/NEUTRAL/AGAINST)
 * - Current score
 * - Number of participants
 * - Voting buttons for current participant
 *
 * @param scenarioWithVotes The scenario and its voting data
 * @param isSelected Whether this scenario is selected for comparison
 * @param isComparisonMode Whether we're in comparison mode
 * @param onSelect Callback when scenario is selected (tapped)
 * @param onVote Callback when user votes on the scenario
 * @param onEdit Callback when user wants to edit the scenario
 * @param onDelete Callback when user wants to delete the scenario
 * @param isOrganizer Whether current user is organizer
 * @param isLocked Whether the scenario is locked (selected)
 * @param modifier Modifier for the card
 */
@Composable
private fun ScenarioCard(
    scenarioWithVotes: ScenarioWithVotes,
    isSelected: Boolean,
    isComparisonMode: Boolean,
    onSelect: (String) -> Unit,
    onVote: (String, ScenarioVoteType) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isOrganizer: Boolean,
    isLocked: Boolean,
    canAccessDetails: Boolean,
    canVote: Boolean,
    modifier: Modifier = Modifier
) {
    val scenario = scenarioWithVotes.scenario
    val votingResult = scenarioWithVotes.votingResult

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (!isComparisonMode) {
                    onSelect(scenario.id)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with title and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Title with status indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = scenario.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (scenario.status == ScenarioStatus.SELECTED) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    scenarioSelectedStatusLabel(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(4.dp, 2.dp)
                                )
                            }
                        } else if (scenario.status == ScenarioStatus.DRAFT) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    scenarioDraftStatusLabel(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(4.dp, 2.dp)
                                )
                            }
                        }
                    }

                    if (scenario.generationType == ScenarioGenerationType.MATRIX) {
                        Text(
                            text = listOfNotNull(
                                scenario.sourceTimeSlotId?.let { scenarioMatrixSlotLabel(it) },
                                scenario.sourcePotentialLocationId?.let { scenarioMatrixDestinationLabel(it) }
                            ).joinToString(" • ", prefix = scenarioMatrixPrefix()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Description
                    Text(
                        text = scenario.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Edit/Delete buttons for organizer
                if (isOrganizer) {
                    Row {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = scenarioEditContentDescription(),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = scenarioDeleteContentDescription(),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Details row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailBadge(
                    icon = Icons.AutoMirrored.Outlined.EventNote,
                    text = scenarioPeriodLabel(scenario.dateOrPeriod),
                    modifier = Modifier.weight(1f)
                )
                DetailBadge(
                    icon = Icons.Outlined.PersonAdd,
                    text = scenarioParticipantsLabel(scenario.estimatedParticipants),
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = scenarioLocationLabel(scenario.location),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Budget and duration info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailBadge(
                    icon = Icons.Outlined.Info,
                    text = scenarioDurationLabel(scenario.duration),
                    modifier = Modifier.weight(1f)
                )
                DetailBadge(
                    icon = Icons.Outlined.Info,
                    text = scenarioBudgetLabel(scenario.estimatedBudgetPerPerson),
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Voting breakdown
            VotingBreakdown(
                votingResult = votingResult,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Voting buttons
            if (canVote || isLocked) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = false,
                        onClick = {
                            if (!isLocked && canVote) {
                                onVote(scenario.id, ScenarioVoteType.PREFER)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        label = { Text(scenarioPreferVoteLabel()) },
                        enabled = !isLocked && canVote
                    )
                    SegmentedButton(
                        selected = false,
                        onClick = {
                            if (!isLocked && canVote) {
                                onVote(scenario.id, ScenarioVoteType.NEUTRAL)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        label = { Text(scenarioNeutralVoteLabel()) },
                        enabled = !isLocked && canVote
                    )
                    SegmentedButton(
                        selected = false,
                        onClick = {
                            if (!isLocked && canVote) {
                                onVote(scenario.id, ScenarioVoteType.AGAINST)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        label = { Text(scenarioAgainstVoteLabel()) },
                        enabled = !isLocked && canVote
                    )
                }
            }

            if (isLocked) {
                Text(
                    text = scenarioVotingLockedMessage(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (!canAccessDetails) {
                LockedAccessMessage(
                    message = scenarioAccessLockedDetailsMessage(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Comparison checkbox
            if (isComparisonMode && canAccessDetails) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = isSelected,
                            onValueChange = { onSelect(scenario.id) }
                        )
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        scenarioSelectForComparisonLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                Text("✓", color = Color.White, modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detail badge for scenario information
 */
@Composable
private fun DetailBadge(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WorkflowStatusCard(
    eventStatus: EventStatus?,
    isAccessLocked: Boolean,
    lockMessage: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAccessLocked) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAccessLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = scenarioWorkflowStatusLabel(eventStatus),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isAccessLocked) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
                Text(
                    text = lockMessage ?: scenarioWorkflowAvailableMessage(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAccessLocked) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }
        }
    }
}

@Composable
private fun LockedAccessMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * Voting breakdown showing PREFER/NEUTRAL/AGAINST counts and percentages
 */
@Composable
private fun VotingBreakdown(
    votingResult: com.guyghost.wakeve.models.ScenarioVotingResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Score header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = scenarioVotingResultsTitle(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = scenarioScoreLabel(votingResult.score),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (votingResult.score > 0) {
                    MaterialTheme.colorScheme.primary
                } else if (votingResult.score < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        if (votingResult.totalVotes == 0) {
            Text(
                text = scenarioNoVotesMessage(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            // Vote breakdowns
            VoteBreakdownRow(
                label = scenarioPreferVoteBreakdownLabel(),
                count = votingResult.preferCount,
                percentage = votingResult.preferPercentage,
                color = MaterialTheme.colorScheme.primary
            )
            VoteBreakdownRow(
                label = scenarioNeutralVoteBreakdownLabel(),
                count = votingResult.neutralCount,
                percentage = votingResult.neutralPercentage,
                color = MaterialTheme.colorScheme.tertiary
            )
            VoteBreakdownRow(
                label = scenarioAgainstVoteBreakdownLabel(),
                count = votingResult.againstCount,
                percentage = votingResult.againstPercentage,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = scenarioTotalVotesLabel(votingResult.totalVotes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Single vote breakdown row showing count and percentage
 */
@Composable
private fun VoteBreakdownRow(
    label: String,
    count: Int,
    percentage: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(color.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentage.toFloat() / 100f)
                        .fillMaxSize()
                        .background(color, RoundedCornerShape(3.dp))
                )
            }
            Text(
                text = "$count (${String.format("%.0f", percentage)}%)",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(80.dp)
            )
        }
    }
}

/**
 * Empty state when no scenarios exist
 */
@Composable
private fun ScenarioEmptyState(
    modifier: Modifier = Modifier,
    canCreate: Boolean,
    isAccessLocked: Boolean,
    onCreateClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.EventNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isAccessLocked) scenarioAccessLockedTitle() else scenarioEmptyTitle(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isAccessLocked) {
                scenarioAccessLockedEmptyMessage()
            } else {
                scenarioEmptyMessage()
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (canCreate) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onCreateClick
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(scenarioCreateActionLabel())
            }
        }
    }
}

private fun String?.isConfirmedParticipantAccessError(): Boolean =
    this?.contains("confirmed participants", ignoreCase = true) == true ||
        this?.contains("confirmed participant", ignoreCase = true) == true ||
        this?.contains("participant confirmé", ignoreCase = true) == true

/**
 * Dialog for creating or editing a scenario
 */
@Composable
private fun CreateScenarioDialog(
    scenario: Scenario? = null,
    onDismiss: () -> Unit,
    onCreate: (Scenario) -> Unit,
    onUpdate: (Scenario) -> Unit,
    eventId: String,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(scenario?.name ?: "") }
    var description by remember { mutableStateOf(scenario?.description ?: "") }
    var dateOrPeriod by remember { mutableStateOf(scenario?.dateOrPeriod ?: "") }
    var location by remember { mutableStateOf(scenario?.location ?: "") }
    var duration by remember { mutableStateOf(scenario?.duration?.toString() ?: "3") }
    var estimatedParticipants by remember { mutableStateOf(scenario?.estimatedParticipants?.toString() ?: "5") }
    var budget by remember { mutableStateOf(scenario?.estimatedBudgetPerPerson?.toString() ?: "1000") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (scenario == null) scenarioCreateDialogTitle() else scenarioEditDialogTitle(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Form fields
                DialogTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = scenarioNameFieldLabel(),
                    placeholder = scenarioNamePlaceholder()
                )

                DialogTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = scenarioDescriptionFieldLabel(),
                    placeholder = scenarioDescriptionPlaceholder(),
                    maxLines = 3
                )

                DialogTextField(
                    value = dateOrPeriod,
                    onValueChange = { dateOrPeriod = it },
                    label = scenarioDateFieldLabel(),
                    placeholder = scenarioDatePlaceholder()
                )

                DialogTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = scenarioLocationFieldLabel(),
                    placeholder = scenarioLocationPlaceholder()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DialogTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = scenarioDurationFieldLabel(),
                        placeholder = "3",
                        modifier = Modifier.weight(1f)
                    )
                    DialogTextField(
                        value = estimatedParticipants,
                        onValueChange = { estimatedParticipants = it },
                        label = scenarioEstimatedPeopleFieldLabel(),
                        placeholder = "5",
                        modifier = Modifier.weight(1f)
                    )
                }

                DialogTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    label = scenarioBudgetFieldLabel(),
                    placeholder = "1000"
                )

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(scenarioCancelActionLabel())
                    }

                    Button(
                        onClick = {
                            try {
                                val newScenario = Scenario(
                                    id = scenario?.id ?: "scenario-${System.currentTimeMillis()}",
                                    eventId = eventId,
                                    name = name,
                                    description = description,
                                    dateOrPeriod = dateOrPeriod,
                                    location = location,
                                    duration = duration.toIntOrNull() ?: 3,
                                    estimatedParticipants = estimatedParticipants.toIntOrNull() ?: 5,
                                    estimatedBudgetPerPerson = budget.toDoubleOrNull() ?: 1000.0,
                                    status = scenario?.status ?: ScenarioStatus.PROPOSED,
                                    createdAt = scenario?.createdAt ?: "2025-11-25T10:00:00Z",
                                    updatedAt = "2025-11-25T10:00:00Z"
                                )
                                if (scenario == null) {
                                    onCreate(newScenario)
                                } else {
                                    onUpdate(newScenario)
                                }
                            } catch (e: Exception) {
                                // Handle validation error
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (scenario == null) scenarioCreateActionLabel() else scenarioUpdateActionLabel())
                    }
                }
            }
        }
    }
}

/**
 * Text field for use in dialogs
 */
@Composable
private fun DialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    maxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            maxLines = maxLines,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Delete confirmation dialog
 */
@Composable
private fun DeleteConfirmationDialog(
    scenarioName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Text(
                    text = scenarioDeleteDialogTitle(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = scenarioDeleteDialogMessage(scenarioName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(scenarioCancelActionLabel())
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(scenarioDeleteActionLabel())
                    }
                }
            }
        }
    }
}

/**
 * Error banner shown when there's an error
 */
@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = scenarioDismissContentDescription(),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Padding values for the screen
 */
private object ScenarioScreenDefaults {
    val HorizontalPadding = 16.dp
    val VerticalPadding = 12.dp
}

internal fun scenarioScreenTitle(): String = "Scenarios proposes"

internal fun scenarioComparisonTitle(comparisonCount: Int): String =
    "Comparer ($comparisonCount selectionnes)"

internal fun scenarioCompareActionLabel(): String = "Comparer"

internal fun scenarioCompareMinimumMessage(): String =
    "Selectionnez au moins 2 scenarios a comparer."

internal fun scenarioCancelComparisonContentDescription(): String =
    "Annuler la comparaison"

internal fun scenarioCreateContentDescription(): String = "Creer un scenario"

internal fun scenarioCreateActionLabel(): String = "Creer"

internal fun scenarioEditContentDescription(): String = "Modifier le scenario"

internal fun scenarioDeleteContentDescription(): String = "Supprimer le scenario"

internal fun scenarioVoteSubmittedMessage(): String = "Vote enregistre."

internal fun scenarioCreatedMessage(): String = "Scenario cree."

internal fun scenarioUpdatedMessage(): String = "Scenario mis a jour."

internal fun scenarioDeletedMessage(): String = "Scenario supprime."

internal fun scenarioUnknownErrorMessage(): String = "Erreur inconnue"

internal fun scenarioSelectedStatusLabel(): String = "Retenu"

internal fun scenarioDraftStatusLabel(): String = "Brouillon"

internal fun scenarioMatrixPrefix(): String = "Combinaison: "

internal fun scenarioMatrixSlotLabel(slotId: String): String = "creneau $slotId"

internal fun scenarioMatrixDestinationLabel(destinationId: String): String =
    "destination $destinationId"

internal fun scenarioPeriodLabel(dateOrPeriod: String): String = "Periode: $dateOrPeriod"

internal fun scenarioParticipantsLabel(count: Int): String = "$count participants"

internal fun scenarioLocationLabel(location: String): String = "Destination / logement: $location"

internal fun scenarioDurationLabel(days: Int): String =
    if (days <= 1) "$days jour" else "$days jours"

internal fun scenarioBudgetLabel(amount: Double): String = "Budget: $amount par personne"

internal fun scenarioPreferVoteLabel(): String = "Pour"

internal fun scenarioNeutralVoteLabel(): String = "Neutre"

internal fun scenarioAgainstVoteLabel(): String = "Contre"

internal fun scenarioPreferVoteBreakdownLabel(): String = "Pour"

internal fun scenarioNeutralVoteBreakdownLabel(): String = "Neutre"

internal fun scenarioAgainstVoteBreakdownLabel(): String = "Contre"

internal fun scenarioVotingLockedMessage(): String =
    "Ce scenario a ete retenu. Les votes sont verrouilles."

internal fun scenarioAccessLockedDetailsMessage(): String =
    "Confirmez votre presence pour voir les details et voter."

internal fun scenarioSelectForComparisonLabel(): String = "Selectionner pour comparaison"

internal fun scenarioWorkflowStatusLabel(eventStatus: EventStatus?): String =
    "Statut: ${eventStatus?.name ?: "INCONNU"}"

internal fun scenarioWorkflowAvailableMessage(): String =
    "Les scenarios de destination et de logement sont disponibles pour comparaison."

internal fun scenarioVotingResultsTitle(): String = "Resultats des votes"

internal fun scenarioScoreLabel(score: Int): String = "Score: $score"

internal fun scenarioNoVotesMessage(): String = "Aucun vote pour le moment"

internal fun scenarioTotalVotesLabel(totalVotes: Int): String = "Total: $totalVotes votes"

internal fun scenarioAccessLockedTitle(): String = "Acces aux scenarios verrouille"

internal fun scenarioEmptyTitle(): String = "Aucun scenario"

internal fun scenarioAccessLockedEmptyMessage(): String =
    "Confirmez votre presence pour acceder aux details de destination, logement et vote."

internal fun scenarioEmptyMessage(): String =
    "Creez un scenario pour aider les participants a comparer destination, logement et budget."

internal fun scenarioCreateDialogTitle(): String = "Creer un scenario"

internal fun scenarioEditDialogTitle(): String = "Modifier le scenario"

internal fun scenarioNameFieldLabel(): String = "Nom du scenario"

internal fun scenarioNamePlaceholder(): String = "Ex: week-end plage"

internal fun scenarioDescriptionFieldLabel(): String = "Description"

internal fun scenarioDescriptionPlaceholder(): String = "Details utiles pour comparer cette option"

internal fun scenarioDateFieldLabel(): String = "Date ou periode"

internal fun scenarioDatePlaceholder(): String = "Ex: 20-22 decembre 2025"

internal fun scenarioLocationFieldLabel(): String = "Lieu"

internal fun scenarioLocationPlaceholder(): String = "Ex: Marseille, France"

internal fun scenarioDurationFieldLabel(): String = "Duree (jours)"

internal fun scenarioEstimatedPeopleFieldLabel(): String = "Participants estimes"

internal fun scenarioBudgetFieldLabel(): String = "Budget par personne"

internal fun scenarioCancelActionLabel(): String = "Annuler"

internal fun scenarioUpdateActionLabel(): String = "Mettre a jour"

internal fun scenarioDeleteDialogTitle(): String = "Supprimer le scenario ?"

internal fun scenarioDeleteDialogMessage(scenarioName: String): String =
    "Voulez-vous vraiment supprimer \"$scenarioName\" ? Cette action est definitive."

internal fun scenarioDeleteActionLabel(): String = "Supprimer"

internal fun scenarioDismissContentDescription(): String = "Fermer"
