package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Recommandation de date enrichie pour l'affichage UI.
 * Contient le créneau horaire, le taux de participation prédit et le score global.
 *
 * @property timeSlot Le créneau horaire recommandé (utilise le TimeSlot de Event.kt)
 * @property predictedAttendance Taux de participation prédit (0.0 - 1.0)
 * @property score Score global de la recommandation (0.0 - 1.0)
 */
@Serializable
data class DateRecommendation(
    val timeSlot: TimeSlot,
    val predictedAttendance: Double,
    val score: Double
)

/**
 * Recommandation de lieu enrichie pour l'affichage UI.
 * Contient le lieu potentiel, le score de correspondance et la catégorie.
 *
 * @property location Le lieu potentiel recommandé
 * @property matchScore Score de correspondance avec les préférences (0.0 - 1.0)
 * @property category Catégorie de la suggestion (utilise SuggestionCategory de EventType.kt)
 */
@Serializable
data class LocationRecommendation(
    val location: PotentialLocation,
    val matchScore: Double,
    val category: SuggestionCategory
)

/**
 * Résumé statistique de l'ensemble des recommandations générées.
 * Utile pour l'affichage d'aperçu rapide dans l'UI.
 *
 * @property totalRecommendations Nombre total de suggestions générées
 * @property highConfidenceCount Nombre de suggestions à haute confiance
 * @property averageConfidence Score de confiance moyen de toutes les suggestions
 * @property bestRecommendation Identifiant de la meilleure recommandation
 */
@Serializable
data class RecommendationSummary(
    val totalRecommendations: Int,
    val highConfidenceCount: Int,
    val averageConfidence: Double,
    val bestRecommendation: String
)
