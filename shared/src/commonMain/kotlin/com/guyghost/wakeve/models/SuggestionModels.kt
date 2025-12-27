package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Score d'un scénario ou élément pour un utilisateur
 */
@Serializable
data class RecommendationScore(
    val itemId: String,
    val userId: String,
    val overallScore: Double,      // 0.0 à 1.0
    val costScore: Double,         // 0.0 à 1.0 (1 = bon marché)
    val accessibilityScore: Double, // 0.0 à 1.0 (1 = très accessible)
    val popularityScore: Double,    // 0.0 à 1.0 (1 = populaire)
    val seasonalityScore: Double,  // 0.0 à 1.0 (1 = bonne saison)
    val personalizationScore: Double // 0.0 à 1.0 (1 = correspond aux préférences)
)

/**
 * Configuration des poids pour le scoring
 */
@Serializable
data class ScoringWeights(
    val cost: Double = 0.3,          // 30%
    val accessibility: Double = 0.2,    // 20%
    val popularity: Double = 0.1,      // 10%
    val seasonality: Double = 0.15,     // 15%
    val personalization: Double = 0.25   // 25%
)

/**
 * Contexte de recommandation
 */
@Serializable
data class RecommendationContext(
    val eventId: String,
    val userId: String,
    val userPreferences: SuggestionUserPreferences,
    val participantCount: Int,
    val season: SuggestionSeason,
    val dateRange: ClosedRange<kotlinx.datetime.Instant>? = null
)

/**
 * Types de recommandation
 */
enum class SuggestionRecommendationType {
    SCENARIO,        // Recommandations de scénarios
    DESTINATION,      // Destinations potentielles
    ACTIVITY,         // Activités suggérées
    RESTAURANT,       // Restaurants pour les repas
    TRANSPORT,        // Options de transport
    LODGING            // Options de logement
}

/**
 * Saison de l'année
 */
enum class SuggestionSeason {
    WINTER, SPRING, SUMMER, FALL, ALL_YEAR
}

/**
 * Résultat de recommandation
 */
@Serializable
data class RecommendationResult(
    val itemId: String,
    val item: RecommendationItem,
    val overallScore: Double,
    val collaborativeScore: Double = 0.0,
    val contentScore: Double = 0.0,
    val reasons: List<String>
)

/**
 * Item recommandé
 */
@Serializable
data class RecommendationItem(
    val id: String,
    val type: SuggestionRecommendationType,
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Préférences utilisateur pour les recommandations
 */
@Serializable
data class SuggestionUserPreferences(
    val userId: String,
    val budgetRange: SuggestionBudgetRange,
    val preferredDurationRange: ClosedRange<Int>,
    val preferredSeasons: List<SuggestionSeason>,
    val preferredActivities: List<String>,
    val maxGroupSize: Int,
    val locationPreferences: LocationPreferences,
    val accessibilityNeeds: List<String>
)

/**
 * Plage de budget
 */
@Serializable
data class SuggestionBudgetRange(
    val min: Double,
    val max: Double,
    val currency: String
)

/**
 * Préférences de localisation
 */
@Serializable
data class LocationPreferences(
    val preferredRegions: List<String>,
    val maxDistanceFromCity: Int, // km
    val nearbyCities: List<String>
)

/**
 * Données pour A/B testing
 */
@Serializable
data class SuggestionABTest(
    val id: String,
    val name: String,
    val variantA: String,
    val variantB: String,
    val metric: String,
    val createdAt: String,
    val status: ABTestStatus,
    val winner: String? = null
)

enum class ABTestStatus {
    RUNNING,
    COMPLETED,
    CANCELLED
}

/**
 * Résultat de génération de suggestions
 */
sealed class SuggestionResult {
    data class Success(val suggestions: List<RecommendationResult>) : SuggestionResult()
    data class Failure(val error: String) : SuggestionResult()
}

/**
 * Types d'interaction pour tracking
 */
enum class SuggestionInteractionType {
    VIEWED,
    CLICKED,
    DISMISSED,
    ACCEPTED
}