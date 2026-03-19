import Foundation
import Shared

/// ViewModel for the ScenarioListView.
///
/// Manages the state and intents for displaying a list of scenarios.
/// Uses the shared Kotlin state machine to handle all business logic.
class ScenarioListViewModel: StateMachineViewModel<
    ScenarioManagementContract.State,
    ScenarioManagementContractIntent,
    ScenarioManagementContractSideEffect
> {

    // MARK: - Initialization

    init() {
        let database = RepositoryProvider.shared.database
        let wrapper = IosFactory.shared.createScenarioStateMachine(database: database)
        super.init(stateMachineWrapper: wrapper)
    }

    // MARK: - Public Methods

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

    // MARK: - Side Effect Mapping

    override func mapSideEffect(_ effect: ScenarioManagementContractSideEffect) -> MappedSideEffect {
        switch effect {
        case let showToast as ScenarioManagementContractSideEffectShowToast:
            return .toast(showToast.message)
        case let navigateTo as ScenarioManagementContractSideEffectNavigateTo:
            return .navigate(navigateTo.route)
        case is ScenarioManagementContractSideEffectNavigateBack:
            return .back
        case let showError as ScenarioManagementContractSideEffectShowError:
            return .toast(showError.message)
        default:
            return .unhandled(effect)
        }
    }

    // MARK: - Additional Side Effect Handling

    override func handleAdditionalSideEffect(_ effect: ScenarioManagementContractSideEffect) {
        switch effect {
        case let shareScenario as ScenarioManagementContractSideEffectShareScenario:
            shareScenarioInternal(scenario: shareScenario.scenario)
        default:
            break
        }
    }

    // MARK: - Private Methods

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
}
