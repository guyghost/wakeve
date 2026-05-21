import SwiftUI
import Shared

// MARK: - Event Filter Enum

enum HomeEventFilter: String, CaseIterable, Identifiable {
    case upcoming
    case past
    case drafts
    case organizedByMe
    case confirmed

    var id: String { self.rawValue }

    var displayName: String {
        switch self {
        case .upcoming: return String(localized: "home.filter.upcoming")
        case .past: return String(localized: "home.filter.past")
        case .drafts: return String(localized: "home.filter.drafts")
        case .organizedByMe: return String(localized: "home.filter.organized_by_me")
        case .confirmed: return String(localized: "home.filter.confirmed")
        }
    }
    
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
        emojis: ["📅", "✨", "🎈"],
        emojiPositions: [
            CGPoint(x: 0.52, y: 0.34),
            CGPoint(x: 0.78, y: 0.18),
            CGPoint(x: 0.72, y: 0.67)
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

// MARK: - Home View

struct HomeView: View {
    let userId: String
    let repository: EventRepositoryInterface
    let onEventSelected: (Event) -> Void
    let onCreateEvent: () -> Void
    let onProfileClick: () -> Void

    @State private var events: [Event] = []
    @State private var isLoading = true

    var body: some View {
        HomeContentView(
            events: events,
            isLoading: isLoading,
            userId: userId,
            onEventSelected: onEventSelected,
            onCreateEvent: onCreateEvent,
            onProfileClick: onProfileClick
        )
        .onAppear { loadEvents() }
    }

    private func loadEvents() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            events = repository.getAllEvents()
            isLoading = false
        }
    }
}

// MARK: - Home Content View

struct HomeContentView: View {
    let events: [Event]
    let isLoading: Bool
    let userId: String
    let onEventSelected: (Event) -> Void
    let onCreateEvent: () -> Void
    let onProfileClick: () -> Void

    @State private var showFilterMenu = false
    @State private var selectedFilter: HomeEventFilter = .upcoming

    init(
        events: [Event],
        isLoading: Bool,
        userId: String,
        initialSelectedFilter: HomeEventFilter = .upcoming,
        onEventSelected: @escaping (Event) -> Void,
        onCreateEvent: @escaping () -> Void,
        onProfileClick: @escaping () -> Void
    ) {
        self.events = events
        self.isLoading = isLoading
        self.userId = userId
        self.onEventSelected = onEventSelected
        self.onCreateEvent = onCreateEvent
        self.onProfileClick = onProfileClick
        self._selectedFilter = State(initialValue: initialSelectedFilter)
    }

    var body: some View {
        ZStack {
            backgroundView

            VStack(spacing: 0) {
                headerView
                    .padding(.horizontal, 16)
                    .padding(.top, WakeveTheme.Navigation.controlTopSpacing)

                if isLoading {
                    LoadingEventsView()
                } else if filteredEvents.isEmpty {
                    HomeEmptyStateView(
                        onCreateEvent: onCreateEvent,
                        title: emptyStateTitle,
                        subtitle: emptyStateSubtitle
                    )
                } else {
                    EventsCarouselView(
                        events: filteredEvents,
                        onEventSelected: onEventSelected,
                        userId: userId
                    )
                    .frame(maxHeight: .infinity, alignment: .center)
                }
            }
            
            if showFilterMenu {
                FilterDropdownMenu(
                    selectedFilter: $selectedFilter,
                    isShowing: $showFilterMenu,
                    draftCount: draftCount
                )
                .transition(.opacity.combined(with: .scale(scale: 0.95, anchor: .topLeading)))
            }
        }
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
        case .upcoming: return String(localized: "home.empty.upcoming.title")
        case .past: return String(localized: "home.empty.past.title")
        case .drafts: return String(localized: "home.empty.drafts.title")
        case .organizedByMe: return String(localized: "home.empty.organized.title")
        case .confirmed: return String(localized: "home.empty.confirmed.title")
        }
    }

    private var emptyStateSubtitle: String {
        switch selectedFilter {
        case .upcoming: return String(localized: "home.empty.upcoming.subtitle")
        case .past: return String(localized: "home.empty.past.subtitle")
        case .drafts: return String(localized: "home.empty.drafts.subtitle")
        case .organizedByMe: return String(localized: "home.empty.organized.subtitle")
        case .confirmed: return String(localized: "home.empty.confirmed.subtitle")
        }
    }

    private var backgroundView: some View {
        WakeveScreenBackground(style: .grouped)
    }

    private var headerView: some View {
        HStack {
            Button(action: { withAnimation(.spring(response: 0.3)) { showFilterMenu.toggle() } }) {
                HStack(spacing: 4) {
                    Text(selectedFilter.displayName)
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.primary)

                    Image(systemName: showFilterMenu ? "chevron.up" : "chevron.down")
                        .font(.system(size: 20, weight: .medium))
                        .foregroundColor(.primary)
                }
            }

            Spacer()

            if shouldShowHeaderCreateButton {
                WakeveCircleButton(
                    systemImage: "plus",
                    accessibilityLabel: String(localized: "home.create_event"),
                    variant: .light,
                    size: 44,
                    action: onCreateEvent
                )
            }

            Button(action: onProfileClick) {
                WakeveAvatar(initials: "U", size: 44)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Profil")
        }
    }

    private var shouldShowHeaderCreateButton: Bool {
        !isLoading && !filteredEvents.isEmpty
    }
}

