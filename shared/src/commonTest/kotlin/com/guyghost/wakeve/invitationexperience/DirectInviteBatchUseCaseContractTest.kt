package com.guyghost.wakeve.invitationexperience

import com.guyghost.wakeve.models.EventStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DirectInviteBatchUseCaseContractTest {
    private val useCase = DefaultDirectInviteBatchUseCase()
    private val reducer = DirectInviteOperationReducer()
    private val capability = DirectInviteCapability.Ready(
        eventId = "event-1",
        actorId = "organizer-1",
        accessRevision = 4,
        allowedEventStatuses = setOf(EventStatus.DRAFT)
    )
    private val recipient1 = RecipientKey("hmac:v1:recipient-1")
    private val recipient2 = RecipientKey("hmac:v1:recipient-2")
    private val recipient3 = RecipientKey("opaque:v1:recipient-3")

    @Test
    fun `direct invite submission is draft only and capability binding is exact`() = runTest {
        val valid = useCase.submit(
            submitCommand(status = EventStatus.DRAFT, capability = capability)
        )
        assertEquals(
            DirectInviteOperation.Submitting(
                batchId = "batch-1",
                operationId = "operation-1",
                recipientKeys = setOf(recipient1, recipient2)
            ),
            valid
        )

        listOf(
            submitCommand(status = EventStatus.POLLING, capability = capability),
            submitCommand(
                status = EventStatus.DRAFT,
                capability = capability.copy(eventId = "other-event")
            ),
            submitCommand(
                status = EventStatus.DRAFT,
                capability = capability.copy(actorId = "other-actor")
            ),
            submitCommand(status = EventStatus.DRAFT, capability = DirectInviteCapability.Hidden)
        ).forEach { rejectedCommand ->
            val failed = assertIs<DirectInviteOperation.Failed>(useCase.submit(rejectedCommand))
            assertEquals(InvitationExperienceError.FORBIDDEN, failed.batchError)
        }
    }

    @Test
    fun `retry replays only retryable unresolved recipients under the same batch operation`() = runTest {
        val failed = DirectInviteOperation.Failed(
            batchId = "batch-1",
            operationId = "operation-1",
            requestedRecipientKeys = setOf(recipient1, recipient2, recipient3),
            outcomesByRecipientKey = mapOf(
                recipient1 to DirectInviteRecipientOutcome.ServerAccepted("invitation-1"),
                recipient2 to DirectInviteRecipientOutcome.Failed(InvitationExperienceError.NETWORK_UNAVAILABLE),
                recipient3 to DirectInviteRecipientOutcome.Invalid("INVALID_RECIPIENT")
            ),
            batchError = InvitationExperienceError.NETWORK_UNAVAILABLE
        )

        assertEquals(
            DirectInviteOperation.Submitting(
                batchId = "batch-1",
                operationId = "operation-1",
                recipientKeys = setOf(recipient2)
            ),
            useCase.retry(RetryDirectInviteBatchCommand(failed, capability))
        )
    }

    @Test
    fun `cancel affects only unresolved recipients and preserves settled outcomes`() = runTest {
        val failed = DirectInviteOperation.Failed(
            batchId = "batch-1",
            operationId = "operation-1",
            requestedRecipientKeys = setOf(recipient1, recipient2, recipient3),
            outcomesByRecipientKey = mapOf(
                recipient1 to DirectInviteRecipientOutcome.ServerAccepted("invitation-1"),
                recipient2 to DirectInviteRecipientOutcome.Failed(InvitationExperienceError.NETWORK_UNAVAILABLE),
                recipient3 to DirectInviteRecipientOutcome.Invalid("INVALID_RECIPIENT")
            ),
            batchError = InvitationExperienceError.NETWORK_UNAVAILABLE
        )

        assertEquals(
            DirectInviteOperation.Cancelled(
                batchId = "batch-1",
                outcomesByRecipientKey = mapOf(
                    recipient1 to DirectInviteRecipientOutcome.ServerAccepted("invitation-1"),
                    recipient2 to DirectInviteRecipientOutcome.Cancelled,
                    recipient3 to DirectInviteRecipientOutcome.Invalid("INVALID_RECIPIENT")
                )
            ),
            useCase.cancel(CancelDirectInviteBatchCommand(failed, capability))
        )
    }

    @Test
    fun `acknowledgement completes only when batch operation and recipient key set match exactly`() {
        val submitting = DirectInviteOperation.Submitting(
            batchId = "batch-1",
            operationId = "operation-1",
            recipientKeys = setOf(recipient1, recipient2)
        )
        val exactOutcomes = mapOf(
            recipient1 to DirectInviteRecipientOutcome.ServerAccepted("invitation-1"),
            recipient2 to DirectInviteRecipientOutcome.Invalid("INVALID_RECIPIENT")
        )

        assertEquals(
            DirectInviteOperation.Completed("batch-1", exactOutcomes),
            reducer.acknowledge(
                state = submitting,
                batchId = "batch-1",
                operationId = "operation-1",
                outcomesByRecipientKey = exactOutcomes
            )
        )

        listOf(
            reducer.acknowledge(submitting, "other-batch", "operation-1", exactOutcomes),
            reducer.acknowledge(submitting, "batch-1", "other-operation", exactOutcomes),
            reducer.acknowledge(submitting, "batch-1", "operation-1", exactOutcomes - recipient2),
            reducer.acknowledge(
                submitting,
                "batch-1",
                "operation-1",
                exactOutcomes + (recipient3 to DirectInviteRecipientOutcome.Invalid("EXTRA"))
            )
        ).forEach { rejected ->
            assertEquals(
                submitting,
                rejected,
                "A stale or incomplete acknowledgement must preserve the active operation."
            )
        }
    }

    private fun submitCommand(
        status: EventStatus,
        capability: DirectInviteCapability
    ) = SubmitDirectInviteBatchCommand(
        eventId = "event-1",
        actorId = "organizer-1",
        eventStatus = status,
        batchId = "batch-1",
        operationId = "operation-1",
        recipientKeys = setOf(recipient1, recipient2),
        capability = capability
    )
}
