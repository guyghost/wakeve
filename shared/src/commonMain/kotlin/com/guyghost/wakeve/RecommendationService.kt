package com.guyghost.wakeve

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.Recommendation
import com.guyghost.wakeve.models.UserPreferences

class RecommendationService(
    private val suggestionEngine: SuggestionEngine = DefaultSuggestionEngine(),
    private val userPreferencesRepository: UserPreferencesRepository
) {

    fun getDateRecommendations(event: Event, userId: String): List<Recommendation> {
        val preferences = userPreferencesRepository.getPreferences(userId) ?: getDefaultPreferences(userId)
        return suggestionEngine.suggestDates(event, preferences)
    }

    fun getLocationRecommendations(event: Event, userId: String): List<Recommendation> {
        val preferences = userPreferencesRepository.getPreferences(userId) ?: getDefaultPreferences(userId)
        return suggestionEngine.suggestLocations(event, preferences)
    }

    fun getActivityRecommendations(event: Event, userId: String): List<Recommendation> {
        val preferences = userPreferencesRepository.getPreferences(userId) ?: getDefaultPreferences(userId)
        return suggestionEngine.suggestActivities(event, preferences)
    }

    fun updateUserPreferences(userId: String, preferences: UserPreferences) {
        userPreferencesRepository.savePreferences(preferences)
    }

    private fun getDefaultPreferences(userId: String): UserPreferences {
        return UserPreferences(
            userId = userId,
            preferredDaysOfWeek = emptyList(),
            preferredTimes = emptyList(),
            preferredLocations = emptyList(),
            preferredActivities = emptyList(),
            budgetRange = null,
            groupSizePreference = null,
            lastUpdated = "2025-11-20T10:00:00Z"
        )
    }
}