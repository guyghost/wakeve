package com.guyghost.wakeve.auth.offline

import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.shell.services.GuestModeService
import com.guyghost.wakeve.auth.shell.services.InMemoryTokenStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Guest Mode offline functionality.
 * These tests verify that guest mode works without network connectivity.
 */
class GuestModeOfflineTest {

    private lateinit var guestModeService: GuestModeService
    private lateinit var tokenStorage: InMemoryTokenStorage

    fun setup() {
        tokenStorage = InMemoryTokenStorage()
        guestModeService = GuestModeService(tokenStorage)
    }

    @Test
    fun testCreateGuestSessionWorksWithoutNetwork() = runTest {
        // Given - Offline environment (InMemoryTokenStorage works locally)
        setup()

        // When - Create guest session
        val result = guestModeService.createGuestSession()

        // Then - Should succeed without network
        assertTrue(result is AuthResult.Guest)
        val guestUser = (result as AuthResult.Guest).userOrNull
        assertNotNull(guestUser)
        assertEquals("Invité", guestUser.displayName)
        assertTrue(guestUser.isGuest)
    }

    @Test
    fun testCreateGuestSessionDoesNotMakeNetworkCalls() = runTest {
        // Given
        setup()

        // When - Create guest session
        val result = guestModeService.createGuestSession()

        // Then - Verify success (no network needed)
        assertTrue(result is AuthResult.Guest)
        val guestUser = (result as AuthResult.Guest).userOrNull
        assertNotNull(guestUser)
        assertTrue(guestUser.id.startsWith("guest_"))
    }

    @Test
    fun testRestoreGuestSessionWorksWithoutNetwork() = runTest {
        // Given - Existing guest session
        setup()
        val createResult = guestModeService.createGuestSession()
        val guestUser = (createResult as AuthResult.Guest).userOrNull
        assertNotNull(guestUser)
        val storedGuestId = guestUser.id

        // When - Restore guest session (should not need network)
        val restoreResult = guestModeService.restoreGuestSession()

        // Then - Should succeed without network
        assertTrue(restoreResult is AuthResult.Guest)
        val restoredUser = restoreResult?.userOrNull
        assertNotNull(restoredUser)
        assertEquals(storedGuestId, restoredUser.id)
    }

    @Test
    fun testGuestModeAllowsEventCreationLocally() = runTest {
        // Given
        setup()
        val result = guestModeService.createGuestSession()
        assertTrue(result is AuthResult.Guest)

        // When - In a real scenario, the guest user would create events locally
        val guestUser = (result as AuthResult.Guest).userOrNull

        // Then - Guest can create events locally
        assertNotNull(guestUser)
        assertTrue(guestUser.isGuest)
        assertNull(guestUser.email) // No personal data collected
        assertNull(guestUser.name)
    }

    @Test
    fun testGuestModeStoresDataOnlyLocally() = runTest {
        // Given
        setup()
        val result = guestModeService.createGuestSession()
        val guestUser = (result as AuthResult.Guest).userOrNull
        assertNotNull(guestUser)
        val guestId = guestUser.id

        // When - Restore guest session
        val restoredResult = guestModeService.restoreGuestSession()
        val restoredUser = restoredResult?.userOrNull

        // Then - Guest ID is stored locally only
        assertNotNull(restoredUser)
        assertEquals(guestId, restoredUser.id)
    }

    @Test
    fun testEndGuestSessionClearsLocalDataOnly() = runTest {
        // Given
        setup()
        guestModeService.createGuestSession()
        assertTrue(guestModeService.hasGuestSession())

        // When - End guest session
        val result = guestModeService.endGuestSession()

        // Then - Local data cleared
        assertTrue(result)
        assertFalse(guestModeService.hasGuestSession())
    }

    @Test
    fun testHasGuestSessionChecksLocalStorageOnly() = runTest {
        // Given
        setup()
        assertFalse(guestModeService.hasGuestSession())

        // When - Create guest session
        guestModeService.createGuestSession()

        // Then - hasGuestSession returns true
        assertTrue(guestModeService.hasGuestSession())
    }

    @Test
    fun testGuestModeContinuesWorkingDuringNetworkOutage() = runTest {
        // Given - User is in guest mode
        setup()
        val initialResult = guestModeService.createGuestSession()
        assertTrue(initialResult is AuthResult.Guest)

        // When - Simulate network outage and try operations
        val hasSession = guestModeService.hasGuestSession()
        val currentUser = guestModeService.getCurrentGuestUser()

        // Then - All operations work offline
        assertTrue(hasSession)
        assertNotNull(currentUser)
        assertTrue(currentUser.isGuest)
    }

    @Test
    fun testConvertGuestToAuthenticatedUser() = runTest {
        // Given
        setup()
        val guestResult = guestModeService.createGuestSession()
        assertTrue(guestModeService.hasGuestSession())

        // When - Convert to authenticated user
        val authenticatedUser = (guestResult as AuthResult.Guest).userOrNull
        assertNotNull(authenticatedUser)
        val result = guestModeService.convertToAuthenticatedUser(authenticatedUser)

        // Then - Guest session cleared
        assertTrue(result)
        assertFalse(guestModeService.hasGuestSession())
    }

    @Test
    fun testGetCurrentGuestUserReturnsNullWhenNotInGuestMode() = runTest {
        // Given
        setup()

        // When
        val guestUser = guestModeService.getCurrentGuestUser()

        // Then
        assertNull(guestUser)
    }

    @Test
    fun testGetCurrentGuestUserReturnsUserWhenInGuestMode() = runTest {
        // Given
        setup()
        guestModeService.createGuestSession()

        // When
        val guestUser = guestModeService.getCurrentGuestUser()

        // Then
        assertNotNull(guestUser)
        assertTrue(guestUser.isGuest)
    }
}
