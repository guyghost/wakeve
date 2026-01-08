package com.guyghost.wakeve.auth.core.models

import com.guyghost.wakeve.auth.core.models.AuthMethod.APPLE
import com.guyghost.wakeve.auth.core.models.AuthMethod.EMAIL
import com.guyghost.wakeve.auth.core.models.AuthMethod.GOOGLE
import com.guyghost.wakeve.auth.core.models.AuthMethod.GUEST
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for User model.
 * These tests verify the pure data class and companion functions.
 */
class UserTest {

    @Test
    fun `createGuest creates guest user with null email and name`() {
        // Given
        val guestId = "guest_123"
        
        // When
        val guest = User.createGuest(guestId)
        
        // Then
        assertEquals(guestId, guest.id)
        assertNull(guest.email)
        assertNull(guest.name)
        assertEquals(GUEST, guest.authMethod)
        assertTrue(guest.isGuest)
        assertTrue(guest.canSync == false)
    }

    @Test
    fun `createAuthenticated creates authenticated user`() {
        // Given
        val id = "user_123"
        val email = "test@example.com"
        val name = "Test User"
        
        // When
        val user = User.createAuthenticated(id, email, name, GOOGLE)
        
        // Then
        assertEquals(id, user.id)
        assertEquals(email, user.email)
        assertEquals(name, user.name)
        assertEquals(GOOGLE, user.authMethod)
        assertFalse(user.isGuest)
        assertTrue(user.canSync)
    }

    @Test
    fun `displayName returns Invité for guest users`() {
        // Given
        val guest = User.createGuest("guest_123")
        
        // When
        val displayName = guest.displayName
        
        // Then
        assertEquals("Invité", displayName)
    }

    @Test
    fun `displayName returns name for authenticated users with name`() {
        // Given
        val user = User.createAuthenticated("user_123", "test@example.com", "John Doe", GOOGLE)
        
        // When
        val displayName = user.displayName
        
        // Then
        assertEquals("John Doe", displayName)
    }

    @Test
    fun `displayName returns email prefix for authenticated users without name`() {
        // Given
        val user = User.createAuthenticated("user_123", "john.doe@example.com", null, EMAIL)
        
        // When
        val displayName = user.displayName
        
        // Then
        assertEquals("john.doe", displayName)
    }

    @Test
    fun `hasEmail returns true for users with email`() {
        // Given
        val user = User.createAuthenticated("user_123", "test@example.com", "Test", GOOGLE)
        
        // When/Then
        assertTrue(user.hasEmail)
    }

    @Test
    fun `hasEmail returns false for guest users`() {
        // Given
        val guest = User.createGuest("guest_123")
        
        // When/Then
        assertFalse(guest.hasEmail)
    }

    @Test
    fun `canSync returns false for guest users`() {
        // Given
        val guest = User.createGuest("guest_123")
        
        // When/Then
        assertFalse(guest.canSync)
    }

    @Test
    fun `canSync returns true for authenticated users`() {
        // Given
        val user = User.createAuthenticated("user_123", "test@example.com", "Test", APPLE)
        
        // When/Then
        assertTrue(user.canSync)
    }

    @Test
    fun `User equality works correctly`() {
        // Given
        val user1 = User(
            id = "user_123",
            email = "test@example.com",
            name = "Test",
            authMethod = GOOGLE,
            isGuest = false,
            createdAt = 1000L,
            lastLoginAt = 1000L
        )
        val user2 = User(
            id = "user_123",
            email = "test@example.com",
            name = "Test",
            authMethod = GOOGLE,
            isGuest = false,
            createdAt = 1000L,
            lastLoginAt = 1000L
        )
        
        // Then
        assertEquals(user1, user2)
    }
}
