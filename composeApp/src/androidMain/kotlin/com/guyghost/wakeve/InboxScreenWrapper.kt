package com.guyghost.wakeve

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.InboxItem

/**
 * InboxScreen wrapper for bottom navigation tab.
 * 
 * Displays three categories with filters:
 * - Tâches (Tasks): Actions à faire (voter, valider, répondre)
 * - Messages (Messages): Commentaires et discussions
 * - Notifications: Mises à jour d'événements
 * 
 * @param userId The current authenticated user ID
 * @param onNotificationClick Callback when notification is clicked
 * @param onBack Callback for back navigation (unused for main tab)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    userId: String,
    onNotificationClick: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToNotifications: (() -> Unit)? = null
) {
    var selectedFilter by remember { mutableStateOf(InboxFilterType.ALL) }
    val items = remember { getSampleInboxItems() }
    
    val filteredItems = remember(items, selectedFilter) {
        when (selectedFilter) {
            InboxFilterType.ALL -> items
            InboxFilterType.TASKS -> items.filter { categorizeInboxItem(it.type) == InboxFilterType.TASKS }
            InboxFilterType.MESSAGES -> items.filter { categorizeInboxItem(it.type) == InboxFilterType.MESSAGES }
            InboxFilterType.NOTIFICATIONS -> items.filter { categorizeInboxItem(it.type) == InboxFilterType.NOTIFICATIONS }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inbox",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    if (onNavigateToNotifications != null) {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Preferences de notifications"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter tabs
            TabRow(
                selectedTabIndex = selectedFilter.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                InboxFilterType.entries.forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = filter.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    )
                }
            }
            
            // Content
            when {
                filteredItems.isEmpty() -> {
                    EmptyInboxState(filter = selectedFilter)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            InboxItemCard(
                                item = item,
                                onClick = { onNotificationClick(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Inbox filter types for the UI.
 */
enum class InboxFilterType(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ALL("Tout", Icons.Default.Notifications),
    TASKS("Tâches", Icons.Default.CheckCircle),
    MESSAGES("Messages", Icons.Default.Email),
    NOTIFICATIONS("Notifications", Icons.Default.Info)
}

/**
 * Helper to categorize inbox items by filter type.
 */
private fun categorizeInboxItem(type: com.guyghost.wakeve.models.InboxItemType): InboxFilterType {
    return when (type) {
        com.guyghost.wakeve.models.InboxItemType.EVENT_INVITATION,
        com.guyghost.wakeve.models.InboxItemType.VOTE_REMINDER -> InboxFilterType.TASKS
        
        com.guyghost.wakeve.models.InboxItemType.COMMENT_POSTED,
        com.guyghost.wakeve.models.InboxItemType.COMMENT_REPLY -> InboxFilterType.MESSAGES
        
        else -> InboxFilterType.NOTIFICATIONS
    }
}

/**
 * Empty state for each filter.
 */
@Composable
private fun EmptyInboxState(filter: InboxFilterType) {
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
            Icon(
                imageVector = when (filter) {
                    InboxFilterType.ALL -> Icons.Default.CheckCircle
                    InboxFilterType.TASKS -> Icons.Default.CheckCircle
                    InboxFilterType.MESSAGES -> Icons.Default.Email
                    InboxFilterType.NOTIFICATIONS -> Icons.Default.Info
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when (filter) {
                    InboxFilterType.ALL -> "Aucun élément"
                    InboxFilterType.TASKS -> "Aucune tâche"
                    InboxFilterType.MESSAGES -> "Aucun message"
                    InboxFilterType.NOTIFICATIONS -> "Aucune notification"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (filter) {
                    InboxFilterType.ALL -> "Vous êtes à jour !"
                    InboxFilterType.TASKS -> "Pas de tâches en attente"
                    InboxFilterType.MESSAGES -> "Pas de nouveaux messages"
                    InboxFilterType.NOTIFICATIONS -> "Pas de nouvelles notifications"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * Inbox item card.
 */
@Composable
private fun InboxItemCard(
    item: InboxItem,
    onClick: () -> Unit
) {
    val itemCategory = categorizeInboxItem(item.type)
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Icon(
                imageVector = when (itemCategory) {
                    InboxFilterType.TASKS -> Icons.Default.CheckCircle
                    InboxFilterType.MESSAGES -> Icons.Default.Email
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when (itemCategory) {
                    InboxFilterType.TASKS -> MaterialTheme.colorScheme.primary
                    InboxFilterType.MESSAGES -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.tertiary
                }
            )
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold
                    )
                )
                item.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                Text(
                    text = formatTimeAgo(item.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            // Unread indicator
            if (!item.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxSize()
                    ) {}
                }
            }
        }
    }
}

/**
 * Format timestamp to "il y a X" format.
 */
private fun formatTimeAgo(timestamp: String): String {
    // TODO: Implement proper time ago formatting
    return "récemment"
}

/**
 * Sample inbox items for testing.
 */
private fun getSampleInboxItems(): List<InboxItem> = listOf(
    InboxItem(
        id = "1",
        type = com.guyghost.wakeve.models.InboxItemType.VOTE_REMINDER,
        status = com.guyghost.wakeve.models.InboxItemStatus.ACTION_REQUIRED,
        title = "Voter sur le sondage",
        description = "Le sondage pour \"Week-end ski 2024\" attend votre vote",
        eventId = "event1",
        eventTitle = "Week-end ski 2024",
        timestamp = "2024-01-31T10:00:00Z",
        isRead = false
    ),
    InboxItem(
        id = "2",
        type = com.guyghost.wakeve.models.InboxItemType.COMMENT_POSTED,
        status = com.guyghost.wakeve.models.InboxItemStatus.INFO,
        title = "Nouveau message de Alice",
        description = "Alice a commenté sur \"Voyage Espagne\"",
        eventId = "event2",
        eventTitle = "Voyage Espagne",
        timestamp = "2024-01-31T08:00:00Z",
        isRead = false
    ),
    InboxItem(
        id = "3",
        type = com.guyghost.wakeve.models.InboxItemType.EVENT_CONFIRMED,
        status = com.guyghost.wakeve.models.InboxItemStatus.SUCCESS,
        title = "Date confirmée",
        description = "La date de \"Réunion famille\" a été confirmée au 15 janvier",
        eventId = "event3",
        eventTitle = "Réunion famille",
        timestamp = "2024-01-30T10:00:00Z",
        isRead = true
    ),
    InboxItem(
        id = "4",
        type = com.guyghost.wakeve.models.InboxItemType.BUDGET_UPDATE,
        status = com.guyghost.wakeve.models.InboxItemStatus.ACTION_REQUIRED,
        title = "Valider le budget",
        description = "Le budget de \"Week-end montagne\" attend votre validation",
        eventId = "event4",
        eventTitle = "Week-end montagne",
        timestamp = "2024-01-29T10:00:00Z",
        isRead = false
    ),
    InboxItem(
        id = "5",
        type = com.guyghost.wakeve.models.InboxItemType.PARTICIPANT_JOINED,
        status = com.guyghost.wakeve.models.InboxItemStatus.INFO,
        title = "Nouveau participant",
        description = "Bob a rejoint l'événement \"Voyage Espagne\"",
        eventId = "event2",
        eventTitle = "Voyage Espagne",
        timestamp = "2024-01-28T10:00:00Z",
        isRead = true
    )
)
