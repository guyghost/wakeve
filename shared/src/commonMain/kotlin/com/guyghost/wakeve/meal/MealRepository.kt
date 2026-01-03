package com.guyghost.wakeve.meal

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.DailyMealSchedule
import com.guyghost.wakeve.models.DietaryRestriction
import com.guyghost.wakeve.models.DietaryRestrictionRequest
import com.guyghost.wakeve.models.Meal
import com.guyghost.wakeve.models.MealPlanningSummary
import com.guyghost.wakeve.models.MealRequest
import com.guyghost.wakeve.models.MealStatus
import com.guyghost.wakeve.models.MealType
import com.guyghost.wakeve.models.MealWithRestrictions
import com.guyghost.wakeve.models.ParticipantDietaryRestriction
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Meal Repository - Manages meal and dietary restriction persistence.
 * 
 * Responsibilities:
 * - CRUD operations for meals
 * - CRUD operations for dietary restrictions
 * - Meal queries and filtering
 * - Statistics and aggregations
 * - Map between SQLDelight entities and Kotlin models
 */
class MealRepository(private val db: WakevDb) {
    
    private val mealQueries = db.mealQueries
    private val dietaryRestrictionQueries = db.participantDietaryRestrictionQueries
    
    // ==================== Meal Operations ====================
    
    /**
     * Create a new meal.
     * 
     * @param request Meal creation request
     * @return Created Meal
     */
    fun createMeal(request: MealRequest): Meal {
        val now = getCurrentUtcIsoString()
        val mealId = generateId()
        
        val meal = Meal(
            id = mealId,
            eventId = request.eventId,
            type = request.type,
            name = request.name,
            date = request.date,
            time = request.time,
            location = request.location,
            responsibleParticipantIds = request.responsibleParticipantIds,
            estimatedCost = request.estimatedCost,
            actualCost = request.actualCost,
            servings = request.servings,
            status = request.status,
            notes = request.notes,
            createdAt = now,
            updatedAt = now
        )
        
        mealQueries.insertMeal(
            id = meal.id,
            event_id = meal.eventId,
            type = meal.type.name,
            name = meal.name,
            date = meal.date,
            time = meal.time,
            location = meal.location,
            responsible_participant_ids = meal.responsibleParticipantIds.joinToString(","),
            estimated_cost = meal.estimatedCost,
            actual_cost = meal.actualCost,
            servings = meal.servings.toLong(),
            status = meal.status.name,
            notes = meal.notes,
            created_at = meal.createdAt,
            updated_at = meal.updatedAt
        )
        
        return meal
    }
    
    /**
     * Get meal by ID.
     */
    fun getMealById(mealId: String): Meal? {
        return mealQueries.getMealById(mealId).executeAsOneOrNull()?.toModel()
    }
    
    /**
     * Get all meals for an event.
     */
    fun getMealsByEventId(eventId: String): List<Meal> {
        return mealQueries.getMealsByEventId(eventId).executeAsList().map { it.toModel() }
    }
    
    /**
     * Get meals by type.
     */
    fun getMealsByType(eventId: String, type: MealType): List<Meal> {
        return mealQueries.getMealsByType(eventId, type.name).executeAsList().map { it.toModel() }
    }
    
    /**
     * Get meals by status.
     */
    fun getMealsByStatus(eventId: String, status: MealStatus): List<Meal> {
        return mealQueries.getMealsByStatus(eventId, status.name).executeAsList().map { it.toModel() }
    }
    
    /**
     * Get meals for a specific date.
     */
    fun getMealsByDate(eventId: String, date: String): List<Meal> {
        return mealQueries.getMealsByDate(eventId, date).executeAsList().map { it.toModel() }
    }
    
    /**
     * Get upcoming meals (sorted by date, time).
     */
    fun getUpcomingMeals(eventId: String, limit: Long = 10): List<Meal> {
        return mealQueries.getUpcomingMeals(eventId).executeAsList().take(limit.toInt()).map { it.toModel() }
    }
    
    /**
     * Update an existing meal.
     */
    fun updateMeal(meal: Meal): Meal {
        val updated = meal.copy(updatedAt = getCurrentUtcIsoString())
        
        mealQueries.updateMeal(
            type = updated.type.name,
            name = updated.name,
            date = updated.date,
            time = updated.time,
            location = updated.location,
            responsible_participant_ids = updated.responsibleParticipantIds.joinToString(","),
            estimated_cost = updated.estimatedCost,
            actual_cost = updated.actualCost,
            servings = updated.servings.toLong(),
            status = updated.status.name,
            notes = updated.notes,
            updated_at = updated.updatedAt,
            id = updated.id
        )
        
        return updated
    }
    
    /**
     * Delete a meal.
     */
    fun deleteMeal(mealId: String) {
        mealQueries.deleteMeal(mealId)
    }
    
    /**
     * Delete all meals for an event.
     */
    fun deleteMealsByEventId(eventId: String) {
        mealQueries.deleteMealsByEventId(eventId)
    }
    
    // ==================== Dietary Restrictions ====================
    
    /**
     * Add dietary restriction for a participant.
     */
    fun addDietaryRestriction(request: DietaryRestrictionRequest): ParticipantDietaryRestriction {
        val now = getCurrentUtcIsoString()
        val id = generateId()
        
        val restriction = ParticipantDietaryRestriction(
            id = id,
            participantId = request.participantId,
            eventId = request.eventId,
            restriction = request.restriction,
            notes = request.notes,
            createdAt = now
        )
        
        dietaryRestrictionQueries.insertDietaryRestriction(
            id = restriction.id,
            participant_id = restriction.participantId,
            event_id = restriction.eventId,
            restriction = restriction.restriction.name,
            notes = restriction.notes,
            created_at = restriction.createdAt
        )
        
        return restriction
    }
    
