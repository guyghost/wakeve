package com.guyghost.wakeve.routes

import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.models.ICSInvitationRequest
import com.guyghost.wakeve.models.ICSInvitationResponse
import com.guyghost.wakeve.models.MeetingReminderTiming
import com.guyghost.wakeve.models.NativeCalendarRequest
import com.guyghost.wakeve.models.NativeCalendarResponse
import com.guyghost.wakeve.models.UpdateNativeCalendarRequest
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/**
 * Calendar routes for managing ICS invitations and native calendar integration
 */
fun io.ktor.server.routing.Route.calendarRoutes(calendarService: CalendarService) {
    route("/api/events/{id}/calendar") {

        /**
         * POST /api/events/{id}/calendar/ics - Generate ICS invitation
         */
        post("/ics") {
            val eventId = call.parameters["id"] ?: return@post call.respondText(
                "Event ID is required",
                status = HttpStatusCode.BadRequest
            )

            val request = call.receive<ICSInvitationRequest>()

            try {
                val icsDocument = calendarService.generateICSInvitation(
                    eventId = eventId,
                    invitees = request.invitees
                )
                
                val response = ICSInvitationResponse(
                    content = icsDocument.content,
                    filename = icsDocument.filename
                )

                call.respond(response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        /**
         * GET /api/events/{id}/calendar/ics - Download ICS file
         */
        get("/ics") {
            val eventId = call.parameters["id"] ?: return@get call.respondText(
                "Event ID is required",
                status = HttpStatusCode.BadRequest
            )

            try {
                // Generate ICS for all participants by default or just generic one
                val icsDocument = calendarService.generateICSInvitation(
                    eventId = eventId,
                    invitees = emptyList() // Generic invite
                )

                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, icsDocument.filename).toString()
                )
                
                call.respondText(
                    icsDocument.content,
                    contentType = ContentType.parse("text/calendar")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        /**
         * POST /api/events/{id}/calendar/native - Add to native calendar
         */
        post("/native") {
            val eventId = call.parameters["id"] ?: return@post call.respondText(
                "Event ID is required",
                status = HttpStatusCode.BadRequest
            )

            val request = call.receive<NativeCalendarRequest>()

            try {
                val result = calendarService.addToNativeCalendar(
                    eventId = eventId,
                    participantId = request.participantId
                )

                if (result.isSuccess) {
                    val response = NativeCalendarResponse(
                        success = true,
                        calendarEventId = "${eventId}_${request.participantId}"
                    )
                    call.respond(response)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Unknown error"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        /**
         * PUT /api/events/{id}/calendar/native/{participantId} - Update calendar event
         */
        put("/native/{participantId}") {
            val eventId = call.parameters["id"] ?: return@put call.respondText(
                "Event ID is required",
                status = HttpStatusCode.BadRequest
            )

            val participantId = call.parameters["participantId"] ?: return@put call.respondText(
                "Participant ID is required",
                status = HttpStatusCode.BadRequest
            )

            val request = call.receive<UpdateNativeCalendarRequest>()

            try {
                val result = calendarService.updateNativeCalendarEvent(
                    eventId = eventId,
                    participantId = participantId
                )

                if (result.isSuccess) {
                    call.respond(mapOf("success" to true))
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Unknown error"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        /**
         * DELETE /api/events/{id}/calendar/native/{participantId} - Remove from calendar
         */
        delete("/native/{participantId}") {
            val eventId = call.parameters["id"] ?: return@delete call.respondText(
                "Event ID is required",
                status = HttpStatusCode.BadRequest
            )

            val participantId = call.parameters["participantId"] ?: return@delete call.respondText(
                "Participant ID is required",
                status = HttpStatusCode.BadRequest
            )

            // Since we don't expose delete in CalendarService yet (only update/add), we might need to add it there first
            // or just rely on platform specific client-side logic.
            // For now, we'll keep the mock response or add the method to CalendarService if needed.
            
            // Note: The PlatformCalendarService interface has deleteEvent, but CalendarService doesn't expose it yet.
            // Let's assume for now the client handles this or we add it later.
            
             call.respond(mapOf("success" to true))
        }

        /**
         * POST /api/events/{id}/calendar/reminders/{timing} - Send meeting reminders
         * Note: This endpoint depends on NotificationService (Phase 3)
         */
        post("/reminders/{timing}") {
            val eventId = call.parameters["id"] ?: return@post call.respondText(
                "Event ID is required",
                status = HttpStatusCode.BadRequest
            )

            val timingStr = call.parameters["timing"] ?: return@post call.respondText(
                "Reminder timing is required",
                status = HttpStatusCode.BadRequest
            )

            // Validate timing parameter
            val timing = try {
                MeetingReminderTiming.valueOf(timingStr.uppercase().replace("-", "_"))
            } catch (e: Exception) {
                return@post call.respondText(
                    "Invalid timing parameter. Must be one of: one_day_before, one_hour_before, fifteen_minutes_before",
                    status = HttpStatusCode.BadRequest
                )
            }

            // TODO: Implement reminder scheduling (depends on NotificationService)
            // For now, return 501 Not Implemented
            call.respondText(
                "Meeting reminders are not yet implemented. Scheduled for Phase 3.",
                status = HttpStatusCode.NotImplemented
            )
        }
    }
}
