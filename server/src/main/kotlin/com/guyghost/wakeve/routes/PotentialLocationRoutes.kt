package com.guyghost.wakeve.routes

import com.guyghost.wakeve.PotentialLocationRepositoryInterface
import com.guyghost.wakeve.models.CreatePotentialLocationRequest
import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.PotentialLocationResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun io.ktor.server.routing.Route.potentialLocationRoutes(
    locationRepository: PotentialLocationRepositoryInterface
) {
    route("/events/{eventId}/potential-locations") {
        
        // GET /api/events/{eventId}/potential-locations - Get all potential locations for an event
        get {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                
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
                    mapOf("error" to e.message.orEmpty())
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
                
                val request = call.receive<CreatePotentialLocationRequest>()
                
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
                            mapOf("error" to error.message.orEmpty())
                        )
                        is IllegalStateException -> call.respond(
                            HttpStatusCode.Conflict,
                            mapOf("error" to error.message.orEmpty())
                        )
                        else -> call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to (error?.message ?: "Failed to add location"))
                        )
                    }
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message.orEmpty())
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
                
                val result = locationRepository.removeLocation(eventId, locationId)
                
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Location removed successfully"))
                } else {
                    val error = result.exceptionOrNull()
                    when (error) {
                        is IllegalArgumentException -> call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to error.message.orEmpty())
                        )
                        is IllegalStateException -> call.respond(
                            HttpStatusCode.Conflict,
                            mapOf("error" to error.message.orEmpty())
                        )
                        else -> call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to (error?.message ?: "Failed to remove location"))
                        )
                    }
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
    }
}
