package com.guyghost.wakeve.routes

import kotlinx.serialization.json.Json

import com.guyghost.wakeve.database.WakeveDb
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.util.UUID

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class CreateMeetingRequest(
    val platform: String,           // ZOOM, GOOGLE_MEET, FACETIME
    val title: String,
    val description: String? = null,
    val startTime: String,          // ISO 8601 UTC
    val duration: String = "1h",
    val meetingLink: String? = null
)

/**
 * Server-side persisted meetings (proposal #45): satisfies MEETING_REQUIRED in
 * the finalization checklist via the API — previously only the client-side
 * MeetingService could write meetings (QA-13).
 */
fun io.ktor.server.routing.Route.meetingRoutes(database: WakeveDb) {
    route("/events/{eventId}/meetings/persisted") {

        // GET — list persisted meetings for the event (members only)
        get {
            val eventId = call.parameters["eventId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Event ID required")
            )
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
            val event = database.eventQueries.selectById(eventId).executeAsOneOrNull()
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Event not found"))
            val isMember = event.organizerId == userId || database.participantQueries
                .selectByEventIdAndUserId(eventId, userId).executeAsOneOrNull() != null
            if (!isMember) {
                return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You do not have access to this event"))
            }

            val meetings = database.meetingQueries.selectByEventId(eventId).executeAsList()
            call.respond(HttpStatusCode.OK, mapOf(
                "meetings" to meetings.map { m ->
                    mapOf(
                        "id" to m.id,
                        "title" to m.title,
                        "platform" to m.platform,
                        "startTime" to m.startTime,
                        "duration" to m.duration,
                        "meetingLink" to m.meetingLink,
                        "status" to m.status
                    )
                }
            ))
        }

        // POST — organizer creates a persisted meeting
        post {
            val eventId = call.parameters["eventId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Event ID required")
            )
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
            val event = database.eventQueries.selectById(eventId).executeAsOneOrNull()
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Event not found"))
            if (event.organizerId != userId) {
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to "Only the event organizer can create meetings")
                )
            }

            val request = try {
                call.receive<CreateMeetingRequest>()
            } catch (e: Exception) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid meeting payload: platform, title, startTime are required")
                )
            }

            val platform = request.platform.trim().uppercase()
            if (platform !in setOf("ZOOM", "GOOGLE_MEET", "FACETIME")) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Unsupported platform: ${request.platform}. Use ZOOM, GOOGLE_MEET or FACETIME")
                )
            }
            if (request.title.isBlank() || request.startTime.isBlank()) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Meeting title and startTime are required")
                )
            }

            val now = OffsetDateTime.now().toString()
            val meetingId = "mtg_${UUID.randomUUID()}"
            val link = request.meetingLink ?: when (platform) {
                "FACETIME" -> "https://facetime.apple.com/join#${meetingId.takeLast(8)}"
                "GOOGLE_MEET" -> "https://meet.google.com/${meetingId.takeLast(9)}"
                else -> ""
            }
            val status = if (request.meetingLink.isNullOrBlank() && platform == "ZOOM") "PENDING_LINK" else "SCHEDULED"

            database.meetingQueries.insertMeeting(
                id = meetingId,
                eventId = eventId,
                organizerId = event.organizerId,
                title = request.title.trim(),
                description = request.description,
                startTime = request.startTime,
                duration = request.duration,
                platform = platform,
                meetingLink = link,
                provider = "",
                displayLabel = request.title.trim(),
                targetUrl = link,
                creatorId = userId,
                verificationState = "UNVERIFIED",
                hostMeetingId = meetingId,
                password = "",
                invitedParticipants = json.encodeToString(
                    kotlinx.serialization.serializer(),
                    database.participantQueries.selectByEventId(eventId).executeAsList().map { it.userId }
                ),
                status = "SCHEDULED",
                createdAt = now
            )

            // Rappel par défaut (proposal #45): une réunion créée a un rappel
            // 24h avant — satisfait NOTIFICATION_REMINDERS_REQUIRED et honore
            // la promesse « rappels » de l'agent Réunions.
            database.meetingReminderQueries.insertMeetingReminder(
                id = "rem_${UUID.randomUUID()}",
                meeting_id = meetingId,
                participant_id = null,
                timing = "ONE_DAY_BEFORE",
                scheduled_for = java.time.Instant.parse(request.startTime)
                    .minusSeconds(24 * 3600).toString(),
                sent_at = null,
                status = "SCHEDULED"
            )

            call.respond(HttpStatusCode.Created, mapOf(
                "id" to meetingId,
                "eventId" to eventId,
                "title" to request.title.trim(),
                "platform" to platform,
                "startTime" to request.startTime,
                "duration" to request.duration,
                "meetingLink" to link,
                "status" to status
            ))
        }
    }
}


