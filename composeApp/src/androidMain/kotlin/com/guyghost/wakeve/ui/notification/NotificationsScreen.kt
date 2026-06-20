package com.guyghost.wakeve.ui.notification

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.compose.ui.res.stringResource
import com.guyghost.wakeve.R
import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.NotificationType
import com.guyghost.wakeve.notification.NotificationService
import com.guyghost.wakeve.notification.resolveNotificationClickTarget
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

// MARK: - Models

enum class NotificationDateGroup(val labelRes: Int) {
    TODAY(R.string.notification_date_today),
    YESTERDAY(R.string.notification_date_yesterday),
    THIS_WEEK(R.string.notification_date_this_week),
    OLDER(R.string.notification_date_older)
}

enum class NotificationItemType(
    val icon: ImageVector,
    val color: Color
) {
    VOTE(Icons.Filled.HowToVote, Color(0xFF2563EB)),
    COMMENT(Icons.Filled.ChatBubble, Color(0xFF16A34A)),
    STATUS_CHANGE(Icons.Filled.SwapHoriz, Color(0xFFF97316)),
    DEADLINE(Icons.Filled.AccessTime, Color(0xFFDC2626)),
    REMINDER(Icons.Filled.Notifications, Color(0xFF9333EA)),
    INVITE(Icons.Filled.Email, Color(0xFF4F46E5)),
    EVENT_UPDATE(Icons.Filled.CalendarMonth, Color(0xFF6B7280))
}

data class NotificationItem(
    val id: String,
    val type: NotificationItemType,
    val title: String,
    val body: String,
    val eventId: String? = null,
    val clickTarget: String? = eventId,
    val isRead: Boolean = false,
    val createdAt: Date = Date()
) {
    val dateGroup: NotificationDateGroup
        get() {
            val cal = Calendar.getInstance()
            val today = Calendar.getInstance()
            cal.time = createdAt

            return when {
                cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> NotificationDateGroup.TODAY
                cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> NotificationDateGroup.YESTERDAY
                System.currentTimeMillis() - createdAt.time < TimeUnit.DAYS.toMillis(7) -> NotificationDateGroup.THIS_WEEK
                else -> NotificationDateGroup.OLDER
            }
        }

    val relativeTimestamp: String
        get() {
            val diff = System.currentTimeMillis() - createdAt.time
            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "A l'instant"
                diff < TimeUnit.HOURS.toMillis(1) -> "Il y a ${TimeUnit.MILLISECONDS.toMinutes(diff)} min"
                diff < TimeUnit.DAYS.toMillis(1) -> "Il y a ${TimeUnit.MILLISECONDS.toHours(diff)}h"
                diff < TimeUnit.DAYS.toMillis(7) -> "Il y a ${TimeUnit.MILLISECONDS.toDays(diff)}j"
                else -> {
                    SimpleDateFormat("dd/MM", Locale.FRANCE).format(createdAt)
                }
            }
    }
}

enum class NotificationInboxFilter {
    ALL,
    UNREAD
}

internal fun parseNotificationInboxFilter(value: String?): NotificationInboxFilter {
    return when (value?.trim()?.lowercase()) {
        "unread" -> NotificationInboxFilter.UNREAD
        else -> NotificationInboxFilter.ALL
    }
}

internal fun filterNotificationItems(
    notifications: List<NotificationItem>,
    filter: NotificationInboxFilter
): List<NotificationItem> {
    return when (filter) {
        NotificationInboxFilter.ALL -> notifications
        NotificationInboxFilter.UNREAD -> notifications.filter { !it.isRead }
    }
}

internal fun notificationInboxErrorMessage(error: Throwable? = null): String {
    return "Impossible de charger les notifications."
}

