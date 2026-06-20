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
    val generationUnavailableReason = when {
        !isTransportProviderConfigured -> "Aucun fournisseur de transport réel n'est configuré. Wakeve peut collecter les départs, mais ne génère pas encore de prix, horaires ou réservations."
        !isOrganizer -> "Seul l'organisateur peut générer le plan de transport partagé."
        selectedDestination == null -> "Sélectionnez une destination de scénario avant de générer un plan de transport."
        transportNotNeeded -> "Le transport est indiqué comme non requis pour cet événement."
        isReadOnly || !isMutableTransportStatus -> "Le transport est en lecture seule pour cet événement."
        readiness?.canGeneratePlan != true -> "Ajoutez tous les points de départ manquants avant de générer les options de transport."
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
                    Text("Générer le plan")
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
                        Text("Transport non requis")
                    }
                } else if (transportNotNeeded) {
                    Text(
                        text = "Le transport est indiqué comme non requis pour cet événement.",
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
                text = "Point de départ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Ville, gare ou aéroport") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onSaveDepartureLocation,
                enabled = canSaveDeparture && value.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (pendingSync) "Mettre à jour le départ" else "Enregistrer le départ")
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
                text = "Transport verrouillé",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Confirmez votre présence pour accéder aux départs, aux trajets et aux plans retenus.",
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
                    text = "Date confirmée et destination",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text("Event $eventId", style = MaterialTheme.typography.labelMedium)
            Text("Date confirmée : ${confirmedDate ?: "Date bientôt confirmée"}")
            Text("Destination : ${selectedDestination?.name ?: "Aucune destination sélectionnée"}")
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
                text = "Destination requise",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Sélectionnez une destination de scénario avant d'enregistrer les départs, de générer les plans ou de finaliser le transport.",
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
                text = "Synchronisation en attente. Modifications locales en attente d'envoi.",
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
                    text = if (isReadinessComplete) "Préparation complète" else "Préparation incomplète",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (readiness?.transportNotNeeded == true) {
                Text("Transport non requis pour cet événement.")
            } else if (missingDepartureParticipants.isEmpty()) {
                Text("Tous les participants confirmés ont un point de départ.")
            } else {
                Text("Départ manquant")
                missingDepartureParticipants.forEach { participant ->
                    Text(
                        text = "- $participant",
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
                text = "Optimisation",
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
            Text("Aucun plan généré", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (isTransportProviderConfigured) {
                    "Choisissez un mode d'optimisation et générez un plan lorsque tous les départs sont prêts."
                } else {
                    "Les départs peuvent être collectés maintenant. La génération de trajets nécessite un fournisseur réel de transport."
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
                    Text("Plan sélectionné", color = MaterialTheme.colorScheme.primary)
                }
            }
            Text("Plan généré : ${plan.id}")
            Text("Coût total du groupe : ${plan.totalGroupCost}")
            Text("Trajets : ${plan.participantRoutes.size}")
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canSelectFinal) {
                    Button(onClick = onSelectFinal) {
                        Text("Sélectionner le plan final")
                    }
                } else {
                    OutlinedButton(onClick = {}, enabled = false) {
                        Text(if (isSelected) "Plan final sélectionné" else "Sélection indisponible")
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
