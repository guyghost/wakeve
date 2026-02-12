package com.guyghost.wakeve.logistics

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.DatabaseProvider
import com.guyghost.wakeve.TestDatabaseFactory
import com.guyghost.wakeve.comment.CommentRepository
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.CommentRequest
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for Accommodation Planning features in Wakeve.
 * 
 * Tests accommodation selection and discussion:
 * - Creating accommodation suggestions
 * - Discussing hotel/lodging options
 * - Managing room assignments
 * - Tracking check-in/check-out information
 * - Commenting on accommodation details
 */
class AccommodationIntegrationTest {
    
    private lateinit var db: WakeveDb
    private lateinit var commentRepository: CommentRepository
    private lateinit var eventRepository: DatabaseEventRepository
    
    private val participant1Id = "user-1"
    private val participant1Name = "Alice"
    private val participant2Id = "user-2"
    private val participant2Name = "Bob"
    private val participant3Id = "user-3"
    private val participant3Name = "Charlie"
    
    private fun createTestDatabase(): WakeveDb {
        return DatabaseProvider.getDatabase(TestDatabaseFactory())
    }
    
    @BeforeTest
    fun setup() {
        DatabaseProvider.resetDatabase()
        db = createTestDatabase()
        commentRepository = CommentRepository(db)
        eventRepository = DatabaseEventRepository(db)
    }
    
    // ============================================================================
    // Scenario 1: Hotel Suggestion and Comparison
    // ============================================================================
    
    @Test
    fun testHotelSuggestionAndComparison_discusses_accommodation_options() {
        runBlocking {
            val event = createTestEvent()
            
            // Alice suggests a luxury hotel
            val luxuryHotel = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "paris-hotels",
                    content = "I found a 5-star hotel near the Eiffel Tower - $200/night"
                )
            )
            
            assertEquals(CommentSection.ACCOMMODATION, luxuryHotel.section)
            assertEquals("paris-hotels", luxuryHotel.sectionItemId)
            
