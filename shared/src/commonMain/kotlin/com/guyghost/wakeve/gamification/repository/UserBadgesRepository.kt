package com.guyghost.wakeve.gamification.repository

import com.guyghost.wakeve.gamification.Badge
import com.guyghost.wakeve.gamification.BadgeCategory
import com.guyghost.wakeve.gamification.BadgeRarity
import com.guyghost.wakeve.gamification.UserBadges

/**
 * Repository interface for managing user badges.
 * Provides an abstraction layer for badge data access.
 */
interface UserBadgesRepository {
    suspend fun getUserBadges(userId: String): UserBadges
    suspend fun getAllBadgeDefinitions(): List<Badge>
    suspend fun unlockBadge(userId: String, badgeId: String): Boolean
    suspend fun userHasBadge(userId: String, badgeId: String): Boolean
    suspend fun getNearCompletionBadges(userId: String): List<Badge>
    suspend fun getBadgeCountByRarity(userId: String): Map<BadgeRarity, Int>
    suspend fun getNextBadge(userId: String): Badge?
    suspend fun getUserBadgePoints(userId: String): Int
    suspend fun acknowledgeBadge(userId: String, badgeId: String)
    suspend fun getUnacknowledgedBadges(userId: String): List<Badge>
    suspend fun deleteAllUserBadges(userId: String)
}

/**
 * In-memory implementation of UserBadgesRepository.
 * Uses simple collections for storage, suitable for testing and development.
 */
class InMemoryUserBadgesRepository : UserBadgesRepository {
    private val userBadgesMap = mutableMapOf<String, MutableList<Badge>>()
    private val unlockedAtMap = mutableMapOf<String, MutableMap<String, String>>()

    init {
        // Seed badge definitions
        seedBadgeDefinitions()
    }

    private val badgeDefinitions = mutableListOf<Badge>()

    private fun seedBadgeDefinitions() {
        // Creation badges
        badgeDefinitions.add(createBadge("badge-first-event", "Premier Événement",
            "A créé son premier événement", "🎉", 1, 50, BadgeCategory.CREATION, BadgeRarity.COMMON))
        badgeDefinitions.add(createBadge("badge-dedicated", "Organisateur Dévoué",
            "A créé 5 événements", "💪", 5, 75, BadgeCategory.CREATION, BadgeRarity.RARE))
        badgeDefinitions.add(createBadge("badge-super-organizer", "Super Organisateur",
            "A créé 10 événements", "🏆", 10, 100, BadgeCategory.CREATION, BadgeRarity.EPIC))
        badgeDefinitions.add(createBadge("badge-event-master", "Maître des Événements",
            "A créé 25 événements", "👑", 25, 250, BadgeCategory.CREATION, BadgeRarity.LEGENDARY))

        // Voting badges
        badgeDefinitions.add(createBadge("badge-first-vote", "Premier Vote",
            "A participé à son premier vote", "👍", 1, 25, BadgeCategory.VOTING, BadgeRarity.COMMON))
        badgeDefinitions.add(createBadge("badge-dedicated-voter", "Voteur Dévoué",
            "A participé à 10 votes", "🗳️", 10, 50, BadgeCategory.VOTING, BadgeRarity.RARE))
        badgeDefinitions.add(createBadge("badge-quick-responder", "Réponse Rapide",
            "A répondu à 5 votes en moins de 2 heures", "⚡", 5, 75, BadgeCategory.VOTING, BadgeRarity.EPIC))
        badgeDefinitions.add(createBadge("badge-early-bird", "Levé Tot",
            "A voté 24 heures de suite", "🌅", 24, 100, BadgeCategory.VOTING, BadgeRarity.EPIC))

        // Participation badges
        badgeDefinitions.add(createBadge("badge-first-steps", "Premiers Pas",
            "A participé à son premier événement", "👣", 1, 50, BadgeCategory.PARTICIPATION, BadgeRarity.COMMON))
        badgeDefinitions.add(createBadge("badge-regular-attendee", "Participant Régulier",
            "A participé à 5 événements", "📅", 5, 75, BadgeCategory.PARTICIPATION, BadgeRarity.RARE))
        badgeDefinitions.add(createBadge("badge-social-butterfly", "Papillon Social",
            "A participé à 10 événements", "🦋", 10, 100, BadgeCategory.PARTICIPATION, BadgeRarity.EPIC))
        badgeDefinitions.add(createBadge("badge-party-animal", "Fêteur",
            "A participé à 25 événements", "🎊", 25, 250, BadgeCategory.PARTICIPATION, BadgeRarity.LEGENDARY))

        // Engagement badges
        badgeDefinitions.add(createBadge("badge-chatty", "Bavard",
            "A commenté 10 fois", "💬", 10, 50, BadgeCategory.ENGAGEMENT, BadgeRarity.RARE))
        badgeDefinitions.add(createBadge("badge-voice-of-reason", "Voix de la Raison",
            "A commenté 25 fois", "🗨️", 25, 100, BadgeCategory.ENGAGEMENT, BadgeRarity.EPIC))
        badgeDefinitions.add(createBadge("badge-scenario-creator", "Créateur de Scénarios",
            "A créé 5 scénarios", "📝", 5, 75, BadgeCategory.ENGAGEMENT, BadgeRarity.RARE))
        badgeDefinitions.add(createBadge("badge-opinionated", "Avisé",
            "A voté sur 20 scénarios", "🤝", 20, 60, BadgeCategory.ENGAGEMENT, BadgeRarity.COMMON))

        // Special badges
        badgeDefinitions.add(createBadge("badge-gamification-pioneer", "Pionnier",
            "A participé au lancement de la gamification", "🚀", 0, 200, BadgeCategory.SPECIAL, BadgeRarity.LEGENDARY))
        badgeDefinitions.add(createBadge("badge-century-club", "Club des Cent",
            "A atteint 100 points totaux", "💯", 100, 50, BadgeCategory.SPECIAL, BadgeRarity.RARE))
        badgeDefinitions.add(createBadge("badge-millenium-club", "Club des Mille",
            "A atteint 1000 points totaux", "💎", 1000, 150, BadgeCategory.SPECIAL, BadgeRarity.EPIC))
    }

