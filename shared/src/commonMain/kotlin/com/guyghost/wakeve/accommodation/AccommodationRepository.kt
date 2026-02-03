package com.guyghost.wakeve.accommodation

import com.guyghost.wakeve.accommodation.AccommodationService
import com.guyghost.wakeve.models.Accommodation
import com.guyghost.wakeve.models.AccommodationType
import com.guyghost.wakeve.models.BookingStatus
import com.guyghost.wakeve.models.RoomAssignment

/**
 * Accommodation Repository - Manages accommodation and room assignment persistence.
 *
 * Responsibilities:
 * - CRUD operations for accommodations
 * - CRUD operations for room assignments
 * - Statistics calculations from database
 * - Map between SQLDelight entities and Kotlin models
 */
class AccommodationRepository(private val db: com.guyghost.wakeve.database.WakevDb) {

    private val accommodationQueries = db.accommodationQueries
    private val roomAssignmentQueries = db.roomAssignmentQueries

    // ==================== Accommodation Operations ====================

    /**
     * Create a new accommodation.
     *
     * @param accommodation The accommodation to create
     * @return Created accommodation
     */
    fun createAccommodation(accommodation: Accommodation): Accommodation {
        accommodationQueries.insertAccommodation(
            id = accommodation.id,
            event_id = accommodation.eventId,
            name = accommodation.name,
            type = accommodation.type.name,
            address = accommodation.address,
            capacity = accommodation.capacity.toLong(),
            price_per_night = accommodation.pricePerNight,
            total_nights = accommodation.totalNights.toLong(),
            total_cost = accommodation.totalCost,
            booking_status = accommodation.bookingStatus.name,
            booking_url = accommodation.bookingUrl,
            check_in_date = accommodation.checkInDate,
            check_out_date = accommodation.checkOutDate,
            notes = accommodation.notes,
            created_at = accommodation.createdAt,
            updated_at = accommodation.updatedAt
        )
        return accommodation
    }

    /**
     * Get accommodation by ID.
     *
     * @param accommodationId The accommodation ID
     * @return Accommodation or null if not found
     */
    fun getAccommodationById(accommodationId: String): Accommodation? {
        return accommodationQueries.getAccommodationById(accommodationId)
            .executeAsOneOrNull()
            ?.toModel()
    }

    /**
     * Get all accommodations for an event.
     *
     * @param eventId The event ID
     * @return List of accommodations sorted by check-in date
     */
    fun getAccommodationsByEventId(eventId: String): List<Accommodation> {
        return accommodationQueries.getAccommodationsByEventId(eventId)
            .executeAsList()
            .map { it.toModel() }
    }

    /**
     * Get accommodations by booking status for an event.
     *
     * @param eventId The event ID
     * @param status The booking status to filter by
     * @return List of accommodations with the specified status
     */
    fun getAccommodationsByStatus(eventId: String, status: BookingStatus): List<Accommodation> {
        return accommodationQueries.getAccommodationsByStatus(eventId, status.name)
            .executeAsList()
            .map { it.toModel() }
    }

    /**
     * Get confirmed accommodations for an event.
     *
     * @param eventId The event ID
     * @return List of confirmed accommodations
     */
    fun getConfirmedAccommodations(eventId: String): List<Accommodation> {
        return accommodationQueries.getConfirmedAccommodations(eventId)
            .executeAsList()
            .map { it.toModel() }
    }

    /**
     * Update an accommodation.
     *
     * @param accommodation The accommodation to update
     * @return Updated accommodation
     */
    fun updateAccommodation(accommodation: Accommodation): Accommodation {
        accommodationQueries.updateAccommodation(
            name = accommodation.name,
            type = accommodation.type.name,
            address = accommodation.address,
            capacity = accommodation.capacity.toLong(),
            price_per_night = accommodation.pricePerNight,
            total_nights = accommodation.totalNights.toLong(),
            total_cost = accommodation.totalCost,
            booking_status = accommodation.bookingStatus.name,
            booking_url = accommodation.bookingUrl,
            check_in_date = accommodation.checkInDate,
            check_out_date = accommodation.checkOutDate,
            notes = accommodation.notes,
            updated_at = accommodation.updatedAt,
            id = accommodation.id
        )
        return accommodation
    }

