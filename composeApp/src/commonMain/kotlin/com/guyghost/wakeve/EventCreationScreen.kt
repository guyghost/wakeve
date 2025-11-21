package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import kotlin.random.Random

data class EventCreationState(
    val title: String = "",
    val description: String = "",
    val deadline: String = "",
    val slots: List<TimeSlot> = emptyList(),
    val slotStart: String = "",
    val slotEnd: String = "",
    val isError: Boolean = false,
    val errorMessage: String = ""
)

@Composable
fun EventCreationScreen(
    onEventCreated: (Event) -> Unit,
    onNavigateToParticipants: (String) -> Unit = {}
) {
    var state by remember { mutableStateOf(EventCreationState()) }
    
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Create Event",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Title Input
        TextField(
            value = state.title,
            onValueChange = { state = state.copy(title = it) },
            label = { Text("Event Title") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            singleLine = true
        )

        // Description Input
        TextField(
            value = state.description,
            onValueChange = { state = state.copy(description = it) },
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(bottom = 12.dp)
        )

        // Deadline Input
        TextField(
            value = state.deadline,
            onValueChange = { state = state.copy(deadline = it) },
            label = { Text("Deadline (ISO 8601, e.g., 2025-12-25T18:00:00Z)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            singleLine = true
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Time Slots Section
        Text(
            "Proposed Time Slots",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
        )

        // Slot Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = state.slotStart,
                onValueChange = { state = state.copy(slotStart = it) },
                label = { Text("Start") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                singleLine = true
            )
            TextField(
                value = state.slotEnd,
                onValueChange = { state = state.copy(slotEnd = it) },
                label = { Text("End") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                singleLine = true
            )
            Button(
                onClick = {
                    if (state.slotStart.isNotEmpty() && state.slotEnd.isNotEmpty()) {
                        val newSlot = TimeSlot(
                            id = "slot-${state.slots.size + 1}",
                            start = state.slotStart,
                            end = state.slotEnd,
                            timezone = "UTC"
                        )
                        state = state.copy(
                            slots = state.slots + newSlot,
                            slotStart = "",
                            slotEnd = ""
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .height(56.dp)
            ) {
                Text("Add")
            }
        }

        // Display Added Slots
        if (state.slots.isNotEmpty()) {
            Text(
                "Added Slots (${state.slots.size}):",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )
            state.slots.forEach { slot ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(slot.start, style = MaterialTheme.typography.bodySmall)
                            Text("→", style = MaterialTheme.typography.bodySmall)
                            Text(slot.end, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = {
                                state = state.copy(slots = state.slots.filter { it.id != slot.id })
                            }
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Error Display
        if (state.isError) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    state.errorMessage,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Create Button
        Button(
            onClick = {
                // Validate inputs
                when {
                    state.title.isEmpty() -> {
                        state = state.copy(
                            isError = true,
                            errorMessage = "Event title is required"
                        )
                    }
                    state.deadline.isEmpty() -> {
                        state = state.copy(
                            isError = true,
                            errorMessage = "Deadline is required"
                        )
                    }
                    state.slots.isEmpty() -> {
                        state = state.copy(
                            isError = true,
                            errorMessage = "At least one time slot is required"
                        )
                    }
                    else -> {
                        val now = java.time.Instant.now().toString()
                        val event = Event(
                            id = "event-${Random.nextLong(1000000)}",
                            title = state.title,
                            description = state.description,
                            organizerId = "organizer-1", // TODO: Get from auth
                            participants = emptyList(),
                            proposedSlots = state.slots,
                            deadline = state.deadline,
                            status = EventStatus.DRAFT,
                            createdAt = now,
                            updatedAt = now
                        )
                        onEventCreated(event)
                        onNavigateToParticipants(event.id)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = state.slots.isNotEmpty()
        ) {
            Text("Create Event", style = MaterialTheme.typography.labelLarge)
        }
    }
}
