package com.guyghost.wakeve.gamification

import kotlinx.serialization.Serializable

/**
 * Represents a user's points across different activity categories.
 *
 * @property userId The unique identifier of the user
 * @property totalPoints Total points including all categories and adjustments
 * @property eventCreationPoints Points earned from creating events
 * @property votingPoints Points earned from voting on polls
 * @property commentPoints Points earned from commenting on scenarios
 * @property participationPoints Points earned from participating in events
 * @property decayPoints Total points lost due to decay over time
 * @property lastUpdated ISO 8601 UTC timestamp of last update
 */
@Serializable
data class UserPoints(
    val userId: String,
    val totalPoints: Int = 0,
    val eventCreationPoints: Int = 0,
    val votingPoints: Int = 0,
    val commentPoints: Int = 0,
    val participationPoints: Int = 0,
    val decayPoints: Int = 0,
    val lastUpdated: String = ""
) {
    /**
     * Calculates the effective points after applying decay.
     * Effective points = earned points - decayed points.
     */
    val effectivePoints: Int
        get() = totalPoints + decayPoints
}
