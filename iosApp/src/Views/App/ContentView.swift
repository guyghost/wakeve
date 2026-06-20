import SwiftUI
import Shared
#if canImport(UIKit)
import UIKit
#endif

// MARK: - UserDefaults Helpers

struct UserDefaultsKeys {
    static let hasCompletedOnboarding = "hasCompletedOnboarding"
}

func hasCompletedOnboarding() -> Bool {
    return UserDefaults.standard.bool(forKey: UserDefaultsKeys.hasCompletedOnboarding)
}

func markOnboardingComplete() {
    UserDefaults.standard.set(true, forKey: UserDefaultsKeys.hasCompletedOnboarding)
}

struct ContentView: View {
    @EnvironmentObject var authStateManager: AuthStateManager
    @EnvironmentObject var authService: AuthenticationService
    @AppStorage("darkMode") private var darkMode = true
    @State private var hasOnboarded = false

    var body: some View {
        ZStack {
            if !authStateManager.hasCheckedAuthStatus {
                AuthLaunchLoadingView()
            } else if authStateManager.isAuthenticated {
                if let user = authStateManager.currentUser {
                    AuthenticatedView(userId: user.id)
                } else {
                    ErrorView(message: "Authentication error: no user data", onRetry: {
                        Task {
                            authStateManager.checkAuthStatus()
                        }
                    })
                }
            } else if !hasOnboarded {
                OnboardingView(onOnboardingComplete: completeOnboarding)
            } else {
                LoginView()
            }
        }
        .onAppear {
            hasOnboarded = hasCompletedOnboarding()
        }
        .preferredColorScheme(darkMode ? .dark : .light)
    }

    private func completeOnboarding() {
        markOnboardingComplete()

        withAnimation(.easeInOut(duration: 0.25)) {
            hasOnboarded = true
        }
    }
}

private struct AuthLaunchLoadingView: View {
    var body: some View {
        ZStack {
            WakeveScreenBackground(style: .utility)

            ProgressView()
                .tint(WakeveTheme.ColorToken.permissionBlue)
                .scaleEffect(1.2)
                .accessibilityLabel(String(localized: "common.loading"))
        }
        .ignoresSafeArea()
    }
}

// MARK: - Error View

struct ErrorView: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        ZStack {
            LinearGradient(
                gradient: Gradient(colors: [
                    Color.blue.opacity(0.1),
                    Color.purple.opacity(0.1),
                    Color.pink.opacity(0.1)
                ]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 24) {
                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 60))
                    .foregroundColor(.red.opacity(0.8))

                Text(String(localized: "common.error_generic"))
                    .font(.title2)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)

                Text(message)
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)

                Button(action: onRetry) {
                    Text(String(localized: "common.try_again"))
                        .font(.headline)
                        .foregroundColor(.white)
                        .padding(.horizontal, 32)
                        .padding(.vertical, 16)
                        .background(Color.blue)
                        .cornerRadius(12)
                }
            }
        }
    }
}

// MARK: - Authenticated View

struct AuthenticatedView: View {
    let userId: String
    @State private var selectedTab: WakeveTab = .home
    @State private var currentView: AppView = .eventList
    @State private var selectedEvent: Event?
    @State private var invitationLandingEventId: String?
    // Use persistent database-backed repository instead of in-memory mock
    private let repository: EventRepositoryInterface = RepositoryProvider.shared.repository
    @State private var showEventCreationSheet = false
    @State private var eventCreationScenario: EventScenario?
    @State private var preparedCreationChecklists: [String: [ChecklistItem]] = [:]
    
    // New state variables for PRD features
    @State private var showScenarioList = false
    @State private var showBudgetDetail = false
    @State private var showAccommodation = false
    @State private var showMealPlanning = false
    @State private var showEquipmentChecklist = false
    @State private var showActivityPlanning = false
    @State private var selectedMeetingId: String?
    @State private var selectedBudget: Budget_?
    @StateObject private var transportPlanningViewModel = TransportPlanningViewModel()

    // Notifications state
    @State private var showNotificationPreferencesSheet = false
    @State private var unreadInboxCount: Int = 0

    // Get auth state from environment
    @EnvironmentObject var authStateManager: AuthStateManager
    @EnvironmentObject private var deepLinkService: DeepLinkService

    var body: some View {
        // Main tabs are destinations only. One-off actions such as Create Event
        // stay contextual in Home, toolbars, or sheets.
        TabView(selection: $selectedTab) {
            tabContent(for: .home)
                .tabItem {
                    Label(WakeveTab.home.title, systemImage: WakeveTab.home.systemImage)
                }
                .tag(WakeveTab.home)

            tabContent(for: .groups)
                .tabItem {
                    Label(WakeveTab.groups.title, systemImage: WakeveTab.groups.systemImage)
                }
                .tag(WakeveTab.groups)

            tabContent(for: .messages)
                .tabItem {
                    Label(WakeveTab.messages.title, systemImage: WakeveTab.messages.systemImage)
                }
                .tag(WakeveTab.messages)
                .badge(unreadInboxCount)

            tabContent(for: .profile)
                .tabItem {
                    Label(WakeveTab.profile.title, systemImage: WakeveTab.profile.systemImage)
                }
                .tag(WakeveTab.profile)
        }
        .tint(.wakevePrimary)
        .toolbar(tabBarVisibility, for: .tabBar)
        .fullScreenCover(isPresented: $showEventCreationSheet) {
            CreateEventSheet(
                userId: userId,
                userName: authStateManager.currentUser?.name,
                initialScenario: eventCreationScenario
            ) { event, context in
                // Save event to repository
                Task {
                    do {
                        try await repository.saveEvent(event: event)
                        persistCreationContext(context, for: event)
                        await MainActor.run {
                            // Navigate to participant management
                            selectedEvent = event
                            selectedTab = .home
                            currentView = event.planningMode == .scenarioMatrix ? .scenarioList : .participantManagement
                            eventCreationScenario = nil
                        }
                    } catch {
                        debugLog("Failed to save event: \(error)")
                    }
                }
            }
        }
        .sheet(isPresented: $showNotificationPreferencesSheet) {
            NavigationStack {
                NotificationPreferencesView(userId: userId)
            }
        }
        .onReceive(deepLinkService.$navigationPath) { path in
            handleDeepLinkNavigation(path)
        }
    }

    private var tabBarVisibility: Visibility {
        selectedTab == .home && currentView != .eventList ? .hidden : .visible
    }
    
    // MARK: - Home Tab
    
