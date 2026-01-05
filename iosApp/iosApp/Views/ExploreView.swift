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
                        SearchBar(text: $searchText, onSearchChanged: performSearch)

                        // Category Filter
                        CategoryPicker(selectedCategory: $selectedCategory)

                        // Featured Events
                        if !isLoading && !events.isEmpty {
                            VStack(spacing: 16) {
                                SectionHeader(title: "Événements en vedette")

                                LazyVStack(spacing: 16) {
                                    ForEach(events.prefix(3), id: \.id) { event in
                                        ExploreEventCard(
                                            event: event,
                                            onTap: {
                                                // Navigate to event detail
                                            }
                                        )
                                    }
                                }

                                // See More Button
                                Button(action: {
                                    // Show all events
                                }) {
                                    HStack {
                                        Text("Voir plus")
                                            .font(.subheadline.weight(.medium))
                                            .foregroundColor(.wakevPrimary)

                                        Spacer()

                                        Image(systemName: "chevron.right")
                                            .font(.system(size: 14, weight: .semibold))
                                            .foregroundColor(.wakevPrimary)
                                    }
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 12)
                                    .background(Color.wakevPrimary.opacity(0.1))
                                    .cornerRadius(12)
                                }
                            }
                            .padding(.horizontal, 16)

                            Divider()
                                .padding(.leading, 16)

                            // Recommended Events
                            VStack(spacing: 16) {
                                SectionHeader(title: "Recommandés pour vous")

                                LazyVStack(spacing: 16) {
                                    ForEach(events.suffix(from: events.count > 3 ? 3 : 0), id: \.id) { event in
                                        ExploreEventCard(
                                            event: event,
                                            onTap: {
                                                // Navigate to event detail
                                            }
                                        )
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

// MARK: - Search Bar

struct SearchBar: View {
    @Binding var text: String
    let onSearchChanged: (String) -> Void

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 16))
                .foregroundColor(.secondary)

            TextField("Rechercher...", text: $text)
                .textFieldStyle(.plain)
                .onChange(of: text) { _, newValue in
                    onSearchChanged(newValue)
                }

            if !text.isEmpty {
                Button(action: {
                    text = ""
                    onSearchChanged("")
                }) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 16))
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(16)
        .background(Color(.systemGray6))
        .cornerRadius(12)
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
                    CategoryButton(
                        category: category,
                        isSelected: selectedCategory == category
                    )
                }
            }
            .padding(.horizontal, 16)
        }
        .padding(.vertical, 12)
    }
}

struct CategoryButton: View {
    let category: ExploreCategory
    let isSelected: Bool

    var body: some View {
        Button(action: {
            // Toggle category
        }) {
            HStack(spacing: 8) {
                Image(systemName: category.iconName)
                    .font(.system(size: 16, weight: .medium))

                Text(category.title)
                    .font(.subheadline.weight(.medium))
            }
            .foregroundColor(isSelected ? .white : .primary)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(isSelected ? Color.wakevPrimary : Color(.systemGray6))
            )
        }
        .accessibilityLabel(category.title)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

// MARK: - Explore Event Card

struct ExploreEventCard: View {
    let event: Event
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
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
                                .fill(Color(.systemGray5))
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
                .cornerRadius(12)

                // Event Info
                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 8) {
                        EventStatusBadge(status: event.status)

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
        }
        .buttonStyle(PlainButtonStyle())
        .accessibilityLabel(event.title)
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
