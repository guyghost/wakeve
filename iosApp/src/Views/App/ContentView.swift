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

                Text("Something went wrong")
                    .font(.title2)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)

                Text(message)
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)

                Button(action: onRetry) {
                    Text("Try Again")
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
    // Use persistent database-backed repository instead of in-memory mock
    private let repository: EventRepositoryInterface = RepositoryProvider.shared.repository
    @State private var showEventCreationSheet = false
    
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
                userName: authStateManager.currentUser?.name
            ) { event in
                // Save event to repository
                Task {
                    do {
                        try await repository.saveEvent(event: event)
                        await MainActor.run {
                            // Navigate to participant management
                            selectedEvent = event
                            selectedTab = .home
                            currentView = .participantManagement
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
                    showEventCreationSheet = true
                },
                onProfileClick: {
                    selectedTab = .profile
                }
            )
            
        case .eventCreation:
            Text("Création d'événement")
                .font(.title2)
                .foregroundColor(.secondary)
            
        case .eventDetail:
            if let event = selectedEvent {
                EventDetailView(
                    event: event,
                    repository: repository,
                    userId: userId,
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
                    onBack: {
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
                Text("Sélectionnez un événement pour voir les scénarios")
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
                Text("Sélectionnez un événement pour comparer les scénarios")
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .budgetOverview:
            if let event = selectedEvent {
                if canAccessOrganizationDetails(for: event) {
                    BudgetOverviewView(eventId: event.id)
                } else {
                    AccessDenied(message: "Confirmez votre présence avant d'ouvrir le budget.") {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text("Sélectionnez un événement pour voir le budget")
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
                Text("Selectionnez un evenement pour voir le logement")
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .mealPlanning:
            if selectedEvent != nil {
                // Meal planning stays behind a placeholder until shared types are integrated.
                Text("Planification des repas")
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .equipmentChecklist:
            if selectedEvent != nil {
                Text("Liste d'équipement")
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .activityPlanning:
            Text("Planification des activités")
                .font(.title2)
                .foregroundColor(.secondary)
            
        case .scenarioDetail:
            Text("Détail du scénario")
                .font(.title2)
                .foregroundColor(.secondary)
            
        case .budgetDetail:
            if let event = selectedEvent {
                if canAccessOrganizationDetails(for: event) {
                    BudgetDetailView(eventId: event.id)
                } else {
                    AccessDenied(message: "Confirmez votre présence avant d'ouvrir les dépenses.") {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text("Sélectionnez un événement pour voir les dépenses")
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .meetingList:
            if let event = selectedEvent {
                if isParticipantConfirmed(for: event) == true || canAccessOrganizationDetails(for: event) {
                    MeetingListView(
                        eventId: event.id,
                        currentUserId: userId,
                        isOrganizer: event.organizerId == userId,
                        canCreateMeetings: event.organizerId == userId && event.status == .organizing,
                        isReadOnly: isFinalizedOrganizationState(event)
                    )
                } else {
                    AccessDenied(message: "Confirmez votre présence avant d'ouvrir les réunions.") {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text("Sélectionnez un événement pour voir les réunions")
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .meetingDetail:
            if let meetingId = selectedMeetingId, let event = selectedEvent {
                MeetingDetailView(
                    meetingId: meetingId,
                    eventId: event.id,
                    currentUserId: userId,
                    isOrganizer: event.organizerId == userId,
                    isReadOnly: isFinalizedOrganizationState(event)
                )
            } else {
                Text("Sélectionnez une réunion")
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
                        currentUserId: userId,
                        isOrganizer: event.organizerId == userId,
                        canManagePayment: canManagePayment,
                        isReadOnly: isFinalizedOrganizationState(event)
                    )
                } else {
                    AccessDenied(message: "Confirmez votre présence avant d'ouvrir la cagnotte.") {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text("Sélectionnez un événement pour voir la cagnotte")
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
                    AccessDenied(message: "Confirmez votre présence avant d'ouvrir Tricount.") {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text("Sélectionnez un événement pour voir Tricount")
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .transportPlanning:
            if let event = selectedEvent {
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
                Text("Sélectionnez un événement pour le transport")
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
            ExploreTabView { _ in
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
        VStack(spacing: 16) {
            Image(systemName: "lock.fill")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text("Accès restreint")
                .font(.title3.bold())
            Text(message)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            Button("Retour", action: onBack)
                .buttonStyle(.borderedProminent)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct Phase5SyncBanner: View {
    let pendingSync: Bool
    let isOnline: Bool

    var body: some View {
        if pendingSync || !isOnline {
            Label(
                pendingSync ? "Modifications locales en attente d'envoi" : "Données locales disponibles hors ligne",
                systemImage: "arrow.triangle.2.circlepath"
            )
            .font(.footnote.weight(.semibold))
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.yellow.opacity(0.16), in: RoundedRectangle(cornerRadius: 12))
        }
    }
}

private struct PaymentPotView: View {
    let eventId: String
    let currentUserId: String
    let isOrganizer: Bool
    let isReadOnly: Bool
    private let canManagePayment: Bool
    private let pendingSync: Bool
    @State private var statusText = "Aucune cagnotte active"

    init(eventId: String, currentUserId: String, isOrganizer: Bool, canManagePayment: Bool, isReadOnly: Bool = false) {
        self.eventId = eventId
        self.currentUserId = currentUserId
        self.isOrganizer = isOrganizer
        self.isReadOnly = isReadOnly
        self.canManagePayment = canManagePayment
        self.pendingSync = Phase5PendingSync.selectPending(eventId: eventId)
    }

    var body: some View {
        NavigationStack {
            List {
                Phase5SyncBanner(pendingSync: pendingSync, isOnline: !pendingSync)
                Section("Cagnotte") {
                    Label("Cagnotte partagée", systemImage: "creditcard.fill")
                    Text(statusText)
                        .foregroundStyle(.secondary)
                    Text("Les contributions pour \(eventId) restent disponibles sur cet appareil et seront envoyées dès que la connexion sera disponible.")
                        .foregroundStyle(.secondary)
                    Button("Créer une cagnotte") {
                        onCreatePaymentPot()
                    }
                    .disabled(!canManagePayment)
                    Button("Activer la cagnotte") {
                        onActivatePaymentPot()
                    }
                    .disabled(!canManagePayment)
                    Button("Ouvrir la cagnotte") {
                        onOpenPaymentPot()
                    }
                    Button("Clôturer la cagnotte") {
                        onClosePaymentPot()
                    }
                    .disabled(!canManagePayment)
                }
            }
            .navigationTitle("Cagnotte")
        }
    }

    private func onCreatePaymentPot() {
        guard canManagePayment else { return }
        let repository = PaymentPotRepository(db: RepositoryProvider.shared.database)
        _ = repository.createPot(
            eventId: eventId,
            organizerId: currentUserId,
            goalAmount: 0,
            title: "Cagnotte \(eventId)",
            currency: "EUR",
            paymentProvider: "TRICOUNT",
            tricountGroupId: nil,
            tricountGroupUrl: nil
        )
            statusText = "Cagnotte créée. Synchronisation en attente."
    }

    private func onActivatePaymentPot() {
        onCreatePaymentPot()
    }

    private func onOpenPaymentPot() {
        statusText = "Cagnotte ouverte localement"
    }

    private func onClosePaymentPot() {
        guard canManagePayment else { return }
        let repository = PaymentPotRepository(db: RepositoryProvider.shared.database)
        if let pot = repository.getActivePotForEvent(eventId: eventId),
           repository.closePot(id: pot.id) != nil {
            statusText = "Cagnotte clôturée. Synchronisation en attente."
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

    init(eventId: String, currentUserId: String, isOrganizer: Bool, canManageTricount: Bool, isReadOnly: Bool = false) {
        self.eventId = eventId
        self.currentUserId = currentUserId
        self.isOrganizer = isOrganizer
        self.isReadOnly = isReadOnly
        self.canManageTricount = canManageTricount
        self.pendingSync = Phase5PendingSync.selectPending(eventId: eventId)
        self._safeLink = State(initialValue: Self.loadSafeLink(eventId: eventId))
    }

    private static func loadSafeLink(eventId: String) -> SafeExternalLink? {
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        let handoff = repository.getHandoff(eventId: eventId)
        return handoff?.providerUrl.flatMap { rawUrl in
            SafeExternalLink.sanitize(
                label: "Ouvrir Tricount",
                provider: "TRICOUNT",
                rawURL: rawUrl,
                verifier: { provider, url in
                    repository.isTrustedProviderUrl(provider: provider, providerUrl: url)
                }
            )
        }
    }

    var body: some View {
        NavigationStack {
            List {
                Phase5SyncBanner(pendingSync: pendingSync, isOnline: !pendingSync)
                Section("Tricount") {
                    if let safeLink, safeLink.isVerified, safeLink.validatedURL != nil {
                        Button {
                            openSafeURL(safeLink)
                        } label: {
                            Label(safeLink.label, systemImage: "link")
                        }
                        Text("Lien \(safeLink.verificationStatus)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    } else {
                        Label("Aucun lien Tricount vérifié", systemImage: "lock.shield")
                        Text("Ajoutez un lien Tricount validé avant toute ouverture externe.")
                            .foregroundStyle(.secondary)
                    }
                    Button("Associer Tricount") {
                        onLinkTricount()
                    }
                    .disabled(!canManageTricount)
                    Button("Dissocier Tricount") {
                        onUnlinkTricount()
                    }
                    .disabled(!canManageTricount)
                    Button("Tricount non requis") {
                        onMarkTricountNotNeeded()
                    }
                    .disabled(!canManageTricount)
                }
            }
            .navigationTitle("Tricount")
        }
    }

    private func onLinkTricount() {
        guard canManageTricount else { return }
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        _ = repository.linkHandoff(
            eventId: eventId,
            provider: "TRICOUNT",
            providerId: "tricount-\(eventId)",
            providerUrl: "https://tricount.com/group/\(eventId)",
            syncStatus: "LINKED"
        )
        safeLink = Self.loadSafeLink(eventId: eventId)
    }

    private func onUnlinkTricount() {
        guard canManageTricount else { return }
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        repository.unlinkHandoff(eventId: eventId)
        safeLink = nil
    }

    private func onMarkTricountNotNeeded() {
        guard canManageTricount else { return }
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        _ = repository.markNotNeeded(eventId: eventId, decidedBy: currentUserId)
        safeLink = nil
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
            verificationStatus: "vérifié",
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
                
                Text("Collaborative Event Planning")
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
                                
                                Text("Create New Event")
                                    .font(.system(size: 18, weight: .semibold, design: .rounded))
                                    .foregroundColor(.primary)
                                
                                Text("Start planning your collaborative event")
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
                            
                            Text("Loading events...")
                                .font(.system(size: 14, design: .rounded))
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 40)
                    } else if events.isEmpty {
                        VStack(spacing: 16) {
                            Image(systemName: "calendar.badge.exclamationmark")
                                .font(.system(size: 48))
                                .foregroundColor(Color(.tertiaryLabel))
                            
                            Text("No events yet")
                                .font(.system(size: 16, weight: .medium, design: .rounded))
                                .foregroundColor(.secondary)
                            
                            Text("Create your first event to get started")
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
                        
                        Text("\(event.participants.count) participants")
                            .font(.system(size: 12, design: .rounded))
                            .foregroundColor(Color(.tertiaryLabel))
                    }
                }
                
                HStack {
                    Image(systemName: "calendar")
                        .font(.system(size: 14))
                        .foregroundColor(Color(.tertiaryLabel))
                    
                    Text("\(event.proposedSlots.count) time slots")
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
        case .draft: return "Draft"
        case .polling: return "Polling"
        case .confirmed: return "Confirmed"
        default: return "Unknown"
        }
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
    let onManageParticipants: () -> Void
    let onVote: () -> Void
    let onViewResults: () -> Void
    let onOrganize: () -> Void
    let onOpenTransport: () -> Void
    let onOpenMeetings: () -> Void
    let onOpenBudget: () -> Void
    let onOpenPayment: () -> Void
    let onOpenTricount: () -> Void
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

    var body: some View {
        ZStack {
            WakeveTheme.ColorToken.pageBackground(for: colorScheme)
                .ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                    heroSection
                    metadataOverview
                    eventAISuggestionPanel
                    urgentNextAction
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
                .padding(.top, 92)
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
        LiquidGlassCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text("Résumé")
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: WakeveTheme.Spacing.sm) {
                    EventDetailMetadataPill(icon: statusIcon, title: "Statut", value: statusText)
                    EventDetailMetadataPill(icon: "timer", title: "Sondage", value: pollDurationText)
                    EventDetailMetadataPill(icon: "calendar", title: "Créneaux", value: "\(event.proposedSlots.count) option\(event.proposedSlots.count > 1 ? "s" : "")")
                    EventDetailMetadataPill(icon: "person.2.fill", title: "Invités", value: participantSummary)
                }
            }
        }
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
                title: "Inviter",
                subtitle: "Participants",
                tint: Color(hex: "7DD3FC"),
                action: onManageParticipants
            )

            OrganizerChip(
                icon: "chart.bar.xaxis",
                title: "Sondage",
                subtitle: pollDurationText,
                tint: Color(hex: "FDE68A"),
                action: event.status == .draft ? onManageParticipants : onViewResults
            )
        }
    }

    private var urgentNextAction: some View {
        EventDetailNextActionCard(
            title: primaryActionTitle,
            subtitle: nextActionSubtitle,
            systemImage: nextActionIcon,
            isDisabled: primaryActionDisabled,
            action: primaryAction
        )
    }

    private var participantsPreview: some View {
        EventDetailParticipantsPreview(
            title: "Participants",
            subtitle: participantPreviewSubtitle,
            initials: participantInitials,
            action: onManageParticipants
        )
    }

    private var detailRows: some View {
        EventDetailSectionCard(title: "Organisation") {
            EventDetailActionRow(
                icon: "calendar",
                label: "Créneaux proposés",
                value: "\(event.proposedSlots.count) option\(event.proposedSlots.count > 1 ? "s" : "")",
                action: event.status == .polling ? onVote : onViewResults
            )

            if canAccessScenarioPlanning {
                EventDetailActionRow(
                    icon: "map.fill",
                    label: "Scénarios, destination et logement",
                    value: scenarioPlanningText,
                    action: onOrganize
                )
            }

            if canAccessTransportPlanning {
                EventDetailActionRow(
                    icon: "point.topleft.down.curvedto.point.bottomright.up.fill",
                    label: "Transport",
                    value: "Départs, optimisation et plan final",
                    action: onOpenTransport
                )
            }

            if canShowOrganizationDashboard {
                EventDetailActionRow(
                    icon: "video.fill",
                    label: "Réunions",
                    value: organizationDashboardValue("Réunions et liens de coordination"),
                    action: onOpenMeetings
                )

                EventDetailActionRow(
                    icon: "eurosign.circle.fill",
                    label: "Budget et dépenses",
                    value: organizationDashboardValue("Dépenses, soldes et baseline"),
                    action: onOpenBudget
                )

                EventDetailActionRow(
                    icon: "creditcard.fill",
                    label: "Cagnotte",
                    value: organizationDashboardValue("Cagnotte commune"),
                    action: onOpenPayment
                )

                EventDetailActionRow(
                    icon: "link.circle.fill",
                    label: "Tricount",
                    value: organizationDashboardValue("Handoff sécurisé"),
                    action: onOpenTricount
                )
            }
        }
    }

    private var messagePreview: some View {
        EventDetailMessagePreview(
            title: "Messages",
            subtitle: compactMessageSubtitle,
            badgeText: event.status == .polling ? "Vote" : "Événement"
        )
    }

    private var eventAISuggestionPanel: some View {
        LiquidGlassCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack(alignment: .top, spacing: WakeveTheme.Spacing.sm) {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text("Suggestion")
                            .font(WakeveTheme.Typography.section)
                            .foregroundColor(primaryText)

                        Text("Préparez les prochaines actions à relire avant application.")
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
                    .accessibilityLabel("Préparer des suggestions")
                }

                if isGeneratingEventAI {
                    HStack(spacing: WakeveTheme.Spacing.sm) {
                        ProgressView()
                        Text("Préparation en cours")
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
                        Text("Sondages proposés")
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
                        Text("Checklist")
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
        LiquidGlassToolbar(title: "Événement", subtitle: statusText) {
            WakeveCircleButton(
                systemImage: "chevron.left",
                accessibilityLabel: "Retour",
                variant: .glass,
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
            .accessibilityLabel("Options organisateur")
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
            Label("Ajouter des participants", systemImage: "person.badge.plus")
        }

        Button(action: event.status == .polling ? onViewResults : onManageParticipants) {
            Label("Régler le sondage", systemImage: "timer")
        }

        if event.status == .polling {
            Button(action: onViewResults) {
                Label("Voir les résultats", systemImage: "chart.bar.fill")
            }
        }

        if canAccessScenarioPlanning {
            Button(action: onOrganize) {
                Label("Organiser les scénarios", systemImage: "map.fill")
            }
        }

        if canAccessTransportPlanning {
            Button(action: onOpenTransport) {
                Label("Transport", systemImage: "point.topleft.down.curvedto.point.bottomright.up.fill")
            }
        }

        if canShowOrganizationDashboard {
            Button(action: onOpenMeetings) {
                Label("Réunions", systemImage: "video.fill")
            }
            Button(action: onOpenBudget) {
                Label("Budget", systemImage: "eurosign.circle.fill")
            }
            Button(action: onOpenPayment) {
                Label("Pot commun", systemImage: "creditcard.fill")
            }
            Button(action: onOpenTricount) {
                Label("Tricount", systemImage: "link.circle.fill")
            }
        }
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
                systemImage: nextActionIcon,
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
        case .draft: return "Prévisualisation organisateur"
        case .polling: return "Sondage actif"
        case .confirmed: return "Date confirmée"
        case .organizing: return "Organisation"
        case .finalized: return "Finalisé"
        default: return "Événement"
        }
    }

    private var subtitleText: String {
        if let firstSlot = event.proposedSlots.first {
            return formatSlot(firstSlot)
        }
        return "Configurez les participants et la durée du sondage avant de publier."
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

        return "Date à choisir"
    }

    private var nextActionSubtitle: String {
        switch event.status {
        case .draft:
            return primaryActionDisabled ? "Ajoutez au moins un participant pour lancer le sondage." : "Le sondage est prêt à être envoyé aux invités."
        case .polling:
            return "Consultez les votes et confirmez le créneau le plus solide."
        case .confirmed, .comparing:
            return "Passez à la destination, au logement et aux choix collectifs."
        case .organizing:
            return "Coordonnez réunions, budget, transport et confirmations."
        case .finalized:
            return "L’événement est finalisé; les sections restent consultables."
        default:
            return "Continuez la préparation de l’événement."
        }
    }

    private var nextActionIcon: String {
        switch event.status {
        case .draft: return "paperplane.fill"
        case .polling: return "chart.bar.fill"
        case .confirmed, .comparing: return "map.fill"
        case .organizing: return "checklist"
        case .finalized: return "checkmark.seal.fill"
        default: return "arrow.right"
        }
    }

    private var participantRangeText: String {
        if let min = event.minParticipants?.intValue, let max = event.maxParticipants?.intValue {
            return "\(min) à \(max) participants"
        }
        if let expected = event.expectedParticipants?.intValue {
            return "\(expected) participants attendus"
        }
        return "\(event.participants.count) participant\(event.participants.count > 1 ? "s" : "") invité\(event.participants.count > 1 ? "s" : "")"
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
            ? "Aucun invité ajouté pour le moment."
            : "\(event.participants.count) invité\(event.participants.count > 1 ? "s" : "") à suivre."
    }

    private var participantSummary: String {
        if event.participants.isEmpty {
            return "Vous"
        }
        return "Vous + \(event.participants.count)"
    }

    private var pollDurationText: String {
        guard let deadline = parseDate(event.deadline) else {
            return "À définir"
        }

        let interval = deadline.timeIntervalSince(Date())
        if interval <= 0 {
            return "Terminé"
        }

        let days = Int(ceil(interval / 86_400))
        if days >= 2 {
            return "\(days) jours"
        }

        let hours = max(1, Int(ceil(interval / 3_600)))
        return "\(hours) h"
    }

    private var footerHint: String {
        switch event.status {
        case .draft:
            return "Ajoutez des participants et vérifiez la durée du sondage avant de lancer l’événement."
        case .polling:
            return "Suivez les votes en temps réel avant de confirmer le meilleur créneau."
        default:
            return "Les participants retrouvent ici le résumé de l’événement."
        }
    }

    private var compactMessageSubtitle: String {
        switch event.status {
        case .draft:
            return "La conversation apparaîtra quand l’événement sera partagé."
        case .polling:
            return "Rappels, réponses et questions de vote regroupés ici."
        case .confirmed, .comparing:
            return "Les échanges restent attachés au scénario en cours."
        default:
            return "Coordination, décisions et rappels visibles depuis l’événement."
        }
    }

    private var primaryActionTitle: String {
        switch event.status {
        case .draft: return "Lancer le sondage"
        case .polling: return "Voir les résultats"
        case .confirmed, .comparing: return "Comparer les scénarios"
        case .organizing, .finalized: return "Voir l'organisation"
        default: return "Continuer"
        }
    }

    private var eventHeroGradient: LinearGradient {
        LinearGradient(
            colors: heroColors,
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    private var primaryActionDisabled: Bool {
        event.status == .draft && event.participants.isEmpty
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

        Task {
            do {
                async let summary = EventSummaryGenerator(
                    client: client,
                    contextProvider: contextProvider
                ).generate(eventId: event.id, localeIdentifier: "fr_FR")
                async let polls = PollSuggestionGenerator(client: client).generate(
                    context: contextProvider.promptSummary,
                    knownFacts: knownFacts,
                    localeIdentifier: "fr_FR"
                )
                async let checklist = ChecklistGenerator(client: client).generate(
                    context: contextProvider.promptSummary,
                    knownFacts: knownFacts,
                    localeIdentifier: "fr_FR"
                )
                async let messages = InvitationMessageGenerator(client: client).generate(
                    context: contextProvider.promptSummary,
                    knownFacts: knownFacts,
                    localeIdentifier: "fr_FR"
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
                    eventAIError = "La suggestion n'est pas disponible pour le moment."
                    isGeneratingEventAI = false
                }
            }
        }
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
            return "Options ouvertes"
        case .comparing:
            return "Comparaison active"
        case .organizing:
            return "Logistique en cours"
        case .finalized:
            return "Organisation finalisée en lecture seule"
        default:
            return "Indisponible"
        }
    }

    private func organizationDashboardValue(_ value: String) -> String {
        isFinalizedOrganizationState(event) ? "\(value) - détails finalisés, aucune modification possible" : value
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
            return "Créneau à confirmer"
        }

        let dateFormatter = DateFormatter()
        dateFormatter.locale = Locale(identifier: "fr_FR")
        dateFormatter.dateFormat = "d MMM"

        let timeFormatter = DateFormatter()
        timeFormatter.locale = Locale(identifier: "fr_FR")
        timeFormatter.dateFormat = "HH:mm"

        return "\(dateFormatter.string(from: start)) · \(timeFormatter.string(from: start))"
    }

    private func formatEventDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "fr_FR")
        formatter.dateFormat = "d MMM"
        return formatter.string(from: date)
    }
}

private enum EventAIInvitationVariant: String, CaseIterable, Identifiable {
    case simple
    case warm
    case shortWhatsApp

    var id: String { rawValue }

    var title: String {
        switch self {
        case .simple: return "Simple"
        case .warm: return "Chaleureux"
        case .shortWhatsApp: return "WhatsApp"
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
        EventAIReviewBox(title: "Résumé", colorScheme: colorScheme, applied: applied, ignored: ignored, onModify: onModify, onApply: onApply, onIgnore: onIgnore) {
            EventAIList(title: "Décidé", values: summary.decided, colorScheme: colorScheme)
            EventAIList(title: "Manquant", values: summary.missing, colorScheme: colorScheme)
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
            Text("\(item.category.rawValue) · \(item.priority.rawValue)")
                .font(WakeveTheme.Typography.caption)
                .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
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
        EventAIReviewBox(title: "Invitation", colorScheme: colorScheme, applied: applied, ignored: ignored, onModify: onModify, onApply: onApply, onIgnore: onIgnore) {
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
                    Label("Appliqué", systemImage: "checkmark.circle.fill")
                        .font(WakeveTheme.Typography.tiny)
                        .foregroundColor(WakeveTheme.ColorToken.confirmation(for: colorScheme))
                } else if ignored {
                    Label("Ignoré", systemImage: "minus.circle.fill")
                        .font(WakeveTheme.Typography.tiny)
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                }
            }

            content

            HStack(spacing: WakeveTheme.Spacing.sm) {
                EventAIActionButton(title: "Modifier", systemImage: "pencil", colorScheme: colorScheme, action: onModify)
                EventAIActionButton(title: "Appliquer", systemImage: "checkmark", colorScheme: colorScheme, action: onApply)
                EventAIActionButton(title: "Ignorer", systemImage: "xmark", colorScheme: colorScheme, action: onIgnore)
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
    let isDisabled: Bool
    let action: () -> Void

    var body: some View {
        LiquidGlassCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            HStack(spacing: WakeveTheme.Spacing.md) {
                Image(systemName: systemImage)
                    .font(.title3.weight(.bold))
                    .foregroundColor(.white)
                    .frame(width: 48, height: 48)
                    .background(WakeveTheme.ColorToken.progress(for: colorScheme))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                    Text("Prochaine action")
                        .font(WakeveTheme.Typography.tiny)
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                        .textCase(.uppercase)

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
        LiquidGlassCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
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
        LiquidGlassCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
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
        LiquidGlassCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
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
