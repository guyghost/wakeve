package com.guyghost.wakeve.ui.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.InboxFilter
import com.guyghost.wakeve.models.InboxItem
import com.guyghost.wakeve.models.InboxItemStatus
import com.guyghost.wakeve.models.InboxItemType
import com.guyghost.wakeve.ui.components.FilterChipRow
import com.guyghost.wakeve.ui.components.InboxItemRow
import kotlinx.coroutines.launch

/**
 * Wakeve Design System Colors
 */
private object InboxScreenColors {
    val Primary = Color(0xFF2563EB)
    val Surface = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFF0F172A)
    val OnSurfaceVariant = Color(0xFF475569)
    val Outline = Color(0xFFE2E8F0)
}

/**
 * Data class for empty state content
 */
private data class EmptyStateContent(
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

/**
 * InboxScreen - Full inbox screen with filters and notification list
 * 
 * Inspired by GitHub Mobile's Inbox screen:
 * - Large title that collapses on scroll
 * - Horizontal filter chips
 * - Pull-to-refresh
 * - Empty states per filter
 * 
 * Adapted to Wakeve's event notification system.
 * 
 * @param items List of inbox items to display
 * @param isLoading Whether the data is being loaded
 * @param isRefreshing Whether a refresh is in progress
 * @param onRefresh Callback to refresh the inbox
 * @param onItemClick Callback when an item is clicked
 * @param onMarkAllRead Callback to mark all as read
 * @param modifier Modifier for the component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    items: List<InboxItem>,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onItemClick: (InboxItem) -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(InboxFilter.ALL) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Filter items based on selected filter
    val filteredItems by remember(items, selectedFilter) {
        derivedStateOf {
            when (selectedFilter) {
                InboxFilter.ALL -> items
                InboxFilter.UNREAD -> items.filter { !it.isRead }
                InboxFilter.EVENTS -> items.filter { 
                    it.type in listOf(
                        InboxItemType.EVENT_INVITATION,
                        InboxItemType.EVENT_CONFIRMED,
                        InboxItemType.POLL_UPDATE,
                        InboxItemType.VOTE_REMINDER
                    )
                }
                InboxFilter.COMMENTS -> items.filter {
                    it.type in listOf(
                        InboxItemType.COMMENT_POSTED,
                        InboxItemType.COMMENT_REPLY
                    )
                }
                InboxFilter.ACTIONS -> items.filter {
                    it.status == InboxItemStatus.ACTION_REQUIRED
                }
            }
        }
    }
    
    // Unread count for badge
    val unreadCount by remember(items) {
        derivedStateOf { items.count { !it.isRead } }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            InboxTopAppBar(
                unreadCount = unreadCount,
                scrollBehavior = scrollBehavior,
                onMarkAllRead = onMarkAllRead,
                onRefresh = onRefresh
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            FilterChipRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { 
                    selectedFilter = it
                    // Scroll to top when filter changes
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            )
            
            // Divider
            HorizontalDivider(
                color = InboxScreenColors.Outline,
                thickness = 0.5.dp
            )
            
            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && items.isEmpty() -> {
                        // Initial loading
                        LoadingState()
                    }
                    filteredItems.isEmpty() -> {
                        // Empty state
                        EmptyInboxState(filter = selectedFilter)
                    }
                    else -> {
                        // Inbox list
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = filteredItems,
                                key = { it.id }
                            ) { item ->
                                InboxItemRow(
                                    item = item,
                                    onClick = { onItemClick(item) }
                                )
                                HorizontalDivider(
                                    color = InboxScreenColors.Outline,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 68.dp)
                                )
                            }
                            
                            // Bottom spacing
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
                
                // Pull to refresh indicator (simplified - full implementation needs platform-specific)
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .size(24.dp),
                        color = InboxScreenColors.Primary,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

/**
 * Top app bar for inbox
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxTopAppBar(
    unreadCount: Int,
    scrollBehavior: TopAppBarScrollBehavior,
    onMarkAllRead: () -> Unit,
    onRefresh: () -> Unit
) {
    LargeTopAppBar(
        title = {
            Text(
                text = "Notifications",
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            // Refresh button
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualiser"
                )
            }
            
            // Mark all as read
            if (unreadCount > 0) {
                TextButton(onClick = onMarkAllRead) {
                    Text(
                        text = "Tout lire",
                        color = InboxScreenColors.Primary
                    )
                }
            }
            
            // More options
            IconButton(onClick = { /* TODO: Options menu */ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Plus d'options"
                )
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = InboxScreenColors.Surface,
            scrolledContainerColor = InboxScreenColors.Surface
        )
    )
}

