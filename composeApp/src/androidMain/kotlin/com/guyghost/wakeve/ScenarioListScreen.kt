package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.models.ScenarioVotingResult
import com.guyghost.wakeve.models.ScenarioWithVotes
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract
import com.guyghost.wakeve.viewmodel.ScenarioManagementViewModel

/**
 * Scenario List Screen
 * 
 * Displays all scenarios for an event with voting interface.
 * Users can vote PREFER, NEUTRAL, or AGAINST for each scenario.
 * 
 * Uses the ScenarioManagementViewModel with StateFlow for state management.
 * Follows the State Machine (MVI) pattern with side effect handling.
 * 
 * ## Architecture
 * 
 * - State: Observed via `state.collectAsState()` from the ViewModel
 * - Intents: Dispatched via `viewModel.voteScenario()`, `viewModel.selectScenario()`, etc.
 * - Side Effects: Collected in LaunchedEffect for navigation and error handling
 * 
 * @param event The event to display scenarios for
 * @param viewModel The ScenarioManagementViewModel providing state and intent handling
 * @param onScenarioClick Callback when a scenario is clicked
 * @param onCreateScenario Callback to create a new scenario
 * @param onCompareScenarios Callback to compare scenarios
 */
@Composable
fun ScenarioListScreen(
    event: Event,
    viewModel: ScenarioManagementViewModel,
    onScenarioClick: (String) -> Unit,
    onCreateScenario: () -> Unit,
    onCompareScenarios: () -> Unit
) {
    // Collect state from ViewModel
    val state by viewModel.state.collectAsState()

    // Initialize on first composition or when event changes
    LaunchedEffect(event.id) {
        viewModel.initialize(event.id, "participant_id") // TODO: Get actual participant ID from context
    }

    // Handle side effects (error messages, navigation)
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is ScenarioManagementContract.SideEffect.ShowError -> {
                    // TODO: Show error via Snackbar or AlertDialog
                    println("Error: ${effect.message}")
                }
                is ScenarioManagementContract.SideEffect.ShowToast -> {
                    // TODO: Show toast message
                    println("Toast: ${effect.message}")
                }
                is ScenarioManagementContract.SideEffect.NavigateTo -> {
                    onScenarioClick(effect.route)
                }
                is ScenarioManagementContract.SideEffect.NavigateBack -> {
                    // Handled by caller or parent navigation
                }
                else -> {} // Handle other side effects if needed
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateScenario,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Scenario")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(paddingValues)
                .safeContentPadding()
                .padding(16.dp)
        ) {
            // Header
            Text(
                "Scenario Voting",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Event: ${event.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Compare Button (if multiple scenarios)
            if (state.scenarios.size >= 2) {
                Button(
                    onClick = {
                        viewModel.compareScenarios(state.scenarios.map { it.scenario.id })
                        onCompareScenarios()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text("Compare Scenarios")
                }
            }

            // Loading State
            if (state.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading scenarios...")
                }
                return@Scaffold
            }

            // Error State
            state.error?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Error Loading Scenarios",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // Empty State
            if (state.scenarios.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No scenarios yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Create a scenario to start planning",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onCreateScenario) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Scenario")
                    }
                }
                return@Scaffold
            }

            // Scenarios List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.scenarios, key = { it.scenario.id }) { scenarioWithVotes ->
                    // Get current user's vote from voting results
                    val userVote = state.votingResults[scenarioWithVotes.scenario.id]?.let { result ->
                        // Find user's vote in the voting results
                        scenarioWithVotes.votes.find { it.participantId == state.participantId }?.vote
                    }

                    ScenarioCard(
                        scenarioWithVotes = scenarioWithVotes,
                        userVote = userVote,
                        onVote = { voteType ->
                            viewModel.voteScenario(scenarioWithVotes.scenario.id, voteType)
                        },
                        onClick = {
                            viewModel.selectScenario(scenarioWithVotes.scenario.id)
                            onScenarioClick(scenarioWithVotes.scenario.id)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Individual Scenario Card with voting interface
 */
@Composable
fun ScenarioCard(
    scenarioWithVotes: ScenarioWithVotes,
    userVote: ScenarioVoteType?,
    onVote: (ScenarioVoteType) -> Unit,
    onClick: () -> Unit
) {
    val scenario = scenarioWithVotes.scenario
    val result = scenarioWithVotes.votingResult

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    scenario.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = scenario.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        scenario.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        scenario.dateOrPeriod,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Budget and Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(label = "Duration", value = "${scenario.duration} days")
                InfoChip(label = "Budget", value = "$${scenario.estimatedBudgetPerPerson}/person")
                InfoChip(label = "Participants", value = "${scenario.estimatedParticipants}")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Voting Results
            VotingResultsSection(result = result)

            Spacer(modifier = Modifier.height(12.dp))

            // Voting Buttons
            if (scenario.status == ScenarioStatus.PROPOSED) {
                VotingButtons(
                    currentVote = userVote,
                    onVote = onVote
                )
            } else {
                Text(
                    "This scenario is ${scenario.status.name.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

/**
 * Status badge showing scenario status
 */
@Composable
fun StatusBadge(status: ScenarioStatus) {
    val color = when (status) {
        ScenarioStatus.PROPOSED -> MaterialTheme.colorScheme.primary
        ScenarioStatus.SELECTED -> MaterialTheme.colorScheme.tertiary
        ScenarioStatus.REJECTED -> MaterialTheme.colorScheme.error
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            status.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

/**
 * Info chip for displaying key-value pairs
 */
@Composable
fun InfoChip(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Voting results visualization
 */
@Composable
fun VotingResultsSection(result: ScenarioVotingResult) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Score: ${result.score}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    result.score > 0 -> MaterialTheme.colorScheme.tertiary
                    result.score < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                "${result.totalVotes} votes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (result.totalVotes > 0) {
            Spacer(modifier = Modifier.height(8.dp))

            // Vote breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoteBreakdownChip(
                    label = "Prefer",
                    count = result.preferCount,
                    percentage = result.preferPercentage,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                VoteBreakdownChip(
                    label = "Neutral",
                    count = result.neutralCount,
                    percentage = result.neutralPercentage,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                VoteBreakdownChip(
                    label = "Against",
                    count = result.againstCount,
                    percentage = result.againstPercentage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visual progress bar
            LinearProgressIndicator(
                progress = (result.preferPercentage / 100.0).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/**
 * Vote breakdown chip
 */
@Composable
fun VoteBreakdownChip(
    label: String,
    count: Int,
    percentage: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Text(
                "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                "${percentage.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

/**
 * Voting buttons
 */
@Composable
fun VotingButtons(
    currentVote: ScenarioVoteType?,
    onVote: (ScenarioVoteType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VoteButton(
            label = "Prefer",
            voteType = ScenarioVoteType.PREFER,
            isSelected = currentVote == ScenarioVoteType.PREFER,
            color = MaterialTheme.colorScheme.tertiary,
            onClick = { onVote(ScenarioVoteType.PREFER) },
            modifier = Modifier.weight(1f)
        )
        VoteButton(
            label = "Neutral",
            voteType = ScenarioVoteType.NEUTRAL,
            isSelected = currentVote == ScenarioVoteType.NEUTRAL,
            color = MaterialTheme.colorScheme.primary,
            onClick = { onVote(ScenarioVoteType.NEUTRAL) },
            modifier = Modifier.weight(1f)
        )
        VoteButton(
            label = "Against",
            voteType = ScenarioVoteType.AGAINST,
            isSelected = currentVote == ScenarioVoteType.AGAINST,
            color = MaterialTheme.colorScheme.error,
            onClick = { onVote(ScenarioVoteType.AGAINST) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual vote button
 */
@Composable
fun VoteButton(
    label: String,
    voteType: ScenarioVoteType,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = if (isSelected) {
            androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = color
            )
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
