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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.R
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.ui.components.EventTypeSelector
import com.guyghost.wakeve.ui.components.LocationInputDialog
import com.guyghost.wakeve.ui.components.PotentialLocationsList
import com.guyghost.wakeve.ui.components.TimeSlotInputAndroid
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Multi-step wizard for creating an event in DRAFT phase.
 * 
 * Steps:
 * 1. Basic Info (title, description, type)
 * 2. Participants + Potential Locations
 * 3. Time Slots
 * 4. Preview (summary of all information)
 * 
 * Features:
 * - Auto-save on each step
 * - Validation before proceeding
 * - Material You design with progress indicator
 * - Preview step before final creation
 * 
 * @param initialEvent Initial event data (for editing)
 * @param userId The current user ID (organizer)
 * @param onSaveStep Callback when moving between steps (for auto-save)
 * @param onComplete Callback when wizard is complete
 * @param onCancel Callback when wizard is cancelled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftEventWizard(
    initialEvent: Event?,
    userId: String,
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
    
    // State for Step 2: Participants (single field)
    var participantCount by remember(initialEvent) { mutableStateOf(initialEvent?.expectedParticipants?.toString() ?: "") }
    
    // State for Step 2: Locations
    var locations by remember { mutableStateOf<List<PotentialLocation>>(emptyList()) }
    var showLocationDialog by remember { mutableStateOf(false) }
    
    // State for Step 3: Time Slots
    var timeSlots by remember(initialEvent) { mutableStateOf(initialEvent?.proposedSlots ?: emptyList()) }
    var editingTimeSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var showTimeSlotInput by remember { mutableStateOf(false) }
    
    val steps = listOf(
        stringResource(R.string.step_basic_info),
        stringResource(R.string.step_participants_locations),
        stringResource(R.string.step_time_slots),
        stringResource(R.string.step_preview)
    )
    
    // Formatters for preview
    val dateFormatter = remember { 
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    }
    val timeFormatter = remember {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
    }
    
    // Build current event
    fun buildEvent(): Event {
        val participantCountInt = participantCount.toIntOrNull()
        return Event(
            id = initialEvent?.id ?: "event-${Clock.System.now().toEpochMilliseconds()}",
            title = title,
            description = description,
            organizerId = initialEvent?.organizerId ?: userId,
            participants = initialEvent?.participants ?: emptyList(),
            proposedSlots = timeSlots,
            deadline = initialEvent?.deadline ?: Clock.System.now().toString(), // TODO: Set proper deadline
            status = EventStatus.DRAFT,
            createdAt = initialEvent?.createdAt ?: Clock.System.now().toString(),
            updatedAt = Clock.System.now().toString(),
            finalDate = null,
            eventType = eventType,
            eventTypeCustom = if (eventType == EventType.CUSTOM) eventTypeCustom else null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = participantCountInt
        )
    }
    
    // Validation for each step
    fun isStepValid(step: Int): Boolean {
        return when (step) {
            0 -> title.isNotBlank() && description.isNotBlank() && 
                 (eventType != EventType.CUSTOM || eventTypeCustom.isNotBlank())
            1 -> {
                // Participant count is optional, but if provided must be positive
                val count = participantCount.toIntOrNull()
                participantCount.isBlank() || (count != null && count > 0)
            }
            2 -> timeSlots.isNotEmpty()
            3 -> true // Preview is always valid if we got here
            else -> false
        }
    }
    
    // Helper to get event type display name
    @Composable
    fun getEventTypeDisplayName(type: EventType, customName: String?): String {
        return when (type) {
            EventType.BIRTHDAY -> stringResource(R.string.event_type_birthday)
            EventType.WEDDING -> stringResource(R.string.event_type_wedding)
            EventType.TEAM_BUILDING -> stringResource(R.string.event_type_team_building)
            EventType.CONFERENCE -> stringResource(R.string.event_type_conference)
            EventType.WORKSHOP -> stringResource(R.string.event_type_workshop)
            EventType.PARTY -> stringResource(R.string.event_type_party)
            EventType.SPORTS_EVENT -> stringResource(R.string.event_type_sports_event)
            EventType.CULTURAL_EVENT -> stringResource(R.string.event_type_cultural_event)
            EventType.FAMILY_GATHERING -> stringResource(R.string.event_type_family_gathering)
            EventType.SPORT_EVENT -> stringResource(R.string.event_type_sport_event)
            EventType.OUTDOOR_ACTIVITY -> stringResource(R.string.event_type_outdoor_activity)
            EventType.FOOD_TASTING -> stringResource(R.string.event_type_food_tasting)
            EventType.TECH_MEETUP -> stringResource(R.string.event_type_tech_meetup)
            EventType.WELLNESS_EVENT -> stringResource(R.string.event_type_wellness_event)
            EventType.CREATIVE_WORKSHOP -> stringResource(R.string.event_type_creative_workshop)
            EventType.CUSTOM -> customName ?: stringResource(R.string.event_type_other)
            EventType.OTHER -> stringResource(R.string.event_type_other)
        }
    }
    
    // Helper to format time slot for preview
    fun formatTimeSlotForPreview(slot: TimeSlot): Pair<String, String> {
        val dateStr = slot.start?.let { startIso ->
            try {
                val instant = Instant.parse(startIso)
                val localDate = instant.toLocalDateTime(TimeZone.of(slot.timezone)).date
                val javaDate = java.time.LocalDate.of(localDate.year, localDate.monthNumber, localDate.dayOfMonth)
                javaDate.format(dateFormatter)
            } catch (e: Exception) {
                startIso
            }
        } ?: ""
        
        val timeStr = when (slot.timeOfDay) {
            TimeOfDay.ALL_DAY -> "Toute la journée"
            TimeOfDay.MORNING -> "Matin (8h-12h)"
            TimeOfDay.AFTERNOON -> "Après-midi (12h-18h)"
            TimeOfDay.EVENING -> "Soir (18h-minuit)"
            TimeOfDay.SPECIFIC -> {
                try {
                    val startInstant = Instant.parse(slot.start!!)
                    val endInstant = Instant.parse(slot.end!!)
                    val startLocalTime = startInstant.toLocalDateTime(TimeZone.of(slot.timezone)).time
                    val endLocalTime = endInstant.toLocalDateTime(TimeZone.of(slot.timezone)).time
                    val javaStartTime = java.time.LocalTime.of(startLocalTime.hour, startLocalTime.minute)
                    val javaEndTime = java.time.LocalTime.of(endLocalTime.hour, endLocalTime.minute)
                    "${javaStartTime.format(timeFormatter)} - ${javaEndTime.format(timeFormatter)}"
                } catch (e: Exception) {
                    "Heure spécifique"
                }
            }
        }
        
        return dateStr to timeStr
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_event)) },
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
                            contentDescription = stringResource(R.string.back)
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
                            text = stringResource(R.string.step_indicator, currentStep + 1, steps.size, steps[currentStep]),
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
                        // Show "Previous" on steps 1-2, "Save Draft" on preview step (3)
                        if (currentStep > 0 && currentStep < steps.size - 1) {
                            // Steps 1-2: Show Previous button
                            OutlinedButton(
                                onClick = { currentStep-- }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.previous))
                            }
                        } else if (currentStep == steps.size - 1) {
                            // Preview step: Show Save Draft button
                            OutlinedButton(
                                onClick = {
                                    onSaveStep(buildEvent())
                                    onCancel() // Return to main page
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.save_draft))
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
                                Text(stringResource(R.string.next))
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
                                Text(stringResource(R.string.create_event))
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
                            text = stringResource(R.string.tell_us_about_event),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(stringResource(R.string.event_title)) },
                            placeholder = { Text(stringResource(R.string.event_title_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = title.isBlank()
                        )
                        
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text(stringResource(R.string.event_description)) },
                            placeholder = { Text(stringResource(R.string.event_description_hint)) },
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
                    
                    // Step 2: Participants + Locations
                    1 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = stringResource(R.string.how_many_people),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        
                        Text(
                            text = stringResource(R.string.help_us_plan),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        OutlinedTextField(
                            value = participantCount,
                            onValueChange = { newValue ->
                                // Only allow digits
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    participantCount = newValue
                                }
                            },
                            label = { Text(stringResource(R.string.participant_count)) },
                            placeholder = { Text(stringResource(R.string.participant_count_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = participantCount.isNotEmpty() && (participantCount.toIntOrNull() ?: 0) <= 0
                        )
                        
                        // Locations section
                        Text(
                            text = stringResource(R.string.where_could_this_happen),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        
                        Text(
                            text = stringResource(R.string.add_potential_locations),
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
                    
                    // Step 3: Time Slots
                    2 -> Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.when_should_it_happen),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        
                        Text(
                            text = stringResource(R.string.add_at_least_one),
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
                                    text = stringResource(R.string.at_least_one_required),
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        
                        FilledTonalButton(
                            onClick = { showTimeSlotInput = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.add_time_slot))
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            // Display formatted date
                                            val formattedDate = slot.start?.let { startIso ->
                                                try {
                                                    val instant = Instant.parse(startIso)
                                                    val localDate = instant.toLocalDateTime(TimeZone.of(slot.timezone)).date
                                                    val javaDate = java.time.LocalDate.of(localDate.year, localDate.monthNumber, localDate.dayOfMonth)
                                                    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
                                                    javaDate.format(dateFormatter)
                                                } catch (e: Exception) {
                                                    startIso
                                                }
                                            } ?: ""
                                            
                                            Text(
                                                text = formattedDate,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            
                                            Text(
                                                text = when (slot.timeOfDay) {
                                                    TimeOfDay.ALL_DAY -> stringResource(R.string.time_of_day_all_day)
                                                    TimeOfDay.MORNING -> stringResource(R.string.time_of_day_morning)
                                                    TimeOfDay.AFTERNOON -> stringResource(R.string.time_of_day_afternoon)
                                                    TimeOfDay.EVENING -> stringResource(R.string.time_of_day_evening)
                                                    TimeOfDay.SPECIFIC -> {
                                                        // Display start-end times for SPECIFIC
                                                        try {
                                                            val startInstant = Instant.parse(slot.start!!)
                                                            val endInstant = Instant.parse(slot.end!!)
                                                            val startLocalTime = startInstant.toLocalDateTime(TimeZone.of(slot.timezone)).time
                                                            val endLocalTime = endInstant.toLocalDateTime(TimeZone.of(slot.timezone)).time
                                                            val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
                                                            val javaStartTime = java.time.LocalTime.of(startLocalTime.hour, startLocalTime.minute)
                                                            val javaEndTime = java.time.LocalTime.of(endLocalTime.hour, endLocalTime.minute)
                                                            "${javaStartTime.format(timeFormatter)} - ${javaEndTime.format(timeFormatter)}"
                                                        } catch (e: Exception) {
                                                            stringResource(R.string.time_of_day_specific)
                                                        }
                                                    }
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            
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
                                                contentDescription = stringResource(R.string.remove_slot),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Step 4: Preview
                    3 -> Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Preview,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.preview_title),
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                        
                        Text(
                            text = stringResource(R.string.preview_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Event Preview Card
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Title Section
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                HorizontalDivider()
                                
                                // Description Section
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = stringResource(R.string.event_description),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                
                                // Event Type Section
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Category,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = stringResource(R.string.event_type),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = getEventTypeDisplayName(eventType, eventTypeCustom.takeIf { it.isNotBlank() }),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                
                                // Participants Section
                                if (participantCount.isNotBlank()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.People,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = stringResource(R.string.participant_count),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = stringResource(R.string.preview_participants_count, participantCount),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                                
                                // Locations Section
                                if (locations.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = stringResource(R.string.preview_locations),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            locations.forEach { location ->
                                                Text(
                                                    text = "• ${location.name}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Time Slots Section
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = stringResource(R.string.preview_time_slots),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        timeSlots.forEach { slot ->
                                            val (dateStr, timeStr) = formatTimeSlotForPreview(slot)
                                            Text(
                                                text = "• $dateStr - $timeStr",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Edit hint
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.preview_edit_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
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
        // Track the current slot being created/edited
        var pendingTimeSlot by remember(editingTimeSlot) { mutableStateOf(editingTimeSlot) }
        
        AlertDialog(
            onDismissRequest = { 
                showTimeSlotInput = false
                editingTimeSlot = null
            },
            title = { Text(if (editingTimeSlot != null) stringResource(R.string.edit_time_slot_title) else stringResource(R.string.add_time_slot_title)) },
            text = {
                TimeSlotInputAndroid(
                    timeSlot = editingTimeSlot,
                    onTimeSlotChanged = { slot ->
                        // Only store the pending slot, don't add to list yet
                        pendingTimeSlot = slot
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Only add/update the slot when user confirms
                        pendingTimeSlot?.let { slot ->
                            if (editingTimeSlot != null) {
                                timeSlots = timeSlots.map { if (it.id == editingTimeSlot?.id) slot else it }
                            } else {
                                timeSlots = timeSlots + slot
                            }
                        }
                        showTimeSlotInput = false
                        editingTimeSlot = null
                    }
                ) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }
}
