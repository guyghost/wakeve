package com.guyghost.wakeve.notification

import android.app.NotificationManager
import com.guyghost.wakeve.notification.NotificationChannelManager.Companion.ChannelId
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationChannelManagerContractTest {

    @Test
    fun getChannelImportance_keepsOnlyExplicitlyImportantChannelHigh() {
        assertEquals(
            NotificationManager.IMPORTANCE_HIGH,
            NotificationChannelManager.getChannelImportance(ChannelId.HIGH_PRIORITY)
        )
        assertEquals(
            NotificationManager.IMPORTANCE_DEFAULT,
            NotificationChannelManager.getChannelImportance(ChannelId.EVENTS)
        )
        assertEquals(
            NotificationManager.IMPORTANCE_DEFAULT,
            NotificationChannelManager.getChannelImportance(ChannelId.REMINDERS)
        )
        assertEquals(
            NotificationManager.IMPORTANCE_LOW,
            NotificationChannelManager.getChannelImportance(ChannelId.PROGRESS)
        )
    }

    @Test
    fun getChannelId_routesRoutineCommentsToNonHeadsUpEventsChannel() {
        val channelId = NotificationChannelManager.getChannelId(NotificationType.NEW_COMMENT)

        assertEquals(ChannelId.EVENTS, channelId)
        assertEquals(
            NotificationManager.IMPORTANCE_DEFAULT,
            NotificationChannelManager.getChannelImportance(channelId)
        )
    }

    @Test
    fun getChannelId_keepsInvitesOnHighPriorityChannel() {
        val channelId = NotificationChannelManager.getChannelId(NotificationType.EVENT_INVITE)

        assertEquals(ChannelId.HIGH_PRIORITY, channelId)
        assertEquals(
            NotificationManager.IMPORTANCE_HIGH,
            NotificationChannelManager.getChannelImportance(channelId)
        )
    }
}