    @ViewBuilder
    private var homeTabContent: some View {
        switch currentView {
        case .eventList:
            HomeView(
                userId: userId,
                repository: repository,
                onEventSelected: { event in
                    selectedEvent = event
                    currentView = .eventDetail
                },
                onCreateEvent: {
                    // Show bottom sheet instead of navigating
                    eventCreationScenario = nil
                    showEventCreationSheet = true
                },
                onProfileClick: {
                    selectedTab = .profile
                }
            )
            
        case .eventCreation:
            Text(String(localized: "navigation.placeholder.event_creation"))
                .font(.title2)
                .foregroundColor(.secondary)
            
        case .eventDetail:
            if let event = selectedEvent {
                EventDetailView(
                    event: event,
                    repository: repository,
                    userId: userId,
                    preparedCreationChecklist: preparedCreationChecklists[event.id] ?? [],
                    onManageParticipants: {
                        currentView = .participantManagement
                    },
                    onVote: {
                        currentView = .pollVoting
                    },
                    onViewResults: {
                        currentView = .pollResults
                    },
                    onOrganize: {
                        currentView = .scenarioList
                    },
                    onOpenTransport: {
                        currentView = .transportPlanning
                    },
                    onOpenMeetings: {
                        currentView = .meetingList
                    },
                    onOpenBudget: {
                        currentView = .budgetOverview
                    },
                    onOpenPayment: {
                        currentView = .paymentPot
                    },
                    onOpenTricount: {
                        currentView = .tricount
                    },
                    isInvitationLanding: invitationLandingEventId == event.id,
                    onDismissInvitationLanding: {
                        invitationLandingEventId = nil
                    },
                    onBack: {
                        invitationLandingEventId = nil
                        currentView = .eventList
                    }
                )
            }
            
        case .participantManagement:
            if let event = selectedEvent {
                ParticipantManagementView(
                    event: event,
                    repository: repository,
                    onParticipantsUpdated: {
                        // Refresh the event data
                        if let updatedEvent = repository.getEvent(id: event.id) {
                            selectedEvent = updatedEvent
                        }
                    },
                    onBack: {
                        currentView = .eventDetail
                    }
                )
            }
            
        case .pollVoting:
            if let event = selectedEvent {
                PollVotingView(
                    event: event,
                    repository: repository,
                    participantId: userId,
                    onVoteSubmitted: {
                        currentView = .eventDetail
                    },
                    onBack: {
                        currentView = .eventDetail
                    }
                )
            }
            
        case .pollResults:
            if let event = selectedEvent {
                PollResultsView(
                    event: event,
                    repository: repository,
                    userId: userId,
                    onDateConfirmed: { _ in
                        currentView = .eventDetail
                    },
                    onBack: {
                        currentView = .eventDetail
                    }
                )
            }
            
        // MARK: - New PRD Features Navigation Cases
            
        case .scenarioList:
            if let event = selectedEvent {
                ScenarioOrganizationView(
                    event: event,
                    participantId: userId,
                    repository: repository,
                    onBack: {
                        currentView = .eventDetail
                    },
                    onOpenMeetings: {
                        if let updatedEvent = repository.getEvent(id: event.id) {
                            selectedEvent = updatedEvent
                        }
                        currentView = .meetingList
                    },
                    onOpenTransport: {
                        if let updatedEvent = repository.getEvent(id: event.id) {
                            selectedEvent = updatedEvent
                        }
                        currentView = .transportPlanning
                    }
                )
            } else {
                Text(String(localized: "navigation.placeholder.select_event_options"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .scenarioComparison:
            if let event = selectedEvent {
                ScenarioOrganizationView(
                    event: event,
                    participantId: userId,
                    repository: repository,
                    onBack: {
                        currentView = .eventDetail
                    },
                    onOpenMeetings: {
                        currentView = .meetingList
                    },
                    onOpenTransport: {
                        currentView = .transportPlanning
                    }
                )
            } else {
                Text(String(localized: "navigation.placeholder.select_event_compare_options"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .budgetOverview:
            if let event = selectedEvent {
                if canAccessOrganizationDashboard(for: event) {
                    BudgetOverviewView(eventId: event.id)
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_budget")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_budget"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .accommodation:
            if let event = selectedEvent {
                ScenarioOrganizationView(
                    event: event,
                    participantId: userId,
                    repository: repository,
                    onBack: {
                        currentView = .eventDetail
                    },
                    onOpenMeetings: {
                        currentView = .meetingList
                    },
                    onOpenTransport: {
                        currentView = .transportPlanning
                    }
                )
            } else {
                Text(String(localized: "navigation.placeholder.select_event_accommodation"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .mealPlanning:
            if selectedEvent != nil {
                // Meal planning stays behind a placeholder until shared types are integrated.
                Text(String(localized: "events.meal_planning"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .equipmentChecklist:
            if selectedEvent != nil {
                Text(String(localized: "events.equipment_checklist"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .activityPlanning:
            Text(String(localized: "events.activity_planning"))
                .font(.title2)
                .foregroundColor(.secondary)
            
        case .scenarioDetail:
            Text(String(localized: "navigation.placeholder.scenario_detail"))
                .font(.title2)
                .foregroundColor(.secondary)
            
        case .budgetDetail:
            if let event = selectedEvent {
                if canAccessOrganizationDashboard(for: event) {
                    BudgetDetailView(eventId: event.id)
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_expenses")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_expenses"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .meetingList:
            if let event = selectedEvent {
                if canAccessOrganizationDashboard(for: event) {
                    MeetingListView(
                        eventId: event.id,
                        currentUserId: userId,
                        isOrganizer: event.organizerId == userId,
                        canCreateMeetings: event.organizerId == userId && event.status == .organizing,
                        isReadOnly: isFinalizedOrganizationState(event)
                    )
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_meetings")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_meetings"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .meetingDetail:
            if let meetingId = selectedMeetingId, let event = selectedEvent {
                if canAccessOrganizationDashboard(for: event) {
                    MeetingDetailView(
                        meetingId: meetingId,
                        eventId: event.id,
                        currentUserId: userId,
                        isOrganizer: event.organizerId == userId,
                        isReadOnly: isFinalizedOrganizationState(event)
                    )
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_meeting_detail")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_meeting"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .paymentPot:
            if let event = selectedEvent {
                let canAccessPhase5Organization = (event.status == .organizing || event.status == .finalized) && canAccessOrganizationDetails(for: event)
                if canAccessPhase5Organization {
                    let canManagePayment = event.status == .organizing && event.organizerId == userId
                    PaymentPotView(
                        eventId: event.id,
                        eventTitle: event.title,
                        currentUserId: userId,
                        isOrganizer: event.organizerId == userId,
                        canManagePayment: canManagePayment,
                        isReadOnly: isFinalizedOrganizationState(event)
                    )
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_payment_pot")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_payment_pot"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .tricount:
            if let event = selectedEvent {
                let canAccessPhase5Organization = (event.status == .organizing || event.status == .finalized) && canAccessOrganizationDetails(for: event)
                if canAccessPhase5Organization {
                    let canManageTricount = event.status == .organizing && event.organizerId == userId
                    TricountHandoffView(
                        eventId: event.id,
                        currentUserId: userId,
                        isOrganizer: event.organizerId == userId,
                        canManageTricount: canManageTricount,
                        isReadOnly: isFinalizedOrganizationState(event)
                    )
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_tricount")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_tricount"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .transportPlanning:
            if let event = selectedEvent {
                if canAccessTransportPlanning(for: event) {
                    let selectedDestination = transportDestination(for: event)
                    let transportPlanningAdapter: TransportPlanningViewModel = transportPlanningViewModel
                    let transportState = transportPlanningViewModel.state.eventId == event.id
                        ? transportPlanningViewModel.state
                        : makeTransportPlanningState(for: event)
                    TransportPlanningView(
                        event: event,
                        isOrganizer: event.organizerId == userId,
                        isParticipantConfirmed: isParticipantConfirmed(for: event),
                        isReadOnly: isFinalizedOrganizationState(event),
                        eventStatus: event.status,
                        confirmedDate: event.finalDate,
                        selectedDestination: selectedDestination,
                        readiness: transportState.readiness,
                        missingDeparture: transportState.missingDeparture,
                        plans: transportState.plans,
                        selectedPlanId: transportState.selectedPlanId,
                        pendingSync: transportState.pendingSync,
                        onGenerate: { optimization in
                            transportPlanningAdapter.generate(
                                event: event,
                                userId: userId,
                                repository: repository,
                                selectedDestination: selectedDestination,
                                optimization: optimization
                            )
                        },
                        onSelectFinalPlan: { plan in
                            transportPlanningAdapter.selectFinalPlan(
                                event: event,
                                userId: userId,
                                repository: repository,
                                selectedDestination: selectedDestination,
                                plan: plan
                            )
                        },
                        onMarkTransportNotNeeded: {
                            transportPlanningAdapter.markTransportNotNeeded(
                                event: event,
                                userId: userId,
                                repository: repository,
                                selectedDestination: selectedDestination
                            )
                        },
                        onSaveDepartureLocation: { location in
                            transportPlanningAdapter.saveDepartureLocation(
                                event: event,
                                userId: userId,
                                repository: repository,
                                selectedDestination: selectedDestination,
                                location: location
                            )
                        },
                        onChooseDestination: {
                            currentView = .scenarioList
                        },
                        onBack: {
                            currentView = .eventDetail
                        }
                    )
                    .onAppear {
                        transportPlanningViewModel.load(
                            event: event,
                            userId: userId,
                            repository: repository,
                            selectedDestination: selectedDestination
                        )
                    }
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_transport")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_transport"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .inbox:
            InboxView(
                userId: userId,
                onBack: { /* Inbox handled by tab */ },
                unreadCount: $unreadInboxCount
            )
        }
    }
    
    // MARK: - Tab Content
    
    @ViewBuilder
    private func tabContent(for tab: WakeveTab) -> some View {
        switch tab {
        case .home:
            homeTabContent
        case .groups:
            ExploreTabView { scenario in
                eventCreationScenario = scenario
                showEventCreationSheet = true
            }
        case .messages:
            InboxView(
                userId: userId,
                onBack: { /* Messages is a main tab, no back action needed */ },
                unreadCount: $unreadInboxCount
            )
        case .profile:
            ProfileTabView(
                userId: userId,
                userName: authStateManager.currentUser?.name,
                userEmail: authStateManager.currentUser?.email,
                onDismiss: nil,
                onSignOut: {
                    authStateManager.signOut()
                }
            )
        }
    }

    private func persistCreationContext(_ context: EventCreationContext, for event: Event) {
        if !context.preparedChecklist.isEmpty {
            preparedCreationChecklists[event.id] = context.preparedChecklist
        }

        guard let locationName = context.potentialLocationName else { return }

        let existingLocations = RepositoryProvider.shared.database.potentialLocationQueries
            .selectByEventId(eventId: event.id)
            .executeAsList()

        guard !existingLocations.contains(where: { $0.name.caseInsensitiveCompare(locationName) == .orderedSame }) else {
            return
        }

        let now = ISO8601DateFormatter().string(from: Date())
        RepositoryProvider.shared.database.potentialLocationQueries.insertLocation(
            id: "location-\(UUID().uuidString.prefix(8))",
            eventId: event.id,
            name: locationName,
            locationType: "SPECIFIC_VENUE",
            address: nil,
            coordinates: nil,
            createdAt: now
        )
    }

    private func handleDeepLinkNavigation(_ path: [String]) {
        guard !path.isEmpty else { return }

        let route = path[0]
        let identifier = path.count > 1 ? path[1] : nil
        let subroute = path.count > 2 ? path[2] : nil

        switch (route, identifier, subroute) {
        case ("event", let eventId?, nil):
            invitationLandingEventId = nil
            navigateToEvent(eventId: eventId, destination: .eventDetail)
        case ("event", let eventId?, "poll"):
            invitationLandingEventId = nil
            navigateToEvent(eventId: eventId, destination: .pollVoting)
        case ("meeting", let meetingId?, _):
            invitationLandingEventId = nil
            navigateToMeeting(meetingId: meetingId)
        case ("invite", let token?, _):
            if let eventId = InvitationTokenCodec.eventId(fromInvitationCode: token) {
                invitationLandingEventId = eventId
                navigateToEvent(eventId: eventId, destination: .eventDetail)
            } else {
                invitationLandingEventId = nil
                selectedTab = .home
                currentView = .eventList
            }
        default:
            break
        }

        deepLinkService.clearPendingInvite()
        deepLinkService.clearPendingDeepLink()
        deepLinkService.resetNavigation()
    }

    private func navigateToEvent(eventId: String, destination: AppView) {
        guard let event = repository.getEvent(id: eventId) else {
            selectedTab = .home
            currentView = .eventList
            return
        }

        selectedEvent = event
        selectedTab = .home

        if destination == .pollVoting, event.status != .polling {
            currentView = .eventDetail
        } else {
            currentView = destination
        }
    }

    private func navigateToMeeting(meetingId: String) {
        guard let meeting = RepositoryProvider.shared.database.meetingQueries
            .selectById(id: meetingId)
            .executeAsOneOrNull(),
            let event = repository.getEvent(id: meeting.eventId)
        else {
            selectedTab = .home
            currentView = .eventList
            selectedMeetingId = nil
            return
        }

        selectedMeetingId = meetingId
        selectedEvent = event
        selectedTab = .home
        currentView = .meetingDetail
    }

    private func isParticipantConfirmed(for event: Event) -> Bool? {
        if event.organizerId == userId {
            return true
        }

        guard let participantRecords = repository.getParticipantRecords(eventId: event.id), !participantRecords.isEmpty else {
            return false
        }

        let participantAccessStates = participantRecords.map { record in
            ParticipantAccessMapper.shared.fromRepositoryRecord(record: record)
        }
        let rows = ParticipantManagementPresentationMapper.shared.map(participants: participantAccessStates)
        return rows.first { $0.userIdOrEmail == userId }?.canAccessOrganizationDetails ?? false
    }

    private func canAccessOrganizationDetails(for event: Event) -> Bool {
        event.organizerId == userId || isParticipantConfirmed(for: event) == true
    }

    private func canAccessOrganizationDashboard(for event: Event) -> Bool {
        switch event.status {
        case .organizing, .finalized:
            return canAccessOrganizationDetails(for: event)
        default:
            return false
        }
    }

    private func canAccessTransportPlanning(for event: Event) -> Bool {
        switch event.status {
        case .confirmed, .organizing, .finalized:
            return canAccessOrganizationDetails(for: event)
        default:
            return false
        }
    }

    private func makeTransportPlanningState(for event: Event) -> TransportPlanningPresentationState {
        transportPlanningViewModel.makeState(
            event: event,
            userId: userId,
            repository: repository,
            selectedDestination: transportDestination(for: event)
        )
    }

    private func transportDestination(for event: Event) -> TransportLocation? {
        guard let selectedScenario = loadSelectedScenario(for: event),
              selectedScenario.status == ScenarioStatus.selected,
              let location = selectedScenario.location.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
        else {
            return nil
        }

        return TransportLocation(
            name: location,
            address: nil,
            latitude: nil,
            longitude: nil,
            iataCode: nil
        )
    }

    private func loadSelectedScenario(for event: Event) -> Scenario_? {
        let selectedScenarioRepository = ScenarioRepository(db: RepositoryProvider.shared.database)
        if let selected = selectedScenarioRepository.getSelectedScenario(eventId: event.id),
           selected.status == ScenarioStatus.selected {
            return selected
        }

        return selectedScenarioRepository
            .getScenariosByEventIdAndStatus(eventId: event.id, status: ScenarioStatus.selected)
            .first
    }
}

private extension String {
    var nilIfEmpty: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty else {
            return nil
        }

        return value
    }
}

private enum OrganizationUXLabels {
    static let sectionKeys = [
        "organization.section.participants",
        "organization.section.scenario",
        "organization.section.destination",
        "organization.section.lodging",
        "organization.section.transport",
        "organization.section.meetings",
        "organization.section.calendar",
        "organization.section.notifications",
        "organization.section.budget",
        "organization.section.payment",
        "organization.section.tricount",
        "organization.section.sync",
        "organization.section.unsafe_links",
        "organization.section.access_control"
    ]
    static let stateKeys = [
        "organization.state.empty",
        "organization.state.optional_not_needed",
        "organization.state.incomplete",
        "organization.state.complete",
        "organization.state.pending_sync",
        "organization.state.failed_sync",
        "organization.state.access_denied"
    ]
    static let accessDenied = "AccessDenied confirmationRequired access denied Confirm your attendance"
    static let optionalNotNeeded = "NotNeeded optional not needed non requis"
    static let incomplete = "Incomplete missing required"
    static let complete = "Complete ready"
    static let pendingSync = "PendingSync queued local-first queued for sync not server confirmed pending server confirmation"
    static let failedSync = "FailedSync retry conflict ConflictDetected resolveConflict"
    static let finalizedReadOnly = "Finalized ReadOnly readOnly viewOnly mutationsDisabled"
}

private func isFinalizedOrganizationState(_ event: Event) -> Bool {
    let readOnly = event.status == EventStatus.finalized
    let viewOnly = readOnly
    let mutationsDisabled = viewOnly
    return mutationsDisabled
}

// MARK: - Explore Tab View

// ProfileTabView is now in its own file: Views/Profile/ProfileTabView.swift

enum AppView {
    case eventList
    case eventCreation
    case eventDetail
    case participantManagement
    case pollVoting
    case pollResults
    // New PRD features
    case scenarioList
    case scenarioDetail
    case scenarioComparison
    case budgetOverview
    case budgetDetail
    case accommodation
    case mealPlanning
    case equipmentChecklist
    case activityPlanning
    // Phase 4 - Meetings & Communication
    case inbox
    case meetingList
    case meetingDetail
    case transportPlanning
    case paymentPot
    case tricount
}

private struct AccessDenied: View {
    let message: String
    let onBack: () -> Void

    var body: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.xl) {
            VStack(spacing: WakeveTheme.Spacing.md) {
                Image(systemName: "lock.fill")
                    .font(.largeTitle)
                    .foregroundStyle(WakeveTheme.ColorToken.permissionBlue)
                Text(String(localized: "access_denied.title"))
                    .font(WakeveTheme.Typography.title2)
                Text(message)
                    .font(WakeveTheme.Typography.body)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
                WakeveActionButton(
                    String(localized: "common.back"),
                    systemImage: "chevron.left",
                    variant: .primary,
                    action: onBack
                )
            }
        }
        .padding(WakeveTheme.Spacing.page)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(WakeveScreenBackground(style: .grouped))
    }
}

private struct Phase5SyncBanner: View {
    let pendingSync: Bool
    let isOnline: Bool

    var body: some View {
        if pendingSync || !isOnline {
            WakeveContentCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.lg, padding: WakeveTheme.Spacing.md) {
                Label(
                    pendingSync ? String(localized: "sync.pending_changes") : String(localized: "sync.offline_available"),
                    systemImage: "arrow.triangle.2.circlepath"
                )
                .font(WakeveTheme.Typography.metadata.weight(.semibold))
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

private struct PaymentTrustChecklistCard: View {
    let title: String
    let items: [PaymentTrustChecklistItem]

    var body: some View {
        WakeveContentCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                Label(title, systemImage: "checkmark.shield.fill")
                    .font(WakeveTheme.Typography.section)

                ForEach(items) { item in
                    HStack(alignment: .top, spacing: WakeveTheme.Spacing.sm) {
                        Image(systemName: item.systemImage)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(.green)
                            .frame(width: 24, height: 24)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.title)
                                .font(WakeveTheme.Typography.bodySemibold)
                            Text(item.detail)
                                .font(WakeveTheme.Typography.caption)
                                .foregroundStyle(.secondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                }
            }
        }
    }
}

private struct PaymentTrustChecklistItem: Identifiable {
    let id: String
    let systemImage: String
    let title: String
    let detail: String
}

private func formatCurrencyAmount(_ amount: Double, currency: String) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .currency
    formatter.currencyCode = currency
    formatter.maximumFractionDigits = amount.rounded() == amount ? 0 : 2
    return formatter.string(from: NSNumber(value: amount)) ?? "\(currency) \(amount)"
}

private struct PaymentPotView: View {
    let eventId: String
    let eventTitle: String
    let currentUserId: String
    let isOrganizer: Bool
    let isReadOnly: Bool
    private let canManagePayment: Bool
    private let pendingSync: Bool
    @State private var activePot: PaymentPotRecord?
    @State private var goalAmountText: String
    @State private var statusText: String

    init(eventId: String, eventTitle: String, currentUserId: String, isOrganizer: Bool, canManagePayment: Bool, isReadOnly: Bool = false) {
        self.eventId = eventId
        self.eventTitle = eventTitle
        self.currentUserId = currentUserId
        self.isOrganizer = isOrganizer
        self.isReadOnly = isReadOnly
        self.canManagePayment = canManagePayment
        self.pendingSync = Phase5PendingSync.selectPending(eventId: eventId)
        let existingPot = Self.loadActivePot(eventId: eventId)
        self._activePot = State(initialValue: existingPot)
        self._goalAmountText = State(initialValue: Self.initialGoalAmountText(for: existingPot))
        self._statusText = State(initialValue: Self.statusText(for: existingPot))
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                Phase5SyncBanner(pendingSync: pendingSync, isOnline: !pendingSync)

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                    WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                            Label(String(localized: "payment.shared_title"), systemImage: "creditcard.fill")
                                .font(WakeveTheme.Typography.section)

                            Text(statusText)
                                .font(WakeveTheme.Typography.callout)
                                .foregroundStyle(.secondary)

                            if let activePot {
                                HStack {
                                    PriceDisplay(amount: activePot.currentAmount, currency: activePot.currency, style: .large)
                                    Text(String(format: String(localized: "payment.goal_ratio_format"), formatCurrencyAmount(activePot.goalAmount, currency: activePot.currency)))
                                        .font(WakeveTheme.Typography.callout)
                                        .foregroundStyle(.secondary)
                                }
                            } else {
                                Text(String(localized: "payment.no_active_body"))
                                    .font(WakeveTheme.Typography.callout)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }

                    PaymentTrustChecklistCard(
                        title: String(localized: "payment.trust.title"),
                        items: [
                            PaymentTrustChecklistItem(
                                id: "positive-goal",
                                systemImage: "number.circle.fill",
                                title: String(localized: "payment.trust.goal_title"),
                                detail: String(localized: "payment.trust.goal_detail")
                            ),
                            PaymentTrustChecklistItem(
                                id: "local-sync",
                                systemImage: "arrow.triangle.2.circlepath.circle.fill",
                                title: String(localized: "payment.trust.sync_title"),
                                detail: String(localized: "payment.trust.sync_detail")
                            ),
                            PaymentTrustChecklistItem(
                                id: "verified-links",
                                systemImage: "lock.shield.fill",
                                title: String(localized: "payment.trust.link_title"),
                                detail: String(localized: "payment.trust.link_detail")
                            )
                        ]
                    )

                    WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                            Text(String(localized: "payment.goal_section"))
                                .font(WakeveTheme.Typography.section)

                            TextField(String(localized: "payment.goal_placeholder"), text: $goalAmountText)
                                .keyboardType(.decimalPad)
                                .padding(.horizontal, WakeveTheme.Spacing.md)
                                .frame(height: 52)
                                .background(.thinMaterial, in: RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
                                .disabled(!canManagePayment || activePot != nil)
                                .accessibilityLabel(String(localized: "payment.goal_section"))

                            Text(goalAmountHelpText)
                                .font(WakeveTheme.Typography.caption)
                                .foregroundStyle(.secondary)
                        }
                    }

                    WakeveContentCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                            Text(String(localized: "payment.actions"))
                                .font(WakeveTheme.Typography.section)

                            WakeveActionButton(
                                String(localized: "payment.create_pot"),
                                systemImage: "plus.circle.fill",
                                variant: .primary,
                                isDisabled: !canCreateConfiguredPot
                            ) {
                                onCreatePaymentPot()
                            }

                            WakeveActionButton(
                                String(localized: "payment.activate_pot"),
                                systemImage: "checkmark.seal.fill",
                                variant: .secondary,
                                isDisabled: activePot == nil && !canCreateConfiguredPot
                            ) {
                                onActivatePaymentPot()
                            }

                            WakeveActionButton(
                                String(localized: "payment.open_pot"),
                                systemImage: "arrow.up.right.square",
                                variant: .neutral,
                                isDisabled: activePot == nil
                            ) {
                                onOpenPaymentPot()
                            }

                            WakeveActionButton(
                                String(localized: "payment.close_pot"),
                                systemImage: "xmark.circle",
                                variant: .destructive,
                                isDisabled: !canManagePayment || activePot == nil
                            ) {
                                onClosePaymentPot()
                            }
                        }
                    }
                }
                .padding(WakeveTheme.Spacing.md)
            }
            .background(WakeveScreenBackground(style: .grouped))
            .navigationTitle(String(localized: "payment.title"))
        }
    }

    private var parsedGoalAmount: Double? {
        let normalized = goalAmountText
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: ",", with: ".")
        return Double(normalized)
    }

    private var canCreateConfiguredPot: Bool {
        canManagePayment && activePot == nil && (parsedGoalAmount ?? 0) > 0
    }

    private var goalAmountHelpText: String {
        if activePot != nil {
            return String(localized: "payment.help.locked")
        }

        if !canManagePayment {
            return isReadOnly ? String(localized: "payment.help.read_only") : String(localized: "payment.help.organizer_only")
        }

        return String(localized: "payment.help.positive_required")
    }

    private static func loadActivePot(eventId: String) -> PaymentPotRecord? {
        let repository = PaymentPotRepository(db: RepositoryProvider.shared.database)
        return repository.getActivePotForEvent(eventId: eventId)
    }

    private static func initialGoalAmountText(for pot: PaymentPotRecord?) -> String {
        guard let pot, pot.goalAmount > 0 else { return "" }
        return decimalText(pot.goalAmount)
    }

    private static func statusText(for pot: PaymentPotRecord?) -> String {
        guard let pot else { return String(localized: "payment.status.none") }
        return String(format: String(localized: "payment.status.active_format"), formatCurrencyAmount(pot.goalAmount, currency: pot.currency))
    }

    private static func decimalText(_ amount: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 0
        formatter.usesGroupingSeparator = false
        return formatter.string(from: NSNumber(value: amount)) ?? "\(amount)"
    }

    private func onCreatePaymentPot() {
        guard canCreateConfiguredPot, let goalAmount = parsedGoalAmount else {
            statusText = String(localized: "payment.status.create_requires_positive")
            return
        }

        let repository = PaymentPotRepository(db: RepositoryProvider.shared.database)
        let pot = repository.createPot(
            eventId: eventId,
            organizerId: currentUserId,
            goalAmount: goalAmount,
            title: String(format: String(localized: "payment.default_title_format"), eventTitle),
            currency: "EUR",
            paymentProvider: "WAKEVE_LOCAL",
            tricountGroupId: nil,
            tricountGroupUrl: nil
        )
        activePot = pot
        statusText = String(format: String(localized: "payment.status.created_pending_format"), formatCurrencyAmount(pot.goalAmount, currency: pot.currency))
    }

    private func onActivatePaymentPot() {
        if activePot == nil {
            onCreatePaymentPot()
        } else {
            statusText = String(localized: "payment.status.ready_to_share")
        }
    }

    private func onOpenPaymentPot() {
        guard let activePot else {
            statusText = String(localized: "payment.status.none_to_open")
            return
        }

        if let url = activePot.tricountGroupUrl, !url.isEmpty {
            statusText = String(localized: "payment.status.provider_verified")
        } else {
            statusText = String(localized: "payment.status.local_only")
        }
    }

    private func onClosePaymentPot() {
        guard canManagePayment else { return }
        let repository = PaymentPotRepository(db: RepositoryProvider.shared.database)
        if let pot = activePot ?? repository.getActivePotForEvent(eventId: eventId),
           repository.closePot(id: pot.id) != nil {
            activePot = nil
            goalAmountText = ""
            statusText = String(localized: "payment.status.closed_pending")
        }
    }
}

private struct TricountHandoffView: View {
    let eventId: String
    let currentUserId: String
    let isOrganizer: Bool
    let isReadOnly: Bool
    private let canManageTricount: Bool
    private let pendingSync: Bool
    @Environment(\.openURL) private var openURL
    @State private var safeLink: SafeExternalLink?
    @State private var handoffStatusText: String
    @State private var tricountURLText: String

    init(eventId: String, currentUserId: String, isOrganizer: Bool, canManageTricount: Bool, isReadOnly: Bool = false) {
        self.eventId = eventId
        self.currentUserId = currentUserId
        self.isOrganizer = isOrganizer
        self.isReadOnly = isReadOnly
        self.canManageTricount = canManageTricount
        self.pendingSync = Phase5PendingSync.selectPending(eventId: eventId)
        self._safeLink = State(initialValue: Self.loadSafeLink(eventId: eventId))
        self._handoffStatusText = State(initialValue: Self.loadStatusText(eventId: eventId))
        self._tricountURLText = State(initialValue: Self.initialTricountURLText(eventId: eventId))
    }

    private static func loadSafeLink(eventId: String) -> SafeExternalLink? {
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        let handoff = repository.getHandoff(eventId: eventId)
        return handoff?.providerUrl.flatMap { rawUrl in
            SafeExternalLink.sanitize(
                label: String(localized: "tricount.open"),
                provider: "TRICOUNT",
                rawURL: rawUrl,
                verifier: { provider, url in
                    repository.isTrustedProviderUrl(provider: provider, providerUrl: url)
                }
            )
        }
    }

    private static func loadStatusText(eventId: String) -> String {
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        let readiness = repository.getPaymentReadiness(eventId: eventId)

        if readiness.handoff?.explicitNotNeeded == true {
            return String(localized: "tricount.status.not_needed")
        }

        if readiness.complete {
            return String(localized: "tricount.status.verified")
        }

        return String(localized: "tricount.status.missing")
    }

    private static func initialTricountURLText(eventId: String) -> String {
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        return repository.getHandoff(eventId: eventId)?.providerUrl ?? ""
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                Phase5SyncBanner(pendingSync: pendingSync, isOnline: !pendingSync)

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                    WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                            Label(String(localized: "tricount.title"), systemImage: "link.circle.fill")
                                .font(WakeveTheme.Typography.section)

                            Text(handoffStatusText)
                                .font(WakeveTheme.Typography.callout)
                                .foregroundStyle(.secondary)

                            if let safeLink, safeLink.isVerified, safeLink.validatedURL != nil {
                                WakeveActionButton(
                                    safeLink.label,
                                    systemImage: "link",
                                    variant: .primary
                                ) {
                                    WakeveHaptics.selection()
                                    openSafeURL(safeLink)
                                }
                                Text(String(format: String(localized: "tricount.link_status_format"), safeLink.verificationStatus))
                                    .font(WakeveTheme.Typography.caption)
                                    .foregroundStyle(.secondary)
                            } else {
                                Label(String(localized: "tricount.no_verified_link"), systemImage: "lock.shield")
                                    .font(WakeveTheme.Typography.callout)
                                Text(String(localized: "tricount.safe_link_explanation"))
                                    .font(WakeveTheme.Typography.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }

                    PaymentTrustChecklistCard(
                        title: String(localized: "tricount.trust.title"),
                        items: [
                            PaymentTrustChecklistItem(
                                id: "verified-url",
                                systemImage: "link.circle.fill",
                                title: String(localized: "tricount.trust.url_title"),
                                detail: String(localized: "tricount.trust.url_detail")
                            ),
                            PaymentTrustChecklistItem(
                                id: "explicit-decision",
                                systemImage: "checkmark.circle.fill",
                                title: String(localized: "tricount.trust.decision_title"),
                                detail: String(localized: "tricount.trust.decision_detail")
                            ),
                            PaymentTrustChecklistItem(
                                id: "readonly-final",
                                systemImage: "lock.fill",
                                title: String(localized: "tricount.trust.readonly_title"),
                                detail: String(localized: "tricount.trust.readonly_detail")
                            )
                        ]
                    )

                    WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                            Text(String(localized: "tricount.decision"))
                                .font(WakeveTheme.Typography.section)

                            TextField(String(localized: "tricount.url_placeholder"), text: $tricountURLText)
                                .keyboardType(.URL)
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled()
                                .padding(.horizontal, WakeveTheme.Spacing.md)
                                .frame(height: 52)
                                .background(.thinMaterial, in: RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
                                .disabled(!canManageTricount)
                                .accessibilityLabel(String(localized: "tricount.url_section"))

                            Text(String(localized: "tricount.url_help"))
                                .font(WakeveTheme.Typography.caption)
                                .foregroundStyle(.secondary)

                            WakeveActionButton(
                                String(localized: "tricount.link"),
                                systemImage: "link.badge.plus",
                                variant: .primary,
                                isDisabled: !canManageTricount
                            ) {
                                onLinkTricount()
                            }

                            WakeveActionButton(
                                String(localized: "tricount.unlink"),
                                systemImage: "link.badge.minus",
                                variant: .neutral,
                                isDisabled: !canManageTricount
                            ) {
                                onUnlinkTricount()
                            }

                            WakeveActionButton(
                                String(localized: "tricount.not_needed"),
                                systemImage: "checkmark.shield",
                                variant: .secondary,
                                isDisabled: !canManageTricount
                            ) {
                                onMarkTricountNotNeeded()
                            }
                        }
                    }
                }
                .padding(WakeveTheme.Spacing.md)
            }
            .background(WakeveScreenBackground(style: .grouped))
            .navigationTitle(String(localized: "tricount.title"))
        }
    }

    private func onLinkTricount() {
        guard canManageTricount else { return }
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        let trimmedURL = tricountURLText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard repository.isTrustedProviderUrl(provider: "TRICOUNT", providerUrl: trimmedURL) else {
            safeLink = nil
            handoffStatusText = String(localized: "tricount.status.invalid_url")
            WakeveHaptics.warning()
            return
        }

        _ = repository.linkHandoff(
            eventId: eventId,
            provider: "TRICOUNT",
            providerId: "tricount-\(eventId)",
            providerUrl: trimmedURL,
            syncStatus: "LINKED"
        )
        safeLink = Self.loadSafeLink(eventId: eventId)
        handoffStatusText = Self.loadStatusText(eventId: eventId)
        WakeveHaptics.success()
    }

    private func onUnlinkTricount() {
        guard canManageTricount else { return }
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        repository.unlinkHandoff(eventId: eventId)
        safeLink = nil
        tricountURLText = ""
        handoffStatusText = Self.loadStatusText(eventId: eventId)
        WakeveHaptics.selection()
    }

    private func onMarkTricountNotNeeded() {
        guard canManageTricount else { return }
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        _ = repository.markNotNeeded(eventId: eventId, decidedBy: currentUserId)
        safeLink = nil
        tricountURLText = ""
        handoffStatusText = Self.loadStatusText(eventId: eventId)
        WakeveHaptics.success()
    }

    private func openSafeURL(_ link: SafeExternalLink) {
        SafeURLOpener.openSafeURL(link, openURL: openURL)
        _ = link.validatedURL
    }
}


private enum SafeURLOpener {
    static func openSafeURL(_ link: SafeExternalLink, openURL: OpenURLAction) {
        guard link.isVerified, let validatedURL = link.validatedURL else {
            return
        }

        openURL(validatedURL)
    }
}

private struct SafeExternalLink {
    let label: String
    let provider: String
    let validatedURL: URL?
    let verificationStatus: String
    let isVerified: Bool

    static func sanitize(
        label: String,
        provider: String,
        rawURL: String,
        verifier: (String, String) -> Bool
    ) -> SafeExternalLink? {
        let trimmed = rawURL.trimmingCharacters(in: .whitespacesAndNewlines)
        guard verifier(provider, trimmed), let url = URL(string: trimmed), url.scheme == "https" else {
            return nil
        }

        return SafeExternalLink(
            label: label,
            provider: provider,
            validatedURL: url,
            verificationStatus: String(localized: "safe_link.verified"),
            isVerified: true
        )
    }
}

private enum Phase5PendingSync {
    static func selectPending(eventId: String) -> Bool {
        RepositoryProvider.shared.database.syncMetadataQueries.selectPending().executeAsList().contains { pending in
            let phase5Types = [
                "meeting",
                "budget",
                "budget_item",
                "expense",
                "settlement",
                "payment",
                "payment_pot",
                "tricount",
                "tricount_handoff"
            ]
            return phase5Types.contains(pending.entityType) &&
                (pending.entityId == eventId || pending.entityId.hasPrefix("\(eventId):") || pending.entityId.contains(eventId))
        }
    }
}

struct EventListView: View {
    let repository: EventRepository
    let onEventSelected: (Event) -> Void
    let onCreateEvent: () -> Void
    
    @State private var events: [Event] = []
    @State private var isLoading = true
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            VStack(spacing: 8) {
                Text("Wakeve")
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                
                Text(String(localized: "events.legacy_list.subtitle"))
                    .font(.system(size: 16, weight: .medium, design: .rounded))
                    .foregroundColor(.secondary)
            }
            .padding(.top, 60)
            
            ScrollView {
                VStack(spacing: 24) {
                    // Create Event Card
                    VStack(spacing: 20) {
                        Button(action: onCreateEvent) {
                            VStack(spacing: 16) {
                                Image(systemName: "plus.circle.fill")
                                    .font(.system(size: 48))
                                    .foregroundColor(.blue)
                                
                                Text(String(localized: "events.legacy_list.create_title"))
                                    .font(.system(size: 18, weight: .semibold, design: .rounded))
                                    .foregroundColor(.primary)
                                
                                Text(String(localized: "events.legacy_list.create_subtitle"))
                                    .font(.system(size: 14, design: .rounded))
                                    .foregroundColor(.secondary)
                                    .multilineTextAlignment(.center)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 40)
                        }
                    }
                    .padding(24)
                    .glassCard(cornerRadius: 24)
                    
                    // Events List
                    if isLoading {
                        VStack(spacing: 16) {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .secondary))
                                .accessibilityLabel(String(localized: "common.loading"))
                            
                            Text(String(localized: "home.loading"))
                                .font(.system(size: 14, design: .rounded))
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 40)
                    } else if events.isEmpty {
                        VStack(spacing: 16) {
                            Image(systemName: "calendar.badge.exclamationmark")
                                .font(.system(size: 48))
                                .foregroundColor(Color(.tertiaryLabel))
                            
                            Text(String(localized: "events.empty.title"))
                                .font(.system(size: 16, weight: .medium, design: .rounded))
                                .foregroundColor(.secondary)
                            
                            Text(String(localized: "events.empty.subtitle"))
                                .font(.system(size: 14, design: .rounded))
                                .foregroundColor(Color(.tertiaryLabel))
                                .multilineTextAlignment(.center)
                        }
                        .padding(.vertical, 40)
                    } else {
                        VStack(spacing: 16) {
                            ForEach(events, id: \.id) { event in
                                EventCard(event: event, onTap: {
                                    onEventSelected(event)
                                })
                            }
                        }
                    }
                    
                    Spacer(minLength: 40)
                }
                .padding(.horizontal, 20)
            }
        }
        .onAppear {
            loadEvents()
        }
    }
    
    private func loadEvents() {
        // In a real app, this would load from a service
        // For now, just simulate loading
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            events = repository.getAllEvents()
            isLoading = false
        }
    }
}

struct EventCard: View {
    let event: Event
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(event.title)
                            .font(.system(size: 18, weight: .semibold, design: .rounded))
                            .foregroundColor(.primary)
                        
                        if !event.description.isEmpty {
                            Text(event.description)
                                .font(.system(size: 14, design: .rounded))
                                .foregroundColor(.secondary)
                                .lineLimit(2)
                        }
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing, spacing: 4) {
                        Text(statusText)
                            .font(.system(size: 12, weight: .medium, design: .rounded))
                            .foregroundColor(statusColor)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(statusColor.opacity(0.2))
                            .cornerRadius(8)
                        
                        Text(participantCountText)
                            .font(.system(size: 12, design: .rounded))
                            .foregroundColor(Color(.tertiaryLabel))
                    }
                }
                
                HStack {
                    Image(systemName: "calendar")
                        .font(.system(size: 14))
                        .foregroundColor(Color(.tertiaryLabel))
                    
                    Text(slotOptionsText)
                        .font(.system(size: 14, design: .rounded))
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    Image(systemName: "clock")
                        .font(.system(size: 14))
                        .foregroundColor(Color(.tertiaryLabel))
                    
                    Text(formatDeadline(event.deadline))
                        .font(.system(size: 14, design: .rounded))
                        .foregroundColor(.secondary)
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .glassCard(cornerRadius: 16)
    }
    
    private var statusText: String {
        switch event.status {
        case .draft: return String(localized: "events.status.draft_preview")
        case .polling: return String(localized: "events.status.polling")
        case .confirmed: return String(localized: "events.status.confirmed")
        case .organizing: return String(localized: "events.status.organizing")
        case .finalized: return String(localized: "events.status.finalized")
        default: return String(localized: "events.status.event")
        }
    }

    private var participantCountText: String {
        event.participants.count == 1
            ? String(format: String(localized: "participants.count.singular_format"), event.participants.count)
            : String(format: String(localized: "participants.count.plural_format"), event.participants.count)
    }

    private var slotOptionsText: String {
        event.proposedSlots.count == 1
            ? String(format: String(localized: "event.detail.slot_option_singular_format"), event.proposedSlots.count)
            : String(format: String(localized: "event.detail.slot_options_plural_format"), event.proposedSlots.count)
    }
    
    private var statusColor: Color {
        switch event.status {
        case .draft: return .orange
        case .polling: return .blue
        case .confirmed: return .green
        default: return .gray
        }
    }
    
    private func formatDeadline(_ deadlineString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: deadlineString) {
            let formatter = DateFormatter()
            formatter.locale = .autoupdatingCurrent
            formatter.dateStyle = .short
            formatter.timeStyle = .none
            return formatter.string(from: date)
        }
        return deadlineString
    }
}

struct EventDetailView: View {
    @Environment(\.colorScheme) private var colorScheme

    let event: Event
    let repository: EventRepositoryInterface
    let userId: String
    let preparedCreationChecklist: [ChecklistItem]
    let onManageParticipants: () -> Void
    let onVote: () -> Void
    let onViewResults: () -> Void
    let onOrganize: () -> Void
    let onOpenTransport: () -> Void
    let onOpenMeetings: () -> Void
    let onOpenBudget: () -> Void
    let onOpenPayment: () -> Void
    let onOpenTricount: () -> Void
    let isInvitationLanding: Bool
    let onDismissInvitationLanding: () -> Void
    let onBack: () -> Void

    @State private var eventAISummary: EventSummary?
    @State private var eventAIPolls: [PollSuggestion] = []
    @State private var eventAIChecklist: [ChecklistItem] = []
    @State private var eventAIInvitationMessages: InvitationMessageSet?
    @State private var selectedInvitationVariant: EventAIInvitationVariant = .simple
    @State private var editableInvitationDraft: String = ""
    @State private var appliedEventAISuggestions: Set<String> = []
    @State private var ignoredEventAISuggestions: Set<String> = []
    @State private var isGeneratingEventAI = false
    @State private var eventAIError: String?
    @State private var seededPreparedCreationChecklist = false
    @State private var moderationTarget: ModerationActionTarget?
    @State private var didPlayInvitationLandingHaptic = false
    @StateObject private var eventWeatherViewModel = EventWeatherViewModel()

    var body: some View {
        ZStack {
            WakeveTheme.ColorToken.pageBackground(for: colorScheme)
                .ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                    heroSection
                    if isInvitationLanding {
                        invitationLandingCard
                    }
                    metadataOverview
                    if canShowWeatherContext {
                        EventWeatherMapCard(state: eventWeatherViewModel.state)
                    }
                    anticipationPanel
                    eventAISuggestionPanel
                    urgentNextAction
                    if canShowGroupReadiness {
                        groupReadinessPanel
                    }
                    participantsPreview
                    detailRows
                    messagePreview

                    Text(footerHint)
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                        .frame(maxWidth: .infinity)
                        .multilineTextAlignment(.center)
                        .padding(.top, WakeveTheme.Spacing.xs)
                }
                .padding(.horizontal, WakeveTheme.Spacing.page)
                .padding(.top, WakeveTheme.Spacing.md)
                .padding(.bottom, 118)
            }
        }
        .toolbar(.hidden, for: .tabBar)
        .safeAreaInset(edge: .top, spacing: 0) {
            topControls
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            bottomPrimaryAction
        }
        .sheet(item: $moderationTarget) { target in
            ModerationActionSheet(target: target)
        }
        .task(id: "\(event.id)-\(canShowWeatherContext)") {
            guard canShowWeatherContext else {
                eventWeatherViewModel.hide()
                return
            }
            await eventWeatherViewModel.load(event: event)
        }
        .onAppear {
            seedPreparedCreationChecklistIfNeeded()
            triggerInvitationLandingHapticIfNeeded()
        }
    }

    private var heroSection: some View {
        EventHeroCard(
            title: event.title,
            subtitle: subtitleText,
            metadata: statusText,
            gradient: eventHeroGradient
        ) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                if let description = displayDescription {
                    Text(description)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(.white.opacity(0.82))
                        .lineSpacing(3)
                        .lineLimit(3)
                }

                HStack(spacing: WakeveTheme.Spacing.sm) {
                    EventDetailHeroMetric(
                        systemImage: "person.2.fill",
                        value: participantRangeText
                    )

                    EventDetailHeroMetric(
                        systemImage: "calendar",
                        value: primaryDateText
                    )
                }
            }
            .overlay(alignment: .topTrailing) {
                eventSymbol
                    .font(.system(size: 76, weight: .black))
                    .foregroundColor(.white.opacity(0.18))
                    .rotationEffect(.degrees(-8))
                    .offset(x: 16, y: -114)
            }
        }
        .accessibilityElement(children: .combine)
    }

    private var metadataOverview: some View {
        WakeveGlassCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text(String(localized: "event.detail.summary_title"))
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: WakeveTheme.Spacing.sm) {
                    EventDetailMetadataPill(icon: statusIcon, title: String(localized: "event.detail.metadata.status"), value: statusText)
                    EventDetailMetadataPill(icon: "timer", title: String(localized: "event.detail.metadata.poll"), value: pollDurationText)
                    EventDetailMetadataPill(icon: "calendar", title: String(localized: "event.detail.metadata.slots"), value: slotOptionsText)
                    EventDetailMetadataPill(icon: "person.2.fill", title: String(localized: "event.detail.metadata.guests"), value: participantSummary)
                }
            }
        }
    }

    private var invitationLandingCard: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack(alignment: .top, spacing: WakeveTheme.Spacing.sm) {
                    Image(systemName: "envelope.open.fill")
                        .font(.headline.weight(.bold))
                        .foregroundColor(.white)
                        .frame(width: 42, height: 42)
                        .background(WakeveTheme.ColorToken.permissionBlue, in: Circle())

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text(String(localized: "event.detail.invite_landing.title"))
                            .font(WakeveTheme.Typography.section)
                            .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

                        Text(invitationLandingSubtitle)
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }

                VStack(spacing: WakeveTheme.Spacing.sm) {
                    WakeveActionButton(
                        invitationLandingPrimaryActionTitle,
                        systemImage: invitationLandingPrimaryActionIcon,
                        variant: .primary
                    ) {
                        WakeveHaptics.selection()
                        if event.status == .polling {
                            onVote()
                        } else {
                            onManageParticipants()
                        }
                    }
                    .accessibilityIdentifier("eventInvitationLandingPrimaryAction")

                    WakeveActionButton(
                        String(localized: "event.detail.invite_landing.continue_action"),
                        systemImage: "checkmark.circle.fill",
                        variant: .neutral
                    ) {
                        WakeveHaptics.selection()
                        onDismissInvitationLanding()
                    }
                    .accessibilityIdentifier("eventInvitationLandingContinueAction")
                }
            }
        }
        .accessibilityIdentifier("eventInvitationLandingCard")
    }

    private var anticipationPanel: some View {
        EventDetailAnticipationCard(
            countdownTitle: countdownTitle,
            countdownSubtitle: countdownSubtitle,
            items: anticipationItems,
            returnHookTitle: String(localized: "event.detail.return_hook.share_action"),
            returnHookMessage: returnHookMessage
        )
    }

    private var titleBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(event.title)
                .font(.system(size: 32, weight: .bold))
                .foregroundColor(primaryText)
                .lineLimit(3)
                .minimumScaleFactor(0.82)

            Text(subtitleText)
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(secondaryText)
                .lineLimit(2)
        }
    }

    private var organizerControls: some View {
        HStack(spacing: 10) {
            OrganizerChip(
                icon: "person.badge.plus",
                title: String(localized: "event.detail.organizer.invite_title"),
                subtitle: String(localized: "event.detail.participants_title"),
                tint: Color(hex: "7DD3FC"),
                action: onManageParticipants
            )

            OrganizerChip(
                icon: "chart.bar.xaxis",
                title: String(localized: "event.detail.organizer.poll_title"),
                subtitle: pollDurationText,
                tint: Color(hex: "FDE68A"),
                action: event.status == .draft ? onManageParticipants : onViewResults
            )
        }
    }

    private var urgentNextAction: some View {
        let action = nextAction

        return EventDetailNextActionCard(
            title: action.title,
            subtitle: action.displaySubtitle,
            systemImage: action.systemImage,
            blockedReason: action.blockedReason,
            isDisabled: action.isBlocked,
            action: primaryAction
        )
    }

    private var groupReadinessPanel: some View {
        EventDetailReadinessCard(
            title: String(localized: "event.detail.readiness.title"),
            subtitle: String(localized: "event.detail.readiness.subtitle"),
            progressText: readinessProgressText,
            items: groupReadinessItems
        )
    }

    private var participantsPreview: some View {
        EventDetailParticipantsPreview(
            title: String(localized: "event.detail.participants_title"),
            subtitle: participantPreviewSubtitle,
            initials: participantInitials,
            action: onManageParticipants
        )
    }

    private var detailRows: some View {
        EventDetailSectionCard(title: String(localized: "event.detail.section.organization")) {
            EventDetailActionRow(
                icon: "calendar",
                label: String(localized: "event.detail.organization.slots_label"),
                value: slotOptionsSummary,
                action: event.status == .polling ? onVote : onViewResults
            )

            if canAccessScenarioPlanning {
                EventDetailActionRow(
                    icon: "map.fill",
                    label: String(localized: "event.detail.organization.scenario_label"),
                    value: scenarioPlanningText,
                    action: onOrganize
                )
            }

            if canAccessTransportPlanning {
                EventDetailActionRow(
                    icon: "point.topleft.down.curvedto.point.bottomright.up.fill",
                    label: String(localized: "event.detail.organization.transport_label"),
                    value: String(localized: "event.detail.organization.transport_value"),
                    action: onOpenTransport
                )
            }

            if canShowOrganizationDashboard {
                EventDetailActionRow(
                    icon: "video.fill",
                    label: String(localized: "event.detail.organization.meetings_label"),
                    value: organizationDashboardValue(String(localized: "event.detail.organization.meetings_value")),
                    action: onOpenMeetings
                )

                EventDetailActionRow(
                    icon: "eurosign.circle.fill",
                    label: String(localized: "event.detail.organization.budget_label"),
                    value: organizationDashboardValue(String(localized: "event.detail.organization.budget_value")),
                    action: onOpenBudget
                )

                EventDetailActionRow(
                    icon: "creditcard.fill",
                    label: String(localized: "event.detail.organization.payment_pot_label"),
                    value: paymentPotSummaryValue(),
                    action: onOpenPayment
                )

                EventDetailActionRow(
                    icon: "link.circle.fill",
                    label: String(localized: "event.detail.menu.tricount"),
                    value: tricountSummaryValue(),
                    action: onOpenTricount
                )
            }
        }
    }

    private var messagePreview: some View {
        EventDetailMessagePreview(
            title: String(localized: "event.detail.messages_title"),
            subtitle: compactMessageSubtitle,
            badgeText: event.status == .polling ? String(localized: "event.detail.message_preview.vote_badge") : String(localized: "event.detail.message_preview.event_badge")
        )
    }

    private var eventAISuggestionPanel: some View {
        WakeveGlassCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack(alignment: .top, spacing: WakeveTheme.Spacing.sm) {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text(String(localized: "event.detail.ai.title"))
                            .font(WakeveTheme.Typography.section)
                            .foregroundColor(primaryText)

                        Text(String(localized: "event.detail.ai.review_subtitle"))
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(secondaryText)
                    }

                    Spacer()

                    Button {
                        generateEventAISuggestions()
                    } label: {
                        Image(systemName: "sparkles")
                            .font(.headline.weight(.bold))
                            .foregroundColor(isGeneratingEventAI ? secondaryText : WakeveTheme.ColorToken.accent(for: colorScheme))
                            .frame(width: 44, height: 44)
                            .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .disabled(isGeneratingEventAI)
                    .accessibilityLabel(String(localized: "ai.prepare_suggestions_accessibility"))
                }

                if isGeneratingEventAI {
                    HStack(spacing: WakeveTheme.Spacing.sm) {
                        ProgressView()
                            .accessibilityLabel(String(localized: "common.loading"))
                        Text(String(localized: "ai.preparing"))
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(secondaryText)
                    }
                }

                if let eventAIError {
                    Text(eventAIError)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(WakeveTheme.ColorToken.destructive(for: colorScheme))
                }

                if let eventAISummary {
                    EventAISummaryBlock(
                        summary: eventAISummary,
                        colorScheme: colorScheme,
                        applied: appliedEventAISuggestions.contains("summary"),
                        ignored: ignoredEventAISuggestions.contains("summary"),
                        onModify: { appliedEventAISuggestions.remove("summary") },
                        onApply: { appliedEventAISuggestions.insert("summary") },
                        onIgnore: { ignoredEventAISuggestions.insert("summary") }
                    )
                }

                if !eventAIPolls.isEmpty {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                        Text(String(localized: "event.detail.ai.polls_title"))
                            .font(WakeveTheme.Typography.bodySemibold)
                            .foregroundColor(primaryText)

                        ForEach(Array(eventAIPolls.enumerated()), id: \.offset) { index, poll in
                            EventAIPollSuggestionRow(
                                poll: poll,
                                colorScheme: colorScheme,
                                applied: appliedEventAISuggestions.contains("poll-\(index)"),
                                ignored: ignoredEventAISuggestions.contains("poll-\(index)"),
                                onModify: { appliedEventAISuggestions.remove("poll-\(index)") },
                                onApply: { appliedEventAISuggestions.insert("poll-\(index)") },
                                onIgnore: { ignoredEventAISuggestions.insert("poll-\(index)") }
                            )
                        }
                    }
                }

                if !eventAIChecklist.isEmpty {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                        Text(String(localized: "event.detail.ai.checklist_title"))
                            .font(WakeveTheme.Typography.bodySemibold)
                            .foregroundColor(primaryText)

                        ForEach(Array(eventAIChecklist.enumerated()), id: \.offset) { index, item in
                            EventAIChecklistRow(
                                item: item,
                                colorScheme: colorScheme,
                                applied: appliedEventAISuggestions.contains("checklist-\(index)"),
                                ignored: ignoredEventAISuggestions.contains("checklist-\(index)"),
                                onModify: { appliedEventAISuggestions.remove("checklist-\(index)") },
                                onApply: { appliedEventAISuggestions.insert("checklist-\(index)") },
                                onIgnore: { ignoredEventAISuggestions.insert("checklist-\(index)") }
                            )
                        }
                    }
                }

                if eventAIInvitationMessages != nil {
                    EventAIInvitationBlock(
                        variant: $selectedInvitationVariant,
                        editableDraft: $editableInvitationDraft,
                        colorScheme: colorScheme,
                        applied: appliedEventAISuggestions.contains("invitation"),
                        ignored: ignoredEventAISuggestions.contains("invitation"),
                        onSelectVariant: updateInvitationDraft,
                        onModify: { appliedEventAISuggestions.remove("invitation") },
                        onApply: { appliedEventAISuggestions.insert("invitation") },
                        onIgnore: { ignoredEventAISuggestions.insert("invitation") }
                    )
                }
            }
        }
    }

    private var topControls: some View {
        LiquidGlassToolbar(title: String(localized: "event.detail.title"), subtitle: statusText) {
            WakeveCircleButton(
                systemImage: "chevron.left",
                accessibilityLabel: String(localized: "common.back"),
                variant: .light,
                size: 40,
                action: onBack
            )
        } trailing: {
        Menu {
            organizerMenuContent
        } label: {
                Image(systemName: "ellipsis")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                    .frame(width: 40, height: 40)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(Circle())
            }
            .accessibilityLabel(String(localized: "events.organizer_options_accessibility"))
        }
        .padding(.horizontal, WakeveTheme.Spacing.page)
        .padding(.top, WakeveTheme.Spacing.sm)
        .padding(.bottom, WakeveTheme.Spacing.xs)
        .background(
            LinearGradient(
                colors: [
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme),
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme).opacity(0)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea(edges: .top)
        )
    }

    @ViewBuilder
    private var organizerMenuContent: some View {
        Button(action: onManageParticipants) {
            Label(String(localized: "event.detail.menu.add_participants"), systemImage: "person.badge.plus")
        }

        Button(action: event.status == .polling ? onViewResults : onManageParticipants) {
            Label(String(localized: "event.detail.menu.configure_poll"), systemImage: "timer")
        }

        if event.status == .polling {
            Button(action: onViewResults) {
                Label(String(localized: "event.detail.menu.view_results"), systemImage: "chart.bar.fill")
            }
        }

        if canAccessScenarioPlanning {
            Button(action: onOrganize) {
                Label(String(localized: "event.detail.menu.organize_scenarios"), systemImage: "map.fill")
            }
        }

        if canAccessTransportPlanning {
            Button(action: onOpenTransport) {
                Label(String(localized: "event.detail.menu.transport"), systemImage: "point.topleft.down.curvedto.point.bottomright.up.fill")
            }
        }

        if canShowOrganizationDashboard {
            Button(action: onOpenMeetings) {
                Label(String(localized: "event.detail.menu.meetings"), systemImage: "video.fill")
            }
            Button(action: onOpenBudget) {
                Label(String(localized: "event.detail.menu.budget"), systemImage: "eurosign.circle.fill")
            }
            Button(action: onOpenPayment) {
                Label(String(localized: "event.detail.menu.payment_pot"), systemImage: "creditcard.fill")
            }
            Button(action: onOpenTricount) {
                Label(String(localized: "event.detail.menu.tricount"), systemImage: "link.circle.fill")
            }
        }

        Divider()

        Button {
            moderationTarget = ModerationActionTarget(
                type: .event,
                targetId: event.id,
                eventId: event.id,
                authorId: event.organizerId,
                displayName: String(localized: "moderation.report_event_context"),
                allowsBlock: false
            )
        } label: {
            Label(String(localized: "moderation.report_event"), systemImage: "exclamationmark.bubble")
        }
        .accessibilityIdentifier("reportEventAction")

        Link(
            String(localized: "moderation.contact_support"),
            destination: URL(string: "mailto:support@wakeve.app?subject=Wakeve%20abuse%20report")!
        )
    }

    private var bottomPrimaryAction: some View {
        VStack(spacing: 0) {
            LinearGradient(
                colors: [
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme).opacity(0),
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 32)
            .allowsHitTesting(false)

            LiquidGlassButton(
                primaryActionTitle,
                systemImage: nextAction.systemImage,
                variant: .primary,
                isDisabled: primaryActionDisabled,
                action: primaryAction
            )
            .padding(.horizontal, WakeveTheme.Spacing.page)
            .padding(.bottom, WakeveTheme.Spacing.sm)
            .background(WakeveTheme.ColorToken.pageBackground(for: colorScheme))
        }
    }

    private var statusBadge: some View {
        HStack(spacing: 6) {
            Image(systemName: statusIcon)
                .font(.system(size: 13, weight: .bold))
            Text(statusText)
                .font(WakeveTheme.Typography.caption)
        }
        .foregroundColor(.white)
        .padding(.horizontal, WakeveTheme.Spacing.sm)
        .padding(.vertical, WakeveTheme.Spacing.xs)
        .background(Color.black.opacity(0.28))
        .clipShape(Capsule())
    }

    @ViewBuilder
    private var eventSymbol: some View {
        switch event.eventType {
        case .sportsEvent:
            Image(systemName: "sportscourt.fill")
        case .birthday, .party:
            Image(systemName: "party.popper.fill")
        case .teamBuilding, .conference, .workshop, .techMeetup:
            Image(systemName: "person.3.sequence.fill")
        case .outdoorActivity:
            Image(systemName: "map.fill")
        case .familyGathering:
            Image(systemName: "figure.2.and.child.holdinghands")
        default:
            Image(systemName: "sparkles")
        }
    }

    private var statusIcon: String {
        switch event.status {
        case .draft: return "wand.and.stars"
        case .polling: return "chart.bar.fill"
        case .confirmed: return "checkmark.circle.fill"
        case .organizing: return "calendar.badge.clock"
        case .finalized: return "checkmark.seal.fill"
        default: return "sparkles"
        }
    }

    private var statusText: String {
        switch event.status {
        case .draft: return String(localized: "events.status.draft_preview")
        case .polling: return String(localized: "events.status.polling")
        case .confirmed: return String(localized: "events.status.date_confirmed")
        case .organizing: return String(localized: "events.status.organizing")
        case .finalized: return String(localized: "events.status.finalized")
        default: return String(localized: "events.status.event")
        }
    }

    private var subtitleText: String {
        if let firstSlot = event.proposedSlots.first {
            return formatSlot(firstSlot)
        }
        return String(localized: "event.detail.subtitle.empty")
    }

    private var primaryDateText: String {
        if let finalDate = event.finalDate, let date = parseDate(finalDate) {
            return formatEventDate(date)
        }

        if let firstSlot = event.proposedSlots.first,
           let startValue = firstSlot.start,
           let start = parseDate(startValue) {
            return formatEventDate(start)
        }

        return String(localized: "event.detail.date_to_choose")
    }

    private var nextAction: EventNextAction {
        EventNextAction(event: event)
    }

    private var eventMomentDate: Date? {
        if let finalDate = event.finalDate {
            if let finalSlot = event.proposedSlots.first(where: { $0.id == finalDate }),
               let startValue = finalSlot.start,
               let start = parseDate(startValue) {
                return start
            }

            if let date = parseDate(finalDate) {
                return date
            }
        }

        if let firstSlot = event.proposedSlots.first,
           let startValue = firstSlot.start,
           let start = parseDate(startValue) {
            return start
        }

        return nil
    }

    private var countdownTitle: String {
        guard let eventMomentDate else {
            return String(localized: "event.detail.countdown.to_create")
        }

        let days = Calendar.current.dateComponents([.day], from: Calendar.current.startOfDay(for: Date()), to: Calendar.current.startOfDay(for: eventMomentDate)).day ?? 0

        if days < 0 {
            return String(localized: "event.detail.countdown.past")
        }

        if days == 0 {
            return String(localized: "event.detail.countdown.today")
        }

        if days == 1 {
            return String(localized: "event.detail.countdown.tomorrow")
        }

        return String(format: String(localized: "event.detail.countdown.days_format"), days)
    }

    private var countdownSubtitle: String {
        guard let eventMomentDate else {
            return String(localized: "event.detail.countdown.empty_subtitle")
        }

        return "\(formatLongEventDate(eventMomentDate)) · \(anticipationStatusText)"
    }

    private var anticipationStatusText: String {
        switch event.status {
        case .draft:
            return String(localized: "event.detail.anticipation.status.draft")
        case .polling:
            return String(localized: "event.detail.anticipation.status.polling")
        case .confirmed:
            return String(localized: "event.detail.anticipation.status.confirmed")
        case .comparing:
            return String(localized: "event.detail.anticipation.status.comparing")
        case .organizing:
            return String(localized: "event.detail.anticipation.status.organizing")
        case .finalized:
            return String(localized: "event.detail.anticipation.status.finalized")
        default:
            return String(localized: "event.detail.anticipation.status.default")
        }
    }

    private var invitationLandingSubtitle: String {
        if event.status == .polling {
            return String(localized: "event.detail.invite_landing.polling_subtitle")
        }

        return String(localized: "event.detail.invite_landing.default_subtitle")
    }

    private var invitationLandingPrimaryActionTitle: String {
        event.status == .polling
            ? String(localized: "event.detail.invite_landing.vote_action")
            : String(localized: "event.detail.invite_landing.view_invite_action")
    }

    private var invitationLandingPrimaryActionIcon: String {
        event.status == .polling ? "checklist.checked" : "person.badge.plus"
    }

    private var anticipationItems: [EventAnticipationItem] {
        var items: [EventAnticipationItem] = []

        if event.participants.isEmpty {
            items.append(EventAnticipationItem(icon: "person.badge.plus", title: String(localized: "event.detail.anticipation.invite.title"), subtitle: String(localized: "event.detail.anticipation.invite.subtitle")))
        } else {
            items.append(EventAnticipationItem(icon: "person.2.fill", title: participantSummary, subtitle: participantPreviewSubtitle))
        }

        switch event.status {
        case .draft:
            items.append(EventAnticipationItem(icon: "chart.bar.doc.horizontal", title: String(localized: "event.detail.anticipation.start_poll.title"), subtitle: String(localized: "event.detail.anticipation.start_poll.subtitle")))
        case .polling:
            items.append(EventAnticipationItem(icon: "megaphone.fill", title: String(localized: "event.detail.anticipation.announcement.title"), subtitle: String(localized: "event.detail.anticipation.announcement.subtitle")))
        case .confirmed, .comparing:
            items.append(EventAnticipationItem(icon: "map.fill", title: String(localized: "event.detail.anticipation.final_option.title"), subtitle: String(localized: "event.detail.anticipation.final_option.subtitle")))
        case .organizing:
            items.append(EventAnticipationItem(icon: "checklist", title: String(localized: "event.detail.anticipation.lock_next.title"), subtitle: String(localized: "event.detail.anticipation.lock_next.subtitle")))
        case .finalized:
            items.append(EventAnticipationItem(icon: "checkmark.seal.fill", title: String(localized: "event.detail.anticipation.ready.title"), subtitle: String(localized: "event.detail.anticipation.ready.subtitle")))
        default:
            items.append(EventAnticipationItem(icon: "sparkles", title: String(localized: "event.detail.anticipation.next_step.title"), subtitle: nextAction.title))
        }

        if event.status != .finalized {
            items.append(EventAnticipationItem(icon: "photo.on.rectangle.angled", title: String(localized: "event.detail.anticipation.excitement.title"), subtitle: String(localized: "event.detail.anticipation.excitement.subtitle")))
        }

        return Array(items.prefix(3))
    }

    private var returnHookMessage: String {
        String(
            format: String(localized: "event.detail.return_hook.message_format"),
            event.title,
            countdownTitle,
            nextAction.title,
            eventInviteURLString
        )
    }

    private var eventInviteURLString: String {
        let invitationCode = InvitationTokenCodec.invitationCode(forEventId: event.id)
        return "https://wakeve.app/invite/\(invitationCode)"
    }

    private var participantRangeText: String {
        if let min = event.minParticipants?.intValue, let max = event.maxParticipants?.intValue {
            return String(format: String(localized: "event.detail.participant_range_format"), min, max)
        }
        if let expected = event.expectedParticipants?.intValue {
            return String(format: String(localized: "event.detail.expected_participants_format"), expected)
        }
        return event.participants.count == 1
            ? String(format: String(localized: "event.detail.invited_participant_singular_format"), event.participants.count)
            : String(format: String(localized: "event.detail.invited_participants_plural_format"), event.participants.count)
    }

    private var slotOptionsText: String {
        event.proposedSlots.count == 1
            ? String(format: String(localized: "event.detail.slot_option_singular_format"), event.proposedSlots.count)
            : String(format: String(localized: "event.detail.slot_options_plural_format"), event.proposedSlots.count)
    }

    private var participantInitials: [String] {
        let values = event.participants.isEmpty ? [userId] : event.participants
        return values.map { value in
            let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
            let source = trimmed.isEmpty ? "?" : trimmed
            let components = source
                .replacingOccurrences(of: "@", with: " ")
                .replacingOccurrences(of: ".", with: " ")
                .split(separator: " ")

            if components.count >= 2,
               let first = components.first?.first,
               let second = components.dropFirst().first?.first {
                return "\(first)\(second)".uppercased()
            }

            return String(source.prefix(2)).uppercased()
        }
    }

    private var participantPreviewSubtitle: String {
        event.participants.isEmpty
            ? String(localized: "event.detail.participant_preview.empty")
            : participantPreviewCountText
    }

    private var participantSummary: String {
        if event.participants.isEmpty {
            return String(localized: "event.detail.participant_summary.you")
        }
        return String(format: String(localized: "event.detail.participant_summary.you_plus_format"), event.participants.count)
    }

    private var participantPreviewCountText: String {
        event.participants.count == 1
            ? String(format: String(localized: "event.detail.participant_preview.singular_format"), event.participants.count)
            : String(format: String(localized: "event.detail.participant_preview.plural_format"), event.participants.count)
    }

    private var slotOptionsSummary: String {
        event.proposedSlots.count == 1
            ? String(format: String(localized: "event.detail.slot_option_singular_format"), event.proposedSlots.count)
            : String(format: String(localized: "event.detail.slot_options_plural_format"), event.proposedSlots.count)
    }

    private var pollDurationText: String {
        guard let deadline = parseDate(event.deadline) else {
            return String(localized: "event.detail.poll_duration.to_define")
        }

        let interval = deadline.timeIntervalSince(Date())
        if interval <= 0 {
            return String(localized: "event.detail.poll_duration.done")
        }

        let days = Int(ceil(interval / 86_400))
        if days >= 2 {
            return String(format: String(localized: "event.detail.poll_duration.days_format"), days)
        }

        let hours = max(1, Int(ceil(interval / 3_600)))
        return "\(hours) h"
    }

    private var footerHint: String {
        switch event.status {
        case .draft:
            return String(localized: "event.detail.footer.draft")
        case .polling:
            return String(localized: "event.detail.footer.polling")
        default:
            return String(localized: "event.detail.footer.default")
        }
    }

    private var compactMessageSubtitle: String {
        switch event.status {
        case .draft:
            return String(localized: "event.detail.message_preview.draft")
        case .polling:
            return String(localized: "event.detail.message_preview.polling")
        case .confirmed, .comparing:
            return String(localized: "event.detail.message_preview.scenario")
        default:
            return String(localized: "event.detail.message_preview.default")
        }
    }

    private var primaryActionTitle: String {
        nextAction.title
    }

    private var eventHeroGradient: LinearGradient {
        LinearGradient(
            colors: heroColors,
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    private var primaryActionDisabled: Bool {
        nextAction.isBlocked
    }

    private var heroColors: [Color] {
        switch event.eventType {
        case .sportsEvent:
            return [Color(hex: "88D18A"), Color(hex: "2F855A"), pageBackground]
        case .birthday, .party:
            return [Color(hex: "FFB86B"), Color(hex: "F43F5E"), pageBackground]
        case .teamBuilding, .conference, .workshop, .techMeetup:
            return [Color(hex: "67E8F9"), Color(hex: "2563EB"), pageBackground]
        case .outdoorActivity:
            return [Color(hex: "FDE68A"), Color(hex: "0F766E"), pageBackground]
        default:
            return [Color(hex: "F6C177"), Color(hex: "7C3AED"), pageBackground]
        }
    }

    private var pageBackground: Color {
        colorScheme == .dark ? Color(hex: "111119") : Color(hex: "F7F4F1")
    }

    private var primaryText: Color {
        colorScheme == .dark ? .white : Color(hex: "17171F")
    }

    private var secondaryText: Color {
        colorScheme == .dark ? Color.white.opacity(0.62) : Color(hex: "4F5260")
    }

    private var primaryButtonBackground: Color {
        colorScheme == .dark ? Color.white.opacity(0.9) : Color(hex: "17171F")
    }

    private var primaryButtonText: Color {
        colorScheme == .dark ? Color(hex: "17171F") : .white
    }

    private var displayDescription: String? {
        let raw = event.description.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !raw.isEmpty else { return nil }
        if raw.hasPrefix("Event(") {
            return extractEventDescription(from: raw)
        }
        return raw
    }

    private func extractEventDescription(from raw: String) -> String? {
        guard let startRange = raw.range(of: "description=") else { return nil }
        let afterDescription = raw[startRange.upperBound...]
        let endRange = afterDescription.range(of: ", organizerId=")
        let value = String(afterDescription[..<(endRange?.lowerBound ?? afterDescription.endIndex)])
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty || value == "null" ? nil : value
    }

    private func primaryAction() {
        switch event.status {
        case .draft:
            onManageParticipants()
        case .polling:
            onViewResults()
        case .confirmed, .comparing, .organizing, .finalized:
            onOrganize()
        default:
            onViewResults()
        }
    }

    private func generateEventAISuggestions() {
        guard !isGeneratingEventAI else { return }
        isGeneratingEventAI = true
        eventAIError = nil
        ignoredEventAISuggestions.removeAll()

        let contextProvider = EventDetailWakeveAIContextProvider(
            eventId: event.id,
            title: event.title,
            date: primaryDateText,
            location: displayDescription,
            participantNames: event.participants,
            voteSummaries: event.proposedSlots.map(formatSlot),
            status: statusText,
            message: compactMessageSubtitle
        )
        let client = HeuristicWakeveAIClient()
        let knownFacts = contextProvider.facts
        let localeIdentifier = Locale.autoupdatingCurrent.identifier

        Task {
            do {
                async let summary = EventSummaryGenerator(
                    client: client,
                    contextProvider: contextProvider
                ).generate(eventId: event.id, localeIdentifier: localeIdentifier)
                async let polls = PollSuggestionGenerator(client: client).generate(
                    context: contextProvider.promptSummary,
                    knownFacts: knownFacts,
                    localeIdentifier: localeIdentifier
                )
                async let checklist = ChecklistGenerator(client: client).generate(
                    context: contextProvider.promptSummary,
                    knownFacts: knownFacts,
                    localeIdentifier: localeIdentifier
                )
                async let messages = InvitationMessageGenerator(client: client).generate(
                    context: contextProvider.promptSummary,
                    knownFacts: knownFacts,
                    localeIdentifier: localeIdentifier
                )

                let results = try await (summary, polls, checklist, messages)
                await MainActor.run {
                    eventAISummary = results.0
                    eventAIPolls = results.1
                    eventAIChecklist = results.2
                    eventAIInvitationMessages = results.3
                    selectedInvitationVariant = .simple
                    editableInvitationDraft = results.3.text(for: .simple)
                    isGeneratingEventAI = false
                }
            } catch {
                await MainActor.run {
                    eventAIError = String(localized: "event.detail.ai.error_unavailable")
                    isGeneratingEventAI = false
                }
            }
        }
    }

    private func seedPreparedCreationChecklistIfNeeded() {
        guard !seededPreparedCreationChecklist, eventAIChecklist.isEmpty, !preparedCreationChecklist.isEmpty else {
            return
        }

        eventAIChecklist = preparedCreationChecklist
        seededPreparedCreationChecklist = true
    }

    private func triggerInvitationLandingHapticIfNeeded() {
        guard isInvitationLanding, !didPlayInvitationLandingHaptic else { return }
        WakeveHaptics.success()
        didPlayInvitationLandingHaptic = true
    }

    private func updateInvitationDraft(_ variant: EventAIInvitationVariant) {
        selectedInvitationVariant = variant
        guard let eventAIInvitationMessages else { return }
        editableInvitationDraft = eventAIInvitationMessages.text(for: variant)
    }

    private var canAccessScenarioPlanning: Bool {
        switch event.status {
        case .confirmed, .comparing, .organizing, .finalized:
            return true
        default:
            return false
        }
    }

    private var canAccessTransportPlanning: Bool {
        switch event.status {
        case .confirmed:
            return true
        case .organizing:
            return true
        case .finalized:
            return true
        default:
            return false
        }
    }

    private var canShowOrganizationDashboard: Bool {
        canAccessOrganizationDetails && organizationDashboardVisible
    }

    private var canShowGroupReadiness: Bool {
        switch event.status {
        case .confirmed, .comparing, .organizing, .finalized:
            return canAccessOrganizationDetails
        default:
            return false
        }
    }

    private var canShowWeatherContext: Bool {
        canAccessOrganizationDetails
    }

    private var organizationDashboardVisible: Bool {
        switch event.status {
        case .organizing:
            return true
        default:
            return isFinalizedOrganizationState(event)
        }
    }

    private var canAccessOrganizationDetails: Bool {
        if event.organizerId == userId {
            return true
        }

        guard let participantRecords = repository.getParticipantRecords(eventId: event.id), !participantRecords.isEmpty else {
            return false
        }

        let participantAccessStates = participantRecords.map { record in
            ParticipantAccessMapper.shared.fromRepositoryRecord(record: record)
        }
        let rows = ParticipantManagementPresentationMapper.shared.map(participants: participantAccessStates)
        let isParticipantConfirmed = rows.first { $0.userIdOrEmail == userId }?.canAccessOrganizationDetails ?? false
        return isParticipantConfirmed
    }

    private var scenarioPlanningText: String {
        switch event.status {
        case .confirmed:
            return String(localized: "event.detail.scenario.open")
        case .comparing:
            return String(localized: "event.detail.scenario.comparing")
        case .organizing:
            return String(localized: "event.detail.scenario.organizing")
        case .finalized:
            return String(localized: "event.detail.scenario.finalized_readonly")
        default:
            return String(localized: "event.detail.scenario.unavailable")
        }
    }

    private func organizationDashboardValue(_ value: String) -> String {
        isFinalizedOrganizationState(event)
            ? String(format: String(localized: "event.detail.organization.finalized_value_format"), value)
            : value
    }

    private func paymentPotSummaryValue() -> String {
        let repository = PaymentPotRepository(db: RepositoryProvider.shared.database)
        guard let pot = repository.getActivePotForEvent(eventId: event.id) else {
            return organizationDashboardValue(String(localized: "event.detail.payment_pot.define_before_share"))
        }

        guard pot.goalAmount > 0 else {
            return organizationDashboardValue(String(localized: "event.detail.payment_pot.define_goal"))
        }

        let amount = formatCurrencyAmount(pot.goalAmount, currency: pot.currency)
        return organizationDashboardValue(String(format: String(localized: "event.detail.payment_pot.goal_format"), amount))
    }

    private func tricountSummaryValue() -> String {
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        let readiness = repository.getPaymentReadiness(eventId: event.id)

        if readiness.handoff?.explicitNotNeeded == true {
            return organizationDashboardValue(String(localized: "event.detail.tricount.not_required"))
        }

        if readiness.complete {
            return organizationDashboardValue(String(localized: "event.detail.tricount.link_verified"))
        }

        if readiness.handoff != nil {
            return organizationDashboardValue(String(localized: "event.detail.tricount.link_to_check"))
        }

        return organizationDashboardValue(String(localized: "event.detail.tricount.decide_before_expenses"))
    }

    private var groupReadinessItems: [EventDetailReadinessItem] {
        [
            EventDetailReadinessItem(
                icon: "person.2.fill",
                title: String(localized: "event.detail.readiness.participants.title"),
                subtitle: event.participants.isEmpty
                    ? String(localized: "event.detail.readiness.participants.missing")
                    : String(localized: "event.detail.readiness.participants.ready"),
                isReady: !event.participants.isEmpty
            ),
            EventDetailReadinessItem(
                icon: "map.fill",
                title: String(localized: "event.detail.readiness.scenario.title"),
                subtitle: selectedScenarioLocation == nil
                    ? String(localized: "event.detail.readiness.scenario.missing")
                    : String(localized: "event.detail.readiness.scenario.ready"),
                isReady: selectedScenarioLocation != nil
            ),
            EventDetailReadinessItem(
                icon: "point.topleft.down.curvedto.point.bottomright.up.fill",
                title: String(localized: "event.detail.readiness.transport.title"),
                subtitle: isTransportReady
                    ? String(localized: "event.detail.readiness.transport.ready")
                    : String(localized: "event.detail.readiness.transport.missing"),
                isReady: isTransportReady
            ),
            EventDetailReadinessItem(
                icon: "video.fill",
                title: String(localized: "event.detail.readiness.meetings.title"),
                subtitle: hasScheduledMeeting
                    ? String(localized: "event.detail.readiness.meetings.ready")
                    : String(localized: "event.detail.readiness.meetings.missing"),
                isReady: hasScheduledMeeting
            ),
            EventDetailReadinessItem(
                icon: "eurosign.circle.fill",
                title: String(localized: "event.detail.readiness.payment.title"),
                subtitle: isPaymentReady
                    ? String(localized: "event.detail.readiness.payment.ready")
                    : String(localized: "event.detail.readiness.payment.missing"),
                isReady: isPaymentReady
            ),
            EventDetailReadinessItem(
                icon: "checklist",
                title: String(localized: "event.detail.readiness.checklist.title"),
                subtitle: hasPreparedChecklist
                    ? String(localized: "event.detail.readiness.checklist.ready")
                    : String(localized: "event.detail.readiness.checklist.missing"),
                isReady: hasPreparedChecklist
            )
        ]
    }

    private var readinessProgressText: String {
        let readyCount = groupReadinessItems.filter(\.isReady).count
        return String(format: String(localized: "event.detail.readiness.progress_format"), readyCount, groupReadinessItems.count)
    }

    private var selectedScenarioLocation: String? {
        let repository = ScenarioRepository(db: RepositoryProvider.shared.database)
        let selected = repository.getSelectedScenario(eventId: event.id)
            ?? repository.getScenariosByEventIdAndStatus(eventId: event.id, status: ScenarioStatus.selected).first
        let location = selected?.location.trimmingCharacters(in: .whitespacesAndNewlines)
        return location?.isEmpty == false ? location : nil
    }

    private var isTransportReady: Bool {
        canAccessTransportPlanning && selectedScenarioLocation != nil
    }

    private var hasScheduledMeeting: Bool {
        RepositoryProvider.shared.database.meetingQueries
            .selectByEventId(eventId: event.id)
            .executeAsList()
            .contains { $0.status != "CANCELLED" }
    }

    private var isPaymentReady: Bool {
        let pot = PaymentPotRepository(db: RepositoryProvider.shared.database)
            .getActivePotForEvent(eventId: event.id)
        let tricountReadiness = TricountHandoffRepository(db: RepositoryProvider.shared.database)
            .getPaymentReadiness(eventId: event.id)
        return (pot?.goalAmount ?? 0) > 0 || tricountReadiness.complete
    }

    private var hasPreparedChecklist: Bool {
        !eventAIChecklist.isEmpty || !preparedCreationChecklist.isEmpty
    }

    private func parseDate(_ value: String) -> Date? {
        let isoFormatter = ISO8601DateFormatter()
        if let date = isoFormatter.date(from: value) {
            return date
        }

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "fr_FR")
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        return formatter.date(from: value)
    }

    private func formatSlot(_ slot: TimeSlot) -> String {
        guard let startValue = slot.start, let start = parseDate(startValue) else {
            return String(localized: "event.detail.slot_to_confirm")
        }

        let dateFormatter = DateFormatter()
        dateFormatter.locale = .autoupdatingCurrent
        dateFormatter.setLocalizedDateFormatFromTemplate("d MMM")

        let timeFormatter = DateFormatter()
        timeFormatter.locale = .autoupdatingCurrent
        timeFormatter.timeStyle = .short

        return "\(dateFormatter.string(from: start)) · \(timeFormatter.string(from: start))"
    }

    private func formatEventDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = .autoupdatingCurrent
        formatter.setLocalizedDateFormatFromTemplate("d MMM")
        return formatter.string(from: date)
    }

    private func formatLongEventDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = .autoupdatingCurrent
        formatter.setLocalizedDateFormatFromTemplate("EEEEdMMMM")
        return formatter.string(from: date)
    }
}

