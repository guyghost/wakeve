package com.guyghost.wakeve

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random

data class EventCreationState(
    val title: String = "",
    val description: String = "",
    val deadline: String = "",
    val slots: List<TimeSlot> = emptyList(),
    val slotStartDate: String = "",
    val slotStartTime: String = "",
    val slotEndDate: String = "",
    val slotEndTime: String = "",
    val isError: Boolean = false,
    val errorMessage: String = ""
)

package com.guyghost.wakeve

/**
 * @deprecated This screen is deprecated and will be removed in a future major version.
 * Use [DraftEventWizard] instead, which provides a better UX with a multi-step wizard.
 *
 * ## Migration Guide
 *
 * The deprecated `EventCreationScreen` used a simple form approach. The new `DraftEventWizard`
 * provides a progressive 4-step experience:
 * 1. Event Details (title, description, type)
 * 2. Participants Estimation (min/max/expected)
 * 3. Time Slots (flexible with timeOfDay)
 * 4. Location (potential locations)
 *
 * ### Migration Example
 *
 * ```kotlin
 * // OLD (deprecated) - EventCreationScreen usage
 * EventCreationScreen(
 *     userId = userId,
 *     onEventCreated = { event ->
 *         // Handle created event
 *         navController.navigate(Screen.ParticipantManagement.createRoute(event.id))
 *     },
 *     onBack = {
 *         navController.navigateUp()
 *     }
 * )
 *
 * // NEW - DraftEventWizard usage
 * DraftEventWizard(
 *     initialEvent = null,
 *     onSaveStep = { event ->
 *         // Optionally save draft state between steps
 *         viewModel.dispatch(Intent.UpdateDraftEvent(event))
 *     },
 *     onComplete = { event ->
 *         // Event creation completed - navigate to next screen
 *         navController.navigate(Screen.ParticipantManagement.createRoute(event.id)) {
 *             popUpTo(Screen.EventCreation.route) { inclusive = true }
 *         }
 *     },
 *     onCancel = {
 *         navController.navigateUp()
 *     }
 * )
 * ```
 *
 * ### Benefits of DraftEventWizard
 * - **Progressive Disclosure**: Users fill information in manageable steps
 * - **Better UX**: Clear step indicators and validation per step
 * - **Enhanced Features**: Supports event types, participant estimation, flexible time slots, and potential locations
 * - **State Preservation**: Draft state can be saved between steps
 *
 * ### Deprecation Timeline
 * - **Current version**: @Deprecated annotation added, warning logged when used
 * - **Next minor version**: Warning message in logs
 * - **Next major version**: This screen will be removed entirely
 *
 * @see DraftEventWizard
 * @see <a href="docs/implementation/draft-event-wizard-guide.md">Draft Event Wizard Guide</a>
 */
