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
import com.guyghost.wakeve.ui.activity.ParticipantInfo
import kotlinx.datetime.Clock

/**
 * Dialog for adding or editing an equipment item
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    item: EquipmentItem?,
    participants: List<ParticipantInfo>,
    onDismiss: () -> Unit,
    onConfirm: (EquipmentItem) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var notes by remember { mutableStateOf(item?.notes ?: "") }
    var quantity by remember { mutableStateOf(item?.quantity?.toString() ?: "1") }
    var category by remember { mutableStateOf(item?.category ?: EquipmentCategory.OTHER) }
    var sharedCost by remember { mutableStateOf((item?.sharedCost?.div(100))?.toString() ?: "") }
    var assignedTo by remember { mutableStateOf(item?.assignedTo) }
    var status by remember { mutableStateOf(item?.status ?: ItemStatus.NEEDED) }

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
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
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
                            value = sharedCost,
                            onValueChange = { sharedCost = it },
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
                            value = getStatusLabel(status),
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
                                    text = { Text(getStatusLabel(st)) },
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val costInCents = sharedCost.toDoubleOrNull()?.times(100)?.toLong()
                    val now = Clock.System.now().toString()
                    val updatedItem = EquipmentItem(
                        id = item?.id ?: java.util.UUID.randomUUID().toString(),
                        eventId = item?.eventId ?: "",
                        name = name.trim(),
                        category = category,
                        quantity = quantity.toInt(),
                        assignedTo = assignedTo,
                        status = status,
                        sharedCost = costInCents,
                        notes = notes.trim().takeIf { it.isNotBlank() },
                        createdAt = item?.createdAt ?: now,
                        updatedAt = now
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
    participants: List<ParticipantInfo>,
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

                items(participants.size) { index ->
                    val participant = participants[index]
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
        "camping" to "Camping",
        "beach" to "Plage",
        "ski" to "Ski / Montagne",
        "hiking" to "Randonnée",
        "picnic" to "Pique-nique",
        "indoor" to "Intérieur"
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
        EquipmentCategory.SPORTS -> "Sport"
        EquipmentCategory.COOKING -> "Cuisine"
        EquipmentCategory.ELECTRONICS -> "Électronique"
        EquipmentCategory.SAFETY -> "Sécurité"
        EquipmentCategory.OTHER -> "Autre"
    }
}

private fun getStatusLabel(status: ItemStatus): String {
    return when (status) {
        ItemStatus.NEEDED -> "Requis"
        ItemStatus.ASSIGNED -> "Assigné"
        ItemStatus.CONFIRMED -> "Confirmé"
        ItemStatus.PACKED -> "Emballé"
        ItemStatus.CANCELLED -> "Annulé"
    }
}
