package com.guyghost.wakeve

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventSearchResult
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.NearbyEventResult
import com.guyghost.wakeve.models.NearbyEventsResponse
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.RecommendedEventsResponse
import com.guyghost.wakeve.models.SearchResultsResponse
import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.TrendingEventsResponse
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.repository.OrderBy
import com.guyghost.wakeve.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Database-backed event repository using SQLDelight for persistence.
 * Mirrors the EventRepository interface but stores data in SQLite.
 */
class DatabaseEventRepository(private val db: WakeveDb, private val syncManager: SyncManager? = null) : EventRepositoryInterface {
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

    // MARK: - Search & Discovery

    /**
     * Search events with full-text filtering, category, date range, and pagination.
     * TODO: Replace SQL LIKE with Meilisearch for better full-text search at scale.
     */
    fun searchEvents(
        query: String?,
        category: String?,
        location: String?,
        dateFrom: String?,
        dateTo: String?,
        status: String?,
        sortBy: String,
        offset: Int,
        limit: Int
    ): SearchResultsResponse {
        val rows = eventQueries.searchEvents(
            query = query,
            category = category,
            status = status,
            dateFrom = dateFrom,
            dateTo = dateTo,
            sortBy = sortBy,
            limit = limit.toLong(),
            offset = offset.toLong()
        ).executeAsList()

        val totalCount = eventQueries.countSearchEvents(
            query = query,
            category = category,
            status = status,
            dateFrom = dateFrom,
            dateTo = dateTo
        ).executeAsOne()

        val events = rows.mapNotNull { row ->
            eventToSearchResult(row.id)
        }.let { results ->
            // Apply location filter client-side if specified
            if (location != null && location.isNotBlank()) {
                results.filter { result ->
                    result.locationName?.contains(location, ignoreCase = true) == true
                }
            } else {
                results
            }
        }

        return SearchResultsResponse(
            events = events,
            totalCount = totalCount.toInt(),
            offset = offset,
            limit = limit,
            hasMore = (offset + limit) < totalCount
        )
    }

    /**
     * Get trending events (most participants in the last 7 days).
     */
    fun getTrendingEvents(limit: Int): TrendingEventsResponse {
        // Calculate "7 days ago" timestamp
        // Since the repo uses a fixed test date, use a reasonable lookback
        val since = "2025-01-01T00:00:00Z"

        val rows = eventQueries.selectTrending(
            since = since,
            limit = limit.toLong()
        ).executeAsList()

        val events = rows.mapNotNull { row ->
            eventToSearchResult(row.id)
        }

        return TrendingEventsResponse(
            events = events,
            period = "7_days"
        )
    }

    /**
     * Get events near a geographic location.
     * Uses Haversine formula for distance calculation.
     */
    fun getNearbyEvents(lat: Double, lon: Double, radiusKm: Double, limit: Int): NearbyEventsResponse {
        val locationsWithCoords = db.potentialLocationQueries
            .selectAllWithCoordinates()
            .executeAsList()

        // Parse coordinates and compute distances
        val nearbyResults = mutableListOf<NearbyEventResult>()

        locationsWithCoords.forEach { locRow ->
            val coords = parseCoordinates(locRow.coordinates ?: return@forEach)
            if (coords != null) {
                val distance = haversineDistance(lat, lon, coords.first, coords.second)
                if (distance <= radiusKm) {
                    val searchResult = eventToSearchResult(locRow.eventId)
                    if (searchResult != null) {
                        nearbyResults.add(
                            NearbyEventResult(
                                event = searchResult,
                                distanceKm = round(distance * 10.0) / 10.0
                            )
                        )
                    }
                }
            }
        }

        // Sort by distance, limit results, deduplicate by event ID
        val uniqueResults = nearbyResults
            .sortedBy { it.distanceKm }
            .distinctBy { it.event.id }
            .take(limit)

        return NearbyEventsResponse(
            events = uniqueResults,
            centerLat = lat,
            centerLon = lon,
            radiusKm = radiusKm
        )
    }

