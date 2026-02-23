package com.guyghost.wakeve.routes

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.gamification.GamificationService
import com.guyghost.wakeve.gamification.PointsAction
import com.guyghost.wakeve.models.CreateEventRequest
import com.guyghost.wakeve.models.Event
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
import com.guyghost.wakeve.notification.EventNotificationTrigger
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun io.ktor.server.routing.Route.eventRoutes(
    repository: DatabaseEventRepository,
    gamificationService: GamificationService? = null,
    eventNotificationTrigger: EventNotificationTrigger? = null
) {
    route("/events") {
        // GET /api/events - Get all events
        get {
            try {
                val events = repository.getAllEvents()
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
                        expectedParticipants = event.expectedParticipants
                    )
                }
                call.respond(HttpStatusCode.OK, mapOf("events" to responses))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // GET /api/events/{id} - Get specific event
        get("/{id}") {
            try {
                val eventId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val event = repository.getEvent(eventId) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Event not found")
                )

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
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // POST /api/events - Create new event
        post {
            try {
                val request = call.receive<CreateEventRequest>()
                
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
                    organizerId = request.organizerId,
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
                    expectedParticipants = request.expectedParticipants
                )

                val result = repository.createEvent(event)
                
                if (result.isSuccess) {
                    // Award points for creating an event (+50 points)
                    try {
                        gamificationService?.awardPoints(
                            userId = event.organizerId,
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
                        expectedParticipants = event.expectedParticipants
                    )
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to create event"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // GET /api/events/search - Search events with filters and pagination
        // Query params: q, category, location, dateFrom, dateTo, status, sortBy, offset, limit
        get("/search") {
            try {
                val query = call.parameters["q"]
                val category = call.parameters["category"]
                val location = call.parameters["location"]
                val dateFrom = call.parameters["dateFrom"]
                val dateTo = call.parameters["dateTo"]
                val status = call.parameters["status"]
                val sortBy = call.parameters["sortBy"] ?: "RELEVANCE"
                val offset = call.parameters["offset"]?.toIntOrNull() ?: 0
                val limit = (call.parameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1, 100)

                val results = repository.searchEvents(
                    query = query,
                    category = category,
                    location = location,
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    status = status,
                    sortBy = sortBy,
                    offset = offset,
                    limit = limit
                )

                call.respond(HttpStatusCode.OK, results)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // GET /api/events/trending - Events with most participants in last 7 days
        get("/trending") {
            try {
                val limit = (call.parameters["limit"]?.toIntOrNull() ?: 10).coerceIn(1, 50)
                val results = repository.getTrendingEvents(limit = limit)
                call.respond(HttpStatusCode.OK, results)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // GET /api/events/nearby - Events near a location
        // Query params: lat, lon, radius (km, default 50)
        get("/nearby") {
            try {
                val lat = call.parameters["lat"]?.toDoubleOrNull()
                val lon = call.parameters["lon"]?.toDoubleOrNull()
                val radius = call.parameters["radius"]?.toDoubleOrNull() ?: 50.0

                if (lat == null || lon == null) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "lat and lon parameters are required")
                    )
                }

                val limit = (call.parameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
                val results = repository.getNearbyEvents(
                    lat = lat,
                    lon = lon,
                    radiusKm = radius,
                    limit = limit
                )

                call.respond(HttpStatusCode.OK, results)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                val limit = (call.parameters["limit"]?.toIntOrNull() ?: 10).coerceIn(1, 50)
                val results = repository.getRecommendedEvents(userId = userId, limit = limit)

                call.respond(HttpStatusCode.OK, results)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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
                    com.guyghost.wakeve.models.EventStatus.valueOf(request.status)
                } catch (e: IllegalArgumentException) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid status: ${request.status}")
                    )
                }

                val result = repository.updateEventStatus(eventId, status, request.finalDate)

                if (result.isSuccess) {
                    // Trigger notification for status change (async, non-blocking)
                    eventNotificationTrigger?.onEventStatusChanged(
                        eventId = eventId,
                        newStatus = status.name,
                        finalDate = request.finalDate
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
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to update status"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
    }
}
