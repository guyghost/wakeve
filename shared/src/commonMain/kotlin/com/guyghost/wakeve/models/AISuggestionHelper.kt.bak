package com.guyghost.wakeve.models

import com.guyghost.wakeve.models.AIBadge
import com.guyghost.wakeve.models.AIBadgeType
import com.guyghost.wakeve.models.AIMetadata
import com.guyghost.wakeve.models.AISuggestion
import com.guyghost.wakeve.models.PredictionSource
import com.guyghost.wakeve.models.SuggestionCategory
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

/**
 * Helper functions pour convertir les modèles de scoring existants en suggestions IA.
 * Fournit des fonctions de conversion et de génération de badges.
 */
object AISuggestionHelper {

    private const val DEFAULT_MODEL_VERSION = "1.0.0"

    /**
     * Génère un badge IA basé sur le score de confiance.
     *
     * @param confidenceScore Score de confiance (0.0 - 1.0)
     * @param isPersonalized Si la suggestion est personnalisée pour l'utilisateur
     * @param isPopular Si la suggestion est populaire auprès d'événements similaires
     * @param isSeasonal Si la suggestion est saisonnière
     * @return Le badge approprié selon le score de confiance
     */
    fun calculateAIBadge(
        confidenceScore: Double,
        isPersonalized: Boolean = false,
        isPopular: Boolean = false,
        isSeasonal: Boolean = false
    ): AIBadge {
        return when {
            // Priorité aux badges spéciaux (personalized, popular, seasonal)
            isPersonalized -> AIBadge(
                type = AIBadgeType.PERSONALIZED,
                displayName = "Personalized for You",
                icon = "⭐",
                color = "#FFD700", // Gold
                tooltip = "Tailored to your preferences based on your event history"
            )
            isPopular -> AIBadge(
                type = AIBadgeType.POPULAR_CHOICE,
                displayName = "Popular Choice",
                icon = "🔥",
                color = "#FF5722", // Deep Orange
                tooltip = "Frequently chosen for similar events"
            )
            isSeasonal -> AIBadge(
                type = AIBadgeType.SEASONAL,
                displayName = "Seasonal Pick",
                icon = "🌸",
                color = "#4CAF50", // Green
                tooltip = "Perfect for this time of year"
            )
            // Badges de confiance
            confidenceScore >= 0.90 -> AIBadge(
                type = AIBadgeType.HIGH_CONFIDENCE,
                displayName = "High Confidence",
                icon = "🎯",
                color = "#4CAF50", // Material Green 500
                tooltip = "High confidence prediction (≥90%)"
            )
            confidenceScore >= 0.70 -> AIBadge(
                type = AIBadgeType.MEDIUM_CONFIDENCE,
                displayName = "Medium Confidence",
                icon = "📊",
                color = "#FF9800", // Material Orange 500
                tooltip = "Medium confidence prediction (70-90%)"
            )
            else -> AIBadge(
                type = AIBadgeType.AI_SUGGESTION,
                displayName = "AI Suggestion",
                icon = "🤖",
                color = "#6200EE", // Material Purple 500
                tooltip = "AI-generated suggestion"
            )
        }
    }

    /**
     * Convertit un DateScore en AISuggestion pour une recommandation de date.
     *
     * @param dateScore Le score de date existant
     * @param timeSlot Le créneau horaire associé (utilise TimeSlot de Event.kt)
     * @param modelVersion Version du modèle (optionnel)
     * @param reasoning Texte de raisonnement optionnel
     * @return Une suggestion IA包装 DateRecommendation
     */
    fun DateScore.toAISuggestion(
        timeSlot: TimeSlot,
        modelVersion: String = DEFAULT_MODEL_VERSION,
        reasoning: String? = null
    ): AISuggestion<DateRecommendation> {
        val recommendation = DateRecommendation(
            timeSlot = timeSlot,
            predictedAttendance = this.breakdown.socialScore, // Utiliser socialScore comme proxy
            score = this.score
        )

        val generatedReasoning = reasoning ?: generateDateReasoning(this)

        // Mapper PredictionSource vers la bonne valeur (ML_MODEL, HEURISTIC_FALLBACK, ou HYBRID)
        val mappedSource = when {
            this.confidenceScore >= 0.7 -> PredictionSource.ML_MODEL
            else -> PredictionSource.HEURISTIC_FALLBACK
        }

        return AISuggestion(
            id = UUID.randomUUID().toString(),
            data = recommendation,
            metadata = AIMetadata(
                confidenceScore = this.confidenceScore,
                predictionSource = mappedSource,
                modelVersion = modelVersion,
                featuresUsed = this.features,
                createdAt = getCurrentTimestamp()
            ),
            badge = calculateAIBadge(this.confidenceScore),
            reasoning = generatedReasoning
        )
    }

