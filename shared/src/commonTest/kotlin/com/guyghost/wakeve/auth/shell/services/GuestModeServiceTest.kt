package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for GuestModeService.
 *
 * Tests guest session creation, restoration, lifecycle,
 * and conversion to authenticated user.
 */
class GuestModeServiceTest {

    private fun createService(storage: InMemoryTokenStorage = InMemoryTokenStorage()): GuestModeService {
        return GuestModeService(tokenStorage = storage)
    }

    // ========================================================================
    // Create Guest Session Tests
    // ========================================================================

    @Test
    fun `createGuestSession returns guest result`() {
        val service = createService()
        val result = runTest { service.createGuestSession() }

        assertNotNull(result)
        assertTrue(result.isGuest, "Result should be a guest result")
    }

    @Test
    fun `createGuestSession returns user with guest id`() {
        val service = createService()
        val result = runTest { service.createGuestSession() }

        val user = result.userOrNull
        assertNotNull(user)
        assertTrue(user.id.startsWith("guest_"), "Guest user ID should start with 'guest_'")
    }

    @Test
    fun `createGuestSession stores guest ID in token storage`() {
        val storage = InMemoryTokenStorage()
        val service = createService(storage)
        val result = runTest { service.createGuestSession() }

        val storedId = runTest { storage.getString("guest_user_id") }
        assertNotNull(storedId, "Guest ID should be stored in token storage")
        assertEquals(result.userOrNull?.id, storedId)
    }

    @Test
    fun `createGuestSession generates unique IDs`() {
        val service = createService()
        val result1 = runTest { service.createGuestSession() }
        val result2 = runTest { service.createGuestSession() }

        // IDs should be different (timestamp + random)
        // Note: in fast execution, timestamp might be same but random differs
        assertTrue(result1.userOrNull?.id != result2.userOrNull?.id,
            "Two guest sessions should have different IDs")
    }

    // ========================================================================
    // Restore Guest Session Tests
    // ========================================================================

    @Test
    fun `restoreGuestSession returns null when no session exists`() {
        val service = createService()
        val result = runTest { service.restoreGuestSession() }

        assertNull(result, "Should return null when no guest session exists")
    }

    @Test
    fun `restoreGuestSession returns session after creation`() {
        val storage = InMemoryTokenStorage()
        val service = createService(storage)

        val created = runTest { service.createGuestSession() }
        val restored = runTest { service.restoreGuestSession() }

        assertNotNull(restored)
        assertEquals(created.userOrNull?.id, restored.userOrNull?.id,
            "Restored session should have same user ID")
    }

    @Test
    fun `restoreGuestSession returns guest result`() {
        val storage = InMemoryTokenStorage()
        val service = createService(storage)

        runTest { service.createGuestSession() }
        val restored = runTest { service.restoreGuestSession() }

        assertNotNull(restored)
        assertTrue(restored.isGuest)
    }

    // ========================================================================
    // Has Guest Session Tests
    // ========================================================================

    @Test
    fun `hasGuestSession returns false initially`() {
        val service = createService()
        assertFalse(runTest { service.hasGuestSession() })
    }

    @Test
    fun `hasGuestSession returns true after creating session`() {
        val service = createService()
        runTest { service.createGuestSession() }
        assertTrue(runTest { service.hasGuestSession() })
    }

    @Test
    fun `hasGuestSession returns false after ending session`() {
        val service = createService()
        runTest { service.createGuestSession() }
        runTest { service.endGuestSession() }
        assertFalse(runTest { service.hasGuestSession() })
    }

    // ========================================================================
    // End Guest Session Tests
    // ========================================================================

    @Test
    fun `endGuestSession returns true`() {
        val service = createService()
        runTest { service.createGuestSession() }
        val result = runTest { service.endGuestSession() }
        assertTrue(result)
    }

    @Test
    fun `endGuestSession clears stored guest ID`() {
        val storage = InMemoryTokenStorage()
        val service = createService(storage)

        runTest { service.createGuestSession() }
        assertNotNull(runTest { storage.getString("guest_user_id") })

        runTest { service.endGuestSession() }
        assertNull(runTest { storage.getString("guest_user_id") })
    }

    @Test
    fun `endGuestSession is idempotent`() {
        val service = createService()
        runTest { service.createGuestSession() }
        assertTrue(runTest { service.endGuestSession() })
        assertTrue(runTest { service.endGuestSession() })
    }

    // ========================================================================
    // Get Current Guest User Tests
    // ========================================================================

    @Test
    fun `getCurrentGuestUser returns null when no session`() {
        val service = createService()
        assertNull(runTest { service.getCurrentGuestUser() })
    }

    @Test
    fun `getCurrentGuestUser returns user after session creation`() {
        val service = createService()
        val created = runTest { service.createGuestSession() }
        val current = runTest { service.getCurrentGuestUser() }

        assertNotNull(current)
        assertEquals(created.userOrNull?.id, current.id)
    }

    @Test
    fun `getCurrentGuestUser returns null after session end`() {
        val service = createService()
        runTest { service.createGuestSession() }
        runTest { service.endGuestSession() }
        assertNull(runTest { service.getCurrentGuestUser() })
    }

    // ========================================================================
    // Convert to Authenticated User Tests
    // ========================================================================

    @Test
    fun `convertToAuthenticatedUser clears guest session`() {
        val storage = InMemoryTokenStorage()
        val service = createService(storage)

        runTest { service.createGuestSession() }
        assertTrue(runTest { service.hasGuestSession() })

        val authUser = User.createAuthenticated(
            id = "real-user-123",
            email = "user@example.com",
            name = "Real User",
            authMethod = AuthMethod.EMAIL,
            currentTime = 0L
        )

        assertTrue(runTest { service.convertToAuthenticatedUser(authUser) })
    }

    // ========================================================================
    // Full Lifecycle Test
    // ========================================================================

    @Test
    fun `full guest lifecycle - create, restore, convert`() {
        val service = createService()

        // 1. No session initially
        assertFalse(runTest { service.hasGuestSession() })

        // 2. Create guest session
        val guestResult = runTest { service.createGuestSession() }
        assertTrue(guestResult.isGuest)
        assertTrue(runTest { service.hasGuestSession() })

        // 3. Restore session
        val restored = runTest { service.restoreGuestSession() }
        assertNotNull(restored)
        assertEquals(guestResult.userOrNull?.id, restored.userOrNull?.id)

        // 4. Get current user
        val current = runTest { service.getCurrentGuestUser() }
        assertNotNull(current)
        assertEquals(guestResult.userOrNull?.id, current.id)

        // 5. Convert to authenticated
        val authUser = User.createAuthenticated(
            id = "real-user-456",
            email = "converted@example.com",
            name = "Converted User",
            authMethod = AuthMethod.GOOGLE,
            currentTime = 0L
        )
        assertTrue(runTest { service.convertToAuthenticatedUser(authUser) })

        // 6. Session is gone
        assertFalse(runTest { service.hasGuestSession() })
        assertNull(runTest { service.getCurrentGuestUser() })
        assertNull(runTest { service.restoreGuestSession() })
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private fun <T> runTest(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking { block() }
    }
}
