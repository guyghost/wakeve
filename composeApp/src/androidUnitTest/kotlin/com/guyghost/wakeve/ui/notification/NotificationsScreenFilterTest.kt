package com.guyghost.wakeve.ui.notification

import com.guyghost.wakeve.models.NotificationType
import com.guyghost.wakeve.R
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationsScreenFilterTest {
    @Test
    fun parseNotificationInboxFilterAcceptsOnlyUnread() {
        assertEquals(NotificationInboxFilter.UNREAD, parseNotificationInboxFilter(" unread "))
        assertEquals(NotificationInboxFilter.ALL, parseNotificationInboxFilter(null))
        assertEquals(NotificationInboxFilter.ALL, parseNotificationInboxFilter("all"))
        assertEquals(NotificationInboxFilter.ALL, parseNotificationInboxFilter("unread&admin=true"))
    }

    @Test
    fun filterNotificationItemsKeepsUnreadOnlyWhenRequested() {
        val unread = notificationItem(id = "unread", isRead = false)
        val read = notificationItem(id = "read", isRead = true)
        val notifications = listOf(unread, read)

        assertEquals(notifications, filterNotificationItems(notifications, NotificationInboxFilter.ALL))
        assertEquals(listOf(unread), filterNotificationItems(notifications, NotificationInboxFilter.UNREAD))
    }

    @Test
    fun notificationAttentionCueMarksBlockingRemindersAsActionable() {
        val cue = notificationAttentionCue(
            notificationItem(
                id = "deadline",
                isRead = false,
                notificationType = NotificationType.DEADLINE_REMINDER
            )
        )

        assertEquals(R.string.notification_attention_blocking, cue.labelRes)
        assertTrue(cue.requiresAttention)
    }

    @Test
    fun notificationAttentionCueMarksMentionsAsRequired() {
        val cue = notificationAttentionCue(
            notificationItem(
                id = "mention",
                isRead = false,
                notificationType = NotificationType.MENTION
            )
        )

        assertEquals(R.string.notification_attention_required, cue.labelRes)
        assertTrue(cue.requiresAttention)
    }

    @Test
    fun notificationAttentionCueExplainsDateConfirmationImportance() {
        val cue = notificationAttentionCue(
            notificationItem(
                id = "confirmed",
                isRead = false,
                notificationType = NotificationType.EVENT_CONFIRMED
            )
        )

        assertEquals(R.string.notification_attention_confirmed, cue.labelRes)
        assertTrue(cue.requiresAttention)
    }

    @Test
    fun notificationAttentionCueKeepsGenericUpdatesLowNoise() {
        val cue = notificationAttentionCue(
            notificationItem(
                id = "update",
                isRead = true,
                notificationType = NotificationType.EVENT_UPDATE
            )
        )

        assertEquals(R.string.notification_information, cue.labelRes)
        assertFalse(cue.requiresAttention)
    }

    private fun notificationItem(
        id: String,
        isRead: Boolean,
        notificationType: NotificationType? = null
    ): NotificationItem {
        return NotificationItem(
            id = id,
            type = NotificationItemType.EVENT_UPDATE,
            notificationType = notificationType,
            title = "Title",
            body = "Body",
            isRead = isRead,
            createdAt = Date(1_800_000_000_000L)
        )
    }
}
