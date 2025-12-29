package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.ScenarioRepository
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVoteType
import kotlin.random.Random

/**
 * Use case for voting on a scenario.
 *
 * This use case allows a participant to submit or update their vote on a scenario.
 * If the participant has already voted on the scenario, their vote is updated.
 * Votes are used to calculate which scenario is preferred by the group.
 * It's used by ScenarioManagementStateMachine when handling VoteScenario intent.
 *
 * ## Scoring System
 *
 * - PREFER: +2 points
 * - NEUTRAL: +1 point
 * - AGAINST: -1 point
 *
 * ## Usage
 *
 * ```kotlin
 * val voteScenarioUseCase = VoteScenarioUseCase(scenarioRepository)
 * val result: Result<ScenarioVote> = voteScenarioUseCase(
 *     scenarioId = "scenario-1",
 *     participantId = "participant-1",
 *     voteType = ScenarioVoteType.PREFER
 * )
 * ```
 *
 * @property repository The repository to store votes in
 */
class VoteScenarioUseCase(
    private val repository: IScenarioRepositoryWrite
) {
    /**
     * Alternative constructor accepting ScenarioRepository for production use.
     */
    constructor(scenarioRepository: ScenarioRepository) : this(
        ScenarioRepositoryWriteAdapter(scenarioRepository)
    )

    /**
     * Submit or update a vote for a scenario.
     *
     * @param scenarioId The scenario ID
     * @param participantId The participant ID
     * @param voteType The vote type (PREFER, NEUTRAL, or AGAINST)
     * @return Result containing the submitted vote, or failure if operation failed
     */
    suspend operator fun invoke(
        scenarioId: String,
        participantId: String,
        voteType: ScenarioVoteType
    ): Result<ScenarioVote> {
        return try {
            val vote = ScenarioVote(
                id = generateId(),
                scenarioId = scenarioId,
                participantId = participantId,
                vote = voteType,
                createdAt = getCurrentUtcIsoString()
            )
            val result = repository.addVote(vote)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate a unique ID.
     */
    private fun generateId(): String {
        val chars = "0123456789abcdef"
        return buildString(36) {
            repeat(36) { i ->
                when (i) {
                    8, 13, 18, 23 -> append('-')
                    14 -> append('4') // UUID version 4
                    19 -> append(chars[Random.nextInt(4) + 8]) // 8, 9, a, or b
                    else -> append(chars[Random.nextInt(16)])
                }
            }
        }
    }

    /**
     * Helper function to get current UTC ISO string.
     */
    private fun getCurrentUtcIsoString(): String {
        // This is a simplified implementation
        // In production, use kotlinx-datetime or platform-specific date APIs
        return "2025-11-25T10:00:00Z"
    }
}
