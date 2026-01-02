package com.guyghost.wakeve.repository

import com.guyghost.wakeve.UserRepository
import com.guyghost.wakeve.gamification.LeaderboardEntry
import com.guyghost.wakeve.gamification.LeaderboardType
import com.guyghost.wakeve.gamification.UserPoints
import com.guyghost.wakeve.gamification.UserBadges
import com.guyghost.wakeve.gamification.BadgeRarity
import com.guyghost.wakeve.gamification.BadgeCategory
import com.guyghost.wakeve.gamification.repository.UserPointsRepository
import com.guyghost.wakeve.gamification.repository.UserBadgesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Repository interface for managing leaderboard data.
 * Provides methods to retrieve leaderboard entries, user rankings, and manage caching.
 *
 * The leaderboard supports multiple filter types:
 * - ALL_TIME: All-time rankings
 * - THIS_MONTH: Rankings for the current month
 * - THIS_WEEK: Rankings for the current week
 * - FRIENDS: Rankings restricted to the user's friends
 */
interface LeaderboardRepository {
    /**
     * Gets the leaderboard for a specific type.
     *
     * @param type The type of leaderboard (ALL_TIME, THIS_MONTH, THIS_WEEK, FRIENDS)
     * @param limit Maximum number of entries to return (default 20)
     * @param excludeAnonymous Whether to exclude anonymous users (default true)
     * @param currentUserId Optional current user ID to mark their entry
     * @param friendIds List of friend user IDs (for FRIENDS filter)
     * @return List of leaderboard entries sorted by rank
     */
    suspend fun getLeaderboard(
        type: LeaderboardType,
        limit: Int = 20,
        excludeAnonymous: Boolean = true,
        currentUserId: String? = null,
        friendIds: List<String> = emptyList()
    ): List<LeaderboardEntry>

    /**
     * Gets a user's rank on a specific leaderboard.
     *
     * @param userId The user to get rank for
     * @param type The type of leaderboard
     * @param friendIds List of friend user IDs (for FRIENDS filter)
     * @return The user's rank (1-based) or null if not ranked
     */
    suspend fun getUserRank(
        userId: String,
        type: LeaderboardType,
        friendIds: List<String> = emptyList()
    ): Int?

    /**
     * Gets a user's rank along with their leaderboard entry.
     *
     * @param userId The user to get rank for
     * @param type The type of leaderboard
     * @param friendIds List of friend user IDs (for FRIENDS filter)
     * @return Pair of (rank, entry) or null if user not ranked
     */
    suspend fun getUserRankWithEntry(
        userId: String,
        type: LeaderboardType,
        friendIds: List<String> = emptyList()
    ): Pair<Int, LeaderboardEntry>?

    /**
     * Refreshes the leaderboard cache for all types.
     * Useful after significant data changes.
     */
    suspend fun refreshLeaderboardCache()

    /**
     * Checks if a user is in anonymous mode.
     *
     * @param userId The user to check
     * @return true if the user is anonymous, false otherwise
     */
    suspend fun isAnonymous(userId: String): Boolean
}

/**
 * Implementation of LeaderboardRepository.
 * Provides cached access to leaderboard data with 5-minute cache duration.
 *
 * Features:
 * - 5-minute cache with automatic invalidation
 * - Support for all leaderboard filter types
 * - Anonymous user filtering
 * - Friend-based filtering
 */
