package com.guyghost.wakeve.voice.handlers

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.ml.VoiceCommand
import com.guyghost.wakeve.ml.VoiceSession
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Handler for poll management voice commands.
 */
class VoicePollHandlers(
    private val eventRepository: EventRepositoryInterface
) {
    private fun generateId(): String = "slot_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"

    /**
     * Handles the ADD_SLOT intent.
     */
    suspend fun handleAddSlot(
        session: VoiceSession,
        command: VoiceCommand
    ): Result<String> {
        val eventId = session.context.eventId
            ?: command.parameters["eventId"]
            ?: return Result.failure(IllegalStateException("No event specified"))

        val event = eventRepository.getEvent(eventId) ?: return Result.failure(
            IllegalStateException("Event not found")
        )

        // Check if event is in POLLING status
        if (event.status != EventStatus.POLLING) {
            return Result.failure(
                IllegalStateException("Cannot add slots: Event is not in POLLING status")
            )
        }

        val dateString = command.parameters["date"]
            ?: return Result.failure(IllegalArgumentException("No date provided"))

        val timeOfDay = command.parameters["timeOfDay"]?.let {
            runCatching { TimeOfDay.valueOf(it) }.getOrNull()
        } ?: TimeOfDay.SPECIFIC

        // Create new time slot
        val slotId = generateId()
        val timeZone = TimeZone.currentSystemDefault().id

        val timeSlot = TimeSlot(
            id = slotId,
            start = dateString,
            end = null,
            timezone = timeZone,
            timeOfDay = timeOfDay
        )

        val updatedEvent = event.copy(
            proposedSlots = event.proposedSlots + timeSlot,
            updatedAt = getCurrentTimestamp()
        )

        eventRepository.updateEvent(updatedEvent)
        return Result.success(slotId)
    }

    /**
     * Handles the CONFIRM_POLL intent.
     * Locks in the winning date and transitions event to CONFIRMED status.
     *
     * @param session The current voice session
     * @param command The parsed voice command
     * @return Result containing the confirmed event or error
     */
    suspend fun handleConfirmPoll(
        session: VoiceSession,
        command: VoiceCommand
    ): Result<String> {
        val eventId = session.context.eventId
            ?: command.parameters["eventId"]
            ?: return Result.failure(IllegalStateException("No event specified"))

        val event = eventRepository.getEvent(eventId) ?: return Result.failure(
            IllegalStateException("Event not found")
        )

        // Check if event is in POLLING status
        if (event.status != EventStatus.POLLING) {
            return Result.failure(
                IllegalStateException("Cannot confirm: Event is not in POLLING status")
            )
        }

        // Calculate the winning slot based on votes
        val poll = eventRepository.getPoll(eventId)
            ?: return Result.failure(IllegalStateException("Poll not found"))

        val winningSlot = calculateWinningSlot(event.proposedSlots, poll)
            ?: return Result.failure(IllegalStateException("No slots to confirm"))

        // Update event status to CONFIRMED
        eventRepository.updateEventStatus(
            id = eventId,
            status = EventStatus.CONFIRMED,
            finalDate = winningSlot.start
        )

        return Result.success(winningSlot.start ?: winningSlot.id)
    }

    /**
     * Handles the GET_STATS intent.
     * Returns voting statistics for the current poll.
     *
     * @param session The current voice session
     * @param command The parsed voice command
     * @return Result containing statistics string or error
     */
    suspend fun handleGetStats(
        session: VoiceSession,
        command: VoiceCommand
    ): Result<PollStatistics> {
        val eventId = session.context.eventId
            ?: command.parameters["eventId"]
            ?: // Return overall stats if no specific event
            return getOverallStatistics()

        val event = eventRepository.getEvent(eventId) ?: return Result.failure(
            IllegalStateException("Event not found")
        )

        val poll = eventRepository.getPoll(eventId)
            ?: return Result.failure(IllegalStateException("Poll not found"))

        // Calculate statistics
        val totalVotes = poll.votes.values.sumOf { it.size }
        val yesCount = countVotesByType(poll, Vote.YES)
        val maybeCount = countVotesByType(poll, Vote.MAYBE)
        val noCount = countVotesByType(poll, Vote.NO)
        val participants = event.participants.size

        return Result.success(
            PollStatistics(
                totalVotes = totalVotes,
                yesCount = yesCount,
                maybeCount = maybeCount,
                noCount = noCount,
                totalParticipants = participants,
                slotBreakdown = calculateSlotBreakdown(event.proposedSlots, poll)
            )
        )
    }

    /**
     * Calculates the winning slot based on vote scores.
     * Scoring: YES = 2 points, MAYBE = 1 point, NO = -1 point
     */
    private fun calculateWinningSlot(
        slots: List<TimeSlot>,
        poll: Poll
    ): TimeSlot? {
        if (slots.isEmpty()) return null

        return slots.maxByOrNull { slot ->
            val slotVotes = poll.votes.values.mapNotNull { it[slot.id] }
            slotVotes.sumOf { vote ->
                when (vote) {
                    Vote.YES -> 2
                    Vote.MAYBE -> 1
                    Vote.NO -> -1
                }
            }
        }
    }

    /**
     * Counts votes of a specific type.
     */
    private fun countVotesByType(poll: Poll, voteType: Vote): Int {
        return poll.votes.values.sumOf { slotVotes ->
            slotVotes.values.count { it == voteType }
        }
    }

    /**
     * Calculates vote breakdown per slot.
     */
    private fun calculateSlotBreakdown(
        slots: List<TimeSlot>,
        poll: Poll
    ): Map<String, SlotVoteStats> {
        return slots.associate { slot ->
            val slotVotes = poll.votes.values.mapNotNull { it[slot.id] }
            slot.id to SlotVoteStats(
                slotId = slot.id,
                startDate = slot.start,
                yesCount = slotVotes.count { it == Vote.YES },
                maybeCount = slotVotes.count { it == Vote.MAYBE },
                noCount = slotVotes.count { it == Vote.NO },
                totalCount = slotVotes.size
            )
        }
    }

    /**
     * Gets overall statistics across all events.
     */
    private suspend fun getOverallStatistics(): Result<PollStatistics> {
        val events = eventRepository.getAllEvents()
        val pollingEvents = events.filter { it.status == EventStatus.POLLING }

        val totalEvents = events.size
        val activePolls = pollingEvents.size
        val totalParticipants = events.sumOf { it.participants.size }

        return Result.success(
            PollStatistics(
                totalVotes = 0,
                yesCount = 0,
                maybeCount = 0,
                noCount = 0,
                totalParticipants = totalParticipants,
                additionalInfo = mapOf(
                    "total_events" to totalEvents.toString(),
                    "active_polls" to activePolls.toString()
                )
            )
        )
    }

    /**
     * Gets the current timestamp in ISO format.
     */
    private fun getCurrentTimestamp(): String {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            .toString()
            .replace("T", " ")
            .substringBefore(".")
    }

    /**
     * Data class for poll statistics.
     */
    data class PollStatistics(
        val totalVotes: Int,
        val yesCount: Int,
        val maybeCount: Int,
        val noCount: Int,
        val totalParticipants: Int,
        val slotBreakdown: Map<String, SlotVoteStats> = emptyMap(),
        val additionalInfo: Map<String, String> = emptyMap()
    )

    /**
     * Data class for per-slot vote statistics.
     */
    data class SlotVoteStats(
        val slotId: String,
        val startDate: String?,
        val yesCount: Int,
        val maybeCount: Int,
        val noCount: Int,
        val totalCount: Int
    )
}
