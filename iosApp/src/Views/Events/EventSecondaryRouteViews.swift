import SwiftUI
import Shared

struct ScenarioDetailParityView: View {
    let event: Event
    let scenarioId: String?
    let repository: EventRepositoryInterface
    let userId: String
    let isReadOnly: Bool
    let onBack: () -> Void
    let onOpenMeetings: () -> Void
    let onOpenTransport: () -> Void

    @State private var scenario: Scenario_?
    @State private var votingResult: ScenarioVotingResult?

    var body: some View {
        NavigationStack {
            ZStack {
                WakeveScreenBackground(style: .grouped)

                ScrollView {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                        if let scenario {
                            WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl) {
                                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                                    Label(statusText(for: scenario), systemImage: "flag.checkered")
                                        .font(WakeveTheme.Typography.caption)
                                        .foregroundStyle(.secondary)

                                    Text(scenario.name)
                                        .font(WakeveTheme.Typography.title2)
                                        .foregroundStyle(.primary)

                                    routeInfoRow("calendar", scenario.dateOrPeriod)
                                    routeInfoRow("mappin.and.ellipse", scenario.location)
                                    routeInfoRow("clock", String(format: String(localized: "scenario.detail.duration_days_format"), scenario.duration))
                                    routeInfoRow("person.2", String(format: String(localized: "scenario.detail.participants_format"), scenario.estimatedParticipants))
                                    routeInfoRow("eurosign.circle", String(format: String(localized: "scenario.detail.budget_format"), scenario.estimatedBudgetPerPerson))

                                    if !scenario.description_.isEmpty {
                                        Text(scenario.description_)
                                            .font(WakeveTheme.Typography.body)
                                            .foregroundStyle(.secondary)
                                    }
                                }
                            }

                            if let votingResult {
                                WakeveContentCard(cornerRadius: WakeveTheme.Radius.lg) {
                                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                                        Text(String(localized: "scenario.detail.votes_title"))
                                            .font(WakeveTheme.Typography.rowTitle)
                                        HStack {
                                            metric(String(localized: "scenario.vote.prefer"), value: votingResult.preferCount)
                                            metric(String(localized: "scenario.vote.neutral"), value: votingResult.neutralCount)
                                            metric(String(localized: "scenario.vote.against"), value: votingResult.againstCount)
                                        }
                                        Text(String(format: String(localized: "scenario.detail.score_format"), votingResult.score))
                                            .font(WakeveTheme.Typography.callout)
                                            .foregroundStyle(.secondary)
                                    }
                                }
                            }

                            WakeveContentCard(cornerRadius: WakeveTheme.Radius.lg) {
                                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                                    Text(String(localized: "scenario.detail.next_actions"))
                                        .font(WakeveTheme.Typography.rowTitle)

                                    WakeveActionButton(
                                        String(localized: "scenario.decision.open_transport"),
                                        systemImage: "car.fill",
                                        variant: .primary,
                                        isDisabled: isReadOnly,
                                        action: onOpenTransport
                                    )

                                    WakeveActionButton(
                                        String(localized: "scenario.decision.open_meetings"),
                                        systemImage: "video.fill",
                                        variant: .secondary,
                                        isDisabled: isReadOnly,
                                        action: onOpenMeetings
                                    )
                                }
                            }
                        } else {
                            EventRouteEmptyState(
                                icon: "map",
                                title: String(localized: "scenario.detail.empty_title"),
                                message: String(localized: "scenario.detail.empty_message"),
                                actionTitle: String(localized: "common.back"),
                                action: onBack
                            )
                        }
                    }
                    .padding(WakeveTheme.Spacing.page)
                }
            }
            .navigationTitle(String(localized: "scenario.detail.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                    }
                    .accessibilityLabel(String(localized: "common.back"))
                }
            }
            .onAppear(perform: loadScenario)
        }
    }

    private func loadScenario() {
        let scenarioRepository = ScenarioRepository(db: RepositoryProvider.shared.database)
        let scenarios = scenarioRepository.getScenariosWithVotes(eventId: event.id)
        let selected = scenarioId.flatMap { id in scenarios.first { $0.scenario.id == id } } ?? scenarios.first { $0.scenario.status == .selected } ?? scenarios.first
        scenario = selected?.scenario
        votingResult = selected.flatMap { scenarioRepository.getVotingResult(scenarioId: $0.scenario.id) }
    }

    private func statusText(for scenario: Scenario_) -> String {
        switch scenario.status {
        case .selected:
            return String(localized: "scenario.status.selected")
        case .rejected:
            return String(localized: "scenario.status.rejected")
        default:
            return String(localized: "scenario.status.proposed")
        }
    }

    private func routeInfoRow(_ icon: String, _ text: String) -> some View {
        Label(text, systemImage: icon)
            .font(WakeveTheme.Typography.callout)
            .foregroundStyle(.secondary)
    }

    private func metric(_ title: String, value: Int32) -> some View {
        VStack(spacing: 4) {
            Text("\(value)")
                .font(WakeveTheme.Typography.section)
                .foregroundStyle(.primary)
            Text(title)
                .font(WakeveTheme.Typography.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

struct AccommodationPlanningRouteView: View {
    let event: Event
    let isOrganizer: Bool
    let isReadOnly: Bool
    let onBack: () -> Void
    let onOpenComments: () -> Void

    @State private var accommodations: [Accommodation_] = []

    var body: some View {
        EventPlanningListRouteView(
            title: String(localized: "events.accommodation"),
            icon: "bed.double.fill",
            emptyTitle: String(localized: "accommodation.empty.title"),
            emptyMessage: String(localized: "accommodation.empty.subtitle"),
            isEmpty: accommodations.isEmpty,
            isReadOnly: isReadOnly,
            pendingSync: hasPendingSync(eventId: event.id),
            onBack: onBack,
            onOpenComments: onOpenComments
        ) {
            ForEach(accommodations, id: \.id) { accommodation in
                EventRouteSummaryCard(
                    icon: "bed.double",
                    title: accommodation.name,
                    subtitle: accommodation.address.isEmpty ? String(describing: accommodation.type) : accommodation.address,
                    detail: String(format: String(localized: "accommodation.capacity_format"), accommodation.capacity)
                )
            }
        }
        .onAppear {
            accommodations = AccommodationRepository(db: RepositoryProvider.shared.database)
                .getAccommodationsByEventId(eventId: event.id)
        }
    }
}

struct MealPlanningRouteView: View {
    let event: Event
    let participants: [ParticipantModel]
    let isOrganizer: Bool
    let isReadOnly: Bool
    let onBack: () -> Void
    let onOpenComments: () -> Void

    @State private var meals: [MealModel] = []
    @State private var showMealForm = false

    var body: some View {
        EventPlanningListRouteView(
            title: String(localized: "events.meal_planning"),
            icon: "fork.knife",
            emptyTitle: String(localized: "meal.empty.title"),
            emptyMessage: String(localized: "meal.empty.subtitle"),
            isEmpty: meals.isEmpty,
            isReadOnly: isReadOnly,
            pendingSync: hasPendingSync(eventId: event.id),
            primaryActionTitle: isOrganizer && !isReadOnly ? String(localized: "meal.form.new_title") : nil,
            onPrimaryAction: { showMealForm = true },
            onBack: onBack,
            onOpenComments: onOpenComments
        ) {
            ForEach(meals) { meal in
                EventRouteSummaryCard(
                    icon: "fork.knife",
                    title: meal.name,
                    subtitle: "\(displayName(for: meal.type)) - \(meal.date) \(meal.time)",
                    detail: String(format: String(localized: "meal.servings_format"), meal.servings)
                )
            }
        }
        .sheet(isPresented: $showMealForm) {
            MealFormSheet(eventId: event.id, meal: nil, participants: participants) { meal in
                meals.append(meal)
                showMealForm = false
            }
        }
        .onAppear(perform: loadMeals)
    }

    private func loadMeals() {
        let sharedMeals = MealRepository(db: RepositoryProvider.shared.database)
            .getMealsByEventId(eventId: event.id)
        meals = sharedMeals.map { meal in
            MealModel(
                id: meal.id,
                eventId: meal.eventId,
                type: MealType(rawValue: meal.type.name) ?? .lunch,
                name: meal.name,
                date: meal.date,
                time: meal.time,
                location: meal.location,
                responsibleParticipantIds: meal.responsibleParticipantIds,
                estimatedCost: meal.estimatedCost,
                actualCost: meal.actualCost?.int64Value,
                servings: Int(meal.servings),
                status: MealStatus(rawValue: meal.status.name) ?? .planned,
                notes: meal.notes,
                createdAt: meal.createdAt,
                updatedAt: meal.updatedAt
            )
        }
    }

    private func displayName(for type: MealType) -> String {
        switch type {
        case .breakfast: return String(localized: "meal.type.breakfast")
        case .lunch: return String(localized: "meal.type.lunch")
        case .dinner: return String(localized: "meal.type.dinner")
        case .snack: return String(localized: "meal.type.snack")
        case .aperitif: return String(localized: "meal.type.aperitif")
        }
    }
}

struct EquipmentChecklistRouteView: View {
    let event: Event
    let isOrganizer: Bool
    let isReadOnly: Bool
    let onBack: () -> Void
    let onOpenComments: () -> Void

    @State private var items: [EquipmentItem] = []

    var body: some View {
        EventPlanningListRouteView(
            title: String(localized: "events.equipment_checklist"),
            icon: "checklist",
            emptyTitle: String(localized: "equipment.empty.title"),
            emptyMessage: String(localized: "equipment.empty.subtitle"),
            isEmpty: items.isEmpty,
            isReadOnly: isReadOnly,
            pendingSync: hasPendingSync(eventId: event.id),
            onBack: onBack,
            onOpenComments: onOpenComments
        ) {
            ForEach(items, id: \.id) { item in
                EventRouteSummaryCard(
                    icon: "shippingbox",
                    title: item.name,
                    subtitle: String(describing: item.category),
                    detail: "\(item.quantity)x - \(String(describing: item.status))"
                )
            }
        }
        .onAppear {
            items = EquipmentRepository(db: RepositoryProvider.shared.database)
                .getEquipmentItemsByEventId(eventId: event.id)
        }
    }
}

struct ActivityPlanningRouteView: View {
    let event: Event
    let isOrganizer: Bool
    let isReadOnly: Bool
    let onBack: () -> Void
    let onOpenComments: () -> Void

    @State private var activities: [Activity_] = []

    var body: some View {
        EventPlanningListRouteView(
            title: String(localized: "events.activity_planning"),
            icon: "figure.socialdance",
            emptyTitle: String(localized: "activity.empty.title"),
            emptyMessage: String(localized: "activity.empty.subtitle"),
            isEmpty: activities.isEmpty,
            isReadOnly: isReadOnly,
            pendingSync: hasPendingSync(eventId: event.id),
            onBack: onBack,
            onOpenComments: onOpenComments
        ) {
            ForEach(activities, id: \.id) { activity in
                EventRouteSummaryCard(
                    icon: "sparkles",
                    title: activity.name,
                    subtitle: activity.location ?? activity.date ?? String(localized: "activity.date_pending"),
                    detail: String(format: String(localized: "activity.duration_format"), activity.duration)
                )
            }
        }
        .onAppear {
            activities = ActivityRepository(db: RepositoryProvider.shared.database)
                .getActivitiesByEventId(eventId: event.id)
        }
    }
}

struct EventCommentsRouteView: View {
    let event: Event
    let section: CommentSectionType
    let currentUserId: String
    let isOrganizer: Bool
    let onBack: () -> Void

    @State private var comments: [CommentThread] = []

    var body: some View {
        CommentListView(
            eventId: event.id,
            section: section,
            comments: comments,
            mentionableUsers: event.participants,
            currentUserId: currentUserId,
            isOrganizer: isOrganizer,
            onNavigateBack: onBack
        )
        .onAppear(perform: loadComments)
    }

    private func loadComments() {
        let repository = IosFactory.shared.createCommentRepository(database: RepositoryProvider.shared.database)
        let sharedSection = section.sharedValue
        let topLevel = repository.getTopLevelComments(eventId: event.id, section: sharedSection, sectionItemId: nil)
        comments = topLevel.compactMap { repository.getCommentThread(commentId: $0.id) }
    }
}

struct EventPhotosFollowUpRouteView: View {
    let event: Event
    let isReadOnly: Bool
    let onBack: () -> Void

    var body: some View {
        EventRouteUnavailableState(
            icon: "photo.on.rectangle.angled",
            title: String(localized: "photos.follow_up.title"),
            message: isReadOnly
                ? String(localized: "photos.follow_up.finalized_message")
                : String(localized: "photos.follow_up.message"),
            actionTitle: String(localized: "common.back"),
            action: onBack
        )
    }
}

struct OrganizerDashboardRouteView: View {
    let events: [Event]
    let currentUserId: String
    let onBack: () -> Void

    private var organizerEvents: [Event] {
        events.filter { $0.organizerId == currentUserId }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                WakeveScreenBackground(style: .grouped)
                ScrollView {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                        if organizerEvents.isEmpty {
                            EventRouteEmptyState(
                                icon: "person.2.badge.gearshape",
                                title: String(localized: "organizer.dashboard.empty_title"),
                                message: String(localized: "organizer.dashboard.empty_message")
                            )
                        } else {
                            ForEach(organizerEvents, id: \.id) { event in
                                EventRouteSummaryCard(
                                    icon: statusIcon(for: event),
                                    title: event.title,
                                    subtitle: event.status.localizedTitle,
                                    detail: organizerNextAction(for: event)
                                )
                            }
                        }
                    }
                    .padding(WakeveTheme.Spacing.page)
                }
            }
            .navigationTitle(String(localized: "organizer.dashboard.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                    }
                    .accessibilityLabel(String(localized: "common.back"))
                }
            }
        }
    }

    private func statusIcon(for event: Event) -> String {
        switch event.status {
        case .draft: return "pencil"
        case .polling: return "chart.bar"
        case .confirmed, .comparing: return "checkmark.circle"
        case .organizing: return "list.bullet.clipboard"
        case .finalized: return "checkmark.seal"
        default: return "calendar"
        }
    }

    private func organizerNextAction(for event: Event) -> String {
        switch event.status {
        case .draft:
            return String(localized: "organizer.dashboard.action_start_poll")
        case .polling:
            return String(localized: "organizer.dashboard.action_review_votes")
        case .confirmed, .comparing:
            return String(localized: "organizer.dashboard.action_choose_scenario")
        case .organizing:
            return String(localized: "organizer.dashboard.action_complete_logistics")
        case .finalized:
            return String(localized: "organizer.dashboard.action_ready")
        default:
            return String(localized: "organizer.dashboard.action_review")
        }
    }
}

