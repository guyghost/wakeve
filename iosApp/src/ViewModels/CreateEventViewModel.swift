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
    private var pendingCreationEvent: WakeveEvent?
    @Published private(set) var isCreating = false
    /// Localized reason of the last failed creation, surfaced by the wizard.
    @Published private(set) var creationErrorMessage: String?
    var isCreationInFlight: Bool { isCreating }

    // MARK: - WakeveAI

    @Published var smartEventDraftState = SmartEventDraftState()

    private let smartEventDraftGenerator: EventDraftGenerator
    private var smartEventDraftTask: Task<Void, Never>?

    // MARK: - Initialization

    init(smartEventDraftGenerator: EventDraftGenerator = EventDraftGenerator()) {
        self.smartEventDraftGenerator = smartEventDraftGenerator
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
    ///   - selectedDate: ISO 8601 date string used as a backwards-compatible single proposed slot (optional)
    ///   - selectedSlots: ISO 8601 slot inputs staged by the Create Event wizard
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
        selectedSlots: [EventTimeSlotInput] = [],
        minParticipants: Int32? = nil,
        maxParticipants: Int32? = nil,
        expectedParticipants: Int32? = nil,
        planningMode: EventPlanningMode = .timeSlotPoll
    ) {
        guard !isCreating else { return }
        guard !title.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        creationErrorMessage = nil

        // An empty slot list is a valid DRAFT ("date à décider avec le groupe"):
        // the organizer adds dates before starting the poll.
        let proposedSlots = selectedSlots.isEmpty
            ? EventTimeSlotFactory.proposedSlots(from: selectedDate)
            : EventTimeSlotFactory.proposedSlots(from: selectedSlots)
        isCreating = true

        let iso8601 = ISO8601DateFormatter()
        let now = iso8601.string(from: Date())
        let deadline = iso8601.string(from: Calendar.current.date(byAdding: .day, value: 7, to: Date())!)

        let event = WakeveEvent(
            id: "event-\(Int(Date().timeIntervalSince1970 * 1000))",
            title: title.trimmingCharacters(in: .whitespaces),
            description: description.trimmingCharacters(in: .whitespaces),
            organizerId: userId,
            participants: [],
            proposedSlots: proposedSlots,
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
            heroImageUrl: nil,
            planningMode: planningMode,
            aggregateRevision: 1,
            aggregateSchemaVersion: 1
        )

        // Dispatch to the single persistence owner. Completion is derived from
        // the refreshed state, never from the fact that dispatch returned.
        pendingCreationEvent = event
        dispatch(EventManagementContractIntentCreateEvent(event: event))
    }

    override func onStateDidChange() {
        guard let pending = pendingCreationEvent, !state.isLoading else { return }
        guard state.error == nil else {
            failPendingCreation()
            return
        }
        guard let persisted = state.events.first(where: { $0.id == pending.id }) else { return }
        completePendingCreation(with: persisted)
    }

    /// The state machine emits a toast once a creation settles, on success and on failure.
    /// State updates are conflated: a failure identical to the previous one produces no
    /// new state, so the toast is the reliable settlement signal for a pending creation.
    private func settlePendingCreationAfterToast() {
        guard let pending = pendingCreationEvent else { return }
        if let persisted = RepositoryProvider.shared.repository.getEvent(id: pending.id) {
            completePendingCreation(with: persisted)
        } else {
            failPendingCreation()
        }
    }

    private func completePendingCreation(with persisted: WakeveEvent) {
        pendingCreationEvent = nil
        isCreating = false
        let completion = onEventCreated
        onEventCreated = nil
        completion?(persisted)
    }

    private func failPendingCreation() {
        pendingCreationEvent = nil
        isCreating = false
        creationErrorMessage = String(localized: "create_event.error.failed")
    }

    func updateSmartEventDraftPhrase(_ phrase: String) {
        smartEventDraftState.phrase = phrase
        if case .unavailable = smartEventDraftState.phase {
            smartEventDraftState.phase = .idle
        }
    }

    func generateSmartEventDraft() {
        smartEventDraftTask?.cancel()

        let phrase = smartEventDraftState.phrase.trimmingCharacters(in: .whitespacesAndNewlines)
        guard phrase.count >= 6 else { return }

        smartEventDraftState.phase = .preparing
        smartEventDraftState.streamedTitle = ""
        smartEventDraftState.streamedDescription = ""
        smartEventDraftState.streamedDateOptions = []
        smartEventDraftState.streamedChecklist = []
        smartEventDraftState.streamedPolls = []
        smartEventDraftState.metadata = nil

        smartEventDraftTask = Task { [weak self] in
            guard let self else { return }

            do {
                let request = WakeveAIGenerationRequest(userInput: phrase)
                for try await section in smartEventDraftGenerator.generate(request: request) {
                    guard !Task.isCancelled else { throw WakeveAIError.cancelled }

                    await MainActor.run {
                        switch section {
                        case .title(let title):
                            self.smartEventDraftState.phase = .streaming
                            self.smartEventDraftState.streamedTitle = title
                        case .description(let description):
                            self.smartEventDraftState.phase = .streaming
                            self.smartEventDraftState.streamedDescription = description
                        case .dateOptions(let options):
                            self.smartEventDraftState.phase = .streaming
                            self.smartEventDraftState.streamedDateOptions = options
                        case .checklist(let checklist):
                            self.smartEventDraftState.phase = .streaming
                            self.smartEventDraftState.streamedChecklist = checklist
                        case .suggestedPolls(let polls):
                            self.smartEventDraftState.phase = .streaming
                            self.smartEventDraftState.streamedPolls = polls
                        case .completed(let draft):
                            self.smartEventDraftState.phase = .ready(draft)
                            let metrics = WakeveAIMetricsRecorder.shared.snapshot().last
                            self.smartEventDraftState.metrics = metrics
                            self.smartEventDraftState.metadata = WakeveAIInteractionMetadata.fromMetrics(
                                useCase: .eventPlanDraft,
                                promptId: metrics?.promptId ?? "event_draft_v1",
                                availability: metrics?.availability ?? .available,
                                sanitizedInputSummary: "Smart event draft phrase",
                                sanitizedOutputSummary: "Structured event draft",
                                reasoningSummary: "Generated typed event draft for explicit user review.",
                                validation: .needsReview(),
                                metrics: metrics
                            )
                        case .failed(let message):
                            self.smartEventDraftState.phase = .failed(message)
                        }
                    }
                }
            } catch WakeveAIError.unavailable(let availability) {
                await MainActor.run {
                    self.smartEventDraftState.phase = .unavailable(availability)
                }
            } catch WakeveAIError.cancelled {
                await MainActor.run {
                    self.smartEventDraftState.phase = .cancelled
                }
            } catch is CancellationError {
                await MainActor.run {
                    self.smartEventDraftState.phase = .cancelled
                }
            } catch WakeveAIError.timedOut {
                await MainActor.run {
                    self.smartEventDraftState.phase = .failed("La suggestion prend trop de temps. Continuez manuellement.")
                }
            } catch {
                await MainActor.run {
                    self.smartEventDraftState.phase = .failed("Suggestion indisponible. Continuez manuellement.")
                }
            }
        }
    }

    func cancelSmartEventDraft() {
        smartEventDraftTask?.cancel()
        smartEventDraftTask = nil
        smartEventDraftState.phase = .cancelled
    }

    func ignoreSmartEventDraft() {
        smartEventDraftTask?.cancel()
        smartEventDraftTask = nil
        smartEventDraftState = SmartEventDraftState()
    }

    // MARK: - Side Effect Mapping

    override func mapSideEffect(_ effect: EventManagementContractSideEffect) -> MappedSideEffect {
        switch effect {
        case let showToast as EventManagementContractSideEffectShowToast:
            settlePendingCreationAfterToast()
            return .toast(showToast.message)
        case let navigateTo as EventManagementContractSideEffectNavigateTo:
            return .navigate(navigateTo.route)
        default:
            return .unhandled(effect)
        }
    }
}

