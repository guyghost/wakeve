package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.guyghost.wakeve.models.Event

enum class Screen {
    EVENT_CREATION,
    PARTICIPANT_MANAGEMENT,
    POLL_VOTING,
    POLL_RESULTS
}

data class AppState(
    val currentScreen: Screen = Screen.EVENT_CREATION,
    val currentEventId: String? = null,
    val currentUserId: String = "organizer-1" // TODO: Get from auth
)

@Composable
@Preview
fun App() {
    MaterialTheme {
        val repository = remember { EventRepository() }
        var appState by remember { mutableStateOf(AppState()) }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
        ) {
            when (appState.currentScreen) {
                Screen.EVENT_CREATION -> {
                    EventCreationScreen(
                        onEventCreated = { event ->
                            repository.createEvent(event)
                            appState = appState.copy(
                                currentEventId = event.id,
                                currentScreen = Screen.PARTICIPANT_MANAGEMENT
                            )
                        }
                    )
                }
                Screen.PARTICIPANT_MANAGEMENT -> {
                    val event = repository.getEvent(appState.currentEventId ?: "")
                    if (event != null) {
                        ParticipantManagementScreen(
                            event = event,
                            repository = repository,
                            onParticipantsAdded = { eventId ->
                                appState = appState.copy(currentEventId = eventId)
                            },
                            onNavigateToPoll = { eventId ->
                                appState = appState.copy(
                                    currentEventId = eventId,
                                    currentScreen = Screen.POLL_VOTING
                                )
                            }
                        )
                    }
                }
                Screen.POLL_VOTING -> {
                    val event = repository.getEvent(appState.currentEventId ?: "")
                    if (event != null) {
                        PollVotingScreen(
                            event = event,
                            repository = repository,
                            onVoteSubmitted = { eventId ->
                                appState = appState.copy(
                                    currentEventId = eventId,
                                    currentScreen = Screen.POLL_RESULTS
                                )
                            }
                        )
                    }
                }
                Screen.POLL_RESULTS -> {
                    val event = repository.getEvent(appState.currentEventId ?: "")
                    if (event != null) {
                        PollResultsScreen(
                            event = event,
                            repository = repository,
                            onDateConfirmed = { eventId ->
                                // Could navigate to event details or home
                                appState = appState.copy(currentEventId = eventId)
                            }
                        )
                    }
                }
            }
        }
    }
}