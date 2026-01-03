package com.guyghost.wakeve.suggestions

import com.guyghost.wakeve.models.RecommendationContext
import com.guyghost.wakeve.models.RecommendationResult
import com.guyghost.wakeve.models.RecommendationScore
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScoringWeights
import com.guyghost.wakeve.models.SuggestionBudgetRange
import com.guyghost.wakeve.models.SuggestionSeason
import com.guyghost.wakeve.models.SuggestionUserPreferences
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Engine for calculating recommendation scores and generating personalized suggestions.
 * Uses content-based filtering, collaborative filtering, and hybrid approaches.
 */
object RecommendationEngine {

    /**
     * Calcule le score d'un scénario pour un utilisateur
     */
    fun calculateScenarioScore(
        scenario: Scenario,
        preferences: SuggestionUserPreferences,
        weights: ScoringWeights = ScoringWeights()
    ): RecommendationScore {
        val costScore = calculateCostScore(scenario.estimatedBudgetPerPerson, preferences.budgetRange)
        val accessibilityScore = calculateAccessibilityScore(scenario.location)
        val popularityScore = 0.0 // À implémenter avec stats réelles
        val seasonalityScore = calculateSeasonalityScore(scenario.dateOrPeriod, preferences.preferredSeasons)
        val personalizationScore = calculatePersonalizationScore(scenario, preferences)

        val overallScore = (
            costScore * weights.cost +
            accessibilityScore * weights.accessibility +
            popularityScore * weights.popularity +
            seasonalityScore * weights.seasonality +
            personalizationScore * weights.personalization
        ).coerceIn(0.0, 1.0)

        return RecommendationScore(
            itemId = scenario.id,
            userId = preferences.userId,
            overallScore = overallScore,
            costScore = costScore,
            accessibilityScore = accessibilityScore,
            popularityScore = popularityScore,
            seasonalityScore = seasonalityScore,
            personalizationScore = personalizationScore
        )
    }

    /**
     * Calcule le score de coût (plus c'est bas, mieux c'est)
     */
    private fun calculateCostScore(budgetPerPerson: Double, budgetRange: SuggestionBudgetRange): Double {
        return when {
            budgetPerPerson <= budgetRange.min -> 1.0
            budgetPerPerson <= budgetRange.max -> {
                val range = budgetRange.max - budgetRange.min
                if (range > 0) {
                    1.0 - ((budgetPerPerson - budgetRange.min) / range) * 0.5
                } else 1.0
            }
            budgetPerPerson <= budgetRange.max * 1.5 -> 0.3
            else -> 0.0
        }.coerceIn(0.0, 1.0)
    }

    /**
     * Calcule le score d'accessibilité (distance de transport)
     */
    private fun calculateAccessibilityScore(location: String): Double {
        // Simplifié: utiliser distance moyenne depuis ville de l'utilisateur
        // À implémenter avec API de géocoding
        // Pour l'instant, retourner un score basé sur la popularité du lieu
        return when {
            location.contains("Paris") || location.contains("London") -> 0.9
            location.contains("Rome") || location.contains("Barcelona") -> 0.8
            else -> 0.7
        }
    }

    /**
     * Calcule le score de saisonnalité
     */
    private fun calculateSeasonalityScore(dateOrPeriod: String, preferredSeasons: List<SuggestionSeason>): Double {
        if (preferredSeasons.contains(SuggestionSeason.ALL_YEAR)) return 1.0

        val season = extractSeasonFromDate(dateOrPeriod)
        return if (preferredSeasons.contains(season)) 1.0 else 0.3
    }

    /**
     * Calcule le score de personnalisation (correspondance avec préférences)
     */
    private fun calculatePersonalizationScore(scenario: Scenario, preferences: SuggestionUserPreferences): Double {
        var score = 0.0
        var criteria = 0

        // Vérifier la durée
        if (scenario.duration in preferences.preferredDurationRange) {
            score += 1.0
            criteria++
        }

        // Vérifier la taille de groupe
        if (scenario.estimatedParticipants <= preferences.maxGroupSize) {
            score += 1.0
            criteria++
        }

        // Vérifier les préférences de localisation
        if (preferences.locationPreferences.preferredRegions.any { region ->
            scenario.location.contains(region, ignoreCase = true)
        }) {
            score += 1.0
            criteria++
        }

        return if (criteria > 0) score / criteria else 0.5
    }

