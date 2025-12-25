package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

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
    val updatedAt: String // ISO string (UTC)
) {
    init {
        require(name.isNotBlank()) { "Scenario name cannot be blank" }
        require(location.isNotBlank()) { "Location cannot be blank" }
        require(duration > 0) { "Duration must be positive" }
        require(estimatedParticipants > 0) { "Estimated participants must be positive" }
        require(estimatedBudgetPerPerson >= 0.0) { "Budget cannot be negative" }
    }
}

/**
 * Status of a scenario in the voting process.
 */
@Serializable
enum class ScenarioStatus {
    /** Scenario is proposed and open for voting */
    PROPOSED,
    
    /** Scenario has been selected by the organizer */
    SELECTED,
    
    /** Scenario has been rejected and is no longer considered */
    REJECTED
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
data class ScenarioWithVotes(
    val scenario: Scenario,
    val votes: List<ScenarioVote>,
    val votingResult: ScenarioVotingResult
)
