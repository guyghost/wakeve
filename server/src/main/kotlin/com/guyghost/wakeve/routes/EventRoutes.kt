package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.userId
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.gamification.GamificationService
import com.guyghost.wakeve.gamification.PointsAction
import com.guyghost.wakeve.models.CreateEventRequest
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventResponse
import com.guyghost.wakeve.models.EventSearchResult
import com.guyghost.wakeve.models.NearbyEventResult
import com.guyghost.wakeve.models.NearbyEventsResponse
import com.guyghost.wakeve.models.RecommendedEventsResponse
import com.guyghost.wakeve.models.SearchResultsResponse
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.TimeSlotResponse
import com.guyghost.wakeve.models.TrendingEventsResponse
import com.guyghost.wakeve.models.UpdateEventStatusRequest
import com.guyghost.wakeve.moderation.ModerationPolicy
import com.guyghost.wakeve.moderation.ModerationStatus
import com.guyghost.wakeve.notification.EventNotificationTrigger
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun io.ktor.server.routing.Route.eventRoutes(
    repository: DatabaseEventRepository,
    gamificationService: GamificationService? = null,
    eventNotificationTrigger: EventNotificationTrigger? = null,
    database: WakeveDb? = null,
    moderationPolicy: ModerationPolicy = ModerationPolicy()
) {
    route("/events") {
        // GET /api/events - Get all events
        get {
            try {
                val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Not authenticated")
                )
                val currentUserId = principal.userId
                val events = repository.getAllEvents().filter { event ->
                    event.organizerId == currentUserId ||
                        database?.participantQueries
                            ?.selectByEventIdAndUserId(event.id, currentUserId)
                            ?.executeAsOneOrNull() != null
                }
                val responses = events.map { event ->
                    EventResponse(
                        id = event.id,
                        title = event.title,
                        description = event.description,
                        organizerId = event.organizerId,
                        participants = event.participants,
                        deadline = event.deadline,
                        status = event.status.name,
                        proposedSlots = event.proposedSlots.map { slot ->
                            TimeSlotResponse(
                                id = slot.id,
                                start = slot.start,
                                end = slot.end,
                                timezone = slot.timezone,
                                timeOfDay = slot.timeOfDay.name
                            )
                        },
                        finalDate = event.finalDate,
                        // Enhanced DRAFT phase fields
                        eventType = event.eventType.name,
                        eventTypeCustom = event.eventTypeCustom,
                        minParticipants = event.minParticipants,
                        maxParticipants = event.maxParticipants,
                        expectedParticipants = event.expectedParticipants,
                        planningMode = event.planningMode.name
                    )
                }
                call.respond(HttpStatusCode.OK, mapOf("events" to responses))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to eventListFailureMessage())
                )
            }
        }

        // GET /api/events/{id} - Get specific event
        get("/{id}") {
            try {
                val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Not authenticated")
                )
                val eventId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val event = repository.getEvent(eventId) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Event not found")
                )

                if (!canReadEventDetails(database, event, principal.userId)) {
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Event not found")
                    )
                }

                val response = EventResponse(
                    id = event.id,
                    title = event.title,
                    description = event.description,
                    organizerId = event.organizerId,
                    participants = event.participants,
                    deadline = event.deadline,
                    status = event.status.name,
                    proposedSlots = event.proposedSlots.map { slot ->
                        TimeSlotResponse(
                            id = slot.id,
                            start = slot.start,
                            end = slot.end,
                            timezone = slot.timezone,
                            timeOfDay = slot.timeOfDay.name
                        )
                    },
                    finalDate = event.finalDate,
                    // Enhanced DRAFT phase fields
                    eventType = event.eventType.name,
                    eventTypeCustom = event.eventTypeCustom,
                    minParticipants = event.minParticipants,
                    maxParticipants = event.maxParticipants,
                    expectedParticipants = event.expectedParticipants,
                    planningMode = event.planningMode.name
                )
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to eventDetailFailureMessage())
                )
            }
        }

        // POST /api/events - Create new event
        post {
            try {
                val principal = call.principal<JWTPrincipal>() ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Not authenticated")
                )
                val organizerId = principal.userId
                val request = call.receive<CreateEventRequest>()
                val textFields = listOfNotNull(request.title, request.description, request.eventTypeCustom)
                val rejectedField = textFields
                    .map { moderationPolicy.evaluate(it) }
                    .firstOrNull { it.status == ModerationStatus.REJECTED }
                if (rejectedField != null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to rejectedField.userMessage, "reasonCode" to rejectedField.reasonCode)
                    )
                    return@post
                }
                
                val timeSlots = request.proposedSlots.map { slot ->
                    TimeSlot(
                        id = slot.id,
                        start = slot.start,
                        end = slot.end,
                        timezone = slot.timezone,
                        timeOfDay = slot.timeOfDay?.let { 
                            com.guyghost.wakeve.models.TimeOfDay.valueOf(it)
                        } ?: com.guyghost.wakeve.models.TimeOfDay.SPECIFIC
                    )
                }

                val now = java.time.Instant.now().toString()
                val event = Event(
                    id = "event_${System.currentTimeMillis()}_${Math.random()}",
                    title = request.title,
                    description = request.description,
                    organizerId = organizerId,
                    participants = emptyList(),
                    proposedSlots = timeSlots,
                    deadline = request.deadline,
                    status = com.guyghost.wakeve.models.EventStatus.DRAFT,
                    createdAt = now,
                    updatedAt = now,
                    // Enhanced DRAFT phase fields
                    eventType = request.eventType?.let { 
                        com.guyghost.wakeve.models.EventType.valueOf(it)
                    } ?: com.guyghost.wakeve.models.EventType.OTHER,
                    eventTypeCustom = request.eventTypeCustom,
                    minParticipants = request.minParticipants,
                    maxParticipants = request.maxParticipants,
                    expectedParticipants = request.expectedParticipants,
                    planningMode = request.planningMode?.let {
                        com.guyghost.wakeve.models.EventPlanningMode.valueOf(it)
                    } ?: com.guyghost.wakeve.models.EventPlanningMode.TIME_SLOT_POLL
                )

                val result = repository.createEvent(event)
                
                if (result.isSuccess) {
                    // Award points for creating an event (+50 points)
                    try {
                        gamificationService?.awardPoints(
                            userId = organizerId,
                            action = PointsAction.CREATE_EVENT,
                            eventId = event.id
                        )
                    } catch (_: Exception) {
                        // Non-blocking: don't fail event creation if gamification fails
                    }

                    val response = EventResponse(
                        id = event.id,
                        title = event.title,
                        description = event.description,
                        organizerId = event.organizerId,
                        participants = event.participants,
                        deadline = event.deadline,
                        status = event.status.name,
                        proposedSlots = event.proposedSlots.map { slot ->
                            TimeSlotResponse(
                                id = slot.id,
                                start = slot.start,
                                end = slot.end,
                                timezone = slot.timezone,
                                timeOfDay = slot.timeOfDay.name
                            )
                        },
                        // Enhanced DRAFT phase fields
                        eventType = event.eventType.name,
                        eventTypeCustom = event.eventTypeCustom,
                        minParticipants = event.minParticipants,
                        maxParticipants = event.maxParticipants,
                        expectedParticipants = event.expectedParticipants,
                        planningMode = event.planningMode.name
                    )
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to eventCreateFailureMessage())
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to eventCreateFailureMessage())
                )
            }
        }

        // GET /api/events/search - Search events with filters and pagination
        // Query params: q, category, location, dateFrom, dateTo, status, sortBy, offset, limit
        get("/search") {
            try {
                val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Not authenticated")
                )
                val currentUserId = principal.userId
                val queryParameters = call.request.queryParameters
                val query = queryParameters["q"]
                val category = queryParameters["category"]
                val location = queryParameters["location"]
                val dateFrom = queryParameters["dateFrom"]
                val dateTo = queryParameters["dateTo"]
                val status = queryParameters["status"]
                val sortBy = queryParameters["sortBy"] ?: "RELEVANCE"
                val offset = (queryParameters["offset"]?.toIntOrNull() ?: 0).coerceAtLeast(0)
                val limit = (queryParameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
                val candidateLimit = (limit * 5).coerceAtMost(500)

                val results = repository.searchEvents(
                    query = query,
                    category = category,
                    location = location,
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    status = status,
                    sortBy = sortBy,
                    offset = offset,
                    limit = candidateLimit
                )
                val visibleEvents = results.events.filter { result ->
                    canReadEventSearchResult(database, result, currentUserId)
                }.take(limit)

                call.respond(
                    HttpStatusCode.OK,
                    results.copy(
                        events = visibleEvents,
                        totalCount = visibleEvents.size,
                        limit = limit,
                        hasMore = results.hasMore && visibleEvents.size == limit
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to eventSearchFailureMessage())
                )
            }
        }

        // GET /api/events/trending - Events with most participants in last 7 days
        get("/trending") {
            try {
                val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Not authenticated")
                )
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 10).coerceIn(1, 50)
                val candidateLimit = (limit * 5).coerceAtMost(250)
                val results = repository.getTrendingEvents(limit = candidateLimit)
                call.respond(
                    HttpStatusCode.OK,
                    results.copy(
                        events = results.events.filter { result ->
                            canReadEventSearchResult(database, result, principal.userId)
                        }.take(limit)
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to trendingEventsFailureMessage())
                )
            }
        }

        // GET /api/events/nearby - Events near a location
        // Query params: lat, lon, radius (km, default 50)
        get("/nearby") {
            try {
                val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Not authenticated")
                )
                val queryParameters = call.request.queryParameters
                val lat = queryParameters["lat"]?.toDoubleOrNull()
                val lon = queryParameters["lon"]?.toDoubleOrNull()
                val radius = queryParameters["radius"]?.toDoubleOrNull() ?: 50.0

                if (lat == null || lon == null) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "lat and lon parameters are required")
                    )
                }

                val limit = (queryParameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
                val candidateLimit = (limit * 5).coerceAtMost(500)
                val results = repository.getNearbyEvents(
                    lat = lat,
                    lon = lon,
                    radiusKm = radius,
                    limit = candidateLimit
                )

                call.respond(
                    HttpStatusCode.OK,
                    results.copy(
                        events = results.events.filter { result ->
                            canReadEventSearchResult(database, result.event, principal.userId)
                        }.take(limit)
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to nearbyEventsFailureMessage())
                )
            }
        }

        // GET /api/events/recommended/{userId} - Recommendations based on past event types
        get("/recommended/{userId}") {
            try {
                val userId = call.parameters["userId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "User ID required")
                )
                val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Not authenticated")
                )
                if (principal.userId != userId) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You cannot request recommendations for another user")
                    )
                }

                val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 10).coerceIn(1, 50)
                val candidateLimit = (limit * 5).coerceAtMost(250)
                val results = repository.getRecommendedEvents(userId = userId, limit = candidateLimit)

                call.respond(
                    HttpStatusCode.OK,
                    results.copy(events = visibleRecommendedEvents(repository, database, results.events, userId, limit))
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to recommendedEventsFailureMessage())
                )
            }
        }

        // PUT /api/events/{id}/status - Update event status
        put("/{id}/status") {
            try {
                val eventId = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val request = call.receive<UpdateEventStatusRequest>()
                val status = try {
                    EventStatus.valueOf(request.status)
                } catch (e: IllegalArgumentException) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid status: ${request.status}")
                    )
                }

                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                val event = repository.getEvent(eventId) ?: return@put call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Event not found")
                )

                if (event.organizerId != principal.userId) {
                    return@put call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer can update event status")
                    )
                }

                if (!isAllowedEventStatusTransition(event.status, status)) {
                    return@put call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Invalid workflow transition: ${event.status.name} -> ${status.name}")
                    )
                }

                val notificationFinalDate: String?
                val result = if (status == EventStatus.CONFIRMED) {
                    val slotId = request.slotId ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "slotId is required when confirming an event")
                    )
                    val db = database ?: return@put call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Database access is required to confirm a selected slot")
                    )
                    val selectedSlot = db.timeSlotQueries.selectById(slotId).executeAsOneOrNull()
                    if (selectedSlot == null || selectedSlot.eventId != eventId) {
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Selected slot does not belong to this event")
                        )
                    }

                    notificationFinalDate = selectedSlot.startTime ?: request.finalDate
                    repository.updateEventStatus(eventId, status, null).also { updateResult ->
                        if (updateResult.isSuccess) {
                            val now = java.time.Instant.now().toString()
                            db.confirmedDateQueries.deleteByEventId(eventId)
                            db.confirmedDateQueries.insertConfirmedDate(
                                id = "confirmed_$eventId",
                                eventId = eventId,
                                timeslotId = slotId,
                                confirmedByOrganizerId = principal.userId,
                                confirmedAt = now,
                                updatedAt = now
                            )
                        }
                    }
                } else {
                    notificationFinalDate = request.finalDate
                    repository.updateEventStatus(eventId, status, request.finalDate)
                }

                if (result.isSuccess) {
                    // Trigger notification for status change (async, non-blocking)
                    eventNotificationTrigger?.onEventStatusChanged(
                        eventId = eventId,
                        newStatus = status.name,
                        finalDate = notificationFinalDate
                    )
                    val event = repository.getEvent(eventId)
                    if (event != null) {
                        val response = EventResponse(
                            id = event.id,
                            title = event.title,
                            description = event.description,
                            organizerId = event.organizerId,
                            participants = event.participants,
                            deadline = event.deadline,
                            status = event.status.name,
                            proposedSlots = event.proposedSlots.map { slot ->
                                TimeSlotResponse(
                                    id = slot.id,
                                    start = slot.start,
                                    end = slot.end,
                                    timezone = slot.timezone,
                                    timeOfDay = slot.timeOfDay.name
                                )
                            },
                            finalDate = event.finalDate,
                            // Enhanced DRAFT phase fields
                            eventType = event.eventType.name,
                            eventTypeCustom = event.eventTypeCustom,
                            minParticipants = event.minParticipants,
                            maxParticipants = event.maxParticipants,
                            expectedParticipants = event.expectedParticipants
                        )
                        call.respond(HttpStatusCode.OK, response)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to "Event not found")
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to eventStatusUpdateFailureMessage())
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to eventStatusUpdateFailureMessage())
                )
            }
        }
    }
}

