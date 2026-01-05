import SwiftUI
import Shared

/// Messages tab view - notifications and chat
///
/// Features:
/// - Event notifications
/// - Chat with participants
/// - Notifications preferences
/// - Mark as read/unread
struct MessagesView: View {
    let userId: String

    @State private var notifications: [Notification] = []
    @State private var isLoading = true
    @State private var selectedTab: MessagesTab = .notifications

    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemBackground)
                    .ignoresSafeArea()

                VStack(spacing: 0) {
                    // Tab Bar
                    MessagesTabBar(selectedTab: $selectedTab)

                    // Content
                    if isLoading {
                        LoadingMessagesView()
                    } else if selectedTab == .notifications {
                        NotificationsList(notifications: notifications)
                    } else {
                        ConversationsList()
                    }
                }
            }
            .navigationTitle("Messages")
            .navigationBarTitleDisplayMode(.large)
        }
        .onAppear {
            loadMessages()
        }
    }

    // MARK: - Data Loading

    private func loadMessages() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            // Mock notifications
            notifications = [
                Notification(
                    id: "notif-1",
                    eventId: "event-1",
                    title: "Nouveau sondage",
                    message: "Un nouvel événement nécessite votre vote",
                    timestamp: Date().addingTimeInterval(-3600),
                    isRead: false,
                    type: .poll
                ),
                Notification(
                    id: "notif-2",
                    eventId: "event-2",
                    title: "Date confirmée",
                    message: "La date de votre événement a été confirmée",
                    timestamp: Date().addingTimeInterval(-86400),
                    isRead: true,
                    type: .confirmation
                ),
                Notification(
                    id: "notif-3",
                    eventId: "event-1",
                    title: "Rappel de vote",
                    message: "La date limite de vote est demain",
                    timestamp: Date().addingTimeInterval(-172800),
                    isRead: true,
                    type: .reminder
                )
            ]
            isLoading = false
        }
    }
}

// MARK: - Messages Tab Bar

enum MessagesTab: String, CaseIterable {
    case notifications = "Notifications"
    case conversations = "Conversations"

    var title: String {
        return self.rawValue
    }

    var iconName: String {
        switch self {
        case .notifications: return "bell.fill"
        case .conversations: return "message.fill"
        }
    }
}

struct MessagesTabBar: View {
    @Binding var selectedTab: MessagesTab

    var body: some View {
        HStack(spacing: 0) {
            ForEach(MessagesTab.allCases, id: \.self) { tab in
                Button(action: {
                    selectedTab = tab
                }) {
                    VStack(spacing: 8) {
                        Image(systemName: tab.iconName)
                            .font(.system(size: 20, weight: selectedTab == tab ? .semibold : .regular))

                        Text(tab.title)
                            .font(.subheadline.weight(.medium))
                    }
                    .foregroundColor(selectedTab == tab ? .wakevPrimary : .secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                }
            }
        }
        .overlay(
            Rectangle()
                .fill(Color.wakevPrimary)
                .frame(height: 2)
                .frame(maxWidth: .infinity),
            alignment: .bottom
        )
    }
}

// MARK: - Notifications List

struct NotificationsList: View {
    let notifications: [Notification]

    var body: some View {
        if notifications.isEmpty {
            EmptyNotificationsView()
        } else {
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(notifications, id: \.id) { notification in
                        NotificationCard(notification: notification)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            }
        }
    }
}

// MARK: - Notification Card

struct NotificationCard: View {
    let notification: Notification

    var body: some View {
        HStack(spacing: 12) {
            // Icon
            ZStack {
                Circle()
                    .fill(notification.type.backgroundColor)

                Image(systemName: notification.type.iconName)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white)
            }
            .frame(width: 40, height: 40)

            // Content
            VStack(alignment: .leading, spacing: 4) {
                Text(notification.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(.primary)

                Text(notification.message)
                    .font(.body)
                    .foregroundColor(.secondary)
                    .lineLimit(2)

                Text(formatTimestamp(notification.timestamp))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // Unread indicator
            if !notification.isRead {
                Circle()
                    .fill(Color.red)
                    .frame(width: 8, height: 8)
            }
        }
        .padding(16)
        .background(notification.isRead ? Color(.systemGray6) : Color(.systemGray5))
        .cornerRadius(12)
        .accessibilityElement(children: .combine)
    }

    private func formatTimestamp(_ date: Date) -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return formatter.localizedString(for: date, relativeTo: Date())
    }
}

// MARK: - Conversations List

struct ConversationsList: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Mock conversations
                ConversationRow(
                    name: "Alice Martin",
                    lastMessage: "Ok, je serai là !",
                    timestamp: Date().addingTimeInterval(-300),
                    unreadCount: 2
                )

                ConversationRow(
                    name: "Bob Dupont",
                    lastMessage: "Ça marche pour toi ?",
                    timestamp: Date().addingTimeInterval(-3600),
                    unreadCount: 0
                )

                ConversationRow(
                    name: "Charlie Durand",
                    lastMessage: "On se voit à 14h",
                    timestamp: Date().addingTimeInterval(-86400),
                    unreadCount: 0
                )
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
    }
}

struct ConversationRow: View {
    let name: String
    let lastMessage: String
    let timestamp: Date
    let unreadCount: Int

    var body: some View {
        HStack(spacing: 12) {
            // Avatar
            Circle()
                .fill(Color.wakevPrimary)
                .frame(width: 48, height: 48)
                .overlay(
                    Text(String(name.prefix(1)).uppercased())
                        .font(.headline.weight(.semibold))
                        .foregroundColor(.white)
                )

            // Content
            VStack(alignment: .leading, spacing: 4) {
                Text(name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(.primary)

                Text(lastMessage)
                    .font(.body)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // Metadata
            VStack(spacing: 4) {
                Text(formatTimestamp(timestamp))
                    .font(.caption)
                    .foregroundColor(.secondary)

                if unreadCount > 0 {
                    ZStack {
                        Circle()
                            .fill(Color.red)

                        Text("\(unreadCount)")
                            .font(.caption2.weight(.bold))
                            .foregroundColor(.white)
                    }
                    .frame(minWidth: 20, minHeight: 20)
                }
            }
        }
        .padding(16)
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }

    private func formatTimestamp(_ date: Date) -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return formatter.localizedString(for: date, relativeTo: Date())
    }
}

// MARK: - Empty Notifications View

struct EmptyNotificationsView: View {
    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "bell.slash")
                .font(.system(size: 48))
                .foregroundColor(.secondary)

            Text("Aucune notification")
                .font(.title2.weight(.semibold))
                .foregroundColor(.primary)

            Text("Vous serez notifié ici des nouvelles importantes")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 60)
    }
}

// MARK: - Loading Messages View

struct LoadingMessagesView: View {
    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: .wakevPrimary))
                .scaleEffect(1.3)

            Text("Chargement des messages...")
                .font(.body.weight(.medium))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Notification Model

struct Notification: Identifiable {
    let id: String
    let eventId: String
    let title: String
    let message: String
    let timestamp: Date
    let isRead: Bool
    let type: NotificationType
}

enum NotificationType {
    case poll
    case confirmation
    case reminder

    var backgroundColor: Color {
        switch self {
        case .poll: return Color.wakevPrimary
        case .confirmation: return Color.wakevSuccess
        case .reminder: return Color.wakevWarning
        }
    }

    var iconName: String {
        switch self {
        case .poll: return "chart.bar.fill"
        case .confirmation: return "checkmark.circle.fill"
        case .reminder: return "bell.fill"
        }
    }
}

// MARK: - Preview

#Preview("Messages View") {
    MessagesView(userId: "user123")
}
