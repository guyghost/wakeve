package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.userId
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.meal.MealPlanner
import com.guyghost.wakeve.meal.MealRepository
import com.guyghost.wakeve.models.AutoMealPlanRequest
import com.guyghost.wakeve.models.DietaryRestrictionRequest
import com.guyghost.wakeve.models.MealRequest
import com.guyghost.wakeve.moderation.ModerationPolicy
import com.guyghost.wakeve.repository.DatabaseEventRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.time.Instant

/**
 * Meal API Routes
 * 
 * Provides RESTful endpoints for meal planning and management including:
 * - Meal CRUD operations
 * - Daily meal schedules
 * - Meal status tracking
 * - Auto-generation of meal plans
 * - Dietary restriction management
 * - Meal statistics and summaries
 */
fun io.ktor.server.routing.Route.mealRoutes(
    repository: MealRepository,
    eventRepository: DatabaseEventRepository,
    database: WakeveDb,
    moderationPolicy: ModerationPolicy = ModerationPolicy()
) {
    route("/events/{eventId}/meals") {
        
        // GET /api/events/{eventId}/meals - Get all meals for event
        get {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedMealUserId() ?: return@get
                if (!hasMealReadAccess(eventRepository, database, eventId, userId)) {
                    return@get call.respond(HttpStatusCode.Forbidden, mealAccessDenied(eventId, userId, "read_meals"))
                }

                val meals = repository.getMealsByEventId(eventId)
                call.respond(HttpStatusCode.OK, meals)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to mealListFailureMessage())
                )
            }
        }
        
        // POST /api/events/{eventId}/meals - Create a meal
        post {
            try {
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedMealUserId() ?: return@post
                if (!isMealOrganizer(eventRepository, eventId, userId)) {
                    return@post call.respond(HttpStatusCode.Forbidden, mealAccessDenied(eventId, userId, "create_meal"))
                }

                val request = call.receive<MealRequest>()
                if (call.rejectRejectedModeratedText(
                        moderationPolicy,
                        mealModeratedTextFields(request)
                    )
                ) {
                    return@post
                }
                
                // Ensure eventId matches
                if (request.eventId != eventId) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Event ID mismatch")
                    )
                }
                
                // Validate meal
                val validationError = MealPlanner.validateMeal(
                    name = request.name,
                    date = request.date,
                    time = request.time,
                    servings = request.servings,
                    estimatedCost = request.estimatedCost
                )
                
                if (validationError != null) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to validationError)
                    )
                }

                val meal = repository.createMeal(request)
                call.respond(HttpStatusCode.Created, meal)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to mealCreateFailureMessage())
                )
            }
        }
        
        // GET /api/events/{eventId}/meals/{mealId} - Get meal by ID
        get("/{mealId}") {
            try {
                val mealId = call.parameters["mealId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Meal ID required")
                )
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedMealUserId() ?: return@get
                if (!hasMealReadAccess(eventRepository, database, eventId, userId)) {
                    return@get call.respond(HttpStatusCode.Forbidden, mealAccessDenied(eventId, userId, "read_meal"))
                }

                val meal = repository.getMealById(mealId)
                if (meal != null) {
                    if (meal.eventId != eventId) {
                        return@get call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to "Meal not found")
                        )
                    }
                    call.respond(HttpStatusCode.OK, meal)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Meal not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to mealDetailFailureMessage())
                )
            }
        }
        
        // PUT /api/events/{eventId}/meals/{mealId} - Update meal
        put("/{mealId}") {
            try {
                val mealId = call.parameters["mealId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Meal ID required")
                )
                val eventId = call.parameters["eventId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedMealUserId() ?: return@put
                if (!isMealOrganizer(eventRepository, eventId, userId)) {
                    return@put call.respond(HttpStatusCode.Forbidden, mealAccessDenied(eventId, userId, "update_meal"))
                }

                val existing = repository.getMealById(mealId)
                if (existing == null) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Meal not found")
                    )
                }
                if (existing.eventId != eventId) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Meal not found")
                    )
                }

                val request = call.receive<MealRequest>()
                if (call.rejectRejectedModeratedText(
                        moderationPolicy,
                        mealModeratedTextFields(request)
                    )
                ) {
                    return@put
                }
                
                // Validate meal
                val validationError = MealPlanner.validateMeal(
                    name = request.name,
                    date = request.date,
                    time = request.time,
                    servings = request.servings,
                    estimatedCost = request.estimatedCost
                )
                
                if (validationError != null) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to validationError)
                    )
                }

                val updated = existing.copy(
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
                    notes = request.notes
                )

                val savedMeal = repository.updateMeal(updated)
                call.respond(HttpStatusCode.OK, savedMeal)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to mealUpdateFailureMessage())
                )
            }
        }
        
        // DELETE /api/events/{eventId}/meals/{mealId} - Delete meal
        delete("/{mealId}") {
            try {
                val mealId = call.parameters["mealId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Meal ID required")
                )
                val eventId = call.parameters["eventId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedMealUserId() ?: return@delete
                if (!isMealOrganizer(eventRepository, eventId, userId)) {
                    return@delete call.respond(HttpStatusCode.Forbidden, mealAccessDenied(eventId, userId, "delete_meal"))
                }

                val existing = repository.getMealById(mealId)
                if (existing == null) {
                    return@delete call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Meal not found")
                    )
                }
                if (existing.eventId != eventId) {
                    return@delete call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Meal not found")
                    )
                }

                repository.deleteMeal(mealId)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to mealDeleteFailureMessage())
                )
            }
        }
        
        // GET /api/events/{eventId}/meals/schedule - Get daily meal schedule
        get("/schedule") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedMealUserId() ?: return@get
                if (!hasMealReadAccess(eventRepository, database, eventId, userId)) {
                    return@get call.respond(HttpStatusCode.Forbidden, mealAccessDenied(eventId, userId, "read_meal_schedule"))
                }

                val schedule = repository.getDailyMealSchedule(eventId)
                call.respond(HttpStatusCode.OK, schedule)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to mealScheduleFailureMessage())
                )
            }
        }
        
        // GET /api/events/{eventId}/meals/summary - Get meal planning summary
        get("/summary") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedMealUserId() ?: return@get
                if (!hasMealReadAccess(eventRepository, database, eventId, userId)) {
                    return@get call.respond(HttpStatusCode.Forbidden, mealAccessDenied(eventId, userId, "read_meal_summary"))
                }

                val summary = repository.getMealPlanningSummary(eventId)
                call.respond(HttpStatusCode.OK, summary)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to mealSummaryFailureMessage())
                )
            }
        }
        
        // GET /api/events/{eventId}/meals/upcoming - Get upcoming meals
        get("/upcoming") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedMealUserId() ?: return@get
                if (!hasMealReadAccess(eventRepository, database, eventId, userId)) {
                    return@get call.respond(HttpStatusCode.Forbidden, mealAccessDenied(eventId, userId, "read_upcoming_meals"))
                }
                
                val limit = call.request.queryParameters["limit"]?.toLongOrNull() ?: 10L

                val meals = repository.getUpcomingMeals(eventId, limit)
                call.respond(HttpStatusCode.OK, meals)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to upcomingMealsFailureMessage())
                )
            }
        }
        
        // POST /api/events/{eventId}/meals/auto-generate - Auto-generate meal plan
        post("/auto-generate") {
            try {
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedMealUserId() ?: return@post
                if (!isMealOrganizer(eventRepository, eventId, userId)) {
                    return@post call.respond(HttpStatusCode.Forbidden, mealAccessDenied(eventId, userId, "auto_generate_meals"))
                }

                val request = call.receive<AutoMealPlanRequest>()
                
                // Ensure eventId matches
                if (request.eventId != eventId) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Event ID mismatch")
                    )
                }

                // Generate meals
                val generatedMeals = MealPlanner.autoGenerateMeals(request)
                
                // Save each generated meal
                val savedMeals = generatedMeals.map { meal ->
                    val mealRequest = MealRequest(
                        eventId = meal.eventId,
                        type = meal.type,
                        name = meal.name,
                        date = meal.date,
                        time = meal.time,
                        location = meal.location,
                        responsibleParticipantIds = meal.responsibleParticipantIds,
                        estimatedCost = meal.estimatedCost,
                        actualCost = meal.actualCost,
                        servings = meal.servings,
                        status = meal.status,
                        notes = meal.notes
                    )
                    repository.createMeal(mealRequest)
                }

                call.respond(HttpStatusCode.Created, savedMeals)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to mealAutoGenerateFailureMessage())
                )
            }
        }
    }
    
    // Dietary Restrictions Routes
    route("/events/{eventId}/dietary-restrictions") {
        
        // GET /api/events/{eventId}/dietary-restrictions - Get all restrictions for event
        get {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedMealUserId() ?: return@get
                if (!hasMealReadAccess(eventRepository, database, eventId, userId)) {
                    return@get call.respond(HttpStatusCode.Forbidden, mealAccessDenied(eventId, userId, "read_dietary_restrictions"))
                }

                val restrictions = repository.getDietaryRestrictionsByEventId(eventId)
                call.respond(HttpStatusCode.OK, restrictions)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to dietaryRestrictionsListFailureMessage())
                )
            }
        }
        
        // POST /api/events/{eventId}/dietary-restrictions - Add dietary restriction
        post {
            try {
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedMealUserId() ?: return@post

                val request = call.receive<DietaryRestrictionRequest>()
                
                // Ensure eventId matches
                if (request.eventId != eventId) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Event ID mismatch")
                    )
                }
                if (!canManageDietaryRestriction(eventRepository, database, eventId, userId, request.participantId)) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mealAccessDenied(eventId, userId, "create_dietary_restriction")
                    )
                }

                val restriction = repository.addDietaryRestriction(request)
                call.respond(HttpStatusCode.Created, restriction)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to dietaryRestrictionCreateFailureMessage())
                )
            }
        }
        
        // GET /api/events/{eventId}/dietary-restrictions/participant/{participantId} - Get restrictions for participant
        get("/participant/{participantId}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedMealUserId() ?: return@get
                
                val participantId = call.parameters["participantId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Participant ID required")
                )
                if (!canManageDietaryRestriction(eventRepository, database, eventId, userId, participantId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mealAccessDenied(eventId, userId, "read_participant_dietary_restrictions")
                    )
                }

                val restrictions = repository.getDietaryRestrictionsByParticipant(eventId, participantId)
                call.respond(HttpStatusCode.OK, restrictions)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to participantDietaryRestrictionsFailureMessage())
                )
            }
        }
        
        // GET /api/events/{eventId}/dietary-restrictions/counts - Get restriction counts
        get("/counts") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedMealUserId() ?: return@get
                if (!hasMealReadAccess(eventRepository, database, eventId, userId)) {
                    return@get call.respond(HttpStatusCode.Forbidden, mealAccessDenied(eventId, userId, "read_dietary_counts"))
                }

                val counts = repository.getDietaryRestrictionCounts(eventId)
                call.respond(HttpStatusCode.OK, counts)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to dietaryRestrictionCountsFailureMessage())
                )
            }
        }
        
        // DELETE /api/events/{eventId}/dietary-restrictions/{restrictionId} - Delete restriction
        delete("/{restrictionId}") {
            try {
                val restrictionId = call.parameters["restrictionId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Restriction ID required")
                )
                val eventId = call.parameters["eventId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedMealUserId() ?: return@delete
                val restriction = repository.getDietaryRestrictionById(restrictionId)
                    ?: return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Restriction not found"))
                if (restriction.eventId != eventId) {
                    return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Restriction not found"))
                }
                if (!canManageDietaryRestriction(eventRepository, database, eventId, userId, restriction.participantId)) {
                    return@delete call.respond(
                        HttpStatusCode.Forbidden,
                        mealAccessDenied(eventId, userId, "delete_dietary_restriction")
                    )
                }

                repository.deleteDietaryRestriction(restrictionId)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to dietaryRestrictionDeleteFailureMessage())
                )
            }
        }
    }
}

