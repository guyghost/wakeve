package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Type of accommodation
 */
@Serializable
enum class AccommodationType {
    HOTEL,
    AIRBNB,
    CAMPING,
    HOSTEL,
    VACATION_RENTAL,
    OTHER
}

/**
 * Booking status for accommodation
 */
@Serializable
enum class BookingStatus {
    SEARCHING,   // Looking for options
    RESERVED,    // Reserved but not paid
    CONFIRMED,   // Paid and confirmed
    CANCELLED    // Cancelled
}

/**
 * Accommodation for an event
 * 
 * Represents a place where participants will stay during the event.
 * Can be a hotel, Airbnb, camping site, etc.
 * 
 * @property id Unique identifier
 * @property eventId Event this accommodation belongs to
 * @property name Name of the accommodation (e.g., "Hotel California")
 * @property type Type of accommodation
 * @property address Full address
 * @property capacity Maximum number of people it can host
 * @property pricePerNight Price per night (in cents to avoid float precision issues)
 * @property totalNights Number of nights booked
 * @property totalCost Total cost (pricePerNight * totalNights in cents)
 * @property bookingStatus Current booking status
 * @property bookingUrl URL to booking page or confirmation
 * @property checkInDate Check-in date (ISO 8601 format)
 * @property checkOutDate Check-out date (ISO 8601 format)
 * @property notes Additional notes or special requirements
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
@Serializable
data class Accommodation(
    val id: String,
    val eventId: String,
    val name: String,
    val type: AccommodationType,
    val address: String,
    val capacity: Int,
    val pricePerNight: Long,  // In cents
    val totalNights: Int,
    val totalCost: Long,      // In cents (pricePerNight * totalNights)
    val bookingStatus: BookingStatus,
    val bookingUrl: String? = null,
    val checkInDate: String,  // ISO 8601 date (e.g., "2025-12-20")
    val checkOutDate: String, // ISO 8601 date (e.g., "2025-12-25")
    val notes: String? = null,
    val createdAt: String,    // ISO 8601 UTC timestamp
    val updatedAt: String     // ISO 8601 UTC timestamp
)

/**
 * Room assignment within an accommodation
 * 
 * Assigns participants to specific rooms within an accommodation.
 * Helps organize who sleeps where and calculate per-person costs.
 * 
 * @property id Unique identifier
 * @property accommodationId Accommodation this room belongs to
 * @property roomNumber Room number or identifier (e.g., "101", "Room A")
 * @property capacity Maximum occupancy of this room
 * @property assignedParticipants List of participant IDs assigned to this room
 * @property priceShare Cost per person for this room (in cents)
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
@Serializable
data class RoomAssignment(
    val id: String,
    val accommodationId: String,
    val roomNumber: String,
    val capacity: Int,
    val assignedParticipants: List<String>,  // Participant IDs
    val priceShare: Long,  // Cost per person in cents
    val createdAt: String, // ISO 8601 UTC timestamp
    val updatedAt: String  // ISO 8601 UTC timestamp
)

/**
 * Summary of accommodation with room assignments
 * 
 * Used to present a complete view of accommodation with all rooms and assignments.
 * 
 * @property accommodation The accommodation details
 * @property roomAssignments List of room assignments for this accommodation
 * @property totalAssignedParticipants Count of assigned participants
 * @property remainingCapacity Number of spots still available
 * @property averageCostPerPerson Average cost per assigned participant (in cents)
 */
@Serializable
data class AccommodationWithRooms(
    val accommodation: Accommodation,
    val roomAssignments: List<RoomAssignment>,
    val totalAssignedParticipants: Int,
    val remainingCapacity: Int,
    val averageCostPerPerson: Long  // In cents
)

/**
 * Participant accommodation details
 * 
 * Shows where a specific participant is staying.
 * 
 * @property participantId The participant's ID
 * @property accommodation The accommodation they're assigned to
 * @property roomAssignment The specific room assignment
 * @property costShare Their share of the accommodation cost (in cents)
 */
@Serializable
data class ParticipantAccommodation(
    val participantId: String,
    val accommodation: Accommodation,
    val roomAssignment: RoomAssignment,
    val costShare: Long  // In cents
)

/**
 * Request to create or update an accommodation
 */
@Serializable
data class AccommodationRequest(
    val eventId: String,
    val name: String,
    val type: AccommodationType,
    val address: String,
    val capacity: Int,
    val pricePerNight: Long,  // In cents
    val totalNights: Int,
    val bookingStatus: BookingStatus = BookingStatus.SEARCHING,
    val bookingUrl: String? = null,
    val checkInDate: String,
    val checkOutDate: String,
    val notes: String? = null
)

/**
 * Request to create or update a room assignment
 */
@Serializable
data class RoomAssignmentRequest(
    val accommodationId: String,
    val roomNumber: String,
    val capacity: Int,
    val assignedParticipants: List<String>
)
