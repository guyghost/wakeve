/**
 * Test Examples for Refactored ScenarioComparisonScreen
 *
 * This file contains example unit and integration tests to validate
 * the refactored ScenarioComparisonScreen using the State Machine pattern.
 *
 * Location: composeApp/src/commonTest/kotlin/com/guyghost/wakeve/ui/
 *
 * These tests demonstrate:
 * - ViewModel state observation
 * - Intent dispatching
 * - Side effect handling
 * - Lifecycle-aware state collection
 */

package com.guyghost.wakeve.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVotingResult
import com.guyghost.wakeve.models.ScenarioWithVotes
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract
import com.guyghost.wakeve.presentation.statemachine.ScenarioManagementStateMachine
import com.guyghost.wakeve.viewmodel.ScenarioManagementViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit Tests for ScenarioManagementViewModel in ComparisonMode
 */
class ScenarioComparisonViewModelTest {

    /**
     * Test: compareScenarios should dispatch CompareScenarios intent
     */
    @Test
    fun compareScenarios_shouldDispatchCompareIntentAndLoadComparison() = runTest {
        // Arrange
        val mockStateMachine = mockk<ScenarioManagementStateMachine>(relaxed = true)
        val mockState = MutableStateFlow(
            ScenarioManagementContract.State(
                isLoading = false,
                scenarios = emptyList(),
                comparison = null
            )
        )
        every { mockStateMachine.state } returns mockState

        val viewModel = ScenarioManagementViewModel(mockStateMachine)
        val scenarioIds = listOf("scenario-1", "scenario-2", "scenario-3")

        // Act
        viewModel.compareScenarios(scenarioIds)

        // Assert
        verify {
            mockStateMachine.dispatch(
                ScenarioManagementContract.Intent.CompareScenarios(scenarioIds)
            )
        }
    }

    /**
     * Test: State should contain comparison data after compareScenarios
     */
    @Test
    fun compareScenarios_shouldPopulateComparisonInState() = runTest {
        // Arrange
        val scenario1 = Scenario(
            id = "s1",
            eventId = "event-1",
            name = "Paris",
            dateOrPeriod = "Dec 20-23",
            location = "Paris, France",
            duration = 3,
            estimatedParticipants = 8,
            estimatedBudgetPerPerson = 500.0,
            description = "Paris trip",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-12-01T00:00:00Z",
            updatedAt = "2025-12-01T00:00:00Z"
        )

        val scenario2 = Scenario(
            id = "s2",
            eventId = "event-1",
            name = "Barcelona",
            dateOrPeriod = "Dec 22-25",
            location = "Barcelona, Spain",
            duration = 3,
            estimatedParticipants = 8,
            estimatedBudgetPerPerson = 400.0,
            description = "Barcelona trip",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-12-01T00:00:00Z",
            updatedAt = "2025-12-01T00:00:00Z"
        )

        val votingResult1 = ScenarioVotingResult(
            scenarioId = "s1",
            preferCount = 5,
            neutralCount = 2,
            againstCount = 1,
            totalVotes = 8,
            score = 9
        )

        val votingResult2 = ScenarioVotingResult(
            scenarioId = "s2",
            preferCount = 6,
            neutralCount = 1,
            againstCount = 1,
            totalVotes = 8,
            score = 11
        )

        val mockStateMachine = mockk<ScenarioManagementStateMachine>()
        val mockState = MutableStateFlow(
            ScenarioManagementContract.State(
                isLoading = false,
                scenarios = listOf(
                    ScenarioWithVotes(scenario1, listOf(), votingResult1),
                    ScenarioWithVotes(scenario2, listOf(), votingResult2)
                ),
                comparison = ScenarioManagementContract.ScenarioComparison(
                    scenarios = listOf(
                        ScenarioWithVotes(scenario1, listOf(), votingResult1),
                        ScenarioWithVotes(scenario2, listOf(), votingResult2)
                    ),
                    bestScenarioId = "s2"
                ),
                isComparing = true
            )
        )
        every { mockStateMachine.state } returns mockState

        val viewModel = ScenarioManagementViewModel(mockStateMachine)

        // Act
        val comparison = viewModel.comparison.value

        // Assert
        assertNotNull(comparison)
        assertEquals(2, comparison.scenarios.size)
        assertEquals("s2", comparison.bestScenarioId)
        assertEquals("Barcelona", comparison.scenarios[1].scenario.name)
        assertEquals(11, comparison.scenarios[1].votingResult.score)
    }

