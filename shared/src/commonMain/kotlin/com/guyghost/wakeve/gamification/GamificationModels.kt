package com.guyghost.wakeve.gamification

import kotlinx.serialization.Serializable

/**
 * Actions that can earn points in the gamification system.
 * Each action has an associated point value defined in [GamificationService].
 */
@Serializable
enum class PointsAction {
    CREATE_EVENT,
    VOTE,
    COMMENT,
    PARTICIPATE,
    CREATE_SCENARIO,
    VOTE_SCENARIO,
    INVITE_PARTICIPANT
}

/**
 * Result of awarding points to a user.
 *
 * @property pointsEarned Number of points earned from the action
 * @property newTotal User's new total points after awarding
 * @property badgesUnlocked List of new badges unlocked by this action
 */
data class AwardResult(
    val pointsEarned: Int,
    val newTotal: Int,
    val badgesUnlocked: List<Badge>
)

/**
 * Result of attempting to unlock a badge.
 *
 * @property unlocked Whether the badge was successfully unlocked
 * @property message Descriptive message about the unlock result
 * @property pointsReward Points awarded for unlocking the badge
 */
data class UnlockResult(
    val unlocked: Boolean,
    val message: String,
    val pointsReward: Int
)

/**
 * Represents a user's level based on accumulated points.
 * Levels provide a visual progression indicator.
 *
 * @property level Current level number (1-based)
 * @property name Display name for the level (in French)
 * @property currentPoints User's current total points
 * @property pointsForCurrentLevel Points needed to reach the current level
 * @property pointsForNextLevel Points needed to reach the next level
 * @property progressToNextLevel Progress fraction (0.0 to 1.0) towards next level
 */
@Serializable
data class UserLevel(
    val level: Int,
    val name: String,
    val nameKey: String,
    val currentPoints: Int,
    val pointsForCurrentLevel: Int,
    val pointsForNextLevel: Int,
    val progressToNextLevel: Float
) {
    companion object {
        /** Level thresholds: list of (points required, localization key) */
        val LEVEL_THRESHOLDS: List<Pair<Int, String>> = listOf(
            0 to "level.beginner",
            50 to "level.explorer",
            150 to "level.contributor",
            300 to "level.organizer",
            500 to "level.expert",
            800 to "level.master",
            1200 to "level.champion",
            1800 to "level.legend",
            2500 to "level.mythic",
            3500 to "level.transcendent"
        )

        /** Localized display names for each level key, keyed by language code. */
        val displayNames: Map<String, Map<String, String>> = mapOf(
            "level.beginner" to mapOf(
                "en" to "Beginner", "fr" to "Débutant", "es" to "Principiante",
                "it" to "Principiante", "pt" to "Iniciante"
            ),
            "level.explorer" to mapOf(
                "en" to "Explorer", "fr" to "Explorateur", "es" to "Explorador",
                "it" to "Esploratore", "pt" to "Explorador"
            ),
            "level.contributor" to mapOf(
                "en" to "Contributor", "fr" to "Contributeur", "es" to "Contribuidor",
                "it" to "Collaboratore", "pt" to "Contribuidor"
            ),
            "level.organizer" to mapOf(
                "en" to "Organizer", "fr" to "Organisateur", "es" to "Organizador",
                "it" to "Organizzatore", "pt" to "Organizador"
            ),
            "level.expert" to mapOf(
                "en" to "Expert", "fr" to "Expert", "es" to "Experto",
                "it" to "Esperto", "pt" to "Especialista"
            ),
            "level.master" to mapOf(
                "en" to "Master", "fr" to "Maître", "es" to "Maestro",
                "it" to "Maestro", "pt" to "Mestre"
            ),
            "level.champion" to mapOf(
                "en" to "Champion", "fr" to "Champion", "es" to "Campeón",
                "it" to "Campione", "pt" to "Campeão"
            ),
            "level.legend" to mapOf(
                "en" to "Legend", "fr" to "Légende", "es" to "Leyenda",
                "it" to "Leggenda", "pt" to "Lenda"
            ),
            "level.mythic" to mapOf(
                "en" to "Mythic", "fr" to "Mythique", "es" to "Mítico",
                "it" to "Mitico", "pt" to "Mítico"
            ),
            "level.transcendent" to mapOf(
                "en" to "Transcendent", "fr" to "Transcendant", "es" to "Trascendente",
                "it" to "Trascendente", "pt" to "Transcendente"
            )
        )

        /**
         * Resolves a level name key to a localized display name.
         *
         * @param key The level key (e.g., "level.beginner")
         * @param locale The locale code ("en", "fr", "es", "it", "pt")
         * @return The localized level name, falling back to French then to the key.
         */
        fun localizedName(key: String, locale: String = "fr"): String {
            return displayNames[key]?.get(locale)
                ?: displayNames[key]?.get("fr")
                ?: key
        }

        /**
         * Calculates the user level from total points.
         */
        fun fromPoints(totalPoints: Int, locale: String = "fr"): UserLevel {
            var currentLevelIndex = 0
            for (i in LEVEL_THRESHOLDS.indices) {
                if (totalPoints >= LEVEL_THRESHOLDS[i].first) {
                    currentLevelIndex = i
                } else {
                    break
                }
            }

            val currentThreshold = LEVEL_THRESHOLDS[currentLevelIndex].first
            val nextThreshold = if (currentLevelIndex + 1 < LEVEL_THRESHOLDS.size) {
                LEVEL_THRESHOLDS[currentLevelIndex + 1].first
            } else {
                // Max level: use a high number
                currentThreshold + 1000
            }

            val pointsInLevel = totalPoints - currentThreshold
            val pointsNeeded = nextThreshold - currentThreshold
            val progress = if (pointsNeeded > 0) {
                (pointsInLevel.toFloat() / pointsNeeded.toFloat()).coerceIn(0f, 1f)
            } else {
                1f
            }

            val nameKey = LEVEL_THRESHOLDS[currentLevelIndex].second

            return UserLevel(
                level = currentLevelIndex + 1,
                name = localizedName(nameKey, locale),
                nameKey = nameKey,
                currentPoints = totalPoints,
                pointsForCurrentLevel = currentThreshold,
                pointsForNextLevel = nextThreshold,
                progressToNextLevel = progress
            )
        }
    }
}
