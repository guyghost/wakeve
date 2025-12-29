package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.models.ScenarioVotingResult
import com.guyghost.wakeve.models.ScenarioWithVotes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [LoadScenariosUseCase].
 *
 * Verifies:
 * - Loading scenarios with votes successfully
 * - Handling empty scenario list
 * - Aggregating voting results correctly
 * - Handling exceptions from repository
 * - Filtering scenarios by event ID
 * - Result wrapping (success vs failure)
 */
class LoadScenariosUseCaseTest {

    // ========================================================================
    // Test Fixtures & Setup
    // ========================================================================

    private lateinit var loadScenariosUseCase: LoadScenariosUseCase
    private lateinit var mockRepository: MockScenarioRepository

    @BeforeTest
    fun setup() {
        mockRepository = MockScenarioRepository()
        loadScenariosUseCase = LoadScenariosUseCase(mockRepository)
    }

    // ========================================================================
    // Mock Repository Implementation
    // ========================================================================

    /**
     * Mock ScenarioRepository for testing.
     * Implements the IScenarioRepository interface needed by LoadScenariosUseCase.
     */
    class MockScenarioRepository : IScenarioRepository {
        var scenariosToReturn: List<ScenarioWithVotes> = emptyList()
        var shouldThrowError = false

        override fun getScenariosWithVotes(eventId: String): List<ScenarioWithVotes> {
            if (shouldThrowError) {
                throw TestRepositoryException("Repository error")
            }
            return scenariosToReturn.filter { it.scenario.eventId == eventId }
        }
    }

    class TestRepositoryException(message: String) : Exception(message)

    // ========================================================================
    // Test Helpers
    // ========================================================================

    private fun createScenarioWithVotes(
        scenarioId: String,
        eventId: String,
        preferCount: Int = 0,
        neutralCount: Int = 0,
        againstCount: Int = 0
    ): ScenarioWithVotes {
        val scenario = Scenario(
            id = scenarioId,
            eventId = eventId,
            name = "Test Scenario",
            dateOrPeriod = "March 15-17, 2025",
            location = "Chamonix, France",
            duration = 3,
            estimatedParticipants = 10,
            estimatedBudgetPerPerson = 250.0,
            description = "Test description",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-03-01T10:00:00Z",
            updatedAt = "2025-03-01T10:00:00Z"
        )

        // Create votes
        val votes = mutableListOf<ScenarioVote>()
        repeat(preferCount) { i ->
            votes.add(
                ScenarioVote(
                    id = "vote-prefer-$i",
                    scenarioId = scenarioId,
                    participantId = "user-prefer-$i",
                    vote = ScenarioVoteType.PREFER,
                    createdAt = "2025-03-01T10:00:00Z"
                )
            )
        }
        repeat(neutralCount) { i ->
            votes.add(
                ScenarioVote(
                    id = "vote-neutral-$i",
                    scenarioId = scenarioId,
                    participantId = "user-neutral-$i",
                    vote = ScenarioVoteType.NEUTRAL,
                    createdAt = "2025-03-01T10:00:00Z"
                )
            )
        }
        repeat(againstCount) { i ->
            votes.add(
                ScenarioVote(
                    id = "vote-against-$i",
                    scenarioId = scenarioId,
                    participantId = "user-against-$i",
                    vote = ScenarioVoteType.AGAINST,
                    createdAt = "2025-03-01T10:00:00Z"
                )
            )
        }

        // Create voting result
        val totalVotes = preferCount + neutralCount + againstCount
        val score = (preferCount * 2) + neutralCount - againstCount
        val votingResult = ScenarioVotingResult(
            scenarioId = scenarioId,
            preferCount = preferCount,
            neutralCount = neutralCount,
            againstCount = againstCount,
            totalVotes = totalVotes,
            score = score
        )

        return ScenarioWithVotes(
            scenario = scenario,
            votes = votes,
            votingResult = votingResult
        )
    }

    // ========================================================================
    // Tests
    // ========================================================================

