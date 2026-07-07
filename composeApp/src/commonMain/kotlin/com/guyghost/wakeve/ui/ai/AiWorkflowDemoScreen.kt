package com.guyghost.wakeve.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.ai.EventAiSummary
import com.guyghost.wakeve.ai.GeneratedOrganizerMessage
import com.guyghost.wakeve.ai.OrganizerMessageType
import com.guyghost.wakeve.viewmodel.AiWorkflowDemoUiState
import com.guyghost.wakeve.viewmodel.AiWorkflowDemoViewModel

@Composable
fun AiWorkflowDemoScreen(
    viewModel: AiWorkflowDemoViewModel,
    onClose: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    AiWorkflowDemoContent(
        state = state,
        onClose = onClose,
        onGenerateSummary = viewModel::generateSummary,
        onMessageTypeSelected = viewModel::selectMessageType,
        onGenerateMessage = viewModel::generateMessage,
        onStartAgent = viewModel::startPlanningAgent,
        onResolveAgentConfirmation = viewModel::resolveAgentConfirmation
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWorkflowDemoContent(
    state: AiWorkflowDemoUiState,
    onClose: () -> Unit,
    onGenerateSummary: () -> Unit,
    onMessageTypeSelected: (OrganizerMessageType) -> Unit,
    onGenerateMessage: () -> Unit,
    onStartAgent: () -> Unit,
    onResolveAgentConfirmation: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI workflows") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EventContextCard(state)
            SummaryCard(
                state = state,
                onGenerateSummary = onGenerateSummary
            )
            MessageCard(
                state = state,
                onMessageTypeSelected = onMessageTypeSelected,
                onGenerateMessage = onGenerateMessage
            )
            PlanningAgentCard(
                state = state,
                onStartAgent = onStartAgent,
                onResolveAgentConfirmation = onResolveAgentConfirmation
            )
        }
    }
}

@Composable
private fun EventContextCard(state: AiWorkflowDemoUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = state.context.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = listOfNotNull(
                    state.context.destination,
                    state.context.dates.joinToString(" to ").ifBlank { null },
                    state.context.participantCount?.let { "$it participants" }
                ).joinToString(" - "),
                style = MaterialTheme.typography.bodyMedium
            )
            AssistChip(
                onClick = {},
                label = { Text("Budget ${state.context.budget?.amount?.toInt()} ${state.context.budget?.currencyCode}") },
                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun SummaryCard(
    state: AiWorkflowDemoUiState,
    onGenerateSummary: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Event summary", state.summaryRouteLabel)
            Button(
                onClick = onGenerateSummary,
                enabled = !state.isGeneratingSummary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Text(
                    text = if (state.isGeneratingSummary) "Generating" else "Generate summary",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            LoadingOrError(state.isGeneratingSummary, state.summaryError)
            state.summary?.let { EventSummaryResult(it) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageCard(
    state: AiWorkflowDemoUiState,
    onMessageTypeSelected: (OrganizerMessageType) -> Unit,
    onGenerateMessage: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Organizer message", state.messageRouteLabel)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrganizerMessageType.entries.forEach { type ->
                    FilterChip(
                        selected = state.selectedMessageType == type,
                        onClick = { onMessageTypeSelected(type) },
                        label = { Text(type.label()) }
                    )
                }
            }
            Button(
                onClick = onGenerateMessage,
                enabled = !state.isGeneratingMessage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Text(
                    text = if (state.isGeneratingMessage) "Generating" else "Generate message",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            LoadingOrError(state.isGeneratingMessage, state.messageError)
            state.generatedMessage?.let { GeneratedMessageResult(it) }
        }
    }
}

@Composable
private fun PlanningAgentCard(
    state: AiWorkflowDemoUiState,
    onStartAgent: () -> Unit,
    onResolveAgentConfirmation: (String, Boolean) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Planning agent", state.agentSession?.status?.name ?: "Idle")
            Button(
                onClick = onStartAgent,
                enabled = !state.isAgentRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text(
                    text = if (state.isAgentRunning) "Running" else "Start agent demo",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            LoadingOrError(state.isAgentRunning, state.agentError)
            state.agentEvents.forEach { item ->
                AgentEventItem(
                    item = item,
                    onResolveAgentConfirmation = onResolveAgentConfirmation
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    routeLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() }
        )
        AssistChip(onClick = {}, label = { Text(routeLabel) })
    }
}

@Composable
private fun LoadingOrError(
    isLoading: Boolean,
    errorMessage: String?
) {
    if (isLoading) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(18.dp),
                strokeWidth = 2.dp
            )
            Text("Working", style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (errorMessage != null) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EventSummaryResult(summary: EventAiSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(summary.shortSummary, style = MaterialTheme.typography.bodyLarge)
        BulletSection("Preparation", summary.preparationAdvice)
        BulletSection("Packing", summary.packingChecklist)
        BulletSection("Missing", summary.missingInformation)
    }
}

@Composable
private fun GeneratedMessageResult(message: GeneratedOrganizerMessage) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message.messageType.label(), style = MaterialTheme.typography.labelLarge)
        Text(message.body, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AgentEventItem(
    item: PlanningAgentEventUiItem,
    onResolveAgentConfirmation: (String, Boolean) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(item.title, style = MaterialTheme.typography.titleSmall)
            Text(item.body, style = MaterialTheme.typography.bodyMedium)
            if (item.requiresUserAction && item.requestId != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onResolveAgentConfirmation(item.requestId, true) }) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Text(item.primaryActionLabel ?: "Confirm", modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(onClick = { onResolveAgentConfirmation(item.requestId, false) }) {
                        Text(item.secondaryActionLabel ?: "Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
private fun BulletSection(
    title: String,
    items: List<String>
) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    if (items.isEmpty()) {
        Text("None", style = MaterialTheme.typography.bodyMedium)
    } else {
        items.forEach { item ->
            Text("- $item", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun OrganizerMessageType.label(): String = when (this) {
    OrganizerMessageType.INVITATION -> "Invitation"
    OrganizerMessageType.REMINDER -> "Reminder"
    OrganizerMessageType.RSVP_FOLLOW_UP -> "RSVP"
    OrganizerMessageType.BUDGET_REMINDER -> "Budget"
    OrganizerMessageType.LOGISTICS_UPDATE -> "Logistics"
    OrganizerMessageType.REVIEW_REQUEST -> "Review"
}
