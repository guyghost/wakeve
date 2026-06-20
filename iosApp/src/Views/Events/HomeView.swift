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

// MARK: - Event Next Action

struct EventNextAction {
    let title: String
    let shortTitle: String
    let subtitle: String
    let blockedReason: String?
    let systemImage: String

    var isBlocked: Bool { blockedReason != nil }
    var displaySubtitle: String { blockedReason ?? subtitle }

    init(event: Event) {
        switch event.status {
        case .draft:
            title = String(localized: "events.next_action.draft.title")
            shortTitle = String(localized: "events.next_action.draft.short")
            systemImage = "paperplane.fill"

            let hasSlots = !event.proposedSlots.isEmpty
            if !hasSlots {
                blockedReason = String(localized: "events.next_action.draft.blocked.slots")
            } else {
                blockedReason = nil
            }
            subtitle = String(localized: "events.next_action.draft.subtitle")

        case .polling:
            title = String(localized: "events.next_action.polling.title")
            shortTitle = String(localized: "events.next_action.polling.short")
            subtitle = String(localized: "events.next_action.polling.subtitle")
            blockedReason = nil
            systemImage = "chart.bar.fill"

        case .confirmed, .comparing:
            title = String(localized: "events.next_action.confirmed.title")
            shortTitle = String(localized: "events.next_action.confirmed.short")
            subtitle = String(localized: "events.next_action.confirmed.subtitle")
            blockedReason = nil
            systemImage = "map.fill"

        case .organizing:
            title = String(localized: "events.next_action.organizing.title")
            shortTitle = String(localized: "events.next_action.organizing.short")
            subtitle = String(localized: "events.next_action.organizing.subtitle")
            blockedReason = nil
            systemImage = "checklist"

        case .finalized:
            title = String(localized: "events.next_action.finalized.title")
            shortTitle = String(localized: "events.next_action.finalized.short")
            subtitle = String(localized: "events.next_action.finalized.subtitle")
            blockedReason = nil
            systemImage = "checkmark.seal.fill"

        default:
            title = String(localized: "events.next_action.default.title")
            shortTitle = String(localized: "events.next_action.default.short")
            subtitle = String(localized: "events.next_action.default.subtitle")
            blockedReason = nil
            systemImage = "arrow.right"
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

    @State private var selectedFilter: HomeEventFilter = .upcoming
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

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

            if isLoading {
                LoadingSkeleton(rows: 3, showsHero: true)
                    .padding(.horizontal, WakeveTheme.Spacing.page)
                    .padding(.top, WakeveTheme.Navigation.controlTopSpacing + 76)
            } else if filteredEvents.isEmpty {
                ScrollView(showsIndicators: false) {
                    VStack(spacing: WakeveTheme.Spacing.xl) {
                        headerView

                        if shouldShowDraftResumeCard, let primaryDraft {
                            HomeDraftResumeCard(
                                event: primaryDraft,
                                additionalDraftCount: additionalDraftCount,
                                onTap: { onEventSelected(primaryDraft) }
                            )
                        }

                        EmptyState(
                            systemImage: "calendar.badge.plus",
                            title: emptyStateTitle,
                            subtitle: displayedEmptyStateSubtitle,
                            actionTitle: dynamicTypeSize.isAccessibilitySize ? nil : String(localized: "home.create_event"),
                            action: dynamicTypeSize.isAccessibilitySize ? nil : onCreateEvent
                        )
                        .padding(.top, WakeveTheme.Spacing.lg)
                    }
                    .padding(.horizontal, WakeveTheme.Spacing.page)
                    .padding(.top, WakeveTheme.Navigation.controlTopSpacing)
                    .padding(.bottom, 144)
                }
            } else {
                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xl) {
                        headerView

                        if shouldShowDraftResumeCard, let primaryDraft {
                            HomeDraftResumeCard(
                                event: primaryDraft,
                                additionalDraftCount: additionalDraftCount,
                                onTap: { onEventSelected(primaryDraft) }
                            )
                        }

                        HomeNextActionCard(
                            event: featuredEvent,
                            action: EventNextAction(event: featuredEvent),
                            onTap: { onEventSelected(featuredEvent) }
                        )

                        HomeFeaturedEventView(
                            event: featuredEvent,
                            isOrganizer: featuredEvent.organizerId == userId,
                            onTap: { onEventSelected(featuredEvent) }
                        )

                        HomeUpcomingEventsSection(
                            events: listEvents,
                            userId: userId,
                            onEventSelected: onEventSelected
                        )
                    }
                    .padding(.horizontal, WakeveTheme.Spacing.page)
                    .padding(.top, WakeveTheme.Navigation.controlTopSpacing)
                    .padding(.bottom, 112)
                }
            }