// MARK: - Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNotificationClick: (String) -> Unit = {},
    userId: String? = null,
    notificationService: NotificationService? = null,
    initialFilter: NotificationInboxFilter = NotificationInboxFilter.ALL,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadErrorMessage by remember { mutableStateOf<String?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun loadNotifications() {
        if (notificationService == null || userId.isNullOrBlank()) return
        scope.launch {
            isLoading = true
            loadErrorMessage = null
            try {
                notifications = notificationService.getNotifications(userId)
                    .map(NotificationMessage::toNotificationItem)
            } catch (e: Exception) {
                loadErrorMessage = notificationInboxErrorMessage(e)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(notificationService, userId) {
        loadNotifications()
    }

    val unreadCount = remember(notifications) {
        notifications.count { !it.isRead }
    }

    val visibleNotifications = remember(notifications, initialFilter) {
        filterNotificationItems(notifications, initialFilter)
    }

    val groupedNotifications = remember(visibleNotifications) {
        visibleNotifications
            .sortedByDescending { it.createdAt }
            .groupBy { it.dateGroup }
            .toSortedMap(compareBy { it.ordinal })
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.notifications),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = {
                            if (notificationService != null && !userId.isNullOrBlank()) {
                                scope.launch {
                                    val result = notificationService.markAllAsRead(userId)
                                    if (result.isSuccess) {
                                        loadNotifications()
                                    } else {
                                        loadErrorMessage = notificationInboxErrorMessage(result.exceptionOrNull())
                                    }
                                }
                            } else {
                                notifications = notifications.map { it.copy(isRead = true) }
                            }
                        }) {
                            Text(
                                text = stringResource(R.string.mark_all_read),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(onClick = onNavigateToPreferences) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.prefs_title)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                loadErrorMessage != null && notifications.isEmpty() -> {
                    NotificationErrorState(
                        message = loadErrorMessage.orEmpty(),
                        onRetry = { loadNotifications() }
                    )
                }

                visibleNotifications.isEmpty() -> {
                    EmptyNotificationsState()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        groupedNotifications.forEach { (group, items) ->
                            item(key = "header-${group.name}") {
                                Text(
                                    text = stringResource(group.labelRes),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        start = 16.dp, end = 16.dp,
                                        top = 16.dp, bottom = 8.dp
                                    )
                                )
                            }

                            items(
                                items = items,
                                key = { it.id }
                            ) { notification ->
                                SwipeToDismissNotificationRow(
                                    notification = notification,
                                    onMarkAsRead = {
                                        if (notificationService != null && !userId.isNullOrBlank()) {
                                            scope.launch {
                                                val result = notificationService.markAsReadForUser(notification.id, userId)
                                                if (result.isSuccess) {
                                                    loadNotifications()
                                                } else {
                                                    loadErrorMessage = notificationInboxErrorMessage(result.exceptionOrNull())
                                                }
                                            }
                                        } else {
                                            notifications = notifications.map {
                                                if (it.id == notification.id) it.copy(isRead = true) else it
                                            }
                                        }
                                    },
                                    onDelete = {
                                        if (notificationService != null && !userId.isNullOrBlank()) {
                                            scope.launch {
                                                val result = notificationService.deleteNotificationForUser(notification.id, userId)
                                                if (result.isSuccess) {
                                                    loadNotifications()
                                                } else {
                                                    loadErrorMessage = notificationInboxErrorMessage(result.exceptionOrNull())
                                                }
                                            }
                                        } else {
                                            notifications = notifications.filter { it.id != notification.id }
                                        }
                                    },
                                    onClick = {
                                        if (!notification.isRead && notificationService != null && !userId.isNullOrBlank()) {
                                            scope.launch {
                                                val result = notificationService.markAsReadForUser(notification.id, userId)
                                                if (result.isSuccess) {
                                                    loadNotifications()
                                                } else {
                                                    loadErrorMessage = notificationInboxErrorMessage(result.exceptionOrNull())
                                                }
                                            }
                                        }
                                        notification.clickTarget?.let(onNotificationClick)
                                    }
                                )
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(start = 72.dp)
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// MARK: - Swipe to Dismiss Row

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissNotificationRow(
    notification: NotificationItem,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onMarkAsRead()
                dismissState.reset()
            }
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
            }
            SwipeToDismissBoxValue.Settled -> { /* nothing */ }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection

            val backgroundColor by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF16A34A)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFDC2626)
                    else -> Color.Transparent
                },
                label = "swipe_bg"
            )

            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.MarkEmailRead
                SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
                else -> Icons.Filled.DoneAll
            }

            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        },
        content = {
            NotificationRow(
                notification = notification,
                onClick = onClick
            )
        }
    )
}

// MARK: - Notification Row

@Composable
private fun NotificationRow(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (!notification.isRead)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                else
                    MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(notification.type.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = notification.type.icon,
                contentDescription = null,
                tint = notification.type.color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (!notification.isRead) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = notification.relativeTimestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Unread indicator
        if (!notification.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// MARK: - Empty State

@Composable
private fun EmptyNotificationsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.NotificationsOff,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.no_notifications),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.notification_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NotificationErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.NotificationsOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onRetry) {
            Text(text = "Réessayer")
        }
    }
}

private fun NotificationMessage.toNotificationItem(): NotificationItem =
    NotificationItem(
        id = id,
        type = type.toItemType(),
        title = title,
        body = body,
        eventId = data["eventId"] ?: data["event_id"],
        clickTarget = resolveNotificationClickTarget(data),
        isRead = readAt != null,
        createdAt = sentAt.toDateOrNow()
    )

private fun NotificationType.toItemType(): NotificationItemType =
    when (this) {
        NotificationType.VOTE_SUBMITTED,
        NotificationType.VOTE_CLOSE_REMINDER -> NotificationItemType.VOTE
        NotificationType.COMMENT_POSTED,
        NotificationType.COMMENT_REPLY,
        NotificationType.MENTION -> NotificationItemType.COMMENT
        NotificationType.EVENT_CONFIRMED,
        NotificationType.PARTICIPANT_JOINED -> NotificationItemType.STATUS_CHANGE
        NotificationType.DEADLINE_REMINDER -> NotificationItemType.DEADLINE
        NotificationType.EVENT_UPDATE -> NotificationItemType.EVENT_UPDATE
    }

private fun String?.toDateOrNow(): Date =
    runCatching {
        this?.let { Date(Instant.parse(it).toEpochMilliseconds()) }
    }.getOrNull() ?: Date()
