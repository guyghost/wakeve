package com.guyghost.wakeve

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.sync.SyncManager

/**
 * Database-backed event repository using SQLDelight for persistence.
 * Mirrors the EventRepository interface but stores data in SQLite.
 */
class DatabaseEventRepository(private val db: WakevDb, private val syncManager: SyncManager? = null) : EventRepositoryInterface {
    private val eventQueries = db.eventQueries
    private val timeSlotQueries = db.timeSlotQueries
    private val participantQueries = db.participantQueries
    private val voteQueries = db.voteQueries
    private val confirmedDateQueries = db.confirmedDateQueries
    private val syncMetadataQueries = db.syncMetadataQueries

    override suspend fun createEvent(event: Event): Result<Event> {
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
                version = 1,
                eventType = event.eventType.name,
                eventTypeCustom = event.eventTypeCustom,
                minParticipants = event.minParticipants?.toLong(),
                maxParticipants = event.maxParticipants?.toLong(),
                expectedParticipants = event.expectedParticipants?.toLong()
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
                    updatedAt = now,
                    timeOfDay = slot.timeOfDay.name
                )
            }

            // Record creation in sync metadata
            // Record sync change for offline tracking
            syncManager?.recordLocalChange(
                table = "events",
                operation = SyncOperation.CREATE,
                recordId = event.id,
                data = """{"id":"${event.id}","title":"${event.title}","description":"${event.description}","organizerId":"${event.organizerId}","deadline":"${event.deadline}"}""",
                userId = event.organizerId
            )

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

    override fun getEvent(id: String): Event? {
        val eventRow = eventQueries.selectById(id).executeAsOneOrNull() ?: return null
        val participants = participantQueries.selectByEventId(id).executeAsList()
        val timeSlots = timeSlotQueries.selectByEventId(id).executeAsList()

        return Event(
            id = eventRow.id,
            title = eventRow.title,
            description = eventRow.description,
            organizerId = eventRow.organizerId,
            participants = participants.map { it.userId },
            proposedSlots = timeSlots.map { 
                TimeSlot(
                    id = it.id,
                    start = it.startTime,
                    end = it.endTime,
                    timezone = it.timezone,
                    timeOfDay = com.guyghost.wakeve.models.TimeOfDay.valueOf(it.timeOfDay ?: "SPECIFIC")
                )
            },
            deadline = eventRow.deadline,
            status = EventStatus.valueOf(eventRow.status),
            finalDate = null, // Will be populated from confirmedDate table if exists
            createdAt = eventRow.createdAt,
            updatedAt = eventRow.updatedAt,
            eventType = com.guyghost.wakeve.models.EventType.valueOf(eventRow.eventType ?: "OTHER"),
            eventTypeCustom = eventRow.eventTypeCustom,
            minParticipants = eventRow.minParticipants?.toInt(),
            maxParticipants = eventRow.maxParticipants?.toInt(),
            expectedParticipants = eventRow.expectedParticipants?.toInt()
        )
    }

    override fun getPoll(eventId: String): Poll? {
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

    override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> {
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

            // Record sync change for offline tracking
            syncManager?.recordLocalChange(
                table = "participants",
                operation = SyncOperation.CREATE,
                recordId = newParticipantId,
                data = """{"eventId":"$eventId","userId":"$participantId"}""",
                userId = participantId
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

    override fun getParticipants(eventId: String): List<String>? {
        return try {
            participantQueries.selectByEventId(eventId).executeAsList().map { it.userId }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun addVote(eventId: String, participantId: String, slotId: String, vote: Vote): Result<Boolean> {
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

        // Get the actual participant record ID (not userId)
        val participantRecord = participantQueries.selectByEventIdAndUserId(eventId, participantId).executeAsOneOrNull()
            ?: return Result.failure(IllegalArgumentException("Participant record not found"))

        return try {
            val now = getCurrentUtcIsoString()
            val voteId = "vote_${slotId}_${participantId}"
            voteQueries.insertVote(
                id = voteId,
                eventId = eventId,
                timeslotId = slotId,
                participantId = participantRecord.id,  // Use the actual participant record ID
                vote = vote.name,
                createdAt = now,
                updatedAt = now
            )

            // Record sync change for offline tracking
            syncManager?.recordLocalChange(
                table = "votes",
                operation = SyncOperation.CREATE,
                recordId = voteId,
                data = """{"eventId":"$eventId","participantId":"$participantId","slotId":"$slotId","preference":"${vote.name}"}""",
                userId = participantId
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

    override suspend fun updateEvent(event: Event): Result<Event> {
        return try {
            val now = getCurrentUtcIsoString()
            eventQueries.updateEvent(
                title = event.title,
                description = event.description,
                status = event.status.name,
                deadline = event.deadline,
                updatedAt = now,
                eventType = event.eventType.name,
                eventTypeCustom = event.eventTypeCustom,
                minParticipants = event.minParticipants?.toLong(),
                maxParticipants = event.maxParticipants?.toLong(),
                expectedParticipants = event.expectedParticipants?.toLong(),
                id = event.id
            )

            // Record sync change
            syncManager?.recordLocalChange(
                table = "events",
                operation = SyncOperation.UPDATE,
                recordId = event.id,
                data = """{"title":"${event.title}","description":"${event.description}","status":"${event.status}","deadline":"${event.deadline}"}""",
                userId = event.organizerId
            )

            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save an event (create if it doesn't exist, otherwise update).
     * This is useful for auto-save functionality during draft wizard steps.
     *
     * When updating, this also syncs the time slots to ensure they match the event.
     *
     * @param event The event to save
     * @return Result containing saved event, or an error
     */
    override suspend fun saveEvent(event: Event): Result<Event> {
        return try {
            val existingEvent = getEvent(event.id)
            if (existingEvent != null) {
                // Event exists, update it
                updateEvent(event)

                // Also update time slots
                syncTimeSlots(event.id, event.proposedSlots)
            } else {
                // Event doesn't exist, create it (createEvent already handles time slots)
                createEvent(event)
            }
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Synchronize time slots for an event by replacing all slots with the new list.
     *
     * @param eventId The event ID
     * @param timeSlots The new list of time slots
     */
    private suspend fun syncTimeSlots(eventId: String, timeSlots: List<TimeSlot>) {
        try {
            // Delete existing time slots for this event
            timeSlotQueries.deleteByEventId(eventId)

            // Insert new time slots
            val now = getCurrentUtcIsoString()
            timeSlots.forEach { slot ->
                timeSlotQueries.insertTimeSlot(
                    id = slot.id,
                    eventId = eventId,
                    startTime = slot.start,
                    endTime = slot.end,
                    timezone = slot.timezone,
                    proposedByParticipantId = null,
                    createdAt = now,
                    updatedAt = now,
                    timeOfDay = slot.timeOfDay.name
                )
            }

            // Record sync change
            syncManager?.recordLocalChange(
                table = "timeSlots",
                operation = SyncOperation.UPDATE,
                recordId = eventId,
                data = """{"count":"${timeSlots.size}"}""",
                userId = getEvent(eventId)?.organizerId ?: "unknown"
            )
        } catch (e: Exception) {
            // Log error but don't fail the entire save operation
            println("⚠️ Failed to sync time slots: ${e.message}")
        }
    }

    override suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean> {
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

            // Use unique timestamp by appending status to avoid conflicts
            val uniqueTimestamp = "${now}_${status.name}"
            syncMetadataQueries.insertSyncMetadata(
                id = "sync_status_${id}_${status.name}",
                entityType = "event",
                entityId = id,
                operation = "UPDATE",
                timestamp = uniqueTimestamp,
                synced = 0
            )

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isDeadlinePassed(deadline: String): Boolean {
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

    override fun isOrganizer(eventId: String, userId: String): Boolean {
        return getEvent(eventId)?.organizerId == userId
    }

    override fun canModifyEvent(eventId: String, userId: String): Boolean {
        return isOrganizer(eventId, userId)
    }

    override fun getAllEvents(): List<Event> {
        return eventQueries.selectAll().executeAsList().mapNotNull { eventRow ->
            getEvent(eventRow.id)
        }
    }
}
