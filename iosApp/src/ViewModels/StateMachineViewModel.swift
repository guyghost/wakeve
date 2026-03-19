import SwiftUI
import Shared

/// Generic base class that eliminates boilerplate for ViewModels wrapping Kotlin state machines.
///
/// Subclasses only need to:
/// 1. Call `super.init(stateMachineWrapper:)` with the correct factory call
/// 2. Override `onStateDidChange()` if they need to derive local state
/// 3. Override `mapSideEffect(_:)` to bridge Kotlin sealed types
/// 4. Override `handleAdditionalSideEffect(_:)` for domain-specific side effects
@MainActor
class StateMachineViewModel<State: AnyObject, Intent: AnyObject, SideEffect: AnyObject>: ObservableObject {

    enum MappedSideEffect {
        case toast(String)
        case navigate(String)
        case back
        case unhandled(SideEffect)
    }

    @Published var state: State
    @Published var toastMessage: String?
    @Published var navigationRoute: String?
    @Published var shouldNavigateBack = false

    private let stateMachineWrapper: ObservableStateMachine<State, Intent, SideEffect>

    init(stateMachineWrapper: ObservableStateMachine<State, Intent, SideEffect>) {
        self.stateMachineWrapper = stateMachineWrapper
        self.state = stateMachineWrapper.currentState!

        self.stateMachineWrapper.onStateChange = { [weak self] newState in
            guard let self = self, let newState = newState else { return }
            DispatchQueue.main.async {
                self.state = newState
                self.onStateDidChange()
            }
        }

        self.stateMachineWrapper.onSideEffect = { [weak self] effect in
            guard let self = self, let effect = effect else { return }
            DispatchQueue.main.async {
                self.handleSideEffect(effect)
            }
        }
    }

    func dispatch(_ intent: Intent) {
        stateMachineWrapper.dispatch(intent: intent)
    }

    // MARK: - Hooks for subclasses

    func onStateDidChange() { }

    func mapSideEffect(_ effect: SideEffect) -> MappedSideEffect {
        return .unhandled(effect)
    }

    func handleAdditionalSideEffect(_ effect: SideEffect) { }

    // MARK: - Private

    private func handleSideEffect(_ effect: SideEffect) {
        let mapped = mapSideEffect(effect)
        switch mapped {
        case .toast(let message): toastMessage = message
        case .navigate(let route): navigationRoute = route
        case .back: shouldNavigateBack = true
        case .unhandled(let raw): handleAdditionalSideEffect(raw)
        }
    }

    deinit {
        stateMachineWrapper.dispose()
    }
}
