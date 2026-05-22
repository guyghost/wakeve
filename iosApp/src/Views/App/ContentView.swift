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
    @AppStorage("darkMode") private var darkMode = false
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
    
    // Profile sheet state
    @State private var showProfileSheet = false

    // Notifications state
    @State private var showNotificationPreferencesSheet = false
    @State private var unreadInboxCount: Int = 0

    // Get auth state from environment
    @EnvironmentObject var authStateManager: AuthStateManager

    var body: some View {
        // Using native iOS TabView with Apple's Liquid Glass effect (iOS 18+)
        // Profile tab removed - now accessed via profile icon in Home header
        TabView(selection: $selectedTab) {
            tabContent(for: .home)
                .tabItem {
                    Label("Accueil", systemImage: "house.fill")
                }
                .tag(WakeveTab.home)

            tabContent(for: .inbox)
                .tabItem {
                    Label("Inbox", systemImage: "tray.fill")
                }
                .tag(WakeveTab.inbox)
                .badge(unreadInboxCount)

            tabContent(for: .explore)
                .tabItem {
                    Label("Explorer", systemImage: "sparkles")
                }
                .tag(WakeveTab.explore)
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
                        print("Failed to save event: \(error)")
                    }
                }
            }
        }
        .sheet(isPresented: $showProfileSheet) {
            ProfileTabView(
                userId: userId,
                userName: authStateManager.currentUser?.name,
                userEmail: authStateManager.currentUser?.email,
                onDismiss: { showProfileSheet = false },
                onSignOut: {
                    authStateManager.signOut()
                }
            )
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
                    // Show profile settings sheet
                    showProfileSheet = true
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
                // TODO: Re-enable MealPlanningView when Shared types are properly integrated
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
        case .inbox:
            InboxView(
                userId: userId,
                onBack: { /* Inbox is main tab, no back action needed */ },
                unreadCount: $unreadInboxCount
            )
        case .explore:
            ExploreTabView { _ in
                showEventCreationSheet = true
            }
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
                    if let safeLink, safeLink.isVerified, let validatedURL = safeLink.validatedURL {
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

    var body: some View {
        ZStack(alignment: .top) {
            pageBackground
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    heroSection

                    VStack(alignment: .leading, spacing: 18) {
                        titleBlock
                        organizerControls

                        if let description = displayDescription {
                            Text(description)
                                .font(.system(size: 15, weight: .regular))
                                .foregroundColor(secondaryText)
                                .lineSpacing(3)
                                .padding(.top, 2)
                        }

                        detailRows

                        Text(footerHint)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(.white.opacity(0.42))
                            .frame(maxWidth: .infinity)
                            .multilineTextAlignment(.center)
                            .padding(.top, 20)
                            .padding(.bottom, 84)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, -34)
                }
            }
            .ignoresSafeArea(edges: .top)

            topControls
        }
        .toolbar(.hidden, for: .tabBar)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            bottomPrimaryAction
        }
    }

    private var heroSection: some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(
                colors: heroColors,
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .overlay {
                eventSymbol
                    .font(.system(size: 118, weight: .black))
                    .foregroundColor(.white.opacity(0.2))
                    .rotationEffect(.degrees(-10))
                    .offset(x: 86, y: -18)
            }
            .overlay(alignment: .bottom) {
                LinearGradient(
                    colors: [.clear, pageBackground.opacity(colorScheme == .dark ? 0.94 : 0.98)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 150)
            }

            VStack(alignment: .leading, spacing: 10) {
                statusBadge

                HStack(spacing: 8) {
                    Image(systemName: "person.2.fill")
                        .font(.system(size: 14, weight: .semibold))
                    Text(participantRangeText)
                        .font(.system(size: 15, weight: .semibold))
                }
                .foregroundColor(.white.opacity(0.86))
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 54)
        }
        .frame(height: 330)
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

    private var detailRows: some View {
        VStack(spacing: 10) {
            EventPreviewDetailRow(
                icon: "timer",
                label: "Temps imparti au sondage",
                value: pollDurationText,
                accessory: event.status == .draft ? "chevron.up.chevron.down" : nil,
                action: onManageParticipants
            )

            EventPreviewDetailRow(
                icon: "calendar",
                label: "Créneaux proposés",
                value: "\(event.proposedSlots.count) option\(event.proposedSlots.count > 1 ? "s" : "")",
                accessory: "chevron.right",
                action: event.status == .polling ? onVote : onViewResults
            )

            EventPreviewDetailRow(
                icon: "person.crop.circle.fill",
                label: "Participants",
                value: participantSummary,
                accessory: "chevron.right",
                action: onManageParticipants
            )

            if canAccessScenarioPlanning {
                EventPreviewDetailRow(
                    icon: "map.fill",
                    label: "Scénarios, destination et logement",
                    value: scenarioPlanningText,
                    accessory: "chevron.right",
                    action: onOrganize
                )
            }

            if canAccessTransportPlanning {
                EventPreviewDetailRow(
                    icon: "point.topleft.down.curvedto.point.bottomright.up.fill",
                    label: "Transport",
                    value: "Départs, optimisation et plan final",
                    accessory: "chevron.right",
                    action: onOpenTransport
                )
            }

            if canShowOrganizationDashboard {
                EventPreviewDetailRow(
                    icon: "video.fill",
                    label: "Réunions",
                    value: organizationDashboardValue("Réunions et liens de coordination"),
                    accessory: "chevron.right",
                    action: onOpenMeetings
                )

                EventPreviewDetailRow(
                    icon: "eurosign.circle.fill",
                    label: "Budget et dépenses",
                    value: organizationDashboardValue("Dépenses, soldes et baseline"),
                    accessory: "chevron.right",
                    action: onOpenBudget
                )

                EventPreviewDetailRow(
                    icon: "creditcard.fill",
                    label: "Cagnotte",
                    value: organizationDashboardValue("Cagnotte commune"),
                    accessory: "chevron.right",
                    action: onOpenPayment
                )

                EventPreviewDetailRow(
                    icon: "link.circle.fill",
                    label: "Tricount",
                    value: organizationDashboardValue("Handoff sécurisé"),
                    accessory: "chevron.right",
                    action: onOpenTricount
                )
            }
        }
        .padding(.top, 6)
    }

    private var topControls: some View {
        HStack {
            WakeveCircleButton(
                systemImage: "chevron.left",
                accessibilityLabel: "Retour",
                variant: .glass,
                size: 44,
                action: onBack
            )

            Spacer()

            Menu {
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
            } label: {
                WakeveGlassControl {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                }
            }
            .accessibilityLabel("Options organisateur")
        }
        .padding(.horizontal, 16)
        .padding(.top, WakeveTheme.Navigation.controlTopSpacing)
    }

    private var bottomPrimaryAction: some View {
        VStack(spacing: 0) {
            LinearGradient(
                colors: [pageBackground.opacity(0), pageBackground],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 36)
            .allowsHitTesting(false)

            Button(action: primaryAction) {
                Text(primaryActionTitle)
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(primaryButtonText)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(primaryActionDisabled ? primaryButtonBackground.opacity(0.54) : primaryButtonBackground)
                    .clipShape(Capsule())
            }
            .disabled(primaryActionDisabled)
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 12)
            .background(pageBackground)
        }
    }

    private var statusBadge: some View {
        HStack(spacing: 6) {
            Image(systemName: statusIcon)
                .font(.system(size: 13, weight: .bold))
            Text(statusText)
                .font(.system(size: 14, weight: .bold))
        }
        .foregroundColor(.white)
        .padding(.horizontal, 12)
        .padding(.vertical, 7)
        .background(Color.black.opacity(0.26))
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

    private var participantRangeText: String {
        if let min = event.minParticipants?.intValue, let max = event.maxParticipants?.intValue {
            return "\(min) à \(max) participants"
        }
        if let expected = event.expectedParticipants?.intValue {
            return "\(expected) participants attendus"
        }
        return "\(event.participants.count) participant\(event.participants.count > 1 ? "s" : "") invité\(event.participants.count > 1 ? "s" : "")"
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

    private var primaryActionTitle: String {
        switch event.status {
        case .draft: return "Lancer le sondage"
        case .polling: return "Voir les résultats"
        case .confirmed, .comparing: return "Comparer les scénarios"
        case .organizing, .finalized: return "Voir l'organisation"
        default: return "Continuer"
        }
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
