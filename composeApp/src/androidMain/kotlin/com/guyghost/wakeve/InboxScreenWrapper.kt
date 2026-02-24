package com.guyghost.wakeve

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.InboxItem
import com.guyghost.wakeve.models.InboxItemStatus
import com.guyghost.wakeve.models.InboxItemType
import com.guyghost.wakeve.ui.components.StatusIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Inbox filter types matching iOS implementation.
 */
enum class InboxFilterType(
    val label: String,
    val icon: ImageVector
) {
    ALL("Tout", Icons.Outlined.Inbox),
    FOCUSED("Focus", Icons.Filled.Star),
    UNREAD("Non lues", Icons.Outlined.MarkEmailRead),
    EVENT("Événement", Icons.Outlined.Event)
}

/**
 * InboxScreen - Material You inbox with filters, selection mode, and dynamic badge.
 *
 * Features:
 * - LargeTopAppBar with collapsing scroll
 * - FilterChip row (All, Focused, Unread, Event)
 * - LazyColumn with InboxItemRow using StatusIndicator
 * - Selection mode with batch actions (Mark Read, Mark Done)
 * - Pull-to-refresh
 * - Empty states per filter
 * - Dynamic unread count via onUnreadCountChanged callback
 *
 * @param userId Current user ID
 * @param onNotificationClick Callback when a notification item is tapped
 * @param onBack Back navigation callback
 * @param onNavigateToNotifications Navigate to notification preferences
 * @param onUnreadCountChanged Callback to report unread count to parent (for tab badge)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InboxScreen(
    userId: String,
    onNotificationClick: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToNotifications: (() -> Unit)? = null,
    onUnreadCountChanged: ((Int) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    // State
    var selectedFilter by remember { mutableStateOf(InboxFilterType.ALL) }
    val items = remember { mutableStateListOf<InboxItem>() }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var showEventSheet by remember { mutableStateOf(false) }
    var selectedEventFilter by remember { mutableStateOf<String?>(null) }

    // Derived state
    val filteredItems by remember(selectedFilter, selectedEventFilter) {
        derivedStateOf {
            when (selectedFilter) {
                InboxFilterType.ALL -> items.toList()
                InboxFilterType.FOCUSED -> items.filter { it.status == InboxItemStatus.ACTION_REQUIRED }
                InboxFilterType.UNREAD -> items.filter { !it.isRead }
                InboxFilterType.EVENT -> {
                    if (selectedEventFilter != null) {
                        items.filter { it.eventTitle == selectedEventFilter }
                    } else {
                        items.filter { it.eventTitle != null }
                    }
                }
            }
        }
    }

    val unreadCount by remember {
        derivedStateOf { items.count { !it.isRead } }
    }

    // Report unread count to parent
    LaunchedEffect(unreadCount) {
        onUnreadCountChanged?.invoke(unreadCount)
    }

    // Load sample data
    LaunchedEffect(Unit) {
        delay(500)
        items.addAll(getSampleInboxItems())
        isLoading = false
    }

    // Available events for the filter sheet
    val availableEvents by remember {
        derivedStateOf { items.mapNotNull { it.eventTitle }.distinct() }
    }

    // Actions
    fun markAllAsRead() {
        val updated = items.map { it.copy(isRead = true) }
        items.clear()
        items.addAll(updated)
    }

    fun markSelectedAsRead() {
        val updated = items.map { item ->
            if (selectedIds.contains(item.id)) item.copy(isRead = true) else item
        }
        items.clear()
        items.addAll(updated)
        selectedIds.clear()
        isSelectionMode = false
    }

    fun markSelectedAsDone() {
        items.removeAll { selectedIds.contains(it.id) }
        selectedIds.clear()
        isSelectionMode = false
    }

    fun handleItemTap(item: InboxItem) {
        if (isSelectionMode) {
            if (selectedIds.contains(item.id)) {
                selectedIds.remove(item.id)
                if (selectedIds.isEmpty()) isSelectionMode = false
            } else {
                selectedIds.add(item.id)
            }
        } else {
            // Mark as read on tap
            val index = items.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                items[index] = items[index].copy(isRead = true)
            }
            onNotificationClick(item.id)
        }
    }

    fun handleItemLongPress(item: InboxItem) {
        if (!isSelectionMode) {
            isSelectionMode = true
            selectedIds.clear()
        }
        if (!selectedIds.contains(item.id)) {
            selectedIds.add(item.id)
        }
    }

    fun refresh() {
        scope.launch {
            isRefreshing = true
            delay(1000)
            isRefreshing = false
        }
    }

    fun cancelSelection() {
        isSelectionMode = false
        selectedIds.clear()
    }

    // Event filter bottom sheet
    if (showEventSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showEventSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Filtrer par événement",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // "All events" option
                Surface(
                    onClick = {
                        selectedEventFilter = null
                        showEventSheet = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedEventFilter == null)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tous les événements",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                availableEvents.forEach { eventName ->
                    Surface(
                        onClick = {
                            selectedEventFilter = eventName
                            showEventSheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedEventFilter == eventName)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = eventName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isSelectionMode) "${selectedIds.size} sélectionné(s)" else "Inbox",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        TextButton(onClick = { cancelSelection() }) {
                            Text("Annuler")
                        }
                    } else {
                        if (unreadCount > 0) {
                            IconButton(onClick = { markAllAsRead() }) {
                                Icon(
                                    imageVector = Icons.Filled.DoneAll,
                                    contentDescription = "Tout marquer comme lu"
                                )
                            }
                        }
                        if (onNavigateToNotifications != null) {
                            IconButton(onClick = onNavigateToNotifications) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Préférences de notifications"
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            // Selection action bar
            AnimatedVisibility(
                visible = isSelectionMode && selectedIds.isNotEmpty(),
                enter = slideInVertically(
                    animationSpec = spring(dampingRatio = 0.8f),
                    initialOffsetY = { it }
                ),
                exit = slideOutVertically(
                    animationSpec = spring(dampingRatio = 0.8f),
                    targetOffsetY = { it }
                )
            ) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { markSelectedAsRead() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MarkEmailRead,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Marquer lu")
                        }
                        FilledTonalButton(
                            onClick = { markSelectedAsDone() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Terminé")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(InboxFilterType.entries.toList()) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = {
                            if (filter == InboxFilterType.EVENT) {
                                selectedFilter = filter
                                showEventSheet = true
                            } else {
                                selectedFilter = filter
                                selectedEventFilter = null
                            }
                            scope.launch { listState.animateScrollToItem(0) }
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(filter.label)
                                // Show unread count on the "Unread" chip
                                if (filter == InboxFilterType.UNREAD && unreadCount > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                // Show event name on the "Event" chip if filtered
                                if (filter == InboxFilterType.EVENT && selectedEventFilter != null) {
                                    Text(
                                        text = "· $selectedEventFilter",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = filter.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // Content
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    filteredItems.isEmpty() -> {
                        InboxEmptyState(filter = selectedFilter)
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = filteredItems,
                                key = { it.id }
                            ) { item ->
                                InboxItemRowMaterial(
                                    item = item,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedIds.contains(item.id),
                                    onClick = { handleItemTap(item) },
                                    onLongClick = { handleItemLongPress(item) }
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = if (isSelectionMode) 100.dp else 68.dp)
                                )
                            }

                            // Bottom spacing for bottom bar
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single inbox item row with Material You styling.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InboxItemRowMaterial(
    item: InboxItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            !item.isRead -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox in selection mode
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // Status indicator
            StatusIndicator(
                status = item.status,
                type = item.type,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                // Context line (event title)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = item.eventTitle ?: getDefaultContext(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Title
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Description
                item.description?.let { desc ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right side: timestamp and comment count
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatTimeAgo(item.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.commentCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (item.commentCount > 99) "99+" else item.commentCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get default context text based on item type.
 */
