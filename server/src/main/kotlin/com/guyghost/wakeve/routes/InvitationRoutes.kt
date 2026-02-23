package com.guyghost.wakeve.routes

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.invitation.InvitationRepository
import com.guyghost.wakeve.models.CreateInvitationRequest
import com.guyghost.wakeve.models.Invitation
import com.guyghost.wakeve.models.InvitationAcceptResponse
import com.guyghost.wakeve.models.InvitationResolveResponse
import com.guyghost.wakeve.models.InvitationResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * Invitation routes for event sharing and invitation links.
 *
 * Authenticated routes (under /api):
 * - POST /api/events/{id}/invite - Generate a shareable invitation link
 *
 * These routes require JWT authentication.
 */
fun Route.invitationRoutes(
    invitationRepository: InvitationRepository,
    eventRepository: DatabaseEventRepository,
    database: WakeveDb? = null
) {
    route("/events/{id}/invite") {
        // POST /api/events/{id}/invite - Generate invitation link
        post {
            try {
                val eventId = call.parameters["id"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                // Get authenticated user
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Authentication required")
                    )

                // Verify event exists
                val event = eventRepository.getEvent(eventId) ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Event not found")
                )

                // Parse request body (optional parameters)
                val request = try {
                    call.receive<CreateInvitationRequest>()
                } catch (e: Exception) {
                    CreateInvitationRequest()
                }

                // Generate unique invitation code (8 characters, alphanumeric)
                val code = generateInvitationCode()
                val now = java.time.Instant.now().toString()

                val invitation = Invitation(
                    id = "inv_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}",
                    code = code,
                    eventId = eventId,
                    createdBy = userId,
                    expiresAt = request.expiresAt,
                    maxUses = request.maxUses,
                    currentUses = 0,
                    createdAt = now
                )

                val result = invitationRepository.createInvitation(invitation)

                if (result.isSuccess) {
                    val response = InvitationResponse(
                        id = invitation.id,
                        code = invitation.code,
                        eventId = invitation.eventId,
                        createdBy = invitation.createdBy,
                        expiresAt = invitation.expiresAt,
                        maxUses = invitation.maxUses,
                        currentUses = invitation.currentUses,
                        createdAt = invitation.createdAt,
                        inviteUrl = "https://wakeve.app/invite/$code",
                        deepLinkUrl = "wakeve://invite/$code"
                    )
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to create invitation"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
    }
}

/**
 * Public invitation routes (no auth required for resolve, auth required for accept).
 *
 * Public routes:
 * - GET /api/invite/{code} - Resolve invitation code (no auth)
 *
 * These are registered outside the authenticate block.
 */
fun Route.publicInvitationRoutes(
    invitationRepository: InvitationRepository,
    eventRepository: DatabaseEventRepository
) {
    route("/invite/{code}") {
        // GET /api/invite/{code} - Resolve invitation code (no auth required)
        get {
            try {
                val code = call.parameters["code"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invitation code required")
                )

                // Look up invitation by code
                val invitation = invitationRepository.getByCode(code) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Invitation not found or invalid")
                )

                // Check validity
                val now = java.time.Instant.now().toString()
                val isValid = invitation.isValid(now)

                // Get event info
                val event = eventRepository.getEvent(invitation.eventId)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Associated event not found")
                    )

                val response = InvitationResolveResponse(
                    code = invitation.code,
                    eventId = event.id,
                    eventTitle = event.title,
                    eventDescription = event.description,
                    eventStatus = event.status.name,
                    organizerId = event.organizerId,
                    participantCount = event.participants.size,
                    isValid = isValid,
                    expiresAt = invitation.expiresAt
                )
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
    }
}

/**
 * Authenticated invitation accept route.
 *
 * - POST /api/invite/{code}/accept - Accept invitation and join event
 *
 * This route requires JWT authentication.
 */
fun Route.invitationAcceptRoutes(
    invitationRepository: InvitationRepository,
    eventRepository: DatabaseEventRepository,
    database: WakeveDb? = null
) {
    route("/invite/{code}/accept") {
        // POST /api/invite/{code}/accept - Accept invitation and join event
        post {
            try {
                val code = call.parameters["code"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invitation code required")
                )

                // Get authenticated user
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Authentication required")
                    )

                // Look up invitation
                val invitation = invitationRepository.getByCode(code) ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Invitation not found or invalid")
                )

                // Check validity
                val now = java.time.Instant.now().toString()
                if (!invitation.isValid(now)) {
                    return@post call.respond(
                        HttpStatusCode.Gone,
                        mapOf("error" to "This invitation has expired or reached its maximum uses")
                    )
                }

                // Check if event exists
                val event = eventRepository.getEvent(invitation.eventId)
                    ?: return@post call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Associated event not found")
                    )

                // Check if user is already a participant
                val existingParticipants = eventRepository.getParticipants(invitation.eventId)
                if (existingParticipants != null && existingParticipants.contains(userId)) {
                    call.respond(
                        HttpStatusCode.OK,
                        InvitationAcceptResponse(
                            success = true,
                            eventId = invitation.eventId,
                            message = "Vous participez déjà à cet événement"
                        )
                    )
                    return@post
                }

                // Add participant to event via invitation (bypasses DRAFT-only restriction)
                try {
                    val joinedAt = java.time.Instant.now().toString()
                    val participantId = "part_${invitation.eventId}_${userId}"
                    database!!.participantQueries.insertParticipant(
                        id = participantId,
                        eventId = invitation.eventId,
                        userId = userId,
                        role = "PARTICIPANT",
                        hasValidatedDate = 0,
                        joinedAt = joinedAt,
                        updatedAt = joinedAt
                    )

                    // Increment invitation usage count
                    invitationRepository.incrementUses(code)

                    call.respond(
                        HttpStatusCode.OK,
                        InvitationAcceptResponse(
                            success = true,
                            eventId = invitation.eventId,
                            message = "Vous avez rejoint l'événement « ${event.title} »"
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        InvitationAcceptResponse(
                            success = false,
                            eventId = invitation.eventId,
                            message = "Impossible de rejoindre l'événement: ${e.message}"
                        )
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
    }
}

/**
 * Generate a random alphanumeric invitation code.
 *
 * Produces an 8-character code using uppercase letters and digits,
 * suitable for sharing in URLs and QR codes.
 */
private fun generateInvitationCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Excluding ambiguous chars (I, O, 0, 1)
    return (1..8).map { chars.random() }.joinToString("")
}
