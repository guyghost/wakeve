import Foundation
import Shared

/// ViewModel for the ScenarioListView.
///
/// Manages the state and intents for displaying a list of scenarios.
/// Uses the shared Kotlin state machine to handle all business logic.
@MainActor
class ScenarioListViewModel: ObservableObject {
    // MARK: - Published Properties

    @Published var state: ScenarioManagementContract.State
    @Published var toastMessage: String?
    @Published var navigationRoute: String?
    @Published var shouldNavigateBack = false

    // MARK: - Private Properties

    private let stateMachineWrapper: ObservableStateMachine<
        ScenarioManagementContract.State,
        ScenarioManagementContractIntent,
        ScenarioManagementContractSideEffect
    >

    // MARK: - Initialization

    init() {
        let database = RepositoryProvider.shared.database
        self.stateMachineWrapper = IosFactory.shared.createScenarioStateMachine(database: database)
        self.state = self.stateMachineWrapper.currentState!

        self.stateMachineWrapper.onStateChange = { [weak self] newState in
            guard let self = self, let newState = newState else { return }
            DispatchQueue.main.async {
                self.state = newState
            }
        }

        self.stateMachineWrapper.onSideEffect = { [weak self] effect in
            guard let self = self, let effect = effect else { return }
            DispatchQueue.main.async {
                self.handleSideEffect(effect)
            }
        }
    }

    // MARK: - Public Methods

    func dispatch(_ intent: ScenarioManagementContractIntent) {
        stateMachineWrapper.dispatch(intent: intent)
    }

    func initialize(eventId: String, participantId: String) {
        dispatch(ScenarioManagementContractIntentLoadScenariosForEvent(
            eventId: eventId,
            participantId: participantId
        ))
    }

    func loadScenarios() {
        dispatch(ScenarioManagementContractIntentLoadScenarios.shared)
    }

    func createScenario(
        name: String,
        eventId: String,
        dateOrPeriod: String,
        location: String,
        duration: Int32,
        estimatedParticipants: Int32,
        estimatedBudgetPerPerson: Double,
        description: String
    ) {
        let scenario = Scenario_(
            id: "",
            eventId: eventId,
            name: name,
            dateOrPeriod: dateOrPeriod,
            location: location,
            duration: duration,
            estimatedParticipants: estimatedParticipants,
            estimatedBudgetPerPerson: estimatedBudgetPerPerson,
            description: description,
            status: ScenarioStatus.proposed,
            createdAt: "",
            updatedAt: ""
        )
        dispatch(ScenarioManagementContractIntentCreateScenario(scenario: scenario))
    }

    func selectScenario(scenarioId: String) {
        dispatch(ScenarioManagementContractIntentSelectScenario(scenarioId: scenarioId))
    }

    func updateScenario(scenario: Scenario_) {
        dispatch(ScenarioManagementContractIntentUpdateScenario(scenario: scenario))
    }

    func deleteScenario(scenarioId: String) {
        dispatch(ScenarioManagementContractIntentDeleteScenario(scenarioId: scenarioId))
    }

    func voteScenario(scenarioId: String, voteType: ScenarioVoteType) {
        dispatch(ScenarioManagementContractIntentVoteScenario(scenarioId: scenarioId, vote: voteType))
    }

    func compareScenarios(scenarioIds: [String]) {
        dispatch(ScenarioManagementContractIntentCompareScenarios(scenarioIds: scenarioIds))
    }

    func clearComparison() {
        dispatch(ScenarioManagementContractIntentClearComparison.shared)
    }

    func clearError() {
        dispatch(ScenarioManagementContractIntentClearError.shared)
    }

    // MARK: - Convenience Properties

    var scenarios: [ScenarioWithVotes] { state.scenarios }
    var selectedScenario: Scenario_? { state.selectedScenario }
    var votingResults: [String: ScenarioVotingResult] { state.votingResults }
    var comparison: ScenarioManagementContract.ScenarioComparison? { state.comparison }
    var isEmpty: Bool { scenarios.isEmpty }
    var isComparing: Bool { comparison != nil }
    var scenariosRanked: [ScenarioWithVotes] { state.getScenariosRanked() }
    var isLoading: Bool { state.isLoading }
    var hasError: Bool { state.hasError }
    var errorMessage: String? { state.error }

    // MARK: - Private Methods

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
        text += "⏱️ Duration: \(scenario.duration) days\n"
        text += "👥 Estimated participants: \(scenario.estimatedParticipants)\n"
        text += "💰 Budget per person: €\(String(format: "%.2f", scenario.estimatedBudgetPerPerson))\n"
        if !scenario.description_.isEmpty {
            text += "\nDescription:\n\(scenario.description_)"
        }
        return text
    }

    // MARK: - Deinit

    deinit {
        stateMachineWrapper.dispose()
    }
}