@OptIn(ExperimentalMaterial3Api::class)
@Deprecated(
    message = "Use DraftEventWizard instead for better UX with multi-step wizard. " +
             "See docs/implementation/draft-event-wizard-guide.md for migration details.",
    replaceWith = ReplaceWith("DraftEventWizard"),
    level = DeprecationLevel.WARNING
)
@Composable
fun EventCreationScreen(
    userId: String,
    onEventCreated: (Event) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(EventCreationState()) }
    var isCreating by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }
    var deadlineError by remember { mutableStateOf(false) }
    var slotsError by remember { mutableStateOf(false) }

    // Date Picker States
    val deadlineDatePickerState = rememberDatePickerState()
    var showDeadlinePicker by remember { mutableStateOf(false) }

    // Time Slot Pickers
    val startDatePickerState = rememberDatePickerState()
    val startTimePickerState = rememberTimePickerState()
    val endDatePickerState = rememberDatePickerState()
    val endTimePickerState = rememberTimePickerState()
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // Validation
    val isFormValid = state.title.isNotBlank() && state.deadline.isNotBlank() && state.slots.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer un événement", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Retour" }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isCreating && isFormValid) {
                FloatingActionButton(
                    onClick = {
                        isCreating = true
                        val now = Clock.System.now().toString()
                        val event = Event(
                            id = "event-${Random.nextLong(1000000)}",
                            title = state.title,
                            description = state.description,
                            organizerId = userId,
                            participants = emptyList(),
                            proposedSlots = state.slots,
                            deadline = state.deadline,
                            status = EventStatus.DRAFT,
                            createdAt = now,
                            updatedAt = now
                        )
                        onEventCreated(event)
                    },
                    modifier = Modifier.semantics { contentDescription = "Créer l'événement" }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Input
            OutlinedTextField(
                value = state.title,
                onValueChange = {
                    state = state.copy(title = it)
                    titleError = false
                },
                label = { Text("Titre de l'événement *") },
                isError = titleError,
                supportingText = if (titleError) { { Text("Le titre est requis") } } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description Input
            OutlinedTextField(
                value = state.description,
                onValueChange = { state = state.copy(description = it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Deadline Picker
            OutlinedTextField(
                value = state.deadline.ifBlank { "Sélectionner une date limite" },
                onValueChange = {},
                label = { Text("Date limite *") },
                readOnly = true,
                isError = deadlineError,
                supportingText = if (deadlineError) { { Text("La date limite est requise") } } else null,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showDeadlinePicker = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Sélectionner la date limite")
                    }
                }
            )

            HorizontalDivider()

            // Time Slots Section
            Text(
                "Créneaux horaires proposés",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            if (slotsError) {
                Text(
                    "Au moins un créneau est requis",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Add Slot Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.slotStartDate.ifBlank { "Date début" },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { showStartDatePicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Sélectionner la date de début")
                        }
                    }
                )
                OutlinedTextField(
                    value = state.slotStartTime.ifBlank { "Heure début" },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { showStartTimePicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Sélectionner l'heure de début")
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.slotEndDate.ifBlank { "Date fin" },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { showEndDatePicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Sélectionner la date de fin")
                        }
                    }
                )
                OutlinedTextField(
                    value = state.slotEndTime.ifBlank { "Heure fin" },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { showEndTimePicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Sélectionner l'heure de fin")
                        }
                    }
                )
            }

            Button(
                onClick = {
                    if (state.slotStartDate.isNotBlank() && state.slotStartTime.isNotBlank() &&
                        state.slotEndDate.isNotBlank() && state.slotEndTime.isNotBlank()) {
                        val startIso = "${state.slotStartDate}T${state.slotStartTime}:00Z"
                        val endIso = "${state.slotEndDate}T${state.slotEndTime}:00Z"
                        val newSlot = TimeSlot(
                            id = "slot-${state.slots.size + 1}",
                            start = startIso,
                            end = endIso,
                            timezone = "UTC"
                        )
                        state = state.copy(
                            slots = state.slots + newSlot,
                            slotStartDate = "",
                            slotStartTime = "",
                            slotEndDate = "",
                            slotEndTime = ""
                        )
                        slotsError = false
                    }
                },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Ajouter le créneau" }
            ) {
                Text("Ajouter le créneau")
            }

            // Display Added Slots
            state.slots.forEach { slot ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${slot.start} → ${slot.end}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "UTC",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                state = state.copy(slots = state.slots.filter { it.id != slot.id })
                            },
                            modifier = Modifier.semantics { contentDescription = "Supprimer le créneau" }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
            }

            // Loading Indicator
            if (isCreating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text("Création en cours...", modifier = Modifier.padding(start = 8.dp))
                }
            }

            // Cancel Button
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.End).semantics { contentDescription = "Annuler" }
            ) {
                Text("Annuler")
            }
        }
    }

    // Deadline Date Picker Dialog
    if (showDeadlinePicker) {
        DatePickerDialog(
            onDismissRequest = { showDeadlinePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlineDatePickerState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        val localDateTime = instant.toLocalDateTime(TimeZone.UTC)
                        state = state.copy(deadline = "${localDateTime.date}T23:59:59Z")
                        deadlineError = false
                    }
                    showDeadlinePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeadlinePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = deadlineDatePickerState)
        }
    }

    // Start Date Picker
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        val localDateTime = instant.toLocalDateTime(TimeZone.UTC)
                        state = state.copy(slotStartDate = localDateTime.date.toString())
                    }
                    showStartDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    // Start Time Picker
    if (showStartTimePicker) {
        TimePickerDialog(
            title = { Text("Sélectionner l'heure de début") },
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state = state.copy(slotStartTime = String.format("%02d:%02d", startTimePickerState.hour, startTimePickerState.minute))
                    showStartTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            TimePicker(state = startTimePickerState)
        }
    }

    // End Date Picker
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        val localDateTime = instant.toLocalDateTime(TimeZone.UTC)
                        state = state.copy(slotEndDate = localDateTime.date.toString())
                    }
                    showEndDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    // End Time Picker
    if (showEndTimePicker) {
        TimePickerDialog(
            title = { Text("Sélectionner l'heure de fin") },
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state = state.copy(slotEndTime = String.format("%02d:%02d", endTimePickerState.hour, endTimePickerState.minute))
                    showEndTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            TimePicker(state = endTimePickerState)
        }
    }
}