package com.guyghost.wakeve.auth.core.validation

import com.guyghost.wakeve.auth.core.logic.validateEmail
import com.guyghost.wakeve.auth.core.logic.validateOTP
import com.guyghost.wakeve.auth.core.logic.currentTimeMillis
import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.AuthToken
import com.guyghost.wakeve.auth.core.models.User
import kotlin.random.Random

/**
 * Combined validation for the complete authentication flow.
 * This validator orchestrates multiple validation steps for different auth methods.
 */
object AuthValidators {

    /**
     * Validates the complete Google sign-in response.
     * 
     * @param idToken The Google ID token
     * @param userId The user ID from Google
     * @param email The user's email
     * @param name The user's display name
     * @return AuthResult.success() or AuthResult.error()
     */
    fun validateGoogleSignIn(
        idToken: String,
        userId: String,
        email: String,
        name: String?
    ): AuthResult {
        // Validate ID token format (basic check)
        if (idToken.isBlank()) {
            return AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.GOOGLE,
                    message = "Token Google invalide"
                )
            )
        }

        // Validate user ID format (should be a valid Google user ID)
        if (userId.isBlank()) {
            return AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.GOOGLE,
                    message = "ID utilisateur Google invalide"
                )
            )
        }

        // Validate email
        val emailValidation = validateEmail(email)
        if (emailValidation.isFailure) {
            return AuthResult.error(emailValidation.errorOrNull!!)
        }

        return AuthResult.success(
            User(
                id = userId,
                email = email,
                name = name,
                authMethod = AuthMethod.GOOGLE,
                isGuest = false,
                createdAt = currentTimeMillis(),
                lastLoginAt = currentTimeMillis()
            ),
            AuthToken.create(idToken, expiresInSeconds = 3600) // 1 hour for ID token
        )
    }

    /**
     * Validates the complete Apple sign-in response.
     * 
     * @param identityToken The Apple identity token
     * @param userId The user ID from Apple
     * @param email The user's email (may be null if user hides it)
     * @param name The user's name (may be null)
     * @return AuthResult.success() or AuthResult.error()
     */
    fun validateAppleSignIn(
        identityToken: String,
        userId: String,
        email: String?,
        name: String?
    ): AuthResult {
        // Validate identity token format
        if (identityToken.isBlank()) {
            return AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.APPLE,
                    message = "Token Apple invalide"
                )
            )
        }

        // Validate user ID format (Apple user IDs start with a specific prefix)
        if (userId.isBlank() || !userId.startsWith("001")) {
            return AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.APPLE,
                    message = "ID utilisateur Apple invalide"
                )
            )
        }

        // Validate email if provided (Apple may hide it on subsequent sign-ins)
        if (email != null) {
            val emailValidation = validateEmail(email)
            if (emailValidation.isFailure) {
                return AuthResult.error(emailValidation.errorOrNull!!)
            }
        }

        return AuthResult.success(
            User(
                id = userId,
                email = email,
                name = name,
                authMethod = AuthMethod.APPLE,
                isGuest = false,
                createdAt = currentTimeMillis(),
                lastLoginAt = currentTimeMillis()
            ),
            AuthToken.create(identityToken, expiresInSeconds = 3600)
        )
    }

    /**
     * Validates the email OTP verification flow.
     * 
     * @param email The email address
     * @param otp The OTP code entered by user
     * @param expectedOTP The expected OTP for comparison
     * @param otpExpiryTimestamp When the OTP expires
     * @param attemptNumber Current attempt number
     * @return AuthResult.success() or AuthResult.error()
     */
    fun validateEmailOTP(
        email: String,
        otp: String,
        expectedOTP: String,
        otpExpiryTimestamp: Long,
        attemptNumber: Int = 1
    ): AuthResult {
        // Validate email first
        val emailValidation = validateEmail(email)
        if (emailValidation.isFailure) {
            return AuthResult.error(emailValidation.errorOrNull!!)
        }

        // Validate OTP - use current time from parameter
        val otpResult = validateOTP(
            otp = otp,
            expectedOTP = expectedOTP,
            otpExpiryTimestamp = otpExpiryTimestamp,
            currentTime = currentTimeMillis(),
            maxAttempts =3,
            currentAttempt = attemptNumber
        )

        return when (otpResult) {
            is com.guyghost.wakeve.auth.core.logic.OTPAttemptResult.Success -> {
                // Generate a session token for email auth
                val sessionToken = generateSessionToken(email)
                val currentTime = currentTimeMillis()
                AuthResult.success(
                    User.createAuthenticated(
                        id = generateUserId(),
                        email = email,
                        name = null,
                        authMethod = AuthMethod.EMAIL,
                        currentTime = currentTime
                    ),
                    AuthToken.createLongLived(sessionToken, expiresInDays = 30)
                )
            }
            is com.guyghost.wakeve.auth.core.logic.OTPAttemptResult.Invalid -> {
                AuthResult.invalidOTP(otpResult.attemptsRemaining)
            }
            is com.guyghost.wakeve.auth.core.logic.OTPAttemptResult.Expired -> {
                AuthResult.error(AuthError.OTPExpired)
            }
        }
    }

    /**
     * Validates a token refresh request.
     * 
     * @param refreshToken The refresh token
     * @return AuthResult.success() or AuthResult.error()
     */
    fun validateTokenRefresh(refreshToken: String): AuthResult {
        if (refreshToken.isBlank()) {
            return AuthResult.error(AuthError.InvalidCredentials)
        }

        // Basic format validation (JWT format)
        val parts = refreshToken.split(".")
        if (parts.size != 3) {
            return AuthResult.error(AuthError.InvalidCredentials)
        }

        return AuthResult.success(
            User(
                id = "refresh",
                email = null,
                name = null,
                authMethod = AuthMethod.GOOGLE, // Placeholder
                isGuest = false,
                createdAt = 0,
                lastLoginAt = currentTimeMillis()
            ),
            AuthToken.create(refreshToken, expiresInSeconds = 3600)
        )
    }

    /**
     * Generates a simple session token for email auth.
     * In production, this would be generated by the backend.
     */
    private fun generateSessionToken(email: String): String {
        val timestamp = currentTimeMillis()
        val random = Random.nextInt(1000000)
        return "session_${email.hashCode()}_${timestamp}_$random"
    }

    /**
     * Generates a unique user ID.
     * In production, this would be the user ID from the backend.
     */
    private fun generateUserId(): String {
        return "user_${currentTimeMillis()}_${Random.nextInt(1000000)}"
    }
}
