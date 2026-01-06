import SwiftUI
import Shared

// MARK: - Event Filter Enum
enum EventFilter: String, CaseIterable {
    case upcoming = "À venir"
    case inProgress = "En cours"
    case past = "Passés"
    
    var title: String {
        return self.rawValue
    }
}

// MARK: - Mock Event Status (Placeholder until we use real Shared.EventStatus)
enum MockEventStatus {
    case draft
    case polling
    case comparing
    case confirmed
    case organizing
    case finalized
}

// MARK: - Events Tab View
/// Main view for displaying and managing user events.
/// Uses Liquid Glass design system components for consistent UI.
///
/// ## Architecture
/// - **Functional Core**: Filtering and sorting logic in `filteredAndSortedEvents`
/// - **Imperative Shell**: UI state management and side effects
///
/// ## Components Used
/// - `LiquidGlassCard` for event rows
/// - `LiquidGlassBadge` for status indicators
/// - `LiquidGlassButton` for primary actions
/// - `LiquidGlassIconButton` for FAB
/// - `LiquidGlassDivider` for visual separation
struct EventsTabView: View {
    let userId: String
    
    // MARK: - State
    @State private var events: [MockEvent] = []
    @State private var selectedFilter: EventFilter = .upcoming
    @State private var isLoading = false
    @State private var showEventCreationSheet = false
    
    // Use persistent database-backed repository instead of in-memory mock
    private let repository: EventRepositoryInterface = RepositoryProvider.shared.repository
    
    // MARK: - Body
    var body: some View {
        NavigationStack {
            ZStack {
                VStack(spacing: 0) {
                    // Filter Segmented Control
                    filterSegmentedControl
                    
                    // Events List
                    eventsListContent
                }
                
                // Floating Action Button
                floatingActionButtonView
            }
            .background(Color(.systemBackground).ignoresSafeArea())
            .navigationTitle("Mes Événements")
            .navigationBarTitleDisplayMode(.inline)
            .refreshable {
                refreshEvents()
            }
            .onAppear {
                loadEvents()
            }
            .sheet(isPresented: $showEventCreationSheet) {
                CreateEventView(
                    userId: userId,
                    repository: repository,
                    onEventCreated: { eventId in
                        // Refresh events after creation
                        loadEvents()
                    }
                )
            }
        }
    }
    
    // MARK: - Filter Segmented Control
    /// Custom filter control with Liquid Glass styling
    private var filterSegmentedControl: some View {
        Picker("Filtres", selection: $selectedFilter) {
            ForEach(EventFilter.allCases, id: \.self) { filter in
                Text(filter.title)
                    .tag(filter)
            }
        }
        .pickerStyle(.segmented)
        .padding(.horizontal, 16)
        .padding(.vertical, 16)
        .background(Color(.systemBackground))
    }
    
    // MARK: - Events List Content
    @ViewBuilder
    private var eventsListContent: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(filteredAndSortedEvents, id: \.id) { event in
                    EventRowView(event: event)
                }
                
                // Empty State
                if filteredAndSortedEvents.isEmpty && !isLoading {
                    EventsEmptyStateView(onCreateEvent: {
                        showEventCreationSheet = true
                    })
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 16)
        }
    }
    
    // MARK: - Floating Action Button
    /// FAB using LiquidGlassIconButton component
    private var floatingActionButtonView: some View {
        VStack {
            Spacer()
            HStack {
                Spacer()
                LiquidGlassIconButton(
                    icon: "plus",
                    size: 56,
                    gradientColors: [.wakevPrimary, .wakevAccent]
                ) {
                    showEventCreationSheet = true
                }
                .accessibilityLabel("Créer un événement")
                .accessibilityHint("Ouvre la fenêtre de création d'événement")
            }
            .padding(.trailing, 20)
            .padding(.bottom, 20)
        }
    }
    
    // MARK: - Computed Properties
    
    /// Filters and sorts events based on selected filter.
    /// Pure function - no side effects.
    /// - Returns: Filtered and sorted array of events
    private var filteredAndSortedEvents: [MockEvent] {
        let filteredEvents = events.filter { event in
            switch selectedFilter {
            case .upcoming:
                // Filter upcoming events (future events)
                return event.date > Date()
            case .inProgress:
                // Filter in-progress events (events happening today)
                return Calendar.current.isDateInToday(event.date)
            case .past:
                // Filter past events
                return event.date < Date()
            }
        }
        
        // Sort by date (closest first)
        return filteredEvents.sorted { event1, event2 in
            event1.date < event2.date
        }
    }
    
    // MARK: - Data Loading
    
    /// Loads events from repository.
    /// Updates local state with fetched events.
    private func loadEvents() {
        isLoading = true
        
        // Simulate loading events
        events = [
            MockEvent(id: "1", title: "Réunion d'équipe", description: "Planification du prochain sprint", status: .polling, participantCount: 5, date: Date().addingTimeInterval(86400)), // Tomorrow
            MockEvent(id: "2", title: "Weekend de détente", description: "Sortie à la montagne", status: .confirmed, participantCount: 8, date: Date().addingTimeInterval(-86400)), // Yesterday
            MockEvent(id: "3", title: "Conférence annuelle", description: "Conférence technique", status: .confirmed, participantCount: 12, date: Date().addingTimeInterval(172800)) // In 2 days
        ]
        
        isLoading = false
    }
    
    /// Refreshes events list.
    private func refreshEvents() {
        loadEvents()
    }
}

