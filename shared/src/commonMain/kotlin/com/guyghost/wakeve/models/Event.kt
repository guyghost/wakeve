package com.guyghost.wakeve.models

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val organizerId: String,
    val participants: List<String> = emptyList(), // List of participant IDs/emails
    val proposedSlots: List<TimeSlot>,
    val deadline: String, // ISO string (UTC)
    val status: EventStatus,
    val finalDate: String? = null // ISO string (UTC)
)

enum class EventStatus {
    DRAFT, POLLING, CONFIRMED
}

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