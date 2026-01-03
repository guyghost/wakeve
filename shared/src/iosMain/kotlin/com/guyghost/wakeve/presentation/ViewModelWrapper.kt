package com.guyghost.wakeve.presentation

import com.guyghost.wakeve.presentation.statemachine.StateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS bridge to expose a Kotlin [StateMachine] to SwiftUI via callbacks.
 *
 * This wrapper allows SwiftUI views to observe state changes and side effects
 * from a Kotlin state machine. It handles the necessary thread marshalling
 * (Kotlin coroutines to iOS main thread).
 *
 * ## SwiftUI Usage Example
 *
 * ```swift
 * import SwiftUI
 * import shared
 *
 * class EventListViewModel: ObservableObject {
 *     @Published var state: EventManagementContract.State
 *     private let wrapped: ObservableStateMachine<...>
 *
 *     init() {
 *         self.wrapped = IosFactory().createEventStateMachine()
 *         self.state = wrapped.currentState
 *         wrapped.onStateChange = { [weak self] newState in
 *             self?.state = newState
 *         }
 *         wrapped.onSideEffect = { effect in
 *             self?.handleSideEffect(effect)
 *         }
 *     }
 *
 *     func dispatch(_ intent: EventManagementContract.Intent) {
 *         wrapped.dispatch(intent: intent)
 *     }
 *
 *     deinit {
 *         wrapped.dispose()
 *     }
 * }
 * ```
 *
 * @param State The type of state
 * @param Intent The type of intents
 * @param SideEffect The type of side effects
 * @property stateMachine The underlying Kotlin state machine
 */
class ObservableStateMachine<State, Intent, SideEffect>(
    private val stateMachine: StateMachine<State, Intent, SideEffect>
) {
    // ============================================================
    // Scope Management
    // ============================================================

    /**
     * Coroutine scope for observing state and side effects.
     * Uses MainDispatcher to ensure callbacks are called on the main thread.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ============================================================
    // Callbacks
    // ============================================================

    /**
     * Called whenever state changes.
     *
     * This callback is invoked on the main thread whenever the state machine's
     * state is updated. Use this in SwiftUI to update @Published properties.
     *
     * Set by SwiftUI view controller in init.
     */
    var onStateChange: ((State) -> Unit)? = null

    /**
     * Called whenever a side effect is emitted.
     *
     * This callback is invoked on the main thread whenever a one-shot side effect
     * is emitted (navigation, toast, etc). Handle these separately from state changes.
     *
     * Set by SwiftUI view controller in init.
     */
    var onSideEffect: ((SideEffect) -> Unit)? = null

    // ============================================================
    // State Access
    // ============================================================

    /**
     * Get the current state value immediately without collecting.
     *
     * Use this to initialize SwiftUI @Published properties.
     */
    val currentState: State get() = stateMachine.state.value

    // ============================================================
    // Initialization
    // ============================================================

    init {
        // ============================================================
        // Observe State Changes
        // ============================================================
        scope.launch {
            stateMachine.state.collect { newState ->
                // Marshal back to main thread for SwiftUI callback
                dispatch_async(dispatch_get_main_queue()) {
                    onStateChange?.invoke(newState)
                }
            }
        }

        // ============================================================
        // Observe Side Effects
        // ============================================================
        scope.launch {
            stateMachine.sideEffect.collect { effect ->
                // Marshal back to main thread for SwiftUI callback
                dispatch_async(dispatch_get_main_queue()) {
                    onSideEffect?.invoke(effect)
                }
            }
        }
    }

    // ============================================================
    // Intent Dispatch
    // ============================================================

    /**
     * Dispatch an intent to the underlying state machine.
     *
     * @param intent The intent to dispatch
     */
    fun dispatch(intent: Intent) {
        stateMachine.dispatch(intent)
    }

    // ============================================================
    // Lifecycle
    // ============================================================

    /**
     * Dispose of this wrapper and cancel all coroutines.
     *
     * Call this from SwiftUI's deinit to clean up resources.
     *
     * ```swift
     * deinit {
     *     stateMachineWrapper.dispose()
     * }
     * ```
     */
    fun dispose() {
        scope.cancel()
    }
}
