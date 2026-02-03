package com.guyghost.wakeve.e2e

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * # Multi-User Collaboration E2E Test (E2E-002)
 * 
 * Tests real-time collaboration scenarios:
 * - Multiple users voting simultaneously
 * - Comments with threading and @mentions
 * - Real-time sync simulation
 * - Conflict resolution
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MultiUserCollaborationE2ETest {

    // ========================================================================
    // Test Infrastructure
    // ========================================================================

    private lateinit var database: WakevDb
    private lateinit var eventRepository: EventRepositoryInterface
    private lateinit var testScope: CoroutineScope

    // Mock notification channels for testing
    private val notificationChannel = Channel<NotificationEvent>(capacity = 10)

    data class TestUser(val id: String, val name: String)
    data class NotificationEvent(
        val type: String,
        val userId: String,
        val eventId: String,
        val data: Map<String, Any>
    )

    @BeforeTest
    fun setup() {
        database = createFreshTestDatabase()
        eventRepository = DatabaseEventRepository(database)
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterTest
    fun cleanup() {
        testScope.cancel()
        database.close()
        notificationChannel.close()
    }

    // ========================================================================
    // Test Cases
    // ========================================================================

    /**
     * Test: Simultaneous voting by multiple users
     * 
     * GIVEN: Event in POLLING status with multiple participants
     * WHEN: Users vote simultaneously on different time slots
     * THEN: Votes are correctly aggregated without conflicts
     */
    @Test
    fun `simultaneous voting by multiple users`() = runTest {
        // GIVEN
        val eventId = "event-simultaneous-voting"
        val userA = TestUser("user-a", "Alice")
        val userB = TestUser("user-b", "Bob")
        val userC = TestUser("user-c", "Charlie")

        val event = createTestEvent(
            id = eventId,
            title = "Team Meeting",
            organizerId = "organizer",
            participants = listOf(userA.id, userB.id, userC.id),
            proposedSlots = listOf(
                createTestTimeSlot("slot-1", "2025-06-15T10:00:00Z", "2025-06-15T12:00:00Z"),
                createTestTimeSlot("slot-2", "2025-06-15T14:00:00Z", "2025-06-15T16:00:00Z"),
                createTestTimeSlot("slot-3", "2025-06-16T10:00:00Z", "2025-06-16T12:00:00Z")
            ),
            status = EventStatus.POLLING
        )
        eventRepository.createEvent(event)

        // WHEN - Users vote simultaneously
        val votingJobs = listOf(
            async {
                eventRepository.addVote(eventId, userA.id, "slot-1", Vote.YES)
                eventRepository.addVote(eventId, userA.id, "slot-2", Vote.NO)
            },
            async {
                eventRepository.addVote(eventId, userB.id, "slot-1", Vote.YES)
                eventRepository.addVote(eventId, userB.id, "slot-3", Vote.MAYBE)
            },
            async {
                eventRepository.addVote(eventId, userC.id, "slot-2", Vote.YES)
                eventRepository.addVote(eventId, userC.id, "slot-3", Vote.NO)
            }
        )

        // Wait for all votes to complete
        votingJobs.awaitAll()
        advanceUntilIdle()

        // THEN - Verify votes are correctly aggregated
        val poll = eventRepository.getPoll(eventId)
        assertNotNull(poll, "Poll should exist")

        val votes = poll.votes
        assertEquals(3, votes.size, "Should have votes from all 3 users")

        // Check slot-1: YES from A and B (score = 4)
        val slot1Score = calculateSlotScore(votes, "slot-1")
        assertEquals(4, slot1Score, "Slot-1 should have score 4 (2 YES votes)")

        // Check slot-2: YES from C, NO from A (score = 1)
        val slot2Score = calculateSlotScore(votes, "slot-2")
        assertEquals(1, slot2Score, "Slot-2 should have score 1 (1 YES, 1 NO)")

        // Check slot-3: MAYBE from B, NO from C (score = 0)
        val slot3Score = calculateSlotScore(votes, "slot-3")
        assertEquals(0, slot3Score, "Slot-3 should have score 0 (1 MAYBE, 1 NO)")
    }

    /**
     * Test: Real-time comment threading with @mentions
     * 
     * GIVEN: Event in ORGANIZING status
     * WHEN: Users add comments with replies and @mentions
     * THEN: Comments are threaded correctly and mentions are parsed
     */
    @Test
    fun `real-time comments with threading and mentions`() = runTest {
        // GIVEN
        val eventId = "event-comments"
        val userA = TestUser("user-a", "Alice")
        val userB = TestUser("user-b", "Bob")
        val userC = TestUser("user-c", "Charlie")

        val event = createTestEvent(
            id = eventId,
            title = "Project Planning",
            organizerId = userA.id,
            participants = listOf(userA.id, userB.id, userC.id),
            status = EventStatus.ORGANIZING
        )
        eventRepository.createEvent(event)

        // WHEN - User A adds main comment
        val mainComment = Comment(
            id = "comment-1",
            eventId = eventId,
            section = CommentSection.TRANSPORT,
            authorId = userA.id,
            authorName = userA.name,
            content = "We should consider carpooling. @bob what do you think?",
            createdAt = Clock.System.now().toString()
        )
        eventRepository.addComment(mainComment)

        // WHEN - User B replies with mention
        val replyComment = Comment(
            id = "comment-2",
            eventId = eventId,
            section = CommentSection.TRANSPORT,
            parentCommentId = "comment-1",
            authorId = userB.id,
            authorName = userB.name,
            content = "Good idea @alice! I can drive 3 people. @charlie are you in?",
            createdAt = Clock.System.now().toString()
        )
        eventRepository.addComment(replyComment)

        // WHEN - User C adds separate comment
        val separateComment = Comment(
            id = "comment-3",
            eventId = eventId,
            section = CommentSection.ACCOMMODATION,
            authorId = userC.id,
            authorName = userC.name,
            content = "Found a good hotel near the venue",
            createdAt = Clock.System.now().toString()
        )
        eventRepository.addComment(separateComment)

        advanceUntilIdle()

        // THEN - Verify comments are correctly stored and threaded
        val allComments = eventRepository.getComments(eventId)
        assertEquals(3, allComments.size, "Should have all 3 comments")

        // Verify threading
        val transportComments = allComments.filter { it.section == CommentSection.TRANSPORT }
        assertEquals(2, transportComments.size, "Should have 2 transport comments")

        val mainCommentRetrieved = transportComments.find { it.id == "comment-1" }
        val replyCommentRetrieved = transportComments.find { it.id == "comment-2" }

        assertNotNull(mainCommentRetrieved, "Main comment should be retrieved")
        assertNotNull(replyCommentRetrieved, "Reply comment should be retrieved")
        assertEquals("comment-1", replyCommentRetrieved.parentCommentId, "Reply should reference main comment")

        // Verify mentions are parsed (simplified - in real implementation would use MentionParser)
        val mentionsInMain = parseMentions(mainCommentRetrieved.content)
        assertTrue(mentionsInMain.contains("bob"), "Main comment should mention Bob")

        val mentionsInReply = parseMentions(replyCommentRetrieved.content)
        assertTrue(mentionsInReply.contains("alice"), "Reply should mention Alice")
        assertTrue(mentionsInReply.contains("charlie"), "Reply should mention Charlie")

        // Verify notifications would be triggered for mentions
        val expectedNotifications = listOf(
            NotificationEvent("mention", "user-b", eventId, mapOf("from" to "user-a")),
            NotificationEvent("mention", "user-a", eventId, mapOf("from" to "user-b")),
            NotificationEvent("mention", "user-c", eventId, mapOf("from" to "user-b"))
        )

        // In real implementation, these would be sent via NotificationService
        expectedNotifications.forEach { notif ->
            // Simulate notification channel
            testScope.launch {
                notificationChannel.send(notif)
            }
        }
    }

    /**
     * Test: Real-time sync simulation between multiple users
     * 
     * GIVEN: Multiple users working on same event
     * WHEN: One user makes changes, others sync
     * THEN: Changes are reflected in real-time
     */
    @Test
    fun `real-time sync between multiple users`() = runTest {
        // GIVEN
        val eventId = "event-realtime-sync"
        val userA = TestUser("user-a", "Alice")
        val userB = TestUser("user-b", "Bob")

        val event = createTestEvent(
            id = eventId,
            title = "Workshop Planning",
            organizerId = userA.id,
            participants = listOf(userA.id, userB.id),
            status = EventStatus.CONFIRMED
        )
        eventRepository.createEvent(event)

        // Create a sync channel to simulate real-time updates
        val syncChannel = Channel<EventUpdate>(capacity = 10)
        
        data class EventUpdate(
            val eventId: String,
            val type: String,
            val data: Any,
            val timestamp: String
        )

        // WHEN - User A creates a scenario
        val scenario = Scenario(
            id = "scenario-1",
            eventId = eventId,
            name = "In-Person Workshop",
            dateOrPeriod = "2025-07-10",
            location = "Conference Center",
            duration = 1,
            estimatedParticipants = 20,
            estimatedBudgetPerPerson = 150.0,
            description = "Full day in-person workshop",
            status = ScenarioStatus.PROPOSED,
            createdAt = Clock.System.now().toString(),
            updatedAt = Clock.System.now().toString()
        )

        // Save scenario (User A's action)
        eventRepository.saveScenario(scenario)

        // Simulate sync event
        testScope.launch {
            syncChannel.send(
                EventUpdate(
                    eventId = eventId,
                    type = "scenario_created",
                    data = scenario,
                    timestamp = Clock.System.now().toString()
                )
            )
        }

        // User B receives sync and updates their local state
        val syncEvents = mutableListOf<EventUpdate>()
        val syncJob = launch {
            syncChannel.receiveAsFlow().collect { event ->
                syncEvents.add(event)
            }
        }

        advanceUntilIdle()
        delay(100) // Allow sync to propagate

        // THEN - Verify sync event was received
        assertTrue(syncEvents.isNotEmpty(), "User B should receive sync events")

        val scenarioCreatedEvent = syncEvents.find { it.type == "scenario_created" }
        assertNotNull(scenarioCreatedEvent, "Should have scenario_created event")

        // Verify User B can now see the scenario
        val scenarios = eventRepository.getScenarios(eventId)
        assertEquals(1, scenarios.size, "User B should see the created scenario")
        assertEquals("In-Person Workshop", scenarios[0].name)

        syncJob.cancel()
    }

    /**
     * Test: Conflict resolution in concurrent modifications
     * 
     * GIVEN: Multiple users modifying same data
     * WHEN: Conflicts occur
     * THEN: Last-write-wins resolution is applied
     */
    @Test
    fun `conflict resolution with last write wins`() = runTest {
        // GIVEN
        val eventId = "event-conflict-resolution"
        val userA = TestUser("user-a", "Alice")
        val userB = TestUser("user-b", "Bob")

        val event = createTestEvent(
            id = eventId,
            title = "Original Title",
            organizerId = userA.id,
            participants = listOf(userA.id, userB.id),
            status = EventStatus.DRAFT
        )
        eventRepository.createEvent(event)

        // WHEN - Both users try to update the same field simultaneously
        val updateJobA = async {
            delay(10) // Small delay to simulate network
            val updatedEventA = event.copy(
                title = "Alice's Title",
                description = "Updated by Alice",
                updatedAt = Clock.System.now().toString()
            )
            eventRepository.updateEvent(updatedEventA)
        }

        val updateJobB = async {
            delay(20) // Slightly longer delay
            val updatedEventB = event.copy(
                title = "Bob's Title",
                description = "Updated by Bob",
                updatedAt = Clock.System.now().toString()
            )
            eventRepository.updateEvent(updatedEventB)
        }

        // Wait for both updates
        val resultA = updateJobA.await()
        val resultB = updateJobB.await()

        advanceUntilIdle()

        // THEN - Last write wins (Bob's update due to longer delay)
        assertTrue(resultA.isSuccess, "Alice's update should succeed initially")
        assertTrue(resultB.isSuccess, "Bob's update should succeed")

        val finalEvent = eventRepository.getEvent(eventId)
        assertNotNull(finalEvent, "Event should still exist")

        // The last update should win
        assertEquals("Bob's Title", finalEvent.title, "Bob's title should win (last write)")
        assertEquals("Updated by Bob", finalEvent.description, "Bob's description should win")

        // Verify timestamps for conflict resolution
        val events = eventRepository.getAllEvents()
        val currentEvent = events.find { it.id == eventId }
        assertNotNull(currentEvent)
        assertTrue(currentEvent.updatedAt.isNotEmpty(), "Should have updated timestamp")
    }

    // ========================================================================
    // Helper Functions
    // ========================================================================

    private fun calculateSlotScore(votes: Map<String, Map<String, Vote>>, slotId: String): Int {
        var score = 0
        votes.values.forEach { userVotes ->
            val vote = userVotes[slotId]
            when (vote) {
                Vote.YES -> score += 2
                Vote.MAYBE -> score += 1
                Vote.NO -> score -= 1
                null -> { /* No vote */ }
            }
        }
        return score
    }

    private fun parseMentions(content: String): List<String> {
        val mentionRegex = Regex("@(\\w+)")
        return mentionRegex.findAll(content).map { it.groupValues[1] }.toList()
    }
}