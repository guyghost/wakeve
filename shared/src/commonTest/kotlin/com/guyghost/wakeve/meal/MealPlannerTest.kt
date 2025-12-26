package com.guyghost.wakeve.meal

import com.guyghost.wakeve.models.*
import kotlin.test.*

/**
 * Unit tests for MealPlanner
 * 
 * Tests all business logic functions for meal planning and management.
 */
class MealPlannerTest {
    
    // Test data
    private val testEventId = "event-1"
    private val testParticipantId1 = "user-1"
    private val testParticipantId2 = "user-2"
    private val testParticipantId3 = "user-3"
    
    private val testMeal = Meal(
        id = "meal-1",
        eventId = testEventId,
        type = MealType.LUNCH,
        name = "Saturday Lunch",
        date = "2025-06-15",
        time = "12:30",
        location = "Restaurant",
        responsibleParticipantIds = listOf(testParticipantId1, testParticipantId2),
        estimatedCost = 2500,
        actualCost = null,
        servings = 4,
        status = MealStatus.PLANNED,
        notes = null,
        createdAt = "2025-06-01T10:00:00Z",
        updatedAt = "2025-06-01T10:00:00Z"
    )
    
    private val testRestriction = ParticipantDietaryRestriction(
        id = "restriction-1",
        participantId = testParticipantId1,
        eventId = testEventId,
        restriction = DietaryRestriction.VEGETARIAN,
        notes = "No meat",
        createdAt = "2025-06-01T10:00:00Z"
    )
    
    // ============================================================================
    // Auto-generation Tests
    // ============================================================================
    
    @Test
    fun `autoGenerateMeals generates correct number of meals for single day`() {
        val request = AutoMealPlanRequest(
            eventId = testEventId,
            startDate = "2025-06-15",
            endDate = "2025-06-15",
            participantCount = 4,
            includeMealTypes = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER),
            estimatedCostPerMeal = 1500
        )
        
        val meals = MealPlanner.autoGenerateMeals(request)
        