private fun getDefaultContext(item: InboxItem): String {
    return when (item.type) {
        InboxItemType.EVENT_INVITATION -> "Invitation"
        InboxItemType.POLL_UPDATE -> "Sondage"
        InboxItemType.VOTE_REMINDER -> "Rappel de vote"
        InboxItemType.EVENT_CONFIRMED -> "Confirmation"
        InboxItemType.PARTICIPANT_JOINED -> "Participant"
        InboxItemType.VOTE_SUBMITTED -> "Vote"
        InboxItemType.COMMENT_POSTED -> "Commentaire"
        InboxItemType.COMMENT_REPLY -> "Réponse"
        InboxItemType.BUDGET_UPDATE -> "Budget"
        InboxItemType.ACTIVITY_UPDATE -> "Activité"
        InboxItemType.ACCOMMODATION_UPDATE -> "Hébergement"
        InboxItemType.GENERAL -> "Notification"
    }
}

/**
 * Format timestamp to relative time.
 */
private fun formatTimeAgo(isoTimestamp: String): String {
    return try {
        val timestamp = kotlinx.datetime.Instant.parse(isoTimestamp)
        val now = kotlinx.datetime.Clock.System.now()
        val duration = now - timestamp
        val minutes = duration.inWholeMinutes
        val hours = duration.inWholeHours
        val days = duration.inWholeDays
        when {
            minutes < 1 -> "À l'instant"
            minutes < 60 -> "${minutes}min"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}j"
            days < 30 -> "${days / 7}sem"
            days < 365 -> "${days / 30}mois"
            else -> "${days / 365}an"
        }
    } catch (e: Exception) {
        isoTimestamp.take(10)
    }
}

