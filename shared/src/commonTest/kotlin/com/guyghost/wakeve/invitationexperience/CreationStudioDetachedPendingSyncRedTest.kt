package com.guyghost.wakeve.invitationexperience

import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.poll.PollBallotContract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CreationStudioDetachedPendingSyncRedTest {
    private val machine = CreationStudioStateMachine()

    @Test
    fun `LATE_LOCAL_COMMIT enters detached pending with binding and accepts sync outcomes`() {
        val envelope = envelope("late-local-operation")
        val detached = detachedCommitting(envelope)
        val transitioned = machine.transition(
            detached,
            CreationStudioEvent.LateLocalCommit(
                eventId = EVENT_ID,
                draftRevision = DRAFT_REVISION,
                committedRevision = COMMITTED_REVISION,
                operationId = envelope.identity.operationId,
                pendingSync = true
            )
        )

        val pending = assertIs<CreationStudioState.DetachedPendingSync>(
            transitioned,
            "A detached late local receipt must expose its durable sync binding, never DetachedCommitted."
        )
        assertEquals(expectedBinding(envelope), pending.binding)
        assertEquals(envelope, pending.envelope)
        assertIs<CreationStudioState.Completed>(
            machine.transition(pending, syncCompleted(envelope.identity.operationId))
        )
        assertIs<CreationStudioState.SyncFailed>(
            machine.transition(
                pending,
                CreationStudioEvent.SyncFailed(
                    eventId = EVENT_ID,
                    committedRevision = COMMITTED_REVISION,
                    operationId = envelope.identity.operationId,
                    error = InvitationExperienceError.NETWORK_UNAVAILABLE,
                    retryable = true
                )
            )
        )
    }

    @Test
    fun `RESOLUTION_RESULT local committed enters detached pending with binding and accepts sync outcomes`() {
        val envelope = envelope("resolved-local-operation")
        val resolving = CreationStudioState.DetachedResolving(
            operationId = envelope.identity.operationId,
            mode = StudioMode.New,
            draft = draft(),
            artwork = Artwork.None,
            durableOperationRef = envelope.durableOperationRef,
            requestFingerprint = envelope.requestFingerprint,
            resolutionRetryBudget = envelope.maxResolutionAttempts,
            resolutionAttempt = 1,
            attemptId = ATTEMPT_ID,
            fence = 1,
            envelope = envelope
        )
        val transitioned = machine.transition(
            resolving,
            CreationStudioEvent.ResolutionResult(
                draftRevision = DRAFT_REVISION,
                operationId = envelope.identity.operationId,
                outcome = StudioResolutionOutcome.LOCAL_COMMITTED,
                eventId = EVENT_ID,
                committedRevision = COMMITTED_REVISION,
                pendingSync = true,
                attemptId = ATTEMPT_ID,
                fence = 1
            )
        )

        val pending = assertIs<CreationStudioState.DetachedPendingSync>(
            transitioned,
            "A proven detached local receipt must expose its binding for the immediate idempotent sync trigger."
        )
        assertEquals(expectedBinding(envelope), pending.binding)
        assertEquals(envelope, pending.envelope)
        assertIs<CreationStudioState.Completed>(
            machine.transition(pending, syncCompleted(envelope.identity.operationId))
        )
        assertIs<CreationStudioState.SyncFailed>(
            machine.transition(
                pending,
                CreationStudioEvent.SyncFailed(
                    eventId = EVENT_ID,
                    committedRevision = COMMITTED_REVISION,
                    operationId = envelope.identity.operationId,
                    error = InvitationExperienceError.SERVER_UNAVAILABLE,
                    retryable = true
                )
            )
        )
    }

    @Test
    fun `missing malformed or divergent sync binding is terminal repository inconsistency from every pending state`() {
        val envelope = envelope("corrupt-binding-operation")
        val binding = expectedBinding(envelope)
        val pendingStates = listOf(
            "missing attached binding" to CreationStudioState.PendingSync(
                eventId = EVENT_ID,
                committedRevision = COMMITTED_REVISION,
                operationId = envelope.identity.operationId,
                envelope = envelope
            ),
            "malformed detached binding" to CreationStudioState.DetachedPendingSync(
                eventId = EVENT_ID,
                committedRevision = COMMITTED_REVISION,
                operationId = envelope.identity.operationId,
                binding = binding,
                envelope = envelope
            ),
            "divergent detached binding" to CreationStudioState.DetachedPendingSync(
                eventId = EVENT_ID,
                committedRevision = COMMITTED_REVISION,
                operationId = envelope.identity.operationId,
                binding = binding,
                envelope = envelope
            )
        )

        pendingStates.forEach { (caseName, pending) ->
            val terminal = assertIs<CreationStudioState.SyncFailed>(
                machine.transition(
                    pending,
                    CreationStudioEvent.SyncFailed(
                        eventId = EVENT_ID,
                        committedRevision = COMMITTED_REVISION,
                        operationId = envelope.identity.operationId,
                        error = InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN,
                        retryable = false,
                        code = PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT,
                        commitOutcome = PollBallotContract.CommitOutcome.UNKNOWN
                    )
                ),
                "$caseName must leave PendingSync instead of silently returning or polling forever."
            )
            assertEquals(PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT, terminal.code, caseName)
            assertEquals(PollBallotContract.CommitOutcome.UNKNOWN, terminal.commitOutcome, caseName)
            assertEquals(false, terminal.retryable, caseName)
            assertEquals(
                terminal,
                machine.transition(
                    terminal,
                    CreationStudioEvent.RetrySync(
                        eventId = EVENT_ID,
                        committedRevision = COMMITTED_REVISION,
                        operationId = envelope.identity.operationId
                    )
                ),
                "$caseName is terminal and cannot be retried."
            )
        }
    }

    private fun detachedCommitting(envelope: StudioCommitEnvelope) =
        CreationStudioState.DetachedCommitting(
            operationId = envelope.identity.operationId,
            mode = StudioMode.New,
            draft = draft(),
            artwork = Artwork.None,
            durableOperationRef = envelope.durableOperationRef,
            requestFingerprint = envelope.requestFingerprint,
            resolutionRetryBudget = envelope.maxResolutionAttempts,
            envelope = envelope
        )

    private fun envelope(operationId: String) = StudioCommitEnvelopeFactory.build(
        UpdateDraftAggregateCommand(
            eventId = EVENT_ID,
            actorId = ACTOR_ID,
            expectedBaseRevision = 0,
            eventDraft = fields(),
            artwork = Artwork.None,
            operationId = operationId,
            draftRevision = DRAFT_REVISION
        )
    )

    private fun expectedBinding(envelope: StudioCommitEnvelope) = CreationStudioSyncBinding(
        eventId = EVENT_ID,
        aggregateRevision = COMMITTED_REVISION,
        operationId = envelope.identity.operationId,
        durableOperationRef = envelope.durableOperationRef,
        requestFingerprint = envelope.requestFingerprint
    )

    private fun syncCompleted(operationId: String) = CreationStudioEvent.SyncCompleted(
        eventId = EVENT_ID,
        committedRevision = COMMITTED_REVISION,
        operationId = operationId
    )

    private fun draft() = CreationDraft(
        draftRevision = DRAFT_REVISION,
        fields = fields(),
        artworkChoice = ArtworkChoice.None
    )

    private fun fields() = StudioEventFields(
        title = "Detached Studio receipt",
        description = "Durable local truth remains pending until its exact ACK.",
        deadline = "2099-01-01T00:00:00Z",
        eventType = EventType.OTHER
    )

    private companion object {
        const val EVENT_ID = "detached-studio-event"
        const val ACTOR_ID = "detached-studio-actor"
        const val DRAFT_REVISION = 4L
        const val COMMITTED_REVISION = 1L
        const val ATTEMPT_ID = "detached-resolution-attempt"
    }
}
