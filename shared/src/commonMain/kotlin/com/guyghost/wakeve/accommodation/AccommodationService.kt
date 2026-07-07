package com.guyghost.wakeve.accommodation

import com.guyghost.wakeve.models.Accommodation
import com.guyghost.wakeve.models.AccommodationWithRooms
import com.guyghost.wakeve.models.RoomAssignment
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

/**
 * Service for managing accommodations and room assignments.
 * 
 * This service provides business logic for:
 * - Creating and managing accommodations
 * - Assigning participants to rooms
 * - Calculating costs per person
 * - Validating capacity constraints
 * - Automatic room distribution algorithms
 */
object AccommodationService {

    private const val MAX_ACCOMMODATION_NAME_LENGTH = 160
    private const val MAX_ACCOMMODATION_ADDRESS_LENGTH = 300
    private const val MAX_BOOKING_URL_LENGTH = 2048
    private const val MAX_NOTES_LENGTH = 1000
    private const val MAX_CAPACITY = 10_000
    private const val MAX_TOTAL_NIGHTS = 365
    private const val MAX_ROOM_NUMBER_LENGTH = 80
    private const val MAX_ROOM_CAPACITY = 100
    private val bookingUrlPattern = Regex("^https?://\\S+$")

    /**
     * Calculate the total cost of an accommodation
     */
    fun calculateTotalCost(pricePerNight: Long, totalNights: Int): Long {
        if (pricePerNight > 0 && totalNights > 0 && pricePerNight > Long.MAX_VALUE / totalNights) {
            throw IllegalArgumentException("Total cost is too large")
        }
        return pricePerNight * totalNights
    }

    /**
     * Calculate the average cost per person for an accommodation
     * 
     * @param totalCost Total cost of accommodation in cents
     * @param participantCount Number of participants sharing the cost
     * @return Cost per person in cents
     */
    fun calculateCostPerPerson(totalCost: Long, participantCount: Int): Long {
        if (participantCount <= 0) return 0L
        return totalCost / participantCount
    }

    /**
     * Calculate the price share for a room assignment
     * 
     * @param accommodationTotalCost Total cost of the accommodation
     * @param roomCapacity Capacity of this specific room
     * @param totalAccommodationCapacity Total capacity of the accommodation
     * @param assignedParticipants Number of participants assigned to this room
     * @return Cost per person in this room in cents
     */
    fun calculateRoomPriceShare(
        accommodationTotalCost: Long,
        roomCapacity: Int,
        totalAccommodationCapacity: Int,
        assignedParticipants: Int
    ): Long {
        if (assignedParticipants <= 0) return 0L
        if (totalAccommodationCapacity <= 0) return 0L
        
        // Calculate this room's share of total cost based on its capacity
        val roomShare = (accommodationTotalCost * roomCapacity) / totalAccommodationCapacity
        
        // Divide by assigned participants
        return roomShare / assignedParticipants
    }

