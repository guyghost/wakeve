import SwiftUI
import Shared

/// ViewModel for the ExploreTabView.
///
/// Manages search, filtering, and discovery of events.
/// Calls the DatabaseEventRepository search/trending/nearby/recommended methods.
@MainActor
class ExploreViewModel: ObservableObject {
    // MARK: - Published Properties

    /// Search text entered by the user
    @Published var searchText: String = ""

    /// Currently selected category filter
    @Published var selectedCategory: EventCategoryItem = .all

    /// Trending events
    @Published var trendingEvents: [ExploreEventItem] = []

    /// Nearby events
    @Published var nearbyEvents: [ExploreEventItem] = []

    /// Recommended events
    @Published var recommendedEvents: [ExploreEventItem] = []

    /// Search results (shown when search text is non-empty)
    @Published var searchResults: [ExploreEventItem] = []

    /// Whether a search/load is in progress
    @Published var isLoading: Bool = false

    /// Whether a refresh is in progress (pull-to-refresh)
    @Published var isRefreshing: Bool = false

    /// Error message if any
    @Published var errorMessage: String?

    /// Whether search results are currently shown
    var isSearching: Bool {
        !searchText.trimmingCharacters(in: .whitespaces).isEmpty
    }

    // MARK: - Private Properties

    private let repository: DatabaseEventRepository

    // MARK: - Initialization

    init() {
        self.repository = RepositoryProvider.shared.databaseRepository
        loadAll()
    }

    // MARK: - Public Methods

    /// Load all discovery sections (trending, nearby, recommended)
    func loadAll() {
        isLoading = true
        errorMessage = nil

        Task {
            await loadTrending()
            await loadRecommended()
            isLoading = false
        }
    }

    /// Refresh all data (pull-to-refresh)
    func refresh() {
        isRefreshing = true
        errorMessage = nil

        Task {
            await loadTrending()
            await loadRecommended()
            if isSearching {
                await performSearch()
            }
            isRefreshing = false
        }
    }

    /// Perform search when search text changes
    func search() {
        guard isSearching else {
            searchResults = []
            return
        }

        Task {
            await performSearch()
        }
    }

    /// Update the selected category and re-search if needed
    func selectCategory(_ category: EventCategoryItem) {
        selectedCategory = category
        if isSearching {
            search()
        } else {
            // Filter trending/recommended by category locally
            loadAll()
        }
    }

    // MARK: - Private Methods

    private func loadTrending() async {
        do {
            let response = repository.getTrendingEvents(limit: 10)
            let items = response.events.map { mapToExploreItem($0) }
            await MainActor.run {
                self.trendingEvents = items
            }
        } catch {
            await MainActor.run {
                self.errorMessage = error.localizedDescription
            }
        }
    }

    private func loadRecommended() async {
        // Use a placeholder userId. In production, this comes from auth.
        do {
            let response = repository.getRecommendedEvents(userId: "current_user", limit: 10)
            let items = response.events.map { mapToExploreItem($0) }
            await MainActor.run {
                self.recommendedEvents = items
            }
        } catch {
            await MainActor.run {
                self.errorMessage = error.localizedDescription
            }
        }
    }

    private func performSearch() async {
        let query = searchText.trimmingCharacters(in: .whitespaces)
        let categoryFilter: String? = selectedCategory == .all ? nil : selectedCategory.eventTypes.first

        do {
            let response = repository.searchEvents(
                query: query.isEmpty ? nil : query,
                category: categoryFilter,
                location: nil,
                dateFrom: nil,
                dateTo: nil,
                status: nil,
                sortBy: "RELEVANCE",
                offset: 0,
                limit: 30
            )
            let items = response.events.map { mapToExploreItem($0) }
            await MainActor.run {
                self.searchResults = items
            }
        } catch {
            await MainActor.run {
                self.errorMessage = error.localizedDescription
            }
        }
    }

    /// Map from shared EventSearchResult to local display model
    private func mapToExploreItem(_ result: EventSearchResult) -> ExploreEventItem {
        return ExploreEventItem(
            id: result.id,
            title: result.title,
            description: result.description_,
            eventType: result.eventType,
            participantCount: Int(result.participantCount),
            maxParticipants: result.maxParticipants?.intValue,
            status: result.status,
            locationName: result.locationName,
            deadline: result.deadline
        )
    }
}

// MARK: - Display Models

/// A lightweight event item for display in the explore UI.
struct ExploreEventItem: Identifiable, Equatable {
    let id: String
    let title: String
    let description: String
    let eventType: String
    let participantCount: Int
    let maxParticipants: Int?
    let status: String
    let locationName: String?
    let deadline: String

