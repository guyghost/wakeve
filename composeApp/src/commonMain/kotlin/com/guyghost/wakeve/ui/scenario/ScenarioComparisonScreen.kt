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
import com.guyghost.wakeve.models.EventStatus
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
    eventStatus: EventStatus? = null,
    isParticipantConfirmed: Boolean? = null,
    onVote: (String) -> Unit = {},
    onSelectWinner: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToMeetings: (String) -> Unit = {},
    onNavigateToTransport: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedWinnerId by remember { mutableStateOf<String?>(null) }
    var votingForId by remember { mutableStateOf<String?>(null) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.FRANCE) }
    val canAccessDetails = isOrganizer || isParticipantConfirmed == true
    val canVote = canAccessDetails
    val canSelectFinal = isOrganizer && eventStatus == EventStatus.COMPARING
    val canNavigateToMeetings = eventStatus == EventStatus.ORGANIZING ||
        eventStatus == EventStatus.FINALIZED
    val canNavigateToTransport = eventStatus == EventStatus.ORGANIZING ||
        eventStatus == EventStatus.FINALIZED
    val openMeetings: () -> Unit = { onNavigateToMeetings(eventId) }
    val openTransport: () -> Unit = { onNavigateToTransport(eventId) }

    // Find winning scenario based on score
    val winningScenario = scenarios.maxByOrNull { it.votingResult.score }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = scenarioComparisonScreenTitle(),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = scenarioComparisonBackContentDescription()
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
                        text = scenarioComparisonEmptyTitle(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = scenarioComparisonEmptyMessage(),
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
                item {
                    ComparisonWorkflowCard(
                        eventStatus = eventStatus,
                        isAccessLocked = !canAccessDetails
                    )
                }

                if (!canAccessDetails) {
                    item {
                        LockedComparisonAccessCard()
                    }
                } else {
                    // Header with winner highlight
                    winningScenario?.let { winner ->
                        item {
                            WinnerHighlightCard(
                                scenarioName = winner.scenario.name,
                                score = winner.votingResult.score,
                                isOrganizer = canSelectFinal,
                                canViewMeetings = canNavigateToMeetings,
                                onSelectAsWinner = {
                                    selectedWinnerId = winner.scenario.id
                                    onSelectWinner(winner.scenario.id)
                                },
                                onViewMeetings = openMeetings
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
                            canVote = canVote,
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

                        if (isOrganizer && canNavigateToMeetings) {
                            OutlinedButton(
                                onClick = openMeetings,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(scenarioComparisonViewMeetingsLabel())
                            }
                        }
                        if (isOrganizer && canNavigateToTransport) {
                            Button(
                                onClick = openTransport,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(scenarioComparisonOpenTransportLabel())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockedComparisonAccessCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = scenarioComparisonLockedAccessMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
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
    canViewMeetings: Boolean,
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
                text = scenarioComparisonCurrentLeaderLabel(),
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
                text = scenarioComparisonScoreLabel(score),
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
                    Text(scenarioComparisonSelectFinalLabel())
                }

                if (canViewMeetings) {
                    OutlinedButton(
                        onClick = onViewMeetings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(scenarioComparisonViewMeetingsLabel())
                    }
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
    canVote: Boolean,
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
                                text = scenarioComparisonSelectedStatusLabel(),
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
                                text = scenarioComparisonLeaderBadgeLabel(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = scenarioComparisonLocationLabel(scenario.location),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                    text = scenarioComparisonPointsLabel(votingResult.score),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickStatChip(
                    icon = Icons.Outlined.ConfirmationNumber,
                    text = scenarioComparisonBudgetPerPersonLabel(
                        currencyFormat.format(scenario.estimatedBudgetPerPerson)
                    ),
                    modifier = Modifier.weight(1f)
                )
                QuickStatChip(
                    icon = Icons.Outlined.People,
                    text = scenarioComparisonPeopleLabel(scenario.estimatedParticipants),
                    modifier = Modifier.weight(1f)
                )
            }

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
                    label = scenarioComparisonPreferVoteLabel(),
                    count = votingResult.preferCount,
                    percentage = votingResult.preferPercentage,
                    color = MaterialTheme.colorScheme.primary
                )
                VotePill(
                    label = scenarioComparisonNeutralVoteLabel(),
                    count = votingResult.neutralCount,
                    percentage = votingResult.neutralPercentage,
                    color = MaterialTheme.colorScheme.tertiary
                )
                VotePill(
                    label = scenarioComparisonAgainstVoteLabel(),
                    count = votingResult.againstCount,
                    percentage = votingResult.againstPercentage,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Voting button
            if (scenario.status != ScenarioStatus.SELECTED && canVote) {
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
                            Text(scenarioComparisonVoteForThisLabel())
                        }
                    }
                }
            } else if (!canVote) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = scenarioComparisonVoteLockedMessage(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ComparisonWorkflowCard(
    eventStatus: EventStatus?,
    isAccessLocked: Boolean,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = scenarioComparisonWorkflowStatusLabel(eventStatus),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isAccessLocked) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
            Text(
                text = if (isAccessLocked) {
                    scenarioComparisonWorkflowLockedMessage()
                } else {
                    scenarioComparisonWorkflowAvailableMessage()
                },
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

internal fun scenarioComparisonScreenTitle(): String = "Comparer les scenarios"

internal fun scenarioComparisonBackContentDescription(): String = "Retour"

internal fun scenarioComparisonEmptyTitle(): String = "Aucun scenario a comparer"

internal fun scenarioComparisonEmptyMessage(): String =
    "Creez au moins deux scenarios pour comparer les options du groupe."

internal fun scenarioComparisonViewMeetingsLabel(): String = "Voir les reunions"

internal fun scenarioComparisonOpenTransportLabel(): String = "Ouvrir le transport"

internal fun scenarioComparisonLockedAccessMessage(): String =
    "Confirmez votre presence pour comparer les details des scenarios."

internal fun scenarioComparisonCurrentLeaderLabel(): String = "Option en tete"

internal fun scenarioComparisonScoreLabel(score: Int): String = "Score: $score"

internal fun scenarioComparisonSelectFinalLabel(): String = "Retenir ce scenario"

internal fun scenarioComparisonSelectedStatusLabel(): String = "Retenu"

internal fun scenarioComparisonLeaderBadgeLabel(): String = "En tete"

internal fun scenarioComparisonLocationLabel(location: String): String =
    "Destination / logement: $location"

internal fun scenarioComparisonPointsLabel(score: Int): String = "$score pts"

internal fun scenarioComparisonBudgetPerPersonLabel(formattedAmount: String): String =
    "$formattedAmount / personne"

internal fun scenarioComparisonPeopleLabel(count: Int): String = "$count personnes"

internal fun scenarioComparisonPreferVoteLabel(): String = "Pour"

internal fun scenarioComparisonNeutralVoteLabel(): String = "Neutre"

internal fun scenarioComparisonAgainstVoteLabel(): String = "Contre"

internal fun scenarioComparisonVoteForThisLabel(): String = "Voter pour cette option"

internal fun scenarioComparisonVoteLockedMessage(): String =
    "Confirmez votre presence pour voter et ouvrir les details."

internal fun scenarioComparisonWorkflowStatusLabel(eventStatus: EventStatus?): String =
    "Statut: ${eventStatus?.name ?: "INCONNU"}"

internal fun scenarioComparisonWorkflowLockedMessage(): String =
    "Les details des scenarios sont disponibles apres confirmation de presence."

internal fun scenarioComparisonWorkflowAvailableMessage(): String =
    "Comparez destination, logement, periode, budget, duree et adequation au groupe."