    /**
     * Validate that accommodation data is correct
     * 
     * @return Validation error message, or null if valid
     */
    fun validateAccommodation(
        name: String,
        capacity: Int,
        pricePerNight: Long,
        totalNights: Int,
        checkInDate: String,
        checkOutDate: String,
        address: String? = null,
        bookingUrl: String? = null,
        notes: String? = null
    ): String? {
        val normalizedName = name.trim()
        val normalizedAddress = address?.trim()
        val normalizedBookingUrl = bookingUrl?.trim()
        val normalizedNotes = notes?.trim()
        val normalizedCheckInDate = checkInDate.trim()
        val normalizedCheckOutDate = checkOutDate.trim()

        if (normalizedName.isBlank()) return "Name cannot be empty"
        if (normalizedName.length > MAX_ACCOMMODATION_NAME_LENGTH) {
            return "Name cannot exceed $MAX_ACCOMMODATION_NAME_LENGTH characters"
        }
        if (normalizedAddress != null) {
            if (normalizedAddress.isBlank()) return "Address cannot be empty"
            if (normalizedAddress.length > MAX_ACCOMMODATION_ADDRESS_LENGTH) {
                return "Address cannot exceed $MAX_ACCOMMODATION_ADDRESS_LENGTH characters"
            }
        }
        if (capacity <= 0) return "Capacity must be positive"
        if (capacity > MAX_CAPACITY) return "Capacity cannot exceed $MAX_CAPACITY"
        if (pricePerNight < 0) return "Price per night cannot be negative"
        if (totalNights <= 0) return "Total nights must be positive"
        if (totalNights > MAX_TOTAL_NIGHTS) return "Total nights cannot exceed $MAX_TOTAL_NIGHTS"
        if (pricePerNight > 0 && pricePerNight > Long.MAX_VALUE / totalNights) {
            return "Total cost is too large"
        }
        if (normalizedCheckInDate.isBlank()) return "Check-in date is required"
        if (normalizedCheckOutDate.isBlank()) return "Check-out date is required"

        // Validate that checkOut is strictly after checkIn
        val checkInParsed = runCatching { LocalDate.parse(normalizedCheckInDate) }.getOrNull()
            ?: return "Check-in date must be an ISO-8601 date"
        val checkOutParsed = runCatching { LocalDate.parse(normalizedCheckOutDate) }.getOrNull()
            ?: return "Check-out date must be an ISO-8601 date"
        if (checkOutParsed <= checkInParsed) {
            return "Check-out date must be after check-in date"
        }
        if (normalizedBookingUrl != null) {
            if (normalizedBookingUrl.isBlank()) return "Booking URL cannot be empty"
            if (normalizedBookingUrl.length > MAX_BOOKING_URL_LENGTH) {
                return "Booking URL cannot exceed $MAX_BOOKING_URL_LENGTH characters"
            }
            if (!bookingUrlPattern.matches(normalizedBookingUrl)) {
                return "Booking URL must be an http or https URL"
            }
        }
        if (normalizedNotes != null && normalizedNotes.length > MAX_NOTES_LENGTH) {
            return "Notes cannot exceed $MAX_NOTES_LENGTH characters"
        }
        
        return null
    }

    fun normalizeAccommodation(accommodation: Accommodation): Result<Accommodation> = runCatching {
        val normalizedName = accommodation.name.trim()
        val normalizedAddress = accommodation.address.trim()
        val normalizedBookingUrl = accommodation.bookingUrl?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedNotes = accommodation.notes?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedCheckInDate = accommodation.checkInDate.trim()
        val normalizedCheckOutDate = accommodation.checkOutDate.trim()

        val validationError = validateAccommodation(
            name = normalizedName,
            capacity = accommodation.capacity,
            pricePerNight = accommodation.pricePerNight,
            totalNights = accommodation.totalNights,
            checkInDate = normalizedCheckInDate,
            checkOutDate = normalizedCheckOutDate,
            address = normalizedAddress,
            bookingUrl = normalizedBookingUrl,
            notes = normalizedNotes
        )
        require(validationError == null) { validationError ?: "Invalid accommodation" }

        accommodation.copy(
            eventId = accommodation.eventId.trim(),
            name = normalizedName,
            address = normalizedAddress,
            totalCost = calculateTotalCost(accommodation.pricePerNight, accommodation.totalNights),
            bookingUrl = normalizedBookingUrl,
            checkInDate = normalizedCheckInDate,
            checkOutDate = normalizedCheckOutDate,
            notes = normalizedNotes
        )
    }

