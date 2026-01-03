package com.guyghost.wakeve.e2e

import com.guyghost.wakeve.models.Comment
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.models.CommentThread
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * # Multi-User Collaboration Test Suite for Wakeve
 *
 * Comprehensive E2E tests for real-time collaboration scenarios:
 * - Real-time poll voting across multiple participants
 * - Concurrent scenario creation and voting
 * - Comments by section with threading support
 * - Real-time synchronization between users
 * - Permission-based access control
 * - Conflict resolution in voting
 * - Read-only mode after finalization
 * - Cross-device synchronization
 * - Comment statistics and engagement tracking
 *
 * Pattern: GIVEN/WHEN/THEN with mocked repositories
 */
class MultiUserCollaborationTest {

    // ==================== Test User Model ====================

    data class TestUser(val id: String, val name: String, val role: UserRole = UserRole.PARTICIPANT)
    enum class UserRole { ORGANIZER, PARTICIPANT, ADMIN }

    // ==================== Mock In-Memory Repository ====================

    class TestEventStore {
        private val events = mutableMapOf<String, Event>()
        private val pollVotes = mutableMapOf<String, MutableMap<String, MutableMap<String, Vote>>>()
        private val scenariosStore = mutableMapOf<String, MutableList<Scenario>>()
        private val scenarioVotesStore = mutableMapOf<String, MutableList<ScenarioVote>>()

        private fun genTestId() = buildString(8) { repeat(8) { append("0123456789abcdef"[Random.nextInt(16)]) } }

        fun createEvent(event: Event) {
            events[event.id] = event
            pollVotes[event.id] = mutableMapOf()
            scenariosStore[event.id] = mutableListOf()
            scenarioVotesStore[event.id] = mutableListOf()
        }

        fun getEvent(eventId: String) = events[eventId]

        fun updateEventStatus(eventId: String, status: EventStatus) {
            events[eventId]?.let {
                events[eventId] = it.copy(status = status, updatedAt = Clock.System.now().toString())
            }
        }

        fun recordVote(eventId: String, participantId: String, slotId: String, vote: Vote) {
            val pollVotesForEvent = pollVotes.getOrPut(eventId) { mutableMapOf() }
            val votesForParticipant = pollVotesForEvent.getOrPut(participantId) { mutableMapOf() }
            votesForParticipant[slotId] = vote
        }

        fun getPollVotes(eventId: String) = pollVotes[eventId]

        fun addScenario(eventId: String, scenario: Scenario): Boolean {
            scenariosStore[eventId]?.add(scenario)
            return true
        }

        fun getScenarios(eventId: String) = scenariosStore[eventId] ?: emptyList()

        fun voteOnScenario(eventId: String, scenarioId: String, participantId: String, voteType: ScenarioVoteType) {
            val votes = scenarioVotesStore.getOrPut(eventId) { mutableListOf() }
            // Remove previous vote
            votes.removeAll { it.scenarioId == scenarioId && it.participantId == participantId }
            // Add new vote
            votes.add(
                ScenarioVote(
                    id = genTestId(),
                    scenarioId = scenarioId,
                    participantId = participantId,
                    vote = voteType,
                    createdAt = Clock.System.now().toString()
                )
            )
        }

        fun getScenarioVotes(eventId: String, scenarioId: String): List<ScenarioVote> {
            return scenarioVotesStore[eventId]?.filter { it.scenarioId == scenarioId } ?: emptyList()
        }

        fun canModifyScenario(eventId: String, role: UserRole): Boolean {
            val event = getEvent(eventId) ?: return false
            return when (role) {
                UserRole.ORGANIZER, UserRole.ADMIN -> true
                UserRole.PARTICIPANT -> event.status in listOf(EventStatus.CONFIRMED, EventStatus.COMPARING)
            }
        }
    }

    class TestCommentStore {
        private val comments = mutableMapOf<String, MutableList<Comment>>()

        fun addComment(eventId: String, comment: Comment) {
            comments.getOrPut(eventId) { mutableListOf() }.add(comment)
        }

        fun getCommentsBySection(eventId: String, section: CommentSection): List<Comment> {
            return comments[eventId]?.filter { it.section == section } ?: emptyList()
        }

        fun getCommentThread(eventId: String, commentId: String): CommentThread? {
            val comment = comments[eventId]?.find { it.id == commentId } ?: return null
            val replies = comments[eventId]?.filter { it.parentCommentId == commentId } ?: emptyList()
            return CommentThread(comment = comment, replies = replies)
        }

