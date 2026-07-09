package com.guyghost.wakeve.test

import com.guyghost.wakeve.confirmation.confirmationEffectKeys
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.repository.EventRepositoryInterface

/**
 * In-memory confirmation receipt seam for state-machine tests that use a
 * non-durable delegate. It models the same confirmation preconditions and
 * replay behavior as the durable repository without making a production
 * fallback look durable.
 */
class TypedConfirmationTestRepository(
    private val delegate: EventRepositoryInterface
) : EventRepositoryInterface by delegate {

    private val receiptsByOperationId = mutableMapOf<String, EventManagementContract.ConfirmationReceipt>()
    private val receiptsByEventId = mutableMapOf<String, EventManagementContract.ConfirmationReceipt>()
    private val syncStatusByReceiptId = mutableMapOf<String, EventManagementContract.DecisionSyncStatus>()

    override suspend fun confirmPollDate(
        command: EventManagementContract.ConfirmPollDateCommand
    ): EventManagementContract.ConfirmationResult {
        val event = delegate.getEvent(command.eventId)
            ?: return failure(command, EventManagementContract.ConfirmationFailureCode.EVENT_NOT_FOUND)
        if (event.organizerId != command.actorId) {
            return failure(command, EventManagementContract.ConfirmationFailureCode.NOT_ORGANIZER)
        }

        receiptsByOperationId[command.operationId]?.let { receipt ->
            return EventManagementContract.ConfirmationResult.AlreadyCommitted(
                receipt = receipt,
                projection = projection(receipt)
            )
        }
        receiptsByEventId[command.eventId]?.let { receipt ->
            return if (receipt.slotId == command.slotId) {
                EventManagementContract.ConfirmationResult.AlreadyCommitted(
                    receipt = receipt,
                    projection = projection(receipt)
                )
            } else {
                EventManagementContract.ConfirmationResult.Conflict(
                    operationId = command.operationId,
                    failure = EventManagementContract.ConfirmationFailure(
                        EventManagementContract.ConfirmationFailureCode.ALREADY_CONFIRMED_DIFFERENT_SLOT,
                        retryable = false
                    )
                )
            }
        }

        if (event.status != EventStatus.POLLING) {
            return failure(command, EventManagementContract.ConfirmationFailureCode.INVALID_EVENT_STATUS)
        }
        if (delegate.getPoll(command.eventId)?.votes.isNullOrEmpty()) {
            return failure(command, EventManagementContract.ConfirmationFailureCode.NO_VOTES)
        }
        val selectedSlot = event.proposedSlots.firstOrNull { it.id == command.slotId }
            ?: return failure(command, EventManagementContract.ConfirmationFailureCode.SLOT_NOT_FOUND)
        if (selectedSlot.start == null) {
            return failure(command, EventManagementContract.ConfirmationFailureCode.SLOT_NOT_CONFIRMABLE)
        }

        val confirmation = delegate.confirmEventDate(
            eventId = command.eventId,
            slotId = command.slotId,
            confirmedByOrganizerId = command.actorId
        )
        if (confirmation.isFailure) {
            return failure(
                command,
                EventManagementContract.ConfirmationFailureCode.LOCAL_PERSISTENCE_FAILED,
                retryable = true
            )
        }

        val effectKeys = confirmationEffectKeys(command.eventId, command.slotId)
        val receipt = EventManagementContract.ConfirmationReceipt(
            receiptId = command.operationId,
            operationId = command.operationId,
            eventId = command.eventId,
            slotId = command.slotId,
            actorId = command.actorId,
            committedAt = command.requestedAt.toString(),
            nextNavigationTarget = "event/${command.eventId}/scenarios",
            decisionSyncStatus = EventManagementContract.DecisionSyncStatus.LOCAL_PENDING,
            effectDispatchStatus = EventManagementContract.EffectDispatchStatus.QUEUED,
            effectOutbox = EventManagementContract.ConfirmationEffectOutbox(
                domainEventId = effectKeys.domainEventId,
                effectKey = effectKeys.effectKey
            )
        )
        receiptsByOperationId[receipt.operationId] = receipt
        receiptsByEventId[receipt.eventId] = receipt
        syncStatusByReceiptId[receipt.receiptId] = receipt.decisionSyncStatus
        return EventManagementContract.ConfirmationResult.Committed(
            receipt = receipt,
            projection = projection(receipt)
        )
    }

    override suspend fun markConfirmationSynced(
        receiptId: String
    ): EventManagementContract.ConfirmationProjection? {
        val receipt = receiptsByOperationId.values.firstOrNull { it.receiptId == receiptId } ?: return null
        syncStatusByReceiptId[receiptId] = EventManagementContract.DecisionSyncStatus.SERVER_ACKNOWLEDGED
        return projection(receipt)
    }

    private fun failure(
        command: EventManagementContract.ConfirmPollDateCommand,
        code: EventManagementContract.ConfirmationFailureCode,
        retryable: Boolean = false
    ) = EventManagementContract.ConfirmationResult.Failed(
        operationId = command.operationId,
        failure = EventManagementContract.ConfirmationFailure(code, retryable)
    )

    private fun projection(
        receipt: EventManagementContract.ConfirmationReceipt
    ) = EventManagementContract.ConfirmationProjection.Confirmed(
        eventId = receipt.eventId,
        slotId = receipt.slotId,
        receiptId = receipt.receiptId,
        decisionSyncStatus = syncStatusByReceiptId[receipt.receiptId] ?: receipt.decisionSyncStatus,
        effectDispatchStatus = receipt.effectDispatchStatus
    )
}
