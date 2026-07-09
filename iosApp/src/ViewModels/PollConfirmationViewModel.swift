import Foundation
import SwiftUI
import Shared

/// SwiftUI adapter for the durable shared poll-confirmation workflow.
///
/// The adapter owns presentation concerns only: native prompts, local render
/// state, accessibility feedback, and a receipt-gated navigation callback.
/// The shared state machine owns every business-state transition.
@MainActor
final class PollConfirmationViewModel: ObservableObject {
    enum RenderState: Equatable {
        case reviewingResults
        case confirmPrompt
        case confirming
        case failed
        case pendingSync
        case synced
        case legacyApplied
        case quarantined
    }

    private enum Action {
        case presentConfirmation(slotId: String)
        case cancelConfirmation
        case submitConfirmation
        case retryConfirmation
        case confirmationFailed(message: String)
        case confirmationCommitted(receiptId: String)
    }

    @Published private(set) var state: RenderState = .reviewingResults
    @Published private(set) var slotScores: [PollLogic.SlotScore] = []
    @Published private(set) var bestSlot: TimeSlot?
    @Published private(set) var canConfirmDate = false
    @Published private(set) var failureMessage: String?

    /// Disables the confirm action while the single durable command is active.
    var isConfirmActionDisabled: Bool {
        operationInFlight || state == .confirming || state == .legacyApplied || state == .quarantined
    }

    var selectedSlotId: String?
    private var operationId: String?
    private var confirmationReceiptId: String?
    private var operationInFlight = false
    private var navigatedReceiptId: String?
    /// A receipt restored from disk may render its state, but must not replay
    /// the one-shot completion haptic or navigation callback.
    private var coldStartReceiptId: String?

    private let event: Event
    private let actorId: String
    private let repository: EventRepositoryInterface
    private let onDateConfirmed: (String) -> Void
    private let stateMachine: ObservableStateMachine<
        EventManagementContract.State,
        EventManagementContractIntent,
        EventManagementContractSideEffect
    >

    init(
        event: Event,
        actorId: String,
        onDateConfirmed: @escaping (String) -> Void
    ) {
        self.event = event
        self.actorId = actorId
        self.onDateConfirmed = onDateConfirmed

        let composition = RepositoryProvider.shared
        self.repository = composition.repository
        self.stateMachine = IosFactory.shared.createEventStateMachine(
            database: composition.database,
            eventRepository: composition.repository
        )

        stateMachine.onStateChange = { [weak self] sharedState in
            guard let sharedState else { return }
            DispatchQueue.main.async {
                self?.apply(sharedState: sharedState)
            }
        }
        stateMachine.onSideEffect = { [weak self] sideEffect in
            guard let sideEffect else { return }
            DispatchQueue.main.async {
                self?.handle(sideEffect: sideEffect)
            }
        }

        loadPollResults()
        if let initialState = stateMachine.currentState {
            apply(sharedState: initialState)
        }
        rehydrateConfirmation()
    }

    deinit {
        stateMachine.dispose()
    }

    func requestConfirmation(for slotId: String) {
        send(.presentConfirmation(slotId: slotId))
    }

    func cancelConfirmation() {
        guard state == .confirmPrompt else { return }
        send(.cancelConfirmation)
    }

    func submitConfirmation() {
        send(.submitConfirmation)
    }

    func retryConfirmation() {
        send(.retryConfirmation)
    }

    /// Called by the delivery authority once the server acknowledges this receipt.
    func markSyncCompleted(receiptId: String) {
        stateMachine.dispatch(
            intent: EventManagementContractIntentSyncCompleted(receiptId: receiptId)
        )
    }