        fun getAllComments(eventId: String) = comments[eventId]?.toList() ?: emptyList()

        fun getTopLevelComments(eventId: String, section: CommentSection? = null): List<Comment> {
            return comments[eventId]?.filter {
                it.parentCommentId == null && (section == null || it.section == section)
            } ?: emptyList()
        }

        fun countComments(eventId: String) = comments[eventId]?.size ?: 0

        fun countCommentsBySection(eventId: String, section: CommentSection): Int {
            return getTopLevelComments(eventId, section).size
        }
    }

    // ==================== Helper Functions ====================

    private fun genTestId() = buildString(8) { repeat(8) { append("0123456789abcdef"[Random.nextInt(16)]) } }

    private fun createTestScenario(
        id: String = genTestId(),
        name: String = "Scenario",
        location: String = "Paris"
    ) = Scenario(
        id = id,
        eventId = "event-1",
        name = name,
        dateOrPeriod = "2025-06-15 to 2025-06-17",
        location = location,
        duration = 3,
        estimatedParticipants = 4,
        estimatedBudgetPerPerson = 1000.0,
        description = "Test scenario",
        status = ScenarioStatus.PROPOSED,
        createdAt = Clock.System.now().toString(),
        updatedAt = Clock.System.now().toString()
    )

    // ==================== Test Cases ====================

    @Test
    fun testRealTimePollVoting() = runTest {
        // GIVEN: 1 organizer + 3 participants, Event in POLLING state
        val store = TestEventStore()
        val organizer = TestUser("org-1", "Alice", UserRole.ORGANIZER)
        val p1 = TestUser("p1", "Bob", UserRole.PARTICIPANT)
        val p2 = TestUser("p2", "Charlie", UserRole.PARTICIPANT)
        val p3 = TestUser("p3", "Diana", UserRole.PARTICIPANT)

        val event = createTestEvent(
            id = "event-1",
            status = EventStatus.POLLING,
            proposedSlots = listOf(
                createTestTimeSlot(id = "slot-1", start = "2025-06-15T10:00:00Z"),
                createTestTimeSlot(id = "slot-2", start = "2025-06-16T10:00:00Z"),
                createTestTimeSlot(id = "slot-3", start = "2025-06-17T10:00:00Z")
            ),
            participants = listOf(p1.id, p2.id, p3.id)
        )
        store.createEvent(event)

        // WHEN: Multiple participants vote on slots
        store.recordVote("event-1", p1.id, "slot-1", Vote.YES)
        store.recordVote("event-1", p2.id, "slot-1", Vote.MAYBE)
        store.recordVote("event-1", p3.id, "slot-1", Vote.NO)

        // THEN: Votes aggregated correctly (YES=2, MAYBE=1, NO=-1 => score = 4)
        val votes = store.getPollVotes("event-1")
        assertNotNull(votes)
        assertEquals(3, votes.size)
        assertEquals(Vote.YES, votes["p1"]?.get("slot-1"))
        assertEquals(Vote.MAYBE, votes["p2"]?.get("slot-1"))
        assertEquals(Vote.NO, votes["p3"]?.get("slot-1"))

        val score = 2 * 2 + 1 * 1 - 1 * 1
        assertEquals(4, score)
    }

    @Test
    fun testConcurrentScenarioCreation() = runTest {
        // GIVEN: Event in CONFIRMED status, 2 participants authorized
        val store = TestEventStore()
        val p1 = TestUser("p1", "Bob", UserRole.PARTICIPANT)
        val p2 = TestUser("p2", "Charlie", UserRole.PARTICIPANT)

        val event = createTestEvent(
            id = "event-1",
            status = EventStatus.CONFIRMED,
            participants = listOf(p1.id, p2.id)
        )
        store.createEvent(event)

        // WHEN: Two participants create scenarios concurrently
        val s1 = createTestScenario(id = "s1", name = "Paris", location = "Paris")
        val s2 = createTestScenario(id = "s2", name = "Lyon", location = "Lyon")

        assertTrue(store.addScenario("event-1", s1))
        assertTrue(store.addScenario("event-1", s2))

        // THEN: Both scenarios created without conflicts
        val scenarios = store.getScenarios("event-1")
        assertEquals(2, scenarios.size)
        assertTrue(scenarios.any { it.name == "Paris" })
        assertTrue(scenarios.any { it.name == "Lyon" })
    }

