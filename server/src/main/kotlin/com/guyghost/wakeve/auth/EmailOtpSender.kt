package com.guyghost.wakeve.auth

/**
 * Sends email OTP codes through a real delivery provider.
 */
interface EmailOtpSender {
    suspend fun sendOtp(
        email: String,
        otp: String,
        expiresInSeconds: Long
    ): Result<Unit>
}

/**
 * Default sender used until SMTP or a transactional email provider is configured.
 */
object NoConfiguredEmailOtpSender : EmailOtpSender {
    override suspend fun sendOtp(
        email: String,
        otp: String,
        expiresInSeconds: Long
    ): Result<Unit> = Result.failure(
        IllegalStateException("Email OTP sender is not configured")
    )
}
