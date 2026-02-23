package com.guyghost.wakeve.routes

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.gamification.GamificationService
import com.guyghost.wakeve.gamification.PointsAction
import com.guyghost.wakeve.models.AddParticipantRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.participantRoutes(
    repository: DatabaseEventRepository,
    gamificationService: GamificationService? = null
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
    }
}
