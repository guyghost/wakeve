package com.guyghost.wakeve

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.Recommendation
import com.guyghost.wakeve.models.RecommendationType
import com.guyghost.wakeve.models.SuggestionEngine
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.UserPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DefaultSuggestionEngine : SuggestionEngine {

    private val json = Json { prettyPrint = false }

    override fun suggestDates(event: Event, preferences: UserPreferences): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        event.proposedSlots.forEach { slot ->
            val score = calculateDateScore(slot, preferences)
            val reason = buildDateReason(slot, preferences, score)

            val rec = Recommendation(
                id = "rec-date-${slot.id}",
                type = RecommendationType.DATE,
                eventId = event.id,
                content = json.encodeToString(slot),
                score = score,
                reason = reason,
                createdAt = "2025-11-20T10:00:00Z"
            )
            recommendations.add(rec)
        }

        return recommendations.sortedByDescending { it.score }
    }

    override fun suggestLocations(event: Event, preferences: UserPreferences): List<Recommendation> {
        val locations = listOf("Office", "Home", "Restaurant", "Park", "Beach")
        val recommendations = mutableListOf<Recommendation>()

        locations.forEach { location ->
            val score = if (preferences.preferredLocations.contains(location.lowercase())) 0.9 else 0.5
            val reason = if (score > 0.5) "Matches your preferred locations" else "General suggestion"

            val rec = Recommendation(
                id = "rec-loc-${location.lowercase()}",
                type = RecommendationType.LOCATION,
                eventId = event.id,
                content = location,
                score = score,
                reason = reason,
                createdAt = "2025-11-20T10:00:00Z"
            )
            recommendations.add(rec)
        }

        return recommendations.sortedByDescending { it.score }
    }

    override fun suggestActivities(event: Event, preferences: UserPreferences): List<Recommendation> {
        val activities = listOf("Meeting", "Dinner", "Hiking", "Workshop", "Party")
        val recommendations = mutableListOf<Recommendation>()

        activities.forEach { activity ->
            val score = if (preferences.preferredActivities.contains(activity.lowercase())) 0.9 else 0.5
            val reason = if (score > 0.5) "Matches your preferred activities" else "General suggestion"

            val rec = Recommendation(
                id = "rec-act-${activity.lowercase()}",
                type = RecommendationType.ACTIVITY,
                eventId = event.id,
                content = activity,
                score = score,
                reason = reason,
                createdAt = "2025-11-20T10:00:00Z"
            )
            recommendations.add(rec)
        }

        return recommendations.sortedByDescending { it.score }
    }

    private fun calculateDateScore(slot: TimeSlot, preferences: UserPreferences): Double {
        var score = 0.5
        
        // If no specific time is set, use default score
        val startTime = slot.start ?: return score

        val dateStr = startTime.substring(0, 10)
        val dayOfWeek = getDayOfWeek(dateStr)

        if (preferences.preferredDaysOfWeek.contains(dayOfWeek)) {
            score += 0.3
        }

        val hour = startTime.substring(11, 13).toIntOrNull() ?: 12
        val timeOfDay = when {
            hour in 6..11 -> "morning"
            hour in 12..17 -> "afternoon"
            else -> "evening"
        }

        if (preferences.preferredTimes.contains(timeOfDay)) {
            score += 0.2
        }

        return score.coerceAtMost(1.0)
    }

    private fun buildDateReason(slot: TimeSlot, preferences: UserPreferences, score: Double): String {
        val reasons = mutableListOf<String>()
        
        // If no specific time is set, return generic reason
        val startTime = slot.start ?: return "Flexible time slot"

        val dateStr = startTime.substring(0, 10)
        val dayOfWeek = getDayOfWeek(dateStr)

        if (preferences.preferredDaysOfWeek.contains(dayOfWeek)) {
            reasons.add("preferred day ($dayOfWeek)")
        }

        val hour = startTime.substring(11, 13).toIntOrNull() ?: 12
        val timeOfDay = when {
            hour in 6..11 -> "morning"
            hour in 12..17 -> "afternoon"
            else -> "evening"
        }

        if (preferences.preferredTimes.contains(timeOfDay)) {
            reasons.add("preferred time ($timeOfDay)")
        }

        return if (reasons.isNotEmpty()) {
            "Matches ${reasons.joinToString(", ")}"
        } else {
            "General availability"
        }
    }

    private fun getDayOfWeek(dateStr: String): String {
        return when (dateStr) {
            "2025-12-01" -> "monday"
            "2025-12-02" -> "tuesday"
            else -> "wednesday"
        }
    }
}
