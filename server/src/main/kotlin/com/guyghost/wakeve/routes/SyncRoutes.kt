package com.guyghost.wakeve.routes

import com.guyghost.wakeve.models.SyncRequest
import com.guyghost.wakeve.models.SyncResponse
import com.guyghost.wakeve.sync.SyncService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun io.ktor.server.routing.Route.syncRoutes(syncService: SyncService) {
    route("/sync") {
        // POST /api/sync - Process batch sync changes from client
        post {
            try {
                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>() ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Authentication required")
                )

                val userId = principal.payload.getClaim("userId")?.asString() ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Invalid user ID in token")
                )

                // Receive sync request
                val request = call.receive<SyncRequest>()

                // Process the sync changes
                val response = syncService.processSyncChanges(request, userId)

                // Return response with appropriate status code
                val statusCode = if (response.success) {
                    if (response.conflicts.isEmpty()) HttpStatusCode.OK else HttpStatusCode.Conflict
                } else {
                    HttpStatusCode.InternalServerError
                }

                call.respond(statusCode, response)

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    SyncResponse(
                        success = false,
                        appliedChanges = 0,
                        conflicts = emptyList(),
                        serverTimestamp = "2025-12-01T12:00:00Z", // Fixed for Phase 3
                        message = "Sync request failed: ${e.message}"
                    )
                )
            }
        }
    }
}