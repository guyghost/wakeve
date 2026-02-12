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
 * Integration tests for Meal Planning features in Wakeve.
 * 
 * Tests meal planning and discussion functionality:
 * - Creating meal plans
 * - Adding dietary restrictions
 * - Assigning meal responsibility to participants
 * - Commenting on meal planning details
 * - Tracking meal status (proposed, confirmed, completed)
 */
class MealPlanningIntegrationTest {
    
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
    // Scenario 1: Complete Meal Planning Workflow
    // ============================================================================
    
    @Test
    fun testCompleteMealPlanningWorkflow_creates_meals_with_discussions() {
        runBlocking {
            // Create event
            val event = createTestEvent()
            
            // 1. Create initial meal plan comment
            val mealDiscussion = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "breakfast",
                    content = "Should we do continental or full breakfast?"
                )
            )
            
            assertNotNull(mealDiscussion)
            assertEquals(CommentSection.MEAL, mealDiscussion.section)
            assertEquals("breakfast", mealDiscussion.sectionItemId)
            assertEquals(participant1Id, mealDiscussion.authorId)
            
            // 2. Bob suggests a meal option
            val mealSuggestion = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "breakfast",
                    content = "I can handle making pancakes and bacon for everyone",
                    parentCommentId = mealDiscussion.id
                )
            )
            
            assertEquals(mealDiscussion.id, mealSuggestion.parentCommentId)
            
            // 3. Charlie agrees and adds dietary notes
            val dietaryNote = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "breakfast",
                    content = "Great! Can you make some vegetarian options too?",
                    parentCommentId = mealSuggestion.id
                )
            )
            
            assertNotNull(dietaryNote)
            assertEquals(mealSuggestion.id, dietaryNote.parentCommentId)
            
            // Verify all breakfast comments are retrievable
            val breakfastComments = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.MEAL,
                "breakfast"
            )
            assertEquals(3, breakfastComments.size)
        }
    }
    
    // ============================================================================
    // Scenario 2: Dietary Restrictions Discussion
    // ============================================================================
    
    @Test
    fun testDietaryRestrictionsDiscussion_tracks_participant_requirements() {
        runBlocking {
            val event = createTestEvent()
            
            // Create a thread about dietary requirements
            val dietaryThread = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "dietary-info",
                    content = "Let's collect dietary restrictions and preferences"
                )
            )
            
            // Alice notes her dietary preference
            val aliceDiet = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "dietary-info",
                    content = "I'm vegetarian - no meat",
                    parentCommentId = dietaryThread.id
                )
            )
            
            assertEquals(participant1Id, aliceDiet.authorId)
            
            // Bob notes his dietary preference
            val bobDiet = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "dietary-info",
                    content = "I'm allergic to nuts - be careful with desserts",
                    parentCommentId = dietaryThread.id
                )
            )
            
            assertEquals(participant2Id, bobDiet.authorId)
            
            // Charlie notes his dietary preference
            val charlieDiet = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "dietary-info",
                    content = "I'm gluten-free",
                    parentCommentId = dietaryThread.id
                )
            )
            
            assertEquals(participant3Id, charlieDiet.authorId)
            
            // Verify all dietary notes are captured
            val dietaryComments = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.MEAL,
                "dietary-info"
            )
            assertEquals(4, dietaryComments.size) // Thread + 3 preferences
        }
    }
    
    // ============================================================================
    // Scenario 3: Meal Responsibility Assignment
    // ============================================================================
    
    @Test
    fun testMealResponsibilityAssignment_tracks_who_brings_what() {
        runBlocking {
            val event = createTestEvent()
            
            // Create responsibility assignment comment thread
            val assignmentThread = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "responsibilities",
                    content = "Let's assign meal responsibilities"
                )
            )
            
            // Alice volunteers for main course
            val mainCourseAssignment = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "responsibilities",
                    content = "I'll prepare the main course (vegetarian pasta)",
                    parentCommentId = assignmentThread.id
                )
            )
            
            // Bob volunteers for appetizers
            val appetizersAssignment = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "responsibilities",
                    content = "I'll handle appetizers - cheese and charcuterie board (no nuts!)",
                    parentCommentId = assignmentThread.id
                )
            )
            
            // Charlie volunteers for dessert
            val dessertAssignment = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "responsibilities",
                    content = "I'll make gluten-free brownies for dessert",
                    parentCommentId = assignmentThread.id
                )
            )
            
            // Verify all assignments are recorded
            val assignments = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.MEAL,
                "responsibilities"
            )
            assertEquals(4, assignments.size) // Thread + 3 assignments
            
            // Verify each person has their assignment
            val aliceAssignments = assignments.filter { it.authorId == participant1Id }
            assertEquals(2, aliceAssignments.size) // Thread creation + assignment
        }
    }
    
    // ============================================================================
    // Scenario 4: Meal Status Progression
    // ============================================================================
    
    @Test
    fun testMealStatusProgression_tracks_from_planning_to_confirmation() {
        runBlocking {
            val event = createTestEvent()
            
            // Initial proposal
            val proposal = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "lunch",
                    content = "[PROPOSED] Grilled chicken with vegetables"
                )
            )
            
            assertNotNull(proposal)
            assertTrue(proposal.content.contains("[PROPOSED]"))
            
            // Team discussion
            val approval1 = commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "lunch",
                    content = "Sounds great! I approve this.",
                    parentCommentId = proposal.id
                )
            )
            
            val approval2 = commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "lunch",
                    content = "Approved - I'll bring the vegetarian alternative.",
                    parentCommentId = proposal.id
                )
            )
            
            // Final confirmation
            val confirmation = commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(
                    section = CommentSection.MEAL,
                    sectionItemId = "lunch",
                    content = "[CONFIRMED] Lunch is set! Grilled chicken with vegetables and vegan alternatives"
                )
            )
            
            assertTrue(confirmation.content.contains("[CONFIRMED]"))
            
            // Verify status progression is documented
            val lunchComments = commentRepository.getCommentsBySection(
                event.id,
                CommentSection.MEAL,
                "lunch"
            )
            assertEquals(4, lunchComments.size) // Proposal + 2 approvals + confirmation
        }
    }
    
    // ============================================================================
    // Scenario 5: Meal Statistics and Summary
    // ============================================================================
    
    @Test
    fun testMealPlanningStatistics_aggregates_meal_planning_activity() {
        runBlocking {
            val event = createTestEvent()
            
            // Create comments for breakfast
            commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(CommentSection.MEAL, "breakfast", "Breakfast discussion 1", null)
            )
            commentRepository.createComment(
                event.id,
                participant2Id,
                participant2Name,
                CommentRequest(CommentSection.MEAL, "breakfast", "Breakfast discussion 2", null)
            )
            
            // Create comments for lunch
            commentRepository.createComment(
                event.id,
                participant1Id,
                participant1Name,
                CommentRequest(CommentSection.MEAL, "lunch", "Lunch discussion", null)
            )
            
            // Create comments for dinner
            commentRepository.createComment(
                event.id,
                participant3Id,
                participant3Name,
                CommentRequest(CommentSection.MEAL, "dinner", "Dinner discussion", null)
            )
            
            // Get all meal comments
            val mealComments = commentRepository.getCommentsBySection(event.id, CommentSection.MEAL)
            assertEquals(4, mealComments.size)
            
            // Get statistics
            val stats = commentRepository.getCommentStatistics(event.id)
            assertEquals(4, stats.totalComments)
            assertEquals(4, stats.commentsBySection[CommentSection.MEAL])
        }
    }
    
    // ============================================================================
    // Helper Methods
    // ============================================================================
    
    private suspend fun createTestEvent(
        eventId: String = "meal-event-1"
    ): Event {
        val event = Event(
            id = eventId,
            title = "Meal Planning Event",
            description = "Event for testing meal planning features",
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
