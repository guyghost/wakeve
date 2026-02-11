package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.models.ScenarioVotingResult
import com.guyghost.wakeve.models.ScenarioWithVotes
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract
import com.guyghost.wakeve.presentation.statemachine.ScenarioManagementStateMachine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for scenario management that wraps the ScenarioManagementStateMachine.
 *
 * This ViewModel serves as the bridge between Jetpack Compose UI and the state machine.
 * It exposes the state and side effects from the state machine in a way that Compose
 * can consume them efficiently via StateFlow and Flow.
 *
 * Scenarios represent different planning options for an event, combining:
 * - Date or period (e.g., "Dec 20-23" or "Weekend of Dec 21")
 * - Location / destination
 * - Duration (in days)
 * - Estimated participants and budget
 *
 * Participants vote on scenarios using PREFER/NEUTRAL/AGAINST votes.
 * The organizer can select or reject scenarios based on voting results.
 *
 * ## Usage in Compose
 *
 * ```kotlin
 * @Composable
 * fun ScenarioListScreen(
 *     viewModel: ScenarioManagementViewModel = koinViewModel()
 * ) {
 *     val state by viewModel.state.collectAsState()
 *
 *     // Load scenarios when screen appears
 *     LaunchedEffect(Unit) {
 *         viewModel.initialize(eventId = "event-1", participantId = "participant-1")
 *     }
 *
 *     // Handle side effects (navigation, toasts, etc.)
 *     LaunchedEffect(Unit) {
 *         viewModel.sideEffect.collect { effect ->
 *             when (effect) {
 *                 is ScenarioManagementContract.SideEffect.NavigateTo -> {
 *                     navController.navigate(effect.route)
 *                 }
 *                 is ScenarioManagementContract.SideEffect.ShowToast -> {
 *                     showToast(effect.message)
 *                 }
 *                 is ScenarioManagementContract.SideEffect.ShowError -> {
 *                     showError(effect.message)
 *                 }
 *                 is ScenarioManagementContract.SideEffect.NavigateBack -> {
 *                     navController.popBackStack()
 *                 }
 *                 else -> {} // Handle other side effects
 *             }
 *         }
 *     }
 *
 *     // Render UI with state
 *     ScenarioListContent(
 *         state = state,
 *         onSelectScenario = { scenarioId ->
 *             viewModel.selectScenario(scenarioId)
 *         },
 *         onVote = { scenarioId, voteType ->
 *             viewModel.voteScenario(scenarioId, voteType)
 *         }
 *     )
 * }
 * ```
 *
 * @property stateMachine The underlying state machine for scenario management
 */
