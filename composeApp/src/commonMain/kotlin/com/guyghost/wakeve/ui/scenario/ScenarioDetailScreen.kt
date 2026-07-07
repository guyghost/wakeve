package com.guyghost.wakeve.ui.scenario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVotingResult
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract
import com.guyghost.wakeve.viewmodel.ScenarioManagementViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Scenario Detail Screen for Android (Jetpack Compose)
 *
 * Displays detailed information about a single scenario with:
 * - Scenario name, description, and status
 * - Date/period and location information
 * - Duration and participant estimates
 * - Budget breakdown and total cost
 * - Voting results and participant breakdown
 * - Action buttons (select as final, navigate to meetings)
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun ScenarioDetailScreen(
 *     viewModel: ScenarioManagementViewModel = koinViewModel(),
 *     eventId: String,
 *     scenarioId: String,
 *     isOrganizer: Boolean = false,
 *     onNavigateBack: () -> Unit,
 *     onNavigateToMeetings: (String) -> Unit
 * ) {
 *     LaunchedEffect(scenarioId) {
 *         viewModel.dispatch(ScenarioManagementContract.Intent.SelectScenario(scenarioId))
 *     }
 *
 *     val state by viewModel.state.collectAsState()
 *     val selectedScenario = state.selectedScenario
 *
 *     selectedScenario?.let { scenario ->
 *         ScenarioDetailContent(
 *             scenario = scenario,
 *             votingResult = state.votingResults[scenarioId],
 *             votes = state.scenarios.find { it.scenario.id == scenarioId }?.votes ?: emptyList(),
 *             onSelectAsFinal = {
 *                 viewModel.dispatch(ScenarioManagementContract.Intent.SelectScenarioAsFinal(scenarioId))
 *             },
 *             onNavigateToMeetings = { onNavigateToMeetings(eventId) }
 *         )
 *     }
 * }
 * ```
 *
 * @param scenario The scenario to display
 * @param votingResult The voting results for this scenario (optional)
 * @param votes List of individual votes (optional)
 * @param onSelectAsFinal Callback when organizer selects this scenario as final
 * @param onNavigateToMeetings Callback to navigate to meetings screen
 * @param onNavigateBack Callback to navigate back
 * @param isOrganizer Whether the current user is the organizer
 * @param modifier Modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioDetailScreen(
    scenario: Scenario,
    votingResult: ScenarioVotingResult? = null,
    votes: List<ScenarioVote> = emptyList(),
    onSelectAsFinal: () -> Unit = {},
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToTransport: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    isOrganizer: Boolean = false,
    isParticipantConfirmed: Boolean? = null,
    eventStatus: EventStatus? = null,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(false) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.FRANCE) }
    val canAccessDetails = isOrganizer || isParticipantConfirmed == true
    val canNavigateToMeetings = eventStatus == EventStatus.ORGANIZING ||
        eventStatus == EventStatus.FINALIZED
    val canNavigateToTransport = eventStatus == EventStatus.ORGANIZING ||
        eventStatus == EventStatus.FINALIZED
    val openMeetings: () -> Unit = { onNavigateToMeetings() }
    val openTransport: () -> Unit = { onNavigateToTransport() }

    if (!canAccessDetails) {
        ScenarioDetailLockedScreen(
            onNavigateBack = onNavigateBack,
            modifier = modifier
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = scenarioDetailScreenTitle(),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = scenarioDetailBackContentDescription()
                        )
                    }
                },
                actions = {
                    if (scenario.status == ScenarioStatus.SELECTED) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = scenarioDetailSelectedStatusLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card with Scenario Name and Status
            ScenarioHeaderCard(
                name = scenario.name,
                description = scenario.description,
                status = scenario.status
            )

            // Date and Location Information
            ScenarioInfoCard(
                icon = Icons.Outlined.CalendarToday,
                label = scenarioDetailDateLabel(),
                value = scenario.dateOrPeriod
            )

            ScenarioInfoCard(
                icon = Icons.Default.LocationOn,
                label = scenarioDetailLocationLabel(),
                value = scenario.location
            )

            // Duration and Participants
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScenarioInfoCard(
                    icon = Icons.Default.Schedule,
                    label = scenarioDetailDurationLabel(),
                    value = scenarioDetailDurationValue(scenario.duration),
                    modifier = Modifier.weight(1f)
                )
                ScenarioInfoCard(
                    icon = Icons.Default.People,
                    label = scenarioDetailParticipantsLabel(),
                    value = scenarioDetailParticipantsValue(scenario.estimatedParticipants),
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Budget Section
            Text(
                text = scenarioDetailBudgetTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            BudgetCard(
                budgetPerPerson = scenario.estimatedBudgetPerPerson,
                estimatedParticipants = scenario.estimatedParticipants
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Voting Results
            if (votingResult != null && votingResult.totalVotes > 0) {
                Text(
                    text = scenarioDetailVotingResultsTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                VotingResultsCard(
                    votingResult = votingResult,
                    totalParticipants = scenario.estimatedParticipants
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            if (isOrganizer && scenario.status != ScenarioStatus.SELECTED) {
                Button(
                    onClick = {
                        isLoading = true
                        onSelectAsFinal()
                        isLoading = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(scenarioDetailSelectFinalLabel())
                    }
                }

                if (canNavigateToMeetings) {
                    OutlinedButton(
                        onClick = openMeetings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(scenarioDetailViewMeetingsLabel())
                    }
                }
                if (canNavigateToTransport) {
                    OutlinedButton(
                        onClick = openTransport,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(scenarioDetailOpenTransportLabel())
                    }
                }
            } else if (scenario.status == ScenarioStatus.SELECTED && canNavigateToMeetings) {
                Button(
                    onClick = openMeetings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(scenarioDetailViewMeetingsLabel())
                }
                if (canNavigateToTransport) {
                    OutlinedButton(
                        onClick = openTransport,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(scenarioDetailOpenTransportLabel())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScenarioDetailLockedScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = scenarioDetailScreenTitle(),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = scenarioDetailBackContentDescription()
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = scenarioDetailAccessLockedTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = scenarioDetailAccessLockedMessage(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/**
 * Header card showing scenario name, description, and status
 */
@Composable
private fun ScenarioHeaderCard(
    name: String,
    description: String,
    status: ScenarioStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )

                if (status == ScenarioStatus.SELECTED) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = scenarioDetailSelectedStatusLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Info card for displaying single pieces of scenario information
 */
@Composable
private fun ScenarioInfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Budget card showing per-person cost and total
 */
@Composable
private fun BudgetCard(
    budgetPerPerson: Double,
    estimatedParticipants: Int,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.FRANCE) }
    val totalBudget = budgetPerPerson * estimatedParticipants

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = scenarioDetailBudgetPerPersonLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(budgetPerPerson),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = scenarioDetailBudgetTotalLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(totalBudget),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = scenarioDetailBudgetParticipantsLabel(estimatedParticipants),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Voting results card showing vote breakdown
 */
@Composable
private fun VotingResultsCard(
    votingResult: ScenarioVotingResult,
    totalParticipants: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Score header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scenarioDetailScoreTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${votingResult.score}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        votingResult.score > 0 -> MaterialTheme.colorScheme.primary
                        votingResult.score < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // Vote breakdown
            VoteProgressRow(
                label = scenarioDetailPreferVoteLabel(),
                count = votingResult.preferCount,
                percentage = votingResult.preferPercentage,
                color = MaterialTheme.colorScheme.primary
            )

            VoteProgressRow(
                label = scenarioDetailNeutralVoteLabel(),
                count = votingResult.neutralCount,
                percentage = votingResult.neutralPercentage,
                color = MaterialTheme.colorScheme.tertiary
            )

            VoteProgressRow(
                label = scenarioDetailAgainstVoteLabel(),
                count = votingResult.againstCount,
                percentage = votingResult.againstPercentage,
                color = MaterialTheme.colorScheme.error
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = scenarioDetailTotalVotesLabel(votingResult.totalVotes, totalParticipants),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Progress row showing vote count and percentage
 */
@Composable
private fun VoteProgressRow(
    label: String,
    count: Int,
    percentage: Double,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$count (${String.format("%.0f", percentage)}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage.toFloat() / 100f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Placeholder for scenario detail when loading or error
 */
@Composable
fun ScenarioDetailPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

internal fun scenarioDetailScreenTitle(): String = "Details du scenario"

internal fun scenarioDetailBackContentDescription(): String = "Retour"

internal fun scenarioDetailSelectedStatusLabel(): String = "Retenu"

internal fun scenarioDetailDateLabel(): String = "Date / periode"

internal fun scenarioDetailLocationLabel(): String = "Destination / logement"

internal fun scenarioDetailDurationLabel(): String = "Duree"

internal fun scenarioDetailDurationValue(days: Int): String =
    if (days <= 1) "$days jour" else "$days jours"

internal fun scenarioDetailParticipantsLabel(): String = "Participants"

internal fun scenarioDetailParticipantsValue(count: Int): String = "$count personnes"

internal fun scenarioDetailBudgetTitle(): String = "Budget"

internal fun scenarioDetailVotingResultsTitle(): String = "Resultats des votes"

internal fun scenarioDetailSelectFinalLabel(): String = "Retenir ce scenario"

internal fun scenarioDetailViewMeetingsLabel(): String = "Voir les reunions"

internal fun scenarioDetailOpenTransportLabel(): String = "Ouvrir le transport"

internal fun scenarioDetailAccessLockedTitle(): String = "Acces au scenario verrouille"

internal fun scenarioDetailAccessLockedMessage(): String =
    "Confirmez votre presence pour voir la destination, le logement et le budget."

internal fun scenarioDetailBudgetPerPersonLabel(): String = "Par personne"

internal fun scenarioDetailBudgetTotalLabel(): String = "Estimation totale"

internal fun scenarioDetailBudgetParticipantsLabel(estimatedParticipants: Int): String =
    "Pour $estimatedParticipants participants"

internal fun scenarioDetailScoreTitle(): String = "Score"

internal fun scenarioDetailPreferVoteLabel(): String = "Pour"

internal fun scenarioDetailNeutralVoteLabel(): String = "Neutre"

internal fun scenarioDetailAgainstVoteLabel(): String = "Contre"

internal fun scenarioDetailTotalVotesLabel(totalVotes: Int, totalParticipants: Int): String =
    "Votes: $totalVotes / $totalParticipants participants"
