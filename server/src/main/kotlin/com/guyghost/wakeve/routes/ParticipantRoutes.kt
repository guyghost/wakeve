package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.userId
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.gamification.GamificationService
import com.guyghost.wakeve.gamification.PointsAction
import com.guyghost.wakeve.models.AddParticipantRequest
import com.guyghost.wakeve.repository.DatabaseEventRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ParticipantRsvpRequest(
    val slotId: String,
    val attendance: String
)

@Serializable
private data class ParticipantRsvpResponse(
    val eventId: String,
    val userId: String,
    val slotId: String,
    val attendance: String,
    val hasValidatedDate: Boolean
)

fun Route.participantRoutes(
    repository: DatabaseEventRepository,
    gamificationService: GamificationService? = null,
    database: WakeveDb
) {
    route("/events/{id}/participants") {
        // GET /api/events/{id}/participants - Get event participants
        get {
            try {
                val eventId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val participants = repository.getParticipants(eventId) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Event not found")
                )

                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!hasParticipantRouteAccess(repository, database, eventId, principal.userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to this event")
                    )
                }

                call.respond(HttpStatusCode.OK, mapOf("participants" to participants))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // POST /api/events/{id}/participants - Add participant to event
        post {
            try {
                val eventId = call.parameters["id"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                val event = repository.getEvent(eventId) ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Event not found")
                )

                if (event.organizerId != principal.userId) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer can add participants")
                    )
                }

                val request = call.receive<AddParticipantRequest>()

                val result = repository.addParticipant(eventId, request.participantId)
                
                if (result.isSuccess) {
                    // Award points for inviting a participant (+20 points to organizer)
                    try {
                        val event = repository.getEvent(eventId)
                        if (event != null) {
                            gamificationService?.awardPoints(
                                userId = event.organizerId,
                                action = PointsAction.INVITE_PARTICIPANT,
                                eventId = eventId
                            )
                        }
                    } catch (_: Exception) {
                        // Non-blocking: don't fail participant addition if gamification fails
                    }

                    val participants = repository.getParticipants(eventId) ?: emptyList()
                    call.respond(HttpStatusCode.Created, mapOf("participants" to participants))
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to add participant"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // POST /api/events/{id}/participants/{userId}/rsvp - Confirm or update attendance for retained date
        post("/{userId}/rsvp") {
            try {
                val eventId = call.parameters["id"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val participantUserId = call.parameters["userId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Participant user ID required")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                val authenticatedUserId = principal.userId

                val event = repository.getEvent(eventId) ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Event not found")
                )

                if (authenticatedUserId != participantUserId && event.organizerId != authenticatedUserId) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You cannot update this participant RSVP")
                    )
                }

                val request = Json.decodeFromString<ParticipantRsvpRequest>(call.receiveText())
                val requestedSlotId = request.slotId.trim()
                if (requestedSlotId.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "slotId is required")
                    )
                }

                val confirmedDate = database.confirmedDateQueries
                    .selectByEventId(eventId)
                    .executeAsOneOrNull()
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Event has no confirmed retained date")
                    )

                val requestedSlot = database.timeSlotQueries
                    .selectById(requestedSlotId)
                    .executeAsOneOrNull()
                if (requestedSlot == null || requestedSlot.eventId != eventId) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Unknown RSVP slot")
                    )
                }

                if (confirmedDate.timeslotId != requestedSlotId) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "RSVP slot must match the retained date")
                    )
                }

                val participant = database.participantQueries
                    .selectByEventIdAndUserId(eventId, participantUserId)
                    .executeAsOneOrNull()
                    ?: return@post call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Participant not found")
                    )

                val hasValidatedDate = if (request.attendance.uppercase() == "CONFIRMED") 1L else 0L
                database.participantQueries.updateValidation(
                    hasValidatedDate = hasValidatedDate,
                    updatedAt = java.time.Instant.now().toString(),
                    id = participant.id
                )

                call.respond(
                    HttpStatusCode.OK,
                    ParticipantRsvpResponse(
                        eventId = eventId,
                        userId = participantUserId,
                        slotId = requestedSlotId,
                        attendance = request.attendance.uppercase(),
                        hasValidatedDate = hasValidatedDate == 1L
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
    }
}

private fun hasParticipantRouteAccess(
    repository: DatabaseEventRepository,
    database: WakeveDb,
    eventId: String,
    userId: String
): Boolean {
    val event = repository.getEvent(eventId) ?: return false
    if (event.organizerId == userId) {
        return true
    }

    return database.participantQueries
        .selectByEventIdAndUserId(eventId, userId)
        .executeAsOneOrNull() != null
}
