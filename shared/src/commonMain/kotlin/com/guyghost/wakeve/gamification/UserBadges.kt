package com.guyghost.wakeve.gamification

import kotlinx.serialization.Serializable

/**
 * Represents a user's unlocked badges with metadata.
 *
 * @property userId The unique identifier of the user
 * @property badges List of badges the user has earned
 * @property unlockedAt Map of badge IDs to their unlock timestamps
 */
@Serializable
data class UserBadges(
    val userId: String,
    val badges: List<Badge> = emptyList(),
    val unlockedAt: Map<String, String> = emptyMap()
) {
    /**
     * Gets the count of badges by rarity.
     */
    fun countByRarity(rarity: BadgeRarity): Int {
        return badges.count { it.rarity == rarity }
    }

    /**
     * Gets the total points earned from badges.
     */
    fun totalBadgePoints(): Int {
        return badges.sumOf { it.pointsReward }
    }

    /**
     * Checks if user has a specific badge.
     */
    fun hasBadge(badgeId: String): Boolean {
        return badges.any { it.id == badgeId }
    }

    /**
     * Gets badges by category.
     */
    fun getBadgesByCategory(category: BadgeCategory): List<Badge> {
        return badges.filter { it.category == category }
    }
}