private struct EventDetailReadinessItem: Identifiable {
    let id = UUID()
    let icon: String
    let title: String
    let subtitle: String
    let isReady: Bool
}

private struct EventDetailReadinessCard: View {
    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let subtitle: String
    let progressText: String
    let items: [EventDetailReadinessItem]

    var body: some View {
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack(alignment: .top, spacing: WakeveTheme.Spacing.sm) {
                    Image(systemName: isComplete ? "checkmark.seal.fill" : "checklist")
                        .font(.headline.weight(.bold))
                        .foregroundColor(statusColor)
                        .frame(width: 42, height: 42)
                        .background(statusColor.opacity(0.14), in: Circle())

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text(title)
                            .font(WakeveTheme.Typography.section)
                            .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

                        Text(subtitle)
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                            .lineLimit(2)
                    }

                    Spacer(minLength: WakeveTheme.Spacing.sm)

                    Text(progressText)
                        .font(WakeveTheme.Typography.caption.weight(.bold))
                        .foregroundColor(statusColor)
                        .padding(.horizontal, WakeveTheme.Spacing.sm)
                        .padding(.vertical, WakeveTheme.Spacing.xs)
                        .background(statusColor.opacity(0.12), in: Capsule())
                }

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: WakeveTheme.Spacing.sm) {
                    ForEach(items) { item in
                        EventDetailReadinessTile(item: item)
                    }
                }
            }
        }
    }

    private var isComplete: Bool {
        !items.isEmpty && items.allSatisfy(\.isReady)
    }

    private var statusColor: Color {
        isComplete ? SemanticColor.confirmation(for: colorScheme) : SemanticColor.warning(for: colorScheme)
    }
}

