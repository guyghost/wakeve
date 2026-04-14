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
 * - GlassBadge for status badges
 */
struct InboxView: View {
    let userId: String
    let onBack: () -> Void
    @Binding var unreadCount: Int

    /// Optional override for initial items (used by previews).
    var initialItems: [InboxItemModel]? = nil

    @StateObject private var viewModel: InboxViewModel
    @State private var selectedFilter: InboxFilter = .inbox
    @State private var showNotificationBanner = true
    @State private var selectedEventFilter: String? = nil
    @State private var showEventSheet = false
    @State private var isSelectionMode = false
    @State private var selectedItemIds: Set<String> = []
    @State private var showActionBar = false

    init(userId: String, onBack: @escaping () -> Void, unreadCount: Binding<Int>, initialItems: [InboxItemModel]? = nil) {
        self.userId = userId
        self.onBack = onBack
        self._unreadCount = unreadCount
        self.initialItems = initialItems
        self._viewModel = StateObject(wrappedValue: InboxViewModel(userId: userId))
    }

    // Derived from loaded items for the event filter dropdown
    private var availableEvents: [String] {
        Array(Set(viewModel.items.compactMap { $0.eventName })).sorted()
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Filter chips
                filterTabsView
                
                // Content
                if viewModel.isLoading {
                    loadingView
                } else if filteredItems.isEmpty {
                    emptyStateView
                } else {
                    List {
                        ForEach(filteredItems) { item in
                            if isSelectionMode {
                                InboxRow(
                                    item: item,
                                    isSelectionMode: true,
                                    isSelected: selectedItemIds.contains(item.id)
                                )
                                .onTapGesture {
                                    toggleSelection(for: item.id)
                                }
                                .listRowBackground(Color.clear)
                            } else {
                                NavigationLink {
                                    InboxDetailView(item: item)
                                        .onAppear {
                                            markItemAsRead(item.id)
                                        }
                                } label: {
                                    InboxRow(
                                        item: item,
                                        isSelectionMode: false,
                                        isSelected: false
                                    )
                                }
                                .listRowBackground(
                                    item.isRead ? Color.clear : Color.accentColor.opacity(0.04)
                                )
                            }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle(isSelectionMode ? "\(selectedItemIds.count) selected" : "Inbox")
            #if os(iOS)
            .navigationBarTitleDisplayMode(isSelectionMode ? .inline : .large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    if isSelectionMode {
                        Button("Cancel") {
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                isSelectionMode = false
                                showActionBar = false
                                selectedItemIds.removeAll()
                            }
                        }
                    } else {
                        Button("Select") {
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                isSelectionMode = true
                                showActionBar = true
                            }
                        }
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    if isSelectionMode {
                        Button("Select All") {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                selectedItemIds = Set(filteredItems.map { $0.id })
                            }
                        }
                    }
                }
            }
            .toolbar(showActionBar ? .hidden : .visible, for: .tabBar)
            #endif
            .overlay(alignment: .bottom) {
                if showActionBar {
                    actionBarView
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .sheet(isPresented: $showEventSheet) {
                EventFilterSheet(
                    events: availableEvents,
                    selectedEvent: $selectedEventFilter,
                    onSelect: {
                        selectedFilter = .event
                    },
                    onDismiss: {
                        showEventSheet = false
                    }
                )
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
            }
        }
        .onAppear(perform: loadItems)
        .onChange(of: viewModel.items) { _, newItems in
            unreadCount = newItems.filter { !$0.isRead }.count
        }
        .refreshable {
            viewModel.loadNotifications()
        }
    }
    
    // MARK: - Filter Tabs View
    
    private var filterTabsView: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Filter chips row
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    // Active filter indicator (shows when filter is active)
                    if selectedFilter != .inbox || selectedEventFilter != nil {
                        ActiveFilterIndicator(count: activeFilterCount) {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                selectedFilter = .inbox
                                selectedEventFilter = nil
                            }
                        }
                    }
                    
