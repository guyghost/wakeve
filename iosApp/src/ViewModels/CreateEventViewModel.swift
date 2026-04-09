import SwiftUI
import Shared

/// ViewModel for creating a new event.
///
/// Connects the CreateEventSheet form to the EventManagementStateMachine,
/// ensuring events are persisted to the local SQLDelight database.
///
/// ## Lifecycle
/// 1. User fills the form → form state lives in CreateEventSheet @State
/// 2. User taps "Créer" → calls `createEvent(...)` which dispatches CreateEvent intent
/// 3. StateMachine saves to DB → emits NavigateTo side effect
/// 4. ViewModel bridges side effect → triggers `onEventCreated` callback
@MainActor
class CreateEventViewModel: StateMachineViewModel<
    EventManagementContract.State,
    EventManagementContractIntent,
    EventManagementContractSideEffect
> {

    // MARK: - Callbacks

    var onEventCreated: ((WakeveEvent) -> Void)?
    var onDismiss: (() -> Void)?

    // MARK: - Initialization

    init() {
        let database = RepositoryProvider.shared.database
        let wrapper = IosFactory.shared.createEventStateMachine(database: database)
        super.init(stateMachineWrapper: wrapper)
    }

    // MARK: - Public API

    /// Create a new event and persist it via the StateMachine.
    ///
    /// - Parameters:
    ///   - title: Event title (required, non-empty)
    ///   - description: Event description
    ///   - userId: ID of the organizer (current user)
    ///   - eventType: Type of event (default: OTHER)
    ///   - eventTypeCustom: Custom type description (required if eventType == CUSTOM)
    ///   - selectedDate: ISO 8601 date string for the deadline (optional)
    ///   - minParticipants: Minimum expected participants (optional)
    ///   - maxParticipants: Maximum expected participants (optional)
    ///   - expectedParticipants: Expected participant count (optional)
    func createEvent(
        title: String,
        description: String,
        userId: String,
        eventType: Shared.EventType = Shared.EventType.other,
        eventTypeCustom: String? = nil,
        selectedDate: String? = nil,
        minParticipants: Int32? = nil,
        maxParticipants: Int32? = nil,
        expectedParticipants: Int32? = nil
    ) {
        guard !title.trimmingCharacters(in: .whitespaces).isEmpty else { return }

        let iso8601 = ISO8601DateFormatter()
        let now = iso8601.string(from: Date())
        let deadline = selectedDate ?? iso8601.string(from: Calendar.current.date(byAdding: .day, value: 7, to: Date())!)

        let event = WakeveEvent(
            id: "event-\(Int(Date().timeIntervalSince1970 * 1000))",
            title: title.trimmingCharacters(in: .whitespaces),
            description: description.trimmingCharacters(in: .whitespaces),
            organizerId: userId,
            participants: [],
            proposedSlots: [],
            deadline: deadline,
            status: .draft,
            finalDate: nil,
            createdAt: now,
            updatedAt: now,
            eventType: eventType,
            eventTypeCustom: eventTypeCustom,
            minParticipants: minParticipants.map { KotlinInt(value: $0) },
            maxParticipants: maxParticipants.map { KotlinInt(value: $0) },
            expectedParticipants: expectedParticipants.map { KotlinInt(value: $0) },
            heroImageUrl: nil
        )

        // Dispatch to StateMachine — persists to SQLDelight DB
        dispatch(EventManagementContractIntentCreateEvent(event: event))

        // Also notify parent directly so UI can close immediately
        onEventCreated?(event)
    }

    // MARK: - Side Effect Mapping

    override func mapSideEffect(_ effect: EventManagementContractSideEffect) -> MappedSideEffect {
        switch effect {
        case let showToast as EventManagementContractSideEffectShowToast:
            return .toast(showToast.message)
        case let navigateTo as EventManagementContractSideEffectNavigateTo:
            return .navigate(navigateTo.route)
        default:
            return .unhandled(effect)
        }
    }
}