// MARK: - Mock Event Model
struct MockEvent: Identifiable {
    let id: String
    let title: String
    let description: String
    let status: MockEventStatus
    let participantCount: Int
    let date: Date
}

// MARK: - Event Row View
/// Displays a single event row with Liquid Glass styling.
///
/// ## Components Used
/// - `LiquidGlassCard`: Main card container (replaces .glassCard)
/// - `LiquidGlassBadge`: Status badge (replaces inline statusBadge)
/// - Design system colors: .wakevPrimary, .wakevAccent
struct EventRowView: View {
    let event: MockEvent
    
    var body: some View {
        LiquidGlassCard(cornerRadius: 16, padding: 16) {
            VStack(alignment: .leading, spacing: 12) {
                // Event Header
                eventHeader
                
                // Divider
                LiquidGlassDivider(style: .subtle)
                    .padding(.vertical, 4)
                
                // Participants and Date
                eventMetadata
            }
        }
        .accessibilityLabel("\(event.title)")
        .accessibilityHint("\(event.participantCount) participants, \(formattedDate)")
    }
    
    // MARK: - Event Header
    private var eventHeader: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(event.title)
                    .font(.headline)
                    .foregroundColor(.primary)
                
                if !event.description.isEmpty {
                    Text(event.description)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
            }
            
            Spacer()
            
            // Status Badge using LiquidGlassBadge component
            LiquidGlassBadge.from(status: event.status)
        }
    }
    
    // MARK: - Event Metadata
    private var eventMetadata: some View {
        HStack(spacing: 16) {
            // Participant count
            HStack(spacing: 4) {
                Image(systemName: "person.2")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.secondary)
                
                Text("\(event.participantCount) participants")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            // Date
            HStack(spacing: 4) {
                Image(systemName: "calendar")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.secondary)
                
                Text(formattedDate)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
    
    // MARK: - Formatted Date
    private var formattedDate: String {
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        return formatter.string(from: event.date)
    }
}

// MARK: - Events Empty State View
/// Empty state displayed when no events are available.
/// Uses LiquidGlassButton for the call-to-action.
///
/// ## Components Used
/// - `LiquidGlassButton`: Primary action button
private struct EventsEmptyStateView: View {
    let onCreateEvent: () -> Void
    
    @State private var isAnimating = false
    
    var body: some View {
        VStack(spacing: 24) {
            // Animated icon
            Image(systemName: "calendar.badge.exclamationmark")
                .font(.system(size: 54, weight: .light))
                .foregroundColor(.wakevPrimary)
                .scaleEffect(isAnimating ? 1.05 : 1.0)
                .animation(
                    Animation.spring(response: 1.5, dampingFraction: 0.6)
                        .repeatForever(autoreverses: true),
                    value: isAnimating
                )
                .onAppear { isAnimating = true }
            
            // Text content
            VStack(spacing: 8) {
                Text("Aucun événement")
                    .font(.title2.weight(.semibold))
                    .foregroundColor(.primary)
                
                Text("Créez votre premier événement\npour commencer")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
            }
            
            // Create event button using LiquidGlassButton
            LiquidGlassButton(
                title: "Créer un événement",
                style: .primary
            ) {
                onCreateEvent()
            }
            .padding(.horizontal)
            .padding(.top, 8)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 60)
    }
}

// MARK: - Preview
#Preview {
    EventsTabView(
        userId: "user123"
    )
}