private struct EventDetailReadinessTile: View {
    @Environment(\.colorScheme) private var colorScheme

    let item: EventDetailReadinessItem

    var body: some View {
        HStack(alignment: .top, spacing: WakeveTheme.Spacing.sm) {
            Image(systemName: item.isReady ? "checkmark.circle.fill" : item.icon)
                .font(.caption.weight(.bold))
                .foregroundColor(statusColor)
                .frame(width: 28, height: 28)
                .background(statusColor.opacity(0.12), in: Circle())

            VStack(alignment: .leading, spacing: 2) {
                Text(item.title)
                    .font(WakeveTheme.Typography.caption.weight(.semibold))
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                    .lineLimit(1)
                    .minimumScaleFactor(0.78)

                Text(item.subtitle)
                    .font(WakeveTheme.Typography.tiny)
                    .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                    .lineLimit(2)
                    .minimumScaleFactor(0.82)
            }

            Spacer(minLength: 0)
        }
        .padding(WakeveTheme.Spacing.sm)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
    }

    private var statusColor: Color {
        item.isReady ? SemanticColor.confirmation(for: colorScheme) : SemanticColor.warning(for: colorScheme)
    }
}

private struct EventAnticipationItem: Identifiable {
    let id = UUID()
    let icon: String
    let title: String
    let subtitle: String
}

