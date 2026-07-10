package com.guyghost.wakeve.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.R
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.productlanguage.AllowedAction
import com.guyghost.wakeve.productlanguage.PendingFact
import com.guyghost.wakeve.productlanguage.ProductLanguageInput
import com.guyghost.wakeve.productlanguage.ProductLanguageProjection
import com.guyghost.wakeve.productlanguage.SemanticKey
import com.guyghost.wakeve.productlanguage.UserRole
import com.guyghost.wakeve.productlanguage.projectEventState

/**
 * Modern Event Detail View
 * 
 * Displays event details and actions based on event status.
 * Integrates all new PRD features with contextual access controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernEventDetailView(
    event: Event,
    userId: String,
    onNavigateToScenarioList: () -> Unit,
    onNavigateToBudgetOverview: () -> Unit,
    onNavigateToAccommodation: () -> Unit,
    onNavigateToMealPlanning: () -> Unit,
    onNavigateToEquipmentChecklist: () -> Unit,
    onNavigateToActivityPlanning: () -> Unit,
    onNavigateToComments: () -> Unit,
    onNavigateToHome: () -> Unit,
    onAddToCalendar: () -> Unit = {},
    onShareInvite: () -> Unit = {},
    dayOfSummary: EventDayOfSummary? = null,
    destinationSummary: EventDestinationSummary? = null,
    settlementSummary: EventSettlementSummary? = null,
    pendingFacts: Set<PendingFact> = emptySet(),
    allowedAction: AllowedAction? = AllowedAction.CONTINUE,
    onProjectedPrimaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val openBudget = onNavigateToBudgetOverview
    val projection = projectEventState(
        ProductLanguageInput(event.status, UserRole.PARTICIPANT, emptySet(), pendingFacts, allowedAction)
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(event.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.navigate_back))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToComments) {
                        Icon(Icons.AutoMirrored.Filled.Comment, stringResource(R.string.participants))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Event Status Header
            EventStatusHeader(event, projection)
            
            // Event Description
            EventDescriptionCard(event)

            EventNextStepCard(projection)

            ProjectionOutcome(projection, onProjectedPrimaryAction)

            dayOfSummary?.let {
                EventDayOfSummaryCard(summary = it)
            }

            destinationSummary?.let {
                EventDestinationSummaryCard(summary = it, onOpenScenarios = onNavigateToScenarioList)
            }

            settlementSummary?.let {
                EventSettlementSummaryCard(summary = it, onOpenSettlements = openBudget)
            }
            
            // Action Buttons based on event status
            when (event.status) {
                EventStatus.COMPARING -> {
                    ComparingModeActions(
                        onNavigateToScenarioList = onNavigateToScenarioList,
                        onOpenBudget = openBudget
                    )
                }
                EventStatus.CONFIRMED -> {
                    ConfirmedModeActions(
                        onNavigateToScenarioList = onNavigateToScenarioList,
                        onOpenBudget = openBudget,
                        onNavigateToAccommodation = onNavigateToAccommodation,
                        onNavigateToMealPlanning = onNavigateToMealPlanning,
                        onNavigateToEquipmentChecklist = onNavigateToEquipmentChecklist,
                        onNavigateToActivityPlanning = onNavigateToActivityPlanning,
                        onAddToCalendar = onAddToCalendar,
                        onShareInvite = onShareInvite
                    )
                }
                EventStatus.ORGANIZING -> {
                    OrganizingModeActions(
                        onNavigateToScenarioList = onNavigateToScenarioList,
                        onOpenBudget = openBudget,
                        onNavigateToAccommodation = onNavigateToAccommodation,
                        onNavigateToMealPlanning = onNavigateToMealPlanning,
                        onNavigateToEquipmentChecklist = onNavigateToEquipmentChecklist,
                        onNavigateToActivityPlanning = onNavigateToActivityPlanning
                    )
                }
                EventStatus.FINALIZED -> {
                    FinalizedModeActions()
                }
                EventStatus.DRAFT -> {
                    DraftModeActions(
                        onNavigateToScenarioList = onNavigateToScenarioList
                    )
                }
                EventStatus.POLLING -> {
                    PollingModeActions()
                }
            }
        }
    }
}

@Composable
private fun EventDestinationSummaryCard(
    summary: EventDestinationSummary,
    onOpenScenarios: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
                    text = stringResource(R.string.event_option_summary, option.typeLabel, option.title, option.body),
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.view_scenarios))
                }
            }
        }
    }
}

@Composable
private fun EventSettlementSummaryCard(
    summary: EventSettlementSummary,
    onOpenSettlements: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
                onClick = onOpenSettlements,
                enabled = summary.canOpenBudget,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(summary.actionLabel)
            }
        }
    }
}

@Composable
private fun EventDayOfSummaryCard(summary: EventDayOfSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = summary.controlLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                summary.checklist.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.event_checklist_summary, item.statusLabel, item.title),
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
private fun EventStatusHeader(event: Event, projection: ProductLanguageProjection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (event.status) {
                EventStatus.DRAFT -> MaterialTheme.colorScheme.primaryContainer
                EventStatus.POLLING -> MaterialTheme.colorScheme.secondaryContainer
                EventStatus.COMPARING -> MaterialTheme.colorScheme.tertiaryContainer
                EventStatus.CONFIRMED -> MaterialTheme.colorScheme.primary
                EventStatus.ORGANIZING -> MaterialTheme.colorScheme.tertiary
                EventStatus.FINALIZED -> MaterialTheme.colorScheme.primary
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(eventDetailResource(projection.title)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (event.status) {
                    EventStatus.DRAFT -> MaterialTheme.colorScheme.onPrimaryContainer
                    EventStatus.POLLING -> MaterialTheme.colorScheme.onSecondaryContainer
                    EventStatus.COMPARING -> MaterialTheme.colorScheme.onTertiaryContainer
                    EventStatus.CONFIRMED -> MaterialTheme.colorScheme.onPrimary
                    EventStatus.ORGANIZING -> MaterialTheme.colorScheme.onTertiary
                    EventStatus.FINALIZED -> MaterialTheme.colorScheme.onPrimary
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (event.status) {
                    EventStatus.DRAFT -> stringResource(R.string.status_draft_description)
                    EventStatus.POLLING -> stringResource(R.string.status_polling_description)
                    EventStatus.COMPARING -> stringResource(R.string.status_comparing_description)
                    EventStatus.CONFIRMED -> stringResource(R.string.status_confirmed_description)
                    EventStatus.ORGANIZING -> stringResource(R.string.status_organizing_description)
                    EventStatus.FINALIZED -> stringResource(R.string.status_finalized_description)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = when (event.status) {
                    EventStatus.DRAFT -> MaterialTheme.colorScheme.onPrimaryContainer
                    EventStatus.POLLING -> MaterialTheme.colorScheme.onSecondaryContainer
                    EventStatus.COMPARING -> MaterialTheme.colorScheme.onTertiaryContainer
                    EventStatus.CONFIRMED -> MaterialTheme.colorScheme.onPrimary
                    EventStatus.ORGANIZING -> MaterialTheme.colorScheme.onTertiary
                    EventStatus.FINALIZED -> MaterialTheme.colorScheme.onPrimary
                }
            )
        }
    }
}

@Composable
private fun EventNextStepCard(projection: ProductLanguageProjection) {
    val status = projection.domainStatus
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(eventDetailNextStepTitle(status)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(eventDetailNextStepBody(status)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProjectionOutcome(projection: ProductLanguageProjection, onPrimaryAction: (() -> Unit)?) {
    projection.status?.let { Text(stringResource(eventDetailResource(it))) }
    projection.primaryAction?.let { action ->
        if (onPrimaryAction != null) {
            Button(onClick = onPrimaryAction, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(eventDetailResource(action)))
            }
        }
    }
}

@Composable
private fun EventDescriptionCard(event: Event) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.hosted_by_you),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DraftModeActions(
    onNavigateToScenarioList: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(eventDetailNextStepTitle(EventStatus.DRAFT)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Button(
            onClick = onNavigateToScenarioList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.create_scenarios))
        }
    }
}

@Composable
private fun PollingModeActions() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(eventDetailNextStepTitle(EventStatus.POLLING)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = stringResource(R.string.waiting_for_votes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ComparingModeActions(
    onNavigateToScenarioList: () -> Unit,
    onOpenBudget: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(eventDetailNextStepTitle(EventStatus.COMPARING)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Button(
            onClick = onNavigateToScenarioList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.view_scenarios))
        }
        
        Button(
            onClick = onOpenBudget,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Euro, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.configure_budget))
        }
    }
}

@Composable
private fun ConfirmedModeActions(
    onNavigateToScenarioList: () -> Unit,
    onOpenBudget: () -> Unit,
    onNavigateToAccommodation: () -> Unit,
    onNavigateToMealPlanning: () -> Unit,
    onNavigateToEquipmentChecklist: () -> Unit,
    onNavigateToActivityPlanning: () -> Unit,
    onAddToCalendar: () -> Unit,
    onShareInvite: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(eventDetailNextStepTitle(EventStatus.CONFIRMED)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Button(
            onClick = onAddToCalendar,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_to_calendar))
        }

        Button(
            onClick = onShareInvite,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.share_invitation))
        }
        
        Button(
            onClick = onNavigateToScenarioList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.view_scenarios))
        }
        
        Button(
            onClick = onOpenBudget,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Euro, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.view_budget))
        }
        
        Button(
            onClick = onNavigateToAccommodation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Hotel, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.view_accommodation))
        }
        
        Button(
            onClick = onNavigateToMealPlanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Restaurant, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.view_meals))
        }
        
        Button(
            onClick = onNavigateToEquipmentChecklist,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ShoppingBag, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.view_equipment))
        }
        
        Button(
            onClick = onNavigateToActivityPlanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.LocalActivity, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.view_activities))
        }
    }
}

@Composable
private fun OrganizingModeActions(
    onNavigateToScenarioList: () -> Unit,
    onOpenBudget: () -> Unit,
    onNavigateToAccommodation: () -> Unit,
    onNavigateToMealPlanning: () -> Unit,
    onNavigateToEquipmentChecklist: () -> Unit,
    onNavigateToActivityPlanning: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(eventDetailNextStepTitle(EventStatus.ORGANIZING)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Button(
            onClick = onNavigateToScenarioList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.view_scenarios))
        }
        
        Button(
            onClick = onOpenBudget,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Euro, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.view_budget))
        }
        
        Button(
            onClick = onNavigateToAccommodation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Hotel, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.view_accommodation))
        }
        
        Button(
            onClick = onNavigateToMealPlanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Restaurant, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.view_meals))
        }
        
        Button(
            onClick = onNavigateToEquipmentChecklist,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ShoppingBag, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.view_equipment))
        }
        
        Button(
            onClick = onNavigateToActivityPlanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.LocalActivity, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.view_activities))
        }
    }
}

@Composable
private fun FinalizedModeActions(
) {
    val readOnly = true
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(eventDetailNextStepTitle(EventStatus.FINALIZED)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(eventDetailNextStepBody(EventStatus.FINALIZED)),
            style = MaterialTheme.typography.bodyMedium,
            color = if (readOnly) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

internal fun eventDetailResource(key: SemanticKey): Int = when (key.value) {
    "event.state.draft" -> R.string.event_state_draft
    "event.state.polling" -> R.string.event_state_polling
    "event.state.comparing" -> R.string.event_state_comparing
    "event.state.confirmed" -> R.string.event_state_confirmed
    "event.state.organizing" -> R.string.event_state_organizing
    "event.state.finalized" -> R.string.event_state_finalized
    "sync.waiting" -> R.string.sync_waiting
    "sync.conflict" -> R.string.sync_conflict_title
    "sync.retry" -> R.string.action_retry
    "event.action.continue" -> R.string.next
    else -> error(key.value)
}

internal fun eventDetailStatusLabel(status: EventStatus): Int = eventDetailResource(
    projectEventState(ProductLanguageInput(status, UserRole.PARTICIPANT, emptySet(), emptySet(), null)).title
)

internal fun eventDetailNextStepTitle(status: EventStatus): Int = when (status) {
    EventStatus.DRAFT -> R.string.event_next_step_draft
    EventStatus.POLLING -> R.string.event_next_step_polling
    EventStatus.COMPARING -> R.string.event_next_step_comparing
    EventStatus.CONFIRMED -> R.string.event_next_step_confirmed
    EventStatus.ORGANIZING -> R.string.event_next_step_organizing
    EventStatus.FINALIZED -> R.string.event_terminal_summary
}

internal fun eventDetailNextStepBody(status: EventStatus): Int = when (status) {
    EventStatus.DRAFT -> R.string.event_next_step_draft_body
    EventStatus.POLLING -> R.string.event_next_step_polling_body
    EventStatus.COMPARING -> R.string.event_next_step_comparing_body
    EventStatus.CONFIRMED -> R.string.event_next_step_confirmed_body
    EventStatus.ORGANIZING -> R.string.event_next_step_organizing_body
    EventStatus.FINALIZED -> R.string.event_terminal_body
}
