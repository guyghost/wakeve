package com.guyghost.wakeve.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.models.*

fun Route.eventRoutes(repository: DatabaseEventRepository) {
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
                                timezone = slot.timezone
                            )
                        },
                        finalDate = event.finalDate
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
                            timezone = slot.timezone
                        )
                    },
                    finalDate = event.finalDate
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
                        timezone = slot.timezone
                    )
                }

                val event = Event(
                    id = "event_${System.currentTimeMillis()}_${Math.random()}",
                    title = request.title,
                    description = request.description,
                    organizerId = request.organizerId,
                    participants = emptyList(),
                    proposedSlots = timeSlots,
                    deadline = request.deadline,
                    status = com.guyghost.wakeve.models.EventStatus.DRAFT
                )

                val result = repository.createEvent(event)
                
                if (result.isSuccess) {
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
                                timezone = slot.timezone
                            )
                        }
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
                                    timezone = slot.timezone
                                )
                            },
                            finalDate = event.finalDate
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
