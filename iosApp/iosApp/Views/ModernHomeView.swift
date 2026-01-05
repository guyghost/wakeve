import SwiftUI
import SwiftUI
import Shared

// MARK: - Event Filter Enum

/// Filter options for events list (matches Android's TabRow filters)
enum ModernEventFilter: String, CaseIterable {
    case all = "Tous"
    case upcoming = "À venir"
    case past = "Passés"

    var title: String {
        return self.rawValue
    }
}

// MARK: - Modern Home View

/// Modern home view inspired by Apple Invites
/// Features: Card-based event display, functional filters, clean typography
struct ModernHomeView: View {
    let userId: String
    let repository: EventRepositoryInterface
    let onEventSelected: (Event) -> Void
    let onCreateEvent: () -> Void

    @State private var events: [Event] = []
    @State private var isLoading = true
    @State private var selectedFilter: ModernEventFilter = .upcoming

    var body: some View {
        ZStack {
            // System background adapts to light/dark mode
            Color(.systemBackground)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Filter Picker
                VStack(spacing: 0) {
                    EventFilterPicker(
                        selectedFilter: $selectedFilter
                    )
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)

                    Divider()
                }
                .background(Color(.systemBackground))

                // Content
                if isLoading {
                    LoadingEventsView()
                } else if filteredEvents.isEmpty {
                    AppleInvitesEmptyState(onCreateEvent: onCreateEvent)
                } else {
                    ScrollView {
                        VStack(spacing: 16) {
                            // Event Cards
                            ForEach(filteredEvents, id: \.id) { event in
                                ModernEventCard(
                                    event: event,
                                    onTap: { onEventSelected(event) }
                                )
                            }

                            // Add Event Card
                            AddEventCard(onTap: onCreateEvent)

                            Spacer()
                                .frame(height: 40)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                    }
                }
            }
        }
        .onAppear {
            loadEvents()
        }
    }

    // MARK: - Computed Properties

    /// Filter events based on selected filter and dates
    private var filteredEvents: [Event] {
        let now = Date()

        return events.filter { event in
            // Get relevant date for filtering
            let eventDate = getEventDate(event)

            // Apply filter
            switch selectedFilter {
            case .all:
                return true
            case .upcoming:
                return eventDate > now
            case .past:
                return eventDate <= now
            }
        }
        // Sort by date (most recent first)
        .sorted { event1, event2 in
            let date1 = getEventDate(event1)
            let date2 = getEventDate(event2)
            return date1 < date2
        }
    }

    /// Helper to get relevant date from an event for filtering
    private func getEventDate(_ event: Event) -> Date {
        let formatter = ISO8601DateFormatter()

        // Prefer finalDate
        if let finalDateStr = event.finalDate, let finalDate = formatter.date(from: finalDateStr) {
            return finalDate
        }

        // Fall back to deadline
        if let deadline = formatter.date(from: event.deadline) {
            return deadline
        }

        // Final fallback to createdAt
        if let createdAt = formatter.date(from: event.createdAt) {
            return createdAt
        }

        return Date()
    }

    // MARK: - Data Loading

    private func loadEvents() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            events = repository.getAllEvents()
            isLoading = false
        }
    }
}

// MARK: - Event Filter Picker

/// Segmented control for filtering events (All, Upcoming, Past)
/// Matches Android's TabRow filtering behavior
struct EventFilterPicker: View {
    @Binding var selectedFilter: ModernEventFilter

    var body: some View {
        Picker("Filtre", selection: $selectedFilter) {
            ForEach(ModernEventFilter.allCases, id: \.self) { filter in
                Text(filter.title)
                    .tag(filter)
            }
        }
        .pickerStyle(.segmented)
        .accessibilityLabel("Filtre d'événements")
        .accessibilityValue(selectedFilter.title)
    }
}

// MARK: - Apple Invites Empty State

/// Empty state when no events match the current filter
struct AppleInvitesEmptyState: View {
    let onCreateEvent: () -> Void

    @State private var isAnimating = false

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: 24) {
                // Calendar icon with animation
                ZStack {
                    Circle()
                        .fill(Color.wakevPrimary.opacity(0.15))
                        .frame(width: 80, height: 80)

                    Image(systemName: "calendar")
                        .font(.system(size: 40, weight: .light))
                        .foregroundColor(.wakevPrimary)
                        .scaleEffect(isAnimating ? 1.05 : 1.0)
                        .animation(
                            Animation.spring(response: 1.5, dampingFraction: 0.6)
                                .repeatForever(autoreverses: true),
                            value: isAnimating
                        )
                }
                .onAppear { isAnimating = true }

                // Text content
                VStack(spacing: 12) {
                    Text("Aucun événement")
                        .font(.title2.weight(.bold))
                        .foregroundColor(.primary)

                    Text("Créez votre premier événement\npour commencer")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .lineSpacing(2)
                }

                // Create Event button
                Button(action: onCreateEvent) {
                    HStack(spacing: 8) {
                        Image(systemName: "plus.circle.fill")
                        Text("Créer un événement")
                    }
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.wakevPrimary,
                                Color.wakevAccent
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .continuousCornerRadius(12)
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }
            .padding(.horizontal, 24)

            Spacer()
        }
    }
}