/**
 * Helper function to get current ISO timestamp
 */
private fun getCurrentIsoTimestamp(): String {
    return Instant.now().toString()
}

private fun mealModeratedTextFields(request: MealRequest): List<ModeratedTextField> =
    listOf(
        ModeratedTextField("name", request.name),
        ModeratedTextField("location", request.location),
        ModeratedTextField("notes", request.notes)
    )

private suspend fun io.ktor.server.application.ApplicationCall.authenticatedMealUserId(): String? {
    val principal = principal<JWTPrincipal>()
    if (principal == null) {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
        return null
    }
    return principal.userId
}

private fun hasMealReadAccess(
    eventRepository: DatabaseEventRepository,
    database: WakeveDb,
    eventId: String,
    userId: String
): Boolean {
    return isMealOrganizer(eventRepository, eventId, userId) ||
        database.participantQueries.selectByEventIdAndUserId(eventId, userId).executeAsOneOrNull() != null
}

private fun isMealOrganizer(
    eventRepository: DatabaseEventRepository,
    eventId: String,
    userId: String
): Boolean = eventRepository.getEvent(eventId)?.organizerId == userId

private fun canManageDietaryRestriction(
    eventRepository: DatabaseEventRepository,
    database: WakeveDb,
    eventId: String,
    userId: String,
    participantId: String
): Boolean {
    return (userId == participantId || isMealOrganizer(eventRepository, eventId, userId)) &&
        hasMealReadAccess(eventRepository, database, eventId, participantId)
}