            if !filteredEvents.isEmpty || dynamicTypeSize.isAccessibilitySize {
                HomeFloatingCreateButton(action: onCreateEvent)
                    .padding(.trailing, WakeveTheme.Spacing.lg)
                    .padding(.bottom, WakeveTheme.Spacing.xl)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
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

    private var draftEvents: [Event] {
        events
            .filter { $0.status == .draft }
            .sorted { lhs, rhs in
                eventSortDate(lhs) < eventSortDate(rhs)
            }
    }

    private var primaryDraft: Event? {
        draftEvents.first
    }

    private var additionalDraftCount: Int {
        max(draftEvents.count - 1, 0)
    }

    private var shouldShowDraftResumeCard: Bool {
        selectedFilter == .upcoming && primaryDraft != nil
    }

    private var featuredEvent: Event {
        sortedFilteredEvents.first ?? filteredEvents[0]
    }

    private var listEvents: [Event] {
        Array(sortedFilteredEvents.dropFirst())
    }

    private var sortedFilteredEvents: [Event] {
        filteredEvents.sorted { lhs, rhs in
            eventSortDate(lhs) < eventSortDate(rhs)
        }
    }
    
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

    private var displayedEmptyStateSubtitle: String {
        if dynamicTypeSize.isAccessibilitySize {
            return String(localized: "home.empty.compact_subtitle")
        }

        return emptyStateSubtitle
    }

    private var backgroundView: some View {
        WakeveScreenBackground(style: .grouped)
    }

    private var headerView: some View {
        HStack {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                Text(String(localized: "home.greeting"))
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(.secondary)

                Text(String(localized: "home.upcoming_title"))
                    .font(WakeveTheme.Typography.display)
                    .foregroundColor(.primary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.72)
            }

            Spacer()

            Menu {
                ForEach(HomeEventFilter.allCases) { filter in
                    Button {
                        selectedFilter = filter
                    } label: {
                        Label(filter.displayName, systemImage: filter.icon)
                    }
                }
            } label: {
                Image(systemName: "line.3.horizontal.decrease.circle")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundColor(.primary)
                    .frame(width: 44, height: 44)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(Circle())
            }
            .accessibilityLabel(String(localized: "home.filter_events_accessibility"))

            Button(action: onProfileClick) {
                WakeveAvatar(initials: "U", size: 44)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(String(localized: "profile.title"))
        }
    }

    private var shouldShowHeaderCreateButton: Bool {
        !isLoading && !filteredEvents.isEmpty
    }

    private func eventSortDate(_ event: Event) -> Date {
        eventPrimaryDate(event) ?? .distantFuture
    }
}

// MARK: - Premium Home Sections

private struct HomeFeaturedEventView: View {
    let event: Event
    let isOrganizer: Bool
    let onTap: () -> Void

