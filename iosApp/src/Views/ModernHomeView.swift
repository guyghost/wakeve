import SwiftUI
import Shared

// MARK: - Event Filter Enum

enum HomeEventFilter: String, CaseIterable, Identifiable {
    case upcoming = "À venir"
    case past = "Évènements passés"
    case drafts = "Brouillons"
    case organizedByMe = "Organisés par moi"
    case confirmed = "Confirmés"
    
    var id: String { self.rawValue }
    
    var icon: String {
        switch self {
        case .upcoming: return "calendar"
        case .past: return "arrow.counterclockwise"
        case .drafts: return "pencil"
        case .organizedByMe: return "crown"
        case .confirmed: return "checkmark.circle"
        }
    }
    
    var showBadge: Bool {
        switch self {
        case .drafts: return true
        default: return false
        }
    }
}

// MARK: - Event Theme

struct EventTheme {
    let backgroundColor: Color
    let accentColor: Color
    let emojis: [String]
    let emojiPositions: [CGPoint]
    
    static let beach = EventTheme(
        backgroundColor: Color(hex: "7DD3C0"),
        accentColor: Color(hex: "F97316"),
        emojis: ["🏖️", "☀️", "🐚", "🐠", "🚤"],
        emojiPositions: [
            CGPoint(x: 0.5, y: 0.4),  // Parasol
            CGPoint(x: 0.85, y: 0.15), // Soleil
            CGPoint(x: 0.75, y: 0.75), // Coquillage
            CGPoint(x: 0.15, y: 0.8),  // Poisson
            CGPoint(x: 0.1, y: 0.55)   // Bateau
        ]
    )
    
    static let party = EventTheme(
        backgroundColor: Color(hex: "C084FC"),
        accentColor: Color(hex: "FACC15"),
        emojis: ["🎉", "🎈", "🎊", "🎁", "🎂"],
        emojiPositions: [
            CGPoint(x: 0.5, y: 0.35),
            CGPoint(x: 0.8, y: 0.2),
            CGPoint(x: 0.2, y: 0.25),
            CGPoint(x: 0.75, y: 0.7),
            CGPoint(x: 0.25, y: 0.75)
        ]
    )
    
    static let dinner = EventTheme(
        backgroundColor: Color(hex: "FB923C"),
        accentColor: Color(hex: "DC2626"),
        emojis: ["🍽️", "🍷", "🥗", "🍝", "🥂"],
        emojiPositions: [
            CGPoint(x: 0.5, y: 0.4),
            CGPoint(x: 0.75, y: 0.2),
            CGPoint(x: 0.25, y: 0.3),
            CGPoint(x: 0.7, y: 0.75),
            CGPoint(x: 0.2, y: 0.7)
        ]
    )
    
    static let sport = EventTheme(
        backgroundColor: Color(hex: "34D399"),
        accentColor: Color(hex: "059669"),
        emojis: ["⚽", "🏆", "🥅", "👟", "🎯"],
        emojiPositions: [
            CGPoint(x: 0.5, y: 0.4),
            CGPoint(x: 0.8, y: 0.15),
            CGPoint(x: 0.2, y: 0.2),
            CGPoint(x: 0.75, y: 0.8),
            CGPoint(x: 0.15, y: 0.75)
        ]
    )
    
    static let travel = EventTheme(
        backgroundColor: Color(hex: "60A5FA"),
        accentColor: Color(hex: "1D4ED8"),
        emojis: ["✈️", "🗺️", "🧳", "📸", "🏔️"],
        emojiPositions: [
            CGPoint(x: 0.5, y: 0.35),
            CGPoint(x: 0.75, y: 0.2),
            CGPoint(x: 0.25, y: 0.3),
            CGPoint(x: 0.8, y: 0.75),
            CGPoint(x: 0.2, y: 0.8)
        ]
    )
    
