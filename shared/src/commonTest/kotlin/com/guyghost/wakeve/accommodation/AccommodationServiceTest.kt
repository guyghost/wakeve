package com.guyghost.wakeve.accommodation

import com.guyghost.wakeve.models.*
import kotlin.test.*

/**
 * Unit tests for AccommodationService
 * 
 * Tests all business logic functions for accommodation and room assignment management.
 */
class AccommodationServiceTest {

    // Test data
    private val testAccommodation = Accommodation(
        id = "acc-1",
        eventId = "event-1",
        name = "Hotel California",
        type = AccommodationType.HOTEL,
        address = "123 Main St, Los Angeles, CA",
        capacity = 10,
        pricePerNight = 20000, // $200 in cents
        totalNights = 3,
        totalCost = 60000, // $600
        bookingStatus = BookingStatus.CONFIRMED,
        bookingUrl = "https://booking.com/hotel-california",
        checkInDate = "2025-12-20",
        checkOutDate = "2025-12-23",
        notes = "Free breakfast included",
        createdAt = "2025-12-01T10:00:00Z",
        updatedAt = "2025-12-01T10:00:00Z"
    )

    private val testRoomAssignment = RoomAssignment(
        id = "room-1",
        accommodationId = "acc-1",
        roomNumber = "101",
        capacity = 2,
        assignedParticipants = listOf("user-1", "user-2"),
        priceShare = 10000, // $100 per person
        createdAt = "2025-12-01T10:00:00Z",
        updatedAt = "2025-12-01T10:00:00Z"
    )

    @Test
    fun `calculateTotalCost calculates correctly`() {
        val total = AccommodationService.calculateTotalCost(20000, 3)
        assertEquals(60000, total)
    }

    @Test
    fun `calculateTotalCost handles zero nights`() {
        val total = AccommodationService.calculateTotalCost(20000, 0)
        assertEquals(0, total)
    }

    @Test
    fun `calculateCostPerPerson divides correctly`() {
        val costPerPerson = AccommodationService.calculateCostPerPerson(60000, 6)
        assertEquals(10000, costPerPerson) // $100 per person
    }

    @Test
    fun `calculateCostPerPerson handles zero participants`() {
        val costPerPerson = AccommodationService.calculateCostPerPerson(60000, 0)
        assertEquals(0, costPerPerson)
    }

    @Test
    fun `calculateCostPerPerson handles division with remainder`() {
        val costPerPerson = AccommodationService.calculateCostPerPerson(100, 3)
        assertEquals(33, costPerPerson) // Integer division: 100/3 = 33
    }

    @Test
    fun `calculateRoomPriceShare calculates proportionally`() {
        // Accommodation: 60000 total, capacity 10
        // Room: capacity 2, assigned 2 participants
        // Room share: (60000 * 2) / 10 = 12000
        // Per person: 12000 / 2 = 6000
        val priceShare = AccommodationService.calculateRoomPriceShare(
            accommodationTotalCost = 60000,
            roomCapacity = 2,
            totalAccommodationCapacity = 10,
            assignedParticipants = 2
        )
        assertEquals(6000, priceShare)
    }

    @Test
    fun `calculateRoomPriceShare handles zero participants`() {
        val priceShare = AccommodationService.calculateRoomPriceShare(
            accommodationTotalCost = 60000,
            roomCapacity = 2,
            totalAccommodationCapacity = 10,
            assignedParticipants = 0
        )
        assertEquals(0, priceShare)
    }

    @Test
    fun `validateAccommodation passes with valid data`() {
        val error = AccommodationService.validateAccommodation(
            name = "Hotel California",
            capacity = 10,
            pricePerNight = 20000,
            totalNights = 3,
            checkInDate = "2025-12-20",
            checkOutDate = "2025-12-23"
        )
        assertNull(error)
    }

    @Test
    fun `validateAccommodation rejects empty name`() {
        val error = AccommodationService.validateAccommodation(
            name = "",
            capacity = 10,
            pricePerNight = 20000,
            totalNights = 3,
            checkInDate = "2025-12-20",
            checkOutDate = "2025-12-23"
        )
        assertNotNull(error)
        assertTrue(error.contains("Name"))
    }

    @Test
    fun `validateAccommodation rejects zero capacity`() {
        val error = AccommodationService.validateAccommodation(
            name = "Hotel",
            capacity = 0,
            pricePerNight = 20000,
            totalNights = 3,
            checkInDate = "2025-12-20",
            checkOutDate = "2025-12-23"
        )
        assertNotNull(error)
        assertTrue(error.contains("Capacity"))
    }

    @Test
    fun `validateAccommodation rejects negative price`() {
        val error = AccommodationService.validateAccommodation(
            name = "Hotel",
            capacity = 10,
            pricePerNight = -1000,
            totalNights = 3,
            checkInDate = "2025-12-20",
            checkOutDate = "2025-12-23"
        )
        assertNotNull(error)
        assertTrue(error.contains("Price"))
    }

