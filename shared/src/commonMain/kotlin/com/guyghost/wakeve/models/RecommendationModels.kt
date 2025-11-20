package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val userId: String,
    val preferredDaysOfWeek: List<String> = emptyList(), // e.g., ["monday", "friday"]
    val preferredTimes: List<String> = emptyList(), // e.g., ["morning", "evening"]
    val preferredLocations: List<String> = emptyList(), // e.g., ["paris", "beach"]
    val preferredActivities: List<String> = emptyList(), // e.g., ["dinner", "hiking"]
    val budgetRange: BudgetRange? = null,
    val groupSizePreference: Int? = null, // preferred number of participants
    val lastUpdated: String // ISO timestamp
)

@Serializable
enum class BudgetRange {
    LOW, MEDIUM, HIGH
}

@Serializable
enum class RecommendationType {
    DATE, LOCATION, ACTIVITY
}

@Serializable
data class Recommendation(
    val id: String,
    val type: RecommendationType,
    val eventId: String,
    val content: String, // JSON string representing the suggestion (e.g., TimeSlot for DATE)
    val score: Double, // 0.0 to 1.0
    val reason: String, // explanation for the recommendation
    val createdAt: String // ISO timestamp
)

interface SuggestionEngine {
    fun suggestDates(event: Event, preferences: UserPreferences): List<Recommendation>
    fun suggestLocations(event: Event, preferences: UserPreferences): List<Recommendation>
    fun suggestActivities(event: Event, preferences: UserPreferences): List<Recommendation>
}</content>
<parameter name="filePath">shared/src/commonMain/kotlin/com/guyghost/wakeve/models/RecommendationModels.kt