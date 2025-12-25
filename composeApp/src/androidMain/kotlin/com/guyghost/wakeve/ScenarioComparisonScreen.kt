package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.ScenarioLogic
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.ScenarioWithVotes
import kotlin.math.max

/**
 * State for Scenario Comparison Screen
 */
data class ScenarioComparisonState(
    val eventId: String = "",
    val scenarios: List<ScenarioWithVotes> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val errorMessage: String = "",
    val bestScenarioId: String? = null
)

/**
 * Scenario Comparison Screen
 * 
 * Side-by-side comparison of all scenarios for an event.
 * Shows all key metrics in a table format for easy comparison.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ScenarioComparisonScreen(
    event: Event,
    repository: ScenarioRepository,
    onBack: () -> Unit
) {
    var state by remember { mutableStateOf(ScenarioComparisonState(eventId = event.id)) }

    // Load scenarios on mount
    LaunchedEffect(event.id) {
        state = state.copy(isLoading = true, isError = false)
        try {
            val scenariosWithVotes = repository.getScenariosWithVotes(event.id)
            
            // Calculate best scenario
            val allVotes = scenariosWithVotes.flatMap { it.votes }
            val rankedScenarios = ScenarioLogic.rankScenariosByScore(
                scenariosWithVotes.map { it.scenario },
                allVotes
            )
            val bestScenarioId = rankedScenarios.firstOrNull()?.scenario?.id
            
            state = state.copy(
                scenarios = scenariosWithVotes,
                bestScenarioId = bestScenarioId,
                isLoading = false
            )
        } catch (e: Exception) {
            state = state.copy(
                isLoading = false,
                isError = true,
                errorMessage = e.message ?: "Failed to load scenarios"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare Scenarios") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(paddingValues)
                .safeContentPadding()
        ) {
            // Loading State
            if (state.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
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
            if (state.isError) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            state.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                return@Scaffold
            }

            // Empty State
            if (state.scenarios.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No scenarios to compare",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Scaffold
            }

            // Header
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Event: ${event.title}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${state.scenarios.size} scenarios to compare",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Comparison Table
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                ComparisonTableImpl(
                    scenarios = state.scenarios,
                    bestScenarioId = state.bestScenarioId
                )
            }
        }
    }
}

/**
 * Header cell for each scenario
 */
@Composable
fun ComparisonHeaderCell(
    name: String,
    isBest: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBest) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isBest) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Best scenario",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = if (isBest) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Redefine ComparisonRow to pass scenarios
 */
@Composable
fun ComparisonRowWithScenarios(
    label: String,
    labelWidth: androidx.compose.ui.unit.Dp,
    columnWidth: androidx.compose.ui.unit.Dp,
    scenarios: List<ScenarioWithVotes>,
    content: @Composable (ScenarioWithVotes) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.width(labelWidth)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(12.dp)
            )
        }

        // Cells
        scenarios.forEach { swv ->
            Card(
                modifier = Modifier
                    .width(columnWidth)
                    .padding(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    content(swv)
                }
            }
        }
    }
}

/**
 * Highlighted comparison value
 */
@Composable
fun ComparisonValue(
    value: String,
    isHighlighted: Boolean,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
        color = if (isHighlighted) MaterialTheme.colorScheme.primary else color,
        textAlign = TextAlign.Center
    )
}

/**
 * Vote count with percentage
 */
@Composable
fun VoteCount(
    count: Int,
    percentage: Double,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

// Implementation of comparison table
@Composable
private fun ComparisonTableImpl(
    scenarios: List<ScenarioWithVotes>,
    bestScenarioId: String?
) {
    val columnWidth = 180.dp
    val labelWidth = 140.dp

    Column {
        // Header Row
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(labelWidth))
            scenarios.forEach { swv ->
                ComparisonHeaderCell(
                    name = swv.scenario.name,
                    isBest = swv.scenario.id == bestScenarioId,
                    modifier = Modifier.width(columnWidth)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // All comparison rows
        ComparisonRowWithScenarios("Status", labelWidth, columnWidth, scenarios) { swv ->
            StatusBadge(status = swv.scenario.status)
        }
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        ComparisonRowWithScenarios("Location", labelWidth, columnWidth, scenarios) { swv ->
            Text(swv.scenario.location, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        
        ComparisonRowWithScenarios("Date/Period", labelWidth, columnWidth, scenarios) { swv ->
            Text(swv.scenario.dateOrPeriod, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        
        ComparisonRowWithScenarios("Duration", labelWidth, columnWidth, scenarios) { swv ->
            ComparisonValue("${swv.scenario.duration} days", swv.scenario.duration == scenarios.minOf { it.scenario.duration })
        }
        
        ComparisonRowWithScenarios("Participants", labelWidth, columnWidth, scenarios) { swv ->
            Text("${swv.scenario.estimatedParticipants}", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        
        ComparisonRowWithScenarios("Budget/Person", labelWidth, columnWidth, scenarios) { swv ->
            ComparisonValue("$${swv.scenario.estimatedBudgetPerPerson}", swv.scenario.estimatedBudgetPerPerson == scenarios.minOf { it.scenario.estimatedBudgetPerPerson })
        }
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        ComparisonRowWithScenarios("Score", labelWidth, columnWidth, scenarios) { swv ->
            ComparisonValue(
                "${swv.votingResult.score}",
                swv.votingResult.score == scenarios.maxOf { it.votingResult.score },
                when { swv.votingResult.score > 0 -> MaterialTheme.colorScheme.tertiary; swv.votingResult.score < 0 -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.onSurface }
            )
        }
        
        ComparisonRowWithScenarios("Total Votes", labelWidth, columnWidth, scenarios) { swv ->
            Text("${swv.votingResult.totalVotes}", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        
        ComparisonRowWithScenarios("Prefer", labelWidth, columnWidth, scenarios) { swv ->
            VoteCount(swv.votingResult.preferCount, swv.votingResult.preferPercentage, MaterialTheme.colorScheme.tertiary)
        }
        
        ComparisonRowWithScenarios("Neutral", labelWidth, columnWidth, scenarios) { swv ->
            VoteCount(swv.votingResult.neutralCount, swv.votingResult.neutralPercentage, MaterialTheme.colorScheme.primary)
        }
        
        ComparisonRowWithScenarios("Against", labelWidth, columnWidth, scenarios) { swv ->
            VoteCount(swv.votingResult.againstCount, swv.votingResult.againstPercentage, MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
