package com.guyghost.wakeve.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.guyghost.wakeve.R
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Android-specific Time slot input with native DatePicker and TimePicker.
 * 
 * Features:
 * - TimeOfDay selector (ALL_DAY, MORNING, AFTERNOON, EVENING, SPECIFIC)
 * - Native Material 3 DatePicker dialog
 * - Native Material 3 TimePicker dialog (shown only for SPECIFIC)
 * - Timezone selector
 * - User-friendly date/time display
 * 
 * @param timeSlot Current time slot (nullable for new slots)
 * @param onTimeSlotChanged Callback when time slot changes
 * @param modifier Modifier for the component
 * @param enabled Whether the input is enabled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSlotInputAndroid(
    timeSlot: TimeSlot?,
    onTimeSlotChanged: (TimeSlot) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var timeOfDay by remember(timeSlot) { 
        mutableStateOf(timeSlot?.timeOfDay ?: TimeOfDay.ALL_DAY) 
    }
    
    // Parse existing dates or use defaults
    val now = Clock.System.now()
    val defaultDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    
    var selectedDate by remember(timeSlot) {
        mutableStateOf(
            timeSlot?.start?.let { 
                try {
                    Instant.parse(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
                } catch (e: Exception) {
                    defaultDate
                }
            } ?: defaultDate
        )
    }
    
    var startTime by remember(timeSlot) {
        mutableStateOf(
            timeSlot?.start?.let {
                try {
                    Instant.parse(it).toLocalDateTime(TimeZone.currentSystemDefault()).time
                } catch (e: Exception) {
                    LocalTime(9, 0)
                }
            } ?: LocalTime(9, 0)
        )
    }
    
    var endTime by remember(timeSlot) {
        mutableStateOf(
            timeSlot?.end?.let {
                try {
                    Instant.parse(it).toLocalDateTime(TimeZone.currentSystemDefault()).time
                } catch (e: Exception) {
                    LocalTime(18, 0)
                }
            } ?: LocalTime(18, 0)
        )
    }
    
    var timezone by remember(timeSlot) { 
        mutableStateOf(timeSlot?.timezone ?: TimeZone.currentSystemDefault().id) 
    }
    
    // Dialog states
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var timeOfDayExpanded by remember { mutableStateOf(false) }
    var timezoneExpanded by remember { mutableStateOf(false) }
    
    // Common timezones
    val commonTimezones = listOf(
        TimeZone.currentSystemDefault().id,
        "UTC",
        "Europe/Paris",
        "Europe/London",
        "America/New_York",
        "America/Los_Angeles",
        "Asia/Tokyo",
        "Australia/Sydney"
    ).distinct()
    
    // Format helpers
    val dateFormatter = remember { 
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    }
    val timeFormatter = remember {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
    }
    
    fun formatDate(date: LocalDate): String {
        return try {
            val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
            javaDate.format(dateFormatter)
        } catch (e: Exception) {
            "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
        }
    }
    
    fun formatTime(time: LocalTime): String {
        return try {
            val javaTime = java.time.LocalTime.of(time.hour, time.minute)
            javaTime.format(timeFormatter)
        } catch (e: Exception) {
            "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
        }
    }
    
    // Update callback when values change
    LaunchedEffect(timeOfDay, selectedDate, startTime, endTime, timezone) {
        val tz = TimeZone.of(timezone)
        
        val (startIso, endIso) = when (timeOfDay) {
            TimeOfDay.ALL_DAY -> {
                val start = selectedDate.atTime(0, 0).toInstant(tz).toString()
                val end = selectedDate.atTime(23, 59).toInstant(tz).toString()
                start to end
            }
            TimeOfDay.MORNING -> {
                val start = selectedDate.atTime(8, 0).toInstant(tz).toString()
                val end = selectedDate.atTime(12, 0).toInstant(tz).toString()
                start to end
            }
            TimeOfDay.AFTERNOON -> {
                val start = selectedDate.atTime(12, 0).toInstant(tz).toString()
                val end = selectedDate.atTime(18, 0).toInstant(tz).toString()
                start to end
            }
            TimeOfDay.EVENING -> {
                val start = selectedDate.atTime(18, 0).toInstant(tz).toString()
                val end = selectedDate.atTime(23, 59).toInstant(tz).toString()
                start to end
            }
            TimeOfDay.SPECIFIC -> {
                val start = selectedDate.atTime(startTime.hour, startTime.minute).toInstant(tz).toString()
                val end = selectedDate.atTime(endTime.hour, endTime.minute).toInstant(tz).toString()
                start to end
            }
        }
        
        val newSlot = TimeSlot(
            id = timeSlot?.id ?: "slot-${Clock.System.now().toEpochMilliseconds()}",
            start = startIso,
            end = endIso,
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
                    text = stringResource(R.string.time_preference),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Date picker field
            OutlinedTextField(
                value = formatDate(selectedDate),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.select_date)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { showDatePicker = true },
                enabled = false, // Disabled to make entire field clickable
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            
            // Time of Day selector
            ExposedDropdownMenuBox(
                expanded = timeOfDayExpanded,
                onExpandedChange = { if (enabled) timeOfDayExpanded = it }
            ) {
                OutlinedTextField(
                    value = when (timeOfDay) {
                        TimeOfDay.ALL_DAY -> stringResource(R.string.time_of_day_all_day)
                        TimeOfDay.MORNING -> stringResource(R.string.time_of_day_morning)
                        TimeOfDay.AFTERNOON -> stringResource(R.string.time_of_day_afternoon)
                        TimeOfDay.EVENING -> stringResource(R.string.time_of_day_evening)
                        TimeOfDay.SPECIFIC -> stringResource(R.string.time_of_day_specific)
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.time_of_day)) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
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
                                            TimeOfDay.ALL_DAY -> stringResource(R.string.all_day)
                                            TimeOfDay.MORNING -> stringResource(R.string.morning)
                                            TimeOfDay.AFTERNOON -> stringResource(R.string.afternoon)
                                            TimeOfDay.EVENING -> stringResource(R.string.evening)
                                            TimeOfDay.SPECIFIC -> stringResource(R.string.specific_time)
                                        }
                                    )
                                    if (tod != TimeOfDay.SPECIFIC && tod != TimeOfDay.ALL_DAY) {
                                        Text(
                                            text = when (tod) {
                                                TimeOfDay.MORNING -> stringResource(R.string.morning_short)
                                                TimeOfDay.AFTERNOON -> stringResource(R.string.afternoon_short)
                                                TimeOfDay.EVENING -> stringResource(R.string.evening_short)
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
                    // Start time picker
                    OutlinedTextField(
                        value = formatTime(startTime),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.start_time)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { showStartTimePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    
                    // End time picker
                    OutlinedTextField(
                        value = formatTime(endTime),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.end_time)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { showEndTimePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    label = { Text(stringResource(R.string.timezone)) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
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
                        TimeOfDay.ALL_DAY -> stringResource(R.string.time_hint_all_day)
                        TimeOfDay.MORNING -> stringResource(R.string.time_hint_morning)
                        TimeOfDay.AFTERNOON -> stringResource(R.string.time_hint_afternoon)
                        TimeOfDay.EVENING -> stringResource(R.string.time_hint_evening)
                        TimeOfDay.SPECIFIC -> stringResource(R.string.time_hint_specific)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDays() * 24 * 60 * 60 * 1000L
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = Instant.fromEpochMilliseconds(millis)
                            selectedDate = instant.toLocalDateTime(TimeZone.UTC).date
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Start Time Picker Dialog
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startTime.hour,
            initialMinute = startTime.minute,
            is24Hour = true
        )
        
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                startTime = LocalTime(timePickerState.hour, timePickerState.minute)
                showStartTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
    
    // End Time Picker Dialog
    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = endTime.hour,
            initialMinute = endTime.minute,
            is24Hour = true
        )
        
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                endTime = LocalTime(timePickerState.hour, timePickerState.minute)
                showEndTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

/**
 * Custom TimePickerDialog since Material 3 doesn't provide one by default.
 */
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_time),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                content()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = onConfirm) {
                        Text(stringResource(R.string.ok))
                    }
                }
            }
        }
    }
}
