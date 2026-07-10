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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.EquipmentCategory
import com.guyghost.wakeve.R
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
        title = { Text(stringResource(if (item == null) R.string.equipment_add_title else R.string.equipment_edit_title)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.equipment_name_required)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.equipment_notes_label)) },
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
                            label = { Text(stringResource(R.string.equipment_quantity_required)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = sharedCost,
                            onValueChange = { sharedCost = it },
                            label = { Text(stringResource(R.string.equipment_shared_cost_label)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            suffix = { Text(stringResource(R.string.currency_symbol_euro)) }
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
                            value = stringResource(category.dialogLabelResource()),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.equipment_category_label)) },
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
                                    text = { Text(stringResource(cat.dialogLabelResource())) },
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
                            value = stringResource(status.dialogLabelResource()),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.equipment_status_label)) },
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
                                    text = { Text(stringResource(st.dialogLabelResource())) },
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
                                participants.find { it.id == id }?.name ?: stringResource(R.string.equipment_unassigned)
                            } ?: stringResource(R.string.equipment_unassigned),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.equipment_assigned_to_label)) },
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
                                text = { Text(stringResource(R.string.equipment_unassigned)) },
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
                Text(stringResource(if (item == null) R.string.equipment_add_action else R.string.equipment_edit_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.equipment_cancel_action))
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
        title = { Text(stringResource(R.string.equipment_assign_title, item.name)) },
        text = {
            LazyColumn {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.equipment_unassigned)) },
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
                Text(stringResource(R.string.equipment_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.equipment_cancel_action))
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
        "camping" to R.string.equipment_event_type_camping,
        "beach" to R.string.equipment_event_type_beach,
        "ski" to R.string.equipment_event_type_ski,
        "hiking" to R.string.equipment_event_type_hiking,
        "picnic" to R.string.equipment_event_type_picnic,
        "indoor" to R.string.equipment_event_type_indoor
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.equipment_auto_generate_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.equipment_auto_generate_description),
                    style = MaterialTheme.typography.bodyMedium
                )

                LazyColumn {
                    items(eventTypes) { (type, labelResource) ->
                        ListItem(
                            headlineContent = { Text(stringResource(labelResource)) },
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
                Text(stringResource(R.string.equipment_prepare_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.equipment_cancel_action))
            }
        }
    )
}

private fun EquipmentCategory.dialogLabelResource(): Int = when (this) {
    EquipmentCategory.CAMPING -> R.string.equipment_category_camping
    EquipmentCategory.SPORTS -> R.string.equipment_category_sports
    EquipmentCategory.COOKING -> R.string.equipment_category_cooking
    EquipmentCategory.ELECTRONICS -> R.string.equipment_category_electronics
    EquipmentCategory.SAFETY -> R.string.equipment_category_safety
    EquipmentCategory.OTHER -> R.string.equipment_category_other
}

private fun ItemStatus.dialogLabelResource(): Int = when (this) {
    ItemStatus.NEEDED -> R.string.equipment_status_needed
    ItemStatus.ASSIGNED -> R.string.equipment_status_assigned
    ItemStatus.CONFIRMED -> R.string.equipment_status_confirmed
    ItemStatus.PACKED -> R.string.equipment_status_packed
    ItemStatus.CANCELLED -> R.string.equipment_status_cancelled
}
