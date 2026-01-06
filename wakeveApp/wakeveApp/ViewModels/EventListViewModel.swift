import SwiftUI
import Shared

/// ViewModel for the EventListView.
///
/// Manages the state and intents for displaying a list of events.
/// Uses the shared Kotlin state machine to handle all business logic.
///
/// ## Usage
///
/// ```swift
/// @StateObject private var viewModel = EventListViewModel()
///
/// List(viewModel.state.events) { event in
///     EventRow(event)
///         .onTapGesture {
///             viewModel.selectEvent(eventId: event.id)
///         }
/// }
/// ```
@MainActor
class EventListViewModel: ObservableObject {
    // MARK: - Published Properties
    
    /// Current state from the state machine
    @Published var state: EventManagementContract.State
    
    /// Toast message to display (auto-clears after display)
    @Published var toastMessage: String?
    
    /// Navigation route to trigger (e.g., "detail/event-123")
    @Published var navigationRoute: String?
    
    /// Whether to pop back to previous screen
    @Published var shouldNavigateBack = false
    
    // MARK: - Private Properties
    
    /// The observable state machine wrapper
    private let stateMachineWrapper: ObservableStateMachine<
        EventManagementContract.State,
        EventManagementContractIntent,
        EventManagementContractSideEffect
    >
    
    // MARK: - Initialization
    
    init() {
        // Get the shared database from RepositoryProvider
        let database = RepositoryProvider.shared.database
        
        // Create the state machine via iOS factory (IosFactory is an object/singleton)
        self.stateMachineWrapper = IosFactory.shared.createEventStateMachine(database: database)
        
        // Initialize state with current state from state machine
        self.state = self.stateMachineWrapper.currentState!
        
        // Observe state changes from the state machine
        self.stateMachineWrapper.onStateChange = { [weak self] newState in
            guard let self = self, let newState = newState else { return }
            
            DispatchQueue.main.async {
                self.state = newState
            }
        }
        
        // Observe side effects from the state machine
        self.stateMachineWrapper.onSideEffect = { [weak self] effect in
            guard let self = self, let effect = effect else { return }
            
            DispatchQueue.main.async {
                self.handleSideEffect(effect)
            }
        }
        
        // Load events on initialization
        loadEvents()
    }
    
    // MARK: - Public Methods
    
    /// Dispatch an intent to the state machine.
    func dispatch(_ intent: EventManagementContractIntent) {
        stateMachineWrapper.dispatch(intent: intent)
    }
    
    /// Load events from the repository
    func loadEvents() {
        dispatch(EventManagementContractIntentLoadEvents.shared)
    }
    
    /// Select an event by ID
    func selectEvent(eventId: String) {
        dispatch(EventManagementContractIntentSelectEvent(eventId: eventId))
    }
    
    /// Clear any error state
    func clearError() {
        dispatch(EventManagementContractIntentClearError.shared)
    }
    
    // MARK: - Private Methods
    
    /// Handle side effects emitted by the state machine.
    private func handleSideEffect(_ effect: EventManagementContractSideEffect) {
        switch effect {
        case let showToast as EventManagementContractSideEffectShowToast:
            toastMessage = showToast.message
        case let navigateTo as EventManagementContractSideEffectNavigateTo:
            navigationRoute = navigateTo.route
        case is EventManagementContractSideEffectNavigateBack:
            shouldNavigateBack = true
        default:
            break
        }
    }
    
    // MARK: - Deinit
    
    deinit {
        // Clean up state machine resources
        stateMachineWrapper.dispose()
    }
}
