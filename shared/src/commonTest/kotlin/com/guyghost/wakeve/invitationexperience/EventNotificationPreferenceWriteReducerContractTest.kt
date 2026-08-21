package com.guyghost.wakeve.invitationexperience

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EventNotificationPreferenceWriteReducerContractTest {
    private val reducer = EventNotificationPreferenceWriteReducer()
    private val key = operationKey()
    private val previousRecord = record(
        preference = EventNotificationPreference.INHERIT_ACCOUNT,
        operationId = "previous-operation",
        pendingSync = false
    )
    private val previous = EventNotificationPreferenceWriteState.Stable(previousRecord)

    @Test
    fun `cancel before local commit restores the exact previous stable snapshot`() {
        val saving = saving()

        assertEquals(previous, reducer.cancel(saving, key))
        mismatchedKeys().forEach { mismatch ->
            assertEquals(
                saving,
                reducer.cancel(saving, mismatch),
                "Cancellation must match subject, action, target, and operation id."
            )
        }
    }

    @Test
    fun `matching acknowledgements move local commit to pending sync then repository truth to stable`() {
        val pending = assertIs<EventNotificationPreferenceWriteState.PendingSync>(
            reducer.acknowledge(saving(), key)
        )
        assertEquals(key, pending.operationKey)
        assertEquals(
            record(
                preference = EventNotificationPreference.ESSENTIAL_ONLY,
                operationId = "operation-1",
                pendingSync = true
            ),
            pending.record
        )

        val stable = assertIs<EventNotificationPreferenceWriteState.Stable>(
            reducer.acknowledge(pending, key)
        )
        assertEquals(pending.record.copy(pendingSync = false), stable.record)
    }

    @Test
    fun `late acknowledgement failure and retry require the full matching operation key`() {
        val states = listOf<EventNotificationPreferenceWriteState>(
            saving(),
            EventNotificationPreferenceWriteState.PendingSync(
                operationKey = key,
                record = record(
                    preference = EventNotificationPreference.ESSENTIAL_ONLY,
                    operationId = "operation-1",
                    pendingSync = true
                )
            )
        )

        states.forEach { state ->
            mismatchedKeys().forEach { mismatch ->
                assertEquals(state, reducer.acknowledge(state, mismatch))
                assertEquals(
                    state,
                    reducer.fail(state, mismatch, InvitationExperienceError.NETWORK_UNAVAILABLE)
                )
            }
        }

        val failed = EventNotificationPreferenceWriteState.Failed(
            operationKey = key,
            preference = EventNotificationPreference.ESSENTIAL_ONLY,
            error = InvitationExperienceError.NETWORK_UNAVAILABLE,
            previous = previous,
            committedRecord = null
        )
        mismatchedKeys().forEach { mismatch ->
            assertEquals(failed, reducer.retry(failed, mismatch))
        }
    }

    @Test
    fun `failure and retry preserve whether the operation committed locally`() {
        val failedBeforeCommit = assertIs<EventNotificationPreferenceWriteState.Failed>(
            reducer.fail(saving(), key, InvitationExperienceError.NETWORK_UNAVAILABLE)
        )
        assertEquals(previous, failedBeforeCommit.previous)
        assertEquals(null, failedBeforeCommit.committedRecord)
        assertEquals(saving(), reducer.retry(failedBeforeCommit, key))

        val pending = EventNotificationPreferenceWriteState.PendingSync(
            operationKey = key,
            record = record(
                preference = EventNotificationPreference.ESSENTIAL_ONLY,
                operationId = "operation-1",
                pendingSync = true
            )
        )
        val failedAfterCommit = assertIs<EventNotificationPreferenceWriteState.Failed>(
            reducer.fail(pending, key, InvitationExperienceError.NETWORK_UNAVAILABLE)
        )
        assertEquals(pending.record, failedAfterCommit.committedRecord)
        assertEquals(pending, reducer.retry(failedAfterCommit, key))
        assertEquals(
            failedAfterCommit,
            reducer.cancel(failedAfterCommit, key),
            "Cancellation must not roll back a locally committed preference."
        )
    }

    private fun saving() = EventNotificationPreferenceWriteState.Saving(
        operationKey = key,
        preference = EventNotificationPreference.ESSENTIAL_ONLY,
        previous = previous
    )

    private fun operationKey(
        eventId: String = "event-1",
        userId: String = "user-1",
        targetUserId: String = userId,
        action: InformationOperationAction = InformationOperationAction.SAVE_EVENT_PREFERENCE,
        operationId: String = "operation-1"
    ) = OperationKey(
        subject = OperationSubject.EventNotification(eventId, userId),
        action = action,
        target = OperationTarget.User(targetUserId),
        operationId = operationId
    )

    private fun mismatchedKeys() = listOf(
        operationKey(eventId = "event-2"),
        operationKey(userId = "user-2", targetUserId = "user-1"),
        operationKey(targetUserId = "user-2"),
        operationKey(action = InformationOperationAction.DELETE_EVENT),
        operationKey(operationId = "operation-2")
    )

    private fun record(
        preference: EventNotificationPreference,
        operationId: String,
        pendingSync: Boolean
    ) = EventNotificationPreferenceRecord(
        eventId = "event-1",
        userId = "user-1",
        preference = preference,
        operationId = operationId,
        pendingSync = pendingSync
    )
}
