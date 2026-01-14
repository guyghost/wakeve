package com.guyghost.wakeve.auth.core.models

import com.guyghost.wakeve.auth.core.models.AuthMethod.GOOGLE
import com.guyghost.wakeve.auth.core.models.AuthMethod.APPLE
import com.guyghost.wakeve.auth.core.models.AuthMethod.EMAIL
import com.guyghost.wakeve.auth.core.models.AuthMethod.GUEST
import com.guyghost.wakeve.auth.core.logic.currentTimeMillis

/**
 * Represents an authenticated user or guest user in the Wakeve application.
 * 
 * This model contains minimal user data for GDPR compliance - only necessary
 * information is stored: id, email, name, authentication method, and guest status.
 * 
 * @property id Unique user identifier (UUID format)
 * @property email User's email address (null for guest mode)
 * @property name User's display name (null for some Apple sign-ins)
 * @property authMethod The method used for authentication
 * @property isGuest Whether this is a guest session (true) or authenticated user (false)
 * @property createdAt Timestamp when the user account was created
 * @property lastLoginAt Timestamp of the last successful login
 */
data class User(
    val id: String,
    val email: String?,
    val name: String?,
    val authMethod: AuthMethod,
    val isGuest: Boolean,
    val createdAt: Long,
    val lastLoginAt: Long
) {
    companion object {
        /**
         * Creates a new User instance for guest mode.
         * 
         * @param id Unique guest session identifier
         * @return A new User configured for guest mode
         */
        fun createGuest(id: String): User = User(
            id = id,
            email = null,
            name = null,
            authMethod = GUEST,
            isGuest = true,
            createdAt = currentTimeMillis(),
            lastLoginAt = currentTimeMillis()
        )

        /**
         * Creates a new User instance for authenticated user.
         * 
         * @param id Unique user identifier
         * @param email User's email address
         * @param name User's display name
         * @param authMethod Authentication method used
         * @return A new User configured for authenticated mode
         */
        fun createAuthenticated(
            id: String,
            email: String,
            name: String?,
            authMethod: AuthMethod
        ): User = User(
            id = id,
            email = email,
            name = name,
            authMethod = authMethod,
            isGuest = false,
            createdAt = currentTimeMillis(),
            lastLoginAt = currentTimeMillis()
        )
    }

    /**
     * Returns the display name for the user.
     * For guests, returns "Invité". For authenticated users, returns name or email.
     */
    val displayName: String
        get() = when {
            isGuest -> "Invité"
            name != null -> name
            email != null -> email.substringBefore("@")
            else -> "Utilisateur"
        }

    /**
     * Returns true if the user has an email address.
     */
    val hasEmail: Boolean
        get() = email != null

    /**
     * Returns true if the user can sync data to the cloud.
     */
    val canSync: Boolean
        get() = !isGuest
}
