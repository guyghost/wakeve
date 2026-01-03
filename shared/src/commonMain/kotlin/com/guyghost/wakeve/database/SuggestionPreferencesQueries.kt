package com.guyghost.wakeve.database

import kotlinx.coroutines.flow.Flow

/**
 * SQLDelight queries for suggestion preferences.
 */
interface SuggestionPreferencesQueries {
    /**
     * Get suggestion preferences for a user.
     */
    fun selectPreferencesByUserId(userId: String): Query<SuggestionPreferences>

    /**
     * Insert or replace preferences for a user.
     */
    fun insertOrReplacePreferences(
        user_id: String,
        budget_min: Double,
        budget_max: Double,
        budget_currency: String,
        preferred_duration_min: Int,
        preferred_duration_max: Int,
        preferred_seasons: String,
        preferred_activities: String,
        max_group_size: Int,
        preferred_regions: String,
        max_distance_from_city: Int,
        nearby_cities: String,
        accessibility_needs: String,
        last_updated: String
    ): Unit

    /**
     * Update budget range for a user.
     */
    fun updateBudgetRange(
        min: Double,
        max: Double,
        last_updated: String,
        userId: String
    ): Unit

    /**
     * Update duration range for a user.
     */
    fun updateDurationRange(
        min: Int,
        max: Int,
        last_updated: String,
        userId: String
    ): Unit

    /**
     * Update preferred seasons for a user.
     */
    fun updatePreferredSeasons(
        seasons: String,
        last_updated: String,
        userId: String
    ): Unit

    /**
     * Update preferred activities for a user.
     */
    fun updatePreferredActivities(
        activities: String,
        last_updated: String,
        userId: String
    ): Unit

    /**
     * Update location preferences for a user.
     */
    fun updateLocationPreferences(
        regions: String,
        max_distance_from_city: Int,
        nearby_cities: String,
        last_updated: String,
        userId: String
    ): Unit
}

/**
 * SQLDelight query result wrapper.
 */
data class Query<out T : Any>(
    val executeAsOne: () -> T?,
    val executeAsList: () -> List<T>,
    val executeAsOneOrNull: () -> T?,
    val execute:List: () -> List<T>
)
