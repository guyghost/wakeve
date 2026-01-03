package com.guyghost.wakeve.suggestions

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.LocationPreferences
import com.guyghost.wakeve.models.SuggestionBudgetRange
import com.guyghost.wakeve.models.SuggestionInteractionType
import com.guyghost.wakeve.models.SuggestionSeason
import com.guyghost.wakeve.models.SuggestionUserPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for managing user preferences specific to suggestions.
 * Adapts from existing UserPreferencesRepository and adds suggestion-specific storage.
 * 
 * This class is part of the Imperative Shell layer, handling I/O operations
 * (SQLite database access) while delegating pure logic to the Functional Core
 * (SuggestionUserPreferences models).
 * 
 * @param database The SQLDelight WakevDb instance for persistence
 * @param json The JSON serializer for complex preference fields
 */
class SuggestionPreferencesRepository(
    private val database: WakevDb,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SuggestionPreferencesRepositoryInterface {

    private val dbRepository: DatabaseSuggestionPreferencesRepository by lazy {
        DatabaseSuggestionPreferencesRepository(database, json)
    }

    /**
     * Get suggestion preferences for a user
     * Falls back to converting from existing preferences if no suggestion-specific prefs exist
     */
    override fun getSuggestionPreferences(userId: String): SuggestionUserPreferences? {
        return dbRepository.getSuggestionPreferences(userId)
    }

    /**
     * Save suggestion preferences for a user
     */
    override suspend fun saveSuggestionPreferences(preferences: SuggestionUserPreferences) {
        dbRepository.saveSuggestionPreferences(preferences)
    }

    /**
     * Update specific preference fields
     */
    override suspend fun updateBudgetRange(userId: String, budgetRange: SuggestionBudgetRange) {
        val existing = getSuggestionPreferences(userId)
        if (existing != null) {
            val updated = existing.copy(budgetRange = budgetRange)
            saveSuggestionPreferences(updated)
        } else {
            // Create new preferences with default values and updated budget
            val newPrefs = SuggestionUserPreferences(
                userId = userId,
                budgetRange = budgetRange,
                preferredDurationRange = 1..7,
                preferredSeasons = listOf(SuggestionSeason.ALL_YEAR),
                preferredActivities = emptyList(),
                maxGroupSize = 10,
                locationPreferences = LocationPreferences(
                    preferredRegions = emptyList(),
                    maxDistanceFromCity = 100,
                    nearbyCities = emptyList()
                ),
                accessibilityNeeds = emptyList()
            )
            saveSuggestionPreferences(newPrefs)
        }
    }

    override suspend fun updateDurationRange(userId: String, durationRange: ClosedRange<Int>) {
        dbRepository.updateDurationRange(userId, durationRange)
    }

    override suspend fun updatePreferredSeasons(userId: String, seasons: List<SuggestionSeason>) {
        val existing = getSuggestionPreferences(userId)
        if (existing != null) {
            dbRepository.updatePreferredSeasons(userId, seasons)
        } else {
            val newPrefs = createDefaultPreferences(userId).copy(preferredSeasons = seasons)
            saveSuggestionPreferences(newPrefs)
        }
    }

    override suspend fun updatePreferredActivities(userId: String, activities: List<String>) {
        val existing = getSuggestionPreferences(userId)
        if (existing != null) {
            dbRepository.updatePreferredActivities(userId, activities)
        } else {
            val newPrefs = createDefaultPreferences(userId).copy(preferredActivities = activities)
            saveSuggestionPreferences(newPrefs)
        }
    }

    override suspend fun updateLocationPreferences(userId: String, locationPrefs: LocationPreferences) {
        val existing = getSuggestionPreferences(userId)
        if (existing != null) {
            dbRepository.updateLocationPreferences(userId, locationPrefs)
        } else {
            val newPrefs = createDefaultPreferences(userId).copy(locationPreferences = locationPrefs)
            saveSuggestionPreferences(newPrefs)
        }
    }

    override suspend fun updateAccessibilityNeeds(userId: String, needs: List<String>) {
        val existing = getSuggestionPreferences(userId)
        if (existing != null) {
            dbRepository.updateAccessibilityNeeds(userId, needs)
        } else {
            val newPrefs = createDefaultPreferences(userId).copy(accessibilityNeeds = needs)
            saveSuggestionPreferences(newPrefs)
        }
    }

    override suspend fun deleteSuggestionPreferences(userId: String) {
        dbRepository.deleteSuggestionPreferences(userId)
    }

    /**
     * Track user interactions for improving recommendations
     */
    override fun trackInteraction(
        userId: String,
        suggestionId: String,
        interactionType: SuggestionInteractionType
    ) {
        dbRepository.trackInteraction(userId, suggestionId, interactionType)
    }

    override fun trackInteractionWithMetadata(
        userId: String,
        suggestionId: String,
        interactionType: SuggestionInteractionType,
        metadata: Map<String, String>
    ) {
        dbRepository.trackInteractionWithMetadata(userId, suggestionId, interactionType, metadata)
    }

    /**
     * Get interaction history for a user
     */
    override fun getInteractionHistory(userId: String): List<SuggestionInteraction> {
        return dbRepository.getInteractionHistory(userId)
    }

    override fun getRecentInteractions(userId: String, sinceTimestamp: String): List<SuggestionInteraction> {
        return dbRepository.getRecentInteractions(userId, sinceTimestamp)
    }

    override fun getInteractionCountsByType(userId: String, sinceTimestamp: String): Map<SuggestionInteractionType, Long> {
        return dbRepository.getInteractionCountsByType(userId, sinceTimestamp)
    }

    override fun getTopSuggestions(sinceTimestamp: String, limit: Int): List<Pair<String, Long>> {
        return dbRepository.getTopSuggestions(sinceTimestamp, limit)
    }

    override suspend fun cleanupOldInteractions(olderThanTimestamp: String) {
        dbRepository.cleanupOldInteractions(olderThanTimestamp)
    }

    /**
     * Creates default preferences for a new user.
     * Provides sensible defaults that can be customized later.
     */
    private fun createDefaultPreferences(userId: String): SuggestionUserPreferences {
        return SuggestionUserPreferences(
            userId = userId,
            budgetRange = SuggestionBudgetRange(
                min = 0.0,
                max = 500.0,
                currency = "EUR"
            ),
            preferredDurationRange = 1..7, // 1 to 7 days
            preferredSeasons = listOf(SuggestionSeason.ALL_YEAR),
            preferredActivities = emptyList(),
            maxGroupSize = 10,
            locationPreferences = LocationPreferences(
                preferredRegions = emptyList(),
                maxDistanceFromCity = 100, // 100 km
                nearbyCities = emptyList()
            ),
            accessibilityNeeds = emptyList()
        )
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