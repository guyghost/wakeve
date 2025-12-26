package com.guyghost.wakeve.meal

import com.guyghost.wakeve.models.*
import kotlinx.datetime.*
import kotlin.random.Random

/**
 * Service for meal planning and management
 * 
 * This service provides business logic for:
 * - Creating and managing meals
 * - Auto-generating meal plans
 * - Managing dietary restrictions
 * - Calculating meal costs
 * - Assigning responsibilities
 * - Validating meal data
 */
object MealPlanner {
    
    // Default meal times
    private val DEFAULT_BREAKFAST_TIME = "08:00"
    private val DEFAULT_LUNCH_TIME = "12:30"
    private val DEFAULT_DINNER_TIME = "19:30"
    private val DEFAULT_SNACK_TIME = "16:00"
    private val DEFAULT_APERITIF_TIME = "18:30"
    
    /**
     * Generate a random UUID string for cross-platform compatibility
     */
    private fun generateUuid(): String {
        val chars = "0123456789abcdef"
        return buildString(36) {
            repeat(36) { i ->
                when (i) {
                    8, 13, 18, 23 -> append('-')
                    14 -> append('4') // UUID version 4
                    19 -> append(chars[Random.nextInt(4) + 8]) // 8, 9, a, or b
                    else -> append(chars[Random.nextInt(16)])
                }
            }
        }
    }

    /**
     * Get default time for a meal type
     */
    fun getDefaultMealTime(type: MealType): String = when (type) {
        MealType.BREAKFAST -> DEFAULT_BREAKFAST_TIME
        MealType.LUNCH -> DEFAULT_LUNCH_TIME
        MealType.DINNER -> DEFAULT_DINNER_TIME
        MealType.SNACK -> DEFAULT_SNACK_TIME
        MealType.APERITIF -> DEFAULT_APERITIF_TIME
    }

    /**
     * Get default name for a meal type
     */
    fun getDefaultMealName(type: MealType, date: String): String {
        val dayOfWeek = try {
            val localDate = LocalDate.parse(date)
            localDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            ""
        }
        
        return when (type) {
            MealType.BREAKFAST -> "Petit-déjeuner"
            MealType.LUNCH -> "Déjeuner"
            MealType.DINNER -> if (dayOfWeek.isNotEmpty()) "Dîner du $dayOfWeek" else "Dîner"
            MealType.SNACK -> "Goûter"
            MealType.APERITIF -> "Apéritif"
        }
    }

    /**
     * Auto-generate meals for an event
     * 
     * Creates a complete meal plan from start to end date.
     * 
     * @param request Auto-meal plan configuration
     * @return List of generated meals
     */
    fun autoGenerateMeals(request: AutoMealPlanRequest): List<Meal> {
        val meals = mutableListOf<Meal>()
        
        try {
            val startDate = LocalDate.parse(request.startDate)
            val endDate = LocalDate.parse(request.endDate)
            val daysBetween = startDate.daysUntil(endDate) + 1
            
            var currentDate = startDate
            
            for (day in 0 until daysBetween) {
                val dateString = currentDate.toString()
                
                // Generate meals for each requested type
                for (mealType in request.includeMealTypes) {
                    val estimatedCost = request.estimatedCostPerMeal * request.participantCount
                    
                    val meal = Meal(
                        id = generateUuid(),
                        eventId = request.eventId,
                        type = mealType,
                        name = getDefaultMealName(mealType, dateString),
                        date = dateString,
                        time = getDefaultMealTime(mealType),
                        location = null,
                        responsibleParticipantIds = emptyList(),
                        estimatedCost = estimatedCost,
                        actualCost = null,
                        servings = request.participantCount,
                        status = MealStatus.PLANNED,
                        notes = null,
                        createdAt = getCurrentUtcIsoString(),
                        updatedAt = getCurrentUtcIsoString()
                    )
                    
                    meals.add(meal)
                }
                
                currentDate = currentDate.plus(1, DateTimeUnit.DAY)
            }
        } catch (e: Exception) {
            // Invalid date format - return empty list
            return emptyList()
        }
        
        return meals
    }

