package com.guyghost.wakeve.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.R
import com.guyghost.wakeve.models.EventType

/**
 * Event type selector with dropdown menu.
 * Shows all predefined event types + custom option.
 * 
 * Material You design system component.
 * 
 * @param selectedType Currently selected event type
 * @param customTypeValue Custom type text (if selectedType is CUSTOM)
 * @param onTypeSelected Callback when a predefined type is selected
 * @param onCustomTypeChanged Callback when custom type text changes
 * @param modifier Modifier for the component
 * @param enabled Whether the selector is enabled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTypeSelector(
    selectedType: EventType,
    customTypeValue: String,
    onTypeSelected: (EventType) -> Unit,
    onCustomTypeChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        val selectedTypeName = eventTypeDisplayName(selectedType)

        // Dropdown button
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = selectedTypeName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.event_type)) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.select_event_type)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = enabled,
                colors = OutlinedTextFieldDefaults.colors()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                EventType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(eventTypeDisplayName(type)) },
                        onClick = {
                            onTypeSelected(type)
                            expanded = false
                        },
                        leadingIcon = {
                            if (type == selectedType) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.selected)
                                )
                            }
                        }
                    )
                }
            }
        }
        
        // Custom type text field (shown only if CUSTOM selected)
        if (selectedType == EventType.CUSTOM) {
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = customTypeValue,
                onValueChange = onCustomTypeChanged,
                label = { Text(stringResource(R.string.custom_event_type)) },
                placeholder = { Text(stringResource(R.string.custom_event_type_example)) },
                supportingText = { Text(stringResource(R.string.custom_event_type_supporting)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                isError = customTypeValue.isBlank()
            )
            
            if (customTypeValue.isBlank()) {
                Text(
                    text = stringResource(R.string.custom_event_type_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun eventTypeDisplayName(type: EventType): String {
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
        EventType.CUSTOM -> stringResource(R.string.event_type_custom)
        EventType.OTHER -> stringResource(R.string.event_type_other)
    }
}