    /**
     * Get all dietary restrictions for an event.
     */
    fun getDietaryRestrictionsByEventId(eventId: String): List<ParticipantDietaryRestriction> {
        return dietaryRestrictionQueries.getRestrictionsForEvent(eventId)
            .executeAsList()
            .map { it.toRestrictionModel() }
    }
    
    /**
     * Get dietary restrictions for a specific participant.
     */
    fun getDietaryRestrictionsByParticipant(eventId: String, participantId: String): List<ParticipantDietaryRestriction> {
        return dietaryRestrictionQueries.getRestrictionsForParticipant(participantId, eventId)
            .executeAsList()
            .map { it.toRestrictionModel() }
    }
    
    /**
     * Get count of participants with each dietary restriction.
     */
    fun getDietaryRestrictionCounts(eventId: String): Map<DietaryRestriction, Int> {
        val counts = dietaryRestrictionQueries.countRestrictionsByType(eventId)
            .executeAsList()
            .associate { 
                DietaryRestriction.valueOf(it.restriction) to it.COUNT.toInt()
            }
        return counts
    }
    
    /**
     * Delete a dietary restriction.
     */
    fun deleteDietaryRestriction(restrictionId: String) {
        dietaryRestrictionQueries.deleteDietaryRestriction(restrictionId)
    }
    
    /**
     * Delete all dietary restrictions for a participant.
     */
    fun deleteDietaryRestrictionsByParticipant(eventId: String, participantId: String) {
        dietaryRestrictionQueries.deleteRestrictionsForParticipant(participantId, eventId)
    }
    
    // ==================== Aggregations & Statistics ====================
    
    /**
     * Get meals grouped by date (daily schedule).
     */
    fun getDailyMealSchedule(eventId: String): List<DailyMealSchedule> {
        val meals = getMealsByEventId(eventId)
        return MealPlanner.groupMealsByDate(meals)
    }
    
    /**
     * Get meal with dietary restrictions to consider.
     */
    fun getMealWithRestrictions(mealId: String): MealWithRestrictions? {
        val meal = getMealById(mealId) ?: return null
        val allRestrictions = getDietaryRestrictionsByEventId(meal.eventId)
        
        val restrictionCounts = allRestrictions
            .groupBy { it.restriction }
            .mapValues { it.value.size }
        
        return MealWithRestrictions(
            meal = meal,
            relevantRestrictions = allRestrictions,
            restrictionCounts = restrictionCounts
        )
    }
    
    /**
     * Calculate meal planning summary.
     */
    fun getMealPlanningSummary(eventId: String): MealPlanningSummary {
        val meals = getMealsByEventId(eventId)
        return MealPlanner.generateMealSummary(meals)
    }
    
    /**
     * Get total estimated cost for all meals.
     */
    fun getTotalEstimatedCost(eventId: String): Long {
        val sum = mealQueries.getTotalEstimatedCost(eventId).executeAsOneOrNull()
        return (sum as? Long) ?: 0L
    }
    
    /**
     * Get total actual cost for completed meals.
     */
    fun getTotalActualCost(eventId: String): Long {
        val sum = mealQueries.getTotalActualCost(eventId).executeAsOneOrNull()
        return (sum as? Long) ?: 0L
    }
    
    /**
     * Count meals by status.
     */
    fun countMealsByStatus(eventId: String): Map<MealStatus, Int> {
        return mealQueries.countMealsByStatus(eventId)
            .executeAsList()
            .associate { 
                MealStatus.valueOf(it.status) to it.COUNT.toInt()
            }
    }
    
    /**
     * Count meals by type.
     */
    fun countMealsByType(eventId: String): Map<MealType, Int> {
        return mealQueries.countMealsByType(eventId)
            .executeAsList()
            .associate { 
                MealType.valueOf(it.type) to it.COUNT.toInt()
            }
    }
    
    // ==================== Helper Methods ====================
    
    private fun getCurrentUtcIsoString(): String {
        return Clock.System.now().toString()
    }
    
    private fun generateId(): String {
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
     * Convert SQL Meal entity to Kotlin model.
     */
    private fun com.guyghost.wakeve.Meal.toModel(): Meal {
        return Meal(
            id = this.id,
            eventId = this.event_id,
            type = MealType.valueOf(this.type),
            name = this.name,
            date = this.date,
            time = this.time,
            location = this.location,
            responsibleParticipantIds = if (this.responsible_participant_ids.isBlank()) {
                emptyList()
            } else {
                this.responsible_participant_ids.split(",")
            },
            estimatedCost = this.estimated_cost,
            actualCost = this.actual_cost,
            servings = this.servings.toInt(),
            status = MealStatus.valueOf(this.status),
            notes = this.notes,
            createdAt = this.created_at,
            updatedAt = this.updated_at
        )
    }
    
    /**
     * Convert SQL DietaryRestriction entity to Kotlin model.
     */
    private fun com.guyghost.wakeve.Participant_dietary_restriction.toRestrictionModel(): ParticipantDietaryRestriction {
        return ParticipantDietaryRestriction(
            id = this.id,
            participantId = this.participant_id,
            eventId = this.event_id,
            restriction = DietaryRestriction.valueOf(this.restriction),
            notes = this.notes,
            createdAt = this.created_at
        )
    }
}
