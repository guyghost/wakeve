package com.guyghost.wakeve.gamification

import kotlinx.serialization.Serializable

/**
 * Badge Assets definition for the 15 core badges.
 * Contains visual assets (emoji/icon, color) for each badge.
 *
 * Each badge has:
 * - An emoji icon for display
 * - A Material Design 3 color (hex format)
 * - Rarity tier (COMMON, RARE, EPIC, LEGENDARY)
 * - Category (CREATION, VOTING, PARTICIPATION, ENGAGEMENT)
 * - Points reward and unlock requirement
 */
object BadgeAssets {

    /**
     * Complete map of all badge assets keyed by badge ID.
     */
    val BADGES: Map<String, BadgeAsset> = mapOf(
        // ========== CREATION Category ==========

        "badge-first-event" to BadgeAsset(
            id = "badge-first-event",
            name = "Premier événement",
            description = "A créé votre premier événement",
            icon = "🎉",
            color = "#6200EE",  // Material Purple 500
            rarity = BadgeRarity.COMMON,
            category = BadgeCategory.CREATION,
            pointsReward = 10,
            requirement = 1
        ),

        "badge-dedicated" to BadgeAsset(
            id = "badge-dedicated",
            name = "Organisateur Dévoué",
            description = "A créé 5 événements",
            icon = "💪",
            color = "#7C4DFF",  // Material Purple A200
            rarity = BadgeRarity.RARE,
            category = BadgeCategory.CREATION,
            pointsReward = 75,
            requirement = 5
        ),

        "badge-super-organizer" to BadgeAsset(
            id = "badge-super-organizer",
            name = "Super Organisateur",
            description = "A créé 10 événements",
            icon = "🏆",
            color = "#FF9800",  // Material Orange 500
            rarity = BadgeRarity.EPIC,
            category = BadgeCategory.CREATION,
            pointsReward = 100,
            requirement = 10
        ),

        "badge-event-master" to BadgeAsset(
            id = "badge-event-master",
            name = "Event Master",
            description = "A créé 5 événements en un mois",
            icon = "👑",
            color = "#FFD700",  // Gold
            rarity = BadgeRarity.LEGENDARY,
            category = BadgeCategory.CREATION,
            pointsReward = 250,
            requirement = 5
        ),

        "badge-industry" to BadgeAsset(
            id = "badge-industry",
            name = "Producteur d'Événements",
            description = "A créé 25 événements",
            icon = "🏭",
            color = "#FF5722",  // Material Deep Orange 500
            rarity = BadgeRarity.LEGENDARY,
            category = BadgeCategory.CREATION,
            pointsReward = 500,
            requirement = 25
        ),

        // ========== VOTING Category ==========

        "badge-first-vote" to BadgeAsset(
            id = "badge-first-vote",
            name = "Premier Vote",
            description = "A participé à son premier vote",
            icon = "👍",
            color = "#4CAF50",  // Material Green 500
            rarity = BadgeRarity.COMMON,
            category = BadgeCategory.VOTING,
            pointsReward = 5,
            requirement = 1
        ),

        "badge-early-bird" to BadgeAsset(
            id = "badge-early-bird",
            name = "Early Bird",
            description = "A voté dans les 24h sur 10 sondages",
            icon = "⏰",
            color = "#00BCD4",  // Material Cyan 500
            rarity = BadgeRarity.RARE,
            category = BadgeCategory.VOTING,
            pointsReward = 50,
            requirement = 10
        ),

        "badge-decision-maker" to BadgeAsset(
            id = "badge-decision-maker",
            name = "Decision Maker",
            description = "A voté sur 50 sondages",
            icon = "🗳️",
            color = "#2196F3",  // Material Blue 500
            rarity = BadgeRarity.EPIC,
            category = BadgeCategory.VOTING,
            pointsReward = 100,
            requirement = 50
        ),

        "badge-quick-responder" to BadgeAsset(
            id = "badge-quick-responder",
            name = "Réponse Rapide",
            description = "A répondu à 5 votes en moins de 2 heures",
            icon = "⚡",
            color = "#FFEB3B",  // Material Yellow 500
            rarity = BadgeRarity.EPIC,
            category = BadgeCategory.VOTING,
            pointsReward = 75,
            requirement = 5
        ),

        "badge-voting-champion" to BadgeAsset(
            id = "badge-voting-champion",
            name = "Champion du Vote",
            description = "A participé à 100 votes",
            icon = "🛡️",
            color = "#3F51B5",  // Material Indigo 500
            rarity = BadgeRarity.LEGENDARY,
            category = BadgeCategory.VOTING,
            pointsReward = 250,
            requirement = 100
        ),

        // ========== PARTICIPATION Category ==========

        "badge-first-steps" to BadgeAsset(
            id = "badge-first-steps",
            name = "Premiers Pas",
            description = "A participé à son premier événement",
            icon = "👣",
            color = "#8BC34A",  // Material Light Green 500
            rarity = BadgeRarity.COMMON,
            category = BadgeCategory.PARTICIPATION,
            pointsReward = 10,
            requirement = 1
        ),

        "badge-team-player" to BadgeAsset(
            id = "badge-team-player",
            name = "Team Player",
            description = "A participé à 20 événements",
            icon = "🤝",
            color = "#009688",  // Material Teal 500
            rarity = BadgeRarity.RARE,
            category = BadgeCategory.PARTICIPATION,
            pointsReward = 100,
            requirement = 20
        ),

        "badge-social-butterfly" to BadgeAsset(
            id = "badge-social-butterfly",
            name = "Social Butterfly",
            description = "A participé à 10 événements",
            icon = "🦋",
            color = "#E91E63",  // Material Pink 500
            rarity = BadgeRarity.EPIC,
            category = BadgeCategory.PARTICIPATION,
            pointsReward = 150,
            requirement = 10
        ),

        "badge-party-animal" to BadgeAsset(
            id = "badge-party-animal",
            name = "Fêteur",
            description = "A participé à 25 événements",
            icon = "🎊",
            color = "#F44336",  // Material Red 500
            rarity = BadgeRarity.LEGENDARY,
            category = BadgeCategory.PARTICIPATION,
            pointsReward = 300,
            requirement = 25
        ),

        "badge-festival-goer" to BadgeAsset(
            id = "badge-festival-goer",
            name = "Festival Goer",
            description = "A assisté à 5 événements sur une même journée",
            icon = "🎪",
            color = "#9C27B0",  // Material Purple 700
            rarity = BadgeRarity.EPIC,
            category = BadgeCategory.PARTICIPATION,
            pointsReward = 200,
            requirement = 5
        ),

        // ========== ENGAGEMENT Category ==========

        "badge-chatty" to BadgeAsset(
            id = "badge-chatty",
            name = "Bavard",
            description = "A commenté 10 fois",
            icon = "💬",
            color = "#673AB7",  // Material Deep Purple 500
            rarity = BadgeRarity.RARE,
            category = BadgeCategory.ENGAGEMENT,
            pointsReward = 25,
            requirement = 10
        ),

        "badge-conversationalist" to BadgeAsset(
            id = "badge-conversationalist",
            name = "Conversationalist",
            description = "A commenté sur 30 événements",
            icon = "🗨️",
            color = "#3F51B5",  // Material Indigo 500
            rarity = BadgeRarity.EPIC,
            category = BadgeCategory.ENGAGEMENT,
            pointsReward = 75,
            requirement = 30
        ),

        "badge-opinionated" to BadgeAsset(
            id = "badge-opinionated",
            name = "Avisé",
            description = "A voté sur 20 scénarios",
            icon = "🤝",
            color = "#607D8B",  // Material Blue Grey 500
            rarity = BadgeRarity.COMMON,
            category = BadgeCategory.ENGAGEMENT,
            pointsReward = 30,
            requirement = 20
        ),

        "badge-scenario-creator" to BadgeAsset(
            id = "badge-scenario-creator",
            name = "Créateur de Scénarios",
            description = "A créé 5 scénarios",
            icon = "📝",
            color = "#795548",  // Material Brown 500
            rarity = BadgeRarity.RARE,
            category = BadgeCategory.ENGAGEMENT,
            pointsReward = 50,
            requirement = 5
        ),

        // ========== SPECIAL Category ==========

        "badge-legend" to BadgeAsset(
            id = "badge-legend",
            name = "Event Legend",
            description = "A atteint 10,000 points",
            icon = "⭐",
            color = "#FFD700",  // Gold
            rarity = BadgeRarity.LEGENDARY,
            category = BadgeCategory.SPECIAL,
            pointsReward = 1000,
            requirement = 10000
        ),

        "badge-century-club" to BadgeAsset(
            id = "badge-century-club",
            name = "Club des Cent",
            description = "A atteint 100 points totaux",
            icon = "💯",
            color = "#FF9800",  // Material Orange 500
            rarity = BadgeRarity.COMMON,
            category = BadgeCategory.SPECIAL,
            pointsReward = 50,
            requirement = 100
        ),

        "badge-millenium-club" to BadgeAsset(
            id = "badge-millenium-club",
            name = "Club des Mille",
            description = "A atteint 1000 points totaux",
            icon = "💎",
            color = "#00BCD4",  // Material Cyan 500
            rarity = BadgeRarity.EPIC,
            category = BadgeCategory.SPECIAL,
            pointsReward = 200,
            requirement = 1000
        )
    )