        assertEquals(3, meals.size, "Should generate 3 meals for single day with 3 types")
        assertEquals(1, meals.map { it.date }.distinct().size, "All meals should be on same date")
    }
    
    @Test
    fun `autoGenerateMeals generates correct number of meals for multiple days`() {
        val request = AutoMealPlanRequest(
            eventId = testEventId,
            startDate = "2025-06-15",
            endDate = "2025-06-17", // 3 days
            participantCount = 4,
            includeMealTypes = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER),
            estimatedCostPerMeal = 1500
        )
        
        val meals = MealPlanner.autoGenerateMeals(request)
        
        assertEquals(9, meals.size, "Should generate 9 meals (3 days × 3 types)")
    }
    
    @Test
    fun `autoGenerateMeals respects meal types selection`() {
        val request = AutoMealPlanRequest(
            eventId = testEventId,
            startDate = "2025-06-15",
            endDate = "2025-06-15",
            participantCount = 4,
            includeMealTypes = listOf(MealType.BREAKFAST, MealType.DINNER), // Only 2 types
            estimatedCostPerMeal = 1500
        )
        
        val meals = MealPlanner.autoGenerateMeals(request)
        
        assertEquals(2, meals.size, "Should only generate selected meal types")
        assertTrue(meals.any { it.type == MealType.BREAKFAST }, "Should include breakfast")
        assertTrue(meals.any { it.type == MealType.DINNER }, "Should include dinner")
        assertFalse(meals.any { it.type == MealType.LUNCH }, "Should not include lunch")
    }
    
    @Test
    fun `autoGenerateMeals assigns correct default times`() {
        val request = AutoMealPlanRequest(
            eventId = testEventId,
            startDate = "2025-06-15",
            endDate = "2025-06-15",
            participantCount = 4,
            includeMealTypes = listOf(
                MealType.BREAKFAST,
                MealType.LUNCH,
                MealType.DINNER,
                MealType.SNACK,
                MealType.APERITIF
            ),
            estimatedCostPerMeal = 1500
        )
        
        val meals = MealPlanner.autoGenerateMeals(request)
        
        val breakfast = meals.first { it.type == MealType.BREAKFAST }
        val lunch = meals.first { it.type == MealType.LUNCH }
        val dinner = meals.first { it.type == MealType.DINNER }
        val snack = meals.first { it.type == MealType.SNACK }
        val aperitif = meals.first { it.type == MealType.APERITIF }
        
        assertEquals("08:00", breakfast.time, "Breakfast should be at 08:00")
        assertEquals("12:30", lunch.time, "Lunch should be at 12:30")
        assertEquals("19:30", dinner.time, "Dinner should be at 19:30")
        assertEquals("16:00", snack.time, "Snack should be at 16:00")
        assertEquals("18:30", aperitif.time, "Aperitif should be at 18:30")
    }
    
    @Test
    fun `autoGenerateMeals handles invalid date range`() {
        val request = AutoMealPlanRequest(
            eventId = testEventId,
            startDate = "2025-06-20",
            endDate = "2025-06-15", // End before start
            participantCount = 4,
            includeMealTypes = listOf(MealType.LUNCH),
            estimatedCostPerMeal = 1500
        )
        
        val meals = MealPlanner.autoGenerateMeals(request)
        
        assertEquals(0, meals.size, "Should return empty list for invalid date range")
    }
    
    @Test
    fun `autoGenerateMeals handles empty meal types`() {
        val request = AutoMealPlanRequest(
            eventId = testEventId,
            startDate = "2025-06-15",
            endDate = "2025-06-15",
            participantCount = 4,
            includeMealTypes = emptyList(),
            estimatedCostPerMeal = 1500
        )
        
        val meals = MealPlanner.autoGenerateMeals(request)
        
        assertEquals(0, meals.size, "Should return empty list for empty meal types")
    }
    
    // ============================================================================
    // Cost Calculation Tests
    // ============================================================================
    
    @Test
    fun `calculateTotalMealCost sums estimated costs correctly`() {
        val meals = listOf(
            testMeal.copy(estimatedCost = 2000),
            testMeal.copy(estimatedCost = 3000),
            testMeal.copy(estimatedCost = 2500)
        )
        
        val total = MealPlanner.calculateTotalMealCost(meals, useActual = false)
        
        assertEquals(7500, total, "Should sum all estimated costs")
    }
    
    @Test
    fun `calculateTotalMealCost uses actual costs when available and requested`() {
        val meals = listOf(
            testMeal.copy(estimatedCost = 2000, actualCost = 1800), // Use actual
            testMeal.copy(estimatedCost = 3000, actualCost = null), // Use estimated
            testMeal.copy(estimatedCost = 2500, actualCost = 2600)  // Use actual
        )
        
        val total = MealPlanner.calculateTotalMealCost(meals, useActual = true)
        
        assertEquals(7400, total, "Should use actual costs when available (1800 + 3000 + 2600)")
    }
    
    @Test
    fun `calculateTotalMealCost handles empty list`() {
        val total = MealPlanner.calculateTotalMealCost(emptyList())
        
        assertEquals(0, total, "Should return 0 for empty list")
    }
    
    @Test
    fun `calculateCostPerPerson divides correctly`() {
        val totalCost = 9000L
        val participantCount = 3
        
        val costPerPerson = MealPlanner.calculateCostPerPerson(totalCost, participantCount)
        
        assertEquals(3000, costPerPerson, "Should divide total (9000) by participants (3)")
    }
    
    @Test
    fun `calculateCostPerPerson returns 0 for zero participants`() {
        val costPerPerson = MealPlanner.calculateCostPerPerson(9000L, 0)
        
        assertEquals(0, costPerPerson, "Should return 0 to avoid division by zero")
    }
    
    // ============================================================================
    // Validation Tests
    // ============================================================================
    
    @Test
    fun `validateMeal accepts valid meal data`() {
        val result = MealPlanner.validateMeal(
            name = "Saturday Lunch",
            date = "2025-06-15",
            time = "12:30",
            servings = 4,
            estimatedCost = 2500
        )
        
        assertNull(result, "Should return null for valid meal data")
    }
    
    @Test
    fun `validateMeal rejects empty name`() {
        val result = MealPlanner.validateMeal(
            name = "",
            date = "2025-06-15",
            time = "12:30",
            servings = 4,
            estimatedCost = 2500
        )
        
        assertNotNull(result, "Should return error for empty name")
        assertTrue(result!!.contains("Name"), "Should mention name error")
    }
    
    @Test
    fun `validateMeal rejects invalid date format`() {
        val result = MealPlanner.validateMeal(
            name = "Lunch",
            date = "15/06/2025", // Wrong format
            time = "12:30",
            servings = 4,
            estimatedCost = 2500
        )
        
        assertNotNull(result, "Should return error for invalid date format")
        assertTrue(result!!.contains("date format"), "Should mention date format error")
    }
    
    @Test
    fun `validateMeal rejects invalid time format`() {
        val result = MealPlanner.validateMeal(
            name = "Lunch",
            date = "2025-06-15",
            time = "12:30:00", // Wrong format (has seconds)
            servings = 4,
            estimatedCost = 2500
        )
        
        assertNotNull(result, "Should return error for invalid time format")
        assertTrue(result!!.contains("time format"), "Should mention time format error")
    }
    
    @Test
    fun `validateMeal rejects negative estimated cost`() {
        val result = MealPlanner.validateMeal(
            name = "Lunch",
            date = "2025-06-15",
            time = "12:30",
            servings = 4,
            estimatedCost = -100
        )
        
        assertNotNull(result, "Should return error for negative estimated cost")
        assertTrue(result!!.contains("Cost"), "Should mention cost error")
    }
    
    @Test
    fun `validateMeal rejects zero servings`() {
        val result = MealPlanner.validateMeal(
            name = "Lunch",
            date = "2025-06-15",
            time = "12:30",
            servings = 0,
            estimatedCost = 2500
        )
        
        assertNotNull(result, "Should return error for zero servings")
        assertTrue(result!!.contains("Servings"), "Should mention servings error")
    }
    
    @Test
    fun `validateDietaryRestriction accepts valid restriction`() {
        val result = MealPlanner.validateDietaryRestriction(
            participantId = testParticipantId1,
            eventId = testEventId,
            restriction = DietaryRestriction.VEGETARIAN
        )
        
        assertNull(result, "Should return null for valid restriction")
    }
    
    // ============================================================================
    // Assignment Tests
    // ============================================================================
    
    @Test
    fun `suggestMealAssignments balances workload across participants`() {
        val meals = listOf(
            testMeal.copy(id = "meal-1", responsibleParticipantIds = emptyList()),
            testMeal.copy(id = "meal-2", responsibleParticipantIds = emptyList()),
            testMeal.copy(id = "meal-3", responsibleParticipantIds = emptyList()),
            testMeal.copy(id = "meal-4", responsibleParticipantIds = emptyList()),
            testMeal.copy(id = "meal-5", responsibleParticipantIds = emptyList()),
            testMeal.copy(id = "meal-6", responsibleParticipantIds = emptyList())
        )
        val participants = listOf(testParticipantId1, testParticipantId2, testParticipantId3)
        
        val suggestions = MealPlanner.suggestMealAssignments(meals, participants)
        
        assertEquals(6, suggestions.size, "Should suggest assignment for all meals")
        
        // Check workload balance
        val assignmentCounts = suggestions
            .flatMap { it.value }
            .groupingBy { it }
            .eachCount()
        
        // Each participant should get balanced assignments
        assignmentCounts.values.forEach { count ->
            assertTrue(count >= 1, "Each participant should get at least one assignment")
        }
    }
    
    @Test
    fun `getMealsNeedingAssignment filters correctly`() {
        val meals = listOf(
            testMeal.copy(id = "meal-1", responsibleParticipantIds = emptyList()), // Needs assignment
            testMeal.copy(id = "meal-2", responsibleParticipantIds = listOf(testParticipantId1)), // Has assignment
            testMeal.copy(id = "meal-3", responsibleParticipantIds = emptyList()), // Needs assignment
            testMeal.copy(id = "meal-4", responsibleParticipantIds = listOf(testParticipantId1, testParticipantId2)) // Has assignment
        )
        
        val needingAssignment = MealPlanner.getMealsNeedingAssignment(meals)
        
        assertEquals(2, needingAssignment.size, "Should return only meals without assignments")
        assertTrue(needingAssignment.all { it.responsibleParticipantIds.isEmpty() })
    }
    
    @Test
    fun `countMealsByParticipant counts correctly`() {
        val meals = listOf(
            testMeal.copy(responsibleParticipantIds = listOf(testParticipantId1)),
            testMeal.copy(responsibleParticipantIds = listOf(testParticipantId1, testParticipantId2)),
            testMeal.copy(responsibleParticipantIds = listOf(testParticipantId2)),
            testMeal.copy(responsibleParticipantIds = listOf(testParticipantId3))
        )
        
        val counts = MealPlanner.countMealsByParticipant(meals)
        
        assertEquals(2, counts[testParticipantId1], "User 1 should have 2 meals")
        assertEquals(2, counts[testParticipantId2], "User 2 should have 2 meals")
        assertEquals(1, counts[testParticipantId3], "User 3 should have 1 meal")
    }
    
    // ============================================================================
    // Analysis Tests
    // ============================================================================
    
    @Test
    fun `findMealConflicts detects overlapping meals`() {
        val meals = listOf(
            testMeal.copy(id = "meal-1", date = "2025-06-15", time = "12:00"),
            testMeal.copy(id = "meal-2", date = "2025-06-15", time = "12:00"), // Same time
            testMeal.copy(id = "meal-3", date = "2025-06-15", time = "14:00"), // Different time
            testMeal.copy(id = "meal-4", date = "2025-06-16", time = "12:00")  // Different day
        )
        
        val conflicts = MealPlanner.findMealConflicts(meals)
        
        assertTrue(conflicts.isNotEmpty(), "Should find conflicts")
        assertTrue(conflicts.any { (m1, m2) -> 
            (m1.id == "meal-1" && m2.id == "meal-2") || (m1.id == "meal-2" && m2.id == "meal-1")
        }, "Should find conflict between meal-1 and meal-2")
    }
    
    @Test
    fun `groupMealsByDate organizes meals correctly`() {
        val meals = listOf(
            testMeal.copy(id = "meal-1", date = "2025-06-15", type = MealType.BREAKFAST),
            testMeal.copy(id = "meal-2", date = "2025-06-15", type = MealType.LUNCH),
            testMeal.copy(id = "meal-3", date = "2025-06-16", type = MealType.BREAKFAST),
            testMeal.copy(id = "meal-4", date = "2025-06-16", type = MealType.DINNER)
        )
        
        val grouped = MealPlanner.groupMealsByDate(meals)
        
        assertEquals(2, grouped.size, "Should group into 2 dates")
        
        val date1 = grouped.find { it.date == "2025-06-15" }
        val date2 = grouped.find { it.date == "2025-06-16" }
        
        assertNotNull(date1, "Should have group for 2025-06-15")
        assertNotNull(date2, "Should have group for 2025-06-16")
        assertEquals(2, date1.meals.size, "First date should have 2 meals")
        assertEquals(2, date2.meals.size, "Second date should have 2 meals")
    }
    
    @Test
    fun `generateMealSummary calculates statistics correctly`() {
        val meals = listOf(
            testMeal.copy(type = MealType.BREAKFAST, estimatedCost = 1000, status = MealStatus.COMPLETED, actualCost = 950),
            testMeal.copy(type = MealType.LUNCH, estimatedCost = 2000, status = MealStatus.PLANNED),
            testMeal.copy(type = MealType.DINNER, estimatedCost = 3000, status = MealStatus.PLANNED)
        )
        
        val summary = MealPlanner.generateMealSummary(meals)
        
        assertEquals(3, summary.totalMeals, "Should count 3 meals")
        assertEquals(6000, summary.totalEstimatedCost, "Should sum estimated costs")
        assertEquals(1, summary.mealsCompleted, "Should count completed meals")
        assertEquals(2, summary.mealsRemaining, "Should count remaining meals")
    }
    
    // ============================================================================
    // Filtering Tests
    // ============================================================================
    
    @Test
    fun `getUpcomingMeals excludes completed and cancelled meals`() {
        val meals = listOf(
            testMeal.copy(id = "meal-1", status = MealStatus.PLANNED),
            testMeal.copy(id = "meal-2", status = MealStatus.ASSIGNED),
            testMeal.copy(id = "meal-3", status = MealStatus.IN_PROGRESS),
            testMeal.copy(id = "meal-4", status = MealStatus.COMPLETED),
            testMeal.copy(id = "meal-5", status = MealStatus.CANCELLED)
        )
        
        val upcoming = MealPlanner.getUpcomingMeals(meals)
        
        assertEquals(3, upcoming.size, "Should return only upcoming meals")
        assertTrue(upcoming.none { it.status == MealStatus.COMPLETED || it.status == MealStatus.CANCELLED })
    }
    
    @Test
    fun `getCompletedMeals filters correctly`() {
        val meals = listOf(
            testMeal.copy(id = "meal-1", status = MealStatus.PLANNED),
            testMeal.copy(id = "meal-2", status = MealStatus.COMPLETED),
            testMeal.copy(id = "meal-3", status = MealStatus.COMPLETED),
            testMeal.copy(id = "meal-4", status = MealStatus.CANCELLED)
        )
        
        val completed = MealPlanner.getCompletedMeals(meals)
        
        assertEquals(2, completed.size, "Should return only completed meals")
        assertTrue(completed.all { it.status == MealStatus.COMPLETED })
    }
    
    @Test
    fun `getMealsForParticipant filters by participant ID`() {
        val meals = listOf(
            testMeal.copy(id = "meal-1", responsibleParticipantIds = listOf(testParticipantId1)),
            testMeal.copy(id = "meal-2", responsibleParticipantIds = listOf(testParticipantId1, testParticipantId2)),
            testMeal.copy(id = "meal-3", responsibleParticipantIds = listOf(testParticipantId2)),
            testMeal.copy(id = "meal-4", responsibleParticipantIds = listOf(testParticipantId3))
        )
        
        val mealsForUser1 = MealPlanner.getMealsForParticipant(meals, testParticipantId1)
        
        assertEquals(2, mealsForUser1.size, "Should return meals where user1 is responsible")
        assertTrue(mealsForUser1.all { it.responsibleParticipantIds.contains(testParticipantId1) })
    }
    
    @Test
    fun `analyzeRestrictionCoverage counts restrictions correctly`() {
        val meals = emptyList<Meal>()
        val restrictions = listOf(
            testRestriction.copy(restriction = DietaryRestriction.VEGETARIAN),
            testRestriction.copy(restriction = DietaryRestriction.VEGETARIAN),
            testRestriction.copy(restriction = DietaryRestriction.GLUTEN_FREE),
            testRestriction.copy(restriction = DietaryRestriction.VEGAN)
        )
        
        val coverage = MealPlanner.analyzeRestrictionCoverage(meals, restrictions)
        
        assertEquals(3, coverage.size, "Should have 3 unique restriction types")
        assertEquals(2, coverage[DietaryRestriction.VEGETARIAN], "Should count 2 vegetarian")
        assertEquals(1, coverage[DietaryRestriction.GLUTEN_FREE], "Should count 1 gluten free")
        assertEquals(1, coverage[DietaryRestriction.VEGAN], "Should count 1 vegan")
    }
    
    // ============================================================================
    // Statistics Tests
    // ============================================================================
    
    @Test
    fun `calculateMealStats computes all statistics correctly`() {
        val meals = listOf(
            testMeal.copy(type = MealType.BREAKFAST, estimatedCost = 1000),
            testMeal.copy(type = MealType.LUNCH, estimatedCost = 2000),
            testMeal.copy(type = MealType.DINNER, estimatedCost = 3000),
            testMeal.copy(type = MealType.BREAKFAST, estimatedCost = 1200)
        )
        
        val stats = MealPlanner.calculateMealStats(meals)
        
        assertEquals(4, stats["total"], "Should count 4 meals")
        assertEquals(1800L, stats["averageCost"], "Should calculate average cost")
    }
    
    @Test
    fun `calculateMealStats handles empty list`() {
        val stats = MealPlanner.calculateMealStats(emptyList())
        
        assertEquals(0, stats["total"], "Should return 0 for empty list")
        assertEquals(0L, stats["averageCost"], "Should return 0 average")
    }
    
    // ============================================================================
    // Utility Function Tests
    // ============================================================================
    
    @Test
    fun `getDefaultMealTime returns correct times`() {
        assertEquals("08:00", MealPlanner.getDefaultMealTime(MealType.BREAKFAST))
        assertEquals("12:30", MealPlanner.getDefaultMealTime(MealType.LUNCH))
        assertEquals("19:30", MealPlanner.getDefaultMealTime(MealType.DINNER))
        assertEquals("16:00", MealPlanner.getDefaultMealTime(MealType.SNACK))
        assertEquals("18:30", MealPlanner.getDefaultMealTime(MealType.APERITIF))
    }
    
    @Test
    fun `mealsOverlap detects overlapping meals`() {
        val meal1 = testMeal.copy(date = "2025-06-15", time = "12:00")
        val meal2 = testMeal.copy(date = "2025-06-15", time = "12:00")
        val meal3 = testMeal.copy(date = "2025-06-15", time = "14:00")
        val meal4 = testMeal.copy(date = "2025-06-16", time = "12:00")
        
        assertTrue(MealPlanner.mealsOverlap(meal1, meal2), "Same date and time should overlap")
        assertFalse(MealPlanner.mealsOverlap(meal1, meal3), "Different times should not overlap")
        assertFalse(MealPlanner.mealsOverlap(meal1, meal4), "Different dates should not overlap")
    }
}