    /**
     * Validate that room assignment data is correct
     * 
     * @return Validation error message, or null if valid
     */
    fun validateRoomAssignment(
        roomNumber: String,
        capacity: Int,
        assignedParticipants: List<String>
    ): String? {
        val normalizedRoomNumber = roomNumber.trim()
        val normalizedParticipants = assignedParticipants.map { it.trim() }

        if (normalizedRoomNumber.isBlank()) return "Room number cannot be empty"
        if (normalizedRoomNumber.length > MAX_ROOM_NUMBER_LENGTH) {
            return "Room number cannot exceed $MAX_ROOM_NUMBER_LENGTH characters"
        }
        if (capacity <= 0) return "Room capacity must be positive"
        if (capacity > MAX_ROOM_CAPACITY) return "Room capacity cannot exceed $MAX_ROOM_CAPACITY"
        if (normalizedParticipants.any { it.isBlank() }) return "Assigned participants cannot contain blank IDs"
        if (normalizedParticipants.size > capacity) {
            return "Too many participants assigned (${normalizedParticipants.size} > $capacity)"
        }
        
        // Check for duplicate participant IDs
        val uniqueParticipants = normalizedParticipants.toSet()
        if (uniqueParticipants.size != normalizedParticipants.size) {
            return "Duplicate participants found in assignment"
        }
        
        return null
    }

    fun normalizeRoomAssignment(roomAssignment: RoomAssignment): Result<RoomAssignment> = runCatching {
        val normalizedParticipants = normalizeRoomParticipantIds(roomAssignment.assignedParticipants).getOrThrow()
        val validationError = validateRoomAssignment(
            roomNumber = roomAssignment.roomNumber,
            capacity = roomAssignment.capacity,
            assignedParticipants = normalizedParticipants
        )
        require(validationError == null) { validationError ?: "Invalid room assignment" }
        require(roomAssignment.priceShare >= 0) { "Room price share cannot be negative" }

        roomAssignment.copy(
            accommodationId = roomAssignment.accommodationId.trim(),
            roomNumber = roomAssignment.roomNumber.trim(),
            assignedParticipants = normalizedParticipants
        )
    }

    fun normalizeRoomParticipantIds(assignedParticipants: List<String>): Result<List<String>> = runCatching {
        val normalized = assignedParticipants.map { it.trim() }
        require(normalized.none { it.isBlank() }) { "Assigned participants cannot contain blank IDs" }
        require(normalized.toSet().size == normalized.size) { "Duplicate participants found in assignment" }
        normalized
    }

    /**
     * Check if accommodation has remaining capacity
     */
    fun hasRemainingCapacity(accommodationCapacity: Int, assignedCount: Int): Boolean {
        return assignedCount < accommodationCapacity
    }

    /**
     * Calculate remaining capacity
     */
    fun calculateRemainingCapacity(accommodationCapacity: Int, assignedCount: Int): Int {
        return (accommodationCapacity - assignedCount).coerceAtLeast(0)
    }

    /**
     * Automatically distribute participants into rooms
     * 
     * This algorithm tries to:
     * 1. Fill rooms efficiently (prefer filling rooms completely)
     * 2. Minimize the number of partially-filled rooms
     * 3. Respect room capacity constraints
     * 
     * @param participants List of participant IDs to assign
     * @param roomCapacities Map of room numbers to their capacities
     * @return Map of room numbers to assigned participant lists
     */
    fun autoAssignRooms(
        participants: List<String>,
        roomCapacities: Map<String, Int>
    ): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        
        // Initialize empty room assignments
        roomCapacities.keys.forEach { roomNumber ->
            result[roomNumber] = mutableListOf()
        }
        
        // Sort rooms by capacity (largest first) for efficient filling
        val sortedRooms = roomCapacities.entries.sortedByDescending { it.value }
        
        var participantIndex = 0
        
        // Fill rooms one by one
        for ((roomNumber, capacity) in sortedRooms) {
            val roomAssignments = result[roomNumber]!!
            
            // Fill this room up to capacity
            while (roomAssignments.size < capacity && participantIndex < participants.size) {
                roomAssignments.add(participants[participantIndex])
                participantIndex++
            }
            
            // If all participants are assigned, stop
            if (participantIndex >= participants.size) break
        }
        
