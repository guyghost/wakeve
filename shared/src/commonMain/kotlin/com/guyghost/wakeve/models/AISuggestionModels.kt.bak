package com.guyghost.wakeve.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Wrapper générique pour les suggestions générées par l'IA.
 * Contient les données de la suggestion ainsi que les métadonnées de confiance et de provenance.
 *
 * @param T Type de donnée suggérée (TimeSlot, PotentialLocation, Activity, etc.)
 * @property id Identifiant unique de la suggestion
 * @property data Les données de la suggestion (ex: TimeSlot, PotentialLocation)
 * @property metadata Métadonnées de confiance et de provenance du modèle
 * @property badge Badge visuel indiquant le type de suggestion IA
 * @property reasoning Texte optionnel expliquant pourquoi cette suggestion a été faite
 */
@Serializable
data class AISuggestion<T>(
    val id: String,
    val data: T,
    val metadata: AIMetadata,
    val badge: AIBadge,
    val reasoning: String? = null
)

/**
 * Métadonnées associées à une prédiction IA.
 * Contient le score de confiance, la source du modèle, la version et les features utilisées.
 *
 * @property confidenceScore Score de confiance de la prédiction (0.0 - 1.0)
 * @property predictionSource Source de la prédiction (ML, HEURISTIC, HYBRID)
 * @property modelVersion Version du modèle utilisé (pour A/B testing)
 * @property featuresUsed Map des features utilisées pour cette prédiction (avec @Contextual pour Any)
 * @property createdAt Timestamp ISO 8601 de création de la suggestion
 */
@Serializable
data class AIMetadata(
    val confidenceScore: Double,
    val predictionSource: PredictionSource,
    val modelVersion: String,
    @Contextual val featuresUsed: Map<String, Any>,
    val createdAt: String
) {
    init {
        require(confidenceScore in 0.0..1.0) { "Confidence score must be between 0.0 and 1.0" }
    }
}

/**
 * Badge visuel pour afficher le type de suggestion IA dans l'UI.
 * Chaque type de badge a un nom d'affichage, une icône et une couleur associés.
 *
 * @property type Type de badge (AI_SUGGESTION, HIGH_CONFIDENCE, etc.)
 * @property displayName Nom à afficher dans l'UI (ex: "High Confidence")
 * @property icon Icône ou emoji à afficher (ex: "🎯", "🤖")
 * @property color Couleur hexadécimale du badge (ex: "#6200EE" pour Material Purple)
 * @property tooltip Texte d'aide optionnel au survol
 */
@Serializable
data class AIBadge(
    val type: AIBadgeType,
    val displayName: String,
    val icon: String,
    val color: String,
    val tooltip: String? = null
) {
    init {
        require(color.matches(Regex("^#[0-9A-Fa-f]{6}$"))) { "Color must be a valid hex color (e.g., #6200EE)" }
    }
}

/**
 * Types de badges IA disponibles.
 *
 * - AI_SUGGESTION: Badge principal pour les suggestions générées par IA
 * - HIGH_CONFIDENCE: Confiance >= 90% (vert)
 * - MEDIUM_CONFIDENCE: Confiance 70-90% (orange)
 * - PERSONALIZED: Suggestion personnalisée basée sur l'historique utilisateur
 * - POPULAR_CHOICE: Choix populaire auprès d'événements similaires
 * - SEASONAL: Suggestion saisonnière recommandée
 */
@Serializable
enum class AIBadgeType {
    /** Badge principal pour les suggestions générées par l'IA */
    AI_SUGGESTION,

    /** Haute confiance (>= 90%), affiché en vert avec cible 🎯 */
    HIGH_CONFIDENCE,

    /** Confiance moyenne (70-90%), affiché en orange avec graphique 📊 */
    MEDIUM_CONFIDENCE,

    /** Suggestion personnalisée pour cet utilisateur spécifique */
    PERSONALIZED,

    /** Choix populaire auprès d'événements similaires */
    POPULAR_CHOICE,

    /** Suggestion saisonnière recommandée */
    SEASONAL
}
