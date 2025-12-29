package com.guyghost.wakeve.presentation.statemachine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Base class for all state machines implementing the MVI/FSM (Model-View-Intent / Finite State Machine) pattern.
 *
 * This class provides:
 * - Immutable state management via [StateFlow]
 * - One-shot side effects via [Channel]
 * - Thread-safe coroutine-based intent handling
 *
 * @param State The type of state managed by this state machine
 * @param Intent The type of intents this state machine can handle
 * @param SideEffect The type of side effects this state machine can emit
 *
 * @property initialState The initial state of the state machine
 * @property scope The [CoroutineScope] to use for launching coroutines
 *
 * ## Usage Example
 *
 * ```kotlin
 * class EventManagementStateMachine(
 *     loadEventsUseCase: LoadEventsUseCase,
 *     scope: CoroutineScope
 * ) : StateMachine<EventManagementContract.State, EventManagementContract.Intent, EventManagementContract.SideEffect>(
 *     initialState = EventManagementContract.State(),
 *     scope = scope
 * ) {
 *     override suspend fun handleIntent(intent: EventManagementContract.Intent) {
 *         when (intent) {
 *             is EventManagementContract.Intent.LoadEvents -> loadEvents()
 *             // ... handle other intents
 *         }
 *     }
 *
 *     private suspend fun loadEvents() {
 *         updateState { it.copy(isLoading = true) }
 *         // ... perform async work
 *         updateState { it.copy(isLoading = false, events = result) }
 *     }
 * }
 * ```
 */
abstract class StateMachine<State, Intent, SideEffect>(
    initialState: State,
    private val scope: CoroutineScope
) {
    // ============================================================
    // State Management
    // ============================================================

    /**
     * Internal mutable state flow
     */
    private val _state = MutableStateFlow(initialState)

    /**
     * Observable state flow - read-only, collect changes
     */
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * Get current state value without collecting
     */
    protected val currentState: State get() = _state.value

    // ============================================================
    // Side Effects
    // ============================================================

    /**
     * Channel for one-shot side effects that need to be handled once (e.g., navigation, toasts)
     */
    private val _sideEffect = Channel<SideEffect>(Channel.BUFFERED)

    /**
     * Observable side effect flow
     */
    val sideEffect: Flow<SideEffect> = _sideEffect.receiveAsFlow()

    // ============================================================
    // Intent Dispatch
    // ============================================================

    /**
     * Dispatch an intent to this state machine.
     *
     * The intent is processed asynchronously in the provided scope.
     * Multiple intents can be dispatched concurrently.
     *
     * @param intent The intent to process
     */
    fun dispatch(intent: Intent) {
        scope.launch {
            handleIntent(intent)
        }
    }

    // ============================================================
    // Abstract Methods - Override in Subclass
    // ============================================================

    /**
     * Handle the given intent and update state/emit side effects as needed.
     *
     * This method is called in the scope's context. It's safe to call suspend functions.
     *
     * @param intent The intent to handle
     */
    protected abstract suspend fun handleIntent(intent: Intent)

    // ============================================================
    // Protected Utilities - Use in Subclass
    // ============================================================

    /**
     * Update state using a reducer function.
     *
     * The reducer receives the current state and returns the new state.
     * This operation is atomic and thread-safe.
     *
     * @param reducer Lambda that receives current state and returns new state
     *
     * ## Usage
     * ```kotlin
     * updateState { currentState ->
     *     currentState.copy(isLoading = false, items = newItems)
     * }
     * ```
     */
    protected fun updateState(reducer: (State) -> State) {
        _state.value = reducer(_state.value)
    }

    /**
     * Emit a one-shot side effect.
     *
     * This is typically used for non-state changes like navigation events, toast messages, or
     * opening dialogs. Side effects are NOT persisted in state.
     *
     * This is a suspend function and should be called from within [handleIntent].
     *
     * @param effect The side effect to emit
     *
     * ## Usage
     * ```kotlin
     * emitSideEffect(EventManagementContract.SideEffect.NavigateTo("detail/${eventId}"))
     * ```
     */
    protected suspend fun emitSideEffect(effect: SideEffect) {
        _sideEffect.send(effect)
    }
}