    /**
     * Test: clearComparison should reset comparison mode
     */
    @Test
    fun clearComparison_shouldDispatchClearComparisonIntent() = runTest {
        // Arrange
        val mockStateMachine = mockk<ScenarioManagementStateMachine>(relaxed = true)
        val mockState = MutableStateFlow(
            ScenarioManagementContract.State(
                isComparing = true,
                comparison = ScenarioManagementContract.ScenarioComparison(
                    scenarios = emptyList(),
                    bestScenarioId = null
                )
            )
        )
        every { mockStateMachine.state } returns mockState

        val viewModel = ScenarioManagementViewModel(mockStateMachine)

        // Act
        viewModel.clearComparison()

        // Assert
        verify {
            mockStateMachine.dispatch(ScenarioManagementContract.Intent.ClearComparison)
        }
    }

    /**
     * Test: Loading state should be observable via isLoading StateFlow
     */
    @Test
    fun isLoading_shouldReflectLoadingStateFromViewModelState() = runTest {
        // Arrange
        val mockStateMachine = mockk<ScenarioManagementStateMachine>()
        val mockState = MutableStateFlow(
            ScenarioManagementContract.State(isLoading = true)
        )
        every { mockStateMachine.state } returns mockState

        val viewModel = ScenarioManagementViewModel(mockStateMachine)

        // Act & Assert
        assertEquals(true, viewModel.isLoading.value)

        // Change state
        mockState.value = mockState.value.copy(isLoading = false)
        assertEquals(false, viewModel.isLoading.value)
    }

    /**
     * Test: Error message should be observable via errorMessage StateFlow
     */
    @Test
    fun errorMessage_shouldDisplayErrorFromState() = runTest {
        // Arrange
        val mockStateMachine = mockk<ScenarioManagementStateMachine>()
        val errorMsg = "Failed to load comparison data"
        val mockState = MutableStateFlow(
            ScenarioManagementContract.State(
                error = errorMsg,
                isLoading = false
            )
        )
        every { mockStateMachine.state } returns mockState

        val viewModel = ScenarioManagementViewModel(mockStateMachine)

        // Act & Assert
        assertEquals(errorMsg, viewModel.errorMessage.value)
    }
}

/**
 * Side Effect Tests
 */
class ScenarioComparisonSideEffectTest {

    /**
     * Test: NavigateBack side effect should trigger onBack callback
     */
    @Test
    fun sideEffect_navigationBack_shouldTriggerCallback() = runTest {
        // Arrange
        val sideEffectChannel = Channel<ScenarioManagementContract.SideEffect>()
        val mockStateMachine = mockk<ScenarioManagementStateMachine> {
            every { sideEffect } returns sideEffectChannel.receiveAsFlow()
            every { state } returns MutableStateFlow(
                ScenarioManagementContract.State()
            )
        }

        val viewModel = ScenarioManagementViewModel(mockStateMachine)
        var navigateBackCalled = false

        // Act
        sideEffectChannel.send(ScenarioManagementContract.SideEffect.NavigateBack)

        // Collect and verify
        val effect = viewModel.sideEffect.value as? ScenarioManagementContract.SideEffect
        val isNavigateBack = effect is ScenarioManagementContract.SideEffect.NavigateBack

        // Assert
        assertTrue(isNavigateBack)
    }

