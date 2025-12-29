package com.guyghost.wakeve.di

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.presentation.ObservableStateMachine
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Factory for creating observable state machines for iOS.
 *
 * This factory is responsible for creating state machine instances that are
 * properly wrapped for SwiftUI consumption. It handles:
 * - Coroutine scope creation with proper dispatchers
 * - Dependency creation
 * - Wrapping state machines in ObservableStateMachine for Swift/iOS
 *
 * ## Usage in iOS/SwiftUI
 *
 * ```swift
 * import SwiftUI
 * import shared
 *
 * class EventListViewModel: ObservableObject {
 *     @Published var state: EventManagementContract.State
 *     private let wrapped: ViewModelWrapper
 *
 *     init() {
 *         // Create the observable state machine
 *         self.wrapped = IosFactory().createEventStateMachine()
 *         self.state = wrapped.currentState as! EventManagementContract.State
 *
 *         // Observe state changes
 *         wrapped.onStateChange = { [weak self] newState in
 *             self?.state = newState as? EventManagementContract.State ?? self?.state ?? .init()
 *         }
 *
 *         // Observe side effects
 *         wrapped.onSideEffect = { [weak self] effect in
 *             self?.handleSideEffect(effect)
 *         }
 *     }
 *
 *     func dispatch(_ intent: EventManagementContract.Intent) {
 *         wrapped.dispatch(intent: intent)
 *     }
 *
 *     private func handleSideEffect(_ effect: Any) {
 *         // Handle navigation, toasts, etc
 *     }
 *
 *     deinit {
 *         wrapped.dispose()
 *     }
 * }
 *
 * struct EventListView: View {
 *     @StateObject private var viewModel = EventListViewModel()
 *
 *     var body: some View {
 *         // Render using viewModel.state
 *     }
 * }
 * ```
 */
object IosFactory {

    /**
     * Create an observable Event Management state machine.
     *
     * Flow:
     * 1. Create a CoroutineScope with Main dispatcher
     * 2. Create dependencies manually (EventRepository, UseCases)
     * 3. Create the state machine with the scope
     * 4. Wrap it in ObservableStateMachine for SwiftUI
     * 5. Return the wrapper
     *
     * The returned wrapper exposes:
     * - `currentState`: The initial state
     * - `onStateChange`: Callback called when state updates
     * - `onSideEffect`: Callback called when side effects emit
     * - `dispatch(intent:)`: Method to dispatch intents
     * - `dispose()`: Method to clean up resources
     *
     * @param database The WakevDb instance (must be provided by iOS app)
     * @return An ObservableStateMachine wrapper for Event Management
      */
     fun createEventStateMachine(database: WakevDb): ObservableStateMachine<
             EventManagementContract.State,
             EventManagementContract.Intent,
             EventManagementContract.SideEffect
     > {
        // Create scope with Main dispatcher for iOS
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // Create dependencies
        val eventRepository: EventRepositoryInterface = DatabaseEventRepository(database)
        val loadEventsUseCase = LoadEventsUseCase(eventRepository)
        val createEventUseCase = CreateEventUseCase(eventRepository)

        // Create state machine
        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = eventRepository,
            scope = scope
        )

         // Wrap for SwiftUI
         return ObservableStateMachine(stateMachine)
    }
}