private struct EventPlanningListRouteView<Content: View>: View {
    let title: String
    let icon: String
    let emptyTitle: String
    let emptyMessage: String
    let isEmpty: Bool
    let isReadOnly: Bool
    let pendingSync: Bool
    var primaryActionTitle: String?
    var onPrimaryAction: (() -> Void)?
    let onBack: () -> Void
    let onOpenComments: () -> Void
    @ViewBuilder let content: () -> Content

    var body: some View {
        NavigationStack {
            ZStack {
                WakeveScreenBackground(style: .grouped)

                ScrollView {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                        EventRouteStateHeader(
                            icon: icon,
                            title: title,
                            isReadOnly: isReadOnly,
                            pendingSync: pendingSync,
                            onOpenComments: onOpenComments
                        )

                        if isEmpty {
                            EventRouteEmptyState(icon: icon, title: emptyTitle, message: emptyMessage)
                        }

                        content()

                        if let primaryActionTitle, !isReadOnly {
                            WakeveActionButton(
                                primaryActionTitle,
                                systemImage: "plus",
                                variant: .primary,
                                action: { onPrimaryAction?() }
                            )
                        }
                    }
                    .padding(WakeveTheme.Spacing.page)
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                    }
                    .accessibilityLabel(String(localized: "common.back"))
                }
            }
        }
    }
}

