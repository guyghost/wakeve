package com.guyghost.wakeve.invitationexperience

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventNotificationPolicyContractTest {
    private val policy = EventNotificationPolicy()

    @Test
    fun `effective notification policy applies system account event and quiet-hour priority`() {
        val cases = listOf(
            Case("denied", system = SystemNotificationAuthorization.DENIED, expected = blocked(EffectiveNotificationReason.BLOCKED_BY_SYSTEM)),
            Case("restricted", system = SystemNotificationAuthorization.RESTRICTED, expected = blocked(EffectiveNotificationReason.BLOCKED_BY_SYSTEM)),
            Case("not determined", system = SystemNotificationAuthorization.NOT_DETERMINED, expected = blocked(EffectiveNotificationReason.BLOCKED_BY_SYSTEM)),
            Case("provisional authorization", system = SystemNotificationAuthorization.PROVISIONAL, expected = eligible()),
            Case("ephemeral authorization", system = SystemNotificationAuthorization.EPHEMERAL, expected = eligible()),
            Case(
                "critical remains OS-gated but ignores event mute",
                type = EventNotificationType.SECURITY_CRITICAL,
                preference = EventNotificationPreference.MUTED,
                accountEnabled = setOf(EventNotificationType.SECURITY_CRITICAL),
                expected = eligible()
            ),
            Case(
                "critical security is not deferred by event quiet hours",
                type = EventNotificationType.SECURITY_CRITICAL,
                preference = EventNotificationPreference.MUTED,
                accountEnabled = setOf(EventNotificationType.SECURITY_CRITICAL),
                quietHours = true,
                expected = eligible()
            ),
            Case(
                "account disabled type cannot be re-enabled",
                preference = EventNotificationPreference.ALL_EVENT_UPDATES,
                accountEnabled = emptySet(),
                expected = blocked(EffectiveNotificationReason.BLOCKED_BY_ACCOUNT)
            ),
            Case(
                "event muted",
                preference = EventNotificationPreference.MUTED,
                expected = blocked(EffectiveNotificationReason.BLOCKED_BY_EVENT)
            ),
            Case(
                "essential allows reminder",
                type = EventNotificationType.VOTE_REMINDER,
                preference = EventNotificationPreference.ESSENTIAL_ONLY,
                expected = eligible()
            ),
            Case(
                "essential blocks comment",
                type = EventNotificationType.COMMENT,
                preference = EventNotificationPreference.ESSENTIAL_ONLY,
                accountEnabled = setOf(EventNotificationType.COMMENT),
                expected = blocked(EffectiveNotificationReason.BLOCKED_BY_EVENT)
            ),
            Case(
                "inherit account",
                preference = EventNotificationPreference.INHERIT_ACCOUNT,
                expected = eligible()
            ),
            Case(
                "quiet hours defer otherwise eligible event delivery",
                quietHours = true,
                expected = EffectiveNotificationDecision(
                    eligible = true,
                    deferred = true,
                    reason = EffectiveNotificationReason.DEFERRED_BY_QUIET_HOURS
                )
            )
        )

        cases.forEach { case ->
            assertEquals(
                case.expected,
                policy.evaluate(
                    EventNotificationPolicyInput(
                        notificationType = case.type,
                        eventPreference = case.preference,
                        accountEnabledTypes = case.accountEnabled,
                        quietHoursActive = case.quietHours,
                        systemAuthorization = case.system
                    )
                ),
                case.name
            )
        }
    }

    @Test
    fun `event preference repository accepts only an exact event scoped operation key and exposes pending sync`() = runTest {
        val repository = InMemoryEventNotificationPreferenceRepository()
        val key = OperationKey(
            subject = OperationSubject.EventNotification("event-1", "user-1"),
            action = InformationOperationAction.SAVE_EVENT_PREFERENCE,
            target = OperationTarget.User("user-1"),
            operationId = "operation-1"
        )

        val saved = repository.save(key, EventNotificationPreference.ESSENTIAL_ONLY)
        assertTrue(saved.isSuccess)
        assertEquals(
            EventNotificationPreferenceRecord(
                eventId = "event-1",
                userId = "user-1",
                preference = EventNotificationPreference.ESSENTIAL_ONLY,
                operationId = "operation-1",
                pendingSync = true
            ),
            saved.getOrNull()
        )
        assertNotNull(repository.get("event-1", "user-1"))

        assertEquals(
            saved.getOrNull(),
            repository.save(key, EventNotificationPreference.ESSENTIAL_ONLY).getOrNull(),
            "Exact operation replay must be idempotent."
        )
        assertTrue(
            repository.save(key, EventNotificationPreference.MUTED).isFailure,
            "The same operation id cannot be reused for different content."
        )

        val mismatchedKeys = listOf(
            key.copy(subject = OperationSubject.EventNotification("event-2", "user-1")),
            key.copy(target = OperationTarget.User("other-user")),
            key.copy(action = InformationOperationAction.DELETE_EVENT),
            key.copy(operationId = "")
        )
        mismatchedKeys.forEach { invalid ->
            assertTrue(repository.save(invalid, EventNotificationPreference.MUTED).isFailure)
        }
        assertNull(repository.get("event-2", "user-1"))
        assertEquals(
            EventNotificationPreference.ESSENTIAL_ONLY,
            repository.get("event-1", "user-1")?.preference,
            "Rejected writes must not alter the stable record."
        )
    }

    private fun blocked(reason: EffectiveNotificationReason) =
        EffectiveNotificationDecision(eligible = false, deferred = false, reason = reason)

    private fun eligible() =
        EffectiveNotificationDecision(
            eligible = true,
            deferred = false,
            reason = EffectiveNotificationReason.ELIGIBLE
        )

    private data class Case(
        val name: String,
        val type: EventNotificationType = EventNotificationType.EVENT_INVITE,
        val preference: EventNotificationPreference = EventNotificationPreference.ALL_EVENT_UPDATES,
        val accountEnabled: Set<EventNotificationType> = setOf(type),
        val quietHours: Boolean = false,
        val system: SystemNotificationAuthorization = SystemNotificationAuthorization.AUTHORIZED,
        val expected: EffectiveNotificationDecision
    )
}
