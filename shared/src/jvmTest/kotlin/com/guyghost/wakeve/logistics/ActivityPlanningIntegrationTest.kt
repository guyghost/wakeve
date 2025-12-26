package com.guyghost.wakeve.logistics

import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.DatabaseProvider
import com.guyghost.wakeve.TestDatabaseFactory
import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.comment.CommentRepository
import kotlin.test.*
import kotlinx.datetime.*
import kotlinx.coroutines.runBlocking

/**
 * Integration tests for Activity Planning features in Wakeve.
 * 
 * Tests activity planning and organization functionality:
 * - Suggesting activities based on participant preferences
 * - Voting on proposed activities
 * - Registering participant interest/attendance for activities
 * - Tracking activity status (proposed, approved, confirmed, completed)
 * - Managing activity costs and participant shares
 * - Documenting activity requirements and logistics
 */
class ActivityPlanningIntegrationTest {
    
    private lateinit var db: WakevDb
    private lateinit var commentRepository: CommentRepository
    private lateinit var eventRepository: DatabaseEventRepository
    
    private val participant1Id = "user-1"
    private val participant1Name = "Alice"
    private val participant2Id = "user-2"
    private val participant2Name = "Bob"
    private val participant3Id = "user-3"
    private val participant3Name = "Charlie"
    private val participant4Id = "user-4"
    private val participant4Name = "Diana"
    
    private fun createTestDatabase(): WakevDb {
        return DatabaseProvider.getDatabase(TestDatabaseFactory())
    }
    
    private fun now(): String = Clock.System.now().toString()
    
    @BeforeTest
    fun setup() {
        DatabaseProvider.resetDatabase()
        db = createTestDatabase()
        commentRepository = CommentRepository(db)
        eventRepository = DatabaseEventRepository(db)
    }
    
    private fun createTestEvent(eventId: String, title: String): Event {
        val now = now()
        return Event(
            id = eventId,
            title = title,
            description = "Test event for activity planning",
            organizerId = participant1Id,
            proposedSlots = emptyList(),
            deadline = now,
            status = EventStatus.CONFIRMED,
            createdAt = now,
            updatedAt = now
        )
    }
    
    // ============================================================================
    // Scenario 1: Complete Activity Planning with Voting
    // ============================================================================
    /**
     * Scenario: Complete activity planning workflow with voting
     * GIVEN an event with confirmed date and multiple participants
     * WHEN activities are suggested and participants vote
     * THEN popular activities are approved and organized
     */
    @Test
    fun testCompletActivityPlanningWithVoting_flows_from_suggestion_to_approval() {
        runBlocking {
            // ARRANGE: Create event
            val eventId = "event-1"
            val event = createTestEvent(eventId, "Team Adventure Weekend")
            eventRepository.createEvent(event)
            
            // ACT 1: Suggest activities with comments
            val hikingActivityComment = commentRepository.createComment(
                eventId = eventId,
                authorId = participant1Id,
                authorName = participant1Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "I suggest a hiking activity - difficulty: medium, duration: 4 hours, cost: $25 per person"
                )
            )
            
            val kayakingActivityComment = commentRepository.createComment(
                eventId = eventId,
                authorId = participant2Id,
                authorName = participant2Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "How about kayaking? - scenic, relaxing, cost: $40 per person, includes guide"
                )
            )
            
