package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

private const val MAX_SCENARIO_NAME_LENGTH = 160
private const val MAX_SCENARIO_DATE_OR_PERIOD_LENGTH = 160
private const val MAX_SCENARIO_LOCATION_LENGTH = 300
private const val MAX_SCENARIO_DESCRIPTION_LENGTH = 2_000
private const val MAX_SCENARIO_DURATION_DAYS = 365
private const val MAX_SCENARIO_ESTIMATED_PARTICIPANTS = 10_000
private const val MAX_SCENARIO_BUDGET_PER_PERSON = 1_000_000.0

/**
 * Represents a planning scenario for an event.
 * A scenario combines date, location, duration and budget estimates
 * to provide different options for participants to vote on.
 */
@Serializable
data class Scenario(
    val id: String,
    val eventId: String,
    val name: String,
    val dateOrPeriod: String, // ISO date or period description
    val location: String,
    val duration: Int, // in days
    val estimatedParticipants: Int,
    val estimatedBudgetPerPerson: Double,
    val description: String,
    val status: ScenarioStatus,
    val createdAt: String, // ISO string (UTC)
    val updatedAt: String, // ISO string (UTC)
    val sourceTimeSlotId: String? = null,
    val sourcePotentialLocationId: String? = null,
    val generationType: ScenarioGenerationType = ScenarioGenerationType.MANUAL
) {
    init {
        require(name.isNotBlank()) { "Scenario name cannot be blank" }
        require(dateOrPeriod.isNotBlank()) { "Scenario date or period cannot be blank" }
        require(location.isNotBlank()) { "Location cannot be blank" }
        require(description.isNotBlank()) { "Scenario description cannot be blank" }
        require(duration > 0) { "Duration must be positive" }
        require(estimatedParticipants > 0) { "Estimated participants must be positive" }
        require(estimatedBudgetPerPerson.isFinite()) { "Budget must be finite" }
        require(estimatedBudgetPerPerson >= 0.0) { "Budget cannot be negative" }
    }

    fun normalized(): Scenario {
        val normalizedName = name.trim()
        val normalizedDateOrPeriod = dateOrPeriod.trim()
        val normalizedLocation = location.trim()
        val normalizedDescription = description.trim()
        val normalizedSourceTimeSlotId = sourceTimeSlotId?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedSourcePotentialLocationId = sourcePotentialLocationId?.trim()?.takeIf { it.isNotEmpty() }

        require(normalizedName.isNotBlank()) { "Scenario name cannot be blank" }
        require(normalizedName.length <= MAX_SCENARIO_NAME_LENGTH) {
            "Scenario name cannot exceed $MAX_SCENARIO_NAME_LENGTH characters"
        }
        require(normalizedDateOrPeriod.isNotBlank()) { "Scenario date or period cannot be blank" }
        require(normalizedDateOrPeriod.length <= MAX_SCENARIO_DATE_OR_PERIOD_LENGTH) {
            "Scenario date or period cannot exceed $MAX_SCENARIO_DATE_OR_PERIOD_LENGTH characters"
        }
        require(normalizedLocation.isNotBlank()) { "Location cannot be blank" }
        require(normalizedLocation.length <= MAX_SCENARIO_LOCATION_LENGTH) {
            "Location cannot exceed $MAX_SCENARIO_LOCATION_LENGTH characters"
        }
        require(normalizedDescription.isNotBlank()) { "Scenario description cannot be blank" }
        require(normalizedDescription.length <= MAX_SCENARIO_DESCRIPTION_LENGTH) {
            "Scenario description cannot exceed $MAX_SCENARIO_DESCRIPTION_LENGTH characters"
        }
        require(duration in 1..MAX_SCENARIO_DURATION_DAYS) {
            "Duration must be between 1 and $MAX_SCENARIO_DURATION_DAYS days"
        }
        require(estimatedParticipants in 1..MAX_SCENARIO_ESTIMATED_PARTICIPANTS) {
            "Estimated participants must be between 1 and $MAX_SCENARIO_ESTIMATED_PARTICIPANTS"
        }
        require(estimatedBudgetPerPerson.isFinite()) { "Budget must be finite" }
        require(estimatedBudgetPerPerson in 0.0..MAX_SCENARIO_BUDGET_PER_PERSON) {
            "Budget must be between 0 and $MAX_SCENARIO_BUDGET_PER_PERSON"
        }

        return copy(
            eventId = eventId.trim(),
            name = normalizedName,
            dateOrPeriod = normalizedDateOrPeriod,
            location = normalizedLocation,
            description = normalizedDescription,
            sourceTimeSlotId = normalizedSourceTimeSlotId,
            sourcePotentialLocationId = normalizedSourcePotentialLocationId
        )
    }
}

/**
 * Status of a scenario in the voting process.
 */
@Serializable
enum class ScenarioStatus {
    /** Scenario is generated or edited by the organizer before publication */
    DRAFT,

    /** Scenario is proposed and open for voting */
    PROPOSED,
    
    /** Scenario has been selected by the organizer */
    SELECTED,
    
    /** Scenario has been rejected and is no longer considered */
    REJECTED
}

@Serializable
enum class ScenarioGenerationType {
    MANUAL,
    MATRIX
}

/**
 * Represents a participant's vote on a scenario.
 */
@Serializable
data class ScenarioVote(
    val id: String,
    val scenarioId: String,
    val participantId: String,
    val vote: ScenarioVoteType,
    val createdAt: String // ISO string (UTC)
)

/**
 * Type of vote for a scenario.
 */
@Serializable
enum class ScenarioVoteType {
    /** Participant prefers this scenario */
    PREFER,
    
    /** Participant is neutral about this scenario */
    NEUTRAL,
    
    /** Participant is against this scenario */
    AGAINST
}

/**
 * Aggregated voting results for a scenario.
 */
@Serializable
data class ScenarioVotingResult(
    val scenarioId: String,
    val preferCount: Int,
    val neutralCount: Int,
    val againstCount: Int,
    val totalVotes: Int,
    val score: Int // preferCount * 2 + neutralCount - againstCount
) {
    val preferPercentage: Double
        get() = if (totalVotes > 0) (preferCount.toDouble() / totalVotes) * 100 else 0.0
    
    val neutralPercentage: Double
        get() = if (totalVotes > 0) (neutralCount.toDouble() / totalVotes) * 100 else 0.0
    
    val againstPercentage: Double
        get() = if (totalVotes > 0) (againstCount.toDouble() / totalVotes) * 100 else 0.0
}

/**
 * Scenario with its associated votes.
 */
@Serializable
data class ScenarioWithVotes(
    val scenario: Scenario,
    val votes: List<ScenarioVote>,
    val votingResult: ScenarioVotingResult
)