    /**
     * Gets a specific badge asset by its ID.
     *
     * @param badgeId The unique identifier of the badge
     * @return The BadgeAsset if found, null otherwise
     */
    fun getBadgeAsset(badgeId: String): BadgeAsset? {
        return BADGES[badgeId]
    }

    /**
     * Gets all badge assets as a list.
     *
     * @return List of all BadgeAsset objects
     */
    fun getAllBadgeAssets(): List<BadgeAsset> {
        return BADGES.values.toList()
    }

    /**
     * Gets all badge assets for a specific category.
     *
     * @param category The category to filter by
     * @return List of BadgeAsset objects in the specified category
     */
    fun getBadgesByCategory(category: BadgeCategory): List<BadgeAsset> {
        return BADGES.values.filter { it.category == category }
    }

    /**
     * Gets all badge assets for a specific rarity.
     *
     * @param rarity The rarity tier to filter by
     * @return List of BadgeAsset objects with the specified rarity
     */
    fun getBadgesByRarity(rarity: BadgeRarity): List<BadgeAsset> {
        return BADGES.values.filter { it.rarity == rarity }
    }

    /**
     * Gets the color for a specific rarity tier.
     * Useful for displaying rarity indicators.
     *
     * @param rarity The rarity tier
     * @return Hex color code for the rarity
     */
    fun getColorForRarity(rarity: BadgeRarity): String {
        return when (rarity) {
            BadgeRarity.COMMON -> "#9E9E9E"      // Grey
            BadgeRarity.RARE -> "#2196F3"        // Blue
            BadgeRarity.EPIC -> "#9C27B0"        // Purple
            BadgeRarity.LEGENDARY -> "#FFD700"   // Gold
        }
    }
}

/**
 * Visual asset definition for a badge.
 *
 * @property id Unique identifier for the badge (e.g., "badge-super-organizer")
 * @property name Display name of the badge (e.g., "Super Organisateur")
 * @property description Description of how to earn the badge
 * @property icon Emoji or icon identifier for display
 * @property color Hex color code (Material Design 3 palette)
 * @property rarity Rarity level affecting display and prestige
 * @property category Category of the badge for organization
 * @property pointsReward Points awarded when badge is unlocked
 * @property requirement Threshold needed to unlock (events count, votes, etc.)
 */
@Serializable
data class BadgeAsset(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val color: String,
    val rarity: BadgeRarity,
    val category: BadgeCategory,
    val pointsReward: Int,
    val requirement: Int
)
