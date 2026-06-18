package com.guyghost.wakeve.ui.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.viewmodel.EventManagementViewModel

@Composable
fun EventWorkspaceRoute(
    viewModel: EventManagementViewModel,
    currentUserId: String,
    onNavigateTo: (String) -> Unit,
    onShowToast: (String) -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var selectedFilter by rememberSaveable { mutableStateOf(EventListFilter.Upcoming) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedEventId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.dispatch(EventManagementContract.Intent.LoadEvents)
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is EventManagementContract.SideEffect.NavigateTo -> onNavigateTo(effect.route)
                is EventManagementContract.SideEffect.ShowToast -> onShowToast(effect.message)
                EventManagementContract.SideEffect.NavigateBack -> Unit
                is EventManagementContract.SideEffect.ConflictDetected ->
                    onShowToast("${effect.criticalFieldCount} sync conflict(s) need your attention")
            }
        }
    }

    val uiState = state.toEventWorkspaceUiState(
        currentUserId = currentUserId,
        selectedFilter = selectedFilter,
        searchQuery = searchQuery,
        selectedEventId = selectedEventId
    )

    EventWorkspaceScreen(
        state = uiState,
        onFilterChange = { selectedFilter = it },
        onSearchChange = { searchQuery = it },
        onCreateEvent = { onNavigateTo("event_creation") },
        onOpenProfile = onOpenProfile,
        onSelectEvent = { eventId, navigate ->
            selectedEventId = eventId
            if (navigate) {
                viewModel.dispatch(EventManagementContract.Intent.SelectEvent(eventId))
            } else {
                viewModel.dispatch(EventManagementContract.Intent.LoadParticipants(eventId))
                viewModel.dispatch(EventManagementContract.Intent.LoadPollResults(eventId))
            }
        },
        onOpenEvent = { eventId ->
            viewModel.dispatch(EventManagementContract.Intent.SelectEvent(eventId))
        },
        onOpenPoll = { eventId -> onNavigateTo("event/$eventId/poll/vote") },
        onRetry = {
            viewModel.clearError()
            viewModel.dispatch(EventManagementContract.Intent.LoadEvents)
        },
        modifier = modifier
    )
}