private struct EventDetailAnticipationCard: View {
    @Environment(\.colorScheme) private var colorScheme

    let countdownTitle: String
    let countdownSubtitle: String
    let items: [EventAnticipationItem]
    let returnHookTitle: String
    let returnHookMessage: String

    var body: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack(alignment: .center, spacing: WakeveTheme.Spacing.md) {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text(String(localized: "event.detail.anticipation.upcoming_label"))
                            .font(WakeveTheme.Typography.caption)
                            .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                            .textCase(.uppercase)

                        Text(countdownTitle)
                            .font(.system(size: 34, weight: .black, design: .rounded))
                            .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

                        Text(countdownSubtitle)
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                    }

                    Spacer()

                    Image(systemName: "sparkles")
                        .font(.title2.weight(.bold))
                        .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme))
                        .frame(width: 48, height: 48)
                        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme), in: Circle())
                }

                VStack(spacing: WakeveTheme.Spacing.sm) {
                    ForEach(items) { item in
                        EventAnticipationRow(item: item)
                    }
                }

                ShareLink(item: returnHookMessage) {
                    HStack(spacing: WakeveTheme.Spacing.sm) {
                        Image(systemName: "square.and.arrow.up")
                            .font(.caption.weight(.bold))

                        Text(returnHookTitle)
                            .font(WakeveTheme.Typography.callout.weight(.semibold))

                        Spacer(minLength: 0)

                        Image(systemName: "message.fill")
                            .font(.caption.weight(.bold))
                    }
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                    .padding(.horizontal, WakeveTheme.Spacing.md)
                    .frame(height: 48)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
                }
                .simultaneousGesture(TapGesture().onEnded {
                    WakeveHaptics.selection()
                })
                .accessibilityIdentifier("eventAnticipationReturnHookShare")
            }
        }
    }
}

