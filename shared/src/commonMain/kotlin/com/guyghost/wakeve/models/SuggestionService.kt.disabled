package com.guyghost.wakeve.models

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.ScenarioRepository
import com.guyghost.wakeve.UserPreferencesRepository as ExistingUserPreferencesRepository
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.suggestions.RecommendationEngine
import kotlinx.datetime.*

/**
 * Service for generating personalized event suggestions.
 * Integrates with RecommendationEngine and various repositories.
 */
class SuggestionService(
    private val eventRepository: DatabaseEventRepository,
    private val scenarioRepository: ScenarioRepository,
    private val userPreferencesRepository: ExistingUserPreferencesRepository,
    private val recommendationEngine: RecommendationEngine
) {

    /**
     * Génère des suggestions pour un événement
     */
    suspend fun generateSuggestionsForEvent(
        eventId: String,
        userId: String,
        types: List<SuggestionRecommendationType> = SuggestionRecommendationType.values().toList()
    ): SuggestionResult {
        val event = eventRepository.getEvent(eventId) ?: return SuggestionResult.Failure("Event not found")

        val participants = eventRepository.getParticipants(eventId)
        val preferences = getUserPreferences(userId)

        val context = RecommendationContext(
            eventId = eventId,
            userId = userId,
            userPreferences = preferences,
            participantCount = participants?.size ?: 0,
            season = extractSeasonFromEvent(event),
            dateRange = extractDateRange(event)
        )

        val suggestions = mutableListOf<RecommendationResult>()

        // Générer les suggestions par type
        if (SuggestionRecommendationType.SCENARIO in types) {
            suggestions.addAll(generateScenarioSuggestions(context))
        }

        if (SuggestionRecommendationType.ACTIVITY in types) {
            suggestions.addAll(generateActivitySuggestions(context))
        }

        if (SuggestionRecommendationType.RESTAURANT in types) {
            suggestions.addAll(generateRestaurantSuggestions(context))
        }

        // Sortir par score
        val sortedSuggestions = suggestions.sortedByDescending { it.overallScore }

        return SuggestionResult.Success(sortedSuggestions)
    }

    /**
     * Génère des suggestions de scénarios
     */
    private suspend fun generateScenarioSuggestions(
        context: RecommendationContext
    ): List<RecommendationResult> {
        val existingScenarios = scenarioRepository.getScenariosByEventId(context.eventId)

        // Récupérer les scénarios suggérés (mockés ou depuis un provider)
        val suggestedScenarios = fetchSuggestedScenarios(context)

        // Calculer les scores pour chaque scénario
        val scored = suggestedScenarios.map { scenario ->
            val score = recommendationEngine.calculateScenarioScore(
                scenario,
                context.userPreferences
            )

            RecommendationResult(
                itemId = scenario.id,
                item = RecommendationItem(
                    id = scenario.id,
                    type = SuggestionRecommendationType.SCENARIO,
                    name = scenario.name,
                    description = scenario.description,
                    metadata = mapOf(
                        "location" to scenario.location,
                        "duration" to scenario.duration.toString(),
                        "budgetPerPerson" to scenario.estimatedBudgetPerPerson.toString()
                    )
                ),
                overallScore = score.overallScore,
                collaborativeScore = 0.0,
                contentScore = score.overallScore,
                reasons = generateReasons(score)
            )
        }

        return scored
    }

    /**
     * Génère des suggestions d'activités
     */
    private suspend fun generateActivitySuggestions(
        context: RecommendationContext
    ): List<RecommendationResult> {
        // Basé sur les préférences d'activités de l'utilisateur
        val preferredActivities = context.userPreferences.preferredActivities

        val activities = fetchActivitiesForEvent(context)

        return activities
            .filter { it.category in preferredActivities || preferredActivities.contains("any") }
            .map { activity ->
                val score = calculateActivityScore(activity, context)

                RecommendationResult(
                    itemId = activity.id,
                    item = RecommendationItem(
                        id = activity.id,
                        type = SuggestionRecommendationType.ACTIVITY,
                        name = activity.name,
                        description = activity.description,
                        imageUrl = activity.imageUrl,
                        metadata = mapOf(
                            "category" to activity.category,
                            "duration" to activity.duration.toString(),
                            "cost" to activity.cost.toString()
                        )
                    ),
                    overallScore = score,
                    collaborativeScore = score * 0.3,
                    contentScore = score * 0.7,
                    reasons = listOf("Match avec vos préférences d'activités")
                )
            }
            .sortedByDescending { it.overallScore }
            .take(10)
    }

    /**
     * Génère des suggestions de restaurants
     */
    private suspend fun generateRestaurantSuggestions(
        context: RecommendationContext
    ): List<RecommendationResult> {
        val restaurants = fetchRestaurants(context)

        return restaurants.map { restaurant ->
            val score = calculateRestaurantScore(restaurant, context)

            RecommendationResult(
                itemId = restaurant.id,
                item = RecommendationItem(
                    id = restaurant.id,
                    type = SuggestionRecommendationType.RESTAURANT,
                    name = restaurant.name,
                    description = restaurant.cuisine,
                    imageUrl = restaurant.imageUrl,
                    metadata = mapOf(
                        "cuisine" to restaurant.cuisine,
                        "priceRange" to restaurant.priceRange,
                        "rating" to restaurant.rating.toString()
                    )
                ),
                overallScore = score,
                collaborativeScore = score * 0.4,
                contentScore = score * 0.6,
                reasons = listOf("Populaire dans la région", "Correspond à vos préférences culinaires")
            )
        }
        .sortedByDescending { it.overallScore }
        .take(5)
    }

    /**
     * Track l'interaction d'un utilisateur avec une suggestion
     */
    suspend fun trackSuggestionInteraction(
        userId: String,
        suggestionId: String,
        interactionType: SuggestionInteractionType
    ) {
        // Enregistrer l'interaction pour A/B testing et amélioration
        // À implémenter avec vraie persistance
    }

    /**
     * Génère les raisons d'une recommandation (explicabilité)
     */
    private fun generateReasons(score: RecommendationScore): List<String> {
        val reasons = mutableListOf<String>()

        if (score.costScore > 0.7) {
            reasons.add("Dans votre budget")
        }

        if (score.accessibilityScore > 0.7) {
            reasons.add("Facile d'accès depuis votre ville")
        }

        if (score.seasonalityScore > 0.7) {
            reasons.add("Bonne saison pour cette destination")
        }

        if (score.personalizationScore > 0.7) {
            reasons.add("Correspond à vos préférences")
        }

        return reasons.ifEmpty { listOf("Recommandation personnalisée") }
    }

    // Helpers
    private fun extractSeasonFromEvent(event: Event): SuggestionSeason {
        // Extraire depuis proposedSlots ou date
        // Placeholder
        return SuggestionSeason.SUMMER
    }

    private fun extractDateRange(event: Event): ClosedRange<Instant>? {
        // Extraire depuis proposedSlots
        // Placeholder
        return null
    }

    private fun getUserPreferences(userId: String): SuggestionUserPreferences {
        // Adapter depuis l'ancien modèle
        val existingPrefs = userPreferencesRepository.getPreferences(userId)
        return if (existingPrefs != null) {
            // Convertir
            SuggestionUserPreferences(
                userId = existingPrefs.userId,
                budgetRange = SuggestionBudgetRange(
                    min = when (existingPrefs.budgetRange) {
                        com.guyghost.wakeve.models.BudgetRange.LOW -> 0.0
                        com.guyghost.wakeve.models.BudgetRange.MEDIUM -> 50.0
                        com.guyghost.wakeve.models.BudgetRange.HIGH -> 200.0
                        else -> 0.0
                    },
                    max = when (existingPrefs.budgetRange) {
                        com.guyghost.wakeve.models.BudgetRange.LOW -> 100.0
                        com.guyghost.wakeve.models.BudgetRange.MEDIUM -> 300.0
                        com.guyghost.wakeve.models.BudgetRange.HIGH -> 1000.0
                        else -> 500.0
                    },
                    currency = "EUR"
                ),
                preferredDurationRange = 1..7,
                preferredSeasons = listOf(SuggestionSeason.ALL_YEAR),
                preferredActivities = existingPrefs.preferredActivities,
                maxGroupSize = existingPrefs.groupSizePreference?.toInt() ?: 20,
                locationPreferences = LocationPreferences(
                    preferredRegions = existingPrefs.preferredLocations,
                    maxDistanceFromCity = 500,
                    nearbyCities = emptyList()
                ),
                accessibilityNeeds = emptyList()
            )
        } else {
            // Valeurs par défaut
            SuggestionUserPreferences(
                userId = userId,
                budgetRange = SuggestionBudgetRange(0.0, 500.0, "EUR"),
                preferredDurationRange = 1..7,
                preferredSeasons = listOf(SuggestionSeason.ALL_YEAR),
                preferredActivities = listOf("any"),
                maxGroupSize = 20,
                locationPreferences = LocationPreferences(
                    preferredRegions = emptyList(),
                    maxDistanceFromCity = 500,
                    nearbyCities = emptyList()
                ),
                accessibilityNeeds = emptyList()
            )
        }
    }

    private fun calculateRestaurantScore(restaurant: RestaurantItem, context: RecommendationContext): Double {
        var score = restaurant.rating / 5.0 // Normaliser rating

        // Bonus pour haute note
        if (restaurant.rating >= 4.5) {
            score += 0.1
        }

        // Ajuster selon budget
        if (restaurant.priceRange == "$") {
            if (context.userPreferences.budgetRange.max < 50) score += 0.1
        } else if (restaurant.priceRange == "$$") {
            if (context.userPreferences.budgetRange.max >= 50.0 && context.userPreferences.budgetRange.max <= 200.0) score += 0.1
        } else if (restaurant.priceRange == "$$$") {
            if (context.userPreferences.budgetRange.max > 200) score += 0.1
        }

        return score.coerceIn(0.0, 1.0)
    }

    private fun calculateActivityScore(activity: ActivityItem, context: RecommendationContext): Double {
        var score = 0.5

        // Bonus si l'activité correspond aux préférences
        if (context.userPreferences.preferredActivities.contains(activity.category)) {
            score += 0.3
        }

        // Pénalité si coût élevé
        if (activity.cost > context.userPreferences.budgetRange.max * 0.2) {
            score -= 0.2
        }

        return score.coerceIn(0.0, 1.0)
    }

    // Mock data classes for providers (to be replaced with real implementations)
    private data class ActivityItem(
        val id: String,
        val name: String,
        val description: String,
        val category: String,
        val duration: Int,
        val cost: Double,
        val imageUrl: String? = null
    )

    private data class RestaurantItem(
        val id: String,
        val name: String,
        val cuisine: String,
        val priceRange: String,
        val rating: Double,
        val imageUrl: String? = null
    )

    // Mock fetch functions (to be replaced with real providers)
    private fun fetchSuggestedScenarios(context: RecommendationContext): List<Scenario> {
        // Mock scenarios based on preferences
        return listOf(
            Scenario(
                id = "scenario-1",
                eventId = context.eventId,
                name = "Weekend à Paris",
                dateOrPeriod = "2025-06-15",
                location = "Paris, France",
                duration = 2,
                estimatedParticipants = context.participantCount,
                estimatedBudgetPerPerson = 150.0,
                description = "Un weekend culturel dans la Ville Lumière",
                status = com.guyghost.wakeve.models.ScenarioStatus.PROPOSED,
                createdAt = Clock.System.now().toString(),
                updatedAt = Clock.System.now().toString()
            ),
            Scenario(
                id = "scenario-2",
                eventId = context.eventId,
                name = "Randonnée dans les Alpes",
                dateOrPeriod = "2025-07-20",
                location = "Chamonix, France",
                duration = 3,
                estimatedParticipants = context.participantCount,
                estimatedBudgetPerPerson = 200.0,
                description = "Escapade en montagne avec randonnées",
                status = com.guyghost.wakeve.models.ScenarioStatus.PROPOSED,
                createdAt = Clock.System.now().toString(),
                updatedAt = Clock.System.now().toString()
            )
        )
    }

    private fun fetchActivitiesForEvent(context: RecommendationContext): List<ActivityItem> {
        // Mock activities
        return listOf(
            ActivityItem(
                id = "activity-1",
                name = "Visite du Louvre",
                description = "Découverte des chefs-d'œuvre du musée",
                category = "culture",
                duration = 180,
                cost = 25.0
            ),
            ActivityItem(
                id = "activity-2",
                name = "Randonnée en montagne",
                description = "Balade guidée dans les Alpes",
                category = "sports",
                duration = 240,
                cost = 0.0
            )
        )
    }

    private fun fetchRestaurants(context: RecommendationContext): List<RestaurantItem> {
        // Mock restaurants
        return listOf(
            RestaurantItem(
                id = "restaurant-1",
                name = "Le Petit Bistro",
                cuisine = "Française",
                priceRange = "$$",
                rating = 4.5
            ),
            RestaurantItem(
                id = "restaurant-2",
                name = "Pizza Roma",
                cuisine = "Italienne",
                priceRange = "$",
                rating = 4.2
            )
        )
    }
}