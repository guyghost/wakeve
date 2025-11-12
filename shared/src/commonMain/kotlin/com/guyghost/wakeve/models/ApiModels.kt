package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * API Request/Response models for the Wakev backend.
 * These are separate from domain models to allow for API evolution independent of domain changes.
 */

@Serializable
data class CreateEventRequest(
    val title: String,
    val description: String,
    val organizerId: String,
    val deadline: String,
    val proposedSlots: List<CreateTimeSlotRequest>
)

@Serializable
data class CreateTimeSlotRequest(
    val id: String,
    val start: String,
    val end: String,
    val timezone: String
)

@Serializable
data class EventResponse(
    val id: String,
    val title: String,
    val description: String,
    val organizerId: String,
    val participants: List<String>,
    val deadline: String,
    val status: String,
    val proposedSlots: List<TimeSlotResponse>,
    val finalDate: String? = null
)

@Serializable
data class TimeSlotResponse(
    val id: String,
    val start: String,
    val end: String,
    val timezone: String
)

@Serializable
data class AddParticipantRequest(
    val eventId: String,
    val participantId: String
)

@Serializable
data class AddVoteRequest(
    val eventId: String,
    val participantId: String,
    val slotId: String,
    val vote: String  // YES, MAYBE, NO
)

@Serializable
data class PollResponse(
    val eventId: String,
    val votes: Map<String, Map<String, String>>  // participantId -> slotId -> vote
)

@Serializable
data class UpdateEventStatusRequest(
    val eventId: String,
    val status: String,  // DRAFT, POLLING, CONFIRMED
    val finalDate: String? = null
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null
)
