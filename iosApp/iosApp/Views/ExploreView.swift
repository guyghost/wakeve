import SwiftUI
import Shared

/// Explore tab view - discover events and features
///
/// Features:
/// - Featured events
/// - Recommended events
/// - Discover new features
/// - Search functionality
struct ExploreView: View {
    let userId: String
    let repository: EventRepositoryInterface

    @State private var searchText = ""
    @State private var events: [Event] = []
    @State private var isLoading = true
    @State private var selectedCategory: ExploreCategory = .all

    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemBackground)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Search Bar
                        LiquidGlassTextField(
                            title: nil,
                            placeholder: "Rechercher...",
                            text: $searchText,
                            leftIcon: "magnifyingglass",
                            leftIconAction: {}
                        )
                        .onChange(of: searchText) { _, newValue in
                            performSearch(newValue)
                        }

                        // Category Filter
                        CategoryPicker(selectedCategory: $selectedCategory)

                        // Featured Events
                        if !isLoading && !events.isEmpty() {
                            VStack(spacing: 16) {
                                SectionHeader(title: "Événements en vedette")

                                LazyVStack(spacing: 16) {
                                    ForEach(events.prefix(3), id: \.id) { event in
                                        LiquidGlassCard {
                                            ExploreEventCardContent(event: event)
                                        }
                                        .onTapGesture {
                                            // Navigate to event detail
                                        }
                                    }
                                }

                                // See More Button
                                LiquidGlassButton(
                                    title: "Voir plus",
                                    style: .text,
                                    icon: "chevron.right",
                                    action: {
                                        // Show all events
                                    }
                                )
                            }
                            .padding(.horizontal, 16)

                            LiquidGlassDivider(style: .thin)
                                .padding(.leading, 16)

                            // Recommended Events
                            VStack(spacing: 16) {
                                SectionHeader(title: "Recommandés pour vous")

                                LazyVStack(spacing: 16) {
                                    ForEach(events.suffix(from: events.count > 3 ? 3 : 0), id: \.id) { event in
                                        LiquidGlassCard {
                                            ExploreEventCardContent(event: event)
                                        }
                                        .onTapGesture {
                                            // Navigate to event detail
                                        }
                                    }
                                }
                            }
                            .padding(.horizontal, 16)
                        } else if isLoading {
                            LoadingView()
                        } else {
                            EmptyStateView()
                        }
                    }
                    .padding(.top, 12)
                }
            }
            .navigationTitle("Explorer")
            .navigationBarTitleDisplayMode(.large)
        }
        .onAppear {
            loadEvents()
        }
    }

    // MARK: - Data Loading

    private func loadEvents() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            events = repository.getAllEvents()
            isLoading = false
        }
    }

    private func performSearch(_ query: String) {
        // Filter events by title or description
        if query.isEmpty {
            events = repository.getAllEvents()
        } else {
            events = repository.getAllEvents().filter { event in
                event.title.localizedCaseInsensitiveContains(query) ||
                event.description.localizedCaseInsensitiveContains(query)
            }
        }
    }
}

// MARK: - Explore Event Card Content

/// Content for event cards inside LiquidGlassCard
struct ExploreEventCardContent: View {
    let event: Event

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Hero Image
            Group {
                if let heroImage = event.heroImageUrl {
                    AsyncImage(url: URL(string: heroImage)) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Rectangle()
                            .fill(Color.wakevSurfaceLight)
                    }
                } else {
                    // Fallback gradient
                    Rectangle()
                        .fill(
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    Color.wakevPrimary,
                                    Color.wakevAccent
                                ]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                }
            }
            .frame(height: 160)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            // Event Info
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    EventStatusBadge(status: event.status)

                    Spacer()

                    if !event.participants.isEmpty() {
                        HStack(spacing: 4) {
                            Image(systemName: "person.2")
                                .font(.system(size: 14))
                                .foregroundColor(.secondary)

                            Text("\(event.participants.count)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                Text(event.title)
                    .font(.headline.weight(.semibold))
                    .foregroundColor(.primary)
                    .lineLimit(2)

                if !event.description.isEmpty() {
                    Text(event.description)
                        .font(.body)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
            }
        }
        .accessibilityLabel(event.title)
    }
}

// MARK: - Event Status Badge

/// Badge displaying event status using LiquidGlassBadge
struct EventStatusBadge: View {
    let status: EventStatus

    var body: some View {
        LiquidGlassBadge(
            text: statusText,
            icon: statusIcon,
            type: statusBadgeType,
            size: .small
        )
    }

    private var statusText: String {
        switch status {
        case .draft: return "Brouillon"
        case .polling: return "Sondage"
        case .comparing: return "Comparaison"
        case .confirmed: return "Confirmé"
        case .organizing: return "Organisation"
        case .finalized: return "Finalisé"
        }
    }

