import SwiftUI

/**
 * InboxView - Notifications and invitations screen for iOS.
 *
 * Displays:
 * - Notifications (event invites, poll updates, comments)
 * - Filter chips (All, Unread, Invitations)
 * - Liquid Glass design system
 * - Matches Android InboxScreen functionality
 *
 * Uses:
 * - LiquidGlassCard for cards
 * - LiquidGlassButton for actions
 * - LiquidGlassBadge for status badges
 * - LiquidGlassDivider for separators
 * - LiquidGlassListItem for items
 */
struct InboxView: View {
    let userId: String
    let onBack: () -> Void
    
    @State private var selectedFilter: InboxFilter = .all
    @State private var items: [InboxItemModel] = []
    @State private var isLoading = false
    
    var body: some View {
        NavigationView {
            ZStack {
                // Background gradient using design system colors
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color.wakevPrimary.opacity(0.08),
                        Color.wakevAccent.opacity(0.08)
                    ]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Filter chips using LiquidGlassButton and LiquidGlassBadge
                    filterChipsView
                    
                    // Content
                    if isLoading {
                        loadingView
                    } else if filteredItems.isEmpty {
                        emptyStateView
                    } else {
                        itemListView
                    }
                }
            }
            .navigationTitle("Boîte de réception")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    LiquidGlassIconButton(
                        icon: "chevron.left",
                        size: 44,
                        gradientColors: [.wakevPrimary.opacity(0.3), .wakevAccent.opacity(0.3)]
                    ) {
                        onBack()
                    }
                    .accessibilityLabel("Retour")
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    if hasUnreadItems {
                        LiquidGlassIconButton(
                            icon: "checkmark.circle",
                            size: 44,
                            gradientColors: [.wakevSuccess.opacity(0.3), .wakevSuccess.opacity(0.2)]
                        ) {
                            markAllAsRead()
                        }
                        .accessibilityLabel("Tout marquer comme lu")
                    }
                }
            }
            #endif
        }
        .onAppear(perform: loadItems)
    }
    
    // MARK: - Filter Chips View
    
    private var filterChipsView: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                FilterChip(
                    title: "Tout",
                    count: items.count,
                    isSelected: selectedFilter == .all,
                    action: { selectedFilter = .all }
                )
                FilterChip(
                    title: "Tâches",
                    count: items.filter { $0.requiresAction }.count,
                    isSelected: selectedFilter == .tasks,
                    action: { selectedFilter = .tasks }
                )
                FilterChip(
                    title: "Messages",
                    count: items.filter { $0.type == .comment }.count,
                    isSelected: selectedFilter == .messages,
                    action: { selectedFilter = .messages }
                )
                FilterChip(
                    title: "Notifications",
                    count: items.filter { !$0.requiresAction && $0.type != .comment }.count,
                    isSelected: selectedFilter == .notifications,
                    action: { selectedFilter = .notifications }
                )
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
    }
    
    // MARK: - Filtered Items
    
    private var filteredItems: [InboxItemModel] {
        switch selectedFilter {
        case .all:
            return items
        case .tasks:
            return items.filter { $0.requiresAction }
        case .messages:
            return items.filter { $0.type == .comment }
        case .notifications:
            return items.filter { !$0.requiresAction && $0.type != .comment }
        }
    }
    
    private var hasUnreadItems: Bool {
        items.contains { !$0.isRead }
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.5)
                .tint(.wakevPrimary)
            Text("Chargement...")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    
    // MARK: - Empty State View
    
    private var emptyStateView: some View {
        VStack(spacing: 20) {
            Image(systemName: emptyStateIcon)
                .font(.system(size: 64))
                .foregroundColor(.wakevPrimary.opacity(0.3))
            
            Text(emptyStateTitle)
                .font(.title3)
                .fontWeight(.semibold)
                .foregroundColor(.primary)
            
            Text(emptyStateSubtitle)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    
    private var emptyStateIcon: String {
        switch selectedFilter {
        case .all: return "tray"
        case .tasks: return "checkmark.circle"
        case .messages: return "bubble.left"
        case .notifications: return "bell"
        }
    }
    
    private var emptyStateTitle: String {
        switch selectedFilter {
        case .all: return "Aucune notification"
        case .tasks: return "Aucune tâche"
        case .messages: return "Aucun message"
        case .notifications: return "Aucune notification"
        }
    }
    
    private var emptyStateSubtitle: String {
        switch selectedFilter {
        case .all: return "Vos notifications apparaîtront ici"
        case .tasks: return "Vous n'avez pas de tâches en pendiente"
        case .messages: return "Vous n'avez pas de nouveaux messages"
        case .notifications: return "Vous n'avez pas de notifications"
        }
    }
    
    // MARK: - Item List View
    
    private var itemListView: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(filteredItems) { item in
                    InboxItemRow(item: item)
                        .onTapGesture {
                            handleItemTap(item)
                        }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
    }
    
    // MARK: - Actions
    
    private func loadItems() {
        isLoading = true
        
        // TODO: Load from repository
        // For now, show sample data
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            items = sampleInboxItems
            isLoading = false
        }
    }
    
    private func markAllAsRead() {
        for index in items.indices {
            items[index].isRead = true
        }
    }
    
    private func handleItemTap(_ item: InboxItemModel) {
        // Mark as read
        if let index = items.firstIndex(where: { $0.id == item.id }) {
            items[index].isRead = true
        }
        
        // TODO: Navigate to relevant screen based on item type
    }
}

