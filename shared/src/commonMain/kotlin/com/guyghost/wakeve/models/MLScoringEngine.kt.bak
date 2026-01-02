package com.guyghost.wakeve.models

import com.guyghost.wakeve.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ML Scoring Engine for event recommendations.
 *
 * This engine provides predictive recommendations using a hybrid approach:
 * - Phase 1: Feature extraction from user preferences, event data, and historical patterns
 * - Phase 2: ML-based scoring using TensorFlow Lite (simulated for now)
 * - Phase 3: Fallback to heuristic rules if ML confidence < 70%
 * - Phase 4: Score normalization (0.0 - 1.0)
 *
 * The scoring algorithm uses weighted factors:
 * - Proximity (30%): How close the date/time is to user preferences
 * - Type Match (20%): How well the event type matches user preferences
 * - Seasonality (30%): Seasonal preferences and historical attendance patterns
 * - Social (20%): Social factors like friends attending
 *
 * @property userPreferencesRepository Repository for accessing user preferences
 * @property confidenceThreshold Threshold below which to use fallback heuristics (default: 0.7)
 * @property currentModelVersion Version identifier for A/B testing
 */
class MLScoringEngine(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    companion object {
        /** Default confidence threshold for fallback */
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.7

        /** Model version for A/B testing */
        const val CURRENT_MODEL_VERSION = "v1.0-hybrid"

        /** Weights for scoring components */
        private const val PROXIMITY_WEIGHT = 0.30
        private const val TYPE_MATCH_WEIGHT = 0.20
        private const val SEASONALITY_WEIGHT = 0.30
        private const val SOCIAL_WEIGHT = 0.20
    }

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Predicts scores for a list of proposed dates.
     *
     * This method:
     * 1. Extracts features from each time slot and user preferences
     * 2. Runs ML inference (simulated) to get initial scores
     * 3. Applies fallback heuristics if confidence is too low
     * 4. Normalizes scores to 0.0 - 1.0 range
     *
     * @param eventId ID of the event to get recommendations for
     * @param proposedDates List of time slots to score
     * @param eventType Type of event (for scoring context)
     * @param userId ID of the user (for personalized scoring)
     * @return List of DateScore sorted by score (highest first)
     */
    suspend fun predictDateScores(
        eventId: String,
        proposedDates: List<TimeSlot>,
        eventType: EventType,
        userId: String
    ): List<DateScore> = withContext(Dispatchers.Default) {
        val userPrefs = userPreferencesRepository.getUserPreferences(userId)
            ?: UserPreference.empty(userId)

        val season = getCurrentSeason()

        proposedDates.map { timeSlot ->
            val features = extractDateFeatures(timeSlot, eventType, userPrefs, season)
            val (mlScore, mlConfidence) = runMLInference(features)

            val finalScore: Double
            val finalConfidence: Double

            if (mlConfidence >= DEFAULT_CONFIDENCE_THRESHOLD) {
                finalScore = mlScore
                finalConfidence = mlConfidence
            } else {
                // Fallback to heuristics
                val heuristicScore = applyFallbackHeuristics(features)
                finalScore = (mlScore + heuristicScore) / 2.0
                finalConfidence = mlConfidence * 0.5 + 0.5 // Boost confidence for fallback
            }

            val normalizedScore = normalizeScore(finalScore)
            val breakdown = calculateScoreBreakdown(features)

            DateScore(
                date = timeSlot.start?.take(10) ?: "flexible",
                score = normalizedScore,
                confidenceScore = finalConfidence,
                features = features,
                breakdown = breakdown
            )
        }.sortedByDescending { it.score }
    }

    /**
     * Predicts suitability scores for potential event locations.
     *
     * @param eventId ID of the event
     * @param potentialLocations List of locations to score
     * @param eventType Type of event (for type matching)
     * @param userId ID of the user (for personalized scoring)
     * @return List of LocationScore sorted by score (highest first)
     */
    suspend fun predictLocationSuitability(
        eventId: String,
        potentialLocations: List<PotentialLocation>,
        eventType: EventType,
        userId: String
    ): List<LocationScore> = withContext(Dispatchers.Default) {
        val userPrefs = userPreferencesRepository.getUserPreferences(userId)
            ?: UserPreference.empty(userId)

        potentialLocations.map { location ->
            val features = extractLocationFeatures(location, eventType, userPrefs)
            val (mlScore, mlConfidence) = runMLInference(features)

            val finalScore: Double
            val finalConfidence: Double

            if (mlConfidence >= DEFAULT_CONFIDENCE_THRESHOLD) {
                finalScore = mlScore
                finalConfidence = mlConfidence
            } else {
                val heuristicScore = applyFallbackHeuristics(features)
                finalScore = (mlScore + heuristicScore) / 2.0
                finalConfidence = mlConfidence * 0.5 + 0.5
            }

            val normalizedScore = normalizeScore(finalScore)
            val breakdown = calculateScoreBreakdown(features)

            LocationScore(
                locationId = location.id,
                locationName = location.name,
                score = normalizedScore,
                confidenceScore = finalConfidence,
                features = features,
                breakdown = breakdown
            )
        }.sortedByDescending { it.score }
    }

    /**
     * Predicts attendance rate for a specific time slot.
     *
     * Uses historical patterns and user preferences to estimate
     * how many participants are likely to attend.
     *
     * @param eventId ID of the event
     * @param timeSlot Time slot to predict attendance for
     * @param expectedParticipants Expected number of participants
     * @return AttendancePrediction with rate and confidence
     */
    suspend fun predictAttendance(
        eventId: String,
        timeSlot: TimeSlot,
        expectedParticipants: Int
    ): AttendancePrediction = withContext(Dispatchers.Default) {
        val features = mutableMapOf<String, Any>(
            "dayOfWeek" to getDayOfWeekIndex(timeSlot),
            "timeOfDay" to timeSlot.timeOfDay.ordinal,
            "expectedParticipants" to expectedParticipants,
            "historicalAttendanceRate" to 0.75 // Default baseline
        )

        val (mlRate, mlConfidence) = runMLInference(features)

        val finalRate: Double
        val finalConfidence: Double

        if (mlConfidence >= DEFAULT_CONFIDENCE_THRESHOLD) {
            finalRate = mlRate
            finalConfidence = mlConfidence
        } else {
            // Fallback to baseline with adjustment
            finalRate = 0.75
            finalConfidence = 0.5
        }

        val predictedCount = (finalRate * expectedParticipants).toInt()

        AttendancePrediction(
            timeSlotId = timeSlot.id,
            predictedAttendanceRate = finalRate,
            confidenceScore = finalConfidence,
            predictedCount = predictedCount
        )
    }

    // ==================== Feature Extraction ====================

    /**
     * Extracts features from a time slot for ML scoring.
     */
    private fun extractDateFeatures(
        timeSlot: TimeSlot,
        eventType: EventType,
        preferences: UserPreference,
        currentSeason: Season
    ): Map<String, Any> {
        val dayOfWeek = getDayOfWeekIndex(timeSlot)
        val isWeekend = dayOfWeek in listOf(5, 6) // Saturday, Sunday

        return mapOf(
            "dayOfWeek" to dayOfWeek,
            "timeOfDay" to timeSlot.timeOfDay.ordinal,
            "eventType" to eventType.ordinal,
            "season" to currentSeason.ordinal,
            "isWeekend" to if (isWeekend) 1 else 0,
            "userPreferredDays" to preferences.preferredDays.map { it.ordinal },
            "userPreferredTimeOfDay" to preferences.preferredTimeOfDay.map { it.ordinal },
            "preferredEventTypes" to preferences.preferredEventTypes.map { it.ordinal },
            "historicalAttendanceRate" to calculateHistoricalAttendanceRate(eventType, currentSeason),
            "proximityScore" to calculateProximityScore(timeSlot, preferences),
            "typeMatchScore" to calculateTypeMatchScore(eventType, preferences),
            "seasonalityScore" to calculateSeasonalityScore(currentSeason, preferences)
        )
    }

    /**
     * Extracts features from a location for ML scoring.
     */
    private fun extractLocationFeatures(
        location: PotentialLocation,
        eventType: EventType,
        preferences: UserPreference
    ): Map<String, Any> {
        return mapOf(
            "locationId" to location.id,
            "locationType" to location.locationType.ordinal,
            "locationName" to location.name,
            "eventType" to eventType.ordinal,
            "preferredLocations" to preferences.preferredLocations,
            "typeMatchScore" to calculateLocationTypeMatchScore(location.locationType, eventType),
            "proximityScore" to calculateLocationProximityScore(location.name, preferences),
            "historicalAttendanceRate" to 0.75
        )
    }

    // ==================== ML Inference Simulation ====================

    /**
     * Simulates TensorFlow Lite inference for ML scoring.
     * In production, this would call the actual TFLite model.
     *
     * For now, it implements a simplified linear combination of features
     * that mimics what a trained model would produce.
     */
    private fun runMLInference(features: Map<String, Any>): Pair<Double, Double> {
        // Simulated ML inference - in production, this would be TFLite
        val baseScore = when {
            features.containsKey("locationId") -> {
                // Location scoring
                val typeMatch = (features["typeMatchScore"] as? Double) ?: 0.5
                val proximity = (features["proximityScore"] as? Double) ?: 0.5
                typeMatch * 0.6 + proximity * 0.4
            }
            else -> {
                // Date scoring
                val proximity = (features["proximityScore"] as? Double) ?: 0.5
                val typeMatch = (features["typeMatchScore"] as? Double) ?: 0.5
                val seasonality = (features["seasonalityScore"] as? Double) ?: 0.5
                proximity * PROXIMITY_WEIGHT +
                    typeMatch * TYPE_MATCH_WEIGHT +
                    seasonality * SEASONALITY_WEIGHT +
                    0.5 * SOCIAL_WEIGHT // Placeholder for social features
            }
        }

        // Simulated confidence based on feature completeness
        val confidence = calculateConfidence(features)

        return Pair(baseScore.coerceIn(0.0, 1.0), confidence)
    }

    // ==================== Confidence Calculation ====================

    /**
     * Calculates the confidence score for a prediction.
     * Higher confidence when more features are available and consistent.
     *
     * @param features Feature map used for prediction
     * @return Confidence score (0.0 - 1.0)
     */
    private fun calculateConfidence(features: Map<String, Any>): Double {
        var confidence = 0.5 // Base confidence

        // Boost for preferred day match
        @Suppress("UNCHECKED_CAST")
        val userPreferredDays = features["userPreferredDays"] as? List<Int>
        val dayOfWeek = features["dayOfWeek"] as? Int
        if (userPreferredDays != null && dayOfWeek != null) {
            if (dayOfWeek in userPreferredDays) {
                confidence += 0.15
            }
        }

        // Boost for preferred time match
        @Suppress("UNCHECKED_CAST")
        val userPreferredTimeOfDay = features["userPreferredTimeOfDay"] as? List<Int>
        val timeOfDay = features["timeOfDay"] as? Int
        if (userPreferredTimeOfDay != null && timeOfDay != null) {
            if (timeOfDay in userPreferredTimeOfDay) {
                confidence += 0.10
            }
        }

        // Boost for weekend
        val isWeekend = features["isWeekend"] as? Int
        if (isWeekend == 1) {
            confidence += 0.05
        }

        // Boost for good historical rate
        val historicalRate = features["historicalAttendanceRate"] as? Double ?: 0.5
        if (historicalRate > 0.7) {
            confidence += 0.10
        }

        return confidence.coerceIn(0.0, 1.0)
    }

    // ==================== Fallback Heuristics ====================

    /**
     * Applies fallback heuristic rules when ML confidence is low.
     * These rules provide reasonable defaults based on common patterns.
     *
     * @param features Feature map used for prediction
     * @return Heuristic score (0.0 - 1.0)
     */
    private fun applyFallbackHeuristics(features: Map<String, Any>): Double {
        var score = 0.5 // Base score

        // Weekend preference heuristic
        val isWeekend = features["isWeekend"] as? Int ?: 0
        if (isWeekend == 1) {
            score += 0.15
        }

        // Evening preference for social events
        val eventType = features["eventType"] as? Int ?: 0
        val timeOfDay = features["timeOfDay"] as? Int ?: 4 // Default to SPECIFIC
        if (eventType in listOf(EventType.BIRTHDAY.ordinal, EventType.PARTY.ordinal) && timeOfDay == TimeOfDay.EVENING.ordinal) {
            score += 0.10
        }

        // Summer preference heuristic
        val season = features["season"] as? Int ?: 0
        if (season == Season.SUMMER.ordinal) { // SUMMER
            score += 0.10
        }

        // Time flexibility bonus (flexible slots get slight boost)
        if (timeOfDay < TimeOfDay.SPECIFIC.ordinal) { // Not SPECIFIC
            score += 0.05
        }

        // Day of week preference (Friday and Saturday best)
        val dayOfWeek = features["dayOfWeek"] as? Int ?: 0
        if (dayOfWeek == DayOfWeek.FRIDAY.ordinal || dayOfWeek == DayOfWeek.SATURDAY.ordinal) {
            score += 0.10
        }

        return score.coerceIn(0.0, 1.0)
    }

    // ==================== Score Component Calculations ====================

    /**
     * Calculates proximity score based on user preferences.
     */
    private fun calculateProximityScore(timeSlot: TimeSlot, preferences: UserPreference): Double {
        val dayOfWeek = getDayOfWeekIndex(timeSlot)

        // Check preferred days
        if (preferences.preferredDays.isNotEmpty()) {
            val preferredDayOrdinals = preferences.preferredDays.map { it.ordinal }
            if (dayOfWeek in preferredDayOrdinals) {
                return 0.9
            }
        }

        // Check preferred times
        if (preferences.preferredTimeOfDay.isNotEmpty()) {
            if (timeSlot.timeOfDay in preferences.preferredTimeOfDay) {
                return 0.85
            }
        }

        // Weekend bonus
        if (dayOfWeek in listOf(DayOfWeek.SATURDAY.ordinal, DayOfWeek.SUNDAY.ordinal)) {
            return 0.7
        }

        // Default score
        return 0.5
    }

    /**
     * Calculates type match score based on event type preferences.
     */
    private fun calculateTypeMatchScore(eventType: EventType, preferences: UserPreference): Double {
        if (preferences.preferredEventTypes.isEmpty()) {
            return 0.6 // Neutral score if no preferences
        }

        return if (eventType in preferences.preferredEventTypes) {
            0.95
        } else {
            0.4
        }
    }

    /**
     * Calculates seasonality score based on user preferences and event type.
     */
    private fun calculateSeasonalityScore(season: Season, preferences: UserPreference): Double {
        // Default seasonal preferences (can be enhanced with actual user data)
        return 0.7 // Default score
    }

    /**
     * Calculates location type match score.
     */
    private fun calculateLocationTypeMatchScore(
        locationType: LocationType,
        eventType: EventType
    ): Double {
        val idealLocationTypes = mapOf(
            EventType.WEDDING to listOf(LocationType.SPECIFIC_VENUE, LocationType.REGION),
            EventType.CONFERENCE to listOf(LocationType.CITY, LocationType.SPECIFIC_VENUE),
            EventType.OUTDOOR_ACTIVITY to listOf(LocationType.REGION, LocationType.CITY),
            EventType.TECH_MEETUP to listOf(LocationType.CITY, LocationType.ONLINE)
        )

        val idealTypes = idealLocationTypes[eventType] ?: listOf(LocationType.CITY, LocationType.SPECIFIC_VENUE)

        return if (locationType in idealTypes) {
            0.9
        } else {
            0.5
        }
    }

    /**
     * Calculates location proximity score based on user preferences.
     */
    private fun calculateLocationProximityScore(
        locationName: String,
        preferences: UserPreference
    ): Double {
        if (preferences.preferredLocations.isEmpty()) {
            return 0.6
        }

        return if (preferences.preferredLocations.any {
            it.lowercase() in locationName.lowercase() || locationName.lowercase() in it.lowercase()
        }) {
            0.95
        } else {
            0.4
        }
    }

    /**
     * Calculates historical attendance rate for similar events.
     */
    private fun calculateHistoricalAttendanceRate(
        eventType: EventType,
        season: Season
    ): Double {
        // Baseline rates by event type (simulated historical data)
        val baselineRates = mapOf(
            EventType.BIRTHDAY to 0.85,
            EventType.WEDDING to 0.92,
            EventType.TEAM_BUILDING to 0.78,
            EventType.CONFERENCE to 0.88,
            EventType.PARTY to 0.82,
            EventType.FAMILY_GATHERING to 0.90,
            EventType.OUTDOOR_ACTIVITY to 0.75,
            EventType.TECH_MEETUP to 0.80
        )

        val baseRate = baselineRates[eventType] ?: 0.75

        // Seasonal adjustment
        val seasonalMultiplier = when (season) {
            Season.SUMMER -> 1.05
            Season.WINTER -> 0.95
            else -> 1.0
        }

        return (baseRate * seasonalMultiplier).coerceIn(0.0, 1.0)
    }

    // ==================== Score Breakdown ====================

    /**
     * Calculates individual score components for breakdown display.
     */
    private fun calculateScoreBreakdown(features: Map<String, Any>): ScoreBreakdown {
        val proximity = (features["proximityScore"] as? Double) ?: 0.5
        val typeMatch = (features["typeMatchScore"] as? Double) ?: 0.5
        val seasonality = (features["seasonalityScore"] as? Double) ?: 0.5
        val social = 0.5 // Placeholder - would need actual social data

        return ScoreBreakdown(
            proximityScore = proximity,
            typeMatchScore = typeMatch,
            seasonalityScore = seasonality,
            socialScore = social
        )
    }

    // ==================== Utility Methods ====================

    /**
     * Normalizes a score to the 0.0 - 1.0 range.
     */
    private fun normalizeScore(score: Double): Double {
        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Gets the day of week index (0 = Monday, 6 = Sunday) from a time slot.
     */
    private fun getDayOfWeekIndex(timeSlot: TimeSlot): Int {
        return try {
            if (timeSlot.start != null) {
                val date = LocalDate.parse(timeSlot.start.take(10), dateFormatter)
                val dayValue = date.dayOfWeek.value // Monday = 1
                (dayValue + 5) % 7 // Convert to 0 = Monday, 6 = Sunday
            } else {
                0 // Default to Monday for flexible slots
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Gets the current season based on the current date.
     */
    private fun getCurrentSeason(): Season {
        val month = LocalDate.now().monthValue
        return Season.fromMonth(month)
    }
}