    @Test
    fun `validateAccommodation rejects zero nights`() {
        val error = AccommodationService.validateAccommodation(
            name = "Hotel",
            capacity = 10,
            pricePerNight = 20000,
            totalNights = 0,
            checkInDate = "2025-12-20",
            checkOutDate = "2025-12-23"
        )
        assertNotNull(error)
        assertTrue(error.contains("nights"))
    }

    @Test
    fun `validateRoomAssignment passes with valid data`() {
        val error = AccommodationService.validateRoomAssignment(
            roomNumber = "101",
            capacity = 2,
            assignedParticipants = listOf("user-1", "user-2")
        )
        assertNull(error)
    }

    @Test
    fun `validateRoomAssignment rejects empty room number`() {
        val error = AccommodationService.validateRoomAssignment(
            roomNumber = "",
            capacity = 2,
            assignedParticipants = listOf("user-1")
        )
        assertNotNull(error)
        assertTrue(error.contains("Room number"))
    }

    @Test
    fun `validateRoomAssignment rejects too many participants`() {
        val error = AccommodationService.validateRoomAssignment(
            roomNumber = "101",
            capacity = 2,
            assignedParticipants = listOf("user-1", "user-2", "user-3")
        )
        assertNotNull(error)
        assertTrue(error.contains("Too many"))
    }

    @Test
    fun `validateRoomAssignment rejects duplicate participants`() {
        val error = AccommodationService.validateRoomAssignment(
            roomNumber = "101",
            capacity = 3,
            assignedParticipants = listOf("user-1", "user-2", "user-1") // Duplicate user-1
        )
        assertNotNull(error)
        assertTrue(error.contains("Duplicate"))
    }

    @Test
    fun `hasRemainingCapacity returns true when capacity available`() {
        val hasCapacity = AccommodationService.hasRemainingCapacity(10, 6)
        assertTrue(hasCapacity)
    }

    @Test
    fun `hasRemainingCapacity returns false when at capacity`() {
        val hasCapacity = AccommodationService.hasRemainingCapacity(10, 10)
        assertFalse(hasCapacity)
    }

    @Test
    fun `hasRemainingCapacity returns false when over capacity`() {
        val hasCapacity = AccommodationService.hasRemainingCapacity(10, 12)
        assertFalse(hasCapacity)
    }

    @Test
    fun `calculateRemainingCapacity returns correct value`() {
        val remaining = AccommodationService.calculateRemainingCapacity(10, 6)
        assertEquals(4, remaining)
    }

    @Test
    fun `calculateRemainingCapacity returns zero when at capacity`() {
        val remaining = AccommodationService.calculateRemainingCapacity(10, 10)
        assertEquals(0, remaining)
    }

    @Test
    fun `calculateRemainingCapacity returns zero when over capacity`() {
        val remaining = AccommodationService.calculateRemainingCapacity(10, 12)
        assertEquals(0, remaining)
    }

    @Test
    fun `autoAssignRooms distributes participants efficiently`() {
        val participants = listOf("user-1", "user-2", "user-3", "user-4", "user-5")
        val roomCapacities = mapOf(
            "101" to 3,
            "102" to 2,
            "103" to 1
        )

        val assignments = AccommodationService.autoAssignRooms(participants, roomCapacities)

        // Should fill largest room first (101 with capacity 3)
        assertEquals(3, assignments["101"]?.size)
        assertEquals(2, assignments["102"]?.size)
        assertEquals(0, assignments["103"]?.size)

        // All participants should be assigned
        val allAssigned = assignments.values.flatten()
        assertEquals(5, allAssigned.size)
        assertEquals(participants.toSet(), allAssigned.toSet())
    }

    @Test
    fun `autoAssignRooms handles exact fit`() {
        val participants = listOf("user-1", "user-2", "user-3", "user-4")
        val roomCapacities = mapOf(
            "101" to 2,
            "102" to 2
        )

        val assignments = AccommodationService.autoAssignRooms(participants, roomCapacities)

        assertEquals(2, assignments["101"]?.size)
        assertEquals(2, assignments["102"]?.size)
    }

    @Test
    fun `autoAssignRooms handles more capacity than participants`() {
        val participants = listOf("user-1", "user-2")
        val roomCapacities = mapOf(
            "101" to 4,
            "102" to 3
        )

        val assignments = AccommodationService.autoAssignRooms(participants, roomCapacities)

        // Should use only one room (largest)
        assertEquals(2, assignments["101"]?.size)
        assertEquals(0, assignments["102"]?.size)
    }

    @Test
    fun `autoAssignRooms handles empty participants list`() {
        val participants = emptyList<String>()
        val roomCapacities = mapOf(
            "101" to 2,
            "102" to 2
        )

        val assignments = AccommodationService.autoAssignRooms(participants, roomCapacities)

        assertEquals(0, assignments["101"]?.size)
        assertEquals(0, assignments["102"]?.size)
    }