        // Convert mutable lists to immutable
        return result.mapValues { it.value.toList() }
    }

    /**
     * Optimize room assignments to minimize partially-filled rooms
     * 
     * This is a more advanced algorithm that tries to balance room occupancy.
     * Use this when you want to avoid having one person alone in a large room.
     * 
     * @param participants List of participant IDs to assign
     * @param roomCapacities Map of room numbers to their capacities
     * @return Map of room numbers to assigned participant lists
     */
    fun optimizeRoomAssignments(
        participants: List<String>,
        roomCapacities: Map<String, Int>
    ): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        
        // Initialize empty room assignments
        roomCapacities.keys.forEach { roomNumber ->
            result[roomNumber] = mutableListOf()
        }
        
        val totalCapacity = roomCapacities.values.sum()
        val participantCount = participants.size
        
        if (participantCount == 0) return result.mapValues { it.value.toList() }
        if (totalCapacity < participantCount) {
            // Not enough capacity - just use auto-assign
            return autoAssignRooms(participants, roomCapacities)
        }
        
        // Calculate ideal occupancy rate
        val occupancyRate = participantCount.toDouble() / totalCapacity
        
        // Sort rooms by capacity
        val sortedRooms = roomCapacities.entries.sortedBy { it.value }
        
        var participantIndex = 0
        
        // Distribute participants proportionally based on room capacity
        for ((roomNumber, capacity) in sortedRooms) {
            val targetCount = (capacity * occupancyRate).toInt().coerceAtLeast(1)
            val actualCount = minOf(targetCount, capacity, participants.size - participantIndex)
            
            val roomAssignments = result[roomNumber]!!
            repeat(actualCount) {
                if (participantIndex < participants.size) {
                    roomAssignments.add(participants[participantIndex])
                    participantIndex++
                }
            }
            
            if (participantIndex >= participants.size) break
        }
        
        // Distribute any remaining participants
        for ((roomNumber, capacity) in sortedRooms) {
            val roomAssignments = result[roomNumber]!!
            while (roomAssignments.size < capacity && participantIndex < participants.size) {
                roomAssignments.add(participants[participantIndex])
                participantIndex++
            }
            if (participantIndex >= participants.size) break
        }
        
        return result.mapValues { it.value.toList() }
    }

    /**
     * Find unassigned participants
     * 
     * @param allParticipants All event participants
     * @param roomAssignments Current room assignments
     * @return List of participant IDs not assigned to any room
     */
    fun findUnassignedParticipants(
        allParticipants: List<String>,
        roomAssignments: List<RoomAssignment>
    ): List<String> {
        val assignedParticipants = roomAssignments
            .flatMap { it.assignedParticipants }
            .toSet()
        
        return allParticipants.filter { it !in assignedParticipants }
    }

    /**
     * Check if a participant is assigned to any room in an accommodation
     */
    fun isParticipantAssigned(
        participantId: String,
        roomAssignments: List<RoomAssignment>
    ): Boolean {
        return roomAssignments.any { participantId in it.assignedParticipants }
    }

    /**
     * Get the room assignment for a specific participant
     */
    fun getRoomForParticipant(
        participantId: String,
        roomAssignments: List<RoomAssignment>
    ): RoomAssignment? {
        return roomAssignments.find { participantId in it.assignedParticipants }
    }

    /**
     * Calculate statistics for accommodation with rooms
     */
    fun calculateAccommodationStats(
        accommodation: Accommodation,
        roomAssignments: List<RoomAssignment>
    ): AccommodationWithRooms {
        val totalAssigned = roomAssignments.sumOf { it.assignedParticipants.size }
        val remainingCapacity = calculateRemainingCapacity(accommodation.capacity, totalAssigned)
        val avgCost = if (totalAssigned > 0) accommodation.totalCost / totalAssigned else 0L
        
        return AccommodationWithRooms(
            accommodation = accommodation,
            roomAssignments = roomAssignments,
            totalAssignedParticipants = totalAssigned,
            remainingCapacity = remainingCapacity,
            averageCostPerPerson = avgCost
        )
    }

    /**
     * Get current UTC timestamp in ISO 8601 format
     */
    fun getCurrentUtcIsoString(): String {
        return Clock.System.now().toString()
    }

    /**
     * Validate that total cost matches price per night * total nights
     */
    fun validateTotalCost(pricePerNight: Long, totalNights: Int, totalCost: Long): Boolean {
        return totalCost == calculateTotalCost(pricePerNight, totalNights)
    }
}
