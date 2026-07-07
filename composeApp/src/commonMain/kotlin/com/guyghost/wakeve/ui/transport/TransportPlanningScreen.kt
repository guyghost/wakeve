package com.guyghost.wakeve.ui.transport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.OptimizationType
import com.guyghost.wakeve.models.TransportLocation
import com.guyghost.wakeve.models.TransportPlan
import com.guyghost.wakeve.models.TransportReadiness

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportPlanningScreen(
    eventId: String,
    isOrganizer: Boolean,
    isParticipantConfirmed: Boolean?,
    confirmedDate: String?,
    selectedDestination: TransportLocation?,
    eventStatus: EventStatus,
    isReadOnly: Boolean,
    readiness: TransportReadiness? = null,
    plans: List<TransportPlan> = emptyList(),
    selectedPlanId: String? = null,
    pendingSync: Boolean = false,
    isTransportProviderConfigured: Boolean = true,
    onSaveDepartureLocation: (String) -> Unit = {},
    onGeneratePlan: (OptimizationType) -> Unit = {},
    onSelectFinalPlan: (TransportPlan) -> Unit = {},
    onMarkTransportNotNeeded: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val canAccessTransportDetails = isOrganizer || isParticipantConfirmed == true
    var selectedOptimization by remember { mutableStateOf(OptimizationType.BALANCED) }
    var departureInput by remember(eventId) { mutableStateOf("") }
    val missingDepartureParticipants = readiness?.missingDepartureParticipantNames.orEmpty()
        .ifEmpty { readiness?.missingDepartureParticipantIds.orEmpty() }
    val isReadinessComplete = readiness?.isComplete ?: false
    val transportNotNeeded = readiness?.transportNotNeeded == true
    val isMutableTransportStatus = eventStatus == EventStatus.CONFIRMED || eventStatus == EventStatus.COMPARING || eventStatus == EventStatus.ORGANIZING
    val canGenerate = isTransportProviderConfigured && isOrganizer && !isReadOnly && selectedDestination != null && !transportNotNeeded && (eventStatus == EventStatus.CONFIRMED || eventStatus == EventStatus.COMPARING || eventStatus == EventStatus.ORGANIZING) && (readiness?.canGeneratePlan ?: false)
    val canSelectFinal = isOrganizer && !isReadOnly && selectedDestination != null && plans.isNotEmpty() && (eventStatus == EventStatus.CONFIRMED || eventStatus == EventStatus.COMPARING || eventStatus == EventStatus.ORGANIZING)
    val canMarkTransportNotNeeded = isOrganizer && !isReadOnly && selectedDestination != null && !transportNotNeeded && (eventStatus == EventStatus.CONFIRMED || eventStatus == EventStatus.COMPARING || eventStatus == EventStatus.ORGANIZING)
    val canSaveDeparture = canAccessTransportDetails && !isReadOnly && selectedDestination != null && (eventStatus == EventStatus.CONFIRMED || eventStatus == EventStatus.COMPARING || eventStatus == EventStatus.ORGANIZING)
    val highlightedPlan = selectedPlanId
        ?.let { selectedId -> plans.firstOrNull { it.id == selectedId } }
        ?: plans.firstOrNull()
    val meetingPointSummary = transportMeetingPointSummary(
        selectedDestination = selectedDestination,
        highlightedPlan = highlightedPlan,
        readiness = readiness
    )
    val departureSummary = transportDepartureSummary(
        highlightedPlan = highlightedPlan,
        readiness = readiness
    )
    val generationUnavailableReason = when {
        !isTransportProviderConfigured -> transportProviderMissingMessage()
        !isOrganizer -> transportOrganizerOnlyGenerationMessage()
        selectedDestination == null -> transportDestinationRequiredForGenerationMessage()
        transportNotNeeded -> transportNotNeededMessage()
        isReadOnly || !isMutableTransportStatus -> transportReadOnlyMessage()
        readiness?.canGeneratePlan != true -> transportMissingDeparturesForGenerationMessage()
        else -> null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Transport") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!canAccessTransportDetails) {
            TransportAccessDenied(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TransportAnchorCard(
                    eventId = eventId,
                    confirmedDate = confirmedDate,
                    selectedDestination = selectedDestination,
                    pendingSync = pendingSync
                )
            }

            if (selectedDestination == null) {
                item {
                    MissingDestinationCard()
                }
            }

            item {
                TransportMeetingPointCard(summary = meetingPointSummary)
            }

            item {
                TransportDepartureSummaryCard(summary = departureSummary)
            }

            item {
                DepartureInputCard(
                    value = departureInput,
                    onValueChange = { departureInput = it },
                    onSaveDepartureLocation = {
                        val normalizedDeparture = departureInput.trim()
                        if (normalizedDeparture.isNotEmpty()) {
                            onSaveDepartureLocation(normalizedDeparture)
                            departureInput = ""
                        }
                    },
                    pendingSync = pendingSync,
                    canSaveDeparture = canSaveDeparture
                )
            }

            item {
                TransportReadinessCard(
                    readiness = readiness,
                    isReadinessComplete = isReadinessComplete,
                    missingDepartureParticipants = missingDepartureParticipants
                )
            }

            item {
                OptimizationModeCard(
                    selectedOptimization = selectedOptimization,
                    onOptimizationChanged = { selectedOptimization = it }
                )
            }

            item {
                Button(
                    onClick = { onGeneratePlan(selectedOptimization) },
                    enabled = canGenerate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(transportGeneratePlanLabel())
                }

                if (!canGenerate) {
                    Text(
                        text = generationUnavailableReason.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (canMarkTransportNotNeeded) {
                    OutlinedButton(
                        onClick = onMarkTransportNotNeeded,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(transportNotNeededActionLabel())
                    }
                } else if (transportNotNeeded) {
                    Text(
                        text = transportNotNeededMessage(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (plans.isEmpty()) {
                item {
                    EmptyPlansCard(isTransportProviderConfigured = isTransportProviderConfigured)
                }
            } else {
                items(plans, key = { it.id }) { plan ->
                    TransportPlanCard(
                        plan = plan,
                        isSelected = plan.id == selectedPlanId,
                        canSelectFinal = canSelectFinal && plan.id != selectedPlanId,
                        onSelectFinal = { onSelectFinalPlan(plan) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransportDepartureSummaryCard(summary: TransportDepartureSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Route, contentDescription = null)
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = summary.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            summary.detail?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TransportMeetingPointCard(summary: TransportMeetingPointSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = summary.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            summary.detail?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun DepartureInputCard(
    value: String,
    onValueChange: (String) -> Unit,
    onSaveDepartureLocation: () -> Unit,
    pendingSync: Boolean,
    canSaveDeparture: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = transportDepartureTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(transportDepartureInputLabel()) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onSaveDepartureLocation,
                enabled = canSaveDeparture && value.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(transportSaveDepartureLabel(pendingSync))
            }
        }
    }
}

@Composable
private fun TransportAccessDenied(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
                text = transportAccessDeniedTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = transportAccessDeniedMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun TransportAnchorCard(
    eventId: String,
    confirmedDate: String?,
    selectedDestination: TransportLocation?,
    pendingSync: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null)
                Text(
                    text = transportAnchorTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(transportEventLabel(eventId), style = MaterialTheme.typography.labelMedium)
            Text(transportConfirmedDateLabel(confirmedDate))
            Text(transportDestinationLabel(selectedDestination?.name))
            if (pendingSync) {
                PendingSyncLabel()
            }
        }
    }
}

@Composable
private fun MissingDestinationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
                text = transportMissingDestinationTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = transportMissingDestinationMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun PendingSyncLabel() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = transportPendingSyncMessage(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun TransportReadinessCard(
    readiness: TransportReadiness?,
    isReadinessComplete: Boolean,
    missingDepartureParticipants: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isReadinessComplete) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null
                )
                Text(
                    text = transportReadinessTitle(isReadinessComplete),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (readiness?.transportNotNeeded == true) {
                Text(transportNotNeededMessage())
            } else if (missingDepartureParticipants.isEmpty()) {
                Text(transportReadinessCompleteMessage())
            } else {
                Text(transportMissingDepartureTitle())
                missingDepartureParticipants.forEach { participant ->
                    Text(
                        text = transportMissingDepartureParticipantLabel(participant),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OptimizationModeCard(
    selectedOptimization: OptimizationType,
    onOptimizationChanged: (OptimizationType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = transportOptimizationTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    OptimizationType.COST_MINIMIZE,
                    OptimizationType.TIME_MINIMIZE,
                    OptimizationType.BALANCED
                ).forEach { type ->
                    FilterChip(
                        selected = selectedOptimization == type,
                        onClick = { onOptimizationChanged(type) },
                        label = {
                            Text(
                                text = type.label(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPlansCard(isTransportProviderConfigured: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Route, contentDescription = null)
            Text(transportEmptyPlansTitle(), style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (isTransportProviderConfigured) {
                    transportEmptyPlansConfiguredMessage()
                } else {
                    transportEmptyPlansProviderMissingMessage()
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransportPlanCard(
    plan: TransportPlan,
    isSelected: Boolean,
    canSelectFinal: Boolean,
    onSelectFinal: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = plan.optimizationType.label(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Text(transportSelectedPlanLabel(), color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(transportGeneratedPlanLabel(plan.id))
            Text(transportTotalGroupCostLabel(plan.totalGroupCost))
            Text(transportRoutesCountLabel(plan.participantRoutes.size))
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canSelectFinal) {
                    Button(onClick = onSelectFinal) {
                        Text(transportSelectFinalPlanLabel())
                    }
                } else {
                    OutlinedButton(onClick = {}, enabled = false) {
                        Text(transportFinalSelectionUnavailableLabel(isSelected))
                    }
                }
            }
        }
    }
}

private fun OptimizationType.label(): String {
    return when (this) {
        OptimizationType.COST_MINIMIZE -> "Coût"
        OptimizationType.TIME_MINIMIZE -> "Temps"
        OptimizationType.BALANCED -> "Équilibré"
    }
}

internal fun transportProviderMissingMessage(): String =
    "Aucun fournisseur de transport réel n'est configuré. Wakeve peut collecter les départs, mais ne génère pas encore de prix, horaires ou réservations."

internal fun transportOrganizerOnlyGenerationMessage(): String =
    "Seul l'organisateur peut générer le plan de transport partagé."

internal fun transportDestinationRequiredForGenerationMessage(): String =
    "Sélectionnez une destination de scénario avant de générer un plan de transport."

internal fun transportNotNeededMessage(): String =
    "Le transport est indiqué comme non requis pour cet événement."

internal fun transportReadOnlyMessage(): String =
    "Le transport est en lecture seule pour cet événement."

internal fun transportMissingDeparturesForGenerationMessage(): String =
    "Ajoutez tous les points de départ manquants avant de générer les options de transport."

internal fun transportGeneratePlanLabel(): String = "Générer le plan"

internal fun transportNotNeededActionLabel(): String = "Transport non requis"

internal fun transportDepartureTitle(): String = "Point de départ"

internal fun transportDepartureInputLabel(): String = "Ville, gare ou aéroport"

internal fun transportSaveDepartureLabel(pendingSync: Boolean): String =
    if (pendingSync) "Mettre à jour le départ" else "Enregistrer le départ"

internal fun transportAccessDeniedTitle(): String = "Transport verrouillé"

internal fun transportAccessDeniedMessage(): String =
    "Confirmez votre présence pour accéder aux départs, aux trajets et aux plans retenus."

internal fun transportAnchorTitle(): String = "Date confirmée et destination"

internal fun transportEventLabel(eventId: String): String = "Événement $eventId"

internal fun transportConfirmedDateLabel(confirmedDate: String?): String =
    "Date confirmée : ${confirmedDate ?: "date bientôt confirmée"}"

internal fun transportDestinationLabel(destinationName: String?): String =
    "Destination : ${destinationName ?: "aucune destination sélectionnée"}"

internal fun transportMissingDestinationTitle(): String = "Destination requise"

internal fun transportMissingDestinationMessage(): String =
    "Sélectionnez une destination de scénario avant d'enregistrer les départs, de générer les plans ou de finaliser le transport."

internal fun transportPendingSyncMessage(): String =
    "Synchronisation en attente. Modifications locales en attente d'envoi."

internal fun transportReadinessTitle(isComplete: Boolean): String =
    if (isComplete) "Préparation complète" else "Préparation incomplète"

internal fun transportReadinessCompleteMessage(): String =
    "Tous les participants confirmés ont un point de départ."

internal fun transportMissingDepartureTitle(): String = "Départ manquant"

internal fun transportMissingDepartureParticipantLabel(participant: String): String =
    "- $participant"

internal fun transportOptimizationTitle(): String = "Optimisation"

internal fun transportEmptyPlansTitle(): String = "Aucun plan généré"

internal fun transportEmptyPlansConfiguredMessage(): String =
    "Choisissez un mode d'optimisation et générez un plan lorsque tous les départs sont prêts."

internal fun transportEmptyPlansProviderMissingMessage(): String =
    "Les départs peuvent être collectés maintenant. La génération de trajets nécessite un fournisseur réel de transport."

internal fun transportSelectedPlanLabel(): String = "Plan sélectionné"

internal fun transportGeneratedPlanLabel(planId: String): String = "Plan généré : $planId"

internal fun transportTotalGroupCostLabel(totalGroupCost: Double): String =
    "Coût total du groupe : $totalGroupCost EUR"

internal fun transportRoutesCountLabel(routeCount: Int): String = "Trajets : $routeCount"

internal fun transportSelectFinalPlanLabel(): String = "Sélectionner le plan final"

internal fun transportFinalSelectionUnavailableLabel(isSelected: Boolean): String =
    if (isSelected) "Plan final sélectionné" else "Sélection indisponible"

internal data class TransportMeetingPointSummary(
    val title: String,
    val body: String,
    val detail: String? = null
)

internal fun transportMeetingPointSummary(
    selectedDestination: TransportLocation?,
    highlightedPlan: TransportPlan?,
    readiness: TransportReadiness?
): TransportMeetingPointSummary {
    val destinationName = selectedDestination?.name?.trim().orEmpty()
    val missingParticipants = readiness?.missingDepartureParticipantNames.orEmpty()
        .ifEmpty { readiness?.missingDepartureParticipantIds.orEmpty() }

    if (readiness?.transportNotNeeded == true) {
        return TransportMeetingPointSummary(
            title = transportMeetingPointTitle(destinationName),
            body = transportMeetingPointTransportNotNeededBody(destinationName)
        )
    }

    if (destinationName.isBlank()) {
        return TransportMeetingPointSummary(
            title = transportMeetingPointMissingDestinationTitle(),
            body = transportMeetingPointMissingDestinationBody()
        )
    }

    val arrivals = highlightedPlan?.groupArrivals.orEmpty().filter { it.isNotBlank() }
    if (arrivals.isNotEmpty()) {
        return TransportMeetingPointSummary(
            title = transportMeetingPointTitle(destinationName),
            body = transportMeetingPointPlannedArrivalBody(destinationName, arrivals),
            detail = transportMeetingPointMissingDeparturesDetail(missingParticipants)
        )
    }

    return TransportMeetingPointSummary(
        title = transportMeetingPointTitle(destinationName),
        body = if (readiness?.isComplete == true) {
            transportMeetingPointReadyBody(destinationName)
        } else {
            transportMeetingPointProvisionalBody(destinationName)
        },
        detail = transportMeetingPointMissingDeparturesDetail(missingParticipants)
    )
}

internal fun transportMeetingPointTitle(destinationName: String): String =
    if (destinationName.isBlank()) "Point de rendez-vous" else "Rendez-vous à $destinationName"

internal fun transportMeetingPointMissingDestinationTitle(): String = "Point de rendez-vous à définir"

internal fun transportMeetingPointMissingDestinationBody(): String =
    "Choisissez d'abord la destination finale pour afficher clairement où le groupe se retrouve."

internal fun transportMeetingPointTransportNotNeededBody(destinationName: String): String =
    if (destinationName.isBlank()) {
        "Transport non requis : le groupe se retrouve directement sur place."
    } else {
        "Transport non requis : le groupe se retrouve directement à $destinationName."
    }

internal fun transportMeetingPointReadyBody(destinationName: String): String =
    "Tous les départs sont prêts. Générez un plan pour confirmer l'heure de rendez-vous à $destinationName."

internal fun transportMeetingPointProvisionalBody(destinationName: String): String =
    "$destinationName est le point de rendez-vous provisoire tant que tous les départs ne sont pas renseignés."

internal fun transportMeetingPointPlannedArrivalBody(
    destinationName: String,
    arrivals: List<String>
): String {
    return if (arrivals.size == 1) {
        "Arrivée groupée prévue à $destinationName : ${transportMeetingArrivalLabel(arrivals.first())}."
    } else {
        "${arrivals.size} arrivées groupées prévues à $destinationName. Ouvrez le plan retenu pour suivre chaque trajet."
    }
}

internal fun transportMeetingPointMissingDeparturesDetail(missingParticipants: List<String>): String? {
    if (missingParticipants.isEmpty()) return null
    val shownNames = missingParticipants.take(3).joinToString(", ")
    val remainingCount = missingParticipants.size - 3
    val suffix = if (remainingCount > 0) " + $remainingCount autre${if (remainingCount > 1) "s" else ""}" else ""
    return "Départs encore manquants : $shownNames$suffix."
}

internal fun transportMeetingArrivalLabel(rawArrival: String): String {
    val trimmed = rawArrival.trim()
    val date = trimmed.substringBefore('T', missingDelimiterValue = "")
    val timePart = trimmed.substringAfter('T', missingDelimiterValue = "")
        .take(5)
    return if (date.isNotBlank() && timePart.length == 5) {
        "$date à $timePart"
    } else {
        trimmed
    }
}

internal data class TransportDepartureSummary(
    val title: String,
    val body: String,
    val detail: String? = null
)

internal fun transportDepartureSummary(
    highlightedPlan: TransportPlan?,
    readiness: TransportReadiness?
): TransportDepartureSummary {
    if (readiness?.transportNotNeeded == true) {
        return TransportDepartureSummary(
            title = transportDepartureSummaryTitle(),
            body = "Aucun départ collectif requis pour cet événement."
        )
    }

    val departures = highlightedPlan
        ?.participantRoutes
        .orEmpty()
        .values
        .mapNotNull { route -> route.segments.firstOrNull()?.departureTime?.takeIf { it.isNotBlank() } }
        .sorted()

    if (departures.isNotEmpty()) {
        return TransportDepartureSummary(
            title = transportDepartureSummaryTitle(),
            body = "Premier départ prévu : ${transportMeetingArrivalLabel(departures.first())}.",
            detail = transportDeparturePlanDetail(departures)
        )
    }

    val missingParticipants = readiness?.missingDepartureParticipantNames.orEmpty()
        .ifEmpty { readiness?.missingDepartureParticipantIds.orEmpty() }

    if (missingParticipants.isNotEmpty()) {
        return TransportDepartureSummary(
            title = transportDepartureIncompleteTitle(),
            body = "Impossible de répondre à quand partons-nous tant que tous les départs ne sont pas renseignés.",
            detail = transportMeetingPointMissingDeparturesDetail(missingParticipants)
        )
    }

    return if (readiness?.isComplete == true) {
        TransportDepartureSummary(
            title = transportDepartureIncompleteTitle(),
            body = "Tous les points de départ sont prêts. Générez un plan pour afficher l'heure de départ du groupe."
        )
    } else {
        TransportDepartureSummary(
            title = transportDepartureIncompleteTitle(),
            body = "Ajoutez les points de départ pour calculer l'heure de départ du groupe."
        )
    }
}

internal fun transportDepartureSummaryTitle(): String = "Départ du groupe"

internal fun transportDepartureIncompleteTitle(): String = "Départ à confirmer"

internal fun transportDeparturePlanDetail(departures: List<String>): String? {
    if (departures.isEmpty()) return null
    return if (departures.size == 1) {
        "1 trajet suivi."
    } else {
        "${departures.size} trajets suivis · Dernier départ : ${transportMeetingArrivalLabel(departures.last())}."
    }
}
