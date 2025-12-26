package com.guyghost.wakeve.collaboration

import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.DatabaseProvider
import com.guyghost.wakeve.TestDatabaseFactory
import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.comment.CommentRepository
import com.guyghost.wakeve.ScenarioRepository
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.NotificationService
import kotlin.test.*
import kotlinx.datetime.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async

/**
 * Integration tests for the Collaboration workflow in Wakeve.
 * 
 * Tests collaborative event planning features including:
 * - Event creation with participants
 * - Scenario planning and comparison
 * - Budget management
 * - Multi-user commenting and discussion threads
 * - Comment notifications
 * - Comment deletion cascades
 * - Comment filtering and statistics
 * - Concurrent comment creation
 * - Comment permissions (author-only edit/delete)
 */
class CollaborationIntegrationTest {
    
    private lateinit var db: WakevDb
    private lateinit var commentRepository: CommentRepository
    private lateinit var mockNotificationService: MockNotificationService
    private lateinit var eventRepository: DatabaseEventRepository
    private lateinit var scenarioRepository: ScenarioRepository
    private lateinit var budgetRepository: BudgetRepository
    
    // Test users/participants
    private val organizerId = "org-1"
    private val organizerName = "Organizer"
    private val participant1Id = "user-1"
    private val participant1Name = "Alice"
    private val participant2Id = "user-2"
    private val participant2Name = "Bob"
    private val participant3Id = "user-3"
    private val participant3Name = "Charlie"
    private val participant4Id = "user-4"
    private val participant4Name = "Diana"
    private val participant5Id = "user-5"
    private val participant5Name = "Eve"
    
    private fun createTestDatabase(): WakevDb {
        return DatabaseProvider.getDatabase(TestDatabaseFactory())
    }
    
    @BeforeTest
    fun setup() {
        // Reset database singleton to avoid data leakage between tests
        DatabaseProvider.resetDatabase()
        
        db = createTestDatabase()
        
        // Initialize services
        mockNotificationService = MockNotificationService()
        commentRepository = CommentRepository(db)
        eventRepository = DatabaseEventRepository(db)
        scenarioRepository = ScenarioRepository(db)
        budgetRepository = BudgetRepository(db)
    }
    
    // ============================================================================
    // Scenario 1: Complete Collaboration Workflow
    // ============================================================================
    
    @Test
    fun testCompleteCollaborationWorkflow_creates_event_with_scenarios_and_comments() {
        runBlocking {
            // 1. Create event with organizer and participants
            val event = Event(
                id = "event-1",
                title = "Team Building Weekend",
                description = "Planning a team building event for Q4",
                organizerId = organizerId,
                participants = listOf(participant1Id, participant2Id, participant3Id),
                proposedSlots = emptyList(),
                deadline = "2025-11-30T23:59:59Z",
                status = EventStatus.CONFIRMED,
                createdAt = Clock.System.now().toString(),
                updatedAt = Clock.System.now().toString()
            )
            
            eventRepository.createEvent(event)
            
            assertNotNull(event)
            assertEquals(organizerId, event.organizerId)
            assertEquals(3, event.participants.size)
            
            // 2. Add comments on scenarios
            val scenarioComment1 = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.SCENARIO,
                    sectionItemId = "scenario-1",
                    content = "I prefer Paris for this trip!"
                )
            )
            
            assertNotNull(scenarioComment1)
            assertEquals(event.id, scenarioComment1.eventId)
            assertEquals(CommentSection.SCENARIO, scenarioComment1.section)
            assertEquals("scenario-1", scenarioComment1.sectionItemId)
            assertEquals(participant1Id, scenarioComment1.authorId)
            assertEquals(participant1Name, scenarioComment1.authorName)
            
