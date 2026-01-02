package com.guyghost.wakeve.gamification

import kotlinx.serialization.Serializable

/**
 * Represents an entry in the leaderboard ranking.
 *
 * @property userId The unique identifier of the user
 * @property username Display name of the user
 * @property totalPoints Total points the user has earned
 * @property badgesCount Number of badges the user has unlocked
 * @property rank Current rank on the leaderboard
 * @property isCurrentUser Whether this entry belongs to the current user
 * @property isFriend Whether this user is a friend of the current user
 * @property legendaryCount Number of legendary badges (for tiebreakers)
 * @property epicCount Number of epic badges (for tiebreakers)
 */
@Serializable
data class LeaderboardEntry(
    val userId: String,
    val username: String,
    val totalPoints: Int,
    val badgesCount: Int,
    val rank: Int,
    val isCurrentUser: Boolean = false,
    val isFriend: Boolean = false,
    val legendaryCount: Int = 0,
    val epicCount: Int = 0
) {
    companion object {
        /**
         * Creates a placeholder entry for anonymous users.
         */
        fun anonymous(rank: Int): LeaderboardEntry {
            return LeaderboardEntry(
                userId = "anonymous",
                username = "Anonyme",
                totalPoints = 0,
                badgesCount = 0,
                rank = rank
            )
        }
    }
}

@Serializable
enum class LeaderboardType {
    ALL_TIME,
    THIS_MONTH,
    THIS_WEEK,
    FRIENDS
}
