package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.AuthenticationService
import com.guyghost.wakeve.models.AccountDeletionResponse
import com.guyghost.wakeve.models.AuthErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.put
import kotlinx.serialization.Serializable

fun Route.accountRoutes(authService: AuthenticationService) {
    // PUT /user/display-name - Set the authenticated user's display name.
    // The name is stored on the server-side user record and resolved from it
    // for attribution (comments, ICS organizer) — clients cannot forge it.
    put("/user/display-name") {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal?.payload?.getClaim("userId")?.asString()

        if (userId.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.Unauthorized,
                AuthErrorResponse("UNAUTHORIZED", "Authentication required")
            )
            return@put
        }

        val request = try {
            call.receive<DisplayNameRequest>()
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                AuthErrorResponse("INVALID_REQUEST", "displayName is required")
            )
            return@put
        }

        authService.updateDisplayName(userId, request.displayName).fold(
            onSuccess = {
                call.respond(HttpStatusCode.OK, DisplayNameResponse(request.displayName.trim()))
            },
            onFailure = { error ->
                val message = error.message ?: "Display name update failed"
                val status = if (message.contains("User not found")) {
                    HttpStatusCode.NotFound
                } else {
                    HttpStatusCode.BadRequest
                }
                call.respond(status, AuthErrorResponse("DISPLAY_NAME_UPDATE_FAILED", message))
            }
        )
    }

    delete("/user/delete") {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal?.payload?.getClaim("userId")?.asString()

        if (userId.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.Unauthorized,
                AuthErrorResponse("UNAUTHORIZED", "Authentication required")
            )
            return@delete
        }

        val currentJwtToken = call.request.header(HttpHeaders.Authorization)
            ?.removePrefix("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        val result = authService.deleteAccount(userId, currentJwtToken).getOrElse {
            call.respond(
                HttpStatusCode.InternalServerError,
                AuthErrorResponse("ACCOUNT_DELETION_FAILED", "Account deletion failed")
            )
            return@delete
        }

        call.respond(
            HttpStatusCode.OK,
            AccountDeletionResponse(
                success = true,
                deleted = result.deleted,
                message = if (result.deleted) {
                    "Account deleted successfully"
                } else {
                    "Account was already deleted"
                },
                localCleanupRequired = true,
                providerRevocationStatus = result.providerRevocationStatus
            )
        )
    }
}

@Serializable
data class DisplayNameRequest(val displayName: String)

@Serializable
data class DisplayNameResponse(val displayName: String)
