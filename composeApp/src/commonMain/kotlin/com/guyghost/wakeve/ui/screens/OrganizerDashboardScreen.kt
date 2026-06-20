package com.guyghost.wakeve.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote

// ==================== Data Models ====================

data class DashboardOverviewData(
    val totalEvents: Int,
    val totalParticipants: Int,
    val averageParticipants: Double,
    val totalVotes: Int,
    val totalComments: Int,
    val averageResponseRate: Double,
    val eventsByStatus: Map<String, Int>
)

data class DashboardEventData(
    val eventId: String,
    val title: String,
    val status: String,
    val eventType: String? = null,
    val participantCount: Int,
    val voteCount: Int,
    val commentCount: Int,
    val responseRate: Double,
    val votedParticipants: Int,
    val timeSlotVotes: List<TimeSlotVotes>
)

data class EventAnalyticsData(
    val popularTimeSlots: List<TimeSlotVotes>,
    val pollCompletionRate: Double,
    val totalParticipants: Int,
    val votedParticipants: Int
)

data class TimeSlotVotes(
    val label: String,
    val yesVotes: Int,
    val maybeVotes: Int,
    val noVotes: Int
) {
    val totalVotes: Int get() = yesVotes + maybeVotes + noVotes
}

// ==================== Main Screen ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerDashboardScreen(
    onNavigateBack: () -> Unit,
    events: List<Event>,
    currentUserId: String,
    pollVotesByEvent: Map<String, Map<String, Map<String, Vote>>> = emptyMap(),
    isLoading: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    val dashboardEvents = remember(events, currentUserId, pollVotesByEvent) {
        events
            .filter { it.organizerId == currentUserId }
            .map { event ->
                event.toDashboardEventData(pollVotesByEvent[event.id].orEmpty())
            }
            .sortedByDescending { it.eventId }
    }
    val overview = remember(dashboardEvents) {
        dashboardEvents.toOverviewData()
    }

    var selectedEvent by remember { mutableStateOf<DashboardEventData?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tableau de bord",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            // Summary Cards
            item {
                Text(
                    text = "Vue d'ensemble",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                SummaryCardsRow(overview)
            }

            item {
                SummaryCardsRowSecond(overview)
            }

            if (isLoading) {
                item { LoadingDashboardCard() }
            } else if (dashboardEvents.isEmpty()) {
                item { EmptyOrganizerDashboardCard(error = error) }
            } else {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Evenements par statut",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    StatusBreakdownCard(overview.eventsByStatus)
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mes evenements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(dashboardEvents, key = { it.eventId }) { event ->
                    EventAnalyticsCard(
                        event = event,
                        onClick = {
                            selectedEvent = event
                            showDetailSheet = true
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Detail Sheet
    if (showDetailSheet && selectedEvent != null) {
        ModalBottomSheet(
            onDismissRequest = { showDetailSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            val event = selectedEvent!!
            EventDetailedAnalyticsContent(
                event = event,
                analytics = remember(event) {
                    EventAnalyticsData(
                        popularTimeSlots = event.timeSlotVotes,
                        pollCompletionRate = event.responseRate,
                        totalParticipants = event.participantCount,
                        votedParticipants = event.votedParticipants
                    )
                }
            )
        }
    }
}

private fun Event.toDashboardEventData(
    pollVotes: Map<String, Map<String, Vote>>
): DashboardEventData {
    val participantCount = participants.distinct().size
    val votedParticipants = pollVotes.keys.size
    val voteCount = pollVotes.values.sumOf { it.size }
    val responseRate = if (participantCount > 0) {
        (votedParticipants.toDouble() / participantCount.toDouble()) * 100.0
    } else {
        0.0
    }

    return DashboardEventData(
        eventId = id,
        title = title,
        status = status.name,
        eventType = eventType.name,
        participantCount = participantCount,
        voteCount = voteCount,
        commentCount = 0,
        responseRate = responseRate,
        votedParticipants = votedParticipants,
        timeSlotVotes = proposedSlots.map { slot ->
            slot.toTimeSlotVotes(pollVotes)
        }
    )
}

private fun TimeSlot.toTimeSlotVotes(
    pollVotes: Map<String, Map<String, Vote>>
): TimeSlotVotes {
    val votes = pollVotes.values.mapNotNull { it[id] }
    return TimeSlotVotes(
        label = dashboardLabel(),
        yesVotes = votes.count { it == Vote.YES },
        maybeVotes = votes.count { it == Vote.MAYBE },
        noVotes = votes.count { it == Vote.NO }
    )
}

private fun TimeSlot.dashboardLabel(): String =
    start?.substringBefore('T')
        ?: timeOfDay.name.lowercase().replace('_', ' ')

private fun List<DashboardEventData>.toOverviewData(): DashboardOverviewData {
    val totalParticipants = sumOf { it.participantCount }
    val totalEvents = size
    return DashboardOverviewData(
        totalEvents = totalEvents,
        totalParticipants = totalParticipants,
        averageParticipants = if (totalEvents > 0) totalParticipants.toDouble() / totalEvents else 0.0,
        totalVotes = sumOf { it.voteCount },
        totalComments = sumOf { it.commentCount },
        averageResponseRate = if (totalEvents > 0) sumOf { it.responseRate } / totalEvents else 0.0,
        eventsByStatus = groupingBy { it.status }.eachCount()
    )
}

// ==================== Summary Cards ====================

@Composable
private fun SummaryCardsRow(overview: DashboardOverviewData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardSummaryCard(
            icon = Icons.Default.CalendarMonth,
            title = "Evenements",
            value = "${overview.totalEvents}",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        DashboardSummaryCard(
            icon = Icons.Default.Groups,
            title = "Participants",
            value = "${overview.totalParticipants}",
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        DashboardSummaryCard(
            icon = Icons.Default.TrendingUp,
            title = "Moy. / evt",
            value = String.format("%.1f", overview.averageParticipants),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCardsRowSecond(overview: DashboardOverviewData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardSummaryCard(
            icon = Icons.Default.ThumbUp,
            title = "Votes",
            value = "${overview.totalVotes}",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        DashboardSummaryCard(
            icon = Icons.Default.ChatBubble,
            title = "Commentaires",
            value = "${overview.totalComments}",
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        DashboardSummaryCard(
            icon = Icons.Default.Percent,
            title = "Taux rep.",
            value = String.format("%.0f%%", overview.averageResponseRate),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LoadingDashboardCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Text(
                text = "Chargement des evenements...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyOrganizerDashboardCard(error: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Aucun evenement organise",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = error ?: "Les statistiques apparaitront ici quand vous aurez cree votre premier evenement.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DashboardSummaryCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==================== Status Breakdown ====================

@Composable
private fun StatusBreakdownCard(eventsByStatus: Map<String, Int>) {
    val total = eventsByStatus.values.sum().coerceAtLeast(1)
    val statusLabels = mapOf(
        "DRAFT" to "Brouillon",
        "POLLING" to "Sondage",
        "COMPARING" to "Comparaison",
        "CONFIRMED" to "Confirme",
        "ORGANIZING" to "Organisation",
        "FINALIZED" to "Finalise"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            eventsByStatus.entries.sortedByDescending { it.value }.forEach { (status, count) ->
                val color = dashboardStatusColor(status)
                val label = statusLabels[status] ?: status
                val fraction by animateFloatAsState(
                    targetValue = count.toFloat() / total.toFloat(),
                    animationSpec = tween(600),
                    label = "bar_$status"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(100.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                        )
                    }
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                        modifier = Modifier.width(30.dp)
                    )
                }
            }
        }
    }
}

// ==================== Event Analytics Card ====================

@Composable
private fun EventAnalyticsCard(
    event: DashboardEventData,
    onClick: () -> Unit
) {
    val statusLabels = mapOf(
        "DRAFT" to "Brouillon",
        "POLLING" to "Sondage",
        "COMPARING" to "Comparaison",
        "CONFIRMED" to "Confirme",
        "ORGANIZING" to "Organisation",
        "FINALIZED" to "Finalise"
    )
    val color = dashboardStatusColor(event.status)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusLabels[event.status] ?: event.status,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = color,
                    modifier = Modifier
                        .background(
                            color.copy(alpha = 0.15f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Mini stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat(label = "Participants", value = "${event.participantCount}")
                MiniStat(label = "Votes", value = "${event.voteCount}")
                MiniStat(label = "Comm.", value = "${event.commentCount}")
                MiniStat(label = "Rep.", value = String.format("%.0f%%", event.responseRate))
            }

            // Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Voir details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun dashboardStatusColor(status: String): Color =
    when (status) {
        "DRAFT" -> MaterialTheme.colorScheme.outline
        "POLLING" -> MaterialTheme.colorScheme.primary
        "COMPARING" -> MaterialTheme.colorScheme.secondary
        "CONFIRMED" -> MaterialTheme.colorScheme.tertiary
        "ORGANIZING" -> MaterialTheme.colorScheme.primary
        "FINALIZED" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

@Composable
private fun MiniStat(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== Event Detailed Analytics (Bottom Sheet) ====================

@Composable
private fun EventDetailedAnalyticsContent(
    event: DashboardEventData,
    analytics: EventAnalyticsData
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Poll Completion
        item {
            PollCompletionCard(
                completionRate = analytics.pollCompletionRate,
                votedParticipants = analytics.votedParticipants,
                totalParticipants = analytics.totalParticipants
            )
        }

        item {
            PopularTimeSlotsCard(slots = analytics.popularTimeSlots)
        }
    }
}

@Composable
private fun PollCompletionCard(
    completionRate: Double,
    votedParticipants: Int,
    totalParticipants: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Taux de participation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Circular progress
            val animatedProgress by animateFloatAsState(
                targetValue = (completionRate / 100.0).toFloat().coerceIn(0f, 1f),
                animationSpec = tween(800),
                label = "pollCompletion"
            )

            val progressColor = when {
                completionRate > 75 -> MaterialTheme.colorScheme.tertiary
                completionRate > 50 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            }
            val outlineColor = MaterialTheme.colorScheme.outline

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                // Background circle
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(100.dp)
                ) {
                    drawArc(
                        color = outlineColor.copy(alpha = 0.15f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10.dp.toPx())
                    )
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 10.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
                Text(
                    text = String.format("%.0f%%", completionRate),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "$votedParticipants sur $totalParticipants participants ont vote",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PopularTimeSlotsCard(slots: List<TimeSlotVotes>) {
    val yesColor = MaterialTheme.colorScheme.tertiary
    val maybeColor = MaterialTheme.colorScheme.primary
    val noColor = MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Creneaux les plus populaires",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (slots.isEmpty()) {
                Text(
                    text = "Aucun creneau disponible pour cet evenement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            slots.forEachIndexed { index, slot ->
                val total = slot.totalVotes.coerceAtLeast(1)
                val yesFraction = slot.yesVotes.toFloat() / total
                val maybeFraction = slot.maybeVotes.toFloat() / total
                val noFraction = slot.noVotes.toFloat() / total

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Rank badge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                        ) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = slot.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${slot.totalVotes} votes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Stacked bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(yesFraction.coerceAtLeast(0.01f))
                                .height(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(yesColor)
                        )
                        Box(
                            modifier = Modifier
                                .weight(maybeFraction.coerceAtLeast(0.01f))
                                .height(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(maybeColor)
                        )
                        Box(
                            modifier = Modifier
                                .weight(noFraction.coerceAtLeast(0.01f))
                                .height(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(noColor)
                        )
                    }

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        VoteLegend(color = yesColor, label = "Oui: ${slot.yesVotes}")
                        VoteLegend(color = maybeColor, label = "Peut-etre: ${slot.maybeVotes}")
                        VoteLegend(color = noColor, label = "Non: ${slot.noVotes}")
                    }
                }

                if (index < slots.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun VoteLegend(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
