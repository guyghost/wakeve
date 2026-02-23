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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// ==================== Data Models ====================

data class DashboardOverviewData(
    val totalEvents: Int = 12,
    val totalParticipants: Int = 87,
    val averageParticipants: Double = 7.25,
    val totalVotes: Int = 234,
    val totalComments: Int = 56,
    val eventsByStatus: Map<String, Int> = mapOf(
        "DRAFT" to 3,
        "POLLING" to 2,
        "CONFIRMED" to 4,
        "FINALIZED" to 3
    )
)

data class DashboardEventData(
    val eventId: String,
    val title: String,
    val status: String,
    val eventType: String? = null,
    val participantCount: Int,
    val voteCount: Int,
    val commentCount: Int,
    val responseRate: Double
)

data class EventAnalyticsData(
    val voteTimeline: List<Pair<String, Int>> = listOf(
        "01/10" to 3, "02/10" to 7, "03/10" to 12, "04/10" to 8, "05/10" to 15, "06/10" to 5, "07/10" to 2
    ),
    val participantTimeline: List<Pair<String, Int>> = listOf(
        "01/10" to 2, "02/10" to 4, "03/10" to 3, "04/10" to 5, "05/10" to 1
    ),
    val popularTimeSlots: List<TimeSlotVotes> = listOf(
        TimeSlotVotes("Apres-midi", 12, 3, 2),
        TimeSlotVotes("Matin", 8, 5, 4),
        TimeSlotVotes("Soiree", 6, 7, 4)
    ),
    val pollCompletionRate: Double = 85.0,
    val totalParticipants: Int = 15,
    val votedParticipants: Int = 13
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
    modifier: Modifier = Modifier
) {
    val overview = remember { DashboardOverviewData() }

    val events = remember {
        listOf(
            DashboardEventData("1", "Anniversaire Marie", "CONFIRMED", "BIRTHDAY", 15, 42, 8, 87.5),
            DashboardEventData("2", "Team Building Q4", "POLLING", "TEAM_BUILDING", 22, 38, 12, 65.0),
            DashboardEventData("3", "Soiree de Noel", "DRAFT", "PARTY", 8, 0, 3, 0.0),
            DashboardEventData("4", "Reunion Projet Alpha", "FINALIZED", "CONFERENCE", 10, 28, 5, 93.0),
            DashboardEventData("5", "Brunch Dominical", "CONFIRMED", "FOOD_TASTING", 6, 18, 4, 100.0)
        )
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

            // Status Breakdown
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

            // Events List
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mes evenements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(events, key = { it.eventId }) { event ->
                EventAnalyticsCard(
                    event = event,
                    onClick = {
                        selectedEvent = event
                        showDetailSheet = true
                    }
                )
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
            EventDetailedAnalyticsContent(
                event = selectedEvent!!,
                analytics = remember { EventAnalyticsData() }
            )
        }
    }
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
            color = Color(0xFF2563EB),
            modifier = Modifier.weight(1f)
        )
        DashboardSummaryCard(
            icon = Icons.Default.Groups,
            title = "Participants",
            value = "${overview.totalParticipants}",
            color = Color(0xFF16A34A),
            modifier = Modifier.weight(1f)
        )
        DashboardSummaryCard(
            icon = Icons.Default.TrendingUp,
            title = "Moy. / evt",
            value = String.format("%.1f", overview.averageParticipants),
            color = Color(0xFF9333EA),
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
            color = Color(0xFFEA580C),
            modifier = Modifier.weight(1f)
        )
        DashboardSummaryCard(
            icon = Icons.Default.ChatBubble,
            title = "Commentaires",
            value = "${overview.totalComments}",
            color = Color(0xFF0D9488),
            modifier = Modifier.weight(1f)
        )
        DashboardSummaryCard(
            icon = Icons.Default.Percent,
            title = "Taux rep.",
            value = "78%",
            color = Color(0xFFDC2626),
            modifier = Modifier.weight(1f)
        )
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
    val statusColors = mapOf(
        "DRAFT" to Color.Gray,
        "POLLING" to Color(0xFF2563EB),
        "COMPARING" to Color(0xFF4F46E5),
        "CONFIRMED" to Color(0xFF16A34A),
        "ORGANIZING" to Color(0xFFEA580C),
        "FINALIZED" to Color(0xFF9333EA)
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
                val color = statusColors[status] ?: Color.Gray
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
    val statusColors = mapOf(
        "DRAFT" to Color.Gray,
        "POLLING" to Color(0xFF2563EB),
        "COMPARING" to Color(0xFF4F46E5),
        "CONFIRMED" to Color(0xFF16A34A),
        "ORGANIZING" to Color(0xFFEA580C),
        "FINALIZED" to Color(0xFF9333EA)
    )

    val color = statusColors[event.status] ?: Color.Gray

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

        // Vote Timeline
        item {
            TimelineChartCard(
                title = "Chronologie des votes",
                entries = analytics.voteTimeline,
                color = Color(0xFF2563EB)
            )
        }

        // Participant Timeline
        item {
            TimelineChartCard(
                title = "Chronologie des inscriptions",
                entries = analytics.participantTimeline,
                color = Color(0xFF16A34A)
            )
        }

        // Popular Time Slots
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
                targetValue = (completionRate / 100.0).toFloat(),
                animationSpec = tween(800),
                label = "pollCompletion"
            )

            val progressColor = when {
                completionRate > 75 -> Color(0xFF16A34A)
                completionRate > 50 -> Color(0xFFEA580C)
                else -> Color(0xFFDC2626)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                // Background circle
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(100.dp)
                ) {
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.15f),
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
private fun TimelineChartCard(
    title: String,
    entries: List<Pair<String, Int>>,
    color: Color
) {
    val maxCount = entries.maxOfOrNull { it.second } ?: 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                entries.forEach { (date, count) ->
                    val fraction by animateFloatAsState(
                        targetValue = count.toFloat() / maxCount.toFloat(),
                        animationSpec = tween(600),
                        label = "bar_$date"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height((fraction * 70).dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(color, color.copy(alpha = 0.6f))
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PopularTimeSlotsCard(slots: List<TimeSlotVotes>) {
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
                                    if (index == 0) Color(0xFF9333EA) else Color.Gray
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
                                .background(Color(0xFF16A34A))
                        )
                        Box(
                            modifier = Modifier
                                .weight(maybeFraction.coerceAtLeast(0.01f))
                                .height(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFEA580C))
                        )
                        Box(
                            modifier = Modifier
                                .weight(noFraction.coerceAtLeast(0.01f))
                                .height(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFDC2626))
                        )
                    }

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        VoteLegend(color = Color(0xFF16A34A), label = "Oui: ${slot.yesVotes}")
                        VoteLegend(color = Color(0xFFEA580C), label = "Peut-etre: ${slot.maybeVotes}")
                        VoteLegend(color = Color(0xFFDC2626), label = "Non: ${slot.noVotes}")
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
