package com.guyghost.wakeve.ui.meeting

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.VirtualMeeting
import com.guyghost.wakeve.presentation.state.MeetingManagementContract
import com.guyghost.wakeve.viewmodel.MeetingManagementViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Meeting List Screen for Android (Jetpack Compose)
 *
 * Displays a list of virtual meetings for an event with Material Design 3.
 * Features:
 * - List of meetings
 * - Pull-to-refresh
 * - Create new meeting button (organizer only)
 * - Click to view meeting details
 * - Material Design 3 theme
 *
 * Uses MeetingManagementViewModel with StateFlow for state management following MVI/FSM pattern.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MeetingListScreen(
 *     viewModel: MeetingManagementViewModel = koinViewModel(),
 *     eventId: String,
 *     isOrganizer: Boolean
 * ) {
 *     val state by viewModel.state.collectAsStateWithLifecycle()
 *
 *     // Load meetings when screen appears
 *     LaunchedEffect(eventId) {
 *         viewModel.initialize(eventId)
 *     }
 *
 *     // Handle side effects
 *     LaunchedEffect(Unit) {
 *         viewModel.sideEffect.collect { effect ->
 *             when (effect) {
 *                 is MeetingManagementContract.SideEffect.NavigateTo -> navigate(effect.route)
 *                 is MeetingManagementContract.SideEffect.NavigateBack -> navController.popBackStack()
 *                 is MeetingManagementContract.SideEffect.ShowToast -> showToast(effect.message)
 *                 else -> {} // Handle other side effects
 *             }
 *         }
 *     }
 *
 *     // Render UI
 *     MeetingListContent(
 *         state = state,
 *         onDispatch = { viewModel.dispatch(it) },
 *         isOrganizer = isOrganizer
 *     )
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingListScreen(
    viewModel: MeetingManagementViewModel,
    eventId: String? = null,
    isOrganizer: Boolean = false,
    onNavigateToDetail: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingMeeting by remember { mutableStateOf<VirtualMeeting?>(null) }
    var showGenerateLinkDialog by remember { mutableStateOf(false) }
    var generatingMeeting by remember { mutableStateOf<VirtualMeeting?>(null) }

    // Load meetings on first composition
    LaunchedEffect(eventId ?: state.eventId) {
        val id = eventId ?: state.eventId
        if (id.isNotEmpty()) {
            viewModel.initialize(id)
        }
    }

    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is MeetingManagementContract.SideEffect.NavigateTo -> onNavigateToDetail(effect.route)
                is MeetingManagementContract.SideEffect.NavigateBack -> {} // Handled by parent navigation
                is MeetingManagementContract.SideEffect.ShareMeetingLink -> {
                    // Handle share action - typically triggers platform share sheet
                    // For now, we show a toast or navigate
                    viewModel.dispatch(MeetingManagementContract.Intent.ClearError)
                }
                else -> {} // Other side effects
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meetings") },
                actions = {
                    if (isOrganizer) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Create meeting")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                state.isLoading && state.isEmpty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.isEmpty -> {
                    EmptyStateContent(
                        isOrganizer = isOrganizer,
                        onCreateClick = { showCreateDialog = true }
                    )
                }

                else -> {
                    MeetingsListContent(
                        state = state,
                        onDispatch = { viewModel.dispatch(it) },
                        isOrganizer = isOrganizer,
                        onEditClick = { meeting ->
                            editingMeeting = meeting
                            showEditDialog = true
                        },
                        onGenerateLinkClick = { meeting ->
                            generatingMeeting = meeting
                            showGenerateLinkDialog = true
                        }
                    )
                }
            }
        }
    }

    // Edit Meeting Dialog
    if (showEditDialog && editingMeeting != null) {
        EditMeetingDialog(
            meeting = editingMeeting!!,
            onDismiss = {
                showEditDialog = false
                editingMeeting = null
            },
            onConfirm = { title, description, scheduledFor, duration ->
                viewModel.updateMeeting(
                    meetingId = editingMeeting!!.id,
                    title = title,
                    description = description,
                    scheduledFor = scheduledFor,
                    duration = duration
                )
                showEditDialog = false
                editingMeeting = null
            }
        )
    }

    // Generate Link Dialog
    if (showGenerateLinkDialog && generatingMeeting != null) {
        GenerateLinkDialog(
            meeting = generatingMeeting!!,
            onDismiss = {
                showGenerateLinkDialog = false
                generatingMeeting = null
            },
            onGenerate = { platform ->
                viewModel.generateMeetingLink(generatingMeeting!!.id, platform)
                showGenerateLinkDialog = false
                generatingMeeting = null
            }
        )
    }
}

