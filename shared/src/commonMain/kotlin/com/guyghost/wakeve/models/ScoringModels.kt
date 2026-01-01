package com.guyghost.wakeve.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Represents a scored date for event recommendation.
 * Contains the ML prediction score along with confidence metrics and feature details.
 *
 * @property date ISO date string for the predicted optimal date
 * @property score Final recommendation score (0.0 - 1.0)
 * @property confidenceScore Confidence level of the prediction (0.0 - 1.0)
 * @property features Map of features used for this prediction (for debugging/analytics)
 * @property breakdown Individual component scores (proximity, typeMatch, seasonality, social)
 */
@Serializable
data class DateScore(
    val date: String,
    val score: Double,
    val confidenceScore: Double,
    @Contextual val features: Map<String, Any> = emptyMap(),
    val breakdown: ScoreBreakdown = ScoreBreakdown()
)

/**
 * Represents a scored location for event recommendation.
 * Contains the ML prediction score along with confidence metrics and feature details.
 *
 * @property locationId Unique identifier of the location
 * @property locationName Display name of the location
 * @property score Final recommendation score (0.0 - 1.0)
 * @property confidenceScore Confidence level of the prediction (0.0 - 1.0)
 * @property features Map of features used for this prediction (for debugging/analytics)
 * @property breakdown Individual component scores (proximity, typeMatch, seasonality, social)
 */
@Serializable
data class LocationScore(
    val locationId: String,
    val locationName: String,
    val score: Double,
    val confidenceScore: Double,
    @Contextual val features: Map<String, Any> = emptyMap(),
    val breakdown: ScoreBreakdown = ScoreBreakdown()
)

/**
 * Prediction result for event attendance.
 * Estimates how many participants are likely to attend based on historical patterns.
 *
 * @property timeSlotId Unique identifier of the time slot
 * @property predictedAttendanceRate Predicted attendance rate (0.0 - 1.0)
 * @property confidenceScore Confidence level of the prediction (0.0 - 1.0)
 * @property predictedCount Estimated number of attendees (calculated from rate * expected)
 */
@Serializable
data class AttendancePrediction(
    val timeSlotId: String,
    val predictedAttendanceRate: Double,
    val confidenceScore: Double,
    val predictedCount: Int = 0
)

/**
 * Individual component scores that make up the final recommendation score.
 * Used for debugging and understanding why a particular recommendation was made.
 *
 * @property proximityScore Score for proximity to preferred dates/times (0.0 - 1.0)
 * @property typeMatchScore Score for matching preferred event types (0.0 - 1.0)
 * @property seasonalityScore Score for seasonal preferences (0.0 - 1.0)
 * @property socialScore Score for social factors like friends attending (0.0 - 1.0)
 */
@Serializable
data class ScoreBreakdown(
    val proximityScore: Double = 0.0,
    val typeMatchScore: Double = 0.0,
    val seasonalityScore: Double = 0.0,
    val socialScore: Double = 0.0
)

/**
 * Enum representing the four seasons for seasonality scoring.
 */
@Serializable
enum class Season {
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER;

    companion object {
        /**
         * Determines the season for a given month (1-12).
         *
         * @param month The month number (1-12)
         * @return The corresponding season
         */
        fun fromMonth(month: Int): Season {
            return when (month) {
                in 3..5 -> SPRING
                in 6..8 -> SUMMER
                in 9..11 -> AUTUMN
                else -> WINTER
            }
        }
    }
}

/**
 * Enum for tracking the source of a prediction score.
 * Used for A/B testing and analytics.
 */
@Serializable
enum class PredictionSource {
    /** Score computed using ML model */
    ML_MODEL,

    /** Score computed using heuristic fallback rules */
    HEURISTIC_FALLBACK,

    /** Score computed using hybrid approach (ML + heuristics) */
    HYBRID
}

/**
 * A/B testing configuration for ML model variants.
 *
 * @property variantName Name identifier for this variant
 * @property mlWeight Weight given to ML predictions (0.0 - 1.0)
 * @property heuristicWeight Weight given to heuristic rules (0.0 - 1.0)
 * @property trafficPercentage Percentage of users assigned to this variant (0.0 - 1.0)
 */
@Serializable
data class ABTestVariant(
    val variantName: String,
    val mlWeight: Double = 0.7,
    val heuristicWeight: Double = 0.3,
    val trafficPercentage: Double = 0.1
)

/**
 * Represents a feature vector for ML model input.
 * This is the internal representation used by the scoring engine.
 *
 * @property dayOfWeek Day of week (0-6, Monday-Sunday)
 * @property timeOfDay Time of day category (0-4)
 * @property eventType Event type category (0-14)
 * @property season Season category (0-3)
 * @property participantCount Number of expected participants
 * @property userPreferredDays Bitmask or list of preferred days
 * @property userPreferredTimeOfDay Bitmask or list of preferred times
 * @property historicalAttendanceRate Historical attendance rate for similar events
 * @property isWeekend Whether the date falls on a weekend
 */
@Serializable
data class FeatureVector(
    val dayOfWeek: Int,
    val timeOfDay: Int,
    val eventType: Int,
    val season: Int,
    val participantCount: Int,
    val userPreferredDays: List<Int>,
    val userPreferredTimeOfDay: List<Int>,
    val historicalAttendanceRate: Double,
    val isWeekend: Boolean
)
