package com.guyghost.wakeve.suggestions

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.models.SuggestionInteractionType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for DatabaseSuggestionPreferencesRepository — interaction tracking methods.
 *
 * Verifies that the 7 interaction tracking methods correctly persist and retrieve
 * data from the suggestion_interactions SQLDelight table.
 */
class SuggestionInteractionTrackingTest {

    private lateinit var repository: DatabaseSuggestionPreferencesRepository

    @BeforeTest
    fun setUp() {
        repository = DatabaseSuggestionPreferencesRepository(createFreshTestDatabase())
    }

    // ──────────────────────────────────────────────────────────────
    // trackInteraction
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `trackInteraction stores interaction in database`() {
        repository.trackInteraction(
            userId = "user-1",
            suggestionId = "suggestion-abc",
            interactionType = SuggestionInteractionType.CLICKED
        )

        val history = repository.getInteractionHistory("user-1")

        assertEquals(1, history.size)
        assertEquals("user-1", history.first().userId)
        assertEquals("suggestion-abc", history.first().suggestionId)
        assertEquals(SuggestionInteractionType.CLICKED, history.first().interactionType)
        assertTrue(history.first().metadata.isEmpty())
    }

    @Test
    fun `trackInteraction multiple times accumulates history`() {
        repository.trackInteraction("user-1", "suggestion-abc", SuggestionInteractionType.VIEWED)
        repository.trackInteraction("user-1", "suggestion-abc", SuggestionInteractionType.CLICKED)
        repository.trackInteraction("user-1", "suggestion-xyz", SuggestionInteractionType.DISMISSED)

        val history = repository.getInteractionHistory("user-1")

        assertEquals(3, history.size)
    }

    @Test
    fun `trackInteraction is scoped to userId`() {
        repository.trackInteraction("user-1", "suggestion-abc", SuggestionInteractionType.CLICKED)
        repository.trackInteraction("user-2", "suggestion-abc", SuggestionInteractionType.VIEWED)

        assertEquals(1, repository.getInteractionHistory("user-1").size)
        assertEquals(1, repository.getInteractionHistory("user-2").size)
        assertTrue(repository.getInteractionHistory("user-3").isEmpty())
    }

    // ──────────────────────────────────────────────────────────────
    // trackInteractionWithMetadata
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `trackInteractionWithMetadata stores metadata`() {
        val metadata = mapOf("source" to "recommendation_panel", "position" to "3")

        repository.trackInteractionWithMetadata(
            userId = "user-1",
            suggestionId = "suggestion-abc",
            interactionType = SuggestionInteractionType.ACCEPTED,
            metadata = metadata
        )

        val history = repository.getInteractionHistory("user-1")

        assertEquals(1, history.size)
        assertEquals(SuggestionInteractionType.ACCEPTED, history.first().interactionType)
        assertEquals("recommendation_panel", history.first().metadata["source"])
        assertEquals("3", history.first().metadata["position"])
    }

