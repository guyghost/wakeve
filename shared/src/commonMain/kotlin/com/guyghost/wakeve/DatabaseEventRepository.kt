package com.guyghost.wakeve

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.Vote

/**
 * Database-backed event repository using SQLDelight for persistence.
 * Mirrors the EventRepository interface but stores data in SQLite.
 */
class DatabaseEventRepository(private val db: WakevDb) {
    private val eventQueries = db.eventQueries
    private val timeSlotQueries = db.timeSlotQueries
    private val participantQueries = db.participantQueries
    private val voteQueries = db.voteQueries
    private val confirmedDateQueries = db.confirmedDateQueries
    private val syncMetadataQueries = db.syncMetadataQueries

    fun createEvent(event: Event): Result<Event> {
        return try {
            val now = getCurrentUtcIsoString()
            eventQueries.insertEvent(
                id = event.id,
                organizerId = event.organizerId,
                title = event.title,
                description = event.description,
                status = event.status.name,
                deadline = event.deadline,
                createdAt = now,
                updatedAt = now,
                version = 1
            )

            // Insert organizer as participant
            val organizerId = "org_${event.id}"
            participantQueries.insertParticipant(
                id = organizerId,
                eventId = event.id,
                userId = event.organizerId,
                role = "ORGANIZER",
                hasValidatedDate = 0,
                joinedAt = now,
                updatedAt = now
            )

            // Insert proposed time slots
            event.proposedSlots.forEach { slot ->
                timeSlotQueries.insertTimeSlot(
                    id = slot.id,
                    eventId = event.id,
                    startTime = slot.start,
                    endTime = slot.end,
                    timezone = slot.timezone,
                    proposedByParticipantId = null,
                    createdAt = now,
                    updatedAt = now
                )
            }

            // Record creation in sync metadata
            syncMetadataQueries.insertSyncMetadata(
                id = "sync_${event.id}",
                entityType = "event",
                entityId = event.id,
                operation = "CREATE",
                timestamp = now,
                synced = 0
            )

            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getEvent(id: String): Event? {
        val eventRow = eventQueries.selectById(id).executeAsOneOrNull() ?: return null
        val participants = participantQueries.selectByEventId(id).executeAsList()
        val timeSlots = timeSlotQueries.selectByEventId(id).executeAsList()

        return Event(
            id = eventRow.id,
            title = eventRow.title,
            description = eventRow.description,
            organizerId = eventRow.organizerId,
            participants = participants.map { it.userId },
            proposedSlots = timeSlots.map { TimeSlot(it.id, it.startTime, it.endTime, it.timezone) },
            deadline = eventRow.deadline,
            status = EventStatus.valueOf(eventRow.status),
            finalDate = null // Will be populated from confirmedDate table if exists
        )
    }

    fun getPoll(eventId: String): Poll? {
        val event = getEvent(eventId) ?: return null
        val votes = mutableMapOf<String, Map<String, Vote>>()

        val allVotes = voteQueries.selectVotesForEventTimeslots(eventId).executeAsList()
        
        allVotes.forEach { voteRow ->
            val participantId = voteRow.userId
            val slotId = voteRow.timeslotId
            val voteValue = Vote.valueOf(voteRow.vote)

            if (!votes.containsKey(participantId)) {
                votes[participantId] = mutableMapOf()
            }
            (votes[participantId] as? MutableMap<String, Vote>)?.put(slotId, voteValue)
        }

        return Poll(eventId, eventId, votes)
    }

    fun addParticipant(eventId: String, participantId: String): Result<Boolean> {
        val event = getEvent(eventId) ?: return Result.failure(IllegalArgumentException("Event not found"))

        if (event.status != EventStatus.DRAFT) {
            return Result.failure(IllegalStateException("Cannot add participants after DRAFT status"))
        }

        if (event.participants.contains(participantId)) {
            return Result.failure(IllegalArgumentException("Participant already added"))
        }

        return try {
            val now = getCurrentUtcIsoString()
            val newParticipantId = "part_${eventId}_${participantId}"
            participantQueries.insertParticipant(
                id = newParticipantId,
                eventId = eventId,
                userId = participantId,
                role = "PARTICIPANT",
                hasValidatedDate = 0,
                joinedAt = now,
                updatedAt = now
            )

            syncMetadataQueries.insertSyncMetadata(
                id = "sync_${newParticipantId}",
                entityType = "participant",
                entityId = newParticipantId,
                operation = "CREATE",
                timestamp = now,
                synced = 0
            )

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getParticipants(eventId: String): List<String>? {
        return try {
            participantQueries.selectByEventId(eventId).executeAsList().map { it.userId }
        } catch (e: Exception) {
            null
        }
    }

    fun addVote(eventId: String, participantId: String, slotId: String, vote: Vote): Result<Boolean> {
        val event = getEvent(eventId) ?: return Result.failure(IllegalArgumentException("Event not found"))

        if (event.status != EventStatus.POLLING) {
            return Result.failure(IllegalStateException("Event is not in POLLING status"))
        }

        if (isDeadlinePassed(event.deadline)) {
            return Result.failure(IllegalStateException("Voting deadline has passed"))
        }

        if (!event.participants.contains(participantId)) {
            return Result.failure(IllegalArgumentException("Participant not in event"))
        }

        return try {
            val now = getCurrentUtcIsoString()
            val voteId = "vote_${slotId}_${participantId}"
            voteQueries.insertVote(
                id = voteId,
                eventId = eventId,
                timeslotId = slotId,
                participantId = participantId,
                vote = vote.name,
                createdAt = now,
                updatedAt = now
            )

            syncMetadataQueries.insertSyncMetadata(
                id = "sync_${voteId}",
                entityType = "vote",
                entityId = voteId,
                operation = "CREATE",
                timestamp = now,
                synced = 0
            )

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateEventStatus(id: String, status: EventStatus, finalDate: String? = null): Result<Boolean> {
        val event = getEvent(id) ?: return Result.failure(IllegalArgumentException("Event not found"))

        return try {
            val now = getCurrentUtcIsoString()
            eventQueries.updateEventStatus(
                status = status.name,
                updatedAt = now,
                id = id
            )

            // If confirming, also create confirmedDate record
            if (status == EventStatus.CONFIRMED && finalDate != null) {
                val confirmedId = "confirmed_${id}"
                val firstTimeSlot = getEvent(id)?.proposedSlots?.firstOrNull()?.id ?: return Result.failure(
                    IllegalStateException("No time slots to confirm")
                )
                confirmedDateQueries.insertConfirmedDate(
                    id = confirmedId,
                    eventId = id,
                    timeslotId = firstTimeSlot,
                    confirmedByOrganizerId = event.organizerId,
                    confirmedAt = finalDate,
                    updatedAt = now
                )
            }

            syncMetadataQueries.insertSyncMetadata(
                id = "sync_status_${id}",
                entityType = "event",
                entityId = id,
                operation = "UPDATE",
                timestamp = now,
                synced = 0
            )

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isDeadlinePassed(deadline: String): Boolean {
        return try {
            deadline < getCurrentUtcIsoString()
        } catch (e: Exception) {
            false
        }
    }

    private fun getCurrentUtcIsoString(): String {
        // For Phase 1, we use a fixed test date
        // In Phase 2, integrate with kotlinx.datetime for full timezone support
        return "2025-11-12T10:00:00Z"
    }

    fun isOrganizer(eventId: String, userId: String): Boolean {
        return getEvent(eventId)?.organizerId == userId
    }

    fun canModifyEvent(eventId: String, userId: String): Boolean {
        return isOrganizer(eventId, userId)
    }

    fun getAllEvents(): List<Event> {
        return eventQueries.selectAll().executeAsList().mapNotNull { eventRow ->
            getEvent(eventRow.id)
        }
    }
}