    var body: some View {
        let nextAction = EventNextAction(event: event)

        Button(action: onTap) {
            EventHeroCard(
                title: event.title,
                subtitle: eventSubtitle(event),
                metadata: featuredMetadata,
                moodPalette: EventMoodPalette.palette(for: event.eventType.name)
            ) {
                HStack(spacing: WakeveTheme.Spacing.md) {
                    ParticipantAvatarStack(initials: participantInitials(event), size: 34, maxVisible: 4)

                    Text(nextAction.shortTitle)
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(.white.opacity(0.84))
                        .padding(.horizontal, WakeveTheme.Spacing.sm)
                        .padding(.vertical, WakeveTheme.Spacing.xs)
                        .background(Color.white.opacity(0.16))
                        .clipShape(Capsule())
                }
            }
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
    }

    private var featuredMetadata: String {
        isOrganizer ? String(localized: "home.organized_by_me_badge") : String(localized: "home.featured.next_moment")
    }
}

private struct HomeNextActionCard: View {
    @Environment(\.colorScheme) private var colorScheme

    let event: Event
    let action: EventNextAction
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
                HStack(alignment: .center, spacing: WakeveTheme.Spacing.md) {
                    Image(systemName: action.systemImage)
                        .font(.headline.weight(.bold))
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                        .background(action.isBlocked ? Color.orange.opacity(0.86) : WakeveTheme.ColorToken.progress(for: colorScheme))
                        .clipShape(Circle())

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text(String(localized: "events.next_action.label"))
                            .font(WakeveTheme.Typography.tiny)
                            .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                            .textCase(.uppercase)

                        Text(action.title)
                            .font(WakeveTheme.Typography.rowTitle)
                            .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

                        Text(action.displaySubtitle)
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                            .lineLimit(2)
                    }

                    Spacer(minLength: WakeveTheme.Spacing.xs)

                    Text(compactDateLabel(event))
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                        .padding(.horizontal, WakeveTheme.Spacing.sm)
                        .padding(.vertical, WakeveTheme.Spacing.xs)
                        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                        .clipShape(Capsule())
                }
            }
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
    }
}

private struct HomeDraftResumeCard: View {
    @Environment(\.colorScheme) private var colorScheme

    let event: Event
    let additionalDraftCount: Int
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
                HStack(alignment: .center, spacing: WakeveTheme.Spacing.md) {
                    Image(systemName: "pencil.and.outline")
                        .font(.headline.weight(.bold))
                        .foregroundColor(WakeveTheme.ColorToken.midnight)
                        .frame(width: 44, height: 44)
                        .background(SemanticColor.warning(for: colorScheme).opacity(0.18))
                        .clipShape(Circle())

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text(String(localized: "home.draft_resume.eyebrow"))
                            .font(WakeveTheme.Typography.tiny)
                            .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                            .textCase(.uppercase)

                        Text(event.title)
                            .font(WakeveTheme.Typography.rowTitle)
                            .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)

                        Text(draftSubtitle)
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                            .lineLimit(2)
                    }

                    Spacer(minLength: WakeveTheme.Spacing.xs)

                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.bold))
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                }
            }
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
    }

    private var draftSubtitle: String {
        if additionalDraftCount > 0 {
            return String(format: String(localized: "home.draft_resume.subtitle_with_count_format"), additionalDraftCount)
        }

        return String(localized: "home.draft_resume.subtitle")
    }
}

private struct HomeUpcomingEventsSection: View {
    let events: [Event]
    let userId: String
    let onEventSelected: (Event) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
            HStack {
                    Text(String(localized: "home.next_events"))
                        .font(WakeveTheme.Typography.section)
                        .foregroundColor(.primary)

                Spacer()

                Text(String(format: String(localized: "home.events_count_format"), events.count))
                    .font(WakeveTheme.Typography.caption)
                    .foregroundColor(.secondary)
            }

            if events.isEmpty {
                WakeveContentCard(prominence: .subtle) {
                    Text(String(localized: "home.no_other_events"))
                        .font(WakeveTheme.Typography.body)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            } else {
                VStack(spacing: WakeveTheme.Spacing.sm) {
                    ForEach(events, id: \.id) { event in
                        EventListRow(
                            title: event.title,
                            subtitle: eventSubtitle(event),
                            dateLabel: compactDateLabel(event),
                            participantInitials: participantInitials(event),
                            nextActionHint: EventNextAction(event: event).shortTitle,
                            action: { onEventSelected(event) }
                        )
                    }
                }
            }
        }
    }
}

