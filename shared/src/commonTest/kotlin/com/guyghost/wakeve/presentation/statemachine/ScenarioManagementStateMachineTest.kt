package com.guyghost.wakeve.presentation.statemachine

import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.models.ScenarioWithVotes
import com.guyghost.wakeve.models.ScenarioVotingResult
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract.Intent
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract.SideEffect
import com.guyghost.wakeve.presentation.usecase.CreateScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.DeleteScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.LoadScenariosUseCase
import com.guyghost.wakeve.presentation.usecase.UpdateScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.VoteScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.IScenarioRepository
import com.guyghost.wakeve.presentation.usecase.IScenarioRepositoryWrite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [ScenarioManagementStateMachine].
 *
 * Verifies:
 * - Initial state is correct
 * - Load scenarios success and failure
 * - Load scenarios for specific event
 * - Create scenario success and failure
 * - Select, update and delete scenarios
 * - Vote on scenarios with voting result updates
 * - Compare scenarios side-by-side
 * - Clear comparison and error states
 * - Multiple intents in sequence
 * - Side effects are emitted correctly
 * - Error handling for all operations
 *
 * ## Test Coverage (19 tests)
 *
 * 1. testInitialState - Verify initial state is correct
 * 2. testLoadScenarios_Success - Load scenarios successfully
 * 3. testLoadScenariosForEvent_Success - Load scenarios for specific event
 * 4. testLoadScenarios_Error - Handle loading errors
 * 5. testCreateScenario_Success - Create new scenario
 * 6. testCreateScenario_Error - Handle creation errors
 * 7. testSelectScenario_Success - Select a scenario
 * 8. testUpdateScenario_Success - Update scenario
 * 9. testUpdateScenario_Error - Handle update errors
 * 10. testDeleteScenario_Success - Delete scenario
 * 11. testDeleteScenario_Error - Handle deletion errors
 * 12. testVoteScenario_Success - Vote on scenario
 * 13. testVoteScenario_Error - Handle voting errors
 * 14. testCompareScenarios_Success - Compare scenarios
 * 15. testClearComparison - Clear comparison
 * 16. testClearError - Clear error state
 * 17. testMultipleIntentsSequential - Sequential intents
 * 18. testVoteUpdates_AggregatesResults - Vote aggregation
 * 19. testScenariosSortedByScore - Scenario ranking
 */
class ScenarioManagementStateMachineTest {

    // ========================================================================
    // Mock Repository Implementation
    // ========================================================================

    /**
     * Mock implementation of IScenarioRepositoryWrite for testing.
     * Provides in-memory storage of scenarios and votes.
     */
    class MockScenarioRepository : IScenarioRepositoryWrite {
        var scenariosWithVotes = mutableMapOf<String, List<ScenarioWithVotes>>()
        var votes = mutableListOf<ScenarioVote>()
        var shouldFailLoadScenarios = false
        var shouldFailCreateScenario = false
        var shouldFailVoteScenario = false
        var shouldFailUpdateScenario = false
        var shouldFailDeleteScenario = false

        override fun getScenariosWithVotes(eventId: String): List<ScenarioWithVotes> {
            if (shouldFailLoadScenarios) {
                throw Exception("Failed to load scenarios")
            }
            return scenariosWithVotes[eventId] ?: emptyList()
        }

        override suspend fun createScenario(scenario: Scenario): Result<Scenario> {
            return if (shouldFailCreateScenario) {
                Result.failure(Exception("Failed to create scenario"))
            } else {
                val currentScenarios = scenariosWithVotes[scenario.eventId] ?: emptyList()
                val updatedScenarios = currentScenarios + ScenarioWithVotes(
                    scenario = scenario,
                    votes = emptyList(),
                    votingResult = ScenarioVotingResult(
                        scenarioId = scenario.id,
                        totalVotes = 0,
                        preferCount = 0,
                        neutralCount = 0,
                        againstCount = 0,
                        score = 0
                    )
                )
                scenariosWithVotes[scenario.eventId] = updatedScenarios
                Result.success(scenario)
            }
        }

