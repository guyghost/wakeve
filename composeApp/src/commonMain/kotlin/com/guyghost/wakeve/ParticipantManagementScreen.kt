package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.access.ParticipantAccessMapper
import com.guyghost.wakeve.contacts.ContactParticipantCandidate
import com.guyghost.wakeve.contacts.ContactParticipantSelectionPolicy
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.presentation.participants.ParticipantManagementPresentationMapper
import com.guyghost.wakeve.presentation.participants.ParticipantManagementRow
import com.guyghost.wakeve.repository.EventRepositoryInterface
import kotlinx.coroutines.launch

typealias ContactPickerRequest = ((Result<List<ContactParticipantCandidate>>) -> Unit) -> Unit

data class ParticipantManagementState(
    val eventId: String = "",
    val newParticipantEmail: String = "",
    val participants: List<ParticipantManagementRow> = emptyList(),
    val contactCandidates: List<ContactParticipantCandidate> = emptyList(),
    val selectedContactEmails: Set<String> = emptySet(),
    val contactSearchQuery: String = "",
    val isContactPickerVisible: Boolean = false,
    val isLoadingContacts: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String = ""
)

@Composable
fun ParticipantManagementScreen(
    event: Event,
    repository: EventRepositoryInterface,
    onParticipantsAdded: (String) -> Unit,
    onNavigateToPoll: (String) -> Unit,
    onContactPickerRequested: ContactPickerRequest? = null
) {
    var state by remember {
        mutableStateOf(
            ParticipantManagementState(
                eventId = event.id,
                participants = loadParticipantRows(repository, event.id)
            )
        )
    }
    val scope = rememberCoroutineScope()

    fun showError(message: String) {
        state = state.copy(
            isError = true,
            errorMessage = message,
            isLoadingContacts = false
        )
    }

    fun refreshParticipants() {
        state = state.copy(
            participants = loadParticipantRows(repository, event.id),
            isError = false
        )
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Gérer les participants",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Événement : ${event.title}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Add Participant Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Ajouter un participant",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = state.newParticipantEmail,
                        onValueChange = { state = state.copy(newParticipantEmail = it) },
                        label = { Text("Email") },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val rawEmail = state.newParticipantEmail.trim()
                            val email = ContactParticipantSelectionPolicy.normalizeEmailOrNull(rawEmail)
                            when {
                                rawEmail.isEmpty() -> {
                                    state = state.copy(
                                        isError = true,
                                        errorMessage = "L'e-mail est requis"
                                    )
                                }
                                email != null &&
                                    state.participants.any {
                                        ContactParticipantSelectionPolicy.normalizeEmailOrNull(it.userIdOrEmail) == email
                                    } -> {
                                    state = state.copy(
                                        isError = true,
                                        errorMessage = "Participant déjà ajouté"
                                    )
                                }
                                email == null -> {
                                    state = state.copy(
                                        isError = true,
                                        errorMessage = "Format d'e-mail invalide"
                                    )
                                }
                                else -> {
                                    scope.launch {
                                        val result = repository.addParticipant(event.id, email)
                                        if (result.isSuccess) {
                                            state = state.copy(
                                                participants = loadParticipantRows(repository, event.id),
                                                newParticipantEmail = "",
                                                isError = false
                                            )
                                        } else {
                                            state = state.copy(
                                                isError = true,
                                                errorMessage = participantAddFailureMessage()
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .height(56.dp)
                    ) {
                        Text("Ajouter")
                    }
                }

                if (onContactPickerRequested != null) {
                    Button(
                        onClick = {
                            state = state.copy(isLoadingContacts = true, isError = false)
                            onContactPickerRequested { result ->
                                val contacts = result.getOrElse {
                                    showError(contactAccessFailureMessage())
                                    return@onContactPickerRequested
                                }
                                    .mapNotNull { contact ->
                                        val normalizedEmail = ContactParticipantSelectionPolicy.normalizeEmailOrNull(contact.email)
                                            ?: return@mapNotNull null
                                        contact.copy(email = normalizedEmail)
                                    }
                                    .distinctBy { it.email }
                                    .sortedWith(
                                        compareBy<ContactParticipantCandidate> { it.displayName.ifBlank { it.email }.lowercase() }
                                            .thenBy { it.email }
                                    )

                                if (contacts.isEmpty()) {
                                    showError("Aucun contact avec adresse e-mail disponible")
                                } else {
                                    state = state.copy(
                                        contactCandidates = contacts,
                                        selectedContactEmails = emptySet(),
                                        contactSearchQuery = "",
                                        isContactPickerVisible = true,
                                        isLoadingContacts = false,
                                        isError = false
                                    )
                                }
                            }
                        },
                        enabled = !state.isLoadingContacts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (state.isLoadingContacts) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                        Text("Choisir depuis les contacts")
                    }
                }

                if (state.isError) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            state.errorMessage,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Participants List
        if (state.participants.isNotEmpty()) {
            Text(
                "Participants (${state.participants.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.participants) { participant ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    participant.userIdOrEmail,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    participant.subtitleLabel(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    state = state.copy(
                                        participants = state.participants.filter {
                                            it.userIdOrEmail != participant.userIdOrEmail
                                        }
                                    )
                                }
                            ) {
                                Text("Retirer")
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                "Aucun participant ajouté",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onParticipantsAdded(event.id) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text("Retour")
            }
            Button(
                onClick = {
                    // Transition event to POLLING status
                    scope.launch {
                        repository.updateEventStatus(event.id, EventStatus.POLLING, null)
                        onNavigateToPoll(event.id)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                enabled = state.participants.isNotEmpty()
            ) {
                Text("Démarrer le sondage")
            }
        }
    }

    if (state.isContactPickerVisible) {
        ContactParticipantPickerDialog(
            contacts = state.contactCandidates,
            selectedEmails = state.selectedContactEmails,
            searchQuery = state.contactSearchQuery,
            existingParticipantIds = state.participants.map { it.userIdOrEmail },
            onSearchQueryChange = { query ->
                state = state.copy(contactSearchQuery = query)
            },
            onToggleContact = { email ->
                state = state.copy(
                    selectedContactEmails = if (email in state.selectedContactEmails) {
                        state.selectedContactEmails - email
                    } else {
                        state.selectedContactEmails + email
                    }
                )
            },
            onDismiss = {
                state = state.copy(
                    isContactPickerVisible = false,
                    selectedContactEmails = emptySet(),
                    contactSearchQuery = ""
                )
            },
            onAddSelected = {
                val selectedContacts = state.contactCandidates.filter { it.email in state.selectedContactEmails }
                val selection = ContactParticipantSelectionPolicy.prepareSelection(
                    selectedContacts = selectedContacts,
                    existingParticipantIds = state.participants.map { it.userIdOrEmail }
                )

                if (selection.emailsToAdd.isEmpty()) {
                    showError("Aucun nouveau participant à ajouter")
                    state = state.copy(isContactPickerVisible = false)
                    return@ContactParticipantPickerDialog
                }

                scope.launch {
                    val failedEmails = mutableListOf<String>()
                    selection.emailsToAdd.forEach { email ->
                        val result = repository.addParticipant(event.id, email)
                        if (result.isFailure) failedEmails += email
                    }

                    refreshParticipants()
                    state = state.copy(
                        isContactPickerVisible = false,
                        selectedContactEmails = emptySet(),
                        contactSearchQuery = "",
                        isError = failedEmails.isNotEmpty(),
                        errorMessage = if (failedEmails.isNotEmpty()) {
                            "Impossible d'ajouter ${failedEmails.size} participant(s)"
                        } else {
                            ""
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun ContactParticipantPickerDialog(
    contacts: List<ContactParticipantCandidate>,
    selectedEmails: Set<String>,
    searchQuery: String,
    existingParticipantIds: List<String>,
    onSearchQueryChange: (String) -> Unit,
    onToggleContact: (String) -> Unit,
    onDismiss: () -> Unit,
    onAddSelected: () -> Unit
) {
    val existingEmails = existingParticipantIds
        .mapNotNull(ContactParticipantSelectionPolicy::normalizeEmailOrNull)
        .toSet()
    val filteredContacts = contacts.filter { contact ->
        val query = searchQuery.trim()
        query.isBlank() ||
            contact.displayName.contains(query, ignoreCase = true) ||
            contact.email.contains(query, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir des contacts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Rechercher") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredContacts) { contact ->
                        val alreadyAdded = contact.email in existingEmails
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = contact.email in selectedEmails,
                                enabled = !alreadyAdded,
                                onCheckedChange = {
                                    if (!alreadyAdded) onToggleContact(contact.email)
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    contact.displayName.ifBlank { contact.email },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    if (alreadyAdded) "${contact.email} - déjà ajouté" else contact.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAddSelected,
                enabled = selectedEmails.isNotEmpty()
            ) {
                Text("Ajouter (${selectedEmails.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

private fun loadParticipantRows(
    repository: EventRepositoryInterface,
    eventId: String
): List<ParticipantManagementRow> {
    val participantRecords = repository.getParticipantRecords(eventId).orEmpty()
    if (participantRecords.isNotEmpty()) {
        return ParticipantManagementPresentationMapper.map(
            participantRecords.map(ParticipantAccessMapper::fromRepositoryRecord)
        )
    }

    return repository.getParticipants(eventId).orEmpty().map { participantId ->
        ParticipantManagementRow(
            userIdOrEmail = participantId,
            roleLabel = "Membre",
            statusLabel = "En attente",
            canAccessOrganizationDetails = false
        )
    }
}

private fun ParticipantManagementRow.detailsAccessLabel(): String =
    if (canAccessOrganizationDetails) {
        "Détails débloqués"
    } else {
        "Détails verrouillés"
    }

private fun ParticipantManagementRow.subtitleLabel(): String =
    "$roleLabel - $statusLabel - ${detailsAccessLabel()}"

internal fun participantAddFailureMessage(): String =
    "Impossible d'ajouter le participant. Reessayez."

internal fun contactAccessFailureMessage(): String =
    "Impossible d'acceder aux contacts. Reessayez."
