import SwiftUI

// Import the Wakeve color extensions

/// Explore Tab View with search, filtering, and discovery sections.
/// Sections: "Tendances", "Pres de vous", "Recommandes pour vous"
struct ExploreTabView: View {
    @StateObject private var viewModel = ExploreViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    // Category filter circles
                    CategoryCirclesRow(
                        selectedCategory: $viewModel.selectedCategory,
                        onSelect: { category in
                            viewModel.selectCategory(category)
                        }
                    )
                    .padding(.top, 8)

                    // Content
                    if viewModel.isLoading && viewModel.trendingEvents.isEmpty {
                        LoadingStateView()
                    } else if viewModel.isSearching {
                        SearchResultsSection(
                            results: viewModel.searchResults,
                            searchText: viewModel.searchText
                        )
                    } else if viewModel.trendingEvents.isEmpty && viewModel.recommendedEvents.isEmpty {
                        ExploreEmptyStateView()
                    } else {
                        DiscoverySections(viewModel: viewModel)
                    }
                }
                .padding(.bottom, 24)
            }
            .refreshable {
                viewModel.refresh()
            }
            .navigationTitle(String(localized: "explore.title"))
            .searchable(
                text: $viewModel.searchText,
                prompt: String(localized: "explore.search_prompt")
            )
            .onChange(of: viewModel.searchText) { _, _ in
                viewModel.search()
            }
            .navigationDestination(for: EventScenario.self) { scenario in
                ExploreScenarioDetailView(scenario: scenario)
            }
        }
    }
}

// MARK: - Category Circle Selectors

struct CategoryCirclesRow: View {
    @Binding var selectedCategory: EventCategoryItem
    let onSelect: (EventCategoryItem) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 20) {
                ForEach(EventCategoryItem.allCases) { category in
                    CategoryCircle(
                        category: category,
                        isSelected: selectedCategory == category,
                        onTap: {
                            selectedCategory = category
                            onSelect(category)
                        }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
    }
}

struct CategoryCircle: View {
    let category: EventCategoryItem
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 8) {
                ZStack {
                    Circle()
                        .fill(isSelected ? category.tintColor : category.tintColor.opacity(0.12))
                        .frame(width: 64, height: 64)

                    Image(systemName: category.icon)
                        .font(.title2)
                        .foregroundColor(isSelected ? .white : category.tintColor)
                }

                Text(category.displayName)
                    .font(.caption)
                    .fontWeight(isSelected ? .semibold : .regular)
                    .foregroundColor(isSelected ? .primary : .secondary)
                    .lineLimit(1)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(category.displayName)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

// MARK: - Discovery Sections

struct DiscoverySections: View {
    @ObservedObject var viewModel: ExploreViewModel

    var body: some View {
        VStack(spacing: 24) {
            // Scenario grid ("A decouvrir")
            ScenarioGridSection()

            // Trending section
            if !viewModel.trendingEvents.isEmpty {
                ExploreSection(
                    title: String(localized: "explore.trending"),
                    icon: "flame.fill",
                    iconColor: .orange
                ) {
                    HorizontalEventCards(events: viewModel.trendingEvents)
                }
            }

            // Nearby section (placeholder until geolocation is wired)
            if !viewModel.nearbyEvents.isEmpty {
                ExploreSection(
                    title: String(localized: "explore.nearby"),
                    icon: "location.fill",
                    iconColor: .blue
                ) {
                    HorizontalEventCards(events: viewModel.nearbyEvents)
                }
            }

            // Recommended section
            if !viewModel.recommendedEvents.isEmpty {
                ExploreSection(
                    title: String(localized: "explore.recommended"),
                    icon: "sparkles",
                    iconColor: .wakeveAccent
                ) {
                    HorizontalEventCards(events: viewModel.recommendedEvents)
                }
            }
        }
        .padding(.top, 8)
    }
}

// MARK: - Scenario Grid Section ("A decouvrir")

struct ScenarioGridSection: View {
    let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 8) {
                Image(systemName: "sparkle")
                    .foregroundColor(.wakeveAccent)
                Text(String(localized: "explore.discover"))
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
            }
            .padding(.horizontal, 16)

            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(EventScenario.allScenarios) { scenario in
                    NavigationLink(value: scenario) {
                        ScenarioCard(scenario: scenario)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
        }
    }
}

struct ScenarioCard: View {
    let scenario: EventScenario

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: scenario.gradientColors,
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            VStack(alignment: .leading, spacing: 4) {
                Spacer()
                Text(scenario.title)
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .lineLimit(3)
                    .multilineTextAlignment(.leading)
            }
            .padding(14)
        }
        .frame(height: 140)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

// MARK: - Section Container

struct ExploreSection<Content: View>: View {
    let title: String
    let icon: String
    let iconColor: Color
    let content: () -> Content

    init(
        title: String,
        icon: String,
        iconColor: Color,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.title = title
        self.icon = icon
        self.iconColor = iconColor
        self.content = content
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: icon)
                    .foregroundColor(iconColor)
                Text(title)
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
            }
            .padding(.horizontal, 16)

            content()
        }
    }
}

// MARK: - Horizontal Event Cards

struct HorizontalEventCards: View {
    let events: [ExploreEventItem]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 14) {
                ForEach(events) { event in
                    ExploreEventCard(event: event)
                }
            }
            .padding(.horizontal, 16)
        }
    }
}

