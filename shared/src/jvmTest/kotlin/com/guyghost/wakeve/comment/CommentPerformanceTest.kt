package com.guyghost.wakeve.comment

import com.guyghost.wakeve.models.CommentRequest
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.comment.CommentNotificationService
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.random.Random

/**
 * Performance tests for comment system optimizations.
 *
 * Tests database indexes, pagination, caching, and memory usage.
 */
class CommentPerformanceTest {

    private lateinit var db: WakevDb
    private lateinit var repository: CommentRepository

    @Before
    fun setup() {
        // Note: In a real implementation, this would use an in-memory database
        // For now, we'll use mock implementations for testing
        db = createTestDatabase()
        repository = CommentRepository(db)
    }

    @Test
    fun `testLoad100CommentsWithPagination`() = runBlocking {
        // Create test event
        val eventId = "test-event-${Random.nextInt()}"

        // Create 100 comments
        repeat(100) { i ->
            val request = CommentRequest(
                section = CommentSection.GENERAL,
                sectionItemId = null,
                content = "Test comment $i with some content to make it realistic",
                parentCommentId = null
            )
            repository.createComment(eventId, "user-1", "Test User", request)
        }

        // Test pagination performance
        val startTime = System.currentTimeMillis()
        val page1 = repository.getTopLevelCommentsByEventPaginated(eventId, limit = 20, offset = 0)
        val duration1 = System.currentTimeMillis() - startTime

        assertTrue(duration1 < 1000, "Page 1 should load in < 1s, took ${duration1}ms")
        assertEquals(20, page1.items.size)
        assertTrue(page1.hasMore, "Should have more pages")
        assertEquals(20, page1.nextOffset, "Next offset should be 20")
    }

    @Test
    fun `testCacheReducesQueryTime`() = runBlocking {
        val eventId = "test-event-cache-${Random.nextInt()}"

        // Create 50 comments
        repeat(50) { i ->
            val request = CommentRequest(
                section = CommentSection.BUDGET,
                sectionItemId = null,
                content = "Cache test comment $i",
                parentCommentId = null
            )
            repository.createComment(eventId, "user-1", "Test User", request)
        }

        // First call (cache miss)
        val start1 = System.currentTimeMillis()
        val comments1 = repository.getCommentsByEventCached(eventId, useCache = true)
        val time1 = System.currentTimeMillis() - start1

        // Second call (cache hit)
        val start2 = System.currentTimeMillis()
        val comments2 = repository.getCommentsByEventCached(eventId, useCache = true)
        val time2 = System.currentTimeMillis() - start2

        assertTrue(time2 < time1, "Cache should be faster: ${time1}ms vs ${time2}ms")
        assertTrue(time2 < 10, "Cache hit should be < 10ms, took ${time2}ms")
        assertEquals(comments1.size, comments2.size, "Cached results should match")
    }

    @Test
    fun `testPaginationWithLargeDataset`() = runBlocking {
        val eventId = "test-event-large-${Random.nextInt()}"

        // Create 200 comments across different sections
        val sections = listOf(CommentSection.GENERAL, CommentSection.BUDGET, CommentSection.TRANSPORT)
        repeat(200) { i ->
            val request = CommentRequest(
                section = sections[i % sections.size],
                sectionItemId = null,
                content = "Large dataset comment $i with realistic content length",
                parentCommentId = null
            )
            repository.createComment(eventId, "user-$i", "User $i", request)
        }

        // Test multiple pages
        val page1 = repository.getTopLevelCommentsByEventPaginated(eventId, limit = 50, offset = 0)
        val page2 = repository.getTopLevelCommentsByEventPaginated(eventId, limit = 50, offset = 50)
        val page3 = repository.getTopLevelCommentsByEventPaginated(eventId, limit = 50, offset = 100)

        assertEquals(50, page1.items.size)
        assertEquals(50, page2.items.size)
        assertEquals(100, page3.items.size) // Should be 100 but hasMore=false for last page
        assertTrue(page1.hasMore)
        assertTrue(page2.hasMore)
        assertFalse(page3.hasMore)
    }