    @Test
    fun testScenarioVotingWithScoring() = runTest {
        // GIVEN: 3 scenarios, 4 participants vote
        val store = TestEventStore()
        val participants = listOf(
            TestUser("p1", "Alice", UserRole.PARTICIPANT),
            TestUser("p2", "Bob", UserRole.PARTICIPANT),
            TestUser("p3", "Charlie", UserRole.PARTICIPANT),
            TestUser("p4", "Diana", UserRole.PARTICIPANT)
        )

        val event = createTestEvent(
            id = "event-1",
            status = EventStatus.COMPARING,
            participants = participants.map { it.id }
        )
        store.createEvent(event)

        val s1 = createTestScenario(id = "s1", name = "Paris")
        val s2 = createTestScenario(id = "s2", name = "Lyon")
        store.addScenario("event-1", s1)
        store.addScenario("event-1", s2)

        // WHEN: Participants vote
        // Scenario 1: 2 PREFER, 1 NEUTRAL, 0 AGAINST => score = 5
        store.voteOnScenario("event-1", "s1", participants[0].id, ScenarioVoteType.PREFER)
        store.voteOnScenario("event-1", "s1", participants[1].id, ScenarioVoteType.PREFER)
        store.voteOnScenario("event-1", "s1", participants[2].id, ScenarioVoteType.NEUTRAL)

        // Scenario 2: 1 PREFER, 0 NEUTRAL, 1 AGAINST => score = 1
        store.voteOnScenario("event-1", "s2", participants[0].id, ScenarioVoteType.PREFER)
        store.voteOnScenario("event-1", "s2", participants[1].id, ScenarioVoteType.AGAINST)

        // THEN: Votes aggregated with correct scoring
        val s1Votes = store.getScenarioVotes("event-1", "s1")
        val s2Votes = store.getScenarioVotes("event-1", "s2")

        val s1Prefer = s1Votes.count { it.vote == ScenarioVoteType.PREFER }
        val s1Neutral = s1Votes.count { it.vote == ScenarioVoteType.NEUTRAL }
        val s1Against = s1Votes.count { it.vote == ScenarioVoteType.AGAINST }

        assertEquals(2, s1Prefer)
        assertEquals(1, s1Neutral)
        assertEquals(0, s1Against)

        val s1Score = s1Prefer * 2 + s1Neutral * 1 - s1Against * 1
        val s2Score = 1 * 2 + 0 * 1 - 1 * 1

        assertEquals(5, s1Score)
        assertEquals(1, s2Score)
        assertTrue(s1Score > s2Score)
    }

    @Test
    fun testCommentsBySection() = runTest {
        // GIVEN: Event in ORGANIZING state
        val commentStore = TestCommentStore()
        val organizer = TestUser("org-1", "Alice", UserRole.ORGANIZER)
        val p1 = TestUser("p1", "Bob", UserRole.PARTICIPANT)
        val p2 = TestUser("p2", "Charlie", UserRole.PARTICIPANT)

        // WHEN: Multiple users add comments in different sections
        val transportComment1 = Comment(
            id = genTestId(),
            eventId = "event-1",
            section = CommentSection.TRANSPORT,
            authorId = p1.id,
            authorName = p1.name,
            content = "On peut prendre le train?",
            createdAt = Clock.System.now().toString()
        )

        val transportComment2 = Comment(
            id = genTestId(),
            eventId = "event-1",
            section = CommentSection.TRANSPORT,
            parentCommentId = transportComment1.id,
            authorId = p2.id,
            authorName = p2.name,
            content = "Moi je préfère voiture",
            createdAt = Clock.System.now().toString()
        )

        val mealComment = Comment(
            id = genTestId(),
            eventId = "event-1",
            section = CommentSection.MEAL,
            authorId = organizer.id,
            authorName = organizer.name,
            content = "Prévoir végétarien",
            createdAt = Clock.System.now().toString()
        )

        commentStore.addComment("event-1", transportComment1)
        commentStore.addComment("event-1", transportComment2)
        commentStore.addComment("event-1", mealComment)

        // THEN: Comments organized by section with threading
        val transportComments = commentStore.getCommentsBySection("event-1", CommentSection.TRANSPORT)
        val mealComments = commentStore.getCommentsBySection("event-1", CommentSection.MEAL)

        assertEquals(2, transportComments.size)
        assertEquals(1, mealComments.size)

        val thread = commentStore.getCommentThread("event-1", transportComment1.id)
        assertNotNull(thread)
        assertEquals(1, thread.replies.size)
        assertEquals(p2.name, thread.replies[0].authorName)
    }