class ScenarioManagementViewModel(
    private val stateMachine: ScenarioManagementStateMachine
) : ViewModel() {

    /**
     * Observable state flow from the state machine.
     *
     * Collect this in Compose UI using:
     * ```kotlin
     * val state by viewModel.state.collectAsState()
     * ```
     *
     * This state includes:
     * - `isLoading`: Whether scenarios are being loaded
     * - `eventId`: The ID of the event these scenarios belong to
     * - `participantId`: The ID of the current participant (for voting)
     * - `scenarios`: List of all scenarios with their votes
     * - `selectedScenario`: The currently selected scenario (for detail views)
     * - `votingResults`: Voting aggregates for each scenario
     * - `comparison`: Current scenario comparison data (if comparing)
     * - `error`: Error message if an operation failed
     */
    val state: StateFlow<ScenarioManagementContract.State> = stateMachine.state

    /**
     * Observable side effect flow from the state machine.
     *
     * Collect this in a LaunchedEffect to handle one-shot events:
     * ```kotlin
     * LaunchedEffect(Unit) {
     *     viewModel.sideEffect.collect { effect ->
     *         when (effect) {
     *             is ScenarioManagementContract.SideEffect.NavigateTo -> navigate(effect.route)
     *             is ScenarioManagementContract.SideEffect.ShowToast -> showToast(effect.message)
     *             is ScenarioManagementContract.SideEffect.ShowError -> showError(effect.message)
     *             is ScenarioManagementContract.SideEffect.NavigateBack -> navController.popBackStack()
     *             is ScenarioManagementContract.SideEffect.ShareScenario -> shareScenario(effect.scenario)
     *         }
     *     }
     * }
     * ```
     *
     * Side effects are one-time events that don't persist in state:
     * - Navigation to other screens
     * - Toast messages
     * - Error notifications
     * - Back navigation
     * - Sharing scenarios
     */
    val sideEffect: Flow<ScenarioManagementContract.SideEffect> = stateMachine.sideEffect

    /**
     * Dispatch an intent to the state machine.
     *
     * This is the primary way to trigger state updates or side effects.
     * All user actions in the UI should be translated to intents and dispatched here.
     *
     * ## Example
     *
     * ```kotlin
     * Button(onClick = {
     *     viewModel.dispatch(ScenarioManagementContract.Intent.LoadScenarios)
     * }) {
     *     Text("Reload")
     * }
     * ```
     *
     * @param intent The intent to dispatch to the state machine
     */
    fun dispatch(intent: ScenarioManagementContract.Intent) {
        stateMachine.dispatch(intent)
    }

    /**
     * Initialize the ViewModel with event and participant IDs.
     *
     * This should be called once when the screen appears to load scenarios
     * for the specified event and set up the participant context.
     *
     * ## Example
     *
     * ```kotlin
     * LaunchedEffect(Unit) {
     *     viewModel.initialize(eventId = "event-1", participantId = "participant-1")
     * }
     * ```
     *
     * @param eventId The ID of the event
     * @param participantId The ID of the current participant
     */
    fun initialize(eventId: String, participantId: String) {
        dispatch(
            ScenarioManagementContract.Intent.LoadScenariosForEvent(eventId, participantId)
        )
    }

    /**
     * Load scenarios for the current event.
     *
     * This is a convenience method that reloads scenarios without changing
     * the eventId or participantId. Use this for manual refreshes.
     *
     * Equivalent to:
     * ```kotlin
     * viewModel.dispatch(ScenarioManagementContract.Intent.LoadScenarios)
     * ```
     */
    fun loadScenarios() {
        dispatch(ScenarioManagementContract.Intent.LoadScenarios)
    }

    /**
     * Create a new scenario.
     *
     * Only the organizer can create scenarios. This method constructs a Scenario
     * object from the provided parameters and dispatches the create intent.
     *
     * ## Example
     *
     * ```kotlin
     * viewModel.createScenario(
     *     name = "Paris Weekend",
     *     eventId = "event-1",
     *     dateOrPeriod = "Dec 20-23",
     *     location = "Paris, France",
     *     duration = 3,
     *     estimatedParticipants = 8,
     *     estimatedBudgetPerPerson = 500.0,
     *     description = "Weekend in Paris with visits to museums and restaurants"
     * )
     * ```
     *
     * @param name The name of the scenario
     * @param eventId The ID of the event this scenario belongs to
     * @param dateOrPeriod The date or date range (e.g., "Dec 20-23")
     * @param location The destination/location
     * @param duration The estimated duration in days
     * @param estimatedParticipants The estimated number of participants
     * @param estimatedBudgetPerPerson The estimated budget per person
     * @param description A description of the scenario
     */
    fun createScenario(
        name: String,
        eventId: String,
        dateOrPeriod: String,
        location: String,
        duration: Int,
        estimatedParticipants: Int,
        estimatedBudgetPerPerson: Double,
        description: String
    ) {
        val scenario = Scenario(
            id = "", // Auto-generated by backend
            eventId = eventId,
            name = name,
            dateOrPeriod = dateOrPeriod,
            location = location,
            duration = duration,
            estimatedParticipants = estimatedParticipants,
            estimatedBudgetPerPerson = estimatedBudgetPerPerson,
            description = description,
            status = ScenarioStatus.PROPOSED,
            createdAt = "", // Auto-generated by backend
            updatedAt = "" // Auto-generated by backend
        )
        dispatch(ScenarioManagementContract.Intent.CreateScenario(scenario))
    }

    /**
     * Select a scenario for viewing details.
     *
     * Can be used by any participant to view details or by the organizer
     * to select a scenario as the final choice.
     *
     * Equivalent to:
     * ```kotlin
     * viewModel.dispatch(ScenarioManagementContract.Intent.SelectScenario(scenarioId))
     * ```
     *
     * @param scenarioId The ID of the scenario to select
     */
    fun selectScenario(scenarioId: String) {
        dispatch(ScenarioManagementContract.Intent.SelectScenario(scenarioId))
    }

    /**
     * Update an existing scenario.
     *
     * Only the organizer can update scenarios. This method dispatches
     * an update intent to the state machine.
     *
     * If the scenario is SELECTED, participants cannot change their votes.
     *
     * ## Example
     *
     * ```kotlin
     * val updatedScenario = existingScenario.copy(
     *     name = "Updated Name",
     *     description = "Updated description"
     * )
     * viewModel.updateScenario(updatedScenario)
     * ```
     *
     * @param scenario The updated scenario
     */
    fun updateScenario(scenario: Scenario) {
        dispatch(ScenarioManagementContract.Intent.UpdateScenario(scenario))
    }

    /**
     * Delete a scenario.
     *
     * Only the organizer can delete scenarios. Cannot delete if the scenario
     * is SELECTED. This method dispatches a delete intent to the state machine.
     *
     * Equivalent to:
     * ```kotlin
     * viewModel.dispatch(ScenarioManagementContract.Intent.DeleteScenario(scenarioId))
     * ```
     *
     * @param scenarioId The ID of the scenario to delete
     */
    fun deleteScenario(scenarioId: String) {
        dispatch(ScenarioManagementContract.Intent.DeleteScenario(scenarioId))
    }

    /**
     * Vote on a scenario.
     *
     * Any participant can vote on scenarios. Votes can be changed before
     * the organizer selects a scenario.
     *
     * ## Example
     *
     * ```kotlin
     * viewModel.voteScenario(
     *     scenarioId = "scenario-1",
     *     voteType = ScenarioVoteType.PREFER
     * )
     * ```
     *
     * @param scenarioId The ID of the scenario to vote on
     * @param voteType The vote type (PREFER, NEUTRAL, or AGAINST)
     */
    fun voteScenario(scenarioId: String, voteType: ScenarioVoteType) {
        dispatch(ScenarioManagementContract.Intent.VoteScenario(scenarioId, voteType))
    }

    /**
     * Compare multiple scenarios side-by-side.
     *
     * This is typically called when a participant wants to make an informed
     * choice between multiple scenarios. The state machine will load detailed
     * comparison data and navigate to the comparison screen.
     *
     * ## Example
     *
     * ```kotlin
     * viewModel.compareScenarios(listOf("scenario-1", "scenario-2", "scenario-3"))
     * ```
     *
     * @param scenarioIds List of scenario IDs to compare (should have at least 2)
     */
    fun compareScenarios(scenarioIds: List<String>) {
        dispatch(ScenarioManagementContract.Intent.CompareScenarios(scenarioIds))
    }

    /**
     * Clear scenario comparison mode.
     *
     * Used when the user navigates back from the comparison screen to return
     * to the regular scenario list view.
     *
     * Equivalent to:
     * ```kotlin
     * viewModel.dispatch(ScenarioManagementContract.Intent.ClearComparison)
     * ```
     */
    fun clearComparison() {
        dispatch(ScenarioManagementContract.Intent.ClearComparison)
    }

    /**
     * Clear error state.
     *
     * This is a convenience method to dismiss error messages in the UI.
     * Call this when the user acknowledges an error.
     *
     * Equivalent to:
     * ```kotlin
     * viewModel.dispatch(ScenarioManagementContract.Intent.ClearError)
     * ```
     */
    fun clearError() {
        dispatch(ScenarioManagementContract.Intent.ClearError)
    }

    // ========================================================================
    // Convenience StateFlow Properties
    // ========================================================================

    /**
     * Loading state as a separate StateFlow.
     *
     * Use this when you only need to observe the loading state
     * rather than the entire state.
     *
     * ## Example
     *
     * ```kotlin
     * val isLoading by viewModel.isLoading.collectAsState()
     * if (isLoading) {
     *     CircularProgressIndicator()
     * }
     * ```
     */
    val isLoading: StateFlow<Boolean> = state
        .map { it.isLoading }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Error state as a separate StateFlow.
     *
     * Use this when you only need to observe the error state.
     * Returns true if there's an error, false otherwise.
     *
     * ## Example
     *
     * ```kotlin
     * val hasError by viewModel.hasError.collectAsState()
     * if (hasError) {
     *     ErrorDialog(onDismiss = { viewModel.clearError() })
     * }
     * ```
     */
    val hasError: StateFlow<Boolean> = state
        .map { it.error != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Error message as a separate StateFlow.
     *
     * Use this to display error messages to the user.
     *
     * ## Example
     *
     * ```kotlin
     * val errorMessage by viewModel.errorMessage.collectAsState()
     * errorMessage?.let { message ->
     *     Snackbar(message = message)
     * }
     * ```
     */
    val errorMessage: StateFlow<String?> = state
        .map { it.error }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Scenarios list as a separate StateFlow.
     *
     * Use this when you only need to observe the scenarios list.
     *
     * ## Example
     *
     * ```kotlin
     * val scenarios by viewModel.scenarios.collectAsState()
     * LazyColumn {
     *     items(scenarios) { scenario ->
     *         ScenarioCard(scenario)
     *     }
     * }
     * ```
     */
    val scenarios: StateFlow<List<ScenarioWithVotes>> = state
        .map { it.scenarios }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Selected scenario as a separate StateFlow.
     *
     * Use this to observe the currently selected scenario (for detail views).
     *
     * ## Example
     *
     * ```kotlin
     * val selectedScenario by viewModel.selectedScenario.collectAsState()
     * selectedScenario?.let { scenario ->
     *     ScenarioDetailContent(scenario)
     * }
     * ```
     */
    val selectedScenario: StateFlow<Scenario?> = state
        .map { it.selectedScenario }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Voting results as a separate StateFlow.
     *
     * Use this to observe voting aggregates for all scenarios.
     *
     * ## Example
     *
     * ```kotlin
     * val votingResults by viewModel.votingResults.collectAsState()
     * votingResults[scenarioId]?.let { result ->
     *     VotingResultsDisplay(result)
     * }
     * ```
     */
    val votingResults: StateFlow<Map<String, ScenarioVotingResult>> = state
        .map { it.votingResults }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    /**
     * Scenario comparison as a separate StateFlow.
     *
     * Use this to observe comparison data when in comparison mode.
     *
     * ## Example
     *
     * ```kotlin
     * val comparison by viewModel.comparison.collectAsState()
     * comparison?.let { comp ->
     *     ScenarioComparisonContent(comp.scenarios)
     * }
     * ```
     */
    val comparison: StateFlow<ScenarioManagementContract.ScenarioComparison?> = state
        .map { it.comparison }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Is in comparison mode as a separate StateFlow.
     *
     * Use this to know if we're currently in comparison mode.
     *
     * ## Example
     *
     * ```kotlin
     * val isComparing by viewModel.isComparing.collectAsState()
     * if (isComparing) {
     *     ScenarioComparisonScreen()
     * } else {
     *     ScenarioListScreen()
     * }
     * ```
     */
    val isComparing: StateFlow<Boolean> = state
        .map { it.isComparing }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Is scenarios list empty as a separate StateFlow.
     *
     * Use this to show empty state UI when there are no scenarios.
     *
     * ## Example
     *
     * ```kotlin
     * val isEmpty by viewModel.isEmpty.collectAsState()
     * if (isEmpty) {
     *     EmptyStateContent()
     * } else {
     *     ScenariosList()
     * }
     * ```
     */
    val isEmpty: StateFlow<Boolean> = state
        .map { it.isEmpty }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    /**
     * Scenarios ranked by voting score as a separate StateFlow.
     *
     * Use this to display scenarios in order of preference based on votes.
     *
     * ## Example
     *
     * ```kotlin
     * val rankedScenarios by viewModel.scenariosRanked.collectAsState()
     * LazyColumn {
     *     items(rankedScenarios) { scenario ->
     *         ScenarioCard(scenario, rank = index + 1)
     *     }
     * }
     * ```
     */
    val scenariosRanked: StateFlow<List<ScenarioWithVotes>> = state
        .map { it.getScenariosRanked() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
