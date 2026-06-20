package com.guyghost.wakeve.ui.notification

import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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
    fun notificationInboxErrorMessageDoesNotExposeExceptionDetails() {
        val message = notificationInboxErrorMessage(
            IllegalStateException("SQL failed for user SECRET-USER and notification SECRET-NOTIFICATION")
        )

        assertEquals("Impossible de charger les notifications.", message)
        assertFalse(message.contains("SECRET-USER"))
        assertFalse(message.contains("SECRET-NOTIFICATION"))
        assertFalse(message.contains("SQL failed"))
    }

    private fun notificationItem(
        id: String,
        isRead: Boolean
    ): NotificationItem {
        return NotificationItem(
            id = id,
            type = NotificationItemType.EVENT_UPDATE,
            title = "Title",
            body = "Body",
            isRead = isRead,
            createdAt = Date(1_800_000_000_000L)
        )
    }
}
