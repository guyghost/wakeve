package com.guyghost.wakeve.ui.equipment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.EquipmentCategory
import com.guyghost.wakeve.models.EquipmentItem
import com.guyghost.wakeve.models.ItemStatus
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
        title = { Text(equipmentItemDialogTitle(isNewItem = item == null)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(equipmentItemNameLabel()) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(equipmentItemNotesLabel()) },
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
                            label = { Text(equipmentItemQuantityLabel()) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = sharedCost,
                            onValueChange = { sharedCost = it },
                            label = { Text(equipmentItemSharedCostLabel()) },
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
                            value = equipmentCategoryLabel(category),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(equipmentItemCategoryLabel()) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showCategoryMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(
                                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                )
                        )

                        ExposedDropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            EquipmentCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(equipmentCategoryLabel(cat)) },
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
                            value = equipmentStatusLabel(status),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(equipmentItemStatusLabel()) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showStatusMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(
                                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                )
                        )

                        ExposedDropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            ItemStatus.entries.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(equipmentStatusLabel(st)) },
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
                                participants.find { it.id == id }?.name ?: equipmentUnassignedLabel()
                            } ?: equipmentUnassignedLabel(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(equipmentAssignedToLabel()) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showParticipantMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(
                                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                )
                        )

                        ExposedDropdownMenu(
                            expanded = showParticipantMenu,
                            onDismissRequest = { showParticipantMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(equipmentUnassignedLabel()) },
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
                Text(equipmentItemDialogConfirmLabel(isNewItem = item == null))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(equipmentCancelActionLabel())
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
        title = { Text(equipmentAssignDialogTitle(item.name)) },
        text = {
            LazyColumn {
                item {
                    ListItem(
                        headlineContent = { Text(equipmentUnassignedLabel()) },
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
                Text(equipmentConfirmActionLabel())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(equipmentCancelActionLabel())
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
        title = { Text(equipmentAutoGenerateTitle()) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = equipmentAutoGenerateDescription(),
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
                Text(equipmentPrepareActionLabel())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(equipmentCancelActionLabel())
            }
        }
    )
}

internal fun equipmentItemDialogTitle(isNewItem: Boolean): String =
    if (isNewItem) "Ajouter un équipement" else "Modifier l'équipement"

internal fun equipmentItemNameLabel(): String = "Nom requis"

internal fun equipmentItemNotesLabel(): String = "Notes"

internal fun equipmentItemQuantityLabel(): String = "Quantité requise"

internal fun equipmentItemSharedCostLabel(): String = "Coût partagé (€)"

internal fun equipmentItemCategoryLabel(): String = "Catégorie"

internal fun equipmentItemStatusLabel(): String = "Statut"

internal fun equipmentAssignedToLabel(): String = "Responsable"

internal fun equipmentUnassignedLabel(): String = "Non assigné"

internal fun equipmentItemDialogConfirmLabel(isNewItem: Boolean): String =
    if (isNewItem) "Ajouter" else "Modifier"

internal fun equipmentCancelActionLabel(): String = "Annuler"

internal fun equipmentConfirmActionLabel(): String = "Confirmer"

internal fun equipmentAssignDialogTitle(itemName: String): String = "Assigner « $itemName »"

internal fun equipmentAutoGenerateTitle(): String = "Préparer une liste d'équipement"

internal fun equipmentAutoGenerateDescription(): String =
    "Sélectionnez le type d'événement pour préparer une liste adaptée au groupe."

internal fun equipmentPrepareActionLabel(): String = "Préparer"

internal fun equipmentCategoryLabel(category: EquipmentCategory): String {
    return when (category) {
        EquipmentCategory.CAMPING -> "Camping"
        EquipmentCategory.SPORTS -> "Sport"
        EquipmentCategory.COOKING -> "Cuisine"
        EquipmentCategory.ELECTRONICS -> "Électronique"
        EquipmentCategory.SAFETY -> "Sécurité"
        EquipmentCategory.OTHER -> "Autre"
    }
}

internal fun equipmentStatusLabel(status: ItemStatus): String {
    return when (status) {
        ItemStatus.NEEDED -> "Requis"
        ItemStatus.ASSIGNED -> "Assigné"
        ItemStatus.CONFIRMED -> "Confirmé"
        ItemStatus.PACKED -> "Prêt"
        ItemStatus.CANCELLED -> "Annulé"
    }
}
