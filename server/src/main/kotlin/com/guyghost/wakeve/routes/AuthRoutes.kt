package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.AuthenticationService
import com.guyghost.wakeve.models.AppleAuthRequest
import com.guyghost.wakeve.models.AuthErrorResponse
import com.guyghost.wakeve.models.EmailOTPRequest
import com.guyghost.wakeve.models.EmailOTPVerifyRequest
import com.guyghost.wakeve.models.GuestSessionRequest
import com.guyghost.wakeve.models.GoogleAuthRequest
import com.guyghost.wakeve.models.OAuthLoginRequest
import com.guyghost.wakeve.models.OTPRequestResponse
import com.guyghost.wakeve.models.OAuthProvider
import com.guyghost.wakeve.models.TokenRefreshRequest
import io.ktor.server.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

/**
 * Authentication routes for OAuth2 login, Email OTP, and Guest sessions.
 * 
 * Endpoints:
 * - POST /api/auth/google - Google OAuth callback
 * - POST /api/auth/apple - Apple OAuth callback
 * - POST /api/auth/email/request - Send OTP to email
 * - POST /api/auth/email/verify - Verify OTP and authenticate
 * - POST /api/auth/guest - Create guest session
 * - POST /api/auth/refresh - Refresh access token
 */
fun Route.authRoutes(authService: AuthenticationService) {
    route("/auth") {
        
        // ========== OAuth Authentication ==========
        
        // Google OAuth2 login
        post("/google") {
            try {
                val request = call.receive<GoogleAuthRequest>()
                
                // Validate request
                if (request.idToken.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("INVALID_TOKEN", "Google ID token is required")
                    )
                    return@post
                }
                
                // Process Google OAuth
                val response = authService.loginWithOAuth(
                    OAuthLoginRequest(
                        provider = OAuthProvider.GOOGLE.name.lowercase(),
                        idToken = request.idToken
                    )
                )
                    .getOrThrow()
                
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.handleAuthError(e)
            }
        }
        
        // Apple OAuth2 login
        post("/apple") {
            try {
                val request = call.receive<AppleAuthRequest>()
                
                // Validate request
                if (request.idToken.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("INVALID_TOKEN", "Apple identity token is required")
                    )
                    return@post
                }
                
                // Process Apple OAuth
                val response = authService.loginWithOAuth(
                    OAuthLoginRequest(
                        provider = OAuthProvider.APPLE.name.lowercase(),
                        idToken = request.idToken
                    )
                ).getOrThrow()
                
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.handleAuthError(e)
            }
        }
        
        // ========== Email OTP Authentication ==========
        
        // Send OTP to email
        post("/email/request") {
            try {
                val request = call.receive<EmailOTPRequest>()
                
                // Validate email format
                if (!isValidEmail(request.email)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("INVALID_EMAIL", "Invalid email format")
                    )
                    return@post
                }
                
                // TODO(auth): wire Email OTP service in backend.
                call.respond(
                    HttpStatusCode.NotImplemented,
                    OTPRequestResponse(
                        success = false,
                        message = "Email OTP flow is not enabled on this server",
                        expiresInSeconds = 0
                    )
                )
            } catch (e: Exception) {
                call.handleAuthError(e)
            }
        }
        
        // Verify OTP and authenticate
        post("/email/verify") {
            try {
                val request = call.receive<EmailOTPVerifyRequest>()
                
                // Validate OTP format (6 digits)
                if (request.otp.length != 6 || !request.otp.all { it.isDigit() }) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("INVALID_OTP", "OTP must be 6 digits")
                    )
                    return@post
                }
                
                // TODO(auth): wire Email OTP service in backend.
                call.respond(
                    HttpStatusCode.NotImplemented,
                    AuthErrorResponse("NOT_IMPLEMENTED", "Email OTP verification is not enabled")
                )
            } catch (e: Exception) {
                call.handleAuthError(e)
            }
        }
        
        // ========== Guest Session ==========
        
        // Create guest session
        post("/guest") {
            try {
                call.receive<GuestSessionRequest>() // Keep request contract for clients.
                call.respond(
                    HttpStatusCode.NotImplemented,
                    AuthErrorResponse("NOT_IMPLEMENTED", "Guest sessions are not enabled")
                )
            } catch (e: Exception) {
                call.handleAuthError(e)
            }
        }
        
        // ========== Token Management ==========
        
        // Token refresh
        post("/refresh") {
            try {
                val request = call.receive<TokenRefreshRequest>()
                
                if (request.refreshToken.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("INVALID_REQUEST", "Refresh token is required")
                    )
                    return@post
                }
                
                val response = authService.refreshToken(request.refreshToken).getOrThrow()
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.handleAuthError(e)
            }
        }
        
        // ========== Utility Endpoints ==========
        
        // Get authorization URLs (for frontend redirect)
        get("/google/url") {
            try {
                val state = call.request.queryParameters["state"] ?: UUID.randomUUID().toString()
                val url = authService.getAuthorizationUrl(OAuthProvider.GOOGLE, state)
                call.respond(HttpStatusCode.OK, mapOf("url" to url))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to generate auth URL"))
            }
        }
        
        get("/apple/url") {
            try {
                val state = call.request.queryParameters["state"] ?: UUID.randomUUID().toString()
                val url = authService.getAuthorizationUrl(OAuthProvider.APPLE, state)
                call.respond(HttpStatusCode.OK, mapOf("url" to url))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to generate auth URL"))
            }
        }
    }
}

/**
 * Validates an email address format.
 * 
 * @param email The email to validate
 * @return true if the email format is valid
 */
private fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    return emailRegex.matches(email)
}

/**
 * Generates a unique device ID for guest sessions.
 * 
 * @return A unique device identifier
 */
private fun generateDeviceId(): String {
    return "guest_${UUID.randomUUID()}"
}

/**
 * Handles authentication errors and returns appropriate HTTP responses.
 * 
 * @param e The exception that occurred
 * @param call The route call context
 */
private suspend fun ApplicationCall.handleAuthError(e: Exception) {
    when (e) {
        is IllegalArgumentException -> {
            respond(
                HttpStatusCode.BadRequest,
                AuthErrorResponse("INVALID_REQUEST", e.message ?: "Invalid request")
            )
        }
        is com.guyghost.wakeve.auth.OAuth2Exception -> {
            respond(
                HttpStatusCode.Unauthorized,
                AuthErrorResponse("AUTH_FAILED", e.message ?: "Authentication failed")
            )
        }
        else -> {
            respond(
                HttpStatusCode.InternalServerError,
                AuthErrorResponse("INTERNAL_ERROR", "An unexpected error occurred")
            )
        }
    }
}