    @Test
    fun testLoadScenarios_Success() {
        // Given
        val eventId = "event-123"
        mockRepository.scenariosToReturn = listOf(
            createScenarioWithVotes(scenarioId = "scenario-1", eventId = eventId),
            createScenarioWithVotes(scenarioId = "scenario-2", eventId = eventId)
        )

        // When
        val result = loadScenariosUseCase(eventId)

        // Then
        assertTrue(result.isSuccess)
        val scenarios = result.getOrNull()
        assertNotNull(scenarios)
        assertEquals(2, scenarios.size)
        assertEquals("scenario-1", scenarios[0].scenario.id)
        assertEquals("scenario-2", scenarios[1].scenario.id)
    }

    @Test
    fun testLoadScenarios_EmptyList() {
        // Given
        val eventId = "event-empty"
        mockRepository.scenariosToReturn = emptyList()

        // When
        val result = loadScenariosUseCase(eventId)

        // Then
        assertTrue(result.isSuccess)
        val scenarios = result.getOrNull()
        assertNotNull(scenarios)
        assertTrue(scenarios.isEmpty())
    }

    @Test
    fun testLoadScenarios_WithVotingResults() {
        // Given
        val eventId = "event-votes"
        val scenarioWithVotes = createScenarioWithVotes(
            scenarioId = "scenario-vote-1",
            eventId = eventId,
            preferCount = 5,
            neutralCount = 3,
            againstCount = 2
        )
        mockRepository.scenariosToReturn = listOf(scenarioWithVotes)

        // When
        val result = loadScenariosUseCase(eventId)

        // Then
        assertTrue(result.isSuccess)
        val scenarios = result.getOrNull()
        assertNotNull(scenarios)
        assertEquals(1, scenarios.size)

        val votingResult = scenarios[0].votingResult
        assertEquals(11, votingResult.score) // (5 * 2) + 3 - 2 = 11
        assertEquals(5, votingResult.preferCount)
        assertEquals(3, votingResult.neutralCount)
        assertEquals(2, votingResult.againstCount)
        assertEquals(10, votingResult.totalVotes)
    }

    @Test
    fun testLoadScenarios_Exception() {
        // Given
        val eventId = "event-error"
        mockRepository.shouldThrowError = true

        // When
        val result = loadScenariosUseCase(eventId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is TestRepositoryException)
        assertEquals("Repository error", exception.message)
    }

    @Test
    fun testLoadScenarios_FilterByEventId() {
        // Given
        val eventId1 = "event-1"
        val eventId2 = "event-2"
        mockRepository.scenariosToReturn = listOf(
            createScenarioWithVotes(scenarioId = "scenario-1", eventId = eventId1),
            createScenarioWithVotes(scenarioId = "scenario-2", eventId = eventId2),
            createScenarioWithVotes(scenarioId = "scenario-3", eventId = eventId1)
        )

        // When - load only event-1
        val result = loadScenariosUseCase(eventId1)

        // Then
        assertTrue(result.isSuccess)
        val scenarios = result.getOrNull()
        assertNotNull(scenarios)
        assertEquals(2, scenarios.size)
        assertEquals("scenario-1", scenarios[0].scenario.id)
        assertEquals("scenario-3", scenarios[1].scenario.id)
        // Verify all scenarios belong to event-1
        scenarios.forEach { scenarioWithVotes ->
            assertEquals(eventId1, scenarioWithVotes.scenario.eventId)
        }
    }

