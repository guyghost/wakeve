import Foundation
import Shared

// MARK: - Inbox ViewModel

/// ViewModel connecting the iOS Inbox UI to the shared NotificationService KMP.
/// Provides notification loading, read/unread state, deletion, and type mapping.
@MainActor
class InboxViewModel: ObservableObject {

    // MARK: - Published State

    @Published var items: [InboxItemModel] = []
    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    @Published var unreadCount: Int = 0

    // MARK: - Private Dependencies

    private let userId: String
    private let database: WakeveDb

    // MARK: - Init

    init(userId: String) {
        self.userId = userId
        self.database = RepositoryProvider.shared.database
    }

    // MARK: - Load Notifications

    func loadNotifications() {
        Task {
            isLoading = true
            errorMessage = nil
            do {
                let notifications = database.notificationQueries.getNotifications(
                    user_id: userId, value_: 50
                ).executeAsList()

                self.items = notifications.map { mapToInboxItem($0) }
                self.unreadCount = items.filter { !$0.isRead }.count
            } catch {
                self.errorMessage = "Impossible de charger les notifications."
            }
            isLoading = false
        }
    }

    // MARK: - Actions

    func markAsRead(_ notificationId: String) {
        Task {
            database.notificationQueries.markAsRead(
                read_at: KotlinLong(value: Int64(Date().timeIntervalSince1970 * 1000)),
                id: notificationId
            )
            if let index = items.firstIndex(where: { $0.id == notificationId }) {
                items[index].isRead = true
                unreadCount = items.filter { !$0.isRead }.count
            }
        }
    }

    func markAllAsRead() {
        Task {
            database.notificationQueries.markAllAsRead(
                read_at: KotlinLong(value: Int64(Date().timeIntervalSince1970 * 1000)),
                user_id: userId
            )
            for index in items.indices {
                items[index].isRead = true
            }
            unreadCount = 0
        }
    }

    func deleteNotification(_ notificationId: String) {
        Task {
            database.notificationQueries.deleteNotification(id: notificationId)
            items.removeAll { $0.id == notificationId }
            unreadCount = items.filter { !$0.isRead }.count
        }
    }

    // MARK: - Mapping: KMP Notification row → InboxItemModel

    private func mapToInboxItem(_ notification: Shared.Notification) -> InboxItemModel {
        let typeStr = notification.type
        let inboxType = mapNotificationTypeString(typeStr)
        let isRead = notification.read_at != nil
        let timeAgo = formatTimeAgo(notification.sent_at?.int64Value)

        // Parse data JSON for eventName and eventId
        var eventName: String? = nil
        var eventId: String? = nil
        if let dataStr = notification.data_ {
            if let data = try? JSONSerialization.jsonObject(with: Data(dataStr.utf8)) as? [String: String] {
                eventName = data["eventName"]
                eventId = data["eventId"]
            }
        }

        return InboxItemModel(
            id: notification.id,
            title: notification.title,
            message: notification.body,
            timeAgo: timeAgo,
            type: inboxType,
            isRead: isRead,
            commentCount: 0,
            isFocused: inboxType == .invitation,
            eventName: eventName,
            eventId: eventId
        )
    }

    /// Map notification type string to InboxItemType
    private func mapNotificationTypeString(_ type: String) -> InboxItemType {
        let lower = type.lowercased()
        if lower.contains("invite") || lower.contains("invitation") { return .invitation }
        if lower.contains("vote") || lower.contains("poll") { return .pollUpdate }
        if lower.contains("comment") || lower.contains("mention") || lower.contains("reply") { return .comment }
        return .eventUpdate
    }

    // MARK: - Time Formatting

    private func formatTimeAgo(_ epochMs: Int64?) -> String {
        guard let ms = epochMs else { return "" }
        let date = Date(timeIntervalSince1970: Double(ms) / 1000.0)
        let interval = Date().timeIntervalSince(date)

        if interval < 60 { return "À l'instant" }
        if interval < 3600 { return "Il y a \(Int(interval / 60))m" }
        if interval < 86400 { return "Il y a \(Int(interval / 3600))h" }
        if interval < 604800 { return "Il y a \(Int(interval / 86400))j" }

        let df = DateFormatter()
        df.dateStyle = .short
        df.timeStyle = .none
        return df.string(from: date)
    }
}
