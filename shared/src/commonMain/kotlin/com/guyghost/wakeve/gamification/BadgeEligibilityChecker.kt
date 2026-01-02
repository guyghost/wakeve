package com.guyghost.wakeve.gamification

import com.guyghost.wakeve.gamification.repository.UserBadgesRepository
import com.guyghost.wakeve.gamification.repository.UserPointsRepository

/**
 * Checks badge eligibility for users based on their activities.
 * Evaluates all badge requirements and returns badges the user has earned.
 */
class BadgeEligibilityChecker(
    private val userPointsRepository: UserPointsRepository,
    private val userBadgesRepository: UserBadgesRepository
) {
    /**
     * Checks all badge eligibility for a user.
     *
     * @param userId The user to check eligibility for
     * @param eventCount Number of events created by the user
     * @param participationCount Number of events participated in
     * @param voteCount Number of votes cast
     * @param commentCount Number of comments made
     * @param scenarioCount Number of scenarios created
     * @param scenarioVoteCount Number of scenario votes cast
     * @return List of badges the user is eligible to unlock
     */
    suspend fun checkEligibility(
        userId: String,
        eventCount: Int,
        participationCount: Int,
        voteCount: Int,
        commentCount: Int,
        scenarioCount: Int,
        scenarioVoteCount: Int
    ): List<Badge> {
        val eligibleBadges = mutableListOf<Badge>()
        val userBadges = userBadgesRepository.getUserBadges(userId)
        val existingBadgeIds = userBadges.badges.map { it.id }.toSet()
        val badgeDefinitions = userBadgesRepository.getAllBadgeDefinitions()
        val userPoints = userPointsRepository.getUserPointsOrDefault(userId)
        val totalPoints = userPoints.totalPoints

        // Creation badges
        if ("badge-first-event" !in existingBadgeIds && eventCount >= 1) {
            badgeDefinitions.find { it.id == "badge-first-event" }?.let {
                eligibleBadges.add(it)
            }
        }
        if ("badge-dedicated" !in existingBadgeIds && eventCount >= 5) {
            badgeDefinitions.find { it.id == "badge-dedicated" }?.let {
                eligibleBadges.add(it)
            }
        }
        if ("badge-super-organizer" !in existingBadgeIds && eventCount >= 10) {
            badgeDefinitions.find { it.id == "badge-super-organizer" }?.let {
                eligibleBadges.add(it)
            }
        }
        if ("badge-event-master" !in existingBadgeIds && eventCount >= 25) {
            badgeDefinitions.find { it.id == "badge-event-master" }?.let {
                eligibleBadges.add(it)
            }
        }

        // Voting badges
        if ("badge-first-vote" !in existingBadgeIds && voteCount >= 1) {
            badgeDefinitions.find { it.id == "badge-first-vote" }?.let {
                eligibleBadges.add(it)
            }
        }
        if ("badge-dedicated-voter" !in existingBadgeIds && voteCount >= 10) {
            badgeDefinitions.find { it.id == "badge-dedicated-voter" }?.let {
                eligibleBadges.add(it)
            }
        }
        if ("badge-quick-responder" !in existingBadgeIds && voteCount >= 5) {
            badgeDefinitions.find { it.id == "badge-quick-responder" }?.let {
                eligibleBadges.add(it)
            }
        }

        // Participation badges
        if ("badge-first-steps" !in existingBadgeIds && participationCount >= 1) {
            badgeDefinitions.find { it.id == "badge-first-steps" }?.let {
                eligibleBadges.add(it)
            }
        }
        if ("badge-regular-attendee" !in existingBadgeIds && participationCount >= 5) {
            badgeDefinitions.find { it.id == "badge-regular-attendee" }?.let {
                eligibleBadges.add(it)
            }
        }
        if ("badge-social-butterfly" !in existingBadgeIds && participationCount >= 10) {
            badgeDefinitions.find { it.id == "badge-social-butterfly" }?.let {
                eligibleBadges.add(it)
            }
        }
        if ("badge-party-animal" !in existingBadgeIds && participationCount >= 25) {
            badgeDefinitions.find { it.id == "badge-party-animal" }?.let {
                eligibleBadges.add(it)
            }
        }

        // Engagement badges
        if ("badge-chatty" !in existingBadgeIds && commentCount >= 10) {
            badgeDefinitions.find { it.id == "badge-chatty" }?.let {
                eligibleBadges.add(it)
            }
        }
        if ("badge-voice-of-reason" !in existingBadgeIds && commentCount >= 25) {
            badgeDefinitions.find { it.id == "badge-voice-of-reason" }?.let {
                eligibleBadges.add(it)
            }
        }
        if ("badge-scenario-creator" !in existingBadgeIds && scenarioCount >= 5) {
            badgeDefinitions.find { it.id == "badge-scenario-creator" }?.let {
                eligibleBadges.add(it)
            }
        }
        if ("badge-opinionated" !in existingBadgeIds && scenarioVoteCount >= 20) {
            badgeDefinitions.find { it.id == "badge-opinionated" }?.let {
                eligibleBadges.add(it)
            }
        }

        // Special badges
        if ("badge-century-club" !in existingBadgeIds && totalPoints >= 100) {
            badgeDefinitions.find { it.id == "badge-century-club" }?.let {
                eligibleBadges.add(it)
            }
        }
        if ("badge-millenium-club" !in existingBadgeIds && totalPoints >= 1000) {
            badgeDefinitions.find { it.id == "badge-millenium-club" }?.let {
                eligibleBadges.add(it)
            }
        }

        return eligibleBadges
    }

    /**
     * Simplified eligibility check using repository data.
     *
     * @param userId The user to check
     * @return List of badges the user is eligible to unlock
     */
    suspend fun checkEligibility(userId: String): List<Badge> {
        // This is a placeholder - in real implementation, you would fetch
        // the actual counts from respective repositories
        return checkEligibility(
            userId = userId,
            eventCount = 0,
            participationCount = 0,
            voteCount = 0,
            commentCount = 0,
            scenarioCount = 0,
            scenarioVoteCount = 0
        )
    }
}
