import Foundation
import SwiftUI
import Shared

/// Adds the first possible dates to a DRAFT created with "date à décider avec le groupe",
/// through the shared `UpdateEvent` intent (persisted by `saveEvent`, which syncs slots).
@MainActor
final class EventDraftDatesController: ObservableObject {
    enum Outcome: Equatable {
        case saved
        case failed(String)
    }

    @Published private(set) var isSaving = false

    private let eventId: String
    private let repository: EventRepositoryInterface
    private let stateMachine: ObservableStateMachine<
        EventManagementContract.State,
        EventManagementContractIntent,
        EventManagementContractSideEffect
    >
    private var expectedSlotCount = 0
    private var completion: ((Outcome) -> Void)?

    init(eventId: String, repository: EventRepositoryInterface) {
        self.eventId = eventId
        self.repository = repository
        self.stateMachine = IosFactory.shared.createEventStateMachine(
            database: RepositoryProvider.shared.database,
            eventRepository: repository
        )
        // UpdateEvent settles with a toast on success and on failure; state updates are
        // conflated and cannot be relied on to observe a repeated identical failure.
        stateMachine.onSideEffect = { [weak self] effect in
            guard effect is EventManagementContractSideEffectShowToast else { return }
            DispatchQueue.main.async {
                self?.settle()
            }
        }
    }

    deinit {
        stateMachine.dispose()
    }

    func save(slots: [EventTimeSlotInput], completion: @escaping (Outcome) -> Void) {
        guard !isSaving else { return }
        guard let draft = repository.getEvent(id: eventId), draft.status == .draft else {
            completion(.failed(String(localized: "draft_dates.error.save_failed")))
            return
        }
        let proposedSlots = EventTimeSlotFactory.proposedSlots(from: slots)
        guard !proposedSlots.isEmpty else {
            completion(.failed(String(localized: "participants.start_poll.requires_slot")))
            return
        }

        isSaving = true
        expectedSlotCount = proposedSlots.count
        self.completion = completion
        stateMachine.dispatch(
            intent: EventManagementContractIntentUpdateEvent(
                event: Self.draft(draft, withSlots: proposedSlots)
            )
        )
    }

    /// Kotlin data-class `copy` is not exposed with default arguments to Swift.
    private static func draft(_ draft: WakeveEvent, withSlots slots: [TimeSlot]) -> WakeveEvent {
        WakeveEvent(
            id: draft.id,
            title: draft.title,
            description: draft.description_,
            organizerId: draft.organizerId,
            participants: draft.participants,
            proposedSlots: slots,
            deadline: draft.deadline,
            status: draft.status,
            finalDate: draft.finalDate,
            createdAt: draft.createdAt,
            updatedAt: draft.updatedAt,
            eventType: draft.eventType,
            eventTypeCustom: draft.eventTypeCustom,
            minParticipants: draft.minParticipants,
            maxParticipants: draft.maxParticipants,
            expectedParticipants: draft.expectedParticipants,
            heroImageUrl: draft.heroImageUrl,
            planningMode: draft.planningMode,
            aggregateRevision: draft.aggregateRevision,
            aggregateSchemaVersion: draft.aggregateSchemaVersion
        )
    }

    private func settle() {
        guard isSaving else { return }
        isSaving = false
        let saved = repository.getEvent(id: eventId)?.proposedSlots.count == expectedSlotCount
        let callback = completion
        completion = nil
        callback?(saved ? .saved : .failed(String(localized: "draft_dates.error.save_failed")))
    }
}
