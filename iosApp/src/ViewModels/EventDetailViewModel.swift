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
class EventDetailViewModel: StateMachineViewModel<
    EventManagementContract.State,
    EventManagementContractIntent,
    EventManagementContractSideEffect
> {

    // MARK: - Published Properties

    /// The currently selected event (filtered from state.events)
    @Published var selectedEvent: Event?

    // MARK: - Private Properties

    /// The event ID passed to this view model
    private let eventId: String

    /// The current user ID (for organizer checks and delete permission)
    private let userId: String

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

        let database = RepositoryProvider.shared.database
        let wrapper = IosFactory.shared.createEventStateMachine(database: database)
        super.init(stateMachineWrapper: wrapper)

        // Load participants and poll results
        loadParticipants()
        loadPollResults()
    }

    // MARK: - Public Methods

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

    // MARK: - State Change Hook

    override func onStateDidChange() {
        updateSelectedEvent()
    }

    // MARK: - Private Methods

    /// Update the selected event by filtering from the state's events list.
    private func updateSelectedEvent() {
        selectedEvent = state.events.first { $0.id == eventId }
    }

    // MARK: - Side Effect Mapping

    override func mapSideEffect(_ effect: EventManagementContractSideEffect) -> MappedSideEffect {
        switch effect {
        case let showToast as EventManagementContractSideEffectShowToast:
            return .toast(showToast.message)
        case let navigateTo as EventManagementContractSideEffectNavigateTo:
            return .navigate(navigateTo.route)
        case is EventManagementContractSideEffectNavigateBack:
            return .back
        default:
            return .unhandled(effect)
        }
    }
}