class LeaderboardRepositoryImpl(
    private val userPointsRepository: UserPointsRepository,
    private val userBadgesRepository: UserBadgesRepository,
    private val userRepository: UserRepository
) : LeaderboardRepository {

    companion object {
        /** Cache duration in milliseconds (5 minutes) */
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L

        /** Default leaderboard limit */
        private const val DEFAULT_LIMIT = 20

        /** Maximum entries to fetch for processing */
        private const val MAX_FETCH_LIMIT = 100

        /** Gets current time in milliseconds */
        private fun currentTimeMs(): Long {
            return Clock.System.now().toEpochMilliseconds()
        }
    }

    // Cache storage: LeaderboardType -> List of entries
    private val leaderboardCache = mutableMapOf<LeaderboardType, List<LeaderboardEntry>>()

    // Cache timestamp: LeaderboardType -> Timestamp
    private val lastCacheTime = mutableMapOf<LeaderboardType, Long>()

    // Cache for user badges count: userId -> badge count
    private val badgeCountCache = mutableMapOf<String, Int>()

    // Cache for legendary/epic counts: userId -> Pair(legendary, epic)
    private val badgeRarityCache = mutableMapOf<String, Pair<Int, Int>>()

    // Cache for anonymous users
    private val anonymousUsers = mutableSetOf<String>()

    override suspend fun getLeaderboard(
        type: LeaderboardType,
        limit: Int,
        excludeAnonymous: Boolean,
        currentUserId: String?,
        friendIds: List<String>
    ): List<LeaderboardEntry> = withContext(Dispatchers.Default) {
        // Check cache validity
        if (isCacheValid(type)) {
            val cached = leaderboardCache[type] ?: emptyList()
            return@withContext filterAndLimitLeaderboard(
                cached, limit, excludeAnonymous, currentUserId, friendIds
            )
        }

        // Fetch fresh data from repositories
        val allUsers = fetchAllUserPoints()

        // Filter by type
        val filteredUsers = filterByLeaderboardType(allUsers, type, friendIds)

        // Filter anonymous users if needed
        val filteredForAnonymous = if (excludeAnonymous) {
            filteredUsers.filter { !anonymousUsers.contains(it.userId) }
        } else {
            filteredUsers
        }

        // Pre-fetch badge counts for all users
        val rarityCounts = mutableMapOf<String, Pair<Int, Int>>()
        for (user in filteredForAnonymous) {
            rarityCounts[user.userId] = getBadgeRarityCount(user.userId)
        }

        // Sort by total points (descending), then by legendary count, then by epic count
        val sorted = filteredForAnonymous
            .sortedWith { a, b ->
                val pointsCompare = b.totalPoints.compareTo(a.totalPoints)
                if (pointsCompare != 0) return@sortedWith pointsCompare
                val legendaryA = rarityCounts[a.userId]?.first ?: 0
                val legendaryB = rarityCounts[b.userId]?.first ?: 0
                val legendaryCompare = legendaryB.compareTo(legendaryA)
                if (legendaryCompare != 0) return@sortedWith legendaryCompare
                val epicA = rarityCounts[a.userId]?.second ?: 0
                val epicB = rarityCounts[b.userId]?.second ?: 0
                return@sortedWith epicB.compareTo(epicA)
            }
            .take(MAX_FETCH_LIMIT)

        // Create leaderboard entries
        val entries = sorted.mapIndexed { index, points ->
            createLeaderboardEntry(points, index + 1, currentUserId, friendIds, rarityCounts)
        }

        // Update cache
        leaderboardCache[type] = entries
        lastCacheTime[type] = currentTimeMs()

        // Return filtered and limited result
        filterAndLimitLeaderboard(entries, limit, excludeAnonymous, currentUserId, friendIds)
    }

    override suspend fun getUserRank(
        userId: String,
        type: LeaderboardType,
        friendIds: List<String>
    ): Int? {
        val leaderboard = getLeaderboard(type, MAX_FETCH_LIMIT, true, userId, friendIds)
        val entry = leaderboard.find { it.userId == userId }
        return entry?.rank
    }

    override suspend fun getUserRankWithEntry(
        userId: String,
        type: LeaderboardType,
        friendIds: List<String>
    ): Pair<Int, LeaderboardEntry>? {
        val leaderboard = getLeaderboard(type, MAX_FETCH_LIMIT, true, userId, friendIds)
        val entry = leaderboard.find { it.userId == userId }
        return entry?.let { it.rank to it }
    }

    override suspend fun refreshLeaderboardCache() = withContext(Dispatchers.Default) {
        leaderboardCache.clear()
        lastCacheTime.clear()
        badgeCountCache.clear()
        badgeRarityCache.clear()
    }

    override suspend fun isAnonymous(userId: String): Boolean = withContext(Dispatchers.Default) {
        anonymousUsers.contains(userId)
    }

    // ================ Private Helper Methods ================

    /**
     * Fetches all user points from the repository.
     */
    private suspend fun fetchAllUserPoints(): List<UserPoints> {
        // For now, use getTopPointEarners with a large limit
        return userPointsRepository.getTopPointEarners(MAX_FETCH_LIMIT)
    }

    /**
     * Filters users based on leaderboard type.
     */
    private fun filterByLeaderboardType(
        users: List<UserPoints>,
        type: LeaderboardType,
        friendIds: List<String>
    ): List<UserPoints> {
        return when (type) {
            LeaderboardType.ALL_TIME -> users
            LeaderboardType.THIS_MONTH -> filterByMonth(users, 0)
            LeaderboardType.THIS_WEEK -> filterByWeek(users, 0)
            LeaderboardType.FRIENDS -> {
                if (friendIds.isEmpty()) {
                    emptyList()
                } else {
                    users.filter { friendIds.contains(it.userId) }
                }
            }
        }
    }

    /**
     * Filters users who earned points this month.
     * For now, uses a simplified implementation.
     */
    private fun filterByMonth(users: List<UserPoints>, monthsAgo: Int): List<UserPoints> {
        // In a full implementation, this would filter by lastUpdated timestamp
        // For now, return all users (placeholder implementation)
        return users
    }

    /**
     * Filters users who earned points this week.
     * For now, uses a simplified implementation.
     */
    private fun filterByWeek(users: List<UserPoints>, weeksAgo: Int): List<UserPoints> {
        // In a full implementation, this would filter by lastUpdated timestamp
        // For now, return all users (placeholder implementation)
        return users
    }

    /**
     * Filters and limits the leaderboard entries.
     */
    private fun filterAndLimitLeaderboard(
        entries: List<LeaderboardEntry>,
        limit: Int,
        excludeAnonymous: Boolean,
        currentUserId: String?,
        friendIds: List<String>
    ): List<LeaderboardEntry> {
        var filtered = entries

        // Filter anonymous users if needed
        if (excludeAnonymous) {
            filtered = filtered.filter { it.userId != "anonymous" }
        }

        // Mark current user and friends
        if (currentUserId != null || friendIds.isNotEmpty()) {
            filtered = filtered.map { entry ->
                entry.copy(
                    isCurrentUser = entry.userId == currentUserId,
                    isFriend = friendIds.contains(entry.userId)
                )
            }
        }

        return filtered.take(limit)
    }

    /**
     * Creates a leaderboard entry from user points.
     */
    private suspend fun createLeaderboardEntry(
        points: UserPoints,
        rank: Int,
        currentUserId: String?,
        friendIds: List<String>,
        rarityCounts: Map<String, Pair<Int, Int>> = emptyMap()
    ): LeaderboardEntry {
        val username = getUsername(points.userId)
        val badgesCount = getBadgesCount(points.userId)
        val (legendaryCount, epicCount) = rarityCounts[points.userId] ?: (0 to 0)

        return LeaderboardEntry(
            userId = points.userId,
            username = username,
            totalPoints = points.totalPoints,
            badgesCount = badgesCount,
            rank = rank,
            isCurrentUser = points.userId == currentUserId,
            isFriend = friendIds.contains(points.userId),
            legendaryCount = legendaryCount,
            epicCount = epicCount
        )
    }

    /**
     * Gets the username for a user ID.
     */
    private suspend fun getUsername(userId: String): String {
        return userRepository.getUserById(userId)?.name ?: "Utilisateur"
    }

    /**
     * Gets the badges count for a user (cached).
     */
    private suspend fun getBadgesCount(userId: String): Int {
        return badgeCountCache.getOrPut(userId) {
            userBadgesRepository.getUserBadges(userId).badges.size
        }
    }

    /**
     * Gets the legendary badge count for a user (cached).
     */
    private suspend fun getLegendaryCount(userId: String): Int {
        return getBadgeRarityCount(userId).first
    }

    /**
     * Gets the epic badge count for a user (cached).
     */
    private suspend fun getEpicCount(userId: String): Int {
        return getBadgeRarityCount(userId).second
    }

    /**
     * Gets both legendary and epic badge counts (cached).
     */
    private suspend fun getBadgeRarityCount(userId: String): Pair<Int, Int> {
        return badgeRarityCache.getOrPut(userId) {
            val userBadges = userBadgesRepository.getUserBadges(userId)
            val legendary = userBadges.badges.count { it.rarity == BadgeRarity.LEGENDARY }
            val epic = userBadges.badges.count { it.rarity == BadgeRarity.EPIC }
            legendary to epic
        }
    }

    /**
     * Checks if the cache for a leaderboard type is still valid.
     */
    private fun isCacheValid(type: LeaderboardType): Boolean {
        val cacheTime = lastCacheTime[type] ?: return false
        return (currentTimeMs() - cacheTime) < CACHE_DURATION_MS
    }
}

