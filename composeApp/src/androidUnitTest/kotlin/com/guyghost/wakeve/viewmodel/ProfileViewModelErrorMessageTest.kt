package com.guyghost.wakeve.viewmodel

import com.guyghost.wakeve.gamification.BadgeEligibilityChecker
import com.guyghost.wakeve.gamification.GamificationService
import com.guyghost.wakeve.gamification.UserPoints
import com.guyghost.wakeve.gamification.repository.InMemoryUserBadgesRepository
import com.guyghost.wakeve.gamification.repository.UserPointsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelErrorMessageTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadProfileDataUsesGenericMessageWhenGamificationFails() = runTest {
        val userPointsRepository = FailingUserPointsRepository()
        val userBadgesRepository = InMemoryUserBadgesRepository()
        val viewModel = ProfileViewModel(
            currentUserId = "user-123",
            gamificationService = GamificationService(
                userPointsRepository = userPointsRepository,
                userBadgesRepository = userBadgesRepository,
                badgeEligibilityChecker = BadgeEligibilityChecker(
                    userPointsRepository = userPointsRepository,
                    userBadgesRepository = userBadgesRepository
                )
            ),
            userBadgesRepository = userBadgesRepository
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(profileLoadFailureMessage(), state.error)
        assertEquals(0, state.userPoints?.totalPoints)
        assertEquals(com.guyghost.wakeve.gamification.UserLevel.fromPoints(0), state.userLevel)
        assertTrue(state.userBadges.isEmpty())
        assertTrue(state.allBadges.isEmpty())
        assertTrue(state.leaderboard.isEmpty())
        assertDoesNotExposeSensitiveDetails(state.error.orEmpty())
    }

    @Test
    fun profileLoadFailureMessageUsesStableSafeCopy() {
        val message = profileLoadFailureMessage()

        assertEquals("Impossible de charger le profil. Réessayez.", message)
        assertDoesNotExposeSensitiveDetails(message)
    }

    private fun assertDoesNotExposeSensitiveDetails(message: String) {
        listOf(
            "secret@example.com",
            "SECRET",
            "SQL constraint",
            "http://internal.local",
            "token="
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }

    private class FailingUserPointsRepository : UserPointsRepository {
        private fun failure(): Nothing {
            throw IllegalStateException(
                "SQL constraint failed for secret@example.com token=SECRET http://internal.local/profile"
            )
        }

        override suspend fun getUserPoints(userId: String): UserPoints? = failure()
        override suspend fun getUserPointsOrDefault(userId: String): UserPoints = failure()
        override suspend fun createUserPoints(userId: String): UserPoints = failure()
        override suspend fun incrementEventCreationPoints(userId: String, points: Int): UserPoints = failure()
        override suspend fun incrementVotingPoints(userId: String, points: Int): UserPoints = failure()
        override suspend fun incrementCommentPoints(userId: String, points: Int): UserPoints = failure()
        override suspend fun incrementParticipationPoints(userId: String, points: Int): UserPoints = failure()
        override suspend fun applyPointsDecay(userId: String): UserPoints? = failure()
        override suspend fun getTopPointEarners(limit: Int): List<UserPoints> = failure()
        override suspend fun getPointsStatistics(): Map<String, Long> = failure()
        override suspend fun userHasMinimumPoints(userId: String, minimumPoints: Int): Boolean = failure()
        override suspend fun getTotalPoints(userId: String): Int = failure()
        override suspend fun deleteUserPoints(userId: String) = failure()
    }
}