/**
 * Loading state
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = InboxScreenColors.Primary
        )
    }
}

/**
 * Get empty state content based on filter
 */
private fun getEmptyStateContent(filter: InboxFilter): EmptyStateContent {
    return when (filter) {
        InboxFilter.ALL -> EmptyStateContent(
            icon = Icons.Outlined.Inbox,
            title = "Aucune notification",
            subtitle = "Vous êtes à jour ! Les nouvelles notifications apparaîtront ici."
        )
        InboxFilter.UNREAD -> EmptyStateContent(
            icon = Icons.Outlined.Notifications,
            title = "Tout est lu",
            subtitle = "Vous n'avez aucune notification non lue."
        )
        InboxFilter.EVENTS -> EmptyStateContent(
            icon = Icons.Outlined.Notifications,
            title = "Aucun événement",
            subtitle = "Les notifications d'événements apparaîtront ici."
        )
        InboxFilter.COMMENTS -> EmptyStateContent(
            icon = Icons.Outlined.Notifications,
            title = "Aucun commentaire",
            subtitle = "Les réponses et commentaires apparaîtront ici."
        )
        InboxFilter.ACTIONS -> EmptyStateContent(
            icon = Icons.Outlined.NotificationsOff,
            title = "Rien à faire",
            subtitle = "Aucune action n'est requise pour le moment."
        )
    }
}

/**
 * Empty state for inbox
 */
@Composable
private fun EmptyInboxState(
    filter: InboxFilter,
    modifier: Modifier = Modifier
) {
    val content = getEmptyStateContent(filter)
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = content.icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = InboxScreenColors.OnSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = content.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = InboxScreenColors.OnSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = content.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = InboxScreenColors.OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================================
// PREVIEW DATA - For testing and previews
// ============================================================================

/**
 * Sample data for previews and testing
 */
object InboxSampleData {
    val sampleItems = listOf(
        InboxItem(
            id = "1",
            type = InboxItemType.EVENT_INVITATION,
            status = InboxItemStatus.ACTION_REQUIRED,
            title = "Week-end à la montagne",
            subtitle = "Marie vous invite",
            eventTitle = "Invitation",
            timestamp = "2025-01-15T10:30:00Z",
            isRead = false,
            commentCount = 3
        ),
        InboxItem(
            id = "2",
            type = InboxItemType.POLL_UPDATE,
            status = InboxItemStatus.ACTION_REQUIRED,
            title = "Nouveau sondage de dates disponible",
            eventTitle = "Anniversaire de Paul",
            timestamp = "2025-01-15T08:00:00Z",
            isRead = false,
            commentCount = 0
        ),
        InboxItem(
            id = "3",
            type = InboxItemType.VOTE_REMINDER,
            status = InboxItemStatus.WARNING,
            title = "N'oubliez pas de voter !",
            description = "Le sondage se termine dans 2 jours",
            eventTitle = "Soirée jeux",
            timestamp = "2025-01-14T18:00:00Z",
            isRead = false,
            commentCount = 0
        ),
        InboxItem(
            id = "4",
            type = InboxItemType.EVENT_CONFIRMED,
            status = InboxItemStatus.SUCCESS,
            title = "Date confirmée : 25 janvier",
            eventTitle = "Dîner d'équipe",
            timestamp = "2025-01-14T14:00:00Z",
            isRead = true,
            commentCount = 5
        ),
        InboxItem(
            id = "5",
            type = InboxItemType.COMMENT_REPLY,
            status = InboxItemStatus.INFO,
            title = "Thomas a répondu à votre commentaire",
            description = "\"Super idée pour le restaurant !\"",
            eventTitle = "Dîner d'équipe",
            timestamp = "2025-01-14T12:30:00Z",
            isRead = true,
            commentCount = 1
        ),
        InboxItem(
            id = "6",
            type = InboxItemType.PARTICIPANT_JOINED,
            status = InboxItemStatus.INFO,
            title = "Sophie a rejoint l'événement",
            eventTitle = "Week-end à la montagne",
            timestamp = "2025-01-13T09:00:00Z",
            isRead = true,
            commentCount = 0
        ),
        InboxItem(
            id = "7",
            type = InboxItemType.BUDGET_UPDATE,
            status = InboxItemStatus.INFO,
            title = "Budget mis à jour : 150€/personne",
            eventTitle = "Week-end à la montagne",
            timestamp = "2025-01-12T16:00:00Z",
            isRead = true,
            commentCount = 2
        )
    )
}
