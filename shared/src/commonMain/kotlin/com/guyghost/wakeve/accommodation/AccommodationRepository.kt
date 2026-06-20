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
class AccommodationRepository(private val db: com.guyghost.wakeve.database.WakeveDb) {

    private val accommodationQueries = db.accommodationQueries
    private val roomAssignmentQueries = db.roomAssignmentQueries
    private val syncMetadataQueries = db.syncMetadataQueries

    // ==================== Accommodation Operations ====================

    /**
     * Create a new accommodation.
     *
     * @param accommodation The accommodation to create
     * @return Created accommodation
     */
    fun createAccommodation(accommodation: Accommodation): Accommodation {
        val normalized = AccommodationService.normalizeAccommodation(accommodation).getOrThrow()
        accommodationQueries.insertAccommodation(
            id = normalized.id,
            event_id = normalized.eventId,
            name = normalized.name,
            type = normalized.type.name,
            address = normalized.address,
            capacity = normalized.capacity.toLong(),
            price_per_night = normalized.pricePerNight,
            total_nights = normalized.totalNights.toLong(),
            total_cost = normalized.totalCost,
            booking_status = normalized.bookingStatus.name,
            booking_url = normalized.bookingUrl,
            check_in_date = normalized.checkInDate,
            check_out_date = normalized.checkOutDate,
            notes = normalized.notes,
            created_at = normalized.createdAt,
            updated_at = normalized.updatedAt
        )
        return normalized
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
        val normalized = AccommodationService.normalizeAccommodation(accommodation).getOrThrow()
        accommodationQueries.updateAccommodation(
            name = normalized.name,
            type = normalized.type.name,
            address = normalized.address,
            capacity = normalized.capacity.toLong(),
            price_per_night = normalized.pricePerNight,
            total_nights = normalized.totalNights.toLong(),
            total_cost = normalized.totalCost,
            booking_status = normalized.bookingStatus.name,
            booking_url = normalized.bookingUrl,
            check_in_date = normalized.checkInDate,
            check_out_date = normalized.checkOutDate,
            notes = normalized.notes,
            updated_at = normalized.updatedAt,
            id = normalized.id
        )
        return normalized
    }

    /**
     * Update only the booking status of an accommodation.
     *
     * @param accommodationId The accommodation ID
     * @param status The new booking status
     */
    fun updateBookingStatus(accommodationId: String, status: BookingStatus) {
        val now = AccommodationService.getCurrentUtcIsoString()
        val accommodation = getAccommodationById(accommodationId) ?: return
        db.transaction {
            if (status == BookingStatus.CONFIRMED) {
                getAccommodationsByEventId(accommodation.eventId)
                    .filter { it.id != accommodationId && it.bookingStatus == BookingStatus.CONFIRMED }
                    .forEach { competing ->
                        accommodationQueries.updateBookingStatus(
                            booking_status = BookingStatus.RESERVED.name,
                            updated_at = now,
                            id = competing.id
                        )
                    }
            }

            accommodationQueries.updateBookingStatus(
                booking_status = status.name,
                updated_at = now,
                id = accommodationId
            )

            if (status == BookingStatus.CONFIRMED) {
                queueSyncMetadata(
                    id = "sync_lodging_selection_${accommodation.eventId}",
                    entityType = "lodging_selection",
                    entityId = accommodation.eventId,
                    operation = "CONFLICT_RESOLVED",
                    timestamp = "${now}_${accommodationId}"
                )
            }
        }
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
        val normalized = AccommodationService.normalizeRoomAssignment(roomAssignment).getOrThrow()
        roomAssignmentQueries.insertRoomAssignment(
            id = normalized.id,
            accommodation_id = normalized.accommodationId,
            room_number = normalized.roomNumber,
            capacity = normalized.capacity.toLong(),
            assigned_participants = normalized.assignedParticipants.joinToString(","),
            price_share = normalized.priceShare,
            created_at = normalized.createdAt,
            updated_at = normalized.updatedAt
        )
        return normalized
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
        val normalized = AccommodationService.normalizeRoomAssignment(roomAssignment).getOrThrow()
        roomAssignmentQueries.updateRoomAssignment(
            room_number = normalized.roomNumber,
            capacity = normalized.capacity.toLong(),
            assigned_participants = normalized.assignedParticipants.joinToString(","),
            price_share = normalized.priceShare,
            updated_at = normalized.updatedAt,
            id = normalized.id
        )
        return normalized
    }

    /**
     * Update only the assigned participants for a room.
     *
     * @param roomId The room assignment ID
     * @param assignedParticipants List of participant IDs
     */
    fun updateAssignedParticipants(roomId: String, assignedParticipants: List<String>) {
        val now = AccommodationService.getCurrentUtcIsoString()
        val normalized = AccommodationService.normalizeRoomParticipantIds(assignedParticipants).getOrThrow()
        roomAssignmentQueries.updateAssignedParticipants(
            assigned_participants = normalized.joinToString(","),
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

    private fun queueSyncMetadata(
        id: String,
        entityType: String,
        entityId: String,
        operation: String,
        timestamp: String
    ) {
        val existingForEntity = syncMetadataQueries.selectByEntity(entityType, entityId).executeAsList()
        val uniqueId = if (syncMetadataQueries.selectById(id).executeAsOneOrNull() == null) {
            id
        } else {
            "${id}_${existingForEntity.size}"
        }
        val uniqueTimestamp = if (existingForEntity.none { it.timestamp == timestamp }) {
            timestamp
        } else {
            "${timestamp}_${existingForEntity.size}"
        }
        syncMetadataQueries.insertSyncMetadata(
            id = uniqueId,
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            timestamp = uniqueTimestamp,
            synced = 0
        )
    }
}