    @Test
    fun `trackInteractionWithMetadata with empty metadata stores empty map`() {
        repository.trackInteractionWithMetadata(
            userId = "user-1",
            suggestionId = "suggestion-abc",
            interactionType = SuggestionInteractionType.DISMISSED,
            metadata = emptyMap()
        )

        val history = repository.getInteractionHistory("user-1")
        assertTrue(history.first().metadata.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────
    // getInteractionHistory
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `getInteractionHistory returns empty list for unknown user`() {
        val history = repository.getInteractionHistory("unknown-user")
        assertTrue(history.isEmpty())
    }

    @Test
    fun `getInteractionHistory returns all interaction types`() {
        SuggestionInteractionType.entries.forEach { type ->
            repository.trackInteraction("user-1", "suggestion-${type.name}", type)
        }

        val history = repository.getInteractionHistory("user-1")

        assertEquals(SuggestionInteractionType.entries.size, history.size)
        val types = history.map { it.interactionType }.toSet()
        assertEquals(SuggestionInteractionType.entries.toSet(), types)
    }

    // ──────────────────────────────────────────────────────────────
    // getRecentInteractions
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `getRecentInteractions filters by timestamp`() {
        val past = "2024-01-01T00:00:00Z"
        val futureEpoch = "2099-01-01T00:00:00Z"

        repository.trackInteraction("user-1", "suggestion-old", SuggestionInteractionType.VIEWED)
        repository.trackInteraction("user-1", "suggestion-new", SuggestionInteractionType.CLICKED)

        // With a past cutoff, both current-time interactions should be returned
        val allRecent = repository.getRecentInteractions("user-1", past)
        assertEquals(2, allRecent.size)

        // With a far-future cutoff, none should be returned
        val noneRecent = repository.getRecentInteractions("user-1", futureEpoch)
        assertTrue(noneRecent.isEmpty())
    }

    @Test
    fun `getRecentInteractions returns empty for unknown user`() {
        val result = repository.getRecentInteractions("unknown", "2020-01-01T00:00:00Z")
        assertTrue(result.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────
    // getInteractionCountsByType
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `getInteractionCountsByType counts correctly`() {
        repository.trackInteraction("user-1", "s1", SuggestionInteractionType.VIEWED)
        repository.trackInteraction("user-1", "s2", SuggestionInteractionType.VIEWED)
        repository.trackInteraction("user-1", "s3", SuggestionInteractionType.CLICKED)

        val counts = repository.getInteractionCountsByType("user-1", "2020-01-01T00:00:00Z")

        assertEquals(2L, counts[SuggestionInteractionType.VIEWED])
        assertEquals(1L, counts[SuggestionInteractionType.CLICKED])
        assertFalse(counts.containsKey(SuggestionInteractionType.DISMISSED))
    }

    @Test
    fun `getInteractionCountsByType returns empty map for user with no interactions`() {
        val counts = repository.getInteractionCountsByType("unknown", "2020-01-01T00:00:00Z")
        assertTrue(counts.isEmpty())
    }

    @Test
    fun `getInteractionCountsByType excludes interactions before cutoff`() {
        repository.trackInteraction("user-1", "s1", SuggestionInteractionType.VIEWED)

        // Far-future cutoff: no interactions should be counted
        val counts = repository.getInteractionCountsByType("user-1", "2099-01-01T00:00:00Z")
        assertTrue(counts.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────
    // getTopSuggestions
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `getTopSuggestions returns suggestions ranked by interaction count`() {
        // suggestion-popular: 3 interactions; suggestion-rare: 1 interaction
        repeat(3) { repository.trackInteraction("user-1", "suggestion-popular", SuggestionInteractionType.VIEWED) }
        repository.trackInteraction("user-2", "suggestion-popular", SuggestionInteractionType.CLICKED)
        repository.trackInteraction("user-1", "suggestion-rare", SuggestionInteractionType.DISMISSED)

        val top = repository.getTopSuggestions("2020-01-01T00:00:00Z", limit = 10)

        assertTrue(top.isNotEmpty())
        assertEquals("suggestion-popular", top.first().first)
        assertTrue(top.first().second > top.last().second)
    }

    @Test
    fun `getTopSuggestions respects limit parameter`() {
        repeat(5) { i ->
            repository.trackInteraction("user-1", "suggestion-$i", SuggestionInteractionType.VIEWED)
        }

        val top3 = repository.getTopSuggestions("2020-01-01T00:00:00Z", limit = 3)
        assertEquals(3, top3.size)
    }

    @Test
    fun `getTopSuggestions returns empty list when no interactions`() {
        val top = repository.getTopSuggestions("2020-01-01T00:00:00Z", limit = 10)
        assertTrue(top.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────
    // cleanupOldInteractions
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `cleanupOldInteractions removes old interactions`() = runTest {
        repository.trackInteraction("user-1", "s1", SuggestionInteractionType.VIEWED)
        repository.trackInteraction("user-1", "s2", SuggestionInteractionType.CLICKED)

        assertEquals(2, repository.getInteractionHistory("user-1").size)

        // Cleanup with a far-future timestamp removes everything
        repository.cleanupOldInteractions("2099-01-01T00:00:00Z")

        assertTrue(repository.getInteractionHistory("user-1").isEmpty())
    }

    @Test
    fun `cleanupOldInteractions with old timestamp keeps recent interactions`() = runTest {
        repository.trackInteraction("user-1", "s1", SuggestionInteractionType.VIEWED)

        // Cleanup with a very old timestamp should not remove recent interactions
        repository.cleanupOldInteractions("2000-01-01T00:00:00Z")

        assertEquals(1, repository.getInteractionHistory("user-1").size)
    }
}
