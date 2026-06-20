package com.guyghost.wakeve.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.PotentialLocation
import kotlinx.datetime.Clock

/**
 * Dialog for adding a potential location.
 * 
 * Fields:
 * - Name (required)
 * - Type (CITY, REGION, SPECIFIC_VENUE, ONLINE)
 * - Address (optional)
 * - Coordinates (optional, hidden for now - Phase 4 feature)
 * 
 * Material You design system component.
 * 
 * @param onDismiss Callback when dialog is dismissed
 * @param onConfirm Callback when location is confirmed
 * @param eventId Event ID for the location
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (PotentialLocation) -> Unit,
    eventId: String
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(LocationType.CITY) }
    var address by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    val isValid = name.isNotBlank()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Ajouter un lieu",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Ajoutez une option de lieu pour cet evenement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider()
                
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du lieu") },
                    placeholder = { Text("Ex : Paris, Hotel Royal, visio") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = name.isEmpty()
                )
                
                // Type selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = locationTypeLabel(selectedType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type de lieu") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Choisir un type de lieu"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        LocationType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(locationTypeLabel(type))
                                },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Address field (optional, hidden for online events)
                if (selectedType != LocationType.ONLINE) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Adresse (optionnel)") },
                        placeholder = { Text("Rue, ville, pays") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                }
                
                // Help text
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = locationTypeHelpText(selectedType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                HorizontalDivider()
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    
                    FilledTonalButton(
                        onClick = {
                            if (isValid) {
                                val location = PotentialLocation(
                                    id = "loc-${Clock.System.now().toEpochMilliseconds()}",
                                    eventId = eventId,
                                    name = name.trim(),
                                    locationType = selectedType,
                                    address = address.trim().takeIf { it.isNotBlank() },
                                    coordinates = null, // Phase 4 feature
                                    createdAt = Clock.System.now().toString()
                                )
                                onConfirm(location)
                            }
                        },
                        enabled = isValid
                    ) {
                        Text("Ajouter le lieu")
                    }
                }
            }
        }
    }
}

internal fun locationTypeLabel(type: LocationType): String =
    when (type) {
        LocationType.CITY -> "Ville"
        LocationType.REGION -> "Region"
        LocationType.SPECIFIC_VENUE -> "Lieu precis"
        LocationType.ONLINE -> "En ligne"
    }

internal fun locationTypeHelpText(type: LocationType): String =
    when (type) {
        LocationType.CITY -> "Utile si l'evenement peut se tenir n'importe ou dans la ville."
        LocationType.REGION -> "Utile pour une zone large, par exemple le sud de la France."
        LocationType.SPECIFIC_VENUE -> "Utile pour une adresse ou un etablissement precis."
        LocationType.ONLINE -> "Utile pour un evenement a distance via Zoom, Meet, etc."
    }