/**
 * In-memory implementation of LeaderboardRepository for testing.
 * Uses simple collections without database dependencies.
 */
class InMemoryLeaderboardRepository(
    private val userPointsRepository: UserPointsRepository,
    private val userBadgesRepository: UserBadgesRepository,
    private val getUsername: suspend (String) -> String = { "User" }
) : LeaderboardRepository {

    companion object {
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L
        private const val MAX_FETCH_LIMIT = 100

        /** Gets current time in milliseconds */
        private fun currentTimeMs(): Long {
            return Clock.System.now().toEpochMilliseconds()
        }
    }

    private val leaderboardCache = mutableMapOf<LeaderboardType, List<LeaderboardEntry>>()
    private val lastCacheTime = mutableMapOf<LeaderboardType, Long>()
    private val anonymousUsers = mutableSetOf<String>()

    override suspend fun getLeaderboard(
        type: LeaderboardType,
        limit: Int,
        excludeAnonymous: Boolean,
        currentUserId: String?,
        friendIds: List<String>
    ): List<LeaderboardEntry> {
        // Check cache
        if (isCacheValid(type)) {
            val cached = leaderboardCache[type] ?: emptyList()
            return processLeaderboard(cached, limit, excludeAnonymous, currentUserId, friendIds)
        }

        // Fetch from repository
        val allUsers = userPointsRepository.getTopPointEarners(MAX_FETCH_LIMIT)
        val filteredUsers = filterByType(allUsers, type, friendIds)
        val sorted = filteredUsers.sortedByDescending { it.totalPoints }

        val entries = sorted.mapIndexed { index, points ->
            val badges = userBadgesRepository.getUserBadges(points.userId)
            LeaderboardEntry(
                userId = points.userId,
                username = getUsername(points.userId),
                totalPoints = points.totalPoints,
                badgesCount = badges.badges.size,
                rank = index + 1,
                isCurrentUser = points.userId == currentUserId,
                isFriend = friendIds.contains(points.userId)
            )
        }

        leaderboardCache[type] = entries
        lastCacheTime[type] = currentTimeMs()

        return processLeaderboard(entries, limit, excludeAnonymous, currentUserId, friendIds)
    }

    override suspend fun getUserRank(
        userId: String,
        type: LeaderboardType,
        friendIds: List<String>
    ): Int? {
        val leaderboard = getLeaderboard(type, MAX_FETCH_LIMIT, true, userId, friendIds)
        return leaderboard.find { it.userId == userId }?.rank
    }

    override suspend fun getUserRankWithEntry(
        userId: String,
        type: LeaderboardType,
        friendIds: List<String>
    ): Pair<Int, LeaderboardEntry>? {
        val leaderboard = getLeaderboard(type, MAX_FETCH_LIMIT, true, userId, friendIds)
        val entry = leaderboard.find { it.userId == userId }
        return entry?.let { it.rank to it }
    }

    override suspend fun refreshLeaderboardCache() {
        leaderboardCache.clear()
        lastCacheTime.clear()
    }

    override suspend fun isAnonymous(userId: String): Boolean {
        return anonymousUsers.contains(userId)
    }

    private fun filterByType(
        users: List<UserPoints>,
        type: LeaderboardType,
        friendIds: List<String>
    ): List<UserPoints> {
        return when (type) {
            LeaderboardType.ALL_TIME -> users
            LeaderboardType.THIS_MONTH, LeaderboardType.THIS_WEEK -> users
            LeaderboardType.FRIENDS -> {
                if (friendIds.isEmpty()) emptyList()
                else users.filter { friendIds.contains(it.userId) }
            }
        }
    }

    private fun processLeaderboard(
        entries: List<LeaderboardEntry>,
        limit: Int,
        excludeAnonymous: Boolean,
        currentUserId: String?,
        friendIds: List<String>
    ): List<LeaderboardEntry> {
        var result = if (excludeAnonymous) {
            entries.filter { it.userId != "anonymous" }
        } else {
            entries
        }

        if (currentUserId != null || friendIds.isNotEmpty()) {
            result = result.map { entry ->
                entry.copy(
                    isCurrentUser = entry.userId == currentUserId,
                    isFriend = friendIds.contains(entry.userId)
                )
            }
        }

        return result.take(limit)
    }

    private fun isCacheValid(type: LeaderboardType): Boolean {
        val time = lastCacheTime[type] ?: return false
        return (currentTimeMs() - time) < CACHE_DURATION_MS
    }
}
