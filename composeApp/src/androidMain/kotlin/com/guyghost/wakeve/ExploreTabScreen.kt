package com.guyghost.wakeve

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Explore Tab Screen - Featured events, templates, and tips.
 * 
 * Displays:
 * - Featured event templates
 * - Planning tips and best practices
 * - Placeholder for Phase 3 Suggestions (AI-powered recommendations)
 * 
 * Matches iOS ExploreTabView with Material You design.
 * 
 * @param onEventClick Callback when user clicks on a featured event (eventId)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreTabScreen(
    onEventClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Explorer",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Découvrez de nouvelles idées",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Modèles d'événements, conseils et suggestions personnalisées",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Event Templates Section
            item {
                SectionHeader(title = "Modèles d'événements")
            }
            
            items(eventTemplates) { template ->
                TemplateCard(template = template, onClick = {
                    // TODO: Create event from template
                })
            }
            
            // Planning Tips Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "Conseils de planification")
            }
            
            items(planningTips) { tip ->
                TipCard(tip = tip)
            }
            
            // Phase 3 Placeholder
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ComingSoonCard(
                    title = "Suggestions intelligentes",
                    description = "Bientôt : Recommandations personnalisées basées sur vos préférences et historique d'événements.",
                    icon = Icons.Default.Star
                )
            }
        }
    }
}

/**
 * Section header composable.
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * Template card composable.
 */
@Composable
private fun TemplateCard(
    template: EventTemplate,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = template.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Utiliser ce modèle",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Tip card composable.
 */
@Composable
private fun TipCard(tip: PlanningTip) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tip.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Coming soon card for Phase 3 features.
 */
@Composable
private fun ComingSoonCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Event template data class.
 */
private data class EventTemplate(
    val name: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * Planning tip data class.
 */
private data class PlanningTip(
    val title: String,
    val content: String
)

/**
 * Sample event templates.
 */
private val eventTemplates = listOf(
    EventTemplate(
        name = "Week-end entre amis",
        description = "Escapade de 2-3 jours avec hébergement et activités",
        icon = Icons.Default.Favorite
    ),
    EventTemplate(
        name = "Réunion de famille",
        description = "Rassemblement familial avec repas et photos",
        icon = Icons.Default.Face
    ),
    EventTemplate(
        name = "Voyage de groupe",
        description = "Séjour de plusieurs jours avec transport et budget partagé",
        icon = Icons.Default.DateRange
    ),
    EventTemplate(
        name = "Événement sportif",
        description = "Compétition ou sortie sportive collective",
        icon = Icons.Default.AccountCircle
    )
)

/**
 * Sample planning tips.
 */
private val planningTips = listOf(
    PlanningTip(
        title = "Proposez plusieurs créneaux",
        content = "Plus vous proposez de dates, plus il est facile de trouver un consensus."
    ),
    PlanningTip(
        title = "Définissez une échéance claire",
        content = "Une date limite de vote incite les participants à répondre rapidement."
    ),
    PlanningTip(
        title = "Créez des scénarios",
        content = "Comparez différentes options (destination, hébergement, activités) pour optimiser votre événement."
    ),
    PlanningTip(
        title = "Partagez le budget tôt",
        content = "Une transparence précoce sur les coûts évite les mauvaises surprises."
    )
)
