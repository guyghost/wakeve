import Foundation
import Shared
import Combine

/// ViewModel for the ScenarioDetailView.
///
/// Manages the state and intents for displaying detailed information about a single scenario.
@MainActor
class ScenarioDetailViewModel: ObservableObject {
    // MARK: - Published Properties

    @Published var state: ScenarioManagementContract.State
    @Published var scenario: Scenario_?
    @Published var scenarioWithVotes: ScenarioWithVotes?
    @Published var toastMessage: String?
    @Published var navigationRoute: String?
    @Published var shouldNavigateBack = false
    @Published var isEditing = false

    // MARK: - Private Properties

    private let scenarioId: String
    private let stateMachineWrapper: ObservableStateMachine<
        ScenarioManagementContract.State,
        ScenarioManagementContractIntent,
        ScenarioManagementContractSideEffect
    >

    // MARK: - Initialization

    init(scenarioId: String) {
        self.scenarioId = scenarioId
        let database = RepositoryProvider.shared.database
        self.stateMachineWrapper = IosFactory.shared.createScenarioStateMachine(database: database)
        self.state = self.stateMachineWrapper.currentState!

        self.stateMachineWrapper.onStateChange = { [weak self] newState in
            guard let self = self, let newState = newState else { return }
            DispatchQueue.main.async {
                self.state = newState
                self.updateSelectedScenario()
            }
        }

        self.stateMachineWrapper.onSideEffect = { [weak self] effect in
            guard let self = self, let effect = effect else { return }
            DispatchQueue.main.async {
                self.handleSideEffect(effect)
            }
        }

        loadScenarios()
    }

    // MARK: - Public Methods

    func dispatch(_ intent: ScenarioManagementContractIntent) {
        stateMachineWrapper.dispatch(intent: intent)
    }

    func loadScenarios() {
        dispatch(ScenarioManagementContractIntentLoadScenarios.shared)
    }

    func reload() {
        loadScenarios()
    }

    func updateScenario(
        name: String,
        dateOrPeriod: String,
        location: String,
        duration: Int32,
        estimatedParticipants: Int32,
        estimatedBudgetPerPerson: Double,
        description: String
    ) {
        let updatedScenario = Scenario_(
            id: scenarioId,
            eventId: state.eventId,
            name: name,
            dateOrPeriod: dateOrPeriod,
            location: location,
            duration: duration,
            estimatedParticipants: estimatedParticipants,
            estimatedBudgetPerPerson: estimatedBudgetPerPerson,
            description: description,
            status: scenario?.status ?? ScenarioStatus.proposed,
            createdAt: scenario?.createdAt ?? "",
            updatedAt: ""
        )
        dispatch(ScenarioManagementContractIntentUpdateScenario(scenario: updatedScenario))
        isEditing = false
    }

    func deleteScenario() {
        dispatch(ScenarioManagementContractIntentDeleteScenario(scenarioId: scenarioId))
    }

    func voteScenario(voteType: ScenarioVoteType) {
        dispatch(ScenarioManagementContractIntentVoteScenario(scenarioId: scenarioId, vote: voteType))
    }

    func startEditing() { isEditing = true }
    func cancelEditing() { isEditing = false }
    func clearError() { dispatch(ScenarioManagementContractIntentClearError.shared) }

    func shareScenario() {
        if let scenario = scenario {
            let shareText = prepareShareText(scenario: scenario)
            print("Share scenario: \(shareText)")
        }
    }

    // MARK: - Convenience Properties

    var votingResult: ScenarioVotingResult? { state.votingResults[scenarioId] }
    var isLoaded: Bool { scenario != nil }
    var isEmpty: Bool { scenario == nil }
    var isLoading: Bool { state.isLoading }
    var hasError: Bool { state.hasError }
    var errorMessage: String? { state.error }

    // MARK: - Private Methods

    private func updateSelectedScenario() {
        scenario = state.scenarios.first { $0.scenario.id == scenarioId }?.scenario
        scenarioWithVotes = state.scenarios.first { $0.scenario.id == scenarioId }
    }

    private func handleSideEffect(_ effect: ScenarioManagementContractSideEffect) {
        switch effect {
        case let showToast as ScenarioManagementContractSideEffectShowToast:
            toastMessage = showToast.message
        case let navigateTo as ScenarioManagementContractSideEffectNavigateTo:
            navigationRoute = navigateTo.route
        case is ScenarioManagementContractSideEffectNavigateBack:
            shouldNavigateBack = true
        case let showError as ScenarioManagementContractSideEffectShowError:
            toastMessage = showError.message
        case let shareScenario as ScenarioManagementContractSideEffectShareScenario:
            shareScenarioInternal(scenario: shareScenario.scenario)
        default:
            break
        }
    }

    private func shareScenarioInternal(scenario: Scenario_) {
        let shareText = prepareShareText(scenario: scenario)
        print("Share scenario: \(shareText)")
    }

    private func prepareShareText(scenario: Scenario_) -> String {
        var text = "📅 \(scenario.name)\n"
        text += "📍 \(scenario.location)\n"
        text += "📆 \(scenario.dateOrPeriod)\n"
        text += "⏱️ Duration: \(scenario.duration) days\n"
        text += "👥 Estimated participants: \(scenario.estimatedParticipants)\n"
        text += "💰 Budget per person: €\(String(format: "%.2f", scenario.estimatedBudgetPerPerson))\n"
        if !scenario.description_.isEmpty {
            text += "\nDescription:\n\(scenario.description_)"
        }
        if let votingResult = votingResult {
            text += "\n\nVoting Results:\n"
            text += "👍 Prefer: \(votingResult.preferCount)\n"
            text += "😐 Neutral: \(votingResult.neutralCount)\n"
            text += "👎 Against: \(votingResult.againstCount)\n"
            text += "Total Score: \(votingResult.score)"
        }
        return text
    }

    // MARK: - Deinit

    deinit {
        stateMachineWrapper.dispose()
    }
}