/**
 * Empty state for inbox per filter.
 */
@Composable
private fun InboxEmptyState(
    filter: InboxFilterType,
    modifier: Modifier = Modifier
) {
    val (icon, title, subtitle) = when (filter) {
        InboxFilterType.ALL -> Triple(
            Icons.Outlined.Inbox,
            "Aucune notification",
            "Vous êtes à jour ! Les nouvelles notifications apparaîtront ici."
        )
        InboxFilterType.FOCUSED -> Triple(
            Icons.Filled.Star,
            "Rien en priorité",
            "Les notifications nécessitant une action apparaîtront ici."
        )
        InboxFilterType.UNREAD -> Triple(
            Icons.Outlined.MarkEmailRead,
            "Tout est lu",
            "Vous n'avez aucune notification non lue."
        )
        InboxFilterType.EVENT -> Triple(
            Icons.Outlined.Event,
            "Aucun événement",
            "Les notifications liées aux événements apparaîtront ici."
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Sample inbox items for testing.
 */
private fun getSampleInboxItems(): List<InboxItem> = listOf(
    InboxItem(
        id = "1",
        type = InboxItemType.EVENT_INVITATION,
        status = InboxItemStatus.ACTION_REQUIRED,
        title = "Week-end à la montagne",
        subtitle = "Marie vous invite",
        eventTitle = "Week-end ski 2024",
        timestamp = "2026-02-24T10:30:00Z",
        isRead = false,
        commentCount = 3
    ),
    InboxItem(
        id = "2",
        type = InboxItemType.POLL_UPDATE,
        status = InboxItemStatus.ACTION_REQUIRED,
        title = "Nouveau sondage de dates disponible",
        description = "3 créneaux proposés pour le week-end",
        eventTitle = "Anniversaire de Paul",
        timestamp = "2026-02-24T08:00:00Z",
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
        timestamp = "2026-02-23T18:00:00Z",
        isRead = false,
        commentCount = 0
    ),
    InboxItem(
        id = "4",
        type = InboxItemType.EVENT_CONFIRMED,
        status = InboxItemStatus.SUCCESS,
        title = "Date confirmée : 25 février",
        eventTitle = "Dîner d'équipe",
        timestamp = "2026-02-23T14:00:00Z",
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
        timestamp = "2026-02-23T12:30:00Z",
        isRead = true,
        commentCount = 1
    ),
    InboxItem(
        id = "6",
        type = InboxItemType.PARTICIPANT_JOINED,
        status = InboxItemStatus.INFO,
        title = "Sophie a rejoint l'événement",
        eventTitle = "Week-end ski 2024",
        timestamp = "2026-02-22T09:00:00Z",
        isRead = true,
        commentCount = 0
    ),
    InboxItem(
        id = "7",
        type = InboxItemType.BUDGET_UPDATE,
        status = InboxItemStatus.INFO,
        title = "Budget mis à jour : 150€/personne",
        eventTitle = "Week-end ski 2024",
        timestamp = "2026-02-21T16:00:00Z",
        isRead = true,
        commentCount = 2
    )
)