    @Test
    fun testLoadScenarios_MultipleScenarios_WithDifferentVotes() {
        // Given
        val eventId = "event-complex"
        mockRepository.scenariosToReturn = listOf(
            createScenarioWithVotes(
                scenarioId = "scenario-1",
                eventId = eventId,
                preferCount = 8,
                neutralCount = 2,
                againstCount = 0
            ),
            createScenarioWithVotes(
                scenarioId = "scenario-2",
                eventId = eventId,
                preferCount = 3,
                neutralCount = 4,
                againstCount = 3
            ),
            createScenarioWithVotes(
                scenarioId = "scenario-3",
                eventId = eventId,
                preferCount = 0,
                neutralCount = 0,
                againstCount = 0
            )
        )

        // When
        val result = loadScenariosUseCase(eventId)

        // Then
        assertTrue(result.isSuccess)
        val scenarios = result.getOrNull()
        assertNotNull(scenarios)
        assertEquals(3, scenarios.size)

        // Verify scenario 1 (high preference)
        assertEquals(18, scenarios[0].votingResult.score) // (8*2) + 2 - 0 = 18
        assertEquals(10, scenarios[0].votingResult.totalVotes)

        // Verify scenario 2 (balanced)
        assertEquals(7, scenarios[1].votingResult.score) // (3*2) + 4 - 3 = 7
        assertEquals(10, scenarios[1].votingResult.totalVotes)

        // Verify scenario 3 (no votes)
        assertEquals(0, scenarios[2].votingResult.score)
        assertEquals(0, scenarios[2].votingResult.totalVotes)
    }

    @Test
    fun testLoadScenarios_ResultWrapping() {
        // Given
        val eventId = "event-wrap"
        val scenario = createScenarioWithVotes(scenarioId = "scenario-1", eventId = eventId)
        mockRepository.scenariosToReturn = listOf(scenario)

        // When
        val result = loadScenariosUseCase(eventId)

        // Then
        // Verify Result is properly wrapping the value
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun testLoadScenarios_InvokeOperator() {
        // Given
        val eventId = "event-invoke"
        val scenario = createScenarioWithVotes(scenarioId = "scenario-1", eventId = eventId)
        mockRepository.scenariosToReturn = listOf(scenario)

        // When
        // Use invoke operator (operator fun invoke())
        val result = loadScenariosUseCase(eventId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun testLoadScenarios_VotingPercentages() {
        // Given
        val eventId = "event-percentages"
        val scenarioWithVotes = createScenarioWithVotes(
            scenarioId = "scenario-1",
            eventId = eventId,
            preferCount = 5,
            neutralCount = 3,
            againstCount = 2
        )
        mockRepository.scenariosToReturn = listOf(scenarioWithVotes)

        // When
        val result = loadScenariosUseCase(eventId)

        // Then
        assertTrue(result.isSuccess)
        val scenarios = result.getOrNull()
        assertNotNull(scenarios)
        assertEquals(1, scenarios.size)

        val votingResult = scenarios[0].votingResult
        // 5 prefer out of 10 = 50%
        assertTrue(votingResult.preferPercentage in 49.9..50.1)
        // 3 neutral out of 10 = 30%
        assertTrue(votingResult.neutralPercentage in 29.9..30.1)
        // 2 against out of 10 = 20%
        assertTrue(votingResult.againstPercentage in 19.9..20.1)
    }

    @Test
    fun testLoadScenarios_NoVotesScenario() {
        // Given
        val eventId = "event-no-votes"
        val scenarioWithVotes = createScenarioWithVotes(
            scenarioId = "scenario-1",
            eventId = eventId,
            preferCount = 0,
            neutralCount = 0,
            againstCount = 0
        )
        mockRepository.scenariosToReturn = listOf(scenarioWithVotes)

        // When
        val result = loadScenariosUseCase(eventId)

        // Then
        assertTrue(result.isSuccess)
        val scenarios = result.getOrNull()
        assertNotNull(scenarios)
        assertEquals(1, scenarios.size)

        val scenario = scenarios[0]
        assertTrue(scenario.votes.isEmpty())
        assertEquals(0, scenario.votingResult.totalVotes)
        assertEquals(0, scenario.votingResult.score)
        assertEquals(0.0, scenario.votingResult.preferPercentage)
        assertEquals(0.0, scenario.votingResult.neutralPercentage)
        assertEquals(0.0, scenario.votingResult.againstPercentage)
    }
}
