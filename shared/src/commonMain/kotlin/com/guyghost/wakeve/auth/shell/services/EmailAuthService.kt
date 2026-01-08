package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.User

/**
 * Service for handling email-based authentication with OTP.
 * 
 * This service manages the complete email OTP flow:
 * - Generate and send OTP codes
 * - Verify OTP codes
 * - Create/validate user sessions
 * 
 * @property otpExpiryMinutes How long OTP codes are valid (default 5)
 * @property maxAttempts Maximum OTP verification attempts (default 3)
 */
class EmailAuthService(
    private val otpExpiryMinutes: Int = 5,
    private val maxAttempts: Int = 3
) {
    /**
     * Internal OTP storage (in production, this would be in the backend).
     * Maps email to OTP data.
     */
    private val otpStore = mutableMapOf<String, OTPData>()

    /**
     * Requests an OTP code for the given email.
     * 
     * @param email The email address to send OTP to
     * @return Result indicating success or failure
     */
    suspend fun requestOTP(email: String): Result<Unit> {
        // Generate new OTP
        val otp = generateOTP()
        val expiry = calculateOTPExpiry(otpExpiryMinutes)

        // Store OTP data
        otpStore[email.lowercase()] = OTPData(
            otp = otp,
            expiryTimestamp = expiry,
            attempts = 0
        )

        // In production, send email via backend API
        // For now, log it (will be replaced with actual email sending)
        println("OTP for $email: $otp")

        return Result.success(Unit)
    }

    /**
     * Verifies the OTP code for the given email.
     * 
     * @param email The email address
     * @param otp The OTP code entered by user
     * @param userCreator Function to create user after successful verification
     * @return AuthResult with authenticated user or error
     */
    suspend fun verifyOTP(
        email: String,
        otp: String,
        userCreator: (String) -> User
    ): AuthResult {
        val normalizedEmail = email.lowercase()
        val otpData = otpStore[normalizedEmail]

        if (otpData == null) {
            return AuthResult.error(AuthError.OTPExpired)
        }

        val currentTime = System.currentTimeMillis()
        
        // Check if OTP has expired
        if (currentTime >= otpData.expiryTimestamp) {
            otpStore.remove(normalizedEmail)
            return AuthResult.error(AuthError.OTPExpired)
        }

        // Check max attempts
        if (otpData.attempts >= maxAttempts) {
            otpStore.remove(normalizedEmail)
            return AuthResult.error(AuthError.InvalidOTP(0))
        }

        // Increment attempts
        val updatedOtpData = otpData.copy(attempts = otpData.attempts + 1)
        otpStore[normalizedEmail] = updatedOtpData

        // Verify OTP
        return if (otpData.otp == otp.trim()) {
            // Success - create user and clear OTP
            otpStore.remove(normalizedEmail)
            val user = userCreator(email)
            // Generate session token (in production, from backend)
            val sessionToken = generateSessionToken()
            AuthResult.success(user, sessionToken)
        } else {
            val remaining = maxAttempts - updatedOtpData.attempts
            if (remaining <= 0) {
                otpStore.remove(normalizedEmail)
                AuthResult.error(AuthError.InvalidOTP(0))
            } else {
                AuthResult.error(AuthError.InvalidOTP(remaining))
            }
        }
    }

    /**
     * Resends OTP for the given email (clears old OTP and sends new one).
     * 
     * @param email The email address
     * @return Result indicating success or failure
     */
    suspend fun resendOTP(email: String): Result<Unit> {
        // Clear old OTP if exists
        otpStore.remove(email.lowercase())
        // Request new OTP
        return requestOTP(email)
    }

    /**
     * Cancels OTP request for the given email.
     * 
     * @param email The email address
     */
    fun cancelOTP(email: String) {
        otpStore.remove(email.lowercase())
    }

    /**
     * Checks if an OTP request is pending for the given email.
     * 
     * @param email The email address
     * @return true if there's a pending OTP request
     */
    fun hasPendingOTP(email: String): Boolean {
        val otpData = otpStore[email.lowercase()] ?: return false
        return System.currentTimeMillis() < otpData.expiryTimestamp
    }

    /**
     * Gets the remaining time for OTP validity.
     * 
     * @param email The email address
     * @return Remaining time in seconds, or 0 if no pending OTP
     */
    fun getRemainingOTPTime(email: String): Long {
        val otpData = otpStore[email.lowercase()] ?: return 0
        val remaining = otpData.expiryTimestamp - System.currentTimeMillis()
        return (remaining / 1000).coerceAtLeast(0)
    }

    /**
     * Clears all stored OTPs (for testing or cleanup).
     */
    fun clearAllOTPs() {
        otpStore.clear()
    }

    /**
     * Data class for storing OTP-related information.
     */
    private data class OTPData(
        val otp: String,
        val expiryTimestamp: Long,
        val attempts: Int
    )

    // Import pure functions from core logic
    private fun generateOTP(): String {
        val random = java.security.SecureRandom()
        val digits = StringBuilder(6)
        repeat(6) {
            digits.append(random.nextInt(10))
        }
        return digits.toString()
    }

    private fun calculateOTPExpiry(validityMinutes: Int): Long {
        return System.currentTimeMillis() + (validityMinutes * 60 * 1000L)
    }

    private fun generateSessionToken(): com.guyghost.wakeve.auth.core.models.AuthToken {
        val tokenValue = "session_${System.currentTimeMillis()}_${java.security.SecureRandom().nextInt(1000000)}"
        return com.guyghost.wakeve.auth.core.models.AuthToken.createLongLived(
            value = tokenValue,
            expiresInDays = 30
        )
    }
}