private struct EventAnticipationRow: View {
    @Environment(\.colorScheme) private var colorScheme

    let item: EventAnticipationItem

    var body: some View {
        HStack(alignment: .top, spacing: WakeveTheme.Spacing.sm) {
            Image(systemName: item.icon)
                .font(.caption.weight(.bold))
                .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme))
                .frame(width: 28, height: 28)
                .background(WakeveTheme.ColorToken.controlFill(for: colorScheme), in: Circle())

            VStack(alignment: .leading, spacing: 2) {
                Text(item.title)
                    .font(WakeveTheme.Typography.callout.weight(.semibold))
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                    .lineLimit(1)
                    .minimumScaleFactor(0.82)

                Text(item.subtitle)
                    .font(WakeveTheme.Typography.caption)
                    .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                    .lineLimit(2)
            }

            Spacer(minLength: 0)
        }
    }
}

private enum EventAIInvitationVariant: String, CaseIterable, Identifiable {
    case simple
    case warm
    case shortWhatsApp

    var id: String { rawValue }

    var title: String {
        switch self {
        case .simple: return String(localized: "event.detail.ai.invitation_variant.simple")
        case .warm: return String(localized: "event.detail.ai.invitation_variant.warm")
        case .shortWhatsApp: return String(localized: "event.detail.ai.invitation_variant.short_whatsapp")
        }
    }
}

