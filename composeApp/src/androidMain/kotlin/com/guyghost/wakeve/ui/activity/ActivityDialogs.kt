package com.guyghost.wakeve.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.activity.ActivityRepository
import com.guyghost.wakeve.models.*
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * Dialog for adding or editing an activity
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditActivityDialog(
    activity: Activity?,
    organizerId: String,
    onDismiss: () -> Unit,
    onConfirm: (Activity) -> Unit
) {
    var name by remember { mutableStateOf(activity?.name ?: "") }
    var description by remember { mutableStateOf(activity?.description ?: "") }
    var date by remember { mutableStateOf(activity?.date ?: "") }
    var time by remember { mutableStateOf(activity?.time ?: "") }
    var duration by remember { mutableStateOf(activity?.duration?.toString() ?: "60") }
    var location by remember { mutableStateOf(activity?.location ?: "") }
    var cost by remember { mutableStateOf(activity?.cost?.div(100)?.toString() ?: "") }
    var maxParticipants by remember { mutableStateOf(activity?.maxParticipants?.toString() ?: "") }

    val isValidDate = date.isBlank() || date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
    val isValid = name.isNotBlank() &&
                  description.isNotBlank() &&
                  isValidDate &&
                  duration.toIntOrNull() != null &&
                  duration.toInt() > 0 &&
                  (time.isBlank() || time.matches(Regex("\\d{2}:\\d{2}")))
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (activity == null) "Ajouter une activité" else "Modifier l'activité") },
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
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Date (YYYY-MM-DD) *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("2025-12-25") }
                        )
                        
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Heure (HH:MM)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("14:30") }
                        )
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it },
                            label = { Text("Durée (min) *") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = maxParticipants,
                            onValueChange = { maxParticipants = it },
                            label = { Text("Places max") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            placeholder = { Text("Illimité") }
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Lieu") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { cost = it },
                        label = { Text("Coût par personne (€)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        suffix = { Text("€") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val costInCents = cost.toDoubleOrNull()?.times(100)?.toLong()
                    val now = Clock.System.now().toString()
                    val updatedActivity = Activity(
                        id = activity?.id ?: UUID.randomUUID().toString(),
                        eventId = activity?.eventId ?: "",
                        scenarioId = activity?.scenarioId,
                        name = name.trim(),
                        description = description.trim(),
                        date = date.takeIf { it.isNotBlank() },
                        time = time.takeIf { it.isNotBlank() },
                        duration = duration.toInt(),
                        location = location.trim().takeIf { it.isNotBlank() },
                        cost = costInCents,
                        maxParticipants = maxParticipants.toIntOrNull(),
                        registeredParticipantIds = activity?.registeredParticipantIds ?: emptyList(),
                        organizerId = activity?.organizerId ?: organizerId,
                        notes = activity?.notes,
                        createdAt = activity?.createdAt ?: now,
                        updatedAt = now
                    )
                    onConfirm(updatedActivity)
                },
                enabled = isValid
            ) {
                Text(if (activity == null) "Ajouter" else "Modifier")
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
 * Participant info for UI display
 */
data class ParticipantInfo(
    val id: String,
    val name: String
)

/**
 * Dialog for managing participants in an activity
 */
@Composable
fun ManageParticipantsDialog(
    activity: ActivityWithStats,
    allParticipants: List<ParticipantInfo>,
    activityRepository: ActivityRepository,
    onDismiss: () -> Unit,
    onReload: () -> Unit
) {
    var registeredParticipants by remember {
        mutableStateOf(
            activityRepository.getParticipantsByActivity(activity.activity.id)
        )
    }

    val registeredIds = registeredParticipants.map { it.participantId }.toSet()

    fun toggleParticipant(participantId: String) {
        if (registeredIds.contains(participantId)) {
            // Unregister
            activityRepository.unregisterParticipant(activity.activity.id, participantId)
        } else {
            // Register (if not full)
            if (!activity.isFull) {
                val now = Clock.System.now().toString()
                activityRepository.registerParticipant(
                    ActivityParticipant(
                        id = UUID.randomUUID().toString(),
                        activityId = activity.activity.id,
                        participantId = participantId,
                        registeredAt = now,
                        notes = null
                    )
                )
            }
        }
        registeredParticipants = activityRepository.getParticipantsByActivity(activity.activity.id)
        onReload()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Participants - ${activity.activity.name}")
                if (activity.activity.maxParticipants != null) {
                    Text(
                        text = "${activity.registeredCount} / ${activity.activity.maxParticipants} inscrits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            LazyColumn {
                items(allParticipants.size) { index ->
                    val participant = allParticipants[index]
                    val isRegistered = registeredIds.contains(participant.id)
                    val canRegister = !activity.isFull || isRegistered

                    ListItem(
                        headlineContent = { Text(participant.name) },
                        leadingContent = {
                            Checkbox(
                                checked = isRegistered,
                                onCheckedChange = {
                                    if (canRegister) {
                                        toggleParticipant(participant.id)
                                    }
                                },
                                enabled = canRegister
                            )
                        },
                        trailingContent = {
                            if (isRegistered) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (activity.isFull) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Activité complète",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
