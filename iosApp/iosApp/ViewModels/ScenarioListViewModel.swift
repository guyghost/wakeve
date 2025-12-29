import Foundation
import Shared

/// ViewModel for the ScenarioListView.
///
/// Manages the state and intents for displaying a list of scenarios.
/// Uses the shared Kotlin state machine to handle all business logic.
///
/// ## State Management
///
/// The ViewModel:
/// 1. Creates a state machine via `IosFactory`
/// 2. Observes state changes from the state machine
/// 3. Observes side effects for navigation and toasts
/// 4. Exposes `dispatch()` method for the view to send intents
///
/// ## Usage
///
/// ```swift
/// @StateObject private var viewModel = ScenarioListViewModel()
///
/// var body: some View {
///     List(viewModel.scenarios) { scenario in
///         ScenarioRow(scenarioWithVotes: scenario)
///             .onTapGesture {
///                 viewModel.selectScenario(scenarioId: scenario.scenario.id)
///             }
///     }
/// }
///
/// func selectScenario(scenarioId: String) {
///     dispatch(.selectScenario(scenarioId: scenarioId))
/// }
/// ```
@MainActor
class ScenarioListViewModel: ObservableObject {
    // MARK: - Published Properties

    /// Current state from the state machine
    @Published var state: ScenarioManagementContractState

    /// Toast message to display (auto-clears after display)
    @Published var toastMessage: String?

    /// Navigation route to trigger (e.g., "detail/scenario-123")
    @Published var navigationRoute: String?

    /// Whether to pop back to previous screen
    @Published var shouldNavigateBack = false

    // MARK: - Private Properties

    /// The observable state machine wrapper
    private let stateMachineWrapper: ViewModelWrapper<
        ScenarioManagementContractState,
        ScenarioManagementContractIntent,
        ScenarioManagementContractSideEffect
    >

    // MARK: - Initialization

    init() {
        // Get the shared database from RepositoryProvider
        let database = RepositoryProvider.shared.database

        // Create the state machine via iOS factory
        self.stateMachineWrapper = IosFactory().createScenarioStateMachine(database: database)

        // Initialize state with current state from state machine
        self.state = self.stateMachineWrapper.currentState as! ScenarioManagementContractState

        // Observe state changes from the state machine
        self.stateMachineWrapper.onStateChange = { [weak self] newState in
            guard let self = self else { return }

            DispatchQueue.main.async {
                if let newState = newState as? ScenarioManagementContractState {
                    self.state = newState
                }
            }
        }

        // Observe side effects from the state machine
        self.stateMachineWrapper.onSideEffect = { [weak self] effect in
            guard let self = self else { return }

            DispatchQueue.main.async {
                self.handleSideEffect(effect)
            }
        }
    }

    // MARK: - Public Methods

    /// Dispatch an intent to the state machine.
    ///
    /// - Parameter intent: The intent to dispatch (e.g., LoadScenarios, SelectScenario)
    func dispatch(_ intent: ScenarioManagementContractIntent) {
        stateMachineWrapper.dispatch(intent: intent)
    }

    /// Initialize with event ID and participant ID.
    ///
    /// - Parameters:
    ///   - eventId: The ID of the event
    ///   - participantId: The ID of the current participant
    func initialize(eventId: String, participantId: String) {
        dispatch(.loadScenariosForEvent(eventId: eventId, participantId: participantId))
    }

    /// Load scenarios for the current event.
    func loadScenarios() {
        dispatch(.loadScenarios())
    }

    /// Create a new scenario.
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
        let scenario = Scenario(
            id: "", // Auto-generated
            eventId: eventId,
            name: name,
            dateOrPeriod: dateOrPeriod,
            location: location,
            duration: Int(duration),
            estimatedParticipants: Int(estimatedParticipants),
            estimatedBudgetPerPerson: estimatedBudgetPerPerson,
            description: description,
            status: ScenarioStatus.proposed,
            createdAt: "", // Auto-generated
            updatedAt: "" // Auto-generated
        )

