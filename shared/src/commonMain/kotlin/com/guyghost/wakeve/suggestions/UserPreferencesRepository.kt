package com.guyghost.wakeve.suggestions

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.models.LocationPreferences
import com.guyghost.wakeve.models.SuggestionBudgetRange
import com.guyghost.wakeve.models.SuggestionInteractionType
import com.guyghost.wakeve.models.SuggestionSeason
import com.guyghost.wakeve.models.SuggestionUserPreferences

/**
 * Repository for managing user preferences specific to suggestions.
 * Adapts from existing UserPreferencesRepository and adds suggestion-specific storage.
 */
class SuggestionPreferencesRepository(
    private val databaseRepository: DatabaseEventRepository
) {

    /**
     * Get suggestion preferences for a user
     * Falls back to converting from existing preferences if no suggestion-specific prefs exist
     */
    fun getSuggestionPreferences(userId: String): SuggestionUserPreferences? {
        // TODO: Implement actual database storage for suggestion preferences
        // For now, return null to trigger fallback in SuggestionService
        return null
    }

    /**
     * Save suggestion preferences for a user
     */
    fun saveSuggestionPreferences(preferences: SuggestionUserPreferences) {
        // TODO: Implement persistence in database
        // This would store suggestion-specific preferences separately from general user prefs
    }

    /**
     * Update specific preference fields
     */
    fun updateBudgetRange(userId: String, budgetRange: SuggestionBudgetRange) {
        val existing = getSuggestionPreferences(userId)
        if (existing != null) {
            val updated = existing.copy(budgetRange = budgetRange)
            saveSuggestionPreferences(updated)
        }
    }

    fun updatePreferredActivities(userId: String, activities: List<String>) {
        val existing = getSuggestionPreferences(userId)
        if (existing != null) {
            val updated = existing.copy(preferredActivities = activities)
            saveSuggestionPreferences(updated)
        }
    }

    fun updatePreferredSeasons(userId: String, seasons: List<SuggestionSeason>) {
        val existing = getSuggestionPreferences(userId)
        if (existing != null) {
            val updated = existing.copy(preferredSeasons = seasons)
            saveSuggestionPreferences(updated)
        }
    }

    fun updateLocationPreferences(userId: String, locationPrefs: LocationPreferences) {
        val existing = getSuggestionPreferences(userId)
        if (existing != null) {
            val updated = existing.copy(locationPreferences = locationPrefs)
            saveSuggestionPreferences(updated)
        }
    }

    /**
     * Track user interactions for improving recommendations
     */
    fun trackInteraction(userId: String, suggestionId: String, interactionType: SuggestionInteractionType) {
        // TODO: Store interaction data for A/B testing and recommendation improvement
    }

    /**
     * Get interaction history for a user
     */
    fun getInteractionHistory(userId: String): List<SuggestionInteraction> {
        // TODO: Retrieve interaction history from database
        return emptyList()
    }
}

/**
 * Data class for tracking suggestion interactions
 */
data class SuggestionInteraction(
    val userId: String,
    val suggestionId: String,
    val interactionType: SuggestionInteractionType,
    val timestamp: String,
    val metadata: Map<String, String> = emptyMap()
)