    private func send(_ action: Action) {
        switch action {
        case .presentConfirmation(let slotId):
            guard state == .reviewingResults, !slotId.isEmpty else { return }
            selectedSlotId = slotId
            operationId = UUID().uuidString
            failureMessage = nil
            state = .confirmPrompt
            stateMachine.dispatch(
                intent: EventManagementContractIntentOpenConfirmPrompt(
                    eventId: event.id,
                    slotId: slotId,
                    actorId: actorId
                )
            )

        case .cancelConfirmation:
            state = .reviewingResults
            failureMessage = nil
            selectedSlotId = nil
            operationId = nil
            operationInFlight = false
            stateMachine.dispatch(intent: EventManagementContractIntentCancelConfirmation.shared)

        case .submitConfirmation:
            guard !operationInFlight,
                  let operationId,
                  let selectedSlotId,
                  !selectedSlotId.isEmpty else { return }
            operationInFlight = true
            state = .confirming
            stateMachine.dispatch(
                intent: EventManagementContractIntentSubmitConfirmation(operationId: operationId)
            )

        case .retryConfirmation:
            guard let operationId,
                  let selectedSlotId,
                  !selectedSlotId.isEmpty else { return }
            self.operationId = operationId
            operationInFlight = true
            failureMessage = nil
            state = .confirming
            stateMachine.dispatch(intent: EventManagementContractIntentRetryConfirmation.shared)

        case .confirmationFailed(let message):
            operationInFlight = false
            failureMessage = message
            state = .failed

        case .confirmationCommitted(let receiptId):
            guard confirmationReceiptId == receiptId,
                  navigatedReceiptId != receiptId else { return }
            navigatedReceiptId = receiptId
            operationInFlight = false
            WakeveHaptics.success()
            onDateConfirmed(event.id)
        }
    }

    private func apply(sharedState: EventManagementContract.State) {
        let phase = sharedState.confirmationPhase

        if phase == EventManagementContract.ConfirmationPhase.reviewingResults {
            guard state != .pendingSync && state != .synced else { return }
            operationInFlight = false
            state = .reviewingResults

        } else if phase == EventManagementContract.ConfirmationPhase.confirmPrompt {
            state = .confirmPrompt

        } else if phase == EventManagementContract.ConfirmationPhase.confirming {
            operationInFlight = true
            state = .confirming

        } else if phase == EventManagementContract.ConfirmationPhase.failed {
            let message = sharedState.error ?? String(localized: "poll.results.error.confirm_failed")
            send(.confirmationFailed(message: message))

        } else if phase == EventManagementContract.ConfirmationPhase.legacyApplied {
            operationInFlight = false
            failureMessage = nil
            state = .legacyApplied

        } else if phase == EventManagementContract.ConfirmationPhase.quarantined {
            operationInFlight = false
            failureMessage = nil
            state = .quarantined

        } else if phase == EventManagementContract.ConfirmationPhase.confirmedPendingSync ||
                    phase == EventManagementContract.ConfirmationPhase.confirmedSynced {
            guard let receiptId = sharedState.confirmationReceiptId else {
                send(.confirmationFailed(message: String(localized: "poll.results.error.confirm_failed")))
                return
            }

            let isColdStartRehydration = coldStartReceiptId == receiptId
            if isColdStartRehydration {
                coldStartReceiptId = nil
            }
            let hasUnprocessedReceipt = navigatedReceiptId != receiptId
            confirmationReceiptId = receiptId
            operationInFlight = false
            state = phase == .confirmedPendingSync ? .pendingSync : .synced
            if !isColdStartRehydration && hasUnprocessedReceipt {
                send(.confirmationCommitted(receiptId: receiptId))
            }
        }
    }

    private func handle(sideEffect: EventManagementContractSideEffect) {
        // Navigation is deliberately ignored here.  The state observer invokes
        // the callback exactly once, and only after it receives a durable receipt.
        if let toast = sideEffect as? EventManagementContractSideEffectShowToast,
           state == .failed {
            failureMessage = toast.message
        }
    }

    private func loadPollResults() {
        guard let poll = repository.getPoll(eventId: event.id) else { return }

        slotScores = PollLogic.shared.getSlotScores(poll: poll, slots: event.proposedSlots)
        bestSlot = PollLogic.shared.getBestSlotWithScore(poll: poll, slots: event.proposedSlots)?.first
        canConfirmDate = repository.isOrganizer(eventId: event.id, userId: actorId)
    }

    private func rehydrateConfirmation() {
        let projection = repository.loadConfirmationProjection(eventId: event.id)
        if let confirmed = projection as? EventManagementContractConfirmationProjectionConfirmed {
            coldStartReceiptId = confirmed.receiptId
            navigatedReceiptId = confirmed.receiptId
        }
        stateMachine.dispatch(
            intent: EventManagementContractIntentRehydrateConfirmation(projection: projection)
        )
    }
}
