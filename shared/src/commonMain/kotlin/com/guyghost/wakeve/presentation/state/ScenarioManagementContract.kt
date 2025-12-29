package com.guyghost.wakeve.presentation.state

import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.models.ScenarioVotingResult
import com.guyghost.wakeve.models.ScenarioWithVotes
import kotlinx.serialization.Serializable

/**
 * Contract for Scenario Management state machine.
 *
 * Defines the State, Intent and SideEffect types for the ScenarioManagementStateMachine.
 * This contract is used by both Android (Compose) and iOS (SwiftUI).
 *
 * Scenarios represent different planning options for an event, combining:
 * - Date or period
 * - Location / destination
 * - Duration (in days)
 * - Estimated participants and budget
 *
 * Participants vote on scenarios using PREFER/NEUTRAL/AGAINST votes.
 * The organizer can select or reject scenarios based on voting results.
 *
 * ## State Flow Example
 *
 * 1. User opens ScenarioListScreen
 * 2. Screen dispatches Intent.LoadScenariosForEvent(eventId, participantId)
 * 3. StateMachine loads scenarios from repository, updates state with isLoading=true
 * 4. Scenarios loaded with voting results, isLoading=false
 * 5. UI recomposes with scenarios list and voting results
 * 6. User taps scenario, screen dispatches Intent.SelectScenario(scenarioId)
 * 7. StateMachine emits SideEffect.NavigateTo("scenario-detail/{scenarioId}")
 * 8. User votes on scenario, dispatches Intent.VoteScenario(scenarioId, PREFER)
 * 9. StateMachine updates voting result in repository and local state
 * 10. Organizer compares scenarios with Intent.CompareScenarios([id1, id2])
 * 11. StateMachine loads comparison data and updates state
 * 12. Organizer selects best scenario with Intent.SelectScenario(bestId)
 */
object ScenarioManagementContract {

    // ========================================================================
    // STATE
    // ========================================================================

    /**
     * Immutable state for scenario management.
     *
     * This state is consumed by UI to render scenario lists, details and voting.
     * All fields are immutable - updates are done via copy().
     *
     * @property isLoading True while loading scenarios from repository
     * @property eventId The ID of the event these scenarios belong to
     * @property participantId The ID of the current participant (for voting)
     * @property scenarios List of scenarios with their votes and voting results
     * @property selectedScenario The currently selected scenario (for detail view)
     * @property votingResults Map of scenario ID to voting result aggregates
     * @property comparison Current scenario comparison (if in comparison mode)
     * @property error Error message if operation failed (null = no error)
     */
    @Serializable
    data class State(
        val isLoading: Boolean = false,
        val eventId: String = "",
        val participantId: String = "",
        val scenarios: List<ScenarioWithVotes> = emptyList(),
        val selectedScenario: Scenario? = null,
        val votingResults: Map<String, ScenarioVotingResult> = emptyMap(),
        val comparison: ScenarioComparison? = null,
        val error: String? = null
    ) {
        /**
         * Convenient property to check if there's an error
         */
        val hasError: Boolean get() = error != null

        /**
         * Convenient property to check if list is empty
         */
        val isEmpty: Boolean get() = scenarios.isEmpty()

        /**
         * Convenient property to check if in comparison mode
         */
        val isComparing: Boolean get() = comparison != null

        /**
         * Get voting result for a specific scenario
         */
        fun getVotingResult(scenarioId: String): ScenarioVotingResult? =
            votingResults[scenarioId]

        /**
         * Get scenario with votes by ID
         */
        fun getScenarioWithVotes(scenarioId: String): ScenarioWithVotes? =
            scenarios.find { it.scenario.id == scenarioId }

        /**
         * Get selected scenarios in comparison mode
         */
        fun getComparisonScenarios(): List<ScenarioWithVotes> =
            comparison?.scenarios ?: emptyList()

        /**
         * Sort scenarios by voting score (descending)
         */
        fun getScenariosRanked(): List<ScenarioWithVotes> =
            scenarios.sortedByDescending { scenario ->
                votingResults[scenario.scenario.id]?.score ?: 0
            }
    }

    /**
     * Scenario comparison data for side-by-side analysis.
     *
     * Used when user wants to compare multiple scenarios to make
     * an informed decision about which to select.
     *
     * @property scenarioIds List of scenario IDs being compared
     * @property scenarios List of full scenarios with votes for comparison
     */
    @Serializable
    data class ScenarioComparison(
        val scenarioIds: List<String>,
        val scenarios: List<ScenarioWithVotes>
    ) {
        /**
         * Check if this comparison is valid (has at least 2 scenarios)
         */
        val isValid: Boolean get() = scenarioIds.size >= 2

        /**
         * Get comparison summary (e.g., for display)
         */
        fun getSummary(): String =
            "Comparing ${scenarioIds.size} scenarios"
    }

    // ========================================================================
    // INTENT
    // ========================================================================

