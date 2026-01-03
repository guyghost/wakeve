package com.guyghost.wakeve.logistics

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.DatabaseProvider
import com.guyghost.wakeve.TestDatabaseFactory
import com.guyghost.wakeve.comment.CommentRepository
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.CommentRequest
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for Equipment Checklist features in Wakeve.
 * 
 * Tests equipment planning and tracking:
 * - Creating equipment checklists
 * - Assigning equipment responsibility to participants
 * - Tracking equipment status (needed, assigned, confirmed, brought)
 * - Discussing equipment specifics and quantities
 * - Verifying all equipment is accounted for
 */
class EquipmentChecklistIntegrationTest {
    
    private lateinit var db: WakevDb
    private lateinit var commentRepository: CommentRepository
    private lateinit var eventRepository: DatabaseEventRepository
    
    private val participant1Id = "user-1"
    private val participant1Name = "Alice"
    private val participant2Id = "user-2"
    private val participant2Name = "Bob"
    private val participant3Id = "user-3"
    private val participant3Name = "Charlie"
    
    private fun createTestDatabase(): WakevDb {
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
    // Scenario 1: Complete Equipment Checklist Creation
    // ============================================================================
    
    @Test
    fun testCompleteEquipmentChecklist_creates_and_assigns_all_items() {
        runBlocking {
            val event = createTestEvent()
            
            // Alice creates the equipment checklist
            val checklist = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "main-checklist",
                    content = "Equipment checklist for camping trip"
                )
            )
            
            assertEquals(CommentSection.EQUIPMENT, checklist.section)
            assertEquals("main-checklist", checklist.sectionItemId)
            
            // Alice adds tent assignment
            val tentAssignment = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "main-checklist",
                    content = "[] Tent - need 2 tents for 3 people",
                    parentCommentId = checklist.id
                )
            )
            
            // Bob volunteers for tent
            val bobAssignment = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "main-checklist",
                    content = "[X] I'll bring my 2-person tent",
                    parentCommentId = tentAssignment.id
                )
            )
            
            assertEquals(participant2Id, bobAssignment.authorId)
            
            // Alice adds sleeping bags
            val sleepingBags = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "main-checklist",
                    content = "[] Sleeping bags - need 3",
                    parentCommentId = checklist.id
                )
            )
            
            // Charlie volunteers for sleeping bags
            val charlieAssignment = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "main-checklist",
                    content = "[X] I have 3 sleeping bags and will bring them",
                    parentCommentId = sleepingBags.id
                )
            )
            
            assertEquals(participant3Id, charlieAssignment.authorId)
            
            // Verify all equipment discussions are captured
            val equipment = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.EQUIPMENT,
                "main-checklist"
            )
            assertEquals(5, equipment.size)
        }
    }
    
    // ============================================================================
    // Scenario 2: Equipment Quantity Confirmation
    // ============================================================================
    
    @Test
    fun testEquipmentQuantityConfirmation_tracks_quantities_and_assignments() {
        runBlocking {
            val event = createTestEvent()
            
            // Create quantity discussion thread
            val quantityThread = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "quantities",
                    content = "Let's confirm equipment quantities needed"
                )
            )
            
            // Alice specifies cooking equipment
            val cookware = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "quantities",
                    content = "Need: 1 camping stove, 2 pans, 3 plates, utensils, pot",
                    parentCommentId = quantityThread.id
                )
            )
            
            // Bob takes cooking assignment
            val cookingAssignment = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "quantities",
                    content = "I'll bring camping stove, 2 pans, pot, and utensils",
                    parentCommentId = cookware.id
                )
            )
            
            // Charlie confirms he has plates
            val plateConfirmation = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "quantities",
                    content = "I'll bring 3 plates and cups for everyone",
                    parentCommentId = cookware.id
                )
            )
            
            // Verify all quantity discussions are recorded
            val quantityComments = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.EQUIPMENT,
                "quantities"
            )
            assertEquals(4, quantityComments.size)
        }
    }
    
    // ============================================================================
    // Scenario 3: Equipment Status Tracking
    // ============================================================================
    
    @Test
    fun testEquipmentStatusTracking_progresses_from_needed_to_confirmed() {
        runBlocking {
            val event = createTestEvent()
            
            // Initial status: needed
            val neededStatus = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "lanterns",
                    content = "[NEEDED] Lanterns - required for campsite"
                )
            )
            
            assertTrue(neededStatus.content.contains("[NEEDED]"))
            
            // Update status: assigned
            val assigned = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "lanterns",
                    content = "[ASSIGNED] Bob will bring 2 lanterns",
                    parentCommentId = neededStatus.id
                )
            )
            
            assertTrue(assigned.content.contains("[ASSIGNED]"))
            
            // Confirm purchased/ready
            val confirmed = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "lanterns",
                    content = "[CONFIRMED] Purchased 2 LED lanterns, batteries included",
                    parentCommentId = assigned.id
                )
            )
            
            assertTrue(confirmed.content.contains("[CONFIRMED]"))
            
            // Final status: brought
            val brought = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "lanterns",
                    content = "[BROUGHT] Lanterns packed in my car",
                    parentCommentId = confirmed.id
                )
            )
            
            assertTrue(brought.content.contains("[BROUGHT]"))
            
            // Verify status progression
            val lanternComments = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.EQUIPMENT,
                "lanterns"
            )
            assertEquals(4, lanternComments.size)
        }
    }
    
    // ============================================================================
    // Scenario 4: Special Equipment Requirements
    // ============================================================================
    
    @Test
    fun testSpecialEquipmentRequirements_documents_specific_needs() {
        runBlocking {
            val event = createTestEvent()
            
            // Create special requirements thread
            val specialThread = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "special-items",
                    content = "Special equipment requirements"
                )
            )
            
            // Alice needs climbing gear
            val climbingGear = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "special-items",
                    content = "Need climbing rope and harnesses for rock climbing activity",
                    parentCommentId = specialThread.id
                )
            )
            
            // Bob offers his climbing equipment
            val climbingOffer = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "special-items",
                    content = "I have rock climbing equipment - 3 harnesses, rope, carabiners",
                    parentCommentId = climbingGear.id
                )
            )
            
            // Charlie needs photography equipment
            val photoEquipment = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "special-items",
                    content = "I'll bring a tripod for group photos and videos",
                    parentCommentId = specialThread.id
                )
            )
            
            // Confirm all special items
            val confirmation = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "special-items",
                    content = "Perfect! All special equipment is covered.",
                    parentCommentId = photoEquipment.id
                )
            )
            
            // Verify all special items are documented
            val specialComments = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.EQUIPMENT,
                "special-items"
            )
            assertEquals(5, specialComments.size)
        }
    }
    
    // ============================================================================
    // Scenario 5: Final Equipment Verification
    // ============================================================================
    
    @Test
    fun testFinalEquipmentVerification_confirms_all_items_covered() {
        runBlocking {
            val event = createTestEvent()
            
            // Create final verification checklist
            val verification = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "final-check",
                    content = "Final equipment verification before departure"
                )
            )
            
            // Alice verifies shelter
            val shelterCheck = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "final-check",
                    content = "[✓] SHELTER - 2 tents (Bob bringing)",
                    parentCommentId = verification.id
                )
            )
            
            // Bob verifies cooking
            val cookingCheck = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "final-check",
                    content = "[✓] COOKING - Stove, pans, pot, utensils (Bob bringing)",
                    parentCommentId = verification.id
                )
            )
            
            // Charlie verifies sleeping and misc
            val sleepingCheck = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "final-check",
                    content = "[✓] SLEEPING - 3 bags, plates, cups (Charlie bringing)",
                    parentCommentId = verification.id
                )
            )
            
            // Alice confirms all ready
            val allReady = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.EQUIPMENT,
                    sectionItemId = "final-check",
                    content = "[✓] ALL EQUIPMENT CONFIRMED - Ready for departure!",
                    parentCommentId = sleepingCheck.id
                )
            )
            
            // Verify final checklist is complete
            val finalComments = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.EQUIPMENT,
                "final-check"
            )
            assertEquals(5, finalComments.size)
        }
    }
    
    // ============================================================================
    // Scenario 6: Equipment Statistics and Summary
    // ============================================================================
    
    @Test
    fun testEquipmentStatistics_aggregates_equipment_planning_activity() {
        runBlocking {
            val event = createTestEvent()
            
            // Create various equipment comments
            commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(CommentSection.EQUIPMENT, "tents", "Tent discussion 1", null)
            )
            commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(CommentSection.EQUIPMENT, "tents", "Tent discussion 2", null)
            )
            commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(CommentSection.EQUIPMENT, "sleeping-bags", "Sleeping bag discussion", null)
            )
            commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(CommentSection.EQUIPMENT, "cooking", "Cooking equipment discussion", null)
            )
            
            // Get all equipment comments
            val equipmentComments = commentRepository.getCommentsBySection(event.id, CommentSection.EQUIPMENT)
            assertEquals(4, equipmentComments.size)
            
            // Get statistics
            val stats = commentRepository.getCommentStatistics(event.id)
            assertEquals(4, stats.totalComments)
            assertEquals(4, stats.commentsBySection[CommentSection.EQUIPMENT])
            
            // Verify top contributors
            assertTrue(stats.topContributors.isNotEmpty())
        }
    }
    
    // ============================================================================
    // Helper Methods
    // ============================================================================
    
    private suspend fun createTestEvent(
        eventId: String = "equipment-event-1"
    ): Event {
        val event = Event(
            id = eventId,
            title = "Equipment Planning Event",
            description = "Event for testing equipment planning features",
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