                    // Inbox filter with dropdown
                    FilterTabButton(
                        title: "Inbox",
                        isSelected: selectedFilter == .inbox && selectedEventFilter == nil,
                        hasDropdown: true,
                        action: { 
                            selectedFilter = .inbox
                            selectedEventFilter = nil
                        }
                    )
                    
                    // Focused filter with "New" badge
                    FilterTabButton(
                        title: "Focused",
                        isSelected: selectedFilter == .focused,
                        badge: "New",
                        action: { selectedFilter = .focused }
                    )
                    
                    // Unread filter
                    FilterTabButton(
                        title: "Unread",
                        isSelected: selectedFilter == .unread,
                        action: { selectedFilter = .unread }
                    )
                    
                    // Event filter with sheet
                    EventFilterTabButton(
                        title: selectedEventFilter ?? "Event",
                        isSelected: selectedFilter == .event,
                        hasDropdown: true,
                        action: {
                            showEventSheet = true
                        }
                    )
                }
                .padding(.horizontal, 16)
            }
            
            // Divider line
            Divider()
                .padding(.top, 8)
        }
    }
    
    // MARK: - Filtered Items
    
    private var filteredItems: [InboxItemModel] {
        switch selectedFilter {
        case .inbox:
            return viewModel.items
        case .focused:
            return viewModel.items.filter { $0.isFocused }
        case .unread:
            return viewModel.items.filter { !$0.isRead }
        case .event:
            if let eventName = selectedEventFilter {
                return viewModel.items.filter { $0.eventName?.contains(eventName) ?? false }
            }
            return viewModel.items.filter { $0.eventName != nil }
        }
    }

    private var hasUnreadItems: Bool {
        viewModel.items.contains { !$0.isRead }
    }
    
    private var activeFilterCount: Int {
        var count = 0
        if selectedFilter != .inbox {
            count += 1
        }
        if selectedEventFilter != nil {
            count += 1
        }
        return count
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.5)
                .tint(.wakevePrimary)
            Text(String(localized: "common.loading"))
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    
    // MARK: - Empty State View
    
    private var emptyStateView: some View {
        ContentUnavailableView(
            emptyStateTitle,
            systemImage: emptyStateIcon,
            description: Text(emptyStateSubtitle)
        )
    }
    
    private var emptyStateIcon: String {
        switch selectedFilter {
        case .inbox: return "tray"
        case .focused: return "star"
        case .unread: return "envelope.open"
        case .event: return "calendar"
        }
    }
    
    private var emptyStateTitle: String {
        switch selectedFilter {
        case .inbox: return String(localized: "inbox.empty.no_notifications")
        case .focused: return String(localized: "inbox.empty.no_focused")
        case .unread: return String(localized: "inbox.empty.no_unread")
        case .event: return selectedEventFilter != nil ? String(format: String(localized: "inbox.empty.no_notifications_for"), selectedEventFilter!) : String(localized: "inbox.empty.no_events")
        }
    }
    
    private var emptyStateSubtitle: String {
        switch selectedFilter {
        case .inbox: return String(localized: "inbox.empty.notifications_subtitle")
        case .focused: return String(localized: "inbox.empty.focused_subtitle")
        case .unread: return String(localized: "inbox.empty.unread_subtitle")
        case .event: return String(localized: "inbox.empty.events_subtitle")
        }
    }
    
    // MARK: - Action Bar View
    
    private var actionBarView: some View {
        HStack(spacing: 12) {
            ActionBarButton(
                title: "Mark as Read",
                isEnabled: !selectedItemIds.isEmpty,
                action: {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        markSelectedAsRead()
                        isSelectionMode = false
                        showActionBar = false
                        selectedItemIds.removeAll()
                    }
                }
            )
            
            ActionBarButton(
                title: "Mark as Done",
                isEnabled: !selectedItemIds.isEmpty,
                action: {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        markSelectedAsDone()
                        isSelectionMode = false
                        showActionBar = false
                        selectedItemIds.removeAll()
                    }
                }
            )
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(.bar)
    }
    
    private func markSelectedAsRead() {
        for id in selectedItemIds {
            viewModel.markAsRead(id)
        }
    }

    private func markSelectedAsDone() {
        for id in selectedItemIds {
            viewModel.deleteNotification(id)
        }
    }
    
    private func toggleSelection(for itemId: String) {
        if selectedItemIds.contains(itemId) {
            selectedItemIds.remove(itemId)
        } else {
            selectedItemIds.insert(itemId)
        }
    }
    
    // MARK: - Actions

    private func loadItems() {
        if let override = initialItems {
            // Preview mode: use injected data
            viewModel.items = override
            viewModel.isLoading = false
            return
        }
        viewModel.loadNotifications()
    }

    private func markItemAsRead(_ itemId: String) {
        viewModel.markAsRead(itemId)
    }
}