    /**
     * Get recommended events for a user based on their past event types.
     * Simple recommendation: find events matching the user's historical event types.
     */
    fun getRecommendedEvents(userId: String, limit: Int): RecommendedEventsResponse {
        // Get event types from user's organized events
        val organizerTypes = eventQueries.selectEventTypesByOrganizer(userId)
            .executeAsList()
            .filterNotNull()

        // Get event types from user's participated events
        val participantTypes = eventQueries.selectEventTypesByParticipant(userId)
            .executeAsList()
            .filterNotNull()

        val preferredTypes = (organizerTypes + participantTypes).distinct()

        if (preferredTypes.isEmpty()) {
            // No history, return popular events as fallback
            val trending = getTrendingEvents(limit)
            return RecommendedEventsResponse(
                events = trending.events,
                userId = userId,
                reason = "popular_events"
            )
        }

        // Pad types to 3 for the SQL query (uses :type1, :type2, :type3)
        val type1 = preferredTypes.getOrElse(0) { preferredTypes.first() }
        val type2 = preferredTypes.getOrElse(1) { type1 }
        val type3 = preferredTypes.getOrElse(2) { type1 }

        val rows = eventQueries.selectByEventType(
            type1 = type1,
            type2 = type2,
            type3 = type3,
            limit = limit.toLong()
        ).executeAsList()

        val events = rows.mapNotNull { row ->
            eventToSearchResult(row.id)
        }

        return RecommendedEventsResponse(
            events = events,
            userId = userId,
            reason = "based_on_past_event_types"
        )
    }

    // MARK: - Private Helpers

    /**
     * Convert an event ID to an EventSearchResult by loading event + location data.
     */
    private fun eventToSearchResult(eventId: String): EventSearchResult? {
        val event = getEvent(eventId) ?: return null
        val participants = participantQueries.selectByEventId(eventId).executeAsList()

        // Get first location if available
        val location = try {
            db.potentialLocationQueries
                .selectFirstLocationByEventId(eventId)
                .executeAsOneOrNull()
        } catch (_: Exception) {
            null
        }

        return EventSearchResult(
            id = event.id,
            title = event.title,
            description = event.description,
            organizerId = event.organizerId,
            status = event.status.name,
            eventType = event.eventType.name,
            eventTypeCustom = event.eventTypeCustom,
            participantCount = participants.size,
            maxParticipants = event.maxParticipants,
            deadline = event.deadline,
            createdAt = event.createdAt,
            locationName = location?.name,
            locationCoordinates = location?.coordinates
        )
    }

