package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Type of meal
 */
@Serializable
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    APERITIF
}

/**
 * Status of meal planning
 */
@Serializable
enum class MealStatus {
    PLANNED,      // Meal is planned but not prepared
    ASSIGNED,     // Someone is responsible for this meal
    IN_PROGRESS,  // Meal preparation in progress
    COMPLETED,    // Meal has been served
    CANCELLED     // Meal cancelled
}

/**
 * Common dietary restrictions
 */
@Serializable
enum class DietaryRestriction {
    VEGETARIAN,
    VEGAN,
    GLUTEN_FREE,
    LACTOSE_INTOLERANT,
    NUT_ALLERGY,
    SHELLFISH_ALLERGY,
    KOSHER,
    HALAL,
    DIABETIC,
    OTHER
}

/**
 * Meal for an event
 * 
 * Represents a planned meal with assignments and dietary considerations.
 * 
 * @property id Unique identifier
 * @property eventId Event this meal belongs to
 * @property type Type of meal
 * @property name Name/description of the meal (e.g., "Barbecue du samedi soir")
 * @property date Date of the meal (ISO 8601 date)
 * @property time Time of the meal (HH:MM format)
 * @property location Where the meal takes place (optional)
 * @property responsibleParticipantIds List of participant IDs responsible for this meal
 * @property estimatedCost Estimated cost in cents
 * @property actualCost Actual cost in cents (null if not yet incurred)
 * @property servings Number of people to serve
 * @property status Current status of meal planning
 * @property notes Additional notes or menu details
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
@Serializable
data class Meal(
    val id: String,
    val eventId: String,
    val type: MealType,
    val name: String,
    val date: String,  // ISO 8601 date (e.g., "2025-12-20")
    val time: String,  // HH:MM format (e.g., "19:00")
    val location: String? = null,
    val responsibleParticipantIds: List<String>,
    val estimatedCost: Long,  // In cents
    val actualCost: Long? = null,  // In cents
    val servings: Int,
    val status: MealStatus,
    val notes: String? = null,
    val createdAt: String,  // ISO 8601 UTC timestamp
    val updatedAt: String   // ISO 8601 UTC timestamp
)

/**
 * Participant dietary restrictions mapping
 * 
 * Associates dietary restrictions with participants.
 * 
 * @property id Unique identifier
 * @property participantId Participant ID
 * @property eventId Event ID (for scoping)
 * @property restriction Type of dietary restriction
 * @property notes Additional details about the restriction
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 */
@Serializable
data class ParticipantDietaryRestriction(
    val id: String,
    val participantId: String,
    val eventId: String,
    val restriction: DietaryRestriction,
    val notes: String? = null,
    val createdAt: String  // ISO 8601 UTC timestamp
)

/**
 * Meal with associated restrictions
 * 
 * Combines meal details with relevant dietary restrictions to consider.
 * 
 * @property meal The meal details
 * @property relevantRestrictions Dietary restrictions of participants attending
 * @property restrictionCounts Count of each type of restriction
 */
@Serializable
data class MealWithRestrictions(
    val meal: Meal,
    val relevantRestrictions: List<ParticipantDietaryRestriction>,
    val restrictionCounts: Map<DietaryRestriction, Int>
)

/**
 * Meal schedule for an event
 * 
 * Groups meals by day for easy visualization.
 * 
 * @property date The date
 * @property meals List of meals for this date
 */
@Serializable
data class DailyMealSchedule(
    val date: String,  // ISO 8601 date
    val meals: List<Meal>
)

/**
 * Meal planning summary
 * 
 * @property totalMeals Total number of planned meals
 * @property totalEstimatedCost Total estimated cost in cents
 * @property totalActualCost Total actual cost in cents (completed meals only)
 * @property mealsCompleted Number of completed meals
 * @property mealsRemaining Number of meals not yet completed
 * @property mealsByType Count of meals by type
 * @property mealsByStatus Count of meals by status
 */
@Serializable
data class MealPlanningSummary(
    val totalMeals: Int,
    val totalEstimatedCost: Long,  // In cents
    val totalActualCost: Long,     // In cents
    val mealsCompleted: Int,
    val mealsRemaining: Int,
    val mealsByType: Map<MealType, Int>,
    val mealsByStatus: Map<MealStatus, Int>
)

/**
 * Request to create or update a meal
 */
@Serializable
data class MealRequest(
    val eventId: String,
    val type: MealType,
    val name: String,
    val date: String,
    val time: String,
    val location: String? = null,
    val responsibleParticipantIds: List<String>,
    val estimatedCost: Long,
    val actualCost: Long? = null,
    val servings: Int,
    val status: MealStatus = MealStatus.PLANNED,
    val notes: String? = null
)

/**
 * Request to add dietary restriction for a participant
 */
@Serializable
data class DietaryRestrictionRequest(
    val participantId: String,
    val eventId: String,
    val restriction: DietaryRestriction,
    val notes: String? = null
)

/**
 * Auto-generated meal plan request
 * 
 * Used to automatically generate meals for an event.
 * 
 * @property eventId Event ID
 * @property startDate Start date (ISO 8601)
 * @property endDate End date (ISO 8601)
 * @property participantCount Number of participants
 * @property includeMealTypes Which meal types to plan
 * @property estimatedCostPerMeal Average cost per meal per person in cents
 */
@Serializable
data class AutoMealPlanRequest(
    val eventId: String,
    val startDate: String,
    val endDate: String,
    val participantCount: Int,
    val includeMealTypes: List<MealType>,
    val estimatedCostPerMeal: Long  // In cents per person
)
