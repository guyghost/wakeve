package com.guyghost.wakeve.suggestions

import com.guyghost.wakeve.database.WakevDb
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for managing user preferences for suggestions.
 * Extends the existing UserPreferencesRepository with suggestion-specific functionality.
 */
class SuggestionPreferencesRepository(private val database: WakevDb) {

    private val json = Json { prettyPrint = false }

    /**
     * Get user preferences for suggestions
     */
    fun getSuggestionPreferences(userId: String): UserPreferences? {
        // Try to get from existing preferences and convert
        val existingPrefs = database.userPreferencesQueries.selectPreferencesByUserId(userId).executeAsOneOrNull()

        return if (existingPrefs != null) {
            UserPreferences(
                userId = existingPrefs.user_id,
                budgetRange = BudgetRange(
                    min = when (existingPrefs.budget_range) {
                        "LOW" -> 0.0
                        "MEDIUM" -> 50.0
                        "HIGH" -> 200.0
                        else -> 0.0
                    },
                    max = when (existingPrefs.budget_range) {
                        "LOW" -> 100.0
                        "MEDIUM" -> 300.0
                        "HIGH" -> 1000.0
                        else -> 500.0
                    },
                    currency = "EUR"
                ),
                preferredDurationRange = 1..7, // Default
                preferredSeasons = listOf(Season.ALL_YEAR), // Default
                preferredActivities = json.decodeFromString(existingPrefs.preferred_activities),
                maxGroupSize = existingPrefs.group_size_preference?.toInt() ?: 20,
                locationPreferences = LocationPreferences(
                    preferredRegions = json.decodeFromString(existingPrefs.preferred_locations),
                    maxDistanceFromCity = 500,
                    nearbyCities = emptyList()
                ),
                accessibilityNeeds = emptyList() // Default
            )
        } else {
            // Return default preferences
            getDefaultPreferences(userId)
        }
    }

    /**
     * Update user preferences for suggestions
     */
    fun updateSuggestionPreferences(userId: String, preferences: UserPreferences) {
        // For now, update the existing preferences table
        // In the future, we might want a separate table for suggestion preferences

        val existingPrefs = database.userPreferencesQueries.selectPreferencesByUserId(userId).executeAsOneOrNull()

        val budgetRangeString = when {
            preferences.budgetRange.max <= 100.0 -> "LOW"
            preferences.budgetRange.max <= 300.0 -> "MEDIUM"
            else -> "HIGH"
        }

        if (existingPrefs == null) {
            database.userPreferencesQueries.insertPreferences(
                user_id = userId,
                preferred_days_of_week = json.encodeToString(emptyList<String>()),
                preferred_times = json.encodeToString(emptyList<String>()),
                preferred_locations = json.encodeToString(preferences.locationPreferences.preferredRegions),
                preferred_activities = json.encodeToString(preferences.preferredActivities),
                budget_range = budgetRangeString,
                group_size_preference = preferences.maxGroupSize.toLong(),
                last_updated = kotlinx.datetime.Clock.System.now().toString()
            )
        } else {
            database.userPreferencesQueries.updatePreferences(
                preferred_days_of_week = json.encodeToString(emptyList<String>()),
                preferred_times = json.encodeToString(emptyList<String>()),
                preferred_locations = json.encodeToString(preferences.locationPreferences.preferredRegions),
                preferred_activities = json.encodeToString(preferences.preferredActivities),
                budget_range = budgetRangeString,
                group_size_preference = preferences.maxGroupSize.toLong(),
                last_updated = kotlinx.datetime.Clock.System.now().toString(),
                user_id = userId
            )
        }
    }

    /**
     * Update budget preference
     */
    fun updateBudgetPreference(userId: String, min: Double, max: Double, currency: String) {
        val prefs = getSuggestionPreferences(userId) ?: getDefaultPreferences(userId)
        val updatedPrefs = prefs.copy(
            budgetRange = BudgetRange(min, max, currency)
        )
        updateSuggestionPreferences(userId, updatedPrefs)
    }

    /**
     * Track interaction with suggestions
     */
    fun trackInteraction(
        userId: String,
        suggestionId: String,
        type: SuggestionInteractionType
    ) {
        // For now, just log. In the future, we could store in a dedicated table
        // This would be used for A/B testing and algorithm improvement
        println("User $userId $type suggestion $suggestionId")
    }

    /**
     * Get default preferences
     */
    private fun getDefaultPreferences(userId: String): UserPreferences {
        return UserPreferences(
            userId = userId,
            budgetRange = BudgetRange(0.0, 500.0, "EUR"),
            preferredDurationRange = 1..7,
            preferredSeasons = listOf(Season.ALL_YEAR),
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
}</content>
<parameter name="filePath">shared/src/commonMain/kotlin/com/guyghost/wakeve/suggestions/UserPreferencesRepository.kt