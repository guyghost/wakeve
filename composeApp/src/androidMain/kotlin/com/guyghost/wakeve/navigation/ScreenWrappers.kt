package com.guyghost.wakeve.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.ParticipantManagementScreen
import com.guyghost.wakeve.PollResultsScreen
import com.guyghost.wakeve.PollVotingScreen
import org.koin.compose.koinInject

/**
 * Wrapper composables that bridge navigation parameters to screen requirements.
 * These wrappers fetch Event objects and repositories before calling the actual screens.
 */

/**
 * Wrapper for ParticipantManagementScreen that fetches the event by ID.
 */
@Composable
fun ParticipantManagementScreenWrapper(
    eventId: String,
    onParticipantAdded: () -> Unit,
    onPollStarted: () -> Unit,
    onBack: () -> Unit
) {
    val repository: EventRepositoryInterface = koinInject()
    val event = remember(eventId) { repository.getEvent(eventId) }
    
    when {
        event == null -> {
            ErrorPlaceholder(
                message = "Événement non trouvé",
                onBack = onBack
            )
        }
        else -> {
            ParticipantManagementScreen(
                event = event,
                repository = repository,
                onParticipantsAdded = { onParticipantAdded() },
                onNavigateToPoll = { onPollStarted() }
            )
        }
    }
}

/**
 * Wrapper for PollVotingScreen that fetches the event by ID.
 */
@Composable
fun PollVotingScreenWrapper(
    eventId: String,
    participantId: String,
    onVoteSubmitted: () -> Unit,
    onBack: () -> Unit
) {
    val repository: EventRepositoryInterface = koinInject()
    val event = remember(eventId) { repository.getEvent(eventId) }
    
    when {
        event == null -> {
            ErrorPlaceholder(
                message = "Événement non trouvé",
                onBack = onBack
            )
        }
        else -> {
            PollVotingScreen(
                event = event,
                repository = repository,
                participantId = participantId,
                onVoteSubmitted = { onVoteSubmitted() }
            )
        }
    }
}

/**
 * Wrapper for PollResultsScreen that fetches the event by ID.
 */
@Composable
fun PollResultsScreenWrapper(
    eventId: String,
    userId: String,
    onDateConfirmed: () -> Unit,
    onBack: () -> Unit
) {
    val repository: EventRepositoryInterface = koinInject()
    val event = remember(eventId) { repository.getEvent(eventId) }
    
    when {
        event == null -> {
            ErrorPlaceholder(
                message = "Événement non trouvé",
                onBack = onBack
            )
        }
        else -> {
            PollResultsScreen(
                event = event,
                repository = repository,
                userId = userId,
                onDateConfirmed = { onDateConfirmed() }
            )
        }
    }
}

/**
 * Error placeholder screen shown when an event cannot be found.
 */
@Composable
private fun ErrorPlaceholder(
    message: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) {
                Text("Retour")
            }
        }
    }
}
