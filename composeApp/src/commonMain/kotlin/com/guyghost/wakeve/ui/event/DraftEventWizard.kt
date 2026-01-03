package com.guyghost.wakeve.ui.event

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.ui.components.EventTypeSelector
import com.guyghost.wakeve.ui.components.LocationInputDialog
import com.guyghost.wakeve.ui.components.ParticipantsEstimationCard
import com.guyghost.wakeve.ui.components.PotentialLocationsList
import com.guyghost.wakeve.ui.components.TimeSlotInput
import kotlinx.datetime.Clock

/**
 * Multi-step wizard for creating an event in DRAFT phase.
 * 
 * Steps:
 * 1. Basic Info (title, description, type)
 * 2. Participants Estimation (min/max/expected)
 * 3. Potential Locations
 * 4. Time Slots
 * 
 * Features:
 * - Auto-save on each step
 * - Validation before proceeding
 * - Material You design with progress indicator
 * 
 * @param initialEvent Initial event data (for editing)
 * @param onSaveStep Callback when moving between steps (for auto-save)
 * @param onComplete Callback when wizard is complete
 * @param onCancel Callback when wizard is cancelled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftEventWizard(
    initialEvent: Event?,
    onSaveStep: (Event) -> Unit,
    onComplete: (Event) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(0) }
    
    // State for Step 1: Basic Info
    var title by remember(initialEvent) { mutableStateOf(initialEvent?.title ?: "") }
    var description by remember(initialEvent) { mutableStateOf(initialEvent?.description ?: "") }
    var eventType by remember(initialEvent) { mutableStateOf(initialEvent?.eventType ?: EventType.OTHER) }
    var eventTypeCustom by remember(initialEvent) { mutableStateOf(initialEvent?.eventTypeCustom ?: "") }
    
    // State for Step 2: Participants
    var minParticipants by remember(initialEvent) { mutableStateOf(initialEvent?.minParticipants) }
    var maxParticipants by remember(initialEvent) { mutableStateOf(initialEvent?.maxParticipants) }
    var expectedParticipants by remember(initialEvent) { mutableStateOf(initialEvent?.expectedParticipants) }
    
    // State for Step 3: Locations
    var locations by remember { mutableStateOf<List<PotentialLocation>>(emptyList()) }
    var showLocationDialog by remember { mutableStateOf(false) }
    
    // State for Step 4: Time Slots
    var timeSlots by remember(initialEvent) { mutableStateOf(initialEvent?.proposedSlots ?: emptyList()) }
    var editingTimeSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var showTimeSlotInput by remember { mutableStateOf(false) }
    
    val steps = listOf("Basic Info", "Participants", "Locations", "Time Slots")
    
    // Build current event
    fun buildEvent(): Event {
        return Event(
            id = initialEvent?.id ?: "event-${Clock.System.now().toEpochMilliseconds()}",
            title = title,
            description = description,
            organizerId = initialEvent?.organizerId ?: "current-user", // TODO: Get from auth
            participants = initialEvent?.participants ?: emptyList(),
            proposedSlots = timeSlots,
            deadline = initialEvent?.deadline ?: Clock.System.now().toString(), // TODO: Set proper deadline
            status = EventStatus.DRAFT,
            createdAt = initialEvent?.createdAt ?: Clock.System.now().toString(),
            updatedAt = Clock.System.now().toString(),
            finalDate = null,
            eventType = eventType,
            eventTypeCustom = if (eventType == EventType.CUSTOM) eventTypeCustom else null,
            minParticipants = minParticipants,
            maxParticipants = maxParticipants,
            expectedParticipants = expectedParticipants
        )
    }
    
    // Validation for each step
    fun isStepValid(step: Int): Boolean {
        return when (step) {
            0 -> title.isNotBlank() && description.isNotBlank() && 
                 (eventType != EventType.CUSTOM || eventTypeCustom.isNotBlank())
            1 -> {
                val minOk = minParticipants == null || minParticipants!! > 0
                val maxOk = maxParticipants == null || maxParticipants!! > 0
                val rangeOk = minParticipants == null || maxParticipants == null || maxParticipants!! >= minParticipants!!
                val expectedOk = expectedParticipants == null || expectedParticipants!! > 0
                minOk && maxOk && rangeOk && expectedOk
            }
            2 -> true // Locations are optional
            3 -> timeSlots.isNotEmpty()
            else -> false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Event") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) {
                            currentStep--
                        } else {
                            onCancel()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp
            ) {
                Column {
                    // Progress indicator
                    LinearProgressIndicator(
                        progress = { (currentStep + 1) / steps.size.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Step indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Step ${currentStep + 1} of ${steps.size}: ${steps[currentStep]}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Navigation buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        if (currentStep > 0) {
                            OutlinedButton(
                                onClick = { currentStep-- }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Previous")
                            }
                        }
                        
                        if (currentStep < steps.size - 1) {
                            FilledTonalButton(
                                onClick = {
                                    if (isStepValid(currentStep)) {
                                        onSaveStep(buildEvent())
                                        currentStep++
                                    }
                                },
                                enabled = isStepValid(currentStep)
                            ) {
                                Text("Next")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (isStepValid(currentStep)) {
                                        onComplete(buildEvent())
                                    }
                                },
                                enabled = isStepValid(currentStep)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Create Event")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    // Step 1: Basic Info
                    0 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Tell us about your event",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Event Title") },
                            placeholder = { Text("Summer Team Building") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = title.isBlank()
                        )
                        
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            placeholder = { Text("A fun day of activities and team bonding") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            isError = description.isBlank()
                        )
                        
                        EventTypeSelector(
                            selectedType = eventType,
                            customTypeValue = eventTypeCustom,
                            onTypeSelected = { eventType = it },
                            onCustomTypeChanged = { eventTypeCustom = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Step 2: Participants
                    1 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "How many people?",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        
                        Text(
                            text = "Give us an idea of how many participants to expect. This helps us suggest appropriate venues.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        ParticipantsEstimationCard(
                            minParticipants = minParticipants,
                            maxParticipants = maxParticipants,
                            expectedParticipants = expectedParticipants,
                            onMinChanged = { minParticipants = it },
                            onMaxChanged = { maxParticipants = it },
                            onExpectedChanged = { expectedParticipants = it }
                        )
                    }
                    
                    // Step 3: Locations
                    2 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Where could this happen?",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        
                        Text(
                            text = "Add potential locations. Participants will vote on these options later.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        PotentialLocationsList(
                            locations = locations,
                            onAddLocation = { showLocationDialog = true },
                            onRemoveLocation = { locationId ->
                                locations = locations.filter { it.id != locationId }
                            }
                        )
                    }
                    
                    // Step 4: Time Slots
                    3 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "When could this happen?",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        
                        Text(
                            text = "Add time slot options. Participants will vote on their availability.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (timeSlots.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = "⚠️ At least one time slot is required",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        
                        FilledTonalButton(
                            onClick = { showTimeSlotInput = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Time Slot")
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            timeSlots.forEach { slot ->
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        editingTimeSlot = slot
                                        showTimeSlotInput = true
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = when (slot.timeOfDay) {
                                                    TimeOfDay.ALL_DAY -> "All Day"
                                                    TimeOfDay.MORNING -> "Morning (8am-12pm)"
                                                    TimeOfDay.AFTERNOON -> "Afternoon (12pm-6pm)"
                                                    TimeOfDay.EVENING -> "Evening (6pm-12am)"
                                                    TimeOfDay.SPECIFIC -> "Specific Time"
                                                },
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            if (slot.start != null && slot.end != null) {
                                                Text(
                                                    text = "${slot.start} - ${slot.end}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = slot.timezone,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        
                                        IconButton(
                                            onClick = {
                                                timeSlots = timeSlots.filter { it.id != slot.id }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove slot",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Location dialog
    if (showLocationDialog) {
        LocationInputDialog(
            onDismiss = { showLocationDialog = false },
            onConfirm = { location ->
                locations = locations + location
                showLocationDialog = false
            },
            eventId = initialEvent?.id ?: "temp-event-id"
        )
    }
    
    // Time slot input dialog
    if (showTimeSlotInput) {
        AlertDialog(
            onDismissRequest = { 
                showTimeSlotInput = false
                editingTimeSlot = null
            },
            title = { Text(if (editingTimeSlot != null) "Edit Time Slot" else "Add Time Slot") },
            text = {
                TimeSlotInput(
                    timeSlot = editingTimeSlot,
                    onTimeSlotChanged = { slot ->
                        if (editingTimeSlot != null) {
                            timeSlots = timeSlots.map { if (it.id == editingTimeSlot?.id) slot else it }
                        } else {
                            timeSlots = timeSlots + slot
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimeSlotInput = false
                        editingTimeSlot = null
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }
}