    private fun createBadge(
        id: String, name: String, description: String, icon: String,
        requirement: Int, pointsReward: Int, category: BadgeCategory, rarity: BadgeRarity
    ): Badge {
        return Badge(id, name, description, icon, requirement, pointsReward, category, rarity)
    }

    override suspend fun getUserBadges(userId: String): UserBadges {
        val badges = userBadgesMap[userId]?.toList() ?: emptyList()
        val unlockedAt = unlockedAtMap[userId]?.toMap() ?: emptyMap()
        return UserBadges(userId, badges, unlockedAt)
    }

    override suspend fun getAllBadgeDefinitions(): List<Badge> {
        return badgeDefinitions.toList()
    }

    override suspend fun unlockBadge(userId: String, badgeId: String): Boolean {
        if (userHasBadge(userId, badgeId)) return false

        val badge = badgeDefinitions.find { it.id == badgeId } ?: return false

        userBadgesMap.getOrPut(userId) { mutableListOf() }.add(badge)
        unlockedAtMap.getOrPut(userId) { mutableMapOf() }[badgeId] = ""

        return true
    }

    override suspend fun userHasBadge(userId: String, badgeId: String): Boolean {
        return userBadgesMap[userId]?.any { it.id == badgeId } == true
    }

    override suspend fun getNearCompletionBadges(userId: String): List<Badge> {
        // Simplified implementation
        return emptyList()
    }

    override suspend fun getBadgeCountByRarity(userId: String): Map<BadgeRarity, Int> {
        val badges = userBadgesMap[userId] ?: return emptyMap()
        return badges.groupBy { it.rarity }.mapValues { it.value.size }
    }

    override suspend fun getNextBadge(userId: String): Badge? {
        val userBadges = userBadgesMap[userId]?.map { it.id }?.toSet() ?: emptySet()
        return badgeDefinitions.firstOrNull { it.id !in userBadges }
    }

    override suspend fun getUserBadgePoints(userId: String): Int {
        return userBadgesMap[userId]?.sumOf { it.pointsReward } ?: 0
    }

    override suspend fun acknowledgeBadge(userId: String, badgeId: String) {
        // No-op for in-memory implementation
    }

    override suspend fun getUnacknowledgedBadges(userId: String): List<Badge> {
        return userBadgesMap[userId]?.toList() ?: emptyList()
    }

    override suspend fun deleteAllUserBadges(userId: String) {
        userBadgesMap.remove(userId)
        unlockedAtMap.remove(userId)
    }
}