    /**
     * Update only the booking status of an accommodation.
     *
     * @param accommodationId The accommodation ID
     * @param status The new booking status
     */
    fun updateBookingStatus(accommodationId: String, status: BookingStatus) {
        val now = AccommodationService.getCurrentUtcIsoString()
        accommodationQueries.updateBookingStatus(
            booking_status = status.name,
            updated_at = now,
            id = accommodationId
        )
    }

    /**
     * Delete an accommodation (will cascade to room assignments via foreign key).
     *
     * @param accommodationId The accommodation ID
     */
    fun deleteAccommodation(accommodationId: String) {
        accommodationQueries.deleteAccommodation(accommodationId)
    }

    /**
     * Delete all accommodations for an event.
     *
     * @param eventId The event ID
     */
    fun deleteAccommodationsByEventId(eventId: String) {
        accommodationQueries.deleteAccommodationsByEventId(eventId)
    }

    // ==================== Room Assignment Operations ====================

    /**
     * Create a new room assignment.
     *
     * @param roomAssignment The room assignment to create
     * @return Created room assignment
     */
    fun createRoomAssignment(roomAssignment: RoomAssignment): RoomAssignment {
        roomAssignmentQueries.insertRoomAssignment(
            id = roomAssignment.id,
            accommodation_id = roomAssignment.accommodationId,
            room_number = roomAssignment.roomNumber,
            capacity = roomAssignment.capacity.toLong(),
            assigned_participants = roomAssignment.assignedParticipants.joinToString(","),
            price_share = roomAssignment.priceShare,
            created_at = roomAssignment.createdAt,
            updated_at = roomAssignment.updatedAt
        )
        return roomAssignment
    }

    /**
     * Get a room assignment by ID.
     *
     * @param roomId The room assignment ID
     * @return Room assignment or null if not found
     */
    fun getRoomAssignmentById(roomId: String): RoomAssignment? {
        return roomAssignmentQueries.getRoomAssignmentById(roomId)
            .executeAsOneOrNull()
            ?.toModel()
    }

    /**
     * Get all room assignments for an accommodation.
     *
     * @param accommodationId The accommodation ID
     * @return List of room assignments sorted by room number
     */
    fun getRoomAssignmentsByAccommodationId(accommodationId: String): List<RoomAssignment> {
        return roomAssignmentQueries.getRoomAssignmentsByAccommodationId(accommodationId)
            .executeAsList()
            .map { it.toModel() }
    }

    /**
     * Get a room assignment by room number for an accommodation.
     *
     * @param accommodationId The accommodation ID
     * @param roomNumber The room number
     * @return Room assignment or null if not found
     */
    fun getRoomAssignmentByRoomNumber(accommodationId: String, roomNumber: String): RoomAssignment? {
        return roomAssignmentQueries.getRoomAssignmentByRoomNumber(accommodationId, roomNumber)
            .executeAsOneOrNull()
            ?.toModel()
    }

    /**
     * Get room assignments where a specific participant is assigned.
     *
     * @param participantId The participant ID
     * @return List of room assignments for the participant
     */
    fun getRoomAssignmentsByParticipant(participantId: String): List<RoomAssignment> {
        return roomAssignmentQueries.getRoomAssignmentsByParticipant(participantId)
            .executeAsList()
            .map { it.toModel() }
    }