// MARK: - Filter Tab Button

struct FilterTabButton: View {
    let title: String
    let isSelected: Bool
    var hasDropdown: Bool = false
    var badge: String? = nil
    let action: @MainActor () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Text(title)
                    .font(.subheadline.weight(isSelected ? .semibold : .regular))
                
                if let badge = badge {
                    Text(badge)
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.accentColor)
                        .clipShape(RoundedRectangle(cornerRadius: 4))
                }
                
                if hasDropdown {
                    Image(systemName: "chevron.down")
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(.secondary)
                }
            }
            .foregroundStyle(isSelected ? .primary : .secondary)
            .padding(.vertical, 8)
        }
    }
}

// MARK: - Active Filter Indicator

struct ActiveFilterIndicator: View {
    let count: Int
    let action: @MainActor () -> Void
    
    var body: some View {
        Menu {
            Section {
                Text(filterAppliedText)
                    .foregroundStyle(.secondary)
            }
            
            Button(role: .destructive) {
                action()
            } label: {
                Text("Clear all filters")
            }
        } label: {
            HStack(spacing: 6) {
                Image(systemName: "line.3.horizontal.decrease")
                    .font(.footnote)
                
                Text("\(count)")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(.white)
                    .frame(width: 20, height: 20)
                    .background(Circle().fill(Color.accentColor))
            }
            .foregroundStyle(.primary)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                Capsule()
                    .fill(Color(.systemGray5))
            )
        }
    }
    
    private var filterAppliedText: String {
        if count == 1 {
            return "One filter applied."
        } else {
            return "\(count) filters applied."
        }
    }
}

// MARK: - Event Filter Tab Button

struct EventFilterTabButton: View {
    let title: String
    let isSelected: Bool
    var hasDropdown: Bool = false
    let action: @MainActor () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Text(title)
                    .font(.subheadline.weight(isSelected ? .semibold : .regular))
                
                if hasDropdown {
                    Image(systemName: "chevron.down")
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(.secondary)
                }
            }
            .foregroundStyle(isSelected ? .primary : .secondary)
            .padding(.vertical, 8)
        }
    }
}

// MARK: - Event Filter Sheet

struct EventFilterSheet: View {
    let events: [String]
    @Binding var selectedEvent: String?
    let onSelect: @MainActor () -> Void
    let onDismiss: @MainActor () -> Void
    
