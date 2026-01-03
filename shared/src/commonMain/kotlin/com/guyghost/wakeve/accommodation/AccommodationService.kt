package com.guyghost.wakeve.accommodation

import com.guyghost.wakeve.models.Accommodation
import com.guyghost.wakeve.models.AccommodationWithRooms
import com.guyghost.wakeve.models.RoomAssignment
import kotlinx.datetime.Clock

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

    /**
     * Calculate the total cost of an accommodation
     */
    fun calculateTotalCost(pricePerNight: Long, totalNights: Int): Long {
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
        checkOutDate: String
    ): String? {
        if (name.isBlank()) return "Name cannot be empty"
        if (capacity <= 0) return "Capacity must be positive"
        if (pricePerNight < 0) return "Price per night cannot be negative"
        if (totalNights <= 0) return "Total nights must be positive"
        if (checkInDate.isBlank()) return "Check-in date is required"
        if (checkOutDate.isBlank()) return "Check-out date is required"
        
        // TODO: Add date validation (checkOut > checkIn)
        
        return null
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
        if (roomNumber.isBlank()) return "Room number cannot be empty"
        if (capacity <= 0) return "Room capacity must be positive"
        if (assignedParticipants.size > capacity) {
            return "Too many participants assigned (${assignedParticipants.size} > $capacity)"
        }
        
        // Check for duplicate participant IDs
        val uniqueParticipants = assignedParticipants.toSet()
        if (uniqueParticipants.size != assignedParticipants.size) {
            return "Duplicate participants found in assignment"
        }
        
        return null
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
