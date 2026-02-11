package com.guyghost.wakeve.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.activity.ActivityManager
import com.guyghost.wakeve.activity.ActivityRepository
import com.guyghost.wakeve.comment.CommentRepository
import com.guyghost.wakeve.models.ActivitiesByDate
import com.guyghost.wakeve.models.Activity
import com.guyghost.wakeve.models.ActivityWithStats
import com.guyghost.wakeve.models.CommentSection

/**
 * Activity Planning Screen
 *
 * Features:
 * - List activities grouped by date
 * - Create/Edit/Delete activities
 * - Participant registration with capacity management
 * - Display costs and registration count
 * - Filter by date
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityPlanningScreen(
    eventId: String,
    organizerId: String,
    participants: List<ParticipantInfo>,
    activityRepository: ActivityRepository,
    commentRepository: CommentRepository,
    onNavigateBack: () -> Unit,
    onNavigateToComments: (eventId: String, section: CommentSection, sectionItemId: String?) -> Unit
) {
    var activitiesByDate by remember { mutableStateOf<List<ActivitiesByDate>>(emptyList()) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var showAddActivityDialog by remember { mutableStateOf(false) }
    var activityToEdit by remember { mutableStateOf<Activity?>(null) }
    var activityToDelete by remember { mutableStateOf<Activity?>(null) }
    var showParticipantsDialog by remember { mutableStateOf<ActivityWithStats?>(null) }
    var totalCost by remember { mutableStateOf(0L) }
    var commentCount by remember { mutableIntStateOf(0) }

    // Load data
    fun loadData() {
        activitiesByDate = activityRepository.getActivitiesByDateGrouped(eventId)
        totalCost = activityRepository.sumActivityCostByEvent(eventId)
        commentCount = commentRepository.countCommentsBySection(eventId, CommentSection.ACTIVITY).toInt()
    }

    LaunchedEffect(eventId) {
        loadData()
    }

    // Filter activities by selected date
    val displayedActivities = if (selectedDate != null) {
        activitiesByDate.filter { it.date == selectedDate }
    } else {
        activitiesByDate
    }

    val allActivities = displayedActivities.flatMap { it.activities }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planification des activités") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    // Comments icon with badge
                    IconButton(onClick = {
                        onNavigateToComments(eventId, CommentSection.ACTIVITY, null)
                    }) {
                        Box {
                            Icon(
                                Icons.Outlined.Comment,
                                contentDescription = if (commentCount == 0) "Aucun commentaire" else "$commentCount commentaires"
                            )
                            if (commentCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.error,
                                            shape = CircleShape
                                        )
                                        .align(Alignment.TopEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = commentCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onError,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddActivityDialog = true }
            ) {
                Icon(Icons.Default.Add, "Ajouter une activité")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Card
            ActivitySummaryCard(
                totalActivities = allActivities.size,
                totalCost = totalCost,
                modifier = Modifier.padding(16.dp)
            )

            // Date Filter
            if (activitiesByDate.isNotEmpty()) {
                DateFilterRow(
                    dates = activitiesByDate.map { it.date },
                    selectedDate = selectedDate,
                    onDateSelected = { date ->
                        selectedDate = if (selectedDate == date) null else date
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Activities List
            if (allActivities.isEmpty()) {
                EmptyStateCard(
                    message = if (activitiesByDate.isEmpty())
                        "Aucune activité planifiée"
                    else
                        "Aucune activité à cette date",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    displayedActivities.forEach { dayActivities ->
                        item {
                            DateSection(
                                date = dayActivities.date,
                                activities = dayActivities.activities.map {
                                    ActivityManager.calculateActivityStats(it)
                                },
                                participants = participants,
                                activityRepository = activityRepository,
                                onActivityClick = { activity -> activityToEdit = activity },
                                onDeleteClick = { activity -> activityToDelete = activity },
                                onParticipantsClick = { activity ->
                                    showParticipantsDialog = activity
                                },
                                onReload = { loadData() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddActivityDialog) {
        AddEditActivityDialog(
            activity = null,
            organizerId = organizerId,
            onDismiss = { showAddActivityDialog = false },
            onConfirm = { activity ->
                activityRepository.createActivity(activity.copy(eventId = eventId))
                showAddActivityDialog = false
                loadData()
            }
        )
    }

    activityToEdit?.let { activity ->
        AddEditActivityDialog(
            activity = activity,
            organizerId = organizerId,
            onDismiss = { activityToEdit = null },
            onConfirm = { updated ->
                activityRepository.updateActivity(updated)
                activityToEdit = null
                loadData()
            }
        )
    }

    showParticipantsDialog?.let { activityWithStats ->
        ManageParticipantsDialog(
            activity = activityWithStats,
            allParticipants = participants,
            activityRepository = activityRepository,
            onDismiss = { showParticipantsDialog = null },
            onReload = { loadData() }
        )
    }

    activityToDelete?.let { activity ->
        AlertDialog(
            onDismissRequest = { activityToDelete = null },
            title = { Text("Supprimer l'activité ?") },
            text = { Text("Voulez-vous vraiment supprimer « ${activity.name} » ?") },
            confirmButton = {
                TextButton(onClick = {
                    activityRepository.deleteActivity(activity.id)
                    activityToDelete = null
                    loadData()
                }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { activityToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun ActivitySummaryCard(
    totalActivities: Int,
    totalCost: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = totalActivities.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Activités",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            VerticalDivider(modifier = Modifier.height(48.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${totalCost / 100}€",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Coût total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DateFilterRow(
    dates: List<String>,
    selectedDate: String?,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedDate == null,
                onClick = { onDateSelected("") },
                label = { Text("Toutes") }
            )
        }

        items(dates.sorted()) { date ->
            FilterChip(
                selected = selectedDate == date,
                onClick = { onDateSelected(date) },
                label = {
                    Text(formatDateString(date))
                }
            )
        }
    }
}

@Composable
private fun DateSection(
    date: String,
    activities: List<ActivityWithStats>,
    participants: List<ParticipantInfo>,
    activityRepository: ActivityRepository,
    onActivityClick: (Activity) -> Unit,
    onDeleteClick: (Activity) -> Unit,
    onParticipantsClick: (ActivityWithStats) -> Unit,
    onReload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDateString(date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Badge {
                    Text(activities.size.toString())
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            activities.forEach { activityWithStats ->
                ActivityItemRow(
                    activity = activityWithStats,
                    participants = participants,
                    onClick = { onActivityClick(activityWithStats.activity) },
                    onDeleteClick = { onDeleteClick(activityWithStats.activity) },
                    onParticipantsClick = { onParticipantsClick(activityWithStats) }
                )

                if (activityWithStats != activities.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ActivityItemRow(
    activity: ActivityWithStats,
    participants: List<ParticipantInfo>,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onParticipantsClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.activity.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                if (activity.activity.description.isNotBlank()) {
                    Text(
                        text = activity.activity.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time and Duration
                    if (activity.activity.time != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${activity.activity.time} (${activity.activity.duration}min)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Location
                    activity.activity.location?.let { location ->
                        if (location.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = location,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Cost per person
                    activity.activity.cost?.let { cost ->
                        if (cost > 0) {
                            Text(
                                text = "${cost / 100}€/pers",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Participants and capacity
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = onParticipantsClick,
                        label = {
                            Text(
                                text = if (activity.activity.maxParticipants != null) {
                                    "${activity.registeredCount} / ${activity.activity.maxParticipants} inscrits"
                                } else {
                                    "${activity.registeredCount} inscrits"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    // Full indicator
                    if (activity.isFull) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Complet", style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                labelColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        }

        // Expanded Actions
        if (expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onClick,
                    label = { Text("Modifier") },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    }
                )

                AssistChip(
                    onClick = onParticipantsClick,
                    label = { Text("Participants") },
                    leadingIcon = {
                        Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp))
                    }
                )

                AssistChip(
                    onClick = onDeleteClick,
                    label = { Text("Supprimer") },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.error
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Format ISO date string (YYYY-MM-DD) to display format
 */
private fun formatDateString(dateStr: String): String {
    val months = listOf(
        "Jan", "Fév", "Mar", "Avr", "Mai", "Juin",
        "Juil", "Août", "Sep", "Oct", "Nov", "Déc"
    )

    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val day = parts[2].toInt()
            val month = parts[1].toInt()
            "$day ${months[month - 1]}"
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}
