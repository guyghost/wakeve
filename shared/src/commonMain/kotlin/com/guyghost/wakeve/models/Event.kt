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
    val updatedAt: String, // ISO string (UTC)
    
    // Enhanced DRAFT phase fields (enhance-draft-phase)
    val eventType: EventType = EventType.OTHER,
    val eventTypeCustom: String? = null, // Required if eventType == CUSTOM
    val minParticipants: Int? = null,
    val maxParticipants: Int? = null,
    val expectedParticipants: Int? = null
) {
    /**
     * Validate that the event data is consistent.
     * Returns null if valid, error message if invalid.
     */
    fun validate(): String? {
        // EventType.CUSTOM requires eventTypeCustom
        if (eventType == EventType.CUSTOM && eventTypeCustom.isNullOrBlank()) {
            return "Custom event type requires a description"
        }
        
        // maxParticipants must be >= minParticipants
        if (minParticipants != null && maxParticipants != null && maxParticipants < minParticipants) {
            return "Maximum participants must be greater than or equal to minimum"
        }
        
        // Participants counts must be positive
        if (minParticipants != null && minParticipants < 1) {
            return "Minimum participants must be at least 1"
        }
        if (maxParticipants != null && maxParticipants < 1) {
            return "Maximum participants must be at least 1"
        }
        if (expectedParticipants != null && expectedParticipants < 1) {
            return "Expected participants must be at least 1"
        }
        
        return null
    }
}

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
    val start: String?, // ISO string, nullable for flexible slots
    val end: String?,   // ISO string, nullable for flexible slots
    val timezone: String,
    val timeOfDay: TimeOfDay = TimeOfDay.SPECIFIC // Time of day indication
) {
    /**
     * Validate that the time slot is consistent.
     * Returns null if valid, error message if invalid.
     */
    fun validate(): String? {
        // SPECIFIC timeOfDay requires start and end times
        if (timeOfDay == TimeOfDay.SPECIFIC && (start == null || end == null)) {
            return "Specific time slots require start and end times"
        }
        
        return null
    }
}

data class Poll(
    val id: String,
    val eventId: String,
    val votes: Map<String, Map<String, Vote>> // participantId -> slotId -> vote
)

enum class Vote {
    YES, MAYBE, NO
}