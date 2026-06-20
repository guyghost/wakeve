package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.gamification.Badge
import com.guyghost.wakeve.gamification.BadgeEligibilityChecker
import com.guyghost.wakeve.gamification.GamificationService
import com.guyghost.wakeve.gamification.LeaderboardEntry
import com.guyghost.wakeve.gamification.LeaderboardType
import com.guyghost.wakeve.gamification.UserLevel
import com.guyghost.wakeve.gamification.UserPoints
import com.guyghost.wakeve.gamification.repository.InMemoryUserBadgesRepository
import com.guyghost.wakeve.gamification.repository.InMemoryUserPointsRepository
import com.guyghost.wakeve.gamification.repository.UserBadgesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State for the Profile Screen.
 */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val userPoints: UserPoints? = null,
    val userLevel: UserLevel? = null,
    val userBadges: List<Badge> = emptyList(),
    val allBadges: List<Badge> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val selectedLeaderboardTab: LeaderboardType = LeaderboardType.ALL_TIME,
    val currentUserId: String = "",
    val error: String? = null
)

/**
 * ViewModel for the Profile & Achievements screen.
 *
 * It reads gamification state from the app repositories. When no repository data
 * exists yet, the screen shows zero points and locked badge definitions instead
 * of invented achievements.
 */
class ProfileViewModel(
    val currentUserId: String = "currentUser",
    gamificationService: GamificationService? = null,
    userBadgesRepository: UserBadgesRepository? = null
) : ViewModel() {

    private val fallbackPointsRepository = InMemoryUserPointsRepository()
    private val fallbackBadgesRepository = InMemoryUserBadgesRepository()
    private val resolvedBadgesRepository = userBadgesRepository ?: fallbackBadgesRepository
    private val resolvedGamificationService = gamificationService ?: GamificationService(
        userPointsRepository = fallbackPointsRepository,
        userBadgesRepository = resolvedBadgesRepository,
        badgeEligibilityChecker = BadgeEligibilityChecker(
            userPointsRepository = fallbackPointsRepository,
            userBadgesRepository = resolvedBadgesRepository
        )
    )

    private val _uiState = MutableStateFlow(ProfileUiState(currentUserId = currentUserId))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    /**
     * Loads all profile data (points, badges, leaderboard).
     */
    fun loadProfileData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            runCatching {
                val points = resolvedGamificationService.getUserPoints(currentUserId)
                    ?: UserPoints(userId = currentUserId)
                val userBadges = resolvedGamificationService.getUserBadges(currentUserId)
                val allBadges = resolvedBadgesRepository.getAllBadgeDefinitions()
                    .map { definition ->
                        userBadges.firstOrNull { it.id == definition.id }
                            ?: definition.copy(unlockedAt = null)
                    }
                val leaderboard = resolvedGamificationService.getLeaderboard(
                    type = _uiState.value.selectedLeaderboardTab,
                    limit = 20,
                    currentUserId = currentUserId
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userPoints = points,
                    userLevel = UserLevel.fromPoints(points.totalPoints),
                    userBadges = userBadges,
                    allBadges = allBadges,
                    leaderboard = leaderboard,
                    error = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userPoints = UserPoints(userId = currentUserId),
                    userLevel = UserLevel.fromPoints(0),
                    userBadges = emptyList(),
                    allBadges = emptyList(),
                    leaderboard = emptyList(),
                    error = error.message ?: "Impossible de charger le profil"
                )
            }
        }
    }

    /**
     * Changes the selected leaderboard tab.
     */
    fun selectLeaderboardTab(tab: LeaderboardType) {
        _uiState.value = _uiState.value.copy(selectedLeaderboardTab = tab)
        loadProfileData()
    }

    /**
     * Clears the error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
