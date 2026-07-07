package com.guyghost.wakeve.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.guyghost.wakeve.ai.EventPlanDraft
import com.guyghost.wakeve.ai.EventPlanMissingInformation
import com.guyghost.wakeve.ai.EventPlanningAiAvailability
import com.guyghost.wakeve.viewmodel.EventPlanningAssistantViewModel

@Composable
fun EventPlanningAssistantScreen(
    viewModel: EventPlanningAssistantViewModel,
    onClose: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    EventPlanningAssistantContent(
        prompt = state.prompt,
        availability = state.availability,
        isExtracting = state.isExtracting,
        draft = state.draft,
        errorMessage = state.errorMessage,
        onPromptChange = viewModel::updatePrompt,
        onExtract = { viewModel.extract(referenceYear = 2026) },
        onClearDraft = viewModel::clearDraft,
        onClose = onClose
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventPlanningAssistantContent(
    prompt: String,
    availability: EventPlanningAiAvailability,
    isExtracting: Boolean,
    draft: EventPlanDraft?,
    errorMessage: String?,
    onPromptChange: (String) -> Unit,
    onExtract: () -> Unit,
    onClearDraft: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI event draft") },
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
            Text(
                text = "Describe the event",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )

            AssistChip(
                onClick = {},
                label = { Text(availability.label()) },
                leadingIcon = {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                }
            )

            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text("Natural language description") },
                placeholder = {
                    Text("On part à Biarritz du 12 au 15 juillet, 8 personnes, budget 300€ par personne.")
                },
                isError = errorMessage != null
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = onExtract,
                enabled = !isExtracting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isExtracting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                }
                Text(if (isExtracting) "Extracting" else "Extract event plan")
            }

            if (isExtracting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (draft != null) {
                EventPlanDraftCard(
                    draft = draft,
                    onClearDraft = onClearDraft
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventPlanDraftCard(
    draft: EventPlanDraft,
    onClearDraft: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Extracted fields",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() }
                )
                IconButton(onClick = onClearDraft) {
                    Icon(Icons.Default.Close, contentDescription = "Clear draft")
                }
            }

            FieldRow("Destination", draft.destination)
            FieldRow("Start date", draft.startDate)
            FieldRow("End date", draft.endDate)
            FieldRow("Participants", draft.participantCount?.toString())
            FieldRow(
                "Budget/person",
                draft.budgetPerPerson?.let { "${it.amount.formatAmount()} ${it.currencyCode}" }
            )
            FieldRow("Event type", draft.eventType.displayName)
            FieldRow("Source", draft.source.name)

            if (draft.constraints.isNotEmpty()) {
                Text("Constraints", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    draft.constraints.forEach { constraint ->
                        AssistChip(onClick = {}, label = { Text(constraint) })
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Missing information", style = MaterialTheme.typography.titleSmall)
            if (draft.missingInformation.isEmpty()) {
                Text("Nothing required is missing.", style = MaterialTheme.typography.bodyMedium)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    draft.missingInformation.forEach { missing ->
                        AssistChip(onClick = {}, label = { Text(missing.label()) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value ?: "Missing",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun EventPlanningAiAvailability.label(): String = when (this) {
    EventPlanningAiAvailability.AVAILABLE -> "Gemini Nano available"
    EventPlanningAiAvailability.DOWNLOADABLE -> "Gemini Nano downloadable"
    EventPlanningAiAvailability.DOWNLOADING -> "Gemini Nano downloading"
    EventPlanningAiAvailability.UNAVAILABLE -> "Local fallback"
    EventPlanningAiAvailability.FALLBACK_ONLY -> "Rule-based fallback"
}

private fun EventPlanMissingInformation.label(): String = when (this) {
    EventPlanMissingInformation.DESTINATION -> "Destination"
    EventPlanMissingInformation.START_DATE -> "Start date"
    EventPlanMissingInformation.END_DATE -> "End date"
    EventPlanMissingInformation.PARTICIPANT_COUNT -> "Participants"
    EventPlanMissingInformation.BUDGET_PER_PERSON -> "Budget"
    EventPlanMissingInformation.EVENT_TYPE -> "Event type"
    EventPlanMissingInformation.CONSTRAINTS -> "Constraints"
}

private fun Double.formatAmount(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