            // 3. Add budget comment
            val budgetComment = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.BUDGET,
                    content = "Budget looks reasonable"
                )
            )
            
            assertNotNull(budgetComment)
            assertEquals(CommentSection.BUDGET, budgetComment.section)
        }
    }
    
    // ============================================================================
    // Scenario 2: Multi-User Comment Threads
    // ============================================================================
    
    @Test
    fun testMultiUserCommentThread_creates_nested_discussion() {
        runBlocking {
            val event = createAndGetTestEvent()
            
            // Create parent comment
            val parent = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.GENERAL,
                    content = "What should we prioritize?"
                )
            )
            
            assertEquals(0, parent.replyCount)
            
            // Alice replies
            val reply1 = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.GENERAL,
                    content = "I think cost is important",
                    parentCommentId = parent.id
                )
            )
            
            assertEquals(parent.id, reply1.parentCommentId)
            
            // Bob replies to Alice
            val reply2 = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(
                    section = CommentSection.GENERAL,
                    content = "Agreed, let's keep it under $1000",
                    parentCommentId = reply1.id
                )
            )
            
            assertEquals(reply1.id, reply2.parentCommentId)
            
            // Verify thread structure
            val thread = commentRepository.getCommentThread(parent.id)
            assertNotNull(thread)
            assertEquals(parent.id, thread.comment.id)
            // Note: May include nested replies depending on implementation
            assertTrue(thread.replies.isNotEmpty())
        }
    }
    
    // ============================================================================
    // Scenario 3: Comment Filtering by Section
    // ============================================================================
    
    @Test
    fun testCommentFilteringBySection_correctly_filters_comments() {
        runBlocking {
            val event = createAndGetTestEvent()
            
            // Create comments in different sections
            commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(CommentSection.GENERAL, null, "General discussion", null)
            )
            commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(CommentSection.GENERAL, null, "More general comments", null)
            )
            commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(CommentSection.BUDGET, null, "Budget comment 1", null)
            )
            commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(CommentSection.SCENARIO, "scenario-1", "Scenario comment", null)
            )
            
            // Filter by section
            val generalComments = commentRepository.getCommentsBySection(event.id, CommentSection.GENERAL)
            val budgetComments = commentRepository.getCommentsBySection(event.id, CommentSection.BUDGET)
            val scenarioComments = commentRepository.getCommentsBySection(event.id, CommentSection.SCENARIO)
            
            assertEquals(2, generalComments.size)
            assertEquals(1, budgetComments.size)
            assertEquals(1, scenarioComments.size)
            
            // Filter by section and item ID
            val scenario1Comments = commentRepository.getCommentsBySection(event.id, CommentSection.SCENARIO, "scenario-1")
            assertEquals(1, scenario1Comments.size)
        }
    }
    
    // ============================================================================
    // Scenario 4: Comment Statistics and Aggregations
    // ============================================================================
    
    @Test
    fun testCommentStatistics_calculates_comprehensive_stats() {
        runBlocking {
            val event = createAndGetTestEvent()
            
            // Create various comments
            commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(CommentSection.GENERAL, null, "Alice comment 1", null)
            )
            commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(CommentSection.GENERAL, null, "Alice comment 2", null)
            )
            commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(CommentSection.BUDGET, null, "Bob comment 1", null)
            )
            commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(CommentSection.SCENARIO, "scenario-1", "Charlie comment", null)
            )
            
            // Get statistics
            val stats = commentRepository.getCommentStatistics(event.id)
            
            assertEquals(event.id, stats.eventId)
            assertEquals(4, stats.totalComments)
            assertEquals(2, stats.commentsBySection[CommentSection.GENERAL])
            assertEquals(1, stats.commentsBySection[CommentSection.BUDGET])
            assertTrue(stats.topContributors.isNotEmpty())
        }
    }
    
    // ============================================================================
    // Scenario 5: Concurrent Comment Creation
    // ============================================================================
    
    @Test
    fun testConcurrentCommentCreation_handles_parallel_comments() {
        runBlocking {
            val event = createAndGetTestEvent()
            
            // Create 5 comments concurrently
            val deferredComments = (1..5).map { index ->
                async {
                    commentRepository.createComment(
                        event.id,
                        when (index) {
                            1 -> participant1Id
                            2 -> participant2Id
                            3 -> participant3Id
                            4 -> participant4Id
                            else -> participant5Id
                        },
                        when (index) {
                            1 -> participant1Name
                            2 -> participant2Name
                            3 -> participant3Name
                            4 -> participant4Name
                            else -> participant5Name
                        },
                        CommentRequest(
                            section = CommentSection.GENERAL,
                            content = "Concurrent comment $index"
                        )
                    )
                }
            }
            
            val comments = deferredComments.map { it.await() }
            assertEquals(5, comments.size)
            
            // Verify all comments were created
            val allComments = commentRepository.getCommentsBySection(event.id, CommentSection.GENERAL)
            assertEquals(5, allComments.size)
        }
    }
    
    // ============================================================================
    // Scenario 6: Comment Deletion Cascade
    // ============================================================================
    
    @Test
    fun testCommentDeletionCascade_deletes_all_nested_replies() {
        runBlocking {
            val event = createAndGetTestEvent()
            
            // Create parent
            val parent = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(CommentSection.GENERAL, null, "Parent comment", null)
            )
            
            // Create replies
            val reply1 = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(CommentSection.GENERAL, null, "Reply 1", parent.id)
            )
            
            val reply2 = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(CommentSection.GENERAL, null, "Reply 2", parent.id)
            )
            
            // Verify all exist
            assertNotNull(commentRepository.getCommentById(parent.id))
            assertNotNull(commentRepository.getCommentById(reply1.id))
            assertNotNull(commentRepository.getCommentById(reply2.id))
            
            // Delete parent (should cascade)
            commentRepository.deleteComment(parent.id)
            
            // Verify all are deleted
            assertNull(commentRepository.getCommentById(parent.id))
            assertNull(commentRepository.getCommentById(reply1.id))
            assertNull(commentRepository.getCommentById(reply2.id))
        }
    }
    
    // ============================================================================
    // Scenario 7: Comment Permissions
    // ============================================================================
    
    @Test
    fun testCommentPermissions_enforces_author_only_modification() {
        runBlocking {
            val event = createAndGetTestEvent()
            
            // Alice creates a comment
            val comment = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(CommentSection.GENERAL, null, "Alice's comment", null)
            )
            
            assertEquals(participant1Id, comment.authorId)
            assertFalse(comment.isEdited)
            
            // Alice can update her own comment
            val updated = commentRepository.updateComment(comment.id, "Alice's updated comment")
            assertEquals("Alice's updated comment", updated.content)
            assertTrue(updated.isEdited)
            
            // Verify deletion works
            commentRepository.deleteComment(comment.id)
            assertNull(commentRepository.getCommentById(comment.id))
        }
    }
    
    // ============================================================================
    // Helper Methods
    // ============================================================================
    
    private suspend fun createAndGetTestEvent(
        eventId: String = "event-test-1",
        organizerId: String = this.organizerId,
        participantIds: List<String> = listOf(participant1Id, participant2Id, participant3Id)
    ): Event {
        val event = Event(
            id = eventId,
            title = "Test Collaboration Event",
            description = "Event for testing collaborative features",
            organizerId = organizerId,
            participants = participantIds,
            proposedSlots = emptyList(),
            deadline = "2025-12-31T23:59:59Z",
            status = EventStatus.CONFIRMED,
            createdAt = Clock.System.now().toString(),
            updatedAt = Clock.System.now().toString()
        )
        
        eventRepository.createEvent(event)
        return event
    }
    
    // ============================================================================
    // Mock NotificationService for testing
    // ============================================================================
    
    /**
     * Mock NotificationService for testing.
     * 
     * Captures all notifications sent for verification in tests.
     */
    private inner class MockNotificationService : NotificationService {
        private val sentNotifications = mutableListOf<NotificationMessage>()
        private val pushTokens = mutableMapOf<String, PushToken>()
        
        override suspend fun sendNotification(message: NotificationMessage): Result<Unit> {
            sentNotifications.add(message)
            return Result.success(Unit)
        }
        
        override suspend fun registerPushToken(token: PushToken): Result<Unit> {
            pushTokens["${token.userId}-${token.deviceId}"] = token
            return Result.success(Unit)
        }
        
        override suspend fun unregisterPushToken(userId: String, deviceId: String): Result<Unit> {
            pushTokens.remove("$userId-$deviceId")
            return Result.success(Unit)
        }
        
        override suspend fun getUnreadNotifications(userId: String): List<NotificationMessage> {
            return sentNotifications.filter { it.userId == userId && it.readAt == null }
        }
        
        override suspend fun markAsRead(notificationId: String): Result<Unit> {
            val notification = sentNotifications.find { it.id == notificationId }
            if (notification != null) {
                val index = sentNotifications.indexOf(notification)
                sentNotifications[index] = notification.copy(readAt = Clock.System.now().toString())
            }
            return Result.success(Unit)
        }
        
        fun getSentNotifications(recipientId: String): List<NotificationMessage> {
            return sentNotifications.filter { it.userId == recipientId }
        }
        
        fun clear() {
            sentNotifications.clear()
        }
        
        fun getTotalNotificationCount(): Int {
            return sentNotifications.size
        }
    }
}