    /// Icon for the event type
    var icon: String {
        switch eventType {
        case "BIRTHDAY": return "cake.fill"
        case "WEDDING": return "heart.fill"
        case "TEAM_BUILDING": return "person.3.fill"
        case "CONFERENCE": return "building.2.fill"
        case "WORKSHOP": return "wrench.and.screwdriver.fill"
        case "PARTY": return "party.popper.fill"
        case "SPORTS_EVENT", "SPORT_EVENT": return "figure.run"
        case "CULTURAL_EVENT": return "theatermasks.fill"
        case "FAMILY_GATHERING": return "house.fill"
        case "OUTDOOR_ACTIVITY": return "leaf.fill"
        case "FOOD_TASTING": return "fork.knife"
        case "TECH_MEETUP": return "laptopcomputer"
        case "WELLNESS_EVENT": return "heart.circle.fill"
        case "CREATIVE_WORKSHOP": return "paintpalette.fill"
        default: return "calendar"
        }
    }

    /// Localized display name for the event type
    var eventTypeDisplayName: String {
        switch eventType {
        case "BIRTHDAY": return String(localized: "explore.event_type.birthday")
        case "WEDDING": return String(localized: "explore.event_type.wedding")
        case "TEAM_BUILDING": return String(localized: "explore.event_type.team_building")
        case "CONFERENCE": return String(localized: "explore.event_type.conference")
        case "WORKSHOP": return String(localized: "explore.event_type.workshop")
        case "PARTY": return String(localized: "explore.event_type.party")
        case "SPORTS_EVENT", "SPORT_EVENT": return String(localized: "explore.event_type.sport")
        case "CULTURAL_EVENT": return String(localized: "explore.event_type.culture")
        case "FAMILY_GATHERING": return String(localized: "explore.event_type.family")
        case "OUTDOOR_ACTIVITY": return String(localized: "explore.event_type.outdoor")
        case "FOOD_TASTING": return String(localized: "explore.event_type.food")
        case "TECH_MEETUP": return String(localized: "explore.event_type.tech")
        case "WELLNESS_EVENT": return String(localized: "explore.event_type.wellness")
        case "CREATIVE_WORKSHOP": return String(localized: "explore.event_type.creative")
        default: return String(localized: "explore.event_type.other")
        }
    }

    /// Color accent for the event type
    var accentColor: Color {
        switch eventType {
        case "BIRTHDAY", "PARTY": return .pink
        case "WEDDING": return .red
        case "TEAM_BUILDING", "CONFERENCE", "TECH_MEETUP": return .blue
        case "WORKSHOP", "CREATIVE_WORKSHOP": return .purple
        case "SPORTS_EVENT", "SPORT_EVENT", "OUTDOOR_ACTIVITY": return .green
        case "CULTURAL_EVENT": return .orange
        case "FAMILY_GATHERING": return .yellow
        case "FOOD_TASTING": return .brown
        case "WELLNESS_EVENT": return .mint
        default: return .gray
        }
    }
}

/// Category filter chip model for the explore UI.
enum EventCategoryItem: String, CaseIterable, Identifiable {
    case all = "ALL"
    case social = "SOCIAL"
    case sport = "SPORT"
    case culture = "CULTURE"
    case professional = "PROFESSIONAL"
    case food = "FOOD"
    case wellness = "WELLNESS"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .all: return String(localized: "explore.category.all")
        case .social: return String(localized: "explore.category.social")
        case .sport: return String(localized: "explore.category.sport")
        case .culture: return String(localized: "explore.category.culture")
        case .professional: return String(localized: "explore.category.professional")
        case .food: return String(localized: "explore.category.food")
        case .wellness: return String(localized: "explore.category.wellness")
        }
    }

    var icon: String {
        switch self {
        case .all: return "square.grid.2x2"
        case .social: return "person.2.fill"
        case .sport: return "figure.run"
        case .culture: return "theatermasks.fill"
        case .professional: return "briefcase.fill"
        case .food: return "fork.knife"
        case .wellness: return "leaf.fill"
        }
    }

    var eventTypes: [String] {
        switch self {
        case .all: return []
        case .social: return ["BIRTHDAY", "WEDDING", "PARTY", "FAMILY_GATHERING"]
        case .sport: return ["SPORTS_EVENT", "SPORT_EVENT", "OUTDOOR_ACTIVITY"]
        case .culture: return ["CULTURAL_EVENT", "CREATIVE_WORKSHOP"]
        case .professional: return ["TEAM_BUILDING", "CONFERENCE", "WORKSHOP", "TECH_MEETUP"]
        case .food: return ["FOOD_TASTING"]
        case .wellness: return ["WELLNESS_EVENT"]
        }
    }
}
