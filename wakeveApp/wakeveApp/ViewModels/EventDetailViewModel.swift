import SwiftUI
import Shared

/// ViewModel for the EventDetailView.
///
/// Manages the state and intents for displaying detailed information about a single event.
/// Uses the shared Kotlin state machine to handle all business logic.
///
/// ## Usage
///
/// ```swift
/// @StateObject private var viewModel = EventDetailViewModel(eventId: "event-123", userId: "user-456")
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
    @Published var state: EventManagementContract.State
    
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
    
    /// The current user ID (for organizer checks and delete permission)
    private let userId: String
    
    /// The observable state machine wrapper
    private let stateMachineWrapper: ObservableStateMachine<
        EventManagementContract.State,
        EventManagementContractIntent,
        EventManagementContractSideEffect
    >
    
    // MARK: - Computed Properties
    
    /// Whether the current user is the organizer of this event
    var isOrganizer: Bool {
        selectedEvent?.organizerId == userId
    }
    
    /// Whether the user can delete this event (organizer AND not FINALIZED)
    var canDelete: Bool {
        isOrganizer && selectedEvent?.status != .finalized
    }
    
    // MARK: - Initialization
    
    /// Initialize the ViewModel with a specific event ID and user ID.
    /// - Parameters:
    ///   - eventId: The ID of the event to display
    ///   - userId: The current user's ID (used for organizer check and delete permission)
    init(eventId: String, userId: String) {
        self.eventId = eventId
        self.userId = userId
        
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
                self.updateSelectedEvent()
            }
        }
        
        // Observe side effects from the state machine
        self.stateMachineWrapper.onSideEffect = { [weak self] effect in
            guard let self = self, let effect = effect else { return }
            
            DispatchQueue.main.async {
                self.handleSideEffect(effect)
            }
        }
        
        // Load participants and poll results
        loadParticipants()
        loadPollResults()
    }
    
    // MARK: - Public Methods
    
    /// Dispatch an intent to the state machine.
    func dispatch(_ intent: EventManagementContractIntent) {
        stateMachineWrapper.dispatch(intent: intent)
    }
    
    /// Load participants for the event
    func loadParticipants() {
        dispatch(EventManagementContractIntentLoadParticipants(eventId: eventId))
    }
    
    /// Load poll results for the event
    func loadPollResults() {
        dispatch(EventManagementContractIntentLoadPollResults(eventId: eventId))
    }
    
    /// Delete the event (requires organizer permission)
    func deleteEvent() {
        dispatch(EventManagementContractIntentDeleteEvent(eventId: eventId, userId: userId))
    }
    
    /// Update the event
    func updateEvent(_ event: Event) {
        dispatch(EventManagementContractIntentUpdateEvent(event: event))
    }
    
    // MARK: - Private Methods
    
    /// Update the selected event by filtering from the state's events list.
    private func updateSelectedEvent() {
        selectedEvent = state.events.first { $0.id == eventId }
    }
    
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
