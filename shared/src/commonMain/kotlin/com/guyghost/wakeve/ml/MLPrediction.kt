package com.guyghost.wakeve.ml

import kotlinx.serialization.Serializable

/**
 * ML prediction result for recommendation scoring.
 * Contains the predicted outcome along with confidence metrics.
 *
 * @property date ISO date string for the predicted date
 * @property confidenceScore Confidence level of the prediction (0.0 - 1.0)
 * @property predictedAttendance Predicted attendance rate (0.0 - 1.0)
 * @property features Map of features used for this prediction
 * @property modelVersion Version of the ML model used (for A/B testing)
 */
@Serializable
data class MLPrediction(
    val date: String,
    val confidenceScore: Double,
    val predictedAttendance: Double,
    val features: Map<String, String>,
    val modelVersion: String
)

/**
 * Training data point for ML model training and validation.
 * Captures historical event outcomes to learn patterns.
 *
 * @property eventId Unique identifier of the event
 * @property eventDate ISO date string of the event
 * @property eventDayOfWeek Day of week (e.g., "MONDAY", "FRIDAY")
 * @property eventType Type of event (e.g., "BIRTHDAY", "WEDDING")
 * @property participantCount Number of participants invited
 * @property actualAttendance Number of participants who actually attended
 * @property season Season of the event (e.g., "SUMMER", "WINTER")
 */
@Serializable
data class TrainingData(
    val eventId: String,
    val eventDate: String,
    val eventDayOfWeek: String,
    val eventType: String,
    val participantCount: Int,
    val actualAttendance: Int,
    val season: String
)