    @Test
    fun `optimizeRoomAssignments balances occupancy`() {
        val participants = listOf("user-1", "user-2", "user-3", "user-4", "user-5")
        val roomCapacities = mapOf(
            "101" to 4, // Can fit 4
            "102" to 4  // Can fit 4
        )

        val assignments = AccommodationService.optimizeRoomAssignments(participants, roomCapacities)

        // Should distribute more evenly than autoAssign
        val room1Count = assignments["101"]?.size ?: 0
        val room2Count = assignments["102"]?.size ?: 0

        // Total should be 5
        assertEquals(5, room1Count + room2Count)

        // Should be relatively balanced (not all in one room)
        assertTrue(room1Count > 0 && room2Count > 0, "Both rooms should have participants")

        // All participants assigned
        val allAssigned = assignments.values.flatten()
        assertEquals(participants.toSet(), allAssigned.toSet())
    }

    @Test
    fun `findUnassignedParticipants identifies unassigned correctly`() {
        val allParticipants = listOf("user-1", "user-2", "user-3", "user-4", "user-5")
        val roomAssignments = listOf(
            testRoomAssignment.copy(assignedParticipants = listOf("user-1", "user-2")),
            testRoomAssignment.copy(id = "room-2", roomNumber = "102", assignedParticipants = listOf("user-3"))
        )

        val unassigned = AccommodationService.findUnassignedParticipants(allParticipants, roomAssignments)

        assertEquals(2, unassigned.size)
        assertTrue(unassigned.contains("user-4"))
        assertTrue(unassigned.contains("user-5"))
    }

    @Test
    fun `findUnassignedParticipants returns empty when all assigned`() {
        val allParticipants = listOf("user-1", "user-2")
        val roomAssignments = listOf(
            testRoomAssignment.copy(assignedParticipants = listOf("user-1", "user-2"))
        )

        val unassigned = AccommodationService.findUnassignedParticipants(allParticipants, roomAssignments)

        assertTrue(unassigned.isEmpty())
    }

    @Test
    fun `isParticipantAssigned returns true when assigned`() {
        val roomAssignments = listOf(testRoomAssignment)

        val isAssigned = AccommodationService.isParticipantAssigned("user-1", roomAssignments)

        assertTrue(isAssigned)
    }

    @Test
    fun `isParticipantAssigned returns false when not assigned`() {
        val roomAssignments = listOf(testRoomAssignment)

        val isAssigned = AccommodationService.isParticipantAssigned("user-3", roomAssignments)

        assertFalse(isAssigned)
    }

    @Test
    fun `getRoomForParticipant returns correct room`() {
        val roomAssignments = listOf(
            testRoomAssignment.copy(assignedParticipants = listOf("user-1", "user-2")),
            testRoomAssignment.copy(id = "room-2", roomNumber = "102", assignedParticipants = listOf("user-3"))
        )

        val room = AccommodationService.getRoomForParticipant("user-3", roomAssignments)

        assertNotNull(room)
        assertEquals("102", room.roomNumber)
    }

    @Test
    fun `getRoomForParticipant returns null when not assigned`() {
        val roomAssignments = listOf(testRoomAssignment)

        val room = AccommodationService.getRoomForParticipant("user-999", roomAssignments)

        assertNull(room)
    }

    @Test
    fun `calculateAccommodationStats calculates correctly`() {
        val roomAssignments = listOf(
            testRoomAssignment.copy(assignedParticipants = listOf("user-1", "user-2")),
            testRoomAssignment.copy(id = "room-2", roomNumber = "102", assignedParticipants = listOf("user-3", "user-4"))
        )

        val stats = AccommodationService.calculateAccommodationStats(testAccommodation, roomAssignments)

        assertEquals(testAccommodation, stats.accommodation)
        assertEquals(2, stats.roomAssignments.size)
        assertEquals(4, stats.totalAssignedParticipants)
        assertEquals(6, stats.remainingCapacity) // 10 capacity - 4 assigned
        assertEquals(15000, stats.averageCostPerPerson) // 60000 / 4
    }

    @Test
    fun `calculateAccommodationStats handles zero assigned participants`() {
        val roomAssignments = emptyList<RoomAssignment>()

        val stats = AccommodationService.calculateAccommodationStats(testAccommodation, roomAssignments)

        assertEquals(0, stats.totalAssignedParticipants)
        assertEquals(10, stats.remainingCapacity)
        assertEquals(0, stats.averageCostPerPerson)
    }

    @Test
    fun `validateTotalCost returns true for correct calculation`() {
        val isValid = AccommodationService.validateTotalCost(20000, 3, 60000)
        assertTrue(isValid)
    }

    @Test
    fun `validateTotalCost returns false for incorrect calculation`() {
        val isValid = AccommodationService.validateTotalCost(20000, 3, 50000)
        assertFalse(isValid)
    }

    @Test
    fun `getCurrentUtcIsoString returns valid ISO format`() {
        val timestamp = AccommodationService.getCurrentUtcIsoString()

        // Should be in ISO 8601 format: 2025-12-25T10:00:00Z
        assertNotNull(timestamp)
        assertTrue(timestamp.contains("T"))
        assertTrue(timestamp.endsWith("Z"))
    }
}