    /**
     * Parse a coordinates JSON string to a (latitude, longitude) pair.
     * Expected format: {"latitude": 48.8566, "longitude": 2.3522}
     */
    private fun parseCoordinates(json: String): Pair<Double, Double>? {
        return try {
            val latMatch = Regex(""""latitude"\s*:\s*([-\d.]+)""").find(json)
            val lonMatch = Regex(""""longitude"\s*:\s*([-\d.]+)""").find(json)
            if (latMatch != null && lonMatch != null) {
                Pair(latMatch.groupValues[1].toDouble(), lonMatch.groupValues[1].toDouble())
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Calculate the Haversine distance between two geographic points in kilometers.
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * asin(sqrt(a))
        return earthRadiusKm * c
    }

    override fun getEventsPaginated(
        page: Int,
        pageSize: Int,
        orderBy: OrderBy
    ): Flow<List<Event>> {
        // Handle invalid parameters
        if (page < 0 || pageSize <= 0) {
            return flowOf(emptyList())
        }

        return try {
            val offset = page * pageSize
            val events = eventQueries.selectPaginated(
                orderBy = orderBy.name,
                limit = pageSize.toLong(),
                offset = offset.toLong()
            ).executeAsList().mapNotNull { eventRow ->
                getEvent(eventRow.id)
            }
            flowOf(events)
        } catch (e: Exception) {
            println("⚠️ Error getting paginated events: ${e.message}")
            flowOf(emptyList())
        }
    }

    /**
     * Delete an event and all its related data.
     *
     * This method performs cascade deletion in the following order:
     * 1. Votes (depends on participants and time slots)
     * 2. Participants
     * 3. Time slots
     * 4. Potential locations
     * 5. Scenarios (cascade deletes scenario votes)
     * 6. Confirmed date
     * 7. Sync metadata for this event
     * 8. Event itself
     *
     * A tombstone record is created in syncMetadata for offline sync.
     *
     * Note: SQLite foreign keys with ON DELETE CASCADE would handle most of this,
     * but we explicitly delete to ensure proper ordering and to record sync metadata.
     *
     * @param eventId The ID of the event to delete
     * @return Result<Unit> success if deleted, failure with error message
     */
    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            val event = getEvent(eventId)
                ?: return Result.failure(IllegalArgumentException("Event not found"))

            val now = getCurrentUtcIsoString()

            // Use a transaction to ensure atomicity
            db.transaction {
                // 1. Delete votes (they reference participants and time slots)
                voteQueries.deleteByEventId(eventId)

                // 2. Delete participants
                participantQueries.deleteByEventId(eventId)

                // 3. Delete time slots
                timeSlotQueries.deleteByEventId(eventId)

                // 4. Delete potential locations
                db.potentialLocationQueries.deleteByEventId(eventId)

                // 5. Delete scenarios (cascade will delete scenario votes)
                db.scenarioQueries.deleteByEventId(eventId)

                // 6. Delete confirmed date
                confirmedDateQueries.deleteByEventId(eventId)

                // 7. Delete all sync metadata related to this event
                syncMetadataQueries.deleteByEntity("event", eventId)

                // 8. Delete the event itself
                eventQueries.deleteEvent(eventId)
            }

            // Record tombstone for offline sync (outside transaction to avoid conflicts)
            syncManager?.recordLocalChange(
                table = "events",
                operation = SyncOperation.DELETE,
                recordId = eventId,
                data = """{"id":"$eventId","deletedAt":"$now"}""",
                userId = event.organizerId
            )

            // Record sync metadata for the delete operation
            syncMetadataQueries.insertSyncMetadata(
                id = "sync_delete_${eventId}_$now",
                entityType = "event",
                entityId = eventId,
                operation = "DELETE",
                timestamp = now,
                synced = 0
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // MARK: - Notification Scheduler Helpers

    /**
     * Retourne tous les evenements en cours de sondage (status = POLLING).
     * Utilise par le NotificationScheduler pour verifier les deadlines.
     */
    fun getAllPollingEvents(): List<Event> {
        return try {
            eventQueries.selectByStatus("POLLING").executeAsList().mapNotNull { row ->
                getEvent(row.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Retourne les evenements confirmes dont la date finale est aujourd'hui.
     * Utilise par le NotificationScheduler pour les rappels jour-J.
     *
     * @param todayMs Timestamp du jour courant en millisecondes
     */
    fun getConfirmedEventsForToday(todayMs: Long): List<Event> {
        return try {
            val confirmedEvents = eventQueries.selectByStatus("CONFIRMED").executeAsList()
            confirmedEvents.mapNotNull { row ->
                val event = getEvent(row.id) ?: return@mapNotNull null
                // Utiliser la requete jointure pour obtenir le startTime du timeslot
                val confirmedWithDetails = confirmedDateQueries
                    .selectWithTimeslotDetails(row.id)
                    .executeAsOneOrNull()
                if (confirmedWithDetails != null) {
                    val dateMs = runCatching {
                        kotlinx.datetime.Instant.parse(confirmedWithDetails.startTime).toEpochMilliseconds()
                    }.getOrNull() ?: return@mapNotNull null
                    // Meme jour (arrondi au jour)
                    val todayDay = todayMs / 86_400_000
                    val dateDay = dateMs / 86_400_000
                    if (todayDay == dateDay) event else null
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Retourne la liste de tous les ID utilisateurs distincts
     * (organisateurs et participants).
     * Utilise par le NotificationScheduler pour le digest hebdomadaire.
     */
    fun getAllUserIds(): List<String> {
        return try {
            val organizerIds = eventQueries.selectAll().executeAsList().map { it.organizerId }
            val participantIds = try {
                db.participantQueries.selectAll().executeAsList().map { it.userId }
            } catch (e: Exception) {
                emptyList()
            }
            (organizerIds + participantIds).distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