    /**
     * Extrait la saison depuis une date
     */
    private fun extractSeasonFromDate(dateOrPeriod: String): SuggestionSeason {
        // Simplifié: analyser le mois depuis ISO 8601 ou description
        val month = try {
            // Essayer de parser comme ISO date
            val instant = Instant.parse(dateOrPeriod)
            instant.toLocalDateTime(TimeZone.UTC).monthNumber
        } catch (e: Exception) {
            // Fallback: analyser le texte
            when {
                dateOrPeriod.contains("winter", ignoreCase = true) ||
                dateOrPeriod.contains("hiver", ignoreCase = true) -> 12
                dateOrPeriod.contains("spring", ignoreCase = true) ||
                dateOrPeriod.contains("printemps", ignoreCase = true) -> 3
                dateOrPeriod.contains("summer", ignoreCase = true) ||
                dateOrPeriod.contains("été", ignoreCase = true) -> 6
                dateOrPeriod.contains("fall", ignoreCase = true) ||
                dateOrPeriod.contains("autumn", ignoreCase = true) ||
                dateOrPeriod.contains("automne", ignoreCase = true) -> 9
                else -> 6 // Default to summer
            }
        }

        return when (month) {
            12, 1, 2 -> SuggestionSeason.WINTER
            3, 4, 5 -> SuggestionSeason.SPRING
            6, 7, 8 -> SuggestionSeason.SUMMER
            9, 10, 11 -> SuggestionSeason.FALL
            else -> SuggestionSeason.SUMMER
        }
    }

    /**
     * Calcule la similarité entre deux utilisateurs (cosine similarity)
     */
    fun calculateUserSimilarity(
        user1: SuggestionUserPreferences,
        user2: SuggestionUserPreferences
    ): Double {
        var similarity = 0.0
        var factors = 0

        // Activités
        val commonActivities = user1.preferredActivities.intersect(user2.preferredActivities.toSet())
        if (commonActivities.isNotEmpty()) {
            similarity += commonActivities.size.toDouble() / kotlin.math.max(
                user1.preferredActivities.size,
                user2.preferredActivities.size
            )
            factors++
        }

        // Saisons
        val commonSeasons = user1.preferredSeasons.intersect(user2.preferredSeasons)
        if (commonSeasons.isNotEmpty()) {
            similarity += commonSeasons.size.toDouble() / kotlin.math.max(
                user1.preferredSeasons.size,
                user2.preferredSeasons.size
            )
            factors++
        }

        // Budget (overlap des ranges)
        val budgetOverlap = calculateBudgetOverlap(user1.budgetRange, user2.budgetRange)
        if (budgetOverlap > 0) {
            similarity += budgetOverlap
            factors++
        }

        // Taille de groupe
        val groupSizeDiff = kotlin.math.abs(user1.maxGroupSize - user2.maxGroupSize).toDouble()
        val maxGroupSize = kotlin.math.max(user1.maxGroupSize, user2.maxGroupSize)
        if (maxGroupSize > 0) {
            similarity += 1.0 - (groupSizeDiff / maxGroupSize)
            factors++
        }

        return if (factors > 0) similarity / factors else 0.0
    }

    /**
     * Calcule l'overlap des ranges de budget
     */
    private fun calculateBudgetOverlap(range1: SuggestionBudgetRange, range2: SuggestionBudgetRange): Double {
        val start = kotlin.math.max(range1.min, range2.min)
        val end = kotlin.math.min(range1.max, range2.max)
        if (start >= end) return 0.0

        val overlap = end - start
        val union = kotlin.math.max(range1.max, range2.max) - kotlin.math.min(range1.min, range2.min)
        return if (union > 0) overlap / union else 0.0
    }

    /**
     * Recommande des éléments basés sur le collaborative filtering
     */
    fun recommendCollaborative(
        userId: String,
        similarUsers: List<String>,
        context: RecommendationContext,
        limit: Int = 10
    ): List<RecommendationResult> {
        // Placeholder: à implémenter avec vraie logique collaborative
        // Idée: trouver les éléments bien notés par les utilisateurs similaires
        return emptyList()
    }

    /**
     * Recommande des éléments basés sur le content (preferences)
     */
    fun recommendContentBased(
        userId: String,
        context: RecommendationContext,
        limit: Int = 10
    ): List<RecommendationResult> {
        // Placeholder: à implémenter avec vraie logique content-based
        // Idée: filtrer les éléments correspondant aux préférences
        return emptyList()
    }

    /**
     * Combine collaborative et content-based avec weighted average
     */
    fun recommendHybrid(
        userId: String,
        similarUsers: List<String>,
        context: RecommendationContext,
        weights: ScoringWeights = ScoringWeights(),
        limit: Int = 10
    ): List<RecommendationResult> {
        val collaborative = recommendCollaborative(userId, similarUsers, context, limit * 2)
        val contentBased = recommendContentBased(userId, context, limit * 2)

        // Fusionner et recalculer les scores
        val combined = (collaborative + contentBased)
            .groupBy { it.itemId }
            .map { (itemId, results) ->
                val collabScore = results.find { it.collaborativeScore > 0 }?.collaborativeScore ?: 0.0
                val contentScore = results.find { it.contentScore > 0 }?.contentScore ?: 0.0
                val combinedScore = (collabScore * 0.4 + contentScore * 0.6).coerceIn(0.0, 1.0)

                results.first().copy(
                    overallScore = combinedScore,
                    collaborativeScore = collabScore,
                    contentScore = contentScore
                )
            }
            .sortedByDescending { it.overallScore }
            .take(limit)

        return combined
    }
}