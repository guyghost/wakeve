package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.AuthenticationService
import com.guyghost.wakeve.auth.EmailOtpSender
import com.guyghost.wakeve.auth.NoConfiguredEmailOtpSender
import com.guyghost.wakeve.auth.OtpManager
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
fun Route.authRoutes(
    authService: AuthenticationService,
    otpManager: OtpManager = OtpManager(),
    emailOtpSender: EmailOtpSender = NoConfiguredEmailOtpSender
) {
    route("/auth") {
        
        // ========== OAuth Authentication ==========
        
        // Google OAuth2 login
        post("/google") {
            try {
                val request = call.receive<GoogleAuthRequest>()
                val idToken = request.idToken.trim()
                
                // Validate request
                if (!isValidAuthSecret(idToken)) {
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
                        idToken = idToken
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
                val idToken = request.idToken.trim()
                
                // Validate request
                if (!isValidAuthSecret(idToken)) {
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
                        idToken = idToken
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
                val email = normalizeEmail(request.email)
                
                // Validate email format
                if (!isValidEmail(email)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("INVALID_EMAIL", "Invalid email format")
                    )
                    return@post
                }
                
                // Vérifier le rate limiting
                if (otpManager.isRateLimited(email)) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        AuthErrorResponse(
                            "RATE_LIMITED",
                            "Trop de demandes OTP. Réessayez dans quelques minutes."
                        )
                    )
                    return@post
                }

                // Générer et stocker l'OTP
                val otp = otpManager.generateOtp(email)

                if (otp == null) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        AuthErrorResponse(
                            "RATE_LIMITED",
                            "Trop de demandes OTP. Réessayez dans quelques minutes."
                        )
                    )
                    return@post
                }

                val deliveryResult = try {
                    emailOtpSender.sendOtp(
                        email = email,
                        otp = otp,
                        expiresInSeconds = 300
                    )
                } catch (error: Throwable) {
                    Result.failure(error)
                }

                if (deliveryResult.isFailure) {
                    otpManager.discardOtp(email)
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        AuthErrorResponse(
                            "EMAIL_OTP_UNAVAILABLE",
                            emailOtpUnavailableMessage()
                        )
                    )
                    return@post
                }

                call.respond(
                    HttpStatusCode.OK,
                    OTPRequestResponse(
                        success = true,
                        message = "OTP envoyé à $email",
                        expiresInSeconds = 300
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
                val email = normalizeEmail(request.email)
                val otp = request.otp.trim()
                
                // Validate OTP format (6 digits)
                if (otp.length != 6 || !otp.all { it.isDigit() }) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("INVALID_OTP", "OTP must be 6 digits")
                    )
                    return@post
                }
                
                // Valider le format de l'email
                if (!isValidEmail(email)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("INVALID_EMAIL", "Invalid email format")
                    )
                    return@post
                }

                // Vérifier l'OTP
                val isValid = otpManager.verifyOtp(email, otp)

                if (!isValid) {
                    val remaining = otpManager.remainingAttempts(email)
                    val message = if (remaining > 0) {
                        "Code OTP invalide. $remaining tentative(s) restante(s)."
                    } else {
                        "Code OTP invalide ou expiré. Veuillez demander un nouveau code."
                    }
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        AuthErrorResponse("INVALID_OTP", message)
                    )
                    return@post
                }

                // OTP valide : authentifier l'utilisateur
                val authResponse = authService.loginWithEmail(email).getOrThrow()
                call.respond(HttpStatusCode.OK, authResponse)
            } catch (e: Exception) {
                call.handleAuthError(e)
            }
        }
        
        // ========== Guest Session ==========
        
        // Create guest session
        post("/guest") {
            try {
                val request = call.receive<GuestSessionRequest>()
                val deviceId = request.deviceId?.trim()
                if (deviceId != null && (deviceId.isBlank() || deviceId.length > MAX_DEVICE_ID_LENGTH)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("INVALID_DEVICE_ID", "Device ID is invalid")
                    )
                    return@post
                }
                val authResponse = authService.loginAsGuest(deviceId).getOrThrow()
                call.respond(HttpStatusCode.OK, authResponse)
            } catch (e: Exception) {
                call.handleAuthError(e)
            }
        }
        
        // ========== Token Management ==========
        
        // Token refresh
        post("/refresh") {
            try {
                val request = call.receive<TokenRefreshRequest>()
                val refreshToken = request.refreshToken.trim()
                
                if (!isValidAuthSecret(refreshToken)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("INVALID_REQUEST", "Refresh token is required")
                    )
                    return@post
                }
                
                val response = authService.refreshToken(refreshToken).getOrThrow()
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.handleAuthError(e)
            }
        }
        
        // ========== Utility Endpoints ==========
        
        // Get authorization URLs (for frontend redirect)
        get("/google/url") {
            try {
                val state = call.request.queryParameters["state"].normalizedOAuthState()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("INVALID_STATE", "OAuth state is invalid")
                    )
                val url = authService.getAuthorizationUrl(OAuthProvider.GOOGLE, state)
                call.respond(HttpStatusCode.OK, mapOf("url" to url))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to generate auth URL"))
            }
        }
        
        get("/apple/url") {
            try {
                val state = call.request.queryParameters["state"].normalizedOAuthState()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("INVALID_STATE", "OAuth state is invalid")
                    )
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
    if (email.length !in 3..MAX_EMAIL_LENGTH) return false
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    return emailRegex.matches(email)
}

private const val MAX_EMAIL_LENGTH = 320
private const val MAX_AUTH_SECRET_LENGTH = 8_192
private const val MAX_DEVICE_ID_LENGTH = 200
private const val MAX_OAUTH_STATE_LENGTH = 512

private fun normalizeEmail(email: String): String = email.trim().lowercase()

private fun isValidAuthSecret(secret: String): Boolean =
    secret.isNotBlank() && secret.length <= MAX_AUTH_SECRET_LENGTH

private fun String?.normalizedOAuthState(): String? {
    val state = this?.trim().takeUnless { it.isNullOrEmpty() } ?: UUID.randomUUID().toString()
    return state.takeIf { it.length <= MAX_OAUTH_STATE_LENGTH }
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
                AuthErrorResponse("INVALID_REQUEST", invalidAuthRequestFailureMessage())
            )
        }
        is com.guyghost.wakeve.auth.OAuth2Exception -> {
            respond(
                HttpStatusCode.Unauthorized,
                AuthErrorResponse("AUTH_FAILED", authFailedFailureMessage())
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

internal fun emailOtpUnavailableMessage(): String =
    "Email OTP delivery is temporarily unavailable. Please try again later."

internal fun invalidAuthRequestFailureMessage(): String =
    "Invalid authentication request. Please review the details and try again."

internal fun authFailedFailureMessage(): String =
    "Authentication failed. Please try again."
