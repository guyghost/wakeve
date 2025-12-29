package com.guyghost.wakeve.presentation.statemachine

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import kotlinx.coroutines.CoroutineScope

/**
 * State Machine for event management workflows.
 *
 * This state machine handles all event-related intents:
 * - Loading events list
 * - Creating, updating, deleting events
 * - Selecting events and loading their details
 * - Managing participants and poll results
 *
 * ## Architecture
 *
 * ```
 * Intent (user action)
 *   ↓
 * handleIntent()
 *   ↓
 * updateState() ← Updates the UI state
 * emitSideEffect() ← Triggers navigation/toasts
 * ```
 *
 * ## Usage Example (Android)
 *
 * ```kotlin
 * @Composable
 * fun EventListScreen(
 *     viewModel: EventManagementViewModel = koinViewModel()
 * ) {
 *     val state by viewModel.state.collectAsState()
 *
 *     LaunchedEffect(Unit) {
 *         viewModel.dispatch(EventManagementContract.Intent.LoadEvents)
 *     }
 *
 *     LaunchedEffect(Unit) {
 *         viewModel.sideEffect.collect { effect ->
 *             when (effect) {
 *                 is EventManagementContract.SideEffect.NavigateTo -> navigate(effect.route)
 *                 is EventManagementContract.SideEffect.ShowToast -> showToast(effect.message)
 *                 is EventManagementContract.SideEffect.NavigateBack -> navController.popBackStack()
 *             }
 *         }
 *     }
 *
 *     EventListContent(
 *         state = state) { intent ->
 *             viewModel.dispatch(intent)
 *         }
 *     }
 * ```
 *
 * ## Usage Example (iOS)
 *
 * ```swift
 * class EventListViewModel: ObservableObject {
 *     @Published var state: EventManagementContract.State
 *     private let stateMachine: ObservableStateMachine<...>
 *
 *     init() {
 *         stateMachine = IosFactory().createEventStateMachine()
 *         state = stateMachine.currentState
 *         stateMachine.onStateChange = { [weak self] in self?.state = $0 }
 *     }
 *
 *     func dispatch(_ intent: EventManagementContract.Intent) {
 *         stateMachine.dispatch(intent: intent)
 *     }
 * }
 * ```
 *
 * @property loadEventsUseCase Use case for loading events
 * @property createEventUseCase Use case for creating events
 * @property eventRepository Direct access to repository for additional operations (nullable)
 * @property scope CoroutineScope for launching async work
 */
