package com.guyghost.wakeve.auth.core.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for OTP validation and generation logic.
 * These tests verify the pure functions without any mocks.
 */
class ValidateOTPTest {

    @Test
    fun `validateOTP returns success for valid OTP`() {
        // Given
        val otp = "123456"
        val expectedOTP = "123456"
        val expiry = System.currentTimeMillis() + 300000 // 5 minutes from now
        
        // When
        val result = validateOTP(otp, expectedOTP, expiry, 3, 1)
        
        // Then
        assertTrue(result.isSuccess, "Valid OTP should return Success")
    }

    @Test
    fun `validateOTP returns Invalid for wrong OTP`() {
        // Given
        val otp = "123456"
        val expectedOTP = "654321"
        val expiry = System.currentTimeMillis() + 300000
        
        // When
        val result = validateOTP(otp, expectedOTP, expiry, 3, 1)
        
        // Then
        assertTrue(result is OTPAttemptResult.Invalid)
        assertEquals(2, (result as OTPAttemptResult.Invalid).attemptsRemaining)
    }

    @Test
    fun `validateOTP returns Expired for expired OTP`() {
        // Given
        val otp = "123456"
        val expectedOTP = "123456"
        val expiry = System.currentTimeMillis() - 1000 // Already expired
        
        // When
        val result = validateOTP(otp, expectedOTP, expiry, 3, 1)
        
        // Then
        assertTrue(result is OTPAttemptResult.Expired)
    }

    @Test
    fun `validateOTP returns Invalid for wrong length OTP`() {
        // Given
        val otp = "12345" // Only 5 digits
        val expectedOTP = "123456"
        val expiry = System.currentTimeMillis() + 300000
        
        // When
        val result = validateOTP(otp, expectedOTP, expiry, 3, 1)
        
        // Then
        assertTrue(result is OTPAttemptResult.Invalid)
    }

    @Test
    fun `validateOTP returns Invalid for OTP with letters`() {
        // Given
        val otp = "12a456"
        val expectedOTP = "123456"
        val expiry = System.currentTimeMillis() + 300000
        
        // When
        val result = validateOTP(otp, expectedOTP, expiry, 3, 1)
        
        // Then
        assertTrue(result is OTPAttemptResult.Invalid)
    }

    @Test
    fun `validateOTP returns Expired after max attempts`() {
        // Given
        val otp = "123456"
        val expectedOTP = "654321"
        val expiry = System.currentTimeMillis() + 300000
        
        // When - Try 3 times with wrong OTP
        val result1 = validateOTP(otp, expectedOTP, expiry, 3, 1)
        val result2 = validateOTP(otp, expectedOTP, expiry, 3, 2)
        val result3 = validateOTP(otp, expectedOTP, expiry, 3, 3)
        
        // Then
        assertTrue(result3 is OTPAttemptResult.Expired, "After max attempts, should return Expired")
    }

    @Test
    fun `validateOTP trims whitespace from OTP`() {
        // Given
        val otp = "  123456  "
        val expectedOTP = "123456"
        val expiry = System.currentTimeMillis() + 300000
        
        // When
        val result = validateOTP(otp, expectedOTP, expiry, 3, 1)
        
        // Then
        assertTrue(result.isSuccess, "OTP with whitespace should be valid after trimming")
    }

    @Test
    fun `generateOTP returns 6 digit string`() {
        // Given/When
        val otp = generateOTP()
        
        // Then
        assertEquals(6, otp.length, "OTP should be 6 digits")
        assertTrue(otp.all { it.isDigit() }, "OTP should only contain digits")
    }

    @Test
    fun `generateOTP generates unique codes`() {
        // Given
        val generatedOTPs = mutableSetOf<String>()
        
        // When - Generate 100 OTPs
        repeat(100) {
            generatedOTPs.add(generateOTP())
        }
        
        // Then - Should have 100 unique OTPs (allowing for rare collisions)
        assertTrue(generatedOTPs.size >= 95, "Most OTPs should be unique")
    }

    @Test
    fun `calculateOTPExpiry returns future timestamp`() {
        // Given
        val validityMinutes = 5
        
        // When
        val expiry = calculateOTPExpiry(validityMinutes)
        val now = System.currentTimeMillis()
        
        // Then
        assertTrue(expiry > now, "Expiry should be in the future")
        assertTrue(expiry - now <= validityMinutes * 60 * 1000 + 1000, "Expiry should be approximately 5 minutes")
    }

    @Test
    fun `OTPAttemptResult Success isSuccess returns true`() {
        // Given
        val result: OTPAttemptResult = OTPAttemptResult.Success
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(!result.isFailure)
    }

    @Test
    fun `OTPAttemptResult Invalid isFailure returns true`() {
        // Given
        val result: OTPAttemptResult = OTPAttemptResult.Invalid(attemptsRemaining = 2)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(!result.isSuccess)
    }

    @Test
    fun `OTPAttemptResult Expired isFailure returns true`() {
        // Given
        val result: OTPAttemptResult = OTPAttemptResult.Expired
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(!result.isSuccess)
    }
}