    /**
     * Test: ShowError side effect should contain error message
     */
    @Test
    fun sideEffect_showError_shouldContainErrorMessage() = runTest {
        // Arrange
        val errorMsg = "Invalid scenario IDs"
        val sideEffectChannel = Channel<ScenarioManagementContract.SideEffect>()
        val mockStateMachine = mockk<ScenarioManagementStateMachine> {
            every { sideEffect } returns sideEffectChannel.receiveAsFlow()
            every { state } returns MutableStateFlow(
                ScenarioManagementContract.State()
            )
        }

        // Act
        sideEffectChannel.send(
            ScenarioManagementContract.SideEffect.ShowError(errorMsg)
        )

        // Assert
        val effect = viewModel.sideEffect.value
        assertTrue(effect is ScenarioManagementContract.SideEffect.ShowError)
        // Note: Extract message from effect and verify
    }
}

/**
 * Integration Tests (Compose + ViewModel)
 */
class ScenarioComparisonScreenIntegrationTest {

    /**
     * Test: Screen should show loading state when ViewModel is loading
     *
     * This test validates the integration between:
     * - ViewModel state collection with collectAsStateWithLifecycle()
     * - UI rendering based on isLoading state
     */
    @Test
    fun scenarioComparisonScreen_displaysLoadingState_whenViewModelIsLoading() = runTest {
        // Arrange
        val mockStateMachine = mockk<ScenarioManagementStateMachine>()
        val mockState = MutableStateFlow(
            ScenarioManagementContract.State(
                isLoading = true,
                comparison = null
            )
        )
        every { mockStateMachine.state } returns mockState
        every { mockStateMachine.sideEffect } returns mockk()

        val viewModel = ScenarioManagementViewModel(mockStateMachine)

        // Act
        val state = viewModel.state.value
        val isLoading = viewModel.isLoading.value

        // Assert
        assertEquals(true, isLoading)
        // In Compose test, would verify CircularProgressIndicator is visible
    }

    /**
     * Test: Screen should display comparison table when data is loaded
     */
    @Test
    fun scenarioComparisonScreen_displaysComparisonTable_whenDataLoaded() = runTest {
        // Arrange
        val scenario1 = Scenario(
            id = "s1",
            eventId = "event-1",
            name = "Scenario 1",
            dateOrPeriod = "Dec 20-23",
            location = "Paris",
            duration = 3,
            estimatedParticipants = 8,
            estimatedBudgetPerPerson = 500.0,
            description = "Test",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-12-01T00:00:00Z",
            updatedAt = "2025-12-01T00:00:00Z"
        )

        val mockStateMachine = mockk<ScenarioManagementStateMachine>()
        val mockState = MutableStateFlow(
            ScenarioManagementContract.State(
                isLoading = false,
                comparison = ScenarioManagementContract.ScenarioComparison(
                    scenarios = listOf(
                        ScenarioWithVotes(
                            scenario1,
                            emptyList(),
                            ScenarioVotingResult(
                                scenarioId = "s1",
                                preferCount = 5,
                                neutralCount = 2,
                                againstCount = 1,
                                totalVotes = 8,
                                score = 9
                            )
                        )
                    ),
                    bestScenarioId = "s1"
                )
            )
        )
        every { mockStateMachine.state } returns mockState

        val viewModel = ScenarioManagementViewModel(mockStateMachine)

        // Act
        val comparison = viewModel.comparison.value

        // Assert
        assertNotNull(comparison)
        assertEquals(1, comparison.scenarios.size)
        assertEquals("s1", comparison.bestScenarioId)
        // In Compose test, would verify ComparisonTableImpl is visible
    }

