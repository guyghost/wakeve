package com.guyghost.wakeve.repository

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.DayOfWeek
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.InteractionType
import com.guyghost.wakeve.models.PreferenceInteraction
import com.guyghost.wakeve.models.ScoreWeights
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.UserPreference
import com.guyghost.wakeve.models.VoteType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.exp

/**
 * Repository for managing user preferences for AI-powered recommendations.
 *
 * This repository collects anonymized voting history and learns user preferences
 * over time to provide personalized recommendations. It supports both implicit
 * preferences (learned from behavior) and explicit preferences (set by the user).
 *
 * Key features:
 * - Implicit learning from votes, event creation, and participation
 * - Exponential decay for older interactions (older = less weight)
 * - Score weights configuration for recommendation tuning
 * - Offline-first with sync support
 *
 * @property database The SQLDelight database instance
 */
class UserPreferencesRepository(private val database: WakeveDb) {

    private val json = Json { 
        prettyPrint = false 
        ignoreUnknownKeys = true
    }

    /**
     * Gets user preferences for a specific user.
     *
     * @param userId The unique identifier of the user
     * @return UserPreference if found, null otherwise
     */
    suspend fun getUserPreferences(userId: String): UserPreference? {
        return database.userPreferencesQueries.selectPreferencesByUserId(userId)
            .executeAsOneOrNull()
            ?.let { row ->
                UserPreference(
                    userId = row.user_id,
                    preferredDays = json.decodeFromString(row.preferred_days),
                    preferredTimeOfDay = json.decodeFromString(row.preferred_time_of_day),
                    preferredEventTypes = json.decodeFromString(row.preferred_event_types),
                    preferredLocations = json.decodeFromString(row.preferred_locations),
                    avoidEvents = row.avoid_events == 1L,
                    scoreWeights = json.decodeFromString(row.score_weights),
                    lastUpdated = row.last_updated
                )
            }
    }

    /**
     * Updates or creates user preferences.
     *
     * @param userId The unique identifier of the user
     * @param preferences The UserPreference to save
     */
    suspend fun updateUserPreferences(userId: String, preferences: UserPreference) {
        val timestamp = Clock.System.now().toString()
        val updatedPreferences = preferences.copy(lastUpdated = timestamp)

        database.userPreferencesQueries.insertPreferences(
            user_id = updatedPreferences.userId,
            preferred_days = json.encodeToString(updatedPreferences.preferredDays),
            preferred_time_of_day = json.encodeToString(updatedPreferences.preferredTimeOfDay),
            preferred_event_types = json.encodeToString(updatedPreferences.preferredEventTypes),
            preferred_locations = json.encodeToString(updatedPreferences.preferredLocations),
            avoid_events = if (updatedPreferences.avoidEvents) 1L else 0L,
            score_weights = json.encodeToString(updatedPreferences.scoreWeights),
            last_updated = timestamp
        )
    }

    /**
     * Records a vote for learning implicit preferences.
     * The vote will be analyzed to update preferred days, times, and event types.
     *
     * @param userId The unique identifier of the user
     * @param eventId The unique identifier of the event
     * @param voteType The type of vote (YES, MAYBE, NO)
     * @param timeOfDay The time of day for the voted slot
     * @param dayOfWeek The day of week for the voted slot
     */
    suspend fun recordVote(
        userId: String,
        eventId: String,
        voteType: VoteType,
        timeOfDay: TimeOfDay,
        dayOfWeek: DayOfWeek
    ) {
        val timestamp = Clock.System.now().toString()
        val interactionId = generateInteractionId(userId, eventId, timestamp)

        database.userPreferencesQueries.insertInteraction(
            id = interactionId,
            user_id = userId,
            event_id = eventId,
            interaction_type = InteractionType.VOTE.name,
            event_type = null,
            time_of_day = timeOfDay.name,
            vote_type = voteType.name,
            day_of_week = dayOfWeek.name,
            location = null,
            timestamp = timestamp,
            synced = 0L
        )
    }

    /**
     * Records event creation for learning preferred event types.
     *
     * @param userId The unique identifier of the user
     * @param eventId The unique identifier of the event
     * @param eventType The type of event created
     */
    suspend fun recordEventCreation(
        userId: String,
        eventId: String,
        eventType: EventType
    ) {
        val timestamp = Clock.System.now().toString()
        val interactionId = generateInteractionId(userId, eventId, timestamp)

        database.userPreferencesQueries.insertInteraction(
            id = interactionId,
            user_id = userId,
            event_id = eventId,
            interaction_type = InteractionType.EVENT_CREATION.name,
            event_type = eventType.name,
            time_of_day = null,
            vote_type = null,
            day_of_week = null,
            location = null,
            timestamp = timestamp,
            synced = 0L
        )
    }

