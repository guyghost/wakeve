package com.guyghost.wakeve

import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.models.ScenarioVotingResult
import com.guyghost.wakeve.models.ScenarioWithVotes

/**
 * Business logic for scenario voting and ranking.
 * Similar to PollLogic but for planning scenarios instead of time slots.
 */
object ScenarioLogic {

    /**
     * Calculate the best scenario based on participant votes.
     * Scoring: PREFER = 2 points, NEUTRAL = 1 point, AGAINST = -1 point
     * 
     * @param scenarios List of scenarios to evaluate
     * @param votes List of all votes cast on these scenarios
     * @return The scenario with the highest score, or null if no scenarios provided
     */
    fun calculateBestScenario(
        scenarios: List<Scenario>,
        votes: List<ScenarioVote>
    ): Scenario? {
        if (scenarios.isEmpty()) return null

        val votesByScenario = votes.groupBy { it.scenarioId }

        val scores = scenarios.associateWith { scenario ->
            votesByScenario[scenario.id]?.sumOf { vote ->
                when (vote.vote) {
                    ScenarioVoteType.PREFER -> 2
                    ScenarioVoteType.NEUTRAL -> 1
                    ScenarioVoteType.AGAINST -> -1
                }
            } ?: 0
        }

        return scores.maxByOrNull { it.value }?.key
    }

    /**
     * Get voting results for all scenarios.
     * 
     * @param scenarios List of scenarios to evaluate
     * @param votes List of all votes cast on these scenarios
     * @return List of voting results with counts and scores
     */
    fun getScenarioVotingResults(
        scenarios: List<Scenario>,
        votes: List<ScenarioVote>
    ): List<ScenarioVotingResult> {
        val votesByScenario = votes.groupBy { it.scenarioId }

        return scenarios.map { scenario ->
            val scenarioVotes = votesByScenario[scenario.id] ?: emptyList()

            val preferCount = scenarioVotes.count { it.vote == ScenarioVoteType.PREFER }
            val neutralCount = scenarioVotes.count { it.vote == ScenarioVoteType.NEUTRAL }
            val againstCount = scenarioVotes.count { it.vote == ScenarioVoteType.AGAINST }
            val totalVotes = scenarioVotes.size
            val score = preferCount * 2 + neutralCount * 1 - againstCount * 1

            ScenarioVotingResult(
                scenarioId = scenario.id,
                preferCount = preferCount,
                neutralCount = neutralCount,
                againstCount = againstCount,
                totalVotes = totalVotes,
                score = score
            )
        }
    }

    /**
     * Get the best scenario along with its voting details.
     * 
     * @param scenarios List of scenarios to evaluate
     * @param votes List of all votes cast on these scenarios
     * @return Pair of best scenario and its voting result, or null if no scenarios
     */
    fun getBestScenarioWithScore(
        scenarios: List<Scenario>,
        votes: List<ScenarioVote>
    ): Pair<Scenario, ScenarioVotingResult>? {
        if (scenarios.isEmpty()) return null

        val results = getScenarioVotingResults(scenarios, votes)
        val bestResult = results.maxByOrNull { it.score } ?: return null
        val bestScenario = scenarios.find { it.id == bestResult.scenarioId } ?: return null

        return bestScenario to bestResult
    }

    /**
     * Rank scenarios by their vote scores in descending order.
     * 
     * @param scenarios List of scenarios to rank
     * @param votes List of all votes cast on these scenarios
     * @return List of scenarios with their votes, sorted by score (highest first)
     */
    fun rankScenariosByScore(
        scenarios: List<Scenario>,
        votes: List<ScenarioVote>
    ): List<ScenarioWithVotes> {
        val votesByScenario = votes.groupBy { it.scenarioId }
        val votingResults = getScenarioVotingResults(scenarios, votes)
        val resultsByScenarioId = votingResults.associateBy { it.scenarioId }

        return scenarios
            .map { scenario ->
                val scenarioVotes = votesByScenario[scenario.id] ?: emptyList()
                val votingResult = resultsByScenarioId[scenario.id]
                    ?: ScenarioVotingResult(
                        scenarioId = scenario.id,
                        preferCount = 0,
                        neutralCount = 0,
                        againstCount = 0,
                        totalVotes = 0,
                        score = 0
                    )

                ScenarioWithVotes(
                    scenario = scenario,
                    votes = scenarioVotes,
                    votingResult = votingResult
                )
            }
            .sortedByDescending { it.votingResult.score }
    }
}
