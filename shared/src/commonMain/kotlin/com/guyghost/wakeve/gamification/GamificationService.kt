package com.guyghost.wakeve.gamification

import com.guyghost.wakeve.gamification.repository.UserBadgesRepository
import com.guyghost.wakeve.gamification.repository.UserPointsRepository

/**
 * Service for managing gamification features including points, badges, and leaderboards.
 *
 * This service handles:
 * - Awarding points for user actions
 * - Checking and unlocking badges
 * - Calculating point decay over time
 * - Generating leaderboard rankings
 */
class GamificationService(
    private val userPointsRepository: UserPointsRepository,
    private val userBadgesRepository: UserBadgesRepository,
    private val badgeEligibilityChecker: BadgeEligibilityChecker
) {
    companion object {
        /** Points awarded for creating an event */
        const val POINTS_CREATE_EVENT = 50

        /** Points awarded for voting on a poll */
        const val POINTS_VOTE = 5

        /** Points awarded for commenting */
        const val POINTS_COMMENT = 10

        /** Points awarded for participating in an event */
        const val POINTS_PARTICIPATE = 20

        /** Points awarded for creating a scenario */
        const val POINTS_CREATE_SCENARIO = 15

        /** Points awarded for voting on a scenario */
        const val POINTS_VOTE_SCENARIO = 3

        /** Days before point decay starts */
        const val DECAY_START_DAYS = 30

        /** Daily decay rate (1%) */
        const val DECAY_RATE = 0.01
    }

    /**
     * Gets the current points for a user.
     *
     * @param userId The user to get points for
     * @return UserPoints or null if user doesn't exist
     */
    suspend fun getUserPoints(userId: String): UserPoints? {
        return userPointsRepository.getUserPoints(userId)
    }

    /**
     * Awards points to a user for a specific action.
     *
     * @param userId The user to award points to
     * @param action The action that earned points
     * @param eventId Optional event ID related to the action
     * @return AwardResult containing points earned and any new badges
     */
    suspend fun awardPoints(
        userId: String,
        action: PointsAction,
        eventId: String? = null
    ): AwardResult {
        val points = getPointsForAction(action)

        // Increment points based on action type
        when (action) {
            PointsAction.CREATE_EVENT -> {
                userPointsRepository.incrementEventCreationPoints(userId, points)
            }
            PointsAction.VOTE -> {
                userPointsRepository.incrementVotingPoints(userId, points)
            }
            PointsAction.COMMENT -> {
                userPointsRepository.incrementCommentPoints(userId, points)
            }
            PointsAction.PARTICIPATE -> {
                userPointsRepository.incrementParticipationPoints(userId, points)
            }
            PointsAction.CREATE_SCENARIO -> {
                userPointsRepository.incrementEventCreationPoints(userId, points)
            }
            PointsAction.VOTE_SCENARIO -> {
                userPointsRepository.incrementVotingPoints(userId, points)
            }
        }

        val newTotal = userPointsRepository.getTotalPoints(userId)
        val newlyUnlocked = badgeEligibilityChecker.checkEligibility(userId)

        // Unlock badges
        newlyUnlocked.forEach { badge ->
            userBadgesRepository.unlockBadge(userId, badge.id)
        }

        return AwardResult(
            pointsEarned = points,
            newTotal = newTotal,
            badgesUnlocked = newlyUnlocked
        )
    }

    /**
     * Gets the points value for a specific action.
     *
     * @param action The action to get points for
     * @return Number of points awarded for the action
     */
    fun getPointsForAction(action: PointsAction): Int {
        return when (action) {
            PointsAction.CREATE_EVENT -> POINTS_CREATE_EVENT
            PointsAction.VOTE -> POINTS_VOTE
            PointsAction.COMMENT -> POINTS_COMMENT
            PointsAction.PARTICIPATE -> POINTS_PARTICIPATE
            PointsAction.CREATE_SCENARIO -> POINTS_CREATE_SCENARIO
            PointsAction.VOTE_SCENARIO -> POINTS_VOTE_SCENARIO
        }
    }

    /**
     * Applies point decay to a user's points based on inactivity.
     * Points decay at 1% per day after 30 days of inactivity.
     *
     * @param userId The user to apply decay to
     * @return Updated UserPoints or null if no decay applied
     */
    suspend fun applyPointsDecay(userId: String): UserPoints? {
        return userPointsRepository.applyPointsDecay(userId)
    }

    /**
     * Gets all badges for a user.
     *
     * @param userId The user to get badges for
     * @return List of badges the user has earned
     */
    suspend fun getUserBadges(userId: String): List<Badge> {
        return userBadgesRepository.getUserBadges(userId).badges
    }

    /**
     * Unlocks a specific badge for a user.
     *
     * @param userId The user to award the badge to
     * @param badgeId The badge to unlock
     * @return UnlockResult containing the result of the unlock attempt
     */
    suspend fun unlockBadge(userId: String, badgeId: String): UnlockResult {
        val alreadyUnlocked = userBadgesRepository.userHasBadge(userId, badgeId)
        if (alreadyUnlocked) {
            return UnlockResult(
                unlocked = false,
                message = "Vous avez déjà ce badge",
                pointsReward = 0
            )
        }

        val unlocked = userBadgesRepository.unlockBadge(userId, badgeId)
        if (!unlocked) {
            return UnlockResult(
                unlocked = false,
                message = "Badge non trouvé",
                pointsReward = 0
            )
        }

        // Award bonus points for badge
        val badge = userBadgesRepository.getAllBadgeDefinitions()
            .find { it.id == badgeId }
        val pointsReward = badge?.pointsReward ?: 0

        if (pointsReward > 0) {
            userPointsRepository.incrementParticipationPoints(userId, pointsReward)
        }

        return UnlockResult(
            unlocked = true,
            message = "Félicitations! Vous avez débloqué le badge '${badge?.name}'!",
            pointsReward = pointsReward
        )
    }

    /**
     * Checks badge eligibility for a user.
     *
     * @param userId The user to check
     * @return List of badges the user is eligible to unlock
     */
    suspend fun checkBadgeEligibility(userId: String): List<Badge> {
        return badgeEligibilityChecker.checkEligibility(userId)
    }

    /**
     * Gets the leaderboard for a specific type.
     *
     * @param type The type of leaderboard (ALL_TIME, THIS_MONTH, THIS_WEEK, FRIENDS)
     * @param limit Maximum number of entries to return
     * @param currentUserId The current user's ID (for highlighting)
     * @param friendIds List of friend user IDs (for FRIENDS filter)
     * @return List of leaderboard entries
     */
    suspend fun getLeaderboard(
        type: LeaderboardType,
        limit: Int = 20,
        currentUserId: String? = null,
        friendIds: List<String> = emptyList()
    ): List<LeaderboardEntry> {
        val topEarners = userPointsRepository.getTopPointEarners(100)

        val filteredAndRanked = when (type) {
            LeaderboardType.ALL_TIME -> {
                topEarners.mapIndexed { index, points ->
                    createLeaderboardEntry(points, index + 1, currentUserId, friendIds)
                }
            }
            LeaderboardType.THIS_MONTH, LeaderboardType.THIS_WEEK -> {
                // For now, use all-time data
                // In real implementation, filter by date
                topEarners.mapIndexed { index, points ->
                    createLeaderboardEntry(points, index + 1, currentUserId, friendIds)
                }
            }
            LeaderboardType.FRIENDS -> {
                topEarners
                    .filter { friendIds.isEmpty() || it.userId in friendIds }
                    .mapIndexed { index, points ->
                        createLeaderboardEntry(points, index + 1, currentUserId, friendIds)
                    }
            }
        }

        return filteredAndRanked.take(limit)
    }

    /**
     * Gets a user's rank on a specific leaderboard.
     *
     * @param userId The user to get rank for
     * @param type The type of leaderboard
     * @param friendIds List of friend user IDs (for FRIENDS filter)
     * @return The user's rank or null if not ranked
     */
    suspend fun getUserRank(
        userId: String,
        type: LeaderboardType,
        friendIds: List<String> = emptyList()
    ): Int? {
        val leaderboard = getLeaderboard(type, 100, userId, friendIds)
        return leaderboard.find { it.userId == userId }?.rank
    }

    /**
     * Creates a leaderboard entry from user points.
     */
    private suspend fun createLeaderboardEntry(
        points: UserPoints,
        rank: Int,
        currentUserId: String?,
        friendIds: List<String>
    ): LeaderboardEntry {
        val userBadges = userBadgesRepository.getUserBadges(points.userId)

        return LeaderboardEntry(
            userId = points.userId,
            username = "Utilisateur",
            totalPoints = points.totalPoints,
            badgesCount = userBadges.badges.size,
            rank = rank,
            isCurrentUser = points.userId == currentUserId,
            isFriend = points.userId in friendIds,
            legendaryCount = userBadges.countByRarity(BadgeRarity.LEGENDARY),
            epicCount = userBadges.countByRarity(BadgeRarity.EPIC)
        )
    }

    /**
     * Gets statistics about gamification usage.
     *
     * @return Map containing gamification statistics
     */
    suspend fun getGamificationStatistics(): Map<String, Any> {
        val pointsStats = userPointsRepository.getPointsStatistics()
        return mapOf(
            "totalUsersWithPoints" to (pointsStats["userCount"] ?: 0L),
            "totalPointsDistributed" to (pointsStats["totalPoints"] ?: 0L),
            "averagePointsPerUser" to (pointsStats["averagePoints"] ?: 0L),
            "topPoints" to (pointsStats["maxPoints"] ?: 0L)
        )
    }
}
