package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val organizerId: String,  // Keep as string for backward compatibility, will be replaced with User reference
    val participants: List<String> = emptyList(), // List of participant IDs/emails - will be replaced with User references
    val proposedSlots: List<TimeSlot>,
    val deadline: String, // ISO string (UTC)
    val status: EventStatus,
    val finalDate: String? = null, // ISO string (UTC)
    val createdAt: String, // ISO string (UTC)
    val updatedAt: String // ISO string (UTC)
)

@Serializable
enum class EventStatus {
    /** Event is being drafted, not yet shared with participants */
    DRAFT,
    
    /** Date polling is active, participants are voting on time slots */
    POLLING,
    
    /** Multiple planning scenarios are being compared */
    COMPARING,
    
    /** Date and scenario are confirmed, ready for detailed planning */
    CONFIRMED,
    
    /** Detailed logistics (transport, accommodation, meals, etc.) are being organized */
    ORGANIZING,
    
    /** All details are finalized and confirmed */
    FINALIZED
}

@Serializable
data class TimeSlot(
    val id: String,
    val start: String, // ISO string
    val end: String, // ISO string
    val timezone: String
)

data class Poll(
    val id: String,
    val eventId: String,
    val votes: Map<String, Map<String, Vote>> // participantId -> slotId -> vote
)

enum class Vote {
    YES, MAYBE, NO
}