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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.guyghost.wakeve.models.InboxItem
import com.guyghost.wakeve.models.InboxItemStatus
import com.guyghost.wakeve.models.InboxItemType
import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.NotificationType
import com.guyghost.wakeve.notification.NotificationService
import com.guyghost.wakeve.notification.resolveNotificationClickTarget
import com.guyghost.wakeve.ui.components.StatusIndicator
import kotlinx.datetime.Clock
import kotlinx.coroutines.launch

/**
 * Inbox filter types matching iOS implementation.
 */
enum class InboxFilterType(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    ALL(R.string.inbox_filter_all, Icons.Outlined.Inbox),
    FOCUSED(R.string.inbox_filter_focused, Icons.Filled.Star),
    UNREAD(R.string.inbox_filter_unread, Icons.Outlined.MarkEmailRead),
    EVENT(R.string.inbox_filter_event, Icons.Outlined.Event)
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
    notificationService: NotificationService? = null,
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

    suspend fun loadInboxItems(showRefreshing: Boolean = false) {
        if (showRefreshing) {
            isRefreshing = true
        } else {
            isLoading = true
        }

        val loadedItems = notificationService
            ?.getNotifications(userId)
            ?.map(NotificationMessage::toInboxItem)
            .orEmpty()

        items.clear()
        items.addAll(loadedItems)
        isLoading = false
        isRefreshing = false
    }

    // Load persisted inbox notifications.
    LaunchedEffect(notificationService, userId) {
        loadInboxItems()
    }

    // Available events for the filter sheet
    val availableEvents by remember {
        derivedStateOf { items.mapNotNull { it.eventTitle }.distinct() }
    }

    // Actions
    fun markAllAsRead() {
        scope.launch {
            if (notificationService != null) {
                notificationService.markAllAsRead(userId)
                loadInboxItems()
            } else {
                val updated = items.map { it.copy(isRead = true) }
                items.clear()
                items.addAll(updated)
            }
        }
    }

    fun markSelectedAsRead() {
        val idsToMark = selectedIds.toList()
        scope.launch {
            if (notificationService != null) {
                idsToMark.forEach { notificationService.markAsReadForUser(it, userId) }
                loadInboxItems()
            } else {
                val updated = items.map { item ->
                    if (idsToMark.contains(item.id)) item.copy(isRead = true) else item
                }
                items.clear()
                items.addAll(updated)
            }
            selectedIds.clear()
            isSelectionMode = false
        }
    }

    fun markSelectedAsDone() {
        val idsToDelete = selectedIds.toList()
        scope.launch {
            if (notificationService != null) {
                idsToDelete.forEach { notificationService.deleteNotificationForUser(it, userId) }
                loadInboxItems()
            } else {
                items.removeAll { idsToDelete.contains(it.id) }
            }
            selectedIds.clear()
            isSelectionMode = false
        }
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
            scope.launch {
                if (notificationService != null && !item.isRead) {
                    notificationService.markAsReadForUser(item.id, userId)
                    loadInboxItems()
                } else {
                    val index = items.indexOfFirst { it.id == item.id }
                    if (index >= 0) {
                        items[index] = items[index].copy(isRead = true)
                    }
                }
            }
            (resolveNotificationClickTarget(item.metadata) ?: item.eventId)?.let(onNotificationClick)
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
            loadInboxItems(showRefreshing = true)
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
                    text = stringResource(R.string.inbox_filter_by_event),
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
                        text = stringResource(R.string.inbox_all_events),
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isSelectionMode) {
                                pluralStringResource(
                                    R.plurals.inbox_selected_count,
                                    selectedIds.size,
                                    selectedIds.size,
                                )
                            } else {
                                stringResource(R.string.notifications)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        TextButton(onClick = { cancelSelection() }) {
                            Text(stringResource(R.string.cancel))
                        }
                    } else {
                        if (unreadCount > 0) {
                            IconButton(onClick = { markAllAsRead() }) {
                                Icon(
                                    imageVector = Icons.Filled.DoneAll,
                                    contentDescription = stringResource(R.string.a11y_mark_all_notifications_read)
                                )
                            }
                        }
                        if (onNavigateToNotifications != null) {
                            IconButton(onClick = onNavigateToNotifications) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.a11y_open_notification_preferences)
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
                            Text(stringResource(R.string.inbox_mark_read))
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
                            Text(stringResource(R.string.inbox_mark_done))
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
                                Text(stringResource(filter.labelRes))
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
                                        text = stringResource(R.string.inbox_event_filter_value, selectedEventFilter.orEmpty()),
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
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
@Composable
private fun getDefaultContext(item: InboxItem): String {
    return when (item.type) {
        InboxItemType.EVENT_INVITATION -> stringResource(R.string.inbox_context_invitation)
        InboxItemType.POLL_UPDATE -> stringResource(R.string.inbox_context_poll)
        InboxItemType.VOTE_REMINDER -> stringResource(R.string.inbox_context_vote_reminder)
        InboxItemType.EVENT_CONFIRMED -> stringResource(R.string.inbox_context_confirmation)
        InboxItemType.PARTICIPANT_JOINED -> stringResource(R.string.inbox_context_participant)
        InboxItemType.VOTE_SUBMITTED -> stringResource(R.string.inbox_context_vote)
        InboxItemType.COMMENT_POSTED -> stringResource(R.string.inbox_context_comment)
        InboxItemType.COMMENT_REPLY -> stringResource(R.string.inbox_context_reply)
        InboxItemType.BUDGET_UPDATE -> stringResource(R.string.inbox_context_budget)
        InboxItemType.ACTIVITY_UPDATE -> stringResource(R.string.inbox_context_activity)
        InboxItemType.ACCOMMODATION_UPDATE -> stringResource(R.string.inbox_context_accommodation)
        InboxItemType.GENERAL -> stringResource(R.string.inbox_context_notification)
    }
}

/**
 * Format timestamp to relative time.
 */
@Composable
private fun formatTimeAgo(isoTimestamp: String): String {
    val duration = runCatching {
        val timestamp = kotlinx.datetime.Instant.parse(isoTimestamp)
        val now = kotlinx.datetime.Clock.System.now()
        now - timestamp
    }.getOrNull() ?: return isoTimestamp.take(10)
    val minutes = duration.inWholeMinutes
    val hours = duration.inWholeHours
    val days = duration.inWholeDays
    return when {
        minutes < 1 -> stringResource(R.string.relative_time_now)
        minutes < 60 -> stringResource(R.string.relative_time_minutes, minutes)
        hours < 24 -> stringResource(R.string.relative_time_hours, hours)
        days < 7 -> stringResource(R.string.relative_time_days, days)
        days < 30 -> stringResource(R.string.relative_time_weeks, days / 7)
        days < 365 -> stringResource(R.string.relative_time_months, days / 30)
        else -> stringResource(R.string.relative_time_years, days / 365)
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
    val (icon, titleRes, subtitleRes) = when (filter) {
        InboxFilterType.ALL -> Triple(
            Icons.Outlined.Inbox,
            R.string.inbox_empty_all_title,
            R.string.inbox_empty_all_body
        )
        InboxFilterType.FOCUSED -> Triple(
            Icons.Filled.Star,
            R.string.inbox_empty_focused_title,
            R.string.inbox_empty_focused_body
        )
        InboxFilterType.UNREAD -> Triple(
            Icons.Outlined.MarkEmailRead,
            R.string.inbox_empty_unread_title,
            R.string.inbox_empty_unread_body
        )
        InboxFilterType.EVENT -> Triple(
            Icons.Outlined.Event,
            R.string.inbox_empty_event_title,
            R.string.inbox_empty_event_body
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
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(subtitleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun NotificationMessage.toInboxItem(): InboxItem =
    InboxItem(
        id = id,
        type = type.toInboxType(),
        status = type.toInboxStatus(),
        title = title,
        description = body,
        eventId = data["eventId"],
        eventTitle = data["eventTitle"],
        timestamp = sentAt ?: Clock.System.now().toString(),
        isRead = readAt != null,
        commentCount = data["commentCount"]?.toIntOrNull() ?: 0,
        metadata = data
    )

private fun NotificationType.toInboxType(): InboxItemType =
    when (this) {
        NotificationType.DEADLINE_REMINDER -> InboxItemType.VOTE_REMINDER
        NotificationType.EVENT_UPDATE -> InboxItemType.POLL_UPDATE
        NotificationType.VOTE_CLOSE_REMINDER -> InboxItemType.VOTE_REMINDER
        NotificationType.EVENT_CONFIRMED -> InboxItemType.EVENT_CONFIRMED
        NotificationType.PARTICIPANT_JOINED -> InboxItemType.PARTICIPANT_JOINED
        NotificationType.VOTE_SUBMITTED -> InboxItemType.VOTE_SUBMITTED
        NotificationType.COMMENT_POSTED -> InboxItemType.COMMENT_POSTED
        NotificationType.COMMENT_REPLY -> InboxItemType.COMMENT_REPLY
        NotificationType.MENTION -> InboxItemType.COMMENT_REPLY
    }

private fun NotificationType.toInboxStatus(): InboxItemStatus =
    when (this) {
        NotificationType.DEADLINE_REMINDER,
        NotificationType.VOTE_CLOSE_REMINDER -> InboxItemStatus.WARNING
        NotificationType.EVENT_CONFIRMED -> InboxItemStatus.SUCCESS
        NotificationType.EVENT_UPDATE,
        NotificationType.PARTICIPANT_JOINED,
        NotificationType.VOTE_SUBMITTED,
        NotificationType.COMMENT_POSTED,
        NotificationType.COMMENT_REPLY,
        NotificationType.MENTION -> InboxItemStatus.INFO
    }
