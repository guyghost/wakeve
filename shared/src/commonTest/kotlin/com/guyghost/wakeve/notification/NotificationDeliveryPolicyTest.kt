package com.guyghost.wakeve.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationDeliveryPolicyTest {
    @Test
    fun `roadmap notification moments have explicit delivery profiles`() {
        val invite = NotificationType.EVENT_INVITE.deliveryProfile()
        assertEquals(NotificationPriority.HIGH, invite.priority)
        assertEquals(NotificationUserValue.ACTIONABLE, invite.userValue)
        assertEquals(NotificationSpamRisk.LOW, invite.spamRisk)
        assertFalse(invite.bypassQuietHours)

        val voteClosing = NotificationType.VOTE_CLOSE_REMINDER.deliveryProfile()
        assertEquals(NotificationPriority.MEDIUM, voteClosing.priority)
        assertEquals(NotificationUserValue.ACTIONABLE, voteClosing.userValue)
        assertEquals(NotificationSpamRisk.MEDIUM, voteClosing.spamRisk)

        val dateConfirmed = NotificationType.DATE_CONFIRMED.deliveryProfile()
        assertEquals(NotificationPriority.HIGH, dateConfirmed.priority)
        assertEquals(NotificationUserValue.CRITICAL, dateConfirmed.userValue)
        assertEquals(NotificationSpamRisk.LOW, dateConfirmed.spamRisk)

        val programChange = NotificationType.EVENT_UPDATE.deliveryProfile()
        assertEquals(NotificationPriority.LOW, programChange.priority)
        assertEquals(NotificationUserValue.CONTEXTUAL, programChange.userValue)
        assertEquals(NotificationSpamRisk.HIGH, programChange.spamRisk)
        assertTrue(programChange.batchWhenPossible)

        val departureReminder = NotificationType.MEETING_REMINDER.deliveryProfile()
        assertEquals(NotificationPriority.URGENT, departureReminder.priority)
        assertEquals(NotificationUserValue.CRITICAL, departureReminder.userValue)
        assertEquals(NotificationSpamRisk.LOW, departureReminder.spamRisk)
        assertTrue(departureReminder.bypassQuietHours)

        val budget = NotificationType.PAYMENT_DUE.deliveryProfile()
        assertEquals(NotificationPriority.MEDIUM, budget.priority)
        assertEquals(NotificationUserValue.ACTIONABLE, budget.userValue)
        assertEquals(NotificationSpamRisk.MEDIUM, budget.spamRisk)
    }

    @Test
    fun `noisy notification types are marked for batching`() {
        assertTrue(NotificationType.NEW_COMMENT.isHighSpamRisk())
        assertTrue(NotificationType.COMMENT_REPLY.isHighSpamRisk())
        assertTrue(NotificationType.EVENT_UPDATE.isHighSpamRisk())

        assertTrue(NotificationType.NEW_COMMENT.shouldBatchWhenPossible())
        assertTrue(NotificationType.COMMENT_REPLY.shouldBatchWhenPossible())
        assertTrue(NotificationType.EVENT_UPDATE.shouldBatchWhenPossible())
        assertFalse(NotificationType.EVENT_INVITE.shouldBatchWhenPossible())
        assertFalse(NotificationType.MEETING_REMINDER.shouldBatchWhenPossible())
    }

    @Test
    fun `only urgent delivery profiles bypass quiet hours`() {
        val bypassingTypes = NotificationType.entries
            .filter { it.deliveryProfile().bypassQuietHours }

        assertEquals(listOf(NotificationType.MEETING_REMINDER), bypassingTypes)
    }
}
