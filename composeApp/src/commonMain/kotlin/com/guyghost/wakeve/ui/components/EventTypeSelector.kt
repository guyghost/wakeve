package com.guyghost.wakeve.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        // Dropdown button
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = selectedType.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Event Type") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select event type"
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
                        text = { Text(type.displayName) },
                        onClick = {
                            onTypeSelected(type)
                            expanded = false
                        },
                        leadingIcon = {
                            if (type == selectedType) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected"
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
                label = { Text("Custom Event Type") },
                placeholder = { Text("e.g., Charity Gala, Product Launch") },
                supportingText = { Text("Describe your event type") },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                isError = customTypeValue.isBlank()
            )
            
            if (customTypeValue.isBlank()) {
                Text(
                    text = "Custom event type is required",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}