/**
 * Meeting list content component
 */
@Composable
private fun MeetingsListContent(
    state: MeetingManagementContract.State,
    onDispatch: (MeetingManagementContract.Intent) -> Unit,
    isOrganizer: Boolean,
    onEditClick: (VirtualMeeting) -> Unit = {},
    onGenerateLinkClick: (VirtualMeeting) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = state.meetings,
            key = { it.id }
        ) { meeting ->
            MeetingCard(
                meeting = meeting,
                onClick = { onDispatch(MeetingManagementContract.Intent.SelectMeeting(meeting.id)) },
                isOrganizer = isOrganizer,
                onEditClick = onEditClick,
                onGenerateLinkClick = onGenerateLinkClick
            )
        }
    }
}

/**
 * Meeting card component
 */
@Composable
private fun MeetingCard(
    meeting: VirtualMeeting,
    onClick: () -> Unit,
    isOrganizer: Boolean,
    onEditClick: (VirtualMeeting) -> Unit = {},
    onGenerateLinkClick: (VirtualMeeting) -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title
            Text(
                text = meeting.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            // Platform and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = meeting.platform.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = formatDateTime(meeting.scheduledFor),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatDuration(meeting.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Platform link (if exists)
            if (meeting.meetingUrl.isNotEmpty()) {
                Text(
                    text = "Link: ${meeting.meetingUrl}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Actions for organizer
            if (isOrganizer) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { onEditClick(meeting) }) {
                        Text("Edit")
                    }

                    Button(
                        onClick = { onGenerateLinkClick(meeting) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Share Link")
                    }
                }
            }
        }
    }
}

/**
 * Empty state when no meetings exist
 */
@Composable
private fun EmptyStateContent(
    isOrganizer: Boolean,
    onCreateClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isOrganizer) {
                    "No meetings yet"
                } else {
                    "No meetings"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isOrganizer) {
                    "Create a meeting to get started with virtual collaboration"
                } else {
                    "When organizer creates a meeting, it will appear here"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isOrganizer) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onCreateClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Meeting")
                }
            }
        }
    }
}

/**
 * Format date/time for display
 */
private fun formatDateTime(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.month.name.take(3)} ${localDateTime.dayOfMonth}, ${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
}

/**
 * Format duration for display
 */
private fun formatDuration(duration: Duration): String {
    val hours = duration.inWholeHours
    val minutes = (duration.inWholeMinutes % 60)
    return "${hours}h ${minutes}m"
}

/**
 * Edit Meeting Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMeetingDialog(
    meeting: VirtualMeeting,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Instant, Duration) -> Unit
) {
    var title by remember { mutableStateOf(meeting.title) }
    var description by remember { mutableStateOf(meeting.description) }
    var duration by remember { mutableStateOf(meeting.duration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Meeting") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description ?: "",
                    onValueChange = { description = if (it.isBlank()) null else it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = "${duration.inWholeHours}h",
                    onValueChange = { input ->
                        val hours = input.removeSuffix("h").toIntOrNull() ?: 1
                        duration = hours.hours
                    },
                    label = { Text("Duration (hours)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(title, description, meeting.scheduledFor, duration)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Generate Link Dialog
 */
@Composable
private fun GenerateLinkDialog(
    meeting: VirtualMeeting,
    onDismiss: () -> Unit,
    onGenerate: (MeetingPlatform) -> Unit
) {
    var selectedPlatform by remember { mutableStateOf(meeting.platform) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Meeting Link") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select platform to generate a new meeting link:",
                    style = MaterialTheme.typography.bodyMedium
                )

                listOf(
                    MeetingPlatform.ZOOM,
                    MeetingPlatform.GOOGLE_MEET,
                    MeetingPlatform.FACETIME
                ).forEach { platform ->
                    Button(
                        onClick = {
                            selectedPlatform = platform
                            onGenerate(platform)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (platform == selectedPlatform) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Text(platform.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