    static let defaultTheme = EventTheme(
        backgroundColor: Color(hex: "94A3B8"),
        accentColor: Color(hex: "475569"),
        emojis: ["📅", "✨", "🎊", "🎈", "🎉"],
        emojiPositions: [
            CGPoint(x: 0.5, y: 0.4),
            CGPoint(x: 0.8, y: 0.2),
            CGPoint(x: 0.2, y: 0.25),
            CGPoint(x: 0.75, y: 0.75),
            CGPoint(x: 0.25, y: 0.8)
        ]
    )
    
    static func theme(for eventType: String?) -> EventTheme {
        guard let type = eventType?.lowercased() else { return .defaultTheme }
        if type.contains("beach") || type.contains("plage") || type.contains("sea") { return .beach }
        if type.contains("party") || type.contains("fête") || type.contains("birthday") { return .party }
        if type.contains("dinner") || type.contains("dîner") || type.contains("restaurant") { return .dinner }
        if type.contains("sport") || type.contains("football") || type.contains("match") { return .sport }
        if type.contains("travel") || type.contains("voyage") || type.contains("trip") { return .travel }
        return .defaultTheme
    }
}

// MARK: - Modern Home View

struct ModernHomeView: View {
    let userId: String
    let repository: EventRepositoryInterface
    let onEventSelected: (Event) -> Void
    let onCreateEvent: () -> Void
    let onProfileClick: () -> Void
    var onNotificationsClick: (() -> Void)? = nil

    @State private var events: [Event] = []
    @State private var isLoading = true
    @State private var showFilterMenu = false
    @State private var selectedFilter: HomeEventFilter = .upcoming
    @State private var unreadNotificationCount: Int = 3
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        ZStack {
            backgroundView

            VStack(spacing: 0) {
                headerView
                    .padding(.horizontal, 16)
                    .padding(.top, 8)

                if isLoading {
                    LoadingEventsView()
                } else if filteredEvents.isEmpty {
                    HomeEmptyStateView(
                        onCreateEvent: onCreateEvent,
                        colorScheme: colorScheme,
                        title: emptyStateTitle,
                        subtitle: emptyStateSubtitle
                    )
                } else {
                    EventsCarouselView(
                        events: filteredEvents,
                        onEventSelected: onEventSelected,
                        userId: userId
                    )
                }
            }
            
            if showFilterMenu {
                FilterDropdownMenu(
                    selectedFilter: $selectedFilter,
                    isShowing: $showFilterMenu,
                    colorScheme: colorScheme,
                    draftCount: draftCount
                )
                .transition(.opacity.combined(with: .scale(scale: 0.95, anchor: .topLeading)))
            }
        }
        .onAppear { loadEvents() }
    }
    
    private var filteredEvents: [Event] {
        events.filter { event in
            switch selectedFilter {
            case .upcoming:
                return event.status != .finalized && event.status != .draft
            case .past:
                return event.status == .finalized
            case .drafts:
                return event.status == .draft
            case .organizedByMe:
                return event.organizerId == userId
            case .confirmed:
                return event.status == .confirmed
            }
        }
    }
    
    private var draftCount: Int { events.filter { $0.status == .draft }.count }
    
    private var emptyStateTitle: String {
        switch selectedFilter {
        case .upcoming: return "Aucun évènement à venir"
        case .past: return "Aucun évènement passé"
        case .drafts: return "Aucun brouillon"
        case .organizedByMe: return "Aucun évènement organisé"
        case .confirmed: return "Aucun évènement confirmé"
        }
    }
    
    private var emptyStateSubtitle: String {
        switch selectedFilter {
        case .upcoming: return "Les évènements à venir apparaîtront ici, que vous les organisiez ou non."
        case .past: return "Les évènements passés apparaîtront ici."
        case .drafts: return "Vos brouillons d'évènements apparaîtront ici."
        case .organizedByMe: return "Les évènements que vous organisez apparaîtront ici."
        case .confirmed: return "Les évènements confirmés apparaîtront ici."
        }
    }

    private var backgroundView: some View {
        Color(.systemBackground)
            .ignoresSafeArea()
    }

    private var headerView: some View {
        HStack {
            Button(action: { withAnimation(.spring(response: 0.3)) { showFilterMenu.toggle() } }) {
                HStack(spacing: 4) {
                    Text(selectedFilter.rawValue)
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(colorScheme == .dark ? .white : Color(hex: "0F172A"))

                    Image(systemName: showFilterMenu ? "chevron.up" : "chevron.down")
                        .font(.system(size: 20, weight: .medium))
                        .foregroundColor(colorScheme == .dark ? .white : Color(hex: "0F172A"))
                }
            }

            Spacer()

            Button(action: onCreateEvent) {
                Image(systemName: "plus")
                    .font(.system(size: 24, weight: .medium))
                    .foregroundColor(colorScheme == .dark ? .white : Color(hex: "0F172A"))
                    .frame(width: 40, height: 40)
                    .background(
                        Circle()
                            .fill(colorScheme == .dark ? Color(hex: "2A2A2A") : Color(hex: "E2E8F0"))
                    )
            }

            Button(action: { onNotificationsClick?() }) {
                ZStack(alignment: .topTrailing) {
                    Image(systemName: "bell.fill")
                        .font(.system(size: 20, weight: .medium))
                        .foregroundColor(colorScheme == .dark ? .white : Color(hex: "0F172A"))
                        .frame(width: 40, height: 40)
                        .background(
                            Circle()
                                .fill(colorScheme == .dark ? Color(hex: "2A2A2A") : Color(hex: "E2E8F0"))
                        )

                    if unreadNotificationCount > 0 {
                        Text(unreadNotificationCount > 9 ? "9+" : "\(unreadNotificationCount)")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.white)
                            .frame(minWidth: 16, minHeight: 16)
                            .background(Circle().fill(Color.red))
                            .offset(x: 4, y: -4)
                    }
                }
            }

            Button(action: onProfileClick) {
                Text("U")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(Color(hex: "F97316")))
            }
        }
    }

    private func loadEvents() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            events = repository.getAllEvents()
            isLoading = false
        }
    }
}

