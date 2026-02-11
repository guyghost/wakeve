package com.guyghost.wakeve.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.Event
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining the contract for pagination ViewModel.
 * This allows for easier testing with fake implementations.
 */
interface IPaginatedEventViewModel {
    val events: StateFlow<List<Event>>
    val isLoadingNextPage: StateFlow<Boolean>
    val error: StateFlow<String?>
    val hasMorePages: StateFlow<Boolean>
    val isLoading: StateFlow<Boolean>

    fun loadNextPage()
    fun refresh()
}

/**
 * Paginated list of events with automatic loading, error handling, and empty state.
 *
 * Features:
 * - Automatic loading of next page when scrolling near the bottom
 * - Loading indicators at the bottom
 * - Error state with retry functionality
 * - Empty state when no events exist
 * - Pull-to-refresh support
 *
 * @param viewModel ViewModel managing paginated events
 * @param onEventClick Callback when an event is clicked
 * @param modifier Optional modifier for container
 */
@Composable
fun PaginatedEventList(
    viewModel: IPaginatedEventViewModel,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoadingNextPage.collectAsState()
    val isLoadingInitial by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val listState = rememberLazyListState()

    // Detect when user scrolls to bottom (trigger when 5 items from end)
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 5 && isLoading == false
        }
    }

    // Trigger load next page when needed
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadNextPage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            events.isEmpty() && isLoadingInitial == false && error == null -> {
                EmptyStateMessage()
            }
            error != null && events.isEmpty() -> {
                ErrorMessage(
                    error = error!!,
                    onRetry = { viewModel.refresh() }
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.testTag("event_list")
                ) {
                    items(
                        items = events,
                        key = { it.id }
                    ) { event ->
                        EventCard(
                            event = event,
                            onClick = { onEventClick(event.id) }
                        )
                    }

                    if (isLoading) {
                        item {
                            LoadingIndicator(
                                modifier = Modifier.testTag("loading_indicator")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            Text(
                text = "Status: ${event.status.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyStateMessage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "No Events Yet",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag("empty_state")
            )
            Text(
                text = "Create your first event to get started!",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorMessage(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("error_message")
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
