package com.guyghost.wakeve

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.viewmodel.EventManagementViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

/**
 * Events Tab Screen - Dedicated events list with segmented filters.
 * 
 * Displays events in three categories:
 * - À venir (Upcoming): Events in the future
 * - En cours (Ongoing): Events happening now
 * - Passés (Past): Events that have ended
 * 
 * Matches iOS EventsTabView with Material You design.
 * 
 * @param userId The current authenticated user ID
 * @param onEventClick Callback when user clicks on an event
 * @param onCreateEvent Callback when user wants to create a new event
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsTabScreen(
    userId: String,
    onEventClick: (Event) -> Unit,
    onCreateEvent: () -> Unit
) {
    val viewModel: EventManagementViewModel = koinInject()
    val state by viewModel.state.collectAsState()
    
    // Load events on first composition
    LaunchedEffect(Unit) {
        viewModel.dispatch(EventManagementContract.Intent.LoadEvents)
    }
    
    // Filter state
    var selectedFilter by remember { mutableStateOf(EventFilter.UPCOMING) }
    
    // Filter events based on selected filter
    val filteredEvents = remember(state.events, selectedFilter) {
        val now = Clock.System.now()
        when (selectedFilter) {
            EventFilter.UPCOMING -> state.events.filter { event ->
                // Events that haven't started yet or are in draft/polling
                event.status == EventStatus.DRAFT || 
                event.status == EventStatus.POLLING ||
                (event.status == EventStatus.CONFIRMED && isEventUpcoming(event, now))
            }
            EventFilter.ONGOING -> state.events.filter { event ->
                // Events that are confirmed and happening now
                event.status == EventStatus.CONFIRMED && isEventOngoing(event, now)
            }
            EventFilter.PAST -> state.events.filter { event ->
                // Events that have ended
                event.status == EventStatus.CONFIRMED && isEventPast(event, now)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Événements",
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateEvent,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Créer un événement"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Segmented filter tabs
            TabRow(
                selectedTabIndex = selectedFilter.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                EventFilter.entries.forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        text = {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    )
                }
            }
            
            // Events list
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                filteredEvents.isEmpty() -> {
                    EmptyState(
                        filter = selectedFilter,
                        onCreateEvent = onCreateEvent
                    )
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredEvents, key = { it.id }) { event ->
                            EventListItem(
                                event = event,
                                onClick = { onEventClick(event) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Event filter categories.
 */
private enum class EventFilter(val label: String) {
    UPCOMING("À venir"),
    ONGOING("En cours"),
    PAST("Passés")
}

/**
 * Empty state for each filter category.
 */
@Composable
private fun EmptyState(
    filter: EventFilter,
    onCreateEvent: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when (filter) {
                    EventFilter.UPCOMING -> "Aucun événement à venir"
                    EventFilter.ONGOING -> "Aucun événement en cours"
                    EventFilter.PAST -> "Aucun événement passé"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            
            if (filter == EventFilter.UPCOMING) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onCreateEvent) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Créer un événement")
                }
            }
        }
    }
}

/**
 * Event list item card.
 */
@Composable
private fun EventListItem(
    event: Event,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                // Status badge
                Surface(
                    color = when (event.status) {
                        EventStatus.DRAFT -> MaterialTheme.colorScheme.tertiaryContainer
                        EventStatus.POLLING -> MaterialTheme.colorScheme.secondaryContainer
                        EventStatus.CONFIRMED -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when (event.status) {
                            EventStatus.DRAFT -> "Brouillon"
                            EventStatus.POLLING -> "Vote en cours"
                            EventStatus.CONFIRMED -> "Confirmé"
                            else -> "Inconnu"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (event.status) {
                            EventStatus.DRAFT -> MaterialTheme.colorScheme.onTertiaryContainer
                            EventStatus.POLLING -> MaterialTheme.colorScheme.onSecondaryContainer
                            EventStatus.CONFIRMED -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // Description
            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            
            // Metadata
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${event.participants.size} participants",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${event.proposedSlots.size} créneaux",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Helper function to determine if an event is upcoming.
 */
private fun isEventUpcoming(event: Event, now: Instant): Boolean {
    // TODO: Parse event.confirmedSlot and compare with now
    // For now, return true if status is CONFIRMED
    return true
}

/**
 * Helper function to determine if an event is ongoing.
 */
private fun isEventOngoing(event: Event, now: Instant): Boolean {
    // TODO: Parse event.confirmedSlot and check if now is within event timeframe
    return false
}

/**
 * Helper function to determine if an event is past.
 */
private fun isEventPast(event: Event, now: Instant): Boolean {
    // TODO: Parse event.confirmedSlot and check if event has ended
    return false
}