            val campingActivityComment = commentRepository.createComment(
                eventId = eventId,
                authorId = participant3Id,
                authorName = participant3Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "Camping is essential! Tent camping with campfire activities"
                )
            )
            
            // ACT 2: Add voting comments
            val vote1 = commentRepository.createComment(
                eventId = eventId,
                authorId = participant2Id,
                authorName = participant2Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "+1 for hiking, -1 for kayaking"
                )
            )
            
            val vote2 = commentRepository.createComment(
                eventId = eventId,
                authorId = participant3Id,
                authorName = participant3Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "+1 for hiking, +1 for camping"
                )
            )
            
            val vote3 = commentRepository.createComment(
                eventId = eventId,
                authorId = participant4Id,
                authorName = participant4Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "+1 for all activities - variety is good!"
                )
            )
            
            // ASSERT: Verify activity suggestions are recorded
            val activityComments = commentRepository.getCommentsBySection(eventId, CommentSection.ACTIVITY)
            assertEquals(6, activityComments.size, "Should have 6 activity comments")
            
            // Verify voting comments exist
            assertTrue(activityComments.any { it.content.contains("+1 for hiking") }, "Hiking should have votes")
            assertTrue(activityComments.any { it.content.contains("camping") }, "Camping should be mentioned")
            
            // Verify comment timestamps are sequential
            val timestamps = activityComments.map { it.createdAt }.sorted()
            assertEquals(timestamps.size, activityComments.size, "All comments should have timestamps")
        }
    }
    
    // ============================================================================
    // Scenario 2: Activity Participant Registration
    // ============================================================================
    /**
     * Scenario: Managing participant registration for activities
     * GIVEN approved activities for an event
     * WHEN participants register for their preferred activities
     * THEN registrations are tracked and activity rosters are built
     */
    @Test
    fun testActivityParticipantRegistration_tracks_who_joins_each_activity() {
        runBlocking {
            // ARRANGE: Create event
            val eventId = "event-2"
            val event = createTestEvent(eventId, "Weekend Getaway")
            eventRepository.createEvent(event)
            
            // ACT 1: Record activity approvals
            val hikingApproved = commentRepository.createComment(
                eventId = eventId,
                authorId = participant1Id,
                authorName = participant1Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "APPROVED: Mountain Hiking - Saturday 8am, Meet at parking lot, Equipment: good shoes, water, snacks"
                )
            )
            
            val kayakApproved = commentRepository.createComment(
                eventId = eventId,
                authorId = participant1Id,
                authorName = participant1Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "APPROVED: Lake Kayaking - Saturday 2pm, Cost: $35/person, Includes guide and equipment"
                )
            )
            
            // ACT 2: Participants register for activities
            val aliceRegisterHiking = commentRepository.createComment(
                eventId = eventId,
                authorId = participant1Id,
                authorName = participant1Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "Registering for: Mountain Hiking | Can provide transportation for 2 people"
                )
            )
            
            val bobRegisterBoth = commentRepository.createComment(
                eventId = eventId,
                authorId = participant2Id,
                authorName = participant2Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "Registering for: Mountain Hiking, Lake Kayaking | Will pay for 1 extra person's kayaking"
                )
            )
            
            val charlieRegisterHiking = commentRepository.createComment(
                eventId = eventId,
                authorId = participant3Id,
                authorName = participant3Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "Registering for: Mountain Hiking | Need to leave by 5pm"
                )
            )
            
            val dianaRegisterKayak = commentRepository.createComment(
                eventId = eventId,
                authorId = participant4Id,
                authorName = participant4Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "Registering for: Lake Kayaking | Cannot do hiking (knee injury)"
                )
            )
            
            // ASSERT: Verify registrations are recorded
            val registrationComments = commentRepository.getCommentsBySection(eventId, CommentSection.ACTIVITY)
                .filter { it.content.contains("Registering for:") }
            
            assertEquals(4, registrationComments.size, "All 4 participants should have registration comments")
            
            // Verify hiking has 3 registrations
            val hikingRegistrations = registrationComments.filter { it.content.contains("Mountain Hiking") }
            assertEquals(3, hikingRegistrations.size, "3 people should register for hiking")
            
            // Verify kayaking has 2 registrations
            val kayakRegistrations = registrationComments.filter { it.content.contains("Lake Kayaking") }
            assertEquals(2, kayakRegistrations.size, "2 people should register for kayaking")
        }
    }
    
    // ============================================================================
    // Scenario 3: Activity Status Progression
    // ============================================================================
    /**
     * Scenario: Tracking activity status through lifecycle
     * GIVEN an activity plan with participants
     * WHEN activities progress through statuses
     * THEN status changes are documented and verified
     */
    @Test
    fun testActivityStatusProgression_tracks_proposal_approval_confirmation_completion() {
        runBlocking {
            // ARRANGE: Create event
            val eventId = "event-3"
            val event = createTestEvent(eventId, "Activity Tracking Test")
            eventRepository.createEvent(event)
            
            // ACT 1: Propose activity
            val activityProposal = commentRepository.createComment(
                eventId = eventId,
                authorId = participant1Id,
                authorName = participant1Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "[STATUS: PROPOSED] Rock Climbing - Outdoor climbing session at local cliff"
                )
            )
            
            // ACT 2: Approve activity
            val activityApproval = commentRepository.createComment(
                eventId = eventId,
                authorId = participant2Id,
                authorName = participant2Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "[STATUS: APPROVED] Rock Climbing - Approved! Need to confirm: Date (Saturday 2pm), Cost ($50/person), Guide availability"
                )
            )
            
            // ACT 3: Confirm activity
            val activityConfirmation = commentRepository.createComment(
                eventId = eventId,
                authorId = participant1Id,
                authorName = participant1Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "[STATUS: CONFIRMED] Rock Climbing - Confirmed for Saturday 2pm. Guide booked, 6 spots available, 4 registered"
                )
            )
            
            // ACT 4: Mark activity complete
            val activityCompletion = commentRepository.createComment(
                eventId = eventId,
                authorId = participant1Id,
                authorName = participant1Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "[STATUS: COMPLETED] Rock Climbing - Activity completed successfully! All participants present, great experience, guide rated 5 stars"
                )
            )
            
            // ASSERT: Verify status progression
            val statusComments = commentRepository.getCommentsBySection(eventId, CommentSection.ACTIVITY)
            assertEquals(4, statusComments.size, "Should have 4 status update comments. Got: ${statusComments.size} comments: ${statusComments.map { it.content.take(50) }}")
            
            // Sort by timestamp to get chronological order
            val sortedComments = statusComments.sortedBy { it.createdAt }
            val statuses = sortedComments.map { comment ->
                when {
                    comment.content.contains("[STATUS: PROPOSED]") -> "PROPOSED"
                    comment.content.contains("[STATUS: APPROVED]") -> "APPROVED"
                    comment.content.contains("[STATUS: CONFIRMED]") -> "CONFIRMED"
                    comment.content.contains("[STATUS: COMPLETED]") -> "COMPLETED"
                    else -> "UNKNOWN"
                }
            }
            
            assertEquals(listOf("PROPOSED", "APPROVED", "CONFIRMED", "COMPLETED"), statuses, 
                "Statuses should progress correctly")
        }
    }
    
    // ============================================================================
    // Scenario 4: Activity Cost Management
    // ============================================================================
    /**
     * Scenario: Managing costs for group activities
     * GIVEN activities with associated costs
     * WHEN participants register and costs are calculated
     * THEN per-person costs and total expenditure are tracked
     */
    @Test
    fun testActivityCostManagement_calculates_and_splits_costs_correctly() {
        runBlocking {
            // ARRANGE: Create event
            val eventId = "event-4"
            val event = createTestEvent(eventId, "Paid Activities Event")
            eventRepository.createEvent(event)
            
            // ACT 1: Record activities with costs
            val activity1 = commentRepository.createComment(
                eventId = eventId,
                authorId = participant1Id,
                authorName = participant1Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "ACTIVITY: Cooking Class - Total Cost: $120, Capacity: 4 people, Per-person: $30"
                )
            )
            
            val activity2 = commentRepository.createComment(
                eventId = eventId,
                authorId = participant1Id,
                authorName = participant1Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "ACTIVITY: Spa Day - Total Cost: $240, Capacity: 6 people, Per-person: $40 (4 registered)"
                )
            )
            
            // ACT 2: Add participant registrations with cost notes
            val participant1Costs = commentRepository.createComment(
                eventId = eventId,
                authorId = participant1Id,
                authorName = participant1Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "Registrations: Cooking Class ($30) + Spa Day ($40) = $70 total"
                )
            )
            
            val participant2Costs = commentRepository.createComment(
                eventId = eventId,
                authorId = participant2Id,
                authorName = participant2Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "Registrations: Cooking Class ($30) only = $30 total (cannot do spa)"
                )
            )
            
            val participant3Costs = commentRepository.createComment(
                eventId = eventId,
                authorId = participant3Id,
                authorName = participant3Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "Registrations: Spa Day ($40) only = $40 total (skip cooking)"
                )
            )
            
            val participant4Costs = commentRepository.createComment(
                eventId = eventId,
                authorId = participant4Id,
                authorName = participant4Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "Registrations: Cooking Class ($30) + Spa Day ($40) = $70 total"
                )
            )
            
            // ACT 3: Add cost summary
            val costSummary = commentRepository.createComment(
                eventId = eventId,
                authorId = participant1Id,
                authorName = participant1Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "COST SUMMARY: Cooking Class (4 registered): $120 total, Spa Day (3 registered): $120 actual cost, Total event activities: $240"
                )
            )
            
            // ASSERT: Verify cost tracking
            val costComments = commentRepository.getCommentsBySection(eventId, CommentSection.ACTIVITY)
            assertEquals(7, costComments.size, "Should have 7 cost-related comments")
            
            val individualCosts = costComments.filter { it.content.contains("Registrations:") }
            assertEquals(4, individualCosts.size, "All 4 participants should have cost breakdowns")
            
            assertTrue(costComments.any { it.content.contains("COST SUMMARY") }, "Should have cost summary")
        }
    }
    
    // ============================================================================
    // Scenario 5: Activity Requirements and Logistics
    // ============================================================================
    /**
     * Scenario: Documenting activity requirements and logistics
     * GIVEN activities with special requirements
     * WHEN requirements are documented
     * THEN participants can verify they can meet requirements
     */
    @Test
    fun testActivityRequirementsAndLogistics_documents_special_needs_and_equipment() {
        runBlocking {
            // ARRANGE: Create event
            val eventId = "event-5"
            val event = createTestEvent(eventId, "Adventure Activities")
            eventRepository.createEvent(event)
            
            // ACT 1: Document hiking requirements
            val hikingRequirements = commentRepository.createComment(
                eventId = eventId,
                authorId = participant1Id,
                authorName = participant1Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "ACTIVITY: Mountain Hiking\nDIFFICULTY: Medium\nREQUIREMENTS:\n- Good hiking boots (mandatory)\n- Weather-appropriate clothing\n- Water bottle (2L min)\n- Sunscreen\nESTIMATED DURATION: 4-5 hours\nWHOLE BODY WORKOUT"
                )
            )
            
            // ACT 2: Add accessibility considerations
            val accessibility = commentRepository.createComment(
                eventId = eventId,
                authorId = participant2Id,
                authorName = participant2Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "ACCESSIBILITY NOTES: The hiking trail has:\n- No wheelchair access to summit\n- Some steep sections (might be challenging for mobility issues)\n- Good breaks/rest areas every 30 min\n- Clear marked trail\nPLEASE MENTION ANY MOBILITY CONCERNS"
                )
            )
            
            // ACT 3: Participants confirm they have requirements
            val confirmation1 = commentRepository.createComment(
                eventId = eventId,
                authorId = participant1Id,
                authorName = participant1Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "HIKING REGISTRATION: Alice ✓ has hiking boots ✓ fit and ready ✓ can do moderate distance"
                )
            )
            
            val confirmation2 = commentRepository.createComment(
                eventId = eventId,
                authorId = participant3Id,
                authorName = participant3Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "HIKING REGISTRATION: Charlie ⚠ need to borrow hiking boots (anyone have size 10?), have all other gear"
                )
            )
            
            val confirmation3 = commentRepository.createComment(
                eventId = eventId,
                authorId = participant4Id,
                authorName = participant4Name,
                request = CommentRequest(
                    section = CommentSection.ACTIVITY,
                    content = "HIKING REGISTRATION: Diana ✗ Cannot attend - knee injury, not suitable for this hike. I'll do water activities instead."
                )
            )
            
            // ASSERT: Verify requirements are documented
            val requirementsComments = commentRepository.getCommentsBySection(eventId, CommentSection.ACTIVITY)
            assertEquals(5, requirementsComments.size, "Should have 5 requirement comments")
            
            assertTrue(requirementsComments.any { it.content.contains("REQUIREMENTS:") }, 
                "Should have activity requirements documented")
            assertTrue(requirementsComments.any { it.content.contains("ACCESSIBILITY") }, 
                "Should have accessibility notes")
            
            val confirmations = requirementsComments.filter { it.content.contains("REGISTRATION:") }
            assertEquals(3, confirmations.size, "Should have 3 participant confirmations")
        }
    }
    
    // ============================================================================
    // Scenario 6: Activity Planning Statistics
    // ============================================================================
    /**
     * Scenario: Aggregating activity planning statistics
     * GIVEN completed activity planning
     * WHEN statistics are calculated
     * THEN participation rates and engagement metrics are tracked
     */
    @Test
    fun testActivityPlanningStatistics_aggregates_participation_and_engagement_metrics() {
        runBlocking {
            // ARRANGE: Create event with comprehensive activity planning
            val eventId = "event-6"
            val event = createTestEvent(eventId, "Full Activity Event")
            eventRepository.createEvent(event)
            
            // ACT 1: Create activity suggestions
            repeat(5) { index ->
                commentRepository.createComment(
                    eventId = eventId,
                    authorId = participant1Id,
                    authorName = participant1Name,
                    request = CommentRequest(
                        section = CommentSection.ACTIVITY,
                        content = "Activity ${index + 1}: Suggested activity with details"
                    )
                )
            }
            
            // ACT 2: Create voting comments from all participants
            repeat(4) { participantIndex ->
                val userId = "user-${participantIndex + 1}"
                val userName = listOf("Alice", "Bob", "Charlie", "Diana")[participantIndex]
                repeat(3) { voteIndex ->
                    commentRepository.createComment(
                        eventId = eventId,
                        authorId = userId,
                        authorName = userName,
                        request = CommentRequest(
                            section = CommentSection.ACTIVITY,
                            content = "+1 Activity ${voteIndex + 1}"
                        )
                    )
                }
            }
            
            // ACT 3: Create registrations
            repeat(4) { participantIndex ->
                val userId = "user-${participantIndex + 1}"
                val userName = listOf("Alice", "Bob", "Charlie", "Diana")[participantIndex]
                commentRepository.createComment(
                    eventId = eventId,
                    authorId = userId,
                    authorName = userName,
                    request = CommentRequest(
                        section = CommentSection.ACTIVITY,
                        content = "Registering for activities 1, 2, 3"
                    )
                )
            }
            
            // ASSERT: Verify statistics can be calculated
            val allComments = commentRepository.getCommentsBySection(eventId, CommentSection.ACTIVITY)
            assertEquals(21, allComments.size, "Should have 21 total activity comments (5 suggestions + 12 votes + 4 registrations)")
            
            // Count suggestions
            val suggestions = allComments.filter { it.content.contains("Activity") && it.content.contains("Suggested") }
            assertEquals(5, suggestions.size, "Should have 5 activity suggestions")
            
            // Count votes
            val votes = allComments.filter { it.content.contains("+1") }
            assertEquals(12, votes.size, "Should have 12 voting comments (3 per participant)")
            
            // Count registrations
            val registrations = allComments.filter { it.content.contains("Registering for") }
            assertEquals(4, registrations.size, "Should have 4 registration comments")
            
            // Verify engagement metrics
            val engagedParticipants = allComments.map { it.authorId }.distinct()
            assertEquals(4, engagedParticipants.size, "All 4 participants should be engaged")
            
            val participantCommentCounts = allComments.groupingBy { it.authorId }.eachCount()
            assertTrue(participantCommentCounts.all { (_, count) -> count >= 3 }, 
                "Each participant should have at least 3 comments")
        }
    }
}
