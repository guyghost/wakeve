package com.guyghost.wakeve

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.BudgetRange
import com.guyghost.wakeve.models.UserPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserPreferencesRepository(private val database: WakevDb) {

    private val json = Json { prettyPrint = false }

    fun getPreferences(userId: String): UserPreferences? {
        return database.userPreferencesQueries.selectPreferencesByUserId(userId).executeAsOneOrNull()?.let { row ->
            UserPreferences(
                userId = row.user_id,
                preferredDaysOfWeek = json.decodeFromString(row.preferred_days_of_week),
                preferredTimes = json.decodeFromString(row.preferred_times),
                preferredLocations = json.decodeFromString(row.preferred_locations),
                preferredActivities = json.decodeFromString(row.preferred_activities),
                budgetRange = row.budget_range?.let { BudgetRange.valueOf(it) },
                groupSizePreference = row.group_size_preference,
                lastUpdated = row.last_updated
            )
        }
    }

    fun savePreferences(preferences: UserPreferences) {
        val existing = getPreferences(preferences.userId)
        if (existing == null) {
            database.userPreferencesQueries.insertPreferences(
                user_id = preferences.userId,
                preferred_days_of_week = json.encodeToString(preferences.preferredDaysOfWeek),
                preferred_times = json.encodeToString(preferences.preferredTimes),
                preferred_locations = json.encodeToString(preferences.preferredLocations),
                preferred_activities = json.encodeToString(preferences.preferredActivities),
                budget_range = preferences.budgetRange?.name,
                group_size_preference = preferences.groupSizePreference,
                last_updated = preferences.lastUpdated
            )
        } else {
            database.userPreferencesQueries.updatePreferences(
                preferred_days_of_week = json.encodeToString(preferences.preferredDaysOfWeek),
                preferred_times = json.encodeToString(preferences.preferredTimes),
                preferred_locations = json.encodeToString(preferences.preferredLocations),
                preferred_activities = json.encodeToString(preferences.preferredActivities),
                budget_range = preferences.budgetRange?.name,
                group_size_preference = preferences.groupSizePreference,
                last_updated = preferences.lastUpdated,
                user_id = preferences.userId
            )
        }
    }

    fun deletePreferences(userId: String) {
        database.userPreferencesQueries.deletePreferences(userId)
    }
}