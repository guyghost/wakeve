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
    VOTE_SCENARIO
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
