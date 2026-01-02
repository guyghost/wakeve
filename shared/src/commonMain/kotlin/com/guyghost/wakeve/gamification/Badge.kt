package com.guyghost.wakeve.gamification

import kotlinx.serialization.Serializable

/**
 * Represents a badge that can be earned by users for specific achievements.
 *
 * @property id Unique identifier for the badge (e.g., "badge-super-organizer")
 * @property name Display name of the badge (e.g., "Super Organisateur")
 * @property description Description of how to earn the badge
 * @property icon Emoji or icon identifier for display
 * @property requirement Threshold needed to unlock (events count, points, etc.)
 * @property pointsReward Points awarded when badge is unlocked
 * @property category Category of the badge for organization
 * @property rarity Rarity level affecting display and prestige
 */
@Serializable
data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val requirement: Int,
    val pointsReward: Int,
    val category: BadgeCategory,
    val rarity: BadgeRarity,
    val unlockedAt: String? = null
)

@Serializable
enum class BadgeCategory {
    CREATION,
    VOTING,
    PARTICIPATION,
    ENGAGEMENT,
    SPECIAL
}

@Serializable
enum class BadgeRarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}
