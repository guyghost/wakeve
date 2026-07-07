package com.guyghost.wakeve.routes

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.moderation.ModerationAuditOutcome
import com.guyghost.wakeve.moderation.ModerationDecisionAction
import com.guyghost.wakeve.moderation.ModerationRepository
import com.guyghost.wakeve.moderation.ReportReason
import com.guyghost.wakeve.moderation.ReportTarget
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import java.util.UUID

fun io.ktor.server.routing.Route.moderationRoutes(
    moderationRepository: ModerationRepository,
    database: WakeveDb
) {
    route("/moderation") {
        post("/reports") {
            val userId = call.authenticatedUserId() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val request = call.receive<CreateContentReportRequest>()
            val eventId = request.eventId?.trim()?.takeIf { it.isNotBlank() }
            if (eventId != null && !hasModerationEventAccess(database, eventId, userId)) {
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to "You cannot report content for this event")
                )
            }
            if (request.targetType == ReportTarget.EVENT && eventId != null && request.targetId != eventId) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event reports must target the scoped event")
                )
            }
            val report = moderationRepository.createReport(
                id = "report-${UUID.randomUUID()}",
                reporterId = userId,
                targetType = request.targetType,
                targetId = request.targetId,
                eventId = eventId,
                reason = request.reason,
                details = request.details
            )

            call.respond(HttpStatusCode.Created, report)
        }

        get("/blocks") {
            val userId = call.authenticatedUserId() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )

            call.respond(HttpStatusCode.OK, mapOf("blocks" to moderationRepository.getActiveBlocks(userId)))
        }

        post("/blocks") {
            val userId = call.authenticatedUserId() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val request = call.receive<CreateUserBlockRequest>()
            val eventId = request.eventId?.trim()?.takeIf { it.isNotBlank() }
            if (request.blockedUserId == userId) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "You cannot block yourself")
                )
            }
            if (eventId != null) {
                if (!hasModerationEventAccess(database, eventId, userId)) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You cannot block users for this event")
                    )
                }
                if (!hasModerationEventAccess(database, eventId, request.blockedUserId)) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Blocked user is not part of this event")
                    )
                }
            }
            val block = moderationRepository.blockUser(
                id = "block-${UUID.randomUUID()}",
                blockerUserId = userId,
                blockedUserId = request.blockedUserId,
                eventId = eventId,
                reason = request.reason
            )

            call.respond(HttpStatusCode.Created, block)
        }

        delete("/blocks/{blockedUserId}") {
            val userId = call.authenticatedUserId() ?: return@delete call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val blockedUserId = call.parameters["blockedUserId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Blocked user ID required")
            )
            val eventId = call.request.queryParameters["eventId"]
            moderationRepository.unblockUser(userId, blockedUserId, eventId)

            call.respond(HttpStatusCode.NoContent)
        }

        post("/reports/{reportId}/decisions") {
            val principal = call.principal<JWTPrincipal>() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication required")
            )
            val userId = principal.payload.getClaim("userId")?.asString() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Invalid user ID in token")
            )
            if (!principal.canModerate()) {
                moderationRepository.recordDecision(
                    id = "decision-${UUID.randomUUID()}",
                    moderatorId = userId,
                    reportId = call.parameters["reportId"],
                    targetType = ReportTarget.USER,
                    targetId = userId,
                    action = ModerationDecisionAction.ESCALATE,
                    reason = "Unauthorized moderation review attempt",
                    outcome = ModerationAuditOutcome.DENIED
                )
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Moderator role required"))
            }

            val reportId = call.parameters["reportId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Report ID required")
            )
            val request = call.receive<CreateModerationDecisionRequest>()
            val decision = moderationRepository.recordDecision(
                id = "decision-${UUID.randomUUID()}",
                moderatorId = userId,
                reportId = reportId,
                targetType = request.targetType,
                targetId = request.targetId,
                action = request.action,
                reason = request.reason,
                outcome = ModerationAuditOutcome.ACCEPTED
            )

            call.respond(HttpStatusCode.Created, decision)
        }
    }
}

@Serializable
data class CreateContentReportRequest(
    val targetType: ReportTarget,
    val targetId: String,
    val eventId: String? = null,
    val reason: ReportReason,
    val details: String? = null
)

@Serializable
data class CreateUserBlockRequest(
    val blockedUserId: String,
    val eventId: String? = null,
    val reason: ReportReason? = null
)

@Serializable
data class CreateModerationDecisionRequest(
    val targetType: ReportTarget,
    val targetId: String,
    val action: ModerationDecisionAction,
    val reason: String
)

private fun io.ktor.server.application.ApplicationCall.authenticatedUserId(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

private fun hasModerationEventAccess(database: WakeveDb, eventId: String, userId: String): Boolean {
    val event = database.eventQueries.selectById(eventId).executeAsOneOrNull() ?: return false
    if (event.organizerId == userId) return true
    return database.participantQueries
        .selectByEventIdAndUserId(eventId, userId)
        .executeAsOneOrNull() != null
}

private fun JWTPrincipal.canModerate(): Boolean {
    val role = payload.getClaim("role")?.asString()
    if (role == "MODERATOR" || role == "ADMIN") return true
    return payload.getClaim("permissions").asList(String::class.java)?.contains("MODERATE") == true
}