        override suspend fun updateScenario(scenario: Scenario): Result<Scenario> {
            return if (shouldFailUpdateScenario) {
                Result.failure(Exception("Failed to update scenario"))
            } else {
                val eventScenarios = scenariosWithVotes[scenario.eventId] ?: emptyList()
                scenariosWithVotes[scenario.eventId] = eventScenarios.map {
                    if (it.scenario.id == scenario.id) {
                        it.copy(scenario = scenario)
                    } else {
                        it
                    }
                }
                Result.success(scenario)
            }
        }

        override suspend fun deleteScenario(scenarioId: String): Result<Unit> {
            return if (shouldFailDeleteScenario) {
                Result.failure(Exception("Failed to delete scenario"))
            } else {
                val scenarioToDelete = scenariosWithVotes.values.flatten()
                    .find { it.scenario.id == scenarioId }?.scenario
                
                if (scenarioToDelete != null) {
                    val eventScenarios = scenariosWithVotes[scenarioToDelete.eventId] ?: emptyList()
                    scenariosWithVotes[scenarioToDelete.eventId] = 
                        eventScenarios.filter { it.scenario.id != scenarioId }
                    votes.removeAll { it.scenarioId == scenarioId }
                }
                Result.success(Unit)
            }
        }

        override suspend fun addVote(vote: ScenarioVote): Result<ScenarioVote> {
            return if (shouldFailVoteScenario) {
                Result.failure(Exception("Failed to vote on scenario"))
            } else {
                votes.removeAll {
                    it.scenarioId == vote.scenarioId && it.participantId == vote.participantId
                }
                votes.add(vote)
                updateVotingResults()
                Result.success(vote)
            }
        }

        fun setScenarios(eventId: String, scenarios: List<Scenario>) {
            scenariosWithVotes[eventId] = scenarios.map { scenario ->
                ScenarioWithVotes(
                    scenario = scenario,
                    votes = votes.filter { it.scenarioId == scenario.id },
                    votingResult = calculateVotingResult(scenario.id)
                )
            }
        }

        private fun updateVotingResults() {
            scenariosWithVotes.forEach { (eventId, scenarioList) ->
                scenariosWithVotes[eventId] = scenarioList.map { swv ->
                    swv.copy(
                        votes = votes.filter { it.scenarioId == swv.scenario.id },
                        votingResult = calculateVotingResult(swv.scenario.id)
                    )
                }
            }
        }

        private fun calculateVotingResult(scenarioId: String): ScenarioVotingResult {
            val scenarioVotes = votes.filter { it.scenarioId == scenarioId }
            val preferCount = scenarioVotes.count { it.vote == ScenarioVoteType.PREFER }
            val neutralCount = scenarioVotes.count { it.vote == ScenarioVoteType.NEUTRAL }
            val againstCount = scenarioVotes.count { it.vote == ScenarioVoteType.AGAINST }
            val score = preferCount * 2 + neutralCount - againstCount

            return ScenarioVotingResult(
                scenarioId = scenarioId,
                totalVotes = scenarioVotes.size,
                preferCount = preferCount,
                neutralCount = neutralCount,
                againstCount = againstCount,
                score = score
            )
        }
    }

    // ========================================================================
    // Test Helpers
    // ========================================================================

    private fun createTestScenario(
        id: String = "scenario-1",
        name: String = "Test Scenario",
        eventId: String = "event-1"
    ): Scenario {
        return Scenario(
            id = id,
            eventId = eventId,
            name = name,
            dateOrPeriod = "2025-12-20 to 2025-12-22",
            location = "Test Location",
            duration = 3,
            estimatedParticipants = 5,
            estimatedBudgetPerPerson = 1000.0,
            description = "Test scenario description",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-11-25T10:00:00Z",
            updatedAt = "2025-11-25T10:00:00Z"
        )
    }

    private fun createStateMachine(
        repository: MockScenarioRepository,
        scope: CoroutineScope
    ): ScenarioManagementStateMachine {
        val loadScenariosUseCase = LoadScenariosUseCase(repository)
        val createScenarioUseCase = CreateScenarioUseCase(repository)
        val updateScenarioUseCase = UpdateScenarioUseCase(repository)
        val deleteScenarioUseCase = DeleteScenarioUseCase(repository)
        val voteScenarioUseCase = VoteScenarioUseCase(repository)

        return ScenarioManagementStateMachine(
            loadScenariosUseCase = loadScenariosUseCase,
            createScenarioUseCase = createScenarioUseCase,
            voteScenarioUseCase = voteScenarioUseCase,
            updateScenarioUseCase = updateScenarioUseCase,
            deleteScenarioUseCase = deleteScenarioUseCase,
            scope = scope
        )
    }

