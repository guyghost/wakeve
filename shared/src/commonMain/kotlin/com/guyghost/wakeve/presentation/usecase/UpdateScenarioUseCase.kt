package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.ScenarioRepository
import com.guyghost.wakeve.models.Scenario

/**
 * Use case for updating an existing scenario.
 *
 * This use case updates the details of an existing scenario. It preserves the
 * original creation timestamp and auto-updates the modification timestamp.
 * It's used by ScenarioManagementStateMachine when handling UpdateScenario intent.
 *
 * ## Usage
 *
 * ```kotlin
 * val updateScenarioUseCase = UpdateScenarioUseCase(scenarioRepository)
 * val scenario = // ... get existing scenario ...
 * val updated = scenario.copy(
 *     name = "Updated Beach Weekend",
 *     location = "Bahamas"
 * )
 * val result: Result<Scenario> = updateScenarioUseCase(updated)
 * ```
 *
 * @property repository The repository to update scenarios in
 */
class UpdateScenarioUseCase(
    private val repository: IScenarioRepositoryWrite
) {
    /**
     * Alternative constructor accepting ScenarioRepository for production use.
     */
    constructor(scenarioRepository: ScenarioRepository) : this(
        ScenarioRepositoryWriteAdapter(scenarioRepository)
    )

    /**
     * Update an existing scenario.
     *
     * @param scenario The scenario with updated fields
     * @return Result containing the updated scenario, or failure if operation failed
     */
    suspend operator fun invoke(scenario: Scenario): Result<Scenario> {
        return try {
            val result = repository.updateScenario(scenario)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