// MARK: - Event Card

struct ExploreEventCard: View {
    let event: ExploreEventItem

    var body: some View {
        if #available(iOS 26.0, *) {
            cardContent
                .frame(width: 240)
                .padding()
                .glassEffect()
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        } else {
            cardContent
                .frame(width: 240)
                .padding()
                .background(.regularMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 4)
        }
    }

    private var cardContent: some View {
        VStack(alignment: .leading, spacing: 10) {
            // Type badge and icon
            HStack {
                Image(systemName: event.icon)
                    .font(.title2)
                    .foregroundColor(event.accentColor)
                Spacer()
                Text(event.eventTypeDisplayName)
                    .font(.caption)
                    .fontWeight(.medium)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(event.accentColor.opacity(0.15))
                    .foregroundColor(event.accentColor)
                    .clipShape(Capsule())
            }

            // Title
            Text(event.title)
                .font(.headline)
                .foregroundColor(.primary)
                .lineLimit(2)

            // Description
            Text(event.description)
                .font(.caption)
                .foregroundColor(.secondary)
                .lineLimit(2)

            // Location if available
            if let locationName = event.locationName {
                HStack(spacing: 4) {
                    Image(systemName: "mappin")
                        .font(.caption2)
                    Text(locationName)
                        .font(.caption2)
                }
                .foregroundColor(.secondary)
            }

            // Participant count
            HStack(spacing: 4) {
                Image(systemName: "person.2.fill")
                    .font(.caption2)
                if let max = event.maxParticipants {
                    Text("\(event.participantCount)/\(max)")
                        .font(.caption2)
                } else {
                    Text(String(format: String(localized: "explore.participants_count"), event.participantCount))
                        .font(.caption2)
                }
            }
            .foregroundColor(.secondary)
        }
    }
}

// MARK: - Search Results Section

struct SearchResultsSection: View {
    let results: [ExploreEventItem]
    let searchText: String

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            if results.isEmpty {
                SearchEmptyStateView(searchText: searchText)
            } else {
                Text(String(format: String(localized: "explore.results_count"), results.count))
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 16)
                    .padding(.top, 12)

                LazyVStack(spacing: 12) {
                    ForEach(results) { event in
                        SearchResultRow(event: event)
                    }
                }
                .padding(.horizontal, 16)
            }
        }
    }
}

// MARK: - Search Result Row

struct SearchResultRow: View {
    let event: ExploreEventItem

    var body: some View {
        if #available(iOS 26.0, *) {
            rowContent
                .padding()
                .glassEffect()
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        } else {
            rowContent
                .padding()
                .background(.regularMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .shadow(color: .black.opacity(0.04), radius: 6, x: 0, y: 3)
        }
    }

    private var rowContent: some View {
        HStack(spacing: 12) {
            // Event type icon
            Image(systemName: event.icon)
                .font(.title2)
                .foregroundColor(event.accentColor)
                .frame(width: 44, height: 44)
                .background(event.accentColor.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text(event.title)
                    .font(.headline)
                    .foregroundColor(.primary)
                    .lineLimit(1)

                Text(event.description)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(1)

                HStack(spacing: 12) {
                    HStack(spacing: 3) {
                        Image(systemName: "person.2.fill")
                            .font(.caption2)
                        Text("\(event.participantCount)")
                            .font(.caption2)
                    }

                    if let locationName = event.locationName {
                        HStack(spacing: 3) {
                            Image(systemName: "mappin")
                                .font(.caption2)
                            Text(locationName)
                                .font(.caption2)
                                .lineLimit(1)
                        }
                    }

                    Text(event.eventTypeDisplayName)
                        .font(.caption2)
                        .fontWeight(.medium)
                        .foregroundColor(event.accentColor)
                }
                .foregroundColor(.secondary)
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - Empty States

struct ExploreEmptyStateView: View {
    var body: some View {
        VStack(spacing: 20) {
            Spacer()
                .frame(height: 60)

            Image(systemName: "binoculars.fill")
                .font(.system(size: 56))
                .foregroundColor(.wakeveAccent.opacity(0.5))

            Text(String(localized: "explore.empty_title"))
                .font(.title3)
                .fontWeight(.semibold)
                .foregroundColor(.primary)

            Text(String(localized: "explore.empty_subtitle"))
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Spacer()
        }
    }
}

struct SearchEmptyStateView: View {
    let searchText: String

    var body: some View {
        VStack(spacing: 16) {
            Spacer()
                .frame(height: 60)

            Image(systemName: "magnifyingglass")
                .font(.system(size: 48))
                .foregroundColor(.secondary.opacity(0.5))

            Text(String(format: String(localized: "explore.no_results"), searchText))
                .font(.headline)
                .foregroundColor(.primary)

            Text(String(localized: "explore.search_hint"))
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Spacer()
        }
    }
}

struct LoadingStateView: View {
    var body: some View {
        VStack(spacing: 16) {
            Spacer()
                .frame(height: 80)
            ProgressView()
                .scaleEffect(1.2)
            Text(String(localized: "common.loading"))
                .font(.subheadline)
                .foregroundColor(.secondary)
            Spacer()
        }
    }
}

// MARK: - Previews

#Preview("ExploreTabView Light") {
    ExploreTabView()
        .preferredColorScheme(.light)
}

#Preview("ExploreTabView Dark") {
    ExploreTabView()
        .preferredColorScheme(.dark)
}