    /**
     * Update a room assignment.
     *
     * @param roomAssignment The room assignment to update
     * @return Updated room assignment
     */
    fun updateRoomAssignment(roomAssignment: RoomAssignment): RoomAssignment {
        roomAssignmentQueries.updateRoomAssignment(
            room_number = roomAssignment.roomNumber,
            capacity = roomAssignment.capacity.toLong(),
            assigned_participants = roomAssignment.assignedParticipants.joinToString(","),
            price_share = roomAssignment.priceShare,
            updated_at = roomAssignment.updatedAt,
            id = roomAssignment.id
        )
        return roomAssignment
    }

    /**
     * Update only the assigned participants for a room.
     *
     * @param roomId The room assignment ID
     * @param assignedParticipants List of participant IDs
     */
    fun updateAssignedParticipants(roomId: String, assignedParticipants: List<String>) {
        val now = AccommodationService.getCurrentUtcIsoString()
        roomAssignmentQueries.updateAssignedParticipants(
            assigned_participants = assignedParticipants.joinToString(","),
            updated_at = now,
            id = roomId
        )
    }

    /**
     * Delete a room assignment.
     *
     * @param roomId The room assignment ID
     */
    fun deleteRoomAssignment(roomId: String) {
        roomAssignmentQueries.deleteRoomAssignment(roomId)
    }

    /**
     * Delete all room assignments for an accommodation (will cascade via foreign key).
     *
     * @param accommodationId The accommodation ID
     */
    fun deleteRoomAssignmentsByAccommodationId(accommodationId: String) {
        roomAssignmentQueries.deleteRoomAssignmentsByAccommodationId(accommodationId)
    }

    // ==================== Statistics Operations ====================

    /**
     * Get accommodation statistics for an event.
     *
     * @param eventId The event ID
     * @return Map of statistics
     */
    fun getStatistics(eventId: String): Map<String, Any> {
        val accommodations = getAccommodationsByEventId(eventId)
        val roomAssignments = accommodations.flatMap { getRoomAssignmentsByAccommodationId(it.id) }

        val totalAccommodations = accommodations.size
        val totalCapacity = accommodations.sumOf { it.capacity }
        val totalAssignedParticipants = roomAssignments.sumOf { it.assignedParticipants.size }
        val totalCost = accommodations.sumOf { it.totalCost }
        val averageCostPerPerson = if (totalAssignedParticipants > 0) {
            totalCost / totalAssignedParticipants
        } else {
            0L
        }

        return mapOf(
            "totalAccommodations" to totalAccommodations,
            "totalCapacity" to totalCapacity,
            "totalAssigned" to totalAssignedParticipants,
            "totalCost" to totalCost,
            "averageCostPerPerson" to averageCostPerPerson
        )
    }

    // ==================== Mappers ====================
    // ==================== Mappers ====================

    /**
     * Convert SQLDelight Accommodation entity to Kotlin model.
     */
    private fun com.guyghost.wakeve.Accommodation.toModel(): Accommodation {
        return Accommodation(
            id = id,
            eventId = event_id,
            name = name,
            type = AccommodationType.valueOf(type),
            address = address,
            capacity = capacity.toInt(),
            pricePerNight = price_per_night,
            totalNights = total_nights.toInt(),
            totalCost = total_cost,
            bookingStatus = BookingStatus.valueOf(booking_status),
            bookingUrl = booking_url,
            checkInDate = check_in_date,
            checkOutDate = check_out_date,
            notes = notes,
            createdAt = created_at,
            updatedAt = updated_at
        )
    }

    /**
     * Convert SQLDelight RoomAssignment entity to Kotlin model.
     */
    private fun com.guyghost.wakeve.Room_assignment.toModel(): RoomAssignment {
        val participants = if (assigned_participants.isBlank()) {
            emptyList()
        } else {
            assigned_participants
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

        return RoomAssignment(
            id = id,
            accommodationId = accommodation_id,
            roomNumber = room_number,
            capacity = capacity.toInt(),
            assignedParticipants = participants,
            priceShare = price_share,
            createdAt = created_at,
            updatedAt = updated_at
        )
    }
}
