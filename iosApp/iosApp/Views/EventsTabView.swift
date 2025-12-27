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
struct EventsTabView: View {
    let userId: String
    
    // State
    @State private var events: [MockEvent] = []
    @State private var selectedFilter: EventFilter = .upcoming
    @State private var isLoading = false
    @State private var showEventCreationSheet = false
    
    // Repository for event creation
    @State private var repository = EventRepository()
    
    var body: some View {
        NavigationStack {
            ZStack {
                VStack(spacing: 0) {
                    // Filter Segmented Control
                    Picker("Filtres", selection: $selectedFilter) {
                        ForEach(EventFilter.allCases, id: \.self) { filter in
                            Text(filter.title)
                                .tag(filter)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal)
                    .padding(.top, 20)
                    
                    // Events List
                    ScrollView {
                        LazyVStack(spacing: 16) {
                            ForEach(filteredAndSortedEvents, id: \.id) { event in
                                EventRowView(event: event)
                            }
                            
                            // Empty State
                            if filteredAndSortedEvents.isEmpty && !isLoading {
                                EmptyStateView(onCreateEvent: {
                                    showEventCreationSheet = true
                                })
                            }
                        }
                        .padding(.horizontal)
                        .padding(.vertical, 16)
                    }
                }
                
                // Floating Action Button
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        floatingActionButton
                    }
                    .padding(.trailing, 20)
                    .padding(.bottom, 20)
                }
            }
            .background(
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color.blue.opacity(0.1),
                        Color.purple.opacity(0.1)
                    ]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()
            )
            .navigationTitle("Mes Événements")
            .refreshable {
                refreshEvents()
            }
            .onAppear {
                loadEvents()
            }
            .sheet(isPresented: $showEventCreationSheet) {
                EventCreationSheet(
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
    
    // MARK: - Floating Action Button
    
    private var floatingActionButton: some View {
        Button {
            showEventCreationSheet = true
        } label: {
            Image(systemName: "plus")
                .font(.system(size: 24, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: 56, height: 56)
                .background(
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [Color.blue, Color.purple],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .shadow(color: .blue.opacity(0.4), radius: 8, x: 0, y: 4)
                )
        }
        .accessibilityLabel("Créer un événement")
        .accessibilityHint("Ouvre la fenêtre de création d'événement")
    }
    
    // MARK: - Computed Properties
    
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
struct EventRowView: View {
    let event: MockEvent
    
    var body: some View {
        // Using a simple card style since we can't access LiquidGlassCard
        ZStack {
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white)
                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 4)
            
            VStack(alignment: .leading, spacing: 12) {
                // Event Header
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(event.title)
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.primary)
                        
                        if !event.description.isEmpty {
                            Text(event.description)
                                .font(.system(size: 14))
                                .foregroundColor(.secondary)
                                .lineLimit(2)
                        }
                    }
                    
                    Spacer()
                    
                    // Status Badge
                    statusBadge
                }
                
                // Participants and Date
                HStack(spacing: 16) {
                    HStack(spacing: 4) {
                        Image(systemName: "person.2")
                            .font(.system(size: 14))
                            .foregroundColor(.secondary)
                        
                        Text("\(event.participantCount) participants")
                            .font(.system(size: 14))
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    HStack(spacing: 4) {
                        Image(systemName: "calendar")
                            .font(.system(size: 14))
                            .foregroundColor(.secondary)
                        
                        Text(formattedDate)
                            .font(.system(size: 14))
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding()
        }
    }
    
    private var statusBadge: some View {
        HStack(spacing: 4) {
            Circle()
                .fill(statusColor)
                .frame(width: 8, height: 8)
            
            Text(statusText)
                .font(.system(size: 12, weight: .medium))
                .foregroundColor(statusColor)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(statusColor.opacity(0.1))
        .cornerRadius(12)
    }
    
    private var statusText: String {
        switch event.status {
        case .draft: return "Brouillon"
        case .polling: return "Sondage"
        case .comparing: return "Comparaison"
        case .confirmed: return "Confirmé"
        case .organizing: return "Organisation"
        case .finalized: return "Finalisé"
        }
    }
    
    private var statusColor: Color {
        switch event.status {
        case .draft: return .orange
        case .polling: return .blue
        case .comparing: return .purple
        case .confirmed: return .green
        case .organizing: return .yellow
        case .finalized: return .green
        }
    }
    
    private var formattedDate: String {
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        return formatter.string(from: event.date)
    }
}

// MARK: - Empty State View
struct EmptyStateView: View {
    let onCreateEvent: () -> Void
    
    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "calendar.badge.exclamationmark")
                .font(.system(size: 64))
                .foregroundColor(.secondary)
            
            VStack(spacing: 8) {
                Text("Aucun événement")
                    .font(.title2)
                    .fontWeight(.semibold)
                
                Text("Vous n'avez aucun événement correspondant à ce filtre")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            
            Button("Créer un événement") {
                onCreateEvent()
            }
            .buttonStyle(.borderedProminent)
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