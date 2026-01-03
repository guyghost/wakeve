package com.guyghost.wakeve.routes

import com.guyghost.wakeve.accommodation.AccommodationService
import com.guyghost.wakeve.models.Accommodation
import com.guyghost.wakeve.models.AccommodationRequest
import com.guyghost.wakeve.models.RoomAssignment
import com.guyghost.wakeve.models.RoomAssignmentRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID

/**
 * Accommodation API Routes
 * 
 * Provides RESTful endpoints for accommodation management including:
 * - Accommodation CRUD operations
 * - Room assignment management
 * - Cost calculations
 * - Booking status tracking
 * - Statistics and summaries
 */
fun io.ktor.server.routing.Route.accommodationRoutes() {
    route("/events/{eventId}/accommodation") {
        
        // GET /api/events/{eventId}/accommodation - Get all accommodations for event
        get {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                
                // TODO: Get from repository
                // val accommodations = repository.getAccommodationsByEventId(eventId)
                val accommodations = emptyList<Accommodation>()
                call.respond(HttpStatusCode.OK, accommodations)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // POST /api/events/{eventId}/accommodation - Create new accommodation
        post {
            try {
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                
                val request = call.receive<AccommodationRequest>()
                
                // Validate data
                val validationError = AccommodationService.validateAccommodation(
                    name = request.name,
                    capacity = request.capacity,
                    pricePerNight = request.pricePerNight,
                    totalNights = request.totalNights,
                    checkInDate = request.checkInDate,
                    checkOutDate = request.checkOutDate
                )
                
                if (validationError != null) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to validationError)
                    )
                }
                
                // Calculate total cost
                val totalCost = AccommodationService.calculateTotalCost(
                    request.pricePerNight,
                    request.totalNights
                )
                
                // Create accommodation
                val accommodation = Accommodation(
                    id = UUID.randomUUID().toString(),
                    eventId = eventId,
                    name = request.name,
                    type = request.type,
                    address = request.address,
                    capacity = request.capacity,
                    pricePerNight = request.pricePerNight,
                    totalNights = request.totalNights,
                    totalCost = totalCost,
                    bookingStatus = request.bookingStatus,
                    bookingUrl = request.bookingUrl,
                    checkInDate = request.checkInDate,
                    checkOutDate = request.checkOutDate,
                    notes = request.notes,
                    createdAt = AccommodationService.getCurrentUtcIsoString(),
                    updatedAt = AccommodationService.getCurrentUtcIsoString()
                )
                
                // TODO: Save to repository
                // repository.createAccommodation(accommodation)
                
                call.respond(HttpStatusCode.Created, accommodation)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // GET /api/events/{eventId}/accommodation/{id} - Get specific accommodation
        get("/{id}") {
            try {
                val accommodationId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Accommodation ID required")
                )
                
                // TODO: Get from repository
                // val accommodation = repository.getAccommodationById(accommodationId)
                
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Accommodation not found")
                )
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // PUT /api/events/{eventId}/accommodation/{id} - Update accommodation
        put("/{id}") {
            try {
                val accommodationId = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Accommodation ID required")
                )
                
                val request = call.receive<AccommodationRequest>()
                
                // Validate data
                val validationError = AccommodationService.validateAccommodation(
                    name = request.name,
                    capacity = request.capacity,
                    pricePerNight = request.pricePerNight,
                    totalNights = request.totalNights,
                    checkInDate = request.checkInDate,
                    checkOutDate = request.checkOutDate
                )
                
                if (validationError != null) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to validationError)
                    )
                }
                
                // Calculate total cost
                val totalCost = AccommodationService.calculateTotalCost(
                    request.pricePerNight,
                    request.totalNights
                )
                
                // TODO: Get existing accommodation and update
                // val existing = repository.getAccommodationById(accommodationId)
                // if (existing == null) return@put call.respond(HttpStatusCode.NotFound)
                
                val accommodation = Accommodation(
                    id = accommodationId,
                    eventId = request.eventId,
                    name = request.name,
                    type = request.type,
                    address = request.address,
                    capacity = request.capacity,
                    pricePerNight = request.pricePerNight,
                    totalNights = request.totalNights,
                    totalCost = totalCost,
                    bookingStatus = request.bookingStatus,
                    bookingUrl = request.bookingUrl,
                    checkInDate = request.checkInDate,
                    checkOutDate = request.checkOutDate,
                    notes = request.notes,
                    createdAt = "", // existing.createdAt
                    updatedAt = AccommodationService.getCurrentUtcIsoString()
                )
                
                // TODO: Save to repository
                // repository.updateAccommodation(accommodation)
                
                call.respond(HttpStatusCode.OK, accommodation)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // DELETE /api/events/{eventId}/accommodation/{id} - Delete accommodation
        delete("/{id}") {
            try {
                val accommodationId = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Accommodation ID required")
                )
                
                // TODO: Delete from repository (will cascade to room assignments)
                // repository.deleteAccommodation(accommodationId)
                
                call.respond(HttpStatusCode.NoContent)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // GET /api/events/{eventId}/accommodation/{id}/rooms - Get room assignments
        get("/{id}/rooms") {
            try {
                val accommodationId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Accommodation ID required")
                )
                
                // TODO: Get from repository
                // val rooms = repository.getRoomAssignmentsByAccommodationId(accommodationId)
                val rooms = emptyList<RoomAssignment>()
                
                call.respond(HttpStatusCode.OK, rooms)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // POST /api/events/{eventId}/accommodation/{id}/rooms - Create room assignment
        post("/{id}/rooms") {
            try {
                val accommodationId = call.parameters["id"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Accommodation ID required")
                )
                
                val request = call.receive<RoomAssignmentRequest>()
                
                // Validate room assignment
                val validationError = AccommodationService.validateRoomAssignment(
                    roomNumber = request.roomNumber,
                    capacity = request.capacity,
                    assignedParticipants = request.assignedParticipants
                )
                
                if (validationError != null) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to validationError)
                    )
                }
                
                // TODO: Get accommodation to calculate price share
                // val accommodation = repository.getAccommodationById(accommodationId)
                // if (accommodation == null) return@post call.respond(HttpStatusCode.NotFound)
                
                // For now, use placeholder price share
                val priceShare = 0L
                
                val roomAssignment = RoomAssignment(
                    id = UUID.randomUUID().toString(),
                    accommodationId = accommodationId,
                    roomNumber = request.roomNumber,
                    capacity = request.capacity,
                    assignedParticipants = request.assignedParticipants,
                    priceShare = priceShare,
                    createdAt = AccommodationService.getCurrentUtcIsoString(),
                    updatedAt = AccommodationService.getCurrentUtcIsoString()
                )
                
                // TODO: Save to repository
                // repository.createRoomAssignment(roomAssignment)
                
                call.respond(HttpStatusCode.Created, roomAssignment)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // PUT /api/events/{eventId}/accommodation/{id}/rooms/{roomId} - Update room assignment
        put("/{id}/rooms/{roomId}") {
            try {
                val roomId = call.parameters["roomId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Room ID required")
                )
                
                val request = call.receive<RoomAssignmentRequest>()
                
                // Validate room assignment
                val validationError = AccommodationService.validateRoomAssignment(
                    roomNumber = request.roomNumber,
                    capacity = request.capacity,
                    assignedParticipants = request.assignedParticipants
                )
                
                if (validationError != null) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to validationError)
                    )
                }
                
                // TODO: Update in repository
                call.respond(HttpStatusCode.OK, mapOf("message" to "Room assignment updated"))
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // DELETE /api/events/{eventId}/accommodation/{id}/rooms/{roomId} - Delete room assignment
        delete("/{id}/rooms/{roomId}") {
            try {
                val roomId = call.parameters["roomId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Room ID required")
                )
                
                // TODO: Delete from repository
                // repository.deleteRoomAssignment(roomId)
                
                call.respond(HttpStatusCode.NoContent)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // GET /api/events/{eventId}/accommodation/statistics - Get accommodation stats
        get("/statistics") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                
                // TODO: Calculate statistics from repository
                val stats = mapOf(
                    "totalAccommodations" to 0,
                    "totalCapacity" to 0,
                    "totalAssigned" to 0,
                    "totalCost" to 0,
                    "averageCostPerPerson" to 0
                )
                
                call.respond(HttpStatusCode.OK, stats)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
    }
}