internal fun mealListFailureMessage(): String =
    "Failed to fetch meals. Please try again."

internal fun mealCreateFailureMessage(): String =
    "Failed to create the meal. Please try again."

internal fun mealDetailFailureMessage(): String =
    "Failed to fetch meal details. Please try again."

internal fun mealUpdateFailureMessage(): String =
    "Failed to update the meal. Please try again."

internal fun mealDeleteFailureMessage(): String =
    "Failed to delete the meal. Please try again."

internal fun mealScheduleFailureMessage(): String =
    "Failed to fetch the meal schedule. Please try again."

internal fun mealSummaryFailureMessage(): String =
    "Failed to fetch the meal summary. Please try again."

internal fun upcomingMealsFailureMessage(): String =
    "Failed to fetch upcoming meals. Please try again."

internal fun mealAutoGenerateFailureMessage(): String =
    "Failed to generate the meal plan. Please try again."

internal fun dietaryRestrictionsListFailureMessage(): String =
    "Failed to fetch dietary restrictions. Please try again."

internal fun dietaryRestrictionCreateFailureMessage(): String =
    "Failed to add the dietary restriction. Please try again."

internal fun participantDietaryRestrictionsFailureMessage(): String =
    "Failed to fetch participant dietary restrictions. Please try again."

internal fun dietaryRestrictionCountsFailureMessage(): String =
    "Failed to fetch dietary restriction counts. Please try again."

internal fun dietaryRestrictionDeleteFailureMessage(): String =
    "Failed to delete the dietary restriction. Please try again."

private fun mealAccessDenied(eventId: String, userId: String, action: String): Map<String, String> =
    mapOf(
        "error" to "You do not have access to this event meal plan",
        "auditReference" to "audit-${eventId.take(12)}-${userId.take(12)}-${System.currentTimeMillis()}",
        "action" to action
    )