// MARK: - Events Carousel View

struct EventsCarouselView: View {
    let events: [Event]
    let onEventSelected: (Event) -> Void
    let userId: String
    @Environment(\.colorScheme) var colorScheme
    
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 16) {
                ForEach(events, id: \.id) { event in
                    VisualEventCard(
                        event: event,
                        isOrganizer: event.organizerId == userId,
                        onTap: { onEventSelected(event) }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
    }
}

// MARK: - Visual Event Card

struct VisualEventCard: View {
    let event: Event
    let isOrganizer: Bool
    let onTap: () -> Void
    @Environment(\.colorScheme) var colorScheme
    
    private var theme: EventTheme {
        EventTheme.theme(for: event.eventType.name)
    }
    
    var body: some View {
        Button(action: onTap) {
            ZStack {
                // Background
                RoundedRectangle(cornerRadius: 32)
                    .fill(theme.backgroundColor)
                
                // Emoji decorations - scattered around
                ForEach(0..<min(theme.emojis.count, theme.emojiPositions.count), id: \.self) { index in
                    Text(theme.emojis[index])
                        .font(.system(size: 90))
                        .position(
                            x: theme.emojiPositions[index].x * 360,
                            y: theme.emojiPositions[index].y * 640
                        )
                        .shadow(color: Color.black.opacity(0.15), radius: 8, x: 0, y: 4)
                }
                
                VStack(spacing: 0) {
                    HStack {
                        // Badge "Organisé par moi"
                        if isOrganizer {
                            HStack(spacing: 8) {
                                Image(systemName: "crown.fill")
                                    .font(.system(size: 16))
                                Text("Organisé par moi")
                                    .font(.system(size: 16, weight: .semibold))
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(
                                Capsule()
                                    .fill(Color.white.opacity(0.25))
                            )
                        }
                        
                        Spacer()
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 20)
                    
                    Spacer()
                    
                    // Event title - large at bottom
                    Text(event.title)
                        .font(.system(size: 52, weight: .bold))
                        .foregroundColor(.white)
                        .shadow(color: Color.black.opacity(0.4), radius: 8, x: 0, y: 4)
                        .padding(.horizontal, 24)
                        .padding(.bottom, 32)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .frame(width: 360, height: 640)
            .shadow(color: Color.black.opacity(0.2), radius: 20, x: 0, y: 10)
        }
        .buttonStyle(ScaleButtonStyle())
    }
}

// MARK: - Filter Dropdown Menu

struct FilterDropdownMenu: View {
    @Binding var selectedFilter: HomeEventFilter
    @Binding var isShowing: Bool
    let colorScheme: ColorScheme
    let draftCount: Int
    
    var body: some View {
        VStack {
            HStack {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(HomeEventFilter.allCases.enumerated()), id: \.element.id) { index, filter in
                        Button(action: {
                            withAnimation(.spring(response: 0.3)) {
                                selectedFilter = filter
                                isShowing = false
                            }
                        }) {
                            HStack(spacing: 12) {
                                ZStack {
                                    if selectedFilter == filter {
                                        Image(systemName: "checkmark")
                                            .font(.system(size: 16, weight: .semibold))
                                            .foregroundColor(colorScheme == .dark ? .white : Color(hex: "0F172A"))
                                    }
                                }
                                .frame(width: 24)
                                
                                Image(systemName: filter.icon)
                                    .font(.system(size: 18))
                                    .foregroundColor(colorScheme == .dark ? .white : Color(hex: "0F172A"))
                                    .frame(width: 24)
                                
                                HStack(spacing: 4) {
                                    Text(filter.rawValue)
                                        .font(.body.weight(.medium))
                                        .foregroundColor(colorScheme == .dark ? .white : Color(hex: "0F172A"))
                                    
                                    if filter.showBadge && draftCount > 0 {
                                        Text("(\(draftCount))")
                                            .font(.body.weight(.medium))
                                            .foregroundColor(colorScheme == .dark ? .white : Color(hex: "0F172A"))
                                    }
                                }
                                
                                Spacer()
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                        }
                        
                        if index == 1 {
                            Divider()
                                .background(colorScheme == .dark ? Color.white.opacity(0.1) : Color.black.opacity(0.1))
                                .padding(.horizontal, 16)
                        }
                    }
                }
                .frame(width: 280)
                .padding(.vertical, 8)
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(colorScheme == .dark ? Color(hex: "1E293B").opacity(0.95) : Color.white.opacity(0.95))
                        .shadow(color: Color.black.opacity(0.2), radius: 20, x: 0, y: 10)
                )
                .padding(.top, 60)
                .padding(.leading, 16)
                
                Spacer()
            }
            
            Spacer()
        }
        .background(Color.black.opacity(0.001))
        .onTapGesture {
            withAnimation(.spring(response: 0.3)) {
                isShowing = false
            }
        }
    }
}

// MARK: - Empty State View

struct HomeEmptyStateView: View {
    let onCreateEvent: () -> Void
    let colorScheme: ColorScheme
    let title: String
    let subtitle: String

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: 24) {
                Image(systemName: "calendar")
                    .font(.system(size: 80, weight: .light))
                    .foregroundColor(colorScheme == .dark ? Color(hex: "64748B") : Color(hex: "94A3B8"))

                Text(title)
                    .font(.title2.weight(.semibold))
                    .foregroundColor(colorScheme == .dark ? .white : Color(hex: "0F172A"))
                    .multilineTextAlignment(.center)

                Text(subtitle)
                    .font(.body)
                    .foregroundColor(colorScheme == .dark ? Color(hex: "94A3B8") : Color(hex: "64748B"))
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
                    .padding(.horizontal, 32)
            }

            Spacer()

            Button(action: onCreateEvent) {
                Text("Créer un évènement")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.black)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(RoundedRectangle(cornerRadius: 28).fill(Color.white))
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
        }
    }
}

// MARK: - Loading View

struct LoadingEventsView: View {
    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: .wakevePrimary))
                .scaleEffect(1.3)

            Text("Chargement des événements...")
                .font(.body.weight(.medium))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Button Styles

struct ScaleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

// MARK: - Preview
// Preview commented out due to API changes in shared module
