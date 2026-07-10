package com.guyghost.wakeve.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.guyghost.wakeve.contacts.ContactParticipantCandidate
import com.guyghost.wakeve.contacts.loadAndroidContactParticipantCandidates
import com.guyghost.wakeve.repository.EventRepositoryInterface
import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.analytics.AnalyticsProvider
import com.guyghost.wakeve.ParticipantManagementScreen
import com.guyghost.wakeve.R
import com.guyghost.wakeve.PollVotingState
import com.guyghost.wakeve.PollResultsScreen
import com.guyghost.wakeve.PollVotingScreen
import com.guyghost.wakeve.ui.event.toPollResultsUiState
import com.guyghost.wakeve.viewmodel.PollViewModel
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
    val eventNotFound = stringResource(R.string.event_not_found)
    val repository: EventRepositoryInterface = koinInject()
    val context = LocalContext.current
    val event = remember(eventId) { repository.getEvent(eventId) }
    var pendingContactCallback by remember {
        mutableStateOf<(((Result<List<ContactParticipantCandidate>>) -> Unit))?>(null)
    }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val callback = pendingContactCallback ?: return@rememberLauncherForActivityResult
        pendingContactCallback = null
        if (granted) {
            callback(runCatching { loadAndroidContactParticipantCandidates(context) })
        } else {
            callback(Result.failure(IllegalStateException("Contact permission denied by user")))
        }
    }
    
    when {
        event == null -> {
            ErrorPlaceholder(
                message = eventNotFound,
                onBack = onBack
            )
        }
        else -> {
            ParticipantManagementScreen(
                event = event,
                repository = repository,
                onParticipantsAdded = { onParticipantAdded() },
                onNavigateToPoll = { onPollStarted() },
                onContactPickerRequested = { callback ->
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        callback(runCatching { loadAndroidContactParticipantCandidates(context) })
                    } else {
                        pendingContactCallback = callback
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                }
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
    val eventNotFound = stringResource(R.string.event_not_found)
    val repository: EventRepositoryInterface = koinInject()
    val event = remember(eventId) { repository.getEvent(eventId) }
    
    when {
        event == null -> {
            ErrorPlaceholder(
                message = eventNotFound,
                onBack = onBack
            )
        }
        else -> {
            val pollViewModel = remember(eventId, participantId) {
                PollViewModel(
                    eventRepository = repository,
                    eventId = eventId,
                    analyticsProvider = NoOpAnalyticsProvider
                )
            }
            val selectedVotes by pollViewModel.selectedVotes.collectAsState()
            val isVoting by pollViewModel.isVoting.collectAsState()
            val errorMessage by pollViewModel.errorMessage.collectAsState()
            val hasSubmitted by pollViewModel.hasSubmitted.collectAsState()

            PollVotingScreen(
                event = event,
                state = PollVotingState(
                    eventId = event.id,
                    participantId = participantId,
                    votes = selectedVotes,
                    hasVoted = hasSubmitted,
                    isSubmitting = isVoting,
                    errorMessage = errorMessage
                ),
                onVoteChange = pollViewModel::selectVote,
                onSubmitVotes = {
                    pollViewModel.submitVotes(
                        event = event,
                        participantId = participantId,
                        onSuccess = onVoteSubmitted
                    )
                }
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
    val eventNotFound = stringResource(R.string.event_not_found)
    val repository: EventRepositoryInterface = koinInject()
    val event = remember(eventId) { repository.getEvent(eventId) }
    
    when {
        event == null -> {
            ErrorPlaceholder(
                message = eventNotFound,
                onBack = onBack
            )
        }
        else -> {
            val pollViewModel = remember(eventId, userId) {
                PollViewModel(
                    eventRepository = repository,
                    eventId = eventId,
                    analyticsProvider = NoOpAnalyticsProvider
                )
            }
            val poll by pollViewModel.poll.collectAsState()
            val selectedSlotId by pollViewModel.selectedFinalSlotId.collectAsState()
            val isConfirming by pollViewModel.isConfirmingFinalDate.collectAsState()
            val confirmationError by pollViewModel.confirmationError.collectAsState()
            val hasConfirmed by pollViewModel.hasConfirmedFinalDate.collectAsState()
            val isOrganizer = remember(eventId, userId, event) {
                repository.isOrganizer(eventId, userId)
            }
            val uiState = event.toPollResultsUiState(
                poll = poll,
                isOrganizer = isOrganizer,
                selectedSlotId = selectedSlotId,
                isConfirming = isConfirming,
                hasConfirmed = hasConfirmed,
                errorMessage = confirmationError
            )

            PollResultsScreen(
                state = uiState,
                onSlotSelected = pollViewModel::selectFinalSlot,
                onConfirmFinalDate = {
                    pollViewModel.confirmFinalDate(
                        event = event,
                        userId = userId,
                        onSuccess = onDateConfirmed
                    )
                },
                onBack = onBack
            )
        }
    }
}

private object NoOpAnalyticsProvider : AnalyticsProvider {
    override fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?>) = Unit
    override fun setUserProperty(name: String, value: String) = Unit
    override fun setUserId(userId: String?) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
    override fun clearUserData() = Unit
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
                Text(stringResource(R.string.back))
            }
        }
    }
}
