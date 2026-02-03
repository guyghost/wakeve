package com.guyghost.wakeve.routes

import com.guyghost.wakeve.accommodation.AccommodationService
import com.guyghost.wakeve.accommodation.AccommodationRepository
import com.guyghost.wakeve.models.Accommodation
import com.guyghost.wakeve.models.AccommodationRequest
import com.guyghost.wakeve.models.RoomAssignment
import com.guyghost.wakeve.models.RoomAssignmentRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
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
fun Route.accommodationRoutes(repository: AccommodationRepository) {
    route("/events/{eventId}/accommodation") {

        // GET /api/events/{eventId}/accommodation - Get all accommodations for event
        get {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val accommodations = repository.getAccommodationsByEventId(eventId)
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

                val created = repository.createAccommodation(accommodation)
                call.respond(HttpStatusCode.Created, created)
                
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

                val accommodation = repository.getAccommodationById(accommodationId)

                if (accommodation != null) {
                    call.respond(HttpStatusCode.OK, accommodation)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Accommodation not found")
                    )
                }
                
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

                // Get existing accommodation
                val existing = repository.getAccommodationById(accommodationId)

                if (existing == null) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Accommodation not found")
                    )
                }

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
                    createdAt = existing.createdAt,
                    updatedAt = AccommodationService.getCurrentUtcIsoString()
                )

                val updated = repository.updateAccommodation(accommodation)
                call.respond(HttpStatusCode.OK, updated)
                
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

                // Delete from repository (will cascade to room assignments via foreign key)
                repository.deleteAccommodation(accommodationId)

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

                val rooms = repository.getRoomAssignmentsByAccommodationId(accommodationId)

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

                // Get accommodation to calculate price share
                val accommodation = repository.getAccommodationById(accommodationId)
                if (accommodation == null) {
                    return@post call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Accommodation not found")
                    )
                }

                // Calculate price share per person
                val priceShare = AccommodationService.calculateRoomPriceShare(
                    accommodationTotalCost = accommodation.totalCost,
                    roomCapacity = request.capacity,
                    totalAccommodationCapacity = accommodation.capacity,
                    assignedParticipants = request.assignedParticipants.size
                )

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

                val created = repository.createRoomAssignment(roomAssignment)
                call.respond(HttpStatusCode.Created, created)
                
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

                // Get existing room assignment
                val existing = repository.getRoomAssignmentById(roomId)

                if (existing == null) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Room assignment not found")
                    )
                }

                // Get accommodation for price share calculation
                val accommodation = repository.getAccommodationById(existing.accommodationId)
                if (accommodation == null) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Accommodation not found")
                    )
                }

                // Calculate price share per person
                val priceShare = AccommodationService.calculateRoomPriceShare(
                    accommodationTotalCost = accommodation.totalCost,
                    roomCapacity = request.capacity,
                    totalAccommodationCapacity = accommodation.capacity,
                    assignedParticipants = request.assignedParticipants.size
                )

                val updated = RoomAssignment(
                    id = roomId,
                    accommodationId = existing.accommodationId,
                    roomNumber = request.roomNumber,
                    capacity = request.capacity,
                    assignedParticipants = request.assignedParticipants,
                    priceShare = priceShare,
                    createdAt = existing.createdAt,
                    updatedAt = AccommodationService.getCurrentUtcIsoString()
                )

                repository.updateRoomAssignment(updated)
                call.respond(HttpStatusCode.OK, updated)
                
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

                repository.deleteRoomAssignment(roomId)

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

                val stats = repository.getStatistics(eventId)

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
