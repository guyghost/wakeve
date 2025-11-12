package com.guyghost.wakeve

class EventRepository {
    private val events = mutableMapOf<String, Event>()
    private val polls = mutableMapOf<String, Poll>()

    fun createEvent(event: Event) {
        events[event.id] = event
        polls[event.id] = Poll(event.id, event.id, emptyMap())
    }

    fun getEvent(id: String): Event? = events[id]

    fun getPoll(eventId: String): Poll? = polls[eventId]

    fun addVote(eventId: String, participantId: String, slotId: String, vote: Vote) {
        val poll = polls[eventId] ?: return
        val participantVotes = poll.votes[participantId]?.toMutableMap() ?: mutableMapOf()
        participantVotes[slotId] = vote
        polls[eventId] = poll.copy(votes = poll.votes + (participantId to participantVotes))
    }

    fun updateEventStatus(id: String, status: EventStatus, finalDate: String? = null) {
        events[id]?.let { event ->
            events[id] = event.copy(status = status, finalDate = finalDate)
        }
    }
}