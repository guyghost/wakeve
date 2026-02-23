import SwiftUI

// MARK: - Notification Date Group

enum NotificationDateGroup: String, CaseIterable {
    case today = "Aujourd'hui"
    case yesterday = "Hier"
    case thisWeek = "Cette semaine"
    case older = "Plus ancien"
}

// MARK: - Notification Item Model

struct NotificationItemModel: Identifiable {
    let id: String
    let type: NotificationItemType
    var title: String
    var body: String
    var eventId: String?
    var isRead: Bool
    var createdAt: Date

    var icon: String {
        switch type {
        case .vote: return "chart.bar.fill"
        case .comment: return "bubble.left.fill"
        case .statusChange: return "arrow.triangle.2.circlepath"
        case .deadline: return "clock.fill"
        case .reminder: return "bell.fill"
        case .mention: return "at"
        case .invite: return "envelope.fill"
        case .eventUpdate: return "calendar"
        }
    }

    var iconColor: Color {
        switch type {
        case .vote: return .blue
        case .comment: return .green
        case .statusChange: return .orange
        case .deadline: return .red
        case .reminder: return .purple
        case .mention: return .teal
        case .invite: return .indigo
        case .eventUpdate: return .gray
        }
    }

    var relativeTimestamp: String {
        let now = Date()
        let interval = now.timeIntervalSince(createdAt)

        if interval < 60 {
            return "A l'instant"
        } else if interval < 3600 {
            let minutes = Int(interval / 60)
            return "Il y a \(minutes) min"
        } else if interval < 86400 {
            let hours = Int(interval / 3600)
            return "Il y a \(hours)h"
        } else if interval < 604800 {
            let days = Int(interval / 86400)
            return "Il y a \(days)j"
        } else {
            let formatter = DateFormatter()
            formatter.dateStyle = .short
            formatter.timeStyle = .none
            return formatter.string(from: createdAt)
        }
    }

    var dateGroup: NotificationDateGroup {
        let calendar = Calendar.current
        if calendar.isDateInToday(createdAt) {
            return .today
        } else if calendar.isDateInYesterday(createdAt) {
            return .yesterday
        } else if let weekAgo = calendar.date(byAdding: .day, value: -7, to: Date()),
                  createdAt > weekAgo {
            return .thisWeek
        } else {
            return .older
        }
    }
}

enum NotificationItemType {
    case vote
    case comment
    case statusChange
    case deadline
    case reminder
    case mention
    case invite
    case eventUpdate
}

// MARK: - Notifications View

struct NotificationsView: View {
    let userId: String
    let onDismiss: () -> Void

    @State private var notifications: [NotificationItemModel] = []
    @State private var isLoading = false
    @State private var isRefreshing = false
    @Environment(\.colorScheme) var colorScheme

    private var groupedNotifications: [(NotificationDateGroup, [NotificationItemModel])] {
        let grouped = Dictionary(grouping: notifications) { $0.dateGroup }
        return NotificationDateGroup.allCases.compactMap { group in
            guard let items = grouped[group], !items.isEmpty else { return nil }
            return (group, items.sorted { $0.createdAt > $1.createdAt })
        }
    }

    private var unreadCount: Int {
        notifications.filter { !$0.isRead }.count
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemBackground)
                    .ignoresSafeArea()

