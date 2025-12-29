package com.guyghost.wakeve.presentation.statemachine

import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioWithVotes
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract.State
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract.Intent
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract.SideEffect
import com.guyghost.wakeve.presentation.usecase.LoadScenariosUseCase
import com.guyghost.wakeve.presentation.usecase.CreateScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.VoteScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.UpdateScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.DeleteScenarioUseCase
import kotlinx.coroutines.CoroutineScope

/**
 * State Machine for managing scenarios in an event planning workflow.
 *
 * This state machine handles all scenario-related operations:
 * - Loading scenarios for an event
 * - Creating new scenarios
 * - Updating and deleting scenarios
 * - Voting on scenarios (PREFER/NEUTRAL/AGAINST)
 * - Comparing scenarios side-by-side
 * - Managing voting results and aggregates
 *
 * ## Architecture
 *
 * ```
 * Intent (user action)
 *   ↓
 * handleIntent()
 *   ↓
 * updateState() ← Updates the UI state
 * emitSideEffect() ← Triggers navigation/toasts
 * ```
 *
 * ## Usage Example (Android)
 *
 * ```kotlin
 * @Composable
 * fun ScenarioListScreen(
 *     viewModel: ScenarioManagementViewModel = koinViewModel()
 * ) {
 *     val state by viewModel.state.collectAsState()
 *
 *     LaunchedEffect(Unit) {
 *         viewModel.dispatch(
 *             Intent.LoadScenariosForEvent(eventId, participantId)
 *         )
 *     }
 *
 *     LaunchedEffect(Unit) {
 *         viewModel.sideEffect.collect { effect ->
 *             when (effect) {
 *                 is SideEffect.NavigateTo -> navigate(effect.route)
 *                 is SideEffect.ShowToast -> showToast(effect.message)
 *                 is SideEffect.ShowError -> showError(effect.message)
 *                 is SideEffect.NavigateBack -> navController.popBackStack()
 *                 else -> {} // Handle other side effects
 *             }
 *         }
 *     }
 *
 *     ScenarioListContent(
 *         state = state,
 *         onSelectScenario = { scenarioId ->
 *             viewModel.dispatch(Intent.SelectScenario(scenarioId))
 *         },
 *         onVote = { scenarioId, voteType ->
 *             viewModel.dispatch(Intent.VoteScenario(scenarioId, voteType))
 *         }
 *     )
 * }
 * ```
 *
 * ## Usage Example (iOS)
 *
 * ```swift
 * class ScenarioListViewModel: ObservableObject {
 *     @Published var state: ScenarioManagementContract.State
 *     private let stateMachine: StateMachine<...>
 *
 *     init(eventId: String, participantId: String) {
 *         stateMachine = factory.createScenarioStateMachine()
 *         state = stateMachine.currentState
 *
 *         stateMachine.onStateChange = { [weak self] in
 *             self?.state = $0
 *         }
 *
 *         dispatch(.loadScenariosForEvent(eventId, participantId))
 *     }
 *
 *     func dispatch(_ intent: ScenarioManagementContract.Intent) {
 *         stateMachine.dispatch(intent: intent)
 *     }
 * }
 * ```
 *
 * @property loadScenariosUseCase Use case for loading scenarios with voting results
 * @property createScenarioUseCase Use case for creating new scenarios
 * @property voteScenarioUseCase Use case for voting on scenarios
 * @property updateScenarioUseCase Use case for updating scenarios
 * @property deleteScenarioUseCase Use case for deleting scenarios
 * @property scope CoroutineScope for launching async work
 */
