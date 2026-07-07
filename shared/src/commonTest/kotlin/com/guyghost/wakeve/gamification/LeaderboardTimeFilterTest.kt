package com.guyghost.wakeve.gamification

import com.guyghost.wakeve.gamification.repository.InMemoryUserBadgesRepository
import com.guyghost.wakeve.gamification.repository.InMemoryUserPointsRepository
import com.guyghost.wakeve.gamification.repository.UserPointsRepository
import com.guyghost.wakeve.repository.InMemoryLeaderboardRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LeaderboardTimeFilterTest {
    @Test
    fun leaderboardRepositoryFiltersWeeklyEntriesByLastUpdated() = runTest {
        val repository = InMemoryLeaderboardRepository(
            userPointsRepository = FixedUserPointsRepository(
                listOf(
                    points("recent-high", total = 100, ageDays = 2),
                    points("old-higher", total = 200, ageDays = 10),
                    points("invalid", total = 300, lastUpdated = "not-a-date")
                )
            ),
            userBadgesRepository = InMemoryUserBadgesRepository(),
            getUsername = { it }
        )

        val weekly = repository.getLeaderboard(LeaderboardType.THIS_WEEK)
        val allTime = repository.getLeaderboard(LeaderboardType.ALL_TIME)

        assertEquals(listOf("recent-high"), weekly.map { it.userId })
        assertEquals(listOf("invalid", "old-higher", "recent-high"), allTime.map { it.userId })
    }

    @Test
    fun leaderboardRepositoryFiltersMonthlyEntriesByLastUpdated() = runTest {
        val repository = InMemoryLeaderboardRepository(
            userPointsRepository = FixedUserPointsRepository(
                listOf(
                    points("this-month", total = 70, ageDays = 12),
                    points("last-month", total = 80, ageDays = 45),
                    points("missing-date", total = 90, lastUpdated = "")
                )
            ),
            userBadgesRepository = InMemoryUserBadgesRepository(),
            getUsername = { it }
        )

        val monthly = repository.getLeaderboard(LeaderboardType.THIS_MONTH)

        assertEquals(listOf("this-month"), monthly.map { it.userId })
    }

    @Test
    fun gamificationServiceDoesNotUseAllTimeDataForRecentLeaderboards() = runTest {
        val pointsRepository = FixedUserPointsRepository(
            listOf(
                points("recent", total = 50, ageDays = 1),
                points("old", total = 500, ageDays = 40)
            )
        )
        val badgesRepository = InMemoryUserBadgesRepository()
        val service = GamificationService(
            userPointsRepository = pointsRepository,
            userBadgesRepository = badgesRepository,
            badgeEligibilityChecker = BadgeEligibilityChecker(pointsRepository, badgesRepository)
        )

        val weekly = service.getLeaderboard(LeaderboardType.THIS_WEEK)
        val allTime = service.getLeaderboard(LeaderboardType.ALL_TIME)

        assertEquals(listOf("recent"), weekly.map { it.userId })
        assertEquals(listOf("old", "recent"), allTime.map { it.userId })
    }

    @Test
    fun inMemoryPointsRepositoryWritesParseableLastUpdatedTimestamps() = runTest {
        val repository = InMemoryUserPointsRepository()

        val created = repository.createUserPoints("created")
        val updated = repository.incrementVotingPoints("created", 5)

        assertFalse(created.lastUpdated.isBlank())
        assertFalse(updated.lastUpdated.isBlank())
        assertTrue(runCatching { Instant.parse(created.lastUpdated) }.isSuccess)
        assertTrue(runCatching { Instant.parse(updated.lastUpdated) }.isSuccess)
    }

    private fun points(
        userId: String,
        total: Int,
        ageDays: Long? = null,
        lastUpdated: String = timestampDaysAgo(ageDays ?: 0)
    ): UserPoints {
        return UserPoints(
            userId = userId,
            totalPoints = total,
            lastUpdated = lastUpdated
        )
    }

    private class FixedUserPointsRepository(
        initialPoints: List<UserPoints>
    ) : UserPointsRepository {
        private val pointsByUser = initialPoints.associateBy { it.userId }.toMutableMap()

        override suspend fun getUserPoints(userId: String): UserPoints? = pointsByUser[userId]

        override suspend fun getUserPointsOrDefault(userId: String): UserPoints {
            return pointsByUser[userId] ?: UserPoints(userId = userId).also {
                pointsByUser[userId] = it
            }
        }

        override suspend fun createUserPoints(userId: String): UserPoints {
            return UserPoints(userId = userId, lastUpdated = timestampDaysAgo(0)).also {
                pointsByUser[userId] = it
            }
        }

        override suspend fun incrementEventCreationPoints(userId: String, points: Int): UserPoints {
            return increment(userId, points) { current, total ->
                current.copy(eventCreationPoints = current.eventCreationPoints + points, totalPoints = total)
            }
        }

        override suspend fun incrementVotingPoints(userId: String, points: Int): UserPoints {
            return increment(userId, points) { current, total ->
                current.copy(votingPoints = current.votingPoints + points, totalPoints = total)
            }
        }

        override suspend fun incrementCommentPoints(userId: String, points: Int): UserPoints {
            return increment(userId, points) { current, total ->
                current.copy(commentPoints = current.commentPoints + points, totalPoints = total)
            }
        }

        override suspend fun incrementParticipationPoints(userId: String, points: Int): UserPoints {
            return increment(userId, points) { current, total ->
                current.copy(participationPoints = current.participationPoints + points, totalPoints = total)
            }
        }

        override suspend fun applyPointsDecay(userId: String): UserPoints? = pointsByUser[userId]

        override suspend fun getTopPointEarners(limit: Int): List<UserPoints> {
            return pointsByUser.values.sortedByDescending { it.totalPoints }.take(limit)
        }

        override suspend fun getPointsStatistics(): Map<String, Long> = emptyMap()

        override suspend fun userHasMinimumPoints(userId: String, minimumPoints: Int): Boolean {
            return (pointsByUser[userId]?.totalPoints ?: 0) >= minimumPoints
        }

        override suspend fun getTotalPoints(userId: String): Int = pointsByUser[userId]?.totalPoints ?: 0

        override suspend fun deleteUserPoints(userId: String) {
            pointsByUser.remove(userId)
        }

        private fun increment(
            userId: String,
            points: Int,
            apply: (UserPoints, Int) -> UserPoints
        ): UserPoints {
            val current = pointsByUser[userId] ?: UserPoints(userId = userId)
            val updated = apply(current, current.totalPoints + points).copy(lastUpdated = timestampDaysAgo(0))
            pointsByUser[userId] = updated
            return updated
        }
    }
}

private fun timestampDaysAgo(days: Long): String {
    val timestamp = Clock.System.now().toEpochMilliseconds() - days * 24L * 60L * 60L * 1000L
    return Instant.fromEpochMilliseconds(timestamp).toString()
}
