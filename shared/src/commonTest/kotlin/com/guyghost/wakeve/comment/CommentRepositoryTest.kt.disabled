package com.guyghost.wakeve.comment

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.Comment
import com.guyghost.wakeve.models.CommentRequest
import com.guyghost.wakeve.models.CommentSection
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CommentRepository
 * Tests CRUD operations, pagination, and thread building
 */
class CommentRepositoryTest {

    private lateinit var db: WakevDb
    private lateinit var repository: CommentRepository

    @Before
    fun createTestDatabase() {
        // Create in-memory database for testing
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WakevDb.Schema.create(driver)
        db = WakevDb(driver)
        repository = CommentRepository(db)
    }

    @After
    fun closeDatabase() {
        // No need to close in-memory database
    }

    // ==================== Create Comment Tests ====================

    @Test
    fun `createComment creates new comment successfully`() {
        // Given
        val eventId = "event-1"
        val authorId = "user-1"
        val authorName = "Test User"
        val request = CommentRequest(
            section = CommentSection.GENERAL,
            content = "This is a test comment"
        )

        // When
        val comment = repository.createComment(
            eventId = eventId,
            authorId = authorId,
            authorName = authorName,
            request = request
        )

        // Then
        assertNotNull(comment)
        assertEquals(eventId, comment.eventId)
        assertEquals(authorId, comment.authorId)
        assertEquals(authorName, comment.authorName)
        assertEquals(request.content, comment.content)
        assertEquals(CommentSection.GENERAL, comment.section)
        assertNull(comment.parentCommentId)
        assertFalse(comment.isEdited)
        assertEquals(0, comment.replyCount)
        assertNotNull(comment.id)
        assertNotNull(comment.createdAt)
    }