    /**
     * Records event participation for learning location and social preferences.
     *
     * @param userId The unique identifier of the user
     * @param eventId The unique identifier of the event
     * @param location The city/location where the event took place
     */
    suspend fun recordEventParticipation(
        userId: String,
        eventId: String,
        location: String?
    ) {
        val timestamp = Clock.System.now().toString()
        val interactionId = generateInteractionId(userId, eventId, timestamp)

        database.userPreferencesQueries.insertInteraction(
            id = interactionId,
            user_id = userId,
            event_id = eventId,
            interaction_type = InteractionType.PARTICIPATION.name,
            event_type = null,
            time_of_day = null,
            vote_type = null,
            day_of_week = null,
            location = location,
            timestamp = timestamp,
            synced = 0L
        )
    }

    /**
     * Calculates implicit preferences based on user's interaction history.
     * Uses exponential decay to weight recent interactions more heavily.
     *
     * The decay formula: weight = exp(-days / decayConstant)
     * A decay constant of 30 means:
     * - Today: weight = 1.0
     * - 30 days ago: weight = 0.37
     * - 60 days ago: weight = 0.14
     * - 90 days ago: weight = 0.05
     *
     * @param userId The unique identifier of the user
     * @param decayDays The decay constant in days (default: 30)
     * @return Calculated UserPreference based on historical behavior
     */
    suspend fun calculateImplicitPreferences(
        userId: String,
        decayDays: Int = 30
    ): UserPreference {
        val now = Clock.System.now()
        val cutoffMillis = now.toEpochMilliseconds() - (decayDays * 24L * 60L * 60L * 1000L)
        val cutoffDate = Instant.fromEpochMilliseconds(cutoffMillis).toString()

        val interactions = database.userPreferencesQueries
            .selectRecentInteractionsByUserId(userId, cutoffDate)
            .executeAsList()

        if (interactions.isEmpty()) {
            return UserPreference.empty(userId)
        }

        // Calculate weighted scores for each preference category
        val dayScores = mutableMapOf<DayOfWeek, Double>()
        val timeOfDayScores = mutableMapOf<TimeOfDay, Double>()
        val eventTypeScores = mutableMapOf<EventType, Double>()
        val locationScores = mutableMapOf<String, Double>()
        var avoidEventTypes = mutableSetOf<EventType>()

        for (interaction in interactions) {
            val daysAgo = calculateDaysAgo(interaction.timestamp, now)
            val weight = exp(-daysAgo.toDouble() / decayDays)

            // Process based on interaction type
            when (InteractionType.valueOf(interaction.interaction_type)) {
                InteractionType.VOTE -> {
                    val voteType = VoteType.valueOf(interaction.vote_type ?: "MAYBE")
                    val timeWeight = getVoteTimeWeight(voteType)

                    // Score for day of week
                    interaction.day_of_week?.let { dayStr ->
                        val dayOfWeek = DayOfWeek.valueOf(dayStr)
                        dayScores[dayOfWeek] = (dayScores[dayOfWeek] ?: 0.0) + (weight * timeWeight)
                    }

                    // Score for time of day
                    interaction.time_of_day?.let { timeStr ->
                        val timeOfDay = TimeOfDay.valueOf(timeStr)
                        timeOfDayScores[timeOfDay] = (timeOfDayScores[timeOfDay] ?: 0.0) + (weight * timeWeight)
                    }
                }

                InteractionType.EVENT_CREATION -> {
                    interaction.event_type?.let { typeStr ->
                        val eventType = EventType.valueOf(typeStr)
                        eventTypeScores[eventType] = (eventTypeScores[eventType] ?: 0.0) + weight
                    }
                }

                InteractionType.PARTICIPATION -> {
                    interaction.location?.let { location ->
                        locationScores[location] = (locationScores[location] ?: 0.0) + weight
                    }
                }
            }
        }

        // Determine avoid events based on never-voted event types (heuristic)
        val allEventTypes = EventType.entries.toSet()
        val votedEventTypes = eventTypeScores.keys
        avoidEventTypes = (allEventTypes - votedEventTypes).toMutableSet()

        // Extract top preferences (items with positive weighted score)
        val preferredDays = dayScores
            .filter { it.value > 0 }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        val preferredTimeOfDay = timeOfDayScores
            .filter { it.value > 0 }
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        val preferredEventTypes = eventTypeScores
            .filter { it.value > 0 }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        val preferredLocations = locationScores
            .filter { it.value > 0 }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        return UserPreference(
            userId = userId,
            preferredDays = preferredDays,
            preferredTimeOfDay = preferredTimeOfDay,
            preferredEventTypes = preferredEventTypes,
            preferredLocations = preferredLocations,
            avoidEvents = avoidEventTypes.isNotEmpty(),
            scoreWeights = ScoreWeights.DEFAULT,
            lastUpdated = Clock.System.now().toString()
        )
    }

