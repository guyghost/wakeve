package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.AuthenticationService
import com.guyghost.wakeve.models.AccountDeletionResponse
import com.guyghost.wakeve.models.AuthErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.server.request.header
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete

fun Route.accountRoutes(authService: AuthenticationService) {
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
                }
            )
        )
    }
}
