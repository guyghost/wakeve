package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.userId
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.repository.PotentialLocationRepositoryInterface
import com.guyghost.wakeve.models.CreatePotentialLocationRequest
import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.PotentialLocationResponse
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
import io.ktor.server.routing.route

fun io.ktor.server.routing.Route.potentialLocationRoutes(
    locationRepository: PotentialLocationRepositoryInterface,
    eventRepository: DatabaseEventRepository,
    database: WakeveDb,
    moderationPolicy: ModerationPolicy = ModerationPolicy()
) {
    route("/events/{eventId}/potential-locations") {
        
        // GET /api/events/{eventId}/potential-locations - Get all potential locations for an event
        get {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedPotentialLocationUserId() ?: return@get
                if (!hasPotentialLocationReadAccess(eventRepository, database, eventId, userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        potentialLocationAccessDenied(eventId, userId, "read_potential_locations")
                    )
                }
                
                val locations = locationRepository.getLocationsByEventId(eventId)
                val responses = locations.map { location ->
                    PotentialLocationResponse(
                        id = location.id,
                        eventId = location.eventId,
                        name = location.name,
                        locationType = location.locationType.name,
                        address = location.address,
                        createdAt = location.createdAt
                    )
                }
                
                call.respond(HttpStatusCode.OK, mapOf("locations" to responses))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to potentialLocationListFailureMessage())
                )
            }
        }
        
        // POST /api/events/{eventId}/potential-locations - Add a potential location
        post {
            try {
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.authenticatedPotentialLocationUserId() ?: return@post
                if (!isPotentialLocationOrganizer(eventRepository, eventId, userId)) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        potentialLocationAccessDenied(eventId, userId, "create_potential_location")
                    )
                }
                
                val request = call.receive<CreatePotentialLocationRequest>()
                if (call.rejectRejectedModeratedText(
                        moderationPolicy,
                        listOf(
                            ModeratedTextField("name", request.name),
                            ModeratedTextField("address", request.address)
                        )
                    )
                ) {
                    return@post
                }
                
                // Validate location type
                val locationType = try {
                    LocationType.valueOf(request.locationType)
                } catch (e: IllegalArgumentException) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid location type: ${request.locationType}")
                    )
                }
                
                // Create location
                val now = java.time.Instant.now().toString()
                val location = PotentialLocation(
                    id = "location_${System.currentTimeMillis()}_${Math.random()}",
                    eventId = eventId,
                    name = request.name,
                    locationType = locationType,
                    address = request.address,
                    createdAt = now
                )
                
                val result = locationRepository.addLocation(eventId, location)
                
                if (result.isSuccess) {
                    val response = PotentialLocationResponse(
                        id = location.id,
                        eventId = location.eventId,
                        name = location.name,
                        locationType = location.locationType.name,
                        address = location.address,
                        createdAt = location.createdAt
                    )
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    val error = result.exceptionOrNull()
                    when (error) {
                        is IllegalArgumentException -> call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to potentialLocationCreateTargetNotFoundMessage())
                        )
                        is IllegalStateException -> call.respond(
                            HttpStatusCode.Conflict,
                            mapOf("error" to potentialLocationCreateConflictMessage())
                        )
                        else -> call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to potentialLocationCreateFailureMessage())
                        )
                    }
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to potentialLocationCreateFailureMessage())
                )
            }
        }
        
        // DELETE /api/events/{eventId}/potential-locations/{locationId} - Remove a potential location
        delete("/{locationId}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                
                val locationId = call.parameters["locationId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Location ID required")
                )
                val userId = call.authenticatedPotentialLocationUserId() ?: return@delete
                if (!isPotentialLocationOrganizer(eventRepository, eventId, userId)) {
                    return@delete call.respond(
                        HttpStatusCode.Forbidden,
                        potentialLocationAccessDenied(eventId, userId, "delete_potential_location")
                    )
                }
                
                val result = locationRepository.removeLocation(eventId, locationId)
                
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Location removed successfully"))
                } else {
                    val error = result.exceptionOrNull()
                    when (error) {
                        is IllegalArgumentException -> call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to potentialLocationDeleteTargetNotFoundMessage())
                        )
                        is IllegalStateException -> call.respond(
                            HttpStatusCode.Conflict,
                            mapOf("error" to potentialLocationDeleteConflictMessage())
                        )
                        else -> call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to potentialLocationDeleteFailureMessage())
                        )
                    }
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to potentialLocationDeleteFailureMessage())
                )
            }
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.authenticatedPotentialLocationUserId(): String? {
    val principal = principal<JWTPrincipal>()
    if (principal == null) {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
        return null
    }
    return principal.userId
}

private fun hasPotentialLocationReadAccess(
    eventRepository: DatabaseEventRepository,
    database: WakeveDb,
    eventId: String,
    userId: String
): Boolean {
    return isPotentialLocationOrganizer(eventRepository, eventId, userId) ||
        database.participantQueries.selectByEventIdAndUserId(eventId, userId).executeAsOneOrNull() != null
}

private fun isPotentialLocationOrganizer(
    eventRepository: DatabaseEventRepository,
    eventId: String,
    userId: String
): Boolean = eventRepository.getEvent(eventId)?.organizerId == userId

private fun potentialLocationAccessDenied(eventId: String, userId: String, action: String): Map<String, String> =
    mapOf(
        "error" to "You do not have access to this event location planning",
        "auditReference" to "audit-${eventId.take(12)}-${userId.take(12)}-${System.currentTimeMillis()}",
        "action" to action
    )

internal fun potentialLocationListFailureMessage(): String =
    "Failed to fetch potential locations. Please try again."

internal fun potentialLocationCreateTargetNotFoundMessage(): String =
    "The event for this potential location could not be found."

internal fun potentialLocationCreateConflictMessage(): String =
    "This potential location cannot be added in the current event state."

internal fun potentialLocationCreateFailureMessage(): String =
    "Failed to add the potential location. Please review the details and try again."

internal fun potentialLocationDeleteTargetNotFoundMessage(): String =
    "The potential location could not be found."

internal fun potentialLocationDeleteConflictMessage(): String =
    "This potential location cannot be removed in the current event state."

internal fun potentialLocationDeleteFailureMessage(): String =
    "Failed to remove the potential location. Please try again."
