package com.guyghost.wakeve.auth.core.logic

import com.guyghost.wakeve.auth.core.models.AuthError
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for email validation logic.
 * These tests verify the pure function validateEmail() without any mocks.
 */
class ValidateEmailTest {

    @Test
    fun `validateEmail returns success for valid standard email`() {
        // Given
        val email = "user@example.com"
        
        // When
        val result = validateEmail(email)
        
        // Then
        assertTrue(result.isSuccess, "Valid email should return success")
    }

    @Test
    fun `validateEmail returns success for valid email with subdomain`() {
        // Given
        val email = "user@mail.example.com"
        
        // When
        val result = validateEmail(email)
        
        // Then
        assertTrue(result.isSuccess, "Email with subdomain should be valid")
    }

    @Test
    fun `validateEmail returns success for valid email with plus sign`() {
        // Given
        val email = "user+tag@example.com"
        
        // When
        val result = validateEmail(email)
        
        // Then
        assertTrue(result.isSuccess, "Email with plus sign should be valid")
    }

    @Test
    fun `validateEmail returns failure for email without at symbol`() {
        // Given
        val email = "userexample.com"
        
        // When
        val result = validateEmail(email)
        
        // Then
        assertTrue(result.isFailure, "Email without @ should fail")
        assertTrue(result.errorOrNull?.field == "email")
    }

    @Test
    fun validateEmailReturnsFailureForEmailStartingWithAtSymbol() {
        // Given
        val email = "@example.com"
        
        // When
        val result = validateEmail(email)
        
        // Then
        assertTrue(result.isFailure, "Email starting with @ should fail")
    }

    @Test
    fun `validateEmail returns failure for email with empty domain`() {
        // Given
        val email = "user@"
        
        // When
        val result = validateEmail(email)
        
        // Then
        assertTrue(result.isFailure, "Email with empty domain should fail")
    }

    @Test
    fun `validateEmail returns failure for email with consecutive dots`() {
        // Given
        val email = "user@..example.com"
        
        // When
        val result = validateEmail(email)
        
        // Then
        assertTrue(result.isFailure, "Email with consecutive dots should fail")
    }

    @Test
    fun `validateEmail returns failure for empty string`() {
        // Given
        val email = ""
        
        // When
        val result = validateEmail(email)
        
        // Then
        assertTrue(result.isFailure, "Empty email should fail")
        assertTrue(result.errorOrNull is AuthError.ValidationError)
    }

    @Test
    fun `validateEmail returns failure for whitespace only`() {
        // Given
        val email = "   "
        
        // When
        val result = validateEmail(email)
        
        // Then
        assertTrue(result.isFailure, "Whitespace-only email should fail")
    }

    @Test
    fun `validateEmail returns failure for too short email`() {
        // Given
        val email = "a@b"
        
        // When
        val result = validateEmail(email)
        
        // Then
        assertTrue(result.isFailure, "Too short email should fail")
    }

    @Test
    fun `validateEmail returns failure for disposable email domain`() {
        // Given
        val email = "user@tempmail.com"
        
        // When
        val result = validateEmail(email)
        
        // Then
        assertTrue(result.isFailure, "Disposable email domain should fail")
    }

    @Test
    fun `validateEmail trims whitespace around email`() {
        // Given
        val email = "  user@example.com  "
        
        // When
        val result = validateEmail(email)
        
        // Then
        assertTrue(result.isSuccess, "Trimmed email should be valid")
    }

    @Test
    fun `validateEmail returns failure for invalid characters`() {
        // Given
        val email = "user@example.com<>"
        
        // When
        val result = validateEmail(email)
        
        // Then
        assertTrue(result.isFailure, "Email with invalid characters should fail")
    }
}