private extension InvitationMessageSet {
    func text(for variant: EventAIInvitationVariant) -> String {
        switch variant {
        case .simple: return simple
        case .warm: return warm
        case .shortWhatsApp: return shortWhatsApp
        }
    }
}

private struct EventDetailWakeveAIContextProvider: WakeveAIContextProviding {
    let eventId: String
    let title: String
    let date: String
    let location: String?
    let participantNames: [String]
    let voteSummaries: [String]
    let status: String
    let message: String

    var facts: WakeveAIKnownFacts {
        WakeveAIKnownFacts(
            participantNames: Set(participantNames),
            voteLabels: Set(voteSummaries),
            availabilityLabels: [status]
        )
    }

    var promptSummary: String {
        WakeveAIEventContext(
            eventId: eventId,
            title: title,
            date: date,
            location: location,
            participantNames: participantNames,
            voteSummaries: voteSummaries,
            taskTitles: [status],
            recentMessages: [message]
        ).promptSummary
    }

    func currentGroup() async -> WakeveAIGroupContext? {
        WakeveAIGroupContext(groupId: eventId, memberDisplayNames: participantNames)
    }

    func eventContext(eventId: String) async -> WakeveAIEventContext? {
        WakeveAIEventContext(
            eventId: eventId,
            title: title,
            date: date,
            location: location,
            participantNames: participantNames,
            voteSummaries: voteSummaries,
            taskTitles: [status],
            recentMessages: [message]
        )
    }

    func participantStatuses(eventId: String) async -> WakeveAIParticipantStatuses? {
        WakeveAIParticipantStatuses(accepted: [], pending: participantNames, declined: [])
    }

    func voteResults(eventId: String) async -> WakeveAIVoteResults? {
        WakeveAIVoteResults(activePolls: voteSummaries, results: voteSummaries)
    }

    func transportContext(eventId: String) async -> WakeveAITransportContext? { nil }

    func userPreferences() async -> WakeveAIUserPreferences? {
        WakeveAIUserPreferences(languageCode: "fr", localPreferences: [])
    }
}

private struct EventAISummaryBlock: View {
    let summary: EventSummary
    let colorScheme: ColorScheme
    let applied: Bool
    let ignored: Bool
    let onModify: () -> Void
    let onApply: () -> Void
    let onIgnore: () -> Void

    var body: some View {
        EventAIReviewBox(title: String(localized: "event.detail.ai.summary_title"), colorScheme: colorScheme, applied: applied, ignored: ignored, onModify: onModify, onApply: onApply, onIgnore: onIgnore) {
            EventAIList(title: String(localized: "event.detail.ai.decided_title"), values: summary.decided, colorScheme: colorScheme)
            EventAIList(title: String(localized: "event.detail.ai.missing_title"), values: summary.missing, colorScheme: colorScheme)
            Text(summary.recommendedNextAction)
                .font(WakeveTheme.Typography.callout)
                .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
        }
    }
}

private struct EventAIPollSuggestionRow: View {
    let poll: PollSuggestion
    let colorScheme: ColorScheme
    let applied: Bool
    let ignored: Bool
    let onModify: () -> Void
    let onApply: () -> Void
    let onIgnore: () -> Void

