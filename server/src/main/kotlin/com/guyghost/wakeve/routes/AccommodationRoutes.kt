package com.guyghost.wakeve.routes

import com.guyghost.wakeve.accommodation.AccommodationService
import com.guyghost.wakeve.accommodation.AccommodationRepository
import com.guyghost.wakeve.auth.userId
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Accommodation
import com.guyghost.wakeve.models.AccommodationRequest
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.RoomAssignment
import com.guyghost.wakeve.models.RoomAssignmentRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
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
fun Route.accommodationRoutes(repository: AccommodationRepository, database: WakeveDb) {
    route("/events/{eventId}/accommodation") {

        // GET /api/events/{eventId}/accommodation - Get all accommodations for event
        get {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!hasConfirmedAccommodationAccess(database, eventId, principal.userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to accommodation details")
                    )
                }

                val accommodations = repository.getAccommodationsByEventId(eventId)
                call.respond(HttpStatusCode.OK, accommodations)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to accommodationListFailureMessage())
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
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!isAccommodationOrganizer(database, eventId, principal.userId)) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer can create accommodations")
                    )
                }

                if (!isAccommodationCreationAllowed(database, eventId)) {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Accommodations can only be created for CONFIRMED or COMPARING events")
                    )
                }
                
                val request = call.receive<AccommodationRequest>()
                if (request.eventId.trim() != eventId) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Accommodation eventId must match path eventId")
                    )
                }

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
                    totalCost = 0,
                    bookingStatus = request.bookingStatus,
                    bookingUrl = request.bookingUrl,
                    checkInDate = request.checkInDate,
                    checkOutDate = request.checkOutDate,
                    notes = request.notes,
                    createdAt = AccommodationService.getCurrentUtcIsoString(),
                    updatedAt = AccommodationService.getCurrentUtcIsoString()
                )
                val normalized = AccommodationService.normalizeAccommodation(accommodation).getOrElse { _ ->
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to accommodationValidationFailureMessage())
                    )
                }

                val created = repository.createAccommodation(normalized)
                call.respond(HttpStatusCode.Created, created)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to accommodationCreateFailureMessage())
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
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!hasConfirmedAccommodationAccess(database, eventId, principal.userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to accommodation details")
                    )
                }

                val accommodation = repository.getAccommodationById(accommodationId)

                if (accommodation == null || accommodation.eventId != eventId) {
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Accommodation not found")
                    )
                }

                call.respond(HttpStatusCode.OK, accommodation)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to accommodationDetailFailureMessage())
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
                val eventId = call.parameters["eventId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!isAccommodationOrganizer(database, eventId, principal.userId)) {
                    return@put call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer can update accommodations")
                    )
                }
                
                val request = call.receive<AccommodationRequest>()
                if (request.eventId.trim() != eventId) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Accommodation eventId must match path eventId")
                    )
                }

                // Get existing accommodation
                val existing = repository.getAccommodationById(accommodationId)

                if (existing == null) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Accommodation not found")
                    )
                }
                if (existing.eventId != eventId) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Accommodation not found")
                    )
                }

                if (isAccommodationEventFinalized(database, eventId)) {
                    return@put call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Finalized events are read-only")
                    )
                }

                val accommodation = Accommodation(
                    id = accommodationId,
                    eventId = eventId,
                    name = request.name,
                    type = request.type,
                    address = request.address,
                    capacity = request.capacity,
                    pricePerNight = request.pricePerNight,
                    totalNights = request.totalNights,
                    totalCost = existing.totalCost,
                    bookingStatus = request.bookingStatus,
                    bookingUrl = request.bookingUrl,
                    checkInDate = request.checkInDate,
                    checkOutDate = request.checkOutDate,
                    notes = request.notes,
                    createdAt = existing.createdAt,
                    updatedAt = AccommodationService.getCurrentUtcIsoString()
                )
                val normalized = AccommodationService.normalizeAccommodation(accommodation).getOrElse { _ ->
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to accommodationValidationFailureMessage())
                    )
                }

                val updated = repository.updateAccommodation(normalized)
                call.respond(HttpStatusCode.OK, updated)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to accommodationUpdateFailureMessage())
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
                val eventId = call.parameters["eventId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!isAccommodationOrganizer(database, eventId, principal.userId)) {
                    return@delete call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer can delete accommodations")
                    )
                }

                val existing = repository.getAccommodationById(accommodationId)
                if (existing == null || existing.eventId != eventId) {
                    return@delete call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Accommodation not found")
                    )
                }

                if (isAccommodationEventFinalized(database, eventId)) {
                    return@delete call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Finalized events are read-only")
                    )
                }

                // Delete from repository (will cascade to room assignments via foreign key)
                repository.deleteAccommodation(accommodationId)

                call.respond(HttpStatusCode.NoContent)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to accommodationDeleteFailureMessage())
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
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!hasConfirmedAccommodationAccess(database, eventId, principal.userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to room details")
                    )
                }

                val accommodation = repository.getAccommodationById(accommodationId)
                if (accommodation == null || accommodation.eventId != eventId) {
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Accommodation not found")
                    )
                }

                val rooms = repository.getRoomAssignmentsByAccommodationId(accommodationId)

                call.respond(HttpStatusCode.OK, rooms)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to roomListFailureMessage())
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
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!isAccommodationOrganizer(database, eventId, principal.userId)) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer can create room assignments")
                    )
                }
                
                val request = call.receive<RoomAssignmentRequest>()
                if (request.accommodationId.trim() != accommodationId) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Room accommodationId must match path accommodationId")
                    )
                }
                
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
                if (accommodation.eventId != eventId) {
                    return@post call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Accommodation not found")
                    )
                }
                val assignedParticipants = validateRoomAssignedParticipants(
                    database = database,
                    eventId = eventId,
                    assignedParticipants = request.assignedParticipants
                ).getOrElse { _ ->
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to roomAssignedParticipantsFailureMessage())
                    )
                }

                // Calculate price share per person
                val priceShare = AccommodationService.calculateRoomPriceShare(
                    accommodationTotalCost = accommodation.totalCost,
                    roomCapacity = request.capacity,
                    totalAccommodationCapacity = accommodation.capacity,
                    assignedParticipants = assignedParticipants.size
                )

                val roomAssignment = RoomAssignment(
                    id = UUID.randomUUID().toString(),
                    accommodationId = accommodationId,
                    roomNumber = request.roomNumber,
                    capacity = request.capacity,
                    assignedParticipants = assignedParticipants,
                    priceShare = priceShare,
                    createdAt = AccommodationService.getCurrentUtcIsoString(),
                    updatedAt = AccommodationService.getCurrentUtcIsoString()
                )
                val normalized = AccommodationService.normalizeRoomAssignment(roomAssignment).getOrElse { _ ->
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to roomValidationFailureMessage())
                    )
                }

                val created = repository.createRoomAssignment(normalized)
                call.respond(HttpStatusCode.Created, created)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to roomCreateFailureMessage())
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
                val accommodationId = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Accommodation ID required")
                )
                val eventId = call.parameters["eventId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!isAccommodationOrganizer(database, eventId, principal.userId)) {
                    return@put call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer can update room assignments")
                    )
                }
                
                val request = call.receive<RoomAssignmentRequest>()
                if (request.accommodationId.trim() != accommodationId) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Room accommodationId must match path accommodationId")
                    )
                }
                
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
                if (existing.accommodationId != accommodationId || accommodation.eventId != eventId) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Room assignment not found")
                    )
                }

                if (isAccommodationEventFinalized(database, eventId)) {
                    return@put call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Finalized events are read-only")
                    )
                }
                val assignedParticipants = validateRoomAssignedParticipants(
                    database = database,
                    eventId = eventId,
                    assignedParticipants = request.assignedParticipants
                ).getOrElse { _ ->
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to roomAssignedParticipantsFailureMessage())
                    )
                }

                // Calculate price share per person
                val priceShare = AccommodationService.calculateRoomPriceShare(
                    accommodationTotalCost = accommodation.totalCost,
                    roomCapacity = request.capacity,
                    totalAccommodationCapacity = accommodation.capacity,
                    assignedParticipants = assignedParticipants.size
                )

                val updated = RoomAssignment(
                    id = roomId,
                    accommodationId = existing.accommodationId,
                    roomNumber = request.roomNumber,
                    capacity = request.capacity,
                    assignedParticipants = assignedParticipants,
                    priceShare = priceShare,
                    createdAt = existing.createdAt,
                    updatedAt = AccommodationService.getCurrentUtcIsoString()
                )
                val normalized = AccommodationService.normalizeRoomAssignment(updated).getOrElse { _ ->
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to roomValidationFailureMessage())
                    )
                }

                val saved = repository.updateRoomAssignment(normalized)
                call.respond(HttpStatusCode.OK, saved)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to roomUpdateFailureMessage())
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
                val accommodationId = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Accommodation ID required")
                )
                val eventId = call.parameters["eventId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!isAccommodationOrganizer(database, eventId, principal.userId)) {
                    return@delete call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer can delete room assignments")
                    )
                }

                val existing = repository.getRoomAssignmentById(roomId)
                if (existing == null || existing.accommodationId != accommodationId) {
                    return@delete call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Room assignment not found")
                    )
                }
                val accommodation = repository.getAccommodationById(existing.accommodationId)
                if (accommodation == null || accommodation.eventId != eventId) {
                    return@delete call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Room assignment not found")
                    )
                }

                if (isAccommodationEventFinalized(database, eventId)) {
                    return@delete call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Finalized events are read-only")
                    )
                }

                repository.deleteRoomAssignment(roomId)

                call.respond(HttpStatusCode.NoContent)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to roomDeleteFailureMessage())
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
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!hasConfirmedAccommodationAccess(database, eventId, principal.userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to accommodation statistics")
                    )
                }

                val stats = repository.getStatistics(eventId)

                call.respond(HttpStatusCode.OK, stats)
                
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to accommodationStatisticsFailureMessage())
                )
            }
        }
    }
}

