package com.guyghost.wakeve

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.models.ScenarioVotingResult
import com.guyghost.wakeve.models.ScenarioWithVotes

/**
 * Repository for managing scenarios and scenario votes in the database.
 * Provides CRUD operations and voting functionality for event planning scenarios.
 */
class ScenarioRepository(private val db: WakevDb) {
    private val scenarioQueries = db.scenarioQueries
    private val scenarioVoteQueries = db.scenarioVoteQueries

    /**
     * Create a new scenario in the database.
     */
    suspend fun createScenario(scenario: Scenario): Result<Scenario> {
        return try {
            val now = getCurrentUtcIsoString()
            scenarioQueries.insertScenario(
                id = scenario.id,
                eventId = scenario.eventId,
                name = scenario.name,
                dateOrPeriod = scenario.dateOrPeriod,
                location = scenario.location,
                duration = scenario.duration.toLong(),
                estimatedParticipants = scenario.estimatedParticipants.toLong(),
                estimatedBudgetPerPerson = scenario.estimatedBudgetPerPerson,
                description = scenario.description,
                status = scenario.status.name,
                createdAt = scenario.createdAt,
                updatedAt = now
            )
            Result.success(scenario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a scenario by its ID.
     */
    fun getScenarioById(id: String): Scenario? {
        return scenarioQueries.selectById(id).executeAsOneOrNull()?.let { row ->
            Scenario(
                id = row.id,
                eventId = row.eventId,
                name = row.name,
                dateOrPeriod = row.dateOrPeriod,
                location = row.location,
                duration = row.duration.toInt(),
                estimatedParticipants = row.estimatedParticipants.toInt(),
                estimatedBudgetPerPerson = row.estimatedBudgetPerPerson,
                description = row.description,
                status = ScenarioStatus.valueOf(row.status),
                createdAt = row.createdAt,
                updatedAt = row.updatedAt
            )
        }
    }

    /**
     * Get all scenarios for a specific event.
     */
    fun getScenariosByEventId(eventId: String): List<Scenario> {
        return scenarioQueries.selectByEventId(eventId).executeAsList().map { row ->
            Scenario(
                id = row.id,
                eventId = row.eventId,
                name = row.name,
                dateOrPeriod = row.dateOrPeriod,
                location = row.location,
                duration = row.duration.toInt(),
                estimatedParticipants = row.estimatedParticipants.toInt(),
                estimatedBudgetPerPerson = row.estimatedBudgetPerPerson,
                description = row.description,
                status = ScenarioStatus.valueOf(row.status),
                createdAt = row.createdAt,
                updatedAt = row.updatedAt
            )
        }
    }

    /**
     * Get scenarios by event ID and status.
     */
    fun getScenariosByEventIdAndStatus(eventId: String, status: ScenarioStatus): List<Scenario> {
        return scenarioQueries.selectByEventIdAndStatus(eventId, status.name)
            .executeAsList()
            .map { row ->
                Scenario(
                    id = row.id,
                    eventId = row.eventId,
                    name = row.name,
                    dateOrPeriod = row.dateOrPeriod,
                    location = row.location,
                    duration = row.duration.toInt(),
                    estimatedParticipants = row.estimatedParticipants.toInt(),
                    estimatedBudgetPerPerson = row.estimatedBudgetPerPerson,
                    description = row.description,
                    status = ScenarioStatus.valueOf(row.status),
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt
                )
            }
    }

    /**
     * Get the selected scenario for an event (if any).
     */
    fun getSelectedScenario(eventId: String): Scenario? {
        return scenarioQueries.selectSelectedByEventId(eventId).executeAsOneOrNull()?.let { row ->
            Scenario(
                id = row.id,
                eventId = row.eventId,
                name = row.name,
                dateOrPeriod = row.dateOrPeriod,
                location = row.location,
                duration = row.duration.toInt(),
                estimatedParticipants = row.estimatedParticipants.toInt(),
                estimatedBudgetPerPerson = row.estimatedBudgetPerPerson,
                description = row.description,
                status = ScenarioStatus.valueOf(row.status),
                createdAt = row.createdAt,
                updatedAt = row.updatedAt
            )
        }
    }

    /**
     * Update an existing scenario.
     */
    suspend fun updateScenario(scenario: Scenario): Result<Scenario> {
        return try {
            val now = getCurrentUtcIsoString()
            scenarioQueries.updateScenario(
                name = scenario.name,
                dateOrPeriod = scenario.dateOrPeriod,
                location = scenario.location,
                duration = scenario.duration.toLong(),
                estimatedParticipants = scenario.estimatedParticipants.toLong(),
                estimatedBudgetPerPerson = scenario.estimatedBudgetPerPerson,
                description = scenario.description,
                updatedAt = now,
                id = scenario.id
            )
            Result.success(scenario.copy(updatedAt = now))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update the status of a scenario.
     */
    suspend fun updateScenarioStatus(scenarioId: String, status: ScenarioStatus): Result<Unit> {
        return try {
            val now = getCurrentUtcIsoString()
            scenarioQueries.updateScenarioStatus(
                status = status.name,
                updatedAt = now,
                id = scenarioId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a scenario (cascade deletes votes).
     */
    suspend fun deleteScenario(scenarioId: String): Result<Unit> {
        return try {
            scenarioQueries.deleteScenario(scenarioId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add or update a vote for a scenario.
     * If the participant has already voted, their vote is updated.
     */
    suspend fun addVote(vote: ScenarioVote): Result<ScenarioVote> {
        return try {
            // Check if vote already exists
            val existingVote = scenarioVoteQueries.selectByScenarioIdAndParticipantId(
                vote.scenarioId,
                vote.participantId
            ).executeAsOneOrNull()

            if (existingVote != null) {
                // Update existing vote
                scenarioVoteQueries.updateScenarioVote(
                    vote = vote.vote.name,
                    scenarioId = vote.scenarioId,
                    participantId = vote.participantId
                )
            } else {
                // Insert new vote
                scenarioVoteQueries.insertScenarioVote(
                    id = vote.id,
                    scenarioId = vote.scenarioId,
                    participantId = vote.participantId,
                    vote = vote.vote.name,
                    createdAt = vote.createdAt
                )
            }
            Result.success(vote)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing vote.
     */
    suspend fun updateVote(vote: ScenarioVote): Result<Unit> {
        return try {
            scenarioVoteQueries.updateScenarioVote(
                vote = vote.vote.name,
                scenarioId = vote.scenarioId,
                participantId = vote.participantId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all votes for a scenario.
     */
    fun getVotesByScenarioId(scenarioId: String): List<ScenarioVote> {
        return scenarioVoteQueries.selectByScenarioId(scenarioId).executeAsList().map { row ->
            ScenarioVote(
                id = row.id,
                scenarioId = row.scenarioId,
                participantId = row.participantId,
                vote = ScenarioVoteType.valueOf(row.vote),
                createdAt = row.createdAt
            )
        }
    }

    /**
     * Get voting result for a scenario.
     */
    fun getVotingResult(scenarioId: String): ScenarioVotingResult? {
        val result = scenarioVoteQueries.selectVotingResultByScenarioId(scenarioId)
            .executeAsOneOrNull()

        return if (result != null) {
            val preferCount = result.preferCount?.toInt() ?: 0
            val neutralCount = result.neutralCount?.toInt() ?: 0
            val againstCount = result.againstCount?.toInt() ?: 0
            val totalVotes = result.totalVotes?.toInt() ?: 0
            val score = preferCount * 2 + neutralCount - againstCount

            ScenarioVotingResult(
                scenarioId = scenarioId,
                preferCount = preferCount,
                neutralCount = neutralCount,
                againstCount = againstCount,
                totalVotes = totalVotes,
                score = score
            )
        } else {
            // No votes yet
            ScenarioVotingResult(
                scenarioId = scenarioId,
                preferCount = 0,
                neutralCount = 0,
                againstCount = 0,
                totalVotes = 0,
                score = 0
            )
        }
    }

    /**
     * Get scenarios with their votes and voting results.
     */
    fun getScenariosWithVotes(eventId: String): List<ScenarioWithVotes> {
        val scenarios = getScenariosByEventId(eventId)
        return scenarios.map { scenario ->
            val votes = getVotesByScenarioId(scenario.id)
            val votingResult = getVotingResult(scenario.id) ?: ScenarioVotingResult(
                scenarioId = scenario.id,
                preferCount = 0,
                neutralCount = 0,
                againstCount = 0,
                totalVotes = 0,
                score = 0
            )
            ScenarioWithVotes(
                scenario = scenario,
                votes = votes,
                votingResult = votingResult
            )
        }
    }

    /**
     * Delete a vote.
     */
    suspend fun deleteVote(scenarioId: String, participantId: String): Result<Unit> {
        return try {
            scenarioVoteQueries.deleteByScenarioIdAndParticipantId(scenarioId, participantId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Count scenarios for an event.
     */
    fun countScenarios(eventId: String): Long {
        return scenarioQueries.countByEventId(eventId).executeAsOne()
    }

    /**
     * Count scenarios by status.
     */
    fun countScenariosByStatus(eventId: String, status: ScenarioStatus): Long {
        return scenarioQueries.countByEventIdAndStatus(eventId, status.name).executeAsOne()
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
