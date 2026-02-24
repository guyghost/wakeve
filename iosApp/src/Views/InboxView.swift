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
    @Binding var unreadCount: Int

    @State private var selectedFilter: InboxFilter = .inbox
    @State private var showNotificationBanner = true
    @State private var items: [InboxItemModel] = []
    @State private var isLoading = false
    @State private var selectedEventFilter: String? = nil
    @State private var showEventSheet = false
    @State private var isSelectionMode = false
    @State private var selectedItemIds: Set<String> = []
    @State private var showActionBar = false
    
    // Sample events for the dropdown
    private let availableEvents = ["Week-end ski 2024", "Réunion famille", "Voyage Espagne", "Week-end montagne", "Anniversaire Alice"]
    
    var body: some View {
        NavigationView {
            ZStack {
                // System background adapts to light/dark mode (white like home page)
                Color(.systemBackground)
                    .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Header
                    headerView
                    
                    // Filter tabs
                    filterTabsView
                    
                    // Content
                    if isLoading {
                        loadingView
                    } else if filteredItems.isEmpty {
                        ScrollView {
                            if showNotificationBanner {
                                notificationBanner
                            }
                            emptyStateView
                        }
                    } else {
                        ScrollView {
                            itemListView
                        }
                    }
                }
            }
            #if os(iOS)
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
    
    // MARK: - Header View
    
    private var headerView: some View {
        HStack {
            if isSelectionMode {
                Text("\(selectedItemIds.count) selected")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.primary)
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
    }
    
    // MARK: - Filter Tabs View
    
    private var filterTabsView: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Large title
            Text("Inbox")
                .font(.system(size: 34, weight: .bold))
                .foregroundColor(.primary)
                .padding(.horizontal, 16)
                .padding(.bottom, 12)
            
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
    
    // MARK: - Notification Banner
    
    private var notificationBanner: some View {
        HStack(alignment: .top, spacing: 12) {
            // Bell icon
            ZStack {
                Circle()
                    .fill(Color.red)
                    .frame(width: 36, height: 36)
                
                Image(systemName: "bell.fill")
                    .font(.system(size: 16))
                    .foregroundColor(.white)
            }
            
            // Content
            VStack(alignment: .leading, spacing: 4) {
                Text("Never miss what's important to you.")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.primary)
                
                Text("Customize your Notification experience with push notifications, working hours, and swipe actions.")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
                    .lineLimit(2)
                
                Button(action: {
                    // Configure action
                }) {
                    Text("Configure")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.blue)
                        .padding(.vertical, 4)
                }
            }
            
            Spacer()
            
            // Close button
            Button(action: {
                withAnimation(.easeOut(duration: 0.2)) {
                    showNotificationBanner = false
                }
            }) {
                Image(systemName: "xmark")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.secondary)
                    .frame(width: 24, height: 24)
            }
        }
        .padding(16)
        .background(Color(.secondarySystemBackground))
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
        VStack(spacing: 20) {
            Image(systemName: emptyStateIcon)
                .font(.system(size: 64))
                .foregroundColor(.wakevePrimary.opacity(0.3))
            
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
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                // Mark as Read button
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
                
                // Mark as Done button
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
            .padding(.bottom, 34) // Extra padding for safe area
            .background(
                Color(.systemBackground)
                    .ignoresSafeArea()
            )
        }
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
    
    // MARK: - Item List View
    
    private var itemListView: some View {
        LazyVStack(spacing: 0) {
            // Notification banner
            if showNotificationBanner && !isSelectionMode {
                notificationBanner
            }
            
            // Items list
            ForEach(filteredItems) { item in
                InboxGitHubStyleRow(
                    item: item,
                    isSelectionMode: isSelectionMode,
                    isSelected: selectedItemIds.contains(item.id)
                )
                .onTapGesture {
                    if isSelectionMode {
                        toggleSelection(for: item.id)
                    } else {
                        handleItemTap(item)
                    }
                }
                
                if item.id != filteredItems.last?.id {
                    Divider()
                        .padding(.leading, isSelectionMode ? 68 : 56)
                }
            }
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

// MARK: - Filter Tab Button

struct FilterTabButton: View {
    let title: String
    let isSelected: Bool
    var hasDropdown: Bool = false
    var badge: String? = nil
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Text(title)
                    .font(.system(size: 15, weight: isSelected ? .semibold : .regular))
                
                if let badge = badge {
                    Text(badge)
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.blue)
                        .cornerRadius(4)
                }
                
                if hasDropdown {
                    Image(systemName: "chevron.down")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(.secondary)
                }
            }
            .foregroundColor(isSelected ? .primary : .secondary)
            .padding(.vertical, 8)
        }
    }
}

// MARK: - Active Filter Indicator

struct ActiveFilterIndicator: View {
    let count: Int
    let action: () -> Void
    