// MARK: - Events Carousel View

struct EventsCarouselView: View {
    let events: [Event]
    let onEventSelected: (Event) -> Void
    let userId: String
    
    var body: some View {
        if events.count == 1, let event = events.first {
            VStack(spacing: 0) {
                Spacer(minLength: 0)
                VisualEventCard(
                    event: event,
                    isOrganizer: event.organizerId == userId,
                    onTap: { onEventSelected(event) }
                )
                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            VStack(spacing: 0) {
                Spacer(minLength: 0)
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
                }
                Spacer(minLength: 0)
            }
            .frame(maxHeight: .infinity)
        }
    }
}

// MARK: - Visual Event Card

struct VisualEventCard: View {
    let event: Event
    let isOrganizer: Bool
    let onTap: () -> Void

    private let cardWidth: CGFloat = 340
    private let cardHeight: CGFloat = 552
    private let cardCornerRadius: CGFloat = 30
    
    private var theme: EventTheme {
        EventTheme.theme(for: event.eventType.name)
    }
    
    var body: some View {
        Button(action: onTap) {
            ZStack(alignment: .bottomLeading) {
                backgroundLayer
                decorationLayer
                readabilityGradient
                VStack(spacing: 0) {
                    HStack {
                        // Badge "Organisé par moi"
                        if isOrganizer {
                            HStack(spacing: 8) {
                                Image(systemName: "crown.fill")
                                    .font(.system(size: 16))
                                Text(String(localized: "home.organized_by_me_badge"))
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
                        .font(.system(size: 42, weight: .bold))
                        .foregroundColor(.white)
                        .lineLimit(3)
                        .minimumScaleFactor(0.8)
                        .shadow(color: Color.black.opacity(0.18), radius: 5, x: 0, y: 2)
                        .padding(.horizontal, 24)
                        .padding(.bottom, 32)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .frame(width: cardWidth, height: cardHeight)
            .clipShape(RoundedRectangle(cornerRadius: cardCornerRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: cardCornerRadius, style: .continuous)
                    .stroke(Color.white.opacity(0.18), lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.08), radius: 14, x: 0, y: 8)
        }
        .buttonStyle(ScaleButtonStyle())
    }

    private var backgroundLayer: some View {
        RoundedRectangle(cornerRadius: cardCornerRadius, style: .continuous)
            .fill(
                LinearGradient(
                    colors: [
                        theme.backgroundColor.opacity(0.98),
                        theme.backgroundColor.opacity(0.88),
                        theme.accentColor.opacity(0.58)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
    }

    private var decorationLayer: some View {
        GeometryReader { proxy in
            ForEach(0..<decorationCount, id: \.self) { index in
                Text(theme.emojis[index])
                    .font(.system(size: decorationSize(for: index)))
                    .opacity(decorationOpacity(for: index))
                    .position(
                        x: clampedX(for: index, in: proxy.size.width),
                        y: clampedY(for: index, in: proxy.size.height)
                    )
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: cardCornerRadius, style: .continuous))
    }

    private var readabilityGradient: some View {
        LinearGradient(
            colors: [
                Color.black.opacity(0),
                Color.black.opacity(0.08),
                Color.black.opacity(0.30)
            ],
            startPoint: .center,
            endPoint: .bottom
        )
    }

    private var decorationCount: Int {
        min(3, theme.emojis.count, theme.emojiPositions.count)
    }

    private func decorationSize(for index: Int) -> CGFloat {
        switch index {
        case 0: return 78
        case 1: return 58
        default: return 68
        }
    }

    private func decorationOpacity(for index: Int) -> Double {
        index == 0 ? 0.88 : 0.72
    }

    private func clampedX(for index: Int, in width: CGFloat) -> CGFloat {
        let position = theme.emojiPositions[index].x * width
        return min(max(position, 48), width - 48)
    }

    private func clampedY(for index: Int, in height: CGFloat) -> CGFloat {
        let position = theme.emojiPositions[index].y * height
        return min(max(position, 48), height - 72)
    }
}

// MARK: - Filter Dropdown Menu

struct FilterDropdownMenu: View {
    @Binding var selectedFilter: HomeEventFilter
    @Binding var isShowing: Bool
    let draftCount: Int

    @Environment(\.colorScheme) private var colorScheme
    
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
                                            .foregroundColor(.primary)
                                    }
                                }
                                .frame(width: 24)
                                
                                Image(systemName: filter.icon)
                                    .font(.system(size: 18))
                                    .foregroundColor(.primary)
                                    .frame(width: 24)
                                
                                HStack(spacing: 4) {
                                    Text(filter.displayName)
                                        .font(.body.weight(.medium))
                                        .foregroundColor(.primary)
                                    
                                    if filter.showBadge && draftCount > 0 {
                                        Text("(\(draftCount))")
                                            .font(.body.weight(.medium))
                                            .foregroundColor(.primary)
                                    }
                                }
                                
                                Spacer()
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                        }
                        
                        if index == 1 {
                            Divider()
                                .padding(.horizontal, 16)
                        }
                    }
                }
                .frame(width: 280)
                .padding(.vertical, 8)
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(WakeveTheme.ColorToken.cardFill(for: colorScheme))
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
    let title: String
    let subtitle: String

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: 24) {
                Image(systemName: "calendar")
                    .font(.system(size: 80, weight: .light))
                    .foregroundColor(.secondary)

                Text(title)
                    .font(.title2.weight(.semibold))
                    .foregroundColor(.primary)
                    .multilineTextAlignment(.center)

                Text(subtitle)
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
                    .padding(.horizontal, 32)
            }

            Spacer()

            WakeveActionButton(
                String(localized: "home.create_event"),
                systemImage: "plus",
                variant: .primary
            ) {
                onCreateEvent()
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

            Text(String(localized: "home.loading"))
                .font(.body.weight(.medium))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Preview

#if DEBUG
#Preview("Home - One Card Light") {
    HomeContentView(
        events: [EventFactory.polling],
        isLoading: false,
        userId: UserFactory.organizer.id,
        onEventSelected: { _ in },
        onCreateEvent: {},
        onProfileClick: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Home - One Card Dark") {
    HomeContentView(
        events: [EventFactory.polling],
        isLoading: false,
        userId: UserFactory.organizer.id,
        onEventSelected: { _ in },
        onCreateEvent: {},
        onProfileClick: {}
    )
    .preferredColorScheme(.dark)
}

#Preview("Home - Multiple Cards Light") {
    HomeContentView(
        events: [EventFactory.polling, EventFactory.complete, EventFactory.withManyParticipants],
        isLoading: false,
        userId: UserFactory.participant.id,
        onEventSelected: { _ in },
        onCreateEvent: {},
        onProfileClick: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Home - Multiple Cards Dark") {
    HomeContentView(
        events: [EventFactory.polling, EventFactory.complete, EventFactory.withManyParticipants],
        isLoading: false,
        userId: UserFactory.participant.id,
        onEventSelected: { _ in },
        onCreateEvent: {},
        onProfileClick: {}
    )
    .preferredColorScheme(.dark)
}

#Preview("Home - Organized By Me") {
    HomeContentView(
        events: [EventFactory.withManyParticipants],
        isLoading: false,
        userId: UserFactory.organizer.id,
        initialSelectedFilter: .organizedByMe,
        onEventSelected: { _ in },
        onCreateEvent: {},
        onProfileClick: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Home - Drafts") {
    HomeContentView(
        events: [
            EventFactory.make(
                id: "event-draft-home-preview",
                title: "Voyage entre amis",
                description: "Brouillon de voyage pour valider la vue d'accueil.",
                status: .draft,
                eventType: .outdoorActivity,
                expectedParticipants: 8
            )
        ],
        isLoading: false,
        userId: UserFactory.organizer.id,
        initialSelectedFilter: .drafts,
        onEventSelected: { _ in },
        onCreateEvent: {},
        onProfileClick: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Home - Past Events") {
    HomeContentView(
        events: [EventFactory.past],
        isLoading: false,
        userId: UserFactory.organizer.id,
        initialSelectedFilter: .past,
        onEventSelected: { _ in },
        onCreateEvent: {},
        onProfileClick: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Home - Empty Light") {
    HomeContentView(
        events: [],
        isLoading: false,
        userId: UserFactory.organizer.id,
        onEventSelected: { _ in },
        onCreateEvent: {},
        onProfileClick: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Home - Empty Dark") {
    HomeContentView(
        events: [],
        isLoading: false,
        userId: UserFactory.organizer.id,
        onEventSelected: { _ in },
        onCreateEvent: {},
        onProfileClick: {}
    )
    .preferredColorScheme(.dark)
}

#Preview("Home - Loading") {
    HomeContentView(
        events: [],
        isLoading: true,
        userId: UserFactory.organizer.id,
        onEventSelected: { _ in },
        onCreateEvent: {},
        onProfileClick: {}
    )
    .preferredColorScheme(.light)
}
#endif