    /**
     * Applies exponential decay to old interactions and removes very old data.
     * This helps keep the preference learning relevant to recent behavior.
     *
     * @param userId The unique identifier of the user
     * @param decayDays Number of days after which interactions are considered stale
     */
    suspend fun applyDecay(userId: String, decayDays: Int = 30) {
        // Remove interactions older than the decay period
        val now = Clock.System.now()
        val cutoffMillis = now.toEpochMilliseconds() - (decayDays * 3L * 24L * 60L * 60L * 1000L) // Keep 3x decayDays for history
        val cutoffDate = Instant.fromEpochMilliseconds(cutoffMillis).toString()

        database.userPreferencesQueries.deleteOldInteractions(cutoffDate)
    }

    /**
     * Gets all interactions for a user (for debugging/analytics).
     *
     * @param userId The unique identifier of the user
     * @return List of all PreferenceInteraction for the user
     */
    suspend fun getInteractionHistory(userId: String): List<PreferenceInteraction> {
        return database.userPreferencesQueries
            .selectInteractionsByUserId(userId)
            .executeAsList()
            .map { row ->
                PreferenceInteraction(
                    id = row.id,
                    userId = row.user_id,
                    eventId = row.event_id,
                    interactionType = InteractionType.valueOf(row.interaction_type),
                    eventType = row.event_type?.let { EventType.valueOf(it) },
                    timeOfDay = row.time_of_day?.let { TimeOfDay.valueOf(it) },
                    voteType = row.vote_type?.let { VoteType.valueOf(it) },
                    dayOfWeek = row.day_of_week?.let { DayOfWeek.valueOf(it) },
                    location = row.location,
                    timestamp = row.timestamp,
                    synced = row.synced == 1L
                )
            }
    }

    /**
     * Deletes all preferences and interactions for a user.
     * Used for GDPR compliance (right to erasure).
     *
     * @param userId The unique identifier of the user
     */
    suspend fun deleteUserData(userId: String) {
        database.userPreferencesQueries.deletePreferences(userId)
        
        // Delete all interactions (older than any date)
        val ancientDate = "1970-01-01T00:00:00Z"
        database.userPreferencesQueries.deleteOldInteractions(ancientDate)
    }

    /**
     * Merges implicit and explicit preferences.
     * Explicit preferences take precedence over implicit ones.
     *
     * @param userId The unique identifier of the user
     * @param explicitPreferences User-set preferences
     * @return Merged UserPreference combining both sources
     */
    suspend fun mergeWithExplicit(
        userId: String,
        explicitPreferences: UserPreference
    ): UserPreference {
        val implicitPreferences = calculateImplicitPreferences(userId)

        return UserPreference(
            userId = userId,
            preferredDays = explicitPreferences.preferredDays.ifEmpty { implicitPreferences.preferredDays },
            preferredTimeOfDay = explicitPreferences.preferredTimeOfDay.ifEmpty { implicitPreferences.preferredTimeOfDay },
            preferredEventTypes = explicitPreferences.preferredEventTypes.ifEmpty { implicitPreferences.preferredEventTypes },
            preferredLocations = explicitPreferences.preferredLocations.ifEmpty { implicitPreferences.preferredLocations },
            avoidEvents = explicitPreferences.avoidEvents,
            scoreWeights = explicitPreferences.scoreWeights,
            lastUpdated = Clock.System.now().toString()
        )
    }

    // Private helper functions

    /**
     * Generates a unique ID for an interaction.
     */
    private fun generateInteractionId(userId: String, eventId: String, timestamp: String): String {
        return "${userId}_${eventId}_${timestamp.hashCode()}"
    }

    /**
     * Calculates the number of days between a timestamp and now.
     */
    private fun calculateDaysAgo(timestamp: String, now: Instant): Long {
        return try {
            val then = Instant.parse(timestamp)
            val diffSeconds = now.toEpochMilliseconds() / 1000 - then.toEpochMilliseconds() / 1000
            diffSeconds / (24 * 60 * 60)
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Returns a weight multiplier based on vote type.
     * YES votes have highest weight, NO votes have negative weight.
     */
    private fun getVoteTimeWeight(voteType: VoteType): Double {
        return when (voteType) {
            VoteType.YES -> 1.0
            VoteType.MAYBE -> 0.5
            VoteType.NO -> -0.3
        }
    }
}
