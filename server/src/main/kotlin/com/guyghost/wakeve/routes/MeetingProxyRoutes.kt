package com.guyghost.wakeve.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

/**
 * Backend proxy routes for securing external meeting platform APIs
 *
 * This proxy hides API keys for Zoom, Google Meet, etc. from the client.
 * The server manages authentication with external platforms and only exposes
 * necessary data to the client.
 *
 * ## Security
 * - API keys are stored in environment variables
 * - Never exposed to clients
 * - All external API calls happen server-side
 *
 * ## Endpoints
 *
 * ### Zoom
 * - POST /api/meetings/proxy/zoom/create - Create Zoom meeting
 * - POST /api/meetings/proxy/zoom/{meetingId}/cancel - Cancel Zoom meeting
 * - GET /api/meetings/proxy/zoom/{meetingId}/status - Get Zoom meeting status
 *
 * ### Google Meet
 * - POST /api/meetings/proxy/google-meet/create - Create Google Meet meeting
 *
 * ## Configuration
 *
 * Required environment variables:
 * - ZOOM_API_KEY: Zoom API key
 * - ZOOM_API_SECRET: Zoom API secret
 * - GOOGLE_MEET_CREDENTIALS: Google Calendar API credentials (JSON)
 */
fun Route.meetingProxyRoutes() {
    route("/api/meetings/proxy") {

        // ============================================================
        // Zoom Meeting Endpoints
        // ============================================================

        /**
         * POST /api/meetings/proxy/zoom/create
         *
         * Creates a new Zoom meeting via Zoom API
         *
         * Request body:
         * ```json
         * {
         *   "title": "Planning Meeting",
         *   "description": "Kickoff session",
         *   "scheduledFor": "2026-02-08T14:00:00Z",
         *   "duration": 60,
         *   "timezone": "Europe/Paris",
         *   "participantLimit": 100,
         *   "requirePassword": true,
         *   "waitingRoom": true
         * }
         * ```
         *
         * Response:
         * ```json
         * {
         *   "meetingId": "123456789",
         *   "joinUrl": "https://zoom.us/j/123456789?pwd=abc123",
         *   "password": "abc123",
         *   "hostUrl": "https://zoom.us/j/123456789?pwd=xyz789",
         *   "hostKey": "123456",
         *   "dialInNumber": "+33 1 23 45 67 89",
         *   "dialInPassword": "123456"
         * }
         * ```
         */
        post("/zoom/create") {
            try {
                val request = call.receive<CreateZoomMeetingRequest>()

                // Validate required fields
                if (request.title.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "title is required")
                    )
                }

                // Check if Zoom API credentials are configured
                val apiKey = System.getenv("ZOOM_API_KEY")
                if (apiKey.isNullOrBlank()) {
                    return@post call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        mapOf(
                            "error" to "zoom_not_configured",
                            "message" to "Zoom API is not configured on the server"
                        )
                    )
                }

                // In production, this would call the actual Zoom API:
                // POST https://api.zoom.us/v2/users/me/meetings
                // With Authorization: Bearer <JWT>
                // For now, return a mock response
                val response = CreateZoomMeetingResponse(
                    meetingId = generateZoomMeetingId(),
                    joinUrl = generateZoomJoinUrl(request.title),
                    password = generateZoomPassword(),
                    hostUrl = generateZoomHostUrl(),
                    hostKey = generateHostKey(),
                    dialInNumber = "+33 1 23 45 67 89",
                    dialInPassword = "123456"
                )

                call.respond(response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error creating Zoom meeting"))
                )
            }
        }

        /**
         * POST /api/meetings/proxy/zoom/{meetingId}/cancel
         *
         * Cancels a Zoom meeting
         *
         * Path parameter:
         * - meetingId: The Zoom meeting ID to cancel
         *
         * Response:
         * ```json
         * {
         *   "success": true,
         *   "message": "Meeting cancelled successfully"
         * }
         * ```
         */
        post("/zoom/{meetingId}/cancel") {
            val meetingId = call.parameters["meetingId"]

            if (meetingId.isNullOrBlank()) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "meetingId is required")
                )
            }

            try {
                // In production, this would call:
                // DELETE https://api.zoom.us/v2/meetings/{meetingId}
                // For now, return mock response
                call.respond(
                    mapOf(
                        "success" to true,
                        "message" to "Meeting $meetingId cancelled successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error cancelling Zoom meeting"))
                )
            }
        }

        /**
         * GET /api/meetings/proxy/zoom/{meetingId}/status
         *
         * Gets the status of a Zoom meeting
         *
         * Path parameter:
         * - meetingId: The Zoom meeting ID to query
         *
         * Response:
         * ```json
         * {
         *   "meetingId": "123456789",
         *   "status": "scheduled",
         *   "startTime": "2026-02-08T14:00:00Z",
         *   "duration": 60,
         *   "participantCount": 0
         * }
         * ```
         */
        get("/zoom/{meetingId}/status") {
            val meetingId = call.parameters["meetingId"]

            if (meetingId.isNullOrBlank()) {
                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "meetingId is required")
                )
            }

            try {
                // In production, this would call:
                // GET https://api.zoom.us/v2/meetings/{meetingId}
                // For now, return mock response
                val response = ZoomMeetingStatusResponse(
                    meetingId = meetingId,
                    status = "scheduled",
                    startTime = "2026-02-08T14:00:00Z",
                    duration = 60,
                    participantCount = 0
                )

                call.respond(response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error getting Zoom meeting status"))
                )
            }
        }

        // ============================================================
        // Google Meet Endpoints
        // ============================================================

        /**
         * POST /api/meetings/proxy/google-meet/create
         *
         * Creates a new Google Meet meeting via Google Calendar API
         *
         * Request body:
         * ```json
         * {
         *   "title": "Planning Meeting",
         *   "description": "Kickoff session",
         *   "scheduledFor": "2026-02-08T14:00:00Z",
         *   "duration": 60,
         *   "timezone": "Europe/Paris"
         * }
         * ```
         *
         * Response:
         * ```json
         * {
         *   "meetingUrl": "https://meet.google.com/abc-def-ghi",
         *   "meetingCode": "abc-def-ghi"
         * }
         * ```
         */
        post("/google-meet/create") {
            try {
                val request = call.receive<CreateGoogleMeetRequest>()

                // Validate required fields
                if (request.title.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "title is required")
                    )
                }

                // Check if Google Meet API credentials are configured
                val credentials = System.getenv("GOOGLE_MEET_CREDENTIALS")
                if (credentials.isNullOrBlank()) {
                    return@post call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        mapOf(
                            "error" to "google_meet_not_configured",
                            "message" to "Google Meet API is not configured on the server"
                        )
                    )
                }

                // In production, this would call Google Calendar API:
                // POST https://www.googleapis.com/calendar/v3/calendars/primary/events
                // With conferenceDataVersion: 1 and conferenceData: { createRequest: { requestId: "..." } }
                // For now, return a mock response
                val meetingCode = generateGoogleMeetCode()
                val response = CreateGoogleMeetResponse(
                    meetingUrl = "https://meet.google.com/$meetingCode",
                    meetingCode = meetingCode
                )

                call.respond(response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error creating Google Meet meeting"))
                )
            }
        }
    }
}