    var body: some View {
        Menu {
            Section {
                Text(filterAppliedText)
                    .foregroundColor(.secondary)
            }
            
            Button(role: .destructive) {
                action()
            } label: {
                Text("Clear all filters")
            }
        } label: {
            HStack(spacing: 6) {
                Image(systemName: "line.3.horizontal.decrease")
                    .font(.system(size: 14))
                
                Text("\(count)")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 20, height: 20)
                    .background(Circle().fill(Color.blue))
            }
            .foregroundColor(.primary)
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
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Text(title)
                    .font(.system(size: 15, weight: isSelected ? .semibold : .regular))
                
                if hasDropdown {
                    Image(systemName: "chevron.down")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(.secondary)
                }
            }
            .foregroundColor(isSelected ? .primary : .secondary)
            .padding(.vertical, 8)
        }
    }
}

// MARK: - Event Filter Sheet

struct EventFilterSheet: View {
    let events: [String]
    @Binding var selectedEvent: String?
    let onSelect: () -> Void
    let onDismiss: () -> Void
    
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

// MARK: - GitHub Style Inbox Row

struct InboxGitHubStyleRow: View {
    let item: InboxItemModel
    var isSelectionMode: Bool = false
    var isSelected: Bool = false
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            if isSelectionMode {
                // Selection checkbox
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 22))
                    .foregroundColor(isSelected ? .blue : .secondary)
                    .padding(.top, 2)
            } else {
                // Status dot
                Circle()
                    .fill(item.statusColor)
                    .frame(width: 8, height: 8)
                    .padding(.top, 6)
            }
            
            // Content
            VStack(alignment: .leading, spacing: 4) {
                // Top row: repo/time
                HStack {
                    Text("\(item.repository) / \(item.repository)#\(item.number)")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    Text(item.timeAgo)
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }
                
                // Title row
                HStack(spacing: 8) {
                    Text(item.title)
                        .font(.system(size: 15, weight: item.isRead ? .regular : .semibold))
                        .foregroundColor(.primary)
                        .lineLimit(1)
                    
                    if item.commentCount > 0 {
                        Text("\(item.commentCount)")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 1)
                            .background(Color(.systemGray5))
                            .cornerRadius(10)
                    }
                }
                
                // Subtitle row
                HStack(spacing: 4) {
                    Image(systemName: "person.circle.fill")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                    
                    Text(item.message)
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(item.isRead || isSelectionMode ? Color.clear : Color.blue.opacity(0.03))
        .contentShape(Rectangle())
    }
}

// MARK: - Action Bar Button

struct ActionBarButton: View {
    let title: String
    let isEnabled: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 16, weight: .regular))
                .foregroundColor(.primary)
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

// MARK: - Legacy Inbox Item Row (kept for compatibility)

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
                }
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
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
        .opacity(item.isRead ? 0.85 : 1.0)
    }
}

// MARK: - Supporting Types

enum InboxFilter {
    case inbox, focused, unread, event
}

struct InboxItemModel: Identifiable, Equatable {
    let id: String
    var title: String
    var message: String
    var timeAgo: String
    var type: InboxItemType
    var isRead: Bool
    
    // GitHub-style properties
    var repository: String
    var number: Int
    var commentCount: Int
    var status: InboxItemStatus
    var isFocused: Bool
    var eventName: String?  // Optional event name for event filtering
    
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
    
    var statusColor: Color {
        switch status {
        case .open: return .green
        case .closed: return .red
        case .merged: return .purple
        case .draft: return .gray
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

enum InboxItemStatus {
    case open, closed, merged, draft
}

enum InboxItemType {
    case invitation, pollUpdate, comment, eventUpdate
}

// MARK: - Sample Data

private let sampleInboxItems: [InboxItemModel] = [
    InboxItemModel(
        id: "1",
        title: "Naming input with SavedModel format",
        message: "System information:",
        timeAgo: "17m",
        type: .invitation,
        isRead: false,
        repository: "tensorflow",
        number: 34070,
        commentCount: 1,
        status: .open,
        isFocused: true,
        eventName: "Week-end ski 2024"
    ),
    InboxItemModel(
        id: "2",
        title: "Upgrade Electron from v5 to v7",
        message: "Merged #8967 into development",
        timeAgo: "3h",
        type: .eventUpdate,
        isRead: true,
        repository: "desktop",
        number: 8967,
        commentCount: 29,
        status: .merged,
        isFocused: false,
        eventName: "Réunion famille"
    ),
    InboxItemModel(
        id: "3",
        title: "Adding docs for account creation",
        message: "This screenshot: https://github.co...",
        timeAgo: "1h",
        type: .comment,
        isRead: false,
        repository: "storybookjs",
        number: 8750,
        commentCount: 2,
        status: .open,
        isFocused: true,
        eventName: "Voyage Espagne"
    ),
    InboxItemModel(
        id: "4",
        title: "Prisma Client connection pool issue",
        message: "@matthewmueller commented",
        timeAgo: "2h",
        type: .pollUpdate,
        isRead: true,
        repository: "prisma",
        number: 5920,
        commentCount: 0,
        status: .open,
        isFocused: false,
        eventName: "Week-end montagne"
    )
]

// MARK: - Preview

#Preview("InboxView - Default") {
    InboxView(userId: "preview-user", onBack: {
        print("Back tapped")
    }, unreadCount: .constant(0))
}

#Preview("InboxView - Empty") {
    InboxView(userId: "preview-user", onBack: {
        print("Back tapped")
    }, unreadCount: .constant(0))
    .onAppear {
        // Simulate empty state
    }
}
