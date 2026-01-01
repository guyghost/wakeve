package com.guyghost.wakeve.services

import com.guyghost.wakeve.ml.MLPrediction
import com.guyghost.wakeve.ml.UserPreference

/**
 * Result type for recommendation operations.
 * Provides type-safe success/error handling.
 */
sealed class RecommendationResult<out T> {
    /** Successful operation with data */
    data class Success<T>(val data: T) : RecommendationResult<T>()

    /** Failed operation with exception */
    data class Error(val exception: Exception) : RecommendationResult<Nothing>()
}

/**
 * Interface for ML-based recommendation engine.
 * Provides predictive recommendations using machine learning models.
 *
 * This interface is implemented on each platform (Android/iOS) with:
 * - On-device inference using TensorFlow Lite for fast responses
 * - Fallback to heuristic rules when ML confidence is low (< 70%)
 * - A/B testing support for model variants
 */
interface RecommendationEngine {
    /**
     * Get personalized ML predictions for an event.
     *
     * @param eventId ID of the event to get recommendations for
     * @param numRecommendations Number of recommendations to return (default: 5)
     * @return List of ML predictions sorted by confidence score
     */
    suspend fun getRecommendations(
        eventId: String,
        numRecommendations: Int = 5
    ): RecommendationResult<List<MLPrediction>>

    /**
     * Update user preferences for personalized recommendations.
     * Preferences are learned both explicitly and implicitly from user behavior.
     *
     * @param userId ID of the user
     * @param preferences New or updated preferences
     */
    suspend fun updateUserPreferences(
        userId: String,
        preferences: UserPreference
    ): RecommendationResult<Unit>

    /**
     * Get stored user preferences.
     *
     * @param userId ID of the user
     * @return User preferences or null if not set
     */
    suspend fun getUserPreferences(
        userId: String
    ): RecommendationResult<UserPreference?>

    /**
     * Record user feedback on a recommendation for model improvement.
     * Feedback is used for retraining the ML model.
     *
     * @param eventId ID of the event
     * @param date Date that was recommended
     * @param accepted Whether the user accepted the recommendation
     * @param userRating Optional user rating (1-5)
     */
    suspend fun recordFeedback(
        eventId: String,
        date: String,
        accepted: Boolean,
        userRating: Int?
    ): RecommendationResult<Unit>
}
