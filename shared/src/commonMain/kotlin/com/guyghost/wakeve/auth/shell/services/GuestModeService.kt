package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.User
import com.guyghost.wakeve.auth.core.models.User.Companion.createGuest
import com.guyghost.wakeve.auth.core.logic.currentTimeMillis
import kotlin.random.Random

/**
 * Service for managing guest mode operations.
 * 
 * Guest mode allows users to try the app without creating an account.
 * All data is stored locally and never synced to the cloud.
 * 
 * Key features:
 * - Create guest sessions
 * - Persist guest sessions across app restarts
 * - Full offline support (100% local)
 * - Upgrade path: Convert guest session to full account
 */
class GuestModeService(
    private val tokenStorage: TokenStorage
) {
    companion object {
        private const val GUEST_USER_KEY = "guest_user_id"
    }

    /**
     * Creates a new guest session.
     *
     * @return AuthResult with a new guest user
     */
    suspend fun createGuestSession(): AuthResult {
        val guestId = generateGuestId()
        val currentTime = currentTimeMillis()
        val guestUser = createGuest(guestId, currentTime)

        // Store guest ID for persistence
        tokenStorage.storeString(GUEST_USER_KEY, guestId)

        return AuthResult.guest(guestUser)
    }

    /**
     * Restores an existing guest session if one exists.
     *
     * @return AuthResult with the restored guest user, or null if no session exists
     */
    suspend fun restoreGuestSession(): AuthResult? {
        val guestId = tokenStorage.getString(GUEST_USER_KEY)

        return if (guestId != null) {
            val currentTime = currentTimeMillis()
            val guestUser = createGuest(guestId, currentTime)
            AuthResult.guest(guestUser)
        } else {
            null
        }
    }

    /**
     * Checks if there is an existing guest session.
     * 
     * @return true if a guest session exists
     */
    suspend fun hasGuestSession(): Boolean {
        return tokenStorage.getString(GUEST_USER_KEY) != null
    }

    /**
     * Ends the current guest session and clears all local data.
     * 
     * @return true if session was cleared successfully
     */
    suspend fun endGuestSession(): Boolean {
        return try {
            tokenStorage.remove(GUEST_USER_KEY)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets the current guest user if in guest mode.
     *
     * @return User if in guest mode, null otherwise
     */
    suspend fun getCurrentGuestUser(): User? {
        val guestId = tokenStorage.getString(GUEST_USER_KEY)
        return if (guestId != null) {
            val currentTime = currentTimeMillis()
            createGuest(guestId, currentTime)
        } else {
            null
        }
    }

    /**
     * Generates a unique guest ID.
     */
    private fun generateGuestId(): String {
        val timestamp = currentTimeMillis()
        val random = Random.nextInt(1000000)
        return "guest_${timestamp}_$random"
    }

    /**
     * Converts a guest session to a full authenticated user.
     * This is called when a guest user decides to sign up.
     * 
     * @param authenticatedUser The new authenticated user
     * @return true if conversion was successful
     */
    suspend fun convertToAuthenticatedUser(authenticatedUser: User): Boolean {
        return try {
            // Clear guest session
            tokenStorage.remove(GUEST_USER_KEY)
            true
        } catch (e: Exception) {
            false
        }
    }
}