    var body: some View {
        NavigationView {
            List {
                // All events option
                Section {
                    Button {
                        selectedEvent = nil
                        onSelect()
                        onDismiss()
                    } label: {
                        HStack {
                            Text(String(localized: "inbox.all_events"))
                                .foregroundColor(.primary)
                            Spacer()
                            if selectedEvent == nil {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.blue)
                            }
                        }
                    }
                }
                
                // List of events
                Section(header: Text(String(localized: "inbox.events"))) {
                    ForEach(events, id: \.self) { event in
                        Button {
                            selectedEvent = event
                            onSelect()
                            onDismiss()
                        } label: {
                            HStack {
                                Text(event)
                                    .foregroundColor(.primary)
                                Spacer()
                                if selectedEvent == event {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle(String(localized: "inbox.filter_by_event"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(String(localized: "common.close")) {
                        onDismiss()
                    }
                }
            }
        }
    }
}

// MARK: - Inbox Row

struct InboxRow: View {
    let item: InboxItemModel
    var isSelectionMode: Bool = false
    var isSelected: Bool = false
    
    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            if isSelectionMode {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title2)
                    .foregroundStyle(isSelected ? Color.accentColor : .secondary)
                    .padding(.top, 2)
            } else {
                // Unread indicator (like Mail.app)
                Circle()
                    .fill(item.isRead ? Color.clear : Color.accentColor)
                    .frame(width: 10, height: 10)
                    .padding(.top, 6)
            }
            
            // Type icon
            Image(systemName: item.icon)
                .font(.title3.weight(.medium))
                .foregroundStyle(item.iconColor)
                .frame(width: 24)
                .padding(.top, 2)
            
            // Content
            VStack(alignment: .leading, spacing: 3) {
                // Event name
                if let eventName = item.eventName {
                    Text(eventName)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                
                // Title
                Text(item.title)
                    .font(.subheadline.weight(item.isRead ? .regular : .semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(2)
                
                // Subtitle
                Text(item.message)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            
            Spacer()
            
            // Timestamp
            Text(item.timeAgo)
                .font(.caption)
                .foregroundStyle(.secondary)
                .padding(.top, 2)
        }
        .padding(.vertical, 6)
    }
}

// MARK: - Action Bar Button

struct ActionBarButton: View {
    let title: String
    let isEnabled: Bool
    let action: @MainActor () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .foregroundStyle(.primary)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(
                    Capsule()
                        .fill(Color(.systemGray5))
                )
        }
        .disabled(!isEnabled)
        .opacity(isEnabled ? 1.0 : 0.5)
        .animation(.easeInOut(duration: 0.2), value: isEnabled)
    }
}

// MARK: - Supporting Types

enum InboxFilter {
    case inbox, focused, unread, event
}

struct InboxItemModel: Identifiable, Equatable, Hashable {
    let id: String
    var title: String
    var message: String
    var timeAgo: String
    var type: InboxItemType
    var isRead: Bool
    var commentCount: Int
    var isFocused: Bool
    var eventName: String?
    var eventId: String?
    
    var requiresAction: Bool {
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
        case .invitation: return .wakevePrimary
        case .pollUpdate: return .wakeveAccent
        case .comment: return .wakeveSuccess
        case .eventUpdate: return .wakeveWarning
        }
    }
    
    var accessibilityLabel: String {
        let typeLabel: String
        switch type {
        case .invitation: typeLabel = String(localized: "inbox.type.invitation")
        case .pollUpdate: typeLabel = String(localized: "inbox.type.poll_update")
        case .comment: typeLabel = String(localized: "inbox.type.comment")
        case .eventUpdate: typeLabel = String(localized: "inbox.type.event_update")
        }
        return "\(typeLabel): \(title)"
    }
    
    var accessibilityHint: String {
        switch type {
        case .invitation:
            return String(localized: "inbox.invitation_hint")
        case .pollUpdate:
            return String(localized: "inbox.vote_hint")
        case .comment:
            return String(localized: "inbox.comment_hint")
        case .eventUpdate:
            return String(localized: "inbox.event_hint")
        }
    }
}

enum InboxItemType {
    case invitation, pollUpdate, comment, eventUpdate
}

// MARK: - Previews

#if DEBUG
#Preview("Inbox - With Notifications") {
    InboxView(userId: "preview-user", onBack: {}, unreadCount: .constant(3), initialItems: InboxItemFactory.mixedList())
        .previewEnvironment()
}
#endif

#Preview("Inbox - Empty") {
    InboxView(userId: "preview-user", onBack: {}, unreadCount: .constant(0), initialItems: [])
        .previewEnvironment()
}
