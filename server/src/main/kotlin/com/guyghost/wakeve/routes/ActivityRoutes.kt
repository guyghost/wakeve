
import com.guyghost.wakeve.activity.ActivityRepository
import com.guyghost.wakeve.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.time.LocalDate

/**
 * Activity API Routes
 * 
 * Provides RESTful endpoints for activity planning and management including:
 * - Activity CRUD operations
 * - Participant registration and capacity management
 * - Date-based grouping and filtering
 * - Cost calculations
 * - Schedule generation
 */
fun io.ktor.server.routing.Route.activityRoutes(repository: ActivityRepository) {
    route("/events/{eventId}/activities") {
        
        // GET /api/events/{eventId}/activities - Get all activities for event
        get {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val activities = repository.getActivitiesByEventId(eventId)
                call.respond(HttpStatusCode.OK, activities)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // GET /api/events/{eventId}/activities/schedule - Get activities grouped by date
        get("/schedule") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val schedule = repository.getActivitiesByDateGrouped(eventId)
                call.respond(HttpStatusCode.OK, schedule)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // GET /api/events/{eventId}/activities/date/{date} - Get activities for specific date
        get("/date/{date}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                
                val dateStr = call.parameters["date"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Date required")
                )
                
                // Validate date format
                try {
                    LocalDate.parse(dateStr)
                } catch (e: Exception) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid date format. Use YYYY-MM-DD")
                    )
                }

                val activities = repository.getActivitiesByEventAndDate(eventId, dateStr)
                call.respond(HttpStatusCode.OK, activities)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // GET /api/events/{eventId}/activities/statistics - Get activity statistics
        get("/statistics") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val totalCost = repository.sumActivityCostByEvent(eventId)
                val totalActivities = repository.countActivitiesByEvent(eventId)
                
                // For deeper stats we need to fetch activities
                val activities = repository.getActivitiesByEventId(eventId)
                val totalRegistrations = activities.sumOf { it.registeredParticipantIds.size }
                
                val stats = mapOf(
                    "totalActivities" to totalActivities,
                    "totalCost" to totalCost,
                    "totalRegistrations" to totalRegistrations,
                    "fullActivities" to activities.count { 
                         val max = it.maxParticipants
                         max != null && it.registeredParticipantIds.size >= max
                    }
                )
                
                call.respond(HttpStatusCode.OK, stats)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // GET /api/events/{eventId}/activities/{activityId} - Get activity by ID
        get("/{activityId}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                
                val activityId = call.parameters["activityId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Activity ID required")
                )

                val activity = repository.getActivityById(activityId)
                if (activity == null) {
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Activity not found")
                    )
                }
                
                if (activity.eventId != eventId) {
                     return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Activity not found in this event")
                    )
                }
                
                call.respond(HttpStatusCode.OK, activity)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // GET /api/events/{eventId}/activities/{activityId}/participants - Get participants for activity
        get("/{activityId}/participants") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                
                val activityId = call.parameters["activityId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Activity ID required")
                )

                val participants = repository.getParticipantsByActivity(activityId)
                call.respond(HttpStatusCode.OK, participants)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // POST /api/events/{eventId}/activities - Create an activity
        post {
            try {
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val request = call.receive<CreateActivityRequest>()
                
                // Validate request
                if (request.name.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Activity name is required")
                    )
                }
                
                if (request.durationMinutes <= 0) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Duration must be greater than 0")
                    )
                }
                
                if (request.time != null && !request.time.matches(Regex("\\d{2}:\\d{2}"))) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid time format. Use HH:MM")
                    )
                }
                
                if (request.maxParticipants != null && request.maxParticipants <= 0) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Max participants must be greater than 0")
                    )
                }

                val activity = repository.createActivity(request.toActivity(eventId))
                call.respond(HttpStatusCode.Created, activity)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // POST /api/events/{eventId}/activities/{activityId}/register - Register participant for activity
        post("/{activityId}/register") {
            try {
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                
                val activityId = call.parameters["activityId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Activity ID required")
                )

                val request = call.receive<RegisterActivityRequest>()
                
                // Check if activity exists
                val activity = repository.getActivityById(activityId)
                if (activity == null) {
                    return@post call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Activity not found")
                    )
                }
                
                // Check if activity is full
                val maxParticipants = activity.maxParticipants
                if (maxParticipants != null && activity.registeredParticipantIds.size >= maxParticipants) {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Activity is full")
                    )
                }
                
                // Check if already registered
                if (repository.isParticipantRegistered(activityId, request.participantId)) {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Participant already registered")
                    )
                }
                
                val registration = ActivityParticipant(
                     id = java.util.UUID.randomUUID().toString(),
                     activityId = activityId,
                     participantId = request.participantId,
                     registeredAt = java.time.Instant.now().toString(),
                     notes = null
                )

                repository.registerParticipant(registration)
                call.respond(HttpStatusCode.Created, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // PUT /api/events/{eventId}/activities/{activityId} - Update an activity
        put("/{activityId}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                
                val activityId = call.parameters["activityId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Activity ID required")
                )

                val request = call.receive<UpdateActivityRequest>()
                
                val existingActivity = repository.getActivityById(activityId)
                if (existingActivity == null) {
                    return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Activity not found"))
                }
                
                // Validate request
                if (request.name != null && request.name.isBlank()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Activity name cannot be empty")
                    )
                }
                
                if (request.durationMinutes != null && request.durationMinutes <= 0) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Duration must be greater than 0")
                    )
                }
                
                if (request.time != null && !request.time.matches(Regex("\\d{2}:\\d{2}"))) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid time format. Use HH:MM")
                    )
                }
                
                if (request.maxParticipants != null && request.maxParticipants <= 0) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Max participants must be greater than 0")
                    )
                }
                
                // Apply updates to existing activity - preserving ID, eventID and registered participants
                val updatedActivityData = existingActivity.copy(
                    name = request.name ?: existingActivity.name,
                    description = request.description ?: existingActivity.description,
                    date = request.date ?: existingActivity.date,
                    time = request.time ?: existingActivity.time,
                    duration = request.durationMinutes ?: existingActivity.duration,
                    location = request.location ?: existingActivity.location,
                    maxParticipants = request.maxParticipants ?: existingActivity.maxParticipants,
                    cost = request.costPerPerson ?: existingActivity.cost,
                    organizerId = request.organizerId ?: existingActivity.organizerId,
                    updatedAt = java.time.Instant.now().toString()
                )

                val updatedActivity = repository.updateActivity(updatedActivityData)
                call.respond(HttpStatusCode.OK, updatedActivity)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // DELETE /api/events/{eventId}/activities/{activityId} - Delete an activity
        delete("/{activityId}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                
                val activityId = call.parameters["activityId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Activity ID required")
                )

                repository.deleteActivity(activityId)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // DELETE /api/events/{eventId}/activities/{activityId}/register/{participantId} - Unregister participant
        delete("/{activityId}/register/{participantId}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                
                val activityId = call.parameters["activityId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Activity ID required")
                )
                
                val participantId = call.parameters["participantId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Participant ID required")
                )

                repository.unregisterParticipant(activityId, participantId)
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