struct EventTimeSlotInput: Equatable {
    let start: String
    let end: String?
    let timeOfDay: Shared.TimeOfDay

    init(
        start: String,
        end: String? = nil,
        timeOfDay: Shared.TimeOfDay = .specific
    ) {
        self.start = start
        self.end = end
        self.timeOfDay = timeOfDay
    }
}

/// Builds the slot input saved by the creation wizard from its date/time pickers.
///
/// An all-day slot spans the whole local day (start of day -> start of next day),
/// exactly like `DraftDatesSheet`; it never keeps the time at which it was created.
enum EventSlotInputBuilder {
    /// Default start time of a new specific-time slot (an evening meetup), instead of "now".
    static let defaultStartHour = 19

    static func defaultStartTime(on day: Date, calendar: Calendar = .current) -> Date {
        calendar.date(bySettingHour: defaultStartHour, minute: 0, second: 0, of: day) ?? day
    }

    static func input(
        startDate: Date,
        startTime: Date,
        isAllDay: Bool,
        hasEndTime: Bool,
        endTime: Date,
        formatter: ISO8601DateFormatter,
        calendar: Calendar = .current
    ) -> EventTimeSlotInput {
        let start = startDateTime(startDate: startDate, startTime: startTime, isAllDay: isAllDay, calendar: calendar)
        let end = endDateTime(
            startDate: startDate,
            start: start,
            isAllDay: isAllDay,
            hasEndTime: hasEndTime,
            endTime: endTime,
            calendar: calendar
        )
        return EventTimeSlotInput(
            start: formatter.string(from: start),
            end: formatter.string(from: end),
            timeOfDay: isAllDay ? .allDay : .specific
        )
    }

