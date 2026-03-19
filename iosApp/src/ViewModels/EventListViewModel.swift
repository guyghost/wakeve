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
class EventListViewModel: StateMachineViewModel<
    EventManagementContract.State,
    EventManagementContractIntent,
    EventManagementContractSideEffect
> {

    // MARK: - Initialization

    init() {
        let database = RepositoryProvider.shared.database
        let wrapper = IosFactory.shared.createEventStateMachine(database: database)
        super.init(stateMachineWrapper: wrapper)

        // Load events on initialization
        loadEvents()
    }

    // MARK: - Public Methods

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
