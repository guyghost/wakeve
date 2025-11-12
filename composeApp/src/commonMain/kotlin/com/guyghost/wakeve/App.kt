package com.guyghost.wakeve

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import wakeve.composeapp.generated.resources.Res
import wakeve.composeapp.generated.resources.compose_multiplatform

@Composable
@Preview
fun App() {
    MaterialTheme {
        val repository = remember { EventRepository() }
        var currentEventId by remember { mutableStateOf<String?>(null) }
        if (currentEventId == null) {
            EventCreationScreen(onEventCreated = { event ->
                repository.createEvent(event)
                currentEventId = event.id
            })
        } else {
            PollScreen(eventId = currentEventId!!, repository = repository)
        }
    }
}

@Composable
fun EventCreationScreen(onEventCreated: (Event) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Create Event", style = MaterialTheme.typography.headlineMedium)
        TextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = deadline,
            onValueChange = { deadline = it },
            label = { Text("Deadline (ISO)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            val event = Event(
                id = kotlin.random.Random.nextLong().toString(),
                title = title,
                description = description,
                organizerId = "organizer1", // mock
                proposedSlots = listOf(TimeSlot("slot1", "2024-01-01T10:00", "2024-01-01T12:00", "UTC")),
                deadline = deadline,
                status = EventStatus.POLLING
            )
            onEventCreated(event)
        }) {
            Text("Create Event and Start Poll")
        }
    }
}

@Composable
fun PollScreen(eventId: String, repository: EventRepository) {
    val event = repository.getEvent(eventId)
    val poll = repository.getPoll(eventId)
    var bestSlot by remember { mutableStateOf<TimeSlot?>(null) }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Poll for ${event?.title}", style = MaterialTheme.typography.headlineMedium)
        Text("Voting UI would go here")
        Button(onClick = {
            poll?.let { p ->
                event?.let { e ->
                    bestSlot = PollLogic.calculateBestSlot(p, e.proposedSlots)
                }
            }
        }) {
            Text("Calculate Best Slot")
        }
        bestSlot?.let { slot ->
            Text("Best Slot: ${slot.start} - ${slot.end}")
            Button(onClick = {
                repository.updateEventStatus(eventId, EventStatus.CONFIRMED, slot.start)
            }) {
                Text("Confirm Date")
            }
        }
    }
}