private fun hasConfirmedAccommodationAccess(database: WakeveDb, eventId: String, userId: String): Boolean {
    return isAccommodationOrganizer(database, eventId, userId) ||
        isConfirmedAccommodationParticipant(database, eventId, userId)
}

private fun isAccommodationOrganizer(database: WakeveDb, eventId: String, userId: String): Boolean {
    return database.eventQueries
        .selectById(eventId)
        .executeAsOneOrNull()
        ?.organizerId == userId
}

private fun isConfirmedAccommodationParticipant(database: WakeveDb, eventId: String, userId: String): Boolean {
    val participant = database.participantQueries
        .selectByEventIdAndUserId(eventId, userId)
        .executeAsOneOrNull()

    return participant?.hasValidatedDate == 1L
}

private fun isAccommodationCreationAllowed(database: WakeveDb, eventId: String): Boolean {
    val status = database.eventQueries
        .selectById(eventId)
        .executeAsOneOrNull()
        ?.status

    return status == EventStatus.CONFIRMED.name || status == EventStatus.COMPARING.name
}

private fun isAccommodationEventFinalized(database: WakeveDb, eventId: String): Boolean {
    return database.eventQueries
        .selectById(eventId)
        .executeAsOneOrNull()
        ?.status == EventStatus.FINALIZED.name
}

