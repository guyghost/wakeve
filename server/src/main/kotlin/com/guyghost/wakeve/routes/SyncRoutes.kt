package com.guyghost.wakeve.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.guyghost.wakeve.sync.SyncService
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Route.syncRoutes(syncService: SyncService) {
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