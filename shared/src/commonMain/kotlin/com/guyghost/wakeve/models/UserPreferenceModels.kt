package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Represents user preferences learned from voting history and interactions.
 * Used by the AI recommendation system to provide personalized suggestions.
 *
 * @property userId Unique identifier of the user
 * @property preferredDays List of preferred days of the week (MONDAY, TUESDAY, etc.)
 * @property preferredTimeOfDay List of preferred times of day (MORNING, AFTERNOON, EVENING)
 * @property preferredEventTypes List of preferred event types (BIRTHDAY, WEDDING, etc.)
 * @property preferredLocations List of preferred city/location names
 * @property avoidEvents Boolean flag to avoid certain event types (weddings, funerals, etc.)
 * @property scoreWeights Configuration of weights for recommendation scoring
 * @property lastUpdated ISO 8601 UTC timestamp of last update
 */
@Serializable
data class UserPreference(
    val userId: String,
    val preferredDays: List<DayOfWeek>,
    val preferredTimeOfDay: List<TimeOfDay>,
    val preferredEventTypes: List<EventType>,
    val preferredLocations: List<String>,
    val avoidEvents: Boolean = false,
    val scoreWeights: ScoreWeights = ScoreWeights(),
    val lastUpdated: String
) {
    companion object {
        /**
         * Creates an empty UserPreference for a new user
         */
        fun empty(userId: String): UserPreference = UserPreference(
            userId = userId,
            preferredDays = emptyList(),
            preferredTimeOfDay = emptyList(),
            preferredEventTypes = emptyList(),
            preferredLocations = emptyList(),
            avoidEvents = false,
            scoreWeights = ScoreWeights(),
            lastUpdated = ""
        )
    }
}

/**
 * Configuration of weights for AI recommendation scoring.
 * Controls how different factors influence the final recommendation score.
 *
 * @property proximityWeight Weight for location proximity (default: 0.3)
 * @property typeMatchWeight Weight for event type matching (default: 0.2)
 * @property seasonalityWeight Weight for seasonal preferences (default: 0.3)
 * @property socialWeight Weight for social aspects (default: 0.2)
 * @property totalWeight Sum of all weights (should equal 1.0)
 */
@Serializable
data class ScoreWeights(
    val proximityWeight: Double = 0.3,
    val typeMatchWeight: Double = 0.2,
    val seasonalityWeight: Double = 0.3,
    val socialWeight: Double = 0.2,
    val totalWeight: Double = 1.0
) {
    /**
     * Validates that weights sum to approximately 1.0
     */
    fun isValid(): Boolean {
        val sum = proximityWeight + typeMatchWeight + seasonalityWeight + socialWeight
        return kotlin.math.abs(sum - totalWeight) < 0.001
    }

    companion object {
        /**
         * Default weights optimized for balanced recommendations
         */
        val DEFAULT = ScoreWeights()
    }
}

/**
 * Days of the week for preference tracking.
 * Extracted from time slot voting patterns.
 */
@Serializable
enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    val displayName: String
        get() = when (this) {
            MONDAY -> "Monday"
            TUESDAY -> "Tuesday"
            WEDNESDAY -> "Wednesday"
            THURSDAY -> "Thursday"
            FRIDAY -> "Friday"
            SATURDAY -> "Saturday"
            SUNDAY -> "Sunday"
        }

    val shortName: String
        get() = when (this) {
            MONDAY -> "Mon"
            TUESDAY -> "Tue"
            WEDNESDAY -> "Wed"
            THURSDAY -> "Thu"
            FRIDAY -> "Fri"
            SATURDAY -> "Sat"
            SUNDAY -> "Sun"
        }
}

/**
 * Type of vote for time slots.
 * Used to track user preferences through their voting patterns.
 */
@Serializable
enum class VoteType {
    /** User is available for this time slot */
    YES,

    /** User might be available */
    MAYBE,

    /** User is not available */
    NO
}

/**
 * Type of interaction for preference learning.
 */
@Serializable
enum class InteractionType {
    /** User voted on a time slot */
    VOTE,

    /** User created an event */
    EVENT_CREATION,

    /** User participated in an event */
    PARTICIPATION
}

/**
 * Represents a single user interaction for preference learning.
 * Stored in the database for analytics and decay calculations.
 */
@Serializable
data class PreferenceInteraction(
    val id: String,
    val userId: String,
    val eventId: String,
    val interactionType: InteractionType,
    val eventType: EventType? = null,
    val timeOfDay: TimeOfDay? = null,
    val voteType: VoteType? = null,
    val dayOfWeek: DayOfWeek? = null,
    val location: String? = null,
    val timestamp: String,
    val synced: Boolean = false
)