private fun canReadEventDetails(database: WakeveDb?, event: Event, userId: String): Boolean {
    if (event.organizerId == userId) return true

    return database?.participantQueries
        ?.selectByEventIdAndUserId(event.id, userId)
        ?.executeAsOneOrNull() != null
}

private fun canReadEventSearchResult(database: WakeveDb?, event: EventSearchResult, userId: String): Boolean {
    if (event.organizerId == userId) return true

    return database?.participantQueries
        ?.selectByEventIdAndUserId(event.id, userId)
        ?.executeAsOneOrNull() != null
}

private fun visibleRecommendedEvents(
    repository: DatabaseEventRepository,
    database: WakeveDb?,
    candidates: List<EventSearchResult>,
    userId: String,
    limit: Int
): List<EventSearchResult> {
    val visibleCandidates = candidates
        .filter { result -> canReadEventSearchResult(database, result, userId) }
        .take(limit)
    if (visibleCandidates.isNotEmpty()) {
        return visibleCandidates
    }

    return repository.getAllEvents()
        .asSequence()
        .filter { event -> canReadEventDetails(database, event, userId) }
        .sortedByDescending { it.createdAt }
        .map { event ->
            EventSearchResult(
                id = event.id,
                title = event.title,
                description = event.description,
                organizerId = event.organizerId,
                status = event.status.name,
                eventType = event.eventType.name,
                eventTypeCustom = event.eventTypeCustom,
                participantCount = event.participants.size,
                maxParticipants = event.maxParticipants,
                deadline = event.deadline,
                createdAt = event.createdAt
            )
        }
        .take(limit)
        .toList()
}

