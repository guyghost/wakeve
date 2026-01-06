package com.guyghost.wakeve.ui.meeting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

/**
 * Meeting Detail Screen for Android (Jetpack Compose)
 *
 * Displays detailed information about a single virtual meeting.
 * Organizers can edit and delete meetings.
 * Uses MeetingManagementViewModel with StateFlow for state management following MVI/FSM pattern.
 *
 * ## Features
 *
 * - Display meeting details (title, description, platform, link)
 * - Display meeting time (date, duration, timezone)
 * - Edit mode for organizers (inline editing)
 * - Delete confirmation dialog
 * - Share meeting link
 * - Material Design 3 theme
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MeetingDetailScreen(
 *     meetingId: String,
 *     viewModel: MeetingManagementViewModel = koinViewModel(),
 *     isOrganizer: Boolean,
 *     onBack: () -> Unit,
 *     onDeleted: () -> Unit
 * ) {
 *     val state by viewModel.state.collectAsStateWithLifecycle()
 *
 *     // Load meeting when screen appears
 *     LaunchedEffect(meetingId) {
 *         viewModel.selectMeeting(meetingId)
 *     }
 *
 *     // Handle side effects
 *     LaunchedEffect(Unit) {
 *         viewModel.sideEffect.collect { effect ->
 *             when (effect) {
 *                 is SideEffect.NavigateTo -> navigate(effect.route)
 *                 is SideEffect.NavigateBack -> onBack()
 *                 else -> {} // Handle other side effects
 *             }
 *         }
 *     }
 *
 *     MeetingDetailContent(
 *         state = state,
 *         isOrganizer = isOrganizer,
 *         onDispatch = { viewModel.dispatch(it) },
 *         onBack = onBack
 *     )
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailScreen(
    meetingId: String,
    viewModel: MeetingManagementViewModel,
    isOrganizer: Boolean = false,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Load meeting on first composition
    LaunchedEffect(meetingId) {
        viewModel.dispatch(MeetingManagementContract.Intent.SelectMeeting(meetingId))
    }

    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is MeetingManagementContract.SideEffect.NavigateBack -> onBack()
                else -> {} // Other side effects handled by parent
            }
        }
    }

    MeetingDetailContent(
        state = state,
        meetingId = meetingId,
        isOrganizer = isOrganizer,
        onDispatch = { viewModel.dispatch(it) },
        onBack = onBack,
        onDeleted = onDeleted
    )
}

/**
 * Meeting detail content component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeetingDetailContent(
    state: MeetingManagementContract.State,
    meetingId: String,
    isOrganizer: Boolean,
    onDispatch: (MeetingManagementContract.Intent) -> Unit,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    // Local UI state for editing
    var isEditing by remember { mutableStateOf(false) }

    // Edit fields
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }

    // Delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Update UI when meeting changes
    LaunchedEffect(state.selectedMeeting) {
        val meeting = state.selectedMeeting ?: return@LaunchedEffect

        if (!isEditing) {
            editTitle = meeting.title
            editDescription = meeting.description ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meeting Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isOrganizer && state.selectedMeeting != null) {
                        if (isEditing) {
                            IconButton(onClick = {
                                onDispatch(MeetingManagementContract.Intent.UpdateMeeting(
                                    meetingId = state.selectedMeeting!!.id,
                                    request = com.guyghost.wakeve.models.UpdateMeetingRequest(
                                        title = editTitle,
                                        description = editDescription.ifBlank { null },
                                        scheduledFor = Clock.System.now(),
                                        duration = state.selectedMeeting!!.duration
                                    )
                                ))
                                isEditing = false
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Save")
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
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
                .verticalScroll(rememberScrollState())
        ) {
            // Loading state
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading meeting details...")
                }
                return@Scaffold
            }

            // Error state
            if (state.hasError) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = state.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onDispatch(MeetingManagementContract.Intent.ClearError) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry")
                }
                return@Scaffold
            }

            // Meeting details
            val meeting = state.selectedMeeting ?: return@Scaffold

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Meeting info card
                MeetingInfoCard(
                    meeting = meeting,
                    isEditing = isEditing,
                    editTitle = editTitle,
                    editDescription = editDescription,
                    onTitleChange = { editTitle = it },
                    onDescriptionChange = { editDescription = it },
                    onSave = {
                        onDispatch(MeetingManagementContract.Intent.UpdateMeeting(
                            meetingId = meeting.id,
                            request = com.guyghost.wakeve.models.UpdateMeetingRequest(
                                title = editTitle,
                                description = editDescription.ifBlank { null },
                                scheduledFor = Clock.System.now(),
                                duration = meeting.duration
                            )
                        ))
                    },
                    isOrganizer = isOrganizer,
                    onDelete = { showDeleteDialog = true }
                )

                // Meeting link card
                MeetingLinkCard(
                    meeting = meeting,
                    isOrganizer = isOrganizer,
                    onGenerateLink = { platform ->
                        onDispatch(MeetingManagementContract.Intent.GenerateMeetingLink(
                            meetingId = meeting.id,
                            platform = platform
                        ))
                    }
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        val meeting = state.selectedMeeting ?: return

        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Meeting") },
            text = { Text("Are you sure you want to delete \"${meeting.title}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDispatch(MeetingManagementContract.Intent.CancelMeeting(meetingId))
                        showDeleteDialog = false
                        onDeleted()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Meeting info card component
 */