private struct HomeFloatingCreateButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "plus")
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(WakeveTheme.ColorToken.midnight)
                .frame(width: 62, height: 62)
                .background(Color.white.opacity(0.92))
                .clipShape(Circle())
                .liquidGlass(cornerRadius: WakeveTheme.Radius.full)
                .shadow(color: Color.black.opacity(0.18), radius: 18, x: 0, y: 10)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(String(localized: "home.create_event"))
    }
}

// MARK: - Home Presentation Helpers

private func eventPrimaryDate(_ event: Event) -> Date? {
    if let finalDate = event.finalDate {
        if let parsedFinalDate = parseHomeDate(finalDate) {
            return parsedFinalDate
        }

        if let matchingSlot = event.proposedSlots.first(where: { $0.id == finalDate }),
           let start = matchingSlot.start {
            return parseHomeDate(start)
        }
    }

    if let firstSlotStart = event.proposedSlots.compactMap(\.start).first {
        return parseHomeDate(firstSlotStart)
    }

    return parseHomeDate(event.deadline)
}

private func eventSubtitle(_ event: Event) -> String {
    let dateText = eventPrimaryDate(event).map(longHomeDateFormatter.string(from:)) ?? String(localized: "home.date_to_confirm")
    let participantText = participantCountText(event)
    return "\(dateText) · \(participantText)"
}

private func compactDateLabel(_ event: Event) -> String {
    guard let date = eventPrimaryDate(event) else {
        return String(localized: "home.to_define")
    }

    return compactHomeDateFormatter.string(from: date)
}

private func participantCountText(_ event: Event) -> String {
    if let expected = event.expectedParticipants?.intValue {
        return String(format: String(localized: "home.expected_participants_format"), expected)
    }

    let count = event.participants.count
    let visibleCount = max(count, 1)
    if visibleCount == 1 {
        return String(format: String(localized: "home.participant_count_singular_format"), visibleCount)
    }

    return String(format: String(localized: "home.participant_count_plural_format"), visibleCount)
}

private func participantInitials(_ event: Event) -> [String] {
    let values = event.participants.prefix(4).enumerated().map { pair in
        let index = pair.offset
        let participant = pair.element
        let trimmed = participant.trimmingCharacters(in: .whitespacesAndNewlines)
        if let first = trimmed.first {
            return String(first).uppercased()
        }
        return String(index + 1)
    }

    return values.isEmpty ? ["U"] : values
}

private func eventGradient(_ event: Event) -> LinearGradient {
    EventMoodPalette.palette(for: event.eventType.name).gradient(for: .dark)
}

private func parseHomeDate(_ value: String) -> Date? {
    let isoFormatter = ISO8601DateFormatter()
    if let date = isoFormatter.date(from: value) {
        return date
    }

    let fractionalFormatter = DateFormatter()
    fractionalFormatter.locale = Locale(identifier: "en_US_POSIX")
    fractionalFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    return fractionalFormatter.date(from: value)
}

private let longHomeDateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.locale = .autoupdatingCurrent
    formatter.dateStyle = .medium
    formatter.timeStyle = .none
    return formatter
}()

private let compactHomeDateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.locale = .autoupdatingCurrent
    formatter.setLocalizedDateFormatFromTemplate("d MMM")
    return formatter
}()

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

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: dynamicTypeSize.isAccessibilitySize ? 16 : 24) {
                    if !dynamicTypeSize.isAccessibilitySize {
                        Image(systemName: "calendar")
                            .font(.system(size: 80, weight: .light))
                            .foregroundColor(.secondary)
                            .accessibilityHidden(true)
                    }

                    Text(title)
                        .font(.title2.weight(.semibold))
                        .foregroundColor(.primary)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)

                    Text(subtitle)
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .lineSpacing(4)
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.horizontal, 24)
                }
                .frame(maxWidth: .infinity)
                .padding(.top, dynamicTypeSize.isAccessibilitySize ? 8 : 80)
                .padding(.bottom, 24)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

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
                .accessibilityLabel(String(localized: "home.loading"))

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
