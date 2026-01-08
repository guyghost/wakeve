package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects for Authentication API endpoints.
 * 
 * These DTOs are used for:
 * - Request bodies from mobile apps
 * - Response bodies to mobile apps
 * 
 * GDPR Compliance:
 * - Only necessary data is transmitted
 * - No unnecessary personal information is stored or transmitted
 */

/**
 * Request body for Google OAuth callback.
 * 
 * @property idToken The Google ID token from the mobile SDK
 * @property email The user's email address (from Google account)
 * @property name The user's display name (optional, from Google account)
 */
@Serializable
data class GoogleAuthRequest(
    val idToken: String,
    val email: String,
    val name: String? = null
)

/**
 * Request body for Apple OAuth callback.
 * 
 * @property idToken The Apple identity token
 * @property email The user's email (may be null if private email relay is enabled)
 * @property name The user's real name (only provided on first sign-in)
 */
@Serializable
data class AppleAuthRequest(
    val idToken: String,
    val email: String? = null,
    val name: String? = null
)

/**
 * Request body to send OTP to an email address.
 * 
 * @property email The email address to send the OTP to
 */
@Serializable
data class EmailOTPRequest(
    val email: String
)

/**
 * Request body to verify an OTP code.
 * 
 * @property email The email address the OTP was sent to
 * @property otp The 6-digit OTP code
 */
@Serializable
data class EmailOTPVerifyRequest(
    val email: String,
    val otp: String
)

/**
 * Request body to create a guest session.
 * 
 * @property deviceId A unique identifier for the device (for guest session tracking)
 */
@Serializable
data class GuestSessionRequest(
    val deviceId: String? = null
)

/**
 * Generic error response for authentication endpoints.
 * 
 * @property error The error code
 * @property message A user-friendly error message
 * @property details Additional error details (optional)
 */
@Serializable
data class AuthErrorResponse(
    val error: String,
    val message: String,
    val details: String? = null
)

/**
 * Success response for OTP request.
 * 
 * @property success Whether the OTP was sent successfully
 * @property message A status message
 * @property expiresInSeconds How long the OTP is valid (default: 300 seconds = 5 minutes)
 */
@Serializable
data class OTPRequestResponse(
    val success: Boolean,
    val message: String,
    val expiresInSeconds: Int = 300
)

/**
 * Authentication response containing user info and tokens.
 * 
 * @property user The authenticated user information
 * @property accessToken JWT access token for API authentication
 * @property refreshToken Refresh token for obtaining new access tokens
 * @property expiresInSeconds How long the access token is valid
 */
@Serializable
data class AuthResponse(
    val user: UserDTO,
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresInSeconds: Int = 3600
)

/**
 * User data transfer object for API responses.
 * 
 * This DTO contains only the necessary user information:
 * - id: Unique user identifier
 * - email: User's email (null for guests)
 * - name: Display name (null for guests and some Apple sign-ins)
 * - isGuest: Whether this is a guest session
 * 
 * GDPR Note: Only stores essential data, no extra personal information.
 * 
 * @property id Unique user identifier
 * @property email User's email address (null for guests)
 * @property name User's display name (null for guests)
 * @property isGuest Whether this is a guest session
 * @property authMethod The authentication method used (e.g., "GOOGLE", "APPLE", "EMAIL", "GUEST")
 */
@Serializable
data class UserDTO(
    val id: String,
    val email: String?,
    val name: String?,
    val isGuest: Boolean,
    val authMethod: String? = null
) {
    companion object {
        /**
         * Creates a UserDTO from a User domain model.
         * 
         * @param user The User domain model
         * @return A UserDTO for API responses
         */
        fun fromDomain(user: com.guyghost.wakeve.auth.core.models.User): UserDTO {
            return UserDTO(
                id = user.id,
                email = user.email,
                name = user.name,
                isGuest = user.isGuest,
                authMethod = user.authMethod.name
            )
        }
    }
}

/**
 * Token refresh request.
 * 
 * @property refreshToken The refresh token to use
 */
@Serializable
data class TokenRefreshRequest(
    val refreshToken: String
)

/**
 * Response for token refresh.
 * 
 * @property accessToken New access token
 * @property refreshToken New refresh token (may be same as old one)
 * @property expiresInSeconds How long the new access token is valid
 */
@Serializable
data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresInSeconds: Int = 3600
)