// MARK: - Modern Event Card

/// Card displaying event information with gradient background
struct ModernEventCard: View {
    let event: Event
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            LiquidGlassCard.thick(cornerRadius: 20, padding: 0) {
                ZStack {
                    // Background Gradient
                    RoundedRectangle(cornerRadius: 20)
                        .fill(
                            LinearGradient(
                                colors: gradientColors,
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .overlay(
                            // Pattern overlay
                            GeometryReader { geometry in
                                Image(systemName: "calendar")
                                    .font(.system(size: 200))
                                    .foregroundColor(.white.opacity(0.05))
                                    .offset(x: geometry.size.width * 0.6, y: -50)
                            }
                        )

                    // Content Overlay
                    VStack {
                        // Top Bar with status badge
                        HStack {
                            EventStatusBadge(status: event.status)
                            Spacer()
                        }
                        .padding(20)

                        Spacer()

                        // Bottom Content
                        VStack(alignment: .leading, spacing: 12) {
                            // Participant Avatars
                            if !event.participants.isEmpty {
                                HStack(spacing: -8) {
                                    ForEach(event.participants.prefix(5), id: \.self) { participant in
                                        ParticipantAvatar(participantId: participant)
                                    }

                                    if event.participants.count > 5 {
                                        AdditionalParticipantsCount(count: event.participants.count - 5)
                                    }
                                }
                                .padding(.bottom, 4)
                            }

                            // Event Info
                            VStack(alignment: .leading, spacing: 4) {
                                Text(event.title)
                                    .font(.title.weight(.bold))
                                    .foregroundColor(.white)
                                    .lineLimit(2)

                                if let finalDate = event.finalDate {
                                    Text(formatEventDate(finalDate))
                                        .font(.subheadline.weight(.medium))
                                        .foregroundColor(.white.opacity(0.9))
                                } else {
                                    Text(formatDeadline(event.deadline))
                                        .font(.subheadline.weight(.medium))
                                        .foregroundColor(.white.opacity(0.9))
                                }

                                if !event.description.isEmpty {
                                    Text(event.description)
                                        .font(.caption)
                                        .foregroundColor(.white.opacity(0.8))
                                        .lineLimit(2)
                                        .padding(.top, 2)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(20)
                        .background(
                            LinearGradient(
                                colors: [Color.black.opacity(0), Color.black.opacity(0.7)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                    }
                }
                .frame(height: 380)
            }
        }
        .buttonStyle(ScaleButtonStyle())
    }

    private var gradientColors: [Color] {
        switch event.status {
        case .draft:
            return [Color.orange, Color.red]
        case .polling:
            return [Color.blue, Color.purple]
        case .confirmed:
            return [Color.green, Color.teal]
        default:
            return [Color.gray, Color.gray.opacity(0.7)]
        }
    }

    private func formatEventDate(_ dateString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.dateFormat = "EEEE, MMM d, h:mm a"
            return formatter.string(from: date)
        }
        return dateString
    }

    private func formatDeadline(_ deadlineString: String?) -> String {
        guard let deadlineString = deadlineString,
              let date = ISO8601DateFormatter().date(from: deadlineString) else {
            return "No deadline set"
        }
        let formatter = DateFormatter()
        formatter.dateFormat = "Deadline: MMM d"
        return formatter.string(from: date)
    }
}

// MARK: - Add Event Card

/// Card button to create a new event
struct AddEventCard: View {
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            LiquidGlassCard(cornerRadius: 20, padding: 0) {
                VStack(spacing: 12) {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: 44, weight: .light))
                        .foregroundColor(.wakevPrimary)

                    Text("Créer un événement")
                        .font(.headline.weight(.semibold))
                        .foregroundColor(.primary)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 160)
            }
        }
        .buttonStyle(ScaleButtonStyle())
        .accessibilityLabel("Créer un nouvel événement")
    }
}

// MARK: - Event Status Badge

/// Badge displaying event status
struct EventStatusBadge: View {
    let status: EventStatus

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: statusIcon)
                .font(.system(size: 12, weight: .semibold))

            Text(statusText)
                .font(.caption.weight(.semibold))
        }
        .foregroundColor(.white)
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .ultraThinGlass(cornerRadius: 12)
    }

    private var statusText: String {
        switch status {
        case .draft: return "Draft"
        case .polling: return "Polling"
        case .confirmed: return "Confirmed"
        default: return ""
        }
    }

    private var statusIcon: String {
        switch status {
        case .draft: return "doc"
        case .polling: return "chart.bar"
        case .confirmed: return "checkmark.circle"
        default: return "questionmark"
        }
    }
}