    var body: some View {
        EventAIReviewBox(title: poll.question, colorScheme: colorScheme, applied: applied, ignored: ignored, onModify: onModify, onApply: onApply, onIgnore: onIgnore) {
            Text(poll.options.joined(separator: " · "))
                .font(WakeveTheme.Typography.caption)
                .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                .lineLimit(3)
        }
    }
}

private struct EventAIChecklistRow: View {
    let item: ChecklistItem
    let colorScheme: ColorScheme
    let applied: Bool
    let ignored: Bool
    let onModify: () -> Void
    let onApply: () -> Void
    let onIgnore: () -> Void

    var body: some View {
        EventAIReviewBox(title: item.title, colorScheme: colorScheme, applied: applied, ignored: ignored, onModify: onModify, onApply: onApply, onIgnore: onIgnore) {
            Text("\(categoryLabel) · \(priorityLabel)")
                .font(WakeveTheme.Typography.caption)
                .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
        }
    }

    private var categoryLabel: String {
        switch item.category {
        case .food: return String(localized: "create_event.checklist.food")
        case .transport: return String(localized: "create_event.checklist.transport")
        case .venue: return String(localized: "create_event.checklist.venue")
        case .guests: return String(localized: "create_event.checklist.guests")
        case .equipment: return String(localized: "create_event.checklist.equipment")
        case .budget: return String(localized: "create_event.checklist.budget")
        }
    }

    private var priorityLabel: String {
        switch item.priority {
        case .high: return String(localized: "event.detail.ai.priority.high")
        case .medium: return String(localized: "event.detail.ai.priority.medium")
        case .low: return String(localized: "event.detail.ai.priority.low")
        }
    }
}

private struct EventAIInvitationBlock: View {
    @Binding var variant: EventAIInvitationVariant
    @Binding var editableDraft: String

    let colorScheme: ColorScheme
    let applied: Bool
    let ignored: Bool
    let onSelectVariant: (EventAIInvitationVariant) -> Void
    let onModify: () -> Void
    let onApply: () -> Void
    let onIgnore: () -> Void

    var body: some View {
        EventAIReviewBox(title: String(localized: "event.detail.ai.invitation_title"), colorScheme: colorScheme, applied: applied, ignored: ignored, onModify: onModify, onApply: onApply, onIgnore: onIgnore) {
            HStack(spacing: 8) {
                ForEach(EventAIInvitationVariant.allCases) { option in
                    Button {
                        onSelectVariant(option)
                    } label: {
                        Text(option.title)
                            .font(WakeveTheme.Typography.tiny.weight(.semibold))
                            .foregroundColor(variant == option ? .white : WakeveTheme.ColorToken.primaryText(for: colorScheme))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                            .background(variant == option ? WakeveTheme.ColorToken.accent(for: colorScheme) : WakeveTheme.ColorToken.controlFill(for: colorScheme))
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }

            TextEditor(text: $editableDraft)
                .font(WakeveTheme.Typography.callout)
                .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                .frame(minHeight: 86)
                .padding(WakeveTheme.Spacing.xs)
                .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
        }
    }
}

private struct EventAIReviewBox<Content: View>: View {
    let title: String
    let colorScheme: ColorScheme
    let applied: Bool
    let ignored: Bool
    let onModify: () -> Void
    let onApply: () -> Void
    let onIgnore: () -> Void
    let content: Content

    init(
        title: String,
        colorScheme: ColorScheme,
        applied: Bool,
        ignored: Bool,
        onModify: @escaping () -> Void,
        onApply: @escaping () -> Void,
        onIgnore: @escaping () -> Void,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.colorScheme = colorScheme
        self.applied = applied
        self.ignored = ignored
        self.onModify = onModify
        self.onApply = onApply
        self.onIgnore = onIgnore
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
            HStack(alignment: .top) {
                Text(title)
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                    .lineLimit(3)

                Spacer()

                if applied {
                    Label(String(localized: "event.detail.ai.applied"), systemImage: "checkmark.circle.fill")
                        .font(WakeveTheme.Typography.tiny)
                        .foregroundColor(WakeveTheme.ColorToken.confirmation(for: colorScheme))
                } else if ignored {
                    Label(String(localized: "event.detail.ai.ignored"), systemImage: "minus.circle.fill")
                        .font(WakeveTheme.Typography.tiny)
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                }
            }

            content

            HStack(spacing: WakeveTheme.Spacing.sm) {
                EventAIActionButton(title: String(localized: "common.edit"), systemImage: "pencil", colorScheme: colorScheme, action: onModify)
                EventAIActionButton(title: String(localized: "common.apply"), systemImage: "checkmark", colorScheme: colorScheme, action: onApply)
                EventAIActionButton(title: String(localized: "event.detail.ai.ignore_action"), systemImage: "xmark", colorScheme: colorScheme, action: onIgnore)
            }
        }
        .padding(WakeveTheme.Spacing.sm)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
    }
}

private struct EventAIActionButton: View {
    let title: String
    let systemImage: String
    let colorScheme: ColorScheme
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .font(WakeveTheme.Typography.tiny.weight(.semibold))
                .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                .frame(maxWidth: .infinity)
                .frame(height: 36)
                .background(WakeveTheme.ColorToken.pageBackground(for: colorScheme).opacity(0.72))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

private struct EventAIList: View {
    let title: String
    let values: [String]
    let colorScheme: ColorScheme

    var body: some View {
        if !values.isEmpty {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(WakeveTheme.Typography.tiny.weight(.semibold))
                    .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                    .textCase(.uppercase)

                ForEach(values, id: \.self) { value in
                    Label(value, systemImage: "checkmark.circle")
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                        .lineLimit(2)
                }
            }
        }
    }
}

private struct EventDetailHeroMetric: View {
    let systemImage: String
    let value: String

    var body: some View {
        HStack(spacing: WakeveTheme.Spacing.xs) {
            Image(systemName: systemImage)
                .font(.caption.weight(.bold))
            Text(value)
                .font(WakeveTheme.Typography.caption)
                .lineLimit(1)
                .minimumScaleFactor(0.78)
        }
        .foregroundColor(.white.opacity(0.88))
        .padding(.horizontal, WakeveTheme.Spacing.sm)
        .padding(.vertical, WakeveTheme.Spacing.xs)
        .background(Color.black.opacity(0.24))
        .clipShape(Capsule())
    }
}

private struct EventDetailMetadataPill: View {
    @Environment(\.colorScheme) private var colorScheme

    let icon: String
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xs) {
            Image(systemName: icon)
                .font(.body.weight(.semibold))
                .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme))

            Text(title)
                .font(WakeveTheme.Typography.tiny)
                .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                .lineLimit(1)
                .minimumScaleFactor(0.78)

            Text(value)
                .font(WakeveTheme.Typography.callout.weight(.semibold))
                .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                .lineLimit(2)
                .minimumScaleFactor(0.82)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(WakeveTheme.Spacing.sm)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
    }
}

private struct EventDetailNextActionCard: View {
    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let subtitle: String
    let systemImage: String
    let blockedReason: String?
    let isDisabled: Bool
    let action: () -> Void

    var body: some View {
        WakeveGlassCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            HStack(spacing: WakeveTheme.Spacing.md) {
                Image(systemName: systemImage)
                    .font(.title3.weight(.bold))
                    .foregroundColor(.white)
                    .frame(width: 48, height: 48)
                    .background(WakeveTheme.ColorToken.progress(for: colorScheme))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                    HStack(spacing: WakeveTheme.Spacing.xs) {
                        Text(String(localized: "events.next_action.label"))
                            .font(WakeveTheme.Typography.tiny)
                            .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                            .textCase(.uppercase)

                        if blockedReason != nil {
                            Text(String(localized: "events.next_action.blocked_label"))
                                .font(WakeveTheme.Typography.tiny)
                                .foregroundColor(.orange)
                                .padding(.horizontal, WakeveTheme.Spacing.xs)
                                .padding(.vertical, 2)
                                .background(Color.orange.opacity(0.14))
                                .clipShape(Capsule())
                        }
                    }
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)

                    Text(title)
                        .font(WakeveTheme.Typography.rowTitle)
                        .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

                    Text(subtitle)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                        .lineLimit(2)
                }

                Spacer(minLength: WakeveTheme.Spacing.xs)

                Button(action: action) {
                    Image(systemName: "arrow.right")
                        .font(.headline.weight(.bold))
                        .foregroundColor(colorScheme == .dark ? WakeveTheme.ColorToken.midnight : .white)
                        .frame(width: 42, height: 42)
                        .background(colorScheme == .dark ? Color.white.opacity(0.9) : WakeveTheme.ColorToken.accent(for: colorScheme))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .disabled(isDisabled)
                .accessibilityLabel(title)
                .opacity(isDisabled ? WakeveTheme.Opacity.disabled : 1)
            }
        }
    }
}

private struct EventDetailParticipantsPreview: View {
    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let subtitle: String
    let initials: [String]
    let action: () -> Void

    var body: some View {
        WakeveGlassCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            Button(action: action) {
                HStack(spacing: WakeveTheme.Spacing.md) {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text(title)
                            .font(WakeveTheme.Typography.section)
                            .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

                        Text(subtitle)
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                            .lineLimit(2)
                    }

                    Spacer()

                    ParticipantAvatarStack(initials: initials, size: 36, maxVisible: 4)

                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.bold))
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                }
            }
            .buttonStyle(.plain)
        }
    }
}

private struct EventDetailSectionCard<Content: View>: View {
    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let content: Content

    init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        WakeveGlassCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                Text(title)
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

                VStack(spacing: 0) {
                    content
                }
            }
        }
    }
}

private struct EventDetailActionRow: View {
    @Environment(\.colorScheme) private var colorScheme

    let icon: String
    let label: String
    let value: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: WakeveTheme.Spacing.md) {
                Image(systemName: icon)
                    .font(.body.weight(.semibold))
                    .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme))
                    .frame(width: 38, height: 38)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.sm, style: .continuous))

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                    Text(label)
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                        .lineLimit(1)
                        .minimumScaleFactor(0.78)

                    Text(value)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                        .lineLimit(2)
                }

                Spacer(minLength: WakeveTheme.Spacing.xs)

                Image(systemName: "chevron.right")
                    .font(.caption.weight(.bold))
                    .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
            }
            .padding(.vertical, WakeveTheme.Spacing.sm)
        }
        .buttonStyle(.plain)
    }
}

private struct EventDetailMessagePreview: View {
    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let subtitle: String
    let badgeText: String

    var body: some View {
        WakeveGlassCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            HStack(spacing: WakeveTheme.Spacing.md) {
                Image(systemName: "message.fill")
                    .font(.title3.weight(.bold))
                    .foregroundColor(WakeveTheme.ColorToken.eventHighlight(for: colorScheme))
                    .frame(width: 46, height: 46)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                    HStack(spacing: WakeveTheme.Spacing.xs) {
                        Text(title)
                            .font(WakeveTheme.Typography.rowTitle)
                            .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

                        Text(badgeText)
                            .font(WakeveTheme.Typography.tiny)
                            .foregroundColor(WakeveTheme.ColorToken.eventHighlight(for: colorScheme))
                            .padding(.horizontal, WakeveTheme.Spacing.xs)
                            .padding(.vertical, 3)
                            .background(WakeveTheme.ColorToken.eventHighlight(for: colorScheme).opacity(0.14))
                            .clipShape(Capsule())
                    }

                    Text(subtitle)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                        .lineLimit(2)
                }
            }
        }
    }
}

private struct OrganizerChip: View {
    @Environment(\.colorScheme) private var colorScheme

    let icon: String
    let title: String
    let subtitle: String
    let tint: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(tint)
                    .frame(width: 34, height: 34)
                    .background(tint.opacity(0.16))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(primaryText)
                    Text(subtitle)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(secondaryText)
                        .lineLimit(1)
                        .minimumScaleFactor(0.75)
                }

                Spacer(minLength: 0)
            }
            .padding(12)
            .frame(maxWidth: .infinity)
            .background(surfaceColor)
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(borderColor, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 18))
        }
        .buttonStyle(.plain)
    }

    private var primaryText: Color {
        colorScheme == .dark ? .white : Color(hex: "17171F")
    }

    private var secondaryText: Color {
        colorScheme == .dark ? Color.white.opacity(0.55) : Color(hex: "646775")
    }

    private var surfaceColor: Color {
        colorScheme == .dark ? Color.white.opacity(0.075) : Color.white.opacity(0.78)
    }

    private var borderColor: Color {
        colorScheme == .dark ? Color.white.opacity(0.08) : Color.black.opacity(0.08)
    }
}

private struct EventPreviewDetailRow: View {
    @Environment(\.colorScheme) private var colorScheme

    let icon: String
    let label: String
    let value: String
    let accessory: String?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.system(size: 21, weight: .semibold))
                    .foregroundColor(iconForeground)
                    .frame(width: 42, height: 42)
                    .background(iconBackground)
                    .clipShape(RoundedRectangle(cornerRadius: 10))

                VStack(alignment: .leading, spacing: 2) {
                    Text(label)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(secondaryText)

                    Text(value)
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(primaryText)
                        .lineLimit(1)
                        .minimumScaleFactor(0.78)
                }

                Spacer()

                if let accessory {
                    Image(systemName: accessory)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(secondaryText)
                }
            }
            .padding(13)
            .frame(minHeight: 68)
            .background(surfaceColor)
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(borderColor, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
        .buttonStyle(.plain)
    }

    private var primaryText: Color {
        colorScheme == .dark ? .white : Color(hex: "17171F")
    }

    private var secondaryText: Color {
        colorScheme == .dark ? Color.white.opacity(0.5) : Color(hex: "6B6E7D")
    }

    private var surfaceColor: Color {
        colorScheme == .dark ? Color.white.opacity(0.07) : Color.white.opacity(0.84)
    }

    private var borderColor: Color {
        colorScheme == .dark ? Color.white.opacity(0.07) : Color.black.opacity(0.08)
    }

    private var iconForeground: Color {
        colorScheme == .dark ? .white : Color(hex: "17171F")
    }

    private var iconBackground: Color {
        colorScheme == .dark ? Color.white.opacity(0.14) : Color.black.opacity(0.08)
    }
}

#if DEBUG
struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
            .previewEnvironment(isAuthenticated: true)
            .preferredColorScheme(.light)
    }
}

#Preview("Content - Login") {
    ContentView()
        .previewEnvironment(isAuthenticated: false)
        .preferredColorScheme(.dark)
}
#endif
