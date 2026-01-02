package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.gamification.Badge
import com.guyghost.wakeve.gamification.BadgeCategory
import com.guyghost.wakeve.gamification.BadgeRarity
import com.guyghost.wakeve.gamification.LeaderboardEntry
import com.guyghost.wakeve.gamification.LeaderboardType
import com.guyghost.wakeve.gamification.UserBadges
import com.guyghost.wakeve.gamification.UserPoints
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
    val userBadges: List<Badge> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val selectedLeaderboardTab: LeaderboardType = LeaderboardType.ALL_TIME,
    val currentUserId: String = "",
    val error: String? = null
)

/**
 * ViewModel for the Profile & Achievements screen.
 * Manages gamification data including points, badges, and leaderboard.
 */
class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Mock user ID for demo purposes
    val currentUserId: String = "user-current"

    init {
        _uiState.value = _uiState.value.copy(currentUserId = currentUserId)
        loadProfileData()
    }

    /**
     * Loads all profile data (points, badges, leaderboard).
     */
    fun loadProfileData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Load user points
                loadUserPoints()

                // Load user badges
                loadUserBadges()

                // Load leaderboard
                loadLeaderboard()

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Loads user points data.
     */
    private suspend fun loadUserPoints() {
        // Mock data for demonstration
        // In production, this would call the GamificationService
        val mockPoints = UserPoints(
            userId = currentUserId,
            totalPoints = 1250,
            eventCreationPoints = 500,
            votingPoints = 300,
            commentPoints = 250,
            participationPoints = 200,
            decayPoints = 0,
            lastUpdated = "2026-01-02T10:30:00Z"
        )
        _uiState.value = _uiState.value.copy(userPoints = mockPoints)
    }

    /**
     * Loads user badges data.
     */
    private suspend fun loadUserBadges() {
        // Mock data for demonstration
        // In production, this would call the GamificationService
        val mockBadges = listOf(
            Badge(
                id = "badge-first-event",
                name = "Premier Événement",
                description = "A créé son premier événement",
                icon = "🎉",
                requirement = 1,
                pointsReward = 50,
                category = BadgeCategory.CREATION,
                rarity = BadgeRarity.COMMON,
                unlockedAt = "2025-12-01T10:00:00Z"
            ),
            Badge(
                id = "badge-super-organizer",
                name = "Super Organisateur",
                description = "A créé 10 événements",
                icon = "🏆",
                requirement = 10,
                pointsReward = 100,
                category = BadgeCategory.CREATION,
                rarity = BadgeRarity.EPIC,
                unlockedAt = "2025-12-15T14:30:00Z"
            ),
            Badge(
                id = "badge-early-bird",
                name = "早起鸟 (Early Bird)",
                description = "A voté dans les 24h",
                icon = "🐦",
                requirement = 1,
                pointsReward = 25,
                category = BadgeCategory.VOTING,
                rarity = BadgeRarity.COMMON,
                unlockedAt = "2025-12-20T08:00:00Z"
            ),
            Badge(
                id = "badge-voting-master",
                name = "Maître du Vote",
                description = "A voté 50 fois",
                icon = "🗳️",
                requirement = 50,
                pointsReward = 75,
                category = BadgeCategory.VOTING,
                rarity = BadgeRarity.RARE,
                unlockedAt = "2025-12-28T16:00:00Z"
            ),
            Badge(
                id = "badge-active-participant",
                name = "Participant Actif",
                description = "A participé à 5 événements",
                icon = "🙋",
                requirement = 5,
                pointsReward = 50,
                category = BadgeCategory.PARTICIPATION,
                rarity = BadgeRarity.COMMON,
                unlockedAt = "2025-12-10T12:00:00Z"
            ),
            Badge(
                id = "badge-event-master",
                name = "Maître des Événements",
                description = "A organisé 5 événements ce mois",
                icon = "🎭",
                requirement = 5,
                pointsReward = 150,
                category = BadgeCategory.ENGAGEMENT,
                rarity = BadgeRarity.LEGENDARY,
                unlockedAt = "2025-12-25T20:00:00Z"
            ),
            Badge(
                id = "badge-commentator",
                name = "Commentateur",
                description = "A commenté 10 scénarios",
                icon = "💬",
                requirement = 10,
                pointsReward = 30,
                category = BadgeCategory.PARTICIPATION,
                rarity = BadgeRarity.COMMON,
                unlockedAt = "2025-12-18T11:00:00Z"
            ),
            Badge(
                id = "badge-dedicated",
                name = "Dévoué",
                description = "7 jours consécutifs de participation",
                icon = "⭐",
                requirement = 7,
                pointsReward = 100,
                category = BadgeCategory.ENGAGEMENT,
                rarity = BadgeRarity.RARE,
                unlockedAt = "2025-12-30T09:00:00Z"
            )
        )
        _uiState.value = _uiState.value.copy(userBadges = mockBadges)
    }

    /**
     * Loads leaderboard data for the selected tab.
     */
    private suspend fun loadLeaderboard() {
        // Mock data for demonstration
        // In production, this would call the GamificationService
        val mockLeaderboard = listOf(
            LeaderboardEntry(
                userId = "user-1",
                username = "Alice Martin",
                totalPoints = 2450,
                badgesCount = 12,
                rank = 1,
                isFriend = false,
                legendaryCount = 2,
                epicCount = 4
            ),
            LeaderboardEntry(
                userId = "user-2",
                username = "Bob Dupont",
                totalPoints = 2100,
                badgesCount = 10,
                rank = 2,
                isFriend = true,
                legendaryCount = 1,
                epicCount = 3
            ),
            LeaderboardEntry(
                userId = "user-3",
                username = "Claire Bernard",
                totalPoints = 1950,
                badgesCount = 9,
                rank = 3,
                isFriend = false,
                legendaryCount = 1,
                epicCount = 2
            ),
            LeaderboardEntry(
                userId = currentUserId,
                username = "Vous",
                totalPoints = 1250,
                badgesCount = 8,
                rank = 4,
                isCurrentUser = true,
                legendaryCount = 1,
                epicCount = 1
            ),
            LeaderboardEntry(
                userId = "user-5",
                username = "David Leroy",
                totalPoints = 1100,
                badgesCount = 7,
                rank = 5,
                isFriend = true,
                legendaryCount = 0,
                epicCount = 2
            ),
            LeaderboardEntry(
                userId = "user-6",
                username = "Emma Moreau",
                totalPoints = 980,
                badgesCount = 6,
                rank = 6,
                isFriend = false,
                legendaryCount = 0,
                epicCount = 1
            ),
            LeaderboardEntry(
                userId = "user-7",
                username = "Frank Rousseau",
                totalPoints = 850,
                badgesCount = 5,
                rank = 7,
                isFriend = false,
                legendaryCount = 0,
                epicCount = 1
            ),
            LeaderboardEntry(
                userId = "user-8",
                username = "Grace Wang",
                totalPoints = 720,
                badgesCount = 4,
                rank = 8,
                isFriend = true,
                legendaryCount = 0,
                epicCount = 0
            ),
            LeaderboardEntry(
                userId = "user-9",
                username = "Henry Chen",
                totalPoints = 650,
                badgesCount = 4,
                rank = 9,
                isFriend = false,
                legendaryCount = 0,
                epicCount = 0
            ),
            LeaderboardEntry(
                userId = "user-10",
                username = "Isabelle Dubois",
                totalPoints = 580,
                badgesCount = 3,
                rank = 10,
                isFriend = false,
                legendaryCount = 0,
                epicCount = 0
            )
        )
        _uiState.value = _uiState.value.copy(leaderboard = mockLeaderboard)
    }

    /**
     * Changes the selected leaderboard tab.
     */
    fun selectLeaderboardTab(tab: LeaderboardType) {
        _uiState.value = _uiState.value.copy(selectedLeaderboardTab = tab)
        viewModelScope.launch {
            loadLeaderboard()
        }
    }

    /**
     * Clears the error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