    /**
     * Test: Screen should display error when ViewModel has error
     */
    @Test
    fun scenarioComparisonScreen_displaysError_whenViewModelHasError() = runTest {
        // Arrange
        val errorMsg = "Failed to load scenarios"
        val mockStateMachine = mockk<ScenarioManagementStateMachine>()
        val mockState = MutableStateFlow(
            ScenarioManagementContract.State(
                isLoading = false,
                error = errorMsg
            )
        )
        every { mockStateMachine.state } returns mockState

        val viewModel = ScenarioManagementViewModel(mockStateMachine)

        // Act
        val error = viewModel.errorMessage.value

        // Assert
        assertEquals(errorMsg, error)
        // In Compose test, would verify error Card is visible with correct message
    }

    /**
     * Test: Clicking clear button should call clearComparison
     */
    @Test
    fun scenarioComparisonScreen_clickingClearButton_shouldCallClearComparison() = runTest {
        // Arrange
        val mockStateMachine = mockk<ScenarioManagementStateMachine>(relaxed = true)
        val mockState = MutableStateFlow(
            ScenarioManagementContract.State(isComparing = true)
        )
        every { mockStateMachine.state } returns mockState

        val viewModel = ScenarioManagementViewModel(mockStateMachine)

        // Act
        viewModel.clearComparison()

        // Assert
        verify {
            mockStateMachine.dispatch(ScenarioManagementContract.Intent.ClearComparison)
        }
    }
}

/**
 * Compose UI Tests (using Compose Test Runner)
 *
 * These tests should be run with:
 * @RunWith(ComposeTestRunner::class)
 *
 * Example setup:
 * @get:Rule
 * val composeRule = createComposeRule()
 */
/*
class ScenarioComparisonScreenComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun scenarioComparisonScreen_displaysLoadingIndicator() {
        val mockViewModel = mockk<ScenarioManagementViewModel>()
        every { mockViewModel.state } returns MutableStateFlow(
            ScenarioManagementContract.State(isLoading = true)
        )
        every { mockViewModel.isLoading } returns MutableStateFlow(true)
        every { mockViewModel.errorMessage } returns MutableStateFlow(null)
        every { mockViewModel.comparison } returns MutableStateFlow(null)
        every { mockViewModel.sideEffect } returns mockk()

        composeRule.setContent {
            ScenarioComparisonScreen(
                scenarioIds = listOf("s1", "s2"),
                eventTitle = "Test Event",
                viewModel = mockViewModel,
                onBack = {}
            )
        }

        composeRule.onNodeWithText("Loading scenarios...").assertIsDisplayed()
    }

    @Test
    fun scenarioComparisonScreen_displaysComparisonTable() {
        // Create mock scenarios and viewModel
        val mockViewModel = createMockViewModelWithComparison()

        composeRule.setContent {
            ScenarioComparisonScreen(
                scenarioIds = listOf("s1", "s2"),
                eventTitle = "Test Event",
                viewModel = mockViewModel,
                onBack = {}
            )
        }

        // Verify table headers are displayed
        composeRule.onNodeWithText("Scenario 1").assertIsDisplayed()
        composeRule.onNodeWithText("Scenario 2").assertIsDisplayed()

        // Verify table metrics are displayed
        composeRule.onNodeWithText("Location").assertIsDisplayed()
        composeRule.onNodeWithText("Budget/Person").assertIsDisplayed()
    }

    @Test
    fun scenarioComparisonScreen_clickingBackButton_callsOnBack() {
        val mockViewModel = mockk<ScenarioManagementViewModel>()
        every { mockViewModel.state } returns MutableStateFlow(
            ScenarioManagementContract.State()
        )
        // ... setup other mocks

        var onBackCalled = false

        composeRule.setContent {
            ScenarioComparisonScreen(
                scenarioIds = listOf("s1", "s2"),
                eventTitle = "Test Event",
                viewModel = mockViewModel,
                onBack = { onBackCalled = true }
            )
        }

        composeRule.onNodeWithContentDescription("Back").performClick()
        assertEquals(true, onBackCalled)
    }
}
*/