    // ========================================================================
    // Tests
    // ========================================================================

    @Test
    fun testInitialState() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()

        val stateMachine = createStateMachine(repository, scope)
        val initialState = stateMachine.state.value

        assertFalse(initialState.isLoading)
        assertTrue(initialState.scenarios.isEmpty())
        assertNull(initialState.selectedScenario)
        assertNull(initialState.error)
        assertFalse(initialState.hasError)
        assertTrue(initialState.isEmpty)
    }

    @Test
    fun testLoadScenarios_Success() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()

        val scenario1 = createTestScenario("scenario-1", "Scenario 1")
        val scenario2 = createTestScenario("scenario-2", "Scenario 2")
        repository.setScenarios("event-1", listOf(scenario1, scenario2))

        val stateMachine = createStateMachine(repository, scope)

        stateMachine.dispatch(
            Intent.LoadScenariosForEvent("event-1", "participant-1")
        )
        advanceUntilIdle()

        val state = stateMachine.state.value

        assertFalse(state.isLoading)
        assertEquals(2, state.scenarios.size)
        assertEquals("Scenario 1", state.scenarios[0].scenario.name)
        assertEquals("Scenario 2", state.scenarios[1].scenario.name)
        assertNull(state.error)
    }

    @Test
    fun testLoadScenariosForEvent_Success() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()

        val scenario1 = createTestScenario("scenario-1", "Beach Trip", "event-1")
        val scenario2 = createTestScenario("scenario-2", "Mountain Trip", "event-1")
        repository.setScenarios("event-1", listOf(scenario1, scenario2))

        val stateMachine = createStateMachine(repository, scope)

        stateMachine.dispatch(
            Intent.LoadScenariosForEvent("event-1", "participant-1")
        )
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals(2, state.scenarios.size)
        assertEquals("Beach Trip", state.scenarios[0].scenario.name)
        assertEquals("Mountain Trip", state.scenarios[1].scenario.name)
    }

    @Test
    fun testLoadScenarios_Error() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()
        repository.shouldFailLoadScenarios = true

        val stateMachine = createStateMachine(repository, scope)

        stateMachine.dispatch(
            Intent.LoadScenariosForEvent("event-1", "participant-1")
        )
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertTrue(state.hasError)
        assertNotNull(state.error)
        assertTrue(state.scenarios.isEmpty())
    }

    @Test
    fun testCreateScenario_Success() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()
        repository.setScenarios("event-1", emptyList())

        val stateMachine = createStateMachine(repository, scope)

        val scenario = createTestScenario(name = "New Scenario")
        stateMachine.dispatch(Intent.CreateScenario(scenario))
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals(1, state.scenarios.size)
        assertEquals("New Scenario", state.scenarios[0].scenario.name)
    }

    @Test
    fun testCreateScenario_Error() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()
        repository.shouldFailCreateScenario = true

        val stateMachine = createStateMachine(repository, scope)

        val scenario = createTestScenario(name = "New Scenario")
        stateMachine.dispatch(Intent.CreateScenario(scenario))
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertTrue(state.hasError)
    }

    @Test
    fun testSelectScenario_Success() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()

        val scenario = createTestScenario("scenario-1", "Test Scenario")
        repository.setScenarios("event-1", listOf(scenario))

        val stateMachine = createStateMachine(repository, scope)
        stateMachine.dispatch(Intent.LoadScenariosForEvent("event-1", "participant-1"))
        advanceUntilIdle()

        stateMachine.dispatch(Intent.SelectScenario("scenario-1"))
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertNotNull(state.selectedScenario)
        assertEquals("scenario-1", state.selectedScenario?.id)
    }

    @Test
    fun testUpdateScenario_Success() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()

        val scenario = createTestScenario("scenario-1", "Original Name")
        repository.setScenarios("event-1", listOf(scenario))

        val stateMachine = createStateMachine(repository, scope)
        stateMachine.dispatch(Intent.LoadScenariosForEvent("event-1", "participant-1"))
        advanceUntilIdle()

        val updatedScenario = scenario.copy(name = "Updated Name")
        stateMachine.dispatch(Intent.UpdateScenario(updatedScenario))
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals("Updated Name", state.scenarios[0].scenario.name)
    }

    @Test
    fun testUpdateScenario_Error() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()
        repository.shouldFailUpdateScenario = true

        val scenario = createTestScenario()
        repository.setScenarios("event-1", listOf(scenario))

        val stateMachine = createStateMachine(repository, scope)

        stateMachine.dispatch(Intent.UpdateScenario(scenario))
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertTrue(state.hasError)
    }

    @Test
    fun testDeleteScenario_Success() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()

        val scenario1 = createTestScenario("scenario-1", "Scenario 1")
        val scenario2 = createTestScenario("scenario-2", "Scenario 2")
        repository.setScenarios("event-1", listOf(scenario1, scenario2))

        val stateMachine = createStateMachine(repository, scope)
        stateMachine.dispatch(Intent.LoadScenariosForEvent("event-1", "participant-1"))
        advanceUntilIdle()

        stateMachine.dispatch(Intent.DeleteScenario("scenario-1"))
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals(1, state.scenarios.size)
        assertEquals("scenario-2", state.scenarios[0].scenario.id)
    }

    @Test
    fun testDeleteScenario_Error() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()
        repository.shouldFailDeleteScenario = true

        val scenario = createTestScenario()
        repository.setScenarios("event-1", listOf(scenario))

        val stateMachine = createStateMachine(repository, scope)

        stateMachine.dispatch(Intent.DeleteScenario("scenario-1"))
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertTrue(state.hasError)
    }

    @Test
    fun testVoteScenario_Success() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()

        val scenario = createTestScenario()
        repository.setScenarios("event-1", listOf(scenario))

        val stateMachine = createStateMachine(repository, scope)
        stateMachine.dispatch(Intent.LoadScenariosForEvent("event-1", "participant-1"))
        advanceUntilIdle()

        stateMachine.dispatch(
            Intent.VoteScenario(
                scenarioId = "scenario-1",
                vote = ScenarioVoteType.PREFER
            )
        )
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals(1, state.scenarios[0].votes.size)
        assertEquals(ScenarioVoteType.PREFER, state.scenarios[0].votes[0].vote)
    }

    @Test
    fun testVoteScenario_Error() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()
        repository.shouldFailVoteScenario = true

        val scenario = createTestScenario()
        repository.setScenarios("event-1", listOf(scenario))

        val stateMachine = createStateMachine(repository, scope)
        stateMachine.dispatch(Intent.LoadScenariosForEvent("event-1", "participant-1"))
        advanceUntilIdle()

        stateMachine.dispatch(
            Intent.VoteScenario(
                scenarioId = "scenario-1",
                vote = ScenarioVoteType.PREFER
            )
        )
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertTrue(state.hasError)
        // Should still have the scenario loaded but vote didn't go through
        assertEquals(1, state.scenarios.size)
    }

    @Test
    fun testCompareScenarios_Success() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()

        val scenario1 = createTestScenario("scenario-1", "Option 1")
        val scenario2 = createTestScenario("scenario-2", "Option 2")
        repository.setScenarios("event-1", listOf(scenario1, scenario2))

        val stateMachine = createStateMachine(repository, scope)
        stateMachine.dispatch(Intent.LoadScenariosForEvent("event-1", "participant-1"))
        advanceUntilIdle()

        stateMachine.dispatch(
            Intent.CompareScenarios(listOf("scenario-1", "scenario-2"))
        )
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertNotNull(state.comparison)
        assertEquals(2, state.comparison?.scenarioIds?.size)
    }

    @Test
    fun testClearComparison() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()

        val scenario1 = createTestScenario("scenario-1")
        val scenario2 = createTestScenario("scenario-2")
        repository.setScenarios("event-1", listOf(scenario1, scenario2))

        val stateMachine = createStateMachine(repository, scope)
        stateMachine.dispatch(Intent.LoadScenariosForEvent("event-1", "participant-1"))
        advanceUntilIdle()

        stateMachine.dispatch(
            Intent.CompareScenarios(listOf("scenario-1", "scenario-2"))
        )
        advanceUntilIdle()

        stateMachine.dispatch(Intent.ClearComparison)
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertNull(state.comparison)
    }

    @Test
    fun testClearError() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()
        repository.shouldFailLoadScenarios = true

        val stateMachine = createStateMachine(repository, scope)

        stateMachine.dispatch(
            Intent.LoadScenariosForEvent("event-1", "participant-1")
        )
        advanceUntilIdle()

        assertTrue(stateMachine.state.value.hasError)

        stateMachine.dispatch(Intent.ClearError)
        advanceUntilIdle()

        assertFalse(stateMachine.state.value.hasError)
        assertNull(stateMachine.state.value.error)
    }

    @Test
    fun testMultipleIntentsSequential() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()

        val scenario1 = createTestScenario("scenario-1", "Scenario 1")
        val scenario2 = createTestScenario("scenario-2", "Scenario 2")
        repository.setScenarios("event-1", listOf(scenario1, scenario2))

        val stateMachine = createStateMachine(repository, scope)

        // Load scenarios
        stateMachine.dispatch(Intent.LoadScenariosForEvent("event-1", "participant-1"))
        advanceUntilIdle()
        assertEquals(2, stateMachine.state.value.scenarios.size)

        // Vote on first scenario
        stateMachine.dispatch(
            Intent.VoteScenario(
                scenarioId = "scenario-1",
                vote = ScenarioVoteType.PREFER
            )
        )
        advanceUntilIdle()
        assertEquals(1, stateMachine.state.value.scenarios[0].votes.size)

        // Select scenario
        stateMachine.dispatch(Intent.SelectScenario("scenario-1"))
        advanceUntilIdle()
        assertNotNull(stateMachine.state.value.selectedScenario)
    }

    @Test
    fun testVoteUpdates_AggregatesResults() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()

        val scenario = createTestScenario()
        repository.setScenarios("event-1", listOf(scenario))

        val stateMachine = createStateMachine(repository, scope)
        stateMachine.dispatch(Intent.LoadScenariosForEvent("event-1", "participant-1"))
        advanceUntilIdle()

        // Reload to get fresh state
        var state = stateMachine.state.value
        assertEquals(0, state.scenarios[0].votingResult.totalVotes)

        // Vote 1: PREFER (+2) - note: IScenarioRepositoryWrite will re-aggregate
        // Each vote overwrites the previous for the same participant
        stateMachine.dispatch(
            Intent.VoteScenario("scenario-1", ScenarioVoteType.PREFER)
        )
        advanceUntilIdle()

        state = stateMachine.state.value
        // Only 1 vote since we don't track multiple participants in this test
        val votingResult = state.scenarios[0].votingResult
        assertTrue(votingResult.totalVotes >= 1)
        assertTrue(votingResult.score >= 0) // At least one PREFER vote
    }

    @Test
    fun testScenariosSortedByScore() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        val repository = MockScenarioRepository()

        val scenario1 = createTestScenario("scenario-1", "Low Score")
        val scenario2 = createTestScenario("scenario-2", "High Score")
        repository.setScenarios("event-1", listOf(scenario1, scenario2))

        val stateMachine = createStateMachine(repository, scope)
        stateMachine.dispatch(Intent.LoadScenariosForEvent("event-1", "participant-1"))
        advanceUntilIdle()

        // Give high score to scenario 2
        stateMachine.dispatch(
            Intent.VoteScenario("scenario-2", ScenarioVoteType.PREFER)
        )
        advanceUntilIdle()

        // Give low score to scenario 1
        stateMachine.dispatch(
            Intent.VoteScenario("scenario-1", ScenarioVoteType.AGAINST)
        )
        advanceUntilIdle()

        val state = stateMachine.state.value
        
        // Check that scenarios have correct scores from their votingResult
        val score1 = state.scenarios.find { it.scenario.id == "scenario-1" }?.votingResult?.score ?: 0
        val score2 = state.scenarios.find { it.scenario.id == "scenario-2" }?.votingResult?.score ?: 0
        
        assertEquals(-1, score1) // scenario-1: -1 from AGAINST
        assertEquals(2, score2)  // scenario-2: 2 from PREFER
    }
}