class EventManagementStateMachine(
    private val loadEventsUseCase: LoadEventsUseCase,
    private val createEventUseCase: CreateEventUseCase,
    private val eventRepository: com.guyghost.wakeve.EventRepositoryInterface?,
    scope: CoroutineScope
) : StateMachine<EventManagementContract.State, EventManagementContract.Intent, EventManagementContract.SideEffect>(
    initialState = EventManagementContract.State(),
    scope = scope
) {

    override suspend fun handleIntent(intent: EventManagementContract.Intent) {
        when (intent) {
            is EventManagementContract.Intent.LoadEvents -> loadEvents()
            is EventManagementContract.Intent.SelectEvent -> selectEvent(intent.eventId)
            is EventManagementContract.Intent.CreateEvent -> createEvent(intent.event)
            is EventManagementContract.Intent.UpdateEvent -> updateEvent(intent.event)
            is EventManagementContract.Intent.DeleteEvent -> deleteEvent(intent.eventId)
            is EventManagementContract.Intent.LoadParticipants -> loadParticipants(intent.eventId)
            is EventManagementContract.Intent.AddParticipant -> addParticipant(intent.eventId, intent.participantId)
            is EventManagementContract.Intent.LoadPollResults -> loadPollResults(intent.eventId)
            is EventManagementContract.Intent.ClearError -> clearError()
        }
    }

    // ========================================================================
    // Intent Handlers
    // ========================================================================

    /**
     * Load all events from repository and update state.
     *
     * Flow:
     * 1. Set isLoading = true
     * 2. Call loadEventsUseCase()
     * 3. On success: update events list, set isLoading = false
     * 4. On failure: set error message, emit ShowToast, set isLoading = false
     */
    private suspend fun loadEvents() {
        updateState { it.copy(isLoading = true, error = null) }

        val result = loadEventsUseCase()

        result.fold(
            onSuccess = { events ->
                updateState { it.copy(isLoading = false, events = events) }
            },
            onFailure = { error ->
                val errorMessage = error.message ?: "Failed to load events"
                updateState { it.copy(isLoading = false, error = errorMessage) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMessage))
            }
        )
    }

    /**
     * Select an event and prepare for detail view.
     *
     * Flow:
     * 1. Find the event in current events list
     * 2. Update selectedEvent
     * 3. Load its participants and poll results
     * 4. Emit NavigateTo side effect
     *
     * @param eventId The ID of the event to select
     */
    private suspend fun selectEvent(eventId: String) {
        val event = currentState.events.find { it.id == eventId }

        if (event == null) {
            updateState { it.copy(error = "Event not found") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event not found"))
            return
        }

        updateState { it.copy(selectedEvent = event) }

        // Load additional data
        loadParticipants(eventId)
        loadPollResults(eventId)

        // Emit navigation side effect
        emitSideEffect(EventManagementContract.SideEffect.NavigateTo("detail/$eventId"))
    }

    /**
     * Create a new event.
     *
     * Flow:
     * 1. Set isLoading = true
     * 2. Call createEventUseCase(event)
     * 3. On success: refresh events list, emit ShowToast, emit NavigateBack
     * 4. On failure: set error, emit ShowToast
     *
     * @param event The event to create
     */
    private suspend fun createEvent(event: com.guyghost.wakeve.models.Event) {
        updateState { it.copy(isLoading = true, error = null) }

        val result = createEventUseCase(event)

        result.fold(
            onSuccess = {
                // Refresh events list
                loadEvents()
                emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event created successfully"))
                emitSideEffect(EventManagementContract.SideEffect.NavigateBack)
            },
            onFailure = { error ->
                val errorMessage = error.message ?: "Failed to create event"
                updateState { it.copy(isLoading = false, error = errorMessage) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMessage))
            }
        )
    }

    /**
     * Update an existing event.
     *
     * Flow:
     * 1. Check if repository is available
     * 2. Set isLoading = true
     * 3. Call repository.updateEvent(event)
     * 4. On success: update in state, emit ShowToast
     * 5. On failure: set error, emit ShowToast
     *
     * @param event The updated event
     */
    private suspend fun updateEvent(event: com.guyghost.wakeve.models.Event) {
        if (eventRepository == null) {
            updateState { it.copy(error = "Repository not available") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Repository not available"))
            return
        }

        updateState { it.copy(isLoading = true, error = null) }

        val result = eventRepository.updateEvent(event)

        result.fold(
            onSuccess = {
                // Update in state
                val updatedEvents = currentState.events.map { if (it.id == event.id) event else it }
                updateState { it.copy(isLoading = false, events = updatedEvents, selectedEvent = event) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event updated successfully"))
            },
            onFailure = { error ->
                val errorMessage = error.message ?: "Failed to update event"
                updateState { it.copy(isLoading = false, error = errorMessage) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMessage))
            }
        )
    }

    /**
     * Delete an event.
     *
     * Currently a placeholder - deletion logic would be similar to create/update.
     *
     * @param eventId The ID of the event to delete
     */
    private suspend fun deleteEvent(eventId: String) {
        // TODO: Implement deletion in Phase 2
        updateState { it.copy(error = "Delete not yet implemented") }
        emitSideEffect(EventManagementContract.SideEffect.ShowToast("Delete not yet implemented"))
    }

    /**
     * Load participants for an event.
     *
     * Flow:
     * 1. Check if repository is available
     * 2. Call repository.getParticipants(eventId)
     * 3. Update state with participant IDs
     *
     * @param eventId The ID of the event
     */
    private suspend fun loadParticipants(eventId: String) {
        if (eventRepository == null) return

        val participantIds = eventRepository.getParticipants(eventId) ?: return

        updateState { it.copy(participantIds = participantIds) }
    }

    /**
     * Add a participant to an event.
     *
     * Flow:
     * 1. Check if repository is available
     * 2. Call repository.addParticipant(eventId, participantId)
     * 3. On success: reload participants, emit ShowToast
     * 4. On failure: emit ShowToast with error
     *
     * @param eventId The ID of the event
     * @param participantId The ID of the participant to add
     */
    private suspend fun addParticipant(eventId: String, participantId: String) {
        if (eventRepository == null) {
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Repository not available"))
            return
        }

        val result = eventRepository.addParticipant(eventId, participantId)

        result.fold(
            onSuccess = {
                loadParticipants(eventId)
                emitSideEffect(EventManagementContract.SideEffect.ShowToast("Participant added successfully"))
            },
            onFailure = { error ->
                val errorMessage = error.message ?: "Failed to add participant"
                emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMessage))
            }
        )
    }

    /**
     * Load poll results for an event.
     *
     * Flow:
     * 1. Check if repository is available
     * 2. Call repository.getPoll(eventId)
     * 3. Update state with poll votes
     *
     * @param eventId The ID of the event
     */
    private suspend fun loadPollResults(eventId: String) {
        if (eventRepository == null) return

        val poll = eventRepository.getPoll(eventId) ?: return

        updateState { it.copy(pollVotes = poll.votes) }
    }

    /**
     * Clear error state.
     *
     * Used to dismiss error messages in the UI.
     */
    private suspend fun clearError() {
        updateState { it.copy(error = null) }
    }
}