@Composable
private fun MeetingInfoCard(
    meeting: VirtualMeeting,
    isEditing: Boolean,
    editTitle: String,
    editDescription: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    isOrganizer: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            if (isEditing) {
                androidx.compose.material3.OutlinedTextField(
                    value = editTitle,
                    onValueChange = onTitleChange,
                    label = { Text("Meeting Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                Text(
                    text = meeting.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            if (isEditing) {
                androidx.compose.material3.OutlinedTextField(
                    value = editDescription,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            } else if (meeting.description != null && meeting.description!!.isNotEmpty()) {
                Text(
                    text = meeting.description!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Divider
            Divider()

            // Platform and time
            MeetingDetailsRow(label = "Platform", value = meeting.platform.name)
            MeetingDetailsRow(label = "Date & Time", value = formatDateTime(meeting.scheduledFor))
            MeetingDetailsRow(label = "Duration", value = formatDuration(meeting.duration))

            // Actions for organizer
            if (isOrganizer) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        enabled = editTitle.isNotBlank() && editTitle != meeting.title
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Changes")
                    }

                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Meeting")
                    }
                }
            }
        }
    }
}

/**
 * Meeting link card component
 */
@Composable
private fun MeetingLinkCard(
    meeting: VirtualMeeting,
    isOrganizer: Boolean,
    onGenerateLink: (MeetingPlatform) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Meeting Link",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (meeting.meetingUrl.isNotEmpty()) {
                Text(
                    text = meeting.meetingUrl,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    text = "No link generated yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Platform dropdown for generating new link
            if (isOrganizer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Generate link:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = { onGenerateLink(MeetingPlatform.ZOOM) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Zoom")
                    }

                    Button(
                        onClick = { onGenerateLink(MeetingPlatform.GOOGLE_MEET) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Google Meet")
                    }

                    Button(
                        onClick = { onGenerateLink(MeetingPlatform.FACETIME) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("FaceTime")
                    }
                }
            }
        }
    }
}

/**
 * Meeting details row component
 */
@Composable
private fun MeetingDetailsRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Format date/time for display
 */
private fun formatDateTime(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.month.name.take(3)} ${localDateTime.dayOfMonth}, ${localDateTime.year} at ${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
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
 * Helper to return null if blank string
 */
private fun String.ifBlank(defaultValue: String): String? {
    return if (isBlank()) defaultValue else this
}
