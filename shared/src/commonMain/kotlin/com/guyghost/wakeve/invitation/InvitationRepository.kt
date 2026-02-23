package com.guyghost.wakeve.invitation

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Invitation

/**
 * Repository for managing event invitation links.
 *
 * Handles CRUD operations for invitation codes that can be shared
 * to invite participants to events.
 */
class InvitationRepository(private val db: WakeveDb) {

    /**
     * Create a new invitation.
     */
    fun createInvitation(invitation: Invitation): Result<Invitation> {
        return try {
            db.invitationQueries.insertInvitation(
                id = invitation.id,
                code = invitation.code,
                eventId = invitation.eventId,
                createdBy = invitation.createdBy,
                expiresAt = invitation.expiresAt,
                maxUses = invitation.maxUses?.toLong(),
                currentUses = invitation.currentUses.toLong(),
                createdAt = invitation.createdAt
            )
            Result.success(invitation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get an invitation by its unique code.
     */
    fun getByCode(code: String): Invitation? {
        return try {
            db.invitationQueries.selectByCode(code).executeAsOneOrNull()?.toInvitation()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get an invitation by its ID.
     */
    fun getById(id: String): Invitation? {
        return try {
            db.invitationQueries.selectById(id).executeAsOneOrNull()?.toInvitation()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get all invitations for a specific event.
     */
    fun getByEventId(eventId: String): List<Invitation> {
        return try {
            db.invitationQueries.selectByEventId(eventId).executeAsList().map { it.toInvitation() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Increment the current uses count for an invitation.
     */
    fun incrementUses(code: String): Result<Unit> {
        return try {
            db.invitationQueries.incrementCurrentUses(code)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete an invitation by ID.
     */
    fun deleteById(id: String): Result<Unit> {
        return try {
            db.invitationQueries.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete all invitations for an event.
     */
    fun deleteByEventId(eventId: String): Result<Unit> {
        return try {
            db.invitationQueries.deleteByEventId(eventId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Extension function to convert SQLDelight generated model to domain model.
 */
private fun com.guyghost.wakeve.Invitation.toInvitation(): Invitation {
    return Invitation(
        id = id,
        code = code,
        eventId = eventId,
        createdBy = createdBy,
        expiresAt = expiresAt,
        maxUses = maxUses?.toInt(),
        currentUses = currentUses.toInt(),
        createdAt = createdAt
    )
}