    /**
     * Calculate total meal cost for an event
     * 
     * @param meals List of meals
     * @param useActual If true, use actual costs where available
     * @return Total cost in cents
     */
    fun calculateTotalMealCost(meals: List<Meal>, useActual: Boolean = false): Long {
        return meals.sumOf { meal ->
            if (useActual && meal.actualCost != null) {
                meal.actualCost
            } else {
                meal.estimatedCost
            }
        }
    }

    /**
     * Calculate cost per person for meals
     * 
     * @param totalCost Total meal cost in cents
     * @param participantCount Number of participants
     * @return Cost per person in cents
     */
    fun calculateCostPerPerson(totalCost: Long, participantCount: Int): Long {
        if (participantCount <= 0) return 0L
        return totalCost / participantCount
    }

    /**
     * Generate meal planning summary
     */
    fun generateMealSummary(meals: List<Meal>): MealPlanningSummary {
        val totalEstimatedCost = meals.sumOf { it.estimatedCost }
        val totalActualCost = meals.mapNotNull { it.actualCost }.sum()
        val completed = meals.count { it.status == MealStatus.COMPLETED }
        val remaining = meals.count { it.status != MealStatus.COMPLETED && it.status != MealStatus.CANCELLED }
        
        val mealsByType = meals.groupBy { it.type }
            .mapValues { it.value.size }
        
        val mealsByStatus = meals.groupBy { it.status }
            .mapValues { it.value.size }
        
        return MealPlanningSummary(
            totalMeals = meals.size,
            totalEstimatedCost = totalEstimatedCost,
            totalActualCost = totalActualCost,
            mealsCompleted = completed,
            mealsRemaining = remaining,
            mealsByType = mealsByType,
            mealsByStatus = mealsByStatus
        )
    }

    /**
     * Group meals by date
     * 
     * @param meals List of meals
     * @return Map of date to meals for that date
     */
    fun groupMealsByDate(meals: List<Meal>): List<DailyMealSchedule> {
        return meals
            .groupBy { it.date }
            .map { (date, mealsForDate) ->
                DailyMealSchedule(
                    date = date,
                    meals = mealsForDate.sortedBy { it.time }
                )
            }
            .sortedBy { it.date }
    }

    /**
     * Validate meal data
     * 
     * @return Validation error message, or null if valid
     */
    fun validateMeal(
        name: String,
        date: String,
        time: String,
        servings: Int,
        estimatedCost: Long
    ): String? {
        if (name.isBlank()) return "Name cannot be empty"
        if (date.isBlank()) return "Date is required"
        if (time.isBlank()) return "Time is required"
        
        // Validate date format
        try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            return "Invalid date format (use YYYY-MM-DD)"
        }
        
        // Validate time format (HH:MM)
        if (!time.matches(Regex("^([01]\\d|2[0-3]):([0-5]\\d)$"))) {
            return "Invalid time format (use HH:MM)"
        }
        
        if (servings <= 0) return "Servings must be positive"
        if (estimatedCost < 0) return "Cost cannot be negative"
        