private struct EventRouteStateHeader: View {
    let icon: String
    let title: String
    let isReadOnly: Bool
    let pendingSync: Bool
    let onOpenComments: () -> Void

    var body: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack {
                    Image(systemName: icon)
                        .font(.title2)
                        .foregroundStyle(WakeveTheme.ColorToken.permissionBlue)
                    Text(title)
                        .font(WakeveTheme.Typography.title2)
                    Spacer()
                    Button(action: onOpenComments) {
                        Image(systemName: "bubble.left.and.bubble.right")
                    }
                    .accessibilityLabel(String(localized: "comments.open"))
                }

                HStack(spacing: WakeveTheme.Spacing.sm) {
                    statusPill(
                        text: isReadOnly ? String(localized: "organization.state.read_only") : String(localized: "organization.state.editable"),
                        icon: isReadOnly ? "lock.fill" : "pencil"
                    )

                    if pendingSync {
                        statusPill(text: String(localized: "organization.state.pending_sync"), icon: "arrow.triangle.2.circlepath")
                    }
                }
            }
        }
    }

    private func statusPill(text: String, icon: String) -> some View {
        Label(text, systemImage: icon)
            .font(WakeveTheme.Typography.caption)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(WakeveTheme.ColorToken.permissionBlue.opacity(0.12), in: Capsule())
    }
}