        dispatch(.createScenario(scenario: scenario))
    }

    /// Select a scenario for viewing details.
    ///
    /// - Parameter scenarioId: The ID of the scenario to select
    func selectScenario(scenarioId: String) {
        dispatch(.selectScenario(scenarioId: scenarioId))
    }

    /// Update an existing scenario.
    ///
    /// - Parameter scenario: The updated scenario
    func updateScenario(scenario: Scenario) {
        dispatch(.updateScenario(scenario: scenario))
    }

    /// Delete a scenario.
    ///
    /// - Parameter scenarioId: The ID of the scenario to delete
    func deleteScenario(scenarioId: String) {
        dispatch(.deleteScenario(scenarioId: scenarioId))
    }

    /// Vote on a scenario.
    ///
    /// - Parameters:
    ///   - scenarioId: The ID of the scenario to vote on
    ///   - voteType: The vote type (PREFER, NEUTRAL, AGAINST)
    func voteScenario(scenarioId: String, voteType: ScenarioVoteType) {
        dispatch(.voteScenario(scenarioId: scenarioId, vote: voteType))
    }

    /// Compare multiple scenarios side-by-side.
    ///
    /// - Parameter scenarioIds: List of scenario IDs to compare (must have at least 2)
    func compareScenarios(scenarioIds: [String]) {
        dispatch(.compareScenarios(scenarioIds: scenarioIds))
    }

    /// Clear scenario comparison mode.
    func clearComparison() {
        dispatch(.clearComparison())
    }

    /// Clear any error state.
    func clearError() {
        dispatch(.clearError())
    }

    // MARK: - Convenience Properties

    /// List of scenarios with their votes
    var scenarios: [ScenarioWithVotes] {
        state.scenarios
    }

    /// Currently selected scenario
    var selectedScenario: Scenario? {
        state.selectedScenario
    }

    /// Map of scenario ID to voting results
    var votingResults: [String: ScenarioVotingResult] {
        state.votingResults
    }

    /// Current scenario comparison (if in comparison mode)
    var comparison: ScenarioManagementContractScenarioComparison? {
        state.comparison
    }

    /// Whether scenarios list is empty
    var isEmpty: Bool {
        scenarios.isEmpty
    }

    /// Whether currently in comparison mode
    var isComparing: Bool {
        comparison != nil
    }

    /// Scenarios sorted by voting score (descending)
    var scenariosRanked: [ScenarioWithVotes] {
        state.getScenariosRanked()
    }

    /// Current loading state
    var isLoading: Bool {
        state.isLoading
    }

    /// Whether there is an error
    var hasError: Bool {
        state.hasError
    }

    /// Error message (if any)
    var errorMessage: String? {
        state.error
    }

    // MARK: - Private Methods

    /// Handle side effects emitted by the state machine.
    ///
    /// - Parameter effect: The side effect to handle
    private func handleSideEffect(_ effect: Any) {
        if let showToast = effect as? ScenarioManagementContractSideEffectShowToast {
            toastMessage = showToast.message
        } else if let navigateTo = effect as? ScenarioManagementContractSideEffectNavigateTo {
            navigationRoute = navigateTo.route
        } else if effect is ScenarioManagementContractSideEffectNavigateBack {
            shouldNavigateBack = true
        } else if let showError = effect as? ScenarioManagementContractSideEffectShowError {
            toastMessage = showError.message
        } else if let shareScenario = effect as? ScenarioManagementContractSideEffectShareScenario {
            // Handle share scenario side effect
            shareScenario(scenario: shareScenario.scenario)
        }
    }

    /// Share a scenario via the system share sheet.
    ///
    /// - Parameter scenario: The scenario to share
    private func shareScenario(scenario: Scenario) {
        // Prepare scenario data for sharing
        let shareText = prepareShareText(scenario: scenario)
        // TODO: Present UIActivityViewController or iOS 16+ ShareLink
        print("Share scenario: \(shareText)")
    }

    /// Prepare text content for sharing a scenario.
    ///
    /// - Parameter scenario: The scenario to prepare for sharing
    /// - Returns: Formatted text representation of the scenario
    private func prepareShareText(scenario: Scenario) -> String {
        var text = "📅 \(scenario.name)\n"
        text += "📍 \(scenario.location)\n"
        text += "⏱️ Duration: \(scenario.duration) days\n"
        text += "👥 Estimated participants: \(scenario.estimatedParticipants)\n"
        text += "💰 Budget per person: €\(String(format: "%.2f", scenario.estimatedBudgetPerPerson))\n"

        if !scenario.description.isEmpty {
            text += "\nDescription:\n\(scenario.description)"
        }

        return text
    }

    // MARK: - Deinit

    deinit {
        // Clean up state machine resources
        stateMachineWrapper.dispose()
    }
}

// MARK: - Type Extensions

/// Helper extensions for creating intents
extension ScenarioManagementContractIntent {
    static func loadScenariosForEvent(
        eventId: String,
        participantId: String
    ) -> ScenarioManagementContractIntent {
        return ScenarioManagementContractIntent.loadScenariosForEvent(
            eventId: eventId,
            participantId: participantId
        )
    }

    /// Helper to create LoadScenarios intent
    static func loadScenarios() -> ScenarioManagementContractIntent {
        return ScenarioManagementContractIntent.loadScenarios()
    }

    /// Helper to create SelectScenario intent
    static func selectScenario(scenarioId: String) -> ScenarioManagementContractIntent {
        return ScenarioManagementContractIntent.selectScenario(scenarioId: scenarioId)
    }

    /// Helper to create VoteScenario intent
    static func voteScenario(
        scenarioId: String,
        vote: ScenarioVoteType
    ) -> ScenarioManagementContractIntent {
        return ScenarioManagementContractIntent.voteScenario(
            scenarioId: scenarioId,
            vote: vote
        )
    }

    /// Helper to create CompareScenarios intent
    static func compareScenarios(scenarioIds: [String]) -> ScenarioManagementContractIntent {
        return ScenarioManagementContractIntent.compareScenarios(scenarioIds: scenarioIds)
    }

    /// Helper to create CreateScenario intent
    static func createScenario(scenario: Scenario) -> ScenarioManagementContractIntent {
        return ScenarioManagementContractIntent.createScenario(scenario: scenario)
    }

    /// Helper to create UpdateScenario intent
    static func updateScenario(scenario: Scenario) -> ScenarioManagementContractIntent {
        return ScenarioManagementContractIntent.updateScenario(scenario: scenario)
    }

    /// Helper to create DeleteScenario intent
    static func deleteScenario(scenarioId: String) -> ScenarioManagementContractIntent {
        return ScenarioManagementContractIntent.deleteScenario(scenarioId: scenarioId)
    }

    /// Helper to create ClearComparison intent
    static func clearComparison() -> ScenarioManagementContractIntent {
        return ScenarioManagementContractIntent.clearComparison()
    }

    /// Helper to create ClearError intent
    static func clearError() -> ScenarioManagementContractIntent {
        return ScenarioManagementContractIntent.clearError()
    }
}
