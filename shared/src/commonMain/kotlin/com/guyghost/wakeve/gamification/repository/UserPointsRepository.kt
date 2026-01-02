package com.guyghost.wakeve.gamification.repository

import com.guyghost.wakeve.gamification.UserPoints

/**
 * Repository interface for managing user points.
 * Provides an abstraction layer for points data access.
 */
interface UserPointsRepository {
    suspend fun getUserPoints(userId: String): UserPoints?
    suspend fun getUserPointsOrDefault(userId: String): UserPoints
    suspend fun createUserPoints(userId: String): UserPoints
    suspend fun incrementEventCreationPoints(userId: String, points: Int): UserPoints
    suspend fun incrementVotingPoints(userId: String, points: Int): UserPoints
    suspend fun incrementCommentPoints(userId: String, points: Int): UserPoints
    suspend fun incrementParticipationPoints(userId: String, points: Int): UserPoints
    suspend fun applyPointsDecay(userId: String): UserPoints?
    suspend fun getTopPointEarners(limit: Int = 20): List<UserPoints>
    suspend fun getPointsStatistics(): Map<String, Long>
    suspend fun userHasMinimumPoints(userId: String, minimumPoints: Int): Boolean
    suspend fun getTotalPoints(userId: String): Int
    suspend fun deleteUserPoints(userId: String)
}

/**
 * In-memory implementation of UserPointsRepository.
 * Uses a simple Map for storage, suitable for testing and development.
 */
class InMemoryUserPointsRepository : UserPointsRepository {
    private val pointsMap = mutableMapOf<String, UserPoints>()

    override suspend fun getUserPoints(userId: String): UserPoints? {
        return pointsMap[userId]
    }

    override suspend fun getUserPointsOrDefault(userId: String): UserPoints {
        return pointsMap[userId] ?: createUserPoints(userId)
    }

    override suspend fun createUserPoints(userId: String): UserPoints {
        val userPoints = UserPoints(
            userId = userId,
            totalPoints = 0,
            eventCreationPoints = 0,
            votingPoints = 0,
            commentPoints = 0,
            participationPoints = 0,
            decayPoints = 0,
            lastUpdated = ""
        )
        pointsMap[userId] = userPoints
        return userPoints
    }

    override suspend fun incrementEventCreationPoints(userId: String, points: Int): UserPoints {
        val current = getUserPointsOrDefault(userId)
        val updated = current.copy(
            eventCreationPoints = current.eventCreationPoints + points,
            totalPoints = current.totalPoints + points,
            lastUpdated = ""
        )
        pointsMap[userId] = updated
        return updated
    }

    override suspend fun incrementVotingPoints(userId: String, points: Int): UserPoints {
        val current = getUserPointsOrDefault(userId)
        val updated = current.copy(
            votingPoints = current.votingPoints + points,
            totalPoints = current.totalPoints + points,
            lastUpdated = ""
        )
        pointsMap[userId] = updated
        return updated
    }

    override suspend fun incrementCommentPoints(userId: String, points: Int): UserPoints {
        val current = getUserPointsOrDefault(userId)
        val updated = current.copy(
            commentPoints = current.commentPoints + points,
            totalPoints = current.totalPoints + points,
            lastUpdated = ""
        )
        pointsMap[userId] = updated
        return updated
    }

    override suspend fun incrementParticipationPoints(userId: String, points: Int): UserPoints {
        val current = getUserPointsOrDefault(userId)
        val updated = current.copy(
            participationPoints = current.participationPoints + points,
            totalPoints = current.totalPoints + points,
            lastUpdated = ""
        )
        pointsMap[userId] = updated
        return updated
    }

    override suspend fun applyPointsDecay(userId: String): UserPoints? {
        val current = getUserPoints(userId) ?: return null
        if (current.totalPoints <= 0) return current

        val decayAmount = (current.totalPoints * 0.01).toInt().coerceAtLeast(1)
        val newTotal = (current.totalPoints - decayAmount).coerceAtLeast(0)
        val updated = current.copy(
            totalPoints = newTotal,
            decayPoints = current.decayPoints + decayAmount,
            lastUpdated = ""
        )
        pointsMap[userId] = updated
        return updated
    }

    override suspend fun getTopPointEarners(limit: Int): List<UserPoints> {
        return pointsMap.values
            .sortedByDescending { it.totalPoints }
            .take(limit)
    }

    override suspend fun getPointsStatistics(): Map<String, Long> {
        val values = pointsMap.values
        return mapOf(
            "userCount" to values.size.toLong(),
            "totalPoints" to values.sumOf { it.totalPoints.toLong() },
            "averagePoints" to if (values.isNotEmpty()) values.sumOf { it.totalPoints }.toLong() / values.size.toLong() else 0L,
            "maxPoints" to (values.maxOfOrNull { it.totalPoints } ?: 0).toLong(),
            "minPoints" to (values.minOfOrNull { it.totalPoints } ?: 0).toLong()
        )
    }

    override suspend fun userHasMinimumPoints(userId: String, minimumPoints: Int): Boolean {
        return (getUserPoints(userId)?.totalPoints ?: 0) >= minimumPoints
    }

    override suspend fun getTotalPoints(userId: String): Int {
        return getUserPoints(userId)?.totalPoints ?: 0
    }

    override suspend fun deleteUserPoints(userId: String) {
        pointsMap.remove(userId)
    }
}
