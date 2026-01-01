package com.guyghost.wakeve.ml

import kotlinx.serialization.Serializable

/**
 * User preference model for ML-based recommendations.
 * Tracks explicit and implicit preferences learned from user behavior.
 *
 * @property userId Unique identifier for the user
 * @property preferredDays List of preferred days of the week (e.g., MONDAY, FRIDAY)
 * @property preferredTimeOfDay List of preferred time slots (e.g., MORNING, AFTERNOON)
 * @property preferredEventTypes List of preferred event types (e.g., BIRTHDAY, WEDDING)
 * @property preferredLocations List of preferred location names or regions
 * @property avoidEvents Whether the user wants to avoid certain event types
 * @property scoreWeights Configuration for how different factors weigh in scoring
 * @property lastUpdated ISO timestamp of last preference update
 */
@Serializable
data class UserPreference(
    val userId: String,
    val preferredDays: List<String>,
    val preferredTimeOfDay: List<String>,
    val preferredEventTypes: List<String>,
    val preferredLocations: List<String>,
    val avoidEvents: Boolean = false,
    val scoreWeights: ScoreWeights,
    val lastUpdated: String
)

/**
 * Configuration for how different factors weigh in the recommendation scoring algorithm.
 * All weights should sum to 1.0 for optimal results.
 *
 * @property proximityWeight Weight for being close to preferred dates (default: 0.3)
 * @property typeMatchWeight Weight for matching preferred event types (default: 0.2)
 * @property seasonalityWeight Weight for seasonal preferences (default: 0.3)
 * @property socialWeight Weight for social factors like friends attending (default: 0.2)
 * @property totalWeight Sum of all weights (default: 1.0)
 */
@Serializable
data class ScoreWeights(
    val proximityWeight: Double = 0.3,
    val typeMatchWeight: Double = 0.2,
    val seasonalityWeight: Double = 0.3,
    val socialWeight: Double = 0.2,
    val totalWeight: Double = 1.0
)
