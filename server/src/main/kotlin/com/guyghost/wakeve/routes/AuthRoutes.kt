package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.AuthenticationService
import com.guyghost.wakeve.auth.OAuthProvider
import com.guyghost.wakeve.models.AppleAuthRequest
import com.guyghost.wakeve.models.AuthResponse
import com.guyghost.wakeve.models.AuthErrorResponse
import com.guyghost.wakeve.models.EmailOTPRequest
import com.guyghost.wakeve.models.EmailOTPVerifyRequest
import com.guyghost.wakeve.models.GuestSessionRequest
import com.guyghost.wakeve.models.GoogleAuthRequest
import com.guyghost.wakeve.models.OAuthLoginRequest
import com.guyghost.wakeve.models.OTPRequestResponse
import com.guyghost.wakeve.models.TokenRefreshRequest
import com.guyghost.wakeve.models.UserDTO
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
                val response = authService.loginWithGoogle(request.idToken, request.email, request.name)
                    .getOrThrow()
                
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                handleAuthError(e)
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
                val response = authService.loginWithApple(
                    idToken = request.idToken,
                    email = request.email,
                    name = request.name
                ).getOrThrow()
                
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                handleAuthError(e)
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
                
                // Send OTP
                val result = authService.sendOTP(request.email)
                
                if (result.isSuccess) {
                    call.respond(
                        HttpStatusCode.OK,
                        OTPRequestResponse(
                            success = true,
                            message = "OTP sent successfully",
                            expiresInSeconds = 300
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        AuthErrorResponse("SEND_FAILED", "Failed to send OTP")
                    )
                }
            } catch (e: Exception) {
                handleAuthError(e)
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
                
                // Verify OTP and authenticate
                val response = authService.verifyOTP(request.email, request.otp)
                    .getOrThrow()
                
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                handleAuthError(e)
            }
        }
        
        // ========== Guest Session ==========
        
        // Create guest session
        post("/guest") {
            try {
                val request = call.receive<GuestSessionRequest>()
                val deviceId = request.deviceId ?: generateDeviceId()
                
                // Create guest session
                val response = authService.createGuestSession(deviceId)
                    .getOrThrow()
                
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                handleAuthError(e)
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
                handleAuthError(e)
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
private suspend fun Route.handleAuthError(e: Exception) {
    when (e) {
        is IllegalArgumentException -> {
            call.respond(
                HttpStatusCode.BadRequest,
                AuthErrorResponse("INVALID_REQUEST", e.message ?: "Invalid request")
            )
        }
        is com.guyghost.wakeve.auth.OAuth2Exception -> {
            call.respond(
                HttpStatusCode.Unauthorized,
                AuthErrorResponse("AUTH_FAILED", e.message ?: "Authentication failed")
            )
        }
        else -> {
            call.respond(
                HttpStatusCode.InternalServerError,
                AuthErrorResponse("INTERNAL_ERROR", "An unexpected error occurred")
            )
        }
    }
}