// MARK: - Inbox Item Row

struct InboxItemRow: View {
    let item: InboxItemModel
    
    var body: some View {
        HStack(spacing: 12) {
            // Icon with Liquid Glass effect
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                item.iconColor.opacity(0.2),
                                item.iconColor.opacity(0.1)
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 44, height: 44)
                
                Image(systemName: item.icon)
                    .font(.system(size: 18, weight: .medium))
                    .foregroundColor(item.iconColor)
            }
            .accessibilityHidden(true)
            
            // Content
            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(.subheadline)
                    .fontWeight(item.isRead ? .regular : .semibold)
                    .foregroundColor(.primary)
                
                Text(item.message)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
                
                HStack(spacing: 8) {
                    Text(item.timeAgo)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    
                    // Badge for item type
                    itemTypeBadge
                }
            }
            
            Spacer()
            
            // Unread indicator using LiquidGlassBadge
            if !item.isRead {
                LiquidGlassBadge(text: "Nouveau", style: .info)
                    .accessibilityLabel("Non lu")
            }
        }
        .padding(16)
        .liquidGlass(cornerRadius: 16, opacity: 0.85, intensity: 0.9)
        .opacity(item.isRead ? 0.85 : 1.0)
        .accessibilityLabel(item.accessibilityLabel)
        .accessibilityHint(item.accessibilityHint)
    }
    
    @ViewBuilder
    private var itemTypeBadge: some View {
        switch item.type {
        case .invitation:
            LiquidGlassBadge(text: "Invitation", icon: "envelope.fill", style: .info)
        case .pollUpdate:
            LiquidGlassBadge(text: "Sondage", icon: "chart.bar.fill", style: .accent)
        case .comment:
            LiquidGlassBadge(text: "Commentaire", icon: "bubble.left.fill", style: .success)
        case .eventUpdate:
            LiquidGlassBadge(text: "Mise à jour", icon: "calendar", style: .warning)
        }
    }
}

// MARK: - Supporting Types

enum InboxFilter {
    case all, tasks, messages, notifications
}

struct InboxItemModel: Identifiable {
    let id: String
    var title: String
    var message: String
    var timeAgo: String
    var type: InboxItemType
    var isRead: Bool
    
    var requiresAction: Bool {
        // Tasks are items that require user action (invitations, poll updates)
        switch type {
        case .invitation, .pollUpdate:
            return true
        case .comment, .eventUpdate:
            return false
        }
    }
    
    var icon: String {
        switch type {
        case .invitation: return "envelope.fill"
        case .pollUpdate: return "chart.bar.fill"
        case .comment: return "bubble.left.fill"
        case .eventUpdate: return "calendar"
        }
    }
    
    var iconColor: Color {
        switch type {
        case .invitation: return .wakevPrimary
        case .pollUpdate: return .wakevAccent
        case .comment: return .wakevSuccess
        case .eventUpdate: return .wakevWarning
        }
    }
    
    var accessibilityLabel: String {
        let typeLabel: String
        switch type {
        case .invitation: typeLabel = "Invitation"
        case .pollUpdate: typeLabel = "Mise à jour du sondage"
        case .comment: typeLabel = "Commentaire"
        case .eventUpdate: typeLabel = "Mise à jour de l'événement"
        }
        return "\(typeLabel): \(title)"
    }
    
    var accessibilityHint: String {
        switch type {
        case .invitation:
            return "Appuyez pour voir les détails de l'invitation"
        case .pollUpdate:
            return "Appuyez pour voter ou voir les résultats"
        case .comment:
            return "Appuyez pour lire le commentaire"
        case .eventUpdate:
            return "Appuyez pour voir les détails de l'événement"
        }
    }
}

enum InboxItemType {
    case invitation, pollUpdate, comment, eventUpdate
}

// MARK: - Sample Data

private let sampleInboxItems: [InboxItemModel] = [
    InboxItemModel(
        id: "1",
        title: "Nouvelle invitation",
        message: "Alice vous a invité à \"Week-end ski 2024\"",
        timeAgo: "il y a 5 min",
        type: .invitation,
        isRead: false
    ),
    InboxItemModel(
        id: "2",
        title: "Sondage mis à jour",
        message: "Les résultats du sondage pour \"Réunion famille\" sont disponibles",
        timeAgo: "il y a 2 h",
        type: .pollUpdate,
        isRead: false
    ),
    InboxItemModel(
        id: "3",
        title: "Nouveau commentaire",
        message: "Bob a commenté sur \"Voyage Espagne\"",
        timeAgo: "il y a 1 jour",
        type: .comment,
        isRead: true
    ),
    InboxItemModel(
        id: "4",
        title: "Événement confirmé",
        message: "La date de \"Week-end montagne\" a été confirmée",
        timeAgo: "il y a 2 jours",
        type: .eventUpdate,
        isRead: true
    )
]

// MARK: - Preview

#Preview("InboxView - Default") {
    InboxView(userId: "preview-user") {
        print("Back tapped")
    }
}

#Preview("InboxView - Empty") {
    InboxView(userId: "preview-user") {
        print("Back tapped")
    }
    .onAppear {
        // Simulate empty state
    }
}
