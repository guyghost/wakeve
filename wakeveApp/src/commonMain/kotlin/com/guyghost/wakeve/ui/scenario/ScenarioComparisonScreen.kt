package com.guyghost.wakeve.ui.scenario

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVotingResult
import com.guyghost.wakeve.models.ScenarioWithVotes
import java.text.NumberFormat
import java.util.Locale

/**
 * Scenario Comparison Screen for Android (Jetpack Compose)
 *
 * Displays scenarios side-by-side for comparison with:
 * - All scenarios for an event displayed as cards
 * - Key information: name, date, location, budget, participants
 * - Voting buttons for each scenario
 * - Highlighted differences between scenarios
 * - Winner selection for organizer
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun ScenarioComparisonScreen(
 *     scenarios: List<ScenarioWithVotes>,
 *     eventId: String,
 *     isOrganizer: Boolean = false,
 *     onVote: (scenarioId: String) -> Unit,
 *     onSelectWinner: (scenarioId: String) -> Unit,
 *     onNavigateBack: () -> Unit,
 *     onNavigateToMeetings: (String) -> Unit
 * ) {
 *     ScenarioComparisonContent(
 *         scenarios = scenarios,
 *         onVote = onVote,
 *         onSelectWinner = onSelectWinner,
 *         onNavigateBack = onNavigateBack,
 *         onNavigateToMeetings = { onNavigateToMeetings(eventId) }
 *     )
 * }
 * ```
 *
 * @param scenarios List of scenarios to compare with their votes
 * @param eventId The event ID for navigation
 * @param isOrganizer Whether the current user is the organizer
 * @param onVote Callback when user votes for a scenario
 * @param onSelectWinner Callback when organizer selects a winning scenario
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToMeetings Callback to navigate to meetings screen
 * @param modifier Modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioComparisonScreen(
    scenarios: List<ScenarioWithVotes>,
    eventId: String,
    isOrganizer: Boolean = false,
    onVote: (String) -> Unit = {},
    onSelectWinner: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToMeetings: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedWinnerId by remember { mutableStateOf<String?>(null) }
    var votingForId by remember { mutableStateOf<String?>(null) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.FRANCE) }

    // Find winning scenario based on score
    val winningScenario = scenarios.maxByOrNull { it.votingResult.score }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compare Scenarios",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (scenarios.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No scenarios to compare",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Create scenarios to start comparing options",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with winner highlight
                winningScenario?.let { winner ->
                    item {
                        WinnerHighlightCard(
                            scenarioName = winner.scenario.name,
                            score = winner.votingResult.score,
                            isOrganizer = isOrganizer,
                            onSelectAsWinner = {
                                selectedWinnerId = winner.scenario.id
                                onSelectWinner(winner.scenario.id)
                            },
                            onViewMeetings = { onNavigateToMeetings(eventId) }
                        )
                    }
                }

                // Scenario comparison cards
                items(
                    items = scenarios.sortedByDescending { it.votingResult.score },
                    key = { it.scenario.id }
                ) { scenarioWithVotes ->
                    ComparisonCard(
                        scenarioWithVotes = scenarioWithVotes,
                        isLeading = scenarioWithVotes.scenario.id == winningScenario?.scenario?.id,
                        isSelectedWinner = selectedWinnerId == scenarioWithVotes.scenario.id,
                        isVoting = votingForId == scenarioWithVotes.scenario.id,
                        onVote = {
                            votingForId = scenarioWithVotes.scenario.id
                            onVote(scenarioWithVotes.scenario.id)
                        },
                        onVoteComplete = { votingForId = null },
                        currencyFormat = currencyFormat
                    )
                }

                // Bottom actions
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isOrganizer) {
                        OutlinedButton(
                            onClick = { onNavigateToMeetings(eventId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Skip Comparison - Go to Meetings")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Highlight card showing the current leading scenario
 */
@Composable
private fun WinnerHighlightCard(
    scenarioName: String,
    score: Int,
    isOrganizer: Boolean,
    onSelectAsWinner: () -> Unit,
    onViewMeetings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = "Current Leader",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = scenarioName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Score: $score",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            if (isOrganizer) {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSelectAsWinner,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select This Scenario")
                }

                OutlinedButton(
                    onClick = onViewMeetings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Meetings")
                }
            }
        }
    }
}

/**
 * Individual comparison card for a scenario
 */
@Composable
private fun ComparisonCard(
    scenarioWithVotes: ScenarioWithVotes,
    isLeading: Boolean,
    isSelectedWinner: Boolean,
    isVoting: Boolean,
    onVote: () -> Unit,
    onVoteComplete: () -> Unit,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val scenario = scenarioWithVotes.scenario
    val votingResult = scenarioWithVotes.votingResult

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelectedWinner -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                isLeading -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLeading) 4.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with leader badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scenario.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (scenario.status == ScenarioStatus.SELECTED) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Selected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (isLeading) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Leader",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Quick stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickStatChip(
                    icon = Icons.Outlined.CalendarToday,
                    text = scenario.dateOrPeriod,
                    modifier = Modifier.weight(1f)
                )
                QuickStatChip(
                    icon = Icons.Default.Star,
                    text = "${votingResult.score} pts",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickStatChip(
                    icon = Icons.Outlined.ConfirmationNumber,
                    text = "${currencyFormat.format(scenario.estimatedBudgetPerPerson)}/person",
                    modifier = Modifier.weight(1f)
                )
                QuickStatChip(
                    icon = Icons.Outlined.People,
                    text = "${scenario.estimatedParticipants} people",
                    modifier = Modifier.weight(1f)
                )
            }

            // Location
            Text(
                text = "📍 ${scenario.location}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Description
            if (scenario.description.isNotBlank()) {
                Text(
                    text = scenario.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Voting breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VotePill(
                    label = "Prefer",
                    count = votingResult.preferCount,
                    percentage = votingResult.preferPercentage,
                    color = MaterialTheme.colorScheme.primary
                )
                VotePill(
                    label = "Neutral",
                    count = votingResult.neutralCount,
                    percentage = votingResult.neutralPercentage,
                    color = MaterialTheme.colorScheme.tertiary
                )
                VotePill(
                    label = "Against",
                    count = votingResult.againstCount,
                    percentage = votingResult.againstPercentage,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Voting button
            if (scenario.status != ScenarioStatus.SELECTED) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = isVoting,
                        onClick = {
                            onVote()
                        },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isVoting
                    ) {
                        if (isVoting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("👍 Vote for this")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick stat chip for comparison card
 */
@Composable
private fun QuickStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Pill showing vote count and percentage
 */
@Composable
private fun VotePill(
    label: String,
    count: Int,
    percentage: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "${String.format("%.0f", percentage)}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Placeholder for scenario comparison when loading
 */
@Composable
fun ScenarioComparisonPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
