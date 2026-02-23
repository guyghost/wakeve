package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Domain model representing an invitation link for an event.
 *
 * Invitation codes can be shared via deep links (wakeve://invite/{code})
 * or universal links (https://wakeve.app/invite/{code}).
 */
@Serializable
data class Invitation(
    val id: String,
    val code: String,
    val eventId: String,
    val createdBy: String,
    val expiresAt: String? = null,
    val maxUses: Int? = null,
    val currentUses: Int = 0,
    val createdAt: String
) {
    /**
     * Check if the invitation has expired.
     * Returns false if expiresAt is null (no expiry).
     */
    fun isExpired(currentTime: String): Boolean {
        return expiresAt?.let { it < currentTime } ?: false
    }

    /**
     * Check if the invitation has reached its maximum uses.
     * Returns false if maxUses is null (unlimited).
     */
    fun isMaxUsesReached(): Boolean {
        return maxUses?.let { currentUses >= it } ?: false
    }

    /**
     * Check if the invitation is valid (not expired and not max uses reached).
     */
    fun isValid(currentTime: String): Boolean {
        return !isExpired(currentTime) && !isMaxUsesReached()
    }
}

// MARK: - API Request/Response Models

/**
 * Request to create a new invitation link for an event.
 */
@Serializable
data class CreateInvitationRequest(
    val expiresAt: String? = null,
    val maxUses: Int? = null
)

/**
 * Response when an invitation link is created.
 */
@Serializable
data class InvitationResponse(
    val id: String,
    val code: String,
    val eventId: String,
    val createdBy: String,
    val expiresAt: String? = null,
    val maxUses: Int? = null,
    val currentUses: Int = 0,
    val createdAt: String,
    val inviteUrl: String,
    val deepLinkUrl: String
)

/**
 * Response when resolving an invitation code (public, no auth required).
 */
@Serializable
data class InvitationResolveResponse(
    val code: String,
    val eventId: String,
    val eventTitle: String,
    val eventDescription: String,
    val eventStatus: String,
    val organizerId: String,
    val participantCount: Int,
    val isValid: Boolean,
    val expiresAt: String? = null
)

/**
 * Response when accepting an invitation.
 */
@Serializable
data class InvitationAcceptResponse(
    val success: Boolean,
    val eventId: String,
    val message: String
)
