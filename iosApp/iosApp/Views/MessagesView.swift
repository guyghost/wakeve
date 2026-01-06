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
                .accessibilityLabel(tab.title)
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
        LiquidGlassCard(cornerRadius: 16, padding: 16) {
            HStack(spacing: 12) {
                // Icon
                ZStack {
                    Circle()
                        .fill(notification.type.gradientColors)
                        .frame(width: 40, height: 40)

                    Image(systemName: notification.type.iconName)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                }

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

                // Unread indicator badge
                if !notification.isRead {
                    LiquidGlassBadge(
                        text: "",
                        icon: "circle.fill",
                        style: notification.type.badgeStyle
                    )
                    .frame(width: 8, height: 8)
                    .padding(4)
                }
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(notification.title): \(notification.message)")
    }

    private func formatTimestamp(_ date: Date) -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .abbreviated
        return formatter.localizedString(for: date, relativeTo: Date())
    }
}

// MARK: - Conversations List

struct ConversationsList: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
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
        LiquidGlassListItem(
            title: name,
            subtitle: lastMessage,
            icon: "person.circle.fill",
            iconColor: .wakevPrimary,
            style: .default
        ) {
            EmptyView()
        } trailing: {
            VStack(alignment: .trailing, spacing: 4) {
                Text(formatTimestamp(timestamp))
                    .font(.caption)
                    .foregroundColor(.secondary)

                if unreadCount > 0 {
                    LiquidGlassBadge(
                        text: "\(unreadCount)",
                        style: .warning
                    )
                }
            }
        }
        .accessibilityLabel("\(name), \(lastMessage). \(unreadCount) messages non lus")
    }

    private func formatTimestamp(_ date: Date) -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .abbreviated
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

    var badgeStyle: LiquidGlassBadgeStyle {
        switch self {
        case .poll: return .info
        case .confirmation: return .success
        case .reminder: return .warning
        }
    }

    var gradientColors: LinearGradient {
        switch self {
        case .poll:
            return LinearGradient(
                gradient: Gradient(colors: [.wakevPrimary, .wakevAccent]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .confirmation:
            return LinearGradient(
                gradient: Gradient(colors: [.wakevSuccess, .wakevSuccessLight]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .reminder:
            return LinearGradient(
                gradient: Gradient(colors: [.wakevWarning, .wakevWarningLight]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
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
