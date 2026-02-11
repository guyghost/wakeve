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
                            isSecure: nil,
                            isDisabled: nil,
                            keyboardType: nil,
                            errorMessage: nil,
                            leftIcon: "magnifyingglass",
                            rightIcon: nil,
                            leftIconAction: {},
                            rightIconAction: nil
                        )
                        .onChange(of: searchText) { _, newValue in
                            performSearch(newValue)
                        }

                        // Category Filter
                        CategoryPicker(selectedCategory: $selectedCategory)

                        // Featured Events
                        if !isLoading && !events.isEmpty {
                            VStack(spacing: 16) {
                                SectionHeader("Événements en vedette")

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
                                    icon: "chevron.right",
                                    style: .text,
                                    action: {
                                        // Show all events
                                    }
                                )
                            }
                            .padding(.horizontal, 16)

                            LiquidGlassDivider(style: .subtle)
                                .padding(.leading, 16)

                            // Recommended Events
                            VStack(spacing: 16) {
                                SectionHeader("Recommandés pour vous")

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
                            LoadingView(message: "Chargement des événements...")
                        } else {
                            EmptyStateView(
                                icon: "calendar.badge.exclamationmark",
                                title: "Aucun événement",
                                message: "Créez votre premier événement pour commencer !"
                            )
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
                    eventStatusBadge(for: event.status)

                    Spacer()

                    if !event.participants.isEmpty {
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

                if !event.description.isEmpty {
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
private func eventStatusBadge(for status: EventStatus) -> LiquidGlassBadge {
    let (text, style): (String, LiquidGlassBadgeStyle)
    switch status {
    case .draft:
        text = "Brouillon"
        style = .warning
    case .polling:
        text = "Sondage"
        style = .info
    case .comparing:
        text = "Comparaison"
        style = .accent
    case .confirmed:
        text = "Confirmé"
        style = .success
    case .organizing:
        text = "Organisation"
        style = .warning
    case .finalized:
        text = "Finalisé"
        style = .success
    default:
        text = "Inconnu"
        style = .default
    }

    return LiquidGlassBadge(text: text, style: style)
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

// SectionHeader, LoadingView, EmptyStateView are defined in Components/SharedComponents.swift

// MARK: - Preview
// Preview commented out due to API changes in shared module
// TODO: Update preview when Event model is stabilized