    /**
     * Convertit un LocationScore en AISuggestion pour une recommandation de lieu.
     *
     * @param locationScore Le score de lieu existant
     * @param location Le lieu potentiel associé
     * @param modelVersion Version du modèle (optionnel)
     * @param reasoning Texte de raisonnement optionnel
     * @return Une suggestion IA包装 LocationRecommendation
     */
    fun LocationScore.toAISuggestion(
        location: PotentialLocation,
        modelVersion: String = DEFAULT_MODEL_VERSION,
        reasoning: String? = null
    ): AISuggestion<LocationRecommendation> {
        val recommendation = LocationRecommendation(
            location = location,
            matchScore = this.score,
            category = SuggestionCategory.TRAVEL // Par défaut pour les lieux
        )

        val generatedReasoning = reasoning ?: generateLocationReasoning(this, location)

        // Mapper PredictionSource vers la bonne valeur
        val mappedSource = when {
            this.confidenceScore >= 0.7 -> PredictionSource.ML_MODEL
            else -> PredictionSource.HEURISTIC_FALLBACK
        }

        return AISuggestion(
            id = UUID.randomUUID().toString(),
            data = recommendation,
            metadata = AIMetadata(
                confidenceScore = this.confidenceScore,
                predictionSource = mappedSource,
                modelVersion = modelVersion,
                featuresUsed = this.features,
                createdAt = getCurrentTimestamp()
            ),
            badge = calculateAIBadge(this.confidenceScore),
            reasoning = generatedReasoning
        )
    }

    /**
     * Génère un résumé des recommandations.
     *
     * @param suggestions Liste des suggestions IA
     * @return Résumé statistique des recommandations
     */
    fun generateSummary(suggestions: List< AISuggestion<*>>): RecommendationSummary {
        val highConfidenceCount = suggestions.count {
            it.metadata.confidenceScore >= 0.9
        }
        val averageConfidence = if (suggestions.isNotEmpty()) {
            suggestions.map { it.metadata.confidenceScore }.average()
        } else 0.0
        val bestSuggestion = suggestions.maxByOrNull { it.metadata.confidenceScore }

        return RecommendationSummary(
            totalRecommendations = suggestions.size,
            highConfidenceCount = highConfidenceCount,
            averageConfidence = averageConfidence,
            bestRecommendation = bestSuggestion?.id ?: ""
        )
    }

    /**
     * Génère un texte de raisonnement pour une recommandation de date.
     *
     * @param dateScore Le score de date
     * @return Texte explicatif
     */
    private fun generateDateReasoning(dateScore: DateScore): String {
        val reasons = mutableListOf<String>()

        if (dateScore.breakdown.proximityScore >= 0.8) {
            reasons.add("matches your preferred dates")
        }
        if (dateScore.breakdown.typeMatchScore >= 0.8) {
            reasons.add("ideal for ${dateScore.features["eventType"] ?: "this type of event"}")
        }
        if (dateScore.breakdown.seasonalityScore >= 0.8) {
            reasons.add("perfect for this season")
        }
        if (dateScore.breakdown.socialScore >= 0.8) {
            reasons.add("high attendance expected")
        }

        return if (reasons.isNotEmpty()) {
            "This date is recommended because it ${reasons.joinToString(", ")}."
        } else {
            "AI analysis suggests this date based on multiple factors."
        }
    }

    /**
     * Génère un texte de raisonnement pour une recommandation de lieu.
     *
     * @param locationScore Le score de lieu
     * @param location Le lieu potentiel
     * @return Texte explicatif
     */
    private fun generateLocationReasoning(locationScore: LocationScore, location: PotentialLocation): String {
        val reasons = mutableListOf<String>()

        if (locationScore.breakdown.proximityScore >= 0.8) {
            reasons.add("easy to reach")
        }
        if (locationScore.breakdown.typeMatchScore >= 0.8) {
            reasons.add("perfect for your event type")
        }
        if (locationScore.breakdown.seasonalityScore >= 0.8) {
            reasons.add("ideal for current season")
        }

        return if (reasons.isNotEmpty()) {
            "${location.name} is recommended because it is ${reasons.joinToString(", ")}."
        } else {
            "AI analysis suggests ${location.name} based on multiple factors."
        }
    }

    /**
     * Génère le timestamp ISO 8601 actuel.
     *
     * @return Timestamp formaté ISO 8601
     */
    private fun getCurrentTimestamp(): String {
        val now = Clock.System.now()
        val localDateTime = now.toLocalDateTime(TimeZone.UTC)
        return "${localDateTime.year}-${localDateTime.monthNumber.toString().padStart(2, '0')}-${localDateTime.dayOfMonth.toString().padStart(2, '0')}T${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}:${localDateTime.second.toString().padStart(2, '0')}Z"
    }
}