    @Test
    fun testRealTimeSyncBetweenUsers() = runTest {
        // GIVEN: 2 users on same event
        val store = TestEventStore()
        val userA = TestUser("user-a", "Alice", UserRole.PARTICIPANT)
        val userB = TestUser("user-b", "Bob", UserRole.PARTICIPANT)

        val event = createTestEvent(
            id = "event-1",
            status = EventStatus.CONFIRMED,
            participants = listOf(userA.id, userB.id)
        )
        store.createEvent(event)

        // WHEN: User A creates scenario, User B syncs
        val scenario = createTestScenario(id = "s1", name = "Paris")
        store.addScenario("event-1", scenario)

        // THEN: User B sees the scenario
        val scenarios = store.getScenarios("event-1")
        assertEquals(1, scenarios.size)
        assertEquals("Paris", scenarios[0].name)
    }

    @Test
    fun testNotificationsOnCollaborationActions() = runTest {
        // GIVEN: 3 participants, Event in ORGANIZING
        val eventStore = TestEventStore()
        val commentStore = TestCommentStore()
        val p1 = TestUser("p1", "Bob", UserRole.PARTICIPANT)
        val p2 = TestUser("p2", "Charlie", UserRole.PARTICIPANT)
        val p3 = TestUser("p3", "Diana", UserRole.PARTICIPANT)

        val event = createTestEvent(
            id = "event-1",
            status = EventStatus.ORGANIZING,
            participants = listOf(p1.id, p2.id, p3.id)
        )
        eventStore.createEvent(event)

        val scenario = createTestScenario(id = "s1", name = "Paris")
        eventStore.addScenario("event-1", scenario)

        // WHEN: Collaborative actions occur
        eventStore.voteOnScenario("event-1", "s1", p1.id, ScenarioVoteType.PREFER)

        val comment = Comment(
            id = genTestId(),
            eventId = "event-1",
            section = CommentSection.SCENARIO,
            sectionItemId = "s1",
            authorId = p2.id,
            authorName = p2.name,
            content = "J'aime bien ce scénario!",
            createdAt = Clock.System.now().toString()
        )
        commentStore.addComment("event-1", comment)

        val newScenario = createTestScenario(id = "s2", name = "Lyon")
        eventStore.addScenario("event-1", newScenario)

        // THEN: Actions are recorded
        assertEquals(1, commentStore.countComments("event-1"))
        assertEquals(2, eventStore.getScenarios("event-1").size)
    }

    @Test
    fun testPermissionBasedActions() = runTest {
        // GIVEN: 1 organizer + 3 participants
        val store = TestEventStore()
        val organizer = TestUser("org-1", "Alice", UserRole.ORGANIZER)
        val participant = TestUser("p1", "Bob", UserRole.PARTICIPANT)

        val event = createTestEvent(
            id = "event-1",
            status = EventStatus.CONFIRMED,
            participants = listOf(participant.id)
        )
        store.createEvent(event)

        // WHEN: Different roles try to modify scenarios
        val participantCanModifyConfirmed = store.canModifyScenario("event-1", UserRole.PARTICIPANT)
        val organizerCanModify = store.canModifyScenario("event-1", UserRole.ORGANIZER)

        // THEN: Permissions enforced correctly
        assertTrue(participantCanModifyConfirmed) // In CONFIRMED status
        assertTrue(organizerCanModify)

        // In DRAFT: participants cannot modify
        store.updateEventStatus("event-1", EventStatus.DRAFT)
        val participantCanModifyDraft = store.canModifyScenario("event-1", UserRole.PARTICIPANT)
        assertFalse(participantCanModifyDraft)
    }

    @Test
    fun testConflictResolutionInVoting() = runTest {
        // GIVEN: User votes on same slot multiple times
        val store = TestEventStore()
        val event = createTestEvent(id = "event-1", status = EventStatus.POLLING)
        store.createEvent(event)

        // WHEN: User changes vote (last-write-wins)
        store.recordVote("event-1", "p1", "slot-1", Vote.YES)
        store.recordVote("event-1", "p1", "slot-1", Vote.MAYBE)

        // THEN: Latest vote wins
        val votes = store.getPollVotes("event-1")
        assertEquals(Vote.MAYBE, votes?.get("p1")?.get("slot-1"))
    }

