package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.gamification.Badge
import com.guyghost.wakeve.gamification.BadgeCategory
import com.guyghost.wakeve.gamification.BadgeRarity
import com.guyghost.wakeve.gamification.LeaderboardEntry
import com.guyghost.wakeve.gamification.LeaderboardType
import com.guyghost.wakeve.gamification.UserLevel
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
        val userLevel = UserLevel.fromPoints(mockPoints.totalPoints)
        _uiState.value = _uiState.value.copy(
            userPoints = mockPoints,
            userLevel = userLevel
        )
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
        // All possible badges (including locked ones without unlockedAt)
        val allPossibleBadges = listOf(
            Badge(id = "badge-first-event", name = "Premier Evenement", description = "A cree son premier evenement", icon = "\uD83C\uDF89", requirement = 1, pointsReward = 50, category = BadgeCategory.CREATION, rarity = BadgeRarity.COMMON),
            Badge(id = "badge-dedicated", name = "Organisateur Devoue", description = "A cree 5 evenements", icon = "\uD83D\uDCAA", requirement = 5, pointsReward = 75, category = BadgeCategory.CREATION, rarity = BadgeRarity.RARE),
            Badge(id = "badge-super-organizer", name = "Super Organisateur", description = "A cree 10 evenements", icon = "\uD83C\uDFC6", requirement = 10, pointsReward = 100, category = BadgeCategory.CREATION, rarity = BadgeRarity.EPIC),
            Badge(id = "badge-event-master", name = "Event Master", description = "A organise 5 evenements ce mois", icon = "\uD83D\uDC51", requirement = 5, pointsReward = 250, category = BadgeCategory.CREATION, rarity = BadgeRarity.LEGENDARY),
            Badge(id = "badge-first-vote", name = "Premier Vote", description = "A participe a son premier vote", icon = "\uD83D\uDC4D", requirement = 1, pointsReward = 25, category = BadgeCategory.VOTING, rarity = BadgeRarity.COMMON),
            Badge(id = "badge-early-bird", name = "Early Bird", description = "A vote dans les 24h", icon = "\uD83D\uDC26", requirement = 1, pointsReward = 25, category = BadgeCategory.VOTING, rarity = BadgeRarity.RARE),
            Badge(id = "badge-voting-master", name = "Maitre du Vote", description = "A vote 50 fois", icon = "\uD83D\uDDF3\uFE0F", requirement = 50, pointsReward = 75, category = BadgeCategory.VOTING, rarity = BadgeRarity.EPIC),
            Badge(id = "badge-first-steps", name = "Premiers Pas", description = "A participe a son premier evenement", icon = "\uD83D\uDC63", requirement = 1, pointsReward = 50, category = BadgeCategory.PARTICIPATION, rarity = BadgeRarity.COMMON),
            Badge(id = "badge-active-participant", name = "Participant Actif", description = "A participe a 5 evenements", icon = "\uD83D\uDE4B", requirement = 5, pointsReward = 50, category = BadgeCategory.PARTICIPATION, rarity = BadgeRarity.RARE),
            Badge(id = "badge-social-butterfly", name = "Papillon Social", description = "A participe a 10 evenements", icon = "\uD83E\uDD8B", requirement = 10, pointsReward = 100, category = BadgeCategory.PARTICIPATION, rarity = BadgeRarity.EPIC),
            Badge(id = "badge-chatty", name = "Bavard", description = "A commente 10 fois", icon = "\uD83D\uDCAC", requirement = 10, pointsReward = 50, category = BadgeCategory.ENGAGEMENT, rarity = BadgeRarity.RARE),
            Badge(id = "badge-scenario-creator", name = "Createur de Scenarios", description = "A cree 5 scenarios", icon = "\uD83D\uDCDD", requirement = 5, pointsReward = 75, category = BadgeCategory.ENGAGEMENT, rarity = BadgeRarity.RARE),
            Badge(id = "badge-century-club", name = "Club des Cent", description = "A atteint 100 points totaux", icon = "\uD83D\uDCAF", requirement = 100, pointsReward = 50, category = BadgeCategory.SPECIAL, rarity = BadgeRarity.COMMON),
            Badge(id = "badge-millenium-club", name = "Club des Mille", description = "A atteint 1000 points totaux", icon = "\uD83D\uDC8E", requirement = 1000, pointsReward = 150, category = BadgeCategory.SPECIAL, rarity = BadgeRarity.EPIC)
        )

        val earnedIds = mockBadges.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(
            userBadges = mockBadges,
            allBadges = allPossibleBadges.map { badge ->
                if (badge.id in earnedIds) {
                    mockBadges.first { it.id == badge.id }
                } else {
                    badge // locked: unlockedAt is null
                }
            }
        )
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
