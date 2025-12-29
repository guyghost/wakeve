package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.ScenarioRepository
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioWithVotes

/**
 * Minimal interface for scenario repository operations.
 * Allows use cases to be testable without requiring the full ScenarioRepository.
 */
interface IScenarioRepository {
    fun getScenariosWithVotes(eventId: String): List<ScenarioWithVotes>
}

/**
 * Extended interface for scenario repository with create/update/delete operations.
 * Used by use cases that need write operations.
 */
interface IScenarioRepositoryWrite : IScenarioRepository {
    suspend fun createScenario(scenario: Scenario): Result<Scenario>
    suspend fun updateScenario(scenario: Scenario): Result<Scenario>
    suspend fun deleteScenario(scenarioId: String): Result<Unit>
    suspend fun addVote(vote: ScenarioVote): Result<ScenarioVote>
}

/**
 * Adapter to make ScenarioRepository compatible with IScenarioRepository.
 */
internal class ScenarioRepositoryAdapter(
    private val repository: ScenarioRepository
) : IScenarioRepository {
    override fun getScenariosWithVotes(eventId: String): List<ScenarioWithVotes> {
        return repository.getScenariosWithVotes(eventId)
    }
}

/**
 * Adapter to make ScenarioRepository compatible with IScenarioRepositoryWrite.
 */
internal class ScenarioRepositoryWriteAdapter(
    private val repository: ScenarioRepository
) : IScenarioRepositoryWrite {
    override fun getScenariosWithVotes(eventId: String): List<ScenarioWithVotes> {
        return repository.getScenariosWithVotes(eventId)
    }

    override suspend fun createScenario(scenario: Scenario): Result<Scenario> {
        return repository.createScenario(scenario)
    }

    override suspend fun updateScenario(scenario: Scenario): Result<Scenario> {
        return repository.updateScenario(scenario)
    }

    override suspend fun deleteScenario(scenarioId: String): Result<Unit> {
        return repository.deleteScenario(scenarioId)
    }

    override suspend fun addVote(vote: ScenarioVote): Result<ScenarioVote> {
        return repository.addVote(vote)
    }
}

/**
 * Use case for loading scenarios for an event.
 *
 * This use case retrieves all scenarios for a specific event along with their
 * voting results. It's used by ScenarioManagementStateMachine when handling
 * LoadScenarios intent.
 *
 * ## Usage
 *
 * ```kotlin
 * val loadScenariosUseCase = LoadScenariosUseCase(scenarioRepository)
 * val result: Result<List<ScenarioWithVotes>> = loadScenariosUseCase("event-id")
 * ```
 *
 * @property repository The repository to load scenarios from
 */
class LoadScenariosUseCase(
    private val repository: IScenarioRepository
) {
    /**
     * Alternative constructor accepting ScenarioRepository for production use.
     */
    constructor(scenarioRepository: ScenarioRepository) : this(
        ScenarioRepositoryAdapter(scenarioRepository)
    )

    /**
     * Load all scenarios with votes for an event.
     *
     * @param eventId The event ID
     * @return Result containing list of scenarios with voting results, or failure if operation failed
     */
    operator fun invoke(eventId: String): Result<List<ScenarioWithVotes>> {
        return try {
            val scenarios = repository.getScenariosWithVotes(eventId)
            Result.success(scenarios)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