// MARK: - Participant Avatar

/// Avatar showing participant initial
struct ParticipantAvatar: View {
    let participantId: String

    private var initials: String {
        String(participantId.prefix(1).uppercased())
    }

    private var avatarColor: Color {
        let colors: [Color] = [
            .wakevError,
            .wakevWarning,
            .wakevSuccess,
            .wakevPrimary,
            .wakevAccent,
            .wakevSuccessDark
        ]
        let hash = abs(participantId.hashValue)
        return colors[hash % colors.count]
    }

    var body: some View {
        Circle()
            .fill(avatarColor)
            .frame(width: 40, height: 40)
            .overlay(
                Text(initials)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
            )
            .overlay(
                Circle()
                    .stroke(Color.white, lineWidth: 2)
            )
    }
}

/// Badge showing additional participants count
struct AdditionalParticipantsCount: View {
    let count: Int

    var body: some View {
        ZStack {
            Circle()
                .fill(.ultraThinMaterial)

            Text("+\(count)")
                .font(.caption.weight(.semibold))
                .foregroundColor(.white)
        }
        .frame(width: 40, height: 40)
        .overlay(
            Circle()
                .stroke(Color.white, lineWidth: 2)
        )
    }
}

// MARK: - Loading View

/// View shown while loading events
struct LoadingEventsView: View {
    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: .wakevPrimary))
                .scaleEffect(1.3)

            Text("Chargement des événements...")
                .font(.body.weight(.medium))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Button Styles

/// Button style with scale animation on press
struct ScaleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

// MARK: - Preview

#Preview("Modern Home View") {
    ModernHomeView(
        userId: "user123",
        repository: MockEventRepository(),
        onEventSelected: { event in
            print("Selected: \(event.title)")
        },
        onCreateEvent: {
            print("Create event")
        }
    )
}

// MARK: - Mock Repository for Preview

/// Mock repository for SwiftUI Preview
class MockEventRepository: EventRepositoryInterface {
    func getAllEvents() -> [Event] {
        let formatter = ISO8601DateFormatter()
        let now = Date()

        return [
            Event(
                id: "event-1",
                title: "Réunion d'équipe",
                description: "Planification du prochain sprint",
                organizerId: "user1",
                participants: ["user1", "user2", "user3"],
                proposedSlots: [],
                deadline: formatter.string(from: now.addingTimeInterval(86400)),
                status: .polling,
                finalDate: nil,
                createdAt: formatter.string(from: now.addingTimeInterval(-86400)),
                updatedAt: formatter.string(from: now),
                eventType: .custom,
                eventTypeCustom: "meeting",
                minParticipants: 2,
                maxParticipants: 10,
                expectedParticipants: 3,
                heroImageUrl: nil
            ),
            Event(
                id: "event-2",
                title: "Weekend de détente",
                description: "Sortie à la montagne",
                organizerId: "user1",
                participants: ["user1", "user2", "user3", "user4", "user5", "user6"],
                proposedSlots: [],
                deadline: formatter.string(from: now.addingTimeInterval(-86400)),
                status: .confirmed,
                finalDate: formatter.string(from: now.addingTimeInterval(172800)),
                createdAt: formatter.string(from: now.addingTimeInterval(-172800)),
                updatedAt: formatter.string(from: now),
                eventType: .custom,
                eventTypeCustom: "social",
                minParticipants: 3,
                maxParticipants: 15,
                expectedParticipants: 6,
                heroImageUrl: nil
            )
        ]
    }

    func getEvent(id: String) -> Event? {
        return getAllEvents().first { $0.id == id }
    }

    func createEvent(event: Event) async throws -> Any? {
        return event
    }

    func updateEvent(event: Event) async throws -> Any? {
        return event
    }

    func deleteEvent(id: String) async throws -> Bool {
        return true
    }

    func getParticipants(eventId: String) -> [String]? {
        return []
    }

    func addParticipant(eventId: String, participantId: String) async throws -> Any? {
        return true
    }

    func removeParticipant(eventId: String, participantId: String) async throws -> Bool {
        return true
    }

    func getPollVotes(eventId: String) -> [String : [String : Vote]] {
        return [:]
    }

    func submitVote(eventId: String, slotId: String, participantId: String, response: Vote) async throws -> Bool {
        return true
    }

    func addVote(eventId: String, participantId: String, slotId: String, vote: Vote) async throws -> Any? {
        return true
    }

    func canModifyEvent(eventId: String, userId: String) -> Bool {
        return true
    }

    func getPoll(eventId: String) -> Poll? {
        return nil
    }

    func isDeadlinePassed(deadline: String) -> Bool {
        return false
    }

    func isOrganizer(eventId: String, userId: String) -> Bool {
        return true
    }

    func updateEventStatus(id: String, status: EventStatus, finalDate: String?) async throws -> Any? {
        return true
    }
}