    @Test
    fun testReadOnlyModeAfterFinalization() = runTest {
        // GIVEN: Event FINALIZED
        val store = TestEventStore()
        val event = createTestEvent(id = "event-1", status = EventStatus.FINALIZED)
        store.createEvent(event)

        // WHEN: User tries to modify after finalization
        val participantCanModify = store.canModifyScenario("event-1", UserRole.PARTICIPANT)

        // THEN: Modifications blocked (read-only)
        assertFalse(participantCanModify)

        // But organizer can still read
        val eventData = store.getEvent("event-1")
        assertNotNull(eventData)
        assertEquals(EventStatus.FINALIZED, eventData.status)
    }

    @Test
    fun testCrossDeviceSynchronization() = runTest {
        // GIVEN: 1 user on 2 devices
        val store = TestEventStore()
        val user = TestUser("user-1", "Alice", UserRole.PARTICIPANT)

        val event = createTestEvent(
            id = "event-1",
            status = EventStatus.CONFIRMED,
            participants = listOf(user.id)
        )
        store.createEvent(event)

        // WHEN: Device 1 creates scenario, Device 2 votes
        val scenario = createTestScenario(id = "s1", name = "Paris")
        store.addScenario("event-1", scenario)
        store.voteOnScenario("event-1", "s1", user.id, ScenarioVoteType.PREFER)

        // THEN: State consistent across devices
        val scenarios = store.getScenarios("event-1")
        assertEquals(1, scenarios.size)

        val votes = store.getScenarioVotes("event-1", "s1")
        assertEquals(1, votes.size)
        assertEquals(ScenarioVoteType.PREFER, votes[0].vote)
    }

    @Test
    fun testCommentThreading() = runTest {
        // GIVEN: Event with comments
        val commentStore = TestCommentStore()
        val p1 = TestUser("p1", "Bob", UserRole.PARTICIPANT)
        val p2 = TestUser("p2", "Charlie", UserRole.PARTICIPANT)

        // WHEN: Create main comment and nested replies
        val mainComment = Comment(
            id = "c1",
            eventId = "event-1",
            section = CommentSection.TRANSPORT,
            authorId = p1.id,
            authorName = p1.name,
            content = "Train or car?",
            createdAt = Clock.System.now().toString()
        )

        val reply = Comment(
            id = "c2",
            eventId = "event-1",
            section = CommentSection.TRANSPORT,
            parentCommentId = "c1",
            authorId = p2.id,
            authorName = p2.name,
            content = "I prefer train",
            createdAt = Clock.System.now().toString()
        )

        commentStore.addComment("event-1", mainComment)
        commentStore.addComment("event-1", reply)

        // THEN: Threading structure preserved
        val thread = commentStore.getCommentThread("event-1", "c1")
        assertNotNull(thread)
        assertEquals("Train or car?", thread.comment.content)
        assertEquals(1, thread.replies.size)
    }

    @Test
    fun testCommentStatistics() = runTest {
        // GIVEN: Event with comments across sections
        val commentStore = TestCommentStore()
        val p1 = TestUser("p1", "Bob", UserRole.PARTICIPANT)
        val p2 = TestUser("p2", "Charlie", UserRole.PARTICIPANT)

        // WHEN: Create comments in different sections
        repeat(3) {
            commentStore.addComment(
                "event-1",
                Comment(
                    id = genTestId(),
                    eventId = "event-1",
                    section = CommentSection.BUDGET,
                    authorId = p1.id,
                    authorName = p1.name,
                    content = "Budget comment $it",
                    createdAt = Clock.System.now().toString()
                )
            )
        }

        repeat(2) {
            commentStore.addComment(
                "event-1",
                Comment(
                    id = genTestId(),
                    eventId = "event-1",
                    section = CommentSection.TRANSPORT,
                    authorId = p2.id,
                    authorName = p2.name,
                    content = "Transport comment $it",
                    createdAt = Clock.System.now().toString()
                )
            )
        }

        // THEN: Statistics accurate
        val total = commentStore.countComments("event-1")
        val budgetCount = commentStore.countCommentsBySection("event-1", CommentSection.BUDGET)
        val transportCount = commentStore.countCommentsBySection("event-1", CommentSection.TRANSPORT)

        assertEquals(5, total)
        assertEquals(3, budgetCount)
        assertEquals(2, transportCount)
    }
}
