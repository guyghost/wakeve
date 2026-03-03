package com.guyghost.wakeve.preview.factories

import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.User

/**
 * Factory for creating preview/test User instances.
 *
 * All properties use `get()` to return fresh instances on each access,
 * avoiding shared mutable state across previews.
 */
object UserFactory {

    private val previewTime = 1_700_000_000_000L // ~2023-11-14

    /** Authenticated organizer (Google sign-in). */
    val organizer
        get() = User.createAuthenticated(
            id = "user-organizer-001",
            email = "marie@example.com",
            name = "Marie Dupont",
            authMethod = AuthMethod.GOOGLE,
            currentTime = previewTime
        )

    /** Authenticated participant (Apple sign-in). */
    val participant
        get() = User.createAuthenticated(
            id = "user-participant-002",
            email = "thomas@example.com",
            name = "Thomas Martin",
            authMethod = AuthMethod.APPLE,
            currentTime = previewTime
        )

    /** Authenticated participant signed in via email. */
    val emailUser
        get() = User.createAuthenticated(
            id = "user-email-003",
            email = "claire@example.com",
            name = "Claire Bernard",
            authMethod = AuthMethod.EMAIL,
            currentTime = previewTime
        )

    /** Guest user (no email, no name). */
    val guest
        get() = User.createGuest(
            id = "user-guest-004",
            currentTime = previewTime
        )

    /**
     * Returns a list of [count] distinct users.
     * Cycles through organizer, participant, emailUser, and guest
     * for variety, generating unique IDs for extras beyond four.
     */
    fun group(count: Int): List<User> {
        val base = listOf(organizer, participant, emailUser, guest)
        if (count <= base.size) return base.take(count)

        val extras = (base.size until count).map { index ->
            User.createAuthenticated(
                id = "user-group-${index.toString().padStart(3, '0')}",
                email = "user$index@example.com",
                name = "User $index",
                authMethod = AuthMethod.EMAIL,
                currentTime = previewTime
            )
        }
        return base + extras
    }
}
