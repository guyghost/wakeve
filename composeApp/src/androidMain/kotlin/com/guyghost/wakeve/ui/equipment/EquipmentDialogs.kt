package com.guyghost.wakeve.ui.equipment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.*
import kotlinx.datetime.Clock

/**
 * Dialog for adding or editing an equipment item
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    item: EquipmentItem?,
    participants: List<Participant>,
    onDismiss: () -> Unit,
    onConfirm: (EquipmentItem) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var quantity by remember { mutableStateOf(item?.quantity?.toString() ?: "1") }
    var category by remember { mutableStateOf(item?.category ?: EquipmentCategory.OTHER) }
    var estimatedCost by remember { mutableStateOf((item?.estimatedCost?.div(100))?.toString() ?: "") }
    var assignedTo by remember { mutableStateOf(item?.assignedTo) }
    var status by remember { mutableStateOf(item?.status ?: ItemStatus.NEEDED) }
    var sharedItem by remember { mutableStateOf(item?.sharedItem ?: false) }
    
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var showParticipantMenu by remember { mutableStateOf(false) }
    
    val isValid = name.isNotBlank() && 
                  quantity.toIntOrNull() != null && 
                  quantity.toInt() > 0
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Ajouter un équipement" else "Modifier l'équipement") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Quantité *") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = estimatedCost,
                            onValueChange = { estimatedCost = it },
                            label = { Text("Coût (€)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            suffix = { Text("€") }
                        )
                    }
                }
                
                item {
                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = showCategoryMenu,
                        onExpandedChange = { showCategoryMenu = it }
                    ) {
                        OutlinedTextField(
                            value = getCategoryLabel(category),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Catégorie") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showCategoryMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            EquipmentCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(getCategoryLabel(cat)) },
                                    onClick = {
                                        category = cat
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    // Status Dropdown
                    ExposedDropdownMenuBox(
                        expanded = showStatusMenu,
                        onExpandedChange = { showStatusMenu = it }
                    ) {
                        OutlinedTextField(
                            value = status.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Statut") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showStatusMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            ItemStatus.entries.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st.name) },
                                    onClick = {
                                        status = st
                                        showStatusMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    // Assigned Participant Dropdown
                    ExposedDropdownMenuBox(
                        expanded = showParticipantMenu,
                        onExpandedChange = { showParticipantMenu = it }
                    ) {
                        OutlinedTextField(
                            value = assignedTo?.let { id ->
                                participants.find { it.id == id }?.name ?: "Non assigné"
                            } ?: "Non assigné",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigné à") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showParticipantMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showParticipantMenu,
                            onDismissRequest = { showParticipantMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Non assigné") },
                                onClick = {
                                    assignedTo = null
                                    showParticipantMenu = false
                                }
                            )
                            
                            participants.forEach { participant ->
                                DropdownMenuItem(
                                    text = { Text(participant.name) },
                                    onClick = {
                                        assignedTo = participant.id
                                        showParticipantMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = sharedItem,
                            onCheckedChange = { sharedItem = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Équipement partagé par le groupe")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val costInCents = estimatedCost.toDoubleOrNull()?.times(100)?.toLong() ?: 0L
                    val updatedItem = EquipmentItem(
                        id = item?.id ?: "",
                        eventId = item?.eventId ?: "",
                        name = name.trim(),
                        description = description.trim(),
                        category = category,
                        quantity = quantity.toInt(),
                        assignedTo = assignedTo,
                        status = status,
                        estimatedCost = costInCents,
                        sharedItem = sharedItem,
                        createdAt = item?.createdAt ?: Clock.System.now().toString(),
                        updatedAt = Clock.System.now().toString()
                    )
                    onConfirm(updatedItem)
                },
                enabled = isValid
            ) {
                Text(if (item == null) "Ajouter" else "Modifier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Dialog for assigning an item to a participant
 */
@Composable
fun AssignItemDialog(
    item: EquipmentItem,
    participants: List<Participant>,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var selectedParticipant by remember { mutableStateOf(item.assignedTo) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assigner « ${item.name} »") },
        text = {
            LazyColumn {
                item {
                    ListItem(
                        headlineContent = { Text("Non assigné") },
                        leadingContent = {
                            RadioButton(
                                selected = selectedParticipant == null,
                                onClick = { selectedParticipant = null }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                items(participants) { participant ->
                    ListItem(
                        headlineContent = { Text(participant.name) },
                        leadingContent = {
                            RadioButton(
                                selected = selectedParticipant == participant.id,
                                onClick = { selectedParticipant = participant.id }
                            )
                        },
                        trailingContent = {
                            if (selectedParticipant == participant.id) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedParticipant) }) {
                Text("Confirmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Dialog for auto-generating equipment checklist
 */
@Composable
fun AutoGenerateDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedType by remember { mutableStateOf("camping") }
    
    val eventTypes = listOf(
        "camping" to "Camping 🏕️",
        "beach" to "Plage 🏖️",
        "ski" to "Ski / Montagne ⛷️",
        "hiking" to "Randonnée 🥾",
        "city" to "Ville / Urbain 🏙️",
        "bbq" to "BBQ / Pique-nique 🍖",
        "road_trip" to "Road Trip 🚗",
        "festival" to "Festival 🎪"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Générer une liste d'équipement") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Sélectionnez le type d'événement pour générer une liste d'équipement adaptée :",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                LazyColumn {
                    items(eventTypes) { (type, label) ->
                        ListItem(
                            headlineContent = { Text(label) },
                            leadingContent = {
                                RadioButton(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedType) }) {
                Text("Générer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

private fun getCategoryLabel(category: EquipmentCategory): String {
    return when (category) {
        EquipmentCategory.CAMPING -> "Camping"
        EquipmentCategory.COOKING -> "Cuisine"
        EquipmentCategory.CLOTHING -> "Vêtements"
        EquipmentCategory.SPORT -> "Sport"
        EquipmentCategory.ENTERTAINMENT -> "Divertissement"
        EquipmentCategory.HYGIENE -> "Hygiène"
        EquipmentCategory.MEDICAL -> "Médical"
        EquipmentCategory.SAFETY -> "Sécurité"
        EquipmentCategory.ELECTRONICS -> "Électronique"
        EquipmentCategory.OTHER -> "Autre"
    }
}
