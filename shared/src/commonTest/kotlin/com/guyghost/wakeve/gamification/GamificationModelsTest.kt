package com.guyghost.wakeve.gamification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for gamification models.
 *
 * Tests UserLevel progression, PointsAction enum,
 * AwardResult, UnlockResult, and level calculation logic.
 */
class GamificationModelsTest {

    // ========================================================================
    // PointsAction Tests
    // ========================================================================

    @Test
    fun `PointsAction has 7 actions`() {
        assertEquals(7, PointsAction.entries.size)
    }

    @Test
    fun `PointsAction contains all expected actions`() {
        val actions = PointsAction.entries
        assertTrue(actions.contains(PointsAction.CREATE_EVENT))
        assertTrue(actions.contains(PointsAction.VOTE))
        assertTrue(actions.contains(PointsAction.COMMENT))
        assertTrue(actions.contains(PointsAction.PARTICIPATE))
        assertTrue(actions.contains(PointsAction.CREATE_SCENARIO))
        assertTrue(actions.contains(PointsAction.VOTE_SCENARIO))
        assertTrue(actions.contains(PointsAction.INVITE_PARTICIPANT))
    }

    // ========================================================================
    // AwardResult Tests
    // ========================================================================

    @Test
    fun `AwardResult holds fields correctly`() {
        val result = AwardResult(pointsEarned = 50, newTotal = 150, badgesUnlocked = emptyList())
        assertEquals(50, result.pointsEarned)
        assertEquals(150, result.newTotal)
        assertTrue(result.badgesUnlocked.isEmpty())
    }

    @Test
    fun `AwardResult with badges`() {
        val badge = Badge(
            id = "badge-1", name = "First Vote",
            description = "Voted for the first time",
            icon = "🗳️", requirement = 1, pointsReward = 25,
            category = BadgeCategory.VOTING, rarity = BadgeRarity.COMMON
        )
        val result = AwardResult(pointsEarned = 25, newTotal = 75, badgesUnlocked = listOf(badge))
        assertEquals(1, result.badgesUnlocked.size)
        assertEquals("badge-1", result.badgesUnlocked.first().id)
    }

    // ========================================================================
    // UnlockResult Tests
    // ========================================================================

    @Test
    fun `UnlockResult for successful unlock`() {
        val result = UnlockResult(unlocked = true, message = "Badge unlocked!", pointsReward = 50)
        assertTrue(result.unlocked)
        assertEquals(50, result.pointsReward)
    }

    @Test
    fun `UnlockResult for failed unlock`() {
        val result = UnlockResult(unlocked = false, message = "Already unlocked", pointsReward = 0)
        assertTrue(!result.unlocked)
        assertEquals(0, result.pointsReward)
    }

    // ========================================================================
    // UserLevel.LEVEL_THRESHOLDS Tests
    // ========================================================================

    @Test
    fun `LEVEL_THRESHOLDS has 10 levels`() {
        assertEquals(10, UserLevel.LEVEL_THRESHOLDS.size)
    }

    @Test
    fun `first level starts at 0 points`() {
        assertEquals(0, UserLevel.LEVEL_THRESHOLDS.first().first)
    }

    @Test
    fun `level thresholds are in ascending order`() {
        val thresholds = UserLevel.LEVEL_THRESHOLDS.map { it.first }
        for (i in 1 until thresholds.size) {
            assertTrue(thresholds[i] > thresholds[i - 1],
                "Threshold $i (${thresholds[i]}) should be > threshold ${i-1} (${thresholds[i-1]})")
        }
    }

    // ========================================================================
    // UserLevel.fromPoints Tests
    // ========================================================================

    @Test
    fun `0 points gives level 1 beginner`() {
        val level = UserLevel.fromPoints(0)
        assertEquals(1, level.level)
        assertEquals("level.beginner", level.nameKey)
        assertEquals(0, level.currentPoints)
        assertEquals(0, level.pointsForCurrentLevel)
    }

    @Test
    fun `1 point gives level 1`() {
        val level = UserLevel.fromPoints(1)
        assertEquals(1, level.level)
        assertEquals(0, level.pointsForCurrentLevel)
    }

