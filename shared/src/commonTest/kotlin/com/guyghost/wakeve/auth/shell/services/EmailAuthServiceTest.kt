package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.User
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EmailAuthServiceTest {
    @Test
    fun `requestOTP fails and does not store OTP when sender is not configured`() = runTest {
        val service = EmailAuthService(random = Random(1))

        val result = service.requestOTP("user@example.com")

        assertTrue(result.isFailure)
        assertEquals("Email OTP sender is not configured", result.exceptionOrNull()?.message)
        assertFalse(service.hasPendingOTP("user@example.com"))
    }

    @Test
    fun `requestOTP stores OTP only after sender succeeds`() = runTest {
        val sender = CapturingEmailOtpSender()
        val service = EmailAuthService(
            random = Random(1),
            otpSender = sender
        )

        val result = service.requestOTP("User@Example.com")

        assertTrue(result.isSuccess)
        assertEquals("User@Example.com", sender.lastEmail)
        assertNotNull(sender.lastOtp)
        assertTrue(service.hasPendingOTP("user@example.com"))
    }

    @Test
    fun `verifyOTP authenticates with delivered OTP`() = runTest {
        val sender = CapturingEmailOtpSender()
        val service = EmailAuthService(
            random = Random(2),
            otpSender = sender
        )
        service.requestOTP("user@example.com").getOrThrow()

        val authResult = service.verifyOTP(
            email = "user@example.com",
            otp = sender.lastOtp ?: error("Expected captured OTP"),
            userCreator = { email ->
                User.createAuthenticated(
                    id = "user-1",
                    email = email,
                    name = null,
                    authMethod = AuthMethod.EMAIL,
                    currentTime = 1_000L
                )
            }
        )

        assertTrue(authResult is AuthResult.Success)
        assertEquals("user@example.com", (authResult as AuthResult.Success).user.email)
        assertFalse(service.hasPendingOTP("user@example.com"))
    }

    private class CapturingEmailOtpSender : EmailOtpSender {
        var lastEmail: String? = null
            private set
        var lastOtp: String? = null
            private set

        override suspend fun sendOtp(
            email: String,
            otp: String,
            expiresAtMillis: Long
        ): Result<Unit> {
            lastEmail = email
            lastOtp = otp
            return Result.success(Unit)
        }
    }
}
