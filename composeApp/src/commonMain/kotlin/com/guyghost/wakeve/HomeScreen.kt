package com.guyghost.wakeve

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    events: List<Event>,
    userId: String,
    onCreateEvent: () -> Unit,
    onEventClick: (Event) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(HomeTab.All) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accueil") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Menu utilisateur")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateEvent,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Créer un événement")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab selector
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                HomeTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label) }
                    )
                }
            }

            // Events list
            val filteredEvents = when (selectedTab) {
                HomeTab.All -> events
                HomeTab.Upcoming -> events.filter { it.status == EventStatus.CONFIRMED || it.status == EventStatus.ORGANIZING }
                HomeTab.Past -> events.filter { it.status == EventStatus.FINALIZED }
            }

            if (filteredEvents.isEmpty()) {
                EmptyState(
                    tab = selectedTab,
                    onCreateEvent = onCreateEvent
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredEvents) { event ->
                        EventCard(
                            event = event,
                            onClick = { onEventClick(event) }
                        )
                    }
                }
            }
        }
    }

    if (showMenu) {
        UserMenu(
            onDismiss = { showMenu = false },
            onSignOut = onSignOut
        )
    }
}

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = event.description.take(100),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(status = event.status)
                Text(
                    text = formatDate(event.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusChip(
    status: EventStatus,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (status) {
        EventStatus.DRAFT -> "Brouillon" to androidx.compose.ui.graphics.Color(0xFF64748B)
        EventStatus.POLLING -> "En sondage" to androidx.compose.ui.graphics.Color(0xFF2563EB)
        EventStatus.COMPARING -> "Comparaison" to androidx.compose.ui.graphics.Color(0xFF8B5CF6)
        EventStatus.CONFIRMED -> "Confirmé" to androidx.compose.ui.graphics.Color(0xFF059669)
        EventStatus.ORGANIZING -> "Organisation" to androidx.compose.ui.graphics.Color(0xFFD97706)
        EventStatus.FINALIZED -> "Finalisé" to androidx.compose.ui.graphics.Color(0xFF7C3AED)
    }

    androidx.compose.material3.Surface(
        color = color.copy(alpha = 0.1f),
        modifier = modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
    ) {
        androidx.compose.material3.Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyState(
    tab: HomeTab,
    onCreateEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Event,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (tab) {
                HomeTab.All -> "Aucun événement"
                HomeTab.Upcoming -> "Aucun événement à venir"
                HomeTab.Past -> "Aucun événement passé"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Commencez par créer votre premier événement !",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateEvent,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Créer un événement")
        }
    }
}

@Composable
fun UserMenu(
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onSignOut) {
                Text("Se déconnecter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        title = { Text("Menu utilisateur") },
        text = { Text("Voulez-vous vous déconnecter ?") }
    )
}

enum class HomeTab(val label: String) {
    All("Tous"),
    Upcoming("À venir"),
    Past("Passés")
}

private fun formatDate(isoDate: String): String {
    return try {
        val date = kotlinx.datetime.Instant.parse(isoDate)
        val local = date.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        "${local.dayOfMonth} ${local.month.name.lowercase().take(3)}"
    } catch (e: Exception) {
        isoDate.take(10)
    }
}
