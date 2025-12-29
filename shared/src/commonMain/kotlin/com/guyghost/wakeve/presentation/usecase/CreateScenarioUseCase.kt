package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.ScenarioRepository
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import kotlin.random.Random

/**
 * Use case for creating a new scenario.
 *
 * This use case creates a new scenario for an event with the provided details.
 * The scenario is initialized with PROPOSED status and auto-generated timestamps.
 * It's used by ScenarioManagementStateMachine when handling CreateScenario intent.
 *
 * ## Usage
 *
 * ```kotlin
 * val createScenarioUseCase = CreateScenarioUseCase(scenarioRepository)
 * val result: Result<Scenario> = createScenarioUseCase(
 *     name = "Beach Weekend",
 *     eventId = "event-1",
 *     dateOrPeriod = "2025-12-20 to 2025-12-22",
 *     location = "Maldives",
 *     duration = 3,
 *     estimatedParticipants = 8,
 *     estimatedBudgetPerPerson = 1500.0,
 *     description = "Relaxing beach vacation"
 * )
 * ```
 *
 * @property repository The repository to create scenarios in
 */
class CreateScenarioUseCase(
    private val repository: IScenarioRepositoryWrite
) {
    /**
     * Alternative constructor accepting ScenarioRepository for production use.
     */
    constructor(scenarioRepository: ScenarioRepository) : this(
        ScenarioRepositoryWriteAdapter(scenarioRepository)
    )
    /**
     * Create a new scenario.
     *
     * @param name Scenario name
     * @param eventId Parent event ID
     * @param dateOrPeriod Date or period description
     * @param location Location/destination
     * @param duration Duration in days
     * @param estimatedParticipants Estimated number of participants
     * @param estimatedBudgetPerPerson Budget per person in event currency
     * @param description Scenario description
     * @return Result containing the created scenario, or failure if operation failed
     */
    suspend operator fun invoke(
        name: String,
        eventId: String,
        dateOrPeriod: String,
        location: String,
        duration: Int,
        estimatedParticipants: Int,
        estimatedBudgetPerPerson: Double,
        description: String = ""
    ): Result<Scenario> {
        return try {
            val scenario = Scenario(
                id = generateId(),
                eventId = eventId,
                name = name,
                dateOrPeriod = dateOrPeriod,
                location = location,
                duration = duration,
                estimatedParticipants = estimatedParticipants,
                estimatedBudgetPerPerson = estimatedBudgetPerPerson,
                description = description,
                status = ScenarioStatus.PROPOSED,
                createdAt = getCurrentUtcIsoString(),
                updatedAt = getCurrentUtcIsoString()
            )
            val result = repository.createScenario(scenario)
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
