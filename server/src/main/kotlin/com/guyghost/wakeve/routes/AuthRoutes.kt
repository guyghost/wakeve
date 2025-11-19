package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.AuthenticationService
import com.guyghost.wakeve.auth.OAuth2Exception
import com.guyghost.wakeve.models.OAuthLoginRequest
import com.guyghost.wakeve.models.TokenRefreshRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

/**
 * Authentication routes for OAuth2 login
 */
fun Route.authRoutes(authService: AuthenticationService) {
    route("/auth") {

        // Google OAuth2 login
        post("/google") {
            try {
                val request = call.receive<OAuthLoginRequest>()
                require(request.provider == "google") {
                    "Provider must be 'google' for this endpoint"
                }

                val response = authService.loginWithOAuth(request).getOrThrow()
                call.respond(HttpStatusCode.OK, response)
            } catch (e: OAuth2Exception) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Authentication failed"))
            }
        }

        // Apple OAuth2 login
        post("/apple") {
            try {
                val request = call.receive<OAuthLoginRequest>()
                require(request.provider == "apple") {
                    "Provider must be 'apple' for this endpoint"
                }

                val response = authService.loginWithOAuth(request).getOrThrow()
                call.respond(HttpStatusCode.OK, response)
            } catch (e: OAuth2Exception) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Authentication failed"))
            }
        }

        // Token refresh
        post("/refresh") {
            try {
                val refreshRequest = call.receive<TokenRefreshRequest>()
                val response = authService.refreshToken(refreshRequest.refreshToken).getOrThrow()
                call.respond(HttpStatusCode.OK, response)
            } catch (e: OAuth2Exception) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Token refresh failed"))
            }
        }

        // Get authorization URLs (for frontend redirect)
        get("/google/url") {
            try {
                val state = call.request.queryParameters["state"] ?: UUID.randomUUID().toString()
                val service = authService // We'll need to expose the OAuth service
                // TODO: Implement getAuthorizationUrl in AuthenticationService
                call.respond(HttpStatusCode.OK, mapOf("url" to "https://accounts.google.com/oauth/authorize?state=$state"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to generate auth URL"))
            }
        }

        get("/apple/url") {
            try {
                val state = call.request.queryParameters["state"] ?: UUID.randomUUID().toString()
                // TODO: Implement getAuthorizationUrl in AuthenticationService
                call.respond(HttpStatusCode.OK, mapOf("url" to "https://appleid.apple.com/auth/authorize?state=$state"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to generate auth URL"))
            }
        }
    }
}