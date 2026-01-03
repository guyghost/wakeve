package com.guyghost.wakeve.suggestions

import com.guyghost.wakeve.models.LocationPreferences
import com.guyghost.wakeve.models.SuggestionBudgetRange
import com.guyghost.wakeve.models.SuggestionInteractionType
import com.guyghost.wakeve.models.SuggestionSeason
import com.guyghost.wakeve.models.SuggestionUserPreferences

/**
 * Interface for managing user preferences specific to suggestions.
 * 
 * This interface defines the contract for suggestion preferences storage,
 * supporting both basic preference management and interaction tracking
 * for recommendation improvement.
 * 
 * Implementations should follow the Imperative Shell pattern,
 * handling I/O operations while delegating pure logic to models.
 */
interface SuggestionPreferencesRepositoryInterface {
    
    /**
     * Get suggestion preferences for a user.
     * Returns null if no preferences exist for the user.
     */
    fun getSuggestionPreferences(userId: String): SuggestionUserPreferences?
    
    /**
     * Save suggestion preferences for a user.
     */
    suspend fun saveSuggestionPreferences(preferences: SuggestionUserPreferences)
    
    /**
     * Update only the budget range for a user.
     */
    suspend fun updateBudgetRange(userId: String, budgetRange: SuggestionBudgetRange)
    
    /**
     * Update preferred duration range for a user.
     */
    suspend fun updateDurationRange(userId: String, durationRange: ClosedRange<Int>)
    
    /**
     * Update preferred seasons for a user.
     */
    suspend fun updatePreferredSeasons(userId: String, seasons: List<SuggestionSeason>)
    
    /**
     * Update preferred activities for a user.
     */
    suspend fun updatePreferredActivities(userId: String, activities: List<String>)
    
    /**
     * Update location preferences for a user.
     */
    suspend fun updateLocationPreferences(userId: String, locationPrefs: LocationPreferences)
    
    /**
     * Update accessibility needs for a user.
     */
    suspend fun updateAccessibilityNeeds(userId: String, needs: List<String>)
    
    /**
     * Delete suggestion preferences for a user.
     */
    suspend fun deleteSuggestionPreferences(userId: String)
    
    /**
     * Track user interaction for improving recommendations.
     */
    fun trackInteraction(
        userId: String,
        suggestionId: String,
        interactionType: SuggestionInteractionType
    )
    
    /**
     * Track user interaction with additional metadata.
     */
    fun trackInteractionWithMetadata(
        userId: String,
        suggestionId: String,
        interactionType: SuggestionInteractionType,
        metadata: Map<String, String>
    )
    
    /**
     * Get interaction history for a user.
     */
    fun getInteractionHistory(userId: String): List<SuggestionInteraction>
    
    /**
     * Get recent interactions for a user since a specific timestamp.
     */
    fun getRecentInteractions(userId: String, sinceTimestamp: String): List<SuggestionInteraction>
    
    /**
     * Get counts of interaction types for a user since a specific timestamp.
     */
    fun getInteractionCountsByType(userId: String, sinceTimestamp: String): Map<SuggestionInteractionType, Long>
    
    /**
     * Get top suggestions since a specific timestamp.
     */
    fun getTopSuggestions(sinceTimestamp: String, limit: Int): List<Pair<String, Long>>
    
    /**
     * Clean up old interactions older than a specific timestamp.
     */
    suspend fun cleanupOldInteractions(olderThanTimestamp: String)
}
