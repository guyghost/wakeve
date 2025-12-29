package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for event management that wraps the EventManagementStateMachine.
 *
 * This ViewModel serves as the bridge between Jetpack Compose UI and the state machine.
 * It exposes the state and side effects from the state machine in a way that Compose
 * can consume them efficiently via StateFlow and Flow.
 *
 * ## Usage in Compose
 *
 * ```kotlin
 * @Composable
 * fun EventListScreen(
 *     viewModel: EventManagementViewModel = koinViewModel()
 * ) {
 *     val state by viewModel.state.collectAsState()
 *
 *     // Load events when screen appears
 *     LaunchedEffect(Unit) {
 *         viewModel.dispatch(EventManagementContract.Intent.LoadEvents)
 *     }
 *
 *     // Handle side effects (navigation, toasts, etc.)
 *     LaunchedEffect(Unit) {
 *         viewModel.sideEffect.collect { effect ->
 *             when (effect) {
 *                 is EventManagementContract.SideEffect.NavigateTo -> {
 *                     navController.navigate(effect.route)
 *                 }
 *                 is EventManagementContract.SideEffect.ShowToast -> {
 *                     showToast(effect.message)
 *                 }
 *                 is EventManagementContract.SideEffect.NavigateBack -> {
 *                     navController.popBackStack()
 *                 }
 *             }
 *         }
 *     }
 *
 *     // Render UI with state
 *     EventListContent(
 *         state = state,
 *         onIntent = { viewModel.dispatch(it) }
 *     )
 * }
 * ```
 *
 * @property stateMachine The underlying state machine for event management
 */
class EventManagementViewModel(
    private val stateMachine: EventManagementStateMachine
) : ViewModel() {

    /**
     * Observable state flow from the state machine.
     *
     * Collect this in Compose UI using:
     * ```kotlin
     * val state by viewModel.state.collectAsState()
     * ```
     *
     * This state includes:
     * - `isLoading`: Whether events are being loaded
     * - `events`: List of all loaded events
     * - `selectedEvent`: The currently selected event (for detail views)
     * - `participantIds`: List of participant IDs for the selected event
     * - `pollVotes`: Poll voting data for the selected event
     * - `error`: Error message if an operation failed
     */
    val state: StateFlow<EventManagementContract.State> = stateMachine.state

    /**
     * Observable side effect flow from the state machine.
     *
     * Collect this in a LaunchedEffect to handle one-shot events:
     * ```kotlin
     * LaunchedEffect(Unit) {
     *     viewModel.sideEffect.collect { effect ->
     *         when (effect) {
     *             is EventManagementContract.SideEffect.NavigateTo -> navigate(effect.route)
     *             is EventManagementContract.SideEffect.ShowToast -> showToast(effect.message)
     *             is EventManagementContract.SideEffect.NavigateBack -> navController.popBackStack()
     *         }
     *     }
     * }
     * ```
     *
     * Side effects are one-time events that don't persist in state:
     * - Navigation to other screens
     * - Toast messages
     * - Back navigation
     */
    val sideEffect: Flow<EventManagementContract.SideEffect> = stateMachine.sideEffect

    /**
     * Dispatch an intent to the state machine.
     *
     * This is the primary way to trigger state updates or side effects.
     * All user actions in the UI should be translated to intents and dispatched here.
     *
     * ## Example
     *
     * ```kotlin
     * Button(onClick = {
     *     viewModel.dispatch(EventManagementContract.Intent.LoadEvents)
     * }) {
     *     Text("Reload")
     * }
     * ```
     *
     * @param intent The intent to dispatch to the state machine
     */
    fun dispatch(intent: EventManagementContract.Intent) {
        stateMachine.dispatch(intent)
    }

    /**
     * Optional: Clear error state convenience method.
     *
     * This is a common operation, so it's provided as a helper method.
     * It's equivalent to:
     * ```kotlin
     * viewModel.dispatch(EventManagementContract.Intent.ClearError)
     * ```
     */
    fun clearError() {
        dispatch(EventManagementContract.Intent.ClearError)
    }

    /**
     * Optional: Load events convenience method.
     *
     * This is a common operation, so it's provided as a helper method.
     * It's equivalent to:
     * ```kotlin
     * viewModel.dispatch(EventManagementContract.Intent.LoadEvents)
     * ```
     */
    fun loadEvents() {
        dispatch(EventManagementContract.Intent.LoadEvents)
    }

    /**
     * Optional: Select an event convenience method.
     *
     * This is a common operation, so it's provided as a helper method.
     * It's equivalent to:
     * ```kotlin
     * viewModel.dispatch(EventManagementContract.Intent.SelectEvent(eventId))
     * ```
     *
     * @param eventId The ID of the event to select
     */
    fun selectEvent(eventId: String) {
        dispatch(EventManagementContract.Intent.SelectEvent(eventId))
    }
}