internal fun eventListFailureMessage(): String =
    "Failed to fetch events. Please try again."

internal fun eventDetailFailureMessage(): String =
    "Failed to fetch event details. Please try again."

internal fun eventCreateFailureMessage(): String =
    "Failed to create the event. Please try again."

internal fun eventSearchFailureMessage(): String =
    "Failed to search events. Please try again."

internal fun trendingEventsFailureMessage(): String =
    "Failed to fetch trending events. Please try again."

internal fun nearbyEventsFailureMessage(): String =
    "Failed to fetch nearby events. Please try again."

internal fun recommendedEventsFailureMessage(): String =
    "Failed to fetch recommended events. Please try again."

internal fun eventStatusUpdateFailureMessage(): String =
    "Failed to update event status. Please try again."

private fun isAllowedEventStatusTransition(current: EventStatus, requested: EventStatus): Boolean {
    return when (current) {
        EventStatus.DRAFT -> requested == EventStatus.POLLING
        EventStatus.POLLING -> requested == EventStatus.CONFIRMED
        EventStatus.CONFIRMED -> requested == EventStatus.COMPARING
        EventStatus.COMPARING -> requested == EventStatus.ORGANIZING
        EventStatus.ORGANIZING -> requested == EventStatus.FINALIZED
        EventStatus.FINALIZED -> false
    }
}
