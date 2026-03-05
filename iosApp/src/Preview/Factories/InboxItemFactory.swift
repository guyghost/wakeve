import Foundation

#if DEBUG

enum InboxItemFactory {

    // MARK: - Named Variants

    /// Unread invitation — action required, not yet read.
    static var unreadInvitation: InboxItemModel {
        InboxItemModel(
            id: "inbox-unread-inv-001",
            title: "You're invited to Week-end ski 2024",
            message: "Marie Dupont invited you to join the trip",
            timeAgo: "17m",
            type: .invitation,
            isRead: false,
            commentCount: 0,
            isFocused: true,
            eventName: "Week-end ski 2024",
            eventId: "evt-inv-001"
        )
    }

    /// Read update — poll update type, informational, already read.
    static var readUpdate: InboxItemModel {
        InboxItemModel(
            id: "inbox-read-upd-002",
            title: "Poll results updated",
            message: "3 new votes on Birthday Dinner",
            timeAgo: "3h",
            type: .pollUpdate,
            isRead: true,
            commentCount: 5,
            isFocused: false,
            eventName: "Birthday Dinner",
            eventId: "evt-upd-002"
        )
    }

    /// Action required — vote reminder, unread.
    static var actionRequired: InboxItemModel {
        InboxItemModel(
            id: "inbox-action-003",
            title: "Vote reminder: Team Offsite",
            message: "Deadline is tomorrow — cast your vote now",
            timeAgo: "1h",
            type: .pollUpdate,
            isRead: false,
            commentCount: 2,
            isFocused: true,
            eventName: "Team Offsite",
            eventId: "evt-action-003"
        )
    }

    /// Completed — event confirmed, success status, already read.
    static var completed: InboxItemModel {
        InboxItemModel(
            id: "inbox-done-004",
            title: "Event confirmed: Summer BBQ",
            message: "Saturday 14 June at 16:00 — see you there!",
            timeAgo: "1d",
            type: .eventUpdate,
            isRead: true,
            commentCount: 12,
            isFocused: false,
            eventName: "Summer BBQ",
            eventId: "evt-done-004"
        )
    }

    // MARK: - Builder

    /// Create an inbox item with sensible defaults. Override only the parameters you need.
    static func make(
        id: String = "inbox-\(UUID().uuidString.prefix(8))",
        title: String = "Test notification",
        message: String = "Something happened in your event",
        timeAgo: String = "5m",
        type: InboxItemType = .invitation,
        isRead: Bool = false,
        commentCount: Int = 0,
        isFocused: Bool = false,
        eventName: String? = nil,
        eventId: String? = nil
    ) -> InboxItemModel {
        InboxItemModel(
            id: id,
            title: title,
            message: message,
            timeAgo: timeAgo,
            type: type,
            isRead: isRead,
            commentCount: commentCount,
            isFocused: isFocused,
            eventName: eventName,
            eventId: eventId
        )
    }

    // MARK: - Collections

    /// Generates a mixed list of inbox items cycling through the named variants.
    static func mixedList(count: Int = 8) -> [InboxItemModel] {
        let templates: [() -> InboxItemModel] = [
            { unreadInvitation },
            { readUpdate },
            { actionRequired },
            { completed }
        ]

        return (0..<count).map { index in
            var item = templates[index % templates.count]()
            item.title = "\(item.title) (\(index + 1))"
            return InboxItemModel(
                id: "inbox-mixed-\(index)",
                title: item.title,
                message: item.message,
                timeAgo: item.timeAgo,
                type: item.type,
                isRead: item.isRead,
                commentCount: item.commentCount,
                isFocused: item.isFocused,
                eventName: item.eventName,
                eventId: item.eventId
            )
        }
    }
}

#endif
