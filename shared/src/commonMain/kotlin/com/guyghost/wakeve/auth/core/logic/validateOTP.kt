package com.guyghost.wakeve.auth.core.logic

import com.guyghost.wakeve.auth.core.models.AuthError
import kotlin.random.Random

/**
 * Validates a One-Time Password (OTP) code.
 *
 * This function validates OTP codes with the following rules:
 * - Must be exactly 6 digits
 * - Must not be expired (default 5 minute expiry)
 * - Must not exceed maximum attempts (default 3 attempts)
 *
 * @param otp The OTP code to validate (string of digits)
 * @param expectedOTP The expected OTP code for comparison
 * @param otpExpiryTimestamp The timestamp when the OTP expires
 * @param currentTime The current time (injected for testability)
 * @param maxAttempts Maximum number of failed attempts allowed
 * @param currentAttempt Current attempt number (1-based)
 * @return OTPAttemptResult indicating success, failure, or expiry
 *
 * @example
 * ```
 * validateOTP("123456", "123456", expiry, now, 3, 1) // Success
 * validateOTP("123456", "654321", expiry, now, 3, 1) // InvalidOTP(2 attempts remaining)
 * validateOTP("123456", "123456", expiredTimestamp, now, 3, 1) // OTPExpired
 * ```
 */
fun validateOTP(
    otp: String,
    expectedOTP: String,
    otpExpiryTimestamp: Long,
    currentTime: Long,
    maxAttempts: Int = 3,
    currentAttempt: Int = 1
): OTPAttemptResult {
    // Check if OTP has expired
    if (currentTime >= otpExpiryTimestamp) {
        return OTPAttemptResult.Expired
    }

    // Validate OTP format (6 digits)
    val trimmedOTP = otp.trim()
    if (trimmedOTP.length != 6) {
        return OTPAttemptResult.Invalid(
            attemptsRemaining = maxAttempts - currentAttempt + 1
        )
    }

    // Check if all characters are digits
    if (!trimmedOTP.all { it.isDigit() }) {
        return OTPAttemptResult.Invalid(
            attemptsRemaining = maxAttempts - currentAttempt + 1
        )
    }

    // Check if OTP matches
    if (trimmedOTP == expectedOTP) {
        return OTPAttemptResult.Success
    }

    // Calculate remaining attempts
    val remaining = maxAttempts - currentAttempt
    return if (remaining > 0) {
        OTPAttemptResult.Invalid(attemptsRemaining = remaining)
    } else {
        OTPAttemptResult.Expired // Treat max attempts as expired
    }
}

/**
 * Generates a random 6-digit OTP code.
 *
 * @param random Random number generator (injected for testability)
 * @return A string containing 6 random digits
 *
 * @example
 * ```
 * generateOTP(Random.Default) // "123456"
 * generateOTP(Random.Default) // "847290"
 * ```
 */
fun generateOTP(random: Random): String {
    val digits = StringBuilder(6)
    repeat(6) {
        digits.append(random.nextInt(10))
    }
    return digits.toString()
}

/**
 * Calculates the OTP expiry timestamp.
 *
 * @param validityMinutes Number of minutes the OTP is valid (default 5)
 * @param currentTime The current time (injected for testability)
 * @return Timestamp when the OTP expires
 *
 * @example
 * ```
 * calculateOTPExpiry(5, 1000) // returns timestamp 5 minutes from 1000
 * calculateOTPExpiry(10, 1000) // returns timestamp 10 minutes from 1000
 * ```
 */
fun calculateOTPExpiry(validityMinutes: Int = 5, currentTime: Long): Long {
    return currentTime + (validityMinutes * 60 * 1000L)
}

/**
 * Result type for OTP validation.
 */
sealed class OTPAttemptResult {
    /** OTP validation succeeded */
    data object Success : OTPAttemptResult()
    
    /** OTP validation failed due to invalid code
     * @property attemptsRemaining Number of attempts remaining before lockout
     */
    data class Invalid(
        val attemptsRemaining: Int
    ) : OTPAttemptResult()
    
    /** OTP has expired (either due to time or max attempts) */
    data object Expired : OTPAttemptResult()

    /**
     * Returns true if validation succeeded.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if validation failed (invalid or expired).
     */
    val isFailure: Boolean
        get() = this is Invalid || this is Expired

    /**
     * Returns the AuthError corresponding to this result.
     */
    val toAuthError: AuthError
        get() = when (this) {
            is Success -> throw IllegalStateException("Cannot convert Success to AuthError")
            is Invalid -> AuthError.InvalidOTP(attemptsRemaining)
            is Expired -> AuthError.OTPExpired
        }
}
