package com.guyghost.wakeve.auth.core.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for JWT parsing logic.
 * These tests verify the pure function parseJWT() without any mocks.
 */
class ParseJWTTest {

    @Test
    fun `parseJWT parses valid JWT token`() {
        // Given - A valid JWT token with known payload
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        
        // When
        val payload = parseJWT(token)
        
        // Then
        assertNotNull(payload, "Valid JWT should return a payload")
        assertEquals("1234567890", payload.subject)
        assertEquals(1516239022L, payload.issuedAt)
    }

    @Test
    fun `parseJWT returns null for blank token`() {
        // Given
        val token = ""
        
        // When
        val payload = parseJWT(token)
        
        // Then
        assertNull(payload, "Blank token should return null")
    }

    @Test
    fun `parseJWT returns null for whitespace token`() {
        // Given
        val token = "   "
        
        // When
        val payload = parseJWT(token)
        
        // Then
        assertNull(payload, "Whitespace-only token should return null")
    }

    @Test
    fun `parseJWT returns null for token with wrong number of parts`() {
        // Given
        val token = "invalid.token"
        
        // When
        val payload = parseJWT(token)
        
        // Then
        assertNull(payload, "Token with wrong number of parts should return null")
    }

    @Test
    fun `parseJWT returns null for single part token`() {
        // Given
        val token = "nosegments"
        
        // When
        val payload = parseJWT(token)
        
        // Then
        assertNull(payload, "Single segment token should return null")
    }

    @Test
    fun `parseJWT returns null for two part token`() {
        // Given
        val token = "header.payload"
        
        // When
        val payload = parseJWT(token)
        
        // Then
        assertNull(payload, "Two segment token should return null")
    }

    @Test
    fun `parseJWT parses JWT with string claims`() {
        // Given - JWT with various claim types
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLWlkIiwiZW1haWwiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.test"
        
        // When
        val payload = parseJWT(token)
        
        // Then
        assertNotNull(payload)
        assertEquals("user-id", payload.subject)
        assertEquals("test@example.com", payload["email"])
    }

    @Test
    fun `parseJWT parses JWT with boolean claims`() {
        // Given - JWT with boolean claims
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhZG1pbiI6dHJ1ZSwiaXNDb25maXJtZWQiOmZhbHNlfQ.test"
        
        // When
        val payload = parseJWT(token)
        
        // Then
        assertNotNull(payload)
        assertEquals(true, payload["admin"])
        assertEquals(false, payload["isConfirmed"])
    }

    @Test
    fun `parseJWT parses JWT with numeric claims`() {
        // Given - JWT with numeric claims
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MTYyMzkwMjAsImlhdCI6MTUxNjIzOTAyMH0.test"
        
        // When
        val payload = parseJWT(token)
        
        // Then
        assertNotNull(payload)
        assertEquals(1716239020L, payload.expirationTime)
        assertEquals(1516239020L, payload.issuedAt)
    }

    @Test
    fun `JWTPayload isExpired returns true for expired token`() {
        // Given - JWT with past expiration
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE1MTYyMzkwMjB9.test"
        
        // When
        val payload = parseJWT(token)
        
        // Then
        assertNotNull(payload)
        assertTrue(payload.isExpired(), "Token with past exp should be expired")
    }

    @Test
    fun `JWTPayload isExpired returns false for valid token`() {
        // Given - JWT with future expiration
        val futureTime = (System.currentTimeMillis() / 1000) + 3600 // 1 hour from now
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOj$futureTime.test"
        
        // When
        val payload = parseJWT(token)
        
        // Then
        assertNotNull(payload)
        assertTrue(!payload.isExpired(), "Token with future exp should not be expired")
    }

    @Test
    fun `JWTPayload getAs returns typed claim`() {
        // Given
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNTE2MjM5MDIyfQ.test"
        
        // When
        val payload = parseJWT(token)
        
        // Then
        assertNotNull(payload)
        assertEquals("test", payload.getAs<String>("sub"))
    }
}
