package com.guyghost.wakeve.ui.notification

import com.guyghost.wakeve.notification.NotificationType
import com.guyghost.wakeve.notification.QuietTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPreferencesContractTest {
    @Test
    fun quietHoursLabelUsesSharedZeroPaddedFormat() {
        assertEquals(
            "22:00 - 08:00",
            notificationQuietHoursWindowLabel()
        )

        assertEquals(
            "09:05 - 17:30",
            notificationQuietHoursWindowLabel(
                start = QuietTime(9, 5),
                end = QuietTime(17, 30)
            )
        )
    }

    @Test
    fun defaultQuietHoursMatchSharedNotificationDefaults() {
        assertEquals(QuietTime(22, 0), notificationDefaultQuietHoursStart())
        assertEquals(QuietTime(8, 0), notificationDefaultQuietHoursEnd())
    }

    @Test
    fun notificationPreferenceContractsGroupActionableTypes() {
        val contracts = notificationPreferenceContracts().associateBy { it.id }

        assertEquals(
            setOf(NotificationType.VOTE_REMINDER, NotificationType.VOTE_CLOSE_REMINDER),
            contracts.getValue("votes").notificationTypes
        )
        assertEquals(
            setOf(NotificationType.NEW_COMMENT, NotificationType.COMMENT_REPLY, NotificationType.MENTION),
            contracts.getValue("comments").notificationTypes
        )
        assertEquals(
            setOf(NotificationType.DATE_CONFIRMED, NotificationType.NEW_SCENARIO, NotificationType.SCENARIO_SELECTED),
            contracts.getValue("status_changes").notificationTypes
        )
        assertEquals(
            setOf(NotificationType.EVENT_INVITE, NotificationType.MEETING_REMINDER, NotificationType.PAYMENT_DUE),
            contracts.getValue("reminders").notificationTypes
        )
        assertEquals(
            setOf(NotificationType.DEADLINE_REMINDER),
            contracts.getValue("deadlines").notificationTypes
        )
        assertEquals(
            setOf(NotificationType.EVENT_UPDATE),
            contracts.getValue("weekly_digest").notificationTypes
        )
    }

    @Test
    fun weeklyDigestIsOptInByDefaultToLimitNotificationNoise() {
        val contracts = notificationPreferenceContracts().associateBy { it.id }
        val defaultTypes = defaultEnabledNotificationTypes()

        assertFalse(contracts.getValue("weekly_digest").enabledByDefault)
        assertFalse(NotificationType.EVENT_UPDATE in defaultTypes)
        assertTrue(NotificationType.EVENT_INVITE in defaultTypes)
        assertTrue(NotificationType.MENTION in defaultTypes)
    }

    @Test
    fun notificationPreferenceContractsHaveStableUniqueIds() {
        val contracts = notificationPreferenceContracts()
        val ids = contracts.map { it.id }

        assertEquals(ids.toSet().size, ids.size)
        assertEquals(
            listOf("votes", "comments", "status_changes", "reminders", "deadlines", "weekly_digest"),
            ids
        )
    }
}
