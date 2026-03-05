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

    @State private var selectedFilter: InboxFilter = .inbox
    @State private var showNotificationBanner = true
    @State private var items: [InboxItemModel] = []
    @State private var isLoading = false
    @State private var selectedEventFilter: String? = nil
    @State private var showEventSheet = false
    @State private var isSelectionMode = false
    @State private var selectedItemIds: Set<String> = []
    @State private var showActionBar = false

    // Derived from loaded items for the event filter dropdown
    private var availableEvents: [String] {
        Array(Set(items.compactMap { $0.eventName })).sorted()
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Filter chips
                filterTabsView
                
                // Content
                if isLoading {
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
                                .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
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
                                .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
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
        .onChange(of: items) { _, newItems in
            unreadCount = newItems.filter { !$0.isRead }.count
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
            return items
        case .focused:
            return items.filter { $0.isFocused }
        case .unread:
            return items.filter { !$0.isRead }
        case .event:
            if let eventName = selectedEventFilter {
                return items.filter { $0.eventName?.contains(eventName) ?? false }
            }
            return items.filter { $0.eventName != nil }
        }
    }
    
    private var hasUnreadItems: Bool {
        items.contains { !$0.isRead }
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
        for index in items.indices {
            if selectedItemIds.contains(items[index].id) {
                items[index].isRead = true
            }
        }
    }
    
    private func markSelectedAsDone() {
        // Remove selected items from the list (mark as done/archived)
        items.removeAll { selectedItemIds.contains($0.id) }
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
            items = override
            isLoading = false
            return
        }
        
        isLoading = true
        
        // TODO: Load from repository when backend is available
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            #if DEBUG
            items = InboxItemFactory.mixedList()
            #else
            items = []
            #endif
            isLoading = false
        }
    }
    
    private func markAllAsRead() {
        for index in items.indices {
            items[index].isRead = true
        }
    }
    
    private func markItemAsRead(_ itemId: String) {
        if let index = items.firstIndex(where: { $0.id == itemId }) {
            items[index].isRead = true
        }
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
        HStack(alignment: .top, spacing: 12) {
            if isSelectionMode {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title2)
                    .foregroundStyle(isSelected ? Color.accentColor : .secondary)
                    .padding(.top, 2)
            }
            
            // Type icon
            Image(systemName: item.icon)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(item.iconColor)
                .frame(width: 28, height: 28)
                .background(item.iconColor.opacity(0.12))
                .clipShape(Circle())
                .padding(.top, 2)
            
            // Content
            VStack(alignment: .leading, spacing: 3) {
                // Top row: event name / time
                HStack {
                    if let eventName = item.eventName {
                        Text(eventName)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    
                    Spacer()
                    
                    Text(item.timeAgo)
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
            
            // Unread indicator
            if !item.isRead && !isSelectionMode {
                Circle()
                    .fill(Color.accentColor)
                    .frame(width: 10, height: 10)
                    .padding(.top, 6)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(item.isRead || isSelectionMode ? Color.clear : Color.accentColor.opacity(0.04))
        .contentShape(Rectangle())
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