        return null
    }

    /**
     * Validate dietary restriction
     */
    fun validateDietaryRestriction(
        participantId: String,
        eventId: String,
        restriction: DietaryRestriction
    ): String? {
        if (participantId.isBlank()) return "Participant ID is required"
        if (eventId.isBlank()) return "Event ID is required"
        
        return null
    }

    /**
     * Check if meals cover dietary restrictions
     * 
     * Identifies which restrictions are not being accommodated.
     * 
     * @param meals List of planned meals
     * @param restrictions List of dietary restrictions
     * @return Map of restriction to count of participants with that restriction
     */
    fun analyzeRestrictionCoverage(
        meals: List<Meal>,
        restrictions: List<ParticipantDietaryRestriction>
    ): Map<DietaryRestriction, Int> {
        return restrictions
            .groupBy { it.restriction }
            .mapValues { it.value.size }
    }

    /**
     * Get meals that need assignment
     * 
     * Returns meals with no responsible participants assigned.
     */
    fun getMealsNeedingAssignment(meals: List<Meal>): List<Meal> {
        return meals.filter { it.responsibleParticipantIds.isEmpty() }
    }

    /**
     * Get meals assigned to a participant
     */
    fun getMealsForParticipant(meals: List<Meal>, participantId: String): List<Meal> {
        return meals.filter { participantId in it.responsibleParticipantIds }
    }

    /**
     * Count meals by participant
     * 
     * @return Map of participant ID to number of meals assigned
     */
    fun countMealsByParticipant(meals: List<Meal>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        
        for (meal in meals) {
            for (participantId in meal.responsibleParticipantIds) {
                counts[participantId] = (counts[participantId] ?: 0) + 1
            }
        }
        
        return counts
    }

    /**
     * Suggest meal assignments based on workload balance
     * 
     * Distributes meal responsibilities evenly among participants.
     * 
     * @param meals Meals to assign
     * @param participantIds Available participants
     * @param currentAssignments Current assignments to consider
     * @return Map of meal ID to suggested participant IDs
     */
    fun suggestMealAssignments(
        meals: List<Meal>,
        participantIds: List<String>,
        currentAssignments: Map<String, Int> = emptyMap()
    ): Map<String, List<String>> {
        if (participantIds.isEmpty()) return emptyMap()
        
        val suggestions = mutableMapOf<String, List<String>>()
        val workload = currentAssignments.toMutableMap()
        
        // Initialize workload for all participants
        participantIds.forEach { id ->
            if (!workload.containsKey(id)) {
                workload[id] = 0
            }
        }
        
        // Sort meals by date/time
        val sortedMeals = meals.sortedWith(compareBy({ it.date }, { it.time }))
        
        // Assign meals to participants with least workload
        for (meal in sortedMeals) {
            if (meal.responsibleParticipantIds.isEmpty()) {
                // Find participant with minimum workload
                val assignedParticipant = workload.minByOrNull { it.value }?.key
                
                if (assignedParticipant != null) {
                    suggestions[meal.id] = listOf(assignedParticipant)
                    workload[assignedParticipant] = workload.getValue(assignedParticipant) + 1
                }
            }
        }
        
        return suggestions
    }

    /**
     * Get upcoming meals (not completed or cancelled)
     */
    fun getUpcomingMeals(meals: List<Meal>): List<Meal> {
        return meals.filter { 
            it.status != MealStatus.COMPLETED && it.status != MealStatus.CANCELLED 
        }.sortedWith(compareBy({ it.date }, { it.time }))
    }

    /**
     * Get completed meals
     */
    fun getCompletedMeals(meals: List<Meal>): List<Meal> {
        return meals.filter { it.status == MealStatus.COMPLETED }
            .sortedWith(compareBy({ it.date }, { it.time }))
    }

    /**
     * Calculate meal statistics
     */
    fun calculateMealStats(meals: List<Meal>): Map<String, Any> {
        val total = meals.size
        val byType = meals.groupBy { it.type }.mapValues { it.value.size }
        val byStatus = meals.groupBy { it.status }.mapValues { it.value.size }
        val avgCost = if (total > 0) {
            meals.sumOf { it.estimatedCost } / total
        } else 0L
        
        return mapOf(
            "total" to total,
            "byType" to byType,
            "byStatus" to byStatus,
            "averageCost" to avgCost
        )
    }

    /**
     * Get current UTC timestamp in ISO 8601 format
     */
    fun getCurrentUtcIsoString(): String {
        return Clock.System.now().toString()
    }

    /**
     * Check if two meals overlap in time
     */
    fun mealsOverlap(meal1: Meal, meal2: Meal): Boolean {
        return meal1.date == meal2.date && meal1.time == meal2.time
    }

    /**
     * Find meal conflicts (same date/time)
     */
    fun findMealConflicts(meals: List<Meal>): List<Pair<Meal, Meal>> {
        val conflicts = mutableListOf<Pair<Meal, Meal>>()
        
        for (i in meals.indices) {
            for (j in i + 1 until meals.size) {
                if (mealsOverlap(meals[i], meals[j])) {
                    conflicts.add(Pair(meals[i], meals[j]))
                }
            }
        }
        
        return conflicts
    }
}
