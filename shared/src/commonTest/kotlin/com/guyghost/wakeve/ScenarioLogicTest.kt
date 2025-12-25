package com.guyghost.wakeve

import com.guyghost.wakeve.models.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScenarioLogicTest {

    private val scenario1 = Scenario(
        id = "scenario-1",
        eventId = "event-1",
        name = "Paris Weekend",
        dateOrPeriod = "2025-12-15/2025-12-17",
        location = "Paris, France",
        duration = 2,
        estimatedParticipants = 5,
        estimatedBudgetPerPerson = 450.0,
        description = "Weekend trip to Paris",
        status = ScenarioStatus.PROPOSED,
        createdAt = "2025-11-20T10:00:00Z",
        updatedAt = "2025-11-20T10:00:00Z"
    )

    private val scenario2 = Scenario(
        id = "scenario-2",
        eventId = "event-1",
        name = "Barcelona Week",
        dateOrPeriod = "2025-12-20/2025-12-27",
        location = "Barcelona, Spain",
        duration = 7,
        estimatedParticipants = 6,
        estimatedBudgetPerPerson = 850.0,
        description = "Week-long trip to Barcelona",
        status = ScenarioStatus.PROPOSED,
        createdAt = "2025-11-20T11:00:00Z",
        updatedAt = "2025-11-20T11:00:00Z"
    )

    private val scenario3 = Scenario(
        id = "scenario-3",
        eventId = "event-1",
        name = "London Short Stay",
        dateOrPeriod = "2025-12-10/2025-12-12",
        location = "London, UK",
        duration = 2,
        estimatedParticipants = 4,
        estimatedBudgetPerPerson = 600.0,
        description = "Short stay in London",
        status = ScenarioStatus.PROPOSED,
        createdAt = "2025-11-20T12:00:00Z",
        updatedAt = "2025-11-20T12:00:00Z"
    )

    @Test
    fun calculateBestScenarioWithPreferMajority() {
        val votes = listOf(
            ScenarioVote("vote-1", "scenario-1", "p1", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-2", "scenario-1", "p2", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-3", "scenario-1", "p3", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-4", "scenario-2", "p1", ScenarioVoteType.NEUTRAL, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-5", "scenario-2", "p2", ScenarioVoteType.AGAINST, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-6", "scenario-2", "p3", ScenarioVoteType.NEUTRAL, "2025-11-21T10:00:00Z")
        )

        val bestScenario = ScenarioLogic.calculateBestScenario(listOf(scenario1, scenario2), votes)

        assertNotNull(bestScenario)
        assertEquals("scenario-1", bestScenario.id)
    }

    @Test
    fun calculateBestScenarioWithMixedVotes() {
        val votes = listOf(
            ScenarioVote("vote-1", "scenario-1", "p1", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-2", "scenario-1", "p2", ScenarioVoteType.NEUTRAL, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-3", "scenario-2", "p1", ScenarioVoteType.NEUTRAL, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-4", "scenario-2", "p2", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-5", "scenario-2", "p3", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z")
        )

        val bestScenario = ScenarioLogic.calculateBestScenario(listOf(scenario1, scenario2), votes)

        assertNotNull(bestScenario)
        // scenario1: PREFER(2) + NEUTRAL(1) = 3
        // scenario2: NEUTRAL(1) + PREFER(2) + PREFER(2) = 5 -> scenario2 should win
        assertEquals("scenario-2", bestScenario.id)
    }

    @Test
    fun getScenarioScoresBreakdown() {
        val votes = listOf(
            ScenarioVote("vote-1", "scenario-1", "p1", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-2", "scenario-1", "p2", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-3", "scenario-1", "p3", ScenarioVoteType.NEUTRAL, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-4", "scenario-2", "p1", ScenarioVoteType.NEUTRAL, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-5", "scenario-2", "p2", ScenarioVoteType.AGAINST, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-6", "scenario-2", "p3", ScenarioVoteType.AGAINST, "2025-11-21T10:00:00Z")
        )

        val results = ScenarioLogic.getScenarioVotingResults(listOf(scenario1, scenario2), votes)

        assertEquals(2, results.size)

        val scenario1Result = results.find { it.scenarioId == "scenario-1" }
        assertNotNull(scenario1Result)
        assertEquals(2, scenario1Result.preferCount)  // p1, p2
        assertEquals(1, scenario1Result.neutralCount)  // p3
        assertEquals(0, scenario1Result.againstCount)
        assertEquals(5, scenario1Result.score)  // 2*2 + 1*1 - 0*1 = 5

        val scenario2Result = results.find { it.scenarioId == "scenario-2" }
        assertNotNull(scenario2Result)
        assertEquals(0, scenario2Result.preferCount)
        assertEquals(1, scenario2Result.neutralCount)  // p1
        assertEquals(2, scenario2Result.againstCount)  // p2, p3
        assertEquals(-1, scenario2Result.score)  // 0*2 + 1*1 - 2*1 = -1
    }

    @Test
    fun getBestScenarioWithScoreDetails() {
        val votes = listOf(
            ScenarioVote("vote-1", "scenario-1", "p1", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-2", "scenario-1", "p2", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-3", "scenario-2", "p1", ScenarioVoteType.NEUTRAL, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-4", "scenario-2", "p2", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-5", "scenario-3", "p1", ScenarioVoteType.AGAINST, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-6", "scenario-3", "p2", ScenarioVoteType.NEUTRAL, "2025-11-21T10:00:00Z")
        )

        val result = ScenarioLogic.getBestScenarioWithScore(
            listOf(scenario1, scenario2, scenario3), 
            votes
        )

        assertNotNull(result)
        val (bestScenario, votingResult) = result
        // scenario-1: p1=PREFER (2) + p2=PREFER (2) = 4 (BEST)
        // scenario-2: p1=NEUTRAL (1) + p2=PREFER (2) = 3
        // scenario-3: p1=AGAINST (-1) + p2=NEUTRAL (1) = 0
        assertEquals("scenario-1", bestScenario.id)
        assertEquals(2, votingResult.preferCount)  // p1, p2
        assertEquals(0, votingResult.neutralCount)
        assertEquals(0, votingResult.againstCount)
        assertEquals(4, votingResult.score)  // 2*2 = 4
    }

    @Test
    fun emptyScenariosListReturnsNull() {
        val bestScenario = ScenarioLogic.calculateBestScenario(emptyList(), emptyList())
        assertNull(bestScenario)
    }

    @Test
    fun allNegativeVotesStillReturnsScenario() {
        val votes = listOf(
            ScenarioVote("vote-1", "scenario-1", "p1", ScenarioVoteType.AGAINST, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-2", "scenario-1", "p2", ScenarioVoteType.AGAINST, "2025-11-21T10:00:00Z")
        )

        val bestScenario = ScenarioLogic.calculateBestScenario(listOf(scenario1), votes)

        assertNotNull(bestScenario)
        assertEquals("scenario-1", bestScenario.id)
    }

    @Test
    fun calculateVotingPercentages() {
        val votes = listOf(
            ScenarioVote("vote-1", "scenario-1", "p1", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-2", "scenario-1", "p2", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-3", "scenario-1", "p3", ScenarioVoteType.NEUTRAL, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-4", "scenario-1", "p4", ScenarioVoteType.AGAINST, "2025-11-21T10:00:00Z")
        )

        val results = ScenarioLogic.getScenarioVotingResults(listOf(scenario1), votes)
        val result = results.first()

        assertEquals(50.0, result.preferPercentage)  // 2/4 = 50%
        assertEquals(25.0, result.neutralPercentage)  // 1/4 = 25%
        assertEquals(25.0, result.againstPercentage)  // 1/4 = 25%
    }

    @Test
    fun rankScenariosByScore() {
        val votes = listOf(
            ScenarioVote("vote-1", "scenario-1", "p1", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-2", "scenario-1", "p2", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-3", "scenario-2", "p1", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-4", "scenario-2", "p2", ScenarioVoteType.NEUTRAL, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-5", "scenario-3", "p1", ScenarioVoteType.NEUTRAL, "2025-11-21T10:00:00Z"),
            ScenarioVote("vote-6", "scenario-3", "p2", ScenarioVoteType.AGAINST, "2025-11-21T10:00:00Z")
        )

        val ranked = ScenarioLogic.rankScenariosByScore(
            listOf(scenario1, scenario2, scenario3), 
            votes
        )

        assertEquals(3, ranked.size)
        // scenario-1: PREFER(2) + PREFER(2) = 4 (1st)
        // scenario-2: PREFER(2) + NEUTRAL(1) = 3 (2nd)
        // scenario-3: NEUTRAL(1) + AGAINST(-1) = 0 (3rd)
        assertEquals("scenario-1", ranked[0].scenario.id)
        assertEquals(4, ranked[0].votingResult.score)
        assertEquals("scenario-2", ranked[1].scenario.id)
        assertEquals(3, ranked[1].votingResult.score)
        assertEquals("scenario-3", ranked[2].scenario.id)
        assertEquals(0, ranked[2].votingResult.score)
    }
}
