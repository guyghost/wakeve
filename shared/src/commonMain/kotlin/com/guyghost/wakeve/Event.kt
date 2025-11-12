package com.guyghost.wakeve

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val organizerId: String,
    val proposedSlots: List<TimeSlot>,
    val deadline: String, // ISO string
    val status: EventStatus,
    val finalDate: String? = null // ISO string
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