            // Bob suggests a budget-friendly option
            val budgetHotel = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "paris-hotels",
                    content = "Found a nice 3-star in Marais district - $80/night, great location",
                    parentCommentId = luxuryHotel.id
                )
            )
            
            assertEquals(luxuryHotel.id, budgetHotel.parentCommentId)
            
            // Charlie votes for budget option
            val budgetVote = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "paris-hotels",
                    content = "I prefer the budget option - better value for money",
                    parentCommentId = budgetHotel.id
                )
            )
            
            assertNotNull(budgetVote)
            
            // Verify all hotel discussion is captured
            val hotelComments = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.ACCOMMODATION,
                "paris-hotels"
            )
            assertEquals(3, hotelComments.size)
        }
    }
    
    // ============================================================================
    // Scenario 2: Room Assignment Discussion
    // ============================================================================
    
    @Test
    fun testRoomAssignmentDiscussion_organizes_room_allocations() {
        runBlocking {
            val event = createTestEvent()
            
            // Create room assignment thread
            val assignmentThread = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "room-assignments",
                    content = "Let's decide who shares which rooms"
                )
            )
            
            // Suggest room grouping
            val roomGrouping = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "room-assignments",
                    content = "I'll share a room with Bob. Charlie, would you like a single room?",
                    parentCommentId = assignmentThread.id
                )
            )
            
            assertEquals(assignmentThread.id, roomGrouping.parentCommentId)
            
            // Charlie agrees
            val agreement = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "room-assignments",
                    content = "Yes, I'd prefer a single room. That works for me!",
                    parentCommentId = roomGrouping.id
                )
            )
            
            assertEquals(roomGrouping.id, agreement.parentCommentId)
            
            // Verify assignments are documented
            val assignments = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.ACCOMMODATION,
                "room-assignments"
            )
            assertEquals(3, assignments.size)
        }
    }
    
    // ============================================================================
    // Scenario 3: Check-in/Check-out Details
    // ============================================================================
    
    @Test
    fun testCheckInCheckOutDetails_tracks_accommodation_timing() {
        runBlocking {
            val event = createTestEvent()
            
            // Create check-in details thread
            val checkInThread = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "check-in-details",
                    content = "Hotel check-in is at 3pm on Friday, check-out is 11am on Sunday"
                )
            )
            
            // Discuss early arrival
            val earlyArrival = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "check-in-details",
                    content = "I'll arrive Thursday evening - can we arrange early check-in or luggage storage?",
                    parentCommentId = checkInThread.id
                )
            )
            
            // Confirm arrangements
            val confirmation = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "check-in-details",
                    content = "Will check with hotel about early check-in by 2pm Friday",
                    parentCommentId = earlyArrival.id
                )
            )
            
            // Verify check-in information is documented
            val checkInComments = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.ACCOMMODATION,
                "check-in-details"
            )
            assertTrue(checkInComments.isNotEmpty())
        }
    }
    
    // ============================================================================
    // Scenario 4: Accommodation Cost Tracking
    // ============================================================================
    
    @Test
    fun testAccommodationCostTracking_documents_pricing_details() {
        runBlocking {
            val event = createTestEvent()
            
            // Create cost discussion thread
            val costThread = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "cost-breakdown",
                    content = "Let's confirm the accommodation costs and how we'll split them"
                )
            )
            
            // Provide cost details
            val costDetails = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "cost-breakdown",
                    content = "Hotel: $80/night per person x 2 nights = $160 per person",
                    parentCommentId = costThread.id
                )
            )
            
            // Discuss payment method
            val paymentDiscussion = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "cost-breakdown",
                    content = "I can pay upfront and you two can reimburse me",
                    parentCommentId = costDetails.id
                )
            )
            
            // Agree on split
            val agreement = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "cost-breakdown",
                    content = "Sounds good. $160 each - I'll Venmo you after the trip",
                    parentCommentId = paymentDiscussion.id
                )
            )
            
            // Verify cost information is captured
            val costComments = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.ACCOMMODATION,
                "cost-breakdown"
            )
            assertEquals(4, costComments.size)
        }
    }
    
    // ============================================================================
    // Scenario 5: Special Requirements and Accessibility
    // ============================================================================
    
    @Test
    fun testAccessibilityAndSpecialRequirements_documents_accommodation_needs() {
        runBlocking {
            val event = createTestEvent()
            
            // Create special requirements thread
            val requirementsThread = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "special-requirements",
                    content = "Any special accommodation needs we should know about?"
                )
            )
            
            // Alice mentions accessibility needs
            val accessibilityNote = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "special-requirements",
                    content = "I need an elevator-accessible room if possible",
                    parentCommentId = requirementsThread.id
                )
            )
            
            // Bob mentions amenity preferences
            val amenityNote = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "special-requirements",
                    content = "Please make sure there's a gym at the hotel",
                    parentCommentId = requirementsThread.id
                )
            )
            
            // Charlie notes room feature preference
            val featureNote = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "special-requirements",
                    content = "I'd prefer a high floor with a view if possible",
                    parentCommentId = requirementsThread.id
                )
            )
            
            // Confirm with hotel
            val confirmation = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "special-requirements",
                    content = "Will request accessible room, gym availability, and high floor at booking",
                    parentCommentId = featureNote.id
                )
            )
            
            // Verify all special requirements are documented
            val requirementComments = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.ACCOMMODATION,
                "special-requirements"
            )
            assertEquals(5, requirementComments.size)
        }
    }
    
    // ============================================================================
    // Scenario 6: Accommodation Status and Confirmation
    // ============================================================================
    
    @Test
    fun testAccommodationStatusConfirmation_tracks_booking_progress() {
        runBlocking {
            val event = createTestEvent()
            
            // Start with proposal
            val proposal = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "main-hotel",
                    content = "[PROPOSED] Marais Hotel - 3-star, $80/night"
                )
            )
            
            assertTrue(proposal.content.contains("[PROPOSED]"))
            
            // Get team approval
            val approval1 = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "main-hotel",
                    content = "Approved by me",
                    parentCommentId = proposal.id
                )
            )
            
            val approval2 = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "main-hotel",
                    content = "Looks good!",
                    parentCommentId = proposal.id
                )
            )
            
            // Confirm booking
            val confirmed = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "main-hotel",
                    content = "[CONFIRMED] Booking made for 3 rooms, Dec 1-3, Marais Hotel"
                )
            )
            
            assertTrue(confirmed.content.contains("[CONFIRMED]"))
            
            // Provide confirmation details
            val confirmationDetails = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.ACCOMMODATION,
                    sectionItemId = "main-hotel",
                    content = "Confirmation #: 12345. Email sent to all with booking details.",
                    parentCommentId = confirmed.id
                )
            )
            
            // Verify status progression
            val allComments = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.ACCOMMODATION,
                "main-hotel"
            )
            assertEquals(5, allComments.size) // Proposal + 2 approvals + confirmation + details
        }
    }
    
    // ============================================================================
    // Helper Methods
    // ============================================================================
    
    private suspend fun createTestEvent(
        eventId: String = "accommodation-event-1"
    ): Event {
        val event = Event(
            id = eventId,
            title = "Accommodation Planning Event",
            description = "Event for testing accommodation features",
            organizerId = participant1Id,
            participants = listOf(participant1Id, participant2Id, participant3Id),
            proposedSlots = emptyList(),
            deadline = "2025-12-31T23:59:59Z",
            status = EventStatus.CONFIRMED,
            createdAt = Clock.System.now().toString(),
            updatedAt = Clock.System.now().toString()
        )
        
        eventRepository.createEvent(event)
        return event
    }
}