class ScenarioManagementStateMachine(
    private val loadScenariosUseCase: LoadScenariosUseCase,
    private val createScenarioUseCase: CreateScenarioUseCase,
    private val voteScenarioUseCase: VoteScenarioUseCase,
    private val updateScenarioUseCase: UpdateScenarioUseCase,
    private val deleteScenarioUseCase: DeleteScenarioUseCase,
    scope: CoroutineScope
) : StateMachine<State, Intent, SideEffect>(
    initialState = State(),
    scope = scope
) {

    // ========================================================================
    // Intent Dispatcher
    // ========================================================================

    override suspend fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.LoadScenarios -> handleLoadScenarios()
            is Intent.LoadScenariosForEvent -> handleLoadScenariosForEvent(intent)
            is Intent.CreateScenario -> handleCreateScenario(intent)
            is Intent.SelectScenario -> handleSelectScenario(intent)
            is Intent.UpdateScenario -> handleUpdateScenario(intent)
            is Intent.DeleteScenario -> handleDeleteScenario(intent)
            is Intent.VoteScenario -> handleVoteScenario(intent)
            is Intent.CompareScenarios -> handleCompareScenarios(intent)
            is Intent.ClearComparison -> handleClearComparison()
            is Intent.ClearError -> handleClearError()
        }
    }

    // ========================================================================
    // Intent Handlers - Load Operations
    // ========================================================================

    /**
     * Handle load all scenarios intent.
     *
     * Legacy operation - loads scenarios using the current eventId from state.
     * Use [handleLoadScenariosForEvent] for new code.
     *
     * Flow:
     * 1. Set isLoading = true, clear error
     * 2. Validate eventId is not empty
     * 3. Call loadScenariosUseCase(eventId)
     * 4. On success: update scenarios and voting results, set isLoading = false
     * 5. On failure: set error, emit ShowError, set isLoading = false
     */
    private suspend fun handleLoadScenarios() {
        updateState { it.copy(isLoading = true, error = null) }

        val eventId = currentState.eventId
        if (eventId.isEmpty()) {
            val errorMsg = "Event ID not set"
            updateState { it.copy(isLoading = false, error = errorMsg) }
            emitSideEffect(SideEffect.ShowError(errorMsg))
            return
        }

        loadScenariosUseCase(eventId).fold(
            onSuccess = { scenarios ->
                updateState {
                    it.copy(
                        isLoading = false,
                        scenarios = scenarios,
                        votingResults = scenarios.associate { swv ->
                            swv.scenario.id to swv.votingResult
                        }
                    )
                }
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Failed to load scenarios"
                updateState { it.copy(isLoading = false, error = errorMsg) }
                emitSideEffect(SideEffect.ShowError(errorMsg))
            }
        )
    }

    /**
     * Handle load scenarios for a specific event.
     *
     * Typically dispatched when ScenarioListScreen appears.
     * Sets eventId and participantId in state.
     *
     * Flow:
     * 1. Set isLoading = true, update eventId and participantId
     * 2. Call loadScenariosUseCase(eventId)
     * 3. On success: update scenarios and voting results, set isLoading = false
     * 4. On failure: set error, emit ShowError, set isLoading = false
     *
     * @param intent Contains eventId and participantId to load for
     */
    private suspend fun handleLoadScenariosForEvent(intent: Intent.LoadScenariosForEvent) {
        updateState {
            it.copy(
                isLoading = true,
                error = null,
                eventId = intent.eventId,
                participantId = intent.participantId
            )
        }

        loadScenariosUseCase(intent.eventId).fold(
            onSuccess = { scenarios ->
                updateState {
                    it.copy(
                        isLoading = false,
                        scenarios = scenarios,
                        votingResults = scenarios.associate { swv ->
                            swv.scenario.id to swv.votingResult
                        }
                    )
                }
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Failed to load scenarios"
                updateState { it.copy(isLoading = false, error = errorMsg) }
                emitSideEffect(SideEffect.ShowError(errorMsg))
            }
        )
    }

    // ========================================================================
    // Intent Handlers - Create/Update/Delete Operations
    // ========================================================================

    /**
     * Handle create scenario intent.
     *
     * Creates a new scenario and reloads the scenarios list.
     * Only organizers can create scenarios.
     *
     * Flow:
     * 1. Set isLoading = true
     * 2. Call createScenarioUseCase(scenario)
     * 3. On success: reload scenarios, emit ShowToast, set isLoading = false
     * 4. On failure: set error, emit ShowError, set isLoading = false
     *
     * @param intent Contains the scenario to create
     */
    private suspend fun handleCreateScenario(intent: Intent.CreateScenario) {
        updateState { it.copy(isLoading = true, error = null) }

        createScenarioUseCase(
            name = intent.scenario.name,
            eventId = intent.scenario.eventId,
            dateOrPeriod = intent.scenario.dateOrPeriod,
            location = intent.scenario.location,
            duration = intent.scenario.duration,
            estimatedParticipants = intent.scenario.estimatedParticipants,
            estimatedBudgetPerPerson = intent.scenario.estimatedBudgetPerPerson,
            description = intent.scenario.description
        ).fold(
            onSuccess = { _ ->
                // Reload scenarios
                reloadScenarios(intent.scenario.eventId)
                emitSideEffect(SideEffect.ShowToast("Scenario created successfully"))
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Failed to create scenario"
                updateState { it.copy(isLoading = false, error = errorMsg) }
                emitSideEffect(SideEffect.ShowError(errorMsg))
            }
        )
    }

    /**
     * Handle update scenario intent.
     *
     * Updates an existing scenario and reloads the scenarios list.
     * Only organizers can update scenarios.
     *
     * Flow:
     * 1. Set isLoading = true
     * 2. Call updateScenarioUseCase(scenario)
     * 3. On success: reload scenarios, update selectedScenario, emit ShowToast
     * 4. On failure: set error, emit ShowError, set isLoading = false
     *
     * @param intent Contains the updated scenario
     */
    private suspend fun handleUpdateScenario(intent: Intent.UpdateScenario) {
        updateState { it.copy(isLoading = true, error = null) }

        updateScenarioUseCase(intent.scenario).fold(
            onSuccess = { updated ->
                // Reload scenarios
                reloadScenarios(intent.scenario.eventId)
                updateState { it.copy(selectedScenario = updated) }
                emitSideEffect(SideEffect.ShowToast("Scenario updated successfully"))
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Failed to update scenario"
                updateState { it.copy(isLoading = false, error = errorMsg) }
                emitSideEffect(SideEffect.ShowError(errorMsg))
            }
        )
    }

    /**
     * Handle delete scenario intent.
     *
     * Deletes a scenario and reloads the scenarios list.
     * Only organizers can delete scenarios.
     * Cannot delete if scenario is SELECTED.
     *
     * Flow:
     * 1. Set isLoading = true
     * 2. Get eventId from scenario in list or use current state eventId
     * 3. Call deleteScenarioUseCase(scenarioId)
     * 4. On success: reload scenarios, clear selectedScenario, emit NavigateBack
     * 5. On failure: set error, emit ShowError, set isLoading = false
     *
     * @param intent Contains the ID of the scenario to delete
     */
    private suspend fun handleDeleteScenario(intent: Intent.DeleteScenario) {
        updateState { it.copy(isLoading = true, error = null) }

        val scenario = currentState.scenarios.find { it.scenario.id == intent.scenarioId }
        val eventId = scenario?.scenario?.eventId ?: currentState.eventId

        deleteScenarioUseCase(intent.scenarioId).fold(
            onSuccess = { _ ->
                // Reload scenarios
                reloadScenarios(eventId)
                updateState { it.copy(selectedScenario = null) }
                emitSideEffect(SideEffect.ShowToast("Scenario deleted successfully"))
                emitSideEffect(SideEffect.NavigateBack)
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Failed to delete scenario"
                updateState { it.copy(isLoading = false, error = errorMsg) }
                emitSideEffect(SideEffect.ShowError(errorMsg))
            }
        )
    }

    // ========================================================================
    // Intent Handlers - Selection and Navigation
    // ========================================================================

    /**
     * Handle select scenario intent.
     *
     * Selects a scenario for viewing details.
     * Can also be used by organizer to select a scenario as final choice.
     *
     * Flow:
     * 1. Find scenario in scenarios list by ID
     * 2. If found: update selectedScenario, emit NavigateTo
     * 3. If not found: emit ShowError
     *
     * @param intent Contains the ID of the scenario to select
     */
    private suspend fun handleSelectScenario(intent: Intent.SelectScenario) {
        val scenario = currentState.scenarios.find { it.scenario.id == intent.scenarioId }

        if (scenario != null) {
            updateState { it.copy(selectedScenario = scenario.scenario) }
            emitSideEffect(SideEffect.NavigateTo("scenario/${intent.scenarioId}"))
        } else {
            emitSideEffect(SideEffect.ShowError("Scenario not found"))
        }
    }

    // ========================================================================
    // Intent Handlers - Voting
    // ========================================================================

    /**
     * Handle vote scenario intent.
     *
     * Votes on a scenario with the given vote type.
     * A participant can change their vote before organizer selects a scenario.
     * Reloads scenarios to get updated voting results.
     *
     * Flow:
     * 1. Get participantId from state
     * 2. If participantId is empty: emit ShowError and return
     * 3. Call voteScenarioUseCase(scenarioId, participantId, voteType)
     * 4. On success: reload scenarios, emit ShowToast
     * 5. On failure: set error, emit ShowError
     *
     * @param intent Contains scenarioId and vote type
     */
    private suspend fun handleVoteScenario(intent: Intent.VoteScenario) {
        val participantId = currentState.participantId

        if (participantId.isEmpty()) {
            emitSideEffect(SideEffect.ShowError("Participant ID not set"))
            return
        }

        updateState { it.copy(error = null) }

        voteScenarioUseCase(
            scenarioId = intent.scenarioId,
            participantId = participantId,
            voteType = intent.vote
        ).fold(
            onSuccess = { _ ->
                // Reload scenarios to get updated results
                reloadScenarios(currentState.eventId)
                emitSideEffect(SideEffect.ShowToast("Vote submitted successfully"))
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Failed to submit vote"
                updateState { it.copy(error = errorMsg) }
                emitSideEffect(SideEffect.ShowError(errorMsg))
            }
        )
    }

    // ========================================================================
    // Intent Handlers - Comparison
    // ========================================================================

    /**
     * Handle compare scenarios intent.
     *
     * Prepares scenarios for side-by-side comparison.
     * Validates that at least 2 scenarios are selected.
     *
     * Flow:
     * 1. Filter scenarios by IDs in intent
     * 2. If no scenarios found: emit ShowError and return
     * 3. Create ScenarioComparison with selected scenarios
     * 4. Update comparison in state
     * 5. Emit NavigateTo side effect to comparison screen
     *
     * @param intent Contains the list of scenario IDs to compare
     */
    private suspend fun handleCompareScenarios(intent: Intent.CompareScenarios) {
        val scenariosToCompare = currentState.scenarios.filter {
            it.scenario.id in intent.scenarioIds
        }

        if (scenariosToCompare.isEmpty()) {
            emitSideEffect(SideEffect.ShowError("No scenarios to compare"))
            return
        }

        val comparison = ScenarioManagementContract.ScenarioComparison(
            scenarioIds = intent.scenarioIds,
            scenarios = scenariosToCompare
        )

        updateState { it.copy(comparison = comparison) }
        emitSideEffect(SideEffect.NavigateTo("scenarios/compare"))
    }

    /**
     * Handle clear comparison intent.
     *
     * Clears the comparison mode when user navigates back from comparison screen.
     *
     * Flow:
     * 1. Set comparison to null
     */
    private suspend fun handleClearComparison() {
        updateState { it.copy(comparison = null) }
    }

    // ========================================================================
    // Intent Handlers - Error Management
    // ========================================================================

    /**
     * Handle clear error intent.
     *
     * Clears any error state to dismiss error messages in the UI.
     *
     * Flow:
     * 1. Set error to null
     */
    private suspend fun handleClearError() {
        updateState { it.copy(error = null) }
    }

    // ========================================================================
    // Private Helpers
    // ========================================================================

    /**
     * Helper to reload scenarios from repository.
     *
     * Used internally after operations that modify scenarios
     * (create, update, delete, vote).
     *
     * @param eventId The ID of the event to reload scenarios for
     */
    private suspend fun reloadScenarios(eventId: String) {
        loadScenariosUseCase(eventId).fold(
            onSuccess = { scenarios ->
                updateState {
                    it.copy(
                        isLoading = false,
                        scenarios = scenarios,
                        votingResults = scenarios.associate { swv ->
                            swv.scenario.id to swv.votingResult
                        }
                    )
                }
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Failed to reload scenarios"
                updateState { it.copy(isLoading = false, error = errorMsg) }
                emitSideEffect(SideEffect.ShowError(errorMsg))
            }
        )
    }
}
