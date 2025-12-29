package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.ScenarioRepository

/**
 * Use case for deleting a scenario.
 *
 * This use case deletes a scenario and cascades the deletion to all related votes.
 * It should only be performed by the event organizer.
 * It's used by ScenarioManagementStateMachine when handling DeleteScenario intent.
 *
 * ## Usage
 *
 * ```kotlin
 * val deleteScenarioUseCase = DeleteScenarioUseCase(scenarioRepository)
 * val result: Result<Unit> = deleteScenarioUseCase("scenario-id")
 * ```
 *
 * @property repository The repository to delete scenarios from
 */
class DeleteScenarioUseCase(
    private val repository: IScenarioRepositoryWrite
) {
    /**
     * Alternative constructor accepting ScenarioRepository for production use.
     */
    constructor(scenarioRepository: ScenarioRepository) : this(
        ScenarioRepositoryWriteAdapter(scenarioRepository)
    )

    /**
     * Delete a scenario by ID.
     *
     * This operation cascades to delete all votes associated with the scenario.
     *
     * @param scenarioId The scenario ID to delete
     * @return Result containing Unit if successful, or failure if operation failed
     */
    suspend operator fun invoke(scenarioId: String): Result<Unit> {
        return try {
            val result = repository.deleteScenario(scenarioId)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
