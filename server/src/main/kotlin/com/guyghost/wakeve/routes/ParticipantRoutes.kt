package com.guyghost.wakeve.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.models.AddParticipantRequest

fun Route.participantRoutes(repository: DatabaseEventRepository) {
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