    @Test
    fun `50 points gives level 2 explorer`() {
        val level = UserLevel.fromPoints(50)
        assertEquals(2, level.level)
        assertEquals("level.explorer", level.nameKey)
    }

    @Test
    fun `51 points gives level 2`() {
        val level = UserLevel.fromPoints(51)
        assertEquals(2, level.level)
    }

    @Test
    fun `150 points gives level 3 contributor`() {
        val level = UserLevel.fromPoints(150)
        assertEquals(3, level.level)
        assertEquals("level.contributor", level.nameKey)
    }

    @Test
    fun `300 points gives level 4 organizer`() {
        val level = UserLevel.fromPoints(300)
        assertEquals(4, level.level)
        assertEquals("level.organizer", level.nameKey)
    }

    @Test
    fun `500 points gives level 5 expert`() {
        val level = UserLevel.fromPoints(500)
        assertEquals(5, level.level)
        assertEquals("level.expert", level.nameKey)
    }

    @Test
    fun `800 points gives level 6 master`() {
        val level = UserLevel.fromPoints(800)
        assertEquals(6, level.level)
        assertEquals("level.master", level.nameKey)
    }

    @Test
    fun `1200 points gives level 7 champion`() {
        val level = UserLevel.fromPoints(1200)
        assertEquals(7, level.level)
        assertEquals("level.champion", level.nameKey)
    }

    @Test
    fun `3500 points gives level 10 transcendent`() {
        val level = UserLevel.fromPoints(3500)
        assertEquals(10, level.level)
        assertEquals("level.transcendent", level.nameKey)
    }

    @Test
    fun `very high points stays at max level`() {
        val level = UserLevel.fromPoints(999999)
        assertEquals(10, level.level)
    }

    // ========================================================================
    // Progress Calculation Tests
    // ========================================================================

    @Test
    fun `progress at level start is 0`() {
        val level = UserLevel.fromPoints(50) // Level 2 starts at 50
        assertEquals(0f, level.progressToNextLevel, 0.01f)
    }

    @Test
    fun `progress is between 0 and 1`() {
        listOf(0, 25, 75, 150, 300, 500).forEach { pts ->
            val level = UserLevel.fromPoints(pts)
            assertTrue(level.progressToNextLevel in 0f..1f,
                "Progress for $pts pts should be in [0,1], got ${level.progressToNextLevel}")
        }
    }

    @Test
    fun `progress at midpoint of level is roughly 0 5`() {
        // Level 1: 0-50 pts. Midpoint = 25 pts
        val level = UserLevel.fromPoints(25)
        assertEquals(0.5f, level.progressToNextLevel, 0.01f)
    }

    @Test
    fun `nextLevel threshold is correct`() {
        val level = UserLevel.fromPoints(0) // Level 1: next threshold = 50
        assertEquals(50, level.pointsForNextLevel)
    }

    // ========================================================================
    // Localization Tests
    // ========================================================================

    @Test
    fun `localizedName returns French by default`() {
        val name = UserLevel.localizedName("level.beginner")
        assertEquals("Débutant", name)
    }

    @Test
    fun `localizedName returns English when requested`() {
        val name = UserLevel.localizedName("level.beginner", "en")
        assertEquals("Beginner", name)
    }

    @Test
    fun `localizedName returns Spanish`() {
        val name = UserLevel.localizedName("level.expert", "es")
        assertEquals("Experto", name)
    }

    @Test
    fun `localizedName falls back to French for unknown locale`() {
        val name = UserLevel.localizedName("level.master", "ja")
        assertEquals("Maître", name)
    }

    @Test
    fun `localizedName returns key for unknown level`() {
        val name = UserLevel.localizedName("level.unknown")
        assertEquals("level.unknown", name)
    }

    @Test
    fun `fromPoints localizes name correctly`() {
        val level = UserLevel.fromPoints(0, "en")
        assertEquals("Beginner", level.name)
    }

    @Test
    fun `fromPoints with French locale`() {
        val level = UserLevel.fromPoints(0, "fr")
        assertEquals("Débutant", level.name)
    }
}
