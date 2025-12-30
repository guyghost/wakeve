import SwiftUI

/**
 * InboxView - Notifications and invitations screen for iOS.
 * 
 * Displays:
 * - Notifications (event invites, poll updates, comments)
 * - Filter chips (All, Unread, Invitations)
 * - Liquid Glass design system
 * - Matches Android InboxScreen functionality
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
                // Background gradient
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color.blue.opacity(0.1),
                        Color.purple.opacity(0.1)
                    ]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Filter chips
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            FilterChip(
                                title: "Tout",
                                isSelected: selectedFilter == .all,
                                count: items.count,
                                action: { selectedFilter = .all }
                            )
                            FilterChip(
                                title: "Non lus",
                                isSelected: selectedFilter == .unread,
                                count: items.filter { !$0.isRead }.count,
                                action: { selectedFilter = .unread }
                            )
                            FilterChip(
                                title: "Invitations",
                                isSelected: selectedFilter == .invitations,
                                count: items.filter { $0.type == .invitation }.count,
                                action: { selectedFilter = .invitations }
                            )
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                    }
                    
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
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(.primary)
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: markAllAsRead) {
                        Image(systemName: "checkmark.circle")
                            .foregroundColor(.primary)
                    }
                }
            }
            #endif
        }
        .onAppear(perform: loadItems)
    }
    
    // MARK: - Filtered Items
    
    private var filteredItems: [InboxItemModel] {
        switch selectedFilter {
        case .all:
            return items
        case .unread:
            return items.filter { !$0.isRead }
        case .invitations:
            return items.filter { $0.type == .invitation }
        }
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.5)
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
                .foregroundColor(.gray.opacity(0.5))
            
            Text(emptyStateTitle)
                .font(.title3)
                .fontWeight(.semibold)
            
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
        case .unread: return "checkmark.circle"
        case .invitations: return "envelope"
        }
    }
    
    private var emptyStateTitle: String {
        switch selectedFilter {
        case .all: return "Aucune notification"
        case .unread: return "Tout est lu"
        case .invitations: return "Aucune invitation"
        }
    }
    
    private var emptyStateSubtitle: String {
        switch selectedFilter {
        case .all: return "Vos notifications apparaîtront ici"
        case .unread: return "Vous êtes à jour avec toutes vos notifications"
        case .invitations: return "Vous n'avez pas d'invitations en attente"
        }
    }
    
    // MARK: - Item List View
    
    private var itemListView: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(filteredItems) { item in
                    InboxItemCard(item: item)
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

// MARK: - Filter Chip

struct FilterChip: View {
    let title: String
    let isSelected: Bool
    let count: Int
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(isSelected ? .semibold : .regular)
                
                if count > 0 {
                    Text("\(count)")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(isSelected ? Color.white.opacity(0.3) : Color.gray.opacity(0.2))
                        .cornerRadius(10)
                }
            }
            .foregroundColor(isSelected ? .white : .primary)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(
                isSelected
                    ? Color.blue
                    : Color.gray.opacity(0.1)
            )
            .cornerRadius(20)
        }
    }
}

// MARK: - Inbox Item Card

struct InboxItemCard: View {
    let item: InboxItemModel
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            // Icon
            Image(systemName: item.icon)
                .font(.system(size: 24))
                .foregroundColor(item.iconColor)
                .frame(width: 40, height: 40)
                .background(item.iconColor.opacity(0.1))
                .cornerRadius(20)
            
            // Content
            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(.subheadline)
                    .fontWeight(item.isRead ? .regular : .semibold)
                
                Text(item.message)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
                
                Text(item.timeAgo)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            // Unread indicator
            if !item.isRead {
                Circle()
                    .fill(Color.blue)
                    .frame(width: 8, height: 8)
            }
        }
        .padding(16)
        .background(.ultraThinMaterial)
        .cornerRadius(16)
    }
}

// MARK: - Supporting Types

enum InboxFilter {
    case all, unread, invitations
}

struct InboxItemModel: Identifiable {
    let id: String
    var title: String
    var message: String
    var timeAgo: String
    var type: InboxItemType
    var isRead: Bool
    
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
        case .invitation: return .blue
        case .pollUpdate: return .purple
        case .comment: return .green
        case .eventUpdate: return .orange
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
