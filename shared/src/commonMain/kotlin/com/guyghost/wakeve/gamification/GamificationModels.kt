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
    val currentPoints: Int,
    val pointsForCurrentLevel: Int,
    val pointsForNextLevel: Int,
    val progressToNextLevel: Float
) {
    companion object {
        /** Level thresholds: list of (points required, level name) */
        val LEVEL_THRESHOLDS: List<Pair<Int, String>> = listOf(
            0 to "Debutant",
            50 to "Explorateur",
            150 to "Contributeur",
            300 to "Organisateur",
            500 to "Expert",
            800 to "Maitre",
            1200 to "Champion",
            1800 to "Legende",
            2500 to "Mythique",
            3500 to "Transcendant"
        )

        /**
         * Calculates the user level from total points.
         */
        fun fromPoints(totalPoints: Int): UserLevel {
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

            return UserLevel(
                level = currentLevelIndex + 1,
                name = LEVEL_THRESHOLDS[currentLevelIndex].second,
                currentPoints = totalPoints,
                pointsForCurrentLevel = currentThreshold,
                pointsForNextLevel = nextThreshold,
                progressToNextLevel = progress
            )
        }
    }
}
