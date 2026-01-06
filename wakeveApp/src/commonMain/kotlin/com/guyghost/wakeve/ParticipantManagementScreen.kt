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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import kotlinx.coroutines.launch

data class ParticipantManagementState(
    val eventId: String = "",
    val newParticipantEmail: String = "",
    val participants: List<String> = emptyList(),
    val isError: Boolean = false,
    val errorMessage: String = ""
)

@Composable
fun ParticipantManagementScreen(
    event: Event,
    repository: EventRepositoryInterface,
    onParticipantsAdded: (String) -> Unit,
    onNavigateToPoll: (String) -> Unit
) {
    var state by remember {
        mutableStateOf(
            ParticipantManagementState(
                eventId = event.id,
                participants = repository.getParticipants(event.id) ?: emptyList()
            )
        )
    }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Manage Participants",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Event: ${event.title}",
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
                    "Add Participant",
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
                            val email = state.newParticipantEmail.trim()
                            when {
                                email.isEmpty() -> {
                                    state = state.copy(
                                        isError = true,
                                        errorMessage = "Email is required"
                                    )
                                }
                                state.participants.contains(email) -> {
                                    state = state.copy(
                                        isError = true,
                                        errorMessage = "Participant already added"
                                    )
                                }
                                !isValidEmail(email) -> {
                                    state = state.copy(
                                        isError = true,
                                        errorMessage = "Invalid email format"
                                    )
                                }
                                else -> {
                                    scope.launch {
                                        val result = repository.addParticipant(event.id, email)
                                        if (result.isSuccess) {
                                            state = state.copy(
                                                participants = state.participants + email,
                                                newParticipantEmail = "",
                                                isError = false
                                            )
                                        } else {
                                            state = state.copy(
                                                isError = true,
                                                errorMessage = result.exceptionOrNull()?.message ?: "Failed to add participant"
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
                        Text("Add")
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
                items(state.participants) { email ->
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
                                Text(email, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Not yet voted",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    state = state.copy(
                                        participants = state.participants.filter { it != email }
                                    )
                                }
                            ) {
                                Text("Remove")
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
                    "No participants added yet",
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
                Text("Back")
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
                Text("Start Poll")
            }
        }
    }
}

private fun isValidEmail(email: String): Boolean {
    return email.contains("@") && email.contains(".")
}