    private var statusIcon: String {
        switch status {
        case .draft: return "doc.fill"
        case .polling: return "chart.bar.fill"
        case .comparing: return "arrow.left.arrow.right"
        case .confirmed: return "checkmark.circle.fill"
        case .organizing: return "calendar.badge.clock"
        case .finalized: return "checkmark.seal.fill"
        }
    }

    private var statusBadgeType: LiquidGlassBadge.BadgeType {
        switch status {
        case .draft: return .primary
        case .polling: return .accent
        case .comparing: return .warning
        case .confirmed: return .success
        case .organizing: return .primary
        case .finalized: return .success
        }
    }
}

// MARK: - Category Picker

enum ExploreCategory: String, CaseIterable {
    case all = "Tous"
    case social = "Social"
    case professional = "Professionnel"
    case sport = "Sport"
    case culture = "Culture"

    var title: String {
        return self.rawValue
    }

    var iconName: String {
        switch self {
        case .all: return "square.grid.2x2.fill"
        case .social: return "person.2.fill"
        case .professional: return "briefcase.fill"
        case .sport: return "figure.run"
        case .culture: return "theatermasks.fill"
        }
    }
}

struct CategoryPicker: View {
    @Binding var selectedCategory: ExploreCategory

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(ExploreCategory.allCases, id: \.self) { category in
                    LiquidGlassButton(
                        title: category.title,
                        icon: category.iconName,
                        style: selectedCategory == category ? .primary : .secondary,
                        size: .small,
                        action: {
                            // Toggle category
                        }
                    )
                    .accessibilityLabel(category.title)
                    .accessibilityAddTraits(selectedCategory == category ? .isSelected : [])
                }
            }
            .padding(.horizontal, 16)
        }
        .padding(.vertical, 12)
    }
}

// MARK: - Section Header

struct SectionHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(.title3.weight(.bold))
            .foregroundColor(.primary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
    }
}

// MARK: - Loading View

struct LoadingView: View {
    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: .wakevPrimary))
                .scaleEffect(1.3)

            Text("Chargement...")
                .font(.body.weight(.medium))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Empty State View

struct EmptyStateView: View {
    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 48))
                .foregroundColor(.secondary)

            Text("Aucun événement trouvé")
                .font(.title2.weight(.semibold))
                .foregroundColor(.primary)

            Text("Essayez une autre recherche ou catégorie")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 60)
    }
}

// MARK: - Preview

#Preview("Explore View") {
    ExploreView(
        userId: "user123",
        repository: MockEventRepository()
    )
}

// MARK: - Mock Repository for Preview

struct MockEventRepository: EventRepositoryInterface {
    func getAllEvents() -> [Event] {
        return [
            Event(
                id: "1",
                title: "Week-end à la montagne",
                description: "Un week-end détente entre amis dans les Alpes",
                status: .confirmed,
                organizerId: "user1",
                createdAt: "2024-01-15T10:00:00Z",
                finalDate: nil,
                minParticipants: nil,
                maxParticipants: nil,
                expectedParticipants: 8,
                heroImageUrl: nil,
                eventType: .OTHER,
                eventTypeCustom: nil,
                potentialLocations: [],
                proposedSlots: [],
                participants: [],
                scenarios: [],
                budgets: [],
                syncStatus: .synced
            ),
            Event(
                id: "2",
                title: "Conférence Tech 2024",
                description: "La plus grande conférence tech de l'année",
                status: .polling,
                organizerId: "user2",
                createdAt: "2024-01-10T09:00:00Z",
                finalDate: nil,
                minParticipants: nil,
                maxParticipants: nil,
                expectedParticipants: 50,
                heroImageUrl: nil,
                eventType: .CONFERENCE,
                eventTypeCustom: nil,
                potentialLocations: [],
                proposedSlots: [],
                participants: [],
                scenarios: [],
                budgets: [],
                syncStatus: .synced
            )
        ]
    }

    func getEvent(id: String) -> Event? {
        return getAllEvents().first { $0.id == id }
    }

    func createEvent(event: Event) -> Event {
        return event
    }

    func updateEvent(event: Event) -> Event {
        return event
    }

    func deleteEvent(id: String) {}

    func getParticipants(eventId: String) -> [Participant] {
        return []
    }

    func addParticipant(eventId: String, participant: Participant) -> Participant {
        return participant
    }

    func getProposedSlots(eventId: String) -> [ProposedSlot] {
        return []
    }

    func addProposedSlot(eventId: String, slot: ProposedSlot) -> ProposedSlot {
        return slot
    }

    func vote(slotId: String, participantId: String, voteType: VoteType) -> Vote {
        return Vote(id: UUID().uuidString, participantId: participantId, slotId: slotId, voteType: voteType, timestamp: Date().ISO8601String())
    }
}
