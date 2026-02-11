package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.EventRepository
import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.analytics.AnalyticsProvider
import com.guyghost.wakeve.models.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the event list screen with analytics tracking.
 *
 * This ViewModel manages the state of the event list, loads events from the
 * repository, and tracks user interactions for analytics purposes.
 *
 * ## Usage in Compose
 *
 * ```kotlin
 * @Composable
 * fun EventListScreen(
 *     viewModel: EventListViewModel = koinViewModel()
 * ) {
 *     val events by viewModel.events.collectAsState()
 *     val isLoading by viewModel.isLoading.collectAsState()
 *
 *     LaunchedEffect(Unit) {
 *         viewModel.loadEvents()
 *     }
 *
 *     EventListContent(
 *         events = events,
 *         isLoading = isLoading,
 *         onEventClick = { eventId -> viewModel.onEventClicked(eventId) },
 *         onEventShare = { eventId, method -> viewModel.onEventShared(eventId, method) },
 *         onCreateEvent = { viewModel.onCreateEventClick() }
 *     )
 * }
 * ```
 *
 * @property eventRepository Repository for accessing event data
 * @property analyticsProvider Analytics provider for tracking user actions
 */
class EventListViewModel(
    private val eventRepository: EventRepository,
    analyticsProvider: AnalyticsProvider
) : AnalyticsViewModel(analyticsProvider) {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        trackScreenView("event_list", "EventListViewModel")
        loadEvents()
    }

    /**
     * Load all events from the repository.
     *
     * This method tracks the loading status and any errors that occur
     * during the operation.
     */
    fun loadEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                eventRepository.getAllEvents().let { events ->
                    _events.value = events
                }
            } catch (e: Exception) {
                trackError("load_events_failed", e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Handle event click from the list.
     *
     * Tracks when a user views an event from the list view.
     *
     * @param eventId The ID of the clicked event
     */
    fun onEventClicked(eventId: String) {
        trackEvent(AnalyticsEvent.EventViewed(eventId, "list"))
    }

    /**
     * Handle event share action.
     *
     * Tracks when a user shares an event with the specified method.
     *
     * @param eventId The ID of the shared event
     * @param method The share method used (link, qr_code, native_share)
     */
    fun onEventShared(eventId: String, method: String) {
        trackEvent(AnalyticsEvent.EventShared(eventId, method))
    }

    /**
     * Handle create event button click.
     *
     * Tracks when a user initiates the event creation flow.
     * This can help measure conversion from viewing events to creating them.
     */
    fun onCreateEventClick() {
        trackEvent(AnalyticsEvent.EventCreated("initiated", false, 0))
    }
}