    static func startDateTime(startDate: Date, startTime: Date, isAllDay: Bool, calendar: Calendar = .current) -> Date {
        if isAllDay {
            return calendar.startOfDay(for: startDate)
        }
        return combinedDateTime(date: startDate, time: startTime, calendar: calendar)
    }

    static func endDateTime(
        startDate: Date,
        start: Date,
        isAllDay: Bool,
        hasEndTime: Bool,
        endTime: Date,
        calendar: Calendar = .current
    ) -> Date {
        if isAllDay {
            return calendar.date(byAdding: .day, value: 1, to: start) ?? start.addingTimeInterval(24 * 3600)
        }

        if hasEndTime {
            let combinedEnd = combinedDateTime(date: startDate, time: endTime, calendar: calendar)
            if combinedEnd <= start {
                return calendar.date(byAdding: .day, value: 1, to: combinedEnd) ?? combinedEnd
            }
            return combinedEnd
        }

        return calendar.date(byAdding: .hour, value: 1, to: start) ?? start.addingTimeInterval(3600)
    }

    private static func combinedDateTime(date: Date, time: Date, calendar: Calendar) -> Date {
        let dateComponents = calendar.dateComponents([.year, .month, .day], from: date)
        let timeComponents = calendar.dateComponents([.hour, .minute], from: time)

        var components = DateComponents()
        components.year = dateComponents.year
        components.month = dateComponents.month
        components.day = dateComponents.day
        components.hour = timeComponents.hour
        components.minute = timeComponents.minute

        return calendar.date(from: components) ?? date
    }
}

enum EventTimeSlotFactory {
    static func proposedSlots(from selectedDate: String?) -> [TimeSlot] {
        guard let selectedDate, !selectedDate.isEmpty else {
            return []
        }

        return proposedSlots(from: [
            EventTimeSlotInput(start: selectedDate)
        ])
    }

    static func proposedSlots(from selectedSlotStarts: [String]) -> [TimeSlot] {
        proposedSlots(from: selectedSlotStarts.map { EventTimeSlotInput(start: $0) })
    }

    static func proposedSlots(from selectedSlots: [EventTimeSlotInput]) -> [TimeSlot] {
        selectedSlots
            .filter { !$0.start.isEmpty }
            .map { slot in
                TimeSlot(
                    id: "slot-\(UUID().uuidString.prefix(8))",
                    start: slot.start,
                    end: slot.end ?? defaultEndDate(for: slot.start),
                    timezone: TimeZone.current.identifier,
                    timeOfDay: slot.timeOfDay
                )
            }
    }

    private static func defaultEndDate(for start: String) -> String? {
        let formatter = ISO8601DateFormatter()
        guard let startDate = formatter.date(from: start),
              let endDate = Calendar.current.date(byAdding: .hour, value: 1, to: startDate) else {
            return nil
        }

        return formatter.string(from: endDate)
    }
}