private struct EventRouteSummaryCard: View {
    let icon: String
    let title: String
    let subtitle: String
    let detail: String

    var body: some View {
        WakeveContentCard(cornerRadius: WakeveTheme.Radius.lg) {
            HStack(alignment: .top, spacing: WakeveTheme.Spacing.md) {
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundStyle(WakeveTheme.ColorToken.permissionBlue)
                    .frame(width: 36, height: 36)
                    .background(WakeveTheme.ColorToken.permissionBlue.opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(WakeveTheme.Typography.bodySemibold)
                    Text(subtitle)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundStyle(.secondary)
                    Text(detail)
                        .font(WakeveTheme.Typography.caption)
                        .foregroundStyle(.secondary)
                }

                Spacer()
            }
        }
    }
}

private struct EventRouteEmptyState: View {
    let icon: String
    let title: String
    let message: String
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Image(systemName: icon)
                    .font(.title)
                    .foregroundStyle(WakeveTheme.ColorToken.permissionBlue)
                Text(title)
                    .font(WakeveTheme.Typography.section)
                Text(message)
                    .font(WakeveTheme.Typography.body)
                    .foregroundStyle(.secondary)
                if let actionTitle, let action {
                    WakeveActionButton(actionTitle, systemImage: "chevron.left", variant: .primary, action: action)
                }
            }
        }
    }
}

private struct EventRouteUnavailableState: View {
    let icon: String
    let title: String
    let message: String
    let actionTitle: String
    let action: () -> Void

    var body: some View {
        NavigationStack {
            ZStack {
                WakeveScreenBackground(style: .grouped)
                EventRouteEmptyState(icon: icon, title: title, message: message, actionTitle: actionTitle, action: action)
                    .padding(WakeveTheme.Spacing.page)
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

private func hasPendingSync(eventId: String) -> Bool {
    !RepositoryProvider.shared.repository.getWorkflowOutbox(eventId: eventId).isEmpty
}

private extension EventStatus {
    var localizedTitle: String {
        switch self {
        case .draft: return String(localized: "events.status.draft_preview")
        case .polling: return String(localized: "events.status.polling")
        case .confirmed: return String(localized: "events.status.confirmed")
        case .comparing: return String(localized: "events.status.comparing")
        case .organizing: return String(localized: "events.status.organizing")
        case .finalized: return String(localized: "events.status.finalized")
        default: return String(describing: self)
        }
    }
}