    @Test
    fun `createComment creates reply successfully`() {
        // Given - Create parent comment first
        val eventId = "event-1"
        val parentRequest = CommentRequest(
            section = CommentSection.GENERAL,
            content = "Parent comment"
        )
        val parentComment = repository.createComment(
            eventId = eventId,
            authorId = "user-1",
            authorName = "Parent Author",
            request = parentRequest
        )

        // When - Create reply
        val replyRequest = CommentRequest(
            section = CommentSection.GENERAL,
            content = "Reply comment",
            parentCommentId = parentComment.id
        )
        val reply = repository.createComment(
            eventId = eventId,
            authorId = "user-2",
            authorName = "Reply Author",
            request = replyRequest
        )

        // Then
        assertNotNull(reply)
        assertEquals(parentComment.id, reply.parentCommentId)
        assertEquals("Reply comment", reply.content)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createComment throws when parent comment does not exist`() {
        // Given
        val request = CommentRequest(
            section = CommentSection.GENERAL,
            content = "Reply to non-existent comment",
            parentCommentId = "non-existent-id"
        )

        // When
        repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "Test User",
            request = request
        )
    }

    // ==================== Get Comment Tests ====================

    @Test
    fun `getCommentById returns comment when exists`() {
        // Given
        val created = repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "Test User",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Test")
        )

        // When
        val retrieved = repository.getCommentById(created.id)

        // Then
        assertNotNull(retrieved)
        assertEquals(created.id, retrieved?.id)
        assertEquals(created.content, retrieved?.content)
    }

    @Test
    fun `getCommentById returns null when not exists`() {
        // When
        val result = repository.getCommentById("non-existent-id")

        // Then
        assertNull(result)
    }

    @Test
    fun `getCommentsByEvent returns all comments for event`() {
        // Given
        repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Comment 1")
        )
        repository.createComment(
            eventId = "event-1",
            authorId = "user-2",
            authorName = "User 2",
            request = CommentRequest(section = CommentSection.SCENARIO, content = "Comment 2")
        )
        repository.createComment(
            eventId = "event-2",  // Different event
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Comment 3")
        )

        // When
        val event1Comments = repository.getCommentsByEvent("event-1")

        // Then
        assertEquals(2, event1Comments.size)
        assertTrue(event1Comments.all { it.eventId == "event-1" })
    }

    // ==================== Get Top-Level Comments Tests ====================

    @Test
    fun `getTopLevelComments returns only parent comments`() {
        // Given
        val parent = repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Parent")
        )
        repository.createComment(
            eventId = "event-1",
            authorId = "user-2",
            authorName = "User 2",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Reply", parentCommentId = parent.id)
        )

        // When
        val topLevel = repository.getTopLevelComments("event-1")

        // Then
        assertEquals(1, topLevel.size)
        assertNull(topLevel[0].parentCommentId)
    }

    @Test
    fun `getTopLevelComments filters by section`() {
        // Given
        repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.GENERAL, content = "General comment")
        )
        repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.SCENARIO, content = "Scenario comment")
        )

        // When
        val generalComments = repository.getTopLevelComments("event-1", CommentSection.GENERAL)
        val scenarioComments = repository.getTopLevelComments("event-1", CommentSection.SCENARIO)

        // Then
        assertEquals(1, generalComments.size)
        assertEquals(CommentSection.GENERAL, generalComments[0].section)
        assertEquals(1, scenarioComments.size)
        assertEquals(CommentSection.SCENARIO, scenarioComments[0].section)
    }

    // ==================== Get Replies Tests ====================

    @Test
    fun `getReplies returns only direct replies`() {
        // Given
        val parent = repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Parent")
        )
        val reply1 = repository.createComment(
            eventId = "event-1",
            authorId = "user-2",
            authorName = "User 2",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Reply 1", parentCommentId = parent.id)
        )
        // Create a reply to the reply (nested)
        repository.createComment(
            eventId = "event-1",
            authorId = "user-3",
            authorName = "User 3",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Reply to Reply", parentCommentId = reply1.id)
        )

        // When
        val directReplies = repository.getReplies(parent.id)

        // Then
        assertEquals(1, directReplies.size)
        assertEquals(reply1.id, directReplies[0].id)
    }

    // ==================== Update Comment Tests ====================

    @Test
    fun `updateComment updates content and marks as edited`() {
        // Given
        val original = repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Original content")
        )
        val newContent = "Updated content"

        // When
        val updated = repository.updateComment(original.id, newContent)

        // Then
        assertNotNull(updated)
        assertEquals(newContent, updated?.content)
        assertTrue(updated?.isEdited == true)
        assertNotNull(updated?.updatedAt)
    }

    @Test
    fun `updateComment returns null for non-existent comment`() {
        // When
        val result = repository.updateComment("non-existent-id", "New content")

        // Then
        assertNull(result)
    }

    // ==================== Delete Comment Tests ====================

    @Test
    fun `deleteComment removes comment from database`() {
        // Given
        val comment = repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.GENERAL, content = "To be deleted")
        )

        // When
        repository.deleteComment(comment.id)

        // Then
        assertNull(repository.getCommentById(comment.id))
    }

    @Test
    fun `deleteComment decrements parent reply count`() {
        // Given
        val parent = repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Parent")
        )
        repository.createComment(
            eventId = "event-1",
            authorId = "user-2",
            authorName = "User 2",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Reply", parentCommentId = parent.id)
        )

        // When
        val comments = repository.getCommentsByEvent("event-1")
        val parentComment = comments.find { it.id == parent.id }

        // Then - Reply count should be 1
        assertEquals(1, parentComment?.replyCount)

        // When - Delete the reply
        val reply = comments.find { it.parentCommentId == parent.id }
        repository.deleteComment(reply!!.id)

        // Then - Parent reply count should be 0
        val updatedParent = repository.getCommentById(parent.id)
        assertEquals(0, updatedParent?.replyCount)
    }

    // ==================== Pagination Tests ====================

    @Test
    fun `getTopLevelCommentsByEventPaginated returns paginated results`() {
        // Given - Create 5 comments
        repeat(5) { i ->
            repository.createComment(
                eventId = "event-1",
                authorId = "user-$i",
                authorName = "User $i",
                request = CommentRequest(section = CommentSection.GENERAL, content = "Comment $i")
            )
        }

        // When - Get first page (limit 2)
        val page1 = repository.getTopLevelCommentsByEventPaginated("event-1", limit = 2, offset = 0)

        // Then
        assertEquals(2, page1.items.size)
        assertTrue(page1.hasMore)
        assertEquals(2, page1.nextOffset)

        // When - Get second page
        val page2 = repository.getTopLevelCommentsByEventPaginated("event-1", limit = 2, offset = 2)

        // Then
        assertEquals(2, page2.items.size)
        assertTrue(page2.hasMore)
        assertEquals(4, page2.nextOffset)

        // When - Get last page
        val page3 = repository.getTopLevelCommentsByEventPaginated("event-1", limit = 2, offset = 4)

        // Then
        assertEquals(1, page3.items.size)
        assertFalse(page3.hasMore)
        assertNull(page3.nextOffset)
    }

    // ==================== Thread Building Tests ====================

    @Test
    fun `getCommentThread builds thread with all replies`() {
        // Given
        val parent = repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Parent")
        )
        val reply1 = repository.createComment(
            eventId = "event-1",
            authorId = "user-2",
            authorName = "User 2",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Reply 1", parentCommentId = parent.id)
        )
        val reply2 = repository.createComment(
            eventId = "event-1",
            authorId = "user-3",
            authorName = "User 3",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Reply 2", parentCommentId = parent.id)
        )

        // When
        val thread = repository.getCommentThread(parent.id)

        // Then
        assertNotNull(thread)
        assertEquals(parent.id, thread?.comment?.id)
        assertEquals(2, thread?.replies?.size)
        assertTrue(thread?.replies?.any { it.id == reply1.id } == true)
        assertTrue(thread?.replies?.any { it.id == reply2.id } == true)
    }

    @Test
    fun `getCommentsWithThreads groups comments by threads`() {
        // Given
        repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Thread 1")
        )
        repository.createComment(
            eventId = "event-1",
            authorId = "user-2",
            authorName = "User 2",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Thread 2")
        )

        // When
        val threads = repository.getCommentsWithThreads("event-1", CommentSection.GENERAL)

        // Then
        assertEquals(2, threads.comments.size)
        assertEquals(CommentSection.GENERAL, threads.section)
        assertEquals(2, threads.totalComments)
    }

    // ==================== Statistics Tests ====================

    @Test
    fun `countCommentsByEvent returns correct count`() {
        // Given
        repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Comment 1")
        )
        repository.createComment(
            eventId = "event-1",
            authorId = "user-2",
            authorName = "User 2",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Comment 2")
        )
        repository.createComment(
            eventId = "event-2",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Comment 3")
        )

        // When
        val count = repository.countCommentsByEvent("event-1")

        // Then
        assertEquals(2, count)
    }

    @Test
    fun `commentExists returns true for existing comment`() {
        // Given
        val comment = repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.GENERAL, content = "Test")
        )

        // When
        val exists = repository.commentExists(comment.id)
        val notExists = repository.commentExists("non-existent-id")

        // Then
        assertTrue(exists)
        assertFalse(notExists)
    }

    // ==================== Cache Tests ====================

    @Test
    fun `invalidateEventCache clears cache for event`() {
        // Given - First call should cache
        repository.getCommentsByEventCached("event-1")

        // When
        repository.invalidateEventCache("event-1")

        // Then - No exception, cache is cleared
    }

    @Test
    fun `getCacheStats returns valid statistics`() {
        // Given
        val stats = repository.getCacheStats()

        // Then
        assertNotNull(stats)
        assertTrue(stats.totalEntries >= 0)
        assertTrue(stats.expiredEntries >= 0)
        assertTrue(stats.averageAgeSeconds >= 0)
    }

    // ==================== Section-Specific Tests ====================

    @Test
    fun `getCommentsBySection filters by section correctly`() {
        // Given
        repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.GENERAL, content = "General")
        )
        repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.SCENARIO, content = "Scenario")
        )

        // When
        val generalComments = repository.getCommentsBySection("event-1", CommentSection.GENERAL)

        // Then
        assertEquals(1, generalComments.size)
        assertEquals(CommentSection.GENERAL, generalComments[0].section)
    }

    @Test
    fun `getCommentsBySection with sectionItemId filters correctly`() {
        // Given
        val scenarioId = "scenario-1"
        repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.SCENARIO, sectionItemId = scenarioId, content = "Comment 1")
        )
        repository.createComment(
            eventId = "event-1",
            authorId = "user-1",
            authorName = "User 1",
            request = CommentRequest(section = CommentSection.SCENARIO, sectionItemId = "other-scenario", content = "Comment 2")
        )

        // When
        val comments = repository.getCommentsBySection("event-1", CommentSection.SCENARIO, scenarioId)

        // Then
        assertEquals(1, comments.size)
        assertEquals(scenarioId, comments[0].sectionItemId)
    }
}
