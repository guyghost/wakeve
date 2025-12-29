package com.guyghost.wakeve.presentation.state

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.Vote
import kotlinx.serialization.Serializable

/**
 * Contract for Event Management state machine.
 *
 * Defines the State, Intent and SideEffect types for the EventManagementStateMachine.
 * This contract is used by both Android (Compose) and iOS (SwiftUI).
 *
 * ## State Flow Example
 *
 * 1. User opens EventListScreen
 * 2. Screen dispatches Intent.LoadEvents
 * 3. StateMachine loads events from repository, updates state with isLoading=true
 * 4. Events loaded, state updated with events list, isLoading=false
 * 5. UI recomposes with new state
 * 6. User taps event, screen dispatches Intent.SelectEvent(eventId)
 * 7. StateMachine emits SideEffect.NavigateTo("detail/{eventId}")
 * 8. UI listens to sideEffects and performs navigation
 */
object EventManagementContract {

    // ========================================================================
    // STATE
    // ========================================================================

    /**
     * Immutable state for event management.
     *
     * This state is consumed by UI to render the event list and details.
     * All fields are immutable - updates are done via copy().
     *
     * @property isLoading True while loading events from repository
     * @property events List of loaded events (can be empty)
     * @property selectedEvent The currently selected event (for detail view)
     * @property participantIds List of participant IDs for a selected event
     * @property pollVotes Map of votes for a selected event's poll
     * @property error Error message if operation failed (null = no error)
     */
    @Serializable
    data class State(
        val isLoading: Boolean = false,
        val events: List<Event> = emptyList(),
        val selectedEvent: Event? = null,
        val participantIds: List<String> = emptyList(),
        val pollVotes: Map<String, Map<String, Vote>> = emptyMap(),
        val error: String? = null
    ) {
        /**
         * Convenient property to check if there's an error
         */
        val hasError: Boolean get() = error != null

        /**
         * Convenient property to check if list is empty
         */
        val isEmpty: Boolean get() = events.isEmpty()
    }

    // ========================================================================
    // INTENT
    // ========================================================================

    /**
     * Intents that can be dispatched to the state machine.
     *
     * Each intent represents a user action or lifecycle event that should
     * trigger state updates or side effects.
     *
     * Use sealed interfaces for type-safe intent handling:
     * ```kotlin
     * when (intent) {
     *     is Intent.LoadEvents -> { }
     *     is Intent.SelectEvent -> { }
     *     // ... exhaustive
     * }
     * ```
     */
    sealed interface Intent {
        /**
         * Load all events from repository.
         *
         * Typically dispatched when EventListScreen appears.
         * Sets isLoading=true, then loads events, then sets isLoading=false.
         */
        data object LoadEvents : Intent

        /**
         * Select an event for viewing details.
         *
         * Sets selectedEvent and loads its participants/poll data.
         * Emits NavigateTo side effect to navigate to detail screen.
         *
         * @property eventId The ID of the event to select
         */
        data class SelectEvent(val eventId: String) : Intent

        /**
         * Create a new event.
         *
         * Persists the event to the repository.
         * Emits side effects for success or error handling.
         *
         * @property event The event to create
         */
        data class CreateEvent(val event: Event) : Intent

        /**
         * Update an existing event.
         *
         * Only the organizer can update.
         * Persists to repository and updates local state.
         *
         * @property event The updated event
         */
        data class UpdateEvent(val event: Event) : Intent

        /**
         * Delete an event.
         *
         * Only the organizer can delete.
         * Removes from repository and updates local state.
         *
         * @property eventId The ID of the event to delete
         */
        data class DeleteEvent(val eventId: String) : Intent

        /**
         * Load participants for a selected event.
         *
         * Called after selecting an event to populate participantIds.
         *
         * @property eventId The ID of the event
         */
        data class LoadParticipants(val eventId: String) : Intent

        /**
         * Add a participant to an event.
         *
         * Only works if event is in DRAFT status.
         *
         * @property eventId The ID of the event
         * @property participantId The ID of the participant to add
         */
        data class AddParticipant(val eventId: String, val participantId: String) : Intent

        /**
         * Load poll results for a selected event.
         *
         * Called after selecting an event to populate pollVotes.
         *
         * @property eventId The ID of the event
         */
        data class LoadPollResults(val eventId: String) : Intent

        /**
         * Clear any error state.
         *
         * Use this to dismiss error messages in the UI.
         */
        data object ClearError : Intent
    }

    // ========================================================================
    // SIDE EFFECT
    // ========================================================================

    /**
     * One-shot side effects emitted by the state machine.
     *
     * Side effects are NOT part of the state - they are one-time events
     * that need to be handled by the UI (navigation, showing toasts, etc).
     *
     * Each side effect should be collected and handled once, then discarded.
     *
     * ```kotlin
     * LaunchedEffect(Unit) {
     *     viewModel.sideEffect.collect { effect ->
     *         when (effect) {
     *             is SideEffect.NavigateTo -> navigation.navigate(effect.route)
     *             is SideEffect.ShowToast -> showToast(effect.message)
     *         }
     *     }
     * }
     * ```
     */
    sealed interface SideEffect {
        /**
         * Show a toast message to the user.
         *
         * Typically used for success/error confirmation messages.
         *
         * @property message The message to show
         */
        data class ShowToast(val message: String) : SideEffect

        /**
         * Navigate to a specific route/screen.
         *
         * The UI is responsible for interpreting the route and navigating.
         *
         * @property route The route to navigate to (e.g., "detail/event-123")
         */
        data class NavigateTo(val route: String) : SideEffect

        /**
         * Navigate back to the previous screen.
         *
         * The UI should interpret this as popping the back stack.
         */
        data object NavigateBack : SideEffect
    }
}
