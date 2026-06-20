package com.guyghost.wakeve.offline

import com.guyghost.wakeve.access.DateValidationState
import com.guyghost.wakeve.access.ParticipantRsvp
import com.guyghost.wakeve.invitation.InvitationParticipantOfflineRepository
import com.guyghost.wakeve.invitation.InvitationParticipantState
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.sync.PendingSyncOperation
import com.guyghost.wakeve.sync.SyncOperationType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InvitationParticipantOfflineRepositoryTest {

    private val event = Event(
        id = "event-invite-offline-1",
        title = "Offline invite event",
        description = "Invitation flow should be local-first",
        organizerId = "organizer-1",
        participants = emptyList(),
        proposedSlots = listOf(
            TimeSlot(
                id = "slot-1",
                start = "2026-08-10T09:00:00Z",
                end = "2026-08-10T17:00:00Z",
                timezone = "UTC",
                timeOfDay = TimeOfDay.SPECIFIC
            )
        ),
        deadline = "2026-08-01T23:59:59Z",
        status = EventStatus.DRAFT,
        createdAt = "2026-05-21T10:00:00Z",
        updatedAt = "2026-05-21T10:00:00Z"
    )

    @Test
    fun `creating invitation writes locally and queues create sync operation`() {
        val repository = InvitationParticipantOfflineRepository.inMemory()
        repository.saveEvent(event)

        val invitation = repository.createInvitation(
            eventId = event.id,
            createdBy = "organizer-1",
            createdAt = "2026-05-21T10:05:00Z",
            expiresAt = "2026-06-21T10:05:00Z",
            maxUses = 12
        ).getOrThrow()

        assertEquals(invitation, repository.getInvitationByCode(invitation.code))

        val pending = repository.pendingSyncOperations()
        assertEquals(1, pending.size)
        assertEquals("invitation", pending.single().entityType)
        assertEquals(invitation.id, pending.single().entityId)
        assertEquals(SyncOperationType.CREATE, pending.single().operation)
        assertFalse(pending.single().isSynced)
    }

    @Test
    fun `accepting invitation creates pending participant locally and queues participant sync`() {
        val repository = InvitationParticipantOfflineRepository.inMemory()
        repository.saveEvent(event)
        val invitation = repository.createInvitation(
            eventId = event.id,
            createdBy = "organizer-1",
            createdAt = "2026-05-21T10:05:00Z"
        ).getOrThrow()

        val participant = repository.acceptInvitation(
            code = invitation.code,
            userId = "participant-1",
            acceptedAt = "2026-05-21T10:10:00Z"
        ).getOrThrow()

        assertEquals("participant-1", participant.userId)
        assertEquals(ParticipantRsvp.PENDING, participant.rsvp)
        assertEquals(DateValidationState.NOT_VALIDATED, participant.dateValidation)
        assertEquals(participant, repository.getParticipantState(event.id, "participant-1"))

        val participantSync = repository.pendingSyncOperations().single {
            it.entityType == "participant" && it.entityId == participant.id
        }
        assertEquals(SyncOperationType.CREATE, participantSync.operation)
        assertFalse(participantSync.isSynced)

        val invitationSync = repository.pendingSyncOperations().single {
            it.entityType == "invitation" &&
                it.entityId == invitation.id &&
                it.operation == SyncOperationType.UPDATE
        }
        assertFalse(invitationSync.isSynced)
        assertEquals(1, repository.getInvitationByCode(invitation.code)?.currentUses)
    }

    @Test
    fun `accepting invitation is idempotent for the same participant`() {
        val repository = InvitationParticipantOfflineRepository.inMemory()
        repository.saveEvent(event)
        val invitation = repository.createInvitation(
            eventId = event.id,
            createdBy = "organizer-1",
            createdAt = "2026-05-21T10:05:00Z",
            maxUses = 1
        ).getOrThrow()

        val first = repository.acceptInvitation(invitation.code, "participant-1", "2026-05-21T10:10:00Z").getOrThrow()
        val second = repository.acceptInvitation(invitation.code, "participant-1", "2026-05-21T10:12:00Z").getOrThrow()

        assertEquals(first, second)
        assertEquals(1, repository.getInvitationByCode(invitation.code)?.currentUses)
        assertEquals(
            1,
            repository.pendingSyncOperations().count {
                it.entityType == "participant" && it.entityId == first.id && it.operation == SyncOperationType.CREATE
            }
        )
    }

    @Test
    fun `accepting expired invitation fails without creating participant`() {
        val repository = InvitationParticipantOfflineRepository.inMemory()
        repository.saveEvent(event)
        val invitation = repository.createInvitation(
            eventId = event.id,
            createdBy = "organizer-1",
            createdAt = "2026-05-21T10:05:00Z",
            expiresAt = "2026-05-21T10:06:00Z"
        ).getOrThrow()

        val result = repository.acceptInvitation(invitation.code, "participant-1", "2026-05-21T10:10:00Z")

        assertInvitationFailure(result, "Invitation expired: ${invitation.code}")
        assertEquals(null, repository.getParticipantState(event.id, "participant-1"))
        assertEquals(0, repository.getInvitationByCode(invitation.code)?.currentUses)
    }

    @Test
    fun `accepting invitation at max uses fails without consuming another use`() {
        val repository = InvitationParticipantOfflineRepository.inMemory()
        repository.saveEvent(event)
        val invitation = repository.createInvitation(
            eventId = event.id,
            createdBy = "organizer-1",
            createdAt = "2026-05-21T10:05:00Z",
            maxUses = 1
        ).getOrThrow()
        repository.acceptInvitation(invitation.code, "participant-1", "2026-05-21T10:10:00Z").getOrThrow()

        val result = repository.acceptInvitation(invitation.code, "participant-2", "2026-05-21T10:12:00Z")

        assertInvitationFailure(result, "Invitation max uses reached: ${invitation.code}")
        assertEquals(null, repository.getParticipantState(event.id, "participant-2"))
        assertEquals(1, repository.getInvitationByCode(invitation.code)?.currentUses)
    }

    @Test
    fun `updating RSVP and retained date validation is local first and queued for sync`() {
        val repository = InvitationParticipantOfflineRepository.inMemory()
        repository.saveEvent(event)
        val invitation = repository.createInvitation(
            eventId = event.id,
            createdBy = "organizer-1",
            createdAt = "2026-05-21T10:05:00Z"
        ).getOrThrow()
        repository.acceptInvitation(invitation.code, "participant-1", "2026-05-21T10:10:00Z")

        val updated = repository.updateParticipantRsvp(
            eventId = event.id,
            userId = "participant-1",
            rsvp = ParticipantRsvp.ACCEPTED,
            dateValidation = DateValidationState.VALIDATED_RETAINED_DATE,
            updatedAt = "2026-05-21T10:15:00Z"
        ).getOrThrow()

        assertEquals(ParticipantRsvp.ACCEPTED, updated.rsvp)
        assertEquals(DateValidationState.VALIDATED_RETAINED_DATE, updated.dateValidation)
        assertEquals(updated, repository.getParticipantState(event.id, "participant-1"))

        val rsvpSync = repository.pendingSyncOperations().last()
        assertEquals("participant", rsvpSync.entityType)
        assertEquals(updated.id, rsvpSync.entityId)
        assertEquals(SyncOperationType.UPDATE, rsvpSync.operation)
        assertFalse(rsvpSync.isSynced)
    }

    @Test
    fun `sync replay sends pending invitation and participant operations in creation order`() {
        val repository = InvitationParticipantOfflineRepository.inMemory()
        val gateway = RecordingInvitationSyncGateway()
        repository.saveEvent(event)
        val invitation = repository.createInvitation(
            eventId = event.id,
            createdBy = "organizer-1",
            createdAt = "2026-05-21T10:05:00Z"
        ).getOrThrow()
        repository.acceptInvitation(invitation.code, "participant-1", "2026-05-21T10:10:00Z")
        repository.updateParticipantRsvp(
            eventId = event.id,
            userId = "participant-1",
            rsvp = ParticipantRsvp.ACCEPTED,
            dateValidation = DateValidationState.VALIDATED_RETAINED_DATE,
            updatedAt = "2026-05-21T10:15:00Z"
        )

        val replayResult = repository.replayPendingSync(gateway)

        assertTrue(replayResult.isSuccess)
        assertEquals(
            listOf(
                "invitation:${SyncOperationType.CREATE}",
                "participant:${SyncOperationType.CREATE}",
                "invitation:${SyncOperationType.UPDATE}",
                "participant:${SyncOperationType.UPDATE}"
            ),
            gateway.sentOperations.map { "${it.entityType}:${it.operation}" }
        )
        assertTrue(repository.pendingSyncOperations().isEmpty())
    }

    private fun assertInvitationFailure(
        result: Result<InvitationParticipantState>,
        expectedMessage: String
    ) {
        assertFalse(result.isSuccess)
        val error = assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertContains(error.message.orEmpty(), expectedMessage)
    }
}

private class RecordingInvitationSyncGateway : InvitationParticipantOfflineRepository.SyncGateway {
    val sentOperations = mutableListOf<PendingSyncOperation>()

    override fun send(operation: PendingSyncOperation): Result<Unit> {
        sentOperations += operation
        return Result.success(Unit)
    }
}
