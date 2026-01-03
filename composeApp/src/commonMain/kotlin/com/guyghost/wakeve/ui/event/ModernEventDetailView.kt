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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.List
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus

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
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(event.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToComments) {
                        Icon(Icons.Default.Comment, "Commentaires")
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
            EventStatusHeader(event)
            
            // Event Description
            EventDescriptionCard(event)
            
            // Action Buttons based on event status
            when (event.status) {
                EventStatus.DRAFT -> {
                    DraftModeActions(
                        onNavigateToScenarioList = onNavigateToScenarioList,
                        onNavigateToBudgetOverview = onNavigateToBudgetOverview
                    )
                }
                EventStatus.POLLING -> {
                    PollingModeActions()
                }
                EventStatus.COMPARING -> {
                    ComparingModeActions(
                        onNavigateToScenarioList = onNavigateToScenarioList,
                        onNavigateToBudgetOverview = onNavigateToBudgetOverview
                    )
                }
                EventStatus.CONFIRMED -> {
                    ConfirmedModeActions(
                        onNavigateToScenarioList = onNavigateToScenarioList,
                        onNavigateToBudgetOverview = onNavigateToBudgetOverview,
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
                        onNavigateToBudgetOverview = onNavigateToBudgetOverview,
                        onNavigateToAccommodation = onNavigateToAccommodation,
                        onNavigateToMealPlanning = onNavigateToMealPlanning,
                        onNavigateToEquipmentChecklist = onNavigateToEquipmentChecklist,
                        onNavigateToActivityPlanning = onNavigateToActivityPlanning
                    )
                }
                EventStatus.FINALIZED -> {
                    FinalizedModeActions(
                        onNavigateToScenarioList = onNavigateToScenarioList,
                        onNavigateToBudgetOverview = onNavigateToBudgetOverview,
                        onNavigateToAccommodation = onNavigateToAccommodation,
                        onNavigateToMealPlanning = onNavigateToMealPlanning,
                        onNavigateToEquipmentChecklist = onNavigateToEquipmentChecklist,
                        onNavigateToActivityPlanning = onNavigateToActivityPlanning,
                        onAddToCalendar = onAddToCalendar,
                        onShareInvite = onShareInvite
                    )
                }
            }
        }
    }
}

@Composable
private fun EventStatusHeader(event: Event) {
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
                text = event.status.name.lowercase().replaceFirstChar { it.uppercase() },
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
                    EventStatus.DRAFT -> "En cours de création"
                    EventStatus.POLLING -> "Sondage en cours"
                    EventStatus.COMPARING -> "Comparaison des scénarios"
                    EventStatus.CONFIRMED -> "Date confirmée"
                    EventStatus.ORGANIZING -> "Organisation en cours"
                    EventStatus.FINALIZED -> "Événement finalisé"
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
                text = "Organisé par: Vous", // Simplified for now
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DraftModeActions(
    onNavigateToScenarioList: () -> Unit,
    onNavigateToBudgetOverview: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Actions en mode brouillon",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Button(
            onClick = onNavigateToScenarioList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.List, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Créer des scénarios")
        }
        
        Button(
            onClick = onNavigateToBudgetOverview,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Euro, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Configurer le budget")
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
            text = "Actions en mode sondage",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "En attente des votes des participants...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ComparingModeActions(
    onNavigateToScenarioList: () -> Unit,
    onNavigateToBudgetOverview: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Actions en mode comparaison",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Button(
            onClick = onNavigateToScenarioList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.List, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Voir les scénarios")
        }
        
        Button(
            onClick = onNavigateToBudgetOverview,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Euro, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Configurer le budget")
        }
    }
}

@Composable
private fun ConfirmedModeActions(
    onNavigateToScenarioList: () -> Unit,
    onNavigateToBudgetOverview: () -> Unit,
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
            text = "Actions en mode confirmé",
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
            Text("Ajouter au calendrier")
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
            Text("Partager l'invitation")
        }
        
        Button(
            onClick = onNavigateToScenarioList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.List, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Voir les scénarios")
        }
        
        Button(
            onClick = onNavigateToBudgetOverview,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Euro, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Budget")
        }
        
        Button(
            onClick = onNavigateToAccommodation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Hotel, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Hébergement")
        }
        
        Button(
            onClick = onNavigateToMealPlanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Restaurant, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Repas")
        }
        
        Button(
            onClick = onNavigateToEquipmentChecklist,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ShoppingBag, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Équipement")
        }
        
        Button(
            onClick = onNavigateToActivityPlanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.LocalActivity, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Activités")
        }
    }
}

@Composable
private fun OrganizingModeActions(
    onNavigateToScenarioList: () -> Unit,
    onNavigateToBudgetOverview: () -> Unit,
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
            text = "Actions en mode organisation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Button(
            onClick = onNavigateToScenarioList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.List, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Voir les scénarios")
        }
        
        Button(
            onClick = onNavigateToBudgetOverview,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Euro, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Budget")
        }
        
        Button(
            onClick = onNavigateToAccommodation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Hotel, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Hébergement")
        }
        
        Button(
            onClick = onNavigateToMealPlanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Restaurant, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Repas")
        }
        
        Button(
            onClick = onNavigateToEquipmentChecklist,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ShoppingBag, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Équipement")
        }
        
        Button(
            onClick = onNavigateToActivityPlanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.LocalActivity, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Activités")
        }
    }
}

@Composable
private fun FinalizedModeActions(
    onNavigateToScenarioList: () -> Unit,
    onNavigateToBudgetOverview: () -> Unit,
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
            text = "Actions en mode finalisé",
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
            Text("Ajouter au calendrier")
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
            Text("Partager l'invitation")
        }
        
        Button(
            onClick = onNavigateToScenarioList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.List, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Voir les scénarios")
        }
        
        Button(
            onClick = onNavigateToBudgetOverview,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Euro, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Budget")
        }
        
        Button(
            onClick = onNavigateToAccommodation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Hotel, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Hébergement")
        }
        
        Button(
            onClick = onNavigateToMealPlanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Restaurant, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Repas")
        }
        
        Button(
            onClick = onNavigateToEquipmentChecklist,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ShoppingBag, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Équipement")
        }
        
        Button(
            onClick = onNavigateToActivityPlanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.LocalActivity, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Activités")
        }
    }
}