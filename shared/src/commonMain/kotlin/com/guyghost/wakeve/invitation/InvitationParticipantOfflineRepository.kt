package com.guyghost.wakeve.invitation

import com.guyghost.wakeve.access.DateValidationState
import com.guyghost.wakeve.access.ParticipantRsvp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.Invitation
import com.guyghost.wakeve.sync.PendingSyncOperation
import com.guyghost.wakeve.sync.SyncOperationType

data class InvitationParticipantState(
    val id: String,
    val eventId: String,
    val userId: String,
    val rsvp: ParticipantRsvp,
    val dateValidation: DateValidationState,
    val joinedAt: String,
    val updatedAt: String
)

class InvitationParticipantOfflineRepository private constructor() {
    private val events = mutableMapOf<String, Event>()
    private val invitationsByCode = linkedMapOf<String, Invitation>()
    private val participantsByEventAndUser = linkedMapOf<Pair<String, String>, InvitationParticipantState>()
    private val pendingSync = mutableListOf<PendingSyncOperation>()
    private var sequence = 0

    interface SyncGateway {
        fun send(operation: PendingSyncOperation): Result<Unit>
    }

    fun saveEvent(event: Event) {
        events[event.id] = event
    }

    fun createInvitation(
        eventId: String,
        createdBy: String,
        createdAt: String,
        expiresAt: String? = null,
        maxUses: Int? = null
    ): Result<Invitation> = runCatching {
        require(events.containsKey(eventId)) { "Event not found: $eventId" }

        val invitation = Invitation(
            id = nextId("invitation"),
            code = nextId("invite-code"),
            eventId = eventId,
            createdBy = createdBy,
            expiresAt = expiresAt,
            maxUses = maxUses,
            createdAt = createdAt
        )

        invitationsByCode[invitation.code] = invitation
        enqueue(
            entityType = "invitation",
            entityId = invitation.id,
            operation = SyncOperationType.CREATE,
            createdAt = createdAt
        )
        invitation
    }

    fun getInvitationByCode(code: String): Invitation? = invitationsByCode[code]

    fun acceptInvitation(
        code: String,
        userId: String,
        acceptedAt: String
    ): Result<InvitationParticipantState> = runCatching {
        val invitation = invitationsByCode[code] ?: error("Invitation not found: $code")
        val participant = InvitationParticipantState(
            id = "${invitation.eventId}:$userId",
            eventId = invitation.eventId,
            userId = userId,
            rsvp = ParticipantRsvp.PENDING,
            dateValidation = DateValidationState.NOT_VALIDATED,
            joinedAt = acceptedAt,
            updatedAt = acceptedAt
        )

        participantsByEventAndUser[participant.eventId to participant.userId] = participant
        invitationsByCode[code] = invitation.copy(currentUses = invitation.currentUses + 1)
        enqueue(
            entityType = "participant",
            entityId = participant.id,
            operation = SyncOperationType.CREATE,
            createdAt = acceptedAt
        )
        participant
    }

    fun getParticipantState(
        eventId: String,
        userId: String
    ): InvitationParticipantState? = participantsByEventAndUser[eventId to userId]

    fun updateParticipantRsvp(
        eventId: String,
        userId: String,
        rsvp: ParticipantRsvp,
        dateValidation: DateValidationState,
        updatedAt: String
    ): Result<InvitationParticipantState> = runCatching {
        val current = participantsByEventAndUser[eventId to userId]
            ?: error("Participant not found: $eventId/$userId")
        val updated = current.copy(
            rsvp = rsvp,
            dateValidation = dateValidation,
            updatedAt = updatedAt
        )

        participantsByEventAndUser[eventId to userId] = updated
        enqueue(
            entityType = "participant",
            entityId = updated.id,
            operation = SyncOperationType.UPDATE,
            createdAt = updatedAt
        )
        updated
    }

    fun pendingSyncOperations(): List<PendingSyncOperation> = pendingSync.filterNot { it.isSynced }

    fun replayPendingSync(gateway: SyncGateway): Result<Unit> = runCatching {
        val sent = mutableListOf<PendingSyncOperation>()
        pendingSyncOperations().forEach { operation ->
            gateway.send(operation).getOrThrow()
            sent += operation
        }
        pendingSync.removeAll(sent.toSet())
    }

    private fun enqueue(
        entityType: String,
        entityId: String,
        operation: SyncOperationType,
        createdAt: String
    ) {
        pendingSync += PendingSyncOperation(
            id = nextId("sync"),
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            createdAt = createdAt
        )
    }

    private fun nextId(prefix: String): String {
        sequence += 1
        return "$prefix-$sequence"
    }

    companion object {
        fun inMemory(): InvitationParticipantOfflineRepository = InvitationParticipantOfflineRepository()
    }
}
