package com.guyghost.wakeve.routes

import com.guyghost.wakeve.SessionManager
import com.guyghost.wakeve.auth.sessionId
import com.guyghost.wakeve.auth.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class SessionResponse(
    val id: String,
    val deviceName: String,
    val deviceId: String,
    val ipAddress: String?,
    val createdAt: String,
    val lastAccessed: String,
    val isCurrent: Boolean
)

@Serializable
data class SessionListResponse(
    val sessions: List<SessionResponse>,
    val total: Int
)

@Serializable
data class RevokeSessionRequest(
    val sessionId: String
)

@Serializable
data class RevokeSessionResponse(
    val success: Boolean,
    val message: String
)

/**
 * Session management routes
 */
fun Route.sessionRoutes(sessionManager: SessionManager) {

    route("/api/sessions") {

        // Get all active sessions for the authenticated user
        authenticate("auth-jwt") {
            get {
                try {
                    val principal = call.principal<JWTPrincipal>()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, "Not authenticated")

                    val userId = principal.userId
                    val currentSessionId = principal.sessionId

                    // Get all sessions for this user
                    val sessions = sessionManager.getUserSessions(userId)

                    val sessionResponses = sessions.map { session ->
                        SessionResponse(
                            id = session.id,
                            deviceName = session.deviceName,
                            deviceId = session.deviceId,
                            ipAddress = session.ipAddress,
                            createdAt = session.createdAt,
                            lastAccessed = session.lastAccessed,
                            isCurrent = session.id == currentSessionId
                        )
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        SessionListResponse(
                            sessions = sessionResponses,
                            total = sessionResponses.size
                        )
                    )
                } catch (e: Exception) {
                    call.application.log.error("Error fetching sessions", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to fetch sessions: ${e.message}")
                    )
                }
            }
        }

        // Revoke a specific session
        authenticate("auth-jwt") {
            delete("/{sessionId}") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                        ?: return@delete call.respond(HttpStatusCode.Unauthorized, "Not authenticated")

                    val userId = principal.userId
                    val currentSessionId = principal.sessionId
                    val sessionIdToRevoke = call.parameters["sessionId"]
                        ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Session ID required")
                        )

                    // Prevent revoking current session
                    if (sessionIdToRevoke == currentSessionId) {
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            RevokeSessionResponse(
                                success = false,
                                message = "Cannot revoke current session. Use logout instead."
                            )
                        )
                    }

                    // Verify session belongs to user
                    val session = sessionManager.getSession(sessionIdToRevoke)
                    if (session == null || session.userId != userId) {
                        return@delete call.respond(
                            HttpStatusCode.NotFound,
                            RevokeSessionResponse(
                                success = false,
                                message = "Session not found"
                            )
                        )
                    }

                    // Revoke the session
                    sessionManager.revokeSession(sessionIdToRevoke)

                    call.respond(
                        HttpStatusCode.OK,
                        RevokeSessionResponse(
                            success = true,
                            message = "Session revoked successfully"
                        )
                    )
                } catch (e: Exception) {
                    call.application.log.error("Error revoking session", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        RevokeSessionResponse(
                            success = false,
                            message = "Failed to revoke session: ${e.message}"
                        )
                    )
                }
            }
        }

        // Revoke all other sessions (except current)
        authenticate("auth-jwt") {
            post("/revoke-all-others") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, "Not authenticated")

                    val userId = principal.userId
                    val currentSessionId = principal.sessionId

                    // Get all sessions for this user
                    val sessions = sessionManager.getUserSessions(userId)

                    // Revoke all except current
                    var revokedCount = 0
                    sessions.forEach { session ->
                        if (session.id != currentSessionId) {
                            sessionManager.revokeSession(session.id)
                            revokedCount++
                        }
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "success" to true,
                            "message" to "Revoked $revokedCount session(s)",
                            "revokedCount" to revokedCount
                        )
                    )
                } catch (e: Exception) {
                    call.application.log.error("Error revoking sessions", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf(
                            "success" to false,
                            "error" to "Failed to revoke sessions: ${e.message}"
                        )
                    )
                }
            }
        }

        // Get current session info
        authenticate("auth-jwt") {
            get("/current") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, "Not authenticated")

                    val sessionId = principal.sessionId
                    val session = sessionManager.getSession(sessionId)

                    if (session == null) {
                        return@get call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to "Current session not found")
                        )
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        SessionResponse(
                            id = session.id,
                            deviceName = session.deviceName,
                            deviceId = session.deviceId,
                            ipAddress = session.ipAddress,
                            createdAt = session.createdAt,
                            lastAccessed = session.lastAccessed,
                            isCurrent = true
                        )
                    )
                } catch (e: Exception) {
                    call.application.log.error("Error fetching current session", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to fetch current session: ${e.message}")
                    )
                }
            }
        }
    }
}
