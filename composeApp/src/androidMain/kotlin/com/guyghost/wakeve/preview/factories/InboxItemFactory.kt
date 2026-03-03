package com.guyghost.wakeve.preview.factories

import com.guyghost.wakeve.models.InboxItem
import com.guyghost.wakeve.models.InboxItemStatus
import com.guyghost.wakeve.models.InboxItemType

/**
 * Factory for creating preview/test InboxItem instances.
 *
 * All properties use `get()` to return fresh instances on each access.
 */
object InboxItemFactory {

    /** An unread event invitation requiring RSVP. */
    val unreadInvitation
        get() = InboxItem(
            id = "inbox-001",
            type = InboxItemType.EVENT_INVITATION,
            status = InboxItemStatus.ACTION_REQUIRED,
            title = "You're invited to Weekend Hiking Trip",
            subtitle = "Marie Dupont invited you",
            description = "A two-day hike in the Vosges mountains. Please vote on your preferred dates.",
            eventId = "event-confirmed-002",
            eventTitle = "Weekend Hiking Trip",
            timestamp = "2026-03-03T09:00:00Z",
            isRead = false,
            commentCount = 0
        )

    /** A read informational update about poll results. */
    val readUpdate
        get() = InboxItem(
            id = "inbox-002",
            type = InboxItemType.POLL_UPDATE,
            status = InboxItemStatus.INFO,
            title = "Poll results updated",
            subtitle = "Team Offsite Q2",
            description = "3 new votes have been submitted. April 12 is currently leading.",
            eventId = "event-polling-003",
            eventTitle = "Team Offsite Q2",
            timestamp = "2026-03-02T15:30:00Z",
            isRead = true,
            commentCount = 2
        )

    /** An unread item that requires the user to take action (vote reminder). */
    val actionRequired
        get() = InboxItem(
            id = "inbox-003",
            type = InboxItemType.VOTE_REMINDER,
            status = InboxItemStatus.ACTION_REQUIRED,
            title = "Reminder: vote before April 8",
            subtitle = "Team Offsite Q2",
            description = "The voting deadline is approaching. Please submit your availability.",
            eventId = "event-polling-003",
            eventTitle = "Team Offsite Q2",
            timestamp = "2026-03-03T08:00:00Z",
            isRead = false,
            commentCount = 0
        )

    /** A completed / closed notification about a confirmed event. */
    val completed
        get() = InboxItem(
            id = "inbox-004",
            type = InboxItemType.EVENT_CONFIRMED,
            status = InboxItemStatus.COMPLETED,
            title = "Date confirmed: December 20",
            subtitle = "Holiday Dinner 2025",
            description = "The date has been finalized. Check the event for details.",
            eventId = "event-past-004",
            eventTitle = "Holiday Dinner 2025",
            timestamp = "2025-12-16T10:00:00Z",
            isRead = true,
            commentCount = 5
        )

    /** A success notification when a new participant joins. */
    val participantJoined
        get() = InboxItem(
            id = "inbox-005",
            type = InboxItemType.PARTICIPANT_JOINED,
            status = InboxItemStatus.SUCCESS,
            title = "Claire Bernard joined",
            subtitle = "Weekend Hiking Trip",
            description = "A new participant has joined your event.",
            eventId = "event-confirmed-002",
            eventTitle = "Weekend Hiking Trip",
            timestamp = "2026-03-02T12:00:00Z",
            isRead = true,
            commentCount = 0
        )

    /** A warning notification about a budget update. */
    val budgetWarning
        get() = InboxItem(
            id = "inbox-006",
            type = InboxItemType.BUDGET_UPDATE,
            status = InboxItemStatus.WARNING,
            title = "Budget exceeded by 15%",
            subtitle = "Team Offsite Q2",
            description = "The estimated cost now exceeds the planned budget.",
            eventId = "event-polling-003",
            eventTitle = "Team Offsite Q2",
            timestamp = "2026-03-01T16:45:00Z",
            isRead = false,
            commentCount = 1
        )

    /**
     * Returns a mixed list of inbox items covering various types and statuses.
     *
     * @param count Number of items to return (default 6, max cycles through all templates).
     */
    fun mixedList(count: Int = 6): List<InboxItem> {
        val all = listOf(
            unreadInvitation,
            readUpdate,
            actionRequired,
            completed,
            participantJoined,
            budgetWarning
        )
        if (count <= all.size) return all.take(count)

        // Cycle through templates for larger lists, generating unique IDs
        return (0 until count).map { index ->
            val template = all[index % all.size]
            template.copy(id = "inbox-mixed-${index.toString().padStart(3, '0')}")
        }
    }
}