// ============================================================================
// Request/Response DTOs
// ============================================================================

/**
 * Request to create a Zoom meeting
 */
@Serializable
data class CreateZoomMeetingRequest(
    val title: String,
    val description: String? = null,
    val scheduledFor: String, // ISO-8601 datetime
    val duration: Int, // Duration in minutes
    val timezone: String = "UTC",
    val participantLimit: Int? = null,
    val requirePassword: Boolean = true,
    val waitingRoom: Boolean = true
) {
    init {
        // Validate duration: Zoom allows 1 to 1440 minutes (24 hours)
        require(duration in 1..1440) { "Duration must be between 1 and 1440 minutes" }
        // Validate title length to prevent DoS
        require(title.length in 1..200) { "Title must be between 1 and 200 characters" }
        // Validate description length
        require(description == null || description.length <= 5000) { "Description must not exceed 5000 characters" }
    }
}

/**
 * Response from creating a Zoom meeting
 */
@Serializable
data class CreateZoomMeetingResponse(
    val meetingId: String,
    val joinUrl: String,
    val password: String,
    val hostUrl: String,
    val hostKey: String,
    val dialInNumber: String,
    val dialInPassword: String
)

/**
 * Response from getting Zoom meeting status
 */
@Serializable
data class ZoomMeetingStatusResponse(
    val meetingId: String,
    val status: String, // "scheduled", "started", "ended", "cancelled"
    val startTime: String, // ISO-8601 datetime
    val duration: Int, // Duration in minutes
    val participantCount: Int
)

/**
 * Request to create a Google Meet meeting
 */
@Serializable
data class CreateGoogleMeetRequest(
    val title: String,
    val description: String? = null,
    val scheduledFor: String, // ISO-8601 datetime
    val duration: Int, // Duration in minutes
    val timezone: String = "UTC"
) {
    init {
        // Google Meet typically allows up to 24 hours
        require(duration in 1..1440) { "Duration must be between 1 and 1440 minutes" }
        require(title.length in 1..200) { "Title must be between 1 and 200 characters" }
        require(description == null || description.length <= 5000) { "Description must not exceed 5000 characters" }
    }
}

/**
 * Response from creating a Google Meet meeting
 */
@Serializable
data class CreateGoogleMeetResponse(
    val meetingUrl: String,
    val meetingCode: String
)

// ============================================================================
// Mock Helpers (Replace with actual API calls in production)
// ============================================================================

private fun generateZoomMeetingId(): String {
    return (1..10).map { (0..9).random() }.joinToString("")
}

private fun generateZoomJoinUrl(title: String): String {
    val meetingId = generateZoomMeetingId()
    val password = generateZoomPassword()
    return "https://zoom.us/j/$meetingId?pwd=$password"
}

private fun generateZoomHostUrl(): String {
    val meetingId = generateZoomMeetingId()
    val password = generateZoomPassword()
    return "https://zoom.us/j/$meetingId?pwd=$password"
}

private fun generateZoomPassword(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..6).map { chars.random() }.joinToString("")
}

private fun generateHostKey(): String {
    return (1..6).map { (0..9).random() }.joinToString("")
}

private fun generateGoogleMeetCode(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz-"
    val part1 = (1..3).map { chars.random() }.joinToString("")
    val part2 = (1..3).map { chars.random() }.joinToString("")
    val part3 = (1..4).map { chars.random() }.joinToString("")
    return "$part1-$part2-$part3"
}
