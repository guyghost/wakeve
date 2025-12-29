import SwiftUI
import Shared

/// ViewModel for the EventDetailView.
///
/// Manages the state and intents for displaying detailed information about a single event.
/// Uses the shared Kotlin state machine to handle all business logic.
///
/// ## State Management
///
/// The ViewModel:
/// 1. Creates a state machine via `IosFactory`
/// 2. Observes state changes from the state machine
/// 3. Observes side effects for navigation and toasts
/// 4. Exposes `dispatch()` method for the view to send intents
/// 5. Filters the selected event from the state's events list
///
/// ## Usage
///
/// ```swift
/// @StateObject private var viewModel = EventDetailViewModel(eventId: "event-123")
///
/// if let event = viewModel.selectedEvent {
///     VStack {
///         Text(event.title)
///         Text(event.eventDescription)
///     }
/// }
/// ```
@MainActor
class EventDetailViewModel: ObservableObject {
    // MARK: - Published Properties
    
    /// Current state from the state machine
    @Published var state: EventManagementContractState
    
    /// The currently selected event (filtered from state.events)
    @Published var selectedEvent: Event?
    
    /// Toast message to display (auto-clears after display)
    @Published var toastMessage: String?
    
    /// Navigation route to trigger
    @Published var navigationRoute: String?
    
    /// Whether to pop back to previous screen
    @Published var shouldNavigateBack = false
    
    // MARK: - Private Properties
    
    /// The event ID passed to this view model
    private let eventId: String
    
    /// The observable state machine wrapper
    private let stateMachineWrapper: ViewModelWrapper<
        EventManagementContractState,
        EventManagementContractIntent,
        EventManagementContractSideEffect
    >
    
    // MARK: - Initialization
    
    /// Initialize the ViewModel with a specific event ID.
    ///
    /// - Parameter eventId: The ID of the event to display details for
    init(eventId: String) {
        self.eventId = eventId
        
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
                    self.updateSelectedEvent()
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
        
        // Load participants and poll results
        dispatch(EventManagementContractIntent.loadParticipants(eventId: eventId))
        dispatch(EventManagementContractIntent.loadPollResults(eventId: eventId))
    }
    
    // MARK: - Public Methods
    
    /// Dispatch an intent to the state machine.
    ///
    /// - Parameter intent: The intent to dispatch
    func dispatch(_ intent: EventManagementContractIntent) {
        stateMachineWrapper.dispatch(intent: intent)
    }
    
    // MARK: - Private Methods
    
    /// Update the selected event by filtering from the state's events list.
    ///
    /// This is called whenever the state changes to keep selectedEvent in sync.
    private func updateSelectedEvent() {
        selectedEvent = state.events.first { $0.id == eventId }
    }
    
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

/// Helper to create LoadParticipants intent
extension EventManagementContractIntent {
    static func loadParticipants(eventId: String) -> EventManagementContractIntent {
        return EventManagementContractIntent.loadParticipants(eventId: eventId)
    }
    
    /// Helper to create LoadPollResults intent
    static func loadPollResults(eventId: String) -> EventManagementContractIntent {
        return EventManagementContractIntent.loadPollResults(eventId: eventId)
    }
    
    /// Helper to create DeleteEvent intent
    static func deleteEvent(eventId: String) -> EventManagementContractIntent {
        return EventManagementContractIntent.deleteEvent(eventId: eventId)
    }
    
    /// Helper to create UpdateEvent intent
    static func updateEvent(_ event: Event) -> EventManagementContractIntent {
        return EventManagementContractIntent.updateEvent(event: event)
    }
}
