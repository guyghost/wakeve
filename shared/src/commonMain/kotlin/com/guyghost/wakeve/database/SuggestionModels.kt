package com.guyghost.wakeve.database

import kotlinx.serialization.Serializable

/**
 * Preferences for AI-powered recommendations.
 * Stores user preferences like budget, duration, seasons, activities, location.
 */
data class SuggestionPreferences(
    val userId: String,
    val budgetMin: Double,
    val budgetMax: Double,
    val budgetCurrency: String,
    val preferredDurationMin: Int,
    val preferredDurationMax: Int,
    val preferredSeasons: List<SuggestionSeason>,
    val preferredActivities: List<String>,
    val maxGroupSize: Int,
    val preferredRegions: List<String>,
    val maxDistanceFromCity: Int,
    val nearbyCities: List<String>,
    val accessibilityNeeds: List<String>,
    val lastUpdated: String
)

/**
 * Interaction tracking for A/B testing and recommendation improvement.
 */
data class SuggestionInteraction(
    val id: String,
    val userId: String,
    val suggestionId: String,
    val interactionType: SuggestionInteractionType,
    val timestamp: String
)

/**
 * Type of interaction for tracking.
 */
enum class SuggestionInteractionType {
    VIEWED,
    CLICKED,
    DISMISSED,
    ACCEPTED
}

/**
 * Season preference for recommendations.
 */
enum class SuggestionSeason {
    WINTER,
    SPRING,
    SUMMER,
    FALL,
    ALL_YEAR
}

/**
 * Budget range for suggestions.
 */
data class SuggestionBudgetRange(
    val min: Double,
    val max: Double,
    val currency: String
)

/**
 * Location preferences.
 */
data class LocationPreferences(
    val preferredRegions: List<String>,
    val maxDistanceFromCity: Int,
    val nearbyCities: List<String>
)

/**
 * Database result for preference operations.
 */
sealed class SuggestionPreferencesResult {
    data class Success(val preferences: SuggestionPreferences) : SuggestionPreferencesResult()
    data class Error(val message: String) : SuggestionPreferencesResult()
}
