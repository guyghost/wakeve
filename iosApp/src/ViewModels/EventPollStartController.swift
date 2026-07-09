import Foundation
import SwiftUI
import Shared

/// iOS bridge for the existing typed `StartPoll` workflow.
///
/// `ParticipantManagementView` remains a presentation shell; this controller
/// delegates the state transition to the shared EventManagementStateMachine.
@MainActor
final class EventPollStartController: ObservableObject {
    enum Outcome {
        case started
        case failed(String)
    }

    private let eventId: String
    private let userId: String
    private let repository: EventRepositoryInterface?
    private let stateMachine: ObservableStateMachine<
        EventManagementContract.State,
        EventManagementContractIntent,
        EventManagementContractSideEffect
    >?
    private var awaitingOutcome = false
    private var completion: ((Outcome) -> Void)?

    init(eventId: String, userId: String, repository: EventRepositoryInterface?) {
        self.eventId = eventId
        self.userId = userId
        self.repository = repository

        guard let repository else {
            self.stateMachine = nil
            return
        }

        let wrapper = IosFactory.shared.createEventStateMachine(
            database: RepositoryProvider.shared.database,
            eventRepository: repository
        )
        self.stateMachine = wrapper
        wrapper.onStateChange = { [weak self] state in
            guard let state else { return }
            DispatchQueue.main.async {
                self?.apply(state: state)
            }
        }

        wrapper.onSideEffect = { _ in }
    }

    deinit {
        stateMachine?.dispose()
    }

    func start(completion: @escaping (Outcome) -> Void) {
        guard let stateMachine else {
            completion(.failed(String(localized: "participants.error.start_poll_failed")))
            return
        }

        awaitingOutcome = true
        self.completion = completion
        stateMachine.dispatch(
            intent: EventManagementContractIntentStartPoll(eventId: eventId, userId: userId)
        )
    }

    private func apply(state: EventManagementContract.State) {
        guard awaitingOutcome, !state.isLoading else { return }

        if let error = state.error {
            finish(.failed(error))
        } else if repository?.getEvent(id: eventId)?.status == .polling {
            finish(.started)
        }
    }

    private func finish(_ outcome: Outcome) {
        guard awaitingOutcome else { return }
        awaitingOutcome = false
        let callback = completion
        completion = nil
        callback?(outcome)
    }
}
