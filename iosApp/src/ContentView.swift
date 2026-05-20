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
    @State private var hasSeenGetStarted = false
    @State private var hasOnboarded = false

    var body: some View {
        ZStack {
            if authStateManager.isAuthenticated {
                if let user = authStateManager.currentUser {
                    if hasOnboarded {
                        AuthenticatedView(userId: user.id)
                    } else {
                        OnboardingView(onOnboardingComplete: {
                            markOnboardingComplete()
                            hasOnboarded = true
                        })
                    }
                } else {
                    ErrorView(message: "Authentication error: no user data", onRetry: {
                        Task {
                            authStateManager.checkAuthStatus()
                        }
                    })
                }
            } else {
                if hasSeenGetStarted {
                    LoginView()
                } else {
                    GetStartedView(onGetStarted: {
                        withAnimation(.easeInOut(duration: 0.3)) {
                            hasSeenGetStarted = true
                        }
                    })
                    .transition(.opacity)
                }
            }
        }
        .onAppear {
            hasOnboarded = hasCompletedOnboarding()
        }
        .preferredColorScheme(darkMode ? .dark : .light)
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
    @State private var selectedScenario: Scenario_?
    @State private var selectedMeetingId: String?
    @State private var selectedBudget: Budget_?
    
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
            Text("Event Creation - Coming Soon")
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
            Text("Scenario List - Coming Soon")
                .font(.title2)
                .foregroundColor(.secondary)
            
        case .scenarioComparison:
            Text("Scenario Comparison - Coming Soon")
                .font(.title2)
                .foregroundColor(.secondary)
            
        case .budgetOverview:
            if let event = selectedEvent {
                BudgetOverviewView(eventId: event.id)
            } else {
                Text("Sélectionnez un événement pour voir le budget")
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .accommodation:
            Text("Accommodation - Coming Soon")
                .font(.title2)
                .foregroundColor(.secondary)
            
        case .mealPlanning:
            if selectedEvent != nil {
                // TODO: Re-enable MealPlanningView when Shared types are properly integrated
                Text("Meal Planning - Coming Soon")
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .equipmentChecklist:
            if selectedEvent != nil {
                Text("Equipment Checklist - Coming Soon")
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .activityPlanning:
            Text("Activity Planning - Coming Soon")
                .font(.title2)
                .foregroundColor(.secondary)
            
        case .scenarioDetail:
            Text("Scenario Detail - Coming Soon")
                .font(.title2)
                .foregroundColor(.secondary)
            
        case .budgetDetail:
            if let event = selectedEvent {
                BudgetDetailView(eventId: event.id)
            } else {
                Text("Sélectionnez un événement pour voir les dépenses")
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .meetingList:
            if let event = selectedEvent {
                MeetingListView(eventId: event.id)
            } else {
                Text("Sélectionnez un événement pour voir les réunions")
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .meetingDetail:
            if let meetingId = selectedMeetingId, let event = selectedEvent {
                MeetingDetailView(meetingId: meetingId, eventId: event.id)
            } else {
                Text("Sélectionnez une réunion")
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
            ExploreTabView()
        }
    }
}



// MARK: - Explore Tab View

// ProfileTabView is now in its own file: Views/ProfileTabView.swift

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
        .padding(.top, 54)
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
        case .confirmed, .organizing, .finalized: return "Voir le récapitulatif"
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
        default:
            onViewResults()
        }
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
    }
}
