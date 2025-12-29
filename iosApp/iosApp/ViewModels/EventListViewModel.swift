import SwiftUI
import Shared

/// ViewModel for the EventListView.
///
/// Manages the state and intents for displaying a list of events.
/// Uses the shared Kotlin state machine to handle all business logic.
///
/// ## State Management
///
/// The ViewModel:
/// 1. Creates a state machine via `IosFactory`
/// 2. Observes state changes from the state machine
/// 3. Observes side effects for navigation and toasts
/// 4. Exposes `dispatch()` method for the view to send intents
///
/// ## Usage
///
/// ```swift
/// @StateObject private var viewModel = EventListViewModel()
///
/// List(viewModel.state.events) { event in
///     EventRow(event)
///         .onTapGesture {
///             viewModel.dispatch(
///                 EventManagementContractIntent.selectEvent(eventId: event.id)
///             )
///         }
/// }
/// ```
@MainActor
class EventListViewModel: ObservableObject {
    // MARK: - Published Properties
    
    /// Current state from the state machine
    @Published var state: EventManagementContractState
    
    /// Toast message to display (auto-clears after display)
    @Published var toastMessage: String?
    
    /// Navigation route to trigger (e.g., "detail/event-123")
    @Published var navigationRoute: String?
    
    /// Whether to pop back to previous screen
    @Published var shouldNavigateBack = false
    
    // MARK: - Private Properties
    
    /// The observable state machine wrapper
    private let stateMachineWrapper: ViewModelWrapper<
        EventManagementContractState,
        EventManagementContractIntent,
        EventManagementContractSideEffect
    >
    
    // MARK: - Initialization
    
    init() {
        // Get the shared database from RepositoryProvider
        let database = RepositoryProvider.shared.database
        
        // Create the state machine via iOS factory
        self.stateMachineWrapper = IosFactory().createEventStateMachine(database: database)
        
        // Initialize state with current state from state machine
        self.state = self.stateMachineWrapper.currentState as! EventManagementContractState
        
        // Observe state changes from the state machine
        self.stateMachineWrapper.onStateChange = { [weak self] newState in
            guard let self = self else { return }
            
            DispatchQueue.main.async {
                if let newState = newState as? EventManagementContractState {
                    self.state = newState
                }
            }
        }
        
        // Observe side effects from the state machine
        self.stateMachineWrapper.onSideEffect = { [weak self] effect in
            guard let self = self else { return }
            
            DispatchQueue.main.async {
                self.handleSideEffect(effect)
            }
        }
        
        // Load events on initialization
        dispatch(EventManagementContractIntent.loadEvents())
    }
    
    // MARK: - Public Methods
    
    /// Dispatch an intent to the state machine.
    ///
    /// - Parameter intent: The intent to dispatch (e.g., LoadEvents, SelectEvent)
    func dispatch(_ intent: EventManagementContractIntent) {
        stateMachineWrapper.dispatch(intent: intent)
    }
    
    // MARK: - Private Methods
    
    /// Handle side effects emitted by the state machine.
    ///
    /// - Parameter effect: The side effect to handle
    private func handleSideEffect(_ effect: Any) {
        if let showToast = effect as? EventManagementContractSideEffectShowToast {
            toastMessage = showToast.message
        } else if let navigateTo = effect as? EventManagementContractSideEffectNavigateTo {
            navigationRoute = navigateTo.route
        } else if effect is EventManagementContractSideEffectNavigateBack {
            shouldNavigateBack = true
        }
    }
    
    // MARK: - Deinit
    
    deinit {
        // Clean up state machine resources
        stateMachineWrapper.dispose()
    }
}

// MARK: - Type Extensions

/// Helper to create LoadEvents intent
extension EventManagementContractIntent {
    static func loadEvents() -> EventManagementContractIntent {
        return EventManagementContractIntent.loadEvents()
    }
    
    /// Helper to create SelectEvent intent
    static func selectEvent(eventId: String) -> EventManagementContractIntent {
        return EventManagementContractIntent.selectEvent(eventId: eventId)
    }
    
    /// Helper to create ClearError intent
    static func clearError() -> EventManagementContractIntent {
        return EventManagementContractIntent.clearError()
    }
}