    @Test
    fun `testLazyLoadingRepliesPerformance`() = runBlocking {
        val eventId = "test-event-lazy-${Random.nextInt()}"

        // Create a top-level comment
        val topCommentRequest = CommentRequest(
            section = CommentSection.ACTIVITY,
            sectionItemId = "activity-1",
            content = "Top level comment for lazy loading test",
            parentCommentId = null
        )
        val topComment = repository.createComment(eventId, "user-1", "Test User", topCommentRequest)

        // Create 10 replies
        repeat(10) { i ->
            val replyRequest = CommentRequest(
                section = CommentSection.ACTIVITY,
                sectionItemId = "activity-1",
                content = "Reply $i to test lazy loading",
                parentCommentId = topComment.id
            )
            repository.createComment(eventId, "user-2", "Reply User", replyRequest)
        }

        // Test lazy loading (loadReplies = false)
        val startLazy = System.currentTimeMillis()
        val lazyResult = repository.getCommentsWithThreadsLazy(
            eventId = eventId,
            section = CommentSection.ACTIVITY,
            sectionItemId = "activity-1",
            loadReplies = false
        )
        val lazyTime = System.currentTimeMillis() - startLazy

        // Test eager loading (loadReplies = true)
        val startEager = System.currentTimeMillis()
        val eagerResult = repository.getCommentsWithThreadsLazy(
            eventId = eventId,
            section = CommentSection.ACTIVITY,
            sectionItemId = "activity-1",
            loadReplies = true
        )
        val eagerTime = System.currentTimeMillis() - startEager

        assertEquals(1, lazyResult.comments.size)
        assertEquals(1, eagerResult.comments.size)
        assertTrue(lazyResult.comments[0].hasMoreReplies, "Lazy loading should indicate more replies")
        assertEquals(10, eagerResult.comments[0].replies.size, "Eager loading should load all replies")

        // Lazy loading should be faster for this small dataset
        assertTrue(lazyTime <= eagerTime, "Lazy loading should be at least as fast")
    }

    @Test
    fun `testCacheInvalidationOnUpdate`() = runBlocking {
        val eventId = "test-event-invalidation-${Random.nextInt()}"

        // Create initial comment
        val request = CommentRequest(
            section = CommentSection.EQUIPMENT,
            sectionItemId = null,
            content = "Original content",
            parentCommentId = null
        )
        val comment = repository.createComment(eventId, "user-1", "Test User", request)

        // Load into cache
        val cachedBefore = repository.getCommentsByEventCached(eventId, useCache = true)

        // Update comment (should invalidate cache)
        repository.updateComment(comment.id, "Updated content")

        // Cache should be invalidated - next call should be cache miss
        val cachedAfter = repository.getCommentsByEventCached(eventId, useCache = true)

        assertEquals(1, cachedBefore.size)
        assertEquals(1, cachedAfter.size)
        assertEquals("Updated content", cachedAfter[0].content)
    }

    @Test
    fun `testPreCalculatedStatisticsPerformance`() = runBlocking {
        val eventId = "test-event-stats-${Random.nextInt()}"

        // Create comments across multiple sections
        val sections = CommentSection.values()
        repeat(100) { i ->
            val request = CommentRequest(
                section = sections[i % sections.size],
                sectionItemId = null,
                content = "Stats test comment $i",
                parentCommentId = null
            )
            repository.createComment(eventId, "user-${i % 10}", "User ${i % 10}", request)
        }

        // Test pre-calculated stats vs on-the-fly calculation
        val startPreCalc = System.currentTimeMillis()
        val preCalcStats = repository.getCommentSectionStats(eventId)
        val preCalcTime = System.currentTimeMillis() - startPreCalc

        val startOnFly = System.currentTimeMillis()
        val onFlyStats = repository.getCommentStatsBySection(eventId)
        val onFlyTime = System.currentTimeMillis() - startOnFly

        assertTrue(preCalcStats.isNotEmpty(), "Should have section statistics")
        assertEquals(onFlyStats.size, preCalcStats.size, "Should have same number of sections")

        // Pre-calculated should be at least as fast (likely much faster with larger datasets)
        assertTrue(preCalcTime <= onFlyTime + 50, "Pre-calculated stats should be competitive: ${preCalcTime}ms vs ${onFlyTime}ms")
    }

    @Test
    fun `testCacheMemoryLimits`() = runBlocking {
        val eventId = "test-event-memory-${Random.nextInt()}"

        // Create many different events to test cache eviction
        repeat(120) { i -> // More than cache limit of 100
            val eventIdTest = "event-$i"
            val request = CommentRequest(
                section = CommentSection.GENERAL,
                sectionItemId = null,
                content = "Memory test comment $i",
                parentCommentId = null
            )
            repository.createComment(eventIdTest, "user-1", "Test User", request)
        }

        // Load many into cache
        repeat(120) { i ->
            val eventIdTest = "event-$i"
            repository.getCommentsByEventCached(eventIdTest, useCache = true)
        }

        val cacheStats = repository.getCacheStats()
        assertTrue(cacheStats.totalEntries <= 100, "Cache should not exceed max size of 100, got ${cacheStats.totalEntries}")
        assertTrue(cacheStats.totalEntries > 0, "Should have some cached entries")
    }

    // Helper method to create test database
    // In real implementation, this would be an in-memory SQLite database
    private fun createTestDatabase(): WakevDb {
        // This is a placeholder - real implementation would create an in-memory database
        // For now, we'll return a mock or skip database-dependent tests
        throw NotImplementedError("Database setup needed for performance tests")
    }
}