private fun validateRoomAssignedParticipants(
    database: WakeveDb,
    eventId: String,
    assignedParticipants: List<String>
): Result<List<String>> = runCatching {
    val normalized = assignedParticipants.map { it.trim() }
    require(normalized.none { it.isBlank() }) { "assignedParticipants cannot contain blank participant IDs" }
    require(normalized.toSet().size == normalized.size) { "Duplicate participants are not allowed" }

    val event = database.eventQueries.selectById(eventId).executeAsOneOrNull()
        ?: throw IllegalArgumentException("Event not found")
    val eventMemberIds = database.participantQueries
        .selectByEventId(eventId)
        .executeAsList()
        .map { it.userId }
        .toSet() + event.organizerId
    val unknownParticipants = normalized.filterNot { it in eventMemberIds }
    require(unknownParticipants.isEmpty()) {
        "Room assignments can only include event members: ${unknownParticipants.joinToString(", ")}"
    }

    normalized
}

internal fun accommodationListFailureMessage(): String =
    "Failed to fetch accommodations. Please try again."

internal fun accommodationValidationFailureMessage(): String =
    "Invalid accommodation details. Please review the request and try again."

internal fun accommodationCreateFailureMessage(): String =
    "Failed to create accommodation. Please try again."

internal fun accommodationDetailFailureMessage(): String =
    "Failed to fetch accommodation details. Please try again."

internal fun accommodationUpdateFailureMessage(): String =
    "Failed to update accommodation. Please try again."

internal fun accommodationDeleteFailureMessage(): String =
    "Failed to delete accommodation. Please try again."

internal fun roomListFailureMessage(): String =
    "Failed to fetch room assignments. Please try again."

internal fun roomAssignedParticipantsFailureMessage(): String =
    "Invalid assigned participants. Please select event members and try again."

internal fun roomValidationFailureMessage(): String =
    "Invalid room assignment details. Please review the request and try again."

internal fun roomCreateFailureMessage(): String =
    "Failed to create room assignment. Please try again."

internal fun roomUpdateFailureMessage(): String =
    "Failed to update room assignment. Please try again."

internal fun roomDeleteFailureMessage(): String =
    "Failed to delete room assignment. Please try again."

internal fun accommodationStatisticsFailureMessage(): String =
    "Failed to fetch accommodation statistics. Please try again."
