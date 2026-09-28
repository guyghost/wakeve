import Foundation
import SwiftUI
import Shared

/// iOS bridge for the organizer lifecycle transitions of the shared
/// EventManagementStateMachine: CONFIRMED -> ORGANIZING and ORGANIZING -> FINALIZED.
///
/// Mirrors `EventPollStartController`: the view stays a presentation shell and the
/// state machine remains the single owner of the status write.
@MainActor
final class EventLifecycleTransitionController: ObservableObject {
    enum Target {
        case organizing
        case finalized

        var status: EventStatus {
            switch self {
            case .organizing: return .organizing
            case .finalized: return .finalized
            }
        }
    }

    enum Outcome: Equatable {
        case transitioned
        case failed(String)
    }

    @Published private(set) var inFlightTarget: Target?

    private let eventId: String
    private let userId: String
    private let repository: EventRepositoryInterface
    private let stateMachine: ObservableStateMachine<
        EventManagementContract.State,
        EventManagementContractIntent,
        EventManagementContractSideEffect
    >
    private var completion: ((Outcome) -> Void)?

    init(eventId: String, userId: String, repository: EventRepositoryInterface) {
        self.eventId = eventId
        self.userId = userId
        self.repository = repository
        self.stateMachine = IosFactory.shared.createEventStateMachine(
            database: RepositoryProvider.shared.database,
            eventRepository: repository
        )

        // State updates are conflated, so an identical repeated failure would never be
        // observed. Every transition path settles with a toast: use it as the signal.
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

    func transition(to target: Target, completion: @escaping (Outcome) -> Void) {
        guard inFlightTarget == nil else { return }
        inFlightTarget = target
        self.completion = completion

        switch target {
        case .organizing:
            stateMachine.dispatch(
                intent: EventManagementContractIntentTransitionToOrganizing(eventId: eventId, userId: userId)
            )
        case .finalized:
            stateMachine.dispatch(
                intent: EventManagementContractIntentMarkAsFinalized(eventId: eventId, userId: userId)
            )
        }
    }

    private func settle() {
        guard let target = inFlightTarget else { return }
        let outcome: Outcome
        if repository.getEvent(id: eventId)?.status == target.status {
            outcome = .transitioned
        } else {
            outcome = .failed(EventLifecycleBlockerFormatter.message(for: stateMachine.currentState?.error))
        }
        inFlightTarget = nil
        let callback = completion
        completion = nil
        callback?(outcome)
    }
}

/// Turns shared lifecycle failures (e.g. "Finalization blocked by MEETING_REQUIRED,LODGING_REQUIRED")
/// into a sentence the organizer can act on. Raw blocker codes never reach the UI.
enum EventLifecycleBlockerFormatter {
    private static let blockedPrefix = "Finalization blocked by "

    static func message(for failure: String?) -> String {
        guard let failure, failure.hasPrefix(blockedPrefix) else {
            return String(localized: "event.lifecycle.error.generic")
        }

        let labels = failure
            .dropFirst(blockedPrefix.count)
            .split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
            .reduce(into: [String]()) { labels, code in
                if let label = blockerLabel(for: code), !labels.contains(label) {
                    labels.append(label)
                }
            }

        guard !labels.isEmpty else {
            return String(localized: "event.lifecycle.error.generic")
        }

        let list = ListFormatter.localizedString(byJoining: labels)
        return "\(String(localized: "event.lifecycle.blocked_intro")) \(list)."
    }

    private static func blockerLabel(for code: String) -> String? {
        let key = "event.lifecycle.blocker.\(code.lowercased())"
        let label = Bundle.main.localizedString(forKey: key, value: nil, table: nil)
        return label == key ? nil : label
    }
}