                if isLoading && notifications.isEmpty {
                    loadingView
                } else if notifications.isEmpty {
                    emptyStateView
                } else {
                    notificationListView
                }
            }
            .navigationTitle("Notifications")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onDismiss) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(.accentColor)
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    if unreadCount > 0 {
                        Button(action: markAllAsRead) {
                            Text("Tout lire")
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(.accentColor)
                        }
                    }
                }
            }
            .onAppear { loadNotifications() }
        }
    }

    // MARK: - List View

    private var notificationListView: some View {
        List {
            ForEach(groupedNotifications, id: \.0) { group, items in
                Section {
                    ForEach(items) { notification in
                        NotificationRow(notification: notification)
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    deleteNotification(notification.id)
                                } label: {
                                    Label("Supprimer", systemImage: "trash")
                                }

                                if !notification.isRead {
                                    Button {
                                        markAsRead(notification.id)
                                    } label: {
                                        Label("Lu", systemImage: "envelope.open")
                                    }
                                    .tint(.blue)
                                }
                            }
                            .swipeActions(edge: .leading) {
                                if !notification.isRead {
                                    Button {
                                        markAsRead(notification.id)
                                    } label: {
                                        Label("Marquer comme lu", systemImage: "checkmark.circle")
                                    }
                                    .tint(.green)
                                }
                            }
                            .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
                    }
                } header: {
                    Text(group.rawValue)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.secondary)
                        .textCase(nil)
                }
            }
        }
        .listStyle(.plain)
        .refreshable {
            await refreshNotifications()
        }
    }

    // MARK: - Empty State

    private var emptyStateView: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "bell.slash")
                .font(.system(size: 72, weight: .light))
                .foregroundColor(colorScheme == .dark ? Color(hex: "64748B") : Color(hex: "94A3B8"))

            Text("Aucune notification")
                .font(.title2.weight(.semibold))
                .foregroundColor(.primary)

            Text("Vos notifications apparaitront ici lorsque quelqu'un votera, commentera ou mettra a jour un evenement.")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Spacer()
        }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: .wakevePrimary))
                .scaleEffect(1.3)
            Text("Chargement des notifications...")
                .font(.body.weight(.medium))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Actions

    private func loadNotifications() {
        isLoading = true
        // TODO: Charger depuis le repository via l'API
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            notifications = sampleNotifications
            isLoading = false
        }
    }

    private func refreshNotifications() async {
        isRefreshing = true
        // TODO: Appeler l'API pour rafraichir
        try? await Task.sleep(nanoseconds: 1_000_000_000)
        await MainActor.run {
            isRefreshing = false
        }
    }

    private func markAsRead(_ notificationId: String) {
        if let index = notifications.firstIndex(where: { $0.id == notificationId }) {
            withAnimation(.easeInOut(duration: 0.2)) {
                notifications[index].isRead = true
            }
        }
        // TODO: Appeler l'API PUT /api/notifications/{id}/read
    }

    private func markAllAsRead() {
        withAnimation(.easeInOut(duration: 0.2)) {
            for index in notifications.indices {
                notifications[index].isRead = true
            }
        }
        // TODO: Appeler l'API PUT /api/notifications/read-all
    }

    private func deleteNotification(_ notificationId: String) {
        withAnimation(.easeOut(duration: 0.3)) {
            notifications.removeAll { $0.id == notificationId }
        }
        // TODO: Appeler l'API DELETE /api/notifications/{id}
    }
}

// MARK: - Notification Row

struct NotificationRow: View {
    let notification: NotificationItemModel

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            // Icon
            ZStack {
                Circle()
                    .fill(notification.iconColor.opacity(0.15))
                    .frame(width: 40, height: 40)

                Image(systemName: notification.icon)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(notification.iconColor)
            }

            // Content
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(notification.title)
                        .font(.system(size: 15, weight: notification.isRead ? .regular : .semibold))
                        .foregroundColor(.primary)
                        .lineLimit(1)

                    Spacer()

                    Text(notification.relativeTimestamp)
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }

                Text(notification.body)
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }

            // Unread indicator
            if !notification.isRead {
                Circle()
                    .fill(Color.blue)
                    .frame(width: 8, height: 8)
                    .padding(.top, 6)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(notification.isRead ? Color.clear : Color.blue.opacity(0.03))
        .contentShape(Rectangle())
    }
}

// MARK: - Sample Data

private let sampleNotifications: [NotificationItemModel] = [
    NotificationItemModel(
        id: "n1",
        type: .vote,
        title: "Nouveaux votes",
        body: "3 personnes ont vote pour \"Week-end ski 2024\"",
        eventId: "event1",
        isRead: false,
        createdAt: Date().addingTimeInterval(-300)
    ),
    NotificationItemModel(
        id: "n2",
        type: .comment,
        title: "Alice a commente",
        body: "\"Reunion famille\" : Super idee pour le restaurant !",
        eventId: "event2",
        isRead: false,
        createdAt: Date().addingTimeInterval(-3600)
    ),
    NotificationItemModel(
        id: "n3",
        type: .statusChange,
        title: "Date confirmee !",
        body: "La date de \"Voyage Espagne\" est confirmee : 15 mars 2026",
        eventId: "event3",
        isRead: true,
        createdAt: Date().addingTimeInterval(-7200)
    ),
    NotificationItemModel(
        id: "n4",
        type: .deadline,
        title: "Deadline approche",
        body: "Il reste 1 heure pour voter sur \"Anniversaire Bob\"",
        eventId: "event4",
        isRead: false,
        createdAt: Date().addingTimeInterval(-86400)
    ),
    NotificationItemModel(
        id: "n5",
        type: .reminder,
        title: "C'est aujourd'hui !",
        body: "\"Week-end montagne\" a lieu aujourd'hui. Bonne journee !",
        eventId: "event5",
        isRead: true,
        createdAt: Date().addingTimeInterval(-90000)
    ),
    NotificationItemModel(
        id: "n6",
        type: .invite,
        title: "Nouvelle invitation",
        body: "Marc vous invite a \"Soiree jeux de societe\"",
        eventId: "event6",
        isRead: true,
        createdAt: Date().addingTimeInterval(-259200)
    ),
    NotificationItemModel(
        id: "n7",
        type: .eventUpdate,
        title: "Resume de la semaine",
        body: "Vous avez 5 notifications non lues. 3 votes, 2 commentaires.",
        eventId: nil,
        isRead: true,
        createdAt: Date().addingTimeInterval(-604800)
    )
]

// MARK: - Preview

#Preview("NotificationsView") {
    NotificationsView(userId: "preview-user") {
        print("Dismiss")
    }
}