    /**
     * Intents that can be dispatched to the state machine.
     *
     * Each intent represents a user action or lifecycle event that should
     * trigger state updates or side effects.
     *
     * Use sealed interfaces for type-safe intent handling:
     * ```kotlin
     * when (intent) {
     *     is Intent.LoadScenariosForEvent -> { }
     *     is Intent.VoteScenario -> { }
     *     // ... exhaustive
     * }
     * ```
     */
    sealed interface Intent {
        /**
         * Load scenarios for a specific event.
         *
         * Typically dispatched when ScenarioListScreen appears.
         * Sets isLoading=true, then loads scenarios with their voting results,
         * then sets isLoading=false.
         *
         * @property eventId The ID of the event
         * @property participantId The ID of the current participant (for voting)
         */
        data class LoadScenariosForEvent(
            val eventId: String,
            val participantId: String
        ) : Intent

        /**
         * Load all scenarios (legacy, use LoadScenariosForEvent instead).
         *
         * Sets isLoading=true, then loads scenarios, then sets isLoading=false.
         */
        data object LoadScenarios : Intent

        /**
         * Create a new scenario.
         *
         * Persists the scenario to the repository.
         * Only the organizer can create scenarios.
         * Emits side effects for success or error handling.
         *
         * @property scenario The scenario to create
         */
        data class CreateScenario(val scenario: Scenario) : Intent

        /**
         * Select a scenario for viewing details.
         *
         * Sets selectedScenario and loads its voting data.
         * Can also be used to select a scenario as the final choice (by organizer).
         * Emits NavigateTo side effect to navigate to detail screen.
         *
         * @property scenarioId The ID of the scenario to select
         */
        data class SelectScenario(val scenarioId: String) : Intent

        /**
         * Update an existing scenario.
         *
         * Only the organizer can update scenarios.
         * Persists to repository and updates local state.
         * If scenario is SELECTED, participants cannot change votes.
         *
         * @property scenario The updated scenario
         */
        data class UpdateScenario(val scenario: Scenario) : Intent

        /**
         * Delete a scenario.
         *
         * Only the organizer can delete scenarios.
         * Cannot delete if scenario is SELECTED.
         * Removes from repository and updates local state.
         *
         * @property scenarioId The ID of the scenario to delete
         */
        data class DeleteScenario(val scenarioId: String) : Intent

        /**
         * Vote on a scenario.
         *
         * Participants can vote PREFER/NEUTRAL/AGAINST.
         * A participant can change their vote before organizer selects a scenario.
         * Persists to repository and updates voting results.
         *
         * @property scenarioId The ID of the scenario to vote on
         * @property vote The vote type (PREFER, NEUTRAL, AGAINST)
         */
        data class VoteScenario(
            val scenarioId: String,
            val vote: ScenarioVoteType
        ) : Intent

        /**
         * Compare multiple scenarios side-by-side.
         *
         * Typically called when participant wants to make an informed choice.
         * Sets comparison property with detailed voting breakdown.
         * Emits NavigateTo side effect to navigate to comparison screen.
         *
         * @property scenarioIds List of scenario IDs to compare (must have at least 2)
         */
        data class CompareScenarios(val scenarioIds: List<String>) : Intent

        /**
         * Clear scenario comparison mode.
         *
         * Used when user navigates back from comparison screen.
         * Sets comparison to null.
         */
        data object ClearComparison : Intent

        /**
         * Clear any error state.
         *
         * Use this to dismiss error messages in the UI.
         */
        data object ClearError : Intent
    }

    // ========================================================================
    // SIDE EFFECT
    // ========================================================================

    /**
     * One-shot side effects emitted by the state machine.
     *
     * Side effects are NOT part of the state - they are one-time events
     * that need to be handled by the UI (navigation, showing toasts, etc).
     *
     * Each side effect should be collected and handled once, then discarded.
     *
     * ```kotlin
     * LaunchedEffect(Unit) {
     *     viewModel.sideEffect.collect { effect ->
     *         when (effect) {
     *             is SideEffect.NavigateTo -> navigation.navigate(effect.route)
     *             is SideEffect.ShowToast -> showToast(effect.message)
     *             // ... exhaustive
     *         }
     *     }
     * }
     * ```
     */
    sealed interface SideEffect {
        /**
         * Show a toast message to the user.
         *
         * Typically used for success/error confirmation messages.
         *
         * @property message The message to show
         */
        data class ShowToast(val message: String) : SideEffect

        /**
         * Navigate to a specific route/screen.
         *
         * The UI is responsible for interpreting the route and navigating.
         * Common routes:
         * - "list" - Scenario list
         * - "detail/{scenarioId}" - Scenario detail
         * - "create" - Create new scenario
         * - "compare/{ids}" - Compare scenarios
         *
         * @property route The route to navigate to
         */
        data class NavigateTo(val route: String) : SideEffect

        /**
         * Navigate back to the previous screen.
         *
         * The UI should interpret this as popping the back stack.
         */
        data object NavigateBack : SideEffect

        /**
         * Show an error message to the user.
         *
         * Used for more prominent error display compared to regular toast.
         *
         * @property message The error message to show
         */
        data class ShowError(val message: String) : SideEffect

        /**
         * Share a scenario (export as ICS, send link, etc).
         *
         * The UI should present sharing options.
         *
         * @property scenario The scenario to share
         */
        data class ShareScenario(val scenario: Scenario) : SideEffect
    }
}
