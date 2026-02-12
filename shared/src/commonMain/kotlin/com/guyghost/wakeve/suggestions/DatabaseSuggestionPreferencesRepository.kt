package com.guyghost.wakeve.suggestions

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.LocationPreferences
import com.guyghost.wakeve.models.SuggestionBudgetRange
import com.guyghost.wakeve.models.SuggestionInteractionType
import com.guyghost.wakeve.models.SuggestionSeason
import com.guyghost.wakeve.models.SuggestionUserPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock

/**
 * Database-backed repository for managing user suggestion preferences.
 * Implements SQLDelight persistence for offline-first functionality.
 *
 * This repository is part of the Imperative Shell layer, handling I/O operations
 * (SQLite database access, JSON serialization) while delegating pure logic
 * to the Functional Core (SuggestionUserPreferences models).
 */
class DatabaseSuggestionPreferencesRepository(
    private val database: WakeveDb,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SuggestionPreferencesRepositoryInterface {

    private companion object {
        private const val TAG = "SuggestionPrefsRepo"
    }

     private val preferencesQueries = database.suggestionPreferencesQueries

    /**
     * Get suggestion preferences for a user.
     * Returns null if no preferences exist for the user.
     */
    override fun getSuggestionPreferences(userId: String): SuggestionUserPreferences? {
        return try {
            preferencesQueries.selectPreferencesByUserId(userId).executeAsOneOrNull()?.let { row ->
                SuggestionUserPreferences(
                    userId = row.user_id,
                    budgetRange = SuggestionBudgetRange(
                        min = row.budgetMin,
                        max = row.budgetMax,
                        currency = row.budgetCurrency
                    ),
                    preferredDurationRange = row.preferredDurationMin.toInt()..row.preferredDurationMax.toInt(),
                    preferredSeasons = decodeSeasons(row.preferredSeasons),
                    preferredActivities = decodeStringList(row.preferredActivities),
                    maxGroupSize = row.maxGroupSize.toInt(),
                    locationPreferences = LocationPreferences(
                        preferredRegions = decodeStringList(row.preferredRegions),
                        maxDistanceFromCity = row.maxDistanceFromCity.toInt(),
                        nearbyCities = decodeStringList(row.nearbyCities)
                    ),
                    accessibilityNeeds = decodeStringList(row.accessibilityNeeds)
                )
            }
        } catch (e: Exception) {
            // Log error and return null for graceful degradation
            null
        }
    }

     /**
      * Save suggestion preferences for a user.
      * Uses INSERT OR REPLACE to upsert preferences.
      */
     override suspend fun saveSuggestionPreferences(preferences: SuggestionUserPreferences) {
         val now = Clock.System.now().toString()

         preferencesQueries.insertOrReplacePreferences(
             user_id = preferences.userId,
             budgetMin = preferences.budgetRange.min,
             budgetMax = preferences.budgetRange.max,
             budgetCurrency = preferences.budgetRange.currency,
             preferredDurationMin = preferences.preferredDurationRange.start.toLong(),
             preferredDurationMax = preferences.preferredDurationRange.endInclusive.toLong(),
             preferredSeasons = encodeSeasons(preferences.preferredSeasons),
             preferredActivities = encodeStringList(preferences.preferredActivities),
             maxGroupSize = preferences.maxGroupSize.toLong(),
             preferredRegions = encodeStringList(preferences.locationPreferences.preferredRegions),
             maxDistanceFromCity = preferences.locationPreferences.maxDistanceFromCity.toLong(),
             nearbyCities = encodeStringList(preferences.locationPreferences.nearbyCities),
             accessibilityNeeds = encodeStringList(preferences.accessibilityNeeds),
             lastUpdated = now
         )
     }

     /**
      * Update only the budget range for a user.
      */
     override suspend fun updateBudgetRange(userId: String, budgetRange: SuggestionBudgetRange) {
         val now = Clock.System.now().toString()
         preferencesQueries.updateBudgetRange(
             budgetMin = budgetRange.min,
             budgetMax = budgetRange.max,
             lastUpdated = now,
             user_id = userId
         )
     }

     /**
      * Update preferred duration range for a user.
      */
     override suspend fun updateDurationRange(userId: String, durationRange: ClosedRange<Int>) {
         val now = Clock.System.now().toString()
         preferencesQueries.updateDurationRange(
             preferredDurationMin = durationRange.start.toLong(),
             preferredDurationMax = durationRange.endInclusive.toLong(),
             lastUpdated = now,
             user_id = userId
         )
     }

     /**
      * Update preferred seasons for a user.
      */
     override suspend fun updatePreferredSeasons(userId: String, seasons: List<SuggestionSeason>) {
         val now = Clock.System.now().toString()
         preferencesQueries.updatePreferredSeasons(
             preferredSeasons = encodeSeasons(seasons),
             lastUpdated = now,
             user_id = userId
         )
     }

     /**
      * Update preferred activities for a user.
      */
     override suspend fun updatePreferredActivities(userId: String, activities: List<String>) {
         val now = Clock.System.now().toString()
         preferencesQueries.updatePreferredActivities(
             preferredActivities = encodeStringList(activities),
             lastUpdated = now,
             user_id = userId
         )
     }

      /**
       * Update location preferences for a user.
       */
      override suspend fun updateLocationPreferences(userId: String, locationPrefs: LocationPreferences) {
          val now = Clock.System.now().toString()
          preferencesQueries.updateLocationPreferences(
              preferredRegions = encodeStringList(locationPrefs.preferredRegions),
              maxDistanceFromCity = locationPrefs.maxDistanceFromCity.toLong(),
              nearbyCities = encodeStringList(locationPrefs.nearbyCities),
              lastUpdated = now,
              user_id = userId
          )
      }

      /**
       * Update accessibility needs for a user.
       */
      override suspend fun updateAccessibilityNeeds(userId: String, needs: List<String>) {
          val now = Clock.System.now().toString()
          preferencesQueries.updateAccessibilityNeeds(
              accessibilityNeeds = encodeStringList(needs),
              lastUpdated = now,
              user_id = userId
          )
      }

     /**
      * Delete suggestion preferences for a user.
      */
     override suspend fun deleteSuggestionPreferences(userId: String) {
         preferencesQueries.deletePreferences(userId)
     }

      /**
       * Track user interaction for A/B testing and recommendation improvement.
       */
      override fun trackInteraction(
          userId: String,
          suggestionId: String,
          interactionType: SuggestionInteractionType
      ) {
          // TODO: Implement interaction tracking via suggestion_interactions table
          // For now, this is a stub implementation
      }

      /**
       * Track user interaction with metadata for enhanced analytics.
       */
      override fun trackInteractionWithMetadata(
          userId: String,
          suggestionId: String,
          interactionType: SuggestionInteractionType,
          metadata: Map<String, String>
      ) {
          // TODO: Implement interaction tracking with metadata
          // For now, this is a stub implementation
      }

      /**
       * Get interaction history for a user.
       */
      override fun getInteractionHistory(userId: String): List<SuggestionInteraction> {
          // TODO: Implement interaction history retrieval
          return emptyList()
      }

      /**
       * Get recent interactions for a user since a specific timestamp.
       */
      override fun getRecentInteractions(userId: String, sinceTimestamp: String): List<SuggestionInteraction> {
          // TODO: Implement recent interactions filtering
          return emptyList()
      }

      /**
       * Get counts of interaction types for a user since a specific timestamp.
       */
      override fun getInteractionCountsByType(userId: String, sinceTimestamp: String): Map<SuggestionInteractionType, Long> {
          // TODO: Implement interaction type counts
          return emptyMap()
      }

      /**
       * Get top suggestions since a specific timestamp.
       */
      override fun getTopSuggestions(sinceTimestamp: String, limit: Int): List<Pair<String, Long>> {
          // TODO: Implement top suggestions ranking
          return emptyList()
      }

      /**
       * Clean up old interactions older than a specific timestamp.
       */
      override suspend fun cleanupOldInteractions(olderThanTimestamp: String) {
          // TODO: Implement old interactions cleanup
      }

    // Private helper functions

    /**
     * Decode seasons from JSON string.
     */
    private fun decodeSeasons(jsonString: String?): List<SuggestionSeason> {
        return if (jsonString.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<SuggestionSeason>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Encode seasons to JSON string.
     */
    private fun encodeSeasons(seasons: List<SuggestionSeason>): String {
        return json.encodeToString(seasons)
    }

    /**
     * Decode string list from JSON.
     */
    private fun decodeStringList(jsonString: String?): List<String> {
        return if (jsonString.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<String>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Encode string list to JSON.
     */
    private fun encodeStringList(list: List<String>): String {
        return json.encodeToString(list)
    }

      /**
       * Generate a unique interaction ID.
       */
      private fun generateInteractionId(userId: String, suggestionId: String): String {
          val timestamp = Clock.System.now().epochSeconds
          return "$userId-$suggestionId-$timestamp"
      }

      /**
       * Decode metadata from JSON string.
       */
      private fun decodeMetadata(jsonString: String?): Map<String, String> {
          return if (jsonString.isNullOrBlank() || jsonString == "{}") {
              emptyMap()
          } else {
              try {
                  json.decodeFromString<Map<String, String>>(jsonString)
              } catch (e: Exception) {
                  emptyMap()
              }
          }
      }
}
