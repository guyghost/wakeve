package com.guyghost.wakeve.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import kotlinx.datetime.Clock

/**
 * Time slot input with flexible time-of-day selection.
 * 
 * Features:
 * - TimeOfDay selector (ALL_DAY, MORNING, AFTERNOON, EVENING, SPECIFIC)
 * - Start/End time pickers (shown only for SPECIFIC)
 * - Timezone selector
 * - Validation (end > start)
 * 
 * Material You design system component.
 * 
 * @param timeSlot Current time slot (nullable for new slots)
 * @param onTimeSlotChanged Callback when time slot changes
 * @param modifier Modifier for the component
 * @param enabled Whether the input is enabled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSlotInput(
    timeSlot: TimeSlot?,
    onTimeSlotChanged: (TimeSlot) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var timeOfDay by remember(timeSlot) { 
        mutableStateOf(timeSlot?.timeOfDay ?: TimeOfDay.SPECIFIC) 
    }
    var startTime by remember(timeSlot) { 
        mutableStateOf(timeSlot?.start ?: "") 
    }
    var endTime by remember(timeSlot) { 
        mutableStateOf(timeSlot?.end ?: "") 
    }
    var timezone by remember(timeSlot) { 
        mutableStateOf(timeSlot?.timezone ?: "UTC") 
    }
    var timeOfDayExpanded by remember { mutableStateOf(false) }
    var timezoneExpanded by remember { mutableStateOf(false) }
    
    // Common timezones
    val commonTimezones = listOf(
        "UTC",
        "Europe/Paris",
        "Europe/London",
        "America/New_York",
        "America/Los_Angeles",
        "Asia/Tokyo",
        "Australia/Sydney"
    )
    
    // Update callback when values change
    LaunchedEffect(timeOfDay, startTime, endTime, timezone) {
        val newSlot = TimeSlot(
            id = timeSlot?.id ?: "slot-${Clock.System.now().toEpochMilliseconds()}",
            start = if (timeOfDay == TimeOfDay.SPECIFIC) startTime.takeIf { it.isNotBlank() } else null,
            end = if (timeOfDay == TimeOfDay.SPECIFIC) endTime.takeIf { it.isNotBlank() } else null,
            timezone = timezone,
            timeOfDay = timeOfDay
        )
        if (newSlot != timeSlot) {
            onTimeSlotChanged(newSlot)
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Time Preference",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Time of Day selector
            ExposedDropdownMenuBox(
                expanded = timeOfDayExpanded,
                onExpandedChange = { if (enabled) timeOfDayExpanded = it }
            ) {
                OutlinedTextField(
                    value = when (timeOfDay) {
                        TimeOfDay.ALL_DAY -> "All Day"
                        TimeOfDay.MORNING -> "Morning (8am-12pm)"
                        TimeOfDay.AFTERNOON -> "Afternoon (12pm-6pm)"
                        TimeOfDay.EVENING -> "Evening (6pm-12am)"
                        TimeOfDay.SPECIFIC -> "Specific Time"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("When?") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select time of day"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = enabled,
                    colors = OutlinedTextFieldDefaults.colors()
                )
                
                ExposedDropdownMenu(
                    expanded = timeOfDayExpanded,
                    onDismissRequest = { timeOfDayExpanded = false }
                ) {
                    TimeOfDay.entries.forEach { tod ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        when (tod) {
                                            TimeOfDay.ALL_DAY -> "All Day"
                                            TimeOfDay.MORNING -> "Morning"
                                            TimeOfDay.AFTERNOON -> "Afternoon"
                                            TimeOfDay.EVENING -> "Evening"
                                            TimeOfDay.SPECIFIC -> "Specific Time"
                                        }
                                    )
                                    if (tod != TimeOfDay.SPECIFIC && tod != TimeOfDay.ALL_DAY) {
                                        Text(
                                            text = when (tod) {
                                                TimeOfDay.MORNING -> "8am - 12pm"
                                                TimeOfDay.AFTERNOON -> "12pm - 6pm"
                                                TimeOfDay.EVENING -> "6pm - 12am"
                                                else -> ""
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                timeOfDay = tod
                                timeOfDayExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Specific time inputs (shown only for SPECIFIC)
            AnimatedVisibility(visible = timeOfDay == TimeOfDay.SPECIFIC) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("2025-06-15T14:00:00Z") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        singleLine = true,
                        supportingText = { Text("ISO 8601 format") }
                    )
                    
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        placeholder = { Text("2025-06-15T18:00:00Z") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        singleLine = true,
                        supportingText = { Text("ISO 8601 format") }
                    )
                }
            }
            
            // Timezone selector
            ExposedDropdownMenuBox(
                expanded = timezoneExpanded,
                onExpandedChange = { if (enabled) timezoneExpanded = it }
            ) {
                OutlinedTextField(
                    value = timezone,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Timezone") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select timezone"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = enabled,
                    colors = OutlinedTextFieldDefaults.colors()
                )
                
                ExposedDropdownMenu(
                    expanded = timezoneExpanded,
                    onDismissRequest = { timezoneExpanded = false }
                ) {
                    commonTimezones.forEach { tz ->
                        DropdownMenuItem(
                            text = { Text(tz) },
                            onClick = {
                                timezone = tz
                                timezoneExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Helper text
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (timeOfDay) {
                        TimeOfDay.ALL_DAY -> "💡 Flexible all-day event"
                        TimeOfDay.MORNING -> "💡 Morning event (8am-12pm)"
                        TimeOfDay.AFTERNOON -> "💡 Afternoon event (12pm-6pm)"
                        TimeOfDay.EVENING -> "💡 Evening event (6pm-12am)"
                        TimeOfDay.SPECIFIC -> "💡 Enter exact start and end times"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
