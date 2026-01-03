package com.guyghost.wakeve.routes

import com.guyghost.wakeve.meal.MealPlanner
import com.guyghost.wakeve.meal.MealRepository
import com.guyghost.wakeve.models.AutoMealPlanRequest
import com.guyghost.wakeve.models.DietaryRestrictionRequest
import com.guyghost.wakeve.models.MealRequest
import io.ktor.http.HttpStatusCode
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
fun io.ktor.server.routing.Route.mealRoutes(repository: MealRepository) {
    route("/events/{eventId}/meals") {
        
        // GET /api/events/{eventId}/meals - Get all meals for event
        get {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val meals = repository.getMealsByEventId(eventId)
                call.respond(HttpStatusCode.OK, meals)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                val request = call.receive<MealRequest>()
                
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
                    mapOf("error" to e.message.orEmpty())
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

                val meal = repository.getMealById(mealId)
                if (meal != null) {
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
                    mapOf("error" to e.message.orEmpty())
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

                val existing = repository.getMealById(mealId)
                if (existing == null) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Meal not found")
                    )
                }

                val request = call.receive<MealRequest>()
                
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
                    mapOf("error" to e.message.orEmpty())
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

                val existing = repository.getMealById(mealId)
                if (existing == null) {
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
                    mapOf("error" to e.message.orEmpty())
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

                val schedule = repository.getDailyMealSchedule(eventId)
                call.respond(HttpStatusCode.OK, schedule)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                val summary = repository.getMealPlanningSummary(eventId)
                call.respond(HttpStatusCode.OK, summary)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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
                
                val limit = call.request.queryParameters["limit"]?.toLongOrNull() ?: 10L

                val meals = repository.getUpcomingMeals(eventId, limit)
                call.respond(HttpStatusCode.OK, meals)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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
                    mapOf("error" to e.message.orEmpty())
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

                val restrictions = repository.getDietaryRestrictionsByEventId(eventId)
                call.respond(HttpStatusCode.OK, restrictions)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                val request = call.receive<DietaryRestrictionRequest>()
                
                // Ensure eventId matches
                if (request.eventId != eventId) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Event ID mismatch")
                    )
                }

                val restriction = repository.addDietaryRestriction(request)
                call.respond(HttpStatusCode.Created, restriction)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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
                
                val participantId = call.parameters["participantId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Participant ID required")
                )

                val restrictions = repository.getDietaryRestrictionsByParticipant(eventId, participantId)
                call.respond(HttpStatusCode.OK, restrictions)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                val counts = repository.getDietaryRestrictionCounts(eventId)
                call.respond(HttpStatusCode.OK, counts)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                repository.deleteDietaryRestriction(restrictionId)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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
